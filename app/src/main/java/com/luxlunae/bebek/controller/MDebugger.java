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

package com.luxlunae.bebek.controller;

import android.os.Environment;
import android.support.annotation.NonNull;

import com.luxlunae.bebek.model.MALR;
import com.luxlunae.bebek.model.MAction;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MDescription;
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.MEventOrWalkControl;
import com.luxlunae.bebek.model.MGroup;
import com.luxlunae.bebek.model.MHint;
import com.luxlunae.bebek.model.MItem;
import com.luxlunae.bebek.model.MLocation;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MProperty;
import com.luxlunae.bebek.model.MRestriction;
import com.luxlunae.bebek.model.MSingleDescription;
import com.luxlunae.bebek.model.MSubEvent;
import com.luxlunae.bebek.model.MSubWalk;
import com.luxlunae.bebek.model.MSynonym;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.model.MTopic;
import com.luxlunae.bebek.model.MVariable;
import com.luxlunae.bebek.model.MWalk;
import com.luxlunae.bebek.model.collection.MActionArrayList;
import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MPropertyHashMap;
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.io.MFileIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.luxlunae.bebek.MGlobals.stripCarats;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TimeBased;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.CountingDownToStart;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllObjects;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.InLocation;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.SingleLocation;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.StateList;
import static com.luxlunae.bebek.model.MSubEvent.MeasureEnum.Seconds;
import static com.luxlunae.bebek.model.MSubWalk.WhatEnum.DisplayMessage;
import static com.luxlunae.bebek.model.MVariable.VariableType.Numeric;

public class MDebugger {
    public static final boolean BEBEK_DEBUG_ENABLED = false;
    private static final boolean ALLOW_BEBEK_DEBUG_TESTING = true;

