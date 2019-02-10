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
package com.luxlunae.fabularium;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.luxlunae.fabularium.create.ProjectSelectorFragment;
import com.luxlunae.fabularium.explore.FileListAdapter;
import com.luxlunae.fabularium.explore.FileSelectorFragment;
import com.luxlunae.fabularium.play.GameSelectorFragment;
import com.luxlunae.glk.GLKConstants;

import java.io.File;
import java.io.IOException;

/**
 * Fabularium's main entry class.
 */
public class MainActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback, FileListAdapter.ToolbarListener, AsyncProgressFragment.CopyAssetsCallback {
    public final static String GAME_PATH = "com.luxlunae.fabularium.GAME_PATH";
    public final static String GAME_FORMAT = "com.luxlunae.fabularium.GAME_FORMAT";
    public final static String GAME_IFID = "com.luxlunae.fabularium.GAME_IFID";
    public final static String USER_PREFS = "com.luxlunae.fabularium.USER_PREFS";
    public final static String DEFAULT_BITMAP = "com.luxlunae.fabularium.DEFAULT_BITMAP";
    public static final int DB_LOADER = 0;
    public static final int DB_LOADER_PROJECTS = 1;
    public static final int POS_TAB_PLAY = 0;
    public static final int POS_TAB_EXPLORE = 1;
    public static final int POS_TAB_CREATE = 2;
    private static final boolean DEBUG_LIFECYCLE = false;
    private static final int REQUEST_EXT_STORAGE = 0;
    private static final int NUM_TABS = 3;
    private int mTabCurPos = POS_TAB_PLAY;

    private ScreenSlidePagerAdapter mAdapter;
    private ViewPager mPager;
    private Toolbar mToolbar;

    private CheckBox mToolbarCheckBox;
    private Button mToolbarButOK;
    private Button mToolbarButCancel;

