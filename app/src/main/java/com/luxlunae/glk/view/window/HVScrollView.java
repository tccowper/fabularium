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
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ScrollingView;
import android.util.TypedValue;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.AnimationUtils;
import android.widget.EdgeEffect;
import android.widget.FrameLayout;
import android.widget.OverScroller;

import com.luxlunae.fabularium.R;

import static android.support.v4.widget.ViewDragHelper.INVALID_POINTER;

// A view that can scroll its content both horizontally and vertically
// Modified code from ScrollView and HorizontalScrollView
public class HVScrollView extends FrameLayout implements ScrollingView {

    private static final int ANIMATED_SCROLL_GAP = 250;
    @NonNull
    private final OverScroller mScroller;
    private final int mTouchSlop;
    private final int mMinimumVelocity;
    private final int mMaximumVelocity;
    private final int mOverscrollDistance;
    private final int mOverflingDistance;
    int mHeightLessPadding;
    private int mWidthLessPadding;
    private float mVerticalScrollFactor;
    @Nullable
    private EdgeEffect mEdgeGlowTop;
    @Nullable
    private EdgeEffect mEdgeGlowBottom;
    @Nullable
    private EdgeEffect mEdgeGlowLeft;
    @Nullable
    private EdgeEffect mEdgeGlowRight;
    private long mLastScroll;
    private int mLastMotionY;
    private int mLastMotionX;
    @NonNull
    private DragType mIsBeingDragged = DragType.DRAG_NONE;
    @Nullable
    private VelocityTracker mVelocityTracker;
    private int mActivePointerId = INVALID_POINTER;
    @Nullable
    private GLKNonPairV.ScrollChild mScrollChild;

    public HVScrollView(@NonNull Context context) {
        // N.B. We must initialise this object by calling the super
        // View constructor WITH attributes. The default View constructor with Context
        // only does not call initialiseScrollbars, which means the scrollbars will never
        // awaken properly (see View source code). And there does not appear to be any
        // other sensible way to call that method after the View has been created. Odd.
        super(context, null, R.attr.scrollingStyle);

        mScroller = new OverScroller(context);
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
        mOverscrollDistance = configuration.getScaledOverscrollDistance();
        mOverflingDistance = configuration.getScaledOverflingDistance();
    }

    private static int clamp(int n, int my, int child) {
        // fixed
        // same as HorizontalScrollView
        if (my >= child || n < 0) {
            return 0;
        }
        if ((my + n) > child) {
            return child - my;
        }
        return n;
    }

    private static GradientDrawable getScrollbarDrawable(@NonNull Resources res, boolean vertical) {
        Drawable d = ResourcesCompat.getDrawable(res,
                vertical ? R.drawable.scrollbar_vertical_thumb : R.drawable.scrollbar_horizontal_thumb, null);
        if (d instanceof GradientDrawable) {
            return (GradientDrawable) d;
        }
        return null;
    }

    static void setScrollbarColors(@NonNull Resources res, boolean vertical, @ColorInt int[] colors) {
        GradientDrawable d = getScrollbarDrawable(res, vertical);
        if (d != null) {
            d.setColors(colors);
        }
    }

    static void setScrollbarSize(@NonNull Resources res, boolean vertical, int scrollbarSizePx) {
        GradientDrawable d = getScrollbarDrawable(res, vertical);
        if (d != null) {
            if (vertical) {
                d.setSize(scrollbarSizePx, 0);
            } else {
                d.setSize(0, scrollbarSizePx);
            }
        }
    }

