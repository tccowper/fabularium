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
import com.luxlunae.bebek.controller.MController;
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.model.io.MFileOlder;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlSerializer;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static com.luxlunae.bebek.MGlobals.REFERENCE_NAMES;
import static com.luxlunae.bebek.MGlobals.getArgs;
import static com.luxlunae.bebek.MGlobals.instr;
import static com.luxlunae.bebek.MGlobals.left;
import static com.luxlunae.bebek.MGlobals.mid;
import static com.luxlunae.bebek.MGlobals.right;
import static com.luxlunae.bebek.MGlobals.toProper;
import static com.luxlunae.bebek.VB.cint;
import static com.luxlunae.bebek.VB.isNumeric;
import static com.luxlunae.bebek.VB.val;
import static com.luxlunae.bebek.model.MAdventure.MGameState.restoreDisplayOnce;
import static com.luxlunae.bebek.model.MAdventure.MGameState.saveDisplayOnce;
import static com.luxlunae.bebek.model.MVariable.OpType.ASSIGNMENT;
import static com.luxlunae.bebek.model.MVariable.TokenType.COMMA;
import static com.luxlunae.bebek.model.MVariable.TokenType.EXPR;
import static com.luxlunae.bebek.model.MVariable.TokenType.FUNCT;
import static com.luxlunae.bebek.model.MVariable.TokenType.LOGIC;
import static com.luxlunae.bebek.model.MVariable.TokenType.LP;
import static com.luxlunae.bebek.model.MVariable.TokenType.NEGATE;
import static com.luxlunae.bebek.model.MVariable.TokenType.OP;
import static com.luxlunae.bebek.model.MVariable.TokenType.RP;
import static com.luxlunae.bebek.model.MVariable.TokenType.TEST;
import static com.luxlunae.bebek.model.MVariable.TokenType.TESTOP;
import static com.luxlunae.bebek.model.MVariable.VariableType.Numeric;
import static com.luxlunae.bebek.model.MVariable.VariableType.Text;
import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * There are two types of variables in ADRIFT. These are Numeric (Integer) and Text. Both
 * variable types can also be created as arrays (a set of variable values which can be
 * accessed by a unique index starting from 1).
 * <p>
 * Variables can be compared with other values in Restrictions, and altered using Actions.
 * <p>
 * Properties and references can also be used in a similar way to variables.
 */
public class MVariable extends MItem {
    @NonNull
    private String mName = "";
    @Nullable
    private int[] mIntVals;
    @Nullable
    private String[] mStrVals;
    private int mLength = 1;
    private VariableType mType = Numeric;

    public MVariable(@NonNull MAdventure adv) {
        super(adv);
    }

    public MVariable(@NonNull MAdventure adv, @NonNull MFileOlder.V4Reader reader,
                     int iVar, double v) throws EOFException {
        // ADRIFT V3.90 and V4 Loader
        this(adv);

        setKey("Variable" + iVar);
        setName(reader.readLine());
        if (v < 4) {
            setType(Numeric);
            set(cint(reader.readLine()));
        } else {
            setType(getTypeFromInt(cint(reader.readLine())));
            if (getType() == Numeric) {
                set(cint(reader.readLine()));
            } else {
                set(reader.readLine());
            }
        }
    }
    
    public MVariable(@NonNull MAdventure adv, @NonNull XmlPullParser xpp,
                     boolean isLibrary, boolean addDupKeys) throws Exception {
        // ADRIFT V5 Loader
        this(adv);

        xpp.require(START_TAG, null, "Variable");

        String initValue = "";
        ItemHeaderDetails header = new ItemHeaderDetails();
        int depth = xpp.getDepth();
        int evType;

        while ((evType = xpp.nextTag()) != END_DOCUMENT && xpp.getDepth() > depth) {
            if (evType == START_TAG) {
                switch (xpp.getName()) {
                    default:
                        header.processTag(xpp);
                        break;

                    case "Name":
                        setName(xpp.nextText());
                        break;

                    case "Type":
                        setType(VariableType.valueOf(xpp.nextText()));
                        break;

                    case "InitialValue":
                        initValue = xpp.nextText();
                        break;

                    case "ArrayLength":
                        String len = xpp.nextText();
                        if (len != null) {
                            setLength(cint(len));
                        }
                        break;

                }
            }
        }
        xpp.require(END_TAG, null, "Variable");

        if (!header.finalise(this,
                adv.mVariables, isLibrary, addDupKeys, null)) {
            throw new Exception();
        }

        // Set the variable's initial value(s).
        VariableType varType = getType();
        if (varType == Text || (getLength() > 1 && initValue.contains(","))) {
            if (varType == Numeric) {
                String vals[] = initValue.split(",");
                for (int i = 1; i <= vals.length; i++) {
                    setAt(i, cint(vals[i - 1]));
                }
                set(initValue);
            } else {
                for (int i = 1; i <= getLength(); i++) {
                    setAt(i, initValue);
                }
            }
        } else {
            for (int i = 1; i <= getLength(); i++) {
                setAt(i, cint(initValue));
            }
        }
    }

    private static VariableType getTypeFromInt(int value) {
        switch (value) {
            default:
            case 0:
                return Numeric;
            case 1:
                return Text;
        }
    }

    @NonNull
    private static StringBuilder stripRedundantSpaces(@NonNull String expr) {
        boolean okToStrip = true;
        String chunk;
        StringBuilder ret = new StringBuilder();

        while (!expr.equals("")) {
            int pos = instr(1, expr, "\"");
            if (pos > 0) {
                chunk = left(expr, pos);
                expr = right(expr, expr.length() - pos);
                if (okToStrip) {
                    chunk = chunk.replace(" ", "")
                            .replace("\n", "");
                }
                okToStrip = !okToStrip;
            } else {
                chunk = expr;
                if (okToStrip) {
                    chunk = chunk.replace(" ", "")
                            .replace("\n", "");
                }
                expr = "";
            }
            ret.append(chunk);
        }

        return ret;
    }

    private static boolean isValidVLUChar(@NonNull Character ch) {
        // is ch a valid vlu char?
        // in other words it must not be one of the operators
        // "*", "/", "+", "-", "^", "(", ")", ",", "-", "%",
        // "|", "&", "=", "<", ">", "!", "\""
        switch (ch) {
            case '*':
            case '/':
            case '+':
            case '-':
            case '^':
            case '(':
            case ')':
            case ',':
            case '%':
            case '|':
            case '&':
            case '=':
            case '<':
            case '>':
            case '!':
            case '"':
                return false;
            default:
                return true;
        }
    }

