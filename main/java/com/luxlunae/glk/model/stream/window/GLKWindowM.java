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

import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.ViewGroup;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.GLKStream;

import java.io.IOException;
import java.util.EnumSet;

public abstract class GLKWindowM implements GLKStream {
    final EnumSet<ModifierFlag> mModifierFlags = EnumSet.noneOf(ModifierFlag.class);             // used by UI thread to determine what, if any, aspects of the window have changed
    @NonNull
    final GLKModel mModel;
    int mStreamID;
    int mWidthPX, mHeightPX;
    @Nullable
    private
    GLKWindowM mSibling = null;
    @Nullable
    private ViewGroup.LayoutParams mLayoutParams = null;
    private int mRock;
    @Nullable
    private GLKPairM mParent = null;

    GLKWindowM(@NonNull GLKModel m) {
        mModel = m;
    }

    @Override
    public int getRock() {
        return mRock;
    }

    @Override
    public void setRock(int rock) {
        mRock = rock;
    }

    @Override
    public int getStreamId() {
        return mStreamID;
    }

    @Override
    public void setStreamId(int id) {
        mStreamID = id;
    }

    @Override
    public void close() throws IOException {
        if (mModel.mRootWinID == mStreamID) {
            mModel.mRootWinID = GLKConstants.NULL;
        }

        // If this window has a parent, destroy that parent and
        // promote this window's sibling up to take its parent's position.
        GLKPairM o;
        int o_id = getParentId();
        if (o_id != GLKConstants.NULL) {
            o = (GLKPairM) mModel.mStreamMgr.getWindow(o_id);
            setParent(null);  // important to avoid infinite loop!
            if (o != null) {
                o.closeSplit(this);
                if (o.getParentId() == GLKConstants.NULL) {
                    // The pair that is about to be destroyed was the root window
                    GLKWindowM sib = getSibling();
                    mModel.mRootWinID = (sib != null) ? sib.getStreamId() : GLKConstants.NULL;
                }
            }
            mModel.mStreamMgr.closeStream(o_id);
        }
    }

    public int getParentId() {
        return (mParent != null) ? mParent.getStreamId() : GLKConstants.NULL;
    }

    @Nullable
    public GLKPairM getParent() {
        return mParent;
    }

    public void setParent(@Nullable GLKPairM p) {
        if (mParent != p || p == null) {
            mParent = p;
            mModifierFlags.add(ModifierFlag.FLAG_PARENT_CHANGED);
        }
    }

    public int getSiblingId() {
        return (mSibling != null) ? mSibling.getStreamId() : GLKConstants.NULL;
    }

    @Nullable
    public GLKWindowM getSibling() {
        return mSibling;
    }

    public void setSibling(@Nullable GLKWindowM sib) {
        if (mSibling != sib || sib == null) {
            mSibling = sib;
            mModifierFlags.add(ModifierFlag.FLAG_SIBLING_CHANGED);
        }
    }

    public int getWidth() {
        return mWidthPX;
    }

    public int getHeight() {
        return mHeightPX;
    }

    public boolean isFlagSet(ModifierFlag flag) {
        return mModifierFlags.contains(flag);
    }

    public void clearFlag(ModifierFlag flag) {
        mModifierFlags.remove(flag);
    }

    @Nullable
    public ViewGroup.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }

    void setLayoutParams(ViewGroup.LayoutParams lp) {
        mLayoutParams = lp;
        mModifierFlags.add(ModifierFlag.FLAG_LAYOUT_CHANGED);
    }

    @NonNull
    public abstract Point getGLKSize();     // returns width and height in the GLK window's units

    public abstract void resize(int w, int h);

    public enum ModifierFlag {
        FLAG_CONTENT_CHANGED, FLAG_BG_COLOR_CHANGED, FLAG_BG_IMAGE_CHANGED,
        FLAG_PARENT_CHANGED, FLAG_SIBLING_CHANGED, FLAG_CLEARED, FLAG_LAYOUT_CHANGED
    }
}
