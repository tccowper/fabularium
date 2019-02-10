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

package com.luxlunae.bebek.controller;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MDescription;
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MSynonym;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.model.MWalk;
import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MOrderedHashMap;
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.collection.MTaskHashMap;
import com.luxlunae.bebek.view.MView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;

import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Definite;
import static com.luxlunae.bebek.MGlobals.ItemEnum.General;
import static com.luxlunae.bebek.MGlobals.ItemEnum.Task;
import static com.luxlunae.bebek.MGlobals.containsWord;
import static com.luxlunae.bebek.MGlobals.replaceWord;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.controller.MSystemTask.executeSystemTask;
import static com.luxlunae.bebek.model.MAction.EndGameEnum.Running;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Subjective;
import static com.luxlunae.bebek.model.MAdventure.MTasksListEnum.GeneralTasks;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TimeBased;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TurnBased;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.High;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Low;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;
import static com.luxlunae.bebek.view.MView.debugPrint;
import static com.luxlunae.bebek.view.MView.displayText;
import static com.luxlunae.bebek.view.MView.updateStatusBar;

public class MParser {
    @NonNull
    public static String mRouteErrorText = "";
    public static boolean mEventsRunning = false;
    @NonNull
    public static String mInput = "";
    @NonNull
    public static MOrderedHashMap<String, MReferenceList> mPassResponses = new MOrderedHashMap<>();
    @NonNull
    public static MOrderedHashMap<String, MReferenceList> mFailResponses = new MOrderedHashMap<>();
    @Nullable
    public static MReferenceList mReferences;
    @NonNull
    public static String mRestrictionText = "";
    @NonNull
    private static String mLastProperInput = "";
    @NonNull
    private static String mRememberedVerb = "";
    @Nullable
    private static MTask mAmbiguousTask;
    @Nullable
    private static HashSet<String> mKnownWords;

    /**
     * Process the user's next input command in the game.
     *
     * @param adv1  - the Adventure model.
     * @param input - the input to process.
     * @throws InterruptedException if, e.g., the player presses the back key.
     */
    public static void processInput(@NonNull MAdventure[] adv1,
                                    @NonNull String input) throws InterruptedException {
        MAdventure adv = adv1[0];

        MView.mNoDebug = false;
        MView.mDebugIndent = 0;
        adv.mJustRunSystemTask = false;
        mInput = input;
        if (evaluateInput(adv1, 0, false).equals("***SYSTEM***")) {
            MView.mOutputText.setLength(0);
            return;
        }
        MView.mNoDebug = true;

        checkEndOfGame(adv);
        if (adv.mGameState != Running) {
            return;
        }

        adv.prepareForNextTurn();
        adv.getPlayer().doWalk();
    }

