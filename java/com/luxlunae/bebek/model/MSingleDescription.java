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

import com.luxlunae.bebek.model.collection.MRestrictionArrayList;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import static com.luxlunae.bebek.MGlobals.getBool;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MSingleDescription implements Cloneable {
    @NonNull
    public String mDescription = "";

    /**
     * Restrictions that control whether this alternate description will be displayed,
     * and how it interacts with the default and any other alternate descriptions.
     */
    @NonNull
    public MRestrictionArrayList mRestrictions;

    /**
     * If its restrictions pass, this field determines when this description is displayed.
     * <p>
     * AppendThisToPrevious - The default option is "append this to previous". This description
     * will simply be added to the end of any other descriptions
     * that are being displayed. If multiple "append this to previous"
     * pages pass their restrictions, then they are displayed in left-to-right order
     * after the Default Description (or after a "start description with" page, see below)
     * <p>
     * StartDescriptionWith - This is similar to an override for tasks, if this description
     * passes restrictions then none of the earlier descriptions in the same MDescription (including the
     * Default Description) will be displayed. This will be the first description displayed,
     * followed by any "append this to previous" descriptions in the same MDescription that come after.
     * If more than one "start description with" description passes restrictions, the later one
     * will override all of the others.
     * <p>
     * DisplayThisAfterDefault - This description will be displayed immediately after the
     * display of the Default Description. It will override all Alternate Descriptions that come
     * bebore it, preventing them from being displayed. Any "append this to previous" descriptions after
     * it, which pass restrictions, will then be added.
     */
    public DisplayWhenEnum mDisplayWhen = DisplayWhenEnum.AppendToPreviousDescription;

    /**
     * Name of the tab containing this description in the Adrift Runner.
     */
    @NonNull
    public String mTabLabel = "";

    /**
     * The "Only Display Once" option is a selectable tick-box that can be set
     * independently for each alternate description. If you select this for the
     * Default Description then the first time this description is displayed it will
     * show the Default Description, while on all following occasions it will
     * display the Alternate Description instead. This allows you to have a
     * very long detailed description the first time the player enters a
     * location or examines an object, and a more concise description with
     * just the important details subsequently.
     */
    public boolean mDisplayOnce = false;
    public boolean mDisplayed = false;
    boolean mReturnToDefault = false;

    /**
     * Work around to provide support for older ADRIFT games
     * that use the hide objects feature of room descriptions.
     */
    public boolean mCompatHideObjects = false;

    public MSingleDescription(@NonNull MAdventure adv) {
        super();
        mRestrictions = new MRestrictionArrayList(adv);
    }

    public MSingleDescription(@NonNull MAdventure adv,
                              @NonNull XmlPullParser xppDesc, double dFileVersion) throws Exception {
        this(adv);

        xppDesc.require(START_TAG, null, "Description");

        int eventType = xppDesc.nextTag();
        findNodes:
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case START_TAG:
                    String nodName = xppDesc.getName();
                    if (nodName.equals("Restrictions")) {
                        mRestrictions = new MRestrictionArrayList(adv, xppDesc, dFileVersion);
                    } else {
                        String s;
                        try {
                            s = xppDesc.nextText();
                        } catch (Exception e) {
                            // ignore
                            continue;
                        }
                        if (!s.equals("")) {
                            switch (nodName) {
                                case "DisplayWhen":
                                    mDisplayWhen = MSingleDescription.DisplayWhenEnum.valueOf(s);
                                    break;
                                case "Text":
                                    mDescription = s;
                                    break;
                                case "DisplayOnce":
                                    mDisplayOnce = getBool(s);
                                    break;
                                case "ReturnToDefault":
                                    mDisplayOnce = getBool(s);
                                    break;
                                case "TabLabel":
                                    mTabLabel = s;
                                    break;
                            }
                        }
                    }
                    break;

                case END_TAG:
                    if (xppDesc.getName().equals("Description")) {
                        break findNodes;
                    }
                    break;
            }
            eventType = xppDesc.nextTag();
        }
        xppDesc.require(END_TAG, null, "Description");
    }

    static DisplayWhenEnum MDisplayWhenEnumFromInt(int value) {
        switch (value) {
            default:
            case 0:
                return DisplayWhenEnum.StartDescriptionWithThis;
            case 1:
                return DisplayWhenEnum.StartAfterDefaultDescription;
            case 2:
                return DisplayWhenEnum.AppendToPreviousDescription;
        }
    }

    @Override
    @Nullable
    public MSingleDescription clone() {
        try {
            return (MSingleDescription)super.clone();
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("Could not clone MSingleDescription.");
            return null;
        }
    }

    public enum DisplayWhenEnum {
        StartDescriptionWithThis,       // 0
        StartAfterDefaultDescription,   // 1
        AppendToPreviousDescription     // 2
    }
}
