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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.luxlunae.fabularium.AsyncProgressFragment;
import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.glk.GLKConstants;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The IFictionDbHelper provides a single interface for querying and updating game metadata (IFiction records)
 * in the Fabularium database on the user's Android device. That database is accessible by name to any
 * class in the application, but not outside the application.
 * <p>
 * At some point, we may update this design pattern to follow the more recent Google recommendation,
 * which is to use the Room persistence library (see https://developer.android.com/guide/topics/data/data-storage.html#db)
 * Of course we would need to do that in a manner that doesn't cause too much inconvenience for existing
 * users.
 */
public class IFictionDbHelper extends SQLiteOpenHelper implements AsyncProgressFragment.RefreshDbHelper {
    static final String[] PROJ_SUMMARY = new String[]{
            IFictionEntry._ID,
            IFictionEntry.COLUMN_NAME_ENTRY_ID,
            IFictionEntry.COLUMN_NAME_TITLE
    };
    static final String[] PROJ_ALL = {
            IFictionEntry.COLUMN_NAME_ENTRY_ID,
            IFictionEntry.COLUMN_NAME_GAME_PATH,
            IFictionEntry.COLUMN_NAME_FORMAT,
            IFictionEntry.COLUMN_NAME_BAFN,
            IFictionEntry.COLUMN_NAME_TITLE,
            IFictionEntry.COLUMN_NAME_AUTHOR,
            IFictionEntry.COLUMN_NAME_DESCRIPTION,
            IFictionEntry.COLUMN_NAME_HEADLINE,
            IFictionEntry.COLUMN_NAME_FIRSTPUBLISHED,
            IFictionEntry.COLUMN_NAME_GENRE,
            IFictionEntry.COLUMN_NAME_FORGIVENESS,
            IFictionEntry.COLUMN_NAME_GROUP,
            IFictionEntry.COLUMN_NAME_LANGUAGE,
            IFictionEntry.COLUMN_NAME_SERIES,
            IFictionEntry.COLUMN_NAME_SERIESNUM,
            IFictionEntry.COLUMN_NAME_STARRATING,
            IFictionEntry.COLUMN_NAME_RATINGCOUNTTOT
    };
    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "IFictionMetadata.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String REAL_TYPE = " REAL";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + IFictionEntry.TABLE_NAME + " ( " +
                    IFictionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +  // we need to do this so we can support custom cursor
                    IFictionEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_GAME_PATH + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_COVER_PATH + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_FORMAT + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_BAFN + INT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_AUTHOR + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_DESCRIPTION + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_HEADLINE + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_FIRSTPUBLISHED + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_GENRE + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_FORGIVENESS + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_GROUP + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_LANGUAGE + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_SERIES + TEXT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_SERIESNUM + INT_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_STARRATING + REAL_TYPE + COMMA_SEP +
                    IFictionEntry.COLUMN_NAME_RATINGCOUNTTOT + INT_TYPE +
                    " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + IFictionEntry.TABLE_NAME;
    private static final String[] PROJ_ID = {
            IFictionEntry.COLUMN_NAME_ENTRY_ID
    };
    private static final String[] PROJ_ID_PATH = {
            IFictionEntry.COLUMN_NAME_ENTRY_ID,
            IFictionEntry.COLUMN_NAME_GAME_PATH
    };
    // we assume column order is same as the SELECT statement implied by
    // the above projection
    private static final int ifid_col = 0;
    private static final int gamepath_col = 1;
    private static final int format_col = 2;
    private static final int bafn_col = 3;
    private static final int title_col = 4;
    private static final int author_col = 5;
    private static final int descr_col = 6;
    private static final int headline_col = 7;
    private static final int firstpub_col = 8;
    private static final int genre_col = 9;
    private static final int forgiveness_col = 10;
    private static final int group_col = 11;
    private static final int lang_col = 12;
    private static final int series_col = 13;
    private static final int seriesnum_col = 14;
    private static final int starrating_col = 15;
    private static final int ratingcounttot_col = 16;

    public IFictionDbHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static void addGameMetaDataManually(@NonNull File file, @Nullable Fragment fr) {
        if (fr != null) {
            String name = file.getName();
            String[] split_path = name.split("\\.");

            final IFictionRecord ifr = new IFictionRecord();
            ifr.file_path = file.getPath();
            ifr.title = split_path[0];

            EditIFictionDialogFragment d = new EditIFictionDialogFragment();
            d.setIFictionRecord(ifr);
            d.setTargetFragment(fr, 0);
            d.show(fr.getActivity().getSupportFragmentManager(), "EditMetadata");
        }
    }

    public static void addGameMetaData(@NonNull Context c, @NonNull File file, @NonNull IFictionDbHelper dbHelper,
                                       @NonNull ByteBuffer bbTemp, @Nullable Fragment fr) {
        // if fr is not null, we prompt user for unrecognised formats
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                addGameMetaData(c, child, dbHelper, bbTemp, fr);
            }
        } else {
            FabLogger.debug("Scanning '" + file.getPath() + "' ...");

            String name = file.getName();
            String[] split_path = name.split("\\.");
            String ext = (split_path.length > 1) ? split_path[1].toLowerCase() : "";

            final IFictionRecord ifr = new IFictionRecord();
            ifr.file_path = file.getPath();
            ifr.title = split_path[0];

            // FIXME:
            // if (ext.toLowerCase().equals("da1")) {
            // Looks like we've got an AGT package,
            // try to convert to AGX if this hasn't been done already
            //     String path2 = ifr.file_path.substring(0, ifr.file_path.length() - ext.length() - 1);
            //     File agxFile = new File(path2 + ".agx");
            //     if (!agxFile.exists()) {
            //         Logger.debug("\tConverting " + path2 + " to AGX and skipping.");
            //         if (RunProgramService.agt2agx(path2)) {
            //             continue;
            //         }
            //     }
            // }

            // Initialise Babel library with this game
            // (note this call also sets the game format)
            if (!IFictionRecord.startOps(ifr)) {
                FabLogger.error("  => Could not initialise Babel library (try adding manually?)");
                IFictionRecord.stopOps();
                return;
            }

            // Try to work out whether this is a valid game or not.
            // If it doesn't seem to be valid, then don't add it to the database.
            FabLogger.debug("  => " + ifr.format + (ifr.isFormatAuthoritative ? " (authoritative)" : ""));
            if (!IFictionRecord.isPlayableStory(ifr, ext)) {
                FabLogger.error("  => Appears this is not a playable story (if you disagree, try adding it manually).");
                IFictionRecord.stopOps();
                return;
            }

            // Load IFID information.
            if (!IFictionRecord.opLoadIFIDsFromGameFile(ifr, bbTemp)) {
                FabLogger.error("  => Babel could not load / generate game file IFIDs.");
                IFictionRecord.stopOps();
                return;
            }

            // Create the game data directory for this game
            String gameDir;
            try {
                gameDir = GLKConstants.getDir(c, GLKConstants.SUBDIR.GAMEDATA) + "/" + ifr.ifids[0] + "/";
            } catch (@NonNull IOException | SecurityException e) {
                FabLogger.error("  => Could not create game data directory for " + ifr.ifids[0] +
                        ". Please check you have granted file read/write permissions and try again. If you have overridden the default paths in your settings, also check that Fabularium has read/write access to that path.");
                IFictionRecord.stopOps();
                return;
            }

            // Attempt to load metadata/cover image from the database trying each of the IFIDs
            // until one works. If none of the IFIDs are already in the database, try to load the
            // metadata/cover image from the underlying file instead.
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            boolean metadataInDatabase = get(ifr, db);
            if (!metadataInDatabase) {
                IFictionRecord.opLoadMetadataFromFile(ifr, bbTemp);
            }

            // Is the cover already saved to disk?  If not, attempt to extract it from the file.
            File f = new File(gameDir + "cover.png");
            if (!f.exists()) {
                IFictionRecord.opSaveCoverFromGameFile(c, ifr, bbTemp);
            }
            IFictionRecord.stopOps();

            // If we've got to this point, we know we have a valid game (format is
            // recognised and not null), so just ensure that the basic fields are set
            // and then add it to the list.
            if (!metadataInDatabase) {
                // Save it back into the database
                put(ifr, db);
                FabLogger.debug("  => Appears to be a valid game - saved metadata back to database!");
                if (fr != null) {
                    Toast.makeText(fr.getContext(), "Saved metadata (babel says game format is '" + ifr.format + "')", Toast.LENGTH_SHORT).show();
                }
            }
            db.close();
        }
    }

    /**
     * Sets the given IFictionRecord to match the record in the database
     * with the same IFID, if it exists.
     *
     * @param ifid - the ID to lookup.
     * @return null if IFID doesn't exist (or another error).
     */
    @Nullable
    static IFictionRecord get(@NonNull String ifid, @NonNull SQLiteDatabase db) {
        IFictionRecord ifr = new IFictionRecord();
        ifr.ifids = new String[1];
        ifr.ifids[0] = ifid;
        if (!get(ifr, db)) {
            return null;
        }
        return ifr;
    }

    private static boolean get(@NonNull IFictionRecord ifr, @NonNull SQLiteDatabase db) {
        Cursor cursor = db.query(
                IFictionEntry.TABLE_NAME,
                PROJ_ALL,
                IFictionEntry.COLUMN_NAME_ENTRY_ID + " = \"" + ifr.ifids[0] + "\"",
                null,
                null,
                null,
                null
        );

        if (!cursor.moveToFirst()) {
            // cursor is empty
            cursor.close();
            return false;
        }

        // Copy the data across
        ifr.file_path = cursor.getString(gamepath_col);
        ifr.ifids = new String[1];
        ifr.ifids[0] = cursor.getString(ifid_col);
        ifr.format = cursor.getString(format_col);
        ifr.bafn = Integer.valueOf(cursor.getString(bafn_col));
        ifr.title = cursor.getString(title_col);
        ifr.author = cursor.getString(author_col);
        ifr.description = cursor.getString(descr_col);
        ifr.headline = cursor.getString(headline_col);
        ifr.firstpublished = cursor.getString(firstpub_col);
        ifr.genre = cursor.getString(genre_col);
        ifr.forgiveness = cursor.getString(forgiveness_col);
        ifr.group = cursor.getString(group_col);
        ifr.language = cursor.getString(lang_col);
        ifr.series = cursor.getString(series_col);
        ifr.seriesnumber = Integer.valueOf(cursor.getString(seriesnum_col));
        ifr.starrating = Float.valueOf(cursor.getString(starrating_col));
        ifr.ratingcounttot = Integer.valueOf(cursor.getString(ratingcounttot_col));

        cursor.close();
        return true;
    }

    static boolean put(@NonNull IFictionRecord ifr, @NonNull SQLiteDatabase db) {
        Cursor cursor = db.query(
                IFictionEntry.TABLE_NAME,
                PROJ_ID,
                IFictionEntry.COLUMN_NAME_ENTRY_ID + " = \"" + ifr.ifids[0] + "\"",
                null,
                null,
                null,
                null
        );

        if (!cursor.moveToFirst()) {
            // cursor is empty - insert into database rather than update
            cursor.close();
            long newRowID = db.insert(
                    IFictionEntry.TABLE_NAME,
                    IFictionEntry.COLUMN_NAME_NULLABLE,
                    getContentForDB(ifr));
            return (newRowID != -1);
        }

        // it's already there - overwrite the existing record
        cursor.close();
        String selection = IFictionEntry.COLUMN_NAME_ENTRY_ID + " = ?";
        String[] selectionArgs = {ifr.ifids[0]};
        int count = db.update(
                IFictionEntry.TABLE_NAME,
                getContentForDB(ifr),
                selection,
                selectionArgs);
        return (count > 0);
    }

    @NonNull
    private static ContentValues getContentForDB(@NonNull IFictionRecord ifr) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(IFictionEntry.COLUMN_NAME_ENTRY_ID, ifr.ifids[0]);   // TODO: don't rely on first ifid only
        values.put(IFictionEntry.COLUMN_NAME_GAME_PATH, ifr.file_path);
        values.put(IFictionEntry.COLUMN_NAME_FORMAT, ifr.format);
        values.put(IFictionEntry.COLUMN_NAME_BAFN, ifr.bafn);
        values.put(IFictionEntry.COLUMN_NAME_TITLE, ifr.title);
        values.put(IFictionEntry.COLUMN_NAME_AUTHOR, ifr.author);
        values.put(IFictionEntry.COLUMN_NAME_DESCRIPTION, ifr.description);
        values.put(IFictionEntry.COLUMN_NAME_HEADLINE, ifr.headline);
        values.put(IFictionEntry.COLUMN_NAME_FIRSTPUBLISHED, ifr.firstpublished);
        values.put(IFictionEntry.COLUMN_NAME_GENRE, ifr.genre);
        values.put(IFictionEntry.COLUMN_NAME_FORGIVENESS, ifr.forgiveness);
        values.put(IFictionEntry.COLUMN_NAME_GROUP, ifr.group);
        values.put(IFictionEntry.COLUMN_NAME_LANGUAGE, ifr.language);
        values.put(IFictionEntry.COLUMN_NAME_SERIES, ifr.series);
        values.put(IFictionEntry.COLUMN_NAME_SERIESNUM, ifr.seriesnumber);
        values.put(IFictionEntry.COLUMN_NAME_STARRATING, ifr.starrating);
        values.put(IFictionEntry.COLUMN_NAME_RATINGCOUNTTOT, ifr.ratingcounttot);
        return values;
    }

    public boolean deleteGameMetaData(@NonNull String ifid) {
        // returns TRUE if game metadata successfully deleted, false otherwise
        SQLiteDatabase db = getWritableDatabase();
        int nRows = db.delete(IFictionEntry.TABLE_NAME, IFictionEntry.COLUMN_NAME_ENTRY_ID + " = '" + ifid + "'", null);
        db.close();
        return (nRows > 0);
    }

    public void refresh(@NonNull Context c, @NonNull File gameDir) {
        SQLiteDatabase db = getWritableDatabase();

        // 1) Go through each item in the database and check that its filename still corresponds
        // to a real file on the system. If not delete the database item.
        String path, ifid;
        int nRows;
        Cursor cursor = db.query(
                IFictionEntry.TABLE_NAME,
                PROJ_ID_PATH,
                null, null, null, null, null
        );

        try {
            while (cursor.moveToNext()) {
                ifid = cursor.getString(ifid_col);
                path = cursor.getString(gamepath_col);
                File f = new File(path);
                if (!f.exists()) {
                    FabLogger.error("removing " + path + " from database");
                    nRows = db.delete(IFictionEntry.TABLE_NAME, IFictionEntry.COLUMN_NAME_ENTRY_ID + " = '" + ifid + "'", null);
                    FabLogger.error(nRows + " deleted.");
                }
            }
        } finally {
            cursor.close();
        }
        db.close();

        // 2) Go through valid game files in the game directory and its sub-folders, and
        // ensure for each that metadata is stored in the database
        addGameMetaData(c, gameDir, this, ByteBuffer.allocateDirect(2000000), null);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /* Inner class that defines the table contents */
    static abstract class IFictionEntry implements BaseColumns {
        static final String TABLE_NAME = "ifmetadata";
        static final String COLUMN_NAME_ENTRY_ID = "ifid";
        static final String COLUMN_NAME_GAME_PATH = "gamepath";
        static final String COLUMN_NAME_COVER_PATH = "coverpath";
        static final String COLUMN_NAME_FORMAT = "format";
        static final String COLUMN_NAME_BAFN = "bafn";
        static final String COLUMN_NAME_TITLE = "title";
        static final String COLUMN_NAME_AUTHOR = "author";
        static final String COLUMN_NAME_DESCRIPTION = "description";
        static final String COLUMN_NAME_HEADLINE = "headline";
        static final String COLUMN_NAME_FIRSTPUBLISHED = "firstpublished";
        static final String COLUMN_NAME_GENRE = "genre";
        static final String COLUMN_NAME_FORGIVENESS = "forgiveness";
        static final String COLUMN_NAME_GROUP = "igroup";
        static final String COLUMN_NAME_LANGUAGE = "language";
        static final String COLUMN_NAME_SERIES = "series";
        static final String COLUMN_NAME_SERIESNUM = "seriesnum";
        static final String COLUMN_NAME_STARRATING = "starrating";
        static final String COLUMN_NAME_RATINGCOUNTTOT = "ratingcounttot";
        static final String COLUMN_NAME_NULLABLE = "null";
    }
}