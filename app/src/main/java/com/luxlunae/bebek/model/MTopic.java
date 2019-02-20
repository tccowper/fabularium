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

import com.luxlunae.bebek.model.collection.MActionArrayList;
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import static com.luxlunae.bebek.MGlobals.getBool;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MTopic implements Cloneable {
    @NonNull
    public String mKey = "";
    @NonNull
    public String mParentKey = "";
    @NonNull
    public String mSummary = "";
    @NonNull
    public String mKeywords = "";
    @NonNull
    public MDescription mDescription;
    @NonNull
    public MRestrictionArrayList mRestrictions;
    @NonNull
    public MActionArrayList mActions;
    public boolean mIsIntro;
    public boolean mIsAsk;
    public boolean mIsTell;
    public boolean mIsCommand;
    public boolean mIsFarewell;
    boolean mStayInMode;

    MTopic(@NonNull MAdventure adv) {
        mDescription = new MDescription(adv);
        mRestrictions = new MRestrictionArrayList(adv);
        mActions = new MActionArrayList(adv);
    }

    MTopic(@NonNull MAdventure adv, @NonNull XmlPullParser xpp, double dFileVersion) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "Topic");

        String s;
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    case "Key":
                        mKey = xpp.nextText();
                        break;

                    case "ParentKey":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mParentKey = s;
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Summary":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mSummary = s;
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Keywords":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mKeywords = s;
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Description":
                        mDescription = new MDescription(adv, xpp, dFileVersion, "Description");
                        break;

                    case "IsAsk":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mIsAsk = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "IsCommand":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mIsCommand = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "IsFarewell":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mIsFarewell = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "IsIntro":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mIsIntro = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "IsTell":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mIsTell = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "StayInMode":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mStayInMode = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Actions":
                        mActions = new MActionArrayList(adv, xpp, dFileVersion);
                        break;

                    case "Restrictions":
                        mRestrictions = new MRestrictionArrayList(adv, xpp, dFileVersion);
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Topic");
    }

    @Override
    @Nullable
    public MTopic clone() {
        MTopic topic;
        try {
            topic = (MTopic) super.clone();
            topic.mRestrictions = topic.mRestrictions.copy();
            topic.mActions = topic.mActions.copy();
            return topic;
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("Couldn't clone topic: " + e.getMessage());
            return null;
        }
    }
}
