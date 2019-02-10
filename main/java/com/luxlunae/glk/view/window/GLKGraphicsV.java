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
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.stream.window.GLKGraphicsM;

import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_CONTENT_CHANGED;

@SuppressLint("ViewConstructor")
public class GLKGraphicsV extends GLKNonPairV implements HVScrollView.ScrollChild, GLKWindowV {
    @NonNull
    private final GLKGraphicsM mModel;
    private Bitmap mGraphicsBitmap;                // The background bitmap for this window

    public GLKGraphicsV(@NonNull Context c, @NonNull GLKGraphicsM model) {
        super(c, model);
        mModel = model;
        if (mModel.mGLKWidthMultiplier > 1 || mModel.mGLKHeightMultiplier > 1) {
            setScrollChild(this);  // enable scrolling
        }
        setContentDescription("graphics " + mModel.getStreamId());
    }

    @Override
    public void updateContents() {
        super.updateContents();
        if (mModel.isFlagSet(FLAG_CONTENT_CHANGED)) {
            mGraphicsBitmap = Bitmap.createBitmap(mModel.getBitmap());
            mModel.clearFlag(FLAG_CONTENT_CHANGED);
            invalidate();
        }
    }

    @Override
    public void updateInputState() {
        // Logger.error("FIXME: graphics window - update input state");
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mGraphicsBitmap != null) {
            canvas.drawBitmap(mGraphicsBitmap, mPaddingLeft, mPaddingTop, null);
        }
    }

    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent arg0) {
        // mouse clicks only valid in text grid windows and graphics windows
        // In a graphics window, they are the x and y coordinates of the pixel that
        // was clicked on. Again, the top left corner of the window is (0,0).
        // Only send the mouse click to the event queue if it was actually requested
        if (mModel.mouseRequested()) {
            //Logger.debug("Got mouse event on graphics window at pixel coordinates (" + (int)arg0.getX() + ", " + (int)arg0.getY() + ")");
            GLKEvent ev = new GLKEvent();
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int x = (int) (arg0.getX() + getScrollX() - (float) mPaddingLeft);
            int y = (int) (arg0.getY() + getScrollY() - (float) mPaddingTop);

            if (x > 0 && y > 0) {
                ev.mouseEvent(mModel.getStreamId(), GLKUtils.pxToDp(x, dm), GLKUtils.pxToDp(y, dm));
                GLKController.postEvent(ev);
            }
        }
        return true;
    }

    @Override
    public int getChildWidth() {
        return (mGraphicsBitmap != null) ? mGraphicsBitmap.getWidth() : 0;
    }

    @Override
    public int getChildHeight() {
        return (mGraphicsBitmap != null) ? mGraphicsBitmap.getHeight() : 0;
    }
}
