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

package com.luxlunae.glk.view.keyboard;

/*
 * Copyright (C) 2008-2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKActivity;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.view.keyboard.GLKKeyboard.Key;
import com.luxlunae.glk.view.window.GLKScreen;

import java.util.List;

import static android.content.Context.ACCESSIBILITY_SERVICE;

/**
 * A view that renders a virtual {@link GLKKeyboard}. It handles rendering of keys and
 * detecting key presses and touch movements.
 * <p>
 * R.styleable#AdjustedKeyboardView_keyBackground
 * R.styleable#AdjustedKeyboardView_keyPreviewLayout
 * R.styleable#AdjustedKeyboardView_keyPreviewOffset
 * R.styleable#AdjustedKeyboardView_labelTextSize
 * R.styleable#AdjustedKeyboardView_keyTextSize
 * R.styleable#AdjustedKeyboardView_keyTextColor
 * R.styleable#AdjustedKeyboardView_verticalCorrection
 * R.styleable#AdjustedKeyboardView_popupLayout
 */
public class GLKKeyboardView extends View implements View.OnClickListener {
    public static final int NOT_A_KEY = -1;    // The same definition as in the parent class (which was also private)

    private static final boolean DEBUG = false;
    private static final int[] LONG_PRESSABLE_STATE_SET = {R.attr.state_long_pressable};
    private static final int MSG_SHOW_PREVIEW = 1;
    private static final int MSG_REMOVE_PREVIEW = 2;
    private static final int MSG_REPEAT = 3;
    private static final int MSG_LONGPRESS = 4;
    private static final int DELAY_BEFORE_PREVIEW = 0;
    private static final int DELAY_AFTER_PREVIEW = 70;
    private static final int DEBOUNCE_TIME = 70;
    private static final int REPEAT_INTERVAL = 50; // ~20 keys per second
    private static final int REPEAT_START_DELAY = 400;
    private static final int LONGPRESS_TIMEOUT = ViewConfiguration.getLongPressTimeout();
    private static final int MULTITAP_INTERVAL = 800; // milliseconds
    private static final int PRIMARY_LABEL_Y_OFFSET = 5;  // in SP
    private static final int HINT_TEXT_SIZE = 13;      // in SP (assume key text size = 18 SP)
    private static final float HINT_X_WIDTH_RATIO = 1f / 2f;
    private static final float HINT_Y_HEIGHT_RATIO = 1f / 7f;

    // Working variable
    private final int[] mCoordinates = new int[2];
    private final Paint mHintPaint = new Paint();
    private final int mHintTextSize;
    protected Key[] mKeys;
    Handler mHandler;
    private GLKKeyboard mKeyboard;
    private int mCurrentKeyIndex = NOT_A_KEY;
    private int mLabelTextSize;
    private int mKeyTextSize;
    private int mKeyTextColor;
    private float mShadowRadius;
    private int mShadowColor;
    private float mBackgroundDimAmount;
    private TextView mPreviewText;
    private PopupWindow mPreviewPopup;
    private int mPreviewTextSizeLarge;
    private int mPreviewOffset;
    private int mPreviewHeight;
    private PopupWindow mPopupKeyboard;
    private boolean mMiniKeyboardOnScreen;
    private View mPopupParent;
    private int mMiniKeyboardOffsetX;
    private int mMiniKeyboardOffsetY;
    private int mVerticalCorrection;
    private int mProximityThreshold;
    private boolean mPreviewCentered = false;
    private boolean mShowPreview = true;
    private boolean mShowTouchPoints = true;
    private int mPopupPreviewX;
    private int mPopupPreviewY;
    private int mLastX;
    private int mLastY;
    private int mStartX;
    private int mStartY;
    private Paint mPaint;
    private Rect mPadding;
    private long mDownTime;
    private long mLastMoveTime;
    private int mLastKey;
    private int mLastCodeX;
    private int mLastCodeY;
    private int mCurrentKey = NOT_A_KEY;
    private int mDownKey = NOT_A_KEY;
    private long mLastKeyTime;
    private long mCurrentKeyTime;
    private GestureDetector mGestureDetector;
    private int mPopupX;
    private int mPopupY;
    private int mRepeatKeyIndex = NOT_A_KEY;
    private int mPopupLayout;
    private boolean mAbortKey;
    @Nullable
    private Key mInvalidatedKey;
    @NonNull
    private Rect mClipRegion = new Rect(0, 0, 0, 0);
    private boolean mPossiblePoly;
    @NonNull
    private SwipeTracker mSwipeTracker = new SwipeTracker();
    private int mSwipeThreshold;
    private boolean mDisambiguateSwipe;
    // Variables for dealing with multiple pointers
    private int mOldPointerCount = 1;
    private float mOldPointerX;
    private float mOldPointerY;
    @Nullable
    private Drawable mKeyBackground;
    // For multi-tap
    private int mLastSentIndex;
    private int mTapCount;
    private long mLastTapTime;
    private boolean mInMultiTap;
    @NonNull
    private StringBuilder mPreviewLabel = new StringBuilder(1);
    /**
     * Whether the keyboard bitmap needs to be redrawn before it's blitted.
     **/
    private boolean mDrawPending;
    /**
     * The dirty region in the keyboard bitmap
     */
    @NonNull
    private Rect mDirtyRect = new Rect();
    /**
     * The keyboard bitmap for faster updates
     */
    @Nullable
    private Bitmap mBuffer;
    /**
     * Notes if the keyboard just changed, so that we could possibly reallocate the mBuffer.
     */
    private boolean mKeyboardChanged;
    /**
     * The canvas for the above mutable keyboard bitmap
     */
    @Nullable
    private Canvas mCanvas;
    /**
     * The accessibility manager for accessibility support
     */
    @Nullable
    private AccessibilityManager mAccessibilityManager;
    @Nullable
    private GLKKeyboardMapping[] mKeyboardMappings;
    private GLKScreen mScreen;
    private int mCurKeyboard = 0;
    private GLKKeyboardTouchHelper mGLKKeyboardTouchHelper;

