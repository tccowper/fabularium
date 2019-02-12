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
package com.luxlunae.glk.model;

import android.app.Application;
import android.app.FragmentManager;
import android.arch.lifecycle.AndroidViewModel;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.widget.Toast;

import com.luxlunae.fabularium.PreferencesActivity;
import com.luxlunae.fabularium.PreferencesActivity.ParcelableSharedPrefs;
import com.luxlunae.glk.GLKCharsetManager;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.controller.GLKEvent;
import com.luxlunae.glk.model.stream.GLKStreamManager;
import com.luxlunae.glk.model.stream.window.GLKTextWindowM;
import com.luxlunae.glk.model.stream.window.GLKWindowM;
import com.luxlunae.glk.model.style.GLKStyleSpan;
import com.luxlunae.glk.view.keyboard.GLKKeyboardMapping;
import com.luxlunae.glk.view.window.GLKScreen;
import com.luxlunae.glk.view.window.HVScrollView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.IllformedLocaleException;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;

/**
 * The Model updates the View and is manipulated by the Controller.
 * <p>
 * The Model always runs on the worker thread (not the UI thread).
 * <p>
 * Updates View:
 * -------------
 * The view is updated via the GlkUiMsgHandler.
 * <p>
 * Manipulated by Controller:
 * --------------------------
 * Currently the fields of the model are directly manipulated by the Controller (perhaps
 * this gives a slight performance boost). In future we may use getters and setters
 * for more transparency.
 */
@Keep
public class GLKModel extends AndroidViewModel {

    public static final int TADS_COLOR_ALINK = 0;
    public static final int TADS_COLOR_BGCOLOR = 1;
    public static final int TADS_COLOR_HLINK = 2;
    public static final int TADS_COLOR_LINK = 3;
    public static final int TADS_COLOR_STATUSBG = 4;
    public static final int TADS_COLOR_STATUSTEXT = 5;
    public static final int TADS_COLOR_TEXT = 6;
    public static final int TADS_COLOR_INPUT = 7;
    public static final int TADS_FONT_SERIF = 0;
    public static final int TADS_FONT_SANS = 1;
    public static final int TADS_FONT_SCRIPT = 2;
    public static final int TADS_FONT_TYPEWRITER = 3;
    public static final int TADS_FONT_INPUT = 4;
    private static final String TERP_LOG = "/terp.log";
    public final int[] tadsSoundLayers = new int[4];
    public final GLKStyleSpan[] mDefaultStylesTextBuf = new GLKStyleSpan[GLKConstants.style_NUMSTYLES];
    public final GLKStyleSpan[] mDefaultStylesTextGrid = new GLKStyleSpan[GLKConstants.style_NUMSTYLES];
    public final int[] mVScrollbarColors = new int[2];
    public final int[] mHScrollbarColors = new int[2];
    @NonNull
    public final int[] mTadsColors = new int[8];
    @NonNull
    public final Typeface[] mTadsFonts = new Typeface[5];

    // IMMUTABLE DATA
    @Nullable
    public Locale mLocale;
    public String mIFID;
    public String mGamePath;
    public String mGameName;
    public String mGameExt;
    public String mGameDataPath;
    public String mGameFormat;
    public String mTerpLog;
    public GLKController.TerpType mTerpID;
    public float mDefaultFontSizeTextBuf;
    public float mDefaultFontSizeTextGrid;
    public int mCursorWidthPX;
    public boolean mAutoResizeLargeImages;
    public Rect mDefaultMarginsTextGridPX;
    public Rect mDefaultMarginsTextBufPX;
    public Rect mDefaultWindowMarginsPX;
    public boolean mEnableHyperlinks;
    public boolean mEnableDateTime;
    public boolean mEnableTimer;
    public boolean mShowRawHTML;
    public boolean mEnableTextToSpeech;
    public float mTtsRate;
    public float mTtsPitch;
    public float mTextBufWidthMultiplier;
    public float mTextBufHeightMultiplier;
    public float mTextGridWidthMultiplier;
    public float mTextGridHeightMultiplier;
    public float mGraphicsWidthMultiplier;
    public float mGraphicsHeightMultiplier;
    public boolean mUsingHardwareKeyboard;
    public boolean mUseBuiltinKeyboard;
    public boolean mKeyboardOff = false;

    @NonNull
    public final GLKStreamManager mStreamMgr;
    @NonNull
    public final GLKResourceManager mResourceMgr;
    public int mPicCacheSize;
    @Nullable
    public GLKStyleSpan mTADSStatusStyle = null;

    // MUTABLE DATA
    public boolean mSyncScreenBG;
    @Nullable
    public GLKCharsetManager mCharsetMgr;
    @Nullable
    public GLKKeyboardMapping[] mKeyboardMappings = null;
    @Nullable
    public String mKeyboardName = null;
    public String mTerpName;
    public String mTerpLibName;
    public boolean mTerpIsJava = false;
    @Nullable
    public String[] mTerpArgs;
    public int mVScrollbarWidthPx;
    public int mHScrollbarHeightPx;
    public boolean mVScrollbarColorsOverride;
    public boolean mHScrollbarColorsOverride;
    public float mDefaultLeadingTextBuf;
    public float mDefaultLeadingTextGrid;
    @Nullable
    public Timer mTimer = null;  // only one timer can be running at any given moment
    public int mRootWinID = GLKConstants.NULL;
    public int mHTMLBannerRootID;
    public GLKTextWindowM mHTMLBannerRoot;
    public Map<String, Integer> mHTMLBanners;
    public boolean mHTMLOutput = false;
    public int mBorderStyle;
    public int mBorderHeightPX;
    public int mBorderWidthPX;
    public int mBorderColor;
    public boolean mEnableSounds;
    public boolean mEnableGraphics;
    public int mBackgroundColor;
    public int mHyperlinkColor;
    public boolean mSubPixel = true;
    public int mWinFocusID = GLKConstants.NULL;
    private Point mScreenSize;

    private WeakReference<GLKScreen> mScreen;
    private WeakReference<FragmentManager> mFragMgr;

    public GLKModel(@NonNull Application application) {
        super(application);
        mResourceMgr = new GLKResourceManager(this);
        mStreamMgr = new GLKStreamManager();
    }

    private static GLKController.TerpType getTerpFromName(@NonNull String terpName) {
        switch (terpName) {
            case "git":
                return GLKController.TerpType.GIT;
            case "scott":
                return GLKController.TerpType.SCOTT;
            case "bocfel":
                return GLKController.TerpType.BOCFEL;
            case "scare":
                return GLKController.TerpType.SCARE;
            case "hugo":
                return GLKController.TerpType.HUGO;
            case "tads":
                return GLKController.TerpType.TADS;
            case "agility":
                return GLKController.TerpType.AGILITY;
            case "alan2":
                return GLKController.TerpType.ALAN2;
            case "alan3":
                return GLKController.TerpType.ALAN3;
            case "magnetic":
                return GLKController.TerpType.MAGNETIC;
            case "level9":
                return GLKController.TerpType.LEVEL9;
            case "advsys":
                return GLKController.TerpType.ADVSYS;
            case "glulxe":
                return GLKController.TerpType.GLULXE;
            case "bebek":
                return GLKController.TerpType.BEBEK;
            default:
                // not recognised
                return null;
        }
    }

