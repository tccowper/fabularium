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
package com.luxlunae.glk.model.style;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.AlignmentSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.MetricAffectingSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;

public class GLKStyleSpan extends MetricAffectingSpan implements AlignmentSpan, LeadingMarginSpan {

    private static final String DEFAULT_MONO_FONT_FAMILY = "monospace";
    private static final String DEFAULT_SERIF_FONT_FAMILY = "serif";

    public final int mStyleID;
    private float mDefaultTextSize;
    private final int mWinBGColor;
    private final int mDefaultTypeface;
    public int mTextColor;
    public int mBackColor;
    public boolean mReverse;
    public boolean mIsMono;
    public float mLetterSpacing = 0;
    private int mStyle, mFirst, mBase;
    private float mTextSizePX;
    private int mDefaultTextColor;
    private int mDefaultBackColor;
    private Layout.Alignment mAlignment;
    private boolean mNoStyleHints;    // If this style is locked, games cannot override with style hints
    private Typeface[] mTypefaces = new Typeface[2];    // 0 = mono typeface, 1 = proportional
    private int mCurrentTypeface;                       // which typeface in mTypefaces we are currently using

    public GLKStyleSpan(int styl, boolean mono, float size) {
        // Create the default for the specified style
        mStyleID = styl;
        mStyle = Typeface.NORMAL;
        mFirst = mBase = 0;
        mAlignment = Layout.Alignment.ALIGN_NORMAL;
        mIsMono = mono;
        mReverse = false;
        if (styl == GLKConstants.style_Preformatted || styl == GLKConstants.style_BlockQuote) {
            mCurrentTypeface = mDefaultTypeface = 0;
            mIsMono = true;
        } else {
            mCurrentTypeface = mDefaultTypeface = (mono ? 0 : 1);
        }

        mDefaultTextSize = mTextSizePX = size;
        mTextColor = GLKConstants.GLK_DEFAULT_TEXT_COLOUR;
        mBackColor = Color.TRANSPARENT;
        mWinBGColor = GLKConstants.GLK_DEFAULT_BACK_COLOUR;

        switch (styl) {
            case GLKConstants.style_Normal:
                break;
            case GLKConstants.style_Emphasized:
                mStyle |= Typeface.ITALIC;
                break;
            case GLKConstants.style_Preformatted:
                break;
            case GLKConstants.style_Header:
                mStyle |= Typeface.BOLD;
                mTextSizePX *= 1.1f;  // increase size by 10%
                break;
            case GLKConstants.style_Subheader:
                mStyle |= Typeface.BOLD;
                mTextSizePX *= 1.05f;  // increase size by 5%
                break;
            case GLKConstants.style_Alert:
                mStyle |= Typeface.BOLD;
                mTextColor = Color.RED;
                break;
            case GLKConstants.style_Note:
                mStyle |= Typeface.ITALIC;
                break;
            case GLKConstants.style_BlockQuote:
                break;
            case GLKConstants.style_Input:
                mTextColor = GLKConstants.GLK_DEFAULT_INPUT_COLOUR;
                break;
            case GLKConstants.style_User1:
                break;
            case GLKConstants.style_User2:
                break;
            default:
                break;
        }

        mDefaultTextColor = mTextColor;
        mDefaultBackColor = mBackColor;
        mTypefaces[0] = Typeface.create(DEFAULT_MONO_FONT_FAMILY, mStyle);
        mTypefaces[1] = Typeface.create(DEFAULT_SERIF_FONT_FAMILY, mStyle);
    }

    public GLKStyleSpan(@NonNull GLKStyleSpan styl) {
        // creates a copy of the specified style
        this.mStyleID = styl.mStyleID;
        this.mCurrentTypeface = styl.mCurrentTypeface;
        this.mDefaultTypeface = styl.mDefaultTypeface;
        this.mTypefaces = styl.mTypefaces;
        this.mIsMono = styl.mIsMono;
        this.mNoStyleHints = styl.mNoStyleHints;
        this.mStyle = styl.mStyle;
        this.mTextSizePX = styl.mTextSizePX;
        this.mFirst = styl.mFirst;
        this.mBase = styl.mBase;
        this.mTextColor = styl.mTextColor;
        this.mDefaultTextColor = styl.mDefaultTextColor;
        this.mBackColor = styl.mBackColor;
        this.mDefaultBackColor = styl.mDefaultBackColor;
        this.mReverse = styl.mReverse;
        this.mAlignment = styl.mAlignment;
        this.mWinBGColor = styl.mWinBGColor;
        this.mLetterSpacing = styl.mLetterSpacing;
        this.mDefaultTextSize = styl.mDefaultTextSize;
    }

