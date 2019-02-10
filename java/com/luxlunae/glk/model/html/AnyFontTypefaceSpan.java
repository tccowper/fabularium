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

package com.luxlunae.glk.model.html;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.style.TypefaceSpan;

public class AnyFontTypefaceSpan extends TypefaceSpan {
    // Thanks https://stackoverflow.com/questions/6612316/how-set-spannable-object-font-with-custom-font
    @Nullable
    private final Typeface newType;

    AnyFontTypefaceSpan(@NonNull String family) {
        // In this instance, we behave just like a normal TypefaceSpan
        super(family);
        newType = null;
    }

    AnyFontTypefaceSpan(@NonNull Typeface type) {
        // In this instance we use the custom typeface provided
        super("");
        newType = type;
    }

    private static void applyCustomTypeFace(@NonNull Paint paint, @NonNull Typeface tf) {
        int oldStyle;
        Typeface old = paint.getTypeface();
        if (old == null) {
            oldStyle = 0;
        } else {
            oldStyle = old.getStyle();
        }

        int fake = oldStyle & ~tf.getStyle();
        if ((fake & Typeface.BOLD) != 0) {
            paint.setFakeBoldText(true);
        }

        if ((fake & Typeface.ITALIC) != 0) {
            paint.setTextSkewX(-0.25f);
        }

        paint.setTypeface(tf);
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        if (newType != null) {
            applyCustomTypeFace(ds, newType);
        } else {
            super.updateDrawState(ds);
        }
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint paint) {
        if (newType != null) {
            applyCustomTypeFace(paint, newType);
        } else {
            super.updateMeasureState(paint);
        }
    }
}
