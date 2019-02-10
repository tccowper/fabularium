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
import com.luxlunae.bebek.model.MAction;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MProperty;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Definite;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.InLocation;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.OnObject;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.SelectionOnly;

public class MObjectHashMap extends MItemHashMap<MObject> {
    @NonNull
    private final MAdventure mAdv;

    public MObjectHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public static MObjectHashMap getObjectsToMove(@NonNull MAdventure adv,
                                                  @NonNull MAction.MoveObjectWhatEnum what,
                                                  @NonNull String key,
                                                  @NonNull String propValue) {
        MObjectHashMap ret = new MObjectHashMap(adv);
        switch (what) {
            case Object:
                ret.put(key, adv.mObjects.get(key));
                break;
            case EverythingAtLocation:
                for (MObject ob : adv.mObjects.values()) {
                    MObject.MObjectLocation obLoc = ob.getLocation();
                    if (!ob.isStatic() &&
                            obLoc.mDynamicExistWhere == InLocation &&
                            obLoc.getKey().equals(key)) {
                        ret.put(ob.getKey(), ob);
                    }
                }
                break;
            case EverythingHeldBy: {
                MCharacter ch = adv.mCharacters.get(key);
                if (ch != null) {
                    ret = ch.getHeldObjects();
                }
                break;
            }
            case EverythingInGroup:
                for (String obKey : adv.mGroups.get(key).getArlMembers()) {
                    ret.put(obKey, adv.mObjects.get(obKey));
                }
                break;
            case EverythingInside:
                ret = adv.mObjects.get(key).getChildObjects(InsideObject);
                break;
            case EverythingOn:
                ret = adv.mObjects.get(key).getChildObjects(OnObject);
                break;
            case EverythingWithProperty:
                MProperty prop = adv.mObjectProperties.get(key);
                String pKey = prop.getKey();
                MProperty.PropertyTypeEnum pType = prop.getType();
                for (MObject ob : adv.mObjects.values()) {
                    String obKey = ob.getKey();
                    if (ob.hasProperty(pKey)) {
                        if (pType == SelectionOnly) {
                            ret.put(obKey, ob);
                        } else {
                            if (ob.getPropertyValue(pKey).equals(propValue)) {
                                ret.put(obKey, ob);
                            }
                        }
                    }
                }
                break;
            case EverythingWornBy: {
                MCharacter ch = adv.mCharacters.get(key);
                if (ch != null) {
                    ret = ch.getWornObjects();
                }
                break;
            }
        }
        return ret;
    }

    public void load(@NonNull MFileOlder.V4Reader reader,
                     final double v, ArrayList<MObject> newObs,
                     final int iNumLocations, final int iStartLocations,
                     MStringArrayList salWithStates,
                     HashMap<String, String> dictDodgyStates, HashMap<MObject, MProperty> dodgyArlStates) throws EOFException {
        int nObs = cint(reader.readLine());
        for (int i = 1; i <= nObs; i++) {
            MObject obj = new MObject(mAdv, reader, i, v, iNumLocations, iStartLocations,
                    salWithStates, dictDodgyStates, dodgyArlStates, newObs);
            put(obj.getKey(), obj);
        }
    }

    @Override
    @Nullable
    public MObject put(@NonNull String key, @NonNull MObject ob) {
        if (this == mAdv.mObjects) {
            mAdv.mAllItems.put(ob);
        }
        return super.put(key, ob);
    }

    @Override
    @Nullable
    public MObject remove(Object key) {
        if (this == mAdv.mObjects) {
            mAdv.mAllItems.remove(key);
        }
        return super.remove(key);
    }

    @NonNull
    public String toList() {
        return toList("and", false, Definite);
    }

    @NonNull
    public String toList(@NonNull String separator) {
        return toList(separator, false, Definite);
    }

    @NonNull
    public String toList(@NonNull String separator, boolean bIncludeSubObjects) {
        return toList(separator, bIncludeSubObjects, Definite);
    }

    @NonNull
    public String toList(@NonNull String separator, boolean bIncludeSubObjects, MGlobals.ArticleTypeEnum Article) {
        StringBuilder ret = new StringBuilder();
        int n = size();
        for (MObject ob : values()) {
            ret.append(ob.getFullName(Article));
            n--;
            if (n > 1) {
                ret.append(", ");
            }
            if (n == 1) {
                ret.append(" ").append(separator).append(" ");
            }
        }
        if (ret.length() == 0) {
            ret.append("nothing");
        }

        if (bIncludeSubObjects) {
            for (MObject ob : values()) {
                if (ob.getChildObjects(OnObject).size() > 0) {
                    if (ret.length() > 0) {
                        ret.append(".  ");
                    }
                    StringBuilder msg = new StringBuilder(ob.getChildObjects(OnObject)
                            .toList("and", false, Article));
                    toProper(msg);
                    ret.append(msg);
                    if (ob.getChildObjects(OnObject).size() == 1) {
                        ret.append(" is on ");
                    } else {
                        ret.append(" are on ");
                    }
                    ret.append(ob.getFullName(Definite));
                }
                if (!ob.isOpenable() || ob.isOpen()) {
                    if (ob.getChildObjects(InsideObject).size() > 0) {
                        if (ob.getChildObjects(OnObject).size() > 0) {
                            ret.append(", and inside");
                        } else {
                            if (ret.length() > 0) {
                                ret.append(".  ");
                            }
                            ret.append("Inside ").append(ob.getFullName(Definite));
                        }
                        if (ob.getChildObjects(InsideObject).size() == 1) {
                            ret.append(" is ");
                        } else {
                            ret.append(" are ");
                        }
                        ret.append(ob.getChildObjects(InsideObject).toList("and", false, Article));
                    }
                }
            }
        }
        return ret.toString();
    }

    @NonNull
    public MObjectHashMap getSeenBy() {
        return getSeenBy("");
    }

    @NonNull
    public MObjectHashMap getSeenBy(@NonNull String chKey) {
        MObjectHashMap ret = new MObjectHashMap(mAdv);
        if (chKey.equals("") || chKey.equals("%Player%")) {
            chKey = mAdv.getPlayer().getKey();
        }
        for (MObject ob : values()) {
            if (ob.hasBeenSeenBy(chKey)) {
                ret.put(ob.getKey(), ob);
            }
        }
        return ret;
    }

    @NonNull
    public MObjectHashMap getVisibleTo(@NonNull String chKey) {
        MObjectHashMap ret = new MObjectHashMap(mAdv);
        if (chKey.equals("") || chKey.equals("%Player%")) {
            chKey = mAdv.getPlayer().getKey();
        }
        for (MObject ob : values()) {
            if (ob.isVisibleTo(chKey)) {
                ret.put(ob.getKey(), ob);
            }
        }
        return ret;
    }
}