    public static void setScrollbarGradientOrientation(@NonNull Resources res, boolean vertical, @NonNull String gradientOrientation) {
        GradientDrawable d = getScrollbarDrawable(res, vertical);
        if (d != null) {
            switch (gradientOrientation) {
                case "0":
                    d.setOrientation(GradientDrawable.Orientation.BL_TR);
                    break;
                case "1":
                    d.setOrientation(GradientDrawable.Orientation.BOTTOM_TOP);
                    break;
                case "2":
                    d.setOrientation(GradientDrawable.Orientation.BR_TL);
                    break;
                case "3":
                    d.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                    break;
                case "4":
                    d.setOrientation(GradientDrawable.Orientation.RIGHT_LEFT);
                    break;
                case "5":
                    d.setOrientation(GradientDrawable.Orientation.TL_BR);
                    break;
                case "6":
                    d.setOrientation(GradientDrawable.Orientation.TOP_BOTTOM);
                    break;
                case "7":
                    d.setOrientation(GradientDrawable.Orientation.TR_BL);
                    break;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int width = w - getPaddingLeft() - getPaddingRight();
        int height = h - getPaddingTop() - getPaddingBottom();
        if (width < 0) width = 0;
        if (height < 0) height = 0;
        mWidthLessPadding = width;
        mHeightLessPadding = height;
    }

    void setScrollChild(@Nullable GLKNonPairV.ScrollChild child) {
        mScrollChild = child;
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        // fixed
        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onMotionEvent will be called and we do the actual
         * scrolling there.
         */
        /*
        * Shortcut the most recurring case: the user is in the dragging
        * state and he is moving his finger.  We want to intercept this
        * motion.
        */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mIsBeingDragged != DragType.DRAG_NONE)) {
            return true;
        }

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDraggedY == false, otherwise the shortcut would have caught it. Check
                 * whether the user has moved far enough from his original down touch.
                 */
                /*
                * Locally do absolute value. mLastMotionY is set to the y value
                * of the down event.
                */
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);
                final int x = (int) ev.getX(pointerIndex);
                final int y = (int) ev.getY(pointerIndex);
                final int xDiff = Math.abs(x - mLastMotionX);
                final int yDiff = Math.abs(y - mLastMotionY);
                if (xDiff > mTouchSlop || yDiff > mTouchSlop) {
                    if (xDiff > mTouchSlop && yDiff > mTouchSlop) {
                        // choose the predominant direction
                        // if an exact tie, go with y
                        if (yDiff < xDiff) {
                            mLastMotionX = x;
                            mIsBeingDragged = DragType.DRAG_HORIZONTAL;
                        } else {
                            mLastMotionY = y;
                            mIsBeingDragged = DragType.DRAG_VERTICAL;
                        }
                    } else if (xDiff > mTouchSlop) {
                        mLastMotionX = x;
                        mIsBeingDragged = DragType.DRAG_HORIZONTAL;
                    } else if (yDiff > mTouchSlop) {
                        mLastMotionY = y;
                        mIsBeingDragged = DragType.DRAG_VERTICAL;
                    }
                    initVelocityTrackerIfNotExists();
                    assert mVelocityTracker != null;
                    mVelocityTracker.addMovement(ev);
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final int x = (int) ev.getX();
                final int y = (int) ev.getY();
                if (!inChild(x, y)) {
                    mIsBeingDragged = DragType.DRAG_NONE;
                    recycleVelocityTracker();
                    break;
                }

                /*
                 * Remember location of down touch.
                 * ACTION_DOWN always refers to pointer index 0.
                 */
                mLastMotionX = x;
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);

                initOrResetVelocityTracker();
                assert mVelocityTracker != null;
                mVelocityTracker.addMovement(ev);

