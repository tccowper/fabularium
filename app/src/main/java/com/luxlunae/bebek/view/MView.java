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

import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.MCharacter;
import com.luxlunae.bebek.model.MLocation;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.GLKModel;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import static com.luxlunae.bebek.MGlobals.ItemEnum.Nothing;
import static com.luxlunae.bebek.MGlobals.appendDoubleSpace;
import static com.luxlunae.bebek.MGlobals.stripCarats;
import static com.luxlunae.bebek.controller.MDebugger.BEBEK_DEBUG_ENABLED;
import static com.luxlunae.bebek.model.MAction.EndGameEnum.Running;
import static com.luxlunae.bebek.model.MAdventure.EnabledOptionEnum.Score;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.AtLocation;
import static com.luxlunae.bebek.model.MCharacter.MCharacterLocation.ExistsWhere.Hidden;
import static com.luxlunae.bebek.model.MEvent.EventTypeEnum.TimeBased;
import static com.luxlunae.glk.GLKConstants.evtype_CharInput;
import static com.luxlunae.glk.GLKConstants.evtype_LineInput;
import static com.luxlunae.glk.GLKConstants.evtype_Timer;
import static com.luxlunae.glk.GLKConstants.filemode_Read;
import static com.luxlunae.glk.GLKConstants.filemode_Write;
import static com.luxlunae.glk.GLKConstants.fileusage_SavedGame;
import static com.luxlunae.glk.GLKConstants.winmethod_Above;
import static com.luxlunae.glk.GLKConstants.winmethod_Fixed;
import static com.luxlunae.glk.GLKConstants.wintype_TextBuffer;
import static com.luxlunae.glk.GLKConstants.wintype_TextGrid;
import static com.luxlunae.glk.controller.GLKController.glk_exit;
import static com.luxlunae.glk.controller.GLKController.glk_fileref_create_by_prompt;
import static com.luxlunae.glk.controller.GLKController.glk_fileref_destroy;
import static com.luxlunae.glk.controller.GLKController.glk_put_string;
import static com.luxlunae.glk.controller.GLKController.glk_request_char_event;
import static com.luxlunae.glk.controller.GLKController.glk_request_line_event;
import static com.luxlunae.glk.controller.GLKController.glk_request_timer_events;
import static com.luxlunae.glk.controller.GLKController.glk_select;
import static com.luxlunae.glk.controller.GLKController.glk_select_poll;
import static com.luxlunae.glk.controller.GLKController.glk_set_window;
import static com.luxlunae.glk.controller.GLKController.glk_window_clear;
import static com.luxlunae.glk.controller.GLKController.glk_window_get_size;
import static com.luxlunae.glk.controller.GLKController.glk_window_open;
import static com.luxlunae.glk.controller.GLKController.glkplus_fileref_get_name;
import static com.luxlunae.glk.controller.GLKController.glkplus_set_html_mode;

public class MView {
    private static final boolean ENSURE_TEXT_READABLE = true;
    private static OUTPUT_TYPE outputType = OUTPUT_TYPE.HTML;
    private final int mMainWin;
    private final int mStatusWin;
    private final ByteBuffer inputBuf;
    public boolean mDisplaying = false; // In case any output is once only - don't want it to trigger when we're just testing the text
    public int mDebugIndent;
    @NonNull
    public StringBuilder mOutputText = new StringBuilder();
    public boolean mNoDebug;
    @Nullable
    private GLKModel mGLKModel;
    /**
     * If this is not NULL then debug playback mode is on.
     */
    @Nullable
    private StringBuilder sbTestOut;

    public MView(@NonNull GLKModel g) {
        mGLKModel = g;

        if (outputType == OUTPUT_TYPE.HTML) {
            glkplus_set_html_mode(mGLKModel, true);
        }

        mMainWin = glk_window_open(mGLKModel, GLKConstants.NULL, GLKConstants.NULL,
                GLKConstants.NULL, wintype_TextBuffer, GLKConstants.NULL);
        mStatusWin = glk_window_open(mGLKModel, mMainWin,
                winmethod_Above | winmethod_Fixed, 1,
                wintype_TextGrid, GLKConstants.NULL);
        inputBuf = ByteBuffer.allocateDirect(1000);
    }

