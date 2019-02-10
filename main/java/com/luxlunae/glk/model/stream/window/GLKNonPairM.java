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
package com.luxlunae.glk.model.stream.window;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.annotation.CallSuper;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import com.luxlunae.glk.GLKCharsetManager;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.GLKDrawable;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.GLKOutputStream;
import com.luxlunae.glk.model.style.GLKStyleSpan;

import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class GLKNonPairM extends GLKWindowM implements GLKOutputStream {
    public final GLKStyleSpan[] mWindowStyles = new GLKStyleSpan[GLKConstants.style_NUMSTYLES];
    @NonNull
    public final GLKCharsetManager mCharsetMgr;
    public final float mGLKWidthMultiplier;
    public final float mGLKHeightMultiplier;
    public int mHeightGLK_PX, mWidthGLK_PX;
    int mWidthLessPaddingPx;
    int mHeightLessPaddingPx;
    int mPaddingLeftPx = 0;
    int mPaddingTopPx = 0;
    int mPaddingRightPx = 0;
    int mPaddingBottomPx = 9;
    GLKStyleSpan mCurrentStyle;
    int mHeightGLK, mWidthGLK;
    int mBGColor;                           // background colour of the window
    @Nullable
    private GLKDrawable mBackgroundImage = null;
    @Nullable
    private GLKOutputStream mEchoee = null;         // if not null, mEchoee will receive a copy of everything sent to this window
    @Nullable
    private GLKNonPairM mEchoer = null;              // if not null, mEchoer is echoing its inputs to this window
    private long mWriteCount = 0;
    public int mLineEventCount = 1;
    private boolean mIsCharRequested = false;
    private boolean mIsLineRequested = false;
    private boolean mIsHyperlinkRequested = false;

    /**
     * Creates a new NonPair window, with the given parameters.
     */
    GLKNonPairM(@NonNull GLKNonPairBuilder builder) {
        super(builder.model);

        mCharsetMgr = builder.model.mCharsetMgr;
        mGLKWidthMultiplier = (builder.glkWidthMultiplier > 1) ? builder.glkWidthMultiplier : 1;
        mGLKHeightMultiplier = (builder.glkHeightMultiplier > 1) ? builder.glkHeightMultiplier : 1;

        // Set the window's background colour to the same as the default style or, if that is Color.TRANSPARENT,
        // to the specified default BG colour
        int bg = builder.defaultStyles[GLKConstants.style_Normal].mBackColor;
        postClearWindowBackground(bg != Color.TRANSPARENT ? bg : builder.colourBG);

        // Set up styles
        // Note: we make local copies of the objects rather than taking references
        for (int i = 0, sz = GLKConstants.style_NUMSTYLES; i < sz; ++i)
            mWindowStyles[i] = new GLKStyleSpan(builder.defaultStyles[i]);
        mCurrentStyle = mWindowStyles[GLKConstants.style_Normal];
    }

    /**
     * Helper function for setZStyleColors
     *
     * @param color       - the color code, in Z machine format
     * @param input_color - the default input color
     * @param sty         - the style to change
     * @param isFG        - whether this is foreground or background color
     */
    private static void setZStyleColor(int color, @ColorInt int input_color, @NonNull GLKStyleSpan sty, boolean isFG) {
        int stylehint = isFG ? GLKConstants.stylehint_TextColor : GLKConstants.stylehint_BackColor;
        switch (color) {
            case GLKConstants.zcolor_Current:
                // do nothing
                break;
            case GLKConstants.zcolor_Default:
                GLKStyleSpan.clear_stylehint(stylehint, sty);
                break;
            case GLKConstants.zcolor_Transparent:
                GLKStyleSpan.set_stylehint(stylehint, Color.TRANSPARENT, sty, false);
                break;
            case GLKConstants.zcolor_Cursor:
                GLKStyleSpan.set_stylehint(stylehint, input_color, sty, false);
                break;
            default:
                GLKStyleSpan.set_stylehint(stylehint, color | 0xFF000000, sty, false);
                break;
        }
    }

    public int getBGColor() {
        return mBGColor;
    }

    @Nullable
    public GLKDrawable getBGImage() {
        return mBackgroundImage;
    }

    @NonNull
    public Rect getPadding() {
        return new Rect(mPaddingLeftPx, mPaddingTopPx, mPaddingRightPx, mPaddingBottomPx);
    }

    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeftPx = left;
        mPaddingTopPx = top;
        mPaddingRightPx = right;
        mPaddingBottomPx = bottom;
    }

    public int getPaddingLeft() {
        return mPaddingLeftPx;
    }

    public int getPaddingRight() {
        return mPaddingRightPx;
    }

    @ColorInt
    int getWindowBG() {
        return mBGColor;
    }

    /**
     * Change this window's background colour, the next time
     * it is redrawn.
     *
     * @param bg - new background colour.
     */
    public void postClearWindowBackground(@ColorInt int bg) {
        mBGColor = bg;
        mModifierFlags.add(ModifierFlag.FLAG_BG_COLOR_CHANGED);
    }

    public void postSetBackground(GLKDrawable bg) {
        mBackgroundImage = bg;
        mModifierFlags.add(ModifierFlag.FLAG_BG_IMAGE_CHANGED);
    }

    public int getEchoee() {
        if (mEchoee != null && mEchoee instanceof GLKNonPairM) {
            return mEchoee.getStreamId();
        }
        return GLKConstants.NULL;
    }

    public void setEchoee(@Nullable GLKOutputStream s) {
        GLKLogger.warn("FIXME: GLKWindow: TODO: ensure that echo streams are not set up to create an infinite loop");
        mEchoee = s;
        if (s != null) {
            s.setEchoer(this);
        }
    }

    public void setEchoer(GLKNonPairM w) {
        if (mEchoer != null) {
            // detach ourselves from the existing mEchoer
            mEchoer.setEchoee(null);
        }
        mEchoer = w;
    }

    public boolean styleMeasure(int style, int hint, @NonNull int[] result) {
        return GLKStyleSpan.get_stylehint(hint, result, mWindowStyles[style]);
    }

    @Override
    public void resize(int widthPx, int heightPx) {
        if (mWidthPX != widthPx || mHeightPX != heightPx) {
            mWidthPX = widthPx;
            mHeightPX = heightPx;

            int width = widthPx - mPaddingLeftPx - mPaddingRightPx;
            int height = heightPx - mPaddingTopPx - mPaddingBottomPx;
            if (width < 0) width = 0;
            if (height < 0) height = 0;
            mWidthLessPaddingPx = width;
            mHeightLessPaddingPx = height;

            // calculate size of this window in pixels, scaled by GLK width / height multipliers
            mWidthGLK_PX = (int) ((float) mWidthLessPaddingPx * mGLKWidthMultiplier);
            mHeightGLK_PX = (int) ((float) mHeightLessPaddingPx * mGLKHeightMultiplier);

            // resize the background image, if there is one
            if (mBackgroundImage != null) {
                if ((widthPx > 0 && heightPx > 0) &&
                        (mBackgroundImage.getIntrinsicWidth() != widthPx ||
                                mBackgroundImage.getIntrinsicHeight() != heightPx)) {
                    Bitmap b = mBackgroundImage.getBitmap();
                    Bitmap bitmapResized = Bitmap.createScaledBitmap(b, widthPx, heightPx, false);
                    mBackgroundImage = new GLKDrawable(mModel.getApplicationContext().getResources(), bitmapResized, mBackgroundImage.mImgID);
                }
            }
        }
    }

    /**
     * Change the output style of this window.
     */
    @Override
    public void setStyle(int s) {
        if (mEchoee != null) {
            mEchoee.setStyle(s);
        }
        mCurrentStyle = mWindowStyles[s];
    }

    /**
     * Set the foreground and background colours of all of this window's styles.
     * <p>
     * This is a convenience method provided for backwards compatibility with old
     * zcode games. This only affects new text output to the window - the
     * fore/back colours of previous text do not change.
     *
     * @param fg - the new foreground colour, or GLKConstants.zcolor_Default to
     *           return to the default foreground colour (initial setup or color
     *           set by glk_set_stylehint()).
     * @param bg - the new background colour, or GLKConstants.zcolor_Default to
     *           return to the default foreground colour (initial setup or color
     *           set by glk_set_stylehint()).
     */
    public void setZStyleColors(int fg, int bg) {
        // we need to create new styles as if we modify the existing
        // styles we will end up retrospectively changing colours of
        // any of those styles that have already been applied to earlier text
        for (int i = 0, sz = mWindowStyles.length; i < sz; i++) {
            GLKStyleSpan sty = new GLKStyleSpan(mWindowStyles[i]);
            setZStyleColor(fg, mWindowStyles[GLKConstants.style_Input].mTextColor, sty, true);
            setZStyleColor(bg, mWindowStyles[GLKConstants.style_Input].mTextColor, sty, false);
            GLKStyleSpan.ensureIsReadable(sty);
            mWindowStyles[i] = sty;
        }

        // ensure current style is pointing to the modified span object
        mCurrentStyle = mWindowStyles[mCurrentStyle.mStyleID];
    }

    /**
     * Set the reverse video attribute for all of this window's styles.
     * This is a convenience method provided for backwards compatibility with old
     * zcode games. This only affects new text output to the window - the
     * fore/back colours of previous text do not change.
     *
     * @param reverse - TRUE if subsequent text should be reverse video, FALSE otherwise.
     */
    public void setStyleReverse(boolean reverse) {
        // we need to create new styles as if we modify the existing
        // styles we will end up retrospectively changing colours of
        // any of those styles that have already been applied to earlier text
        for (int i = 0, sz = mWindowStyles.length; i < sz; i++) {
            GLKStyleSpan sty = new GLKStyleSpan(mWindowStyles[i]);
            GLKStyleSpan.set_stylehint(GLKConstants.stylehint_ReverseColor, reverse ? 1 : 0, sty, false);
            mWindowStyles[i] = sty;
        }

        // ensure current style is pointing to the modified span object
        mCurrentStyle = mWindowStyles[mCurrentStyle.mStyleID];
    }

    @Override
    public long getWriteCount() {
        return mWriteCount;
    }

    @Override
    public void putString(@NonNull String s) {
        if (mEchoee != null) {
            mEchoee.putString(s);
        }
        mWriteCount += s.length();
    }

    @Override
    public void putChar(int ch) {
        if (mEchoee != null) {
            mEchoee.putChar(ch);
        }
        mWriteCount++;
    }

    @Override
    public void putBuffer(@NonNull ByteBuffer buf, boolean unicode) {
        String s = mCharsetMgr.getGLKString(buf, unicode);
        if (mEchoee != null) {
            mEchoee.putString(s);
        }
        putString(s);
    }

    /**
     * Returns the total rows and columns of this window if it were to be filled
     * by "0"s in its normal style.
     * <p/>
     * x = max cols
     * y = max rows
     *
     * @return a new point
     */
    @NonNull
    public Point getGLKSize() {
        return new Point(mWidthGLK, mHeightGLK);
    }

    public abstract int getPreferredHeight(int size, DisplayMetrics dm);

    public abstract int getPreferredWidth(int size, DisplayMetrics dm);

    /**
     * Set the background colour of this window to match the background colour of the
     * window's normal style.
     */
    public void clear() {
        GLKStyleSpan sty = mWindowStyles[GLKConstants.style_Normal];
        if (sty.mReverse && sty.mTextColor != Color.TRANSPARENT && sty.mTextColor != mBGColor) {
            postClearWindowBackground(sty.mTextColor);
        } else if (sty.mBackColor != Color.TRANSPARENT && sty.mBackColor != mBGColor) {
            postClearWindowBackground(sty.mBackColor);
        }
        mModifierFlags.add(ModifierFlag.FLAG_CLEARED);
    }

    public boolean charRequested() {
        return mIsCharRequested;
    }

    public boolean lineRequested() {
        return mIsLineRequested;
    }

    public boolean hyperlinkRequested() {
        return mIsHyperlinkRequested;
    }

    @CallSuper
    public void requestLineEvent(ByteBuffer buf, int initlen, boolean unicode) {
        if (mIsCharRequested) {
            // According to the GLK spec "A window cannot have requests for both character and
            // line input at the same time... It is illegal to call glk_request_line_event() if
            // the window already has a pending request for either character or line input."
            // So, if we hit this point, behaviour is undefined. In this case, we simply cancel
            // the pending char event.
            GLKLogger.warn("GLKNonPairM: line input requested when character request still active.");
            cancelCharEvent();
        }
        mIsLineRequested = true;
        mLineEventCount++;
    }

    @CallSuper
    public void cancelLineEvent(GLKEvent event) {
        mIsLineRequested = false;
    }

    @CallSuper
    public void requestCharEvent() {
        if (mIsLineRequested) {
            // According to the GLK spec "A window cannot have requests for both character and
            // line input at the same time... It is illegal to call glk_request_line_event() if
            // the window already has a pending request for either character or line input."
            // So, if we hit this point, behaviour is undefined. In this case, we simply cancel
            // the pending char event.
            GLKLogger.warn("GLKNonPairM: character requested when line input request still active.");
            cancelLineEvent(null);
        }
        mIsCharRequested = true;
    }

    @CallSuper
    public void cancelCharEvent() {
        mIsCharRequested = false;
    }

    @CallSuper
    public void requestHyperlinkEvent() {
        mIsHyperlinkRequested = true;
    }

    @CallSuper
    public void cancelHyperlinkEvent() {
        mIsHyperlinkRequested = false;
    }

    @Override
    public void close() throws IOException {
        super.close();
        setEchoer(null);
    }

    public static class GLKNonPairBuilder {
        private GLKStyleSpan[] defaultStyles;
        private int colourBG;
        private float glkWidthMultiplier;
        private float glkHeightMultiplier;
        private GLKModel model;

        @NonNull
        public GLKNonPairBuilder setModel(GLKModel m) {
            model = m;
            return this;
        }

        @NonNull
        public GLKNonPairBuilder setDefaultStyles(GLKStyleSpan[] defaultStyles) {
            this.defaultStyles = defaultStyles;
            return this;
        }

        @NonNull
        public GLKNonPairBuilder setBGColor(@ColorInt int colourBG) {
            this.colourBG = colourBG;
            return this;
        }

        @NonNull
        public GLKNonPairBuilder setGLKMultipliers(float widthMultiplier, float heightMultiplier) {
            this.glkWidthMultiplier = widthMultiplier;
            this.glkHeightMultiplier = heightMultiplier;
            return this;
        }
    }
}
