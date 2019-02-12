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
import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.io.MFileOlder;
import com.luxlunae.bebek.view.MView;

import org.xmlpull.v1.XmlPullParser;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;

import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Indefinite;
import static com.luxlunae.bebek.MGlobals.LONGLOCATIONDESCRIPTION;
import static com.luxlunae.bebek.MGlobals.MDirectionsEnumFromInt;
import static com.luxlunae.bebek.MGlobals.SELECTED;
import static com.luxlunae.bebek.MGlobals.SHORTLOCATIONDESCRIPTION;
import static com.luxlunae.bebek.MGlobals.THEPLAYER;
import static com.luxlunae.bebek.MGlobals.UNSELECTED;
import static com.luxlunae.bebek.MGlobals.appendDoubleSpace;
import static com.luxlunae.bebek.MGlobals.dVersion;
import static com.luxlunae.bebek.MGlobals.getBool;
import static com.luxlunae.bebek.MGlobals.replaceAllIgnoreCase;
import static com.luxlunae.bebek.MGlobals.safeInt;
import static com.luxlunae.bebek.MGlobals.stripCarats;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.model.MAdventure.EnabledOptionEnum.Map;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Subjective;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MCharacter.PKEY_CHAR_HERE_DESC;
import static com.luxlunae.bebek.model.MCharacter.PKEY_CHAR_POSITION;
import static com.luxlunae.bebek.model.MGroup.GroupTypeEnum.Locations;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllGeneralListedObjects;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllListedObjects;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllSpecialListedObjects;
import static com.luxlunae.bebek.model.MObject.PKEY_LOCKABLE;
import static com.luxlunae.bebek.model.MObject.PKEY_OPENABLE;
import static com.luxlunae.bebek.model.MObject.PKEY_OPEN_STATUS;
import static com.luxlunae.bebek.model.MObject.PVAL_CLOSED;
import static com.luxlunae.bebek.model.MObject.PVAL_LOCKED;
import static com.luxlunae.bebek.model.MObject.PVAL_OPEN;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeHoldingObject;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeInSameLocationAsObject;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeWearingObject;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.Must;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.MustNot;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeInState;
import static com.luxlunae.bebek.model.MRestriction.RestrictionTypeEnum.Property;
import static com.luxlunae.bebek.model.MRestriction.RestrictionTypeEnum.Task;
import static com.luxlunae.bebek.model.MRestriction.TaskEnum.Complete;
import static com.luxlunae.bebek.model.MSingleDescription.DisplayWhenEnum.StartAfterDefaultDescription;
import static com.luxlunae.bebek.model.MSingleDescription.DisplayWhenEnum.StartDescriptionWithThis;
import static com.luxlunae.bebek.model.MSingleDescription.MDisplayWhenEnumFromInt;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.WithStateOrOpenable;
import static com.luxlunae.bebek.model.io.MFileOlder.convertV4FuncsToV5;
import static com.luxlunae.bebek.model.io.MFileOlder.getObjectKey;
import static com.luxlunae.bebek.model.io.MFileOlder.loadResource;
import static com.luxlunae.bebek.view.MView.debugPrint;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Adventure games are usually based around a series of locations that the player
 * can move between and explore.
 * <p>
 * These locations can be rooms of a house, chambers and tunnels in a cave system, or
 * outdoor locations such as streets and fields.
 * <p>
 * Different locations are linked together in such a way that the player can move
 * between them by asking to move in a particular direction. Eight compass directions
 * (north, northeast, east etc.) are provided as well as up/down and in/out to allow
 * moving in any direction. Both the Developer and Runner display a map of the locations,
 * with the connections between them shown as a conecting line.
 * <p>
 * Locations were previously named 'Rooms' in version 4.0.
 */
public class MLocation extends MItemWithProperties implements MItemFunctionEvaluator {
    private static final boolean SHOW_SHORT_LOCATIONS = true;
    private static final int V390_V380_ALT_TYPEHIDE_MULT = 10;

    private final boolean[] mCompatHideObjs = new boolean[1];
    @NonNull
    public EnumMap<MAdventure.DirectionsEnum, MDirection> mDirections =
            new EnumMap<>(MAdventure.DirectionsEnum.class);
    @NonNull
    private MDescription mShortDesc;
    @NonNull
    private MDescription mLongDesc;
    // public MMapNode MapNode;
    private boolean mHideOnMap;

