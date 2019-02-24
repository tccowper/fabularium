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

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.model.collection.MActionArrayList;
import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MItemHashMap;
import com.luxlunae.bebek.model.collection.MLocationHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MOrderedHashMap;
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.collection.MStringHashMap;
import com.luxlunae.bebek.model.collection.MTaskHashMap;
import com.luxlunae.bebek.model.io.MFileOlder;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.luxlunae.bebek.MGlobals.ItemEnum.Task;
import static com.luxlunae.bebek.MGlobals.appendDoubleSpace;
import static com.luxlunae.bebek.MGlobals.containsWord;
import static com.luxlunae.bebek.MGlobals.getBool;
import static com.luxlunae.bebek.MGlobals.guessPluralNoun;
import static com.luxlunae.bebek.MGlobals.left;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.model.MAction.ItemEnum.DecreaseVariable;
import static com.luxlunae.bebek.model.MAction.ItemEnum.IncreaseVariable;
import static com.luxlunae.bebek.model.MAction.ItemEnum.SetTasks;
import static com.luxlunae.bebek.model.MAction.ItemEnum.SetVariable;
import static com.luxlunae.bebek.model.MAdventure.MGameState.restoreDisplayOnce;
import static com.luxlunae.bebek.model.MAdventure.MGameState.saveDisplayOnce;
import static com.luxlunae.bebek.model.MAdventure.MTasksListEnum.SpecificTasks;
import static com.luxlunae.bebek.model.MAdventure.TaskExecutionEnum.HighestPriorityPassingTask;
import static com.luxlunae.bebek.model.MEventOrWalkControl.CompleteOrNotEnum.Completion;
import static com.luxlunae.bebek.model.MEventOrWalkControl.CompleteOrNotEnum.UnCompletion;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.Must;
import static com.luxlunae.bebek.model.MRestriction.createLocRestriction;
import static com.luxlunae.bebek.model.MTask.BeforeAfterEnum.After;
import static com.luxlunae.bebek.model.MTask.BeforeAfterEnum.Before;
import static com.luxlunae.bebek.model.MTask.ExecutionStatus.ContinueExecuting;
import static com.luxlunae.bebek.model.MTask.ExecutionStatus.HasOutput;
import static com.luxlunae.bebek.model.MTask.SetTasksEnum.Execute;
import static com.luxlunae.bebek.model.MTask.SetTasksEnum.Unset;
import static com.luxlunae.bebek.model.MTask.SpecificOverrideTypeEnum.BeforeActionsOnly;
import static com.luxlunae.bebek.model.MTask.SpecificOverrideTypeEnum.BeforeTextAndActions;
import static com.luxlunae.bebek.model.MTask.SpecificOverrideTypeEnum.BeforeTextOnly;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.General;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.Specific;
import static com.luxlunae.bebek.model.MWalk.StatusEnum.Paused;
import static com.luxlunae.bebek.model.MWalk.StatusEnum.Running;
import static com.luxlunae.bebek.model.io.MFileIO.correctBracketSequence;
import static com.luxlunae.bebek.model.io.MFileIO.fixInitialRefs;
import static com.luxlunae.bebek.model.io.MFileOlder.convertV4FuncsToV5;
import static com.luxlunae.bebek.model.io.MFileOlder.getRoomGroupFromList;
import static com.luxlunae.bebek.model.io.MFileOlder.loadResource;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.High;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Low;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Everything that happens within an ADRIFT adventure is the result of a task.
 * Tasks do things. All tasks have a set of restrictions (conditions that must be
 * met in order for the task to run) and a set of actions (things that will be
 * carried out if the task runs).
 * <p>
 * The ADRIFT command parser decides which task to run by matching what the
 * player types with the commands defined in general tasks, and with the
 * names of the objects, characters and locations in the game.
 * <p>
 * A task is like an IF statement in a programming language, when it is
 * executed it first checks its restrictions to see if they pass. Each
 * restriction contains text that is displayed to the player if it is the first one to fails.
 * <p>
 * If the restrictions pass then the task does two things, it displays the
 * text in its main text box, and it performs the actions on its actions page.
 * <p>
 * There are three main types of task in ADRIFT 5. These are:
 * <p>
 * 1) Specific tasks: Use these to define what happens when the
 * player uses a command on a specific object, character or
 * location, or in specific circumstances. This is the
 * most common type of task that you will use.
 * <p>
 * 2) General tasks: Use these to define new commands that the
 * player can enter. You create a new general task if you want
 * to use a command (verb) that is not defined in the standard library.
 * <p>
 * 3) System tasks: Controls what happens when the player first starts
 * the game or whenever they enter a particular location
 */
public class MTask extends MItem {
    /**
     * The priority of the first library task (as per Adrift 5 convention
     * we start library tasks as priority 50000).
     */
    public static final int LIBRARY_START_TASK_PRIORITY = 50000;

