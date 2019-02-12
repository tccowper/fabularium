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
package com.luxlunae.glk;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.fabularium.PreferencesActivity;

import java.io.File;
import java.io.IOException;

public final class GLKConstants {

    public static final int GLK_DEFAULT_TEXT_COLOUR = Color.parseColor("#212121");
    public static final int GLK_DEFAULT_BACK_COLOUR = Color.parseColor("#FFFFFF");
    public static final int GLK_DEFAULT_INPUT_COLOUR = Color.parseColor("#0277bd");
    public static final int EMPTY_SCREEN_COLOR = Color.BLACK;

    public static final String GLK_SAVE_EXT = ".glksave";
    public static final String GLK_DATA_EXT = ".glkdata";
    public static final String GLK_TRANSCRIPT_EXT = ".txt";
    public static final String GLK_INPUT_RECORD_EXT = ".txt";

    // HANDY UNICODE CONSTANTS
    public static final int LDQUOTE = 8220;
    public static final String LDQUOTE_STRING = "\u201C";
    public static final int RDQUOTE = 8221;
    public static final String RDQUOTE_STRING = "\u201D";
    public static final int LSQUOTE = 8216;
    public static final int RSQUOTE = 8217;
    public static final int EM_DASH = 8212;
    public static final int NULL = 0;
    public static final int gestalt_CharOutput_CannotPrint = 0;
    public static final int gestalt_CharOutput_ApproxPrint = 1;
    public static final int gestalt_Timer = 5;
    public static final int evtype_Timer = 1;
    public static final int evtype_CharInput = 2;
    public static final int evtype_LineInput = 3;
    public static final int evtype_Arrange = 5;
    public static final int evtype_Redraw = 6;
    public static final int evtype_SoundNotify = 7;
    public static final int evtype_VolumeNotify = 9;
    public static final int evtype_Debug = 10;
    public static final int keycode_Unknown = 0xffffffff;
    public static final int keycode_Left = 0xfffffffe;
    public static final int keycode_Right = 0xfffffffd;
    public static final int keycode_Up = 0xfffffffc;
    public static final int keycode_Down = 0xfffffffb;
    public static final int keycode_Return = 0xfffffffa;
    public static final int keycode_Delete = 0xfffffff9;
    public static final int keycode_Escape = 0xfffffff8;
    public static final int keycode_Tab = 0xfffffff7;
    public static final int keycode_PageUp = 0xfffffff6;
    public static final int keycode_PageDown = 0xfffffff5;
    public static final int keycode_Home = 0xfffffff4;
    public static final int keycode_End = 0xfffffff3;
    public static final int keycode_Func1 = 0xffffffef;
    public static final int keycode_Func2 = 0xffffffee;
    public static final int keycode_Func3 = 0xffffffed;
    public static final int keycode_Func4 = 0xffffffec;
    public static final int keycode_Func5 = 0xffffffeb;
    public static final int keycode_Func6 = 0xffffffea;
    public static final int keycode_Func7 = 0xffffffe9;
    public static final int keycode_Func8 = 0xffffffe8;
    public static final int keycode_Func9 = 0xffffffe7;
    public static final int keycode_Func10 = 0xffffffe6;
    public static final int keycode_Func11 = 0xffffffe5;
    public static final int keycode_Func12 = 0xffffffe4;
    public static final int FAB_DEBUG_KEY_SHOW_WINDOWS = 0xffffffe3;
    public static final int FAB_DEBUG_KEY_MEM_INFO = 0xffffffe2;
    public static final int FAB_DEBUG_KEY_SHOW_STREAMS = 0xffffffe1;
    public static final int FAB_DEBUG_KEY_SHOW_STYLES = 0xffffffe0;
    public static final int FAB_KEY_NEXT = 0xffffffdf;
    public static final int FAB_KEY_DEBUG = 0xffffffde;
    public static final int keycode_MAXVAL = 28;
    public static final int style_Normal = 0;
    public static final int style_Emphasized = 1;
    public static final int style_Preformatted = 2;
    public static final int style_Header = 3;
    public static final int style_Subheader = 4;
    public static final int style_Alert = 5;
    public static final int style_Note = 6;
    public static final int style_BlockQuote = 7;
    public static final int style_Input = 8;
    public static final int style_User1 = 9;
    public static final int style_User2 = 10;
    public static final int style_NUMSTYLES = 11;
    public static final int wintype_Blank = 2;
    public static final int wintype_TextBuffer = 3;
    public static final int wintype_TextGrid = 4;
    public static final int winmethod_Left = 0x00;
    public static final int winmethod_Right = 0x01;
    public static final int winmethod_Above = 0x02;
    public static final int winmethod_Below = 0x03;
    public static final int winmethod_DirMask = 0x0f;
    public static final int winmethod_Fixed = 0x10;
    public static final int winmethod_Proportional = 0x20;
    public static final int winmethod_DivisionMask = 0xf0;
    public static final int winmethod_Border = 0x000;
    public static final int winmethod_NoBorder = 0x100;
    public static final int winmethod_BorderMask = 0x100;
    public static final int fileusage_Data = 0x00;
    public static final int fileusage_SavedGame = 0x01;
    public static final int fileusage_Transcript = 0x02;
    public static final int fileusage_InputRecord = 0x03;
    public static final int fileusage_TypeMask = 0x0f;
    public static final int fileusage_TextMode = 0x100;
    public static final int fileusage_BinaryMode = 0x000;
    public static final int filemode_Write = 0x01;
    public static final int filemode_Read = 0x02;
    public static final int filemode_ReadWrite = 0x03;
    public static final int filemode_WriteAppend = 0x05;
    public static final int seekmode_Start = 0;
    public static final int seekmode_Current = 1;
    public static final int seekmode_End = 2;
    public static final int stylehint_Indentation = 0;
    public static final int stylehint_ParaIndentation = 1;
    public static final int stylehint_Justification = 2;
    public static final int stylehint_Size = 3;
    public static final int stylehint_Weight = 4;
    public static final int stylehint_Oblique = 5;
    public static final int stylehint_Proportional = 6;
    public static final int stylehint_TextColor = 7;
    public static final int stylehint_BackColor = 8;
    public static final int stylehint_ReverseColor = 9;
    public static final int stylehint_NUMHINTS = 10;
    public static final int stylehint_just_LeftFlush = 0;
    public static final int stylehint_just_LeftRight = 1;
    public static final int stylehint_just_Centered = 2;
    public static final int stylehint_just_RightFlush = 3;
    public static final int imagealign_InlineUp = 0x01;
    public static final int imagealign_InlineDown = 0x02;
    public static final int imagealign_InlineCenter = 0x03;
    public static final int imagealign_MarginLeft = 0x04;
    public static final int imagealign_MarginRight = 0x05;
    // Additional GARGLK constants
    public static final int zcolor_Transparent = -4;
    public static final int zcolor_Cursor = -3;
    public static final int zcolor_Current = -2;
    public static final int zcolor_Default = -1;
    // banner alignment types
    public static final int TADS_BANNER_ALIGN_TOP = 0;
    public static final int TADS_BANNER_ALIGN_BOTTOM = 1;
    public static final int TADS_BANNER_ALIGN_LEFT = 2;
    public static final int TADS_BANNER_ALIGN_RIGHT = 3;
    // size units
    public static final int TADS_BANNER_SIZE_PCT = 1;
    public static final int TADS_BANNER_SIZE_ABS = 2;
    // The banner has a visible border; this indicates that a line is to be
    // drawn to separate the banner from the adjacent window or windows
    // "inside" the banner.  So, a top-aligned banner will have its border
    // drawn along its bottom edge; a left-aligned banner will show a border
    // along its right edge; and so forth.
    //
    //  Note that character-mode platforms generally do NOT respect the border
    //  style, since doing so takes up too much screen space.
    public static final int TADS_BANNER_STYLE_BORDER = 0x00000001;
    public static final int wintype_Pair = 1;
    public static final int wintype_Graphics = 5;
    public static final int gestalt_Version = 0;
    public static final int gestalt_CharInput = 1;
    public static final int gestalt_LineInput = 2;
    public static final int gestalt_CharOutput = 3;
    public static final int gestalt_CharOutput_ExactPrint = 2;
    public static final int gestalt_MouseInput = 4;
    public static final int gestalt_Graphics = 6;
    public static final int gestalt_DrawImage = 7;
    public static final int gestalt_Sound = 8;
    public static final int gestalt_SoundVolume = 9;
    public static final int gestalt_SoundNotify = 10;
    public static final int gestalt_Hyperlinks = 11;
    public static final int gestalt_HyperlinkInput = 12;
    public static final int gestalt_SoundMusic = 13;
    public static final int gestalt_GraphicsTransparency = 14;
    public static final int gestalt_Unicode = 15;
    public static final int gestalt_UnicodeNorm = 16;
    public static final int gestalt_LineInputEcho = 17;
    public static final int gestalt_LineTerminators = 18;
    public static final int gestalt_LineTerminatorKey = 19;
    public static final int gestalt_DateTime = 20;
    public static final int gestalt_Sound2 = 21;
    public static final int gestalt_ResourceStream = 22;
    public static final int gestalt_GraphicsCharInput = 23;
    public static final int gestalt_HTML = 99;  // fabularium-specific addition
    public static final int evtype_None = 0;
    public static final int evtype_MouseInput = 4;
    public static final int evtype_Hyperlink = 8;
    public static final int wintype_AllTypes = 0;
    // insertion positions
    public static final int TADS_BANNER_FIRST = 1;
    // banner types
    public static final int TADS_BANNER_TYPE_TEXT = 1;
    public static final int TADS_BANNER_TYPE_TEXTGRID = 2;
    public static final int TADS_BANNER_TYPE_STATUS = 99; // fabularium-specific addition
    // Error and type codes
    public static final int giblorb_err_None = 0;
    public static final int giblorb_err_CompileTime = 1;
    public static final int giblorb_err_Alloc = 2;
    public static final int giblorb_err_Read = 3;
    public static final int giblorb_err_NotAMap = 4;
    // Additional TADS BANNER constants
    public static final int giblorb_err_Format = 5;
    public static final int giblorb_err_NotFound = 6;
    // Methods for loading a chunk
    public static final int bb_method_DontLoad = 0;
    public static final int bb_method_Memory = 1;
    public static final int bb_method_FilePos = 2;
    // Four-byte constants
    public static final int bb_ID_Exec = GLKUtils.ubyteToID32('E', 'x', 'e', 'c');
    public static final int bb_ID_Snd = GLKUtils.ubyteToID32('S', 'n', 'd', ' ');
    public static final int bb_ID_Pict = GLKUtils.ubyteToID32('P', 'i', 'c', 't');
    public static final int bb_ID_Data = GLKUtils.ubyteToID32('D', 'a', 't', 'a');
    public static final int bb_ID_Copyright = GLKUtils.ubyteToID32('(', 'c', ')', ' ');
    public static final int bb_ID_AUTH = GLKUtils.ubyteToID32('A', 'U', 'T', 'H');
    public static final int bb_ID_ANNO = GLKUtils.ubyteToID32('A', 'N', 'N', 'O');
    public static final int bb_ID_TEXT = GLKUtils.ubyteToID32('T', 'E', 'X', 'T');
    public static final int bb_ID_BINA = GLKUtils.ubyteToID32('B', 'I', 'N', 'A');
    public static final int TADS_BANNER_LAST = 2;
    public static final int TADS_BANNER_BEFORE = 3;
    public static final int TADS_BANNER_AFTER = 4;
    // The banner has a vertical/horizontal scrollbar.  Character-mode
    // platforms generally do not support scrollbars.
    public static final int TADS_BANNER_STYLE_VSCROLL = 0x00000002;
    public static final int TADS_BANNER_STYLE_HSCROLL = 0x00000004;
    // Default paths
    private static final String DEFAULT_APP_DIR = Environment.getExternalStorageDirectory().getPath() + "/Fabularium/";
    private static final String DEFAULT_GAME_SUBDIR = "Games/";
    private static final String DEFAULT_GAME_DATA_SUBDIR = "GameData/";
    private static final String DEFAULT_PROJECTS_SUBDIR = "Projects/";
    private static final String DEFAULT_LIB_SUBDIR = "Lib/";
    private static final String DEFAULT_INCLUDE_SUBDIR = "Include/";
    private static final String DEFAULT_FONTS_SUBDIR = "Fonts/";
    // inform paths
    private static final String INFORM_LIB_SUBDIR = "Inform/";
    private static final String INFORM_INCLUDE_SUBDIR = "Inform/";
    // tads3 paths
    private static final String TADS3_LIB_SUBDIR = "Tads3/";
    private static final String TADS3_INCLUDE_SUBDIR = "Tads3/";
    // adrift paths
    private static final String ADRIFT_LIB_SUBDIR = "Adrift/";

