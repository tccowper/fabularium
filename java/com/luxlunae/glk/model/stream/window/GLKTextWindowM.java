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

import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.DisplayMetrics;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.model.style.GLKStyleSpan;

import java.util.Arrays;

public abstract class GLKTextWindowM extends GLKNonPairM {

    public final TextPaint mBaseTextPaint = new TextPaint();    // default text paint - how text will be rendered in the absence of modifying spans
    public final float mLeadingMult;  //  factor by which to scale the font size to get the default line spacing (optimal is between 1.2 and 1.45)
    final int mCursorWidthPX;
    public float mCharWidth;
    public float mCharHeight;
    private float mLeadingPX;
    @Nullable
    private int[] mCustomLineTerminators = null;        // custom line terminators (in addition to the default of [ENTER])

    GLKTextWindowM(@NonNull GLKTextWindowBuilder builder) {
        super(builder);

        mLeadingMult = builder.leadingMult;
        mCursorWidthPX = builder.cursorWidthPx;

        // Set the default "normal" font
        mBaseTextPaint.setAntiAlias(true);
        mBaseTextPaint.setSubpixelText(builder.mSubPixel);
        mBaseTextPaint.linkColor = Color.TRANSPARENT;
        setNormalFont(mWindowStyles[GLKConstants.style_Normal]);
    }

    @CallSuper
    public void setNormalFont(@NonNull GLKStyleSpan s) {
        s.updateDrawState(mBaseTextPaint);

        // Measure window size in terms of "normal" font
        // We set char width using measureText to avoid rounding error -
        // this gives a float value rather than the bounds which gives an int
        // See http://stackoverflow.com/questions/7549182/android-paint-measuretext-vs-gettextbounds
        Paint.FontMetrics fm = mBaseTextPaint.getFontMetrics();
        float asc = -fm.ascent;
        float des = fm.descent;
        mCharWidth = mBaseTextPaint.measureText("m");
        mCharHeight = mLeadingMult * (asc + des);
        mLeadingPX = mCharHeight - asc - des;
        resize(mWidthPX, mHeightPX);
    }

    public float getLeadingMult() {
        return mLeadingMult;
    }

    @NonNull
    public TextPaint getBaseTextPaint() {
        return mBaseTextPaint;
    }

    public void setTextColor(int color) {
        mBaseTextPaint.setColor(color);
    }

    @Override
    public int getPreferredHeight(int lines, DisplayMetrics dm) {
        if (lines == 0) {
            // caller wants to hide this window - so return the absolute smallest height we can
            // N.B. we can't return 0
            return 1;
        }
        return (int) (mCharHeight * ((float) lines / mGLKHeightMultiplier) + (float) mPaddingTopPx + (float) mPaddingBottomPx + mLeadingPX);
    }

    @Override
    public int getPreferredWidth(int cols, DisplayMetrics dm) {
        if (cols == 0) {
            // caller wants to hide this window - so return the absolute smallest height we can
            // N.B. we can't return 0
            return 1;
        }
        return (int) (mCharWidth * ((float) cols / mGLKWidthMultiplier) + (float) mPaddingLeftPx + (float) mPaddingRightPx);
    }

    @CallSuper
    @Override
    public void resize(int widthPx, int heightPx) {
        super.resize(widthPx, heightPx);

        // the GLK width and height of text windows is in rows and cols, not pixels
        mWidthGLK = (int) ((float) mWidthLessPaddingPx / mCharWidth * mGLKWidthMultiplier);
        mHeightGLK = (int) ((float) mHeightLessPaddingPx / mCharHeight * mGLKHeightMultiplier);
    }

    @Nullable
    public int[] getTerminators() {
        return (mCustomLineTerminators != null) ?
                Arrays.copyOf(mCustomLineTerminators, mCustomLineTerminators.length) : null;
    }

    public void setTerminators(@Nullable long[] keycodes) {
        if (keycodes == null) {
            mCustomLineTerminators = null;
        } else {
            int sz = keycodes.length;
            int ch;
            mCustomLineTerminators = new int[sz];
            for (int i = 0; i < sz; i++) {
                ch = (int) keycodes[i];
                if (ch < GLKConstants.keycode_Func12) {
                    // Assume that the byte ordering is the wrong way around and try to
                    // auto-correct...
                    ch = GLKUtils.swapEndian32(ch);
                }
                mCustomLineTerminators[i] = ch;
            }
        }
    }

    public static class GLKTextWindowBuilder extends GLKNonPairBuilder {
        private float leadingMult;
        private int cursorWidthPx;
        private boolean mSubPixel;

        @NonNull
        public GLKTextWindowBuilder setLeadingMultiplier(float leadingMult) {
            this.leadingMult = leadingMult;
            return this;
        }

        @NonNull
        public GLKTextWindowBuilder setSubPixelRendering(boolean on) {
            mSubPixel = on;
            return this;
        }

        @NonNull
        public GLKTextWindowBuilder setCursorWidth(int widthPx) {
            this.cursorWidthPx = widthPx;
            return this;
        }
    }
}