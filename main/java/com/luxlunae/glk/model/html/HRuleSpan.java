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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.style.ReplacementSpan;

import com.luxlunae.glk.model.stream.window.GLKTextWindowM;

class HRuleSpan extends ReplacementSpan {
    @NonNull
    private final Point mSizeTracker;
    @NonNull
    private final GLKTextWindowM mAttachedWin;
    @NonNull
    private final HTMLLength mWidth;
    private final int mHeight;
    private final boolean mNoShade;
    @NonNull
    private final Layout.Alignment mAlign;
    private int mActWidth;

    HRuleSpan(@NonNull Layout.Alignment align, boolean noshade, int size,
              @NonNull HTMLLength width, @NonNull Point sizeTracker, @NonNull GLKTextWindowM w) {
        mSizeTracker = sizeTracker;
        mAttachedWin = w;
        mHeight = size;
        mWidth = width;
        mNoShade = noshade;
        mAlign = align;
    }


    @Override
    public int getSize(@NonNull Paint paint, @NonNull CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        if (fm != null) {
            // N.B. ascent and top are negative
            // We centre the horizontal rule vertically over the baseline
            fm.ascent = fm.top = 0;
            fm.descent = fm.bottom = mHeight;
            fm.leading = 0;
        }

        int max_width = mSizeTracker.x != -1 ? mSizeTracker.x : mAttachedWin.mWidthGLK_PX;
        if (mWidth.mValue == HTMLLength.SIZE_TO_CONTENTS) {
            mActWidth = max_width;
        } else {
            // Table width is either in pixels or a percentage
            int cand_width = mWidth.mIsPercent ? (int) (((float) mWidth.mValue / 100f) * (float) max_width) : mWidth.mValue;
            mActWidth = Math.min(cand_width, max_width);
        }

        return mActWidth;
    }

    @Override
    public void draw(@NonNull Canvas canvas, @NonNull CharSequence text, int start, int end,
                     float x, int top, int y, int bottom, @NonNull Paint paint) {
        // draw two lines - dark gray then light gray
        Paint p = new Paint();
        int l;
        int t = mAttachedWin.mWidthGLK_PX;
        switch (mAlign) {
            case ALIGN_CENTER:
                l = (int) ((float) (t - mActWidth) / 2f);
                break;
            case ALIGN_OPPOSITE:
                l = t - mActWidth;
                break;
            case ALIGN_NORMAL:
            default:
                l = (int) x;
                break;
        }

        int r = l + mActWidth;
        int b = y + mHeight;

        if (mNoShade) {
            p.setColor(Color.rgb(150, 150, 150));
            canvas.drawRect(l, y, r, b, p);
        } else {
            p.setColor(Color.rgb(76, 76, 76));
            canvas.drawRect(l, y, r, y + 2, p);
            canvas.drawRect(l, y, l + 2, b, p);

            p.setColor(Color.rgb(210, 210, 210));
            canvas.drawRect(l, b - 2, r, b, p);
            canvas.drawRect(r - 2, y, r, b, p);
        }
    }
}
