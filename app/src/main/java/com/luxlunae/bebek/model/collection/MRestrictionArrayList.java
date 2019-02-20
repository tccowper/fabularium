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
import com.luxlunae.bebek.model.MDescription;
import com.luxlunae.bebek.model.MRestriction;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

import static com.luxlunae.bebek.MGlobals.getCharacterCount;
import static com.luxlunae.bebek.MGlobals.left;
import static com.luxlunae.bebek.MGlobals.mid;
import static com.luxlunae.bebek.MGlobals.right;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.inputBox;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.model.io.MFileIO.correctBracketSequence;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MRestrictionArrayList extends ArrayList<MRestriction> {
    public static int mRestNum;
    public static boolean mAskedAboutBrackets = false;
    private static boolean mCorrectBrackets = false;

    @NonNull
    private final MAdventure mAdv;
    @NonNull
    public String mBrackSeq = "";

    public MRestrictionArrayList(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public MRestrictionArrayList(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                                 double version) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "Restrictions");

        int depth = xpp.getDepth();
        int evType;
        while ((evType = xpp.nextTag()) != END_DOCUMENT && xpp.getDepth() > depth) {
            if (evType == START_TAG) {
                switch (xpp.getName()) {
                    case "BracketSequence":
                        mBrackSeq = xpp.nextText();
                        if (!mAskedAboutBrackets &&
                                version < 5.000026 && mBrackSeq.contains("#A#O#")) {
                            String ret = inputBox(adv,
                                    "<b>There was a logic correction in version 5.0.26 " +
                                            "which means OR restrictions after AND restrictions " +
                                            "were not evaluated. Would you like to auto-correct " +
                                            "these tasks?<br><br>You may not wish to do so if " +
                                            "you have already used brackets around any OR " +
                                            "restrictions following AND restrictions.</b><br>" +
                                            "<br>Enter y or n:",
                                    "Adventure Upgrade", "y");
                            assert ret != null;
                            ret = ret.toLowerCase();
                            mCorrectBrackets = !ret.equals("n");
                            mAskedAboutBrackets = true;
                        }
                        if (mCorrectBrackets) {
                            mBrackSeq = correctBracketSequence(mBrackSeq);
                        }
                        mBrackSeq = mBrackSeq.replace("[", "((")
                                .replace("]", "))");
                        break;

                    case "Restriction":
                        MRestriction rest = new MRestriction(adv);
                        String restType = "";
                        String restMsg = null;
                        int depth2 = xpp.getDepth();
                        int evType2;

                        while ((evType2 = xpp.nextTag()) != END_DOCUMENT &&
                                xpp.getDepth() > depth2) {
                            if (evType2 == START_TAG) {
                                String s = xpp.getName();
                                switch (s) {
                                    case "Message":
                                        rest.mMessage = new MDescription(adv, xpp,
                                                version, "Message");
                                        break;

                                    default:
                                        restType = s;
                                        restMsg = xpp.nextText();
                                        break;
                                }
                            }
                        }
                        xpp.require(END_TAG, null, "Restriction");

                        if (restMsg == null) {
                            continue;
                        }

                        String[] restMsgWords = restMsg.split(" ");
                        switch (restType) {
                            case "Location": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Location;
                                rest.mKey1 = restMsgWords[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[1]);
                                rest.mLocationType =
                                        MRestriction.LocationEnum.valueOf(restMsgWords[2]);
                                rest.mKey2 = restMsgWords.length > 3 ? restMsgWords[3] : "";
                                break;
                            }

                            case "Object": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Object;
                                rest.mKey1 = restMsgWords[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[1]);
                                rest.mObjectType =
                                        MRestriction.ObjectEnum.valueOf(restMsgWords[2]);
                                rest.mKey2 = restMsgWords.length > 3 ? restMsgWords[3] : "";
                                break;
                            }

                            case "Task": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Task;
                                rest.mKey1 = restMsgWords[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[1]);
                                rest.mTaskType = MRestriction.TaskEnum.Complete;
                                break;
                            }

                            case "Character": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Character;
                                rest.mKey1 = restMsgWords[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[1]);
                                rest.mCharacterType =
                                        MRestriction.CharacterEnum.valueOf(restMsgWords[2]);
                                rest.mKey2 = restMsgWords.length > 3 ? restMsgWords[3] : "";
                                break;
                            }

                            case "Item": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Item;
                                rest.mKey1 = restMsgWords[0];
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[1]);
                                rest.mItemType = MRestriction.ItemEnum.valueOf(restMsgWords[2]);
                                rest.mKey2 = restMsgWords.length > 3 ? restMsgWords[3] : "";
                                break;
                            }

                            case "Variable": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Variable;
                                rest.mKey1 = restMsgWords[0];
                                if (rest.mKey1.contains("[") && rest.mKey1.contains("]")) {
                                    rest.mKey2 = rest.mKey1.substring(rest.mKey1.indexOf("[") + 1,
                                            rest.mKey1.lastIndexOf("]"));
                                    rest.mKey1 = rest.mKey1.substring(0, rest.mKey1.indexOf("["));
                                }
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[1]);
                                rest.mVariableType =
                                        MRestriction.VariableEnum.valueOf(restMsgWords[2].substring(2));
                                StringBuilder sb = new StringBuilder();
                                for (int i = 3; i < restMsgWords.length; i++) {
                                    sb.append(restMsgWords[i]);
                                    if (i < restMsgWords.length - 1) {
                                        sb.append(" ");
                                    }
                                }
                                String val = sb.toString();
                                if (restMsgWords.length == 4 && isNumeric(restMsgWords[3])) {
                                    // Integer value
                                    rest.mIntValue = cint(restMsgWords[3]);
                                    rest.mStringValue = String.valueOf(rest.mIntValue);
                                } else {
                                    if (val.startsWith("\"") && val.endsWith("\"")) {
                                        // String constant
                                        rest.mStringValue = val.substring(1, val.length() - 1);
                                    } else if (val.startsWith("'") && val.endsWith("'")) {
                                        // Expression
                                        rest.mStringValue = val.substring(1, val.length() - 1);
                                    } else {
                                        // A key to a variable
                                        rest.mStringValue = restMsgWords[3];
                                        rest.mIntValue = Integer.MIN_VALUE;
                                    }
                                }
                                break;
                            }

                            case "Property": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Property;
                                rest.mKey1 = restMsgWords[0];
                                rest.mKey2 = restMsgWords[1];
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[2]);
                                rest.mIntValue = -1;

                                int startExpr = 3;
                                for (MRestriction.VariableEnum compare :
                                        MRestriction.VariableEnum.values()) {
                                    if (restMsgWords[3].equals(compare.toString())) {
                                        switch (compare) {
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
                                    startExpr = 4;
                                } else {
                                    // MRestriction.VariableEnum.EqualTo
                                    rest.mIntValue = 2;
                                }
                                StringBuilder val = new StringBuilder();
                                for (int i = startExpr; i < restMsgWords.length; i++) {
                                    val.append(restMsgWords[i]);
                                    if (i < restMsgWords.length - 1) {
                                        val.append(" ");
                                    }
                                }
                                rest.mStringValue = val.toString();
                                break;
                            }

                            case "Direction": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Direction;
                                rest.mMust = MRestriction.MustEnum.valueOf(restMsgWords[0]);
                                rest.mKey1 = restMsgWords[1];
                                // Trim off the Be:
                                rest.mKey1 = right(rest.mKey1, rest.mKey1.length() - 2);
                                break;
                            }

                            case "Expression": {
                                rest.mType = MRestriction.RestrictionTypeEnum.Expression;
                                rest.mMust = MRestriction.MustEnum.Must;
                                StringBuilder val = new StringBuilder();
                                for (int i = 0; i < restMsgWords.length; i++) {
                                    val.append(restMsgWords[i]);
                                    if (i < restMsgWords.length - 1) {
                                        val.append(" ");
                                    }
                                }
                                rest.mStringValue = val.toString();
                                break;
                            }
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

    public boolean passes(boolean ignoreRefs, final @Nullable MReferenceList refs) {
        // IgnoreReferences is for when we are evaluating whether the
        // task is completable or not, but we don't have any refs yet
        // if tas is not null its description will be used in any error reporting
        mRestNum = 0;
        mAdv.mRouteErrorText = "";
        return size() == 0 || evalBrackSeq(mBrackSeq, ignoreRefs, refs);
    }

    private boolean evalBrackSeq(@NonNull String seq, boolean ignoreRefs,
                                 final @Nullable MReferenceList refs) {
        while (seq.contains("A#O")) {
            // #A#O# => (#A#)O#
            // #A#A#O# => (#A#A#)O#
            //(#O#)A#O# => ((#O#)A#)O#
            //#A(#A#O#) => #A((#A#)O#)
            // Insert a ( at beginning, or after previous (
            int i = seq.indexOf("A#O");
            int j = seq.substring(0, i).lastIndexOf("(") + 1;
            seq = left(seq, j) + "(" + seq.substring(j, i) +
                    "A#)O" + seq.substring(i + 3);
        }

        switch (seq.substring(0, 1)) {
            case "(":
                // Get seq
                int brackDepth = 1;
                StringBuilder subSeq = new StringBuilder("(");
                int pos = 1;
                while (brackDepth > 0) {
                    String s = seq.substring(pos, pos + 1);
                    switch (s) {
                        case "(":
                            brackDepth++;
                            break;
                        case ")":
                            brackDepth--;
                            break;
                        default:
                            // Do nothing
                            break;
                    }
                    subSeq.append(s);
                    pos++;
                }
                subSeq = new StringBuilder(subSeq.substring(1, subSeq.length() - 1));
                if (seq.length() - 2 == subSeq.length()) {
                    return evalBrackSeq(subSeq.toString(), ignoreRefs, refs);
                } else {
                    switch (seq.substring(subSeq.length() + 2, subSeq.length() + 3)) {
                        case "A": {
                            boolean isFirst =
                                    evalBrackSeq(subSeq.toString(), ignoreRefs, refs);
                            if (!isFirst) {
                                mRestNum += getCharacterCount(seq.substring(subSeq.length() + 3), '#');
                                return false;
                            } else {
                                return evalBrackSeq(seq.substring(subSeq.length() + 3), ignoreRefs, refs);
                            }
                        }

                        case "O": {
                            boolean isFirst =
                                    evalBrackSeq(subSeq.toString(), ignoreRefs, refs);
                            if (isFirst) {
                                mRestNum += getCharacterCount(seq.substring(subSeq.length() + 3), '#');
                                return true;
                            } else {
                                return evalBrackSeq(seq.substring(subSeq.length() + 3), ignoreRefs, refs);
                            }
                        }

                        default:
                            // Error
                            break;
                    }
                }
                break;

            case "#":
                mRestNum++;
                if (seq.length() == 1) {
                    return get(mRestNum - 1).passes(ignoreRefs, refs);
                } else {
                    switch (seq.substring(1, 2)) {
                        case "A": {
                            boolean bFirst = get(mRestNum - 1).passes(ignoreRefs, refs);
                            if (!bFirst) {
                                mRestNum += getCharacterCount(seq.substring(2), '#');
                                return false;
                            } else {
                                return evalBrackSeq(seq.substring(2), ignoreRefs, refs);
                            }
                        }

                        case "O": {
                            boolean bFirst = get(mRestNum - 1).passes(ignoreRefs, refs);
                            if (bFirst) {
                                mRestNum += getCharacterCount(seq.substring(2), '#');
                                return true;
                            } else {
                                return evalBrackSeq(seq.substring(2), ignoreRefs, refs);
                            }
                        }

                        default:
                            // Error
                            break;
                    }
                }
                break;

            default:
                mAdv.mView.errMsg("Bad Bracket Sequence");
                break;
        }

        return false;
    }

    public int getNumberOfKeyRefs(@NonNull String key) {
        int ret = 0;
        for (MRestriction rest : this) {
            if (rest.referencesKey(key)) {
                ret++;
            }
        }
        return ret;
    }

    public boolean deleteKey(@NonNull String key) {
        for (int i = size() - 1; i >= 0; i--) {
            if (get(i).referencesKey(key)) {
                remove(i);
                if (size() == 0) {
                    mBrackSeq = "";
                } else if (size() == 1) {
                    mBrackSeq = "#";
                } else {
                    stripRestriction(i);
                }
            }
        }
        return true;
    }

    private void stripRestriction(int restIndex) {
        int nFound = 0;
        for (int i = 0; i < mBrackSeq.length(); i++) {
            if (mid(mBrackSeq, i, 1).equals("#")) {
                nFound++;
            }
            if (nFound == restIndex + 1) {
                // Delete the marker
                mBrackSeq = left(mBrackSeq, i) + right(mBrackSeq, mBrackSeq.length() - i - 1);
                // Remove any trailing And/Or markers
                if (mBrackSeq.length() >= i &&
                        (mid(mBrackSeq, i, 1).equals("A") ||
                                mid(mBrackSeq, i, 1).equals("O"))) {
                    mBrackSeq = left(mBrackSeq, i) + right(mBrackSeq,
                            mBrackSeq.length() - i - 1);
                }
                break;
            }
        }
    }

    @NonNull
    public MRestrictionArrayList copy() {
        MRestrictionArrayList ret = new MRestrictionArrayList(mAdv);
        ret.mBrackSeq = mBrackSeq;
        for (int i = 0; i <= size() - 1; i++) {
            ret.add(get(i).copy());
        }
        return ret;
    }
}

