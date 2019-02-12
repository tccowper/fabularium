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
import com.luxlunae.bebek.controller.MReference;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.view.MView;

import java.util.ArrayList;

import static com.luxlunae.bebek.MGlobals.replaceAllIgnoreCase;

public class MReferenceList extends ArrayList<MReference> {
    public MReferenceList() {
        super();
    }

    public MReferenceList(int size) {
        this();
        for (int i = 0; i < size; i++) {
            add(null);
        }
    }

    public void replaceInText(@NonNull String refName,
                              @NonNull StringBuilder text,
                              boolean isExpression,
                              @NonNull MReference.ReferencesType refType) {
        String tgt = "%" + refName + "%";
        if (MGlobals.contains(text, tgt)) {
            boolean bQuote = isExpression &&
                    !MGlobals.contains(text, tgt + ".") &&
                    !MGlobals.contains(text, "\"" + tgt + "\"");
            for (MReference nr : this) {
                if (nr.mType == refType) {
                    if (nr.mReferenceMatch.equals(refName)) {
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
        boolean bRefsAreAmbiguous = false;

        for (int iR = 0; iR < size(); iR++) {
            MReference nr = get(iR);
            boolean bResetRef = false;
            if (nr != null) {
                if (nr.mItems.size() == 0) {
                    // This reference has no items, so we need to
                    // reset it then try to refine further under
                    // the next scope.
                    bRefsAreAmbiguous = true;
                    bResetRef = true;
                } else {
                    for (int iI = nr.mItems.size() - 1; iI >= 0; iI--) {
                        MReference.MReferenceItem itm = nr.mItems.get(iI);
                        int nPossible = itm.mMatchingPossibilities.size();
                        if (nPossible == 0) {
                            // This reference item no longer has any valid
                            // possibilities, so remove it.
                            nr.mItems.remove(iI);
                            if (nr.mItems.size() == 0) {
                                // We no longer have any items for this
                                // reference, so we need to reset it and
                                // try refining again under the next scope.
                                bRefsAreAmbiguous = true;
                                bResetRef = true;
                            }
                        } else if (nPossible > 1) {
                            // Multiple possibilities, so try to
                            // refine further under the next scope.
                            bRefsAreAmbiguous = true;
                        }
                    }
                }
                if (bResetRef) {
                    // Reset this reference item to a copy of the task's original references
                    set(iR, (origRefs.get(iR) != null) ? origRefs.get(iR).copy() : null);
                }
            }
        }
        return bRefsAreAmbiguous;
    }

    @NonNull
    public MReferenceList copyShallow() {
        // Copy references but not their items
        int len = size();
        MReferenceList ret = new MReferenceList(len);
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
        MReferenceList ret = new MReferenceList(len);
        for (int i = 0; i < len; i++) {
            if (get(i) != null) {
                ret.set(i, get(i).copy());
            }
        }
        return ret;
    }

    @Nullable
    public String getReference(@NonNull String sReference) {
        for (MReference ref : this) {
            if (ref != null) {
                switch (sReference) {
                    case "ReferencedObject":
                    case "ReferencedObject1":
                    case "ReferencedObject2":
                    case "ReferencedObject3":
                    case "ReferencedObject4":
                    case "ReferencedObject5":
                    case "ReferencedObjects":
                        if (ref.mType == MReference.ReferencesType.Object) {
                            if (sReference.equals("ReferencedObjects") ||
                                    (ref.mReferenceMatch.equals("object1") && (sReference.equals("ReferencedObject") || sReference.equals("ReferencedObject1"))) ||
                                    (ref.mReferenceMatch.equals("object2") && sReference.equals("ReferencedObject2")) ||
                                    (ref.mReferenceMatch.equals("object3") && sReference.equals("ReferencedObject3")) ||
                                    (ref.mReferenceMatch.equals("object4") && sReference.equals("ReferencedObject4")) ||
                                    (ref.mReferenceMatch.equals("object5") && sReference.equals("ReferencedObject5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingPossibilities.size() > 0) {
                                    return ref.mItems.get(0).mMatchingPossibilities.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;

                    case "ReferencedDirection":
                    case "ReferencedDirection1":
                    case "ReferencedDirection2":
                    case "ReferencedDirection3":
                    case "ReferencedDirection4":
                    case "ReferencedDirection5":
                        if (ref.mType == MReference.ReferencesType.Direction) {
                            if ((ref.mReferenceMatch.equals("direction1") && (sReference.equals("ReferencedDirection") || sReference.equals("ReferencedDirection1"))) ||
                                    (ref.mReferenceMatch.equals("direction2") && sReference.equals("ReferencedDirection2")) ||
                                    (ref.mReferenceMatch.equals("direction3") && sReference.equals("ReferencedDirection3")) ||
                                    (ref.mReferenceMatch.equals("direction4") && sReference.equals("ReferencedDirection4")) ||
                                    (ref.mReferenceMatch.equals("direction5") && sReference.equals("ReferencedDirection5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingPossibilities.size() > 0) {
                                    return ref.mItems.get(0).mMatchingPossibilities.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;

                    case "ReferencedCharacter":
                    case "ReferencedCharacter1":
                    case "ReferencedCharacter2":
                    case "ReferencedCharacter3":
                    case "ReferencedCharacter4":
                    case "ReferencedCharacter5":
                        if (ref.mType == MReference.ReferencesType.Character) {
                            if (ref.mReferenceMatch.equals("character1") && (sReference.equals("ReferencedCharacter") || sReference.equals("ReferencedCharacter1")) ||
                                    ref.mReferenceMatch.equals("character2") && sReference.equals("ReferencedCharacter2") ||
                                    ref.mReferenceMatch.equals("character3") && sReference.equals("ReferencedCharacter3") ||
                                    ref.mReferenceMatch.equals("character4") && sReference.equals("ReferencedCharacter4") ||
                                    ref.mReferenceMatch.equals("character5") && sReference.equals("ReferencedCharacter5")) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingPossibilities.size() > 0) {
                                    return ref.mItems.get(0).mMatchingPossibilities.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;

                    case "ReferencedLocation":
                    case "ReferencedLocation1":
                    case "ReferencedLocation2":
                    case "ReferencedLocation3":
                    case "ReferencedLocation4":
                    case "ReferencedLocation5":
                        if (ref.mType == MReference.ReferencesType.Location) {
                            if ((ref.mReferenceMatch.equals("location1") && (sReference.equals("ReferencedLocation") || sReference.equals("ReferencedLocation1"))) ||
                                    (ref.mReferenceMatch.equals("location2") && sReference.equals("ReferencedLocation2")) ||
                                    (ref.mReferenceMatch.equals("location3") && sReference.equals("ReferencedLocation3")) ||
                                    (ref.mReferenceMatch.equals("location4") && sReference.equals("ReferencedLocation4")) ||
                                    (ref.mReferenceMatch.equals("location5") && sReference.equals("ReferencedLocation5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingPossibilities.size() > 0) {
                                    return ref.mItems.get(0).mMatchingPossibilities.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;

                    case "ReferencedItem":
                    case "ReferencedItem1":
                    case "ReferencedItem2":
                    case "ReferencedItem3":
                    case "ReferencedItem4":
                    case "ReferencedItem5":
                        if (ref.mType == MReference.ReferencesType.Item) {
                            if ((ref.mReferenceMatch.equals("item1") && (sReference.equals("ReferencedItem") || sReference.equals("ReferencedItem1"))) ||
                                    (ref.mReferenceMatch.equals("item2") && sReference.equals("ReferencedItem2")) ||
                                    (ref.mReferenceMatch.equals("item3") && sReference.equals("ReferencedItem3")) ||
                                    (ref.mReferenceMatch.equals("item4") && sReference.equals("ReferencedItem4")) ||
                                    (ref.mReferenceMatch.equals("item5") && sReference.equals("ReferencedItem5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingPossibilities.size() > 0) {
                                    return ref.mItems.get(0).mMatchingPossibilities.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;

                    case "ReferencedNumber":
                    case "ReferencedNumber1":
                    case "ReferencedNumber2":
                    case "ReferencedNumber3":
                    case "ReferencedNumber4":
                    case "ReferencedNumber5":
                        if (ref.mType == MReference.ReferencesType.Number) {
                            if ((ref.mReferenceMatch.equals("number1") && (sReference.equals("ReferencedNumber") || sReference.equals("ReferencedNumber1"))) ||
                                    (ref.mReferenceMatch.equals("number2") && sReference.equals("ReferencedNumber2")) ||
                                    (ref.mReferenceMatch.equals("number3") && sReference.equals("ReferencedNumber3")) ||
                                    (ref.mReferenceMatch.equals("number4") && sReference.equals("ReferencedNumber4")) ||
                                    (ref.mReferenceMatch.equals("number5") && sReference.equals("ReferencedNumber5"))) {
                                if (ref.mItems.size() > 0 && ref.mItems.get(0).mMatchingPossibilities.size() > 0) {
                                    return ref.mItems.get(0).mMatchingPossibilities.get(0);
                                } else {
                                    return null;
                                }
                            }
                        }
                        break;
                }
            }
        }

        return null;
    }

    public void printReferences(@NonNull MAdventure adv) {
        for (int iRef = 0; iRef < size(); iRef++) {
            switch (iRef) {
                case 0:
                    MView.debugPrint(MGlobals.ItemEnum.Task, "",
                            MView.DebugDetailLevelEnum.Medium,
                            "First Reference: ", false);
                    break;
                case 1:
                    MView.debugPrint(MGlobals.ItemEnum.Task, "",
                            MView.DebugDetailLevelEnum.Medium,
                            "Second Reference: ", false);
                    break;
                case 2:
                    MView.debugPrint(MGlobals.ItemEnum.Task, "",
                            MView.DebugDetailLevelEnum.Medium,
                            "Third Reference: ", false);
                    break;
                case 3:
                    MView.debugPrint(MGlobals.ItemEnum.Task, "",
                            MView.DebugDetailLevelEnum.Medium,
                            "Fourth Reference: ", false);
                    break;
                default:
                    MView.debugPrint(MGlobals.ItemEnum.Task, "",
                            MView.DebugDetailLevelEnum.Medium,
                            "Reference " + iRef + ": ", false);
                    break;
            }

            int iCount = 0;
            MReference ref = get(iRef);
            if (ref != null) {
                for (MReference.MReferenceItem itm : ref.mItems) {
                    for (String sKey : itm.mMatchingPossibilities) {
                        if (sKey != null) {
                            switch (ref.mType) {
                                case Object:
                                    if (adv.mObjects.containsKey(sKey)) {
                                        MView.debugPrint(MGlobals.ItemEnum.Task, "",
                                                MView.DebugDetailLevelEnum.Medium,
                                                adv.mObjects.get(sKey).getFullName(), false);
                                    }
                                    break;
                                case Direction:
                                    MView.debugPrint(MGlobals.ItemEnum.Task, "",
                                            MView.DebugDetailLevelEnum.Medium, sKey, false);
                                    break;
                                case Character:
                                    if (adv.mCharacters.containsKey(sKey)) {
                                        MView.debugPrint(MGlobals.ItemEnum.Task, "",
                                                MView.DebugDetailLevelEnum.Medium,
                                                adv.mCharacters.get(sKey).getProperName(), false);
                                    }
                                    break;
                                case Number:
                                    break;
                                case Text:
                                    break;
                            }
                            iCount++;
                            if (iCount < ref.mItems.size()) {
                                MView.debugPrint(MGlobals.ItemEnum.Task, "",
                                        MView.DebugDetailLevelEnum.Medium,
                                        ", ", false);
                            }
                        } else {
                            MView.debugPrint(MGlobals.ItemEnum.Task, "",
                                    MView.DebugDetailLevelEnum.Medium,
                                    "NULL reference", false);
                        }
                    }
                }
            } else {
                MView.debugPrint(MGlobals.ItemEnum.Task, "",
                        MView.DebugDetailLevelEnum.Medium,
                        "Nothing", false);
            }
        }
    }

    public void setToNull() {
        for (int i = 0; i < size(); i++) {
            set(i, null);
        }
    }

}
