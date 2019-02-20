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
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * These are functions which you create yourself. You can specify any number of
 * parameters to be used in the function call, and these can be any combination
 * of object references, character references, location references, numbers or
 * text. A text box is used to provide the text that is output by the function,
 * and this can contain other functions and embedded expressions to calculate
 * and format the desired output. If the output of the function needs to change
 * depending on restrictions then you can create Alternate Descriptions for full
 * control over the output text.
 */
public class MUserFunction extends MItem {

    private static final Pattern RE_ARG = Pattern.compile("\\d( )*[+-/*^]( )*\\d");

    @NonNull
    public
    String mName = "";
    @NonNull
    private MDescription mOutput;
    @NonNull
    private ArrayList<MArgument> mArgs = new ArrayList<>();

    private MUserFunction(@NonNull MAdventure adv) {
        super(adv);
        mOutput = new MDescription(adv);
    }

    public MUserFunction(@NonNull MAdventure adv,
                         @NonNull XmlPullParser xpp,
                         boolean isLibrary, boolean addDuplicateKeys,
                         double fileVersion) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "Function");

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

                    case "Name":
                        mName = xpp.nextText();
                        break;

                    case "Output":
                        mOutput = new MDescription(adv, xpp, fileVersion, "Output");
                        break;

                    case "Argument":
                        mArgs.add(new MUserFunction.MArgument(xpp));
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Function");

        if (!header.finalise(this, adv.mUDFs, isLibrary, addDuplicateKeys, null)) {
            throw new Exception();
        }
    }

    @NonNull
    private static ArrayList<String> splitArgs(@NonNull String sArgs) {
        ArrayList<String> lArgs = new ArrayList<>();
        int iLevel = 0;
        StringBuilder sArg = new StringBuilder();

        for (int i = 0; i < sArgs.length(); i++) {
            switch (sArgs.charAt(i)) {
                case ',':
                    if (iLevel == 0) {
                        if (sArg.length() > 0) {
                            lArgs.add(sArg.toString());
                        }
                        sArg = new StringBuilder();
                    } else {
                        sArg.append(sArgs.charAt(i));
                    }
                    break;
                case '(':
                case '[':
                    iLevel++;
                    sArg.append(sArgs.charAt(i));
                    break;
                case ')':
                case ']':
                    iLevel--;
                    sArg.append(sArgs.charAt(i));
                    break;
                default:
                    sArg.append(sArgs.charAt(i));
                    break;
            }
        }
        if (sArg.length() > 0) {
            lArgs.add(sArg.toString());
        }

        return lArgs;
    }

    public void evaluate(@NonNull StringBuilder text, @Nullable MReferenceList refs) {
        // Evaluate all occurrences of this udf in the given text and do inline
        // replacement of the evaluated result.

        // This will need to be a bit more sophisticated once we have arguments...
        Pattern re = Pattern.compile("%" + mName + "(\\[.*?\\])?%", Pattern.DOTALL);
        Matcher m = re.matcher(text);

        if (m.find()) {
            if (mOutput.toString().contains("%" + mName + "%")) {
                GLKLogger.error("Recursive User Defined Function - " + mName);
            } else {
                // Replace each parameter with it's resolved value
                String match = m.group(1); // group(0) is the entire string
                MDescription dOut = mOutput.copy();
                MReferenceList refsUDF = new MReferenceList(mAdv);
                if (match.contains("[") && match.contains("]")) {
                    String sArgs = match.substring(match.indexOf("[") + 1, match.lastIndexOf("]"));
                    ArrayList<String> sArg = splitArgs(sArgs);
                    int i = 0;
                    for (MArgument arg : mArgs) {
                        String evaluatedArg = mAdv.evalFuncs(sArg.get(i), refs);
                        if (evaluatedArg.contains("|")) {
                            // Means it evaluated to multiple items
                            // Depending on arg type, create an objects parameter, and set the refs
                            switch (arg.mType) {
                                case Object:
                                    MReference refOb = new MReference(MReference.ReferencesType.Object);
                                    for (String sOb : evaluatedArg.split("\\|")) {
                                        MReference.MReferenceItem itmOb = new MReference.MReferenceItem();
                                        itmOb.mMatchingKeys.add(sOb);
                                        itmOb.mIsExplicitlyMentioned = true;
                                        refOb.mItems.add(itmOb);
                                    }
                                    refsUDF.add(refOb);
                                    break;
                            }
                        }

                        // Our function argument could be an expression
                        Matcher m2 = RE_ARG.matcher(evaluatedArg);
                        if (m2.find()) {
                            evaluatedArg = mAdv.evalStrExpr(evaluatedArg, refs);
                        }

                        for (MSingleDescription d : dOut) {
                            d.mDescription = d.mDescription.replace("%" + arg.mName + "%", evaluatedArg);
                            for (MRestriction r : d.mRestrictions) {
                                if (r.mKey1.equals("Parameter-" + arg.mName)) {
                                    r.mKey1 = (evaluatedArg.contains("|") ? "ReferencedObjects" : evaluatedArg);
                                }
                                if (r.mKey2.equals("Parameter-" + arg.mName)) {
                                    r.mKey2 = (evaluatedArg.contains("|") ? "ReferencedObjects" : evaluatedArg);
                                }
                            }
                        }
                        i++;
                    }
                }

                String funcResult = dOut.toString(refsUDF);
                m.reset();
                text.setLength(0);
                text.append(mAdv.evalFuncs(m.replaceFirst(funcResult), refs));
            }
        }
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> all = new ArrayList<>();
        all.add(mOutput);
        return all;
    }

    @Nullable
    @Override
    public MUserFunction clone() {
        return (MUserFunction) super.clone();
    }

    @NonNull
    @Override
    public String getCommonName() {
        return mName;
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (MDescription d : getAllDescriptions()) {
            if (!d.deleteKey(key)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected int findLocal(@NonNull String toFind, @Nullable String toReplace,
                            boolean findAll, @NonNull int[] nReplaced) {
        int iCount = nReplaced[0];
        String[] n = new String[1];
        n[0] = mName;
        nReplaced[0] += MGlobals.find(n, toFind, toReplace);
        return nReplaced[0] - iCount;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        // do nothing
        return 0;
    }

    @NonNull
    @Override
    public String getSymbol() {
        // summation symbol
        return "\u8721";
    }

    public enum ArgumentType {
        Object,     // 0
        Character,  // 1
        Location,   // 2
        Number,     // 3
        Text        // 4
    }

    public static class MArgument {
        public ArgumentType mType = ArgumentType.Object;
        @NonNull
        String mName = "";

        MArgument() {
            super();
        }

        MArgument(@NonNull XmlPullParser xpp) throws IOException, XmlPullParserException {
            this();

            xpp.require(START_TAG, null, "Argument");

            int depth = xpp.getDepth();
            int eventType;

            while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT &&
                    xpp.getDepth() > depth) {
                if (eventType == START_TAG) {
                    switch (xpp.getName()) {
                        case "Name":
                            mName = xpp.nextText();
                            break;

                        case "Type":
                            mType = MUserFunction.ArgumentType.valueOf(xpp.nextText());
                            break;
                    }
                }
            }
            xpp.require(END_TAG, null, "Argument");
        }
    }
}
