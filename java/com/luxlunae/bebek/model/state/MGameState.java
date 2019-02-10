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

import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MWalk;

import java.util.ArrayList;
import java.util.HashMap;

public class MGameState {

    @Nullable
    String sOutputText;
    @NonNull
    public HashMap<String, MObjectState> htblObjectStates = new HashMap<>();
    @NonNull
    public HashMap<String, MCharacterState> htblCharacterStates = new HashMap<>();
    @NonNull
    public HashMap<String, MTaskState> htblTaskStates = new HashMap<>();
    @NonNull
    public HashMap<String, MEventState> htblEventStates = new HashMap<>();
    @NonNull
    public HashMap<String, MVariableState> htblVariableStates = new HashMap<>();
    @NonNull
    public HashMap<String, MLocationState> htblLocationStates = new HashMap<>();
    @NonNull
    public HashMap<String, MGroupState> htblGroupStates = new HashMap<>();

    public static class MObjectState {
        @Nullable
        public MObject.MObjectLocation Location;
        @NonNull
        public HashMap<String, MStateProperty> htblProperties = new HashMap<>();
        @NonNull
        public HashMap<String, Boolean> htblDisplayedDescriptions = new HashMap<>();

        public static class MStateProperty {
            @Nullable
            public String Value;
        }
    }

    public static class MCharacterState {
        @Nullable
        public MCharacter.MCharacterLocation Location;
        @NonNull
        public ArrayList<MWalkState> lWalks = new ArrayList<>();
        @NonNull
        public ArrayList<String> lSeenKeys = new ArrayList<>();
        @NonNull
        public HashMap<String, MStateProperty> htblProperties = new HashMap<>();
        @NonNull
        public HashMap<String, Boolean> htblDisplayedDescriptions = new HashMap<>();

        public static class MWalkState {
            public MWalk.StatusEnum Status;
            public int TimerToEndOfWalk;
        }

        public static class MStateProperty {
            @Nullable
            public String Value;
        }
    }

    public static class MTaskState {
        public boolean Completed;
        public boolean Scored;
        @NonNull
        public HashMap<String, Boolean> htblDisplayedDescriptions = new HashMap<>();
    }

    public static class MEventState {
        public MEvent.StatusEnum Status;
        public int TimerToEndOfEvent;
        public int iLastSubEventTime;
        public int iLastSubEventIndex;
        @NonNull
        public HashMap<String, Boolean> htblDisplayedDescriptions = new HashMap<>();
    }

    public static class MVariableState {
        @Nullable
        public String[] Value;
        @NonNull
        public HashMap<String, Boolean> htblDisplayedDescriptions = new HashMap<>();
    }

    public static class MLocationState {
        @NonNull
        public HashMap<String, MStateProperty> htblProperties = new HashMap<>();
        @NonNull
        public HashMap<String, Boolean> htblDisplayedDescriptions = new HashMap<>();
        boolean bSeen;

        public static class MStateProperty {
            @Nullable
            public String Value;
        }
    }

    public static class MGroupState {
        @NonNull
        public ArrayList<String> lstMembers = new ArrayList<>();
    }
}
