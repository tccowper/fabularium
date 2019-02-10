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
package com.luxlunae.glk.controller;

import android.support.annotation.Keep;
import android.support.annotation.NonNull;

import com.luxlunae.glk.GLKConstants;

@Keep
public class GLKEvent {
    public int type;
    public int win;
    public int val1, val2;

    public void debugEvent(int w, int ch) {
        type = GLKConstants.evtype_Debug;
        win = w;
        val1 = ch;
        val2 = 0;
    }

    public void charEvent(int w, int ch) {
		/* Initialises this GLKEvent to a charEvent */
        type = GLKConstants.evtype_CharInput;
        win = w;
        val1 = ch;
        val2 = 0;
    }

    public void lineEvent(int w, int nChars, int terminator) {
		/* Initialises this GLKEvent to a lineEvent */
        type = GLKConstants.evtype_LineInput;
        win = w;
        val1 = nChars;

        // See GLK Spec 4.2:
        // In the event structure, win tells what window the event came from. val1 tells how many
        // characters were entered. val2 will be 0 unless input was ended by a special terminator key,
        // in which case val2 will be the keycode (one of the values passed to glk_set_terminators_line_event()).
        val2 = (terminator == GLKConstants.keycode_Return) ? 0 : terminator;
    }

    public void mouseEvent(int w, int x, int y) {
		/* Initialises this GLKEvent to a mouseEvent */
        type = GLKConstants.evtype_MouseInput;
        win = w;
        val1 = x;
        val2 = y;
    }

    void timerEvent() {
		/* Initialises this GLKEvent to a timerEvent */
        type = GLKConstants.evtype_Timer;
        win = 0; val1 = val2 = 0;
    }

    /**
     * Some platforms allow the player to resize the Glk window during play. This
     * will naturally change the sizes of your windows. If this occurs, then
     * immediately *after* all the rearrangement, glk_select() will return an
     * event whose type is evtype_Arrange. You can use this notification to
     * redisplay the contents of a graphics or text grid window whose size has
     * changed. [[The display of a text buffer window is entirely up to the
     * library, so you don't need to worry about those.]]
     * <p>
     * In the event structure, win will be NULL if all windows are affected. If
     * only some windows are affected, win will refer to a window which contains
     * all the affected windows. [[You can always play it safe, ignore win, and
     * redraw every graphics and text grid window.]] val1 and val2 will be 0.
     * <p>
     * An arrangement event is guaranteed to occur whenever the player causes any
     * window to change size, as measured by its own metric. [[Size changes caused
     * by you -- for example, if you open, close, or resize a window -- do not
     * trigger arrangement events. You must be aware of the effects of your window
     * management, and redraw the windows that you affect.]]
     * <p>
     * [[It is possible that several different player actions can cause windows to
     * change size. For example, if the player changes the screen resolution, an
     * arrangement event might be triggered. This might also happen if the player
     * changes his display font to a different size; the windows would then be
     * different "sizes" in the metric of rows and columns, which is the important
     * metric and the only one you have access to.]]
     * <p>
     * Arrangement events, like timer events, can be returned by
     * glk_select_poll(). But this will not occur on all platforms. You must be
     * ready to receive an arrangement event when you call glk_select_poll(), but
     * it is possible that it will not arrive until the next time you call
     * glk_select(). [[This is because on some platforms, window resizing is
     * handled as part of player input; on others, it can be triggered by an
     * external process such as a window manager.]]
     */
    public void arrangeEvent() {
        type = GLKConstants.evtype_Arrange;
        win = GLKConstants.NULL;
        val1 = val2 = 0;
    }

    public void redrawEvent() {
        type = GLKConstants.evtype_Redraw;
        win = 0; val1 = val2 = 0;
    }

    public void hyperlinkEvent(int w, int val) {
        type = GLKConstants.evtype_Hyperlink;
        win = w;
        val1 = val;
    }

    public void copy(@NonNull GLKEvent event) {
        this.win = event.win;
        this.val1 = event.val1;
        this.val2 = event.val2;
        this.type = event.type;
    }
}