    @NonNull
    private Token getToken(@NonNull StringBuilder text,
                           int index, @Nullable MReferenceList refs) throws Exception {
        int lenText = text.length();
        String rem = text.toString();
        String remLower = rem.toLowerCase();

        Token tok = new Token();

        switch (left(remLower, 1)) {
            case "*":
            case "/":
            case "+":
            case "-":
            case "^":
                tok.mValue = left(rem, 1);
                tok.mType = OP;
                text.delete(0, 1);
                return tok;

            case "(":
                tok.mValue = left(rem, 1);
                tok.mType = LP;
                text.delete(0, 1);
                return tok;

            case ")":
                tok.mValue = left(rem, 1);
                tok.mType = RP;
                text.delete(0, 1);
                return tok;

            case ",":
                tok.mValue = left(rem, 1);
                tok.mType = COMMA;
                text.delete(0, 1);
                return tok;

            case "0":
            case "1":
            case "2":
            case "3":
            case "4":
            case "5":
            case "6":
            case "7":
            case "8":
            case "9":
                // 0 To 9
                int n = 1;
                boolean hasDP = false;
                while (isNumeric(mid(rem, n, 1)) ||
                        (!hasDP && mid(rem, n, 1).equals("."))) {
                    if (mid(rem, n, 1).equals(".")) {
                        hasDP = true;
                    }
                    n++;
                }
                n--;
                tok.mValue = left(rem, n);
                tok.mType = EXPR;
                text.delete(0, n);
                return tok;

            case "\"":
                if (instr(2, rem, "\"") > 0) {
                    String tmp = mid(rem, 2, instr(2, rem, "\"") - 2);
                    tok.mValue = tmp;
                    tok.mType = EXPR;
                    text.delete(0, tmp.length() + 2);
                    return tok;
                }
                break;

            case "'":
                if (instr(2, rem, "'") > 0) {
                    String tmp = mid(rem, 2, instr(2, rem, "'") - 2);
                    tok.mValue = tmp;
                    tok.mType = EXPR;
                    text.delete(0, tmp.length() + 2);
                    return tok;
                }
                break;

            case "%":
                for (MVariable var : mAdv.mVariables.values()) {
                    String vName = var.getName();
                    String vNameLower = vName.toLowerCase();
                    int lenVName = vName.length();
                    if (lenVName > 1 && remLower.startsWith("%" + vNameLower + "[")) {
                        String args = getArgs(text,
                                remLower.indexOf("%" + vNameLower + "[") + lenVName);
                        int vIndex = mAdv.safeInt(mAdv.evalFuncs(args, refs));
                        if (var.getType() == Text) {
                            tok.mValue = var.getStrAt(vIndex);
                        } else {
                            tok.mValue = String.valueOf(var.getIntAt(vIndex));
                        }
                        tok.mType = EXPR;
                        text.replace(0, lenText, rem.replace("%" +
                                vName + "[" + args + "]%", ""));
                        return tok;
                    }
                    if (rem.startsWith("%" + vName + "%")) {
                        if (var.getType() == Text) {
                            tok.mValue = var.getStrAt(1);
                        } else {
                            tok.mValue = String.valueOf(var.getIntAt(1));
                        }
                        tok.mType = EXPR;
                        text.delete(0, lenVName + 2);
                        return tok;
                    }
                }
                if (remLower.startsWith("%loop%")) {
                    tok.mValue = String.valueOf(index);
                    tok.mType = EXPR;
                    text.delete(0, 6);
                    return tok;
                }
                if (remLower.startsWith("%maxscore%")) {
                    tok.mValue = String.valueOf(mAdv.getMaxScore());
                    tok.mType = EXPR;
                    text.delete(0, 10);
                    return tok;
                }
                if (remLower.startsWith("%number%")) {
                    tok.mValue = "";
                    if (refs != null) {
                        for (MReference ref : refs) {
                            if (ref.mType == MReference.ReferencesType.Number) {
                                if (ref.mItems.size() == 1) {
                                    if (ref.mItems.get(0).mMatchingKeys.size() == 1) {
                                        tok.mValue = ref.mItems.get(0).mMatchingKeys.get(0);
                                    }
                                }
                            }
                        }
                    }
                    tok.mType = EXPR;
                    text.delete(0, 8);
                    return tok;
                }
                if (remLower.startsWith("%score%")) {
                    tok.mValue = String.valueOf(mAdv.getScore());
                    tok.mType = EXPR;
                    text.delete(0, 7);
                    return tok;
                }
                if (remLower.startsWith("%time%")) {
                    tok.mValue = "0";
                    tok.mType = EXPR;
                    text.delete(0, 6);
                    return tok;
                }
                if (remLower.startsWith("%turns%")) {
                    tok.mValue = String.valueOf(mAdv.mTurns);
                    tok.mType = EXPR;
                    text.delete(0, 7);
                    return tok;
                }
                if (remLower.startsWith("%version%")) {
                    String[] version = MController.AdriftProductVersion.split("\\.");
                    tok.mValue = version[0] +
                            String.format(Locale.UK, "%02d", mAdv.safeInt(version[1])) +
                            String.format(Locale.UK, "%04d", mAdv.safeInt(version[2]));
                    tok.mType = EXPR;
                    text.delete(0, 9);
                    return tok;
                }
                if (remLower.startsWith("%text%")) {
                    tok.mValue = "";
                    if (refs != null) {
                        for (MReference ref : refs) {
                            if (ref.mType == MReference.ReferencesType.Text) {
                                if (ref.mItems.size() == 1) {
                                    if (ref.mItems.get(0).mMatchingKeys.size() == 1) {
                                        tok.mValue = ref.mItems.get(0).mMatchingKeys.get(0);
                                    }
                                }
                            }
                        }
                    }
                    tok.mType = EXPR;
                    text.delete(0, 6);
                    return tok;
                }
                if (remLower.startsWith("%player%")) {
                    tok.mValue = mAdv.getPlayer().getName();
                    tok.mType = EXPR;
                    text.delete(0, 8);
                    return tok;
                }
                for (String fn : mAdv.getFunctionNames()) {
                    if (remLower.startsWith("%" + fn.toLowerCase() + "[") &&
                            remLower.contains("]%")) {
                        String args = getArgs(text,
                                remLower.indexOf("%" + fn + "[") + fn.length() + 1);
                        String tmp = "fun-%" + fn + "[" + args + "]%";
                        tok.mValue = mAdv.evalFuncs(tmp.substring(4), refs);
                        tok.mType = EXPR;
                        text.replace(0, lenText, rem.replace(tmp.substring(4), ""));
                        return tok;
                    }
                }
                for (String ref : REFERENCE_NAMES) {
                    if (remLower.startsWith(ref.toLowerCase())) {
                        tok.mValue = mAdv.evalFuncs(ref, refs);
                        tok.mType = EXPR;
                        text.delete(0, ref.length());
                        return tok;
                    }
                }
                break;

            case "|":
                tok.mValue = "OR";
                tok.mType = LOGIC;
                if (rem.startsWith("||")) {
                    text.delete(0, 2);
                    return tok;
                }
                text.delete(0, 1);
                return tok;

            case "&":
                tok.mValue = "AND";
                tok.mType = LOGIC;
                if (rem.startsWith("&&")) {
                    text.delete(0, 2);
                    return tok;
                }
                text.delete(0, 1);
                return tok;

            case "=":
                tok.mValue = "EQ";
                tok.mType = TESTOP;
                if (rem.startsWith("==")) {
                    text.delete(0, 2);
                    return tok;
                }
                text.delete(0, 1);
                return tok;

            case "<":
                if (rem.startsWith("<>")) {
                    tok.mValue = "NE";
                    tok.mType = TESTOP;
                    text.delete(0, 2);
                    return tok;
                }
                if (rem.startsWith("<=")) {
                    tok.mValue = "LE";
                    tok.mType = TESTOP;
                    text.delete(0, 2);
                    return tok;
                }
                tok.mValue = "LT";
                tok.mType = TESTOP;
                text.delete(0, 1);
                return tok;

            case ">":
                if (rem.startsWith(">=")) {
                    tok.mValue = "GE";
                    tok.mType = TESTOP;
                    text.delete(0, 2);
                    return tok;
                }
                tok.mValue = "GT";
                tok.mType = TESTOP;
                text.delete(0, 1);
                return tok;

            case "!":
                if (rem.startsWith("!=")) {
                    tok.mValue = "NE";
                    tok.mType = TESTOP;
                    text.delete(0, 2);
                    return tok;
                }
                tok.mValue = "!";
                tok.mType = NEGATE;
                text.delete(0, 1);
                return tok;

            case "a":
                if (remLower.startsWith("and")) {
                    tok.mValue = "AND";
                    tok.mType = LOGIC;
                    text.delete(0, 3);
                    return tok;
                }
                if (remLower.startsWith("abs")) {
                    tok.mValue = "abs";
                    tok.mType = FUNCT;
                    text.delete(0, 3);
                    return tok;
                }
                break;

            case "e":
                if (remLower.startsWith("either")) {
                    tok.mValue = "either";
                    tok.mType = FUNCT;
                    text.delete(0, 6);
                    return tok;
                }
                break;

            case "i":
                if (remLower.startsWith("if")) {
                    tok.mValue = "if";
                    tok.mType = FUNCT;
                    text.delete(0, 2);
                    return tok;
                }
                if (remLower.startsWith("instr")) {
                    tok.mValue = "ist";
                    tok.mType = FUNCT;
                    text.delete(0, 5);
                    return tok;
                }
                break;

            case "l":
                if (remLower.startsWith("lower") ||
                        remLower.startsWith("lcase")) {
                    tok.mValue = "lwr";
                    tok.mType = FUNCT;
                    text.delete(0, 5);
                    return tok;
                }
                if (remLower.startsWith("left")) {
                    tok.mValue = "lft";
                    tok.mType = FUNCT;
                    text.delete(0, 4);
                    return tok;
                }
                if (remLower.startsWith("lenText")) {
                    tok.mValue = "lenText";
                    tok.mType = FUNCT;
                    text.delete(0, 3);
                    return tok;
                }
                break;

            case "m":
                if (remLower.startsWith("max")) {
                    tok.mValue = "max";
                    tok.mType = FUNCT;
                    text.delete(0, 3);
                    return tok;
                }
                if (remLower.startsWith("min")) {
                    tok.mValue = "min";
                    tok.mType = FUNCT;
                    text.delete(0, 3);
                    return tok;
                }
                if (remLower.startsWith("mod")) {
                    tok.mValue = "mod";
                    tok.mType = OP;
                    text.delete(0, 3);
                    return tok;
                }
                if (remLower.startsWith("mid")) {
                    tok.mValue = "mid";
                    tok.mType = FUNCT;
                    text.delete(0, 3);
                    return tok;
                }
                break;

            case "o":
                if (remLower.startsWith("or")) {
                    tok.mValue = "OR";
                    tok.mType = LOGIC;
                    text.delete(0, 2);
                    return tok;
                }
                if (remLower.startsWith("oneof")) {
                    tok.mValue = "oneof";
                    tok.mType = FUNCT;
                    text.delete(0, 5);
                    return tok;
                }
                break;

            case "p":
                if (remLower.startsWith("proper")) {
                    tok.mValue = "ppr";
                    tok.mType = FUNCT;
                    text.delete(0, 6);
                    return tok;
                }
                if (remLower.startsWith("pcase")) {
                    tok.mValue = "ppr";
                    tok.mType = FUNCT;
                    text.delete(0, 5);
                    return tok;
                }
                break;

            case "r":
                if (remLower.startsWith("rand")) {
                    tok.mValue = "rand";
                    tok.mType = FUNCT;
                    text.delete(0, 4);
                    return tok;
                }
                if (remLower.startsWith("replace")) {
                    tok.mValue = "replace";
                    tok.mType = FUNCT;
                    text.delete(0, 7);
                    return tok;
                }
                if (remLower.startsWith("right")) {
                    tok.mValue = "rgt";
                    tok.mType = FUNCT;
                    text.delete(0, 5);
                    return tok;
                }
                break;

            case "s":
                if (remLower.startsWith("str")) {
                    tok.mValue = "str";
                    tok.mType = FUNCT;
                    text.delete(0, 3);
                    return tok;
                }
                break;

            case "u":
                if (remLower.startsWith("upper") ||
                        remLower.startsWith("ucase")) {
                    tok.mValue = "upr";
                    tok.mType = FUNCT;
                    text.delete(0, 5);
                    return tok;
                }
                if (remLower.startsWith("urand")) {
                    tok.mValue = "urand";
                    tok.mType = FUNCT;
                    text.delete(0, 5);
                    return tok;
                }
                break;

            case "v":
                if (remLower.startsWith("val")) {
                    tok.mValue = "val";
                    tok.mType = FUNCT;
                    text.delete(0, 3);
                    return tok;
                }
                break;

            default:
                break;
        }

        // If we have reached this point,
        // assume the token is a string.
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < lenText; i++) {
            char ch = rem.charAt(i);
            if (!isValidVLUChar(ch)) {
                break;
            }
            ret.append(ch);
        }
        if (ret.length() > 0) {
            tok.mValue = ret.toString();
            tok.mType = EXPR;
            text.delete(0, ret.length());
            return tok;
        }

