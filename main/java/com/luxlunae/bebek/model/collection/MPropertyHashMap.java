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
import com.luxlunae.bebek.model.MProperty;

import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum;

public class MPropertyHashMap extends MItemHashMap<MProperty> implements Cloneable {
    @NonNull
    private final MAdventure mAdv;

    public MPropertyHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    @Override
    @Nullable
    public MProperty put(@NonNull MProperty value) {
        if (this == mAdv.mAllProperties) {
            switch (value.getPropertyOf()) {
                case Locations:
                    mAdv.mLocationProperties.put(value);
                    break;
                case Objects:
                    mAdv.mObjectProperties.put(value);
                    break;
                case Characters:
                    mAdv.mCharacterProperties.put(value);
                    break;
                case AnyItem:
                    mAdv.mObjectProperties.put(value);
                    mAdv.mLocationProperties.put(value);
                    mAdv.mCharacterProperties.put(value);
                    break;
            }
            mAdv.mAllItems.put(value);
        }
        return super.put(value.getKey(), value);
    }

    /**
     * Removes the mapping for a key from this map if it is present.
     *
     * @param key - property key whose mapping is to be
     *            removed from the map.
     * @return the previous value associated with key,
     * or null if there was no mapping for key.
     */
    @Override
    @Nullable
    public MProperty remove(Object key) {
        if (this == mAdv.mAllProperties) {
            // Also remove the property from
            // specific cached property hash maps
            // associated with the adventure object.
            switch (mAdv.mAllProperties.get(key).getPropertyOf()) {
                case Locations:
                    mAdv.mLocationProperties.remove(key);
                    break;
                case Objects:
                    mAdv.mObjectProperties.remove(key);
                    break;
                case Characters:
                    mAdv.mCharacterProperties.remove(key);
                    break;
                case AnyItem:
                    mAdv.mObjectProperties.remove(key);
                    mAdv.mCharacterProperties.remove(key);
                    mAdv.mLocationProperties.remove(key);
                    break;
            }
            mAdv.mAllItems.remove(key);
        }
        return super.remove(key);
    }

    @NonNull
    @Override
    public MPropertyHashMap clone() {
        // This probably isn't ideal, but you can't cast an ArrayList to a StringArrayList,
        // so we need to do the shallow copy ourselves.
        MPropertyHashMap htblReturn = new MPropertyHashMap(mAdv);
        for (Entry<String, MProperty> entry : entrySet()) {
            MProperty prop = entry.getValue().clone();
            if (prop == null) {
                return null;
            }
            htblReturn.put(prop);
        }
        return htblReturn;
    }

    public int getNumberOfKeyRefs(@NonNull String sKey) {
        int iCount = 0;
        for (MProperty p : values()) {
            if (p.getAppendToProperty().equals(sKey)) {
                iCount++;
            }
            if (p.getDependentKey().equals(sKey)) {
                iCount++;
            }
            if (p.getRestrictProperty().equals(sKey)) {
                iCount++;
            }
            if ((p.getType() == PropertyTypeEnum.CharacterKey ||
                    p.getType() == PropertyTypeEnum.LocationGroupKey ||
                    p.getType() == PropertyTypeEnum.LocationKey ||
                    p.getType() == PropertyTypeEnum.ObjectKey)) {
                if (p.getValue().equals(sKey)) {
                    iCount++;
                }
            }
        }
        return iCount;
    }

    private boolean resetOrRemoveProperty(@NonNull MProperty p) {
        // If we're not mandatory we can just remove the property
        if (!p.getMandatory()) {
            // Ok, just remove it
            remove(p.getKey());
            return true;
        }

        // If we are mandatory, we may be able to reset the value, depending on the property type
        boolean bDelete = false;
        switch (p.getType()) {
            case Integer:
                break;
            case Text:
                break;
            case ObjectKey:
            case CharacterKey:
            case LocationKey:
            case LocationGroupKey:
                // Key types can't be reset, must be deleted, as they can't be Null on a mandatory property
                bDelete = true;
                break;
            case StateList: {
                // Ok, we just need to make sure we change the value to something that doesn't produce child property
                boolean bAssignedValue = false;
                for (String sValue : p.mStates) {
                    boolean bSafeValue = true;
                    // If we're unable to do this, we need to remove, and reset/remove parent
                    for (MProperty pOther : values()) {
                        if (pOther.getDependentKey().equals(p.getKey()) &&
                                pOther.getDependentValue().equals(sValue)) {
                            bSafeValue = false;
                            break;
                        }
                    }
                    if (bSafeValue) {
                        // Ok to set the value to this
                        p.setValue(sValue);
                        bAssignedValue = true;
                        break;
                    }
                }
                if (!bAssignedValue) {
                    bDelete = true;
                }
                break;
            }
        }

        // If we can't reset the property, we need to reset or remove our parent
        if (bDelete) {
            if (containsKey(p.getDependentKey())) {
                MProperty p2 = get(p.getDependentKey());
                resetOrRemoveProperty(p2);
            }
            remove(p.getKey());
            return true;
        }

        return false;
    }

    public boolean deleteKey(@NonNull String sKey) {
        boolean restart = false;

        do {
            for (MProperty p : values()) {
                if (p.getAppendToProperty().equals(sKey)) {
                    p.setAppendToProperty("");
                }
                if (p.getDependentKey().equals(sKey)) {
                    p.setDependentKey("");
                }
                if (p.getRestrictProperty().equals(sKey)) {
                    p.setRestrictProperty("");
                }
                if ((p.getType() == PropertyTypeEnum.CharacterKey ||
                        p.getType() == PropertyTypeEnum.LocationGroupKey ||
                        p.getType() == PropertyTypeEnum.LocationKey ||
                        p.getType() == PropertyTypeEnum.ObjectKey)) {
                    if (p.getValue().equals(sKey)) {
                        if (resetOrRemoveProperty(p)) {
                            restart = true;
                            break;
                        }
                    }
                }
            }
        } while (restart);

        return true;
    }

    public void setSelected() {
        for (MProperty prop : values()) {
            prop.setSelected(isPropertySelected(prop));
        }
    }

    private boolean isPropertySelected(@NonNull MProperty prop) {
        if (!prop.getDependentKey().equals("")) {
            if (prop.getDependentValue().equals("") || (containsKey(prop.getDependentKey()) &&
                    get(prop.getDependentKey()).getValue().equals(prop.getDependentValue()))) {
                return isPropertySelected(get(prop.getDependentKey()));
            }
        } else {
            return prop.getSelected();
        }
        return false;
    }
}
