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
package com.luxlunae.fabularium.play;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.glk.GLKConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * The IFictionRecord encapsulates game metadata using the fields set out in the Treaty of Babel
 * IF metadata standard. It also provides an interface to the underlying Babel C library via
 * JNI.
 */
public class IFictionRecord {
    /* The treaty insists that programs supply output buffers at least this big (in bytes) */
    public static final int TREATY_MINIMUM_EXTENT = 512;

    /* Babel Treaty return codes */
    private static final int NO_REPLY_RV = 0;
    private static final int INVALID_STORY_FILE_RV = -1;
    private static final int UNAVAILABLE_RV = -2;
    private static final int INVALID_USAGE_RV = -3;
    private static final int INCOMPLETE_REPLY_RV = -4;
    private static final int VALID_STORY_FILE_RV = 1;

    /* Babel Treaty selectors */
    private static final int GET_HOME_PAGE_SEL = 0x201;
    private static final int GET_FORMAT_NAME_SEL = 0x202;
    private static final int GET_FILE_EXTENSIONS_SEL = 0x203;
    private static final int CLAIM_STORY_FILE_SEL = 0x104;
    private static final int GET_STORY_FILE_METADATA_EXTENT_SEL = 0x105;
    private static final int GET_STORY_FILE_COVER_EXTENT_SEL = 0x106;
    private static final int GET_STORY_FILE_COVER_FORMAT_SEL = 0x107;
    private static final int GET_STORY_FILE_IFID_SEL = 0x308;
    private static final int GET_STORY_FILE_METADATA_SEL = 0x309;
    private static final int GET_STORY_FILE_COVER_SEL = 0x30A;
    private static final int GET_STORY_FILE_EXTENSION_SEL = 0x30B;

    static {
        System.loadLibrary("babel");
    }

    String file_path;
    public String[] ifids;
    @Nullable
    public String format;
    boolean isFormatAuthoritative;
    public String title;
    public String description;
    int bafn;
    public String author;
    public String headline;
    public String firstpublished;
    public String genre;
    String forgiveness;
    public String group;
    public String language;
    public String series;
    public int seriesnumber;
    float starrating;
    int ratingcounttot;
    public String coverURL;

    /**
     * Initializes the babel handler with the given file.  This function must
     * be called successfully before any other babel handler function can be called.
     *
     * @param story_path - the path to the story file.
     *
     * @return on a successful load, this returns the name of the format.
     * If this fails, it will return NULL.  If it returns NULL, you must not
     * use any of the other functions, except babel_release,
     * babel_md5_ifid and babel_get_length.
     */
    @Nullable
    @SuppressWarnings("JniMissingFunction")
    private static native String nBabelInit(String story_path);

    /**
     * Dispatches the call to the treaty handler for the currently loaded file.
     *
     * @param selector - one of GET_HOME_PAGE_SEL, GET_FORMAT_NAME_SEL, GET_FILE_EXTENSIONS_SEL,
     *                 CLAIM_STORY_FILE_SEL, GET_STORY_FILE_METADATA_EXTENT_SEL,
     *                 GET_STORY_FILE_COVER_EXTENT_SEL, GET_STORY_FILE_COVER_FORMAT_SEL.
     *                 GET_STORY_FILE_IFID_SEL. GET_STORY_FILE_METADATA_SEL, GET_STORY_FILE_COVER_SEL
     * @param output - the output buffer for storing the result
     * @return - either:
     *              NO_REPLY_RV - there is no meaningful return value, or if the requested data
     *              cannot be found in the story file, but there was no indication that the story
     *              file was broken or invalid.
     *
     *              INVALID_STORY_FILE_RV - something went wrong, and the story file looks
     *              to be broken or invalid.
     *
     *              UNAVAILABLE_RV - support for this selector is not provided, the selector is
     *              not recognised.
     *
     *              INVALID_USAGE_RV - the selector believes it has been called with parameters
     *              which are not as they should be.
     *
     *              VALID_STORY_FILE_RV
     *
     *              INCOMPLETE_REPLY_RV
     */
    @SuppressWarnings("JniMissingFunction")
    private static native int nBabelTreaty(int selector, ByteBuffer output);

