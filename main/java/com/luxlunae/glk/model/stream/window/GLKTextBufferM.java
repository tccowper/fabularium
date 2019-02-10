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

import android.graphics.Paint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.GLKDrawable;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.style.GLKHyperlinkSpan;
import com.luxlunae.glk.model.style.GLKStyleSpan;
import com.luxlunae.glk.model.html.Parser;
import com.luxlunae.glk.model.html.State;
import com.luxlunae.glk.model.html.WatchableStringBuilder;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

public class GLKTextBufferM extends GLKTextWindowM {
    // PRETTY PRINT CONSTANTS
    private static final Pattern FIND_EM_DASH = Pattern.compile("--", Pattern.LITERAL);
    private static final String REPLACE_EM_DASH = String.valueOf((char) GLKConstants.EM_DASH);
    private static final Pattern FIND_DOUBLE_QUOTES = Pattern.compile("\"([^\"]*?)\"");
    private static final String REPLACE_DOUBLE_QUOTES = String.valueOf((char) GLKConstants.LDQUOTE) + "$1" + String.valueOf((char) GLKConstants.RDQUOTE);
    private static final Pattern FIND_SINGLE_QUOTE = Pattern.compile("(\\w)'");
    private static final String REPLACE_SINGLE_QUOTE = "$1" + String.valueOf((char) GLKConstants.RSQUOTE);

    // INPUT
    public final StringBuilder mInput = new StringBuilder(); // for constructing mInput
    public final TextPaint mHighlightPaint = new TextPaint();
    @Nullable
    public ByteBuffer mInputBB; // the initial byte buffer to which we post final result
    public boolean mIs32Bit;
    private int mHyperlinkValue = GLKConstants.NULL;

    // OUTPUT
    @NonNull
    private WatchableStringBuilder mOutputBuffer = new WatchableStringBuilder();
    private boolean mFlagDQ = false;
    private boolean mFlagLastW = false;
    private boolean mFlagLastDSH = false;
    @Nullable
    private State mHTMLState = null;
    private boolean mIsInHTMLMode = false;
    private boolean mShowCompletedInput = true;

    private GLKTextBufferM(@NonNull GLKTextBufferBuilder builder) {
        super(builder);
        setStyle(GLKConstants.style_Normal);

        // initialise cursor
        mHighlightPaint.setStyle(Paint.Style.STROKE);
        mHighlightPaint.setStrokeWidth(mCursorWidthPX);
    }

    private static String prettify(@NonNull String s) {
        String ret = FIND_EM_DASH.matcher(s).replaceAll(REPLACE_EM_DASH);
        ret = FIND_SINGLE_QUOTE.matcher(ret).replaceAll(REPLACE_SINGLE_QUOTE);
        ret = FIND_DOUBLE_QUOTES.matcher(ret).replaceAll(REPLACE_DOUBLE_QUOTES);
        return ret;
    }

    public boolean hasShownHTML() {
        return (mHTMLState != null);
    }

    @NonNull
    public SpannableStringBuilder getBuffer() {
        // N.B. caller should NOT modify the returned builder
        // (make a copy first if you need to change it)
        return mOutputBuffer;
    }

    public boolean showCompletedInput() {
        return mShowCompletedInput;
    }

    public void startNewStyle() {
        if (!mIsInHTMLMode) {
            GLKStyleSpan.start(mOutputBuffer, new GLKTextBufferM.StyleMarker());
        }
    }

    public void endCurrentStyle() {
        if (!mIsInHTMLMode) {
            GLKStyleSpan.end(mOutputBuffer, GLKTextBufferM.StyleMarker.class, CharacterStyle.wrap(mCurrentStyle));
        }
    }

    /**
     * Allow or suppress input buffer changes.
     *
     * @param on - if this is false, all *subsequent* line input requests in
     *           the given window will collect the player's input in the buffer
     *           but not add any new lines; and this input will not be printed
     *           when the line is complete or cancelled.
     */
    public void setShowCompletedInput(boolean on) {
        mShowCompletedInput = on;
    }

    @Override
    public void requestLineEvent(@NonNull ByteBuffer buf, int initlen, boolean unicode) {
        super.requestLineEvent(buf, initlen, unicode);
        if (mInputBB != null) {
            GLKController.freeByteBuffer(mInputBB);
            mInputBB = null;
        }

        mIs32Bit = unicode;
        mInputBB = buf;

        mInput.setLength(0);
        mInputBB.limit(unicode ? initlen * 4 : initlen);
        mInput.append(mCharsetMgr.getGLKString(mInputBB, unicode));
        mInputBB.limit(mInputBB.capacity());
        mInputBB.rewind();

        // update cursor to match current input style
        mWindowStyles[GLKConstants.style_Input].updateDrawState(mHighlightPaint);
    }

