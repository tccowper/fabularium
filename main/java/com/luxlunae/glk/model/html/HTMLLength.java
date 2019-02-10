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
package com.luxlunae.glk.model.html;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.TypedValue;

import com.luxlunae.glk.GLKLogger;


class HTMLLength {
    static final int SIZE_TO_CONTENTS = -2; // we start at -2 for size constants, as there cannot be any valid size less than -1
    final boolean mIsPercent;
    final int mValue;
    final int mRawValue;

    /**
     * Create a new length
     *
     * @param length - nn for pixels or nn% for percentage length
     */
    HTMLLength(@Nullable String length, @NonNull Resources r) {
        if (length == null) {
            mValue = SIZE_TO_CONTENTS;
            mRawValue = 0;
            mIsPercent = false;
        } else {
            int len = length.length();

            if (length.charAt(len - 1) == '%') {
                // proportional (%)
                mIsPercent = true;
                int tmp;
                try {
                    tmp = Integer.valueOf(length.substring(0, len - 1));
                } catch (NumberFormatException e) {
                    GLKLogger.error("HTMLLength: invalid % dimension: '" + length + "', defaulting to 0%.");
                    tmp = 0;
                }
                if (tmp < 0) {
                    tmp = 0;
                } else if (tmp > 100) {
                    tmp = 100;
                }
                mRawValue = tmp;
                mValue = mRawValue;
            } else {
                // fixed (dip)
                mIsPercent = false;
                int tmp;
                try {
                    tmp = Integer.valueOf(length);
                } catch (NumberFormatException e) {
                    GLKLogger.error("HTMLLength: invalid fixed dimension: '" + length + "', defaulting to 0.");
                    tmp = 0;
                }
                mRawValue = tmp;
                mValue = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, mRawValue, r.getDisplayMetrics());
            }

        }
    }
}
