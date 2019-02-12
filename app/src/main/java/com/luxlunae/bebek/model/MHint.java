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
import com.luxlunae.bebek.model.collection.MRestrictionArrayList;
import com.luxlunae.bebek.model.io.MFileOlder;

import org.xmlpull.v1.XmlPullParser;

import java.io.EOFException;
import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MHint extends MItem {
    @NonNull
    public MRestrictionArrayList mRestrictions;
    @NonNull
    private String mQuestion = "";
    @NonNull
    private MDescription mSubtleHint;
    @NonNull
    private MDescription mSledgeHammerHint;

    private MHint(@NonNull MAdventure adv) {
        super(adv);
        mSubtleHint = new MDescription(adv);
        mSledgeHammerHint = new MDescription(adv);
        mRestrictions = new MRestrictionArrayList(adv);
    }

    MHint(@NonNull MAdventure adv, @NonNull MFileOlder.V4Reader reader,
          @NonNull String question) throws EOFException {
        // ADRIFT V3.90 and V4 Loader
        this(adv);

        setKey("Hint" + String.valueOf(adv.mHints.size() + 1));
        setQuestion(question);
        setSubtleHint(new MDescription(adv, MFileOlder.convertV4FuncsToV5(adv, reader.readLine())));
        setSledgeHammerHint(new MDescription(adv, MFileOlder.convertV4FuncsToV5(adv, reader.readLine())));
    }

    public MHint(@NonNull MAdventure adv,
                 @NonNull XmlPullParser xpp,
                 boolean isLibrary, boolean addDuplicateKeys, double version) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Hint");

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

                    case "Question":
                        setQuestion(xpp.nextText());
                        break;

                    case "Subtle":
                        setSubtleHint(new MDescription(adv, xpp, version, "Subtle"));
                        break;

                    case "Sledgehammer":
                        setSledgeHammerHint(new MDescription(adv, xpp, version, "Sledgehammer"));
                        break;

                    case "Restrictions":
                        mRestrictions = new MRestrictionArrayList(adv, xpp, version);
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Hint");

        if (!header.finalise(this, adv.mHints,
                isLibrary, addDuplicateKeys, null)) {
            throw new Exception();
        }
    }

    @NonNull
    public String getQuestion() {
        return mQuestion;
    }

    public void setQuestion(@NonNull String value) {
        mQuestion = value;
    }

    @NonNull
    public MDescription getSubtleHint() {
        return mSubtleHint;
    }

    private void setSubtleHint(@NonNull MDescription value) {
        mSubtleHint = value;
    }

    @NonNull
    public MDescription getSledgeHammerHint() {
        return mSledgeHammerHint;
    }

    private void setSledgeHammerHint(@NonNull MDescription value) {
        mSledgeHammerHint = value;
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> ret = new ArrayList<>();
        ret.add(getSledgeHammerHint());
        ret.add(getSubtleHint());
        return ret;
    }

    @Nullable
    @Override
    public MHint clone() {
        return (MHint) super.clone();
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getQuestion();
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (MDescription d : getAllDescriptions()) {
            if (!d.deleteKey(key)) {
                return false;
            }
        }
        return mRestrictions.deleteKey(key);
    }

    @Override
    protected int findLocal(@NonNull String toFind,
                            @Nullable String toReplace,
                            boolean findAll, @NonNull int[] nReplaced) {
        int iCount = nReplaced[0];
        String[] q = new String[1];
        q[0] = mQuestion;
        nReplaced[0] += MGlobals.find(q, toFind, toReplace);
        return nReplaced[0] - iCount;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        // do nothing
        return 0;
    }
}
