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

import android.content.Context;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;

import com.luxlunae.glk.model.GLKDrawable;
import com.luxlunae.glk.model.stream.window.GLKNonPairM;
import com.luxlunae.glk.model.stream.window.GLKWindowM;

import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_BG_COLOR_CHANGED;
import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_BG_IMAGE_CHANGED;

public abstract class GLKNonPairV extends HVScrollView
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, GLKWindowV {

    @NonNull
    private final GLKNonPairM mModel;
    private final int mStreamID;
    int mPaddingLeft = 0;
    int mPaddingTop = 0;
    int mPaddingRight = 0;
    int mPaddingBottom = 9;
    private GestureDetectorCompat mDetector;
    private boolean mLineRequested = false;
    private boolean mCharRequested = false;

    /**
     * Creates a new NonPair window, with the given parameters.
     */
    GLKNonPairV(@NonNull Context c, @NonNull GLKNonPairM model) {
        super(c);
        mModel = model;
        mStreamID = mModel.getStreamId();
        setFocusable(true);
        setFocusableInTouchMode(true);
        setWillNotDraw(false);
    }

    @Override
    public int getStreamId() {
        return mStreamID;
    }

    @NonNull
    @Override
    public GLKWindowM getModel() {
        return mModel;
    }

    public boolean lineRequested() {
        return mLineRequested;
    }

    public boolean charRequested() {
        return mCharRequested;
    }

    @CallSuper
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        // cache the padding info so descendants like GLKTextBufferM
        // don't have to repeatedly keep calling getPaddingLeft(), etc,
        super.setPadding(left, top, right, bottom);
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;
    }

    public void initHandler() {
        mDetector = new GestureDetectorCompat(getContext(), this);
    }

    @CallSuper
    public void updateContents() {
        if (mModel.isFlagSet(FLAG_BG_COLOR_CHANGED)) {
            setBackgroundColor(mModel.getBGColor());
            mModel.clearFlag(FLAG_BG_COLOR_CHANGED);
        }
        if (mModel.isFlagSet(FLAG_BG_IMAGE_CHANGED)) {
            GLKDrawable img = mModel.getBGImage();
            if (img != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    setBackground(img);
                } else {
                    //noinspection deprecation
                    setBackgroundDrawable(img);
                }
            }
            mModel.clearFlag(FLAG_BG_IMAGE_CHANGED);
        }
        mLineRequested = mModel.lineRequested();
        mCharRequested = mModel.charRequested();
    }

    /**
     * Notified when a tap occurs with the down MotionEvent that triggered it. This
     * will be triggered immediately for every down event. All other events should be preceded by this.
     *
     * @param e - The down motion event.
     * @return true if the event is consumed, else false
     */
    @Override
    public boolean onDown(MotionEvent e) {
        // Dummy method to be overridden as appropriate by descendant classes
        // We default to true, consistent with Android developer recommendation:
        //
        //   "Whether or not you use GestureDetector.OnGestureListener, it's best practice to
        //   implement an onDown() method that returns true. This is because all gestures begin
        //   with an onDown() message. If you return false from onDown() ... the system assumes
        //   that you want to ignore the rest of the gesture, and the other methods of
        //   GestureDetector.OnGestureListener never get called. This has the potential to
        //   cause unexpected problems in your app. The only time you should return false
        //   from onDown() is if you truly want to ignore an entire gesture."
        return true;
    }

    /**
     * Notified when a scroll occurs with the initial on down MotionEvent and the current
     * move MotionEvent. The distance in x and y is also supplied for convenience.
     *
     * @param e1        - The first down motion event that started the scrolling.
     * @param e2        - The move motion event that triggered the current onScroll.
     * @param distanceX - The distance along the X axis that has been scrolled since the
     *                  last call to onScroll. This is NOT the distance between e1 and e2.
     * @param distanceY - The distance along the Y axis that has been scrolled since the
     *                  last call to onScroll. This is NOT the distance between e1 and e2.
     * @return true if the event is consumed, else false
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // By default, scrolling is handled directly in onTouchEvent(), so don't do anything
        // here. In general, descendants should not override this method.
        return true;
    }

    /**
     * Notified of a fling event when it occurs with the initial on down MotionEvent
     * and the matching up MotionEvent. The calculated velocity is supplied along the x and y axis in pixels per second.
     *
     * @param e1        - The first down motion event that started the fling.
     * @param e2        - The move motion event that triggered the current onFling.
     * @param velocityX - The velocity of this fling measured in pixels per second along the x axis.
     * @param velocityY - The velocity of this fling measured in pixels per second along the y axis.
     * @return true if the event is consumed, else false
     */
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // By default, scrolling is handled directly in onTouchEvent(), so don't do anything
        // here. In general, descendants should not override this method.
        return true;
    }

    /**
     * Notified when a long press occurs with the initial on down
     * MotionEvent that triggered it.
     *
     * @param e - The initial on down motion event that started the longpress.
     */
    @Override
    public void onLongPress(MotionEvent e) {
        // dummy method to be overridden as appropriate by descendant classes
    }

    /**
     * The user has performed a down MotionEvent and not performed a move or up
     * yet. This event is commonly used to provide visual feedback to the user
     * to let them know that their action has been recognized i.e. highlight an element.
     *
     * @param e - The down motion event.
     */
    @Override
    public void onShowPress(MotionEvent e) {
        // dummy method to be overridden as appropriate by descendant classes
    }

    /**
     * Notified when an event within a double-tap gesture occurs, including
     * the down, move, and up events.
     *
     * @param e - The motion event that occurred during the double-tap gesture.
     * @return true if the event is consumed, else false.
     */
    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    /**
     * Notified when a double-tap occurs.
     *
     * @param e - The down motion event of the first tap of the double-tap.
     * @return true if the event is consumed, else false.
     */
    @Override
    public boolean onDoubleTap(MotionEvent e) {
        // dummy method to be overridden as appropriate by descendant classes
        return false;
    }

    /**
     * Notified when a single-tap occurs.
     * <p>
     * Unlike onSingleTapUp(MotionEvent), this will only be
     * called after the detector is confident that the user's
     * first tap is not followed by a second tap leading to a double-tap gesture.
     *
     * @param e - The down motion event of the single-tap.
     * @return true if the event is consumed, else false
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // dummy method to be overridden as appropriate by descendant classes
        return false;
    }

    /**
     * Implement this method to handle touch screen motion events.
     *
     * @param ev - The motion event.
     * @return True if the event was handled, false otherwise.
     */
    @CallSuper
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        super.onTouchEvent(ev);
        mDetector.onTouchEvent(ev);
        return true;
    }

    @Override
    public void announceForAccessibility(@NonNull CharSequence text) {
        // we override this function to ensure that an announcement is still
        // made irrespective of whether this view has a parent or not
        AccessibilityManager manager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (manager != null && manager.isEnabled()) {
            AccessibilityEvent e = AccessibilityEvent.obtain();
            e.setEventType(AccessibilityEvent.TYPE_ANNOUNCEMENT);
            e.setClassName(getClass().getName());
            e.setPackageName(getContext().getPackageName());
            e.getText().add(text);
            manager.sendAccessibilityEvent(e);
        }
    }

    abstract void updateInputState();
}
