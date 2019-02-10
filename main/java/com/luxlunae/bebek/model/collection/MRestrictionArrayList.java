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

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.VB;
import com.luxlunae.bebek.controller.MParser;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MDescription;
import com.luxlunae.bebek.model.MRestriction;
import com.luxlunae.bebek.model.io.MFileIO;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

import static com.luxlunae.bebek.VB.inputBox;
import static com.luxlunae.bebek.VB.isNumeric;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MRestrictionArrayList extends ArrayList<MRestriction> {
    public static int iRestNum;
    public static boolean mAskedAboutBrackets = false;
    private static boolean mCorrectBrackets = false;
    @NonNull
    public String mBracketSequence = "";
    @NonNull
    private final MAdventure mAdv;

    public MRestrictionArrayList(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public MRestrictionArrayList(@NonNull MAdventure adv,
                                 @NonNull XmlPullParser xpp,
                                 double dFileVersion) throws Exception {
        // Load all of the FIRST generation child node restrictions under
        // nodContainerXML. We do not want to pick up anything other than first generation - e.g. we
        // don't want to pick up restrictions within description when we're just looking at task restrictions
        this(adv);

        xpp.require(START_TAG, null, "Restrictions");

        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT &&
                xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    case "BracketSequence":
                        mBracketSequence = xpp.nextText();
                        if (!mAskedAboutBrackets && dFileVersion < 5.000026 && mBracketSequence.contains("#A#O#")) {
                            String sResult =
                                    inputBox("<b>There was a logic correction in version 5.0.26 which " +
                                                    "means OR restrictions after AND restrictions were not evaluated. " +
                                                    "Would you like to auto-correct these tasks?<br><br>You may not wish " +
                                                    "to do so if you have already used brackets around any OR restrictions " +
                                                    "following AND restrictions.</b><br><br>Enter y or n:",
                                            "Adventure Upgrade", "y");
                            sResult = sResult.toLowerCase();
                            mCorrectBrackets = !sResult.equals("n");
                            mAskedAboutBrackets = true;
                        }
                        if (mCorrectBrackets) {
                            mBracketSequence = MFileIO.correctBracketSequence(mBracketSequence);
                        }
                        mBracketSequence = mBracketSequence.replace("[", "((").replace("]", "))");
                        break;

                    case "Restriction":
                        String sType = "";
                        String sRest = null;
                        MRestriction rest = new MRestriction(adv);
                        String s;
                        int depth2 = xpp.getDepth();
                        int eventType2;

                        while ((eventType2 = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth2) {
                            if (eventType2 == START_TAG) {
                                s = xpp.getName();
                                switch (s) {
                                    case "Message":
                                        rest.mMessage = new MDescription(adv, xpp, dFileVersion, "Message");
                                        break;

                                    default:
                                        sType = s;
                                        sRest = xpp.nextText();
                                        break;
                                }
                            }
                        }
                        xpp.require(END_TAG, null, "Restriction");

                        if (sRest == null) {
                            continue;
                        }

                        String[] sElements = sRest.split(" ");
                        switch (sType) {
                            case "Location":
                                rest.mType = MRestriction.RestrictionTypeEnum.Location;
                                rest.mKey1 = sElements[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[1]);
                                rest.mLocationType = MRestriction.LocationEnum.valueOf(sElements[2]);
                                rest.mKey2 = sElements.length > 3 ? sElements[3] : "";
                                break;

                            case "Object":
                                rest.mType = MRestriction.RestrictionTypeEnum.Object;
                                rest.mKey1 = sElements[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[1]);
                                rest.mObjectType = MRestriction.ObjectEnum.valueOf(sElements[2]);
                                rest.mKey2 = sElements.length > 3 ? sElements[3] : "";
                                break;

                            case "Task":
                                rest.mType = MRestriction.RestrictionTypeEnum.Task;
                                rest.mKey1 = sElements[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[1]);
                                rest.mTaskType = MRestriction.TaskEnum.Complete;
                                break;

                            case "Character":
                                rest.mType = MRestriction.RestrictionTypeEnum.Character;
                                rest.mKey1 = sElements[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[1]);
                                rest.mCharacterType = MRestriction.CharacterEnum.valueOf(sElements[2]);
                                rest.mKey2 = sElements.length > 3 ? sElements[3] : "";
                                break;

                            case "Item":
                                rest.mType = MRestriction.RestrictionTypeEnum.Item;
                                rest.mKey1 = sElements[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[1]);
                                rest.mItemType = MRestriction.ItemEnum.valueOf(sElements[2]);
                                rest.mKey2 = sElements.length > 3 ? sElements[3] : "";
                                break;

                            case "Variable":
                                rest.mType = MRestriction.RestrictionTypeEnum.Variable;
                                rest.mKey1 = sElements[0];
                                if (rest.mKey1.contains("[") && rest.mKey1.contains("]")) {
                                    rest.mKey2 = rest.mKey1.substring(rest.mKey1.indexOf("[") + 1, rest.mKey1.lastIndexOf("]"));
                                    rest.mKey1 = rest.mKey1.substring(0, rest.mKey1.indexOf("["));
                                }
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[1]);
                                rest.mVariableType = MRestriction.VariableEnum.valueOf(sElements[2].substring(2));
                                StringBuilder sbValue = new StringBuilder();
                                for (int k = 3; k < sElements.length; k++) {
                                    sbValue.append(sElements[k]);
                                    if (k < sElements.length - 1) {
                                        sbValue.append(" ");
                                    }
                                }
                                String sValue = sbValue.toString();
                                if (sElements.length == 4 && isNumeric(sElements[3])) {
                                    rest.mIntValue = VB.cint(sElements[3]); // Integer value
                                    rest.mStringValue = String.valueOf(rest.mIntValue);
                                } else {
                                    if (sValue.startsWith("\"") && sValue.endsWith("\"")) {
                                        rest.mStringValue = sValue.substring(1, sValue.length() - 1); // String constant
                                    } else if (sValue.startsWith("'") && sValue.endsWith("'")) {
                                        rest.mStringValue = sValue.substring(1, sValue.length() - 1); // Expression
                                    } else {
                                        rest.mStringValue = sElements[3];
                                        rest.mIntValue = Integer.MIN_VALUE; // A key to a variable
                                    }
                                }
                                break;

                            case "Property":
                                rest.mType = MRestriction.RestrictionTypeEnum.Property;
                                rest.mKey1 = sElements[0];
                                rest.mKey2 = sElements[1];
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[2]);
                                int iStartExpression = 3;
                                rest.mIntValue = -1;
                                for (MRestriction.VariableEnum eEquals : MRestriction.VariableEnum.values()) {
                                    if (sElements[3].equals(eEquals.toString())) {
                                        switch (eEquals) {
                                            case LessThan:
                                                rest.mIntValue = 0;
                                                break;
                                            case LessThanOrEqualTo:
                                                rest.mIntValue = 1;
                                                break;
                                            case EqualTo:
                                                rest.mIntValue = 2;
                                                break;
                                            case GreaterThanOrEqualTo:
                                                rest.mIntValue = 3;
                                                break;
                                            case GreaterThan:
                                                rest.mIntValue = 4;
                                                break;
                                            case Contain:
                                                rest.mIntValue = 5;
                                                break;
                                        }
                                    }
                                }
                                if (rest.mIntValue > -1) {
                                    iStartExpression = 4;
                                } else {
                                    rest.mIntValue = 2;   // MRestriction.VariableEnum.EqualTo
                                }
                                StringBuilder sValue2 = new StringBuilder();
                                for (int k = iStartExpression; k < sElements.length; k++) {
                                    sValue2.append(sElements[k]);
                                    if (k < sElements.length - 1) {
                                        sValue2.append(" ");
                                    }
                                }
                                rest.mStringValue = sValue2.toString();
                                break;

                            case "Direction":
                                rest.mType = MRestriction.RestrictionTypeEnum.Direction;
                                rest.mMust = MRestriction.MustEnum.valueOf(sElements[0]);
                                rest.mKey1 = sElements[1];
                                rest.mKey1 = MGlobals.right(rest.mKey1, rest.mKey1.length() - 2); // Trim off the Be
                                break;

                            case "Expression":
                                rest.mType = MRestriction.RestrictionTypeEnum.Expression;
                                rest.mMust = MRestriction.MustEnum.Must;
                                StringBuilder sValue3 = new StringBuilder();
                                for (int l = 0; l < sElements.length; l++) {
                                    sValue3.append(sElements[l]);
                                    if (l < sElements.length - 1) {
                                        sValue3.append(" ");
                                    }
                                }
                                rest.mStringValue = sValue3.toString();
                                break;
                        }
                        add(rest);
                }
            }
        }

        xpp.require(END_TAG, null, "Restrictions");
    }

    public boolean passes(final @Nullable MReferenceList refs) {
        return passes(false, refs);
    }

    public boolean passes(boolean bIgnoreReferences, final @Nullable MReferenceList refs) {
        // IgnoreReferences is for when we are evaluating whether the task is completable or not, but we don't have any refs yet
        // if tas is not null its description will be used in any error reporting
        iRestNum = 0;
        MParser.mRouteErrorText = "";
        return size() == 0 || evaluateBracketSequence(mBracketSequence, bIgnoreReferences, refs);
    }

    private boolean evaluateBracketSequence(@NonNull String sBlock, boolean bIgnoreReferences, final @Nullable MReferenceList refs) {
        while (sBlock.contains("A#O")) {
            // #A#O# => (#A#)O#
            // #A#A#O# => (#A#A#)O#
            //(#O#)A#O# => ((#O#)A#)O#
            //#A(#A#O#) => #A((#A#)O#)
            // Insert a ( at beginning, or after previous (
            int i = sBlock.indexOf("A#O");
            int j = sBlock.substring(0, i).lastIndexOf("(") + 1;
            sBlock = MGlobals.left(sBlock, j) + "(" + sBlock.substring(j, i) + "A#)O" + sBlock.substring(i + 3);
        }

        switch (sBlock.substring(0, 1)) {
            case "(":
                // Get block
                int iBrackDepth = 1;
                StringBuilder sSubBlock = new StringBuilder("(");
                int iOffset = 1;
                while (iBrackDepth > 0) {
                    String s = sBlock.substring(iOffset, iOffset + 1);
                    switch (s) {
                        case "(":
                            iBrackDepth++;
                            break;
                        case ")":
                            iBrackDepth--;
                            break;
                        default:
                            // Do nothing
                            break;
                    }
                    sSubBlock.append(s);
                    iOffset++;
                }
                sSubBlock = new StringBuilder(sSubBlock.substring(1, sSubBlock.length() - 1));
                if (sBlock.length() - 2 == sSubBlock.length()) {
                    return evaluateBracketSequence(sSubBlock.toString(), bIgnoreReferences, refs);
                } else {
                    switch (sBlock.substring(sSubBlock.length() + 2, sSubBlock.length() + 3)) {
                        case "A": {
                            boolean bFirst = evaluateBracketSequence(sSubBlock.toString(), bIgnoreReferences, refs);
                            if (!bFirst) {
                                iRestNum += MGlobals.getCharacterCount(sBlock.substring(sSubBlock.length() + 3), '#');
                                return false;
                            } else {
                                return evaluateBracketSequence(sBlock.substring(sSubBlock.length() + 3), bIgnoreReferences, refs);
                            }
                        }

                        case "O": {
                            boolean bFirst = evaluateBracketSequence(sSubBlock.toString(), bIgnoreReferences, refs);
                            if (bFirst) {
                                iRestNum += MGlobals.getCharacterCount(sBlock.substring(sSubBlock.length() + 3), '#');
                                return true;
                            } else {
                                return evaluateBracketSequence(sBlock.substring(sSubBlock.length() + 3), bIgnoreReferences, refs);
                            }
                        }

                        default:
                            // Error
                            break;
                    }
                }
                break;

            case "#":
                iRestNum++;
                if (sBlock.length() == 1) {
                    return get(iRestNum - 1).passes(bIgnoreReferences, refs);
                } else {
                    switch (sBlock.substring(1, 2)) {
                        case "A": {
                            boolean bFirst = get(iRestNum - 1).passes(bIgnoreReferences, refs);
                            if (!bFirst) {
                                iRestNum += MGlobals.getCharacterCount(sBlock.substring(2), '#');
                                return false;
                            } else {
                                return evaluateBracketSequence(sBlock.substring(2), bIgnoreReferences, refs);
                            }
                        }

                        case "O": {
                            boolean bFirst = get(iRestNum - 1).passes(bIgnoreReferences, refs);
                            if (bFirst) {
                                iRestNum += MGlobals.getCharacterCount(sBlock.substring(2), '#');
                                return true;
                            } else {
                                return evaluateBracketSequence(sBlock.substring(2), bIgnoreReferences, refs);
                            }
                        }

                        default:
                            // Error
                            break;
                    }
                }
                break;

            default:
                MGlobals.errMsg("Bad Bracket Sequence");
                break;
        }

        return false;
    }

    public int referencesKey(@NonNull String sKey) {
        int num = 0;
        for (MRestriction current : this) {
            if (current.referencesKey(sKey)) {
                num++;
            }
        }
        return num;
    }

    public boolean deleteKey(@NonNull String sKey) {
        for (int i = size() - 1; i >= 0; i--) {
            if (get(i).referencesKey(sKey)) {
                remove(i);
                if (size() == 0) {
                    mBracketSequence = "";
                } else if (size() == 1) {
                    mBracketSequence = "#";
                } else {
                    stripRestriction(i);
                }
            }
        }
        return true;
    }

    private void stripRestriction(int iRest) {
        int iFound = 0;
        for (int i = 0; i < mBracketSequence.length(); i++) {
            if (MGlobals.mid(mBracketSequence, i, 1).equals("#")) {
                iFound++;
            }
            if (iFound == iRest + 1) {
                // Delete the marker
                mBracketSequence = MGlobals.left(mBracketSequence, i) +
                        MGlobals.right(mBracketSequence, mBracketSequence.length() - i - 1);
                // Remove any trailing And/Or markers
                if (mBracketSequence.length() >= i &&
                        (MGlobals.mid(mBracketSequence, i, 1).equals("A") ||
                                MGlobals.mid(mBracketSequence, i, 1).equals("O"))) {
                    mBracketSequence = MGlobals.left(mBracketSequence, i) +
                            MGlobals.right(mBracketSequence, mBracketSequence.length() - i - 1);
                }
                // Correct any duff brackets
                break;
            }
        }
    }

    @NonNull
    public MRestrictionArrayList copy() {
        MRestrictionArrayList MRestrictionArrayList = new MRestrictionArrayList(mAdv);
        MRestrictionArrayList.mBracketSequence = mBracketSequence;
        for (int i = 0; i <= size() - 1; i++) {
            MRestriction item = get(i).copy();
            MRestrictionArrayList.add(item);
        }
        return MRestrictionArrayList;
    }
}

