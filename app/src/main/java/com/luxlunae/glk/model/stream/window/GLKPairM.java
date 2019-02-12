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
import android.widget.LinearLayout;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.GLKModel;

import java.io.IOException;

public class GLKPairM extends GLKWindowM {
    private final GLKWindowM[] mChildren = new GLKWindowM[2];
    private final int[] mChildPositions = new int[2];
    private int mConsChildPos;                  // The index of the child window (in mChildren) to which the constraint currently applies
    private int mSplitType;
    private int mSplitDirection;
    private int mSplitSize;
    private boolean mBorder;
    private boolean mFixedSizeIsPx;
    private int mKeyID = GLKConstants.NULL;     // The key window determines how mSplitSize is converted into pixels, in case of fixed splits.
    private int mOrientation = LinearLayout.VERTICAL;

    public GLKPairM(@NonNull GLKModel m) {
        super(m);
    }

    public boolean showingBorder() {
        return mBorder;
    }

    public int getOrientation() {
        return mOrientation;
    }

    private void setOrientation(int orientation) {
        mOrientation = orientation;
    }

    private void resizeChildren() {
        // N.B. we steal any space needed for the border from the non-constrained window
        GLKWindowM winCons;
        GLKWindowM winSib;
        if (mConsChildPos == 0) {
            winCons = mChildren[0];
            winSib = mChildren[1];
        } else {
            winCons = mChildren[1];
            winSib = mChildren[0];
        }
        LinearLayout.LayoutParams consParams = (LinearLayout.LayoutParams) winCons.getLayoutParams();
        assert consParams != null;
        int widthCons, heightCons;
        int widthSib, heightSib;

        if (mOrientation == LinearLayout.VERTICAL) {
            // width of children is same, only height is different
            widthCons = widthSib = getWidth();
            if (consParams.weight == 0) {
                // fixed height
                heightCons = Math.min(consParams.height, getHeight());
            } else {
                // percentage height
                heightCons = (int) ((float) mSplitSize / 100f * (float) getHeight());
            }
            heightSib = getHeight() - heightCons;
            if (mBorder) {
                heightSib -= mModel.mBorderHeightPX;
            }
            heightSib = Math.max(0, heightSib);
        } else {
            // height of children is same, only width is different
            heightCons = heightSib = getHeight();
            if (consParams.weight == 0) {
                // fixed height
                widthCons = Math.min(consParams.width, getWidth());
            } else {
                // percentage height
                widthCons = (int) ((float) mSplitSize / 100f * (float) getWidth());
            }
            widthSib = getWidth() - widthCons;
            if (mBorder) {
                widthSib -= mModel.mBorderWidthPX;
            }
            widthSib = Math.max(0, widthSib);
        }
        winCons.resize(widthCons, heightCons);
        winSib.resize(widthSib, heightSib);
    }

