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
import com.luxlunae.bebek.model.collection.MLocationHashMap;
import com.luxlunae.bebek.model.collection.MObjectHashMap;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.io.MFileOlder;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import static com.luxlunae.bebek.MGlobals.ALLROOMS;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Definite;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.Indefinite;
import static com.luxlunae.bebek.MGlobals.ArticleTypeEnum.None;
import static com.luxlunae.bebek.MGlobals.EVERYWHERE;
import static com.luxlunae.bebek.MGlobals.HIDDEN;
import static com.luxlunae.bebek.MGlobals.NOWHERE;
import static com.luxlunae.bebek.MGlobals.OBJECTARTICLE;
import static com.luxlunae.bebek.MGlobals.OBJECTNOUN;
import static com.luxlunae.bebek.MGlobals.OBJECTPREFIX;
import static com.luxlunae.bebek.MGlobals.THEPLAYER;
import static com.luxlunae.bebek.MGlobals.TODO;
import static com.luxlunae.bebek.MGlobals.UNSELECTED;
import static com.luxlunae.bebek.MGlobals.errMsg;
import static com.luxlunae.bebek.MGlobals.guessPluralNoun;
import static com.luxlunae.bebek.MGlobals.safeBool;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.HeldByCharacter;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.Hidden;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.InLocation;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.InObject;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.OnObject;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.DynamicExistsWhereEnum.WornByCharacter;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.AllRooms;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.LocationGroup;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.NoRooms;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.PartOfCharacter;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.PartOfObject;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.StaticExistsWhereEnum.SingleLocation;
import static com.luxlunae.bebek.model.MObject.MObjectLocation.toStaticExistsWhere;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.Everything;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideObject;
import static com.luxlunae.bebek.model.MObject.WhereChildrenEnum.InsideOrOnObject;
import static com.luxlunae.bebek.model.MProperty.PropertyOfEnum.Objects;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.StateList;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.Must;
import static com.luxlunae.bebek.model.MRestriction.MustEnum.MustNot;
import static com.luxlunae.bebek.model.MRestriction.RestrictionTypeEnum.Task;
import static com.luxlunae.bebek.model.MRestriction.TaskEnum.Complete;
import static com.luxlunae.bebek.model.MSingleDescription.DisplayWhenEnum.StartDescriptionWithThis;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Container;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Dynamic;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.Surface;
import static com.luxlunae.bebek.model.io.MFileOlder.ComboEnum.SurfaceContainer;
import static com.luxlunae.bebek.model.io.MFileOlder.convertV4FuncsToV5;
import static com.luxlunae.bebek.model.io.MFileOlder.getRoomGroupFromList;
import static com.luxlunae.bebek.model.io.MFileOlder.loadResource;
import static com.luxlunae.bebek.view.MView.debugPrint;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Objects in ADRIFT are anything that can be seen, touched or
 * referred to, and that are not characters.
 * <p>
 * They are normally categorised as one of two types; dynamic
 * objects, that can be picked up and moved about, and static,
 * which cannot.
 */
public class MObject extends MItemWithProperties implements MItemFunctionEvaluator {
    // -----------------------------------
    //    STANDARD LOCATION PROPERTIES
    // -----------------------------------
    /**
     * Displays the KEY of a static object's location. Entering
     * another dot after this will access the properties of that
     * location (location).
     */
    private static final String PKEY_AT_LOC = "AtLocation";
    private static final String PVAL_AT_LOC = "Single Location";
    /**
     * Displays the KEY of a dynamic object's
     * location. (location)
     */
    private static final String PKEY_IN_LOCATION = "InLocation";
    private static final String PVAL_IN_LOCATION = "In Location";

    // ---------------------------------
    //   STANDARD CHARACTER PROPERTIES
    // ---------------------------------
    /**
     * Key of character that this object is being
     * held by (Dynamic objects only) (character).
     */
    private static final String PKEY_HELD_BY = "HeldByWho";
    private static final String PVAL_HELD_BY = "Held By Character";
    /**
     * Key of character that this object is
     * a part of (Face, hands, nose etc.) (character).
     */
    private static final String PKEY_PART_OF_CHAR = "PartOfWho";
    private static final String PVAL_PART_OF_CHAR = "Part of Character";
    /**
     * Key of character that this object is being
     * worn by (Wearable objects only) (character).
     */
    private static final String PKEY_WORN_BY = "WornByWho";
    private static final String PVAL_WORN_BY = "Worn By Character";

    // ------------------------------
    //    STANDARD TEXT PROPERTIES
    // ------------------------------
    /**
     * Description when read - used for books and signs that
     * we want to be readable (text).
     */
    private static final String PKEY_READ_TEXT = "ReadText";
    /**
     * The name of the current location of a dynamic
     * object (text).
     */
    private static final String PKEY_DYNAMIC_LOC = "DynamicLocation";
    /**
     * The name of the location of a static object (text).
     */
    private static final String PKEY_STATIC_LOC = "StaticLocation";
    /**
     * Can the object be locked (text).
     */
    private static final String PKEY_LOCK_STATUS = "LockStatus";
    /**
     * Open, Closed or Locked (text).
     */
    static final String PKEY_OPEN_STATUS = "OpenStatus";
    static final String PVAL_OPEN = "Open";
    static final String PVAL_CLOSED = "Closed";
    static final String PVAL_LOCKED = "Locked";
    /**
     * Object's type - Static or Dynamic (text).
     */
    private static final String PKEY_STATIC_OR_DYN = "StaticOrDynamic";
    private static final String PVAL_STATIC = "Static";
    private static final String PVAL_DYNAMIC = "Dynamic";
    /**
     * Text to describe dynamic object in a location (text).
     */
    private static final String PKEY_LIST_DESC_DYN = "ListDescriptionDynamic";
    /**
     * Text used to describe static object in a location
     * description (text).
     */
    private static final String PKEY_LIST_DESC = "ListDescription";

    // -------------------------------
    //   STANDARD INTEGER PROPERTIES
    // -------------------------------
    /**
     * How many tiny objects can fit in this
     * object (Containers only) (integer).
     */
    private static final String PKEY_CAPACITY = "Capacity";
    /**
     * How big is this object (integer).
     */
    private static final String PKEY_SIZE = "Size";
    /**
     * How heavy is this object (integer).
     */
    private static final String PKEY_WEIGHT = "Weight";

    // -------------------------------
    //   STANDARD BOOLEAN PROPERTIES
    // -------------------------------
    /**
     * Characters cam lie on this object (boolean).
     */
    private static final String PKEY_IS_LIEABLE = "Lieable";
    /**
     * Characters can sit on this object (boolean).
     */
    private static final String PKEY_IS_SITTABLE = "Sittable";
    /**
     * Characters can stand on this object (boolean).
     */
    private static final String PKEY_IS_STANDABLE = "Standable";
    /**
     * Object can be locked (boolean).
     */
    static final String PKEY_LOCKABLE = "Lockable";
    /**
     * Object can be opened and closed (boolean).
     */
    static final String PKEY_OPENABLE = "Openable";
    /**
     * Object is a container (boolean).
     */
    private static final String PKEY_IS_CONTAINER = "Container";
    /**
     * Object is a supporter (boolean).
     */
    private static final String PKEY_HAS_SURFACE = "Surface";
    /**
     * Object is readable (boolean).
     */
    private static final String PKEY_READABLE = "Readable";
    /**
     * Object is wearable (boolean).
     */
    private static final String PKEY_IS_WEARABLE = "Wearable";

    /**
     * Specifically exclude object from location
     * descriptions (dynamic objects) (boolean).
     */
    private static final String PKEY_EXCLUDE = "ExplicitlyExclude";
    /**
     * Specifically list object in location
     * descriptions (static objects) (boolean).
     */
    private static final String PKEY_LIST = "ExplicitlyList";

    // ------------------------------
    //   STANDARD GROUP PROPERTIES
    // ------------------------------
    /**
     * The location group that a static object
     * occupies (group).
     */
    private static final String PKEY_AT_LOC_GROUP = "AtLocationGroup";
    private static final String PVAL_AT_LOC_GROUP = "Location Group";

    // ------------------------------
    //   STANDARD OBJECT PROPERTIES
    // ------------------------------
    /**
     * The key of the container object that the
     * object is inside of (object).
     */
    private static final String PKEY_IN_OBJECT = "InsideWhat";
    private static final String PVAL_IN_OBJECT = "Inside Object";
    /**
     * The key of the object that is used
     * as a key to unlock this object (object).
     */
    private static final String PKEY_LOCK_KEY = "LockKey";
    /**
     * The object that this object is sitting
     * on top of (object).
     */
    private static final String PKEY_ON_OBJECT = "OnWhat";
    private static final String PVAL_ON_OBJECT = "On Object";
    /**
     * The object or character that this
     * object is a part of (object).
     */
    private static final String PKEY_PART_OF_OBJECT = "PartOfWhat";
    private static final String PVAL_PART_OF_OBJECT = "Part of Object";

    /**
     * Article is a mandatory field. For most common objects
     * you would normally insert an indefinite article here.
     * This is normally used when a noun is being referred to
     * for the very first time, and is typically a or an in
     * English. ADRIFT will automatically accept the definite
     * article ("the") when matching object names, and will
     * replace the indefinite article with the definite article
     * in prose when using the %TheObject[key]% function. For
     * unique objects like "the sword excalibur" or even "the
     * front door" you would insert "the" here. If this object
     * represents several objects, a powder or liquid, you can
     * use the word "some", eg. "some brass tacks" or "some
     * washing powder". If you have several similar objects
     * that are each owned by a particular character, you could
     * use the characters name as the article, eg. "belinda's
     * mobile phone".
     */
    @NonNull
    private String mArticle = "";
    /**
     * Prefix/Adjective is an optional field, but is used to
     * distinguish objects with the same name, or to qualify
     * the noun, giving more information about the object
     * signified. Multiple adjectives can be added, separated
     * by a space. However, all adjectives will be listed when
     * the object is listed during play. So for example, you
     * might set this to "large green", "wooden", "fragile old"
     * etc.
     */
    @NonNull
    private String mPrefix = "";
    /**
     * Name/Noun(s) is a mandatory field. It is the main
     * name of the object, and can typically be referred
     * to uniquely during play to identify the object.
     * Multiple object nouns can be provided by typing the
     * name and pressing <Enter>. This will add a new
     * entry to the drop down list. To edit an existing
     * entry from the drop down, simply select it and make
     * any changes. Similarly, to remove the entry, select
     * it, then clear the value. Only the first noun in
     * the list will be used when listing objects during
     * play. However, all other nouns will be used to
     * identify the object.
     * <p>
     * You cannot have two things with identical names.
     * So if you need to have 3 pens in your game then
     * you need to give them different adjectives, eg.
     * The blue pen, The black pen and The expensive
     * gold pen. You also cannot have a character named
     * Ben and simply replace him with an object called
     * Ben when he is unconscious so that he doesn't talk
     * or move. Instead you could call the object "ben's
     * unconscious body" or else set a property that stops
     * him from behaving like a conscious character.
     */
    @NonNull
    private MStringArrayList mNames = new MStringArrayList();
    /**
     * The plural form(s) of this object's name(s). This
     * array is only initialised and populated once, on
     * the first call to getPlurals().
     */
    @Nullable
    private MStringArrayList mPlurals;
    /**
     * Description is what will be displayed during play when
     * the player examines the object. Being a standard text
     * box then alternate descriptions can be displayed
     * depending on different circumstances. If the object
     * has the "Readable" property, but you do not specify
     * a "Description when read", then this description is
     * also used when the player reads this object. It is
     * also possible to play sound or display an image when
     * the description is displayed.
     */
    @NonNull
    private MDescription mDescription;
    /**
     * Initial Location This is used to specify where this
     * object will be located at the start of the game. The
     * first drop-down list has 6 options (Static and Dynamic
     * objects have different options), and selecting one of
     * these will change the type of item that can be selected
     * from the second list.
     * <p>
     * Any object can be:
     * <p>
     * Hidden - The object is nowhere, it cannot not be found
     * by the player until it is moved to somewhere else.
     * <p>
     * In Location/Single Location - This object will be found
     * at the location selected on the second list.
     * <p>
     * Static objects can be:
     * <p>
     * Location Group - Used for large static objects that span
     * many locations, or doors which connect two locations.
     * <p>
     * Everywhere - The player can refer to this (static) object
     * wherever they are.
     * <p>
     * Part of Object - Something which can't be detached from
     * its parent object, such as the knob on a door.
     * <p>
     * Part of Character - A part of a characters body, ears,
     * eyes, nose, face etc.
     * <p>
     * Dynamic objects can be:
     * <p>
     * Held by Character - The object is in the characters
     * "Inventory".
     * <p>
     * Worn by Character - An item of clothing or jewelery.
     * <p>
     * Inside Object - Select an object with the "Object is a
     * Container" property, to put this inside of.
     * <p>
     * On Object - Select an object with the "Object is a
     * Supporter" property, to put this on top of.
     */
    @Nullable
    private MObjectLocation mLocation;
    /**
     * Initialised upon the first call to isStatic(),
     * we cache the result here to avoid the overheads of
     * having to re-evaluate the property upon every
     * subsequent call to that function.
     */
    @Nullable
    private Boolean mIsStatic = null;
    /**
     * This gets updated at the end of each turn, and
     * allows any tasks to reference the parent before
     * they are updated from task actions.
     */
    @NonNull
    private String mLastParent = "";

