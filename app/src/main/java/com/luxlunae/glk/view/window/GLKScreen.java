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
package com.luxlunae.glk.view.window;

import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.luxlunae.glk.GLKActivity;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.GLKStreamManager;
import com.luxlunae.glk.model.stream.window.GLKGraphicsM;
import com.luxlunae.glk.model.stream.window.GLKPairM;
import com.luxlunae.glk.model.stream.window.GLKTextBufferM;
import com.luxlunae.glk.model.stream.window.GLKTextGridM;
import com.luxlunae.glk.model.stream.window.GLKWindowM;

import java.util.ArrayList;

import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_LAYOUT_CHANGED;
import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_PARENT_CHANGED;

public class GLKScreen extends LinearLayout {
    private static final boolean DEBUG_VIEW_OPS = false;

    private static final LayoutParams mRootParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    private static final int UPDATE_UI = -1;
    private static final int SHUTDOWN = -3;
    private static final Object mUILock = new Object();
    private static final GlkUIMsgHandler mUIHandler = new GlkUIMsgHandler();
    private static final SparseArray<GLKWindowV> mWindows = new SparseArray<>();
    private boolean mScreenSizeChanged = false;
    @Nullable
    private GLKWindowV mRootWin = null;
    @Nullable
    private GLKScreenResizeCallback mResizeCallback = null;
    private int mLMargin = 0;
    private int mRMargin = 0;
    private int mTMargin = 0;
    private int mBMargin = 0;
    private int mScreenWidthPX = 0;
    private int mScreenHeightPX = 0;
    private int mWinFocusID = GLKConstants.NULL;
    private boolean mShowingSoftKeyboard = false;

    public GLKScreen(@NonNull Context context) {
        super(context);
        initialise();
    }

    public GLKScreen(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
        initialise();
    }

    static GLKWindowV getViewForModel(@NonNull GLKWindowM winM) {
        // if the model doesn't have an associated view yet this
        // will return null
        return mWindows.get(winM.getStreamId());
    }

    public static @NonNull
    String promptUserForFilePath(@NonNull final GLKModel m, int fmode, int fileType) throws InterruptedException {
        // show dialog and block until we get a response
        final StringBuilder result = new StringBuilder();
        GLKFileDialog newFragment = GLKFileDialog.newInstance(m, fileType, fmode);
        newFragment.setListener(new GLKFileDialog.GLKFileDialogListener() {
            @Override
            public void onDialogFinish(@Nullable String selPath) {
                if (selPath != null) {
                    result.append(selPath);
                }
                synchronized (mUILock) {
                    mUILock.notifyAll();
                }
            }
        });

        FragmentManager fm = m.getFragmentManager();
        if (fm != null) {
            newFragment.show(fm, "File Selector");
            synchronized (mUILock) {
                mUILock.wait();
            }
        } else {
            GLKLogger.warn("GLKScreen: cannot show file prompt as have lost fragment reference.");
        }
        return result.toString();
    }

    private static void detachParent(@NonNull GLKWindowV winV) {
        View v = (View) winV;
        ViewParent pp = v.getParent();
        if (pp != null && pp instanceof LinearLayout) {
            ((LinearLayout) pp).removeView(v);
        }
    }

    private static void detachChildren(@NonNull GLKWindowV winV) {
        if (winV instanceof LinearLayout) {
            ((LinearLayout) winV).removeAllViews();
        }
    }

    @Nullable
    public GLKWindowV getFocusedWindow() {
        return mWindows.get(mWinFocusID);
    }

    public void postShutdown() throws InterruptedException {
        Message msg = mUIHandler.obtainMessage();
        msg.what = SHUTDOWN;
        msg.obj = this;
        postMessageToUIAndWait(msg);
    }

    public void postUpdate(@NonNull GLKModel m) throws InterruptedException {
        // updates the displayed components using the View thread,
        // worker thread block until action fully complete
        Message msg = mUIHandler.obtainMessage();
        msg.what = UPDATE_UI;
        msg.obj = new GlkUIArgs(m, this);
        postMessageToUIAndWait(msg);
    }

