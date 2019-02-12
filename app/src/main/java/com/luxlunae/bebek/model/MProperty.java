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
import com.luxlunae.bebek.VB;
import com.luxlunae.bebek.controller.MParser;
import com.luxlunae.bebek.model.collection.MStringArrayList;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static com.luxlunae.bebek.MGlobals.SELECTED;
import static com.luxlunae.bebek.MGlobals.UNSELECTED;
import static com.luxlunae.bebek.MGlobals.getBool;
import static com.luxlunae.bebek.MGlobals.safeInt;
import static com.luxlunae.bebek.model.MProperty.PropertyOfEnum.Locations;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.SelectionOnly;
import static com.luxlunae.bebek.model.MProperty.PropertyTypeEnum.StateList;
import static com.luxlunae.bebek.model.MVariable.VariableType.NUMERIC;
import static com.luxlunae.bebek.model.MVariable.VariableType.TEXT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * If you create a variable then there is only one copy of it for the whole game. Properties are similar
 * to variables in that they store a value, however there is a separate copy of each property inside
 * each item (object, character and location) that each stores its own value.
 * <p>
 * Properties in v5 are now dynamic. Rather than the pre-defined list that you get in v4 objects,
 * you can define your own. These can be applied to Locations, Objects, Characters or all three.
 * <p>
 * There are several types of properties. These are:
 * <p>
 * Character – the property value must be a character
 * Integer – the property value must be an integer value
 * Location Group – the property value must be a location group
 * Location – the property value must be a location
 * Object – the property value must be an object
 * Selection Only – the property is either True or False
 * State – the property value must be one of a pre-defined list
 * Value - Similar to a state, except each state has a specific integer value associated with it
 * Text – the property value can be any text
 * <p>
 * Each property will by default always appear. If you want to restrict when a property will
 * appear (for example, you wouldn’t want a Static object to have a wearable property), you can
 * select a property and value that must be true for this property to be available. So, in the
 * wearable example, you can select that the property will only be available if property
 * “Object type” is set to value “Dynamic”.
 * <p>
 * You may want to make a property mandatory. This means, that if the property is available,
 * it must be selected. So, object type is mandatory, as all objects are either Static or Dynamic.
 * Similarly, you may want to make a property mandatory dependant on another property, or even
 * another property value. Examples of this are the “Held by who” property, which is a character
 * list type. This property is mandatory when the “Location of the object” property is set to
 * “Held by Character”, as you would always want the “Held by who” property to appear.
 * <p>
 * If you create a property of type “Object”, you can restrict the list of objects displayed
 * to ones having particular properties. So, for example, the “Inside what?” property which
 * is displayed when the location of an object is set to “inside object”, is restricted to
 * container objects. This prevents you from being able to put objects inside other objects that are not containers.
 */
public class MProperty extends MItem {

    @NonNull
    public MStringArrayList mStates = new MStringArrayList();
    @NonNull
    public LinkedHashMap<String, Integer> mValueList = new LinkedHashMap<>();
    @NonNull
    private PropertyTypeEnum mType = SelectionOnly;
    private int mIntData;
    @NonNull
    private MDescription mStringData;
    private boolean mIsSelected;
    @NonNull
    private String mDescription = "";
    @NonNull
    private String mDependentKey = "";
    @NonNull
    private String mDependentValue = "";
    private boolean mIsMandatory;
    private boolean mIsFromGroup;
    @NonNull
    private String mRestrictProperty = "";
    @NonNull
    private String mRestrictValue = "";
    @NonNull
    private String mPrivateTo = "";
    @Nullable
    private String mPopupDescription;
    @NonNull
    private PropertyOfEnum mPropertyOf = Locations;
    @NonNull
    private String mAppendToProperty = "";
    private boolean mIsGroupOnly;
    private int mIndent = 0;

    public MProperty(@NonNull MAdventure adv) {
        super(adv);
        mStringData = new MDescription(adv);
    }

