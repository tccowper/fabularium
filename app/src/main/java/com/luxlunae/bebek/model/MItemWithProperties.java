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

package com.luxlunae.bebek.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.model.collection.MPropertyHashMap;

public abstract class MItemWithProperties extends MItem {
    /**
     * Properties that belong to the item.
     */
    @NonNull
    private MPropertyHashMap mLocalProperties;
    /**
     * Properties inherited from the group or class
     * (dynamically generated).
     */
    @Nullable
    private MPropertyHashMap mInheritedProperties;
    /**
     * All properties - including those that belong
     * to the item and those inherited (dynamically
     * generated).
     */
    @Nullable
    private MPropertyHashMap mAllProperties;

    MItemWithProperties(@NonNull MAdventure adv) {
        super(adv);
        mLocalProperties = new MPropertyHashMap(adv);
    }

    @NonNull
    public MPropertyHashMap getProperties() {
        if (mAllProperties == null) {
            // Need a shallow clone of actual properties
            // (so we have unique list of original properties)
            mAllProperties = new MPropertyHashMap(mAdv);
            for (MProperty p : mLocalProperties.values()) {
                mAllProperties.put(p);
            }

            // Take all local properties, then layer on
            // the inherited ones
            for (MProperty pInherited : getInheritedProperties().values()) {
                MProperty p = getProperty(pInherited.getKey());
                if (p == null) {
                    mAllProperties.put(pInherited.copy());
                } else {
                    // We have a property in both local and
                    // inherited. Overwrite the existing
                    // local property with the values of the
                    // inherited property.
                    p.setValue(pInherited.getValue());
                    p.setStringData(pInherited.getStringData().copy());
                    p.setFromGroup(true);
                }
            }
        }
        return mAllProperties;
    }

    @NonNull
    public MPropertyHashMap getLocalProperties() {
        return mLocalProperties;
    }

    void setLocalProperties(@NonNull MPropertyHashMap value) {
        mLocalProperties = value;

        // Force a rebuild of the properties cache on the
        // next call to getProperties().
        mAllProperties = null;
    }

    @NonNull
    private MPropertyHashMap getInheritedProperties() {
        if (mInheritedProperties == null) {
            // (Re-)build the inherited properties cache.
            mInheritedProperties = new MPropertyHashMap(mAdv);

            // Search through all of the groups, find groups
            // that have the same type as this item, then
            // add any properties of those groups that haven't
            // already been added.
            MGroup.GroupTypeEnum grpType = getPropertyGroupType();
            String myKey = getKey();
            for (MGroup g : mAdv.mGroups.values()) {
                if (g.getGroupType() == grpType) {
                    if (g.getMembers().contains(myKey)) {
                        for (MProperty pGroup : g.mProperties.values()) {
                            MProperty pInherited = mInheritedProperties.get(pGroup.getKey());
                            if (pInherited == null) {
                                pInherited = pGroup.copy();
                                mInheritedProperties.put(pInherited);
                            }
                            pInherited.setValue(pGroup.getValue());
                            pInherited.setStringData(pGroup.getStringData().copy());
                            pInherited.setFromGroup(true);
                        }
                    }
                }
            }
        }
        return mInheritedProperties;
    }

    protected abstract MGroup.GroupTypeEnum getPropertyGroupType();

    @NonNull
    public abstract String getParent();

    /**
     * Force rebuilds of the inherited properties cache
     * and the all properties cache for this object.
     * <p>
     * The caches are rebuilt upon the next call to
     * getProperties().
     * <p>
     * This is required if we do an action that changes the
     * properties of the object, such as add/remove from
     * a group.
     */
    public void resetInherited() {
        mInheritedProperties = null;
        mAllProperties = null;
    }

    /**
     * Get a property of this item.
     *
     * @param propKey - the key of the property to get.
     * @return the property, if it exists, otherwise NULL.
     */
    @Nullable
    public MProperty getProperty(@NonNull String propKey) {
        return getProperties().get(propKey);
    }

    @NonNull
    public String getPropertyValue(@NonNull String propKey) {
        MProperty prop = getProperty(propKey);
        return prop != null ? prop.getValue() : "";
    }

    void setPropertyValue(@NonNull String propKey,
                          @NonNull MDescription value) {
        addProperty(propKey).setStringData(value);
    }

    /**
     * Add a string property to this item
     * and set its value.
     *
     * @param propKey - the key of the
     *                string property.
     * @param value   - the value to set
     *                the property to.
     */
    void setPropertyValue(@NonNull String propKey,
                          @NonNull String value) {
        addProperty(propKey).setValue(value);
    }

    /**
     * Set the value of a boolean property
     * associated with this item.
     *
     * @param propKey - the key of the property.
     * @param value   - if TRUE, the property is
     *                added to this item and then
     *                set as selected. If FALSE, the
     *                property is removed from this item.
     */
    void setPropertyValue(@NonNull String propKey,
                          boolean value) {
        if (value) {
            addProperty(propKey).setSelected(true);
        } else {
            removeProperty(propKey);
        }
    }

    /**
     * Does this item currently have a specific
     * property (either local or inherited)?
     *
     * @param propKey - the key of the
     *                property to check.
     *
     * @return TRUE if the item has the property has
     * the property, FALSE otherwise.
     */
    public boolean hasProperty(@NonNull String propKey) {
        return getProperties().containsKey(propKey);
    }

    /**
     * Add a local property to this item.
     *
     * If this item already has a local or
     * inherited property with the same key,
     * that existing property is removed first.
     *
     * NOTE: we add the referenced property,
     * not a copy of it.
     *
     * @param prop - the property to add.
     */
    public void addProperty(@NonNull MProperty prop) {
        removeProperty(prop.getKey());
        mLocalProperties.put(prop);

        // Force a rebuild of the properties cache on the
        // next call to getProperties().
        mAllProperties = null;
    }

    /**
     * If it doesn't already exist, add a copy of a
     * global property to this item, as a new local
     * property.
     *
     * If the specified global property doesn't exist
     * that is a game error and we throw a runtime
     * exception).
     *
     * @param propKey - the key of the global property
     *                to add.
     *
     * @return the property that was added (or the
     * existing property).
     */
    MProperty addProperty(@NonNull String propKey) {
        MProperty ret = getProperty(propKey);
        if (ret == null) {
            MProperty prop = mAdv.mAllProperties.get(propKey);
            if (prop == null) {
                throw new RuntimeException("Global property " + propKey + " does not exist!");
            }
            ret = prop.copy();
            addProperty(ret);
        }
        return ret;
    }

    /**
     * Remove a local property from this item, if it exists.
     *
     * @param propKey - the key of the property to
     *                remove.
     */
    public void removeProperty(@NonNull String propKey) {
        if (mLocalProperties.remove(propKey) != null) {
            // Force a rebuild of the properties cache on the
            // next call to getProperties().
            mAllProperties = null;
        }
    }
}