    // BLORB CONSTANTS
    // Automatically scroll the banner vertically/horizontally whenever new
    // text is displayed in the window.  In other words, whenever
    // os_banner_disp() is called, scroll the window so that the text that the
    // new cursor position after the new text is displayed is visible in the
    // window.
    //
    // Note that this style is independent of the presence of scrollbars.
    // Even if there are no scrollbars, we can still scroll the window's
    // contents programmatically.
    //
    // Implementations can, if desired, keep an internal buffer of the
    // window's contents, so that the contents can be recalled via the
    // scrollbars if the text displayed in the banner exceeds the space
    // available in the banner's window on the screen.  If the implementation
    // does keep such a buffer, we recommend the following method for managing
    // this buffer.  If the AUTO_VSCROLL flag is not set, then the banner's
    // contents should be truncated at the bottom when the contents overflow
    // the buffer; that is, once the banner's internal buffer is full, any new
    // text that the calling program attempts to add to the banner should
    // simply be discarded.  If the AUTO_VSCROLL flag is set, then the OLDEST
    // text should be discarded instead, so that the most recent text is
    // always retained.
    private static final int TADS_BANNER_STYLE_AUTO_VSCROLL = 0x00000008;
    private static final int TADS_BANNER_STYLE_AUTO_HSCROLL = 0x00000010;
    // Tab-based alignment is required/supported.  On creation, this is a hint
    // to the implementation that is sometimes necessary to determine what
    // kind of font to use in the new window, for non-HTML platforms.  If this
    // flag is set on creation, the caller is indicating that it wants to use
    // <TAB> tags to align text in the window.
    //
    // ...
    //
    // Full HTML TADS implementations can also ignore this.  HTML TADS
    // implementations always have full <TAB> support via the HTML
    // parser/renderer.
    private static final int TADS_BANNER_STYLE_TAB_ALIGN = 0x00000020;
    // Use "MORE" mode in this window.  By default, a banner window should
    // happily allow text to overflow the vertical limits of the window; the
    // only special thing that should happen on overflow is that the window
    // should be srolled down to show the latest text, if the auto-vscroll
    // style is set.  With this flag, though, a banner window acts just like
    // the main text window: when the window fills up vertically, we show a
    // MORE prompt (using appropriate system conventions), and wait for the
    // user to indicate that they're ready to see more text.  On most systems,
    // the user acknowledges a MORE prompt by pressing a key or scrolling with
    // the mouse, but it's up to the system implementor to decide what's
    // appropriate for the system.
    private static final int TADS_BANNER_STYLE_MOREMODE = 0x00000040;
    // This banner is a horizontal/vertical "strut" for sizing purposes.  This
    // means that the banner's content size is taken into account when figuring
    // the content size of its *parent* banner.  If the banner has the same
    // orientation as the parent, its content size is added to its parent's
    // internal content size to determine the parent's overall content size.
    // If the banner's orientation is orthogonal to the parent's, then the
    // parent's overall content size is the larger of the parent's internal
    // content size and this banner's content size.
    private static final int TADS_BANNER_STYLE_HSTRUT = 0x00000080;
    private static final int TADS_BANNER_STYLE_VSTRUT = 0x00000100;

