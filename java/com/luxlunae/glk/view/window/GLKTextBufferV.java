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
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.DynamicLayout;
import android.text.Layout;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.style.CharacterStyle;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;

import com.luxlunae.glk.GLKActivity;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.stream.window.GLKPairM;
import com.luxlunae.glk.model.stream.window.GLKTextBufferM;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;
import com.luxlunae.glk.model.stream.window.GLKWindowM;
import com.luxlunae.glk.model.style.GLKHyperlinkSpan;
import com.luxlunae.glk.model.style.GLKStyleSpan;
import com.luxlunae.glk.model.html.TableSpan;

import java.util.ArrayList;

import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_BG_COLOR_CHANGED;
import static com.luxlunae.glk.model.stream.window.GLKWindowM.ModifierFlag.FLAG_CLEARED;

@SuppressLint("ViewConstructor")
public class GLKTextBufferV extends GLKTextWindowV implements HVScrollView.ScrollChild {

    private static final int ID_PASTE = android.R.id.paste;

    // INPUT
    @NonNull
    private final Path mHighlightPath = new Path();
    @NonNull
    private final GLKTextBufferM mModel;
    @NonNull
    private final GLKScreen mScreen;
    private final boolean mSyncBGColor;
    @NonNull
    private ArrayList<Integer> mBufferedInput = new ArrayList<>();
    private boolean mComposing = false;

    // OUTPUT
    @NonNull
    private SpannableStringBuilder mOutput = new SpannableStringBuilder();
    @Nullable
    private DynamicLayout mLayout;
    private int mVOffset;
    private int mCurLineEvent = -1;
    private boolean mAcceptingBufferedInput = true;
    @Nullable
    private String mLastFlush;

    public GLKTextBufferV(@NonNull Context c, @NonNull GLKTextBufferM model, @NonNull GLKScreen screen,
                          @Nullable int[] vScrollbarColors, @Nullable int[] hScrollbarColors,
                          int vScrollbarWidth, int hScrollbarHeight, boolean syncBGColor) {
        super(c, model);
        mModel = model;
        mScreen = screen;
        mSyncBGColor = syncBGColor;
        setScrollX(0);
        setScrollY(0);
        setScrollChild(this);  // enable scrolling

        // Set the scrollbar colors based on input style, unless
        // overriden by user or config file
        Resources res = screen.getContext().getResources();
        GLKStyleSpan sty = model.mWindowStyles[GLKConstants.style_Input];
        int[] colors = new int[2];
        colors[0] = colors[1] = sty.mTextColor;
        setScrollbarColors(res, true, (vScrollbarColors != null) ? vScrollbarColors : colors);
        setScrollbarColors(res, false, (hScrollbarColors != null) ? hScrollbarColors : colors);
        setScrollbarSize(res, true, vScrollbarWidth);
        setScrollbarSize(res, false, hScrollbarHeight);
        setContentDescription("");
    }

    /**
     * Look for a hyperlink in layout "layout" at pixel coordinates (x, y).
     * This may require traversing tablespans which in themselves contain
     * layouts (one for each cell), which may contain further tablespans, etc.
     *
     * @param layout - the layout to inspect.
     * @param x      - x ordinate.
     * @param y      - y ordinate.
     * @return NULL if no layout found.
     */
    private static GLKHyperlinkSpan findHyperlink(@NonNull Layout layout, int x, int y) {
        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);
        Spanned text = (Spanned) layout.getText();

