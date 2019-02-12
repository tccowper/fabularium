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

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import com.luxlunae.bebek.Bebek;
import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.controller.MParser;
import com.luxlunae.bebek.controller.MReference;
import com.luxlunae.bebek.model.collection.MALRHashMap;
import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MEventHashMap;
import com.luxlunae.bebek.model.collection.MGroupHashMap;
import com.luxlunae.bebek.model.collection.MHintHashMap;
import com.luxlunae.bebek.model.collection.MItemHashMap;
import com.luxlunae.bebek.model.collection.MLocationHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MPropertyHashMap;
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.collection.MSynonymHashMap;
import com.luxlunae.bebek.model.collection.MTaskHashMap;
import com.luxlunae.bebek.model.collection.MTopicHashMap;
import com.luxlunae.bebek.model.collection.MUDFHashMap;
import com.luxlunae.bebek.model.collection.MVariableHashMap;
import com.luxlunae.bebek.model.io.MFileIO;
import com.luxlunae.bebek.model.state.MGameState;
import com.luxlunae.bebek.model.state.MStateStack;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.glk.GLKLogger;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static com.luxlunae.bebek.MGlobals.ANYCHARACTER;
import static com.luxlunae.bebek.MGlobals.ANYOBJECT;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Definite;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Indefinite;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.None;
import static com.luxlunae.bebek.MGlobals.CHARACTERPROPERNAME;
import static com.luxlunae.bebek.MGlobals.HIDDEN;
import static com.luxlunae.bebek.MGlobals.NOCHARACTER;
import static com.luxlunae.bebek.MGlobals.NOOBJECT;
import static com.luxlunae.bebek.MGlobals.PLAYERLOCATION;
import static com.luxlunae.bebek.MGlobals.THEFLOOR;
import static com.luxlunae.bebek.MGlobals.THEPLAYER;
import static com.luxlunae.bebek.MGlobals.appendDoubleSpace;
import static com.luxlunae.bebek.MGlobals.chopLast;
import static com.luxlunae.bebek.MGlobals.contains;
import static com.luxlunae.bebek.MGlobals.displayError;
import static com.luxlunae.bebek.MGlobals.findIgnoreCase;
import static com.luxlunae.bebek.MGlobals.getArgs;
import static com.luxlunae.bebek.MGlobals.instr;
import static com.luxlunae.bebek.MGlobals.left;
import static com.luxlunae.bebek.MGlobals.numberToString;
import static com.luxlunae.bebek.MGlobals.replaceAllIgnoreCase;
import static com.luxlunae.bebek.MGlobals.safeInt;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.inputBox;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.VB.msgBoxYesNo;
import static com.luxlunae.bebek.model.MAction.EndGameEnum.Running;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Objective;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Possessive;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Reflective;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Subjective;
import static com.luxlunae.bebek.model.MAdventure.MTasksListEnum.GeneralTasks;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllObjects;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideOrOnObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.OnObject;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.General;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.Specific;
import static com.luxlunae.bebek.model.MVariable.VariableType.NUMERIC;
import static com.luxlunae.bebek.model.MVariable.VariableType.TEXT;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.Blorb;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.Exe;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.TextAdventure_TAF;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.All;

public class MAdventure {

    private static final Pattern reExp =
            Pattern.compile("<#(.*?)#>", Pattern.DOTALL);  // . doesn't match new lines unless we use Pattern.DOTALL
    private static final Pattern reIgnore =
            Pattern.compile(".*?(?<embeddedexpression><#.*?#>).*?", Pattern.DOTALL);
    private static final Pattern reItemFunc =
            Pattern.compile("(?!<#.*?)(?<firstkey>%?[A-Za-z][\\w\\|_]*%?)(?<nextkey>\\.%?[A-Za-z][\\w\\|_]*%?(\\(.+?\\))?)+(?!.*?#>)",
                    Pattern.DOTALL);
    private static final Pattern rePerspective =
            Pattern.compile("\\[(?<first>[^\\]]*?)/(?<second>[^\\]]*?)/(?<third>[^\\]]*?)\\]");
    private static final String[] PRONOUNS =
            {"subject", "subjective", "personal", "target", "object", "objective", "possessive"};

    @NonNull
    public final MStateStack mStates = new MStateStack();
    @NonNull
    public final MStringArrayList mCommands = new MStringArrayList();
    @NonNull
    public final MTaskHashMap mCompletableTasks = new MTaskHashMap(this);
    @NonNull
    public final MItemHashMap<MItem> mAllItems;
    @NonNull
    public final MLocationHashMap mLocations;
    @NonNull
    public final MObjectHashMap mObjects;
    @NonNull
    public final MTaskHashMap mTasks;
    @NonNull
    public final MEventHashMap mEvents;
    @NonNull
    public final MCharacterHashMap mCharacters;
    @NonNull
    public final MGroupHashMap mGroups;
    @NonNull
    public final MVariableHashMap mVariables;
    @NonNull
    public final MALRHashMap mALRs;
    @NonNull
    public final MHintHashMap mHints;
    @NonNull
    public final MUDFHashMap mUDFs;
    @NonNull
    public final MSynonymHashMap mSynonyms;
    @NonNull
    public final MPropertyHashMap mAllProperties;
    @NonNull
    public final MPropertyHashMap mObjectProperties;
    @NonNull
    public final MPropertyHashMap mCharacterProperties;
    @NonNull
    public final MPropertyHashMap mLocationProperties;
    /**
     * The library items removed, so we don't keep reloading them.
     */
    @NonNull
    public final ArrayList<String> mExcludedItems = new ArrayList<>();
    @NonNull
    public final String[] mReferencedText = new String[5];
    @NonNull
    public final Queue<MTask> mTasksToRun = new ArrayDeque<>();
    @NonNull
    public final EnumMap<DirectionsEnum, String> mDirectionNames = new EnumMap<>(DirectionsEnum.class);
    @NonNull
    public final LinkedHashMap<String, v4Media> mV4Media = new LinkedHashMap<>();
    @NonNull
    public final Hashtable<String, Integer> mBlorbMappings = new Hashtable<>();
    @NonNull
    final MPronounInfoList mPronounKeys = new MPronounInfoList();
    @NonNull
    final Hashtable<String, ArrayList<Integer>> mRandomValues = new Hashtable<>();
    @NonNull
    final Hashtable<MCharacter.Gender, List<String>> mCharsMentionedThisTurn = new Hashtable<>();
    @NonNull
    private final MSearchOptions mSearchOptions = new MSearchOptions();
    @NonNull
    private final EnumMap<EnabledOptionEnum, Boolean> mEnabledOptions = new EnumMap<>(EnabledOptionEnum.class);
    @NonNull
    public String mHer = "";
    @NonNull
    public String mIt = "";
    @NonNull
    public String mHim = "";
    @NonNull
    public String mThem = "";
    public boolean mJustRunSystemTask = false;
    @NonNull
    public String mTurnOutput = "";
    public double mVersion;
    public int iElapsed;
    public int mTurns;
    /**
     * Who we are in conversation with.
     */
    @NonNull
    String mConversationCharKey = "";
    public MAction.EndGameEnum mGameState = Running;
    public boolean mDisplayedWinOrLose = false;
    /**
     * HighestPriorityTask is the default for ADRIFT 5. HighestPriorityPassingTask is
     * the setting for ADRIFT 4 and earlier.
     */
    public TaskExecutionEnum mTaskExecutionMode = TaskExecutionEnum.HighestPriorityTask;
    @Nullable
    public String mPassword;
    @Nullable
    public String mKeyPrefix;
    public int mCompatSizeRatio = 3;
    public int mCompatWeightRatio = 3;
    public int mCompatCompassPoints = 8;
    /**
     * Whether the ADRIFT 4 and earlier battle system
     * is enabled.
     */
    public boolean mCompatBattleSystem = false;
    public boolean mCompatSound = false;
    public boolean mCompatGraphics = false;
    @Nullable
    public String mCompatWinningText;
    @Nullable
    public String mCompatCompileDate;
    /**
     * Where we currently are in the conversation tree.
     */
    @NonNull
    String mConversationNode = "";
    private MMap mMap;
    @Nullable
    private MStringArrayList mFuncNames = null;
    private int mScore;
    private int mMaxScore;
    @Nullable
    private Random mRandomGenerator;
    private int mLastRndSeed = 0;
    /**
     * The text that you type in the text box will be
     * displayed when your game first starts. It should
     * tell the player what the game is about, what
     * character they will be playing as and what they
     * need to achieve to win the game. If your game
     * uses any unusual verbs as commands, then this
     * should also be explained. Under the text box is
     * a drop-down menu where you select the location
     * where the player will start the game. If you tick
     * "Display first room description" above the text
     * box, then the description of this starting room
     * will be displayed immediately after the introductory
     * text. If you don't tick the box then you can enter
     * an alternative description of their start
     * location as part of the introduction.
     */
    @Nullable
    private MDescription mIntroduction;
    /**
     * The text that you type into this text box will be
     * displayed when the game finishes. You can display
     * different text depending upon whether the player
     * won or lost, and what other events occurred during
     * the game, by right-clicking the text box and
     * selecting "Add Alternate Description".
     */
    @Nullable
    private MDescription mEndGameText;
    /**
     * This is the title of your adventure.
     * Any HTML tags in here won’t show up in
     * the Runner title bar.
     */
    @Nullable
    private String mTitle;
    /**
     * That’s you! (Or someone else you wish to
     * credit with your work!)
     */
    @Nullable
    private String mAuthor;
    /**
     * This is the message displayed when the player
     * input is not understood at all.
     */
    @NonNull
    private String mNotUnderstoodMsg = "";
    @Nullable
    private String mFilename;
    @Nullable
    private String mFullPath;
    private boolean mHasChanged;
    @Nullable
    private String mUserStatus;
    //    public MBabelTreatyInfo BabelTreatyInfo = new MBabelTreatyInfo();
    @Nullable
    private String mCoverFilename;
    @Nullable
    private Date mLastUpdated;
    @Nullable
    private MCharacter mPlayer;
    @NonNull
    private String mDefaultFontName = "Arial";
    private int mDefaultFontSize = 12;
    @Nullable
    private Typeface mDefaultFont;
    /**
     * This is the number of turns which pass whenever the
     * player types “wait”. If this is set at 3 (the default),
     * it means a character could walk three spaces, or an
     * event could run three times.
     */
    private int mWaitTurns = 3;
    /**
     * Whether the first room description should appear at
     * the end of the introduction.
     */
    private boolean mShowFirstRoom = true;
    /**
     * Whether the exits should be listed after the player
     * views a room.
     */
    private boolean mShowExits = true;
    private boolean mEnableMenu = true;
    private boolean mEnableDebugger = true;
    private MTaskHashMap mAllTasks;
    private MTaskHashMap mGeneralTasks;
    private MTaskHashMap mGeneralAndOverrideableSpecificTasks;
    private MTaskHashMap mSpecificTasks;
    private MTaskHashMap mSystemTasks;

    public MAdventure() {
        setTitle("Untitled");
        setAuthor("Anonymous");
        setFilename("untitled.taf");
        mDefaultFont = null;

        for (EnabledOptionEnum e : EnabledOptionEnum.values()) {
            setEnabled(e, true);
        }
        setWaitTurns(0);
        mAllItems = new MItemHashMap<>();
        mLocations = new MLocationHashMap(this);
        mObjects = new MObjectHashMap(this);
        mTasks = new MTaskHashMap(this);
        mEvents = new MEventHashMap(this);
        mCharacters = new MCharacterHashMap(this);
        mGroups = new MGroupHashMap(this);
        mVariables = new MVariableHashMap(this);
        mALRs = new MALRHashMap(this);
        mHints = new MHintHashMap(this);
        mUDFs = new MUDFHashMap(this);
        mAllProperties = new MPropertyHashMap(this);
        mObjectProperties = new MPropertyHashMap(this);
        mCharacterProperties = new MPropertyHashMap(this);
        mLocationProperties = new MPropertyHashMap(this);
        mSynonyms = new MSynonymHashMap(this);
        setIntroduction(new MDescription(this));
        setEndGameText(new MDescription(this));

        mMap = new MMap();

        mDirectionNames.put(DirectionsEnum.North, "North/N");
        mDirectionNames.put(DirectionsEnum.NorthEast, "NorthEast/NE/North-East/N-E");
        mDirectionNames.put(DirectionsEnum.East, "East/E");
        mDirectionNames.put(DirectionsEnum.SouthEast, "SouthEast/SE/South-East/S-E");
        mDirectionNames.put(DirectionsEnum.South, "South/S");
        mDirectionNames.put(DirectionsEnum.SouthWest, "SouthWest/SW/South-West/S-W");
        mDirectionNames.put(DirectionsEnum.West, "West/W");
        mDirectionNames.put(DirectionsEnum.NorthWest, "NorthWest/NW/North-West/N-W");
        mDirectionNames.put(DirectionsEnum.In, "In/Inside");
        mDirectionNames.put(DirectionsEnum.Out, "Out/O/Outside");
        mDirectionNames.put(DirectionsEnum.Up, "Up/U");
        mDirectionNames.put(DirectionsEnum.Down, "Down/D");

        for (MCharacter.Gender eGen : MCharacter.Gender.values()) {
            mCharsMentionedThisTurn.put(eGen, new ArrayList<String>());
        }
    }

    public boolean open(@NonNull String sFilename) throws InterruptedException {
        mStates.clear();
        mCommands.clear();
        mCommands.add("");
        MFileIO.FileTypeEnum fileTypeEnum = TextAdventure_TAF;
        String sFileNameLower = sFilename.toLowerCase();
        if (sFileNameLower.endsWith(".blorb")) {
            fileTypeEnum = Blorb;
        }
        if (sFileNameLower.endsWith(".exe")) {
            fileTypeEnum = Exe;
        }

        if (!MFileIO.loadFile(this, sFilename, fileTypeEnum, All, false,
                null, 0)) {
            return false;
        }

        if (mCompatBattleSystem) {
            Bebek.out("<b>WARNING: This game has the ADRIFT battle system enabled. " +
                    "Bebek doesn't support that yet. The game may not work as intended.<br><br>" +
                    "Press any key to continue...</b><waitkey>");
        }

        // Ensure the tasks are sorted by priority and
        // the ALRs are sorted by decreasing length of old text.
        // We only need to do this once for each play session
        mTasks.sort();
        mALRs.sort();

        for (DirectionsEnum eDirection : DirectionsEnum.values()) {
            mDirectionNames.put(eDirection, mDirectionNames.get(eDirection).toLowerCase());
        }

        mGameState = Running;

        // Initialise any array values
        for (MVariable v : mVariables.values()) {
            if (v.getLength() > 1) {
                String sInitialValue[] = v.getStr().split("\n");
                if (sInitialValue.length == v.getLength()) {
                    int i = 1;
                    for (String sValue : sInitialValue) {
                        if (v.getType() == MVariable.VariableType.NUMERIC) {
                            v.setAt(i, MGlobals.safeInt(sValue));
                        } else {
                            v.setAt(i, sValue.replace("\r", ""));
                        }
                        i++;
                    }
                }
            }
        }

        for (MTask t : mTasks.values()) {
            for (int i = 0; i < t.mCommands.size(); i++) {
                t.mCommands.set(i, CommmandUpdater.correctCommand(t.mCommands.get(i)));
            }
            if (t.mType == MTask.TaskTypeEnum.System && t.getRunImmediately()) {
                t.attemptToExecute(true);
            }
        }

        MView.updateStatusBar(this);

        String playerLocKey = getPlayer().getLocation().getLocationKey();
        getPlayer().setHasSeenLocation(playerLocKey, true);
      /*  UserSession.Map.RecalculateNode(Adventure.Map.FindNode(Adventure.Player.Location.LocationKey));
        UserSession.Map.SelectNode(Adventure.Player.Location.LocationKey); */
        Bebek.clearTextWindow();
        MView.displayText(this, "<c>" + getTitle() + "</c>" + "\n", true);
        MView.displayText(this, getIntroduction().toString(), true);

        // ----------------------------------------------------
        // If specified, show the description of the first room
        // ----------------------------------------------------
        if (getShowFirstRoom() && mLocations.containsKey(playerLocKey)) {
            StringBuilder txt = new StringBuilder();
            txt.append("\n");
            txt.append(mLocations.get(playerLocKey).getViewLocation());
            MGlobals.appendDoubleSpace(txt);
            MView.displayText(this, txt.toString(), true);
        }

        for (MEvent e : mEvents.values()) {
            switch (e.mWhenStart) {
                case AfterATask:
                    e.mStatus = MEvent.StatusEnum.NotYetStarted;
                    break;
                case BetweenXandYTurns:
                    e.mStatus = MEvent.StatusEnum.CountingDownToStart;
                    e.setTimerToEndOfEvent(e.mStartDelay.getValue() + e.mLength.getValue());
                    break;
                case Immediately:
                    e.start(true);
                    break;
            }
        }

        // Needs to be a separate loop in case a later event runs a task that starts an earlier event
        for (MEvent e : mEvents.values()) {
            e.mJustStarted = false;
        }

        for (MCharacter c : mCharacters.values()) {
            for (MWalk w : c.mWalks) {
                if (w.getStartActive()) {
                    w.start(true);
                }
            }
            for (MWalk w : c.mWalks) {
                w.mJustStarted = false;
            }

            // Sort our topics by descending length
            TreeMap<String, ArrayList<String>> topickeys = new TreeMap<>(
                    new Comparator<String>() {
                        @Override
                        public int compare(String x, String y) {
                            return y.length() - x.length();
                        }
                    }
            );
            for (MTopic t : c.mTopics.values()) {
                if (t.mIsCommand) {
                    t.mKeywords = CommmandUpdater.correctCommand(t.mKeywords);
                }
                if (!topickeys.containsKey(t.mKeywords)) {
                    topickeys.put(t.mKeywords, new ArrayList<String>());
                }
                topickeys.get(t.mKeywords).add(t.mKey);
            }
            MTopicHashMap htblTopicsNew = new MTopicHashMap();
            for (String sTopic : topickeys.keySet()) {
                for (String sKey : topickeys.get(sTopic)) {
                    htblTopicsNew.put(c.mTopics.get(sKey));
                }
            }
            c.mTopics = htblTopicsNew;
        }

        MView.displayText(this, "\n\n", true);

        mTasks.createTaskReferenceLists();

        if (mLocations.size() == 0) {
            MGlobals.errMsg("This adventure has no locations.  Cannot continue.");
            return false;
        }

        prepareForNextTurn();
        return true;
    }