    public MObject(@NonNull MAdventure adv) {
        super(adv);
        mDescription = new MDescription(adv);
    }

    public MObject(@NonNull MAdventure adv,
                   @NonNull final MFileOlder.V4Reader reader,
                   final int iObj, final double v, final int nLocations,
                   final int iStartLocations,
                   @NonNull MStringArrayList salWithStates,
                   @NonNull HashMap<String, String> dictDodgyStates,
                   @NonNull HashMap<MObject, MProperty> dodgyArlStates,
                   @NonNull ArrayList<MObject> newObs) throws EOFException {
        // ADRIFT V3.80, 3.90 and V4 Loader
        this(adv);

        String myKey = "Object" + iObj;
        while (adv.mObjects.containsKey(myKey)) {
            myKey = incrementKey(myKey);
        }
        setKey(myKey);
        newObs.add(this);

        // ===========================================================
        //    PREFIX, NAME(S), STATIC OR DYNAMIC TYPE, DESCRIPTION
        // -----------------------------------------------------------
        // The prefix should contain any adjectives for the object,
        // and determine whether or not it is singular (i.e. “a
        // large”, “an”, “some purple”).
        String[] split = convertPrefix(reader.readLine());                  // Prefix
        setArticle(split[0]);
        setPrefix(split[1]);

        // Every object requires to be given a name. This is how
        // you will refer to the object in the game. You should try
        // to keep this as short as possible with any extra
        // descriptions being put into the object prefix. This means
        // that it will be easier for the player to refer to the object
        // during the game, as this will have to be typed every time
        // the object is referenced.
        mNames.add(reader.readLine());                                      // Name

        // Objects can be given any number of aliases. These are
        // alternative names that can be used in the game to refer
        // to the object. For example, if you wanted to create a
        // red poppy, you would set the prefix to be "a bright red",
        // the object name to be "poppy", and an alias to be "flower".
        // To add multiple aliases, simply type the alias into the box
        // and press Enter.
        if (v < 4) {
            // Earlier versions only allow one alias.
            String alias = reader.readLine();                               // Alias
            if (!alias.equals("")) {
                mNames.add(alias);
            }
        } else {
            // Version 4 allows multiple aliases.
            int nAliases = cint(reader.readLine());                         // # Aliases
            for (int i = 0; i < nAliases; i++) {
                mNames.add(reader.readLine());                              // Alias
            }
        }

        // There are two types of object in ADRIFT; Dynamic
        // objects, and Static objects. The Player can pick up
        // dynamic objects, whereas static objects are fixed in
        // specific rooms. When you view a room all dynamic objects
        // will be listed, in the format "Also here is ...".
        MProperty sod =
                adv.mAllProperties.get(PKEY_STATIC_OR_DYN).copy();
        addProperty(sod);
        setStatic(safeBool(reader.readLine()));                             // Type

        // Objects can be given a description. This will be
        // displayed if the player types "examine <object>" in
        // the game. If nothing is entered, it will appear in
        // the game as "You see nothing special about
        // <the object>."
        setDescription(new MDescription(adv,
                convertV4FuncsToV5(adv, reader.readLine())));               // Description

        // ===========================================================
        //            INITIAL POSITION OF DYNAMIC OBJECTS
        // -----------------------------------------------------------
        // If the object is dynamic, you have to select the initial
        // position for the object from the pull down menu. This can
        // be either "Hidden", "Held by someone", "Inside an object",
        // "On an object", a specific room, or "Worn by someone" if
        // the object is wearable.
        //
        // If you select "Held by someone", the "Held by who" pull
        // down menu becomes active. You should then select whether
        // the Player or another character holds it. Similarly, for
        // "Worn by someone". If you select "Inside an object", or
        // "On an object", the "Inside/on object" pull down menu
        // becomes active. This will then give a list of all objects
        // that have a surface or are containers.
        MObjectLocation obLoc = new MObjectLocation(adv);
        if (isStatic()) {
            // Next line is not used for static objects - skip.
            reader.skipLine();                                              // Unused
            MProperty sl =
                    adv.mAllProperties.get(PKEY_STATIC_LOC).copy();
            addProperty(sl);
        } else {
            MProperty dl =
                    adv.mAllProperties.get(PKEY_DYNAMIC_LOC).copy();
            addProperty(dl);
            int initPos = cint(reader.readLine());                          // Initial Position
            if (v < 3.9 && initPos > 2) {
                // For version 3.8, initial position = 2 means ON
                // *or* IN. Later on, we'll work out which, depending
                // upon the type of this object's parent. For now, if
                // initial position is above on/in increment it, so
                // higher values are correctly interpreted as a
                // location reference.
                initPos++;
            }
            switch (initPos) {
                case 0:
                    obLoc.mDynamicExistWhere = Hidden;
                    break;
                case 1:
                    obLoc.mDynamicExistWhere = HeldByCharacter;
                    break;
                case 2:
                    obLoc.mDynamicExistWhere = InObject;
                    break;
                case 3:
                    obLoc.mDynamicExistWhere = OnObject;
                    break;
                default:
                    if (initPos == nLocations + 4) {
                        obLoc.mDynamicExistWhere = WornByCharacter;
                    } else {
                        obLoc.mDynamicExistWhere = InLocation;
                        obLoc.setKey("Location" +
                                (initPos - 3 + iStartLocations));
                        MProperty p =
                                adv.mAllProperties.get(PKEY_IN_LOCATION).copy();
                        addProperty(p);
                    }
                    break;
            }
        }

        // ===========================================================
        //                 ALTERNATIVE DESCRIPTION
        // -----------------------------------------------------------
        // If completing a task changes the appearance of the object,
        // then you can select the task from the "But if task" pull
        // down menu, and enter the new description in the second text
        // box. This description supersedes the first one.
        String tKey = "Task" + reader.readLine();                           // Task #
        boolean taskNotDone = cbool(reader.readLine());                     // Not completed?
        String altDesc = convertV4FuncsToV5(adv, reader.readLine());        // Alt Desc
        if (!tKey.equals("Task0")) {
            MSingleDescription sd = new MSingleDescription(adv);
            sd.mDescription = altDesc;
            MRestriction rest = new MRestriction(adv);
            rest.mType = Task;
            rest.mMust = taskNotDone ? MustNot : Must;
            rest.mTaskType = Complete;
            rest.mKey1 = tKey;
            sd.mRestrictions.add(rest);
            sd.mRestrictions.mBracketSequence = "#";
            sd.mDisplayWhen = StartDescriptionWithThis;
            getDescription().add(sd);
        }

        // ===========================================================
        //           INITIAL POSITION OF STATIC OBJECTS
        // -----------------------------------------------------------
        // If the object is static, then you have to say in which
        // room(s) the object is present. Usually this will just be
        // a single room, but there may be reasons you would want it
        // to span more than one. This might be a river that was in
        // more than one room, a generic object such as the sky,
        // ground, walls etc, or just a door, which you can view from
        // either side. To choose the rooms, click on the room names
        // in the list on the right hand side. Clicking on "All Rooms"
        // will highlight all the rooms in the list. Similarly
        // "No Rooms" will deselect all the rooms.
        //
        // A special case for the static object type is if it is part
        // of a character. Instead of selecting a room for the object,
        // select the location “Part of Character”. This will activate
        // the Character dropdown list. You can then select whether this
        // should be the Player or a specific character. You can now
        // only examine this object when the particular character is in
        // the room.
        if (isStatic()) {
            // Where this static object is located
            obLoc.mStaticExistWhere =
                    toStaticExistsWhere(cint(reader.readLine()));           // #Type
            switch (obLoc.mStaticExistWhere) {
                case NoRooms:
                case AllRooms:
                case PartOfCharacter:
                case PartOfObject:
                    // Key defined later
                    break;
                case SingleLocation:
                    obLoc.setKey("Location" + reader.readLine());           // Room #
                    MProperty pLoc =
                            adv.mAllProperties.get(PKEY_AT_LOC).copy();
                    addProperty(pLoc);
                    break;
                case LocationGroup:
                    // Read a list of boolean flags for every room
                    // specifying whether this object is located there
                    // or not.
                    MStringArrayList rooms = new MStringArrayList();
                    for (int i = 0; i <= nLocations; i++) {
                        if (cbool(reader.readLine())) {                     // Room #
                            rooms.add("Location" + i);
                        }
                    }
                    obLoc.setKey(getRoomGroupFromList(adv, rooms,
                            "object '" +
                                    getFullName() + "'").getKey());
                    MProperty pLG =
                            adv.mAllProperties.get(PKEY_AT_LOC_GROUP).copy();
                    addProperty(pLG);
                    break;
            }
        }

        // ===========================================================
        //                      ATTRIBUTES
        // -----------------------------------------------------------
        // You will be given different options, depending on whether
        // the object is static or dynamic.

        // -----------------------------------------------------------
        //         PROPERTIES OF DYNAMIC AND STATIC OBJECTS:
        //                   Container, Surface
        // -----------------------------------------------------------
        // "Object is a container" allows you to be able to put other
        // objects inside it.
        //
        // "Object has a surface" allows you to put things onto the
        // object in the game. Objects on other objects won't appear
        // in the room description, so the player has to examine the
        // parent object to see if there are any objects on or in it.
        // There is no limit to the number of objects you can put on a
        // surface object.
        if (v < 3.9) {
            // Version 3.8 allowed objects to be a container or
            // surface but not both.
            int surfaceOrContainer = cint(reader.readLine());             // Surface or Container?
            switch (surfaceOrContainer) {
                case 1:
                    // CONTAINER
                    MProperty c =
                            adv.mAllProperties.get(PKEY_IS_CONTAINER).copy();
                    c.setSelected(true);
                    addProperty(c);
                    break;
                case 2:
                    // SURFACE
                    MProperty s =
                            adv.mAllProperties.get(PKEY_HAS_SURFACE).copy();
                    s.setSelected(true);
                    addProperty(s);
                    break;
            }
        } else {
            // Version 3.9 and 4.0 allowed objects to be both
            // a container and a surface.
            if (cbool(reader.readLine())) {                              // Container?
                // CONTAINER
                MProperty c =
                        adv.mAllProperties.get(PKEY_IS_CONTAINER).copy();
                c.setSelected(true);
                addProperty(c);
            }
            if (cbool(reader.readLine())) {                              // Surface?
                // SURFACE
                MProperty s =
                        adv.mAllProperties.get(PKEY_HAS_SURFACE).copy();
                s.setSelected(true);
                addProperty(s);
            }
        }

        // If the object is a container, you have to say how
        // many objects it can contain, up to a maximum of 99,
        // and the size of objects it can contain; If you attempt
        // to put objects inside a container object that is full,
        // you will receive a failure message. There is no limit
        // to the depth of object containers; i.e. you could have a
        // coin inside a purse, inside a bag, inside a box etc.
        int iCapacity = cint(reader.readLine());                         // Capacity
        if (isContainer()) {
            if (v < 3.9) {
                iCapacity = iCapacity * 100 + 2;
            }
            int iCapacity2 = iCapacity % 10;
            iCapacity -= iCapacity2;
            iCapacity = (int) ((iCapacity / 10) * Math.pow(3, iCapacity2));
            if (adv.mObjectProperties.containsKey(PKEY_CAPACITY)) {
                MProperty p =
                        adv.mObjectProperties.get(PKEY_CAPACITY).copy();
                p.setSelected(true);
                p.setValue(String.valueOf(iCapacity));
                addProperty(p);
            }
        }

        if (isStatic()) {
            // ------------------------------------------------------
            //             PROPERTIES OF STATIC OBJECTS
            //                  <Nothing further>
            // ------------------------------------------------------
            // For static objects that are part of a character,
            // we now read additional information about the
            // character.
            if (obLoc.mStaticExistWhere == PartOfCharacter) {
                int iChar = cint(reader.readLine());                      // Parent if part of char
                obLoc.setKey((iChar == 0) ?
                        THEPLAYER : ("Character" + iChar));
                MProperty c =
                        adv.mAllProperties.get(PKEY_PART_OF_CHAR).copy();
                c.setStringData(new MDescription(adv,
                        convertV4FuncsToV5(adv, obLoc.getKey())));
                addProperty(c);
            }
            moveTo(obLoc);
        } else {
            // ------------------------------------------------------
            //             PROPERTIES OF DYNAMIC OBJECTS
            //                 Wearable, Size, Weight
            // ------------------------------------------------------
            if (cbool(reader.readLine())) {                              // Wearable?
                // Object is wearable allows the Player and
                // characters to wear and remove it. You can then
                // restrict tasks depending whether or not the
                // object is being worn.
                MProperty w =
                        adv.mAllProperties.get(PKEY_IS_WEARABLE).copy();
                w.setSelected(true);
                addProperty(w);
            }

            // You can define the size and weight of the object
            // from the pull down lists at the bottom of the
            // screen. Each increase in size or weight is 3 times
            // greater than the previous entry; i.e. a Huge object
            // is 81 times the size of a Tiny object. What these
            // sizes actually mean is relative, and determined by
            // you. If an object is put inside a container object
            // and the container is dynamic, the container will
            // increase in weight by the weight of the object put
            // inside it but it won't increase in size. Limits can
            // be put on the Player to limit the size and weight
            // that they can carry.
            //
            // Only the exponents of the size and weight are stored
            // in the data file. These are:
            //
            //    Value:  1       3      9      27      81
            //    Exp:    0       1      2       3      4
            //    Size:  Tiny   Small  Normal  Large   Huge
            //  Weight: V Light Light  Normal  Heavy  V Heavy
            //
            // N.B. ADRIFT 3.9 and 4 games also stored the size
            // and weight ratios (bases) in the game file. It seems
            // these were always set to 3 with no means of
            // customisation by the player.
            int size = cint(reader.readLine());                         // Size and Weight exponents
            int weight;
            if (v < 3.9) {
                // In v3.8, it seems that only a size exponent
                // was stored (ranging from 0..4). We make the
                // weight exponent the same as the size exponent.
                weight = size;
            } else {
                // In v3.9 and 4.0, size and weight exponents
                // are stored as a single integer (y) calculated
                // as follows:
                //
                //         y = Size * 10 + Weight
                //
                weight = size % 10;
                size -= weight;
                size /= 10;
            }
            MProperty p;
            if (adv.mObjectProperties.containsKey(PKEY_SIZE)) {
                p = adv.mObjectProperties.get(PKEY_SIZE).copy();
                p.setSelected(true);
                p.setValue(String.valueOf(Math.pow(adv.mCompatSizeRatio, size)));
                addProperty(p);
            }
            if (adv.mObjectProperties.containsKey(PKEY_WEIGHT)) {
                p = adv.mObjectProperties.get(PKEY_WEIGHT).copy();
                p.setSelected(true);
                p.setValue(String.valueOf(Math.pow(adv.mCompatWeightRatio, weight)));
                addProperty(p);
            }

            // For dynamic objects that are "held by", "worn by",
            // "in object" or "on object", we now read additional
            // information about the parent character or object.
            int parent = cint(reader.readLine());                      // Parent if held, worn, in, on
            switch (obLoc.mDynamicExistWhere) {
                case HeldByCharacter:
                    obLoc.setKey((parent == 0) ?
                            THEPLAYER : ("Character" + parent));
                    p = adv.mAllProperties.get(PKEY_HELD_BY).copy();
                    addProperty(p);
                    p.setValue(obLoc.getKey());
                    setLocation(obLoc);
                    break;
                case WornByCharacter:
                    obLoc.setKey((parent == 0) ?
                            THEPLAYER : ("Character" + parent));
                    p = adv.mAllProperties.get(PKEY_WORN_BY).copy();
                    addProperty(p);
                    p.setValue(obLoc.getKey());
                    setLocation(obLoc);
                    break;
                case InObject:
                    // N.B. setLocation must be done before
                    // adding the property!
                    setLocation(obLoc);
                    p = adv.mAllProperties.get(PKEY_IN_OBJECT).copy();
                    addProperty(p);
                    p.setValue(String.valueOf(parent));
                    break;
                case OnObject:
                    // N.B. setLocation must be done before
                    // adding the property!
                    setLocation(obLoc);
                    p = adv.mAllProperties.get(PKEY_ON_OBJECT).copy();
                    addProperty(p);
                    p.setValue(String.valueOf(parent));
                    break;
                default:
                    setLocation(obLoc);
                    break;
            }
        }

        // -----------------------------------------------------------
        //         PROPERTIES OF DYNAMIC AND STATIC OBJECTS:
        //                Openable, SitStandLieable
        // -----------------------------------------------------------
        // "Object can be Opened and Closed". This can be used with
        // containers, or just on its own (e.g. a door). Tasks can
        // be restricted depending the status of an object. If the
        // object is also a container, any objects inside it are only
        // listed on examining the object if it is open. You must
        // specify from the dropdown list the state you want the
        // object to start off in. If you define the object as being
        // lockable (see below), then you can also start the object
        // being Locked.
        int iOpenableLockable = cint(reader.readLine());                    // Openable?
        if (v < 4 && iOpenableLockable > 1) {
            iOpenableLockable = 11 - iOpenableLockable;
        }

        // 0 = Not openable
        // 5 = Openable, open
        // 6 = Openable, closed
        // 7 = Openable, locked
        if (iOpenableLockable > 0) {
            MProperty op =
                    adv.mAllProperties.get(PKEY_OPENABLE).copy();
            op.setSelected(true);
            addProperty(op);
            MProperty pOS =
                    adv.mAllProperties.get(PKEY_OPEN_STATUS).copy();
            pOS.setSelected(true);
            switch (iOpenableLockable) {
                case 5:
                    pOS.setValue(PVAL_OPEN);
                    break;
                case 6:
                default:
                    pOS.setValue(PVAL_CLOSED);
                    break;
                case 7:
                    // N.B. Unlike in the original Adrift 5 code, we
                    // allow games to make objects lockable and locked
                    // even if they have not defined an object to be the
                    // key. For example, in Goldilocks is a Fox, the
                    // pedlar's suitcase should be locked, even though
                    // there is no way for the player to unlock it (the
                    // player is supposed to get the contents - the beans -
                    // by trading a cow with the pedlar).
                    MProperty pLk =
                            adv.mAllProperties.get(PKEY_LOCKABLE).copy();
                    pLk.setSelected(true);
                    addProperty(pLk);

                    MProperty pLS =
                            adv.mAllProperties.get(PKEY_LOCK_STATUS).copy();
                    pLS.setSelected(true);
                    addProperty(pLS);

                    pOS.setValue(PVAL_LOCKED);
                    break;
            }
            addProperty(pOS);
            if (v >= 4) {
                int lockKey = cint(reader.readLine());                         // Key object ID
                if (lockKey > -1) {
                    // "...and is Lockable, with key" allows
                    // you to lock objects. This option only
                    // becomes enabled if you’ve defined the
                    // object as being openable. You must select
                    // a dynamic object as being a key. You will
                    // then be able to lock and unlock the object
                    // with that key. If you wanted multiple keys,
                    // for example a master key, you would need to
                    // do that using tasks.
                    MProperty pKey =
                            adv.mAllProperties.get(PKEY_LOCK_KEY).copy();
                    pKey.setSelected(true);
                    pKey.setValue(String.valueOf(lockKey));
                    addProperty(pKey);
                }
            }
        }

        // "The player is allowed to sit/stand on the object" does
        // just that. This enhances the reality of the adventure,
        // and can be used in task restrictions. "The player can lie
        // on the object" does the same as above, except for lying.
        int iSitStandLie = cint(reader.readLine());                         // SitStandLie?
        if (iSitStandLie == 1 || iSitStandLie == 3) {
            setSittable(true);
        }
        setStandable(isSittable());
        if (iSitStandLie == 2 || iSitStandLie == 3) {
            setLieable(true);
        }

        // -----------------------------------------------------------
        //              PROPERTIES OF DYNAMIC OBJECTS:
        //                          Edible
        // -----------------------------------------------------------
        // "Object is edible" means that if the Player "eats" the
        // object in the game, it will disappear. If you want
        // something specific to happen when the object is eaten,
        // you can add a task such as "eat <object>" which would
        // override this option.
        if (!isStatic()) {
            // TODO
            reader.skipLine();                                              // Edible?
        }

        // -----------------------------------------------------------
        //         PROPERTIES OF DYNAMIC AND STATIC OBJECTS:
        //                        Readable
        // -----------------------------------------------------------
        // "Object is readable" means that the player can type
        // "read <object>". If you enter a description in the text
        // box, this will be displayed. If not, the same description
        // is given as when the object is examined.
        if (cbool(reader.readLine())) {                                     // Readable?
            MProperty r =
                    adv.mAllProperties.get(PKEY_READABLE).copy();
            r.setSelected(true);
            addProperty(r);
        }
        if (isReadable()) {
            String readText = reader.readLine();                            // Text when read
            if (!readText.equals("")) {
                MProperty r =
                        adv.mAllProperties.get(PKEY_READ_TEXT).copy();
                r.setSelected(true);
                addProperty(r);
                setReadText(readText);
            }
        }

        // -----------------------------------------------------------
        //              PROPERTIES OF DYNAMIC OBJECTS:
        //                       Weapon
        // -----------------------------------------------------------
        // "Object can be used as a weapon" defines the object to be
        // something that the Player could potentially use to attack
        // characters with, although the default message will be that
        // you miss the character. To enhance this, you’d need to use
        // tasks.
        if (!isStatic()) {
            // TODO
            reader.skipLine();                                              // Weapon?
        }

        if (v >= 4) {
            // ------------------------------------------------------
            //         PROPERTIES OF DYNAMIC AND STATIC OBJECTS:
            //                     Object state
            //             (only supported in version 4)
            // ------------------------------------------------------
            // "Object starts off in state" allows you to create any
            // state for the object. This defaults to On and Off, but
            // by clicking on "Define", you can insert, edit or delete
            // the different states available, so for example, you
            // could have "Up/Down". These states can be used in task
            // restrictions.
            int iState = cint(reader.readLine());                           // Index of initial state
            if (iState > 0) {
                // If the initial state index is nonzero then this object
                // has states defined (and the initial state is index
                // iState - 1 of the state list).
                String states = reader.readLine();                          // State list

                // We need to strip out any commas from the state values.
                // Otherwise this causes problems later when the property
                // is set, as the comma is interpreted as an argument
                // delimiter for an expression. In "pure" ADRIFT 5, each
                // state should only be one word.
                states = states.replace(",", "");
                MStringArrayList stateList = new MStringArrayList();
                for (String state : states.split("\\|")) {
                    StringBuilder sb = new StringBuilder(state);
                    toProper(sb);
                    stateList.add(sb.toString());
                }

                // We need to strip out any punctuation from the key.
                //
                // Some older games have states with spaces (e.g. To
                // Hell in a Hamper) - if these are not removed
                // ReplaceOO gets confused about what the property is -
                // e.g. interprets coat.buttoned up|open|closed as
                // referring to property "buttoned" instead of property
                // "buttoned up|open|closed".
                //
                // Other games have states with periods (e.g. Professor
                // Von Witt's Flying Machine). This also cause problems
                // for ReplaceOO.
                String newPKey = states.replace(" ", "_")
                        .replaceAll("[^A-Za-z0-9|_]", "");
                String existingPKey = adv.findProperty(stateList);
                MProperty s;
                boolean isLibraryOverride = false;
                if (existingPKey == null) {
                    // There isn't an existing property with all of the states.
                    // Create a new one.
                    s = new MProperty(adv);
                    s.setType(StateList);
                    s.setDescription("Object can be \"" +
                            states.replace("|", "\" or \"") + "\"");
                    s.setKey(newPKey);
                    s.mStates = stateList;
                    s.setPropertyOf(Objects);
                    adv.mAllProperties.put(s.copy());
                } else {
                    // There is an existing property with all of the states.
                    // Create a copy of that.
                    s = adv.mAllProperties.get(existingPKey).copy();
                    if (!newPKey.equals(existingPKey)) {
                        // Hmm, the states are not in the same order as before
                        // This can cause problems if restrictions/actions
                        // use the state index
                        dictDodgyStates.put(getKey(), states);
                    }
                    if (existingPKey.equals(PKEY_OPEN_STATUS)) {
                        // We need to add the Openable property as well
                        // At least one game, Goldilocks is a Fox, sets
                        // a location movement restriction on an object
                        // that does not have openable status (in the
                        // Goldilocks case, the trapdoor in the cottage).
                        MProperty op =
                                adv.mAllProperties.get(PKEY_OPENABLE).copy();
                        op.setSelected(true);
                        addProperty(op);
                        isLibraryOverride = true;
                    }
                }
                s.setValue(stateList.get(iState - 1));
                s.setSelected(true);
                addProperty(s);
                salWithStates.add(getKey());

                // If you want the state to be displayed when
                // examining the object in the format “The
                // <object> is <state>.”, then click the
                // "Show in description" checkbox.
                boolean showState = cbool(reader.readLine());              // Show state?
                if (!isLibraryOverride && showState) {
                    getDescription().get(0).mDescription +=
                            "  " + getKey() + ".Name is %LCase[" +
                                    getKey() + "." + s.getKey() + "]%.";
                }
            }

            // ======================================================
            //                  ADVANCED FEATURES
            //            (only supported in version 4)
            // ------------------------------------------------------
            // By default, all dynamic objects are listed in a room
            // description if they are in that room, and static
            // objects are not listed – you are expected to describe
            // them explicitly in your room description.
            //
            // In the advanced tab, if your object is static, you
            // have the option to select "Specifically list object in
            // room descriptions." This lists the object in the
            // form “Also here is ...” as though it were a dynamic
            // object.
            //
            // If the object is dynamic, the checkbox becomes
            // "Do NOT list object in room descriptions." This
            // prevents the object being listed. The object will
            // still be there; just there will be no notification.
            // You would usually want to use this feature if you
            // were to explicitly write the object into some other
            // description.
            boolean listFlag = cbool(reader.readLine());                   // List Flag
            if (isStatic()) {
                setExplicitlyList(listFlag);
            } else {
                setExplicitlyExclude(listFlag);
            }
        }

        // ===========================================================
        //                        RESOURCES
        // -----------------------------------------------------------
        for (int i = 0; i < 2; i++) {
            loadResource(adv, reader, v, getDescription().get(0));
        }

        if (adv.mCompatBattleSystem) {
            // ======================================================
            //                      BATTLE SYSTEM
            // -------------------------------------------------------
            // When the Battle System is enabled, additional options
            // become available in object attributes.
            // TODO
            reader.skipLine();                                              // iProtectionValue
            reader.skipLine();                                              // iHitValue
            reader.skipLine();                                              // iMethod
            if (v >= 4) {
                reader.skipLine();                                          // iAccuracy
            }
        }

        // ===========================================================
        //                      ADVANCED FEATURES
        //                 (only supported in version 4)
        // -----------------------------------------------------------
        if (v >= 4) {
            // You can also override the default “Also here
            // is <objectname>” with your own custom message
            // by filling in the box "When the object is listed
            // in the room description, display this." This will
            // then be displayed on its own after listing any
            // other objects.
            String listDesc = reader.readLine();                            // List Desc
            if (!listDesc.equals("")) {
                MProperty r = adv.mAllProperties.get(isStatic() ?
                        PKEY_LIST_DESC : PKEY_LIST_DESC_DYN).copy();
                r.setSelected(true);
                addProperty(r);
                setListDescription(listDesc);
            }

            // If you want the custom description of the
            // object to only occur when the Player first comes
            // across the object (i.e. before they take it for the
            // first time), check the "Only show above for the
            // object’s initial location" box.
            // TODO
            reader.skipLine();                                              // Only if not moved?
        }

        // Store the pseudo-states that will need further processing later.
        for (MProperty prop : getLocalProperties().values()) {
            if (prop.getKey().indexOf("|") > 0) {
                dodgyArlStates.put(this, prop);
                break;
            }
        }
    }

