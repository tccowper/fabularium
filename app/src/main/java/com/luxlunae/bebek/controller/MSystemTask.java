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

package com.luxlunae.bebek.controller;

import android.support.annotation.NonNull;

import com.luxlunae.bebek.Bebek;
import com.luxlunae.bebek.model.MAction;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MEvent;
import com.luxlunae.bebek.model.MHint;
import com.luxlunae.bebek.model.MTask;
import com.luxlunae.bebek.model.io.MFileIO;
import com.luxlunae.bebek.model.state.MStateStack;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.glk.GLKLogger;

import java.io.File;

class MSystemTask {
    static boolean executeSystemTask(@NonNull MAdventure[] adv1,
                                     @NonNull String input,
                                     boolean bEarly) throws InterruptedException {
        // We take a 1 size array of Adventure objects so that we can change it if restart is called
        MAdventure adv = adv1[0];

        switch (input) {
            case "hint":
            case "hints":
                if (bEarly) {
                    return false;
                } else {
                    hint(adv);
                }
                break;
            case "pronouns":
                if (bEarly) {
                    return false;
                } else {
                    String msg = "At the moment, <q>it</q> means " +
                            adv.mIt + ", <q>him</q> means " + adv.mHim +
                            ", <q>her</q> means " + adv.mHer + " and " +
                            "<q>them</q> means " + adv.mThem + ".";
                    MView.displayText(adv, msg, true);
                    MView.mOutputText.setLength(0);
                }
                break;
            case "restart":
                String path = adv.getFullPath();
                adv = new MAdventure();
                adv.setFullPath(path);
                adv1[0] = adv;
                restart(adv);
                if (!bEarly) {
                    MView.mOutputText.setLength(0);
                    MView.mOutputText.append("***SYSTEM***");
                }
                break;
            case "restore":
                restore(adv);
                MView.displayText(adv, "\n\n", true);
                if (!bEarly) {
                    MView.mOutputText.setLength(0);
                    MView.mOutputText.append("***SYSTEM***");
                }
                break;
            case "save":
                if (bEarly) {
                    return false;
                } else {
                    save(adv);
                }
                break;
            case "save as":
            case "saveas":
                if (bEarly) {
                    return false;
                } else {
                    save(adv);
                }
                break;
            case "quit":
                quit(adv, !bEarly);
                break;
            case "undo":
                undo(adv);
                if (bEarly) {
                    MView.displayText(adv, "\n\n", true);
                }
                break;
            case "wait":
            case "z":
                if (bEarly) {
                    return false;
                } else {
                    MView.mOutputText.setLength(0);
                    StringBuilder tmp = new StringBuilder("Time passes...");
                    adv.mALRs.evaluate(tmp, MParser.mReferences);
                    MView.mOutputText.append(tmp);
                    for (int i = 0; i < adv.getWaitTurns(); i++) {
                        MParser.incrementTurnOrTime(adv, MEvent.EventTypeEnum.TurnBased);
                    }
                }
                break;
            default:
                return false;
        }

        return true;
    }

    private static void restart(@NonNull MAdventure adv) throws InterruptedException {
        adv.open(adv.getFullPath());
    }

    private static void restore(@NonNull MAdventure adv) throws InterruptedException {
        // The Bebek implementation always prompts for file names
        String sFilename = Bebek.promptForSaveFileName(false);

        if (!sFilename.equals("")) {
            if (new File(sFilename).exists()) {
                adv.mStates.clear();
                for (MTask t : adv.mTasks.values()) {
                    // Just in case the save file doesn't cover that task
                    t.setCompleted(false);
                }
                MStateStack states = new MStateStack();
                states.loadState(adv, sFilename);
                MView.displayText(adv, "Game restored" + "\n", true);
                adv.mGameState = MAction.EndGameEnum.Running;
                adv.mDisplayedWinOrLose = false;
                MView.updateStatusBar(adv);
                MView.displayText(adv, adv.mLocations.get(adv.getPlayer().getLocation().getLocationKey()).getViewLocation(), true);
                adv.mJustRunSystemTask = false; // Allow events to run
                adv.prepareForNextTurn();

                GLKLogger.error("TODO: MUserSession: Restore - restore map node");
                //UserSession.Map.RecalculateNode(Adventure.Map.FindNode(Adventure.Player.Location.LocationKey))
                //UserSession.Map.SelectNode(Adventure.Player.Location.LocationKey)
            } else {
                MView.displayText(adv, "Save file not found.", true);
            }
        }
    }