    @NonNull
    private static GLKController.TerpType getDefaultTerp(String format) {
        // remvove any "blorbed" at the start of the string
        String tmp = format;
        int pos = tmp.indexOf("blorbed ");
        if (pos >= 0) {
            tmp = tmp.substring(pos + 8);
        }

        // now, what do we have here?
        switch (tmp) {
            case "adrift":
                return GLKController.TerpType.BEBEK;
            case "advsys":
                return GLKController.TerpType.ADVSYS;
            case "agt":
                return GLKController.TerpType.AGILITY;
            case "alan":
                return GLKController.TerpType.ALAN3;
            case "glulx":
                return GLKController.TerpType.GIT;
            case "hugo":
                return GLKController.TerpType.HUGO;
            case "level9":
                return GLKController.TerpType.LEVEL9;
            case "magscrolls":
                return GLKController.TerpType.MAGNETIC;
            // formats detected by isValidExeExtension
            case "scott":
                return GLKController.TerpType.SCOTT;
            case "tads2":
            case "tads3":
                return GLKController.TerpType.TADS;
            case "zcode":
                return GLKController.TerpType.BOCFEL;

            // These are the formats that we currently don't support:
            case "html":
                // The format "html" means that this is an HTML file, perhaps collected
                // with associated files such as CSS and Javascript. The intended
                // interpreter is a standard web browser.
            case "executable":
                // The format "executable" means that this is not a virtual-machine-based
                // story file but a program for a physical machine: say, an old MS-DOS
                // executable. There is therefore no applicable interpreter.
                // Note also that, according to the Treaty of Babel, "any file which
                // isn't in a known format should be presumptively identified as an
                // unknown flavour of executable."
            default:
                // OK, Babel didn't recognise it!
        }
        return GLKController.TerpType.UNKNOWN;
    }

    private static void setTextColor(@NonNull GLKStyleSpan[] styles, @NonNull String styid,
                                     @NonNull String fg, @NonNull String bg) throws IllegalArgumentException {
        GLKStyleSpan sty = styles[Integer.valueOf(styid)];
        sty.setColor(parseConfigColor(fg), parseConfigColor(bg));
    }

    private static void setTextColor(@NonNull GLKStyleSpan sty, @ColorInt int fg, @ColorInt int bg) {
        sty.setColor(fg, bg);
    }

    private static void setFontSize(@NonNull GLKStyleSpan[] styles, float sizeSP, int applyTo, DisplayMetrics dm) {
        // applyTo = 0 means apply to all
        // applyTo = 1 means apply to prop fonts only
        // applyTo = 2 means apply to mono fonts only
        for (GLKStyleSpan sty : styles) {
            if (applyTo == 0 ||
                    (applyTo == 1 && !sty.mIsMono) ||
                    (applyTo == 2 && sty.mIsMono)) {
                sty.setTextSize(sizeSP, dm, true);
            }
        }
    }

    private static void setLockStyles(@NonNull GLKStyleSpan[] styles, boolean on) {
        for (GLKStyleSpan sty : styles) {
            sty.lock(on);
        }
    }

    private static void setFont(@NonNull HashMap<String, Typeface> fonts, @NonNull GLKStyleSpan[] styles,
                                @NonNull String styid, @NonNull String fontId,
                                float propsize, float monosize, DisplayMetrics dm) throws IllegalArgumentException {
        boolean isMono = fontId.startsWith("mono");
        GLKStyleSpan sty = styles[Integer.valueOf(styid)];
        Typeface fontVal = fonts.get(fontId);
        if (fontVal != null) {
            sty.setTypeface(fontVal);
        }
        sty.setTextSize(isMono ? monosize : propsize, dm, true);
        sty.mIsMono = isMono;
    }

    private static void setFont(@NonNull HashMap<String, Typeface> fonts, @NonNull GLKStyleSpan sty, @NonNull String fontId,
                                float propsize, float monosize, DisplayMetrics dm) throws IllegalArgumentException {
        boolean isMono = fontId.startsWith("mono");
        Typeface fontVal = fonts.get(fontId);
        if (fontVal != null) {
            sty.setTypeface(fontVal);
        }
        sty.setTextSize(isMono ? monosize : propsize, dm, true);
        sty.mIsMono = isMono;
    }

    private static void storeFont(@NonNull Context c, @NonNull String fontName, @NonNull AssetManager assetMgr,
                                  @NonNull HashMap<String, Typeface> fonts, @NonNull String fontId) throws RuntimeException {
        Typeface t = getFont(c, assetMgr, fontName);
        if (t != null) {
            fonts.put(fontId, t);
        }
    }

    private static void storeFont(@NonNull Context c, @NonNull String fontName, @NonNull AssetManager assetMgr,
                                  @NonNull Typeface[] fonts, int index) throws RuntimeException {
        Typeface t = getFont(c, assetMgr, fontName);
        if (t != null) {
            fonts[index] = t;
        }
    }

    @Nullable
    private static Typeface getFont(@NonNull Context c, @NonNull AssetManager assetMgr, @NonNull String fontName) throws RuntimeException {
        String assetPath = null;
        switch (fontName.toLowerCase().trim()) {
            case "libmr":
                assetPath = "fonts/LiberationMono-Regular.ttf";
                break;
            case "libmb":
                assetPath = "fonts/LiberationMono-Bold.ttf";
                break;
            case "libmi":
                assetPath = "fonts/LiberationMono-Italic.ttf";
                break;
            case "libmz":
                assetPath = "fonts/LiberationMono-BoldItalic.ttf";
                break;
            case "linlr":
                assetPath = "fonts/LinLibertine_R.otf";
                break;
            case "linlb":
                assetPath = "fonts/LinLibertine_RB.otf";
                break;
            case "linli":
                assetPath = "fonts/LinLibertine_RI.otf";
                break;
            case "linlz":
                assetPath = "fonts/LinLibertine_RBI.otf";
                break;
            case "linbr":
                assetPath = "fonts/LinBiolinum_R.otf";
                break;
            case "linbb":
                assetPath = "fonts/LinBiolinum_RB.otf";
                break;
            case "linbi":
                assetPath = "fonts/LinBiolinum_RI.otf";
                break;
            case "linbz":
                assetPath = "fonts/LinBiolinum_RBI.otf";
                break;
            default:
                // assume referring to a font file in the fonts directory
                break;
        }

        try {
            return (assetPath != null) ?
                    Typeface.createFromAsset(assetMgr, assetPath) :
                    Typeface.createFromFile(GLKConstants.getDir(c, GLKConstants.SUBDIR.FONTS).concat(fontName));
        } catch (IOException e) {
            GLKLogger.error("GLKModel: Cannot access fonts directory. Check you have enabled read/write permissions. If you have overridden the default paths, also check Fabularium has read/write access to the paths you have specified.");
            return null;
        }
    }