    public MLocation(@NonNull MAdventure adv) {
        super(adv);
        for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
            MDirection dir = new MDirection(adv);
            mDirections.put(d, dir);
        }
        mLongDesc = new MDescription(adv);
        mShortDesc = new MDescription(adv);
    }

    public MLocation(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                     boolean bLibrary, boolean bAddDuplicateKeys, double dFileVersion) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Location");

        String s;
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        header.processTag(xpp);
                        break;

                    case "ShortDescription":
                        setShortDescription((dFileVersion < 5.000015) ?
                                new MDescription(adv, xpp.nextText()) :
                                new MDescription(adv, xpp, dFileVersion, "ShortDescription"));
                        break;

                    case "LongDescription":
                        setLongDescription(new MDescription(adv, xpp, dFileVersion, "LongDescription"));
                        break;

                    case "Hide":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setHideOnMap(getBool(s));
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Movement":
                        MLocation.MDirection dir = null;
                        String dirLK = "";
                        MRestrictionArrayList dirRests = new MRestrictionArrayList(adv);
                        int depth2 = xpp.getDepth();
                        int eventType2;

                        while ((eventType2 = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth2) {
                            if (eventType2 == START_TAG) {
                                switch (xpp.getName()) {
                                    case "Direction":
                                        MAdventure.DirectionsEnum xdir = MAdventure.DirectionsEnum.valueOf(xpp.nextText());
                                        dir = mDirections.get(xdir);
                                        break;

                                    case "Destination":
                                        dirLK = xpp.nextText();
                                        break;

                                    case "Restrictions":
                                        dirRests = new MRestrictionArrayList(adv, xpp, dFileVersion);
                                        break;
                                }
                            }
                        }
                        xpp.require(END_TAG, null, "Movement");

                        if (dir != null) {
                            dir.mLocationKey = dirLK;
                            dir.mRestrictions = dirRests;
                        }
                        break;

                    case "Property":
                        try {
                            addProperty(new MProperty(adv, xpp, dVersion));
                        } catch (Exception e) {
                            // do nothing
                        }
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Location");

        if (!header.finalise(this, adv.mLocations,
                bLibrary, bAddDuplicateKeys, null)) {
            throw new Exception();
        }
    }

    public MLocation(@NonNull MAdventure adv,
                     @NonNull MFileOlder.V4Reader reader, int iLoc, final double v,
                     int[][][] movements, ArrayList<MLocation> newLocs) throws EOFException {
        // ADRIFT V3.80, 3.90 and V4 Loader
        this(adv);

        String sKey = "Location" + String.valueOf(iLoc);
        while (adv.mLocations.containsKey(sKey)) {
            sKey = incrementKey(sKey);
        }
        setKey(sKey);
        newLocs.add(this);

        // ===========================================================
        //                 BASIC ROOM DESCRIPTIONS
        // -----------------------------------------------------------
        // The two most important aspects of a room description are
        // the Short description, and the Long description.

        // The Short description is the label that will appear at the
        // bottom of the Runner screen at all times, to show the
        // player where they are. It will also be the description
        // displayed when revisiting a room, unless verbose has been
        // turned on. You can select to have this displayed in bold
        // before the long room description by checking the relevant
        // box in the display options in Runner.
        setShortDescription(new MDescription(adv, reader.readLine()));  // Short Desc

        // The Long description is the main description that
        // describes the room in detail. Here, you would want to
        // mention everything about the room that does not change
        // each time you visit it.
        setLongDescription(new MDescription(adv,
                convertV4FuncsToV5(adv, reader.readLine())));           // Long Desc

        if (v < 4) {
            // -------------------------------------------------------
            //    ALTERNATE DESCRIPTION IF [1] AND [2] NOT SHOWING
            //              (versions 3.8 and 3.9)
            // -------------------------------------------------------
            // For v3.8 and 3.9, each room can essentially change
            // its description three times. If you would need the
            // description to change more times than this, events
            // can be used to add extra text, or at the extreme
            // level, using another room.
            //
            // This description should only be displayed if the
            // tasks associated with Alternate Description 1 AND
            // Alternate Description 2 below have NOT been
            // completed. In converting to ADRIFT 5, we can
            // simply add the three descriptions in order, setting
            // each to StartAfterDefaultDescription. Then if
            // either [1] or [2] has completed (or both), the last
            // valid description in the list will replace the
            // previous ones.
            MSingleDescription altDesc = new MSingleDescription(adv);
            String lastAltDesc = reader.readLine();                     // Alt Last Desc
            if (!lastAltDesc.equals("")) {
                altDesc.mDescription = convertV4FuncsToV5(adv, lastAltDesc);
                altDesc.mDisplayWhen = StartAfterDefaultDescription;
                altDesc.mRestrictions.mBracketSequence = "#";
                getLongDescription().add(altDesc);
            }
        }

        // ===========================================================
        //                        ROOM EXITS
        // -----------------------------------------------------------
        // To allow movement between rooms, you must define which
        // rooms you can move to from the current room.
        //
        // You can define for each direction, which room you wish to
        // move to. You will notice, initially, the off-Cardinal
        // directions are greyed out. To enable these, you must
        // select "Enable 8-point compass" from the Options screen.
        //
        // You can limit movement in these directions based upon
        // certain criteria. These are:
        //
        // * A task must be either complete or not complete.
        //
        // * An object must be in a particular state. (only for
        //   version 4).
        //
        // To do this, select the relevant options from the drop-down
        // menus. When relying on object states, the last drop down
        // menu will only populate items for objects that have
        // defined states.
        for (int i = 0; i < adv.mCompatCompassPoints; i++) {
            // For each location, we read in a
            // [NUM_DIRECTIONS x N] directions matrix, where
            // each row corresponds to one of the directions and
            // N = 4 columns for version 4 and N = 3 columns
            // for anything earlier. The columns are:
            //
            //    [1] => the connecting room or group (if the
            //           ID is higher than the total number of
            //           rooms) that this direction goes to
            //           (or 0 if there is no exit that way)
            //
            //    [2] => the ID of the task that must or must
            //           not be completed before the player
            //           can move in that direction (or 0
            //           if there is no task restriction).
            //
            //    [3] => whether the task referred to in [2]
            //           must be completed or must not be
            //           completed.
            //
            //    [4] => (only for version 4) if this is 0
            //           [2] and [3] refer to a task and
            //           complete / not complete respectively;
            //           if non-zero [2] and [3] refer to an
            //           object (e.g. a door) and whether it
            //           should be open, closed or locked.
            //
            // Later, after all of the locations and objects
            // have been loaded, this matrix will be converted
            // into actual movement restrictions for the
            // location via a call to fixMovementRestrictions().
            int[] tmp = movements[iLoc][i];
            tmp[0] = safeInt(reader.readLine());               // Rooms
            if (tmp[0] != 0) {
                // This direction has a valid exit
                tmp[1] = safeInt(reader.readLine());           // Tasks
                tmp[2] = safeInt(reader.readLine());           // Completed
                if (v >= 4) {
                    tmp[3] = safeInt(reader.readLine());       // Mode
                }
            }
        }

        // ===========================================================
        //               ALTERNATE ROOM DESCRIPTIONS
        // -----------------------------------------------------------
        // Alternate room descriptions allow you to change what is
        // displayed dependant upon certain circumstances. To add a
        // new alternate description, simply click on the Add button.
        // You can also click on the Description Details tab – if you
        // do this, you will be prompted with a dialog box asking you
        // if you want to add a new description.
        if (v < 4) {
            // -------------------------------------------------------
            //               ALTERNATE DESCRIPTION [1]
            //                (versions 3.8 and 3.9)
            // -------------------------------------------------------
            // If the player completes a certain task which would
            // change the appearance of a room, then you simply select
            // the task from the pull down menu, and enter the new
            // description into text box [1]. This description gets
            // appended to the end of the main description.
            String altDesc1 = reader.readLine();                        // Alt Desc 1
            int iTask1 = safeInt(reader.readLine());                    // Task1 #
            if (!altDesc1.equals("") || iTask1 > 0) {
                MSingleDescription sd = new MSingleDescription(adv);
                sd.mDescription = convertV4FuncsToV5(adv, altDesc1);
                sd.mDisplayWhen = StartAfterDefaultDescription;
                if (iTask1 > 0) {
                    MRestriction rest = new MRestriction(adv);
                    rest.mType = Task;
                    rest.mKey1 = "Task" + iTask1;
                    rest.mMust = Must;
                    rest.mTaskType = Complete;
                    sd.mRestrictions.add(rest);
                }
                sd.mRestrictions.mBracketSequence = "#";
                getLongDescription().add(sd);
            }

            // -------------------------------------------------------
            //               ALTERNATE DESCRIPTION [2]
            //                (versions 3.8 and 3.9)
            // -------------------------------------------------------
            // If you wanted the description to change a further time
            // if a second task has been completed, then select which
            // task this is from the second pull down menu, and enter
            // the new description into text box [2]. This description
            // supersedes all other descriptions apart from the main
            // description. Again, as above, if you wanted only the new
            // description to show, keep the main description blank.
            String altDesc2 = reader.readLine();                        // Alt Desc 2
            int iTask2 = safeInt(reader.readLine());                    // Task2 #
            if (!altDesc2.equals("") || iTask2 > 0) {
                MSingleDescription sd = new MSingleDescription(adv);
                sd.mDescription = convertV4FuncsToV5(adv, altDesc2);
                sd.mDisplayWhen = StartAfterDefaultDescription;
                if (iTask2 > 0) {
                    MRestriction rest = new MRestriction(adv);
                    rest.mType = Task;
                    rest.mKey1 = "Task" + iTask2;
                    rest.mMust = Must;
                    rest.mTaskType = Complete;
                    sd.mRestrictions.add(rest);
                }
                sd.mRestrictions.mBracketSequence = "#";
                getLongDescription().add(sd);
            }

            // -------------------------------------------------------
            //               ALTERNATE DESCRIPTION [3]
            //                (versions 3.8 and 3.9)
            // -------------------------------------------------------
            // You may want to give a different description depending
            // on whether the player is holding, wearing or in the
            // same room as particular objects. The most common usage
            // for this would be to make the room dark unless the
            // player was holding a torch. Select the method you want
            // from the first pull down menu, and from the second,
            // select the object to which this applies. When this
            // description is displayed, the main room description
            // is not shown at all. If you were creating a dark room,
            // you would want to select the check box and hide objects.
            // This means that any objects that are in the room will
            // not be displayed when the room is viewed.
            int iObj = safeInt(reader.readLine());                      // Obj #
            String altDesc3 = reader.readLine();                        // Alt Desc 3
            int typeHideObjs = safeInt(reader.readLine());              // Type Hide Objects
            if (!altDesc3.equals("")) {
                int restType =
                        typeHideObjs / V390_V380_ALT_TYPEHIDE_MULT;
                boolean hideObjs =
                        (typeHideObjs % V390_V380_ALT_TYPEHIDE_MULT) != 0;
                MSingleDescription sd = new MSingleDescription(adv);
                sd.mDescription = convertV4FuncsToV5(adv, altDesc3);
                sd.mDisplayWhen = StartDescriptionWithThis;
                sd.mCompatHideObjects = hideObjs;
                MRestriction rest = new MRestriction(adv);
                rest.mType = MRestriction.RestrictionTypeEnum.Character;
                rest.mKey1 = "Player";
                switch (restType) {
                    case 0:
                        // player is not holding
                        rest.mMust = MustNot;
                        rest.mCharacterType = BeHoldingObject;
                        break;
                    case 1:
                        // player is holding
                        rest.mMust = Must;
                        rest.mCharacterType = BeHoldingObject;
                        break;
                    case 2:
                        // player is not wearing
                        rest.mMust = MustNot;
                        rest.mCharacterType = BeWearingObject;
                        break;
                    case 3:
                        // player is wearing
                        rest.mMust = Must;
                        rest.mCharacterType = BeWearingObject;
                        break;
                    case 4:
                        // player is not in same location as object
                        rest.mMust = MustNot;
                        rest.mCharacterType = BeInSameLocationAsObject;
                        break;
                    case 5:
                        // player is in same location as object
                        rest.mMust = Must;
                        rest.mCharacterType = BeInSameLocationAsObject;
                        break;
                }
                // Needs to be converted once we've loaded objects,
                // via a call to fixDescriptionRestrictions():
                rest.mKey2 = String.valueOf(iObj);
                sd.mRestrictions.add(rest);
                sd.mRestrictions.mBracketSequence = "#";
                getLongDescription().add(sd);
            }
        }

        // -----------------------------------------------------------
        //             LONG (MAIN) DESCRIPTION RESOURCE
        // -----------------------------------------------------------
        loadResource(adv, reader, v, getLongDescription().get(0));

        // Version 4 can have any number of alternate descriptions.
        // For earlier versions it is fixed at four (already loaded
        // above - now we just need to load in any associated
        // resources).
        int nAltDesc = (v >= 4) ? cint(reader.readLine()) : 4;          // # Alt Descs

        for (int i = 0; i < nAltDesc; i++) {
            MRestriction rest = null;
            MSingleDescription sd = null;

            if (v >= 4) {
                // -------------------------------------------------
                //          ALTERNATE DESCRIPTION (1 / 2)
                //              (version 4.0)
                // -------------------------------------------------
                sd = new MSingleDescription(adv);

                // Once you have selected the circumstance for
                // the description change, you can enter the
                // description into the relevant box. You can
                // also have it display a different description
                // if the circumstance has not occurred.
                sd.mDescription =
                        convertV4FuncsToV5(adv, reader.readLine());     // Alt Desc

                // You can change the description of a room
                // depending on:
                //
                //   * Whether or not tasks have been executed
                //
                //   * What state particular objects are in
                //
                //   * Whether the Player is or is not holding,
                //     wearing, or in the same room as a
                //     particular object
                rest = new MRestriction(adv);
                switch (cint(reader.readLine())) {                      // Rest type
                    case 0:
                        // Task
                        rest.mType = Task;
                        break;
                    case 1:
                        // Object
                        rest.mType = MRestriction.RestrictionTypeEnum.Object;
                        break;
                    case 2:
                        // Player
                        rest.mType = MRestriction.RestrictionTypeEnum.Character;
                        rest.mKey1 = THEPLAYER;
                        break;
                }
            }

            // ------------------------------------------------------
            //             ALTERNATE DESCRIPTION RESOURCE
            // ------------------------------------------------------
            loadResource(adv, reader, v,
                    (v < 4) ? getLongDescription().get(0) : sd);

            if (v >= 4) {
                // -------------------------------------------------
                //          ALTERNATE DESCRIPTION (2 / 2)
                //              (version 4.0)
                // -------------------------------------------------
                rest.mMessage = new MDescription(adv,
                        convertV4FuncsToV5(adv, reader.readLine()));    // Rest message

                // Start creating the restriction
                int restKey = cint(reader.readLine());                  // Rest key
                switch (rest.mType) {
                    case Task:
                        rest.mKey1 = "Task" + restKey;
                        break;
                    case Object:
                        rest.mKey1 = "Object" + restKey;
                        rest.mMust = Must;
                        rest.mObjectType = BeInState;
                        break;
                    case Character:
                        switch (restKey) {
                            case 0:
                                // is not holding
                                rest.mCharacterType = BeHoldingObject;
                                rest.mMust = MustNot;
                                break;
                            case 1:
                                // is holding
                                rest.mCharacterType = BeHoldingObject;
                                rest.mMust = Must;
                                break;
                            case 2:
                                // is not wearing
                                rest.mCharacterType = BeWearingObject;
                                rest.mMust = MustNot;
                                break;
                            case 3:
                                // is wearing
                                rest.mCharacterType = BeHoldingObject;
                                rest.mMust = Must;
                                break;
                            case 4:
                                // is not same room as
                                rest.mCharacterType = BeInSameLocationAsObject;
                                rest.mMust = MustNot;
                                break;
                            case 5:
                                // is in same room as
                                rest.mCharacterType = BeInSameLocationAsObject;
                                rest.mMust = Must;
                                break;
                        }
                        break;
                }

                // -------------------------------------------------
                //           RESTRICTION MESSAGE RESOURCE
                // -------------------------------------------------
                loadResource(adv, reader, v, sd);

                // There is also the option to hide objects in the
                // room. Again, this is mainly useful if you are
                // setting the room up as a dark room. Simply check
                // the checkbox.
                if (cbool(reader.readLine())) {                         // Hide Objects?
                    sd.mCompatHideObjects = true;
                }

                // -------------------------------------------------
                //           REPLACEMENT SHORT DESCRIPTION
                // -------------------------------------------------
                // If the restrictions pass and this string is not
                // empty, it will replace the location's default
                // short description.
                String newShortDesc = reader.readLine();                // New Short Desc

                // Finish creating the restriction
                int rest3 = cint(reader.readLine());                    // #Var3
                switch (rest.mType) {
                    case Task:
                        rest.mMust = (rest3 == 0) ? Must : MustNot;
                        rest.mTaskType = Complete;
                        break;
                    case Object:
                        rest.mKey2 = String.valueOf(rest3);
                        break;
                    case Character:
                        rest.mKey2 = String.valueOf(rest3);
                        break;
                }
                if (!(rest.mType == Task && rest.mKey1.equals("Task0"))) {
                    sd.mRestrictions.add(rest);
                }
                sd.mRestrictions.mBracketSequence = "#";

                // There are three occasions when you can display
                // the alternate description. These are:
                //
                // * Start room description with this one – This
                //   overrides the main room description completely,
                //   displaying only the alternate description. Any
                //   other alternate descriptions higher up on the
                //   list are completely ignored.
                //
                // * Start directly after Long Room description –
                //   This appends onto the end of the Long Room
                //   description. Any other alternate descriptions
                //   higher up on the list are completely ignored.
                //
                // * Append to other descriptions – This appends
                //   the room description to any other descriptions
                //   higher on the list which are being displayed.
                int iDisplayWhen = cint(reader.readLine());             // Display When
                sd.mDisplayWhen = MDisplayWhenEnumFromInt(iDisplayWhen);
                getLongDescription().add(sd);

                // If it's set, add the alternate short description.
                if (!newShortDesc.equals("")) {
                    MSingleDescription sdShort = sd.clone();
                    sdShort.mDescription = newShortDesc;
                    sdShort.mDisplayWhen = StartDescriptionWithThis;
                    getShortDescription().add(sdShort);
                }
            }
        }

        if (v >= 3.9 && adv.getEnabled(Map)) {
            // You may want to prevent the room being displayed on
            // the map. Typically, this might be because it is part
            // of a maze or suchlike. To do this, check the "Don’t
            // show on map" checkbox.
            setHideOnMap(cbool(reader.readLine()));                     // Hide on map?
        }
    }

    void setProperty(@NonNull String propName, @NonNull String propValue) {
        MProperty prop = null;
        if (hasProperty(propName)) {
            prop = getProperty(propName);
        }

        if (prop == null) {
            // Property doesn't already exist for this location.
            if (propValue.equals(SELECTED)) {
                // We want to add it
                if (mAdv.mLocationProperties.containsKey(propName)) {
                    prop = mAdv.mLocationProperties.get(propName).clone();
                    switch (prop.getType()) {
                        case SelectionOnly:
                            prop.setSelected(true);
                            addProperty(prop);
                            break;
                    }
                } else {
                    debugPrint(MGlobals.ItemEnum.Task, getKey(), MView.DebugDetailLevelEnum.Error,
                            "Can't select property " + propName + " for location " +
                                    getCommonName() +
                                    " as that property doesn't exist in the global location properties.");
                }
            } else {
                debugPrint(MGlobals.ItemEnum.Task, getKey(), MView.DebugDetailLevelEnum.Error,
                        "Can't set property '" + propName + "' of location '" +
                                getCommonName() + "' to '" + propValue +
                                "' as the location doesn't have that property.");
            }
        } else {
            // Property already exists for this location
            if (propValue.equals(UNSELECTED)) {
                // We want to remove the existing property
                removeProperty(propName);
            } else {
                // We want to set the existing property to a new value
                switch (prop.getType()) {
                    case Integer:
                    case Text:
                    case StateList:
                    case ValueList:
                        // Could be dropdown or expression
                        String result = mAdv.evaluateStringExpression(propValue, MParser.mReferences);
                        if (result.equals("") && !propValue.equals("")) {
                            result = propValue;
                        }
                        prop.setValue(result);
                        break;
                    case ObjectKey:
                    case CharacterKey:
                    case LocationKey:
                        String itemKey = propValue;
                        if (itemKey.startsWith("Referenced")) {
                            itemKey = MParser.mReferences.getReference(itemKey);
                        }
                        prop.setValue(itemKey != null ? itemKey : "");
                        break;
                    default:
                        prop.setValue(propValue);
                        break;
                }
            }

            if (prop.getKey().equals(PKEY_CHAR_POSITION)) {
                // Need to reset the position so the next
                // call to char.getPosition() will reread the
                // property and return the correct PositionEnum
                // result
                MCharacter ch = mAdv.mCharacters.get(getKey());
                if (ch != null) {
                    ch.getLocation().resetPosition();
                }
            }
        }
    }

    @Override
    @NonNull
    public String evaluate(@NonNull String funcName,
                           @NonNull String args,
                           @NonNull String remainder,
                           @NonNull boolean[] resultIsInteger) {
        ArrayList<MItemWithProperties> lst;

        switch (funcName) {
            case "Characters":
                // -------------------------------
                //     %location%.Characters
                // -------------------------------
                // Lists the characters at this location
                lst = new ArrayList<MItemWithProperties>(getCharactersVisibleAtLocation().values());
                return mAdv.evaluateItemFunction(remainder, lst,
                        null, null, null, resultIsInteger);

            case "Contents":
                // -------------------------------------------
                // %location%.Contents(all|characters|objects)
                // -------------------------------------------
                // Lists all characters and objects in this location.
                // or just the characters, or just the objects,
                // depending upon the argument. No argument
                // defaults to list all.
                lst = new ArrayList<>();
                switch (args.toLowerCase()) {
                    case "":
                    case "all":
                        lst.addAll(getObjectsInLocation().values());
                        lst.addAll(getCharactersDirectlyInLocation().values());
                        break;
                    case "characters":
                        lst.addAll(getCharactersDirectlyInLocation().values());
                        break;
                    case "objects":
                        lst.addAll(getObjectsInLocation().values());
                        break;
                }
                return mAdv.evaluateItemFunction(remainder, lst,
                        null, null, null, resultIsInteger);

            case "Count":
                resultIsInteger[0] = true;
                return "1";

            case "Description":
            case LONGLOCATIONDESCRIPTION:
                // -----------------------------------
                //  %location%.LongLocationDescription
                // ------------------------------------
                // The full description of the location.
                String sResult = getViewLocation();
                if (sResult.length() == 0) {
                    sResult = "There is nothing of interest here.";
                }
                return sResult;

            case "Exits":
                // -------------------------------
                //        %location%.Exits
                // -------------------------------
                // The directions in which exits from
                // this location exist.
                //
                // NOTE: This function returns ALL exits,
                // including those which are currently being
                // blocked by restrictions. Use the
                // %ListExits[%character%]% function to list
                // available exits that a particular character
                // may use from their current location.
                ArrayList<MAdventure.DirectionsEnum> lstDirs = new ArrayList<>();
                for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
                    if (!mDirections.get(d).mLocationKey.equals("")) {
                        lstDirs.add(d);
                    }
                }
                return mAdv.evaluateItemFunction(remainder, null,
                        lstDirs, null, null, resultIsInteger);

            case "LocationTo":
                // -----------------------------------
                // %location%.LocationTo(%direction%)
                // -----------------------------------
                // The location in the referenced direction
                lst = new ArrayList<>();
                for (String sDir : args.toLowerCase().split("\\|")) {
                    for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
                        if (sDir.equals(d.toString().toLowerCase())) {
                            String sLocTo = mDirections.get(d).mLocationKey;
                            MLocation locTo = mAdv.mLocations.get(sLocTo);
                            if (locTo != null) {
                                lst.add(locTo);
                            }
                            break;
                        }
                    }
                }
                return (lst.size() == 1) ?
                        mAdv.evaluateItemFunction(remainder, null,
                                null, null, (MLocation) lst.get(0), resultIsInteger) :
                        mAdv.evaluateItemFunction(remainder, lst,
                                null, null, null, resultIsInteger);

            case "Name":
            case SHORTLOCATIONDESCRIPTION:
                // -----------------------------------
                // %location%.ShortLocationDescription
                // -----------------------------------
                // The name of the location.
                return getShortDescription().toString();

            case "Objects":
                // -----------------------------------
                //      %location%.Objects
                // -----------------------------------
                // Lists the visible objects at this location.
                //
                // NOTE: The Objects list only includes objects
                // that are set to be listed in location descriptions.
                //
                // Static objects will only be listed if they have
                // "Specifically list object in location descriptions" ticked.
                //
                // Dynamic objects will only be listed if "Specifically
                // exclude object from location description" is NOT ticked.
                lst = new ArrayList<MItemWithProperties>(getObjectsInLocation().values());
                return mAdv.evaluateItemFunction(remainder, lst,
                        null, null, null, resultIsInteger);

            case "":
                // -------------------------------
                //         %location%
                // -------------------------------
                // Return the location's key.
                return getKey();

            default:
                // Any other valid property not already covered above.
                return mAdv.evaluateItemProperty(funcName,
                        getProperties(), mAdv.mLocationProperties, remainder, resultIsInteger);
        }
    }

    @Override
    @NonNull
    public String toString() {
        // for displaying in list boxes
        return getShortDescription().toString();
    }

    @Override
    protected MGroup.GroupTypeEnum getPropertyGroupType() {
        return Locations;
    }

    @Override
    @NonNull
    public String getParent() {
        // Suppose we could return a Location Group, but what if we were member of more than one?
        return "";
    }

    public boolean getHideOnMap() {
        return mHideOnMap;
    }

    private void setHideOnMap(boolean value) {
        mHideOnMap = value;
    }

    public boolean getSeenBy(@NonNull String chKey) {
        return mAdv.mCharacters.get(chKey).hasSeenLocation(getKey());
    }

    public void setSeenBy(@NonNull String chKey, boolean value) {
        mAdv.mCharacters.get(chKey).setHasSeenLocation(getKey(), value);
    }

    @NonNull
    public String getShortDescriptionSafe() {
        if (mAdv.mVersion < 5) {
            // v4 didn't replace ALRs on statusbar and map
            return stripCarats(mAdv.evaluateFunctions(getShortDescription().toString(), MParser.mReferences));
        } else {
            StringBuilder tmp = new StringBuilder(getShortDescription().toString());
            mAdv.mALRs.evaluate(tmp, MParser.mReferences);
            return stripCarats(tmp.toString());
        }
    }

    /**
     * Short descriptions are essentially a label for the location. These will
     * be displayed on the map, and at the bottom of the screen when playing
     * the game, to show which location you are currently at.
     * <p>
     * Under most circumstances, the short description of a room will
     * stay the same throughout the game. However, there may be times
     * when you need this to change during play. To do this, click
     * on the down arrow to the right of the short description. This will
     * drop down a source text box, where you can add alternate
     * descriptions depending on different circumstances.
     * <p>
     * * @return
     */
    @NonNull
    public MDescription getShortDescription() {
        if (hasProperty(SHORTLOCATIONDESCRIPTION)) {
            MDescription ret = mShortDesc.copy();
            ret.addAll(getProperty(SHORTLOCATIONDESCRIPTION).getStringData());
            return ret;
        } else {
            return mShortDesc;
        }
    }

    private void setShortDescription(@NonNull MDescription value) {
        mShortDesc = value;
    }

    /**
     * The long description is the text that will be displayed when the player
     * moves into a room, and when they 'look' in the room. Again,
     * you can add alternate descriptions so the text will change
     * depending on what happens during play.
     *
     * @return the description.
     */
    @NonNull
    public MDescription getLongDescription() {
        if (hasProperty(LONGLOCATIONDESCRIPTION)) {
            MDescription ret = mLongDesc.copy();
            ret.addAll(getProperty(LONGLOCATIONDESCRIPTION).getStringData());
            return ret;
        } else {
            return mLongDesc;
        }
    }

    private void setLongDescription(@NonNull MDescription value) {
        mLongDesc = value;
    }

    @Override
    @NonNull
    protected String getRegEx(boolean getADRIFTExpr,
                              boolean usePluralForm) {
        StringBuilder ret = new StringBuilder();
        String[] shortDescWords =
                getShortDescription().toString(true)
                        .toLowerCase().split(" ");
        if (getADRIFTExpr) {
            // Create an ADRIFT 'advanced command construction' expression.
            for (String word : shortDescWords) {
                if (word.length() > 0) {
                    ret.append("{").append(word).append("} ");
                }
            }
        } else {
            // Create a real regular expression.
            for (String word : shortDescWords) {
                if (ret.length() > 0) {
                    ret.append("( )?");
                }
                if (word.length() > 0) {
                    ret.append("(").append(word).append(")?");
                }
            }
        }
        return ret.toString();
    }

    @NonNull
    public String getViewLocation() {
        StringBuilder ret = new StringBuilder();

        if (SHOW_SHORT_LOCATIONS) {
            if (mAdv.mVersion >= 5) {
                ret.append("<br>");
            }
            ret.append("<b>")
                    .append(getShortDescription().toString())
                    .append("</b><br>");
        }

        int lenHeader = ret.length();

        ret.append(getLongDescription().toString(mCompatHideObjs));

        // Apply work-around for compatibility with older games.
        if (!mCompatHideObjs[0]) {
            // Do any specific listed objects
            MObjectHashMap obs = getObjectsInLocation(AllSpecialListedObjects, true);
            for (MObject ob : obs.values()) {
                appendDoubleSpace(ret);
                ret.append(ob.getListDescription());
            }

            obs = getObjectsInLocation(AllGeneralListedObjects, true);
            if (obs.size() > 0) {
                if (ret.length() != 0 || mAdv.mVersion < 5) {
                    appendDoubleSpace(ret);
                    if (mAdv.mVersion < 5 && ret.length() == 0) {
                        ret.append("  ");
                    }
                    ret.append("Also here is ").append(obs.toList("and",
                            false, Indefinite)).append(".");
                } else {
                    ret.append("There is ").append(obs.toList("and",
                            false, Indefinite)).append(" here.");
                }
            }
        }

        for (MEvent e : mAdv.mEvents.values()) {
            String lookText = e.getLookText();
            if (lookText.length() > 0) {
                appendDoubleSpace(ret);
                ret.append(lookText);
            }
        }

        // Description without name, list of names
        HashMap<String, MStringArrayList> chDesc = new HashMap<>();
        for (String chKey : getCharactersVisibleAtLocation().keySet()) {
            MCharacter ch = mAdv.mCharacters.get(chKey);
            String name = ch.getName(Subjective, false);

            // Default to Char is here unless we have property
            // (which can set value to blank
            String isHereDesc = (ch.hasProperty(PKEY_CHAR_HERE_DESC)) ?
                    mAdv.evaluateFunctions(ch.getIsHereDesc(), MParser.mReferences) :
                    name + " is here.";

            if (isHereDesc.length() > 0) {
                // ----------------------------------------------
                // TCC: add these next few lines to ensure proper
                // ALR replacement in Escape from New York (when
                // player first comes across barber, steward and
                // purser). This preserves the case sensitivity
                // of the name in the game's original description
                // which is important if games then attempt to
                // use ALRs to replace that name. For other games
                // it shouldn't matter as we'll auto-capitalise
                // later in the Substitutor.
                //Matcher m = Pattern.compile("(" + Pattern.quote(name) + ")",
                //        Pattern.CASE_INSENSITIVE).matcher(isHereDesc);
                //if (m.find()) {
                //    name = m.group(1);
                // }
                // ---------------------------------------------
                StringBuilder sb = new StringBuilder(isHereDesc);
                replaceAllIgnoreCase(sb, name, "##CHARNAME##");
                String sDescWithoutName = sb.toString();
                if (!chDesc.containsKey(sDescWithoutName)) {
                    chDesc.put(sDescWithoutName, new MStringArrayList());
                }
                if (!chDesc.get(sDescWithoutName).contains(name)) {
                    chDesc.get(sDescWithoutName).add(name);
                }
            }
        }

        for (String desc : chDesc.keySet()) {
            MStringArrayList d = chDesc.get(desc);
            appendDoubleSpace(ret);
            if (d.size() > 1) {
                desc = desc.replace(" is ", " are ");
            }
            ret.append(desc.replace("##CHARNAME##", d.list()));
        }

        if (mAdv.getShowExits()) {
            int[] nExits = new int[1];
            nExits[0] = 0;
            String exits = mAdv.getPlayer().listExits(getKey(), nExits);
            appendDoubleSpace(ret);
            if (nExits[0] > 1) {
                ret.append("Exits are ").append(exits).append(".");
            } else if (nExits[0] == 1) {
                ret.append("An exit leads ").append(exits).append(".");
            }
        }

        if (ret.length() == lenHeader) {
            ret.append("Nothing special.");
        }

        return ret.toString();
    }

    @NonNull
    public MObjectHashMap getObjectsInLocation() {
        return getObjectsInLocation(AllListedObjects, true);
    }

    @NonNull
    public MObjectHashMap getObjectsInLocation(WhichObjectsToListEnum listWhat,
                                               boolean bDirectly) {
        // Directly means they have to be directly in the room,
        // i.e. not held by a character etc
        MObjectHashMap ret = new MObjectHashMap(mAdv);

        for (MObject ob : mAdv.mObjects.values()) {
            if (ob.existsAtLocation(getKey(), bDirectly)) {
                switch (listWhat) {
                    case AllGeneralListedObjects:
                        // Dynamic objects not excluded plus static
                        // objects explicitly included, unless specially listed
                        if ((!ob.isStatic() && !ob.isExplicitlyExclude()) ||
                                (ob.isStatic() && ob.isExplicitlyList())) {
                            if (ob.getListDescription().equals("")) {
                                ret.put(ob.getKey(), ob);
                            }
                        }
                        break;
                    case AllListedObjects:
                        // All listed objects, including special listed
                        if ((!ob.isStatic() && !ob.isExplicitlyExclude()) ||
                                (ob.isStatic() && ob.isExplicitlyList())) {
                            ret.put(ob.getKey(), ob);
                        }
                        break;
                    case AllObjects:
                        // Any object in the location, whether indirectly
                        // or not
                        ret.put(ob.getKey(), ob);
                        break;
                    case AllSpecialListedObjects:
                        // Specially listed objects only (i.e. they have
                        // a special listing description)
                        if ((!ob.isStatic() && !ob.isExplicitlyExclude()) ||
                                (ob.isStatic() && ob.isExplicitlyList())) {
                            if (!ob.getListDescription().equals("")) {
                                ret.put(ob.getKey(), ob);
                            }
                        }
                        break;
                }
            }
        }

        return ret;
    }

    @NonNull
    private MCharacterHashMap getCharactersDirectlyInLocation() {
        return getCharactersDirectlyInLocation(false);
    }

    @NonNull
    public MCharacterHashMap getCharactersDirectlyInLocation(boolean bIncludePlayer) {
        // Characters directly in the location
        MCharacterHashMap ret = new MCharacterHashMap(mAdv);
        for (MCharacter ch : mAdv.mCharacters.values()) {
            if (ch.getLocation().getKey().equals(getKey()) &&
                    ch.getLocation().getExistsWhere() == AtLocation &&
                    (bIncludePlayer || !ch.getKey().equals(mAdv.getPlayer().getKey()))) {
                ret.put(ch.getKey(), ch);
            }
        }
        return ret;
    }

    @NonNull
    MCharacterHashMap getCharactersVisibleAtLocation() {
        // Characters visible in location (can be in open objects, on objects etc)
        MCharacterHashMap ret = new MCharacterHashMap(mAdv);
        for (MCharacter ch : mAdv.mCharacters.values()) {
            if (ch != mAdv.getPlayer() && ch.isVisibleAtLocation(getKey())) {
                ret.put(ch.getKey(), ch);
            }
        }
        return ret;
    }

    public void fixMovementRestrictions(final int[][][] movements,
                                        int iLoc, int nLoc, int iStartTask,
                                        final HashMap<String, String> dictDodgyStates,
                                        double v) {
        for (int i = 0; i < 12; i++) {
            int movement[] = movements[iLoc][i];
            if (movement[0] > 0) {
                // This location has a valid exit and movement[0] is
                // the ID of the connecting room (if it is <= nLoc)
                // or group (if it is > nLoc).
                MLocation.MDirection dir = mDirections.get(MDirectionsEnumFromInt(i));
                dir.mLocationKey = (movement[0] <= nLoc) ?
                        "Location" + movement[0] : "Group" + (movement[0] - nLoc);
                if (movement[1] > 0) {
                    // This location has an associated restriction
                    //
                    // if the mode (movement[3]) is 0 or we're earlier than version 4 then
                    //
                    //    movement[1] is the ID of a task (offset by iStartTask) that
                    //    must (movement[2] == 0) or must not (movement[2] == 1) be complete.
                    //
                    // otherwise
                    //
                    //    movement[1] is the ID of an object that must be either open
                    //    (movement[2] == 0), or closed (iLocalVal[2] == 1), or locked
                    //    (movement[2] == 2).
                    MRestriction rest = new MRestriction(mAdv);
                    if (v < 4 || movement[3] == 0) {
                        // If movement[3] == 0, the mode is "Task is / is not complete"
                        rest.mType = Task;
                        rest.mKey1 = "Task" + (movement[1] + iStartTask);
                        rest.mMust = (movement[2] == 0) ? Must : MustNot;
                        rest.mTaskType = Complete;
                    } else {
                        // Filter on objects with open / closed / locked state
                        rest.mType = Property;
                        rest.mKey1 = PKEY_OPEN_STATUS;
                        rest.mKey2 = getObjectKey(mAdv, movement[1] - 1,
                                WithStateOrOpenable);
                        rest.mMust = Must;
                        MObject ob = mAdv.mObjects.get(rest.mKey2);

                        if (!ob.isOpenable()) {
                            // Make it openable
                            MProperty p = mAdv.mAllProperties.get(PKEY_OPENABLE).copy();
                            ob.addProperty(p);
                            p = mAdv.mAllProperties.get(PKEY_OPEN_STATUS).copy();
                            ob.addProperty(p);
                        }

                        if (movement[2] == 0 || movement[2] == 1) {
                            MProperty prop = ob.getProperty(PKEY_OPEN_STATUS);
                            String sIntended = dictDodgyStates.get(rest.mKey2);
                            if (sIntended != null) {
                                sIntended = sIntended.toLowerCase().split("\\|")[movement[2]];
                                for (String state : prop.mStates) {
                                    if (sIntended.equals(state.toLowerCase())) {
                                        rest.mStringValue = state;
                                        break;
                                    }
                                }
                            } else {
                                rest.mStringValue = prop.mStates.get(movement[2]);
                            }
                        } else if (movement[2] == 2) {
                            if (!ob.isLockable()) {
                                // Make it lockable
                                MProperty p = mAdv.mAllProperties.get(PKEY_LOCKABLE).copy();
                                ob.addProperty(p);
                            }
                            rest.mStringValue = PVAL_LOCKED;
                        }
                    }
                    dir.mRestrictions.add(rest);
                    dir.mRestrictions.mBracketSequence = "#";
                }
            }
        }
    }

    public void fixDesciptionRestrictions(@NonNull HashMap<MObject, MProperty> dodgyArlStates) {
        // Replace any numeric restriction keys with the actual
        // character or object key.
        ArrayList<MSingleDescription> listDescriptions = new ArrayList<>();
        listDescriptions.addAll(getShortDescription());
        listDescriptions.addAll(getLongDescription());

        for (MSingleDescription sd : listDescriptions) {
            MRestriction rest = (sd.mRestrictions.size() > 0) ?
                    sd.mRestrictions.get(0) : null;
            if (rest != null && isNumeric(rest.mKey2)) {
                if (rest.mType == MRestriction.RestrictionTypeEnum.Character) {
                    switch (rest.mCharacterType) {
                        case BeInSameLocationAsObject:
                        case BeHoldingObject:
                            rest.mKey2 = getObjectKey(mAdv,
                                    cint(rest.mKey2) - 1,
                                    MFileOlder.ComboEnum.Dynamic);
                            break;
                        case BeWearingObject:
                            rest.mKey2 = getObjectKey(mAdv,
                                    cint(rest.mKey2) - 1,
                                    MFileOlder.ComboEnum.Wearable);
                            break;
                    }
                } else if (rest.mType == MRestriction.RestrictionTypeEnum.Object) {
                    MObject ob = null;
                    String sKey1 = rest.mKey1;
                    String sKey2 = rest.mKey2;
                    boolean bRefObj = sKey1.equals("ReferencedObject");
                    if (!bRefObj) {
                        ob = mAdv.mObjects.get(sKey1);
                    }
                    switch (cint(rest.mKey2)) {
                        case 1:
                            if (bRefObj || ob.isOpenable()) {
                                sKey2 = PVAL_OPEN;
                            } else {
                                MProperty prop = dodgyArlStates.get(ob);
                                if (prop != null) {
                                    sKey2 = prop.mStates.get(cint(rest.mKey2) - 1);
                                }
                            }
                            break;
                        case 2:
                            if (bRefObj || ob.isOpenable()) {
                                sKey2 = PVAL_CLOSED;
                            } else {
                                MProperty prop = dodgyArlStates.get(ob);
                                if (prop != null) {
                                    sKey2 = prop.mStates.get(cint(rest.mKey2) - 1);
                                }
                            }
                            break;
                        case 3:
                            if (bRefObj || ob.isOpenable()) {
                                if (bRefObj || ob.isLockable()) {
                                    sKey2 = PVAL_LOCKED;
                                } else {
                                    MProperty prop = dodgyArlStates.get(ob);
                                    if (prop != null) {
                                        sKey2 = prop.mStates.get(cint(rest.mKey2) - 3);
                                    }
                                }
                            } else {
                                MProperty prop = dodgyArlStates.get(ob);
                                if (prop != null) {
                                    sKey2 = prop.mStates.get(cint(rest.mKey2) - 1);
                                }
                            }
                            break;
                        default:
                            int iOffset = 0;
                            if (bRefObj || ob.isOpenable()) {
                                iOffset = (bRefObj || ob.isLockable()) ? 4 : 3;
                            }
                            MProperty prop = dodgyArlStates.get(ob);
                            if (prop != null) {
                                sKey2 = prop.mStates.get(cint(rest.mKey2) - iOffset);
                            }
                            break;
                    }
                    rest.mKey2 = sKey2;
                }
            }
        }
    }

    @Nullable
    @Override
    public MLocation clone() {
        return (MLocation) super.clone();
    }

    boolean isAdjacent(@NonNull String sKey) {
        for (MDirection dir : mDirections.values()) {
            if (dir.mLocationKey.equals(sKey)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    String getDirectionTo(@NonNull String sKey) {
        if (sKey.equals(getKey())) {
            return "not moved";
        }
        for (MAdventure.DirectionsEnum drn : MAdventure.DirectionsEnum.values()) {
            if (mDirections.get(drn).mLocationKey.equals(sKey)) {
                switch (drn) {
                    case North:
                    case East:
                    case South:
                    case West:
                        return "the " + drn.toString().toLowerCase();
                    case Up:
                        return "above";
                    case Down:
                        return "below";
                    case In:
                        return "inside";
                    case Out:
                        return "outside";
                    case NorthEast:
                        return "the north-east";
                    case NorthWest:
                        return "the north-west";
                    case SouthEast:
                        return "the south-east";
                    case SouthWest:
                        return "the south-west";
                }
            }
        }
        return "nowhere";
    }

    @Override
    @NonNull
    public String getCommonName() {
        return getShortDescription().toString();
    }

    @Override
    @NonNull
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> all = new ArrayList<>();
        all.add(getShortDescription());
        all.add(getLongDescription());
        for (MProperty p : getLocalProperties().values()) {
            all.add(p.getStringData());
        }
        return all;
    }

    @Override
    public int findLocal(@NonNull String toFind,
                         @Nullable String toReplace,
                         boolean findAll, @NonNull int[] nReplaced) {
        int iCount = nReplaced[0];
        return nReplaced[0] - iCount;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int iCount = 0;
        for (MDescription d : getAllDescriptions()) {
            iCount += d.referencesKey(key);
        }
        for (MDirection d : mDirections.values()) {
            if (d.mLocationKey.equals(key)) {
                iCount++;
            }
            iCount += d.mRestrictions.referencesKey(key);
        }
        iCount += getLocalProperties().getNumberOfKeyRefs(key);
        return iCount;
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (MDescription d : getAllDescriptions()) {
            if (!d.deleteKey(key)) {
                return false;
            }
        }
        for (MDirection d : mDirections.values()) {
            if (d.mLocationKey.equals(key)) {
                d.mRestrictions.clear();
                d.mLocationKey = "";
            } else {
                if (!d.mRestrictions.deleteKey(key)) {
                    return false;
                }
            }
        }
        return getLocalProperties().deleteKey(key);
    }

    public enum WhichObjectsToListEnum {
        AllObjects,                 // 0
        AllListedObjects,           // 1
        AllGeneralListedObjects,    // 2
        AllSpecialListedObjects     // 3
    }

    public static class MDirection {
        @NonNull
        public String mLocationKey = "";
        @NonNull
        public MRestrictionArrayList mRestrictions;
        boolean mEverBeenBlocked = false;

        MDirection(@NonNull MAdventure adv) {
            mRestrictions = new MRestrictionArrayList(adv);
        }
    }
}
