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

package com.luxlunae.bebek.model.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.model.MAction;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MDescription;
import com.luxlunae.bebek.model.MGroup;
import com.luxlunae.bebek.model.MLocation;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MProperty;
import com.luxlunae.bebek.model.MRestriction;
import com.luxlunae.bebek.model.MSingleDescription;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.model.MWalk;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.glk.GLKLogger;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.luxlunae.bebek.MGlobals.MPerspectiveEnumFromInt;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.model.MAction.ItemEnum.MoveObject;
import static com.luxlunae.bebek.model.MAction.ItemEnum.SetProperties;
import static com.luxlunae.bebek.model.MAction.ItemEnum.SetTasks;
import static com.luxlunae.bebek.model.MAdventure.EnabledOptionEnum.AutoComplete;
import static com.luxlunae.bebek.model.MAdventure.EnabledOptionEnum.ControlPanel;
import static com.luxlunae.bebek.model.MAdventure.EnabledOptionEnum.Debugger;
import static com.luxlunae.bebek.model.MAdventure.EnabledOptionEnum.Map;
import static com.luxlunae.bebek.model.MAdventure.EnabledOptionEnum.Score;
import static com.luxlunae.bebek.model.MAdventure.TaskExecutionEnum.HighestPriorityPassingTask;
import static com.luxlunae.bebek.model.MCharacter.CharacterType.Player;
import static com.luxlunae.bebek.model.MCharacter.Gender.Female;
import static com.luxlunae.bebek.model.MCharacter.Gender.Male;
import static com.luxlunae.bebek.model.MCharacter.Gender.Unknown;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.OnObject;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.MPositionEnumFromInt;
import static com.luxlunae.bebek.model.MCharacter.PKEY_CHAR_POSITION;
import static com.luxlunae.bebek.model.MCharacter.PKEY_KNOWN;
import static com.luxlunae.bebek.model.MCharacter.PKEY_MAX_BULK;
import static com.luxlunae.bebek.model.MCharacter.PKEY_MAX_WEIGHT;
import static com.luxlunae.bebek.model.MGroup.GroupTypeEnum.Locations;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.Must;
import static com.luxlunae.bebek.model.MRestriction.RestrictionTypeEnum.Task;
import static com.luxlunae.bebek.model.MRestriction.TaskEnum.Complete;
import static com.luxlunae.bebek.model.MSingleDescription.DisplayWhenEnum.StartDescriptionWithThis;
import static com.luxlunae.bebek.model.MTask.LIBRARY_START_TASK_PRIORITY;
import static com.luxlunae.bebek.model.MTask.SpecificOverrideTypeEnum.AfterTextOnly;
import static com.luxlunae.bebek.model.MTask.SpecificOverrideTypeEnum.BeforeActionsOnly;
import static com.luxlunae.bebek.model.MTask.SpecificOverrideTypeEnum.BeforeTextAndActions;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.General;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.Specific;
import static com.luxlunae.bebek.model.io.IOUtils.decode;
import static com.luxlunae.bebek.model.io.IOUtils.decompress;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Lieable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Sittable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Standable;

public class MFileOlder {
    private static final String[] MANDATORY_PROPERTIES = new String[]{
            "AtLocation", "AtLocationGroup", "CharacterAtLocation",
            "CharacterLocation", "CharacterPosition", "CharEnters", "CharExits",
            "Container", "DynamicLocation", "HeldByWho", "InLocation", "InsideWhat",
            "Lockable", "LockKey", "LockStatus", "ListDescription", "OnWhat",
            "Openable", "OpenStatus", "PartOfWho", "Readable", "ReadText",
            "ShowEnterExit", "StaticLocation", "StaticOrDynamic", "Surface",
            "Wearable", "WornByWho"};

    private static final int V380_OBJ_CAPACITY_MULT = 10;
    private static final int V380_OBJ_DEFAULT_SIZE = 2;
    @NonNull
    private static MStringArrayList mWithStates = new MStringArrayList();

    @NonNull
    public static MGroup getRoomGroupFromList(@NonNull MAdventure adv,
                                              @NonNull MStringArrayList locs,
                                              @NonNull String description) {
        MGroup grp = null;

        // See if a group exists with the same rooms
        // In the case of sets, equals "compares the specified object
        // with this set for equality. Returns true if the given object is
        // also a set, the two sets have the same size, and every member of
        // the given set is contained in this set."
        for (MGroup agrp : adv.mGroups.values()) {
            if (agrp.getGroupType() == Locations) {
                if (agrp.getArlMembers().equals(locs)) {
                    grp = agrp;
                    break;
                }
            }
        }

        if (grp == null) {
            // No group found
            grp = new MGroup(adv);
            grp.setName("Generated group for " + description);
            grp.getArlMembers().addAll(locs);
            grp.setKey("GeneratedLocationGroup" + (adv.mGroups.size() + 1));
            grp.setGroupType(Locations);
            adv.mGroups.put(grp.getKey(), grp);
        } else {
            // There's a group already - just append to its name
            grp.setName(grp.getName() + " and " + description);
        }

        return grp;
    }

    private static int getMaxPriority(@NonNull MAdventure adv) {
        // Always excludes library tasks
        int iMax = 0;
        for (MTask tas : adv.mTasks.values()) {
            int p = tas.getPriority();
            if (p > iMax && p < LIBRARY_START_TASK_PRIORITY) {
                iMax = p;
            }
        }
        return iMax;
    }

    @NonNull
    public static String convertV4FuncsToV5(@NonNull MAdventure adv,
                                            @NonNull String s400Text) {
        // Converts v4 functions to v5

        // Convert version 4's reserved variable names:
        //
        //  %character%         -   Name of the Referenced Character
        //  %object%            -   The name of the Referenced Object, as defined
        //  %obstatus%          -   "open", "closed" or "locked" for Referenced Object
        //  %player%            -   The name of the Player
        //  %theobject%         -   The name of the Referenced Object, tense adjusted
        //
        String s500Text = s400Text
                .replace("%character%", "%CharacterName[%character%]%")
                .replace("%object%", "%TheObject[%object%]%")
                .replace("%obstatus%", "%LCase[%PropertyValue[%object%,OpenStatus]%]%")
                .replace("%player%", "%player%.ProperName")
                .replace("%theobject%", "%TheObject[%object%]%")
                .replace("%room%", "%LocationName[%LocationOf[Player]%]%");

        //  %t_<variable>%      -  Value of variable <variable>, spelt out if 0=<=20
        //  %t_number%          -  Value of Referenced Number, spelt out if 0=<=20
        while (s500Text.contains("%t_")) {
            int iStart = s500Text.indexOf("%t_");
            int iEnd = s500Text.indexOf("%", iStart + 1) + 1;
            s500Text = s500Text.replace(s500Text.substring(iStart, iEnd),
                    "%NumberAsText[%" + s500Text.substring(iStart + 3, iEnd) + "]%");
        }

        //  %in_<objectname>%   -  A list of all objects inside object <objectname>
        while (s500Text.contains("%in_")) {
            int iStart = s500Text.indexOf("%in_");
            int iEnd = s500Text.indexOf("%", iStart + 1) + 1;
            String obName = s500Text.substring(iStart + 4, iEnd - 1);
            // convert object name to key
            String sKey = "";
            for (MObject ob : adv.mObjects.values()) {
                if (obName.equals(ob.getNames().get(0))) {
                    sKey = ob.getKey();
                    break;
                }
            }
            s500Text = s500Text.replace(s500Text.substring(iStart, iEnd),
                    "%ListObjectsIn[" + sKey + "]%");
        }

        //  %on_<objectname>%   -   A list of all objects on object <objectname>
        while (s500Text.contains("%on_")) {
            int iStart = s500Text.indexOf("%on_");
            int iEnd = s500Text.indexOf("%", iStart + 1) + 1;
            String obName = s500Text.substring(iStart + 4, iEnd - 1);
            // convert object name to key
            String sKey = "";
            for (MObject ob : adv.mObjects.values()) {
                if (obName.equals(ob.getNames().get(0))) {
                    sKey = ob.getKey();
                    break;
                }
            }
            s500Text = s500Text.replace(s500Text.substring(iStart, iEnd),
                    "%ListObjectsOn[" + sKey + "]%");
        }

        //  %onin_<objectname>% -   A list of all object on or in object <objectname>
        while (s500Text.contains("%onin_")) {
            int iStart = s500Text.indexOf("%onin_");
            int iEnd = s500Text.indexOf("%", iStart + 1) + 1;
            String obName = s500Text.substring(iStart + 6, iEnd - 1);
            // convert object name to key
            String sKey = "";
            for (MObject ob : adv.mObjects.values()) {
                if (obName.equals(ob.getNames().get(0))) {
                    sKey = ob.getKey();
                    break;
                }
            }
            s500Text = s500Text.replace(s500Text.substring(iStart, iEnd),
                    "%ListObjectsOnAndIn[" + sKey + "]%");
        }

        // TODO:
        //  %heshe%               -  "he" or "she", depending on the Referenced Character
        //  %status_<objectname>% -  "open", "closed" or "locked" for object <objectname>
        //  %himher%              -  "him" or "her", depending on the Referenced Character
        //  %state_<objectname>%  -  The user-definable state of object <objectname>
        //  %time%                -  The time the game has been played for in seconds

        // Finally, these Adrift 4 variables have the same meaning in Adrift 5,
        // so no conversion needed:
        //
        //  %author%             -  Name of adventure author
        //  %maxscore%           -  The maximum score obtainable
        //  %modified%           -  The date the adventure was last modified
        //  %number%             -  Numeric value of the Referenced Number
        //  %score%              -  The current score
        //  %text%               -  A string containing the Referenced Text
        //  %title%              -  The title of the Adventure
        //  %turns%              -  The number of turns elapsed for the game
        //  %version%            -  Returns the version of the current Runner executable
        return s500Text;
    }