    /**
     * A pre-compiled pattern that matches a "all but obj1, obj2, obj3"
     * style string.
     */
    private static final Pattern PATTERN_OBJECTS =
            Pattern.compile("^((?<all>all( ^(?!except|but|apart from).+)?)|(?<objects1>.+))( (except|but|apart from) (?<objects2>.+))?$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * A pre-compiled pattern that matches a "obj1, obj2 and obj3"
     * style string.
     */
    private static final Pattern PATTERN_OBJECTS2 =
            Pattern.compile("^(?<commaseparatedobjects>(.+), )*(?<object2>.+) and (?<object3>.+)$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * A pre-compiled pattern that matches a "char1, char2 and char3"
     * style string.
     */
    private static final Pattern PATTERN_CHARACTERS =
            Pattern.compile("^(?<commaseparatedcharacters>(.+), )*(?<character2>.+) and (?<character3>.+)$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * A pattern to match an integer.
     */
    private static final String PATTERN_NUMBER = "^-?[0-9]+$";

    /**
     * A pattern to match any text.
     */
    private static final String PATTERN_TEXT = "^.*$";

    static boolean mTestingOutput = false;

    /**
     * A list of one or more commands that this task will match.
     */
    @NonNull
    public MStringArrayList mCommands = new MStringArrayList();
    /**
     * A list of zero or more restrictions that must pass in order for this
     * task to perform its actions and show its completion message.
     */
    @NonNull
    public MRestrictionArrayList mRestrictions;
    /**
     * A list of zero or more actions that this task will perform if it
     * is executed and its restrictions all pass.
     */
    @NonNull
    public MActionArrayList mActions;
    /**
     * This controls the order in which the task executes:
     * <p>
     * (1) Before: The contents of the tasks' text box is displayed
     * first, then the Actions of the Actions tab are executed.
     * <p>
     * (2) After: The actions are executed first and then the text is displayed.
     * <p>
     * This is only important if: (a) an action executes a task that produces text
     * output; (b) an action alters a variable which is then displayed as part of
     * the text output; or (c) an action alters something which is used to
     * determine if a restriction passes on an alternate description for the task.
     */
    public BeforeAfterEnum mDisplayCompletion = Before;
    /**
     * There are three types of tasks:
     * <p>
     * (1) Specific - the task defines what happens when the player
     * uses a command on a specific object, character or location, or in
     * specific circumstances.
     * <p>
     * (2) General - the task defines new commands that the player can enter.
     * <p>
     * (3) System - the task controls what happens when the player first starts the
     * game or whenever they enter a particular location.
     */
    public TaskTypeEnum mType = General;
    /**
     * By default the task execution logic will follow that specified by MAdventure.TaskExecution
     * (a global setting). If the first option is selected then the highest priority task is
     * executed whether it passes restrictions or not, and lower priority tasks are only used
     * if that task has no text output at all, or if mContinueToExecuteLowerPriority is true.
     * If the second option is selected then tasks that pass restrictions override higher priority
     * tasks that do not, even when the failing higher priority task has output. To change this
     * behaviour, the flag mIsLowPriority needs to be set to true. This equate to the
     * "This task can be overridden by other task restriction failures (apart from other tasks
     * with this checked)" checkbox in the Adrift game builder interface.
     */
    public boolean mIsLowPriority;
    /**
     * Prevents a particular task from changing the %score% variable more than once: if this
     * is false the score will be incremented when the task successfully executes and this
     * flag will then be set to TRUE.
     */
    public boolean mIsScored;
    /**
     * If the game designer edits a library task, the "On load, if another task exists
     * with the same key, this should replace it" checkbox becomes available. If they have
     * multiple libraries, and those libraries contain the same key (for example, both
     * libraries have a task with the key Inventory), the second library will import and
     * the key will be renamed (e.g. to Inventory1). What this flag does is allows the
     * game designer to specify that the task should override any existing tasks with the
     * same key. This is useful if they want to customise an existing library task.
     */
    public boolean mReplaceDuplicateKey;
    /**
     * Only used for General tasks. If true then it will not be possible to
     * override that task with a specific task. It is unlikely game designers
     * will need to set this to true. One example where it is used is in the
     * Take Objects (Parent Task) library task, because we want to override
     * the Take Objects from Object or the Take Objects from Location tasks
     * individually.
     */
    public boolean mPreventOverriding;
    /**
     * Is this a system task? System Tasks are just the same as any other
     * tasks except they are not triggered directly by what the player
     * types at the command line. In most cases, they are just called
     * by other task actions, or by events. However, there are other
     * ways of triggering them also. The options for triggering a System task are:
     * <p>
     * (1) Only if called by event/task - This task will never run unless explicitly called (DEFAULT).
     * (2) Immediately - This task will run at the start of the game.
     * (3) Player enters location - This task will run when the player enters a particular location.
     */
    public boolean mIsSystemTask;
    /**
     * There are three different ways that a Specific task can override a
     * General task. These are:
     * <p>
     * (1) Run before – This means the Specific task will run before the
     * general task (but after its restrictions are checked). If you select
     * this option you can select whether the parent/general task message
     * is displayed, and also whether the parent/general actions are run.
     * The parent of the task is the one we are overriding or
     * running before/after.
     * <p>
     * (2) Override – This means the Specific task completely overrides
     * the General task. Neither the General message will be displayed,
     * nor the actions run, although its restrictions must still pass.
     * <p>
     * (3) Run after – This means the Specific task will run after the
     * General task completes. The General message will be output and
     * actions run, then the Specific actions will be run and message
     * output (if either are defined). In this case both check boxes are
     * selected and can't be changed.
     * <p>
     * Note that the restrictions on the general task must all pass before
     * any of the specific tasks can override it.
     */
    public SpecificOverrideTypeEnum mSpecificOverrideType = SpecificOverrideTypeEnum.Override;
    /**
     * For Specific tasks, any references defined in the General Task are displayed
     * in the command bar with a hyperlink. By clicking on the hyperlink, the game
     * designer can pick the specific object they want to create the special case for.
     * This field is a list of such specific references.
     */
    @Nullable
    public ArrayList<MSpecific> mSpecifics = new ArrayList<>();
    /**
     * For Specific tasks, this is the key of the parent (Specific or General) task
     * that it is overriding.
     */
    @NonNull
    public String mGeneralKey = "";
    /**
     * A task can be run by calling it from another task with an "execute task"
     * action and passing more than one key to a reference, or by the player
     * specifying multiple objects, eg: "get the ball, the bat and the vase".
     * The "Aggregate output, where possible" checkbox is normally ticked (TRUE) and
     * causes each of the the text messages output by the task to be combined into
     * one. For the above example this will produce "You take the ball, the bat and
     * the vase.". If this checkbox is not ticked then a separate message is produced
     * for object, eg: "You take the ball. You take the bat. You take the vase."
     */
    public boolean mAggregateOutput = true;
    /**
     * If we have successfully matched this task against a set of unique references
     * in the last command (i.e. matchesReferences passed), then those references
     * will be stored in this member field. References are essentially placeholders
     * within the task command that can match on any object, character etc.
     */
    @NonNull
    public MReferenceList mRefs;
    /**
     * A set of references created when matchesReferences is first called, which
     * may or may not contain unique, unambiguous elements.
     */
    @Nullable
    MReferenceList mRefsWorking;
    /**
     * For Adrift 3.9 / 4 compatibility.
     * <p>
     * This field is temporarily used to hold a reversed version of this task
     * if appropriate after the constructor is called.
     */
    @Nullable
    public MTask mReversedTask = null;
    /**
     * This list is populated after a call to getAllCommandsAsPatterns().
     * After that call returns, it will contain a list of all the commands
     * in mCommands, in the form of compiled Patterns, ready for use
     * against Matchers.
     */
    @Nullable
    private ArrayList<ArrayList<Pattern>> mPatterns;
    /**
     * The message that will be displayed if all the restrictions pass.
     */
    @NonNull
    private MDescription mCompletionMessage;
    /**
     * This is a special message to be displayed, instead of the usual
     * restriction failure message, if the player entered "All" instead
     * of a specific object in the command.
     */
    @NonNull
    private MDescription mFailOverride;
    /**
     * Whenever ADRIFT finds a task that matches user input and passes its restrictions, it
     * will run that task. If that task has output text, that text will be displayed and no
     * more tasks will be checked. In some instances, the game designer might want to continue
     * to match lower priority tasks after the initial task has run. To do this, they would
     * select the "Continue executing matching lower priority tasks (multiple matching)" checkbox.
     */
    private boolean mContinueToExecuteLowerPriority;
    /**
     * If this is a System task and mRunImmediately is TRUE
     * then this task will run at the start of the game.
     */
    private boolean mRunImmediately = false;
    /**
     * If this is a System task and mLocationTrigger is not blank
     * then this task will run when the player enters the location
     * with the same key as mLocationTrigger.
     */
    @NonNull
    private String mLocationTrigger = "";
    /**
     * If this is TRUE, the task can be run any number of times. If FALSE,
     * then the task can only be run once, after which it will be considered
     * "completed" and will not run again.
     */
    private boolean mRepeatable;
    /**
     * The description of this task. Only used for game design and debugging.
     */
    @NonNull
    private String mDescription = "";
    /**
     * Determines which task should execute if more than one matches the player's
     * input.The task with the lowest number in this field has the highest priority
     * and will be executed first. The tasks in the Standard Library always have very
     * large priority numbers, while the tasks that the user creates will initially be
     * numbered in order of creation starting at 1. This means that any task the user
     * creates will execute before any Standard Library task by default.
     */
    private int mPriority;
    /**
     * Only used for General Tasks and is responsible for the auto-completion feature
     * whereby the player only has to type part of a command or object name and the
     * rest of the word will be filled in automatically. Normally a player will
     * expect common commands such as "north" or "examine" to be chosen by this
     * feature, but if the game designer creates a new command such as "exit" or
     * "nobble" then they may need to change the value of this field to stop them being
     * selected.
     */
    private int mAutoFillPriority;
    /**
     * A task will be marked as completed if: (1) it is not repeatable;
     * (2) it has been executed at least once; and (3) the unset
     * task action has not been used to clear its completed flag.
     * <p>
     * If a task has been marked as completed:
     * <p>
     * (a) If it is a general command then it will be
     * ignored when trying to match the players input to a command.
     * <p>
     * (b) If it is a specific task then it will not be able to override
     * its general task.
     * <p>
     * (c) If it is a system task triggered by entering a location, then
     * it will not be executed again when the player re-enters that
     * location.
     * <p>
     * (d) It can however still be executed using an execute-task action.
     */
    private boolean mHasCompleted;

    /**
     * Indices [0][0], [1][0] and [2][0] indicate whether we have already
     * checked if this task's restrictions include an object, character
     * or location restriction, respectively. If any of those elements are
     * TRUE then the corresponding index [0][1], [1][1] or [2][1] gives the
     * result of that check.
     */
    private boolean mRestrictionCheckCache[][] = new boolean[3][2];
    private int mLastMatchingCommandIndex;

    public MTask(MAdventure adv) {
        super(adv);
        setContinueToExecuteLowerPriority(false);
        mAutoFillPriority = 10;
        mCompletionMessage = new MDescription(adv);
        mFailOverride = new MDescription(adv);
        mRestrictions = new MRestrictionArrayList(adv);
        mActions = new MActionArrayList(adv);
        mRefs = new MReferenceList(adv);
    }

    public MTask(@NonNull MAdventure adv, @NonNull MFileOlder.V4Reader reader,
                 int iTask, double v, final int nLocs, final int iStartLocations,
                 final int iStartTask, final int iStartChar, final int iStartMaxPriority,
                 final HashMap<String, String> dictDodgyStates,
                 final HashMap<MObject, MProperty> dodgyArlStates,
                 @NonNull final String[] locNames) throws EOFException {
        // ADRIFT V3.80, 3.90 and V4 Loader
        this(adv);

        String myKey = "Task" + iTask;
        while (adv.mTasks.containsKey(myKey)) {
            myKey = incrementKey(myKey);
        }
        setKey(myKey);
        setPriority(iStartMaxPriority + iTask);
        mType = MTask.TaskTypeEnum.System;

        // =============================================================
        //                   TASK COMMANDS (V$Command)
        // -------------------------------------------------------------
        // In the box at the top, "What the user must type", you can enter
        // any number of commands. This is what the player must type in
        // the game in order for the task to work.
        //
        // You can override any of the system commands with tasks. For
        // example, if you entered "north" as the command, and you were
        // in a room which had an exit to the north, assuming all the
        // restrictions were passed, the task would be executed instead
        // of moving the Player in that direction. This is useful if you
        // want to check something before going north, or you wanted to
        // add a more descriptive message when moving the Player from
        // one room to another.
        int nCmds = cint(reader.readLine());
        if (v < 4) {
            nCmds++;
        }
        for (int i = 0; i < nCmds; i++) {
            String cmd = reader.readLine();                            // Command

            // Some games (To Hell in a Hamper) don't put spaces between
            // the command groups - fix this by allowing an optional
            // space (that way also shouldn't muck up anything that
            // genuinely doesn't need the space). We are careful with the
            // order of the replacements to avoid doubling up.
            cmd = cmd.replace("}{", "}{ }{")
                    .replace("]{", "]{ }{")
                    .replace("}[", "}{ }[")
                    .replace("][", "]{ }[");

            // Simplify Runner so it only has to deal with multiple or
            // specific refs.
            cmd = fixInitialRefs(cmd);
            if (!cmd.startsWith("#")) {
                // "As well as being typed in by the player, tasks can
                // also be called from events and tasks.  It is best
                // to set the command on these tasks so that the
                // player cannot accidentally type it. If you prefix
                // the command with the hash character (#), then it
                // will be impossible for the player to call the task
                // from Runner, as it strips off any preceding #
                // characters. For example, if you wanted a task that
                // kills off the Player that gets called from an event,
                // you could call the task "# kill player". This would
                // then only be executable by the event or a task."
                mType = General;
            }
            mCommands.add(cmd);
        }

        // =============================================================
        //                SUCCESSFUL COMPLETION MESSAGE
        // -------------------------------------------------------------
        // You need to give a reply to a successful command. Enter
        // your message in the box labelled "Message upon completion".
        // If the task moves the Player to another room, you would
        // often want to give the description of the new room, so you
        // can select this from the pull down list marked Then show
        // description for room. If you wanted any text to appear after
        // this description, you can enter this in the box
        // marked Additional Message.
        StringBuilder sbComplMsg = new StringBuilder();
        setDescription(mCommands.get(0));
        String sCompleteText = reader.readLine();                           // $CompleteText
        sbComplMsg.append(sCompleteText);
        String revMsg = reader.readLine();                                  // $ReverseMessage
        MDescription revCompletionMsg = null;
        if (revMsg.length() > 0) {
            revCompletionMsg = new MDescription(adv,
                    convertV4FuncsToV5(adv, revMsg));
        }
        String sRepeatText = reader.readLine();                             // $RepeatText
        String sAddtlMsg = reader.readLine();                               // $AdditionalMessage
        int iShowRoom = mAdv.safeInt(reader.readLine());                    // #ShowRoomDesc
        if (iShowRoom > 0) {
            if (sbComplMsg.length() > 0) {
                sbComplMsg.append("  ");
            }
            sbComplMsg.append("%DisplayLocation[Location")
                    .append(iShowRoom).append("]%");
        }
        mDisplayCompletion =
                (sAddtlMsg.length() > 0 && sbComplMsg.length() == 0) ?
                        After : Before;
        if (sAddtlMsg.length() > 0) {
            appendDoubleSpace(sbComplMsg);
            sbComplMsg.append(sAddtlMsg);
        }
        if (sbComplMsg.length() == 0) {
            mSpecificOverrideType = BeforeTextAndActions;
        }
        MDescription dComplMsg = new MDescription(adv,
                convertV4FuncsToV5(adv, sbComplMsg.toString()));
        setCompletionMessage(dComplMsg);
        setContinueToExecuteLowerPriority(false);
        setRepeatable(cbool(reader.readLine()));                            // BRepeatable
        if (!sRepeatText.equals("")) {
            getCompletionMessage().get(0).mDisplayOnce = true;
            MSingleDescription sd = new MSingleDescription(adv);
            sd.mDescription = sRepeatText;
            getCompletionMessage().add(sd);
            mRepeatable = true;
        }

        // =============================================================
        //                   V3.80 TASK ACTIONS
        // -------------------------------------------------------------
        V3_8_InitTaskInfo v380RestrInfo = new V3_8_InitTaskInfo();
        if (v < 3.9) {
            int score = cint(reader.readLine());                            // #Score
            adv.setMaxScore(adv.getMaxScore() + score);

            // Create any appropriate score change action.
            if (score != 0) {
                MAction a = new MAction(adv, iStartLocations,
                        iStartChar, iStartTask, dodgyArlStates,
                        4, score, 0, 0,
                        0, "");
                mActions.add(a);
            }

            boolean singleScore = cbool(reader.readLine());                 // BSingleScore

            // Create movement actions (we closely follow SCARE).
            //          ([6]<TASK_MOVE>Movements)
            for (int i = 0; i < 6; i++) {
                int mvar1 = cint(reader.readLine());                        // #Var1: move(nn, 0)
                int mvar2 = cint(reader.readLine());                        // #Var2: move(nn, 1)
                int mvar3 = cint(reader.readLine());                        // #Var3: movemode(nn)

                // If nothing was selected to move, skip this iteration.
                if (mvar1 == 0) {
                    continue;
                }

                // Accept only player moves into rooms. Other combinations,
                // such as move player to worn by player, are unlikely.
                // And move player to same room as player isn't useful.
                if (mvar1 == 1) {
                    if (mvar3 == 0 && mvar2 >= 2) {
                        MAction a = new MAction(adv, iStartLocations,
                                iStartChar, iStartTask, dodgyArlStates,
                                1, 0, 0, mvar2 - 2,
                                0, "");
                        mActions.add(a);
                    }
                    continue;
                }

                // Convert movement var1 into action var1.
                // Var1 is the dynamic object + 3, or 2 for
                // referenced object, or 0 for all held.
                int var1;
                switch (mvar1) {
                    case 2:
                        // Referenced object
                        var1 = 2;
                        break;
                    case 3:
                        // All held
                        var1 = 0;
                        break;
                    default:
                        // Dynamic object
                        var1 = mvar1 - 1;
                        break;
                }

                // Dissect the rest of the movement.
                MAction a = null;
                switch (mvar3) {
                    case 0:  // TO ROOM
                        // Convert movement var2 into action var2 and var3.
                        // Var2 is 0 for move to room, 6 for move to
                        // player room. Var3 is 0 for hidden, otherwise
                        // the room number plus one.
                        if (mvar2 == 0) {
                            // Hidden
                            a = new MAction(adv, iStartLocations,
                                    iStartChar, iStartTask, dodgyArlStates,
                                    0, var1, 0, 0,
                                    0, "");
                        } else if (mvar2 == 1) {
                            // Player room
                            a = new MAction(adv, iStartLocations,
                                    iStartChar, iStartTask, dodgyArlStates,
                                    0, var1, 6, 0,
                                    0, "");
                        } else {
                            // Specified room
                            a = new MAction(adv, iStartLocations,
                                    iStartChar, iStartTask, dodgyArlStates,
                                    0, var1, 0, mvar2 - 1,
                                    0, "");
                        }
                        break;

                    case 1:  // TO INSIDE
                    case 2:  // TO ONTO
                        // Convert movement var2 and var3 into action
                        // var3 and var2, a simple conversion, but
                        // check that var2 is not 'not selected' first.
                        if (mvar2 > 0) {
                            a = new MAction(adv, iStartLocations,
                                    iStartChar, iStartTask, dodgyArlStates,
                                    0, var1, mvar3 + 1, mvar2 - 1,
                                    0, "");
                        }
                        break;

                    case 3:  // TO HELD BY
                    case 4:  // TO WORN BY
                        // Convert movement var2 and var3 into action
                        // var3 and var2, in this case a simple
                        // conversion, since version 4.0 task
                        // actions are close here.
                        a = new MAction(adv, iStartLocations,
                                iStartChar, iStartTask, dodgyArlStates,
                                0, var1, mvar3 + 1, mvar2,
                                0, "");
                        break;
                }

                // If we successfully created the action, add it to this
                // task now.
                if (a != null) {
                    mActions.add(a);
                }
            }
        }

        // =============================================================
        //              REVERSE COMMANDS (V$ReverseCommand)
        // -------------------------------------------------------------
        // You can also make tasks reversible. This will clear the
        // completed status of a task, if it has been completed earlier.
        // Examples of wanting to do this could be if the task was "open
        // door", then the reverse command would be "close door". You
        // could then put a restriction on a movement from a room, to
        // only move if "open door" is complete. You could then open and
        // close the door as much as you like, but only be able to move
        // through it if it was open.
        //
        // You can have any number of commands for the reverse command,
        // much in the same way as the initial command, and wildcards
        // and advanced command construction can again be used. Note
        // that when you reverse a task, any actions that the task
        // performed will not be undone – simply the status of the task
        // will be set back to Not Completed.
        // -------------------------------------------------------------
        boolean bReversible = cbool(reader.readLine());                     // BReversible
        MStringArrayList revCmds = null;
        nCmds = cint(reader.readLine());                                    // # ReverseCommand
        if (v < 4) {
            nCmds++;
        }
        if (nCmds > 0) {
            revCmds = new MStringArrayList();
            for (int i = 0; i < nCmds; i++) {
                String revCmd = reader.readLine();                 // sReverseCommand
                revCmd = revCmd
                        .replace("}{", "}{ }{")
                        .replace("]{", "]{ }{")
                        .replace("}[", "}{ }[")
                        .replace("][", "]{ }[");

                // Simplify Runner so it only has to deal with multiple
                // or specific refs.
                revCmd = fixInitialRefs(revCmd);
                revCmds.add(revCmd);
            }
        }

        if (v < 3.9) {
            for (int i = 0; i < v380RestrInfo.wearObjs.length; i++) {
                v380RestrInfo.wearObjs[i] = cint(reader.readLine());        // #WearObj1,2
            }
            for (int i = 0; i < v380RestrInfo.holdObjs.length; i++) {
                v380RestrInfo.holdObjs[i] = cint(reader.readLine());        // #HoldObj1,2,3
            }
            v380RestrInfo.obj1 = cint(reader.readLine());                   // #Obj1
            v380RestrInfo.task = cint(reader.readLine());                   // #Task
            v380RestrInfo.taskNotDone = cbool(reader.readLine());           // BTaskNotDone
            v380RestrInfo.taskMsg = reader.readLine();                      // $TaskMsg
            v380RestrInfo.holdMsg = reader.readLine();                      // $HoldMsg
            v380RestrInfo.wearMsg = reader.readLine();                      // sWearMsg
            v380RestrInfo.companyMsg = reader.readLine();                   // $CompanyMsg
            v380RestrInfo.notInSameRoom = cbool(reader.readLine());         // BNotInSameRoom
            v380RestrInfo.NPC = cint(reader.readLine());                    // #NPC
            v380RestrInfo.obj1Msg = reader.readLine();                      // $Obj1Msg
            v380RestrInfo.obj1Room = cint(reader.readLine());               // #Obj1Room
        }

        // =============================================================
        //       WHERE THE TASK MAY BE EXECUTED (<ROOM_LIST0>Where)
        // -------------------------------------------------------------
        // Convert the "rooms where this task is executable" data into
        // location restrictions.
        //
        // Possible values for #Type are:
        //
        //   0 = None     : technically this means the task cannot be
        //                  executed anywhere (?, TODO)
        //
        //   1 = Single   : convert to a single MUST restriction
        //
        //   2 = Multiple : if up to 3 marked rooms, add each as separate
        //                  MUST restrictions.
        //
        //                  if up to 2 marked rooms away from total, add
        //                  the non-marked rooms as separate MUST NOT
        //                  restrictions.
        //
        //                  otherwise, create a room group including the
        //                  marked rooms and add that as a single MUST
        //                  restriction.
        //
        //   3 = All     :  we don't need to add any restriction as the
        //                  task is executable everywhere.
        // -------------------------------------------------------------
        MRestrictionArrayList locRests = new MRestrictionArrayList(adv);
        int iDoWhere = mAdv.safeInt(reader.readLine());                          // #Type
        if (iDoWhere == 1) {
            // SINGLE ROOM
            // Add as a separate restriction.
            locRests.add(createLocRestriction(adv,
                    "Location" + (cint(reader.readLine()) +
                            1 + iStartLocations), true));             // Room #
            locRests.mBrackSeq = "#";
        } else if (iDoWhere == 2) {
            // MULTIPLE ROOMS
            HashSet<Integer> allowHere = new HashSet<>();
            HashSet<Integer> denyHere = new HashSet<>();
            int nAllowed = 0;
            for (int i = 0; i < nLocs; i++) {
                boolean isHere = cbool(reader.readLine());                  // iIsHere
                if (isHere) {
                    allowHere.add(i);
                    nAllowed++;
                } else {
                    denyHere.add(i);
                }
            }
            switch (nAllowed) {
                case 2:
                case 3:
                    // Add the 2 or 3 rooms as separate MUST restrictions.
                    for (Integer i : allowHere) {
                        locRests.add(createLocRestriction(adv,
                                locNames[i], true));
                    }
                    locRests.mBrackSeq =
                            (nAllowed == 2) ? "(#O#)" : "(#O#O#)";
                    break;
                default:
                    if (nAllowed == nLocs - 1 ||
                            nAllowed == nLocs - 2) {
                        // We are only 1 or 2 rooms away from the total.
                        // Add those 1 or 2 rooms as separate MUST NOT
                        // restrictions.
                        for (Integer i : denyHere) {
                            locRests.add(createLocRestriction(adv,
                                    locNames[i], false));
                        }
                        locRests.mBrackSeq =
                                (nAllowed == nLocs - 1) ? "#" : "(#O#)";
                    } else {
                        // Create a room group and add it as a separate
                        // restriction.
                        MStringArrayList salHere = new MStringArrayList();
                        for (Integer i : allowHere) {
                            salHere.add(locNames[i]);
                        }
                        locRests.add(createLocRestriction(adv,
                                getRoomGroupFromList(adv, salHere,
                                        "task '" +
                                                getDescription() + "'").getKey(),
                                true));
                        locRests.mBrackSeq = "#";
                    }
                    break;
            }
        }
        mRestrictions = locRests.copy();

        if (v < 3.9) {
            // Create any appropriate player death action
            if (cbool(reader.readLine())) {                                 // BKillsPlayer
                MAction a = new MAction(adv, iStartLocations,
                        iStartChar, iStartTask, dodgyArlStates,
                        6, 2, 0, 0,
                        0, "");
                mActions.add(a);
            }
            v380RestrInfo.holdingSameRoom = cbool(reader.readLine());       // BHoldingSameRoom
        }

        // =============================================================
        //                         TASK HINT
        // -------------------------------------------------------------
        String question = reader.readLine();                                // $Question
        if (!question.equals("")) {
            MHint h = new MHint(adv, reader, question);
            h.mRestrictions = locRests;
            adv.mHints.put(h.getKey(), h);
        }

        // =============================================================
        //               OTHER RESTRICTIONS AND ACTIONS
        // -------------------------------------------------------------
        if (v < 3.9) {
            // ---------------------------------------------------------
            //       TASK RESTRICTIONS |V380_TASK:_Restrictions_|
            //                        (V3.8)
            // ---------------------------------------------------------
            v380RestrInfo.obj2 = cint(reader.readLine());                   // #Obj2
            if (v380RestrInfo.obj2 > 0) {
                v380RestrInfo.obj2Var1 = cint(reader.readLine());           // ?!#Obj2=0:#Obj2Var1
                v380RestrInfo.obj2Var2 = cint(reader.readLine());           // ?!#Obj2=0:#Obj2Var2
                v380RestrInfo.obj2Msg = reader.readLine();                  // ?!#Obj2=0:#Obj2Msg
            }

            // Create any appropriate win game action
            if (cbool(reader.readLine())) {                                 // BWinGame
                MAction a =
                        new MAction(adv, iStartLocations, iStartChar,
                                iStartTask, dodgyArlStates, 6,
                                0, 0, 0, 0, "");
                mActions.add(a);
            }

            createV380Restrictions(adv, v, iStartTask, iStartChar,
                    dictDodgyStates, dodgyArlStates, v380RestrInfo);
        } else {
            // ---------------------------------------------------------
            //      TASK RESTRICTIONS (V<TASK_RESTR>Restrictions)
            //                    (V3.9 and 4.0)
            // ---------------------------------------------------------
            int nRest = cint(reader.readLine());                            // # restrictions
            for (int i = 0; i < nRest; i++) {
                MRestriction r =
                        new MRestriction(adv, reader, v, iStartTask,
                                iStartChar, dictDodgyStates, dodgyArlStates);
                mRestrictions.add(r);
            }

            // ---------------------------------------------------------
            //           TASK ACTIONS (V<TASK_ACTION>Actions)
            //                     (V3.9 and 4.0)
            // ---------------------------------------------------------
            int nAct = cint(reader.readLine());                         // # actions
            for (int i = 0; i < nAct; i++) {
                MAction a =
                        new MAction(adv, reader, iStartLocations,
                                iStartChar, iStartTask, dodgyArlStates, v);
                mActions.add(a);
            }
        }

        // =============================================================
        //         FINALISE THE RESTRICTION BRACKET SEQUENCE
        // -------------------------------------------------------------
        StringBuilder sbBrackSeq = new StringBuilder();
        if (v < 4) {
            // For games earlier than version 4, append ANDs for
            // all restrictions not yet included in the bracket
            // sequence (note: that there might already be location
            // restrictions that have been connected in the
            // bracket sequence using ORs)
            sbBrackSeq.append(mRestrictions.mBrackSeq);
            int i = 0;
            int nRest = 0;

            // Work out how many restrictions (if any) have
            // already been added.
            while ((i = sbBrackSeq.indexOf("#", i)) != -1) {
                nRest++;
                i++;
            }

            // If we haven't added any restrictions yet, add
            // the first one.
            if (nRest == 0 && mRestrictions.size() > 0) {
                sbBrackSeq.append("#");
                nRest++;
            }

            // Now add the remaining restrictions.
            for (i = nRest; i < mRestrictions.size(); i++) {
                sbBrackSeq.append("A#");
            }
            mRestrictions.mBrackSeq = sbBrackSeq.toString();
        } else {
            sbBrackSeq.append(reader.readLine());                           // $RestrMask
            if (sbBrackSeq.length() > 0 &&
                    !mRestrictions.mBrackSeq.equals("")) {
                mRestrictions.mBrackSeq += "A";
            }
            mRestrictions.mBrackSeq += sbBrackSeq;

            // This next line added by TCC - otherwise older v4 games
            // like Escape to New York don't work properly:
            mRestrictions.mBrackSeq =
                    correctBracketSequence(mRestrictions.mBrackSeq);
        }
        mRestrictions.mBrackSeq =
                mRestrictions.mBrackSeq
                        .replace("[", "((")
                        .replace("]", "))");
        // =============================================================

        if (v >= 3.9) {
            // ---------------------------------------------------------
            //               RESOURCES (<RESOURCE>Res)
            // ---------------------------------------------------------
            loadResource(adv, reader, v, dComplMsg.get(0));
        }

        // Support for older v3.9 and v4 games with reversible commands
        if (bReversible && revCmds != null) {
            // N.B. "Reversible tasks share the same restrictions
            // as the forward part of the task."
            mReversedTask = clone();
            mReversedTask.mCommands = revCmds;
            if (revCompletionMsg != null) {
                mReversedTask.setCompletionMessage(revCompletionMsg);
            }
            mReversedTask.setKey("Reverse" + getKey());
            mReversedTask.setDescription(mReversedTask.mCommands.get(0));

            // Ensure that successful execution of the task unsets
            // its reversed task and vice-versa
            MAction act = new MAction(adv);
            act.mType = SetTasks;
            act.mSetTask = Unset;
            act.mKey1 = mReversedTask.getKey();
            mActions.add(act);

            act = new MAction(adv);
            act.mType = SetTasks;
            act.mSetTask = Unset;
            act.mKey1 = getKey();
            mReversedTask.mActions = new MActionArrayList(adv);
            mReversedTask.mActions.add(act);

            MRestriction rest = new MRestriction(adv);
            rest.mKey1 = getKey();
            rest.mType = MRestriction.RestrictionTypeEnum.Task;
            rest.mMust = Must;
            if (!sRepeatText.equals("") && getRepeatable()) {
                // "If the task is repeatable and reversible, the Message if task
                // tried again will also be displayed if the player types the
                // command to reverse the task when it has not been completed
                // (or it has been reversed)."
                rest.mMessage = new MDescription(adv, sRepeatText);
            }
            mReversedTask.mRestrictions.add(rest);
            if (mReversedTask.mRestrictions.mBrackSeq.length() == 0) {
                mReversedTask.mRestrictions.mBrackSeq = "#";
            } else {
                mReversedTask.mRestrictions.mBrackSeq += "A#";
            }
        }
    }

    public MTask(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                 boolean bLibrary, boolean bAddDuplicateKeys, double dFileVersion,
                 @NonNull MStringHashMap htblDuplicateKeyMapping) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Task");

        // Properties that are only valid for General tasks:
        MStringArrayList commands = new MStringArrayList();

        // Properties that are only valid for Specific tasks:
        String sGeneralTask = "";
        ArrayList<MTask.MSpecific> specifics = new ArrayList<>();
        Boolean bExecuteParentActions = null;
        MTask.SpecificOverrideTypeEnum specificOverrideType = null;

        // Properties that are only valid for System tasks:
        Boolean bRunImmediately = null;
        String sLocationTrigger = null;

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

                    case "AutoFillPriority":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setAutoFillPriority(cint(s));
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Type":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mType = MTask.TaskTypeEnum.valueOf(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "CompletionMessage":
                        setCompletionMessage(new MDescription(adv, xpp,
                                dFileVersion, "CompletionMessage"));
                        break;

                    case "Command":
                        // Simplify Runner so it only has to deal with multiple, or specific refs
                        commands.add(fixInitialRefs(xpp.nextText()));
                        break;

                    case "GeneralTask":
                        sGeneralTask = xpp.nextText();
                        break;

                    case "Specific":
                        specifics.add(new MTask.MSpecific(xpp));
                        break;

                    case "ExecuteParentActions":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                // Old checkbox method
                                bExecuteParentActions = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "SpecificOverrideType":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                specificOverrideType = MTask.SpecificOverrideTypeEnum.valueOf(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "RunImmediately":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                bRunImmediately = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "LocationTrigger":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                sLocationTrigger = s;
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Description":
                        setDescription(xpp.nextText());
                        break;

                    case "Repeatable":
                        setRepeatable(getBool(xpp.nextText()));
                        break;

                    case "Aggregate":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mAggregateOutput = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Continue":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                switch (s) {
                                    case "ContinueNever":
                                    case "ContinueOnFail":
                                    case "ContinueOnNoOutput":
                                        setContinueToExecuteLowerPriority(false);
                                        break;
                                    case "ContinueAlways":
                                        setContinueToExecuteLowerPriority(true);
                                        break;
                                }
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "LowPriority":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mIsLowPriority = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Restrictions":
                        mRestrictions = new MRestrictionArrayList(adv, xpp, dFileVersion);
                        break;

                    case "Actions":
                        mActions = new MActionArrayList(adv, xpp, dFileVersion);
                        break;

                    case "FailOverride":
                        setFailOverride(new MDescription(adv, xpp, dFileVersion, "FailOverride"));
                        break;

                    case "MessageBeforeOrAfter":
                        mDisplayCompletion = Before;
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mDisplayCompletion = MTask.BeforeAfterEnum.valueOf(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "PreventOverriding":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mPreventOverriding = getBool(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Task");

        if (!header.finalise(this, adv.mTasks,
                bLibrary, bAddDuplicateKeys, htblDuplicateKeyMapping)) {
            throw new Exception();
        }

        // Set the remaining properties that are relevant to the task's type
        // (and ignore anything else)
        switch (mType) {
            case General:
                mCommands = commands;
                break;

            case Specific: {
                mGeneralKey = sGeneralTask;
                mSpecifics = specifics;
                if (bExecuteParentActions != null) {
                    String msgCompletion = getCompletionMessage().toString(true);
                    if (bExecuteParentActions) {
                        mSpecificOverrideType = (msgCompletion.equals("")) ?
                                BeforeTextAndActions : BeforeActionsOnly;
                    } else {
                        mSpecificOverrideType = (msgCompletion.equals("")) ?
                                BeforeTextOnly :
                                MTask.SpecificOverrideTypeEnum.Override;
                    }
                }
                if (specificOverrideType != null) {
                    mSpecificOverrideType = specificOverrideType;
                }
                break;
            }

            case System:
                if (bRunImmediately != null) {
                    setRunImmediately(bRunImmediately);
                }
                if (sLocationTrigger != null) {
                    setLocationTrigger(sLocationTrigger);
                }
                break;
        }
    }

    void set(@NonNull SetTasksEnum setType, @NonNull String params,
             @NonNull String to, int from, boolean calledFromEvent,
             EnumSet<ExecutionStatus> curStatus) throws InterruptedException {
        // The task actions allow you to either execute or unset another task. This
        // task can be of any type, general, specific or system. Unsetting a task
        // simply clears the flag that indicates that the task has been run at least
        // once. This allows a non-repeatable task to be run again.
        //
        // When an action executes a task, this new task acts like a subroutine,
        // checking its restrictions are valid, displaying its own text box and
        // running its own actions before returning to complete the remaining
        // actions in the first task.
        int first = 1;
        int last = 1;

        if (!to.equals("")) {
            first = from;
            last = mAdv.safeInt(to);
        }

        for (int i = first; i <= last; i++) {
            if (setType == Execute) {
                // -------------------
                // EXECUTE THE TASK
                // -------------------
                mAdv.mView.debugPrint(Task, getKey(), High,
                        "Executing task '" + getDescription() + "'.");

                // Store the existing refs
                MReferenceList refsExisting = mAdv.mReferences;

                if (!params.equals("")) {
                    // If the general task to be executed uses references
                    // such as %character%, %object%, %number% etc., then a
                    // button marked "params" appears. Pressing this button
                    // will open a separate dialog box for each parameter, one
                    // at a time in the order they appear . This can pass on the
                    // referenced item from the first task, specify a particular
                    // item to use, or use a formulae to calculate the value to
                    // be passed to the sub-task.

                    // Tasks execute multiple times if parameter is a group
                    //
                    // If you use a group as a parameter, or any property that
                    // has the IconGroup.jpg icon such as %Player%.Held,
                    // %Player%.Worn, %object%.Contents etc, then the task is
                    // executed once for each item in that group.
                    //
                    // If the task has multiple parameters then the task
                    // will run once for every possible combination of those parameters.
                    //
                    // For example, say you are executing a general task
                    // that has the references %object% %character% %location%
                    // and you pass the parameters: Player.Held, Player.Location.Characters,
                    // Group1.Outdoor
                    //
                    // If Player is holding 5 objects, there are 4 characters at
                    // players location, and Group1 contains 10 locations, then
                    // the task will be run 5*4*10 = 200 times!
                    //
                    // Use Player.Held not Player.Held.List, we want the keys of
                    // the objects not a list of their names.
                    // "Outdoor" in Group1.Outdoor is a property added to
                    // locations because it is not possible to use the group
                    // key by itself as a parameter.
                    // The rightmost reference will change every time the task
                    // runs, when all of it's items have been exhausted then
                    // the next reference will change to it's next item. The
                    // leftmost reference will remain the same until all
                    // combinations of the other references are run and then
                    // it will change to it's next item.

                    // Rewrite the references based on our parameters
                    String[] splitParams = params.split("\\|");
                    int iNewRef = -1;
                    MReferenceList newRefs = new MReferenceList(mAdv, splitParams.length);
                    for (String param : splitParams) {
                        iNewRef++;

                        // Find each ref in the new task that
                        // our parameter corresponds to
                        boolean foundMatchingRef = false;
                        int iOldRef = -1;
                        for (String ref : getReferences()) {
                            iOldRef++;
                            // Again, we may be looking outside NewRefs
                            // if we're looking at subtask with different refs... :-/
                            if (param.equals(ref) &&
                                    iOldRef < mAdv.mReferences.size()) {
                                // Ok, found same ref, so we just pass the ref thru
                                newRefs.set(iNewRef, mAdv.mReferences.get(iOldRef));
                                foundMatchingRef = true;
                            }
                        }

                        if (!foundMatchingRef) {
                            // Determine the type of reference
                            // Need to work this out on our own...
                            MReference.ReferencesType refType = null;
                            if (param.contains(".")) {
                                // Hmm, let's see if this is an OO function
                                for (Map.Entry<String, MProperty> propEntry :
                                        mAdv.mAllProperties.entrySet()) {
                                    String propKey = propEntry.getKey();
                                    if (param.endsWith("." + propKey)) {
                                        MProperty prop = propEntry.getValue();
                                        switch (prop.getType()) {
                                            case CharacterKey:
                                                refType = MReference.ReferencesType.Character;
                                                break;
                                            case Integer:
                                                refType = MReference.ReferencesType.Number;
                                                break;
                                            case LocationGroupKey:
                                                refType = MReference.ReferencesType.Text;
                                                break;
                                            case LocationKey:
                                                refType = MReference.ReferencesType.Location;
                                                break;
                                            case ObjectKey:
                                                refType = MReference.ReferencesType.Object;
                                                break;
                                            case SelectionOnly:
                                                switch (prop.getPropertyOf()) {
                                                    case AnyItem:
                                                        refType = MReference.ReferencesType.Item;
                                                        break;
                                                    case Characters:
                                                        refType = MReference.ReferencesType.Character;
                                                        break;
                                                    case Locations:
                                                        refType = MReference.ReferencesType.Location;
                                                        break;
                                                    case Objects:
                                                        refType = MReference.ReferencesType.Object;
                                                        break;
                                                }
                                                break;
                                            case StateList:
                                                refType = MReference.ReferencesType.Text;
                                                break;
                                            case Text:
                                                refType = MReference.ReferencesType.Text;
                                                break;
                                            case ValueList:
                                                refType = MReference.ReferencesType.Number;
                                                break;
                                        }
                                        break;
                                    }
                                }
                                if (refType == null) {
                                    if (param.endsWith(".Worn")) {
                                        refType = MReference.ReferencesType.Object;
                                    } else if (param.endsWith(".List")) {
                                        mAdv.mView.TODO("ExecuteSingleAction: SetTasks: List");
                                    } else if (param.endsWith(".Count")) {
                                        refType = MReference.ReferencesType.Number;
                                    } else if (param.endsWith(".Exits")) {
                                        refType = MReference.ReferencesType.Direction;
                                    }
                                }
                            }
                            if (refType == null) {
                                // Gotta guess the type of ref...
                                switch (left(param, 6).toLowerCase()) {
                                    case "%convc":
                                        refType = MReference.ReferencesType.Character;
                                        break;
                                    case "%paren":
                                        refType = MReference.ReferencesType.Object;
                                        break;
                                    case "%text%":
                                        refType = MReference.ReferencesType.Text;
                                        break;
                                    case "%loop%":
                                    case "%numbe":
                                        refType = MReference.ReferencesType.Number;
                                        break;
                                    default:
                                        if (isNumeric(left(param, 6).toLowerCase())) {
                                            refType = MReference.ReferencesType.Number;
                                        } else {
                                            refType = MReference.ReferencesType.Object;
                                        }
                                        break;
                                }
                            }

                            // Now work out, e.g. %ParentOf[%objects%]% ...
                            MReference userDefinedRef = new MReference(refType);
                            String funcRef = mAdv.evalFuncs(param, mAdv.mReferences);
                            if (funcRef.toLowerCase().equals("%loop%")) {
                                funcRef = String.valueOf(i);
                            }

                            if (!funcRef.contains("***")) {
                                ArrayList<String> listRefs = new ArrayList<>();
                                if (funcRef.contains("|")) {
                                    listRefs.addAll(Arrays.asList(funcRef.split("\\|")));
                                } else {
                                    listRefs.add(funcRef);
                                }

                                for (String ref : listRefs) {
                                    MReference.MReferenceItem itm = new MReference.MReferenceItem();
                                    if (ref.equals("nothing")) {
                                        // List function
                                        ref = null;
                                    }
                                    itm.mMatchingKeys.add(ref);
                                    userDefinedRef.mItems.add(itm);
                                }
                                if (param.startsWith("%ParentOf")) {
                                    userDefinedRef.mType = MReference.ReferencesType.Object;
                                }
                            } else {
                                mAdv.mView.debugPrint(Task, "", High,
                                        "Error calculating parameter " + param);
                            }
                            newRefs.set(iNewRef, userDefinedRef);
                        }
                    }
                    mAdv.mReferences = newRefs;
                    mAdv.mReferences.printToDebug();
                } else {
                    // Explicitly clear the refs for this task being called
                    mAdv.mReferences = null;
                }

                // was True in ChildTask.  But it's not a child, it's a separate task call...
                // was calledFromEvent in second param - but think this should be set if calling from task
                // Re the above, a reason why would be good.  Means later we don't do the continue? check
                EnumSet<ExecutionStatus> childStatus = EnumSet.noneOf(ExecutionStatus.class);

                // Did have EvaluateResponses = True, but we need to evaluate at the end in
                // case we are inserting responses before ones in actions
                MTask tas = mAdv.mTasks.get(getKey());
                tas.attemptToExecute(calledFromEvent, true, childStatus,
                        false, false);
                if (childStatus.contains(HasOutput)) {
                    curStatus.add(HasOutput);
                }
                mAdv.mReferences = refsExisting;
            } else {
                // UNSET THE TASK
                if (getCompleted()) {
                    mAdv.mView.debugPrint(Task, getKey(), High,
                            "Task '" + getDescription() + "' being uncompleted.");

                    // Check any walks/events to see if anything triggers on this task uncompleting
                    /// TCC: According to ADRIFT 5 manual, only one walk should be running per
                    // character at any given time ("A character can have several walks, but
                    // will follow one walk at a time..") So if this causes a walk to resume
                    // or start we need to pause any other running walks. Original code
                    // didn't do that which seems to be a bug. N.B. if the task uncompleting
                    // triggers more than one walk to start, we only start the last one and
                    // ignore the rest.
                    for (MCharacter ch : mAdv.mCharacters.values()) {
                        MWalk started = null;
                        for (MWalk w : ch.mWalks) {
                            for (MEventOrWalkControl ctrl : w.mWalkControls) {
                                if (ctrl.mCompleteOrNot == UnCompletion &&
                                        ctrl.mTaskKey.equals(getKey())) {
                                    switch (ctrl.mControl) {
                                        case Resume:
                                            if (w.mStatus == Paused) {
                                                w.resume();
                                                started = w;
                                            }
                                            break;
                                        case Start:
                                            if (w.mStatus != Running) {
                                                w.start();
                                                started = w;
                                            }
                                            break;
                                        case Stop:
                                            if (w.mStatus == Running) {
                                                w.stop();
                                            }
                                            break;
                                        case Suspend:
                                            if (w.mStatus == Running) {
                                                w.pause();
                                            }
                                            break;
                                    }
                                }
                            }
                        }

                        if (started != null) {
                            // OK, we just started / resumed a walk for this
                            // character, so make sure no other walks are active
                            for (MWalk w : ch.mWalks) {
                                if (w != started) {
                                    if (w.mStatus == Running) {
                                        w.pause();
                                    }
                                }
                            }
                        }
                    }
                    for (MEvent ev : mAdv.mEvents.values()) {
                        for (MEventOrWalkControl ctrl : ev.mEventControls) {
                            if (ctrl.mCompleteOrNot == UnCompletion &&
                                    ctrl.mTaskKey.equals(getKey())) {
                                switch (ctrl.mControl) {
                                    case Resume:
                                        if (ev.mStatus == MEvent.StatusEnum.Paused) {
                                            ev.resume();
                                        }
                                        break;
                                    case Start:
                                        if (ev.mStatus != MEvent.StatusEnum.Running) {
                                            ev.start();
                                        }
                                        break;
                                    case Stop:
                                        if (ev.mStatus == MEvent.StatusEnum.Running) {
                                            ev.stop();
                                        }
                                        break;
                                    case Suspend:
                                        if (ev.mStatus == MEvent.StatusEnum.Running) {
                                            ev.pause();
                                        }
                                        break;
                                }
                            }
                        }
                    }
                }
                setCompleted(false);
            }
        }
    }

    private void createV380Restrictions(@NonNull MAdventure adv, double v,
                                        final int iStartTask, final int iStartChar,
                                        final HashMap<String, String> dictDodgyStates,
                                        final HashMap<MObject, MProperty> dodgyArlStates,
                                        @NonNull V3_8_InitTaskInfo v3_8Info) {
        // Create restrictions for objects not held or absent.
        for (int i = 0; i < v3_8Info.holdObjs.length; i++) {
            int holdobj = v3_8Info.holdObjs[i];
            if (holdobj > 0) {
                // Create version 4.0 task restriction to check for either the
                // referenced object or a dynamic object being either held or in the
                // same room (visible to player).
                int var1 = (holdobj == 1) ? 2 : holdobj + 1;
                int var2 = v3_8Info.holdingSameRoom ? 1 : 3;
                MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                        dictDodgyStates, dodgyArlStates,
                        0, var1, var2, 0, "", v3_8Info.holdMsg);
                mRestrictions.add(r);
            }
        }

        // Create any task state restriction.
        if (v3_8Info.task > 0) {
            // Create version 4.0 restriction to check task state.
            int var2 = v3_8Info.taskNotDone ? 1 : 0;
            MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                    dictDodgyStates, dodgyArlStates,
                    2, v3_8Info.task, var2, 0, "", v3_8Info.taskMsg);
            mRestrictions.add(r);
        }

        // Create any object not worn restrictions.
        for (int i = 0; i < v3_8Info.wearObjs.length; i++) {
            int wearobj = v3_8Info.wearObjs[i];
            if (wearobj > 0) {
                // Create version 4.0 restrictions for something or
                // nothing worn by player.
                if (wearobj == 1) {
                    // anything
                    MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                            dictDodgyStates, dodgyArlStates,
                            0, 1, 2, 0, "", v3_8Info.wearMsg);
                    mRestrictions.add(r);
                    continue;
                } else if (wearobj == 2) {
                    // nothing
                    MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                            dictDodgyStates, dodgyArlStates,
                            0, 0, 2, 0, "", v3_8Info.wearMsg);
                    mRestrictions.add(r);
                    continue;
                }

                // Create version 4.0 restriction for object worn by player.

                // Convert wearobj from worn index to object index.
                wearobj -= 2;

                int object;
                for (object = 0; object < mAdv.mObjects.size() && wearobj > 0; object++) {
                    MObject obj = mAdv.mObjects.get("Object" + (object + 1));
                    if (!obj.isStatic()) {
                        if (obj.isWearable()) {
                            wearobj--;
                        }
                    }
                }

                int obj_index = object - 1;
                int dynamic = 0;
                for (object = 0; object <= obj_index; object++) {
                    MObject obj = mAdv.mObjects.get("Object" + (object + 1));
                    if (!obj.isStatic()) {
                        dynamic++;
                    }
                }
                dynamic--;

                MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                        dictDodgyStates, dodgyArlStates,
                        0, dynamic + 3, 2, 0, "", v3_8Info.wearMsg);
                mRestrictions.add(r);
            }
        }