    public MProperty(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                     boolean bLibrary, boolean bAddDuplicateKeys) throws Exception {
        // Create a new property from an ADRIFT 5 XML <Property> element
        this(adv);

        xpp.require(START_TAG, null, "Property");

        String s;
        MStringArrayList stateList = new MStringArrayList();
        LinkedHashMap<String, Integer> valueList = new LinkedHashMap<>();
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT &&
                xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        header.processTag(xpp);
                        break;

                    case "Description":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setDescription(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "Mandatory":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setMandatory(getBool(s));
                            }
                        } catch (Exception e) {
                            // ignore it
                        }
                        break;

                    case "PropertyOf":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setPropertyOf(MProperty.PropertyOfEnum.valueOf(s));
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "AppendTo":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setAppendToProperty(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "Type":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setType(MProperty.PropertyTypeEnum.valueOf(s));
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "State":
                        // only valid if property type is StateList
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                stateList.add(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "ValueList":
                        // only valid if property type is ValueList
                        String sLabel = null;
                        int iValue = 0;
                        int depth2 = xpp.getDepth();
                        int eventType2;

                        while ((eventType2 = xpp.nextTag()) != XmlPullParser.END_DOCUMENT &&
                                xpp.getDepth() > depth2) {
                            if (eventType2 == START_TAG) {
                                switch (xpp.getName()) {
                                    case "Label":
                                        try {
                                            s = xpp.nextText();
                                            if (!s.equals("")) {
                                                sLabel = s;
                                            }
                                        } catch (Exception e) {
                                            // ignore it
                                            continue;
                                        }
                                        break;

                                    case "Value":
                                        try {
                                            s = xpp.nextText();
                                            if (!s.equals("")) {
                                                iValue = Integer.parseInt(s);
                                            }
                                        } catch (Exception e) {
                                            // ignore it
                                            continue;
                                        }
                                        break;
                                }
                            }
                        }
                        xpp.require(END_TAG, null, "ValueList");

                        if (sLabel != null) {
                            valueList.put(sLabel, iValue);
                        }
                        break;

                    case "PrivateTo":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setPrivateTo(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "Tooltip":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setPopupDescription(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "DependentKey":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setDependentKey(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "DependentValue":
                        // only valid if there is also a dependent key
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setDependentValue(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "RestrictProperty":
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setRestrictProperty(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;

                    case "RestrictValue":
                        // only valid if there is also a restrictproperty
                        try {
                            s = xpp.nextText();
                            if (!s.equals("")) {
                                setRestrictValue(s);
                            }
                        } catch (Exception e) {
                            // ignore it
                            continue;
                        }
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Property");

        if (!header.finalise(this, adv.mAllProperties,
                bLibrary, bAddDuplicateKeys, null)) {
            throw new Exception();
        }

        switch (getType()) {
            case StateList:
                mStates = stateList;
                if (mStates.size() > 0) {
                    setValue(mStates.get(0));
                }
                break;

            case ValueList:
                mValueList = valueList;
                break;
        }
    }

    public MProperty(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                     double dFileVersion) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "Property");

        boolean inited = false;
        String sPropKey;
        String sValue = "";
        MDescription oValue = new MDescription(adv);
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT &&
                xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    case "Key":
                        sPropKey = xpp.nextText();
                        MProperty pGlobal = adv.mAllProperties.get(sPropKey);
                        if (pGlobal != null) {
                            copyFrom(pGlobal);
                            inited = true;
                        }
                        break;

                    case "Value":
                        try {
                            sValue = xpp.nextText();
                        } catch (Exception e) {
                            oValue = new MDescription(adv, xpp, dFileVersion, "Value");
                        }
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Property");

        if (inited) {
            switch (getType()) {
                case Text:
                    setStringData(oValue);
                    break;

                case SelectionOnly:
                    // do nothing
                    break;

                default:
                    setValue(sValue);
                    break;
            }
            setSelected(true);
        } else {
            throw new Exception();
        }
    }

    @NonNull
    private String getPrivateTo() {
        return mPrivateTo;
    }

    private void setPrivateTo(@NonNull String value) {
        mPrivateTo = value;
    }

    @Nullable
    private String getPopupDescription() {
        return mPopupDescription;
    }

    private void setPopupDescription(@Nullable String value) {
        mPopupDescription = value;
    }

    @NonNull
    public PropertyOfEnum getPropertyOf() {
        return mPropertyOf;
    }

    public void setPropertyOf(@NonNull PropertyOfEnum value) {
        mPropertyOf = value;
    }

    public boolean getMandatory() {
        return mIsMandatory;
    }

    public void setMandatory(boolean value) {
        mIsMandatory = value;
    }

    public boolean getFromGroup() {
        return mIsFromGroup;
    }

    void setFromGroup(boolean value) {
        mIsFromGroup = value;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public void setDescription(@NonNull String value) {
        mDescription = value;
    }

    @NonNull
    public String getAppendToProperty() {
        return mAppendToProperty;
    }

    public void setAppendToProperty(@NonNull String value) {
        mAppendToProperty = value;
    }

    public boolean getGroupOnly() {
        return mIsGroupOnly;
    }

    public void setGroupOnly(boolean value) {
        mIsGroupOnly = value;
    }

    @NonNull
    public PropertyTypeEnum getType() {
        return mType;
    }

    public void setType(@NonNull PropertyTypeEnum value) {
        mType = value;
        switch (mType) {
            case StateList:
                if (!mStates.contains(getStringData().toString()) && mStates.size() > 0) {
                    // Default the value to the first state
                    setStringData(new MDescription(mAdv, mStates.get(0)));
                }
                break;

            case ValueList:
                if (!mValueList.containsValue(mIntData) && mValueList.size() > 0) {
                    // Default the value to the first value in the list
                    mIntData = mValueList.entrySet().iterator().next().getValue();
                }
                break;
        }
    }

    public int getIntData() {
        return mIntData;
    }

    private void setIntData(int value) {
        mIntData = value;
    }

    @NonNull
    public MDescription getStringData() {
        return mStringData;
    }

    void setStringData(@NonNull MDescription value) {
        mStringData = value;
    }

    public boolean getSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean value) {
        mIsSelected = value;
    }

    public int getIndent() {
        return mIndent;
    }

    public void setIndent(int value) {
        mIndent = value;
    }

    @Nullable
    @Override
    public MProperty clone() {
        return copy();
    }

    @NonNull
    public String getDependentKey() {
        return mDependentKey;
    }

    public void setDependentKey(@NonNull String value) {
        if (MGlobals.iLoading > 0 || value.equals("") || mAdv.keyExists(value)) {
            mDependentKey = value;
        } else {
            throw new RuntimeException("Key " + value + " doesn't exist");
        }
    }

    @NonNull
    public String getDependentValue() {
        return mDependentValue;
    }

    public void setDependentValue(@NonNull String value) {
        // TODO - check that it's a valid value
        mDependentValue = value;
    }

    @NonNull
    public String getRestrictProperty() {
        return mRestrictProperty;
    }

    public void setRestrictProperty(@NonNull String value) {
        mRestrictProperty = value;
    }

    @NonNull
    public String getRestrictValue() {
        return mRestrictValue;
    }

    private void setRestrictValue(@NonNull String value) {
        mRestrictValue = value;
    }

    @NonNull
    private MStringArrayList getPossibleValues() {
        MStringArrayList ret = new MStringArrayList();

        switch (getType()) {
            case CharacterKey:
                for (MCharacter ch : mAdv.mCharacters.values()) {
                    if (getRestrictProperty().equals("") ||
                            ch.hasProperty(getRestrictProperty())) {
                        if (getRestrictValue().equals("") ||
                                ch.getPropertyValue(getRestrictProperty()).equals(getRestrictValue())) {
                            ret.add(ch.getKey());
                        }
                    }
                }
                break;

            case Integer:
                MGlobals.TODO("Integer values");
                break;

            case LocationGroupKey:
                MGlobals.TODO("Location Group values");
                break;

            case LocationKey:
                for (MLocation loc : mAdv.mLocations.values()) {
                    if ((getRestrictProperty().equals("") ||
                            loc.hasProperty(getRestrictProperty()))) {
                        if (getRestrictValue().equals("") ||
                                loc.getPropertyValue(getRestrictProperty()).equals(getRestrictValue())) {
                            ret.add(loc.getKey());
                        }
                    }
                }
                break;

            case ObjectKey:
                for (MObject ob : mAdv.mObjects.values()) {
                    if ((getRestrictProperty().equals("") ||
                            ob.hasProperty(getRestrictProperty()))) {
                        if (getRestrictValue().equals("") ||
                                ob.getPropertyValue(getRestrictProperty()).equals(getRestrictValue())) {
                            ret.add(ob.getKey());
                        }
                    }
                }
                break;

            case SelectionOnly:
                ret.add(SELECTED);
                ret.add(UNSELECTED); // Not strictly a property value, but only way for action to remove the property
                break;

            case StateList:
                ret.addAll(mStates);
                for (MProperty prop : mAdv.mAllProperties.values()) {
                    if (prop.getType() == StateList &&
                            prop.getAppendToProperty().equals(getKey())) {
                        ret.addAll(prop.mStates);
                    }
                }
                break;

            case Text:
                MGlobals.TODO("Text values");
                break;
        }

        return ret;
    }

    @NonNull
    public String getValue() {
        return getValue(false);
    }

    public void setValue(@NonNull String value) {
        try {
            switch (mType) {
                case StateList:
                    MStringArrayList validValues = getPossibleValues();
                    if (MGlobals.iLoading > 0 || value.equals("") || validValues.contains(value)) {
                        mStringData = new MDescription(mAdv, value);
                    } else if (validValues.size() > 0) {
                        // Perhaps it's an expression that resolves to a valid state...
                        MVariable v = new MVariable(mAdv);
                        v.setType(TEXT);
                        v.setToExpr(value, MParser.mReferences);
                        String evalVal = v.getStr();
                        if (validValues.contains(evalVal)) {
                            mStringData = new MDescription(mAdv, evalVal);
                        } else {
                            // Secomd chance - some v4 games (e.g. To Hell in a Hamper) seem
                            // to use state list property values that do and do not contain spaces interchangeably.
                            // See if we can match the given state list value when it is stripped of spaces...
                            boolean found = false;
                            for (String s : validValues) {
                                if (s.replace(" ", "").equals(evalVal)) {
                                    mStringData = new MDescription(mAdv, s);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                // Give up!
                                throw new Exception("'" + value + "' is not a valid state list state.");
                            }
                        }
                    }
                    break;

                case ValueList:
                    if (MGlobals.iLoading > 0 || value.equals("") ||
                            mValueList.containsKey(value) ||
                            mValueList.containsValue(safeInt(value))) {
                        mIntData = mValueList.containsKey(value) ?
                                mValueList.get(value) : safeInt(value);
                    } else if (mValueList.size() > 0) {
                        // Perhaps it's an expression that resolves to a valid state...
                        MVariable v = new MVariable(mAdv);
                        v.setType(TEXT);
                        v.setToExpr(value, MParser.mReferences);
                        String evalVal = v.getStr();
                        if (mValueList.containsKey(evalVal)) {
                            mIntData = mValueList.get(evalVal);
                        } else {
                            throw new Exception("'" + value + "' is not a valid value list state.");
                        }
                    }
                    break;

                case ObjectKey:
                case CharacterKey:
                case LocationKey:
                case LocationGroupKey:
                    mStringData = new MDescription(mAdv, value);
                    break;

                case Text:
                    if (mStringData.size() < 2) {
                        // Simple text stored
                        if (!(mStringData.size() == 1 &&
                                mStringData.get(0).mDescription.equals(value))) {
                            mStringData = new MDescription(mAdv, value);
                        }
                    } else {
                        // We have a complex description here, probably with
                        // restrictions etc - if we set the property, we'll lose it
                    }
                    break;

                case Integer:
                    if (VB.isNumeric(value)) {
                        mIntData = safeInt(value);
                    } else {
                        // Assume it's an expression that resolves to an integer...
                        MVariable var = new MVariable(mAdv);
                        var.setType(NUMERIC);
                        var.setToExpr(value, MParser.mReferences);
                        mIntData = var.getInt();
                    }
                    break;
            }
        } catch (Exception ex) {
            MGlobals.errMsg("Error Setting Property " + getDescription() +
                    " to \"" + value + "\"", ex);
        }
    }

    @NonNull
    public String getValue(boolean bTesting) {
        switch (mType) {
            case StateList:
            case ObjectKey:
            case CharacterKey:
            case LocationKey:
            case LocationGroupKey:
                return mStringData.toString();

            case ValueList:
                return String.valueOf(mIntData);

            case Text:
                return mStringData.toString(bTesting);

            case Integer:
                return String.valueOf(mIntData);

            case SelectionOnly:
                return "true";
        }
        return "";
    }

    @NonNull
    public MProperty copy() {
        MProperty p = new MProperty(mAdv);
        p.mStates = mStates.clone();
        p.mValueList.clear();
        for (String sLabel : mValueList.keySet()) {
            p.mValueList.put(sLabel, mValueList.get(sLabel));
        }
        p.setDependentKey(getDependentKey());
        p.setDependentValue(getDependentValue());
        p.setAppendToProperty(getAppendToProperty());
        p.setRestrictProperty(getRestrictProperty());
        p.setRestrictValue(getRestrictValue());
        p.setDescription(getDescription());
        p.setIntData(getIntData());
        p.setKey(getKey());
        p.setMandatory(getMandatory());
        p.setSelected(getSelected());
        p.setType(getType()); // Needs to be set before StringData, otherwise an appended StateList won't be allowed as a valid value (e.g. Locked will be reset to Open)
        p.setStringData(getStringData().copy());
        p.setPropertyOf(getPropertyOf());
        p.setGroupOnly(getGroupOnly());
        p.setFromGroup(getFromGroup());
        p.setPrivateTo(getPrivateTo());
        p.setPopupDescription(getPopupDescription());

        switch (p.getType()) {
            case ObjectKey:
            case CharacterKey:
            case LocationKey:
            case LocationGroupKey:
            case Text:
            case ValueList:
                // These will all re-write StringData, so we can ignore
                break;

            default:
                p.setValue(getValue());
                break;
        }

        return p;
    }

    public void copyFrom(@NonNull MProperty p) {
        mStates = p.mStates.clone();
        mValueList.clear();
        for (String sLabel : p.mValueList.keySet()) {
            mValueList.put(sLabel, p.mValueList.get(sLabel));
        }
        setDependentKey(p.getDependentKey());
        setDependentValue(p.getDependentValue());
        setAppendToProperty(p.getAppendToProperty());
        setRestrictProperty(p.getRestrictProperty());
        setRestrictValue(p.getRestrictValue());
        setDescription(p.getDescription());
        setIntData(p.getIntData());
        setKey(p.getKey());
        setMandatory(p.getMandatory());
        setSelected(p.getSelected());
        setType(p.getType()); // Needs to be set before StringData, otherwise an appended StateList won't be allowed as a valid value (e.g. Locked will be reset to Open)
        setStringData(p.getStringData().copy());
        setPropertyOf(p.getPropertyOf());
        setGroupOnly(p.getGroupOnly());
        setFromGroup(p.getFromGroup());
        setPrivateTo(p.getPrivateTo());
        setPopupDescription(p.getPopupDescription());

        switch (getType()) {
            case ObjectKey:
            case CharacterKey:
            case LocationKey:
            case LocationGroupKey:
            case Text:
            case ValueList:
                // These will all re-write StringData, so we can ignore
                break;

            default:
                setValue(p.getValue());
                break;
        }
    }

    @Override
    public String toString() {
        return getDescription() + " (" + getValue() + ")";
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getDescription();
    }

    @Override
    @NonNull
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> ret = new ArrayList<>();
        ret.add(getStringData());
        return ret;
    }

    @Override
    public int findLocal(@NonNull String toFind, @Nullable String toReplace,
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
        if (getDependentKey().equals(key)) {
            ret++;
        }
        if (getRestrictProperty().equals(key)) {
            ret++;
        }
        return ret;
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        if (getDependentKey().equals(key)) {
            setDependentKey("");
        }
        if (getRestrictProperty().equals(key)) {
            setRestrictProperty("");
        }
        return true;
    }

    public enum PropertyOfEnum {
        Locations,      // 0
        Objects,        // 1
        Characters,     // 2
        AnyItem         // 3
    }

    public enum PropertyTypeEnum {
        SelectionOnly,          // 0
        Integer,                // 1
        Text,                   // 2
        ObjectKey,              // 3
        StateList,              // 4
        CharacterKey,           // 5
        LocationKey,            // 6
        LocationGroupKey,       // 7
        ValueList               // 8
    }
}
