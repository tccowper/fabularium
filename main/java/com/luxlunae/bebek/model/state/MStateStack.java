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

package com.luxlunae.bebek.model.state;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MDescription;
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.MGroup;
import com.luxlunae.bebek.model.MItemWithProperties;
import com.luxlunae.bebek.model.MLocation;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MProperty;
import com.luxlunae.bebek.model.MSingleDescription;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.model.MVariable;
import com.luxlunae.bebek.model.MWalk;
import com.luxlunae.glk.GLKLogger;

import java.util.ArrayList;
import java.util.HashMap;

import static com.luxlunae.bebek.model.MAction.EndGameEnum.Running;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.GameState_TAS;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.All;
import static com.luxlunae.bebek.model.io.MFileIO.loadFile;

public class MStateStack extends MMyStack {

    public void pop(@NonNull MAdventure adv) {
        restoreState(adv, super.pop());
        //GLKLogger.debug("Popped (" + super.Count() + " on stack)");
    }

    private void saveDisplayOnce(@NonNull ArrayList<MDescription> allDescriptions,
                                 @NonNull HashMap<String, Boolean> store) {
        int iDesc = 0;
        for (MDescription d : allDescriptions) {
            iDesc++;
            int iSing = 0;
            for (MSingleDescription sd : d) {
                iSing++;
                if (sd.mDisplayOnce && sd.mDisplayed) {
                    String sKey = iDesc + "-" + iSing;
                    store.put(sKey, true);
                }
            }
        }
    }

