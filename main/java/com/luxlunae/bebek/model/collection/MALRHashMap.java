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

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.model.MALR;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import static com.luxlunae.bebek.MGlobals.isSameString;
import static com.luxlunae.bebek.VB.cint;

public class MALRHashMap extends MItemHashMap<MALR> {
    private static final Pattern RE_CAPITALISE =
            Pattern.compile("^([a-z])|\n([a-z])|[a-z][.!?]\\s+([a-z])");
    @NonNull
    private final MAdventure mAdv;
    private int mALRLoop = 0;

    public MALRHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader) throws EOFException {
        int nALR = cint(reader.readLine());
        for (int i = 1; i <= nALR; i++) {
            MALR alr = new MALR(mAdv, reader, i);
            put(alr.getKey(), alr);
        }
    }

    public void evaluate(@NonNull StringBuilder text,
                         @Nullable MReferenceList refs) {
        evaluate(text, true, refs);
    }

    private void evaluate(@NonNull StringBuilder text, boolean autoCap,
                          @Nullable MReferenceList refs) {
        if (text.length() == 0) {
            return;
        }

        // "ALR processing happens after %variable% substitutions.
        // You cannot, for example, put %theobject% into an ALR
        // file and expect it to be replaced by the current value
        // of the theobject text variable.
        //
        // HTML-style codes are interpreted after ALR processing.
        // Feel free to use <b>, <i>, <br>, etc. in an ALR."
        mAdv.evaluateFunctions(text, refs);
        mAdv.evaluateStringExpressions(text, refs);

        boolean hasChanged = false;

        // Need to be careful to ensure this doesn't become an
        // infinite loop. E.g. consider the following ALR taken
        // from "To Hell in a Hamper":
        //
        //      "I pick up the cuckoo clock" =>
        //      "I pick up the cuckoo clock.
        //      [HUBERT_RETORT1=%random_var1%]"
        //
        // To avoid this descending into an infinite recursive
        // loop, after performing the substitution but before
        // calling evaluate, we replace all instances of the
        // old text in the new string with the Unicode placeholder
        // (U+FFFC). After returning from evaluate back to the
        // uppermost level, we then revert those placeholders
        // back to the original text.
        //
        // Also note that:
        //
        //  "Longer matches are matched first. Substitutions
        //  aren't quite done in the order they appear in your
        //  ALR file; the longer left sides are processed
        //  first. Only if the left sides are the same length
        //  will the matching happen in the same order as in
        //  the ALR."
        //
        // We assume that when this function is called the
        // ALRs attached to the adventure object have already
        // been sorted in order of descending length.
        for (MALR alr : values()) {
            String oldText = alr.getOldText();
            if (oldText.length() > 0 && text.indexOf(oldText) >= 0) {
                // Get the ALR text into a variable in case we
                // have DisplayOnce.
                StringBuilder newText =
                        new StringBuilder(alr.getNewText().toString());
                if (isSameString(text, newText)) {
                    break;
                }
                MGlobals.replaceAll(newText, oldText, "\uFFFC");
                mALRLoop++;
                evaluate(newText, false, refs);
                MGlobals.replaceAll(text, oldText, newText.toString());
                mALRLoop--;
                if (mALRLoop == 0) {
                    MGlobals.replaceAll(text, "\uFFFC", oldText);
                }
            }
        }

        // Auto-capitalise - needs to happen after ALR
        // replacements, as some replacements may be both
        // intra and start of sentences.
        if (autoCap) {
            Matcher m = RE_CAPITALISE.matcher(text);
            while (m.find()) {
                for (int i = 1; i < 4; i++) {
                    String grp = m.group(i);
                    if (grp != null && grp.length() > 0) {
                        int j = m.start(i);
                        char ch = text.charAt(j);
                        text.setCharAt(j, Character.toUpperCase(ch));
                        hasChanged = true;
                    }
                }
            }
        }

        // Do a second round of ALR replacements if we
        // auto-capped anything, as user may want to
        // replace the auto-capped version.
        if (hasChanged) {
            for (MALR alr : values()) {
                String oldText = alr.getOldText();
                if (oldText.length() > 0 && text.indexOf(oldText) >= 0) {
                    // Get the ALR text into a variable
                    // in case we have DisplayOnce.
                    StringBuilder newText =
                            new StringBuilder(alr.getNewText().toString());
                    if (isSameString(text, newText)) {
                        break;
                    }
                    evaluate(newText, false, refs);
                    MGlobals.replaceAll(text, oldText, newText.toString());
                }
            }
        }
    }

    @Override
    @Nullable
    public MALR put(@NonNull String key, @NonNull MALR alr) {
        mAdv.mAllItems.put(alr);
        return super.put(key, alr);
    }

    @Override
    @Nullable
    public MALR remove(Object key) {
        mAdv.mAllItems.remove(key);
        return super.remove(key);
    }

    public void sort() {
        // TODO: Can we do this sort in place??
        ArrayList<Entry<String, MALR>> tmp = new ArrayList<>(entrySet());
        clear();
        Collections.sort(tmp, new Comparator<Entry<String, MALR>>() {
            @Override
            public int compare(Entry<String, MALR> x, Entry<String, MALR> y) {
                return y.getValue().getOldText().length() -
                        x.getValue().getOldText().length();
            }
        });
        for (Entry<String, MALR> e : tmp) {
            put(e.getKey(), e.getValue());
        }
    }
}
