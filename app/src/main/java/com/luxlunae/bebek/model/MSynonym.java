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
import com.luxlunae.bebek.model.collection.MStringArrayList;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Synonyms are alternatives for commands entered in Runner. By adding a
 * synonym for a command, any time the player enters one of the
 * alternatives, it will be treated as though it was the original command.
 * <p>
 * For example, if yopu added a synonym of "put" to be "hang", then every
 * time the player types "hang" it would be treated as though they typed
 * "put". So the command "hang cloak on hook" would execute the put object
 * on object general task as if they had typed "put cloak on hook".
 */
public class MSynonym extends MItem {

    @NonNull
    private MStringArrayList arlFrom = new MStringArrayList();
    @NonNull
    private String sTo = "";

    public MSynonym(@NonNull MAdventure adv) {
        super(adv);
    }

    public MSynonym(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                    boolean bLibrary, boolean bAddDuplicateKeys) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "Synonym");

        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != XmlPullParser.END_DOCUMENT && xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        header.processTag(xpp);
                        break;

                    case "From":
                        getChangeFrom().add(xpp.nextText());
                        break;

                    case "To":
                        setChangeTo(xpp.nextText());
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "Synonym");

        if (!header.finalise(this,
                adv.mSynonyms, bLibrary, bAddDuplicateKeys, null)) {
            throw new Exception();
        }
    }

    @NonNull
    public MStringArrayList getChangeFrom() {
        return arlFrom;
    }

    @NonNull
    public String getChangeTo() {
        return sTo;
    }

    public void setChangeTo(@NonNull String value) {
        sTo = value;
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public MSynonym clone() {
        return (MSynonym) super.clone();
    }

    @NonNull
    @Override
    public String getCommonName() {
        StringBuilder sName = new StringBuilder();
        for (String sFrom : getChangeFrom()) {
            if (!sName.toString().equals("")) {
                sName.append(", ");
            }
            sName.append(sFrom);
        }
        sName.append(" -> ").append(getChangeTo());
        return sName.toString();
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
        String[] t = new String[1];
        for (String sFrom : getChangeFrom()) {
            t[0] = sFrom;
            nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        }
        t[0] = sTo;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        return nReplaced[0] - iCount;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int iCount = 0;
        for (MDescription d : getAllDescriptions()) {
            iCount += d.getNumberOfKeyRefs(key);
        }
        return iCount;
    }

    @NonNull
    @Override
    public String getSymbol() {
        // twisted rightwards arrows
        return new String(Character.toChars(0x1F500));
    }
}
