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
package com.luxlunae.glk.controller;

import android.content.pm.ApplicationInfo;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.os.Process;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;

import com.luxlunae.bebek.MBebek;
import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKActivity;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.model.GLKDate;
import com.luxlunae.glk.model.GLKDrawable;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.GLKTime;
import com.luxlunae.glk.model.TADSBannerInfo;
import com.luxlunae.glk.model.stream.GLKInputStream;
import com.luxlunae.glk.model.stream.GLKOutputStream;
import com.luxlunae.glk.model.stream.GLKStream;
import com.luxlunae.glk.model.stream.file.GLKFileRef;
import com.luxlunae.glk.model.stream.file.GLKFileStream;
import com.luxlunae.glk.model.stream.file.GLKMemoryStream;
import com.luxlunae.glk.model.stream.sound.GLKSoundStream;
import com.luxlunae.glk.model.stream.window.GLKGraphicsM;
import com.luxlunae.glk.model.stream.window.GLKNonPairM;
import com.luxlunae.glk.model.stream.window.GLKPairM;
import com.luxlunae.glk.model.stream.window.GLKTextBufferM;
import com.luxlunae.glk.model.stream.window.GLKTextGridM;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;
import com.luxlunae.glk.model.stream.window.GLKWindowM;
import com.luxlunae.glk.model.style.GLKStyleSpan;
import com.luxlunae.glk.view.window.GLKScreen;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * This class represents the Controller component in the Model-View-Controller (MVC) design pattern
 * used by Fabularium's GLK implementation.
 * <p>
 * The GLKController manipulates the GLKModel and is used by the User. It always runs on the worker
 * thread, not the UI thread.
 * <p>
 * Manipulates model:
 * ------------------
 * <p>
 * When the terp has been started via JNI, it essentially acts as the Controller in the MVC design pattern.
 * It calls the operations in this class to modify / manipulate the GLKModel.
 * These operations are all static; the terp must provide a reference to the GLKModel it intends to modify when calling a function.
 * <p>
 * Used by user:
 * -------------
 * <p>
 * The User may generate events on the UI thread (e.g. a mouse click). Those events are sent to this Controller via the thead-safe
 * static function postEvent(). The Controller only retrieves and processes these events when the glk_select or glk_select_poll
 * functions are called.
 * <p>
 * Create and use the GLKController as follows:
 * <p>
 * <ol>
 * <li>Create and initialise a GLKModel representing the initial GLK state of the virtual machine.</li>
 * <li>Create a new Thread, encapsulating the GLKController, by calling GLKController.create(), passing in the GLKModel created in step 1.</li>
 * <li>Do any other initialisation, then call run() on the Thread created in step 2, to start the terp.</li>
 * </ol>
 */
@Keep
public final class GLKController {

    // DEBUG FLAGS
    private static final boolean DEBUG_GLK_CALLS = false;
    private static final boolean DEBUG_GLK_OUTPUT_CALLS = false;
    private static final boolean DEBUG_GLK_WINDOWS = false;
    private static final boolean DEBUG_GLKPLUS_CALLS = false;
    private static final boolean DEBUG_TADSBAN_CALLS = false;
    private static final boolean DEBUG_FORCE_GESTALT = false;       // if set, forces library to always answer yes to any gestalt call
    private static final boolean DEBUG_CONSTANT_DATE = false;       // if set, forces library to always return the UNIX epoch date

    private static final int MAX_BUFFERED_EVENTS = 50;              // queue can hold up to 50 events before it starts dropping them

    private static final ArrayBlockingQueue<GLKEvent> mEventQueue =
            new ArrayBlockingQueue<>(MAX_BUFFERED_EVENTS);

    private GLKController() {
        // prevent instantiation
        // rather than creating instances of this class, you should
        // create a new thread object via a call to create().
    }

    @SuppressWarnings("JniMissingFunction")
    private static native int runTerp(@NonNull String terpLibName, @NonNull GLKModel m, @NonNull String[] args, @Nullable String outfilePath);

    @SuppressWarnings("JniMissingFunction")
    private static native void stopTerp(int code);

    @SuppressWarnings("JniMissingFunction")
    public static native void freeByteBuffer(@NonNull ByteBuffer b);

    private static void stopTerpJava(GLKModel m, int code) {

    }

    /**
     * To run a GLK-enabled terp, do the following steps:
     * <p>
     * (1) create a GLKModel (this is an object that tracks the state of GLK objects)
     * (2) call this function, passing in the model.
     *
     * @param m - the model that will track GLK state information
     *          <p>
     *          Returns a Thread object ready for use; simply call run() when you wish to
     *          start it.
     */
    public static Thread create(@NonNull GLKModel m, @NonNull GLKActivity a) {
        return new Thread(new RunnableTerp(m, a));
    }

    /**
     * Interpreters should consider calling glk_tick periodically to yield time to the
     * library to undertake other tasks - e.g. yield time to other applications in a
     * cooperative-multitasking OS, or to check for player interrupts in an infinite loop.
     */
    public static void glk_tick(@NonNull GLKModel m) {
        // do nothing
    }