    /**
     * Returns a Fabularium path, based on defaults and user preferences.
     *
     * @param c      - application context
     * @param subdir - the type of sub-directory you want (or null if you want the application directory).
     * @return the path, which will always have a trailing '/'
     */
    @NonNull
    public static String getDir(@NonNull Context c, @Nullable SUBDIR subdir) throws IOException, SecurityException {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(c);
        String path;

        // Construct the directory path
        if (subdir == null) {
            path = sharedPref.getString(PreferencesActivity.KEY_PREF_APP_PATH, "");
            if (path.equals("")) {
                // Default path
                path = DEFAULT_APP_DIR;

                // If the directory doesn't already exist, try to create it
                if (!GLKUtils.makeDir(path)) {
                    throw new IOException();
                }
            } else {
                // User override
                if (!path.endsWith("/")) {
                    path = path.concat("/");
                }

                // If the directory doesn't already exist, throw an exception
                // We are more conservative for user overrides - don't automatically
                // create any directories - let user do that!!
                File f = new File(path);
                if (!f.exists()) {
                    throw new IOException();
                }
            }
        } else {
            String arg = null;
            String defSubDir = null;
            switch (subdir) {
                case GAME:
                    arg = PreferencesActivity.KEY_PREF_GAME_PATH;
                    defSubDir = DEFAULT_GAME_SUBDIR;
                    break;
                case GAMEDATA:
                    arg = PreferencesActivity.KEY_PREF_GAME_DATA_PATH;
                    defSubDir = DEFAULT_GAME_DATA_SUBDIR;
                    break;
                case PROJECTS:
                    arg = PreferencesActivity.KEY_PREF_PROJECTS_PATH;
                    defSubDir = DEFAULT_PROJECTS_SUBDIR;
                    break;
                case LIB:
                    arg = PreferencesActivity.KEY_PREF_LIB_PATH;
                    defSubDir = DEFAULT_LIB_SUBDIR;
                    break;
                case LIB_INFORM:
                    return getDir(c, SUBDIR.LIB) + INFORM_LIB_SUBDIR;
                case LIB_TADS3:
                    return getDir(c, SUBDIR.LIB) + TADS3_LIB_SUBDIR;
                case LIB_ADRIFT:
                    return getDir(c, SUBDIR.LIB) + ADRIFT_LIB_SUBDIR;
                case INCLUDE:
                    arg = PreferencesActivity.KEY_PREF_INCLUDE_PATH;
                    defSubDir = DEFAULT_INCLUDE_SUBDIR;
                    break;
                case INCLUDE_INFORM:
                    return getDir(c, SUBDIR.INCLUDE) + INFORM_INCLUDE_SUBDIR;
                case INCLUDE_TADS3:
                    return getDir(c, SUBDIR.INCLUDE) + TADS3_INCLUDE_SUBDIR;
                case FONTS:
                    arg = PreferencesActivity.KEY_PREF_FONTS_PATH;
                    defSubDir = DEFAULT_FONTS_SUBDIR;
                    break;
            }
            path = sharedPref.getString(arg, "");
            if (path.equals("")) {
                // Default path
                path = getDir(c, null) + defSubDir;

                // If the directory doesn't already exist, try to create it
                if (!GLKUtils.makeDir(path)) {
                    throw new IOException();
                }
            } else {
                // User override
                if (!path.endsWith("/")) {
                    path = path.concat("/");
                }

                // If the directory doesn't already exist, throw an exception
                // We are more conservative for user overrides - don't automatically
                // create any directories - let user do that!!
                File f = new File(path);
                if (!f.exists()) {
                    throw new IOException();
                }
            }
        }

        return path;
    }

