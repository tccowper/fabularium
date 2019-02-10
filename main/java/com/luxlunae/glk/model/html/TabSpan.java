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
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.TextPaint;
import android.text.style.ReplacementSpan;

import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;

/**
 * The TAB tag provides simple alignment capabilities without the more complex TABLE structure.
 * <p>
 * <TAB ID=abc> defines a tab named 'abc' at the current horizontal position in the line;
 * this can occur within ordinary text to indicate an alignment position that can be used
 * in subsequent lines.
 * <p>
 * <TAB TO=abc> adds horizontal whitespace in the current line up to the position of the previously defined tab 'abc'.
 * <p>
 * The ALIGN attribute can be used with <TAB> to specify the type of alignment to use. ALIGN can be used on the
 * defining <TAB ID=abc>, or on each use of <TAB TO=abc>; if no ALIGN is used in the TO tag, the ALIGN from
 * the ID tag is used by default; LEFT is used if neither has an ALIGN attribute. ALIGN=LEFT aligns
 * the material after the <TAB TO> with its left edge aligned at the tab; ALIGN=RIGHT aligns the material after
 * the <TAB TO> and up to the next <TAB TO> or the end of the line, whichever comes first, flush
 * right at the position of the tab. ALIGN=CENTER aligns the material up to the next <TAB TO> centered on
 * the tab position. ALIGN=DP aligns at a decimal point (or at any other character specified with DECIMAL="c",
 * where "c" is the character at which to align entries).  <TAB ALIGN=CENTER> or <TAB ALIGN=RIGHT>, without a TO attribute,
 * align the material with respect to the right margin. This provides a simple way of
 * aligning material against the right margin (ALIGN=RIGHT), or centered between the end of the text up to
 * the <TAB> and the right margin.
 * <p>
 * <TAB INDENT=n> (where n is a number) indents by a given number of "en" units; it simply adds the given
 * amount of whitespace to the line.
 * <p>
 * <TAB MULTIPLE=n> indents to the next multiple of the given number of ens from the left margin.
 * You can use <TAB MULTIPLE> to get the effect of tabs set at regular intervals across the page, without
 * having to set up a bunch of named indent points and figuring out which one you're closest to.
 */
class TabSpan extends ReplacementSpan {
    private static final boolean DEBUG_TABSPAN = false;
    private static final String EN_SPACE = "\u2002";

    private final String mId;
    private final String mTo;
    private final Layout.Alignment mAlign;
    private final int mIndent;
    private final int mMultiple;
    private final State mState;
    private int mWidth;
    private float mLeftWidth;
    private float mRightWidth;
    private int mStart;
    private int mEnd;

    /**
     * Create a new TAB span.
     *
     * @param id       - the ID of this span. Used to indicate alignment position for subsequent spans.
     * @param to       - the ID of another TAB span. If set, adds horizontal whitespace in the current
     *                 line up to the position of that previously defined tab.
     * @param align    - either NORMAL, OPPOSITE or CENTER.
     * @param indent   - indents by a given number of "en" units; it simply adds the given
     *                 amount of whitespace to the line.
     * @param multiple - indents to the next multiple of the given number of ens from the left margin.
     * @param state    - the single State instance.
     */
    TabSpan(String id, String to, Layout.Alignment align, int indent, int multiple, State state) {
        mId = id;
        mTo = to;
        mAlign = align;
        mIndent = indent;
        mMultiple = multiple;
        mState = state;
    }

    /**
     * Returns the width of the span. Extending classes can set the height of the span by updating
     * attributes of Paint.FontMetricsInt.
     *
     * @param paint - Paint instance. This value must never be null.
     * @param text  - Current text.
     * @param start - Start character index for span.
     * @param end   - End character index for span.
     * @param fm    - Font metrics, can be null.
     * @return Width of the span.
     */
    @Override
    public int getSize(@NonNull Paint paint, @NonNull CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        TextPaint tp = new TextPaint(paint);
        float szOfEn = tp.measureText(EN_SPACE);

        if (mIndent != 0) {
            return (int) ((float) mIndent * szOfEn);
        }

        if (mTo != null) {
            GLKLogger.error("TabSpan: implement TO = " + mTo);
            return 0;
        }

        int w = mState.mAttachedWindow.mWidthGLK_PX;
        if (mWidth != w || mStart != start || mEnd != end) {
            // Need to recalculate dimensions
            if (DEBUG_TABSPAN) {
                GLKLogger.debug("TabSpan: text window width is " + w + " pixels.");
                GLKLogger.debug("Current text is '" + text + "'");
                GLKLogger.debug("start = " + start + ", end = " + end);
            }
            mWidth = w;
            mStart = start;
            mEnd = end;

            // Calculate width of left text
            CharSequence leftText = GLKUtils.getLine(start - 1, 0, start, text,
                    mState.mAttachedWindow.mBaseTextPaint, mState.mAttachedWindow.mWidthGLK_PX);
            mLeftWidth = 0;
            if (leftText != null) {
                // There is some text to the left
                mLeftWidth = GLKUtils.getSpannedWidth(leftText, tp, mWidth);
                if (DEBUG_TABSPAN) {
                    GLKLogger.error("TabSpan: width of left text '" + leftText + "' is " + mLeftWidth + " pixels.");
                }
            }

            // If relevant, calculate width of the right text
            mRightWidth = 0;
            if (mAlign != null && mAlign != Layout.Alignment.ALIGN_NORMAL) {
                int len = text.length();
                if (end < len) {
                    // There is some text to the right
                    // We add a little bit to the width to ensure any italicised text is not chopped
                    CharSequence rightText = GLKUtils.getLine(end, end, len, text,
                            mState.mAttachedWindow.mBaseTextPaint, mState.mAttachedWindow.mWidthGLK_PX);
                    if (rightText != null) {
                        mRightWidth = GLKUtils.getSpannedWidth(rightText, tp, mWidth) + (szOfEn / 2f);
                        if (DEBUG_TABSPAN) {
                            GLKLogger.error("TabSpan: width of right text '" + rightText + "' is " + mRightWidth + " pixels.");
                        }
                    }
                }
            }
        }

        if (mMultiple != 0) {
            return (mLeftWidth == 0) ?
                    (int) ((float) mMultiple * szOfEn) :
                    (int) Math.ceil(mLeftWidth % ((float) mMultiple * szOfEn));
        }

        if (mAlign != null) {
            if (mAlign == Layout.Alignment.ALIGN_OPPOSITE) {
                // N.B. Seems to be a bug in the Android text layout that
                // means it doesn't correctly measure distance needed for
                // RelativeSizeSpans, and wraps too early. In such cases
                // may want to reduce spacing here a bit.
                float spacing = mWidth - mLeftWidth - mRightWidth;
                return (int)(spacing > 0 ? spacing : 0);
            } else {
                GLKLogger.error("TabSpan: implement ALIGN = " + mAlign);
                return 0;
            }
        }

        return 0;
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull Paint paint) {
        // we don't need to do anything - setting the correct width with the call to getSize will ensure
        // that a space is left blank; that represents our tab
    }
}