    /**
     * Initialise this Pair window.
     *
     * @param method        - the GLK method flags specifying:
     *                      (a) whether border should be shown or not;
     *                      (b) the position of the key window; and
     *                      (c) whether split size allocated to the key window is proportional (%) or fixed (units).
     * @param size          - the split size, interpreted as a % or in units (pixels if fixedSizeIsPX is TRUE, GLK window-dependent units otherwise)
     * @param fixedSizeIsPX - if TRUE and method indicates a fixed split size, size is interpreted as pixels
     * @param keyWin        - the child that will become the key window in the new split
     * @param splitWin      - the child that will become the sibling (non-key) in the new split
     * @return TRUE if the split was created successfully, FALSE otherwise.
     */
    public boolean openSplit(int method, int size, boolean fixedSizeIsPX, @NonNull GLKNonPairM keyWin, @NonNull GLKWindowM splitWin) {
        mKeyID = keyWin.getStreamId();
        mBorder = ((method & GLKConstants.winmethod_BorderMask) == GLKConstants.winmethod_Border);
        mSplitDirection = (method & GLKConstants.winmethod_DirMask);
        mSplitType = (method & GLKConstants.winmethod_DivisionMask);
        mSplitSize = size;
        mWidthPX = splitWin.getWidth();
        mHeightPX = splitWin.getHeight();
        mFixedSizeIsPx = fixedSizeIsPX;

        // Demote split win down the hierarchy, and occupy its old position
        // with this new Pair window, using the same layout parameters as
        // the split window (which becomes a child, along with the new window).
        GLKWindowM v = splitWin.getParent();
        if (v == null) {
            // The split win was the root window:
            // This pair now becomes the new root window.
            mModel.mRootWinID = mStreamID;
            setParent(null);
        } else {
            // The split win was not the root window:
            // This pair now takes the split win's position and the
            // split window becomes its child, along with the new window.
            setLayoutParams(splitWin.getLayoutParams());
            ((GLKPairM) v).replaceChild(splitWin, this);
        }

        // Link this Pair's children to it and to each other
        if (mSplitDirection == GLKConstants.winmethod_Above || mSplitDirection == GLKConstants.winmethod_Left) {
            mChildren[0] = keyWin;
            mChildren[1] = splitWin;
        } else {
            mChildren[0] = splitWin;
            mChildren[1] = keyWin;
        }
        keyWin.setParent(this);
        splitWin.setParent(this);
        keyWin.setSibling(splitWin);
        splitWin.setSibling(keyWin);

        // Set the split direction
        mChildPositions[0] = mSplitDirection;
        switch (mSplitDirection) {
            case GLKConstants.winmethod_Above:
            case GLKConstants.winmethod_Below:
                setOrientation(LinearLayout.VERTICAL);
                mChildPositions[1] = (mSplitDirection == GLKConstants.winmethod_Above ? GLKConstants.winmethod_Below : GLKConstants.winmethod_Above);
                break;
            case GLKConstants.winmethod_Left:
            case GLKConstants.winmethod_Right:
                setOrientation(LinearLayout.HORIZONTAL);
                mChildPositions[1] = (mSplitDirection == GLKConstants.winmethod_Left ? GLKConstants.winmethod_Right : GLKConstants.winmethod_Left);
                break;
            default:
                // error
                return false;
        }

        // Update child sizes and layout info
        return constrain(keyWin);
    }

    void closeSplit(GLKWindowM closeWin) {
        GLKWindowM sibWin = (mChildren[0] == closeWin) ? mChildren[1] : mChildren[0];
        mKeyID = GLKConstants.NULL;

        // Promote the surviving sibling up the hierarchy, to occupy the position
        // and use the same layout parameters as this pair window (which
        // henceforth will be deleted)
        sibWin.setLayoutParams(getLayoutParams());
        GLKWindowM o = getParent();
        if (o == null) {
            // This pair was the root window:
            // Sibling becomes the new root window.
            mModel.mRootWinID = sibWin.getStreamId();
            sibWin.setParent(null);
        } else {
            // This pair was not the root window:
            // Sibling takes this pair's position as a child
            // of this pair's parent.
            ((GLKPairM) o).replaceChild(this, sibWin);
        }

        // Remove this Pair's children
        mChildren[0] = mChildren[1] = null;
        mChildPositions[0] = mChildPositions[1] = GLKConstants.NULL;
        sibWin.resize(mWidthPX, mHeightPX);
    }

    /**
     * Change the size of an existing split - i.e. change the Pair's constraint.
     * The constraint is placed on a child of 'win' as specified by the direction part of the method argument.
     *
     * @param method        - logical-or of a direction constant (winmethod_Above, winmethod_Below, winmethod_Left, winmethod_Right)
     *                      and a split-method constant (winmethod_Fixed, winmethod_Proportional).
     * @param size          - a new size for the split. This value is interpreted differently depending on the values for <code>method</code>.
     * @param fixedSizeIsPX - if fixedSizeIsPX is TRUE then 'size' is interpreted as pixels if the method is fixed (use this
     *                      for TADS banners).  Otherwise 'size' is interpreted in whatever way the underlying window wants -
     *                      e.g. could be lines/cols in case of text buffers or DP in case of graphics windows(standard GLK behaviour)
     * @param keyWin        - which of the children should become the key window. May be <code>GLKConstants.NULL</code> if the key window
     *                      is to be left unchanged.
     * @return TRUE if success, FALSE otherwise.
     */
    public boolean changeSplit(int method, int size, boolean fixedSizeIsPX, @Nullable GLKNonPairM keyWin) {
        if (keyWin != null) {
            mKeyID = keyWin.getStreamId();
        }
        mBorder = ((method & GLKConstants.winmethod_BorderMask) == GLKConstants.winmethod_Border);
        mSplitDirection = (method & GLKConstants.winmethod_DirMask);
        mSplitType = (method & GLKConstants.winmethod_DivisionMask);
        mSplitSize = size;
        mFixedSizeIsPx = fixedSizeIsPX;

        // Update child sizes and layout info
        GLKWindowM win = getChild(mSplitDirection);
        return win != null && constrain(win);
    }

