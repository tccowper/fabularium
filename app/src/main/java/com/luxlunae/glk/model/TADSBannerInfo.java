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
package com.luxlunae.glk.model;

import android.support.annotation.Keep;

@Keep
public class TADSBannerInfo {
    /* alignment */
    public int align;

    /* style flags - these indicate the style flags actually in use */
    public long style;

    /*
     *   Actual on-screen size of the banner, in rows and columns.  If the
     *   banner is displayed in a proportional font or can display multiple
     *   fonts of different sizes, this is approximated by the number of "0"
     *   characters in the window's default font that will fit in the
     *   window's display area.
     */
    public int rows;
    public int columns;

    /*
     *   Actual on-screen size of the banner in pixels.  This is meaningful
     *   only for full HTML interpreter; for text-only interpreters, these
     *   are always set to zero.
     *
     *   Note that even if we're running on a GUI operating system, these
     *   aren't meaningful unless this is a full HTML interpreter.  Text-only
     *   interpreters should always set these to zero, even on GUI OS's.
     */
    public int pix_width;
    public int pix_height;

    /*
     *   OS line wrapping flag.  If this is set, the window uses OS-level
     *   line wrapping because the window uses a proportional font, so the
     *   caller does not need to (and should not) perform line breaking in
     *   text displayed in the window.
     *
     *   Note that OS line wrapping is a PERMANENT feature of the window.
     *   Callers can note this information once and expect it to remain
     *   fixed through the window's lifetime.
     */
    public int os_line_wrap;
}
