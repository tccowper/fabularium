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

import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MLocationHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;
import java.util.EnumSet;
import java.util.HashMap;

import static com.luxlunae.bebek.MGlobals.ItemEnum.Task;
import static com.luxlunae.bebek.MGlobals.THEFLOOR;
import static com.luxlunae.bebek.MGlobals.THEPLAYER;
import static com.luxlunae.bebek.MGlobals.instr;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.model.MAction.EndGameEnum.Lose;
import static com.luxlunae.bebek.model.MAction.EndGameEnum.Neutral;
import static com.luxlunae.bebek.model.MAction.ItemEnum.AddCharacterToGroup;
import static com.luxlunae.bebek.model.MAction.ItemEnum.AddLocationToGroup;
import static com.luxlunae.bebek.model.MAction.ItemEnum.AddObjectToGroup;
import static com.luxlunae.bebek.model.MAction.ItemEnum.Conversation;
import static com.luxlunae.bebek.model.MAction.ItemEnum.DecreaseVariable;
import static com.luxlunae.bebek.model.MAction.ItemEnum.EndGame;
import static com.luxlunae.bebek.model.MAction.ItemEnum.IncreaseVariable;
import static com.luxlunae.bebek.model.MAction.ItemEnum.MoveCharacter;
import static com.luxlunae.bebek.model.MAction.ItemEnum.MoveObject;
import static com.luxlunae.bebek.model.MAction.ItemEnum.RemoveCharacterFromGroup;
import static com.luxlunae.bebek.model.MAction.ItemEnum.RemoveLocationFromGroup;
import static com.luxlunae.bebek.model.MAction.ItemEnum.RemoveObjectFromGroup;
import static com.luxlunae.bebek.model.MAction.ItemEnum.SetProperties;
import static com.luxlunae.bebek.model.MAction.ItemEnum.SetTasks;
import static com.luxlunae.bebek.model.MAction.ItemEnum.SetVariable;
import static com.luxlunae.bebek.model.MAction.ItemEnum.Time;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.FromGroup;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.InsideObject;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.OntoObject;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.ToCarriedBy;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.ToGroup;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.ToLocation;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.ToLocationGroup;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.ToSameLocationAs;
import static com.luxlunae.bebek.model.MAction.MoveObjectToEnum.ToWornBy;
import static com.luxlunae.bebek.model.MAction.MoveObjectWhatEnum.EverythingHeldBy;
import static com.luxlunae.bebek.model.MAction.MoveObjectWhatEnum.EverythingWornBy;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Ask;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Command;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.EnterConversation;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Farewell;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Greet;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.LeaveConversation;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Tell;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TurnBased;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.ValueList;
import static com.luxlunae.bebek.model.MTask.SetTasksEnum.Execute;
import static com.luxlunae.bebek.model.MTask.SetTasksEnum.Unset;
import static com.luxlunae.bebek.model.MVariable.OpType.ASSIGNMENT;
import static com.luxlunae.bebek.model.MVariable.OpType.LOOP;
import static com.luxlunae.bebek.model.MVariable.VariableType.Numeric;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Container;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Dynamic;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Lieable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Sittable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Standable;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Surface;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.WithStateOrOpenable;
import static com.luxlunae.bebek.model.io.MFileOlder.getObjectKey;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.High;

/**
 * The "Actions" tab of a task or character conversation topic contains a list of
 * actions which are performed in order when a task is executed and passes all
 * of its restrictions.
 */
public class MAction {
    @NonNull
    private final MAdventure mAdv;

    public ItemEnum mType = MoveObject;
    @Nullable
    public String mKey1;
    @Nullable
    public String mKey2;
    @NonNull
    public String mStrValue = "";
    MTask.SetTasksEnum mSetTask = Execute;
    @NonNull
    private String mPropValue = "";
    private int mIntValue;
    private MoveObjectWhatEnum mMoveObjectWhat = MoveObjectWhatEnum.Object;
    private MoveObjectToEnum mMoveObjectTo = ToLocation;
    private MCharacterHashMap.MoveCharacterWhoEnum mMoveCharacterWho =
            MCharacterHashMap.MoveCharacterWhoEnum.Character;
    private MoveCharacterToEnum mMoveCharacterTo = MoveCharacterToEnum.InDirection;
    private MLocationHashMap.MoveLocationWhatEnum mMoveLocationWhat =
            MLocationHashMap.MoveLocationWhatEnum.Location;
    private MoveLocationToEnum mMoveLocationTo = MoveLocationToEnum.ToGroup;
    private ObjectStatusEnum mObjectStatus = ObjectStatusEnum.DunnoYet;
    private MVariable.OpType mVariableType = ASSIGNMENT;
    private EndGameEnum mEndGame = EndGameEnum.Win;
    private MCharacter.ConversationEnum mConversation = MCharacter.ConversationEnum.NotSet;

    public MAction(@NonNull MAdventure adv) {
        mAdv = adv;
    }

    public MAction(@NonNull MAdventure adv, @NonNull ItemEnum type,
                   @NonNull String key1, @NonNull String key2, @NonNull String propVal) {
        // ADRIFT V3.90 and V4 Loader
        this(adv);

        mType = type;
        mKey1 = key1;
        mKey2 = key2;
        mPropValue = propVal;
    }

    public MAction(@NonNull MAdventure adv, int obKey, int moveTo) {
        // ADRIFT V3.90 and V4 Loader
        // specific constructor for an action to move objects
        this(adv);

        mType = MoveObject;
        mKey1 = "Object" + obKey;
        switch (moveTo) {
            case 0:
                // Hidden
                mMoveObjectTo = ToLocation;
                mKey2 = "Hidden";
                break;
            case 1:
                // Player's hands
                MObject ob = adv.mObjects.get(mKey1);
                if (ob == null || ob.isStatic()) {
                    //  Don't allow for static
                    mMoveObjectTo = ToLocation;
                    mKey2 = "Hidden";
                } else {
                    mMoveObjectTo = ToCarriedBy;
                    mKey2 = "%Player%";
                }
                break;
            case 2:
                // Same room as player
                mMoveObjectTo = ToSameLocationAs;
                mKey2 = "%Player%";
                break;
            default:
                // Locations
                mMoveObjectTo = ToLocation;
                mKey2 = "Location" + (moveTo - 2);
                break;
        }
    }

    public MAction(@NonNull MAdventure adv,
                   int startLoc, int startChar, int startTask,
                   final HashMap<MObject, MProperty> dodgyArlStates,
                   int type, int var1, int var2, int var3, int var5,
                   String expr) {
        // ADRIFT V3.80 loader
        this(adv);

        initOlder(adv, startLoc, startChar, startTask, dodgyArlStates,
                type, var1, var2, var3, var5, expr);
    }

    public MAction(@NonNull MAdventure adv, @NonNull MFileOlder.V4Reader reader,
                   int startLoc, int startChar, int startTask,
                   final HashMap<MObject, MProperty> dodgyArlStates,
                   double version) throws EOFException {
        // ADRIFT V3.90 and V4 Loader
        this(adv);

        int type = cint(reader.readLine());                       // #Type
        int var1 = cint(reader.readLine());                       // #Var1
        int var2 = 0, var3 = 0, var5 = 0;
        String expr = "";
        if (version < 4) {
            if (type < 4 || type == 6) {
                var2 = cint(reader.readLine());                   // #Var2
            }
            if (type == 0 || type == 1 || type == 3 || type == 6) {
                var3 = cint(reader.readLine());
            }
            if (type > 4) {
                type++;
            }
            if (type == 1 && var2 == 2) {
                var3 += 2;
            }
            if (type == 7) {
                if (var1 >= 5 && var1 <= 6) {
                    var1 += 2;
                }
                if (var1 == 7) {
                    var1 = 11;
                }
            }
        } else {
            if (type < 4 || type == 5 || type == 6 || type == 7) {
                var2 = cint(reader.readLine());                   // #Var2
            }
            if (type == 0 || type == 1 || type == 3 || type == 6 || type == 7) {
                var3 = cint(reader.readLine());                   // #Var3
            }
        }
        if (type == 3) {
            // ?#Type=3:#Var1,#Var2,#Var3,$Expr,#Var5
            if (version < 4) {
                if (var2 == 5) {
                    expr = reader.readLine();                     // sExpr
                } else {
                    var5 = cint(reader.readLine());               // #Var5
                }
            } else {
                expr = reader.readLine();                         // sExpr
                var5 = cint(reader.readLine());                   // #Var5
            }
        }

        initOlder(adv, startLoc, startChar, startTask, dodgyArlStates,
                type, var1, var2, var3, var5, expr);
    }

