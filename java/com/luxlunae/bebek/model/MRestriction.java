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
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.io.MFileOlder;
import com.luxlunae.bebek.view.MView;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;

import static com.luxlunae.bebek.MGlobals.ANYCHARACTER;
import static com.luxlunae.bebek.MGlobals.ANYDIRECTION;
import static com.luxlunae.bebek.MGlobals.ANYOBJECT;
import static com.luxlunae.bebek.MGlobals.HIDDEN;
import static com.luxlunae.bebek.MGlobals.NOOBJECT;
import static com.luxlunae.bebek.MGlobals.PLAYERLOCATION;
import static com.luxlunae.bebek.MGlobals.REFERENCE_NAMES;
import static com.luxlunae.bebek.MGlobals.THEFLOOR;
import static com.luxlunae.bebek.MGlobals.safeBool;
import static com.luxlunae.bebek.MGlobals.safeInt;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.model.MCharacter.Gender.Female;
import static com.luxlunae.bebek.model.MCharacter.Gender.Male;
import static com.luxlunae.bebek.model.MCharacter.Gender.Unknown;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.OnCharacter;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.Position.Lying;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.Position.Sitting;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.Position.Standing;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllObjects;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.Hidden;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.InObject;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.WornByCharacter;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.NoRooms;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.PartOfCharacter;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.OnObject;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.StateList;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeAlone;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeAtLocation;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeInSameLocationAsCharacter;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeLyingOnObject;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeOfGender;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeSittingOnObject;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeStandingOnObject;
import static com.luxlunae.bebek.model.MRestriction.CharacterEnum.BeWithinLocationGroup;
import static com.luxlunae.bebek.model.MRestriction.LocationEnum.HaveBeenSeenByCharacter;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.Must;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.MustNot;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeExactText;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeHeldByCharacter;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeHidden;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeInState;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeInsideObject;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeOnObject;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeVisibleToCharacter;
import static com.luxlunae.bebek.model.MRestriction.ObjectEnum.BeWornByCharacter;
import static com.luxlunae.bebek.model.MRestriction.RestrictionTypeEnum.Location;
import static com.luxlunae.bebek.model.MRestriction.TaskEnum.Complete;
import static com.luxlunae.bebek.model.MRestriction.VariableEnum.EqualTo;
import static com.luxlunae.bebek.model.MRestriction.VariableEnum.GreaterThan;
import static com.luxlunae.bebek.model.MRestriction.VariableEnum.GreaterThanOrEqualTo;
import static com.luxlunae.bebek.model.MRestriction.VariableEnum.LessThan;
import static com.luxlunae.bebek.model.MRestriction.VariableEnum.LessThanOrEqualTo;
import static com.luxlunae.bebek.model.MVariable.VariableType.NUMERIC;
import static com.luxlunae.bebek.model.MVariable.VariableType.TEXT;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Container;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Dynamic;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Lieable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Sittable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Standable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Surface;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.WithStateOrOpenable;
import static com.luxlunae.bebek.model.io.MFileOlder.convertV4FuncsToV5;
import static com.luxlunae.bebek.model.io.MFileOlder.getObjectKey;

public class MRestriction {

    private static final String[] BACK_REFERENCES = new String[]{
            "ReferencedObject", "ReferencedObject1", "ReferencedObject2",
            "ReferencedObject3", "ReferencedObject4", "ReferencedObject5",
            "ReferencedObjects", "ReferencedDirection", "ReferencedCharacter",
            "ReferencedCharacter1", "ReferencedCharacter2", "ReferencedCharacter3",
            "ReferencedCharacter4", "ReferencedCharacter5", "ReferencedLocation",
            "ReferencedItem"
    };
    @NonNull
    private final MAdventure mAdv;
    public RestrictionTypeEnum mType = Location;
    public MustEnum mMust = Must;
    @Nullable
    public String mKey1 = "";
    @Nullable
    public String mKey2 = "";
    @NonNull
    public MDescription mMessage;
    public LocationEnum mLocationType = HaveBeenSeenByCharacter;
    public ObjectEnum mObjectType = ObjectEnum.BeAtLocation;
    public TaskEnum mTaskType = Complete;
    public CharacterEnum mCharacterType = BeAlone;
    public ItemEnum mItemType = ItemEnum.BeAtLocation;
    public VariableEnum mVariableType = LessThan;
    public int mIntValue;
    @NonNull
    public String mStringValue = "";

    public MRestriction(@NonNull MAdventure adv) {
        mAdv = adv;
        mMessage = new MDescription(adv);
    }

    public MRestriction(@NonNull MAdventure adv, double v,
                        int iStartTask, int iStartChar, final HashMap<String, String> dictDodgyStates,
                        final HashMap<MObject, MProperty> dodgyArlStates,
                        int type, int var1, int var2, int var3, String sVar4,
                        String sFailMessage) {
        // ADRIFT V3.80 loader
        this(adv);

        initOlder(adv, v, iStartTask, iStartChar,
                dictDodgyStates, dodgyArlStates, type, var1,
                var2, var3, sVar4, sFailMessage);
    }

    public MRestriction(@NonNull MAdventure adv, @NonNull MFileOlder.V4Reader reader, double v,
                        int iStartTask, int iStartChar, final HashMap<String, String> dictDodgyStates,
                        final HashMap<MObject, MProperty> dodgyArlStates) throws EOFException {
        // ADRIFT V3.90 and V4 Loader
        this(adv);

        int type = cint(reader.readLine());                      // #Type
        int var1 = cint(reader.readLine());                      // #Var1
        int var2 = cint(reader.readLine());                      // #Var2
        int var3 = 0;
        if (type == 0 || type > 2) {
            var3 = cint(reader.readLine());                      // ?#Type=0:#Var1,#Var2,#Var3
        }
        if (v < 4 && type == 4 && var1 > 0) {
            var1++;
        }
        String sVar4 = null;
        if (v >= 4) {
            if (type == 4) {
                sVar4 = reader.readLine();                          // ?#Type=4:#Var1,#Var2,#Var3,$Var4
            }
        }
        String sFailMessage = reader.readLine();                    // $FailMessage

        initOlder(adv, v, iStartTask, iStartChar, dictDodgyStates,
                dodgyArlStates, type, var1, var2, var3, sVar4, sFailMessage);
    }