    /**
     * @param styles     - the styles array to fill
     * @param fixed      - whether the styles should be fixed width or not
     * @param baseSizeSP - should be in scale-independent pixels (SP)
     */
    public static void getDefaultStyles(@NonNull GLKStyleSpan[] styles, boolean fixed, float baseSizeSP, @NonNull Context c) {
        // Fills styles with the defaults
        Resources r = c.getResources();
        float baseSizePX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, baseSizeSP, r.getDisplayMetrics());
        styles[GLKConstants.style_Normal] = new GLKStyleSpan(GLKConstants.style_Normal, fixed, baseSizePX);
        styles[GLKConstants.style_Emphasized] = new GLKStyleSpan(GLKConstants.style_Emphasized, fixed, baseSizePX);
        styles[GLKConstants.style_Preformatted] = new GLKStyleSpan(GLKConstants.style_Preformatted, true, baseSizePX);
        styles[GLKConstants.style_Header] = new GLKStyleSpan(GLKConstants.style_Header, fixed, baseSizePX);
        styles[GLKConstants.style_Subheader] = new GLKStyleSpan(GLKConstants.style_Subheader, fixed, baseSizePX);
        styles[GLKConstants.style_Alert] = new GLKStyleSpan(GLKConstants.style_Alert, fixed, baseSizePX);
        styles[GLKConstants.style_Note] = new GLKStyleSpan(GLKConstants.style_Note, fixed, baseSizePX);
        styles[GLKConstants.style_BlockQuote] = new GLKStyleSpan(GLKConstants.style_BlockQuote, fixed, baseSizePX);
        styles[GLKConstants.style_Input] = new GLKStyleSpan(GLKConstants.style_Input, fixed, baseSizePX);
        styles[GLKConstants.style_User1] = new GLKStyleSpan(GLKConstants.style_User1, fixed, baseSizePX);
        styles[GLKConstants.style_User2] = new GLKStyleSpan(GLKConstants.style_User2, fixed, baseSizePX);
    }

    public static Object getLast(@NonNull Spanned text, Class kind) {
        /*
         * This knows that the last returned object from getSpans()
         * will be the most recently added.
         */
        Object[] objs = text.getSpans(0, text.length(), kind);
        if (objs.length == 0) {
            return null;
        } else {
            return objs[objs.length - 1];
        }
    }

    public static void start(@NonNull SpannableStringBuilder text, Object mark) {
        int len = text.length();
        text.setSpan(mark, len, len, Spannable.SPAN_MARK_MARK);
    }

    public static void end(@NonNull SpannableStringBuilder text, Class kind, Object repl) {
        Object obj = getLast(text, kind);
        if (obj != null) {
            int len = text.length();
            int where = text.getSpanStart(obj);
            text.removeSpan(obj);
            if (where < len) {
                text.setSpan(repl, where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static boolean get_stylehint(int hint, @NonNull int[] result, @NonNull GLKStyleSpan sty) {
        switch (hint) {
            case GLKConstants.stylehint_BackColor:
                // set the top eight bits back to zero
                result[0] = sty.mBackColor & 0x00FFFFFF;
                break;
            case GLKConstants.stylehint_TextColor:
                // set the top eight bits back to zero
                result[0] = sty.mTextColor & 0x00FFFFFF;
                break;
            case GLKConstants.stylehint_Size:
                GLKLogger.error("TODO: get_stylehint: stylehint_Size.");
                break;
            case GLKConstants.stylehint_ReverseColor:
                result[0] = sty.mReverse ? 1 : 0;
                break;
            case GLKConstants.stylehint_Weight:
                result[0] = ((sty.mStyle & Typeface.BOLD) == Typeface.BOLD) ? 0 : 1;
                break;
            case GLKConstants.stylehint_Oblique:
                result[0] = ((sty.mStyle & Typeface.ITALIC) == Typeface.ITALIC) ? 0 : 1;
                break;
            case GLKConstants.stylehint_Proportional:
                result[0] = sty.mIsMono ? 0 : 1;
                break;
            default:
                return false;
        }
        return true;
    }

    public static void clear_stylehint(int hint, @NonNull GLKStyleSpan sty) {
        if (sty.mNoStyleHints) {
            return;
        }

        switch (hint) {
            case GLKConstants.stylehint_Indentation:
                sty.mBase = 0;
                break;
            case GLKConstants.stylehint_ParaIndentation:
                sty.mFirst = 0;
                break;
            case GLKConstants.stylehint_Justification:
                sty.mAlignment = Layout.Alignment.ALIGN_NORMAL;
                break;
            case GLKConstants.stylehint_Size:
                sty.mTextSizePX = sty.mDefaultTextSize;
                break;
            case GLKConstants.stylehint_Weight:
                sty.mStyle &= ~Typeface.BOLD;
                sty.applyStyle();
                break;
            case GLKConstants.stylehint_Oblique:
                sty.mStyle &= ~Typeface.ITALIC;
                sty.applyStyle();
                break;
            case GLKConstants.stylehint_Proportional:
                sty.mCurrentTypeface = sty.mDefaultTypeface;
                break;
            case GLKConstants.stylehint_TextColor:
                sty.mTextColor = sty.mDefaultTextColor;
                break;
            case GLKConstants.stylehint_BackColor:
                sty.mBackColor = sty.mDefaultBackColor;
                break;
            case GLKConstants.stylehint_ReverseColor:
                sty.mReverse = false;
                break;
        }
    }

    // if makeDefault is TRUE, any call to clear_stylehint will clear back to this setting
    public static void set_stylehint(int hint, int val, @NonNull GLKStyleSpan sty, boolean makeDefault) {
        if (sty.mNoStyleHints) {
            return;
        }

        switch (hint) {
            case GLKConstants.stylehint_Indentation: {
                // How much to indent lines of text in the given
                // style. May be a negative number, to shift the text out (left) instead of in
                // (right). The exact metric isn't precisely specified; you can assume that +1
                // is the smallest indentation possible which is clearly visible to the player.
                TextPaint tp = new TextPaint();
                sty.updateMeasureState(tp);
                float width = tp.measureText("m");
                sty.mBase = (int) (val * width);
                break;
            }
            case GLKConstants.stylehint_ParaIndentation: {
                // How much to indent the first line of each
                // paragraph. This is in addition to the indentation specified by
                // stylehint_Indentation. This too may be negative, and is measured in the
                // same units as stylehint_Indentation.
                TextPaint tp = new TextPaint();
                sty.updateMeasureState(tp);
                float width = tp.measureText("m");
                sty.mFirst = (int) (val * width);
                break;
            }
            case GLKConstants.stylehint_Justification:
                // The value of this hint must be one of the
                // constants stylehint_just_LeftFlush, stylehint_just_LeftRight (full
                // justification), stylehint_just_Centered, or stylehint_just_RightFlush.
                switch (val) {
                    case GLKConstants.stylehint_just_LeftFlush:
                        sty.mAlignment = Layout.Alignment.ALIGN_NORMAL;
                        break;
                    case GLKConstants.stylehint_just_LeftRight:
                        // We don't currently support this and probably never will
                        GLKLogger.error("TODO: set_stylehint called with full justification.");
                        break;
                    case GLKConstants.stylehint_just_Centered:
                        sty.mAlignment = Layout.Alignment.ALIGN_CENTER;
                        break;
                    case GLKConstants.stylehint_just_RightFlush:
                        sty.mAlignment = Layout.Alignment.ALIGN_OPPOSITE;
                        break;
                }
                break;
            case GLKConstants.stylehint_Size:
                // How much to increase or decrease the font size. This is relative; 0 means the interpreter's default
                // font size will be used, positive numbers increase it, and negative numbers decrease it. Again, +1 is the
                // smallest size increase which is easily visible. [The amount of this increase may not be constant. +1 might
                // increase an 8-point font to 9-point, but a 16-point font to 18-point.]
                if (val == 0) {
                    sty.mTextSizePX = sty.mDefaultTextSize;
                } else {
                    GLKLogger.error("TODO: set_stylehint: change text size: " + val);
                    //   sty.mTextSizePX = (int) ((double) val * (double) sty.mTextSizePX);
                }
                break;
            case GLKConstants.stylehint_Weight:
                // The value of this hint must be 1 for heavy-weight fonts (boldface), 0 for normal weight, and -1
                // for light-weight fonts.
                switch (val) {
                    case -1:
                        sty.mStyle &= ~Typeface.BOLD;
                        sty.applyStyle();
                        break;
                    case 0:
                        sty.mStyle &= ~Typeface.BOLD;
                        sty.applyStyle();
                        break;
                    case 1:
                        sty.mStyle |= Typeface.BOLD;
                        sty.applyStyle();
                        break;
                }
                break;
            case GLKConstants.stylehint_Oblique:
                // The value of this hint must be 1 for oblique fonts (italic), or 0 for normal angle.
                switch (val) {
                    case 0:
                        sty.mStyle &= ~Typeface.ITALIC;
                        sty.applyStyle();
                        break;
                    case 1:
                        sty.mStyle |= Typeface.ITALIC;
                        sty.applyStyle();
                        break;
                }
                break;
            case GLKConstants.stylehint_Proportional:
                // The value of this hint must be 1 for proportional-width fonts, or 0 for fixed-width.
                switch (val) {
                    case 0:
                        sty.mIsMono = true;
                        sty.mCurrentTypeface = 0;
                        break;
                    case 1:
                        sty.mIsMono = false;
                        sty.mCurrentTypeface = 1;
                        break;
                }
                break;
            case GLKConstants.stylehint_TextColor:
                // The foreground color of the text. This is encoded in the 32-bit hint value: the top 8 bits must be zero, the next
                // 8 bits are the red value, the next 8 bits are the green value, and the bottom 8 bits are the blue value. Color
                // values range from 0 to 255.
                // To convert to an Android colour we need to set the top 8 bits to ones, this ensures opaqueness
                // From the developer docs:
                //   "Colors are represented as packed ints, made up of 4 bytes: alpha, red, green, blue. The values are
                //   unpremultiplied, meaning any transparency is stored solely in the alpha component, and not in the
                //   color components. The components are stored as follows (alpha << 24) | (red << 16) | (green << 8) | blue.
                //   Each component ranges between 0..255 with 0 meaning no contribution for that component, and 255 meaning 100% contribution.
                //   Thus opaque-black would be 0xFF000000 (100% opaque but no contributions from red, green, or blue), and opaque-white
                //   would be 0xFFFFFFFF."
                sty.mTextColor = (val | 0xFF000000);
                if (makeDefault) {
                    sty.mDefaultTextColor = sty.mTextColor;
                }
                break;
            case GLKConstants.stylehint_BackColor:
                // The background color behind the text. This is encoded the same way as stylehint_TextColor.
                sty.mBackColor = (val | 0xFF000000);
                if (makeDefault) {
                    sty.mDefaultBackColor = sty.mBackColor;
                }
                break;
            case GLKConstants.stylehint_ReverseColor:
                // The value of this hint must be 0 for normal printing (TextColor on BackColor), or 1 for reverse printing (BackColor on TextColor).
                sty.mReverse = (val != 0);
                break;
        }
    }

    /**
     * If the foreground and background colours are the same,
     * brighten or darken this style's foreground colour so it is readable over the current
     * background colour.  If they are not the same, nothing is changed.
     *
     * @param sty - style to analyse.
     */
    public static void ensureIsReadable(@NonNull GLKStyleSpan sty) {
        if (sty.mNoStyleHints) {
            return;
        }

        if (sty.mTextColor == sty.mBackColor) {
            // We need to brighten or darken the foreground colour
            float[] hsv = new float[3];
            if (!sty.mReverse) {
                Color.colorToHSV(sty.mBackColor, hsv);
                if (sty.mTextColor == Color.WHITE) {
                    hsv[2] = 0;
                } else {
                    hsv[2] = 255;
                }
                sty.mTextColor = Color.HSVToColor(hsv);
            } else {
                Color.colorToHSV(sty.mTextColor, hsv);
                if (sty.mBackColor == Color.WHITE) {
                    hsv[2] = 0;
                } else {
                    hsv[2] = 255;
                }
                sty.mBackColor = Color.HSVToColor(hsv);
            }
        }
    }

    public void setTypeface(Typeface typeface) {
        int i = mIsMono ? 0 : 1;
        mTypefaces[i] = typeface;
        mCurrentTypeface = i;
        mStyle = Typeface.NORMAL;
    }

    private void applyStyle() {
        mTypefaces[mCurrentTypeface] = Typeface.create(mTypefaces[mCurrentTypeface], mStyle);
    }

    public void setTextSize(float textSizeSP, DisplayMetrics dm, boolean makeDefault) {
        mTextSizePX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, textSizeSP, dm);
        if (makeDefault) {
            mDefaultTextSize = mTextSizePX;
        }
    }

    public void setColor(@ColorInt int fg, @ColorInt int bg) {
        mTextColor = mDefaultTextColor = fg;
        mBackColor = mDefaultBackColor = bg;
    }

    public void setSpacing(float letterSpacing) {
        mLetterSpacing = letterSpacing;
    }

    public void lock(boolean on) {
        mNoStyleHints = on;
    }

    @Override
    public boolean equals(@Nullable Object styl) {
        if (styl instanceof GLKStyleSpan) {
            GLKStyleSpan s = (GLKStyleSpan) styl;
            return (this.mStyleID == s.mStyleID &&
                    this.mCurrentTypeface == s.mCurrentTypeface &&
                    this.mDefaultTypeface == s.mDefaultTypeface &&
                    this.mTypefaces[0].equals(s.mTypefaces[0]) &&
                    this.mTypefaces[1].equals(s.mTypefaces[1]) &&
                    this.mIsMono == s.mIsMono &&
                    this.mStyle == s.mStyle &&
                    this.mTextSizePX == s.mTextSizePX &&
                    this.mDefaultTextSize == s.mDefaultTextSize &&
                    this.mFirst == s.mFirst &&
                    this.mBase == s.mBase &&
                    this.mTextColor == s.mTextColor &&
                    this.mDefaultTextColor == s.mDefaultTextColor &&
                    this.mBackColor == s.mBackColor &&
                    this.mDefaultBackColor == s.mDefaultBackColor &&
                    this.mReverse == s.mReverse &&
                    this.mAlignment == s.mAlignment &&
                    this.mWinBGColor == s.mWinBGColor &&
                    this.mLetterSpacing == s.mLetterSpacing);
        }
        return false;
    }

    @Override
    public Layout.Alignment getAlignment() {
        return mAlignment;
    }

    @Override
    public void updateMeasureState(@NonNull TextPaint ds) {
        /* Assumes that the ds argument represents the "normal style" */
        Typeface cur = mTypefaces[mCurrentTypeface];
        if (cur != null) {
            ds.setTypeface(cur);

            // If the chosen typeface does not support bold and/or italic,
            // do fake effects to emulate
            int fake = mStyle & ~cur.getStyle();
            if ((fake & Typeface.BOLD) != 0) {
                ds.setFakeBoldText(true);
            }
            if ((fake & Typeface.ITALIC) != 0) {
                ds.setTextSkewX(-0.25f);
            }
        }

        if (mTextSizePX > 0) {
            ds.setTextSize(mTextSizePX);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mLetterSpacing > 0) {
                ds.setLetterSpacing(mLetterSpacing);
            }
        }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        updateMeasureState(ds);

        // TODO: we somehow need to work out what the current window background colour is
        // in order to supply a foreground colour, when that is set to transparent
        if (mReverse) {
            if (ds.linkColor == Color.TRANSPARENT) {
                ds.setColor(mBackColor != Color.TRANSPARENT ? mBackColor : mWinBGColor);
            }
            ds.bgColor = mTextColor;
        } else {
            if (ds.linkColor == Color.TRANSPARENT) {
                ds.setColor(mTextColor != Color.TRANSPARENT ? mTextColor : mWinBGColor);
            }
            ds.bgColor = mBackColor;
        }
    }

    public void updateDrawState(@NonNull TextPaint ds, int bgColor) {
        updateMeasureState(ds);
        if (mReverse) {
            if (ds.linkColor == Color.TRANSPARENT) {
                ds.setColor(mBackColor != Color.TRANSPARENT ? mBackColor : bgColor);
            }
            ds.bgColor = mTextColor;
        } else {
            if (ds.linkColor == Color.TRANSPARENT) {
                ds.setColor(mTextColor != Color.TRANSPARENT ? mTextColor : bgColor);
            }
            ds.bgColor = mBackColor;
        }
    }

    @Override
    public int getLeadingMargin(boolean first) {
        return first ? mBase + mFirst : mBase;
    }

    @Override
    public void drawLeadingMargin(Canvas canvas, Paint paint, int i, int i1, int i2, int i3,
                                  int i4, CharSequence charSequence, int i5, int i6, boolean b, Layout layout) {
        // For now we do nothing
    }
}