    @Nullable
    public Context getContext() {
        if (mGLKModel != null) {
            return mGLKModel.getApplicationContext();
        }
        return null;
    }

    private static String getSpaces(int n) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < n; i++) {
            ret.append(" ");
        }
        return ret.toString();
    }

    public void displayError(@NonNull String msg) {
        debugPrint(Nothing, "",
                MView.DebugDetailLevelEnum.Error,
                "<i><c>*** Game error: " + msg + " ***</c></i>");
    }

    public void errMsg(@NonNull String msg) {
        errMsg(msg, null);
    }

    public void errMsg(@NonNull String msg, @Nullable Exception ex) {
        if (ex != null) {
            msg += ": " + ex.getMessage();
        }
        out("*** ADRIFT Error: " + msg + " ***\n");
        GLKLogger.error("*** ADRIFT Error: " + msg + " ***");
    }

    public void TODO(@NonNull String funcName) {
        funcName = funcName.equals("") ?
                "This section" : "Function \"" + funcName + "\"";
        out("TODO - " + funcName + " still has to be completed.\n");
        GLKLogger.error("TODO - " + funcName + " still has to be completed.\n");
    }

    public void setInputTimer(boolean on) {
        if (mGLKModel == null) {
            return;
        }
        glk_request_timer_events(mGLKModel, on ? 1000 : 0);
    }

    public void tick(@NonNull MAdventure adv) throws InterruptedException {
        if (mGLKModel == null) {
            return;
        }
        GLKEvent e = new GLKEvent();
        glk_select_poll(mGLKModel, e);
        if (e.type == evtype_Timer) {
            // process timer event
            adv.incrementTurnOrTime(TimeBased);
        }
    }

    public void quit() throws InterruptedException {
        assert mGLKModel != null;

        GLKModel m = mGLKModel;
        setInputTimer(false);
        mGLKModel = null;
        glk_exit(m);
    }

    public void die() {
        // similar to quit but no prompt
        setInputTimer(false);
        mGLKModel = null;
    }

    public boolean isStopping() {
        return (mGLKModel == null);
    }

    public String promptForSaveFileName(boolean create) throws InterruptedException {
        if (mGLKModel == null) {
            return "";
        }
        int fref = glk_fileref_create_by_prompt(mGLKModel,
                fileusage_SavedGame, create ? filemode_Write : filemode_Read, 0);
        if (fref == GLKConstants.NULL) {
            return "";
        }
        byte[] b = glkplus_fileref_get_name(mGLKModel, fref);
        glk_fileref_destroy(mGLKModel, fref);
        if (b == null) {
            GLKLogger.error("Bebek: could not get fileref name.");
            return "";
        }
        try {
            return new String(b, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            GLKLogger.error("Bebek: unsupported encoding: " + e.getMessage());
            return "";
        }
    }

    public char msgboxYesNo(@NonNull String prompt) {
        if (mGLKModel == null) {
            return 0;
        }
        outImmediate(prompt);
        try {
            return yesNo();
        } catch (InterruptedException ex) {
            return 0;
        }
    }

    public char yesNo() throws InterruptedException {
        if (mGLKModel == null) {
            return 0;
        }
        GLKEvent e = new GLKEvent();
        char ch = 0;
        while (ch != 'y' && ch != 'Y' && ch != 'n' && ch != 'N') {
            glk_request_char_event(mGLKModel, mMainWin);
            do {
                glk_select(mGLKModel, e);
            } while (e.type != evtype_CharInput);
            ch = (char) e.val1;
        }
        return ch;
    }

    public void clearTextWindow() {
        if (mGLKModel == null || sbTestOut != null) {
            return;
        }
        glk_window_clear(mGLKModel, mMainWin);
    }

    @Nullable
    public String getUserInput(@NonNull MAdventure adv, @Nullable String prompt,
                               @Nullable String defaultResp) throws InterruptedException {
        if (mGLKModel == null) {
            return defaultResp;
        }

        // Output the prompt (may just be a simple >)
        StringBuilder sb = new StringBuilder();
        if (prompt != null) {
            sb.append(prompt);
        }
        if (defaultResp != null && !defaultResp.equals("")) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("(default: '").append(defaultResp).append("') ");
        }
        sb.append("&gt; ");
        out(sb.toString());

        // Now get the user's input
        // While waiting, pass any timer events to the model
        GLKEvent e = new GLKEvent();
        glk_request_line_event(mGLKModel, mMainWin, inputBuf, 0, true);
        do {
            glk_select(mGLKModel, e);
            if (e.type == evtype_Timer) {
                // Process timer event
                adv.incrementTurnOrTime(TimeBased);
            }
        } while (e.type != evtype_LineInput);

        if (e.val1 > 0) {
            // User entered text
            byte[] b = new byte[e.val1 * 4];
            inputBuf.rewind();
            inputBuf.get(b);
            inputBuf.clear();
            return new String(b, Charset.forName("UTF-32LE"));
        } else {
            // User didn't enter text, just pressed enter key
            return defaultResp;
        }
    }

    public void outImmediate(String text) {
        if (mGLKModel == null) {
            return;
        }
        glk_set_window(mGLKModel, mMainWin);
        try {
            glk_put_string(mGLKModel, text.getBytes("UTF-32LE"), true);
            glk_request_timer_events(mGLKModel, 1);
            GLKEvent ev = new GLKEvent();
            glk_select(mGLKModel, ev);  // so the text is flushed
        } catch (UnsupportedEncodingException e) {
            GLKLogger.error("Could not put GLK string - unsupported encoding for ISO-8859-1");
        } catch (InterruptedException e) {
            // do nothing
        }
    }

    public void startDebugPlaybackMode(StringBuilder sb) {
        sbTestOut = sb;
    }

    public void stopDebugPlaybackMode() {
        sbTestOut = null;
    }

    public void out(String text) {
        if (mGLKModel == null) {
            return;
        }
        String outStr;
        switch (outputType) {
            default:
            case RAW:
                outStr = text;
                break;
            case HTML:
                outStr = text.replace("\n", "<br>")
                        .replaceAll("<!--(.*?)-->", "")
                        .replace("<>", "")
                        .replace("<centre>", "<center>")
                        .replace("</centre>", "</center>");
                if (ENSURE_TEXT_READABLE && mGLKModel.mBackgroundColor == Color.WHITE) {
                    // Some ADRIFT games assume background color is black
                    // Replace any such tags so that we use the user's TADS text color setting instead.
                    outStr = outStr.replace("<font color=white>", "<font oolor=text>");
                }
                break;
            case PLAIN_TEXT:
                outStr = stripCarats(text);
                break;
        }

        if (sbTestOut != null) {
            sbTestOut.append(outStr);
            return;
        }

        glk_set_window(mGLKModel, mMainWin);
        try {
            glk_put_string(mGLKModel, outStr.getBytes("UTF-32LE"), true);
        } catch (UnsupportedEncodingException e) {
            GLKLogger.error("Could not put GLK string - unsupported encoding for ISO-8859-1");
        }
    }

    public void updateStatusBar(@NonNull MAdventure adv) {
        String desc = "";
        String score = "";
        StringBuilder userStatus = new StringBuilder();

        if (adv.mGameState == Running) {
            MCharacter chPlayer = adv.getPlayer();
            if (chPlayer != null) {
                MCharacter.MCharacterLocation loc = chPlayer.getLocation();
                if (loc.getExistsWhere() == Hidden || loc.getLocationKey().equals("")) {
                    desc = "(Nowhere)";
                } else {
                    MLocation loc2 = adv.mLocations.get(loc.getLocationKey());
                    if (loc2 != null) {
                        desc = loc2.getShortDescriptionSafe();
                        if (loc.getExistsWhere() != AtLocation) {
                            desc += " (" + loc.toString() + ")";
                        }
                    }
                }
            }
            String status = adv.getUserStatus();
            if (status != null) {
                userStatus.append(status);
            }
        } else {
            desc = "Game Over";
        }

        if (adv.getMaxScore() > 0) {
            score = "Score: " + adv.getScore();
        }
        if (!adv.getEnabled(Score)) {
            score = "";
        }

        updateStatusBar(adv, desc, score, userStatus);
    }

    private void updateStatusBar(@NonNull MAdventure adv,
                                 @Nullable String desc, @Nullable String score,
                                 @Nullable StringBuilder userStatus) {
        if (mGLKModel == null) {
            return;
        }
        glk_set_window(mGLKModel, mStatusWin);
        glk_window_clear(mGLKModel, mStatusWin);

        Point sz = glk_window_get_size(mGLKModel, mStatusWin);

        int nPanels = (desc != null && desc.length() > 0 ? 1 : 0) +
                (score != null && score.length() > 0 ? 1 : 0) +
                (userStatus != null && userStatus.length() > 0 ? 1 : 0);
        if (nPanels == 0) {
            // nothing to display
            return;
        }
        int panelWidth = sz.x / nPanels;

        String status = "";

        if (desc != null && desc.length() > 0) {
            int w = desc.length() - panelWidth;
            status += (w > 0) ? desc.substring(0, panelWidth - 3) + "..." : desc;
            if (w < 0) {
                status += getSpaces(-w);
            }
        }

        if (score != null && score.length() > 0) {
            int w = score.length() - panelWidth;
            if (nPanels == 2) {
                status += getSpaces(-w);
                status += (w > 0) ? score.substring(0, panelWidth - 3) + "..." : score;
            } else {
                status += (w > 0) ? score.substring(0, panelWidth - 3) + "..." : score;
                status += getSpaces(-w);
            }
        }

        if (userStatus != null && userStatus.length() > 0) {
            adv.mALRs.evaluate(userStatus, adv.mReferences);
            int w = userStatus.length() - panelWidth;
            if (nPanels == 2) {
                status += getSpaces(-w);
                status += (w > 0) ? userStatus.substring(0, panelWidth - 3) + "..." : userStatus;
            } else {
                status += (w > 0) ? userStatus.substring(0, panelWidth - 3) + "..." : userStatus;
            }
        }

        try {
            glk_put_string(mGLKModel, status.getBytes("UTF-32LE"), true);
        } catch (UnsupportedEncodingException e) {
            GLKLogger.error("Could not put GLK string - unsupported encoding for ISO-8859-1");
        }
    }

    public void displayText(@NonNull MAdventure adv, @NonNull String text) {
        displayText(adv, text, false, true, true);
    }

    public void displayText(@NonNull MAdventure adv, @NonNull String text, boolean flush) {
        displayText(adv, text, flush, true, true);
    }

    public void displayText(@NonNull MAdventure adv, @NonNull String text, boolean flush,
                            boolean allowALRs, boolean record) {
        mDisplaying = true;

        boolean allowPSpace = true;

        if (adv.mVersion < 5) {
            // TCC: ensure that DisplayLocation is always preceded by a blank line
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
            adv.mALRs.evaluate(tmp, adv.mReferences);
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
            out(mOutputText.toString());
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

    private void underlineNouns(@NonNull StringBuilder text) {
        if (BEBEK_DEBUG_ENABLED) {
            GLKLogger.error("TODO: MUserSession: underlineNouns");
        }
    }

    public void debugPrint(MGlobals.ItemEnum itemType, @NonNull String key,
                           DebugDetailLevelEnum detailLevel, @NonNull String msg) {
        debugPrint(itemType, key, detailLevel, msg, true);
    }

    public void debugPrint(MGlobals.ItemEnum itemType, @NonNull String key,
                           DebugDetailLevelEnum detailLevel, @NonNull String msg,
                           boolean appendNewLine) {
        if (detailLevel == DebugDetailLevelEnum.Error) {
            GLKLogger.error("[" + itemType + (!key.equals("") ? (" " + key) : "") + "] " +
                    (msg.equals("") ? "(no output)" : msg) + (appendNewLine ? "\n" : ""));
        } else if (!mNoDebug && BEBEK_DEBUG_ENABLED) {
            GLKLogger.debug("[" + itemType + (!key.equals("") ? (" " + key) : "") + "] " +
                    (msg.equals("") ? "(no output)" : msg) + (appendNewLine ? "\n" : ""));
        }
    }

    private enum OUTPUT_TYPE {
        RAW,
        HTML,
        PLAIN_TEXT
    }

    public enum DebugDetailLevelEnum {
        Error,          // 0
        High,           // 1
        Medium,         // 2
        Low             // 3
    }
}