    /**
     * Post a message to the UI thread (should only ever be called
     * by the worker thread).  This function blocks the worker thread
     * until the UI thread has finished processing.
     *
     * @param msg - the message to post.
     */
    private void postMessageToUIAndWait(@NonNull Message msg) throws InterruptedException {
        synchronized (mUILock) {
            mUIHandler.sendMessage(msg);
            mUILock.wait();
        }
    }

    private void initialise() {
        setBackgroundColor(GLKConstants.EMPTY_SCREEN_COLOR);
        setId(GLKConstants.NULL);
        setKeepScreenOn(true);
    }

    public void setResizeCallback(@Nullable GLKScreenResizeCallback callback) {
        mResizeCallback = callback;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mScreenWidthPX = w - mLMargin - mRMargin;
        mScreenHeightPX = h - mTMargin - mBMargin;

        GLKLogger.debug("GLKScreen: screen size (less margins) changed to " + mScreenWidthPX + " x " + mScreenHeightPX);
        if (mResizeCallback != null) {
            mResizeCallback.onScreenSizeChanged(mScreenWidthPX, mScreenHeightPX, oldw, oldh);
            //mResizeCallback = null;
        } else {
            mScreenSizeChanged = true;
        }
    }

    public void setWindowMargins(@NonNull Rect margins) {
        mRootParams.setMargins(margins.left, margins.top, margins.right, margins.bottom);
        mLMargin = margins.left;
        mTMargin = margins.top;
        mRMargin = margins.right;
        mBMargin = margins.bottom;
    }

    private void setRootWindow(@Nullable GLKWindowV rootWin) {
        // if 'mRootWin' is NULL this means to close all existing windows
        if (rootWin == null) {
            removeView((View) mRootWin);
        } else {
            if (mRootWin != null) {
                removeView((View) mRootWin);
            }
            mRootWin = rootWin;
            ((View) mRootWin).setLayoutParams(mRootParams);
            addView((View) mRootWin);
        }
    }

    public void shutdown() {
        // should only ever be called on UI thread
        GLKLogger.debug("Shutting down View...");

        if (mShowingSoftKeyboard) {
            // try to hide the soft keyboard, if it's displayed
            // unfortunately, Android doesn't provide a way to detect if the software keyboard is displayed
            // so we just assume it's on, if the user has disabled system keyboard and is not using a hardware key
            // keyboard :-)
            // See https://groups.google.com/forum/#!topic/android-platform/FyjybyM0wGA
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }

        mRootWin = null;
        mWindows.clear();
        setKeepScreenOn(false);
        removeAllViews();
    }