    /**
     * Change the constraint of this Pair window.
     *
     * @param win - the (child) window to constrain.  Its sibling will fill whatever space is remaining after the constraint is satisfied.
     *
     * @return TRUE if the constraint was successfully changed, FALSE otherwise.
     */
    private boolean constrain(GLKWindowM win) {
        GLKWindowM sib;
        if (mChildren[0] == win) {
            mConsChildPos = 0;
            sib = mChildren[1];
        } else {
            mConsChildPos = 1;
            sib = mChildren[0];
        }

        switch (mSplitType) {
            case GLKConstants.winmethod_Fixed:
                // The specified window is constrained to 'newSize' pixels or key window units, or less.
                // Its sibling fills the remaining space, if any.
                int t;
                switch (mSplitDirection) {
                    case GLKConstants.winmethod_Above:
                    case GLKConstants.winmethod_Below:
                        if (mSplitSize == 0) {
                            // Assume the caller wants to hide the specified window.
                            t = 0;
                        } else if (mFixedSizeIsPx) {
                            if (mSplitSize < 0) {
                                // Specified window's height is adjusted to height of its contents.
                                // Currently size-to-contents is only supported for text buffers.
                                if (win instanceof GLKTextBufferM) {
                                    t = ((GLKTextBufferM) win).getTextHeight();
                                    mSplitSize = t;
                                } else {
                                    GLKLogger.error("GLKPairM: FIXME: we currently only support size-to-contents for text buffers.");
                                    return false;
                                }
                            } else {
                                // Specified window's height is constrained to 'size' pixels of pair window's space.
                                int top = 0;
                                int bot = 0;
                                if (win instanceof GLKNonPairM) {
                                    top = ((GLKNonPairM) win).mPaddingTopPx;
                                    bot = ((GLKNonPairM) win).mPaddingBottomPx;
                                }
                                t = mSplitSize + top + bot;
                            }
                        } else {
                            // Specified window's height is constrained to 'size' key window units (e.g. rows).
                            // N.B. GLK Spec 3.3:  "The key window also cannot be a pair window itself."
                            GLKNonPairM key = mModel.mStreamMgr.getNonPairWindow(mKeyID);
                            if (key != null) {
                                t = key.getPreferredHeight(mSplitSize, mModel.getApplicationContext().getResources().getDisplayMetrics());
                            } else {
                                // the key no longer exists
                                // this should not happen if the game has been programmed correctly
                                // according to GLK Spec 3.2, in such cases we default to 0
                                GLKLogger.warn("GLKPairM: Key no longer exists for pair window " + mStreamID + "! This should not normally happen if the game has been programmed correctly.");
                                t = 0;
                                mKeyID = GLKConstants.NULL;
                            }
                        }
                        win.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, t));
                        sib.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 100));
                        resizeChildren();
                        return true;
                    case GLKConstants.winmethod_Left:
                    case GLKConstants.winmethod_Right:
                        if (mSplitSize == 0) {
                            // Assume the caller wants to hide the specified window.
                            t = 0;
                        } else if (mFixedSizeIsPx) {
                            if (mSplitSize < 0) {
                                // Specified window's width is supposed to be resized to width of contents.
                                // But we don't support this.
                                GLKLogger.error("GLKPairM: FIXME: we currently don't support size-to-contents for widths.");
                                return false;
                            } else {
                                int l = 0;
                                int r = 0;
                                if (win instanceof GLKNonPairM) {
                                    l = ((GLKNonPairM) win).mPaddingLeftPx;
                                    r = ((GLKNonPairM) win).mPaddingRightPx;
                                }
                                t = mSplitSize + l + r;
                            }
                        } else {
                            // Specified window's width is constrained to 'size' key window units (e.g. columns).
                            // N.B. GLK Spec 3.3:  "The key window also cannot be a pair window itself."
                            GLKNonPairM key = mModel.mStreamMgr.getNonPairWindow(mKeyID);
                            if (key != null) {
                                t = key.getPreferredWidth(mSplitSize, mModel.getApplicationContext().getResources().getDisplayMetrics());
                            } else {
                                // the key no longer exists
                                // this should not happen if the game has been programmed correctly
                                // according to GLK Spec 3.2, in such cases we default to 0
                                GLKLogger.warn("GLKPairM: Key no longer exists for pair window " + mStreamID + "! This should not normally happen if the game has been programmed correctly.");
                                t = 0;
                                mKeyID = GLKConstants.NULL;
                            }
                        }
                        win.setLayoutParams(new LinearLayout.LayoutParams(t, LinearLayout.LayoutParams.MATCH_PARENT));
                        sib.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100));
                        resizeChildren();
                        return true;
                    default:
                        // Error - winPos is invalid.
                        GLKLogger.error("GLKPairM: constrain: invalid window position: " + mSplitDirection);
                }
                break;
            case GLKConstants.winmethod_Proportional:
                // The specified window takes 'size' % of the pair window's space.
                // Its sibling fills the remaining space (100 - size)%.
                //
                // As this is all a bit confusing, here is explanation from the Android docs:
                //
                //    "LinearLayout also supports assigning a weight to individual children with
                //     the android:layout_weight attribute. This attribute assigns an "importance"
                //     value to a view in terms of how much space it should occupy on the screen. A
                //     larger weight value allows it to expand to fill any remaining space in the parent
                //     view. Child views can specify a weight value, and then any remaining space in the
                //     view group is assigned to children in the proportion of their declared weight. Default weight is zero.
                //
                //     For example, if there are three text fields and two of them declare a weight of 1, while the other is
                //     given no weight, the third text field without weight will not grow and will only occupy the area
                //     required by its content. The other two will expand equally to fill the space remaining after all
                //     three fields are measured. If the third field is then given a weight of 2 (instead of 0), then it
                //     is now declared more important than both the others, so it gets half the total remaining space, while the
                //     first two share the rest equally."
                switch (mSplitDirection) {
                    case GLKConstants.winmethod_Above:
                    case GLKConstants.winmethod_Below:
                        win.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, mSplitSize));
                        sib.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 100 - mSplitSize));
                        resizeChildren();
                        return true;
                    case GLKConstants.winmethod_Left:
                    case GLKConstants.winmethod_Right:
                        win.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, mSplitSize));
                        sib.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 100 - mSplitSize));
                        resizeChildren();
                        return true;
                    default:
                        // Error - winPos is invalid.
                        GLKLogger.error("GLKPairM: constrain: invalid window position: " + mSplitDirection);
                }
                break;
            default:
                // Error - newSplitType is invalid.
                GLKLogger.error("GLKPairM: constrain: invalid new split type: " + mSplitType);
                break;
        }
        return false;
    }

    private void replaceChild(@NonNull GLKWindowM oldChild, @NonNull GLKWindowM newChild) {
        int posOld = (mChildren[0] == oldChild) ? 0 : 1;
        int posSib = (posOld == 0) ? 1 : 0;
        GLKWindowM sib = mChildren[posSib];

        // out with old...
        mChildren[posOld] = null;
        oldChild.setParent(null);
        oldChild.setSibling(null);

        // in with the new
        mChildren[posOld] = newChild;
        newChild.setParent(this);
        newChild.setSibling(sib);
        sib.setSibling(newChild);
    }

    public int getSplitMethod() {
        return mSplitType | mSplitDirection;
    }

    public int getSplitSize() {
        return mSplitSize;
    }

    public int getSplitDirection(GLKWindowM childWin) {
        for (int i = 0; i < 2; i++) {
            if (mChildren[i] == childWin) {
                return mChildPositions[i];
            }
        }
        return GLKConstants.NULL;   // not found
    }

    private GLKWindowM getChild(int childPos) {
        for (int i = 0; i < 2; i++) {
            if (mChildPositions[i] == childPos) {
                return mChildren[i];
            }
        }
        return null;   // not found
    }

    public GLKWindowM getChild1() {
        return mChildren[0];
    }

    public GLKWindowM getChild2() {
        return mChildren[1];
    }

    public int getKeyWinID() {
        return mKeyID;
    }

    @NonNull
    @Override
    public Point getGLKSize() {
        // GLK Spec 0.7.4 - section 3.5.2 -
        // "A pair window is completely filled by the two windows it contains.
        // It supports no input and no output, and it has no size."
        return new Point(0, 0);
    }

    @Override
    public void resize(int w, int h) {
        mWidthPX = w;
        mHeightPX = h;
        resizeChildren();
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (mChildren[0] != null) {
            mModel.mStreamMgr.closeStream(mChildren[0].mStreamID);
            mChildren[0] = null;
        }
        if (mChildren[1] != null) {
            mModel.mStreamMgr.closeStream(mChildren[1].mStreamID);
            mChildren[1] = null;
        }
    }
}
