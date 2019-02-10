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

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.widget.LinearLayout;

import com.luxlunae.glk.model.stream.window.GLKPairM;
import com.luxlunae.glk.model.stream.window.GLKWindowM;

import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_CONTENT_CHANGED;

@SuppressLint("ViewConstructor")
public class GLKPairV extends LinearLayout implements GLKWindowV {
    @NonNull
    private final GLKPairM mModel;
    private final int mStreamID;

    public GLKPairV(@NonNull Context context, @NonNull GLKPairM model, @ColorInt int borderColor, int borderWidth, int borderHeight) {
        super(context);
        setBackgroundColor(Color.TRANSPARENT);
        setDividerDrawable(new SimpleDivider(borderColor, borderWidth, borderHeight));
        setShowDividers(model.showingBorder() ? SHOW_DIVIDER_MIDDLE : SHOW_DIVIDER_NONE);
        setWeightSum(100);
        mModel = model;
        mStreamID = mModel.getStreamId();
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

    public void updateContents() {
        if (mModel.isFlagSet(FLAG_CONTENT_CHANGED)) {
            // currently don't need to do anything
            mModel.clearFlag(FLAG_CONTENT_CHANGED);
        }
    }

    private static class SimpleDivider extends Drawable {
        private final int mColor;
        private final int mHeight;
        private final int mWidth;

        SimpleDivider(@ColorInt int color, int lineWidth, int lineHeight) {
            mColor = color;
            mHeight = lineHeight;
            mWidth = lineWidth;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            canvas.drawColor(mColor);
        }

        @Override
        public void setAlpha(int alpha) {
            // Specify an alpha value for the drawable. 0 means fully transparent, and 255 means fully opaque.
            // do nothing
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            // Specify an optional color filter for the drawable.
            // do nothing
        }

        @Override
        public int getOpacity() {
            return PixelFormat.OPAQUE;
        }

        @Override
        public int getIntrinsicWidth() {
            return mWidth;
        }

        @Override
        public int getIntrinsicHeight() {
            return mHeight;
        }

    }
}
