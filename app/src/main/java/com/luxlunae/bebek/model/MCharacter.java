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
import com.luxlunae.bebek.controller.MDebugger;
import com.luxlunae.bebek.model.collection.MCharacterHashMap;
import com.luxlunae.bebek.model.collection.MLocationHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.collection.MTopicHashMap;
import com.luxlunae.bebek.model.io.MFileOlder;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;

import static com.luxlunae.bebek.MGlobals.ALLROOMS;
import static com.luxlunae.bebek.MGlobals.ANYOBJECT;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Definite;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Indefinite;
import static com.luxlunae.bebek.MGlobals.CHARACTERPROPERNAME;
import static com.luxlunae.bebek.MGlobals.HIDDEN;
import static com.luxlunae.bebek.MGlobals.MPerspectiveEnum.FirstPerson;
import static com.luxlunae.bebek.MGlobals.MPerspectiveEnum.SecondPerson;
import static com.luxlunae.bebek.MGlobals.MPerspectiveEnum.ThirdPerson;
import static com.luxlunae.bebek.MGlobals.NOOBJECT;
import static com.luxlunae.bebek.MGlobals.SELECTED;
import static com.luxlunae.bebek.MGlobals.THEFLOOR;
import static com.luxlunae.bebek.MGlobals.THEPLAYER;
import static com.luxlunae.bebek.MGlobals.UNSELECTED;
import static com.luxlunae.bebek.MGlobals.containsWord;
import static com.luxlunae.bebek.MGlobals.getPatterns;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.None;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Objective;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Possessive;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Reflective;
import static com.luxlunae.bebek.model.MAdventure.MPronounEnum.Subjective;
import static com.luxlunae.bebek.model.MAdventure.MTasksListEnum.SystemTasks;
import static com.luxlunae.bebek.model.MCharacter.CharacterType.NonPlayer;
import static com.luxlunae.bebek.model.MCharacter.CharacterType.Player;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Ask;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Command;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Farewell;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Greet;
import static com.luxlunae.bebek.model.MCharacter.ConversationEnum.Tell;
import static com.luxlunae.bebek.model.MCharacter.Gender.Female;
import static com.luxlunae.bebek.model.MCharacter.Gender.Male;
import static com.luxlunae.bebek.model.MCharacter.Gender.Unknown;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.Hidden;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.InObject;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.OnCharacter;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.OnObject;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.Position.Lying;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.Position.Sitting;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.Position.Standing;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.Position.Uninitialised;
import static com.luxlunae.bebek.model.MEventOrWalkControl.MControlEnum.Start;
import static com.luxlunae.bebek.model.MEventOrWalkControl.MControlEnum.Stop;
import static com.luxlunae.bebek.model.MGroup.GroupTypeEnum.Characters;
import static com.luxlunae.bebek.model.MLocation.WhichObjectsToListEnum.AllObjects;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.HeldByCharacter;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.InLocation;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.WornByCharacter;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideOrOnObject;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.Text;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.Must;
import static com.luxlunae.bebek.model.MRestriction.RestrictionTypeEnum.Task;
import static com.luxlunae.bebek.model.MRestriction.TaskEnum.Complete;
import static com.luxlunae.bebek.model.MSingleDescription.DisplayWhenEnum.StartDescriptionWithThis;
import static com.luxlunae.bebek.model.MSubWalk.WhatEnum.ExecuteCommand;
import static com.luxlunae.bebek.model.MSubWalk.WhatEnum.ExecuteTask;
import static com.luxlunae.bebek.model.MSubWalk.WhenEnum.ComesAcross;
import static com.luxlunae.bebek.model.MTask.ExecutionStatus.HasOutput;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Dynamic;
import static com.luxlunae.bebek.model.io.MFileOlder.convertV4FuncsToV5;
import static com.luxlunae.bebek.model.io.MFileOlder.getObjectKey;
import static com.luxlunae.bebek.model.io.MFileOlder.loadResource;
import static com.luxlunae.bebek.view.MView.DebugDetailLevelEnum.Medium;
import static java.util.regex.Pattern.quote;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Characters are independent people or animals within your game.
 * The Player can interact with these characters by having
 * conversations with them, and they can wander around interacting
 * with objects and running tasks.
 */
public class MCharacter extends MItemWithProperties implements MItemFunctionEvaluator {
    // -------------------------------------------------------------
    //                STANDARD LOCATION PROPERTIES
    // -------------------------------------------------------------
    /**
     * The character's position - standing, sitting or
     * lying (text).
     */
    public static final String PKEY_CHAR_POSITION = "CharacterPosition";
    /**
     * Puts a limit on the size of objects the character can
     * carry. Normally only used for the player
     * character (integer).
     */
    public static final String PKEY_MAX_BULK = "MaxBulk";

    // -------------------------------------------------------------
    //               STANDARD CHARACTER PROPERTIES
    // -------------------------------------------------------------
    /**
     * How much weight this character can carry (integer).
     */
    public static final String PKEY_MAX_WEIGHT = "MaxWeight";
    /**
     * Is this character known to the player? (boolean).
     */
    public static final String PKEY_KNOWN = "Known";
    /**
     * What to show when the character is at the player's
     * location (text).
     */
    static final String PKEY_CHAR_HERE_DESC = "CharHereDesc";
    /**
     * Returns 1 for characters that generate a message when
     * they enter or leave the players location, 0 if not. (boolean).
     */
    static final String PKEY_SHOW_ENTER_EXIT = "ShowEnterExit";

    // -------------------------------------------------------------
    //                  STANDARD TEXT PROPERTIES
    // -------------------------------------------------------------
    /**
     * Text to display if the character has a walk set and
     * enters the player's location (text).
     */
    static final String PKEY_CHAR_ENTERS = "CharEnters";
    /**
     * Text to display if the character has a walk set and
     * exits the player's location (text).
     */
    static final String PKEY_CHAR_EXITS = "CharExits";
    /**
     * Displays the contents of the "At Location" property,
     * which is a location KEY. (location).
     */
    private static final String PKEY_AT_LOC = "CharacterAtLocation";
    /**
     * Displays the KEY of the character's current location. If
     * the character is on or in an object this function will
     * return the location that contains them (location).
     */
    private static final String PKEY_LOCATION = "Location";
    /**
     * Key of character that this character is sitting on
     * (character).
     */
    private static final String PKEY_CHAR_ON_WHO = "CharOnWho";
    private static final String PVAL_STANDING = "Standing";
    private static final String PVAL_SITTING = "Sitting";
    private static final String PVAL_LYING = "Lying";
    /**
     * If "Known" property selected then print proper name,
     * otherwise print the characters descriptor (text).
     */
    private static final String PKEY_NAME = "Name";
    private static final String XTAG_NAME = "Name";
    /**
     * Returns the contents of the "Description" text box at the
     * bottom of the description page for the indicated character.
     * If the character has Alternate Descriptions then the
     * restrictions will be applied and the passing descriptions
     * compiled according to the composition settings of each
     * alternate description (text).
     */
    private static final String PKEY_DESCRIPTION = "Description";
    private static final String XTAG_DESCRIPTION = "Description";
    /**
     * Article, adjective and noun of character
     * descriptor (text).
     */
    private static final String PKEY_DESCRIPTOR = "Descriptor";
    private static final String XTAG_DESCRIPTOR = "Descriptor";
    /**
     * The character's gender - male, female or
     * unknown (text).
     */
    private static final String PKEY_GENDER = "Gender";
    private static final String PVAL_MALE = "Male";
    private static final String PVAL_FEMALE = "Female";
    /**
     * Location of the character - "Hidden", "At Location",
     * "On Object", "In Object" or "On Character" (text).
     */
    private static final String PKEY_CHAR_LOCATION = "CharacterLocation";
    private static final String PVAL_AT_LOC = "At Location";

    // -------------------------------------------------------------
    //                 STANDARD INTEGER PROPERTIES
    // -------------------------------------------------------------
    private static final String PVAL_ON_OBJ = "On Object";
    private static final String PVAL_IN_OBJ = "In Object";
    private static final String PVAL_ON_CHAR = "On Character";

    // -------------------------------------------------------------
    //                STANDARD BOOLEAN PROPERTIES
    // -------------------------------------------------------------
    /**
     * Returns the contents of the "Proper Name" field at the top
     * of the description page for the indicated
     * character (text).
     */
    private static final String PKEY_PROPER_NAME = "ProperName";
    /**
     * Always returns 1.
     */
    private static final String PKEY_COUNT = "Count";

    // -------------------------------------------------------------
    //                 STANDARD GROUP PROPERTIES
    // -------------------------------------------------------------
    /**
     * A list of all objects held by the
     * character (group).
     */
    private static final String PKEY_HELD = "Held";
    /**
     * A list of all objects worn by the
     * character (group).
     */
    private static final String PKEY_WORN = "Worn";
    /**
     * A list of all objects being worn and held
     * by the character (group).
     */
    private static final String PKEY_WORN_AND_HELD = "WornAndHeld";
    /**
     * A list of all the exits the character can
     * take to leave their current location (group).
     */
    private static final String PKEY_EXITS = "Exits";

    // -------------------------------------------------------------
    //                STANDARD OBJECT PROPERTIES
    // -------------------------------------------------------------
    /**
     * The key of the container object that the
     * character is inside of (object).
     */
    private static final String PKEY_CHAR_IN = "CharInsideWhat";
    /**
     * The key of the supporter object that the
     * character is on top of (object).
     */
    private static final String PKEY_CHAR_ON_WHAT = "CharOnWhat";

    // -------------------------------------------------------------
    //                STANDARD ITEM PROPERTIES
    // -------------------------------------------------------------
    /**
     * The key of the location or object that
     * immediately contains the character (item).
     */
    private static final String PKEY_PARENT = "Parent";

    private static final String[] CHAR_FUNCTIONS =
            new String[]{"CharacterName", "DisplayCharacter", "ListHeld", "listExits",
                    "ListWorn", "LocationOf", "ParentOf", "ProperName"};

    // -------------------------------------------------------------
    //                        XML TAGS
    // -------------------------------------------------------------
    private static final String XTAG_CHAR = "Character";
    private static final String XTAG_ARTICLE = "Article";
    private static final String XTAG_PREFIX = "Prefix";
    private static final String XTAG_PERSP = "Perspective";
    private static final String XTAG_PROP = "Property";
    private static final String XTAG_TYPE = "Type";
    private static final String XTAG_WALK = "Walk";
    private static final String XTAG_TOPIC = "Topic";

    /**
     * More than one noun can be entered, on different lines, and
     * the player can use any of them to refer to the character.
     */
    @NonNull
    public MStringArrayList mDescriptors = new MStringArrayList();
    /**
     * The movement page allows you to program one or more
     * routes that this character will follow, and actions
     * they will perform at certain times. Each of these
     * independent routes and its associated actions is
     * called a "Walk". A character can have several walks,
     * but will follow one walk at a time.
     */
    @NonNull
    public ArrayList<MWalk> mWalks = new ArrayList<>();
    /**
     * Conversation topics recognised by this character.
     */
    @NonNull
    public MTopicHashMap mTopics = new MTopicHashMap();
    /**
     * The locations seen by this character.
     */
    @NonNull
    public HashMap<String, Boolean> mSeenLocations = new HashMap<>();
    /**
     * The objects seen by this character.
     */
    @NonNull
    public HashMap<String, Boolean> mSeenObjects = new HashMap<>();
    /**
     * The characters seen by this character.
     */
    @NonNull
    public HashMap<String, Boolean> mSeenChars = new HashMap<>();

    @NonNull
    HashMap<String, Boolean> mValidRouteCache = new HashMap<>();
    /**
     * Used with mValidRouteCache to store error text associated
     * with going in a particular direction.
     */
    @NonNull
    HashMap<String, String> mRouteErrors = new HashMap<>();
    /**
     * An article is a word that is used to indicate the type of
     * reference being made by the noun. The articles in the English
     * language are the and a/an, and some.
     */
    @NonNull
    private String mArticle = "";
    /**
     * The adjectives are optional, except where two characters
     * in the same location use the same noun and it is necessary
     * to distinguish between them. If the character is described
     * as "a tall thin chinese man" then the player can enter any
     * combination of adjectives, such as "tall chinese man",
     * "thin man", "tall thin man" etc.
     */
    @NonNull
    private String mPrefix = "";
    /**
     * The Proper Name is the actual name of the character.
     * This name is displayed to the player when the character
     * is in the same location, and is entered by the player
     * when they want to refer to the character.
     * <p>
     * This is only used if this character has the "Known to
     * player" property set.
     */
    @NonNull
    private String mProperName = "";
    /**
     * A character can start out as unknown to the player
     * but later learn their proper name. In this case you
     * would use a task to set the "Known to player" property
     * when they learn their name.
     */
    private boolean mKnown;
    /**
     * Has the player been told the character exists (e.g.
     * "a man"). Once introduced, they will be referred to
     * as "the man" until they are no longer in the same
     * location.
     */
    private boolean mIntroduced = false;
    /**
     * The "Description" text box is what is displayed
     * when the player examine's the character.
     */
    @Nullable
    private MDescription mDescription;
    /**
     * The current location of the character.
     * <p>
     * A character can be:
     * <p>
     * Hidden - A hidden character is not at any
     * location and cannot be found by the player.
     * <p>
     * At Location - When this is selected the "At
     * which location" property becomes available
     * and you can select which location you want
     * the character to start at.
     * <p>
     * On object - The character can be sitting on
     * a chair or laying on a bed. Only objects with
     * the "supporter" property can be selected.
     * <p>
     * In object - You can select from objects that
     * have the "Object is a container" and
     * "characters can go inside this object" properties.
     * <p>
     * On character - This character is on another
     * character, ie. a parrot on a pirates shoulder,
     * someone sitting on a horse, or just someone
     * getting a piggyback ride.
     */
    @NonNull
    private MCharacterLocation mLocation;
    /**
     * Used for compatibility with older games
     * where characters are following but the
     * lead character is moved with a task.
     */
    @Nullable
    private MCharacterLocation mLastLocation;
    private CharacterType mType = NonPlayer;
    @NonNull
    private String mWalkTo = "";
    @NonNull
    private String mLastPosition = "";
    private MGlobals.MPerspectiveEnum mPerspective = SecondPerson; // Default for Player
    /**
     * This gets updated at the end of each turn, and
     * allows any tasks to reference the parent before
     * they are updated from task actions.
     */
    @NonNull
    private String mLastParent = "";