    private void updateFrom(@NonNull GLKModel m) {
        // We follow the same general strategy as that outlined by Andrew Plotkin in his note -
        // "Threading in GLK libraries" (http://www.eblong.com/zarf/glk/threading-notes.html)
        // N.B. this function should only ever be called on the UI thread, when the worker thread
        // is locked and waiting.
        GLKStreamManager mgr = m.mStreamMgr;

        if (DEBUG_VIEW_OPS) {
            GLKLogger.debug("GLKWindowManager: updating the View to reflect this window structure:");
            GLKLogger.debug(mgr.getWindowInfo());
        }

        // 1. close any UI windows whose VM windows have disappeared
        //    N.B. we can't remove the items in the same loop that
        //    we're iterating in
        if (DEBUG_VIEW_OPS) {
            GLKLogger.debug("GLKWindowManager: view component currently has " + mWindows.size() + " window mappings.");
        }
        ArrayList<Integer> toRemove = new ArrayList<>();
        for (int i = 0, sz = mWindows.size(); i < sz; i++) {
            int winID = mWindows.keyAt(i);
            GLKWindowV winV = mWindows.valueAt(i);
            if (winV != null && mgr.getWindow(winID) == null) {
                // doesn't exist anymore
                detachParent(winV);
                detachChildren(winV);
                toRemove.add(winID);
            }
        }
        for (int id : toRemove) {
            if (DEBUG_VIEW_OPS) {
                GLKLogger.debug("GLKWindowManager: closing win " + id);
            }
            mWindows.remove(id);
        }

        // 2. create new UI windows for any VM windows that don't have them yet
        ArrayList<Integer> toAddID = new ArrayList<>();
        ArrayList<GLKWindowV> toAddWin = new ArrayList<>();
        GLKWindowM win = mgr.getNextWindow(null);
        mShowingSoftKeyboard = !m.mUsingHardwareKeyboard && !m.mUseBuiltinKeyboard;
        while (win != null) {
            int winID = win.getStreamId();
            if (mWindows.get(winID) == null) {
                // window doesn't exist - create it
                GLKWindowV newWin = null;
                if (win instanceof GLKPairM) {
                    if (DEBUG_VIEW_OPS) {
                        GLKLogger.debug("GLKWindowManager: open pair " + winID);
                    }
                    newWin = new GLKPairV(getContext(), (GLKPairM) win, m.mBorderColor, m.mBorderWidthPX, m.mBorderHeightPX);
                } else if (win instanceof GLKTextBufferM) {
                    if (DEBUG_VIEW_OPS) {
                        GLKLogger.debug("GLKWindowManager: open buffer " + winID);
                    }
                    newWin = new GLKTextBufferV(getContext(), (GLKTextBufferM) win, this,
                            m.mVScrollbarColorsOverride ? m.mVScrollbarColors : null,
                            m.mHScrollbarColorsOverride ? m.mHScrollbarColors : null,
                            m.mVScrollbarWidthPx, m.mHScrollbarHeightPx, m.mSyncScreenBG);
                    Rect r = ((GLKTextBufferM) win).getPadding();
                    ((View) newWin).setPadding(r.left, r.top, r.right, r.bottom);
                    ((GLKTextBufferV) newWin).initHandler();
                    ((GLKTextBufferV) newWin).setShowSystemKeyboardOnTouch(mShowingSoftKeyboard);
                } else if (win instanceof GLKTextGridM) {
                    if (DEBUG_VIEW_OPS) {
                        GLKLogger.debug("GLKWindowManager: open grid " + winID);
                    }
                    newWin = new GLKTextGridV(getContext(), (GLKTextGridM) win);
                    Rect r = ((GLKTextGridM) win).getPadding();
                    ((View) newWin).setPadding(r.left, r.top, r.right, r.bottom);
                    ((GLKTextGridV) newWin).initHandler();
                    ((GLKTextGridV) newWin).setShowSystemKeyboardOnTouch(mShowingSoftKeyboard);
                } else if (win instanceof GLKGraphicsM) {
                    if (DEBUG_VIEW_OPS) {
                        GLKLogger.debug("GLKWindowManager: open graphics " + winID);
                    }
                    newWin = new GLKGraphicsV(getContext(), (GLKGraphicsM) win);
                    ((GLKGraphicsV) newWin).initHandler();
                } else {
                    GLKLogger.error("GLKWindowManager: unknown window type - will not open " + winID);
                }
                if (newWin != null) {
                    toAddID.add(winID);
                    toAddWin.add(newWin);
                }
            }
            win = mgr.getNextWindow(win);
        }
        for (int i = 0, sz = toAddID.size(); i < sz; i++) {
            mWindows.put(toAddID.get(i), toAddWin.get(i));
        }

        // Check if root window dimensions changed on UI side; if so
        // recompute sizes in the VM model
        if (mScreenSizeChanged) {
            m.setScreenSize(new Point(mScreenWidthPX, mScreenHeightPX));
            mScreenSizeChanged = false;
        }

        // Clear any queued text to speech playback (assuming the user only wants to hear
        // most recent text and wouldn't have generated key events to move onwards otherwise)
        Context c = getContext();
        if (c instanceof GLKActivity) {
            ((GLKActivity) c).silence();
        }

        for (int i = 0, sz = mWindows.size(); i < sz; i++) {
            GLKWindowV winV = mWindows.valueAt(i);

            if (winV != null) {
                GLKWindowM mo = winV.getModel();

                // 3. recompute UI window sizes based on VM window split info
                if (mo.isFlagSet(FLAG_PARENT_CHANGED) || mo.isFlagSet(FLAG_LAYOUT_CHANGED)) {
                    // update layout params
                    ViewGroup.LayoutParams lp = mo.getLayoutParams();
                    if (lp != null) {
                        ((View) winV).setLayoutParams(mo.getLayoutParams());
                    }
                    mo.clearFlag(FLAG_LAYOUT_CHANGED);

                    // remove this window from any existing parent in the UI
                    detachParent(winV);

                    // add it to the new parent
                    GLKPairM p = mo.getParent();
                    if (p == null) {
                        // this is the root view
                        if (DEBUG_VIEW_OPS) {
                            GLKLogger.debug("GLKWindowManager: set root win to " + winV.getStreamId());
                        }
                        setRootWindow(winV);
                    } else {
                        GLKWindowV wv = mWindows.get(p.getStreamId());
                        if (wv != null && wv instanceof GLKPairV) {
                            GLKPairV pv = (GLKPairV) wv;
                            GLKWindowM c1 = p.getChild1();
                            GLKWindowM c2 = p.getChild2();
                            GLKWindowV winV2 = null;
                            boolean winVIsChild1 = false;

                            if (c1.getStreamId() == winV.getStreamId()) {
                                winVIsChild1 = true;
                                winV2 = mWindows.get(c2.getStreamId());
                            } else if (c2.getStreamId() == winV.getStreamId()) {
                                winVIsChild1 = false;
                                winV2 = mWindows.get(c1.getStreamId());
                            }

                            if (winV2 == null) {
                                GLKLogger.error("GLKWindowManager: error: internal inconsistency in model!");
                            } else {
                                GLKWindowM mSib = winV2.getModel();
                                ViewGroup.LayoutParams lp2 = mSib.getLayoutParams();
                                if (lp2 != null) {
                                    ((View) winV2).setLayoutParams(mSib.getLayoutParams());
                                }
                                mSib.clearFlag(FLAG_LAYOUT_CHANGED);

                                if (DEBUG_VIEW_OPS) {
                                    GLKLogger.debug("GLKWindowManager: set children of win " + pv.getStreamId() +
                                            " to " + (winVIsChild1 ? winV.getStreamId() : winV2.getStreamId()) + " and " +
                                            (winVIsChild1 ? winV2.getStreamId() : winV.getStreamId()));
                                }

                                detachParent(winV2);

                                pv.setOrientation(p.getOrientation());
                                pv.removeAllViews();
                                pv.addView((View) (winVIsChild1 ? winV : winV2));
                                pv.addView((View) (winVIsChild1 ? winV2 : winV));
                                pv.invalidate();

                                mSib.clearFlag(FLAG_PARENT_CHANGED);
                            }
                        } else {
                            GLKLogger.error("GLKWindowManager: error: couldn't find parent window for win " + winV.getStreamId());
                        }
                    }
                    mo.clearFlag(FLAG_PARENT_CHANGED);
                }

                // 4. update the contents of each UI window to match the updates stored in the
                //    matching VM window.
                winV.updateContents();

                // 5. update the input state of each UI window to match the matching VM window
                if (winV instanceof GLKNonPairV) {
                    ((GLKNonPairV) winV).updateInputState();
                }
            }
        }

        // Process any change in window focus
        // Currently we set this every update to ensure that system keyboards (which change
        // focus to other windows) work properly
        // if (m.mFocusedChanged) {
        mWinFocusID = m.mWinFocusID;
        GLKWindowV focusWinV = mWindows.get(mWinFocusID);
        if (focusWinV != null) {
            if (DEBUG_VIEW_OPS) {
                GLKLogger.debug("GLKWindowManager: requesting focus for window " + mWinFocusID);
            }
            ((View) focusWinV).requestFocus();
        }
        //      m.mFocusedChanged = false;
        //  }
    }

    public interface GLKScreenResizeCallback {
        void onScreenSizeChanged(int w, int h, int oldw, int oldh);
    }

    private static class GlkUIArgs {
        @NonNull
        final GLKModel mModel;
        @NonNull
        final GLKScreen mScreen;

        GlkUIArgs(@NonNull GLKModel m, @NonNull GLKScreen scr) {
            mModel = m;
            mScreen = scr;
        }
    }

    /**
     * The GLKUIMsgHandler handles communications from the GLKModel to
     * the View.
     */
    private static class GlkUIMsgHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case UPDATE_UI: {
                    GlkUIArgs args = (GlkUIArgs) msg.obj;
                    args.mScreen.updateFrom(args.mModel);
                    break;
                }

                case SHUTDOWN: {
                    GLKScreen scr = (GLKScreen) msg.obj;
                    scr.shutdown();
                    break;
                }
            }

            // Tell the worker thread that UI operation is now complete and it can continue on
            synchronized (mUILock) {
                mUILock.notify();
            }
        }
    }
}
