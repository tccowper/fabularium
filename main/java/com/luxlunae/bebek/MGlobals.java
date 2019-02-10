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

package com.luxlunae.bebek;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.code.regexp.Matcher;
import com.google.code.regexp.Pattern;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MVariable;
import com.luxlunae.bebek.model.collection.MReferenceList;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.glk.GLKLogger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import static com.luxlunae.bebek.MGlobals.MPerspectiveEnum.FirstPerson;
import static com.luxlunae.bebek.MGlobals.MPerspectiveEnum.None;
import static com.luxlunae.bebek.MGlobals.MPerspectiveEnum.SecondPerson;
import static com.luxlunae.bebek.MGlobals.MPerspectiveEnum.ThirdPerson;
import static com.luxlunae.bebek.VB.cbool;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.Down;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.East;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.In;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.North;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.NorthEast;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.NorthWest;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.Out;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.South;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.SouthEast;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.SouthWest;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.Up;
import static com.luxlunae.bebek.model.MAdventure.DirectionsEnum.West;
import static com.luxlunae.bebek.view.MView.debugPrint;

public class MGlobals {

    public static final String ANYOBJECT = "AnyObject";
    public static final String ANYCHARACTER = "AnyCharacter";
    public static final String ANYDIRECTION = "AnyDirection";
    public static final String NOOBJECT = "NoObject";
    public static final String NOCHARACTER = "NoCharacter";
    public static final String ALLROOMS = "AllLocations";
    public static final String EVERYWHERE = "Everywhere";
    public static final String NOROOMS = "NoLocations";
    public static final String NOWHERE = "Nowhere";
    public static final String THEFLOOR = "TheFloor";
    public static final String THEPLAYER = "%Player%";
    public static final String CHARACTERPROPERNAME = "CharacterProperName";
    public static final String PLAYERLOCATION = "PlayerLocation";
    public static final String HIDDEN = "Hidden";
    public static final int DEFAULT_BACKGROUNDCOLOUR = -16777216; // Color.Black.ToArgb
    public static final int DEFAULT_INPUTCOLOUR = -3005145; // = Color.FromArgb(210, 37, 39)
    public static final int DEFAULT_OUTPUTCOLOUR = -15096438; // Color = Color.FromArgb(25, 165, 138)
    public static final int DEFAULT_LINKCOLOUR = -11806788; // = Color.FromArgb(75, 215, 188)
    public static final String SHORTLOCATIONDESCRIPTION = "ShortLocationDescription";
    public static final String LONGLOCATIONDESCRIPTION = "LongLocationDescription";
    public static final String OBJECTARTICLE = "_ObjectArticle";
    public static final String OBJECTPREFIX = "_ObjectPrefix";
    public static final String OBJECTNOUN = "_ObjectNoun";
    public static final String sLOCATION = "Location";
    public static final String sOBJECT = "Object";
    public static final String sTASK = "Task";
    public static final String sEVENT = "Event";
    public static final String sCHARACTER = "Character";
    public static final String sGROUP = "Group";
    public static final String sVARIABLE = "Variable";
    public static final String sPROPERTY = "Property";
    public static final String sHINT = "Hint";
    public static final String sALR = "Text Override";
    public static final String sGENERAL = "General";
    public static final String SELECTED = "<Selected>";
    public static final String UNSELECTED = "<Unselected>";
    public static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-DD hh:mm:ss", Locale.UK);
    public static final String[] REFERENCE_NAMES =
            new String[]{"%object%", "%objects%", "%object1%", "%object2%", "%object3%", "%object4%",
                    "%object5%", "%direction%", "%direction1%", "%direction2%", "%direction3%", "%direction4%",
                    "%direction5%", "%character%", "%character1%", "%character2%", "%character3%", "%character4%",
                    "%character5%", "%text%", "%text1%", "%text2%", "%text3%", "%text4%", "%text5%", "%number%",
                    "%number1%", "%number2%", "%number3%", "%number4%", "%number5%", "%location%", "%location1%",
                    "%location2%", "%location3%", "%location4%", "%location5%", "%item%", "%item1%", "%item2%",
                    "%item3%", "%item4%", "%item5%"};
    public static final String[] STD_FUNCTION_NAMES =
            new String[]{"AloneWithChar", "CharacterDescriptor", "CharacterName", "CharacterProper",
                    "ConvCharacter", "DisplayCharacter", "DisplayLocation", "DisplayObject", "Held",
                    "LCase", "ListCharactersOn", "ListCharactersIn", "ListCharactersOnAndIn",
                    "ListHeld", "listExits", "ListObjectsAtLocation", "ListWorn", "ListObjectsOn",
                    "ListObjectsIn", "ListObjectsOnAndIn", "LocationName", "LocationOf", "NumberAsText",
                    "ObjectName", "ObjectsIn", "ParentOf", "PCase", "Player", "PopUpChoice", "PopUpInput",
                    "PrevListObjectsOn", "PrevParentOf", "ProperName", "PropertyValue", "Release", "Replace",
                    "Sum", "TaskCompleted", "TheObject", "TheObjects", "Turns", "UCase", "Version", "Worn"};
    private static final String one_to_nineteen[] = new String[]{"zero", "one",
            "two", "three", "four", "five", "six", "seven",
            "eight", "nine", "ten", "eleven", "twelve",
            "thirteen", "fourteen", "fifteen", "sixteen",
            "seventeen", "eighteen", "nineteen"};
    private static final String multiples_of_ten[] = new String[]{"twenty",
            "thirty", "forty", "fifty", "sixty", "seventy",
            "eighty", "ninety"};
    private static final String US_NUMBER_GROUP_NAMES[] = new String[]{"", "thousand", "million",
            "billion", "trillion", "quadrillion",
            "quintillion", "sextillion", "septillion",
            "octillion", "nonillion", "decillion",
            "undecillion", "duodecillion", "tredecillion",
            "quattuordecillion", "quindecillion",
            "sexdecillion", "septendecillion",
            "octodecillion", "novemdecillion",
            "vigintillion"};
    private static final String UK_NUMBER_GROUP_NAMES[] = new String[]{"", "thousand", "million",
            "milliard", "billion", "1000 billion",
            "trillion", "1000 trillion", "quadrillion",
            "1000 quadrillion", "quintillion", "1000 " +
            "quintillion", "sextillion", "1000 sextillion",
            "septillion", "1000 septillion", "octillion",
            "1000 octillion", "nonillion", "1000 " +
            "nonillion", "decillion", "1000 decillion"};
    private static final String CURRENCY = "$";
    private static final String THOUSAND_SEPARATOR = ",";
    private static final String DECIMAL_POINT = ".";
    public static int iLoading;
    public static double dVersion = 9.0021022;
    private static boolean bSearchMatchCase = false;
    private static boolean bFindExactWord = false;
    @Nullable
    public static String sDirectionsRE;

