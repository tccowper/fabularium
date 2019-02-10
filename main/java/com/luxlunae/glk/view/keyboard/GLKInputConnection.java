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
package com.luxlunae.glk.view.keyboard;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.Selection;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.MetaKeyKeyListener;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputContentInfo;
import android.view.inputmethod.InputMethodManager;

import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKActivity;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.view.window.GLKTextBufferV;
import com.luxlunae.glk.view.window.GLKTextWindowV;

import static java.lang.Character.MAX_SURROGATE;
import static java.lang.Character.MIN_SURROGATE;

/**
 * The GLKInputConnection is used to connect the user's preferred system keyboard with the GLK view hierarchy,
 * for cases where the user elects not to use the built-in keyboard.
 * <p>
 * It is a modified version of the BaseInputConnection.
 * <p>
 * See
 * https://developer.android.com/reference/android/view/inputmethod/InputConnection.html
 * https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/view/inputmethod/BaseInputConnection.java
 * https://android.googlesource.com/platform/frameworks/base.git/+/android-4.4_r1.1/core/java/com/android/internal/widget/EditableInputConnection.java
 * <p>
 * for more information and sample implementations.
 */
public class GLKInputConnection implements InputConnection {

    private static final boolean DEBUG_INPUT_CONNECTION = false;

    private static final Object COMPOSING = new ComposingText();
    private static final int INVALID_INDEX = -1;
    private final GLKTextWindowV mView;
    private Editable mEditable;
    private Object[] mDefaultComposingSpans;

