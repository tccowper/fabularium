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

import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.CharacterKey;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.LocationGroupKey;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.LocationKey;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.ObjectKey;

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
     * @param key - property key whose mapping is to be removed from the map.
     *
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
            @SuppressWarnings("SuspiciousMethodCalls")
            MProperty prop = mAdv.mAllProperties.get(key);
            if (prop != null) {
                switch (prop.getPropertyOf()) {
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
        MPropertyHashMap ret = new MPropertyHashMap(mAdv);
        for (Entry<String, MProperty> entry : entrySet()) {
            MProperty prop = entry.getValue().clone();
            if (prop == null) {
                return null;
            }
            ret.put(prop);
        }
        return ret;
    }

    public int getNumberOfKeyRefs(@NonNull String key) {
        int ret = 0;
        for (MProperty prop : values()) {
            if (prop.getAppendToProperty().equals(key)) {
                ret++;
            }
            if (prop.getDependentKey().equals(key)) {
                ret++;
            }
            if (prop.getRestrictProperty().equals(key)) {
                ret++;
            }
            if ((prop.getType() == CharacterKey || prop.getType() == LocationGroupKey ||
                    prop.getType() == LocationKey || prop.getType() == ObjectKey)) {
                if (prop.getValue().equals(key)) {
                    ret++;
                }
            }
        }
        return ret;
    }

    public boolean deleteKey(@NonNull String key) {
        boolean restart = false;
        do {
            for (MProperty prop : values()) {
                if (prop.getAppendToProperty().equals(key)) {
                    prop.setAppendToProperty("");
                }
                if (prop.getDependentKey().equals(key)) {
                    prop.setDependentKey("");
                }
                if (prop.getRestrictProperty().equals(key)) {
                    prop.setRestrictProperty("");
                }
                if ((prop.getType() == CharacterKey || prop.getType() == LocationGroupKey ||
                        prop.getType() == LocationKey || prop.getType() == ObjectKey)) {
                    if (prop.getValue().equals(key)) {
                        if (resetOrRemoveProperty(prop)) {
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

    private boolean resetOrRemoveProperty(@NonNull MProperty prop) {
        // If we're not mandatory we can just remove the property
        if (!prop.getMandatory()) {
            // Ok, just remove it
            remove(prop.getKey());
            return true;
        }

        // If we are mandatory, we may be able to reset the value, depending
        // on the property type
        boolean deleteProp = false;
        switch (prop.getType()) {
            case Integer:
            case Text: {
                break;
            }
            case ObjectKey:
            case CharacterKey:
            case LocationKey:
            case LocationGroupKey: {
                // Key types can't be reset. They must be deleted, as they can't be
                // null on a mandatory property
                deleteProp = true;
                break;
            }
            case StateList: {
                // Ok, we just need to make sure we change the value to something
                // that doesn't produce child property
                boolean valueSet = false;
                for (String val : prop.mStates) {
                    boolean isSafeValue = true;
                    // If we're unable to do this, we need to remove, and
                    // reset/remove parent
                    for (MProperty prop2 : values()) {
                        if (prop2.getDependentKey().equals(prop.getKey()) &&
                                prop2.getDependentValue().equals(val)) {
                            isSafeValue = false;
                            break;
                        }
                    }
                    if (isSafeValue) {
                        // Ok to set the value to this
                        prop.setValue(val);
                        valueSet = true;
                        break;
                    }
                }
                if (!valueSet) {
                    deleteProp = true;
                }
                break;
            }
        }

        // If we can't reset the property, we need to reset or remove our parent
        if (deleteProp) {
            MProperty depProp = get(prop.getDependentKey());
            if (depProp != null) {
                resetOrRemoveProperty(depProp);
            }
            remove(prop.getKey());
            return true;
        }

        return false;
    }

    private boolean isPropertySelected(@NonNull MProperty prop) {
        String depKey = prop.getDependentKey();
        if (!depKey.equals("")) {
            MProperty depProp = get(depKey);
            String depVal = prop.getDependentValue();
            if (depProp != null && (depVal.equals("") || depProp.getValue().equals(depVal))) {
                return isPropertySelected(depProp);
            }
        } else {
            return prop.getSelected();
        }
        return false;
    }
}