    public static String gestaltToString(int c) {
        String[] ret = {"<gestalt_Version>",
                "<gestalt_CharInput>",
                "<gestalt_LineInput>",
                "<gestalt_CharOutput>",
                "<gestalt_MouseInput>",
                "<gestalt_Timer>",
                "<gestalt_Graphics>",
                "<gestalt_DrawImage>",
                "<gestalt_Sound>",
                "<gestalt_SoundVolume>",
                "<gestalt_SoundNotify>",
                "<gestalt_Hyperlinks>",
                "<gestalt_HyperlinkInput>",
                "<gestalt_SoundMusic>",
                "<gestalt_GraphicsTransparency>",
                "<gestalt_Unicode>",
                "<gestalt_UnicodeNorm>",
                "<gestalt_LineInputEcho>",
                "<gestalt_LineTerminators>",
                "<gestalt_LineTerminatorKey>",
                "<gestalt_DateTime>",
                "<gestalt_Sound2>",
                "<gestalt_ResourceStream"};

        if (c == 99) {
            return "<gestalt_HTML>";
        } else if (c >= ret.length || c < 0) {
            return "<unrecognised gestalt constant>";
        } else {
            return ret[c];
        }
    }

    public static String styleToString(int c) {
        String[] ret = {"<style_Normal>",
                "<style_Emphasized>",
                "<style_Preformatted>",
                "<style_Header>",
                "<style_Subheader>",
                "<style_Alert>",
                "<style_Note>",
                "<style_BlockQuote>",
                "<style_Input>",
                "<style_User1>",
                "<style_User2>",
                "<style_User3>",
                "<style_Status>",
                "<style_Symbol>"};

        if (c >= ret.length || c < 0) {
            return "<unrecognised style constant>";
        } else {
            return ret[c];
        }
    }