    @Override
    public void cancelLineEvent(@Nullable GLKEvent event) {
        super.cancelLineEvent(event);
        int len = mInput.length();
        if (mInputBB != null) {
            if (event != null && len > 0) {
                // as this operation could be expensive, only do it if we need to
                len = mCharsetMgr.putGLKString(mInput.toString(), mInputBB, mIs32Bit, false);
            }
            GLKController.freeByteBuffer(mInputBB);
            mInputBB = null;
        }
        mInput.setLength(0);

        if (event != null) {
            // N.B. From GLK Spec:
            // "The event pointed to by the event argument will be filled in as if the player had hit enter"
            // "val2 will be 0 unless input was ended by a special terminator key, in which case val2 will
            // be the keycode (one of the values passed to glk_set_terminators_line_event())"
            event.lineEvent(getStreamId(), len, 0);
        }
    }

    /**
     * GLK Spec 3.7:
     * This may do any number of things, such as delete all text in the window, or
     * print enough blank lines to scroll all text beyond visibility, or insert
     * a page-break marker which is treated specially by the display part of the library.
     */
    @Override
    public void clear() {
        super.clear();

        // Empty the buffers, layouts, etc.
        mOutputBuffer = new WatchableStringBuilder();
        if (mHTMLState != null) {
            mHTMLState.setOutput(mOutputBuffer);
        }
        setStyle(GLKConstants.style_Normal);
    }

    /**
     * Get the current height, in pixels, of all text flushed and pending to this text buffer.
     * We omit any trailing whitespace (including newlines) from the calculation.
     *
     * @return the height, in pixels, including top and bottom padding.
     */
    int getTextHeight() {
        SpannableStringBuilder tmp = new SpannableStringBuilder();
        tmp.append(mOutputBuffer);
        if (tmp.length() == 0) {
            return mPaddingTopPx + mPaddingBottomPx;
        }

        int i, j;
        i = j = tmp.length() - 1;
        for (; i > 0; i--) {
            if (!Character.isWhitespace(tmp.charAt(i))) {
                break;
            }
        }
        if (i < j) {
            tmp = tmp.delete(i + 1, j + 1);
        }

        StaticLayout tmpLayout = new StaticLayout(tmp, mBaseTextPaint,
                mWidthGLK_PX, Alignment.ALIGN_NORMAL, mLeadingMult, 0.0f, false);
        return tmpLayout.getHeight() + mPaddingTopPx + mPaddingBottomPx;
    }

    @Override
    public void setZStyleColors(int fg, int bg) {
        // Finish off the current style block before applying the colour changes
        GLKStyleSpan.end(mOutputBuffer, StyleMarker.class, CharacterStyle.wrap(mCurrentStyle));
        super.setZStyleColors(fg, bg);
        GLKStyleSpan.start(mOutputBuffer, new StyleMarker());
    }

    @Override
    public void setStyleReverse(boolean reverse) {
        // Finish off the current style block before applying the colour changes
        GLKStyleSpan.end(mOutputBuffer, StyleMarker.class, CharacterStyle.wrap(mCurrentStyle));
        super.setStyleReverse(reverse);
        GLKStyleSpan.start(mOutputBuffer, new StyleMarker());
    }

    private int prettify(int ch) {
        // Converts codepoint 'ch' to smart quotes, etc.
        // last_ch is the character previously printed, or null if no character previously printed.
        switch (ch) {
            case '\"':
                if (mFlagDQ) {
                    mFlagDQ = false;
                    return GLKConstants.RDQUOTE;
                } else {
                    mFlagDQ = true;
                    return GLKConstants.LDQUOTE;
                }
            case '\'':
                if (mFlagLastW) {
                    mFlagLastW = false;
                    return GLKConstants.RSQUOTE;
                } else {
                    return ch;
                }
            case '-':
                if (mFlagLastDSH) {
                    mFlagLastDSH = false;
                    int len = mOutputBuffer.length();
                    mOutputBuffer.replace(len - 1, len, String.valueOf((char) GLKConstants.EM_DASH));
                    return -1;
                } else {
                    mFlagLastDSH = true;
                    return ch;
                }
        }

        mFlagLastDSH = false;
        mFlagLastW = Character.isLetter(ch);

        // Just return the same code point
        return ch;
    }

