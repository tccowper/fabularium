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

package com.luxlunae.bebek.model.io;

import android.support.annotation.NonNull;

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.controller.MDebugger;
import com.luxlunae.bebek.model.MALR;
import com.luxlunae.bebek.model.MAction;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MDescription;
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.MGroup;
import com.luxlunae.bebek.model.MHint;
import com.luxlunae.bebek.model.MLocation;
import com.luxlunae.bebek.model.MObject;
import com.luxlunae.bebek.model.MProperty;
import com.luxlunae.bebek.model.MRestriction;
import com.luxlunae.bebek.model.MSingleDescription;
import com.luxlunae.bebek.model.MSynonym;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.model.MUserFunction;
import com.luxlunae.bebek.model.MVariable;
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;
import com.luxlunae.bebek.model.collection.MStringArrayList;
import com.luxlunae.bebek.model.collection.MStringHashMap;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import static com.luxlunae.bebek.MGlobals.CHARACTERPROPERNAME;
import static com.luxlunae.bebek.MGlobals.LONGLOCATIONDESCRIPTION;
import static com.luxlunae.bebek.MGlobals.OBJECTARTICLE;
import static com.luxlunae.bebek.MGlobals.OBJECTNOUN;
import static com.luxlunae.bebek.MGlobals.OBJECTPREFIX;
import static com.luxlunae.bebek.MGlobals.SHORTLOCATIONDESCRIPTION;
import static com.luxlunae.bebek.MGlobals.getBool;
import static com.luxlunae.bebek.model.MCharacter.CharacterType.NonPlayer;
import static com.luxlunae.bebek.model.MProperty.PropertyOfEnum.Characters;
import static com.luxlunae.bebek.model.MProperty.PropertyOfEnum.Locations;
import static com.luxlunae.bebek.model.MProperty.PropertyOfEnum.Objects;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.StateList;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.Text;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.ValueList;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.All;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.AllExceptProperties;
import static com.luxlunae.bebek.model.io.MFileIO.LoadWhatEnum.Properties;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

class MFile500 {
    private static final boolean ADD_DUPLICATE_KEYS = true;

    static boolean load500(@NonNull MAdventure adv,
                           @NonNull byte[] xmlBytes, boolean isLibrary) {
        return load500(adv, xmlBytes, isLibrary, All);
    }

    static boolean load500(@NonNull MAdventure adv,
                           @NonNull byte[] xmlBytes, boolean isLibrary,
                           @NonNull MFileIO.LoadWhatEnum loadWhat) {
        long msStart = System.currentTimeMillis();

        try {
            // Ignore any initial guff in the byte array
            int i = 0;
            while (i < xmlBytes.length && xmlBytes[i] < 0) {
                i++;
            }
            if (i == xmlBytes.length) {
                throw new IOException("No XML found!");
            }

            if (loadWhat == All) {
                MRestrictionArrayList.mAskedAboutBrackets = false;
                MFileIO.mCorrectedTasksCount = 0;
            }
            if (!isLibrary) {
                adv.mBlorbMappings.clear();
            }

            // Parse the XML file in one pass
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            BufferedReader xml = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(xmlBytes, i, xmlBytes.length - i)));
            xpp.setInput(xml);
            readXML(adv, xpp, isLibrary, loadWhat);

