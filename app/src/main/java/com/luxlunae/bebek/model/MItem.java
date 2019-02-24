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

import com.luxlunae.bebek.MGlobals;
import com.luxlunae.bebek.VB;
import com.luxlunae.bebek.model.collection.MStringHashMap;
import com.luxlunae.bebek.model.io.MFileIO;
import com.luxlunae.glk.GLKLogger;

import org.xmlpull.v1.XmlPullParser;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import static com.luxlunae.bebek.MGlobals.dateFormatter;
import static com.luxlunae.bebek.MGlobals.getBool;
import static com.luxlunae.bebek.model.io.MFileIO.LoadItemEnum.No;
import static com.luxlunae.bebek.model.io.MFileIO.LoadItemEnum.Yes;

public abstract class MItem implements Cloneable {
    private static final String XTAG_KEY = "Key";
    private static final String XTAG_LIBRARY = "Library";
    private static final String XTAG_REPLACE_TASK = "ReplaceTask";
    private static final String XTAG_LAST_UPDATED = "LastUpdated";
    private static final String XTAG_PRIORITY = "Priority";

    @NonNull
    protected final MAdventure mAdv;
    /**
     * A key is a unique identifier for the item that
     * consists of letters, numbers, and the underline
     * character "_", but cannot contain any spaces.
     */
    @NonNull
    private String mKey = "";
    @Nullable
    private Date mLastUpdated;
    private boolean mIsLibrary;

    public MItem(@NonNull MAdventure adv) {
        mAdv = adv;
    }

    /**
     * Splits an input string into a prefix and an article.
     *
     * @param in - the input string.
     * @return a String array containing the article (or
     * empty string) in index 0 and the prefix (or empty
     * string) in article 1.
     */
    @NonNull
    static String[] convertPrefix(@NonNull final String in) {
        String ret[] = new String[2];
        if (in.startsWith("a ") || in.startsWith("an ") ||
                in.startsWith("some ") || in.startsWith("his ") ||
                in.startsWith("hers ") || in.startsWith("the ")) {
            String[] words = in.split("\\s+", 2);
            ret[0] = words[0];
            ret[1] = words[1];
        } else {
            switch (in.toLowerCase()) {
                case "":
                    ret[0] = "a";
                    ret[1] = "";
                    break;
                case "a":
                case "an":
                case "hers":
                case "his":
                case "my":
                case "some":
                case "the":
                case "your":
                    // no prefix
                    ret[0] = in;
                    ret[1] = "";
                    break;
                default:
                    // no article
                    ret[0] = "";
                    ret[1] = in;
                    break;
            }
        }
        return ret;
    }

    /**
     * Find text in the given description and optionally replace it.
     *
     * @param toSearch  - the description to search.
     * @param toFind    - the text to find.
     * @param toReplace - (may be NULL) what to replace found text with.
     * @param findAll   - whether to find all occurences or just the first.
     * @param nReplaced - upon completion this variable will be incremented
     *                  by the number of replacements made.
     * @return if findAll is FALSE, returns a reference to the first
     * description containing the given text, if found. Otherwise returns
     * NULL.
     */
    @Nullable
    private static MSingleDescription find(@NonNull MDescription toSearch,
                                           @NonNull String toFind,
                                           @Nullable String toReplace,
                                           boolean findAll, int[] nReplaced) {
        for (MSingleDescription sd : toSearch) {
            if (MGlobals.contains(sd.mDescription, toFind)) {
                if (toReplace != null) {
                    String[] t = new String[1];
                    t[0] = sd.mDescription;
                    sd.mDescription = MGlobals.replace(t, toFind, toReplace, nReplaced);
                }
                if (!findAll) {
                    return sd;
                }
            }
            for (MRestriction rest : sd.mRestrictions) {
                MSingleDescription sd2 =
                        find(rest.mMessage,
                                toFind, toReplace, findAll, nReplaced);
                if (sd2 != null && !findAll) {
                    return sd2;
                }
            }
        }
        return null;
    }

    @NonNull
    public abstract ArrayList<MDescription> getAllDescriptions();

    @Override
    @Nullable
    public MItem clone() {
        try {
            return (MItem) super.clone();
        } catch (CloneNotSupportedException e) {
            GLKLogger.error("Couldn't clone item: " + e.getMessage());
            return null;
        }
    }

    @NonNull
    public abstract String getCommonName();

    public boolean getIsLibrary() {
        return mIsLibrary;
    }

    private void setIsLibrary(boolean value) {
        mIsLibrary = value;
    }

    @NonNull
    public String getKey() {
        return mKey;
    }

    public void setKey(@NonNull String value) {
        mKey = value;
    }

    @NonNull
    private Date getLastUpdated() {
        if (mLastUpdated != null) {
            return mLastUpdated;
        }
        return new Date();
    }

    private void setLastUpdated(@NonNull Date value) {
        mLastUpdated = value;
    }

    public abstract boolean deleteKey(@NonNull String key);

