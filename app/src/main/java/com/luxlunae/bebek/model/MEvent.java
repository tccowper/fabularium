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
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TimeBased;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TurnBased;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.CountingDownToStart;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.Finished;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.NotYetStarted;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.Paused;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.Running;
import static com.luxlunae.bebek.model.MEvent.WhenStartEnum.BetweenXandYTurns;
import static com.luxlunae.bebek.model.MEvent.WhenStartEnum.Immediately;
import static com.luxlunae.bebek.model.MEvent.WhenStartEnum.NotSet;
import static com.luxlunae.bebek.model.MEventOrWalkControl.CompleteOrNotEnum.Completion;
import static com.luxlunae.bebek.model.MEventOrWalkControl.CompleteOrNotEnum.UnCompletion;
import static com.luxlunae.bebek.model.MEventOrWalkControl.MControlEnum.Resume;
import static com.luxlunae.bebek.model.MEventOrWalkControl.MControlEnum.Start;
import static com.luxlunae.bebek.model.MEventOrWalkControl.MControlEnum.Suspend;
import static com.luxlunae.bebek.model.MSubEvent.MeasureEnum.Seconds;
import static com.luxlunae.bebek.model.MSubEvent.MeasureEnum.Turns;
import static com.luxlunae.bebek.model.MSubEvent.WhatEnum.DisplayMessage;
import static com.luxlunae.bebek.model.MSubEvent.WhatEnum.ExecuteCommand;
import static com.luxlunae.bebek.model.MSubEvent.WhatEnum.ExecuteTask;
import static com.luxlunae.bebek.model.MSubEvent.WhatEnum.SetLook;
import static com.luxlunae.bebek.model.MSubEvent.WhatEnum.UnsetTask;
import static com.luxlunae.bebek.model.MSubEvent.WhenEnum.BeforeEndOfEvent;
import static com.luxlunae.bebek.model.MSubEvent.WhenEnum.FromLastSubEvent;
import static com.luxlunae.bebek.model.MSubEvent.WhenEnum.FromStartOfEvent;
import static com.luxlunae.bebek.model.io.MFileOlder.convertV4FuncsToV5;
import static com.luxlunae.bebek.model.io.MFileOlder.getRoomGroupFromList;
import static com.luxlunae.bebek.model.io.MFileOlder.loadResource;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.High;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Low;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;
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
    public EventTypeEnum mEventType = TurnBased;
    public int mLastSubEventTime = 0;
    public StatusEnum mStatus = NotYetStarted;
    @NonNull
    public MFromTo mStartDelay;
    @NonNull
    public MFromTo mLength;
    @Nullable
    public MSubEvent mLastSubEvent;
    boolean mJustStarted = false;
    WhenStartEnum mWhenStart = NotSet;
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
                  @NonNull MFileOlder.V4Reader reader,
                  int iEvent, int startMaxPriority,
                  int nLocs, double v) throws EOFException {
        // ADRIFT V3.80, 3.90 and V4 Loader
        this(adv);

        String locKey = "";
        setKey("Event" + iEvent);
        setDescription(reader.readLine());                                  // $Short
        mWhenStart = MEvent.parseWhenStartEnum(reader.readLine());          // #StarterType
        if (mWhenStart == BetweenXandYTurns) {         // ?#StarterType=2:
            mStartDelay.iFrom = cint(reader.readLine()) - 1;                //   #StartTime
            mStartDelay.iTo = cint(reader.readLine()) - 1;                  //   #EndTime
        }
        if (mWhenStart == MEvent.WhenStartEnum.AfterATask) {                // ?#StarterType=3:
            String sStartTask = "Task" + reader.readLine();                 //  #TaskNum
            MEventOrWalkControl ec = new MEventOrWalkControl();
            ec.eControl = Start;
            ec.mTaskKey = sStartTask;
            mEventControls.add(ec);
        }
        setRepeating(cbool(reader.readLine()));                             // #RestartType
        int taskMode = cint(reader.readLine());                            // BTaskFinished
        mLength.iFrom = cint(reader.readLine());                            // #Time1
        mLength.iTo = cint(reader.readLine());                              // #Time2
        if (mWhenStart == BetweenXandYTurns) {
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
        String buf = reader.readLine();                                 // $StartText
        if (!buf.equals("")) {
            MSubEvent se = new MSubEvent(adv, getKey());
            se.mWhat = DisplayMessage;
            se.mWhen = FromStartOfEvent;
            se.mTurns.iFrom = 0;
            se.mTurns.iTo = 0;
            se.mDescription = new MDescription(adv,
                    convertV4FuncsToV5(adv, buf));
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
        buf = reader.readLine();                                        // $LookText
        if (!buf.equals("")) {
            MSubEvent se = new MSubEvent(adv, getKey());
            se.mWhat = SetLook;
            se.mWhen = FromStartOfEvent;
            se.mTurns.iFrom = 0;
            se.mTurns.iTo = 0;
            se.mDescription = new MDescription(adv,
                    convertV4FuncsToV5(adv, buf));
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
        String endMsg = reader.readLine();                             // $FinishText
        if (!endMsg.equals("")) {
            MSubEvent se = new MSubEvent(adv, getKey());
            se.mWhat = DisplayMessage;
            se.mWhen = BeforeEndOfEvent;
            se.mTurns.iFrom = 0;
            se.mTurns.iTo = 0;
            se.mDescription = new MDescription(adv,
                    convertV4FuncsToV5(adv, endMsg));
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
        int whichRooms = cint(reader.readLine());                          // #Type
        switch (whichRooms) {
            case 0:
                // No rooms
                locKey = "";
                break;
            case 1:
                // Single Room
                locKey = "Location" +
                        (cint(reader.readLine()) + 1);                      // Room #
                break;
            case 2:
                // Multiple Rooms
                // Create a room group containing them
                boolean showRoom;
                MStringArrayList showInRooms = new MStringArrayList();
                for (int i = 1; i <= nLocs; i++) {
                    showRoom = cbool(reader.readLine());                   // iIsHere
                    if (showRoom) {
                        showInRooms.add("Location" + i);
                    }
                }
                locKey = getRoomGroupFromList(adv, showInRooms,
                        "event '" + getDescription() + "'").getKey();
                break;
            case 3:
                // All Rooms
                locKey = ALLROOMS;
                break;
        }

        for (int i = 0; i <= 1; i++) {
            // --------------------------------------------------------
            // You can pause or resume your event depending upon
            // whether a task has completed or not. Up to 2.
            // --------------------------------------------------------
            int taskNum = cint(reader.readLine());                       // #PauseTask / #ResumeTask
            int unComplete = cint(reader.readLine());                    // BPauserCompleted / BResumerCompleted
            if (taskNum > 0) {
                MEventOrWalkControl ec = new MEventOrWalkControl();
                ec.eControl = (i == 0) ?
                        Suspend : Resume;
                ec.mTaskKey = "Task" + (taskNum - 1);
                ec.eCompleteOrNot = (unComplete == 0) ?
                        Completion : UnCompletion;
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
            int from = cint(reader.readLine());                          // #PrefTime1 / #PrefTime2
            buf = reader.readLine();                                     // $PrefText1 / $PrefText2
            if (!buf.equals("")) {
                MSubEvent se = new MSubEvent(adv, getKey());
                se.mWhat = DisplayMessage;
                se.mWhen = BeforeEndOfEvent;
                se.mTurns.iFrom = from;
                se.mTurns.iTo = from;
                se.mDescription = new MDescription(adv,
                        convertV4FuncsToV5(adv, buf));
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
        boolean[] doneTask = new boolean[2];
        int[][] moveObs = new int[3][2];
        for (int i : new int[]{1, 2, 0}) {
            for (int j = 0; j <= 1; j++) {
                moveObs[i][j] = cint(reader.readLine());
            }
        }
        for (int i = 0; i <= 2; i++) {
            int obKey = moveObs[i][0];
            int moveTo = moveObs[i][1];
            if (obKey > 0) {
                boolean isNewTask = true;
                if (i == 1 && mLength.iTo == 0 && doneTask[0]) {
                    isNewTask = false;
                }
                if (i == 2 && (mLength.iTo == 0 || doneTask[1])) {
                    isNewTask = false;
                }
                if (isNewTask) {
                    boolean isMultiple = false;
                    if (tas != null) {
                        isMultiple = true;
                        tas.setDescription("Generated task #" +
                                i + " for event " + getDescription());
                    }
                    tas = new MTask(adv);
                    tas.setKey("GenTask" + (adv.mTasks.size() + 1));
                    tas.setDescription("Generated task" +
                            (isMultiple ? " #" + (i + 1) : "") +
                            " for event " + getDescription());
                    tas.setPriority(startMaxPriority + adv.mTasks.size() + 1);
                }
                if (i < 2) {
                    doneTask[i] = true;
                }
                tas.mType = MTask.TaskTypeEnum.System;
                tas.setRepeatable(true);
                MAction act = new MAction(adv, obKey, moveTo);
                tas.mActions.add(act);
                if (isNewTask) {
                    adv.mTasks.put(tas.getKey(), tas);
                    MSubEvent se = new MSubEvent(adv, tas.getKey());
                    se.mWhat = ExecuteTask;
                    se.mWhen = (i == 0) ?
                            FromStartOfEvent : BeforeEndOfEvent;
                    se.mTurns.iFrom = 0;
                    se.mTurns.iTo = 0;
                    se.mKey = tas.getKey();
                    mSubEvents.add(se);
                }
            }
        }
        for (MSubEvent se : mSubEvents) {
            if (se.mWhat == DisplayMessage ||
                    se.mWhat == SetLook) {
                se.mKey = locKey;
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
        String execTask = "Task" + reader.readLine();               // #TaskAffected
        if (!execTask.equals("Task0")) {
            if (v >= 3.9) {
                MSubEvent se = new MSubEvent(adv, getKey());
                se.mWhen = BeforeEndOfEvent;
                se.mTurns.iFrom = 0;
                se.mTurns.iTo = 0;
                se.mWhat = (taskMode == 0) ?
                        ExecuteTask : UnsetTask;
                se.mKey = execTask;
                mSubEvents.add(se);
            } else {
                final String exeCmd = adv.mTasks.get(execTask).mCommands.get(0);
                if (taskMode == 0) {
                    // For tasks triggered when an event finishes,
                    // version 3.8 simply runs the specified command,
                    // as though the player had typed it. Task execution
                    // behaves in the same way as the
                    // HighestPriorityPassingTask setting - i.e.
                    // tasks that pass restrictions override higher
                    // priority tasks that do not, even when the failing
                    // higher priority task has output.
                    MSubEvent se = new MSubEvent(adv, getKey());
                    se.mWhen = BeforeEndOfEvent;
                    se.mTurns.iFrom = 0;
                    se.mTurns.iTo = 0;
                    se.mWhat = ExecuteCommand;
                    se.mKey = exeCmd;
                    mSubEvents.add(se);
                } else {
                    for (MTask t : adv.mTasks.values()) {
                        if (t.getIsLibrary()) {
                            break;
                        }
                        for (String cmd : t.mCommands) {
                            if (cmd.equals(exeCmd)) {
                                MSubEvent se = new MSubEvent(adv, getKey());
                                se.mWhen = BeforeEndOfEvent;
                                se.mTurns.iFrom = 0;
                                se.mTurns.iTo = 0;
                                se.mWhat = UnsetTask;
                                se.mKey = t.getKey();
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
                loadResource(adv, reader, v, null);
            }
        }
    }

    public MEvent(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                  boolean isLibrary, boolean addDupKeys, double version) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Event");

        String s;
        String[] sData;
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT &&
                xpp.getDepth() > depth) {
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
                        mSubEvents.add(new MSubEvent(adv, xpp, version));
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Event");

        if (!header.finalise(this, adv.mEvents,
                isLibrary, addDupKeys, null)) {
            throw new Exception();
        }

        for (MSubEvent se : mSubEvents) {
            se.mParentKey = getKey();
        }
    }

    private static WhenStartEnum parseWhenStartEnum(@NonNull String val) {
        try {
            return WhenStartEnum.valueOf(val);
        } catch (IllegalArgumentException e) {
            switch (val) {
                case "0":
                    return NotSet;
                case "1":
                    return Immediately;
                case "2":
                    return BetweenXandYTurns;
                case "3":
                    return WhenStartEnum.AfterATask;
                default:
                    GLKLogger.warn("Didn't recognise WhenStartEnum: " + val);
                    return Immediately;
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
        if (mStatus == Running) {
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
        if (mStatus == CountingDownToStart &&
                getTimerFromStartOfEvent() == 0) {
            start(true);
        }

        // if we've reached the end of the timer
        if (mStatus == Running && mTimerToEventOfEvent == 0) {
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
        if (bForce || mAdv.mEventsRunning) {
            lStart();
        } else {
            mNextCommand = Command.Start;
        }
    }

    private void lStart() throws InterruptedException {
        lStart(false);
    }

    private void lStart(boolean restart) throws InterruptedException {
        if (mStatus == NotYetStarted ||
                mStatus == CountingDownToStart ||
                mStatus == Finished || (mStatus == Running && restart)) {
            if (!restart) {
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Event,
                        getKey(), Low, "Starting event " + getDescription());
            }

            mStatus = Running;
            mLength.reset();

            mLastSubEvent = null;
            mLastSubEventTime = 0;

            for (MSubEvent se : mSubEvents) {
                se.mTurns.reset();
                if (se.mMeasure == Seconds && mEventType == TurnBased) {
                    se.mTrigger = new Timer(true);
                    if (se.mWhen == FromStartOfEvent ||
                            (se.mWhen == FromLastSubEvent && se == mSubEvents.get(0))) {
                        se.mTrigger.schedule(se.new SubEventTimerTask(),
                                se.mTurns.getValue() * 1000);
                        se.mStart = new Date();
                    }
                }
            }

            // WHAT TO DO HERE?
            // if it's length 0, we need to run our start actions
            // if it's length 2 we don't want it being set to 1
            // immediately from the incrementtimer
            setTimerToEndOfEvent(mLength.getValue());
            if (getTimerFromStartOfEvent() == 0) {
                doAnySubEvents();
            } // To run 'after 0 turns' subevents

            if (mWhenStart == Immediately) {
                mWhenStart = BetweenXandYTurns;
            } // So we get 'after 0 turns' on any repeats
            mJustStarted = true;
        } else {
            //Throw New Exception("Can't Start an Event that isn't waiting!")
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                    MView.DebugDetailLevelEnum.Error,
                    "Can't Start an Event that isn't waiting!");
        }
    }

    public void pause() {
        if (mAdv.mEventsRunning) {
            lPause();
        } else {
            mNextCommand = Command.Pause;
        }
    }

    private void lPause() {
        if (mStatus == Running) {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                    Low, "Pausing event " + getDescription());
            mStatus = Paused;
            for (MSubEvent se : mSubEvents) {
                if (se.mTrigger != null) {
                    se.mMilliseconds = se.mTurns.getValue() * 1000 -
                            (int) ((new Date()).getTime() - se.mStart.getTime());
                    se.mTrigger.cancel();
                }
            }
        } else {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                    MView.DebugDetailLevelEnum.Error,
                    "Can't Pause an Event that isn't running!");
        }
    }

    public void resume() {
        if (mAdv.mEventsRunning) {
            lResume();
        } else {
            mNextCommand = Command.Resume;
        }
    }

    private void lResume() {
        if (mStatus == Paused) {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                    Low, "Resuming event " + getDescription());
            mStatus = Running;
            for (MSubEvent se : mSubEvents) {
                if (se.mTrigger != null) {
                    if (se.mMilliseconds > 0) {
                        int interval = se.mMilliseconds;
                        se.mMilliseconds = 0;
                        se.mTrigger.schedule(se.new SubEventTimerTask(), interval);
                    }
                }
            }
        } else {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                    MView.DebugDetailLevelEnum.Error,
                    "Can't Resume an Event that isn't paused!");
        }
    }

    public void stop() throws InterruptedException {
        // if an event runs a task and that task starts/stops an event, do it immediately
        if (mAdv.mEventsRunning) {
            lStop();
        } else {
            mNextCommand = Command.Stop;
        }
    }

    private void lStop() throws InterruptedException {
        lStop(false);
    }

    private void lStop(boolean runSubEvents) throws InterruptedException {
        if (runSubEvents) {
            doAnySubEvents();
        }
        if (mStatus == Paused) {
            return;
        }
        mStatus = Finished;
        for (MSubEvent se : mSubEvents) {
            if (se.mTrigger != null) {
                se.mTrigger.cancel();
            }
        }
        if (getRepeating() && getTimerToEndOfEvent() == 0) {
            if (mLength.getValue() > 0) {
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                        Low, "Restarting event " + getDescription());
                if (RepeatCountdown) {
                    mStatus = CountingDownToStart;
                    mStartDelay.reset();
                    setTimerToEndOfEvent(mStartDelay.getValue() + mLength.getValue());
                } else {
                    // Make sure we don't get ourselves in a
                    // loop for zero length events:
                    lStart(true);
                }
            } else {
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                        Low,
                        "Not restarting event " + getDescription() +
                                " otherwise we'd get in an infinite loop as zero length.");
            }
        } else {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, this.getKey(),
                    Low, "Finishing event " + getDescription());
        }
    }

    void incrementTimer() throws InterruptedException {
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

        if (mStatus == Running || mStatus == CountingDownToStart) {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                    High, "Event " + getDescription() + " [" +
                            (getTimerFromStartOfEvent() + 1) +
                            "/" + mLength.getValue() + "]");
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
                int i = 0;
                for (MSubEvent se : mSubEvents) {
                    if (se.mMeasure == Turns || mEventType == TimeBased) {
                        boolean runSubEvent = false;
                        switch (se.mWhen) {
                            case FromStartOfEvent:
                                if (getTimerFromStartOfEvent() == se.mTurns.getValue() &&
                                        se.mTurns.getValue() <= mLength.getValue() &&
                                        (se.mTurns.getValue() > 0 || mWhenStart != Immediately)) {
                                    runSubEvent = true;
                                }
                                break;

                            case FromLastSubEvent:
                                if (getTimerFromLastSubEvent() == se.mTurns.getValue()) {
                                    if ((mLastSubEvent == null && i == 0) || (i > 0 &&
                                            mLastSubEvent == mSubEvents.get(i - 1))) {
                                        runSubEvent = true;
                                    }
                                }
                                break;

                            case BeforeEndOfEvent:
                                if (getTimerToEndOfEvent() == se.mTurns.getValue()) {
                                    runSubEvent = true;
                                }
                                break;
                        }
                        if (runSubEvent) {
                            runSubEvent(se);
                        }
                    }
                    i++;
                }
                break;
        }
    }

    void runSubEvent(@NonNull MSubEvent se) throws InterruptedException {
        switch (se.mWhat) {
            case DisplayMessage:
                if (mAdv.getPlayer().isInGroupOrLocation(se.mKey)) {
                    mAdv.mView.displayText(mAdv, se.mDescription.toString());
                }
                break;
            case ExecuteTask:
                if (mAdv.mTasks.containsKey(se.mKey)) {
                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                            Medium, "Event '" + getDescription() +
                                    "' attempting to execute task '" +
                                    mAdv.mTasks.get(se.mKey).getDescription() + "'");
                    EnumSet<MTask.ExecutionStatus> curStatus = EnumSet.noneOf(MTask.ExecutionStatus.class);
                    MTask tas = mAdv.mTasks.get(se.mKey);
                    tas.attemptToExecute(true, false,
                            curStatus, false, false, true);
                }
                break;
            case ExecuteCommand:
                // This is deprecated and only included to provide
                // compatibility for v3.8 games.
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                        Medium, "Event '" + getDescription() +
                                "' executing command '" + se.mKey + "'");
                MTaskHashMap.MTaskMatchResult match =
                        mAdv.mTasks.find(se.mKey, 0, null);
                if (match.mTask != null) {
                    // Try to execute it
                    match.mTask.attemptToExecute(true);
                }
                break;
            case SetLook:
                // Push a LookText onto the stack
                mStackLookText.push(new MLookText(se.mKey, se.mDescription.toString()));
                break;
            case UnsetTask:
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, getKey(),
                        Medium,
                        "Event '" + getDescription() + "' unsetting task '" + mAdv.mTasks.get(se.mKey).getDescription() + "'");
                mAdv.mTasks.get(se.mKey).setCompleted(false);
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
                    if (sei.mMeasure == Seconds && mEventType == TurnBased) {
                        if (sei.mWhen == FromLastSubEvent) {
                            sei.mTrigger = new Timer(true);
                            if (sei.mTurns.getValue() > 0) {
                                sei.mTrigger.schedule(se.new SubEventTimerTask(), sei.mTurns.getValue() * 1000);
                                sei.mStart = new Date();
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
            all.add(se.mDescription);
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
            if (mEventControls.get(i).mTaskKey.equals(key)) {
                mEventControls.remove(i);
            }
        }
        for (MSubEvent SubEvent : mSubEvents) {
            if (!SubEvent.mDescription.deleteKey(key)) {
                return false;
            }
            if (SubEvent.mKey.equals(key)) {
                SubEvent.mKey = "";
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
            if (EventControl.mTaskKey.equals(key)) {
                iCount++;
            }
        }
        for (MSubEvent SubEvent : mSubEvents) {
            iCount += SubEvent.mDescription.getNumberOfKeyRefs(key);
            if (SubEvent.mKey.equals(key)) {
                iCount++;
            }
        }
        return iCount;
    }

    @NonNull
    @Override
    public String getSymbol() {
        // hour glass
        return "\u231b";
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
