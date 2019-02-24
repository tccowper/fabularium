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

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.model.collection.MPropertyHashMap;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.io.MFileOlder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;

import static com.luxlunae.bebek.MGlobals.ALLROOMS;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.model.MGroup.GroupTypeEnum.Characters;
import static com.luxlunae.bebek.model.MGroup.GroupTypeEnum.Locations;
import static com.luxlunae.bebek.model.MGroup.GroupTypeEnum.Objects;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * A group is a collection of similar items.
 * <p>
 * There are 3 types of group in ADRIFT, Location groups, Object groups and Character groups.
 * <p>
 * Groups can be used as a simple way to determine if two items are related to each other,
 * but can also be used to set a particular property of all of its members to the same value.
 */
public class MGroup extends MItem {
    @NonNull
    public MPropertyHashMap mProperties;
    @NonNull
    private String mName = "";
    private GroupTypeEnum mType = Locations;
    @NonNull
    private MStringArrayList mMembers = new MStringArrayList();

    public MGroup(@NonNull MAdventure adv) {
        super(adv);
        mProperties = new MPropertyHashMap(adv);
    }

    public MGroup(@NonNull MAdventure adv, @NonNull MFileOlder.V4Reader reader,
                  int grpID, int nLocs, @NonNull final String[] locNames) throws EOFException {
        // ADRIFT V3.80, V3.90 and V4 Loader
        this(adv);

        // V4 and earlier versions of ADRIFT only
        // support room groups.
        setKey("Group" + grpID);
        setName(reader.readLine());
        for (int i = 0; i < nLocs; i++) {
            boolean isIncluded = cbool(reader.readLine());
            if (isIncluded) {
                getMembers().add(locNames[i]);
            }
        }
    }

    public MGroup(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                  boolean isLib, boolean addDupKeys) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Group");

        ArrayList<MProperty> props = new ArrayList<>();
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        header.processTag(xpp);
                        break;

                    case "Name":
                        setName(xpp.nextText());
                        break;

                    case "Type":
                        setGroupType(GroupTypeEnum.valueOf(xpp.nextText()));
                        break;

                    case "Member":
                        getMembers().add(xpp.nextText());
                        break;

                    case "Property":
                        try {
                            props.add(new MProperty(adv, xpp, MGlobals.dVersion));
                        } catch (Exception e) {
                            // do nothing
                        }
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Group");

        if (!header.finalise(this, adv.mGroups, isLib, addDupKeys, null)) {
            throw new Exception();
        }

        // Only add properties appropriate for the group type.
        for (MProperty prop : props) {
            if (isPropertyOfThisGroupType(prop)) {
                mProperties.put(prop);
            }
        }

        for (String itmKey : getMembers()) {
            MItemWithProperties itm = (MItemWithProperties) adv.getItemFromKey(itmKey);
            if (itm != null) {
                // In case we've accessed properties, and built
                // inherited before the group existed:
                itm.resetInherited();
            }
        }
    }

    private boolean isPropertyOfThisGroupType(@NonNull MProperty prop) {
        GroupTypeEnum grpType = getGroupType();

        switch (prop.getPropertyOf()) {
            case AnyItem:
                return true;
            case Locations:
                return grpType == Locations;
            case Objects:
                return grpType == Objects;
            case Characters:
                return grpType == Characters;
        }

        // shouldn't get here
        return false;
    }

    void add(@NonNull MItemWithProperties itm) {
        MStringArrayList itmKeys = getMembers();
        String itmKey = itm.getKey();
        if (!itmKeys.contains(itmKey)) {
            itmKeys.add(itmKey);
        }
        itm.resetInherited();
    }

    void remove(@NonNull MItemWithProperties itm) {
        MStringArrayList itmKeys = getMembers();
        itmKeys.remove(itm.getKey());
        itm.resetInherited();
    }

    @NonNull
    public MStringArrayList getMembers() {
        if (getKey().equals(ALLROOMS)) {
            mMembers.clear();
            mMembers.addAll(mAdv.mLocations.keySet());
        }
        return mMembers;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String value) {
        mName = value;
    }

    public GroupTypeEnum getGroupType() {
        return mType;
    }

    public void setGroupType(GroupTypeEnum value) {
        mType = value;
    }

    @NonNull
    String getRandomKey() {
        return getMembers().get(mAdv.getRand(this.getMembers().size() - 1));
    }

    @Nullable
    @Override
    public MGroup clone() {
        return (MGroup) super.clone();
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getName();
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        return new ArrayList<>();
    }

    @Override
    public int findLocal(@NonNull String toFind, @Nullable String toReplace,
                         boolean findAll, @NonNull int[] nReplaced) {
        int nReplacedIn = nReplaced[0];
        String[] t = new String[1];
        t[0] = mName;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        mName = t[0];
        return nReplaced[0] - nReplacedIn;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int ret = 0;
        for (MDescription d : getAllDescriptions()) {
            ret += d.getNumberOfKeyRefs(key);
        }
        if (!getKey().equals(ALLROOMS) && getMembers().contains(key)) {
            ret++;
        }
        ret += mProperties.getNumberOfKeyRefs(key);
        return ret;
    }

    @NonNull
    @Override
    public String getSymbol() {
        // white circle with two black dots
        return "\u2687";
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (MDescription d : getAllDescriptions()) {
            if (!d.deleteKey(key)) {
                return false;
            }
        }
        getMembers().remove(key);
        return mProperties.deleteKey(key);
    }

    public enum GroupTypeEnum {
        Locations,      // 0
        Objects,        // 1
        Characters      // 2
    }

    public static class MGroupState {
        @NonNull
        final ArrayList<String> mMembers = new ArrayList<>();
        public String mKey;

        MGroupState(@NonNull MGroup grp) {
            mKey = grp.getKey();
            mMembers.addAll(grp.getMembers());
        }

        MGroupState(@NonNull XmlPullParser xpp) throws Exception {
            xpp.require(START_TAG, null, "Group");

            int depth = xpp.getDepth();
            int evType;

            while ((evType = xpp.nextTag()) != END_DOCUMENT && xpp.getDepth() > depth) {
                if (evType == START_TAG) {
                    switch (xpp.getName()) {
                        case "Key": {
                            mKey = xpp.nextText();
                            break;
                        }
                        case "Member": {
                            mMembers.add(xpp.nextText());
                            break;
                        }
                    }
                }
            }

            xpp.require(END_TAG, null, "Group");
        }

        public void serialize(@NonNull XmlSerializer xs) throws IOException {
            xs.startTag(null, "Group");

            xs.startTag(null, "Key");
            xs.text(mKey);
            xs.endTag(null, "Key");

            for (String itmKey : mMembers) {
                xs.startTag(null, "Member");
                xs.text(itmKey);
                xs.endTag(null, "Member");
            }

            xs.endTag(null, "Group");
        }

        public void restore(@NonNull MGroup grp) {
            ArrayList<String> memToReset = new ArrayList<>(grp.getMembers());
            grp.getMembers().clear();
            for (String mem : mMembers) {
                grp.getMembers().add(mem);
                if (memToReset.contains(mem)) {
                    memToReset.remove(mem);
                } else {
                    memToReset.add(mem);
                }
            }
            for (String mem : memToReset) {
                MItem itm = grp.mAdv.mAllItems.get(mem);
                if (itm != null) {
                    ((MItemWithProperties) itm).resetInherited();
                }
            }
        }
    }
}