    public MCharacter(@NonNull MAdventure adv) {
        super(adv);
        mLocation = new MCharacterLocation(adv, this);
    }

    public MCharacter(@NonNull MAdventure adv,
                      @NonNull MFileOlder.V4Reader reader,
                      int chID, double v) throws EOFException {
        // ADRIFT V3.80, V3.90 and V4 Loader
        this(adv);

        String myKey = "Character" + chID;
        while (adv.mCharacters.containsKey(myKey)) {
            myKey = incrementKey(myKey);
        }
        setKey(myKey);
        setCharacterType(NonPlayer);

        // ===========================================================
        //               PREFIX, NAME(S), DESCRIPTION
        // -----------------------------------------------------------
        // You must give each character a name.
        // -----------------------------------------------------------
        setProperName(reader.readLine());                               // Name
        setKnown(true);

        // -----------------------------------------------------------
        // You can additionally give them a description, which
        // consists of a prefix and an alias. This is another way
        // that the character can be referenced, much in the same
        // way as the alias for objects. Again, as with objects,
        // any number of aliases can be supplied.
        // -----------------------------------------------------------
        String[] split = convertPrefix(reader.readLine());              // Prefix
        setArticle(split[0]);
        setPrefix(split[1]);

        if (v < 4) {
            // Earlier versions only allow one alias.
            String alias = reader.readLine();                           // Alias
            if (!alias.equals("")) {
                mDescriptors.add(alias);
            }
        } else {
            // Version 4 allows multiple aliases.
            int nAliases = cint(reader.readLine());                     // # Aliases
            for (int i = 0; i < nAliases; i++) {
                mDescriptors.add(reader.readLine());                    // Alias
            }
            if (nAliases == 0 && getPrefix().equals("")) {
                setArticle("");
            }
        }

        // -----------------------------------------------------------
        // You can give the character a description, which
        // appears when the player examines the character.
        // -----------------------------------------------------------
        setDescription(new MDescription(adv,
                convertV4FuncsToV5(adv, reader.readLine())));           // Description

        // ===========================================================
        //                    INITIAL LOCATION
        // -----------------------------------------------------------
        // You must also specify in which room the character
        // should start off, from the last pull down menu. This is
        // either "hidden" or one of the rooms.
        // -----------------------------------------------------------
        MCharacterLocation chLoc = getLocation();
        int chLocID = cint(reader.readLine());                         // Initial Position
        if (chLocID > 0) {
            chLoc.setExistsWhere(AtLocation);
            chLoc.setKey("Location" + chLocID);
        } else {
            chLoc.setExistsWhere(Hidden);
        }

        // ===========================================================
        //                 ALTERNATIVE DESCRIPTION
        // -----------------------------------------------------------
        // You can give a different description depending on
        // whether a certain task has been completed. Just select
        // the task from the pull down menu, and enter the
        // alternative description.
        // -----------------------------------------------------------
        String altDesc = reader.readLine();                             // Alt Desc
        String tKey = "Task" + reader.readLine();                       // Task #
        if (!tKey.equals("Task0")) {
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
            getDescription().add(sd);
        }

        // ===========================================================
        //                      CONVERSATIONS
        // -----------------------------------------------------------
        // Conversations are created simply by adding a subject
        // and a reply. You can enter any number of subjects.
        // -----------------------------------------------------------
        int nTopics = cint(reader.readLine());                          // # Subjects
        for (int i = 1; i <= nTopics; i++) {
            MTopic topic = new MTopic(adv);
            topic.mKey = "Topic" + i;
            topic.mIsAsk = true;

            // The subject is the word that you want to ask the
            // character about in the game. The player would need
            // to type "ask <character> about <subject>". You can
            // enter any number of words in the Subject(s) box,
            // separated by a comma. So for example, if the
            // subject is "fast car, Porsche", then the character
            // would respond to "fast car" and "Porsche", but not
            // "fast" or "car".
            //
            // You can also add a reply to anything by entering
            // an asterix "*" in the subject box. This means that
            // the character will reply to anything you ask them
            // about, unless you have defined other subjects that
            // correctly match what the player types.
            topic.mKeywords = reader.readLine();
            topic.mSummary = "Ask about " + topic.mKeywords;

            // You can give two different replies to any subject,
            // depending on whether or not a particular task has
            // been completed. Simply select which task you want
            // the reply to depend on from the pull down list,
            // and enter your replies in the text boxes.

            // Response 1 (not dependent upon a task completing)
            topic.mDescription =
                    new MDescription(adv,
                            convertV4FuncsToV5(adv, reader.readLine()));

            // Response 2 (may require a task to complete)
            int replyTask = cint(reader.readLine());
            if (replyTask > 0) {
                MSingleDescription sd = new MSingleDescription(adv);
                MRestriction rest = new MRestriction(adv);
                rest.mType = Task;
                rest.mKey1 = "Task" + replyTask;
                rest.mMust = Must;
                rest.mTaskType = Complete;
                sd.mRestrictions.add(rest);
                sd.mDisplayWhen = StartDescriptionWithThis;
                sd.mRestrictions.mBrackSeq = "#";
                topic.mDescription.add(sd);
            }
            topic.mDescription.get(topic.mDescription.size() - 1).mDescription += reader.readLine();

            mTopics.put(topic);
        }

        // ===========================================================
        //                      MOVEMENT
        // -----------------------------------------------------------
        // Character movement can easily be created by adding a
        // series of walks.
        // -----------------------------------------------------------
        int nWalks = cint(reader.readLine());                           // # Walks
        int nLocs = adv.mLocations.size();
        MStringArrayList altCharIsHereDescs = new MStringArrayList();
        for (int i = 0; i < nWalks; i++) {
            // ======================================================
            //                      WALK
            // ------------------------------------------------------
            //             # STEPS, LOOPING, START TASK
            // ------------------------------------------------------
            MWalk walk = new MWalk(adv);
            walk.mCharKey = myKey;

            // ------------------------------------------------------
            // You create walks by adding a sequence of movements
            // (steps).
            // ------------------------------------------------------
            int nSteps = cint(reader.readLine());                       // # Steps

            // ------------------------------------------------------
            // Continue to build up locations to make a complete
            // walk. If you want the character to endlessly loop
            // in that walk, select the Loop walk when finished
            // checkbox. You probably want to ensure that your
            // start and end rooms match up before doing this,
            // otherwise the character will “jump” from one room
            // to the next.
            // ------------------------------------------------------
            walk.setLoops(cbool(reader.readLine()));                    // Looping?

            // ------------------------------------------------------
            // You can select a task to start the walk. As soon as
            // that task executes, the character will begin the
            // defined walk.
            // ------------------------------------------------------
            String startTaskKey = "";
            int startTask = cint(reader.readLine());                    // Start Task #
            if (startTask == 0) {
                // Start the walk immediately.
                walk.setStartActive(true);
            } else {
                // Start the walk after task startTask is complete.
                walk.setStartActive(false);
                startTaskKey = "Task" + startTask;
                MEventOrWalkControl wc = new MEventOrWalkControl();
                wc.eControl = Start;
                wc.mTaskKey = startTaskKey;
                walk.mWalkControls.add(wc);
            }

            // ------------------------------------------------------
            //  TASKS TO RUN WHEN MEETING A CHARACTER AND/OR OBJECT
            // ------------------------------------------------------
            // You can run a task if the character comes across
            // another character or the Player, by selecting
            // from the "If character comes across character"
            // dropdown menu. If this is the Player, and you
            // move into the same room as that character, this
            // will also execute.
            // ------------------------------------------------------
            int findTaskID = cint(reader.readLine());                     // Char Task #

            // ------------------------------------------------------
            // You can also run a task if the character comes
            // across a particular object on their walk, by
            // selecting from the "If character comes across
            // object" dropdown menu.
            // ------------------------------------------------------
            int obFind = cint(reader.readLine());                       // Obj #
            int obTask = cint(reader.readLine());                       // Obj Task #
            if (obFind > 0 && obTask > 0) {
                // Execute task obTask when this character
                // comes across object obFind.
                //
                // Hunting Ground, by Dannymac247, a
                // v4.0.52 game has an object Object56
                // (corpse) which NPCS may stumble across to
                // end the game. The obFind value in that
                // game's source is 4, which implies that,
                // contrary to the original code, the key of
                // the subwalk is derived from the dynamic
                // object index, NOT simply Object + obFind,
                // as used in the original ADRIFT v5 source code.
                MSubWalk sw = new MSubWalk(adv);
                sw.eWhat = ExecuteTask;
                sw.eWhen = ComesAcross;
                sw.sKey = getObjectKey(adv, obFind - 1, Dynamic);
                sw.sKey2 = "Task" + obTask;
                walk.mSubWalks.add(sw);
            }

            // Now fix up any "char comes across" task.
            if (v < 3.9) {
                if (findTaskID > 0) {
                    // Version 3.8 only supports "character comes
                    // across Player" tasks.
                    //
                    // When that happens, it runs the specified
                    // command, as though the player had typed
                    // it. Task execution behaves in the same
                    // way as the HighestPriorityPassingTask
                    // setting - i.e. tasks that pass restrictions
                    // override higher priority tasks that do not,
                    // even when the failing higher priority task
                    // has output.
                    MTask t = adv.mTasks.get("Task" + findTaskID);
                    String cmd = t.mCommands.get(0);
                    MSubWalk sw = new MSubWalk(adv);
                    sw.eWhat = ExecuteCommand;
                    sw.eWhen = ComesAcross;
                    sw.sKey = THEPLAYER;
                    sw.sKey2 = cmd;
                    walk.mSubWalks.add(sw);
                }
            } else {
                // --------------------------------------------------
                //           END TASK, NEW DESCRIPTIONS
                //              (Version 3.9+ only)
                // --------------------------------------------------
                // Finally, you can terminate a walk by selecting
                // a task from the Walk can be terminated if
                // completed task dropdown menu.
                // --------------------------------------------------
                String endTaskKey = "Task" + reader.readLine();         // End Task #
                if (!endTaskKey.equals("Task0")) {
                    // If an end task is set, stop the walk
                    // when that task completes.
                    MEventOrWalkControl wc =
                            new MEventOrWalkControl();
                    wc.eControl = Stop;
                    wc.mTaskKey = endTaskKey;
                    walk.mWalkControls.add(wc);
                }

                int findChID = 0;
                if (v >= 4) {
                    // Version 4 supports "character comes
                    // across Player" AND "character comes
                    // across other character" tasks.
                    // If this is 0, it means the Player.
                    findChID = mAdv.safeInt(reader.readLine());              // Char #
                }
                if (findTaskID > 0) {
                    // Execute task findTaskID when this
                    // character comes across character
                    // findChID.
                    MSubWalk sw = new MSubWalk(adv);
                    sw.eWhat = ExecuteTask;
                    sw.eWhen = ComesAcross;
                    sw.sKey = (v >= 4 && findChID > 0) ?
                            ("Character" + findChID) :
                            THEPLAYER;
                    sw.sKey2 = "Task" + findTaskID;
                    walk.mSubWalks.add(sw);
                }

                // --------------------------------------------------
                // You can update the standard description of
                // the character in a room by adding text to the
                // Description of character in room (look)
                // changes to textbox. This new description will
                // appear any time you view the room the
                // character is in, and supersedes the original
                // text.
                // --------------------------------------------------
                String newDesc = reader.readLine();                     // New Desc
                if (!newDesc.equals("")) {
                    altCharIsHereDescs.add(startTaskKey);
                    altCharIsHereDescs.add(newDesc);
                }
            }

            // ------------------------------------------------------
            //         THE STEPS ASSOCIATED WITH THE WALK
            // ------------------------------------------------------
            // A movement (step) consists of a destination and a
            // length of time.
            //
            // You can move the character to Hidden, Follow Player,
            // a particular room, or to a room group. If the
            // character moves to Follow Player, they will move
            // to the same room as the Player. If the character
            // moves to a room group, they will move to an
            // adjacent room within the room group. If none are
            // available, they will move to a random room within
            // the group. This is a good way to create a random
            // wandering character.
            //
            // You must also specify how long the character should
            // stay at that location before moving onto the next
            // step of the walk. For a fast moving character,
            // this might just be 1.
            // ------------------------------------------------------
            for (int j = 0; j < nSteps; j++) {
                MWalk.MStep step = new MWalk.MStep(adv);
                int dest = cint(reader.readLine());                     // Destination
                switch (dest) {
                    case 0:
                        // Hidden
                        step.mLocation = HIDDEN;
                        break;
                    case 1:
                        // Follow Player
                        step.mLocation = THEPLAYER;
                        break;
                    default:
                        // Locations
                        step.mLocation = (dest - 1 > nLocs) ?
                                "Group" + (dest - 1 - nLocs) :
                                "Location" + (dest - 1);
                        break;
                }
                int waitTurns = cint(reader.readLine());               // Length of time
                step.mTurns.iFrom = waitTurns;
                step.mTurns.iTo = waitTurns;
                walk.mSteps.add(step);
            }

            walk.setDescription(walk.getDefaultDescription());
            mWalks.add(walk);
        }

        // ===========================================================
        //      PROPERTY - SHOW CHARACTER ENTERING/EXITING ROOM
        // -----------------------------------------------------------
        // If you want to be notified when a character enters or
        // exits the room that the Player is currently in, check
        // the check box. This will enable the two description
        // boxes at the bottom of the screen. You can modify what
        // it says when the character moves. You would typically
        // put “enters” and “exits” in these boxes so it displays
        // “<Character> enters from the east.”, but you may want
        // to change this for the different ways a character can
        // move, such as “run”, “shuffle”, “trot” etc.
        // -----------------------------------------------------------
        MProperty p;
        if (cbool(reader.readLine())) {                                 // Show Enter/Exit?
            p = adv.mAllProperties.get(PKEY_SHOW_ENTER_EXIT).copy();
            p.setSelected(true);
            addProperty(p);

            String enterText = reader.readLine();                       // Enter Text
            p = adv.mAllProperties.get(PKEY_CHAR_ENTERS).copy();
            p.setSelected(true);
            p.setStringData(new MDescription(adv,
                    convertV4FuncsToV5(adv, enterText)));
            addProperty(p);

            String exitText = reader.readLine();                        // Exit Text
            p = adv.mAllProperties.get(PKEY_CHAR_EXITS).copy();
            p.setSelected(true);
            p.setStringData(new MDescription(adv,
                    convertV4FuncsToV5(adv, exitText)));
            addProperty(p);
        }

        // ===========================================================
        //    PROPERTY - WHAT TO SHOW WHEN CHARACTER IS IN THE ROOM
        // -----------------------------------------------------------
        // Usually you will want to display a message to say if
        // the character is in the current room. You can modify
        // this by changing the relevant message. This message
        // appears when the player types "look", or moves into a room.
        // -----------------------------------------------------------
        String inRoomText = reader.readLine();                          // In Room Text
        if (inRoomText.equals("#")) {
            inRoomText = "%CharacterName[" + myKey + "]% is here.";
        }
        if (!inRoomText.equals("") || altCharIsHereDescs.size() > 0) {
            p = adv.mAllProperties.get(PKEY_CHAR_HERE_DESC).copy();
            p.setSelected(true);
            addProperty(p);
        }
        if (!inRoomText.equals("")) {
            setPropertyValue(PKEY_CHAR_HERE_DESC,
                    new MDescription(adv, convertV4FuncsToV5(adv, inRoomText)));
        }

        // -----------------------------------------------------------
        //       Add the alternative "char is here" descriptions
        // -----------------------------------------------------------
        // We add these as alternative descriptions to the default
        // char is here property. The altCharIsHereDescs is assumed
        // to consist of (TaskID, AltText) pairs, where AltText is
        // to be displayed instead of the default "char is here"
        // description if task TaskID has completed.
        for (int i = 0; i < altCharIsHereDescs.size(); i += 2) {
            MSingleDescription sd = new MSingleDescription(adv);
            MRestriction rest = new MRestriction(adv);
            rest.mType = Task;
            rest.mKey1 = altCharIsHereDescs.get(i);
            rest.mMust = Must;
            rest.mTaskType = Complete;
            sd.mRestrictions.add(rest);
            sd.mRestrictions.mBrackSeq = "#";
            sd.mDescription = altCharIsHereDescs.get(i + 1);
            sd.mDisplayWhen = StartDescriptionWithThis;
            getProperty(PKEY_CHAR_HERE_DESC).getStringData().add(sd);
        }

        if (v >= 3.9) {
            // ======================================================
            //                   PROPERTY - GENDER
            //             (only supported in version 4)
            // ------------------------------------------------------
            // The Gender of the character must be supplied.
            // This would normally be Male or Female, but
            // for monsters and some animals, you might want
            // to specify it as “Unknown”. This means that it
            // could be referred to as “it” in the game,
            // rather than “he” or “she”.
            // ------------------------------------------------------
            setGender(MCharacter.getGenderEnumFromInt(
                    cint(reader.readLine())));                          // Gender

            // ======================================================
            //                     RESOURCES
            //             (only supported in version 4)
            // ------------------------------------------------------
            for (int i = 0; i < 4; i++) {
                switch (i) {
                    case 0:
                        loadResource(adv, reader, v,
                                getDescription().get(0));
                        break;
                    case 1:
                        MDescription des = getDescription();
                        loadResource(adv, reader, v,
                                (des.size() > 1) ? des.get(1) : null);
                        break;
                    case 2: {
                        MProperty prop = getProperty(PKEY_CHAR_ENTERS);
                        loadResource(adv, reader, v,
                                (prop != null) ? prop.getStringData().get(0) : null);
                        break;
                    }
                    case 3: {
                        MProperty prop = getProperty(PKEY_CHAR_EXITS);
                        loadResource(adv, reader, v,
                                (prop != null) ? prop.getStringData().get(0) : null);
                        break;
                    }
                }
            }

            if (adv.mCompatBattleSystem) {
                // ==========================================
                //             NPC BATTLE SYSTEM
                // ------------------------------------------
                reader.skipLine();                              // iAttitude
                reader.skipLine();                              // iStaminaLo
                if (v >= 4) {
                    reader.skipLine();                          // iStaminaHi
                }
                reader.skipLine();                              // iStrengthLo
                if (v >= 4) {
                    reader.skipLine();                          // iStrengthHi
                    reader.skipLine();                          // iAccuracyLo
                    reader.skipLine();                          // iAccuracyHi
                }
                reader.skipLine();                              // iDefenseLo
                if (v >= 4) {
                    reader.skipLine();                          // iDefenseHi
                    reader.skipLine();                          // iAgilityLo
                    reader.skipLine();                          // iAgilityHi
                }
                reader.skipLine();                              // iSpeed
                reader.skipLine();                              // iKilledTask
                if (v >= 4) {
                    reader.skipLine();                          // iRecovery
                    reader.skipLine();                          // iStaminaTask
                }
            }
        }
    }

