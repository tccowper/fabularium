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

package com.luxlunae.bebek.view;

import android.support.annotation.NonNull;

import com.luxlunae.bebek.Bebek;
import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.controller.MParser;
import com.luxlunae.bebek.model.MAction;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.glk.GLKLogger;

import static com.luxlunae.bebek.MGlobals.appendDoubleSpace;

public class MView {
    public static boolean mDisplaying = false; // In case any output is once only - don't want it to trigger when we're just testing the text
    public static int mDebugIndent;
    @NonNull
    public static StringBuilder mOutputText = new StringBuilder();
    public static boolean mNoDebug;

    public static void updateStatusBar(@NonNull MAdventure adv) {
        String description = "";
        String score = "";
        StringBuilder userStatus = new StringBuilder();

        if (adv.mGameState == MAction.EndGameEnum.Running) {
            if (adv.getPlayer() != null) {
                if (adv.getPlayer().getLocation().getExistsWhere() == MCharacter.MCharacterLocation.ExistsWhere.Hidden ||
                        adv.getPlayer().getLocation().getLocationKey().equals("")) {
                    description = "(Nowhere)";
                } else {
                    description = adv.mLocations.get(adv.getPlayer().getLocation().getLocationKey()).getShortDescriptionSafe();
                    if (adv.getPlayer().getLocation().getExistsWhere() != MCharacter.MCharacterLocation.ExistsWhere.AtLocation) {
                        description += " (" + adv.getPlayer().getLocation().toString() + ")";
                    }
                }
            }
            userStatus.append(adv.getUserStatus());
        } else {
            description = "Game Over";
        }

        if (adv.getMaxScore() > 0) {
            score = "Score: " + adv.getScore();
        }

        if (!adv.getEnabled(MAdventure.EnabledOptionEnum.Score)) {
            score = "";
        }
        Bebek.updateStatusBar(adv, description, score, userStatus);

        if (Bebek.BEBEK_DEBUG_ENABLED) {
            GLKLogger.error("TODO: MUserSession: updateStatusBar: update map");
        }
        //If UserSession.Map.Map IsNot Nothing Then
        //Dim node As MapNode = UserSession.Map.Map.FindNode(Adventure.Player.Location.LocationKey)
        //If node IsNot Nothing Then node.Text = sDescription
        //End If
    }

    public static void displayText(@NonNull MAdventure adv, @NonNull String text) {
        displayText(adv, text, false, true, true);
    }

    public static void displayText(@NonNull MAdventure adv, @NonNull String text, boolean flush) {
        displayText(adv, text, flush, true, true);
    }

    public static void displayText(@NonNull MAdventure adv, @NonNull String text, boolean flush,
                                   boolean allowALRs, boolean record) {
        mDisplaying = true;

        boolean allowPSpace = true;

        if (adv.mVersion < 5) {
            // ViewRoom function used to always start at beginning of line, so no pspace

            // TCC Addition: ensure that DisplayLocation is always preceded by a blank line
            int t = text.indexOf("%DisplayLocation[");
            if (t > 0) {
                if (text.charAt(t - 1) != '\n') {
                    String tmp = text;
                    text = text.substring(0, t);
                    if (!text.endsWith("\n")) {
                        text += "\n";
                    }
                    text = text + "\n" + tmp.substring(t);
                }
            }
            if (text.startsWith("%DisplayLocation[")) {
                allowPSpace = false;
            }
        }

        if (allowALRs) {
            StringBuilder tmp = new StringBuilder(text);
            adv.mALRs.evaluate(tmp, MParser.mReferences);
            text = tmp.toString();
        }
        if (allowPSpace && !text.equals("\n") && !text.equals("")) {
            appendDoubleSpace(mOutputText);
        }
        mOutputText.append(text);

        if (flush) {
            if (!text.startsWith("<c>") && text.endsWith("</c>\n")) {
                underlineNouns(mOutputText);
            }
            Bebek.out(mOutputText.toString());
           /* if (fRunner.miStartTranscript.Text = "Stop Transcript") {
                try {
                    Dim stmWriter As New IO.StreamWriter(sTranscriptFile, True);
                    stmWriter.Write(stripCarats(mOutputText).Replace("Ø", ">"));
                    stmWriter.Close();
                } catch (exIO As IO.IOException){
                    errMsg("Unable to output to transcript: " & exIO.Message)
                }
            }*/
            if (record) {
                String tmp = mOutputText.toString();
                while (tmp.endsWith("\n")) {
                    tmp = tmp.substring(0, tmp.length() - 1);
                }
                adv.mTurnOutput += tmp;
            }
            mOutputText.setLength(0);
        }

        mDisplaying = false;
    }

    private static void underlineNouns(@NonNull StringBuilder text) {
        if (Bebek.BEBEK_DEBUG_ENABLED) {
            GLKLogger.error("TODO: MUserSession: underlineNouns");
        }
    }

    public static void debugPrint(MGlobals.ItemEnum itemType,
                                  @NonNull String key, DebugDetailLevelEnum detailLevel,
                                  @NonNull String message) {
        debugPrint(itemType, key, detailLevel, message, true);
    }

    public static void debugPrint(MGlobals.ItemEnum itemType,
                                  @NonNull String key, DebugDetailLevelEnum detailLevel,
                                  @NonNull String message, boolean appendNewLine) {
        if (detailLevel == DebugDetailLevelEnum.Error) {
            GLKLogger.error("[" + itemType + (!key.equals("") ? (" " + key) : "") + "] " +
                    (message.equals("") ? "(no output)" : message) + (appendNewLine ? "\n" : ""));
        } else if (!mNoDebug && Bebek.BEBEK_DEBUG_ENABLED) {
            GLKLogger.debug("[" + itemType + (!key.equals("") ? (" " + key) : "") + "] " +
                    (message.equals("") ? "(no output)" : message) + (appendNewLine ? "\n" : ""));
        }
    }

    public enum DebugDetailLevelEnum {
        Error,          // 0
        High,           // 1
        Medium,         // 2
        Low             // 3
    }
}
