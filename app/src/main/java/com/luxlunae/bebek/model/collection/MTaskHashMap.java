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

package com.luxlunae.bebek.model.collection;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MProperty;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static com.luxlunae.bebek.MGlobals.ItemEnum.Task;
import static com.luxlunae.bebek.MGlobals.containsWord;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.model.MAdventure.TaskExecutionEnum.HighestPriorityPassingTask;
import static com.luxlunae.bebek.model.MAdventure.TaskExecutionEnum.HighestPriorityTask;
import static com.luxlunae.bebek.model.MTask.LIBRARY_START_TASK_PRIORITY;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.High;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;

public class MTaskHashMap extends MItemHashMap<MTask> {
    @NonNull
    private final MAdventure mAdv;

    /**
     * This variable is used to cache the details of the last failing
     * non-library task, in the event that we are in execution mode
     * HighestPriorityPassingTask. If by the end of the task check loop
     * this variable is not null, we will automatically fall back to
     * the details it stores. It needs to retain state across multiple
     * calls to find() and should only be reset when find is called
     * with minPriority = 0 (i.e. restart search from the task with
     * the highest priority).
     */
    private MTaskMatchResult mNonLibFail = null;

    public MTaskHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader, double version,
                     int nLocs, int startLocs, int startTasks, int startChars,
                     int startMaxPriority, final HashMap<String, String> dictDodgyStates,
                     final HashMap<MObject, MProperty> dodgyArlStates,
                     @NonNull final String[] locNames) throws EOFException {
        int nTasks = cint(reader.readLine());
        for (int i = 1; i <= nTasks; i++) {
            MTask task = new MTask(mAdv, reader, i, version, nLocs,
                    startLocs, startTasks, startChars,
                    startMaxPriority, dictDodgyStates, dodgyArlStates, locNames);
            put(task.getKey(), task);
            if (task.mReversedTask != null) {
                put(task.mReversedTask.getKey(), task.mReversedTask);
                task.mReversedTask = null;
            }
        }
    }

    public void createTaskReferenceLists() {
        for (MTask task : values()) {
            task.mRefs = new MReferenceList(mAdv, task.getReferences().size());
        }
    }

    public void fixOlderVars() {
        for (MTask tas : values()) {
            tas.fixOlderVars();
        }
    }

    /**
     * Find the next task in this hash table that has a priority greater
     * than or equal to the given minimum priority, a command matching
     * the given input string and that passes all of its restrictions. Tasks
     * are checked in ascending order of priority (i.e. 1 is highest priority).
     *
     * @param input             - the input string to match.
     * @param minPriority       - only tasks with a priority the greater than or equal to
     *                          this number will be considered.
     * @param secondChance - may be NULL. If not NULL, when this function returns
     *                          this hash map will contain tasks that should be considered
     *                          on a second pass.
     * @return the key of the matching task, if there is one, otherwise NULL.
     */
    @NonNull
    public MTaskMatchResult find(@NonNull final String input, final int minPriority,
                                 @Nullable MTaskHashMap secondChance) {
        MTaskMatchResult ret = new MTaskMatchResult();

        // Tasks that are marked as "LowPriority" and have a priority greater than
        // this value will not execute. This value is the maximum priority number
        // of all failing tasks that match the user input and produce output.
        // Because Adrift executes tasks in increasing order of priority
        // (1 = highest priority) you can also think of that as the lowest
        // priority failing task with output that is not explicitly marked as
        // "LowPriority".
        int priorityFail = Integer.MAX_VALUE;

        if (minPriority == 0) {
            // We are starting a search again from the beginning, so
            // reset our non-library failing task with output tracker.
            mNonLibFail = null;
        }

        if (minPriority > 0 && minPriority < priorityFail) {
            // Because if we're continuing on from an earlier execution,
            // we shouldn't run low priority tasks.
            priorityFail = minPriority;
        }

        if (secondChance != null) {
            // If the caller has passed in another hash table
            // they want us to fill it - ensure its empty first.
            secondChance.clear();
        }

        try {
            //-------------------------------------------------------------------------
            //              Iterate through each task in this hash map
            //-------------------------------------------------------------------------
            // Check each task, starting from the one with the lowest number in the
            // "Task Priority:" field on its "Advanced" page and proceeding in order
            // until we find a match between the player's input and one of the command
            // lines of a task, using "Advanced Command Construction" matching.
            for (MTask tas : values()) {
                int p = tas.getPriority();
                if (p >= minPriority && !(tas.mIsLowPriority && p > priorityFail)) {
                    if (mNonLibFail != null &&
                            tas.getIsLibrary() && mAdv.mVersion < 5) {
                        //-------------------------------------------------------------
                        //   Don't continue into the library tasks - break the loop
                        //-------------------------------------------------------------
                        // Don't run library tasks if the game is version 3.8, 3.9 or
                        // 4.0 and we have already found a matching failing non-library
                        // task with output. Note: unlike the original Adrift code, we
                        // do run library tasks for v4 or earlier continuation tasks IF
                        // we have not yet found a matching non-library passing task or
                        // failing task with output. Otherwise we have problems with
                        // continuation library tasks *within* the library (not
                        // continuation across non-library / library divide).
                        //
                        // E.g. if the player types "sit" and then "e" (assuming east
                        // direction exists), the standard library task StandBeforeMoving
                        // should run first. Then, because the mContinueToExecuteLowerPriority
                        // flag of StandBeforeMoving is true, find() get called again to
                        // discover and execute PlayerMovement. That won't happen for v4
                        // and earlier games unless we allow continuation tasks within
                        // the library block (but not across the non-library library
                        // boundary).
                        break;
                    }

                    //-----------------------------------------------------------------
                    //             Do any of this task's commands match?
                    //-----------------------------------------------------------------
                    int iCmd = tas.getMatchingCommand(input, secondChance);
                    if (iCmd > -1) {
                        mAdv.mView.debugPrint(Task, tas.getKey(), Medium,
                                "Task '" + tas.getDescription() + "' matches input.");
                        //-------------------------------------------------------------
                        //                YES... do the references match?
                        //-------------------------------------------------------------
                        // E.g. If the command contains an %object% reference then
                        // ADRIFT searches through all of the objects in the game
                        // trying to find one which matches what the player typed
                        // in that part of the input. Note that the player may have
                        // entered the names of several objects, e.g. the watch and
                        // the pendant" or "all except the hat" or "pens" (which can
                        // mean all objects with "pen" as the noun if no objects are
                        // called "pens"). All of these must match items in the game.
                        if (tas.matchesReferences(ret)) {
                            //---------------------------------------------------------
                            //        YES... do the restrictions all pass?
                            //---------------------------------------------------------
                            // Check whether the task passes its restrictions, given
                            // the matched references, if any.
                            boolean passes =
                                    tas.mRestrictions.passes(false, tas.mRefs);
                            String failOverride = tas.getFailOverride().toString();

                            if (!passes && !failOverride.equals("") &&
                                    mAdv.mRestrictionText.equals("") &&
                                    containsWord(input, "all")) {
                                // If the task doesn't pass but it had some failed
                                // output text and: (1) we haven't already encountered
                                // a failing task with output on this call to find();
                                // and (2) the user input had the word "all", then set
                                // our global failed text to this output.
                                mAdv.mRestrictionText = failOverride;
                            }

                            if (passes || !mAdv.mRestrictionText.equals("")) {
                                //----------------------------------------------------
                                // The matched task either passes or fails with output.
                                //-----------------------------------------------------
                                if (passes) {
                                    if (tas.getPriority() > priorityFail) {
                                        mAdv.mView.debugPrint(Task, tas.getKey(), Medium,
                                                "Task passes restrictions and " +
                                                        "overrides previous failing task output");
                                    } else {
                                        mAdv.mView.debugPrint(Task, tas.getKey(), Medium,
                                                "Task passes restrictions.");
                                    }
                                } else if (!mAdv.mRestrictionText.equals("")) {
                                    mAdv.mView.debugPrint(Task, tas.getKey(), Medium,
                                            "Task doesn't pass restrictions, but is " +
                                                    "current highest priority failing task " +
                                                    "with restriction output.");
                                    if (mAdv.mTaskExecutionMode == HighestPriorityPassingTask) {
                                        if (!tas.getIsLibrary() &&
                                                tas.getPriority() < LIBRARY_START_TASK_PRIORITY) {
                                            // This is the highest priority non-library failing
                                            // task with output Cache the details so we can revert
                                            // back later if need be.
                                            mAdv.mView.debugPrint(Task, tas.getKey(), Medium,
                                                    "Task is also current highest " +
                                                            "priority non-library failing " +
                                                            "task with restriction output.");
                                            if (mNonLibFail == null) {
                                                // Note: this is the only place where mNonLibFail
                                                // is initialised - therefore if later we find it
                                                // is not null we also know that the task
                                                // execution mode is HighestPriorityPassingTask.
                                                mNonLibFail = new MTaskMatchResult();
                                            }
                                            mNonLibFail.mTask = tas;
                                        }
                                    }
                                    priorityFail = tas.getPriority();
                                }

                                mAdv.mView.debugPrint(Task, tas.getKey(), High,
                                        "Task priority: " + tas.getPriority());
                                ret.mTask = tas;

                                if (passes || mAdv.mTaskExecutionMode == HighestPriorityTask) {
                                    //--------------------------------------------------------
                                    //   We have found the task to execute - break the loop
                                    //--------------------------------------------------------
                                    // The task is either a:
                                    //
                                    // (1) a matching passing task; OR
                                    //
                                    // (2) a matching (non-library) failing task with
                                    //     output, and we are currently running in execution
                                    //     mode HighestPriorityTask.
                                    //
                                    // Note that HighestPriorityTask will "execute the highest
                                    // priority matching task whether it passes restrictions or
                                    // not, and lower priority tasks are only used if that task
                                    // has no text output at all, or if the task's
                                    // mContinueToExecuteLowerPriority flag is true."
                                    //
                                    // Whereas for HighestPriorityPassingTask "tasks that pass
                                    // restrictions override higher priority tasks that do not,
                                    // even when the failing higher priority task has output."
                                    //
                                    // We extend this definition from the ADRIFT 5 documentation
                                    // as follows. If we do not find a passing task then fall back
                                    // to: (i) the highest priority non-library failing task with
                                    // output; then (ii) the highest priority library failing task
                                    // with output; then (iii) give up. This is important because
                                    // occasionally Adrift 4 games (at least) implicitly assume
                                    // this behaviour. For example, Campbell Wild's Haunted House
                                    // assumes this behaviour in the implementation of "open gate"
                                    // (Task2) in the start location - if the player has not found
                                    // the treasure chest the game is supposed to print a fail
                                    // message saying "you do not want to be branded a coward do
                                    // you?" Instead, if we do not implement this non-library
                                    // failure fallback behaviour, we get the failure message for
                                    // the library command OpenObject - which is "the gate cannot
                                    // be opened or closed!"
                                    //
                                    mNonLibFail = null; // cancel any potential fall back
                                    break;
                                }
                            } else {
                                mAdv.mView.debugPrint(Task, tas.getKey(), Medium,
                                        "Task does not pass restrictions.");
                            }
                        }
                    }
                }
            }

            if (mNonLibFail != null) {
                // We didn't find a passing task but we have found at least one
                // failing non-library task with output, so fall back to that
                // rather than potentially using a library failing task with output.
                ret.mTask = mNonLibFail.mTask;
            }

            if (ret.mTask == null && ret.mNoRefTask != null) {
                // We matched on a task, but didn't find any references,
                // so just return what we do have.
                ret.mTask = ret.mNoRefTask;
            }
        } catch (Exception ex) {
            mAdv.mView.errMsg("MTaskHashTable: get error", ex);
            //ex.printStackTrace();
        }

        return ret;
    }

    @Override
    @Nullable
    public MTask put(@NonNull String key, @NonNull MTask task) {
        if (this == mAdv.mTasks) {
            mAdv.mAllItems.put(task);
        }
        return super.put(key, task);
    }

    @Override
    @Nullable
    public MTask remove(Object key) {
        if (this == mAdv.mTasks) {
            mAdv.mAllItems.remove(key);
        }
        return super.remove(key);
    }

    public void sort() {
        // Because this ultimately extends LinkedHashMap (which defines an
        // order to match the insertion order), the only way we can sort
        // is to sort a temporary array then reinsert the values in the
        // sorted order.
        ArrayList<Entry<String, MTask>> tmp = new ArrayList<>(entrySet());
        clear();
        Collections.sort(tmp, new Comparator<Entry<String, MTask>>() {
            @Override
            public int compare(Entry<String, MTask> x, Entry<String, MTask> y) {
                return x.getValue().getPriority() - y.getValue().getPriority();
            }
        });
        for (Entry<String, MTask> e : tmp) {
            put(e.getKey(), e.getValue());
        }
    }

    public static class MTaskMatchResult {
        @Nullable
        public MTask mTask = null;
        @Nullable
        public MTask mAmbiguousTask;
        @Nullable
        public MTask mNoRefTask;
    }
}
