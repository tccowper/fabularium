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

import com.luxlunae.bebek.VB;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MSubWalk implements Cloneable {

    public WhenEnum eWhen = WhenEnum.FromLastSubWalk;
    public WhatEnum eWhat = WhatEnum.DisplayMessage;
    @NonNull
    public MFromTo ftTurns;
    @NonNull
    public MDescription oDescription;
    @NonNull
    public String sKey = "";
    @NonNull
    public String sKey2 = "";
    @NonNull
    public String sKey3 = "";
    boolean bSameLocationAsChar = false;
    boolean bSameLocationAsObj = false;

    MSubWalk(@NonNull MAdventure adv) {
        ftTurns = new MFromTo(adv);
        oDescription = new MDescription(adv);
    }

    MSubWalk(@NonNull MAdventure adv, @NonNull XmlPullParser xpp, double dFileVersion) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "Activity");

        String s;
        String sData[];
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    case "When":
                        sData = xpp.nextText().split(" ");
                        if (sData[0].equals(WhenEnum.ComesAcross.toString())) {
                            eWhen = WhenEnum.ComesAcross;
                            sKey = sData[1];
                        } else {
                            ftTurns.mFrom = VB.cint(sData[0]);
                            if (sData.length == 4) {
                                ftTurns.mTo = VB.cint(sData[2]);
                                eWhen = WhenEnum.valueOf(sData[3]);
                            } else {
                                ftTurns.mTo = VB.cint(sData[0]);
                                eWhen = WhenEnum.valueOf(sData[1]);
                            }
                        }
                        break;

                    case "Action":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                sData = s.split(" ");
                                eWhat = WhatEnum.valueOf(sData[0]);
                                sKey2 = sData[1];
                            }
                        } catch (Exception e) {
                            oDescription = new MDescription(adv, xpp, dFileVersion, "Action");
                        }
                        break;

                    case "OnlyApplyAt":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                sKey3 = s;
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Activity");
    }

    @Override
    public com.luxlunae.bebek.model.MSubWalk clone() {
        try {
            com.luxlunae.bebek.model.MSubWalk se = (com.luxlunae.bebek.model.MSubWalk) super.clone();
            se.ftTurns = ftTurns.clone();
            se.oDescription = oDescription.copy();
            return se;
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("MSubWalk: clone failed: " + e.getMessage());
            return null;
        }
    }

    public enum WhenEnum {
        FromLastSubWalk,    // 0
        FromStartOfWalk,    // 1
        BeforeEndOfWalk,    // 2
        ComesAcross         // 3
    }


    public enum WhatEnum {
        DisplayMessage,     // 0
        ExecuteTask,        // 1
        UnsetTask,          // 2
        ExecuteCommand      // 99 - for compatibility with version 3.8
    }
}
