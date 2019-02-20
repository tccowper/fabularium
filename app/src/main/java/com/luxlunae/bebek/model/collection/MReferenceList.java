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
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MReference;

import java.util.ArrayList;

import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Definite;
import static com.luxlunae.bebek.MGlobals.replaceAllIgnoreCase;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;

public class MReferenceList extends ArrayList<MReference> {
    @NonNull
    private final MAdventure mAdv;

    public MReferenceList(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public MReferenceList(@NonNull MAdventure adv, int size) {
        this(adv);
        for (int i = 0; i < size; i++) {
            add(null);
        }
    }

    public boolean resolveAmbiguity(@NonNull String input) {
        boolean resolved = false;

        for (MReference ref : this) {
            if (ref != null) {
                for (int i = 0, sz = ref.mItems.size(); i < sz; i++) {
                    MReference.MReferenceItem itm = ref.mItems.get(i);
                    if (itm.mMatchingKeys.size() != 1) {
                        if (!resolved) {
                            itm.mMatchingKeys =
                                    mAdv.resolveKeys(itm.mMatchingKeys, ref.mType, input);
                        }
                        if (itm.mMatchingKeys.size() != 1) {
                            return false;
                        }
                        String key = itm.mMatchingKeys.get(0);
                        switch (ref.mType) {
                            case Object: {
                                MObject ob = mAdv.mObjects.get(key);
                                if (ob != null) {
                                    if (!ob.isPlural()) {
                                        mAdv.mIt = ob.getFullName(Definite);
                                    } else {
                                        mAdv.mThem = ob.getFullName(Definite);
                                    }
                                }
                                break;
                            }
                            case Character: {
                                MCharacter ch = mAdv.mCharacters.get(key);
                                if (ch != null) {
                                    switch (ch.getGender()) {
                                        case Male:
                                            mAdv.mHim = ch.getName();
                                            break;
                                        case Female:
                                            mAdv.mHer = ch.getName();
                                            break;
                                        case Unknown:
                                            mAdv.mIt = ch.getName();
                                            break;
                                    }
                                }
                                break;
                            }
                        }
                        resolved = true;
                    }
                }
            }
        }
        return true;
    }

    public void replaceInText(@NonNull String refName, @NonNull StringBuilder text,
                              boolean isExpr, @NonNull MReference.ReferencesType refType) {
        String tgt = "%" + refName + "%";
        if (MGlobals.contains(text, tgt)) {
            boolean bQuote = isExpr && !MGlobals.contains(text, tgt + ".") &&
                    !MGlobals.contains(text, "\"" + tgt + "\"");
            for (MReference nr : this) {
                if (nr.mType == refType) {
                    if (nr.mRefMatch.equals(refName)) {
                        StringBuilder repl = new StringBuilder();
                        if (bQuote) {
                            repl.append("\"");
                        }
                        nr.appendItemPossibilities(repl);
                        if (bQuote) {
                            repl.append("\"");
                        }
                        if (repl.length() > 0) {
                            replaceAllIgnoreCase(text, tgt, repl.toString());
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Checks this set of references to see if any of them have items
     * with no possible values. In such cases remove those items. If
     * any reference has no items at all, reset that reference to a
     * copy of the reference at that same index in 'origRefs'.
     *
     * @param origRefs - a list of original references to reset to, if
     *                 any of the checked references no longer have any items.
     * @return whether this set of references is still ambiguous or not.
     */
    public boolean pruneImpossibleItems(@NonNull MReferenceList origRefs) {
        boolean refsAreAmbiguous = false;

        for (int iR = 0; iR < size(); iR++) {
            MReference nr = get(iR);
            boolean resetRef = false;
            if (nr != null) {
                if (nr.mItems.size() == 0) {
                    // This reference has no items, so we need to
                    // reset it then try to refine further under
                    // the next scope.
                    refsAreAmbiguous = true;
                    resetRef = true;
                } else {
                    for (int iI = nr.mItems.size() - 1; iI >= 0; iI--) {
                        MReference.MReferenceItem itm = nr.mItems.get(iI);
                        int nPossible = itm.mMatchingKeys.size();
                        if (nPossible == 0) {
                            // This reference item no longer has any valid
                            // possibilities, so remove it.
                            nr.mItems.remove(iI);
                            if (nr.mItems.size() == 0) {
                                // We no longer have any items for this
                                // reference, so we need to reset it and
                                // try refining again under the next scope.
                                refsAreAmbiguous = true;
                                resetRef = true;
                            }
                        } else if (nPossible > 1) {
                            // Multiple possibilities, so try to
                            // refine further under the next scope.
                            refsAreAmbiguous = true;
                        }
                    }
                }
                if (resetRef) {
                    // Reset this reference item to a copy of the task's
                    // original references.
                    set(iR, (origRefs.get(iR) != null) ? origRefs.get(iR).copy() : null);
                }
            }
        }
        return refsAreAmbiguous;
    }

    @NonNull
    public MReferenceList copyShallow() {
        // Copy references but not their items
        int len = size();
        MReferenceList ret = new MReferenceList(mAdv, len);
        for (int i = 0; i < len; i++) {
            if (get(i) != null) {
                ret.set(i, new MReference(get(i)));
            }
        }
        return ret;
    }

    @NonNull
    public MReferenceList copyDeep() {
        // Copy references and their items
        int len = size();
        MReferenceList ret = new MReferenceList(mAdv, len);
        for (int i = 0; i < len; i++) {
            if (get(i) != null) {
                ret.set(i, get(i).copy());
            }
        }
        return ret;
    }

    @Nullable
    public String getMatchingKey(@NonNull String refText) {
        for (MReference ref : this) {
            if (ref != null) {
                switch (refText) {
                    case "ReferencedObject":
                    case "ReferencedObject1":
                    case "ReferencedObject2":
                    case "ReferencedObject3":
                    case "ReferencedObject4":
                    case "ReferencedObject5":
                    case "ReferencedObjects": {
                        if (ref.mType == MReference.ReferencesType.Object) {
                            if (refText.equals("ReferencedObjects") ||
                                    (ref.mRefMatch.equals("object1") && (refText.equals("ReferencedObject") || refText.equals("ReferencedObject1"))) ||
                                    (ref.mRefMatch.equals("object2") && refText.equals("ReferencedObject2")) ||
                                    (ref.mRefMatch.equals("object3") && refText.equals("ReferencedObject3")) ||
                                    (ref.mRefMatch.equals("object4") && refText.equals("ReferencedObject4")) ||
                                    (ref.mRefMatch.equals("object5") && refText.equals("ReferencedObject5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingKeys.size() > 0) {
                                    return ref.mItems.get(0).mMatchingKeys.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;
                    }
                    case "ReferencedDirection":
                    case "ReferencedDirection1":
                    case "ReferencedDirection2":
                    case "ReferencedDirection3":
                    case "ReferencedDirection4":
                    case "ReferencedDirection5": {
                        if (ref.mType == MReference.ReferencesType.Direction) {
                            if ((ref.mRefMatch.equals("direction1") && (refText.equals("ReferencedDirection") || refText.equals("ReferencedDirection1"))) ||
                                    (ref.mRefMatch.equals("direction2") && refText.equals("ReferencedDirection2")) ||
                                    (ref.mRefMatch.equals("direction3") && refText.equals("ReferencedDirection3")) ||
                                    (ref.mRefMatch.equals("direction4") && refText.equals("ReferencedDirection4")) ||
                                    (ref.mRefMatch.equals("direction5") && refText.equals("ReferencedDirection5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingKeys.size() > 0) {
                                    return ref.mItems.get(0).mMatchingKeys.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;
                    }
                    case "ReferencedCharacter":
                    case "ReferencedCharacter1":
                    case "ReferencedCharacter2":
                    case "ReferencedCharacter3":
                    case "ReferencedCharacter4":
                    case "ReferencedCharacter5": {
                        if (ref.mType == MReference.ReferencesType.Character) {
                            if (ref.mRefMatch.equals("character1") && (refText.equals("ReferencedCharacter") || refText.equals("ReferencedCharacter1")) ||
                                    ref.mRefMatch.equals("character2") && refText.equals("ReferencedCharacter2") ||
                                    ref.mRefMatch.equals("character3") && refText.equals("ReferencedCharacter3") ||
                                    ref.mRefMatch.equals("character4") && refText.equals("ReferencedCharacter4") ||
                                    ref.mRefMatch.equals("character5") && refText.equals("ReferencedCharacter5")) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingKeys.size() > 0) {
                                    return ref.mItems.get(0).mMatchingKeys.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;
                    }
                    case "ReferencedLocation":
                    case "ReferencedLocation1":
                    case "ReferencedLocation2":
                    case "ReferencedLocation3":
                    case "ReferencedLocation4":
                    case "ReferencedLocation5": {
                        if (ref.mType == MReference.ReferencesType.Location) {
                            if ((ref.mRefMatch.equals("location1") && (refText.equals("ReferencedLocation") || refText.equals("ReferencedLocation1"))) ||
                                    (ref.mRefMatch.equals("location2") && refText.equals("ReferencedLocation2")) ||
                                    (ref.mRefMatch.equals("location3") && refText.equals("ReferencedLocation3")) ||
                                    (ref.mRefMatch.equals("location4") && refText.equals("ReferencedLocation4")) ||
                                    (ref.mRefMatch.equals("location5") && refText.equals("ReferencedLocation5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingKeys.size() > 0) {
                                    return ref.mItems.get(0).mMatchingKeys.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;
                    }
                    case "ReferencedItem":
                    case "ReferencedItem1":
                    case "ReferencedItem2":
                    case "ReferencedItem3":
                    case "ReferencedItem4":
                    case "ReferencedItem5": {
                        if (ref.mType == MReference.ReferencesType.Item) {
                            if ((ref.mRefMatch.equals("item1") && (refText.equals("ReferencedItem") || refText.equals("ReferencedItem1"))) ||
                                    (ref.mRefMatch.equals("item2") && refText.equals("ReferencedItem2")) ||
                                    (ref.mRefMatch.equals("item3") && refText.equals("ReferencedItem3")) ||
                                    (ref.mRefMatch.equals("item4") && refText.equals("ReferencedItem4")) ||
                                    (ref.mRefMatch.equals("item5") && refText.equals("ReferencedItem5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingKeys.size() > 0) {
                                    return ref.mItems.get(0).mMatchingKeys.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;
                    }
                    case "ReferencedNumber":
                    case "ReferencedNumber1":
                    case "ReferencedNumber2":
                    case "ReferencedNumber3":
                    case "ReferencedNumber4":
                    case "ReferencedNumber5": {
                        if (ref.mType == MReference.ReferencesType.Number) {
                            if ((ref.mRefMatch.equals("number1") && (refText.equals("ReferencedNumber") || refText.equals("ReferencedNumber1"))) ||
                                    (ref.mRefMatch.equals("number2") && refText.equals("ReferencedNumber2")) ||
                                    (ref.mRefMatch.equals("number3") && refText.equals("ReferencedNumber3")) ||
                                    (ref.mRefMatch.equals("number4") && refText.equals("ReferencedNumber4")) ||
                                    (ref.mRefMatch.equals("number5") && refText.equals("ReferencedNumber5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingKeys.size() > 0) {
                                    return ref.mItems.get(0).mMatchingKeys.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }

        return null;
    }

    public void printToDebug() {
        for (int i = 0; i < size(); i++) {
            switch (i) {
                case 0:
                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                            Medium, "First Reference: ", false);
                    break;
                case 1:
                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                            Medium, "Second Reference: ", false);
                    break;
                case 2:
                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                            Medium, "Third Reference: ", false);
                    break;
                case 3:
                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                            Medium, "Fourth Reference: ", false);
                    break;
                default:
                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                            Medium, "Reference " + i + ": ", false);
                    break;
            }

            int refCount = 0;
            MReference ref = get(i);
            if (ref != null) {
                for (MReference.MReferenceItem itm : ref.mItems) {
                    for (String key : itm.mMatchingKeys) {
                        if (key != null) {
                            switch (ref.mType) {
                                case Object:
                                    MObject ob = mAdv.mObjects.get(key);
                                    if (ob != null) {
                                        mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                                                Medium, ob.getFullName(), false);
                                    }
                                    break;
                                case Direction:
                                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                                            Medium, key, false);
                                    break;
                                case Character:
                                    MCharacter ch = mAdv.mCharacters.get(key);
                                    if (ch != null) {
                                        if (mAdv.mCharacters.containsKey(key)) {
                                            mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                                                    Medium, ch.getProperName(), false);
                                        }
                                    }
                                    break;
                                case Number:
                                case Text:
                                    break;
                            }
                            refCount++;
                            if (refCount < ref.mItems.size()) {
                                mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                                        Medium, ", ", false);
                            }
                        } else {
                            mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                                    Medium, "NULL reference", false);
                        }
                    }
                }
            } else {
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, "",
                        Medium, "Nothing", false);
            }
        }
    }

    public void setToNull() {
        for (int i = 0; i < size(); i++) {
            set(i, null);
        }
    }

}