    public static String wintypeToString(int c) {
        String[] ret = {"<wintype_AllTypes>",
                "<wintype_Pair>",
                "<wintype_Blank>",
                "<wintype_TextBuffer>",
                "<wintype_TextGrid>",
                "<wintype_Graphics>"};

        if (c >= ret.length || c < 0) {
            return "<unrecognised wintype constant>";
        } else {
            return ret[c];
        }
    }

    @NonNull
    public static String winmethodToString(int c) {
        String ret = "";

        int splitDirection = (c & GLKConstants.winmethod_DirMask);
        switch (splitDirection) {
            case winmethod_Left:
                ret += "<Left | ";
                break;
            case winmethod_Right:
                ret += "<Right | ";
                break;
            case winmethod_Above:
                ret += "<Above | ";
                break;
            case winmethod_Below:
                ret += "<Below | ";
                break;
            default:
                ret += "<unknown splitDir | ";
                break;
        }

        int splitType = (c & GLKConstants.winmethod_DivisionMask);
        switch (splitType) {
            case winmethod_Fixed:
                ret += "Fixed>";
                break;
            case winmethod_Proportional:
                ret += "Proportional>";
                break;
            default:
                ret += "unknown splitType>";
                break;
        }

        return ret;
    }

    @NonNull
    public static String fileusageToString(int c) {
        String ret = "";

        int fileType = (c & GLKConstants.fileusage_TypeMask);
        switch (fileType) {
            case fileusage_Data:
                ret += "<fileusage_Data | ";
                break;
            case fileusage_SavedGame:
                ret += "<fileusage_SavedGame | ";
                break;
            case fileusage_Transcript:
                ret += "<fileusage_Transcript | ";
                break;
            case fileusage_InputRecord:
                ret += "<fileusage_InputRecord | ";
                break;
            default:
                ret += "<unrecognised fileusage type | ";
                break;
        }

        if ((c & GLKConstants.fileusage_TextMode) != 0) {
            ret += "fileusage_TextMode>";
        } else {
            ret += "fileusage_BinaryMode>";
        }

        return ret;
    }

