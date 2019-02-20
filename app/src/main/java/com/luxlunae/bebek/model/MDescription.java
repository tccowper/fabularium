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

import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

import static com.luxlunae.bebek.model.MSingleDescription.DisplayWhenEnum.StartDescriptionWithThis;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * A collection of ADRIFT description tabs.
 */
public class MDescription extends ArrayList<MSingleDescription> {
    private static final String REGEX_OO_FUNC =
            ".*?(%?[A-Za-z][\\w|_-]*%?)(\\.%?[A-Za-z][\\w|_-]*%?(\\([A-Za-z ,_]+?\\))?)+";

    @NonNull
    private MAdventure mAdv;

    public MDescription(@NonNull MAdventure adv, @NonNull String desc) {
        // Every Description object must have at least one SingleDescription, which
        // is the default. Any optional SingleDescription added after that is an
        // "alternate description".
        mAdv = adv;
        MSingleDescription sd = new MSingleDescription(adv);
        sd.mDescription = desc;
        sd.mDisplayWhen = StartDescriptionWithThis;
        add(sd);
    }

    public MDescription(@NonNull MAdventure adv) {
        this(adv, "");
    }

    public MDescription(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                        double version, @NonNull String closingTag) throws Exception {
        this(adv);

        // Starting tag could be <Description> or others like <Introduction> or
        // <LongDescription>. So we can't verify the opening.

        // Ok, now find the Description nodes under it
        int evType = xpp.getEventType();

        findDescNodes:
        while (evType != END_DOCUMENT) {
            switch (evType) {
                case START_TAG:
                    switch (xpp.getName()) {
                        case "Description":
                            add(new MSingleDescription(adv, xpp, version));
                            break;
                    }
                    break;

                case END_TAG:
                    if (xpp.getName().equals(closingTag)) {
                        break findDescNodes;
                    }
                    break;
            }
            evType = xpp.nextTag();
        }

        xpp.require(END_TAG, null, closingTag);

        if (size() > 1) {
            remove(0);
        }
    }

    /**
     * Should we add spaces between one description tab and another?
     *
     * @param text - the text concatenated so far.
     *
     * @return TRUE if a space should be added before appending the next
     * description tab, FALSE otherwise.
     */
    private static boolean addSpace(@NonNull String text) {
        // Should we add spaces between one description tab and another
        if (text.equals("") || text.endsWith(" ") || text.endsWith("\n")) {
            return false;
        }

        // Add spaces after end of sentences
        if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) {
            return true;
        }

        // if it's a function then add a space - small chance the
        // function could evaluate to "", but we'll take that chance
        if (text.endsWith("%")) {
            return true;
        }

        // if text ends in an OO function return true, otherwise return false
        return text.matches(REGEX_OO_FUNC);
    }

    public int getNumberOfKeyRefs(@NonNull String key) {
        int ret = 0;
        for (MSingleDescription sd : this) {
            for (MRestriction r : sd.mRestrictions) {
                if (r.referencesKey(key)) {
                    ret++;
                }
            }
        }
        return ret;
    }

    public boolean deleteKey(@NonNull String key) {
        for (MSingleDescription sd : this) {
            if (!sd.mRestrictions.deleteKey(key)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    public String toString() {
        return toString(false, mAdv.mReferences, null);
    }

    @NonNull
    public String toString(@NonNull boolean[] hideObjs) {
        return toString(false, mAdv.mReferences, hideObjs);
    }

    @NonNull
    public String toString(boolean testing) {
        return toString(testing, mAdv.mReferences, null);
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
     * @param testing - if TRUE, we're just testing the value so we won't mark any text as having
     *                 been displayed. Otherwise text is marked as having been displayed.
     *
     * @return the concatenated string.
     */
    @NonNull
    public String toString(boolean testing, @Nullable MReferenceList refs,
                           @Nullable boolean[] hideObjs) {
        StringBuilder ret = new StringBuilder();

        String restTextIn = mAdv.mRestrictionText;
        String routeErrorIn = mAdv.mRouteErrorText;
        int restNumIn = MRestrictionArrayList.mRestNum;

        if (refs == null) {
            refs = new MReferenceList(mAdv);
        }

        try {
            for (MSingleDescription sd : this) {
                if (!sd.mDisplayOnce || !sd.mDisplayed) {
                    boolean displayed = false;
                    if (sd.mRestrictions.size() == 0 || sd.mRestrictions.passes(refs)) {
                        // Step 1 passes, concatenate
                        switch (sd.mDisplayWhen) {
                            case AppendToPreviousDescription:
                                if (addSpace(ret.toString())) {
                                    ret.append("  ");
                                }
                                ret.append("<>").append(sd.mDescription);
                                break;

                            case StartAfterDefaultDescription:
                                if (sd == get(0)) {
                                    // sd is the default description
                                    ret = new StringBuilder(sd.mDescription);
                                } else {
                                    // sd is an alternate description
                                    String defaultDesc = get(0).mDescription;
                                    ret = new StringBuilder(defaultDesc);
                                    if (addSpace(defaultDesc)) {
                                        ret.append("  ");
                                    }
                                    ret.append(sd.mDescription);
                                }
                                break;

                            case StartDescriptionWithThis:
                                ret = new StringBuilder(sd.mDescription);
                                break;
                        }
                        displayed = true;
                        if (hideObjs != null) {
                            hideObjs[0] = sd.mCompatHideObjects;
                        }
                    } else {
                        // Step 1 doesn't pass
                        if (!mAdv.mRestrictionText.equals("")) {
                            switch (sd.mDisplayWhen) {
                                case AppendToPreviousDescription:
                                    if (addSpace(ret.toString())) {
                                        ret.append("  ");
                                    }
                                    ret.append("<>").append(mAdv.mRestrictionText);
                                    break;

                                case StartAfterDefaultDescription:
                                    if (sd == get(0)) {
                                        // sd is the default description
                                        ret = new StringBuilder(mAdv.mRestrictionText);
                                    } else {
                                        // sd is an alternate description
                                        String defaultDesc = get(0).mDescription;
                                        ret = new StringBuilder(defaultDesc);
                                        if (addSpace(defaultDesc)) {
                                            ret.append("  ");
                                        }
                                        ret.append(mAdv.mRestrictionText);
                                    }
                                    break;

                                case StartDescriptionWithThis:
                                    ret = new StringBuilder(mAdv.mRestrictionText);
                                    break;
                            }
                        }
                    }
                    if (sd.mDisplayOnce) {
                        if ((!testing || MTask.mTestingOutput) && displayed) {
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
                        return ret.toString();
                    }
                }
            }
        } finally {
            mAdv.mRestrictionText = restTextIn;
            MRestrictionArrayList.mRestNum = restNumIn;
            mAdv.mRouteErrorText = routeErrorIn;
        }

        return ret.toString();
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