/*
 * Copyright (C) 2019 Tim Cadogan-Cowper.
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

package com.luxlunae.bebek.model.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.model.MAdventure;

public class MMyStack {
    // Gives us the same functionality as a stack, but restricts it to 100
    private static final int MAXLENGTH = 100;
    @NonNull
    private MAdventure.MGameState[] mStates = new MAdventure.MGameState[MAXLENGTH + 1];
    private int mStart = -1;
    private int mEnd = -1;

    public void push(@Nullable MAdventure.MGameState state) {
        if (mStart > mEnd) {
            mStart++;
        }
        if (mStart > MAXLENGTH) {
            mStart = 0;
        }
        mEnd++;

        if (mEnd > MAXLENGTH) {
            mEnd = 0;
            if (mStart == 0) {
                mStart = 1;
            }
        }
        if (mStart == -1) {
            mStart = 0;
        }
        mStates[mEnd] = state;
    }

    public int count() {
        if (mEnd >= mStart) {
            if (mStart == -1 && mEnd == -1) {
                return 0;
            }
            return mEnd - mStart + 1;
        } else {
            return MAXLENGTH + 1;
        }
    }

    @Nullable
    public MAdventure.MGameState peek() {
        if (count() == 0) {
            return null;
        }
        return mStates[mEnd];
    }

    @Nullable
    public MAdventure.MGameState pop() {
        MAdventure.MGameState state = peek();
        if (state != null) {
            mEnd--;
            if (mEnd < 0) {
                mEnd = MAXLENGTH;
            }
        }
        return state;
    }

    public void clear() {
        mStart = -1;
        mEnd = -1;
    }
}
