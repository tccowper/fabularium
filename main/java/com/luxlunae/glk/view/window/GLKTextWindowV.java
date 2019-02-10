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

import android.content.Context;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;
import com.luxlunae.glk.view.keyboard.GLKInputConnection;

public abstract class GLKTextWindowV extends GLKNonPairV {
    @NonNull
    private final GLKTextWindowM mModel;
    protected boolean mShowSystemKeyboard = false;
    @Nullable
    private int[] mCustomLineTerminators = null;        // custom line terminators (in addition to the default of [ENTER])

    GLKTextWindowV(@NonNull Context c, @NonNull GLKTextWindowM model) {
        super(c, model);
        mModel = model;
    }

    public abstract int sendKey(int key);

    public abstract void clearInputBuffer();

    public boolean isTerminator(int key) {
        return GLKUtils.isTerminator(key, mCustomLineTerminators);
    }

    public void setShowSystemKeyboardOnTouch(boolean on) {
        mShowSystemKeyboard = on;
    }

    /**
     * Check whether the called view is a text editor, in which case it would make sense
     * to automatically display a soft input window for it. Subclasses should override
     * this if they implement onCreateInputConnection(EditorInfo) to return true if a
     * call on that method would return a non-null InputConnection, and they are really
     * a first-class editor that the user would normally start typing on when they go into
     * a window containing your view.
     * <p>
     * The default implementation always returns false. This does not mean that its
     * onCreateInputConnection(EditorInfo) will not be called or the user can not otherwise
     * perform edits on your view; it is just a hint to the system that this is not the primary
     * purpose of this view.
     *
     * @return Returns true if this view is a text editor, else false.
     */
    @Override
    public boolean onCheckIsTextEditor() {
        return mShowSystemKeyboard;
    }

    @CallSuper
    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent arg0) {
        // Show the system keyboard if appropriate
        requestFocus();
        if (!mShowSystemKeyboard) {
            return false;
        }
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_IMPLICIT_ONLY);
        }
        return true;
    }

    @CallSuper
    @Override
    public void updateContents() {
        super.updateContents();
        mCustomLineTerminators = mModel.getTerminators();
    }

    /**
     * Create a new InputConnection for an InputMethod to interact with the view. The default
     * implementation returns null, since it doesn't support input methods. You can override
     * this to implement such support. This is only needed for views that take focus and text input.
     *
     * When implementing this, you probably also want to implement onCheckIsTextEditor() to indicate
     * you will return a non-null InputConnection.
     *
     * Also, take good care to fill in the EditorInfo object correctly and in its entirety, so that
     * the connected IME can rely on its values. For example, initialSelStart and initialSelEnd members
     * must be filled in with the correct cursor position for IMEs to work correctly with your application.
     *
     * @param outAttrs - Fill in with attribute information about the connection.
     *
     * @return the new InputConnection.
     */
    @Nullable
    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        if (!mShowSystemKeyboard) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            // null or an empty array means that
            // commitContent(InputContentInfo, int, Bundle) is
            // not supported in this editor.
            outAttrs.contentMimeTypes = null;
        }
        outAttrs.initialCapsMode = 0;   // don't start editing in caps mode

        // The text offset of the start and end of the selection at the time editing begins; -1 if
        // not known. Keep in mind that, without knowing the cursor position, many IMEs
        // will not be able to offer their full feature set and may behave in
        // unpredictable ways: pass the actual cursor position here if possible at all.
        outAttrs.initialSelStart = 0;
        outAttrs.initialSelEnd = 0;

        // A label to show to the user describing the text they are writing.
        outAttrs.label = "Fabularium input";

        // The content type of the text box.
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT;

        // Extended type information for the editor, to help the IME better integrate with it.
        outAttrs.imeOptions |= EditorInfo.IME_ACTION_DONE;

        return new GLKInputConnection(this);
    }
}