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

import com.luxlunae.glk.GLKLogger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class VB {

    private static final long UINT_MAX = 4294967295L;
    private static final int USHORT_MAX = 65535;

    // N.B. From Microsoft docs, "The val function recognizes only the period (.) as a valid
    // decimal separator. When other decimal separators are used, as in international applications,
    // use CDbl or cint." In other words, we should be safe assuming that the number locale of ADRIFT
    // games that call val() is always US.
    private static final NumberFormat US_NUMBER_PARSER = NumberFormat.getNumberInstance(Locale.US);

    private int mRndSeed = 327680;

    private static int toInt32(@NonNull byte[] value, int startIndex) {
        return (value[startIndex] & 0xFF) |
                (value[startIndex + 1] & 0xFF) << 8 |
                (value[startIndex + 2] & 0xFF) << 16 |
                (value[startIndex + 3] & 0xFF) << 24;
    }

    @NonNull
    private static byte[] getBytes(float f) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(f).array();
    }

    @NonNull
    private static byte[] getBytes(double d) {
        return ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(d).array();
    }

    public static int cint(@Nullable Object value) {
        if (value == null) {
            return 0;       // maybe should throw an exception
        }
        String val2 = (value instanceof String) ? ((String) value).trim() : value.toString();
        if (val2.equals("")) {
            return 0;
        }
        // TODO: mimic VB cint's rounding of double values
        return Integer.valueOf(val2.trim());
    }

    public static boolean cbool(@Nullable String value) {
        // If an expression evaluates to a nonzero value, cbool
        // returns True; otherwise, it returns False.
        // A run-time error occurs if the expression can not be
        // interpreted as a numeric value
        if (value == null) {
            return false;
        }
        String val2 = value.trim().toLowerCase();
        return !val2.equals("") && !val2.equals("false") && (val2.equals("true") || (Integer.valueOf(val2) != 0));
    }

    public static Number val(@Nullable String value) {
        // Mimic VB's val function
        // TODO: This isn't quite a perfect imitation as the VB function
        // takes malformed strings (e.g. 1jkh345dsfewr => 1345).
        // But for now it should be good enough.
        // Thanks https://stackoverflow.com/questions/852639/convert-vbs-val-to-java
        if (value == null) {
            return 0;
        }
        try {
            return US_NUMBER_PARSER.parse(value);
        } catch (ParseException e) {
            GLKLogger.warn("val(): warning, argument " + value + " is not a number. Defaulting to 0.");
            return 0;
        }
    }

    public static boolean isNumeric(@Nullable Object value) {
        if (value == null) {
            return false;
        }
        String tmp = (value instanceof String ? (String) value : value.toString());
        try {
            //noinspection ResultOfMethodCallIgnored
            Double.parseDouble(tmp);
            return true;
        } catch (IllegalArgumentException e) {
            // VB's isNumeric() also returns true for any boolean value:
            return Boolean.parseBoolean(tmp);
        }
    }

    @NonNull
    public static String inputBox(String prompt, String title, String defaultResponse) {
        return Bebek.promptForInput(prompt, defaultResponse);
    }

    public static char msgBoxYesNo(String prompt) {
        return Bebek.msgboxYesNo(prompt);
    }

    public float rnd(float Number) {
        /* If number is less than zero, rnd generates the same number every time,
         * using Number as the seed. If number is greater than zero, rnd
         * generates the next random number in the sequence. If number is
         * equal to zero, rnd generates the most recently generated number. If
         * number is not supplied, rnd generates the next random number in the sequence. */
        int num1 = mRndSeed;
        if ((double) Number != 0.0) {
            if ((double) Number < 0.0) {
                byte[] bytes = getBytes(Number);
                long num2 = (long) toInt32(bytes, 0) & UINT_MAX;
                num1 = (int) (num2 + (num2 >> 24) & 16777215L);
            }
            num1 = ((int) ((long) num1 * 1140671485L + 12820163L & 16777215L));
        }
        mRndSeed = num1;
        return (float) num1 / 1.677722E+07f;
    }

    public float rnd() {
        return rnd(1.0f);
    }

    public void randomize(double Number) {
        /* The RANDOMIZE function does not return a value, but rather sets the seed value used by the RND function.*/
        int num1 = mRndSeed;
        byte[] bytes = getBytes(Number);
        int num2 = toInt32(bytes, 4);
        int num3 = (num2 & USHORT_MAX ^ num2 >> 16) << 8;
        mRndSeed = (num1 & -16776961) | num3;
    }
}
