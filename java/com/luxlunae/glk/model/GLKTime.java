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
import android.support.annotation.NonNull;

@Keep
public class GLKTime {

	/* The current Unix time is stored in the structure. This is the number of seconds since the beginning of 1970 (UTC). */

    /* The first two values in the structure should be considered a single
    *signed* 64-bit number. This allows the glktimeval_t to store a reasonable
    range of values in the future and past. The high_sec value will remain zero
    until sometime in 2106. If your computer is running in 1969, perhaps due to
    an unexpected solar flare, then high_sec will be negative. */
    public int high_sec;    // signed int
    public long low_sec;    // unsigned int

    /* The third value in the structure represents a fraction of a second, in 
    microseconds (from 0 to 999999). The resolution of the glk_current_time()
	call is platform-dependent; the microsec value may not be updated 
	continuously. */
    public int microsec;

    @NonNull
    @Override
    public String toString() {
        return "GLKTime object: high_sec = " + high_sec + ", low_sec = " + low_sec + ", microsec = " + microsec;
    }

    public void setToNow() {
        setTo(System.currentTimeMillis());
    }

    public void setTo(long ms) {
        long s = (long) Math.floor(ms / 1000D);
        microsec = (int) ((ms % 1000) * 1000);

        // store the low 4 bytes in low_sec
        low_sec = s & 0xFF000000L |
                s & 0x00FF0000L |
                s & 0x0000FF00L |
                s & 0x000000FFL;

        // store the high 4 bytes in high_sec
        high_sec = (int) (s >> 32);
    }
}
