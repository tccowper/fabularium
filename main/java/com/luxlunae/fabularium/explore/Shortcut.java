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

package com.luxlunae.fabularium.explore;

import android.support.annotation.NonNull;

import java.io.File;

class Shortcut extends File {
    @NonNull
    private String mDisplayText;

    Shortcut(@NonNull String pathname) {
        super(pathname);
        mDisplayText = getName();
    }

    @NonNull
    String getDisplayText() {
        return mDisplayText;
    }

    void setDisplayText(@NonNull String txt) {
        mDisplayText = txt;
    }
}
