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
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MGroup;
import com.luxlunae.bebek.model.MLocation;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MProperty;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;
import java.util.ArrayList;

import static com.luxlunae.bebek.MGlobals.HIDDEN;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.SelectionOnly;

public class MLocationHashMap extends MItemHashMap<MLocation> {
    @NonNull
    private final MAdventure mAdv;

    public MLocationHashMap(@NonNull MAdventure adv) {
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader, double version, int[][][] locLinks,
                     @NonNull ArrayList<MLocation> newLocs, int nLocs) throws EOFException {
        for (int i = 1; i <= nLocs; i++) {
            MLocation loc = new MLocation(mAdv, reader, i, version, locLinks, newLocs);
            put(loc.getKey(), loc);
        }
    }

    @NonNull
    public MLocationHashMap get(@NonNull MoveLocationWhatEnum what,
                                @NonNull String key, @NonNull String propValue) {
        MLocationHashMap ret = new MLocationHashMap(mAdv);
        switch (what) {
            case Location: {
                MLocation loc = mAdv.mLocations.get(key);
                if (loc != null) {
                    ret.put(key, loc);
                }
                break;
            }
            case LocationOf: {
                MCharacter ch = mAdv.mCharacters.get(key);
                if (ch != null) {
                    String locKey = ch.getLocation().getLocationKey();
                    if (!locKey.equals(HIDDEN)) {
                        MLocation loc = mAdv.mLocations.get(locKey);
                        if (loc != null) {
                            ret.put(key, loc);
                        }
                    }
                } else {
                    MObject ob = mAdv.mObjects.get(key);
                    if (ob != null) {
                        ret = ob.getRootLocations();
                    }
                }
                break;
            }
            case EverywhereInGroup: {
                MGroup grp = mAdv.mGroups.get(key);
                if (grp != null) {
                    for (String locKey : grp.getMembers()) {
                        MLocation loc = mAdv.mLocations.get(locKey);
                        if (loc != null) {
                            ret.put(locKey, loc);
                        }
                    }
                }
                break;
            }
            case EverywhereWithProperty: {
                MProperty prop = mAdv.mLocationProperties.get(key);
                if (prop != null) {
                    for (MLocation loc : mAdv.mLocations.values()) {
                        if (loc.hasProperty(key)) {
                            if (prop.getType() == SelectionOnly) {
                                ret.put(loc.getKey(), loc);
                            } else {
                                if (loc.getPropertyValue(key).equals(propValue)) {
                                    ret.put(loc.getKey(), loc);
                                }
                            }
                        }
                    }
                }
                break;
            }
        }
        return ret;
    }

    @Override
    @Nullable
    public MLocation put(@NonNull String key, @NonNull MLocation loc) {
        if (this == mAdv.mLocations) {
            mAdv.mAllItems.put(loc);
        }
        return super.put(key, loc);
    }

    @Override
    @Nullable
    public MLocation remove(Object key) {
        if (this == mAdv.mLocations) {
            mAdv.mAllItems.remove(key);
        }
        return super.remove(key);
    }

    @NonNull
    public String toList() {
        return toList("and");
    }

    @NonNull
    public String toList(@NonNull String separator) {
        StringBuilder ret = new StringBuilder();
        int n = size();
        for (MLocation loc : values()) {
            ret.append(loc.getShortDescription().toString(true));
            n--;
            if (n > 1) {
                ret.append(", ");
            }
            if (n == 1) {
                ret.append(" ").append(separator).append(" ");
            }
        }
        if (ret.length() == 0) {
            ret.append("nowhere");
        }
        return ret.toString();
    }

    public enum MoveLocationWhatEnum {
        Location,                   // 0
        LocationOf,                 // 1
        EverywhereInGroup,          // 2
        EverywhereWithProperty      // 3
    }
}