    private static boolean isReference(@Nullable String key) {
        if (key == null) {
            return false;
        }
        for (String backRef : BACK_REFERENCES) {
            if (backRef.equals(key)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    static MRestriction createLocRestriction(@NonNull MAdventure adv,
                                             @NonNull String locKey, boolean bMust) {
        MRestriction r = new MRestriction(adv);
        r.mType = RestrictionTypeEnum.Character;
        r.mKey1 = "%Player%";
        r.mMust = bMust ? Must : MustNot;
        if (adv.mLocations.containsKey(locKey)) {
            r.mCharacterType = BeAtLocation;
        } else if (adv.mGroups.containsKey(locKey)) {
            r.mCharacterType = BeWithinLocationGroup;
        }
        r.mKey2 = locKey;
        return r;
    }

    private void initOlder(@NonNull MAdventure adv, double v,
                           int iStartTask, int iStartChar, final HashMap<String, String> dictDodgyStates,
                           final HashMap<MObject, MProperty> dodgyArlStates,
                           int type, int var1, int var2, int var3, String sVar4,
                           String sFailMessage) {
        // Adrift 4 restrictions are grouped into five sections.
        mMessage = new MDescription(adv, convertV4FuncsToV5(adv, sFailMessage));
        switch (type) {
            case 0:
                // ----------------------------------------------------------
                //                     Object location
                // ----------------------------------------------------------
                // You can specify that NO object, ANY object, the Referenced
                // object, or a specific object must or must not be in a
                // specific room, held by the Player or a specific character,
                // worn by the Player or specific character, visible to the
                // Player or specific character, inside a container object,
                // or on a surface object.
                // ----------------------------------------------------------
                mType = MRestriction.RestrictionTypeEnum.Object;
                switch (var1) {
                    case 0:
                        mKey1 = NOOBJECT;
                        break;
                    case 1:
                        mKey1 = ANYOBJECT;
                        break;
                    case 2:
                        mKey1 = "ReferencedObject";
                        break;
                    default:
                        mKey1 = getObjectKey(adv, var1 - 3, Dynamic);
                        break;
                }
                switch (var2) {
                    case 0:
                    case 6:
                        mObjectType = MRestriction.ObjectEnum.BeAtLocation;
                        if (var2 == 6) {
                            mMust = MustNot;
                        }
                        if (var3 == 0) {
                            mObjectType = BeHidden;
                        } else {
                            mKey2 = "Location" + var3;
                        }
                        break;
                    case 1:
                    case 7:
                        mObjectType = BeHeldByCharacter;
                        if (var2 == 7) {
                            mMust = MustNot;
                        }
                        switch (var3) {
                            case 0:
                                mKey2 = "%Player%";
                                break;
                            case 1:
                                mKey2 = "ReferencedCharacter";
                                break;
                            default:
                                mKey2 = "Character" + (var3 - 1 + iStartChar);
                                break;
                        }
                        break;
                    case 2:
                    case 8:
                        mObjectType = BeWornByCharacter;
                        if (var2 == 8) {
                            mMust = MustNot;
                        }
                        switch (var3) {
                            case 0:
                                mKey2 = "%Player%";
                                break;
                            case 1:
                                mKey2 = "ReferencedCharacter";
                                break;
                            default:
                                mKey2 = "Character" + (var3 - 1 + iStartChar);
                                break;
                        }
                        break;
                    case 3:
                    case 9:
                        mObjectType = BeVisibleToCharacter;
                        if (var2 == 9) {
                            mMust = MustNot;
                        }
                        switch (var3) {
                            case 0:
                                mKey2 = "%Player%";
                                break;
                            case 1:
                                mKey2 = "ReferencedCharacter";
                                break;
                            default:
                                mKey2 = "Character" + (var3 - 1 + iStartChar);
                                break;
                        }
                        break;
                    case 4:
                    case 10:
                        mObjectType = BeInsideObject;
                        if (var2 == 10) {
                            mMust = MustNot;
                        }
                        switch (var3) {
                            case 0:
                                // Nothing
                                break;
                            default:
                                mKey2 = getObjectKey(adv, var3 - 1, Container);
                                break;
                        }
                        break;
                    case 5:
                    case 11:
                        mObjectType = BeOnObject;
                        if (var2 == 11) {
                            mMust = MustNot;
                        }
                        switch (var3) {
                            case 0:
                                // Nothing
                                break;
                            default:
                                mKey2 = getObjectKey(adv, var3 - 1, Surface);
                                break;
                        }
                }
                break;

            case 1:
                // ----------------------------------------------------------
                //                     State of object
                // ----------------------------------------------------------
                // For openable objects, you can restrict that the Referenced
                // object or an openable object must be open or closed, or
                // locked for lockable objects.
                // ----------------------------------------------------------
                mType = MRestriction.RestrictionTypeEnum.Object;
                mKey1 = (var1 == 0) ?
                        "ReferencedObject" : getObjectKey(adv, var1 - 1, WithStateOrOpenable);
                mMust = Must;
                mObjectType = BeInState;
                boolean bObRef = mKey1.equals("ReferencedObject");
                MObject ob = null;
                if (!bObRef) {
                    ob = adv.mObjects.get(mKey1);
                }
                switch (var2) {
                    case 0:
                        if (bObRef || ob.isOpenable()) {
                            mKey2 = "Open";
                        } else {
                            MProperty prop = dodgyArlStates.get(ob);
                            if (prop != null) {
                                String sIntended = dictDodgyStates.get(mKey1);
                                if (sIntended != null) {
                                    sIntended = sIntended.toLowerCase().split("\\|")[var2];
                                    for (String state : prop.mStates) {
                                        if (sIntended.equals(state.toLowerCase())) {
                                            mKey2 = state;
                                            break;
                                        }
                                    }
                                } else {
                                    mKey2 = prop.mStates.get(var2);
                                }
                            }
                        }
                        break;
                    case 1:
                        if (bObRef || ob.isOpenable()) {
                            mKey2 = "Closed";
                        } else {
                            MProperty prop = dodgyArlStates.get(ob);
                            if (prop != null) {
                                String sIntended = dictDodgyStates.get(mKey1);
                                if (sIntended != null) {
                                    sIntended = sIntended.toLowerCase().split("\\|")[var2];
                                    for (String state : prop.mStates) {
                                        if (sIntended.equals(state.toLowerCase())) {
                                            mKey2 = state;
                                            break;
                                        }
                                    }
                                } else {
                                    mKey2 = prop.mStates.get(var2);
                                }
                            }
                        }
                        break;
                    case 2:
                        if (bObRef || ob.isOpenable()) {
                            if (bObRef || ob.isLockable()) {
                                mKey2 = "Locked";
                            } else {
                                MProperty prop = dodgyArlStates.get(ob);
                                if (prop != null) {
                                    String sIntended = dictDodgyStates.get(mKey1);
                                    if (sIntended != null) {
                                        sIntended = sIntended.toLowerCase().split("\\|")[var2 - 2];
                                        for (String state : prop.mStates) {
                                            if (sIntended.equals(state.toLowerCase())) {
                                                mKey2 = state;
                                                break;
                                            }
                                        }
                                    } else {
                                        mKey2 = prop.mStates.get(var2 - 2);
                                    }
                                }
                            }
                        } else {
                            MProperty prop = dodgyArlStates.get(ob);
                            if (prop != null) {
                                String sIntended = dictDodgyStates.get(mKey1);
                                if (sIntended != null) {
                                    sIntended = sIntended.toLowerCase().split("\\|")[var2];
                                    for (String state : prop.mStates) {
                                        if (sIntended.equals(state.toLowerCase())) {
                                            mKey2 = state;
                                            break;
                                        }
                                    }
                                } else {
                                    mKey2 = prop.mStates.get(var2);
                                }
                            }
                        }
                        break;
                    default:
                        int iOffset = 0;
                        if (bObRef || ob.isOpenable()) {
                            iOffset = (bObRef || ob.isLockable()) ? 3 : 2;
                        }
                        MProperty prop = dodgyArlStates.get(ob);
                        if (prop != null) {
                            mKey2 = prop.mStates.get(var2 - iOffset);
                        }
                        break;
                }
                break;

            case 2:
                // ----------------------------------------------------------
                //                       Task state
                // ----------------------------------------------------------
                // You can restrict that any task must be completed or not.
                // ----------------------------------------------------------
                mType = MRestriction.RestrictionTypeEnum.Task;
                mKey1 = "Task" + (var1 + iStartTask);
                mMust = (var2 == 0) ? Must : MustNot;
                mTaskType = Complete;
                break;

            case 3:
                // ----------------------------------------------------------
                //                  Player & Characters
                // ----------------------------------------------------------
                // You can specify that the Player, Referenced character, or
                // specific character must or must not be in the same room as
                // the Player, Referenced character or specific character, or
                // that they must or must not be alone. You can also specify
                // that the Player must be standing, sitting or lying on a
                // specific object or the floor, or that the Player or
                // characters are of a specific gender.
                // ----------------------------------------------------------
                mType = MRestriction.RestrictionTypeEnum.Character;
                switch (var1) {
                    case 0:
                        mKey1 = "%Player%";
                        break;
                    case 1:
                        mKey1 = "ReferencedCharacter";
                        break;
                    default:
                        mKey1 = "Character" + (var1 - 1 + iStartChar);
                        break;
                }
                switch (var2) {
                    case 0:
                        // Same room as
                        mMust = Must;
                        mCharacterType = BeInSameLocationAsCharacter;
                        break;
                    case 1:
                        // Not same room as
                        mMust = MustNot;
                        mCharacterType = BeInSameLocationAsCharacter;
                        break;
                    case 2:
                        // Alone
                        mMust = Must;
                        mCharacterType = BeAlone;
                        break;
                    case 3:
                        // Not alone
                        mMust = MustNot;
                        mCharacterType = BeAlone;
                        break;
                    case 4:
                        // standing on
                        mMust = Must;
                        mCharacterType = BeStandingOnObject;
                        break;
                    case 5:
                        // sitting on
                        mMust = Must;
                        mCharacterType = BeSittingOnObject;
                        break;
                    case 6:
                        // lying on
                        mMust = Must;
                        mCharacterType = BeLyingOnObject;
                        break;
                    case 7:
                        // gender
                        mMust = Must;
                        mCharacterType = BeOfGender;
                        break;
                }
                switch (var2) {
                    case 0:
                    case 1:
                        switch (var3) {
                            case 0:
                                mKey2 = "%Player%";
                                break;
                            case 1:
                                mKey2 = "ReferencedCharacter";
                                break;
                            default:
                                mKey2 = "Character" + (var3 - 1 + iStartChar);
                                break;
                        }
                        break;
                    case 4:
                        // Standables
                        switch (var3) {
                            case 0:
                                // The floor
                                mKey2 = "TheFloor";
                                break;
                            default:
                                mKey2 = getObjectKey(adv, var3 - 1, Standable);
                                break;
                        }
                        break;
                    case 5:
                        // Sittables
                        switch (var3) {
                            case 0:
                                // The floor
                                mKey2 = "TheFloor";
                                break;
                            default:
                                mKey2 = getObjectKey(adv, var3 - 1, Sittable);
                                break;
                        }
                        break;
                    case 6:
                        // Lyables
                        switch (var3) {
                            case 0:
                                // The floor
                                mKey2 = "TheFloor";
                                break;
                            default:
                                mKey2 = getObjectKey(adv, var3 - 1, Lieable);
                                break;
                        }
                        break;
                    case 7:
                        // Gender
                        switch (var3) {
                            case 0:
                                // male
                                mKey2 = Male.toString();
                                break;
                            case 1:
                                // female
                                mKey2 = Female.toString();
                                break;
                            case 2:
                                // unknown
                                mKey2 = Unknown.toString();
                                break;
                        }
                        break;
                }
                break;

            case 4:
                // ----------------------------------------------------------
                //                      Variables
                // ----------------------------------------------------------
                // You can specify that the number the player typed, or a
                // specific variable must be less than, less than or equal to,
                // equal to, greater than or equal to, or greater than a
                // specific value or variable.
                // ----------------------------------------------------------
                mType = MRestriction.RestrictionTypeEnum.Variable;
                switch (var1) {
                    case 0:
                        mKey1 = "ReferencedNumber";
                        break;
                    case 1:
                        mKey1 = "ReferencedText";
                        break;
                    default:
                        mKey1 = "Variable" + (var1 - 1);
                        break;
                }
                mKey2 = ""; // Arrays not used in v4
                mMust = Must;
                switch (var2) {
                    case 0:
                    case 10:
                        mVariableType = LessThan;
                        break;
                    case 1:
                    case 11:
                        mVariableType = LessThanOrEqualTo;
                        break;
                    case 2:
                    case 12:
                        mVariableType = EqualTo;
                        break;
                    case 3:
                    case 13:
                        mVariableType = GreaterThanOrEqualTo;
                        break;
                    case 4:
                    case 14:
                        mVariableType = GreaterThan;
                        break;
                    case 5:
                    case 15:
                        mVariableType = EqualTo;
                        mMust = MustNot;
                        break;
                }
                if (var2 < 10) {
                    mIntValue = var3;
                    if (v >= 4) {
                        mStringValue = sVar4;
                    }
                } else {
                    mIntValue = Integer.MIN_VALUE;
                    mStringValue = "Variable" + var3;
                }
        }
    }

    public void fixOlderVars() {
        if (mType == MRestriction.RestrictionTypeEnum.Variable && mKey1 != null) {
            boolean refText = mKey1.equals("ReferencedText");
            MVariable var = (refText) ? null : mAdv.mVariables.get(mKey1);
            if (refText || (var != null && var.getType() == TEXT)) {
                switch (mVariableType) {
                    case LessThan:
                        mVariableType = EqualTo;
                        break;
                    case LessThanOrEqualTo:
                        mVariableType = EqualTo;
                        mMust = MustNot;
                        break;
                }
                mStringValue = "\"" + mStringValue + "\"";
            }
        }
    }

    private boolean passesLocationRestriction() {
        if (mKey1 == null) {
            return false;
        }

        MLocation loc = null;
        switch (mKey1) {
            case PLAYERLOCATION:
                loc = mAdv.mLocations.get(mAdv.getPlayer().getLocation().getLocationKey());
                break;
            default:
                if (mAdv.mLocations.containsKey(mKey1)) {
                    loc = mAdv.mLocations.get(mKey1);
                }
                break;
        }

        if (loc != null) {
            switch (mLocationType) {
                case HaveBeenSeenByCharacter:
                    return (mKey2 != null && loc.getSeenBy(mKey2));

                case BeInGroup:
                    if (mKey2 == null) {
                        return false;
                    }
                    switch (mKey1) {
                        case PLAYERLOCATION:
                            return mAdv.mGroups.get(mKey2)
                                    .getArlMembers().contains(mAdv.getPlayer().getLocation().getLocationKey());
                        default:
                            return mAdv.mGroups.get(mKey2)
                                    .getArlMembers().contains(mKey1);
                    }

                case HaveProperty:
                    return (mKey2 != null && loc.hasProperty(mKey2));

                case BeLocation:
                    if (mKey2 == null) {
                        return false;
                    }
                    switch (mKey1) {
                        case PLAYERLOCATION:
                            return mAdv.getPlayer().getLocation().getLocationKey().equals(mKey2);
                        default:
                            return (mKey1.equals(mKey2));
                    }

                case Exist:
                    return true;
            }
        }
        return false;
    }

    /**
     * Determines if the object restrictions of this restriction pass.
     *
     * @param objectRefs - may be NULL. Only used if this restriction
     *                   is BeExactText ReferencedObjects, in order to check
     *                   whether the first or second reference is the exact
     *                   text "all".
     * @return TRUE if they pass, FALSE otherwise.
     */
    private boolean passesObjectRestriction(final @Nullable MReferenceList objectRefs) {
        if (mKey1 == null) {
            return false;
        }

        switch (mObjectType) {
            case BeAtLocation:
                // Is object mKey1 at location mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case MGlobals.NOOBJECT:
                        return mAdv.mLocations.get(mKey2)
                                .getObjectsInLocation(AllObjects, false).size() == 0;
                    case MGlobals.ANYOBJECT:
                        return mAdv.mLocations.get(mKey2)
                                .getObjectsInLocation(AllObjects, false).size() > 0;
                    default:
                        return mAdv.mObjects.get(mKey1).existsAtLocation(mKey2);
                }

            case BeHeldByCharacter:
                // Is object mKey1 held by character mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("No object held by any character test");
                                return false;
                            default:
                                return mAdv.mCharacters.get(mKey2).getHeldObjects().size() == 0;
                        }
                    case ANYOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("Any object held by any character test");
                                return false;
                            default:
                                return mAdv.mCharacters.get(mKey2).getHeldObjects().size() > 0;
                        }
                    default:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                return mAdv.mObjects.get(mKey1).isHeldByAnyone();
                            default:
                                return mAdv.mCharacters.get(mKey2).isHoldingObject(mKey1);
                        }
                }

