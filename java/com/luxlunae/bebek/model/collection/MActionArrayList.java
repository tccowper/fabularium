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

import com.luxlunae.bebek.model.MAction;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.view.MView;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.EnumSet;

import static org.xmlpull.v1.XmlPullParser.END_DOCUMENT;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

public class MActionArrayList extends ArrayList<MAction> {
    private static final String XTAG_ACTIONS = "Actions";

    public MActionArrayList() {
        super();
    }

    public MActionArrayList(@NonNull MAdventure adv,
                            @NonNull XmlPullParser xpp,
                            double version) throws Exception {
        this();

        xpp.require(START_TAG, null, XTAG_ACTIONS);

        int depth = xpp.getDepth();
        int eventType;

        while ((eventType = xpp.nextTag()) != END_DOCUMENT &&
                xpp.getDepth() > depth) {
            if (eventType == START_TAG) {
                final String actionType = xpp.getName();
                String action = xpp.nextText();
                add(new MAction(adv, actionType, action, version));
            }
        }

        xpp.require(END_TAG, null, XTAG_ACTIONS);
    }

    public void executeAll(@NonNull EnumSet<MTask.ExecutionStatus> curStatus) {
        MView.mDebugIndent++;
        for (MAction act : this) {
            act.execute("", null, false, curStatus);
        }
        MView.mDebugIndent--;
    }

    public int getNumberOfKeyRefs(@NonNull String key) {
        int ret = 0;
        for (MAction a : this) {
            if (a.referencesKey(key)) {
                ret++;
            }
        }
        return ret;
    }

    public boolean deleteKey(@NonNull String key) {
        for (int i = size() - 1; i >= 0; i--) {
            if (get(i).referencesKey(key)) {
                remove(i);
            }
        }
        return true;
    }

    @NonNull
    public MActionArrayList copy() {
        MActionArrayList ret = new MActionArrayList();
        for (int i = 0; i < size(); i++) {
            MAction act = get(i).copy();
            ret.add(act);
        }
        return ret;
    }
}