    public String getRegEx() {
        return getRegEx(false, false);
    }

    protected String getRegEx(boolean getADRIFTExpr, boolean usePluralForm) {
        // by default we do nothing
        // children may override
        return "";
    }

    public boolean containsText(@NonNull String toFind) {
        Object ret = find(toFind);
        return ret instanceof MSingleDescription || (VB.cint(ret) > 0);
    }

    @NonNull
    protected static String incrementKey(@NonNull String key) {
        // Grab the numeric part of the key (if any) and increment it
        String keyPrefix = key.replaceAll("\\d*$", "");
        String keyNumber = key.replace(keyPrefix, "");
        if (keyNumber.equals("")) {
            keyNumber = "0";
        }
        int keyNum = Integer.parseInt(keyNumber);
        return keyPrefix + (keyNum + 1);
    }

    @NonNull
    private Object find(@NonNull String toFind) {
        return find(toFind, null, false, new int[1]);
    }

    @NonNull
    private Object find(@NonNull String toFind, @Nullable String toReplace,
                        boolean findAll, @NonNull int[] nReplaced) {
        for (MDescription desc : getAllDescriptions()) {
            Object obj = find(desc, toFind, toReplace, findAll, nReplaced);
            if (obj != null && !findAll) {
                return obj;
            }
        }
        return findLocal(toFind, toReplace, findAll, nReplaced);
    }

    protected int findLocal(@NonNull String toFind) {
        return findLocal(toFind, null, true, new int[1]);
    }

    public int find(@NonNull String toFind, @NonNull String toReplace) {
        int[] result = new int[1];
        find(toFind, toReplace, true, result);
        return result[0];
    }

    /**
     * Find and optionally replace text in this object.
     * <p>
     * The implementation may vary depending upon the type
     * of the object.
     *
     * @param toFind    - text to find.
     * @param toReplace - text to replace (nay be NULL, if no replacements to be made)
     * @param findAll   - whether to find/replace all occurrences or just the first.
     * @param nReplaced - a running counter of total replacements.
     *
     * @return the number of replacements made in this object's text.
     */
    protected abstract int findLocal(@NonNull String toFind, @Nullable String toReplace,
                                     boolean findAll, @NonNull int[] nReplaced);

    /**
     * Return the number of times the given key is referenced by
     * this item (e.g. in the item's descriptions)
     *
     * @param key - the key to find.
     * @return the count of references to the key.
     */
    public abstract int getKeyRefCount(@NonNull String key);

    static class ItemHeaderDetails {
        private String mKey = null;
        private String mLastUpdated = null;
        private Boolean mItemIsLibrary = null;
        private Boolean mReplaceTask = null;
        private Integer mPriority = null;

        @NonNull
        private static MFileIO.LoadItemEnum shouldWeLoad(@NonNull ArrayList<String> itemsToExclude,
                                                         @NonNull String itemKey) {
            return itemsToExclude.contains(itemKey) ? No : Yes;
        }

        /**
         * Attempt to process a header tag.
         *
         * @param xpp - the XML parser. This should be at the start
         *            of a header tag.
         * @return TRUE if successfully processed, FALSE otherwise.
         * @throws Exception if there was a processing error.
         */
        boolean processTag(@NonNull XmlPullParser xpp) throws Exception {
            String s;
            switch (xpp.getName()) {
                case XTAG_KEY:
                    mKey = xpp.nextText();
                    return true;

                case XTAG_LIBRARY:
                    s = xpp.nextText();
                    if (!s.equals("")) {
                        mItemIsLibrary = getBool(s);
                    }
                    return true;

                case XTAG_REPLACE_TASK:
                    s = xpp.nextText();
                    if (!s.equals("")) {
                        mReplaceTask = getBool(s);
                    }
                    return true;

                case XTAG_LAST_UPDATED:
                    s = xpp.nextText();
                    if (!s.equals("")) {
                        mLastUpdated = s;
                    }
                    return true;

                case XTAG_PRIORITY:
                    s = xpp.nextText();
                    if (!s.equals("")) {
                        mPriority = VB.cint(s);
                    }
                    return true;

                default:
                    //GLKLogger.warn("Unrecognised header tag: '" +
                    //               xpp.getName() + "'");
                    xpp.nextText();
                    return false;
            }
        }

