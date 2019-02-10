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

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextPaint;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.style.GLKStyleSpan;

import java.util.ArrayList;
import java.util.HashMap;

class GLKTextGridLayout {
    @NonNull
    private final Paint mHighlightPaint;
    @NonNull
    private final TextPaint mTextPaint;
    private final float mLeadingMult;
    private final char[] mChar = new char[1];
    private final ArrayList<DirtyCell> mDirtyCells = new ArrayList<>(50);  // cache the cells that have changed since last call to flush
    @Nullable
    private HashMap<Point, Integer> mHyperlinks;
    private float mLeadingPX;  // leading in pixels
    private float mHalfLeadingPX;
    private float mBaselineOffset;
    private float mAscent;
    private float mDescent;
    private float mCharHeight;
    private float mCharWidth;
    private int mCols;
    private int mRows;
    @Nullable
    private GLKStyleSpan styInsert;         // current style for inserting new chars (maintained across insertions)
    private int mCurHyperlinkVal = 0;       // if non-zero, characters inserted in this grid will be considered hyperlinks with this value

    // styl specifies which style to use for the initial grid
    GLKTextGridLayout(int rows, int cols, float leadingMult, @NonNull GLKStyleSpan styl) {
        mHighlightPaint = new Paint();
        mTextPaint = new TextPaint();
        mLeadingMult = leadingMult;
        resize(rows, cols, styl);
    }

    public void setHyperlinkValue(int val) {
        // 0 = turn off; anything else means turn on with that value
        mCurHyperlinkVal = val;
    }

    @Nullable
    public HashMap<Point, Integer> getHyperlinks() {
        // returns a copy of the current hyperlinks stored in this layout
        // or NULL if there are no hyperlinks
        if (mHyperlinks == null) {
            return null;
        }
        return new HashMap<>(mHyperlinks);
    }

    // styl specifies which style to use for any new rows/columns
    public void resize(int newRows, int newCols, @NonNull GLKStyleSpan styl) {
        mRows = newRows;
        mCols = newCols;
        calculateFontMetricsFromStyle(styl);
    }

    public void clear() {
        mDirtyCells.clear();  // anything that is still pending from before this clear event is now redundant
        styInsert = null;
        if (mHyperlinks != null) {
            mHyperlinks.clear();
            mHyperlinks = null;
        }
    }

    // styl specifies which style to insert the character as
    // N.B. we only store the reference to the style (we don't
    // copy the object), so any changes to the given style after this
    // call and before the update() call will be reflected.
    // If you don't want this to happen, ensure you pass a copy of the
    // style object.
    void putChar(char ch, int row, int col, @NonNull GLKStyleSpan style) {
        markDirty(row, col, ch, style);
        if (mCurHyperlinkVal != GLKConstants.NULL) {
            if (mHyperlinks == null) {
                mHyperlinks = new HashMap<>();
            }
            mHyperlinks.put(new Point(row, col), mCurHyperlinkVal);
        } else if (mHyperlinks != null) {
            mHyperlinks.remove(new Point(row, col));
        }
    }

    // returns true if window needs to be redrawn; false otherwise
    boolean updateUI(@NonNull Canvas canvas, int winBG) {
        // Flush dirty bits of this grid out to the canvas
        // Cells with transparent backgrounds are drawn with the highlight colour 'winBG'
        //Logger.error("Updating grid: " + mDirtyCells.size() + " dirty cells.");
        float t, b, l, r;
        float row, col;
        int lastRow = mRows - 1;
        int lastCol = mCols - 1;
        boolean redraw = (mDirtyCells.size() > 0);

        for (DirtyCell cell : mDirtyCells) {
            // Does this dirty cell still exist?  If not, just drop it.
            row = cell.mRow;
            col = cell.mCol;
            if (row > lastRow || col > lastCol) {
                GLKLogger.error("dropped dirty cell at (" + col + " c, " + row + " r) - outside grid range");
                continue;
            }

            // Get the cell details
            // If the style is null, we assume it hasn't changed since the last draw
            mChar[0] = cell.mChar;
            if (cell.mTextStyle != null) {
                cell.mTextStyle.updateDrawState(mTextPaint, winBG);
            }

            // Work out where we need to draw this cell
            // If we're in the right-most column or bottom row,
            // we extend the highlight rectangle slightly to cover any round-off
            t = row * mCharHeight + mBaselineOffset;
            l = (col * mCharWidth);
            b = (row == lastRow) ? canvas.getHeight() : t + mDescent + mHalfLeadingPX;
            r = (col == lastCol) ? canvas.getWidth() : l + mCharWidth;

            // Draw highlight rectangle
            // (Yes, we have to do this even if the background colour is transparent,
            // otherwise the text just gets drawn over whatever else is already in the supplied canvas
            // and we get a jumbled mess)
            mHighlightPaint.setColor(mTextPaint.bgColor != Color.TRANSPARENT ? mTextPaint.bgColor : winBG);
            canvas.drawRect(l, t - mAscent - (row == 0 ? mLeadingPX : mHalfLeadingPX), r, b, mHighlightPaint);

            // Draw text
            canvas.drawText(mChar, 0, 1, l, t, mTextPaint);
        }

        mDirtyCells.clear();
        styInsert = null;

        return redraw;
    }

    private void calculateFontMetricsFromStyle(@NonNull GLKStyleSpan styl) {
        TextPaint paint = new TextPaint();
        styl.updateDrawState(paint);
        mCharWidth = paint.measureText("0") * (1f + styl.mLetterSpacing);
        Paint.FontMetrics fm = paint.getFontMetrics();
        mAscent = -fm.ascent;
        mDescent = fm.descent;
        mLeadingPX = (mLeadingMult * (mAscent + mDescent)) - mAscent - mDescent;
        mCharHeight = (mLeadingMult * (mAscent + mDescent));
        mHalfLeadingPX = 0.5f * mLeadingPX;
        mBaselineOffset = mLeadingPX + mAscent;
    }

    private void markDirty(int row, int col, char ch, @NonNull GLKStyleSpan style) {
        // To save memory, we only save the style reference if its
        // underlying object has changed since the last call to this function
        if (!style.equals(styInsert)) {
            styInsert = new GLKStyleSpan(style);
            mDirtyCells.add(new DirtyCell(row, col, ch, styInsert));
        } else {
            mDirtyCells.add(new DirtyCell(row, col, ch, null));
        }
    }

    private static class DirtyCell {
        final int mRow;
        final int mCol;
        final char mChar;
        final GLKStyleSpan mTextStyle;

        DirtyCell(int row, int col, char ch, GLKStyleSpan styl) {
            mRow = row;
            mCol = col;
            mChar = ch;
            mTextStyle = styl;
        }
    }
}