    /**
     * Convert the given character to lower case.
     *
     * @param ch - the character.
     * @return a lowercase version of the character.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static char glk_char_to_lower(@NonNull GLKModel m, char ch) {
        return Character.toLowerCase(ch);
    }

    /**
     * Convert the given character to upper case.
     *
     * @param ch - the character.
     * @return an uppercase version of the character.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static char glk_char_to_upper(@NonNull GLKModel m, char ch) {
        return Character.toUpperCase(ch);
    }

    /**
     * The current Unix time is stored in the structure. (The argument may not be
     * NULL.) This is the number of seconds since the beginning of 1970 (UTC).
     *
     * @param time - structure to fill in.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_current_time(@NonNull GLKModel m, @NonNull GLKTime time) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_current_time");
        }
        time.setToNow();
        if (DEBUG_CONSTANT_DATE) {
            time.low_sec = 0;
            time.microsec = 0;
        }
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + time.high_sec + ";" + time.low_sec + ";" + time.microsec);
        }
    }

    /**
     * If dealing with 64-bit values is awkward, you can also get the current time
     * as a lower-resolution 32-bit value.
     * <p>
     * This is simply the Unix time divided by the factor argument (which must
     * not be zero). For example, if factor is 60, the result will be the number
     * of minutes since 1970 (rounded towards negative infinity). If factor is 1,
     * you will get the Unix time directly, but the value will be truncated starting
     * some time in 2038.
     *
     * @param factor - factor argument.
     * @return simple timestamp divided by factor.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_current_simple_time(@NonNull GLKModel m, long factor) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_current_simple_time (factor = " + factor + ")");
        }
        int t = (int) Math.floor(System.currentTimeMillis() / (1000D * factor));
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + t);
        }
        return t;
    }

    /**
     * Convert the given timestamp (as returned by glk_current_time()) to a
     * broken-out structure.
     *
     * @param time - [input] timestamp from glk_current_time.
     * @param date - [output] date and time in universal time (GMT).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_time_to_date_utc(@NonNull GLKModel m, @NonNull GLKTime time, @NonNull GLKDate date) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_time_to_date_utc (time = " + time.toString() + ")");
        }
        long msecs = (((long) time.high_sec << 32) | time.low_sec) * 1000L + (time.microsec / 1000L);
        date.setTime(msecs, TimeZone.getTimeZone("UTC"));
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + date.toString());
        }
    }

    /**
     * Convert the given timestamp (as returned by glk_current_time()) to a
     * broken-out structure.
     *
     * @param time - [input] timestamp from glk_current_time.
     * @param date - [output] date and time in local time.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_time_to_date_local(@NonNull GLKModel m, @NonNull GLKTime time, @NonNull GLKDate date) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_time_to_date_local (time = " + time.toString() + ")");
        }
        long msecs = (((long) time.high_sec << 32) | time.low_sec) * 1000L + (time.microsec / 1000L);
        date.setTime(msecs, TimeZone.getDefault());
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + date.toString());
        }
    }

    /**
     * Convert the given timestamp (as returned by glk_current_simple_time()) to
     * a broken-out structure.
     *
     * @param time   - [input] simple timestamp from glk_current_simple_time.
     * @param factor - time argument is multiplied by this factor to produce a Unix timestamp.
     * @param date   - [output] date and time in universal time (GMT).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_simple_time_to_date_utc(@NonNull GLKModel m, int time, long factor, @NonNull GLKDate date) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_simple_time_to_date_utc (time = " + time + ", factor = " + factor + ")");
        }
        long msecs = time * factor * 1000L;
        date.setTime(msecs, TimeZone.getTimeZone("UTC"));
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + date.toString());
        }
    }

    /**
     * Convert the given timestamp (as returned by glk_current_simple_time()) to
     * a broken-out structure.
     *
     * @param time   - [input] simple timestamp from glk_current_simple_time.
     * @param factor - time argument is multiplied by this factor to produce a Unix timestamp.
     * @param date   - [output] date and time in local time.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_simple_time_to_date_local(@NonNull GLKModel m, int time, long factor, @NonNull GLKDate date) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_simple_time_to_date_local (time = " + time + ", factor = " + factor + ")");
        }
        long msecs = time * factor * 1000L;
        date.setTime(msecs, TimeZone.getDefault());
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + date.toString());
        }
    }

    /**
     * Convert the broken-out structure (interpreted as universal or local time)
     * to a timestamp. The weekday value in glkdate_t is ignored. The other values
     * need not be in their normal ranges; they will be normalized.
     * <p/>
     * If the time cannot be represented by the platform's time library, this may
     * return -1 for the seconds value. (I.e., the high_sec and low_sec fields
     * both $FFFFFFFF. The microseconds field is undefined in this case.)
     *
     * @param date - [input] date and time in universal time [GMT]
     * @param time - [output] timestamp.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_date_to_time_utc(@NonNull GLKModel m, @NonNull GLKDate date, @NonNull GLKTime time) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_date_to_time_utc (date = " + date.toString() + ")");
        }
        date.getTime(time, TimeZone.getTimeZone("UTC"));
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + time.toString());
        }
    }

    /**
     * Convert the broken-out structure (interpreted as universal or local time)
     * to a timestamp. The weekday value in glkdate_t is ignored. The other values
     * need not be in their normal ranges; they will be normalized.
     * <p/>
     * If the time cannot be represented by the platform's time library, this may
     * return -1 for the seconds value. (I.e., the high_sec and low_sec fields
     * both $FFFFFFFF. The microseconds field is undefined in this case.)
     *
     * @param date - [input] date and time in local time.
     * @param time - [output] timestamp.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_date_to_time_local(@NonNull GLKModel m, @NonNull GLKDate date, @NonNull GLKTime time) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_date_to_time_local (date = " + date.toString() + ")");
        }
        date.getTime(time, TimeZone.getDefault());
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + time.toString());
        }
    }

    /**
     * Convert the broken-out structure (interpreted as universal or local time)
     * to a timestamp divided by factor. The weekday value in glkdate_t is
     * ignored. The other values need not be in their normal ranges; they will be
     * normalized.
     * <p/>
     * If the time cannot be represented by the platform's time library, this may
     * return -1.
     *
     * @param date   - [input] date and time in universal (GMT) time.
     * @param factor - factor to divide by.
     * @return simple timestamp divided by factor.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_date_to_simple_time_utc(@NonNull GLKModel m, @NonNull GLKDate date, long factor) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_date_to_simple_time_utc (date = " + date.toString() + ", factor = " + factor + ")");
        }
        int t = date.getSimpleTime(TimeZone.getTimeZone("UTC"), factor);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + t);
        }
        return t;
    }

    /**
     * Convert the broken-out structure (interpreted as universal or local time)
     * to a timestamp divided by factor. The weekday value in glkdate_t is
     * ignored. The other values need not be in their normal ranges; they will be
     * normalized.
     * <p/>
     * If the time cannot be represented by the platform's time library, this may
     * return -1.
     *
     * @param date   - [input] date and time in local time.
     * @param factor - factor to divide by.
     * @return simple timestamp divided by factor.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_date_to_simple_time_local(@NonNull GLKModel m, @NonNull GLKDate date, long factor) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_date_to_simple_time_local (date = " + date.toString() + ", factor = " + factor + ")");
        }
        int t = date.getSimpleTime(TimeZone.getDefault(), factor);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("\t=> " + t);
        }
        return t;
    }

    /**
     * Stops executing the GLK program, displaying a prompt first (call on
     * worker thread only).
     * <p>
     * This function is called by the GLK program, in the worker thread,
     * typically when the user has typed "quit" or some such. We rely on
     * the JNI wrapper to ensure the terp closes down properly after this
     * function returns.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_exit(@NonNull GLKModel m) throws InterruptedException {
        // Display exit prompt and wait for user to hit a key
        int curStrID = m.mStreamMgr.getCurrentOutputStream();
        if (curStrID != GLKConstants.NULL) {
            GLKTextBufferM w = m.mStreamMgr.getTextBuffer(curStrID);
            if (w != null) {
                w.setHTMLOutput(m, false);
                w.setStyle(GLKConstants.style_Input);
                w.putString(m.getApplicationContext().getString(R.string.glk_exit_prompt));
            }
            GLKEvent ev = new GLKEvent();
            glk_request_char_event(m, curStrID);
            do {
                glk_select(m, ev);
            } while (ev.type != GLKConstants.evtype_CharInput);
        }
    }

    /**
     * Provides information about the capabilities of this GLK implementation.
     * This function is equivalent to {@code glk_gestalt_ext(sel, val, null)}.
     *
     * @param sel - the capability to test (e.g. {@code GLKConstants.gestalt_Unicode})
     * @param val - optional argument that may be relevant depending on value of {@code sel}.
     * @return 0 if the tested capability is not supported; otherwise a value that
     * must be interpreted differently depending on the tested capability.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static long glk_gestalt(@NonNull GLKModel m, long sel, long val) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_gestalt");
        }
        return glk_gestalt_ext(m, sel, val, null);
    }

    /**
     * Provides information about the capabilities of this GLK implementation.
     *
     * @param sel - the capability to test.
     * @param val - optional argument that may be relevant depending on value of {@code sel}.
     * @param arr - optional argument that may be relevant depending on value of {@code sel}.
     * @return 0 if the tested capability is not supported; otherwise a value that
     * must be interpreted differently depending on the tested capability.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static long glk_gestalt_ext(@NonNull GLKModel m, long sel, long val, long[] arr) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_gestalt_ext " + GLKConstants.gestaltToString((int) sel) + ", " + val);
        }
        switch ((int) sel) {
            case GLKConstants.gestalt_Version: {
                /* "res will be set to a 32-bit number which encodes the version of the Glk spec which the library
                 *  implements. The upper 16 bits stores the major version number; the next 8 bits stores the minor
                 *  version number; the low 8 bits stores an even more minor version number, if any.
                 *
                 *  [So the version number 78.2.11 would be encoded as 0x004E020B.]
                 *
                 * The current Glk specification version is 0.7.5, so this selector will return 0x00000705." */
                return 0x00000705;
            }
            case GLKConstants.gestalt_CharInput: {
                /* "If you set ch to a character code, or a special code (from 0xFFFFFFFF down), and call
                 *  [the gestalt_CharInput selector] ... then res will be TRUE (1) if that character can be
                 *  typed by the player in character input, and FALSE (0) if not."
                 */
                int ch = (int) val;
                if ((ch >= GLKConstants.keycode_End && ch < GLKConstants.keycode_Unknown) ||
                        (ch >= GLKConstants.keycode_Func12 && ch <= GLKConstants.keycode_Func1)) {
                    return 1;
                } else if ((ch >= 0 && ch <= 9) ||
                        (ch >= 11 && ch <= 31) ||
                        (ch >= 127 && ch <= 159) ||
                        (ch < 0) ||
                        (!Character.isDefined(ch))) {
                    return 0;
                }
                return 1;
            }
            case GLKConstants.gestalt_LineInput: {
                /* "A particular implementation of Glk may not be able to accept all Latin-1 printable characters as input.
                 *  You can test for this by using the gestalt_LineInput selector...
                 *  res will be TRUE (1) if that character can be typed by the player in line input, and FALSE (0) if not. Note
                 *  that if ch is a nonprintable Latin-1 character (0 to 31, 127 to 159), then this is guaranteed to return FALSE."
                 */
                int ch = (int) val;
                if ((ch >= 0 && ch <= 9) ||
                        (ch >= 11 && ch <= 31) ||
                        (ch >= 127 && ch <= 159) ||
                        (ch < 0) ||
                        (!Character.isDefined(ch)) ||
                        (Character.isISOControl(ch))) {
                    return 0;
                }
                return 1;
            }
            case GLKConstants.gestalt_CharOutput: {
                /* "If you set ch to a character code (Latin-1 or higher), and call
                 *
                 *    glui32 res, len;
                 *    res = glk_gestalt_ext(gestalt_CharOutput, ch, &len, 1);
                 *
                 * then res will be one of the following values:
                 *
                 *    gestalt_CharOutput_CannotPrint:
                 *        The character cannot be meaningfully printed. If you try, the
                 *        player may see nothing, or may see a placeholder.
                 *
                 *    gestalt_CharOutput_ExactPrint:
                 *        The character will be printed exactly as defined.
                 *
                 *    gestalt_CharOutput_ApproxPrint:
                 *        The library will print some approximation of the character. It will be
                 *        more or less right, but it may not be precise, and it may not be
                 *        distinguishable from other, similar characters. (Examples: "ae" for
                 *        the one-character "ae" ligature (æ), "e" for an accented "e" (è),
                 *        "|" for a broken vertical bar.)
                 *
                 * In all cases, len (the glui32 value pointed at by the third argument) will be
                 * the number of actual glyphs which will be used to represent the character. In
                 * the case of gestalt_CharOutput_ExactPrint, this will always be 1; for
                 * gestalt_CharOutput_CannotPrint, it may be 0 (nothing printed) or higher;
                 * for gestalt_CharOutput_ApproxPrint, it may be 1 or higher. This information
                 * may be useful when printing text in a fixed-width font.
                 *
                 * [As described in section 1.9, "Other API Conventions", you may skip this information
                 * by passing NULL as the third argument in glk_gestalt_ext(), or by calling glk_gestalt() instead.]
                 *
                 * This selector will always return gestalt_CharOutput_CannotPrint if ch is an unprintable
                 * eight-bit character (0 to 9, 11 to 31, 127 to 159.) */

                // FIXME: we shouldn't just assume that the selected font can render all valid codepoints
                int ch = (int) val;
                if ((ch >= 0 && ch <= 9) ||
                        (ch >= 11 && ch <= 31) ||
                        (ch >= 127 && ch <= 159) ||
                        (ch < 0) ||
                        (!Character.isDefined(ch)) ||
                        (Character.isISOControl(ch))) {
                    return GLKConstants.gestalt_CharOutput_CannotPrint;
                }
                return GLKConstants.gestalt_CharOutput_ExactPrint;
            }
            case GLKConstants.gestalt_MouseInput:
                /* "This will return TRUE (1) if windows of the given type support mouse input.
                 *  If this returns FALSE (0), it is still legal to call glk_request_mouse_event(),
                 *  but it will have no effect, and you will never get mouse events." */
                return 1;
            case GLKConstants.gestalt_Timer:
                /* "This returns TRUE (1) if timer events are supported, and FALSE (0) if they are not." */
                return m.mEnableTimer ? 1 : 0;
            case GLKConstants.gestalt_Graphics:
                /* "This returns 1 if the overall suite of graphics functions is available.
                 *  This includes glk_image_draw(), glk_image_draw_scaled(), glk_image_get_info(),
                 *  glk_window_erase_rect(), glk_window_fill_rect(), glk_window_set_background_color(),
                 *  and glk_window_flow_break(). It also includes the capability to create graphics windows." */
                return m.mEnableGraphics ? 1 : 0;
            case GLKConstants.gestalt_GraphicsTransparency:
                /* "This returns 1 if images with alpha channels can actually be drawn with the appropriate
                 *  degree of transparency. If it returns 0, the alpha channel is ignored; fully transparent
                 *  areas will be drawn in an implementation-defined color. [The JPEG format does not support
                 *  transparency or alpha channels; the PNG format does.] " */
                return m.mEnableGraphics ? 1 : 0;
            case GLKConstants.gestalt_GraphicsCharInput:
                /* "This returns 1 if graphics windows can accept character input requests.
                 *  If it returns zero, do not call glk_request_char_event() or glk_request_char_event_uni()
                 *  on a graphics window." */
                return 1;
            case GLKConstants.gestalt_DrawImage:
                /* "This returns 1 if images can be drawn in windows of the given type. If it returns 0,
                 *  glk_image_draw() will fail and return FALSE (0). You should test wintype_Graphics and
                 *  wintype_TextBuffer separately, since libraries may implement both, neither, or only one." */
                return m.mEnableGraphics && ((val == GLKConstants.wintype_TextBuffer) || (val == GLKConstants.wintype_Graphics)) ? 1 : 0;
            case GLKConstants.gestalt_Sound:
                /* "This returns 1 if the older (pre-0.7.3) suite of sound functions is available. This includes
                 *  glk_schannel_create(), glk_schannel_destroy(), glk_schannel_iterate(), glk_schannel_get_rock(),
                 *  glk_schannel_play(), glk_schannel_play_ext(), glk_schannel_stop(), glk_schannel_set_volume(),
                 *  and glk_sound_load_hint().
                 *
                 *  If this selector returns 0, you should not try to call these functions. They may have no effect,
                 *  or they may cause a run-time error.
                 *
                 *  This selector is guaranteed to return 1 if gestalt_Sound2 does." */
                return m.mEnableSounds ? 1 : 0;
            case GLKConstants.gestalt_Sound2:
                /* "This returns 1 if the overall suite of sound functions is available. This
                 *  includes all the functions defined in this chapter. It also includes the
                 *  capabilities described below under gestalt_SoundMusic, gestalt_SoundVolume,
                 *  and gestalt_SoundNotify. */
                return m.mEnableSounds ? 1 : 0;
            case GLKConstants.gestalt_SoundVolume:
                /* "This selector returns 1 if the glk_schannel_set_volume() function works. If it returns zero,
                 *  glk_schannel_set_volume() has no effect.
                 *
                 *  This selector is guaranteed to return 1 if gestalt_Sound2 does." */
                return m.mEnableSounds ? 1 : 0;
            case GLKConstants.gestalt_SoundNotify:
                /* "This selector returns 1 if the library supports sound notification events.
                 *  If it returns zero, you will never get such events.
                 *
                 *  This selector is guaranteed to return 1 if gestalt_Sound2 does." */
                return m.mEnableSounds ? 1 : 0;
            case GLKConstants.gestalt_SoundMusic:
                /* "This returns 1 if the library is capable of playing music sound resources.
                 *  If it returns 0, only sampled sounds can be played.
                 *
                 *  ["Music sound resources" means MOD songs -- the only music format that
                 *  Blorb currently supports. The presence of this selector is, of course,
                 *  an ugly hack. It is a concession to the current state of the Glk
                 *  libraries, some of which can handle AIFF but not MOD sounds.]
                 *
                 *  This selector is guaranteed to return 1 if gestalt_Sound2 does." */
                return m.mEnableSounds ? 1 : 0;
            case GLKConstants.gestalt_Hyperlinks:
                /* "This returns 1 if the overall suite of hyperlinks functions is available.
                 *  This includes glk_set_hyperlink(), glk_set_hyperlink_stream(),
                 *  glk_request_hyperlink_event(), glk_cancel_hyperlink_event(). */
                return m.mEnableHyperlinks ? 1 : 0;
            case GLKConstants.gestalt_HyperlinkInput:
                /* "This will return TRUE (1) if windows of the given type support hyperlinks.
                 *  If this returns FALSE (0), it is still legal to call glk_set_hyperlink()
                 *  and glk_request_hyperlink_event(), but they will have no effect, and you will never get hyperlink events. */
                return m.mEnableHyperlinks ? 1 : 0;
            case GLKConstants.gestalt_Unicode:
                /* "This returns 1 if the core Unicode functions are available. If it
                 * returns 0, you should not try to call them. They may print nothing, print
                 * gibberish, or cause a run-time error. The Unicode functions include
                 * glk_buffer_to_lower_case_uni, glk_buffer_to_upper_case_uni,
                 * glk_buffer_to_title_case_uni, glk_put_char_uni, glk_put_string_uni,
                 * glk_put_buffer_uni, glk_put_char_stream_uni, glk_put_string_stream_uni,
                 * glk_put_buffer_stream_uni, glk_get_char_stream_uni, glk_get_buffer_stream_uni,
                 * glk_get_line_stream_uni, glk_request_char_event_uni, glk_request_line_event_uni,
                 * glk_stream_open_file_uni, glk_stream_open_memory_uni." */
                return 1;
            case GLKConstants.gestalt_UnicodeNorm:
                /* "This returns 1 if the Unicode normalization functions are available.
                 * If it returns 0, you should not try to call them. The Unicode normalization
                 * functions include glk_buffer_canon_decompose_uni and glk_buffer_canon_normalize_uni." */
                return 1;
            case GLKConstants.gestalt_LineInputEcho:
                /* "This returns 1 if glk_set_echo_line_event() is supported, and 0 if it is not." */
                return 1;
            case GLKConstants.gestalt_LineTerminators:
                /* "This returns 1 if glk_set_terminators_line_event() is supported, and 0 if it is not." */
                return 1;
            case GLKConstants.gestalt_LineTerminatorKey:
                /* "This returns 1 if the keycode ch can be passed to glk_set_terminators_line_event().
                 *  If it returns 0, that keycode will be ignored as a line terminator. Printable
                 *  characters and keycode_Return will always return 0." */
                int ch = (int) val;
                if (ch == GLKConstants.keycode_Return) {
                    return 0;
                } else if ((ch >= GLKConstants.keycode_End && ch < GLKConstants.keycode_Unknown) ||
                        (ch >= GLKConstants.keycode_Func12 && ch <= GLKConstants.keycode_Func1)) {
                    return 1;
                } else if (Character.isISOControl(ch)) {
                    return 1;
                }
                return 0;
            case GLKConstants.gestalt_DateTime:
                /* "This returns 1 if the overall suite of system clock functions, as
                 *  described in this chapter, is available. If this selector returns 0,
                 *  you should not try to call these functions. They may have no effect,
                 *  or they may cause a run-time error." */
                return m.mEnableDateTime ? 1 : 0;
            case GLKConstants.gestalt_ResourceStream:
                /* "This returns 1 if the glk_stream_open_resource() and glk_stream_open_resource_uni()
                 *  functions are available. If it returns 0, you should not call them." */
                return 1;
            case GLKConstants.gestalt_HTML:
                /* Fabularium-specific addition, currently only used by TADS */
                return 1;
        }

        if (DEBUG_FORCE_GESTALT) {
            GLKLogger.warn("GLKActivity: Warning: did not recognise gestalt call, sel = " + sel + ", val = " + val + ". However claiming to have this ability as DEBUG_FORCE_GESTALT is on.");
            return 1;
        }

        return 0;
    }

    /**
     * Converts the Unicode string in 'buf' to lower case.
     *
     * @param buf      - the Unicode string to convert.
     * @param numchars - the number of characters in the buffer initially.
     * @return the number of characters after conversion. If this is
     * greater than len, the characters in the array will be safely truncated at
     * len, but the true count will be returned.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_buffer_to_lower_case_uni(@NonNull GLKModel m, @Nullable ByteBuffer buf, int numchars) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_buffer_to_lower_case_uni (numchars = " + numchars + ")");
        }
        return unicodeBufferConversion(m, buf, numchars, 0);
    }

    /**
     * Converts the Unicode string in 'buf' to upper case.
     *
     * @param buf      - the Unicode string to convert.
     * @param numchars - the number of characters in the buffer initially.
     * @return the number of characters after conversion. If this is
     * greater than len, the characters in the array will be safely truncated at
     * len, but the true count will be returned.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_buffer_to_upper_case_uni(@NonNull GLKModel m, @Nullable ByteBuffer buf, int numchars) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_buffer_to_upper_case_uni (numchars = " + numchars + ")");
        }
        return unicodeBufferConversion(m, buf, numchars, 1);
    }

    /**
     * Converts the Unicode string in 'buf' to title case.
     *
     * @param buf       - the Unicode string to convert.
     * @param numchars  - the number of characters in the buffer initially.
     * @param lowerrest - if FALSE, the function changes the first character of the
     *                  buffer to upper-case, and leaves the rest of the buffer
     *                  unchanged. If TRUE, it changes the first character to
     *                  upper-case and the rest to lower-case.
     * @return the number of characters after conversion. If this is
     * greater than len, the characters in the array will be safely truncated at
     * len, but the true count will be returned.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_buffer_to_title_case_uni(@NonNull GLKModel m, @Nullable ByteBuffer buf, int numchars, boolean lowerrest) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_buffer_to_title_case_uni (numchars = " + numchars + ", lowerrest = " + lowerrest + ")");
        }
        return unicodeBufferConversion(m, buf, numchars, lowerrest ? 3 : 2);
    }

    /**
     * Transforms a string into its canonical decomposition ("Normalization Form D").
     * <p>
     * Effectively, this takes apart multipart characters into their individual parts.
     * For example, it would convert "e" (character 0xE8, an accented "e") into
     * the two-character string containing "e" followed by Unicode character 0x0300
     * (COMBINING GRAVE ACCENT). If a single character has multiple accent marks,
     * they are also rearranged into a standard order.
     *
     * @param buf      - the Unicode string to convert.
     * @param numchars - the number of characters in the buffer initially.
     * @return the number of characters after conversion. If this is
     * greater than len, the characters in the array will be safely truncated at
     * len, but the true count will be returned.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_buffer_canon_decompose_uni(@NonNull GLKModel m, @Nullable ByteBuffer buf, int numchars) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_buffer_canon_decompose_uni (numchars = " + numchars + ")");
        }
        return unicodeBufferConversion(m, buf, numchars, 4);
    }

    /**
     * Transforms a string into its canonical decomposition and recomposition ("Normalization Form C").
     * <p>
     * Effectively, this takes apart multipart characters, and then puts them back together
     * in a standard way. For example, this would convert the two-character string
     * containing "e" followed by Unicode character 0x0300 (COMBINING GRAVE ACCENT)
     * into the one-character string "e" (character 0xE8, an accented "e").
     *
     * @param buf      - the Unicode string to convert.
     * @param numchars - the number of characters in the buffer initially.
     * @return the number of characters after conversion. If this is
     * greater than len, the characters in the array will be safely truncated at
     * len, but the true count will be returned.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_buffer_canon_normalize_uni(@NonNull GLKModel m, @Nullable ByteBuffer buf, int numchars) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_buffer_canon_normalize_uni (numchars = " + numchars + ")");
        }
        return unicodeBufferConversion(m, buf, numchars, 5);
    }

    /**
     * Converts a Unicode buffer, based on value of conv_type.
     *
     * @param buf       - a buffer of Unicode codepoints.
     * @param numchars  - the number of characters in the buffer initially.
     * @param conv_type - if 0 changes buffer to lowercase; 1 = uppercase;
     *                  2 = title case (changes the first character of the
     *                  buffer to upper-case, and leaves the rest of the buffer
     *                  unchanged); 3 = title case (changes the first character to
     *                  upper-case and the rest to lower-case); 4 = normalise (form D);
     *                  5 = normalise (form C).
     * @return the number of characters after conversion. If this is
     * greater than len, the characters in the array will be safely truncated at
     * len, but the true count will be returned.
     */
    private static int unicodeBufferConversion(@NonNull GLKModel m, @Nullable ByteBuffer buf, int numchars, int conv_type) {
        int initLen = numchars * 4;
        if (buf == null || initLen > buf.capacity()) {
            return 0;
        }

        // effect the conversion
        StringBuilder sb = new StringBuilder();
        buf.limit(initLen);
        String s = m.mCharsetMgr.getGLKString(buf, true);
        switch (conv_type) {
            case 0:
                sb.append(s.toLowerCase());
                break;
            case 1:
                sb.append(s.toUpperCase());
                break;
            case 2:
                sb.append(Character.toChars(Character.toUpperCase(s.codePointAt(0)))).append(s.substring(1));
                break;
            case 3:
                sb.append(Character.toChars(Character.toUpperCase(s.codePointAt(0)))).append(s.substring(1).toLowerCase());
                break;
            case 4:
                sb.append(Normalizer.normalize(s, Normalizer.Form.NFD));
                break;
            case 5:
                sb.append(Normalizer.normalize(s, Normalizer.Form.NFC));
                break;
        }
        buf.limit(buf.capacity()).rewind();
        m.mCharsetMgr.putGLKString(sb.toString(), buf, true, false);

        return sb.length();
    }

    /**
     * Iterates through open streams.
     *
     * @param ref  - starting point for iteration
     * @param rock - if not null, will store the rock associated with returned stream.
     * @return next stream after ref, or NULL if there are no more.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_stream_iterate(@NonNull GLKModel m, int ref, @Nullable int[] rock) {
        int ret = m.mStreamMgr.getNextStream(ref, rock);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_iterate " + ref + " => " + ret);
        }
        return ret;
    }

    /**
     * Opens a stream which reads from or writes to a disk file.
     *
     * @param fileref - indicates the file which will be opened.
     * @param fmode   - any of filemode_Read, filemode_Write, filemode_WriteAppend, or
     *                filemode_ReadWrite. If fmode is filemode_Read, the file must already exist; for the other
     *                modes, an empty file is created if none exists. If fmode is filemode_Write,
     *                and the file already exists, it is truncated down to zero length (an empty
     *                file); the other modes do not truncate. If fmode is filemode_WriteAppend,
     *                the file mark is set to the end of the file.
     * @param rock    - an optional rock to associate with the new stream.
     * @param unicode - whether the stream is Latin-1 or Unicode encoding. Unicode values (characters greater
     *                than 255) cannot be written to a Latin-1 stream. If you try, they will be stored as 0x3F ("?") characters.
     * @return a reference to the stream (or NULL if failure)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_stream_open_file(@NonNull GLKModel m, int fileref, int fmode, int rock, boolean unicode) {
        GLKFileRef fref = m.mStreamMgr.getFileRef(fileref);
        if (fref == null) {
            GLKLogger.error("glk_stream_open_file: invalid fref: " + fileref);
            return GLKConstants.NULL;
        }
        GLKFileStream f = new GLKFileStream(fref, unicode, m.mCharsetMgr);
        f.setRock(rock);
        if (f.open(fmode)) {
            m.mStreamMgr.addStreamToPool(f);
            if (DEBUG_GLK_CALLS) {
                GLKLogger.debug("glk_stream_open_file " + fileref + " (fmode = " + GLKConstants.filemodeToString(fmode) +
                        ", rock = " + rock + ", unicode = " + unicode + ") => " + f.getStreamId());
            }
            return f.getStreamId();
        }
        return GLKConstants.NULL;
    }

    /*========================================
     *          STREAMS IN GENERAL
     *=======================================*/

    /**
     * Opens a stream which reads from or writes into a space in memory.
     *
     * @param buf     - the buffer where output will be read from or written to (may be NULL).
     * @param fmode   - must be one of filemode_Read, filemode_Write, or filemode_ReadWrite.
     * @param rock    - an optional rock to associated with the new stream.
     * @param unicode - whether the stream is Latin-1 or Unicode encoding. Unicode values (characters greater
     *                than 255) cannot be written to a Latin-1 stream. If you try, they will be stored as 0x3F ("?") characters.
     * @return a reference to the stream (or NULL if failure)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_stream_open_memory(@NonNull GLKModel m, @Nullable ByteBuffer buf, int fmode, int rock, boolean unicode) {
        // If this is called from JNI, then the Bytebuffer is freed when the memory file is closed or object is destroyed.
        // N.B. standard GLKMemoryStreams are always opened in binary mode, not text mode.
        GLKMemoryStream mf = new GLKMemoryStream(buf, unicode, fmode, m.mCharsetMgr);
        mf.setRock(rock);
        m.mStreamMgr.addStreamToPool(mf);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_open_memory (fmode = " +
                    GLKConstants.filemodeToString(fmode) + ", rock = " + rock + ", unicode = " + unicode + ") => " + mf.getStreamId());
        }
        return mf.getStreamId();
    }

    /**
     * Opens a stream which reads from (but not writes to) a resource file.
     * <p>
     * This is a convenience method. It is exactly equivalent to:
     * <p>
     * fref = glk_fileref_create_by_name(textmode, pathname, rock)
     * return glk_stream_open_file(fref, filemode_Read, rock, textmode)
     * <p>
     * but more efficient.
     *
     * @param pathname - the path to the file to read.
     * @param textmode - whether the stream should be read as UTF-8 text or binary.
     * @param rock     - an optional rock to associate with the new stream.
     * @return a reference to the stream (or NULL if failure)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glkplus_stream_open_pathname(@NonNull GLKModel m, @NonNull byte[] pathname, boolean textmode, int rock) {
        String ss = m.mCharsetMgr.getGLKString(ByteBuffer.wrap(pathname), false);
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS || DEBUG_GLKPLUS_CALLS) {
            GLKLogger.debug("gli_stream_open_pathname '" + ss + "', " + textmode + ", " + rock);
        }
        GLKFileRef fr = new GLKFileRef(ss,
                (textmode ? GLKConstants.fileusage_TextMode : GLKConstants.fileusage_BinaryMode), rock);
        m.mStreamMgr.addStreamToPool(fr);
        GLKFileStream f = new GLKFileStream(fr, textmode, m.mCharsetMgr);
        f.setRock(rock);
        if (f.open(GLKConstants.filemode_Read)) {
            m.mStreamMgr.addStreamToPool(f);
            return f.getStreamId();
        }
        return GLKConstants.NULL;
    }

    /**
     * Opens a stream which reads from (but not writes to) a resource file.
     * <p/>
     * Typically this is embedded in a Blorb file, as Blorb is the official
     * resource-storage format of Glk. A Blorb file can contain images and sounds,
     * but it can also contain raw data files. A data file is identified by number,
     * not by a filename. The Blorb usage field will be 'Data'. The chunk type
     * will be 'TEXT' for text resources, 'BINA' or 'FORM' for binary resources.
     *
     * @param filenum - number of the file to retrieve.
     * @param rock    - an optional rock to associate with the new stream.
     * @param unicode - whether the stream is Latin-1 or Unicode encoding.
     * @return a reference to the stream (or NULL if failure)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_stream_open_resource(@NonNull GLKModel m, int filenum, int rock, boolean unicode) {
        int sId = GLKConstants.NULL;
        GLKMemoryStream mf = m.mResourceMgr.getMemoryStream(filenum, unicode);
        if (mf != null) {
            mf.setRock(rock);
            m.mStreamMgr.addStreamToPool(mf);
            sId = mf.getStreamId();
        }
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_open_resource (filenum = " +
                    filenum + ", rock = " + rock + ", unicode = " + unicode + ") => " + sId);
        }
        return sId;
    }

    /**
     * Closes a stream.
     *
     * @param str    - stream to close (if str is the current output stream, the current
     *               output stream is set to NULL.)
     * @param result - points to a structure which is filled in with
     *               the final character counts of the stream (read count then write count).
     *               If you do not care about these, you may pass NULL as the result argument.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_stream_close(@NonNull GLKModel m, int str, @Nullable long[] result) {
        GLKStream s = m.mStreamMgr.getStream(str);
        if (s == null) {
            GLKLogger.warn("glk_stream_close: invalid stream id: " + str);
            return;
        }

        if (s instanceof GLKWindowM) {
            glk_window_close(m, str, result);
            return;
        } else {
            if (result != null) {
                if (s instanceof GLKInputStream) {
                    result[0] = ((GLKInputStream) s).getReadCount();
                }
                if (result.length > 1 && s instanceof GLKOutputStream) {
                    result[1] = ((GLKOutputStream) s).getWriteCount();
                }
            }
            m.mStreamMgr.closeStream(str);
        }

        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_close " + str + (result != null ? " => read = " + result[0] + ", wrote = " + result[1] : ""));
        }
    }

    /**
     * Sets the current (output) stream.
     * <p/>
     * You may set the current stream to NULL, which means the current stream is not set to anything.
     *
     * @param str - stream to use (must be an output stream), or may be NULL
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_stream_set_current(@NonNull GLKModel m, int str) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_set_current " + str);
        }
        m.mStreamMgr.setCurrentOutputStream(str);
    }

    /**
     * Gets the current (output) stream.
     *
     * @return the current stream, or NULL if there is none.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_stream_get_current(@NonNull GLKModel m) {
        int ret = m.mStreamMgr.getCurrentOutputStream();
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_get_current => " + ret);
        }
        return ret;
    }

    /**
     * Sets the position of the mark.
     * <p/>
     * The position is controlled by pos, and the meaning of pos is controlled by seekmode.
     * <p/>
     * In binary files, the mark position is exact -- it corresponds with the number of characters
     * you have read or written. In text files, this mapping can vary, because of linefeed conversions
     * or other character-set approximations.  Again, in Latin-1 streams, characters are bytes. In Unicode streams,
     * characters are 32-bit words, or four bytes each.
     *
     * @param str      - stream to set position
     * @param pos      - the position to set
     * @param seekmode - either seekmode_Start (pos characters after the beginning of the file.),
     *                 seekmode_Current (pos characters after the current position (moving
     *                 backwards if pos is negative.), or
     *                 seekmode_End (pos characters after the end of the file - pos should
     *                 always be zero or negative, so that this will move backwards to a position
     *                 within the file.)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_stream_set_position(@NonNull GLKModel m, int str, long pos, int seekmode) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_set_position " + str + ", pos = " + pos + ", seekmode = " + seekmode);
        }
        GLKInputStream s = m.mStreamMgr.getInputStream(str);
        if (s == null) {
            GLKLogger.warn("glk_stream_set_position: invalid stream id: " + str);
            return;
        }
        s.setPosition(pos, seekmode);
    }

    /**
     * Gets the position of a read/write mark in a stream.
     * <p/>
     * For memory streams and binary file streams, this is exactly the number of characters
     * read or written from the beginning of the stream (unless you have moved the mark with
     * glk_stream_set_position().) For text file streams, matters are more ambiguous,
     * since (for example) writing one byte to a text file may store more than one character
     * in the platform's native encoding. You can only be sure that the position increases as
     * you read or write to the file.
     * <p/>
     * Additional complication: for Latin-1 memory and file streams, a character is a byte.
     * For Unicode memory and file streams (those created by glk_stream_open_file_uni() and
     * glk_stream_open_memory_uni()), a character is a 32-bit word. So in a binary Unicode
     * file, positions are multiples of four bytes.
     *
     * @param str - stream to get position
     * @return position of the mark.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static long glk_stream_get_position(@NonNull GLKModel m, int str) {
        GLKInputStream s = m.mStreamMgr.getInputStream(str);
        if (s == null) {
            GLKLogger.warn("glk_stream_get_position: invalid stream id: " + str);
            return GLKConstants.NULL;
        }
        long ret = s.getPosition();
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_get_position " + str + " => " + ret);
        }
        return ret;
    }

    /**
     * Returns the rock associated with a given stream.
     *
     * @param str - stream to inspect
     * @return the associated rock.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_stream_get_rock(@NonNull GLKModel m, int str) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stream_get_rock " + str);
        }
        GLKStream s = m.mStreamMgr.getStream(str);
        if (s == null) {
            GLKLogger.warn("glk_stream_get_rock: invalid stream id: " + str);
            return GLKConstants.NULL;
        }
        return s.getRock();
    }

    /**
     * Iterates through open windows.
     * <p>
     * The order in which objects are returned is entirely arbitrary.
     *
     * @param win  - id of the last window returned by this function,
     *             or {@code null} to retrieve the first window.
     * @param rock - if {@code glk_window_iterate} returns a window, the window's
     *             rock (if any) will be stored in this object.
     * @return id of the next window, or {@code null} if there are no more.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_iterate(@NonNull GLKModel m, int win, int[] rock) {
        int ret = m.mStreamMgr.getNextWindow(win, rock);
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_iterate " + win + " => " + ret);
        }
        return ret;
    }

    /**
     * Opens a new window.
     * <p>
     * If there are currently no windows, the first three arguments are ignored.
     *
     * @param split   - the existing window to split.
     * @param method  - logical-or of a direction constant (winmethod_Above, winmethod_Below, winmethod_Left, winmethod_Right)
     *                and a split-method constant (winmethod_Fixed, winmethod_Proportional).
     * @param size    - size of the split. This value is interpreted differently depending on the values for <code>method</code>
     *                and <code>wintype</code>.
     * @param wintype -the type of window being created.
     * @param rock    - (optional) a 'rock' to store with the window.
     * @return the ID of the new window, or GLKConstants.NULL if the call failed.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_open(@NonNull GLKModel m, int split, int method, int size, int wintype, int rock) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("before glk_window_open: " + m.mStreamMgr.getWindowInfo());
        }

        // Create a new window based on the specified window type
        GLKNonPairM newWin;
        switch (wintype) {
            case GLKConstants.wintype_TextGrid: {
                GLKTextGridM.GLKTextGridBuilder builder = new GLKTextGridM.GLKTextGridBuilder();
                builder.setModel(m)
                        .setDefaultStyles(m.mDefaultStylesTextGrid)
                        .setBGColor(m.mBackgroundColor)
                        .setGLKMultipliers(m.mTextGridWidthMultiplier, m.mTextGridHeightMultiplier);
                builder.setLeadingMultiplier(m.mDefaultLeadingTextGrid)
                        .setCursorWidth(m.mCursorWidthPX)
                        .setSubPixelRendering(m.mSubPixel);
                newWin = builder.build();
                newWin.setPadding(m.mDefaultMarginsTextGridPX.left,
                        m.mDefaultMarginsTextGridPX.top,
                        m.mDefaultMarginsTextGridPX.right,
                        m.mDefaultMarginsTextGridPX.bottom);
                break;
            }
            case GLKConstants.wintype_TextBuffer: {
                GLKTextBufferM.GLKTextBufferBuilder builder = new GLKTextBufferM.GLKTextBufferBuilder();
                builder.setModel(m)
                        .setDefaultStyles(m.mDefaultStylesTextBuf)
                        .setBGColor(m.mBackgroundColor)
                        .setGLKMultipliers(m.mTextBufWidthMultiplier, m.mTextBufHeightMultiplier);
                builder.setLeadingMultiplier(m.mDefaultLeadingTextBuf)
                        .setCursorWidth(m.mCursorWidthPX)
                        .setSubPixelRendering(m.mSubPixel);
                newWin = builder.build();
                newWin.setPadding(m.mDefaultMarginsTextBufPX.left,
                        m.mDefaultMarginsTextBufPX.top,
                        m.mDefaultMarginsTextBufPX.right,
                        m.mDefaultMarginsTextBufPX.bottom);
                break;
            }
            case GLKConstants.wintype_Graphics: {
                // N.B. GLK Spec 7.2 states that "The initial background color of each [graphics] window is white."
                GLKGraphicsM.GLKGraphicsBuilder builder = new GLKGraphicsM.GLKGraphicsBuilder();
                builder.setModel(m)
                        .setDefaultStyles(m.mDefaultStylesTextBuf)
                        .setBGColor(m.mBackgroundColor)
                        .setGLKMultipliers(m.mGraphicsWidthMultiplier, m.mGraphicsHeightMultiplier);
                newWin = builder.build();
                break;
            }
            default:
                // error
                GLKLogger.error("glk_window_open: invalid window type: " + wintype);
                return GLKConstants.NULL;
        }

        m.mStreamMgr.addStreamToPool(newWin);
        newWin.setRock(rock);

        // Are we splitting an existing window?
        if (split == GLKConstants.NULL) {
            // No. Just create a new root window.
            m.mRootWinID = newWin.getStreamId();
            newWin.setParent(null);
            newWin.setSibling(null);
            Point rootSize = m.getScreenSize();
            newWin.resize(rootSize.x, rootSize.y);
        } else {
            // Yes. Locate the window to split. Replace it with a new Pair window,
            // and demote it to become the nonkey child of that Pair window.  Insert
            // the new key window as the Pair window's key child.

            // Find the window to split (will become a non-key window)
            GLKWindowM splitWin = m.mStreamMgr.getWindow(split);
            if (splitWin == null) {
                // error
                GLKLogger.error("glk_window_open: invalid split window id: " + split);
                GLKLogger.error("glk_window_open: dumping active windows: " + m.mStreamMgr.getWindowInfo());
                return GLKConstants.NULL;
            }

            // Create a new pair window with the same layout and size.
            // Also, if the split window was the root window, the new pair window
            // now becomes the root window.
            GLKPairM o = new GLKPairM(m);
            m.mStreamMgr.addStreamToPool(o);
            if (m.mRootWinID == split) {
                m.mRootWinID = o.getStreamId();
            }

            switch (m.mBorderStyle) {
                case 0:
                    // always (clear no border flag)
                    method &= ~GLKConstants.winmethod_NoBorder;
                    break;
                case 1:
                    // never (set no border flag)
                    method |= GLKConstants.winmethod_NoBorder;
                    break;
            }

            if (!o.openSplit(method, size, false, newWin, splitWin)) {
                GLKLogger.error("glk_window_open: invalid split method: " + method);
                return GLKConstants.NULL;
            }
        }

        if (m.mHTMLOutput && newWin instanceof GLKTextBufferM) {
            ((GLKTextBufferM) newWin).setHTMLOutput(m, true);
        }

        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_open " + split + ": method = " + (split != 0 ? GLKConstants.winmethodToString(method) : "<null>") +
                    ", size = " + (split != 0 ? size : "<null>") + ", wintype = " + GLKConstants.wintypeToString(wintype) + " => " + newWin.getStreamId());
            GLKLogger.debug(m.mStreamMgr.getWindowInfo());
        }

        return newWin.getStreamId();
    }

    /*========================================
     *           WINDOWS
     *=======================================*/

    /**
     * Closes a window.
     * <p>
     * It is legal to close all your windows, or to close the root
     * window (which does the same thing.) When you close a window (and it is not
     * the root window), the other window in its pair takes over all the freed-up area.
     *
     * @param win    - window to close
     * @param result - output character count of the window stream.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_close(@NonNull GLKModel m, int win, @Nullable long[] result) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_close " + win);
        }

        // Locate the window we've been asked to close, then try to close it.
        GLKWindowM closeWin = m.mStreamMgr.getWindow(win);
        if (closeWin == null) {
            GLKLogger.warn("glk_window_close: invalid window id: " + win);
            return;
        }
        if (result != null && result.length > 1 && closeWin instanceof GLKNonPairM) {
            result[0] = 0;
            result[1] = ((GLKNonPairM) closeWin).getWriteCount();
        }
        m.mStreamMgr.closeStream(win);
    }

    /**
     * Changes the size of an existing split - i.e. changes the constraint settings of a given pair window.
     * The constraint is placed on a child of 'win' as specified by the direction part of the method argument.
     * <p>
     * Note that you can resize windows, and alter the Border/NoBorder flag. But you can't flip or rotate
     * them. You can't move A above D, or change O2 to a vertical split where A is left or right of D.
     * [[To get this effect you could close one of the windows, and re-split the other one with
     * glk_window_open().]]
     *
     * @param win    - the pair window.
     * @param method - logical-or of a direction constant (winmethod_Above, winmethod_Below, winmethod_Left, winmethod_Right)
     *               and a split-method constant (winmethod_Fixed, winmethod_Proportional).
     * @param size   - a new size for the split. This value is interpreted differently depending on the values for <code>method</code>.
     * @param keywin - which of the children should become the key window. May be <code>GLKConstants.NULL</code> if the key window
     *               is to be left unchanged.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_set_arrangement(@NonNull GLKModel m, int win, int method, int size, int keywin) {
        // TODO: adjust size values if width or height multiplier in effect
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_set_arrangement " + win + ": method = " + GLKConstants.winmethodToString(method) + ", size = " + size + ", keywin = " + keywin);
        }

        // Get parent
        GLKPairM o = m.mStreamMgr.getPairWindow((win == GLKConstants.NULL) ? m.mRootWinID : win);
        if (o == null) {
            GLKLogger.warn("glk_window_set_arrangement: invalid window id: " + win);
            return;
        }
        switch (m.mBorderStyle) {
            case 0:
                // always (clear no border flag)
                method &= ~GLKConstants.winmethod_NoBorder;
                break;
            case 1:
                // never (set no border flag)
                method |= GLKConstants.winmethod_NoBorder;
                break;
        }

        // Check key window is not a Pair window or NULL
        GLKNonPairM kw = null;
        if (keywin != GLKConstants.NULL) {
            kw = m.mStreamMgr.getNonPairWindow(keywin);
            if (kw == null) {
                GLKLogger.error("glk_window_set_arrangement: invalid keywin id: " + win + ", will try to ignore it.");
            }
        }

        if (!o.changeSplit(method, size, false, kw)) {
            GLKLogger.error("glk_window_set_arrangement: could not change split.");
        }
    }

    /**
     * Gets a pair window's constraint.
     *
     * @param win       - the pair window.
     * @param methodptr - if not NULL, upon success this will contain the direction and split constant for the Pair window.
     * @param sizeptr   - if not NULL, upon success this will contain the size for the Pair window.
     * @param keywinptr - if not NULL, upon success this will contain a reference to the Pair's key window (if it exists).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_get_arrangement(@NonNull GLKModel m, int win,
                                                  @Nullable int[] methodptr, @Nullable int[] sizeptr, @Nullable int[] keywinptr) {
        GLKPairM o = m.mStreamMgr.getPairWindow(win);
        if (o == null) {
            GLKLogger.warn("glk_window_get_arrangement: invalid window id: " + win);
            return;
        }

        if (methodptr != null) {
            methodptr[0] = o.getSplitMethod();
        }
        if (sizeptr != null) {
            sizeptr[0] = o.getSplitSize();
        }
        if (keywinptr != null) {
            keywinptr[0] = o.getKeyWinID();
        }

        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_get_arrangement " + win + " => meth = " +
                    (methodptr != null ? GLKConstants.winmethodToString(methodptr[0]) : "<unknown>") + ", size = " +
                    (sizeptr != null ? sizeptr[0] : "<unknown>") + ", key = " + (keywinptr != null ? keywinptr[0] : "<null>"));
        }
    }

    /**
     * Gets a window's actual size, in its measurement system.
     *
     * @param win - the window to query.
     * @return - a Point object containing the window's width (x) and height (y), or NULL if failure.
     */
    @NonNull
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static Point glk_window_get_size(@NonNull GLKModel m, int win) {
        GLKWindowM w = m.mStreamMgr.getWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_get_size: invalid window id: " + win);
            return new Point(0, 0);
        }

        Point ret = new Point(w.getGLKSize());
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_get_size " + win + " => size: " + ret.toString());
        }
        return ret;
    }

    /**
     * Moves a text grid window's cursor.
     * <p>
     * If you move the cursor below the last line, or when the cursor reaches the
     * end of the last line, it goes "off the screen" and further output has no effect.
     *
     * @param win  - the text grid window
     * @param xpos - the new x position (column)
     * @param ypos - the new y position (row)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_move_cursor(@NonNull GLKModel m, int win, int xpos, int ypos) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_move_cursor " + win + ": row " + ypos + ", col " + xpos);
        }
        GLKTextGridM w = m.mStreamMgr.getTextGrid(win);
        if (w == null) {
            GLKLogger.warn("glk_window_move_cursor: invalid window id: " + win);
            return;
        }
        w.moveCursor(xpos, ypos);
    }

    /**
     * Sets a window's echo stream.
     * <p>
     * Any text or style debug sent to the
     * window will also be sent to this echo stream.
     *
     * @param win - the window
     * @param str - the echo stream.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_set_echo_stream(@NonNull GLKModel m, int win, int str) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_window_set_echo_stream " + win);
        }

        GLKNonPairM w = m.mStreamMgr.getNonPairWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_set_echo_stream: invalid window id: " + win);
            return;
        }

        GLKOutputStream s = null;
        if (str != GLKConstants.NULL) {
            s = m.mStreamMgr.getOutputStream(str);
            if (s == null) {
                GLKLogger.warn("glk_window_set_echo_stream: invalid stream id: " + str);
                return;
            }
        }

        w.setEchoee(s);
    }

    /**
     * Gets a window's echo stream.
     *
     * @param win - the window
     * @return the echo stream, or NULL if there isn't one.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_get_echo_stream(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_window_get_echo_stream " + win);
        }
        GLKNonPairM w = m.mStreamMgr.getNonPairWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_get_echo_stream: invalid window id: " + win);
            return GLKConstants.NULL;
        }
        return w.getEchoee();
    }

    /**
     * Gets a window's rock.
     *
     * @param win - the window.
     * @return the rock.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_get_rock(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_window_get_rock " + win);
        }
        GLKWindowM w = m.mStreamMgr.getWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_get_rock: invalid window id: " + win);
            return GLKConstants.NULL;
        }
        return w.getRock();
    }

    /**
     * Gets a window's type.
     *
     * @param win - the window.
     * @return the window type (TextBuffer, TextGrid, Pair, Graphics)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_get_type(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_get_type " + win);
        }
        int ret = m.mStreamMgr.getWindowType(win);
        if (ret == GLKConstants.NULL) {
            GLKLogger.warn("glk_window_get_type: invalid window id: " + win);
        }
        return ret;
    }

    /**
     * Get a window's parent.
     * <p>
     * If <code>win</code> is the root window
     * this function returns GLKConstants.NULL, as the root window has no parent.
     * Remember that the parent of every window is a pair window; other window
     * types are always childless.
     *
     * @param win - the window.
     * @return id of the parent window.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_get_parent(@NonNull GLKModel m, int win) {
        int ret;
        GLKWindowM w = m.mStreamMgr.getWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_get_parent: invalid window id: " + win);
            return GLKConstants.NULL;
        }
        ret = w.getParentId();
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_get_parent " + win + " => " + ret);
        }
        return ret;
    }

    /**
     * Get a window's sibling.
     * <p>
     * If the window is the root window, this returns NULL.
     *
     * @param win - the window.
     * @return the mSibling window, or NULL.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_get_sibling(@NonNull GLKModel m, int win) {
        int ret;
        GLKWindowM w = m.mStreamMgr.getWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_get_sibling: invalid window id: " + win);
            return GLKConstants.NULL;
        }
        ret = w.getSiblingId();
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_get_sibling " + win + " => " + ret);
        }
        return ret;
    }

    /**
     * Gets the root (top-level) window.
     *
     * @return the root, or NULL if there are no open windows.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_get_root(@NonNull GLKModel m) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_get_root => " + m.mRootWinID);
        }
        return m.mRootWinID;
    }

    /**
     * Erases a window. The meaning of this depends on the window type.
     * <p>
     * For a text buffer, this may do any number of things, such as delete all
     * text in the window, or print enough blank lines to scroll all text beyond
     * visibility, or insert a page-break marker which is treated specially by the
     * display part of the library.
     * <p>
     * For a text grid, this will clear the window, filling all positions with
     * blanks (in the normal style). The window cursor is moved to the top left
     * corner (position 0,0).
     * <p>
     * For a graphics window, this clears the entire window to its current
     * background color.
     * <p>
     * For other windows, there is no effect.
     * <p>
     * It is illegal to erase a window which has line input pending.
     *
     * @param win - the window.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_clear(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_clear " + win);
        }
        GLKNonPairM w = m.mStreamMgr.getNonPairWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_clear: invalid window id: " + win);
            return;
        }
        w.clear();
    }

    /**
     * Gets a window's stream ID.
     * <p>
     * Every window has a stream which can be printed to, but this may
     * not be useful, depending on the window type.
     *
     * @param win - the window.
     * @return the stream ID.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_window_get_stream(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_get_stream " + win + " => " + win);
        }
        return win;
    }

    /**
     * Set the current output stream to a window's stream.
     *
     * @param win - the window, or may be NULL to set to no stream.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_set_window(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_set_window " + win);
        }
        m.mStreamMgr.setCurrentOutputStream(win);
    }

    /**
     * Sets the window's background color.
     * <p>
     * This does *not* change what is currently displayed; it only affects
     * subsequent clears and resizes. The initial background color of each
     * window is white.
     *
     * @param win   - a graphics window.
     * @param color - a 32 bit value specifying the fill colour of the rectangle (format 0x00RRGGBB)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_set_background_color(@NonNull GLKModel m, int win, int color) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_window_set_background_colour " + win + " (color = " + color + ")");
        }
        GLKGraphicsM w = m.mStreamMgr.getGraphics(win);
        if (w == null) {
            GLKLogger.warn("glk_window_set_background_colour: invalid window id: " + win);
            return;
        }
        w.setPendingWindowBG(color);
    }

    /**
     * Draws a rectangle in the given graphics window.
     * <p>
     * It is legitimate for part of the rectangle to fall outside the window.
     * If width or height is zero, nothing is drawn.
     *
     * @param win      - a graphics window to draw in.
     * @param color    - a 32 bit value specifying the fill colour of the rectangle (format 0x00RRGGBB)
     * @param leftDp   - the left coordinate of the rectangle (in dp).
     * @param topDp    - the top coordinate of the rectangle (in dp).
     * @param widthDp  - the width of the rectangle, in dp.
     * @param heightDp - the height of the rectangle, in dp.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_fill_rect(@NonNull GLKModel m, int win, int color, int leftDp, int topDp, int widthDp, int heightDp) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_fill_rect " + win + " (color = " + color + ", l = " + leftDp + ", t = " + topDp + ", w = " + widthDp + ", h = " + heightDp);
        }
        if (widthDp == 0 || heightDp == 0) {
            return;
        }
        GLKGraphicsM w = m.mStreamMgr.getGraphics(win);
        if (w == null) {
            GLKLogger.warn("glk_window_fill_rect: invalid window id: " + win);
            return;
        }
        Rect rectPx = GLKUtils.dpToPx(new Rect(leftDp, topDp, leftDp + widthDp, topDp + heightDp), m.getApplicationContext().getResources().getDisplayMetrics());
        w.fillRect(rectPx, color);
    }

    /**
     * Draws a rectangle in the given graphics window, filled with the window's background colour.
     * <p>
     * You can also fill an entire graphics window with its background color by
     * calling glk_window_clear().
     *
     * @param win      - a graphics window to draw in.
     * @param leftDp   - the left coordinate of the rectangle (in dp).
     * @param topDp    - the top coordinate of the rectangle (in dp).
     * @param widthDp  - the width of the rectangle, in dp.
     * @param heightDp - the height of the rectangle, in dp.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_erase_rect(@NonNull GLKModel m, int win, int leftDp, int topDp, int widthDp, int heightDp) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_window_erase_rect " + win + " (l = " + leftDp + ", t = " + topDp + ", w = " + widthDp + ", h = " + heightDp);
        }
        if (widthDp == 0 || heightDp == 0) {
            return;
        }
        GLKGraphicsM w = m.mStreamMgr.getGraphics(win);
        if (w == null) {
            GLKLogger.warn("glk_window_erase_rect: invalid window id: " + win);
            return;
        }
        Rect rectPx = GLKUtils.dpToPx(new Rect(leftDp, topDp, leftDp + widthDp, topDp + heightDp), m.getApplicationContext().getResources().getDisplayMetrics());
        w.eraseRect(rectPx);
    }

    /**
     * "Breaks" the stream of text down below the current margin image.
     * <p>
     * If the current point in the text is indented around a margin-aligned image,
     * this acts like the correct number of newlines to start a new line below the
     * image. (If there are several margin-aligned images, it goes below all of
     * them.) If the current point is *not* beside a margin-aligned image, this
     * call has no effect.
     * <p>
     * In all windows other than text buffers, glk_window_flow_break() has no effect.
     *
     * @param win - a text buffer window.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_window_flow_break(@NonNull GLKModel m, int win) {
        GLKLogger.error("TODO: glk_window_flow_break");
    }

    /**
     * Iterates through open sound channels.
     *
     * @param ref  - initial sound channel to start iteration from.
     * @param rock - if non-NULL will contain rock of returned sound channel.
     * @return the next open sound channel after ref.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_schannel_iterate(@NonNull GLKModel m, int ref, int[] rock) {
        int ret = m.mStreamMgr.getNextSChannel(ref, rock);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_iterate " + ref + " => " + ret);
        }
        return ret;
    }

    /**
     * Creates a sound channel.
     * <p>
     * The new sound channel will have full volume.
     *
     * @param rock - optional rock value to store.
     * @return the new sound channel, or NULL if error.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_schannel_create(@NonNull GLKModel m, int rock) {
        GLKSoundStream s = new GLKSoundStream();
        s.setRock(rock);
        m.mStreamMgr.addStreamToPool(s);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_create (rock = " + rock + ") => " + s.getStreamId());
        }
        return s.getStreamId();
    }

    /*========================================
     *             SOUNDS
     *=======================================*/

    /**
     * Creates a sound channel.
     *
     * @param rock   - optional rock value to store.
     * @param volume - volume of sound channel (0x10000 is full volume; 0x8000 half-volume,
     *               0xC000 three-quarters volume, and so on. A volume of zero represents silence.
     * @return the new sound channel, or NULL if error.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_schannel_create_ext(@NonNull GLKModel m, int rock, long volume) {
        GLKLogger.error("TODO: glk_schannel_create_ext");
        return GLKConstants.NULL;
    }

    /**
     * Destroys a sound channel.
     * <p>
     * If the channel is playing a sound, the sound stops
     * immediately (with no notification event).
     *
     * @param chan - the sound channel.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_schannel_destroy(@NonNull GLKModel m, int chan) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_destroy " + chan);
        }
        GLKSoundStream s = m.mStreamMgr.getSoundStream(chan);
        if (s == null) {
            GLKLogger.warn("glk_schannel_destroy: invalid channel id: " + chan);
            return;
        }
        if (s.isPlaying()) {
            s.stop();
        }
        s.release();
        m.mStreamMgr.closeStream(chan);
    }

    /**
     * Plays a sound.
     *
     * @param chan - an open sound channel.
     * @param snd  - sound to play.
     * @return TRUE if the sound actually started playing or FALSE if there was any problem.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static boolean glk_schannel_play(@NonNull GLKModel m, int chan, int snd) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_play " + snd + " on channel " + chan);
        }
        return glk_schannel_play_ext(m, chan, snd, 1, 0);
    }

    /**
     * Plays a sound.
     *
     * @param chan    - an open sound channel.
     * @param snd     - sound to play.
     * @param repeats - number of times the sound should be repeated. A repeat
     *                value of -1 (or rather 0xFFFFFFFF) means that the sound should
     *                repeat forever. A repeat value of 0 means that the sound will not be played
     *                at all; nothing happens.
     * @param notify  - should be nonzero in order to request a sound notification event.
     *                If you do this, when the sound is completed, you will get an event
     *                with type evtype_SoundNotify.
     * @return TRUE if the sound actually started playing or FALSE if there was any problem.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static boolean glk_schannel_play_ext(@NonNull GLKModel m, int chan, int snd, final int repeats, int notify) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_play_ext " + snd + " on channel " + chan + "(repeats = " + repeats + ", notify = " + notify + ")");
        }
        final GLKSoundStream s = m.mStreamMgr.getSoundStream(chan);
        if (s == null) {
            GLKLogger.warn("glk_schannel_play_ext: invalid channel id: " + chan);
            return false;
        }
        if (notify != 0) {
            // TODO!!
            GLKLogger.warn("glk_schannel_play_ext: caller asked for SoundNotify event - this is not yet supported.");
        }

        // If it's currently playing, stop it
        if (s.isPlaying()) {
            s.stop();
        }

        // A repeat value of 0 means nothing happens
        if (repeats == 0) {
            return true;
        }

        // Play the sound
        if (m.mResourceMgr.getSound(String.valueOf(snd), s)) {
            s.setRepeating(repeats);
            s.start();
            return true;
        }

        return false;
    }

    /**
     * This works the same as glk_schannel_play_ext(), except that you can specify
     * more than one sound. The channel references and sound resource numbers are
     * given as two arrays, which must be the same length. The notify argument
     * applies to all the sounds; the repeats value for all the sounds is 1.
     * <p>
     * All the sounds will begin at exactly the same time.
     *
     * @param m         - the single GLKModel instance
     * @param chanarray - an array of channel references
     * @param sndarray  - an array of sound resource numbers
     * @param notify    - (applies to all sounds) should be nonzero in order to request a sound notification event.
     * @return the number of sounds that began playing correctly. (This will be a number from 0 to soundcount.)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_schannel_play_multi(@NonNull GLKModel m, int[] chanarray, int[] sndarray, int notify) {
        GLKLogger.error("FIXME: glk_schannel_play_multi");
        return 0;
    }

    /**
     * Stops sound playback.
     * <p>
     * No notification event is generated, even if you requested one.
     * If no sound is playing, this has no effect.
     *
     * @param chan - an open sound channel.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_schannel_stop(@NonNull GLKModel m, int chan) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_stop channel " + chan);
        }
        GLKSoundStream s = m.mStreamMgr.getSoundStream(chan);
        if (s == null) {
            GLKLogger.warn("glk_schannel_set_volume: invalid channel id: " + chan);
            return;
        }
        if (s.isPlaying()) {
            s.stop();
        }
    }

    /**
     * Pauses sound playback.
     * <p>
     * This does not generate any notification events.
     * If the channel is already paused, this does nothing.
     * New sounds started in a paused channel are paused immediately.
     * A volume change in progress is *not* paused, and may proceed to completion,
     * generating a notification if appropriate.
     *
     * @param chan - an open sound channel.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_schannel_pause(@NonNull GLKModel m, int chan) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_pause channel " + chan);
        }
        GLKSoundStream s = m.mStreamMgr.getSoundStream(chan);
        if (s == null) {
            GLKLogger.warn("glk_schannel_pause: invalid channel id: " + chan);
            return;
        }
        if (s.isPlaying()) {
            s.pause();
        }
    }

    /**
     * Resumes sound playback.
     * <p>
     * Any paused sounds begin playing where they left off.
     * If the channel is not already paused, this does nothing.
     *
     * @param chan - an open sound channel.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_schannel_unpause(@NonNull GLKModel m, int chan) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_unpause channel " + chan);
        }
        GLKSoundStream s = m.mStreamMgr.getSoundStream(chan);
        if (s == null) {
            GLKLogger.warn("glk_schannel_unpause: invalid channel id: " + chan);
            return;
        }
        if (!s.isPlaying()) {
            s.start();
        }
    }

    /**
     * Sets the volume.
     *
     * @param chan - an open sound channel.
     * @param vol  - the new volume level, from 0 (silence)
     *             to 0x10000 (full volume). You can overdrive
     *             the volume by setting a value greater than 0x10000, but
     *             this is not recommended.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_schannel_set_volume(@NonNull GLKModel m, int chan, long vol) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_set_volume of channel " + chan + " to " + vol);
        }
        GLKSoundStream s = m.mStreamMgr.getSoundStream(chan);
        if (s == null) {
            GLKLogger.warn("glk_schannel_set_volume: invalid channel id: " + chan);
            return;
        }

        // glk volume ranges from 0 to 0x1000; map this to android which ranges from 0 to 1
        float androidVol = (float) vol / 0x1000f;
        s.setVolume(androidVol, androidVol);
    }

    /**
     * Sets the volume.
     *
     * @param chan     - an open sound channel.
     * @param vol      - the new volume level, from 0 (silence)
     *                 to 0x10000 (full volume). You can overdrive
     *                 the volume by setting a value greater than 0x10000, but
     *                 this is not recommended.
     * @param duration - if zero, the change is immediate. Otherwise, the change
     *                 begins immediately, and occurs smoothly over the next duration
     *                 milliseconds.
     * @param notify   - should be nonzero in order to request a volume
     *                 notification event. If you do this, when the volume change is completed,
     *                 you will get an event with type evtype_VolumeNotify.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_schannel_set_volume_ext(@NonNull GLKModel m, int chan, long vol,
                                                   int duration, int notify) {
        GLKLogger.error("TODO: glk_schannel_set_volume_ext");
    }

    /**
     * Give the library a hint about whether a given sound should be
     * loaded or not.
     * <p>
     * Calling this function is always optional, and it has no
     * effect on what the library actually plays.
     *
     * @param snd  - the sound.
     * @param flag - if nonzero, the library may preload the sound or
     *             do other initialization, so that glk_schannel_play()
     *             will be faster. If zero, the library may release
     *             memory or other resources associated with the sound.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_sound_load_hint(@NonNull GLKModel m, int snd, int flag) {
        GLKLogger.error("TODO: glk_sound_load_hint");
    }

    /**
     * Returns the rock of an open sound channel.
     *
     * @param chan - the sound channel.
     * @return the rock.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_schannel_get_rock(@NonNull GLKModel m, int chan) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_schannel_get_rock " + chan);
        }
        GLKSoundStream s = m.mStreamMgr.getSoundStream(chan);
        if (s == null) {
            GLKLogger.warn("glk_schannel_get_rock: invalid channel id: " + chan);
            return GLKConstants.NULL;
        }
        return s.getRock();
    }

    /**
     * Iterates through open file references.
     *
     * @param fref - a given fref to start at.
     * @param rock - a holder for the returned rock value (can also be null).
     * @return the next fref after the given fref or NULL if no more.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_fileref_iterate(@NonNull GLKModel m, int fref, int[] rock) {
        int ret = m.mStreamMgr.getNextFRef(fref, rock);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_iterate " + fref + " => " + ret);
        }
        return ret;
    }

    /*========================================
     *             FILES
     *========================================*/

    /**
     * Creates a reference to a file with a specific name.
     * <p>
     * The file will be in a fixed location relevant to your program, and visible to the player.
     *
     * @param usage - indicates the file type and mode (text or binary).  A logical combination of
     *              a file type - one of: fileusage_SavedGame, fileusage_Transcript, fileusage_InputRecord, fileusage_Data -
     *              OR'ed with the mode - one of: fileusage_BinaryMode, fileusage_TextMode.
     * @param name  - name of the file
     * @param rock  - an optional rock value to associate with the file ref.
     * @return the file reference or NULL if creation failed.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_fileref_create_by_name(@NonNull GLKModel m, int usage, byte[] name, int rock) {
        String fileName = m.mCharsetMgr.getGLKString(ByteBuffer.wrap(name), false);
        String path = GLKFileRef.getGLKPath(m, fileName, usage);
        GLKFileRef f = new GLKFileRef(path, usage, rock);
        m.mStreamMgr.addStreamToPool(f);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_create_by_name: " + GLKConstants.fileusageToString(usage) + ", '" + path + "', " + rock + " => " + f.getStreamId());
        }
        return f.getStreamId();
    }

    /**
     * Creates a reference to a file by asking the player to locate it.
     * <p>
     * The library may simply prompt the player to type a name, or may use a
     * platform-native file navigation tool.
     *
     * @param usage - indicates the file type and mode (text or binary).  A logical combination of
     *              a file type - one of: fileusage_SavedGame, fileusage_Transcript, fileusage_InputRecord, fileusage_Data -
     *              OR'ed with the mode - one of: fileusage_BinaryMode, fileusage_TextMode.
     * @param fmode - one of filemode_Read, filemode_Write, filemode_ReadWrite or filemode_WriteAppend.
     * @param rock  - an optional rock value to associate with the file ref.
     * @return the file reference or NULL if creation failed.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_fileref_create_by_prompt(@NonNull GLKModel m, int usage, int fmode, int rock) throws InterruptedException {
        int fileType = (usage & GLKConstants.fileusage_TypeMask);
        String path;
        try {
            path = GLKScreen.promptUserForFilePath(m, fmode, fileType);
        } catch (InterruptedException e) {
            if (m.mTerpIsJava) {
                stopTerpJava(m, 2);
                throw new InterruptedException();
            } else {
                stopTerp(2);  // this call doesn't return
            }
            return GLKConstants.NULL;
        }

        if (path.equals("")) {
            // User has cancelled
            return GLKConstants.NULL;
        }

        // OK, if we get to this point we have a valid file path
        GLKFileRef f = new GLKFileRef(path, usage, rock);
        m.mStreamMgr.addStreamToPool(f);

        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_create_by_prompt: " + GLKConstants.fileusageToString(usage) + ", " +
                    GLKConstants.filemodeToString(fmode) + ", " + rock + " => " + f.getStreamId());
        }
        return f.getStreamId();
    }

    /**
     * Creates a reference to a temporary file.
     * <p>
     * It is always a new file (one which does not yet exist). The file (once created)
     * will be somewhere out of the player's way.
     * <p/>
     * A temporary file should not be used for long-term storage. It may be
     * deleted automatically when the program exits, or at some later time, say
     * when the machine is turned off or rebooted. You do not have to worry about
     * deleting it yourself.
     *
     * @param usage - indicates the file type and mode (text or binary).  A logical combination of
     *              a file type - one of: fileusage_SavedGame, fileusage_Transcript, fileusage_InputRecord, fileusage_Data -
     *              OR'ed with the mode - one of: fileusage_BinaryMode, fileusage_TextMode.
     * @param rock  - an optional rock value to associate with the file ref.
     * @return the file reference or NULL if creation failed.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_fileref_create_temp(@NonNull GLKModel m, int usage, int rock) {
        GLKFileRef f;
        try {
            f = new GLKFileRef(usage, rock);
        } catch (IOException e) {
            GLKLogger.warn("glk_fileref_create_temp: IO exception: " + e.getMessage());
            return GLKConstants.NULL;
        }
        m.mStreamMgr.addStreamToPool(f);
        m.mStreamMgr.registerTempFile(f);

        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_create_temp: " + GLKConstants.fileusageToString(usage) + ", " +
                    ", " + rock + " => " + f.getStreamId());
        }
        return f.getStreamId();
    }

    /**
     * Copies an existing file reference, but changes the usage.
     * <p>
     * The original fileref is not modified.
     *
     * @param usage - indicates the file type and mode (text or binary).  A logical combination of
     *              a file type - one of: fileusage_SavedGame, fileusage_Transcript, fileusage_InputRecord, fileusage_Data -
     *              OR'ed with the mode - one of: fileusage_BinaryMode, fileusage_TextMode.
     * @param fref  - existing file reference to copy.
     * @param rock  - an optional rock value to associate with the file ref.
     * @return the file reference or NULL if creation failed.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_fileref_create_from_fileref(@NonNull GLKModel m, int usage, int fref, int rock) {
        GLKFileRef src = m.mStreamMgr.getFileRef(fref);
        if (src == null) {
            GLKLogger.warn("glk_fileref_create_from_fileref: invalid fref: " + fref);
            return GLKConstants.NULL;
        }

        GLKFileRef dest = new GLKFileRef(src.getAbsolutePath(), usage, rock);
        m.mStreamMgr.addStreamToPool(dest);

        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_create_from_fileref: usage: " +
                    GLKConstants.fileusageToString(usage) + ", fref: " +
                    fref + ", rock: " + rock + " => " + dest.getStreamId());
        }
        return dest.getStreamId();
    }

    /**
     * Destroys a fileref which you have created.
     * <p>
     * This does not affect the disk file; it just reclaims the resources allocated by
     * the glk_fileref_create... function.
     * <p/>
     * It is legal to destroy a fileref after opening a file with it (while the file is still open.) The fileref
     * is only used for the opening operation, not for accessing the file stream.
     *
     * @param fref - the file reference to destroy.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_fileref_destroy(@NonNull GLKModel m, int fref) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_destroy " + fref);
        }
        GLKFileRef f = m.mStreamMgr.getFileRef(fref);
        if (f == null) {
            GLKLogger.warn("glk_fileref_destroy: invalid fref: " + fref);
            return;
        }
        m.mStreamMgr.closeStream(fref);
    }

    /**
     * Deletes the file referred to by fref.
     * <p>
     * Does not destroy fileref itself. You should only call this with a fileref that refers
     * to an existing file.
     *
     * @param fref - a file reference to the file that should be deleted.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_fileref_delete_file(@NonNull GLKModel m, int fref) {
        GLKFileRef f = m.mStreamMgr.getFileRef(fref);
        if (f == null) {
            GLKLogger.warn("glk_fileref_delete_file: invalid fref: " + fref);
            return;
        }
        boolean ret = f.delete();
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_delete_file " + fref + " => " + ret);
        }
    }

    /**
     * Determines if a file referred to by a given fref exists.
     *
     * @param fref - a file reference to test.
     * @return TRUE (1) if the fileref refers to an existing file, FALSE (0) if not.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_fileref_does_file_exist(@NonNull GLKModel m, int fref) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_does_file_exist " + fref);
        }
        GLKFileRef f = m.mStreamMgr.getFileRef(fref);
        if (f == null) {
            GLKLogger.warn("glk_fileref_does_file_exist: invalid file ref: " + fref);
            return 0;
        }
        return (f.exists() && f.canRead()) ? 1 : 0;
    }

    /**
     * Retrieves the fileref's rock value.
     *
     * @param fref - file reference to look up.
     * @return associated rock value.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_fileref_get_rock(@NonNull GLKModel m, int fref) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_fileref_get_rock " + fref);
        }
        GLKFileRef f = m.mStreamMgr.getFileRef(fref);
        if (f == null) {
            GLKLogger.warn("glk_fileref_get_rock: invalid fref id: " + fref);
            return GLKConstants.NULL;
        }
        return f.getRock();
    }

    /**
     * Gets the full path for a file associated with a given fileref.
     *
     * @param fref - an open fileref.
     * @return the full path, if success, or NULL if failure.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static byte[] glkplus_fileref_get_name(@NonNull GLKModel m, int fref) {
        GLKFileRef f = m.mStreamMgr.getFileRef(fref);
        if (f != null) {
            String pathName = f.getAbsolutePath();
            GLKLogger.error("glkplus_fileref_get_name(" + fref + ") => " + pathName);
            byte[] retBytes;
            try {
                retBytes = pathName.getBytes("ISO-8859-1");
            } catch (UnsupportedEncodingException e) {
                GLKLogger.error("glkplus_fileref_get_name: string encoding unsupported: " + e.getMessage());
                return null;
            }
            return retBytes;
        }
        return null;
    }

    /**
     * Prints one Latin-1 or Unicode character to the current output stream.
     *
     * @param ch      - the character to print.
     * @param unicode - whether the character is Latin-1 or Unicode.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_put_char(@NonNull GLKModel m, int ch, boolean unicode) {
        if (DEBUG_GLK_OUTPUT_CALLS) {
            GLKLogger.debug("glk_put_char");
        }
        glk_put_char_stream(m, m.mStreamMgr.getCurrentOutputStream(), ch, unicode);
    }

    /*========================================
     *             STREAM I / O
     *========================================*/

    /**
     * Prints one Latin-1 or Unicode character to the given output stream.
     *
     * @param str     - output stream to print to.
     * @param ch      - the character to print.
     * @param unicode - whether the character is Latin-1 or Unicode.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_put_char_stream(@NonNull GLKModel m, int str, int ch, boolean unicode) {
        if (DEBUG_GLK_OUTPUT_CALLS) {
            GLKLogger.debug("glk_put_char_stream " + str + ": '" + (char) ch + "', unicode = " + unicode);
        }
        GLKOutputStream os = m.mStreamMgr.getOutputStream(str);
        if (os == null) {
            GLKLogger.warn("glk_put_char_stream: invalid stream id: " + str);
            return;
        }
        os.putChar(ch);
    }

    /**
     * Prints a block of Latin-1 or Unicode characters to the given output stream.
     *
     * @param str     - an open output stream. Must not be {@code null}.
     * @param buf     - the buffer containing some text to print.
     * @param len     - number of Latin-1 or Unicode characters to print, depending on value of {@code unicode} argument.
     * @param unicode - whether the buffer should be interpreted as Latin-1 or Unicode.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_put_buffer_stream(@NonNull GLKModel m, int str, byte[] buf, int len, boolean unicode) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_put_buffer_stream " + str + " (len = " + len + ", unicode = " + unicode + ")");
        }
        GLKOutputStream os = m.mStreamMgr.getOutputStream(str);
        if (os == null) {
            GLKLogger.warn("glk_put_buffer_stream: invalid stream id " + str);
            return;
        }
        os.putBuffer(ByteBuffer.wrap(buf, 0, unicode ? (len * 4) : len), unicode);
    }

    /**
     * Prints a block of Latin-1 or Unicode characters to the current output stream.
     * <p/>
     * This is exactly equivalent to
     * <p/>
     * {@code for (i = off; i < len; i++)
     * glk_put_char(buf[i]);}
     * <p/>
     * However, it may be more efficient.
     *
     * @param buf     - the buffer containing some text to print.
     * @param len     - number of Latin-1 or Unicode characters to print, depending on value of {@code unicode} argument.
     * @param unicode - whether the buffer should be interpreted as Latin-1 or Unicode.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_put_buffer(@NonNull GLKModel m, byte[] buf, int len, boolean unicode) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_put_buffer");
        }
        glk_put_buffer_stream(m, m.mStreamMgr.getCurrentOutputStream(), buf, len, unicode);
    }

    /**
     * Prints a null-terminated string to the current output stream.
     *
     * @param s       - the string to print.
     * @param unicode - whether the string is Latin-1 or Unicode.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_put_string(@NonNull GLKModel m, @NonNull byte[] s, boolean unicode) {
        if (DEBUG_GLK_OUTPUT_CALLS) {
            GLKLogger.debug("glk_put_string");
        }
        glk_put_string_stream(m, m.mStreamMgr.getCurrentOutputStream(), s, unicode);
    }

    /**
     * Prints a null-terminated string to the given output stream.
     *
     * @param str     - output stream to print to.
     * @param s       - the string to print.
     * @param unicode - whether the string is ISO-8859-1 (Latin-1) or UTF-32 (Unicode).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_put_string_stream(@NonNull GLKModel m, int str, @NonNull byte[] s, boolean unicode) {
        String ss = m.mCharsetMgr.getGLKString(ByteBuffer.wrap(s), unicode);
        if (DEBUG_GLK_OUTPUT_CALLS) {
            GLKLogger.debug("glk_put_string_stream '" + ss + "', str = " + str + ", unicode = " + unicode);
        }
        GLKOutputStream os = m.mStreamMgr.getOutputStream(str);
        if (os == null) {
            GLKLogger.warn("glk_put_string_stream: invalid stream id: " + str);
            return;
        }
        os.putString(ss);
    }

    /**
     * Sets the current link value in the current output stream.
     *
     * @param linkval - any non-zero integer; zero indicates no
     *                link. Subsequent text output is considered to
     *                make up the body of the link, which continues
     *                until the link value is changed (or set to zero).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_set_hyperlink(@NonNull GLKModel m, int linkval) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_set_hyperlink");
        }
        glk_set_hyperlink_stream(m, m.mStreamMgr.getCurrentOutputStream(), linkval);
    }

    /**
     * Sets the current link value in the given output stream.
     *
     * @param str     - an open output stream.
     * @param linkval - any non-zero integer; zero indicates no
     *                link. Subsequent text output is considered to
     *                make up the body of the link, which continues
     *                until the link value is changed (or set to zero).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_set_hyperlink_stream(@NonNull GLKModel m, int str, int linkval) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_set_hyperlink_stream, str = " + str + ", linkval = " + linkval);
        }
        GLKOutputStream os = m.mStreamMgr.getOutputStream(str);
        if (os == null) {
            GLKLogger.warn("glk_set_hyperlink_stream: invalid stream id: " + str);
            return;
        }

        if (linkval != 0) {
            os.startHyperlink(linkval);
        } else {
            os.endHyperlink();
        }
    }

    /**
     * Reads one character from the given input stream.
     *
     * @param str     - stream to read from (must be an input stream)
     * @param unicode - if true, read Unicode characters (4 bytes per char), if false Latin-1 encoding.
     * @return the character read.  If the end of the stream has been reached, the result will be -1. Note that
     * high-bit characters (128..255) are *not* returned as negative numbers.  If unicode is true then the result
     * will be the Unicode value.  If unicode is false, and the stream contains unicode data, characters
     * beyond 255 will be returned as 0x3F ("?").
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_get_char_stream(@NonNull GLKModel m, int str, boolean unicode) {
        GLKInputStream s = m.mStreamMgr.getInputStream(str);
        if (s == null) {
            GLKLogger.warn("glk_get_char_stream: invalid stream id: " + str);
            return -1;
        }
        return s.getChar(unicode);
    }

    /**
     * Reads len characters from the given input stream, unless the end of stream is reached first, into
     * the given buffer.
     * <p/>
     * No terminal null is placed in the buffer. It returns the number of characters actually read.
     *
     * @param str     - stream to read from (must be an input stream)
     * @param buf     - buffer to read data into
     * @param len     - maximum number of characters to read
     * @param unicode - if true, read Unicode characters (4 bytes per char), if false Latin-1 encoding.
     * @return number of characters actually read.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_get_buffer_stream(@NonNull GLKModel m, int str, @NonNull byte[] buf, int len, boolean unicode) {
        GLKInputStream s = m.mStreamMgr.getInputStream(str);
        if (s == null) {
            GLKLogger.warn("glk_get_buffer_stream: invalid stream id: " + str);
            return 0;
        }

        int ret = s.getBuffer(ByteBuffer.wrap(buf, 0, unicode ? (len * 4) : len), unicode);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_get_buffer_stream " + str + " (len = " + len + ", unicode = " + unicode + ") => " + ret);
        }
        return ret;
    }

    /**
     * Reads characters from the given input stream, until either len-1 characters
     * have been read or a newline has been read. It then puts a terminal null
     * ('\0') character on the end. It returns the number of characters actually
     * read, including the newline (if there is one) but not including the
     * terminal null.
     *
     * @param str     - stream to read from (must be an input stream)
     * @param buf     - buffer to read data into
     * @param len     - maximum number of characters to read
     * @param unicode - if true, read Unicode characters (4 bytes per char), if false Latin-1 encoding.
     * @return number of characters actually read, not including the added terminal null.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_get_line_stream(@NonNull GLKModel m, int str, @NonNull byte[] buf, int len, boolean unicode) {
        GLKInputStream s = m.mStreamMgr.getInputStream(str);
        if (s == null) {
            GLKLogger.warn("glk_get_line_stream: invalid stream id: " + str);
            return 0;
        }

        int ret = s.getLine(ByteBuffer.wrap(buf, 0, unicode ? (len * 4) : len), unicode);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_get_line_stream " + str + " (len = " + len + ", unicode = " + unicode + ") => " + ret);
        }
        return ret;
    }

    /**
     * Changes the style of the current output stream.
     *
     * @param val - should be one of the eleven style constants. However,
     *            any value is actually legal; if the interpreter does not
     *            recognize the style value, it will treat it as style_Normal.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_set_style(@NonNull GLKModel m, int val) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_set_style");
        }
        glk_set_style_stream(m, m.mStreamMgr.getCurrentOutputStream(), val);
    }

    /*========================================
     *                 STYLES
     *========================================*/

    /**
     * Changes the style of the given output stream.
     *
     * @param str - stream for style change.
     * @param val - should be one of the eleven style constants. However,
     *            any value is actually legal; if the interpreter does not
     *            recognize the style value, it will treat it as style_Normal.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_set_style_stream(@NonNull GLKModel m, int str, int val) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_set_style_stream " + str + ": " + GLKConstants.styleToString(val));
        }
        GLKOutputStream s = m.mStreamMgr.getOutputStream(str);
        if (s == null) {
            GLKLogger.warn("glk_set_style_stream: invalid stream id: " + str);
            return;
        }
        if (val < 0 || val > GLKConstants.style_NUMSTYLES - 1) {
            val = GLKConstants.style_Normal;
        }
        s.setStyle(val);
    }

    /**
     * Sets a hint about the appearance of a given style for a given type of window.
     * <p>
     * There is no guarantee that the hint will be honored.
     * <p/>
     * This does *not* affect *existing* windows. It affects the windows which you create subsequently.
     *
     * @param wintype - one of wintype_AllTypes, wintype_TextBuffer, wintype_TextGrid, or wintype_Graphics.
     * @param style   - one of the eleven style constants.
     * @param hint    - one of the nine stylehint constants - stylehint_Indentation, stylehint_ParaIndentation, stylehint_Justification,
     *                stylehint_Size, stylehint_Weight, stylehint_Oblique, stylehint_Proportional, stylehint_TextColor, stylehint_BackColor,
     *                or stylehint_ReverseColor. Again, when passing a style hint to a Glk function, any value is actually
     *                legal. If the interpreter does not recognize the stylehint value, it will ignore it.
     * @param val     - the value to set (meaning varies depending on the hint)
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_stylehint_set(@NonNull GLKModel m, int wintype, int style, int hint, int val) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS) {
            GLKLogger.debug("glk_stylehint_set: " + GLKConstants.wintypeToString(wintype) + ", " +
                    GLKConstants.styleToString(style) + ", " + GLKConstants.stylehintToString(hint) + ", " + val);
        }

        GLKStyleSpan[] styles;
        if (wintype == GLKConstants.wintype_TextBuffer) {
            styles = m.mDefaultStylesTextBuf;
        } else if (wintype == GLKConstants.wintype_TextGrid) {
            styles = m.mDefaultStylesTextGrid;
        } else if (wintype == GLKConstants.wintype_AllTypes) {
            glk_stylehint_set(m, GLKConstants.wintype_TextBuffer, style, hint, val);
            glk_stylehint_set(m, GLKConstants.wintype_TextGrid, style, hint, val);
            return;
        } else {
            GLKLogger.warn("glk_stylehint_set: invalid wintype: " + wintype);
            return;
        }

        GLKStyleSpan st = styles[style];
        if (st == null) {
            boolean isTextGrid = (wintype == GLKConstants.wintype_TextGrid);
            st = new GLKStyleSpan(style, isTextGrid, isTextGrid ? m.mDefaultFontSizeTextGrid : m.mDefaultFontSizeTextBuf);
        } else {
            st = new GLKStyleSpan(st);
        }
        styles[style] = st;
        GLKStyleSpan.set_stylehint(hint, val, st, true);
    }

    /**
     * Clears any hint associated with a given style and window type.
     *
     * @param wintype - one of wintype_AllTypes, wintype_TextBuffer, wintype_TextGrid, or wintype_Graphics.
     * @param style   - one of the eleven style constants.
     * @param hint    - one of the nine stylehint constants.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_stylehint_clear(@NonNull GLKModel m, int wintype, int style, int hint) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_stylehint_clear: " + GLKConstants.wintypeToString(wintype) + ", " +
                    GLKConstants.styleToString(style) + ", " + GLKConstants.stylehintToString(hint));
        }

        GLKStyleSpan[] styles;
        if (wintype == GLKConstants.wintype_TextBuffer) {
            styles = m.mDefaultStylesTextBuf;
        } else if (wintype == GLKConstants.wintype_TextGrid) {
            styles = m.mDefaultStylesTextGrid;
        } else if (wintype == GLKConstants.wintype_AllTypes) {
            glk_stylehint_clear(m, GLKConstants.wintype_TextBuffer, style, hint);
            glk_stylehint_clear(m, GLKConstants.wintype_TextGrid, style, hint);
            return;
        } else {
            GLKLogger.warn("glk_stylehint_clear: invalid wintype: " + wintype);
            return;
        }

        GLKStyleSpan st = styles[style];
        if (st == null) {
            boolean isTextGrid = (wintype == GLKConstants.wintype_TextGrid);
            st = new GLKStyleSpan(style, isTextGrid, isTextGrid ? m.mDefaultFontSizeTextGrid : m.mDefaultFontSizeTextBuf);
        } else {
            st = new GLKStyleSpan(st);
        }
        styles[style] = st;
        GLKStyleSpan.clear_stylehint(hint, st);
    }

    /**
     * Sets the foreground and background colors for all new text printed to
     * the current output stream.
     *
     * @param fg - new foreground colour, using ZMachine colour code
     * @param bg - new background colour, using ZMachine colour code
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glkplus_set_zcolors(@NonNull GLKModel m, int fg, int bg) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS || DEBUG_GLKPLUS_CALLS) {
            GLKLogger.debug("glkplus_set_zcolors");
        }
        glkplus_set_zcolors_stream(m, m.mStreamMgr.getCurrentOutputStream(), fg, bg);
    }

    /**
     * Sets the foreground and background colors for all new text printed to
     * a given output stream.
     *
     * @param str - the output stream to change.
     * @param fg  - new foreground colour, using ZMachine colour code
     * @param bg  - new background colour, using ZMachine colour code
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glkplus_set_zcolors_stream(@NonNull GLKModel m, int str, int fg, int bg) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS || DEBUG_GLKPLUS_CALLS) {
            GLKLogger.debug("glkplus_set_zcolors_stream " + str + ": " + GLKConstants.zcolorToString(fg) + ", " + GLKConstants.zcolorToString(bg));
        }
        GLKTextWindowM w = m.mStreamMgr.getTextWindow(str);
        if (w == null) {
            GLKLogger.warn("glkplus_set_zcolors_stream: invalid stream id: " + str);
            return;
        }
        w.setZStyleColors(fg, bg);
    }

    /**
     * Sets reverse video style for all new text printed to the current output stream.
     *
     * @param reverse - if 0, text is printed normally. if 1, text is reversed.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glkplus_set_reversevideo(@NonNull GLKModel m, int reverse) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS || DEBUG_GLKPLUS_CALLS) {
            GLKLogger.debug("glkplus_set_reversevideo");
        }
        glkplus_set_reversevideo_stream(m, m.mStreamMgr.getCurrentOutputStream(), reverse);
    }

    /**
     * Sets reverse video style for all new text printed to the given stream.
     *
     * @param str     - the output stream to change.
     * @param reverse - if 0, text is printed normally. if 1, text is reversed.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glkplus_set_reversevideo_stream(@NonNull GLKModel m, int str, int reverse) {
        if (DEBUG_GLK_CALLS || DEBUG_GLK_WINDOWS || DEBUG_GLKPLUS_CALLS) {
            GLKLogger.debug("glkplus_set_reversevideo_stream " + str + ": " + reverse);
        }
        GLKTextWindowM w = m.mStreamMgr.getTextWindow(str);
        if (w == null) {
            GLKLogger.warn("glkplus_set_reversevideo_stream: invalid stream id: " + str);
            return;
        }
        w.setStyleReverse(reverse != 0);
    }

    /**
     * Compares the two styles in the given window.
     *
     * @param win   - window to test
     * @param styl1 - first style
     * @param styl2 - second style
     * @return TRUE (1) if the two styles are visually distinguishable in the
     * given window, otherwise FALSE (0).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_style_distinguish(@NonNull GLKModel m, int win, int styl1, int styl2) {
        GLKLogger.error("TODO: glk_style_distinguish");
        return 0;
    }

    /**
     * Tests an attribute of one style in the given window.
     * <p>
     * The library may not be able to determine the attribute; if not, this returns
     * FALSE (0). If it can, it returns TRUE (1) and stores the value in the
     * location pointed at by result. [[As usual, it is legal for result to be
     * NULL, although fairly pointless.]]
     *
     * @param win    - window to test
     * @param styl   - style to test
     * @param hint   - attribute to test
     * @param result - stores the result of the comparison (may be NULL)
     * @return TRUE (1) if success, FALSE (0) otherwise.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int glk_style_measure(@NonNull GLKModel m, int win, int styl, int hint, @Nullable int[] result) {
        if (result == null) {
            // TODO: while it's silly not to include a result reference, we should
            // still return the correct value
            GLKLogger.warn("glk_style_measure: was passed a NULL result argument.");
            return 0;
        }
        result[0] = 0;
        GLKNonPairM np = m.mStreamMgr.getNonPairWindow(win);
        if (np == null) {
            GLKLogger.warn("glk_style_measure: invalid window id: " + win);
            return 0;
        }
        boolean ret = np.styleMeasure(styl, hint, result);
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_style_measure (win = " + win + ", styl = " + GLKConstants.styleToString(styl) +
                    ", hint = " + GLKConstants.stylehintToString(hint) + ") => " + result[0]);
        }
        return ret ? 1 : 0;
    }

    /**
     * Waits for an event.
     * <p/>
     * There are some events which can arrive at any time. This is why you must always
     * call this function in a loop, and continue the loop until you get the event you really want.
     *
     * @param event - structure to store the event in. Must not be <code>null</code>.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_select(@NonNull GLKModel m, @Nullable GLKEvent event) throws InterruptedException {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_select");
        }

        if (event == null) {
            GLKLogger.warn("glk_select: called with NULL event parameter. Will return and hope for the best!...");
            return;
        }

        // We only exit once we have received a requested event or the timer event
        boolean done = false;
        try {
            while (!done) {
                m.updateView();

                // get the next event, waiting if necessary
                GLKEvent e = mEventQueue.take();

                // Process
                if (e.type == GLKConstants.evtype_Debug) {
                    processUIDebugEvent(m, e);
                } else {
                    // Mouse click, line input, key press, timer or arrange event.
                    event.copy(e);
                    done = true;
                    GLKTextWindowM win = m.mStreamMgr.getTextWindow(e.win);
                    if (win != null) {
                        switch (e.type) {
                            case GLKConstants.evtype_CharInput:
                                win.cancelCharEvent();
                                break;
                            case GLKConstants.evtype_LineInput:
                                win.cancelLineEvent(null);
                                break;
                        }
                    }
                }
            }
        } catch (InterruptedException e1) {
            // Ave Caesar, morituri te salutamus..!
            if (m.mTerpIsJava) {
                stopTerpJava(m, 2);
                throw new InterruptedException();
            } else {
                stopTerp(2);  // this call doesn't return
            }
        }
    }

    /**
     * Checks if an internally-spawned event is available. If so, it stores
     * it in the structure pointed to by event. If not, it sets event->type to
     * evtype_None. Either way, it returns almost immediately.
     * <p/>
     * At the moment, glk_select_poll() checks for evtype_Timer, and possibly
     * evtype_Arrange and evtype_SoundNotify events.
     *
     * @param event - when function returns, this stores the captured event.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_select_poll(@NonNull GLKModel m, @NonNull GLKEvent event) throws InterruptedException {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_select_poll");
        }

        // By default, we assume no valid event
        event.type = GLKConstants.evtype_None;
        event.win = 0;
        event.val1 = 0;
        event.val2 = 0;

        // Check if there's an event available
        GLKEvent e = mEventQueue.peek();
        if (e != null) {
            if (e.type == GLKConstants.evtype_Arrange ||
                    e.type == GLKConstants.evtype_SoundNotify ||
                    e.type == GLKConstants.evtype_Redraw ||
                    e.type == GLKConstants.evtype_Timer) {
                try {
                    event.copy(mEventQueue.take());
                } catch (InterruptedException e1) {
                    // Ave Caesar, mortiuri te salutamus..!
                    if (m.mTerpIsJava) {
                        stopTerpJava(m, 2);
                        throw new InterruptedException();
                    } else {
                        stopTerp(2); // this call doesn't return
                    }
                }
            }
        }
    }

    /**
     * Requests input of a Latin-1 or Unicode character or special key.
     * <p>
     * A window cannot have requests for both character and line input at the
     * same time. Nor can it have requests for character input of both types (Latin-1
     * and Unicode). It is illegal to call glk_request_char_event() if the window already
     * has a pending request for either character or line input.
     *
     * @param win - a text buffer, text grid or graphics window.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_request_char_event(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_request_char_event " + win);
        }

        GLKNonPairM w = m.mStreamMgr.getNonPairWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_request_char_event: invalid window id: " + win);
            return;
        }
        w.requestCharEvent();
        m.setWindowFocus(win);
    }

    /**
     * Cancels a pending request for character input. (Either Latin-1 or
     * Unicode.)
     * <p>
     * For convenience, it is legal to call glk_cancel_char_event() even
     * if there is no character input request on that window. Glk will ignore the
     * call in this case.
     *
     * @param win - a text buffer, text grid or graphics window.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_cancel_char_event(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_cancel_char_event " + win);
        }

        GLKNonPairM w = m.mStreamMgr.getNonPairWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_cancel_char_event: invalid window id: " + win);
            return;
        }
        w.cancelCharEvent();
    }

    /**
     * Requests input of a line of Latin-1 or Unicode characters
     *
     * @param win     - either a text buffer or text grid window.
     * @param buf     - space where the line input will be stored (must not be NULL.)
     * @param initlen - if nonzero, then the first initlen Latin-1 or Unicode codepoints of buf will be entered as
     *                pre-existing input -- just as if the player had typed them himself.
     * @param unicode - whether the requested line will be Latin-1 or Unicode.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_request_line_event(@NonNull GLKModel m, int win, @NonNull ByteBuffer buf, int initlen, boolean unicode) {
        // If this is called from JNI, then the Bytebuffer is freed by relevant window when line event is
        // cancelled, window is destroyed or a new line event is set.
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_request_line_event " + win + ", initlen = " + initlen + ", uni = " + unicode);
        }

        GLKTextWindowM w = m.mStreamMgr.getTextWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_request_line_event: invalid window id: " + win);
            return;
        }
        w.requestLineEvent(buf, initlen, unicode);
        m.setWindowFocus(win);
    }

    /**
     * Cancels a pending request for line input. (Either Latin-1 or Unicode.)
     * <p/>
     * The event pointed to by the event argument will be filled in as if the
     * player had hit enter, and the input composed so far will be stored in the
     * buffer (unless event is passed in as NULL).
     *
     * @param win   - either a text buffer or text grid window.
     * @param event - object to store existing input back into (or may be NULL).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_cancel_line_event(@NonNull GLKModel m, int win, @Nullable GLKEvent event) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_cancel_line_event " + win);
        }
        GLKTextWindowM w = m.mStreamMgr.getTextWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_window_cancel_line_event: invalid window id: " + win);
            return;
        }
        w.cancelLineEvent(event);
    }

    /**
     * Normally, after line input is completed or cancelled in a buffer window,
     * the library ensures that the complete input line (or its latest state,
     * after cancelling) is displayed at the end of the buffer, followed by a
     * newline. This call allows you to suppress this behavior.
     *
     * @param win - a text buffer window (this function has no effect on any other window types).
     * @param val - If the val argument is zero, all *subsequent* line input requests in
     *            the given window will leave the buffer unchanged after the input
     *            is completed or cancelled; the player's input will not be printed.
     *            If val is nonzero, subsequent input requests will have the normal printing behavior.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_set_echo_line_event(@NonNull GLKModel m, int win, int val) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_set_echo_line_event win = " + win + ", val = " + val);
        }
        GLKTextBufferM w = m.mStreamMgr.getTextBuffer(win);
        if (w == null) {
            GLKLogger.warn("glk_set_echo_line_event: invalid window id: " + win);
            return;
        }
        w.setShowCompletedInput(val != 0);
    }

    /**
     * If a window has a pending request for line input, the player can generally hit the enter key
     * (in that window) to complete line input. The details will depend on the platform's native user
     * interface.  It is possible to request that other keystrokes complete line input as well. (This
     * allows a game to intercept function keys or other special keys during line input.) To do this,
     * call glk_set_terminators_line_event(), and pass an array of count keycodes. These must all be
     * special keycodes (see section 2.4, "Character Input"). Do not include regular printable
     * characters in the array, nor keycode_Return (which represents the default enter key and will
     * always be recognized). To return to the default behavior, pass a NULL or empty array.
     * <p>
     * The glk_set_terminators_line_event() affects subsequent line input requests in the given window.
     * It does not affect a pending line input request.
     *
     * @param win      - either a text buffer or text grid window.
     * @param keycodes - keycodes to recognise as terminators (or NULL to return to default).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_set_terminators_line_event(@NonNull GLKModel m, int win, @NonNull long[] keycodes) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_set_terminators_line_event " + win);
            StringBuilder sb = new StringBuilder();
            sb.append("\tcodes: ");
            for (long i : keycodes) {
                sb.append(" ").append(String.valueOf(i));
            }
            GLKLogger.debug(sb.toString());
        }
        GLKTextWindowM w = m.mStreamMgr.getTextWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_set_terminators_line_event: invalid window id: " + win);
            return;
        }
        w.setTerminators(keycodes);
    }

    /**
     * Requests mouse input.
     *
     * @param win - either a text grid or graphics window.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_request_mouse_event(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_request_mouse_event " + win);
        }
        GLKStream s = m.mStreamMgr.getStream(win);
        if (s == null) {
            GLKLogger.warn("glk_request_mouse_event: invalid window id: " + win);
            return;
        }

        if (s instanceof GLKGraphicsM) {
            ((GLKGraphicsM) s).requestMouseEvent();
            return;
        }
        if (s instanceof GLKTextGridM) {
            ((GLKTextGridM) s).requestMouseEvent();
        }
    }

    /**
     * Cancels a pending request for mouse input.
     *
     * @param win - either a text grid or graphics window.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_cancel_mouse_event(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_cancel_mouse_event " + win);
        }
        GLKStream s = m.mStreamMgr.getStream(win);
        if (s == null) {
            GLKLogger.warn("glk_cancel_mouse_event: invalid window id: " + win);
            return;
        }

        if (s instanceof GLKGraphicsM) {
            ((GLKGraphicsM) s).cancelMouseEvent();
            return;
        }
        if (s instanceof GLKTextGridM) {
            ((GLKTextGridM) s).cancelMouseEvent();
        }
    }

    /**
     * Requests that an event be sent at fixed intervals, regardless of
     * what the player does.
     * <p/>
     * Initially, there is no timer and you get no timer events. If you call
     * glk_request_timer_events(N), with N not 0, you will get timer events about
     * every N milliseconds thereafter.
     * <p/>
     * Timer events do not stack up. If you spend 10N milliseconds doing
     * computation, and then call glk_select(), you will not get ten timer events
     * in a row. The library will simply note that it has been more than N
     * milliseconds, and return a timer event right away. If you call glk_select()
     * again immediately, it will be N milliseconds before the next timer event.
     *
     * @param millisecs - floor of number of millisecs to wait before firing a timer event
     *                  object (if 0, timer events are cancelled).
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_request_timer_events(@NonNull GLKModel m, long millisecs) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_request_timer_events " + millisecs);
        }
        if (m.mTimer != null) {
            m.mTimer.cancel();
        }
        if (millisecs != 0) {
            m.mTimer = new Timer(true); // run as daemon so if everything else dies won't prolong lifetime of the app
            m.mTimer.schedule(new OnTimer(), millisecs, millisecs);
        }
    }

    /**
     * Requests a hyperlink event on a window.
     *
     * @param win - window to request event.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_request_hyperlink_event(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_request_hyperlink_event " + win);
        }
        GLKTextWindowM w = m.mStreamMgr.getTextWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_request_hyperlink_event: invalid win id: " + win);
            return;
        }
        w.requestHyperlinkEvent();
    }

    /**
     * Cancels a hyperlink event on a window.
     *
     * @param win - window to cancel event.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glk_cancel_hyperlink_event(@NonNull GLKModel m, int win) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_cancel_hyperlink_event " + win);
        }
        GLKTextWindowM w = m.mStreamMgr.getTextWindow(win);
        if (w == null) {
            GLKLogger.warn("glk_cancel_hyperlink_event: invalid win id: " + win);
            return;
        }
        w.cancelHyperlinkEvent();
    }

    /**
     * Retrieves information about an image.
     * <p/>
     * You should always use this function to measure the size of images when
     * you are creating your display. Do this even if you created the images, and
     * you know how big they "should" be. This is because images may be scaled in
     * translating from one platform to another, or even from one machine to another.
     *
     * @param image - the identifier of the image resource.
     * @param sz    - may be null; if you pass a Point object and the image exists, this
     *              will contain the width (x) and height (y) of the image (in dp) when the call returns.
     * @return <code>true</code> if the image exists; <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static boolean glk_image_get_info(@NonNull GLKModel m, int image, @Nullable Point sz) {
        if (sz != null)
            sz.x = sz.y = 0;

        final BitmapFactory.Options options = new BitmapFactory.Options();
        boolean avail = m.mResourceMgr.getImageSize(image, options);

        if (avail) {
            if (sz != null) {
                sz.x = options.outWidth;
                sz.y = options.outHeight;
            }
            if (DEBUG_GLK_CALLS) {
                GLKLogger.debug("glk_image_get_info: " + image +
                        ((sz != null) ? " => (w = " + sz.x + ", h = " + sz.y + ")" :
                                " => sz is null"));
            }
            return true;
        } else {
            if (DEBUG_GLK_CALLS) {
                GLKLogger.debug("glk_image_get_info: " + image + " => not available");
            }
            return false;
        }
    }

    /*========================================
     *                 IMAGES
     *========================================*/

    /**
     * Draws the given image resource in the given graphics or text buffer window.
     * <p/>
     * The position of the image is given by val1 and val2, but their meaning varies depending on
     * what kind of window you are drawing in.  For graphics windows, it is legitimate for part
     * of the image to fall outside the window; the excess is not drawn. Note that val1 and val2 are signed
     * arguments, so you can draw an image which falls outside the left or top edge of the window, as well as
     * the right or bottom.
     *
     * @param win   - a text buffer window or graphics window.
     * @param image - the identifier of the image resource to draw.
     * @param val1  - if win is a graphics window this is the X coordinate (in dp);
     *              otherwise it is an image alignment constant (imagealign_InlineUp,
     *              imagealign_InlineDown, imagealign_InlineCenter, imagealign_MarginLeft or imagealign_MarginRight).
     * @param val2  - if win is a graphics window this is the Y coordinate (in dp); otherwise it is ignored.
     * @return <code>true</code> if the operation succeeded; <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static boolean glk_image_draw(@NonNull GLKModel m, int win, int image, int val1, int val2) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_image_draw " + win + ": " + image + " (val1 = " + val1 + ", val2 = " + val2 + ")");
        }
        GLKStream s = m.mStreamMgr.getStream(win);
        if (s == null || !(s instanceof GLKTextBufferM || s instanceof GLKGraphicsM)) {
            GLKLogger.warn("glk_image_draw: invalid window id: " + win);
            return false;
        }
        GLKDrawable gfx = m.mResourceMgr.getImage(String.valueOf(image), 0, 0, true);
        if (gfx == null) {
            return false;
        }
        if (s instanceof GLKTextBufferM) {
            ((GLKTextBufferM) s).drawPicture(gfx, val1, m.mAutoResizeLargeImages);
        } else {
            DisplayMetrics dm = m.getApplicationContext().getResources().getDisplayMetrics();
            int xPx = GLKUtils.dpToPx(val1, dm);
            int yPx = GLKUtils.dpToPx(val2, dm);
            ((GLKGraphicsM) s).drawPicture(gfx, xPx, yPx, m.mAutoResizeLargeImages);
        }
        return true;
    }

    /**
     * Similar to glk_image_draw(), but it scales the image to the given
     * width and height, instead of using the image's standard size. You can
     * measure the standard size with glk_image_get_info().
     *
     * @param win    - a text buffer window or graphics window.
     * @param image  - the identifier of the image resource to draw.
     * @param val1   - if win is a graphics window this is the X coordinate (in dp); otherwise it is an image alignment constant (imagealign_InlineUp,
     *               imagealign_InlineDown, imagealign_InlineCenter, imagealign_MarginLeft or imagealign_MarginRight).
     * @param val2   - if win is a graphics window this is the Y coordinate (in dp); otherwise it is ignored.
     * @param width  - width of scaled image (in dp, should be >= 0, if 0 nothing drawn).
     * @param height - height of scaled image (in dp, should be >= 0, if 0 nothing drawn).
     * @return <code>true</code> if the operation succeeded; <code>false</code> otherwise.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static boolean glk_image_draw_scaled(@NonNull GLKModel m, int win, int image, int val1, int val2, int width, int height) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glk_image_draw_scaled " + win + ": " + image + " (val1 = " + val1 + ", val2 = " + val2 + ", w = " + width + ", h = " + height + ")");
        }
        if (width <= 0 || height <= 0) {
            GLKLogger.warn("glk_image_draw_scaled: width or height <= 0: not drawing image.");
            return false;
        }
        GLKStream s = m.mStreamMgr.getStream(win);
        if (s == null || !(s instanceof GLKTextBufferM || s instanceof GLKGraphicsM)) {
            GLKLogger.warn("glk_image_draw_scaled: invalid window id: " + win);
            return false;
        }
        GLKDrawable gfx = m.mResourceMgr.getImage(String.valueOf(image), width, height, true);
        if (gfx == null) {
            return false;
        }
        if (s instanceof GLKTextBufferM) {
            ((GLKTextBufferM) s).drawPicture(gfx, val1, m.mAutoResizeLargeImages);
        } else {
            DisplayMetrics dm = m.getApplicationContext().getResources().getDisplayMetrics();
            int xPx = GLKUtils.dpToPx(val1, dm);
            int yPx = GLKUtils.dpToPx(val2, dm);
            ((GLKGraphicsM) s).drawPicture(gfx, xPx, yPx, m.mAutoResizeLargeImages);
        }
        return true;
    }

    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glkplus_set_html_mode(@NonNull GLKModel m, boolean on) {
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glkplus_set_html_mode " + on);
        }
        m.mHTMLOutput = on;
        m.mStreamMgr.setHTMLForAllTextBuffers(m, on);
    }

    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void glkplus_new_html_page(@NonNull GLKModel m, int mainWin) {
        // Move onto a new HTML page
        // I.e. any banners are removed and the screen is cleared
        if (DEBUG_GLK_CALLS) {
            GLKLogger.debug("glkplus_new_html_page " + mainWin);
        }
        tadsban_html_delete_all(m);
        glk_window_clear(m, mainWin);
    }

    /*========================================
     *     NON-OFFICIAL GLK EXTENSIONS
     ========================================*/

    /**
     * Play a sound from a TADS file.
     *
     * @param src       - the name of the resource containing the sound data. The resource
     *                  name must end in .WAV for a wave file; .MID or .MIDI for a MIDI file;
     *                  .MPG, .MP2, or .MP3 for an MPEG audio file; and .OGG for an Ogg Vorbis file.
     * @param layer     - the layer containing the sound. The value of the LAYER attribute must be FOREGROUND, BGAMBIENT, AMBIENT, or BACKGROUND. Although
     *                  you should always specify a layer for each sound, HTML TADS can sometimes infer the correct LAYER attribute for
     *                  a sound based on its other attributes.
     * @param random    - the probability of playing a sound in the ambient layer. Usually, you will want a sound in the ambient layer to be played randomly
     *                  from time to time; this attribute specifies the probability that the sound will be played at any given time. This attribute
     *                  takes a value from 1 to 100.
     * @param repeat    - how many times a sound should play. This can be used for all layers. This attribute takes a numeric value specifying the
     *                  number of times to play the sound, or a value of -1 (LOOP) to indicate that the sound plays repeatedly until cancelled. If you don't specify
     *                  a REPEAT attribute for an ambient sound, and the sound has a RANDOM setting, the default REPEAT=LOOP will be used. If you do
     *                  specify a REPEAT parameter for an ambient sound, the sound will be played a maximum of the given number of times; the
     *                  sound will still play only when the system randomly chooses the play it, the probability of which is controlled through
     *                  the RANDOM attribute.
     * @param sequence  -  used to control the order of repetition for a group of background sounds; it's meant primarily for use with the
     *                  background and bgambient layers. The SEQUENCE value can be REPLACE, RANDOM, or CYCLE. REPLACE causes the sound to
     *                  remove any previous background sounds from the queue when the new sound starts. RANDOM leaves any previous sounds in
     *                  the queue, assuming they have enough REPEAT cycles remaining; when the new sound is finished, if no other background
     *                  sounds are waiting to be played, the system randomly picks one of the sounds remaining in the queue and plays it again.
     *                  CYCLE is similar to random, but goes back to the first sound in the queue when the new sound is done.
     * @param interrupt - if TRUE, the sound is to abort any currently playing sound in the layer and start the new sound immediately. You should
     *                  usually only use INTERRUPT with foreground sounds.
     * @param cancel    - turns off all currently queued sounds. If NULL cancels all queued sounds in all layers. You could
     *                  use this, for example, when switching to a new room, to turn off all sounds. You can also
     *                  specify a layer with CANCEL to specify that sounds in that particular layer are to be cancelled; the layer
     *                  values FOREGROUND, AMBIENT, and BACKGROUND are allowed.
     * @param alt       - lets you specify a textual description of the sound. This could be used by a version of the interpreter that doesn't
     *                  support sound to display a description of the sound (the Windows version of the interpreter currently ignores this attribute).
     * @param fadein    - lets you fade the track in at the beginning. The duration of the fade, in seconds, is given as the value of the attribute.
     *                  The duration can have a fractional part.  When a sound is repeated with the REPEAT attribute, the fade-in applies only to the first
     *                  iteration.
     * @param fadeout   - lets you fade the track out at the end. The duration of the fade, in seconds, is given as the value of the attribute.
     *                  The duration can have a fractional part. When a sound is repeated with the REPEAT attribute, the fade-out applies only to the last
     *                  iteration.
     * @param volume    - set the relative playback volume for a sound. The value is an integer ranging from 0 to 100, where 0 is complete silence,
     *                  100 is the full, unattenuated level as recorded in the sound file, and values in between are proportionally reduced volume,
     *                  akin to using a speaker volume dial. (Values outside the 0-100 range are invalid.) VOLUME only affects the track
     *                  it applies to: it doesn't change the physical speaker volume level of the computer, it doesn't affect any other tracks that
     *                  might be playing concurrently in the game, and it doesn't persist for subsequent tracks played in the same layer. The purpose
     *                  of VOLUME is to let you control the relative loudness of sounds when you're playing back multiple tracks at the same time,
     *                  to achieve the mixing effect you intend.
     * @return true if success, false otherwise
     */
    public static boolean playSoundTADS(@NonNull GLKModel m, @Nullable String src,
                                        @Nullable String layer, int random, int repeat, String sequence, boolean interrupt,
                                        String cancel, String alt, String fadein, String fadeout, int volume) {
        //  if (DEBUG_GLK_CALLS)
        GLKLogger.error("FIXME: playSoundTADS: src = " + src + ", layer = " + layer + ", random = " + random + ", repeat = " + repeat +
                ", sequence = " + sequence + ", interrupt = " + interrupt + ", cancel = " + cancel + ", alt = " + alt +
                ", fadein = " + fadein + ", fadeout = " + fadeout + ", volume = " + volume);

        // get the sound stream (initialise a new one if necessary)
        // either FOREGROUND, BGAMBIENT, AMBIENT or BACKGROUND
        if (layer == null) {
            return false;
        }

        int schan;
        switch (layer.toLowerCase()) {
            case "foreground":
                schan = 0;
                break;
            case "bgambient":
                schan = 1;
                break;
            case "ambient":
                schan = 2;
                break;
            case "background":
            default:
                schan = 3;
                break;
        }

        GLKSoundStream s;
        if (m.tadsSoundLayers[schan] == 0) {
            m.tadsSoundLayers[schan] = glk_schannel_create(m, 0);
        }
        s = m.mStreamMgr.getSoundStream(m.tadsSoundLayers[schan]);
        if (s == null) {
            GLKLogger.error("playSoundTADS failed: could not get sound stream for id " + m.tadsSoundLayers[schan]);
            return false;
        }

        if (random >= 0) {
            s.setProbability(random);
        }

        // If it's currently playing, stop it
        if (interrupt && s.isPlaying()) {
            s.stop();
        }

        if (volume >= 0) {
            // map TADS volume (0-100) to android which ranges from 0 to 1
            float androidVol = (float) volume / 100f;
            s.setVolume(androidVol, androidVol);
        }

        if (repeat == 0 || src == null) {
            // we go no further!
            return true;
        }

        if (m.mResourceMgr.getSound(src, s)) {
            s.setRepeating(repeat);
            s.start();
            return true;
        }

        return false;
    }

    public static boolean playSoundADRIFT(@NonNull GLKModel m, @NonNull String src, int startStop, int schan, int repeat) {
        // startStop = 0 means stop, 1 means start, 2 means pause
        if (schan > m.tadsSoundLayers.length) {
            return false;
        }
        GLKSoundStream s;
        if (m.tadsSoundLayers[schan] == 0) {
            m.tadsSoundLayers[schan] = glk_schannel_create(m, src.hashCode());
        }
        s = m.mStreamMgr.getSoundStream(m.tadsSoundLayers[schan]);
        if (s == null) {
            GLKLogger.error("playSoundADRIFT failed: could not get sound stream for id " + m.tadsSoundLayers[schan]);
            return false;
        }

        switch (startStop) {
            case 0:
                // stop
                if (s.isPlaying()) {
                    s.stop();
                    return true;
                }
                break;
            case 1:
                // start
                // If we're being asked to play the same tune as the one that is
                // already playing, we don't want to do anything. The trick is
                // how to detect it. At the moment we just do a sloppy hack.
                int hash = src.hashCode();  // TODO: this doesn't guarantee uniqueness!
                if (s.getRock() != hash || !s.isPlaying()) {
                    s.setVolume(100f, 100f);
                    if (m.mResourceMgr.getSound(src, s)) {
                        s.setRepeating(repeat);
                        s.setRock(hash);
                        s.start();
                        return true;
                    }
                }
                break;
            case 2:
                // pause
                if (s.isPlaying()) {
                    s.pause();
                    return true;
                }
                break;
        }

        return false;
    }

    public static void tadsban_html_set_root_if_null(@NonNull GLKModel m, @NonNull GLKTextWindowM rootWin) {
        if (m.mHTMLBanners == null) {
            m.mHTMLBannerRoot = rootWin;
            m.mHTMLBannerRootID = rootWin.getStreamId();
            m.mHTMLBanners = new HashMap<>();
        }
    }

    /*========================================
     * NON-OFFICIAL GLK EXTENSIONS
     * FOR TADS 2/3 BANNERS
     *
     * There are two types of function:
     *   1) functions used by TADS 3 (and called direct from the JNI layer) are prefixed tadsban_XXX
     *   2) functions used by TADS 2 HTML are prefixed tadsban_html_XXX
     *
     *========================================*/

    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int tads_get_input(@NonNull GLKModel m, @NonNull byte[] buf, int initlen, int mainwin, int timer) throws InterruptedException {
        // If timer is 0, then there is no timeout
        // Returns a combination of zero, one or both flags:
        //          0x01:  TIMEOUT OCCURRED
        //          0x10:  BUFFER HAS CONTENTS
        //Logger.error("tads_get_input: buffer length = " + buf.length + ", initlen = " + initlen + ", mainwin = " + mainwin + ", timer = " + timer);
        int timeout = 0;
        int ret = 0;
        GLKEvent ev = new GLKEvent();

        // start timer and turn off line echo
        if (timer > 0) {
            glk_request_timer_events(m, timer);
            glk_set_echo_line_event(m, mainwin, 0);
        }

        // Convert any initial text in the input buffer from
        // the game's encoding to UTF-32. For TADS3, convert
        // from UTF-8. For TADS 2, convert from the preferred
        // encoding.
        byte[] bufUTF32 = new byte[4 * (buf.length + 1)];
        ByteBuffer bbUTF32 = ByteBuffer.wrap(bufUTF32);
        if (initlen > 0) {
            // TODO: This is a pretty silly and inefficient way to add any initial text to the buffer;
            // find a better way to do it
            ByteBuffer bbInit = ByteBuffer.wrap(bufUTF32, 0, initlen);
            String strInit = m.mCharsetMgr.getTADSString(bbInit, m.mGameFormat.equals("tads3"));
            m.mCharsetMgr.putGLKString(strInit, bbUTF32, true, false);
        }

        glk_request_line_event(m, mainwin, bbUTF32, initlen, true);

        do {
            glk_select(m, ev);
            if (ev.type == GLKConstants.evtype_Timer) {
                ret |= 0x01;
                glk_cancel_line_event(m, mainwin, ev);
            }
        } while (ev.type != GLKConstants.evtype_LineInput);

        // Convert the UTF-32 input buffer back to the game's encoding
        // For TADS 3, convert back to UTF-8. For TADS 2,
        // convert back to the preferred encoding.
        bbUTF32.rewind();
        bbUTF32.limit(ev.val1 * 4);
        String strResult = m.mCharsetMgr.getGLKString(bbUTF32, true);
        ret |= 0x10;
        if (strResult.length() > 0) {
            ByteBuffer tmp = m.mCharsetMgr.putTADSString(strResult, m.mGameFormat.equals("tads3"));
            if (tmp != null) {
                int max = Math.min(tmp.limit(), buf.length - 4);
                tmp.get(buf, 0, max);
                for (int i = max; i < max + 4; i++) {
                    buf[i] = '\0';
                }
            } else {
                for (int i = 0; i < 4; i++) {
                    buf[i] = '\0';
                }
            }
        } else {
            for (int i = 0; i < 4; i++) {
                buf[i] = '\0';
            }
        }

        // stop timer and turn on line echo
        if (timer > 0) {
            glk_request_timer_events(m, 0);
            glk_set_echo_line_event(m, mainwin, 1);
        }

        return ret;
    }

    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tads_put_string(@NonNull GLKModel m, @NonNull byte[] buf) {
        tads_put_string_stream(m, buf, m.mStreamMgr.getCurrentOutputStream());
    }

    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tads_put_string_stream(@NonNull GLKModel m, @NonNull byte[] buf, int str) {
        GLKOutputStream os = m.mStreamMgr.getOutputStream(str);
        if (os == null) {
            GLKLogger.warn("tads_put_string_stream: invalid stream id: " + str);
            return;
        }

        // Just add the new text to our buffer.
        String ss = m.mCharsetMgr.getTADSString(ByteBuffer.wrap(buf), m.mGameFormat.equals("tads3"));
        os.putString(ss);
    }

    public static GLKTextWindowM tadsban_html_get_root(@NonNull GLKModel m) {
        return m.mHTMLBannerRoot;
    }

    /**
     * Creates a banner window
     *
     * @param parent    - the parent of this banner; this is the banner handle of
     *                  another banner window, or null.  If 'parent' is null, then the new
     *                  banner is a child of the main window, which the system creates
     *                  automatically at startup and which contains the main input/output
     *                  transcript.  The new banner's on-screen area is carved out of the
     *                  parent's space, according to the alignment and size settings of the new
     *                  window, so this determines how the window is laid out on the screen.
     * @param where     - is OS_BANNER_FIRST to make the new window the first child of its
     *                  parent; OS_BANNER_LAST to make it the last child of its parent;
     *                  OS_BANNER_BEFORE to insert it immediately before the existing banner
     *                  identified by handle in 'other'; or OS_BANNER_AFTER to insert
     *                  immediately after 'other'.  When BEFORE or AFTER is used, 'other' must
     *                  be another child of the same parent; if it is not, the routine should
     *                  act as though 'where' were given as OS_BANNER_LAST.
     * @param other     - is a banner handle for an existing banner window.  This is used
     *                  to specify the relative position among children of the new banner's
     *                  parent, if 'where' is either OS_BANNER_BEFORE or OS_BANNER_AFTER.  If
     *                  'where' is OS_BANNER_FIRST or OS_BANNER_LAST, 'other' is ignored.
     * @param wintype   - is the type of the window.  This is one of the
     *                  OS_BANNER_TYPE_xxx codes indicating what kind of window is desired.
     * @param align     - is the banner's alignment, given as an OS_BANNER_ALIGN_xxx
     *                  value.  Top/bottom banners are horizontal: they run across the full
     *                  width of the existing main text area.  Left/right banners are vertical:
     *                  they run down the full height of the existing main text area.
     * @param siz       - is the requested size of the new banner.
     * @param siz_units - The meaning of 'siz'depends on the value of 'siz_units',
     *                  which can be OS_BANNER_SIZE_PCT to
     *                  set the size as a percentage of the REMAINING space, or
     *                  OS_BANNER_SIZE_ABS to set an absolute size in the "natural" units of the
     *                  window.  The natural units vary by window type: for text and text grid
     *                  windows, this is in rows/columns of '0' characters in the default font
     *                  for the window.  Note that when OS_BANNER_SIZE_ABS is used in a text or
     *                  text grid window, the OS implementation MUST add the space needed for
     *                  margins and borders when determining the actual pixel size of the
     *                  window; in other words, the window should be large enough that it can
     *                  actually display the given number or rows or columns.
     * @param style     - is a combination of OS_BANNER_STYLE_xxx flags - see below.  The
     *                  style flags give the REQUESTED style for the banner, which might or
     *                  might not be respected, depending on the platform's capabilities, user
     *                  preferences, and other factors.  os_banner_getinfo() can be used to
     *                  determine which style flags are actually used.
     * @return the "handle" to the new banner window, which is an opaque value
     * that is used in subsequent os_banner_xxx calls to operate on the window.
     * Returns null if the window cannot be created.  An implementation is not
     * required to support this functionality at all, and can subset it if it
     * does support it (for example, an implementation could support only
     * top/bottom-aligned banners, but not left/right-aligned), so callers must
     * be prepared for this routine to return null.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int tadsban_create(@NonNull GLKModel m, int parent, int where,
                                     int other, int wintype, int align, int siz, int siz_units, long style) {
        // this method is very similar to glk_window_open but there are some subtle differences!
        // e.g. siz is interpreted as pixels or %, not line/col or %
        GLKLogger.error("FIXME: tadsban_create: don't ignore where and other arguments!");

        // Create a new window based on the specified window type
        GLKTextWindowM newWin;
        switch (wintype) {
            case GLKConstants.TADS_BANNER_TYPE_TEXT:
            case GLKConstants.TADS_BANNER_TYPE_STATUS: {
                // Text: This is an ordinary window that behaves much as the main game window: it has stream-oriented output,
                // so each bit of text written to the window is displayed at the end of any existing text in the window. Text
                // windows support HTML markups, to the same extent the main game window does. On full HTML interpreters,
                // text windows can display graphics, typefaces, text sizes, and so on - everything that the main
                // game window can display.
                GLKTextBufferM.GLKTextBufferBuilder builder = new GLKTextBufferM.GLKTextBufferBuilder();
                builder.setModel(m)
                        .setBGColor(m.mBackgroundColor);

                if (wintype == GLKConstants.TADS_BANNER_TYPE_TEXT) {
                    builder.setDefaultStyles(m.mDefaultStylesTextBuf)
                            .setGLKMultipliers(m.mTextBufWidthMultiplier, m.mTextBufHeightMultiplier);
                    builder.setLeadingMultiplier(m.mDefaultLeadingTextBuf);
                } else {
                    builder.setDefaultStyles(m.mDefaultStylesTextGrid)
                            .setGLKMultipliers(m.mTextGridWidthMultiplier, m.mTextGridHeightMultiplier);
                    builder.setLeadingMultiplier(m.mDefaultLeadingTextGrid);
                }

                builder.setCursorWidth(m.mCursorWidthPX)
                        .setSubPixelRendering(m.mSubPixel);
                newWin = builder.build();
                newWin.setPadding(m.mDefaultMarginsTextBufPX.left,
                        m.mDefaultMarginsTextBufPX.top,
                        m.mDefaultMarginsTextBufPX.right,
                        m.mDefaultMarginsTextBufPX.bottom);
                break;
            }
            case GLKConstants.TADS_BANNER_TYPE_TEXTGRID: {
                // This window emulates a character-mode terminal. This type of window displays only text, and uses a single
                // fixed-width font (determined by the operating system, or, on some systems, chosen by the user through
                // preferences mechanism). HTML is not interpreted in these windows; it is not possible to display
                // graphics in a text grid, even on a full HTML interpreter on a GUI system, and these windows can't
                // even display different typefaces or text sizes; the one available effect is that the text and
                // background colors can be controlled, on interpreters (and display devices) that support text colors.
                // In exchange for sacrificing all of the HTML formatting controls, text grid windows allow you to
                // control the exact positioning of text within the window: you can set the output position to anywhere
                // within the window for each bit of text you display in the window, and you can even overwrite text
                // previously displayed with new text. Text grids can thus be used for text animation and special text
                // formatting effects.
                GLKTextGridM.GLKTextGridBuilder builder = new GLKTextGridM.GLKTextGridBuilder();
                builder.setModel(m)
                        .setDefaultStyles(m.mDefaultStylesTextGrid)
                        .setBGColor(m.mBackgroundColor)
                        .setGLKMultipliers(m.mTextGridWidthMultiplier, m.mTextGridHeightMultiplier);
                builder.setLeadingMultiplier(m.mDefaultLeadingTextGrid)
                        .setCursorWidth(m.mCursorWidthPX)
                        .setSubPixelRendering(m.mSubPixel);
                newWin = builder.build();
                newWin.setPadding(m.mDefaultMarginsTextGridPX.left,
                        m.mDefaultMarginsTextGridPX.top,
                        m.mDefaultMarginsTextGridPX.right,
                        m.mDefaultMarginsTextGridPX.bottom);
                break;
            }
            default:
                GLKLogger.error("tadsban_create: did not recognise window type: " + wintype);
                return 0;
        }
        m.mStreamMgr.addStreamToPool(newWin);

        // Find the window to split
        GLKWindowM splitWin;
        if (parent != 0) {
            splitWin = m.mStreamMgr.getWindow(parent);
        } else {
            // split window is the main I/O window (not the root window)
            // try to find it
            splitWin = m.mStreamMgr.getFirstTextWindow();
        }
        if (splitWin == null) {
            GLKLogger.error("tadsban_create: could not find split window.");
            return 0;
        }

        // Create a new pair window with the same layout and size.
        // Also, if the split window was the root window, the new pair window
        // now becomes the root window.
        GLKPairM o = new GLKPairM(m);
        m.mStreamMgr.addStreamToPool(o);
        if (m.mRootWinID == parent) {
            m.mRootWinID = o.getStreamId();
        }

        // convert tads split method to GLK split method
        int method = GLKConstants.NULL;
        switch (align) {
            case GLKConstants.TADS_BANNER_ALIGN_TOP:
                method |= GLKConstants.winmethod_Above;
                break;
            case GLKConstants.TADS_BANNER_ALIGN_BOTTOM:
                method |= GLKConstants.winmethod_Below;
                break;
            case GLKConstants.TADS_BANNER_ALIGN_LEFT:
                method |= GLKConstants.winmethod_Left;
                break;
            case GLKConstants.TADS_BANNER_ALIGN_RIGHT:
                method |= GLKConstants.winmethod_Right;
                break;
            default:
                // error
                GLKLogger.error("tadsban_create: did not recognise banner alignment: " + align);
                return 0;
        }
        switch (siz_units) {
            case GLKConstants.TADS_BANNER_SIZE_ABS:
                method |= GLKConstants.winmethod_Fixed;
                break;
            case GLKConstants.TADS_BANNER_SIZE_PCT:
                method |= GLKConstants.winmethod_Proportional;
                break;
            default:
                GLKLogger.error("tadsban_create: did not recognise size units: " + siz_units);
                return 0;
        }
        switch (m.mBorderStyle) {
            case 0:
                // always
                break;
            case 1:
                // never
                method |= GLKConstants.winmethod_NoBorder;
                break;
            case 2:
                // always unless game says no
                if ((style & GLKConstants.TADS_BANNER_STYLE_BORDER) != GLKConstants.TADS_BANNER_STYLE_BORDER) {
                    method |= GLKConstants.winmethod_NoBorder;
                }
                break;
        }

        if (!o.openSplit(method, siz, true, newWin, splitWin)) {
            GLKLogger.error("tadsban_create: invalid split method: " + method);
            return 0;
        }

        if (m.mHTMLOutput && newWin instanceof GLKTextBufferM) {
            ((GLKTextBufferM) newWin).setHTMLOutput(m, true);
        }

        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_create: parent = " + parent +
                    ", where = " + GLKConstants.tadsBannerPosToString(where) + ", other = " + other +
                    ", wintype = " + GLKConstants.tadsBannerTypeToString(wintype) +
                    ", align = " + GLKConstants.tadsBannerAlignmentToString(align) +
                    ", siz = " + siz + ", siz_units = " + GLKConstants.tadsBannerSizeUnitToString(siz_units) +
                    ", style = " + GLKConstants.tadsBannerStyleToString(style) + " => " + newWin.getStreamId());
        }

        return newWin.getStreamId();
    }

    /**
     * BANNER takes an ID attribute that lets the document assign the banner an identifier. The identifier value is arbitrary; its
     * purpose is to name the banner, so that subsequent BANNER tags can refer to the same area on the screen. When HTML TADS
     * formats a BANNER tag, it first looks to see if it already has a banner on the screen with the same identifier; if so, it
     * clears all of the markups out of the area that the banner is using on the screen, and formats the new contents. If no
     * BANNER with the given ID is present, HTML TADS creates a new banner area, and displays the contents of the banner
     * in the new area; the system remembers the ID of the new banner, so that subsequent tags can replace the contents of the
     * banner with a new display.
     *
     * @param banID_HTML -  The identifier value is arbitrary; its purpose is to name the banner, so that subsequent
     *                   BANNER tags can refer to the same area on the screen.
     * @param align      -  Four values are possible: TOP, BOTTOM, LEFT, and RIGHT; the default value is TOP. These values determine where the banner goes.
     * @param siz        - BANNER takes HEIGHT and WIDTH attributes that let you specify the size of the banner on the screen. Because banners are always
     *                   constrained in one dimension according to their alignment, only one of HEIGHT or WIDTH will be meaningful for a particular banner. For
     *                   a horizontal banner (ALIGN=TOP or BOTTOM), only HEIGHT is meaningful, because horizontal banners always occupy the full width of
     *                   the input window. For a horizontal banner, if no HEIGHT attribute is present, BANNER sets the size of the banner's screen area to
     *                   be the same height as the contents. If a HEIGHT value is provided, it specifies the height of the banner in pixels. You can also
     *                   specify the height as a percentage of the main window height, by placing a percent sign ("%") after the height value. The HEIGHT
     *                   attribute can also be set to the value "PREVIOUS", rather than a pixel or percentage height. This specifies that, if banner is
     *                   already being displayed (i.e., the ID matches the ID of a previous BANNER), the height is left unchanged. If HEIGHT=PREVIOUS is
     *                   specified and the banner is not already displayed, the default behavior applies (i.e., the banner's height is set to the height
     *                   of the contents). For a vertical banner (ALIGN=LEFT or RIGHT), only WIDTH is meaningful. As with HEIGHT, WIDTH can specify a width
     *                   in pixels, a percentage of the main window's width, or the special value "PREVIOUS". If WIDTH is not specified for a vertical banner,
     *                   the system sets the width of the banner to the minimum width in which each "unbreakable" item will fit
     * @param border     - The BORDER attribute lets you specify that the banner should be drawn with a border at its inside edge. For a TOP banner, the
     *                   inside edge is at the bottom; for a LEFT banner, it's at the right edge; for a BOTTOM banner, it's at the top; and for a
     *                   RIGHT banner, it's at the left edge.
     */
    public static int tadsban_html_create(@NonNull GLKModel m, @NonNull String banID_HTML, int align, int siz, int siz_units, boolean border) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_html_create: banID_HTML = " + banID_HTML + ", align = " + align + ", size = " + siz + ", border = " + border);
        }

        if (m.mHTMLBanners == null) {
            return GLKConstants.NULL;
        }
        Integer banID_GLK = m.mHTMLBanners.get(banID_HTML);
        if (banID_GLK == null) {
            // We do a hack here to try to make TADS status bars (banners with ID = statusline) by default use same status bar settings
            // as specified by user for other terps
            banID_GLK = tadsban_create(m, m.mHTMLBannerRootID, GLKConstants.TADS_BANNER_FIRST, 0,
                    banID_HTML.toLowerCase().equals("statusline") ? GLKConstants.TADS_BANNER_TYPE_STATUS : GLKConstants.TADS_BANNER_TYPE_TEXT,
                    align, siz, siz_units, 0);
            m.mHTMLBanners.put(banID_HTML, banID_GLK);
        }
        return banID_GLK;
    }

    public static int tadsban_html_get_banner(@NonNull GLKModel m, @NonNull String banID_HTML) {
        if (m.mHTMLBanners == null) {
            return GLKConstants.NULL;
        }
        Integer banID_GLK = m.mHTMLBanners.get(banID_HTML);
        return (banID_GLK == null) ? GLKConstants.NULL : banID_GLK;
    }

    /**
     * Get the character width of the banner, for layout purposes.
     * <p>
     * This is not meaningful when the underlying window uses a proportional
     * font or varying fonts of different sizes.  When the size of text varies
     * in the window, the OS layer is responsible for word-wrapping and other
     * layout, in which case this simply returns zero.
     *
     * @param win - the banner.
     * @return the width of the banner, in character cells.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int tadsban_get_charwidth(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_get_charwidth: id = " + win);
        }

        GLKTextGridM w = m.mStreamMgr.getTextGrid(win);
        if (w == null) {
            GLKLogger.warn("tadsban_get_charheight: invalid window id: " + win);
            return 0;
        }
        Point sz = w.getGLKSize();
        return sz.x;
    }

    /**
     * Get the character height of a banner, for layout purposes.
     * <p>
     * This is not meaningful when the underlying window uses a proportional
     * font or varying fonts of different sizes.  When the size of text varies
     * in the window, the OS layer is responsible for word-wrapping and other
     * layout, in which case this simply returns zero.
     *
     * @param win - the banner.
     * @return the height of the banner, in character cells.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int tadsban_get_charheight(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_get_charheight: id = " + win);
        }

        GLKTextGridM w = m.mStreamMgr.getTextGrid(win);
        if (w == null) {
            GLKLogger.warn("tadsban_get_charheight: invalid window id: " + win);
            return 0;
        }
        Point sz = w.getGLKSize();
        return sz.y;
    }

    /**
     * Get information on a banner - fills in the information structure with
     * the banner's current settings.  Note that this should indicate the
     * ACTUAL properties of the banner, not the requested properties; this
     * allows callers to determine how the banner is actually displayed, which
     * depends upon the platform's capabilities and user preferences.
     *
     * @param win  - the banner.
     * @param info - the information structure to fill in.
     * @return true if the information was successfully obtained, false if
     * not.  This can return false if the underlying OS window has already
     * been closed by a user action, for example.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static int tadsban_getinfo(@NonNull GLKModel m, int win, @NonNull TADSBannerInfo info) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_getinfo: id = " + win);
        }

        GLKTextWindowM w = m.mStreamMgr.getTextWindow(win);
        if (w == null) {
            GLKLogger.error("tadsban_getinfo: invalid window id: " + win);
            return 0;
        }

        Point sz = w.getGLKSize();

        // Fill in the info object
        info.rows = sz.y;
        info.columns = sz.x;
        info.pix_width = w.mWidthGLK_PX;
        info.pix_height = w.mHeightGLK_PX;
        info.os_line_wrap = (w instanceof GLKTextBufferM) ? 1 : 0;
        GLKLogger.error("FIXME: tadsban_get_info: correctly set align and style attributes of info object.");

        return 1;
    }

    /**
     * Delete a banner.  This removes the banner from the display, which
     * requires recalculating the entire screen's layout to reallocate this
     * banner's space to other windows.  When this routine returns, the banner
     * handle is invalid and can no longer be used in any os_banner_xxx
     * function calls.
     *
     * @param win - the banner.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_delete(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_delete: id = " + win);
        }
        glk_window_close(m, win, null);
    }

    public static void tadsban_html_delete(@NonNull GLKModel m, @NonNull String id) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_html_delete: id = " + id);
        }
        if (m.mHTMLBanners != null) {
            Integer winID = m.mHTMLBanners.get(id);
            if (winID != null) {
                glk_window_close(m, winID, null);
                m.mHTMLBanners.remove(id);
            }
        }
    }

    public static void tadsban_html_delete_all(@NonNull GLKModel m) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_html_delete_all: " + m.mHTMLBanners.keySet());
        }
        if (m.mHTMLBanners != null) {
            for (int id : m.mHTMLBanners.values()) {
                glk_window_close(m, id, null);
            }
            m.mHTMLBanners.clear();
        }
    }

    /**
     * "Orphan" a banner.  This tells the osifc implementation that the caller
     * wishes to sever all of its ties with the banner (as part of program
     * termination, for example), but that the calling program does not
     * actually require that the banner's on-screen display be immediately
     * removed.
     *
     * @param win - the banner.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_orphan(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_orphan: id = " + win);
        }
        tadsban_delete(m, win);
    }

    /**
     * Clears the contents of a banner.
     *
     * @param win - the banner.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_clear(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_clear: id = " + win);
        }
        glk_window_clear(m, win);
    }

    /**
     * Set the text attributes in a banner, for subsequent text displays.
     * attr' is a (bitwise-OR'd) combination of OS_ATTR_xxx values.
     *
     * @param win  - the banner.
     * @param attr - the text attributes to set.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_set_attr(@NonNull GLKModel m, int win, int attr) {
        GLKLogger.error("FIXME: tadsban_set_attr: id = " + win + ", attr = " + attr);
    }

    /**
     * Set the text color in a banner, for subsequent text displays.  The 'fg'
     * and 'bg' colors are given as RGB or parameterized colors; see the
     * definition of os_color_t for details.
     * <p>
     * If the underlying renderer is HTML-enabled, then this should not be
     * used; the appropriate HTML code should simply be displayed to the
     * banner instead.
     *
     * @param win - the banner.
     * @param fg  - foreground colour.
     * @param bg  - background colour.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_set_color(@NonNull GLKModel m, int win, int fg, int bg) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_set_color: id = " + win + ", fg = " + fg + ", bg = " + bg);
        }

        // We don't implement this function as we are HTML-enabled.
    }

    /**
     * Set the screen color in the banner - this is analogous to the screen
     * color in the main text area.
     * <p>
     * If the underlying renderer is HTML-enabled, then this should not be
     * used; the HTML <BODY> tag should be used instead.
     *
     * @param win   - the banner.
     * @param color - screen colour.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_set_screen_color(@NonNull GLKModel m, int win, int color) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_set_screen_color: id = " + win + ", color = " + color);
        }

        // We don't implement this function as we are HTML-enabled.
    }

    /**
     * Flush output on a banner.
     *
     * @param win - the banner.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_flush(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_flush: id = " + win);
        }

        // We don't need to do anything here - our GLK implementation flushes
        // automatically when glk_select is called.
    }

    /**
     * Set the banner's size.  The size has the same meaning as in
     * os_banner_create().
     *
     * @param win         - the banner.
     * @param siz         - the new size.
     * @param siz_units   - the units of the new size.
     * @param is_advisory - indicates whether the sizing is required or advisory only.
     *                    If this flag is false, then the size should be set as requested.  If
     *                    this flag is true, it means that the caller intends to call
     *                    os_banner_size_to_contents() at some point, and that the size being set
     *                    now is for advisory purposes only.  Platforms that support
     *                    size-to-contents may simply ignore advisory sizing requests, although
     *                    they might want to ensure that they have sufficient off-screen buffer
     *                    space to keep track of the requested size of display, so that the
     *                    information the caller displays in preparation for calling
     *                    size-to-contents will be retained.  Platforms that do not support
     *                    size-to-contents should set the requested size even when 'is_advisory'
     *                    is true.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_set_size(@NonNull GLKModel m, int win, int siz, int siz_units, int is_advisory) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_set_size: id = " + win + ", siz = " + siz + ", siz_units = " +
                    GLKConstants.tadsBannerSizeUnitToString(siz_units) + ", is_advisory = " + is_advisory);
        }
        if (is_advisory == 1) {
            return;
        }
        resize_window(m, win, siz, siz_units);
    }

    /**
     * Set the banner to the size of its current contents.  This can be used
     * to set the banner's size after some text (or other material) has been
     * displayed to the banner, so that the size can be set according to the
     * banner's actual space requirements.
     * <p>
     * This changes the banner's "requested size" to match the current size.
     * Subsequent calls to os_banner_getinfo() will thus indicate a requested
     * size according to the size set here.
     *
     * @param win - the banner.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_size_to_contents(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_size_to_contents: id = " + win);
        }
        resize_window(m, win, -1, GLKConstants.TADS_BANNER_SIZE_ABS);
    }

    /**
     * Helper function - resize a window's width or height (depending on
     * whether the parent pair window's orientation is horizontal or vertical)
     *
     * @param win       - window to resize
     * @param siz       - the new width or height, in pixels. If less than zero, we will size to contents (and siz_units arg is ignored).
     * @param siz_units - either TADS_BANNER_SIZE_ABS or TADS_BANNER_SIZE_PCT
     */
    private static void resize_window(@NonNull GLKModel m, int win, int siz, int siz_units) {
        GLKTextWindowM w = m.mStreamMgr.getTextWindow(win);
        if (w == null) {
            GLKLogger.error("glk: resize_window: invalid text window id: " + win);
            return;
        }

        // Get parent
        int o_id = w.getParentId();
        GLKPairM o = m.mStreamMgr.getPairWindow(o_id);
        if (o == null) {
            GLKLogger.error("glk: resize_window: cannot resize root window " + win + " - root windows are always maximised.");
            return;
        }
        int method = o.getSplitDirection(w);
        switch (siz_units) {
            case GLKConstants.TADS_BANNER_SIZE_ABS:
                method |= GLKConstants.winmethod_Fixed;
                break;
            case GLKConstants.TADS_BANNER_SIZE_PCT:
                method |= GLKConstants.winmethod_Proportional;
                break;
            default:
                GLKLogger.error("glk: resize_window: did not recognise size units: " + siz_units);
                return;
        }
        if (!o.changeSplit(method, siz, true, null)) {
            GLKLogger.error("glk: resize_window: failed.");
        }
    }

    /**
     * Turn HTML mode on in the banner window.  If the underlying renderer
     * doesn't support HTML, this has no effect.
     *
     * @param win - the banner.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_start_html(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_start_html: id = " + win);
        }
        GLKTextBufferM w = m.mStreamMgr.getTextBuffer(win);
        if (w != null) {
            w.setHTMLOutput(m, true);
        } else {
            GLKLogger.error("tadsban_start_html: invalid window id: " + win);
        }
    }

    /**
     * Turn HTML mode off in the banner window.  If the underlying renderer
     * doesn't support HTML, this has no effect.
     *
     * @param win - the banner.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_end_html(@NonNull GLKModel m, int win) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_end_html: id = " + win);
        }
        GLKTextBufferM w = m.mStreamMgr.getTextBuffer(win);
        if (w != null) {
            w.setHTMLOutput(m, false);
        } else {
            GLKLogger.error("tadsban_end_html: invalid window id: " + win);
        }
    }

    /**
     * Set the output coordinates in a text grid window.  The grid window is
     * arranged into character cells numbered from row zero, column zero for
     * the upper left cell.  This function can only be used if the window was
     * created with type OS_BANNER_TYPE_TEXTGRID; the request should simply be
     * ignored by other window types.
     *
     * @param win - the banner.
     * @param row - the new row.
     * @param col - the new column.
     */
    @SuppressWarnings("unused")   // this is called by the JNI layer
    public static void tadsban_goto(@NonNull GLKModel m, int win, int row, int col) {
        if (DEBUG_TADSBAN_CALLS) {
            GLKLogger.debug("tadsban_goto: id = " + win + ", row = " + row + ", col = " + col);
        }
        glk_window_move_cursor(m, win, row, col);
    }

    /**
     * Post an event to the Controller / Terp.
     * Events will be stored in the queue and will be read by the Terp
     * upon the next call to glk_select or glk_select_poll.
     *
     * @param ev
     */
    public static void postEvent(final @NonNull GLKEvent ev) {
        try {
            mEventQueue.add(ev);
        } catch (IllegalStateException e) {
            GLKLogger.error("GLKController: Dropped an event as the event queue was full! Consider increasing the event queue's capacity.");
        }
    }

    private static void processUIDebugEvent(@NonNull GLKModel m, @NonNull GLKEvent e) throws InterruptedException {
        // Process special debug keys
        GLKTextBufferM w = m.mStreamMgr.getTextBuffer(e.win);
        if (w == null) {
            return;
        }

        if (e.val1 == GLKConstants.FAB_DEBUG_KEY_SHOW_STYLES) {
            w.setStyle(GLKConstants.style_Input);
            w.putString("[DEBUG - STYLES]\n\n");
            w.setStyle(GLKConstants.style_Normal);
            w.putString("Normal\n");
            w.setStyle(GLKConstants.style_Emphasized);
            w.putString("Emphasized\n");
            w.setStyle(GLKConstants.style_Preformatted);
            w.putString("Preformatted\n");
            w.setStyle(GLKConstants.style_Header);
            w.putString("Header\n");
            w.setStyle(GLKConstants.style_Subheader);
            w.putString("Subheader\n");
            w.setStyle(GLKConstants.style_Alert);
            w.putString("Alert\n");
            w.setStyle(GLKConstants.style_Note);
            w.putString("Note\n");
            w.setStyle(GLKConstants.style_BlockQuote);
            w.putString("BlockQuote\n");
            w.setStyle(GLKConstants.style_Input);
            w.putString("Input\n");
            w.setStyle(GLKConstants.style_User1);
            w.putString("User1\n");
            w.setStyle(GLKConstants.style_User2);
            w.putString("User2\n");
            w.setStyle(GLKConstants.style_Normal);
            w.putString("\n");
        } else {
            String msg;
            switch (e.val1) {
                case GLKConstants.FAB_DEBUG_KEY_SHOW_WINDOWS:
                    w.setStyle(GLKConstants.style_Input);
                    w.putString("[DEBUG - WINDOWS]\n\n");
                    w.setStyle(GLKConstants.style_Normal);
                    msg = m.mStreamMgr.getWindowInfo();
                    break;
                case GLKConstants.FAB_DEBUG_KEY_MEM_INFO:
                    w.setStyle(GLKConstants.style_Input);
                    w.putString("[DEBUG - MEMORY]\n\n");
                    w.setStyle(GLKConstants.style_Normal);
                    msg = GLKUtils.getMemoryInfo();
                    break;
                case GLKConstants.FAB_DEBUG_KEY_SHOW_STREAMS:
                    w.setStyle(GLKConstants.style_Input);
                    w.putString("[DEBUG - STREAMS]\n\n");
                    w.setStyle(GLKConstants.style_Normal);
                    msg = m.mStreamMgr.getStreamInfo();
                    break;
                default:
                    // unrecognised - ignore
                    return;
            }
            GLKLogger.debug(msg);
            w.putString(msg);
        }
        w.putString(">");
        m.updateView();
    }

    public enum TerpType {
        UNKNOWN, GIT, SCOTT, BOCFEL, SCARE, HUGO, TADS, AGILITY, ALAN2, ALAN3, MAGNETIC, LEVEL9, ADVSYS, GLULXE, BEBEK
    }

    private static class RunnableTerp implements Runnable {
        @NonNull
        private final GLKModel mModel;
        @NonNull
        private final GLKActivity mAct;

        RunnableTerp(@NonNull GLKModel m, @NonNull GLKActivity a) {
            mModel = m;
            mAct = a;
        }

        private void fatalError(@NonNull String error) {
            GLKLogger.error(error);
            int mainwin = glk_window_open(mModel, GLKConstants.NULL, GLKConstants.NULL, GLKConstants.NULL, GLKConstants.wintype_TextBuffer, 1);
            glk_set_window(mModel, mainwin);
            glk_set_style(mModel, GLKConstants.style_Alert);
            try {
                glk_put_string(mModel, error.getBytes("ISO-8859-1"), false);
            } catch (UnsupportedEncodingException e) {
                GLKLogger.error("GLKController: Unsupported encoding exception: " + e.getMessage());
            }
            try {
                glk_exit(mModel);
            } catch (InterruptedException e) {
                // Don't do anything as this is already a fatal error that is quitting
            }
        }

        @Override
        public void run() {
            // Do we have a valid terp?
            if (mModel.mTerpID == TerpType.UNKNOWN) {
                fatalError("Sorry, there is no interpreter available to play the selected game's format.");
                mModel.shutdown();
                return;
            }

            // Do we have enough arguments?
            if (mModel.mTerpArgs == null || mModel.mTerpArgs.length < 2) {
                fatalError("Attempted to start terp with no arguments.");
                mModel.shutdown();
                return;
            }

            // Load the GLK module if we haven't already
            try {
                // It should be safe to call loadLibrary for the glk module even if it
                // has already been loaded because, as per the Android docs for Runtime.loadLibrary,
                //
                //   "If this method is called more than once with the same library name, the
                //    second and subsequent calls are ignored."
                //
                System.loadLibrary("glk");
            } catch (@NonNull UnsatisfiedLinkError | SecurityException e) {
                fatalError("Cannot load the GLK module: " + e.getMessage());
                mModel.shutdown();
                return;
            }

            // Dynamically load the correct terp and run the game on it.

            // Get the absolute path to the terp shared library plugin (.so file).
            // It seems Android versions above KitKat only need the library name, but KitKat
            // and lower need the absolute path, otherwise they'll fail with a LinkLookupError.
            ApplicationInfo appInfo = mModel.getApplicationContext().getApplicationInfo();
            String terpLibPath = appInfo.nativeLibraryDir + "/lib" + mModel.mTerpLibName + ".so";
            StringBuilder sb = new StringBuilder();
            for (int i = 0, sz = mModel.mTerpArgs.length; i < sz; i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(mModel.mTerpArgs[i]);
            }
            GLKLogger.debug("======================");
            GLKLogger.debug("STARTING TERP...");
            GLKLogger.debug("  Plugin: " + terpLibPath);
            GLKLogger.debug("  Command: '".concat(sb.toString()) + "'");
            GLKLogger.debug("======================");
            int ret;
            if (mModel.mTerpIsJava) {
                // run in Java
                switch (mModel.mTerpLibName) {
                    case "bebek":
                        ret = MBebek.runTerp(mModel, mModel.mTerpArgs);
                        break;
                    default:
                        fatalError("Don't recognise java terp: " + mModel.mTerpLibName);
                        mModel.shutdown();
                        return;
                }
            } else {
                // run in C
                ret = runTerp(terpLibPath, mModel, mModel.mTerpArgs, mModel.mTerpLog);
            }

            // When control returns to this point the terp is ready to die

            GLKLogger.debug("======================");
            GLKLogger.debug("TERP HAS STOPPED");
            GLKLogger.debug("======================");

            // Ensure no events left in the event queue
            if (mModel.mTimer != null) {
                mModel.mTimer.cancel();
                mModel.mTimer = null;
            }
            mEventQueue.clear();

            // Tell the activity to die
            if (ret != 1 && Build.VERSION.SDK_INT <= 19) {
                // We had an abnormal exit and we're running on Dalvik, for some reason in this case
                // telling the activity to finish causes a crash ("PopFrame missed the break").
                // That error message usually means that the stack is trashed; but I can't for the
                // life of me work out where. And the problem doesn't seem to occur on post-Dalvik VMs.
                // So for now, if we're running on Dalvik, just kill the process directly now.
                GLKLogger.debug("Abnormal exit and we're running on Dalvik - kill process now to work around 'PopFrame missed the break' crash.");
                GLKLogger.shutdown();
                Process.killProcess(Process.myPid());
            } else {
                mAct.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAct.finish();
                    }
                });
            }
        }
    }

    private static class OnTimer extends TimerTask {
        @Override
        public void run() {
            // Timer events do not stack up
            // See glk_request_timer_event
            Object[] items = mEventQueue.toArray();
            for (Object e : items) {
                if (((GLKEvent) e).type == GLKConstants.evtype_Timer) {
                    // there is already a timer event in the queue -
                    // don't add another one
                    return;
                }
            }
            GLKEvent ev = new GLKEvent();
            ev.timerEvent();
            postEvent(ev);
        }
    }
}