    @NonNull
    public MGameState getState(@NonNull MAdventure adv) {
        // Get the current game state, and store in a GameState class
        MGameState ret = new MGameState();

        ret.sOutputText = adv.mTurnOutput;

        for (MLocation loc : adv.mLocations.values()) {
            MGameState.MLocationState locs = new MGameState.MLocationState();
            for (String sPropKey : loc.getLocalProperties().keySet()) {
                MProperty prop = loc.getProperties().get(sPropKey);
                MGameState.MLocationState.MStateProperty props =
                        new MGameState.MLocationState.MStateProperty();
                props.Value = prop.getValue(true);
                locs.htblProperties.put(sPropKey, props);
            }
            saveDisplayOnce(loc.getAllDescriptions(), locs.htblDisplayedDescriptions);
            locs.bSeen = adv.getPlayer().hasSeenLocation(loc.getKey());
            ret.htblLocationStates.put(loc.getKey(), locs);
        }

        for (MObject ob : adv.mObjects.values()) {
            MGameState.MObjectState obs = new MGameState.MObjectState();
            obs.Location = ob.getLocation().copy();
            for (String sPropKey : ob.getLocalProperties().keySet()) {
                MProperty prop = ob.getProperties().get(sPropKey);
                MGameState.MObjectState.MStateProperty props =
                        new MGameState.MObjectState.MStateProperty();
                props.Value = prop.getValue(true);
                obs.htblProperties.put(sPropKey, props);
            }
            saveDisplayOnce(ob.getAllDescriptions(), obs.htblDisplayedDescriptions);
            ret.htblObjectStates.put(ob.getKey(), obs);
        }

        for (MTask tas : adv.mTasks.values()) {
            MGameState.MTaskState tass = new MGameState.MTaskState();
            tass.Completed = tas.getCompleted();
            tass.Scored = tas.mIsScored;
            saveDisplayOnce(tas.getAllDescriptions(), tass.htblDisplayedDescriptions);
            ret.htblTaskStates.put(tas.getKey(), tass);
        }

        for (MEvent ev : adv.mEvents.values()) {
            MGameState.MEventState evs = new MGameState.MEventState();
            evs.Status = ev.mStatus;
            evs.TimerToEndOfEvent = ev.getTimerToEndOfEvent();
            evs.iLastSubEventTime = ev.mLastSubEventTime;
            for (int i = 0; i < ev.mSubEvents.size(); i++) {
                if (ev.mLastSubEvent == ev.mSubEvents.get(i)) {
                    evs.iLastSubEventIndex = i;
                    break;
                }
            }
            saveDisplayOnce(ev.getAllDescriptions(), evs.htblDisplayedDescriptions);
            ret.htblEventStates.put(ev.getKey(), evs);
        }

        for (MCharacter ch : adv.mCharacters.values()) {
            MGameState.MCharacterState chs = new MGameState.MCharacterState();
            chs.Location = ch.getLocation();
            for (MWalk w : ch.mWalks) {
                MGameState.MCharacterState.MWalkState ws =
                        new MGameState.MCharacterState.MWalkState();
                ws.Status = w.mStatus;
                ws.TimerToEndOfWalk = w.mTimerToEndWalk;
                chs.lWalks.add(ws);
            }
            chs.lSeenKeys.clear();
            for (String sLocKey : adv.mLocations.keySet()) {
                if (ch.hasSeenLocation(sLocKey)) {
                    chs.lSeenKeys.add(sLocKey);
                }
            }
            for (String sObKey : adv.mObjects.keySet()) {
                if (ch.hasSeenObject(sObKey)) {
                    chs.lSeenKeys.add(sObKey);
                }
            }
            for (String sChKey : adv.mCharacters.keySet()) {
                if (ch.hasSeenCharacter(sChKey)) {
                    chs.lSeenKeys.add(sChKey);
                }
            }
            for (String sPropKey : ch.getLocalProperties().keySet()) {
                MProperty prop = ch.getProperties().get(sPropKey);
                MGameState.MCharacterState.MStateProperty props =
                        new MGameState.MCharacterState.MStateProperty();
                props.Value = prop.getValue(true);
                chs.htblProperties.put(sPropKey, props);
            }
            if (!chs.htblProperties.containsKey("ProperName")) {
                MGameState.MCharacterState.MStateProperty props =
                        new MGameState.MCharacterState.MStateProperty();
                props.Value = ch.getProperName();
                chs.htblProperties.put("ProperName", props);
            }
            saveDisplayOnce(ch.getAllDescriptions(), chs.htblDisplayedDescriptions);
            ret.htblCharacterStates.put(ch.getKey(), chs);
        }

        for (MVariable var : adv.mVariables.values()) {
            MGameState.MVariableState vars = new MGameState.MVariableState();
            vars.Value = new String[var.getLength()];
            for (int i = 0; i < var.getLength(); i++) {
                if (var.getType() == MVariable.VariableType.NUMERIC) {
                    vars.Value[i] = String.valueOf(var.getIntAt(i + 1));
                } else {
                    vars.Value[i] = var.getStrAt(i + 1);
                }
            }
            saveDisplayOnce(var.getAllDescriptions(), vars.htblDisplayedDescriptions);
            ret.htblVariableStates.put(var.getKey(), vars);
        }

        for (MGroup grp : adv.mGroups.values()) {
            MGameState.MGroupState grps = new MGameState.MGroupState();
            grps.lstMembers.addAll(grp.getArlMembers());
            ret.htblGroupStates.put(grp.getKey(), grps);
        }

        return ret;
    }

    public void recordState(@NonNull MAdventure adv) {
        // Save the current game state onto our stack
        push(getState(adv));
    }

    public void loadState(@NonNull MAdventure adv, @NonNull String filePath) {
        // Load from file, and restore
        MGameState state[] = new MGameState[1];
        loadFile(adv, filePath, GameState_TAS, All, false, state, 0);
        if (state[0] != null) {
            restoreState(adv, state[0]);
        }
    }

    private void restoreDisplayOnce(@NonNull ArrayList<MDescription> allDescriptions,
                                    @NonNull HashMap<String, Boolean> store) {
        int iDesc = 0;
        for (MDescription d : allDescriptions) {
            iDesc++;
            int iSing = 0;
            for (MSingleDescription sd : d) {
                iSing++;
                if (sd.mDisplayOnce) {
                    String sKey = iDesc + "-" + iSing;
                    sd.mDisplayed = store.containsKey(sKey);
                }
            }
        }
    }