    @Override
    public void putString(@NonNull String s) {
        if (s.length() == 0) return;
        super.putString(s);

        if (mIsInHTMLMode) {
            if (mHTMLState != null) {
                mHTMLState.appendInput(s);
                Parser.process(mHTMLState);
            } else {
                GLKLogger.warn("GLKTextBufferM: putString: can't process HTML '" + s + "' as html state not initialised!");
            }
        } else {
            if (!mCurrentStyle.mIsMono) {
                s = prettify(s);
            }
            mOutputBuffer.append(s);
        }
    }

    @Override
    public void putChar(int ch) {
        super.putChar(ch);

        if (mIsInHTMLMode) {
            if (mHTMLState != null) {
                mHTMLState.appendInput((char) ch);
                Parser.process(mHTMLState);
            } else {
                GLKLogger.warn("GLKTextBufferM: putChar: can't process HTML as html state not initialised!");
            }
        } else {
            if (!mCurrentStyle.mIsMono) {
                ch = prettify(ch);
            }
            if (ch > 0) {
                mOutputBuffer.append((char) ch);
            }
        }
    }

    @Override
    public void startHyperlink(int linkval) {
        if (mHyperlinkValue != GLKConstants.NULL) {
            endHyperlink();
        }
        mHyperlinkValue = linkval;
        GLKStyleSpan.start(mOutputBuffer, new Hyperlink());
    }

    @Override
    public void endHyperlink() {
        if (mHyperlinkValue != GLKConstants.NULL) {
            GLKStyleSpan.end(mOutputBuffer, Hyperlink.class, new GLKHyperlinkSpan(mHyperlinkValue, mModel.mHyperlinkColor));
            mHyperlinkValue = GLKConstants.NULL;
        }
    }

    public void drawPicture(@NonNull GLKDrawable img, int align, boolean autoresize) {
        // If you call glk_image_draw() or glk_image_draw_scaled() in a text buffer
        // window, val1 gives the image alignment. The val2 argument is currently
        // unused, and should always be zero.
        int len = mOutputBuffer.length();

        if (autoresize) {
            // don't allow image width to be wider than display
            GLKUtils.ensureDrawableWithinWidth(mWidthGLK_PX, img);
        }

        mOutputBuffer.append("\uFFFC");
        mOutputBuffer.setSpan(new ImageSpan(img, DynamicDrawableSpan.ALIGN_BOTTOM), len, len + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    @Override
    public void close() throws IOException {
        super.close();
        if (mInputBB != null) {
            GLKController.freeByteBuffer(mInputBB);
            mInputBB = null;
        }
    }

    @Override
    public void setStyle(int s) {
        GLKStyleSpan.end(mOutputBuffer, StyleMarker.class, CharacterStyle.wrap(mCurrentStyle));
        super.setStyle(s);
        GLKStyleSpan.start(mOutputBuffer, new StyleMarker());
    }

    /**
     * Turn HTML mode on or off.
     * <p/>
     * N.B. TADS 3 always treats text output as HTML. There's no way to turn this on or off,
     * and it applies even on a text-only interpreter.
     *
     * @param on - whether HTML is on or off.
     */
    public void setHTMLOutput(@NonNull GLKModel m, boolean on) {
        // Finish off the current style block before applying the colour changes
        if (!mIsInHTMLMode && on) {
            // we're about to turn on HTML, finish any styled text
            GLKStyleSpan.end(mOutputBuffer, StyleMarker.class, CharacterStyle.wrap(mCurrentStyle));

            // if this is the first time HTML has been enabled, initialise the state object
            if (mHTMLState == null) {
                GLKController.tadsban_html_set_root_if_null(m, this);
                mHTMLState = new State(m, this, mOutputBuffer);
            }
        } else if (mIsInHTMLMode && !on) {
            GLKStyleSpan.start(mOutputBuffer, new StyleMarker());
        }
        mIsInHTMLMode = on;
    }

    public static class GLKTextBufferBuilder extends GLKTextWindowBuilder {
        @NonNull
        public GLKTextBufferM build() {
            return new GLKTextBufferM(this);
        }
    }

    private static class Hyperlink {
    }

    private static class StyleMarker {
    }
}