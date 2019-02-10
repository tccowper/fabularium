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

import com.luxlunae.bebek.VB;
import com.luxlunae.bebek.controller.MParser;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MSubEvent implements Cloneable {
    @NonNull
    private final MSubEvent oMe;
    public WhenEnum eWhen = WhenEnum.FromLastSubEvent;
    public WhatEnum eWhat = WhatEnum.DisplayMessage;
    public MeasureEnum eMeasure = MeasureEnum.Turns;
    @NonNull
    public MFromTo ftTurns;
    @NonNull
    public MDescription oDescription;
    @NonNull
    public String sKey = "";
    @Nullable
    Timer tmrTrigger;
    @Nullable
    Date dtStart;
    int iMilliseconds;
    @NonNull
    String sParentKey;
    @NonNull
    private final MAdventure mAdv;

    MSubEvent(@NonNull MAdventure adv, @NonNull String sEventKey) {
        sParentKey = sEventKey;
        oMe = this;
        ftTurns = new MFromTo(adv);
        mAdv = adv;
        oDescription = new MDescription(adv);
    }

    MSubEvent(@NonNull MAdventure adv, @NonNull XmlPullParser xpp, double dFileVersion) throws Exception {
        this(adv, "");

        xpp.require(START_TAG, null, "SubEvent");

        String s;
        String[] sData;
        int eventType = xpp.nextTag();
        int depth = xpp.getDepth();
        while (eventType != XmlPullParser.END_DOCUMENT && xpp.getDepth() >= depth) {
            switch (eventType) {
                case START_TAG:
                    switch (xpp.getName()) {
                        case "When":
                            try {
                                s = xpp.nextText();
                                if (!s.equals("")) {
                                    sData = s.split(" ");
                                    ftTurns.iFrom = VB.cint(sData[0]);
                                    if (sData.length == 4) {
                                        ftTurns.iTo = VB.cint(sData[2]);
                                        eWhen = MSubEvent.WhenEnum.valueOf(sData[3]);
                                    } else {
                                        ftTurns.iTo = VB.cint(sData[0]);
                                        eWhen = MSubEvent.WhenEnum.valueOf(sData[1]);
                                    }
                                }
                            } catch (Exception e) {
                                // do nothing
                                continue;
                            }
                            break;

                        case "Action":
                            try {
                                s = xpp.nextText();
                                sData = s.split(" ");
                                eWhat = MSubEvent.WhatEnum.valueOf(sData[0]);
                                sKey = sData[1];
                            } catch (Exception e) {
                                oDescription = new MDescription(adv, xpp, dFileVersion, "Action");
                            }
                            break;

                        case "Measure":
                            try {
                                s = xpp.nextText();
                                if (!s.equals("")) {
                                    eMeasure = MSubEvent.MeasureEnum.valueOf(s);
                                }
                            } catch (Exception e) {
                                // do nothing
                                continue;
                            }
                            break;

                        case "What":
                            try {
                                s = xpp.nextText();
                                if (!s.equals("")) {
                                    eWhat = MSubEvent.WhatEnum.valueOf(s);
                                }
                            } catch (Exception e) {
                                // do nothing
                                continue;
                            }
                            break;

                        case "OnlyApplyAt":
                            try {
                                s = xpp.nextText();
                                if (!s.equals("")) {
                                    sKey = s;
                                }
                            } catch (Exception e) {
                                // do nothing
                                continue;
                            }
                            break;
                    }
                    break;
            }
            eventType = xpp.nextTag();
        }

        xpp.require(END_TAG, null, "SubEvent");
    }

    @Override
    @Nullable
    public MSubEvent clone() {
        try {
            MSubEvent se = (MSubEvent) super.clone();
            se.ftTurns = ftTurns.clone();
            se.oDescription = oDescription.copy();
            return se;
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("MSubEvent: clone failed: " + e.getMessage());
            return null;
        }
    }

    public enum WhenEnum {
        FromLastSubEvent,   // 0
        FromStartOfEvent,   // 1
        BeforeEndOfEvent    // 2
    }

    public enum WhatEnum {
        DisplayMessage,     // 0
        SetLook,            // 1
        ExecuteTask,        // 2
        UnsetTask,          // 3
        ExecuteCommand      // 99 - for compatibility with version 3.8
    }

    public enum MeasureEnum {
        Turns,              // 0
        Seconds             // 1
    }

    public class SubEventTimerTask extends TimerTask {
        @Override
        public void run() {
            if (tmrTrigger != null) {
                tmrTrigger.cancel();
            }

            try {
                mAdv.mEvents.get(sParentKey).runSubEvent(oMe);
                MView.displayText(mAdv, "<br><br>", true);
                MParser.checkEndOfGame(mAdv);
            } catch (InterruptedException e) {
                GLKLogger.error("TimerTask: caught interrupted exception!");
            }
        }
    }
}