    public void prepareForNextTurn() {
        //UserSession.Map.imgMap.Refresh()

        // --------------------------------------------
        // If the game is still running and we didn't
        // just execute a system task, add this state
        // to the undo stack.
        // --------------------------------------------
        if (!mJustRunSystemTask && mGameState == Running) {
            mStates.recordState(this);
        }

        mTurnOutput = "";

        // --------------------------------------------
        // Mark the objects, locations and characters
        // that can now be seen by each character.
        // --------------------------------------------
        for (MCharacter ch : mCharacters.values()) {
            // Mark each object that can be seen by this character
            for (MObject ob : mObjects.values()) {
                if (ch.canSeeObject(ob.getKey())) {
                    ob.setHasBeenSeenBy(ch.getKey());
                }
            }

            // Mark each location that can be seen by this character
            String sLocKey = ch.getLocation().getLocationKey();
            if (!sLocKey.equals(HIDDEN) && !sLocKey.equals("")) {
                MLocation locChar = mLocations.get(sLocKey);
                if (locChar != null) {
                    locChar.setSeenBy(ch.getKey(), true);
                }
            }

            // Mark each character that can be seen by this character
            for (MCharacter ch2 : mCharacters.values()) {
                if (!ch.hasBeenSeenBy(ch2.getKey()) && ch.canSeeCharacter(ch2.getKey())) {
                    ch.setSeenBy(ch2.getKey(), true);
                }
            }

            ch.mValidRouteCache.clear();
            ch.mRouteErrors.clear();
            if (ch.getIntroduced()) {
                if (ch != getPlayer() && !getPlayer().canSeeCharacter(ch.getKey())) {
                    ch.setIntroduced(false);
                }
            }
        }

        for (MCharacter.Gender eGen : MCharacter.Gender.values()) {
            mCharsMentionedThisTurn.put(eGen, new ArrayList<String>());
        }

        // Fill mCompletableTasks with tasks
        mCompletableTasks.clear();
        for (MTask tas : getTaskList(GeneralTasks).values()) {
            if (tas.mType == General && (!tas.getCompleted() || tas.getRepeatable())) {
                mCompletableTasks.put(tas.getKey(), tas);
            }
        }

        // --------------------------------------------
        // Record the previous parent locations of every
        // object and character.
        // --------------------------------------------
        for (MObject ob : mObjects.values()) {
            ob.setLastParent(ob.getParent());
        }
        for (MCharacter ch : mCharacters.values()) {
            ch.setLastParent(ch.getParent());
        }

        mPronounKeys.clear();
    }

    @NonNull
    public String evaluateFunctions(@NonNull String text,
                                    @Nullable MReferenceList refs) {
        return evaluateFunctions(text, refs, false);
    }

    public void evaluateFunctions(@NonNull StringBuilder text,
                                  @Nullable MReferenceList refs) {
        evaluateFunctions(text, refs, false, true);
    }

    @NonNull
    public String evaluateFunctions(@NonNull String text,
                                    @Nullable MReferenceList refs,
                                    boolean isExpression) {
        StringBuilder tmp = new StringBuilder(text);
        evaluateFunctions(tmp, refs, isExpression, true);
        return tmp.toString();
    }

    /**
     * A function is a procedure that can be called, which returns a
     * value. Functions can optionally take parameters.
     * <p>
     * There are four types of functions in ADRIFT. These are:
     * <p>
     * 1. ITEM FUNCTIONS
     * <p>
     * Every item in ADRIFT can be referred to by its key. If you type
     * a key (or a function that resolves to a key) of a Location,
     * Character, Object, Group or Event then you can access all the
     * properties of that item, in addition to special functions such as
     * listing and counting etc. A special feature of item functions is
     * that you can chain them together. If you use a function that
     * returns the key of any of the above items then you can
     * append another function to access properties of that item.
     * <p>
     * 2. GENERAL FUNCTIONS
     * <p>
     * These are distinguished from normal text by placing the
     * percent (%) symbol before and after them. They sometimes
     * have parameters which are enclosed in [square brackets].
     * The contents of Variables are accessed by placing the
     * percent (%) symbol before and after their name, and when
     * arrays are used the index of the wanted element is enclosed
     * in square brackets.
     * <p>
     * 3. EXPRESSION FUNCTIONS
     * <p>
     * These are a special set of functions that can only be used
     * within Expressions. Most of these are either math functions
     * or functions used to change a string of text. Expression functions
     * that take parameters enclose them in (parentheses).
     * <p>
     * 4. USER-DEFINED FUNCTIONS
     * <p>
     * These are functions which you create yourself. Multiple references
     * can be defined which become parameters in the function call. The function
     * returns the contents of its Text box, which will usually use one or
     * more of the above function types to generate the wanted text. User functions
     * are used in the same way as general functions, with the percent(%) symbol
     * before and after, and any parameters in square brackets.
     *
     *  @param text               - the text containing functions to evaluate
     *  @param isExpression       - ??
     *  @param evalItemFunctions  - should we evaluate any item functions?
     */
    public void evaluateFunctions(@NonNull StringBuilder text,
                                  @Nullable MReferenceList refs,
                                  boolean isExpression,
                                  boolean evalItemFunctions) {
        // ---------------------------------------
        //       SAVE <# XXX #> EXPRESSIONS
        // ---------------------------------------
        // We evaluate these at the very end.
        Hashtable<String, String> uuids = new Hashtable<>();
        int i1 = text.indexOf("<#");
        while (i1 >= 0) {
            int i2 = text.indexOf("#>", i1);
            String uuid = ":" + UUID.randomUUID().toString() + ":";
            String expr = text.substring(i1, i2 + 2);
            uuids.put(uuid, expr);
            text.replace(i1, i2 + 2, uuid);
            i1 = text.indexOf("<#");
        }

        // =============================================================================
        //                      REPLACE USER-DEFINED FUNCTIONS
        // -----------------------------------------------------------------------------
        for (MUserFunction udf : mUDFs.values()) {
            udf.evaluate(text, refs);
        }

        // =============================================================================
        //                 REPLACE GENERAL FUNCTIONS AND EXPRESSIONS
        // -----------------------------------------------------------------------------
        if (text.indexOf("%") >= 0) {
            // Keep looping until the text no
            // longer changes.
            String check;
            do {
                check = text.toString();

                // =====================================================================
                //                      REPLACE SYSTEM VARIABLES
                // ---------------------------------------------------------------------
                if (mVersion < 5) {
                    // Replace some ADRIFT 4 system variables that aren't present
                    // or interpreted differently in later versions. This is not
                    // an exhaustive list and in the future we may add some others.
                    // Unfortunately, we can't replace %player% with the name of the player
                    // (as specified in the ADRIFT 4 manual) as this then breaks the new
                    // library functions such as "Look", which assume (as with ADRIFT 5)
                    // that %player% evaluates to the *key* of the player, not their
                    // name...! Which means that games such as "Pirate's Plunder!" do
                    // not show the player's name correctly (evaluates as "Captain Player"
                    // instead of "Captain Bloggs", etc.)
                    replaceAllIgnoreCase(text, "%author%", getAuthor());
                    replaceAllIgnoreCase(text, "%modified%", mCompatCompileDate);
                    replaceAllIgnoreCase(text, "%title%", getTitle());
                }
                replaceAllIgnoreCase(text, "%Player%", getPlayer().getKey());
                replaceAllIgnoreCase(text, "%object%", "%object1%");
                replaceAllIgnoreCase(text, "%character%", "%character1%");
                replaceAllIgnoreCase(text, "%location%", "%location1%");
                replaceAllIgnoreCase(text, "%direction%", "%direction1%");
                replaceAllIgnoreCase(text, "%item%", "%item1%");
                replaceAllIgnoreCase(text, "%text%", "%text1%");
                replaceAllIgnoreCase(text, "%number%", "%number1%");
                replaceAllIgnoreCase(text, "%ConvCharacter%", mConversationCharKey);
                replaceAllIgnoreCase(text, "%turns%", String.valueOf(mTurns));
                replaceAllIgnoreCase(text, "%version%",
                        Bebek.AdriftProductVersion.substring(0, 1) +
                                Bebek.AdriftProductVersion.substring(2, 3) +
                                Bebek.AdriftProductVersion.substring(4));
                // text = ReplaceIgnoreCase(text, "%release%",
                //    adv.BabelTreatyInfo.Stories(0).Releases.Attached.Release.Version.ToString);

                // --------------------------------------------------
                //    Evaluate any %AloneWithChar% functions now.
                // --------------------------------------------------
                // This function returns the key of the character in
                // the same location as the player. Always use the
                // restriction "The player character must be alone
                // with any character" to check if this function will
                // return a valid result, before using the result of
                // this function in any equation.
                //
                // This function can be used as the %character%
                // parameter for an Execute-Task action. The executed
                // task will then be applied to the character that is
                // currently in the same location as the player.
                if (contains(text, "%AloneWithChar%")) {
                    String chKey = getPlayer().getAloneWithChar();
                    if (chKey.length() == 0) {
                        chKey = NOCHARACTER;
                    }
                    replaceAllIgnoreCase(text, "%AloneWithChar%", chKey);
                }

                // ----------------------------------------------------
                // Convert any %CharacterName% function references that
                // don't have the (optional) key argument to explicitly
                // point to the Player character.
                // ----------------------------------------------------
                if (contains(text, "%CharacterName[")) {
                    for (String pronoun : PRONOUNS) {
                        replaceAllIgnoreCase(text,
                                "%CharacterName[" + pronoun + "]%",
                                "%CharacterName[%Player%, " + pronoun + "]%");
                    }
                }
                replaceAllIgnoreCase(text,
                        "%CharacterName%",
                        "%CharacterName[%Player%]%");

                // =====================================================================
                //                         REPLACE REFERENCES
                // ---------------------------------------------------------------------
                if (refs != null) {
                    for (int i = 1; i <= 5; i++) {
                        refs.replaceInText("object" + i, text,
                                isExpression, MReference.ReferencesType.Object);
                        refs.replaceInText("character" + i, text,
                                isExpression, MReference.ReferencesType.Character);
                        refs.replaceInText("location" + i, text,
                                isExpression, MReference.ReferencesType.Location);
                        refs.replaceInText("item" + i, text,
                                isExpression, MReference.ReferencesType.Item);
                        refs.replaceInText("direction" + i, text,
                                isExpression, MReference.ReferencesType.Direction);
                        refs.replaceInText("number" + String.valueOf(i), text,
                                isExpression, MReference.ReferencesType.Number);
                        String refText = "%text" + String.valueOf(i) + "%";
                        if (contains(text, refText)) {
                            boolean bQuote = isExpression &&
                                    !contains(text, "%text" + String.valueOf(i) + "%.") &&
                                    !contains(text, "\"%text" + String.valueOf(i) + "%\"");
                            for (MReference ref : refs) {
                                if (ref.mType == MReference.ReferencesType.Text) {
                                    if (ref.mReferenceMatch.equals("text" + i)) {
                                        if (ref.mItems.size() == 1 &&
                                                ref.mItems.get(0).mMatchingPossibilities.size() == 1) {
                                            replaceAllIgnoreCase(text, refText, isExpression ?
                                                    "\"" + ref.mItems.get(0).mMatchingPossibilities.get(0) + "\"" :
                                                    ref.mItems.get(0).mMatchingPossibilities.get(0));
                                        }
                                    }
                                }
                            }
                            replaceAllIgnoreCase(text, refText, bQuote ?
                                    "\"" + mReferencedText[i] + "\"" :
                                    mReferencedText[i]);
                        }
                    }
                    refs.replaceInText("objects", text,
                            isExpression, MReference.ReferencesType.Object);
                    refs.replaceInText("characters", text,
                            isExpression, MReference.ReferencesType.Character);
                }

                // =====================================================================
                //                         REPLACE VARIABLES
                // ---------------------------------------------------------------------
                // Replace any variable name references with the current value of that
                // variable. Note that such references are case-insensitive!
                mVariables.evaluate(text, isExpression, refs);

                // =====================================================================
                //                     REPLACE GENERAL FUNCTIONS
                // ---------------------------------------------------------------------
                // There are quite a few general functions that can be added to any text
                // within ADRIFT 5. These functions must always be within percent (%)
                // symbols. Any arguments must be provided in square brackets. If
                // multiple arguments are required, these should be separated by commas.
                //
                // Need this to evaluate text in order, not replace by function order
                // This is in case eg. "%DisplayCharacter% %CharacterName%" where
                // DisplayCharacter contains name, then we must evaluate first
                // name first in case we abbreviate, else we'll abbreviate the first
                // name with 'he' instead of the second.

                // Find the first recognised function name that appears in the text.
                String funcName = "";
                int firstPos = Integer.MAX_VALUE;
                for (String name : getFunctionNames()) {
                    int pos = findIgnoreCase(text, "%" + name + "[");
                    if (pos >= 0 && pos < firstPos) {
                        funcName = name.toLowerCase();
                        firstPos = pos;
                    }
                }

                int lenFName = funcName.length();
                if (lenFName > 0) {
                    // ----------------------------------------------------------------
                    //        THIS FUNCTION APPEARS AT LEAST ONCE IN THE TEXT
                    // ----------------------------------------------------------------
                    // We found a first occurrence of the function name - need to
                    // evaluate and replace it. We keep replacing references to this
                    // function until there are no references left.
                    String func = "%" + funcName + "[";
                    int funcPos = findIgnoreCase(text, func);
                    while (funcPos >= 0) {
                        // -----------------------------------------------------------
                        //             TRY TO GET THE FUNCTION ARGUMENTS
                        // -----------------------------------------------------------
                        // Retrieve the text between the *matching* enclosing square
                        // brackets after the function name (i.e. any nested brackets
                        // will be included).
                        String args = getArgs(text, funcPos + 1 + lenFName);
                        int argLen = args.length();
                        if (argLen > 0 || funcName.equals("sum")) {
                            // -------------------------------------------------------
                            //          YEP, THE FUNCTION HAS ARGUMENTS
                            // -------------------------------------------------------
                            // Evaluate and replace any nested brackets.
                            //
                            // Only replace the arguments associated
                            // with this particular function reference
                            // because subsequent functions could return
                            // different values (e.g. CharacterName).
                            String oldArgs = args;
                            args = evaluateFunctions(args, refs);
                            int oldArgPos = text.indexOf(oldArgs, funcPos);
                            int oldArgLen = oldArgs.length();
                            text.replace(oldArgPos, oldArgPos + oldArgLen, args);

                            String funcFull = func + args + "]%";
                            if (contains(text, funcFull)) {
                                // --------------------------------------------------
                                //       WE'VE FOUND A VALID FUNCTION CALL
                                // --------------------------------------------------
                                // Evaluate it. Then replace all occurrences of
                                // that function call with the evaluated result.
                                boolean isSentenceStart = false;
                                if (funcPos > 2) {
                                    String lastTwoChars =
                                            text.substring(funcPos - 2, funcPos);
                                    isSentenceStart =
                                            (lastTwoChars.endsWith("\n") ||
                                                    lastTwoChars.equals("  "));
                                }
                                StringBuilder r =
                                        evaluateGeneralFunction(funcName,
                                                args, oldArgs, isSentenceStart,
                                                refs, isExpression, funcPos);
                                replaceAllIgnoreCase(text, funcFull, r.toString());
                            } else {
                                // --------------------------------------------------
                                //       WE'VE FOUND A MALFORMED FUNCTION CALL
                                // --------------------------------------------------
                                // Replace all occurrences of the malformed function
                                // call with a HTML marked up version that will be
                                // displayed to the player.
                                String badFunc = "";
                                String textLower = text.toString().toLowerCase();
                                StringBuilder sbTarg = new StringBuilder("%" + funcName);
                                if (textLower.contains(sbTarg)) {
                                    badFunc = sbTarg.toString();
                                }
                                sbTarg.append("[");
                                if (textLower.contains(sbTarg)) {
                                    badFunc = sbTarg.toString();
                                }
                                sbTarg.append(args.toLowerCase());
                                if (textLower.contains(sbTarg)) {
                                    badFunc = "%" + funcName + "[" + args;
                                }
                                sbTarg.append("]");
                                if (textLower.contains(sbTarg)) {
                                    badFunc = "%" + funcName + "[" + args + "]";
                                }
                                replaceAllIgnoreCase(text, badFunc,
                                        " <c><u>" +
                                                badFunc.replace("%", "&perc;") +
                                                "</u></c>");
                                displayError("Bad function " + funcName);
                            }
                        } else {
                            // -------------------------------------------------------
                            //        FUNCTION DOESN'T HAVE ARGUMENTS - ERROR
                            // -------------------------------------------------------
                            replaceAllIgnoreCase(text, "%" + funcName + "[]%", "");
                            replaceAllIgnoreCase(text, "%" + funcName + "[", "");
                            displayError("No arguments given to function " + funcName);
                        }

                        // Get the next reference to this function, if one exists.
                        funcPos = findIgnoreCase(text, func);
                    }
                }
            } while (!text.toString().equals(check));
        }

        // =============================================================================
        //                           REPLACE ITEM FUNCTIONS
        // -----------------------------------------------------------------------------
        // The item functions are used to get the current value of one of the properties
        // of a particular object, character or location.
        //
        // The item function syntax uses a dot character ("." the Full-Stop or decimal
        // point) between an item key and one or more property keys, to allow direct
        // access to the value of a property in text or an expression without having to
        // use the more cumbersome %PropertyValue[]% function to access each one.
        //
        // Adrift recognises an item function by the presence of a dot joining two
        // words with no space between them. Therefore you don't need to use the %
        // or <# delimiters to separate them from normal text.
        if (evalItemFunctions) {
            String prev;
            do {
                prev = text.toString();
                evaluateItemFunction(text, isExpression, refs);
            } while (!text.toString().equals(prev));
        }

        // =============================================================================
        //                           REPLACE PERSPECTIVES
        // -----------------------------------------------------------------------------
        // Original ADRIFT code had regex
        //
        //      "\\[(?<first>.*?)/(?<second>.*?)/(?<third>.*?)\\]"
        //
        // which is incorrect. For example, given string
        //
        //   "hjhjk [DOGGY=0] jkjkhj hjhjk [am/are/is] wearig hjkhjk"
        //
        // that regex would incorrectly match "[DOGGY=0] jkjkhj hjhjk [am/are/is]"
        //
        // The correct regex is what we now use here:
        Matcher match = rePerspective.matcher(text);
        while (match.find()) {
            String first = match.group("first");
            if (first.contains("[")) {
                first = first.substring(first.lastIndexOf("[") + 1);
            }
            String second = match.group("second");
            String third = match.group("third");
            String value = "";

            if (first.contains("[") || first.contains("]") ||
                    second.contains("[") || second.contains("]") ||
                    third.contains("[") || third.contains("]")) {
                break;
            }

            switch (getPerspective(match.start())) {
                case FirstPerson:
                    value = first;
                    break;
                case SecondPerson:
                    value = second;
                    break;
                case ThirdPerson:
                    value = third;
                    break;
            }

            text.replace(match.start(), match.end(), value);
            match.reset(text);
        }

        // =============================================================================
        //                      REPLACE <# XXX #> EXPRESSIONS
        // -----------------------------------------------------------------------------
        for (String uuid : uuids.keySet()) {
            int start = text.indexOf(uuid);
            int end = start + uuid.length();
            text.replace(start, end, uuids.get(uuid));
        }
    }