    private static boolean hint(@NonNull MAdventure adv) throws InterruptedException {
        boolean first = true;
        for (MHint h : adv.mHints.values()) {
            if (h.mRestrictions.passes(true, null)) {
                if (!first) {
                    Bebek.out("<br>");
                }
                first = false;
                Bebek.out("<br><b><i>" + h.getQuestion() + "</i></b><br>");
                Bebek.out("View the subtle hint for this topic? ");
                char resp = Character.toLowerCase(Bebek.yesNo());
                if (resp == 'n') {
                    Bebek.out("<font color=\"input\">No</font>");
                    return true;
                }
                Bebek.out("<font color=\"input\">Yes</font><br><i>" +
                        h.getSubtleHint().toString(true) + "</i><br>");

                Bebek.out("<br>View the unsubtle hint for this topic? ");
                resp = Character.toLowerCase(Bebek.yesNo());
                if (resp == 'n') {
                    Bebek.out("<font color=\"input\">No</font>");
                    return true;
                }
                Bebek.out("<font color=\"input\">Yes</font><br><i>" +
                        h.getSledgeHammerHint().toString(true) + "</i>");
            }
        }
        if (first) {
            Bebek.out("<i>No hints currently available.</i>");
        }
        return true;
    }

    private static boolean save(@NonNull MAdventure adv) throws InterruptedException {
        // The Bebek implementation always prompts for file names
        String sFilename = Bebek.promptForSaveFileName(true);

        if (sFilename.equals("")) {
            MView.displayText(adv, "Cancelled");
            return false;
        }

        MStateStack states = new MStateStack();
        if (MFileIO.saveState(adv, states.getState(adv), sFilename)) {
            MView.displayText(adv, "Game saved");
            adv.setChanged(false);
            return true;
        } else {
            MView.displayText(adv, "Error saving game");
            return false;
        }
    }

    private static boolean quit(@NonNull MAdventure adv, boolean bJustGame) throws InterruptedException {
        if (adv.mGameState == MAction.EndGameEnum.Running) {
            if (adv.getChanged()) {
              /*  switch (MessageBox.Show("Would you like to save your current position?", "Quit Game",
                        MessageBoxButtons.YesNoCancel, MessageBoxIcon.Question)) {
                    Case DialogResult.Yes:
                    save(True);
                    Case DialogResult.No:
                    // Continue
                    Case DialogResult.Cancel:
                    Return False;
                } */
                GLKLogger.debug("TODO: Quitting... prompt if user wants to save current position");
            }
        }

        if (bJustGame) {
            adv.mGameState = MAction.EndGameEnum.Neutral;
        } else {
            Bebek.quit();
        }

        return true;
    }

    private static void undo(@NonNull MAdventure adv) {
        if (adv.mStates.setLastState(adv)) {
            adv.mDisplayedWinOrLose = false;
            GLKLogger.error("TODO: Undo: recalculate map node");
            //MUserSession.Map.RecalculateNode(Adventure.Map.FindNode(Adventure.Player.Location.LocationKey))
            //MUserSession.Map.SelectNode(Adventure.Player.Location.LocationKey)
            MView.updateStatusBar(adv);
            String sText = adv.mTurnOutput;
            MView.displayText(adv, "Undone.", false, false, false);
            if (!sText.equals("")) {
                MView.displayText(adv, sText);
            }
        } else {
            MView.displayText(adv, "Sorry, <c>undo</c> is not currently available.");
        }
        adv.mJustRunSystemTask = true;
    }
}