            case BeHeldDirectlyByCharacter:
                // Is object mKey1 held directly by character mKey2?
                // That is, it's not in a container, etc. held by the character
                // This is a Bebek-specific addition to support players typing
                // command like "get tray" to remove a tray from a bag the player
                // is carrying (the original code would say something like "You
                // are already carrying the tray!")
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("No object directly held by any character test");
                                return false;
                            default:
                                return mAdv.mCharacters.get(mKey2).getHeldObjects(false).size() == 0;
                        }
                    case ANYOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("Any object directly held by any character test");
                                return false;
                            default:
                                return mAdv.mCharacters.get(mKey2).getHeldObjects(false).size() > 0;
                        }
                    default:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                return mAdv.mObjects.get(mKey1).isHeldByAnyone();
                            default:
                                return mAdv.mCharacters.get(mKey2).isHoldingObject(mKey1, true);
                        }
                }

            case BeHidden:
                // Is object mKey1 hidden?
                switch (mKey1) {
                    case NOOBJECT:
                        MGlobals.TODO("No object hidden");
                        return false;
                    case ANYOBJECT:
                        MGlobals.TODO("Any object hidden");
                        return false;
                    default:
                        if (mAdv.mObjects.get(mKey1).isStatic()) {
                            return mAdv.mObjects.get(mKey1).getLocation()
                                    .mStaticExistWhere == NoRooms;
                        } else {
                            return mAdv.mObjects.get(mKey1).getLocation()
                                    .mDynamicExistWhere == Hidden;
                        }
                }

            case BeInGroup:
                // Is object mKey1 a member of group mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        return mAdv.mGroups.get(mKey2).getArlMembers().size() == 0;
                    case ANYOBJECT:
                        return mAdv.mGroups.get(mKey2).getArlMembers().size() > 0;
                    default:
                        return mAdv.mGroups.get(mKey2).getArlMembers().contains(mKey1);
                }

            case BeInsideObject:
                // Is object mKey1 inside object mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        switch (mKey2) {
                            case NOOBJECT:
                                MGlobals.TODO("No object in no object test");
                                return false;
                            case ANYOBJECT:
                                MGlobals.TODO("No object in any object test");
                                return false;
                            default:
                                return mAdv.mObjects.get(mKey2)
                                        .getChildObjects(InsideObject).size() == 0;
                        }
                    case ANYOBJECT:
                        switch (mKey2) {
                            case NOOBJECT:
                                MGlobals.TODO("Any object in no object test");
                                return false;
                            case ANYOBJECT:
                                MGlobals.TODO("Any object in any object test");
                                return false;
                            default:
                                return mAdv.mObjects.get(mKey2)
                                        .getChildObjects(InsideObject).size() > 0;
                        }
                    default:
                        switch (mKey2) {
                            case NOOBJECT:
                                MGlobals.TODO("Object inside no object test");
                                return false;
                            case ANYOBJECT:
                                return mAdv.mObjects.get(mKey1).getLocation()
                                        .mDynamicExistWhere == InObject;
                            default:
                                return mAdv.mObjects.get(mKey1).isInside(mKey2);
                        }
                }

            case BeInState:
                // Is object mKey1 currently in state mKey2?
                //
                // TCC: We check the value of each state list property of
                // mKey1, excluding any appended properties, to see if it
                // matches mKey2. It is important that we don't include appended
                // properties when doing this check.
                //
                // For example, locked doors will typically be modelled using an
                // object with a state list property "OpenStatus" set to "Locked".
                // Usually OpenStatus will allow only two states "Open" and "Closed".
                // However locked doors will include an additional property appended
                // to OpenStatus called "LockedStatus" that adds another state "Locked".
                // When testing whether the door is locked or not, such games will typically
                // include a restriction on the door such as "Referenced Object must be in
                // state 'Locked'". If we do not excluded appended properties, when testing
                // this restriction we'll pick up the default value of the "LockedStatus"
                // property which will always be "Locked". And hence it will never be
                // possible to unlock the door.
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        for (MObject ob : mAdv.mObjects.values()) {
                            for (MProperty prop : ob.getProperties().values()) {
                                if (prop.getType() == StateList &&
                                        prop.getValue().equals(mKey2) &&
                                        prop.getAppendToProperty().equals("")) {
                                    return false;
                                }
                            }
                        }
                        return true;
                    case ANYOBJECT:
                        for (MObject ob : mAdv.mObjects.values()) {
                            for (MProperty prop : ob.getProperties().values()) {
                                if (prop.getType() == StateList &&
                                        prop.getValue().equals(mKey2) &&
                                        prop.getAppendToProperty().equals("")) {
                                    return true;
                                }
                            }
                        }
                        break;
                    default:
                        for (MProperty prop : mAdv.mObjects.get(mKey1).getProperties().values()) {
                            if (prop.getType() == StateList &&
                                    prop.getValue().equals(mKey2) &&
                                    prop.getAppendToProperty().equals("")) {
                                return true;
                            }
                        }
                        break;
                }
                return false;

            case BeOnObject:
                // Is object mKey1 on object mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        switch (mKey2) {
                            case NOOBJECT:
                                MGlobals.TODO("No Object must be on No Object test");
                                return false;
                            case ANYOBJECT:
                                MGlobals.TODO("No Object must be on Any Object test");
                                return false;
                            default:
                                return mAdv.mObjects.get(mKey2)
                                        .getChildObjects(OnObject).size() == 0;
                        }
                    case ANYOBJECT:
                        switch (mKey2) {
                            case NOOBJECT:
                                MGlobals.TODO("Any Object must be on No Object test");
                                return false;
                            case ANYOBJECT:
                                MGlobals.TODO("Any Object must be on Any Object test");
                                return false;
                            default:
                                return mAdv.mObjects.get(mKey2)
                                        .getChildObjects(OnObject).size() > 0;
                        }
                    default:
                        switch (mKey2) {
                            case NOOBJECT:
                                MGlobals.TODO("Object must be on No Object test");
                                return false;
                            case ANYOBJECT:
                                return mAdv.mObjects.get(mKey1).getLocation()
                                        .mDynamicExistWhere == MObject.MObjectLocation.DynamicExistsWhereEnum.OnObject;
                            default:
                                return mAdv.mObjects.get(mKey1).isOn(mKey2);
                        }
                }

            case BePartOfCharacter:
                // Is object mKey1 part of character mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        MGlobals.TODO("No Object is part of character test");
                        return false;
                    case ANYOBJECT:
                        MGlobals.TODO("Any Object is part of character test");
                        return false;
                    default:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("Object is part of any character test");
                                return false;
                            default:
                                return mAdv.mObjects.get(mKey1).getLocation()
                                        .mStaticExistWhere == PartOfCharacter &&
                                        mAdv.mObjects.get(mKey1).getLocation().getKey().equals(mKey2);
                        }
                }

            case BePartOfObject:
                // Is object mKey1 part of object mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        MGlobals.TODO("No Object is part of object test");
                        return false;
                    case ANYOBJECT:
                        MGlobals.TODO("Any Object is part of object test");
                        return false;
                    default:
                        switch (mKey2) {
                            case NOOBJECT:
                                MGlobals.TODO("Object is part of No object test");
                                return false;
                            case ANYOBJECT:
                                return mAdv.mObjects.get(mKey1).getLocation()
                                        .mStaticExistWhere == MObject.MObjectLocation.StaticExistsWhereEnum.PartOfObject;
                            default:
                                return mAdv.mObjects.get(mKey1).getLocation()
                                        .mStaticExistWhere == MObject.MObjectLocation.StaticExistsWhereEnum.PartOfObject &&
                                        mAdv.mObjects.get(mKey1).getLocation().getKey().equals(mKey2);
                        }
                }

            case HaveBeenSeenByCharacter:
                // Has object mKey1 already been seen by character mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        MGlobals.TODO("No Object has been seen by character test");
                        return false;
                    case ANYOBJECT:
                        MGlobals.TODO("Any Object has been seen by character test");
                        return false;
                    default:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("Object has been seen by any character test");
                                return false;
                            default:
                                return mAdv.mCharacters.get(mKey2).hasSeenObject(mKey1);
                        }
                }

            case BeVisibleToCharacter:
                // Is object mKey1 visible to character to mKey2?
                if (mKey2 == null) {
                    return false;
                }
                MCharacter ch = mAdv.mCharacters.get(mKey2);
                if (ch == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("No Object visible to any character test");
                                return false;
                            default:
                                for (MObject ob : mAdv.mObjects.values()) {
                                    if (ch.canSeeObject(ob.getKey())) {
                                        return false;
                                    }
                                }
                                return true;
                        }
                    case ANYOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("Any Object visible to any character test");
                                return false;
                            default:
                                for (MObject ob : mAdv.mObjects.values()) {
                                    if (ch.canSeeObject(ob.getKey())) {
                                        return true;
                                    }
                                }
                                return false;
                        }
                    default:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("Object visible to any character test");
                                return false;
                            default:
                                return ch.canSeeObject(mKey1);
                        }
                }

            case BeWornByCharacter:
                // Is object mKey1 being worn by character mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("No Object worn by any character test");
                                return false;
                            default:
                                return mAdv.mCharacters.get(mKey2).getWornObjects().size() == 0;
                        }
                    case ANYOBJECT:
                        switch (mKey2) {
                            case ANYCHARACTER:
                                MGlobals.TODO("Any Object worn by any character test");
                                return false;
                            default:
                                return mAdv.mCharacters.get(mKey2).getWornObjects().size() > 0;
                        }
                    default:
                        if (mAdv.mObjects.containsKey(mKey1)) {
                            switch (mKey2) {
                                case ANYCHARACTER:
                                    return mAdv.mObjects.get(mKey1).getLocation()
                                            .mDynamicExistWhere == WornByCharacter;
                                default:
                                    return mAdv.mCharacters.get(mKey2).isWearingObject(mKey1);
                            }
                        }
                        return false;
                }

            case Exist:
                // Does mKey1 refer to a valid object?
                switch (mKey1) {
                    case NOOBJECT:
                        return mAdv.mObjects.size() == 0;
                    case ANYOBJECT:
                        return mAdv.mObjects.size() > 0;
                    default:
                        return mAdv.mObjects.containsKey(mKey1);
                }

            case HaveProperty:
                // Does object mKey1 have the property mKey2?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        for (MObject ob : mAdv.mObjects.values()) {
                            if (ob.hasProperty(mKey2)) {
                                return false;
                            }
                        }
                        return true;
                    case ANYOBJECT:
                        for (MObject ob : mAdv.mObjects.values()) {
                            if (ob.hasProperty(mKey2)) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        return mAdv.mObjects.containsKey(mKey1) &&
                                mAdv.mObjects.get(mKey1).hasProperty(mKey2);
                }

            case BeExactText:
                // Does mKey1 equal "ReferencedObjects" and
                // was the text the user typed "all"?
                switch (mKey1) {
                    case NOOBJECT:
                        MGlobals.TODO("No Object be exact text test");
                        return false;
                    case ANYOBJECT:
                        MGlobals.TODO("Any Object be exact text test");
                        return false;
                    default:
                        // Should probably verify that %objects% was actually reference 0
                        return mKey1.equals("ReferencedObjects") &&
                                objectRefs != null &&
                                ((objectRefs.size() > 0 &&
                                        objectRefs.get(0).mItems.get(0).mCommandReference.equals("all")) ||
                                        (objectRefs.size() > 1 &&
                                                objectRefs.get(1).mItems.size() > 0 &&
                                                objectRefs.get(1).mItems.get(0).mCommandReference.equals("all")));
                }

            case BeObject:
                // Does mKey1 and mKey2 refer to the same object?
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case NOOBJECT:
                        MGlobals.TODO("No Object be specific object test");
                        return false;
                    case ANYOBJECT:
                        MGlobals.TODO("Any Object be specific object test");
                        return false;
                    default:
                        return mKey1.equals(mKey2);
                }
        }

        return false;
    }

    private boolean passesTaskRestriction() {
        switch (mTaskType) {
            case Complete:
                if (mAdv.mTasks.containsKey(mKey1)) {
                    return mAdv.mTasks.get(mKey1).getCompleted();
                }
                break;
        }
        return false;
    }

    private boolean passesVariableRestriction(final @Nullable MReferenceList refs) {
        if (mKey1 == null) {
            return false;
        }
        MVariable var;
        int iIndex = 1;
        if (mKey1.startsWith("ReferencedNumber")) {
            var = new MVariable(mAdv);
            var.setType(NUMERIC);
            var.set(safeInt(refs != null ? refs.getReference(mKey1) : null));
        } else if (mKey1.startsWith("ReferencedText")) {
            int iRef = Math.max(safeInt(mKey1.replace("ReferencedText", "")) - 1, 0);
            var = new MVariable(mAdv);
            var.setType(TEXT);
            var.set(mAdv.mReferencedText[iRef]);
        } else {
            var = mAdv.mVariables.get(mKey1);
            if (mKey2 != null && !mKey2.equals("")) {
                if (mAdv.mVariables.containsKey(mKey2)) {
                    iIndex = mAdv.mVariables.get(mKey2).getInt();
                } else {
                    iIndex = safeInt(mAdv.evaluateStringExpression(mKey2, refs));
                }
            }
        }
        int iIntVal = 0;
        String sStringVal = "";
        if (var.getType() == NUMERIC) {
            if (mIntValue == Integer.MIN_VALUE && !mStringValue.equals("")) {
                iIntVal = mAdv.mVariables.get(mStringValue).getInt(); // Variable value
            } else {
                if (!mStringValue.equals("") && !mStringValue.equals(String.valueOf(mIntValue))) {
                    iIntVal = safeInt(mAdv.evaluateStringExpression(mStringValue, refs)); // Expression
                } else {
                    iIntVal = mIntValue; // Integer value
                }
            }
        } else {
            if (mIntValue == Integer.MIN_VALUE && !mStringValue.equals("")) {
                sStringVal = mAdv.mVariables.get(mStringValue).getStr();
            } else {
                sStringVal = mAdv.evaluateStringExpression(mStringValue, refs);
            }
        }
        switch (mVariableType) {
            case EqualTo:
                if (var.getType() == NUMERIC) {
                    return (var.getIntAt(iIndex) == iIntVal);
                } else {
                    return (var.getStrAt(iIndex).equals(sStringVal));
                }
            case GreaterThan:
                if (var.getType() == NUMERIC) {
                    return (var.getIntAt(iIndex) > iIntVal);
                } else {
                    return (var.getStrAt(iIndex).compareTo(sStringVal) > 0);
                }
            case GreaterThanOrEqualTo:
                if (var.getType() == NUMERIC) {
                    return (var.getIntAt(iIndex) >= iIntVal);
                } else {
                    return (var.getStrAt(iIndex).compareTo(sStringVal) >= 0);
                }
            case LessThan:
                if (var.getType() == NUMERIC) {
                    return (var.getIntAt(iIndex) < iIntVal);
                } else {
                    return (var.getStrAt(iIndex).compareTo(sStringVal) < 0);
                }
            case LessThanOrEqualTo:
                if (var.getType() == NUMERIC) {
                    return (var.getIntAt(iIndex) <= iIntVal);
                } else {
                    return (var.getStrAt(iIndex).compareTo(sStringVal) <= 0);
                }
            case Contain:
                if (var.getType() == TEXT) {
                    return var.getStrAt(iIndex).contains(sStringVal);
                }
                break;
        }
        return false;
    }

    private boolean passesCharacterRestriction() {
        // If the keys do not exist (game error), we default
        // to passing the restriction, in the hope that at
        // least the player can continue with the game.
        if (mKey1 == null) {
            return false;
        }

        // If mKey1 is a pointer to a specific character,
        // attempt to dereference it now.
        MCharacter ch1 = null;
        if (!mKey1.equals(ANYCHARACTER)) {
            ch1 = mAdv.mCharacters.get(mKey1);
            if (ch1 == null) {
                MGlobals.errMsg("Bad restriction - refers to non-existent character: " +
                        getSummary());
                return true;
            }
        }

        switch (mCharacterType) {
            case BeAlone:
                // ------------------------------------------------
                //           Is character mKey1 alone?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter ch : mAdv.mCharacters.values()) {
                            if (ch.isAlone()) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        return ch1.isAlone();
                }

            case BeAloneWith:
                // ------------------------------------------------
                //           Is character mKey1 alone with
                //                character mKey2?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        MGlobals.TODO("Any Character be alone");
                        return false;
                    default:
                        if (mKey2 == null) {
                            return false;
                        }
                        assert ch1 != null;
                        String chOtherChar = ch1.getAloneWithChar();
                        return mKey2.equals(ANYCHARACTER) ?
                                !chOtherChar.equals("") : chOtherChar.equals(mKey2);
                }

            case BeAtLocation:
                // ------------------------------------------------
                //      Is character mKey1 at location mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            if (c.getLocation().getLocationKey().equals(mKey2)) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        return chLoc.getLocationKey().equals(mKey2);
                }

            case BeCharacter:
                // ------------------------------------------------
                //          Do mKey1 and mKey2 refer to the
                //                 same character?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        // Pointless!
                        return true;
                    default:
                        return mKey1.equals(mKey2);
                }

            case BeInConversationWith:
                // ------------------------------------------------
                //           Is character mKey1 in a
                //       conversation with character mKey2?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        MGlobals.TODO("Any Character be in conversation with");
                        return false;
                    default:
                        if (mKey2 == null) {
                            return false;
                        }
                        return (mKey2.equals(ANYCHARACTER)) ?
                                !mAdv.mConversationCharKey.equals("") :
                                mAdv.mConversationCharKey.equals(mKey2);
                }

            case Exist:
                // ------------------------------------------------
                //      Does mKey1 refer to a valid character?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        return mAdv.mCharacters.size() > 0;
                    default:
                        return mAdv.mCharacters.containsKey(mKey1);
                }

            case HaveRouteInDirection:
                // ------------------------------------------------
                //        Does character mKey1 have a route
                //              in direction mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                MParser.mRouteErrorText = mMessage.toString();
                StringBuilder sSpecificError = new StringBuilder();
                switch (mKey1) {
                    case ANYCHARACTER:
                        switch (mKey2) {
                            case ANYDIRECTION:
                                for (MCharacter c : mAdv.mCharacters.values()) {
                                    for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
                                        if (c.hasRouteInDirection(d, false, "", sSpecificError)) {
                                            return true;
                                        }
                                    }
                                }
                                break;
                            default:
                                for (MCharacter c : mAdv.mCharacters.values()) {
                                    if (c.hasRouteInDirection(MAdventure.DirectionsEnum.valueOf(mKey2),
                                            false, "", sSpecificError)) {
                                        return true;
                                    }
                                }
                        }
                        break;
                    default:
                        assert ch1 != null;
                        switch (mKey2) {
                            case ANYDIRECTION:
                                for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
                                    if (ch1.hasRouteInDirection(d, false, "", sSpecificError)) {
                                        return true;
                                    }
                                }
                                break;
                            default:
                                if (ch1.hasRouteInDirection(MAdventure.DirectionsEnum.valueOf(mKey2),
                                        false, "", sSpecificError)) {
                                    return true;
                                }
                                break;
                        }
                }
                if (sSpecificError.length() > 0) {
                    MParser.mRouteErrorText = sSpecificError.toString();
                }
                return false;

            case HaveSeenCharacter:
                // ------------------------------------------------
                //    Has character mKey1 seen character mKey2?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        MGlobals.TODO("Any Character seen character");
                        return false;
                    default:
                        assert ch1 != null;
                        return (mKey2 != null && ch1.hasSeenCharacter(mKey2));
                }

            case HaveSeenLocation:
                // ------------------------------------------------
                //    Has character mKey1 seen location mKey2?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        MGlobals.TODO("Any Character seen location");
                        return false;
                    default:
                        assert ch1 != null;
                        return (mKey2 != null && ch1.hasSeenLocation(mKey2));
                }

            case HaveSeenObject:
                // ------------------------------------------------
                //     Has character mKey1 seen object mKey2?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        MGlobals.TODO("Any Character seen object");
                        return false;
                    default:
                        assert ch1 != null;
                        return (mKey2 != null && ch1.hasSeenObject(mKey2));
                }

            case BeHoldingObject:
                // ------------------------------------------------
                //          Is character mKey1 currently
                //            holding object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        return mAdv.mObjects.get(mKey2).isHeldByAnyone();
                    default:
                        assert ch1 != null;
                        return ch1.isHoldingObject(mKey2);
                }

            case BeInSameLocationAsCharacter:
                // ------------------------------------------------
                //         Is character mKey1 in the same
                //          location as character mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }

                MCharacter ch2 = null;
                if (!mKey2.equals(ANYCHARACTER)) {
                    ch2 = mAdv.mCharacters.get(mKey2);
                    if (ch2 == null) {
                        MGlobals.errMsg("Bad restriction - refers to non-existent character: " +
                                getSummary());
                        return true;
                    }
                }

                switch (mKey1) {
                    case ANYCHARACTER:
                        assert ch2 != null;
                        return !ch2.isAlone();
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        switch (mKey2) {
                            case ANYCHARACTER:
                                for (MCharacter c : mAdv.mCharacters.values()) {
                                    if (!c.getKey().equals(mKey1) &&
                                            c.getLocation().getLocationKey()
                                                    .equals(chLoc.getLocationKey())) {
                                        return true;
                                    }
                                }
                                return false;
                            default:
                                assert ch2 != null;
                                return chLoc.getLocationKey()
                                        .equals(ch2.getLocation().getLocationKey());
                        }
                }

            case BeVisibleToCharacter:
                // ------------------------------------------------
                //         Is character mKey1 currently
                //         visible to character mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                ArrayList<MCharacter> chars = new ArrayList<>();
                switch (mKey1) {
                    case ANYCHARACTER:
                        chars.addAll(mAdv.mCharacters.values());
                        break;
                    default:
                        assert ch1 != null;
                        chars.add(ch1);
                        break;
                }
                for (MCharacter c : chars) {
                    if (mKey2.equals(ANYCHARACTER)) {
                        for (MCharacter c2 : mAdv.mCharacters.values()) {
                            String c2Key = c2.getKey();
                            if (!c.getKey().equals(c2Key) &&
                                    c.canSeeCharacter(c2Key)) {
                                return true;
                            }
                        }
                    } else if (c.canSeeCharacter(mKey2)) {
                        return true;
                    }
                }
                return false;

            case BeInSameLocationAsObject:
                // ------------------------------------------------
                //      Is character mKey1 in the same location
                //                as object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            if (c.canSeeObject(mKey2)) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        return ch1.canSeeObject(mKey2);
                }

            case BeLyingOnObject:
                // ------------------------------------------------
                //    Is character mKey1 lying on object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            MCharacter.MCharacterLocation chLoc = c.getLocation();
                            if (chLoc.getPosition() == Lying) {
                                switch (mKey2) {
                                    case ANYOBJECT:
                                        if (chLoc.getExistsWhere() ==
                                                MCharacter.MCharacterLocation.ExistsWhere.OnObject) {
                                            return true;
                                        }
                                        break;
                                    case THEFLOOR:
                                        if (chLoc.getExistsWhere()
                                                .equals(AtLocation)) {
                                            return true;
                                        }
                                        break;
                                    default:
                                        if (chLoc.getExistsWhere() ==
                                                MCharacter.MCharacterLocation.ExistsWhere.OnObject &&
                                                chLoc.getKey().equals(mKey2)) {
                                            return true;
                                        }
                                        break;
                                }
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        switch (mKey2) {
                            case ANYOBJECT:
                                return chLoc.getPosition() == Lying &&
                                        chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject;
                            case THEFLOOR:
                                return chLoc.getPosition() == Lying &&
                                        chLoc.getExistsWhere() == AtLocation;
                            default:
                                return (chLoc.getPosition() == Lying &&
                                        chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject &&
                                        chLoc.getKey().equals(mKey2));
                        }
                }

            case BeInGroup:
                // ------------------------------------------------
                // Is character mKey1 a member of the group mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                MStringArrayList grpMembers = mAdv.mGroups.get(mKey2).getArlMembers();
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            if (grpMembers.contains(c.getKey())) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        return grpMembers.contains(mKey1);
                }

            case BeOfGender:
                // ------------------------------------------------
                //      Is character mKey1 of gender mKey2?
                // ------------------------------------------------
                switch (mKey1) {
                    case ANYCHARACTER:
                        MGlobals.TODO("Any Character be of gender");
                        return false;
                    default:
                        assert ch1 != null;
                        return ch1.getGender() == MCharacter.Gender.valueOf(mKey2);
                }

            case BeSittingOnObject:
                // ------------------------------------------------
                //   Is character mKey1 sitting on object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            MCharacter.MCharacterLocation chLoc = c.getLocation();
                            if (chLoc.getPosition() == Sitting) {
                                switch (mKey2) {
                                    case MGlobals.ANYOBJECT:
                                        if (chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject) {
                                            return true;
                                        }
                                        break;
                                    case THEFLOOR:
                                        if (chLoc.getExistsWhere() == AtLocation) {
                                            return true;
                                        }
                                        break;
                                    default:
                                        if (chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject &&
                                                chLoc.getKey().equals(mKey2)) {
                                            return true;
                                        }
                                        break;
                                }
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        switch (mKey2) {
                            case MGlobals.ANYOBJECT:
                                return chLoc.getPosition() == Sitting &&
                                        chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject;
                            case THEFLOOR:
                                return chLoc.getPosition() == Sitting &&
                                        chLoc.getExistsWhere() == AtLocation;
                            default:
                                return (chLoc.getPosition() == Sitting &&
                                        chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject &&
                                        chLoc.getKey().equals(mKey2));
                        }
                }

            case BeStandingOnObject:
                // ------------------------------------------------
                //   Is character mKey1 standing on object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            MCharacter.MCharacterLocation chLoc = c.getLocation();
                            if (chLoc.getPosition() == Standing) {
                                switch (mKey2) {
                                    case MGlobals.ANYOBJECT:
                                        if (chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject) {
                                            return true;
                                        }
                                        break;
                                    case THEFLOOR:
                                        if (chLoc.getExistsWhere() == AtLocation) {
                                            return true;
                                        }
                                        break;
                                    default:
                                        if (chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject &&
                                                chLoc.getKey().equals(mKey2)) {
                                            return true;
                                        }
                                        break;
                                }
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        switch (mKey2) {
                            case MGlobals.ANYOBJECT:
                                return chLoc.getPosition() == Standing &&
                                        chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject;
                            case THEFLOOR:
                                return chLoc.getPosition() == Standing &&
                                        chLoc.getExistsWhere() == AtLocation;
                            default:
                                return (chLoc.getPosition() == Standing &&
                                        chLoc.getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.OnObject &&
                                        chLoc.getKey().equals(mKey2));
                        }
                }

            case BeWearingObject:
                // ------------------------------------------------
                //    Is character mKey1 wearing object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        return mAdv.mObjects.get(mKey2).isWornByAnyone();
                    default:
                        assert ch1 != null;
                        return ch1.isWearingObject(mKey2);
                }

            case BeWithinLocationGroup:
                // ------------------------------------------------
                //     Is character mKey1 in a location that
                //           is part of group mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                MStringArrayList grpKeys = mAdv.mGroups.get(mKey2).getArlMembers();
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            if (grpKeys.contains(c.getLocation().getLocationKey())) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        return grpKeys.contains(chLoc.getLocationKey());
                }

            case HaveProperty:
                // ------------------------------------------------
                //          Does character mKey1 have the
                //                property mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            if (c.hasProperty(mKey2)) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        return ch1.hasProperty(mKey2);
                }

            case BeInPosition:
                // ------------------------------------------------
                //     Is character mKey1 in position mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        for (MCharacter c : mAdv.mCharacters.values()) {
                            if (c.hasProperty("CharacterPosition") &&
                                    c.getPropertyValue("CharacterPosition").equals(mKey2)) {
                                return true;
                            }
                        }
                        return false;
                    default:
                        assert ch1 != null;
                        if (ch1.hasProperty("CharacterPosition")) {
                            return ch1.getPropertyValue("CharacterPosition").equals(mKey2);
                        }
                        return false;
                }

            case BeInsideObject:
                // ------------------------------------------------
                //    Is character mKey1 inside object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        switch (mKey2) {
                            case MGlobals.ANYOBJECT:
                                MGlobals.TODO("Any Character be inside any object");
                                return false;
                            default:
                                return mAdv.mObjects.get(mKey2)
                                        .getChildChars(InsideObject).size() > 0;
                        }
                    default:
                        assert ch1 != null;
                        if (mKey2.equals(MGlobals.ANYOBJECT)) {
                            return (ch1.getLocation().getExistsWhere() ==
                                    MCharacter.MCharacterLocation.ExistsWhere.InObject);
                        } else {
                            switch (mKey1) {
                                case MGlobals.ANYOBJECT:
                                    return mAdv.mObjects.get(mKey2)
                                            .getChildObjects(InsideObject).size() > 0;
                                default:
                                    MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                                    return chLoc.getExistsWhere() ==
                                            MCharacter.MCharacterLocation.ExistsWhere.InObject &&
                                            chLoc.getKey().equals(mKey2);
                            }
                        }
                }

            case BeOnObject:
                // ------------------------------------------------
                //      Is character mKey1 on object mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        if (mKey2.equals(MGlobals.ANYOBJECT)) {
                            MGlobals.TODO("Any Character on Any Object");
                            return false;
                        } else {
                            return mAdv.mObjects.get(mKey2)
                                    .getChildChars(OnObject).size() > 0;
                        }
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        if (mKey2.equals(MGlobals.ANYOBJECT)) {
                            return (chLoc.getExistsWhere() ==
                                    MCharacter.MCharacterLocation.ExistsWhere.OnObject);
                        } else {
                            return chLoc.getExistsWhere() ==
                                    MCharacter.MCharacterLocation.ExistsWhere.OnObject &&
                                    chLoc.getKey().equals(mKey2);
                        }
                }

            case BeOnCharacter:
                // ------------------------------------------------
                //     Is character mKey1 on character mKey2?
                // ------------------------------------------------
                if (mKey2 == null) {
                    return false;
                }
                switch (mKey1) {
                    case ANYCHARACTER:
                        MGlobals.TODO("Any Character be on character");
                        return false;
                    default:
                        assert ch1 != null;
                        MCharacter.MCharacterLocation chLoc = ch1.getLocation();
                        if (mKey2.equals(ANYCHARACTER)) {
                            return (chLoc.getExistsWhere() == OnCharacter);
                        } else {
                            return chLoc.getExistsWhere() == OnCharacter &&
                                    chLoc.getKey().equals(mKey2);
                        }
                }
        }

        return false;
    }

    private boolean passesItemRestriction() {
        if (mKey1 == null) {
            return false;
        }

        MObject ob1;
        MCharacter ch1 = null;
        MLocation loc1 = null;

        if ((ob1 = mAdv.mObjects.get(mKey1)) == null &&
                (ch1 = mAdv.mCharacters.get(mKey1)) == null &&
                (loc1 = mAdv.mLocations.get(mKey1)) == null) {
            return false;
        }

        MGroup grp2;
        MCharacter ch2;
        MObject ob2;

        switch (mItemType) {
            case BeAtLocation:
                if (mKey2 != null) {
                    if (ob1 != null) {
                        return ob1.isVisibleAt(mKey2);
                    } else if (ch1 != null) {
                        return ch1.getLocation().getLocationKey().equals(mKey2);
                    } else {
                        return loc1.getKey().equals(mKey2);
                    }
                }
                return false;

            case BeCharacter:
                return (ch1 != null && ch1.getKey().equals(mKey2));

            case BeInGroup:
                return mKey2 != null &&
                        (grp2 = mAdv.mGroups.get(mKey2)) != null &&
                        grp2.getArlMembers().contains(mKey1);

            case BeInSameLocationAsCharacter:
                if (mKey2 != null && (ch2 = mAdv.mCharacters.get(mKey2)) != null) {
                    if (ob1 != null) {
                        return ch2.canSeeObject(ob1.getKey());
                    } else if (ch1 != null) {
                        return ch2.canSeeCharacter(ch1.getKey());
                    } else {
                        return ch2.getLocation().getLocationKey().equals(loc1.getKey());
                    }
                }
                return false;

            case BeInSameLocationAsObject:
                if (mKey2 != null && (ob2 = mAdv.mObjects.get(mKey2)) != null) {
                    if (ob1 != null) {
                        for (String sOb1Loc : ob1.getRootLocations().keySet()) {
                            for (String sOb2Loc : ob2.getRootLocations().keySet()) {
                                if (sOb1Loc.equals(sOb2Loc)) {
                                    return true;
                                }
                            }
                        }
                    } else if (ch1 != null) {
                        return ch1.canSeeObject(ob2.getKey());
                    } else {
                        return ob2.getRootLocations().containsKey(loc1.getKey());
                    }
                }
                return false;

            case BeInsideObject:
                return mKey2 != null &&
                        (ob2 = mAdv.mObjects.get(mKey2)) != null &&
                        ob2.getChildObjects(InsideObject, true)
                                .containsKey(mKey1);

            case BeLocation:
                return (loc1 != null && loc1.getKey().equals(mKey2));

            case BeObject:
                return (ob1 != null && ob1.getKey().equals(mKey2));

            case BeOnCharacter:
                if (mKey2 != null && (ch2 = mAdv.mCharacters.get(mKey2)) != null) {
                    if (ob1 != null) {
                        return ob1.getLocation().mStaticExistWhere ==
                                PartOfCharacter &&
                                ob1.getLocation().getKey().equals(ch2.getKey());
                    } else {
                        return ch1 != null && ch2.getChildChars(true)
                                .containsKey(ch1.getKey());
                    }
                }
                return false;

            case BeType:
                return mKey2 != null &&
                        ((mKey2.equals("Object") && ob1 != null) ||
                                (mKey2.equals("Character") && ch1 != null) ||
                                (mKey2.equals("Location") && loc1 != null));

            case Exist:
                return true;

            case HaveProperty:
                if (mKey2 == null) {
                    return false;
                }
                if (ob1 != null) {
                    return ob1.hasProperty(mKey2);
                } else if (ch1 != null) {
                    return ch1.hasProperty(mKey2);
                } else {
                    return loc1.hasProperty(mKey2);
                }
        }

        return false;
    }

    private boolean passesPropertyRestriction(boolean bIgnoreReferences,
                                              final @Nullable MReferenceList refs) {
        if (mKey1 == null || mKey2 == null) {
            return false;
        }

        MItem item;
        boolean bItemContainsProperty = false;
        if ((item = mAdv.mObjects.get(mKey2)) != null) {
            bItemContainsProperty = ((MObject) item).hasProperty(mKey1);
        } else if ((item = mAdv.mCharacters.get(mKey2)) != null) {
            bItemContainsProperty = ((MCharacter) item).hasProperty(mKey1);
        } else if ((item = mAdv.mLocations.get(mKey2)) != null) {
            bItemContainsProperty = ((MLocation) item).hasProperty(mKey1);
        }

        if (!bItemContainsProperty) {
            return false;
        } else {
            MProperty prop;
            if (item instanceof MObject) {
                prop = ((MObject) item).getProperty(mKey1);
            } else if (item instanceof MCharacter) {
                prop = ((MCharacter) item).getProperty(mKey1);
            } else {
                prop = ((MLocation) item).getProperty(mKey1);
            }

            switch (prop.getType()) {
                case CharacterKey:
                case LocationGroupKey:
                case LocationKey:
                case ObjectKey:
                    String sKey = (refs != null) ?
                            refs.getReference(mStringValue) : null;
                    if (sKey == null) {
                        sKey = mStringValue;
                    }
                    return prop.getValue().equals(sKey);

                case Integer:
                    int iCompareValue;
                    if (mAdv.mVariables.containsKey(mStringValue)) {
                        iCompareValue = mAdv.mVariables.get(mStringValue).getInt();
                    } else {
                        if (bIgnoreReferences) {
                            for (String sRef : REFERENCE_NAMES) {
                                if (mStringValue.contains(sRef)) {
                                    return true;
                                }
                            }
                        }
                        iCompareValue =
                                mAdv.evaluateIntExpression(mStringValue, refs);
                    }
                    switch (mIntValue) {
                        case 0:
                            // MRestriction.VariableEnum.LessThan
                            return (safeInt(prop.getValue()) < iCompareValue);
                        case 1:
                            // MRestriction.VariableEnum.LessThanOrEqualTo
                            return (safeInt(prop.getValue()) <= iCompareValue);
                        case 2:
                        case -1:
                            //  MRestriction.VariableEnum.EqualTo
                            return (safeInt(prop.getValue()) == iCompareValue);
                        case 3:
                            // MRestriction.VariableEnum.GreaterThanOrEqualTo
                            return (safeInt(prop.getValue()) >= iCompareValue);
                        case 4:
                            // MRestriction.VariableEnum.GreaterThan
                            return (safeInt(prop.getValue()) > iCompareValue);
                    }
                    return false;

                case SelectionOnly:
                    return true;

                case StateList:
                    return prop.getValue().equals(mStringValue);

                case Text:
                    String sStringVal2 =
                            mStringValue.contains("\"") ?
                                    mAdv.evaluateStringExpression(mStringValue, refs) :
                                    mAdv.evaluateFunctions(mStringValue, refs);
                    return prop.getValue().equals(sStringVal2);

                case ValueList:
                    int iCompareValue2;
                    if (isNumeric(mStringValue)) {
                        iCompareValue2 = safeInt(mStringValue);
                    } else {
                        if (bIgnoreReferences) {
                            for (String sRef : REFERENCE_NAMES) {
                                if (mStringValue.contains(sRef)) {
                                    return true;
                                }
                            }
                        }
                        iCompareValue2 =
                                mAdv.evaluateIntExpression(mStringValue, refs);
                    }
                    switch (mIntValue) {
                        case 0:
                            // MRestriction.VariableEnum.LessThan
                            return (safeInt(prop.getValue()) < iCompareValue2);
                        case 1:
                            // MRestriction.VariableEnum.LessThanOrEqualTo
                            return (safeInt(prop.getValue()) <= iCompareValue2);
                        case 2:
                        case -1:
                            // MRestriction.VariableEnum.EqualTo
                            return (safeInt(prop.getValue()) == iCompareValue2);
                        case 3:
                            // MRestriction.VariableEnum.GreaterThanOrEqualTo
                            return (safeInt(prop.getValue()) >= iCompareValue2);
                        case 4:
                            // MRestriction.VariableEnum.GreaterThan
                            return (safeInt(prop.getValue()) > iCompareValue2);
                    }
                    return false;
            }
        }
        return false;
    }

    private boolean passesDirectionRestriction(final @Nullable MReferenceList refs) {
        if (mKey1 == null) {
            return false;
        }
        String sRefDirection =
                refs != null ? refs.getReference("ReferencedDirection") : null;
        return mKey1.equals(sRefDirection);
    }

    public boolean passes(boolean bIgnoreReferences, final @Nullable MReferenceList refs) {
        try {
            // Make a working copy of this object as we will be
            // replacing references and we don't want those
            // changes to be permanent.
            MRestriction rest = copy();
            boolean r = false;

            // ---------------------------------------------------
            //              REPLACE REFERENCES
            //
            // If this restriction uses references, replace
            // those now with what the player actually typed.
            // For example, we might replace "ReferencedObject"
            // with "the ball". Also replace any references to
            // "%Player%" with the actual key of the Player
            // character.
            //
            // TCC: included check to ensure we don't replace
            // mKey1 if this is an "exact text" restriction, as
            // otherwise commands like "get all" will try to
            // take things out of containers, from other
            // characters, etc., as the relevant task TakeObjects
            // in the standard library doesn't work properly.
            // ---------------------------------------------------
            if (rest.mObjectType != BeExactText && isReference(rest.mKey1)) {
                rest.mKey1 = (refs != null) ?
                        refs.getReference(rest.mKey1) : null;
                if (bIgnoreReferences && rest.mKey1 == null) {
                    return true;
                }
            }
            if (isReference(rest.mKey2)) {
                rest.mKey2 = (refs != null) ?
                        refs.getReference(rest.mKey2) : null;
                if (bIgnoreReferences && rest.mKey2 == null) {
                    return true;
                }
            }
            if (rest.mKey1 != null && rest.mKey1.equals("%Player%")) {
                rest.mKey1 = mAdv.getPlayer().getKey();
            }
            if (rest.mKey2 != null && rest.mKey2.equals("%Player%")) {
                rest.mKey2 = mAdv.getPlayer().getKey();
            }

            // ---------------------------------------------------
            //             TEST THE CONDITIONS
            //
            // See if we the conditions of this restriction pass
            // with the given key(s).
            // ---------------------------------------------------
            switch (rest.mType) {
                case Location:
                    r = rest.passesLocationRestriction();
                    break;
                case Object:
                    r = rest.passesObjectRestriction(refs);
                    break;
                case Task:
                    r = rest.passesTaskRestriction();
                    break;
                case Variable:
                    r = rest.passesVariableRestriction(refs);
                    break;
                case Character:
                    r = rest.passesCharacterRestriction();
                    break;
                case Item:
                    r = rest.passesItemRestriction();
                    break;
                case Property:
                    r = rest.passesPropertyRestriction(bIgnoreReferences, refs);
                    break;
                case Direction:
                    r = rest.passesDirectionRestriction(refs);
                    break;
                case Expression:
                    r = safeBool(mAdv.evaluateStringExpression(rest.mStringValue, refs));
                    break;
            }

            if (r == (rest.mMust == Must)) {
                MParser.mRestrictionText = "";
                MView.debugPrint(MGlobals.ItemEnum.Task, "",
                        MView.DebugDetailLevelEnum.Medium, getSummary() + ": Passed");
                return true;
            } else {
                // Use original, so we mark any Display Once as seen
                MParser.mRestrictionText = mMessage.toString();
                if (rest.mType == RestrictionTypeEnum.Character &&
                        rest.mCharacterType == CharacterEnum.HaveRouteInDirection) {
                    if (!MParser.mRouteErrorText.equals(MParser.mRestrictionText) &&
                            !MParser.mRouteErrorText.equals("")) {
                        MParser.mRestrictionText = MParser.mRouteErrorText;
                    }
                }
                MView.debugPrint(MGlobals.ItemEnum.Task, "",
                        MView.DebugDetailLevelEnum.Medium, getSummary() + ": Failed");
                return false;
            }

        } catch (Exception ex) {
            MGlobals.errMsg("Error evaluating passes for restriction \"" + toString() + "\"", ex);
            //ex.printStackTrace();
            return false;
        }
    }

    @Override
    public String toString() {
        return getSummary();
    }

    public boolean referencesKey(@NonNull String sKey) {
        return mKey1.equals(sKey) || mKey2.equals(sKey);
    }

    @NonNull
    public String getSummary() {
        String sSummary = "Undefined Restriction";

        try {
            if (!mKey1.equals("") || mType == RestrictionTypeEnum.Expression) {
                switch (mType) {
                    case Location:
                        sSummary = mAdv.getNameFromKey(mKey1, true, true, true);
                        switch (mMust) {
                            case Must:
                                sSummary += " must ";
                                break;
                            case MustNot:
                                sSummary += " must not ";
                                break;
                        }
                        switch (mLocationType) {
                            case HaveBeenSeenByCharacter:
                                sSummary += "have been seen by " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInGroup:
                                sSummary += "be in " + mAdv.getNameFromKey(mKey2);
                                break;
                            case HaveProperty:
                                sSummary += "have " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeLocation:
                                sSummary += "be location " + mAdv.getNameFromKey(mKey2);
                                break;
                            case Exist:
                                sSummary += "exist";
                                break;
                        }
                        break;

                    case Object:
                        sSummary = mAdv.getNameFromKey(mKey1, true, true, true);
                        switch (mMust) {
                            case Must:
                                sSummary += " must ";
                                break;
                            case MustNot:
                                sSummary += " must not ";
                                break;
                        }
                        switch (mObjectType) {
                            case BeAtLocation:
                                sSummary += "be at " + mAdv.getNameFromKey(mKey2);
                                sSummary = sSummary.replace("at " + HIDDEN, HIDDEN);
                                break;
                            case BeHeldByCharacter:
                                sSummary += "be held by " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeHeldDirectlyByCharacter:
                                sSummary += "be held directly by " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeHidden:
                                sSummary += "be hidden";
                                break;
                            case BeInGroup:
                                sSummary += "be in object " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInsideObject:
                                sSummary += ("be inside " + mAdv.getNameFromKey(mKey2));
                                break;
                            case BeInState:
                                sSummary += "be in state '" + mKey2 + "'"; //mStringValue;
                                break;
                            case BeOnObject:
                                sSummary += ("be on " + mAdv.getNameFromKey(mKey2));
                                break;
                            case BePartOfCharacter:
                                sSummary += "be part of " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BePartOfObject:
                                sSummary += ("be part of " + mAdv.getNameFromKey(mKey2));
                                break;
                            case HaveBeenSeenByCharacter:
                                sSummary += "have been seen by " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeVisibleToCharacter:
                                sSummary += "be visible to " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeWornByCharacter:
                                sSummary += "be worn by " + mAdv.getNameFromKey(mKey2);
                                break;
                            case Exist:
                                sSummary += "exist";
                                break;
                            case HaveProperty:
                                sSummary += "have " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeExactText:
                                sSummary += "be exact text '" + mKey2 + "'";
                                break;
                            case BeObject:
                                sSummary += "be " + mAdv.getNameFromKey(mKey2);
                                break;
                        }
                        break;

                    case Task:
                        sSummary = mAdv.getNameFromKey(mKey1, true, true, true);
                        switch (mMust) {
                            case Must:
                                sSummary += " must be complete";
                                break;
                            case MustNot:
                                sSummary += " must not be complete";
                                break;
                        }
                        break;

                    case Character:
                        sSummary = mAdv.getNameFromKey(mKey1, true, true, true);
                        switch (mMust) {
                            case Must:
                                sSummary += " must ";
                                break;
                            case MustNot:
                                sSummary += " must not ";
                                break;
                        }
                        switch (mCharacterType) {
                            case BeAlone:
                                sSummary += "be alone";
                                break;
                            case BeAloneWith:
                                sSummary += "be alone with " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeAtLocation:
                                sSummary += "be at " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeCharacter:
                                sSummary += "be " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInConversationWith:
                                sSummary += "be in conversation with " + mAdv.getNameFromKey(mKey2);
                                break;
                            case Exist:
                                sSummary += "exist";
                                break;
                            case HaveRouteInDirection:
                                sSummary += "have a route available ";
                                if (mKey2.equals(ANYDIRECTION)) {
                                    sSummary += "in any direction";
                                } else if (mKey2.startsWith("ReferencedDirection")) {
                                    sSummary += "to the " +
                                            mKey2.replace("ReferencedDirection",
                                                    "Referenced Direction");
                                } else {
                                    switch (MAdventure.DirectionsEnum.valueOf(mKey2)) {
                                        case North:
                                        case NorthEast:
                                        case East:
                                        case SouthEast:
                                        case South:
                                        case SouthWest:
                                        case West:
                                        case NorthWest:
                                            sSummary += "to the " +
                                                    mAdv.getDirectionName(MAdventure.DirectionsEnum.valueOf(mKey2));
                                            break;
                                        case Up:
                                        case Down:
                                        case In:
                                        case Out:
                                            sSummary +=
                                                    mAdv.getDirectionName(MAdventure.DirectionsEnum.valueOf(mKey2));
                                            break;
                                    }
                                }
                                break;
                            case HaveProperty:
                                sSummary += "have " + mAdv.getNameFromKey(mKey2);
                                break;
                            case HaveSeenCharacter:
                                sSummary += "have seen " + mAdv.getNameFromKey(mKey2);
                                break;
                            case HaveSeenLocation:
                                sSummary += "have seen " + mAdv.getNameFromKey(mKey2);
                                break;
                            case HaveSeenObject:
                                sSummary += "have seen " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeHoldingObject:
                                sSummary += "be holding " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInSameLocationAsCharacter:
                                sSummary += "be in the same location as " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInSameLocationAsObject:
                                if (mAdv.getTypeFromKeyNice(mKey2).equals("Object")) {
                                    sSummary += "be in the same location as " + mAdv.getNameFromKey(mKey2);
                                } else {
                                    sSummary += "be in the same location as any object in " + mAdv.getNameFromKey(mKey2);
                                }
                                break;
                            case BeLyingOnObject:
                                sSummary += "be lying on " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInGroup:
                                sSummary += "be a member of " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeOfGender:
                                sSummary += "be of gender " + mKey2;
                                break;
                            case BeSittingOnObject:
                                sSummary += "be sitting on " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeStandingOnObject:
                                sSummary += "be standing on " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeWearingObject:
                                sSummary += "be wearing " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeWithinLocationGroup:
                                sSummary += "be at a location within " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInPosition:
                                sSummary += "be in position " + mKey2;
                                break;
                            case BeInsideObject:
                                sSummary += "be inside " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeOnObject:
                                sSummary += "be on " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeOnCharacter:
                                sSummary += "be on " + mAdv.getNameFromKey(mKey2);
                                break;
                        }
                        break;

                    case Item:
                        sSummary = mAdv.getNameFromKey(mKey1, true, true, true);
                        switch (mMust) {
                            case Must:
                                sSummary += " must ";
                                break;
                            case MustNot:
                                sSummary += " must not ";
                                break;
                        }
                        switch (mItemType) {
                            case BeAtLocation:
                                sSummary += "be at " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeCharacter:
                                sSummary += "be " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInGroup:
                                sSummary += "be a member of " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInSameLocationAsCharacter:
                                sSummary += "be in the same loc as " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInSameLocationAsObject:
                                sSummary += "be in the same loc as " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeInsideObject:
                                sSummary += "be inside " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeLocation:
                                sSummary += "be " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeObject:
                                sSummary += "be " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeOnCharacter:
                                sSummary += "be on " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeOnObject:
                                sSummary += "be on " + mAdv.getNameFromKey(mKey2);
                                break;
                            case BeType:
                                sSummary += "be item type " + mKey2;
                                break;
                            case Exist:
                                sSummary += "exist";
                                break;
                            case HaveProperty:
                                sSummary += "have " + mAdv.getNameFromKey(mKey2);
                                break;
                        }
                        break;

                    case Variable:
                        MVariable.VariableType eVarType;

                        switch (mKey1) {
                            case "ReferencedNumber":
                                sSummary = mAdv.getNameFromKey(mKey1) + " ";
                                eVarType = NUMERIC;
                                break;
                            case "ReferencedText":
                                sSummary = mAdv.getNameFromKey(mKey1) + " ";
                                eVarType = TEXT;
                                break;
                            default:
                                MVariable var = mAdv.mVariables.get(mKey1);
                                eVarType = var.getType();
                                sSummary = "Variable '" + var.getName();
                                if (!mKey2.equals("")) {
                                    if (mAdv.mVariables.containsKey(mKey2)) {
                                        sSummary += "[%" + mAdv.mVariables.get(mKey2).getName() + "%]";
                                    } else {
                                        sSummary += "[" + mKey2 + "]";
                                    }
                                }
                                sSummary += "' ";
                                break;
                        }

                        switch (mMust) {
                            case Must:
                                sSummary += "must ";
                                break;
                            case MustNot:
                                sSummary += "must not ";
                                break;
                        }
                        if (eVarType == NUMERIC) {
                            sSummary += "be ";
                            switch (mVariableType) {
                                case EqualTo:
                                    sSummary += "equal to ";
                                    break;
                                case GreaterThan:
                                    sSummary += "greater than ";
                                    break;
                                case GreaterThanOrEqualTo:
                                    sSummary += "greater than or equal to ";
                                    break;
                                case LessThan:
                                    sSummary += "less than ";
                                    break;
                                case LessThanOrEqualTo:
                                    sSummary += "less than or equal to ";
                                    break;
                            }
                        } else {
                            switch (mVariableType) {
                                case EqualTo:
                                    sSummary += "be equal to ";
                                    break;
                                case Contain:
                                    sSummary += "contain ";
                                    break;
                            }
                        }

                        if (mIntValue == Integer.MIN_VALUE) {
                            sSummary += mAdv.getNameFromKey(mStringValue);
                        } else {
                            if (eVarType == NUMERIC) {
                                if (!mStringValue.equals("") && !mStringValue.equals(String.valueOf(mIntValue))) {
                                    sSummary += mStringValue; // Must be an expression resulting in an integer
                                } else {
                                    sSummary += mIntValue;
                                }
                            } else {
                                sSummary += "'" + mStringValue + "'";
                            }
                        }
                        break;

                    case Property:
                        sSummary = mAdv.getNameFromKey(mKey1, true,
                                true, true) + " for " + mAdv.getNameFromKey(mKey2);
                        switch (mMust) {
                            case Must:
                                sSummary += " must be ";
                                break;
                            case MustNot:
                                sSummary += " must not be ";
                                break;
                        }
                        switch (mAdv.mAllProperties.get(mKey1).getType()) {
                            case CharacterKey:
                            case LocationGroupKey:
                            case LocationKey:
                            case ObjectKey:
                                sSummary += "'" + mAdv.getNameFromKey(mStringValue) + "'";
                                break;
                            case SelectionOnly:
                                break;
                            case Integer:
                                switch (mIntValue) {
                                    case 0:
                                        // VariableEnum.LessThan
                                        sSummary += "< ";
                                        break;
                                    case 1:
                                        // VariableEnum.LessThanOrEqualTo
                                        sSummary += "<= ";
                                        break;
                                    case 2:
                                        // VariableEnum.EqualTo
                                        sSummary += "= ";
                                        break;
                                    case 3:
                                        // VariableEnum.GreaterThanOrEqualTo
                                        sSummary += ">= ";
                                        break;
                                    case 4:
                                        // VariableEnum.GreaterThan
                                        sSummary += "> ";
                                        break;
                                }
                                sSummary += "'" + mStringValue + "'";
                                break;
                            case StateList:
                            case Text:
                                sSummary += "'" + mStringValue + "'";
                                break;
                        }
                        break;

                    case Direction:
                        sSummary = "Referenced Direction ";
                        switch (mMust) {
                            case Must:
                                sSummary += "must be ";
                                break;
                            case MustNot:
                                sSummary += "must not be ";
                                break;
                        }
                        sSummary += mAdv.getDirectionName(MAdventure.DirectionsEnum.valueOf(mKey1));
                        break;

                    case Expression:
                        sSummary = mStringValue;
                        break;
                }
            }
        } catch (Exception ex) {
            sSummary = "Bad Restriction definition: " + ex.getMessage();
        }

        return sSummary;
    }

    @NonNull
    public MRestriction copy() {
        MRestriction rest = new MRestriction(mAdv);
        rest.mCharacterType = this.mCharacterType;
        rest.mType = this.mType;
        rest.mLocationType = this.mLocationType;
        rest.mMust = this.mMust;
        rest.mObjectType = this.mObjectType;
        rest.mTaskType = this.mTaskType;
        rest.mVariableType = this.mVariableType;
        rest.mKey1 = this.mKey1;
        rest.mKey2 = this.mKey2;
        rest.mIntValue = this.mIntValue;
        rest.mStringValue = this.mStringValue;
        rest.mMessage = this.mMessage.copy();
        return rest;
    }

    public enum RestrictionTypeEnum {
        Location,       // 0
        Object,         // 1
        Task,           // 2
        Character,      // 3
        Variable,       // 4
        Property,       // 5
        Direction,      // 6
        Expression,     // 7
        Item            // 8
    }

    public enum MustEnum {
        Must,           // 0
        MustNot         // 1
    }

    public enum LocationEnum {
        HaveBeenSeenByCharacter,    // 0
        BeInGroup,                  // 1
        HaveProperty,               // 2
        BeLocation,                 // 3
        Exist                       // 4
    }

    public enum ObjectEnum {
        BeAtLocation,               // 0
        BeHeldByCharacter,          // 1
        BeWornByCharacter,          // 2
        BeVisibleToCharacter,       // 3
        BeInsideObject,             // 4
        BeOnObject,                 // 5
        BeInState,                  // 6
        BeInGroup,                  // 7
        HaveBeenSeenByCharacter,    // 8
        BePartOfObject,             // 9
        BePartOfCharacter,          // 10
        Exist,                      // 11
        HaveProperty,               // 12
        BeExactText,                // 13
        BeHidden,                   // 14
        BeObject,                   // 15
        BeHeldDirectlyByCharacter   // 99 (Bebek-specific addition)
    }

    public enum TaskEnum {
        Complete                    // 0
    }

    public enum CharacterEnum {
        BeAlone,                        // 0
        BeAloneWith,                    // 1
        BeAtLocation,                   // 2
        BeCharacter,                    // 3
        BeHoldingObject,                // 4
        BeInConversationWith,           // 5
        BeInPosition,                   // 6
        BeInSameLocationAsCharacter,    // 7
        BeInSameLocationAsObject,       // 8
        BeInsideObject,                 // 9
        BeLyingOnObject,                // 10
        BeInGroup,                      // 11
        BeOfGender,                     // 12
        BeOnObject,                     // 13
        BeOnCharacter,                  // 14
        BeStandingOnObject,             // 15
        BeSittingOnObject,              // 16
        BeWearingObject,                // 17
        BeWithinLocationGroup,          // 18
        Exist,                          // 19
        HaveProperty,                   // 20
        HaveRouteInDirection,           // 21
        HaveSeenLocation,               // 22
        HaveSeenObject,                 // 23
        HaveSeenCharacter,              // 24
        BeVisibleToCharacter            // 25
    }

    // Can only be things common to all items
    public enum ItemEnum {
        BeAtLocation,                   // 0
        BeCharacter,                    // 1
        BeInSameLocationAsCharacter,    // 2
        BeInSameLocationAsObject,       // 3
        BeInsideObject,                 // 4
        BeLocation,                     // 5
        BeInGroup,                      // 6
        BeObject,                       // 7
        BeOnCharacter,                  // 8
        BeOnObject,                     // 9
        BeType,                         // 10
        Exist,                          // 11
        HaveProperty                    // 12
    }

    public enum VariableEnum {
        LessThan,                   // 0
        LessThanOrEqualTo,          // 1
        EqualTo,                    // 2
        GreaterThanOrEqualTo,       // 3
        GreaterThan,                // 4
        Contain                     // 5
    }
}