    @NonNull
    private StringBuilder evaluateGeneralFunction(@NonNull String funcName,
                                                  @NonNull String args,
                                                  @NonNull String oldArgs,
                                                  boolean isStartOfSentence,
                                                  @Nullable MReferenceList refs,
                                                  boolean isExpression, int iMatchLoc) {
        // GENERAL FUNCTIONS
        //
        // There are quite a few general functions that can be added
        // to any text within ADRIFT 5. These functions must always be
        // within percent (%) symbols. Any arguments must be provided in
        // square brackets. If multiple arguments are required, these
        // should be separated by commas.
        //
        // It is also possible to create your own general functions, which
        // are called User functions.
        //
        // If you create a user function with the same name as one of the
        // general functions, then your function overrides it. This allows
        // you to easily customise some of the messages generated by the
        // standard library, and would also be useful for language
        // translations.

        // GENERAL ITEM FUNCTIONS
        //
        // These are general functions which duplicate the
        // functionality of some of the Item functions.
        //
        // These functions are used in libraries instead of the
        // item functions because they make it easier to customise the
        // default messages that the library generates. This is because
        // you can override a general function by creating a user function
        // with the same name. This user function can then re-format the
        // item function output to better suit a particular game, or as
        // part of translating the library into a different language.
        //
        // The Item functions are often shorter and offer extra functionality
        // so in your own games item functions should be used instead.
        StringBuilder ret = new StringBuilder();
        boolean allowBlankRet = false;

        // =============================================================
        //           CONVERT ARGS TO CORRESPONDING GAME ITEM
        // -------------------------------------------------------------
        MObjectHashMap argObs = new MObjectHashMap(this);
        MCharacterHashMap argChs = new MCharacterHashMap(this);
        MLocationHashMap argLocs = new MLocationHashMap(this);

        String[] keys;
        if (args.contains("|")) {
            // Multiple item keys, separated by a pipe
            keys = args.split("\\|");
        } else {
            // One item key
            keys = new String[1];
            keys[0] = args;
        }
        for (String key : keys) {
            if (key.contains(",")) {
                // trim off any other args
                key = left(key, instr(key, ",") - 1);
            }
            if (!argObs.containsKey(key)) {
                MObject ob = mObjects.get(key);
                if (ob != null) {
                    argObs.put(key, ob);
                }
            }
            if (!argChs.containsKey(key)) {
                MCharacter ch = mCharacters.get(key);
                if (ch != null) {
                    argChs.put(key, ch);
                }
            }
            if (!argLocs.containsKey(key)) {
                MLocation loc = mLocations.get(key);
                if (loc != null) {
                    argLocs.put(key, loc);
                }
            }
        }

        // =============================================================
        //                    EVALUATE THE FUNCTION
        // -------------------------------------------------------------
        switch (funcName) {
            case "characterdescriptor": {
                // -------------------------------------------
                //        %CharacterDescriptor[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish
                //         to display the name of.
                //
                // Equivalent to:
                //   Key.Descriptor
                //
                // Returns the full descriptor (including article
                // and prefix) of the character. ADRIFT will need
                // to determine whether the article should be
                // replaced with "the". A "Bad Function" error
                // will occur in Runner if this function is used
                // on a character that does not have a descriptor.
                // If the point of view of the player character is
                // switched to another character which does not
                // have a descriptor then this function will
                // return "me" or "myself", but only if they are
                // unaltered in the descriptor field of the
                // character with the blue icon named "Player".
                MCharacter ch = argChs.get(args);
                if (ch != null) {
                    // This needs to be The, depending whether
                    // we have referred to them already... :-/
                    ret.append(ch
                            .getDescriptor());
                } else {
                    displayError("Bad Argument to " +
                            "&perc;CharacterDescriptor[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }

            case "charactername": {
                // -------------------------------------------
                //             %CharacterName%
                //           %CharacterName[key]%
                //     %CharacterName[key, pronoun_type]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish to
                //         display the name of. If this parameter
                //         is omitted then the function applies
                //         to the Player character, except in a
                //         description or property of a character,
                //         in which case it will apply to that
                //         character.
                //
                //   pronoun_type - Specifies the type of pronoun
                //         the character name should be substituted
                //         with. If this parameter is omitted, the
                //         pronoun is assumed to be subjective.
                //         Possible values for this parameter are
                //         subject(ive) (I, you, he, she, it),
                //         object(ive)/target (me, you, him, her,
                //         it) and possessive (my, your, his, her,
                //         its).
                //
                // Equivalent to:
                //   Key.Name(pronoun_type)
                //
                // If the character is known to the Player (i.e. the
                // Known property of the character is selected) or the
                // Known property does not exist in your game, then
                // this function will return the Proper name of the
                // character.
                //
                // If the Known property exists in your game but is
                // not set for the character in question, this
                // function will return the Descriptor of the character.
                //
                // In any one turn, if the CharacterName function is
                // used more than once for a particular character, it
                // will be substituted with a pronoun. The default
                // pronoun type to be substituted with is the
                // subjective pronoun, however, this can be altered
                // by providing an additional parameter to the function.
                String[] params = args
                        .replace(" ", "")
                        .split(",");
                String chKey = "";
                if (params.length > 0) {
                    chKey = params[0];
                }

                // Get the pronoun type
                MPronounEnum pronounType = Subjective;
                if (params.length == 2) {
                    switch (params[1].toLowerCase()) {
                        case "subject":
                        case "subjective":
                        case "personal":
                            pronounType = Subjective;
                            break;
                        case "target":
                        case "object":
                        case "objective":
                            pronounType = Objective;
                            break;
                        case "possess":
                        case "possessive":
                            pronounType = Possessive;
                            break;
                        case "none":
                            pronounType = MPronounEnum.None;
                            break;
                    }
                }

                MCharacter ch = argChs.get(chKey);
                if (ch != null) {
                    ret.append(ch
                            .getName(pronounType));

                    // Slight fudge - if the name is the start
                    // of a sentence, auto-cap it... (consistent with v4)
                    if (isStartOfSentence) {
                        toProper(ret);
                    }

                    if (MView.mDisplaying) {
                        mPronounKeys.add(chKey,
                                pronounType, ch.getGender(),
                                MView.mOutputText.length() + iMatchLoc);
                    }
                } else if (chKey.equals(NOCHARACTER)) {
                    ret.append("Nobody");
                } else {
                    displayError("Bad Argument to " +
                            "&perc;CharacterName[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }

            case "characterproper":
            case "propername": {
                // -------------------------------------------
                //           %CharacterProper[key]%
                //             %ProperName[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish
                //         to display the name of.
                //
                // Equivalent to:
                //   Key.ProperName
                //
                // Returns the proper name of a particular
                // character. For the default player character,
                // and any character with a blank proper name
                // field, it will return "Anonymous".
                MCharacter ch = argChs.get(args);
                if (ch != null) {
                    ret.append(ch
                            .getProperName());
                } else {
                    displayError("Bad Argument to " +
                            "&perc;CharacterProper[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }

            case "displaycharacter": {
                // -------------------------------------------
                //          %DisplayCharacter[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish
                //         to display the description for.
                //
                // Equivalent to:
                //   Key.Description
                //
                // Displays the description of a particular
                // character.
                MCharacter ch = argChs.get(args);
                if (ch != null) {
                    ret.append(ch
                            .getDescription().toString());
                    if (ret.length() == 0) {
                        ret.append("%CharacterName% see[//s] nothing " +
                                "interesting about %CharacterName[")
                                .append(args).append(", target]%.");
                    }
                } else {
                    displayError("Bad Argument to " +
                            "&perc;DisplayCharacter[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }

            case "displaylocation": {
                // -------------------------------------------
                //          %DisplayLocation[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the location you wish
                //         to display the description for.
                //
                // Equivalent to:
                //   Key.Location.Description
                //
                // Displays the description of a particular
                // Location.
                //
                // If "Display the short location name when
                // viewing descriptions" is selected in the
                // runner options, then the first thing
                // displayed will be the short description
                // of the location. It will be in boldface.
                //
                // Next will be the long description of the
                // location, possibly modified by alternate
                // descriptions.
                //
                // Static objects with "Specifically list
                // objects in location description" selected,
                // and dynamic objects, will then be listed.
                // Objects with a specific "When the object
                // is listed in location, display this"
                // description, will be shown after the listed
                // objects. All dynamic objects will be shown
                // after all static objects (5.0.22 and later).
                //
                // Characters at the location are then listed,
                // those with specific "What to show when the
                // character is at location" descriptions after
                // the list of other characters.
                //
                // This function does not currently show
                // characters or objects that are sitting or
                // laying on another object.
                MLocation loc = mLocations.get(args);
                if (loc != null) {
                    ret.append(loc
                            .getViewLocation());
                    if (ret.length() == 0) {
                        ret.append("There is nothing of interest here.");
                    }
                } else {
                    displayError("Bad Argument to " +
                            "&perc;DisplayLocation[]&perc; - " +
                            "Location Key \"" + args + "\" not found");
                }
                break;
            }

            case "displayobject": {
                // -------------------------------------------
                //          %DisplayObject[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         display the description for.
                //
                // Equivalent to:
                //   Key.Description
                //
                // Displays the description of a particular
                // object.
                MObject ob = argObs.get(args);
                if (ob != null) {
                    ret.append(ob
                            .getDescription().toString());
                    if (ret.length() == 0) {
                        ret.append("%CharacterName% see[//s] nothing " +
                                "interesting about ")
                                .append(ob.getFullName(Definite))
                                .append(".");
                    }
                } else {
                    displayError("Bad Argument to " +
                            "&perc;DisplayObject[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                break;
            }

            case "held": {
                // -------------------------------------------
                //               %Held[key]%
                //             [UNDOCUMENTED]
                // -------------------------------------------
                // Params:
                //   key: key of the character for which we
                //        want to get held items.
                //
                // Returns a pipe-separated list of the items
                // held by the given character.
                MCharacter ch = mCharacters.get(args);
                if (ch != null) {
                    for (MObject ob : ch.getHeldObjects().values()) {
                        if (ret.length() > 0) {
                            ret.append("|");
                        }
                        ret.append(ob.getKey());
                    }
                } else {
                    displayError("Bad Argument to " +
                            "&perc;Held[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                allowBlankRet = true;
                break;
            }

            case "lcase":
                // -------------------------------------------
                //              %LCase[string]%
                // -------------------------------------------
                // Params:
                //   string - The text string you wish to
                //            convert to lower case.
                //
                // Returns a string that has been converted to
                // lower case.
                ret.append(args.toLowerCase());
                break;

            case "listcharacterson": {
                // -------------------------------------------
                //          %ListCharactersOn[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         list the characters that are on
                //         top of.
                //
                // Equivalent to:
                //   Key.Contents(Characters, On).List
                //
                // Lists all the characters sitting, laying or
                // standing on top of a particular object.
                // Example output: "Harry, Fred and George"
                MObject ob = mObjects.get(args);
                if (ob != null) {
                    ret.append(ob
                            .getChildChars(OnObject).toList());
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListCharactersOn[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                break;
            }

            case "listcharactersin": {
                // -------------------------------------------
                //          %ListCharactersIn[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         list the characters that are
                //         inside of.
                //
                // Equivalent to:
                //   Key.Contents(Characters).List
                //
                // Lists all the characters inside a particular
                // object.
                MObject ob = mObjects.get(args);
                if (ob != null) {
                    ret.append(ob
                            .getChildChars(InsideObject).toList());
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListCharactersIn[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                break;
            }

            case "listcharactersonandin": {
                // -------------------------------------------
                //       %ListCharactersOnAndIn[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         list the Characters that are on
                //         and in.
                //
                // Equivalent to:
                //   Key.Children(Characters).List
                //
                // Lists all the characters on and inside a
                // particular object. This is basically the
                // same as ListCharactersOn and ListCharactersIn,
                // but it also formats the output into a complete
                // sentence. Example output: "Harry and fred are
                // on the box, and George are inside the box"
                MObject ob = mObjects.get(args);
                if (ob != null) {
                    ret.append(ob
                            .displayChildChars());
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListCharactersOnAndIn[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                break;
            }

            case "listheld": {
                // -------------------------------------------
                //             %ListHeld[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish
                //         to list the held objects of.
                //
                // Equivalent to:
                //   Key.Held.List
                //
                // Lists all the objects held by a particular
                // character.
                MCharacter ch = mCharacters.get(args);
                if (ch != null) {
                    ret.append(ch
                            .getHeldObjects()
                            .toList("and",
                                    true, Indefinite));
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListHeld[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }

            case "listexits": {
                // -------------------------------------------
                //             %ListExits[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish
                //         to list the exits for.
                //
                // Similar:
                //   Key.Location.Exits.List
                //   (lists ALL of the exits, including those
                //    blocked by restrictions)
                //
                // Lists all the exits currently available to
                // a particular character. This function checks
                // the restrictions of each of the exits from
                // the character's current location and only
                // lists those which are currently available
                // for them to use.
                MCharacter ch = mCharacters.get(args);
                if (ch != null) {
                    ret.append(ch
                            .listExits());
                } else {
                    displayError("Bad Argument to " +
                            "&perc;listExits[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }

            case "listobjectsatlocation": {
                // -------------------------------------------
                //       %ListObjectsAtLocation[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the location you wish
                //         to list the objects at.
                //
                // Equivalent to:
                //   Key.Objects.List
                //
                // Displays a list of all the objects at a
                // particular location. This will include
                // dynamic objects directly at the location
                // (i.e. not inside or on another object) and
                // static objects which have been marked as
                // explicitly list.
                MLocation loc = mLocations.get(args);
                if (loc != null) {
                    ret.append(loc
                            .getObjectsInLocation(AllObjects, true)
                            .toList("and", false, Indefinite));
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListObjectsAtLocation[]&perc; - " +
                            "Location Key \"" + args + "\" not found");
                }
                break;
            }

            case "listobjectson": {
                // -------------------------------------------
                //           %ListObjectsOn[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         list the objects that are on.
                //
                // Equivalent to:
                //   On Key.Name is Key.Children(Objects, On).List
                //
                // Lists all the objects on the surface of a
                // particular object.
                MObject ob = mObjects.get(args);
                if (ob != null) {
                    ret.append(ob
                            .getChildObjects(OnObject)
                            .toList("and", false, Indefinite));
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListObjectsOn[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                break;
            }

            case "listobjectsin": {
                // -------------------------------------------
                //          %ListObjectsIn[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         list the objects that are in.
                //
                // Equivalent to:
                //   Inside Key.Name is Key.Contents(Objects).List
                //
                //   (However, when the container is closed, the
                //    item function will still list the contents
                //    while the general function will say "Nothing
                //    is inside the object" instead.)
                //
                // Lists all the objects inside of a particular
                // container object.
                MObject ob = mObjects.get(args);
                if (ob != null) {
                    ret.append(ob
                            .getChildObjects(InsideObject)
                            .toList("and", false, Indefinite));
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListObjectsIn[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                break;
            }

            case "listobjectsonandin": {
                // -------------------------------------------
                //         %ListObjectsOnAndIn[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         list the objects that are on and in.
                //
                // Similar to:
                //   %object%.Children(Objects).List
                //
                //   (which does include the contents of
                //    closed containers.)
                //
                // Lists all the objects on and inside a
                // particular object. This is basically the
                // same as ListObjectsOn and ListObjectsIn,
                // but it gives the response in a nicer syntax
                // when there are objects both on and in the
                // object.
                //
                //   "A plastic cup is on the cabinet, and
                //    inside is a gold coin."
                //
                // Note that objects inside of a closed
                // container will *not* be listed.
                if (argObs.size() > 0) {
                    for (MObject ob : argObs.values()) {
                        if (argObs.size() == 1 ||
                                ob.getChildObjects(InsideOrOnObject).size() > 0) {
                            if (ret.length() > 0) {
                                appendDoubleSpace(ret);
                            }
                            ret.append(ob.displayChildObjects());
                        }
                    }
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListObjectsOnAndIn[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                break;
            }

            case "listworn": {
                // -------------------------------------------
                //             %ListWorn[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish
                //         to list the worn objects of.
                //
                // Equivalent to:
                //   Key.Worn.List
                //
                // Lists all the objects worn by a particular
                // character.
                MCharacter ch = mCharacters.get(args);
                if (ch != null) {
                    ret.append(ch
                            .getWornObjects()
                            .toList("and", true, Indefinite));
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ListWorn[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }

            case "locationname": {
                // -------------------------------------------
                //            %LocationName[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the location you wish to
                //         return the short description for.
                //
                // Equivalent to:
                //   Key.Name
                //
                // Returns the short description of a particular
                // Location.
                MLocation loc = mLocations.get(args);
                if (loc != null) {
                    ret.append(loc
                            .getShortDescription().toString());
                } else {
                    displayError("Bad Argument to " +
                            "&perc;LocationName[]&perc; - " +
                            "Location Key \"" + args + "\" not found");
                }
                break;
            }

            case "locationof": {
                // -------------------------------------------
                //            %LocationOf[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the character you wish to
                //         find the location of. [UNDOCUMENTED:
                //         this can also be the key of an
                //         object]
                //
                // Equivalent to:
                //   Key.Location
                //
                // Returns the key of the location of a particular
                // character. [UNDOCUMENTED: or root location(s)
                // of an object]
                MCharacter ch = mCharacters.get(args);
                if (ch != null) {
                    ret.append(ch
                            .getLocation()
                            .getLocationKey());
                } else {
                    MObject ob = mObjects.get(args);
                    if (ob != null) {
                        Set<String> locs = ob
                                .getRootLocations()
                                .keySet();
                        for (String locKey : locs) {
                            if (ret.length() > 0) {
                                ret.append("|");
                            }
                            ret.append(locKey);
                        }
                    } else {
                        displayError("Bad Argument to " +
                                "&perc;LocationOf[]&perc; - " +
                                "Character Key \"" + args + "\" not found");
                    }
                }
                break;
            }

            case "numberastext":
                // -------------------------------------------
                //          %NumberAsText[number]%
                // -------------------------------------------
                // Params:
                //   number - Any non-negative integer value.
                //
                // Returns a number written out as text.
                ret.append(numberToString(this, args, refs));
                break;

            case "objectname":
                // -------------------------------------------
                //
                // -------------------------------------------
                ret.append(argObs
                        .toList("and", false, Indefinite));
                break;

            case "objectsin": {
                // -------------------------------------------
                //
                // -------------------------------------------
                MObject ob = mObjects.get(args);
                if (ob != null) {
                    Collection<MObject> childObs = ob
                            .getChildObjects(InsideObject).values();
                    for (MObject childOb : childObs) {
                        if (ret.length() > 0) {
                            ret.append("|");
                        }
                        ret.append(childOb.getKey());
                    }
                } else {
                    displayError("Bad Argument to " +
                            "&perc;ObjectsIn[]&perc; - " +
                            "Object Key \"" + args + "\" not found");
                }
                allowBlankRet = true;
                break;
            }

            case "parentof":
                // -------------------------------------------
                //             %ParentOf[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish to
                //         find the parent of. This can also
                //         be a reference, for example
                //         %objects%. [UNDOCUMENTED: can also
                //         be character key(s)].
                //
                // Equivalent to:
                //   Key.Parent
                //
                // Returns the parent key of an object. So for
                // example, if an object is inside another
                // object, or an object is part of a character,
                // the function will return the key of the
                // container object or character. If the
                // function returns more than one parent (for
                // example, the key supplied is a multiple
                // object reference), the keys will be listed
                // in a pipe separated list.
                //
                // [UNDOCUMENTED: also returns parent key(s)
                // of character(s)]
                for (MObject ob : argObs.values()) {
                    if (ret.length() > 0) {
                        ret.append("|");
                    }
                    ret.append(ob.getParent());
                }
                for (MCharacter ch : argChs.values()) {
                    if (ret.length() > 0) {
                        ret.append("|");
                    }
                    ret.append(ch.getParent());
                }
                break;

            case "pcase": {
                // -------------------------------------------
                //             %PCase[string]%
                // -------------------------------------------
                // Params:
                //   string - The text string you wish to
                //            convert to proper case.
                //
                // Returns a string that has been converted to
                // proper case (i.e. the first letter will be
                // capitalised, and all other letters will be
                // converted to lower case).
                StringBuilder sb = new StringBuilder(args);
                toProper(sb, false, isExpression);
                ret.append(sb);
                break;
            }

            case "popupchoice": {
                // -------------------------------------------
                //   %PopUpChoice[prompt, choice1, choice2]%
                // -------------------------------------------
                // Params:
                //   prompt - An expression (typically a quoted
                //            string) to display as a message in
                //            the inputbox.
                //
                //   choice1 - An expression returned in the
                //             function if the user selects this
                //             option.
                //
                //   choice2 - An expression returned in the
                //             function if the user selects this
                //             option.
                //
                // Causes an input box to popup on the screen,
                // displaying a short message prompt and two
                // choices. The player chooses one of the two
                // options and this becomes the value returned
                // by the function. This is used in the
                // expression field of a set variable action to
                // allow the player to choose which of two text
                // strings is written to the variable.
                //
                // See [| Ask player to enter their gender when
                // the game starts] for an example.
                //
                // Do NOT use directly in a restriction. If you
                // need to do this then call PopupChoice in an
                // action which stores result in a variable and
                // then execute a system task (with another
                // action) which checks the variable with a
                // restriction.
                // TODO - Improve this to read quotes and commas properly
                String[] params = args.split(",");
                if (params.length == 3) {
                    String prompt = evaluateStringExpression(params[0], refs);
                    String choice1 = evaluateStringExpression(params[1], refs);
                    String choice2 = evaluateStringExpression(params[2], refs);
                    char c = msgBoxYesNo(prompt + "\n\nYes for " + choice1 +
                            ", No for " + choice2 + " > ");
                    switch (c) {
                        case 'y':
                        case 'Y':
                            ret.append(choice1);
                            break;
                        case 'n':
                        case 'N':
                            ret.append(choice2);
                            break;
                    }
                    if (isExpression) {
                        ret.append("\"").append(ret).append("\"");
                    }
                } else {
                    displayError("Bad arguments to PopUpChoice " +
                            "function: PopUpChoice[prompt, choice1, choice2]");
                }
                break;
            }

            case "popupinput": {
                // -------------------------------------------
                //        %PopUpInput[prompt, default]%
                // -------------------------------------------
                // Params:
                //   prompt - An expression (typically a quoted
                //            string) to display as a message in
                //            the inputbox.
                //
                //   default - Optional. An expression (typically
                //             a quoted string) to display in the
                //             text field of the inputbox.
                //
                // Causes an input box to popup on the screen,
                // displaying a short message prompt and a
                // single-line text entry field. The player can
                // accept the default text or enter a new line of
                // text, then press OK. This could be used to allow
                // the player to enter their own name, which is then
                // used in the game. If the player presses cancel
                // then this function will return a zero-length
                // string. This function should be used in the
                // expression field of either a "Variables" action
                // or a "Set Properties" action.
                //
                // Do NOT use directly in a restriction. If you need
                // to do this then call PopUpInput in an action which
                // stores the result in a variable, and then execute
                // a system task (with another action) which checks
                // the variable with a restriction.
                // TODO - Improve this to read quotes and commas properly
                String[] params = args.split(",");
                if (params.length == 1 || params.length == 2) {
                    String prompt = evaluateStringExpression(params[0], refs);
                    String defMsg = "";
                    if (params.length == 2) {
                        defMsg = evaluateStringExpression(params[1], refs);
                    }
                    ret.append("\"")
                            .append(inputBox(prompt, "ADRIFT", defMsg))
                            .append("\"");
                } else {
                    displayError("Expecting 1 or two " +
                            "arguments to PopUpInput(prompt, default)");
                }
                break;
            }

            case "prevlistobjectson":
                // -------------------------------------------
                //           %PrevListObjectsOn%
                //              [UNDOCUMENTED]
                // -------------------------------------------
                // Maintain a 'last turn' state
                // Call ListObjectsOn on this state
                ret.append(getPreviousFunction(funcName, args, refs));
                break;

            case "prevparentof":
                // -------------------------------------------
                //             %PrevParentOf%
                //             [UNDOCUMENTED]
                // -------------------------------------------
                // Get rid of PrevParent, and do the
                // same as above
                for (MObject ob : argObs.values()) {
                    if (ret.length() > 0) {
                        ret.append("|");
                    }
                    ret.append(ob.getLastParent());
                }
                for (MCharacter ch : argChs.values()) {
                    if (ret.length() > 0) {
                        ret.append("|");
                    }
                    ret.append(ch.getLastParent());
                }
                break;

            case "propertyvalue": {
                // -------------------------------------------
                //     %PropertyValue[key, propertykey]%
                //               [DEPRECATED]
                // -------------------------------------------
                // Params:
                //   key - The key of the location, object or
                //         character you wish to obtain the
                //         property value of.
                //
                //   propertykey - The key of the property you
                //                 wish to obtain the value of.
                //
                // Equivalent to:
                //   Key.PropertyKey
                //
                // Returns the current value of a property of an
                // object, Location or character.
                //
                // The item functions should be used instead of
                // this function as they are much shorter, have
                // a popup menu to select properties so you don't
                // need to remember the keys, and offer extra
                // functionality.
                allowBlankRet = true;
                String[] params = args
                        .replace(" ", "")
                        .split(",");
                if (params.length == 2) {
                    if (argObs.size() + argChs.size() + argLocs.size() > 0) {
                        MStringArrayList output = new MStringArrayList();
                        for (MObject ob : argObs.values()) {
                            if (ob.hasProperty(params[1])) {
                                output.add(ob.getPropertyValue(params[1]));
                            } else {
                                displayError("Bad 2nd Argument to " +
                                        "&perc;PropertyValue[]&perc; - " +
                                        "Property Key \"" + params[1] + "\" not found");
                            }
                        }
                        for (MCharacter ch : argChs.values()) {
                            if (ch.hasProperty(params[1])) {
                                output.add(ch.getPropertyValue(params[1]));
                            } else {
                                displayError("Bad 2nd Argument to " +
                                        "&perc;PropertyValue[]&perc; - " +
                                        "Property Key \"" + params[1] + "\" not found");
                            }
                        }
                        for (MLocation loc : argLocs.values()) {
                            if (loc.hasProperty(params[1])) {
                                output.add(loc.getPropertyValue(params[1]));
                            } else {
                                displayError("Bad 2nd Argument to " +
                                        "&perc;PropertyValue[]&perc; - " +
                                        "Property Key \"" + params[1] + "\" not found");
                            }
                        }
                        ret.append(output.list());
                    } else {
                        // Only warn about the first arg if
                        // it isn't from a function
                        String orig = oldArgs
                                .replace(" ", "")
                                .split(",")[0];
                        if (orig.equals(params[0])) {
                            displayError("Bad 1st Argument to " +
                                    "&perc;PropertyValue[]&perc; - " +
                                    "Object/Character Key \"" + params[0] + "\" not found");
                        }
                    }
                } else {
                    displayError("Bad call to " +
                            "&perc;PropertyValue[]&perc; - " +
                            "Two arguments expected; Object Key, Property Key");
                }
                break;
            }

            case "sum": {
                // -------------------------------------------
                //               %Sum[string]%
                //              [UNDOCUMENTED]
                // -------------------------------------------
                // Params:
                //   string - the string containing numbers to
                //            sum. Any non-numeric character
                //            will be treated as a separator.
                //            E.g. "12asd32df2" will evaluate
                //            to 46.
                //
                // Returns the sum of the numbers in the given
                // string.
                StringBuilder sb = new StringBuilder();
                for (char ch : args.toCharArray()) {
                    switch (ch) {
                        case '0':
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        case '-':
                            sb.append(String.valueOf(ch));
                            break;
                        default:
                            sb.append(" ");
                            break;
                    }
                }
                String input = sb.toString();
                while (input.contains("  ")) {
                    input = input.replace("  ", " ");
                }
                int total = 0;
                for (String num : input.split(" ")) {
                    total += safeInt(num);
                }
                ret.append(String.valueOf(total));
                break;
            }

            case "taskcompleted": {
                // -------------------------------------------
                //           %TaskCompleted[key]%
                //              [UNDOCUMENTED]
                // -------------------------------------------
                // Params:
                //   key - The key of the task you wish to check.
                //
                // Returns "true" if the task has completed,
                // "false" otherwise.
                MTask tas = mTasks.get(args);
                if (tas != null) {
                    ret.append(String.valueOf(tas.getCompleted()));
                } else {
                    displayError("Bad Argument to " +
                            "&perc;TaskCompleted[]&perc; - " +
                            "Task Key \"" + args + "\" not found");
                }
                break;
            }

            case "theobject":
            case "theobjects": {
                // -------------------------------------------
                //             %TheObject[key]%
                // -------------------------------------------
                // Params:
                //   key - The key of the object you wish.
                //         This can also be a reference, for
                //         example %objects%.
                //
                // Equivalent to:
                //   Key.Name
                //
                // These two functions are identical. However,
                // they are interchangeable to facilitate
                // clearer readability, depending on your task
                // output. These functions will display the full
                // name of an object, replacing any indefinite
                // articles with definite ones, i.e. the word
                // “the”. If the function returns more than one
                // object (for example, the key supplied is a
                // multiple object reference), the objects will
                // be listed in a comma separated list.
                ret.append(argObs.toList());
                break;
            }

            case "ucase": {
                // -------------------------------------------
                //             %UCase[string]%
                // -------------------------------------------
                // Params:
                //   string - The text string you wish to
                //            convert to upper case.
                //
                // Returns a string that has been converted to
                // upper case.
                ret.append(args.toUpperCase());
                break;
            }

            case "worn": {
                // -------------------------------------------
                //               %Worn[key]%
                //             [UNDOCUMENTED]
                // -------------------------------------------
                // Params:
                //   key - The character for whom you wish to
                //         get worn objects.
                //
                // Returns a pipe separated list of the objects
                // worn by the given character.
                MCharacter ch = mCharacters.get(args);
                if (ch != null) {
                    Collection<MObject> obs = ch
                            .getWornObjects().values();
                    for (MObject ob : obs) {
                        if (ret.length() > 0) {
                            ret.append("|");
                        }
                        ret.append(ob.getKey());
                    }
                } else {
                    displayError("Bad Argument to " +
                            "&perc;Worn[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }
        }

        // Return the evaluated result (informing user if blank
        // when it shouldn't be)
        if (!allowBlankRet && ret.length() == 0) {
            displayError("Bad Function - Nothing output");
        }
        return ret;
    }

    private String getPreviousFunction(@NonNull String funcName,
                                       @NonNull String args,
                                       @Nullable MReferenceList refs) {
        String newFunc = funcName.replace("prev", "");
        // Note the previous state:
        MGameState prevState = mStates.peek();
        // Save where we are now:
        mStates.recordState(this);
        // Load up the previous state:
        mStates.restoreState(this, prevState);
        String ret = evaluateFunctions("%" + newFunc + "[" + args + "]%", refs);
        // Get rid of the 'current' state and load it back as present:
        mStates.pop(this);
        return ret;
    }

    private void evaluateItemFunction(@NonNull StringBuilder text,
                                      boolean isExpression,
                                      @Nullable MReferenceList refs) {
        // If this is in an expression, we need to replace
        // anything with a quoted value

        // Match anything unless it's between <# ... #> symbols
        Matcher m = reIgnore.matcher(text);
        if (m.find()) {
            // We found a <# ... #>
            // Temporarily replace it with a UUID,
            // recursively call this function again, then
            // restore the <# ... #> back into the result.
            String match = m.group("embeddedexpression");
            String uuid = ":" + UUID.randomUUID() + ":";
            text.replace(m.start(), m.end(), uuid);
            evaluateItemFunction(text, isExpression, refs);
            int start = text.indexOf(uuid);
            text.replace(start, start + uuid.length(), match);
        } else {
            // OK, there are no <# ... #> matches. We can
            // now go through and evaluate any item functions.
            int start = 0;
            Matcher m2 = reItemFunc.matcher(text);
            while (start < text.length() && m2.find(start)) {
                // We found an item function
                String itemFunc = m2.group();
                boolean retIsInt[] = new boolean[1];
                retIsInt[0] = false;
                String result =
                        evaluateItemFunction(itemFunc, null,
                                null, null, null, retIsInt);
                int prevStart = start;
                start = m2.start() + itemFunc.length();
                if (!result.equals("#*!~#")) {
                    if (result.contains(itemFunc)) {
                        result = result.replace(itemFunc, "*** RECURSIVE REPLACE ***");
                    }
                    if (isExpression &&
                            !retIsInt[0] &&
                            !contains(text, "\"" + itemFunc + "\"", prevStart + 1)) {
                        result = "\"" + result + "\"";
                    }
                    int pos = text.indexOf(itemFunc, prevStart);
                    text.replace(pos, pos + itemFunc.length(), result);
                    start += result.length() - itemFunc.length();
                    m2.reset(text);
                }
            }
            evaluateFunctions(text, refs, false, false);
        }
    }

    @NonNull
    String evaluateStringExpressions(@NonNull String text,
                                     @Nullable MReferenceList refs) {
        Matcher m = reExp.matcher(text);
        while (m.find()) {
            text = text.replace(m.group(),
                    evaluateStringExpression(m.group(1), refs));
        }
        return text;
    }

    public void evaluateStringExpressions(@NonNull StringBuilder text,
                                          @Nullable MReferenceList refs) {
        Matcher m = reExp.matcher(text);
        while (m.find()) {
            GLKLogger.error(text.toString());
            text.replace(m.start(), m.end(),
                    evaluateStringExpression(m.group(1), refs));
            m.reset(text);
        }
    }

    @NonNull
    public String evaluateStringExpression(@NonNull String expr,
                                           @Nullable MReferenceList refs) {
        if (expr.length() == 0) {
            return "";
        }
        MVariable var = new MVariable(this);
        var.setType(TEXT);
        var.setToExpr(expr, refs);
        return var.getStr();
    }

    int evaluateIntExpression(@NonNull String expr,
                              @Nullable MReferenceList refs) {
        MVariable var = new MVariable(this);
        var.setType(NUMERIC);
        var.setToExpr(expr, refs);
        return var.getInt();
    }

    private MGlobals.MPerspectiveEnum getPerspective(int iOffset) {
        // Return the highest perspective that is less the iOffset
        int highest = 0;
        MGlobals.MPerspectiveEnum pers = MGlobals.MPerspectiveEnum.None;
        for (MPronounInfo p : mPronounKeys) {
            if (iOffset >= p.mOffset && p.mOffset > highest) {
                pers = mCharacters.get(p.mKey).getPerspective();
                highest = p.mOffset;
            }
        }
        return (pers != MGlobals.MPerspectiveEnum.None) ?
                pers : getPlayer().getPerspective();
    }

    @NonNull
    String evaluateItemProperty(@NonNull String propKey,
                                @NonNull MPropertyHashMap itemProperties,
                                @NonNull MPropertyHashMap globalProperties,
                                @NonNull String remainder,
                                @NonNull boolean[] resultIsInt) {
        MItemFunctionEvaluator item = null;
        ArrayList<MItemWithProperties> itemList = null;
        MProperty p;

        if ((p = itemProperties.get(propKey)) != null) {
            // Item has the property.
            switch (p.getType()) {
                case CharacterKey:
                    item = mCharacters.get(p.getValue());
                    break;

                case LocationGroupKey:
                    itemList = new ArrayList<>();
                    for (String itemKey :
                            mGroups.get(p.getValue()).getArlMembers()) {
                        itemList.add((MItemWithProperties) mAllItems.get(itemKey));
                    }
                    break;

                case LocationKey:
                    item = mLocations.get(p.getValue());
                    break;

                case ObjectKey:
                    item = mObjects.get(p.getValue());
                    break;

                case Integer:
                case ValueList:
                    resultIsInt[0] = true;
                    return String.valueOf(p.getIntData());

                case SelectionOnly:
                    resultIsInt[0] = true;
                    return "1";

                case Text:
                case StateList:
                    return p.getValue();
            }
        } else if ((p = globalProperties.get(propKey)) != null) {
            // Item doesn't have the property. Give a default based on the
            // global property's type.
            switch (p.getType()) {
                case Integer:
                case ValueList:
                case SelectionOnly:
                    return "0";
            }
        }

        if (remainder.length() > 0) {
            return evaluateItemFunction(remainder, itemList,
                    null, null, item, resultIsInt);
        } else if (item != null) {
            return ((MItem) item).getKey();
        } else if (itemList != null && itemList.size() > 0) {
            return evaluateItemFunction(remainder, itemList,
                    null, null, null, resultIsInt);
        }

        return "#*!~#";
    }

    @NonNull
    public String evaluateItemFunction(@NonNull String itemFuncWithArgs,
                                       @Nullable ArrayList<MItemWithProperties> lst,
                                       @Nullable ArrayList<DirectionsEnum> lstDirs,
                                       @Nullable String propKey,
                                       @Nullable MItemFunctionEvaluator item,
                                       @NonNull boolean[] resultIsInt) {
        String args = "";
        String remainder = "";

        int dotPos = itemFuncWithArgs.indexOf(".");
        int openBracketPos = itemFuncWithArgs.indexOf("(");
        if (openBracketPos > -1 && itemFuncWithArgs.contains(")")) {
            int closeBracketPos = itemFuncWithArgs.indexOf(")", openBracketPos);
            if (dotPos > -1) {
                if (dotPos < openBracketPos) {
                    remainder = itemFuncWithArgs.substring(dotPos + 1);
                    itemFuncWithArgs = itemFuncWithArgs.substring(0, dotPos);
                } else {
                    args = itemFuncWithArgs.substring(openBracketPos + 1, closeBracketPos);
                    remainder = itemFuncWithArgs.substring(closeBracketPos + 2);
                    itemFuncWithArgs = itemFuncWithArgs.substring(0, openBracketPos);
                }
            } else {
                args = itemFuncWithArgs.substring(openBracketPos + 1, itemFuncWithArgs.lastIndexOf(")"));
                remainder = itemFuncWithArgs.substring(closeBracketPos + 1);
                itemFuncWithArgs = itemFuncWithArgs.substring(0, openBracketPos);
            }
        } else if (dotPos > 0) {
            remainder = itemFuncWithArgs.substring(dotPos + 1);
            itemFuncWithArgs = itemFuncWithArgs.substring(0, dotPos);
        }

        if (lst != null || (lstDirs != null &&
                (itemFuncWithArgs.equals("List") || itemFuncWithArgs.equals("Count") ||
                        itemFuncWithArgs.equals("Name") || itemFuncWithArgs.length() == 0))) {
            return evaluateItemList(lst, lstDirs,
                    itemFuncWithArgs, args, remainder, propKey, resultIsInt);
        } else if (item != null) {
            return item.evaluate(itemFuncWithArgs, args, remainder, resultIsInt);
        } else {
            return evaluateItemFunction(itemFuncWithArgs, remainder, resultIsInt);
        }
    }

    private String evaluateItemList(@Nullable ArrayList<MItemWithProperties> lst,
                                    @Nullable ArrayList<DirectionsEnum> lstDirs,
                                    @NonNull String funcName,
                                    @NonNull String args,
                                    @NonNull String remainder,
                                    @Nullable String propKey,
                                    @NonNull boolean[] resultIsInt) {
        StringBuilder ret;
        ArrayList<MItemWithProperties> lstNew;
        ArrayList<String> lstKeys;

        switch (funcName) {
            case "":
                ret = new StringBuilder();
                if (lst != null) {
                    for (int i = 0; i < lst.size(); i++) {
                        ret.append(lst.get(i).getKey());
                        if (i < lst.size() - 1) {
                            ret.append("|");
                        }
                    }
                } else {
                    for (int i = 0; i < lstDirs.size(); i++) {
                        ret.append(lstDirs.get(i).toString());
                        if (i < lstDirs.size() - 1) {
                            ret.append("|");
                        }
                    }
                }
                return ret.toString();

            case "Count":
                if (lst != null) {
                    return String.valueOf(lst.size());
                } else if (lstDirs != null) {
                    return String.valueOf(lstDirs.size());
                }
                resultIsInt[0] = true;
                return "0";

            case "Sum":
                int iSum = 0;
                if (lst != null && propKey != null) {
                    for (MItemWithProperties oItem : lst) {
                        if (oItem.hasProperty(propKey)) {
                            iSum += oItem.getProperties().get(propKey).getIntData();
                        }
                    }
                }
                resultIsInt[0] = true;
                return String.valueOf(iSum);

            case "Description":
                ret = new StringBuilder();
                for (MItem oItem : lst) {
                    appendDoubleSpace(ret);
                    if (oItem instanceof MObject) {
                        ret.append(((MObject) oItem).getDescription().toString());
                    } else if (oItem instanceof MCharacter) {
                        ret.append(((MCharacter) oItem).getDescription().toString());
                    } else if (oItem instanceof MLocation) {
                        if (((MLocation) oItem).getViewLocation().equals("")) {
                            ret.append("There is nothing of interest here.");
                        } else {
                            ret.append(((MLocation) oItem).getViewLocation());
                        }
                    }
                }
                return ret.toString();

            case "List":
            case "Name":
                // List(and) - And separated list - Default
                // List(or) - Or separated list
                String sSeparator = " and ";
                String sArgsTest = args.toLowerCase();
                if (sArgsTest.contains("or")) {
                    sSeparator = " or ";
                }
                if (sArgsTest.contains("rows")) {
                    sSeparator = "\n";
                }

                // List(definite/the) - List the objects names - Default
                // List(indefinite) - List a/an object
                MGlobals.ArticleTypeEnum Article = Definite;
                if (sArgsTest.contains("indefinite")) {
                    Article = Indefinite;
                }
                if (sArgsTest.contains("none")) {
                    Article = None;
                }

                // List(true) - List anything in/on everything in the list (single level) - Default
                // List(false) - Do not list anything in/on
                boolean bListInOn = true; // List any objects in or on anything in this list
                if (funcName.equals("Name") || sArgsTest.contains("false") || sArgsTest.contains("0")) {
                    bListInOn = false;
                }

                boolean bForcePronoun = false;
                MPronounEnum ePronoun = Subjective;
                if (sArgsTest.contains("none")) {
                    ePronoun = MPronounEnum.None;
                }
                if (sArgsTest.contains("force")) {
                    bForcePronoun = true;
                }
                if (sArgsTest.contains("objective") || sArgsTest.contains("object") || sArgsTest.contains("target")) {
                    ePronoun = Objective;
                }
                if (sArgsTest.contains("possessive") || sArgsTest.contains("possess")) {
                    ePronoun = Possessive;
                }
                if (sArgsTest.contains("reflective") || sArgsTest.contains("reflect")) {
                    ePronoun = Reflective;
                }

                ret = new StringBuilder();
                int i = 0;
                if (lst != null) {
                    for (MItem oItem : lst) {
                        i++;
                        if (sSeparator.equals("\n")) {
                            if (i > 1) {
                                ret.append(sSeparator);
                            }
                        } else {
                            if (i > 1 && i < lst.size()) {
                                ret.append(", ");
                            }
                            if (lst.size() > 1 && i == lst.size()) {
                                ret.append(sSeparator);
                            }
                        }
                        if (oItem instanceof MObject) {
                            ret.append(((MObject) oItem).getFullName(Article));
                            if (bListInOn && ((MObject) oItem).getChildObjects(InsideOrOnObject).size() > 0) {
                                ret.append(".  ").append(((MObject) oItem).displayChildObjects());
                                chopLast(ret);
                            }
                        } else if (oItem instanceof MCharacter) {
                            // List(definite/the) - List the objects names - Default
                            // List(indefinite) - List a/an object
                            Article = Indefinite; // opposite default from objects
                            if (sArgsTest.contains("definite")) {
                                Article = Definite;
                            }
                            ret.append(((MCharacter) oItem).getName(ePronoun, true, true, Article, bForcePronoun));
                        } else if (oItem instanceof MLocation) {
                            ret.append(((MLocation) oItem).getShortDescription().toString());
                        }
                    }
                } else if (lstDirs != null) {
                    for (DirectionsEnum oDir : lstDirs) {
                        i++;
                        if (i > 1 && i < lstDirs.size()) {
                            ret.append(", ");
                        }
                        if (lstDirs.size() > 1 && i == lstDirs.size()) {
                            ret.append(sSeparator);
                        }
                        ret.append(getDirectionName(oDir).toLowerCase());
                    }
                }
                if (ret.length() == 0) {
                    return "nothing";
                }
                return ret.toString();

            case "Parent":
                lstNew = new ArrayList<>();
                lstKeys = new ArrayList<>();

                for (MItemWithProperties oItem : lst) {
                    String sParent = oItem.getParent();
                    if (sParent.length() > 0) {
                        if (!lstKeys.contains(sParent)) {
                            MItemWithProperties oItemNew = (MItemWithProperties) mAllItems.get(sParent);
                            lstKeys.add(sParent);
                            lstNew.add(oItemNew);
                        }
                    }
                }

                if (remainder.length() > 0) {
                    return evaluateItemFunction(remainder, lstNew, null,
                            null, null, resultIsInt);
                }
                break;

            case "Children":
                lstNew = new ArrayList<>();
                for (MItemWithProperties oItem : lst) {
                    if (oItem instanceof MObject) {
                        MObject ob = (MObject) oItem;
                        switch (args.toLowerCase().replace(" ", "")) {
                            case "":
                            case "all":
                            case "onandin":
                            case "all,onandin":
                                lstNew.addAll(ob.getChildObjects(InsideOrOnObject, true).values());
                                lstNew.addAll(ob.getChildChars(InsideOrOnObject).values());
                                break;
                            case "characters,in":
                                lstNew.addAll(ob.getChildChars(InsideObject).values());
                                break;
                            case "characters,on":
                                lstNew.addAll(ob.getChildChars(OnObject).values());
                                break;
                            case "characters,onandin":
                            case "characters":
                                lstNew.addAll(ob.getChildChars(InsideOrOnObject).values());
                                break;
                            case "in":
                                lstNew.addAll(ob.getChildObjects(InsideObject, true).values());
                                lstNew.addAll(ob.getChildChars(InsideObject).values());
                                break;
                            case "objects,in":
                                lstNew.addAll(ob.getChildObjects(InsideObject, true).values());
                                break;
                            case "objects,on":
                                lstNew.addAll(ob.getChildObjects(OnObject, true).values());
                                break;
                            case "objects,onandin":
                            case "objects":
                                lstNew.addAll(ob.getChildObjects(InsideOrOnObject, true).values());
                                break;
                        }
                    } else if (oItem instanceof MCharacter) {
                        // No real reason we couldn't give Children from a Character
                    }
                }

                if (remainder.length() > 0 || lstNew.size() > 0) {
                    return evaluateItemFunction(remainder, lstNew,
                            null, null, null, resultIsInt);
                }
                break;

            case "Contents":
                lstNew = new ArrayList<>();
                for (MItemWithProperties oItem : lst) {
                    if (oItem instanceof MObject) {
                        MObject ob = (MObject) oItem;
                        switch (args.toLowerCase()) {
                            case "":
                            case "all":
                                lstNew.addAll(ob.getChildObjects(InsideObject, true).values());
                                lstNew.addAll(ob.getChildChars(InsideObject).values());
                                break;
                            case "characters":
                                lstNew.addAll(ob.getChildChars(InsideObject).values());
                                break;
                            case "objects":
                                lstNew.addAll(ob.getChildObjects(InsideObject, true).values());
                                break;
                        }
                    }
                }
                return evaluateItemFunction(remainder, lstNew, null,
                        null, null, resultIsInt);

            case "Objects":
                lstNew = new ArrayList<>();
                for (MItemWithProperties oItem : lst) {
                    if (oItem instanceof MLocation) {
                        MLocation loc = (MLocation) oItem;
                        lstNew.addAll(loc.getObjectsInLocation().values());
                    } else {
                        GLKLogger.error("TODO: Objects - MGlobals");
                    }
                }
                return evaluateItemFunction(remainder, lstNew, null,
                        null, null, resultIsInt);

            default:
                if (mAllProperties.containsKey(funcName)) {
                    lstNew = new ArrayList<>();
                    lstKeys = new ArrayList<>();
                    ret = new StringBuilder();
                    int iTotal = 0;
                    boolean bIntResult = false;
                    boolean bPropertyFound = false;
                    String sNewPropertyKey = null;

                    for (MItemWithProperties oItem : lst) {
                        if (oItem.hasProperty(funcName)) {
                            bPropertyFound = true;
                            MProperty p = oItem.getProperty(funcName);

                            boolean bValueOK = true;
                            if (args.length() > 0) {
                                bValueOK = false;
                                if (args.contains(".")) {
                                    args = evaluateFunctions(args, MParser.mReferences);
                                }
                                if (args.contains("+") || args.contains("-")) {
                                    String sArgsNew = evaluateStringExpression(args, MParser.mReferences);
                                    if (sArgsNew != null) {
                                        args = sArgsNew;
                                    }
                                }
                                switch (p.getType()) {
                                    case ValueList:
                                        int iCheckValue = 0;
                                        if (isNumeric(args)) {
                                            iCheckValue = safeInt(args);
                                        } else {
                                            if (p.mValueList.containsKey(args)) {
                                                iCheckValue = p.mValueList.get(args);
                                            }
                                        }
                                        bValueOK = (p.getIntData() == iCheckValue);
                                        resultIsInt[0] = true;
                                        break;
                                    case Integer:
                                        int iCheckValue2 = 0;
                                        if (isNumeric(args)) {
                                            iCheckValue2 = safeInt(args);
                                        }
                                        bValueOK = (p.getIntData() == iCheckValue2);
                                        resultIsInt[0] = true;
                                        break;
                                    case StateList:
                                        bValueOK = (p.getValue().toLowerCase().equals(args.toLowerCase()));
                                        break;
                                    case SelectionOnly:
                                        switch (args.toLowerCase()) {
                                            case "false":
                                            case "0":
                                                bValueOK = false;
                                                break;
                                            default:
                                                bValueOK = true;
                                                break;
                                        }
                                        resultIsInt[0] = true;
                                        break;
                                    default:
                                        GLKLogger.error("TODO: Property filter check not yet implemented for " + p.getType().toString());
                                        break;
                                }
                            }

                            if (bValueOK) {
                                switch (p.getType()) {
                                    case CharacterKey:
                                        MCharacter chP = mCharacters.get(p.getValue());
                                        if (!lstKeys.contains(chP.getKey())) {
                                            lstKeys.add(chP.getKey());
                                            lstNew.add(chP);
                                        }
                                        break;
                                    case LocationGroupKey:
                                        for (String sItem : mGroups.get(p.getValue()).getArlMembers()) {
                                            if (!lstKeys.contains(sItem)) {
                                                MItemWithProperties oItemNew = (MItemWithProperties) mAllItems.get(sItem);
                                                lstKeys.add(oItemNew.getKey());
                                                lstNew.add(oItemNew);
                                            }
                                        }
                                        break;
                                    case LocationKey:
                                        MItemWithProperties oItemNew = mLocations.get(p.getValue());
                                        if (!lstKeys.contains(oItemNew.getKey())) {
                                            lstKeys.add(oItemNew.getKey());
                                            lstNew.add(oItemNew);
                                        }
                                        break;
                                    case ObjectKey:
                                        MItemWithProperties oItemNew2 = mObjects.get(p.getValue());
                                        if (!lstKeys.contains(oItemNew2.getKey())) {
                                            lstKeys.add(oItemNew2.getKey());
                                            lstNew.add(oItemNew2);
                                        }
                                        break;
                                    case Integer:
                                    case ValueList:
                                    case StateList:
                                        lstNew.add(oItem);
                                        sNewPropertyKey = funcName;
                                        resultIsInt[0] = false;
                                        break;
                                    case SelectionOnly:
                                        // Selection Only property to further reduce list
                                        lstNew.add(oItem);
                                        resultIsInt[0] = true;
                                        break;
                                    case Text:
                                        if (ret.length() > 0) {
                                            if (oItem == lst.get(lst.size() - 1)) {
                                                ret.append(" and ");
                                            } else {
                                                ret.append(", ");
                                            }
                                        }
                                        ret.append(p.getValue());
                                        break;
                                }
                            }
                        } else {
                            if (mAllProperties.containsKey(funcName)) {
                                MProperty p = mAllProperties.get(funcName);
                                boolean bValueOK = false; // Because this is equiv of arg = (true)
                                if (args.length() > 0) {
                                    bValueOK = false;
                                    if (args.contains(".")) {
                                        args = evaluateFunctions(args, MParser.mReferences);
                                    }
                                    if (args.contains("+") || args.contains("-")) {
                                        String sArgsNew = evaluateStringExpression(args, MParser.mReferences);
                                        if (sArgsNew != null) {
                                            args = sArgsNew;
                                        }
                                    }
                                    switch (p.getType()) {
                                        // Opposite of above, since this item _doesn't_ contain this property
                                        case SelectionOnly:
                                            switch (args.toLowerCase()) {
                                                case "false":
                                                case "0":
                                                    bValueOK = true;
                                                    break;
                                                default:
                                                    bValueOK = false;
                                                    break;
                                            }
                                            resultIsInt[0] = true;
                                            break;
                                        case StateList:
                                            bValueOK = false; // Since we don't have the property
                                            break;
                                        default:
                                            GLKLogger.error("TODO: Property filter check not yet implemented for " + p.getType().toString());
                                            break;
                                    }
                                }
                                if (bValueOK) {
                                    switch (p.getType()) {
                                        case SelectionOnly:
                                            // Selection Only property to further reduce list
                                            lstNew.add(oItem);
                                            break;
                                    }
                                }
                            }
                        }
                    }

                    if (!bPropertyFound) {
                        switch (mAllProperties.get(funcName).getType()) {
                            case Integer:
                            case ValueList:
                                bIntResult = true;
                                resultIsInt[0] = true;
                                break;
                        }
                    }

                    if (remainder.length() > 0 || (lstNew.size() > 0 && !bIntResult)) {
                        return evaluateItemFunction(remainder, lstNew,
                                null, sNewPropertyKey, null, resultIsInt);
                    } else if (bIntResult) {
                        return String.valueOf(iTotal);
                    } else {
                        return ret.toString();
                    }
                }
        }

        return "#*!~#";
    }

    @NonNull
    private String evaluateItemFunction(@NonNull String itemKey,
                                        @NonNull String remainder,
                                        @NonNull boolean[] resultIsInt) {
        // itemKey may be:
        //        (1) a "|" delimited list of item keys
        //        (2) a single item key
        //        (3) a direction name
        ArrayList<MItemWithProperties> itemList = null;

        if (itemKey.contains("|")) {
            itemList = new ArrayList<>();
            for (String key : itemKey.split("\\|")) {
                itemList.add((MItemWithProperties) mAllItems.get(key));
            }
            return evaluateItemFunction(remainder, itemList, null,
                    null, null, resultIsInt);
        } else {
            if (getAllKeys().contains(itemKey)) {
                MItemFunctionEvaluator item = null;
                if (mObjects.containsKey(itemKey)) {
                    item = mObjects.get(itemKey);
                } else if (mCharacters.containsKey(itemKey)) {
                    item = mCharacters.get(itemKey);
                } else if (mLocations.containsKey(itemKey)) {
                    item = mLocations.get(itemKey);
                } else if (mEvents.containsKey(itemKey)) {
                    item = mEvents.get(itemKey);
                } else if (mGroups.containsKey(itemKey)) {
                    MGroup grp = mGroups.get(itemKey);
                    itemList = new ArrayList<>();
                    for (String sMember : grp.getArlMembers()) {
                        MItemWithProperties itm = (MItemWithProperties) mAllItems.get(sMember);
                        itemList.add(itm);
                    }
                }
                return evaluateItemFunction(remainder, itemList,
                        null, null, item, resultIsInt);
            } else {
                for (DirectionsEnum d : DirectionsEnum.values()) {
                    if (String.valueOf(d).equals(itemKey)) {
                        ArrayList<DirectionsEnum> lstDirs = new ArrayList<>();
                        lstDirs.add(d);
                        return evaluateItemFunction(remainder, null,
                                lstDirs, null, null, resultIsInt);
                    }
                }
            }
        }

        return "#*!~#";
    }

    @NonNull
    MStringArrayList getFunctionNames() {
        if (mFuncNames == null) {
            mFuncNames = new MStringArrayList();
            mFuncNames.addAll(Arrays.asList(MGlobals.STD_FUNCTION_NAMES));
            for (MUserFunction UDF : mUDFs.values()) {
                mFuncNames.add(UDF.mName);
            }
        }
        return mFuncNames;
    }

    @NonNull
    public String getDirectionName(DirectionsEnum dir) {
        String sName = mDirectionNames.get(dir);
        if (sName.contains("/")) {
            sName = sName.substring(0, sName.indexOf("/"));
        }
        return sName;
    }

    @NonNull
    String getDirectionRE(DirectionsEnum dir, boolean bBrackets, boolean bRealRE) {
        String sRE = mDirectionNames.get(dir).toLowerCase();
        if (bRealRE) {
            sRE = sRE.replace("/", "|");
        }
        if (bBrackets) {
            return (bRealRE) ?
                    "(" + sRE + ")" :
                    "[" + sRE + "]";
        } else {
            return sRE;
        }
    }

    boolean keyExists(@NonNull String sKey) {
        return mLocations.containsKey(sKey) || mObjects.containsKey(sKey) ||
                mTasks.containsKey(sKey) || mEvents.containsKey(sKey) ||
                mCharacters.containsKey(sKey) || mGroups.containsKey(sKey) ||
                mVariables.containsKey(sKey) || mALRs.containsKey(sKey) ||
                mHints.containsKey(sKey) || mAllProperties.containsKey(sKey);
    }

    /**
     * Try to find a statelist property that contains
     * exactly the same states as those provided (possibly
     * in a different order).
     *
     * @param states - the states to find.
     *
     * @return the key of the property if one was found,
     * otherwise NULL.
     */
    @Nullable
    String findProperty(@NonNull MStringArrayList states) {
        nextProp:
        for (MProperty prop : mAllProperties.values()) {
            if (prop.mStates.size() == states.size()) {
                for (String sState : states) {
                    if (!prop.mStates.contains(sState)) {
                        continue nextProp;
                    }
                }
                return prop.getKey();
            }
        }
        return null;
    }

    public void searchAndReplace(@NonNull String find,
                                 @NonNull String replace,
                                 boolean bSilent) {
        int iReplacements = 0;

        for (MItem item : mAllItems.values()) {
            boolean bLookIn = false;
            switch (mSearchOptions.mSearchInWhat) {
                case AllItems:
                    bLookIn = true;
                    break;
                case NonLibraryItems:
                    bLookIn = !item.getIsLibrary();
                    break;
            }
            if (bLookIn) {
                iReplacements += item.find(find, replace);
            }
        }

        if (!bSilent) {
            if (iReplacements == 0) {
                GLKLogger.debug("The following specified text was not found: " + find);
            } else {
                GLKLogger.debug(iReplacements + " occurrence(s) replaced.");
            }
        }
    }

    int getRandNoRepeat(int min, int max) {
        String key = min + "-" + max;
        ArrayList<Integer> rndVals = mRandomValues.get(key);
        if (rndVals == null) {
            rndVals = new ArrayList<>();
            mRandomValues.put(key, rndVals);
        }

        if (rndVals.size() == 0) {
            // Our list is empty, so create a
            // new random list of values.
            for (int i = min; i <= max; i++) {
                rndVals.add(getRand(rndVals.size()), i);
            }
        }

        // Select a random element from the list, remove it
        // and return it.
        int rndIndex = getRand(rndVals.size() - 1);
        return rndVals.remove(rndIndex);
    }

    int getRand(int max) {
        // returns y : 0 <= y <= max
        if (mRandomGenerator == null) {
            mRandomGenerator = new Random(getNextSeed());
        }
        return mRandomGenerator.nextInt(max + 1);     // N.B. nextInt(x) returns y : 0 <= y < x
    }

    int getRand(int min, int max) {
        // returns y: min <= y <= max
        if (mRandomGenerator == null) {
            mRandomGenerator = new Random(getNextSeed());
        }
        if (max < min) {
            int i = max;
            max = min;
            min = i;
        }
        int tmp = mRandomGenerator.nextInt(max - min + 1);  // N.B. nextInt(x) returns y : 0 <= y < x
        return (tmp + min);
    }

    private int getNextSeed() {
        int ret = (int) (new Date().getTime() % Integer.MAX_VALUE);
        while (ret == mLastRndSeed) {
            ret -= new Date().getTime();
        }
        mLastRndSeed = ret;
        return ret;
    }

    // Key Stuff

    public String getUserStatus() {
        return mUserStatus;
    }

    public void setUserStatus(String value) {
        mUserStatus = value;
    }

    public String getCoverFilename() {
        return mCoverFilename;
    }

    public void setCoverFilename(String value) {
      /*  If value <> mCoverFilename Then
        Try
        If BabelTreatyInfo.Stories(0).Cover Is Nothing Then BabelTreatyInfo.Stories(0).Cover = New clsBabelTreatyInfo.clsStory.clsCover
        With BabelTreatyInfo.Stories(0).Cover
                .imgCoverArt = Nothing
        If IO.File.Exists(value) Then
        '.imgCoverArt = Image.FromFile(value)
        Dim iLength As Integer = cint(FileLen(value))
        Dim bytImage As Byte()
        Dim fs As New IO.FileStream(value, IO.FileMode.Open, IO.FileAccess.Read)
        ReDim bytImage(iLength - 1)
        fs.Read(bytImage, 0, cint(iLength))
                .imgCoverArt = New Bitmap(New IO.MemoryStream(bytImage))
        fs.Close()
                .Format = IO.Path.GetExtension(value).ToLower.Substring(1)
        End If
        End With
        Catch ex As Exception
        errMsg("Failed to set Cover Art", ex)
        End Try */
        mCoverFilename = value;
        //End If
        GLKLogger.error("FIXME: properly implemnt MAdventure: setCoverFilename");
    }

    public String getVersion() {
        if (mVersion == 0) {
            return "N/A";
        }
        String sVersion = String.format(Locale.US, "%.6f", mVersion); // E.g. 5.000020
        return sVersion.charAt(0) + "." +
                cint(sVersion.substring(2, 4)) + "." +
                cint(sVersion.substring(4, 8));
    }

    public boolean getChanged() {
        return mHasChanged;
    }

    public void setChanged(boolean value) {
        mHasChanged = value;
    }

    public Date getLastUpdated() {
        if (mLastUpdated != null) {
            return mLastUpdated;
        } else {
            return new Date();
        }
    }

    public void setLastUpdated(Date value) {
        mLastUpdated = value;
    }

    public MCharacter getPlayer() {
        if (mPlayer == null) {
            for (MCharacter ch : mCharacters.values()) {
                if (ch.getCharacterType() == MCharacter.CharacterType.Player) {
                    mPlayer = ch;
                    return ch;
                }
            }
            if (mCharacters.size() > 0) {
                for (MCharacter ch : mCharacters.values()) {
                    mPlayer = ch;
                    return ch;
                }
            }
        } else {
            return mPlayer;
        }
        return null;
    }

    public void setPlayer(MCharacter value) {
        mPlayer = value;
    }

    public ArrayList<String> getImages() {
        // Returns a list of all references to images within the adventure
        ArrayList<String> lImages = new ArrayList<>();

        if (!getCoverFilename().equals("")) {
            lImages.add(getCoverFilename());
        }

        for (MSingleDescription sd : getAllDescriptions()) {
            String s = sd.mDescription;
            s = s.replace(" src =", " src=");
            int i = s.indexOf("<img ");
            while (i > -1) {
                int j = s.indexOf(" src=", i);
                if (j > -1) {
                    int k = s.indexOf(">", i);
                    String sTag = s.substring(j + 5, k - j - 5);
                    if (sTag.startsWith("\"")) {
                        sTag = sTag.substring(1);
                    }
                    if (sTag.endsWith("\"")) {
                        sTag = sTag.substring(0, sTag.length() - 1);
                    }
                    if (!lImages.contains(sTag) && (new File(sTag).exists() || sTag.startsWith("http"))) {
                        lImages.add(sTag);
                    }
                }
                i = s.indexOf("<img ", i + 1);
            }
        }

        return lImages;
    }

    public ArrayList<MSingleDescription> getAllDescriptions() {
        ArrayList<MSingleDescription> lDescriptions = new ArrayList<>();
        lDescriptions.addAll(getIntroduction());
        lDescriptions.addAll(getEndGameText());
        for (MItem itm : mAllItems.values()) {
            for (MDescription d : itm.getAllDescriptions()) {
                lDescriptions.addAll(d);
            }
        }
        return lDescriptions;
    }

    public int getScore() {
        if (mVariables.containsKey("Score")) {
            mScore = mVariables.get("Score").getInt();
        }
        return mScore;
    }

    public void setScore(int value) {
        if (value != mScore) {
            if (mVariables.containsKey("Score")) {
                mVariables.get("Score").set(value);
            }
        }
        mScore = value;
        MView.updateStatusBar(this);
    }

    public int getMaxScore() {
        if (mVariables.containsKey("MaxScore")) {
            mMaxScore = mVariables.get("MaxScore").getInt();
        }
        return mMaxScore;
    }

    public void setMaxScore(int value) {
        if (value != mMaxScore) {
            if (mVariables.containsKey("MaxScore")) {
                mVariables.get("MaxScore").set(value);
            }
        }
        mMaxScore = value;
    }

    @Nullable
    MItem getItemFromKey(@Nullable String sKey) {
        if (sKey == null || sKey.equals("")) {
            return null;
        }
        if (mLocations.containsKey(sKey)) {
            return mLocations.get(sKey);
        }
        if (mObjects.containsKey(sKey)) {
            return mObjects.get(sKey);
        }
        if (mTasks.containsKey(sKey)) {
            return mTasks.get(sKey);
        }
        if (mEvents.containsKey(sKey)) {
            return mEvents.get(sKey);
        }
        if (mCharacters.containsKey(sKey) || sKey.equals(THEPLAYER)) {
            return mCharacters.get(sKey);
        }
        if (mGroups.containsKey(sKey)) {
            return mGroups.get(sKey);
        }
        if (mVariables.containsKey(sKey)) {
            return mVariables.get(sKey);
        }
        if (mALRs.containsKey(sKey)) {
            return mALRs.get(sKey);
        }
        if (mHints.containsKey(sKey)) {
            return mHints.get(sKey);
        }
        if (mAllProperties.containsKey(sKey)) {
            return mAllProperties.get(sKey);
        }
        if (mUDFs.containsKey(sKey)) {
            return mUDFs.get(sKey);
        }
        if (mSynonyms.containsKey(sKey)) {
            return mSynonyms.get(sKey);
        }
        return null;
    }

    @Nullable
    Object getTypeFromKey(@Nullable String sKey) {
        if (sKey == null || sKey.equals("")) {
            return null;
        }
        switch (sKey) {
            case "ReferencedObject":
            case "ReferencedObjects":
            case "ReferencedObject1":
            case "ReferencedObject2":
            case "ReferencedObject3":
            case "ReferencedObject4":
            case "ReferencedObject5":
                return MObject.class;
            case "ReferencedCharacter":
            case "ReferencedCharacters":
            case "ReferencedCharacter1":
            case "ReferencedCharacter2":
            case "ReferencedCharacter3":
            case "ReferencedCharacter4":
            case "ReferencedCharacter5":
                return MCharacter.class;
            case "ReferencedLocation":
                return MLocation.class;
        }

        MItem o = getItemFromKey(sKey);
        return (o != null) ? o.getClass() : null;
    }

    @NonNull
    String getTypeFromKeyNice(@Nullable String sKey) {
        if (sKey == null || sKey.equals("")) {
            return "";
        }
        if (mLocations.containsKey(sKey)) {
            return "Location";
        }
        if (mObjects.containsKey(sKey)) {
            return "Object";
        }
        if (mTasks.containsKey(sKey)) {
            return "Task";
        }
        if (mEvents.containsKey(sKey)) {
            return "Event";
        }
        if (mCharacters.containsKey(sKey)) {
            return "Character";
        }
        if (mGroups.containsKey(sKey)) {
            return "Group";
        }
        if (mVariables.containsKey(sKey)) {
            return "Variable";
        }
        if (mALRs.containsKey(sKey)) {
            return "Text Override";
        }
        if (mHints.containsKey(sKey)) {
            return "Hint";
        }
        if (mAllProperties.containsKey(sKey)) {
            return "Property";
        }
        if (mUDFs.containsKey(sKey)) {
            return "User Function";
        }
        if (mSynonyms.containsKey(sKey)) {
            return "Synonym";
        }
        return "";
    }

    @Nullable
    public String getNameFromKey(@Nullable String sKey) {
        return getNameFromKey(sKey, true, true, false);
    }

    @Nullable
    public String getNameFromKey(@Nullable String sKey, boolean bQuoted) {
        return getNameFromKey(sKey, bQuoted, true, false);
    }

    @Nullable
    public String getNameFromKey(@Nullable String sKey, boolean bQuoted, boolean bPrefixItem) {
        return getNameFromKey(sKey, bQuoted, bPrefixItem, false);
    }

    @Nullable
    public String getNameFromKey(@Nullable String sKey, boolean bQuoted, boolean bPrefixItem, boolean bPCase) {
        String sQ = "";
        String sO = "";
        String sC = "";

        if (sKey == null) {
            MGlobals.errMsg("Bad Key");
            return null;
        }

        if (bQuoted) {
            sQ = "'";
        } else {
            sO = "[ ";
            sC = " ]";
        }

        int len = Math.min(16, sKey.length());
        if (sKey.startsWith("Referenced")) {
            switch (sKey.substring(0, len)) {
                case "ReferencedCharac":
                    switch (sKey) {
                        case "ReferencedCharacters":
                            return sO + sKey.replace("ReferencedCharacters", "Referenced Characters") + sC;
                        case "ReferencedCharacter":
                            return sO + sKey.replace("ReferencedCharacter", "Referenced Character") + sC;
                        default:
                            return sO + sKey.replace("ReferencedCharacter", "Referenced Character ") + sC;
                    }
                case "ReferencedDirect":
                    switch (sKey) {
                        case "ReferencedDirections":
                            return sO + sKey.replace("ReferencedDirections", "Referenced Directions") + sC;
                        case "ReferencedDirection":
                            return sO + sKey.replace("ReferencedDirection", "Referenced Direction") + sC;
                        default:
                            return sO + sKey.replace("ReferencedDirection", "Referenced Direction ") + sC;
                    }
                case "ReferencedObject":
                    switch (sKey) {
                        case "ReferencedObjects":
                            return sO + sKey.replace("ReferencedObjects", "Referenced Objects") + sC;
                        case "ReferencedObject":
                            return sO + sKey.replace("ReferencedObject", "Referenced Object") + sC;
                        default:
                            return sO + sKey.replace("ReferencedObject", "Referenced Object ") + sC;
                    }
                case "ReferencedNumber":
                    switch (sKey) {
                        case "ReferencedNumbers":
                            return sO + sKey.replace("ReferencedNumbers", "Referenced Numbers") + sC;
                        case "ReferencedNumber":
                            return sO + sKey.replace("ReferencedNumber", "Referenced Number") + sC;
                        default:
                            return sO + sKey.replace("ReferencedNumber", "Referenced Number ") + sC;
                    }
                case "ReferencedText":
                    switch (sKey) {
                        case "ReferencedText":
                            return sO + sKey.replace("ReferencedText", "Referenced Text") + sC;
                        default:
                            return sO + sKey.replace("ReferencedText", "Referenced Text ") + sC;
                    }
                case "ReferencedLocati":
                    switch (sKey) {
                        case "ReferencedLocation":
                            return sO + sKey.replace("ReferencedLocation", "Referenced Location") + sC;
                    }
                    break;
                case "ReferencedItem":
                    return sO + sKey.replace("ReferencedItem", "Referenced Item") + sC;
            }
        } else if (sKey.startsWith("Parameter-")) {
            return sO + sKey.replace("Parameter-", "") + sC;
        }

        if (sKey.equals(ANYOBJECT)) {
            return sO + "Any Object" + sC;
        }
        if (sKey.equals(ANYCHARACTER)) {
            return sO + "Any Character" + sC;
        }
        if (sKey.equals(NOOBJECT)) {
            return sO + "No Object" + sC;
        }
        if (sKey.equals(THEFLOOR)) {
            return sO + (bPCase ? "The Floor" : "the Floor") + sC;
        }
        if (sKey.equals(THEPLAYER)) {
            return (bPCase ? sO + "The Player Character" + sC : "the Player character");
        }
        if (sKey.equals(CHARACTERPROPERNAME)) {
            return (bPrefixItem ? (bPCase ? "Property " : "property ") : "") + sQ + "Name" + sQ;
        }
        if (sKey.equals(PLAYERLOCATION)) {
            return (bPCase ? sO + "The Player's Location" + sC : "the Player's location");
        }

        if (mLocations.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Location " : "location ") : "") + sQ +
                    mLocations.get(sKey).getShortDescription().toString() + sQ;
        }
        if (mObjects.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Object " : "object ") : "") + sQ + mObjects.get(sKey).getFullName() + sQ;
        }
        if (mTasks.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Task " : "task ") : "") + sQ + mTasks.get(sKey).getDescription() + sQ;
        }
        if (mEvents.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Event " : "event ") : "") + sQ + mEvents.get(sKey).getDescription() + sQ;
        }
        if (mCharacters.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Character  " : "character ") : "") + sQ + mCharacters.get(sKey).getName() + sQ;
        }
        if (mGroups.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Group " : "group ") : "") + sQ + mGroups.get(sKey).getName() + sQ;
        }
        if (mVariables.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Variable " : "variable ") : "") + sQ + mVariables.get(sKey).getName() + sQ;
        }
        if (mALRs.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Text Override " : "text override ") : "") + sQ + mALRs.get(sKey).getOldText() + sQ;
        }
        if (mHints.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Hint " : "hint ") : "") + sQ + mHints.get(sKey).getQuestion() + sQ;
        }
        if (mAllProperties.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Property " : "property ") : "") + sQ + mAllProperties.get(sKey).getDescription() + sQ;
        }
        if (mUDFs.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "User Function " : "user function ") : "") + sQ + mUDFs.get(sKey).mName + sQ;
        }
        if (mSynonyms.containsKey(sKey)) {
            return (bPrefixItem ? (bPCase ? "Synonym " : "synonym ") : "") + sQ + mSynonyms.get(sKey).getCommonName() + sQ;
        }
        return sKey;
    }

    @NonNull
    public MTaskHashMap getTaskList(MTasksListEnum eTasksList) {
        switch (eTasksList) {
            case AllTasks:
                if (mAllTasks == null) {
                    mAllTasks = mTasks;
                }
                return mAllTasks;

            case GeneralTasks:
                if (mGeneralTasks == null) {
                    mGeneralTasks = new MTaskHashMap(this);
                    for (MTask Task : mTasks.values()) {
                        if (Task.mType == General) {
                            mGeneralTasks.put(Task.getKey(), Task);
                        }
                    }
                }
                return mGeneralTasks;

            case GeneralAndOverrideableSpecificTasks:
                if (mGeneralAndOverrideableSpecificTasks == null) {
                    mGeneralAndOverrideableSpecificTasks = new MTaskHashMap(this);
                    for (MTask Task : mTasks.values()) {
                        if (Task.mType == General) {
                            mGeneralAndOverrideableSpecificTasks.put(Task.getKey(), Task);
                        }
                        // A specific task is overrideable if any of the specifics are unspecified
                        if (Task.mType == Specific) {
                            if (Task.mSpecifics != null) {
                                for (MTask.MSpecific s : Task.mSpecifics) {
                                    if (s.mKeys.size() == 1) {
                                        if (s.mKeys.get(0).equals("")) {
                                            mGeneralAndOverrideableSpecificTasks.put(Task.getKey(), Task);
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return mGeneralAndOverrideableSpecificTasks;

            case SpecificTasks:
                if (mSpecificTasks == null) {
                    mSpecificTasks = new MTaskHashMap(this);
                    for (MTask Task : mTasks.values()) {
                        if (Task.mType == Specific) {
                            mSpecificTasks.put(Task.getKey(), Task);
                        }
                    }
                }
                return mSpecificTasks;

            case SystemTasks:
                if (mSystemTasks == null) {
                    mSystemTasks = new MTaskHashMap(this);
                    for (MTask Task : mTasks.values()) {
                        if (Task.mType == MTask.TaskTypeEnum.System) {
                            mSystemTasks.put(Task.getKey(), Task);
                        }
                    }
                }
                return mSystemTasks;
        }

        return new MTaskHashMap(this);    // return an empty hash table
    }

    public MDescription getIntroduction() {
        return mIntroduction;
    }

    public void setIntroduction(MDescription value) {
        mIntroduction = value;
    }

    public MDescription getEndGameText() {
        return mEndGameText;
    }

    public void setEndGameText(MDescription value) {
        mEndGameText = value;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String value) {
        mTitle = value;
    }

    public String getAuthor() {
        return mAuthor;
    }

    public void setAuthor(String value) {
        mAuthor = value;
    }

    public String getDefaultFontName() {
        return mDefaultFontName;
    }

    public void setDefaultFontName(String value) {
        if (!value.equals("")) {
            mDefaultFontName = value;
        }
    }

    public int getDefaultFontSize() {
        return mDefaultFontSize;
    }

    public void setDefaultFontSize(int value) {
        if (value >= 8 && value <= 36) {
            mDefaultFontSize = value;
        }
    }

    public Typeface getDefaultFont() {
        if (mDefaultFont == null) {
            mDefaultFont = Typeface.create(getDefaultFontName(), Typeface.NORMAL);
        }
        return mDefaultFont;
    }

    public int getWaitTurns() {
        return mWaitTurns;
    }

    public void setWaitTurns(int value) {
        mWaitTurns = value;
    }

    public String getNotUnderstood() {
        return mNotUnderstoodMsg;
    }

    public void setNotUnderstood(String value) {
        mNotUnderstoodMsg = value;
    }

    public String getFilename() {
        return mFilename;
    }

    public void setFilename(String value) {
        mFilename = value;
    }

    public String getFullPath() {
        return mFullPath;
    }

    public void setFullPath(String value) {
        mFullPath = value;
    }

    private boolean getShowFirstRoom() {
        return mShowFirstRoom;
    }

    public void setShowFirstRoom(boolean value) {
        mShowFirstRoom = value;
    }

    boolean getShowExits() {
        return mShowExits;
    }

    public void setShowExits(boolean value) {
        mShowExits = value;
    }

    public boolean getEnableMenu() {
        return mEnableMenu;
    }

    public void setEnableMenu(boolean value) {
        mEnableMenu = value;
    }

    public boolean getEnableDebugger() {
        return mEnableDebugger;
    }

    public void setEnableDebugger(boolean value) {
        mEnableDebugger = value;
    }

    public boolean getEnabled(EnabledOptionEnum eOption) {
        return mEnabledOptions.get(eOption);
    }

    public void setEnabled(EnabledOptionEnum eOption, boolean value) {
        mEnabledOptions.put(eOption, value);
    }

    private ArrayList<String> getAllKeys() {
        ArrayList<String> salKeys = new ArrayList<>();
        salKeys.addAll(mALRs.keySet());
        salKeys.addAll(mCharacters.keySet());
        salKeys.addAll(mEvents.keySet());
        salKeys.addAll(mGroups.keySet());
        salKeys.addAll(mHints.keySet());
        salKeys.addAll(mLocations.keySet());
        salKeys.addAll(mObjects.keySet());
        salKeys.addAll(mAllProperties.keySet());
        salKeys.addAll(mTasks.keySet());
        salKeys.addAll(mVariables.keySet());
        salKeys.addAll(mUDFs.keySet());

        return salKeys;
    }

    public enum TaskExecutionEnum {
        HighestPriorityTask,                    // 0
        HighestPriorityPassingTask              // 1, v4 logic - tries to execute first matching passing task, if that fails first matching failing task
    }

    public enum MTasksListEnum {
        AllTasks,                               // 0
        GeneralTasks,                           // 1
        GeneralAndOverrideableSpecificTasks,    // 2
        SpecificTasks,                          // 3
        SystemTasks                             // 4
    }

    public enum EnabledOptionEnum {
        ShowExits,                              // 0
        EightPointCompass,                      // 1
        BattleSystem,                           // 2
        Sound,                                  // 3
        Graphics,                               // 4
        UserStatusBox,                          // 5
        Score,                                  // 6
        ControlPanel,                           // 7
        Debugger,                               // 8
        Map,                                    // 9
        AutoComplete,                           // 10
        MouseClicks                             // 11
    }

    public enum DirectionsEnum {
        North,          // 0
        East,           // 1
        South,          // 2
        West,           // 3
        Up,             // 4
        Down,           // 5
        In,             // 6
        Out,            // 7
        NorthEast,      // 8
        SouthEast,      // 9
        SouthWest,      // 10
        NorthWest       // 11
    }

    public enum MPronounEnum {
        None,                // -1
        Subjective,          // 0   I/You/He/She/It/We/They
        Objective,           // 1   Me/You/Him/Her/It/Us/Them
        Reflective,          // 2   Myself/Yourself/Himself/Herself/Itself/Ourselves/Themselves
        Possessive           // 3   Mine/Yours/His/Hers/Its/Ours/Theirs
    }

    private static class CommmandUpdater {
        /**
         * Converts a lazy Advanced Command to strict format,
         * removing possibility for double spaces.
         *
         * @param sCommand - the command to convert
         * @return the converted command.
         */
        @NonNull
        static String correctCommand(@NonNull String sCommand) {
            String sNewCommand = processBlock(sCommand);
            if (!sNewCommand.equals(sCommand)) {
                MView.debugPrint(MGlobals.ItemEnum.General, "",
                        MView.DebugDetailLevelEnum.High,
                        "Converted \"" + sCommand + "\" to \"" + sNewCommand + "\"");
            }
            return sNewCommand;
        }

        @NonNull
        private static String processBlock(@NonNull String sBlock) {
            // A block should be the complete entry between two brackets, or between a bracket and a slash
            StringBuilder sAfter = new StringBuilder(sBlock);
            String sNextBlock;
            String sBefore = "";

            do {
                sNextBlock = getSubBlock(sAfter);
                if (!sNextBlock.equals("")) {
                    if (sNextBlock.startsWith("{")) {
                        // */ "] {#} {" => "]{ #}{"
                        // "] {#} [" => ?
                        // "} {#} [" => ?
                        // "} {#} {" => ? /*
                        // "{#} " => "{# }" if block starts with open bracket
                        // " {#} " => " {# }"
                        // "}{#} " => "}{# }"        -- should this be " }{#} " => " }{# }" ?
                        boolean bContainsMandatory = containsMandatoryText(sAfter);
                        if (bContainsMandatory && sAfter.toString().startsWith(" ")) {
                            // If before final [] block then _{x}_ => _{x_}
                            if (sBefore.equals("") || sBefore.endsWith(" ") || sBefore.endsWith("}")) {
                                if (sNextBlock.contains("/")) {
                                    sNextBlock = "{[" + MGlobals.left(sNextBlock.substring(1), sNextBlock.length() - 2) + "] }";
                                } else {
                                    sNextBlock = MGlobals.left(sNextBlock, sNextBlock.length() - 1) + " }";
                                }
                                sAfter = new StringBuilder(MGlobals.right(sAfter.toString(), sAfter.length() - 1));
                            }
                        } else if (!bContainsMandatory && sBefore.endsWith(" ")) {
                            // If after final [] block then _{x}_ => {_x}_
                            if (sNextBlock.contains("/")) {
                                sNextBlock = "{ [" + MGlobals.left(sNextBlock.substring(1), sNextBlock.length() - 2) + "]}";
                            } else {
                                sNextBlock = "{ " + MGlobals.left(sNextBlock.substring(1), sNextBlock.length() - 1);
                            }
                            sBefore = MGlobals.left(sBefore, sBefore.length() - 1);
                        }

                        // End block
                        // " {#}" => "{ #}" or "{ [#/#]}
                        if (sBefore.endsWith(" ") && sAfter.length() == 0) {
                            if (sNextBlock.contains("/")) {
                                sNextBlock = "{ [" + MGlobals.left(sNextBlock.substring(1), sNextBlock.length() - 2) + "]}";
                            } else {
                                sNextBlock = "{ " + MGlobals.left(sNextBlock.substring(1), sNextBlock.length() - 1);
                            }
                            sBefore = MGlobals.left(sBefore, sBefore.length() - 1);
                        }
                        sBefore += "{" + processBlock(MGlobals.mid(sNextBlock, 2, sNextBlock.length() - 2)) + "}";
                    } else if (sNextBlock.startsWith("[")) {
                        sBefore += "[" + processBlock(MGlobals.mid(sNextBlock, 2, sNextBlock.length() - 2)) + "]";
                    } else if (sNextBlock.endsWith("/")) {
                        sBefore += processBlock(MGlobals.left(sNextBlock, sNextBlock.length() - 1)) + "/";
                    } else {
                        sBefore += sNextBlock;
                    }
                }
            } while (sAfter.length() != 0);

            return sBefore;
        }

        private static boolean containsMandatoryText(@NonNull final StringBuilder sBlock) {
            // Returns True if any part of the block is mandatory (either inside [] or not in brackets at all)
            if (sBlock.length() == 0) {
                return false;
            }

            int iLevel = 0;
            for (int i = 0; i < sBlock.length(); i++) {
                switch (sBlock.charAt(i)) {
                    case ' ':
                        // Ignore
                        break;
                    case '{':
                        iLevel++;
                        break;
                    case '}':
                        iLevel--;
                        break;
                    default:
                        if (iLevel == 0) {
                            return true;
                        }
                        break;
                }
            }

            return false;
        }

        @NonNull
        private static String getSubBlock(@NonNull StringBuilder sBlock) {
            // E.g. from "get {the} ball" > "get ", from "{the} ball" > "the", " ball" > ""
            int iDepth = 0;
            StringBuilder sNewBlock = new StringBuilder();

            for (int i = 0; i < sBlock.length(); i++) {
                sNewBlock.append(sBlock.charAt(i));
                switch (sBlock.charAt(i)) {
                    case '{':
                    case '[':
                        if (iDepth == 0 && !sNewBlock.toString().equals(String.valueOf(sBlock.charAt(i)))) {
                            StringBuilder tmp = new StringBuilder(MGlobals.right(sBlock.toString(), sBlock.length() - sNewBlock.length() + 1));
                            sBlock.setLength(0);
                            sBlock.append(tmp);
                            return MGlobals.left(sNewBlock.toString(), i);
                        }
                        iDepth++;
                        break;
                    case ']':
                    case '}':
                        iDepth--;
                        if (iDepth == 0) {
                            StringBuilder tmp = new StringBuilder(MGlobals.right(sBlock.toString(), sBlock.length() - sNewBlock.length()));
                            sBlock.setLength(0);
                            sBlock.append(tmp);
                            return sNewBlock.toString();
                        }
                        break;
                    case '/':
                        if (iDepth == 0) {
                            StringBuilder tmp = new StringBuilder(MGlobals.right(sBlock.toString(), sBlock.length() - sNewBlock.length()));
                            sBlock.setLength(0);
                            sBlock.append(tmp);
                            return sNewBlock.toString();
                        }
                        break;
                }
            }

            sBlock.setLength(0);
            return sNewBlock.toString();
        }
    }

    public static class v4Media {
        public int mOffset;
        public int mLength;
        boolean mIsImage;

        public v4Media(int offset, int length, boolean isImage) {
            mOffset = offset;
            mLength = length;
            mIsImage = isImage;
        }
    }

    public static class MSearchOptions {
        SearchInWhatEnum mSearchInWhat = SearchInWhatEnum.NonLibraryItems;

        public enum SearchInWhatEnum {
            Uninitialised,  // = -1
            AllItems,       // = 0
            NonLibraryItems // = 1
        }
    }

    static class MPronounInfo {
        String mKey = ""; // What is the pronoun applying to?
        int mOffset; // Where in the command does this substitution take place
        MPronounEnum mPronoun = Subjective;
        MCharacter.Gender mGender = MCharacter.Gender.Male;
    }

    public static class MPronounInfoList extends ArrayList<MPronounInfo> {
        public void add(String sKey, MPronounEnum ePronoun,
                        MCharacter.Gender Gender, int iOffset) {
            MPronounInfo pi = new MPronounInfo();
            pi.mKey = sKey;
            pi.mPronoun = ePronoun;
            pi.mOffset = iOffset;
            pi.mGender = Gender;
            add(pi);

            // Ensure the list is sorted by offset, for checking previous pronouns
            Collections.sort(this, new Comparator<MPronounInfo>() {
                @Override
                public int compare(MPronounInfo x, MPronounInfo y) {
                    return (x.mOffset - y.mOffset);
                }
            });
        }
    }
}
