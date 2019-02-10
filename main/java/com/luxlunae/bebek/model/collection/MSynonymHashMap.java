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
import com.luxlunae.bebek.model.MSynonym;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;

import static com.luxlunae.bebek.VB.cint;

public class MSynonymHashMap extends MItemHashMap<MSynonym> {
    @NonNull
    private final MAdventure mAdv;

    public MSynonymHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader) throws EOFException {
        int nSyn = cint(reader.readLine());
        for (int i = 1; i <= nSyn; i++) {
            String sTo = reader.readLine();     // System Command
            String sFrom = reader.readLine();   // Alternative Command
            MSynonym synNew = null;
            for (MSynonym syn : values()) {
                if (syn.getChangeTo().equals(sTo)) {
                    synNew = syn;
                    break;
                }
            }
            if (synNew == null) {
                synNew = new MSynonym(mAdv);
                synNew.setKey("Synonym" + i);
            }
            synNew.setChangeTo(sTo);
            synNew.getChangeFrom().add(sFrom);
            if (!containsKey(synNew.getKey())) {
                put(synNew);
            }
        }
    }

    @Override
    @Nullable
    public MSynonym put(@NonNull MSynonym value) {
        mAdv.mAllItems.put(value);
        return super.put(value.getKey(), value);
    }

    @Override
    @Nullable
    public MSynonym remove(Object key) {
        mAdv.mAllItems.remove(key);
        return super.remove(key);
    }
}
