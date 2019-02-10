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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.style.ImageSpan;

import com.luxlunae.glk.GLKLogger;

class HTMLImage extends ImageSpan {
    @NonNull
    private final String mAlign;

    HTMLImage(@NonNull Drawable d, @NonNull String src, @Nullable String align) {
        super(d, src);
        mAlign = (align != null) ? align.toLowerCase() : "bottom";
        if (mAlign.equals("left")) {
            GLKLogger.error("FIXME: HTMLImage: draw image in left margin");
        } else if (mAlign.equals("right")) {
            GLKLogger.error("FIXME: HTMLImage: draw image in right margin");
        }
    }

    @Override
    public int getSize(@NonNull Paint paint, @NonNull CharSequence text, int start, int end, @Nullable Paint.FontMetricsInt fm) {
        Drawable d = getDrawable();
        Rect rect = d.getBounds();
        if (fm != null) {
            fm.top = fm.ascent = -rect.bottom;
            fm.descent = fm.bottom = 0;
        }
        return rect.width();
    }

    @Override
    public void draw(@NonNull Canvas canvas, @NonNull CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        Drawable d = getDrawable();
        int transY = 0;

        canvas.save();

        switch (mAlign) {
            case "top":
                transY = top;
                break;
            case "middle":
                transY = bottom - d.getBounds().bottom - paint.getFontMetricsInt().descent / 2;
                break;
            case "bottom":
                transY = bottom - d.getBounds().bottom;
                break;
            case "left":
                transY = bottom - d.getBounds().bottom;
                break;
            case "right":
                transY = bottom - d.getBounds().bottom;
                break;
        }

        canvas.translate(x, transY);
        d.draw(canvas);
        canvas.restore();
    }
}