    public static void loadResource(@NonNull MAdventure adv,
                                    @NonNull V4Reader reader,
                                    double version,
                                    @Nullable MSingleDescription sd) throws EOFException {
        if (adv.mCompatSound) {
            String fname = reader.readLine();                   // ?GSound:$SoundFile
            int fsize = 0;
            if (!fname.equals("")) {
                String loop = "";
                if (fname.endsWith("##")) {
                    loop = " loop=Y";
                    fname = fname.substring(0, fname.length() - 2);
                }
                if (sd != null) {
                    sd.mDescription = "<audio play src=\"" + fname +
                            "\"" + loop + ">" + sd.mDescription;
                }
            }
            if (version >= 4) {
                fsize = adv.safeInt(reader.readLine());    // #SoundLen
            }
            if (!fname.equals("") && fsize > 0) {
                adv.mV4Media.put(fname, new MAdventure.v4Media(0, fsize, false));
            }
        }

        if (adv.mCompatGraphics) {
            String fname = reader.readLine();                   // ?GGraphics:$GraphicFile
            int fsize = 0;
            if (!fname.equals("") && sd != null) {
                sd.mDescription = "<img src=\"" + fname + "\">" + sd.mDescription;
            }
            if (version >= 4) {
                fsize = adv.safeInt(reader.readLine());    // #GraphicLen
            }
            if (!fname.equals("") && fsize > 0) {
                adv.mV4Media.put(fname, new MAdventure.v4Media(0, fsize, true));
            }
        }
    }

