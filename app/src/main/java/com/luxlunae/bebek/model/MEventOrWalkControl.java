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

import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MEventOrWalkControl implements Cloneable {

    public MControlEnum eControl = MControlEnum.Start;
    public CompleteOrNotEnum eCompleteOrNot = CompleteOrNotEnum.Completion;
    @NonNull
    public String mTaskKey = "";

    public MEventOrWalkControl() {
        super();
    }

    public MEventOrWalkControl(@NonNull XmlPullParser xpp) throws IOException, XmlPullParserException {
        this();

        xpp.require(START_TAG, null, "Control");

        String[] sData = xpp.nextText().split(" ");
        eControl = MEventOrWalkControl.MControlEnum.valueOf(sData[0]);
        eCompleteOrNot = MEventOrWalkControl.CompleteOrNotEnum.valueOf(sData[1]);
        mTaskKey = sData[2];

        xpp.require(END_TAG, null, "Control");
    }

    @Override
    @Nullable
    public MEventOrWalkControl clone() {
        try {
            return (MEventOrWalkControl) super.clone();
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("MEventOrWalkControl: clone failed: " + e.getMessage());
            return null;
        }
    }

    public enum MControlEnum {
        Start,      // 0
        Stop,       // 1
        Suspend,    // 2
        Resume      // 3
    }

    public enum CompleteOrNotEnum {
        Completion,     // 0
        UnCompletion    // 1
    }
}
