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

import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.OnObject;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.SelectionOnly;

public class MCharacterHashMap extends MItemHashMap<MCharacter> {
    @NonNull
    private final MAdventure mAdv;

    public MCharacterHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader, double version) throws EOFException {
        int nChars = cint(reader.readLine());
        for (int i = 1; i <= nChars; i++) {
            MCharacter ch = new MCharacter(mAdv, reader, i, version);
            put(ch.getKey(), ch);
        }
    }

    @Override
    @Nullable
    public MCharacter get(@NonNull Object key) {
        if (key.equals("%Player%")) {
            MCharacter player = mAdv.getPlayer();
            if (player == null) {
                return null;
            }
            key = player.getKey();
        }
        return super.get(key);
    }

    @NonNull
    public MCharacterHashMap getSeenBy(@NonNull String chKey) {
        MCharacterHashMap ret = new MCharacterHashMap(mAdv);
        if (chKey.equals("") || chKey.equals("%Player%")) {
            chKey = mAdv.getPlayer().getKey();
        }
        for (MCharacter ch : values()) {
            if (ch.hasBeenSeenBy(chKey)) {
                ret.put(ch.getKey(), ch);
            }
        }
        return ret;
    }

    @NonNull
    public MCharacterHashMap getVisibleTo(@NonNull String chKey) {
        MCharacterHashMap ret = new MCharacterHashMap(mAdv);
        if (chKey.equals("") || chKey.equals("%Player%")) {
            chKey = mAdv.getPlayer().getKey();
        }
        for (MCharacter ch : values()) {
            if (ch.canSeeCharacter(chKey)) {
                ret.put(ch.getKey(), ch);
            }
        }
        return ret;
    }

    @NonNull
    public MCharacterHashMap get(@NonNull MoveCharacterWhoEnum who,
                                 @NonNull String key, @NonNull String propValue) {
        MCharacterHashMap ret = new MCharacterHashMap(mAdv);
        switch (who) {
            case Character: {
                MCharacter ch = mAdv.mCharacters.get(key);
                if (ch != null) {
                    ret.put(key, ch);
                }
                break;
            }
            case EveryoneAtLocation: {
                MLocation loc = mAdv.mLocations.get(key);
                if (loc != null) {
                    ret = loc.getCharactersDirectlyInLocation(true);
                }
                break;
            }
            case EveryoneInGroup: {
                MGroup grp = mAdv.mGroups.get(key);
                if (grp != null) {
                    for (String chKey : grp.getArlMembers()) {
                        MCharacter ch = mAdv.mCharacters.get(chKey);
                        if (ch != null) {
                            ret.put(chKey, ch);
                        }
                    }
                }
                break;
            }
            case EveryoneInside: {
                MObject ob = mAdv.mObjects.get(key);
                if (ob != null) {
                    ret = ob.getChildChars(InsideObject);
                }
                break;
            }
            case EveryoneOn: {
                MObject ob = mAdv.mObjects.get(key);
                if (ob != null) {
                    ret = ob.getChildChars(OnObject);
                }
                break;
            }
            case EveryoneWithProperty: {
                MProperty prop = mAdv.mCharacterProperties.get(key);
                if (prop != null) {
                    for (MCharacter ch : mAdv.mCharacters.values()) {
                        if (ch.hasProperty(key)) {
                            if (prop.getType() == SelectionOnly) {
                                ret.put(ch.getKey(), ch);
                            } else {
                                if (ch.getPropertyValue(key).equals(propValue)) {
                                    ret.put(ch.getKey(), ch);
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

    @Nullable
    public MCharacter put(@NonNull String key, @NonNull MCharacter value) {
        if (this == mAdv.mCharacters) {
            mAdv.mAllItems.put(value);
        }
        return super.put(key, value);
    }

    @Override
    @Nullable
    public MCharacter remove(Object key) {
        if (this == mAdv.mCharacters) {
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
        for (MCharacter ch : values()) {
            ret.append("%CharacterName[").append(ch.getKey()).append("]%");
            n--;
            if (n > 1) {
                ret.append(", ");
            }
            if (n == 1) {
                ret.append(" ").append(separator).append(" ");
            }
        }
        if (ret.length() == 0) {
            ret.append("noone");
        }
        return ret.toString();
    }

    public enum MoveCharacterWhoEnum {
        Character,                  // 0
        EveryoneInside,             // 1
        EveryoneOn,                 // 2
        EveryoneWithProperty,       // 3
        EveryoneInGroup,            // 4
        EveryoneAtLocation          // 5
    }
}