    private static void loadGlobals(@NonNull MAdventure adv,
                                    @NonNull V4Reader reader,
                                    final double version,
                                    final int iStartMaxPriority) throws EOFException {
        // Load v3.8, v3.9 or v4 header, depending upon value of argument version

        // ===========================================================
        //        START TEXT, START ROOM AND WINNING TEXT
        // -----------------------------------------------------------
        String terminator = (version < 4) ? "**" : "½Ð";
        StringBuilder sb = new StringBuilder();

        // The text in the box Text to show on start-up will be displayed
        // as soon as the adventure is opened in ADRIFT. To start with a
        // completely blank screen instead of having the default adventure
        // title displayed, you may want to add a “<cls>” at the beginning
        // of the introduction.
        String buf = reader.readLine();                                     // Startup Text
        while (!buf.equals(terminator)) {
            sb.append("<br>").append(buf);
            buf = reader.readLine();                                        // Startup Text
        }
        if (sb.length() > 0) {
            adv.getIntroduction().get(0).mDescription = sb.toString();
            sb.setLength(0);
        }

        // You must select which room to start the adventure in. If this
        // is not selected, it will default to the first room in the
        // rooms list.
        int iStartLocation = cint(reader.readLine());                       // Start Room

        // Similar to the introduction, you may want a generic winning
        // message to appear. This will be displayed whenever any winning
        // task is executed.
        buf = reader.readLine();                                            // Winning Text
        while (!buf.equals(terminator)) {
            sb.append("\n").append(buf);
            buf = reader.readLine();                                        // Winning Text
        }
        buf = sb.toString().trim();
        if (buf.length() > 0) {
            // In ADRIFT 5, the end game text is ALWAYS displayed,
            // irrespective of whether the player won or lost. In
            // ADRIFT 4 and earlier, there is "winning text" which
            // should only be displayed if the player won (and not
            // on other occasions). I can't see a simple way to
            // convert this to the ADRIFT 5 structure, so instead
            // we use a special compatibility variable in the
            // Adventure object.
            adv.mCompatWinningText = sb.toString();
        }

        // ===========================================================
        //         TITLE, AUTHOR AND OTHER GLOBAL VARIABLES
        // -----------------------------------------------------------

        // This is the title of your adventure. Any HTML tags in
        // here won’t show up in the Runner title bar.
        adv.setTitle(reader.readLine());                                    // Adventure Title

        // That’s you! (Or someone else you wish to credit with
        // your work!)
        adv.setAuthor(reader.readLine());                                   // Author

        int sizeFactor = V380_OBJ_CAPACITY_MULT;
        int weightFactor = V380_OBJ_CAPACITY_MULT;
        int sizeExp = V380_OBJ_DEFAULT_SIZE;
        int weightExp = V380_OBJ_DEFAULT_SIZE;
        if (version < 3.9) {
            sizeFactor = cint(reader.readLine());                           // Max Carried
            weightFactor = sizeFactor;
        }

        // This is the message displayed when the player input
        // is not understood at all.
        adv.setNotUnderstood(reader.readLine());                            // Don't Understand Msg

        // This affects how most standard responses are phrased.
        // If you select First Person, you may get a response
        // such as “I am unable to do that.” If you selected
        // Second Person, the same response might be “You are
        // unable to do that.” If Third Person was selected, the
        // same response might be “Hamish is unable to do that.”
        int iPerspective = cint(reader.readLine()) + 1;                     // Perspective

        // Pretty much what it says. The exits will be listed
        // after the player views a room.
        adv.setShowExits(cbool(reader.readLine()));                         // Show Exits

        // This is the number of turns which pass whenever the
        // player types “wait”. If this is set at 3 (the default),
        // it means a character could walk three spaces, or an
        // event could run three times.
        adv.setWaitTurns(adv.safeInt(reader.readLine()));                       // # Wait Turns

        GLKLogger.debug("Title: " + adv.getTitle());
        GLKLogger.debug("Author: " + adv.getAuthor());

        if (version >= 3.9) {
            // You may well want the first room description to
            // appear at the end of the introduction. To do this,
            // select the Display first room option.
            adv.setShowFirstRoom(cbool(reader.readLine()));                 // Display First Room

            // This activates the battle system. You should see
            // extra options appear in the Player, Characters and
            // Object dialog boxes.
            adv.mCompatBattleSystem = cbool(reader.readLine());             // Battle System

            // The maximum score.
            adv.setMaxScore(cint(reader.readLine()));                       // Max Score
        } else {
            adv.setShowFirstRoom(false);
        }

        // ===========================================================
        //                   THE PLAYER CHARACTER
        // -----------------------------------------------------------
        adv.mCharacters.remove("Player");
        MCharacter player = new MCharacter(adv);
        player.setKey("Player");
        player.setCharacterType(Player);
        player.setPerspective(MPerspectiveEnumFromInt(iPerspective));
        player.mDescriptors.add("self");
        player.mDescriptors.add("myself");
        player.mDescriptors.add("me");
        MProperty p = adv.mCharacterProperties.get(PKEY_KNOWN).copy();
        p.setSelected(true);
        player.addProperty(p);
        p = adv.mCharacterProperties.get(PKEY_CHAR_POSITION).copy();
        player.addProperty(p);

        if (version >= 3.9) {
            // -------------------------------------------------------
            //               ADDITIONAL PLAYER PROPERTIES
            //               (versions 3.9 and 4.0 only)
            // -------------------------------------------------------
            // The Player must have a name. If this is not defined,
            // it will be set to “Anonymous” within the game. (It may
            // or not be important, depending on your game). Setting
            // the Player’s name will allow you to ‘”examine <Player>”
            // within the game. You can also reference this text within
            // the game using the %player% keyword.
            String name = reader.readLine();                                // Player Name
            player.setProperName(name.equals("") ?
                    "Player" : name);

            // You can allow the player to choose a name by selecting the
            // "Prompt for name" checkbox. This will default to whatever
            // is in the Name textbox.
            boolean promptName = cbool(reader.readLine());                  // Prompt For Name

            // The Player can have a description, similar to characters,
            // which can change depending on whether a specific task
            // has been completed.
            player.setDescription(new MDescription(adv,
                    convertV4FuncsToV5(adv, reader.readLine())));           // Description
            String tKey = "Task" + reader.readLine();                       // Task #
            if (!tKey.equals("Task0")) {
                String altDesc = reader.readLine();                         // Alt Desc
                MSingleDescription sd = new MSingleDescription(adv);
                sd.mDescription = altDesc;
                MRestriction rest = new MRestriction(adv);
                rest.mType = Task;
                rest.mMust = Must;
                rest.mTaskType = Complete;
                rest.mKey1 = tKey;
                sd.mRestrictions.add(rest);
                sd.mRestrictions.mBrackSeq = "#";
                sd.mDisplayWhen = StartDescriptionWithThis;
                player.getDescription().add(sd);
            }

            // The Player can be in one of three positions; standing,
            // sitting and lying. This can either be on the floor, or
            // on an object that has been defined as allowing sitting etc.
            MCharacter.MCharacterLocation playerLoc = player.getLocation();
            playerLoc.setPosition(MPositionEnumFromInt(
                    cint(reader.readLine()) + 1));                    // Position
            int onWhat = cint(reader.readLine());                           // Parent Object
            if (onWhat == 0) {
                playerLoc.setExistsWhere(AtLocation);
                playerLoc.setKey("Location" + (iStartLocation + 1));
            } else {
                playerLoc.setExistsWhere(OnObject);
                // Will adjust this below:
                playerLoc.setKey(String.valueOf(onWhat));
            }

            // You can specify the gender of the Player. Again,
            // if you want the player to choose this, select
            // Prompt. You can then create tasks which have
            // restrictions depending on whether the Player is
            // male or female.
            boolean bPromptGender = false;
            switch (adv.safeInt(reader.readLine())) {                           // #PlayerGender
                // Sex
                case 0:
                    // Male
                    player.setGender(Male);
                    break;
                case 1:
                    // Female
                    player.setGender(Female);
                    break;
                case 2:
                    // Prompt
                    player.setGender(Unknown);
                    bPromptGender = true;
                    break;
            }

            // Generate prompting tasks now.
            if (promptName || bPromptGender) {
                MTask tasPrompt = new MTask(adv);
                // Give it a unique key so it doesn't
                // disrupt events calling tasks by index:
                tasPrompt.setKey("GenTask" + (adv.mTasks.size() + 1));
                tasPrompt.setDescription("Generated task for Player prompts");
                tasPrompt.mType = MTask.TaskTypeEnum.System;
                tasPrompt.setRunImmediately(true);
                tasPrompt.setPriority(iStartMaxPriority + adv.mTasks.size() + 1);
                if (bPromptGender) {
                    MAction act = new MAction(adv, SetProperties,
                            player.getKey(), "Gender",
                            "%PopUpChoice[\"Please select player gender\", \"Male\", \"Female\"]%");
                    tasPrompt.mActions.add(act);
                }
                if (promptName) {
                    MAction act = new MAction(adv, SetProperties,
                            player.getKey(), "CharacterProperName",
                            "%PopUpInput[\"Please enter your name\", \"Anonymous\"]%");
                    tasPrompt.mActions.add(act);
                }
                adv.mTasks.put(tasPrompt.getKey(), tasPrompt);
            }

            // You must set limits for what the Player can carry
            // at any one time. If you don’t want this to apply
            // to your adventure, you must set the limits higher
            // than will be required in your game.
            //
            // You must specify the size limit (object bulk) and
            // the weight limit separately – the Player will only
            // be able to hold the minimum of both of these. You
            // can specify from 0 to 99 and from Tiny to Huge (or
            // Very Light to Very Heavy). Object sizes and weights
            // are relative, and in relation to what you specify
            // for the individual objects.
            //
            // These values are stored as follows. If the size or
            // weight is
            //
            //     y = factor * weight/size ratio ^ exp
            //
            // then then value stored in the data file is
            //
            //     x = 10 * factor + exp.
            //
            // E.g. if the weight is 99 (11 * 3 ^ 3) then the
            // value actually stored is 113 (10 * 11 + 3).
            sizeFactor = cint(reader.readLine());                      // Max Size
            sizeExp = sizeFactor % 10;
            sizeFactor = (sizeFactor - sizeExp) / 10;
            weightFactor = cint(reader.readLine());                    // Max Weight
            weightExp = weightFactor % 10;
            weightFactor = (weightFactor - weightExp) / 10;

            // -------------------------------------------------------
            //                    BATTLE SYSTEM
            // -------------------------------------------------------
            if (adv.mCompatBattleSystem) {
                reader.skipLine();                                      // iStaminaLo
                if (version >= 4) {
                    reader.skipLine();                                  // iStaminaHi
                }
                reader.skipLine();                                      // iStrengthLo
                if (version >= 4) {
                    reader.skipLine();                                  // iStrengthHi
                    reader.skipLine();                                  // iAccuracyLo
                    reader.skipLine();                                  // iAccuracyHi
                }
                reader.skipLine();                                      // iDefenseLo
                if (version >= 4) {
                    reader.skipLine();                                  // iDefenseHi
                    reader.skipLine();                                  // iAgilityLo
                    reader.skipLine();                                  // iAgilityHi
                    reader.skipLine();                                  // iRecovery
                }
            }
        } else {
            // Version 3.8 doesn't support player names, descriptions,
            // custom start locations or genders. Set appropriate
            // defaults.
            player.setProperName("Player");

            player.setDescription(new MDescription(adv,
                    "As good looking as ever."));

            MCharacter.MCharacterLocation loc = player.getLocation();
            loc.setExistsWhere(AtLocation);
            loc.setKey("Location" + (iStartLocation + 1));

            player.setGender(Unknown);
        }
        adv.mCharacters.put(player.getKey(), player);

        if (version >= 3.9) {
            // -------------------------------------------------------
            //                OTHER ADVANCED OPTIONS
            //              (versions 3.9 and 4.0 only)
            // -------------------------------------------------------
            // This activates the off-cardinal directions within
            // the room movement tab:
            adv.mCompatCompassPoints = 8 + 4 *
                    cint(reader.readLine());                            // Eight Point Compass

            // You can also disable some of the features within the
            // Runner application. These are:

            // Debugger – you probably want to do this when the game
            // is distributed:
            adv.setEnabled(Debugger, !cbool(reader.readLine()));        // No Debug?
            // Score being displayed – useful if score is not relevant
            // in your game:
            adv.setEnabled(Score, !cbool(reader.readLine()));           // No Score Notify?
            // Map – for the more traditional gaming environment:
            adv.setEnabled(Map, !cbool(reader.readLine()));             // No Map?
            // Auto complete – to prevent giving away extra
            // information:
            adv.setEnabled(AutoComplete, !cbool(reader.readLine()));    // No Auto Complete?
            // Control Panel – can give away useful information such
            // as objects in the room:
            adv.setEnabled(ControlPanel, !cbool(reader.readLine()));    // No Control Panel?
            // Mouse clicks in Runner - prevents menus appearing
            // when clicking on objects or right-clicking
            adv.setEnableMenu(!cbool(reader.readLine()));               // No Mouse?

            // This will enable small icons near most pieces of text.
            // You can attach sound and graphics to anywhere you see
            // these:
            adv.mCompatSound = cbool(reader.readLine());                // Sound?
            adv.mCompatGraphics = cbool(reader.readLine());             // Graphics?

            // -------------------------------------------------------
            //      RESOURCES FOR START TEXT AND WINNING TEXT
            //             (versions 3.9 and 4.0 only)
            // -------------------------------------------------------
            for (int i = 0; i < 2; i++) {
                MDescription d = (i == 0) ?
                        adv.getIntroduction() : adv.getEndGameText();
                loadResource(adv, reader, version,
                        (d != null) ? d.get(0) : null);
            }

            if (version >= 4) {
                // This enables a user status box. This appears at
                // the bottom of the Runner screen and can contain
                // any text. In order to change this in the middle
                // of an adventure, you would need to use a text
                // variable, or use an ALR-variable combination.
                // A suggestion for this might be %turns%.
                reader.skipLine();                                      // Status Box?
                adv.setUserStatus(reader.readLine());                   // Status Box Text
            }

            adv.mCompatSizeRatio = cint(reader.readLine());             // Size ratio
            adv.mCompatWeightRatio = cint(reader.readLine());           // Weight ratio

            if (version >= 4) {
                reader.skipLine();                                      // Embedded?
            }
        }

        // Now that we have the ratios, we can finalise the player
        // max size and weight information.
        if (adv.mCharacterProperties.containsKey(PKEY_MAX_BULK)) {
            p = adv.mCharacterProperties.get(PKEY_MAX_BULK).copy();
            p.setSelected(true);
            p.setValue(String.valueOf((sizeFactor *
                    Math.pow(adv.mCompatSizeRatio, sizeExp))));
            player.addProperty(p);
        }
        if (adv.mCharacterProperties.containsKey(PKEY_MAX_WEIGHT)) {
            p = adv.mCharacterProperties.get(PKEY_MAX_WEIGHT).copy();
            p.setSelected(true);
            p.setValue(String.valueOf(weightFactor *
                    Math.pow(adv.mCompatWeightRatio, weightExp)));
            player.addProperty(p);
        }
    }

