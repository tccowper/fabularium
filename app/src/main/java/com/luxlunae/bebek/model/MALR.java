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
import com.luxlunae.bebek.model.io.MFileOlder;

import org.xmlpull.v1.XmlPullParser;

import java.io.EOFException;
import java.util.ArrayList;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Text Overrides are the new name for the v4 ADRIFT Language Resource (ALR). Rather
 * than being edited externally and imported as in v4, v5 Text Overrides are edited
 * within the application.
 * <p>
 * Text Overrides simply replace the final text before it is displayed to the end user.
 * This provides a convenient way of globally changing particular output without having
 * to modify each individual item. It is also useful for changing any "fixed" text, i.e.
 * anything output by the application that is not stored as part of the library. (NB.
 * I am working to reduce this all the time, and hope to eventually store all text
 * within the library.)
 * <p>
 * Text Overrides can also be a useful tool to use for the ALR's original purpose; a
 * language resource to convert any English output to other languages. This shouldn't
 * be such an issue with v5 as different libraries can be created for each language.
 * <p>
 * Text Overrides are applied in order of length. This ensures an override that is a
 * subset of a longer one does not prevent the longer one from overriding.
 */
public class MALR extends MItem {
    private String mOldText;
    private MDescription mNewText;

    private MALR(@NonNull MAdventure adv) {
        super(adv);
        setNewText(new MDescription(adv));
    }

    /**
     * Create a new ALR from a v3.8, 3.9 or 4.0 Adrift file.
     *
     * @param adv    - the Adventure object.
     * @param reader - the file reader. When this constructor is called, the
     *               reader should be positioned at the start of the ALR
     *               object in the file.
     * @param iALR   - the number of this ALR.
     * @throws EOFException - if the reader encounters EOF before we have loaded
     *                      all the expected fields of the ALR object (i.e. file is malformed).
     */
    public MALR(@NonNull MAdventure adv,
                @NonNull MFileOlder.V4Reader reader, int iALR) throws EOFException {
        this(adv);

        setKey("ALR" + iALR);
        setOldText(reader.readLine());
        setNewText(new MDescription(adv, MFileOlder.convertV4FuncsToV5(adv, reader.readLine())));
    }

    /**
     * Create a new ALR from a v5.0 Adrift file.
     *
     * @param adv               - the Adventure object.
     * @param xpp               - the XML parser. When this constructor is called this
     *                          parser should be positioned at the opening tag of the ALR
     *                          object in the file.
     * @param bLibrary          - whether were are loading from a library file.
     * @param bAddDuplicateKeys - whether this ALR should be loaded if it has the
     *                          same key as another item that has already been loaded.
     * @param dFileVersion      - the ADRIFT version used to create the XML file attached
     *                          to the XML parser.
     * @throws Exception - if XML parser encounters invalid XML tags, EOF, etc.
     */
    public MALR(@NonNull MAdventure adv, @NonNull XmlPullParser xpp, boolean bLibrary,
                boolean bAddDuplicateKeys, double dFileVersion) throws Exception {
        this(adv);

        xpp.require(START_TAG, null, "TextOverride");

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

                    case "OldText":
                        setOldText(xpp.nextText());
                        break;

                    case "NewText":
                        setNewText(new MDescription(adv, xpp,
                                dFileVersion, "NewText"));
                        break;
                }
            }
        }
        xpp.require(END_TAG, null, "TextOverride");

        if (!header.finalise(this, adv.mALRs,
                bLibrary, bAddDuplicateKeys, null)) {
            throw new Exception();
        }
    }

    @NonNull
    public String getOldText() {
        return mOldText;
    }

    private void setOldText(@NonNull String value) {
        mOldText = value;
    }

    @NonNull
    public MDescription getNewText() {
        if (mNewText == null) {
            mNewText = new MDescription(mAdv);
        }
        return mNewText;
    }

    public void setNewText(@NonNull MDescription value) {
        mNewText = value;
    }

    @Nullable
    @Override
    public MItem clone() {
        MALR ret = new MALR(mAdv);
        ret.mOldText = mOldText;
        ret.mNewText = mNewText.copy();
        return ret;
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getOldText();
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        ArrayList<MDescription> all = new ArrayList<>();
        all.add(getNewText());
        return all;
    }

    @Override
    public int findLocal(@NonNull String toFind, String toReplace,
                         boolean findAll, @NonNull int[] nReplaced) {
        int iCount = nReplaced[0];
        String[] t = new String[1];
        t[0] = mOldText;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        mOldText = t[0];
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
        // abc blocks
        return new String(Character.toChars(0x1F524));
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
}
