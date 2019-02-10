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
package com.luxlunae.glk.model.style;

import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;

public class GLKHyperlinkSpan extends ClickableSpan {
    public final boolean mNoEnter;     // TADS-specific extension
    public final boolean mAppend;      // TADS-specific extension
    @Nullable
    public final GLKTextWindowM mInputWin;      // TADS-specific extensions
    @NonNull
    private final String mHref;
    private final boolean mPlain;       // TADS-specific extensions
    private final int mValue;
    private final int mColor;

    public GLKHyperlinkSpan(@NonNull String href, @ColorInt int color, boolean append, boolean noEnter,
                            boolean plain, @Nullable GLKTextWindowM inputWin) {
        mHref = href;
        mAppend = append;
        mNoEnter = noEnter;
        mPlain = plain;
        mInputWin = inputWin;
        mColor = color;
        mValue = GLKConstants.NULL;
    }

    public GLKHyperlinkSpan(int value, @ColorInt int color) {
        mValue = value;
        mPlain = false;
        mNoEnter = false;
        mAppend = false;
        mInputWin = null;
        mHref = "";
        mColor = color;
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        if (!mPlain) {
            ds.setColor(mColor);
            ds.linkColor = mColor;
            //  ds.setUnderlineText(true);
        }
    }

    @Override
    public void onClick(@NonNull View v) {
        // Currently does nothing
    }

    @NonNull
    public String getUrl() {
        return mHref;
    }

    public int getVal() {
        return mValue;
    }
}