    private static void setPlayerPosition(@NonNull MAdventure adv) {
        MCharacter.MCharacterLocation locPlayer = adv.getPlayer().getLocation();
        if (locPlayer.getExistsWhere() == OnObject) {
            switch (locPlayer.getPosition()) {
                case Standing:
                    locPlayer.setKey(getObjectKey(adv,
                            cint(locPlayer.getKey()) - 1, Standable));
                    break;
                case Sitting:
                    locPlayer.setKey(getObjectKey(adv,
                            cint(locPlayer.getKey()) - 1, Sittable));
                    break;
                case Lying:
                    locPlayer.setKey(getObjectKey(adv,
                            cint(locPlayer.getKey()) - 1, Lieable));
                    break;
            }
        }
    }

    private static void fixGroups(@NonNull MAdventure adv) {
        // Sort out anything which needed groups defined
        Collection<MGroup> groups = adv.mGroups.values();
        for (MCharacter c : adv.mCharacters.values()) {
            for (MWalk w : c.mWalks) {
                String d = w.getDescription();
                for (MGroup g : groups) {
                    if (d.contains("<" + g.getKey() + ">")) {
                        w.setDescription(d.replace("<" + g.getKey() + ">", g.getName()));
                    }
                }
            }
        }
    }

    static boolean loadOlder(@NonNull MAdventure adv, @NonNull RandomAccessFile fileStream,
                             double version) {
        long msStart = System.currentTimeMillis();

        try {
            // Check that the core library properties have already
            // been loaded
            if (adv.mAllProperties.size() == 0) {
                GLKLogger.error("You must select at least one library within " +
                        "Generator > File > Settings > Libraries before loading " +
                        "ADRIFT version" + String.valueOf(version) + " adventures.");
                return false;
            }
            StringBuilder sPropCheck = new StringBuilder();
            for (String sProperty : MANDATORY_PROPERTIES) {
                if (!adv.mAllProperties.containsKey(sProperty)) {
                    sPropCheck.append(sProperty).append("\n");
                }
            }
            if (!sPropCheck.toString().equals("")) {
                GLKLogger.error("Library must contain the following " +
                        "properties before loading ADRIFT version" +
                        String.valueOf(version) + " files:" + "\n" + sPropCheck);
                return false;
            }

            V4Reader reader = new V4Reader(fileStream, version);

            int iStartMaxPriority = getMaxPriority(adv);
            int iStartLocations = adv.mLocations.size();
            int iStartObs = adv.mObjects.size();
            int iStartTask = adv.mTasks.size();
            int iStartChar = adv.mCharacters.size();

            // One point to note, is that if there are more than one task
            // which could execute given a specific command, the first
            // task in the list which satisfies all the restrictions will
            // execute. This means that the ordering of tasks in the list
            // is significant. This can be useful, as you can make one
            // task execute the first time you type something, then the
            // next time you type it, the second task will execute if
            // the first one is not repeatable. It can also be useful
            // if you want to restrict a task further than the restriction
            // options will allow, as by making both tasks identical
            // apart from adding an extra restriction on the first one
            // will have the effect of adding a restriction to the
            // second task if the restriction in the first is not so.
            // I.e. if the task is "touch iron", you could add a
            // restriction in the first task of "player must be
            // wearing a glove", so if the player was wearing the
            // glove, the first task would execute.  If the player
            // wasn't wearing the glove, the second task would
            // execute, and you could give a different response.
            adv.mTaskExecutionMode = HighestPriorityPassingTask;
            mWithStates.clear();

            long t2 = System.currentTimeMillis();
            GLKLogger.debug("Took " + (t2 - msStart) + " ms to init reader.");

            // ===========================================================
            //                    HEADERS AND GLOBALS
            // -----------------------------------------------------------
            loadGlobals(adv, reader, version, iStartMaxPriority);
            long t1 = System.currentTimeMillis();
            GLKLogger.debug("Loaded header and globals in " + (t1 - t2) + " ms.");

            // ===========================================================
            //                        LOCATIONS
            // -----------------------------------------------------------
            int nLocs = cint(reader.readLine());             // # locations
            int iX = (version < 4) ? 2 : 3;
            int[][][] iLocations = new int[nLocs + 1][12][iX + 1]; // Temp Store
            ArrayList<MLocation> newLocs = new ArrayList<>();
            adv.mLocations.load(reader, version, iLocations, newLocs, nLocs);
            t2 = System.currentTimeMillis();
            GLKLogger.debug("Loaded " + adv.mLocations.size() +
                    " location(s) in " + (t2 - t1) + " ms.");

            // ===========================================================
            //                         OBJECTS
            // -----------------------------------------------------------
            HashMap<String, String> dictDodgyStates = new HashMap<>();
            HashMap<MObject, MProperty> dodgyStates = new HashMap<>();
            ArrayList<MObject> newObs = new ArrayList<>();
            adv.mObjects.load(reader, version, newObs, nLocs,
                    iStartLocations, mWithStates, dictDodgyStates, dodgyStates);
            t1 = System.currentTimeMillis();
            GLKLogger.debug("Loaded " + adv.mObjects.size() +
                    " object(s) in " + (t1 - t2) + " ms.");

            // Finish converting locations and objects
            setPlayerPosition(adv);
            for (MLocation loc : newLocs) {
                loc.fixDesciptionRestrictions(dodgyStates);
            }
            for (MObject ob : newObs) {
                ob.fixPropertyValues(iStartObs, version);
            }
            int iLoc = 0;
            int numLoc = adv.mLocations.size();
            for (MLocation loc : newLocs) {
                iLoc++;
                loc.fixMovementRestrictions(iLocations, iLoc,
                        numLoc, iStartTask, dictDodgyStates, version);
            }

            // ===========================================================
            //                          TASKS
            // -----------------------------------------------------------
            String[] locNames = new String[nLocs];
            for (int i = 0; i < nLocs; i++) {
                locNames[i] = "Location" + ((i + 1) + iStartLocations);
            }
            adv.mTasks.load(reader, version, nLocs, iStartLocations, iStartTask,
                    iStartChar, iStartMaxPriority, dictDodgyStates, dodgyStates, locNames);
            t2 = System.currentTimeMillis();
            GLKLogger.debug("Loaded " + adv.mTasks.size() +
                    " task(s) in " + (t2 - t1) + " ms.");

            // ===========================================================
            //                          EVENTS
            // -----------------------------------------------------------
            adv.mEvents.load(reader, version, nLocs, iStartMaxPriority);
            t1 = System.currentTimeMillis();
            GLKLogger.debug("Loaded " + adv.mEvents.size() +
                    " event(s) in " + (t1 - t2) + " ms.");

            // ===========================================================
            //                        CHARACTERS
            // -----------------------------------------------------------
            adv.mCharacters.load(reader, version);
            t2 = System.currentTimeMillis();
            GLKLogger.debug("Loaded " + adv.mCharacters.size() +
                    " character(s) in " + (t2 - t1) + " ms.");

            // ===========================================================
            //                          GROUPS
            // -----------------------------------------------------------
            adv.mGroups.load(reader, nLocs, locNames);
            t1 = System.currentTimeMillis();
            GLKLogger.debug("Loaded " + adv.mGroups.size() +
                    " group(s) in " + (t1 - t2) + " ms.");
            fixGroups(adv);

            // ===========================================================
            //                         SYNONYMS
            // -----------------------------------------------------------
            adv.mSynonyms.load(reader);
            t2 = System.currentTimeMillis();
            GLKLogger.debug("Loaded " + adv.mSynonyms.size() +
                    " synonym(s) in " + (t2 - t1) + " ms.");

            if (version >= 3.9) {
                // =======================================================
                //                      VARIABLES
                //                     (3.9+ only)
                // -------------------------------------------------------
                adv.mVariables.load(reader, version);
                t1 = System.currentTimeMillis();
                GLKLogger.debug("Loaded " + adv.mVariables.size() +
                        " variable(s) in " + (t1 - t2) + " ms.");
            }
            adv.mTasks.fixOlderVars();

            if (version >= 3.9) {
                // =======================================================
                //                         ALRS
                //                     (3.9+ only)
                // -------------------------------------------------------
                adv.mALRs.load(reader);
                t2 = System.currentTimeMillis();
                GLKLogger.debug("Loaded " + adv.mALRs.size() +
                        " ALR(s) in " + (t2 - t1) + " ms.");

                boolean bSetFont = cbool(reader.readLine());     // BCustomFont
                if (bSetFont) {
                    String sFont = reader.readLine();               // ?BCustomFont:$FontNameSize
                    if (sFont.contains(",")) {
                        adv.setDefaultFontName(sFont.split(",")[0]);
                        adv.setDefaultFontSize(adv.safeInt(sFont.split(",")[1]));
                    }
                }

                if (version >= 4) {
                    reader.setMediaOffsets(adv.mV4Media);
                }
            }

            adv.mCompatCompileDate = reader.readLine();         // Compile Date

            adv.mPassword = (version < 4) ?
                    reader.readLine() :                          // Password
                    reader.getPassword();

            // Make sure all the 'seen's are set
            for (MCharacter ch : adv.mCharacters.values()) {
                ch.moveTo(ch.getLocation());
            }

            GLKLogger.error("loadOlder(): TODO: set map");
            //UserSession.Map.Map = .Map

            long msEnd = System.currentTimeMillis();
            GLKLogger.debug("Took " + (msEnd - msStart) + " ms to load adventure.");
            return true;

        } catch (Exception ex) {
            adv.mView.errMsg("Error loading Adventure", ex);
            return false;
        }
    }

