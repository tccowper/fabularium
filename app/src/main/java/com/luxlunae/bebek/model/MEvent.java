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

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.controller.MParser;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.collection.MTaskHashMap;
import com.luxlunae.bebek.model.io.MFileOlder;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.Stack;
import java.util.Timer;

import static com.luxlunae.bebek.MGlobals.ALLROOMS;
import static com.luxlunae.bebek.MGlobals.getBool;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.VB.cint;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MEvent extends MItem implements MItemFunctionEvaluator {

    @NonNull
    public final ArrayList<MEventOrWalkControl> mEventControls = new ArrayList<>();
    @NonNull
    public final ArrayList<MSubEvent> mSubEvents = new ArrayList<>();
    public boolean RepeatCountdown;
    @NonNull
    private final Stack<MLookText> mStackLookText = new Stack<>();
    public EventTypeEnum mEventType = EventTypeEnum.TurnBased;
    public int mLastSubEventTime = 0;
    public StatusEnum mStatus = StatusEnum.NotYetStarted;
    @NonNull
    public MFromTo mStartDelay;
    @NonNull
    public MFromTo mLength;
    @Nullable
    public MSubEvent mLastSubEvent;
    public boolean mJustStarted = false;
    WhenStartEnum mWhenStart = WhenStartEnum.NotSet;
    @NonNull
    String mTriggeringTask = "";
    @NonNull
    private String mDescription = "";
    private boolean mRepeating;
    private int mTimerToEventOfEvent;
    private Command mNextCommand = Command.Nothing;

    public MEvent(@NonNull MAdventure adv) {
        super(adv);
        mStartDelay = new MFromTo(adv);
        mLength = new MFromTo(adv);
    }

    public MEvent(@NonNull MAdventure adv,
                  @NonNull MFileOlder.V4Reader reader, int iEvent, int iStartMaxPriority,
                  int iNumLocations, double v) throws EOFException {
        // ADRIFT V3.80, 3.90 and V4 Loader
        this(adv);

        String sLocationKey = "";
        setKey("Event" + iEvent);
        setDescription(reader.readLine());                                  // $Short
        mWhenStart = MEvent.parseWhenStartEnum(reader.readLine());          // #StarterType
        if (mWhenStart == MEvent.WhenStartEnum.BetweenXandYTurns) {         // ?#StarterType=2:
            mStartDelay.iFrom = cint(reader.readLine()) - 1;                //   #StartTime
            mStartDelay.iTo = cint(reader.readLine()) - 1;                  //   #EndTime
        }
        if (mWhenStart == MEvent.WhenStartEnum.AfterATask) {                // ?#StarterType=3:
            String sStartTask = "Task" + reader.readLine();                 //  #TaskNum
            MEventOrWalkControl ec = new MEventOrWalkControl();
            ec.eControl = MEventOrWalkControl.MControlEnum.Start;
            ec.sTaskKey = sStartTask;
            mEventControls.add(ec);
        }
        setRepeating(cbool(reader.readLine()));                             // #RestartType
        int iTaskMode = cint(reader.readLine());                            // BTaskFinished
        mLength.iFrom = cint(reader.readLine());                            // #Time1
        mLength.iTo = cint(reader.readLine());                              // #Time2
        if (mWhenStart == MEvent.WhenStartEnum.BetweenXandYTurns) {
            mLength.iFrom--;
            mLength.iTo--;
        }

        // =============================================================
        //          MESSAGE TO DISPLAY WHEN THE EVENT STARTS
        // -------------------------------------------------------------
        // "When the event starts, you will probably want to say something
        // to announce the fact. If the event was rain, you could put in
        // What to display on event start: "It starts to rain."  This
        // will always be displayed if you are in the selected room(s).
        // -------------------------------------------------------------
        String sBuffer = reader.readLine();                                 // $StartText
        if (!sBuffer.equals("")) {
            MSubEvent se = new MSubEvent(adv, getKey());
            se.eWhat = MSubEvent.WhatEnum.DisplayMessage;
            se.eWhen = MSubEvent.WhenEnum.FromStartOfEvent;
            se.ftTurns.iFrom = 0;
            se.ftTurns.iTo = 0;
            se.oDescription = new MDescription(adv,
                    MFileOlder.convertV4FuncsToV5(adv, sBuffer));
            mSubEvents.add(se);
        }

        // =============================================================
        //         MESSAGE TO APPEND WHEN PLAYER TYPES "LOOK"
        // -------------------------------------------------------------
        // "In the box What to display during event if player "looks",
        // this message is appended to the room description. In the
        // above example, you would want to add something such as
        // 'It is raining.'"
        // -------------------------------------------------------------
        sBuffer = reader.readLine();                                        // $LookText
        if (!sBuffer.equals("")) {
            MSubEvent se = new MSubEvent(adv, getKey());
            se.eWhat = MSubEvent.WhatEnum.SetLook;
            se.eWhen = MSubEvent.WhenEnum.FromStartOfEvent;
            se.ftTurns.iFrom = 0;
            se.ftTurns.iTo = 0;
            se.oDescription = new MDescription(adv,
                    MFileOlder.convertV4FuncsToV5(adv, sBuffer));
            mSubEvents.add(se);
        }

        // =============================================================
        //           MESSAGES TO DISPLAY WHEN EVENT FINISHES
        // -------------------------------------------------------------
        // "Usually, you will want a message displayed when the event
        // finishes. In this example, it might be something like 'The
        // rain stops.'  Again, this will always display if the Player
        // is in the selected room(s)."
        // -------------------------------------------------------------
        String sEndMessage = reader.readLine();                             // $FinishText
        if (!sEndMessage.equals("")) {
            MSubEvent se = new MSubEvent(adv, getKey());
            se.eWhat = MSubEvent.WhatEnum.DisplayMessage;
            se.eWhen = MSubEvent.WhenEnum.BeforeEndOfEvent;
            se.ftTurns.iFrom = 0;
            se.ftTurns.iTo = 0;
            se.oDescription = new MDescription(adv,
                    MFileOlder.convertV4FuncsToV5(adv, sEndMessage));
            mSubEvents.add(se);
        }

        // =============================================================
        //           WHERE THE EVENT MESSAGES WILL BE SHOWN
        //                   (<ROOM_LIST0>Where)
        // -------------------------------------------------------------
        // Get the "rooms where this event's descriptions are shown" data.
        //
        // Possible values for #Type are:
        //
        //   0 = None     : technically this means the event will not be
        //                  visible anywhere.
        //
        //   1 = Single   : visible in one room.
        //
        //   2 = Multiple : visible in multiple rooms.
        //
        //   3 = All     :  visible in all rooms.
        // -------------------------------------------------------------
        int iWhichRooms = cint(reader.readLine());                          // #Type
        switch (iWhichRooms) {
            case 0:
                // No rooms
                sLocationKey = "";
                break;
            case 1:
                // Single Room
                sLocationKey = "Location" +
                        (cint(reader.readLine()) + 1);                      // Room #
                break;
            case 2:
                // Multiple Rooms
                // Create a room group containing them
                boolean bShowRoom;
                MStringArrayList arlShowInRooms = new MStringArrayList();
                for (int n = 1; n <= iNumLocations; n++) {
                    bShowRoom = cbool(reader.readLine());                   // iIsHere
                    if (bShowRoom) {
                        arlShowInRooms.add("Location" + n);
                    }
                }
                sLocationKey =
                        MFileOlder.getRoomGroupFromList(adv, arlShowInRooms,
                        "event '" + getDescription() + "'").getKey();
                break;
            case 3:
                // All Rooms
                sLocationKey = ALLROOMS;
                break;
        }

        for (int i = 0; i <= 1; i++) {
            // --------------------------------------------------------
            // You can pause or resume your event depending upon
            // whether a task has completed or not. Up to 2.
            // --------------------------------------------------------
            int iTask = cint(reader.readLine());                         // #PauseTask / #ResumeTask
            int iCompleteOrNot = cint(reader.readLine());                // BPauserCompleted / BResumerCompleted
            if (iTask > 0) {
                MEventOrWalkControl ec = new MEventOrWalkControl();
                ec.eControl = (i == 0) ?
                        MEventOrWalkControl.MControlEnum.Suspend :
                        MEventOrWalkControl.MControlEnum.Resume;
                ec.sTaskKey = "Task" + (iTask - 1);
                ec.eCompleteOrNot = (iCompleteOrNot == 0) ?
                        MEventOrWalkControl.CompleteOrNotEnum.Completion :
                        MEventOrWalkControl.CompleteOrNotEnum.UnCompletion;
                mEventControls.add(ec);
            }

            // --------------------------------------------------------
            // You can add up to two extra messages that appear
            // when the event is ending. You specify how many turns
            // from the end of the event the message should appear, and
            // set your message.  In the same example, you might want
            // Display this 3 turns from event finish: "The rain eases
            // off slightly.", Display this 1 turns from event
            // finish: "The rain has almost ceased."  This will also
            // always display if in the correct room(s).
            // --------------------------------------------------------
            int iFrom = cint(reader.readLine());                         // #PrefTime1 / #PrefTime2
            sBuffer = reader.readLine();                                 // $PrefText1 / $PrefText2
            if (!sBuffer.equals("")) {
                MSubEvent se = new MSubEvent(adv, getKey());
                se.eWhat = MSubEvent.WhatEnum.DisplayMessage;
                se.eWhen = MSubEvent.WhenEnum.BeforeEndOfEvent;
                se.ftTurns.iFrom = iFrom;
                se.ftTurns.iTo = iFrom;
                se.oDescription = new MDescription(adv,
                        MFileOlder.convertV4FuncsToV5(adv, sBuffer));
                mSubEvents.add(se);
            }
        }

        // =============================================================
        //                    MOVE OBJECTS AROUND
        //     (#Obj2 #Obj2Dest #Obj3 #Obj3Dest #Obj1 #Obj1Dest)
        // -------------------------------------------------------------
        // You may want to move objects about when the event starts or
        // finishes. You can move one object when the event starts and
        // two when it finishes. You also have the added flexibility of
        // being able to move static objects, so if you want a task to
        // move a static object, you can use an event to start as soon
        // as the task is complete, which then moves the object.
        // -------------------------------------------------------------
        MTask tas = null;
        boolean[] iDoneTask = new boolean[2];
        int[][] iMoveObs = new int[3][2];
        for (int i : new int[]{1, 2, 0}) {
            for (int j = 0; j <= 1; j++) {
                iMoveObs[i][j] = cint(reader.readLine());
            }
        }
        for (int i = 0; i <= 2; i++) {
            int iObKey = iMoveObs[i][0];
            int iMoveTo = iMoveObs[i][1];
            if (iObKey > 0) {
                boolean bNewTask = true;
                if (i == 1 && mLength.iTo == 0 && iDoneTask[0]) {
                    bNewTask = false;
                }
                if (i == 2 &&
                        ((mLength.iTo == 0 && !iDoneTask[1]) || iDoneTask[1])) {
                    bNewTask = false;
                }
                if (bNewTask) {
                    boolean bMultiple = false;
                    if (tas != null) {
                        bMultiple = true;
                        tas.setDescription("Generated task #" +
                                i + " for event " + getDescription());
                    }
                    tas = new MTask(adv);
                    tas.setKey("GenTask" + (adv.mTasks.size() + 1));
                    tas.setDescription("Generated task" +
                            (bMultiple ? " #" + (i + 1) : "") +
                            " for event " + getDescription());
                    tas.setPriority(iStartMaxPriority + adv.mTasks.size() + 1);
                }
                if (i < 2) {
                    iDoneTask[i] = true;
                }
                tas.mType = MTask.TaskTypeEnum.System;
                tas.setRepeatable(true);
                MAction act = new MAction(adv, iObKey, iMoveTo);
                tas.mActions.add(act);
                if (bNewTask) {
                    adv.mTasks.put(tas.getKey(), tas);
                    MSubEvent se = new MSubEvent(adv, tas.getKey());
                    se.eWhat = MSubEvent.WhatEnum.ExecuteTask;
                    se.eWhen = (i == 0) ?
                            MSubEvent.WhenEnum.FromStartOfEvent :
                            MSubEvent.WhenEnum.BeforeEndOfEvent;
                    se.ftTurns.iFrom = 0;
                    se.ftTurns.iTo = 0;
                    se.sKey = tas.getKey();
                    mSubEvents.add(se);
                }
            }
        }
        for (MSubEvent se : mSubEvents) {
            if (se.eWhat == MSubEvent.WhatEnum.DisplayMessage ||
                    se.eWhat == MSubEvent.WhatEnum.SetLook) {
                se.sKey = sLocationKey;
            }
        }

        // =============================================================
        //           EXECUTE A TASK WHEN THE EVENT FINISHES
        // -------------------------------------------------------------
        // You may also want to execute another task when the event
        // finishes. This could be for many reasons, but allows you to
        // use the power of tasks spontaneously. An example could be a
        // gust of wind, which blows the Player from one room to another.
        // The gust of wind could be a random event, but the task the
        // event runs would move the Player or other objects etc.
        //
        // When the task is executed, it executes the exact task
        // selected in the list, even if there are more than one with
        // the same command. This is a change from previous versions
        // where they were executed as though the player typed the
        // command. If the restrictions on the task are not met
        // however, the task will not run. If you want to create a
        // form of IF-THEN-ELSE, you will have to create the task as
        // a “master” task. Get this task to execute a number of other
        // tasks, each sub-task with their own restrictions. Any task
        // that passes the restrictions will execute.
        // -------------------------------------------------------------
        String sExecuteTask = "Task" + reader.readLine();               // #TaskAffected
        if (!sExecuteTask.equals("Task0")) {
            if (v >= 3.9) {
                MSubEvent se = new MSubEvent(adv, getKey());
                se.eWhen = MSubEvent.WhenEnum.BeforeEndOfEvent;
                se.ftTurns.iFrom = 0;
                se.ftTurns.iTo = 0;
                se.eWhat = (iTaskMode == 0) ?
                        MSubEvent.WhatEnum.ExecuteTask :
                        MSubEvent.WhatEnum.UnsetTask;
                se.sKey = sExecuteTask;
                mSubEvents.add(se);
            } else {
                final String exeCmd = adv.mTasks.get(sExecuteTask).mCommands.get(0);
                if (iTaskMode == 0) {
                    // For tasks triggered when an event finishes,
                    // version 3.8 simply runs the specified command,
                    // as though the player had typed it. Task execution
                    // behaves in the same way as the
                    // HighestPriorityPassingTask setting - i.e.
                    // tasks that pass restrictions override higher
                    // priority tasks that do not, even when the failing
                    // higher priority task has output.
                    MSubEvent se = new MSubEvent(adv, getKey());
                    se.eWhen = MSubEvent.WhenEnum.BeforeEndOfEvent;
                    se.ftTurns.iFrom = 0;
                    se.ftTurns.iTo = 0;
                    se.eWhat = MSubEvent.WhatEnum.ExecuteCommand;
                    se.sKey = exeCmd;
                    mSubEvents.add(se);
                } else {
                    for (MTask t : adv.mTasks.values()) {
                        if (t.getIsLibrary()) {
                            break;
                        }
                        for (String cmd : t.mCommands) {
                            if (cmd.equals(exeCmd)) {
                                MSubEvent se = new MSubEvent(adv, getKey());
                                se.eWhen = MSubEvent.WhenEnum.BeforeEndOfEvent;
                                se.ftTurns.iFrom = 0;
                                se.ftTurns.iTo = 0;
                                se.eWhat = MSubEvent.WhatEnum.UnsetTask;
                                se.sKey = t.getKey();
                                mSubEvents.add(se);
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (v >= 3.9) {
            // ---------------------------------------------------------------
            //                RESOURCES ([5]<RESOURCE>Res)
            // ---------------------------------------------------------------
            for (int i = 0; i < 5; i++) {
                MFileOlder.loadResource(adv, reader, v, null);
            }
        }
    }

    public MEvent(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                  boolean bLibrary, boolean bAddDuplicateKeys, double dFileVersion) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Event");

        String s;
        String[] sData;
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        header.processTag(xpp);
                        break;

                    case "Type":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mEventType = MEvent.EventTypeEnum.valueOf(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Description":
                        setDescription(xpp.nextText());
                        break;

                    case "WhenStart":
                        mWhenStart = MEvent.parseWhenStartEnum(xpp.nextText());
                        break;

                    case "Repeating":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setRepeating(getBool(s));
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "RepeatCountdown":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                RepeatCountdown = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "StartDelay":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                sData = s.split(" ");
                                mStartDelay.iFrom = cint(sData[0]);
                                mStartDelay.iTo = (sData.length == 1) ?
                                        cint(sData[0]) : cint(sData[2]);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Length":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                sData = s.split(" ");
                                mLength.iFrom = cint(sData[0]);
                                mLength.iTo = (sData.length == 1) ?
                                        cint(sData[0]) : cint(sData[2]);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Control":
                        mEventControls.add(new MEventOrWalkControl(xpp));
                        break;

                    case "SubEvent":
                        mSubEvents.add(new MSubEvent(adv, xpp, dFileVersion));
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Event");

        if (!header.finalise(this, adv.mEvents,
                bLibrary, bAddDuplicateKeys, null)) {
            throw new Exception();
        }

        for (MSubEvent se : mSubEvents) {
            se.sParentKey = getKey();
        }
    }

    private static WhenStartEnum parseWhenStartEnum(@NonNull String val) {
        try {
            return WhenStartEnum.valueOf(val);
        } catch (IllegalArgumentException e) {
            switch (val) {
                case "0":
                    return WhenStartEnum.NotSet;
                case "1":
                    return WhenStartEnum.Immediately;
                case "2":
                    return WhenStartEnum.BetweenXandYTurns;
                case "3":
                    return WhenStartEnum.AfterATask;
                default:
                    GLKLogger.warn("Didn't recognise WhenStartEnum: " + val);
                    return WhenStartEnum.Immediately;
            }
        }
    }

    @Override
    @NonNull
    public String evaluate(@NonNull String funcName,
                           @NonNull String args,
                           @NonNull String remainder,
                           @NonNull boolean[] resultIsInteger) {
        // There are only two functions for events, both of
        // which return integer values.
        switch (funcName) {
            case "Length":
                // The total number of turns in the event.
                resultIsInteger[0] = true;
                return String.valueOf(mLength.getValue());
            case "Position":
                // The current position of the event
                // (number of turns elapsed since it started).
                resultIsInteger[0] = true;
                return String.valueOf(getTimerFromStartOfEvent());
            case "":
                return getKey();
        }

        return "#*!~#";
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String value) {
        mDescription = value;
    }

    public boolean getRepeating() {
        return mRepeating;
    }

    public void setRepeating(boolean value) {
        mRepeating = value;
    }

    @NonNull
    String getLookText() {
        if (mStatus == StatusEnum.Running) {
            // Pop the first matching LookText off the stack
            boolean bOkToDisplay = false;
            String sLookText = "";

            for (MLookText lt : mStackLookText) {
                if (mAdv.getPlayer().isInGroupOrLocation(lt.sLocationKey)) {
                    sLookText = lt.sDescription;
                    bOkToDisplay = true;
                    break;
                }
            }

            if (bOkToDisplay) {
                return sLookText;
            }
        }
        return "";
    }

    public int getTimerToEndOfEvent() {
        return mTimerToEventOfEvent;
    }

    public void setTimerToEndOfEvent(int value) throws InterruptedException {
        mTimerToEventOfEvent = value;

        // if the timer has ticked down and we're ready to start
        if (mStatus == StatusEnum.CountingDownToStart &&
                getTimerFromStartOfEvent() == 0) {
            start(true);
        }

        // if we've reached the end of the timer
        if (mStatus == StatusEnum.Running && mTimerToEventOfEvent == 0) {
            lStop(true);
        }
    }

    private int getTimerFromLastSubEvent() {
        return getTimerFromStartOfEvent() - mLastSubEventTime;
    }

    private int getTimerFromStartOfEvent() {
        return mLength.getValue() - getTimerToEndOfEvent(); // + 1
    }

    public void start() throws InterruptedException {
        start(false);
    }

    public void start(boolean bForce) throws InterruptedException {
        if (bForce || MParser.mEventsRunning) {
            lStart();
        } else {
            mNextCommand = Command.Start;
        }
    }

    private void lStart() throws InterruptedException {
        lStart(false);
    }

    private void lStart(boolean bRestart) throws InterruptedException {
        if (mStatus == StatusEnum.NotYetStarted ||
                mStatus == StatusEnum.CountingDownToStart ||
                mStatus == StatusEnum.Finished || (mStatus == StatusEnum.Running && bRestart)) {
            if (!bRestart) {
                MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                        MView.DebugDetailLevelEnum.Low, "Starting event " + getDescription());
            }

            mStatus = StatusEnum.Running;
            mLength.reset();

            mLastSubEvent = null;
            mLastSubEventTime = 0;

            for (MSubEvent se : mSubEvents) {
                se.ftTurns.reset();
                if (se.eMeasure == MSubEvent.MeasureEnum.Seconds && mEventType == EventTypeEnum.TurnBased) {
                    se.tmrTrigger = new Timer(true);
                    if (se.eWhen == MSubEvent.WhenEnum.FromStartOfEvent ||
                            (se.eWhen == MSubEvent.WhenEnum.FromLastSubEvent && se == mSubEvents.get(0))) {
                        se.tmrTrigger.schedule(se.new SubEventTimerTask(), se.ftTurns.getValue() * 1000);
                        se.dtStart = new Date();
                    }
                }
            }

            // WHAT TO DO HERE?
            // if it's length 0, we need to run our start actions
            // if it's length 2 we don't want it being set to 1 immediately from the incrementtimer

            setTimerToEndOfEvent(mLength.getValue());
            if (getTimerFromStartOfEvent() == 0) {
                doAnySubEvents();
            } // To run 'after 0 turns' subevents

            if (mWhenStart == WhenStartEnum.Immediately) {
                mWhenStart = WhenStartEnum.BetweenXandYTurns;
            } // So we get 'after 0 turns' on any repeats
            mJustStarted = true;
        } else {
            //Throw New Exception("Can't Start an Event that isn't waiting!")
            MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                    MView.DebugDetailLevelEnum.Error, "Can't Start an Event that isn't waiting!");
        }
    }

    public void pause() {
        if (MParser.mEventsRunning) {
            lPause();
        } else {
            mNextCommand = Command.Pause;
        }
    }

    private void lPause() {
        if (mStatus == StatusEnum.Running) {
            MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                    MView.DebugDetailLevelEnum.Low, "Pausing event " + getDescription());
            mStatus = StatusEnum.Paused;
            for (MSubEvent se : mSubEvents) {
                if (se.tmrTrigger != null) {
                    se.iMilliseconds = se.ftTurns.getValue() * 1000 - (int) ((new Date()).getTime() - se.dtStart.getTime());
                    se.tmrTrigger.cancel();
                }
            }
        } else {
            MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                    MView.DebugDetailLevelEnum.Error, "Can't Pause an Event that isn't running!");
        }
    }

    public void resume() {
        if (MParser.mEventsRunning) {
            lResume();
        } else {
            mNextCommand = Command.Resume;
        }
    }

    private void lResume() {
        if (mStatus == StatusEnum.Paused) {
            MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                    MView.DebugDetailLevelEnum.Low, "Resuming event " + getDescription());
            mStatus = StatusEnum.Running;
            for (MSubEvent se : mSubEvents) {
                if (se.tmrTrigger != null) {
                    if (se.iMilliseconds > 0) {
                        int interval = se.iMilliseconds;
                        se.iMilliseconds = 0;
                        se.tmrTrigger.schedule(se.new SubEventTimerTask(), interval);
                    }
                }
            }
        } else {
            MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                    MView.DebugDetailLevelEnum.Error, "Can't Resume an Event that isn't paused!");
        }
    }

    public void stop() throws InterruptedException {
        // if an event runs a task and that task starts/stops an event, do it immediately
        if (MParser.mEventsRunning) {
            lStop();
        } else {
            mNextCommand = Command.Stop;
        }
    }

    private void lStop() throws InterruptedException {
        lStop(false);
    }

    private void lStop(boolean bRunSubEvents) throws InterruptedException {
        if (bRunSubEvents) {
            doAnySubEvents();
        }
        if (mStatus == StatusEnum.Paused) {
            return;
        }
        mStatus = StatusEnum.Finished;
        for (MSubEvent se : mSubEvents) {
            if (se.tmrTrigger != null) {
                se.tmrTrigger.cancel();
            }
        }
        if (getRepeating() && getTimerToEndOfEvent() == 0) {
            if (mLength.getValue() > 0) {
                MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                        MView.DebugDetailLevelEnum.Low, "Restarting event " + getDescription());
                if (RepeatCountdown) {
                    mStatus = StatusEnum.CountingDownToStart;
                    mStartDelay.reset();
                    setTimerToEndOfEvent(mStartDelay.getValue() + mLength.getValue());
                } else {
                    lStart(true); // Make sure we don't get ourselves in a loop for zero length events
                }
            } else {
                MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                        MView.DebugDetailLevelEnum.Low,
                        "Not restarting event " + getDescription() + " otherwise we'd get in an infinite loop as zero length.");
            }
        } else {
            MView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                    MView.DebugDetailLevelEnum.Low, "Finishing event " + getDescription());
        }
    }

    public void incrementTimer() throws InterruptedException {
        if (mNextCommand != Command.Nothing) {
            switch (mNextCommand) {
                case Start:
                    lStart();
                    break;
                case Stop:
                    lStop();
                    break;
                case Pause:
                    lPause();
                    break;
                case Resume:
                    lResume();
                    break;
            }
            mNextCommand = Command.Nothing;
            mTriggeringTask = "";
        }

        if (mStatus == StatusEnum.Running || mStatus == StatusEnum.CountingDownToStart) {
            MView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                    MView.DebugDetailLevelEnum.High,
                    "Event " + getDescription() + " [" + (getTimerFromStartOfEvent() + 1) + "/" + mLength.getValue() + "]");
        }

        // Why are we running subevents before we've incremented the timer?
        // Split this into 2 case statements, as changing timer here may change status
        switch (mStatus) {
            case NotYetStarted:
            case CountingDownToStart:
                setTimerToEndOfEvent(getTimerToEndOfEvent() - 1);
                break;
            case Running:
                if (!mJustStarted) {
                    setTimerToEndOfEvent(getTimerToEndOfEvent() - 1);
                }
                break;
            case Paused:
            case Finished:
                break;
        }

        if (!mJustStarted) {
            doAnySubEvents();
        }
        mJustStarted = false;
    }

    private void doAnySubEvents() throws InterruptedException {
        switch (mStatus) {
            case Running:
                // Check all the subevents to see if we need to do anything
                int iIndex = 0;
                for (MSubEvent se : mSubEvents) {
                    if (se.eMeasure == MSubEvent.MeasureEnum.Turns || mEventType == EventTypeEnum.TimeBased) {
                        boolean bRunSubEvent = false;
                        switch (se.eWhen) {
                            case FromStartOfEvent:
                                if (getTimerFromStartOfEvent() == se.ftTurns.getValue() &&
                                        se.ftTurns.getValue() <= mLength.getValue() &&
                                        (se.ftTurns.getValue() > 0 || this.mWhenStart != WhenStartEnum.Immediately)) {
                                    bRunSubEvent = true;
                                }
                                break;

                            case FromLastSubEvent:
                                if (getTimerFromLastSubEvent() == se.ftTurns.getValue()) {
                                    if ((mLastSubEvent == null && iIndex == 0) || (iIndex > 0 && mLastSubEvent == mSubEvents.get(iIndex - 1))) {
                                        bRunSubEvent = true;
                                    }
                                }
                                break;

                            case BeforeEndOfEvent:
                                if (getTimerToEndOfEvent() == se.ftTurns.getValue()) {
                                    bRunSubEvent = true;
                                }
                                break;
                        }

                        if (bRunSubEvent) {
                            runSubEvent(se);
                        }
                    }
                    iIndex++;
                }
                break;
        }
    }

    void runSubEvent(@NonNull MSubEvent se) throws InterruptedException {
        switch (se.eWhat) {
            case DisplayMessage:
                if (mAdv.getPlayer().isInGroupOrLocation(se.sKey)) {
                    MView.displayText(mAdv, se.oDescription.toString());
                }
                break;
            case ExecuteTask:
                if (mAdv.mTasks.containsKey(se.sKey)) {
                    MView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                            MView.DebugDetailLevelEnum.Medium,
                            "Event '" + getDescription() + "' attempting to execute task '" +
                                    mAdv.mTasks.get(se.sKey).getDescription() + "'");
                    EnumSet<MTask.ExecutionStatus> curStatus = EnumSet.noneOf(MTask.ExecutionStatus.class);
                    MTask tas = mAdv.mTasks.get(se.sKey);
                    tas.attemptToExecute(true, false,
                            curStatus, false, false, true);
                }
                break;
            case ExecuteCommand:
                // This is deprecated and only included to provide
                // compatibility for v3.8 games.
                MView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                        MView.DebugDetailLevelEnum.Medium, "Event '" + getDescription() +
                                "' executing command '" + se.sKey + "'");
                MTaskHashMap.MTaskMatchResult match =
                        mAdv.mTasks.find(se.sKey, 0, null);
                if (match.mTask != null) {
                    // Try to execute it
                    match.mTask.attemptToExecute(true);
                }
                break;
            case SetLook:
                // Push a LookText onto the stack
                mStackLookText.push(new MLookText(se.sKey, se.oDescription.toString()));
                break;
            case UnsetTask:
                MView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                        MView.DebugDetailLevelEnum.Medium,
                        "Event '" + getDescription() + "' unsetting task '" + mAdv.mTasks.get(se.sKey).getDescription() + "'");
                mAdv.mTasks.get(se.sKey).setCompleted(false);
                break;
        }
        mLastSubEventTime = getTimerFromStartOfEvent();
        mLastSubEvent = se;

        int i = 0;
        for (MSubEvent ose : mSubEvents) {
            i++;
            if (ose == se) {
                if (i < mSubEvents.size()) {
                    MSubEvent sei = mSubEvents.get(i);
                    if (sei.eMeasure == MSubEvent.MeasureEnum.Seconds && mEventType == EventTypeEnum.TurnBased) {
                        if (sei.eWhen == MSubEvent.WhenEnum.FromLastSubEvent) {
                            sei.tmrTrigger = new Timer(true);
                            if (sei.ftTurns.getValue() > 0) {
                                sei.tmrTrigger.schedule(se.new SubEventTimerTask(), sei.ftTurns.getValue() * 1000);
                                sei.dtStart = new Date();
                            } else {
                                runSubEvent(sei);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> all = new ArrayList<>();
        for (MSubEvent se : mSubEvents) {
            all.add(se.oDescription);
        }
        return all;
    }

    @Nullable
    @Override
    public MItem clone() {
        MEvent ev = (MEvent) super.clone();
        if (ev != null) {
            ev.mStartDelay = ev.mStartDelay.clone();
            ev.mLength = ev.mLength.clone();
        }
        return ev;
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getDescription();
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (int i = mEventControls.size() - 1; i >= 0; i--) {
            if (mEventControls.get(i).sTaskKey.equals(key)) {
                mEventControls.remove(i);
            }
        }
        for (MSubEvent SubEvent : mSubEvents) {
            if (!SubEvent.oDescription.deleteKey(key)) {
                return false;
            }
            if (SubEvent.sKey.equals(key)) {
                SubEvent.sKey = "";
            }
        }
        return true;
    }

    @Override
    protected int findLocal(@NonNull String toFind, @Nullable String toReplace,
                            boolean findAll, @NonNull int[] nReplaced) {
        int iCount = nReplaced[0];
        String[] t = new String[1];
        t[0] = getDescription();
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        setDescription(t[0]);
        return nReplaced[0] - iCount;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int iCount = 0;
        for (MEventOrWalkControl EventControl : mEventControls) {
            if (EventControl.sTaskKey.equals(key)) {
                iCount++;
            }
        }
        for (MSubEvent SubEvent : mSubEvents) {
            iCount += SubEvent.oDescription.referencesKey(key);
            if (SubEvent.sKey.equals(key)) {
                iCount++;
            }
        }
        return iCount;
    }

    public enum EventTypeEnum {
        TurnBased,              // 0
        TimeBased               // 1
    }

    public enum WhenStartEnum {
        NotSet,                 // 0 (default added for Bebek)
        Immediately,            // 1
        BetweenXandYTurns,      // 2
        AfterATask              // 3
    }

    private enum Command {
        Nothing,                // 0
        Start,                  // 1
        Stop,                   // 2
        Pause,                  // 3
        Resume                  // 4
    }

    public enum StatusEnum {
        NotYetStarted,          // 0
        Running,                // 1
        CountingDownToStart,    // 2
        Paused,                 // 3
        Finished                // 4
    }

    private static class MLookText {
        String sDescription;
        String sLocationKey;

        MLookText(@NonNull String sKey, @NonNull String sDescription) {
            this.sLocationKey = sKey;
            this.sDescription = sDescription;
        }
    }
}