    public MObject(@NonNull MAdventure adv,
                   @NonNull final XmlPullParser xpp,
                   final boolean isLibrary, final boolean addDuplicateKeys,
                   final double v) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Object");

        String s;
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT &&
                xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        try {
                            header.processTag(xpp);
                        } catch (Exception e) {
                            // just ignore it
                        }
                        break;

                    case "Description":
                        setDescription(new MDescription(adv, xpp,
                                v, "Description"));
                        break;

                    case "Article":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setArticle(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Prefix":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setPrefix(s);
                            }
                        } catch (Exception e) {
                            // do nothing
                            continue;
                        }
                        break;

                    case "Name":
                        getNames().add(xpp.nextText());
                        break;

                    case "Property":
                        try {
                            addProperty(new MProperty(adv, xpp, v));
                        } catch (Exception e) {
                            // do nothing
                        }
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Object");

        if (!header.finalise(this, adv.mObjects,
                isLibrary, addDuplicateKeys, null)) {
            throw new Exception();
        }

        // Assigns the location object from the object properties:
        setLocation(getLocation());
    }

    static void moveObject(@NonNull MAdventure adv,
                           @NonNull MObject obj,
                           @NonNull MAction.MoveObjectToEnum op,
                           @NonNull String toKey) {
        // Work out where to move this object to, then move it
        MObjectLocation dest = new MObjectLocation(adv);
        switch (op) {
            case InsideObject:
                dest.mDynamicExistWhere = InObject;
                dest.setKey(toKey);
                break;
            case OntoObject:
                dest.mDynamicExistWhere = OnObject;
                dest.setKey(toKey);
                break;
            case ToCarriedBy:
                dest.mDynamicExistWhere = HeldByCharacter;
                dest.setKey(toKey);
                break;
            case ToLocation:
                if (obj.isStatic()) {
                    if (toKey.equals(HIDDEN)) {
                        dest.mStaticExistWhere = NoRooms;
                    } else {
                        dest.mStaticExistWhere = SingleLocation;
                        dest.setKey(toKey);
                    }
                } else {
                    if (toKey.equals(HIDDEN)) {
                        dest.mDynamicExistWhere = Hidden;
                    } else {
                        dest.mDynamicExistWhere = InLocation;
                        dest.setKey(toKey);
                    }
                }
                break;
            case ToPartOfCharacter:
                dest.mStaticExistWhere = PartOfCharacter;
                dest.setKey(toKey);
                break;
            case ToPartOfObject:
                dest.mStaticExistWhere = PartOfObject;
                dest.setKey(toKey);
                break;
            case ToLocationGroup:
                if (obj.isStatic()) {
                    dest.mStaticExistWhere = LocationGroup;
                    dest.setKey(toKey);
                } else {
                    // Need to select one room at random
                    MGroup group = adv.mGroups.get(toKey);
                    dest.mDynamicExistWhere = InLocation;
                    dest.setKey(group.getRandomKey());
                }
                break;
            case ToSameLocationAs:
                if (obj.isStatic()) {
                    MCharacter chDest = adv.mCharacters.get(toKey);
                    if (chDest != null) {
                        // Move static object to same location as a character
                        MCharacter.MCharacterLocation chDestLoc = chDest.getLocation();
                        switch (chDestLoc.getExistsWhere()) {
                            case AtLocation:
                                dest.mStaticExistWhere = SingleLocation;
                                dest.setKey(chDestLoc.getKey());
                                break;
                            case Hidden:
                                dest.mStaticExistWhere = NoRooms;
                                break;
                        }
                    } else {
                        MObject obDest = adv.mObjects.get(toKey);
                        if (obDest != null) {
                            // Move static object to same location as an object
                            MObjectLocation obDestLoc = obDest.getLocation();
                            if (obDest.isStatic()) {
                                dest = obDestLoc.copy();
                            } else {
                                switch (obDestLoc.mDynamicExistWhere) {
                                    case Hidden:
                                        dest.mStaticExistWhere = NoRooms;
                                        break;
                                    default:
                                        dest.mStaticExistWhere = SingleLocation;
                                        MLocationHashMap locs = obDest.getRootLocations();
                                        if (locs.size() > 0) {
                                            // set to first entry in the hash map
                                            dest.setKey(locs.entrySet().iterator().next().getKey());
                                        }
                                        break;
                                }
                            }
                        } else {
                            errMsg("Cannot move object to same location as " +
                                    toKey + " - key not found!");
                        }
                    }
                } else {
                    MCharacter chDest = adv.mCharacters.get(toKey);
                    if (chDest != null) {
                        // Move dynamic object to same location as a character
                        MCharacter.MCharacterLocation chDestLoc = chDest.getLocation();
                        switch (chDestLoc.getExistsWhere()) {
                            case AtLocation:
                                // Move the object to character's location property
                                dest.mDynamicExistWhere = InLocation;
                                dest.setKey(chDestLoc.getKey());
                                break;
                            case Hidden:
                                // Character is hidden so hide the object also
                                dest.mDynamicExistWhere = Hidden;
                                break;
                            case InObject:
                                // Move the object inside the same object
                                // the character in - if it is a container.
                                // Otherwise move it to the character's location.
                                MObject obDest = adv.mObjects.get(chDestLoc.getKey());
                                if (obDest.isContainer()) {
                                    dest.mDynamicExistWhere = InObject;
                                    dest.setKey(chDestLoc.getKey());
                                } else {
                                    dest.mDynamicExistWhere = InLocation;
                                    dest.setKey(chDestLoc.getLocationKey());
                                }
                                break;
                            case OnCharacter:
                                // Move to the location that the character
                                // is at.
                                dest.mDynamicExistWhere = InLocation;
                                dest.setKey(chDestLoc.getLocationKey());
                                break;
                            case OnObject:
                                // Move the object onto the object we're on -
                                // if it is a surface. Otherwise move it to
                                // the character's location.
                                MObject obDest2 = adv.mObjects.get(chDestLoc.getKey());
                                if (obDest2.hasSurface()) {
                                    dest.mDynamicExistWhere = OnObject;
                                    dest.setKey(chDestLoc.getKey());
                                } else {
                                    dest.mDynamicExistWhere = InLocation;
                                    dest.setKey(chDestLoc.getLocationKey());
                                }
                                break;
                        }
                    } else {
                        MObject obDest = adv.mObjects.get(toKey);
                        if (obDest != null) {
                            // Move dynamic object to same location as an object
                            MObjectLocation obDestLoc = obDest.getLocation();
                            if (obDest.isStatic()) {
                                // If the static destination object exists in more
                                // than one place, pick a random location
                                switch (obDestLoc.mStaticExistWhere) {
                                    case AllRooms:
                                        TODO("Move dynamic object to one of all rooms");
                                        break;
                                    case LocationGroup:
                                        TODO("Move dynamic object to one of a location group");
                                        break;
                                    case NoRooms:
                                        obDestLoc.mDynamicExistWhere = Hidden;
                                        break;
                                    case PartOfCharacter:
                                        TODO("Move dynamic object to same room as character");
                                        break;
                                    case PartOfObject:
                                        TODO("Move dynamic object to same room as part of object");
                                        break;
                                    case SingleLocation:
                                        dest.mDynamicExistWhere = InLocation;
                                        dest.setKey(obDestLoc.getKey());
                                        break;
                                }
                            } else {
                                if (obDestLoc.mDynamicExistWhere == Hidden) {
                                    dest.mDynamicExistWhere = Hidden;
                                } else {
                                    dest = obDestLoc.copy();
                                }
                            }
                        } else {
                            errMsg("Cannot move object to same location as " +
                                    toKey + " - key not found!");
                        }
                    }
                }
                break;
            case ToWornBy:
                dest.mDynamicExistWhere = WornByCharacter;
                dest.setKey(toKey);
                break;
        }
        obj.moveTo(dest);
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
                //               %object%
                //            (Text property)
                // ----------------------------------------
                // No object property specified, which means
                // we just return this object's key.
                return getKey();

            case "Count":
                // ----------------------------------------
                //            %object%.Count
                //          (Integer property)
                // ----------------------------------------
                // Returns 1.
                resultIsInteger[0] = true;
                return "1";

            case "Adjective":
            case "Prefix":
                // ----------------------------------------
                //           %object%.Adjective
                //           %object%.Prefix
                //           (Text property)
                // ----------------------------------------
                // This object's prefix.
                return getPrefix();

            case "Article":
                // ----------------------------------------
                //           %object%.Article
                //           (Text property)
                // ----------------------------------------
                // This object's article.
                return getArticle();

            case "Children": {
                // ----------------------------------------
                //        %object%.Children(Include)
                //            (Group property)
                // ----------------------------------------
                // Get a group of the objects and/or characters
                // on top of and/or inside this object.
                //
                // The possible values for the optional
                // "Include" argument are:
                //
                //   "All", "OnAndIn", "All,OnAndIn":
                //       Get all objects and characters
                //       inside or on this object (as well
                //       as any objects/characters inside
                //       or on them).  (Default)
                //
                //   "Characters,In":
                //       Get all characters inside this
                //       object.
                //
                //   "Characters,On":
                //       Get all characters on this object.
                //
                //   "Characters,OnAndIn", "Characters":
                //       Get all characters inside or on
                //       this object.
                //
                //   "In":
                //       Get all objects and characters
                //       inside this object (as well as
                //       any objects/characters inside
                //       them).
                //
                //   "Objects,In":
                //       Get all objects inside this
                //       object (as well as any objects
                //       inside them).
                //
                //   "Objects,On":
                //       Get all objects on this object
                //       (as well as any objects on them).
                //
                //   "Objects,OnAndIn", "Objects":
                //       Get all objects inside or on
                //       this object (as well as any
                //       objects inside or on them).
                //
                ArrayList<MItemWithProperties> lst = new ArrayList<>();
                switch (args.toLowerCase().replace(" ", "")) {
                    case "":
                    case "all":
                    case "onandin":
                    case "all,onandin":
                        // Objects and characters that are INSIDE or ON this object.
                        lst.addAll(getChildObjects(InsideOrOnObject, true).values());
                        lst.addAll(getChildChars(InsideOrOnObject).values());
                        break;
                    case "characters,in":
                        // Characters that are INSIDE this object.
                        lst.addAll(getChildChars(InsideObject).values());
                        break;
                    case "characters,on":
                        // Characters that are ON this object.
                        lst.addAll(getChildChars(WhereChildrenEnum.OnObject).values());
                        break;
                    case "characters,onandin":
                    case "characters":
                        // Characters that are INSIDE or ON this object.
                        lst.addAll(getChildChars(InsideOrOnObject).values());
                        break;
                    case "in":
                        // Objects and characters that are INSIDE this object.
                        lst.addAll(getChildObjects(InsideObject, true).values());
                        lst.addAll(getChildChars(InsideObject).values());
                        break;
                    case "objects,in":
                        // Objects that are INSIDE this object.
                        lst.addAll(getChildObjects(InsideObject, true).values());
                        break;
                    case "objects,on":
                        // Objects that are ON this object.
                        lst.addAll(getChildObjects(WhereChildrenEnum.OnObject, true).values());
                        break;
                    case "objects,onandin":
                    case "objects":
                        // Objects that are INSIDE or ON this object.
                        lst.addAll(getChildObjects(InsideOrOnObject, true).values());
                        break;
                }
                return mAdv.evaluateItemFunction(remainder, lst,
                        null, null, null, resultIsInteger);
            }

            case "Contents": {
                // ----------------------------------------
                //        %object%.Contents(Include)
                //            (Group property)
                // ----------------------------------------
                // Get a group of the characters and/or
                // objects inside this object.
                //
                // The possible values for the optional
                // "Include" argument are:
                //
                //   "All":
                //       Get all objects and characters
                //       inside this object (as well
                //       as any objects/children inside
                //       them).  (Default)
                //
                //   "Characters":
                //       Get all characters inside this
                //       object.
                //
                //   "Objects":
                //       Get all objects inside this
                //       object (as well as any objects/
                //       children inside them).
                //
                ArrayList<MItemWithProperties> lst = new ArrayList<>();
                switch (args.toLowerCase()) {
                    case "":
                    case "all":
                        lst.addAll(getChildObjects(InsideObject, true).values());
                        lst.addAll(getChildChars(InsideObject).values());
                        break;
                    case "characters":
                        lst.addAll(getChildChars(InsideObject).values());
                        break;
                    case "objects":
                        lst.addAll(getChildObjects(InsideObject, true).values());
                        break;
                }
                return mAdv.evaluateItemFunction(remainder, lst,
                        null, null, null, resultIsInteger);
            }

            case "Description":
                // ----------------------------------------
                //          %object%.Description
                //            (Text property)
                // ----------------------------------------
                // This function prints the contents of the
                // "Description" text box on the main page
                // of an object, as one or more lines of
                // text. No further functions can be appended
                // to this function.
                //
                // If the text box has Alternate Descriptions
                // then the text that is printed will be
                // effected by the restrictions and composition
                // setting of each alternate description.
                return getDescription().toString();

            case "Location": {
                // ----------------------------------------
                //           %object%.Location
                //           (Group property)
                // ----------------------------------------
                //
                ArrayList<MItemWithProperties> lst = new ArrayList<>();
                for (String locKey : getRootLocations().keySet()) {
                    lst.add(mAdv.mLocations.get(locKey));
                }
                return mAdv.evaluateItemFunction(remainder, lst,
                        null, null, null, resultIsInteger);
            }

            case "Name":
            case "List":
                // ----------------------------------------
                //          %object%.Name(Article)
                //          %object%.List(Article)
                //              (Text property)
                // ----------------------------------------
                // This function returns the combined
                // contents of the "Article",
                // "Prefix/Adjective" and "Name/Noun(s)"
                // fields on the description page of an
                // object. If the Name/Noun(s) field
                // contains multiple entries on different
                // lines, then this function will use the
                // one at the top of the list.
                //
                // No further functions can be appended to
                // this function.
                //
                // The possible values for the optional
                // "Article" argument are:
                //
                //   "Definite" or "The":
                //       Force the use of the definite
                //       article "the". (default)
                //
                //   "Indefinite":
                //       Force the use of the indefinite
                //       article "a".
                //
                //   "None":
                //       Don't use any article.
                //
                MGlobals.ArticleTypeEnum article = Definite;
                String largs = args.toLowerCase();
                if (largs.contains("indefinite")) {
                    article = Indefinite;
                }
                if (largs.contains("none")) {
                    article = None;
                }
                return getFullName(article);

            case "Noun":
                // ----------------------------------------
                //            %object%.Noun
                //           (Text property)
                // ----------------------------------------
                // Returns the first name of this object.
                return getNames().get(0);

            case "Parent":
                // ----------------------------------------
                //            %object%.Parent
                //           (Object property)
                // ----------------------------------------
                // The location, character or object that is
                // directly holding this object.
                String parentKey = getParent();
                MItemFunctionEvaluator item = mAdv.mObjects.get(parentKey);
                if (item == null) {
                    item = mAdv.mCharacters.get(parentKey);
                }
                if (item == null) {
                    item = mAdv.mLocations.get(parentKey);
                }
                return mAdv.evaluateItemFunction(remainder, null,
                        null, null, item, resultIsInteger);

            default:
                // Any other valid property not already
                // covered above.
                return mAdv.evaluateItemProperty(funcName, getProperties(),
                        mAdv.mObjectProperties, remainder, resultIsInteger);
        }
    }

    @NonNull
    public MStringArrayList getNames() {
        return mNames;
    }

    private void setNames(@NonNull MStringArrayList value) {
        mNames = value;
    }

    /**
     * Get the plural form(s) of this object's name(s).
     *
     * @return an array containing the pluralised name(s).
     */
    @NonNull
    private MStringArrayList getPlurals() {
        if (mPlurals == null) {
            mPlurals = new MStringArrayList();
            for (String name : getNames()) {
                mPlurals.add(guessPluralNoun(name));
            }
        }
        return mPlurals;
    }

    /**
     * A static object is something that normally remains at one location, such as roads,
     * fences, and furniture, but also objects which are firmly attached to something
     * else, such as the knob on a door or the node on somebody's face.
     * <p>
     * A dynamic object is anything that can be picked up and carried around
     * from place to place.
     *
     * @return whether the object is static or not
     */
    public boolean isStatic() {
        if (mIsStatic != null) {
            return mIsStatic;
        } else {
            return !getPropertyValue(PKEY_STATIC_OR_DYN).equals(PVAL_DYNAMIC);
        }
    }

    private void setStatic(boolean value) {
        setPropertyValue(PKEY_STATIC_OR_DYN, value ? PVAL_STATIC : PVAL_DYNAMIC);
        mIsStatic = value;
    }

    void setProperty(@NonNull String propName, @NonNull String propValue) {
        switch (propName) {
            case OBJECTARTICLE: {
                String result = mAdv.evaluateStringExpression(propValue, MParser.mReferences);
                if (result.equals("") && !propValue.equals("")) {
                    result = propValue;
                }
                setArticle(result);
                break;
            }
            case OBJECTPREFIX: {
                String result = mAdv.evaluateStringExpression(propValue, MParser.mReferences);
                if (result.equals("") && !propValue.equals("")) {
                    result = propValue;
                }
                setPrefix(result);
                break;
            }
            case OBJECTNOUN: {
                String result = mAdv.evaluateStringExpression(propValue, MParser.mReferences);
                if (result.equals("") && !propValue.equals("")) {
                    result = propValue;
                }
                getNames().set(0, result);
                break;
            }
            default:
                MProperty prop = null;
                if (hasProperty(propName)) {
                    prop = getProperty(propName);
                }
                if (prop == null) {
                    // Property doesn't already exist for this object.
                    // We want to add it
                    if (mAdv.mObjectProperties.containsKey(propName)) {
                        prop = mAdv.mObjectProperties.get(propName).clone();
                        prop.setSelected(true);
                        addProperty(prop);
                        switch (prop.getType()) {
                            case SelectionOnly:
                                // Nothing more to do
                                break;
                            case ObjectKey:
                                String obKey = propValue;
                                if (obKey.startsWith("Referenced")) {
                                    obKey = MParser.mReferences.getReference(obKey);
                                }
                                setPropertyValue(prop.getKey(), obKey != null ? obKey : "");
                                break;
                        }
                    } else {
                        debugPrint(MGlobals.ItemEnum.Task, getKey(),
                                MView.DebugDetailLevelEnum.Error,
                                "Can't select property " + propName + " for object " +
                                        getCommonName() + " as that property doesn't exist in the global object properties.");
                    }
                } else {
                    // Property already exists for this object
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
                }
                break;
        }
    }

    public boolean hasSurface() {
        return !getPropertyValue(PKEY_HAS_SURFACE).equals("");
    }

    public boolean isPlural() {
        return getArticle().equals("some");
    }

    public boolean isContainer() {
        return !getPropertyValue(PKEY_IS_CONTAINER).equals("");
    }

    public boolean isWearable() {
        return !getPropertyValue(PKEY_IS_WEARABLE).equals("");
    }

    boolean isExplicitlyList() {
        return hasProperty(PKEY_LIST);
    }

    private void setExplicitlyList(boolean value) {
        setPropertyValue(PKEY_LIST, value);
    }

    @NonNull
    String getListDescription() {
        return getPropertyValue(isStatic() ? PKEY_LIST_DESC : PKEY_LIST_DESC_DYN);
    }

    private void setListDescription(@NonNull String value) {
        setPropertyValue(isStatic() ? PKEY_LIST_DESC : PKEY_LIST_DESC_DYN, value);
    }

    boolean isExplicitlyExclude() {
        return hasProperty(PKEY_EXCLUDE);
    }

    private void setExplicitlyExclude(boolean value) {
        setPropertyValue(PKEY_EXCLUDE, value);
    }

    public boolean isLieable() {
        return hasProperty(PKEY_IS_LIEABLE);
    }

    private void setLieable(boolean value) {
        setPropertyValue(PKEY_IS_LIEABLE, value);
    }

    public boolean isSittable() {
        return hasProperty(PKEY_IS_SITTABLE);
    }

    private void setSittable(boolean value) {
        setPropertyValue(PKEY_IS_SITTABLE, value);
    }

    public boolean isStandable() {
        return hasProperty(PKEY_IS_STANDABLE);
    }

    private void setStandable(boolean value) {
        setPropertyValue(PKEY_IS_STANDABLE, value);
    }

    /**
     * Is this object on a given parent?
     *
     * @param parentKey - the key of the parent.
     * @return TRUE if the object is directly on the parent, or
     * on/in something else that is on it; FALSE otherwise.
     */
    boolean isOn(@NonNull String parentKey) {
        MObjectLocation myLoc = getLocation();
        if ((myLoc.mDynamicExistWhere == OnObject ||
                myLoc.mDynamicExistWhere == InObject) &&
                !getParent().equals("")) {
            if (myLoc.mDynamicExistWhere == OnObject &&
                    getParent().equals(parentKey)) {
                return true;
            } else {
                MObject obParent = mAdv.mObjects.get(getParent());
                return obParent.isOn(parentKey);
            }
        } else {
            return false;
        }
    }

    /**
     * Is this object inside a given parent?
     *
     * @param parentKey - the key of the parent.
     * @return TRUE if the object is directly inside the parent, or
     * on/in something that is inside it; FALSE otherwise.
     */
    boolean isInside(@NonNull String parentKey) {
        MObjectLocation myLoc = getLocation();
        if ((myLoc.mDynamicExistWhere == OnObject ||
                myLoc.mDynamicExistWhere == InObject) &&
                !getParent().equals("")) {
            if (myLoc.mDynamicExistWhere == InObject &&
                    getParent().equals(parentKey)) {
                return true;
            } else {
                MObject obParent = mAdv.mObjects.get(getParent());
                return obParent.isInside(parentKey);
            }
        } else {
            return false;
        }
    }

    /**
     * Does this object exist directly or indirectly at
     * a given location?
     *
     * @param locKey - the location key.
     * @return TRUE if the object is directly or indirectly
     * at the location; FALSE otherwise.
     */
    boolean existsAtLocation(@NonNull String locKey) {
        return existsAtLocation(locKey, false);
    }

    /**
     * Does this object exist at a given location?
     *
     * @param locKey     - the location key.
     * @param directOnly - if TRUE, the object must exist directly
     *                   at the location. If FALSE, the object may
     *                   be indirectly at the location (e.g. held
     *                   by a character who happens to be a the
     *                   location).
     * @return TRUE if the object is at the location, FALSE otherwise.
     */
    boolean existsAtLocation(@NonNull String locKey, boolean directOnly) {
        MObjectLocation myLoc = getLocation();
        String myLocKey = myLoc.getKey();
        if (isStatic()) {
            switch (myLoc.mStaticExistWhere) {
                case AllRooms:
                    return true;
                case NoRooms:
                    return locKey.equals(HIDDEN);
                case LocationGroup:
                    return mAdv.mGroups.get(myLocKey).getArlMembers().contains(locKey);
                case SingleLocation:
                    return locKey.equals(myLocKey);
                case PartOfCharacter:
                    return !directOnly &&
                            mAdv.mCharacters.get(myLocKey).getLocation().getKey().equals(locKey);
                case PartOfObject:
                    return !directOnly &&
                            mAdv.mObjects.get(myLocKey).existsAtLocation(locKey);
            }
        } else {
            switch (myLoc.mDynamicExistWhere) {
                case Hidden:
                    return locKey.equals(HIDDEN);
                case InLocation:
                    return locKey.equals(myLocKey);
                case HeldByCharacter:
                    return !directOnly &&
                            mAdv.mCharacters.get(myLocKey).getLocation().getKey().equals(locKey);
                case InObject:
                    return !directOnly &&
                            mAdv.mObjects.get(myLocKey).existsAtLocation(locKey);
                case OnObject:
                    return !directOnly &&
                            mAdv.mObjects.get(myLocKey).existsAtLocation(locKey);
                case WornByCharacter:
                    return !directOnly &&
                            mAdv.mCharacters.get(myLocKey).getLocation().getKey().equals(locKey);
            }
        }
        return false;
    }

    boolean isTransparent() {
        return false;   // TODO
    }

    public boolean isOpen() {
        String s = getPropertyValue(PKEY_OPEN_STATUS);
        return s.equals("") || s.equals(PVAL_OPEN);
    }

    public void setOpen(boolean value) {
        setPropertyValue(PKEY_OPEN_STATUS, (value ? PVAL_OPEN : PVAL_CLOSED));
    }

    @NonNull
    public String getFullName() {
        return getFullName(Indefinite);
    }

    /**
     * Generates the full name of this object, which is its
     * article (if any) + its prefix + the first entry
     * in its names.
     *
     * @param article - determines the article used by the
     *                generated name. If Definite, will use
     *                "the" (irrespective of the object's actual
     *                article). If Indefinite, will use the
     *                object's actual article. Otherwise will
     *                not use any article.
     * @return the generated name of the object (or "Undefined
     * Object" if the object does not have any names).
     */
    @NonNull
    public String getFullName(MGlobals.ArticleTypeEnum article) {
        if (mNames.size() > 0) {
            StringBuilder ret = new StringBuilder();
            // Append article.
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
            // Append prefix.
            if (!mPrefix.equals("")) {
                ret.append(mPrefix).append(" ");
            }
            // Append first name and return.
            return ret.append(getNames().get(0)).toString();
        } else {
            return "Undefined Object";
        }
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
        if (mDescription.toString(true).equals("")) {
            mDescription.get(0).mDescription =
                    "There is nothing special about " + getFullName(Definite) + ".";
        }
        return mDescription;
    }

    public void setDescription(@NonNull MDescription value) {
        mDescription = value;
    }

    /**
     * Get the actual room(s) an object is in, regardless of
     * its actual location. For example, if the object is being
     * held by a character in a specific room, return that room.
     *
     * @return a hash map containing the rooms (may be empty).
     */
    @NonNull
    public MLocationHashMap getRootLocations() {
        MLocationHashMap ret = new MLocationHashMap(mAdv);
        MObjectLocation myLoc = getLocation();
        String myLocKey = myLoc.getKey();

        if (isStatic()) {
            switch (myLoc.mStaticExistWhere) {
                case NoRooms:
                    // Not in any room. Fall out, to
                    // return an empty hash map.
                    break;
                case AllRooms:
                    // In every room. Return the
                    // hash map of all rooms.
                    return mAdv.mLocations;
                case SingleLocation:
                    // At a single location. Add that
                    // to the hash map.
                    ret.put(myLocKey, mAdv.mLocations.get(myLocKey));
                    break;
                case PartOfCharacter:
                    // Part of a character. If that
                    // character exists and is in a
                    // specific location, add that
                    // location to the hash map.
                    MCharacter ch = mAdv.mCharacters.get(myLocKey);
                    if (ch != null) {
                        MCharacter.MCharacterLocation locChar = ch.getLocation();
                        String locCharKey = locChar.getKey();
                        if (locChar.getExistsWhere() == AtLocation) {
                            ret.put(locCharKey, mAdv.mLocations.get(locCharKey));
                        }
                    }
                    break;
                case PartOfObject:
                    // Part of an object. Return
                    // the root locations of that object.
                    return mAdv.mObjects.get(myLocKey).getRootLocations();
                case LocationGroup:
                    // Part of a location group.
                    // Add all of the locations in that
                    // group to the hash map.
                    for (String key : mAdv.mGroups.get(myLocKey).getArlMembers()) {
                        ret.put(key, mAdv.mLocations.get(key));
                    }
                    break;
            }
        } else {
            switch (myLoc.mDynamicExistWhere) {
                case Hidden:
                    // Hidden. Fall out, to return an
                    // empty hash map.
                    break;
                case InLocation:
                    // In one location. If that is not the
                    // special hidden location or blank,
                    // add it to the list.
                    if (!myLocKey.equals("") && !myLocKey.equals(HIDDEN)) {
                        ret.put(myLocKey, mAdv.mLocations.get(myLocKey));
                    }
                    break;
                case InObject:
                case OnObject:
                    // In or on an object. Return
                    // the root locations of that object.
                    return mAdv.mObjects.get(myLocKey).getRootLocations();
                case HeldByCharacter:
                case WornByCharacter:
                    // Held or worn by a character. If that
                    // character exists and is in a
                    // specific location, add that
                    // location to the hash map.
                    MCharacter ch = mAdv.mCharacters.get(myLocKey);
                    if (ch != null) {
                        MCharacter.MCharacterLocation locChar = ch.getLocation();
                        String locCharKey = locChar.getKey();
                        if (locChar.getExistsWhere() == AtLocation) {
                            ret.put(locCharKey, mAdv.mLocations.get(locCharKey));
                        }
                    }
                    break;
            }
        }
        return ret;
    }

    @NonNull
    public MObjectLocation getLocation() {
        mLocation = new MObjectLocation(mAdv);

        if (isStatic() && hasProperty(PKEY_STATIC_LOC)) {
            switch (getPropertyValue(PKEY_STATIC_LOC)) {
                case NOWHERE:
                case HIDDEN:
                    // Not in any room.
                    mLocation.mStaticExistWhere = NoRooms;
                    break;
                case EVERYWHERE:
                    // In every room.
                    mLocation.mStaticExistWhere = AllRooms;
                    break;
                case PVAL_AT_LOC:
                    // At a single location.
                    mLocation.mStaticExistWhere = SingleLocation;
                    if (hasProperty(PKEY_AT_LOC)) {
                        mLocation.setKey(getPropertyValue(PKEY_AT_LOC));
                    }
                    break;
                case PVAL_PART_OF_CHAR:
                    // Part of a character.
                    mLocation.mStaticExistWhere = PartOfCharacter;
                    if (hasProperty(PKEY_PART_OF_CHAR)) {
                        mLocation.setKey(getPropertyValue(PKEY_PART_OF_CHAR));
                    }
                    break;
                case PVAL_PART_OF_OBJECT:
                    // Part of an object.
                    mLocation.mStaticExistWhere = PartOfObject;
                    if (hasProperty(PKEY_PART_OF_OBJECT)) {
                        mLocation.setKey(getPropertyValue(PKEY_PART_OF_OBJECT));
                    }
                    break;
                case PVAL_AT_LOC_GROUP:
                    // Part of a location group.
                    mLocation.mStaticExistWhere = LocationGroup;
                    if (hasProperty(PKEY_AT_LOC_GROUP)) {
                        mLocation.setKey(getPropertyValue(PKEY_AT_LOC_GROUP));
                    }
                    break;
            }
        } else if (!isStatic() && hasProperty(PKEY_DYNAMIC_LOC)) {
            switch (getPropertyValue(PKEY_DYNAMIC_LOC)) {
                case HIDDEN:
                    // Hidden.
                    mLocation.mDynamicExistWhere = Hidden;
                    break;
                case PVAL_IN_LOCATION:
                    // In one location.
                    mLocation.mDynamicExistWhere = InLocation;
                    if (hasProperty(PKEY_IN_LOCATION)) {
                        mLocation.setKey(getPropertyValue(PKEY_IN_LOCATION));
                    }
                    break;
                case PVAL_IN_OBJECT:
                    // In an object.
                    mLocation.mDynamicExistWhere = InObject;
                    if (hasProperty(PKEY_IN_OBJECT)) {
                        mLocation.setKey(getPropertyValue(PKEY_IN_OBJECT));
                    }
                    break;
                case PVAL_ON_OBJECT:
                    // On an object.
                    mLocation.mDynamicExistWhere = OnObject;
                    if (hasProperty(PKEY_ON_OBJECT)) {
                        mLocation.setKey(getPropertyValue(PKEY_ON_OBJECT));
                    }
                    break;
                case PVAL_HELD_BY:
                    // Held by a character.
                    mLocation.mDynamicExistWhere = HeldByCharacter;
                    if (hasProperty(PKEY_HELD_BY)) {
                        mLocation.setKey(getPropertyValue(PKEY_HELD_BY));
                    }
                    break;
                case PVAL_WORN_BY:
                    // Worn by a character.
                    mLocation.mDynamicExistWhere = WornByCharacter;
                    if (hasProperty(PKEY_WORN_BY)) {
                        mLocation.setKey(getPropertyValue(PKEY_WORN_BY));
                    }
                    break;
            }
        }
        return mLocation;
    }

    public void setLocation(@NonNull MObjectLocation value) {
        mLocation = value;
        if (!isStatic() && hasProperty(PKEY_DYNAMIC_LOC)) {
            switch (mLocation.mDynamicExistWhere) {
                case HeldByCharacter:
                    setPropertyValue(PKEY_DYNAMIC_LOC, PVAL_HELD_BY);
                    addProperty(PKEY_HELD_BY);
                    setPropertyValue(PKEY_HELD_BY, mLocation.getKey());
                    break;
                case Hidden:
                    setPropertyValue(PKEY_DYNAMIC_LOC, HIDDEN);
                    break;
                case InLocation:
                    setPropertyValue(PKEY_DYNAMIC_LOC, PVAL_IN_LOCATION);
                    addProperty(PKEY_IN_LOCATION);
                    setPropertyValue(PKEY_IN_LOCATION, mLocation.getKey());
                    break;
                case InObject:
                    setPropertyValue(PKEY_DYNAMIC_LOC, PVAL_IN_OBJECT);
                    addProperty(PKEY_IN_OBJECT);
                    setPropertyValue(PKEY_IN_OBJECT, mLocation.getKey());
                    break;
                case OnObject:
                    setPropertyValue(PKEY_DYNAMIC_LOC, PVAL_ON_OBJECT);
                    addProperty(PKEY_ON_OBJECT);
                    setPropertyValue(PKEY_ON_OBJECT, mLocation.getKey());
                    break;
                case WornByCharacter:
                    setPropertyValue(PKEY_DYNAMIC_LOC, PVAL_WORN_BY);
                    addProperty(PKEY_WORN_BY);
                    setPropertyValue(PKEY_WORN_BY, mLocation.getKey());
                    break;
            }
        } else if (isStatic() && hasProperty(PKEY_STATIC_LOC)) {
            switch (mLocation.mStaticExistWhere) {
                case AllRooms:
                    setPropertyValue(PKEY_STATIC_LOC, EVERYWHERE);
                    break;
                case LocationGroup:
                    setPropertyValue(PKEY_STATIC_LOC, PVAL_AT_LOC_GROUP);
                    addProperty(PKEY_AT_LOC_GROUP);
                    setPropertyValue(PKEY_AT_LOC_GROUP, mLocation.getKey());
                    break;
                case NoRooms:
                    setPropertyValue(PKEY_STATIC_LOC, HIDDEN);
                    break;
                case PartOfCharacter:
                    setPropertyValue(PKEY_STATIC_LOC, PVAL_PART_OF_CHAR);
                    addProperty(PKEY_PART_OF_CHAR);
                    setPropertyValue(PKEY_PART_OF_CHAR, mLocation.getKey());
                    break;
                case PartOfObject:
                    setPropertyValue(PKEY_STATIC_LOC, PVAL_PART_OF_OBJECT);
                    addProperty(PKEY_PART_OF_OBJECT);
                    setPropertyValue(PKEY_PART_OF_OBJECT, mLocation.getKey());
                    break;
                case SingleLocation:
                    setPropertyValue(PKEY_STATIC_LOC, PVAL_AT_LOC);
                    addProperty(PKEY_AT_LOC);
                    setPropertyValue(PKEY_AT_LOC, mLocation.getKey());
                    break;
            }
        }
    }

    public boolean isOpenable() {
        return hasProperty(PKEY_OPENABLE);
    }

    public boolean isLockable() {
        return hasProperty(PKEY_LOCKABLE);
    }

    public boolean isReadable() {
        return hasProperty(PKEY_READABLE);
    }

    private void setReadText(String value) {
        setPropertyValue(PKEY_READ_TEXT, value);
    }

    public String getLastParent() {
        return mLastParent;
    }

    void setLastParent(@NonNull String value) {
        mLastParent = value;
    }

    /**
     * Get the key of the location, character or object
     * that is directly holding this object.
     *
     * @return the key. If the parent location was
     * indeterminate, this will be an empty string.
     */
    @Override
    @NonNull
    public String getParent() {
        MObjectLocation curLoc = getLocation();
        if (isStatic()) {
            switch (curLoc.mStaticExistWhere) {
                case NoRooms:
                    return HIDDEN;
                case AllRooms:
                    return ALLROOMS;
                case PartOfCharacter:
                case PartOfObject:
                case SingleLocation:
                case LocationGroup:
                    return curLoc.getKey();
            }
        } else {
            switch (curLoc.mDynamicExistWhere) {
                case Hidden:
                    return HIDDEN;
                case HeldByCharacter:
                case InObject:
                case OnObject:
                case WornByCharacter:
                case InLocation:
                    return curLoc.getKey();

            }
        }
        return "";
    }

    /**
     * Generates a regular expression (either in ADRIFT's pseudo regex
     * 'advanced command construction' format, or a proper regular
     * expression) to represent this object. The expression will match
     * against any string containing (optionally) the object's article
     * (or 'the'), zero or more of the object's prefixes and one of
     * the object's names.
     *
     * @param getADRIFTExpr - if TRUE the generated string will
     *                              be in ADRIFT's 'advanced command
     *                              construction' format, if FALSE it
     *                              will be a standard regex pattern.
     * @param usePluralForm    - if TRUE the generated string will use
     *                              the plural forms of this object's names.
     *                              If FALSE it will use the singular names,
     * @return the generated regular expression.
     */
    @Override
    @NonNull
    protected String getRegEx(boolean getADRIFTExpr, boolean usePluralForm) {
        MStringArrayList names =
                usePluralForm ? getPlurals() : getNames();
        if (getADRIFTExpr) {
            // Create an ADRIFT 'advanced command construction' expression.
            StringBuilder ret = new StringBuilder("{" + mArticle + "/the} ");
            if (!mPrefix.equals("")) {
                for (String prefix : mPrefix.split(" ")) {
                    ret.append("{").append(prefix).append("} ");
                }
            }
            ret.append("[");
            for (String name : names) {
                ret.append(name).append("/");
            }
            return ret.substring(0, ret.length() - 1) + "]";
        } else {
            // Create a real regular expression.
            StringBuilder ret = new StringBuilder("(" + mArticle + " |the )?");
            for (String prefix : mPrefix.split(" ")) {
                if (!prefix.equals("")) {
                    ret.append("(").append(Pattern.quote(prefix)).append(" )?");
                }
            }
            ret.append("(");
            for (String name : names) {
                ret.append(Pattern.quote(name)).append("|");
            }
            if (names.size() == 0) {
                // Fudge
                ret.append("|");
            }
            return ret.substring(0, ret.length() - 1) + ")";
        }
    }

    // ---------------------------------------------------
    //                  VISIBILITY
    // ---------------------------------------------------

    /**
     * Has this object ever been seen by the given character?
     *
     * @param chKey - the character's key.
     * @return TRUE if the character has ever seen this
     * object, FALSE otherwise.
     */
    public boolean hasBeenSeenBy(@NonNull String chKey) {
        if (chKey.equals(THEPLAYER)) {
            chKey = mAdv.getPlayer().getKey();
        }
        MCharacter ch = mAdv.mCharacters.get(chKey);
        return (ch != null) && ch.hasSeenObject(getKey());
    }

    void setHasBeenSeenBy(@NonNull String chKey) {
        MCharacter ch = mAdv.mCharacters.get(chKey);
        if (ch != null) {
            ch.setHasSeenObject(getKey(), true);
        }
    }

    /**
     * Is this object currently visible to the given character?
     *
     * @param chKey - the character's key.
     * @return TRUE if the character can currently see this
     * object, FALSE otherwise.
     */
    public boolean isVisibleTo(@NonNull String chKey) {
        if (chKey.equals(THEPLAYER)) {
            chKey = mAdv.getPlayer().getKey();
        }
        try {
            String myBoundVisible = getBoundVisible();
            if (myBoundVisible.equals(HIDDEN)) {
                return false;
            }
            MCharacter ch = mAdv.mCharacters.get(chKey);
            if (ch == null) {
                return false;
            }
            String charBoundVisible = ch.getBoundVisible();
            switch (myBoundVisible) {
                case ALLROOMS:
                    return mAdv.mLocations.containsKey(charBoundVisible);
                default:
                    MGroup grp = mAdv.mGroups.get(myBoundVisible);
                    return (grp != null) ?
                            grp.getArlMembers().contains(charBoundVisible) :
                            myBoundVisible.equals(charBoundVisible);
            }
        } catch (Exception e) {
            // The game has placed this object in an illogical state, e.g.
            // the object is inside itself. Try to help the user to recover
            // by at least allowing them to see the object.
            GLKLogger.error("object isVisibleTo exception: " + e.getMessage());
            return true;
        }
    }

    /**
     * Is this object currently visible to characters at the
     * given location?
     *
     * @param locKey - the location's key.
     * @return TRUE if characters at the location can see
     * this object, FALSE otherwise.
     */
    boolean isVisibleAt(@NonNull String locKey) {
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
            // The game has placed this object in an illogical state, e.g.
            // the object is inside itself. Try to help the user to recover
            // by at least allowing them to see the object.
            GLKLogger.error("object isVisibleAt exception: " + e.getMessage());
            return true;
        }
    }

    /**
     * Get the key of the maximum container within which this
     * object can be seen.
     *
     * @return the key.
     */
    @NonNull
    String getBoundVisible() throws Exception {
        MObjectLocation myLoc = getLocation();
        String myLocKey = myLoc.getKey();

        if (isStatic()) {
            switch (myLoc.mStaticExistWhere) {
                case AllRooms:
                    // Object is visible in every
                    // location.
                    return ALLROOMS;

                case NoRooms:
                    // Object is not visible in any
                    // location.
                    return HIDDEN;

                case LocationGroup:
                case SingleLocation:
                    // Object is visible anywhere
                    // in a given location group or
                    // single location.
                    return myLocKey;

                case PartOfCharacter:
                    // Object is visible wherever the
                    // character it is part of is visible.
                    MCharacter ch = mAdv.mCharacters.get(myLocKey);
                    if (ch == null) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is part of non-existent character '" + myLocKey + "'.");
                    } else if (ch.getLocation().getKey().equals(getKey())) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which part of character '" + myLocKey +
                                "', who is " + ch.getLocation().getExistsWhere() +
                                " same object. Avoiding infinite loop.");
                    }
                    return ch.getBoundVisible();

                case PartOfObject:
                    // Object is visible wherever the object
                    // it is part of is visible.
                    MObject obj = mAdv.mObjects.get(myLocKey);
                    if (obj == null) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is part of non-existent object '" + myLocKey + "'.");
                    } else if (obj.getKey().equals(getKey())) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is part of itself. Avoiding infinite loop.");
                    }
                    return obj.getBoundVisible();
            }
        } else {
            switch (myLoc.mDynamicExistWhere) {
                case Hidden:
                    // Object is not visible in any
                    // location.
                    return HIDDEN;

                case InLocation:
                    // Object is visible in a single
                    // location.
                    return myLocKey;

                case HeldByCharacter: {
                    // Object is visible wherever the
                    // character holding it is visible.
                    MCharacter ch = mAdv.mCharacters.get(myLocKey);
                    if (ch == null) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is held by non-existent character '" + myLocKey + "'.");
                    } else if (ch.getLocation().getKey().equals(getKey())) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is held by character '" + myLocKey +
                                "', who is " + ch.getLocation().getExistsWhere() +
                                " same object. Avoiding infinite loop.");
                    }
                    return ch.getBoundVisible();
                }

                case WornByCharacter: {
                    // Object is visible wherever the
                    // character wearing it is visible.
                    MCharacter ch = mAdv.mCharacters.get(myLocKey);
                    if (ch == null) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is worn by non-existent character '" + myLocKey + "'.");
                    } else if (ch.getLocation().getKey().equals(getKey())) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is worn by character '" + myLocKey +
                                "', who is " + ch.getLocation().getExistsWhere() +
                                " same object. Avoiding infinite loop.");
                    }
                    return ch.getBoundVisible();
                }

                case InObject: {
                    // Object is visible wherever the object
                    // containing it is visible IF that container
                    // is either NOT openable (e.g. a bookcase) OR
                    // currently opened OR transparent. Otherwise
                    // it is only visible to characters that are
                    // also inside that same object.
                    MObject ob = mAdv.mObjects.get(myLocKey);
                    if (ob == null) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is in non-existent object '" + myLocKey + "'.");
                    } else if (ob.getKey().equals(getKey())) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is in itself. Avoiding infinite loop.");
                    }
                    if (!ob.isOpenable() || ob.isOpen() || ob.isTransparent()) {
                        return ob.getBoundVisible();
                    } else {
                        return myLocKey;
                    }
                }

                case OnObject: {
                    // Object is visible wherever the object it
                    // is on is visible.
                    MObject ob = mAdv.mObjects.get(myLocKey);
                    if (ob == null) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is on non-existent object '" + myLocKey + "'.");
                    } else if (ob.getKey().equals(getKey())) {
                        throw new Exception("getBoundsVisible called on object '" + getKey() +
                                "', which is on itself. Avoiding infinite loop.");
                    }
                    return ob.getBoundVisible();
                }
            }
        }
        return HIDDEN;
    }

    boolean isHeldByAnyone() {
        switch (getLocation().mDynamicExistWhere) {
            case HeldByCharacter:
                return true;
            case Hidden:
            case InLocation:
            case WornByCharacter:
                return false;
            case InObject:
            case OnObject:
                return mAdv.mObjects.get(getLocation().getKey()).isHeldByAnyone();

        }
        // Static objects can never be held by anyone.
        return false;
    }

    boolean isWornByAnyone() {
        switch (getLocation().mDynamicExistWhere) {
            case WornByCharacter:
                return true;
            case HeldByCharacter:
            case Hidden:
            case InLocation:
            case InObject:
            case OnObject:
                return false;
        }
        // Static objects can never be worn by anyone.
        return false;
    }

    /**
     * Move this object to a new location.
     *
     * @param dest - the location to move to.
     */
    public void moveTo(@NonNull MObjectLocation dest) {
        // If we're moving into or onto an object, make sure we're not
        // going recursive.
        final String myKey = getKey();

        if (dest.mDynamicExistWhere == InObject ||
                dest.mDynamicExistWhere == OnObject) {
            String destKey = dest.getKey();
            if (destKey.equals(myKey) ||
                    mAdv.mObjects.get(myKey).getChildObjects(Everything).containsKey(destKey)) {
                MGlobals.displayError("Can't move object " + getFullName() + " " +
                        (dest.mDynamicExistWhere == InObject ? "inside" : "onto") +
                        " " + mAdv.mObjects.get(destKey).getFullName() +
                        " as that would create a recursive location.");
                return;
            }
        }

        mLastParent = getParent();
        setLocation(dest);

        // Update any 'seen' things
        if (!myKey.equals("")) {
            // TCC: following code seems more efficient than the original
            // code commented out below. But is it equivalent?
            // Test carefully...!
            for (MCharacter ch : mAdv.mCharacters.values()) {
                if (!ch.hasSeenObject(myKey) && ch.canSeeObject(myKey)) {
                    ch.setHasSeenObject(myKey, true);
                }
            }
         /* for (MLocation loc : getRootLocations().values()) {
                if (loc != null && isVisibleAt(loc.getKey())) {
                    for (MCharacter ch : mAdv.mLocations.get(loc.getKey()).getCharactersVisibleAtLocation().values()) {
                        ch.setHasSeenObject(getKey(), true);
                    }
                }
            } */
        }
    }

    public void fixPropertyValues(int iStartObs, double v) {
        // Replace the index values stored in each object property with the
        // actual corresponding object key.
        if (isLockable()) {
            setPropertyValue(PKEY_LOCK_KEY,
                    MFileOlder.getObjectKey(mAdv,
                            cint(getPropertyValue(PKEY_LOCK_KEY)) + iStartObs,
                            Dynamic));
        }

        if (hasProperty(PKEY_ON_OBJECT)) {
            setPropertyValue(MObject.PKEY_ON_OBJECT,
                    MFileOlder.getObjectKey(mAdv,
                            cint(getPropertyValue(PKEY_ON_OBJECT)),
                            Surface));
        }

        if (hasProperty(PKEY_IN_OBJECT)) {
            if (v >= 3.9) {
                setPropertyValue(PKEY_IN_OBJECT,
                        MFileOlder.getObjectKey(mAdv,
                                cint(getPropertyValue(PKEY_IN_OBJECT)),
                                Container));
            } else {
                // Version 3.8 games didn't distinguish between InsideWhat and OnWhat.
                // By the time we reach this point in execution InsideWhat could
                // actually be either. To work out which it should be, we look
                // at the type of the object's parent - if it's a surface, assume
                // the position should be OnWhat; otherwise stay with InsideWhat.
                MProperty prop = getProperty(PKEY_IN_OBJECT);
                String pKey = MFileOlder.getObjectKey(mAdv,
                        cint(getPropertyValue(PKEY_IN_OBJECT)),
                        SurfaceContainer);

                // If parent is a surface, change this object's position to OnWhat.
                if (mAdv.mObjects.get(pKey).hasSurface()) {
                    removeProperty(MObject.PKEY_IN_OBJECT);
                    MObject.MObjectLocation obLoc = new MObject.MObjectLocation(mAdv);
                    obLoc.mDynamicExistWhere = OnObject;
                    setLocation(obLoc);
                    prop = mAdv.mAllProperties.get(PKEY_ON_OBJECT).copy();
                    addProperty(prop);
                }

                assert prop != null;
                prop.setValue(pKey);
            }
        }
    }

    @NonNull
    public MObjectHashMap getChildObjects(WhereChildrenEnum where) {
        return getChildObjects(where, false);
    }

    /**
     * Get the descendant objects of this object. These are
     * objects that are inside, on or part of this object (and
     * optionally their descendants).
     *
     * @param where      - if Everything, returns all child objects.
     *                   Otherwise can filter by setting this as
     *                   InsideOrOnObject, InsideObject or OnObject.
     * @param bRecursive - if TRUE, the returned hash map will
     *                   also include all descendants of
     *                   objects and characters on and/or in
     *                   (depending upon the value of Where)
     *                   this object. If FALSE, will only
     *                   return the immediate children.
     * @return the group of child or descendant objects.
     */
    @NonNull
    public MObjectHashMap getChildObjects(WhereChildrenEnum where, boolean bRecursive) {
        MObjectHashMap ret = new MObjectHashMap(mAdv);

        for (MObject ob : mAdv.mObjects.values()) {
            MObjectLocation obLoc = ob.getLocation();
            MObjectLocation.DynamicExistsWhereEnum dynWhere = obLoc.mDynamicExistWhere;
            MObjectLocation.StaticExistsWhereEnum statWhere = obLoc.mStaticExistWhere;

            // Check whether this object matches the caller's filter.
            boolean bCheckSubObject = false;
            switch (where) {
                case Everything:
                    // Caller wants to get all child objects of
                    // this object. The current object may be
                    // one of those if it has the property "in" or
                    // "on" another object (for dynamic objects) or
                    // "part of" another object (for static objects).
                    bCheckSubObject =
                            (dynWhere == InObject || dynWhere == OnObject ||
                                    statWhere == PartOfObject);
                    break;
                case InsideOrOnObject:
                    // Caller wants to get child objects in or on
                    // this object (for dynamic objects). Check
                    // if current object has "in" or "on" property.
                    bCheckSubObject =
                            (dynWhere == InObject || dynWhere == OnObject);
                    break;
                case InsideObject:
                    // Caller wants to get child objects in this
                    // object (if this object is dynamic). Check
                    // if current object has "in" property.
                    bCheckSubObject = (dynWhere == InObject);
                    break;
                case OnObject:
                    // Caller wants to get child objects on this
                    // object (if this object is dynamic). Check
                    // if current object has "on" property.
                    bCheckSubObject = (dynWhere == OnObject);
                    break;
            }

            if (bCheckSubObject) {
                // Ok, so the current object has a property matching
                // what the user wants. Now check if its location is
                // this object (i.e. its actually a child).
                if (obLoc.getKey().equals(getKey())) {
                    // Yes, so add it to the return hash map.
                    ret.put(ob.getKey(), ob);
                    if (bRecursive) {
                        // We also need to add all of the child objects
                        // of this child object.
                        for (MObject obChild :
                                ob.getChildObjects(Everything, true).values()) {
                            ret.put(obChild.getKey(), obChild);
                        }
                    }
                }
            }
        }

        if (bRecursive) {
            // We also need to check all characters inside or on the object to
            // include any child objects of theirs (e.g. objects they are wearing
            // or holding).
            for (MCharacter ch : mAdv.mCharacters.values()) {
                MCharacter.MCharacterLocation chLoc = ch.getLocation();
                MCharacter.MCharacterLocation.ExistsWhere chWhere = chLoc.getExistsWhere();
                // Only consider this character if it matches the caller's filter (e.g.
                // caller wants items on the object and this character is not in the object).
                if ((where != WhereChildrenEnum.OnObject &&
                        chWhere == MCharacter.MCharacterLocation.ExistsWhere.InObject) ||
                        (where != InsideObject &&
                                chWhere == MCharacter.MCharacterLocation.ExistsWhere.OnObject)) {
                    // Ok, so the current character has a property matching
                    // what the user wants. Now check if its location is
                    // this object (i.e. its actually a child).
                    if (chLoc.getKey().equals(getKey())) {
                        // Yes, so add its child objects to the
                        // return hash map.
                        for (MObject childOb : ch.getChildObjects(true).values()) {
                            ret.put(childOb.getKey(), childOb);
                        }
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Construct a string listing all the objects immediately inside
     * and on this object.
     * <p>
     * The string will have the form "[XX, XX and XX is/are on
     * the (full object name)][, and inside|.|Inside (full object name)
     * is/are XX, XX and XX.]" depending upon whether objects are on
     * and/or in the object.
     * <p>
     * Otherwise the string will be "Nothing is on or inside
     * the (full object name)."
     *
     * @return the string.
     */
    @NonNull
    public String displayChildObjects() {
        StringBuilder ret = new StringBuilder();
        String myName = getFullName(Definite);

        // Are there any objects on this object?
        MObjectHashMap obsOn = getChildObjects(WhereChildrenEnum.OnObject);
        int nObOn = obsOn.size();
        boolean isObOn = (nObOn > 0);
        if (isObOn) {
            // Yes - add descriptions of all objects on this object
            // to the output text.
            StringBuilder sb = new StringBuilder(obsOn.toList("and", false, Indefinite));
            toProper(sb);
            ret.append(sb);
            ret.append(nObOn == 1 ? " is on " : " are on ");
            ret.append(myName);
        }

        // Is the object either unopenable or open, and contains other objects?
        boolean isObIn = false;
        boolean isUnopenableOrOpen = !isOpenable() || isOpen();
        if (isUnopenableOrOpen) {
            MObjectHashMap obsIn = getChildObjects(InsideObject);
            int nObIn = obsIn.size();
            if (nObIn > 0) {
                // Yes - add descriptions of all objects inside this object
                // to the output text.
                isObIn = true;
                if (isObOn) {
                    ret.append(", and inside");
                } else {
                    ret.append("Inside ").append(myName);
                }
                ret.append(nObIn == 1 ? " is " : " are ");
                ret.append(obsIn.toList("and", false, Indefinite));
            }
        }

        // Complete the sentence
        if (isObOn || isObIn) {
            // We've already appended descriptions of objects and/or in this object, so
            // just terminate the sentence.
            ret.append(".");
        } else if (isUnopenableOrOpen) {
            // The object is either unopenable or currently open, but there is nothing on
            // it and nothing inside it.
            ret.append("Nothing is on or inside ").append(myName).append(".");
        }

        return ret.toString();
    }

    /**
     * Get the child characters of this object. These
     * are characters that are immediately on and/or
     * in the object.
     *
     * @param where - if Everything, returns all child
     *              characters. Otherwise can filter by
     *              setting this as InsideOrOnObject,
     *              InsideObject or OnObject.
     * @return the group of child characters.
     */
    @NonNull
    public MCharacterHashMap getChildChars(WhereChildrenEnum where) {
        MCharacterHashMap ret = new MCharacterHashMap(mAdv);
        for (MCharacter ch : mAdv.mCharacters.values()) {
            MCharacter.MCharacterLocation chLoc = ch.getLocation();
            MCharacter.MCharacterLocation.ExistsWhere chWhere = chLoc.getExistsWhere();
            if ((where != WhereChildrenEnum.OnObject &&
                    chWhere == MCharacter.MCharacterLocation.ExistsWhere.InObject) ||
                    (where != InsideObject &&
                            chWhere == MCharacter.MCharacterLocation.ExistsWhere.OnObject)) {
                if (chLoc.getKey().equals(getKey())) {
                    ret.put(ch.getKey(), ch);
                }
            }
        }
        return ret;
    }

    /**
     * Construct a string listing all the characters
     * immediately on and/or inside this object.
     * <p>
     * The string will have the form "[XX, XX and XX is/are on
     * the (full object name)][, and|.][XX, XX and XX is/are inside
     * the (full object name)]" depending upon whether characters
     * are on and/or in the object.
     * <p>
     * If there are no characters inside or on the object, returns
     * an empty string.
     *
     * @return the string.
     */
    @NonNull
    public String displayChildChars() {
        StringBuilder ret = new StringBuilder();

        // Are there any characters standing on this object?
        MCharacterHashMap chsOn = getChildChars(WhereChildrenEnum.OnObject);
        int nChOn = chsOn.size();
        boolean isChOn = (nChOn > 0);
        if (isChOn) {
            // Yes - add descriptions of all characters on this object
            // to the output text.
            ret.append("%PCase[").append(chsOn.toList("and")).append("]%");
            ret.append(nChOn == 1 ? " [am/are/is] on " : " are on ");
            ret.append(getFullName(Definite));
        }

        // Is the object either unopenable or open, and contains characters?
        boolean isUnopenableOrOpen = !isOpenable() || isOpen();
        boolean isChIn = false;
        if (isUnopenableOrOpen) {
            MCharacterHashMap chsIn = getChildChars(InsideObject);
            int nChIn = chsIn.size();
            if (nChIn > 0) {
                // Yes - add descriptions of all characters inside this object
                // to the output text.
                isChIn = true;
                if (isChOn) {
                    ret.append(", and ").append(chsIn.toList("and"));
                } else {
                    ret.append("%PCase[")
                            .append(chsIn.toList("and")).append("]%");
                }
                ret.append(nChIn == 1 ? " [am/are/is] " : " are ");
                ret.append("inside ").append(getFullName(Definite));
            }
        }

        // Complete the sentence
        if (isChOn || (isUnopenableOrOpen && isChIn)) {
            ret.append(".");
        }

        return ret.toString();
    }

    @Nullable
    @Override
    public MObject clone() {
        MObject ob = (MObject) super.clone();
        ob.setLocalProperties(ob.getLocalProperties().clone());
        ob.setNames(ob.getNames().clone());
        return ob;
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getFullName();
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> all = new ArrayList<>();
        all.add(getDescription());
        for (MProperty p : getLocalProperties().values()) {
            all.add(p.getStringData());
        }
        return all;
    }

    @Override
    public int findLocal(@NonNull String toFind, @Nullable String toReplace,
                         boolean findAll, @NonNull int[] nReplaced) {
        int iCount = nReplaced[0];
        String[] t = new String[1];
        t[0] = mArticle;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        t[0] = mPrefix;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        for (int i = getNames().size() - 1; i >= 0; i--) {
            t[0] = getNames().get(i);
            nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        }
        return nReplaced[0] - iCount;
    }

    @Override
    protected MGroup.GroupTypeEnum getPropertyGroupType() {
        return MGroup.GroupTypeEnum.Objects;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int iCount = 0;
        for (MDescription d : getAllDescriptions()) {
            iCount += d.referencesKey(key);
        }
        iCount += getLocalProperties().getNumberOfKeyRefs(key);
        return iCount;
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (MDescription d : getAllDescriptions()) {
            d.deleteKey(key);
        }

        if (!getLocalProperties().deleteKey(key)) {
            return false;
        }

        // TODO - Need to do something clever here.  E.g. if we have properties Where=InLocation then
        // Location = theroom, we need to set Where=Hidden and remove Location property

        return true;
    }

    public enum WhereChildrenEnum {
        InsideOrOnObject,   // 0
        InsideObject,       // 1
        OnObject,           // 2
        Everything          // 3, Includes objects that are part of this object
    }

    public static class MObjectLocation {
        @NonNull
        private final MAdventure mAdv;
        public DynamicExistsWhereEnum mDynamicExistWhere = Hidden;
        public StaticExistsWhereEnum mStaticExistWhere = NoRooms;
        @NonNull
        private String mKey = "";

        public MObjectLocation(@NonNull MAdventure adv) {
            mAdv = adv;
        }

        static StaticExistsWhereEnum toStaticExistsWhere(int value) {
            switch (value) {
                default:
                case 0:
                    return NoRooms;
                case 1:
                    return SingleLocation;
                case 2:
                    return LocationGroup;
                case 3:
                    return AllRooms;
                case 4:
                    return PartOfCharacter;
                case 5:
                    return PartOfObject;
            }
        }

        @NonNull
        public String getKey() {
            if (mKey.equals(THEPLAYER) && mAdv.getPlayer() != null) {
                mKey = mAdv.getPlayer().getKey();
            }
            return mKey;
        }

        public void setKey(@NonNull String value) {
            mKey = value;
        }

        @NonNull
        public MObjectLocation copy() {
            MObjectLocation loc = new MObjectLocation(mAdv);
            loc.setKey(mKey);
            loc.mDynamicExistWhere = mDynamicExistWhere;
            loc.mStaticExistWhere = mStaticExistWhere;
            return loc;
        }

        public enum DynamicExistsWhereEnum {
            Hidden,             // 0
            InLocation,         // 1
            InObject,           // 2
            OnObject,           // 3
            HeldByCharacter,    // 4
            WornByCharacter     // 5
        }

        public enum StaticExistsWhereEnum {
            NoRooms,            // 0
            SingleLocation,     // 1
            LocationGroup,      // 2
            AllRooms,           // 3
            PartOfCharacter,    // 4
            PartOfObject        // 5
        }
    }
}