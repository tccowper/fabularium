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
import com.luxlunae.bebek.model.MVariable;
import com.luxlunae.bebek.model.io.MFileOlder;

import java.io.EOFException;

import static com.luxlunae.bebek.MGlobals.replaceAllIgnoreCase;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.model.MVariable.VariableType.Numeric;

public class MVariableHashMap extends MItemHashMap<MVariable> {
    @NonNull
    private final MAdventure mAdv;

    public MVariableHashMap(@NonNull MAdventure adv) {
        super();
        mAdv = adv;
    }

    public void load(@NonNull MFileOlder.V4Reader reader, double version) throws EOFException {
        int nVariables = cint(reader.readLine());
        for (int i = 1; i <= nVariables; i++) {
            MVariable var = new MVariable(mAdv, reader, i, version);
            put(var.getKey(), var);
        }
    }

    public void evaluate(@NonNull StringBuilder text,
                         boolean isExpr, @Nullable MReferenceList refs) {
        final String tmp = text.toString().toLowerCase();

        for (MVariable var : values()) {
            int lenVar = var.getLength();
            if (lenVar == 1) {
                // -------------------------------------------
                //   SEARCHING FOR SCALAR VARIABLE REFERENCE
                // -------------------------------------------
                final String tgt = ("%" + var.getName() + "%").toLowerCase();
                if (tmp.contains(tgt)) {
                    // There is a least one occurrence of this
                    // variable in the given string, so proceed
                    // to evaluate the variable, then do the
                    // replacement(s).
                    final String repl = (var.getType() == Numeric) ?
                            String.valueOf(var.getInt()) :
                            isExpr ? "\"" + var.getStr() + "\"" : var.getStr();
                    replaceAllIgnoreCase(text, tgt, repl);
                }
            } else if (lenVar > 1) {
                // -------------------------------------------
                //   SEARCHING FOR ARRAY VARIABLE REFERENCE
                // -------------------------------------------
                final String tgt = ("%" + var.getName() + "[").toLowerCase();
                final int lenTgt1 = tgt.length();

                int pos1 = tmp.indexOf(tgt);
                int off = 0;
                int pos2;

                while (pos1 >= 0) {
                    // We have found the start of a reference
                    // to this array variable.
                    pos2 = pos1 + off;

                    // Work out the index (could itself be an
                    // expression, which we need to evaluate).
                    String index = text.substring(pos2 + lenTgt1,
                            text.indexOf("]", pos2));
                    int i;
                    if (isNumeric(index)) {
                        i = mAdv.safeInt(index);
                    } else {
                        MVariable varIndex = new MVariable(mAdv);
                        varIndex.setType(Numeric);
                        varIndex.setToExpr(index, refs);
                        i = varIndex.getInt();
                    }

                    // Evaluate the value of the variable at the
                    // given index and replace any references to
                    // it in the given string.
                    final String repl = (var.getType() == Numeric) ?
                            String.valueOf(var.getIntAt(i)) :
                            isExpr ? "\"" + var.getStrAt(i) + "\"" : var.getStrAt(i);
                    final int lenTgt = lenTgt1 + index.length() + 2; // add 2 for ']%'
                    final int lenRep = repl.length();
                    final int lenDiff = lenRep - lenTgt;

                    text.replace(pos2, pos2 + lenTgt, repl);
                    off += lenDiff;
                    pos1 = tmp.indexOf(tgt, pos1 + lenTgt);
                }
            }
        }
    }

    @Override
    @Nullable
    public MVariable put(@NonNull String key, @NonNull MVariable var) {
        mAdv.mAllItems.put(var);
        return super.put(key, var);
    }

    @Override
    @Nullable
    public MVariable remove(Object key) {
        mAdv.mAllItems.remove(key);
        return super.remove(key);
    }
}