    @NonNull
    private TOOLBAR_MODE mToolbarMode = TOOLBAR_MODE.SELECT_FILES;
    @Nullable
    private CompoundButton.OnCheckedChangeListener mCheckboxListener;
    private int mNumSelected = 0;

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
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        FabLogger.initialise(this);
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("MainActivity: onCreate()");
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        // Ensure we have the permissions we need
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // runtime permissions apply for Android M and above only
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Explain why we need the permission
                FabLogger.warn("Displaying external storage permission rationale to provide additional context.");

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Snackbar sb = Snackbar.make(findViewById(R.id.mainContent),
                        R.string.permission_rationale_extstorage, Snackbar.LENGTH_INDEFINITE);
                sb.setAction(R.string.dialog_ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXT_STORAGE);
                    }
                });
                sb.show();
            } else {
                // No explanation needed, just request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_EXT_STORAGE);
            }
        } else {
            copyCoreAssets();
            createTabStrip();
        }
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
            FabLogger.debug("MainActivity: onPause()");
        }
        FabLogger.flush();
        super.onPause();
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
            FabLogger.debug("MainActivity: onStop()");
        }
        FabLogger.shutdown();
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
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("MainActivity: onDestroy()");
        }
        FabLogger.shutdown();
        super.onDestroy();
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXT_STORAGE) {
            FabLogger.debug("Received response for external storage permission request.");
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                FabLogger.debug("External storage permission granted.");
                copyCoreAssets();
                createTabStrip();
            } else {
                FabLogger.debug("External storage permission NOT granted.");
                Toast.makeText(this,
                        R.string.permission_extstorage_denied_app_exit,
                        Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onBackPressed() {
        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("MainActivity: onBackPressed()");
            FabLogger.debug("MainActivity: # fragments before = " + getSupportFragmentManager().getFragments().size());
        }
        boolean ret = false;

        if (mTabCurPos == POS_TAB_EXPLORE) {
            // send the back key through to the explore fragment
            Fragment frag = (Fragment) mAdapter.instantiateItem(mPager, POS_TAB_EXPLORE);
            if (frag instanceof FileSelectorFragment) {
                ret = ((FileSelectorFragment) frag).onBackPressed();
            }
        }

        if (DEBUG_LIFECYCLE) {
            FabLogger.debug("MainActivity: # fragments after = " + getSupportFragmentManager().getFragments().size());
        }

        if (!ret) {
            super.onBackPressed();
        }
    }

    @Override
    public void onCopyAssetsPostExecute(@Nullable File[] results) {

    }

    public void copyCoreAssets() {
        String[] assetDirs;
        try {
            assetDirs = new String[]{"/inform/lib", GLKConstants.getDir(this, GLKConstants.SUBDIR.LIB_INFORM),
                    "/inform/include", GLKConstants.getDir(this, GLKConstants.SUBDIR.INCLUDE_INFORM),
                    "/tads3/lib", GLKConstants.getDir(this, GLKConstants.SUBDIR.LIB_TADS3),
                    "/tads3/include", GLKConstants.getDir(this, GLKConstants.SUBDIR.INCLUDE_TADS3),
                    "/adrift/StandardLibrary.amf", GLKConstants.getDir(this, GLKConstants.SUBDIR.LIB_ADRIFT).concat("StandardLibrary.amf"),
                    "/fab.ini", GLKConstants.getDir(this, null).concat("fab.ini"),
                    "/keyboards.ini", GLKConstants.getDir(this, null).concat("keyboards.ini")};

            // (Re-)create and populate any missing asset directories
            AsyncProgressFragment f = new AsyncProgressFragment();
            getSupportFragmentManager().beginTransaction().add(f, "frag_copyassets").commitAllowingStateLoss();
            f.copyAssets(getResources(), this, getResources().getAssets(), assetDirs);
        } catch (@NonNull IOException | SecurityException e) {
            Toast.makeText(this,
                    "Could not create one or more assets. Please check you have granted file read/write permissions and try again. If you have overridden any default paths in your settings, also check Fabularium can read/write to those paths.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void createTabStrip() {
        // Set up the tab strip
        mAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager = findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (mTabCurPos == POS_TAB_EXPLORE && position != POS_TAB_EXPLORE) {
                    setToolbarMode(TOOLBAR_MODE.SELECT_FILES);
                    setToolbarSelected(0, 0);
                    Fragment frag = (Fragment) mAdapter.instantiateItem(mPager, POS_TAB_EXPLORE);
                    if (frag instanceof FileSelectorFragment) {
                        ((FileSelectorFragment) frag).selectAll(false);
                    }
                }
                mTabCurPos = position;
            }
        });
        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mPager);

        // Set up the toolbar
        mToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);
        mToolbarCheckBox = findViewById(R.id.my_toolbar_chkbox);
        mToolbarButOK = findViewById(R.id.my_toolbar_butOK);
        mToolbarButCancel = findViewById(R.id.my_toolbar_butCancel);

        // Initialise default preferences
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
    }

    @NonNull
    public Fragment getTab(int pos) {
        return (Fragment) mAdapter.instantiateItem(mPager, pos);
    }

    public void setToolbarCheckBoxListener(@Nullable CompoundButton.OnCheckedChangeListener listener) {
        if (mToolbarCheckBox != null) {
            mToolbarCheckBox.setOnCheckedChangeListener(listener);
            mCheckboxListener = listener;
        }
    }

    public void setToolbarOKListener(@Nullable View.OnClickListener listener) {
        if (mToolbarButOK != null) {
            mToolbarButOK.setOnClickListener(listener);
        }
    }

    public void setToolbarCancelListener(@Nullable View.OnClickListener listener) {
        if (mToolbarButCancel != null) {
            mToolbarButCancel.setOnClickListener(listener);
        }
    }

    public void setToolbarSelected(int numSelected, int maxItems) {
        if (mToolbarMode == TOOLBAR_MODE.SELECT_FILES && mToolbarCheckBox != null) {
            if (mNumSelected != numSelected) {
                if (numSelected > 0) {
                    // change toolbar's menu to show for selected items
                    mToolbarCheckBox.setVisibility(View.VISIBLE);
                    setToolbarTitle(String.valueOf(numSelected) + " selected");
                } else {
                    mToolbarCheckBox.setVisibility(View.GONE);
                    setToolbarTitle(getString(R.string.app_name));
                }
                mNumSelected = numSelected;
                invalidateOptionsMenu();
            }

            // update the checkbox, but temporarily disable any listener
            // to ensure we don't have a cascading effect
            if (mCheckboxListener != null) {
                mToolbarCheckBox.setOnCheckedChangeListener(null);
            }
            mToolbarCheckBox.setChecked(maxItems > 0 && numSelected == maxItems);
            if (mCheckboxListener != null) {
                mToolbarCheckBox.setOnCheckedChangeListener(mCheckboxListener);
            }
        }
    }

    public int getToolbarSelected() {
        return mNumSelected;
    }

    @NonNull
    public TOOLBAR_MODE getToolbarMode() {
        return mToolbarMode;
    }

    public void setToolbarMode(@NonNull TOOLBAR_MODE mode) {
        if (mToolbarMode != mode && mToolbar != null &&
                mToolbarCheckBox != null && mToolbarButOK != null && mToolbarButCancel != null) {
            if (mode == TOOLBAR_MODE.COPY_FILES || mode == TOOLBAR_MODE.MOVE_FILES) {
                // show paste buttons
                mToolbar.setVisibility(View.VISIBLE);
                mToolbarButOK.setVisibility(View.VISIBLE);
                mToolbarButCancel.setVisibility(View.VISIBLE);
                mToolbarCheckBox.setVisibility(View.GONE);
                setToolbarTitle("");
            } else {
                // show normal select toolbar
                mToolbar.setVisibility(View.VISIBLE);
                mToolbarButOK.setVisibility(View.GONE);
                mToolbarButCancel.setVisibility(View.GONE);
                setToolbarTitle(getString(R.string.app_name));
            }
            mToolbarMode = mode;
        }
    }

    private void setToolbarTitle(@NonNull String text) {
        ActionBar bar = getSupportActionBar();
        if (bar != null && mToolbar != null) {
            if (text.equals("")) {
                bar.setDisplayShowTitleEnabled(false);
                mToolbar.setSubtitle("");
            } else {
                bar.setDisplayShowTitleEnabled(true);
            }
            mToolbar.setTitle(text);
        }
    }

    public enum TOOLBAR_MODE {
        SELECT_FILES,
        COPY_FILES,
        MOVE_FILES
    }

    private static class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment f;
            switch (position) {
                case POS_TAB_PLAY:
                default:
                    f = new GameSelectorFragment();
                    break;
                case POS_TAB_EXPLORE:
                    f = new FileSelectorFragment();
                    break;
                case POS_TAB_CREATE:
                    f = new ProjectSelectorFragment();
                    break;
            }
            return f;
        }

        @Override
        public int getCount() {
            return NUM_TABS;
        }

        @NonNull
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case POS_TAB_PLAY:
                default:
                    return "PLAY";
                case POS_TAB_EXPLORE:
                    return "EXPLORE";
                case POS_TAB_CREATE:
                    return "CREATE";
            }
        }
    }
}