        GLKHyperlinkSpan[] links = text.getSpans(off, off, GLKHyperlinkSpan.class);
        if (links.length > 0) {
            // success
            return links[0];
        } else {
            // if we've got a tablespan, then keep looking
            // otherwise give up
            TableSpan[] tables = text.getSpans(off, off, TableSpan.class);
            if (tables.length > 0) {
                Point cellPos = new Point();
                StaticLayout l = tables[0].getCellForScreenCoords(x, y, cellPos);
                if (l != null) {
                    // try again
                    return findHyperlink(l, x - cellPos.x, y - cellPos.y);
                }
            }
        }
        return null;
    }

    @Override
    public int sendKey(int key) {
        if (!mComposing) {
            return 0;
        }

        boolean lineDone = false;
        int len = mModel.mInput.length();
        if (key == GLKConstants.keycode_Delete) {
            // BACKSPACE: Delete the character to the left of the cursor
            if (len > 0) {
                mModel.mInput.setLength(--len);
            }
        } else if (super.isTerminator(key)) {
            // Terminate the current input
            // Flush the contents of the string builder back out to the byte buffer
            // that was provided when request_line_event was first called
            if (mModel.mInputBB != null) {
                String input = mModel.mInput.toString();
                announceForAccessibility(input);
                len = mModel.mCharsetMgr.putGLKString(input, mModel.mInputBB, mModel.mIs32Bit, false);
            } else {
                len = 0;
            }
            lineDone = true;
        } else {
            // Append the new character to the buffer
            if (!mModel.mIs32Bit) {
                // As this buffer is 8 bit, anything higher than
                // 255 should be converted into a 0x3F ("?") as
                // per the GLK spec.
                mModel.mInput.append(key > 255 ? (char) 0x3F : (char) key);
            } else {
                if (Character.isValidCodePoint(key)) {
                    mModel.mInput.appendCodePoint(key);
                } else {
                    GLKLogger.warn("GLKTextBuffer: received invalid codepoint (" + key + "), dropping.");
                }
            }
        }

        drawInput();
        scrollToEnd();
        if (lineDone) {
            stopComposing(true);
        }
        invalidate();
        return len;
    }

    @Override
    public void updateContents() {
        if (mSyncBGColor && mModel.isFlagSet(FLAG_BG_COLOR_CHANGED)) {
            // If this text buffer's parent is the GLKScreen or a GLKPair
            // that has the GLKScreen as it's parent, and it is the main
            // window (i.e. below, not above) or sole window (no sibling),
            // change the screen background color to match this text
            // buffer's new background color
            GLKPairM pm = mModel.getParent();
            boolean mainTextBuffer = false;

            if (pm != null) {
                if (pm.getOrientation() == LinearLayout.VERTICAL) {
                    mainTextBuffer = (pm.getChild2().getStreamId() == getStreamId());
                    mainTextBuffer = mainTextBuffer && pm.getParent() == null;
                }
            } else {
                mainTextBuffer = true;
            }

            if (mainTextBuffer) {
                // change screen background color
                int bgColor = mModel.getBGColor();
                mScreen.setBackgroundColor(bgColor);

                // change text grid sibling background color also, if it exists
                GLKWindowM sibM = mModel.getSibling();
                if (sibM != null) {
                    GLKWindowV sibV = GLKScreen.getViewForModel(sibM);
                    if (sibV != null && sibV instanceof GLKTextGridV) {
                        ((GLKTextGridV) sibV).setBackgroundColor(bgColor);
                    }
                }
            }
        }

        super.updateContents();

        // Resize the layout if necessary
        if (mLayout == null || mLayout.getWidth() != mModel.mWidthGLK_PX) {
            mLayout = new DynamicLayout(mOutput, mModel.mBaseTextPaint, mModel.mWidthGLK_PX,
                    Alignment.ALIGN_NORMAL, mModel.mLeadingMult, 0.0f, false);
        }

        SpannableStringBuilder mOutputBuffer = mModel.getBuffer();

        // Process clear operation
        if (mModel.isFlagSet(FLAG_CLEARED)) {
            mOutput = new SpannableStringBuilder();
            mLayout = new DynamicLayout(mOutput, mModel.mBaseTextPaint, mModel.mWidthGLK_PX,
                    Alignment.ALIGN_NORMAL, mModel.mLeadingMult, 0.0f, false);
            setScrollX(0);
            setScrollY(0);
            mModel.clearFlag(FLAG_CLEARED);
        }

        // If there's nothing to flush, then return
        if (mOutputBuffer.length() == 0) {
            return;
        }

        // If the text being flushed is interrupting a composing session,
        // temporarily suspend that composition (we'll re-enable after we have
        // added the new text)
        boolean composing = mComposing;
        if (composing) {
            stopComposing(false);
        }

        // Finish off the last style block
        mModel.endCurrentStyle();

        // Flush the output buffer and scroll as appropriate
        int y1 = mLayout.getLineTop(Math.max(mLayout.getLineCount() - 2, 0));
        mOutput.append(mOutputBuffer);
        int y2 = mLayout.getLineBottom(mLayout.getLineCount() - 1);

        // For GLK text layout gravity is BOTTOM, but when in TADS HTML mode it
        // should be TOP...
        if (mModel.hasShownHTML()) {
            mVOffset = 0;
        } else {
            mVOffset = Math.max(0, mModel.getHeight() - getPaddingTop() - getPaddingBottom() - mLayout.getHeight());
        }

        if (y2 - getScrollY() > mHeightLessPadding) {
            // The appended text does not all fit into the display, so
            // scroll down until: (1) last line of previous text is first line
            // in the display; OR (2) last line of appended text is last line of
            // the display - whichever comes first
            setScrollY(Math.min(y1, y2 - mHeightLessPadding));
        }

        if (mModel.lineRequested() || mModel.charRequested()) {
            // Alert the hosting activity that it should start speaking the
            // new text if TTS has been enabled.
            mLastFlush = mOutputBuffer.toString().trim();
            mLastFlush = mLastFlush.replaceAll(">\\Z", "What now?");
            if (mLastFlush != null) {
                Context c = getContext();
                if (c instanceof GLKActivity) {
                    ((GLKActivity) c).speakText(mLastFlush);
                }

                // Also ensure we announce this to users that have accessibility enabled
                // (e.g. Google TalkBack)
                //setContentDescription("text buffer " + mModel.getStreamId() + ": " + utterance);
                announceForAccessibility(mLastFlush);
            }
        }

        // Clear the pending text buffer and get ready for next output chunk
        mOutputBuffer.clear();
        mModel.startNewStyle();

        // If the text just flushed interrupted a composing session,
        // restart composition from the end of the appended text
        if (composing) {
            startComposing();
            updateCursor();
        }

        invalidate();
    }

    @Override
    public void updateInputState() {
        // Get ready for user input, if relevant
        if (mModel.lineRequested()) {
            if (mModel.mLineEventCount != mCurLineEvent) {
                mCurLineEvent = mModel.mLineEventCount;
                if (mComposing) {
                    stopComposing(false);
                }
                startComposing();
                drawInput();
                invalidate();
            }
            processBufferedInputIfAny();
        } else {
            if (mComposing) {
                stopComposing(false);
                invalidate();
            }
            mCurLineEvent = -1;
        }
    }

    /**
     * Get the current contents of the input buffer.
     *
     * @return a copy of the input buffer.
     */
    @NonNull
    public String getInputBuffer() {
        return mModel.mInput.toString();
    }

    /**
     * Clear the underlying input buffer and update the composing input text
     * currently displayed on-screen.
     */
    public void clearInputBuffer() {
        mModel.mInput.setLength(0);
        drawInput();
        invalidate();
    }

    /**
     * Clear any composing input text currently displayed on-screen.
     */
    private void clearInput() {
        int len = mOutput.length();
        int pos = mOutput.getSpanStart(InputMarker.class);
        if (pos < len && pos >= 0) {
            mOutput.delete(pos, len);
        }
    }

    /**
     * Update the composing input text currently displayed on-screen, to reflect the
     * current contents of the input buffer. The cursor position is also updated to
     * appear at the end of the text.
     */
    private void drawInput() {
        // Delete old input (if any)
        clearInput();

        // Add the new input (if any)
        if (mComposing) {
            // If we are still composing the line, draw the current input buffer
            int ilen = mModel.mInput.length();
            if (ilen > 0) {
                int len = mOutput.length();
                mOutput.append(mModel.mInput);
                mOutput.setSpan(CharacterStyle.wrap(mModel.mWindowStyles[GLKConstants.style_Input]),
                        len, len + ilen, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            updateCursor();
        }
    }

    private void updateCursor() {
        mHighlightPath.reset();
        if (mLayout != null) {
            mLayout.getCursorPath(mOutput.length(), mHighlightPath, mOutput);
        }
    }

    /**
     * Allow user to compose text on-screen, commencing at the end of the
     * currently displayed text.
     */
    private void startComposing() {
        mComposing = true;
        int len = mOutput.length();
        mOutput.setSpan(InputMarker.class, len, len, Spanned.SPAN_MARK_MARK);
    }

    /**
     * Prevent user from composing text on screen.
     *
     * @param userTerminated - whether the user terminated input or our GLK library did.
     */
    private void stopComposing(boolean userTerminated) {
        mComposing = false;
        if (mModel.showCompletedInput()) {
            int len = mOutput.length();
            int pos = mOutput.getSpanStart(InputMarker.class);
            if ((pos >= 0 && pos < len) || userTerminated) {
                // if there is some input - or the input was terminated
                // by the user (possibly empty) - and we're supposed to
                // show it, we now move onto the next line
                mOutput.append('\n');
            }
        } else {
            clearInput();
        }
        mOutput.removeSpan(InputMarker.class);
    }

    private void scrollToEnd() {
        // Scroll down to the last line of the displayed text
        if (mLayout != null) {
            int y2 = mLayout.getLineBottom(mLayout.getLineCount() - 1);
            if (y2 - getScrollY() > mHeightLessPadding) {
                scrollTo(0, Math.max(0, y2 - mHeightLessPadding));    // scroll screen down to text entry
            }
        }
    }

    /**
     * Called when a context menu option for the text view is selected.  Currently
     * this will be one of {@link android.R.id#paste}.
     *
     * @return true if the context menu item action was performed.
     */
    private boolean onTextContextMenuItem(int id) {
        switch (id) {
            case ID_PASTE:
                pasteInput();
                return true;
        }
        return false;
    }

    /**
     * If this text buffer is accepting line input, paste the clipboard text.
     */
    private void pasteInput() {
        if (!mComposing) {
            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                CharSequence paste = clip.getItemAt(i).coerceToStyledText(getContext());
                if (paste != null) {
                    for (int j = 0, sz = paste.length(); j < sz; j++) {
                        mBufferedInput.add((int) paste.charAt(j));
                    }
                }
            }
        }
        processBufferedInputIfAny();
    }

    public void appendBufferedInput(@NonNull int[] codes, int offset, int length) {
        if (mAcceptingBufferedInput) {
            for (int i = offset, k = 0, sz = codes.length;
                 k < length && i < sz; i++, k++) {
                mBufferedInput.add(codes[i]);
            }
        }
    }

    private void processBufferedInputIfAny() {
        int sz = mBufferedInput.size();

        if (sz == 0) {
            // nothing to do
            return;
        }

        if (mBufferedInput.get(0) == '>') {
            // to support transcripts that start each line with a >
            mBufferedInput.remove(0);
            sz--;
        }

        int[] codes = GLKUtils.getPrimitiveIntArray(mBufferedInput);
        int[] keysSent = new int[1];

        mAcceptingBufferedInput = false;
        GLKActivity act = GLKUtils.getActivityForView(this);
        if (act != null) {
            act.sendKeyCodes(this, codes, keysSent);
        }
        mAcceptingBufferedInput = true;

        int n = keysSent[0];
        if (n > 0 && n < sz) {
            // remember sublist is (from - inclusive, to - exclusive)
            mBufferedInput.subList(0, n).clear();
        } else {
            mBufferedInput.clear();
        }

        resetInputConnection();
    }

    private void resetInputConnection() {
        if (mShowSystemKeyboard) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                // restartInput:
                //   "If the input method is currently connected to the given view,
                //    restart it with its new contents. You should call this when the text
                //    within your view changes outside of the normal input method or key
                //    input flow, such as when an application calls TextView.setText()."
                imm.restartInput(this);
            }
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        if (mLayout != null) {
            try {
                int line = mLayout.getLineForVertical(getScrollY());
                int start = mLayout.getLineStart(line);
                int end = mLayout.getLineEnd(mLayout.getLineForVertical(getScrollY() + mHeightLessPadding));
                if (end > start && end <= mLayout.getText().length()) {
                    canvas.save();
                    canvas.clipRect(getScrollX() + mPaddingLeft, getScrollY() + mPaddingTop,
                            getScrollX() + getWidth() - mPaddingRight, getScrollY() + getHeight() - mPaddingBottom);
                    canvas.translate(mPaddingLeft, mPaddingTop + mVOffset);
                    if (mComposing) {
                        // in line input mode - draw the cursor
                        mLayout.draw(canvas, mHighlightPath, mModel.mHighlightPaint, 0);
                    } else {
                        // no cursor needed
                        mLayout.draw(canvas);
                    }
                    canvas.restore();
                }
            } catch (@NonNull IndexOutOfBoundsException | IllegalArgumentException e1) {
                // Shouldn't get here, but if we do catch and log the exception
                // rather than letting Android just kill our app
                GLKLogger.error("Error: Couldn't render text layout: " + e1.getMessage());
            }
        }
    }

    private void processTADSLink(@NonNull GLKHyperlinkSpan link) {
        if (!mComposing) {
            return;
        }

        String r = link.getUrl();
        if (!link.mAppend) {
            mModel.mInput.setLength(0);
        }

        mModel.mInput.append(r);

        if (!link.mNoEnter) {
            if (mModel.mInputBB != null) {
                mModel.mCharsetMgr.putGLKString(mModel.mInput.toString(), mModel.mInputBB, mModel.mIs32Bit, false);
            }
            drawInput();
            scrollToEnd();
            stopComposing(false);

            // post the event
            // val2 will be 0 unless input was ended by a special terminator key, in which case val2 will
            // be the keycode (one of the values passed to glk_set_terminators_line_event())
            GLKEvent ev = new GLKEvent();
            ev.lineEvent(mModel.getStreamId(), mModel.mInput.length(), 0);
            GLKController.postEvent(ev);
        }
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {
        // for now, we just paste
        // TODO: show a context menu with various options
        onTextContextMenuItem(ID_PASTE);
    }

    @Override
    public boolean onSingleTapConfirmed(@NonNull MotionEvent arg0) {
        // We need to use onSingleTapConfirmed rather than onSingleTapUp as
        // we support double tap events (see below).
        if (mLayout != null) {
            GLKHyperlinkSpan link = findHyperlink(mLayout,
                    (int) arg0.getX() + getScrollX() - mPaddingLeft,
                    (int) arg0.getY() + getScrollY() - mPaddingTop - mVOffset);
            if (link != null) {
                int v = link.getVal();
                if (v != GLKConstants.NULL) {
                    // generic GLK hyperlink
                    if (mModel.hyperlinkRequested()) {
                        GLKEvent ev = new GLKEvent();
                        ev.hyperlinkEvent(mModel.getStreamId(), v);
                        GLKController.postEvent(ev);
                    }
                } else {
                    // TADS-style hyperlink
                    GLKTextWindowM win = link.mInputWin;
                    if (win != null && win instanceof GLKTextBufferM) {
                        // send the link back to be processed on the view side of the provided window model
                        GLKTextBufferV winV = (GLKTextBufferV) GLKScreen.getViewForModel(win);
                        if (winV != null) {
                            winV.processTADSLink(link);
                        }
                    } else {
                        GLKLogger.error("GLKTextBuffer: Could not process TADS hyperlink '" + link.getUrl() + "' as the attached window was invalid.");
                    }
                }
                return true;
            } else {
                // if accessiblity enabled, repeat the last flushed text
                announceForAccessibility((mLastFlush != null) ?
                        "Text buffer " + getStreamId() + ". " + mLastFlush :
                        "Text buffer 1 is empty.");
            }
        }
        return super.onSingleTapConfirmed(arg0);
    }

    @Override
    public boolean onDoubleTap(@NonNull MotionEvent e) {
        // If this window is composing, detect the word (if any) the
        // user has double-tapped and add it to the composing text
        if (mComposing && mLayout != null) {
            // Get the character offset closest to the event's (x, y) position
            float x = e.getX() + getScrollX() - mPaddingLeft;
            float y = e.getY() + getScrollY() - mPaddingTop - mVOffset;
            int line = mLayout.getLineForVertical((int) y);
            int offset = mLayout.getOffsetForHorizontal(line, (int) x);

            // Now work out what word this is
            int i1, i2;
            CharSequence txt = mLayout.getText();
            for (i1 = offset - 1; i1 >= 0; i1--) {
                if (!Character.isLetterOrDigit(txt.charAt(i1))) {
                    i1++;
                    break;
                }
            }
            i1 = Math.max(i1, 0);
            int sz;
            for (i2 = offset + 1, sz = txt.length(); i2 < sz; i2++) {
                if (!Character.isLetterOrDigit(txt.charAt(i2))) {
                    break;
                }
            }
            i2 = Math.min(i2, sz - 1);

            int len = i2 - i1;
            if (len > 0 && len <= 200) {
                // To avoid overloading the input buffer, We only add the word
                // if it has a least one character and no more than 200 characters
                // According to https://en.wikipedia.org/wiki/Longest_word_in_English,
                // the longest English word to appear in a major dictionary was 45
                // chars long. According to http://www.bbc.com/news/world-europe-22762040
                // the longest German word (until 2013) was 63 chars long. The longest
                // word in any language - as far as Wikipedia has found - is in Sanskrit
                // (see https://en.wikipedia.org/wiki/Longest_words). It is 195 chars long.
                // So we should be safe with our 200 char limit. And in the worst case
                // scenario, if a user does find a word above that limit, all it means is
                // that the user won't be able to double-tap it and will have to type
                // the old-fashioned way (as painful as that would be).
                String word = txt.subSequence(i1, i2).toString().trim();
                StringBuilder sbBuf = mModel.mInput;
                int lenBuf = sbBuf.length();
                if (lenBuf > 0 && !Character.isWhitespace(sbBuf.charAt(lenBuf - 1))) {
                    // Spare the user the annoyance of typing an additional space
                    sbBuf.append(' ');
                }
                sbBuf.append(word);
                drawInput();
                scrollToEnd();
                invalidate();
            }
            return true;
        }
        return false;
    }

    @Override
    public int getChildWidth() {
        return (mLayout != null) ? mLayout.getWidth() : 0;
    }

    @Override
    public int getChildHeight() {
        return (mLayout != null) ? mLayout.getHeight() : 0;
    }

    private static class InputMarker {
    }
}