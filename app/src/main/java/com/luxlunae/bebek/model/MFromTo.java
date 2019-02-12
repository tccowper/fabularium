/*
 * Original ADRIFT code Copyright (C) 1997 - 2018 Campbell Wild
 * This port and modifications Copyright (C) 2018 - 2019 Tim Cadogan-Cowper.
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

package com.luxlunae.bebek.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.glk.GLKLogger;

public class MFromTo implements Cloneable {
    @NonNull
    private final MAdventure mAdv;
    int iFrom;
    int iTo;
    private int iValue = Integer.MIN_VALUE;

    MFromTo(@NonNull MAdventure adv) {
        mAdv = adv;
    }

    public int getValue() {
        if (iValue == Integer.MIN_VALUE) {
            iValue = mAdv.getRand(iFrom, iTo);
        }
        return iValue;
    }

    public void reset() {
        iValue = Integer.MIN_VALUE;
    }

    @Override
    @Nullable
    public MFromTo clone() {
        try {
            return (MFromTo) super.clone();
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("MFromTo: clone failed: " + e.getMessage());
            return null;
        }
    }

    @Override
    @NonNull
    public String toString() {
        return (iFrom == iTo) ?
                String.valueOf(iFrom) : (iFrom + " to " + iTo);
    }
}