    private static int parseConfigColor(@NonNull String color) throws IllegalArgumentException {
        // 'color' should either be:
        //     1) RRGGBB format
        //     2) "trans" - for the transparent color
        //     3) the name of a color recognised by Color.parseColor()
        // if color does not match any of these
        if (color.equals("trans")) {
            return Color.TRANSPARENT;
        } else {
            int ret;
            try {
                ret = Color.parseColor("#".concat(color));
            } catch (IllegalArgumentException e) {
                ret = Color.parseColor(color);
            }
            return ret;
        }
    }

    private static void setMonoAspect(@NonNull GLKStyleSpan[] styles, float aspect) {
        // set the letter spacing for every mono font in fonts to 'aspect'
        // N.B. Android uses negative values for squeezing and positive values for
        // expanding; so we need to subtract 1 from 'aspect'
        float a = aspect - 1f;
        for (GLKStyleSpan s : styles) {
            if (s.mIsMono) {
                s.setSpacing(a);
            }
        }
    }

    /**
     * Parse a string that is expected to contain an integer.
     * If it doesn't actually contain an integer, unlike parseInt
     * don't throw an exception, instead default to provided argument.
     *
     * @param s           - the string to parse
     * @param def         - the value to return if string is invalid
     * @param settingName - the name of the setting (used for log if parsing fails)
     * @param min         - lower valid bound for the return value (inclusive)
     * @param max         - upper valid bound for the return value (inclusive)
     * @return an int either equal to s, def, 0 or 1 (depending on whether s is valid and values of allowNegative and allowZero).
     */
    private static int verifyInt(@NonNull String s, int def,
                                 @NonNull String settingName, int min, int max) {
        int ret;
        try {
            ret = Integer.parseInt(s);
        } catch (IllegalArgumentException e) {
            GLKLogger.error("Your setting for " + settingName +
                    " is invalid - " + e.getMessage() +
                    ". Note you don't need to specify units as we assume DP internally. Using default value (" + def + ") instead...");
            return def;
        }
        if (ret < min) {
            GLKLogger.error("Your setting for " + settingName +
                    " is invalid - too small (needs to be at least " + min + "). Using default value (" + def + ") instead...");
            return def;
        }
        if (ret > max) {
            GLKLogger.error("Your setting for " + settingName +
                    " is invalid - too large (needs to be no more than " + max + "). Using default value (" + def + ") instead...");
            return def;
        }
        return ret;
    }

    /**
     * Parse a string that is expected to contain a float.
     * If it doesn't actually contain an integer, unlike parseFloat
     * don't throw an exception, instead default to provided argument.
     *
     * @param s           - the string to parse
     * @param def         - the value to return if string is invalid
     * @param settingName - the name of the setting (used for log if parsing fails)
     * @param min         - lower valid bound for the return value (inclusive)
     * @param max         - upper valid bound for the return value (inclusive)
     * @return a float representation of s (if s was parsed and within the bounds), or def otherwise
     */
    private static float verifyFloat(@NonNull String s, float def,
                                     @NonNull String settingName, float min, float max) {
        float ret;
        try {
            ret = Float.parseFloat(s);
        } catch (IllegalArgumentException e) {
            GLKLogger.error("Your setting for " + settingName +
                    " is invalid - " + e.getMessage() +
                    ". Note you don't need to specify units as we assume DP internally. Using default value (" + def + ") instead...");
            return def;
        }
        if (ret < min) {
            GLKLogger.error("Your setting for " + settingName +
                    " is invalid - too small (needs to be at least " + min + "). Using default value (" + def + ") instead...");
            return def;
        }
        if (ret > max) {
            GLKLogger.error("Your setting for " + settingName +
                    " is invalid - too large (needs to be no more than " + max + "). Using default value (" + def + ") instead...");
            return def;
        }
        return ret;
    }

    public void initialise(@NonNull ParcelableSharedPrefs sharedPref,
                           @NonNull String gamePath, @NonNull String gameFormat, @NonNull String ifid) {
        GLKLogger.debug("Setting up Model...");

        Resources res = getApplication().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Work out the appropriate terp to use
        mGamePath = gamePath;
        mGameFormat = gameFormat.toLowerCase();
        mGameName = new File(gamePath).getName().toLowerCase();
        int pos = mGameName.indexOf('.');
        if (pos >= 0 && mGameName.length() > pos) {
            mGameExt = mGameName.substring(pos + 1);
        } else {
            mGameExt = "";
        }

        try {
            mTerpLog = GLKConstants.getDir(getApplicationContext(), null).concat(TERP_LOG);
        } catch (IOException e) {
            GLKLogger.warn("GLKModel: Cannot initialise terp log.");
        }

        mIFID = ifid.toLowerCase();
        try {
            mGameDataPath = GLKConstants.getDir(getApplicationContext(), GLKConstants.SUBDIR.GAMEDATA) + "/" + mIFID + "/";
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(),
                    "Warning: Could not access game data directory. You will not be able to save, restore, etc. for this play session. Please check you have granted file read/write permissions and try again, If you have overridden any default paths in your settings, also check those paths exist and Fabularium has read/write access to them.",
                    Toast.LENGTH_LONG).show();
            GLKLogger.warn("Cannot access game data directory. Check you have enabled file read / write permissions. If you have overridden the default path, also check Fabularium has read/write access to your specified path.");
        }
        mTerpID = getDefaultTerp(mGameFormat);
        setTerpDetails(mTerpID);
        if (!mTerpLibName.equals("")) {
            mTerpArgs = new String[2];
            mTerpArgs[0] = mTerpName;
            mTerpArgs[1] = mGamePath;
        } else {
            mTerpArgs = null;
        }