    /**
     * Evaluate an input string and return the associated game output.
     * The algorithm roughly proceeds as follows:
     * <p>
     * (i) replace any "it", "them", "him" and "her" words in the user
     * string with the object or character the player last referred to;
     * <p>
     * (ii) try to use the input to resolve any outstanding ambiguous
     * task from the last turn, for which we asked a question "do you
     * mean X, Y, or Z?";
     * <p>
     * (iii) if we now have a resolved ambiguous task, run it, otherwise
     * try to find a completable general task that has a command
     * matching the player's input, and that passes its restrictions, and
     * run that instead;
     * <p>
     * (iv) if we didn't find any tasks to run in step (iii), display a not
     * understood message;
     * <p>
     * (v) if we didn't just run a system command, update the status bar.
     *
     * @param adv1                - the Adventure model.
     * @param minPriority         - only tasks with a priority above this number will be
     *                            considered for execution.
     * @param runPassingTasksOnly - only execute a task that passes. Means we already
     *                            have a failing task with output, so don't need another.
     * @return the game's output.
     * @throws InterruptedException if, e.g., the player presses the back key.
     */
    @NonNull
    public static String evaluateInput(@NonNull MAdventure[] adv1, int minPriority,
                                       boolean runPassingTasksOnly) throws InterruptedException {
        debugPrint(General, "", High,
                "evaluateInput " + minPriority + ", " + runPassingTasksOnly);

        MAdventure adv = adv1[0];

        // ------------------------------------------------------------------------
        // The first thing the parser does is replace the words "it", "them", "him"
        // and "her" with the appropriate object or character that the player most
        // recently referred to.
        // ------------------------------------------------------------------------
        if (minPriority == 0) {
            // For the Fabularium port, we don't redisplay the input line
            // in the output line as text is composed in-place rather than
            // in a separate window like the original Windows version.
            mInput = mInput.toLowerCase();

            // Replace "it", "them", "him" and "her".
            replaceInputWord(adv, "it", adv.mIt);
            replaceInputWord(adv, "them", adv.mThem);
            replaceInputWord(adv, "him", adv.mHim);
            replaceInputWord(adv, "her", adv.mHer);

            // If mInput is "again", then replace it with the last command just run.
            int nCmds = adv.mCommands.size();
            if ((mInput.equals("again") || mInput.equals("g")) && nCmds > 1) {
                String lastCmd = adv.mCommands.get(nCmds - 2);
                if (lastCmd != null) {
                    displayText(adv, "<c>(" + lastCmd + ")</c><br>", true);
                    mInput = lastCmd;
                    adv.mCommands.remove(nCmds - 1); // Don't store 'again'
                }
            }

            // Get the new "it", "them", "him" and "her".
            grabIt(adv, mInput);

            // Replace synonyms.
            String preSyn = mInput;
            for (MSynonym syn : adv.mSynonyms.values()) {
                for (String from : syn.getChangeFrom()) {
                    mInput = replaceWord(mInput, from, syn.getChangeTo());
                }
            }
            if (!mInput.equals(preSyn)) {
                debugPrint(General, "", Medium,
                        "Synonyms changed input \"" + preSyn + "\" to \"" + mInput + "\"");
            }

            // If cbool(GetSetting("ADRIFT", "Runner", "BlankLine", "0")) Then Display(vbCrLf)
        }

        mInput = mInput.toLowerCase();

        // Don't actually respond to system tasks here, in case the user has created a task
        // to override the system one.
        if (adv.mGameState != Running || adv.mVersion < 5) {
            boolean ret = executeSystemTask(adv1, mInput, true);
            adv = adv1[0];
            if (ret) {
                return "";
            } else {
                if (adv.mGameState != Running) {
                    displayText(adv, "Please give one of the answers above." + "\n");
                    return "";
                }
            }
        }

        // ------------------------------------------------------------------------
        //    See if we need to resolve an ambiguous task from the previous turn.
        // ------------------------------------------------------------------------
        MTask taskToRun = null;
        if (mAmbiguousTask != null) {
            // --------------------------------------------------------------------
            // We have an ambiguous task from the last turn that we asked a
            // question for. See if the user's response is enough to resolve it.
            // --------------------------------------------------------------------
            if (mAmbiguousTask.resolveAmbiguity(adv, mInput)) {
                // We managed to resolve it.
                taskToRun = mAmbiguousTask;
                mRestrictionText = "";
            }
        } else {
            // --------------------------------------------------------------------
            // There is no ambiguity to resolve.
            // --------------------------------------------------------------------
            mLastProperInput = mInput;
        }

        if (taskToRun == null) {
            // --------------------------------------------------------------------
            // We don't have a resolved ambiguous task to run, so let's look for a
            // general task that matches the player's input.
            // --------------------------------------------------------------------
            for (int i = 0; i < 5; i++) {
                adv.mReferencedText[i] = "";
            }

            MTask lastAmbTask = null;
            if (mAmbiguousTask != null) {
                // We have an unresolved ambiguous task from a previous turn.
                // Back it up, so we can restore it if we fail to find a matching
                // completable general task in the next step below.
                lastAmbTask = mAmbiguousTask;
            }

            // --------------------------------------------------------------------
            // Find the next completable general task that has a command matching
            // the player's input, if any.
            // --------------------------------------------------------------------
            MTaskHashMap secondChanceTasks = new MTaskHashMap(adv);
            MTaskHashMap.MTaskMatchResult match =
                    adv.mCompletableTasks.find(mInput, minPriority, secondChanceTasks);
            if (match.mTask == null && match.mAmbiguousTask == null) {
                // We didn't find any tasks (either ambiguous or unambiguous).
                // Let's go back and see if one of our 'exist' tasks worked.
                debugPrint(Task, "", Medium,
                        "No matches found.  Checking again using existence.");
                match = secondChanceTasks.find(mInput, minPriority, null);
            }
            if (adv.mReferencedText[0].equals("")) {
                adv.mReferencedText[0] = mInput;
            }

            // --------------------------------------------------------------------
            // Did we find one?
            // --------------------------------------------------------------------
            if (match.mTask == null) {
                // ---------------------------------------------------
                //   Nope. Should we display an ambiguity question?
                // ---------------------------------------------------
                if (match.mAmbiguousTask != null &&
                        minPriority > 0 && MView.mOutputText.length() > 0) {
                    // -------------------------------------------------------
                    // Suppress ambiguity question if we're running further
                    // down task list and we already have a failure response.
                    // -------------------------------------------------------
                    match.mAmbiguousTask = null;
                }
                if (match.mAmbiguousTask == null && lastAmbTask != null) {
                    // -------------------------------------------------------
                    // We didn't find an ambiguous task either, but we did
                    // have one from the previous turn, so use that for the
                    // ambiguity question.
                    // -------------------------------------------------------
                    match.mAmbiguousTask = lastAmbTask;
                }
            }
            mAmbiguousTask = match.mAmbiguousTask;
            taskToRun = match.mTask;
        }

        if (mAmbiguousTask != null && taskToRun == null) {
            // --------------------------------------------------------------------
            // We didn't find any unambiguous tasks but we did find an ambiguous
            // general task with more than one matching reference - display an
            // ambiguity question.
            // --------------------------------------------------------------------
            if (!mAmbiguousTask.displayAmbiguityQuestion(mLastProperInput)) {
                // -------------------------------------------------------
                // We are unable to resolve the ambiguity and are not
                // expecting a response from the player to help us. So
                // just clear it and wait for the next command.
                // -------------------------------------------------------
                mAmbiguousTask = null;
            }
        } else {
            // --------------------------------------------------------------------
            // We either didn't find any tasks (ambiguous or unambiguous), or we
            // found an unambiguous task so can safely ignore any ambiguous task
            // that was also found.
            // --------------------------------------------------------------------
            mAmbiguousTask = null;
            if (taskToRun != null) {
                // ----------------------------------------------------------------
                //       Great, we found an unambiguous task for execution.
                // ----------------------------------------------------------------
                // ADRIFT has now matched all of the objects, characters and/or
                // locations that the player mentioned to the references in the
                // command, so it can start to execute the task. If there are
                // multiple items for a reference then ADRIFT executes the task
                // once for each item, but only displays the text box once.
                // Functions in the text box which have a different result are
                // combined into a list, so if we run the task for the objects "a
                // blue pen", "a gold coin" and "a flower" then the text "You
                // pick up %object%.Name" is displayed as "You pick up the blue
                // pen, the gold coin and the flower"
                mReferences = taskToRun.mReferencesWorking;
                EnumSet<MTask.ExecutionStatus> curStatus =
                        EnumSet.noneOf(MTask.ExecutionStatus.class);

                // Try to execute it
                taskToRun.attemptToExecute(false,
                        false, curStatus, false,
                        runPassingTasksOnly, false);

                if (taskToRun.mIsSystemTask) {
                    adv.mJustRunSystemTask = true;
                }

                // Try to execute any additional tasks
                while (adv.mTasksToRun.size() > 0) {
                    MTask tas = adv.mTasksToRun.remove();
                    tas.attemptToExecute(true);
                }

                if (minPriority == 0) {
                    if (MView.mOutputText.length() == 0) {
                        // The task didn't produce any output so display the
                        // "not understood" message.
                        displayNotUnderstood(adv1);
                    } else {
                        // The task executed successfully. If it was not a
                        // system task, count this as a turn.
                        if (!adv.mJustRunSystemTask) {
                            // Any Events that are running are moved forward
                            // one turn, which could display text or execute
                            // a system task.
                            incrementTurnOrTime(adv, TurnBased);
                        }
                        adv.setChanged(true);
                        mRememberedVerb = "";
                    }
                }
            } else {
                // ----------------------------------------------------------------
                //       We didn't find any tasks (ambiguous or unambiguous).
                // ----------------------------------------------------------------
                if (minPriority == 0) {
                    if (adv.mGameState == Running) {
                        // See if we can interpret the user's input as a
                        // system task...
                        boolean ret = executeSystemTask(adv1, mInput, false);
                        adv = adv1[0];
                        if (!ret) {
                            // Nope - display the "not understood" message.
                            displayNotUnderstood(adv1);
                        }
                        adv.mJustRunSystemTask = true;
                    }
                }
            }

            if (minPriority == 0 &&
                    !MView.mOutputText.toString().equals("***SYSTEM***")) {
                // If we didn't just run a system task, advance to the next line.
                displayText(adv, "\n");
            }
        }

        if (!MView.mOutputText.toString().equals("***SYSTEM***")) {
            if (minPriority == 0) {
                // ----------------------------------------------------------------
                // Move onto the next line in readiness for user's next input.
                // This turn is complete.
                // ----------------------------------------------------------------
                displayText(adv, "\n", true);
                debugPrint(General, "", Low, "ENDOFTURN");
            }
            updateStatusBar(adv);
        }

        return MView.mOutputText.toString();
    }