    static void processDebugCommand(@NonNull MAdventure adv, String[] args) {
        adv.mView.out("<i>(\uD83E\uDD86 Quack! \uD83E\uDD86)</i>\n");
        if (args.length > 1) {
            StringBuilder sb = new StringBuilder();
            if (args.length > 2) {
                switch (args[1]) {
                    case "loc": {
                        MLocation loc = adv.mLocations.get(args[2]);
                        if (loc == null) {
                            sb.append("Location not found.\n");
                        } else {
                            appendDebugItemPoint(loc, sb, false);
                            sb.append("\n<u>DESCRIPTIONS</u>\n");
                            sb.append("Short description(s):\n");
                            appendDescriptions(sb, loc.getShortDescription());
                            sb.append("~~~~~~~~~~~~~~~~~~~~~\n");
                            sb.append("Long description(s):\n");
                            appendDescriptions(sb, loc.getLongDescription());
                            sb.append("~~~~~~~~~~~~~~~~~~~~\n");
                            sb.append("\n<u>DIRECTIONS</u>\n");
                            for (Map.Entry<MAdventure.DirectionsEnum, MLocation.MDirection> e :
                                    loc.mDirections.entrySet()) {
                                MAdventure.DirectionsEnum dir = e.getKey();
                                MLocation.MDirection d = e.getValue();
                                if (d.mLocationKey.equals("")) {
                                    continue;
                                }
                                sb.append("&nbsp;&nbsp;&#9632; Move ")
                                        .append(dir).append(" to ")
                                        .append(d.mLocationKey).append("\n");
                                appendRestrictions(sb, d.mRestrictions, 4);
                            }
                            appendProperties(sb, loc.getProperties());
                            MObjectHashMap obs =
                                    loc.getObjectsInLocation(AllObjects, false);
                            MCharacterHashMap chs =
                                    loc.getCharactersDirectlyInLocation(true);
                            if (obs.size() > 0 || chs.size() > 0) {
                                sb.append("\n<u>CONTENTS</u>\n");
                                for (MObject ob : obs.values()) {
                                    appendDebugItemPoint(ob, sb, true);
                                }
                                for (MCharacter ch : chs.values()) {
                                    appendDebugItemPoint(ch, sb, true);
                                }
                            }
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "obj": {
                        MObject obj = adv.mObjects.get(args[2]);
                        if (obj == null) {
                            sb.append("Object not found.\n");
                        } else {
                            appendDebugItemPoint(obj, sb, false);
                            sb.append("\n<u>DESCRIPTION</u>\n");
                            sb.append("Article: ").append(obj.getArticle()).append("\n");
                            sb.append("Prefix/Adjective: ").append(obj.getPrefix()).append("\n");
                            sb.append("Name/Nouns: ").append(obj.getNames().list()).append("\n");
                            sb.append("Type: ").append(obj.isStatic() ? "Static" : "Dynamic").append("\n");
                            sb.append("Location: ").append(obj.getLocation().getKey()).append("\n");
                            sb.append("Description(s):\n");
                            appendDescriptions(sb, obj.getDescription());
                            appendProperties(sb, obj.getProperties());
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "char": {
                        MCharacter ch = adv.mCharacters.get(args[2]);
                        if (ch == null) {
                            sb.append("Character not found.\n");
                        } else {
                            appendDebugItemPoint(ch, sb, false);
                            sb.append("\n<u>DESCRIPTION</u>\n");
                            sb.append("Proper Name: ").append(ch.getProperName()).append("\n");
                            sb.append("Article: ").append(ch.getArticle()).append("\n");
                            sb.append("Prefix/Adjective: ").append(ch.getPrefix()).append("\n");
                            if (ch.mDescriptors.size() > 0) {
                                sb.append("Descriptor/Noun:\n");
                                for (String s : ch.mDescriptors) {
                                    sb.append("&nbsp;&nbsp;&#9633; ").append(s).append("\n");
                                }
                            }
                            sb.append("Description(s):\n");
                            appendDescriptions(sb, ch.getDescription());
                            appendProperties(sb, ch.getProperties());
                            if (ch.mWalks.size() > 0) {
                                sb.append("\n<u>MOVEMENT</u>\n");
                                for (MWalk w : ch.mWalks) {
                                    // footprint symbol for each walk
                                    sb.append("----------------").append("\n");
                                    sb.append(Character.toChars(0x1F463))
                                            .append("&nbsp;&nbsp;<b>")
                                            .append(w.getDescription()).append("</b>\n");
                                    sb.append("----------------").append("\n");
                                    sb.append("This walk should start active: ")
                                            .append(w.getStartActive()).append("\n");
                                    if (w.mWalkControls.size() > 0) {
                                        sb.append("Walk Control(s):\n");
                                        for (MEventOrWalkControl c : w.mWalkControls) {
                                            sb.append("&nbsp;&nbsp;&#9632; ")
                                                    .append(c.eControl)
                                                    .append(" this walk on ")
                                                    .append(c.eCompleteOrNot.toString().toLowerCase())
                                                    .append(" of task ")
                                                    .append(c.mTaskKey).append("\n");
                                        }
                                    }
                                    sb.append("Repeat walk on completion: ")
                                            .append(w.getLoops()).append("\n");
                                    if (w.mSteps.size() > 0) {
                                        sb.append("Steps:\n");
                                        for (MWalk.MStep st : w.mSteps) {
                                            sb.append("&nbsp;&nbsp;&#9632; ")
                                                    .append("Move to ").append(st.mLocation)
                                                    .append(" and wait ").append(st.mTurns)
                                                    .append(" turn(s).\n");
                                        }
                                    }
                                    if (w.mSubWalks.size() > 0) {
                                        sb.append("Activities:\n");
                                        for (MSubWalk sw : w.mSubWalks) {
                                            sb.append("&nbsp;&nbsp;&#9632; After ")
                                                    .append(sw.ftTurns)
                                                    .append(" turn(s) ")
                                                    .append(sw.eWhen).append(" ");
                                            if (!sw.sKey.equals("")) {
                                                sb.append(sw.sKey).append(" ");
                                            }
                                            sb.append(sw.eWhat).append(" ");
                                            if (!sw.sKey2.equals("")) {
                                                sb.append(sw.sKey2);
                                            }
                                            sb.append("\n");
                                            if (sw.eWhat == DisplayMessage) {
                                                sb.append("Message:\n");
                                                appendDescriptions(sb, sw.oDescription);
                                            }
                                            if (!sw.sKey3.equals("")) {
                                                sb.append("Only apply at: ")
                                                        .append(sw.sKey3).append("\n");
                                            }
                                        }
                                    }
                                }
                            }
                            if (ch.mTopics.size() > 0) {
                                sb.append("\n<u>CONVERSATION</u>\n");
                                for (MTopic top : ch.mTopics.values()) {
                                    // light bulb symbol for each topic
                                    sb.append("----------------").append("\n");
                                    sb.append(Character.toChars(0x1F4A1))
                                            .append("&nbsp;&nbsp;<b>")
                                            .append(top.mSummary).append("</b>\n");
                                    sb.append("----------------").append("\n");
                                    sb.append("Type: ");
                                    if (top.mIsIntro) {
                                        sb.append("INTRO ");
                                    }
                                    if (top.mIsAsk) {
                                        sb.append("ASK ");
                                    }
                                    if (top.mIsTell) {
                                        sb.append("TELL ");
                                    }
                                    if (top.mIsCommand) {
                                        sb.append("CMD ");
                                    }
                                    if (top.mIsFarewell) {
                                        sb.append("FAREWELL");
                                    }
                                    sb.append("\n");
                                    sb.append("Keywords: \n").append(top.mKeywords).append("\n");
                                    sb.append("Conversation:\n");
                                    appendDescriptions(sb, top.mDescription);
                                    if (top.mRestrictions.size() > 0) {
                                        sb.append("~~~~~\n");
                                        sb.append("Restrictions:\n");
                                        appendRestrictions(sb, top.mRestrictions, 2);
                                    }
                                    if (top.mActions.size() > 0) {
                                        sb.append("~~~~~\n");
                                        sb.append("Actions:\n");
                                        for (MAction a1 : top.mActions) {
                                            sb.append("&nbsp;&nbsp;&#9633; ")
                                                    .append(a1.getSummary()).append("\n");
                                        }
                                    }
                                }
                            }
                            appendDebugRefs(adv, args[2], sb);

                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "charseen": {
                        MCharacter ch = adv.mCharacters.get(args[2]);
                        if (ch == null) {
                            sb.append("Character not found.\n");
                        } else {
                            appendDebugItemPoint(ch, sb, false);
                            sb.append("\n<u>SEEN LOCATIONS</u>").append("\n");
                            for (Map.Entry<String, Boolean> entry :
                                    ch.mSeenLocations.entrySet()) {
                                if (entry.getValue()) {
                                    String key = entry.getKey();
                                    MLocation l = adv.mLocations.get(key);
                                    if (l != null) {
                                        appendDebugItemPoint(l, sb, false);
                                    }
                                }
                            }
                            sb.append("\n<u>SEEN OBJECTS</u>").append("\n");
                            for (Map.Entry<String, Boolean> entry :
                                    ch.mSeenObjects.entrySet()) {
                                if (entry.getValue()) {
                                    String key = entry.getKey();
                                    MObject o = adv.mObjects.get(key);
                                    if (o != null) {
                                        appendDebugItemPoint(o, sb, false);
                                    }
                                }
                            }
                            sb.append("\n<u>SEEN CHARACTERS</u>").append("\n");
                            for (Map.Entry<String, Boolean> entry :
                                    ch.mSeenChars.entrySet()) {
                                if (entry.getValue()) {
                                    String key = entry.getKey();
                                    MCharacter ch2 = adv.mCharacters.get(key);
                                    if (ch2 != null) {
                                        appendDebugItemPoint(ch2, sb, false);
                                    }
                                }
                            }
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "task": {
                        MTask t = adv.mTasks.get(args[2]);
                        if (t == null) {
                            sb.append("Task not found.\n");
                        } else {
                            appendDebugItemPoint(t, sb, false);
                            sb.append("\n<u>DESCRIPTION</u>\n");
                            sb.append("Type: ").append(t.mType).append("\n");
                            switch (t.mType) {
                                case General:
                                    sb.append("Commands:\n");
                                    for (String s : t.mCommands) {
                                        sb.append("<pre>").append(s).append("</pre>\n");
                                    }
                                    break;
                                case Specific:
                                    // Every Specific task is the child of
                                    // either a General task, or another Specific task.
                                    sb.append("Task should ")
                                            .append(t.mSpecificOverrideType).append(" ")
                                            .append(t.mGeneralKey).append("\n");
                                    break;
                                case System:
                                    sb.append("Run this task ");
                                    if (!t.getLocationTrigger().equals("")) {
                                        sb.append(" when player enters location ")
                                                .append(t.getLocationTrigger()).append(".\n");
                                    } else if (t.getRunImmediately()) {
                                        sb.append(" immediately.\n");
                                    } else {
                                        sb.append(" only if called by an event or task.\n");
                                    }
                                    break;
                            }
                            sb.append("Message to display on completion:\n");
                            appendDescriptions(sb, t.getCompletionMessage());
                            sb.append("~~~~~~~~~~~~~~~~~~~~\n");
                            MRestrictionArrayList rr2 = t.mRestrictions;
                            if (rr2.size() > 0) {
                                sb.append("\n<u>RESTRICTIONS</u>\n");
                                appendRestrictions(sb, rr2, 2);
                            }
                            MActionArrayList aa1 = t.mActions;
                            if (aa1.size() > 0) {
                                sb.append("\n<u>ACTIONS</u>\n");
                                for (MAction a1 : aa1) {
                                    sb.append("&nbsp;&nbsp;&#9633; <pre>")
                                            .append(a1.getSummary()).append("</pre>\n");
                                }
                            }
                            sb.append("\n<u>ADVANCED</u>\n");
                            sb.append("Task priority: ")
                                    .append(t.getPriority()).append("\n");
                            sb.append("Auto-fill priority: ")
                                    .append(t.getAutoFillPriority()).append("\n");
                            sb.append("Prevent this task from being inherited: ")
                                    .append(t.mPreventOverriding).append("\n");
                            sb.append("Replace any existing task with same key: ")
                                    .append(t.mReplaceDuplicateKey).append("\n");
                            sb.append("Continue executing matching lower priority tasks: ")
                                    .append(t.getContinueToExecuteLowerPriority()).append("\n");
                            sb.append("Aggregate output: ")
                                    .append(t.mAggregateOutput).append("\n");
                            sb.append("Display completion message ")
                                    .append(t.mDisplayCompletion).append(" executing actions.\n");
                            sb.append("Fail override:\n");
                            appendDescriptions(sb, t.getFailOverride());
                            sb.append("~~~~~~~~~~~~~~~~~~~~\n");
                            sb.append("Task is repeatable: ")
                                    .append(t.getRepeatable()).append("\n");
                            sb.append("Task has completed: ")
                                    .append(t.getCompleted()).append("\n");
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "eventstart": {
                        MEvent ev = adv.mEvents.get(args[2]);
                        if (ev == null) {
                            sb.append("Event not found.\n");
                        } else {
                            try {
                                ev.start(true);
                                sb.append("Event started.");
                            } catch (InterruptedException e) {
                                sb.append("Couldn't start event.");
                            }
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "event": {
                        MEvent ev = adv.mEvents.get(args[2]);
                        if (ev == null) {
                            sb.append("Event not found.\n");
                        } else {
                            appendDebugItemPoint(ev, sb, false);
                            String unit = (ev.mEventType == TimeBased) ? "second(s)" : "turn(s)";
                            sb.append("\n<u>EVENT CONTROL</u>\n");
                            sb.append("This event should start off: ")
                                    .append(ev.mStatus).append("\n");
                            if (ev.mStatus == CountingDownToStart) {
                                sb.append("&nbsp;&nbsp;")
                                        .append(ev.mStartDelay).append(" ")
                                        .append(unit).append("\n");
                            }
                            sb.append("This event will last ")
                                    .append(ev.mLength).append(" turns\n");
                            if (ev.mEventControls.size() > 0) {
                                sb.append("Task control(s)\n");
                                for (MEventOrWalkControl ctl : ev.mEventControls) {
                                    sb.append("&nbsp;&nbsp;&#9632; ").append(ctl.eControl)
                                            .append(" this event on ")
                                            .append(ctl.eCompleteOrNot.toString().toLowerCase())
                                            .append(" of task ").append(ctl.mTaskKey).append("\n");
                                }
                            }
                            sb.append("Repeat event on completion: ")
                                    .append(ev.getRepeating()).append("\n");
                            sb.append("Repeat countdown: ").append(ev.RepeatCountdown).append("\n");
                            if (ev.mSubEvents.size() > 0) {
                                sb.append("\n<u>SUB-EVENTS</u>\n");
                                for (MSubEvent sev : ev.mSubEvents) {
                                    String unit2 =
                                            (sev.mMeasure == Seconds) ? "second(s)" : "turn(s)";
                                    sb.append("----------------").append("\n");
                                    sb.append("<b>").append(sev.mTurns)
                                            .append(" ").append(unit2).append(" ")
                                            .append(sev.mWhen).append(" ")
                                            .append(sev.mWhat).append(":</b>\n");
                                    sb.append("----------------").append("\n");
                                    switch (sev.mWhat) {
                                        case DisplayMessage:
                                        case SetLook:
                                            appendDescriptions(sb, sev.mDescription);
                                            sb.append("~~~~~~~~~~\n");
                                            sb.append("(Only applies when player at location: ")
                                                    .append(sev.mKey).append(")\n");
                                            break;
                                        case ExecuteTask:
                                            appendDebugItemPoint(adv.mTasks.get(sev.mKey), sb, true);
                                            break;
                                        case UnsetTask:
                                            appendDebugItemPoint(adv.mTasks.get(sev.mKey), sb, true);
                                            break;
                                        case ExecuteCommand:
                                            sb.append(sev.mKey).append("\n");
                                            break;
                                    }
                                }
                            }
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "var": {
                        MVariable v = adv.mVariables.get(args[2]);
                        if (v == null) {
                            sb.append("Variable not found.\n");
                        } else {
                            appendDebugItemPoint(v, sb, false);
                            sb.append("\nType: ").append(v.getType()).append("\n");
                            sb.append("Length: ").append(v.getLength()).append("\n");
                            sb.append("Current Value(s):\n");
                            if (v.getType() == MVariable.VariableType.Text) {
                                for (int i = 1; i <= v.getLength(); i++) {
                                    sb.append("&nbsp;&nbsp;[").append(i).append("] ")
                                            .append(v.getStrAt(i)).append("\n");
                                }
                            } else {
                                for (int i = 1; i <= v.getLength(); i++) {
                                    sb.append("&nbsp;&nbsp;[").append(i).append("] ")
                                            .append(v.getIntAt(i)).append("\n");
                                }
                            }
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "varset": {
                        MVariable v = adv.mVariables.get(args[2]);
                        if (v == null) {
                            sb.append("Variable not found.\n");
                        } else {
                            StringBuilder sb2 = new StringBuilder();
                            for (int i = 3; i < args.length; i++) {
                                sb2.append(args[i]);
                                if (i > 3) {
                                    sb2.append(" ");
                                }
                            }
                            v.setToExpr(sb2.toString(), adv.mReferences);
                            sb.append("Ok. Value of variable '")
                                    .append(v.getName())
                                    .append("' is now ")
                                    .append((v.getType() == Numeric) ?
                                            v.getInt() : v.getStr())
                                    .append("\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "group": {
                        MGroup grp = adv.mGroups.get(args[2]);
                        if (grp == null) {
                            sb.append("Group not found.\n");
                        } else {
                            appendDebugItemPoint(grp, sb, false);
                            sb.append("\n<u>SELECTIONS</u>\n");
                            sb.append("Group type: ").append(grp.getGroupType()).append("\n");
                            sb.append("Members:\n");
                            MStringArrayList sl = grp.getArlMembers();
                            for (String sKey : sl) {
                                MItem itm = adv.mAllItems.get(sKey);
                                appendDebugItemPoint(itm, sb, true);
                            }
                            appendProperties(sb, grp.mProperties);
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "prop": {
                        MProperty prop = adv.mAllProperties.get(args[2]);
                        if (prop == null) {
                            sb.append("Property not found.\n");
                        } else {
                            appendDebugItemPoint(prop, sb, false);
                            sb.append("\n<u>DEFINITION</u>\n");
                            sb.append("Property of: ").append(prop.getPropertyOf()).append("\n");
                            MProperty.PropertyTypeEnum typ = prop.getType();
                            sb.append("Type: ").append(typ).append("\n");
                            sb.append("Mandatory: ").append(prop.getMandatory()).append("\n");
                            String rProp = prop.getRestrictProperty();
                            if (!rProp.equals("")) {
                                sb.append("Restrict by property: ").append(rProp).append("\n");
                                sb.append("Restrict by value: ")
                                        .append(prop.getRestrictValue()).append("\n");
                            }
                            String k = prop.getDependentKey();
                            String v = prop.getDependentValue();
                            if (!k.equals("")) {
                                if (!v.equals("")) {
                                    sb.append("Property will only appear if:\n&nbsp;&nbsp;")
                                            .append(prop.getDependentKey()).append(" is set to ")
                                            .append(prop.getDependentValue()).append("\n");
                                } else {
                                    sb.append("Property will only appear if:\n&nbsp;&nbsp;")
                                            .append(k).append(" is selected.\n");
                                }
                            }
                            if (typ == StateList) {
                                sb.append("State List:\n");
                                for (String s : prop.mStates) {
                                    sb.append("&nbsp;&nbsp;&#9632; ").append(s).append("\n");
                                }
                            } else if (typ == MProperty.PropertyTypeEnum.ValueList) {
                                sb.append("Value List:\n");
                                for (String s : prop.mValueList.keySet()) {
                                    sb.append("&nbsp;&nbsp;&#9632; ").append(s)
                                            .append(" => ").append(prop.mValueList.get(s))
                                            .append("\n");
                                }
                            }
                            if (typ == StateList ||
                                    typ == MProperty.PropertyTypeEnum.ValueList) {
                                String ap = prop.getAppendToProperty();
                                sb.append("Append to: ")
                                        .append(ap.equals("") ? "&lt;Do not append&gt;" : ap)
                                        .append("\n");
                            }
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "alr": {
                        MALR alr = adv.mALRs.get(args[2]);
                        if (alr == null) {
                            sb.append("Text override not found.\n");
                        } else {
                            appendDebugItemPoint(alr, sb, false);
                            sb.append("Original text:\n");
                            sb.append("<pre>").append(alr.getOldText())
                                    .append("</pre>").append("\n");
                            sb.append("Replacement text:\n");
                            sb.append("<pre>").append(alr.getNewText().toString())
                                    .append("</pre>").append("\n");
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "hint": {
                        MHint hint = adv.mHints.get(args[2]);
                        if (hint == null) {
                            sb.append("Hint not found.\n");
                        } else {
                            appendDebugItemPoint(hint, sb, false);
                            sb.append("\n<u>DESCRIPTIONS</u>\n");
                            sb.append("Question:\n");
                            sb.append("<pre>").append(hint.getQuestion())
                                    .append("</pre>").append("\n");
                            sb.append("Subtle hint:\n");
                            appendDescriptions(sb, hint.getSubtleHint());
                            sb.append("~~~~~~~~~~~~~~~~~~~~~\n");
                            sb.append("Really obvious hint:\n");
                            appendDescriptions(sb, hint.getSledgeHammerHint());
                            sb.append("~~~~~~~~~~~~~~~~~~~~~\n");
                            MRestrictionArrayList r = hint.mRestrictions;
                            if (r.size() > 0) {
                                sb.append("\n<u>RESTRICTIONS</u>\n");
                                appendRestrictions(sb, r, 2);
                            }
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "syn": {
                        MSynonym syn = adv.mSynonyms.get(args[2]);
                        if (syn == null) {
                            sb.append("Synonym not found.\n");
                        } else {
                            appendDebugItemPoint(syn, sb, false);
                            sb.append("Replace any user input containing any of the following:\n");
                            for (String s : syn.getChangeFrom()) {
                                sb.append("&nbsp;&nbsp;&#9632; ").append(s).append("\n");
                            }
                            sb.append("with this:\n");
                            sb.append("&nbsp;&nbsp;").append(syn.getChangeTo()).append("\n");
                            appendDebugRefs(adv, args[2], sb);
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "img": {
                        // do a reverse lookup
                        try {
                            int resId = Integer.valueOf(args[2]);
                            if (args.length > 3) {
                                // assume this is offset, length
                                int len = Integer.valueOf(args[3]);
                                if (adv.mVersion >= 4 && adv.mVersion < 5) {
                                    MAdventure.v4Media media =
                                            new MAdventure.v4Media(resId, len, true);
                                    String fName = "test" + resId + len;
                                    adv.mV4Media.put(fName, media);
                                    sb.append("Attempting to display ").append(len)
                                            .append(" bytes of TAF starting at offset ")
                                            .append(resId).append(": \n");
                                    sb.append("<img src=\"").append(fName).append("\">\n");
                                }
                            } else {
                                if (adv.mVersion >= 4 && adv.mVersion < 5 &&
                                        adv.mV4Media.size() > 0) {
                                    for (Map.Entry<String, MAdventure.v4Media> file :
                                            adv.mV4Media.entrySet()) {
                                        if (file.getValue().mOffset == resId) {
                                            sb.append("Displaying v4 image at offset ")
                                                    .append(resId).append(": \n");
                                            sb.append("<img src=\"")
                                                    .append(file.getKey()).append("\">\n");
                                        }
                                    }
                                } else if (adv.mBlorbMappings.size() > 0) {
                                    for (Map.Entry<String, Integer> file :
                                            adv.mBlorbMappings.entrySet()) {
                                        if (file.getValue() == resId) {
                                            sb.append("Displaying v5 image ")
                                                    .append(resId).append(": \n");
                                            sb.append("<img src=\"")
                                                    .append(file.getKey()).append("\">\n");
                                        }
                                    }
                                } else {
                                    sb.append("No image found.\n");
                                }
                            }
                        } catch (Exception e) {
                            sb.append("Couldn't display image.\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "dump": {
                        // dumps the current game's source code into the given filename
                        String outFile = Environment.getExternalStorageDirectory().getPath() +
                                "/Fabularium/" + args[2];
                        File f = new File(outFile);
                        String sInPath = adv.getFullPath();
                        if (sInPath != null && !f.exists()) {
                            try {
                                MFileIO.dumpRawTAF(sInPath, outFile);
                                sb.append("Dumped source code of current game to '")
                                        .append(outFile).append("'\n");
                            } catch (Exception e) {
                                sb.append("Couldn't dump file: ")
                                        .append(e.getMessage()).append("\n");
                            }
                        } else {
                            sb.append("Can't dump source. Does the specified output " +
                                    "file already exist (if so you'll need to " +
                                    "delete it manually)?\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "taskf":
                    case "taskfa":
                    case "taskfr": {
                        // search for a task containing specific word
                        // taskf = search task descriptions, actions and restrictions
                        // taska = search task actions only
                        // taskr = search task restrictions only
                        String toFind = args[2];
                        boolean found = false;
                        int opt = args[1].equals("taskf") ? 0 : args[1].equals("taskfa") ? 1 : 2;
                        for (MTask t : adv.mTasks.values()) {
                            if (opt == 0 && t.containsText(toFind)) {
                                sb.append("[").append(t.getKey()).append("] ")
                                        .append(t.getCommonName()).append("\n");
                                found = true;
                            } else {
                                boolean f1 = false;
                                java.util.regex.Pattern regex = java.util.regex.Pattern
                                        .compile(java.util.regex.Pattern.quote(toFind),
                                                java.util.regex.Pattern.CASE_INSENSITIVE);
                                if (opt == 0 || opt == 1) {
                                    for (MAction act : t.mActions) {
                                        // case insensitive contains
                                        if (regex.matcher(act.getSummary()).find()) {
                                            sb.append("[").append(t.getKey()).append("] ")
                                                    .append(t.getCommonName()).append("\n");
                                            f1 = true;
                                            found = true;
                                            break;
                                        }
                                    }
                                }
                                if (!f1) {
                                    if (opt == 0 || opt == 2) {
                                        for (MRestriction r : t.mRestrictions) {
                                            if (regex.matcher(r.getSummary()).find()) {
                                                sb.append("[").append(t.getKey()).append("] ")
                                                        .append(t.getCommonName()).append("\n");
                                                found = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (!found) {
                            sb.append("No tasks found.\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "test": {
                        if (ALLOW_BEBEK_DEBUG_TESTING) {
                            // run a test file
                            StringBuilder dbgOut = new StringBuilder();
                            adv.mView.startDebugPlaybackMode(dbgOut);
                            try {
                                File testFile = new File(adv.getLibAdriftPath() +
                                        "/test", args[2]);
                                adv.mView.outImmediate("<b>*** Begin playback of '" +
                                        testFile.getAbsolutePath() + "' ***</b><br>");
                                BufferedReader bufferedReader =
                                        new BufferedReader(new FileReader(testFile));
                                String line;
                                StringBuilder expected = new StringBuilder();
                                boolean input = false;
                                while ((line = bufferedReader.readLine()) != null &&
                                        !adv.mView.isStopping()) {
                                    if (line.startsWith(">")) {
                                        if (expected.length() > 0 && dbgOut.length() > 0) {
                                            // check sbTestOut matches expected (we get rid of all
                                            // whitespace before doing the comparison as this can cause
                                            // issues for trivial differences we don't care about)
                                            String ee = stripCarats(expected.toString())
                                                    .replaceAll("\\s", "");
                                            String ff = stripCarats(dbgOut.toString())
                                                    .replaceAll("\\s", "");
                                            if (!ee.equals(ff)) {
                                                adv.mView.outImmediate("<font color=red>Failed.</font><br><i>Expected:</i><br>" +
                                                        expected + "<br><i>Got:</i><br>" + dbgOut.toString());
                                            } else {
                                                adv.mView.outImmediate("<font color=green>Passed.</font><br>");
                                            }
                                        } else if (dbgOut.length() > 0) {
                                            // if the debug file doesn't include expected text, then
                                            // just output what the game has generated.
                                            adv.mView.outImmediate(dbgOut.toString());
                                        }
                                        dbgOut.setLength(0);
                                        expected.setLength(0);
                                        line = line.substring(1).trim();
                                        String[] lines = line.split("[,.]");
                                        for (String l : lines) {
                                            adv.mView.outImmediate("&gt;&nbsp;<font color=blue>" + l + "<br></font>");
                                            adv.processCommand(l);
                                        }
                                        input = true;
                                    } else if (input && line.trim().length() > 0 &&
                                            !line.startsWith("#") && !line.startsWith("\n")) {
                                        if (expected.length() > 0) {
                                            expected.append(" ");
                                        }
                                        expected.append(line);
                                    }
                                    adv.mView.tick(adv);
                                }
                                bufferedReader.close();
                            } catch (IOException e) {
                                adv.mView.out("Can't find the test file.\n");
                            } catch (InterruptedException e) {
                                adv.mView.out("Playback interrupted.\n");
                            }
                            adv.mView.stopDebugPlaybackMode();
                            adv.mView.outImmediate("<b>*** Playback finished ***</b><br>");
                        }
                        return;
                    }
                    case "objf": {
                        // search for an object containing specific word
                        String toFind = args[2];
                        boolean found = false;
                        for (MObject t : adv.mObjects.values()) {
                            if (t.containsText(toFind)) {
                                found = true;
                                sb.append("[").append(t.getKey()).append("] ")
                                        .append(t.getCommonName()).append("\n");
                            }
                        }
                        if (!found) {
                            sb.append("No objects found.\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "varf": {
                        // search for a variable containing specific word
                        String toFind = args[2];
                        boolean found = false;
                        for (MVariable t : adv.mVariables.values()) {
                            if (t.containsText(toFind)) {
                                found = true;
                                sb.append("[").append(t.getKey()).append("] ")
                                        .append(t.getCommonName()).append("\n");
                            }
                        }
                        if (!found) {
                            sb.append("No variables found.\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "locf": {
                        // search for a location containing specific word
                        String toFind = args[2];
                        boolean found = false;
                        for (MLocation t : adv.mLocations.values()) {
                            if (t.containsText(toFind)) {
                                found = true;
                                sb.append("[").append(t.getKey()).append("] ")
                                        .append(t.getCommonName()).append("\n");
                            }
                        }
                        if (!found) {
                            sb.append("No locations found.\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "groupf": {
                        // search for a group containing specific word
                        String toFind = args[2];
                        boolean found = false;
                        for (MGroup t : adv.mGroups.values()) {
                            if (t.containsText(toFind)) {
                                found = true;
                                sb.append("[").append(t.getKey()).append("] ")
                                        .append(t.getCommonName()).append("\n");
                            }
                        }
                        if (!found) {
                            sb.append("No groups found.\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "alrf": {
                        // search for a text override containing specific word
                        String toFind = args[2];
                        boolean found = false;
                        for (MALR t : adv.mALRs.values()) {
                            if (t.containsText(toFind)) {
                                found = true;
                                sb.append("[").append(t.getKey()).append("] ")
                                        .append(t.getCommonName()).append("\n");
                            }
                        }
                        if (!found) {
                            sb.append("No overrides found.\n");
                        }
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "alrdel": {
                        String toDel = args[2];
                        MALR ret = adv.mALRs.remove(toDel);
                        sb.append((ret != null ? "Deleted override.\n" :
                                "Override not found.\n"));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    }
                    case "taskexe": {
                        // try to execute a task directly
                        MTask t = adv.mTasks.get(args[2]);
                        if (t == null) {
                            sb.append("Task not found.\n");
                            sb.append("\n");
                            adv.mView.out(sb.toString());
                        } else {
                            try {
                                t.attemptToExecute();
                                adv.checkEndOfGame();
                                if (adv.mGameState != MAction.EndGameEnum.Running) {
                                    return;
                                }
                                adv.prepareForNextTurn();
                                adv.getPlayer().doWalk();
                            } catch (InterruptedException e) {
                                sb.append("Task execution interrupted!");
                                sb.append("\n");
                                adv.mView.out(sb.toString());
                            }
                        }
                        return;
                    }
                    case "objname": {
                        if (args.length > 3) {
                            MObject obj = adv.mObjects.get(args[2]);
                            if (obj == null) {
                                sb.append("Object not found.\n");
                            } else {
                                obj.getNames().add(args[3]);
                                sb.append("Added name '").append(args[3]).append("' to object.\n");
                            }
                            sb.append("\n");
                            adv.mView.out(sb.toString());
                            return;
                        }
                    }
                    case "restdel": {
                        // delete a task restriction
                        // restdel <taskID> <restIndex>
                        if (args.length > 3) {
                            MTask task = adv.mTasks.get(args[2]);
                            if (task == null) {
                                sb.append("Task not found\n");
                            } else {
                                int restIndex = Integer.valueOf(args[3]);
                                if (restIndex > task.mRestrictions.size()) {
                                    sb.append("Index not valid - task has ")
                                            .append(task.mRestrictions.size()).append("\n");
                                } else {
                                    task.mRestrictions.remove(restIndex);
                                    sb.append("Removed restriction ")
                                            .append(restIndex).append(" of task ")
                                            .append(task.getKey()).append("\n");
                                }
                            }
                            sb.append("\n");
                            adv.mView.out(sb.toString());
                            return;
                        }
                    }
                    case "restedit": {
                        // change a bracket sequence
                        // restedit <taskID> <new bracket sequence>
                        if (args.length > 3) {
                            MTask task = adv.mTasks.get(args[2]);
                            if (task == null) {
                                sb.append("Task not found\n");
                            } else {
                                task.mRestrictions.mBrackSeq = args[3];
                                sb.append("Changed bracket sequence of task ").append(" of task ")
                                        .append(task.getKey()).append(" to ")
                                        .append(task.mRestrictions.mBrackSeq).append("\n");
                            }
                            sb.append("\n");
                            adv.mView.out(sb.toString());
                            return;
                        }
                    }
                    case "xyzzy": {
                        if (args.length > 3) {
                            MLocation loc2 = adv.mLocations.get(args[3]);
                            if (loc2 == null) {
                                sb.append("Location not found.\n");
                            } else {
                                MCharacter ch = adv.mCharacters.get(args[2]);
                                if (ch != null) {
                                    // teleport character
                                    MCharacter.MCharacterLocation dest =
                                            new MCharacter.MCharacterLocation(adv, ch);
                                    dest.setExistsWhere(ch.getLocation().getExistsWhere());
                                    dest.setPosition(ch.getLocation().getPosition());
                                    dest.setExistsWhere(AtLocation);
                                    dest.setKey(loc2.getKey());
                                    ch.moveTo(dest);
                                    if (ch == adv.getPlayer()) {
                                        sb.append("You feel groggy. Better eat some peanuts.\n");
                                    } else {
                                        if (loc2.getKey().equals(adv.getPlayer()
                                                .getLocation().getLocationKey())) {
                                            sb.append(ch.getProperName())
                                                    .append(" materialises in front of you!\n");
                                        } else {
                                            sb.append("You sense a disturbance in the Force.\n");
                                        }
                                    }
                                } else {
                                    MObject ob = adv.mObjects.get(args[2]);
                                    if (ob != null) {
                                        // teleport object
                                        MObject.MObjectLocation dest =
                                                new MObject.MObjectLocation(adv);
                                        if (ob.isStatic()) {
                                            dest.mStaticExistWhere = SingleLocation;
                                            dest.setKey(loc2.getKey());
                                        } else {
                                            dest.mDynamicExistWhere = InLocation;
                                            dest.setKey(loc2.getKey());
                                        }
                                        ob.moveTo(dest);
                                        if (loc2.getKey().equals(adv.getPlayer()
                                                .getLocation().getLocationKey())) {
                                            sb.append(ob.getCommonName())
                                                    .append(" materialises in front of you!\n");
                                        } else {
                                            sb.append("You sense a disturbance in the Force.\n");
                                        }
                                    } else {
                                        sb.append("Character or object not found.\n");
                                    }
                                }
                            }
                            sb.append("\n");
                            adv.mView.out(sb.toString());
                            return;
                        }
                    }
                }
            } else {
                switch (args[1]) {
                    case "help":
                        sb.append("Currently I recognise the following commands:\n\n");
                        sb.append("  <b>summary</b>: print a summary of the adventure file.\n");
                        sb.append("  <b>xyzzy &lt;Character or object key&gt; &lt;Location key&gt;</b>: moves the given character or object to the given location.\n");
                        sb.append("  <b>img &lt;blorb resource #&gt;</b>: display the image with the given numeric identifier.\n");
                        sb.append("  <b>locs</b>: list the locations.\n");
                        sb.append("  <b>objs</b>: list the objects.\n");
                        sb.append("  <b>chars</b>: list the characters.\n");
                        sb.append("  <b>charseen &lt;charID&gt;</b>: list all the locations, objects and characters seen by the given character.\n");
                        sb.append("  <b>tasks</b>: list the tasks.\n");
                        sb.append("  <b>tasks2</b>: list the tasks, sorted by execution priority.\n");
                        sb.append("  <b>taskorder</b>: change the order of task execution (first passing or failing task, or highest priority passing task\n");
                        sb.append("  <b>taskf &lt;word&gt;</b>: list all tasks containing the given word in their description, restrictions or actions.\n");
                        sb.append("  <b>taskfa &lt;word&gt;</b>: list all tasks containing the given word in their actions.\n");
                        sb.append("  <b>taskfr &lt;word&gt;</b>: list all tasks containing the given word in their restrictions.\n");
                        sb.append("  <b>restdel &lt;taskID&gt; &lt;restIndex&gt;</b>: delete restriction at given index from the given task.\n");
                        sb.append("  <b>restedit &lt;taskID&gt; &lt;bracketSequence&gt;</b>: change restriction bracket sequence of the given task.\n");
                        sb.append("  <b>vars</b>:  list the variables.\n");
                        sb.append("  <b>varset &lt;Variable key&gt; &lt;expression&gt;</b>: set given varible to expression.\n");
                        sb.append("  <b>events</b>: list the events.\n");
                        sb.append("  <b>eventstart &lt;Event key&gt;</b>: force given event to start.\n");
                        sb.append("  <b>groups</b>: list the groups.\n");
                        sb.append("  <b>props</b>: list the properties.\n");
                        sb.append("  <b>alrs</b>: list the text overrides.\n");
                        sb.append("  <b>hints</b>: list the hints.\n");
                        sb.append("  <b>syns</b>: list the synonyms.\n");
                        sb.append("  <b>udfs</b>: list the user-defined functions.\n");
                        sb.append("  <b>files</b>: list all the image and sound files packed with this game, either in the Blorb (v5+) or in the TAF (v4).\n");
                        sb.append("  <b>dump &lt;outFileName&gt;</b>: dumps the raw source of this game to outFileName in the Fabularium folder.\n\n");
                        sb.append("You may also type the singular form of any of the above, followed by a key, ")
                                .append("to see detailed info about that specific item. E.g. <pre>@bebek char Character1</pre> ")
                                .append(" would print detailed info about the current status of Character1, assuming that key exists.\n");
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "password":
                        sb.append("Password is '").append(adv.mPassword).append("'");
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "summary":
                        sb.append("<pre>").append(adv.getTitle()).append("</pre> by <pre>").append(adv.getAuthor()).append("</pre>\n");
                        sb.append("Last updated: ").append(adv.mCompatCompileDate).append("\n");
                        sb.append("ADRIFT Version: ").append(adv.getVersion()).append("\n\n");
                        sb.append("Locations: ").append(adv.mLocations.size()).append("\n");
                        sb.append("Objects: ").append(adv.mObjects.size()).append("\n");
                        sb.append("Characters: ").append(adv.mCharacters.size()).append("\n");
                        sb.append("Tasks: ").append(adv.mTasks.size()).append("&nbsp;&nbsp;&nbsp;&nbsp;<i>")
                                .append(adv.mTaskExecutionMode).append("</i>\n");
                        sb.append("Variables: ").append(adv.mVariables.size()).append("\n");
                        sb.append("Events: ").append(adv.mEvents.size()).append("\n");
                        sb.append("Groups: ").append(adv.mGroups.size()).append("\n");
                        sb.append("Properties: ").append(adv.mAllProperties.size()).append("\n");
                        sb.append("Text overrides: ").append(adv.mALRs.size()).append("\n");
                        sb.append("Hints: ").append(adv.mHints.size()).append("\n");
                        sb.append("Synonyms: ").append(adv.mSynonyms.size()).append("\n");
                        sb.append("User-defined functions: ").append(adv.mUDFs.size()).append("\n");
                        int nFiles = (adv.mVersion >= 4 && adv.mVersion < 5) ?
                                adv.mV4Media.size() : adv.mBlorbMappings.size();
                        sb.append("Additional files: ").append(nFiles).append("\n");
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "taskorder":
                        adv.mTaskExecutionMode = (adv.mTaskExecutionMode == MAdventure.TaskExecutionEnum.HighestPriorityTask) ?
                                MAdventure.TaskExecutionEnum.HighestPriorityPassingTask :
                                MAdventure.TaskExecutionEnum.HighestPriorityTask;
                        sb.append("Changed task execution order to ").append(adv.mTaskExecutionMode).append(".\n\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "locs":
                        appendItemList(sb, new ArrayList<MItem>(adv.mLocations.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "objs":
                        appendItemList(sb, new ArrayList<MItem>(adv.mObjects.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "chars":
                        appendItemList(sb, new ArrayList<MItem>(adv.mCharacters.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "tasks":
                        appendItemList(sb, new ArrayList<MItem>(adv.mTasks.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "tasks2":
                        sb.append("(completable tasks - will be searched for command matches)\n");
                        appendItemList(sb, new ArrayList<MItem>(adv.mCompletableTasks.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "vars":
                        appendItemList(sb, new ArrayList<MItem>(adv.mVariables.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "events":
                        appendItemList(sb, new ArrayList<MItem>(adv.mEvents.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "groups":
                        appendItemList(sb, new ArrayList<MItem>(adv.mGroups.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "props":
                        appendItemList(sb, new ArrayList<MItem>(adv.mAllProperties.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "alrs":
                        appendItemList(sb, new ArrayList<MItem>(adv.mALRs.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "hints":
                        appendItemList(sb, new ArrayList<MItem>(adv.mHints.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "syns":
                        appendItemList(sb, new ArrayList<MItem>(adv.mSynonyms.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "udfs":
                        appendItemList(sb, new ArrayList<MItem>(adv.mUDFs.values()));
                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                    case "files":
                        if (adv.mVersion >= 4 && adv.mVersion < 5 && adv.mV4Media.size() > 0) {
                            for (Map.Entry<String, MAdventure.v4Media> map : adv.mV4Media.entrySet()) {
                                sb.append(map.getKey()).append(" => off: ")
                                        .append(map.getValue().mOffset).append(", len: ")
                                        .append(map.getValue().mLength)
                                        .append("\n");
                            }
                        } else if (adv.mBlorbMappings.size() > 0) {
                            for (Map.Entry<String, Integer> map : adv.mBlorbMappings.entrySet()) {
                                sb.append(map.getKey()).append(" => ")
                                        .append(map.getValue()).append("\n");
                            }
                        } else {
                            sb.append("No files found.\n");
                        }

                        sb.append("\n");
                        adv.mView.out(sb.toString());
                        return;
                }
            }
        }

        adv.mView.out("Sorry, I don't recognise that debug command. " +
                "Type <pre>@bebek help</pre> for valid options.\n\n");
    }

    private static void appendDebugItemPoint(@NonNull MItem itm,
                                             @NonNull StringBuilder sb, boolean indent) {
        if (indent) {
            sb.append("&nbsp;&nbsp;");
        }
        sb.append(itm.getSymbol())
                .append(" <b>[")
                .append(itm.getKey())
                .append("]</b>&nbsp;&nbsp;<pre>")
                .append(itm.getCommonName())
                .append("</pre>")
                .append("\n");
    }

    private static void appendProperties(StringBuilder sb, MPropertyHashMap props) {
        if (props.size() > 0) {
            sb.append("\n<u>PROPERTIES</u>\n");
            for (MProperty prop : props.values()) {
                // ruler symbol for each property
                if (prop.getAppendToProperty().equals("")) {
                    sb.append("----------------").append("\n");
                    sb.append(Character.toChars(0x1F4CF))
                            .append("&nbsp;&nbsp;<b>")
                            .append(prop.getCommonName()).append("</b>\n");
                    sb.append("<i>").append(prop.getType()).append("</i>\n");
                    sb.append("----------------").append("\n");
                    switch (prop.getType()) {
                        case StateList:
                        case ObjectKey:
                        case CharacterKey:
                        case LocationKey:
                        case LocationGroupKey:
                        case Text:
                            appendDescriptions(sb, prop.getStringData());
                            break;

                        case Integer:
                        case ValueList:
                            sb.append(String.valueOf(prop.getIntData()));
                            break;

                        case SelectionOnly:
                            sb.append("true");
                            break;
                    }
                    sb.append("\n");
                }
            }
        }
    }

    private static void appendDescriptions(StringBuilder sb, MDescription d) {
        int i = 0;
        String lbl;
        for (MSingleDescription s : d) {
            if (!s.mDescription.equals("")) {
                if (i == 0) {
                    sb.append("~~~~ Default Description ~~~~\n");
                    sb.append("(Only display once: ").append(s.mDisplayOnce).append(")\n");
                } else {
                    lbl = s.mTabLabel;
                    sb.append("~~~~ ").append(lbl.equals("") ?
                            "Alternate Description " + i : lbl).append(" ~~~~\n");
                    sb.append("(Only display once: ").append(s.mDisplayOnce).append(")\n");
                    sb.append("(If all restrictions met ").append(s.mDisplayWhen).append(")\n");
                }
                sb.append("<pre>").append(s.mDescription).append("</pre>\n");
                if (s.mCompatHideObjects) {
                    sb.append("[HIDE OBJECTS]\n");
                }
            }
            appendRestrictions(sb, s.mRestrictions, 2);
            i++;
        }
    }

    private static void appendRestrictions(StringBuilder sb, MRestrictionArrayList rr, int indent) {
        if (rr.size() == 0) {
            return;
        }
        StringBuilder t = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            t.append("&nbsp;");
        }
        sb.append(t).append(rr.mBrackSeq).append("\n");
        for (MRestriction r : rr) {
            // stop sign
            sb.append(t).append(Character.toChars(0x1F6D1))
                    .append("&nbsp;<pre>").append(r.getSummary()).append("</pre>\n");
            appendDescriptions(sb, r.mMessage);
            sb.append("\n");
        }
    }

    private static void appendDebugRefs(@NonNull MAdventure adv,
                                        @NonNull String refKey, @NonNull StringBuilder sb) {
        // print the tasks and events that reference 'refKey', if any
        boolean found = false;
        sb.append("\n<u>REFERENCES</u>").append("\n");
        for (MTask t : adv.mTasks.values()) {
            int i = t.getKeyRefCount(refKey);
            if (i > 0) {
                found = true;
                appendDebugItemPoint(t, sb, true);
            }
        }
        for (MEvent e : adv.mEvents.values()) {
            int i = e.getKeyRefCount(refKey);
            if (i > 0) {
                found = true;
                appendDebugItemPoint(e, sb, true);
            }
        }
        if (!found) {
            sb.append("&nbsp;&nbsp;(no tasks or events)");
        }
    }

    private static void appendItemList(StringBuilder sb, List<MItem> items) {
        for (MItem item : items) {
            appendDebugItemPoint(item, sb, true);
        }
    }
}
