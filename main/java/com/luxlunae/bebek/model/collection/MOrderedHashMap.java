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

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.controller.MReference;
import com.luxlunae.bebek.model.MAdventure;

import java.util.LinkedHashMap;

public class MOrderedHashMap<K extends String, V extends MReferenceList> extends LinkedHashMap<K, V> {
    private static final String[] VALID_TAGS = {
            "<br>", "<center>", "<centre>", "<i>", "</i>", "<b>", "</b>",
            "<u>", "</u>", "<c>", "</c>", "<font", "</font>", "<right>", "</right>",
            "<left>", "</left>", "<del>", "<wait", "<cls>", "<img ", "<audio "
    };
    @NonNull
    public MStringArrayList mOrderedKeys = new MStringArrayList();

    private static boolean containsOutput(@NonNull MAdventure adv, @NonNull String text) {
        // Determines whether tags in a message correspond to actual output
        if (text.equals("")) {
            return false;
        } else {
            if (MGlobals.stripCarats(text).equals("")) {
                String textLower = text.toLowerCase();
                for (String sValid : VALID_TAGS) {
                    if (textLower.contains(sValid)) {
                        return true;
                    }
                }
                // Ok, so we have an unknown tag.  But the user may be text
                // replacing it, so check for that
                return adv.mALRs.containsKey(text);
            } else {
                return true;
            }
        }
    }

    public boolean addResponse(@NonNull MAdventure adv, @NonNull boolean[] shouldOutputResponses,
                               @NonNull K response, @NonNull String[] newRefItems,
                               final @Nullable V refs) {
        return addResponse(adv, shouldOutputResponses, response, newRefItems, -1, refs);
    }

    public boolean addResponse(@NonNull MAdventure adv, @NonNull boolean[] shouldOutputResponses,
                               @NonNull K response, @NonNull String[] newRefItems,
                               int pos, final @Nullable V refs) {
        // Returns True if a response was added, or False otherwise
        if (shouldOutputResponses[0] || !containsOutput(adv, response)) {
            return false;
        }

        if (containsKey(response)) {
            // Add our new references to the ones already there
            MReferenceList curRefs = get(response);
            for (int i = 0; i < newRefItems.length; i++) {
                if (i < curRefs.size()) {
                    MReference curRef = curRefs.get(i);
                    if (curRef != null && !curRef.containsKey(newRefItems[i])) {
                        curRef.mItems.add(new MReference.MReferenceItem(newRefItems[i]));
                    }
                }
            }
        } else {
            // Store our references
            if (pos > -1) {
                put(response, refs, pos);
            } else {
                put(response, refs);
            }
        }

        shouldOutputResponses[0] = true;
        return true;
    }

    @Override
    @Nullable
    public V put(@NonNull K key, @Nullable V value) {
        mOrderedKeys.add(key);
        return super.put(key, value);
    }

    @Nullable
    public Object put(@NonNull K key, @Nullable V value, int iPosition) {
        mOrderedKeys.add(iPosition, key);
        return super.put(key, value);
    }

    @Override
    @Nullable
    public V remove(Object key) {
        //noinspection SuspiciousMethodCalls
        mOrderedKeys.remove(key);
        return super.remove(key);
    }

    @Override
    public void clear() {
        mOrderedKeys.clear();
        super.clear();
    }

    @NonNull
    public MOrderedHashMap<String, V> Clone() {
        MOrderedHashMap<String, V> htbl = new MOrderedHashMap<>();
        for (String sKey : mOrderedKeys) {
            htbl.put(sKey, get(sKey));
        }
        return htbl;
    }
}