    /**
     * Checks if the story file format has been positively identified.
     * @return TRUE if a positive ID, FALSE otherwise.
     */
    @SuppressWarnings("JniMissingFunction")
    private static native boolean nBabelGetAuthoritative();


    /**
     * Generates an MD5 IFID from the loaded story.
     *
     * @param ifid - if successful, will contain the IFID.
     * @return TRUE if success, FALSE otherwise.
     */
    @SuppressWarnings("JniMissingFunction")
    private static native boolean nBabelGenerateIFID(ByteBuffer ifid);

    /**
     * Releases the resources held by the given babel handler context.
     *
     * You must call this when you are done with the file loaded by babel_init_ctx, even
     * if babel_init_ctx returned NULL.  Once this is called, the babel handler
     * context must not be used until it is re-initialized.
     */
    @SuppressWarnings("JniMissingFunction")
    private static native void nBabelRelease();

    static void createFormattedString(@NonNull final IFictionRecord ifr, @NonNull SpannableStringBuilder sb) {
        int pos1 = 0, pos2;

        sb.append(ifr.title);
        pos2 = sb.length();
        sb.setSpan(new RelativeSizeSpan(1.4f), pos1, pos2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), pos1, pos2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        pos1 = pos2 + 1;

        if (ifr.headline != null) {
            sb.append("\n").append(ifr.headline);
            pos2 = sb.length();
            sb.setSpan(new RelativeSizeSpan(1.2f), pos1, pos2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos1 = pos2 + 1;
        }

        String author = (ifr.author != null ? "By " + ifr.author : "By (unknown)");
        sb.append("\n\n").append(author);
        pos2 = sb.length();
        sb.setSpan(new RelativeSizeSpan(1.1f), pos1, pos2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        pos1 = pos2 + 1;

        boolean appended = false;
        if (ifr.genre != null) {
            sb.append("\n\n").append(ifr.genre);
            pos2 = sb.length();
            sb.setSpan(new StyleSpan(Typeface.ITALIC), pos1, pos2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            pos1 = pos2 + 1;
            appended = true;
        }

        if (ifr.firstpublished != null) {
            if (!appended) {
                sb.append("\n\n");
            } else {
                sb.append("\n");
            }

            // Try to change the date into a nice format
            // We assume the server has returned in format like 2014-10-01
            // We want to display in format 1 Oct 2014
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date newDate;
            try {
                newDate = format.parse(ifr.firstpublished);
                format = new SimpleDateFormat("dd MMM yyyy", Locale.US);
                sb.append(format.format(newDate));
            } catch (ParseException e) {
                // give up and use the original
                sb.append(ifr.firstpublished);
            }
            pos2 = sb.length();
            pos1 = pos2 + 1;
        }

        if (ifr.starrating != 0) {
            sb.append("\n").append(String.valueOf(ifr.starrating)).append(" stars");
            if (ifr.ratingcounttot != 0) {
                sb.append(" (based on ").append(String.valueOf(ifr.ratingcounttot)).append(" ratings)");
            }
            pos2 = sb.length();
            pos1 = pos2 + 1;
        }

        if (ifr.description != null) {
            sb.append("\n\n").append(ifr.description.trim());
            pos2 = sb.length();
            pos1 = pos2 + 1;
        }

        sb.append("\n\nGame Details\n");
        pos2 = sb.length();
        sb.setSpan(new RelativeSizeSpan(1.1f), pos1, pos2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new StyleSpan(Typeface.BOLD), pos1, pos2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        sb.append("IFID(s):     ");
        for (int i = 0, sz = ifr.ifids.length; i < sz; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(ifr.ifids[i]);
        }
        sb.append("\nPath:        ").append(ifr.file_path);
        sb.append("\nFormat:      ").append(ifr.format);
        if (ifr.language != null) {
            sb.append("\nLanguage:    ").append(ifr.language);
        }
        if (ifr.bafn != 0) {
            sb.append("\nBAF's ID:    ").append(String.valueOf(ifr.bafn));
        }
        if (ifr.forgiveness != null) {
            sb.append("\nForgiveness: ").append(ifr.forgiveness);
        }
    }

    /**
     * You must call this function before calling any other function prefixed
     * by opXXXX. If this function fails, you don't need to call stopOps().
     *
     * @param ifr - the IFictionRecord.
     * @return TRUE if successful (and ifr.format will be set to the recognised
     * game format), or FALSE otherwise (and ifr.format will be NULL).
     */
    static boolean startOps(@NonNull IFictionRecord ifr) {
        // On a successful load, this returns the name of the format.
        // If this fails, it will return NULL.
        ifr.format = nBabelInit(ifr.file_path);
        if (ifr.format == null) {
            stopOps();
            return false;
        }
        ifr.isFormatAuthoritative = nBabelGetAuthoritative();
        return true;
    }

    /**
     * Call this once you have finished performing operations on an
     * IFiction record, to release native memory.
     */
    static void stopOps() {
        nBabelRelease();
    }

    /**
     * Attempts to load IFIDs into the IFictionRecord by interrogating the backing game file.
     * DO NOT CALL THIS IF startOps FAILED OR HAS NOT BEEN CALLED SINCE stopOps.
     *
     * @param ifr         - the IFictionRecord.
     * @param tmp_storage - a temporary memory buffer for transferring data to/from JNI.
     * @return TRUE if loaded the IFIDs, FALSE otherwise.
     */
    static boolean opLoadIFIDsFromGameFile(@NonNull IFictionRecord ifr, @NonNull ByteBuffer tmp_storage) {
        // Get the IFIDs
        // The IFID shall be a sequence of between 8 and 63 characters, each of
        // which shall be a digit, a capital letter or a hyphen.
        tmp_storage.clear();
        int i = nBabelTreaty(GET_STORY_FILE_IFID_SEL, tmp_storage);
        if (i != NO_REPLY_RV) {
            // Returns NO_REPLY_RV if no IFID could be determined
            // Otherwise finds the IFID(s) and copies them as a comma-separated list into
            // the output buffer, and then returns the number of IFIDs in the list.
            // TODO: Make this more efficient - can we avoid allocating memory for a new String object?
            byte[] arr = tmp_storage.array();
            int start = tmp_storage.arrayOffset();
            int end = start;
            int sz = arr.length;
            while (end < sz && arr[end] != 0) {
                end++;
            }
            String t = new String(arr, start, end - start, Charset.forName("UTF-8"));
            ifr.ifids = t.split(",");
            setTitleFromIFID(ifr);
            return true;
        } else {
            FabLogger.error("Could not determine IFID for " + ifr.file_path);
        }
        return false;
    }

    static boolean opGenerateIFIDForGameFile(@NonNull IFictionRecord ifr, @NonNull ByteBuffer tmp_storage) {
        tmp_storage.clear();
        if (nBabelGenerateIFID(tmp_storage)) {
            byte[] arr = tmp_storage.array();
            int start = tmp_storage.arrayOffset();
            int end = start;
            int sz = arr.length;
            while (end < sz && arr[end] != 0) {
                end++;
            }
            String t = new String(arr, start, end - start, Charset.forName("UTF-8"));
            ifr.ifids = t.split(",");
            return true;
        } else {
            FabLogger.error("Could not generate IFID for " + ifr.file_path);
        }
        return false;
    }

    /**
     * Attempt to work out the title of the game using the IFID.
     * Currently this only works for Magnetic Scolls and Level 9 games.
     *
     * @param ifr - the record (should already have IFID set).
     */
    private static void setTitleFromIFID(@NonNull IFictionRecord ifr) {
        if (ifr.format == null) {
            return;
        }
        if (ifr.format.equals("magscrolls")) {
            // ID should be of form "MAGNETIC-x"
            int l = ifr.ifids[0].length();
            if (l == 10 && ifr.ifids[0].startsWith("MAGNETIC-")) {
                char x = ifr.ifids[0].charAt(9);
                switch (x) {
                    case '1':
                        ifr.title = "The Pawn";
                        break;
                    case '2':
                        ifr.title = "The Guild of Thieves";
                        break;
                    case '3':
                        ifr.title = "Jinxter";
                        break;
                    case '4':
                        ifr.title = "Corruption";
                        break;
                    case '5':
                        ifr.title = "Fish!";
                        break;
                    case '6':
                        ifr.title = "Myth";
                        break;
                    case '7':
                        ifr.title = "Wonderland";
                        break;
                }
            }
        } else if (ifr.format.equals("level9")) {
            // ID should be of form "LEVEL9-xxx-y"
            int l = ifr.ifids[0].length();
            if (l > 9 && ifr.ifids[0].startsWith("LEVEL9-")) {
                int x;
                try {
                    x = Integer.parseInt(ifr.ifids[0].substring(7, 10));
                } catch (NumberFormatException e) {
                    // give up
                    return;
                }
                char y = (l == 12) ? ifr.ifids[0].charAt(11) : 1;
                switch (x) {
                    case 1:
                        ifr.title = "Adrian Mole I " + "(" + y + ")";
                        break;
                    case 2:
                        ifr.title = "Adrian Mole II " + "(" + y + ")";
                        break;
                    case 3:
                        ifr.title = "Adventure Quest";
                        break;
                    case 4:
                        ifr.title = "Champion of the Raj";
                        break;
                    case 5:
                        ifr.title = "Colossal Adventure";
                        break;
                    case 6:
                        ifr.title = "Dungeon Adventure";
                        break;
                    case 7:
                        ifr.title = "Emerald Isle";
                        break;
                    case 8:
                        ifr.title = "Erik the Viking";
                        break;
                    case 9:
                        ifr.title = "Gnome Ranger " + "(" + y + ")";
                        break;
                    case 10:
                        ifr.title = "Ingrid's Back " + "(" + y + ")";
                        break;
                    case 11:
                        ifr.title = "Knight Orc " + "(" + y + ")";
                        break;
                    case 12:
                        ifr.title = "Lancelot " + "(" + y + ")";
                        break;
                    case 13:
                        ifr.title = "Lords of Time";
                        break;
                    case 14:
                        ifr.title = "Price of Magik";
                        break;
                    case 15:
                        ifr.title = "Red Moon";
                        break;
                    case 16:
                        ifr.title = "Return to Eden";
                        break;
                    case 17:
                        ifr.title = "Scapeghost";
                        break;
                    case 18:
                        ifr.title = "Snowball";
                        break;
                    case 19:
                        ifr.title = "The Archers";
                        break;
                    case 20:
                        ifr.title = "Worm in Paradise";
                        break;
                }
            }
        }
    }

    /**
     * Attempts to load a cover image into the IFictionRecord by interrogating the backing game file.
     * Then saves a copyFile of the image to the disk.
     * DO NOT CALL THIS IF startOps FAILED OR HAS NOT BEEN CALLED SINCE stopOps.
     *
     * @param ifr         - the IFictionRecord.
     * @param tmp_storage - a temporary memory buffer for transferring data to/from JNI.
     * @return TRUE if successfully extracted the cover image, FALSE otherwise.
     */
    static boolean opSaveCoverFromGameFile(@NonNull Context c, @NonNull IFictionRecord ifr, @NonNull ByteBuffer tmp_storage) {
        tmp_storage.clear();
        int i = nBabelTreaty(GET_STORY_FILE_COVER_SEL, tmp_storage);
        if (i != NO_REPLY_RV && i != INVALID_USAGE_RV) {
            // If there is no cover art, returns NO_REPLY_RV.
            // If the output buffer is too small, returns IMPROPER_USAGE_RV.
            // Otherwise, copies the cover art for the story file into the output buffer,
            // returning the size in bytes
            try {
                saveCoverToDisk(c, tmp_storage.array(), tmp_storage.arrayOffset(), i, ifr.ifids[0]);
            } catch (IOException e) {
                return false;
            }
            return true;
        } else if (i == INVALID_USAGE_RV) {
            FabLogger.error("Not enough memory to hold cover image for " + ifr.file_path);
        }
        return false;
    }

    /**
     * Attempts to load metadata into the IFictionRecord by interrogating the backing game file.
     * DO NOT CALL THIS IF startOps FAILED OR HAS NOT BEEN CALLED SINCE stopOps.
     *
     * @param ifr         - the IFictionRecord.
     * @param tmp_storage - a temporary memory buffer for transferring data to/from JNI.
     * @return TRUE if successfully loaded the metadata, FALSE otherwise.
     */
    static boolean opLoadMetadataFromFile(@NonNull IFictionRecord ifr, @NonNull ByteBuffer tmp_storage) {
        tmp_storage.clear();
        int i = nBabelTreaty(GET_STORY_FILE_METADATA_SEL, tmp_storage);
        if (i != NO_REPLY_RV && i != INVALID_USAGE_RV) {
            // If there is no metadata, returns NO_REPLY_RV.
            // If the output buffer is too small, returns IMPROPER_USAGE_RV.
            // Otherwise copies the metadata for the story file, in "iFiction" format, to
            // the output buffer, together with a zero termination byte, and
            // returns the total number of bytes copied (including the zero
            // termination byte). Encoding must be UTF-8 Unicode.
            // TODO: Make this more efficient - can we avoid allocating memory for a new String object?
            String meta = new String(tmp_storage.array(), tmp_storage.arrayOffset(), i, Charset.forName("UTF-8"));
            try {
                return loadMetadataFromXML(ifr, meta);
            } catch (IOException e) {
                FabLogger.error("Could not load metadata: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    static boolean isPlayableStory(@NonNull IFictionRecord ifr, @NonNull String ext) {
        // Currently we consider a story file to be playable if:
        //   1) its identified format is not executable and authoritative;  OR
        //   2) its identified format is executable and not authoritative, but
        //      the extension seems to imply a specific terp.
        switch (ext) {
            case "html":
            case "htm":
                ifr.format = "html";
                ifr.isFormatAuthoritative = true;
                return true;
            default:
                if (ifr.format == null) {
                    return false;
                }
                boolean isExe = ifr.format.equals("executable");
                return !isExe && ifr.isFormatAuthoritative ||
                        isExe && !ifr.isFormatAuthoritative && isValidExeExtension(ifr, ext);
        }
    }

    private static boolean isValidExeExtension(@NonNull IFictionRecord ifr, @NonNull String ext) {
        switch (ext) {
            case "saga":
                ifr.format = "scott";
                return true;
            case "html":
            case "htm":
                ifr.format = "html";
                return true;
            default:
                return false;
        }
    }

    private static void loadMetadataFromTag(@NonNull IFictionRecord ifr, @NonNull Document d, @NonNull String tagName) {
        NodeList nl = d.getElementsByTagName(tagName);
        if (nl.getLength() > 0) {
            Node node = nl.item(0);
            NodeList children = node.getChildNodes();
            for (int i1 = 0, sz = children.getLength(); i1 < sz; ++i1) {
                Node child = children.item(i1);
                String name = child.getNodeName().toLowerCase();
                NodeList grandchildren = child.getChildNodes();
                int sz2 = grandchildren.getLength();
                if (sz2 == 0) {
                    continue;
                }

                String grandChildValue = child.getFirstChild().getNodeValue();

                try {
                    switch (name) {
                        // COLOPHON
                        //      <generator>, <generatorversion>, <originated>

                        // IDENTIFICATION
                        //      <ifid>, <bafn>, <format>
                        case "bafn":
                            ifr.bafn = Integer.parseInt(grandChildValue);
                            break;
                        case "format":
                            ifr.format = grandChildValue;
                            break;
                        // BIBLIOGRAPHIC
                        //      <title>, <author>, <language>, <headline>, <firstpublished>
                        //      <genre>, <group>, <description>, <series>, <seriesnumber>,
                        //      <forgiveness>
                        case "title":
                            ifr.title = grandChildValue;
                            break;
                        case "author":
                            ifr.author = grandChildValue;
                            break;
                        case "language":
                            ifr.language = grandChildValue;
                            break;
                        case "headline":
                            ifr.headline = grandChildValue;
                            break;
                        case "firstpublished":
                            ifr.firstpublished = grandChildValue;
                            break;
                        case "genre":
                            ifr.genre = grandChildValue;
                            break;
                        case "group":
                            ifr.group = grandChildValue;
                            break;
                        case "description":
                            StringBuilder sb = new StringBuilder();
                            for (int i2 = 0; i2 < sz2; ++i2) {
                                String s = grandchildren.item(i2).getNodeValue();
                                if (s != null) {
                                    if (sb.length() > 0) {
                                        sb.append("\n\n");
                                    }
                                    sb.append(s);
                                }
                            }
                            if (sb.length() > 0) {
                                ifr.description = sb.toString();
                            }
                            break;
                        case "series":
                            ifr.series = grandChildValue;
                            break;
                        case "seriesnumber":
                            ifr.seriesnumber = Integer.parseInt(grandChildValue);
                            break;
                        case "forgiveness":
                            ifr.forgiveness = grandChildValue;
                            break;
                        // RESOURCES
                        //      <auxiliary>
                        // (Ignored)

                        // CONTACT
                        //      <url>, <authoremail>
                        // (Ignored)

                        // IFDB
                        //      <tuid>
                        //      <link>
                        //      <coverart>
                        //          <url>
                        //      <averageRating>
                        //      <starRating>
                        //      <ratingCountAvg>
                        //      <ratingCountTot>
                        case "coverart":
                            for (int i2 = 0; i2 < sz2; ++i2) {
                                Node grandChild = grandchildren.item(i2);
                                String grandChildName = grandChild.getNodeName().toLowerCase();
                                switch (grandChildName) {
                                    case "url":
                                        ifr.coverURL = grandChild.getFirstChild().getNodeValue();
                                        break;
                                }
                            }
                            break;
                        case "starrating":
                            ifr.starrating = Float.parseFloat(grandChildValue);
                            break;
                        case "ratingcounttot":
                            ifr.ratingcounttot = Integer.parseInt(grandChildValue);
                            break;
                        default:
                            // Logger.error("IFictionRecord: skipped tag " + name);
                            break;
                    }
                } catch (NumberFormatException e) {
                    FabLogger.warn("IFictionRecord: loadMetadataFromTag: could not convert a number: " + grandChildValue + ".. skipping.");
                }
            }
        }
    }

    public static boolean loadMetadataFromXML(@NonNull IFictionRecord ifr, @Nullable final String xml) throws IOException {
        // Logger.error("IFR: loadMetadataFromXML: " + xml);

        // Process the XML and load all relevant meta data
        Document d = processXML(xml);
        if (d == null) {
            throw new IOException("cannot load metadata from malformed xml: " + xml);
        }

        // Get containers, one by one
        loadMetadataFromTag(ifr, d, "colophon");
        loadMetadataFromTag(ifr, d, "identification");
        loadMetadataFromTag(ifr, d, "bibliographic");
        loadMetadataFromTag(ifr, d, "contact");
        loadMetadataFromTag(ifr, d, "ifdb");
        return true;
    }

    @Nullable
    private static Document processXML(@Nullable final String xml) {
        if (xml == null) return null;

        Document doc;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setCoalescing(true);

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(xml));
            doc = db.parse(is);
        } catch (ParserConfigurationException e) {
            return null;
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return doc;
    }

    public static void saveCoverToDisk(@NonNull Context c, @NonNull byte[] b,
                                       int offset, int length, @NonNull String ifid) throws IOException {
        // save the complete bitmap to disk
        FileOutputStream out = null;
        Bitmap bmp = BitmapFactory.decodeByteArray(b, offset, length);
        if (bmp != null) {
            try {
                out = new FileOutputStream(GLKConstants.getDir(c, GLKConstants.SUBDIR.GAMEDATA) + "/" + ifid + "/cover.png");
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            bmp.recycle();
        }
    }
}