    public static int safeInt(@Nullable Object expr) {
        if (expr == null) {
            return 0;
        }
        String s = expr.toString().trim();
        if (s.equals("")) {
            return 0;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(s);
            } catch (NumberFormatException ex) {
                errMsg("safeInt error with expr <" + expr.toString() + ">", ex);
                return 0;
            }
        }
    }

    public static boolean safeBool(@Nullable Object expr) {
        if (expr == null) {
            return false;
        }
        String s = expr.toString().trim();
        if (s.equals("")) {
            return false;
        }
        try {
            switch (s.toUpperCase()) {
                case "TRUE":
                    return true;
                case "FALSE":
                    return false;
                default:
                    return cbool(s);
            }
        } catch (Exception ex) {
            errMsg("safeBool error with expr <" + expr.toString() + ">", ex);
            return false;
        }
    }

    public static boolean getBool(@Nullable String sBool) {
        if (sBool == null) {
            return false;
        }

        switch (sBool.toUpperCase()) {
            case "-1":
            case "1":
            case "TRUE":
            case "VRAI":
                return true;
            case "0":
            case "FALSE":
            default:
                return false;
        }
    }

    public static MPerspectiveEnum MPerspectiveEnumFromInt(int value) {
        switch (value) {
            default:
            case 0:
                return None;
            case 1:
                return FirstPerson;
            case 2:
                return SecondPerson;
            case 3:
                return ThirdPerson;
        }
    }

    public static MAdventure.DirectionsEnum MDirectionsEnumFromInt(int value) {
        switch (value) {
            case 0:
            default:
                return North;
            case 1:
                return East;
            case 2:
                return South;
            case 3:
                return West;
            case 4:
                return Up;
            case 5:
                return Down;
            case 6:
                return In;
            case 7:
                return Out;
            case 8:
                return NorthEast;
            case 9:
                return SouthEast;
            case 10:
                return SouthWest;
            case 11:
                return NorthWest;

        }
    }

    /**
     * Add a double space to the given text, if the
     * given text is not blank and does not end in
     * a new line.
     *
     * @param sb - text to append space to.
     */
    public static void appendDoubleSpace(@Nullable StringBuilder sb) {
        if (sb == null) {
            return;
        }
        int len = sb.length();
        if (len == 0 || sb.charAt(len - 1) == '\n') {
            return;
        }
        sb.append("  ");
    }

    public static void toProper(@NonNull StringBuilder text) {
        toProper(text, false, false);
    }

    public static void toProper(@NonNull StringBuilder text,
                                boolean bForceRestLower, boolean bExpression) {
        if (text.length() == 0) {
            return;
        }

        int x = 0, y = text.length() - 1;
        if (bExpression &&
                text.charAt(0) == '"' &&
                text.charAt(y) == '"') {
            x++;
            y--;
        }

        text.setCharAt(0, Character.toUpperCase(text.charAt(0)));
        if (bForceRestLower) {
            for (int i = x; i < y; i++) {
                text.setCharAt(i, Character.toLowerCase(text.charAt(i)));
            }
        }
    }

    /**
     * Are the strings represented by two stringbuilders exactly the
     * same (case sensitive comparison)? We implement this ourselves
     * to avoid the memory allocation overheads of doing a
     * str1.toString().equals(str2.toString()) check.
     *
     * @param str1 - first string to compare
     * @param str2 - second string to compare
     * @return TRUE of the strings are identical, FALSE otherwise.
     */
    public static boolean isSameString(@NonNull StringBuilder str1,
                                       @NonNull StringBuilder str2) {
        if (str1.length() != str2.length()) {
            return false;
        }

        for (int i = 0; i < str1.length(); i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                return false;
            }
        }

        return true;
    }

    @NonNull
    public static String replace(@NonNull String[] toSearch,
                                 @NonNull String toFind,
                                 @NonNull String toReplace,
                                 @NonNull int[] nReplaced) {
        Pattern re;
        String sWordBound = (bFindExactWord ? "\b" : "");
        toFind = toFind.replace("\\", "\\\\").replace("*", ".*").replace("?", "\\?");
        if (bSearchMatchCase) {
            re = Pattern.compile(sWordBound + toFind + sWordBound);
        } else {
            re = Pattern.compile(sWordBound + toFind + sWordBound, Pattern.CASE_INSENSITIVE);
        }
        Matcher m = re.matcher(toSearch[0]);
        nReplaced[0] += m.orderedGroups().size();
        toSearch[0] = m.replaceAll(toReplace);
        return toSearch[0];
    }

    @NonNull
    public static String replaceWord(@NonNull String text,
                                     @NonNull String toFind,
                                     @NonNull String toReplace) {
        return text.replaceAll("\\b" + toFind + "\\b", toReplace);
    }

    /**
     * Replace all occurrences of 'target' in 'text' with
     * 'replacement'. Matching is case-sensitive.
     *
     * @param text        - the text to do the find and replace on.
     * @param target      - the text to find.
     * @param replacement - the text to replace matched text with. May
     *                    be NULL - in such cases, matching target
     *                    text will simply be deleted.
     */
    public static void replaceAll(@NonNull StringBuilder text,
                                  @NonNull final String target,
                                  @Nullable String replacement) {
        if (replacement == null) {
            replacement = "";
        }

        final int lenTgt = target.length();
        final int lenRep = replacement.length();
        final int lenDiff = lenRep - lenTgt;

        int pos = text.indexOf(target);
        while (pos >= 0) {
            text.replace(pos, pos + lenTgt, replacement);
            pos = text.indexOf(target, pos + lenTgt + lenDiff);
        }
    }

    /**
     * Replace all occurrences of 'target' in 'text' with
     * 'replacement'. Matching is case-insensitive - that is,
     * target = "bucket" will match "BUCKET", "Bucket", etc.
     *
     * @param text        - the text to do the find and replace on.
     * @param target      - the text to find (case-insensitive).
     * @param replacement - the text to replace matched text with. May
     *                    be NULL - in such cases, matching target
     *                    text will simply be deleted.
     */
    public static void replaceAllIgnoreCase(@NonNull StringBuilder text,
                                            @NonNull final String target,
                                            @Nullable String replacement) {
        if (replacement == null) {
            replacement = "";
        }

        final String tmp = text.toString().toLowerCase();
        final String tgtStr = target.toLowerCase();

        final int lenTgt = tgtStr.length();
        final int lenRep = replacement.length();
        final int lenDiff = lenRep - lenTgt;

        int pos1 = tmp.indexOf(tgtStr);
        int off = 0;
        int pos2;

        while (pos1 >= 0) {
            pos2 = pos1 + off;
            text.replace(pos2, pos2 + lenTgt, replacement);
            off += lenDiff;
            pos1 = tmp.indexOf(tgtStr, pos1 + lenTgt);
        }
    }

    public static int find(@NonNull String[] toSearch,
                           @NonNull String toFind,
                           @Nullable String toReplace) {
        if (contains(toSearch[0], toFind)) {
            int[] result = new int[1];
            if (toReplace != null) {
                toSearch[0] = replace(toSearch, toFind, toReplace, result);
            } else {
                result[0] = 1;
            }
            return result[0];
        }
        return 0;
    }

    public static int findIgnoreCase(@NonNull StringBuilder text,
                                     @NonNull final String target) {
        // returns -1 if not found, 0 or higher if found
        final String tmp = text.toString().toLowerCase();
        final String tgtStr = target.toLowerCase();
        return tmp.indexOf(tgtStr);
    }

    /**
     * Case insensitive search for 'toFind' in 'toSearch'.
     *
     * @param toSearch   - the toSearch to search.
     * @param toFind - the toSearch to find (case insensitive).
     * @return TRUE if toFind was found, FALSE otherwise.
     */
    public static boolean contains(@NonNull StringBuilder toSearch,
                                   @NonNull String toFind) {
        final String tmp = toSearch.toString().toLowerCase();
        final String tgtStr = toFind.toLowerCase();
        return tmp.contains(tgtStr);
    }

    public static boolean contains(@Nullable String toSearch,
                                   @NonNull String toFind) {
        if (toSearch == null) {
            return false;
        }

        String wordBound = bFindExactWord ? "\b" : "";
        toFind = toFind.replace("\\", "\\\\")
                .replace("*", ".*")
                .replace("?", "\\?")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace(".", "\\.");

        Pattern re;
        if (bSearchMatchCase) {
            re = Pattern.compile(wordBound + toFind + wordBound);
        } else {
            re = Pattern.compile(wordBound + toFind + wordBound,
                    Pattern.CASE_INSENSITIVE);
        }
        return re.matcher(toSearch).find();
    }

    public static boolean containsWord(@NonNull String toSearch,
                                       @NonNull String toFind) {
        return contains(toSearch, toFind, true, 0);
    }

    public static boolean contains(@NonNull StringBuilder toSearch,
                                   @NonNull String toFind,
                                   int start) {
        final String tmp = toSearch.toString().toLowerCase();
        final String tgtStr = toFind.toLowerCase();
        return (tmp.indexOf(tgtStr, start) >= 0);
    }

    public static boolean contains(@NonNull String toSearch,
                                   @NonNull String toFind,
                                   boolean exactWord,
                                   int start) {
        // A case ignoring search
        String sPattern = exactWord ?
                "\\b" + java.util.regex.Pattern.quote(toFind) + "\\b" :
                java.util.regex.Pattern.quote(toFind);
        Pattern regex = Pattern.compile(sPattern, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return regex.matcher(toSearch.substring(start)).find();
    }

    private static String groupToWords(int num) {
        // Convert a number between 0 and 999 into words.

        // If the number is 0, return an empty string.
        if (num == 0) {
            return "";
        }

        // Handle the hundreds digit.
        int digit;
        String result = "";
        boolean bAnd = false;
        if (num > 99) {
            digit = num / 100;
            num = num % 100;
            bAnd = true;
            result = one_to_nineteen[digit] + " hundred";
        }

        // If num = 0, we have hundreds only.
        if (num == 0) {
            return result.trim();
        }

        if (bAnd) {
            result += " and";
        }

        // See if the rest is less than 20.
        if (num < 20) {
            // Look up the correct name.
            result += " " + one_to_nineteen[num];
        } else {
            // Handle the tens digit.
            digit = num / 10;
            num = num % 10;
            result += " " + multiples_of_ten[digit - 2];

            // Handle the final digit.
            if (num > 0) {
                result += " " + one_to_nineteen[num];
            }
        }

        return result.trim();
    }

    @NonNull
    public static String numberToString(@NonNull MAdventure adv,
                                        @NonNull String num_str, final @Nullable MReferenceList refs) {
        return numberToString(adv, num_str, refs, true);
    }

    @NonNull
    public static String numberToString(@NonNull MAdventure adv,
                                        @NonNull String num_str, final @Nullable MReferenceList refs,
                                        boolean use_us_group_names) {
        // Return a word representation of the whole number value.
        String result = "";
        String sIn = num_str;

        if (!VB.isNumeric(num_str)) {
            MVariable v = new MVariable(adv);
            v.setToExpr(num_str, refs);
            num_str = String.valueOf(v.getInt());
        }

        try {
            // Get the appropriate group names.
            String groups[];
            if (use_us_group_names) {
                groups = US_NUMBER_GROUP_NAMES;
            } else {
                groups = UK_NUMBER_GROUP_NAMES;
            }

            // Clean the string a bit.
            // Remove "$", ",", leading zeros, and
            // anything after a decimal point.
            num_str = num_str.replace(CURRENCY, "").replace(THOUSAND_SEPARATOR, "");
            //  num_str = num_str.TrimStart(New Char() { "0" c })
            while (num_str.length() > 0 && num_str.charAt(0) == '0') {
                num_str = (num_str.length() > 1) ? num_str.substring(1) : "";
            }

            int pos = num_str.indexOf(DECIMAL_POINT);
            if (pos == 0) {
                return "zero";
            } else if (pos > 0) {
                num_str = num_str.substring(0, pos - 1);
            }

            // See how many groups there will be.
            int num_groups = (num_str.length() + 2) / 3;

            // Pad so length is a multiple of 3.
            //num_str = num_str.PadLeft(num_groups * 3, " "c);
            int padLen = (num_groups * 3) - num_str.length();
            StringBuilder padding = new StringBuilder();
            for (int i = 0; i < padLen; i++) {
                padding.append(" ");
            }
            num_str = padding.toString() + num_str;

            // Process the groups, largest first.
            int group_num;
            for (group_num = num_groups - 1; group_num >= 0; group_num--) {
                // Get the next three digits.
                String group_str = num_str.substring(0, 3);
                num_str = num_str.substring(3);
                int group_value = VB.cint(group_str);

                // Convert the group into words.
                if (group_value > 0) {
                    if (group_num >= groups.length) {
                        result += groupToWords(group_value) + " ?, ";
                    } else {
                        result += groupToWords(group_value) + " " + groups[group_num] + ", ";
                    }
                }
            }

            // Remove the trailing ", ".
            if (result.endsWith(", ")) {
                result = result.substring(0, result.length() - 2);
            }

            result = result.trim();

        } catch (Exception ex) {
            errMsg("numberToString error parsing \"" + sIn + "\"", ex);
        }

        if (result.equals("")) {
            result = "zero";
        }
        return result;
    }

    public static void displayError(@NonNull String sText) {
        debugPrint(ItemEnum.Nothing, "",
                MView.DebugDetailLevelEnum.Error, "<i><c>*** Game error: " + sText + " ***</c></i>");
    }

    public static void errMsg(@NonNull String sMessage) {
        errMsg(sMessage, null);
    }

    public static void errMsg(@NonNull String sMessage, @Nullable Exception ex) {
        String sErrorMsg = sMessage;
        if (ex != null) {
            sErrorMsg += ": " + ex.getMessage();
        }
        Bebek.out("*** ADRIFT Error: " + sErrorMsg + " ***\n");
        GLKLogger.error("*** ADRIFT Error: " + sErrorMsg + " ***");
    }

    public static void TODO(@NonNull String sFunction) {
        if (sFunction.equals("")) {
            sFunction = "This section";
        } else {
            sFunction = "Function \"" + sFunction + "\"";
        }
        Bebek.out("TODO - " + sFunction + " still has to be completed.\n");
        GLKLogger.error("TODO - " + sFunction + " still has to be completed.\n");
    }

    /**
     * Get the text between two matching square brackets
     * (i.e. the function arguments).
     *
     * @param text - the text to scan, the first character of
     *             this text should be an opening square
     *             bracket, "[".
     * @return the text between the brackets (or nothing if
     * there was no opening or closing bracket).
     */
    @NonNull
    public static String getArgs(@NonNull StringBuilder text,
                                 int startPos) {
        if (text.indexOf("[", startPos) < 0 ||
                text.indexOf("]", startPos) < 0) {
            return "";
        }

        startPos++;
        int off = startPos;
        int level = 1;
        while (level > 0) {
            switch (text.charAt(off)) {
                case '[':
                    level++;
                    break;
                case ']':
                    level--;
                    break;
                default:
                    // Ignore
            }
            off++;
        }

        return text.substring(startPos, off - 1);
    }

    @NonNull
    public static String left(@Nullable String text, int len) {
        // A replacement for VB.Left - Substring returns an error
        // if you try to access part of the string that doesn't exist.
        if (text == null || len < 0) {
            return "";
        }
        if (text.length() < len) {
            return text;
        } else {
            return text.substring(0, Math.max(len, 0));
        }
    }

    @NonNull
    public static String right(@Nullable String text, int len) {
        // A replacement for VB.Right
        if (text == null || len < 0) {
            return "";
        }
        if (len > text.length()) {
            return text;
        } else {
            return text.substring(text.length() - len);
        }
    }

    @NonNull
    public static String mid(@Nullable String text, int offset, int len) {
        if (text == null || len < 0) {
            return "";
        }
        if (offset < 1) {
            offset = 1;
        }
        if (offset > text.length()) {
            return "";
        } else if (len > text.length() || len + offset > text.length()) {
            return text.substring(offset - 1);
        } else {
            return text.substring(offset - 1, offset - 1 + len);
        }
    }

    public static int instr(@Nullable String toSearch, String toFind) {
        return instr(1, toSearch, toFind);
    }

    public static int instr(int start, @Nullable String toSearch, @NonNull String toFind) {
        if (toSearch == null || toSearch.equals("")) {
            return 0;
        }
        return toSearch.indexOf(toFind, start - 1) + 1;
    }

    @NonNull
    public static ArrayList<Pattern> getPatterns(@NonNull MAdventure adv,
                                                 @NonNull String cmd, @NonNull String input) {
        ArrayList<Pattern> ret = new ArrayList<>();
        try {
            if (cmd.contains("*")) {
                // If the cmd contains asterisks, strip them out, then add them
                // in one by one until we (possibly) get a match
                cmd = cmd.replace("**", "*");

                int nAsterix = 0;
                String testCmd = cmd;
                while (testCmd.contains("*")) {
                    testCmd = testCmd.replaceFirst("\\*", "");
                    nAsterix++;
                }

                // We should really try all combinations of wildcard removal.
                // E.g. " * %object * " should give us " * %object% " and " %object% * "
                // otherwise we may always end up matching the object name in *

                // For * # * # * we want
                // _ # _ # _
                // * # _ # _
                // _ # * # _
                // _ # _ # *
                // * # * # _
                // * # _ # *
                // _ # * # *
                // * # * # *
                for (int i = nAsterix - 1; i >= -1; i--) {
                    testCmd = cmd;
                    for (int j = 0; j <= i; j++) {
                        testCmd = testCmd.replaceFirst("\\*", "");
                    }
                    String regex = convertToRegex(adv, testCmd);
                    Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
                    Matcher m = p.matcher(input);
                    if (input.equals("") || m.find()) {
                        ret.add(p);
                    }
                }
                return ret;
            } else {
                String regex = convertToRegex(adv, cmd);
                ret.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
                return ret;
            }
        } catch (Exception ex) {
            errMsg("Error in command \"" + cmd + "\"", ex);
            return ret;
        }
    }

    @NonNull
    private static String convertToRegex(@NonNull MAdventure adv, @NonNull String cmd) {
        // For example
        //
        //      [feed/give] %object1% to bear
        //
        // becomes
        //
        //      ^(feed|give) (?<object1>.+?) to bear$
        //
        // Advanced Command Construction, commonly abbreviated to ACC, allows you to define words within the
        // command as being mandatory or optional, and to also give a choice between different words.
        // You can create a choice between words by separating them with a forward slash "/". Words inside
        // square brackets are mandatory, and words inside curly brackets are optional.
        //
        //      E.g. [put/drop] {a/the} flower in{to/side} {the} vase
        //
        // would match
        //
        //      put the flower in the vase
        //      drop the flower in the vase
        //      put the flower inside vase
        //      drop the flower inside the vase
        //      put flower into the vase
        //
        // A third way of matching commands in ADRIFT is to make use of wildcards. This allows
        // you to match anything a user types against part of your command.
        //
        //      E.g. drop * in*
        //
        // would match anything such as "drop the ball in the box", or "drop match into the
        // large can of petrol".
        String ret = cmd;

        // Convert any special RE characters
        ret = ret.replace("+", "\\+")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("?", "\\?");        // added by Tim (fix 3 Monkeys issue)

        // Convert wildcards
        ret = ret.replace(" * ", " ([[#]] )?")
                .replace("* ", "([[#]] )?")
                .replace(" *", "( [[#]])?")
                .replace("*", "[[#]]")
                .replace("[[#]]", ".*?");

        // ret = ret.replace("_", " ");  // comment out by Tim to allow sclogic tests to run

        // Convert optional text and mandatory text
        ret = ret.replace("{", "(")
                .replace("}", ")?")
                .replace("[", "(")
                .replace("]", ")")
                .replace("/", "|");

        // Replace references
        if (sDirectionsRE == null) {
            StringBuilder sb = new StringBuilder();
            for (MAdventure.DirectionsEnum eDirection : MAdventure.DirectionsEnum.values()) {
                sb.append(adv.mDirectionNames.get(eDirection).toLowerCase().replace("/", "|"));
                if (eDirection != NorthWest) {
                    sb.append("|");
                }
            }
            sDirectionsRE = sb.toString();
        }
        ret = ret.replace("%direction%", "(?<direction>" + sDirectionsRE + ")");
        // TODO - replace above with custom names if changed

        ret = ret.replace("%objects%", "(?<objects>.+?)");
        ret = ret.replace("%characters%", "(?<characters>.+?)");

        for (int i = 1; i <= 5; i++) {
            ret = ret.replace("%object" + i + "%", "(?<object" + i + ">.+?)");
            ret = ret.replace("%character" + i + "%", "(?<character" + i + ">.+?)");
            ret = ret.replace("%number" + i + "%", "(?<number" + i + ">-?[0-9]+)");
            ret = ret.replace("%text" + i + "%", "(?<text" + i + ">.+?)");
            ret = ret.replace("%location" + i + "%", "(?<location" + i + ">.+?)");
            ret = ret.replace("%item" + i + "%", "(?<item" + i + ">.+?)");
            ret = ret.replace("%direction" + i + "%", "(?<direction" + i + ">" + sDirectionsRE + ")");
        }

        return "^" + ret.trim() + "$";
    }

    @NonNull
    public static String stripCarats(@NonNull String sText) {
        if (sText.contains("<#") && sText.contains("#>")) {
            sText = sText.replace("<#", "[[==~~").replace("#>", "~~==]]");
        }
        return sText.replaceAll("<(.|\n)*?>", "").replace("[[==~~", "<#").replace("~~==]]", "#>");
    }

    public static int getCharacterCount(@NonNull String s, char c) {
        int ret = 0;
        for (int i = 0, sz = s.length(); i < sz; i++) {
            if (s.charAt(i) == c) {
                ret++;
            }
        }
        return ret;
    }

    public static void chopLast(@NonNull StringBuilder text) {
        // Chops the last character off a string
        int len = text.length();
        if (len > 0) {
            text.deleteCharAt(len - 1);
        }
    }

    @NonNull
    public static String guessPluralNoun(@Nullable String noun) {
        if (noun == null) {
            return "";
        }

        switch (noun) {
            case "deer":
            case "fish":
            case "cod":
            case "mackerel":
            case "trout":
            case "moose":
            case "sheep":
            case "swine":
            case "aircraft":
            case "blues":
            case "cannon": // Identical Singular & Plural nouns
                return noun;
            case "ox": // Irregular plurals
                return "oxen";
            case "cow":
                return "kine";
            case "child":
                return "children";
            case "foot": // Umlaut plurals
                return "feet";
            case "goose":
                return "geese";
            case "louse":
                return "lice";
            case "mouse":
                return "mice";
            case "tooth":
                return "teeth";
        }

        switch (noun.length()) {
            case 0:
                return "";

            case 1:
            case 2:
                return noun + "s";

            default:
                switch (noun.substring(noun.length() - 3)) {
                    case "man": // Umlaut plural
                        return noun.substring(0, noun.length() - 2) + "en";
                    case "ies":
                        return noun;
                }

                switch (noun.substring(noun.length() - 2)) {
                    case "sh":
                    case "ss":
                    case "ch": // Sibilant sounds
                        return noun + "es";
                    case "ge":
                    case "se": // Sibilant sounds, ending with 'e'
                        return noun + "s";
                    case "ex":
                        return noun.substring(0, noun.length() - 2) + "ices";
                    case "is":
                        return noun.substring(0, noun.length() - 2) + "es";
                    case "um":
                        return noun.substring(0, noun.length() - 2) + "a";
                    case "us":
                        return noun.substring(0, noun.length() - 2) + "i";
                }

                switch (noun.substring(noun.length() - 1)) {
                    case "f":
                        switch (noun) {
                            case "dwarf":
                            case "hoof":
                            case "roof":
                                // the exceptions
                                return (noun + "s");
                            default:
                                return noun.substring(0, noun.length() - 1) + "ves";
                        }

                    case "o":
                        // nouns ending in 'o' preceded by a consonant
                        switch (noun.substring(noun.length() - 2, noun.length() - 1)) {
                            case "b":
                            case "c":
                            case "d":
                            case "f":
                            case "g":
                            case "h":
                            case "j":
                            case "k":
                            case "l":
                            case "m":
                            case "n":
                            case "p":
                            case "q":
                            case "r":
                            case "s":
                            case "t":
                            case "v":
                            case "w":
                            case "x":
                            case "z":
                                return noun + "es";
                        }
                        break;

                    case "x":
                        // normally ends in "es"
                        return noun + "es";

                    case "y":
                        // nouns ending in y preceeded by a consonant usually drop y and add ies
                        switch (noun.substring(noun.length() - 2, noun.length() - 1)) {
                            case "b":
                            case "c":
                            case "d":
                            case "f":
                            case "g":
                            case "h":
                            case "j":
                            case "k":
                            case "l":
                            case "m":
                            case "n":
                            case "p":
                            case "q":
                            case "r":
                            case "s":
                            case "t":
                            case "v":
                            case "w":
                            case "x":
                            case "z":
                                return noun.substring(0, noun.length() - 1) + "ies";
                        }
                        break;
                }
        }

        return noun.endsWith("s") ? noun : (noun + "s");
    }

    public enum ItemEnum {
        Nothing,        // -1
        Location,       // 0
        Object,         // 1
        Task,           // 2
        Event,          // 3
        Character,      // 4
        Group,          // 5
        Variable,       // 6
        Property,       // 7
        Hint,           // 8
        ALR,            // 9
        General         // 10
    }

    public enum MPerspectiveEnum {
        None,           // 0
        FirstPerson,    // 1, I/Me/Myself
        SecondPerson,   // 2, You/Yourself
        ThirdPerson     // 3, He/She/Him/Her
        // It
        // We
        // They
    }


    public enum ArticleTypeEnum {
        Definite,       // 0, The
        Indefinite,     // 1, A
        None            // 2
    }
}