        throw new Exception("Bad token");
    }

    /**
     * Converts the given expression into an array of linked tokens.
     *
     * @param expr  - the expression to tokenise.
     * @param index - the
     * @param refs  - current reference values.
     * @return the array of linked tokens or NULL if no tokens created.
     * @throws Exception if there was a parse error.
     */
    @Nullable
    private TokenList tokenise(@NonNull StringBuilder expr,
                               int index, @Nullable MReferenceList refs) throws Exception {
        String rem = expr.toString();
        String prev = "972398";
        TokenList tokens = new TokenList();

        while (!rem.equals("") && !rem.equals(prev)) {
            prev = rem;
            Token tok = getToken(expr, index, refs);
            rem = expr.toString();
            //tok.TokenType1 = tok.TokenType1.replace("~@+#~", "\"");
            if (tokens.mTokens.size() > 0) {
                Token lastTok = tokens.mTokens.get(tokens.mTokens.size() - 1);
                lastTok.setR(tok);
                tok.setL(lastTok);
            }
            tokens.mTokens.add(tok);
        }

        return (tokens.mTokens.size() != 0) ? tokens : null;
    }

    @Nullable
    private String parse(@NonNull TokenList tokens) {
        Token tokL, tokOp, tokOp2, tokR, tokR2, tokCur, tokLP, tokRP, tokFunc;
        boolean badExpr = false;
        int run = 1;

        // Do while more than one token
        tokCur = tokens.getFirst();
        nxt:
        while (!(tokCur.getL() == null && tokCur.getR() == null)) {
            tokens.mChanged = false;

            if (tokCur.getR() != null) {
                // -----------------------
                //       >= 2 TOKENS
                // -----------------------
                if (tokCur.getR().getR() != null) {
                    // -----------------------
                    //      >= 3 TOKENS
                    // -----------------------
                    if (tokCur.getR().getR().getR() != null) {
                        // -----------------------
                        //      >= 4 TOKENS
                        //      E.g. abs(a)
                        // -----------------------
                        tokFunc = tokCur;
                        tokLP = tokCur.getR();
                        tokL = tokLP.getR();
                        tokRP = tokL.getR();

                        if (tokFunc.mType == FUNCT && tokLP.mType == LP &&
                                tokL.mType == EXPR && tokRP.mType == RP) {
                            switch (tokFunc.mValue) {
                                case "abs":
                                    // ------------------
                                    //      Abs(x)
                                    // ------------------
                                    tokens.add(EXPR,
                                            String.valueOf(Math.abs(mAdv.safeInt(tokL.mValue))),
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "upr":
                                    // ------------------
                                    //    UCase(text)
                                    // ------------------
                                    // Converts <text> to all upper case.
                                    tokens.add(EXPR,
                                            tokL.mValue.toUpperCase(),
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "lwr":
                                    // ------------------
                                    //    LCase(text)
                                    // ------------------
                                    // Converts <text> to all lower case.
                                    tokens.add(EXPR,
                                            tokL.mValue.toLowerCase(),
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "ppr":
                                    // ------------------
                                    //    PCase(text)
                                    // ------------------
                                    // Converts <text> to proper case.
                                    // (Capitalise the first letter of
                                    //  each sentence, remainder unchanged).
                                    StringBuilder sb = new StringBuilder(tokL.mValue);
                                    toProper(sb);
                                    tokens.add(EXPR,
                                            sb.toString(),
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "len":
                                    // ------------------
                                    //     Len(text)
                                    // ------------------
                                    // Returns the length of <text>.
                                    tokens.add(EXPR,
                                            String.valueOf(tokL.mValue.length()),
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "val":
                                    // ------------------
                                    //     Val(text)
                                    // ------------------
                                    // Converts <text> to a number (or zero
                                    // if it can't match).
                                    tokens.add(EXPR,
                                            tokL.mValue,
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "str":
                                    // ------------------
                                    //      Str(x)
                                    // ------------------
                                    // Converts an integer value x to text
                                    // form. Negative numbers are preceded
                                    // with "-" and positive numbers with a
                                    // space.
                                    tokens.add(EXPR,
                                            tokL.mValue,
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "rand":
                                    // ------------------
                                    //     Rand(x)
                                    // ------------------
                                    // Returns a random value between
                                    // 0 and x.
                                    tokens.add(EXPR,
                                            String.valueOf(mAdv.getRand(0,
                                                    mAdv.safeInt(tokL.mValue))),
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                                case "urand":
                                    // ------------------
                                    //      URand(x)
                                    // ------------------
                                    // Returns a unique random value between 0
                                    // and x. When called multiple times no
                                    // value will be repeated until all of the
                                    // values between 0 and x have been returned.
                                    tokens.add(EXPR,
                                            String.valueOf(mAdv.getRandNoRepeat(0,
                                                    mAdv.safeInt(tokL.mValue))),
                                            tokFunc.getL(), tokRP.getR());
                                    break;
                            }
                        }

                        if (tokCur.getR().getR().getR().getR() != null) {
                            // ---------------
                            //   >= 5 TOKENS
                            // ---------------
                            if (tokCur.getR().getR().getR().getR().getR() != null) {
                                // ---------------
                                //   >= 6 TOKENS
                                // ---------------
                                if (tokCur.getR().getR().getR().getR().getR().getR() != null) {
                                    // ---------------
                                    //   >= 7 TOKENS
                                    // ---------------
                                    if (tokCur.getR().getR().getR().getR().getR().getR().getR() != null) {
                                        // ---------------
                                        //   >= 8 TOKENS
                                        // ---------------
                                        // Conditionals:
                                        //   funct lp test|expr comma expr comma expr rp
                                        //   e.g. if (test, a, b)
                                        //
                                        // Anything else:
                                        //   funct lp expr comma expr comma expr rp
                                        //   e.g. mid (text, start, length)
                                        tokFunc = tokCur;
                                        tokLP = tokCur.getR();
                                        tokL = tokLP.getR();
                                        tokOp = tokL.getR();
                                        tokR = tokOp.getR();
                                        tokOp2 = tokR.getR();
                                        tokR2 = tokOp2.getR();
                                        tokRP = tokR2.getR();

                                        if (tokFunc.mType == FUNCT &&
                                                tokLP.mType == LP &&
                                                (tokL.mType == TEST || tokL.mType == EXPR) &&
                                                tokOp.mType == COMMA && tokR.mType == EXPR &&
                                                tokOp2.mType == COMMA && tokR2.mType == EXPR &&
                                                tokRP.mType == RP) {
                                            switch (tokFunc.mValue) {
                                                case "if":
                                                    // ------------------
                                                    //   If(test,x,y)
                                                    // ------------------
                                                    // If "test" evaluates
                                                    // true, returns x, else returns y
                                                    //
                                                    // Where "test" is a=b, a==b, a<b,
                                                    // a<=b, a>b, a>=b, a<>b, a!=b.
                                                    // Conditions can be ANDed using
                                                    // "and", "&" or "&&" or ORed using
                                                    // "or", "|", "||",
                                                    //
                                                    // E.g.
                                                    //
                                                    //   IF(%variable1%=1,%variable2%+1,RAND(5,7))
                                                    //
                                                    // Always use parentheses to ensure
                                                    // that comparison operations occur
                                                    // before AND/OR.
                                                    //
                                                    // E.g.
                                                    //
                                                    //    IF((%var1%=3) AND (%Var2=4),10,20)
                                                    tokens.add(EXPR,
                                                            val(tokL.mValue).intValue() > 0 ?
                                                                    tokR.mValue : tokR2.mValue,
                                                            tokFunc.getL(), tokRP.getR());
                                                    break;
                                            }
                                        }

                                        if (tokFunc.mType == FUNCT &&
                                                tokLP.mType == LP &&
                                                tokL.mType == EXPR && tokOp.mType == COMMA &&
                                                tokR.mType == EXPR && tokOp2.mType == COMMA &&
                                                tokR2.mType == EXPR &&
                                                tokRP.mType == RP) {
                                            switch (tokFunc.mValue) {
                                                case "mid":
                                                    // ------------------
                                                    // Mid(text, start, length)
                                                    // ------------------
                                                    // Returns <length> characters
                                                    // of <text>, starting at <start>
                                                    tokens.add(EXPR,
                                                            mid(tokL.mValue, cint(tokR.mValue),
                                                                    cint(tokR2.mValue)),
                                                            tokFunc.getL(), tokRP.getR());
                                                    break;
                                                case "replace":
                                                    // ------------------
                                                    // Replace(SourceText, FindText, ReplaceText)
                                                    // ------------------
                                                    // This takes three parameters - the first is
                                                    // the piece of text to be altered, the
                                                    // second is the text to find, and the third
                                                    // is the text to replace the found
                                                    // text with. So for example:
                                                    //
                                                    //    Replace("one two three", "two", "TWO")
                                                    //    would return "one TWO three".
                                                    tokens.add(EXPR,
                                                            tokL.mValue.replace(tokR.mValue, tokR2.mValue),
                                                            tokFunc.getL(), tokRP.getR());
                                                    break;
                                            }
                                        }
                                    }
                                }

                                // ---------------
                                //    6 TOKENS
                                // ---------------
                                // E.g. funct(a,b)
                                tokFunc = tokCur;
                                tokLP = tokCur.getR();
                                tokL = tokLP.getR();
                                tokOp = tokL.getR();
                                tokR = tokOp.getR();
                                tokRP = tokR.getR();

                                if (tokFunc.mType == FUNCT &&
                                        tokLP.mType == LP &&
                                        tokL.mType == EXPR &&
                                        tokOp.mType == COMMA &&
                                        tokR.mType == EXPR &&
                                        tokRP.mType == RP) {
                                    switch (tokFunc.mValue) {
                                        case "max":
                                            // ------------------
                                            //     Max(x,y)
                                            // ------------------
                                            // Returns the maximum of value x and y
                                            tokens.add(EXPR,
                                                    String.valueOf(
                                                            Math.max(cint(val(tokL.mValue)),
                                                                    cint(val(tokR.mValue)))),
                                                    tokFunc.getL(), tokRP.getR());
                                            break;
                                        case "min":
                                            // ------------------
                                            //     Min(x,y)
                                            // ------------------
                                            // Returns the minimum of value x and y
                                            tokens.add(EXPR,
                                                    String.valueOf(
                                                            Math.min(cint(val(tokL.mValue)),
                                                                    cint(val(tokR.mValue)))),
                                                    tokFunc.getL(), tokRP.getR());
                                            break;
                                        case "either":
                                            // ------------------
                                            //    Either(x,y)
                                            // ------------------
                                            // Randomly returns either x or y
                                            // either(text1, text2) randomly returns
                                            // either <text1> or <text2>
                                            if (mAdv.getRand(1) == 1) {
                                                tokens.add(EXPR,
                                                        tokL.mValue,
                                                        tokFunc.getL(), tokRP.getR());
                                            } else {
                                                tokens.add(EXPR,
                                                        tokR.mValue,
                                                        tokFunc.getL(), tokRP.getR());
                                            }
                                            break;
                                        case "rand":
                                            // ------------------
                                            //     Rand(x,y)
                                            // ------------------
                                            // Returns a random value between x and y
                                            tokens.add(EXPR,
                                                    String.valueOf(
                                                            mAdv.getRand(mAdv.safeInt(tokL.mValue),
                                                                    mAdv.safeInt(tokR.mValue))),
                                                    tokFunc.getL(), tokRP.getR());
                                            break;
                                        case "urand":
                                            // ------------------
                                            //    URand(x,y)
                                            // ------------------
                                            // Returns a unique random value between x and y.
                                            // When called multiple times no value will be
                                            // repeated until all of the values between x
                                            // and y have been returned.
                                            tokens.add(EXPR,
                                                    String.valueOf(
                                                            mAdv.getRandNoRepeat(mAdv.safeInt(tokL.mValue),
                                                                    mAdv.safeInt(tokR.mValue))),
                                                    tokFunc.getL(), tokRP.getR());
                                            break;
                                        case "lft":
                                            // ------------------
                                            // Left(text, length)
                                            // ------------------
                                            // Returns the <length> leftmost
                                            // characters of <text>.
                                            tokens.add(EXPR,
                                                    left(tokL.mValue, cint(tokR.mValue)),
                                                    tokFunc.getL(), tokRP.getR());
                                            break;
                                        case "rgt":
                                            // ------------------
                                            // Right(text, length)
                                            // ------------------
                                            // Returns the <length> rightmost
                                            // characters of <text>.
                                            tokens.add(EXPR,
                                                    right(tokL.mValue, cint(tokR.mValue)),
                                                    tokFunc.getL(), tokRP.getR());
                                            break;
                                        case "ist":
                                            // ------------------
                                            // Instr(text, search)
                                            // ------------------
                                            // Returns the position of the first
                                            // occurence of <search> within <text>
                                            // eg. instr(“hello”,”e”) = 2
                                            //
                                            // Note that this is not case sensitive, so
                                            // instr("hello","E") will also return 2.
                                            // This function returns zero if <search> does
                                            // not exist within <text>.
                                            tokens.add(EXPR,
                                                    String.valueOf(
                                                            instr(tokL.mValue, tokR.mValue)),
                                                    tokFunc.getL(), tokRP.getR());
                                            break;
                                    }
                                }

                                if (tokFunc.mType == FUNCT &&
                                        tokLP.mType == LP && tokL.mType == EXPR &&
                                        tokFunc.mValue.equals("oneof")) {
                                    // ------------------------------------
                                    //  OneOf(text1, text2, text3, text4)
                                    // ------------------------------------
                                    // This function can take any number of
                                    // parameters, and will return one of the parameters
                                    // randomly. For example, you could embed the
                                    // function into a piece of text like so:
                                    //
                                    //    The top card is the Queen of <# OneOf("club",
                                    //    "spade", "diamond", "heart") #>s.
                                    int len = 1;
                                    Token tok = tokL;

                                    parseTokens:
                                    while (tok.getR() != null) {
                                        Token tokNext = tok.getR();
                                        switch (tokNext.mType) {
                                            case RP:
                                                // Great, we reached the end of the function.
                                                // Let's evaluate
                                                tokRP = tokNext;
                                                int iOneOfIndex = mAdv.getRand(len - 1) + 1;
                                                Token tokRnd = tokFunc;
                                                for (int i = 1; i <= iOneOfIndex; i++) {
                                                    tokRnd = tokRnd.getR().getR();
                                                }
                                                tokens.add(EXPR,
                                                        tokRnd.mValue,
                                                        tokFunc.getL(), tokRP.getR());
                                                break parseTokens;
                                            case COMMA:
                                                Token tokExpr = tokNext.getR();
                                                if (tokExpr.mType == EXPR) {
                                                    len++;
                                                    tok = tokNext.getR();
                                                } else {
                                                    // Not what we were expecting
                                                    break parseTokens;
                                                }
                                                break;
                                            default:
                                                // Ok, not what we were expecting
                                                break parseTokens;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ---------------
                    //    3 TOKENS
                    // ---------------
                    tokL = tokCur;
                    tokOp = tokCur.getR();
                    tokR = tokOp.getR();

                    // 3 tokens
                    // mathematical operators
                    // expr op expr
                    if (tokL.mType == EXPR &&
                            tokOp.mType == OP &&
                            tokR.mType == EXPR) {
                        switch (tokOp.mValue) {
                            case "^":
                                // ------------------
                                //       x ^ y
                                // ------------------
                                tokens.add(EXPR,
                                        String.valueOf(Math.pow(val(tokL.mValue).intValue(),
                                                val(tokR.mValue).intValue())),
                                        tokL.getL(), tokR.getR());
                                break;
                            case "*":
                                // ------------------
                                //       x * y
                                // ------------------
                                tokens.add(EXPR,
                                        String.valueOf(val(tokL.mValue).intValue() *
                                                val(tokR.mValue).intValue()),
                                        tokL.getL(), tokR.getR());
                                break;
                            case "/":
                                // ------------------
                                //       x / y
                                // ------------------
                                // Notes:
                                //
                                // 1) Adrift divides as follows. We need to do the same:
                                //
                                //    add("expr",
                                //             (Math.Round((val(tokL.Value) / val(tokR.Value)),
                                //             MidpointRounding.AwayFromZero)).ToString, tokL.Left,
                                //             tokR.Right)
                                //
                                //    From the VB website:
                                //
                                //    "A rounding operation takes an original number with an
                                //     implicit or specified precision; examines the next digit,
                                //     which is at that precision plus one; and returns the
                                //     nearest number with the same precision as the original
                                //     number. For positive numbers, if the next digit is from 0
                                //     through 4, the nearest number is toward negative infinity.
                                //     If the next digit is from 6 through 9, the nearest number
                                //     is toward positive infinity. For negative numbers, if the
                                //     next digit is from 0 through 4, the nearest number is toward
                                //     positive infinity. If the next digit is from 6 through 9,
                                //     the nearest number is toward negative infinity.
                                //
                                //     In the previous cases, the MidpointRounding enumeration
                                //     does not affect the result of the rounding operation.
                                //     However, if the next digit is 5, which is the midpoint
                                //     between two possible results, and all remaining digits
                                //     are zero or there are no remaining digits, the nearest
                                //     number is ambiguous. In this case, the MidpointRounding
                                //     enumeration enables you to specify whether the rounding
                                //     operation returns the nearest number away from zero or
                                //     the nearest even number."
                                //
                                // 2) Like Scare, we define x / 0 = 0. Avert your eyes...
                                float y = val(tokR.mValue).floatValue();
                                int z = 0;
                                if (y != 0) {
                                    float x = val(tokL.mValue).floatValue();
                                    float tmp = x / y;
                                    if ((int) (tmp * 10) == 5) {
                                        // round away from 0
                                        z = (tmp > 0) ? (int) (tmp + 1) : (int) (tmp - 1);
                                    } else {
                                        z = Math.round(tmp);
                                    }
                                }
                                tokens.add(EXPR,
                                        String.valueOf(z),
                                        tokL.getL(), tokR.getR());
                                break;
                            case "+":
                                // ------------------
                                //       x + y
                                // ------------------
                                if (isNumeric(tokL.mValue) && isNumeric(tokR.mValue)) {
                                    if (run == 2) {
                                        tokens.add(EXPR,
                                                String.valueOf(val(tokL.mValue).intValue() +
                                                        val(tokR.mValue).intValue()),
                                                tokL.getL(), tokR.getR());
                                    }
                                } else {
                                    tokens.add(EXPR,
                                            tokL.mValue + tokR.mValue,
                                            tokL.getL(), tokR.getR());
                                    run = 2;
                                }
                                break;
                            case "-":
                                // ------------------
                                //       x - y
                                // ------------------
                                if (run == 2) {
                                    tokens.add(EXPR,
                                            String.valueOf(val(tokL.mValue).intValue() -
                                                    val(tokR.mValue).intValue()),
                                            tokL.getL(), tokR.getR());
                                }
                                break;
                            case "mod":
                                // ------------------
                                //      x mod y
                                // ------------------
                                if (run == 2) {
                                    tokens.add(EXPR,
                                            String.valueOf(val(tokL.mValue).intValue() %
                                                    val(tokR.mValue).intValue()),
                                            tokL.getL(), tokR.getR());
                                }
                                break;
                        }
                    }

                    // 3 tokens
                    // && || comparison operators
                    // test LOGIC test
                    if (tokL.mType == TEST &&
                            tokOp.mType == LOGIC &&
                            tokR.mType == TEST) {
                        switch (tokOp.mValue) {
                            case "AND":
                                // ------------------
                                //      x && y
                                // ------------------
                                tokens.add(TEST,
                                        (val(tokL.mValue).intValue() > 0 &&
                                                val(tokR.mValue).intValue() > 0) ? "1" : "0",
                                        tokL.getL(), tokR.getR());
                                break;
                            case "OR":
                                // ------------------
                                //      x || y
                                // ------------------
                                tokens.add(TEST,
                                        (val(tokL.mValue).intValue() > 0 ||
                                                val(tokR.mValue).intValue() > 0) ? "1" : "0",
                                        tokL.getL(), tokR.getR());
                                break;
                        }
                    }

                    // 3 tokens
                    // ==  !=  >  <  >=  <= comparison operators
                    // expr TESTOP expr

                    // Do this on run 3 in case we have expr TESTOP expr OP expr,
                    // to ensure the expr OP expr reduces to expr first
                    if (tokL.mType == EXPR &&
                            tokOp.mType == TESTOP &&
                            tokR.mType == EXPR && run == 3) {
                        switch (tokOp.mValue) {
                            case "EQ":
                                // ------------------
                                //       x == y
                                // ------------------
                                if (isNumeric(tokL.mValue) && isNumeric(tokR.mValue)) {
                                    tokens.add(TEST,
                                            (val(tokL.mValue).intValue() ==
                                                    val(tokR.mValue).intValue()) ? "1" : "0",
                                            tokL.getL(), tokR.getR());
                                } else {
                                    tokens.add(TEST,
                                            (tokL.mValue.equals(tokR.mValue)) ? "1" : "0",
                                            tokL.getL(), tokR.getR());
                                }
                                break;
                            case "NE":
                                // ------------------
                                //       x != y
                                // ------------------
                                if (isNumeric(tokL.mValue) && isNumeric(tokR.mValue)) {
                                    tokens.add(TEST,
                                            (val(tokL.mValue).intValue() !=
                                                    val(tokR.mValue).intValue()) ? "1" : "0",
                                            tokL.getL(), tokR.getR());
                                } else {
                                    tokens.add(TEST,
                                            (!tokL.mValue.equals(tokR.mValue)) ? "1" : "0",
                                            tokL.getL(), tokR.getR());
                                }
                                break;
                            case "GT":
                                // ------------------
                                //       x > y
                                // ------------------
                                tokens.add(TEST,
                                        (val(tokL.mValue).intValue() >
                                                val(tokR.mValue).intValue()) ? "1" : "0",
                                        tokL.getL(), tokR.getR());
                                break;
                            case "LT":
                                // ------------------
                                //       x < y
                                // ------------------
                                tokens.add(TEST,
                                        (val(tokL.mValue).intValue() <
                                                val(tokR.mValue).intValue()) ? "1" : "0",
                                        tokL.getL(), tokR.getR());
                                break;
                            case "GE":
                                // ------------------
                                //      x >= y
                                // ------------------
                                tokens.add(TEST,
                                        (val(tokL.mValue).intValue() >=
                                                val(tokR.mValue).intValue()) ? "1" : "0",
                                        tokL.getL(), tokR.getR());
                                break;
                            case "LE":
                                // ------------------
                                //      x <= y
                                // ------------------
                                tokens.add(TEST,
                                        (val(tokL.mValue).intValue() <=
                                                val(tokR.mValue).intValue()) ? "1" : "0",
                                        tokL.getL(), tokR.getR());
                                break;
                        }
                    }

                    // 3 tokens

                    // Concatenate text, e.g. "one" & "two"
                    // Need to be careful that it evaluates late so we
                    // don't get: expr = expr AND expr = expr  reduced to expr = expr = expr
                    // Logic should be the last thing to resolve
                    // Should we somehow check that each expression is a string?
                    if (tokL.mType == EXPR &&
                            tokOp.mValue.equals("AND") &&
                            tokR.mType == EXPR && run == 3) {
                        // ------------------
                        //       x & y
                        // ------------------
                        tokens.add(EXPR, tokL.mValue + tokR.mValue,
                                tokL.getL(), tokR.getR());
                    }

                    if (tokL.mType == LP &&
                            tokOp.mType == EXPR &&
                            tokR.mType == RP) {
                        // ------------------
                        //       ( x )
                        // ------------------
                        tokens.add(EXPR,
                                isNumeric(tokOp.mValue) ?
                                        String.valueOf(val(tokOp.mValue)) : tokOp.mValue,
                                tokL.getL(), tokR.getR());
                    }

                    if (tokL.mType == LP &&
                            tokOp.mType == TEST &&
                            tokR.mType == RP) {
                        // ------------------
                        //      ( test )
                        // ------------------
                        tokens.add(TEST,
                                isNumeric(tokOp.mValue) ?
                                        String.valueOf(val(tokOp.mValue)) : tokOp.mValue,
                                tokL.getL(), tokR.getR());
                    }
                }

                // ---------------
                //    2 TOKENS
                // ---------------
                tokOp = tokCur;
                tokR = tokOp.getR();

                // 2 tokens
                // op expr
                // e.g. +1, -1
                if (tokOp.mType == OP && tokR.mType == EXPR) {
                    switch (tokOp.mValue) {
                        case "-":
                            // ------------------
                            //        -x
                            // ------------------
                            if (run == 2) {
                                tokens.add(EXPR,
                                        String.valueOf(-val(tokR.mValue).intValue()),
                                        tokOp.getL(), tokR.getR());
                            }
                            break;
                        case "+":
                            // ------------------
                            //        +x
                            // ------------------
                            if (run == 2) {
                                tokens.add(EXPR,
                                        String.valueOf(val(tokR.mValue).intValue()),
                                        tokOp.getL(), tokR.getR());
                            }
                            break;
                    }
                }

                // 2 tokens
                // negate test
                // e.g. !0, !1
                //
                // Original ADRIFT 5 code tokenised single !s but did
                // not have this associated case in the parser - seems
                // to have been a bug:
                if (tokOp.mType == NEGATE && tokR.mType == TEST) {
                    // ------------------
                    //        !x
                    // ------------------
                    tokens.add(TEST,
                            (val(tokR.mValue).intValue() > 0) ? "0" : "1",
                            tokOp.getL(), tokR.getR());
                }
            }

            // --------------------------------------
            //    UPDATE THE CURRENT TOKEN POINTER
            // --------------------------------------
            if (tokens.mChanged) {
                // Move pointer to far left
                tokCur = tokens.getLast();
                int bombOut = 0;
                while (tokCur.getL() != null) {
                    tokCur = tokCur.getL();
                    bombOut++;
                    if (bombOut == 5000) {
                        badExpr = false;
                        continue nxt;
                    }
                }
                badExpr = false;
                run = 1;
            } else {
                if (tokCur.getR() == null) {
                    // We can't advance to the right
                    if (badExpr) {
                        // Bad expression flag has
                        // already been set. If this
                        // is first or second pass,
                        // move pointer to far left
                        // and try to parse again.
                        // Otherwise give up.
                        if (run == 1) {
                            run = 2;
                            tokCur = tokens.getLast();
                            int bombOut = 0;
                            while (tokCur.getL() != null) {
                                tokCur = tokCur.getL();
                                bombOut++;
                                if (bombOut == 5000) {
                                    badExpr = false;
                                    continue nxt;
                                }
                            }
                            badExpr = false;
                            continue;
                        } else if (run == 2) {
                            run = 3;
                            tokCur = tokens.getLast();
                            int bombOut = 0;
                            while (tokCur.getL() != null) {
                                tokCur = tokCur.getL();
                                bombOut++;
                                if (bombOut == 5000) {
                                    badExpr = false;
                                    continue nxt;
                                }
                            }
                            badExpr = false;
                            continue;
                        } else {
                            return null;
                        }
                    }
                    badExpr = true;
                } else {
                    // Move token one to right
                    tokCur = tokCur.getR();
                }
            }
        }

        return tokens.getLast().mValue;
    }

    void set(@NonNull OpType op, @Nullable String arg,
             @NonNull String expr, int loopStart, @Nullable MTask task) {
        if (op == ASSIGNMENT) {
            int i = 1;
            if (getLength() > 1 && arg != null) {
                if (arg.startsWith("ReferencedNumber")) {
                    i = mAdv.safeInt(mAdv.mReferences.getMatchingKey(arg));
                } else if (isNumeric(arg)) {
                    i = (int) val(arg);
                } else {
                    i = mAdv.mVariables.get(arg).getInt();
                }
            }
            if (!getKey().equals("Score") || (task != null && !task.mIsScored)) {
                setToExpr(expr, i, mAdv.mReferences);
                if (getKey().equals("Score")) {
                    task.mIsScored = true;
                    mAdv.setScore(getInt());
                }
            }
        } else {
            for (int i = loopStart; i <= cint(arg); i++) {
                setToExpr(expr, i, mAdv.mReferences);
            }
        }
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String value) {
        mName = value;
    }

    public int getLength() {
        return mLength;
    }

    /**
     * (Re-)size the length of this array
     * variable. Any existing values are
     * wiped.
     *
     * @param length - the new length.
     */
    public void setLength(int length) {
        if (getType() == Numeric) {
            mIntVals = new int[length];
            mStrVals = new String[1];
            mStrVals[0] = "";
        } else {
            mStrVals = new String[length];
            mIntVals = null;
        }
        mLength = length;
    }

    /**
     * Convenience function to get the integer value in
     * the first index of this array variable.
     *
     * @return the value, or 0 if there was an error.
     */
    public int getInt() {
        return getIntAt(1);
    }

    /**
     * Convenience function to set the first index of
     * this array variable to a given integer value.
     *
     * @param value - the value.
     */
    public void set(int value) {
        setAt(1, value);
    }

    /**
     * Get the integer stored in a given index of this
     * array variable.
     * <p>
     * N.B. ADRIFT array variables are 1-based.
     *
     * @param index - the index
     * @return the integer or 0 if there was an error.
     */
    public int getIntAt(int index) {
        if (index > 0 && mIntVals != null &&
                index < mIntVals.length + 1) {
            return mIntVals[index - 1];
        } else {
            if (mIntVals != null) {
                mAdv.mView.errMsg("Attempting to read index " + index +
                        " outside bounds of array of variable " +
                        getName() + (mIntVals.length > 1 ? "(" + mIntVals.length + ")" : ""));
            } else {
                mAdv.mView.errMsg("Attempting to read index " + index +
                        " outside bounds of array of variable (0)");
            }
            return 0;
        }
    }

    /**
     * Set the integer stored in a given index of this
     * array variable.
     * <p>
     * N.B. ADRIFT array variables are 1-based.
     *
     * @param index - the index
     * @param value - the value to set it to.
     */
    public void setAt(int index, int value) {
        if (index > 0 && mIntVals != null &&
                index < mIntVals.length + 1) {
            mIntVals[index - 1] = value;
        } else {
            if (mIntVals != null) {
                mAdv.mView.errMsg("Attempting to set index " + index +
                        " outside bounds of array of variable " +
                        getName() + (mIntVals.length > 1 ? "(" + mIntVals.length + ")" : ""));
            } else {
                mAdv.mView.errMsg("Attempting to set index " + index +
                        " outside bounds of array of variable (0)");
            }
        }
    }

    void set(@NonNull String value) {
        setAt(1, value);
    }

    @NonNull
    public String getStr() {
        return getStrAt(1);
    }

    @NonNull
    public String getStrAt(int index) {
        if (index > 0 && mStrVals != null &&
                index < mStrVals.length + 1) {
            return mStrVals[index - 1];
        } else {
            if (mStrVals != null) {
                mAdv.mView.errMsg("Attempting to read index " + index +
                        " outside bounds of array of variable " + getName() +
                        (mStrVals.length > 1 ? "(" + mStrVals.length + ")" : ""));
            } else {
                mAdv.mView.errMsg("Attempting to read index " + index +
                        " outside bounds of array of variable (0)");
            }
            return "";
        }
    }

    public VariableType getType() {
        return mType;
    }

    public void setType(VariableType value) {
        mType = value;
        switch (value) {
            case Numeric:
                mIntVals = new int[1];
                mStrVals = new String[1];
                mStrVals[0] = "";
                break;
            case Text:
                mStrVals = new String[1];
                mIntVals = null;
                break;
        }
    }

    public void setAt(int index, @NonNull String value) {
        if (index > 0 && mStrVals != null && index < mStrVals.length + 1) {
            mStrVals[index - 1] = value;
        } else if (mStrVals == null) {
            mAdv.mView.errMsg("Attempting to set index " + index +
                    " outside bounds of array of variable (0)");
        } else if (index <= 0) {
            mAdv.mView.errMsg("Attempting to set index " + index +
                    " outside bounds of array of variable " + getName() +
                    (mStrVals.length > 1 ? "(" + mStrVals.length + ")" : "") +
                    ".  ADRIFT arrays start at 1, not 0.");
        } else {
            mAdv.mView.errMsg("Attempting to set index " + index +
                    " outside bounds of array of variable " + getName() +
                    (mStrVals.length > 1 ? "(" + mStrVals.length + ")" : ""));
        }
    }

    public void setToExpr(@NonNull String expr,
                          @Nullable MReferenceList refs) {
        setToExpr(expr, 1, refs);
    }

    /**
     * Expressions are used to calculate integer values or produce strings of text
     * that can then be stored in Variables or Properties using Actions, or used in
     * comparison operations in Restrictions.
     * <p>
     * An expression can also be embedded into the middle of text in a text box by
     * placing it between the symbols <# and #>
     * <p>
     * When the text is displayed on the screen the expression will be evaluated
     * and the result inserted into the text.
     * <p>
     * INTEGER EXPRESSIONS:
     * <p>
     * In an action a value can be calculated by an integer expression and the
     * result stored in a 'number' variable, an element of an array variable,
     * or an integer property of a character, object or location. The basic
     * mathematical functions +,-,*,/ can be used to calculate a value, using
     * integer constants, properties, variables, and the reference %number%.
     * <p>
     * An integer expression can also be compared with an integer variable
     * or property in a restriction, to determine if they are equal or one is
     * greater than or less than the other value.
     * <p>
     * Parentheses can be used to control the order of evaluation, eg. (17+4)*(9-6)
     * <p>
     * Variable names must be delimited with '%' symbols, eg. %Score%
     * <p>
     * Array variables use square brackets to enclose the index value, eg. %ArrayVar1[3]%
     * <p>
     * STRING EXPRESSIONS:
     * <p>
     * In an action a line of text can be calculated using a string expression
     * and the result stored in a 'text' variable, an element of an array
     * variable, or a text property of a character, object or location. A
     * string expression can also be compared with a text variable or property
     * in a restriction, to determine if they contain exactly the same text or not.
     * <p>
     * String constants are delimited with the quote character, eg. "Some text".
     * The '&' operator concatenates two strings together, eg. "Some text"&"ures"
     * becomes "Some textures". Any text based property, variable, element of
     * array variable, or the reference %text% can be used in a string expression.
     * <p>
     * MIXED EXPRESSIONS:
     * <p>
     * An expression can be partly an integer expression and partly a string
     * expression. For example an integer expression can be used to calculate
     * the index of a text variable, and the result of that used in a string
     * expression.
     *  @param expr                    - the expression to evaluate
     * @param index                    - the index to store the expression in, for array variables (N.B. 1-based arrays)
     */
    private void setToExpr(@NonNull String expr,
                           int index, @Nullable MReferenceList refs) {
        try {
            // ------------------
            //     TOKENISE
            // ------------------
            expr = expr.replace("%Loop%", String.valueOf(index));
            expr = mAdv.evalFuncs(expr, refs, true);
            expr = expr.replace("\\\"", "~@+#~");
            StringBuilder exprNoSpaces = stripRedundantSpaces(expr);
            TokenList tokens = tokenise(exprNoSpaces, index, refs);
            if (tokens == null) {
                // No tokens found.
                return;
            }

            // ------------------
            //       PARSE
            // ------------------
            String val = parse(tokens);
            if (val == null) {
                // Parse error.
                mAdv.mView.errMsg("Bad expression: " + expr);
                return;
            }

            // ------------------
            //    STORE RESULT
            // ------------------
            if (getType() == Numeric) {
                setAt(index, mAdv.safeInt(val));
            } else {
                setAt(index, val);
            }

        } catch (Exception ex) {
            // Ignore
        }
    }

    @NonNull
    @Override
    public ArrayList<MDescription> getAllDescriptions() {
        return new ArrayList<>();
    }

    @Nullable
    @Override
    public MVariable clone() {
        return (MVariable) super.clone();
    }

    @NonNull
    @Override
    public String getCommonName() {
        return getName();
    }

    @Override
    public boolean deleteKey(@NonNull String key) {
        for (MDescription d : getAllDescriptions()) {
            if (!d.deleteKey(key)) {
                return false;
            }
        }
        MVariable var = mAdv.mVariables.get(key);
        if (var != null &&
                getStr().contains("%" + var.getName() + "%")) {
            if (var.getType() == Numeric) {
                set(getStr().replace("%" +
                        var.getName() + "%", "0"));
            } else {
                set(getStr().replace("%" +
                        var.getName() + "%", "\"\""));
            }
        }
        return true;
    }

    @Override
    protected int findLocal(@NonNull String toFind, @Nullable String toReplace,
                            boolean findAll, @NonNull int[] nReplaced) {
        int initCount = nReplaced[0];
        String[] t = new String[1];
        t[0] = mName;
        nReplaced[0] += MGlobals.find(t, toFind, toReplace);
        mName = t[0];
        return nReplaced[0] - initCount;
    }

    @Override
    public int getKeyRefCount(@NonNull String key) {
        int ret = 0;
        for (MDescription d : getAllDescriptions()) {
            ret += d.getNumberOfKeyRefs(key);
        }
        MVariable var = mAdv.mVariables.get(key);
        if (var != null) {
            if (getStr().contains("%" + var.getName() + "%")) {
                ret++;
            }
        }
        return ret;
    }

    @NonNull
    @Override
    public String getSymbol() {
        // chart with upwards trend
        return new String(Character.toChars(0x1F4C8));
    }

    /**
     * Note: these enum values are
     * hard-coded into ADRIFT game
     * files as text - do not
     * change!
     */
    public enum VariableType {
        Numeric,        // 0
        Text            // 1
    }

    enum TokenType {
        UNINITIALISED,
        EXPR,
        OP,
        LP,
        RP,
        COMMA,
        FUNCT,
        TEST,
        TESTOP,
        LOGIC,
        NEGATE
    }

    public enum OpType {
        ASSIGNMENT,     // 0
        LOOP            // 1
    }

    private static class TokenList {
        final ArrayList<Token> mTokens = new ArrayList<>();
        boolean mChanged = false;

        void printTokens() {
            printTokens(null);
        }

        void printTokens(@Nullable Token tokCur) {
            Token index = getFirst();
            while (index != null) {
                index = printToken(index, tokCur);
            }
        }

        @Nullable
        Token printToken(@NonNull Token index,
                         @Nullable Token tokCur) {
            if (tokCur != null) {
                boolean isCurrent =
                        (index.getL() == tokCur.getL() &&
                                index.getR() == tokCur.getR() &&
                                index.mType == tokCur.mType);
                GLKLogger.debug("[" + (isCurrent ? "#" : "") + index +
                        (isCurrent ? "#" : "") + ": " +
                        index.mType + "/" + index.mValue + "] ");
                return index.getR();
            }
            return null;
        }

        @Nullable
        Token getFirst() {
            for (int i = mTokens.size() - 1; i >= 0; i--) {
                Token tok = mTokens.get(i);
                if (tok.getL() == null) {
                    return tok;
                }
            }
            return null;
        }

        @Nullable
        Token getLast() {
            return mTokens.get(mTokens.size() - 1);
        }

        void add(TokenType type, @NonNull String value,
                 @Nullable Token left, @Nullable Token right) {
            Token tok = new Token();
            tok.mType = type;
            tok.mValue = value;
            tok.mLeft = left;
            tok.mRight = right;
            mTokens.add(tok);
            if (left != null) {
                left.setR(mTokens.get(mTokens.size() - 1));
            }
            if (right != null) {
                right.setL(mTokens.get(mTokens.size() - 1));
            }
            mChanged = true;
        }
    }

    private static class Token {
        @NonNull
        String mValue = "0";
        @Nullable
        private Token mLeft = null;
        @Nullable
        private Token mRight = null;
        TokenType mType = TokenType.UNINITIALISED;

        @Nullable
        Token getL() {
            return mLeft;
        }

        void setL(@Nullable Token tok) {
            mLeft = tok;
        }

        @Nullable
        Token getR() {
            return mRight;
        }

        void setR(@Nullable Token tok) {
            mRight = tok;
        }
    }

    public static class MVariableState {
        public String mKey;
        @Nullable
        public String[] mValue;
        @NonNull
        public final HashMap<String, Boolean> mDisplayedDescriptions = new HashMap<>();

        MVariableState(@NonNull MVariable var) {
            mKey = var.getKey();
            mValue = new String[var.getLength()];
            for (int i = 0; i < var.getLength(); i++) {
                if (var.getType() == Numeric) {
                    mValue[i] = String.valueOf(var.getIntAt(i + 1));
                } else {
                    mValue[i] = var.getStrAt(i + 1);
                }
            }
            saveDisplayOnce(var.getAllDescriptions(), mDisplayedDescriptions);
        }

        MVariableState(@NonNull MAdventure adv, @NonNull XmlPullParser xpp) throws Exception {
            xpp.require(START_TAG, null, "Variable");

            int depth = xpp.getDepth();
            int evType;
            int nValues = 0;

            while ((evType = xpp.nextTag()) != END_DOCUMENT && xpp.getDepth() > depth) {
                if (evType == START_TAG) {
                    String s = xpp.getName();
                    if (s.startsWith("Value_") && s.length() > 6) {
                        String indS = s.substring(6);
                        try {
                            int indI = Integer.valueOf(indS);
                            if (indI < nValues) {
                                mValue[indI] = xpp.nextText();
                            }
                        } catch (IllegalArgumentException e) {
                            // ignore and continue
                        }
                    } else {
                        switch (s) {
                            case "Key": {
                                mKey = xpp.nextText();
                                MVariable var = adv.mVariables.get(mKey);
                                if (var == null) {
                                    throw new Exception("Variable " + mKey + " doesn't exist!");
                                }
                                nValues = var.getLength();
                                mValue = new String[nValues];
                                for (int i = 0; i < nValues; i++) {
                                    if (var.getType() == Numeric) {
                                        mValue[i] = "0";
                                    } else {
                                        mValue[i] = "";
                                    }
                                }
                                break;
                            }
                            case "Value": {
                                // Old style
                                mValue[0] = xpp.nextText();
                                break;
                            }
                            case "Displayed": {
                                mDisplayedDescriptions.put(xpp.nextText(), true);
                                break;
                            }

                        }
                    }
                }
            }

            xpp.require(END_TAG, null, "Variable");
        }

        public void serialize(@NonNull MAdventure adv, @NonNull XmlSerializer xs) throws IOException {
            xs.startTag(null, "Variable");

            xs.startTag(null, "Key");
            xs.text(mKey);
            xs.endTag(null, "Key");

            MVariable var = adv.mVariables.get(mKey);
            for (int i = 0; i < mValue.length; i++) {
                if (var.getType() == Numeric) {
                    if (!mValue[i].equals("0")) {
                        xs.startTag(null, "Value_" + i);
                        xs.text(mValue[i]);
                        xs.endTag(null, "Value_" + i);
                    }
                } else {
                    if (!mValue[i].equals("")) {
                        xs.startTag(null, "Value_" + i);
                        xs.text(mValue[i]);
                        xs.endTag(null, "Value_" + i);
                    }
                }
            }

            for (String descKey : mDisplayedDescriptions.keySet()) {
                xs.startTag(null, "Displayed");
                xs.text(descKey);
                xs.endTag(null, "Displayed");
            }

            xs.endTag(null, "Variable");
        }

        public void restore(@NonNull MVariable var) {
            for (int i = 0; i < var.getLength(); i++) {
                if (var.getType() == Numeric) {
                    var.setAt(i + 1, var.mAdv.safeInt(mValue[i]));
                } else {
                    var.setAt(i + 1, mValue[i]);
                }
            }
            restoreDisplayOnce(var.getAllDescriptions(), mDisplayedDescriptions);
        }
    }
}
