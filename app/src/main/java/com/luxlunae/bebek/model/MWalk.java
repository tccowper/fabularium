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
import com.luxlunae.bebek.view.MView;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.util.ArrayList;

import static com.luxlunae.bebek.MGlobals.HIDDEN;
import static com.luxlunae.bebek.MGlobals.THEPLAYER;
import static com.luxlunae.bebek.MGlobals.getBool;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.Hidden;
import static com.luxlunae.bebek.model.MCharacter.PKEY_CHAR_ENTERS;
import static com.luxlunae.bebek.model.MCharacter.PKEY_CHAR_EXITS;
import static com.luxlunae.bebek.model.MCharacter.PKEY_SHOW_ENTER_EXIT;
import static com.luxlunae.bebek.model.MWalk.Command.Nothing;
import static com.luxlunae.bebek.model.MWalk.Command.Pause;
import static com.luxlunae.bebek.model.MWalk.Command.Restart;
import static com.luxlunae.bebek.model.MWalk.Command.Start;
import static com.luxlunae.bebek.model.MWalk.Command.Stop;
import static com.luxlunae.bebek.model.MWalk.StatusEnum.Finished;
import static com.luxlunae.bebek.model.MWalk.StatusEnum.NotYetStarted;
import static com.luxlunae.bebek.model.MWalk.StatusEnum.Paused;
import static com.luxlunae.bebek.model.MWalk.StatusEnum.Running;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.High;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Low;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MWalk {

    public int mTimerToEndWalk;
    public StatusEnum mStatus = NotYetStarted;
    @NonNull
    public ArrayList<MStep> mSteps = new ArrayList<>();
    @NonNull
    public ArrayList<MSubWalk> mSubWalks = new ArrayList<>();
    @NonNull
    public ArrayList<MEventOrWalkControl> mWalkControls = new ArrayList<>();
    @NonNull
    String mCharKey = "";            // This is the key of the character
    @NonNull
    String mTriggeringTask = "";
    public boolean mJustStarted = false;
    @NonNull
    private String mDescription = "";
    private boolean mIsLooping;
    private boolean mStartsActive;
    private int mLastSubwalkTime = 0;
    private Command mNextCommand = Nothing;
    @Nullable
    private MSubWalk mLastSubwalk;
    @NonNull
    private final MAdventure mAdv;

    public MWalk(@NonNull MAdventure adv) {
        mAdv = adv;
    }

    public MWalk(@NonNull MAdventure adv, @NonNull XmlPullParser xpp, double dFileVersion) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "Walk");

        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    case "Description":
                        setDescription(xpp.nextText());
                        break;

                    case "Loops":
                        setLoops(getBool(xpp.nextText()));
                        break;

                    case "StartActive":
                        setStartActive(getBool(xpp.nextText()));
                        break;

                    case "Step":
                        mSteps.add(new MWalk.MStep(adv, xpp, dFileVersion));
                        break;

                    case "Control":
                        mWalkControls.add(new MEventOrWalkControl(xpp));
                        break;

                    case "Activity":
                        mSubWalks.add(new MSubWalk(adv, xpp, dFileVersion));
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Walk");
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String value) {
        mDescription = value;
    }

    public boolean getLoops() {
        return mIsLooping;
    }

    public void setLoops(boolean value) {
        mIsLooping = value;
    }

    public boolean getStartActive() {
        return mStartsActive;
    }

    void setStartActive(boolean value) {
        mStartsActive = value;
    }

    @NonNull
    String getDefaultDescription() {

        StringBuilder sb = new StringBuilder();

        for (MStep stp : mSteps) {
            switch (stp.mLocation) {
                case "Hidden":
                    sb.append("Hidden");
                    break;
                default:
                    String sDescription = mAdv.getNameFromKey(stp.mLocation);
                    if (mAdv.mCharacters.containsKey(stp.mLocation)) {
                        sDescription = "Follow " + sDescription;
                    }
                    if (sDescription != null && !sDescription.equals("")) {
                        sb.append(sDescription);
                    } else {
                        // Groups may not be defined yet
                        sb.append("<").append(stp.mLocation).append(">");
                    }
            }
            if (stp.mTurns.mFrom == stp.mTurns.mTo) {
                sb.append(" [").append(stp.mTurns.mFrom).append("]");
            } else {
                sb.append(" [").append(stp.mTurns.mFrom).append(" - ").append(stp.mTurns.mTo).append("]");
            }

            if (stp != mSteps.get(mSteps.size() - 1)) {
                sb.append(" -> ");
            }
        }

        if (mIsLooping) {
            sb.append(" {L}");
        }

        if (sb.toString().equals("")) {
            sb.append("Unnamed walk");
        }

        return sb.toString();
    }

    private void resetLength() {
        for (MStep step : mSteps) {
            step.mTurns.reset();
        }
    }

    private int getLength() {
        int iLength = 0;
        for (MStep step : mSteps) {
            iLength += step.mTurns.getValue();
        }
        return iLength;
    }

    private int getTimerToEndOfWalk() {
        return mTimerToEndWalk;
    }

    private void setTimerToEndOfWalk(int value) throws InterruptedException {
        mTimerToEndWalk = value;

        // If we've reached the end of the timer
        if (mStatus == Running && mTimerToEndWalk == 0) {
            lStop(true, true);
        }
    }

    private int getTimerFromLastSubWalk() {
        return getTimerFromStartOfWalk() - mLastSubwalkTime;
    }

    private int getTimerFromStartOfWalk() {
        return getLength() - getTimerToEndOfWalk();
    }

    public void start() throws InterruptedException {
        start(false);
    }

    public void start(boolean bForce) throws InterruptedException {
        if (bForce) {
            lStart();
        } else {
            mNextCommand =
                    (mNextCommand == Stop) ? Restart : Start;
        }
    }

    private void lStart() throws InterruptedException {
        lStart(false);
    }

    private void lStart(boolean bRestart) throws InterruptedException {
        if (mStatus == NotYetStarted || mStatus == Finished ||
                (mStatus == Running && bRestart)) {
            if (!bRestart) {
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                        Low, "Starting walk " + getDescription());
            }

            if (mStatus == NotYetStarted && mAdv.mVersion < 5) {
                // Some older games (e.g. Lair of the Vampire) start an
                // NPC character walk (e.g. Gragor, Character30) to follow
                // the player (or another character) from an initial
                // starting position of Hidden. One could argue this is
                // a game error, but the older Adrift runner seemed to
                // permit it, so we do too. Our work-around: for such cases,
                // move the character to the same room as the player before
                // starting any walk.
                if (mSteps.size() > 0) {
                    String stepKey = mSteps.get(0).mLocation;
                    if (stepKey.equals(THEPLAYER)) {
                        stepKey = mAdv.getPlayer().getKey();
                    }
                    if (mAdv.mCharacters.containsKey(stepKey)) {
                        MCharacter chMove = mAdv.mCharacters.get(mCharKey);
                        MCharacter chTo = mAdv.mCharacters.get(stepKey);
                        MCharacter.MCharacterLocation chLocMove = chMove.getLocation();
                        MCharacter.MCharacterLocation chLocTo = chTo.getLocation();
                        if (chLocMove.getExistsWhere() == Hidden) {
                            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                    Low, "Follow char walk starting from " +
                                            "hidden, so moving to dest char's " +
                                            "location first...");
                            chMove.moveTo(chLocTo.getKey());
                        }
                    }
                }
            }

            mStatus = Running;
            resetLength();
            setTimerToEndOfWalk(getLength());

            if (getTimerFromStartOfWalk() == 0) {
                doAnySteps();
                doAnySubWalks();    // To run 'after 0 turns' subevents
            }

            mJustStarted = true;
        } else {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                    MView.DebugDetailLevelEnum.Error,
                    "Can't Start a Walk that isn't waiting!");
        }
    }

    public void pause() {
        mNextCommand = Pause;
    }

    private void lPause() {
        if (mStatus == Running) {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                    Low, "Pausing walk " + getDescription());
            mStatus = Paused;
        } else {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                    MView.DebugDetailLevelEnum.Error,
                    "Can't Pause a Walk that isn't running!");
        }
    }

    public void resume() {
        mNextCommand = Command.Resume;
    }

    private void lResume() {
        if (mStatus == Paused) {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                    Low, "Resuming walk " + getDescription());
            mStatus = Running;
        } else {
            //Throw New Exception("Can' t Resume a Walk that isn 't paused!")
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                    MView.DebugDetailLevelEnum.Error,
                    "Can't Resume a Walk that isn't paused!");
        }
    }

    public void stop() {
        mNextCommand = Stop;
    }

    private void lStop() throws InterruptedException {
        lStop(false, false);
    }

    private void lStop(boolean bRunSubEvents, boolean bReachedEnd) throws InterruptedException {
        if (bRunSubEvents) {
            doAnySubWalks();
        }
        mStatus = Finished;
        if (mIsLooping && getTimerToEndOfWalk() == 0 && bReachedEnd) {
            if (getLength() > 0) {
                // Only restart if walk comes to and end and it
                // is set to loop - not if it is terminated by task change
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                        Low, "Restarting walk " + getDescription());
                lStart(true);
            } else {
                mAdv.mView.debugPrint(MGlobals.ItemEnum.Event, mCharKey, Low,
                        "Not restarting walk " +
                                getDescription() +
                                " otherwise we'd get in an infinite loop as zero length.");
            }
        } else {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                    Low, "Finishing walk " + getDescription());
        }
    }

    public void incrementTimer() throws InterruptedException {
        if (mNextCommand != Nothing) {
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
                case Restart:
                    lStart(true);
                    break;
            }
            mNextCommand = Nothing;
            mTriggeringTask = "";
        }

        if (mStatus == Running) {
            mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                    High,
                    "Walk " + getDescription() +
                            " [" + (getTimerFromStartOfWalk() + 1) +
                            "/" + getLength() + "]");
        }

        // Split this into 2 case statements, as changing timer here may change status
        switch (mStatus) {
            case NotYetStarted:
            case Running:
                if (!mJustStarted) {
                    setTimerToEndOfWalk(getTimerToEndOfWalk() - 1);
                }
                break;
            case Paused:
            case Finished:
                break;
        }

        if (!mJustStarted) {
            doAnySteps();
            doAnySubWalks();
        }

        mJustStarted = false;
    }

    private void doAnySteps() {
        if (mStatus == Running) {
            int stepLen = 0;

            for (MStep step : mSteps) {
                if (stepLen == getTimerFromStartOfWalk()) {
                    String destKey = "";
                    String stepKey = step.mLocation;

                    if (stepKey.equals(THEPLAYER)) {
                        stepKey = mAdv.getPlayer().getKey();
                    }

                    if (mAdv.mGroups.containsKey(stepKey)) {
                        // Get an adjacent location in the group
                        MGroup grp = mAdv.mGroups.get(stepKey);
                        MStringArrayList locKeys = grp.getMembers();
                        MCharacter ch = mAdv.mCharacters.get(mCharKey);
                        MLocation locCurrent = null;
                        if (ch.getLocation().getExistsWhere() != Hidden) {
                            locCurrent = mAdv.mLocations.get(ch.getLocation().getLocationKey());
                        }
                        boolean bHasAdjacent = false;
                        if (locCurrent != null) {
                            for (String locKey : locKeys) {
                                if (locCurrent.isAdjacent(locKey)) {
                                    bHasAdjacent = true;
                                    break;
                                }
                            }
                        }
                        if (bHasAdjacent) {
                            while (destKey.equals("")) {
                                String sPossibleDest = locKeys.get(mAdv.getRand(locKeys.size() - 1));
                                if (ch.getLocation().getExistsWhere() == Hidden ||
                                        locCurrent.isAdjacent(sPossibleDest)) {
                                    destKey = sPossibleDest;
                                }
                            }
                        } else {
                            // No adjacent room, so just move to a random room in the group
                            destKey = locKeys.get(mAdv.getRand(locKeys.size() - 1));
                        }
                        mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                Medium, "Character " +
                                        mAdv.mCharacters.get(mCharKey).getProperName() +
                                        " walks to " +
                                        mAdv.getNameFromKey(stepKey) + " (" +
                                        mAdv.mLocations.get(destKey).getShortDescription().toString() + ")");
                    } else if (mAdv.mCharacters.containsKey(stepKey)) {
                        // Only move towards the character if they are
                        // in an adjacent room
                        MCharacter chMove = mAdv.mCharacters.get(mCharKey);
                        MCharacter chTo = mAdv.mCharacters.get(stepKey);
                        MCharacter.MCharacterLocation chLocMove = chMove.getLocation();
                        MCharacter.MCharacterLocation chLocTo = chTo.getLocation();
                        if (!chLocMove.getLocationKey().equals(chLocTo.getLocationKey())) {
                            MLocation locCurrent = null;
                            if (chLocMove.getExistsWhere() != Hidden) {
                                locCurrent = mAdv.mLocations.get(chLocMove.getLocationKey());
                            }

                            // We have an issue here with some older games. These games
                            // sometimes moved the player to a new location (while another
                            // character was following them) via an action of a movement
                            // task override. Then the following character should join them
                            // also, but cannot if we strictly require the adjacent rooms
                            // rule, as the two locations are connected by a task rather
                            // than location exit. E.g. Lair of the Vampire - eventually
                            // the player gets to Location42 (junction), with an NPC,
                            // Gragor, following them. When the player types "west", it
                            // executes a task to transport the player to Location51.
                            // Then Gragor can't follow as the rooms are not actually
                            // adjacent.
                            //
                            // Our work-around: look at the lead character's last location.
                            // If it is the same as the following character's current location,
                            // waive the adjacency requirement, on the assumption that if the
                            // last location is not adjacent to the lead character's new
                            // location then the lead character was moved by a task.
                            boolean waiveAdjacencyRule = false;
                            if (mAdv.mVersion < 5) {
                                MCharacter.MCharacterLocation chLocToLast = chTo.getLastLocation();
                                if (chLocToLast != null) {
                                    waiveAdjacencyRule = locCurrent.getKey().equals(chLocToLast.getLocationKey());
                                }
                            }

                            if (locCurrent != null &&
                                    (locCurrent.isAdjacent(chLocTo.getKey()) || waiveAdjacencyRule)) {
                                destKey = chLocTo.getKey();
                                mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                        Medium, "Character " +
                                                mAdv.mCharacters.get(mCharKey).getProperName() +
                                                " walks to " + chTo.getProperName() + " (" +
                                                mAdv.mLocations.get(destKey).getShortDescription().toString() + ")");
                            } else {
                                // Character is not adjacent, so don't move
                                mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                        MView.DebugDetailLevelEnum.Error,
                                        "Character " + chMove.getProperName() +
                                                " can't walk to " + chTo.getProperName() +
                                                " as " + chTo.getGender().toString()
                                                .replace("Male", "he")
                                                .replace("Female", "she")
                                                .replace("Unknown", "it") +
                                                " is not in an adjacent location.");
                                destKey = "";
                            }
                        }
                    } else {
                        destKey = stepKey;
                        mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                Medium, "Character " +
                                        mAdv.mCharacters.get(mCharKey).getProperName() +
                                        " walks to " + mAdv.getNameFromKey(destKey));
                    }

                    if (destKey.equals(HIDDEN) || mAdv.mLocations.containsKey(destKey)) {
                        MCharacter ch = mAdv.mCharacters.get(mCharKey);
                        MCharacter.MCharacterLocation chLoc = ch.getLocation();
                        MCharacter.MCharacterLocation playerLoc = mAdv.getPlayer().getLocation();
                        if (ch.hasProperty(PKEY_SHOW_ENTER_EXIT) &&
                                chLoc.getExistsWhere() == AtLocation) {
                            if (chLoc.getKey().equals(playerLoc.getLocationKey()) &&
                                    !destKey.equals(playerLoc.getLocationKey())) {
                                String sExits = "exits";
                                if (ch.hasProperty(PKEY_CHAR_EXITS)) {
                                    sExits = ch.getPropertyValue(PKEY_CHAR_EXITS);
                                }
                                StringBuilder sLeaves = new StringBuilder(ch.getName());
                                toProper(sLeaves);
                                sLeaves.append(" ").append(sExits); // to the North... etc
                                if (mAdv.mLocations.get(playerLoc.getLocationKey()).isAdjacent(destKey)) {
                                    String toDir = mAdv.mLocations.get(chLoc.getLocationKey()).getDirectionTo(destKey);
                                    if (!toDir.equals("nowhere")) {
                                        switch (toDir) {
                                            case "outside":
                                            case "inside":
                                                sLeaves.append(" ");
                                                break;
                                            default:
                                                sLeaves.append(" to ");
                                                break;
                                        }
                                        sLeaves.append(toDir);
                                    }
                                }
                                sLeaves.append(".");
                                mAdv.mView.displayText(mAdv, sLeaves.toString());
                            } else if (destKey.equals(playerLoc.getLocationKey()) &&
                                    !chLoc.getKey().equals(playerLoc.getLocationKey())) {
                                String sEnters = "enters";
                                if (ch.hasProperty(PKEY_CHAR_ENTERS)) {
                                    sEnters = ch.getPropertyValue(PKEY_CHAR_ENTERS);
                                }
                                StringBuilder sArrives = new StringBuilder(ch.getName());
                                toProper(sArrives);
                                sArrives.append(" ").append(sEnters); // from the North... etc
                                if (mAdv.mLocations.get(playerLoc.getLocationKey()).isAdjacent(chLoc.getKey())) {
                                    String fromDir = mAdv.mLocations.get(destKey).getDirectionTo(chLoc.getLocationKey());
                                    if (!fromDir.equals("nowhere")) {
                                        sArrives.append(" from ").append(fromDir);
                                    }
                                }
                                sArrives.append(".");
                                mAdv.mView.displayText(mAdv, sArrives.toString());
                            }
                        }
                        ch.moveTo(destKey);
                    }
                }
                stepLen += step.mTurns.getValue();
            }
        }
    }

    private void doAnySubWalks() throws InterruptedException {
        switch (mStatus) {
            case Running:
                // Check all the subevents to see if we need to do anything
                int iIndex = 0;
                for (MSubWalk sw : mSubWalks) {
                    boolean bRunSubWalk = false;
                    switch (sw.eWhen) {
                        case FromStartOfWalk:
                            if (getTimerFromStartOfWalk() == sw.ftTurns.getValue() && sw.ftTurns.getValue() <= getLength()) {
                                bRunSubWalk = true;
                            }
                            break;
                        case FromLastSubWalk:
                            if (getTimerFromLastSubWalk() == sw.ftTurns.getValue()) {
                                if (mLastSubwalk == null && iIndex == 0 ||
                                        (iIndex > 0 && mLastSubwalk == mSubWalks.get(iIndex - 1))) {
                                    bRunSubWalk = true;
                                }
                            }
                            break;
                        case BeforeEndOfWalk:
                            if (getTimerToEndOfWalk() == sw.ftTurns.getValue()) {
                                bRunSubWalk = true;
                            }
                            break;
                        case ComesAcross:
                            // can be either an object or a character
                            MObject ob = null;
                            MCharacter ch;
                            if (sw.sKey.equals(THEPLAYER)) {
                                ch = mAdv.getPlayer();
                            } else {
                                ch = mAdv.mCharacters.get(sw.sKey);
                                if (ch == null) {
                                    // do we have an object
                                    ob = mAdv.mObjects.get(sw.sKey);
                                }
                            }
                            if (ch != null || ob != null) {
                                MCharacter me = mAdv.mCharacters.get(mCharKey);
                                if (me != null) {
                                    if (ch != null) {
                                        boolean bPrevSameLocationAsChar = sw.bSameLocationAsChar;
                                        sw.bSameLocationAsChar = (me.getLocation().getLocationKey().equals(ch.getLocation().getLocationKey()));
                                        if (!bPrevSameLocationAsChar && sw.bSameLocationAsChar) {
                                            bRunSubWalk = true;
                                        }
                                    } else {
                                        boolean bPrevSameLocationAsObj = sw.bSameLocationAsObj;
                                        sw.bSameLocationAsObj = (me.getLocation().getLocationKey().equals(ob.getLocation().getKey()));
                                        if (!bPrevSameLocationAsObj && sw.bSameLocationAsObj) {
                                            bRunSubWalk = true;
                                        }
                                    }
                                }
                            }
                            break;
                    }

                    if (bRunSubWalk) {
                        switch (sw.eWhat) {
                            case DisplayMessage:
                                if (mAdv.getPlayer().isInGroupOrLocation(sw.sKey3)) {
                                    mAdv.mView.displayText(mAdv, sw.oDescription.toString());
                                }
                                break;
                            case ExecuteTask:
                                if (mAdv.mTasks.containsKey(sw.sKey2)) {
                                    mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                            Medium, "Walk '" + getDescription() +
                                                    "' attempting to execute task '" +
                                                    mAdv.mTasks.get(sw.sKey2).getDescription() + "'");
                                    MTask tas = mAdv.mTasks.get(sw.sKey2);
                                    tas.attemptToExecute(true);
                                }
                                break;
                            case ExecuteCommand:
                                // This is deprecated and only included to provide
                                // compatibility for v3.8 games.
                                mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                        Medium, "Walk '" + getDescription() +
                                                "' executing command '" + sw.sKey2 + "'");
                                MTaskHashMap.MTaskMatchResult match =
                                        mAdv.mTasks.find(sw.sKey2, 0, null);
                                if (match.mTask != null) {
                                    // Try to execute it
                                    match.mTask.attemptToExecute(true);
                                }
                                break;
                            case UnsetTask:
                                mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, mCharKey,
                                        Medium, "Walk '" + getDescription() +
                                                "' unsetting task '" + mAdv.mTasks.get(sw.sKey2).getDescription() + "'");
                                mAdv.mTasks.get(sw.sKey2).setCompleted(false);
                                break;
                        }
                        mLastSubwalkTime = getTimerFromStartOfWalk();
                        mLastSubwalk = sw;
                    }
                    iIndex++;
                }
                break;
        }
    }

    enum Command {
        Nothing,        // 0
        Start,          // 1
        Stop,           // 2
        Pause,          // 3
        Resume,         // 4
        Restart         // 5
    }

    public enum StatusEnum {
        NotYetStarted,  // 0
        Running,        // 1
        Paused,         // 3
        Finished        // 4
    }

    public static class MStep {
        public String mLocation = "";
        public MFromTo mTurns;

        MStep(@NonNull MAdventure adv) {
            mTurns = new MFromTo(adv);
        }

        MStep(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
              double dFileVersion) throws IOException, XmlPullParserException {
            this(adv);

            xpp.require(START_TAG, null, "Step");

            String sData[] = xpp.nextText().split(" ");
            mLocation = sData[0];
            mTurns.mFrom = cint(sData[1]);
            mTurns.mTo = (sData.length == 2) ? cint(sData[1]) : cint(sData[3]);
            if (dFileVersion < 5.000029) {
                if (mLocation.equals(THEPLAYER)) {
                    if (mTurns.mFrom < 1) {
                        mTurns.mFrom = 1;
                    }
                    if (mTurns.mTo < 1) {
                        mTurns.mTo = 1;
                    }
                }
            }
            xpp.require(END_TAG, null, "Step");
        }
    }

    public static class MWalkState {
        public StatusEnum mStatus;
        int mTimerToEndOfWalk;

        MWalkState(@NonNull MWalk w) {
            mStatus = w.mStatus;
            mTimerToEndOfWalk = w.mTimerToEndWalk;
        }

        MWalkState(@NonNull MAdventure adv, @NonNull XmlPullParser xpp) throws Exception {
            xpp.require(START_TAG, null, "Walk");

            int depth = xpp.getDepth();
            int evType;

            while ((evType = xpp.nextTag()) != END_DOCUMENT &&
                    xpp.getDepth() > depth) {
                if (evType == START_TAG) {
                    switch (xpp.getName()) {
                        case "Status": {
                            mStatus = StatusEnum.valueOf(xpp.nextText());
                            break;
                        }
                        case "Timer": {
                            mTimerToEndOfWalk = adv.safeInt(xpp.nextText());
                            break;
                        }
                    }
                }
            }

            xpp.require(END_TAG, null, "Walk");
        }

        public void serialize(@NonNull XmlSerializer xs) throws IOException {
            xs.startTag(null, "Walk");

            xs.startTag(null, "Status");
            xs.text(mStatus.toString());
            xs.endTag(null, "Status");

            xs.startTag(null, "Timer");
            xs.text(String.valueOf(mTimerToEndOfWalk));
            xs.endTag(null, "Timer");

            xs.endTag(null, "Walk");
        }

        public void restore(@NonNull MWalk w) {
            w.mStatus = mStatus;
            w.mTimerToEndWalk = mTimerToEndOfWalk;
        }
    }
}
