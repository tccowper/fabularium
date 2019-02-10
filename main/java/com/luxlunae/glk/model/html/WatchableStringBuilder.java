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

import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;

public class WatchableStringBuilder extends SpannableStringBuilder {
    private int mZeroBreak = -1;

    void setZeroBreak() {
        mZeroBreak = length();
    }

    void handlePara() {
        int len = length();

        if (len != 0) {
            // <P> tag trumps any outstanding <BR HEIGHT=0>
            mZeroBreak = -1;

            if (charAt(len - 1) == '\n') {
                if (len >= 2 && charAt(len - 2) == '\n') {
                    // content already terminated with two line breaks
                    return;
                }
                // content terminated with one line break, add another
                append("\n");
                return;
            }

            // got content, but don't have any line breaks, add two
            append("\n\n");
        }
    }

    @NonNull
    @Override
    public WatchableStringBuilder append(@NonNull CharSequence text) {
        if (mZeroBreak >= 0) {
            if (mZeroBreak > length()) {
                // text was flushed before zero break candidate
                // triggered or invalidated, so shift the candidate to
                // start of the new text
                mZeroBreak = 0;
            }
            for (int i = 0, len = text.length(); i < len; i++) {
                char ch = text.charAt(i);
                if (!Character.isWhitespace(ch) && ch != '\uFFFC') {
                    super.insert(mZeroBreak, "\n");
                    mZeroBreak = -1;
                    break;
                }
            }
        }
        super.append(text);
        return this;
    }

    @NonNull
    @Override
    public WatchableStringBuilder append(char text) {
        if (mZeroBreak >= 0) {
            if (mZeroBreak > length()) {
                // text was flushed before zero break candidate
                // triggered or invalidated, so shift the candidate to
                // start of the new text
                mZeroBreak = 0;
            }
            if (!Character.isWhitespace(text) && text != '\uFFFC') {
                super.insert(mZeroBreak, "\n");
                mZeroBreak = -1;
            }
        }
        super.append(text);
        return this;
    }
}