    public GLKInputConnection(GLKTextWindowV view) {
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.error("GLKInputConnection: constructor");
        }
        mView = view;
    }

    private static void removeComposingSpans(@NonNull Spannable text) {
        text.removeSpan(COMPOSING);
        Object[] sps = text.getSpans(0, text.length(), Object.class);
        if (sps != null) {
            for (int i = sps.length - 1; i >= 0; i--) {
                Object o = sps[i];
                if ((text.getSpanFlags(o) & Spanned.SPAN_COMPOSING) != 0) {
                    text.removeSpan(o);
                }
            }
        }
    }

    private static void setComposingSpans(@NonNull Spannable text) {
        setComposingSpans(text, text.length());
    }

    private static void setComposingSpans(@NonNull Spannable text, int end) {
        final Object[] sps = text.getSpans(0, end, Object.class);
        if (sps != null) {
            for (int i = sps.length - 1; i >= 0; i--) {
                final Object o = sps[i];
                if (o == COMPOSING) {
                    text.removeSpan(o);
                    continue;
                }

                final int fl = text.getSpanFlags(o);
                if ((fl & (Spanned.SPAN_COMPOSING | Spanned.SPAN_POINT_MARK_MASK))
                        != (Spanned.SPAN_COMPOSING | Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)) {
                    int a = text.getSpanStart(o);
                    int b = text.getSpanEnd(o);
                    if (b > a) {
                        text.setSpan(o, a, b,
                                (fl & ~Spanned.SPAN_POINT_MARK_MASK)
                                        | Spanned.SPAN_COMPOSING
                                        | Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        }

        if (end > 0) {
            text.setSpan(COMPOSING, 0, end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
        }
    }

    private static int getComposingSpanStart(@NonNull Spannable text) {
        return text.getSpanStart(COMPOSING);
    }

    private static int getComposingSpanEnd(@NonNull Spannable text) {
        return text.getSpanEnd(COMPOSING);
    }

    private static int findIndexBackward(@NonNull final CharSequence cs, final int from,
                                         final int numCodePoints) {
        int currentIndex = from;
        boolean waitingHighSurrogate = false;
        final int N = cs.length();
        if (currentIndex < 0 || N < currentIndex) {
            return INVALID_INDEX;  // The starting point is out of range.
        }
        if (numCodePoints < 0) {
            return INVALID_INDEX;  // Basically this should not happen.
        }
        int remainingCodePoints = numCodePoints;
        while (true) {
            if (remainingCodePoints == 0) {
                return currentIndex;  // Reached to the requested length in code points.
            }

            --currentIndex;
            if (currentIndex < 0) {
                if (waitingHighSurrogate) {
                    return INVALID_INDEX;  // An invalid surrogate pair is found.
                }
                return 0;  // Reached to the beginning of the text w/o any invalid surrogate pair.
            }
            final char c = cs.charAt(currentIndex);
            if (waitingHighSurrogate) {
                if (!Character.isHighSurrogate(c)) {
                    return INVALID_INDEX;  // An invalid surrogate pair is found.
                }
                waitingHighSurrogate = false;
                --remainingCodePoints;
                continue;
            }
            if (!isSurrogate(c)) {
                --remainingCodePoints;
                continue;
            }
            if (Character.isHighSurrogate(c)) {
                return INVALID_INDEX;  // A invalid surrogate pair is found.
            }
            waitingHighSurrogate = true;
        }
    }

    private static int findIndexForward(@NonNull final CharSequence cs, final int from,
                                        final int numCodePoints) {
        int currentIndex = from;
        boolean waitingLowSurrogate = false;
        final int N = cs.length();
        if (currentIndex < 0 || N < currentIndex) {
            return INVALID_INDEX;  // The starting point is out of range.
        }
        if (numCodePoints < 0) {
            return INVALID_INDEX;  // Basically this should not happen.
        }
        int remainingCodePoints = numCodePoints;

        while (true) {
            if (remainingCodePoints == 0) {
                return currentIndex;  // Reached to the requested length in code points.
            }

            if (currentIndex >= N) {
                if (waitingLowSurrogate) {
                    return INVALID_INDEX;  // An invalid surrogate pair is found.
                }
                return N;  // Reached to the end of the text w/o any invalid surrogate pair.
            }
            final char c = cs.charAt(currentIndex);
            if (waitingLowSurrogate) {
                if (!Character.isLowSurrogate(c)) {
                    return INVALID_INDEX;  // An invalid surrogate pair is found.
                }
                --remainingCodePoints;
                waitingLowSurrogate = false;
                ++currentIndex;
                continue;
            }
            if (!isSurrogate(c)) {
                --remainingCodePoints;
                ++currentIndex;
                continue;
            }
            if (Character.isLowSurrogate(c)) {
                return INVALID_INDEX;  // A invalid surrogate pair is found.
            }
            waitingLowSurrogate = true;
            ++currentIndex;
        }
    }

    /**
     * Returns true if the given character is a high or low surrogate.
     */
    private static boolean isSurrogate(char ch) {
        // We implement this function here as it is not available in
        // Android API levels < 19 and we want to support from API Level 16
        // Taken directly from the Android source code
        return ch >= MIN_SURROGATE && ch <= MAX_SURROGATE;
    }

    /**
     * Return the target of edit operations, that is used for
     * composing text.
     */
    private Editable getEditable() {
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: getEditable()");
        }
        if (mEditable == null) {
            String curBuf = (mView != null && mView instanceof GLKTextBufferV ?
                    ((GLKTextBufferV) mView).getInputBuffer() : "");
            mEditable = Editable.Factory.getInstance().newEditable(curBuf);
            Selection.setSelection(mEditable, curBuf.length());
            if (DEBUG_INPUT_CONNECTION) {
                GLKLogger.debug("GLKInputConnection: editable is null, made a new one with current buffer text of '" + curBuf + "'");
            }
        }
        return mEditable;
    }

    /**
     * Tell the editor that you are starting a batch of editor operations. The editor will
     * try to avoid sending you updates about its state until endBatchEdit() is called.
     * Batch edits nest.
     *
     * IME authors: use this to avoid getting calls to onUpdateSelection(int, int,
     * int, int, int, int) corresponding to intermediate state. Also, use this
     * to avoid flickers that may arise from displaying intermediate state. Be sure
     * to call endBatchEdit() for each call to this, or you may block updates in the editor.
     *
     * Editor authors: while a batch edit is in progress, take care not to
     * send updates to the input method and not to update the display. IMEs use
     * this intensively to this effect. Also please note that batch edits need to nest correctly.
     *
     * @return true if a batch edit is now in progress, false otherwise. Since this method
     * starts a batch edit, that means it will always return true unless the input connection
     * is no longer valid.
     */
    @Override
    public boolean beginBatchEdit() {
        // Our implementation does nothing and returns false.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: beginBatchEdit()");
        }
        return false;
    }

    /**
     * Clear the given meta key pressed states in the given
     * input connection.
     *
     * This can be used by the IME to clear the meta key states set by a hardware
     * keyboard with latched meta keys, if the editor keeps track of these.
     *
     * @param states - The states to be cleared, may be one or more bits as per KeyEvent.getMetaState().
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean clearMetaKeyStates(int states) {
        // Our implementation uses MetaKeyKeyListener.clearMetaKeyState(long, int) to clear the state.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: clearMetaKeyStates()");
        }
        final Editable content = getEditable();
        if (content == null) {
            return false;
        }
        MetaKeyKeyListener.clearMetaKeyState(content, states);
        return true;
    }

    /**
     * Called by the system up to only once to notify that the system is about to invalidate
     * connection between the input method and the application.
     *
     * Editor authors: You can clear all the nested batch edit right now and you no
     * longer need to handle subsequent callbacks on this connection, including
     * beginBatchEdit()}. Note that although the system tries to call this method
     * whenever possible, there may be a chance that this method is not called in
     * some exceptional situations.
     *
     * Note: This does nothing when called from input methods.
     */
    @Override
    public void closeConnection() {
        // Our implementation just calls finishComposingText().
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.error("GLKInputConnection: closeConnection()");
        }
        finishComposingText();
    }

    /**
     * Commit a completion the user has selected from the possible ones previously
     * reported to InputMethodSession#displayCompletions(CompletionInfo[]) or
     * InputMethodManager#displayCompletions(View, CompletionInfo[]). This will
     * result in the same behavior as if the user had selected the completion
     * from the actual UI. In all other respects, this behaves like commitText(CharSequence, int).
     *
     * IME authors: please take care to send the same object that you received through
     * onDisplayCompletions(CompletionInfo[]).
     *
     * Editor authors: if you never call displayCompletions(CompletionInfo[]) or
     * displayCompletions(View, CompletionInfo[]) then a well-behaved IME should
     * never call this on your input connection, but be ready to deal with misbehaving IMEs without crashing.
     *
     * Calling this method (with a valid CompletionInfo object) will cause the
     * editor to call onUpdateSelection(int, int, int, int, int, int) on the
     * current IME after the batch input is over. Editor authors, for
     * this to happen you need to make the changes known to the input method
     * by calling updateSelection(View, int, int, int, int), but be careful to
     * wait until the batch edit is over if one is in progress.
     *
     * @param text - The committed completion.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean commitCompletion(CompletionInfo text) {
        // Our implementation does nothing and returns false.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: commitCompletion()");
        }
        return false;
    }

    /**
     * Called by the input method to commit content such as a PNG image to the editor.
     *
     * In order to avoid a variety of compatibility issues, this focuses on a simple
     * use case, where editors and IMEs are expected to work cooperatively as follows:
     *
     * - Editor must keep contentMimeTypes equal to null if it does not support this method at all.
     * - Editor can ignore this request when the MIME type specified in inputContentInfo does not match any of contentMimeTypes.
     * - Editor can ignore the cursor position when inserting the provided content.
     * - Editor can return true asynchronously, even before it starts loading the content.
     * - Editor should provide a way to delete the content inserted by this method or to revert the effect caused by this method.
     * - IME should not call this method when there is any composing text, in case calling this method causes a focus change.
     * - IME should grant a permission for the editor to read the content. See packageName about how to obtain the package name of the editor.
     *
     * @param inputContentInfo - Content to be inserted. This value must never be null.
     *
     * @param flags - INPUT_CONTENT_GRANT_READ_URI_PERMISSION if the content
     *              provider allows grantUriPermissions or 0 if the application
     *              does not need to call requestPermission().
     *
     * @param opts - optional bundle data. This can be null.
     *
     * @return true if this request is accepted by the application, whether
     * the request is already handled or still being handled in background, false otherwise.
     */
    @Override
    public boolean commitContent(@NonNull InputContentInfo inputContentInfo, int flags, @Nullable Bundle opts) {
        // Our implementation does nothing and returns false.
        return false;
    }

    /**
     * Commit a correction automatically performed on the raw user's input.
     * A typical example would be to correct typos using a dictionary.
     *
     * Calling this method will cause the editor to call
     * onUpdateSelection(int, int, int, int, int, int) on the
     * current IME after the batch input is over. Editor authors,
     * for this to happen you need to make the changes known to
     * the input method by calling updateSelection(View, int, int,
     * int, int), but be careful to wait until the batch edit is over if one is in progress.
     *
     * @param correctionInfo - Detailed information about the correction.
     *
     * @return true on success, false if the input connection is no
     * longer valid. In N and later, returns false when the target
     * application does not implement this method.
     */
    @Override
    public boolean commitCorrection(CorrectionInfo correctionInfo) {
        // Our implementation does nothing and returns false.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: commitCorrection()");
        }
        return false;
    }

    /**
     * Commit text to the text box and set the new cursor position.
     * <p>
     * This method removes the contents of the currently composing text and replaces
     * it with the passed CharSequence, and then moves the cursor according to
     * newCursorPosition. If there is no composing text when this method is called,
     * the new text is inserted at the cursor position, removing text inside the selection
     * if any. This behaves like calling setComposingText(text, newCursorPosition) then finishComposingText().
     *
     * @param text              - The text to commit. This may include styles.
     *
     * @param newCursorPosition - The new cursor position around the text, in Java characters. If > 0, this
     *                          is relative to the end of the text - 1; if <= 0, this is relative to the
     *                          start of the text. So a value of 1 will always advance the cursor to
     *                          the position after the full text being inserted. Note that this means
     *                          you can't position the cursor within the text, because the editor
     *                          can make modifications to the text you are providing so it is not
     *                          possible to correctly specify locations there.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        // Our implementation replaces the text and then, if the associated window
        // is waiting for a line event, renders the updated editable contents
        // on that window so user can see the change.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: commitText: '" + text + "', newCursorPos = " + newCursorPosition);
        }
        replaceText(text, newCursorPosition, false);
        syncInputBuffer(false);
        return true;
    }

    /**
     * Delete beforeLength characters of text before the current cursor position, and delete
     * afterLength characters of text after the current cursor position, excluding the
     * selection. Before and after refer to the order of the characters in the string,
     * not to their visual representation: this means you don't have to figure out the
     * direction of the text and can just use the indices as-is.
     * <p>
     * The lengths are supplied in Java chars, not in code points or in glyphs.
     * <p>
     * Since this method only operates on text before and after the selection, it can't
     * affect the contents of the selection. This may affect the composing span if the
     * span includes characters that are to be deleted, but otherwise will not change it. If
     * some characters in the composing span are deleted, the composing span will persist but
     * get shortened by however many chars inside it have been removed.
     *
     * @param beforeLength - The number of characters before the cursor to be deleted,
     *                     in code unit. If this is greater than the number of existing
     *                     characters between the beginning of the text and the cursor, then
     *                     this method does not fail but deletes all the characters in that range.
     *
     * @param afterLength  - The number of characters after the cursor to be deleted, in
     *                     code unit. If this is greater than the number of
     *                     existing characters between the cursor and the end of the
     *                     text, then this method does not fail but deletes all the characters in that range.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: deleteSurroundingText: beforeLen = " + beforeLength + ", afterLen = " + afterLength);
        }

        final Editable content = getEditable();
        if (content == null) {
            return false;
        }

        beginBatchEdit();

        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);

        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        // Ignore the composing text.
        int ca = getComposingSpanStart(content);
        int cb = getComposingSpanEnd(content);
        if (cb < ca) {
            int tmp = ca;
            ca = cb;
            cb = tmp;
        }
        if (ca != -1 && cb != -1) {
            if (ca < a) a = ca;
            if (cb > b) b = cb;
        }

        int deleted = 0;

        if (beforeLength > 0) {
            int start = a - beforeLength;
            if (start < 0) start = 0;
            content.delete(start, a);
            deleted = a - start;
        }

        if (afterLength > 0) {
            b = b - deleted;

            int end = b + afterLength;
            if (end > content.length()) end = content.length();

            content.delete(b, end);
        }

        endBatchEdit();

        return true;
    }

    /**
     * A variant of deleteSurroundingText(int, int). Major differences are:
     *
     *   1. The lengths are supplied in code points, not in Java chars or in glyphs.
     *
     *   2. This method does nothing if there are one or more invalid surrogate pairs
     *   in the requested range.
     *
     * Editor authors: In addition to the requirement in deleteSurroundingText(int, int),
     * make sure to do nothing when one ore more invalid surrogate pairs are found in the requested range.
     *
     * @param beforeLength The number of characters before the cursor to be deleted, in code points.
     *                     If this is greater than the number of existing characters between the beginning of the
     *                     text and the cursor, then this method does not fail but deletes all the characters in
     *                     that range.
     *
     * @param afterLength  The number of characters after the cursor to be deleted, in code points.
     *                     If this is greater than the number of existing characters between the cursor and
     *                     the end of the text, then this method does not fail but deletes all the characters in
     *                     that range.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean deleteSurroundingTextInCodePoints(int beforeLength, int afterLength) {
        // Our implementation performs the deletion around the current selection position of the editable text.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: deleteSurroundingTextInCodePoints: beforeLen = " +
                    beforeLength + ", afterLen = " + afterLength);
        }

        final Editable content = getEditable();
        if (content == null) {
            return false;
        }

        beginBatchEdit();

        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);

        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        // Ignore the composing text.
        int ca = getComposingSpanStart(content);
        int cb = getComposingSpanEnd(content);
        if (cb < ca) {
            int tmp = ca;
            ca = cb;
            cb = tmp;
        }
        if (ca != -1 && cb != -1) {
            if (ca < a) a = ca;
            if (cb > b) b = cb;
        }

        if (a >= 0 && b >= 0) {
            final int start = findIndexBackward(content, a, Math.max(beforeLength, 0));
            if (start != INVALID_INDEX) {
                final int end = findIndexForward(content, b, Math.max(afterLength, 0));
                if (end != INVALID_INDEX) {
                    final int numDeleteBefore = a - start;
                    if (numDeleteBefore > 0) {
                        content.delete(start, a);
                    }
                    final int numDeleteAfter = end - b;
                    if (numDeleteAfter > 0) {
                        content.delete(b - numDeleteBefore, end - numDeleteBefore);
                    }
                }
            }
            // NOTE: You may think we should return false here if start and/or end is INVALID_INDEX,
            // but the truth is that IInputConnectionWrapper running in the middle of IPC calls
            // always returns true to the IME without waiting for the completion of this method as
            // IInputConnectionWrapper#isAtive() returns true.  This is actually why some methods
            // including this method look like asynchronous calls from the IME.
        }

        endBatchEdit();

        return true;
    }

    /**
     * Tell the editor that you are done with a batch edit previously initiated with
     * beginBatchEdit(). This ends the latest batch only.
     * <p>
     * IME authors: make sure you call this exactly once for each call to beginBatchEdit().
     * <p>
     * Editor authors: please be careful about batch edit nesting. Updates still to be
     * held back until the end of the last batch edit.
     *
     * @return true if there is still a batch edit in progress after closing the
     * latest one (in other words, if the nesting count is > 0), false otherwise
     * or if the input connection is no longer valid.
     */
    @Override
    public boolean endBatchEdit() {
        // Our implementation does nothing and returns false.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: endBatchEdit()");
        }
        return false;
    }

    /**
     * Have the text editor finish whatever composing text is
     * currently active. This simply leaves the text as-is,
     * removing any special composing styling or other state
     * that was around it. The cursor position remains unchanged.
     *
     * IME authors: be aware that this call may be expensive with some editors.
     *
     * Editor authors: please note that the cursor may be anywhere in the
     * contents when this is called, including in the middle of the composing
     * span or in a completely unrelated place. It must not move.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean finishComposingText() {
        // Our implementation removes the composing state from the
        // current editable text.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: finishComposingText()");
        }
        final Editable content = getEditable();
        if (content != null) {
            beginBatchEdit();
            removeComposingSpans(content);
            endBatchEdit();
        }
        return true;
    }

    /**
     * Retrieve the current capitalization mode in effect at the
     * current cursor position in the text. See TextUtils.getCapsMode
     * for more information.
     *
     * This method may fail either if the input connection has
     * become invalid (such as its process crashing) or the client
     * is taking too long to respond with the text (it is given a
     * couple seconds to return). In either case, 0 is returned.
     *
     * This method does not affect the text in the editor in any way,
     * nor does it affect the selection or composing spans.
     *
     * Editor authors: please be careful of race conditions in
     * implementing this call. An IME can change the cursor
     * position and use this method right away; you need to
     * make sure the returned value is consistent with the results
     * of the latest edits and changes to the cursor position.
     *
     * @param reqModes - The desired modes to retrieve, as defined by TextUtils.getCapsMode.
     *                 These constants are defined so that you can simply pass the current
     *                 TextBoxAttribute.contentType value directly in to here.
     *
     * @return the caps mode flags that are in effect at the current cursor
     * position. See TYPE_TEXT_FLAG_CAPS_* in InputType.
     */
    @Override
    public int getCursorCapsMode(int reqModes) {
        // Our implementation uses TextUtils.getCapsMode to get the
        // cursor caps mode for the current selection position in the
        // editable text.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: getCursorCapsMode()");
        }

        final Editable content = getEditable();
        if (content == null) return 0;

        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);

        if (a > b) {
            a = b;
        }

        return TextUtils.getCapsMode(content, a, reqModes);
    }

    /**
     * Retrieve the current text in the input connection's editor, and monitor for
     * any changes to it. This function returns with the current text, and
     * optionally the input connection can send updates to the
     * input method when its text changes.
     * <p>
     * This method may fail either if the input connection has become
     * invalid (such as its process crashing) or the client is taking
     * too long to respond with the text (it is given a couple seconds to return).
     * In either case, null is returned.
     *
     * @param request - Description of how the text should be returned
     *
     * @param flags   - Additional options to control the client, either 0 or GET_EXTRACTED_TEXT_MONITOR.
     *
     * @return an ExtractedText object describing the state of the text view and containing the
     * extracted text itself, or null if the input connection is no longer valid or the
     * editor can't comply with the request for some reason.
     */
    @Nullable
    @Override
    public ExtractedText getExtractedText(@NonNull ExtractedTextRequest request, int flags) {
        // Our implementation always returns null.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: getExtractedText: " + request.toString() + ", flags = " + flags);
        }
        return null;
    }

    /**
     * Called by the InputMethodManager to enable application
     * developers to specify a dedicated Handler on which
     * incoming IPC method calls from input methods will be dispatched.
     *
     * Note: This does nothing when called from input methods.
     *
     * @return null to use the default Handler.
     */
    @Nullable
    @Override
    public Handler getHandler() {
        // Our implementation uses the default handler.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: getHandler()");
        }
        return null;
    }

    /**
     * Gets the selected text, if any.
     *
     * This method may fail if either the input connection has
     * become invalid (such as its process crashing) or the client
     * is taking too long to respond with the text (it is given a couple
     * of seconds to return). In either case, null is returned.
     *
     * This method must not cause any changes in the editor's state.
     *
     * If GET_TEXT_WITH_STYLES is supplied as flags, the editor should
     * return a SpannableString with all the spans set on the text.
     *
     * IME authors: please consider this will trigger an IPC round-trip
     * that will take some time. Assume this method consumes a lot of time.
     *
     * Editor authors: please be careful of race conditions in implementing
     * this call. An IME can make a change to the text or change the
     * selection position and use this method right away; you need to make
     * sure the returned value is consistent with the results of the latest edits.
     *
     * @param flags - Supplies additional options controlling how the text is
     *              returned. May be either 0 or GET_TEXT_WITH_STYLES.
     *
     * @return the text that is currently selected, if any, or null if no text
     * is selected. In N and later, returns false when the target
     * application does not implement this method.
     */
    @Nullable
    @Override
    public CharSequence getSelectedText(int flags) {
        // Our implementation returns the text currently selected, or null if none is
        // selected.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: getSelectedText: flags = " + flags);
        }
        final Editable content = getEditable();
        if (content == null) return null;

        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);

        if (a > b) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        if (a == b) return null;

        if ((flags & GET_TEXT_WITH_STYLES) != 0) {
            return content.subSequence(a, b);
        }
        return TextUtils.substring(content, a, b);
    }

    /**
     * Get n characters of text before the current cursor position.
     * <p>
     * This method may fail either if the input connection has become
     * invalid (such as its process crashing) or the editor is taking too
     * long to respond with the text (it is given a couple seconds to return).
     * In either case, null is returned. This method does not affect the text in
     * the editor in any way, nor does it affect the selection or composing spans.
     * <p>
     * If GET_TEXT_WITH_STYLES is supplied as flags, the editor should return
     * a SpannableString with all the spans set on the text.
     * <p>
     * IME authors: please consider this will trigger an IPC round-trip
     * that will take some time. Assume this method consumes a lot of time.
     * Also, please keep in mind the Editor may choose to return less characters
     * than requested even if they are available for performance reasons.
     * <p>
     * Editor authors: please be careful of race conditions in implementing this
     * call. An IME can make a change to the text and use this method right away;
     * you need to make sure the returned value is consistent with the result of
     * the latest edits. Also, you may return less than n characters if performance
     * dictates so, but keep in mind IMEs are relying on this for many functions: you
     * should not, for example, limit the returned value to the current line, and
     * specifically do not return 0 characters unless the cursor is really at the
     * start of the text.
     *
     * @param length - The expected length of the text.
     * @param flags  - Supplies additional options controlling how the text is returned.
     *               May be either 0 or GET_TEXT_WITH_STYLES.
     * @return the text before the cursor position; the length of the returned text might be less than n.
     */
    @Nullable
    @Override
    public CharSequence getTextBeforeCursor(int length, int flags) {
        // Our implementation returns the given amount of text from the
        // current cursor position in the buffer.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: getTextBeforeCursor: length = " + length + ", flags = " + flags);
        }
        final Editable content = getEditable();
        if (content == null) return null;

        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);

        if (a > b) {
            a = b;
        }

        if (a <= 0) {
            return "";
        }

        if (length > a) {
            length = a;
        }

        if ((flags & GET_TEXT_WITH_STYLES) != 0) {
            return content.subSequence(a - length, a);
        }
        return TextUtils.substring(content, a - length, a);
    }

    /**
     * Get n characters of text after the current cursor position.
     *
     * This method may fail either if the input connection has become
     * invalid (such as its process crashing) or the client is taking too
     * long to respond with the text (it is given a couple seconds to return).
     * In either case, null is returned.
     *
     * This method does not affect the text in the editor in any way, nor
     * does it affect the selection or composing spans.
     *
     * If GET_TEXT_WITH_STYLES is supplied as flags, the editor should
     * return a SpannableString with all the spans set on the text.
     *
     * IME authors: please consider this will trigger an IPC round-trip that
     * will take some time. Assume this method consumes a lot of time.
     *
     * Editor authors: please be careful of race conditions in implementing this
     * call. An IME can make a change to the text and use this method right away;
     * you need to make sure the returned value is consistent with the result of the
     * latest edits. Also, you may return less than n characters if performance
     * dictates so, but keep in mind IMEs are relying on this for many functions:
     * you should not, for example, limit the returned value to the current line,
     * and specifically do not return 0 characters unless the cursor is really at the end of the text.
     *
     * @param length - The expected length of the text.
     *
     * @param flags - Supplies additional options controlling how the text is
     *              returned. May be either 0 or GET_TEXT_WITH_STYLES.
     *
     * @return the text after the cursor position; the length of the
     * returned text might be less than n.
     */
    @Nullable
    @Override
    public CharSequence getTextAfterCursor(int length, int flags) {
        // Our implementation returns the given amount of text from the
        // current cursor position in the buffer.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: getTextAfterCursor: length = " + length + ", flags = " + flags);
        }
        final Editable content = getEditable();
        if (content == null) return null;

        int a = Selection.getSelectionStart(content);
        int b = Selection.getSelectionEnd(content);

        if (a > b) {
            b = a;
        }

        // Guard against the case where the cursor has not been positioned yet.
        if (b < 0) {
            b = 0;
        }

        if (b + length > content.length()) {
            length = content.length() - b;
        }


        if ((flags & GET_TEXT_WITH_STYLES) != 0) {
            return content.subSequence(b, b + length);
        }
        return TextUtils.substring(content, b, b + length);
    }

    /**
     * Perform a context menu action on the field.
     *
     * @param id - one of: selectAll, startSelectingText, stopSelectingText, cut,
     *           copy, paste, copyUrl, or switchInputMethod
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean performContextMenuAction(int id) {
        // Our implementation does nothing and returns false.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: performContextMenuAction: id = " + id);
        }
        return false;
    }

    /**
     * Have the editor perform an action it has said it can do.
     *
     * This is typically used by IMEs when the user presses the key associated with the action.
     *
     * @param actionCode - This must be one of the action constants for
     *                   EditorInfo.editorType, such as EditorInfo.EDITOR_ACTION_GO.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean performEditorAction(int actionCode) {
        // If the associated window is waiting for a line event, we treat
        // IME_ACTION_DONE like Enter - i.e. the user wishes to submit
        // the current text to the terp. To ensure the text is submitted, we
        // append a new line if there isn't already one there (call syncInputBuffer
        // with finalise = true). We also render the updated editable contents
        // on that window so user can see the change.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: performEditorAction: " + actionCode);
        }
        switch (actionCode) {
            case EditorInfo.IME_ACTION_DONE:
                if (DEBUG_INPUT_CONNECTION) {
                    GLKLogger.debug("Editor action DONE: '" + mEditable + "'");
                }
                syncInputBuffer(true);
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * API to send private commands from an input method to its connected editor.
     * This can be used to provide domain-specific features that are only
     * known between certain input methods and their clients. Note that
     * because the InputConnection protocol is asynchronous, you have no
     * way to get a result back or know if the client understood the command;
     * you can use the information in EditorInfo to determine if a client supports a particular command.
     *
     * @param action - Name of the command to be performed. This must be a
     *               scoped name, i.e. prefixed with a package name
     *               you own, so that different developers will not create
     *               conflicting commands.
     *
     * @param data - Any data to include with the command.
     *
     * @return true if the command was sent (whether or not the associated editor
     * understood it), false if the input connection is no longer valid.
     */
    @Override
    public boolean performPrivateCommand(String action, Bundle data) {
        // Our implementation does nothing and returns false
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: performPrivateCommand: action = " + action);
        }
        return false;
    }

    /**
     * Called back when the connected IME switches between fullscreen and normal modes.
     * <p>
     * Note: On O and later devices, input methods are no longer allowed to
     * directly call this method at any time. To signal this event in the target
     * application, input methods should always call updateFullscreenMode() instead.
     * This approach should work on API N_MR1 and prior devices.
     *
     * @param enabled - whether fullscreen is enabled.
     * @return For editor authors, the return value will always be ignored.
     * For IME authors, this always returns true on N_MR1 and prior devices
     * and false on O and later devices.
     */
    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        // Our implementation does nothing and returns true.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: reportFullscreenMode: enabled = " + enabled);
        }
        return true;
    }

    /**
     * Called by the input method to ask the editor for calling
     * back updateCursorAnchorInfo(android.view.View, CursorAnchorInfo)
     * to notify cursor/anchor locations.
     *
     * @param cursorUpdateMode - CURSOR_UPDATE_IMMEDIATE and/or
     *                         CURSOR_UPDATE_MONITOR. Pass 0 to
     *                         disable the effect of CURSOR_UPDATE_MONITOR.
     *
     * @return true if the request is scheduled. false to indicate that when the
     * application will not call updateCursorAnchorInfo(android.view.View,
     * CursorAnchorInfo). In N and later, returns false also when the
     * target application does not implement this method.
     */
    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        // Our implementation does nothing and returns false.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: requestCursorUpdates: cursorUpdateMode = " + cursorUpdateMode);
        }
        return false;
    }

    /**
     * Send a key event to the process that is currently attached through this input connection.
     * The event will be dispatched like a normal key event, to the currently focused view;
     * this generally is the view that is providing this InputConnection, but due to the
     * asynchronous nature of this protocol that can not be guaranteed and the focus
     * may have changed by the time the event is received.
     *
     * This method can be used to send key events to the application. For example,
     * an on-screen keyboard may use this method to simulate a hardware keyboard.
     * There are three types of standard keyboards, numeric (12-key), predictive (20-key)
     * and ALPHA (QWERTY). You can specify the keyboard type by specify the device id of the key event.
     *
     * You will usually want to set the flag KeyEvent.FLAG_SOFT_KEYBOARD on all key event
     * objects you give to this API; the flag will not be set for you.
     *
     * Note that it's discouraged to send such key events in normal operation; this is
     * mainly for use with TYPE_NULL type text fields. Use the commitText(CharSequence, int)
     * family of methods to send text to the application instead.
     *
     * @param event - The key event.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean sendKeyEvent(@NonNull KeyEvent event) {
        // Our implementation does nothing and returns false.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: sendKeyEvent: " + (char) event.getUnicodeChar());
        }
        return false;
    }

    /**
     * Replace the currently composing text with the given text, and
     * set the new cursor position. Any composing text set
     * previously will be removed automatically.
     *
     * If there is any composing span currently active, all
     * characters that it comprises are removed. The passed text is
     * added in its place, and a composing span is added to this text.
     * If there is no composing span active, the passed text is added at
     * the cursor position (removing selected characters first if any), and a
     * composing span is added on the new text. Finally, the cursor is moved
     * to the location specified by newCursorPosition.
     *
     * This is usually called by IMEs to add or remove or change characters
     * in the composing span. Calling this method will cause the editor to
     * call onUpdateSelection(int, int, int, int, int, int) on the current
     * IME after the batch input is over.
     *
     * Editor authors: please keep in mind the text may be very similar
     * or completely different than what was in the composing span at
     * call time, or there may not be a composing span at all. Please note
     * that although it's not typical use, the string may be empty. Treat
     * this normally, replacing the currently composing text with an empty
     * string. Also, be careful with the cursor position. IMEs rely on
     * this working exactly as described above. Since this changes the
     * contents of the editor, you need to make the changes known to the input
     * method by calling updateSelection(View, int, int, int, int), but be
     * careful to wait until the batch edit is over if one is in progress. Note
     * that this method can set the cursor position on either edge of the
     * composing text or entirely outside it, but the IME may also go on to
     * move the cursor position to within the composing text in a subsequent
     * call so you should make no assumption at all: the composing text
     * and the selection are entirely independent.
     *
     * @param text - The composing text with styles if necessary. If no style
     *             object attached to the text, the default style for
     *             composing text is used. See Spanned for how to attach
     *             style object to the text. SpannableString and SpannableStringBuilder
     *             are two implementations of the interface Spanned
     *
     * @param newCursorPosition - The new cursor position around the
     *                          text. If > 0, this is relative to the end of
     *                          the text - 1; if <= 0, this is relative to the
     *                          start of the text. So a value of 1 will always
     *                          advance you to the position after the full text being
     *                          inserted. Note that this means you can't position
     *                          the cursor within the text, because the editor can
     *                          make modifications to the text you are providing
     *                          so it is not possible to correctly specify locations there.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        // Our implementation places the given text into the editable,
        // replacing any existing composing text. The new text is marked as
        // in a composing state with the composing style. If the associated window
        // is waiting for a line event, we also render the updated editable contents
        // on that window so user can see the change.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: setComposingText: text = " + text + ", newCursorPosition = " + newCursorPosition);
        }
        replaceText(text, newCursorPosition, true);
        syncInputBuffer(false);
        return true;
    }

    /**
     * Mark a certain region of text as composing text. If there was a
     * composing region, the characters are left as they were and the
     * composing span removed, as if finishComposingText() has been called.
     * The default style for composing text is used.
     *
     * The passed indices are clipped to the contents bounds. If the
     * resulting region is zero-sized, no region is marked and the effect is
     * the same as that of calling finishComposingText(). The order of start
     * and end is not important. In effect, the region from start to end and
     * the region from end to start is the same. Editor authors, be ready to
     * accept a start that is greater than end.
     *
     * Since this does not change the contents of the text, editors should not
     * call updateSelection(View, int, int, int, int) and IMEs should not
     * receive onUpdateSelection(int, int, int, int, int, int).
     *
     * This has no impact on the cursor/selection position. It may result in
     * the cursor being anywhere inside or outside the composing region,
     * including cases where the selection and the composing region
     * overlap partially or entirely.
     *
     * @param start - the position in the text at which the composing region begins.
     *
     * @param end - the position in the text at which the composing region ends.
     *
     * @return true on success, false if the input connection is no longer
     * valid. In N and later, false is returned when the target
     * application does not implement this method.
     */
    @Override
    public boolean setComposingRegion(int start, int end) {
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: setComposingRegion: start = " + start + ", end = " + end);
        }
        final Editable content = getEditable();
        if (content != null) {
            beginBatchEdit();
            removeComposingSpans(content);
            int a = start;
            int b = end;
            if (a > b) {
                int tmp = a;
                a = b;
                b = tmp;
            }
            // Clip the end points to be within the content bounds.
            final int length = content.length();
            if (a < 0) a = 0;
            if (b < 0) b = 0;
            if (a > length) a = length;
            if (b > length) b = length;

            ensureDefaultComposingSpans();
            if (mDefaultComposingSpans != null) {
                if (b > a) {
                    for (Object mDefaultComposingSpan : mDefaultComposingSpans) {
                        content.setSpan(mDefaultComposingSpan, a, b,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
                    }
                }
            }

            if (b > a) {
                content.setSpan(COMPOSING, a, b,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
            }

            endBatchEdit();
        }
        return true;
    }

    /**
     * Set the selection of the text editor. To set the cursor position, start and end
     * should have the same value.
     *
     * Since this moves the cursor, calling this method will cause the editor to
     * call onUpdateSelection(int, int, int, int, int, int) on the current IME
     * after the batch input is over. Editor authors, for this to happen you need
     * to make the changes known to the input method by calling
     * updateSelection(View, int, int, int, int), but be careful to wait until
     * the batch edit is over if one is in progress.
     *
     * This has no effect on the composing region which must stay unchanged. The
     * order of start and end is not important. In effect, the region from start
     * to end and the region from end to start is the same. Editor authors, be ready
     * to accept a start that is greater than end.
     *
     * @param start - the character index where the selection should start.
     *
     * @param end - the character index where the selection should end.
     *
     * @return true on success, false if the input connection is no longer valid.
     */
    @Override
    public boolean setSelection(int start, int end) {
        // Our implementation changes the selection position in the current
        // editable text.
        if (DEBUG_INPUT_CONNECTION) {
            GLKLogger.debug("GLKInputConnection: setSelection: start = " + start + ", end = " + end);
        }
        final Editable content = getEditable();
        if (content == null) return false;
        int len = content.length();
        if (start > len || end > len || start < 0 || end < 0) {
            // If the given selection is out of bounds, just ignore it.
            // Most likely the text was changed out from under the IME,
            // and the IME is going to have to update all of its state
            // anyway.
            return true;
        }
        //  if (start == end && MetaKeyKeyListener.getMetaState(content, MetaKeyKeyListener.META_SELECTING) != 0) {
        // If we are in selection mode, then we want to extend the
        // selection instead of replacing it.
        //       Selection.extendSelection(content, start);
        //   } else {
        Selection.setSelection(content, start, end);
        //    }
        return true;
    }

    private void ensureDefaultComposingSpans() {
        if (mDefaultComposingSpans == null) {
            Context context;
            if (mView != null) {
                context = mView.getContext();
            } else {
                context = null;
            }
            if (context != null) {
                TypedArray ta = context.getTheme()
                        .obtainStyledAttributes(new int[]{R.attr.candidatesTextStyleSpans});
                CharSequence style = ta.getText(0);
                ta.recycle();
                if (style != null && style instanceof Spanned) {
                    mDefaultComposingSpans = ((Spanned) style).getSpans(
                            0, style.length(), Object.class);
                }
            }
        }
    }

    private void replaceText(CharSequence text, int newCursorPosition,
                             boolean composing) {
        final Editable content = getEditable();
        if (content == null) {
            return;
        }

        beginBatchEdit();

        // delete composing text set previously.
        int a = getComposingSpanStart(content);
        int b = getComposingSpanEnd(content);

        if (DEBUG_INPUT_CONNECTION) {
            FabLogger.debug("Composing span: " + a + " to " + b);
        }

        if (b < a) {
            int tmp = a;
            a = b;
            b = tmp;
        }

        if (a != -1 && b != -1) {
            removeComposingSpans(content);
        } else {
            a = Selection.getSelectionStart(content);
            b = Selection.getSelectionEnd(content);
            if (a < 0) a = 0;
            if (b < 0) b = 0;
            if (b < a) {
                int tmp = a;
                a = b;
                b = tmp;
            }
        }

        if (composing) {
            Spannable sp;
            if (!(text instanceof Spannable)) {
                sp = new SpannableStringBuilder(text);
                text = sp;
                ensureDefaultComposingSpans();
                if (mDefaultComposingSpans != null) {
                    for (Object mDefaultComposingSpan : mDefaultComposingSpans) {
                        int len = sp.length();
                        if (len > 0) {
                            sp.setSpan(mDefaultComposingSpan, 0, len,
                                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_COMPOSING);
                        }
                    }
                }
            } else {
                sp = (Spannable) text;
            }
            setComposingSpans(sp);
        }

        if (DEBUG_INPUT_CONNECTION) {
            FabLogger.debug("Replacing from " + a + " to " + b + " with \""
                    + text + "\", composing=" + composing
                    + ", type=" + text.getClass().getCanonicalName());
        }

        // Position the cursor appropriately, so that after replacing the
        // desired range of text it will be located in the correct spot.
        // This allows us to deal with filters performing edits on the text
        // we are providing here.
        if (newCursorPosition > 0) {
            newCursorPosition += b - 1;
        } else {
            newCursorPosition += a;
        }
        if (newCursorPosition < 0) newCursorPosition = 0;
        if (newCursorPosition > content.length())
            newCursorPosition = content.length();
        Selection.setSelection(content, newCursorPosition);

        content.replace(a, b, text);

        if (DEBUG_INPUT_CONNECTION) {
            FabLogger.debug("Final text: '" + content + "'");
        }

        endBatchEdit();
    }

    private void syncInputBuffer(boolean finalise) {
        if (DEBUG_INPUT_CONNECTION) {
            FabLogger.debug("GLKInputConnection: syncInputBuffer: finalise = " + finalise);
        }
        if (mEditable != null && mView != null) {
            mView.clearInputBuffer();

            int len = mEditable.length();
            if (len > 0 || finalise) {
                int keyCodes[];
                if (finalise && (len == 0 || mEditable.charAt(len - 1) != '\n')) {
                    keyCodes = new int[len + 1];
                    keyCodes[len] = '\n';
                } else {
                    keyCodes = new int[len];
                }
                for (int i = 0; i < len; i++) {
                    keyCodes[i] = mEditable.charAt(i);
                }

                GLKActivity act = GLKUtils.getActivityForView(mView);
                if (act != null && act.sendKeyCodes(mView, keyCodes, null)) {
                    if (DEBUG_INPUT_CONNECTION) {
                        FabLogger.debug("GLKInputConnection: clear contents of editable.");
                    }
                    mEditable.clear();
                    InputMethodManager imm = (InputMethodManager) mView.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && imm.isActive(mView)) {
                        if (DEBUG_INPUT_CONNECTION) {
                            FabLogger.debug("GLKInputConnection: restartInput.");
                        }
                        imm.restartInput(mView);
                    }
                }
            }
        }
    }

    private static class ComposingText implements NoCopySpan {
    }
}
