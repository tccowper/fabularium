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
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.luxlunae.bebek.model.MSubEvent.MeasureEnum.Turns;
import static com.luxlunae.bebek.model.MSubEvent.WhatEnum.DisplayMessage;
import static com.luxlunae.bebek.model.MSubEvent.WhenEnum.FromLastSubEvent;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MSubEvent implements Cloneable {
    @NonNull
    private final MSubEvent mMe;
    public WhenEnum mWhen = FromLastSubEvent;
    public WhatEnum mWhat = DisplayMessage;
    public MeasureEnum mMeasure = Turns;
    @NonNull
    public MFromTo mTurns;
    @NonNull
    public MDescription mDescription;
    @NonNull
    public String mKey = "";
    @Nullable
    Timer mTrigger;
    @Nullable
    Date mStart;
    int mMilliseconds;
    @NonNull
    String mParentKey;
    @NonNull
    private final MAdventure mAdv;

    MSubEvent(@NonNull MAdventure adv, @NonNull String eventKey) {
        mParentKey = eventKey;
        mMe = this;
        mTurns = new MFromTo(adv);
        mAdv = adv;
        mDescription = new MDescription(adv);
    }

    MSubEvent(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
              double version) throws Exception {
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
                                    mTurns.mFrom = VB.cint(sData[0]);
                                    if (sData.length == 4) {
                                        mTurns.mTo = VB.cint(sData[2]);
                                        mWhen = MSubEvent.WhenEnum.valueOf(sData[3]);
                                    } else {
                                        mTurns.mTo = VB.cint(sData[0]);
                                        mWhen = MSubEvent.WhenEnum.valueOf(sData[1]);
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
                                mWhat = MSubEvent.WhatEnum.valueOf(sData[0]);
                                mKey = sData[1];
                            } catch (Exception e) {
                                mDescription = new MDescription(adv, xpp, version, "Action");
                            }
                            break;

                        case "Measure":
                            try {
                                s = xpp.nextText();
                                if (!s.equals("")) {
                                    mMeasure = MSubEvent.MeasureEnum.valueOf(s);
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
                                    mWhat = MSubEvent.WhatEnum.valueOf(s);
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
                                    mKey = s;
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
            se.mTurns = mTurns.clone();
            se.mDescription = mDescription.copy();
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
            if (mTrigger != null) {
                mTrigger.cancel();
            }

            try {
                mAdv.mEvents.get(mParentKey).runSubEvent(mMe);
                mAdv.mView.displayText(mAdv, "<br><br>", true);
                mAdv.checkEndOfGame();
            } catch (InterruptedException e) {
                GLKLogger.error("TimerTask: caught interrupted exception!");
            }
        }
    }
}