    @NonNull
    public static String getObjectKey(@NonNull MAdventure adv,
                                      int comboIndex, ComboEnum combo) {
        int iMatching = 0;
        int i = 1;
        String sKey;
        MObject ob = null;

        try {
            while (iMatching <= comboIndex && i < adv.mObjects.size() + 1) {
                sKey = "Object" + i;
                ob = adv.mObjects.get(sKey);
                switch (combo) {
                    case Dynamic:
                        if (!ob.isStatic()) {
                            iMatching++;
                        }
                        break;
                    case WithState:
                        if (mWithStates.contains(sKey)) {
                            iMatching++;
                        }
                        break;
                    case WithStateOrOpenable:
                        if (mWithStates.contains(sKey) || ob.isOpenable()) {
                            iMatching++;
                        }
                        break;
                    case Surface:
                        if (ob.hasSurface()) {
                            iMatching++;
                        }
                        break;
                    case Container:
                        if (ob.isContainer()) {
                            iMatching++;
                        }
                        break;
                    case Wearable:
                        if (ob.isWearable()) {
                            iMatching++;
                        }
                        break;
                    case Sittable:
                        if (ob.isSittable()) {
                            iMatching++;
                        }
                        break;
                    case Standable:
                        if (ob.isStandable()) {
                            iMatching++;
                        }
                        break;
                    case Lieable:
                        if (ob.isLieable()) {
                            iMatching++;
                        }
                        break;
                    case SurfaceContainer:
                        if (ob.isContainer() || ob.hasSurface()) {
                            iMatching++;
                        }
                        break;
                }
                i++;
            }

            return (ob != null) ? ob.getKey() : "";

        } catch (Exception ex) {
            adv.mView.errMsg("getObjectKey error", ex);
        }

        return "";
    }

