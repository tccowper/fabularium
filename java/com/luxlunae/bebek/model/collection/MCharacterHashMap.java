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

    public static MCharacterHashMap getCharactersToMove(@NonNull MAdventure adv,
                                                        @NonNull MoveCharacterWhoEnum who,
                                                        @NonNull String itemKey,
                                                        @NonNull String propValue) {
        MCharacterHashMap ret = new MCharacterHashMap(adv);
        switch (who) {
            case Character: {
                MCharacter ch = adv.mCharacters.get(itemKey);
                if (ch != null) {
                    ret.put(itemKey, ch);
                }
                break;
            }
            case EveryoneAtLocation:
                ret = adv.mLocations.get(itemKey).getCharactersDirectlyInLocation(true);
                break;
            case EveryoneInGroup:
                for (String chKey : adv.mGroups.get(itemKey).getArlMembers()) {
                    MCharacter ch = adv.mCharacters.get(chKey);
                    if (ch != null) {
                        ret.put(chKey, ch);
                    }
                }
                break;
            case EveryoneInside:
                ret = adv.mObjects.get(itemKey).getChildChars(InsideObject);
                break;
            case EveryoneOn:
                ret = adv.mObjects.get(itemKey).getChildChars(OnObject);
                break;
            case EveryoneWithProperty:
                MProperty prop = adv.mCharacterProperties.get(itemKey);
                for (MCharacter ch : adv.mCharacters.values()) {
                    if (ch.hasProperty(prop.getKey())) {
                        if (prop.getType() == SelectionOnly) {
                            ret.put(ch.getKey(), ch);
                        } else {
                            if (ch.getPropertyValue(prop.getKey()).equals(propValue)) {
                                ret.put(ch.getKey(), ch);
                            }
                        }
                    }
                }
                break;
        }
        return ret;
    }

    public void load(@NonNull MFileOlder.V4Reader reader,
                     double v) throws EOFException {
        int nChars = cint(reader.readLine());
        for (int i = 1; i <= nChars; i++) {
            MCharacter ch = new MCharacter(mAdv, reader, i, v);
            put(ch.getKey(), ch);
        }
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

    @Override
    @Nullable
    public MCharacter get(@NonNull Object key) {
        if (key.equals("%Player%")) {
            if (mAdv.getPlayer() == null) {
                return null;
            }
            key = mAdv.getPlayer().getKey();
        }
        return super.get(key);
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

    public enum MoveCharacterWhoEnum {
        Character,                  // 0
        EveryoneInside,             // 1
        EveryoneOn,                 // 2
        EveryoneWithProperty,       // 3
        EveryoneInGroup,            // 4
        EveryoneAtLocation          // 5
    }
}