    /**
     * Processes a turn or time increment.
     * <p>
     * For turn increments, we increment all walks and events. For
     * time increments, we increment events, flush any output
     * immediately and check for end of game.
     *
     * @param adv    - the Adventure object.
     * @param evType - whether the increment is a turn or time increment.
     * @throws InterruptedException if, e.g. user presses back key,
     */
    public static void incrementTurnOrTime(@NonNull MAdventure adv,
                                           @NonNull MEvent.EventTypeEnum evType) throws InterruptedException {
        if (adv.mGameState != Running) {
            return;
        }

        // =============================================================
        //                PROCESS WALKS (TURN INCREMENT)
        // -------------------------------------------------------------
        if (evType == TurnBased) {
            for (MCharacter c : adv.mCharacters.values()) {
                for (MWalk w : c.mWalks) {
                    if (adv.mGameState != Running) {
                        return;
                    }
                    w.incrementTimer();
                }
            }
        }

        // =============================================================
        //      PROCESS TURN OR TIME EVENTS (TURN OR TIME INCREMENT)
        // -------------------------------------------------------------
        mEventsRunning = true;
        for (MEvent e : adv.mEvents.values()) {
            if (adv.mGameState != Running) {
                return;
            }
            if (e.mEventType == evType) {
                e.incrementTimer();
            }
        }
        // Needs to be a separate loop in case a later event runs a
        // task that starts an earlier event
        for (MEvent e : adv.mEvents.values()) {
            if (e.mEventType == evType) {
                e.mJustStarted = false;
            }
        }
        mEventsRunning = false;

        // =============================================================
        //     FLUSH OUTPUT TEXT AND CHECK END GAME (TIME INCREMENT)
        // -------------------------------------------------------------
        if (evType == TimeBased) {
            if (MView.mOutputText.length() > 0) {
                // Flush any output text immediately.
                displayText(adv, "", true);
            }
            checkEndOfGame(adv);
        }
    }