                /*
                * If being flinged and user touches the screen, initiate drag;
                * otherwise don't.  mScroller.isFinished should be false when
                * being flinged.
                */
                if (mScroller.isFinished()) {
                    mIsBeingDragged = DragType.DRAG_NONE;
                } else {
                    mIsBeingDragged = (mScroller.getFinalX() - mScroller.getCurrX() != 0) ? DragType.DRAG_HORIZONTAL : DragType.DRAG_VERTICAL;
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                /* Release the drag */
                mActivePointerId = INVALID_POINTER;
                recycleVelocityTracker();
                if (mIsBeingDragged != DragType.DRAG_NONE) {
                    if (mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRangeX(), 0, getScrollRangeY())) {
                        postInvalidateOnAnimation();
                    }
                }
                mIsBeingDragged = DragType.DRAG_NONE;
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        /*
        * The only time we want to intercept motion events is if we are in the
        * drag mode.
        */
        return (mIsBeingDragged != DragType.DRAG_NONE);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent ev) {
        // fixed
        initVelocityTrackerIfNotExists();
        assert mVelocityTracker != null;
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: {
                if (mScrollChild == null) {
                    break;
                }
                if (!mScroller.isFinished()) {
                    mIsBeingDragged = (mScroller.getFinalX() - mScroller.getCurrX() != 0) ? DragType.DRAG_HORIZONTAL : DragType.DRAG_VERTICAL;
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }
                }

                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionX = (int) ev.getX();
                mLastMotionY = (int) ev.getY();
                mActivePointerId = ev.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE:
                if (mScrollChild == null) {
                    break;
                }
                final int activePointerIndex = ev.findPointerIndex(mActivePointerId);
                final int x = (int) ev.getX(activePointerIndex);
                final int y = (int) ev.getY(activePointerIndex);
                int deltaX = mLastMotionX - x;
                int deltaY = mLastMotionY - y;

                if ((mIsBeingDragged == DragType.DRAG_NONE) &&
                        (Math.abs(deltaX) > mTouchSlop || Math.abs(deltaY) > mTouchSlop)) {
                    final ViewParent parent = getParent();
                    if (parent != null) {
                        parent.requestDisallowInterceptTouchEvent(true);
                    }

                    int xDiff = Math.abs(deltaX);
                    int yDiff = Math.abs(deltaY);
                    if (xDiff > mTouchSlop && yDiff > mTouchSlop) {
                        // choose the predominant direction
                        // if an exact tie, go with y
                        if (yDiff < xDiff) {
                            mIsBeingDragged = DragType.DRAG_HORIZONTAL;
                        } else {
                            mIsBeingDragged = DragType.DRAG_VERTICAL;
                        }
                    } else if (xDiff > mTouchSlop) {
                        mIsBeingDragged = DragType.DRAG_HORIZONTAL;
                    } else if (yDiff > mTouchSlop) {
                        mIsBeingDragged = DragType.DRAG_VERTICAL;
                    }

                    if (mIsBeingDragged == DragType.DRAG_HORIZONTAL) {
                        if (deltaX > 0) {
                            deltaX -= mTouchSlop;
                        } else {
                            deltaX += mTouchSlop;
                        }
                        deltaY = 0;
                    } else if (mIsBeingDragged == DragType.DRAG_VERTICAL) {
                        deltaX = 0;
                        if (deltaY > 0) {
                            deltaY -= mTouchSlop;
                        } else {
                            deltaY += mTouchSlop;
                        }
                    }
                }

                if (mIsBeingDragged != DragType.DRAG_NONE) {
                    // Scroll to follow the motion event
                    final int oldX = getScrollX();
                    final int oldY = getScrollY();
                    final int rangeX = getScrollRangeX();
                    final int rangeY = getScrollRangeY();
                    final int overscrollMode = getOverScrollMode();

                    if (mIsBeingDragged == DragType.DRAG_HORIZONTAL) {
                        mLastMotionX = x;
                        final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                                (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeX > 0);
                        if (overScrollBy(deltaX, 0, getScrollX(), getScrollY(), rangeX, rangeY,
                                mOverscrollDistance, mOverscrollDistance, true)) {
                            // Break our velocity if we hit a scroll barrier.
                            mVelocityTracker.clear();
                        }
                        onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);
                        if (canOverscroll) {
                            final int pulledToX = oldX + deltaX;
                            if (pulledToX < 0) {
                                if (mEdgeGlowLeft != null) {
                                    mEdgeGlowLeft.onPull((float) deltaX / getWidth());
                                }
                                if (mEdgeGlowRight != null && !mEdgeGlowRight.isFinished()) {
                                    mEdgeGlowRight.onRelease();
                                }
                            } else if (pulledToX > rangeX) {
                                if (mEdgeGlowRight != null) {
                                    mEdgeGlowRight.onPull((float) deltaX / getWidth());
                                }
                                if (mEdgeGlowLeft != null && !mEdgeGlowLeft.isFinished()) {
                                    mEdgeGlowLeft.onRelease();
                                }
                            }
                            if (mEdgeGlowLeft != null
                                    && (!mEdgeGlowLeft.isFinished() || !mEdgeGlowRight.isFinished())) {
                                postInvalidateOnAnimation();
                            }
                        }
                    } else {
                        mLastMotionY = y;
                        final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                                (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && rangeY > 0);
                        if (overScrollBy(0, deltaY, getScrollX(), getScrollY(),
                                rangeX, rangeY, mOverscrollDistance, mOverscrollDistance, true)) {
                            // Break our velocity if we hit a scroll barrier.
                            mVelocityTracker.clear();
                        }
                        onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);
                        if (canOverscroll) {
                            final int pulledToY = oldY + deltaY;
                            if (pulledToY < 0) {
                                if (mEdgeGlowTop != null) {
                                    mEdgeGlowTop.onPull((float) deltaY / getHeight());
                                }
                                if (mEdgeGlowBottom != null && !mEdgeGlowBottom.isFinished()) {
                                    mEdgeGlowBottom.onRelease();
                                }
                            } else if (pulledToY > rangeY) {
                                if (mEdgeGlowBottom != null) {
                                    mEdgeGlowBottom.onPull((float) deltaY / getHeight());
                                }
                                if (mEdgeGlowTop != null && !mEdgeGlowTop.isFinished()) {
                                    mEdgeGlowTop.onRelease();
                                }
                            }
                            if (mEdgeGlowTop != null
                                    && (!mEdgeGlowTop.isFinished() || !mEdgeGlowBottom.isFinished())) {
                                postInvalidateOnAnimation();
                            }
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
                if (mScrollChild == null) {
                    break;
                }
                if (mIsBeingDragged != DragType.DRAG_NONE) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    int initialVelocity = (mIsBeingDragged == DragType.DRAG_HORIZONTAL) ?
                            (int) velocityTracker.getXVelocity(mActivePointerId) :
                            (int) velocityTracker.getYVelocity(mActivePointerId);
                    if ((Math.abs(initialVelocity) > mMinimumVelocity)) {
                        if (mIsBeingDragged == DragType.DRAG_HORIZONTAL) {
                            fling(-initialVelocity, 0);
                        } else {
                            fling(0, -initialVelocity);
                        }
                    } else {
                        if (mScroller.springBack(getScrollX(), getScrollY(), 0,
                                getScrollRangeX(), 0, getScrollRangeY())) {
                            postInvalidateOnAnimation();
                        }
                    }
                    mActivePointerId = INVALID_POINTER;
                    endDrag();
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (mScrollChild != null) {
                    if (mIsBeingDragged == DragType.DRAG_HORIZONTAL) {
                        // horizontal scroll
                        if (mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRangeX(), 0, getScrollRangeY())) {
                            postInvalidateOnAnimation();
                        }
                        mActivePointerId = INVALID_POINTER;
                        endDrag();
                    } else if (mIsBeingDragged == DragType.DRAG_VERTICAL) {
                        // vertical scroll
                        if (mScroller.springBack(getScrollX(), getScrollY(), 0, getScrollRangeX(), 0, getScrollRangeY())) {
                            postInvalidateOnAnimation();
                        }
                        mActivePointerId = INVALID_POINTER;
                        endDrag();
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN: {
                // vertical only
                final int index = ev.getActionIndex();
                mLastMotionY = (int) ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                mLastMotionY = (int) ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }
        return true;
    }

    private void endDrag() {
        // fixed
        recycleVelocityTracker();
        if (mIsBeingDragged == DragType.DRAG_HORIZONTAL) {
            if (mEdgeGlowLeft != null) {
                mEdgeGlowLeft.onRelease();
            }
            if (mEdgeGlowRight != null) {
                mEdgeGlowRight.onRelease();
            }
        } else if (mIsBeingDragged == DragType.DRAG_VERTICAL) {
            if (mEdgeGlowTop != null) {
                mEdgeGlowTop.onRelease();
            }
            if (mEdgeGlowBottom != null) {
                mEdgeGlowBottom.onRelease();
            }
        }
        mIsBeingDragged = DragType.DRAG_NONE;
    }

    private void onSecondaryPointerUp(@NonNull MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>
                MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = (int) ev.getX(newPointerIndex);
            mLastMotionY = (int) ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        if (disallowIntercept) {
            recycleVelocityTracker();
        }
        super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }

    private void initOrResetVelocityTracker() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        } else {
            mVelocityTracker.clear();
        }
    }

    private boolean inChild(int x, int y) {
        // checked
        if (mScrollChild != null) {
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            return !(y < getTop() - scrollY
                    || y >= mScrollChild.getChildHeight() - scrollY
                    || x < getLeft() - scrollX
                    || x >= mScrollChild.getChildWidth() - scrollX);
        }
        return false;
    }

    @Override
    protected float getTopFadingEdgeStrength() {
        if (mScrollChild == null) {
            return 0.0f;
        }
        final int length = getVerticalFadingEdgeLength();
        if (getScrollY() < length) {
            return getScrollY() / (float) length;
        }
        return 1.0f;
    }

    @Override
    protected float getBottomFadingEdgeStrength() {
        if (mScrollChild == null) {
            return 0.0f;
        }
        final int length = getVerticalFadingEdgeLength();
        final int bottomEdge = getHeight() - getPaddingBottom();
        final int span = mScrollChild.getChildHeight() - getScrollY() - bottomEdge;
        if (span < length) {
            return span / (float) length;
        }
        return 1.0f;
    }

    @Override
    protected float getLeftFadingEdgeStrength() {
        if (mScrollChild == null) {
            return 0.0f;
        }
        final int length = getHorizontalFadingEdgeLength();
        if (getScrollX() < length) {
            return getScrollX() / (float) length;
        }
        return 1.0f;
    }

    @Override
    protected float getRightFadingEdgeStrength() {
        if (mScrollChild == null) {
            return 0.0f;
        }
        final int length = getHorizontalFadingEdgeLength();
        final int rightEdge = getWidth() - getPaddingRight();
        final int span = mScrollChild.getChildWidth() - getScrollX() - rightEdge;
        if (span < length) {
            return span / (float) length;
        }
        return 1.0f;
    }

    private float getHorizontalScrollFactor() {
        // This is from View class, we need to reimplement because it isn't exposed to descendants
        // TODO: Should use something else.
        return getVerticalScrollFactor();
    }

    /**
     * Gets a scale factor that determines the distance the view should scroll
     * vertically in response to {@link MotionEvent#ACTION_SCROLL}.
     *
     * @return The vertical scroll scale factor.
     */
    private float getVerticalScrollFactor() {
        // This is from View class, we need to reimplement because it isn't exposed to descendants
        if (mVerticalScrollFactor == 0) {
            TypedValue outValue = new TypedValue();
            if (!getContext().getTheme().resolveAttribute(android.R.attr.listPreferredItemHeight, outValue, true)) {
                throw new IllegalStateException(
                        "Expected theme to define listPreferredItemHeight.");
            }
            mVerticalScrollFactor = outValue.getDimension(
                    getContext().getResources().getDisplayMetrics());
        }
        return mVerticalScrollFactor;
    }

    @Override
    public boolean onGenericMotionEvent(@NonNull MotionEvent event) {
        // fixed
        if ((event.getSource() & InputDevice.SOURCE_CLASS_POINTER) != 0) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL: {
                    if (mIsBeingDragged == DragType.DRAG_NONE) {
                        final float hscroll = event.getAxisValue(MotionEvent.AXIS_HSCROLL);
                        final float vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
                        if (hscroll != 0) {
                            final int delta = (int) (hscroll * getHorizontalScrollFactor());
                            final int range = getScrollRangeX();
                            int oldScrollX = getScrollX();
                            int newScrollX = oldScrollX + delta;
                            if (newScrollX < 0) {
                                newScrollX = 0;
                            } else if (newScrollX > range) {
                                newScrollX = range;
                            }
                            if (newScrollX != oldScrollX) {
                                super.scrollTo(newScrollX, getScrollY());
                                return true;
                            }
                        }
                        if (vscroll != 0) {
                            final int delta = (int) (vscroll * getVerticalScrollFactor());
                            final int range = getScrollRangeY();
                            int oldScrollY = getScrollY();
                            int newScrollY = oldScrollY - delta;
                            if (newScrollY < 0) {
                                newScrollY = 0;
                            } else if (newScrollY > range) {
                                newScrollY = range;
                            }
                            if (newScrollY != oldScrollY) {
                                super.scrollTo(getScrollX(), newScrollY);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (super.performAccessibilityAction(action, arguments)) {
            return true;
        }
        if (!isEnabled()) {
            return false;
        }
        switch (action) {
            case AccessibilityNodeInfo.ACTION_SCROLL_FORWARD: {
                final int viewportHeight = mHeightLessPadding;
                final int targetScrollY = Math.min(getScrollY() + viewportHeight, getScrollRangeY());
                if (targetScrollY != getScrollY()) {
                    smoothScrollTo(0, targetScrollY);
                    return true;
                }
            }
            return false;
            case AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD: {
                final int viewportHeight = mHeightLessPadding;
                final int targetScrollY = Math.max(getScrollY() - viewportHeight, 0);
                if (targetScrollY != getScrollY()) {
                    smoothScrollTo(0, targetScrollY);
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(@NonNull AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(GLKNonPairV.class.getName());
        if (isEnabled()) {
            final int scrollRangeY = getScrollRangeY();
            if (scrollRangeY > 0) {
                info.setScrollable(true);
                if (getScrollY() > 0) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                }
                if (getScrollY() < scrollRangeY) {
                    info.addAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                }
            }
        }
    }

    @Override
    public void onInitializeAccessibilityEvent(@NonNull AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(GLKNonPairV.class.getName());
        final boolean scrollable = (getScrollRangeX() > 0 || getScrollRangeY() > 0);
        event.setScrollable(scrollable);
        event.setScrollX(getScrollX());
        event.setScrollY(getScrollY());
        event.setMaxScrollX(getScrollRangeX());
        event.setMaxScrollY(getScrollRangeY());
    }

    @Override
    public void setOverScrollMode(int mode) {
        // fixed
        if (mode != OVER_SCROLL_NEVER) {
            if (mEdgeGlowLeft == null) {
                Context context = getContext();
                mEdgeGlowLeft = new EdgeEffect(context);
                mEdgeGlowRight = new EdgeEffect(context);
            }
            if (mEdgeGlowTop == null) {
                Context context = getContext();
                mEdgeGlowTop = new EdgeEffect(context);
                mEdgeGlowBottom = new EdgeEffect(context);
            }
        } else {
            mEdgeGlowLeft = null;
            mEdgeGlowRight = null;
            mEdgeGlowTop = null;
            mEdgeGlowBottom = null;
        }
        super.setOverScrollMode(mode);
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            int oldX = getScrollX();
            int oldY = getScrollY();
            int x = mScroller.getCurrX();
            int y = mScroller.getCurrY();

            if (oldX != x || oldY != y) {
                final int rangeX = getScrollRangeX();
                final int rangeY = getScrollRangeY();
                final int overscrollMode = getOverScrollMode();
                final boolean canOverscroll = overscrollMode == OVER_SCROLL_ALWAYS ||
                        (overscrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && (rangeX > 0 || rangeY > 0));

                overScrollBy(x - oldX, y - oldY, oldX, oldY, rangeX, rangeY,
                        mOverflingDistance, mOverflingDistance, false);
                onScrollChanged(getScrollX(), getScrollY(), oldX, oldY);

                if (canOverscroll) {
                    if (x < 0 && oldX >= 0) {
                        if (mEdgeGlowLeft != null) {
                            mEdgeGlowLeft.onAbsorb((int) mScroller.getCurrVelocity());
                        }
                    } else if (x > rangeX && oldX <= rangeX) {
                        if (mEdgeGlowRight != null) {
                            mEdgeGlowRight.onAbsorb((int) mScroller.getCurrVelocity());
                        }
                    }
                    if (y < 0 && oldY >= 0) {
                        if (mEdgeGlowTop != null) {
                            mEdgeGlowTop.onAbsorb((int) mScroller.getCurrVelocity());
                        }
                    } else if (y > rangeY && oldY <= rangeY) {
                        if (mEdgeGlowBottom != null) {
                            mEdgeGlowBottom.onAbsorb((int) mScroller.getCurrVelocity());
                        }
                    }
                }
            }

            if (!awakenScrollBars()) {
                // Keep on drawing until the animation has finished.
                postInvalidateOnAnimation();
            }
        }
    }

    @Override
    public int computeHorizontalScrollRange() {
        final int contentWidth = mWidthLessPadding;
        if (mScrollChild == null) {
            return contentWidth;
        }

        int scrollRangeX = mScrollChild.getChildWidth();
        final int scrollX = getScrollX();
        final int overscrollRight = Math.max(0, scrollRangeX - contentWidth);
        if (scrollX < 0) {
            scrollRangeX -= scrollX;
        } else if (scrollX > overscrollRight) {
            scrollRangeX += scrollX - overscrollRight;
        }
        return scrollRangeX;
    }

    @Override
    public int computeHorizontalScrollOffset() {
        return Math.max(0, super.computeHorizontalScrollOffset());
    }

    @Override
    public int computeHorizontalScrollExtent() {
        return mWidthLessPadding;
    }

    @Override
    public int computeVerticalScrollRange() {
        final int contentHeight = mHeightLessPadding;
        if (mScrollChild == null) {
            return contentHeight;
        }

        int scrollRangeY = mScrollChild.getChildHeight();
        final int scrollY = getScrollY();
        final int overscrollBottom = Math.max(0, scrollRangeY - contentHeight);
        if (scrollY < 0) {
            scrollRangeY -= scrollY;
        } else if (scrollY > overscrollBottom) {
            scrollRangeY += scrollY - overscrollBottom;
        }
        return scrollRangeY;
    }

    @Override
    public int computeVerticalScrollOffset() {
        return Math.max(0, super.computeVerticalScrollOffset());
    }

    @Override
    public int computeVerticalScrollExtent() {
        return mHeightLessPadding;
    }

    @Override
    public void scrollTo(int x, int y) {
        // fixed
        // basically same as HorizontalScrollView
        // we rely on the fact the View.scrollBy calls scrollTo.
        if (mScrollChild != null) {
            x = clamp(x, mWidthLessPadding, mScrollChild.getChildWidth());
            y = clamp(y, mHeightLessPadding, mScrollChild.getChildHeight());
            if (x != getScrollX() || y != getScrollY()) {
                super.scrollTo(x, y);
            }
        }
    }

    private void smoothScrollBy(int dx, int dy) {
        if (mScrollChild == null) {
            // Nothing to do.
            return;
        }
        long duration = AnimationUtils.currentAnimationTimeMillis() - mLastScroll;
        if (duration > ANIMATED_SCROLL_GAP) {
            final int width = mWidthLessPadding;
            final int height = mHeightLessPadding;
            final int right = mScrollChild.getChildWidth();
            final int bottom = mScrollChild.getChildHeight();
            final int maxX = Math.max(0, right - width);
            final int maxY = Math.max(0, bottom - height);
            final int scrollX = getScrollX();
            final int scrollY = getScrollY();
            dx = Math.max(0, Math.min(scrollX + dx, maxX)) - scrollX;
            dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;

            mScroller.startScroll(scrollX, scrollY, dx, dy);
            postInvalidateOnAnimation();
        } else {
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            scrollBy(dx, dy);
        }
        mLastScroll = AnimationUtils.currentAnimationTimeMillis();
    }

    private void smoothScrollTo(int x, int y) {
        smoothScrollBy(x - getScrollX(), y - getScrollY());
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY,
                                  boolean clampedX, boolean clampedY) {
        // fixed
        // Treat animating scrolls differently; see #computeScroll() for why.
        if (!mScroller.isFinished()) {
            setScrollX(scrollX);
            setScrollY(scrollY);
            // invalidateParentIfNeeded();
            if (clampedX) {
                mScroller.springBack(scrollX, scrollY, 0, getScrollRangeX(), 0, getScrollRangeY());
            }
            if (clampedY) {
                mScroller.springBack(scrollX, scrollY, 0, getScrollRangeX(), 0, getScrollRangeY());
            }
        } else {
            super.scrollTo(scrollX, scrollY);
        }
        awakenScrollBars();
    }

    private int getScrollRangeY() {
        return (mScrollChild != null) ?
                Math.max(0, mScrollChild.getChildHeight() - mHeightLessPadding) : 0;
    }

    private int getScrollRangeX() {
        return (mScrollChild != null) ?
                Math.max(0, mScrollChild.getChildWidth() - mWidthLessPadding) : 0;
    }

    private void fling(int velocityX, int velocityY) {
        // fixed
        if (mScrollChild != null) {
            int width = mWidthLessPadding;
            int right = mScrollChild.getChildWidth();
            int height = mHeightLessPadding;
            int bottom = mScrollChild.getChildHeight();

            mScroller.fling(getScrollX(), getScrollY(), velocityX, velocityY, 0, Math.max(0, right - width),
                    0, Math.max(0, bottom - height), width / 2, height / 2);

            postInvalidateOnAnimation();
        }
    }

    private enum DragType {DRAG_NONE, DRAG_HORIZONTAL, DRAG_VERTICAL}

    interface ScrollChild {
        int getChildWidth();

        int getChildHeight();
    }
}
