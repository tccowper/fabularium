/*
 * Copyright (C) 2019 Tim Cadogan-Cowper.
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

package com.luxlunae.bebek.controller;

import android.support.annotation.NonNull;

import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.view.MView;

import static com.luxlunae.bebek.controller.MDebugger.processDebugCommand;

public class MController {
    // The internal version number of the Adrift Runner we have ported
    // This is version code is used by some games (e.g. Humbug) to determine
    // whether the runner can support them or not.
    public static final String AdriftProductVersion = "9.0.21022";

    private final MAdventure mAdv;
    private final MView mView;

    public MController(@NonNull MAdventure a, @NonNull MView v) {
        mAdv = a;
        mView = v;
    }

    public void start() throws InterruptedException {
        mView.setInputTimer(true);
        while (!mView.isStopping()) {
            String input = mView.getUserInput(mAdv, null, null);
            if (input == null) {
                // ------------------
                // Just the enter key
                // ------------------
                // Still submit this, as some games (like PK Girl) depend
                // upon single key enter press to move to the next screen
                mAdv.processCommand("");
            } else if (input.startsWith("@bebek")) {
                // Debug command
                processDebugCommand(mAdv, input.split(" "));
            } else {
                // --------------------
                // One or more commands
                // --------------------
                // Like Scare, we allow the user to enter multiple commands
                // on one line, either separated by a comma or a period.
                //
                // This is an essential feature for some games - e.g.
                // "Three Monkeys" requires the user to type commands
                // such as "chimp, climb tree" which is then processed as
                // two commands - "chimp" (which changes the actor variable
                // to the chimp) and "climb tree" (which works only if the
                // current actor is the chimp).
                String[] cmds = input.split("[.,]");
                for (String cmd : cmds) {
                    mAdv.processCommand(cmd.trim());
                }
            }
        }
    }
}
