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

import com.luxlunae.bebek.controller.MParser;
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * A collection of ADRIFT description tabs.
 */
public class MDescription extends ArrayList<MSingleDescription> {
    private static final String REGEX_OO_FUNC = ".*?(%?[A-Za-z][\\w|_-]*%?)(\\.%?[A-Za-z][\\w|_-]*%?(\\([A-Za-z ,_]+?\\))?)+";
    @NonNull
    private MAdventure mAdv;

    public MDescription(@NonNull MAdventure adv, @NonNull XmlPullParser xppParent,
                        double dFileVersion, @NonNull String closingTag) throws Exception {
        this(adv);

        // Starting tag could be <Description> or others like, e.g, <Introduction> or <LongDescription>
        // So we can't verify the opening

        // Ok, now find the Description nodes under it
        int eventType = xppParent.getEventType();
        findDescNodes:
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case START_TAG:
                    switch (xppParent.getName()) {
                        case "Description":
                            add(new MSingleDescription(adv, xppParent, dFileVersion));
                            break;
                    }
                    break;

                case END_TAG:
                    if (xppParent.getName().equals(closingTag)) {
                        break findDescNodes;
                    }
                    break;
            }
            eventType = xppParent.nextTag();
        }
        xppParent.require(END_TAG, null, closingTag);

        if (size() > 1) {
            remove(0);
        }
    }

    public MDescription(@NonNull MAdventure adv) {
        this(adv, "");
    }

    public MDescription(@NonNull MAdventure adv, @NonNull String sDescription) {
        // Every Description object must have at least one SingleDescription, which
        // is the default. Any optional SingleDescription added after that is an
        // "alternate description".
        mAdv = adv;
        MSingleDescription sd = new MSingleDescription(adv);
        sd.mDescription = sDescription;
        sd.mDisplayWhen = MSingleDescription.DisplayWhenEnum.StartDescriptionWithThis;
        add(sd);
    }

    /**
     * Should we add spaces between one description tab and another?
     *
     * @param sText - the text concatenated so far.
     * @return TRUE if a space should be added before appending the next
     * description tab, FALSE otherwise.
     */
    private static boolean addSpace(@NonNull String sText) {
        // Should we add spaces between one description tab and another
        if (sText.equals("") || sText.endsWith(" ") || sText.endsWith("\n")) {
            return false;
        }

        // Add spaces after end of sentences
        if (sText.endsWith(".") || sText.endsWith("!") || sText.endsWith("?")) {
            return true;
        }

        // if it's a function then add a space - small chance the function could evaluate to "", but we'll take that chance
        if (sText.endsWith("%")) {
            return true;
        }

        // if text ends in an OO function return true, otherwise return false
        return sText.matches(REGEX_OO_FUNC);
    }

    public int referencesKey(@NonNull String sKey) {
        int iCount = 0;
        for (MSingleDescription m : this) {
            for (MRestriction r : m.mRestrictions) {
                if (r.referencesKey(sKey)) {
                    iCount++;
                }
            }
        }
        return iCount;
    }

    public boolean deleteKey(@NonNull String sKey) {
        for (MSingleDescription m : this) {
            if (!m.mRestrictions.deleteKey(sKey)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    public String toString() {
        return toString(false, MParser.mReferences, null);
    }

    @NonNull
    public String toString(@NonNull boolean[] hideObjs) {
        return toString(false, MParser.mReferences, hideObjs);
    }

    @NonNull
    public String toString(boolean bTesting) {
        return toString(bTesting, MParser.mReferences, null);
    }

    @NonNull
    public String toString(@Nullable MReferenceList refs) {
        return toString(false, refs, null);
    }

    /**
     * Concatenates all of this object's description tabs, by iterating over them in order,
     * starting with the default description and then each alternate description. For each
     * tab, (1) checks whether all of its restrictions are met, then - if they are -
     * (2) concatenates it depending upon its DisplayWhen property.
     *
     * @param bTesting - if TRUE, we're just testing the value so we won't mark any text as having
     *                 been displayed. Otherwise text is marked as having been displayed.
     *
     * @return the concatenated string.
     */
    @NonNull
    public String toString(boolean bTesting, @Nullable MReferenceList refs,
                           @Nullable boolean[] hideObjs) {
        StringBuilder sb = new StringBuilder();
        String sRestrictionTextIn = MParser.mRestrictionText;
        int iRestNumIn = MRestrictionArrayList.iRestNum;
        String sRouteErrorIn = MParser.mRouteErrorText;

        if (refs == null) {
            refs = new MReferenceList();
        }

        try {
            for (MSingleDescription sd : this) {
                if (!sd.mDisplayOnce || !sd.mDisplayed) {
                    boolean bDisplayed = false;
                    if (sd.mRestrictions.size() == 0 || sd.mRestrictions.passes(refs)) {
                        // Step 1 passes, concatenate
                        switch (sd.mDisplayWhen) {
                            case AppendToPreviousDescription:
                                if (addSpace(sb.toString())) {
                                    sb.append("  ");
                                }
                                sb.append("<>").append(sd.mDescription);
                                break;
                            case StartAfterDefaultDescription:
                                if (sd == get(0)) {
                                    // sd is the default description
                                    sb = new StringBuilder(sd.mDescription);
                                } else {
                                    // sd is an alternate description
                                    String sDefault = get(0).mDescription;
                                    if (addSpace(sDefault)) {
                                        sDefault += "  ";
                                    }
                                    sb = new StringBuilder(sDefault + sd.mDescription);
                                }
                                break;
                            case StartDescriptionWithThis:
                                sb = new StringBuilder(sd.mDescription);
                                break;
                        }
                        bDisplayed = true;
                        if (hideObjs != null) {
                            hideObjs[0] = sd.mCompatHideObjects;
                        }
                    } else {
                        // Step 1 doesn't pass
                        if (!MParser.mRestrictionText.equals("")) {
                            switch (sd.mDisplayWhen) {
                                case AppendToPreviousDescription:
                                    if (addSpace(sb.toString())) {
                                        sb.append("  ");
                                    }
                                    sb.append("<>").append(MParser.mRestrictionText);
                                    break;
                                case StartAfterDefaultDescription:
                                    if (sd == get(0)) {
                                        // sd is the default description
                                        sb = new StringBuilder(MParser.mRestrictionText);
                                    } else {
                                        // sd is an alternate description
                                        String sDefault = get(0).mDescription;
                                        if (addSpace(sDefault)) {
                                            sDefault += "  ";
                                        }
                                        sb = new StringBuilder(sDefault + MParser.mRestrictionText);
                                    }
                                    break;
                                case StartDescriptionWithThis:
                                    sb = new StringBuilder(MParser.mRestrictionText);
                                    break;
                            }
                        }
                    }
                    if (sd.mDisplayOnce) {
                        // Is this right, or should it mark Displayed = True if any text is output?
                        if ((!bTesting || MTask.mTestingOutput) && bDisplayed) {
                            sd.mDisplayed = true;
                            if (sd.mReturnToDefault) {
                                for (MSingleDescription sd2 : this) {
                                    sd2.mDisplayed = false;
                                    if (sd2 == sd) {
                                        break;
                                    }
                                }
                            }
                        }
                        return sb.toString();
                    }
                }
            }
        } finally {
            MParser.mRestrictionText = sRestrictionTextIn;
            MRestrictionArrayList.iRestNum = iRestNumIn;
            MParser.mRouteErrorText = sRouteErrorIn;
        }

        return sb.toString();
    }

    @NonNull
    public MDescription copy() {
        MDescription d = new MDescription(mAdv);
        d.clear();

        for (MSingleDescription sd : this) {
            MSingleDescription sdNew = new MSingleDescription(mAdv);
            sdNew.mDescription = sd.mDescription;
            sdNew.mDisplayWhen = sd.mDisplayWhen;
            sdNew.mRestrictions = sd.mRestrictions.copy();
            sdNew.mDisplayOnce = sd.mDisplayOnce;
            sdNew.mReturnToDefault = sd.mReturnToDefault;
            sdNew.mTabLabel = sd.mTabLabel;
            d.add(sdNew);
        }

        return d;
    }
}