    /**
     * Attempts to display a sensible "not understood" message in relation to the player's
     * last input.
     * <p>
     * For example, if the player types a verb but not an associated noun
     * it might respond with "[verb] who?" or "[verb] what?". If no sensible
     * "not understood" message can be generated, falls back to displaying the
     * default message set in the game file.
     *
     * @param adv1 - the Adventure object.
     * @throws InterruptedException if, for example, the player presses the back key.
     */
    private static void displayNotUnderstood(@NonNull MAdventure[] adv1) throws InterruptedException {
        // ------------------------------------------------------------------------
        // If we've got a remembered verb from the last not understood message,
        // see if we can simply add that to the word the user has typed now and
        // evaluate that. E.g. if user typed "eat", then we respond "eat what?",
        // then the user types "apple", we would now try to evaluate "eat apple".
        // ------------------------------------------------------------------------
        if (!mRememberedVerb.equals("")) {
            mInput = mRememberedVerb + " " + mInput;
            mRememberedVerb = "";
            evaluateInput(adv1, -1, false);
            if (MView.mOutputText.length() > 0) {
                return;
            }
        }

        MAdventure adv = adv1[0];

        // ------------------------------------------------------------------------
        // The first time this function is called, compile a list of known
        // words. These are words appearing in: (i) the commands of the
        // general tasks; (ii) the article, prefix and names of the objects;
        // (iii) the article, descriptors, prefix and names of characters;
        // (iv) names of directions; and (v) hard-coded additional words
        // (currently just "and").
        // ------------------------------------------------------------------------
        if (mKnownWords == null) {
            mKnownWords = new HashSet<>();
            for (MTask tas : adv.mTasks.values()) {
                if (tas.mType == MTask.TaskTypeEnum.General) {
                    for (String cmd : tas.mCommands) {
                        cmd = cmd.replaceAll("[\\[\\]{}/]", " ")
                                .trim().replaceAll(" +", " ");
                        Collections.addAll(mKnownWords, cmd.split(" "));
                    }
                }
            }
            for (MObject ob : adv.mObjects.values()) {
                mKnownWords.add(ob.getArticle());
                Collections.addAll(mKnownWords, ob.getPrefix().split(" "));
                mKnownWords.addAll(ob.getNames());
            }
            for (MCharacter ch : adv.mCharacters.values()) {
                mKnownWords.add(ch.getArticle());
                Collections.addAll(mKnownWords, ch.getPrefix().split(" "));
                mKnownWords.addAll(ch.mDescriptors);
                mKnownWords.add(ch.getProperName().toLowerCase());
            }
            Collections.addAll(mKnownWords, MGlobals.sDirectionsRE.split("\\|"));
            Collections.addAll(mKnownWords, "and");
        }

        // ------------------------------------------------------------------------
        // Check all the words just typed against the list of known words, to
        // ensure that they are valid.
        // ------------------------------------------------------------------------
        for (String sWord : mInput.split(" ")) {
            if (!mKnownWords.contains(sWord)) {
                displayText(adv, "I did not understand the word \"" + sWord + "\".");
                return;
            }
        }

        // ------------------------------------------------------------------------
        // If the user just entered a single word verb with no noun, look for
        // a task that matches that verb. If found then print a question asking
        // the player to clarify the noun.
        // ------------------------------------------------------------------------
        if (!mInput.contains(" ")) {
            MTaskHashMap tasks = adv.getTaskList(GeneralTasks);
            for (MTask tas : tasks.values()) {
                String cmd = tas.mCommands.get(0);
                if (cmd.contains(mInput) && cmd.contains(" ")) {
                    String question = null;
                    if (cmd.contains("%object")) {
                        question = " what?";
                    } else if (cmd.contains("%character")) {
                        question = " who?";
                    } else if (cmd.contains("%direction%")) {
                        question = " where?";
                    }

                    if (question != null) {
                        for (ArrayList<Pattern> pats : tas.getPatterns()) {
                            for (Pattern pat : pats) {
                                Matcher m = pat.matcher(mInput + " sdkfjdslkj");
                                if (m.find()) {
                                    mRememberedVerb = mInput;
                                    StringBuilder msg = new StringBuilder(mInput);
                                    toProper(msg);
                                    msg.append(question);
                                    displayText(adv, msg.toString());
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }

        // ------------------------------------------------------------------------
        // If the user entered a noun without a verb, look at all objects and
        // characters the player has seen and can still see for a match. If
        // a match is found, ask the user to clarify the verb.
        // ------------------------------------------------------------------------
        MCharacter player = adv.getPlayer();
        for (MObject ob : adv.mObjects.values()) {
            String obKey = ob.getKey();
            if (player.hasSeenObject(obKey)) {
                if (player.canSeeObject(obKey)) {
                    Pattern regex = Pattern.compile(ob.getRegEx());
                    Matcher m = regex.matcher(mInput);
                    if (m.find()) {
                        displayText(adv,
                                "I don't understand what you want to do with " +
                                        ob.getFullName(Definite) + ".");
                        return;
                    }
                }
            }
        }
        for (MCharacter ch : adv.mCharacters.values()) {
            String chKey = ch.getKey();
            if (player.hasSeenCharacter(chKey)) {
                if (player.canSeeCharacter(chKey)) {
                    Pattern regex = Pattern.compile(ch.getRegEx());
                    Matcher m = regex.matcher(mInput);
                    if (m.find()) {
                        displayText(adv,
                                "I don't understand what you want to do with " +
                                        ch.getName() + ".");
                        return;
                    }
                }
            }
        }

        // ------------------------------------------------------------------------
        // At this point, we give up and simply display the game's default
        // not understood message.
        // ------------------------------------------------------------------------
        displayText(adv, adv.getNotUnderstood());
    }

    /**
     * Checks whether this game is over.
     * <p>
     * If it is, if appropriate and we haven't already displayed
     * it, we display: a message stating the player has won or
     * lost; amy end game text; and the final score. Lastly
     * we display a prompt asking if the player wants to restart,
     * restore, quit, or undo the last command.
     *
     * @param adv - the Adventure object.
     */
    public static void checkEndOfGame(@NonNull MAdventure adv) {
        if (!adv.mDisplayedWinOrLose) {
            switch (adv.mGameState) {
                case Win:
                    displayText(adv,
                            "<center><c><b>*** You have won ***</b></c></center>",
                            true);
                    if (adv.mVersion < 5 && adv.mCompatWinningText != null) {
                        // Display version 3.8, 3.9 or 4.0 winning text
                        //  "Similar to the introduction, you may want a generic
                        //   winning message to appear. This will be displayed
                        //   whenever any winning task is executed."
                        displayText(adv, adv.mCompatWinningText + "<br>");
                    }
                    break;
                case Lose:
                    displayText(adv,
                            "<center><c><b>*** You have lost ***</b></c></center>",
                            true);
                    break;
                case Neutral:
                    // Don't display anything
                    break;
                case Running:
                    // Continue
                    break;
            }
            if (adv.mGameState != Running) {
                adv.mStates.recordState(adv);
            }
        }

        if (adv.mGameState != Running) {
            adv.getPlayer().setWalkTo("");
            if (!adv.mDisplayedWinOrLose) {
                MDescription d = adv.getEndGameText();
                if (d != null) {
                    String endGameText = d.toString(true);
                    if (!endGameText.equals("")) {
                        displayText(adv, endGameText + "<br>");
                    }
                }
                if (adv.getMaxScore() > 0) {
                    displayText(adv,
                            "In that game you scored " + adv.getScore() +
                                    " out of a possible " + adv.getMaxScore() + ", in " +
                                    adv.mTurns + " turns.<br><br>", true);
                }
            }
            displayText(adv,
                    "Would you like to <c>restart</c>, <c>restore</c> a saved game, " +
                            "<c>quit</c> or <c>undo</c> the last command?<br><br>", true);
            adv.mDisplayedWinOrLose = true;
        }
    }

    /**
     * Scans a given input string and attempts to update the "him", "her", "it" and
     * "them" references of the Adventure object.
     * <p>
     * The algorithm works as follows: (a) split the input string into words;
     * (b) iterate through each word and see if it matches the name of an object
     * or character visible to, or seen by, the player - if so, add to the list
     * of "him", "her", "it" and / or "them" possibilities; (c) if we accumulated
     * more than one possibility for a given reference, try to reduce to one.
     *
     * @param adv   - the Adventure object
     * @param input - the input string.
     */
    private static void grabIt(@NonNull MAdventure adv, @NonNull String input) {
        try {
            String newIt = "";
            String newThem = "";
            String newHim = "";
            String newHer = "";
            String words[] = input.split(" ");
            HashSet<String> possibleItKeys = new HashSet<>();
            HashSet<String> possibleThemKeys = new HashSet<>();
            HashSet<String> possibleHimKeys = new HashSet<>();
            HashSet<String> possibleHerKeys = new HashSet<>();
            String playerKey = adv.getPlayer().getKey();

            // First, look at anything visible, then seen
            for (ItScope scope : EnumSet.of(ItScope.Visible, ItScope.Seen)) {
                // --------------------------------------------------------------
                // Find all the possible values for "him", "her", "it" and "them"
                // based on what is valid in this scope
                // --------------------------------------------------------------
                for (String word : words) {
                    MObjectHashMap objs;
                    MCharacterHashMap chars;
                    if (scope == ItScope.Visible) {
                        objs = adv.mObjects.getVisibleTo(playerKey);
                        chars = adv.mCharacters.getVisibleTo(playerKey);
                    } else {
                        objs = adv.mObjects.getSeenBy(playerKey);
                        chars = adv.mCharacters.getSeenBy(playerKey);
                    }
                    if (newIt.equals("")) {
                        for (MObject ob : objs.values()) {
                            String obKey = ob.getKey();
                            for (String obName : ob.getNames()) {
                                if (word.equals(obName)) {
                                    possibleItKeys.add(obKey);
                                }
                            }
                        }
                    }
                    if (newThem.equals("")) {
                        for (MObject ob : objs.values()) {
                            if (ob.isPlural()) {
                                String obKey = ob.getKey();
                                for (String obName : ob.getNames()) {
                                    if (word.equals(obName)) {
                                        possibleThemKeys.add(obKey);
                                    }
                                }
                            }
                        }
                    }
                    for (MCharacter ch : chars.values()) {
                        boolean match = false;

                        if (word.equals(ch.getProperName().toLowerCase())) {
                            match = true;
                        } else {
                            for (String chDesc : ch.mDescriptors) {
                                if (word.equals(chDesc)) {
                                    match = true;
                                    break;
                                }
                            }
                        }

                        if (match) {
                            String chKey = ch.getKey();
                            switch (ch.getGender()) {
                                case Male:
                                    if (!possibleItKeys.contains(chKey)) {
                                        possibleHimKeys.add(chKey);
                                    }
                                    break;
                                case Female:
                                    if (!possibleItKeys.contains(chKey)) {
                                        possibleHerKeys.add(chKey);
                                    }
                                    break;
                                case Unknown:
                                    possibleItKeys.add(chKey);
                                    break;
                            }
                        }
                    }
                } // possibilities

                // --------------------------------------------------------------
                // If we don't already have a unique "it", "then", "him" and "her"
                // values, try to reduce the possibilities to one.
                // --------------------------------------------------------------
                if (newIt.equals("")) {
                    newIt = reduceItPossibilities(adv, possibleItKeys,
                            words, false, false);
                }
                if (newThem.equals("")) {
                    newThem = reduceItPossibilities(adv, possibleThemKeys,
                            words, true, false);
                }
                if (newHim.equals("")) {
                    newHim = reduceItPossibilities(adv, possibleHimKeys,
                            words, false, true);
                }
                if (newHer.equals("")) {
                    newHer = reduceItPossibilities(adv, possibleHerKeys,
                            words, false, true);
                }
            }

            // --------------------------------------------------------------
            // Finally, set the global "it", "them", "her" and "him" values,
            // based on what we found above
            // --------------------------------------------------------------
            if (!newIt.equals("")) {
                adv.mIt = newIt;
            }
            if (!newThem.equals("")) {
                adv.mThem = newThem;
            }
            if (!newHim.equals("")) {
                adv.mHim = newHim;
            }
            if (!newHer.equals("")) {
                adv.mHer = newHer;
            }
            if (adv.mIt.equals("")) {
                adv.mIt = "Absolutely Nothing";
            }
            if (adv.mThem.equals("")) {
                adv.mThem = "Absolutely Nothing";
            }
            if (adv.mHim.equals("")) {
                adv.mHim = "No male";
            }
            if (adv.mHer.equals("")) {
                adv.mHer = "No female";
            }
        } catch (Exception ex) {
            MGlobals.errMsg("Error grabbing \"it\"", ex);
            adv.mIt = "Absolutely Nothing";
        }
    }

    /**
     * Helper function for grabIt.
     * <p>
     * Searches a list of object and/or character keys. If only one of these keys
     * matches an input word, then returns the name of the key's object or character.
     * Otherwise returns an empty string "".
     *
     * @param adv          - the Adventure object,
     * @param keysToSearch - a list of keys to search.
     * @param wordsToFind  - a list of input words to find.
     * @param pluralOnly   - whether we should only consider plural objects.
     * @param charsOnly    - whether we should only consider character keys.
     * @return the unique key, if we were able to determine it. Otherwise an
     * empty string "".
     */
    @NonNull
    private static String reduceItPossibilities(@NonNull MAdventure adv,
                                                @NonNull HashSet<String> keysToSearch,
                                                @NonNull String[] wordsToFind,
                                                boolean pluralOnly, boolean charsOnly) {
        ArrayList<String> keys = new ArrayList<>();

        if (keysToSearch.size() == 1) {
            keys.add(keysToSearch.iterator().next());
        } else if (keysToSearch.size() > 1) {
            for (String key : keysToSearch) {
                MCharacter ch = adv.mCharacters.get(key);
                if (ch != null) {
                    nextCh:
                    for (String chPrefix : ch.getPrefix().split(" ")) {
                        for (String word : wordsToFind) {
                            if (chPrefix.equals(word)) {
                                keys.add(key);
                                break nextCh;
                            }
                        }
                    }
                }
                if (!charsOnly) {
                    MObject ob = adv.mObjects.get(key);
                    if (ob != null) {
                        if (!pluralOnly || ob.isPlural()) {
                            nextOb:
                            for (String obPrefix : ob.getPrefix().split(" ")) {
                                for (String word : wordsToFind) {
                                    if (obPrefix.equals(word)) {
                                        keys.add(key);
                                        break nextOb;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (keys.size() == 1) {
            String key = keys.get(0);
            MCharacter ch = adv.mCharacters.get(key);
            if (ch != null) {
                return charsOnly ? ch.getName(Subjective,
                        false, false,
                        Definite) : ch.getName();
            }
            if (!charsOnly) {
                MObject ob = adv.mObjects.get(key);
                if (ob != null) {
                    return ob.getFullName(Definite);
                }
            }
        }

        return "";
    }

    /**
     * Helper function for evaluateInput.
     * <p>
     * Replaces a given word in the input string with another word given
     * word, and displays text showing the interpreted meaning.
     *
     * @param adv         - the adventure object.
     * @param findWord    - the word to find in the input string.
     * @param replaceWord - the word to replace in the input string.
     */
    private static void replaceInputWord(@NonNull MAdventure adv, @NonNull String findWord,
                                         @NonNull String replaceWord) {
        if (containsWord(mInput, findWord)) {
            displayText(adv, "<c>(" + replaceWord + ")</c><br>", true);
            mInput = replaceWord(mInput, findWord, replaceWord);
        }
    }

    private enum ItScope {
        Applicable,     // 0
        Visible,        // 1
        Seen            // 2
    }
}
