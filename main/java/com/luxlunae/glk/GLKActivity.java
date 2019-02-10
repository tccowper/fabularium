/*
 * Copyright (C) 2018 Tim Cadogan-Cowper.
 *
 * This file is part of Fabularium.
 *
 * Fabularium is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Fabularium; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.luxlunae.glk;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.fabularium.MainActivity;
import com.luxlunae.fabularium.PreferencesActivity;
import com.luxlunae.fabularium.PreferencesActivity.ParcelableSharedPrefs;
import com.luxlunae.fabularium.R;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.view.keyboard.GLKKeyboardView;
import com.luxlunae.glk.view.window.GLKNonPairV;
import com.luxlunae.glk.view.window.GLKScreen;
import com.luxlunae.glk.view.window.GLKTextBufferV;
import com.luxlunae.glk.view.window.GLKTextWindowV;
import com.luxlunae.glk.view.window.GLKWindowV;

public class GLKActivity extends FragmentActivity
        implements GLKScreen.GLKScreenResizeCallback, TextToSpeech.OnInitListener {

    private static final int CHECK_TTS_DATA = 1;

    private static final boolean DEBUG_LIFECYCLE = false;
    private static final boolean DEBUG_SOFT_KEYBOARD = false;        // if true, soft keyboard is always shown, even if hard keyboard present
    private static final boolean DEBUG_INPUT_OPS = false;
    @Nullable
    private Thread mController;
    private GLKModel mModel;
    private GLKScreen mScreen;
    @Nullable
    private GLKKeyboardView mKeyboardView;
    @Nullable
    private TextToSpeech mTts;
    // Both of these flags must be true before we start the terp thread
    private boolean mScreenReady = false;
    private boolean mTTSDone = true;
    private boolean mDebuggingMode = false;

    private static int getBundleSizeInBytes(Bundle bundle) {
        Parcel parcel = Parcel.obtain();
        int size;

        parcel.writeBundle(bundle);
        size = parcel.dataSize();
        parcel.recycle();

        return size;
    }

    /**
     * Called when the activity is starting. This is where most initialization should go:
     * calling setContentView(int) to inflate the activity's UI, using findViewById(int)
     * to programmatically interact with widgets in the UI, calling
     * managedQuery(android.net.Uri, String[], String, String[], String) to
     * retrieve cursors for data being displayed, etc.
     * <p>
     * You can call finish() from within this function, in which case onDestroy()
     * will be immediately called without any of the rest of the activity lifecycle (onStart(), onResume(), onPause(), etc) executing.
     * <p>
     * Derived classes must call through to the super class's implementation of this method. If they do not, an exception will be thrown.
     * <p>
     * This method must be called from the main thread of your app.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     *
     * @param savedInstanceState -  If the activity is being re-initialized after previously being shut down
     *                           then this Bundle contains the data it most recently supplied in
     *                           onSaveInstanceState(Bundle). Note: Otherwise it is null.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        GLKLogger.initialise(this);
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String gameFormat = intent.getStringExtra(MainActivity.GAME_FORMAT);
        if (!gameFormat.equals("html")) {
            // The first time this is called it creates a new ViewModel instance
            // When called again during same application lifecycle, it will return pre-existing instance
            mModel = ViewModelProviders.of(this).get(GLKModel.class);
        }

        if (savedInstanceState == null) {
            GLKLogger.debug("Creating new GLKActivity...");
            String gamePath = intent.getStringExtra(MainActivity.GAME_PATH);
            if (gamePath == null) {
                GLKLogger.error("GLKActivity: intent does not include a game path. Cannot run game, sorry.");
                finish();
                return;
            }
            String gameIFID = intent.getStringExtra(MainActivity.GAME_IFID);
            if (gameIFID == null) {
                // We use the IFID to work out which folder to save the game's data, save files, etc. into.
                // If there is no IFID specified, in the interests of still being able to run the game we'll
                // default to ????, but also warn the user in the log that this may lead to clashes with other
                // games missing IFIDS re saves, data, etc.
                GLKLogger.warn("GLKActivity: intent does not include a game IFID. Defaulting to ????. Please note this means your game will share its data (including saves) with any other games that don't have an IFID, meaning that you may find incompatible saves for the other games appearing when you type 'restore', etc.");
                gameIFID = "????";
            }

            Parcelable p = intent.getParcelableExtra(MainActivity.USER_PREFS);
            if (p == null || !(p instanceof ParcelableSharedPrefs)) {
                GLKLogger.error("GLKActivity: intent does not include user's preferences. Cannot run game, sorry.");
                finish();
                return;
            }

            // Note: we need to retrieve the ParcelableSharedPrefs object that was (hopefully) sent by the
            // calling Activity, rather than calling PreferenceManager.getDefaultSharedPreferences(<Context>),
            // as SharedPreferences objects may not have a consistent state across processes.
            //
            // See https://developer.android.com/reference/android/content/SharedPreferences.html:
            //
            //       "Note: This class does not support use across multiple processes."
            //
            ParcelableSharedPrefs sharedPref = (ParcelableSharedPrefs) p;
            if (gameFormat.equals("html")) {
                // HTML - run game in a webview
                boolean enableJs = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_HTML_ENABLE_JS, true);
                boolean enableDom = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_HTML_ENABLE_DOM, true);
                boolean enableZoom = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_HTML_ENABLE_ZOOM, true);
                boolean displayZoom = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_HTML_DISPLAY_ZOOM, false);
                boolean loadOverview = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_HTML_LOAD_OVEVIEW, false);
                boolean useWideViewport = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_HTML_USE_WIDE_VIEWPORT, false);

                // The one and only view for HTML games (we always use the system keyboard).
                WebView screen = new WebView(this);
                WebSettings settings = screen.getSettings();
                settings.setJavaScriptEnabled(enableJs);
                settings.setDomStorageEnabled(enableDom);
                settings.setBuiltInZoomControls(enableZoom);
                settings.setDisplayZoomControls(displayZoom);
                settings.setLoadWithOverviewMode(loadOverview);
                settings.setUseWideViewPort(useWideViewport);
                screen.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                // Note that requestWindowFeature(...) and setFlags(...) need to be called BEFORE setContentView.
                requestWindowFeature(Window.FEATURE_NO_TITLE);
                Window win = getWindow();
                if (win == null) {
                    // getWindow() may return NULL "if the activity is not visual". Not sure what that means, in the Android
                    // source code it returns mWindow which is set when the internal function attach() is called. Anyway, to
                    // be safe just check here and die if necessary.
                    GLKLogger.error("GLKActivity: cannot get reference to underlying window object. Cannot run game, sorry.");
                    finish();
                    return;
                }
                win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

                // From the Android documentation:
                //   setContentView:
                //     "Note that calling this function "locks in" various characteristics of the window that can not,
                //      from this point forward, be changed: the features that have been requested with requestFeature(int),
                //      and certain window flags as described in setFlags(int, int)."
                setContentView(screen);

                // Keep clicked upon links within the same webview window rather
                // than launching in a new browser. Because we're not passing
                // local HTML files to another app this should also ensure that
                // we don't crash with a FileUriExposedException.
                // Thanks https://stackoverflow.com/questions/40560604/navigating-asset-based-html-files-in-webview-on-nougat
                screen.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return false;
                    }
                });

                screen.loadUrl("file:///" + gamePath);
            } else {
                // Not HTML - run game on GLK

                // 1. Create the model
                mModel.initialise(sharedPref, gamePath, gameFormat, gameIFID);

                // 2. Create the view
                if ((mModel.mUsingHardwareKeyboard && !DEBUG_SOFT_KEYBOARD) || mModel.mUseBuiltinKeyboard || mModel.mKeyboardOff) {
                    // We can show the game full screen for these cases
                    // Note - we can't show the game full screen if the
                    // user wants their system keyboard, because the game window
                    // won't resize when the system keyboard pops up. Google says this
                    // is the intended behaviour, without explaining why:
                    //
                    //   https://developer.android.com/reference/android/view/WindowManager.LayoutParams.html#FLAG_FULLSCREEN
                    //
                    // See also:
                    //
                    //  https://stackoverflow.com/questions/7417123/android-how-to-adjust-layout-in-full-screen-mode-when-softkeyboard-is-visible
                    //  https://issuetracker.google.com/issues/36911528
                    //
                    // If lack of full-screen bothers you, for now you'll just have to use the built-in keyboard instead.
                    requestWindowFeature(Window.FEATURE_NO_TITLE);
                    Window win = getWindow();
                    if (win == null) {
                        // getWindow() may return NULL "if the activity is not visual". Not sure what that means, in the Android
                        // source code it returns mWindow which is set when the internal function attach() is called. Anyway, to
                        // be safe just check here and die if necessary.
                        GLKLogger.error("GLKActivity: cannot get reference to underlying window object. Cannot run game, sorry.");
                        finish();
                        return;
                    }
                    win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
                }

                if ((mModel.mUsingHardwareKeyboard && !DEBUG_SOFT_KEYBOARD) || mModel.mKeyboardOff) {
                    // don't display any soft keyboard as we are either using the hardware keyboard or
                    // the user has turned off the keyboard via fab.ini
                    mScreen = new GLKScreen(this);
                    mKeyboardView = null;
                    setContentView(mScreen);
                } else {
                    // display a soft keyboard
                    if (mModel.mUseBuiltinKeyboard) {
                        // display our built-in keyboard (integrate into our content view)
                        setContentView(R.layout.glk_display);
                        mScreen = findViewById(GLKConstants.NULL);
                        mKeyboardView = findViewById(R.id.glk_keyboard);
                        mKeyboardView.setView(mScreen);
                        mKeyboardView.setKeyboardMappings(mModel.mKeyboardMappings);
                        mKeyboardView.resetKeyboard();
                    } else {
                        // display the system keyboard
                        mScreen = new GLKScreen(this);
                        mKeyboardView = null;
                        Window win = getWindow();
                        if (win == null) {
                            // getWindow() may return NULL "if the activity is not visual". Not sure what that means, in the Android
                            // source code it returns mWindow which is set when the internal function attach() is called. Anyway, to
                            // be safe just check here and die if necessary.
                            GLKLogger.error("GLKActivity: cannot get reference to underlying window object. Cannot run game, sorry.");
                            finish();
                            return;
                        }
                        win.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                        setContentView(mScreen);
                    }
                }
                mScreen.setWindowMargins(mModel.mDefaultWindowMarginsPX);
                mScreen.setBackgroundColor(mModel.mBackgroundColor);
                mScreen.setResizeCallback(this);

                mModel.attachToScreen(mScreen);
                mModel.attachToFragmentManager(getFragmentManager());

                // 3) Create the controller thread
                mController = GLKController.create(mModel, this);

                // Set up text to speech if the user has requested it.
                if (mModel.mEnableTextToSpeech) {
                    // First check if speech resources are available
                    // as per recommendation on official Android developer's blog
                    // at https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html
                    mTTSDone = false;
                    Intent checkIntent = new Intent();
                    checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                    startActivityForResult(checkIntent, CHECK_TTS_DATA);
                } else {
                    mTts = null;
                }
            }
        } else {
            // Activity is restarting after being shut down
            // Here are the state fields we need to consider:
            //   1. mModel - already restored as it extends AndroidViewModel
            //   2. mScreen - should be restored automatically as it is part of the view hierarchy
            //   3. mKeyboardView - ditto
            //   4. mController - a bit trickier, but as this is basically a JNI thread seems like it
            //      won't actually get terminated anyway...? Not sure...?
            //      See https://stackoverflow.com/questions/11400853/life-span-of-jni-c-heap-in-an-android-app
            GLKLogger.debug("Restoring state of GLKActivity...");

            // refresh the volatile references held by mModel
            mModel.attachToFragmentManager(getFragmentManager());

            // Set up text to speech if the user has requested it.
            if (mModel.mEnableTextToSpeech) {
                // First check if speech resources are available
                // as per recommendation on official Android developer's blog
                // at https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, CHECK_TTS_DATA);
            } else {
                mTts = null;
            }
        }
    }

    /**
     * Called after onCreate(Bundle) — or after onRestart() when the activity
     * had been stopped, but is now again being displayed to the user. It will be
     * followed by onResume().
     * <p>
     * Derived classes must call through to the super class's implementation of this
     * method. If they do not, an exception will be thrown.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onStart() {
        if (DEBUG_LIFECYCLE) {
            GLKLogger.debug("GLKActivity: onStart()");
        }
        GLKLogger.flush();
        super.onStart();
    }

    /**
     * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
     * for your activity to start interacting with the user. This is a good place
     * to begin animations, open exclusive-access devices (such as the camera), etc.
     * <p>
     * Keep in mind that onResume is not the best indicator that your activity is
     * visible to the user; a system window such as the keyguard may be in front.
     * Use onWindowFocusChanged(boolean) to know for certain that your activity is
     * visible to the user (for example, to resume a game).
     * <p>
     * Derived classes must call through to the super class's implementation of this
     * method. If they do not, an exception will be thrown.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onResume() {
        if (DEBUG_LIFECYCLE) {
            GLKLogger.debug("GLKActivity: onResume()");
        }
        GLKLogger.flush();
        super.onResume();
    }

    /**
     * Called as part of the activity lifecycle when an activity is going into
     * the background, but has not (yet) been killed. The counterpart to onResume().
     * <p>
     * When activity B is launched in front of activity A, this callback will be invoked on A.
     * B will not be created until A's onPause() returns, so be sure to not do anything lengthy here.
     * <p>
     * This callback is mostly used for saving any persistent state the activity is editing,
     * to present a "edit in place" model to the user and making sure nothing is lost if
     * there are not enough resources to start the new activity without first killing
     * this one. This is also a good place to do things like stop animations and other
     * things that consume a noticeable amount of CPU in order to make the switch to the next
     * activity as fast as possible, or to close resources that are exclusive access such as the camera.
     * <p>
     * In situations where the system needs more memory it may kill paused processes to reclaim
     * resources. Because of this, you should be sure that all of your state is saved by the
     * time you return from this function. In general onSaveInstanceState(Bundle) is used to save
     * per-instance state in the activity and this method is used to store global persistent data
     * (in content providers, files, etc.)
     * <p>
     * After receiving this call you will usually receive a following call to onStop() (after the
     * next activity has been resumed and displayed), however in some cases there will be a
     * direct call back to onResume() without going through the stopped state.
     * <p>
     * Derived classes must call through to the super class's implementation of this method.
     * If they do not, an exception will be thrown.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onPause() {
        if (DEBUG_LIFECYCLE) {
            GLKLogger.debug("GLKActivity: onPause()");
        }
        if (mTts != null) {
            int ret = mTts.stop();
            if (ret == TextToSpeech.ERROR) {
                GLKLogger.warn("GLKActivity: error when asking Android to stop text to speech.");
            }
        }
        GLKLogger.flush();
        super.onPause();
    }

    /**
     * Called to retrieve per-instance state from an activity before being killed so that the state
     * can be restored in onCreate(Bundle) or onRestoreInstanceState(Bundle)
     * (the Bundle populated by this method will be passed to both).
     * <p>
     * This method is called before an activity may be killed so that when it comes back
     * some time in the future it can restore its state. For example, if activity B is launched
     * in front of activity A, and at some point activity A is killed to reclaim resources,
     * activity A will have a chance to save the current state of its user interface via this
     * method so that when the user returns to activity A, the state of the user interface
     * can be restored via onCreate(Bundle) or onRestoreInstanceState(Bundle).
     * <p>
     * Do not confuse this method with activity lifecycle callbacks such as onPause(),
     * which is always called when an activity is being placed in the background or
     * on its way to destruction, or onStop() which is called before destruction. One
     * example of when onPause() and onStop() is called and not this method is
     * when a user navigates back from activity B to activity A: there is no
     * need to call onSaveInstanceState(Bundle) on B because that particular
     * instance will never be restored, so the system avoids calling it. An
     * example when onPause() is called and not onSaveInstanceState(Bundle) is
     * when activity B is launched in front of activity A: the system may avoid
     * calling onSaveInstanceState(Bundle) on activity A if it isn't killed during
     * the lifetime of B since the state of the user interface of A will stay intact.
     * <p>
     * The default implementation takes care of most of the UI per-instance state for
     * you by calling onSaveInstanceState() on each view in the hierarchy that has an id,
     * and by saving the id of the currently focused view (all of which is restored by
     * the default implementation of onRestoreInstanceState(Bundle)). If you override
     * this method to save additional information not captured by each individual view,
     * you will likely want to call through to the default implementation, otherwise be
     * prepared to save all of the state of each view yourself.
     * <p>
     * If called, this method will occur before onStop(). There are no guarantees about
     * whether it will occur before or after onPause().
     *
     * @param outState - Bundle in which to place your saved state.
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG_LIFECYCLE) {
            int nBytes = getBundleSizeInBytes(outState);
            GLKLogger.debug("GLKActivity: onSaveInstanceState() => saved bundle is " + nBytes + " bytes.");
            if (nBytes > 400000) {
                // Warn if more than 400KB
                GLKLogger.error("GLKActivity: onSaveInstanceState(): bundle is too large (" + nBytes + " bytes) - expect crash!");
            }
            GLKLogger.flush();
        }
    }

    /**
     * Called when you are no longer visible to the user. You will next receive
     * either onRestart(), onDestroy(), or nothing, depending on later user activity.
     * <p>
     * Derived classes must call through to the super class's implementation of this
     * method. If they do not, an exception will be thrown.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onStop() {
        if (DEBUG_LIFECYCLE) {
            GLKLogger.debug("GLKActivity: onStop()");
        }
        GLKLogger.flush();
        super.onStop();
    }

    /**
     * Perform any final cleanup before an activity is destroyed. This can happen either
     * because the activity is finishing (someone called finish() on it, or because the
     * system is temporarily destroying this instance of the activity to save space. You
     * can distinguish between these two scenarios with the isFinishing() method.
     * <p>
     * Note: do not count on this method being called as a place for saving data! For
     * example, if an activity is editing data in a content provider, those edits
     * should be committed in either onPause() or onSaveInstanceState(Bundle), not here.
     * This method is usually implemented to free resources like threads that are
     * associated with an activity, so that a destroyed activity does not leave such
     * things around while the rest of its application is still running. There are
     * situations where the system will simply kill the activity's hosting process
     * without calling this method (or any others) in it, so it should not be used to
     * do things that are intended to remain around after the process goes away.
     * <p>
     * Derived classes must call through to the super class's implementation of this
     * method. If they do not, an exception will be thrown.
     * <p>
     * If you override this method you must call through to the superclass implementation.
     */
    @Override
    public void onDestroy() {
        // This should only ever be called on UI thread
        if (isFinishing()) {
            GLKLogger.debug("Finishing GLKActivity...");

            // Tell the model to shut down
            if (mModel != null) {
                mModel.shutdown();
                mModel = null;
            }

            // Tell the View to shut down
            if (mScreen != null) {
                mScreen.shutdown();
                mScreen = null;
            }

            if (mTts != null) {
                // From the Android developer docs for the TextToSpeech class:
                //   "Releases the resources used by the TextToSpeech engine. It is good practice
                //    for instance to call this method in the onDestroy() method of an Activity
                //    so the TextToSpeech engine can be cleanly stopped."
                mTts.shutdown();
                mTts = null;
            }

            // We assume by this stage terp and model already shutdown
            // Now we just need to shutdown the View and any other UI stuff,
            // then join back the threads
            if (mController != null) {
                // Tell the worker thread to die, then
                // wait for it to join back to this UI thread
                mController.interrupt();
                try {
                    mController.join();
                    GLKLogger.debug("Successfully terminated game terp thread.");
                } catch (InterruptedException e) {
                    GLKLogger.debug("Exception raised while terminating game terp thread: " + e.getMessage());
                }
                mController = null;
            }

            GLKLogger.debug("Shutting down logger and finishing...");
            GLKLogger.shutdown();
        } else {
            GLKLogger.error("FIXME: GLKActivity: onDestroy() called by system to save memory...");
        }
        super.onDestroy();
    }

    /**
     * Called by the system when the device configuration changes
     * while your activity is running. Note that this will only be called
     * if you have selected configurations you would like to handle with
     * the configChanges attribute in your manifest. If any configuration
     * change occurs that is not selected to be reported by that attribute,
     * then instead of reporting it the system will stop and restart the
     * activity (to have it launched with the new configuration).
     * <p>
     * At the time that this function has been called, your Resources object will
     * have been updated to return resource values matching the new configuration.
     *
     * @param newConfig - The new device configuration.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // TODO: hide/show on-screen keyboard if the user has
        // connected / disconnected a bluetooth keyboard
    }

    @Override
    public void onScreenSizeChanged(int w, int h, int oldw, int oldh) {
        if (w > 0 && h > 0 && (w != oldw || h != oldh)) {
            GLKLogger.debug("GLKActivity: screen size set to " + w + " x " + h + " pixels.");
            mModel.setScreenSize(new Point(w, h));
            mScreenReady = true;
            if (mController != null && mController.isAlive()) {
                // The terp is already running. All we need to
                // worry about is ensuring the keyboard is correctly
                // resized.
                if (mKeyboardView != null) {
                    mKeyboardView.post(new Runnable() {
                        @Override
                        public void run() {
                            mKeyboardView.resetKeyboard();
                        }
                    });
                }
            } else {
                // The terp is not running. If we've already finished
                // playing any TTS (or no TTS to play), we're now
                // ready to boot it up.
                if (mTTSDone) {
                    if (mController != null) {
                        try {
                            mController.start();
                        } catch (IllegalThreadStateException e) {
                            // thread has already started - just leave it
                            GLKLogger.warn("GLKActivity: onScreenSizeChanged: trying to start a thread that has already started!");
                        }
                    } else {
                        GLKLogger.error("GLKActivity: onScreenSizeChanged: Cannot start terp as the controller object is null,");
                        finish();
                    }
                }
            }
        }
    }

    public boolean sendKeyCodes(@NonNull int[] keyCodes, @Nullable int[] keysSent) {
        // Returns TRUE if this triggered an event (char, line, debug); FALSE otherwise
        if (mScreen == null) {
            // can't do anything as can't locate the appropriate window
            return false;
        }
        GLKWindowV v = mScreen.getFocusedWindow();
        return !(v == null || !(v instanceof GLKNonPairV)) &&
                sendKeyCodes((GLKNonPairV) v, keyCodes, keysSent);
    }

    public boolean sendKeyCodes(@NonNull GLKNonPairV destView, @NonNull int[] keyCodes, @Nullable int[] keysSent) {
        // Returns TRUE if this triggered an event (char, line, debug); FALSE otherwise
        // You can optionally pass an argument to an array keysSent. When this function returns
        // keysSent[0] will be the number of items of 'keyCodes' that were actually consumed.
        if (DEBUG_INPUT_OPS) {
            FabLogger.debug("GLKScreen: sendKeyCodes: length = " + keyCodes.length +
                    "' => window " + destView.getStreamId());
        }

        int szKeyCodes = keyCodes.length;
        if (szKeyCodes == 0) {
            // nothing to do
            return false;
        }

        // if, after processing the codes below, this event
        // is no longer null we will send it through to the terp
        GLKEvent ev = null;
        GLKTextBufferV tb = (destView instanceof GLKTextBufferV) ?
                (GLKTextBufferV) destView : null;
        GLKTextWindowV twV = (destView instanceof GLKTextWindowV) ?
                (GLKTextWindowV) destView : null;
        int k = 0;

        // process the code sequence
        for (int i = 0; i < szKeyCodes && keyCodes[i] != GLKKeyboardView.NOT_A_KEY; i++) {
            k++;
            int ch = keyCodes[i];

            // if we're currently in debug mode, trap any debug codes
            if (mDebuggingMode) {
                int dbg = 0;
                switch (ch) {
                    case 'S':
                        dbg = GLKConstants.FAB_DEBUG_KEY_SHOW_STREAMS;
                        break;
                    case 'W':
                        dbg = GLKConstants.FAB_DEBUG_KEY_SHOW_WINDOWS;
                        break;
                    case 'M':
                        dbg = GLKConstants.FAB_DEBUG_KEY_MEM_INFO;
                        break;
                    case 'F':
                        dbg = GLKConstants.FAB_DEBUG_KEY_SHOW_STYLES;
                        break;
                }

                if (dbg != 0) {
                    // this is a debug code - trap it
                    if (DEBUG_INPUT_OPS) {
                        FabLogger.debug("GLKActivity: trapping debug key.");
                    }
                    if (tb != null) {
                        ev = new GLKEvent();
                        ev.debugEvent(tb.getStreamId(), dbg);
                        break;
                    }
                }
            }

            // trap other codes
            if (ch == GLKConstants.FAB_KEY_DEBUG) {
                if (mScreen != null) {
                    mDebuggingMode = !mDebuggingMode;
                    Toast.makeText(mScreen.getContext(),
                            "Debugging mode is now ".concat((mDebuggingMode ? "on" : "off")), Toast.LENGTH_SHORT).show();
                }
            } else if (ch == GLKConstants.FAB_KEY_NEXT) {
                if (mKeyboardView != null) {
                    mKeyboardView.moveToNextKeyboard();
                }
            } else {
                // key for the terp
                if (DEBUG_INPUT_OPS) {
                    FabLogger.debug("GLKActivity: translating key to GLKCode...");
                }
                int glkKey = GLKUtils.translateKeyToGLKCode(ch);
                if (glkKey != 0) {
                    if (destView.charRequested()) {
                        // Send this key and stop
                        if (DEBUG_INPUT_OPS) {
                            FabLogger.debug("GLKActivity: focused view has char requested.. sending char event");
                        }
                        ev = new GLKEvent();
                        ev.charEvent(destView.getStreamId(), glkKey);
                        break;
                    } else if (twV != null && twV.lineRequested()) {
                        // This is a text buffer or grid window, so send this key and
                        // continue until we run out of keys to send or we hit a terminator
                        if (DEBUG_INPUT_OPS) {
                            FabLogger.debug("GLKActivity: focused view has line requested.. adding key to buffer");
                        }
                        int len = twV.sendKey(glkKey);
                        if (twV.isTerminator(glkKey)) {
                            // if this character is a terminator then stop
                            // "val2 will be 0 unless input was ended by a special terminator key,
                            // in which case val2 will be the keycode (one of the values passed to glk_set_terminators_line_event())"
                            if (DEBUG_INPUT_OPS) {
                                FabLogger.debug("GLKActivity: key was terminator... sending line event");
                            }
                            ev = new GLKEvent();
                            ev.lineEvent(destView.getStreamId(), len, glkKey == GLKConstants.keycode_Return ? 0 : glkKey);
                            break;
                        }
                    }
                }
            }
        }

        if (keysSent != null && keysSent.length > 0) {
            keysSent[0] = k;
        }

        if (ev != null) {
            if (tb != null && k < szKeyCodes) {
                // We didn't process everything and it was directed at a text buffer window
                // Tell the text buffer window to store the rest and use for future commands
                tb.appendBufferedInput(keyCodes, k, szKeyCodes - k);
            }
            GLKController.postEvent(ev);
            return true;
        }

        return false;
    }

    @Override
    public boolean dispatchKeyEvent(@NonNull KeyEvent event) {
        if (mModel != null && mModel.mUsingHardwareKeyboard) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                int keyCode = event.getKeyCode();
                int primaryCode;
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DEL:
                        // backspace
                        primaryCode = 8;
                        break;
                    case KeyEvent.KEYCODE_TAB:
                        primaryCode = 9;
                        break;
                    case KeyEvent.KEYCODE_ESCAPE:
                        primaryCode = 27;
                        break;
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        primaryCode = 8592;
                        break;
                    case KeyEvent.KEYCODE_DPAD_UP:
                        primaryCode = 8593;
                        break;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        primaryCode = 8594;
                        break;
                    case KeyEvent.KEYCODE_DPAD_DOWN:
                        primaryCode = 8595;
                        break;
                    case KeyEvent.KEYCODE_PAGE_UP:
                        primaryCode = 8670;
                        break;
                    case KeyEvent.KEYCODE_PAGE_DOWN:
                        primaryCode = 8671;
                        break;
                    default:
                        primaryCode = event.getUnicodeChar();
                        break;
                }
                int keyCodes[] = new int[1];
                keyCodes[0] = primaryCode;
                sendKeyCodes(keyCodes, null);
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.glk_force_quit_title)
                .setMessage(R.string.glk_force_quit_message)
                .setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mController != null) {
                            mController.interrupt();
                        } else {
                            finish();
                        }
                    }

                })
                .setNegativeButton(R.string.dialog_no, null)
                .show();
    }

    /**
     * Called to signal the completion of the TextToSpeech engine initialisation.
     *
     * @param status - SUCCESS or ERROR
     */
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.ERROR) {
            GLKLogger.error("GLKActivity: Android cannot initialise the text to speech engine. TTS will be disabled for this session.");
            mTts = null;
        } else if (mTts != null) {
            int r = mTts.setLanguage(mModel.mLocale);
            if (r == TextToSpeech.LANG_NOT_SUPPORTED) {
                GLKLogger.error("GLKActivity: Android says the language of your specified locale (" +
                        mModel.mLocale.getLanguage() + ") is not supported for TTS. TTS will be disabled for this session.");
                mTts = null;
            } else if (r == TextToSpeech.LANG_MISSING_DATA) {
                GLKLogger.error("GLKActivity: Android says your device is missing language data for your specified locale (" +
                        mModel.mLocale.getLanguage() + "). TTS will be disabled for this session.");
                mTts = null;
            }
            if (mTts != null) {
                if (mModel.mTtsPitch != 1f) {
                    r = mTts.setPitch(mModel.mTtsPitch);
                    if (r == TextToSpeech.ERROR) {
                        GLKLogger.warn("GLKActivity: Android says it can't set the TTS pitch to " + mModel.mTtsPitch + ".");
                    }
                }
                if (mModel.mTtsRate != 1f) {
                    r = mTts.setSpeechRate(mModel.mTtsRate);
                    if (r == TextToSpeech.ERROR) {
                        GLKLogger.warn("GLKActivity: Android says it can't set the TTS speech rate to " + mModel.mTtsRate + ".");
                    }
                }
            }
            GLKLogger.debug("GLKActivity: successfully initialised text to speech engine with locale '" + mModel.mLocale.getLanguage() + "'.");
        }

        mTTSDone = true;
        if (mScreenReady) {
            // if the screen is not yet ready, the controller will be initialised in the
            // onScreenSizeChanged() callback. We assume onInit and onScreenSizeChanged will
            // be called by the same thread (the UI / main thread) so no synchronisation is needed.
            if (mController != null) {
                mController.start();
            } else {
                GLKLogger.error("GLKActivity: onInit (TTS): Cannot start terp as the controller object is null,");
                finish();
            }
        }
    }

    /**
     * If the text to speech engine is enabled and initialised, this command
     * causes it to stop talking.
     */
    public void silence() {
        if (mTts == null) {
            return;
        }
        mTts.stop();
    }

    /**
     * If the text to speech engine is enabled and initialised, queue the
     * supplied text to be read aloud.
     *
     * @param text - text to read aloud.
     */
    public void speakText(String text) {
        if (mTts == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTts.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        } else {
            //noinspection deprecation
            mTts.speak(text, TextToSpeech.QUEUE_ADD, null);
        }
    }

    /**
     * Called when an activity you launched exits, giving you the requestCode you started
     * it with, the resultCode it returned, and any additional data from it. The
     * resultCode will be RESULT_CANCELED if the activity explicitly returned that,
     * didn't return any result, or crashed during its operation.
     * <p>
     * You will receive this call immediately before onResume() when your activity is re-starting.
     * <p>
     * This method is never invoked if your activity sets noHistory to true.
     *
     * @param requestCode - The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this result came from.
     * @param resultCode  - The integer result code returned by the child activity through its setResult().
     * @param data        - An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        if (requestCode == CHECK_TTS_DATA) {
            // As per recommendation on official Android developer's blog at
            // https://android-developers.googleblog.com/2009/09/introduction-to-text-to-speech-in.html
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance
                mTts = new TextToSpeech(this, this);
            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }
}
