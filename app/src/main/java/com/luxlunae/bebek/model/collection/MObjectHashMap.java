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
import com.luxlunae.bebek.model.MGroup;
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

    public void load(@NonNull MFileOlder.V4Reader reader,
                     final double v, ArrayList<MObject> newObs,
                     final int nLocs, final int startLocs,
                     MStringArrayList withStates, HashMap<String, String> dictDodgyStates,
                     HashMap<MObject, MProperty> dodgyArlStates) throws EOFException {
        int nObs = cint(reader.readLine());
        for (int i = 1; i <= nObs; i++) {
            MObject obj = new MObject(mAdv, reader, i, v, nLocs, startLocs,
                    withStates, dictDodgyStates, dodgyArlStates, newObs);
            put(obj.getKey(), obj);
        }
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

    @NonNull
    public MObjectHashMap get(@NonNull MAction.MoveObjectWhatEnum what,
                              @NonNull String key, @NonNull String propValue) {
        MObjectHashMap ret = new MObjectHashMap(mAdv);
        switch (what) {
            case Object: {
                MObject ob = mAdv.mObjects.get(key);
                if (ob != null) {
                    ret.put(key, ob);
                }
                break;
            }
            case EverythingAtLocation: {
                for (MObject ob : mAdv.mObjects.values()) {
                    MObject.MObjectLocation obLoc = ob.getLocation();
                    if (!ob.isStatic() &&
                            obLoc.mDynamicExistWhere == InLocation &&
                            obLoc.getKey().equals(key)) {
                        ret.put(ob.getKey(), ob);
                    }
                }
                break;
            }
            case EverythingHeldBy: {
                MCharacter ch = mAdv.mCharacters.get(key);
                if (ch != null) {
                    ret = ch.getHeldObjects();
                }
                break;
            }
            case EverythingInGroup: {
                MGroup grp = mAdv.mGroups.get(key);
                if (grp != null) {
                    for (String obKey : grp.getArlMembers()) {
                        MObject ob = mAdv.mObjects.get(obKey);
                        if (ob != null) {
                            ret.put(obKey, ob);
                        }
                    }
                }
                break;
            }
            case EverythingInside: {
                MObject ob = mAdv.mObjects.get(key);
                if (ob != null) {
                    ret = ob.getChildObjects(InsideObject);
                }
                break;
            }
            case EverythingOn: {
                MObject ob = mAdv.mObjects.get(key);
                if (ob != null) {
                    ret = ob.getChildObjects(OnObject);
                }
                break;
            }
            case EverythingWithProperty: {
                MProperty prop = mAdv.mObjectProperties.get(key);
                if (prop != null) {
                    MProperty.PropertyTypeEnum type = prop.getType();
                    for (MObject ob : mAdv.mObjects.values()) {
                        if (ob.hasProperty(key)) {
                            if (type == SelectionOnly) {
                                ret.put(ob.getKey(), ob);
                            } else {
                                if (ob.getPropertyValue(key).equals(propValue)) {
                                    ret.put(ob.getKey(), ob);
                                }
                            }
                        }
                    }
                }
                break;
            }
            case EverythingWornBy: {
                MCharacter ch = mAdv.mCharacters.get(key);
                if (ch != null) {
                    ret = ch.getWornObjects();
                }
                break;
            }
        }
        return ret;
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
    public String toList(@NonNull String separator, boolean includeSubObjects) {
        return toList(separator, includeSubObjects, Definite);
    }

    @NonNull
    public String toList(@NonNull String separator, boolean includeSubObjects,
                         MGlobals.ArticleTypeEnum article) {
        StringBuilder ret = new StringBuilder();
        int n = size();
        for (MObject ob : values()) {
            ret.append(ob.getFullName(article));
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

        if (includeSubObjects) {
            for (MObject ob : values()) {
                MObjectHashMap obsOn = ob.getChildObjects(OnObject);
                if (obsOn.size() > 0) {
                    if (ret.length() > 0) {
                        ret.append(".  ");
                    }
                    StringBuilder msg = new StringBuilder(obsOn.toList("and",
                            false, article));
                    toProper(msg);
                    ret.append(msg);
                    ret.append(obsOn.size() == 1 ? " is on " : " are on ");
                    ret.append(ob.getFullName(Definite));
                }
                if (!ob.isOpenable() || ob.isOpen()) {
                    MObjectHashMap obsIn = ob.getChildObjects(InsideObject);
                    if (obsIn.size() > 0) {
                        if (obsOn.size() > 0) {
                            ret.append(", and inside");
                        } else {
                            if (ret.length() > 0) {
                                ret.append(".  ");
                            }
                            ret.append("Inside ").append(ob.getFullName(Definite));
                        }
                        ret.append(obsIn.size() == 1 ? " is " : " are ");
                        ret.append(obsIn.toList("and", false, article));
                    }
                }
            }
        }
        return ret.toString();
    }
}