        /**
         * Finalise processing of an item's header information, set relevant fields
         * in the item and determine if the item should be loaded.
         *
         * @param item       - the item to which the header information relates.
         * @param items      - a hashmap containing all items already loaded of the
         *                   same type as 'item'.
         * @param isLib  - set to TRUE if this item is being loaded
         *                   from a library file, or FALSE otherwise.
         * @param addDupKeys - Controls how we treat items with duplicate
         *                   keys. If the current item's key duplicates that
         *                   of an existing key - and: (1) this is TRUE: we
         *                   will generate a new unique key for the current item
         *                   before adding it; (2) this is FALSE: we
         *                   won't added the current item.
         * @param dupKeys    - if this is not NULL and the current item's key
         *                   duplicates that of an item already loaded and
         *                   addDupKeys = TRUE, we will store a mapping of
         *                   the duplicate key to the new generated key in this
         *                   hash map.
         * @return TRUE if the item should be loaded, FALSE if not.
         */
        boolean finalise(@NonNull MItem item,
                         @NonNull LinkedHashMap<String, ? extends MItem> items,
                         boolean isLib, boolean addDupKeys,
                         @Nullable MStringHashMap dupKeys) {

            ArrayList<String> itemsToExclude = item.mAdv.mExcludedItems;

            if (mKey == null) {
                // No key specified, so ignore it
                GLKLogger.warn("Skipping an item as no key specified.");
                return false;
            }

            Date lastUpdated = null;
            try {
                lastUpdated = (mLastUpdated != null) ?
                        dateFormatter.parse(mLastUpdated) : null;
            } catch (ParseException e) {
                GLKLogger.error("Could not parse last modified date for item '" +
                        mKey + "' (date string is " + mLastUpdated +
                        "). We will treat as though no date specified.");
                mLastUpdated = null;
            }

            if (mItemIsLibrary != null) {
                item.setIsLibrary(mItemIsLibrary);
            }

            boolean isTask = item instanceof MTask;
            MTask tas = isTask ? (MTask) item : null;
            if (isTask && mReplaceTask != null) {
                tas.mReplaceDuplicateKey = mReplaceTask;
            }

            MItem loadedItem = items.get(mKey);
            if (loadedItem != null) {
                // --------------------------------------------
                //         HANDLE DUPLICATE KEYS
                // --------------------------------------------
                // First handle any attempts to load a library item.
                if (item.getIsLibrary() || isLib) {
                    if (lastUpdated != null &&
                            !lastUpdated.after(loadedItem.getLastUpdated())) {
                        // Don't add this library item unless it was updated
                        // more recently than the item already loaded.
                        // If there's no timestamp, we assume this library
                        // item is newer.
                        GLKLogger.warn("Skipping item " + mKey +
                                " as its date is older than the already loaded item.");
                        return false;
                    }

                    // If this item should be loaded, then we
                    // remove the existing duplicate item and
                    // replace it with this one. If the item
                    // should not be loaded, we skip it unless
                    // it is a task, in which case we update
                    // the existing item's time stamp to now and
                    // do some further checking to decide
                    // how we handle it.
                    switch (shouldWeLoad(itemsToExclude, mKey)) {
                        case Yes:
                            items.remove(mKey);
                            break;
                        case No:
                            if (isTask) {
                                // Set the timestamp of the custom version
                                // to now, so it's more recent than the
                                // "newer" library.  That way we won't
                                // be prompted next time.
                                loadedItem.setLastUpdated(new Date());
                            } else {
                                GLKLogger.warn("Skipping item " + mKey +
                                        " as we are not to load this library item.");
                                return false;
                            }
                        case Both:
                            // Keep key, but still add this new one
                            break;
                    }
                }

                // Now handle all cases.
                if (isTask && tas.mReplaceDuplicateKey) {
                    // This task should replace any existing
                    // task with a duplicate key.
                    items.remove(mKey);
                } else if (addDupKeys) {
                    // Create a new unique identifier for this
                    // item, then add it. Optionally store
                    // the old to new key mapping in dupKeys
                    // (if it is not NULL).
                    String oldKey = mKey;
                    while (items.containsKey(mKey)) {
                        mKey = incrementKey(mKey);
                    }
                    if (dupKeys != null) {
                        dupKeys.put(oldKey, mKey);
                    }
                } else {
                    GLKLogger.warn("Skipping item " + mKey +
                            " as there is an existing duplicate key and setting is not to replace it.");
                    return false;
                }
            } else if (isLib && shouldWeLoad(itemsToExclude, mKey) == No) {
                // --------------------------------------------
                //   NOT A DUPLICATE KEY, BUT SHOULDN'T LOAD
                // --------------------------------------------
                GLKLogger.warn("Skipping library item " + mKey +
                        " as we are not to load this library item.");
                return false;
            }

            // ---------------------------------------------------
            //  WE'RE GOOD TO LOAD - FINISH SETTING ITEM'S FIELDS
            // ---------------------------------------------------
            itemsToExclude.remove(mKey);
            item.setKey(mKey);
            if (isTask && mPriority != null) {
                tas.setPriority(mPriority);
                if (isLib && !tas.getIsLibrary()) {
                    tas.setPriority(MTask.LIBRARY_START_TASK_PRIORITY + tas.getPriority());
                }
            }
            if (isLib) {
                item.setIsLibrary(true);
            }
            if (lastUpdated != null) {
                item.setLastUpdated(lastUpdated);
            }

            return true;
        }
    }

    /**
     * Returns a Unicode symbol representing this item.
     *
     * @return the Unicode symbol.
     */
    @NonNull
    public abstract String getSymbol();
}