    public static String filemodeToString(int c) {
        switch (c) {
            case filemode_Write:
                return "<filemode_Write>";
            case filemode_Read:
                return "<filemode_Read>";
            case filemode_ReadWrite:
                return "<filemode_ReadWrite>";
            case filemode_WriteAppend:
                return "<filemode_WriteAppend>";
            default:
                return "<unrecognised filemode constant>";
        }
    }

    public static String stylehintToString(int c) {
        String[] ret = {"<stylehint_Indentation>",
                "<stylehint_ParaIndentation>",
                "<stylehint_Justification>",
                "<stylehint_Size>",
                "<stylehint_Weight>",
                "<stylehint_Oblique>",
                "<stylehint_Proportional>",
                "<stylehint_TextColor>",
                "<stylehint_BackColor>",
                "<stylehint_ReverseColor>"};

        if (c >= ret.length || c < 0) {
            return "<unrecognised stylehint constant>";
        } else {
            return ret[c];
        }
    }

    public static String zcolorToString(int c) {
        switch (c) {
            case zcolor_Transparent:
                return "<zcolor_Transparent>";
            case zcolor_Cursor:
                return "<zcolor_Cursor>";
            case zcolor_Default:
                return "<zcolor_Default>";
            default:
                return String.valueOf(c);
        }
    }