    public void restoreState(@NonNull MAdventure adv, @Nullable MGameState state) {
        if (state == null) {
            return;
        }

        adv.mTurnOutput = state.sOutputText;

        for (MLocation loc : adv.mLocations.values()) {
            if (state.htblLocationStates.containsKey(loc.getKey())) {
                MGameState.MLocationState locs = state.htblLocationStates.get(loc.getKey());
                ArrayList<String> lDelete = new ArrayList<>();
                for (MProperty prop : loc.getProperties().values()) {
                    if (locs.htblProperties.containsKey(prop.getKey())) {
                        MGameState.MLocationState.MStateProperty props =
                                locs.htblProperties.get(prop.getKey());
                        prop.setValue(props.Value);
                    } else {
                        lDelete.add(prop.getKey());
                    }
                }
                for (String sKey : lDelete) {
                    loc.removeProperty(sKey);
                }
                for (String sPropKey : locs.htblProperties.keySet()) {
                    if (!loc.getLocalProperties().containsKey(sPropKey)) {
                        if (adv.mLocationProperties.containsKey(sPropKey)) {
                            MProperty propAdd = adv.mLocationProperties.get(sPropKey).clone();
                            if (propAdd.getType() == MProperty.PropertyTypeEnum.SelectionOnly) {
                                propAdd.setSelected(true);
                                loc.addProperty(propAdd);
                            }
                        }
                    }
                }
                loc.resetInherited();
                restoreDisplayOnce(loc.getAllDescriptions(), locs.htblDisplayedDescriptions);
            }
        }

        for (MObject ob : adv.mObjects.values()) {
            if (state.htblObjectStates.containsKey(ob.getKey())) {
                MGameState.MObjectState obs = state.htblObjectStates.get(ob.getKey());
                ob.setLocation(obs.Location.copy());
                ArrayList<String> lDelete = new ArrayList<>();
                for (MProperty prop : ob.getProperties().values()) {
                    if (obs.htblProperties.containsKey(prop.getKey())) {
                        MGameState.MObjectState.MStateProperty props =
                                obs.htblProperties.get(prop.getKey());
                        prop.setValue(props.Value);
                    } else {
                        lDelete.add(prop.getKey());
                    }
                }
                for (String sKey : lDelete) {
                    ob.removeProperty(sKey);
                }
                for (String sPropKey : obs.htblProperties.keySet()) {
                    if (!ob.getLocalProperties().containsKey(sPropKey)) {
                        if (adv.mObjectProperties.containsKey(sPropKey)) {
                            MProperty propAdd = adv.mObjectProperties.get(sPropKey).clone();
                            if (propAdd.getType() == MProperty.PropertyTypeEnum.SelectionOnly) {
                                propAdd.setSelected(true);
                                ob.addProperty(propAdd);
                            }
                        }
                    }
                }
                ob.resetInherited();
                restoreDisplayOnce(ob.getAllDescriptions(), obs.htblDisplayedDescriptions);
            }
        }

        for (MTask tas : adv.mTasks.values()) {
            if (state.htblTaskStates.containsKey(tas.getKey())) {
                MGameState.MTaskState tass = state.htblTaskStates.get(tas.getKey());
                tas.setCompleted(tass.Completed);
                tas.mIsScored = tass.Scored;
                restoreDisplayOnce(tas.getAllDescriptions(), tass.htblDisplayedDescriptions);
            }
        }

        for (MEvent ev : adv.mEvents.values()) {
            if (state.htblEventStates.containsKey(ev.getKey())) {
                MGameState.MEventState evs = state.htblEventStates.get(ev.getKey());
                ev.mStatus = evs.Status;
                try {
                    ev.setTimerToEndOfEvent(evs.TimerToEndOfEvent);
                } catch (InterruptedException e) {
                    GLKLogger.error("MStackState: restoreState(): caught interrupted exception!");
                }
                ev.mLastSubEventTime = evs.iLastSubEventTime;
                if (ev.mSubEvents.size() > evs.iLastSubEventIndex) {
                    ev.mLastSubEvent = ev.mSubEvents.get(evs.iLastSubEventIndex);
                }
                restoreDisplayOnce(ev.getAllDescriptions(), evs.htblDisplayedDescriptions);
            }
        }

        for (MCharacter ch : adv.mCharacters.values()) {
            if (state.htblCharacterStates.containsKey(ch.getKey())) {
                MGameState.MCharacterState chs = state.htblCharacterStates.get(ch.getKey());
                ch.setLocation(chs.Location);
                if (ch.mWalks.size() == chs.lWalks.size()) {
                    for (int i = 0; i < ch.mWalks.size(); i++) {
                        MWalk w = ch.mWalks.get(i);
                        MGameState.MCharacterState.MWalkState ws = chs.lWalks.get(i);
                        w.mStatus = ws.Status;
                        w.mTimerToEndWalk = ws.TimerToEndOfWalk;
                    }
                }
                for (String sLocKey : adv.mLocations.keySet()) {
                    ch.setHasSeenLocation(sLocKey, chs.lSeenKeys.contains(sLocKey));
                }
                for (String sObKey : adv.mObjects.keySet()) {
                    ch.setHasSeenObject(sObKey, chs.lSeenKeys.contains(sObKey));
                }
                for (String sChKey : adv.mCharacters.keySet()) {
                    ch.setHasSeenCharacter(sChKey, chs.lSeenKeys.contains(sChKey));
                }
                ArrayList<String> lDelete = new ArrayList<>();
                for (MProperty prop : ch.getProperties().values()) {
                    if (chs.htblProperties.containsKey(prop.getKey())) {
                        MGameState.MCharacterState.MStateProperty props =
                                chs.htblProperties.get(prop.getKey());
                        prop.setValue(props.Value);
                    } else {
                        lDelete.add(prop.getKey());
                    }
                }
                for (String sKey : lDelete) {
                    ch.removeProperty(sKey);
                }
                for (String sPropKey : chs.htblProperties.keySet()) {
                    if (!ch.getLocalProperties().containsKey(sPropKey)) {
                        if (adv.mCharacterProperties.containsKey(sPropKey)) {
                            MProperty propAdd = adv.mCharacterProperties.get(sPropKey).clone();
                            if (propAdd.getType() == MProperty.PropertyTypeEnum.SelectionOnly) {
                                propAdd.setSelected(true);
                                ch.addProperty(propAdd);
                            }
                        } else {
                            switch (sPropKey) {
                                case "ProperName":
                                    ch.setProperName(chs.htblProperties.get(sPropKey).Value);
                                    break;
                            }
                        }
                    }
                }
                ch.resetInherited();
                restoreDisplayOnce(ch.getAllDescriptions(), chs.htblDisplayedDescriptions);
            }
        }

        for (MVariable var : adv.mVariables.values()) {
            if (state.htblVariableStates.containsKey(var.getKey())) {
                MGameState.MVariableState vars = state.htblVariableStates.get(var.getKey());
                for (int i = 0; i < var.getLength(); i++) {
                    if (var.getType() == MVariable.VariableType.NUMERIC) {
                        var.setAt(i + 1, MGlobals.safeInt(vars.Value[i]));
                    } else {
                        var.setAt(i + 1, vars.Value[i]);
                    }
                }
                restoreDisplayOnce(var.getAllDescriptions(), vars.htblDisplayedDescriptions);
            }
        }

        for (MGroup grp : adv.mGroups.values()) {
            if (state.htblGroupStates.containsKey(grp.getKey())) {
                MGameState.MGroupState grps = state.htblGroupStates.get(grp.getKey());
                ArrayList<String> lMembersToReset = new ArrayList<>(grp.getArlMembers());
                grp.getArlMembers().clear();
                for (String sMember : grps.lstMembers) {
                    grp.getArlMembers().add(sMember);
                    if (lMembersToReset.contains(sMember)) {
                        lMembersToReset.remove(sMember);
                    } else {
                        lMembersToReset.add(sMember);
                    }
                }
                for (String sMember : lMembersToReset) {
                    if (adv.mAllItems.containsKey(sMember)) {
                        ((MItemWithProperties) adv.mAllItems.get(sMember)).resetInherited();
                    }
                }
            }
        }
    }

    public boolean setLastState(@NonNull MAdventure adv) {
        if (super.count() > 1) {
            // Discard current state
            super.pop();
            restoreState(adv, super.peek());
            adv.mGameState = Running;
            GLKLogger.debug("Popped (" + super.count() + " on stack)");
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        super.clear();
    }
}
