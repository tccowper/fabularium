/*
 * Copyright (C) 2018 Tim Cadogan-Cowper.
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

package com.luxlunae.glk.view.keyboard;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GLKKeyboardMapping {
    private static final String[] keyConstants =
            {"return", "next", "del",
                    "left", "right", "up", "down",
                    "esc", "tab", "pgup", "pgdown",
                    "home", "end",
                    "f1", "f2", "f3", "f4",
                    "f5", "f6", "f7", "f8",
                    "f9", "f10", "f11", "f12",
                    "dbg"};
    private static final int[] keyConstantCodes =
            {GLKConstants.keycode_Return, GLKConstants.FAB_KEY_NEXT, GLKConstants.keycode_Delete,
                    8592, GLKConstants.keycode_Right, GLKConstants.keycode_Up, GLKConstants.keycode_Down,
                    GLKConstants.keycode_Escape, GLKConstants.keycode_Tab, GLKConstants.keycode_PageUp, GLKConstants.keycode_PageDown,
                    GLKConstants.keycode_Home, GLKConstants.keycode_End,
                    GLKConstants.keycode_Func1, GLKConstants.keycode_Func2, GLKConstants.keycode_Func3, GLKConstants.keycode_Func4,
                    GLKConstants.keycode_Func5, GLKConstants.keycode_Func6, GLKConstants.keycode_Func7, GLKConstants.keycode_Func8,
                    GLKConstants.keycode_Func9, GLKConstants.keycode_Func10, GLKConstants.keycode_Func11, GLKConstants.keycode_Func12,
                    GLKConstants.FAB_KEY_DEBUG};

    int mXmlLayoutResId;
    @Nullable
    CharSequence[] mKeyLabels;
    @Nullable
    int[][] mKeyCodes;
    CharSequence[] mHintLabels;
    @Nullable
    int[][] mHintCodes;
    Boolean[] mRepeatable;  // null means leave unchanged
    Integer[] mKeyLabelSize;
    Integer[] mHintLabelSize;

    private static void addKeyConstant(@NonNull String field,
                                       @NonNull ArrayList<Integer> arrInts) {
        // if field is a recognised constant (+ an optional number), adds that
        // constant to arrInts (x optional number). If unrecognised, arrInts is
        // unmodified.
        field = field.toLowerCase();

        int ch = 0;
        int len = 0;
        for (int i = 0, sz = keyConstants.length; i < sz; i++) {
            if (field.startsWith(keyConstants[i])) {
                ch = keyConstantCodes[i];
                len = keyConstants[i].length();
                break;
            }
        }

        if (ch != 0) {
            if (field.length() > len) {
                try {
                    int r = Integer.parseInt(field.substring(len));
                    if (r > 0) {
                        for (int i = 0; i < r; i++) {
                            arrInts.add(ch);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // syntax error - just ignore it
                }
            } else {
                arrInts.add(ch);
            }
        }
    }

    private static void addKeyBoolean(@Nullable String field,
                                      @NonNull ArrayList<Boolean> arrBooleans) {
        // if field is "*", it means true
        // anything else means false
        if (field == null) {
            arrBooleans.add(null);
            return;
        }
        arrBooleans.add(field.equals("*"));
    }

    private static void addKeyInteger(@Nullable String field,
                                      @NonNull ArrayList<Integer> arrInts,
                                      int lowerBound, int upperBound) {
        // if field is an integer x, s.t. lowerBound <= x <= upperBound
        // we add it to the array arrInts. Otherwise we add a null.
        if (field == null) {
            arrInts.add(null);
            return;
        }

        try {
            int val = Integer.parseInt(field);
            arrInts.add((val >= lowerBound && val <= upperBound) ? val : null);
        } catch (IllegalArgumentException e) {
            arrInts.add(null);
        }
    }

    private static void addKeyField(@Nullable String field,
                                    @Nullable ArrayList<String> arrString,
                                    @Nullable ArrayList<int[]> arrInts,
                                    final @NonNull Pattern regexUnquotedComma) {
        // If arrString is not null, the elements of the field are added to arrString as
        // a String. Otherwise if arrInts is not null, the elements are added to arrInts as
        // a sequence of integers, each integer representing either a Unicode hexadecimal
        // value, or an internal Fabularium constant. Otherwise the function does nothing.
        //GLKLogger.error("got field: " + field);
        if (arrString == null && arrInts == null) {
            return;
        }

        boolean addAsString = (arrString != null);

        if (field == null || field.length() == 0) {
            if (addAsString) {
                arrString.add(null);
            } else {
                arrInts.add(null);
            }
            return;
        }

        String[] elements = splitOnUnquotedCharacter(field, regexUnquotedComma);
        StringBuilder sbRet = new StringBuilder();
        ArrayList<Integer> intRet = new ArrayList<>();
        for (String element : elements) {
            element = element.trim();
            if (element.startsWith("\"")) {
                // string
                int sz = element.length();
                if (element.endsWith("\"")) {
                    if (sz > 2) {
                        element = element.substring(1, sz - 1);
                        element = element.replaceAll("/n", "\n"); // replace newline escapes
                        if (addAsString) {
                            sbRet.append(element);
                        } else {
                            for (int k = 0; k < element.length(); k++) {
                                intRet.add(element.codePointAt(k));
                            }
                        }
                    } else {
                        // user has put "", which means blank
                        // use a UNICODE ZERO-WIDTH NO-BREAK SPACE
                        // which should always be invisible
                        // note: we MUST add a value as Android's Keyboard class
                        // will crash if a key has no value and it is then pressed.
                        if (addAsString) {
                            sbRet.appendCodePoint(0xFEFF);
                        } else {
                            intRet.add(0xFEFF);
                        }
                    }
                }
            } else {
                // should be a hexadecimal Unicode value
                int ch;
                try {
                    ch = Integer.parseInt(element, 16);
                    if (addAsString) {
                        sbRet.appendCodePoint(ch);
                    } else {
                        intRet.add(ch);
                    }
                } catch (NumberFormatException e) {
                    if (!addAsString) {
                        // maybe this is a constant - check
                        addKeyConstant(element, intRet);
                    }
                }
            }
        }

        if (addAsString) {
            String ret = sbRet.toString();
            arrString.add(ret);
        } else {
            if (intRet.size() > 0) {
                int[] ret = GLKUtils.getPrimitiveIntArray(intRet);
                arrInts.add(ret);
            } else {
                arrInts.add(null);
            }
        }
    }

    private static void parseKeyMapping(@NonNull String line,
                                        @NonNull ArrayList<String> keyLabels, @NonNull ArrayList<int[]> keyCodes,
                                        @NonNull ArrayList<String> hintLabels, @NonNull ArrayList<int[]> hintCodes,
                                        @NonNull ArrayList<Boolean> repeatable,
                                        @NonNull ArrayList<Integer> keyLabelSize,
                                        @NonNull ArrayList<Integer> hintLabelSize,
                                        final @NonNull Pattern regexUnquotedPipe,
                                        final @NonNull Pattern regexUnquotedComma) {
        if (line.equals("|")) {
            // increment key number but don't add it
            keyLabels.add(null);
            keyCodes.add(null);
            hintLabels.add(null);
            hintCodes.add(null);
            repeatable.add(null);
            keyLabelSize.add(null);
            hintLabelSize.add(null);
        } else {
            // add this key to current layout
            // split on pipes (|) except when they appear between double quotes
            String[] fields = splitOnUnquotedCharacter(line, regexUnquotedPipe);
            int sz = fields.length;
            addKeyField(sz > 0 ? fields[0].trim() : null, keyLabels, null, regexUnquotedComma);
            addKeyField(sz > 1 ? fields[1].trim() : null, null, keyCodes, regexUnquotedComma);
            addKeyField(sz > 2 ? fields[2].trim() : null, hintLabels, null, regexUnquotedComma);
            addKeyField(sz > 3 ? fields[3].trim() : null, null, hintCodes, regexUnquotedComma);
            addKeyBoolean(sz > 4 ? fields[4].trim() : null, repeatable);
            addKeyInteger(sz > 5 ? fields[5].trim() : null, keyLabelSize, 1, 100);
            addKeyInteger(sz > 6 ? fields[6].trim() : null, hintLabelSize, 1, 100);
        }
    }

    /**
     * Split a string wherever a specific character occurs, except when that character
     * occurs within double quotes.
     *
     * @param s                 - the string to split.
     * @param regexUnquotedChar - the compiled regex to apply to create the split. This is passed
     *                          as an argument so that it does not need to be recompiled every time
     *                          this function is called. It should be of the form
     *                          "\"[^\"]*\" | (split_character)", where split_character
     *                          is the desired delimiter.
     * @return an array containing the split elements of the string.
     */
    @NonNull
    private static String[] splitOnUnquotedCharacter(final @NonNull String s, final @NonNull Pattern regexUnquotedChar) {
        // Thanks
        //   https://stackoverflow.com/questions/19771272/java-regex-find-except-when-between-quotes
        //   https://stackoverflow.com/questions/1433115/what-is-the-proper-way-of-inserting-a-pipe-into-a-java-pattern-expression
        // We use the blank non-breaking Unicode character to create the replacement delimiters (let's just assume
        // nobody uses that as a code - because why would you?)
        Matcher m = regexUnquotedChar.matcher(s);
        StringBuffer b = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(b, m.group(1) != null ? "\uFEFF" : m.group(0));
        }
        m.appendTail(b);
        String replaced = b.toString();
        return replaced.split(Pattern.quote("\uFEFF"));
    }

    @Nullable
    public static GLKKeyboardMapping[] loadFromFile(@NonNull String mappingName, @NonNull File keyboardFile) {
        // returns NULL if: (1) the file cannot be found; (2) the mappingName cannot be found in
        // the file; or (3) the mapping is in the file but there is a syntax error.
        ArrayList<GLKKeyboardMapping> mappings = new ArrayList<>();
        GLKKeyboardMapping curMapping = null;

        try {
            BufferedReader br = new BufferedReader(new FileReader(keyboardFile));
            String line;
            int pos;
            boolean inMapping = false;
            String nameToMatch = mappingName.toLowerCase().trim();
            ArrayList<String> keyLabels = new ArrayList<>();
            ArrayList<int[]> keyCodes = new ArrayList<>();
            ArrayList<String> hintLabels = new ArrayList<>();
            ArrayList<int[]> hintCodes = new ArrayList<>();
            ArrayList<Boolean> repeatable = new ArrayList<>();
            ArrayList<Integer> keyLabelSize = new ArrayList<>();
            ArrayList<Integer> hintLabelSize = new ArrayList<>();
            final Pattern regexUnquotedPipe = Pattern.compile("\"[^\"]*\"|(" + Pattern.quote("|") + ")");
            final Pattern regexUnquotedComma = Pattern.compile("\"[^\"]*\"|(" + Pattern.quote(",") + ")");

            while ((line = br.readLine()) != null) {
                if (line.length() > 0) {
                    // remove any comment (starts with #)
                    pos = line.indexOf('#');
                    if (pos == 0) {
                        // whole line is a comment - skip
                        continue;
                    } else if (pos >= 0) {
                        line = line.substring(0, pos - 1);
                    }
                    line = line.trim();

                    if (line.startsWith("[") && line.endsWith("]")) {
                        // selector - check if it matches the mappingName
                        if (inMapping) {
                            // we've just moved out of the mapping, so we're done
                            break;
                        } else if (line.length() > 2) {
                            String name = line.substring(1, line.length() - 1).trim().toLowerCase();
                            if (name.equals(nameToMatch)) {
                                GLKLogger.debug("Loading keyboard mapping for '" + nameToMatch + "'...");
                                inMapping = true;
                            }
                        }
                    } else if (inMapping) {
                        if (line.startsWith("<") && line.endsWith(">")) {
                            // sub-section - currently only < Layout >
                            String name = line.substring(1, line.length() - 1).trim().toLowerCase();
                            if (name.startsWith("layout")) {
                                if (curMapping != null) {
                                    int sz = keyLabels.size();
                                    if (sz > 0) {
                                        // assume all of the arrays have the same length
                                        curMapping.mKeyLabels = keyLabels.toArray(new String[sz]);
                                        curMapping.mHintLabels = hintLabels.toArray(new String[sz]);
                                        curMapping.mKeyCodes = keyCodes.toArray(new int[sz][]);
                                        curMapping.mHintCodes = hintCodes.toArray(new int[sz][]);
                                        curMapping.mRepeatable = repeatable.toArray(new Boolean[sz]);
                                        curMapping.mKeyLabelSize = keyLabelSize.toArray(new Integer[sz]);
                                        curMapping.mHintLabelSize = hintLabelSize.toArray(new Integer[sz]);
                                        keyLabels.clear();
                                        hintLabels.clear();
                                        keyCodes.clear();
                                        hintCodes.clear();
                                        repeatable.clear();
                                        keyLabelSize.clear();
                                        hintLabelSize.clear();
                                    }
                                }
                                curMapping = new GLKKeyboardMapping();
                                mappings.add(curMapping);

                                // set the layout to use
                                // defaults to 10x4 if not specified or specified but invalid
                                curMapping.mXmlLayoutResId = R.xml.keyboard_10x4;
                                String[] args = name.split("\\s+");
                                if (args.length > 1) {
                                    String layoutName = args[1].trim();
                                    switch (layoutName) {
                                        case "11x4":
                                            curMapping.mXmlLayoutResId = R.xml.keyboard_11x4;
                                            break;
                                        case "10x4":
                                        default:
                                            // don't need to do anything
                                            break;
                                    }
                                }
                                GLKLogger.debug("    Reading layout " + mappings.size());
                            } else {
                                curMapping = null;
                            }
                        } else if (!line.equals("")) {
                            if (curMapping != null) {
                                parseKeyMapping(line, keyLabels, keyCodes, hintLabels,
                                        hintCodes, repeatable, keyLabelSize, hintLabelSize,
                                        regexUnquotedPipe, regexUnquotedComma);
                            }
                        }
                    }
                }
            }

            if (curMapping != null) {
                int sz = keyLabels.size();
                if (sz > 0) {
                    // assume all of the arrays have the same length
                    curMapping.mKeyLabels = keyLabels.toArray(new String[sz]);
                    curMapping.mHintLabels = hintLabels.toArray(new String[sz]);
                    curMapping.mKeyCodes = keyCodes.toArray(new int[sz][]);
                    curMapping.mHintCodes = hintCodes.toArray(new int[sz][]);
                    curMapping.mRepeatable = repeatable.toArray(new Boolean[sz]);
                    curMapping.mKeyLabelSize = keyLabelSize.toArray(new Integer[sz]);
                    curMapping.mHintLabelSize = hintLabelSize.toArray(new Integer[sz]);
                    keyLabels.clear();
                    hintLabels.clear();
                    keyCodes.clear();
                    hintCodes.clear();
                    repeatable.clear();
                    keyLabelSize.clear();
                    hintLabelSize.clear();
                }
            }
        } catch (@NonNull IOException | SecurityException e) {
            return null;
        }

        int sz = mappings.size();
        return (sz > 0) ? mappings.toArray(new GLKKeyboardMapping[sz]) : null;
    }
}