        // Check for presence/absence of NPCs restriction.
        checkNPCs:
        if (v3_8Info.NPC > 0) {
            int var2;

            if (v3_8Info.NPC == 1) {
                // Create restriction to look for alone, or not.
                var2 = v3_8Info.notInSameRoom ? 3 : 2;
                MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                        dictDodgyStates, dodgyArlStates,
                        3, 0, var2, 0, "", v3_8Info.companyMsg);
                mRestrictions.add(r);
                break checkNPCs;
            }

            // Create restriction to look for company.
            var2 = v3_8Info.notInSameRoom ? 1 : 0;
            MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                    dictDodgyStates, dodgyArlStates,
                    3, 0, var2, v3_8Info.NPC, "", v3_8Info.companyMsg);
            mRestrictions.add(r);
        }

        // Create any object location restriction.
        if (v3_8Info.obj1 > 0) {
            // Create version 4.0 restriction to check object in room.
            MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                    dictDodgyStates, dodgyArlStates,
                    0, v3_8Info.obj1 + 1, 0, v3_8Info.obj1Room, "", v3_8Info.obj1Msg);
            mRestrictions.add(r);
        }

        // And finally, any object state restriction.
        checkStates:
        if (v3_8Info.obj2 > 0 && v3_8Info.obj2Var1 != 0) {
            int object, dynamic, var2, var3;
            int ivar1 = v3_8Info.obj2Var1;
            int ivar2 = v3_8Info.obj2Var2;
            int obj = v3_8Info.obj2;

            // Look for opened/closed restrictions, convert and return.
            if (ivar1 == 3 || ivar1 == 4) {
                int stateful;

                // Convert obj from object to openable (stateful) index.
                stateful = 0;
                for (object = 0; object <= obj - 1; object++) {
                    MObject ob = mAdv.mObjects.get("Object" + (object + 1));
                    if (ob.isOpenable()) {
                        stateful++;
                    }
                }
                stateful--;

                // Create a version 4.0 restriction that checks that an object's state
                // is open (var2 = 0) or closed (var2 = 1).
                var2 = (ivar1 == 3) ? 0 : 1;
                MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                        dictDodgyStates, dodgyArlStates,
                        1, stateful + 1, var2, 0, "", v3_8Info.obj2Msg);
                mRestrictions.add(r);
                break checkStates;
            }

            // Convert obj from object to dynamic index.
            dynamic = 0;
            for (object = 0; object <= obj - 1; object++) {
                MObject ob = mAdv.mObjects.get("Object" + (object + 1));
                if (!ob.isStatic()) {
                    dynamic++;
                }
            }
            dynamic--;

            // Create version 4.0 object location restrictions for the rest.
            switch (ivar1) {
                case 1:
                    // Inside
                    var2 = 4;
                    var3 = ivar2;
                    break;
                case 2:
                    // On
                    var2 = 5;
                    var3 = ivar2;
                    break;
                case 5:
                    // Held by
                    var2 = 1;
                    var3 = ivar2 + 1;
                    break;
                case 6:
                    // Worn by
                    var2 = 2;
                    var3 = ivar2 + 1;
                    break;
                default:
                    // ignore
                    // sc_fatal ("parse_fixup_v380_objstate_restr: invalid ivar1, %ld\n", ivar1);
                    break checkStates;
            }
            MRestriction r = new MRestriction(adv, v, iStartTask, iStartChar,
                    dictDodgyStates, dodgyArlStates,
                    0, dynamic + 3, var2, var3, "", v3_8Info.obj2Msg);
            mRestrictions.add(r);
        }
    }

    @NonNull
    private String getAmbWord(@NonNull String input,
                              @NonNull MStringArrayList keys,
                              @NonNull MReference.ReferencesType keyType) {
        // Work out a common word that's in all the object names, and is also in the input
        MCharacter ch;
        MLocation loc;
        MObject ob;

        nextWord:
        for (String inputWord : input.split(" ")) {
            nextItem:
            for (String key : keys) {
                switch (keyType) {
                    case Object:
                        if ((ob = mAdv.mObjects.get(key)) != null) {
                            for (String name : ob.getNames()) {
                                if (inputWord.equals(name)) {
                                    continue nextItem;
                                }
                            }
                        }
                        break;

                    case Character:
                        if ((ch = mAdv.mCharacters.get(key)) != null) {
                            if (inputWord.equals(ch.getProperName())) {
                                continue nextItem;
                            }
                            for (String desc : ch.mDescriptors) {
                                if (inputWord.equals(desc)) {
                                    continue nextItem;
                                }
                            }
                        }
                        break;

                    case Location:
                        if ((loc = mAdv.mLocations.get(key)) != null) {
                            for (String desc : loc.getShortDescription().toString().toLowerCase().split(" ")) {
                                if (inputWord.equals(desc)) {
                                    continue nextItem;
                                }
                            }
                        }
                        break;

                }
                continue nextWord;
            }
            return inputWord;
        }
        return "";
    }

    boolean displayAmbiguityQuestion(@NonNull String input) {
        // returns true if we are waiting for a response, false otherwise
        if (mRefsWorking == null) {
            // We don't have any references with which
            // to construct the ambiguity question, so
            // don't do anything.
            return true;
        }

        for (MReference ref : mRefsWorking) {
            //GLKLogger.debug("Reference " + iRef);
            //GLKLogger.debug("Number of Items in this Reference: " + NewReferences[iRef].Items.size());
            MReference.ReferencesType refType = ref.mType;
            for (MReference.MReferenceItem itm : ref.mItems) {
                int nPossible = itm.mMatchingKeys.size();
                if (nPossible > 1) {
                    // This item is ambiguous - it has more than
                    // one possible value. Try to resolve the
                    // ambiguity by asking the player.
                    switch (refType) {
                        case Object: {
                            // Collect all the referenced object keys
                            // Can the player see any of them?
                            MObjectHashMap obs = new MObjectHashMap(mAdv);
                            boolean canSeeAny = false;
                            for (String obKey : itm.mMatchingKeys) {
                                obs.put(obKey, mAdv.mObjects.get(obKey));
                                if (!canSeeAny && mAdv.getPlayer().canSeeOb(obKey)) {
                                    canSeeAny = true;
                                }
                            }

                            if (!canSeeAny) {
                                // Player can't see any of the referenced objects
                                // (Want to try to move this into the library at some point,
                                // as we _may_ want to resolve ambiguous items that aren't
                                // visible to the player)
                                boolean isAnyPlural = false;
                                for (MObject ob : obs.values()) {
                                    if (ob.isPlural()) {
                                        isAnyPlural = true;
                                        break;
                                    }
                                }
                                String amb = getAmbWord(input, itm.mMatchingKeys, refType);
                                if (isAnyPlural) {
                                    amb = guessPluralNoun(amb);
                                }
                                mAdv.mView.displayText(mAdv, "You can't see any " +
                                        amb + "!" + "\n");
                                return false;
                            } else {
                                // Player can see at least one of the referenced objects,
                                // so ask which one they mean
                                mAdv.mView.displayText(mAdv, "Which " +
                                        getAmbWord(input, itm.mMatchingKeys, refType) + "?");
                                StringBuilder sb = new StringBuilder(obs.toList("or"));
                                sb.append(".\n");
                                toProper(sb);
                                mAdv.mView.displayText(mAdv, sb.toString());
                                return true;
                            }
                        }

                        case Character: {
                            // Collect all the referenced character keys
                            // Can the player see any of them?
                            MCharacterHashMap chars = new MCharacterHashMap(mAdv);
                            boolean canSeeAny = false;
                            for (String chKey : itm.mMatchingKeys) {
                                chars.put(chKey, mAdv.mCharacters.get(chKey));
                                if (!canSeeAny && mAdv.getPlayer().canSeeChar(chKey)) {
                                    canSeeAny = true;
                                }
                            }

                            if (!canSeeAny) {
                                // Player can't see any of the referenced characters
                                mAdv.mView.displayText(mAdv, "You can't see any " +
                                        getAmbWord(input, itm.mMatchingKeys, refType) + "!" + "\n");
                                return false;
                            } else {
                                // Player can see at least one of the referenced characters,
                                // so ask which one they mean
                                mAdv.mView.displayText(mAdv, "Which " +
                                        getAmbWord(input, itm.mMatchingKeys, refType) + "?");
                                StringBuilder sb = new StringBuilder(chars.toList("or"));
                                toProper(sb);
                                sb.append(".\n");
                                mAdv.mView.displayText(mAdv, sb.toString());
                                return true;
                            }
                        }

                        case Location:
                            // Collect the referenced location keys
                            MLocationHashMap locs = new MLocationHashMap(mAdv);
                            for (String locKey : itm.mMatchingKeys) {
                                locs.put(locKey, mAdv.mLocations.get(locKey));
                            }

                            // Ask which location the player means
                            mAdv.mView.displayText(mAdv, "Which " +
                                    getAmbWord(input, itm.mMatchingKeys, refType) + "?");
                            StringBuilder sb = new StringBuilder(locs.toList("or"));
                            toProper(sb);
                            sb.append(".\n");
                            mAdv.mView.displayText(mAdv, sb.toString());
                            return true;

                        default:
                            GLKLogger.error("Unable to disambiguate reference types " + refType);
                            return true;
                    }
                } else if (nPossible == 0) {
                    // This item has no possible values, so we can't proceed.
                    mAdv.mView.displayText(mAdv, "Sorry, that does not clarify the ambiguity." + "\n");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Attempt to find a unique set of possible items/values, that pass the restrictions of this
     * task, for a given list of references.
     *
     * @param refs - the references to reduce (up to 2, anything more is ignored).
     * @return a new list of references, with the same or less possibilities as 'mRefsWorking'.
     */
    @NonNull
    private MReferenceList refineRefsUsingScopes(@NonNull final MReferenceList refs) {
        // The player may have typed an ambiguous name that matches several items in
        // the game, for example "pen" could match "red pen", "blue pen" or
        // "green pen" if these all exist. To decide which one the player meant,
        // ADRIFT uses a "scope" system to help choose between them.
        //
        // The first thing scope does is to check the items against the
        // restrictions of the general task, if only one of them passes
        // then it is assumed to be the one the player meant.
        // (SCOPE: APPLICABLE)
        //
        // If more than one passes restrictions then scope checks to see
        // if one of those is currently visible to the player and chooses it.
        // (SCOPE: VISIBLE)
        //
        // If none of them are currently visible, then scope checks to see if
        // one of the items has ever been seen by the player.
        // (SCOPE: SEEN)
        //
        // If more than one item passes these scope checks, then the player is
        // asked a disambiguation question such as "Do you mean the red pen, the
        // blue pen or the green pen?" to determine which item they meant.
        int lenRefsWorking = refs.size();
        if (lenRefsWorking == 0) {
            // No references, do nothing
            return refs;
        }

        // ---------------------------------------------------
        //                SCOPE: APPLICABLE
        //
        // For each reference, build the set of all items
        // and associated possible values that pass this
        // task's restrictions.
        //
        // "The first thing scope does is to check the
        // items against the restrictions of the general
        // task, if only one of them passes then it is
        // assumed to be the one the player meant."
        // ---------------------------------------------------
        mAdv.mView.debugPrint(Task, getKey(),
                High, "Checking scope: Applicable");

        // Use a backup of the task's working references for this operation
        MReferenceList tmpRefs = refs.copyShallow();
        ArrayList<String>[] lAdded = new ArrayList[lenRefsWorking];
        for (int i = 0; i < tmpRefs.size(); i++) {
            lAdded[i] = new ArrayList<>();
        }

        // We have to try every combination of references against all
        // the others to see if it is a successful combination
        if (refs.get(0) != null) {
            for (MReference.MReferenceItem itm0 : refs.get(0).mItems) {
                boolean addedItm0 = false;
                MReferenceList testRefs = new MReferenceList(mAdv);
                testRefs.add(new MReference(refs.get(0)));
                MReference.MReferenceItem itmOut0 = new MReference.MReferenceItem();
                itmOut0.mCommandReference = itm0.mCommandReference;

                for (String itmKey0 : itm0.mMatchingKeys) {
                    testRefs.get(0).mItems.clear();
                    MReference.MReferenceItem itmSingle0 = new MReference.MReferenceItem();
                    itmSingle0.mMatchingKeys.add(itmKey0);
                    testRefs.get(0).mItems.add(itmSingle0);

                    if (lenRefsWorking > 1 && refs.get(1) != null) {
                        for (MReference.MReferenceItem itm1 : refs.get(1).mItems) {
                            boolean addedItm1 = false;
                            testRefs.add(new MReference(refs.get(1)));
                            MReference.MReferenceItem itmOut1 = new MReference.MReferenceItem();
                            itmOut1.mCommandReference = itm1.mCommandReference;

                            for (String itmKey1 : itm1.mMatchingKeys) {
                                testRefs.get(1).mItems.clear();
                                MReference.MReferenceItem itmSingle1 = new MReference.MReferenceItem();
                                itmSingle1.mMatchingKeys.add(itmKey1);
                                testRefs.get(1).mItems.add(itmSingle1);

                                if (mRestrictions.passes(testRefs)) {
                                    if (!addedItm0) {
                                        tmpRefs.get(0).mItems.add(itmOut0);
                                        addedItm0 = true;
                                    }
                                    if (!addedItm1) {
                                        tmpRefs.get(1).mItems.add(itmOut1);
                                        addedItm1 = true;
                                    }
                                    if (!lAdded[0].contains(itmKey0)) {
                                        itmOut0.mMatchingKeys.add(itmKey0);
                                        lAdded[0].add(itmKey0);
                                    }
                                    if (!lAdded[1].contains(itmKey1)) {
                                        itmOut1.mMatchingKeys.add(itmKey1);
                                        lAdded[1].add(itmKey1);
                                    }
                                }
                            }
                        }
                    } else {
                        if (mRestrictions.passes(testRefs)) {
                            if (!addedItm0 && !lAdded[0].contains(itmKey0)) {
                                tmpRefs.get(0).mItems.add(itmOut0);
                                addedItm0 = true;
                                lAdded[0].add(itmKey0);
                            }
                            itmOut0.mMatchingKeys.add(itmKey0);
                        }
                    }
                }
            }
        }

        if (tmpRefs.pruneImpossibleItems(mRefs)) {
            // ---------------------------------------------------
            //                SCOPE: VISIBLE
            //
            // Remove any item possibilities referring to objects or
            // characters that are not visible to the player.
            //
            // "If more than one passes restrictions then scope
            // checks to see if one of those is currently visible
            // to the player and chooses it."
            // ---------------------------------------------------
            mAdv.mView.debugPrint(Task, getKey(),
                    High, "Checking scope: Visible");

            MObject ob;
            MCharacter ch;
            String playerKey = mAdv.getPlayer().getKey();
            for (MReference nr : tmpRefs) {
                if (nr != null) {
                    for (MReference.MReferenceItem itm : nr.mItems) {
                        for (int i = itm.mMatchingKeys.size() - 1; i >= 0; i--) {
                            String itmKey = itm.mMatchingKeys.get(i);
                            switch (nr.mType) {
                                case Object:
                                    if ((ob = mAdv.mObjects.get(itmKey)) != null) {
                                        if (!ob.isVisibleTo(playerKey)) {
                                            itm.mMatchingKeys.remove(i);
                                        }
                                    }
                                    break;
                                case Character:
                                    if ((ch = mAdv.mCharacters.get(itmKey)) != null) {
                                        if (!ch.canSeeChar(playerKey)) {
                                            itm.mMatchingKeys.remove(i);
                                        }
                                    }
                                    break;
                                case Direction:
                                case Number:
                                case Text:
                                    // Don't think these are applicable
                                    break;
                                case Location:
                                    // Doesn't make sense to restrict by visible for Location
                                    break;
                                case Item:
                                    if ((ob = mAdv.mObjects.get(itmKey)) != null) {
                                        if (!ob.isVisibleTo(playerKey)) {
                                            itm.mMatchingKeys.remove(i);
                                        }
                                    } else if ((ch = mAdv.mCharacters.get(itmKey)) != null) {
                                        if (!ch.canSeeChar(playerKey)) {
                                            itm.mMatchingKeys.remove(i);
                                        }
                                    }
                                    break;
                                default:
                                    mAdv.mView.TODO("Refine visible possibilities for " + nr.mType + " references");
                                    break;
                            }
                        }
                    }
                }
            }
        }

        if (tmpRefs.pruneImpossibleItems(mRefs)) {
            // ---------------------------------------------------
            //                  SCOPE: SEEN
            //
            // Remove any item possibilities referring to objects,
            // characters or locations that have not yet been
            // seen by the player.
            //
            // "If none of them are currently visible, then scope
            // checks to see if one of the items has ever been
            // seen by the player."
            // ---------------------------------------------------
            mAdv.mView.debugPrint(Task, getKey(),
                    High, "Checking scope: Seen");

            MObject ob;
            MCharacter ch;
            String playerKey = mAdv.getPlayer().getKey();
            for (MReference nr : tmpRefs) {
                if (nr != null) {
                    for (MReference.MReferenceItem itm : nr.mItems) {
                        for (int i = itm.mMatchingKeys.size() - 1; i >= 0; i--) {
                            String itmKey = itm.mMatchingKeys.get(i);
                            switch (nr.mType) {
                                case Object:
                                    if ((ob = mAdv.mObjects.get(itmKey)) != null) {
                                        if (!ob.hasBeenSeenBy(playerKey)) {
                                            itm.mMatchingKeys.remove(i);
                                        }
                                    }
                                    break;
                                case Character:
                                    if ((ch = mAdv.mCharacters.get(itmKey)) != null) {
                                        if (!ch.hasBeenSeenBy(playerKey)) {
                                            itm.mMatchingKeys.remove(i);
                                        }
                                    }
                                    break;
                                case Location:
                                    if ((ob = mAdv.mObjects.get(itmKey)) != null) {
                                        if (!ob.hasBeenSeenBy(playerKey)) {
                                            itm.mMatchingKeys.remove(i);
                                        }
                                    }
                                    break;
                            }
                        }
                    }
                }
            }
        }

        tmpRefs.pruneImpossibleItems(mRefs);

        // If more than one item passes these scope checks, then the player is asked
        // a disambiguation question such as "Do you mean the red pen, the blue pen
        // or the green pen?" to determine which item they meant.
        return tmpRefs;
    }

    public boolean attemptToExecute() throws InterruptedException {
        EnumSet<ExecutionStatus> curStatus = EnumSet.noneOf(ExecutionStatus.class);
        return attemptToExecute(false, false, curStatus,
                false, false, false);
    }

    public boolean attemptToExecute(boolean calledFromEvent) throws InterruptedException {
        EnumSet<ExecutionStatus> curStatus = EnumSet.noneOf(ExecutionStatus.class);
        return attemptToExecute(calledFromEvent, false, curStatus,
                false, false, false);
    }

    public boolean attemptToExecute(boolean calledFromEvent,
                                    boolean isChildTask, EnumSet<ExecutionStatus> curStatus,
                                    boolean evalResponses, boolean execPassingOnly)
            throws InterruptedException {
        return attemptToExecute(calledFromEvent, isChildTask, curStatus,
                evalResponses, execPassingOnly, false);
    }

    /**
     * Attempt to execute this task.
     *
     * @param calledFromEvent    - is an event attempting to execute this task? (if TRUE, will
     *                           not continue trying to execute lower priority matching tasks)
     * @param isChildTask        - is this a child task?
     * @param curStatus          - tracks whether task execution should continue and whether any
     *                           of the tasks executed so far have output text.
     * @param evalResponses      - should we replace functions for anything output by this
     *                           task? We do this so that e.g. ViewRoom will be at the time the
     *                           task was executed.
     * @param execPassingOnly    - only do anything with the task if it passes. Means we already
     *                           have a failing task with output, so don't need another.
     * @param assignSpecificRefs - if we are attempting to run a specific task(e.g. from an
     *                           event) then set the refs to those defined in the task.
     * @return TRUE if the task successfully executed, FALSE otherwise.
     * @throws InterruptedException if task execution is interrupted (e.g.
     *                              user has pressed back button).
     */
    public boolean attemptToExecute(boolean calledFromEvent,
                                    boolean isChildTask, EnumSet<ExecutionStatus> curStatus,
                                    boolean evalResponses, boolean execPassingOnly,
                                    boolean assignSpecificRefs) throws InterruptedException {
        // =============================================================
        //       DON'T EXECUTE COMPLETED NON-REPEATABLE TASKS.
        // -------------------------------------------------------------
        if (getCompleted() && !getRepeatable()) {
            return false;
        }

        mAdv.mView.debugPrint(Task, getKey(), Low,
                "Attempting to execute task " + getDescription() + "...");

        // =============================================================
        //   IF THIS ISN'T A CHILD, CREATE NEW GLOBAL RESPONSE ARRAYS.
        // -------------------------------------------------------------
        // If this is not a child task, create new global pass and fail
        // response arrays, saving any existing contents.
        // -------------------------------------------------------------
        MOrderedHashMap<String, MReferenceList> oldPassResponses = null;
        MOrderedHashMap<String, MReferenceList> oldFailResponses = null;
        if (!isChildTask) {
            if (mAdv.mPassResponses.size() > 0) {
                oldPassResponses = mAdv.mPassResponses.clone();
            }
            if (mAdv.mFailResponses.size() > 0) {
                oldFailResponses = mAdv.mFailResponses.clone();
            }
            // Will this cause a problem, or do we just need to call
            // it before events run tasks for example?
            mAdv.mPassResponses.clear();
            mAdv.mFailResponses.clear();
        }

        // =============================================================
        //      SET GLOBAL REFS TO MATCH SPECIFICS, IF REQUESTED.
        // -------------------------------------------------------------
        // If this is a specific task and caller has requested to
        // assign the specific references, do that now.
        //
        // We copy over the specific references into the global
        // references variable. This is really just in case anyone
        // tries to run a specific task from an event - we need to
        // know what the references should be.
        // -------------------------------------------------------------
        if (mType == Specific && assignSpecificRefs) {
            mAdv.mReferences = new MReferenceList(mAdv, mSpecifics.size());
            for (int i = 0; i < mAdv.mReferences.size(); i++) {
                MSpecific spec = mSpecifics.get(i);
                MReference ref = new MReference((spec.mType));
                mAdv.mReferences.set(i, ref);
                for (String sKey : spec.mKeys) {
                    ref.mItems.add(new MReference.MReferenceItem(sKey));
                }
            }
        }

        // Create a local copy of the global references
        MReferenceList refs = (mAdv.mReferences != null) ?
                mAdv.mReferences.copyDeep() : new MReferenceList(mAdv);

        // If we mention any characters on the command line, add them to
        // the mentioned characters list (so we get "the" char rather than "a" char)
        for (MReference ref : refs) {
            if (ref != null) {
                if (ref.mType == MReference.ReferencesType.Character) {
                    for (MReference.MReferenceItem itm : ref.mItems) {
                        if (itm.mMatchingKeys.size() == 1) {
                            String itmKey = itm.mMatchingKeys.get(0);
                            MCharacter ch = mAdv.mCharacters.get(itmKey);
                            if (ch != null) {
                                mAdv.mCharsMentionedThisTurn.get(ch.getGender()).add(ch.getKey());
                            }
                        }
                    }
                }
            }
        }

        // =============================================================
        //          TRY TO EXECUTE THIS TASK'S SUB-TASKS
        // -------------------------------------------------------------
        // We iterate through each one, using the local copy of the
        // references to evaluate them.
        // -------------------------------------------------------------
        String[] refKeys = new String[refs.size()];
        String[] refCmds = new String[refs.size()];

        boolean passes =
                executeSubTasks(calledFromEvent, refs,
                        0, refKeys, refCmds, curStatus, execPassingOnly);

        if (execPassingOnly && !passes) {
            // This task doesn't pass and we already have a failing task
            // with output, so no point in going any further.
            return false;
        }

        // =============================================================
        //     IF THIS ISN'T A CHILD OR CALLER HAS EXPLICITLY ASKED
        //          FOR IT, GATHER AND DISPLAY THE RESPONSES.
        // -------------------------------------------------------------
        // We display a list of responses comprising everything in
        // mPassResponses plus anything in mFailResponses that isn't
        // in mPassResponses.
        // -------------------------------------------------------------
        if (!isChildTask || evalResponses) {
            MOrderedHashMap<String, MReferenceList> responses = new MOrderedHashMap<>();
            for (String key : mAdv.mPassResponses.mOrderedKeys) {
                responses.put(key, mAdv.mPassResponses.get(key));
            }

            if (mAdv.mPassResponses.size() == 0 &&
                    mAdv.mFailResponses.size() >= 0 &&
                    !getFailOverride().toString().equals("") &&
                    containsWord(mAdv.mInput, "all")) {
                // -------------------------------------------------------------
                // We only got fail responses, the fail override message
                // has been set and the user referred to "all". So just
                // display the fail override.
                // -------------------------------------------------------------
                mAdv.mView.displayText(mAdv, getFailOverride().toString());
            } else {
                // -------------------------------------------------------------
                //   Get A, B, C from D.
                //   Pass: You pick up A, B from D.
                //   Fail: You can't get A from D.
                //   Fail: B, C is too heavy to pick up.
                //   Need: You pick up A, B from D. C is too heavy to pick up.
                //
                // We could have responses from different tasks here. We need to
                // only match references from the same parent task. So, for example,
                // if we have an action that executes a different task with a
                // different number of references, the different tasks should not
                // cancel each other out.
                //
                //   E.g. "Stand on current object" has no references, but
                //   executes task as action which has a parameter.
                //
                // So look at every reference combination in each failure message.
                // If we don't have that combination in our pass set then add it.
                // -------------------------------------------------------------
                for (String failMsg : mAdv.mFailResponses.mOrderedKeys) {
                    MReferenceList refsFail = mAdv.mFailResponses.get(failMsg);
                    boolean allMatch = false;

                    for (String passMsg : mAdv.mPassResponses.mOrderedKeys) {
                        MReferenceList refsPass = mAdv.mPassResponses.get(passMsg);

                        // Added by Tim 2018: handle the case where the fail message
                        // doesn't have any references. In this case, we just
                        // check whether the fail message equals any pass messages
                        // and, if not, we add the fail message to the set. This
                        // fixes bug in advent hint system whereby hint responses
                        // were not showing.
                        if (refsFail.size() == 0) {
                            if (!failMsg.equals(passMsg)) {
                                continue;
                            }
                        }

                        allMatch = true;

                        checkFails:
                        for (int i = 0; i < refsFail.size(); i++) {
                            MReference refFail = refsFail.get(i);
                            if (refFail == null) {
                                // I think this is where our task could be executing
                                // a different task, with different references...
                                allMatch = false;
                            } else {
                                for (int j = 0; j < refFail.mItems.size(); j++) {
                                    MReference.MReferenceItem itmFail = refFail.mItems.get(j);
                                    // There should only be one matching possibility
                                    // here, so no need to iterate them

                                    // resPass(i) Is Nothing - think this may
                                    // also be different task/different refs issue...
                                    // Should only do this if it is for the
                                    // same parent. If we execute a sub-task, that
                                    // should become the parent
                                    if (refsPass.size() <= i || refsPass.get(i) == null ||
                                            refsPass.get(i).mItems.size() <= j ||
                                            !refsPass.get(i).mItems.get(j).mMatchingKeys.get(0)
                                                    .equals(itmFail.mMatchingKeys.get(0))) {
                                        // This fail is different from this pass
                                        allMatch = false;
                                        break checkFails;
                                    }
                                }
                            }
                        }

                        if (allMatch) {
                            break;
                        }
                    }

                    if (!allMatch) {
                        // We've found a failure message that didn't match any
                        // pass messages.
                        if (responses.containsKey(failMsg)) {
                            // Need to add refs for this message into the message
                            // TODO
                        } else {
                            // Need to add this message.
                            // Put fail messages before pass messages (TODO: implement
                            // a method that detects the correct order).
                            responses.put(failMsg, mAdv.mFailResponses.get(failMsg), 0);
                        }
                    }
                }

                // -------------------------------------------------------------
                // Display the responses.
                // -------------------------------------------------------------
                for (String msg : responses.mOrderedKeys) {
                    mAdv.mReferences = responses.get(msg);
                    mAdv.mView.displayText(mAdv, msg);
                }
            }

            if (evalResponses) {
                mAdv.mPassResponses.clear();
                mAdv.mFailResponses.clear();
            }
        }

        if (passes) {
            // -------------------------------------------------------
            // PROCESS ANY WALKS AND/OR EVENTS THAT ARE TRIGGERED
            // BY THIS TASK COMPLETING.
            // -------------------------------------------------------
            // TCC: According to ADRIFT 5 manual, only one walk should be running per
            // character at any given time ("A character can have several walks, but
            // will follow one walk at a time..") So if this causes a walk to resume
            // or start we need to pause any other running walks. Original code
            // didn't do that which seems to be a bug. N.B. if the task completing
            // triggers more than one walk to start, we only start the last one and
            // ignore the rest.

            // Need to remember htblResponses, as they'll be cleared
            // out if events run tasks...
            for (MCharacter c : mAdv.mCharacters.values()) {
                MWalk started = null;
                for (MWalk w : c.mWalks) {
                    for (MEventOrWalkControl ctrl : w.mWalkControls) {
                        if (ctrl.mCompleteOrNot == Completion &&
                                ctrl.mTaskKey.equals(getKey())) {
                            // If a child task of the current task has affected
                            // the walk control, ignore this as a trigger
                            if (w.mTriggeringTask.equals("") ||
                                    !getChildTasks(true)
                                            .contains(w.mTriggeringTask)) {
                                switch (ctrl.mControl) {
                                    case Resume:
                                        w.resume();
                                        started = w;
                                        break;
                                    case Start:
                                        w.start();
                                        started = w;
                                        break;
                                    case Stop:
                                        w.stop();
                                        break;
                                    case Suspend:
                                        w.pause();
                                        break;
                                }
                                w.mTriggeringTask = getKey();
                            }
                        }
                    }
                }

                if (started != null) {
                    // OK, we just started / resumed a walk for this
                    // character, so make sure no other walks are active
                    for (MWalk w : c.mWalks) {
                        if (w != started) {
                            if (w.mStatus == Running) {
                                w.pause();
                            }
                        }
                    }
                }
            }

            for (MEvent e : mAdv.mEvents.values()) {
                for (MEventOrWalkControl ctrl : e.mEventControls) {
                    if (ctrl.mCompleteOrNot == Completion &&
                            ctrl.mTaskKey.equals(getKey())) {
                        // If a child task of the current task has affected
                        // the walk control, ignore this as a trigger
                        if (e.mTriggeringTask.equals("") ||
                                !getChildTasks(true)
                                        .contains(e.mTriggeringTask)) {
                            switch (ctrl.mControl) {
                                case Resume:
                                    e.resume();
                                    break;
                                case Start:
                                    e.start();
                                    break;
                                case Stop:
                                    e.stop();
                                    break;
                                case Suspend:
                                    e.pause();
                                    break;
                            }
                            e.mTriggeringTask = getKey();
                        }
                    }
                }
            }
        }

        // -----------------------------------------------------------------
        // IF THIS WASN'T CALLED BY AN EVENT, CHECK IF WE SHOULD CONTINUE
        // EXECUTING MATCHING LOWER PRIORITY TASKS.
        // -----------------------------------------------------------------
        // Then ADRIFT checks the "Continue executing matching lower priority
        // tasks (multiple matching)" checkbox on the "Advanced" page of the
        // general task. If this is selected then ADRIFT continues to search lower
        // priority general tasks for another match and goes back to step 4
        // above if it finds one.
        // -----------------------------------------------------------------
        if (!calledFromEvent) {
            if (getContinueToExecuteLowerPriority()) {
                curStatus.add(ContinueExecuting);
                mAdv.mView.debugPrint(Task, getKey(), High,
                        "Continuing trying to execute lower " +
                                "priority tasks (multiple matches)");
            } else {
                if (passes) {
                    if (curStatus.contains(HasOutput)) {
                        mAdv.mView.debugPrint(Task, getKey(), High,
                                "Task passes and has output.  " +
                                        "Will not execute lower priority tasks");
                    } else {
                        mAdv.mView.debugPrint(Task, getKey(), High,
                                "Task passes but has no output, " +
                                        "therefore will continue to execute " +
                                        "lower priority tasks");
                        curStatus.add(ContinueExecuting);
                    }
                } else {
                    if (mAdv.mTaskExecutionMode == HighestPriorityPassingTask) {
                        if (curStatus.contains(HasOutput)) {
                            mAdv.mView.debugPrint(Task, getKey(), High,
                                    "Task fails but has output.  " +
                                            "Will continue trying to execute " +
                                            "lower priority tasks");
                            // Hmm, we actually want to execute lower
                            // priority tasks EXCEPT library ones...
                            curStatus.add(ContinueExecuting);
                        } else {
                            mAdv.mView.debugPrint(Task, getKey(), High,
                                    "Task does not pass and also has " +
                                            "no output, therefore will continue " +
                                            "to execute lower priority tasks");
                            curStatus.add(ContinueExecuting);
                        }
                    } else {
                        if (!curStatus.contains(HasOutput)) {
                            mAdv.mView.debugPrint(Task, getKey(), High,
                                    "Task does not pass and also has no " +
                                            "output, therefore will continue to " +
                                            "execute lower priority tasks");
                            curStatus.add(ContinueExecuting);
                        }
                    }
                }
            }

            if (curStatus.contains(ContinueExecuting) && !isChildTask) {
                // ----------------------------------------------------
                // We've finished executing all of the child tasks of
                // this parent task and we are required to continue
                // executing. So update the status bar and carry on
                // evaluating input at the next lowest priority level.
                // ----------------------------------------------------
                mAdv.mView.updateStatusBar(mAdv);
                mAdv.mView.debugPrint(Task, getKey(), Medium,
                        "Continuing trying to execute lower priority tasks");
                MAdventure[] adv1 = new MAdventure[1];
                adv1[0] = mAdv;
                mAdv.evalInput(getPriority() + 1,
                        !passes && curStatus.contains(HasOutput));

                // N.B. we don't check whether the adventure object stored in
                // adv1 changes after evalInput as we are assuming that
                // any "restart" command would have been processed by this stage...
            }
        }

        // =============================================================
        // RESTORE THE GLOBAL RESPONSE ARRAYS TO THEIR EARLIER CONTENTS,
        // IF ANY.
        // -------------------------------------------------------------
        if (oldPassResponses != null) {
            mAdv.mPassResponses = oldPassResponses.clone();
        }
        if (oldFailResponses != null) {
            mAdv.mFailResponses = oldFailResponses.clone();
        }

        return passes;
    }

    private boolean executeSubTasks(boolean calledFromEvent,
                                    @NonNull MReferenceList refs, int refIndex,
                                    @NonNull String[] refKeys, @NonNull String[] refCmds,
                                    @NonNull EnumSet<ExecutionStatus> curStatus,
                                    boolean execPassingOnly) {
        // E.g. if our task is "get red ball and blue ball from box", then subtasks are
        // "get red ball from box" and
        // "get blue ball from box"
        boolean ret = false;

        if (refIndex < refs.size()) {
            MReference ref = refs.get(refIndex);
            if (ref == null || ref.mItems.size() == 0) {
                // -------------------------------------------
                // This reference is either not set or contains
                // no items. Skip over it and attempt to execute
                // using the next reference.
                // -------------------------------------------
                refKeys[refIndex] = "";
                refCmds[refIndex] = "";
                if (executeSubTasks(calledFromEvent, refs, refIndex + 1,
                        refKeys, refCmds, curStatus, execPassingOnly)) {
                    return true;
                }
            } else {
                for (int i = 0; i < ref.mItems.size(); i++) {
                    MReference.MReferenceItem itm = ref.mItems.get(i);
                    if (itm.mMatchingKeys.size() > 0) {
                        refKeys[refIndex] = itm.mMatchingKeys.get(0);
                    } else {
                        // Assume it's ok to leave this reference key
                        // as null as there was no match.
                        mAdv.mView.TODO("Check that this is intended...");
                    }
                    refCmds[refIndex] = itm.mCommandReference;
                    if (executeSubTasks(calledFromEvent, refs, refIndex + 1,
                            refKeys, refCmds, curStatus, execPassingOnly)) {
                        ret = true;
                    }
                }
            }
        } else {
            // -------------------------------------------
            // We've gone through all of the references
            // now and have assigned the appropriate
            // values to refKeys and refCmds. Now it's
            // time to actually execute each of the valid
            // sub-tasks.
            // -------------------------------------------
            if (attemptToExecuteSubTask(calledFromEvent, refKeys,
                    refCmds, curStatus, execPassingOnly)) {
                return true;
            }
        }

        return ret;
    }

    private boolean attemptToExecuteSubTask(boolean calledFromEvent,
                                            @NonNull String[] refKeys, @NonNull String[] refCmds,
                                            @NonNull EnumSet<ExecutionStatus> curStatus,
                                            boolean execPassingOnly) {
        // Returns true if successfully executed
        // curStatus will contain details of what happened
        try {
            boolean ret = false;

            //--------------------------------------------------------------
            // Set the global matched refs to reflect the references of this
            // particular sub-task, if any.
            // -------------------------------------------------------------
            mAdv.mReferences = new MReferenceList(mAdv, refKeys.length);
            if (refKeys.length == 0) {
                mAdv.mView.debugPrint(Task, getKey(), Medium,
                        "Checking reference free task " + getDescription());
            } else {
                String sSubTask = getParentTaskCommand(mLastMatchingCommandIndex);
                MStringArrayList refs = getReferences();

                for (int iRef = 0; iRef < refs.size(); iRef++) {
                    String taskRef = refs.get(iRef);
                    sSubTask = sSubTask.replace(taskRef,
                            mAdv.getNameFromKey(refKeys[iRef], false, false));
                    MReference r = new MReference(taskRef);
                    if (!refKeys[iRef].equals("")) {
                        MReference.MReferenceItem itm = new MReference.MReferenceItem();
                        itm.mMatchingKeys.add(refKeys[iRef]);
                        itm.mCommandReference = refCmds[iRef];
                        r.mItems.add(itm);
                        r.mRefMatch = taskRef.replace("%", "");
                    }
                    if (mCommands.size() > 0) {
                        r.setIndex(taskRef, MGlobals.getRefs(mCommands.get(0)));
                    }
                    mAdv.mReferences.set(iRef, r);
                }

                mAdv.mView.debugPrint(Task, getKey(), Medium,
                        "Checking " + (refKeys.length == 1 ? "single" :
                                (refKeys.length == 2 ? "double" : "triple or more")) +
                                " reference task " + sSubTask);
            }

            //-------------------------------------------------------
            // Check if the restrictions of the sub-task pass, given
            // the global matched refs.
            // ------------------------------------------------------
            String sMessage = "";
            boolean bPass = mRestrictions.passes(false, mAdv.mReferences);
            boolean[] bOutputMessages = new boolean[1];
            bOutputMessages[0] = false;

            if (bPass) {
                // ---------------------------------------
                //   THIS TASK PASSED ITS RESTRICTIONS
                // ---------------------------------------
                mAdv.mView.debugPrint(Task, getKey(),
                        Medium, "Passed Restrictions");

                // Remove any failing references
                NextMessage:
                for (String failMsg : mAdv.mFailResponses.mOrderedKeys) {
                    MReferenceList refsFail = mAdv.mFailResponses.get(failMsg);

                    // Added by Tim 2018:
                    // We assume that any fail message with no
                    // references should not be removed.
                    if (refsFail.size() == 0) {
                        continue;
                    }

                    for (int iRef = 0; iRef < refsFail.size(); iRef++) {
                        MReference refFail = refsFail.get(iRef);
                        MReference refPass = mAdv.mReferences.get(iRef);
                        if (refFail != null) {
                            for (int iItm = 0; iItm < refFail.mItems.size(); iItm++) {
                                MReference.MReferenceItem itmFail = refFail.mItems.get(iItm);
                                // There should only be one matching possibility here, so no need to iterate them
                                if (refPass == null ||
                                        !refPass.mItems.get(iItm).mMatchingKeys.get(0).equals(itmFail.mMatchingKeys.get(0))) {
                                    // This fail is different from this pass
                                    continue NextMessage;
                                }
                            }
                        }
                    }

                    // Ok, lets remove the failed one
                    mAdv.mFailResponses.remove(failMsg);
                    break; // There should only be one matching the refs, so bomb out so we don't cause problem iterating loop
                }

                // ----------------------------------------------------------------
                //       ATTEMPT TO EXECUTE SPECIFIC CHILD TASKS, IF ANY
                // ----------------------------------------------------------------
                // * These tasks are checked in priority order.
                //
                // * If the specific task has had any of its references made specific
                //   to a particular item, then that must be the item specified by the player,
                //   otherwise it is ignored.
                //
                // * All of the restrictions of the specific task must pass, otherwise the
                //   same steps specified in section 7 above for general tasks are performed
                //   for the specific tasks.
                MStringArrayList childTasks = getChildTasks();  // this list is sorted in priority order
                if (childTasks.size() > 0) {
                    mAdv.mView.debugPrint(Task, getKey(), High,
                            "Checking whether any of our child tasks should override...");
                }

                boolean bShouldParentOutputText = true;
                boolean bShouldParentExecuteTasks = true;
                boolean bAnyChildHasOutput = false;
                ArrayList<String> lAfterChildren = new ArrayList<>();

                executeChildTasks:
                for (String sChildTask : childTasks) {
                    MTask tasChild = mAdv.mTasks.get(sChildTask);
                    if (mType == Specific) {
                        // If our parent is a Specific task, we may need to pad our
                        // Specifics out with any that have been dropped
                        if (mSpecifics.size() != tasChild.mSpecifics.size()) {
                            // TODO - These may be in the wrong order if we're matching on a secondary command!
                            ArrayList<MSpecific> newSpecs = new ArrayList<>(mSpecifics.size());
                            int iChild = 0;
                            for (int i = 0; i < mSpecifics.size(); i++) {
                                newSpecs.add(mSpecifics.get(i));
                                MSpecific newSpec = newSpecs.get(i);
                                if (newSpec.mKeys.size() == 0 ||
                                        (newSpec.mKeys.size() == 1 && newSpec.mKeys.get(0).equals(""))) {
                                    newSpecs.set(i, tasChild.mSpecifics.get(iChild));
                                    iChild++;
                                }
                            }
                            tasChild.mSpecifics = newSpecs;
                        }
                    }

                    if (tasChild.refsMatchSpecifics(mAdv.mReferences, mLastMatchingCommandIndex)) {
                        // This should remove the ref so it doesn't get processed when we
                        // execute the main task
                        mAdv.mView.debugPrint(Task, getKey(),
                                Medium,
                                "Overriding child task found: " + tasChild.getDescription());

                        switch (tasChild.mSpecificOverrideType) {
                            case BeforeActionsOnly:
                            case BeforeTextAndActions:
                            case BeforeTextOnly:
                            case Override:

                                mAdv.mView.mDebugIndent++;

                                if (tasChild.mSpecificOverrideType == SpecificOverrideTypeEnum.Override) {
                                    mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                            High, "Override Parent");
                                } else {
                                    mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                            High, "Run Before Parent");
                                }

                                // Make a note of how many failing responses we have, so we know if this task has failing output
                                int iFailRefsBefore = 0;
                                if (getReferences().size() == 0) {
                                    iFailRefsBefore += mAdv.mFailResponses.size();
                                } else {
                                    for (MReferenceList responses : mAdv.mFailResponses.values()) {
                                        for (MReference response : responses) {
                                            iFailRefsBefore += response.mItems.size();
                                        }
                                    }
                                }

                                EnumSet<ExecutionStatus> childStatus = EnumSet.noneOf(ExecutionStatus.class);

                                // --------------------------------------------------
                                //   Attempt to execute the specific child task
                                // --------------------------------------------------
                                if (tasChild.attemptToExecute(calledFromEvent, true,
                                        childStatus, false, bAnyChildHasOutput)) {
                                    // --------------------------------------------------
                                    //   Success - work out what implications this has
                                    //   for displaying text before / after the parent
                                    //   task, as well as whether the parent task should
                                    //   even be executed.
                                    // --------------------------------------------------
                                    mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                            High, "Child task passes");
                                    switch (tasChild.mSpecificOverrideType) {
                                        case BeforeTextAndActions:
                                            mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                                    Medium, "Execute Parent actions...");
                                            mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                                    Medium, "Output Parent text...");
                                            break;
                                        case BeforeActionsOnly:
                                            mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                                    Medium, "Execute Parent actions...");
                                            bShouldParentOutputText = false;
                                            break;
                                        case BeforeTextOnly:
                                            mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                                    Medium, "Output Parent text...");
                                            bShouldParentExecuteTasks = false;
                                            break;
                                        case Override:
                                            bShouldParentExecuteTasks = false;
                                            bShouldParentOutputText = false;
                                            break;
                                    }
                                } else {
                                    // --------------------------------------------------
                                    //   Failure - work out what implications this has
                                    //   for displaying text before / after the parent
                                    //   task, as well as whether the parent task should
                                    //   even be executed.
                                    // --------------------------------------------------
                                    mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                            High, "Child task fails");
                                    // Ok, compare failing output vs what it was before - if we have
                                    // failing output, this takes precedence over parent if set
                                    int iFailRefsAfter = 0;
                                    if (getReferences().size() == 0) {
                                        iFailRefsAfter += mAdv.mFailResponses.size();
                                    } else {
                                        for (MReferenceList responses : mAdv.mFailResponses.values()) {
                                            for (MReference response : responses) {
                                                iFailRefsAfter += response.mItems.size();
                                            }
                                        }
                                    }

                                    if (iFailRefsAfter > iFailRefsBefore) {
                                        switch (tasChild.mSpecificOverrideType) {
                                            case Override:
                                            case BeforeTextAndActions:
                                                bShouldParentOutputText = false;
                                                bShouldParentExecuteTasks = false;
                                                break;
                                            case BeforeTextOnly:
                                                bShouldParentOutputText = false;
                                                break;
                                            case BeforeActionsOnly:
                                                bShouldParentExecuteTasks = false;
                                                break;
                                        }
                                        bAnyChildHasOutput = true;
                                    }
                                }

                                // If the child task has output, ensure we are aware of it:
                                if (childStatus.contains(HasOutput)) {
                                    curStatus.add(HasOutput);
                                }
                                mAdv.mView.mDebugIndent--;

                                // If the specific task that executes has the "Continue executing matching
                                // lower priority tasks (multiple matching)" checkbox selected on its
                                // "Advanced" page, then ADRIFT continues to check any remaining specific
                                // tasks in priority order.
                                if (!childStatus.contains(ContinueExecuting)) {
                                    mAdv.mView.debugPrint(Task, tasChild.getKey(), Medium,
                                            "Do not continue executing other child tasks.");
                                    break executeChildTasks;
                                } else {
                                    mAdv.mView.debugPrint(Task, tasChild.getKey(), Medium,
                                            "Continue executing other child tasks.");
                                }
                                break;

                            default:
                                lAfterChildren.add(tasChild.getKey());
                                if (tasChild.mSpecificOverrideType == SpecificOverrideTypeEnum.AfterActionsOnly ||
                                        tasChild.mSpecificOverrideType == SpecificOverrideTypeEnum.AfterTextOnly) {
                                    // TODO - Need to check passes to see if we need to suppress parent Text or Actions

                                }
                                break;
                        }
                    }
                }

                // ----------------------------------------------------------
                //          DISPLAY ANY "BEFORE" EXECUTION TEXT
                // ----------------------------------------------------------
                // If there are no "override" tasks, or they fail matching or
                // restrictions, then the text box of the general task is
                // displayed to the player and its actions are performed. If a
                // "run before" task has executed successfully then it controls
                // which parts of the general task execute with its "Display parent
                // message" and "Execute parent actions" check boxes. The order
                // they are done in is controlled by "Display completion message
                // Before/After executing actions" on the general task "Advanced" page.
                String sBeforeActionsMessage = "";
                int iResponsePosition = -1;
                if (mDisplayCompletion == Before && bShouldParentOutputText) {
                    // We may have already printed these refs out in a child task,
                    // so only print them here if we haven't done that
                    if (mAdv.mReferences != null) {
                        mAdv.mReferences.printToDebug();
                    }
                    sMessage = getCompletionMessage().toString();
                    if (sMessage == null) {
                        sMessage = "";
                    }
                    mAdv.mView.debugPrint(Task, getKey(), High, sMessage);

                    // If we do this then drop all gives us:
                    // Ok, I drop X.  Ok, I drop Y.  Ok, I drop Z.
                    mAdv.mView.mDisplaying = true;
                    mTestingOutput = true; // Ensure any DisplayOnce descriptions aren't marked as displayed, as we'll mark them in final output (Display)
                    sBeforeActionsMessage = mAdv.evalStrRegex(
                            mAdv.evalFuncs(sMessage, mAdv.mReferences),
                            mAdv.mReferences);
                    mTestingOutput = false;
                    if (sBeforeActionsMessage.equals(sMessage)) {
                        sBeforeActionsMessage = "";
                    }
                    mAdv.mView.mDisplaying = false;
                    if (sBeforeActionsMessage.equals("")) {
                        // It is safe to add the response now
                        if (!mAggregateOutput) {
                            sMessage = mAdv.evalStrRegex(
                                    mAdv.evalFuncs(sMessage, mAdv.mReferences),
                                    mAdv.mReferences);
                        }
                        if (mAdv.mPassResponses.addResponse(mAdv, bOutputMessages,
                                sMessage, refKeys, mAdv.mReferences)) {
                            curStatus.add(HasOutput);
                        }
                    } else {
                        // The response changes with functions, so we can't add
                        // yet until we know whether the actions affect the output
                        iResponsePosition = bPass ?
                                mAdv.mPassResponses.size() :
                                mAdv.mFailResponses.size();
                    }
                }

                // ----------------------------------------------------------
                //        IF APPROPRIATE, RUN THIS TASK'S ACTIONS
                // ----------------------------------------------------------
                setCompleted(true);
                if (bShouldParentExecuteTasks) {
                    executeAllActions(calledFromEvent, curStatus);
                }

                if (!sBeforeActionsMessage.equals("")) {
                    // Check to see if the actions had any effect on the message.
                    // If so, add the replaced message. If not, add the unreplaced message
                    mAdv.mView.mDisplaying = true;
                    mTestingOutput = true;
                    if (!sBeforeActionsMessage.equals(mAdv.
                            evalStrRegex(mAdv.evalFuncs(
                                    sMessage, mAdv.mReferences), mAdv.mReferences))) {
                        sMessage = sBeforeActionsMessage;
                    }
                    mTestingOutput = false;
                    mAdv.mView.mDisplaying = false;
                    if (!mAggregateOutput) {
                        sMessage = mAdv.evalStrRegex(mAdv.evalFuncs(
                                sMessage, mAdv.mReferences), mAdv.mReferences);
                    }
                    if (mAdv.mPassResponses.addResponse(mAdv, bOutputMessages, sMessage,
                            refKeys, iResponsePosition, mAdv.mReferences)) {
                        curStatus.add(HasOutput);
                    }
                }

                // ----------------------------------------------------------
                //          DISPLAY ANY "AFTER" EXECUTION TEXT
                // ----------------------------------------------------------
                if (mDisplayCompletion == After && bShouldParentOutputText) {
                    sMessage = getCompletionMessage().toString();
                    if (sMessage == null) {
                        sMessage = "";
                    }
                    if (!mAggregateOutput) {
                        sMessage = mAdv.evalStrRegex(
                                mAdv.evalFuncs(sMessage, mAdv.mReferences),
                                mAdv.mReferences);
                    }
                    mAdv.mView.debugPrint(Task, getKey(), High, sMessage);
                    if (mAdv.mPassResponses.addResponse(mAdv, bOutputMessages,
                            sMessage, refKeys, mAdv.mReferences)) {
                        curStatus.add(HasOutput);
                    }
                }

                // ----------------------------------------------------------
                //          EXECUTE ANY "RUN AFTER" TASKS
                // ----------------------------------------------------------
                // ADRIFT next checks to see if that general task has any
                // specific tasks that are set to "Run after" it, and if
                // so, and they match the references and pass restrictions,
                // then they are executed.
                for (String sChildTask : lAfterChildren) {
                    mAdv.mView.mDebugIndent++;
                    MTask tasChild = mAdv.mTasks.get(sChildTask);
                    mAdv.mView.debugPrint(Task, tasChild.getKey(),
                            High, "Run After Parent");

                    EnumSet<ExecutionStatus> childStatus = EnumSet.noneOf(ExecutionStatus.class);

                    if (tasChild.attemptToExecute(calledFromEvent, true,
                            childStatus, false, false)) {
                        mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                High, "Child task passes");
                    } else {
                        mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                High, "Child task fails");
                    }

                    // If the child task has output, ensure we are aware of it
                    if (childStatus.contains(HasOutput)) {
                        curStatus.add(HasOutput);
                    }
                    mAdv.mView.mDebugIndent--;
                    if (!childStatus.contains(ContinueExecuting)) {
                        mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                Medium, "Do not continue executing other child tasks.");
                        break;
                    } else {
                        mAdv.mView.debugPrint(Task, tasChild.getKey(),
                                Medium, "Continue executing other child tasks.");
                    }
                }

                // If we reach this point, the sub-task executed successfully.
                ret = true;

            } else {
                // ---------------------------------------
                //   THIS TASK FAILED ITS RESTRICTIONS
                // ---------------------------------------
                if (!execPassingOnly) {
                    mAdv.mView.debugPrint(Task, getKey(),
                            Medium, "Failed Restrictions");
                    sMessage = mAdv.mRestrictionText;

                    if (sMessage == null) {
                        sMessage = "";
                    }

                    if (mAdv.mReferences != null) {
                        mAdv.mReferences.printToDebug();
                    }

                    if (mSpecificOverrideType == SpecificOverrideTypeEnum.AfterTextAndActions ||
                            mSpecificOverrideType == SpecificOverrideTypeEnum.AfterTextOnly) {
                        // Although this is a failing restriction, it is being output _after_ a successful task,
                        // so we need to append to the success list :-/
                        if (!sMessage.equals("")) {
                            bPass = true;
                        }
                    }
                }
            }

            boolean r = bPass ?
                    mAdv.mPassResponses.addResponse(mAdv, bOutputMessages,
                            sMessage, refKeys, mAdv.mReferences) :
                    mAdv.mFailResponses.addResponse(mAdv, bOutputMessages,
                            sMessage, refKeys, mAdv.mReferences);
            if (r) {
                curStatus.add(HasOutput);
            }

            return ret;

        } catch (Exception ex) {
            mAdv.mView.errMsg("Error executing subtask " + getKey(), ex);
            return false;
        }
    }

    private boolean refsMatchSpecifics(@Nullable MReferenceList refs, int cmdIndex) {
        // Specifics are always defined in the order of the first command in the task
        // We may be matching on a different command, in which case Specifics will be
        // in a different order from the References

        // See if we have all the Specifics we need in the References
        if (refs != null && mSpecifics.size() == refs.size()) {
            for (int i = 0; i < mSpecifics.size(); i++) {
                // Make sure References contains all Specifics
                for (String key : mSpecifics.get(i).mKeys) {
                    // We must find all of these in order for the task to match
                    boolean keyFoundInRefs = false;
                    if (key.equals("")) {
                        // i.e. match any object/character etc...
                        keyFoundInRefs = true;
                    } else {
                        if (key.contains("%")) {
                            key = mAdv.evalFuncs(key, mAdv.mReferences);
                        }

                        int refIndex = i;

                        // If this is matching on a second command in the task
                        // where the refs are the other way around, it fails to
                        // match the specifics. This is because Specific tasks
                        // always match on the first General task command
                        if (cmdIndex > 0) {
                            refIndex = getAlternateRef(refIndex);
                        }

                        // Grab the correct Reference
                        MReference newRef = refs.get(refIndex);
                        for (MReference ref : refs) {
                            if (ref.mCmdIndex == i) {
                                newRef = ref;
                                break;
                            }
                        }

                        for (int j = newRef.mItems.size() - 1; j >= 0; j--) {
                            if (newRef.mItems.get(j).mMatchingKeys.get(0).
                                    toLowerCase().equals(key.toLowerCase())) {
                                keyFoundInRefs = true;
                                break;
                            }
                        }
                    }
                    if (!keyFoundInRefs) {
                        return false;
                    }
                }
            }
        } else {
            // Not matching same amount
            return false;
        }

        return true;
    }

    private int getAlternateRef(int refIndex) {
        // If we have multiple commands, and we are matching on a different
        // command, make sure we get the right ref
        // E.g. give %object% to %character%
        //      give %character% %object%
        MTask task = this;
        while (task.mType == Specific && !task.getParentTask().equals("")) {
            task = mAdv.mTasks.get(task.getParentTask());
        }

        String matchedRef = task.getReferences().get(refIndex);
        int i = 0;
        for (String origRef :
                MGlobals.getRefs(task.getCommand(false, 0))) {
            if (origRef.equals(matchedRef)) {
                return i;
            }
            i++;
        }

        return refIndex;
    }

    /**
     * Get a list of patterns representing each command of this task.
     *
     * @return a list of patterns (index 0 = patterns for command 0,
     * index 1 = patterns for command 1, etc.)
     */
    @NonNull
    public ArrayList<ArrayList<Pattern>> getPatterns() {
        // Lazy initialisation
        if (mPatterns == null) {
            mPatterns = new ArrayList<>(mCommands.size());
            for (int i = 0; i < mCommands.size(); i++) {
                String cmd = mCommands.get(i);
                mPatterns.add(new ArrayList<Pattern>());
                for (Pattern p : MGlobals.getPatterns(mAdv, cmd, "")) {
                    mPatterns.get(i).add(p);
                }
            }
        }
        return mPatterns;
    }

    private void executeAllActions(boolean calledFromEvent,
                                   @NonNull EnumSet<ExecutionStatus> curStatus) {
        mAdv.mView.mDebugIndent++;
        for (MAction act : mActions) {
            act.executeCopy(getParentTaskCommand(mLastMatchingCommandIndex),
                    this, calledFromEvent, curStatus);
        }
        mAdv.mView.mDebugIndent--;
    }

    /**
     * Attempt to match the given input to the given command of this task.
     *
     * @param input               - input to match.
     * @param cmdIndex                - the index of the command to attempt a match against.
     * @param secondChance - if NULL, this does nothing. Otherwise, if
     *                            upon entry to this function, this hashmap
     *                            contains this task's key, this task will
     *                            automatically match. If the hashmap does
     *                            not contain this task's key, and the task
     *                            matches on the pattern but not the referred
     *                            object / character / location, and the task
     *                            has a Must Exist restriction, then it will
     *                            be added to the secondChance hashmap
     *                            as a possible candidate should the first
     *                            search fail to find an appropriate task.
     * @return TRUE if we have successfully matched input against a command pattern
     * (and in this case "mRefs" will now contain the evaluated references
     * in the same order they appear in the pattern), FALSE otherwise.
     */
    private boolean inputMatchesCommand(@NonNull String input, int cmdIndex,
                                        @Nullable MTaskHashMap secondChance) {
        // Iterate over the patterns associated with given command index and
        // see if any of them completely matches the supplied input.
        for (Pattern re : getPatterns().get(cmdIndex)) {
            // Clear the references.
            int nNewRefs = 0;
            mRefs.setToNull();

            // Try to match all the references.
            Matcher m = re.matcher(input);
            boolean matched = m.matches();
            if (matched) {
                checkMatches:
                for (String grpName : re.groupNames()) {
                    String grpVal = m.group(grpName);
                    if (grpVal == null) {
                        // Input didn't match this group name, so just
                        // continue onto the next one.
                        continue;
                    }
                    grpVal = grpVal.trim();
                    MReference newRef;
                    switch (grpName) {
                        case "objects":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Object);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (!inputMatchesObjects(grpVal,
                                    nNewRefs - 1, false,
                                    false, secondChance)) {
                                matched = false;
                                break checkMatches;
                            }
                            break;

                        case "object1":
                        case "object2":
                        case "object3":
                        case "object4":
                        case "object5":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Object);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (!checkInputMatch(mAdv.mObjects, MReference.ReferencesType.Object,
                                    grpVal, nNewRefs - 1, 0,
                                    false, false, secondChance)) {
                                matched = false;
                                break checkMatches;
                            }
                            break;

                        case "characters":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Character);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (!inputMatchesCharacters(grpVal,
                                    nNewRefs - 1, secondChance)) {
                                matched = false;
                                break checkMatches;
                            }
                            break;

                        case "character1":
                        case "character2":
                        case "character3":
                        case "character4":
                        case "character5":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Character);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (!checkInputMatch(mAdv.mCharacters,
                                    MReference.ReferencesType.Character,
                                    grpVal, nNewRefs - 1, 0,
                                    false, false, secondChance)) {
                                matched = false;
                                break checkMatches;
                            }
                            break;

                        case "location1":
                        case "location2":
                        case "location3":
                        case "location4":
                        case "location5":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Location);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (!checkInputMatch(mAdv.mLocations,
                                    MReference.ReferencesType.Location,
                                    grpVal, nNewRefs - 1, 0,
                                    false, false, secondChance)) {
                                matched = false;
                                break checkMatches;
                            }
                            break;

                        case "item1":
                        case "item2":
                        case "item3":
                        case "item4":
                        case "item5":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Item);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (!inputMatchesObjects(grpVal,
                                    nNewRefs - 1, false, false, secondChance) &&
                                    !checkInputMatch(mAdv.mCharacters,
                                            MReference.ReferencesType.Character,
                                            grpVal, nNewRefs - 1, 0,
                                            false, false, secondChance) &&
                                    !checkInputMatch(mAdv.mLocations,
                                            MReference.ReferencesType.Location,
                                            grpVal, nNewRefs - 1, 0,
                                            false, false, secondChance)) {
                                matched = false;
                                break checkMatches;
                            }
                            break;

                        case "direction1":
                        case "direction2":
                        case "direction3":
                        case "direction4":
                        case "direction5":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Direction);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            for (MAdventure.DirectionsEnum dr : MAdventure.DirectionsEnum.values()) {
                                String sDirTest = mAdv.getDirectionRE(dr);
                                if (grpVal.matches("^" + sDirTest + "$")) {
                                    MReference.MReferenceItem itm = new MReference.MReferenceItem();
                                    itm.mMatchingKeys.add(dr.toString());
                                    itm.mCommandReference = grpVal;
                                    newRef.mItems.add(itm);
                                    break;
                                }
                            }
                            break;

                        case "number1":
                        case "number2":
                        case "number3":
                        case "number4":
                        case "number5":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Number);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (grpVal.matches(PATTERN_NUMBER)) {
                                MReference.MReferenceItem itm = new MReference.MReferenceItem();
                                itm.mMatchingKeys.add(grpVal);
                                itm.mCommandReference = grpVal;
                                newRef.mItems.add(itm);
                            }
                            break;

                        case "text1":
                        case "text2":
                        case "text3":
                        case "text4":
                        case "text5":
                            nNewRefs++;
                            newRef = new MReference(MReference.ReferencesType.Text);
                            mRefs.set(nNewRefs - 1, newRef);
                            newRef.mRefMatch = grpName;
                            if (grpVal.matches(PATTERN_TEXT)) {
                                int iRef = Math.max(mAdv.safeInt(grpName.replace("text", "")) - 1, 0);
                                MReference.MReferenceItem itm = new MReference.MReferenceItem();
                                itm.mMatchingKeys.add(grpVal);
                                itm.mCommandReference = grpVal;
                                newRef.mItems.add(itm);
                                mAdv.mReferencedText[iRef] = grpVal;
                            }
                            break;
                    }
                }
            }

            if (matched) {
                return true;
            } else {
                // We didn't match, but...
                // if this task has a Must Exist restriction, add it to the second chance pool for
                // potential execution on a second pass.
                if (secondChance != null && !secondChance.containsKey(getKey()) &&
                        nNewRefs > 0 && nNewRefs <= mRefs.size()) {
                    MReference.ReferencesType refType = mRefs.get(nNewRefs - 1).mType;
                    if ((refType == MReference.ReferencesType.Object && hasObjectExistRestriction()) ||
                            (refType == MReference.ReferencesType.Character && hasCharacterExistRestriction()) ||
                            (refType == MReference.ReferencesType.Location && hasLocationExistRestriction())) {
                        secondChance.put(getKey(), this);
                    }
                }
            }
        }

        return false;
    }

    private boolean inputMatchesObjects(@NonNull String input, int refNum) {
        return inputMatchesObjects(input, refNum, true, false, null);
    }

    private boolean inputMatchesObjects(@NonNull String input, int refNum, boolean removeMatches) {
        return inputMatchesObjects(input, refNum, removeMatches, true, null);
    }

    private boolean inputMatchesObjects(@NonNull String input, final int refNum,
                                        boolean removeMatches, boolean matchMultiple,
                                        @Nullable MTaskHashMap secondChance) {
        // if matchMultiple is true, we only try to match one object
        // if matchMultiple is false, we try to match a list of objects
        if (!matchMultiple) {
            // First try to match the form
            //    "(all | objects1) (except|but|apart from) (objects2)"
            Matcher m = PATTERN_OBJECTS.matcher(input);
            grpCheck:
            if (m.matches()) {
                ArrayList<MReference.MReferenceItem> items = mRefs.get(refNum).mItems;
                MReference.MReferenceItem itm;

                for (String grpName : PATTERN_OBJECTS.groupNames()) {
                    String grpVal = m.group(grpName);
                    if (grpVal == null) {
                        // input didn't match this group name, so just
                        // continue onto the next one
                        continue;
                    }
                    grpVal = grpVal.trim();
                    switch (grpName) {
                        case "all":
                            if (grpVal.equals("all")) {
                                // User isn't refining 'all', so we need to populate list with all objects
                                for (MObject ob : mAdv.mObjects.getSeenBy().values()) {
                                    itm = new MReference.MReferenceItem();
                                    itm.mMatchingKeys.add(ob.getKey());
                                    itm.mIsExplicitlyMentioned = false;
                                    itm.mCommandReference = "all";
                                    items.add(itm);
                                }
                            } else {
                                // i.e. all balls
                                // object1 should be plural here, in which case we want to
                                // match any object with that as the plural, i.e. balls, cactii, sheep
                                if (!inputMatchesObjects(grpVal.substring(4),
                                        refNum, false)) {
                                    break grpCheck;
                                }
                            }
                            break;

                        case "objects1":
                            if (!grpVal.startsWith("all ")) {
                                // i.e. balls
                                // object1 should be plural here, in which case we want to
                                // match any object with that as the plural, i.e. balls, cactii, sheep
                                if (!inputMatchesObjects(grpVal, refNum, removeMatches)) {
                                    break grpCheck;
                                }
                            }
                            break;

                        case "objects2":
                            // Need to go through and remove any matching ref
                            inputMatchesObjects(grpVal, refNum);
                            break;
                    }
                }
                return true;
            }

            // No matches so far. See if we can match
            //    "(obj1, ... ) obj2 (and obj3)"
            m = PATTERN_OBJECTS2.matcher(input);
            if (m.matches()) {
                int itmNum = 0;
                String obs = m.group("commaseparatedobjects");
                if (obs != null && !obs.equals("")) {
                    // mimic VB's TrimEnd(New Char() { ","c, " "c }
                    for (String ob : obs.replaceAll("[\\s,]+$", "").split(",")) {
                        ob = ob.trim();
                        if (!checkInputMatch(mAdv.mObjects, MReference.ReferencesType.Object,
                                ob, refNum, itmNum,
                                removeMatches, false, null)) {
                            return false;
                        }
                        itmNum++;
                    }
                }
                if (!checkInputMatch(mAdv.mObjects, MReference.ReferencesType.Object,
                        m.group("object2"), refNum, itmNum,
                        removeMatches, false, null)) {
                    return false;
                }
                itmNum++;
                return checkInputMatch(mAdv.mObjects, MReference.ReferencesType.Object,
                        m.group("object3"), refNum, itmNum,
                        removeMatches, false, null);
            }
        }

        // Try to match on unique names before looking at plurals
        // So if we have bar and bars, get bars tries to take the bars before taking the bar
        return checkInputMatch(mAdv.mObjects, MReference.ReferencesType.Object,
                input, refNum, 0, removeMatches, matchMultiple, secondChance);
    }

    private boolean inputMatchesCharacters(@NonNull String input, int refNum,
                                           @Nullable MTaskHashMap secondChance) {
        Matcher m = PATTERN_CHARACTERS.matcher(input);
        if (m.matches()) {
            int itmNum = 0;
            String chars = m.group("commaseparatedcharacters");
            if (chars != null && !chars.equals("")) {
                // mimic VB's TrimEnd(New Char() { ","c, " "c }
                for (String ch : chars.replaceAll("[\\s,]+$", "").split(",")) {
                    ch = ch.trim();
                    if (!checkInputMatch(mAdv.mCharacters, MReference.ReferencesType.Character,
                            ch, refNum, itmNum,
                            false, false, null)) {
                        return false;
                    }
                    itmNum++;
                }
            }
            if (!checkInputMatch(mAdv.mCharacters, MReference.ReferencesType.Character,
                    m.group("character2"), refNum, itmNum,
                    false, false, null)) {
                return false;
            }
            itmNum++;
            return checkInputMatch(mAdv.mCharacters, MReference.ReferencesType.Character,
                    m.group("character3"), refNum, itmNum,
                    false, false, null);
        }

        return checkInputMatch(mAdv.mCharacters, MReference.ReferencesType.Character,
                input, refNum, 0, false, false, secondChance);
    }

    private boolean checkInputMatch(@NonNull MItemHashMap<? extends MItem> items,
                                    MReference.ReferencesType refType,
                                    @NonNull String input, int refNum, int itemNum,
                                    boolean removeMatches, boolean matchMultiple,
                                    @Nullable MTaskHashMap secondChance) {
        boolean matched = false;
        boolean refItemAdded = false;
        ArrayList<MReference.MReferenceItem> refItems = mRefs.get(refNum).mItems;

        if (itemNum == 0 && matchMultiple) {
            itemNum = -1;
        }

        for (MItem itm : items.values()) {
            Pattern p = Pattern.compile("^" +
                    itm.getRegEx(false, matchMultiple) +
                    "$", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(input);
            if (m.matches()) {
                matched = true;
                if (!removeMatches) {
                    if (matchMultiple) {
                        itemNum++;
                    }
                    if (matchMultiple || !refItemAdded) {
                        refItems.add(new MReference.MReferenceItem());
                    }
                    refItemAdded = true;
                    MReference.MReferenceItem refItem = refItems.get(itemNum);
                    refItem.mMatchingKeys.add(itm.getKey());
                    refItem.mIsExplicitlyMentioned = true;
                    refItem.mCommandReference = input;
                } else {
                    for (int i = refItems.size() - 1; i >= 0; i--) {
                        MReference.MReferenceItem refItem = refItems.get(i);
                        refItem.mMatchingKeys.remove(itm.getKey());
                        if (refItem.mMatchingKeys.size() == 0) {
                            refItems.remove(i);
                        }
                    }
                }
            }
        }

        if (!matched && secondChance != null &&
                secondChance.containsKey(getKey())) {
            // If our task has a check that objects should exist and this
            // is the second pass, then return true as a match so the task can
            // deal with that in its restrictions.
            switch (refType) {
                case Location:
                    if (hasLocationExistRestriction()) {
                        return true;
                    }
                    break;

                case Character:
                    if (hasCharacterExistRestriction()) {
                        return true;
                    }
                    break;

                case Object:
                    if (hasObjectExistRestriction()) {
                        return true;
                    }
                    break;
            }
        }

        return matched;
    }

    public boolean getContinueToExecuteLowerPriority() {
        return mContinueToExecuteLowerPriority;
    }

    private void setContinueToExecuteLowerPriority(boolean value) {
        mContinueToExecuteLowerPriority = value;
    }

    public boolean getRunImmediately() {
        return (mType == TaskTypeEnum.System && mRunImmediately);
    }

    public void setRunImmediately(boolean value) {
        mRunImmediately = value;
    }

    public String getLocationTrigger() {
        return mLocationTrigger;
    }

    private void setLocationTrigger(@NonNull String value) {
        mLocationTrigger = value;
    }

    public boolean getCompleted() {
        return mHasCompleted;
    }

    public void setCompleted(boolean value) {
        mHasCompleted = value;
    }

    @NonNull
    public String getDescription() {
        if (!mDescription.equals("")) {
            return mDescription;
        } else if (mCommands.size() > 0) {
            return mCommands.get(0);
        }
        return getKey();
    }

    public void setDescription(@NonNull String value) {
        mDescription = value;
    }

    /**
     * Looks at the references currently stored against this task and
     * attempts to refine them so that every associated item has only
     * one possible value (i.e. no ambiguities). This function should
     * be called after inputMatchesCommand() to ensure that the references
     * from the input string (if any) have been loaded into this task.
     *
     * @return TRUE if refinement was a success (or there were no references to
     * refine in the first place), FALSE otherwise.
     */
    public boolean matchesReferences(@NonNull MTaskHashMap.MTaskMatchResult matchResult) {
        // Does this task already have a unique set of references (or no
        // references at all)?
        boolean matchesPreRefine = true;
        for (MReference nr : mRefs) {
            if (nr != null) {
                for (MReference.MReferenceItem itm : nr.mItems) {
                    if (itm.mMatchingKeys.size() != 1) {
                        matchesPreRefine = false;
                        break;
                    }
                }
            }
        }

        // Remove any references that don't pass the task's restrictions
        // (or other scopes) and this becomes our working set. We don't
        // want to overwrite this task's references at this stage, so
        // operate on a copy of them instead.
        mRefsWorking = refineRefsUsingScopes(mRefs.copyDeep());

        // If we still have at least one matching possibility in each
        // reference item, then run this task, else we don't pass
        boolean okToRun = true;
        for (MReference nr : mRefsWorking) {
            if (nr != null) {
                for (MReference.MReferenceItem itm : nr.mItems) {
                    int nPossible = itm.mMatchingKeys.size();
                    if (nPossible == 0) {
                        // This item does not have any possible values, so
                        // we can't run this task as it stands. However, if
                        // we had a unique set of references prior to refining,
                        // let's fall back to that and execute the task, rather
                        // than falling through to not understood.
                        if (matchesPreRefine) {
                            // Yes we can fall back, so reset mRefsWorking.
                            mRefsWorking = mRefs.copyDeep();
                        } else {
                            // No we can't fall back, so give up.
                            mAdv.mView.debugPrint(Task, getKey(),
                                    High, "No matches found.");

                            if (matchResult.mNoRefTask == null) {
                                // Record that this is the first task with at
                                // least one reference that doesn't contain
                                // any possible values.
                                matchResult.mNoRefTask = this;
                            }
                            okToRun = false;
                        }
                        break;
                    } else if (nPossible > 1) {
                        // This item has more than one possible value, so
                        // there's an ambiguity.
                        if (matchResult.mAmbiguousTask == null) {
                            // Record that this is the first task with at
                            // least one reference that contains more than
                            // one possible value.
                            mAdv.mView.debugPrint(Task, getKey(), High,
                                    "Multiple matches.  Prompt for ambiguity.");
                            matchResult.mAmbiguousTask = this;
                        } else {
                            // We've already found an ambiguous task,
                            mAdv.mView.debugPrint(Task, getKey(), High,
                                    "Multiple matches, but we already have an ambiguity.");
                        }
                        okToRun = false;
                    }
                }
            }
        }

        if (okToRun) {
            if (getReferences().size() > 0) {
                // The task has references, but every associated item
                // has only one possible value, so there is no
                // ambiguity.
                mAdv.mView.debugPrint(Task, getKey(),
                        High, "Command matches without ambiguity.");
            } else {
                // The task has no references.
                mAdv.mView.debugPrint(Task, getKey(),
                        High, "Command matches.");
            }

            // We successfully refined the working references, so
            // set the result back to this task's references.
            mRefs = mRefsWorking;
        }

        return okToRun;
    }

    @NonNull
    public MStringArrayList getReferences() {
        return MGlobals.getRefs(getCommand(false));
    }

    @NonNull
    private String getParentTaskCommand(int cmdIndex) {
        // Returns the command from the parent task if we're a specific task.
        // Does this recursively in case we're specific of a specific etc.
        MTask task = this;
        while (task.mType == Specific) {
            task = mAdv.mTasks.get(task.mGeneralKey);
        }

        int nCmd = task.mCommands.size();
        if (nCmd > 0) {
            return (nCmd > cmdIndex) ?
                    task.mCommands.get(cmdIndex) : task.mCommands.get(0);
        } else {
            return "";
        }
    }

    @NonNull
    private String getCommand(boolean replaceSpecifics) {
        return getCommand(replaceSpecifics, -1);
    }

    /**
     * Attempt to find a command of this task that matches the given input.
     *
     * @param input             - input to match.
     * @param secondChanceTasks - if NULL, this does nothing. Otherwise, if
     *                          upon entry to this function, this hashmap
     *                          contains this task's key, this task will
     *                          automatically match. If the hashmap does
     *                          not contain this task's key, and the task
     *                          matches on the pattern but not the referred
     *                          object / character / location, and the task
     *                          has a Must Exist restriction, then it will
     *                          be added to the secondChanceMatches hashmap
     *                          as a possible candidate should the first
     *                          search fail to find an appropriate task.
     * @return if a matching command is found, return its index. Otherwise
     * return -1.
     */
    public int getMatchingCommand(@NonNull String input,
                                  @Nullable MTaskHashMap secondChanceTasks) {
        for (int i = 0; i < mCommands.size(); i++) {
            if (inputMatchesCommand(input, i, secondChanceTasks)) {
                mLastMatchingCommandIndex = i;
                return i;
            }
        }
        return -1;
    }

    @NonNull
    private String getCommand(boolean replaceSpecifics, int cmdIndex) {
        // if cmdIndex is -1, we use the index of the last matched command
        String taskCmd = "";
        int matchedCmdIndex = 0;
        if (cmdIndex == -1) {
            matchedCmdIndex = mLastMatchingCommandIndex;
        }

        switch (mType) {
            case General:
                // Find the command with the most refs.  E.g.
                // # Get Object
                // get %object%
                int nRefs = 0;
                if (mCommands.size() == 0) {
                    GLKLogger.error("Error, general task \"" + mDescription + "\" has no commands!");
                    return "";
                }
                taskCmd = (mCommands.size() > matchedCmdIndex) ?
                        mCommands.get(matchedCmdIndex) : mCommands.get(0);
                MStringArrayList refs = MGlobals.getRefs(taskCmd);
                if (refs.size() > nRefs) {
                    // Need to make sure we're checking against
                    // the command we matched on
                    nRefs = refs.size();
                }
                // Some v4 games may not have same no. of refs on each line,
                // so if any other lines have more, use that instead
                for (String cmd : mCommands) {
                    int nCmdRefs = MGlobals.getRefs(cmd).size();
                    if (nCmdRefs > nRefs) {
                        nRefs = nCmdRefs;
                        taskCmd = cmd;
                    }
                }
                break;

            case Specific:
                taskCmd = mAdv.mTasks.get(mGeneralKey).getCommand(replaceSpecifics);
                if (replaceSpecifics) {
                    // Replace any Specifics from this key
                    for (int i = 0; i < mSpecifics.size(); i++) {
                        MSpecific spec = mSpecifics.get(i);
                        if (spec.mKeys.size() == 0 ||
                                (spec.mKeys.size() == 1 && spec.mKeys.get(0).equals(""))) {
                            // Allow, as it's passing thru the parent as a reference
                        } else {
                            // Replace the parent task %object% with our specific key
                            taskCmd = taskCmd.replace(getReferences().get(i), spec.toString(mAdv));
                        }
                    }
                }
                break;

            case System:
                break;

        }

        return taskCmd;
    }

    @NonNull
    public MDescription getCompletionMessage() {
        return mCompletionMessage;
    }

    public void setCompletionMessage(@NonNull MDescription value) {
        mCompletionMessage = value;
    }

    @NonNull
    public MDescription getFailOverride() {
        return mFailOverride;
    }

    private void setFailOverride(@NonNull MDescription value) {
        mFailOverride = value;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int value) {
        mPriority = value;
        // TODO - rejig all the other priorities to insert this one
    }

    public int getAutoFillPriority() {
        return mAutoFillPriority;
    }

    private void setAutoFillPriority(int value) {
        mAutoFillPriority = value;
    }

    public boolean getRepeatable() {
        return mRepeatable;
    }

    public void setRepeatable(boolean value) {
        mRepeatable = value;
    }

    @NonNull
    private MStringArrayList getChildTasks() {
        return getChildTasks(false);
    }

    /**
     * Get the specific child tasks of this task.
     *
     * @param includeCompleted - should we include
     *                          child tasks that have already been completed?
     * @return a list of the child tasks.
     */
    @NonNull
    private MStringArrayList getChildTasks(boolean includeCompleted) {
        MStringArrayList sal = new MStringArrayList();

        for (MTask tas : mAdv.getTaskList(SpecificTasks).values()) {
            if (tas.mGeneralKey.equals(getKey()) &&
                    (includeCompleted || !tas.getCompleted() || tas.getRepeatable())) {
                sal.add(tas.getKey());
            }
        }
        Collections.sort(sal, new MTaskPrioritySortComparer());
        return sal;
    }

    @NonNull
    private String getParentTask() {
        return mGeneralKey;
    }

    private boolean hasObjectExistRestriction() {
        if (!mRestrictionCheckCache[0][0]) {
            for (MRestriction rest : mRestrictions) {
                if (rest.mType == MRestriction.RestrictionTypeEnum.Object &&
                        rest.mObjectType == MRestriction.ObjectEnum.Exist) {
                    mRestrictionCheckCache[0][1] = true;
                    break;
                }
            }
            mRestrictionCheckCache[0][0] = true;
            return mRestrictionCheckCache[0][1];
        } else {
            return mRestrictionCheckCache[0][1];
        }
    }

    private boolean hasCharacterExistRestriction() {
        if (!mRestrictionCheckCache[1][0]) {
            for (MRestriction rest : mRestrictions) {
                if (rest.mType == MRestriction.RestrictionTypeEnum.Character &&
                        rest.mCharacterType == MRestriction.CharacterEnum.Exist) {
                    mRestrictionCheckCache[1][1] = true;
                    break;
                }
            }
            mRestrictionCheckCache[1][0] = true;
            return mRestrictionCheckCache[1][1];
        } else {
            return mRestrictionCheckCache[1][1];
        }
    }

    private boolean hasLocationExistRestriction() {
        if (!mRestrictionCheckCache[2][0]) {
            for (MRestriction rest : mRestrictions) {
                if (rest.mType == MRestriction.RestrictionTypeEnum.Location &&
                        rest.mLocationType == MRestriction.LocationEnum.Exist) {
                    mRestrictionCheckCache[2][1] = true;
                }
            }
            mRestrictionCheckCache[2][0] = true;
            return mRestrictionCheckCache[2][1];
        } else {
            return mRestrictionCheckCache[2][1];
        }
    }

    public void fixOlderVars() {
        for (MRestriction rest : mRestrictions) {
            rest.fixOlderVars();
        }

        for (MAction act : mActions) {
            act.fixOlderVars();

            // If the task's completion message is set to display before actions
            // and it has a variable name in it which those actions modify,
            // then change it to display after actions instead.
            if (mDisplayCompletion == Before && act.mKey1 != null &&
                    (act.mType == SetVariable ||
                            act.mType == IncreaseVariable ||
                            act.mType == DecreaseVariable)) {
                String compl = getCompletionMessage().get(0).mDescription;
                String varName =
                        mAdv.getNameFromKey(act.mKey1, false,
                                false, false);
                if (varName != null && compl.contains(varName)) {
                    mDisplayCompletion = After;
                }

                // This is a hack to try to provide appropriate behaviour for
                // quite diverse situations, e.g:

                // RELUCTANT RESURRECTEE: Task10 (display score)
                //    Has compl msg
                //     "At the moment you have a score of %scor% out of a
                //      maximum possible score of 100. This is made up as
                //      follows:<br>"
                //    Has addtl msg ""
                //    And a sequence of actions that have completion text
                //    that prints score contributed by each completed task.
                //  Expects compl msg to display BEFORE actions.

                // RELUCTANT VAMPIRE: Task608 (last para of Maria interview)
                //    Has compl msg
                //     "... Maria jots something down. "Fine, fine. Now…" Another
                //      note. "Let's discuss the future, shall we?"scl%scl%'
                //    Has addtl msg ""
                //    And actions, including one that executes another task,
                //    Task567, which displays a description of the next location.
                //   Expects compl msg to display BEFORE actions.

                // PK GIRL: Task111 (get's player's name)
                //    Has compl msg
                //     ""%player_name%, was it?  No wonder I forgot.  Well,
                //       %player_name%, is it life i..."
                //    Has addtl msg ""
                //    And actions, including an action that sets the variable
                //    player_name to the text just entered.
                //  Expects compl msg to display AFTER actions.

                // TO HELL IN A HAMPER: Task125 (knock off hubert's hat)
                //    Has compl msg
                //      "... The clock immediately begins to chime..."
                //    Has addtl msg ""
                //    And an action that executes task Task124, which prints
                //    the chime message.
                //    Expects compl msg to display BEFORE actions.
            }
        }
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> all = new ArrayList<>();
        for (MRestriction r : mRestrictions) {
            all.add(r.mMessage);
        }
        all.add(getCompletionMessage());
        all.add(getFailOverride());
        return all;
    }

    @Nullable
    @Override
    public MTask clone() {
        MTask tas = (MTask) super.clone();
        if (tas != null) {
            tas.mCommands = tas.mCommands.clone();
            tas.mRestrictions = tas.mRestrictions.copy();
            tas.mActions = tas.mActions.copy();
            if (mSpecifics != null) {
                int sz = mSpecifics.size();
                tas.mSpecifics = new ArrayList<>(sz);
                for (int i = 0; i < sz; i++) {
                    tas.mSpecifics.add(mSpecifics.get(i).clone());
                }
            }
        }
        return tas;
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getDescription();
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        if (!mRestrictions.deleteKey(key)) {
            return false;
        }
        if (!mActions.deleteKey(key)) {
            return false;
        }
        for (MDescription d : getAllDescriptions()) {
            if (!d.deleteKey(key)) {
                return false;
            }
        }
        if (mGeneralKey.equals(key)) {
            mGeneralKey = "";
        }
        return true;
    }

    @Override
    protected int findLocal(@NonNull String toFind, @Nullable String toReplace,
                            boolean findAll, @NonNull int[] nReplaced) {
        int iCount = nReplaced[0];
        String[] t = new String[1];
        t[0] = mDescription;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        mDescription = t[0];
        return nReplaced[0] - iCount;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int ret = 0;
        ret += mRestrictions.getNumberOfKeyRefs(key);
        ret += mActions.getNumberOfKeyRefs(key);
        for (MDescription d : getAllDescriptions()) {
            ret += d.getNumberOfKeyRefs(key);
        }
        if (mGeneralKey.equals(key)) {
            ret++;
        }
        return ret;
    }

    @NonNull
    @Override
    public String getSymbol() {
        // ballot box with check (emoji version)
        return "\u2611\ufe0f";
    }

    public enum ExecutionStatus {
        ContinueExecuting,
        HasOutput
    }

    public enum BeforeAfterEnum {
        Before,     // 0
        After       // 1
    }

    public enum TaskTypeEnum {
        General,    // 0
        Specific,   // 1
        System      // 2
    }

    public enum SpecificOverrideTypeEnum {
        BeforeTextAndActions,   // 0
        BeforeActionsOnly,      // 1
        BeforeTextOnly,         // 2
        Override,               // 3
        AfterTextOnly,          // 4
        AfterActionsOnly,       // 5
        AfterTextAndActions     // 6
    }

    public enum SetTasksEnum {
        Execute,                    // 0
        Unset                       // 1
    }

    private static class V3_8_InitTaskInfo {
        int[] wearObjs = new int[2];
        int[] holdObjs = new int[3];
        int obj1;
        int task;
        boolean taskNotDone;
        String taskMsg;
        String holdMsg;
        String wearMsg;
        String companyMsg;
        boolean notInSameRoom;
        int NPC;
        String obj1Msg;
        int obj1Room;
        boolean holdingSameRoom;
        int obj2;
        int obj2Var1;
        int obj2Var2;
        String obj2Msg;
    }

    public static class MSpecific implements Cloneable {
        public boolean mMultiple;
        @NonNull
        public MStringArrayList mKeys = new MStringArrayList();
        MReference.ReferencesType mType = MReference.ReferencesType.Object;

        public MSpecific() {
            super();
        }

        MSpecific(@NonNull XmlPullParser xpp) throws IOException, XmlPullParserException {
            this();

            xpp.require(START_TAG, null, "Specific");

            int depth = xpp.getDepth();
            int eventType;

            while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
                if (eventType == START_TAG) {
                    switch (xpp.getName()) {
                        case "Type":
                            mType = MReference.ReferencesType.valueOf(xpp.nextText());
                            break;

                        case "Multiple":
                            mMultiple = getBool(xpp.nextText());
                            break;

                        case "Key":
                            mKeys.add(xpp.nextText());
                            break;
                    }
                }
            }
            xpp.require(END_TAG, null, "Specific");
        }

        @NonNull
        public String toString(MAdventure adv) {
            StringBuilder ret = new StringBuilder();
            int sz = mKeys.size();
            if (sz > 0) {
                for (int i = 0; i < sz; i++) {
                    switch (mType) {
                        case Direction:
                            if (!mKeys.get(i).equals("")) {
                                ret.append(adv.getDirectionName(MAdventure.DirectionsEnum.valueOf(mKeys.get(i))));
                            }
                            break;
                        default:
                            ret.append(adv.getNameFromKey(mKeys.get(i), false, false));
                            break;
                    }
                    if (i + 2 < sz) {
                        ret.append(", ");
                    }
                    if (i + 2 == sz) {
                        ret.append(" and ");
                    }
                }
            }
            return ret.toString();
        }

        @Override
        @NonNull
        public MSpecific clone() {
            MSpecific spec = new MSpecific();
            spec.mType = mType;
            spec.mMultiple = mMultiple;
            spec.mKeys = mKeys.clone();
            return spec;
        }
    }

    public static class MTaskState {
        public String mKey;
        boolean mCompleted;
        boolean mScored;
        @NonNull
        public final HashMap<String, Boolean> mDisplayedDescriptions = new HashMap<>();

        MTaskState(@NonNull MTask tas) {
            mKey = tas.getKey();
            mCompleted = tas.getCompleted();
            mScored = tas.mIsScored;
            saveDisplayOnce(tas.getAllDescriptions(), mDisplayedDescriptions);
        }

        MTaskState(@NonNull XmlPullParser xpp) throws Exception {
            xpp.require(START_TAG, null, "Task");

            int depth = xpp.getDepth();
            int evType;

            while ((evType = xpp.nextTag()) != END_DOCUMENT && xpp.getDepth() > depth) {
                if (evType == START_TAG) {
                    switch (xpp.getName()) {
                        case "Key": {
                            mKey = xpp.nextText();
                            break;
                        }
                        case "Completed": {
                            mCompleted = cbool(xpp.nextText());
                            break;
                        }
                        case "Scored": {
                            mScored = cbool(xpp.nextText());
                            break;
                        }
                        case "Displayed": {
                            mDisplayedDescriptions.put(xpp.nextText(), true);
                            break;
                        }

                    }
                }
            }

            xpp.require(END_TAG, null, "Task");
        }

        public void serialize(@NonNull XmlSerializer xs) throws IOException {
            xs.startTag(null, "Task");

            xs.startTag(null, "Key");
            xs.text(mKey);
            xs.endTag(null, "Key");

            xs.startTag(null, "Completed");
            xs.text(String.valueOf(mCompleted));
            xs.endTag(null, "Completed");

            xs.startTag(null, "Scored");
            xs.text(String.valueOf(mScored));
            xs.endTag(null, "Scored");

            for (String descKey : mDisplayedDescriptions.keySet()) {
                xs.startTag(null, "Displayed");
                xs.text(descKey);
                xs.endTag(null, "Displayed");
            }

            xs.endTag(null, "Task");
        }

        public void restore(@NonNull MTask tas) {
            tas.setCompleted(mCompleted);
            tas.mIsScored = mScored;
            restoreDisplayOnce(tas.getAllDescriptions(), mDisplayedDescriptions);
        }
    }

    private class MTaskPrioritySortComparer implements Comparator<String> {
        @Override
        public int compare(@NonNull String x, @NonNull String y) {
            return (mAdv.mTasks.get(x).getPriority() - mAdv.mTasks.get(y).getPriority());
        }
    }
}
