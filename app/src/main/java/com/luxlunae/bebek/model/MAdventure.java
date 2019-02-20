/*
 * Copyright (C) 2019 Tim Cadogan-Cowper.
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
import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.controller.MController;
import com.luxlunae.bebek.model.collection.MALRHashMap;
import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MEventHashMap;
import com.luxlunae.bebek.model.collection.MGroupHashMap;
import com.luxlunae.bebek.model.collection.MHintHashMap;
import com.luxlunae.bebek.model.collection.MItemHashMap;
import com.luxlunae.bebek.model.collection.MLocationHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MOrderedHashMap;
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
import java.util.EnumSet;
import java.util.HashSet;
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
import static com.luxlunae.bebek.MGlobals.ItemEnum.Task;
import static com.luxlunae.bebek.MGlobals.NOCHARACTER;
import static com.luxlunae.bebek.MGlobals.NOOBJECT;
import static com.luxlunae.bebek.MGlobals.PLAYERLOCATION;
import static com.luxlunae.bebek.MGlobals.THEFLOOR;
import static com.luxlunae.bebek.MGlobals.THEPLAYER;
import static com.luxlunae.bebek.MGlobals.appendDoubleSpace;
import static com.luxlunae.bebek.MGlobals.chopLast;
import static com.luxlunae.bebek.MGlobals.contains;
import static com.luxlunae.bebek.MGlobals.containsWord;
import static com.luxlunae.bebek.MGlobals.findIgnoreCase;
import static com.luxlunae.bebek.MGlobals.getArgs;
import static com.luxlunae.bebek.MGlobals.instr;
import static com.luxlunae.bebek.MGlobals.left;
import static com.luxlunae.bebek.MGlobals.mid;
import static com.luxlunae.bebek.MGlobals.numberToString;
import static com.luxlunae.bebek.MGlobals.replaceAllIgnoreCase;
import static com.luxlunae.bebek.MGlobals.replaceWord;
import static com.luxlunae.bebek.MGlobals.right;
import static com.luxlunae.bebek.MGlobals.stripCarats;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.inputBox;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.VB.msgBoxYesNo;
import static com.luxlunae.bebek.model.MAction.EndGameEnum.Neutral;
import static com.luxlunae.bebek.model.MAction.EndGameEnum.Running;
import static com.luxlunae.bebek.model.MAdventure.CommmandUpdater.correctCommand;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Objective;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Possessive;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Reflective;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Subjective;
import static com.luxlunae.bebek.model.MAdventure.MSearchOptions.SearchInWhatEnum.NonLibraryItems;
import static com.luxlunae.bebek.model.MAdventure.MSystemTask.executeSystemTask;
import static com.luxlunae.bebek.model.MAdventure.MTasksListEnum.GeneralTasks;
import static com.luxlunae.bebek.model.MCharacter.Gender.Male;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TimeBased;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TurnBased;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.CountingDownToStart;
import static com.luxlunae.bebek.model.MEvent.StatusEnum.NotYetStarted;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllObjects;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideOrOnObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.OnObject;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.General;
import static com.luxlunae.bebek.model.MTask.TaskTypeEnum.Specific;
import static com.luxlunae.bebek.model.MVariable.VariableType.Numeric;
import static com.luxlunae.bebek.model.MVariable.VariableType.Text;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.Blorb;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.Exe;
import static com.luxlunae.bebek.model.io.MFileIO.FileTypeEnum.TextAdventure_TAF;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.All;
import static com.luxlunae.bebek.model.io.MFileIO.loadFile;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.High;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Low;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;

public class MAdventure {
    private static final Pattern RE_EXP =
            Pattern.compile("<#(.*?)#>", Pattern.DOTALL);  // . doesn't match new lines unless we use Pattern.DOTALL
    private static final Pattern RE_IGNORE =
            Pattern.compile(".*?(?<embeddedexpression><#.*?#>).*?", Pattern.DOTALL);
    private static final Pattern RE_ITEM_FUNC =
            Pattern.compile("(?!<#.*?)(?<firstkey>%?[A-Za-z][\\w\\|_]*%?)(?<nextkey>\\.%?[A-Za-z][\\w\\|_]*%?(\\(.+?\\))?)+(?!.*?#>)",
                    Pattern.DOTALL);
    private static final Pattern RE_PERSPECTIVE =
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
    public final EnumMap<DirectionsEnum, String> mDirectionNames = new EnumMap<>(DirectionsEnum.class);
    @NonNull
    public final LinkedHashMap<String, v4Media> mV4Media = new LinkedHashMap<>();
    @NonNull
    public final Hashtable<String, Integer> mBlorbMappings = new Hashtable<>();
    @NonNull
    public final MView mView;
    @NonNull
    final Queue<MTask> mTasksToRun = new ArrayDeque<>();
    @NonNull
    public final Hashtable<MCharacter.Gender, List<String>> mCharsMentionedThisTurn = new Hashtable<>();
    @NonNull
    final MPronounInfoList mPronounKeys = new MPronounInfoList();
    @NonNull
    private final Hashtable<String, ArrayList<Integer>> mRandomValues = new Hashtable<>();
    @NonNull
    private final MSearchOptions mSearchOptions = new MSearchOptions();
    @NonNull
    private final EnumMap<EnabledOptionEnum, Boolean> mEnabledOptions = new EnumMap<>(EnabledOptionEnum.class);
    @NonNull
    public String mRouteErrorText = "";
    @NonNull
    String mInput = "";
    @NonNull
    public MOrderedHashMap<String, MReferenceList> mPassResponses = new MOrderedHashMap<>();
    @Nullable
    public MReferenceList mReferences;
    @NonNull
    public String mRestrictionText = "";
    @NonNull
    public
    String mHer = "";
    @NonNull
    public
    String mIt = "";
    @NonNull
    public
    String mHim = "";
    @NonNull
    public
    String mThem = "";
    @NonNull
    public String mTurnOutput = "";
    public double mVersion;
    public int iElapsed;
    public int mTurns;
    public MAction.EndGameEnum mGameState = Running;
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
    boolean mEventsRunning = false;
    @NonNull
    MOrderedHashMap<String, MReferenceList> mFailResponses = new MOrderedHashMap<>();
    /**
     * Who we are in conversation with.
     */
    @NonNull
    String mConversationCharKey = "";
    /**
     * Where we currently are in the conversation tree.
     */
    @NonNull
    String mConversationNode = "";
    private boolean mJustRunSystemTask = false;
    private boolean mDisplayedWinOrLose = false;
    @NonNull
    private String mLastProperInput = "";
    @NonNull
    private String mRememberedVerb = "";
    @Nullable
    private MTask mAmbiguousTask;
    @Nullable
    private HashSet<String> mKnownWords;
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
    private String mLibAdriftPath;

    public MAdventure(@NonNull MView v) {
        mView = v;

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

        for (MCharacter.Gender g : MCharacter.Gender.values()) {
            mCharsMentionedThisTurn.put(g, new ArrayList<String>());
        }
    }

    /**
     * Take a list of ambiguous keys of type keyType, then
     * try to reduce that list to one (hopefully). We reduce the
     * keys by removing any that refer to an item that doesn't
     * contain all of the input words in its name or descriptors.
     *
     * @param keys    - the keys to reduce.
     * @param keyType - the type of the keys (object, character or location).
     * @param input   - the input to test against.
     * @return a list of keys that is either the same as, or
     * reduced from, keys.
     */
    @NonNull
    public MStringArrayList resolveKeys(final @NonNull MStringArrayList keys,
                                        MReference.ReferencesType keyType,
                                        @NonNull String input) {
        // Check each word in the refined text to ensure they all match
        MStringArrayList ret = new MStringArrayList();
        String[] inputWords = input.split(" ");

        switch (keyType) {
            case Object:
                for (String obKey : keys) {
                    // Get the object associated with this obKey.
                    MObject ob = mObjects.get(obKey);
                    if (ob == null) {
                        // Key refers to a non-existent object.
                        // Ignore it.
                        continue;
                    }
                    String obArticle = ob.getArticle();
                    String obPrefix = ob.getPrefix();
                    MStringArrayList obNames = ob.getNames();

                    // Try to match each of the input words to
                    // either "the", the object's article, or
                    // a word in the object's prefix or name.
                    boolean matchesAll = true;
                    for (String word : inputWords) {
                        boolean wordInObject = false;
                        if (word.equals("the") || word.equals(obArticle) ||
                                containsWord(obPrefix, word)) {
                            wordInObject = true;
                        } else {
                            for (String obName : obNames) {
                                if (containsWord(obName, word)) {
                                    wordInObject = true;
                                    break;
                                }
                            }
                        }
                        if (!wordInObject) {
                            matchesAll = false;
                            break;
                        }
                    }
                    if (matchesAll) {
                        ret.add(obKey);
                    }
                }
                break;

            case Character:
                for (String chKey : keys) {
                    MCharacter ch = mCharacters.get(chKey);
                    if (ch == null) {
                        // Key refers to a non-existent character.
                        // Ignore it.
                        continue;
                    }
                    String chPrefix = ch.getPrefix();
                    String chName = ch.getProperName();
                    MStringArrayList chDescs = ch.mDescriptors;

                    // Try to match each of the input words to
                    // a word in the character's prefix, name or
                    // descriptors.
                    boolean matchesAll = true;
                    for (String word : inputWords) {
                        boolean wordInChar = false;
                        if (containsWord(chPrefix, word) ||
                                containsWord(chName, word)) {
                            wordInChar = true;
                        } else {
                            for (String chDesc : chDescs) {
                                if (containsWord(chDesc, word)) {
                                    wordInChar = true;
                                    break;
                                }
                            }
                        }
                        if (!wordInChar) {
                            matchesAll = false;
                            break;
                        }
                    }
                    if (matchesAll) {
                        ret.add(chKey);
                    }
                }
                break;

            case Location:
                for (String locKey : keys) {
                    MLocation loc = mLocations.get(locKey);
                    if (loc == null) {
                        // Key refers to a non-existent location.
                        // Ignore it.
                        continue;
                    }
                    String desc = loc.getShortDescription().toString(true);

                    // Try to match each of the input words to
                    // a word in the location's short description.
                    boolean matchesAll = true;
                    for (String word : inputWords) {
                        if (!containsWord(desc, word)) {
                            matchesAll = false;
                            break;
                        }
                    }
                    if (matchesAll) {
                        ret.add(locKey);
                    }
                }
                break;
        }

        return ret;
    }

    public int safeInt(@Nullable Object expr) {
        if (expr == null) {
            return 0;
        }
        String s = expr.toString().trim();
        if (s.equals("")) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                mView.errMsg("safeInt error with expr <" + expr.toString() + ">", ex);
                return 0;
            }
        }
    }

    public boolean safeBool(@Nullable Object expr) {
        if (expr == null) {
            return false;
        }
        String s = expr.toString().trim();
        if (s.equals("")) {
            return false;
        }
        try {
            switch (s.toUpperCase()) {
                case "TRUE":
                    return true;
                case "FALSE":
                    return false;
                default:
                    return cbool(s);
            }
        } catch (Exception ex) {
            mView.errMsg("safeBool error with expr <" + expr.toString() + ">", ex);
            return false;
        }
    }

    /**
     * Attempt to load the contents of the given file into this Adventure object.
     *
     * @param path - the file to load.
     * @return TRUE if successfully loaded, FALSE otherwise.
     * @throws InterruptedException
     */
    public boolean open(@NonNull String path) throws InterruptedException {
        mStates.clear();
        mCommands.clear();
        mCommands.add("");
        MFileIO.FileTypeEnum fileTypeEnum = TextAdventure_TAF;
        String pathLower = path.toLowerCase();
        if (pathLower.endsWith(".blorb")) {
            fileTypeEnum = Blorb;
        }
        if (pathLower.endsWith(".exe")) {
            fileTypeEnum = Exe;
        }

        if (!loadFile(this, path, fileTypeEnum, All, false, null, 0)) {
            return false;
        }

        if (mCompatBattleSystem) {
            mView.out("<b>WARNING: This game has the ADRIFT battle system enabled. " +
                    "Bebek doesn't support that yet. The game may not work as intended.<br><br>" +
                    "Press any key to continue...</b><waitkey>");
        }

        // Ensure the tasks are sorted by priority and
        // the ALRs are sorted by decreasing length of old text.
        // We only need to do this once for each play session
        mTasks.sort();
        mALRs.sort();

        for (DirectionsEnum dir : DirectionsEnum.values()) {
            mDirectionNames.put(dir, mDirectionNames.get(dir).toLowerCase());
        }

        mGameState = Running;

        // Initialise any variable array values
        for (MVariable v : mVariables.values()) {
            if (v.getLength() > 1) {
                String initVals[] = v.getStr().split("\n");
                if (initVals.length == v.getLength()) {
                    int i = 1;
                    for (String val : initVals) {
                        if (v.getType() == Numeric) {
                            v.setAt(i, safeInt(val));
                        } else {
                            v.setAt(i, val.replace("\r", ""));
                        }
                        i++;
                    }
                }
            }
        }

        for (MTask t : mTasks.values()) {
            for (int i = 0; i < t.mCommands.size(); i++) {
                t.mCommands.set(i, correctCommand(t.mCommands.get(i), this));
            }
            if (t.mType == MTask.TaskTypeEnum.System && t.getRunImmediately()) {
                t.attemptToExecute(true);
            }
        }

        mView.updateStatusBar(this);

        String playerLocKey = getPlayer().getLocation().getLocationKey();
        getPlayer().setHasSeenLocation(playerLocKey, true);
      /*  UserSession.Map.RecalculateNode(Adventure.Map.FindNode(Adventure.Player.Location.LocationKey));
        UserSession.Map.SelectNode(Adventure.Player.Location.LocationKey); */
        mView.clearTextWindow();
        mView.displayText(this, "<c>" + getTitle() + "</c>" + "\n", true);
        mView.displayText(this, getIntroduction().toString(), true);

        // ----------------------------------------------------
        // If specified, show the description of the first room
        // ----------------------------------------------------
        if (getShowFirstRoom() && mLocations.containsKey(playerLocKey)) {
            StringBuilder txt = new StringBuilder();
            txt.append("\n");
            txt.append(mLocations.get(playerLocKey).getViewLocation());
            appendDoubleSpace(txt);
            mView.displayText(this, txt.toString(), true);
        }

        for (MEvent e : mEvents.values()) {
            switch (e.mWhenStart) {
                case AfterATask:
                    e.mStatus = NotYetStarted;
                    break;
                case BetweenXandYTurns:
                    e.mStatus = CountingDownToStart;
                    e.setTimerToEndOfEvent(e.mStartDelay.getValue() + e.mLength.getValue());
                    break;
                case Immediately:
                    e.start(true);
                    break;
            }
        }

        // Needs to be a separate loop in case a later event
        // runs a task that starts an earlier event
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
                    t.mKeywords = correctCommand(t.mKeywords, this);
                }
                if (!topickeys.containsKey(t.mKeywords)) {
                    topickeys.put(t.mKeywords, new ArrayList<String>());
                }
                topickeys.get(t.mKeywords).add(t.mKey);
            }
            MTopicHashMap topicsNew = new MTopicHashMap();
            for (String topicKey : topickeys.keySet()) {
                for (String key : topickeys.get(topicKey)) {
                    topicsNew.put(c.mTopics.get(key));
                }
            }
            c.mTopics = topicsNew;
        }

        mView.displayText(this, "\n\n", true);

        mTasks.createTaskReferenceLists();

        if (mLocations.size() == 0) {
            mView.errMsg("This adventure has no locations.  Cannot continue.");
            return false;
        }

        prepareForNextTurn();
        return true;
    }

    public String getLibAdriftPath() {
        return mLibAdriftPath;
    }

    public void setLibAdriftPath(String path) {
        mLibAdriftPath = path;
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
     * @param input - the input string.
     */
    private void grabIt(@NonNull String input) {
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
            String playerKey = getPlayer().getKey();

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
                        objs = mObjects.getVisibleTo(playerKey);
                        chars = mCharacters.getVisibleTo(playerKey);
                    } else {
                        objs = mObjects.getSeenBy(playerKey);
                        chars = mCharacters.getSeenBy(playerKey);
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
                    newIt = reduceItPossibilities(possibleItKeys,
                            words, false, false);
                }
                if (newThem.equals("")) {
                    newThem = reduceItPossibilities(possibleThemKeys,
                            words, true, false);
                }
                if (newHim.equals("")) {
                    newHim = reduceItPossibilities(possibleHimKeys,
                            words, false, true);
                }
                if (newHer.equals("")) {
                    newHer = reduceItPossibilities(possibleHerKeys,
                            words, false, true);
                }
            }

            // --------------------------------------------------------------
            // Finally, set the global "it", "them", "her" and "him" values,
            // based on what we found above
            // --------------------------------------------------------------
            if (!newIt.equals("")) {
                mIt = newIt;
            }
            if (!newThem.equals("")) {
                mThem = newThem;
            }
            if (!newHim.equals("")) {
                mHim = newHim;
            }
            if (!newHer.equals("")) {
                mHer = newHer;
            }
            if (mIt.equals("")) {
                mIt = "Absolutely Nothing";
            }
            if (mThem.equals("")) {
                mThem = "Absolutely Nothing";
            }
            if (mHim.equals("")) {
                mHim = "No male";
            }
            if (mHer.equals("")) {
                mHer = "No female";
            }
        } catch (Exception ex) {
            mView.errMsg("Error grabbing \"it\"", ex);
            mIt = "Absolutely Nothing";
        }
    }

    /**
     * Helper function for grabIt.
     * <p>
     * Searches a list of object and/or character keys. If only one of these keys
     * matches an input word, then returns the name of the key's object or character.
     * Otherwise returns an empty string "".
     *
     * @param keysToSearch - a list of keys to search.
     * @param wordsToFind  - a list of input words to find.
     * @param pluralOnly   - whether we should only consider plural objects.
     * @param charsOnly    - whether we should only consider character keys.
     * @return the unique key, if we were able to determine it. Otherwise an
     * empty string "".
     */
    @NonNull
    private String reduceItPossibilities(@NonNull HashSet<String> keysToSearch,
                                         @NonNull String[] wordsToFind,
                                         boolean pluralOnly, boolean charsOnly) {
        ArrayList<String> keys = new ArrayList<>();

        if (keysToSearch.size() == 1) {
            keys.add(keysToSearch.iterator().next());
        } else if (keysToSearch.size() > 1) {
            for (String key : keysToSearch) {
                MCharacter ch = mCharacters.get(key);
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
                    MObject ob = mObjects.get(key);
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
            MCharacter ch = mCharacters.get(key);
            if (ch != null) {
                return charsOnly ?
                        ch.getName(Subjective, false, false, Definite) :
                        ch.getName();
            }
            if (!charsOnly) {
                MObject ob = mObjects.get(key);
                if (ob != null) {
                    return ob.getFullName(Definite);
                }
            }
        }

        return "";
    }

    /**
     * Helper function for evalInput.
     * <p>
     * Replaces a given word in the input string with another word given
     * word, and displays text showing the interpreted meaning.
     *
     * @param findWord    - the word to find in the input string.
     * @param replaceWord - the word to replace in the input string.
     */
    private void replaceInputWord(@NonNull String findWord,
                                  @NonNull String replaceWord) {
        if (containsWord(mInput, findWord)) {
            mView.displayText(this, "<c>(" + replaceWord + ")</c><br>", true);
            mInput = replaceWord(mInput, findWord, replaceWord);
        }
    }

    /**
     * Checks whether this game is over.
     * <p>
     * If it is, if appropriate and we haven't already displayed
     * it, we display: a message stating the player has won or
     * lost; amy end game text; and the final score. Lastly
     * we display a prompt asking if the player wants to restart,
     * restore, quit, or undo the last command.
     */
    public void checkEndOfGame() {
        if (!mDisplayedWinOrLose) {
            switch (mGameState) {
                case Win:
                    mView.displayText(this,
                            "<center><c><b>*** You have won ***</b></c></center>",
                            true);
                    if (mVersion < 5 && mCompatWinningText != null) {
                        // Display version 3.8, 3.9 or 4.0 winning text
                        //  "Similar to the introduction, you may want a generic
                        //   winning message to appear. This will be displayed
                        //   whenever any winning task is executed."
                        mView.displayText(this, mCompatWinningText + "<br>");
                    }
                    break;
                case Lose:
                    mView.displayText(this,
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
            if (mGameState != Running) {
                mStates.recordState(this);
            }
        }

        if (mGameState != Running) {
            getPlayer().setWalkTo("");
            if (!mDisplayedWinOrLose) {
                MDescription d = getEndGameText();
                if (d != null) {
                    String endGameText = d.toString(true);
                    if (!endGameText.equals("")) {
                        mView.displayText(this, endGameText + "<br>");
                    }
                }
                if (getMaxScore() > 0) {
                    mView.displayText(this,
                            "In that game you scored " + getScore() +
                                    " out of a possible " + getMaxScore() + ", in " +
                                    mTurns + " turns.<br><br>", true);
                }
            }
            mView.displayText(this,
                    "Would you like to <c>restart</c>, <c>restore</c> a saved game, " +
                            "<c>quit</c> or <c>undo</c> the last command?<br><br>", true);
            mDisplayedWinOrLose = true;
        }
    }

    /**
     * Process the user's next input command in the game.
     *
     * @param input - the input to process.
     *
     * @throws InterruptedException if, e.g., the player presses the back key.
     */
    public void processCommand(@NonNull String input) throws InterruptedException {
        if (mCommands.size() == 0) {
            return;
        }

        mCommands.add(input);
        mTurns++;

        mView.mNoDebug = false;
        mView.mDebugIndent = 0;
        mJustRunSystemTask = false;
        mInput = stripCarats(input.trim());
        if (evalInput(0, false).equals("***SYSTEM***")) {
            mView.mOutputText.setLength(0);
            return;
        }
        mView.mNoDebug = true;

        checkEndOfGame();
        if (mGameState != Running) {
            return;
        }

        prepareForNextTurn();
        getPlayer().doWalk();
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
     * @param minPriority         - only tasks with a priority above this number will be
     *                            considered for execution.
     * @param runPassingTasksOnly - only execute a task that passes. Means we already
     *                            have a failing task with output, so don't need another.
     * @return the game's output.
     * @throws InterruptedException if, e.g., the player presses the back key.
     */
    @NonNull
    String evalInput(int minPriority, boolean runPassingTasksOnly) throws InterruptedException {
        mView.debugPrint(MGlobals.ItemEnum.General, "", High,
                "evaluateInput " + minPriority + ", " + runPassingTasksOnly);

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
            replaceInputWord("it", mIt);
            replaceInputWord("them", mThem);
            replaceInputWord("him", mHim);
            replaceInputWord("her", mHer);

            // If mInput is "again", then replace it with the last command just run.
            int nCmds = mCommands.size();
            if ((mInput.equals("again") || mInput.equals("g")) && nCmds > 1) {
                String lastCmd = mCommands.get(nCmds - 2);
                if (lastCmd != null) {
                    mView.displayText(this, "<c>(" + lastCmd + ")</c><br>", true);
                    mInput = lastCmd;
                    mCommands.remove(nCmds - 1); // Don't store 'again'
                }
            }

            // Get the new "it", "them", "him" and "her".
            grabIt(mInput);

            // Replace synonyms.
            String preSyn = mInput;
            for (MSynonym syn : mSynonyms.values()) {
                for (String from : syn.getChangeFrom()) {
                    mInput = replaceWord(mInput, from, syn.getChangeTo());
                }
            }
            if (!mInput.equals(preSyn)) {
                mView.debugPrint(MGlobals.ItemEnum.General, "", Medium,
                        "Synonyms changed input \"" + preSyn + "\" to \"" + mInput + "\"");
            }

            // If cbool(GetSetting("ADRIFT", "Runner", "BlankLine", "0")) Then Display(vbCrLf)
        }

        mInput = mInput.toLowerCase();

        // Don't actually respond to system tasks here, in case the user has created a task
        // to override the system one.
        if (mGameState != Running || mVersion < 5) {
            boolean ret = executeSystemTask(this, mInput, true);
            if (ret) {
                return "";
            } else {
                if (mGameState != Running) {
                    mView.displayText(this, "Please give one of the answers above." + "\n");
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
            // We have an task with ambiguous references from the last turn that
            // we asked a question for. See if the user's response is enough to
            // resolve it.
            // --------------------------------------------------------------------
            if (mAmbiguousTask.mRefsWorking != null &&
                    mAmbiguousTask.mRefsWorking.resolveAmbiguity(mInput)) {
                // We managed to resolve the references.
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
                mReferencedText[i] = "";
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
            MTaskHashMap secondChanceTasks = new MTaskHashMap(this);
            MTaskHashMap.MTaskMatchResult match =
                    mCompletableTasks.find(mInput, minPriority, secondChanceTasks);
            if (match.mTask == null && match.mAmbiguousTask == null) {
                // We didn't find any tasks (either ambiguous or unambiguous).
                // Let's go back and see if one of our 'exist' tasks worked.
                mView.debugPrint(Task, "", Medium,
                        "No matches found.  Checking again using existence.");
                match = secondChanceTasks.find(mInput, minPriority, null);
            }
            if (mReferencedText[0].equals("")) {
                mReferencedText[0] = mInput;
            }

            // --------------------------------------------------------------------
            // Did we find one?
            // --------------------------------------------------------------------
            if (match.mTask == null) {
                // ---------------------------------------------------
                //   Nope. Should we display an ambiguity question?
                // ---------------------------------------------------
                if (match.mAmbiguousTask != null &&
                        minPriority > 0 && mView.mOutputText.length() > 0) {
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
                mReferences = taskToRun.mRefsWorking;
                EnumSet<MTask.ExecutionStatus> curStatus =
                        EnumSet.noneOf(MTask.ExecutionStatus.class);

                // Try to execute it
                taskToRun.attemptToExecute(false,
                        false, curStatus, false,
                        runPassingTasksOnly, false);

                if (taskToRun.mIsSystemTask) {
                    mJustRunSystemTask = true;
                }

                // Try to execute any additional tasks
                while (mTasksToRun.size() > 0) {
                    MTask tas = mTasksToRun.remove();
                    tas.attemptToExecute(true);
                }

                if (minPriority == 0) {
                    if (mView.mOutputText.length() == 0) {
                        // The task didn't produce any output so display the
                        // "not understood" message.
                        displayNotUnderstood();
                    } else {
                        // The task executed successfully. If it was not a
                        // system task, count this as a turn.
                        if (!mJustRunSystemTask) {
                            // Any Events that are running are moved forward
                            // one turn, which could display text or execute
                            // a system task.
                            incrementTurnOrTime(TurnBased);
                        }
                        setChanged(true);
                        mRememberedVerb = "";
                    }
                }
            } else {
                // ----------------------------------------------------------------
                //       We didn't find any tasks (ambiguous or unambiguous).
                // ----------------------------------------------------------------
                if (minPriority == 0) {
                    if (mGameState == Running) {
                        // See if we can interpret the user's input as a
                        // system task...
                        boolean ret = executeSystemTask(this, mInput, false);
                        if (!ret) {
                            // Nope - display the "not understood" message.
                            displayNotUnderstood();
                        }
                        mJustRunSystemTask = true;
                    }
                }
            }

            if (minPriority == 0 &&
                    !mView.mOutputText.toString().equals("***SYSTEM***")) {
                // If we didn't just run a system task, advance to the next line.
                mView.displayText(this, "\n");
            }
        }

        if (!mView.mOutputText.toString().equals("***SYSTEM***")) {
            if (minPriority == 0) {
                // ----------------------------------------------------------------
                // Move onto the next line in readiness for user's next input.
                // This turn is complete.
                // ----------------------------------------------------------------
                mView.displayText(this, "\n", true);
                mView.debugPrint(MGlobals.ItemEnum.General, "", Low, "ENDOFTURN");
            }
            mView.updateStatusBar(this);
        }

        return mView.mOutputText.toString();
    }

    /**
     * Processes a turn or time increment.
     * <p>
     * For turn increments, we increment all walks and events. For
     * time increments, we increment events, flush any output
     * immediately and check for end of game.
     *
     * @param evType - whether the increment is a turn or time increment.
     * @throws InterruptedException if, e.g. user presses back key,
     */
    public void incrementTurnOrTime(@NonNull MEvent.EventTypeEnum evType)
            throws InterruptedException {
        if (mGameState != Running) {
            return;
        }

        // =============================================================
        //                PROCESS WALKS (TURN INCREMENT)
        // -------------------------------------------------------------
        if (evType == TurnBased) {
            for (MCharacter c : mCharacters.values()) {
                for (MWalk w : c.mWalks) {
                    if (mGameState != Running) {
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
        for (MEvent e : mEvents.values()) {
            if (mGameState != Running) {
                return;
            }
            if (e.mEventType == evType) {
                e.incrementTimer();
            }
        }
        // Needs to be a separate loop in case a later event runs a
        // task that starts an earlier event
        for (MEvent e : mEvents.values()) {
            if (e.mEventType == evType) {
                e.mJustStarted = false;
            }
        }
        mEventsRunning = false;

        // =============================================================
        //     FLUSH OUTPUT TEXT AND CHECK END GAME (TIME INCREMENT)
        // -------------------------------------------------------------
        if (evType == TimeBased) {
            if (mView.mOutputText.length() > 0) {
                // Flush any output text immediately.
                mView.displayText(this, "", true);
            }
            checkEndOfGame();
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
     * @throws InterruptedException if, for example, the player presses the back key.
     */
    private void displayNotUnderstood() throws InterruptedException {
        // ------------------------------------------------------------------------
        // If we've got a remembered verb from the last not understood message,
        // see if we can simply add that to the word the user has typed now and
        // evaluate that. E.g. if user typed "eat", then we respond "eat what?",
        // then the user types "apple", we would now try to evaluate "eat apple".
        // ------------------------------------------------------------------------
        if (!mRememberedVerb.equals("")) {
            mInput = mRememberedVerb + " " + mInput;
            mRememberedVerb = "";
            evalInput(-1, false);
            if (mView.mOutputText.length() > 0) {
                return;
            }
        }

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
            for (MTask tas : mTasks.values()) {
                if (tas.mType == General) {
                    for (String cmd : tas.mCommands) {
                        cmd = cmd.replaceAll("[\\[\\]{}/]", " ")
                                .trim().replaceAll(" +", " ");
                        Collections.addAll(mKnownWords, cmd.split(" "));
                    }
                }
            }
            for (MObject ob : mObjects.values()) {
                mKnownWords.add(ob.getArticle());
                Collections.addAll(mKnownWords, ob.getPrefix().split(" "));
                mKnownWords.addAll(ob.getNames());
            }
            for (MCharacter ch : mCharacters.values()) {
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
        for (String inputWord : mInput.split(" ")) {
            if (!mKnownWords.contains(inputWord)) {
                mView.displayText(this,
                        "I did not understand the word \"" + inputWord + "\".");
                return;
            }
        }

        // ------------------------------------------------------------------------
        // If the user just entered a single word verb with no noun, look for
        // a task that matches that verb. If found then print a question asking
        // the player to clarify the noun.
        // ------------------------------------------------------------------------
        if (!mInput.contains(" ")) {
            MTaskHashMap tasks = getTaskList(GeneralTasks);
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
                                    mView.displayText(this, msg.toString());
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
        MCharacter player = getPlayer();
        for (MObject ob : mObjects.values()) {
            String obKey = ob.getKey();
            if (player.hasSeenObject(obKey)) {
                if (player.canSeeObject(obKey)) {
                    Pattern regex = Pattern.compile(ob.getRegEx());
                    Matcher m = regex.matcher(mInput);
                    if (m.find()) {
                        mView.displayText(this,
                                "I don't understand what you want to do with " +
                                        ob.getFullName(Definite) + ".");
                        return;
                    }
                }
            }
        }
        for (MCharacter ch : mCharacters.values()) {
            String chKey = ch.getKey();
            if (player.hasSeenCharacter(chKey)) {
                if (player.canSeeCharacter(chKey)) {
                    Pattern regex = Pattern.compile(ch.getRegEx());
                    Matcher m = regex.matcher(mInput);
                    if (m.find()) {
                        mView.displayText(this,
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
        mView.displayText(this, getNotUnderstood());
    }

    public void prepareForNextTurn() {
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
            String locKey = ch.getLocation().getLocationKey();
            if (!locKey.equals(HIDDEN) && !locKey.equals("")) {
                MLocation loc = mLocations.get(locKey);
                if (loc != null) {
                    loc.setSeenBy(ch.getKey(), true);
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

        for (MCharacter.Gender g : MCharacter.Gender.values()) {
            mCharsMentionedThisTurn.put(g, new ArrayList<String>());
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
    public String evalFuncs(@NonNull String text, @Nullable MReferenceList refs) {
        return evalFuncs(text, refs, false);
    }

    public void evalFuncs(@NonNull StringBuilder text, @Nullable MReferenceList refs) {
        evalFuncs(text, refs, false, true);
    }

    @NonNull
    public String evalFuncs(@NonNull String text, @Nullable MReferenceList refs, boolean isExpr) {
        StringBuilder tmp = new StringBuilder(text);
        evalFuncs(tmp, refs, isExpr, true);
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
     * @param text          - the text containing functions to evaluate
     * @param isExpr        - ??
     * @param evalItemFuncs - should we evaluate any item functions?
     */
    public void evalFuncs(@NonNull StringBuilder text, @Nullable MReferenceList refs,
                          boolean isExpr, boolean evalItemFuncs) {
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
                        MController.AdriftProductVersion.substring(0, 1) +
                                MController.AdriftProductVersion.substring(2, 3) +
                                MController.AdriftProductVersion.substring(4));
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
                                isExpr, MReference.ReferencesType.Object);
                        refs.replaceInText("character" + i, text,
                                isExpr, MReference.ReferencesType.Character);
                        refs.replaceInText("location" + i, text,
                                isExpr, MReference.ReferencesType.Location);
                        refs.replaceInText("item" + i, text,
                                isExpr, MReference.ReferencesType.Item);
                        refs.replaceInText("direction" + i, text,
                                isExpr, MReference.ReferencesType.Direction);
                        refs.replaceInText("number" + String.valueOf(i), text,
                                isExpr, MReference.ReferencesType.Number);
                        String refText = "%text" + String.valueOf(i) + "%";
                        if (contains(text, refText)) {
                            boolean bQuote = isExpr &&
                                    !contains(text, "%text" + String.valueOf(i) + "%.") &&
                                    !contains(text, "\"%text" + String.valueOf(i) + "%\"");
                            for (MReference ref : refs) {
                                if (ref.mType == MReference.ReferencesType.Text) {
                                    if (ref.mRefMatch.equals("text" + i)) {
                                        if (ref.mItems.size() == 1 &&
                                                ref.mItems.get(0).mMatchingKeys.size() == 1) {
                                            replaceAllIgnoreCase(text, refText, isExpr ?
                                                    "\"" + ref.mItems.get(0).mMatchingKeys.get(0) + "\"" :
                                                    ref.mItems.get(0).mMatchingKeys.get(0));
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
                            isExpr, MReference.ReferencesType.Object);
                    refs.replaceInText("characters", text,
                            isExpr, MReference.ReferencesType.Character);
                }

                // =====================================================================
                //                         REPLACE VARIABLES
                // ---------------------------------------------------------------------
                // Replace any variable name references with the current value of that
                // variable. Note that such references are case-insensitive!
                mVariables.evaluate(text, isExpr, refs);

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
                            args = evalFuncs(args, refs);
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
                                        evalGeneralFunc(funcName,
                                                args, oldArgs, isSentenceStart,
                                                refs, isExpr, funcPos);
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
                                mView.displayError("Bad function " + funcName);
                            }
                        } else {
                            // -------------------------------------------------------
                            //        FUNCTION DOESN'T HAVE ARGUMENTS - ERROR
                            // -------------------------------------------------------
                            replaceAllIgnoreCase(text, "%" + funcName + "[]%", "");
                            replaceAllIgnoreCase(text, "%" + funcName + "[", "");
                            mView.displayError("No arguments given to function " + funcName);
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
        if (evalItemFuncs) {
            String prev;
            do {
                prev = text.toString();
                evalItemFunc(text, isExpr, refs);
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
        Matcher match = RE_PERSPECTIVE.matcher(text);
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
    private StringBuilder evalGeneralFunc(@NonNull String funcName,
                                          @NonNull String args,
                                          @NonNull String oldArgs,
                                          boolean isStartOfSentence,
                                          @Nullable MReferenceList refs,
                                          boolean isExpr, int iMatchLoc) {
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
                    ret.append(ch.getDescriptor());
                } else {
                    mView.displayError("Bad Argument to " +
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
                String[] params = args.replace(" ", "").split(",");
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
                    ret.append(ch.getName(pronounType));

                    // Slight fudge - if the name is the start
                    // of a sentence, auto-cap it... (consistent with v4)
                    if (isStartOfSentence) {
                        toProper(ret);
                    }

                    if (mView.mDisplaying) {
                        mPronounKeys.add(chKey,
                                pronounType, ch.getGender(),
                                mView.mOutputText.length() + iMatchLoc);
                    }
                } else if (chKey.equals(NOCHARACTER)) {
                    ret.append("Nobody");
                } else {
                    mView.displayError("Bad Argument to " +
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
                    ret.append(ch.getProperName());
                } else {
                    mView.displayError("Bad Argument to " +
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
                    ret.append(ch.getDescription().toString());
                    if (ret.length() == 0) {
                        ret.append("%CharacterName% see[//s] nothing " +
                                "interesting about %CharacterName[")
                                .append(args).append(", target]%.");
                    }
                } else {
                    mView.displayError("Bad Argument to " +
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
                    ret.append(loc.getViewLocation());
                    if (ret.length() == 0) {
                        ret.append("There is nothing of interest here.");
                    }
                } else {
                    mView.displayError("Bad Argument to " +
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
                    ret.append(ob.getDescription().toString());
                    if (ret.length() == 0) {
                        ret.append("%CharacterName% see[//s] nothing " +
                                "interesting about ")
                                .append(ob.getFullName(Definite))
                                .append(".");
                    }
                } else {
                    mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                    ret.append(ob.getChildChars(OnObject).toList());
                } else {
                    mView.displayError("Bad Argument to " +
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
                    ret.append(ob.getChildChars(InsideObject).toList());
                } else {
                    mView.displayError("Bad Argument to " +
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
                    ret.append(ob.displayChildChars());
                } else {
                    mView.displayError("Bad Argument to " +
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
                            .toList("and", true, Indefinite));
                } else {
                    mView.displayError("Bad Argument to " +
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
                    ret.append(ch.listExits());
                } else {
                    mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                        mView.displayError("Bad Argument to " +
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
                    mView.displayError("Bad Argument to " +
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
                toProper(sb, false, isExpr);
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
                    String prompt = evalStrExpr(params[0], refs);
                    String choice1 = evalStrExpr(params[1], refs);
                    String choice2 = evalStrExpr(params[2], refs);
                    char c = msgBoxYesNo(this, prompt + "\n\nYes for " + choice1 +
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
                    if (isExpr) {
                        ret.append("\"").append(ret).append("\"");
                    }
                } else {
                    mView.displayError("Bad arguments to PopUpChoice " +
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
                    String prompt = evalStrExpr(params[0], refs);
                    String defMsg = "";
                    if (params.length == 2) {
                        defMsg = evalStrExpr(params[1], refs);
                    }
                    ret.append("\"")
                            .append(inputBox(this, prompt, "ADRIFT", defMsg))
                            .append("\"");
                } else {
                    mView.displayError("Expecting 1 or two " +
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
                ret.append(getLastFunc(funcName, args, refs));
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
                                mView.displayError("Bad 2nd Argument to " +
                                        "&perc;PropertyValue[]&perc; - " +
                                        "Property Key \"" + params[1] + "\" not found");
                            }
                        }
                        for (MCharacter ch : argChs.values()) {
                            if (ch.hasProperty(params[1])) {
                                output.add(ch.getPropertyValue(params[1]));
                            } else {
                                mView.displayError("Bad 2nd Argument to " +
                                        "&perc;PropertyValue[]&perc; - " +
                                        "Property Key \"" + params[1] + "\" not found");
                            }
                        }
                        for (MLocation loc : argLocs.values()) {
                            if (loc.hasProperty(params[1])) {
                                output.add(loc.getPropertyValue(params[1]));
                            } else {
                                mView.displayError("Bad 2nd Argument to " +
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
                            mView.displayError("Bad 1st Argument to " +
                                    "&perc;PropertyValue[]&perc; - " +
                                    "Object/Character Key \"" + params[0] + "\" not found");
                        }
                    }
                } else {
                    mView.displayError("Bad call to " +
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
                    mView.displayError("Bad Argument to " +
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
                    Collection<MObject> obs = ch.getWornObjects().values();
                    for (MObject ob : obs) {
                        if (ret.length() > 0) {
                            ret.append("|");
                        }
                        ret.append(ob.getKey());
                    }
                } else {
                    mView.displayError("Bad Argument to " +
                            "&perc;Worn[]&perc; - " +
                            "Character Key \"" + args + "\" not found");
                }
                break;
            }
        }

        // Return the evaluated result (informing user if blank
        // when it shouldn't be)
        if (!allowBlankRet && ret.length() == 0) {
            mView.displayError("Bad Function - Nothing output");
        }
        return ret;
    }

    private String getLastFunc(@NonNull String funcName,
                               @NonNull String args, @Nullable MReferenceList refs) {
        String newFunc = funcName.replace("prev", "");
        // Note the previous state:
        MGameState prevState = mStates.peek();
        // Save where we are now:
        mStates.recordState(this);
        // Load up the previous state:
        mStates.restoreState(this, prevState);
        String ret = evalFuncs("%" + newFunc + "[" + args + "]%", refs);
        // Get rid of the 'current' state and load it back as present:
        mStates.pop(this);
        return ret;
    }

    private void evalItemFunc(@NonNull StringBuilder text,
                              boolean isExpr, @Nullable MReferenceList refs) {
        // If this is in an expression, we need to replace
        // anything with a quoted value

        // Match anything unless it's between <# ... #> symbols
        Matcher m = RE_IGNORE.matcher(text);
        if (m.find()) {
            // We found a <# ... #>
            // Temporarily replace it with a UUID,
            // recursively call this function again, then
            // restore the <# ... #> back into the result.
            String match = m.group("embeddedexpression");
            String uuid = ":" + UUID.randomUUID() + ":";
            text.replace(m.start(), m.end(), uuid);
            evalItemFunc(text, isExpr, refs);
            int start = text.indexOf(uuid);
            text.replace(start, start + uuid.length(), match);
        } else {
            // OK, there are no <# ... #> matches. We can
            // now go through and evaluate any item functions.
            int start = 0;
            Matcher m2 = RE_ITEM_FUNC.matcher(text);
            while (start < text.length() && m2.find(start)) {
                // We found an item function
                String itemFunc = m2.group();
                boolean retIsInt[] = new boolean[1];
                String result =
                        evalItemFunc(itemFunc, null,
                                null, null, null, retIsInt);
                int prevStart = start;
                start = m2.start() + itemFunc.length();
                if (!result.equals("#*!~#")) {
                    if (result.contains(itemFunc)) {
                        result = result.replace(itemFunc, "*** RECURSIVE REPLACE ***");
                    }
                    if (isExpr &&
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
            evalFuncs(text, refs, false, false);
        }
    }

    @NonNull
    String evalStrRegex(@NonNull String text,
                        @Nullable MReferenceList refs) {
        Matcher m = RE_EXP.matcher(text);
        while (m.find()) {
            text = text.replace(m.group(),
                    evalStrExpr(m.group(1), refs));
        }
        return text;
    }

    public void evalStrRegex(@NonNull StringBuilder text,
                             @Nullable MReferenceList refs) {
        Matcher m = RE_EXP.matcher(text);
        while (m.find()) {
            text.replace(m.start(), m.end(),
                    evalStrExpr(m.group(1), refs));
            m.reset(text);
        }
    }

    @NonNull
    public String evalStrExpr(@NonNull String expr,
                              @Nullable MReferenceList refs) {
        if (expr.length() == 0) {
            return "";
        }
        MVariable var = new MVariable(this);
        var.setType(Text);
        var.setToExpr(expr, refs);
        return var.getStr();
    }

    int evalIntExpr(@NonNull String expr,
                    @Nullable MReferenceList refs) {
        MVariable var = new MVariable(this);
        var.setType(Numeric);
        var.setToExpr(expr, refs);
        return var.getInt();
    }

    private MGlobals.MPerspectiveEnum getPerspective(int offset) {
        // Return the highest perspective that is less the offset
        int highest = 0;
        MGlobals.MPerspectiveEnum pers = MGlobals.MPerspectiveEnum.None;
        for (MPronounInfo p : mPronounKeys) {
            if (offset >= p.mOffset && p.mOffset > highest) {
                pers = mCharacters.get(p.mKey).getPerspective();
                highest = p.mOffset;
            }
        }
        return (pers != MGlobals.MPerspectiveEnum.None) ?
                pers : getPlayer().getPerspective();
    }

    @NonNull
    public String evalItemProp(@NonNull String propKey,
                               @NonNull MPropertyHashMap itemProps,
                               @NonNull MPropertyHashMap globalProps,
                               @NonNull String rem,
                               @NonNull boolean[] retIsInt) {
        MItemFunctionEvaluator item = null;
        ArrayList<MItemWithProperties> itemList = null;
        MProperty p;

        if ((p = itemProps.get(propKey)) != null) {
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
                    retIsInt[0] = true;
                    return String.valueOf(p.getIntData());

                case SelectionOnly:
                    retIsInt[0] = true;
                    return "1";

                case Text:
                case StateList:
                    return p.getValue();
            }
        } else if ((p = globalProps.get(propKey)) != null) {
            // Item doesn't have the property. Give a default based on the
            // global property's type.
            switch (p.getType()) {
                case Integer:
                case ValueList:
                case SelectionOnly:
                    return "0";
            }
        }

        if (rem.length() > 0) {
            return evalItemFunc(rem, itemList,
                    null, null, item, retIsInt);
        } else if (item != null) {
            return ((MItem) item).getKey();
        } else if (itemList != null && itemList.size() > 0) {
            return evalItemFunc(rem, itemList,
                    null, null, null, retIsInt);
        }

        return "#*!~#";
    }

    @NonNull
    public String evalItemFunc(@NonNull String itemFunc,
                               @Nullable ArrayList<MItemWithProperties> lst,
                               @Nullable ArrayList<DirectionsEnum> lstDirs,
                               @Nullable String propKey,
                               @Nullable MItemFunctionEvaluator funcEvaluator,
                               @NonNull boolean[] retIsInt) {
        String args = "";
        String rem = "";

        int posDot = itemFunc.indexOf(".");
        int posLP = itemFunc.indexOf("(");

        if (posLP > -1 && itemFunc.contains(")")) {
            int posRP = itemFunc.indexOf(")", posLP);
            if (posDot > -1) {
                if (posDot < posLP) {
                    rem = itemFunc.substring(posDot + 1);
                    itemFunc = itemFunc.substring(0, posDot);
                } else {
                    args = itemFunc.substring(posLP + 1, posRP);
                    rem = itemFunc.substring(posRP + 2);
                    itemFunc = itemFunc.substring(0, posLP);
                }
            } else {
                args = itemFunc.substring(posLP + 1, itemFunc.lastIndexOf(")"));
                rem = itemFunc.substring(posRP + 1);
                itemFunc = itemFunc.substring(0, posLP);
            }
        } else if (posDot > 0) {
            rem = itemFunc.substring(posDot + 1);
            itemFunc = itemFunc.substring(0, posDot);
        }

        if (lst != null || (lstDirs != null &&
                (itemFunc.equals("List") || itemFunc.equals("Count") ||
                        itemFunc.equals("Name") || itemFunc.length() == 0))) {
            return evalItemList(lst, lstDirs,
                    itemFunc, args, rem, propKey, retIsInt);
        } else if (funcEvaluator != null) {
            return funcEvaluator.evaluate(itemFunc, args, rem, retIsInt);
        } else {
            return evalItemFunc(itemFunc, rem, retIsInt);
        }
    }

    private String evalItemList(@Nullable ArrayList<MItemWithProperties> lst,
                                @Nullable ArrayList<DirectionsEnum> lstDirs,
                                @NonNull String funcName,
                                @NonNull String args,
                                @NonNull String rem,
                                @Nullable String propKey,
                                @NonNull boolean[] retIsInt) {
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
                retIsInt[0] = true;
                return "0";

            case "Sum": {
                int sum = 0;
                if (lst != null && propKey != null) {
                    for (MItemWithProperties item : lst) {
                        if (item.hasProperty(propKey)) {
                            sum += item.getProperties().get(propKey).getIntData();
                        }
                    }
                }
                retIsInt[0] = true;
                return String.valueOf(sum);
            }

            case "Description": {
                ret = new StringBuilder();
                for (MItem item : lst) {
                    appendDoubleSpace(ret);
                    if (item instanceof MObject) {
                        ret.append(((MObject) item).getDescription().toString());
                    } else if (item instanceof MCharacter) {
                        ret.append(((MCharacter) item).getDescription().toString());
                    } else if (item instanceof MLocation) {
                        if (((MLocation) item).getViewLocation().equals("")) {
                            ret.append("There is nothing of interest here.");
                        } else {
                            ret.append(((MLocation) item).getViewLocation());
                        }
                    }
                }
                return ret.toString();
            }

            case "List":
            case "Name":
                // List(and) - And separated list - Default
                // List(or) - Or separated list
                String delim = " and ";
                String argsLower = args.toLowerCase();
                if (argsLower.contains("or")) {
                    delim = " or ";
                }
                if (argsLower.contains("rows")) {
                    delim = "\n";
                }

                // List(definite/the) - List the objects names - Default
                // List(indefinite) - List a/an object
                MGlobals.ArticleTypeEnum article = Definite;
                if (argsLower.contains("indefinite")) {
                    article = Indefinite;
                }
                if (argsLower.contains("none")) {
                    article = None;
                }

                // List(true) - List anything in/on everything in the list (single level) - Default
                // List(false) - Do not list anything in/on
                boolean listInOn = true; // List any objects in or on anything in this list
                if (funcName.equals("Name") ||
                        argsLower.contains("false") || argsLower.contains("0")) {
                    listInOn = false;
                }

                boolean forcePronoun = false;
                MPronounEnum pronoun = Subjective;
                if (argsLower.contains("none")) {
                    pronoun = MPronounEnum.None;
                }
                if (argsLower.contains("force")) {
                    forcePronoun = true;
                }
                if (argsLower.contains("objective") ||
                        argsLower.contains("object") || argsLower.contains("target")) {
                    pronoun = Objective;
                }
                if (argsLower.contains("possessive") || argsLower.contains("possess")) {
                    pronoun = Possessive;
                }
                if (argsLower.contains("reflective") || argsLower.contains("reflect")) {
                    pronoun = Reflective;
                }

                ret = new StringBuilder();
                int i = 0;
                if (lst != null) {
                    for (MItem item : lst) {
                        i++;
                        if (delim.equals("\n")) {
                            if (i > 1) {
                                ret.append(delim);
                            }
                        } else {
                            if (i > 1 && i < lst.size()) {
                                ret.append(", ");
                            }
                            if (lst.size() > 1 && i == lst.size()) {
                                ret.append(delim);
                            }
                        }
                        if (item instanceof MObject) {
                            ret.append(((MObject) item).getFullName(article));
                            if (listInOn &&
                                    ((MObject) item).getChildObjects(InsideOrOnObject).size() > 0) {
                                ret.append(".  ").append(((MObject) item).displayChildObjects());
                                chopLast(ret);
                            }
                        } else if (item instanceof MCharacter) {
                            // List(definite/the) - List the objects names - Default
                            // List(indefinite) - List a/an object
                            article = Indefinite; // opposite default from objects
                            if (argsLower.contains("definite")) {
                                article = Definite;
                            }
                            ret.append(((MCharacter) item).getName(pronoun,
                                    true, true, article, forcePronoun));
                        } else if (item instanceof MLocation) {
                            ret.append(((MLocation) item).getShortDescription().toString());
                        }
                    }
                } else if (lstDirs != null) {
                    for (DirectionsEnum dir : lstDirs) {
                        i++;
                        if (i > 1 && i < lstDirs.size()) {
                            ret.append(", ");
                        }
                        if (lstDirs.size() > 1 && i == lstDirs.size()) {
                            ret.append(delim);
                        }
                        ret.append(getDirectionName(dir).toLowerCase());
                    }
                }
                if (ret.length() == 0) {
                    return "nothing";
                }
                return ret.toString();

            case "Parent":
                lstNew = new ArrayList<>();
                lstKeys = new ArrayList<>();
                for (MItemWithProperties item : lst) {
                    String parent = item.getParent();
                    if (parent.length() > 0) {
                        if (!lstKeys.contains(parent)) {
                            MItemWithProperties newItem =
                                    (MItemWithProperties) mAllItems.get(parent);
                            lstKeys.add(parent);
                            lstNew.add(newItem);
                        }
                    }
                }

                if (rem.length() > 0) {
                    return evalItemFunc(rem, lstNew, null,
                            null, null, retIsInt);
                }
                break;

            case "Children":
                lstNew = new ArrayList<>();
                for (MItemWithProperties item : lst) {
                    if (item instanceof MObject) {
                        MObject ob = (MObject) item;
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
                    }
                }

                if (rem.length() > 0 || lstNew.size() > 0) {
                    return evalItemFunc(rem, lstNew,
                            null, null, null, retIsInt);
                }
                break;

            case "Contents":
                lstNew = new ArrayList<>();
                for (MItemWithProperties item : lst) {
                    if (item instanceof MObject) {
                        MObject ob = (MObject) item;
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
                return evalItemFunc(rem, lstNew, null,
                        null, null, retIsInt);

            case "Objects":
                lstNew = new ArrayList<>();
                for (MItemWithProperties item : lst) {
                    if (item instanceof MLocation) {
                        MLocation loc = (MLocation) item;
                        lstNew.addAll(loc.getObjectsInLocation().values());
                    } else {
                        GLKLogger.error("TODO: Objects - MGlobals");
                    }
                }
                return evalItemFunc(rem, lstNew, null,
                        null, null, retIsInt);

            default:
                if (mAllProperties.containsKey(funcName)) {
                    lstNew = new ArrayList<>();
                    lstKeys = new ArrayList<>();
                    ret = new StringBuilder();
                    int sum = 0;
                    boolean isIntResult = false;
                    boolean propFound = false;
                    String newPropKey = null;

                    for (MItemWithProperties item : lst) {
                        if (item.hasProperty(funcName)) {
                            propFound = true;
                            MProperty p = item.getProperty(funcName);
                            boolean evalsToTrue = true;

                            if (args.length() > 0) {
                                evalsToTrue = false;
                                if (args.contains(".")) {
                                    args = evalFuncs(args, mReferences);
                                }
                                if (args.contains("+") || args.contains("-")) {
                                    String argsNew = evalStrExpr(args, mReferences);
                                    if (argsNew != null) {
                                        args = argsNew;
                                    }
                                }
                                switch (p.getType()) {
                                    case ValueList: {
                                        int checkVal = 0;
                                        if (isNumeric(args)) {
                                            checkVal = safeInt(args);
                                        } else {
                                            if (p.mValueList.containsKey(args)) {
                                                checkVal = p.mValueList.get(args);
                                            }
                                        }
                                        evalsToTrue = (p.getIntData() == checkVal);
                                        retIsInt[0] = true;
                                        break;
                                    }
                                    case Integer: {
                                        int checkVal = 0;
                                        if (isNumeric(args)) {
                                            checkVal = safeInt(args);
                                        }
                                        evalsToTrue = (p.getIntData() == checkVal);
                                        retIsInt[0] = true;
                                        break;
                                    }
                                    case StateList:
                                        evalsToTrue = p.getValue().toLowerCase()
                                                .equals(args.toLowerCase());
                                        break;
                                    case SelectionOnly:
                                        switch (args.toLowerCase()) {
                                            case "false":
                                            case "0":
                                                evalsToTrue = false;
                                                break;
                                            default:
                                                evalsToTrue = true;
                                                break;
                                        }
                                        retIsInt[0] = true;
                                        break;
                                    default:
                                        GLKLogger.error("TODO: Property filter check " +
                                                "not yet implemented for " +
                                                p.getType().toString());
                                        break;
                                }
                            }

                            if (evalsToTrue) {
                                switch (p.getType()) {
                                    case CharacterKey:
                                        MCharacter chP = mCharacters.get(p.getValue());
                                        if (!lstKeys.contains(chP.getKey())) {
                                            lstKeys.add(chP.getKey());
                                            lstNew.add(chP);
                                        }
                                        break;
                                    case LocationGroupKey: {
                                        for (String item2 :
                                                mGroups.get(p.getValue()).getArlMembers()) {
                                            if (!lstKeys.contains(item2)) {
                                                MItemWithProperties newItem =
                                                        (MItemWithProperties) mAllItems.get(item2);
                                                lstKeys.add(newItem.getKey());
                                                lstNew.add(newItem);
                                            }
                                        }
                                        break;
                                    }
                                    case LocationKey: {
                                        MItemWithProperties newLoc = mLocations.get(p.getValue());
                                        if (!lstKeys.contains(newLoc.getKey())) {
                                            lstKeys.add(newLoc.getKey());
                                            lstNew.add(newLoc);
                                        }
                                        break;
                                    }
                                    case ObjectKey: {
                                        MItemWithProperties newOb = mObjects.get(p.getValue());
                                        if (!lstKeys.contains(newOb.getKey())) {
                                            lstKeys.add(newOb.getKey());
                                            lstNew.add(newOb);
                                        }
                                        break;
                                    }
                                    case Integer:
                                    case ValueList:
                                    case StateList:
                                        lstNew.add(item);
                                        newPropKey = funcName;
                                        retIsInt[0] = false;
                                        break;
                                    case SelectionOnly:
                                        // Selection Only property to further reduce list
                                        lstNew.add(item);
                                        retIsInt[0] = true;
                                        break;
                                    case Text:
                                        if (ret.length() > 0) {
                                            if (item == lst.get(lst.size() - 1)) {
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
                            MProperty p = mAllProperties.get(funcName);
                            if (p != null) {
                                boolean isValOK = false; // Because this is equiv of arg = (true)
                                if (args.length() > 0) {
                                    isValOK = false;
                                    if (args.contains(".")) {
                                        args = evalFuncs(args, mReferences);
                                    }
                                    if (args.contains("+") || args.contains("-")) {
                                        String argsNew = evalStrExpr(args, mReferences);
                                        if (argsNew != null) {
                                            args = argsNew;
                                        }
                                    }
                                    switch (p.getType()) {
                                        // Opposite of above, since this
                                        // item _doesn't_ contain this property
                                        case SelectionOnly:
                                            switch (args.toLowerCase()) {
                                                case "false":
                                                case "0":
                                                    isValOK = true;
                                                    break;
                                                default:
                                                    isValOK = false;
                                                    break;
                                            }
                                            retIsInt[0] = true;
                                            break;
                                        case StateList:
                                            isValOK = false; // Since we don't have the property
                                            break;
                                        default:
                                            GLKLogger.error("TODO: Property filter check not " +
                                                    "yet implemented for " + p.getType().toString());
                                            break;
                                    }
                                }
                                if (isValOK) {
                                    switch (p.getType()) {
                                        case SelectionOnly:
                                            // Selection Only property to further reduce list
                                            lstNew.add(item);
                                            break;
                                    }
                                }
                            }
                        }
                    }

                    if (!propFound) {
                        switch (mAllProperties.get(funcName).getType()) {
                            case Integer:
                            case ValueList:
                                isIntResult = true;
                                retIsInt[0] = true;
                                break;
                        }
                    }

                    if (rem.length() > 0 || (lstNew.size() > 0 && !isIntResult)) {
                        return evalItemFunc(rem, lstNew,
                                null, newPropKey, null, retIsInt);
                    } else if (isIntResult) {
                        return String.valueOf(sum);
                    } else {
                        return ret.toString();
                    }
                }
        }

        return "#*!~#";
    }

    @NonNull
    private String evalItemFunc(@NonNull String itemKey, @NonNull String rem,
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
            return evalItemFunc(rem, itemList, null,
                    null, null, resultIsInt);
        } else {
            if (getAllKeys().contains(itemKey)) {
                MItemFunctionEvaluator item;
                if ((item = mObjects.get(itemKey)) == null) {
                    if ((item = mCharacters.get(itemKey)) == null) {
                        if ((item = mLocations.get(itemKey)) == null) {
                            if ((item = mEvents.get(itemKey)) == null) {
                                MGroup grp;
                                if ((grp = mGroups.get(itemKey)) != null) {
                                    itemList = new ArrayList<>();
                                    for (String itemKey2 : grp.getArlMembers()) {
                                        MItemWithProperties itm =
                                                (MItemWithProperties) mAllItems.get(itemKey2);
                                        itemList.add(itm);
                                    }
                                }
                            }
                        }
                    }
                }
                return evalItemFunc(rem, itemList, null,
                        null, item, resultIsInt);
            } else {
                for (DirectionsEnum d : DirectionsEnum.values()) {
                    if (String.valueOf(d).equals(itemKey)) {
                        ArrayList<DirectionsEnum> dirs = new ArrayList<>();
                        dirs.add(d);
                        return evalItemFunc(rem, null, dirs,
                                null, null, resultIsInt);
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
        String dirName = mDirectionNames.get(dir);
        if (dirName.contains("/")) {
            dirName = dirName.substring(0, dirName.indexOf("/"));
        }
        return dirName;
    }

    @NonNull
    String getDirectionRE(DirectionsEnum dir,
                          boolean addBrackets, boolean isRealRE) {
        String dirRE = mDirectionNames.get(dir).toLowerCase();
        if (isRealRE) {
            dirRE = dirRE.replace("/", "|");
        }
        if (addBrackets) {
            return (isRealRE) ?
                    "(" + dirRE + ")" :
                    "[" + dirRE + "]";
        } else {
            return dirRE;
        }
    }

    boolean keyExists(@NonNull String itemKey) {
        return mLocations.containsKey(itemKey) || mObjects.containsKey(itemKey) ||
                mTasks.containsKey(itemKey) || mEvents.containsKey(itemKey) ||
                mCharacters.containsKey(itemKey) || mGroups.containsKey(itemKey) ||
                mVariables.containsKey(itemKey) || mALRs.containsKey(itemKey) ||
                mHints.containsKey(itemKey) || mAllProperties.containsKey(itemKey);
    }

    /**
     * Try to find a statelist property that contains
     * exactly the same states as those provided (possibly
     * in a different order).
     *
     * @param states - the states to find.
     * @return the key of the property if one was found,
     * otherwise NULL.
     */
    @Nullable
    String findProp(@NonNull MStringArrayList states) {
        nextProp:
        for (MProperty prop : mAllProperties.values()) {
            if (prop.mStates.size() == states.size()) {
                for (String state : states) {
                    if (!prop.mStates.contains(state)) {
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
                                 boolean noDebugMsg) {
        int nReplaced = 0;

        for (MItem item : mAllItems.values()) {
            boolean lookInItem = false;
            switch (mSearchOptions.mSearchInWhat) {
                case AllItems:
                    lookInItem = true;
                    break;
                case NonLibraryItems:
                    lookInItem = !item.getIsLibrary();
                    break;
            }
            if (lookInItem) {
                nReplaced += item.find(find, replace);
            }
        }

        if (!noDebugMsg) {
            if (nReplaced == 0) {
                GLKLogger.debug("The following specified text was not found: " + find);
            } else {
                GLKLogger.debug(nReplaced + " occurrence(s) replaced.");
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

    private String getCoverFilename() {
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
                mPlayer = mCharacters.values().iterator().next();
                return mPlayer;
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
                    String tag = s.substring(j + 5, k - j - 5);
                    if (tag.startsWith("\"")) {
                        tag = tag.substring(1);
                    }
                    if (tag.endsWith("\"")) {
                        tag = tag.substring(0, tag.length() - 1);
                    }
                    if (!lImages.contains(tag) &&
                            (new File(tag).exists() || tag.startsWith("http"))) {
                        lImages.add(tag);
                    }
                }
                i = s.indexOf("<img ", i + 1);
            }
        }

        return lImages;
    }

    public ArrayList<MSingleDescription> getAllDescriptions() {
        ArrayList<MSingleDescription> ret = new ArrayList<>();
        ret.addAll(getIntroduction());
        ret.addAll(getEndGameText());
        for (MItem itm : mAllItems.values()) {
            for (MDescription d : itm.getAllDescriptions()) {
                ret.addAll(d);
            }
        }
        return ret;
    }

    public int getScore() {
        MVariable var = mVariables.get("Score");
        if (var != null) {
            mScore = var.getInt();
        }
        return mScore;
    }

    public void setScore(int value) {
        if (value != mScore) {
            MVariable var = mVariables.get("Score");
            if (var != null) {
                var.set(value);
            }
        }
        mScore = value;
        mView.updateStatusBar(this);
    }

    public int getMaxScore() {
        MVariable var = mVariables.get("MaxScore");
        if (var != null) {
            mMaxScore = var.getInt();
        }
        return mMaxScore;
    }

    public void setMaxScore(int value) {
        if (value != mMaxScore) {
            MVariable var = mVariables.get("MaxScore");
            if (var != null) {
                var.set(value);
            }
        }
        mMaxScore = value;
    }

    @Nullable
    MItem getItemFromKey(@Nullable String itemKey) {
        if (itemKey == null || itemKey.equals("")) {
            return null;
        }
        MItem ret;
        if ((ret = mLocations.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mObjects.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mTasks.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mEvents.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mCharacters.get(itemKey)) != null || itemKey.equals(THEPLAYER)) {
            return ret;
        }
        if ((ret = mGroups.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mVariables.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mALRs.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mHints.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mAllProperties.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mUDFs.get(itemKey)) != null) {
            return ret;
        }
        if ((ret = mSynonyms.get(itemKey)) != null) {
            return ret;
        }
        return null;
    }

    @NonNull
    String getTypeFromKeyNice(@Nullable String itemKey) {
        if (itemKey == null || itemKey.equals("")) {
            return "";
        }
        if (mLocations.containsKey(itemKey)) {
            return "Location";
        }
        if (mObjects.containsKey(itemKey)) {
            return "Object";
        }
        if (mTasks.containsKey(itemKey)) {
            return "Task";
        }
        if (mEvents.containsKey(itemKey)) {
            return "Event";
        }
        if (mCharacters.containsKey(itemKey)) {
            return "Character";
        }
        if (mGroups.containsKey(itemKey)) {
            return "Group";
        }
        if (mVariables.containsKey(itemKey)) {
            return "Variable";
        }
        if (mALRs.containsKey(itemKey)) {
            return "Text Override";
        }
        if (mHints.containsKey(itemKey)) {
            return "Hint";
        }
        if (mAllProperties.containsKey(itemKey)) {
            return "Property";
        }
        if (mUDFs.containsKey(itemKey)) {
            return "User Function";
        }
        if (mSynonyms.containsKey(itemKey)) {
            return "Synonym";
        }
        return "";
    }

    @Nullable
    public String getNameFromKey(@Nullable String itemKey) {
        return getNameFromKey(itemKey, true, true, false);
    }

    @Nullable
    public String getNameFromKey(@Nullable String itemKey, boolean isQuoted) {
        return getNameFromKey(itemKey, isQuoted, true, false);
    }

    @Nullable
    public String getNameFromKey(@Nullable String itemKey, boolean isQuoted,
                                 boolean prefixItem) {
        return getNameFromKey(itemKey, isQuoted, prefixItem, false);
    }

    @Nullable
    public String getNameFromKey(@Nullable String itemKey, boolean isQuoted,
                                 boolean prefixItem, boolean pcase) {
        String sQ = "";
        String sO = "";
        String sC = "";

        if (itemKey == null) {
            mView.errMsg("Bad Key");
            return null;
        }

        if (isQuoted) {
            sQ = "'";
        } else {
            sO = "[ ";
            sC = " ]";
        }

        int len = Math.min(16, itemKey.length());
        if (itemKey.startsWith("Referenced")) {
            switch (itemKey.substring(0, len)) {
                case "ReferencedCharac":
                    switch (itemKey) {
                        case "ReferencedCharacters":
                            return sO + itemKey.replace("ReferencedCharacters",
                                    "Referenced Characters") + sC;
                        case "ReferencedCharacter":
                            return sO + itemKey.replace("ReferencedCharacter",
                                    "Referenced Character") + sC;
                        default:
                            return sO + itemKey.replace("ReferencedCharacter",
                                    "Referenced Character ") + sC;
                    }
                case "ReferencedDirect":
                    switch (itemKey) {
                        case "ReferencedDirections":
                            return sO + itemKey.replace("ReferencedDirections",
                                    "Referenced Directions") + sC;
                        case "ReferencedDirection":
                            return sO + itemKey.replace("ReferencedDirection",
                                    "Referenced Direction") + sC;
                        default:
                            return sO + itemKey.replace("ReferencedDirection",
                                    "Referenced Direction ") + sC;
                    }
                case "ReferencedObject":
                    switch (itemKey) {
                        case "ReferencedObjects":
                            return sO + itemKey.replace("ReferencedObjects",
                                    "Referenced Objects") + sC;
                        case "ReferencedObject":
                            return sO + itemKey.replace("ReferencedObject",
                                    "Referenced Object") + sC;
                        default:
                            return sO + itemKey.replace("ReferencedObject",
                                    "Referenced Object ") + sC;
                    }
                case "ReferencedNumber":
                    switch (itemKey) {
                        case "ReferencedNumbers":
                            return sO + itemKey.replace("ReferencedNumbers",
                                    "Referenced Numbers") + sC;
                        case "ReferencedNumber":
                            return sO + itemKey.replace("ReferencedNumber",
                                    "Referenced Number") + sC;
                        default:
                            return sO + itemKey.replace("ReferencedNumber",
                                    "Referenced Number ") + sC;
                    }
                case "ReferencedText":
                    switch (itemKey) {
                        case "ReferencedText":
                            return sO + itemKey.replace("ReferencedText",
                                    "Referenced Text") + sC;
                        default:
                            return sO + itemKey.replace("ReferencedText",
                                    "Referenced Text ") + sC;
                    }
                case "ReferencedLocati":
                    switch (itemKey) {
                        case "ReferencedLocation":
                            return sO + itemKey.replace("ReferencedLocation",
                                    "Referenced Location") + sC;
                    }
                    break;
                case "ReferencedItem":
                    return sO + itemKey.replace("ReferencedItem",
                            "Referenced Item") + sC;
            }
        } else if (itemKey.startsWith("Parameter-")) {
            return sO + itemKey.replace("Parameter-", "") + sC;
        }

        if (itemKey.equals(ANYOBJECT)) {
            return sO + "Any Object" + sC;
        }
        if (itemKey.equals(ANYCHARACTER)) {
            return sO + "Any Character" + sC;
        }
        if (itemKey.equals(NOOBJECT)) {
            return sO + "No Object" + sC;
        }
        if (itemKey.equals(THEFLOOR)) {
            return sO + (pcase ? "The Floor" : "the Floor") + sC;
        }
        if (itemKey.equals(THEPLAYER)) {
            return (pcase ? sO + "The Player Character" + sC : "the Player character");
        }
        if (itemKey.equals(CHARACTERPROPERNAME)) {
            return (prefixItem ? (pcase ? "Property " : "property ") : "") + sQ + "Name" + sQ;
        }
        if (itemKey.equals(PLAYERLOCATION)) {
            return (pcase ? sO + "The Player's Location" + sC : "the Player's location");
        }

        MLocation loc = mLocations.get(itemKey);
        if (loc != null) {
            return (prefixItem ? (pcase ? "Location " : "location ") : "") + sQ +
                    loc.getShortDescription().toString() + sQ;
        }
        MObject ob = mObjects.get(itemKey);
        if (ob != null) {
            return (prefixItem ? (pcase ? "Object " : "object ") : "") + sQ +
                    ob.getFullName() + sQ;
        }
        MTask tas = mTasks.get(itemKey);
        if (tas != null) {
            return (prefixItem ? (pcase ? "Task " : "task ") : "") + sQ +
                    tas.getDescription() + sQ;
        }
        MEvent ev = mEvents.get(itemKey);
        if (ev != null) {
            return (prefixItem ? (pcase ? "Event " : "event ") : "") + sQ +
                    ev.getDescription() + sQ;
        }
        MCharacter ch = mCharacters.get(itemKey);
        if (ch != null) {
            return (prefixItem ? (pcase ? "Character  " : "character ") : "") + sQ +
                    ch.getName() + sQ;
        }
        MGroup grp = mGroups.get(itemKey);
        if (grp != null) {
            return (prefixItem ? (pcase ? "Group " : "group ") : "") + sQ +
                    grp.getName() + sQ;
        }
        MVariable var = mVariables.get(itemKey);
        if (var != null) {
            return (prefixItem ? (pcase ? "Variable " : "variable ") : "") + sQ +
                    var.getName() + sQ;
        }
        MALR alr = mALRs.get(itemKey);
        if (alr != null) {
            return (prefixItem ? (pcase ? "Text Override " : "text override ") : "") + sQ +
                    alr.getOldText() + sQ;
        }
        MHint hint = mHints.get(itemKey);
        if (hint != null) {
            return (prefixItem ? (pcase ? "Hint " : "hint ") : "") + sQ +
                    hint.getQuestion() + sQ;
        }
        MProperty prop = mAllProperties.get(itemKey);
        if (prop != null) {
            return (prefixItem ? (pcase ? "Property " : "property ") : "") + sQ +
                    prop.getDescription() + sQ;
        }
        MUserFunction udf = mUDFs.get(itemKey);
        if (udf != null) {
            return (prefixItem ? (pcase ? "User Function " : "user function ") : "") + sQ +
                    udf.mName + sQ;
        }
        MSynonym syn = mSynonyms.get(itemKey);
        if (syn != null) {
            return (prefixItem ? (pcase ? "Synonym " : "synonym ") : "") + sQ +
                    syn.getCommonName() + sQ;
        }
        return itemKey;
    }

    @NonNull
    public MTaskHashMap getTaskList(MTasksListEnum taskListType) {
        switch (taskListType) {
            case AllTasks:
                if (mAllTasks == null) {
                    mAllTasks = mTasks;
                }
                return mAllTasks;

            case GeneralTasks:
                if (mGeneralTasks == null) {
                    mGeneralTasks = new MTaskHashMap(this);
                    for (MTask task : mTasks.values()) {
                        if (task.mType == General) {
                            mGeneralTasks.put(task.getKey(), task);
                        }
                    }
                }
                return mGeneralTasks;

            case GeneralAndOverrideableSpecificTasks:
                if (mGeneralAndOverrideableSpecificTasks == null) {
                    mGeneralAndOverrideableSpecificTasks = new MTaskHashMap(this);
                    for (MTask task : mTasks.values()) {
                        if (task.mType == General) {
                            mGeneralAndOverrideableSpecificTasks.put(task.getKey(), task);
                        }
                        // A specific task is overrideable if any of the specifics are unspecified
                        if (task.mType == Specific) {
                            if (task.mSpecifics != null) {
                                for (MTask.MSpecific s : task.mSpecifics) {
                                    if (s.mKeys.size() == 1) {
                                        if (s.mKeys.get(0).equals("")) {
                                            mGeneralAndOverrideableSpecificTasks.put(task.getKey(), task);
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
                    for (MTask task : mTasks.values()) {
                        if (task.mType == Specific) {
                            mSpecificTasks.put(task.getKey(), task);
                        }
                    }
                }
                return mSpecificTasks;

            case SystemTasks:
                if (mSystemTasks == null) {
                    mSystemTasks = new MTaskHashMap(this);
                    for (MTask task : mTasks.values()) {
                        if (task.mType == MTask.TaskTypeEnum.System) {
                            mSystemTasks.put(task.getKey(), task);
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

    private String getDefaultFontName() {
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

    private int getWaitTurns() {
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
        ArrayList<String> ret = new ArrayList<>();
        ret.addAll(mALRs.keySet());
        ret.addAll(mCharacters.keySet());
        ret.addAll(mEvents.keySet());
        ret.addAll(mGroups.keySet());
        ret.addAll(mHints.keySet());
        ret.addAll(mLocations.keySet());
        ret.addAll(mObjects.keySet());
        ret.addAll(mAllProperties.keySet());
        ret.addAll(mTasks.keySet());
        ret.addAll(mVariables.keySet());
        ret.addAll(mUDFs.keySet());
        return ret;
    }

    private void reset() {
        setTitle("Untitled");
        setAuthor("Anonymous");
        setFilename("untitled.taf");
        mDefaultFont = null;

        for (EnabledOptionEnum e : EnabledOptionEnum.values()) {
            setEnabled(e, true);
        }
        setWaitTurns(0);
        mAllItems.clear();
        mLocations.clear();
        mObjects.clear();
        mTasks.clear();
        mEvents.clear();
        mCharacters.clear();
        mGroups.clear();
        mVariables.clear();
        mALRs.clear();
        mHints.clear();
        mUDFs.clear();
        mAllProperties.clear();
        mObjectProperties.clear();
        mCharacterProperties.clear();
        mLocationProperties.clear();
        mSynonyms.clear();
        setIntroduction(new MDescription(this));
        setEndGameText(new MDescription(this));

        mMap = new MMap();

        mDirectionNames.clear();
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

        mCharsMentionedThisTurn.clear();
        for (MCharacter.Gender g : MCharacter.Gender.values()) {
            mCharsMentionedThisTurn.put(g, new ArrayList<String>());
        }

        mTurns = 0;
        mStates.clear();
        mCommands.clear();
        mCompletableTasks.clear();
        mRandomValues.clear();
        mPassResponses.clear();
        mFailResponses.clear();
        mConversationCharKey = "";
        mConversationNode = "";

        // TODO: ensure we clear all the other bits
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

    public enum ItScope {
        Applicable,     // 0
        Visible,        // 1
        Seen            // 2
    }

    static class CommmandUpdater {
        /**
         * Converts a lazy Advanced Command to strict format,
         * removing possibility for double spaces.
         *
         * @param cmd - the command to convert
         * @return the converted command.
         */
        @NonNull
        static String correctCommand(@NonNull String cmd, @NonNull MAdventure adv) {
            String newCmd = processBlock(cmd);
            if (!newCmd.equals(cmd)) {
                adv.mView.debugPrint(MGlobals.ItemEnum.General, "", High,
                        "Converted \"" + cmd + "\" to \"" + newCmd + "\"");
            }
            return newCmd;
        }

        @NonNull
        private static String processBlock(@NonNull String block) {
            // A block should be the complete entry between two
            // brackets, or between a bracket and a slash
            StringBuilder after = new StringBuilder(block);
            String nextBlock;
            String before = "";

            do {
                nextBlock = getSubBlock(after);
                if (!nextBlock.equals("")) {
                    if (nextBlock.startsWith("{")) {
                        // */ "] {#} {" => "]{ #}{"
                        // "] {#} [" => ?
                        // "} {#} [" => ?
                        // "} {#} {" => ? /*
                        // "{#} " => "{# }" if block starts with open bracket
                        // " {#} " => " {# }"
                        // "}{#} " => "}{# }"        -- should this be " }{#} " => " }{# }" ?
                        boolean containsMandatory = containsMandatoryText(after);
                        if (containsMandatory && after.toString().startsWith(" ")) {
                            // If before final [] block then _{x}_ => _{x_}
                            if (before.equals("") || before.endsWith(" ") || before.endsWith("}")) {
                                if (nextBlock.contains("/")) {
                                    nextBlock = "{[" + left(nextBlock.substring(1), nextBlock.length() - 2) + "] }";
                                } else {
                                    nextBlock = left(nextBlock, nextBlock.length() - 1) + " }";
                                }
                                after = new StringBuilder(right(after.toString(), after.length() - 1));
                            }
                        } else if (!containsMandatory && before.endsWith(" ")) {
                            // If after final [] block then _{x}_ => {_x}_
                            if (nextBlock.contains("/")) {
                                nextBlock = "{ [" + left(nextBlock.substring(1), nextBlock.length() - 2) + "]}";
                            } else {
                                nextBlock = "{ " + left(nextBlock.substring(1), nextBlock.length() - 1);
                            }
                            before = left(before, before.length() - 1);
                        }

                        // End block
                        // " {#}" => "{ #}" or "{ [#/#]}
                        if (before.endsWith(" ") && after.length() == 0) {
                            if (nextBlock.contains("/")) {
                                nextBlock = "{ [" + left(nextBlock.substring(1), nextBlock.length() - 2) + "]}";
                            } else {
                                nextBlock = "{ " + left(nextBlock.substring(1), nextBlock.length() - 1);
                            }
                            before = left(before, before.length() - 1);
                        }
                        before += "{" + processBlock(mid(nextBlock, 2, nextBlock.length() - 2)) + "}";
                    } else if (nextBlock.startsWith("[")) {
                        before += "[" + processBlock(mid(nextBlock, 2, nextBlock.length() - 2)) + "]";
                    } else if (nextBlock.endsWith("/")) {
                        before += processBlock(left(nextBlock, nextBlock.length() - 1)) + "/";
                    } else {
                        before += nextBlock;
                    }
                }
            } while (after.length() != 0);

            return before;
        }

        private static boolean containsMandatoryText(@NonNull final StringBuilder block) {
            // Returns True if any part of the block is mandatory
            // (either inside [] or not in brackets at all)
            if (block.length() == 0) {
                return false;
            }

            int level = 0;
            for (int i = 0; i < block.length(); i++) {
                switch (block.charAt(i)) {
                    case ' ':
                        // Ignore
                        break;
                    case '{':
                        level++;
                        break;
                    case '}':
                        level--;
                        break;
                    default:
                        if (level == 0) {
                            return true;
                        }
                        break;
                }
            }

            return false;
        }

        @NonNull
        private static String getSubBlock(@NonNull StringBuilder block) {
            // E.g. from "get {the} ball" > "get ", from "{the} ball" > "the", " ball" > ""
            int depth = 0;
            StringBuilder newBlock = new StringBuilder();

            for (int i = 0; i < block.length(); i++) {
                newBlock.append(block.charAt(i));
                switch (block.charAt(i)) {
                    case '{':
                    case '[':
                        if (depth == 0 &&
                                !newBlock.toString().equals(String.valueOf(block.charAt(i)))) {
                            StringBuilder tmp = new StringBuilder(right(block.toString(),
                                    block.length() - newBlock.length() + 1));
                            block.setLength(0);
                            block.append(tmp);
                            return MGlobals.left(newBlock.toString(), i);
                        }
                        depth++;
                        break;
                    case ']':
                    case '}':
                        depth--;
                        if (depth == 0) {
                            StringBuilder tmp = new StringBuilder(right(block.toString(),
                                    block.length() - newBlock.length()));
                            block.setLength(0);
                            block.append(tmp);
                            return newBlock.toString();
                        }
                        break;
                    case '/':
                        if (depth == 0) {
                            StringBuilder tmp = new StringBuilder(right(block.toString(),
                                    block.length() - newBlock.length()));
                            block.setLength(0);
                            block.append(tmp);
                            return newBlock.toString();
                        }
                        break;
                }
            }

            block.setLength(0);
            return newBlock.toString();
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

    static class MSearchOptions {
        SearchInWhatEnum mSearchInWhat = NonLibraryItems;

        public enum SearchInWhatEnum {
            Uninitialised,  // = -1
            AllItems,       // = 0
            NonLibraryItems // = 1
        }
    }

    public static class MPronounInfo {
        public String mKey = ""; // What is the pronoun applying to?
        int mOffset; // Where in the command does this substitution take place
        MPronounEnum mPronoun = Subjective;
        MCharacter.Gender mGender = Male;
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

    static class MSystemTask {
        static boolean executeSystemTask(@NonNull MAdventure adv,
                                         @NonNull String input,
                                         boolean isEarly) throws InterruptedException {
            // We take a 1 size array of Adventure objects so that we
            // can change it if restart is called
            switch (input) {
                case "hint":
                case "hints":
                    if (isEarly) {
                        return false;
                    } else {
                        hint(adv);
                    }
                    break;
                case "pronouns":
                    if (isEarly) {
                        return false;
                    } else {
                        String msg = "At the moment, <q>it</q> means " +
                                adv.mIt + ", <q>him</q> means " + adv.mHim +
                                ", <q>her</q> means " + adv.mHer + " and " +
                                "<q>them</q> means " + adv.mThem + ".";
                        adv.mView.displayText(adv, msg, true);
                        adv.mView.mOutputText.setLength(0);
                    }
                    break;
                case "restart":
                    restart(adv);
                    if (!isEarly) {
                        adv.mView.mOutputText.setLength(0);
                        adv.mView.mOutputText.append("***SYSTEM***");
                    }
                    break;
                case "restore":
                    restore(adv);
                    adv.mView.displayText(adv, "\n\n", true);
                    if (!isEarly) {
                        adv.mView.mOutputText.setLength(0);
                        adv.mView.mOutputText.append("***SYSTEM***");
                    }
                    break;
                case "save":
                    if (isEarly) {
                        return false;
                    } else {
                        save(adv);
                    }
                    break;
                case "save as":
                case "saveas":
                    if (isEarly) {
                        return false;
                    } else {
                        save(adv);
                    }
                    break;
                case "quit":
                    quit(adv, !isEarly);
                    break;
                case "undo":
                    undo(adv);
                    if (isEarly) {
                        adv.mView.displayText(adv, "\n\n", true);
                    }
                    break;
                case "wait":
                case "z":
                    if (isEarly) {
                        return false;
                    } else {
                        adv.mView.mOutputText.setLength(0);
                        StringBuilder tmp = new StringBuilder("Time passes...");
                        adv.mALRs.evaluate(tmp, adv.mReferences);
                        adv.mView.mOutputText.append(tmp);
                        for (int i = 0; i < adv.getWaitTurns(); i++) {
                            adv.incrementTurnOrTime(TurnBased);
                        }
                    }
                    break;
                default:
                    return false;
            }

            return true;
        }

        private static void restart(@NonNull MAdventure adv) throws InterruptedException {
            adv.reset();
            adv.open(adv.getFullPath());
        }

        private static void restore(@NonNull MAdventure adv) throws InterruptedException {
            // The Bebek implementation always prompts for file names
            String sFilename = adv.mView.promptForSaveFileName(false);

            if (!sFilename.equals("")) {
                if (new File(sFilename).exists()) {
                    adv.mStates.clear();
                    for (MTask t : adv.mTasks.values()) {
                        // Just in case the save file doesn't cover that task
                        t.setCompleted(false);
                    }
                    MStateStack states = new MStateStack();
                    states.loadState(adv, sFilename);
                    adv.mView.displayText(adv, "Game restored" + "\n", true);
                    adv.mGameState = Running;
                    adv.mDisplayedWinOrLose = false;
                    adv.mView.updateStatusBar(adv);
                    adv.mView.displayText(adv, adv.mLocations.get(adv.getPlayer().getLocation().getLocationKey()).getViewLocation(), true);
                    adv.mJustRunSystemTask = false; // Allow events to run
                    adv.prepareForNextTurn();

                    GLKLogger.error("TODO: MUserSession: Restore - restore map node");
                    //UserSession.Map.RecalculateNode(Adventure.Map.FindNode(Adventure.Player.Location.LocationKey))
                    //UserSession.Map.SelectNode(Adventure.Player.Location.LocationKey)
                } else {
                    adv.mView.displayText(adv, "Save file not found.", true);
                }
            }
        }

        private static boolean hint(@NonNull MAdventure adv) throws InterruptedException {
            boolean first = true;
            for (MHint h : adv.mHints.values()) {
                if (h.mRestrictions.passes(true, null)) {
                    if (!first) {
                        adv.mView.out("<br>");
                    }
                    first = false;
                    adv.mView.out("<br><b><i>" + h.getQuestion() + "</i></b><br>");
                    adv.mView.out("View the subtle hint for this topic? ");
                    char resp = Character.toLowerCase(adv.mView.yesNo());
                    if (resp == 'n') {
                        adv.mView.out("<font color=\"input\">No</font>");
                        return true;
                    }
                    adv.mView.out("<font color=\"input\">Yes</font><br><i>" +
                            h.getSubtleHint().toString(true) + "</i><br>");

                    adv.mView.out("<br>View the unsubtle hint for this topic? ");
                    resp = Character.toLowerCase(adv.mView.yesNo());
                    if (resp == 'n') {
                        adv.mView.out("<font color=\"input\">No</font>");
                        return true;
                    }
                    adv.mView.out("<font color=\"input\">Yes</font><br><i>" +
                            h.getSledgeHammerHint().toString(true) + "</i>");
                }
            }
            if (first) {
                adv.mView.out("<i>No hints currently available.</i>");
            }
            return true;
        }

        private static boolean save(@NonNull MAdventure adv) throws InterruptedException {
            // The Bebek implementation always prompts for file names
            String sFilename = adv.mView.promptForSaveFileName(true);

            if (sFilename.equals("")) {
                adv.mView.displayText(adv, "Cancelled");
                return false;
            }

            MStateStack states = new MStateStack();
            if (MFileIO.saveState(adv, states.getState(adv), sFilename)) {
                adv.mView.displayText(adv, "Game saved");
                adv.setChanged(false);
                return true;
            } else {
                adv.mView.displayText(adv, "Error saving game");
                return false;
            }
        }

        private static boolean quit(@NonNull MAdventure adv,
                                    boolean bJustGame) throws InterruptedException {
            if (adv.mGameState == Running) {
                if (adv.getChanged()) {
              /*  switch (MessageBox.Show("Would you like to save your current position?", "Quit Game",
                        MessageBoxButtons.YesNoCancel, MessageBoxIcon.Question)) {
                    Case DialogResult.Yes:
                    save(True);
                    Case DialogResult.No:
                    // Continue
                    Case DialogResult.Cancel:
                    Return False;
                } */
                    GLKLogger.debug("TODO: Quitting... prompt if user wants to save current position");
                }
            }

            if (bJustGame) {
                adv.mGameState = Neutral;
            } else {
                adv.mView.quit();
            }

            return true;
        }

        private static void undo(@NonNull MAdventure adv) {
            if (adv.mStates.setLastState(adv)) {
                adv.mDisplayedWinOrLose = false;
                GLKLogger.error("TODO: Undo: recalculate map node");
                //MUserSession.Map.RecalculateNode(Adventure.Map.FindNode(Adventure.Player.Location.LocationKey))
                //MUserSession.Map.SelectNode(Adventure.Player.Location.LocationKey)
                adv.mView.updateStatusBar(adv);
                String sText = adv.mTurnOutput;
                adv.mView.displayText(adv, "Undone.", false, false, false);
                if (!sText.equals("")) {
                    adv.mView.displayText(adv, sText);
                }
            } else {
                adv.mView.displayText(adv, "Sorry, <c>undo</c> is not currently available.");
            }
            adv.mJustRunSystemTask = true;
        }
    }
}
