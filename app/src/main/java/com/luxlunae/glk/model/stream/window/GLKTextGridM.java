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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.style.GLKStyleSpan;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class GLKTextGridM extends GLKTextWindowM {
    @Nullable
    private Bitmap mGridBitmap;          // Contains the rendered text grid (by default pixels are TRANSPARENT)
    @Nullable
    private Canvas mGridCanvas;
    @Nullable
    private GLKTextGridLayout mGridLayout;
    private int mCursorCol, mCursorRow;
    private boolean mIsMouseRequested = false;

    private GLKTextGridM(@NonNull GLKTextGridBuilder builder) {
        super(builder);
    }

    public void requestMouseEvent() {
        mIsMouseRequested = true;
    }

    public void cancelMouseEvent() {
        mIsMouseRequested = false;
    }

    public boolean mouseRequested() {
        return mIsMouseRequested;
    }

    @Nullable
    public Bitmap getGridImage() {
        if (mGridCanvas != null && mGridLayout != null &&
                mGridLayout.updateUI(mGridCanvas, mBGColor)) {
            return mGridBitmap;
        }
        return null;
    }

    @Override
    public void resize(int widthPx, int heightPx) {
        // GLK SPEC 3.5.4:
        // When a text grid window is resized smaller, the bottom or right area is
        // thrown away, but the remaining area stays unchanged. When it is resized
        // larger, the new bottom or right area is filled with blanks.
        super.resize(widthPx, heightPx);

        // Clear background colour to current normal style background
        super.clear();

        // Clear any portion of the bitmap that has just been uncovered to the
        // window's current background colour.
        int old_height = 0;
        int old_width = 0;

        if (mGridBitmap != null) {
            old_height = mGridBitmap.getHeight();
            old_width = mGridBitmap.getWidth();
        }

        // Create a new backing bitmap and copy any part of the old bitmap that is still visible
        if ((mHeightGLK_PX != old_height || mWidthGLK_PX != old_width) &&
                mHeightGLK_PX >= 0 && mWidthGLK_PX >= 0) {
            Bitmap tmpBitmap = (mHeightGLK_PX > 0 && mWidthGLK_PX > 0) ?
                    Bitmap.createBitmap(mWidthGLK_PX, mHeightGLK_PX, Bitmap.Config.ARGB_8888) :
                    null;
            Canvas tmpCanvas = tmpBitmap != null ? new Canvas(tmpBitmap) : null;
            if (tmpBitmap != null) {
                tmpBitmap.eraseColor(Color.TRANSPARENT);
            }
            if (mGridBitmap != null) {
                if (tmpCanvas != null) {
                    int min_w = Math.min(mWidthGLK_PX, old_width);
                    int min_h = Math.min(mHeightGLK_PX, old_height);
                    Rect src = new Rect(0, 0, min_w, min_h);
                    RectF dest = new RectF(0, 0, min_w, min_h);
                    tmpCanvas.drawBitmap(mGridBitmap, src, dest, null);
                }
                mGridBitmap.recycle();
            }
            mGridBitmap = tmpBitmap;
            mGridCanvas = tmpCanvas;
        }

        // Create or resize the grid layout
        GLKStyleSpan sty = mWindowStyles[GLKConstants.style_Normal];
        if (mGridLayout == null) {
            mGridLayout = new GLKTextGridLayout(mHeightGLK, mWidthGLK, mLeadingMult, sty);
        } else {
            mGridLayout.resize(mHeightGLK, mWidthGLK, sty);
        }

        mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
    }

    @Override
    public void clear() {
        // GLK Spec 3.7:
        // Clear the window, filling all positions with blanks (in the normal style).
        // The window cursor is moved to the top left corner (position 0,0).
        super.clear();

        // Empty the grid
        if (mWidthGLK > 0 && mHeightGLK > 0) {
            // Set the background of the text grid to the background
            // of the normal style
            GLKStyleSpan sty = mWindowStyles[GLKConstants.style_Normal];
            int mGridBG;
            if (sty.mReverse && sty.mTextColor != Color.TRANSPARENT) {
                mGridBG = sty.mTextColor;
            } else if (sty.mBackColor != Color.TRANSPARENT) {
                mGridBG = sty.mBackColor;
            } else {
                // We need to ensure that the grid is cleared to
                // a solid colour, not Color.TRANSPARENT, to ensure
                // anything already there is completely wiped.
                mGridBG = getWindowBG();
            }
            if (mGridCanvas != null) {
                mGridCanvas.drawColor(mGridBG);
            }
            if (mGridLayout != null) {
                mGridLayout.clear();
            }
            mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
        }
        mCursorCol = 0;
        mCursorRow = 0;
    }

    @Override
    public void putString(@NonNull String s) {
        super.putString(s);

        int k = 0;
        int sz = s.length();
        while (k < sz) {
            putChar(s.codePointAt(k++));
        }
    }

    /**
     * GLK SPEC 3.5.4:
     * When you print, the characters of the output are laid into the array in
     * order, left to right and top to bottom. When the cursor reaches the end of
     * a line, or if a newline (0x0A) is printed, the cursor goes to the beginning
     * of the next line. The library makes *no* attempt to wrap lines at word
     * breaks. If the cursor reaches the end of the last line, further printing
     * has no effect on the window until the cursor is moved.
     *
     * @param ch
     */
    @Override
    public void putChar(int ch) {
        super.putChar(ch);
        if (mCursorCol >= 0 && mCursorRow >= 0 && mCursorCol < mWidthGLK && mCursorRow < mHeightGLK) {
            if (ch == '\n') {
                ++mCursorRow;
                mCursorCol = 0;
            } else if (mGridLayout != null) {
                mGridLayout.putChar((char) ch, mCursorRow, mCursorCol, mCurrentStyle);
                mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
            } else {
                GLKLogger.warn("GLKTextGridM: dropped a character because grid layout not initialised: " + (char) ch);
                return;
            }
            if (++mCursorCol >= mWidthGLK) {
                ++mCursorRow;
                mCursorCol = 0;
            }
        }
    }

    @Override
    public void startHyperlink(int linkval) {
        if (mGridLayout != null) {
            mGridLayout.setHyperlinkValue(linkval);
        }
    }

    @Override
    public void endHyperlink() {
        if (mGridLayout != null) {
            mGridLayout.setHyperlinkValue(GLKConstants.NULL);
        }
    }

    @Nullable
    public HashMap<Point, Integer> getHyperlinks() {
        if (mGridLayout == null) {
            return null;
        }
        return mGridLayout.getHyperlinks();
    }

    /**
     * Set the cursor position.
     * <p/>
     * If you move the cursor right past the end of a line, it wraps;
     * the next character which is printed will appear at the beginning of the next line.
     * <p/>
     * If you move the cursor below the last line, or when the cursor reaches the end of the
     * last line, it goes "off the screen" and further output has no effect. You must call
     * glk_window_move_cursor() or glk_window_clear() to move the cursor back into the visible region.
     *
     * @param col - column from 0 to mWidthGLK - 1
     * @param row - row from 0 to mHeightGLK - 1
     */
    public void moveCursor(int col, int row) {
        mCursorCol = (col < 0) ? 0 : col;
        mCursorRow = (row < 0) ? 0 : row;
        if (mCursorCol >= mWidthGLK) {
            mCursorCol = 0;
            mCursorRow++;
        }
    }

    @Override
    public void requestLineEvent(@NonNull ByteBuffer buf, int initlen, boolean unicode) {
        super.requestLineEvent(buf, initlen, unicode);
        // Logger.error("FIXME: GLKTextGridM: requestLineEvent");
    }

    @Override
    public void cancelLineEvent(@Nullable GLKEvent event) {
        super.cancelLineEvent(event);
        //Logger.error("FIXME: GLKTextGridM: cancelLineEvent");
    }

    public static class GLKTextGridBuilder extends GLKTextWindowBuilder {
        @NonNull
        public GLKTextGridM build() {
            return new GLKTextGridM(this);
        }
    }
}