    private static boolean fixInventoryOverride(@NonNull MAdventure adv,
                                                @NonNull MTask tas,
                                                @NonNull MTask parent,
                                                @NonNull String cmd) {
        // Inventory override only seems to be used in Wrecked,
        // Task44. Behaviour seems to be different to the other
        // overrides - always seems to be a RunAfter type override.
        for (com.google.code.regexp.Pattern pat : parent.getPatterns().get(0)) {
            com.google.code.regexp.Matcher m = pat.matcher(cmd);
            boolean isGetOverride = m.matches();
            if (isGetOverride) {
                GLKLogger.debug("try to fix inventory task " +
                        tas.getKey() + "'...");
                tas.mType = Specific;
                tas.mGeneralKey = parent.getKey();
                tas.mSpecificOverrideType = AfterTextOnly;
                for (MRestriction r : tas.mRestrictions) {
                    r.mMessage = new MDescription(adv);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean fixMovementOverride(@NonNull MAdventure adv,
                                               @NonNull MTask tas,
                                               @NonNull MTask parent,
                                               @NonNull String cmd) {
        // ---------------------------------------------------------
        //  Try to autodetect specific tasks overriding PlayerMovement
        // ---------------------------------------------------------
        // Examples of required behaviour:
        //
        // => (v3.8) Wrecked Task97 overrides the go in
        //    command. If its restrictions pass (player wearing
        //    scuba outfit), run its completion message (CT)
        //    and do NOT execute parent text (PT) or actions
        //    (PA). If it fails, execute PT and PA.
        //    (CT? = true, CA? = false, CR = true).
        //
        // => (v4.0) Lair of Vampire Task5 overrides the move
        //    east command. If its restrictions pass (player
        //    is at cell and doora (locked) variable is 0), run
        //    its completion message (CT) and do NOT execute
        //    parent text (PT) or actions (PA). If it fails,
        //    execute PT and PA.
        //    (CT? = true, CA? = false, CR = true).
        //
        //    Task328 overrides the move east command. It does
        //    not have any restrictions so should always execute
        //    PT and PA.
        //    (CT? = true, CA? = false, CR? = false).
        //
        for (String d : adv.mDirectionNames.values()) {
            for (String d2 : d.toLowerCase().split("/")) {
                boolean isGetOverride = cmd.equals(d2);
                if (isGetOverride) {
                    GLKLogger.debug("Trying to fix movement task " +
                            tas.getKey() + " with direction '" + d2 + "'...");

                    // If the child task has
                    // restrictions or actions, then
                    // we want it to run if those
                    // restrictions pass. We *don't*
                    // want it to check the parent's
                    // restrictions first - and
                    // it should *not* attempt to run the
                    // parent's actions afterwards.
                    // If the child task fails any of
                    // its restrictions, we still
                    // want to give the parent a
                    // chance to run. To do this, we
                    // need to ensure that any failure
                    // messages associated with the
                    // child task's restrictions are
                    // blank - recall for older games
                    // task execution order is highest
                    // priority passing task.
                    if (adv.mVersion < 4) {
                        // From the v3.8 and v3.9 tutorial -
                        //
                        // "It is also important to note that only a
                        //  successful task will override the system
                        //  command. If all the conditions on the task
                        //  are not met, then the default system
                        //  response will be displayed."
                        //
                        // To support this behaviour with the ADRIFT
                        // 5 engine, we need to ensure that none of the
                        // restrictions have failure text. That then
                        // means that if the task fails, the engine
                        // will continue trying to run any lower
                        // priority matching tasks (including,
                        // eventually, the library task).
                        //
                        // Task57 of Cave of Wonders (v3.8) uses
                        // a restriction with non-blank text that
                        // should display (when character is
                        // not wearing a poppy can't enter the
                        // graveyard).
                        for (MRestriction r : tas.mRestrictions) {
                            String tmp = r.mMessage.toString(true).toLowerCase().trim();
                            if (tmp.equals("x")) {
                                r.mMessage = new MDescription(adv);
                            }
                        }
                    } else {
                        // From the 4.0 manual -
                        //
                        // "If you have defined a task that overrides a system
                        // command and the task fails because a restriction is not
                        // met, then one of two things will happen:
                        //
                        // (1) If you have put a message in the else display part of
                        // the restriction, this will be displayed and the system
                        // command will be overridden.
                        //
                        // (2) If the else display message is blank, the system
                        // command will execute normally."

                        // In this case, we don't need to make any changes as this
                        // is the task execution behaviour of the ADRIFT 5 engine
                        // (non-blank failure text will override libraries).
                    }

                    if (tas.mRestrictions.size() == 0 &&
                            tas.mActions.size() == 0) {
                        // If the child task doesn't have
                        // restrictions or actions, then we
                        // want it to run if the parent's
                        // restrictions pass. It should then
                        // display whatever completion text
                        // it has (in which case this text
                        // overrides any completion text
                        // provided by the parent) and attempt
                        // to run the parent's actions.
                        boolean hasCT = !tas.getCompletionMessage().toString(true).equals("");
                        tas.mType = Specific;
                        tas.mGeneralKey = parent.getKey();
                        tas.mSpecificOverrideType =
                                hasCT ? BeforeActionsOnly : BeforeTextAndActions;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean fixGetObjectOverride(@NonNull MAdventure adv,
                                                @NonNull MTask tas,
                                                @NonNull MTask parent,
                                                @NonNull String cmd) {
        // ---------------------------------------------------------
        //  Try to autodetect specific tasks overriding TakeObjects
        // ---------------------------------------------------------
        // Examples of required behaviour:
        //
        // => (v3.8) Wrecked Task53 overrides TakeObjects. If
        //    its restrictions pass, run its completion
        //    message (CT) and do NOT execute parent text (PT)
        //    or actions (PA). If it fails, execute PT and PA.
        //    (CT? = true, CA? = false).
        //
        // => (v3.9) Lost tomb Task9 does NOT override
        //    TakeObjects as TakeObjects has restrictions that
        //    are broader than Task9.
        //
        // => (v4.0) Humbug Task163 overrides TakeObjects. If
        //    its restrictions pass, execute PT and PA. If they
        //    fail, print the fail message (CF) and do NOT
        //    execute PT or PA.
        //    (CT? = false).
        //
        // In short, we want:
        //
        //   if (CR) {
        // #ifdef CT
        //     run CT
        // #else
        //     if (PR) {
        //       run PA
        //       run PT
        //     } else {
        //       run PF
        //     }
        // #endif
        //   } else {
        // #ifdef CT
        //     run PA
        //     run PT
        // #else
        //     run CF
        // #endif
        //   }
        //
        // Strategy: Do a full override. If the child
        // does not have completion text, then also
        // add a ExecuteTask Parent to its list of
        // actions. Otherwise, ensure it has no fail text, so
        // that if it fails the task executor will move onto
        // next matching task (or the library function).

        // In an attempt to avoid false positives we only look
        // at the first pattern - e.g. Task13 of Cave of Wonders
        // matches "use pump", "pump handle", "move handle", etc.,
        // but also "get water".
        for (com.google.code.regexp.Pattern pat : parent.getPatterns().get(0)) {
            com.google.code.regexp.Matcher m = pat.matcher(cmd);
            boolean isGetOverride = m.matches();
            if (isGetOverride) {
                GLKLogger.debug("Trying to fix get task " + tas.getKey() +
                        " with object ref '" + m.group(2).trim() + "'...");

                if (adv.mVersion < 4) {
                    // From the v3.8 tutorial -
                    //
                    // "It is also important to note that only a
                    //  successful task will override the system
                    //  command. If all the conditions on the task
                    //  are not met, then the default system
                    //  response will be displayed."
                    //
                    // To support this behaviour with the ADRIFT
                    // 5 engine, we need to ensure that none of the
                    // restrictions have failure text. That then
                    // means that if the task fails, the engine
                    // will continue trying to run any lower
                    // priority matching tasks (including,
                    // eventually, the library task).
                    for (MRestriction r : tas.mRestrictions) {
                        String tmp = r.mMessage.toString(true).toLowerCase().trim();
                        if (tmp.equals("x")) {
                            r.mMessage = new MDescription(adv);
                        }
                    }
                } else {
                    // From the 4.0 manual -
                    //
                    // "If you have defined a task that overrides a system
                    // command and the task fails because a restriction is not
                    // met, then one of two things will happen:
                    //
                    // (1) If you have put a message in the else display part of
                    // the restriction, this will be displayed and the system
                    // command will be overridden.
                    //
                    // (2) If the else display message is blank, the system
                    // command will execute normally."

                    // In this case, we don't need to make any changes as this
                    // is the task execution behaviour of the ADRIFT 5 engine
                    // (non-blank failure text will override libraries).
                }


                // Don't override any tasks that have
                // an action to move a dynamic object
                // to the player that starts off hidden -
                // the TakeObjects parent requires objects
                // to visible before allowing them to be picked up.
                for (MAction a : tas.mActions) {
                    if (a.mType == MoveObject) {
                        MObject ob = adv.mObjects.get(a.mKey1);
                        if (ob != null && !ob.isStatic()) {
                            if (ob.getLocation().mDynamicExistWhere ==
                                    MObject.MObjectLocation.DynamicExistsWhereEnum.Hidden) {
                                GLKLogger.debug("=> skip task that moves hidden object.");
                                return true;
                            }
                        }
                    }
                }

                // We've got to work out if a particular object is referenced
                // First look to see if the command makes it unique
                String cmdOb = (m.groupCount() > 1 ? m.group(2).trim() : "");
                int nObsFound = 0;
                String foundKey = "";
                nextOb:
                for (MObject ob : adv.mObjects.values()) {
                    for (String name : ob.getNames()) {
                        if (cmdOb.equals(name)) {
                            if (!ob.isStatic()) {
                                nObsFound++;
                                foundKey = ob.getKey();
                                continue nextOb;
                            } else {
                                // Don't override any tasks where the
                                // player is getting something and that
                                // something exists as a static object
                                GLKLogger.debug("=> skip task where player getting static object.");
                                return true;
                            }
                        }
                    }
                }

                if (nObsFound > 1) {
                    // We have more than one possible object.

                    // Let's look in the restrictions/actions to see if
                    // they point us to an exact object
                    for (MRestriction r : tas.mRestrictions) {
                        MObject ob = adv.mObjects.get(r.mKey1);
                        if (ob != null && cmdOb.equals(ob.getNames().get(0))) {
                            foundKey = r.mKey1;
                        } else {
                            ob = adv.mObjects.get(r.mKey2);
                            if (ob != null && cmdOb.equals(ob.getNames().get(0))) {
                                foundKey = r.mKey2;
                            }
                        }
                        if (!foundKey.equals("")) {
                            break;
                        }
                    }

                    if (foundKey.equals("")) {
                        // Nothing in the restrictions, try the actions
                        for (MAction a : tas.mActions) {
                            MObject o = adv.mObjects.get(a.mKey1);
                            if (o != null && cmdOb.equals(o.getNames().get(0))) {
                                foundKey = a.mKey1;
                            } else {
                                o = adv.mObjects.get(a.mKey2);
                                if (o != null && cmdOb.equals(o.getNames().get(0))) {
                                    foundKey = a.mKey2;
                                }
                            }
                            if (!foundKey.equals("")) {
                                break;
                            }
                        }
                    }
                }

                if (!foundKey.equals("")) {
                    boolean hasCT = !tas.getCompletionMessage().toString(true).equals("");

                    if (!hasCT) {
                        // From the v4 manual:
                        //
                        // "If a task successfully matches a player command but
                        //  has no output text, it will still execute as per normal,
                        //  but instead of bypassing the normal response to the
                        //  command, it will continue to be executed as if not being
                        //  overridden by the task at all."
                        MAction act = new MAction(adv);
                        act.mType = SetTasks;
                        act.mKey1 = parent.getKey();
                        act.mStrValue = foundKey;
                        tas.mActions.add(act);
                    } else {
                        // If a task has output text then it completely overrides
                        // the system command.
                        tas.mType = Specific;
                        tas.mSpecificOverrideType = MTask.SpecificOverrideTypeEnum.Override;
                        tas.mGeneralKey = parent.getKey();

                        // Ensure that the references of this specific task
                        // match the references of the general get task.
                        // This fixes problems like the
                        // following - in Wrecked, go w, type "get hook". You
                        // shouldn't be able to - the dog should stop you. But
                        // without the below it doesn't, because the specific
                        // task checks a restriction relating to ReferencedObject
                        // whereas the general task only provides ReferencedObjects
                        // (%objects%). Hence that restriction fails, which means
                        // the completion message (dog stops you) doesn't display,
                        // which means it falls through to the library task and
                        // allows the user to get the hook even though they shouldn't
                        // be able to.
                        tas.find("%object1%", "%objects%");
                        tas.find("%object%", "%objects%");
                        for (MRestriction r : tas.mRestrictions) {
                            if (r.mKey1.equals("ReferencedObject")) {
                                r.mKey1 = "ReferencedObjects";
                            }
                            if (r.mKey2.equals("ReferencedObject")) {
                                r.mKey2 = "ReferencedObjects";
                            }
                        }

                        tas.mSpecifics = new ArrayList<>();
                        MTask.MSpecific s = new MTask.MSpecific();
                        s.mMultiple = false;
                        s.mKeys.add(foundKey);
                        tas.mSpecifics.add(s);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static String removeParentheses(@NonNull String s) {
        StringBuilder buffer = new StringBuilder();

        int parenthesisCounter = 0;

        for (char c : s.toCharArray()) {
            if (c == '{')
                parenthesisCounter++;
            if (c == '}')
                parenthesisCounter--;
            if (!(c == '{' || c == '}') && parenthesisCounter == 0)
                buffer.append(c);
        }

        return buffer.toString();
    }

    static void convertLibraryTaskOverrides(@NonNull MAdventure adv) {
        //     Version 3.8 manual: "It is also important to note that only a
        //     successful task will override the system command. If all the
        //     conditions on the task are not met, then the default system
        //     response will be displayed. If you require to display a
        //     different message when the task is executed and all the
        //     conditions haven't been met, you will have to add another
        //     task without the restrictions."

        // -----------------------------------------------------------
        // The vast majority of all the system commands (i.e. commands
        // that ADRIFT understands without having to explicitly define
        // tasks for, such as getting and dropping objects) can all be
        // overridden with your own tasks. This is necessary to allow
        // you to customise the adventure and do more advanced things.
        // To override the system commands, simply create a task with
        // a command which would normally be understood by the parser.
        //
        // One example where this is useful is if you have a fragile
        // object such as a vase. If you type "drop vase", then the
        // default system command moves the object to the current
        // room the Player is in. You may want to make the object
        // break if the Player tries to drop it, so you would define
        // a task such as "drop * vase", then give the reply "You
        // drop the fragile vase, but it smashes on impact with the
        // ground.", then move the object to hidden.
        //
        // The difficulty with overriding the system commands is to
        // cover all possible ways that it can be phrased. In the
        // example above, if that were the only definition in the
        // task and if the player typed "put vase down", the vase
        // would still be moved to the current room instead of the
        // task running.
        //
        // Overriding the standard commands for taking and dropping
        // objects are slightly different from other commands. The
        // reason for this is in case the player types “take all”
        // or “drop all”. You would then want all objects in the
        // room to be taken (or dropped) with the exception of
        // whichever ones you’ve defined tasks for. You would then
        // want the task to run for these. To enable this to work
        // efficiently, you must define the task as simply
        // “get * <object>” (formatted for your particular
        // circumstance), or “drop * <object>”. You don’t have to
        // worry about synonyms for get and drop as ADRIFT will
        // automatically cater for these, but it is essential to
        // use these keywords.
        //
        // If you have defined a task that overrides a system
        // command and the task fails because a restriction is not
        // met, then one of two things will happen:
        //
        // (1) If you have put a message in the else display part of
        // the restriction, this will be displayed and the system
        // command will be overridden.
        //
        // (2) If the else display message is blank, the system
        // command will execute normally.
        //
        // NB. If a task successfully matches a player command but
        // has no output text, it will still execute as per normal,
        // but instead of bypassing the normal response to the
        // command, it will continue to be executed as if not being
        // overridden by the task at all.
        //
        // The verbs that ADRIFT will respond to are:
        //
        //  ask, attack, block, break, buy, bye, can, chop, clean, clear,
        //  climb, close, cp, cry, cut, dance, date, destroy, dir,
        //  directions, down, drink, drop, e, east, eat, enter, empty, ex,
        //  exam, examine, exits, feed, feel, fight, find, fix, fly, get,
        //  give, go, hit, how, hum, i, inv, inventory, jump, kick, kill,
        //  kiss, l, leave, lie, lift, light, listen, ln, locate, lock,
        //  look, ls, move, mv, n, ne, no, north, northeast, north-east,
        //  northwest, north-west, nw, open, out, panel, past, pick,
        //  press, previous, pull, punch, push, put, read, remove, run, s,
        //  say, se, sell, shake, shoot, shout, sing, sit, slap, sleep,
        //  smash, smell, south, southeast, south-east, southwest, south-
        //  west, stab, stand, status, stop, suck, sw, take, talk, thank,
        //  throw, time, touch, turn, turns, unblock, undo, unlock, up, w,
        //  wait, wash, wear, west, what, when, where, whistle, who, why,
        //  x, yes, z
        //
        // Other commands which have functionality within ADRIFT are:
        //
        //  !, !!, about, again, author, clr, cls, commands, control,
        //  control panel, control-panel, count, end, endgame, g, help,
        //  hint, history, info, information, last, num, quit, restore,
        //  save, score, version
        // -----------------------------------------------------------

        // Detect and fix overrides of these critical system commands:
        //
        //     (*) get * <object>
        //
        //     (*) drop * <object>
        //
        //     down / e / east / n / ne / no / north / northeast /
        //     north-east / northwest / north-west / nw / out / s /
        //     se / south / southeast / south-east / southwest /
        //     south-west / sw / up / w / west / in
        //
        //     save / restore / quit
        MTask tasGet = adv.mTasks.get("TakeObjects");
        MTask tasMove = adv.mTasks.get("PlayerMovement");
        MTask tasInv = adv.mTasks.get("Inventory");

        adv.mTasks.createTaskReferenceLists();

        for (MTask tas : adv.mTasks.values()) {
            if (tas.getIsLibrary()) {
                break;
            }
            if (tas.mType != General) {
                // only override general tasks
                continue;
            }

            for (String cmd : tas.mCommands) {
                // try to convert advanced command format
                // to a string. Remove anything enclosed in {}
                // and only pick the first option of anything
                // in [../...]
                cmd = cmd.replaceAll("\\*", "");
                cmd = removeParentheses(cmd);
                cmd = cmd.replaceAll("\\[(.*?)/[^\\s]*\\]", "$1");
                if (fixGetObjectOverride(adv, tas, tasGet, cmd)) {
                    break;
                }
                if (fixMovementOverride(adv, tas, tasMove, cmd)) {
                    break;
                }
                if (fixInventoryOverride(adv, tas, tasInv, cmd)) {
                    break;
                }
            }
        }

        // ------------------------------------------------
        //      Make ExamineCharacter a system task
        // ------------------------------------------------
        MTask tasExamineChar = adv.mTasks.get("ExamineCharacter");
        if (tasExamineChar != null) {
            // Make it a system task, i.e. don't run events
            tasExamineChar.mIsSystemTask = true;
        }
    }

    static void tweakTasksForV4(@NonNull MAdventure adv) {
        // "Tweak" any v5 library tasks that are different from v4
        // TODO: fix up other messages
        MTask t = adv.mTasks.get("GiveObjectToChar");
        if (t != null) {
            t.setCompletionMessage(new MDescription(adv,
                    "%CharacterName[%character%, subject]% doesn't seem interested in %objects%.Name."));
            t.mActions.clear();
        }

        t = adv.mTasks.get("Look");
        if (t != null) {
            t.mCommands.set(0, "[look/l]{ room}");
            t.mCommands.add("[x/examine] room");
        }

        adv.searchAndReplace("Sorry, I'm not sure which object or character you are trying to examine.",
                "You see no such thing.", true);

        // added by Tim for better ALR compatibility with v4 games
        t = adv.mTasks.get("Jump");
        if (t != null) {
            t.setCompletionMessage(new MDescription(adv, "Wheee-boinng."));
        }
    }

    public enum ComboEnum {
        Dynamic,                // 0
        WithState,              // 1
        WithStateOrOpenable,    // 2
        Surface,                // 3
        Container,              // 4
        Wearable,               // 5
        Sittable,               // 6
        Standable,              // 7
        Lieable,                // 8
        SurfaceContainer        // 99   - added by TCC, only used by v3.8 games
    }

    public static class V4Reader {
        /**
         * All locales other than the default (WinLatin, Windows code-page 1252).
         * which we will try to auto-detect. Currently the only other charset
         * we support is Windows code-page 1251 (WinCyrillic)
         */
        private static final String AVAILABLE_ALT_LOCALES[] = {"windows-1251"};

        /**
         * Signatures for available alternative locales. By "signature",
         * like SCARE, we mean a three-byte representation of a month
         * string.
         * <p>
         * Should be one set of 3 byte signatures for each
         * name in AVAILABLE_ALT_LOCALES.
         */
        private static final int AVAILABLE_ALT_LOCALE_SIGS[][][] = {
                // Locale for Cyrillic (windows-1251).  The signatures in this locale are month names in
                // both mixed case and lowercase Russian Cyrillic:
                {{223, 237, 226}, {212, 229, 226}, {204, 224, 240}, {192, 239, 240},
                        {204, 224, 233}, {200, 254, 237}, {200, 254, 235}, {192, 226, 227},
                        {209, 229, 237}, {206, 234, 242}, {205, 238, 255}, {196, 229, 234},
                        {255, 237, 226}, {244, 229, 226}, {236, 224, 240}, {224, 239, 240},
                        {236, 224, 233}, {232, 254, 237}, {232, 254, 235}, {224, 226, 227},
                        {241, 229, 237}, {238, 234, 242}, {237, 238, 255}, {228, 229, 234}}
        };

        private static int SIGNATURE_LENGTH = 3;
        private final String mData;
        private final int mDataLen;
        private int mCurPos = 0;

        private final int mZipLen;
        private String mPassword;

        V4Reader(@NonNull RandomAccessFile fileStream, double version) throws IOException {
            // Read the game bytes in from the file (unzipping and
            // deobfuscating as necessary)
            byte[] bAdvZLib;
            byte[] bAdventure;

            if (version < 4) {
                fileStream.seek(0);
                byte[] bRawData = new byte[(int) fileStream.length()];
                mZipLen = 0;
                fileStream.read(bRawData);
                bAdventure = decode(bRawData, 0, bRawData.length);
            } else {
                fileStream.skipBytes(2); // CrLf
                byte[] bSize = new byte[8];
                fileStream.read(bSize);
                long lSize = Long.valueOf(new String(bSize, "windows-1252"));
                bAdvZLib = new byte[(int) (lSize - 23)];
                mZipLen = bAdvZLib.length;
                fileStream.read(bAdvZLib);
                byte[] bPass = new byte[12];
                fileStream.read(bPass);
                mPassword = new String(decode(bPass,
                        lSize + 1, bPass.length), "windows-1252");
                ByteArrayOutputStream zStream = new ByteArrayOutputStream();
                if (!decompress(ByteBuffer.wrap(bAdvZLib), zStream)) {
                    throw new IOException("Could not decompress v4 game");
                }
                bAdventure = zStream.toByteArray();
            }

            String charset = detectLocale(bAdventure, version);
            if (charset == null) {
                // default to Latin1
                charset = "windows-1252";
            }

            GLKLogger.debug("Reading game file using charset '" + charset + "'");
            mData = new String(bAdventure, charset);
            mDataLen = mData.length();
        }

        @Nullable
        private static String detectLocale(final byte[] bData, double version) {
            // Like SCARE, we read the compile date and then attempt to use
            // that to determine the text encoding
            int pos1, pos2;

            // for version 4 date is last line of file
            // for version < 4 date is second last line of file
            boolean first = true;
            pos2 = bData.length - 2;
            for (pos1 = pos2; pos1 >= 0; pos1--) {
                if (bData[pos1] == '\n') {
                    if (version >= 4 || !first) {
                        // got it
                        pos1++;
                        break;
                    } else {
                        first = false;
                        pos2 = pos1 - 1;
                    }
                }
            }
            byte[] date = Arrays.copyOfRange(bData, pos1, pos2); // remove trailing \r\n
            int[] sig = locateSignatureInDate(date);

            // Search for a matching locale based on the game compilation date.
            // If we don't find one, we default to Latin1
            return (sig != null) ? findMatchingLocale(sig) : null;
        }

        @Nullable
        private static String findMatchingLocale(@NonNull int[] signature) {
            for (int loc = 0, nLocs = AVAILABLE_ALT_LOCALES.length; loc < nLocs; loc++) {
                for (int sig = 0, nSigs = AVAILABLE_ALT_LOCALE_SIGS[loc].length; sig < nSigs; sig++) {
                    boolean matches = true;
                    for (int i = 0; i < SIGNATURE_LENGTH; i++) {
                        if (AVAILABLE_ALT_LOCALE_SIGS[loc][sig][i] != signature[i]) {
                            matches = false;
                            break;
                        }
                    }
                    if (matches) {
                        return AVAILABLE_ALT_LOCALES[loc];
                    }
                }
            }
            return null;
        }

        /*
         * Checks the format of the input date to ensure it matches the format
         * "dd [Mm]mm yyyy".  Returns the (unsigned) bytes of the month part
         * of the string, or NULL if it doesn't match the expected format.
         *
         * Ported by Tim from SCARE (thanks guys)
         */
        @Nullable
        private static int[] locateSignatureInDate(final byte[] date) {
            int[] sig = new int[SIGNATURE_LENGTH];

            if (date.length != 11) {
                // doesn't match required format
                return null;
            }

            // Pattern we want is "%2ld %3[^ 0-9] %4ld"
            for (int i = 0; i < 2; i++) {
                if (!Character.isDigit((char) date[i])) {
                    // doesn't match
                    return null;
                }
            }
            for (int i = 3; i < 6; i++) {
                if (Character.isDigit((char) date[i])) {
                    // doesn't match
                    return null;
                } else {
                    // convert signed byte to unsigned int
                    // then store in the return array
                    sig[i - 3] = date[i] & 0xFF;
                }
            }
            for (int i = 7; i < 11; i++) {
                if (!Character.isDigit((char) date[i])) {
                    // doesn't match
                    return null;
                }
            }

            // Success
            return sig;
        }

        void setMediaOffsets(@NonNull LinkedHashMap<String, MAdventure.v4Media> mediaInfo) {
            int iMediaOffset = mZipLen + 23;
            for (MAdventure.v4Media m : mediaInfo.values()) {
                m.mOffset = iMediaOffset;
                iMediaOffset += m.mLength + 1;
            }
        }

        @NonNull
        String getPassword() {
            return mPassword;
        }

        @NonNull
        public String readLine() throws EOFException {
            if (mCurPos >= mDataLen) {
                // at the end of the file
                throw new EOFException();
            }
            int start = mCurPos;
            int end = mData.indexOf('\r', start);
            if (end < 0) {
                end = mDataLen - 1;
                if (end < start) {
                    end = start;
                }
            }
            mCurPos = end + 2;
            return mData.substring(start, end);
        }

        public void skipLine() throws EOFException {
            if (mCurPos >= mDataLen) {
                throw new EOFException();
            }
            int end = mData.indexOf('\r', mCurPos);
            mCurPos = (end < 0) ? mDataLen : end + 2;
        }
    }
}
