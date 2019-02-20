/*
 * Copyright (C) 2019 Tim Cadogan-Cowper.
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

import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.glk.GLKLogger;

import java.util.ArrayList;

/**
 * To make things easier, ADRIFT has the concept of References.  There are several types of
 * references.  These are Objects, Characters, Directions, Number and Text.  References
 * are essentially place-holders within the task command that can match on any object,
 * character etc.  References are simply entered as a keyword between two percent symbols.
 * <p>
 * For example, "put %object1% in{side/to} %object2%".
 */
public class MReference implements Cloneable {
    /**
     * A list of possible
     */
    @NonNull
    public ArrayList<MReferenceItem> mItems;

    /**
     * The type of reference - either Object, Character, Direction,
     * Number or Text.
     */
    public ReferencesType mType;

    /**
     * The command text that this reference matched against,
     * without the % boundary symbols. E.g. "object2", "character3",
     * etc.
     */
    @NonNull
    public String mRefMatch = "";
    /**
     * The zero-based index of this reference within a particular
     * command string (e.g. the reference associated with "%object2%"
     * would be position 1 in the command "put %object1% in{side/to}
     * %object2%".
     */
    int mCmdIndex;

    public MReference(@NonNull ReferencesType refType) {
        mItems = new ArrayList<>();
        mType = refType;
        mCmdIndex = -1;
    }

    public MReference(@NonNull MReference ref) {
        mItems = new ArrayList<>();
        mType = ref.mType;
        mCmdIndex = ref.mCmdIndex;
        mRefMatch = ref.mRefMatch;
    }

    public MReference(@NonNull String refType) {
        mItems = new ArrayList<>();
        mCmdIndex = -1;

        switch (refType) {
            case "%object%":
            case "%object1%":
            case "%object2%":
            case "%object3%":
            case "%object4%":
            case "%object5%":
            case "%objects%":
                mType = ReferencesType.Object;
                break;

            case "%character%":
            case "%character1%":
            case "%character2%":
            case "%character3%":
            case "%character4%":
            case "%character5%":
            case "%characters%":
                mType = ReferencesType.Character;
                break;

            case "%location%":
            case "%location1%":
            case "%location2":
            case "%location3":
            case "%location4":
            case "%location5":
                mType = ReferencesType.Location;
                break;

            case "%item%":
            case "%item1%":
            case "%item2%":
            case "%item3%":
            case "%item4%":
            case "%item5%":
                mType = ReferencesType.Item;
                break;

            case "%direction%":
            case "%direction1%":
            case "%direction2%":
            case "%direction3%":
            case "%direction4%":
            case "%direction5%":
                mType = ReferencesType.Direction;
                break;

            case "%text%":
            case "%text1%":
            case "%text2%":
            case "%text3%":
            case "%text4%":
            case "%text5%":
                mType = ReferencesType.Text;
                break;

            case "%number%":
            case "%number1%":
            case "%number2%":
            case "%number3%":
            case "%number4%":
            case "%number5%":
                mType = ReferencesType.Number;
                break;

            default:
                mType = ReferencesType.Unknown;
                //mAdv.mView.errMsg("MReference: Unknown reference type: " + refType);
                break;
        }
    }

    /**
     * Sets the index of this reference to the position of
     * the given reference in the given list of references.
     * If no match is found, the index is unchanged.
     *
     * @param refToFind    - the reference to find (e.g. %object1%)
     * @param refsToSearch - the list of references to search
     */
    void setIndex(@NonNull String refToFind, @NonNull MStringArrayList refsToSearch) {
        switch (refToFind) {
            case "%objects%":
            case "%object1%":
            case "%object2%":
            case "%object3%":
            case "%object4%":
            case "%object5%":
            case "%characters%":
            case "%character1%":
            case "%character2%":
            case "%character3%":
            case "%character4%":
            case "%character5%":
            case "%direction1%":
            case "%direction2%":
            case "%direction3%":
            case "%direction4%":
            case "%direction5%":
            case "%number1%":
            case "%number2%":
            case "%number3%":
            case "%number4%":
            case "%number5%":
            case "%text1%":
            case "%text2%":
            case "%text3%":
            case "%text4%":
            case "%text5%":
            case "%item1%":
            case "%item2%":
            case "%item3%":
            case "%item4%":
            case "%item5%":
                for (int i = 0, sz = refsToSearch.size(); i < sz; i++) {
                    if (refToFind.equals(refsToSearch.get(i))) {
                        mCmdIndex = i;
                        return;
                    }
                }
                break;
        }
    }

    @NonNull
    public MReference copy() {
        // Copy this reference and its items
        MReference nr = new MReference(mType);
        for (int i = 0, sz = mItems.size(); i < sz; i++) {
            MReferenceItem refItem = mItems.get(i);
            MReferenceItem itm = new MReferenceItem();
            itm.mMatchingKeys = refItem.mMatchingKeys.clone();
            itm.mIsExplicitlyMentioned = refItem.mIsExplicitlyMentioned;
            itm.mCommandReference = refItem.mCommandReference;
            nr.mItems.add(itm);
        }
        nr.mCmdIndex = mCmdIndex;
        nr.mRefMatch = mRefMatch;

        return nr;
    }

    /**
     * Appends a pipe ("|") delimited list of
     * the first possibility for each item in this
     * reference to the provided string builder.
     *
     * @param sb - the string builder to append to.
     */
    public void appendItemPossibilities(@NonNull StringBuilder sb) {
        int k = 0;
        for (MReferenceItem itm : mItems) {
            if (k++ > 0) {
                sb.append("|");
            }
            sb.append(itm.mMatchingKeys.get(0));
        }
    }

    public boolean containsKey(@NonNull String key) {
        for (int i = 0, sz = mItems.size(); i < sz; i++) {
            if (mItems.get(i).mMatchingKeys.contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Nullable
    public MReference clone() {
        try {
            return (MReference) super.clone();
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("MNewReference: clone failed: " + e.getMessage());
            return null;
        }
    }

    public static class MReferenceItem {
        @NonNull
        public MStringArrayList mMatchingKeys;
        boolean mIsExplicitlyMentioned;
        @NonNull
        String mCommandReference = "";

        MReferenceItem() {
            mMatchingKeys = new MStringArrayList();
        }

        public MReferenceItem(@NonNull String key) {
            this();
            mMatchingKeys.add(key);
        }
    }

    public enum ReferencesType {
        Object,         // 0
        Character,      // 1
        Number,         // 2
        Text,           // 3
        Direction,      // 4
        Location,       // 5
        Item,           // 6
        Unknown         // Bebek-specific addition
    }
}
