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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.MotionEvent;

import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.stream.window.GLKTextGridM;

import java.util.HashMap;

import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_CONTENT_CHANGED;

@SuppressLint("ViewConstructor")
public class GLKTextGridV extends GLKTextWindowV implements HVScrollView.ScrollChild {
    @NonNull
    private final GLKTextGridM mModel;
    @Nullable
    private Bitmap mGridBitmap;          // Contains the rendered text grid (by default pixels are TRANSPARENT)
    @Nullable
    private HashMap<Point, Integer> mHyperlinks;

    public GLKTextGridV(@NonNull Context c, @NonNull GLKTextGridM model) {
        super(c, model);
        mModel = model;
        if (mModel.mGLKWidthMultiplier > 1 || mModel.mGLKHeightMultiplier > 1) {
            setScrollChild(this);  // enable scrolling
        }

        setContentDescription("text grid " + mModel.getStreamId());
    }

    @Override
    public void updateContents() {
        super.updateContents();
        if (mModel.isFlagSet(FLAG_CONTENT_CHANGED)) {
            Bitmap b = mModel.getGridImage();
            mGridBitmap = (b != null) ? Bitmap.createBitmap(b) : null;
            mHyperlinks = mModel.getHyperlinks();
            mModel.clearFlag(FLAG_CONTENT_CHANGED);
            setBackgroundColor(mModel.getBGColor());
            invalidate();
        }
    }

    @Override
    public void updateInputState() {
        // currently we do nothing here
        // Logger.error("FIXME: GLKTextGrid: updateInputState");
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mGridBitmap != null) {
            canvas.drawBitmap(mGridBitmap, mPaddingLeft, mPaddingTop, null);
        }
    }

    @Override
    public int sendKey(int key) {
        GLKLogger.error("FIXME: GLKTextGrid: sendKey");
        return 0;
    }

    @Override
    public void clearInputBuffer() {
        GLKLogger.error("FIXME: GLKTextGrid: clearInputBuffer");
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent arg0) {
        // mouse clicks only valid in text mGridLayout windows and graphics windows
        // In a text mGridLayout window, the val1 and val2 fields are the x and y coordinates of the character
        // that was clicked on. [So val1 is the column, and val2 is the row.] The top leftmost character is considered to be (0,0).
        // Only send the mouse click to the event queue if it was actually requested
        if (mModel.mouseRequested()) {
            // convert pixel coordinates to row / col coordinates
            int col = (int) ((arg0.getX() + getScrollX() - mPaddingLeft) / mModel.mCharWidth);
            int row = (int) ((arg0.getY() + getScrollY() - mPaddingTop - mModel.mLeadingMult) / mModel.mCharHeight);

            //	Logger.debug("Got mouse event on text mGridLayout window at char coordinates (col " + col + ", row " + row + ")");
            GLKEvent ev = new GLKEvent();
            ev.mouseEvent(mModel.getStreamId(), col, row);
            GLKController.postEvent(ev);
            return true;
        } else if (mModel.hyperlinkRequested()) {
            // convert pixel coordinates to row / col coordinates
            int col = (int) ((arg0.getX() + getScrollX() - mPaddingLeft) / mModel.mCharWidth);
            int row = (int) ((arg0.getY() + getScrollY() - mPaddingTop - mModel.mLeadingMult) / mModel.mCharHeight);
            Integer val = (mHyperlinks != null) ? mHyperlinks.get(new Point(row, col)) : null;
            if (val != null) {
                GLKEvent ev = new GLKEvent();
                ev.hyperlinkEvent(mModel.getStreamId(), val);
                GLKController.postEvent(ev);
                return true;
            }
        }
        return super.onSingleTapUp(arg0);
    }

    @Override
    public int getChildWidth() {
        return (mGridBitmap != null) ? mGridBitmap.getWidth() : 0;
    }

    @Override
    public int getChildHeight() {
        return (mGridBitmap != null) ? mGridBitmap.getHeight() : 0;
    }
}