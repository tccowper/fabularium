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

package com.luxlunae.bebek.model.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MGroup;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;

import static com.luxlunae.bebek.VB.cint;

public class MGroupHashMap extends MItemHashMap<MGroup> {
    @NonNull
    private final MAdventure mAdv;

    public MGroupHashMap(@NonNull MAdventure adv) {
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader, int nLocations,
                     @NonNull final String[] locNames) throws EOFException {
        int nGroups = cint(reader.readLine());
        for (int i = 1; i <= nGroups; i++) {
            MGroup grp = new MGroup(mAdv, reader, i, nLocations, locNames);
            put(grp.getKey(), grp);
        }
    }

    @Override
    @Nullable
    public MGroup put(@NonNull String key, @NonNull MGroup value) {
        mAdv.mAllItems.put(value);
        return super.put(key, value);
    }

    @Override
    @Nullable
    public MGroup remove(Object key) {
        mAdv.mAllItems.remove(key);
        return super.remove(key);
    }
}
