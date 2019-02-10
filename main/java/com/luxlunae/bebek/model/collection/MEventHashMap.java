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
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;

import static com.luxlunae.bebek.VB.cint;

public class MEventHashMap extends MItemHashMap<MEvent> {
    @NonNull
    private final MAdventure mAdv;

    public MEventHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader,
                     final double v, final int nLocations,
                     final int iStartMaxPriority) throws EOFException {
        int nEvents = cint(reader.readLine());
        for (int i = 1; i <= nEvents; i++) {
            MEvent ev = new MEvent(mAdv, reader, i, iStartMaxPriority, nLocations, v);
            put(ev.getKey(), ev);
        }
    }

    @Override
    @Nullable
    public MEvent put(@NonNull String key, @NonNull MEvent value) {
        mAdv.mAllItems.put(value);
        return super.put(key, value);
    }

    @Override
    @Nullable
    public MEvent remove(Object key) {
        mAdv.mAllItems.remove(key);
        return super.remove(key);
    }
}