    public GLKKeyboardView(@NonNull Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.keyboardViewStyle);
    }

    public GLKKeyboardView(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GLKKeyboardView(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);
        TypedArray a = context.obtainStyledAttributes(
                attrs, R.styleable.GLKKeyboardView, defStyleAttr, defStyleRes);
        LayoutInflater inflate =
                (LayoutInflater) context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        int previewLayout = 0;
        int keyTextSize = 0;
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            switch (attr) {
                case R.styleable.GLKKeyboardView_keyBackground:
                    mKeyBackground = a.getDrawable(attr);
                    break;
                case R.styleable.GLKKeyboardView_verticalCorrection:
                    mVerticalCorrection = a.getDimensionPixelOffset(attr, 0);
                    break;
                case R.styleable.GLKKeyboardView_keyPreviewLayout:
                    previewLayout = a.getResourceId(attr, 0);
                    break;
                case R.styleable.GLKKeyboardView_keyPreviewOffset:
                    mPreviewOffset = a.getDimensionPixelOffset(attr, 0);
                    break;
                case R.styleable.GLKKeyboardView_keyPreviewHeight:
                    mPreviewHeight = a.getDimensionPixelSize(attr, 80);
                    break;
                case R.styleable.GLKKeyboardView_keyTextSize:
                    mKeyTextSize = a.getDimensionPixelSize(attr, 18);
                    break;
                case R.styleable.GLKKeyboardView_keyTextColor:
                    mKeyTextColor = a.getColor(attr, 0xFF000000);
                    break;
                case R.styleable.GLKKeyboardView_labelTextSize:
                    mLabelTextSize = a.getDimensionPixelSize(attr, 14);
                    break;
                case R.styleable.GLKKeyboardView_popupLayout:
                    mPopupLayout = a.getResourceId(attr, 0);
                    break;
                case R.styleable.GLKKeyboardView_shadowColor:
                    mShadowColor = a.getColor(attr, 0);
                    break;
                case R.styleable.GLKKeyboardView_shadowRadius:
                    mShadowRadius = a.getFloat(attr, 0f);
                    break;
            }
        }
        a.recycle();

        // a = getContext().obtainStyledAttributes(R.styleable.AdjustedKeyboardView);
        //  mBackgroundDimAmount = a.getFloat(R.styleable.Theme_backgroundDimAmount, 0.5f);
        mBackgroundDimAmount = 0.5f;
        mPreviewPopup = new PopupWindow(context);
        if (previewLayout != 0 && inflate != null) {
            mPreviewText = (TextView) inflate.inflate(previewLayout, null);
            mPreviewTextSizeLarge = (int) mPreviewText.getTextSize();
            mPreviewPopup.setContentView(mPreviewText);
            mPreviewPopup.setBackgroundDrawable(null);
        } else {
            mShowPreview = false;
        }
        mPreviewPopup.setTouchable(false);
        mPopupKeyboard = new PopupWindow(context);
        mPopupKeyboard.setBackgroundDrawable(null);
        mPopupParent = this;
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setTextSize(keyTextSize);
        mPaint.setTextAlign(Align.CENTER);
        mPaint.setAlpha(255);
        mPadding = new Rect(0, 0, 0, 0);
        mKeyBackground.getPadding(mPadding);
        mSwipeThreshold = (int) (500 * getResources().getDisplayMetrics().density);
        mDisambiguateSwipe = getResources().getBoolean(R.bool.config_swipeDisambiguation);
        mAccessibilityManager = (AccessibilityManager) context.getSystemService(ACCESSIBILITY_SERVICE);
        resetMultiTap();

        final DisplayMetrics dm = getResources().getDisplayMetrics();

        setPreviewEnabled(false);

        mHintTextSize = GLKUtils.spToPx(HINT_TEXT_SIZE, dm);
        mHintPaint.setAntiAlias(true);
        mHintPaint.setTextSize(mHintTextSize);
        mHintPaint.setTextAlign(Paint.Align.CENTER);
        mHintPaint.setAlpha(255);
        mHintPaint.setColor(Color.rgb(150, 150, 150));
        mHintPaint.setTypeface(Typeface.DEFAULT);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        initGestureDetector();
        if (mHandler == null) {
            mHandler = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    switch (msg.what) {
                        case MSG_SHOW_PREVIEW:
                            showKey(msg.arg1);
                            break;
                        case MSG_REMOVE_PREVIEW:
                            mPreviewText.setVisibility(INVISIBLE);
                            break;
                        case MSG_REPEAT:
                            if (repeatKey()) {
                                Message repeat = Message.obtain(this, MSG_REPEAT);
                                sendMessageDelayed(repeat, REPEAT_INTERVAL);
                            }
                            break;
                        case MSG_LONGPRESS:
                            openPopupIfRequired((MotionEvent) msg.obj);
                            break;
                    }
                }
            };
        }
    }

    public void moveToNextKeyboard() {
        mCurKeyboard++;
        if (mKeyboardMappings != null) {
            int n = mKeyboardMappings.length;
            if (n > 1) {
                mCurKeyboard %= n;
                resetKeyboard();
            }
        }
    }

    /**
     * Send the codes of a particular key to the GLK window system.
     *
     * @param keyCodes - all of the codes, including the first code at position 0
     */
    public void dispatchOnKey(@NonNull int[] keyCodes) {
        if (keyCodes.length == 0) {
            // nothing to do
            return;
        }

        if (mScreen != null) {
            GLKActivity act = GLKUtils.getActivityForView(mScreen);
            if (act != null) {
                act.sendKeyCodes(keyCodes, null);
            }
        }
    }

    public void dispatchOnPress(int primaryCode) {
    }

    public void dispatchOnRelease(int primaryCode) {
    }

    public void dispatchSwipeDown() {
    }

    public void dispatchSwipeLeft() {
    }

    public void dispatchSwipeRight() {
    }

    public void dispatchSwipeUp() {
    }

    public void setView(GLKScreen scr) {
        mScreen = scr;
    }

    private void initGestureDetector() {
        if (mGestureDetector == null) {
            mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(@NonNull MotionEvent me1, @NonNull MotionEvent me2,
                                       float velocityX, float velocityY) {
                    if (mPossiblePoly) return false;
                    final float absX = Math.abs(velocityX);
                    final float absY = Math.abs(velocityY);
                    float deltaX = me2.getX() - me1.getX();
                    float deltaY = me2.getY() - me1.getY();
                    int travelX = getWidth() / 2; // Half the keyboard width
                    int travelY = getHeight() / 2; // Half the keyboard height
                    mSwipeTracker.computeCurrentVelocity(1000);
                    final float endingVelocityX = mSwipeTracker.getXVelocity();
                    final float endingVelocityY = mSwipeTracker.getYVelocity();
                    boolean sendDownKey = false;
                    if (velocityX > mSwipeThreshold && absY < absX && deltaX > travelX) {
                        if (mDisambiguateSwipe && endingVelocityX < velocityX / 4) {
                            sendDownKey = true;
                        } else {
                            dispatchSwipeRight();
                            return true;
                        }
                    } else if (velocityX < -mSwipeThreshold && absY < absX && deltaX < -travelX) {
                        if (mDisambiguateSwipe && endingVelocityX > velocityX / 4) {
                            sendDownKey = true;
                        } else {
                            dispatchSwipeLeft();
                            return true;
                        }
                    } else if (velocityY < -mSwipeThreshold && absX < absY && deltaY < -travelY) {
                        if (mDisambiguateSwipe && endingVelocityY > velocityY / 4) {
                            sendDownKey = true;
                        } else {
                            dispatchSwipeUp();
                            return true;
                        }
                    } else if (velocityY > mSwipeThreshold && absX < absY / 2 && deltaY > travelY) {
                        if (mDisambiguateSwipe && endingVelocityY < velocityY / 4) {
                            sendDownKey = true;
                        } else {
                            dispatchSwipeDown();
                            return true;
                        }
                    }
                    if (sendDownKey) {
                        detectAndSendKey(mDownKey, me1.getEventTime());
                    }
                    return false;
                }
            });
            mGestureDetector.setIsLongpressEnabled(false);
        }
    }

    /**
     * Returns the current keyboard being displayed by this view.
     *
     * @return the currently attached keyboard
     * @see #setKeyboard(GLKKeyboard)
     */
    public GLKKeyboard getKeyboard() {
        return mKeyboard;
    }

    /**
     * Attaches a keyboard to this view. The keyboard can be switched at any time and the
     * view will re-layout itself to accommodate the keyboard.
     *
     * @param keyboard the keyboard to display in this view
     * @see GLKKeyboard
     * @see #getKeyboard()
     */
    public void setKeyboard(@NonNull GLKKeyboard keyboard) {
        if (mKeyboard != null) {
            showPreview(NOT_A_KEY);
        }
        // Remove any pending messages
        removeMessages();
        mKeyboard = keyboard;
        List<Key> keys = mKeyboard.getKeys();
        mKeys = keys.toArray(new Key[keys.size()]);
        requestLayout();
        // Hint to reallocate the buffer if the size changed
        mKeyboardChanged = true;
        invalidateAllKeys();
        computeProximityThreshold();
        // Switching to a different keyboard should abort any pending keys so that the key up
        // doesn't get delivered to the old or new keyboard
        mAbortKey = true; // Until the next ACTION_DOWN

        // See
        // https://storage.googleapis.com/google-code-archive-downloads/v2/code.google.com/eyes-free/501%20-%20Enabling%20Blind%20and%20Low-Vision%20Accessibility%20on%20Android.pdf
        mGLKKeyboardTouchHelper = new GLKKeyboardTouchHelper(this, mKeyboard);
        ViewCompat.setAccessibilityDelegate(this, mGLKKeyboardTouchHelper);
    }

    /**
     * Sets the state of the shift key of the keyboard, if any.
     *
     * @param shifted whether or not to enable the state of the shift key
     * @return true if the shift key state changed, false if there was no change
     * @see GLKKeyboardView#isShifted()
     */
    public boolean setShifted(boolean shifted) {
        if (mKeyboard != null) {
            if (mKeyboard.setShifted(shifted)) {
                // The whole keyboard probably needs to be redrawn
                invalidateAllKeys();
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the state of the shift key of the keyboard, if any.
     *
     * @return true if the shift is in a pressed state, false otherwise. If there is
     * no shift key on the keyboard or there is no keyboard attached, it returns false.
     * @see GLKKeyboardView#setShifted(boolean)
     */
    public boolean isShifted() {
        return mKeyboard != null && mKeyboard.isShifted();
    }

    /**
     * Returns the enabled state of the key feedback popup.
     *
     * @return whether or not the key feedback popup is enabled
     * @see #setPreviewEnabled(boolean)
     */
    public boolean isPreviewEnabled() {
        return mShowPreview;
    }

    /**
     * Enables or disables the key feedback popup. This is a popup that shows a magnified
     * version of the depressed key. By default the preview is enabled.
     *
     * @param previewEnabled whether or not to enable the key feedback popup
     * @see #isPreviewEnabled()
     */
    public void setPreviewEnabled(boolean previewEnabled) {
        mShowPreview = previewEnabled;
    }

    public void setPopupParent(View v) {
        mPopupParent = v;
    }

    public void setPopupOffset(int x, int y) {
        mMiniKeyboardOffsetX = x;
        mMiniKeyboardOffsetY = y;
        if (mPreviewPopup.isShowing()) {
            mPreviewPopup.dismiss();
        }
    }

    public void setKeyboardMappings(@Nullable GLKKeyboardMapping[] mappings) {
        // one mapping for each screen of the keyboard
        // can pass null to revert to default mappings
        if (mappings == null) {
            // Use defaults
            mKeyboardMappings = new GLKKeyboardMapping[2];

            // keyboard 1
            GLKKeyboardMapping mapping = new GLKKeyboardMapping();
            mKeyboardMappings[0] = mapping;
            mapping.mXmlLayoutResId = R.xml.keyboard_10x4;
            mapping.mHintLabels = new CharSequence[]
                    {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0",
                            "@", "#", "$", "%", "^", "&", "*", "(", ")",
                            null, "/", "\\", "~", "!", "?", "\'", "\"", null,
                            null, null, "."};
            mapping.mHintCodes = new int[][]
                    {{49}, {50}, {51}, {52}, {53}, {54}, {55}, {56}, {57}, {48},
                            {64}, {35}, {36}, {37}, {94}, {38}, {42}, {40}, {41},
                            null, {47}, {92}, {126}, {33}, {63}, {39}, {34}, null,
                            null, null, {46}};

            // keyboard 2
            mapping = new GLKKeyboardMapping();
            mKeyboardMappings[1] = mapping;
            mapping.mXmlLayoutResId = R.xml.keyboard_10x4;
            mapping.mKeyLabels = new CharSequence[]
                    {"Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P",
                            "A", "S", "D", "F", "G", "H", "J", "K", "L",
                            null, "Z", "X", "C", "V", "B", "N", "M", null,
                            null, "FN", ",", null, null, null, null};
            mapping.mKeyCodes = new int[][]
                    {{81}, {87}, {69}, {82}, {84}, {89}, {85}, {73}, {79}, {80},
                            {65}, {83}, {68}, {70}, {71}, {72}, {74}, {75}, {76},
                            null, {90}, {88}, {67}, {86}, {66}, {78}, {77}, null,
                            null, {GLKConstants.FAB_KEY_NEXT}, {44, 32}, null, null, null, null
                    };
            mapping.mHintLabels = new CharSequence[]
                    {"ESC", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8", "F9",
                            "-", "_", "+", "=", "{", "}", "[", "]", "|",
                            null, ":", ";", "<", ">", "DBG", null, null, null, null, null, "."};
            mapping.mHintCodes = new int[][]
                    {{GLKConstants.keycode_Escape}, {GLKConstants.keycode_Func1},
                            {GLKConstants.keycode_Func2}, {GLKConstants.keycode_Func3},
                            {GLKConstants.keycode_Func4}, {GLKConstants.keycode_Func5},
                            {GLKConstants.keycode_Func6}, {GLKConstants.keycode_Func7},
                            {GLKConstants.keycode_Func8}, {GLKConstants.keycode_Func9},
                            {45}, {95}, {43}, {61}, {123}, {125}, {91}, {93}, {124},
                            null, {58}, {59}, {60}, {62}, {GLKConstants.FAB_KEY_DEBUG}, null, null, null, null, null, {46}};
        } else {
            mKeyboardMappings = mappings;
        }
    }

    public void resetKeyboard() {
        Context c = getContext();
        if (c != null && mKeyboardMappings != null && mCurKeyboard < mKeyboardMappings.length) {
            GLKKeyboardMapping mapping = mKeyboardMappings[mCurKeyboard];
            setKeyboard(new GLKKeyboard(getContext(), mapping));
        }
    }

    /**
     * Popup keyboard close button clicked.
     */
    public void onClick(View v) {
        dismissPopupKeyboard();
    }

    @Nullable
    private CharSequence adjustCase(@Nullable CharSequence label) {
        if (mKeyboard.isShifted() && label != null && label.length() < 3
                && Character.isLowerCase(label.charAt(0))) {
            label = label.toString().toUpperCase();
        }
        return label;
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Round up a little
        if (mKeyboard == null) {
            setMeasuredDimension(getPaddingLeft() + getPaddingRight(),
                    getPaddingTop() + getPaddingBottom());
        } else {
            int width = mKeyboard.getMinWidth() + getPaddingLeft() + getPaddingRight();
            if (MeasureSpec.getSize(widthMeasureSpec) < width + 10) {
                width = MeasureSpec.getSize(widthMeasureSpec);
            }
            setMeasuredDimension(width, mKeyboard.getHeight() + getPaddingTop() + getPaddingBottom());
        }
    }

    /**
     * Compute the average distance between adjacent keys (horizontally and vertically)
     * and square it to get the proximity threshold. We use a square here and in computing
     * the touch distance from a key's center to avoid taking a square root.
     */
    private void computeProximityThreshold() {
        final Key[] keys = mKeys;
        if (keys == null) return;
        int length = keys.length;
        int dimensionSum = 0;
        for (Key key : keys) {
            dimensionSum += Math.min(key.width, key.height) + key.gap;
        }
        if (dimensionSum < 0 || length == 0) return;
        mProximityThreshold = (int) (dimensionSum * 1.4f / length);
        mProximityThreshold *= mProximityThreshold; // Square it
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (mKeyboard != null) {
            mKeyboard.resize(w, h);
        }
        // Release the buffer, if any and it will be reallocated on the next draw
        mBuffer = null;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mDrawPending || mBuffer == null || mKeyboardChanged) {
            onBufferDraw();
        }
        canvas.drawBitmap(mBuffer, 0, 0, null);
    }

    private void onBufferDraw() {
        if (mBuffer == null || mKeyboardChanged) {
            if (mBuffer == null || mBuffer.getWidth() != getWidth() || mBuffer.getHeight() != getHeight()) {
                // Make sure our bitmap is at least 1x1
                final int width = Math.max(1, getWidth());
                final int height = Math.max(1, getHeight());
                mBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mCanvas = new Canvas(mBuffer);
            }
            invalidateAllKeys();
            mKeyboardChanged = false;
        }
        if (mKeyboard == null) return;

        final Canvas canvas = mCanvas;
        canvas.save();
        canvas.clipRect(mDirtyRect);

        final Paint paint = mPaint;
        final Drawable keyBackground = mKeyBackground;
        final Rect clipRegion = mClipRegion;
        final Rect padding = mPadding;
        final int kbdPaddingLeft = getPaddingLeft();
        final int kbdPaddingTop = getPaddingTop();
        final Key[] keys = mKeys;
        final Key invalidKey = mInvalidatedKey;
        paint.setColor(mKeyTextColor);
        boolean drawSingleKey = false;
        if (invalidKey != null && canvas.getClipBounds(clipRegion)) {
            // Is clipRegion completely contained within the invalidated key?
            if (invalidKey.x + kbdPaddingLeft - 1 <= clipRegion.left &&
                    invalidKey.y + kbdPaddingTop - 1 <= clipRegion.top &&
                    invalidKey.x + invalidKey.width + kbdPaddingLeft + 1 >= clipRegion.right &&
                    invalidKey.y + invalidKey.height + kbdPaddingTop + 1 >= clipRegion.bottom) {
                drawSingleKey = true;
            }
        }
        canvas.drawColor(0x00000000, PorterDuff.Mode.CLEAR);

        float x, y;
        final Paint hintPaint = mHintPaint;
        int label_y_offset = GLKUtils.spToPx(PRIMARY_LABEL_Y_OFFSET, getResources().getDisplayMetrics());

        for (final Key key : keys) {
            if (drawSingleKey && invalidKey != key) {
                continue;
            }
            int[] drawableState = key.getCurrentDrawableState();
            keyBackground.setState(drawableState);
            // Switch the character to uppercase if shift is pressed
            String label = key.label == null ? null : adjustCase(key.label).toString();
            final Rect bounds = keyBackground.getBounds();
            if (key.width != bounds.right ||
                    key.height != bounds.bottom) {
                keyBackground.setBounds(0, 0, key.width, key.height);
            }
            canvas.translate(key.x + kbdPaddingLeft, key.y + kbdPaddingTop);
            keyBackground.draw(canvas);
            if (label != null) {
                // For characters, use large font. For labels like "Done", use small font.
                if (label.length() > 1 && key.codes.length < 2) {
                    paint.setTextSize(key.labelSize != null ? key.labelSize : mLabelTextSize);
                    paint.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    paint.setTextSize(key.labelSize != null ? key.labelSize : mKeyTextSize);
                    paint.setTypeface(Typeface.DEFAULT);
                }
                // Draw a drop shadow for the text
                paint.setShadowLayer(mShadowRadius, 0, 0, mShadowColor);
                // Draw the text
                canvas.drawText(label,
                        (key.width - padding.left - padding.right) / 2
                                + padding.left,
                        (key.height - padding.top - padding.bottom) / 2
                                + (paint.getTextSize() - paint.descent()) / 2 + padding.top
                                + ((key.hintLabel != null) ? label_y_offset : 0),
                        paint);
                // Turn off drop shadow
                paint.setShadowLayer(0, 0, 0, 0);
            } else if (key.icon != null) {
                final int drawableX = (key.width - padding.left - padding.right
                        - key.icon.getIntrinsicWidth()) / 2 + padding.left;
                final int drawableY = (key.height - padding.top - padding.bottom
                        - key.icon.getIntrinsicHeight()) / 2 + padding.top;
                canvas.translate(drawableX, drawableY);
                key.icon.setBounds(0, 0,
                        key.icon.getIntrinsicWidth(), key.icon.getIntrinsicHeight());
                key.icon.draw(canvas);
                canvas.translate(-drawableX, -drawableY);
            }

            // draw any hint at top of key
            if (key.hintLabel != null) {
                // Draw the text
                hintPaint.setTextSize(key.hintLabelSize != null ? key.hintLabelSize : mHintTextSize);
                float w = (float) key.width;
                float h = (float) key.height;
                x = HINT_X_WIDTH_RATIO * w;
                y = HINT_Y_HEIGHT_RATIO * h;
                canvas.drawText(key.hintLabel.toString(),
                        x,
                        y + (hintPaint.getTextSize() - hintPaint.descent()),
                        hintPaint);
            }

            canvas.translate(-key.x - kbdPaddingLeft, -key.y - kbdPaddingTop);
        }

        mInvalidatedKey = null;

        // Overlay a dark rectangle to dim the keyboard
        if (mMiniKeyboardOnScreen) {
            paint.setColor((int) (mBackgroundDimAmount * 0xFF) << 24);
            canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
        }
        if (DEBUG && mShowTouchPoints) {
            paint.setAlpha(128);
            paint.setColor(0xFFFF0000);
            canvas.drawCircle(mStartX, mStartY, 3, paint);
            canvas.drawLine(mStartX, mStartY, mLastX, mLastY, paint);
            paint.setColor(0xFF0000FF);
            canvas.drawCircle(mLastX, mLastY, 3, paint);
            paint.setColor(0xFF00FF00);
            canvas.drawCircle((mStartX + mLastX) / 2, (mStartY + mLastY) / 2, 2, paint);
        }
        mDrawPending = false;
        mDirtyRect.setEmpty();

        canvas.restore();
    }

    public int getKeyIndex(int x, int y) {
        final Key[] keys = mKeys;
        int primaryIndex = NOT_A_KEY;
        int closestKey = NOT_A_KEY;
        int closestKeyDist = mProximityThreshold + 1;
        int[] nearestKeyIndices = mKeyboard.getNearestKeys(x, y);
        for (int nearestKeyIndice : nearestKeyIndices) {
            final Key key = keys[nearestKeyIndice];
            int dist = 0;
            boolean isInside = key.isInside(x, y);
            if (isInside) {
                primaryIndex = nearestKeyIndice;
                if (key.codes[0] > 32) {
                    // Find insertion point
                    if (dist < closestKeyDist) {
                        closestKeyDist = dist;
                        closestKey = nearestKeyIndice;
                    }
                }
            }
        }
        if (primaryIndex == NOT_A_KEY) {
            primaryIndex = closestKey;
        }
        return primaryIndex;
    }

    private void detectAndSendKey(int index, long eventTime) {
        if (index != NOT_A_KEY && index < mKeys.length) {
            final Key key = mKeys[index];
            dispatchOnKey(key.codes);
            dispatchOnRelease(key.codes[0]);
            mLastSentIndex = index;
            mLastTapTime = eventTime;
        }
    }

    /**
     * Handle multi-tap keys by producing the key label for the current multi-tap state.
     */
    @Nullable
    private CharSequence getPreviewText(@NonNull Key key) {
        if (mInMultiTap) {
            // Multi-tap
            mPreviewLabel.setLength(0);
            mPreviewLabel.append((char) key.codes[mTapCount < 0 ? 0 : mTapCount]);
            return adjustCase(mPreviewLabel);
        } else {
            return adjustCase(key.label);
        }
    }

    private void showPreview(int keyIndex) {
        int oldKeyIndex = mCurrentKeyIndex;
        final PopupWindow previewPopup = mPreviewPopup;
        mCurrentKeyIndex = keyIndex;
        // Release the old key and press the new key
        final Key[] keys = mKeys;
        if (oldKeyIndex != mCurrentKeyIndex) {
            if (oldKeyIndex != NOT_A_KEY && keys.length > oldKeyIndex) {
                Key oldKey = keys[oldKeyIndex];
                oldKey.onReleased(mCurrentKeyIndex == NOT_A_KEY);
                invalidateKey(oldKeyIndex);
                final int keyCode = oldKey.codes[0];
                sendAccessibilityEventForUnicodeCharacter(AccessibilityEvent.TYPE_VIEW_HOVER_EXIT,
                        keyCode);
                // TODO: We need to implement AccessibilityNodeProvider for this view.
                sendAccessibilityEventForUnicodeCharacter(
                        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED, keyCode);
            }
            if (mCurrentKeyIndex != NOT_A_KEY && keys.length > mCurrentKeyIndex) {
                Key newKey = keys[mCurrentKeyIndex];
                newKey.onPressed();
                invalidateKey(mCurrentKeyIndex);
                final int keyCode = newKey.codes[0];
                sendAccessibilityEventForUnicodeCharacter(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER,
                        keyCode);
                // TODO: We need to implement AccessibilityNodeProvider for this view.
                sendAccessibilityEventForUnicodeCharacter(
                        AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED, keyCode);
            }
        }
        // If key changed and preview is on ...
        if (oldKeyIndex != mCurrentKeyIndex && mShowPreview) {
            mHandler.removeMessages(MSG_SHOW_PREVIEW);
            if (previewPopup.isShowing()) {
                if (keyIndex == NOT_A_KEY) {
                    mHandler.sendMessageDelayed(mHandler
                                    .obtainMessage(MSG_REMOVE_PREVIEW),
                            DELAY_AFTER_PREVIEW);
                }
            }
            if (keyIndex != NOT_A_KEY) {
                if (previewPopup.isShowing() && mPreviewText.getVisibility() == VISIBLE) {
                    // Show right away, if it's already visible and finger is moving around
                    showKey(keyIndex);
                } else {
                    mHandler.sendMessageDelayed(
                            mHandler.obtainMessage(MSG_SHOW_PREVIEW, keyIndex, 0),
                            DELAY_BEFORE_PREVIEW);
                }
            }
        }
    }

    private void showKey(final int keyIndex) {
        final PopupWindow previewPopup = mPreviewPopup;
        final Key[] keys = mKeys;
        if (keyIndex < 0 || keyIndex >= mKeys.length) return;
        Key key = keys[keyIndex];
        if (key.icon != null) {
            mPreviewText.setCompoundDrawables(null, null, null,
                    key.iconPreview != null ? key.iconPreview : key.icon);
            mPreviewText.setText(null);
        } else {
            mPreviewText.setCompoundDrawables(null, null, null, null);
            mPreviewText.setText(getPreviewText(key));
            if (key.label.length() > 1 && key.codes.length < 2) {
                mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mKeyTextSize);
                mPreviewText.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                mPreviewText.setTextSize(TypedValue.COMPLEX_UNIT_PX, mPreviewTextSizeLarge);
                mPreviewText.setTypeface(Typeface.DEFAULT);
            }
        }
        mPreviewText.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        int popupWidth = Math.max(mPreviewText.getMeasuredWidth(), key.width
                + mPreviewText.getPaddingLeft() + mPreviewText.getPaddingRight());
        final int popupHeight = mPreviewHeight;
        LayoutParams lp = mPreviewText.getLayoutParams();
        if (lp != null) {
            lp.width = popupWidth;
            lp.height = popupHeight;
        }
        if (!mPreviewCentered) {
            mPopupPreviewX = key.x - mPreviewText.getPaddingLeft() + getPaddingLeft();
            mPopupPreviewY = key.y - popupHeight + mPreviewOffset;
        } else {
            // TODO: Fix this if centering is brought back
            mPopupPreviewX = 160 - mPreviewText.getMeasuredWidth() / 2;
            mPopupPreviewY = -mPreviewText.getMeasuredHeight();
        }
        mHandler.removeMessages(MSG_REMOVE_PREVIEW);
        getLocationInWindow(mCoordinates);
        mCoordinates[0] += mMiniKeyboardOffsetX; // Offset may be zero
        mCoordinates[1] += mMiniKeyboardOffsetY; // Offset may be zero
        // Set the preview background state
        mPreviewText.getBackground().setState(
                key.popupResId != 0 ? LONG_PRESSABLE_STATE_SET : EMPTY_STATE_SET);
        mPopupPreviewX += mCoordinates[0];
        mPopupPreviewY += mCoordinates[1];
        // If the popup cannot be shown above the key, put it on the side
        getLocationOnScreen(mCoordinates);
        if (mPopupPreviewY + mCoordinates[1] < 0) {
            // If the key you're pressing is on the left side of the keyboard, show the popup on
            // the right, offset by enough to see at least one key to the left/right.
            if (key.x + key.width <= getWidth() / 2) {
                mPopupPreviewX += (int) (key.width * 2.5);
            } else {
                mPopupPreviewX -= (int) (key.width * 2.5);
            }
            mPopupPreviewY += popupHeight;
        }
        if (previewPopup.isShowing()) {
            previewPopup.update(mPopupPreviewX, mPopupPreviewY,
                    popupWidth, popupHeight);
        } else {
            previewPopup.setWidth(popupWidth);
            previewPopup.setHeight(popupHeight);
            previewPopup.showAtLocation(mPopupParent, Gravity.NO_GRAVITY,
                    mPopupPreviewX, mPopupPreviewY);
        }
        mPreviewText.setVisibility(VISIBLE);
    }

    private void sendAccessibilityEventForUnicodeCharacter(int eventType, int code) {
        if (mAccessibilityManager.isEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
            onInitializeAccessibilityEvent(event);
            final String text;
            switch (code) {
                case GLKKeyboard.KEYCODE_ALT:
                    text = getContext().getString(R.string.keyboardview_keycode_alt);
                    break;
                case GLKKeyboard.KEYCODE_CANCEL:
                    text = getContext().getString(R.string.keyboardview_keycode_cancel);
                    break;
                case GLKKeyboard.KEYCODE_DELETE:
                    text = getContext().getString(R.string.keyboardview_keycode_delete);
                    break;
                case GLKKeyboard.KEYCODE_DONE:
                    text = getContext().getString(R.string.keyboardview_keycode_done);
                    break;
                case GLKKeyboard.KEYCODE_MODE_CHANGE:
                    text = getContext().getString(R.string.keyboardview_keycode_mode_change);
                    break;
                case GLKKeyboard.KEYCODE_SHIFT:
                    text = getContext().getString(R.string.keyboardview_keycode_shift);
                    break;
                case '\n':
                    text = getContext().getString(R.string.keyboardview_keycode_enter);
                    break;
                default:
                    text = String.valueOf((char) code);
            }
            event.getText().add(text);
            mAccessibilityManager.sendAccessibilityEvent(event);
        }
    }

    /**
     * Requests a redraw of the entire keyboard. Calling {@link #invalidate} is not sufficient
     * because the keyboard renders the keys to an off-screen buffer and an invalidate() only
     * draws the cached buffer.
     *
     * @see #invalidateKey(int)
     */
    public void invalidateAllKeys() {
        mDirtyRect.union(0, 0, getWidth(), getHeight());
        mDrawPending = true;
        invalidate();
    }

    /**
     * Invalidates a key so that it will be redrawn on the next repaint. Use this method if only
     * one key is changing it's content. Any changes that affect the position or size of the key
     * may not be honored.
     *
     * @param keyIndex the index of the key in the attached {@link GLKKeyboard}.
     * @see #invalidateAllKeys
     */
    public void invalidateKey(int keyIndex) {
        if (mKeys == null) return;
        if (keyIndex < 0 || keyIndex >= mKeys.length) {
            return;
        }
        final Key key = mKeys[keyIndex];
        mInvalidatedKey = key;
        int mPaddingLeft = getPaddingLeft();
        int mPaddingTop = getPaddingTop();
        mDirtyRect.union(key.x + mPaddingLeft, key.y + mPaddingTop,
                key.x + key.width + mPaddingLeft, key.y + key.height + mPaddingTop);
        onBufferDraw();
        invalidate(key.x + mPaddingLeft, key.y + mPaddingTop,
                key.x + key.width + mPaddingLeft, key.y + key.height + mPaddingTop);
    }

    private boolean openPopupIfRequired(MotionEvent me) {
        // Check if we have a popup layout specified first.
        /*if (mPopupLayout == 0) {
            return false;
        }*/
        if (mCurrentKey < 0 || mCurrentKey >= mKeys.length) {
            return false;
        }
        Key popupKey = mKeys[mCurrentKey];
        boolean result = onLongPress(popupKey);
        if (result) {
            mAbortKey = true;
            showPreview(NOT_A_KEY);
        }
        return result;
    }

    /**
     * Called when a key is long pressed. By default this will open any popup keyboard associated
     * with this key through the attributes popupLayout and popupCharacters.
     *
     * @param popupKey the key that was long pressed
     * @return true if the long press is handled, false otherwise. Subclasses should call the
     * method on the base class if the subclass doesn't wish to handle the call.
     */
    protected boolean onLongPress(@NonNull Key popupKey) {
        if (mAccessibilityManager.isTouchExplorationEnabled()) {
            // don't process long press events when touch exploration is enabled
            // as this can interfere with that mechanism
            return false;
        }

        if (popupKey.hintCodes != null && popupKey.hintCodes.length > 0) {
            dispatchOnKey(popupKey.hintCodes);
            return true;
        }

      /*  int popupKeyboardId = popupKey.popupResId;
        if (popupKeyboardId != 0) {
            mMiniKeyboardContainer = mMiniKeyboardCache.get(popupKey);
            if (mMiniKeyboardContainer == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                mMiniKeyboardContainer = inflater.inflate(mPopupLayout, null);
                mMiniKeyboard = (AdjustedKeyboardView) mMiniKeyboardContainer.findViewById(R.id.keyboardView);
                View closeButton = mMiniKeyboardContainer.findViewById(R.id.closeButton);
                if (closeButton != null) closeButton.setOnClickListener(this);
                mMiniKeyboard.setOnKeyboardActionListener(new OnKeyboardActionListener() {
                    public void onKey(int primaryCode, int[] keyCodes) {
                        mKeyboardActionListener.onKey(primaryCode, keyCodes);
                        dismissPopupKeyboard();
                    }

                    public void onText(CharSequence text) {
                        mKeyboardActionListener.onText(text);
                        dismissPopupKeyboard();
                    }

                    public void swipeLeft() {
                    }

                    public void swipeRight() {
                    }

                    public void swipeUp() {
                    }

                    public void swipeDown() {
                    }

                    public void onPress(int primaryCode) {
                        mKeyboardActionListener.onPress(primaryCode);
                    }

                    public void onRelease(int primaryCode) {
                        mKeyboardActionListener.onRelease(primaryCode);
                    }
                });
                //mInputView.setSuggest(mSuggest);
                AdjustedKeyboard keyboard;
                if (popupKey.popupCharacters != null) {
                    keyboard = new AdjustedKeyboard(getContext(), popupKeyboardId,
                            popupKey.popupCharacters, -1, getPaddingLeft() + getPaddingRight());
                } else {
                    keyboard = new AdjustedKeyboard(getContext(), popupKeyboardId);
                }
                mMiniKeyboard.setKeyboard(keyboard);
                mMiniKeyboard.setPopupParent(this);
                mMiniKeyboardContainer.measure(
                        MeasureSpec.makeMeasureSpec(getWidth(), MeasureSpec.AT_MOST),
                        MeasureSpec.makeMeasureSpec(getHeight(), MeasureSpec.AT_MOST));
                mMiniKeyboardCache.put(popupKey, mMiniKeyboardContainer);
            } else {
                mMiniKeyboard = (AdjustedKeyboardView) mMiniKeyboardContainer.findViewById(
                        com.android.internal.R.id.keyboardView);
            }
            getLocationInWindow(mCoordinates);
            mPopupX = popupKey.x + getPaddingLeft();
            mPopupY = popupKey.y + getPaddingTop();
            mPopupX = mPopupX + popupKey.width - mMiniKeyboardContainer.getMeasuredWidth();
            mPopupY = mPopupY - mMiniKeyboardContainer.getMeasuredHeight();
            final int x = mPopupX + mMiniKeyboardContainer.getPaddingRight() + mCoordinates[0];
            final int y = mPopupY + mMiniKeyboardContainer.getPaddingBottom() + mCoordinates[1];
            mMiniKeyboard.setPopupOffset(x < 0 ? 0 : x, y);
            mMiniKeyboard.setShifted(isShifted());
            mPopupKeyboard.setContentView(mMiniKeyboardContainer);
            mPopupKeyboard.setWidth(mMiniKeyboardContainer.getMeasuredWidth());
            mPopupKeyboard.setHeight(mMiniKeyboardContainer.getMeasuredHeight());
            mPopupKeyboard.showAtLocation(this, Gravity.NO_GRAVITY, x, y);
            mMiniKeyboardOnScreen = true;
            //mMiniKeyboard.onTouchEvent(getTranslatedEvent(me));
            invalidateAllKeys();
            return true;
        } */
        return false;
    }

    @Override
    public boolean dispatchHoverEvent(@NonNull MotionEvent event) {
        if (mGLKKeyboardTouchHelper.dispatchHoverEvent(event)) {
            if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
                // Implement "lift to type" when accessibility turned on
                // Simulate a key press by injecting touch events directly
                event.setAction(MotionEvent.ACTION_DOWN);
                onTouchEvent(event);
                event.setAction(MotionEvent.ACTION_UP);
                onTouchEvent(event);
            }
            return true;
        }
        return super.dispatchHoverEvent(event);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent me) {
        // Convert multi-pointer up/down events to single up/down events to
        // deal with the typical multi-pointer behavior of two-thumb typing
        final int pointerCount = me.getPointerCount();
        final int action = me.getAction();
        boolean result;
        final long now = me.getEventTime();
        if (pointerCount != mOldPointerCount) {
            if (pointerCount == 1) {
                // Send a down event for the latest pointer
                MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN,
                        me.getX(), me.getY(), me.getMetaState());
                result = onModifiedTouchEvent(down, false);
                down.recycle();
                // If it's an up action, then deliver the up as well.
                if (action == MotionEvent.ACTION_UP) {
                    result = onModifiedTouchEvent(me, true);
                }
            } else {
                // Send an up event for the last pointer
                MotionEvent up = MotionEvent.obtain(now, now, MotionEvent.ACTION_UP,
                        mOldPointerX, mOldPointerY, me.getMetaState());
                result = onModifiedTouchEvent(up, true);
                up.recycle();
            }
        } else {
            if (pointerCount == 1) {
                result = onModifiedTouchEvent(me, false);
                mOldPointerX = me.getX();
                mOldPointerY = me.getY();
            } else {
                // Don't do anything when 2 pointers are down and moving.
                result = true;
            }
        }
        mOldPointerCount = pointerCount;
        return result;
    }

    private boolean onModifiedTouchEvent(MotionEvent me, boolean possiblePoly) {
        int touchX = (int) me.getX() - getPaddingLeft();
        int touchY = (int) me.getY() - getPaddingTop();
        if (touchY >= -mVerticalCorrection)
            touchY += mVerticalCorrection;
        final int action = me.getAction();
        final long eventTime = me.getEventTime();
        int keyIndex = getKeyIndex(touchX, touchY);
        mPossiblePoly = possiblePoly;
        // Track the last few movements to look for spurious swipes.
        if (action == MotionEvent.ACTION_DOWN) mSwipeTracker.clear();
        mSwipeTracker.addMovement(me);
        // Ignore all motion events until a DOWN.
        if (mAbortKey
                && action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_CANCEL) {
            return true;
        }
        if (mGestureDetector.onTouchEvent(me)) {
            showPreview(NOT_A_KEY);
            mHandler.removeMessages(MSG_REPEAT);
            mHandler.removeMessages(MSG_LONGPRESS);
            return true;
        }
        // Needs to be called after the gesture detector gets a turn, as it may have
        // displayed the mini keyboard
        if (mMiniKeyboardOnScreen && action != MotionEvent.ACTION_CANCEL) {
            return true;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mAbortKey = false;
                mStartX = touchX;
                mStartY = touchY;
                mLastCodeX = touchX;
                mLastCodeY = touchY;
                mLastKeyTime = 0;
                mCurrentKeyTime = 0;
                mLastKey = NOT_A_KEY;
                mCurrentKey = keyIndex;
                mDownKey = keyIndex;
                mDownTime = me.getEventTime();
                mLastMoveTime = mDownTime;
                checkMultiTap(eventTime, keyIndex);
                dispatchOnPress(keyIndex != NOT_A_KEY ? mKeys[keyIndex].codes[0] : 0);
                if (mCurrentKey >= 0 && mKeys[mCurrentKey].repeatable) {
                    mRepeatKeyIndex = mCurrentKey;
                    Message msg = mHandler.obtainMessage(MSG_REPEAT);
                    mHandler.sendMessageDelayed(msg, REPEAT_START_DELAY);
                    repeatKey();
                    // Delivering the key could have caused an abort
                    if (mAbortKey) {
                        mRepeatKeyIndex = NOT_A_KEY;
                        break;
                    }
                }
                if (mCurrentKey != NOT_A_KEY) {
                    Message msg = mHandler.obtainMessage(MSG_LONGPRESS, me);
                    mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT);
                }
                showPreview(keyIndex);
                break;
            case MotionEvent.ACTION_MOVE:
                boolean continueLongPress = false;
                if (keyIndex != NOT_A_KEY) {
                    if (mCurrentKey == NOT_A_KEY) {
                        mCurrentKey = keyIndex;
                        mCurrentKeyTime = eventTime - mDownTime;
                    } else {
                        if (keyIndex == mCurrentKey) {
                            mCurrentKeyTime += eventTime - mLastMoveTime;
                            continueLongPress = true;
                        } else if (mRepeatKeyIndex == NOT_A_KEY) {
                            resetMultiTap();
                            mLastKey = mCurrentKey;
                            mLastCodeX = mLastX;
                            mLastCodeY = mLastY;
                            mLastKeyTime =
                                    mCurrentKeyTime + eventTime - mLastMoveTime;
                            mCurrentKey = keyIndex;
                            mCurrentKeyTime = 0;
                        }
                    }
                }
                if (!continueLongPress) {
                    // Cancel old longpress
                    mHandler.removeMessages(MSG_LONGPRESS);
                    // Start new longpress if key has changed
                    if (keyIndex != NOT_A_KEY) {
                        Message msg = mHandler.obtainMessage(MSG_LONGPRESS, me);
                        mHandler.sendMessageDelayed(msg, LONGPRESS_TIMEOUT);
                    }
                }
                showPreview(mCurrentKey);
                mLastMoveTime = eventTime;
                break;
            case MotionEvent.ACTION_UP:
                removeMessages();
                if (keyIndex == mCurrentKey) {
                    mCurrentKeyTime += eventTime - mLastMoveTime;
                } else {
                    resetMultiTap();
                    mLastKey = mCurrentKey;
                    mLastKeyTime = mCurrentKeyTime + eventTime - mLastMoveTime;
                    mCurrentKey = keyIndex;
                    mCurrentKeyTime = 0;
                }
                if (mCurrentKeyTime < mLastKeyTime && mCurrentKeyTime < DEBOUNCE_TIME
                        && mLastKey != NOT_A_KEY) {
                    mCurrentKey = mLastKey;
                    touchX = mLastCodeX;
                    touchY = mLastCodeY;
                }
                showPreview(NOT_A_KEY);
                // If we're not on a repeating key (which sends on a DOWN event)
                if (mRepeatKeyIndex == NOT_A_KEY && !mMiniKeyboardOnScreen && !mAbortKey) {
                    detectAndSendKey(mCurrentKey, eventTime);
                }
                invalidateKey(keyIndex);
                mRepeatKeyIndex = NOT_A_KEY;
                break;
            case MotionEvent.ACTION_CANCEL:
                removeMessages();
                dismissPopupKeyboard();
                mAbortKey = true;
                showPreview(NOT_A_KEY);
                invalidateKey(mCurrentKey);
                break;
        }
        mLastX = touchX;
        mLastY = touchY;
        return true;
    }

    private boolean repeatKey() {
        detectAndSendKey(mCurrentKey, mLastTapTime);
        return true;
    }

    public void closing() {
        if (mPreviewPopup.isShowing()) {
            mPreviewPopup.dismiss();
        }
        removeMessages();
        dismissPopupKeyboard();
        mBuffer = null;
        mCanvas = null;
    }

    private void removeMessages() {
        if (mHandler != null) {
            mHandler.removeMessages(MSG_REPEAT);
            mHandler.removeMessages(MSG_LONGPRESS);
            mHandler.removeMessages(MSG_SHOW_PREVIEW);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        closing();
    }

    private void dismissPopupKeyboard() {
        if (mPopupKeyboard.isShowing()) {
            mPopupKeyboard.dismiss();
            mMiniKeyboardOnScreen = false;
            invalidateAllKeys();
        }
    }

    private void resetMultiTap() {
        mLastSentIndex = NOT_A_KEY;
        mTapCount = 0;
        mLastTapTime = -1;
        mInMultiTap = false;
    }

    private void checkMultiTap(long eventTime, int keyIndex) {
        if (keyIndex == NOT_A_KEY) return;
        Key key = mKeys[keyIndex];
        if (key.codes.length > 1) {
            mInMultiTap = true;
            if (eventTime < mLastTapTime + MULTITAP_INTERVAL
                    && keyIndex == mLastSentIndex) {
                mTapCount = (mTapCount + 1) % key.codes.length;
                return;
            } else {
                mTapCount = -1;
                return;
            }
        }
        if (eventTime > mLastTapTime + MULTITAP_INTERVAL || keyIndex != mLastSentIndex) {
            resetMultiTap();
        }
    }

    private static class SwipeTracker {
        static final int NUM_PAST = 4;
        static final int LONGEST_PAST_TIME = 200;
        final float mPastX[] = new float[NUM_PAST];
        final float mPastY[] = new float[NUM_PAST];
        final long mPastTime[] = new long[NUM_PAST];
        float mYVelocity;
        float mXVelocity;

        public void clear() {
            mPastTime[0] = 0;
        }

        void addMovement(@NonNull MotionEvent ev) {
            long time = ev.getEventTime();
            final int N = ev.getHistorySize();
            for (int i = 0; i < N; i++) {
                addPoint(ev.getHistoricalX(i), ev.getHistoricalY(i),
                        ev.getHistoricalEventTime(i));
            }
            addPoint(ev.getX(), ev.getY(), time);
        }

        private void addPoint(float x, float y, long time) {
            int drop = -1;
            int i;
            final long[] pastTime = mPastTime;
            for (i = 0; i < NUM_PAST; i++) {
                if (pastTime[i] == 0) {
                    break;
                } else if (pastTime[i] < time - LONGEST_PAST_TIME) {
                    drop = i;
                }
            }
            if (i == NUM_PAST && drop < 0) {
                drop = 0;
            }
            if (drop == i) drop--;
            final float[] pastX = mPastX;
            final float[] pastY = mPastY;
            if (drop >= 0) {
                final int start = drop + 1;
                final int count = NUM_PAST - drop - 1;
                System.arraycopy(pastX, start, pastX, 0, count);
                System.arraycopy(pastY, start, pastY, 0, count);
                System.arraycopy(pastTime, start, pastTime, 0, count);
                i -= (drop + 1);
            }
            pastX[i] = x;
            pastY[i] = y;
            pastTime[i] = time;
            i++;
            if (i < NUM_PAST) {
                pastTime[i] = 0;
            }
        }

        void computeCurrentVelocity(int units) {
            computeCurrentVelocity(units, Float.MAX_VALUE);
        }

        void computeCurrentVelocity(int units, float maxVelocity) {
            final float[] pastX = mPastX;
            final float[] pastY = mPastY;
            final long[] pastTime = mPastTime;
            final float oldestX = pastX[0];
            final float oldestY = pastY[0];
            final long oldestTime = pastTime[0];
            float accumX = 0;
            float accumY = 0;
            int N = 0;
            while (N < NUM_PAST) {
                if (pastTime[N] == 0) {
                    break;
                }
                N++;
            }
            for (int i = 1; i < N; i++) {
                final int dur = (int) (pastTime[i] - oldestTime);
                if (dur == 0) continue;
                float dist = pastX[i] - oldestX;
                float vel = (dist / dur) * units;   // pixels/frame.
                if (accumX == 0) accumX = vel;
                else accumX = (accumX + vel) * .5f;
                dist = pastY[i] - oldestY;
                vel = (dist / dur) * units;   // pixels/frame.
                if (accumY == 0) accumY = vel;
                else accumY = (accumY + vel) * .5f;
            }
            mXVelocity = accumX < 0.0f ? Math.max(accumX, -maxVelocity)
                    : Math.min(accumX, maxVelocity);
            mYVelocity = accumY < 0.0f ? Math.max(accumY, -maxVelocity)
                    : Math.min(accumY, maxVelocity);
        }

        float getXVelocity() {
            return mXVelocity;
        }

        float getYVelocity() {
            return mYVelocity;
        }
    }
}