    public MCharacter(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                      boolean bLibrary, boolean bAddDuplicateKeys,
                      double v) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, XTAG_CHAR);

        // defaults:
        setDescription(new MDescription(adv));
        setPerspective((v >= 5.00002) ?
                ThirdPerson : SecondPerson);

        String s;
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag())
                != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        header.processTag(xpp);
                        break;

                    case XTAG_NAME:
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setProperName(s);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        break;

                    case XTAG_ARTICLE:
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setArticle(s);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        break;

                    case XTAG_PREFIX:
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setPrefix(s);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        break;

                    case XTAG_PERSP:
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setPerspective(MGlobals.MPerspectiveEnum.valueOf(s));
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        break;

                    case XTAG_DESCRIPTOR:
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                mDescriptors.add(s);
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        break;

                    case XTAG_PROP:
                        try {
                            addProperty(new MProperty(adv, xpp, v));
                        } catch (Exception e) {
                            // do nothing
                        }
                        break;

                    case XTAG_DESCRIPTION:
                        setDescription(new MDescription(adv, xpp,
                                v, XTAG_DESCRIPTION));
                        break;

                    case XTAG_TYPE:
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setCharacterType(CharacterType.valueOf(s));
                            }
                        } catch (Exception e) {
                            continue;
                        }
                        break;

                    case XTAG_WALK:
                        mWalks.add(new MWalk(adv, xpp, v));
                        break;

                    case XTAG_TOPIC:
                        mTopics.put(new MTopic(adv, xpp, v));
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, XTAG_CHAR);

        if (!header.finalise(this, adv.mCharacters,
                bLibrary, bAddDuplicateKeys, null)) {
            throw new Exception("Can't finalise header for character [" +
                    getKey() + "] " + getProperName());
        }

        for (MWalk walk : mWalks) {
            walk.mCharKey = getKey();
        }

        setLocation(getLocation()); // Assigns the location object from the character properties

        for (String func : CHAR_FUNCTIONS) {
            find("%" + func + "%",
                    "%" + func + "[" + getKey() + "]%");
        }
    }

    private static Gender getGenderEnumFromInt(int value) {
        switch (value) {
            case 0:
            default:
                return Male;
            case 1:
                return Female;
            case 2:
                return Unknown;
        }
    }

    void setProperty(@NonNull String propName, @NonNull String propValue) {
        MProperty prop = null;

        switch (propName) {
            case CHARACTERPROPERNAME:
                prop = new MProperty(mAdv);
                prop.setKey(propName);
                prop.setType(Text);
                String result = mAdv.evalStrExpr(propValue, mAdv.mReferences);
                if (result.equals("") && !propValue.equals("")) {
                    result = propValue;
                }
                setProperName(result);
                break;
            default:
                if (hasProperty(propName)) {
                    prop = getProperty(propName);
                }
                if (prop == null) {
                    // Property doesn't already exist for this character.
                    if (propValue.equals(SELECTED)) {
                        // We want to add it
                        if (mAdv.mCharacterProperties.containsKey(propName)) {
                            prop = mAdv.mCharacterProperties.get(propName).clone();
                            switch (prop.getType()) {
                                case SelectionOnly:
                                    prop.setSelected(true);
                                    addProperty(prop);
                                    break;
                            }
                        } else {
                            mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, getKey(),
                                    MView.DebugDetailLevelEnum.Error,
                                    "Can't select property " + propName + " for character " +
                                            getCommonName() + " as that property doesn't exist in the global character properties.");
                        }
                    } else {
                        mAdv.mView.debugPrint(MGlobals.ItemEnum.Task, getKey(),
                                MView.DebugDetailLevelEnum.Error,
                                "Can't set property '" + propName + "' of character '" +
                                        getCommonName() + "' to '" + propValue +
                                        "' as the character doesn't have that property.");
                    }
                } else {
                    // Property already exists for this character
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
                                result = mAdv.evalStrExpr(propValue, mAdv.mReferences);
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
                                    itemKey = mAdv.mReferences.getMatchingKey(itemKey);
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
                        getLocation().resetPosition();
                    }
                }
        }
    }

    void executeConversation(@NonNull ConversationEnum convType,
                             @NonNull String cmdOrSubject,
                             @NonNull EnumSet<MTask.ExecutionStatus> curStatus) {
        mAdv.mView.debugPrint(MGlobals.ItemEnum.Character, getKey(), Medium,
                "Execute Conversation " + convType.toString() + ": " + cmdOrSubject);

        // Process any enter or leave conversation first
        switch (convType) {
            case EnterConversation:
                mAdv.mConversationCharKey = getKey();
                return;

            case LeaveConversation:
                if (mAdv.mConversationCharKey.equals(getKey())) {
                    mAdv.mConversationCharKey = "";
                }
                return;
        }

        // PROCESS FAREWELL FOR ANY CHARACTER WE ARE TERMINATING CONVERSATION WITH
        // If currently in a conversation with a different character, search for an Implicit Farewell for other char
        if (!mAdv.mConversationCharKey.equals("") && !mAdv.mConversationCharKey.equals(getKey())) {
            // N.B. Original source had ConvType as the 2nd arg in next line, which seems to be a bug - TCC Apr 2018
            MCharacter ch = mAdv.mCharacters.get(mAdv.mConversationCharKey);
            if (ch != null) {
                MTopic farewell = ch.findConversationNode(EnumSet.of(Farewell), "");
                if (farewell != null) {
                    boolean[] b = new boolean[1];
                    if (mAdv.mPassResponses.addResponse(mAdv, b,
                            farewell.mDescription.toString(), new String[]{}, mAdv.mReferences)) {
                        curStatus.add(HasOutput);
                    }
                }
            }
            mAdv.mConversationCharKey = "";
            mAdv.mConversationNode = "";
        }

        // PROCESS INTRO FOR THE CHARACTER WE ARE ENTERING THE CONVERSATION WITH
        // If not currently in conversation and ConvType != Intro, then search for an intro for that char
        if (mAdv.mConversationCharKey.equals("")) {
            // Try to find an explicit intro
            MTopic intro = findConversationNode(EnumSet.of(convType, Greet), cmdOrSubject);
            if (intro == null) {
                // Not found, so look for an implicit one
                intro = findConversationNode(EnumSet.of(Greet), "");
            }
            if (intro != null) {
                boolean[] b = new boolean[1];
                if (mAdv.mPassResponses.addResponse(mAdv, b,
                        intro.mDescription.toString(), new String[]{}, mAdv.mReferences)) {
                    curStatus.add(HasOutput);
                }
                mAdv.mConversationNode = intro.mKey;
                if (intro.mIsAsk || intro.mIsTell || intro.mIsCommand) {
                    // We matched an explicit intro, so no need to look further
                    mAdv.mConversationCharKey = getKey();
                    if (intro.mActions.size() > 0) {
                        intro.mActions.executeAll(curStatus);
                    }
                    return;
                }
                // TODO - Run the implicit actions if we didn't find an explicit match later.
            }
        }

        // START THE CONVERSATION
        // Enter conversation with character
        mAdv.mConversationCharKey = getKey();

        // Find conversation node (try to match on Farewell commands first)
        MTopic topic = null;
        String sRestrictionTextTemp = mAdv.mRestrictionText;
        mAdv.mRestrictionText = "";
        if (convType == Command) {
            topic = findConversationNode(EnumSet.of(convType, Farewell), cmdOrSubject);
        }
        if (topic == null) {
            topic = findConversationNode(EnumSet.of(convType), cmdOrSubject);
        } else {
            mAdv.mConversationCharKey = "";
            mAdv.mConversationNode = "";
        }
        if (topic != null) {
            boolean[] b = new boolean[1];
            if (mAdv.mPassResponses.addResponse(mAdv, b, topic.mDescription.toString(),
                    new String[]{}, mAdv.mReferences)) {
                curStatus.add(HasOutput);
            }

            // If topic has children, set the conversation node
            if (mTopics.doesTopicHaveChildren(topic.mKey)) {
                mAdv.mConversationNode = topic.mKey;
            } else {
                if (!topic.mStayInMode) {
                    mAdv.mConversationNode = "";
                }
            }
            if (topic.mActions.size() > 0) {
                topic.mActions.executeAll(curStatus);
            }
        } else {
            // Hmm, no conversation found.  Need to give a default response back...
            mAdv.mConversationNode = "";
            String msg = "";
            if (!mAdv.mRestrictionText.equals("")) {
                msg = mAdv.mRestrictionText;
            } else {
                switch (convType) {
                    case Ask:
                        msg = (mAdv.mVersion < 5) ?
                                "%CharacterName[" + getKey() + "]% does not respond to your question." :
                                "%CharacterName[" + getKey() + "]% doesn't appear to understand you.";
                        break;
                    case Farewell:
                        msg = "%CharacterName[" + getKey() + "]% doesn't appear to understand you.";
                        break;
                    case Greet:
                        msg = "%CharacterName[" + getKey() + "]% doesn't appear to understand you.";
                        break;
                    case Tell:
                        msg = "%CharacterName[" + getKey() + "]% doesn't appear to understand you.";
                        break;
                    case Command:
                        msg = "%CharacterName[" + getKey() + "]% ignores you.";
                        break;
                }
            }

            boolean[] b = new boolean[1];
            if (mAdv.mPassResponses.addResponse(mAdv, b, msg,
                    new String[]{}, mAdv.mReferences)) {
                curStatus.add(HasOutput);
            }
        }
        // Dunno if we really need to do this, but may as well to be safe:
        mAdv.mRestrictionText = sRestrictionTextTemp;
    }

    void moveCharacter(@NonNull MAction.MoveCharacterToEnum op, @NonNull String toKey) {
        // Characters can be moved to a new location, or added to (or removed from)
        // a character group. One or more characters can be selected from:
        //
        //    The player character
        //    A Referenced character
        //    A particular character
        //    Everyone at a particular location
        //    Everyone in a character group
        //    Everyone inside a particular container object (such as an elevator or car)
        //    Everyone on a particular object with a surface
        //    Everyone who has a particular property set to a particular value
        //
        // The selected character(s) can be moved to:
        //
        //    A specific location (but NOT to a referenced location)
        //    The special 'Hidden' location that removes the character from the game world
        //    A random location chosen from a particular location group
        //    Laying, sitting or standing on or in an appropriate object or
        //    character such as a bed, chair, car, elevator, horse or the floor (can be a referenced object)
        //    The parent location (to get out of or off of an object)
        //    The same location as a specific object or character
        //    Switch places with another character
        //    A specific direction from the current location
        //
        // This action can also be used to change the "point of view" of the player,
        // allowing them to play as different characters in the game world.
        // Use [move][character][ The Player Character ][to switch places with] the
        // character they are to play as.
        MCharacterLocation dest = new MCharacterLocation(mAdv, this);
        MCharacterLocation chLoc = getLocation();
        String chLocKey = getLocation().getLocationKey();

        // Default new destination to current location
        dest.setExistsWhere(chLoc.getExistsWhere());
        dest.setKey(chLocKey);
        dest.setPosition(chLoc.getPosition());

        switch (op) {
            case InDirection:
                MAdventure.DirectionsEnum d = MAdventure.DirectionsEnum.valueOf(toKey);
                MLocation loc = mAdv.mLocations.get(chLocKey);
                MLocation.MDirection dDetails = (loc != null) ?
                        loc.mDirections.get(d) : null;
                String sRouteErrorTask = mAdv.mRouteErrorText; // because mRouteErrorText gets overwritten by checking route restrictions
                String sRestrictionTextTemp = mAdv.mRestrictionText;
                mAdv.mRestrictionText = "";
                if (dDetails != null && hasRouteInDirection(d, false)) {
                    if (mAdv.mLocations.containsKey(dDetails.mLocationKey)) {
                        dest.setKey(dDetails.mLocationKey);
                    } else if (mAdv.mGroups.containsKey(dDetails.mLocationKey)) {
                        dest.setKey(mAdv.mGroups.get(dDetails.mLocationKey).getRandomKey());
                    }
                    dest.setExistsWhere(AtLocation);
                } else {
                    if (!mAdv.mRestrictionText.equals("")) {
                        mAdv.mView.displayText(mAdv, mAdv.mRestrictionText);
                    } else {
                        // Need to grab out the restriction text from the movement task
                        if (!sRouteErrorTask.equals("")) {
                            mAdv.mView.displayText(mAdv, sRouteErrorTask);
                        }
                    }
                    dest = null;
                }
                // TCC: original code sets this to mRestrictionText, which seems to be a bug:
                mAdv.mRestrictionText = sRestrictionTextTemp;
                break;
            case ToLocation:
                if (toKey.equals("Hidden")) {
                    dest.setExistsWhere(Hidden);
                    dest.setKey("");
                } else {
                    dest.setExistsWhere(AtLocation);
                    dest.setKey(toKey);
                }
                break;
            case ToLocationGroup:
                dest.setExistsWhere(AtLocation);
                dest.setKey(mAdv.mGroups.get(toKey).getRandomKey());
                break;
            case ToLyingOn:
                dest.setPosition(Lying);
                if (toKey.equals(THEFLOOR)) {
                    dest.setExistsWhere(AtLocation);
                    dest.setKey(getLocation().getLocationKey());
                } else {
                    dest.setExistsWhere(OnObject);
                    dest.setKey(toKey);
                }
                break;
            case ToSameLocationAs:
                MCharacter chDest = mAdv.mCharacters.get(toKey);
                if (chDest != null) {
                    MCharacterLocation chDestLoc = chDest.getLocation();
                    dest.setExistsWhere(chDestLoc.getExistsWhere());
                    dest.setKey(chDestLoc.getKey());
                } else {
                    MObject obDest = mAdv.mObjects.get(toKey);
                    if (obDest != null) {
                        MObject.MObjectLocation obDestLoc = obDest.getLocation();
                        if (obDest.isStatic()) {
                            switch (obDestLoc.mStaticExistWhere) {
                                case AllRooms:
                                case LocationGroup:
                                    // Doesn't make sense to map
                                    break;
                                case NoRooms:
                                    dest.setExistsWhere(Hidden);
                                    break;
                                case PartOfCharacter:
                                    mAdv.mView.TODO("Move Char to same location as object that is part of a character");
                                    break;
                                case PartOfObject:
                                    mAdv.mView.TODO("Move Char to same location as object that is part of an object");
                                    break;
                                case SingleLocation:
                                    dest.setExistsWhere(AtLocation);
                                    dest.setKey(obDestLoc.getKey());
                                    break;
                            }
                        } else {
                            switch (obDestLoc.mDynamicExistWhere) {
                                case HeldByCharacter:
                                case WornByCharacter:
                                    chDest = mAdv.mCharacters.get(obDest.getKey());
                                    if (chDest != null) {
                                        MCharacterLocation chDestLoc = chDest.getLocation();
                                        dest.setExistsWhere(chDestLoc.getExistsWhere());
                                        dest.setKey(chDestLoc.getKey());
                                    }
                                    break;
                                case Hidden:
                                    dest.setExistsWhere(Hidden);
                                    break;
                                case InLocation:
                                    dest.setExistsWhere(AtLocation);
                                    dest.setKey(obDestLoc.getKey());
                                    break;
                                case InObject:
                                case OnObject:
                                    dest.setExistsWhere(AtLocation);
                                    MLocationHashMap locs = mAdv.mObjects.get(obDest.getKey()).getRootLocations();
                                    if (locs.size() > 0) {
                                        // set to first entry in the hash map
                                        dest.setKey(locs.entrySet().iterator().next().getKey());
                                    }
                                    break;
                            }
                        }
                    }
                }
                break;
            case ToSittingOn:
                dest.setPosition(Sitting);
                if (toKey.equals(THEFLOOR)) {
                    dest.setExistsWhere(AtLocation);
                    dest.setKey(chLocKey);
                } else {
                    dest.setExistsWhere(OnObject);
                    dest.setKey(toKey);
                }
                break;
            case ToStandingOn:
                dest.setPosition(Standing);
                if (toKey.equals(THEFLOOR)) {
                    dest.setExistsWhere(AtLocation);
                    dest.setKey(chLocKey);
                } else {
                    dest.setExistsWhere(OnObject);
                    dest.setKey(toKey);
                }
                break;
            case ToSwitchWith:
                MCharacter oldPlayer = mAdv.getPlayer();
                String oldPlayerKey = oldPlayer.getKey();
                String chKey = getKey();

                if (chKey.equals(oldPlayerKey) || toKey.equals(oldPlayerKey)) {
                    // Don't move the characters, but change which one is the player
                    MGlobals.MPerspectiveEnum eCurrentPerspective = oldPlayer.getPerspective();
                    oldPlayer.setCharacterType(NonPlayer);
                    if (chKey.equals(oldPlayerKey)) {
                        mAdv.setPlayer(mAdv.mCharacters.get(toKey));
                    } else {
                        mAdv.setPlayer(mAdv.mCharacters.get(chKey));
                    }
                    MCharacter newPlayer = mAdv.getPlayer();
                    newPlayer.setCharacterType(Player);
                    newPlayer.setPerspective(eCurrentPerspective);

                    String[] pronouns = new String[0];
                    switch (eCurrentPerspective) {
                        case FirstPerson:
                            pronouns = new String[]{"i", "me", "myself"};
                            break;
                        case SecondPerson:
                            pronouns = new String[]{"i", "me", "myself", "you", "yourself"}; // include 1st in 2nd
                            break;
                        case ThirdPerson:
                            break;
                    }

                    // If the old Player character has any descriptors that match any
                    // pronouns for the player perspective, move them to the new Player
                    for (int i = oldPlayer.mDescriptors.size() - 1; i >= 0; i--) {
                        String desc = oldPlayer.mDescriptors.get(i).toLowerCase();
                        for (String pronoun : pronouns) {
                            if (pronoun.equals(desc)) {
                                oldPlayer.mDescriptors.remove(i);
                                if (!newPlayer.mDescriptors.contains(pronoun)) {
                                    newPlayer.mDescriptors.add(pronoun);
                                }
                            }
                        }
                    }
                } else {
                    // Move the characters about
                    MCharacter chDest2 = mAdv.mCharacters.get(toKey);
                    if (chDest2 != null) {
                        setLocation(chDest2.getLocation());
                        chDest2.setLocation(chLoc);
                    }
                }
                break;
            case InsideObject:
                dest.setExistsWhere(InObject);
                dest.setKey(toKey);
                break;
            case OntoCharacter:
                if (mAdv.mCharacters.containsKey(toKey)) {
                    if (getKey().equals(toKey) ||
                            getChildChars(true).containsKey(toKey)) {
                        mAdv.mView.displayError("Recursive character relationship");
                    } else {
                        dest.setExistsWhere(OnCharacter);
                        dest.setKey(toKey);
                    }
                }
                break;
            case ToParentLocation:
                dest.setExistsWhere(AtLocation);
                String sCurrent = chLoc.getKey();
                if (mAdv.mObjects.containsKey(sCurrent)) {
                    MLocationHashMap locs = mAdv.mObjects.get(sCurrent).getRootLocations();
                    if (locs.size() > 0) {
                        // set to first entry in the hash map
                        dest.setKey(locs.entrySet().iterator().next().getKey());
                    }
                } else {
                    MCharacter chCurrent = mAdv.mCharacters.get(sCurrent);
                    if (chCurrent != null) {
                        dest.setKey(chCurrent.getLocation().getLocationKey());
                    }
                }
                break;
            default:
                mAdv.mView.TODO("TODO: Move Character to " + op.toString());
                break;
        }
        if (dest != null) {
            moveTo(dest);
        }
    }

    @Override
    @NonNull
    public String evaluate(@NonNull String funcName,
                           @NonNull String args,
                           @NonNull String remainder,
                           @NonNull boolean[] resultIsInteger) {
        switch (funcName) {
            case "":
                // ----------------------------------------
                //              %character%
                //            (Text property)
                // ----------------------------------------
                // No character property specified, which means
                // we just return this character's key.
                return getKey();

            case PKEY_COUNT:
                // ----------------------------------------
                //           %character%.Count
                //           (Integer property)
                // ----------------------------------------
                // Returns 1.
                resultIsInteger[0] = true;
                return "1";

            case PKEY_DESCRIPTOR:
                // ----------------------------------------
                //        %character%.Descriptor
                //            (Text property)
                // ----------------------------------------
                // Article, adjective and noun of character
                // descriptor.
                return getDescriptor();

            case PKEY_DESCRIPTION:
                // ----------------------------------------
                //         %character%.Description
                //             (Text property)
                // ----------------------------------------
                return getDescription().toString();

            case PKEY_EXITS:
                // ----------------------------------------
                //           %character%.Exits
                //           (Group property)
                // ----------------------------------------
                // A list of all of the exits the character
                // can take to leave their current location.
                ArrayList<MAdventure.DirectionsEnum> lstDirs = new ArrayList<>();
                MCharacter pl = mAdv.getPlayer();
                String plLocKey = pl.getLocation().getLocationKey();
                for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
                    if (pl.hasRouteInDirection(d, false, plLocKey)) {
                        lstDirs.add(d);
                    }
                }
                return mAdv.evalItemFunc(remainder, null,
                        lstDirs, null, null, resultIsInteger);

            case PKEY_HELD: {
                // ----------------------------------------
                //           %character%.Held
                //           (Group property)
                // ----------------------------------------
                // A list of all objects held by the
                // character.
                ArrayList<MItemWithProperties> lst = new ArrayList<>();
                switch (args.toLowerCase()) {
                    case "":
                    case "true":
                    case "1":
                    case "-1":
                        lst.addAll(getHeldObjects(true).values());
                        break;
                    case "false":
                    case "0":
                        lst.addAll(getHeldObjects(false).values());
                        break;
                }
                return mAdv.evalItemFunc(remainder, lst,
                        null, null, this, resultIsInteger);
            }

            case PKEY_LOCATION:
                // ----------------------------------------
                //          %character%.Location
                //          (Location property)
                // ----------------------------------------
                // Displays the KEY of the character's
                // current location. If the character is
                // on or in an object this function will
                // return the location that contains them.
                String locKey = getLocation().getLocationKey();
                MLocation loc = mAdv.mLocations.get(locKey);
                return mAdv.evalItemFunc(remainder, null,
                        null, null, loc, resultIsInteger);

            case PKEY_NAME:
                // ----------------------------------------
                //            %character%.Name
                //            (Text property)
                // ----------------------------------------
                // If "Known" property selected then print
                // proper name, otherwise print the
                // character's descriptor.
                boolean forcePronoun = false;
                MAdventure.MPronounEnum pronoun = Subjective;
                boolean explicitArticle = false;
                MGlobals.ArticleTypeEnum article = Definite;

                args = args.toLowerCase();
                if (args.contains("none")) {
                    // Could mean either article or pronoun
                    if (args.contains("definite") || args.contains("indefinite")) {
                        pronoun = None;
                    } else if (args.contains("force") || args.contains("objective") ||
                            args.contains("possessive") || args.contains("reflective")) {
                        article = MGlobals.ArticleTypeEnum.None;
                    } else {
                        // if only None is specified, assume they
                        // mean pronouns, as less likely they'll disable
                        // articles on character names.
                        pronoun = None;
                    }
                }
                if (args.contains("force")) {
                    forcePronoun = true;
                }
                if (args.contains("objective") ||
                        args.contains("object") || args.contains("target")) {
                    pronoun = Objective;
                }
                if (args.contains("possessive") || args.contains("possess")) {
                    pronoun = Possessive;
                }
                if (args.contains("reflective") || args.contains("reflect")) {
                    pronoun = Reflective;
                }
                if (containsWord(args, "definite")) {
                    article = Definite;
                    explicitArticle = true;
                }
                if (containsWord(args, "indefinite")) {
                    article = Indefinite;
                    explicitArticle = true;
                }
                return getName(pronoun, true, true,
                        article, forcePronoun, explicitArticle);

            case PKEY_PARENT:
                // ----------------------------------------
                //           %character%.Parent
                //            (Item property)
                // ----------------------------------------
                // The key of the location or object
                // that immediately contains the character.
                String parent = getParent();
                MItemFunctionEvaluator item = mAdv.mObjects.get(parent);
                if (item == null) {
                    item = mAdv.mCharacters.get(parent);
                }
                if (item == null) {
                    item = mAdv.mLocations.get(parent);
                }
                return mAdv.evalItemFunc(remainder, null,
                        null, null, item, resultIsInteger);

            case PKEY_PROPER_NAME:
                // ----------------------------------------
                //     %character%.CharacterProperName
                //            (Text property)
                // ----------------------------------------
                // Returns the contents of the "Proper Name"
                // field at the top of the description page
                // for the indicated character.
                return getProperName();

            case PKEY_WORN: {
                // ----------------------------------------
                //           %character%.Worn
                //           (Group property)
                // ----------------------------------------
                // A list of all objects worn by the character.
                ArrayList<MItemWithProperties> lst = new ArrayList<>();
                switch (args.toLowerCase()) {
                    case "":
                    case "true":
                    case "1":
                    case "-1":
                        lst.addAll(getWornObjects(true).values());
                        break;
                    case "false":
                    case "0":
                        lst.addAll(getWornObjects(false).values());
                        break;
                }
                return mAdv.evalItemFunc(remainder, lst,
                        null, null, null, resultIsInteger);
            }

            case PKEY_WORN_AND_HELD: {
                // ----------------------------------------
                //        %character%.WornAndHeld
                //           (Group property)
                // ----------------------------------------
                // A list of all objects being worn and
                // held by the character.
                ArrayList<MItemWithProperties> lst = new ArrayList<>();
                switch (args.toLowerCase()) {
                    case "":
                    case "true":
                    case "1":
                    case "-1":
                        lst.addAll(getWornObjects(true).values());
                        lst.addAll(getHeldObjects(true).values());
                        break;
                    case "false":
                    case "0":
                        lst.addAll(getWornObjects(false).values());
                        lst.addAll(getHeldObjects(false).values());
                        break;
                }
                return mAdv.evalItemFunc(remainder, lst,
                        null, null, null, resultIsInteger);
            }

            default:
                // Any other valid property not already
                // covered above.
                return mAdv.evalItemProp(funcName, getProperties(),
                        mAdv.mCharacterProperties, remainder, resultIsInteger);
        }
    }

    /**
     * Find the conversation node on this character that best
     * matches the given conversation type and command / subject.
     *
     * @param nodeType     - the conversation type to find.
     * @param cmdOrSubject - the command or subject to find.
     * @return the node that best matches or NULL if nothing found.
     */
    @Nullable
    private MTopic findConversationNode(@NonNull EnumSet<ConversationEnum> nodeType,
                                        @NonNull String cmdOrSubject) {
        boolean findIntro = nodeType.contains(Greet);
        boolean findFarewell = nodeType.contains(Farewell);
        boolean findCommand = nodeType.contains(Command);
        boolean findAsk = nodeType.contains(Ask);
        boolean findTell = nodeType.contains(Tell);
        double highestPercent = 0;
        int nMostMatches = 0;
        MTopic bestTopic = null;

        for (MTopic topic : mTopics.values()) {
            int nMatchedKeywords = 0;

            if ((topic.mParentKey.equals("") ||
                    topic.mParentKey.equals(mAdv.mConversationNode)) &&
                    (!findIntro || topic.mIsIntro) &&
                    (!findFarewell || topic.mIsFarewell) &&
                    (findCommand == topic.mIsCommand) &&
                    (!findAsk || topic.mIsAsk) &&
                    (!findTell || topic.mIsTell)) {

                if (findAsk || findTell) {
                    // Keyword matching

                    // Find the node that matches the most keywords.
                    // Then if there are more than one, pick the one
                    // that matches the most as a percentage.
                    String[] keywords = topic.mKeywords.split(",");
                    boolean isLowPriority = false;

                    for (String keyword : keywords) {
                        if (containsWord(cmdOrSubject, keyword.toLowerCase().trim()) ||
                                keyword.equals("*")) {
                            if (topic.mRestrictions.passes(mAdv.mReferences)) {
                                // Return topic
                                nMatchedKeywords++;
                            }
                            if (keyword.equals("*")) {
                                isLowPriority = true;
                            }
                        }
                    }

                    double dfPercentMatched =
                            (double) nMatchedKeywords / (double) keywords.length;
                    if (isLowPriority && dfPercentMatched == 1) {
                        dfPercentMatched = 0.001;
                    }

                    if (nMatchedKeywords > nMostMatches ||
                            (nMatchedKeywords == nMostMatches &&
                                    dfPercentMatched > highestPercent)) {
                        bestTopic = topic;
                        highestPercent = dfPercentMatched;
                        nMostMatches = nMatchedKeywords;
                    }
                }

                if (findCommand) {
                    // RE matching
                    ArrayList<Pattern> pats =
                            getPatterns(mAdv, topic.mKeywords.trim()
                                    .replace("?", "\\?"), cmdOrSubject);
                    for (Pattern pat : pats) {
                        Matcher m = (pat != null) ? pat.matcher(cmdOrSubject) : null;
                        if (m != null && m.find()) {
                            if (topic.mRestrictions.passes(mAdv.mReferences)) {
                                for (int i = 0; i <= 5; i++) {
                                    String refText = "%text" +
                                            (i > 0 ? String.valueOf(i) : "") + "%";
                                    if (topic.mKeywords.contains(refText)) {
                                        // Needs full parsing really...
                                        mAdv.mReferencedText[i] =
                                                m.group("text").trim();
                                    }
                                }
                                return topic;
                            }
                        }
                    }
                }

                if (!findAsk && !findTell && !findCommand) {
                    // No matching whatsoever
                    if (topic.mRestrictions.passes(mAdv.mReferences)) {
                        return topic;
                    }
                }

            }
        }

        return bestTopic;
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getName();
    }

    public boolean getIntroduced() {
        return mIntroduced;
    }

    public void setIntroduced(boolean value) {
        mIntroduced = value;
    }

    @NonNull
    private String getWalkTo() {
        return mWalkTo;
    }

    void setWalkTo(@NonNull String value) {
        mWalkTo = value;
    }

    public void doWalk() throws InterruptedException {
        if (!getWalkTo().equals("")) {
            if (getLocation().getLocationKey().equals(mLastPosition)) {
                // Something has stopped us moving, so bomb out the walk
                setWalkTo("");
                return;
            }
            WalkNode node = dijkstra(getLocation().getLocationKey(), getWalkTo());
            if (node != null) {
                while (node != null && node.mDistance > 1) {
                    node = node.mPrevious;
                }
                if (node != null && this == mAdv.getPlayer()) {
                    for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
                        if (mAdv.mLocations.get(getLocation().getLocationKey()).mDirections.get(d).mLocationKey.equals(node.mKey)) {
                            if (node.mKey.equals(getWalkTo())) {
                                setWalkTo("");
                            }
                            mAdv.mView.out("> " + d.toString() + "\n");
                            mLastPosition = getLocation().getLocationKey();
                            MAdventure[] adv1 = new MAdventure[1];
                            adv1[0] = mAdv;
                            // N.B. we don't check whether the
                            // adventure object stored in adv1 changes after
                            // submitCommand as we are assuming that d.toString
                            // doesn't evaluate to "restart"...
                            mAdv.mView.TODO("MCharacter: doWalk: dijkstra submit command");
                            //mAdv.mView.submitCommand(adv1, d.toString());
                            mLastPosition = "";
                            return;
                        }
                    }
                }
            } else {
                setWalkTo("");
            }
        }
    }

    // Adapted from http://en.wikipedia.org/wiki/Dijkstra's_algorithm
    @Nullable
    private WalkNode dijkstra(@NonNull String fromKey, @NonNull String toKey) {
        HashMap<String, WalkNode> walkNodes = new HashMap<>();
        ArrayList<WalkNode> Q = new ArrayList<>();

        for (String locKey : mAdv.mLocations.keySet()) {
            WalkNode node = new WalkNode();
            node.mKey = locKey;
            node.mDistance = Integer.MAX_VALUE;   // Unknown distance function from source to v
            node.mPrevious = null;                // Previous node in optimal path from source
            walkNodes.put(locKey, node);
            Q.add(node);
        }

        walkNodes.get(fromKey).mDistance = 0;    // Distance from source to source
        Collections.sort(Q);

        while (Q.size() > 0) {
            WalkNode u = Q.get(0);               // vertex in Q with smallest distance
            if (u.mDistance == Integer.MAX_VALUE) {
                // all remaining vertices are inaccessible from source
                break;
            }
            if (u.mKey.equals(toKey)) {
                return u;
            }
            Q.remove(u);

            for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
                if (hasRouteInDirection(d, false, u.mKey)) {
                    String destKey = mAdv.mLocations.get(u.mKey).mDirections.get(d).mLocationKey;
                    if (mAdv.mLocations.get(destKey).getSeenBy(getKey())) {
                        int alt = u.mDistance + 1;
                        if (alt < walkNodes.get(destKey).mDistance) {
                            walkNodes.get(destKey).mDistance = alt;
                            walkNodes.get(destKey).mPrevious = u;
                            Collections.sort(Q);
                        }
                    }
                }
            }
        }
        return null;
    }

    public MGlobals.MPerspectiveEnum getPerspective() {
        return (mType == Player) ? mPerspective : ThirdPerson;
    }

    public void setPerspective(MGlobals.MPerspectiveEnum value) {
        mPerspective = value;
    }

    @NonNull
    String getLastParent() {
        return mLastParent;
    }

    void setLastParent(@NonNull String value) {
        mLastParent = value;
    }

    @NonNull
    @Override
    public String getParent() {
        return getLocation().getKey();
    }

    public Gender getGender() {
        // Return eGender
        switch (getPropertyValue(PKEY_GENDER)) {
            case PVAL_MALE:
                return Male;
            case PVAL_FEMALE:
                return Female;
            default:
                return Unknown;
        }
    }

    public void setGender(Gender value) {
        setPropertyValue(PKEY_GENDER, value.toString());
    }

    public boolean isKnown() {
        return mKnown;
    }

    public void setKnown(boolean value) {
        if (value != mKnown) {
            setPropertyValue(PKEY_KNOWN, value);
            mKnown = value;
        }
    }

    public CharacterType getCharacterType() {
        return mType;
    }

    public void setCharacterType(CharacterType value) {
        mType = value;
    }

    public boolean hasSeenLocation(@NonNull String locKey) {
        Boolean ret = mSeenLocations.get(locKey);
        return (ret != null) ? ret : false;
    }

    public void setHasSeenLocation(@NonNull String locKey, boolean value) {
        mSeenLocations.put(locKey, value);

        // TODO - implement and uncomment the following:
      /*  if (this == mAdv.getPlayer()) {
            for (MMapPage p : mAdv.MMap.Pages.values()) {
                for (MMapNode n : p.Nodes) {
                    if (n.getKey().equals(locKey)) {
                        n.Seen = value;
                        return;
                    }
                }
            }
        } */
    }

    public boolean hasSeenObject(@NonNull String obKey) {
        Boolean ret = mSeenObjects.get(obKey);
        return (ret != null) ? ret : false;
    }

    public void setHasSeenObject(@NonNull String obKey, boolean value) {
        mSeenObjects.put(obKey, value);
    }

    public boolean hasSeenCharacter(@NonNull String charKey) {
        Boolean ret = mSeenChars.get(charKey);
        return (ret != null) ? ret : false;
    }

    public void setHasSeenCharacter(@NonNull String charKey, boolean value) {
        mSeenChars.put(charKey, value);
    }

    public boolean hasBeenSeenBy(@NonNull String charKey) {
        return mAdv.mCharacters.get(charKey).hasSeenCharacter(getKey());
    }

    public void setSeenBy(@NonNull String charKey, boolean value) {
        mAdv.mCharacters.get(charKey).setHasSeenCharacter(getKey(), value);
    }

    @NonNull
    String getAloneWithChar() {
        int nChars = 0;
        String chKey = "";
        String locKey = getLocation().getLocationKey();
        for (MCharacter ch : mAdv.mCharacters.values()) {
            if (ch != this) {
                if (locKey.equals(ch.getLocation().getLocationKey())) {
                    nChars++;
                    if (nChars > 1) {
                        return "";
                    }
                    chKey = ch.getKey();
                }
            }
        }
        return (nChars == 1) ? chKey : "";
    }

    @NonNull
    public String getName() {
        return getName(Subjective, true, true,
                Indefinite, false, false);
    }

    @NonNull
    public String getName(MAdventure.MPronounEnum pronoun) {
        return getName(pronoun, true, true,
                Indefinite, false, false);
    }

    @NonNull
    public String getName(MAdventure.MPronounEnum pronoun, boolean bMarkAsSeen) {
        return getName(pronoun, bMarkAsSeen, true,
                Indefinite, false, false);
    }

    @NonNull
    public String getName(MAdventure.MPronounEnum pronoun, boolean bMarkAsSeen,
                          boolean bAllowPronouns) {
        return getName(pronoun, bMarkAsSeen, bAllowPronouns,
                Indefinite, false, false);
    }

    @NonNull
    public String getName(MAdventure.MPronounEnum pronoun, boolean bMarkAsSeen,
                          boolean bAllowPronouns, MGlobals.ArticleTypeEnum article) {
        return getName(pronoun, bMarkAsSeen, bAllowPronouns,
                article, false, false);
    }

    @NonNull
    public String getName(MAdventure.MPronounEnum pronoun, boolean bMarkAsSeen,
                          boolean bAllowPronouns, MGlobals.ArticleTypeEnum article,
                          boolean bForcePronoun) {
        return getName(pronoun, bMarkAsSeen, bAllowPronouns,
                article, bForcePronoun, false);
    }

    @NonNull
    public String getName(MAdventure.MPronounEnum pronoun, boolean bMarkAsSeen,
                          boolean bAllowPronouns, MGlobals.ArticleTypeEnum article,
                          boolean bForcePronoun, boolean bExplicitArticle) {
        // ExplicitPronoun - The user has said they want it to be this pronoun, so don't auto-switch it
        boolean bReplaceWithPronoun = bForcePronoun;
        final Gender gender = getGender();
        final MGlobals.MPerspectiveEnum persp = getPerspective();

        if (mAdv.mView.mDisplaying) {
            if (!bExplicitArticle && getIntroduced() &&
                    article == Indefinite) {
                article = Definite;
            }
            if (bMarkAsSeen) {
                setIntroduced(true);
                if (!mAdv.mCharsMentionedThisTurn.get(gender).contains(getKey())) {
                    mAdv.mCharsMentionedThisTurn.get(gender).add(getKey());
                }
            }

            // If we already have mentioned this key, and it's the most
            // recent key, we can replace with pronoun
            if (persp == FirstPerson || persp == SecondPerson) {
                bReplaceWithPronoun = true;
            }

            if (mAdv.mPronounKeys.size() > 0) {
                MAdventure.MPronounInfo oPreviousPronoun = null;
                for (int i = mAdv.mPronounKeys.size() - 1; i >= 0; i--) {
                    MAdventure.MPronounInfo info = mAdv.mPronounKeys.get(i);
                    if (info.mGender == gender && info.mKey.equals(getKey())) {
                        oPreviousPronoun = info;
                        break;
                    }
                }
                if (oPreviousPronoun != null) {
                    bReplaceWithPronoun = true;
                    if (oPreviousPronoun.mPronoun == Subjective && pronoun == Objective) {
                        pronoun = Reflective;
                    }
                }
            }
        }

        if (pronoun == None) {
            bReplaceWithPronoun = false;
        }

        if (!bAllowPronouns || !bReplaceWithPronoun) {
            // Display the actual name/descriptor
            String name;
            if (hasProperty(PKEY_KNOWN)) {
                name = getProperty(PKEY_KNOWN).getSelected() ?
                        getProperName() : getDescriptor(article);
            } else {
                name = !getDescriptor().equals("") ?
                        getDescriptor(article) : getProperName();
            }
            return (pronoun == Possessive) ?
                    (name + "'s") : name;
        } else {
            // Display the pronoun
            switch (persp) {
                case FirstPerson:
                    switch (pronoun) {
                        case Objective:
                            return "me";
                        case Possessive:
                            return "my"; // "mine"
                        case Reflective:
                            return "myself";
                        case Subjective:
                            return "I";
                    }
                    break;
                case SecondPerson:
                    switch (pronoun) {
                        case Objective:
                            return "you";
                        case Possessive:
                            return "your"; // "yours"
                        case Reflective:
                            return "yourself";
                        case Subjective:
                            return "you";
                    }
                    break;
                case ThirdPerson:
                    switch (pronoun) {
                        case Objective:
                            switch (gender) {
                                case Male:
                                    return "him";
                                case Female:
                                    return "her";
                                case Unknown:
                                    return "it";
                            }
                            break;
                        case Possessive:
                            switch (gender) {
                                case Male:
                                    return "his";
                                case Female:
                                    return "her"; // "hers"
                                case Unknown:
                                    return "its";
                            }
                            break;
                        case Reflective:
                            switch (gender) {
                                case Male:
                                    return "himself";
                                case Female:
                                    return "herself";
                                case Unknown:
                                    return "itself";
                            }
                            break;
                        case Subjective:
                            switch (gender) {
                                case Male:
                                    return "he";
                                case Female:
                                    return "she";
                                case Unknown:
                                    return "it";
                            }
                            break;
                    }
                    break;
            }
        }

        return "";
    }

    /**
     * The Proper Name is the actual name of the character. This name is
     * displayed to the player when the character is in the same
     * location, and is entered by the player when they want to refer to the character.
     * <p>
     * This is only used if this character has the "Known to player" property.
     * <p>
     * If not, then the Article, Prefix/Adjective and Descriptor/Noun are used
     * to describe what the character looks like, and can be entered by the player to refer to the character.
     * <p>
     * More than one noun can be entered, on different lines, and the
     * player can use any of them to refer to the character.
     * <p>
     * The adjectives are optional, except where two characters in the same location
     * use the same noun and it is necessary to distinguish between them.
     * <p>
     * If the character is described as "a tall thin chinese man" then the player
     * can enter any combination of adjectives, such as "tall chinese man", "thin man", "tall thin man" etc.
     * <p>
     * A character can start out as unknown to the player but later learn their
     * name. In this case you would use a task to set the "Known to player" property when they learn their name.
     *
     * @return the proper name of the character.
     */
    @NonNull
    public String getProperName() {
        return mProperName.equals("") ? "Anonymous" : mProperName;
    }

    public void setProperName(@NonNull String value) {
        mProperName = value;
    }

    @NonNull
    public String getDescriptor() {
        return getDescriptor(Indefinite);
    }

    @NonNull
    private String getDescriptor(MGlobals.ArticleTypeEnum article) {
        if (mDescriptors.size() > 0) {
            StringBuilder ret = new StringBuilder();
            // Append article.
            if (!mArticle.equals("")) {
                switch (article) {
                    case Definite:
                        ret.append("the ");
                        break;
                    case Indefinite:
                        ret.append(mArticle).append(" ");
                        break;
                    case None:
                    default:
                        break;
                }
            }
            // Append prefix.
            if (!mPrefix.equals("")) {
                ret.append(mPrefix).append(" ");
            }
            // Append first name and return.
            return ret.append(mDescriptors.get(0)).toString();
        } else {
            return "";
        }
    }

    public void moveTo(@NonNull String dest) {
        if (mAdv.mLocations.containsKey(dest)) {
            MCharacterLocation loc = new MCharacterLocation(mAdv, this);
            loc.setExistsWhere(AtLocation);
            loc.setKey(dest);
            moveTo(loc);
        } else if (dest.equals(HIDDEN)) {
            MCharacterLocation loc = new MCharacterLocation(mAdv, this);
            loc.setExistsWhere(Hidden);
            loc.setKey("");
            moveTo(loc);
        }
    }

    public void moveTo(@NonNull MCharacterLocation dest) {
        if (this == mAdv.getPlayer()) {
            // Add system tasks that are triggered by moving to the new location
            // that are not repeatable or haven't already been completed, for
            // execution later - need to be added in order of task priority
            String destLocKey = dest.getLocationKey();
            String playerLocKey = mAdv.getPlayer().getLocation().getLocationKey();
            if (!playerLocKey.equals(destLocKey)) {
                for (MTask t : mAdv.getTaskList(SystemTasks).values()) {
                    if ((t.getRepeatable() || !t.getCompleted()) &&
                            t.getLocationTrigger().equals(destLocKey)) {
                        // Ok, we need to trigger this task
                        mAdv.mTasksToRun.add(t);
                    }
                }
            }
        }

        // Some pre-version 5 games try to move a character onto or into something
        // they are holding (e.g. Professor Von Witt's Flying Machine and the leg
        // to stand on in the library). Unfortunately these games typically also have a
        // restriction on those tasks requiring that the character is holding the
        // object already. So let's try to fix such situations here (by dropping the
        // object just before attempting to move the character).
        if (mAdv.mVersion < 5 &&
                (dest.getExistsWhere() == InObject ||
                        dest.getExistsWhere() == OnObject)) {
            String destKey = dest.getKey();
            if (isHoldingObject(destKey)) {
                MObject ob = mAdv.mObjects.get(destKey);
                mAdv.mView.displayError("Trying to move character " + getName() +
                        " onto or into an object they are holding, " +
                        ob.getFullName());

                // Ok, try to fix the situation by dropping the object (moving it
                // into the same location as the character).
                mAdv.mView.displayText(mAdv, "(Dropping " + ob.getFullName() + ")<br>");
                MObject.MObjectLocation newLoc = new MObject.MObjectLocation(mAdv);
                newLoc.mDynamicExistWhere = InLocation;
                newLoc.setKey(getLocation().getKey());
                ob.moveTo(newLoc);
            }
        }

        // Ensure that character Key is not this character, or on this character
        setLocation(dest);

        // Update any 'seen' things
        if (dest.getExistsWhere() == AtLocation) {
            getLocation().mLastLocKey = dest.getKey();
            mSeenLocations.put(dest.getKey(), true);
            if (mAdv.mLocations.containsKey(dest.getKey())) {
                for (MObject ob : mAdv.mLocations.get(dest.getKey()).getObjectsInLocation(AllObjects, true).values()) {
                    mSeenObjects.put(ob.getKey(), true);
                }
                for (MCharacter ch : mAdv.mLocations.get(dest.getKey()).getCharactersVisibleAtLocation().values()) {
                    mSeenChars.put(ch.getKey(), true);
                }
            }
        }

        // Exit any current conversations and trigger farewell remarks, if relevant
        if (!mAdv.mConversationCharKey.equals("")) {
            if (!mAdv.mCharacters.get(mAdv.mConversationCharKey).getLocation().getKey().equals(mAdv.getPlayer().getLocation().getKey())) {
                if (getKey().equals(mAdv.getPlayer().getKey())) {
                    MCharacter ch = mAdv.mCharacters.get(mAdv.mConversationCharKey);
                    MTopic farewell = ch.findConversationNode(EnumSet.of(Farewell), "");
                    if (farewell != null) {
                        mAdv.mView.displayText(mAdv, farewell.mDescription.toString());
                    }
                }
                mAdv.mConversationCharKey = "";
                mAdv.mConversationNode = "";
            }
        }

        if (this == mAdv.getPlayer()) {
           /* mAdv.MMap.RefreshNode(mAdv.getPlayer().getLocation().LocationKey());
            MUserSession.MMap.SelectNode(mAdv.getPlayer().getLocation().LocationKey()); */
            if (MDebugger.BEBEK_DEBUG_ENABLED) {
                GLKLogger.error("TODO: MCharacter: Move - refresh and select map nodes");
            }
        }
    }

    @NonNull
    public MCharacterLocation getLocation() {
        return mLocation;
    }

    public void setLocation(@NonNull MCharacterLocation value) {
        mLastLocation = mLocation;
        mLocation = value;
    }

    @Nullable
    MCharacterLocation getLastLocation() {
        return mLastLocation;
    }

    @NonNull
    public String getArticle() {
        return mArticle;
    }

    public void setArticle(@NonNull String value) {
        mArticle = value;
    }

    @NonNull
    public String getPrefix() {
        return mPrefix;
    }

    public void setPrefix(@NonNull String value) {
        mPrefix = value;
    }

    @NonNull
    public MDescription getDescription() {
        if (mDescription == null) {
            mDescription = new MDescription(mAdv);
        }
        return mDescription;
    }

    public void setDescription(@NonNull MDescription value) {
        mDescription = value;
    }

    @NonNull
    String getIsHereDesc() {
        return getPropertyValue(PKEY_CHAR_HERE_DESC);
    }

    @Nullable
    @Override
    public MItem clone() {
        MCharacter ch = (MCharacter) super.clone();
        if (ch != null) {
            ch.setLocalProperties(ch.getLocalProperties().clone());
            ch.mDescriptors = ch.mDescriptors.clone();
        }
        return ch;
    }

    @NonNull
    MObjectHashMap getChildObjects(boolean bRecursive) {
        MObjectHashMap ret = new MObjectHashMap(mAdv);
        for (MObject ob : mAdv.mObjects.values()) {
            MObject.MObjectLocation obLoc = ob.getLocation();
            if (!ob.isStatic()) {
                if (obLoc.mDynamicExistWhere == HeldByCharacter ||
                        obLoc.mDynamicExistWhere == WornByCharacter) {
                    if (obLoc.getKey().equals(getKey())) {
                        ret.put(ob.getKey(), ob);
                        if (bRecursive) {
                            for (MObject obChild :
                                    ob.getChildObjects(InsideOrOnObject, true).values()) {
                                ret.put(obChild.getKey(), obChild);
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    private boolean hasRouteInDirection(MAdventure.DirectionsEnum drn, boolean bIgnoreRestrictions) {
        return hasRouteInDirection(drn, bIgnoreRestrictions, "", new StringBuilder());
    }

    private boolean hasRouteInDirection(MAdventure.DirectionsEnum drn, boolean bIgnoreRestrictions,
                                        @NonNull String fromLoc) {
        return hasRouteInDirection(drn, bIgnoreRestrictions, fromLoc, new StringBuilder());
    }

    boolean hasRouteInDirection(MAdventure.DirectionsEnum drn, boolean bIgnoreRestrictions,
                                @NonNull String fromLoc, @NonNull StringBuilder errMsg) {
        if (fromLoc.equals("")) {
            fromLoc = getLocation().getLocationKey();
        }
        if (!mAdv.mLocations.containsKey(fromLoc)) {
            return false;
        }

        MLocation.MDirection d = mAdv.mLocations.get(fromLoc).mDirections.get(drn);
        if (!d.mLocationKey.equals("")) {
            if (bIgnoreRestrictions) {
                return true;
            } else {
                String route = fromLoc + drn.toString();
                boolean bResult;
                if (mValidRouteCache.containsKey(route)) {
                    bResult = mValidRouteCache.get(route);
                    if (!bResult) {
                        errMsg.setLength(0);
                        errMsg.append(mRouteErrors.get(route));
                    }
                    return bResult;
                }
                // evaluate direction restrictions
                bResult = d.mRestrictions.passes(mAdv.mReferences);
                if (!bResult) {
                    errMsg.setLength(0);
                    errMsg.append(mAdv.mRestrictionText);
                }
                mValidRouteCache.put(route, bResult);
                mRouteErrors.put(route, errMsg.toString());
                if (!bResult) {
                    d.mEverBeenBlocked = true;
                }
                return bResult;
            }
        } else {
            return false;
        }
    }

    boolean isInGroupOrLocation(@NonNull String grpOrLocKey) {
        if (grpOrLocKey.equals("")) {
            // interpret this as "everything" - which according to the ADRIFT
            // manual ("Events") is the default
            return true;
        } else if (mAdv.mLocations.containsKey(grpOrLocKey)) {
            return getLocation().getLocationKey().equals(grpOrLocKey);
        } else if (mAdv.mGroups.containsKey(grpOrLocKey)) {
            return mAdv.mGroups.get(grpOrLocKey).getArlMembers().contains(getLocation().getLocationKey());
        }
        return false;
    }

    @NonNull
    String getBoundVisible() throws Exception {
        String locKey = getLocation().getKey();
        switch (getLocation().getExistsWhere()) {
            case Uninitialised:
            case Hidden:
            default:
                // Character is not visible in
                // any location.
                return HIDDEN;

            case AtLocation:
                // Character is visible at a
                // single location.
                return locKey;

            case OnCharacter:
                // Character is visible wherever the character
                // they are on is visible.
                return mAdv.mCharacters.get(locKey).getBoundVisible();

            case InObject:
                // Character is visible wherever the object
                // containing them is visible IF that container
                // is either NOT openable (e.g. a bookcase) OR
                // currently opened OR transparent. Otherwise
                // Character is only visible to characters that
                // are also inside that same object.
                MObject ob = mAdv.mObjects.get(locKey);
                if (!ob.isOpenable() || ob.isOpen() || ob.isTransparent()) {
                    return ob.getBoundVisible();
                } else {
                    return ob.getKey();
                }

            case OnObject:
                // Character is visible wherever the object they
                // are on is visible.
                return mAdv.mObjects.get(locKey).getBoundVisible();
        }
    }

    public boolean canSeeCharacter(@NonNull String chKey) {
        return isVisibleTo(chKey);
    }

    private boolean isVisibleTo(@NonNull String chKey) {
        if (chKey.equals(MGlobals.THEPLAYER)) {
            chKey = mAdv.getPlayer().getKey();
        }
        try {
            String myBoundVisible = getBoundVisible();
            if (myBoundVisible.equals(HIDDEN)) {
                return false;
            }
            String chBoundVisible = mAdv.mCharacters.get(chKey).getBoundVisible();
            return myBoundVisible.equals(chBoundVisible);
        } catch (Exception e) {
            // The game has placed this character in an illogical state, e.g.
            // the character is on an object that is inside itself. Try to help
            // the user to recover by at least allowing them to see the character.
            GLKLogger.error("character isVisibleTo exception: " + e.getMessage());
            return true;
        }
    }

    boolean isVisibleAtLocation(@NonNull String locKey) {
        try {
            String myBoundVisible = getBoundVisible();
            if (myBoundVisible.equals(HIDDEN)) {
                return false;
            }
            switch (myBoundVisible) {
                case ALLROOMS:
                    return mAdv.mLocations.containsKey(locKey);
                default:
                    MGroup grp = mAdv.mGroups.get(myBoundVisible);
                    return (grp != null) ?
                            grp.getArlMembers().contains(locKey) :
                            myBoundVisible.equals(locKey);
            }
        } catch (Exception e) {
            // The game has placed this character in an illogical state, e.g.
            // the character is on an object that is inside itself. Try to help
            // the user to recover by at least allowing them to see the character.
            GLKLogger.error("character isVisibleAt exception: " + e.getMessage());
            return true;
        }
    }

    public boolean canSeeObject(@NonNull String obKey) {
        try {
            String myBoundVisible = getBoundVisible();
            if (myBoundVisible.equals(HIDDEN)) {
                return false;
            }

            MGroup grp = mAdv.mGroups.get(obKey);
            if (grp != null) {
                for (String key : grp.getArlMembers()) {
                    if (canSeeObject(key)) {
                        return true;
                    }
                }
                return false;
            }

            String obBoundVisible = mAdv.mObjects.get(obKey).getBoundVisible();
            grp = mAdv.mGroups.get(obBoundVisible);
            if (grp != null) {
                return grp.getArlMembers().contains(myBoundVisible);
            } else {
                // Allow us to see the object we're in, otherwise we can't do
                // anything with it (open it again!)
                return myBoundVisible.equals(obBoundVisible) || myBoundVisible.equals(obKey);
            }
        } catch (Exception e) {
            // The game has placed this character in an illogical state, e.g.
            // the character is on an object that is inside itself. Try to help
            // the user to recover by at least allowing them to see the character.
            GLKLogger.error("character canSeeObject exception: " + e.getMessage());
            return true;
        }
    }

    boolean isWearingObject(@NonNull String obKey) {
        return isWearingObject(obKey, true);
    }

    private boolean isWearingObject(@NonNull String obKey, boolean directly) {
        if (obKey.equals(NOOBJECT)) {
            return getWornObjects().size() == 0;
        }
        if (obKey.equals(ANYOBJECT)) {
            return getWornObjects().size() > 0;
        }
        if (obKey.equals("")) {
            // TCC - added to handle any buggy games that test if character is wearing
            // an object with an empty key.
            mAdv.mView.displayError("Bad argument to isWearingObject - no object key specified!");
            return true;
        }

        MObject.MObjectLocation obLoc = mAdv.mObjects.get(obKey).getLocation();
        switch (obLoc.mDynamicExistWhere) {
            case WornByCharacter:
                String obLocKey = obLoc.getKey();
                if (obLocKey.equals(getKey()) ||
                        (obLocKey.equals(THEPLAYER) && mType == Player)) {
                    return true;
                }
                break;
            case InObject:
            case OnObject:
                return !directly && isWearingObject(mAdv.mObjects.get(obKey).getParent());
        }
        return false;
    }


    @Override
    @NonNull
    protected String getRegEx(boolean getADRIFTExpr,
                              boolean usePluralForm) {
        return getRegEx(getADRIFTExpr);
    }

    @NonNull
    private String getRegEx(boolean getADRIFTExpr) {
        MStringArrayList names = mDescriptors;
        if (getADRIFTExpr) {
            // Create an ADRIFT 'advanced command construction' expression.
            StringBuilder ret = new StringBuilder("{" + mArticle + "/the} ");
            if (!mPrefix.equals("")) {
                for (String prefix : mPrefix.split(" ")) {
                    if (!prefix.equals("")) {
                        ret.append("{").append(prefix.toLowerCase()).append("} ");
                    }
                }
            }
            ret.append("[");
            for (String name : names) {
                ret.append(name.toLowerCase()).append("/");
            }
            if (!mAdv.mCharacterProperties.containsKey(PKEY_KNOWN) ||
                    hasProperty(PKEY_KNOWN) || names.size() == 0) {
                ret.append(getProperName().toLowerCase()).append("/");
            }
            return ret.substring(0, ret.length() - 1) + "]";
        } else {
            // Create a real regular expression.
            StringBuilder ret = new StringBuilder("(" + mArticle + " |the )?");
            for (String prefix : mPrefix.split(" ")) {
                if (!prefix.equals("")) {
                    ret.append("(").append(quote(prefix.toLowerCase())).append(" )?");
                }
            }
            ret.append("(");
            for (String name : names) {
                ret.append(quote(name.toLowerCase())).append("|");
            }
            if (names.size() == 0) {
                // Fudge
                ret.append("|");
            }
            if (!mAdv.mCharacterProperties.containsKey(PKEY_KNOWN) ||
                    hasProperty(PKEY_KNOWN) || names.size() == 0) {
                ret.append(quote(getProperName().toLowerCase())).append("|");
            }
            return ret.substring(0, ret.length() - 1) + ")";
        }
    }

    /**
     * Is this character directly on the specified object
     * or character, or on/in something that is inside it?
     *
     * @param obOrChKey - the obOrChKey of an object or character.
     * @return TRUE if the character is on the given entity,
     * or on/in something that is inside it. FALSE otherwise.
     */
    public boolean isOn(@NonNull String obOrChKey) {
        String myParent = getParent();
        MCharacterLocation.ExistsWhere where = getLocation().getExistsWhere();
        if ((where == OnObject || where == InObject ||
                where == OnCharacter) && !myParent.equals("")) {
            if ((where == OnObject || where == OnCharacter) &&
                    myParent.equals(obOrChKey)) {
                return true;
            } else {
                if (mAdv.mObjects.containsKey(myParent)) {
                    MObject parentOb = mAdv.mObjects.get(myParent);
                    return parentOb.isOn(obOrChKey);
                } else if (mAdv.mCharacters.containsKey(myParent)) {
                    MCharacter parentCh = mAdv.mCharacters.get(myParent);
                    return parentCh.isOn(obOrChKey);
                }
            }
        } else {
            return false;
        }
        return false;
    }

    boolean isHoldingObject(@NonNull String obKey) {
        return isHoldingObject(obKey, false);
    }

    boolean isHoldingObject(@NonNull String obKey, boolean directly) {
        if (obKey.equals(NOOBJECT)) {
            return getHeldObjects().size() == 0;
        }
        if (obKey.equals(ANYOBJECT)) {
            return getHeldObjects().size() > 0;
        }
        if (obKey.equals("")) {
            // TCC - some buggy games test whether the character is holding
            // an object referred to by an empty key. To avoid any crashes
            // we'll always return true to allow such restrictions to pass.
            mAdv.mView.displayError("Bad argument to " +
                    "isHoldingObject - no object key specified!");
            return true;
        }

        MObject.MObjectLocation obLoc = mAdv.mObjects.get(obKey).getLocation();
        switch (obLoc.mDynamicExistWhere) {
            case HeldByCharacter:
                if (obLoc.getKey().equals(getKey()) ||
                        (obLoc.getKey().equals(THEPLAYER) && mType == Player)) {
                    return true;
                }
                break;
            case InObject:
            case OnObject:
                return !directly && isHoldingObject(obLoc.getKey());
        }
        return false;
    }

    public boolean isAlone() {
        String locKey = getLocation().getLocationKey();
        for (MCharacter ch : mAdv.mCharacters.values()) {
            if (!ch.getKey().equals(getKey()) &&
                    ch.getLocation().getLocationKey().equals(locKey)) {
                return false;
            }
        }
        return true;
    }

    @NonNull
    MCharacterHashMap getChildChars(boolean recursive) {
        MCharacterHashMap ret = new MCharacterHashMap(mAdv);
        for (MCharacter ch : mAdv.mCharacters.values()) {
            MCharacterLocation chLoc = ch.getLocation();
            if (chLoc.getExistsWhere() == OnCharacter) {
                if (chLoc.getKey().equals(getKey())) {
                    ret.put(ch.getKey(), ch);
                    if (recursive) {
                        for (MCharacter childCh :
                                ch.getChildChars(true).values()) {
                            ret.put(childCh.getKey(), childCh);
                        }
                    }
                }
            }
        }

        if (recursive) {
            for (MObject ob : mAdv.mObjects.values()) {
                MObject.MObjectLocation obLoc = ob.getLocation();
                if (ob.isStatic()) {
                    switch (obLoc.mStaticExistWhere) {
                        case PartOfCharacter:
                            if (obLoc.getKey().equals(getKey())) {
                                for (MCharacter childCh :
                                        ob.getChildChars(InsideOrOnObject).values()) {
                                    ret.put(childCh.getKey(), childCh);
                                }
                            }
                            break;
                    }
                } else {
                    switch (obLoc.mDynamicExistWhere) {
                        case HeldByCharacter:
                        case WornByCharacter:
                            if (obLoc.getKey().equals(getKey())) {
                                for (MCharacter childCh :
                                        ob.getChildChars(InsideOrOnObject).values()) {
                                    ret.put(childCh.getKey(), childCh);
                                }
                            }
                            break;
                    }
                }
            }
        }
        return ret;
    }

    @NonNull
    public MObjectHashMap getHeldObjects() {
        return getHeldObjects(false);
    }

    @NonNull
    MObjectHashMap getHeldObjects(boolean recursive) {
        MObjectHashMap ret = new MObjectHashMap(mAdv);
        for (MObject ob : mAdv.mObjects.values()) {
            MObject.MObjectLocation obLoc = ob.getLocation();
            if (obLoc.mDynamicExistWhere == HeldByCharacter) {
                if (obLoc.getKey().equals(getKey()) ||
                        (obLoc.getKey().equals(THEPLAYER) &&
                                mType == Player)) {
                    ret.put(ob.getKey(), ob);
                    if (recursive) {
                        for (MObject childOb :
                                ob.getChildObjects(InsideOrOnObject, true).values()) {
                            ret.put(childOb.getKey(), childOb);
                        }
                    }
                }
            }
        }
        return ret;
    }

    @NonNull
    public MObjectHashMap getWornObjects() {
        return getWornObjects(false);
    }

    @NonNull
    private MObjectHashMap getWornObjects(boolean recursive) {
        MObjectHashMap ret = new MObjectHashMap(mAdv);
        for (MObject ob : mAdv.mObjects.values()) {
            MObject.MObjectLocation obLoc = ob.getLocation();
            if (obLoc.mDynamicExistWhere == WornByCharacter) {
                if (obLoc.getKey().equals(getKey()) ||
                        (obLoc.getKey().equals(THEPLAYER) &&
                                mType == Player)) {
                    ret.put(ob.getKey(), ob);
                    if (recursive) {
                        for (MObject childOb :
                                ob.getChildObjects(InsideOrOnObject, true).values()) {
                            ret.put(childOb.getKey(), childOb);
                        }
                    }
                }
            }
        }
        return ret;
    }

    @NonNull
    String listExits() {
        return listExits("", new int[1]);
    }

    @NonNull
    String listExits(@NonNull String fromLoc, @NonNull int[] exitCount) {
        StringBuilder ret = new StringBuilder();
        if (fromLoc.equals("")) {
            fromLoc = mAdv.getPlayer().getLocation().getLocationKey();
        }
        for (MAdventure.DirectionsEnum d : MAdventure.DirectionsEnum.values()) {
            if (hasRouteInDirection(d, false, fromLoc)) {
                ret.append(mAdv.getDirectionName(d)).append(", ");
                exitCount[0]++;
            }
        }
        if (ret.toString().endsWith(", ")) {
            ret = new StringBuilder(ret.substring(0, ret.length() - 2));
        }
        if (exitCount[0] > 1) {
            ret = new StringBuilder(ret.substring(0, ret.lastIndexOf(", ")) +
                    " and " + ret.substring(ret.lastIndexOf(", ") + 2, ret.length()));
        }
        if (ret.length() == 0) {
            ret.append("nowhere");
        }
        return ret.toString().toLowerCase();
    }

    @Override
    @NonNull
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> ret = new ArrayList<>();
        ret.add(mDescription);
        for (MProperty p : getProperties().values()) {
            ret.add(p.getStringData());
        }
        for (MTopic t : mTopics.values()) {
            for (MRestriction r : t.mRestrictions) {
                ret.add(r.mMessage);
            }
            ret.add(t.mDescription);
        }
        for (MWalk w : mWalks) {
            for (int i = 0; i < w.mSubWalks.size(); i++) {
                ret.add(w.mSubWalks.get(i).oDescription);
            }
        }
        return ret;
    }

    @Override
    public MGroup.GroupTypeEnum getPropertyGroupType() {
        return Characters;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int ret = 0;
        for (MDescription d : getAllDescriptions()) {
            ret += d.getNumberOfKeyRefs(key);
        }
        for (MWalk w : mWalks) {
            for (MWalk.MStep s : w.mSteps) {
                if (s.mLocation.equals(key)) {
                    ret++;
                }
            }
            for (int i = 0; i < w.mSubWalks.size(); i++) {
                if (w.mSubWalks.get(i).sKey2.equals(key) ||
                        w.mSubWalks.get(i).sKey3.equals(key)) {
                    ret++;
                }
            }
            for (int i = 0; i < w.mWalkControls.size(); i++) {
                if (w.mWalkControls.get(i).mTaskKey.equals(key)) {
                    ret++;
                }
            }
        }
        for (MTopic t : mTopics.values()) {
            ret += t.mRestrictions.getNumberOfKeyRefs(key);
            ret += t.mActions.getNumberOfKeyRefs(key);
        }
        if (getLocation().getKey().equals(key)) {
            ret++;
        }
        ret += getLocalProperties().getNumberOfKeyRefs(key);
        return ret;
    }

    @NonNull
    @Override
    public String getSymbol() {
        // male, female or indeterminate profile
        switch (getGender()) {
            case Male:
                return new String(Character.toChars(0x1F468));
            case Female:
                return new String(Character.toChars(0x1F469));
            default:
                return new String(Character.toChars(0x1F464));
        }
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (MDescription d : getAllDescriptions()) {
            if (!d.deleteKey(key)) {
                return false;
            }
        }

        for (MWalk w : mWalks) {
            for (int i = w.mSteps.size() - 1; i >= 0; i--) {
                if (w.mSteps.get(i).mLocation.equals(key)) {
                    w.mSteps.remove(i);
                }
            }
            for (int i = 0; i < w.mSubWalks.size(); i++) {
                MSubWalk sw = w.mSubWalks.get(i);
                if (sw.sKey2.equals(key)) {
                    sw.sKey2 = "";
                }
                if (sw.sKey3.equals(key)) {
                    sw.sKey3 = "";
                }
            }
            for (int i = 0; i < w.mWalkControls.size(); i++) {
                if (w.mWalkControls.get(i).mTaskKey.equals(key)) {
                    for (int j = w.mWalkControls.size() - 2; j >= i; j--) {
                        w.mWalkControls.set(j, w.mWalkControls.get(j + 1));
                    }
                    //  w.mWalkControls = Arrays.copyOf(w.mWalkControls, w.mWalkControls.size() - 2);
                    GLKLogger.error("FIXME: MCharacter: deleteKey: resize walkcontrols array");
                }
            }
        }

        for (MTopic t : mTopics.values()) {
            if (!t.mRestrictions.deleteKey(key)) {
                return false;
            }
            if (!t.mActions.deleteKey(key)) {
                return false;
            }
        }

        MCharacterLocation chLoc = getLocation();
        if (chLoc.getKey().equals(key)) {
            chLoc.setKey("");
            chLoc.setExistsWhere(Hidden);
        }

        return getLocalProperties().deleteKey(key);
    }

    @Override
    public int findLocal(@NonNull String toFind, @Nullable String toReplace,
                         boolean findAll, @NonNull int[] nReplaced) {
        int count = nReplaced[0];
        String[] t = new String[1];
        t[0] = mProperName;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        mProperName = t[0];
        t[0] = mArticle;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        mArticle = t[0];
        for (int i = mDescriptors.size() - 1; i >= 0; i--) {
            t[0] = mDescriptors.get(i);
            nReplaced[0] += MGlobals.find(t, toFind, toReplace);
            mDescriptors.set(i, t[0]);
        }
        return nReplaced[0] - count;
    }

    public enum CharacterType {
        Player,     // 0
        NonPlayer   // 1
    }

    public enum Gender {
        Male,       // 0
        Female,     // 1
        Unknown     // 2
    }

    public enum ConversationEnum {
        NotSet,                     // 0  (default, added for Bebek)
        Greet,                      // 1
        Ask,                        // 2
        Tell,                       // 4
        Command,                    // 8
        Farewell,                   // 16
        EnterConversation,          // 32
        LeaveConversation           // 64
    }

    public static class MCharacterLocation {
        @NonNull
        private static final String[] EXISTS_WHERE_PROPS =
                {PKEY_AT_LOC, PKEY_CHAR_IN, PKEY_CHAR_ON_WHAT, PKEY_CHAR_ON_WHO};
        @NonNull
        private final MAdventure mAdv;
        @NonNull
        String mLastLocKey = "";
        @NonNull
        private MCharacter mParent;
        private ExistsWhere mExistsWhere = ExistsWhere.Uninitialised;
        private Position mPosition = Uninitialised;
        @NonNull
        private String mKey = "";

        public MCharacterLocation(@NonNull MAdventure adv, @NonNull MCharacter parent) {
            mAdv = adv;
            if (parent.getLocation() != null) {
                mLastLocKey = parent.getLocation().mLastLocKey;
            }
            mParent = parent;
        }

        public static Position MPositionEnumFromInt(int value) {
            switch (value) {
                default:
                case 0:
                    return Uninitialised;
                case 1:
                    return Standing;
                case 2:
                    return Sitting;
                case 3:
                    return Lying;
            }
        }

        public ExistsWhere getExistsWhere() {
            if (mExistsWhere == ExistsWhere.Uninitialised) {
                if (mParent.hasProperty(PKEY_CHAR_LOCATION)) {
                    switch (mParent.getPropertyValue(PKEY_CHAR_LOCATION)) {
                        case PVAL_AT_LOC:
                            mExistsWhere = AtLocation;
                            break;
                        case HIDDEN:
                            mExistsWhere = Hidden;
                            break;
                        case PVAL_IN_OBJ:
                            mExistsWhere = InObject;
                            break;
                        case PVAL_ON_CHAR:
                            mExistsWhere = OnCharacter;
                            break;
                        case PVAL_ON_OBJ:
                            mExistsWhere = OnObject;
                            break;
                        default:
                            // Hmmm....
                            break;
                    }
                } else {
                    mExistsWhere = Hidden;
                }
            }
            return mExistsWhere;
        }

        public void setExistsWhere(ExistsWhere value) {
            if (value != mExistsWhere) {
                MProperty p;

                if (!mParent.hasProperty(PKEY_CHAR_LOCATION)) {
                    p = mAdv.mAllProperties.get(PKEY_CHAR_LOCATION).copy();
                    p.setSelected(true);
                    mParent.addProperty(p);
                }

                String newLoc = "";
                switch (value) {
                    case AtLocation:
                        newLoc = PVAL_AT_LOC;
                        break;
                    case Hidden:
                        newLoc = HIDDEN;
                        break;
                    case InObject:
                        newLoc = PVAL_IN_OBJ;
                        break;
                    case OnCharacter:
                        newLoc = PVAL_ON_CHAR;
                        break;
                    case OnObject:
                        newLoc = PVAL_ON_OBJ;
                        break;
                }
                mParent.setPropertyValue(PKEY_CHAR_LOCATION, newLoc);

                for (String sProp : EXISTS_WHERE_PROPS) {
                    if (mParent.hasProperty(sProp)) {
                        mParent.removeProperty(sProp);
                    }
                }

                if (value != Hidden) {
                    String newProp = "";
                    switch (value) {
                        case AtLocation:
                            newProp = PKEY_AT_LOC;
                            break;
                        case InObject:
                            newProp = PKEY_CHAR_IN;
                            break;
                        case OnCharacter:
                            newProp = PKEY_CHAR_ON_WHO;
                            break;
                        case OnObject:
                            newProp = PKEY_CHAR_ON_WHAT;
                            break;
                    }

                    if (!mParent.hasProperty(newProp) &&
                            mAdv.mAllProperties.containsKey(newProp)) {
                        p = mAdv.mAllProperties.get(newProp).copy();
                        p.setSelected(true);
                        mParent.addProperty(p);
                    }
                }

                mExistsWhere = value;
            }
        }

        @NonNull
        public String getKey() {
            // This could be key of location, object or character
            if (mKey.equals("")) {
                switch (getExistsWhere()) {
                    case AtLocation:
                        if (mParent.hasProperty(PKEY_AT_LOC)) {
                            mKey = mParent.getPropertyValue(PKEY_AT_LOC);
                        }
                        break;
                    case Hidden:
                        mKey = "";
                        break;
                    case InObject:
                        if (mParent.hasProperty(PKEY_CHAR_IN)) {
                            mKey = mParent.getPropertyValue(PKEY_CHAR_IN);
                        }
                        break;
                    case OnCharacter:
                        if (mParent.hasProperty(PKEY_CHAR_ON_WHO)) {
                            mKey = mParent.getPropertyValue(PKEY_CHAR_ON_WHO);
                        }
                        if (mKey.equals(THEPLAYER)) {
                            mKey = mAdv.getPlayer().getKey();
                        }
                        break;
                    case OnObject:
                        if (mParent.hasProperty(PKEY_CHAR_ON_WHAT)) {
                            mKey = mParent.getPropertyValue(PKEY_CHAR_ON_WHAT);
                        }
                        break;
                }
            }
            return mKey;
        }

        public void setKey(@NonNull String value) {
            mKey = value;
            switch (getExistsWhere()) {
                case AtLocation:
                    if (mParent.hasProperty(PKEY_AT_LOC)) {
                        mParent.setPropertyValue(PKEY_AT_LOC, value);
                    }
                    break;
                case InObject:
                    if (mParent.hasProperty(PKEY_CHAR_IN)) {
                        mParent.setPropertyValue(PKEY_CHAR_IN, value);
                    }
                    break;
                case OnCharacter:
                    if (mParent.hasProperty(PKEY_CHAR_ON_WHO)) {
                        mParent.setPropertyValue(PKEY_CHAR_ON_WHO, value);
                    }
                    break;
                case OnObject:
                    if (mParent.hasProperty(PKEY_CHAR_ON_WHAT)) {
                        mParent.setPropertyValue(PKEY_CHAR_ON_WHAT, value);
                    }
                    break;
            }
        }

        @NonNull
        public String getLocationKey() {
            // Returns the key of the location that the character is ultimately in
            String ret = "";
            try {
                switch (getExistsWhere()) {
                    case AtLocation:
                        ret = getKey();
                        break;
                    case Hidden:
                        // Hmm
                        ret = HIDDEN;
                        break;
                    case InObject:
                    case OnObject:
                        MObject ob;
                        if ((ob = mAdv.mObjects.get(getKey())) != null) {
                            if (!mLastLocKey.equals("") &&
                                    ob.getRootLocations().containsKey(mLastLocKey)) {
                                ret = mLastLocKey;
                            } else {
                                // return the first key in the location roots
                                ret = ob.getRootLocations().keySet().iterator().next();
                            }
                        }
                        break;
                    case OnCharacter:
                        ret = mAdv.mCharacters.get(getKey()).getLocation().getLocationKey();
                        break;
                    default:
                        ret = "";
                        break;
                }
            } catch (Exception ex) {
                mAdv.mView.errMsg("LocationKey error", ex);
            } finally {
                if (!ret.equals("") && !ret.equals(mLastLocKey)) {
                    mLastLocKey = ret;
                }
            }
            return ret;
        }

        void resetPosition() {
            mPosition = Uninitialised;
        }

        public Position getPosition() {
            if (mPosition == Uninitialised) {
                if (mParent.hasProperty(PKEY_CHAR_POSITION)) {
                    switch (mParent.getPropertyValue(PKEY_CHAR_POSITION)) {
                        case PVAL_STANDING:
                            mPosition = Standing;
                            break;
                        case PVAL_SITTING:
                            mPosition = Sitting;
                            break;
                        case PVAL_LYING:
                            mPosition = Lying;
                            break;
                        default:
                            // Hmmm....
                    }
                } else {
                    mPosition = Standing;
                }
            }
            return mPosition;
        }

        public void setPosition(Position value) {
            if (value != mPosition) {
                mParent.setPropertyValue(PKEY_CHAR_POSITION, value.toString());
                mPosition = value;
            }
        }

        @Override
        @NonNull
        public String toString() {
            if (getExistsWhere() == Hidden) {
                return HIDDEN;
            }

            StringBuilder ret = new StringBuilder();

            switch (getPosition()) {
                case Standing:
                    ret.append("Standing ");
                    break;
                case Sitting:
                    ret.append("Sitting ");
                    break;
                case Lying:
                    ret.append("Lying ");
                    break;
            }

            switch (getExistsWhere()) {
                case AtLocation:
                    ret.append("at ").append(mAdv.mLocations.get(getKey()).getShortDescription().toString());
                    break;
                case OnObject:
                    ret.append("on ").append(mAdv.mObjects.get(getKey()).getFullName());
                    break;
                case InObject:
                    ret.append("in ").append(mAdv.mObjects.get(getKey()).getFullName());
                    break;
                case OnCharacter:
                    ret.append("on ").append(mAdv.mCharacters.get(getKey()).getName(Subjective, false));
                    break;
            }

            return ret.toString();
        }

        public enum ExistsWhere {
            Uninitialised,      // 0
            Hidden,             // 1  A hidden character is not at any location and cannot be found by the player.
            AtLocation,         // 2  When this is selected the "At which location" property becomes available and you can select which location you want the character to start at.
            OnObject,           // 3  The character can be sitting on a chair or laying on a bed. Only objects with the "supporter" property can be selected.
            InObject,           // 4  You can select from objects that have the "Object is a container" and "characters can go inside this object" properties.
            OnCharacter         // 5  This character is on another character, ie. a parrot on a pirates shoulder, someone sitting on a horse, or just someone getting a piggyback ride.
        }

        public enum Position {
            Uninitialised,      // 0
            Standing,           // 1
            Sitting,            // 2
            Lying               // 3
        }
    }

    private class WalkNode implements Comparable<WalkNode> {
        @NonNull
        String mKey = "";
        @Nullable
        WalkNode mPrevious = null;
        Integer mDistance = Integer.MAX_VALUE;

        @Override
        public int compareTo(@NonNull WalkNode other) {
            return mDistance.compareTo(other.mDistance);
        }
    }
}