            long msEnd = System.currentTimeMillis();
            GLKLogger.debug("Took " + (msEnd - msStart) + " ms to load adventure.");
            return true;

        } catch (Exception ex) {
            if (ex.getMessage().contains("Root element is missing")) {
                adv.mView.errMsg("The file you are trying to load is not a valid ADRIFT v5.0 file.");
            } else {
                adv.mView.errMsg("Error loading Adventure", ex);
            }
            return false;
        }
    }

    private static void readXML(@NonNull MAdventure adv,
                                @NonNull XmlPullParser xpp, boolean isLibrary,
                                @NonNull MFileIO.LoadWhatEnum loadWhat) throws Exception {

        MStringHashMap dupTaskKeys = new MStringHashMap();
        MStringArrayList newTasks = new MStringArrayList();

        // Try to find the root node
        int evType;
        boolean foundRoot = false;
        while ((evType = xpp.nextTag()) != END_DOCUMENT) {
            if (evType == START_TAG) {
                if (xpp.getName().equals("Adventure")) {
                    foundRoot = true;
                    break;
                }
            }
        }
        if (!foundRoot) {
            throw new Exception("Root element is missing");
        }

        xpp.require(START_TAG, null, "Adventure");

        int depth = xpp.getDepth();
        boolean itemsStarted = false;        // some tags must be loaded before others
        double version = 0;

        // Load the adventure
        while ((evType = xpp.nextTag()) != END_DOCUMENT && xpp.getDepth() > depth) {
            if (evType == START_TAG) {
                String nodName = xpp.getName();

                if (nodName.equals("Version")) {
                    version = Double.valueOf(xpp.nextText());
                    if (version > MGlobals.dVersion) {
                        GLKLogger.debug("This file is a later version than the software. " +
                                "It is advisable that you upgrade to ensure it runs properly.");
                    }
                    if (!isLibrary) {
                        adv.mVersion = version;
                    }
                    continue;
                }

                if (loadPrelim(adv, xpp, isLibrary, loadWhat, nodName)) {
                    if (itemsStarted) {
                        throw new Exception("Preliminary tags come after ADRIFT items.");
                    }
                    continue;
                }

                try {
                    if (loadWhat == All) {
                        switch (nodName) {
                            case "Introduction":
                                adv.setIntroduction(new MDescription(adv, xpp,
                                        version, "Introduction"));
                                continue;

                            case "EndGameText":
                                adv.setEndGameText(new MDescription(adv, xpp,
                                        version, "EndGameText"));
                                continue;
                        }
                    }

                    if (loadWhat == All || loadWhat == AllExceptProperties) {
                        // These items must come after all of the preliminary items
                        switch (nodName) {
                            case "Location":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MLocation loc = new MLocation(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version);
                                adv.mLocations.put(loc.getKey(), loc);
                                continue;

                            case "Object":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MObject ob = new MObject(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version);
                                adv.mObjects.put(ob.getKey(), ob);
                                continue;

                            case "Task":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MTask tas = new MTask(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version, dupTaskKeys);
                                adv.mTasks.put(tas.getKey(), tas);
                                newTasks.add(tas.getKey());
                                continue;

                            case "Event":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MEvent ev = new MEvent(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version);
                                adv.mEvents.put(ev.getKey(), ev);
                                continue;

                            case "Character":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MCharacter chr = new MCharacter(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version);
                                if (loadWhat == All) {
                                    adv.mCharacters.put(chr.getKey(), chr);
                                } else {
                                    // Only add the Player character
                                    // if we don't already have one
                                    if (chr.getCharacterType() == NonPlayer ||
                                            adv.getPlayer() == null) {
                                        adv.mCharacters.put(chr.getKey(), chr);
                                    }
                                }
                                continue;

                            case "Group":
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MGroup grp = new MGroup(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS);
                                adv.mGroups.put(grp.getKey(), grp);
                                continue;

                            case "TextOverride":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MALR alr = new MALR(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version);
                                adv.mALRs.put(alr.getKey(), alr);
                                continue;

                            case "Hint":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MHint hint = new MHint(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version);
                                adv.mHints.put(hint.getKey(), hint);
                                continue;

                            case "Synonym":
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MSynonym synonym = new MSynonym(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS);
                                adv.mSynonyms.put(synonym);
                                continue;

                            case "Function":
                                if (version == 0) {
                                    throw new Exception("Version tag not specified");
                                }
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MUserFunction udf = new MUserFunction(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS, version);
                                adv.mUDFs.put(udf.getKey(), udf);
                                continue;
                        }
                    }

                    if (loadWhat == All || loadWhat == Properties) {
                        // These items must come after all of the preliminary items
                        switch (nodName) {
                            case "Variable":
                                if (!itemsStarted) {
                                    beginItems(adv);
                                    itemsStarted = true;
                                }
                                MVariable var = new MVariable(adv, xpp,
                                        isLibrary, ADD_DUPLICATE_KEYS);
                                adv.mVariables.put(var.getKey(), var);
                                continue;
                        }
                    }

                    if (!isLibrary) {
                        switch (nodName) {
                            case "Exclude":
                                adv.mExcludedItems.add(xpp.nextText());
                                continue;

                            case "Map":
                                // TODO
                                GLKLogger.error("TODO: load Map node");
                                break;

                            case "FileMappings":
                                loadBlorbMappings(adv, xpp);
                                continue;
                        }
                    }
                } catch (Exception e) {
                    GLKLogger.error("Couldn't load an XML node: " + e.getMessage());
                    //e.printStackTrace();
                    // do nothing - just continue processing
                }
            }

            // tag not recognised - skip until we get to the next tag at the appropriate
            // depth (or end of document)
            if (MDebugger.BEBEK_DEBUG_ENABLED) {
                GLKLogger.warn("Skipping node '" + xpp.getName() + "'");
            }
            while (xpp.getEventType() != END_DOCUMENT && xpp.getDepth() > depth) {
                xpp.next();
            }
        }

        xpp.require(END_TAG, null, "Adventure");

        if (!isLibrary) {
            // Now fix any remapped task keys
            // This must only remap our newly imported tasks, not all the original ones!
            for (String oldKey : dupTaskKeys.keySet()) {
                for (String newTask : newTasks) {
                    MTask tas = adv.mTasks.get(newTask);
                    if (tas.mGeneralKey.equals(oldKey)) {
                        tas.mGeneralKey = dupTaskKeys.get(oldKey);
                    }
                    for (MAction act : tas.mActions) {
                        if (act.mKey1.equals(oldKey)) {
                            act.mKey1 = dupTaskKeys.get(oldKey);
                        }
                        if (act.mKey2.equals(oldKey)) {
                            act.mKey2 = dupTaskKeys.get(oldKey);
                        }
                    }
                }
            }
        }

        // Correct any old style functions
        // Player.Held.Weight > Player.Held.Weight.Sum
        if (!isLibrary && version < 5.0000311) {
            for (MSingleDescription sd : adv.getAllDescriptions()) {
                for (MProperty p : adv.mAllProperties.values()) {
                    if (p.getType() == MProperty.PropertyTypeEnum.Integer ||
                            p.getType() == ValueList) {
                        if (sd.mDescription.contains("." + p.getKey())) {
                            sd.mDescription = sd.mDescription.replace("." +
                                    p.getKey(), "." + p.getKey() + ".Sum");
                        }
                    }
                }
            }
            // TCC: Additional code: fix any old style function
            // references in the restrictions also.
            for (MTask t : adv.mTasks.values()) {
                for (MRestriction r : t.mRestrictions) {
                    for (MProperty p : adv.mAllProperties.values()) {
                        if (p.getType() == MProperty.PropertyTypeEnum.Integer ||
                                p.getType() == ValueList) {
                            if (r.mStringValue.contains("." + p.getKey())) {
                                r.mStringValue = r.mStringValue.replace("." +
                                        p.getKey(), "." + p.getKey() + ".Sum");
                            }
                        }
                    }
                }
            }
        }

        if (loadWhat == All && MFileIO.mCorrectedTasksCount > 0) {
            GLKLogger.debug(MFileIO.mCorrectedTasksCount + " tasks have been updated.");
        }

        GLKLogger.error("TODO: Finish loading mapping pages");
    }

    private static boolean loadPrelim(@NonNull MAdventure adv,
                                      @NonNull XmlPullParser xpp, boolean isLibrary,
                                      @NonNull MFileIO.LoadWhatEnum loadWhat,
                                      @NonNull String nodName) throws Exception {
        if (loadWhat == All || loadWhat == Properties) {
            switch (nodName) {
                case "Property":
                    MProperty prop = new MProperty(adv, xpp, isLibrary, ADD_DUPLICATE_KEYS);
                    adv.mAllProperties.put(prop);
                    return true;
            }
        }

        if (loadWhat == All) {
            try {
                switch (nodName) {
                    case "Title":
                        if (!isLibrary) {
                            adv.setTitle(xpp.nextText());
                        }
                        return true;

                    case "Author":
                        if (!isLibrary) {
                            adv.setAuthor(xpp.nextText());
                        }
                        return true;

                    case "LastUpdated":
                        adv.mCompatCompileDate = xpp.nextText();
                        return true;

                    case "FontName":
                        adv.setDefaultFontName(xpp.nextText());
                        return true;

                    case "FontSize":
                        // TODO
                        GLKLogger.error("TODO: DefaultFontSize = " + xpp.nextText());
                        return true;

                    case "BackgroundColour":
                        // TODO
                        GLKLogger.error("TODO: DefaultBackgroundColor = " + xpp.nextText());
                        return true;

                    case "InputColour":
                        // TODO
                        GLKLogger.error("TODO: InputColour = " + xpp.nextText());
                        return true;

                    case "OutputColour":
                        // TODO
                        GLKLogger.error("TODO: OutputColour = " + xpp.nextText());
                        return true;

                    case "LinkColour":
                        // TODO
                        GLKLogger.error("TODO: LinkColour = " + xpp.nextText());
                        return true;

                    case "ShowFirstLocation":
                        adv.setShowFirstRoom(getBool(xpp.nextText()));
                        return true;

                    case "UserStatus":
                        adv.setUserStatus(xpp.nextText());
                        return true;

                    case "ifindex":
                        // Pre 5.0.20
                        // TODO
                        GLKLogger.error("TODO: ifindex: " + xpp.nextText());
                        return true;

                    case "Cover":
                        adv.setCoverFilename(xpp.nextText());
                        return true;

                    case "ShowExits":
                        adv.setShowExits(getBool(xpp.nextText()));
                        return true;

                    case "EnableMenu":
                        adv.setEnableMenu(getBool(xpp.nextText()));
                        return true;

                    case "EnableDebugger":
                        adv.setEnableDebugger(getBool(xpp.nextText()));
                        return true;

                    case "Elapsed":
                        adv.iElapsed = adv.safeInt(xpp.nextText());
                        return true;

                    case "TaskExecution":
                        adv.mTaskExecutionMode = MAdventure.TaskExecutionEnum.valueOf(xpp.nextText());
                        return true;

                    case "WaitTurns":
                        adv.setWaitTurns(adv.safeInt(xpp.nextText()));
                        return true;

                    case "KeyPrefix":
                        adv.mKeyPrefix = xpp.nextText();
                        return true;

                    case "DirectionNorth":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.North, xpp.nextText());
                        return true;

                    case "DirectionNorthEast":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.NorthEast, xpp.nextText());
                        return true;

                    case "DirectionEast":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.East, xpp.nextText());
                        return true;

                    case "DirectionSouthEast":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.SouthEast, xpp.nextText());
                        return true;

                    case "DirectionSouth":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.South, xpp.nextText());
                        return true;

                    case "DirectionSouthWest":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.SouthWest, xpp.nextText());
                        return true;

                    case "DirectionWest":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.West, xpp.nextText());
                        return true;

                    case "DirectionNorthWest":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.NorthWest, xpp.nextText());
                        return true;

                    case "DirectionIn":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.In, xpp.nextText());
                        return true;

                    case "DirectionOut":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.Out, xpp.nextText());
                        return true;

                    case "DirectionUp":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.Up, xpp.nextText());
                        return true;

                    case "DirectionDown":
                        adv.mDirectionNames.put(MAdventure.DirectionsEnum.Down, xpp.nextText());
                        return true;
                }
            } catch (Exception e) {
                // we recognised the tag, but something failed - do nothing further
                return true;
            }
        }

        return false;
    }

    private static void beginItems(@NonNull MAdventure adv) {
        adv.mAllProperties.setSelected();
        createMandatoryProperties(adv);
    }

    private static void loadBlorbMappings(@NonNull MAdventure adv,
                                          @NonNull XmlPullParser xpp) throws Exception {
        xpp.require(START_TAG, null, "FileMappings");

        int evType = xpp.nextTag();
        int depth = xpp.getDepth();
        while (evType != END_DOCUMENT && xpp.getDepth() >= depth) {
            switch (evType) {
                case START_TAG:
                    switch (xpp.getName()) {
                        case "Mapping":
                            String filePath = null;
                            int resNum = 0;
                            int evType2 = xpp.nextTag();
                            int depth2 = xpp.getDepth();

                            while (evType2 != END_DOCUMENT && xpp.getDepth() >= depth2) {
                                switch (evType2) {
                                    case START_TAG:
                                        switch (xpp.getName()) {
                                            case "File":
                                                filePath = xpp.nextText();
                                                break;
                                            case "Resource":
                                                resNum = adv.safeInt(xpp.nextText());
                                                break;
                                        }
                                        break;
                                }
                                evType2 = xpp.nextTag();
                            }
                            if (filePath != null) {
                                adv.mBlorbMappings.put(filePath, resNum);
                            }
                            break;
                    }
                    break;
            }
            evType = xpp.nextTag();
        }

        xpp.require(END_TAG, null, "FileMappings");
    }

    private static void createMandatoryProperties(@NonNull MAdventure adv) {
        for (String propKey : new String[]{OBJECTARTICLE, OBJECTPREFIX, OBJECTNOUN}) {
            MProperty prop = adv.mObjectProperties.get(propKey);
            if (prop == null) {
                prop = new MProperty(adv);
                prop.setKey(propKey);
                switch (propKey) {
                    case OBJECTARTICLE:
                        prop.setDescription("Object Article");
                        break;
                    case OBJECTPREFIX:
                        prop.setDescription("Object Prefix");
                        break;
                    case OBJECTNOUN:
                        prop.setDescription("Object Name");
                        break;
                }
                prop.setPropertyOf(Objects);
                prop.setType(Text);
                adv.mAllProperties.put(prop);
            }
            prop.setGroupOnly(true);
        }

        MProperty prop = adv.mLocationProperties.get(SHORTLOCATIONDESCRIPTION);
        if (prop == null) {
            prop = new MProperty(adv);
            prop.setKey(SHORTLOCATIONDESCRIPTION);
            prop.setDescription("Short Location Description");
            prop.setPropertyOf(Locations);
            prop.setType(Text);
            adv.mAllProperties.put(prop);
        }
        prop.setGroupOnly(true);

        prop = adv.mLocationProperties.get(LONGLOCATIONDESCRIPTION);
        if (prop == null) {
            prop = new MProperty(adv);
            prop.setKey(LONGLOCATIONDESCRIPTION);
            prop.setDescription("Long Location Description");
            prop.setPropertyOf(Locations);
            prop.setType(Text);
            adv.mAllProperties.put(prop);
        }
        prop.setGroupOnly(true);

        prop = adv.mCharacterProperties.get(CHARACTERPROPERNAME);
        if (prop == null) {
            prop = new MProperty(adv);
            prop.setKey(CHARACTERPROPERNAME);
            prop.setDescription("Character Proper Name");
            prop.setPropertyOf(Characters);
            prop.setType(Text);
            adv.mAllProperties.put(prop);
        }
        prop.setGroupOnly(true);

        prop = adv.mObjectProperties.get("StaticOrDynamic");
        if (prop == null) {
            prop = new MProperty(adv);
            prop.setKey("StaticOrDynamic");
            prop.setDescription("Object type");
            prop.setMandatory(true);
            prop.setPropertyOf(Objects);
            prop.setType(StateList);
            prop.mStates.add("Static");
            prop.mStates.add("Dynamic");
            adv.mAllProperties.put(prop);
        }
        prop.setGroupOnly(true);

        prop = adv.mObjectProperties.get("StaticLocation");
        if (prop == null) {
            prop = new MProperty(adv);
            prop.setKey("StaticLocation");
            prop.setDescription("Location of the object");
            prop.setMandatory(true);
            prop.setPropertyOf(Objects);
            prop.setType(StateList);
            prop.mStates.add("Hidden");
            prop.mStates.add("Single Location");
            prop.mStates.add("Location Group");
            prop.mStates.add("Everywhere");
            prop.mStates.add("Part of Character");
            prop.mStates.add("Part of Object");
            prop.setDependentKey("StaticOrDynamic");
            prop.setDependentValue("Static");
            adv.mAllProperties.put(prop);
        }
        prop.setGroupOnly(true);

        prop = adv.mObjectProperties.get("DynamicLocation");
        if (prop == null) {
            prop = new MProperty(adv);
            prop.setKey("DynamicLocation");
            prop.setDescription("Location of the object");
            prop.setMandatory(true);
            prop.setPropertyOf(Objects);
            prop.setType(StateList);
            prop.mStates.add("Hidden");
            prop.mStates.add("Held by Character");
            prop.mStates.add("Worn by Character");
            prop.mStates.add("In Location");
            prop.mStates.add("Inside Object");
            prop.mStates.add("On Object");
            prop.setDependentKey("StaticOrDynamic");
            prop.setDependentValue("Dynamic");
            adv.mAllProperties.put(prop);
        }
        prop.setGroupOnly(true);

        for (String propKey : new String[]{"AtLocation", "AtLocationGroup", "PartOfWhat",
                "PartOfWho", "HeldByWho", "WornByWho", "InLocation", "InsideWhat", "OnWhat"}) {
            prop = adv.mObjectProperties.get(propKey);
            if (prop != null) {
                prop.setGroupOnly(true);
            }
        }
    }
}