    public static String tadsBannerPosToString(int c) {
        switch (c) {
            case TADS_BANNER_FIRST:
                return "<TADS_BANNER_FIRST>";
            case TADS_BANNER_LAST:
                return "<TADS_BANNER_LAST>";
            case TADS_BANNER_BEFORE:
                return "<TADS_BANNER_BEFORE>";
            case TADS_BANNER_AFTER:
                return "<TADS_BANNER_AFTER>";
            default:
                return "<UNRECOGNISED BANNER POSITION>";
        }
    }

    public static String tadsBannerTypeToString(int c) {
        switch (c) {
            case TADS_BANNER_TYPE_TEXT:
                return "<TADS_BANNER_TYPE_TEXT>";
            case TADS_BANNER_TYPE_TEXTGRID:
                return "<TADS_BANNER_TYPE_TEXTGRID>";
            default:
                return "<UNRECOGNISED BANNER TYPE>";
        }
    }

    public static String tadsBannerAlignmentToString(int c) {
        switch (c) {
            case TADS_BANNER_ALIGN_TOP:
                return "<TADS_BANNER_ALIGN_TOP>";
            case TADS_BANNER_ALIGN_BOTTOM:
                return "<TADS_BANNER_ALIGN_BOTTOM>";
            case TADS_BANNER_ALIGN_LEFT:
                return "<TADS_BANNER_ALIGN_LEFT>";
            case TADS_BANNER_ALIGN_RIGHT:
                return "<TADS_BANNER_ALIGN_RIGHT>";
            default:
                return "<UNRECOGNISED BANNER ALIGN>";
        }
    }

    public static String tadsBannerSizeUnitToString(int c) {
        switch (c) {
            case TADS_BANNER_SIZE_PCT:
                return "<TADS_BANNER_SIZE_PCT>";
            case TADS_BANNER_SIZE_ABS:
                return "<TADS_BANNER_SIZE_ABS>";
            default:
                return "<UNRECOGNISED BANNER SIZE UNIT>";
        }
    }

    public static String tadsBannerStyleToString(long c) {
        StringBuilder sb = new StringBuilder();

        sb.append("<");
        if ((c & TADS_BANNER_STYLE_BORDER) != 0) {
            sb.append(" | TADS_BANNER_STYLE_BORDER ");
        }
        if ((c & TADS_BANNER_STYLE_VSCROLL) != 0) {
            sb.append(" | TADS_BANNER_STYLE_VSCROLL ");
        }
        if ((c & TADS_BANNER_STYLE_HSCROLL) != 0) {
            sb.append(" | TADS_BANNER_STYLE_HSCROLL ");
        }
        if ((c & TADS_BANNER_STYLE_AUTO_VSCROLL) != 0) {
            sb.append(" | TADS_BANNER_STYLE_AUTO_VSCROLL ");
        }
        if ((c & TADS_BANNER_STYLE_AUTO_HSCROLL) != 0) {
            sb.append(" | TADS_BANNER_STYLE_AUTO_HSCROLL ");
        }
        if ((c & TADS_BANNER_STYLE_TAB_ALIGN) != 0) {
            sb.append(" | TADS_BANNER_STYLE_TAB_ALIGN ");
        }
        if ((c & TADS_BANNER_STYLE_MOREMODE) != 0) {
            sb.append(" | TADS_BANNER_STYLE_MOREMODE ");
        }
        if ((c & TADS_BANNER_STYLE_HSTRUT) != 0) {
            sb.append(" | TADS_BANNER_STYLE_HSTRUT ");
        }
        if ((c & TADS_BANNER_STYLE_VSTRUT) != 0) {
            sb.append(" | TADS_BANNER_STYLE_VSTRUT ");
        }
        sb.append(">");
        return sb.toString();
    }

    public enum SUBDIR {GAME, GAMEDATA, PROJECTS, LIB, LIB_INFORM, LIB_TADS3, INCLUDE, INCLUDE_INFORM, INCLUDE_TADS3, FONTS, LIB_ADRIFT }
}