    public MAction(@NonNull MAdventure adv, @NonNull String type,
                   @NonNull String action, double version) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        // TCC - not in orig code but seems some games like
        // Axe of Kolt have VB escaped quotes embedded in their XML source:
        action = action.replace("\"\"", "\"");
        final String actWords[] = action.split(" ");
        final int nActWords = actWords.length;
        final StringBuilder sb = new StringBuilder();

        switch (type) {
            case "EndGame": {
                mType = EndGame;
                mEndGame = MAction.EndGameEnum.valueOf(actWords[0]);
                break;
            }
            case "MoveObject":
            case "AddObjectToGroup":
            case "RemoveObjectFromGroup": {
                switch (type) {
                    case "MoveObject":
                        mType = MoveObject;
                        break;
                    case "AddObjectToGroup":
                        mType = AddObjectToGroup;
                        break;
                    case "RemoveObjectFromGroup":
                        mType = RemoveObjectFromGroup;
                        break;
                }
                if (version <= 5.000016) {
                    // Upgrade previous file format
                    mKey1 = actWords[0];
                    mMoveObjectWhat = MAction.MoveObjectWhatEnum.Object;
                    switch (mKey1) {
                        case "AllHeldObjects":
                            mMoveObjectWhat = EverythingHeldBy;
                            mKey1 = THEPLAYER;
                            break;
                        case "AllWornObjects":
                            mMoveObjectWhat = EverythingWornBy;
                            mKey1 = THEPLAYER;
                            break;
                        default:
                            // Leave as is
                            break;
                    }
                    mMoveObjectTo = MAction.MoveObjectToEnum.valueOf(actWords[1]);
                    mKey2 = actWords[2];
                } else {
                    mMoveObjectWhat = MAction.MoveObjectWhatEnum.valueOf(actWords[0]);
                    mKey1 = actWords[1];
                    if (nActWords > 4) {
                        for (int i = 2; i < nActWords - 2; i++) {
                            sb.append(actWords[i]);
                            if (i < nActWords - 3) {
                                sb.append(" ");
                            }
                        }
                        mPropValue = sb.toString();
                    }
                    switch (mType) {
                        case AddObjectToGroup:
                            mMoveObjectTo = ToGroup;
                            break;
                        case RemoveObjectFromGroup:
                            mMoveObjectTo = FromGroup;
                            break;
                        case MoveObject:
                            mMoveObjectTo =
                                    MAction.MoveObjectToEnum.valueOf(actWords[nActWords - 2]);
                            break;
                    }
                    mKey2 = actWords[nActWords - 1];
                }
                break;
            }
            case "MoveCharacter":
            case "AddCharacterToGroup":
            case "RemoveCharacterFromGroup": {
                switch (type) {
                    case "MoveCharacter":
                        mType = MoveCharacter;
                        break;
                    case "AddCharacterToGroup":
                        mType = AddCharacterToGroup;
                        break;
                    case "RemoveCharacterFromGroup":
                        mType = RemoveCharacterFromGroup;
                        break;
                }
                if (version <= 5.000016) {
                    // Upgrade previous file format
                    mType = MoveCharacter;
                    mKey1 = actWords[0];
                    mMoveCharacterTo = MAction.MoveCharacterToEnum.valueOf(actWords[1]);
                    mKey2 = actWords[2];
                    if (mMoveCharacterTo == MAction.MoveCharacterToEnum.InDirection &&
                            isNumeric(mKey2)) {
                        mKey2 = MAdventure.DirectionsEnum.valueOf(mKey2).toString();
                    }
                } else {
                    mMoveCharacterWho =
                            MCharacterHashMap.MoveCharacterWhoEnum.valueOf(actWords[0]);
                    mKey1 = actWords[1];
                    if (nActWords > 4) {
                        for (int i = 2; i < nActWords - 2; i++) {
                            sb.append(actWords[i]);
                            if (i < nActWords - 3) {
                                sb.append(" ");
                            }
                        }
                        mPropValue = sb.toString();
                    }
                    switch (mType) {
                        case AddCharacterToGroup:
                            mMoveCharacterTo = MAction.MoveCharacterToEnum.ToGroup;
                            break;
                        case RemoveCharacterFromGroup:
                            mMoveCharacterTo = MAction.MoveCharacterToEnum.FromGroup;
                            break;
                        case MoveCharacter:
                            try {
                                mMoveCharacterTo =
                                        MAction.MoveCharacterToEnum.valueOf(actWords[nActWords - 2]);
                            } catch (IllegalArgumentException e) {
                                // for %Player% the correct index seems to be length - 1
                                mMoveCharacterTo =
                                        MAction.MoveCharacterToEnum.valueOf(actWords[nActWords - 1]);
                            }
                            break;
                    }
                    mKey2 = actWords[nActWords - 1];
                }
                break;
            }
            case "AddLocationToGroup":
            case "RemoveLocationFromGroup": {
                switch (type) {
                    case "AddLocationToGroup":
                        mType = AddLocationToGroup;
                        break;
                    case "RemoveLocationFromGroup":
                        mType = RemoveLocationFromGroup;
                }
                mMoveLocationWhat =
                        MLocationHashMap.MoveLocationWhatEnum.valueOf(actWords[0]);
                mKey1 = actWords[1];
                if (nActWords > 4) {
                    for (int i = 2; i < nActWords - 2; i++) {
                        sb.append(actWords[i]);
                        if (i < nActWords - 3) {
                            sb.append(" ");
                        }
                    }
                    mPropValue = sb.toString();
                }
                switch (mType) {
                    case AddLocationToGroup:
                        mMoveLocationTo = MAction.MoveLocationToEnum.ToGroup;
                        break;
                    case RemoveLocationFromGroup:
                        mMoveLocationTo = MAction.MoveLocationToEnum.FromGroup;
                        break;
                }
                mKey2 = actWords[nActWords - 1];
                break;
            }
            case "SetProperty": {
                mType = SetProperties;
                mKey1 = actWords[0];
                mKey2 = actWords[1];
                for (int i = 2; i < nActWords; i++) {
                    sb.append(actWords[i]);
                    if (i < nActWords - 1) {
                        sb.append(" ");
                    }
                }
                mStrValue = mPropValue = sb.toString();
                break;
            }
            case "Score": {
                break;
            }
            case "SetTasks": {
                mType = SetTasks;
                int start = 0;
                if (actWords[0].equals("FOR")) {
                    mIntValue = cint(actWords[3]);
                    mPropValue = actWords[5];
                    start = 7;
                }
                mSetTask = MTask.SetTasksEnum.valueOf(actWords[start]);
                mKey1 = actWords[start + 1];
                for (int i = start + 2; i < nActWords; i++) {
                    sb.append(actWords[i]);
                }
                if (sb.length() > 0) {
                    if (sb.charAt(0) == '(') {
                        sb.deleteCharAt(0);
                    }
                    int len = sb.length() - 1;
                    if (sb.charAt(len) == ')') {
                        sb.deleteCharAt(len);
                    }
                }
                mStrValue = sb.toString();
                break;
            }
            case "SetVariable":
            case "IncVariable":
            case "DecVariable":
            case "ExecuteTask": {
                switch (type) {
                    case "SetVariable":
                        mType = SetVariable;
                        break;
                    case "IncVariable":
                        mType = IncreaseVariable;
                        break;
                    case "DecVariable":
                        mType = DecreaseVariable;
                        break;
                }
                if (actWords[0].equals("FOR")) {
                    mVariableType = LOOP;
                    mIntValue = cint(actWords[3]);
                    mKey2 = actWords[5];
                    mKey1 = actWords[8].split("\\[")[0];
                    for (int i = 10; i < nActWords - 3; i++) {
                        sb.append(actWords[i]);
                        if (i < nActWords - 4) {
                            sb.append(" ");
                        }
                    }
                } else {
                    mVariableType = ASSIGNMENT;
                    if (actWords[0].contains("[")) {
                        String[] sKeys = actWords[0].split("\\[");
                        mKey1 = sKeys[0];
                        mKey2 = sKeys[1].replace("]", "");
                    } else {
                        mKey1 = actWords[0];
                    }
                    for (int i = 2; i < nActWords; i++) {
                        sb.append(actWords[i]);
                        if (i < nActWords - 1) {
                            sb.append(" ");
                        }
                    }
                    if (version > 5.0000321) {
                        int last = sb.length() - 1;
                        if (sb.charAt(0) == '"' && sb.charAt(last) == '"') {
                            sb.deleteCharAt(0);
                            sb.deleteCharAt(last - 1);
                        }
                    }
                }
                mStrValue = sb.toString();
                break;
            }
            case "Time": {
                mType = Time;
                for (int i = 1; i < nActWords - 1; i++) {
                    if (i > 1) {
                        sb.append(" ");
                    }
                    sb.append(actWords[i]);
                }
                sb.deleteCharAt(0);
                sb.deleteCharAt(sb.length() - 1);
                mStrValue = sb.toString();
                break;
            }
            case "Conversation": {
                mType = Conversation;
                final String convType = actWords[0].toUpperCase();
                switch (convType) {
                    case "GREET":
                    case "FAREWELL": {
                        mConversation = convType.equals("GREET") ? Greet : Farewell;
                        mKey1 = actWords[1];
                        if (nActWords > 2) {
                            for (int i = 3; i < nActWords; i++) {
                                sb.append(actWords[i]);
                                if (i < nActWords - 1) {
                                    sb.append(" ");
                                }
                            }
                            if (sb.charAt(0) == '\'') {
                                sb.deleteCharAt(0);
                            }
                            int last = sb.length() - 1;
                            if (sb.charAt(last) == '\'') {
                                sb.deleteCharAt(last);
                            }
                            mStrValue = sb.toString();
                        }
                        break;
                    }
                    case "ASK":
                    case "TELL": {
                        mConversation = convType.equals("ASK") ? Ask : Tell;
                        mKey1 = actWords[1];
                        for (int i = 3; i < nActWords; i++) {
                            sb.append(actWords[i]);
                            if (i < nActWords - 1) {
                                sb.append(" ");
                            }
                        }
                        if (sb.charAt(0) == '\'') {
                            sb.deleteCharAt(0);
                        }
                        int last = sb.length() - 1;
                        if (sb.charAt(last) == '\'') {
                            sb.deleteCharAt(last);
                        }
                        mStrValue = sb.toString();
                        break;
                    }
                    case "SAY": {
                        mConversation = Command;
                        for (int i = 1; i < nActWords - 2; i++) {
                            sb.append(actWords[i]);
                            if (i < nActWords - 3) {
                                sb.append(" ");
                            }
                        }
                        if (sb.charAt(0) == '\'') {
                            sb.deleteCharAt(0);
                        }
                        int last = sb.length() - 1;
                        if (sb.charAt(last) == '\'') {
                            sb.deleteCharAt(last);
                        }
                        mStrValue = sb.toString();
                        mKey1 = actWords[nActWords - 1];
                        break;
                    }
                    case "ENTERWITH":
                    case "LEAVEWITH": {
                        mConversation = convType.equals("ENTERWITH") ?
                                EnterConversation : LeaveConversation;
                        mKey1 = actWords[1];
                        break;
                    }
                }
                break;
            }
            default:
                // unrecognised
                throw new Exception("Unrecognised action type");
        }
    }

    @NonNull
    private static String getRefKey(@NonNull String cmd, int refNum) {
        // Return the token that the reference we're looking at is
        String[] tokens = cmd.split(" ");
        int i = 0;
        for (String token : tokens) {
            switch (token) {
                case "%character1%":
                case "%character2%":
                case "%character3%":
                case "%character4%":
                case "%character5%":
                case "%characters%":
                case "%direction%":
                case "%number%":
                case "%numbers%":
                case "%object1%":
                case "%object2%":
                case "%object3%":
                case "%object4%":
                case "%object5%":
                case "%objects%":
                case "%text%":
                    i++;
                    break;
            }
            if (i == refNum) {
                return token.replace("%object", "ReferencedObject")
                        .replace("%direction", "ReferencedDirection")
                        .replace("%", "");
            }
        }
        return "";
    }

    private void time(@NonNull String expr) throws InterruptedException {
        // This isn't perfect - not sure what'll happen with any fail messages
        for (String msg : mAdv.mPassResponses.mOrderedKeys) {
            mAdv.mReferences = mAdv.mPassResponses.get(msg);
            mAdv.mView.displayText(mAdv, msg);
        }
        mAdv.mPassResponses.clear();
        for (int i = 0; i < mAdv.evalIntExpr(expr, mAdv.mReferences); i++) {
            mAdv.incrementTurnOrTime(TurnBased);
        }
        mAdv.mPassResponses.clear();
    }

    private void replaceRefs(@Nullable MTask task, @NonNull String taskCmd,
                             @NonNull EnumSet<MTask.ExecutionStatus> curStatus) {
        MReferenceList refs = mAdv.mReferences;
        if (refs == null) {
            return;
        }

        if (mKey1 != null) {
            switch (mKey1) {
                case "ReferencedObjects":
                    for (int iRef = 0; iRef < refs.size(); iRef++) {
                        MReference ref = refs.get(iRef);
                        int nObs = ref.mItems.size();
                        if (nObs > 0 &&
                                ref.mType == MReference.ReferencesType.Object &&
                                getRefKey(taskCmd, iRef + 1).equals("ReferencedObjects")) {
                            for (int iOb = 0; iOb < nObs; iOb++) {
                                MStringArrayList poss = ref.mItems.get(iOb).mMatchingKeys;
                                if (poss.size() > 0) {
                                    mKey1 = poss.get(0);
                                    execute(taskCmd, task, false, curStatus);
                                }
                            }
                        }
                    }
                    return;

                case "ReferencedObject":
                case "ReferencedObject1":
                case "ReferencedObject2":
                case "ReferencedObject3":
                case "ReferencedObject4":
                case "ReferencedObject5":
                case "ReferencedDirection":
                case "ReferencedDirection1":
                case "ReferencedDirection2":
                case "ReferencedDirection3":
                case "ReferencedDirection4":
                case "ReferencedDirection5":
                case "ReferencedCharacter":
                case "ReferencedCharacter1":
                case "ReferencedCharacter2":
                case "ReferencedCharacter3":
                case "ReferencedCharacter4":
                case "ReferencedCharacter5":
                case "ReferencedLocation":
                case "ReferencedLocation1":
                case "ReferencedLocation2":
                case "ReferencedLocation3":
                case "ReferencedLocation4":
                case "ReferencedLocation5":
                case "ReferencedItem":
                case "ReferencedItem1":
                case "ReferencedItem2":
                case "ReferencedItem3":
                case "ReferencedItem4":
                case "ReferencedItem5":
                case "ReferencedNumber":
                case "ReferencedNumber1":
                case "ReferencedNumber2":
                case "ReferencedNumber3":
                case "ReferencedNumber4":
                case "ReferencedNumber5":
                    mKey1 = refs.getMatchingKey(mKey1);
                    break;

                case "%Player%":
                    mKey1 = mAdv.getPlayer().getKey();
                    break;
            }
        }

        if (mKey2 != null) {
            switch (mKey2) {
                case "ReferencedObjects":
                    for (int iRef = 0; iRef < refs.size(); iRef++) {
                        MReference ref = refs.get(iRef);
                        int nObs = ref.mItems.size();
                        if (nObs > 0 &&
                                ref.mType == MReference.ReferencesType.Object &&
                                getRefKey(taskCmd, iRef).equals("ReferencedObjects")) {
                            for (int iOb = 0; iOb < nObs; iOb++) {
                                MStringArrayList poss = ref.mItems.get(iOb).mMatchingKeys;
                                if (poss.size() > 0) {
                                    mKey2 = poss.get(0);
                                    execute(taskCmd, task, false, curStatus);
                                }
                            }
                        }
                    }
                    return;

                case "ReferencedObject":
                case "ReferencedObject1":
                case "ReferencedObject2":
                case "ReferencedObject3":
                case "ReferencedObject4":
                case "ReferencedObject5":
                case "ReferencedDirection":
                case "ReferencedDirection1":
                case "ReferencedDirection2":
                case "ReferencedDirection3":
                case "ReferencedDirection4":
                case "ReferencedDirection5":
                case "ReferencedCharacter":
                case "ReferencedCharacter1":
                case "ReferencedCharacter2":
                case "ReferencedCharacter3":
                case "ReferencedCharacter4":
                case "ReferencedCharacter5":
                case "ReferencedLocation":
                case "ReferencedLocation1":
                case "ReferencedLocation2":
                case "ReferencedLocation3":
                case "ReferencedLocation4":
                case "ReferencedLocation5":
                case "ReferencedItem":
                case "ReferencedItem1":
                case "ReferencedItem2":
                case "ReferencedItem3":
                case "ReferencedItem4":
                case "ReferencedItem5":
                case "ReferencedNumber":
                case "ReferencedNumber1":
                case "ReferencedNumber2":
                case "ReferencedNumber3":
                case "ReferencedNumber4":
                case "ReferencedNumber5":
                    mKey2 = refs.getMatchingKey(mKey2);
                    break;

                case "%Player%":
                    mKey2 = mAdv.getPlayer().getKey();
                    break;
            }
        }

        // Replace any %text% refs in conversation actions
        if (mType == Conversation) {
            for (int i = 0; i <= 5; i++) {
                int iRef = i;
                if (iRef > 0) {
                    iRef--;
                }
                String refText = "%text" + (i > 0 ? i : "") + "%";
                if (mStrValue.equals(refText)) {
                    mStrValue = mAdv.mReferencedText[iRef];
                    break;
                }
            }
        }
    }

    public void executeCopy(@NonNull String cmd, @Nullable MTask task,
                            boolean calledFromEvent,
                            @NonNull EnumSet<MTask.ExecutionStatus> curStatus) {
        // We need to execute a copy of this action as the
        // references get overwritten each time
        copy().execute(cmd, task, calledFromEvent, curStatus);
    }

    private void execute(@NonNull String cmd, @Nullable MTask task,
                         boolean calledFromEvent,
                         @NonNull EnumSet<MTask.ExecutionStatus> curStatus) {
        mAdv.mView.debugPrint(Task, "", High,
                "Execute Single Action: " + getSummary());

        try {
            replaceRefs(task, cmd, curStatus);

            switch (mType) {
                case MoveObject:
                case AddObjectToGroup:
                case RemoveObjectFromGroup: {
                    // Requires two arguments
                    if (mKey1 == null || mKey2 == null) {
                        throw new Exception("badkey");
                    }
                    MObjectHashMap obs =
                            mAdv.mObjects.get(mMoveObjectWhat, mKey1, mPropValue);
                    MGroup grp = null;
                    if (mType != MoveObject) {
                        grp = mAdv.mGroups.get(mKey2);
                        if (grp == null) {
                            throw new Exception("badkey");
                        }
                    }
                    for (MObject ob : obs.values()) {
                        switch (mType) {
                            case MoveObject: {
                                ob.moveObject(mMoveObjectTo, mKey2);
                                break;
                            }
                            case AddObjectToGroup: {
                                assert grp != null;
                                grp.addItemWithProps(ob);
                                break;
                            }
                            case RemoveObjectFromGroup: {
                                assert grp != null;
                                grp.removeItemWithProps(ob);
                                break;
                            }
                        }
                    }
                    break;
                }
                case MoveCharacter:
                case AddCharacterToGroup:
                case RemoveCharacterFromGroup: {
                    // Requires two arguments
                    if (mKey1 == null || mKey2 == null) {
                        throw new Exception("badkey");
                    }
                    MCharacterHashMap chars =
                            mAdv.mCharacters.get(mMoveCharacterWho, mKey1, mPropValue);
                    MGroup grp = null;
                    if (mType != MoveCharacter) {
                        grp = mAdv.mGroups.get(mKey2);
                        if (grp == null) {
                            throw new Exception("badkey");
                        }
                    }
                    for (MCharacter ch : chars.values()) {
                        switch (mType) {
                            case MoveCharacter: {
                                ch.moveCharacter(mMoveCharacterTo, mKey2);
                                break;
                            }
                            case AddCharacterToGroup: {
                                assert grp != null;
                                grp.addItemWithProps(ch);
                                break;
                            }
                            case RemoveCharacterFromGroup: {
                                assert grp != null;
                                grp.removeItemWithProps(ch);
                                break;
                            }
                        }
                    }
                    break;
                }
                case AddLocationToGroup:
                case RemoveLocationFromGroup: {
                    // Requires two arguments
                    if (mKey1 == null || mKey2 == null) {
                        throw new Exception("badkey");
                    }
                    MLocationHashMap locs =
                            mAdv.mLocations.get(mMoveLocationWhat, mKey1, mPropValue);
                    MGroup grp = mAdv.mGroups.get(mKey2);
                    if (grp == null) {
                        throw new Exception("badkey");
                    }
                    for (MLocation loc : locs.values()) {
                        switch (mType) {
                            case AddLocationToGroup: {
                                grp.addItemWithProps(loc);
                                break;
                            }
                            case RemoveLocationFromGroup: {
                                grp.removeItemWithProps(loc);
                                break;
                            }
                        }
                    }
                    break;
                }
                case SetProperties: {
                    // requires two arguments - the key of the item and the key
                    // of the property.
                    if (mKey1 == null || mKey2 == null) {
                        throw new Exception("badkey");
                    }

                    MObject ob;
                    MCharacter ch;
                    MLocation loc;

                    if ((ob = mAdv.mObjects.get(mKey1)) != null) {
                        // -------------------
                        // OBJECT PROPERTY
                        // -------------------
                        ob.setProperty(mKey2, mPropValue);
                    } else if ((ch = mAdv.mCharacters.get(mKey1)) != null) {
                        // -------------------
                        // CHARACTER PROPERTY
                        // -------------------
                        ch.setProperty(mKey2, mPropValue);
                    } else if ((loc = mAdv.mLocations.get(mKey1)) != null) {
                        // -------------------
                        // LOCATION PROPERTY
                        // -------------------
                        loc.setProperty(mKey2, mPropValue);
                    } else {
                        throw new Exception("badkey");
                    }
                    break;
                }
                case SetVariable:
                case IncreaseVariable:
                case DecreaseVariable: {
                    // requires at least one argument
                    if (mKey1 == null) {
                        throw new Exception("badkey");
                    }
                    MVariable var = mAdv.mVariables.get(mKey1);
                    if (var == null) {
                        throw new Exception("badkey");
                    }
                    String expr = mStrValue;
                    switch (mType) {
                        case IncreaseVariable:
                            expr = "%" + var.getName() + "% + " + expr;
                            break;
                        case DecreaseVariable:
                            expr = "%" + var.getName() + "% - " + expr;
                            break;
                    }
                    var.set(mVariableType, mKey2, expr, mIntValue, task);
                    break;
                }
                case SetTasks: {
                    // requires one argument - the task key
                    if (mKey1 == null) {
                        throw new Exception("badkey");
                    }
                    MTask tas = mAdv.mTasks.get(mKey1);
                    if (tas == null) {
                        throw new Exception("badkey");
                    }
                    tas.set(mSetTask, mStrValue, mPropValue,
                            mIntValue, calledFromEvent, curStatus);
                    break;
                }
                case Time: {
                    // no arguments
                    time(mStrValue);
                    break;
                }
                case EndGame: {
                    // no arguments
                    mAdv.mGameState = mEndGame;
                    break;
                }
                case Conversation: {
                    // requires at least one argument -
                    // the character key
                    if (mKey1 == null) {
                        throw new Exception("badkey");
                    }
                    MCharacter ch = mAdv.mCharacters.get(mKey1);
                    if (ch == null) {
                        throw new Exception("badkey");
                    }
                    ch.executeConversation(mConversation, mStrValue, curStatus);
                    break;
                }
            }
        } catch (Exception ex) {
            if (ex.getMessage().equals("badkey")) {
                mAdv.mView.debugPrint(Task, "", High, "Bad key(s) for action ");
            } else {
                mAdv.mView.errMsg("Error executing action " + getSummary(), ex);
            }
        }
    }

    private void initOlder(@NonNull MAdventure adv,
                           int startLoc, int startChar, int startTask,
                           final HashMap<MObject, MProperty> dodgyArlStates,
                           int type, int var1, int var2, int var3, int var5,
                           String expr) {
        // Adrift 4 actions are divided into seven sections.
        switch (type) {
            case 0:
                // ----------------------------------------------------------
                //                    Move object
                // ----------------------------------------------------------
                // This allows you to move all held objects, all worn objects,
                // the Referenced object or a specific object to a specific
                // room, to a room group, to inside an object, onto an object,
                // to the same room as the Player or character, or carried or
                // worn by the Player or specific character.
                // ----------------------------------------------------------
                mType = MoveObject;
                switch (var1) {
                    case 0:
                        // move all objects held by the player ...
                        mMoveObjectWhat = EverythingHeldBy;
                        mKey1 = THEPLAYER;
                        break;
                    case 1:
                        // move all objects worn by the player ...
                        mMoveObjectWhat = EverythingWornBy;
                        mKey1 = THEPLAYER;
                        break;
                    case 2:
                        // move the referenced object ...
                        mMoveObjectWhat = MAction.MoveObjectWhatEnum.Object;
                        mKey1 = "ReferencedObject";
                        break;
                    default:
                        // move a specific object ...
                        mMoveObjectWhat = MAction.MoveObjectWhatEnum.Object;
                        mKey1 = getObjectKey(adv, var1 - 3, Dynamic);
                        break;
                }
                switch (var2) {
                    case 0:
                        // ... to a specific location (or hidden).
                        mMoveObjectTo = ToLocation;
                        mKey2 = (var3 == 0) ?
                                "Hidden" : "Location" + (var3 + startLoc);
                        break;
                    case 1:
                        // ... to a location group.
                        mMoveObjectTo = ToLocationGroup;
                        // N.B. original Runner code seems to have
                        // accidentally omitted this line:
                        mKey2 = "Group" + (var3 + 1);
                        break;
                    case 2:
                        // ... to inside an object.
                        mMoveObjectTo = InsideObject;
                        mKey2 = getObjectKey(adv, var3, Container);
                        break;
                    case 3:
                        // ... onto an object.
                        mMoveObjectTo = OntoObject;
                        mKey2 = getObjectKey(adv, var3, Surface);
                        break;
                    case 4:
                        // ... to be carried by a specific character.
                        mMoveObjectTo = ToCarriedBy;
                        break;
                    case 5:
                        // ... to be worn by a specific character.
                        mMoveObjectTo = ToWornBy;
                        break;
                    case 6:
                        // ... to the same location as a specific character.
                        mMoveObjectTo = ToSameLocationAs;
                        break;
                }

                if (var2 > 3) {
                    switch (var3) {
                        case 0:
                            mKey2 = "%Player%";
                            break;
                        case 1:
                            mKey2 = "ReferencedCharacter";
                            break;
                        default:
                            mKey2 = "Character" + (var3 - 1 + startChar);
                            break;
                    }
                }
                break;

            case 1:
                // ----------------------------------------------------------
                //                Move Player or Characters
                // ----------------------------------------------------------
                // This allows you to move the Player or a specific character
                // to a specific room, room group, or to the same room as a
                // specific character. It also allows you to move the Player’s
                // position to standing, sitting or lying on a specific object.
                // ----------------------------------------------------------
                mType = MoveCharacter;
                mMoveCharacterWho = MCharacterHashMap.MoveCharacterWhoEnum.Character;
                switch (var1) {
                    case 0:
                        // move the player ...
                        mKey1 = THEPLAYER;
                        break;
                    case 1:
                        // move the referenced character ...
                        mKey1 = "ReferencedCharacter";
                        break;
                    default:
                        // move a specific character ...
                        mKey1 = "Character" + (var1 - 1 + startChar);
                        break;
                }
                switch (var2) {
                    case 0:
                        // ... to a specific location.
                        mMoveCharacterTo = MAction.MoveCharacterToEnum.ToLocation;
                        mKey2 = (mKey1.equals("%Player%")) ?
                                "Location" + (var3 + startLoc + 1) :
                                "Location" + (var3 + startLoc);
                        if (mKey2.equals("Location0")) {
                            mKey2 = "Hidden";
                        }
                        break;
                    case 1:
                        // ... to a specific location group.
                        mMoveCharacterTo = MAction.MoveCharacterToEnum.ToLocationGroup;
                        // N.B. original Runner code seems to have
                        // accidentally omitted this line:
                        mKey2 = "Group" + (var3 + 1);
                        break;
                    case 2:
                        // ... to the same location as a specific character.
                        mMoveCharacterTo = MAction.MoveCharacterToEnum.ToSameLocationAs;
                        switch (var3) {
                            case 0:
                                mKey2 = "%Player%";
                                break;
                            case 1:
                                mKey2 = "ReferencedCharacter";
                                break;
                            default:
                                mKey2 = "Character" + (var3 - 2 + startChar);
                                break;
                        }
                        break;
                    case 3:
                        // ... to standing on ...
                        mMoveCharacterTo = MAction.MoveCharacterToEnum.ToStandingOn;
                        switch (var3) {
                            case 0:
                                // ... the floor.
                                mKey2 = THEFLOOR;
                                break;
                            default:
                                // ... a specific object.
                                mKey2 = getObjectKey(adv, var3 - 1, Standable);
                                break;
                        }
                        break;
                    case 4:
                        // ... to sitting on ...
                        mMoveCharacterTo = MAction.MoveCharacterToEnum.ToSittingOn;
                        switch (var3) {
                            case 0:
                                // ... the floor.
                                mKey2 = THEFLOOR;
                                break;
                            default:
                                /// ... a specific object.
                                mKey2 = getObjectKey(adv, var3 - 1, Sittable);
                                break;
                        }
                        break;
                    case 5:
                        // ... to lying on ...
                        mMoveCharacterTo = MAction.MoveCharacterToEnum.ToLyingOn;
                        switch (var3) {
                            case 0:
                                // ... the floor.
                                mKey2 = THEFLOOR;
                                break;
                            default:
                                // ... a specific object.
                                mKey2 = getObjectKey(adv, var3 - 1, Lieable);
                                break;
                        }
                        break;
                }
                break;

            case 2:
                // ----------------------------------------------------------
                //                 Change object status
                // ----------------------------------------------------------
                // This enables you to open or close objects.
                // ----------------------------------------------------------
                mType = SetProperties;
                mKey1 = getObjectKey(adv, var1, WithStateOrOpenable);
                MObject ob = adv.mObjects.get(mKey1);
                if (ob == null) {
                    return;
                }
                switch (var2) {
                    case 0:
                        // open a specific object.
                        if (ob.isOpenable()) {
                            mKey2 = "OpenStatus";
                            mPropValue = "Open";
                        } else {
                            MProperty prop = dodgyArlStates.get(ob);
                            if (prop != null) {
                                mKey2 = prop.getKey();
                                mPropValue = prop.mStates.get(var2);
                            }
                        }
                        break;
                    case 1:
                        // close a specific object.
                        if (ob.isOpenable()) {
                            mKey2 = "OpenStatus";
                            mPropValue = "Closed";
                        } else {
                            MProperty prop = dodgyArlStates.get(ob);
                            if (prop != null) {
                                mKey2 = prop.getKey();
                                mPropValue = prop.mStates.get(var2);
                            }
                        }
                        break;
                    case 2:
                        // lock a specific object.
                        if (ob.isOpenable()) {
                            if (ob.isLockable()) {
                                mKey2 = "OpenStatus";
                                mPropValue = "Locked";
                            } else {
                                MProperty prop = dodgyArlStates.get(ob);
                                if (prop != null) {
                                    mKey2 = prop.getKey();
                                    mPropValue = prop.mStates.get(var2 - 2);
                                }
                            }
                        } else {
                            MProperty prop = dodgyArlStates.get(ob);
                            if (prop != null) {
                                mKey2 = prop.getKey();
                                mPropValue = prop.mStates.get(var2);
                            }
                        }
                        break;
                    default:
                        int off = 0;
                        if (ob.isOpenable()) {
                            off = ob.isLockable() ? 3 : 2;
                        }
                        MProperty prop = dodgyArlStates.get(ob);
                        if (prop != null) {
                            mKey2 = prop.getKey();
                            mPropValue = prop.mStates.get(var2 - off);
                        }
                        break;
                }
                break;

            case 3:
                // ----------------------------------------------------------
                //                    Change variable
                // ----------------------------------------------------------
                // You can change any variable to an exact value,change it
                // by an exact value, change it to a random value (between
                // two values), change it by a random value (between two
                // values), or set it to the referenced number. There is
                // also the option to change a variable to a mathematical
                // expression. This is interpreted directly, and can include
                // variables and functions. An example might be something like:
                //
                //               min(%var1%, 2 - %var2%) * 3
                // ----------------------------------------------------------
                mType = SetVariable;
                mKey1 = "Variable" + (var1 + 1);
                mVariableType = ASSIGNMENT;

                // For now we simply store the arguments in this action's
                // string value; later call fixOlderVars() to expand
                // this into the correct values:
                mStrValue = String.valueOf(var2) + (char) (1) +
                        String.valueOf(var3) + (char) (1) +
                        String.valueOf(var5) + (char) (1) +
                        (!expr.equals("") ? expr : "\u2422");
                break;

            case 4:
                // ----------------------------------------------------------
                //                    Change score
                // ----------------------------------------------------------
                // You can change the score by a specific value. If this is
                // negative, the score will decrease. Positive score increments
                // will only happen the first time a task executes if it is
                // repeatable or reversible. Negative scores will occur each
                // time.
                // ----------------------------------------------------------
                mType = SetVariable;
                mKey1 = "Score";
                mVariableType = ASSIGNMENT;

                // For now we simply store the arguments in this action's
                // string value; later call fixOlderVars() to expand
                // this into the correct values:
                mStrValue = "1" + (char) (1) +
                        String.valueOf(var1) + (char) (1) +
                        String.valueOf("0") + (char) (1) +
                        "\u2422";
                break;

            case 5:
                // ----------------------------------------------------------
                //                    Set Task
                // ----------------------------------------------------------
                // Set Task
                // ----------------------------------------------------------
                mType = SetTasks;
                mSetTask = (var1 == 0) ? Execute : Unset;
                mKey1 = "Task" + (var2 + startTask + 1);
                mStrValue = "";
                break;

            case 6:
                // ----------------------------------------------------------
                //                    End game
                // ----------------------------------------------------------
                // You can end the game in one of three states; "Wins the
                // game", "Doesn't win" (just a standard end to the game),
                // and "Kills the Player".
                // ----------------------------------------------------------
                mType = EndGame;
                switch (var1) {
                    case 0:
                        // Wins the game.
                        mEndGame = MAction.EndGameEnum.Win;
                        break;
                    case 1:
                        // Doesn't win.
                        mEndGame = Neutral;
                        break;
                    case 2:
                    case 3:
                        // Kills the player.
                        mEndGame = Lose;
                        break;
                }
                break;

            case 7:
                // ----------------------------------------------------------
                //                   Battle System
                // ----------------------------------------------------------
                // When the Battle System is enabled, extra options become
                // available for task actions. You can:
                //
                //      1) Change the attitude of a character.
                //      2) Change the stamina of the Player or characters.
                //      3) Change the maximum stamina of the Player or
                //      characters.
                //      4) Change the strength of the Player or characters.
                //      5) Change the maximum strength of the Player or
                //      characters
                //      6) Change the defence value of the Player or
                //      characters.
                //      7) Change the maximum defence value of the Player or
                //      characters.
                //      8) Change the speed of a character.
                // ----------------------------------------------------------
                // TODO
                break;

        }
    }

    public void fixOlderVars() {
        if (mType == SetVariable) {
            String[] combos = mStrValue.split(String.valueOf((char) (1)));
            int combo1 = cint(combos[0]);
            int combo2 = cint(combos[1]);
            int combo3 = cint(combos[2]);
            String tmp = combos[3];
            String expr = tmp.equals("\u2422") ? "" : tmp;

            MVariable var = mAdv.mVariables.get(mKey1);
            if (var != null && var.getType() == Numeric) {
                switch (combo1) {
                    case 0:
                        // to exact value
                        mStrValue = String.valueOf(combo2);
                        break;
                    case 1:
                        // by exact value
                        mStrValue = "%" + var.getName() + "% + " + combo2;
                        break;
                    case 2:
                        // to random value between X and Y
                        mStrValue = "Rand(" + combo2 + ", " + combo3 + ")";
                        break;
                    case 3:
                        // by random value between X and Y
                        mStrValue = "%" + var.getName() +
                                "% + Rand(" + combo2 + ", " + combo3 + ")";
                        break;
                    case 4:
                        // to referenced number
                        mStrValue = "%number1%";
                        break;
                    case 5:
                        // to expression
                        mStrValue = expr;
                        break;
                    case 6:
                    case 7:
                    case 8:
                    case 9:
                    case 10:
                        mStrValue = "";
                        break;
                }
            } else {
                switch (combo1) {
                    case 0:
                        // exact text
                        mStrValue = "\"" +
                                expr.replace("\"\"", "\"") + "\"";
                        break;
                    case 1:
                        // to referenced text
                        mStrValue = "%text1%";
                        break;
                    case 2:
                        // to expression
                        mStrValue = expr;
                        break;
                }
            }
        }

        if (instr(mStrValue, "Variable") > 0) {
            for (int i = mAdv.mVariables.size(); i >= 1; i--) {
                MVariable var = mAdv.mVariables.get("Variable" + i);
                if (var != null) {
                    mStrValue = mStrValue.replace("Variable" + i,
                            "%" + var.getName() + "%");
                }
            }
        }
    }

    @NonNull
    public String getSummary() {
        StringBuilder ret = new StringBuilder("Undefined Action");

        try {
            switch (mType) {
                case MoveObject:
                case AddObjectToGroup:
                case RemoveObjectFromGroup: {
                    // What are we doing
                    switch (mType) {
                        case MoveObject:
                            ret = new StringBuilder("Move ");
                            break;
                        case AddObjectToGroup:
                            ret = new StringBuilder("Add ");
                            break;
                        case RemoveObjectFromGroup:
                            ret = new StringBuilder("Remove ");
                            break;
                    }

                    // What are we moving
                    String itmName = mAdv.getNameFromKey(mKey1);
                    boolean isDynamic = true;
                    switch (mMoveObjectWhat) {
                        case Object:
                            if (mKey1 != null && !mKey1.startsWith("ReferencedObject")) {
                                MObject ob = mAdv.mObjects.get(mKey1);
                                if (ob == null) {
                                    throw new Exception("MoveObjectWhat: reference to " +
                                            "non-existent object key: " + mKey1);
                                }
                                isDynamic = !ob.isStatic();
                            }
                            ret.append(itmName);
                            break;
                        case EverythingHeldBy:
                            ret.append("everything held by ").append(itmName);
                            break;
                        case EverythingAtLocation:
                            ret.append("everything at ").append(itmName);
                            break;
                        case EverythingInGroup:
                            ret.append("everything in ").append(itmName);
                            break;
                        case EverythingInside:
                            ret.append("everything inside ").append(itmName);
                            break;
                        case EverythingOn:
                            ret.append("everything on ").append(itmName);
                            break;
                        case EverythingWithProperty:
                            ret.append("everything with ").append(mAdv.getNameFromKey(mKey1));
                            if (!mPropValue.equals("")) {
                                MProperty prop = mAdv.mObjectProperties.get(mKey1);
                                if (prop == null) {
                                    throw new Exception("MoveObjectWhat: reference to " +
                                            "non-existent object property : " + mKey1);
                                }
                                ret.append(" where value is ");
                                switch (prop.getType()) {
                                    case CharacterKey:
                                    case LocationGroupKey:
                                    case LocationKey:
                                    case ObjectKey:
                                        ret.append(mAdv.getNameFromKey(mPropValue));
                                        break;
                                    case Integer:
                                    case StateList:
                                    case Text:
                                        ret.append("'").append(mPropValue).append("'");
                                        break;
                                    case SelectionOnly:
                                        // N/A
                                        break;
                                }
                            }
                            break;
                        case EverythingWornBy:
                            ret.append("everything worn by ").append(itmName);
                            break;
                    }

                    // Where are we moving it to
                    String itmName2 = mAdv.getNameFromKey(mKey2);
                    switch (mMoveObjectTo) {
                        case InsideObject:
                            ret.append(" inside ").append(itmName2);
                            break;
                        case OntoObject:
                            ret.append(" onto ").append(itmName2);
                            break;
                        case ToCarriedBy:
                            ret.append(" to held by ").append(itmName2);
                            break;
                        case ToLocation:
                            if (mKey2 != null && mKey2.equals("Hidden")) {
                                ret.append(" to Hidden");
                            } else {
                                ret.append(" to ").append(itmName2);
                            }
                            break;
                        case ToLocationGroup:
                            if (isDynamic) {
                                ret.append(" to somewhere in location ").append(itmName2);
                            } else {
                                ret.append(" to everywhere in location ").append(itmName2);
                            }
                            break;
                        case ToPartOfCharacter:
                            ret.append(" to part of ").append(itmName2);
                            break;
                        case ToPartOfObject:
                            ret.append(" to part of ").append(itmName2);
                            break;
                        case ToSameLocationAs:
                            ret.append(" to same location as ").append(itmName2);
                            break;
                        case ToWornBy:
                            ret.append(" to worn by ").append(itmName2);
                            break;
                        case ToGroup:
                            ret.append(" to ").append(itmName2);
                            break;
                        case FromGroup:
                            ret.append(" from ").append(itmName2);
                            break;
                    }
                    break;
                }
                case MoveCharacter:
                case AddCharacterToGroup:
                case RemoveCharacterFromGroup: {
                    // what are we doing
                    switch (mType) {
                        case MoveCharacter:
                            ret = new StringBuilder("Move ");
                            break;
                        case AddCharacterToGroup:
                            ret = new StringBuilder("Add ");
                            break;
                        case RemoveCharacterFromGroup:
                            ret = new StringBuilder("Remove ");
                            break;
                    }

                    // who are we doing it to
                    String itmName = mAdv.getNameFromKey(mKey1);
                    switch (mMoveCharacterWho) {
                        case Character:
                            ret.append(itmName);
                            break;
                        case EveryoneAtLocation:
                            ret.append("everyone at ").append(itmName);
                            break;
                        case EveryoneInGroup:
                            ret.append("everyone in ").append(itmName);
                            break;
                        case EveryoneInside:
                            ret.append("everyone inside ").append(itmName);
                            break;
                        case EveryoneOn:
                            ret.append("everyone on ").append(itmName);
                            break;
                        case EveryoneWithProperty:
                            ret.append("everyone with ").append(itmName);
                            if (!mPropValue.equals("")) {
                                MProperty prop = mAdv.mCharacterProperties.get(mKey1);
                                if (prop == null) {
                                    throw new Exception("MoveCharacterWho: reference to " +
                                            "non-existent character property: " + mKey1);
                                }
                                ret.append(" where value is ");
                                switch (prop.getType()) {
                                    case CharacterKey:
                                    case LocationGroupKey:
                                    case LocationKey:
                                    case ObjectKey:
                                        ret.append(mAdv.getNameFromKey(mPropValue));
                                        break;
                                    case Integer:
                                    case StateList:
                                    case Text:
                                        ret.append("'").append(mPropValue).append("'");
                                        break;
                                    case SelectionOnly:
                                        // N/A
                                        break;
                                }
                            }
                            break;
                    }

                    // where are we moving them to
                    String itmName2 = mAdv.getNameFromKey(mKey2);
                    switch (mMoveCharacterTo) {
                        case InDirection:
                            ret.append(" in direction");
                            if (mKey2 != null && mKey2.startsWith("ReferencedDirection")) {
                                ret.append(mKey2.replace("ReferencedDirection",
                                        " Referenced Direction "));
                            } else {
                                ret.append(" ").append(mAdv.getDirectionName(MAdventure.DirectionsEnum.valueOf(mKey2)));
                            }
                            break;
                        case ToLocation:
                            ret.append(" to ").append(itmName2);
                            break;
                        case ToLocationGroup:
                            ret.append(" to ").append(itmName2);
                            break;
                        case ToSameLocationAs:
                            ret.append(" to same location as ").append(itmName2);
                            break;
                        case ToStandingOn:
                            ret.append(" to standing on ").append(itmName2);
                            break;
                        case ToSittingOn:
                            ret.append(" to sitting on ").append(itmName2);
                            break;
                        case ToSwitchWith:
                            ret.append(" to switch places with ").append(itmName2);
                            break;
                        case ToLyingOn:
                            ret.append(" to lying on ").append(itmName2);
                            break;
                        case InsideObject:
                            ret.append(" inside ").append(itmName2);
                            break;
                        case OntoCharacter:
                            ret.append(" onto ").append(itmName2);
                            break;
                        case ToParentLocation:
                            ret.append(" to parent location");
                            break;
                        case ToGroup:
                            ret.append(" to ").append(itmName2);
                            break;
                        case FromGroup:
                            ret.append(" from ").append(itmName2);
                            break;
                    }
                    break;
                }
                case AddLocationToGroup:
                case RemoveLocationFromGroup: {
                    // what are we doing
                    switch (mType) {
                        case AddLocationToGroup:
                            ret = new StringBuilder("Add ");
                            break;
                        case RemoveLocationFromGroup:
                            ret = new StringBuilder("Remove ");
                            break;
                    }

                    // which location are we doing it to
                    String itmName = mAdv.getNameFromKey(mKey1);
                    switch (mMoveLocationWhat) {
                        case Location:
                            ret.append(itmName);
                            break;
                        case LocationOf:
                            ret.append("location of ").append(itmName);
                            break;
                        case EverywhereInGroup:
                            ret.append("everywhere in ").append(itmName);
                            break;
                        case EverywhereWithProperty:
                            ret.append("everywhere with ").append(itmName);
                            if (!mPropValue.equals("")) {
                                MProperty prop = mAdv.mLocationProperties.get(mKey1);
                                if (prop == null) {
                                    throw new Exception("MoveLocationWhat: reference to " +
                                            "non-existent location property: " + mKey1);
                                }
                                ret.append(" where value is ");
                                switch (prop.getType()) {
                                    case CharacterKey:
                                    case LocationGroupKey:
                                    case LocationKey:
                                    case ObjectKey:
                                        ret.append(mAdv.getNameFromKey(mPropValue));
                                        break;
                                    case Integer:
                                    case StateList:
                                    case Text:
                                        ret.append("'").append(mPropValue).append("'");
                                        break;
                                    case SelectionOnly:
                                        // N/A
                                        break;
                                }
                            }
                            break;
                    }

                    // which group are we moving to
                    String grpName = mAdv.getNameFromKey(mKey2);
                    switch (mMoveLocationTo) {
                        case ToGroup:
                            ret.append(" to ").append(grpName);
                            break;
                        case FromGroup:
                            ret.append(" from ").append(grpName);
                            break;
                    }
                    break;
                }
                case SetProperties: {
                    MProperty prop = mAdv.mAllProperties.get(mKey2);
                    if (prop == null) {
                        throw new Exception("SetProperties: reference to " +
                                "non-existent property: " + mKey2);
                    }
                    ret = new StringBuilder("Set property '" + prop.getDescription() +
                            "' of " + mAdv.getNameFromKey(mKey1) + " to ");
                    boolean setVar = false;
                    if (mPropValue.length() > 2 &&
                            mPropValue.startsWith("%") && mPropValue.endsWith("%")) {
                        for (MVariable v : mAdv.mVariables.values()) {
                            if (mPropValue.equals("%" + v.getName() + "%")) {
                                ret.append(mAdv.getNameFromKey(v.getKey()));
                                setVar = true;
                                break;
                            }
                        }
                    }
                    if (!setVar && prop.getType() == ValueList && isNumeric(mPropValue)) {
                        for (String val : prop.mValueList.keySet()) {
                            Integer vl = prop.mValueList.get(val);
                            if (vl != null && cint(mPropValue) == vl) {
                                ret.append("'").append(val).append("'");
                                setVar = true;
                                break;
                            }
                        }
                    }
                    if (!setVar && mPropValue.startsWith("Referenced")) {
                        ret.append(mAdv.getNameFromKey(mPropValue));
                        setVar = true;
                    }
                    if (!setVar) {
                        ret.append("'").append(mPropValue).append("'");
                    }
                    break;
                }
                case SetVariable:
                case IncreaseVariable:
                case DecreaseVariable: {
                    MVariable var = mAdv.mVariables.get(mKey1);
                    if (var == null) {
                        throw new Exception("SetVariable: reference to " +
                                "non-existent variable: " + mKey1);
                    }
                    switch (mVariableType) {
                        case ASSIGNMENT:
                            switch (mType) {
                                case SetVariable:
                                    ret = new StringBuilder("Set");
                                    break;
                                case IncreaseVariable:
                                    ret = new StringBuilder("Increase");
                                    break;
                                case DecreaseVariable:
                                    ret = new StringBuilder("Decrease");
                                    break;
                            }
                            ret.append(" variable ").append(var.getName());
                            if (var.getLength() > 1) {
                                if (isNumeric(mKey2)) {
                                    ret.append("[").append(mKey2).append("]");
                                } else if (mKey2.startsWith("ReferencedNumber")) {
                                    // We already get brackets in the ref name,
                                    // so don't double them up
                                    ret.append(mAdv.getNameFromKey(mKey2, false));
                                } else {
                                    MVariable var2 = mAdv.mVariables.get(mKey2);
                                    if (var2 == null) {
                                        throw new Exception("SetVariable: reference to " +
                                                "non-existent variable: " + mKey2);
                                    }
                                    ret.append("[%").append(var2.getName()).append("%]");
                                }
                            }
                            switch (mType) {
                                case SetVariable:
                                    ret.append(" to ");
                                    break;
                                case IncreaseVariable:
                                case DecreaseVariable:
                                    ret.append(" by ");
                                    break;
                            }
                            if (var.getType() == Numeric) {
                                ret.append("'").append(mStrValue).append("'");
                            } else {
                                // already has quotes around it -
                                // oh really, not if it's an expression
                                ret.append("'").append(mStrValue).append("'");
                            }
                            break;
                        case LOOP:
                            ret = new StringBuilder("FOR %Loop% = " + mIntValue + " TO " +
                                    mKey2 + " : SET " + var.getName() + "[%Loop%] = " +
                                    mStrValue + " : NEXT %Loop%");
                            break;
                    }
                    break;
                }
                case SetTasks: {
                    if (!mPropValue.equals("")) {
                        ret = new StringBuilder("FOR %Loop% = " + mIntValue + " TO " +
                                mPropValue + " : ");
                    } else {
                        ret = new StringBuilder();
                    }
                    String tasName = mAdv.getNameFromKey(mKey1);
                    switch (mSetTask) {
                        case Execute:
                            ret.append("Execute ").append(tasName);
                            if (!mStrValue.equals("")) {
                                ret.append(" (").append(mStrValue.replace("|", ", ")).append(")");
                            }
                            break;
                        case Unset:
                            ret.append("Unset ").append(tasName);
                            break;
                    }
                    if (!mPropValue.equals("")) {
                        ret.append(" : NEXT %Loop%");
                    }
                    break;
                }
                case Time: {
                    ret = new StringBuilder("Skip " + mStrValue + " turn(s)");
                    break;
                }
                case EndGame: {
                    switch (mEndGame) {
                        case Win:
                            ret = new StringBuilder("End game in Victory");
                            break;
                        case Lose:
                            ret = new StringBuilder("End game in Defeat");
                            break;
                        case Neutral:
                            ret = new StringBuilder("End game");
                            break;
                    }
                    break;
                }
                case Conversation: {
                    String chName = mAdv.getNameFromKey(mKey1);
                    switch (mConversation) {
                        case Greet:
                            ret = new StringBuilder("Greet " + chName +
                                    (!mStrValue.equals("") ? " with '" + mStrValue + "'" : ""));
                            break;
                        case Ask:
                            ret = new StringBuilder("Ask " + chName +
                                    " about '" + mStrValue + "'");
                            break;
                        case Tell:
                            ret = new StringBuilder("Tell " + chName +
                                    " about '" + mStrValue + "'");
                            break;
                        case Command:
                            ret = new StringBuilder("Say '" + mStrValue + "' to " + chName);
                            break;
                        case EnterConversation:
                            ret = new StringBuilder("Enter conversation with " + chName);
                            break;
                        case LeaveConversation:
                            ret = new StringBuilder("Leave conversation with " + chName);
                            break;
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            ret = new StringBuilder("Bad Action Definition: ").append(ex.getMessage());
        }

        return ret.toString();
    }

    @NonNull
    public MAction copy() {
        MAction act = new MAction(mAdv);

        act.mEndGame = this.mEndGame;
        act.mType = this.mType;
        act.mMoveObjectWhat = this.mMoveObjectWhat;
        act.mMoveObjectTo = this.mMoveObjectTo;
        act.mMoveCharacterWho = this.mMoveCharacterWho;
        act.mMoveCharacterTo = this.mMoveCharacterTo;
        act.mMoveLocationWhat = this.mMoveLocationWhat;
        act.mMoveLocationTo = this.mMoveLocationTo;
        act.mPropValue = this.mPropValue;
        act.mObjectStatus = this.mObjectStatus;
        act.mSetTask = this.mSetTask;
        act.mVariableType = this.mVariableType;
        act.mConversation = this.mConversation;
        act.mIntValue = this.mIntValue;
        act.mKey1 = this.mKey1;
        act.mKey2 = this.mKey2;
        act.mStrValue = this.mStrValue;

        return act;
    }

    @NonNull
    @Override
    public String toString() {
        return getSummary();
    }

    public boolean referencesKey(@NonNull String key) {
        return (mKey1 != null && mKey1.equals(key)) || (mKey2 != null && mKey2.equals(key));
    }

    public enum ItemEnum {
        // Objects
        MoveObject,                 // 0
        AddObjectToGroup,           // 1
        RemoveObjectFromGroup,      // 2

        // Characters
        MoveCharacter,              // 3
        AddCharacterToGroup,        // 4
        RemoveCharacterFromGroup,   // 5

        // Variables
        SetVariable,                // 6
        IncreaseVariable,           // 7
        DecreaseVariable,           // 8

        SetProperties,              // 9
        SetTasks,                   // 10
        EndGame,                    // 11
        Conversation,               // 12

        // Locations
        AddLocationToGroup,         // 13
        RemoveLocationFromGroup,    // 14

        Time                        // 15
    }

    public enum MoveObjectWhatEnum {
        Object,                     // 0
        EverythingHeldBy,           // 1
        EverythingWornBy,           // 2
        EverythingInside,           // 3
        EverythingOn,               // 4
        EverythingWithProperty,     // 5
        EverythingInGroup,          // 6
        EverythingAtLocation        // 7
    }

    public enum MoveObjectToEnum {
        // Static or Dynamic Moves
        ToLocation,                 // 0
        ToSameLocationAs,           // 1, Object or Character
        ToLocationGroup,            // 2, If static, moves to all locations.  If dynamic, moves to random location

        // Dynamic Moves

        InsideObject,               // 3
        OntoObject,                 // 4
        ToCarriedBy,                // 5
        ToWornBy,                   // 6

        // Static Moves
        ToPartOfCharacter,          // 7
        ToPartOfObject,             // 8

        // To/From Group
        ToGroup,                    // 9
        FromGroup                   // 10
    }

    public enum MoveCharacterToEnum {
        InDirection,
        ToLocation,
        ToLocationGroup,
        ToSameLocationAs, // Object or Character
        ToStandingOn,
        ToSittingOn,
        ToLyingOn,
        ToSwitchWith,
        InsideObject,
        OntoCharacter,
        ToParentLocation,
        ToGroup,
        FromGroup
    }

    public enum MoveLocationToEnum {
        // To/From Group
        ToGroup,                    // 0
        FromGroup                   // 1
    }

    public enum ObjectStatusEnum {
        DunnoYet                    // 0
    }

    public enum EndGameEnum {
        Win,                        // 0
        Lose,                       // 1
        Neutral,                    // 2
        Running                     // 3
    }
}
