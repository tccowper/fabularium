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
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.model.GLKDrawable;

public class GLKGraphicsM extends GLKNonPairM {
    private Bitmap mBufferedBitmap;
    private Canvas mBufferedCanvas;
    private boolean mIsMouseRequested = false;
    @Nullable
    private Integer mPendingBGColor = null;

    private GLKGraphicsM(@NonNull GLKGraphicsBuilder builder) {
        super(builder);
    }

    public Bitmap getBitmap() {
        return mBufferedBitmap;
    }

    public boolean mouseRequested() {
        return mIsMouseRequested;
    }

    public void requestMouseEvent() {
        mIsMouseRequested = true;
    }

    public void cancelMouseEvent() {
        mIsMouseRequested = false;
    }

    public void setPendingWindowBG(int bg) {
        // We need to override this function, because in the case of GLKGraphicsM windows
        // the effect does not take place immediately, rather on the next resize or call to clear().
        // See GLK Spec 7.2
        mPendingBGColor = bg | 0xFF000000;
    }

    public void fillRect(@NonNull Rect rectPx, @ColorInt int colour) {
        // Draw a rectangle with the given colour
        // again we have to ensure top 8 bits are set...!!
        if (mBufferedCanvas == null) {
            GLKLogger.error("GLKGraphicsM: fillRect called when buffered bitmap is null.");
            return;
        }
        Paint p = new Paint();
        p.setColor(colour | 0xFF000000);
        mBufferedCanvas.drawRect(rectPx, p);
        mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
    }

    public void eraseRect(@NonNull Rect rectPx) {
        // Draw a rectangle with the background colour
        if (mBufferedCanvas == null) {
            GLKLogger.error("GLKGraphicsM: eraseRect called when buffered bitmap is null.");
            return;
        }
        Paint p = new Paint();
        p.setColor(Color.TRANSPARENT);
        mBufferedCanvas.drawRect(rectPx, p);
        mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
    }

    @Override
    public void clear() {
        super.clear();

        // Process any pending background colour change
        if (mPendingBGColor != null) {
            postClearWindowBackground(mPendingBGColor);
            mPendingBGColor = null;
        }

        // Clear the backing bitmaps
        if (mBufferedBitmap != null) {
            mBufferedBitmap.eraseColor(Color.TRANSPARENT);
            mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
        }
    }

    @Override
    public void resize(int widthPx, int heightPx) {
        super.resize(widthPx, heightPx);

        // GLK SPEC 3.5.5:
        // When a graphics window is resized smaller, the bottom or right area is
        // thrown away, but the remaining area stays unchanged. When it is resized
        // larger, the new bottom or right area is filled with the background color.

        // In mWidthGLK and mHeightGLK we store the dp equivalent of the pixel width and height:
        // Those fields are reported to terps and games when glk_get_window_size() is called.
        DisplayMetrics dm = mModel.getApplicationContext().getResources().getDisplayMetrics();
        mWidthGLK = GLKUtils.pxToDp(mWidthGLK_PX, dm);
        mHeightGLK = GLKUtils.pxToDp(mHeightGLK_PX, dm);

        // Clear the portion of the bitmap that has just been covered or uncovered to the
        // window's current background colour.
        int old_height = 0;
        int old_width = 0;

        if (mBufferedBitmap != null) {
            old_height = mBufferedBitmap.getHeight();
            old_width = mBufferedBitmap.getWidth();
        }

        // Create a new backing bitmap and, if necessary, copy of the bit of the old
        // bitmap that is still visible
        if ((mHeightGLK_PX != old_height || mWidthGLK_PX != old_width) && (mHeightGLK_PX > 0 && mWidthGLK_PX > 0)) {
            Bitmap tmpBitmap = Bitmap.createBitmap(mWidthGLK_PX, mHeightGLK_PX, Bitmap.Config.ARGB_8888);
            Canvas tmpCanvas = new Canvas(tmpBitmap);
            tmpBitmap.eraseColor(Color.TRANSPARENT);
            if (mBufferedBitmap != null) {
                int min_w = Math.min(mWidthGLK_PX, old_width);
                int min_h = Math.min(mHeightGLK_PX, old_height);
                Rect src = new Rect(0, 0, min_w, min_h);
                RectF dest = new RectF(0, 0, min_w, min_h);
                tmpCanvas.drawBitmap(mBufferedBitmap, src, dest, null);
                mBufferedBitmap.recycle();
            }
            mBufferedBitmap = tmpBitmap;
            mBufferedCanvas = tmpCanvas;
            mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
        }
    }

    public void drawPicture(@NonNull GLKDrawable img, int leftPx, int topPx, boolean autoresize) {
        // N.B. Unless autoresize is enabled, anything outside of the drawable part of the
        // window (including margins) is clipped.
        if (mBufferedCanvas == null) {
            GLKLogger.error("GLKGraphicsM: drawPicture called when buffered bitmap is null.");
            return;
        }

        if (autoresize) {
            Rect rectPx = img.getBounds();
            if (rectPx.width() > mWidthGLK_PX) {
                // Resize the image if it doesn't fit into the available window's space
                // strictly speaking we shouldn't need to do this, but many games don't
                // calculate dimensions properly
                float factor = (float) mWidthGLK_PX / (float) rectPx.width();
                rectPx.right = (int) ((float) rectPx.right * factor);
                rectPx.bottom = (int) ((float) rectPx.bottom * factor);
                img.setBounds(rectPx);

                // Some games may provide a negative or positive leftPx value for large images
                // (e.g. Beyond). This will lead to either the left or right part of the image
                // being cropped, when the image width exceeds the window width. To ensure that
                // doesn't happen, we need to zero out the leftPx value. We also double-check
                // to ensure the top coordinate is within the window bounds.
                leftPx = 0;
                topPx = Math.max(topPx, 0);
            }
        }

        mBufferedCanvas.save();
        mBufferedCanvas.translate(leftPx, topPx);
        img.draw(mBufferedCanvas);
        mBufferedCanvas.restore();
        mModifierFlags.add(ModifierFlag.FLAG_CONTENT_CHANGED);
    }

    @Override
    public int getPreferredHeight(int dp, @NonNull DisplayMetrics dm) {
        if (dp == 0) {
            // caller wants to hide this window - so return the absolute smallest height we can
            // N.B. we can't return 0
            return 1;
        }
        int px = GLKUtils.dpToPx(dp, dm);
        return (int) ((float) px / mGLKHeightMultiplier) + mPaddingBottomPx + mPaddingTopPx;
    }

    @Override
    public int getPreferredWidth(int dp, @NonNull DisplayMetrics dm) {
        if (dp == 0) {
            // caller wants to hide this window - so return the absolute smallest height we can
            // N.B. we can't return 0
            return 1;
        }
        int px = GLKUtils.dpToPx(dp, dm);
        return (int) ((float) px / mGLKWidthMultiplier) + mPaddingLeftPx + mPaddingRightPx;
    }

    @Override
    public void startHyperlink(int linkval) {
        GLKLogger.error("TODO: GLKGraphicsM: startHyperlink");
    }

    @Override
    public void endHyperlink() {
        GLKLogger.error("TODO: GLKGraphicsM: endHyperlink");
    }

    public static class GLKGraphicsBuilder extends GLKNonPairBuilder {
        @NonNull
        public GLKGraphicsM build() {
            return new GLKGraphicsM(this);
        }
    }
}