        mUseBuiltinKeyboard = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_BUILTIN_KEYBOARD, true);
        mUsingHardwareKeyboard = res.getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;

        // Get the locale
        String l = sharedPref.getString(PreferencesActivity.KEY_PREF_LOCALE, null);
        if (l == null || l.equals("")) {
            mLocale = Locale.getDefault();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Locale.Builder b = new Locale.Builder();
                try {
                    mLocale = b.setLanguageTag(l).build();
                } catch (IllformedLocaleException e) {
                    mLocale = Locale.getDefault();
                    GLKLogger.warn("The locale in the user settings was invalid: " + e.getMessage() +
                            ". Reverting to your system's default, which is '" + mLocale.getLanguage() + "'");
                }
            } else {
                // This constructor doesn't do syntactic checking. So any error will only get picked up (hopefully)
                // when / if the TTS engine is initialised.
                mLocale = new Locale(l);
            }
        }

        // Initialise the charset manager
        String charsetName = sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_CHARSET, "UTF-8");
        String bo = sharedPref.getString(PreferencesActivity.KEY_PREF_BYTE_ORDER, "0");
        switch (bo) {
            case "1":
                mCharsetMgr = new GLKCharsetManager(GLKCharsetManager.ENDIAN.BIG_FIXED, charsetName);
                break;
            case "2":
                mCharsetMgr = new GLKCharsetManager(GLKCharsetManager.ENDIAN.LITTLE_FIXED, charsetName);
                break;
            case "0":
            default:
                // auto detect byte order based on game format
                // most interpreters are little endian, so we
                // take that as the default
                switch (mTerpID) {
                    case GIT:
                        mCharsetMgr = new GLKCharsetManager(GLKCharsetManager.ENDIAN.BIG_REVERSE_IF_ERROR, charsetName);
                        break;
                    default:
                        mCharsetMgr = new GLKCharsetManager(GLKCharsetManager.ENDIAN.LITTLE_REVERSE_IF_ERROR, charsetName);
                        break;
                }
                break;
        }

        // Initialise resource and stream managers
        // For the picture cache, we use 1/20th of the memory available to the JVM, and
        // this memory is only allocated if/when the first picture is loaded in the
        // GLKResourceManager
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        mPicCacheSize = maxMemory / 20;

        // Set the scrollbar styles
        //
        // We can do it like this because, unless mutate is called,
        //
        //   "By default, all drawables instances loaded from the same resource
        //    share a common state; if you modify the state of one instance,
        //    all the other instances will receive the same modification."
        //
        // We exploit that default behaviour. Hopefully future versions of
        // Android won't change it!

        // Vertical scrollbars (no-one should need a scrollbar wider than 20 dp right!?)
        int sizeDp = verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_VSCROLLBAR_WIDTH, "3"),
                3, "vertical scrollbar width", 0, 20);
        mVScrollbarWidthPx = sizeDp > 0 ? GLKUtils.dpToPx(sizeDp, dm) : 0;
        String o = sharedPref.getString(PreferencesActivity.KEY_PREF_VSCROLLBAR_GRAD_ORIENT, "6");
        HVScrollView.setScrollbarGradientOrientation(res, true, o);
        String startCol = sharedPref.getString(PreferencesActivity.KEY_PREF_VSCROLLBAR_START_COLOR, "");
        String endCol = sharedPref.getString(PreferencesActivity.KEY_PREF_VSCROLLBAR_END_COLOR, "");
        if (!startCol.equals("") && !endCol.equals("")) {
            try {
                mVScrollbarColors[0] = Color.parseColor(startCol);
                mVScrollbarColors[1] = Color.parseColor(endCol);
                mHScrollbarColorsOverride = true;
            } catch (IllegalArgumentException e) {
                // do nothing
                GLKLogger.warn("Could not set vertical scroll bar colors as didn't recognise the color string.");
            }
        }

        // Horizontal scrollbars (no-one should need a scrollbar higher than 20 dp right!?)
        sizeDp = verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_HSCROLLBAR_HEIGHT, "3"),
                3, "horizontal scrollbar height", 0, 20);
        mHScrollbarHeightPx = sizeDp > 0 ? GLKUtils.dpToPx(sizeDp, dm) : 0;
        o = sharedPref.getString(PreferencesActivity.KEY_PREF_HSCROLLBAR_GRAD_ORIENT, "3");
        HVScrollView.setScrollbarGradientOrientation(res, false, o);
        startCol = sharedPref.getString(PreferencesActivity.KEY_PREF_HSCROLLBAR_START_COLOR, "");
        endCol = sharedPref.getString(PreferencesActivity.KEY_PREF_HSCROLLBAR_END_COLOR, "");
        if (!startCol.equals("") && !endCol.equals("")) {
            try {
                mHScrollbarColors[0] = Color.parseColor(startCol);
                mHScrollbarColors[1] = Color.parseColor(endCol);
                mHScrollbarColorsOverride = true;
            } catch (IllegalArgumentException e) {
                // do nothing
                GLKLogger.warn("Could not set horizontal scroll bar colors as didn't recognise the color string.");
            }
        }

        // Set the border style - by default never show borders
        // Currently valid border style values range from 0-2 (see arrays.xml)
        // And presumably no-one should need borders wider or higher than 20 dp
        mBorderStyle = verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_BORDER_STYLE, "1"),
                1, "border style", 0, 2);
        mBorderWidthPX = verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_BORDER_WIDTH, "1"),
                1, "border width", 0, 20);
        mBorderHeightPX = verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_BORDER_HEIGHT, "1"),
                1, "border height", 0, 20);
        try {
            mBorderColor = Color.parseColor(sharedPref.getString(PreferencesActivity.KEY_PREF_BORDER_COLOR, "darkgrey"));
        } catch (IllegalArgumentException e) {
            mBorderColor = Color.DKGRAY;
        }
        mBorderWidthPX = GLKUtils.dpToPx(mBorderWidthPX, dm);
        mBorderHeightPX = GLKUtils.dpToPx(mBorderHeightPX, dm);

        // Set the default font size (in scale-independent pixels, SP)
        mDefaultFontSizeTextBuf = verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_FONT_SIZE_TEXT_BUF, "15"),
                15, "default text buffer font size", 1, 80);
        mDefaultFontSizeTextGrid = verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_FONT_SIZE_TEXT_GRID, "12"),
                12, "default text grid font size", 1, 80);
        mAutoResizeLargeImages = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_DEFAULT_AUTO_RESIZE_LARGE_IMAGES, true);

        mEnableGraphics = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_ENABLE_GRAPHICS, true);
        mEnableSounds = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_ENABLE_SOUNDS, true);
        mEnableHyperlinks = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_ENABLE_HYPERLINKS, true);
        mEnableDateTime = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_ENABLE_DATETIME, true);
        mEnableTimer = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_ENABLE_TIMER, true);

        mShowRawHTML = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_SHOW_RAW_HTML, false);

        mDefaultLeadingTextBuf = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_LEADING_TEXT_BUF, "1.2"),
                1.2f, "default leading for text buffers", 0.1f, 10f);
        mDefaultLeadingTextGrid = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_LEADING_TEXT_GRID, "1.2"),
                1.2f, "default leading for text grids", 0.1f, 10f);

        // Allow margins to vary between 0 - 100000 dp. That should be enough for anyone, on any device.
        mDefaultMarginsTextBufPX = new Rect();
        mDefaultMarginsTextBufPX.left = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_LMARGIN_BUF, "5"),
                5, "text buffer left margin", 0, 100000), dm);
        mDefaultMarginsTextBufPX.right = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_RMARGIN_BUF, "5"),
                5, "text buffer right margin", 0, 100000), dm);
        mDefaultMarginsTextBufPX.top = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_TMARGIN_BUF, "5"),
                5, "text buffer top margin", 0, 100000), dm);
        mDefaultMarginsTextBufPX.bottom = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_BMARGIN_BUF, "5"),
                5, "text buffer bottom margin", 0, 100000), dm);

        mDefaultMarginsTextGridPX = new Rect();
        mDefaultMarginsTextGridPX.left = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_LMARGIN_GRID, "0"),
                0, "text grid left margin", 0, 100000), dm);
        mDefaultMarginsTextGridPX.right = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_RMARGIN_GRID, "0"),
                0, "text grid right margin", 0, 100000), dm);
        mDefaultMarginsTextGridPX.top = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_TMARGIN_GRID, "0"),
                0, "text grid top margin", 0, 100000), dm);
        mDefaultMarginsTextGridPX.bottom = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_BMARGIN_GRID, "0"),
                0, "text grid bottom margin", 0, 100000), dm);

        mDefaultWindowMarginsPX = new Rect();
        mDefaultWindowMarginsPX.left = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_LMARGIN_WIN, "10"),
                10, "window left margin", 0, 100000), dm);
        mDefaultWindowMarginsPX.right = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_RMARGIN_WIN, "10"),
                10, "window right margin", 0, 100000), dm);
        mDefaultWindowMarginsPX.top = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_TMARGIN_WIN, "10"),
                10, "window top margin", 0, 100000), dm);
        mDefaultWindowMarginsPX.bottom = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_DEFAULT_BMARGIN_WIN, "10"),
                10, "window bottom margin", 0, 100000), dm);
        try {
            mBackgroundColor = Color.parseColor(sharedPref.getString(PreferencesActivity.KEY_PREF_WIN_BGCOLOR, "white"));
        } catch (IllegalArgumentException e) {
            mBackgroundColor = Color.WHITE;
        }
        mHyperlinkColor = Color.RED;

        mSyncScreenBG = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_SYNC_SCREEN_BACKGROUND, true);

        // Users can make windows 1/10,000 of the normal size, up to 10,000 times the normal size
        mTextBufWidthMultiplier = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_TEXTBUF_WIDTH_MULTIPLIER, "1.0"),
                1.0f, "text buffer width multipler", 0.0001f, 10000f);
        mTextBufHeightMultiplier = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_TEXTBUF_HEIGHT_MULTIPLIER, "1.0"),
                1.0f, "text buffer height multiplier", 0.0001f, 10000f);
        mTextGridWidthMultiplier = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_TEXTGRID_WIDTH_MULTIPLIER, "1.0"),
                1.0f, "text grid width multiplier", 0.0001f, 10000f);
        mTextGridHeightMultiplier = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_TEXTGRID_HEIGHT_MULTIPLIER, "1.0"),
                1.0f, "text grid height multiplier", 0.0001f, 10000f);
        mGraphicsWidthMultiplier = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_GRAPHICS_WIDTH_MULTIPLIER, "1.0"),
                1.0f, "graphics window width multiplier", 0.0001f, 10000f);
        mGraphicsHeightMultiplier = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_GRAPHICS_HEIGHT_MULTIPLIER, "1.0"),
                1.0f, "graphics window height multiplier", 0.0001f, 10000f);

        mEnableTextToSpeech = sharedPref.getBoolean(PreferencesActivity.KEY_PREF_ENABLE_TTS, false);
        mTtsPitch = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_TTS_PITCH, "1.0"),
                1.0f, "TTS pitch", 0.1f, 10f);
        mTtsRate = verifyFloat(sharedPref.getString(PreferencesActivity.KEY_PREF_TTS_RATE, "1.0"),
                1.0f, "TTS rate", 0.1f, 10f);

        // cursor (between 1 - 500 dp)
        mCursorWidthPX = GLKUtils.dpToPx(verifyInt(sharedPref.getString(PreferencesActivity.KEY_PREF_CURSOR_WIDTH, "1"),
                1, "cursor width", 1, 500), dm);

        // Load the default fonts
        Context c = getApplicationContext();
        GLKStyleSpan.getDefaultStyles(mDefaultStylesTextBuf, false, mDefaultFontSizeTextBuf, c);
        GLKStyleSpan.getDefaultStyles(mDefaultStylesTextGrid, true, mDefaultFontSizeTextGrid, c);

        // Load default TADS colors
        mTadsColors[TADS_COLOR_ALINK] = Color.BLACK;
        mTadsColors[TADS_COLOR_BGCOLOR] = mBackgroundColor;
        mTadsColors[TADS_COLOR_HLINK] = Color.BLACK;
        mTadsColors[TADS_COLOR_LINK] = Color.BLACK;
        mTadsColors[TADS_COLOR_STATUSBG] = mDefaultStylesTextGrid[GLKConstants.style_Normal].mBackColor;
        mTadsColors[TADS_COLOR_STATUSTEXT] = mDefaultStylesTextGrid[GLKConstants.style_Normal].mTextColor;
        mTadsColors[TADS_COLOR_TEXT] = mDefaultStylesTextBuf[GLKConstants.style_Normal].mTextColor;
        mTadsColors[TADS_COLOR_INPUT] = mDefaultStylesTextBuf[GLKConstants.style_Input].mTextColor;

        // Make the game data directory if it doesn't already exist
        if (mGameDataPath != null) {
            if (!GLKUtils.makeDir(mGameDataPath)) {
                Toast.makeText(getApplicationContext(),
                        "Warning: Could not access game data directory " + mGameDataPath +
                                ". You will not be able to save, restore, etc. for this play session. Please check you have granted file read/write permissions and try again, If you have overridden any default paths in your settings, also check Fabularium has read/write access to those paths.",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // Now read in configuration file overrides for these settings, if any

        // Read global config file
        File config;
        try {
            config = new File(GLKConstants.getDir(getApplicationContext(), null), "fab.ini");
            if (config.exists()) {
                readConfigFile(getApplicationContext(), config, dm, res.getAssets());
            }
        } catch (@NonNull IOException | SecurityException e) {
            GLKLogger.warn("Cannot access app directory. Check you have enabled read/write permissions. If you have overridden the default paths, also check Fabularium has read/write access to your specified paths.");
        }

        if (mGameDataPath != null) {
            // Now see if we can find an ini file for this game.
            // We process the first ini we find in the game's data directory
            // and ignore anything else.
            File gameDataDir = new File(mGameDataPath);
            String[] iniFiles = gameDataDir.list(
                    new FilenameFilter() {
                        @Override
                        public boolean accept(@NonNull File dir, @NonNull String name) {
                            return name.endsWith(".ini");
                        }
                    });
            if (iniFiles != null && iniFiles.length > 0) {
                readConfigFile(getApplicationContext(),
                        new File(mGameDataPath, iniFiles[0]), dm, res.getAssets());
            }
        }

        if (mKeyboardName != null) {
            if (mKeyboardName.equals("off")) {
                mKeyboardOff = true;
                GLKLogger.debug("Turning off keyboard.");
            } else {
                try {
                    mKeyboardMappings =
                            GLKKeyboardMapping.loadFromFile(mKeyboardName,
                                    new File(GLKConstants.getDir(getApplicationContext(), null), "keyboards.ini"));
                    if (mKeyboardMappings == null) {
                        GLKLogger.error("Could not load keyboard mapping for " + mKeyboardName +
                                " from the keyboards.ini file. Does that mapping exist in the file? Reverting to default keyboard.");
                    }
                } catch (IOException e) {
                    GLKLogger.warn("Cannot access app directory. Check you have enabled read/write permissions. If you have overridden the default paths, also check Fabularium has read/write access to your specified paths.");
                }
            }
        }

        GLKLogger.flush();
    }

    @Nullable
    public FragmentManager getFragmentManager() {
        if (mFragMgr != null) {
            return mFragMgr.get();
        }
        return null;
    }

    private void setTerpDetails(@NonNull GLKController.TerpType terpID) {
        mTerpIsJava = false;

        switch (terpID) {
            case GIT:
                mTerpLibName = "git";
                mTerpName = "git";
                break;
            case SCOTT:
                mTerpLibName = "scott";
                mTerpName = "scott";
                break;
            case BOCFEL:
                mTerpLibName = "bocfel";
                mTerpName = "bocfel";
                break;
            case SCARE:
                mTerpLibName = "scare";
                mTerpName = "scare";
                break;
            case HUGO:
                mTerpLibName = "hugo";
                mTerpName = "hugo";
                break;
            case TADS:
                mTerpLibName = "tads";
                mTerpName = "tadsr";
                break;
            case AGILITY:
                mTerpLibName = "agility";
                mTerpName = "glkagil";
                break;
            case ALAN2:
                mTerpLibName = "alan2";
                mTerpName = "alan2";
                break;
            case ALAN3:
                mTerpLibName = "alan3";
                mTerpName = "alan3";
                break;
            case MAGNETIC:
                mTerpLibName = "magnetic";
                mTerpName = "glkmagnetic";
                break;
            case LEVEL9:
                mTerpLibName = "level9";
                mTerpName = "glklevel9";
                break;
            case ADVSYS:
                mTerpLibName = "advsys";
                mTerpName = "advsys";
                break;
            case GLULXE:
                mTerpLibName = "glulxe";
                mTerpName = "glulxe";
                break;
            case BEBEK:
                mTerpLibName = "bebek";
                mTerpName = "bebek";
                mTerpIsJava = true;
                break;
            case UNKNOWN:
            default:
                // unrecognised terp
                mTerpLibName = "";
                mTerpName = "";
                break;
        }
    }

    @NonNull
    public Point getScreenSize() {
        return new Point(mScreenSize);
    }

    public void setScreenSize(@NonNull Point newSize) {
        mScreenSize = new Point(newSize);
        GLKWindowM root = mStreamMgr.getWindow(mRootWinID);
        if (root != null) {
            root.resize(mScreenSize.x, mScreenSize.y);
            GLKEvent ev = new GLKEvent();
            ev.arrangeEvent();
            GLKController.postEvent(ev);
        }
    }

    public Context getApplicationContext() {
        return getApplication().getApplicationContext();
    }

    public void setWindowFocus(int winID) {
        if (mWinFocusID != winID) {
            mWinFocusID = winID;
        }
    }

    private boolean shouldApplySettings(@Nullable String[] currentOption) {
        // work out if setting should be applied to this game / terp combo or not
        if (currentOption == null) {
            // applies to all terp / game combos
            return true;
        }
        for (String aCurrentOption : currentOption) {
            String curOpt = aCurrentOption.trim().toLowerCase();
            if (curOpt.equals(mGameName) || curOpt.equals(mTerpLibName)) {
                return true;
            } else if (curOpt.startsWith("*")) {
                int pos = curOpt.indexOf('.');
                if (pos >= 0 && curOpt.length() > pos) {
                    String ext = curOpt.substring(pos + 1);
                    if (ext.equals(mGameExt)) {
                        return true;
                    }
                }
            } else if (curOpt.startsWith("%")) {
                int len = curOpt.length();
                if (len > 2) {
                    String format = curOpt.substring(1, len - 1);
                    if (format.equals(mGameFormat)) {
                        return true;
                    }
                }
            } else if (curOpt.equals(mIFID)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("ConstantConditions")
    private void readConfigFile(@NonNull Context c, @NonNull File config, @NonNull DisplayMetrics dm, @NonNull AssetManager assetMgr) {
        // Read in the config file
        GLKLogger.debug("Reading config file at " + config.getAbsolutePath());
        try {
            BufferedReader br = new BufferedReader(new FileReader(config));
            String line;
            int lineno = -1;
            String[] setting;
            String[] currentSelector;
            boolean applyCurrentSelector = true;
            HashMap<String, Typeface> fonts = new HashMap<>();
            float propsize = mDefaultFontSizeTextBuf;
            float monosize = mDefaultFontSizeTextGrid;
            float tsize;
            float gsize;
            int pos;
            int sizeDp;
            int leading = -1;
            while ((line = br.readLine()) != null) {
                lineno++;
                if (line.length() > 0) {
                    // remove any comment (starts with #)
                    pos = line.indexOf('#');
                    if (pos == 0) {
                        // whole line is a comment - skip
                        continue;
                    } else if (pos >= 0) {
                        line = line.substring(0, pos - 1);
                    }
                    line = line.toLowerCase().trim();

                    if (line.startsWith("[") && line.endsWith("]") && line.length() > 2) {
                        currentSelector = line.substring(1, line.length() - 1).split("\\s+");
                        applyCurrentSelector = shouldApplySettings(currentSelector);
                        if (applyCurrentSelector) {
                            GLKLogger.debug("Applying selector ".concat(line));
                        }
                    } else if (applyCurrentSelector) {
                        // Get the keyword and arguments
                        setting = line.split("\\s+");
                        if (setting.length > 0) {
                            try {
                                switch (setting[0]) {
                                    case "terp":
                                        if (setting.length >= 2) {
                                            GLKController.TerpType terpID = getTerpFromName(setting[1]);
                                            if (terpID != null) {
                                                setTerpDetails(terpID);
                                                GLKLogger.debug("    Set terp to '" + mTerpName + "'");
                                                if (!mTerpLibName.equals("")) {
                                                    mTerpID = terpID;
                                                    mTerpArgs = new String[2];
                                                    mTerpArgs[0] = mTerpName;
                                                    mTerpArgs[1] = mGamePath;
                                                } else {
                                                    mTerpArgs = null;
                                                }
                                            } else {
                                                GLKLogger.error("    Don't recognise terp type: " + setting[1]);
                                            }
                                        }
                                        break;
                                    case "terpargs":
                                        if (setting.length >= 2) {
                                            int sz = setting.length;
                                            mTerpArgs = new String[sz + 1];
                                            mTerpArgs[0] = mTerpName;
                                            mTerpArgs[sz] = mGamePath;
                                            System.arraycopy(setting, 1, mTerpArgs, 1, sz - 1);
                                        }
                                        break;
                                    case "syncbg":
                                        if (setting.length >= 2) {
                                            mSyncScreenBG = setting[1].equals("1");
                                        }
                                        break;
                                    case "monoaspect":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            float ratio = Float.parseFloat(setting[1]);
                                            if (ratio > 0) {
                                                setMonoAspect(mDefaultStylesTextGrid, ratio);
                                                setMonoAspect(mDefaultStylesTextBuf, ratio);
                                            }
                                        }
                                        break;
                                    case "stylehint":
                                        if (setting.length >= 2) {
                                            boolean on = setting[1].equals("0");
                                            setLockStyles(mDefaultStylesTextGrid, on);
                                            setLockStyles(mDefaultStylesTextBuf, on);
                                        }
                                        break;
                                    case "monofont":
                                    case "propfont":
                                        if (setting.length >= 2) {
                                            String fontName = setting[1];
                                            String prefix = setting[0].substring(0, 4);
                                            try {
                                                switch (fontName.toLowerCase().trim()) {
                                                    case "liberation_mono":
                                                        storeFont(c, "libmr", assetMgr, fonts, prefix.concat("r"));
                                                        storeFont(c, "libmi", assetMgr, fonts, prefix.concat("i"));
                                                        storeFont(c, "libmb", assetMgr, fonts, prefix.concat("b"));
                                                        storeFont(c, "libmz", assetMgr, fonts, prefix.concat("z"));
                                                        break;
                                                    case "linux_libertine":
                                                        storeFont(c, "linlr", assetMgr, fonts, prefix.concat("r"));
                                                        storeFont(c, "linli", assetMgr, fonts, prefix.concat("i"));
                                                        storeFont(c, "linlb", assetMgr, fonts, prefix.concat("b"));
                                                        storeFont(c, "linlz", assetMgr, fonts, prefix.concat("z"));
                                                        break;
                                                    case "linux_biolinum":
                                                        storeFont(c, "linbr", assetMgr, fonts, prefix.concat("r"));
                                                        storeFont(c, "linbi", assetMgr, fonts, prefix.concat("i"));
                                                        storeFont(c, "linbb", assetMgr, fonts, prefix.concat("b"));
                                                        storeFont(c, "linbz", assetMgr, fonts, prefix.concat("z"));
                                                        break;
                                                    default:
                                                        // don't do anything
                                                        GLKLogger.error("    Couldn't process font family " + fontName);
                                                        break;
                                                }
                                            } catch (RuntimeException e) {
                                                GLKLogger.error("    Could not set font '" + setting[1] + "': " + e.getMessage());
                                            }
                                        }
                                        break;
                                    case "tads-serif":
                                    case "tads-sans":
                                    case "tads-script":
                                    case "tads-typewriter":
                                    case "tads-input":
                                        if (setting.length >= 2) {
                                            String fontName = setting[1];
                                            int index = TADS_FONT_SERIF;
                                            switch (setting[0]) {
                                                case "tads-serif":
                                                    index = TADS_FONT_SERIF;
                                                    break;
                                                case "tads-sans":
                                                    index = TADS_FONT_SANS;
                                                    break;
                                                case "tads-script":
                                                    index = TADS_FONT_SCRIPT;
                                                    break;
                                                case "tads-typewriter":
                                                    index = TADS_FONT_TYPEWRITER;
                                                    break;
                                                case "tads-input":
                                                    index = TADS_FONT_INPUT;
                                                    break;
                                            }
                                            try {
                                                storeFont(c, fontName, assetMgr, mTadsFonts, index);
                                            } catch (RuntimeException e) {
                                                GLKLogger.error("    Could not set tads font '" + setting[1] + "': " + e.getMessage());
                                            }
                                        }
                                        break;
                                    case "monor":
                                    case "monoi":
                                    case "monob":
                                    case "monoz":
                                    case "propr":
                                    case "propi":
                                    case "propb":
                                    case "propz":
                                        if (setting.length >= 2) {
                                            String fontName = setting[1];
                                            try {
                                                storeFont(c, fontName, assetMgr, fonts, setting[0]);
                                            } catch (RuntimeException e) {
                                                GLKLogger.error("    Could not set font '" + setting[1] + "': " + e.getMessage());
                                            }
                                        }
                                        break;
                                    case "graphics":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            mEnableGraphics = (Integer.parseInt(setting[1]) == 1);
                                        }
                                        break;
                                    case "lcd":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            mSubPixel = (Integer.parseInt(setting[1]) == 1);
                                        }
                                        break;
                                    case "sound":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            mEnableSounds = (Integer.parseInt(setting[1]) == 1);
                                        }
                                        break;
                                    case "leading":
                                    case "tleading":
                                        if (setting.length >= 2) {
                                            if (setting[1].endsWith("%")) {
                                                // if any exception is thrown, will be caught
                                                // at bottom of this select block:
                                                mDefaultLeadingTextBuf = (float) Integer.parseInt(setting[1].substring(0, setting[1].length() - 1)) / 100f;
                                                mDefaultLeadingTextBuf = Math.max(mDefaultLeadingTextBuf, 0.1f);
                                                leading = 0;
                                            } else {
                                                // if any exception is thrown, will be caught
                                                // at bottom of this select block:
                                                leading = (Integer.parseInt(setting[1]));
                                            }
                                        }
                                        break;
                                    case "gleading":
                                        if (setting.length >= 2) {
                                            if (setting[1].endsWith("%")) {
                                                // if any exception is thrown, will be caught
                                                // at bottom of this select block:
                                                mDefaultLeadingTextGrid = (float) Integer.parseInt(setting[1].substring(0, setting[1].length() - 1)) / 100f;
                                                mDefaultLeadingTextGrid = Math.max(mDefaultLeadingTextGrid, 0.11f);
                                            }
                                        }
                                        break;
                                    case "propsize":
                                        if (setting.length >= 2) {
                                            propsize = Float.valueOf(setting[1]);
                                            if (propsize > 0) {
                                                setFontSize(mDefaultStylesTextGrid, propsize, 1, dm);
                                                setFontSize(mDefaultStylesTextBuf, propsize, 1, dm);
                                            }
                                        }
                                        break;
                                    case "monosize":
                                        if (setting.length >= 2) {
                                            monosize = Float.valueOf(setting[1]);
                                            if (monosize > 0) {
                                                // setFontSize(mDefaultStylesTextGrid, monosize, 2, dm);
                                                setFontSize(mDefaultStylesTextBuf, monosize, 2, dm);
                                            }
                                        }
                                        break;
                                    case "gsize":
                                        if (setting.length >= 2) {
                                            gsize = Float.valueOf(setting[1]);
                                            if (gsize > 0) {
                                                setFontSize(mDefaultStylesTextGrid, gsize, 0, dm);
                                                mDefaultFontSizeTextGrid = gsize;
                                            }
                                        }
                                        break;
                                    case "tsize":
                                        if (setting.length >= 2) {
                                            tsize = Float.valueOf(setting[1]);
                                            if (tsize > 0) {
                                                setFontSize(mDefaultStylesTextBuf, tsize, 0, dm);
                                                mDefaultFontSizeTextBuf = tsize;
                                            }
                                        }
                                        break;
                                    case "gfont":
                                        if (setting.length >= 3) {
                                            setFont(fonts, mDefaultStylesTextGrid, setting[1], setting[2], propsize, monosize, dm);
                                        }
                                        break;
                                    case "tfont":
                                        if (setting.length >= 3) {
                                            setFont(fonts, mDefaultStylesTextBuf, setting[1], setting[2], propsize, monosize, dm);
                                        }
                                        break;
                                    case "tads_statusfont":
                                        if (setting.length >= 2) {
                                            mTADSStatusStyle = new GLKStyleSpan(GLKConstants.style_Normal, true, mDefaultFontSizeTextGrid);
                                            setFont(fonts, mTADSStatusStyle, setting[1], propsize, monosize, dm);
                                        }
                                        break;
                                    case "scrollwidth":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            sizeDp = Integer.parseInt(setting[1]);
                                            mVScrollbarWidthPx = sizeDp > 0 ? GLKUtils.dpToPx(sizeDp, dm) : 0;
                                        }
                                        break;
                                    case "scrollheight":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            sizeDp = Integer.parseInt(setting[1]);
                                            mHScrollbarHeightPx = sizeDp > 0 ? GLKUtils.dpToPx(sizeDp, dm) : 0;
                                        }
                                        break;
                                    case "scrollfg":
                                        if (setting.length >= 2) {
                                            mHScrollbarColors[0] = mHScrollbarColors[1] =
                                                    mVScrollbarColors[0] = mVScrollbarColors[1] =
                                                            parseConfigColor(setting[1]);
                                            mVScrollbarColorsOverride = mHScrollbarColorsOverride = true;
                                        }
                                        break;
                                    case "tads_alink":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_ALINK] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "tads_bgcolor":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_BGCOLOR] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "tads_hlink":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_HLINK] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "tads_link":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_LINK] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "tads_statusbg":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_STATUSBG] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "tads_statustext":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_STATUSTEXT] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "tads_text":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_TEXT] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "tads_input":
                                        if (setting.length >= 2) {
                                            mTadsColors[TADS_COLOR_INPUT] = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "gcolor":
                                        if (setting.length >= 4) {
                                            setTextColor(mDefaultStylesTextGrid, setting[1], setting[2], setting[3]);
                                        }
                                        break;
                                    case "tcolor":
                                        if (setting.length >= 4) {
                                            setTextColor(mDefaultStylesTextBuf, setting[1], setting[2], setting[3]);
                                        }
                                        break;
                                    case "bordercolor":
                                        if (setting.length >= 2) {
                                            mBorderColor = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "linkcolor":
                                        if (setting.length >= 2) {
                                            mHyperlinkColor = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "wborderx":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            mBorderWidthPX = GLKUtils.dpToPx(Integer.parseInt(setting[1]), dm);
                                            if (mBorderWidthPX == 0 && mBorderHeightPX == 0) {
                                                mBorderStyle = 1;   // never show borders
                                            } else {
                                                mBorderStyle = 0;   // always show borders
                                            }
                                        }
                                        break;
                                    case "wbordery":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            mBorderHeightPX = GLKUtils.dpToPx(Integer.parseInt(setting[1]), dm);
                                            if (mBorderWidthPX == 0 && mBorderHeightPX == 0) {
                                                mBorderStyle = 1;   // never show borders
                                            } else {
                                                mBorderStyle = 0;   // always show borders
                                            }
                                        }
                                        break;
                                    case "tmarginx":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            int m = Integer.parseInt(setting[1]);
                                            if (m >= 0) {
                                                mDefaultMarginsTextBufPX.left = mDefaultMarginsTextBufPX.right =
                                                        GLKUtils.dpToPx(m, dm);
                                            }
                                        }
                                        break;
                                    case "tmarginy":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            int m = Integer.parseInt(setting[1]);
                                            if (m >= 0) {
                                                mDefaultMarginsTextBufPX.top = mDefaultMarginsTextBufPX.bottom =
                                                        GLKUtils.dpToPx(m, dm);
                                            }
                                        }
                                        break;
                                    case "gmarginx":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            int m = Integer.parseInt(setting[1]);
                                            if (m >= 0) {
                                                mDefaultMarginsTextGridPX.left = mDefaultMarginsTextGridPX.right =
                                                        GLKUtils.dpToPx(m, dm);
                                            }
                                        }
                                        break;
                                    case "gmarginy":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            int m = Integer.parseInt(setting[1]);
                                            if (m >= 0) {
                                                mDefaultMarginsTextGridPX.top = mDefaultMarginsTextGridPX.bottom =
                                                        GLKUtils.dpToPx(m, dm);
                                            }
                                        }
                                        break;
                                    case "wmarginx":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            int m = Integer.parseInt(setting[1]);
                                            if (m >= 0) {
                                                mDefaultWindowMarginsPX.left = mDefaultWindowMarginsPX.right =
                                                        GLKUtils.dpToPx(m, dm);
                                            }
                                        }
                                        break;
                                    case "wmarginy":
                                        if (setting.length >= 2) {
                                            // if any exception is thrown, will be caught
                                            // at bottom of this select block:
                                            int m = Integer.parseInt(setting[1]);
                                            if (m >= 0) {
                                                mDefaultWindowMarginsPX.top = mDefaultWindowMarginsPX.bottom =
                                                        GLKUtils.dpToPx(m, dm);
                                            }
                                        }
                                        break;
                                    case "windowcolor":
                                        if (setting.length >= 2) {
                                            mBackgroundColor = parseConfigColor(setting[1]);
                                        }
                                        break;
                                    case "keyboard":
                                        if (setting.length >= 2) {
                                            mKeyboardName = setting[1];
                                            GLKLogger.debug("    Set keyboard to '" + mKeyboardName + "'");
                                        }
                                        break;
                                    default:
                                        GLKLogger.error("    Don't (yet) recognise config setting '" + line + "'. Skipping.");
                                        break;
                                }
                            } catch (IllegalArgumentException e) {
                                // note this catches NumberFormatException (inter alia)
                                GLKLogger.error("    Could not process setting on line " + lineno +
                                        " of config file - one or more illegal argument(s): '" + line + "'");
                            }
                        }
                    }
                }
            }
            if (leading > 0) {
                mDefaultLeadingTextBuf = Math.max(leading / propsize, 1f);
                //mDefaultLeadingTextGrid = Math.max(leading / mDefaultFontSizeTextGrid, 1f);
            }
            if (mTADSStatusStyle != null) {
                setTextColor(mTADSStatusStyle, mTadsColors[TADS_COLOR_STATUSTEXT], mTadsColors[TADS_COLOR_STATUSBG]);
            }
        } catch (FileNotFoundException e) {
            // shouldn't get here, as we already tested for existence above!
        } catch (IOException e) {
            GLKLogger.error("readConfigFile: IO exception on reading config file: " + e.getMessage());
        }
    }

    public void updateView() throws InterruptedException {
        if (mScreen != null) {
            GLKScreen scr = mScreen.get();
            if (scr != null) {
                scr.postUpdate(this);
                return;
            }
        }
        GLKLogger.warn("GLKModel: Couldn't update View as have lost reference to screen!");
    }

    public void attachToScreen(GLKScreen scr) {
        mScreen = new WeakReference<>(scr);
    }

    public void attachToFragmentManager(FragmentManager fm) {
        mFragMgr = new WeakReference<>(fm);
    }

    public void shutdown() {
        // Now shut down the Model
        GLKLogger.debug("Shutting down Model...");
        mResourceMgr.clearImageCache();
        mStreamMgr.closeAllStreams(mRootWinID);
        mStreamMgr.deleteTempFiles();
    }
}
