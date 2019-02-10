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
package com.luxlunae.fabularium.create;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.fabularium.AsyncProgressFragment;
import com.luxlunae.fabularium.FabLogger;
import com.luxlunae.fabularium.R;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The ProjectDbHelper provides a single interface for querying and updating project details
 * in the Fabularium database on the user's Android device. That database is accessible by name to any
 * class in the application, but not outside the application.
 * <p>
 * At some point, we may update this design pattern to follow the more recent Google recommendation,
 * which is to use the Room persistence library (see https://developer.android.com/guide/topics/data/data-storage.html#db)
 * Of course we would need to do that in a manner that doesn't cause too much inconvenience for existing
 * users.
 */
public class ProjectDbHelper extends SQLiteOpenHelper implements AsyncProgressFragment.RefreshDbHelper {
    public static final String SOURCE_FILE_DELIMITER = "\n\r\f";    // hopefully no-one will use this exact combo in their file paths!!
    static final String[] PROJ_SUMMARY = new String[]{
            ProjectEntry._ID,
            ProjectEntry.COLUMN_NAME_ENTRY_ID,
            ProjectEntry.COLUMN_NAME_TITLE,
            ProjectEntry.COLUMN_NAME_COMPILER
    };
    static final String[] PROJ_ALL = {
            ProjectEntry.COLUMN_NAME_ENTRY_ID,
            ProjectEntry.COLUMN_NAME_TITLE,
            ProjectEntry.COLUMN_NAME_PROJECT_DIR,
            ProjectEntry.COLUMN_NAME_COMPILER,
            ProjectEntry.COLUMN_NAME_COMPILER_ARGS,
            ProjectEntry.COLUMN_NAME_SOURCE_FILES,
            ProjectEntry.COLUMN_NAME_OUTPUT_FILE,
            ProjectEntry.COLUMN_NAME_OUTPUT_FORMAT
    };
    private static final int PROJECT_TYPE_UNKNOWN = 0;
    private static final int PROJECT_TYPE_INFORM = 1;
    private static final int PROJECT_TYPE_TADS3 = 2;

    // If you change the database schema, you must increment the database version.
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "ProjectMetadata.db";
    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + ProjectEntry.TABLE_NAME + " ( " +
                    ProjectEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +  // we need to do this so we can support custom cursor
                    ProjectEntry.COLUMN_NAME_ENTRY_ID + TEXT_TYPE + COMMA_SEP +
                    ProjectEntry.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA_SEP +
                    ProjectEntry.COLUMN_NAME_PROJECT_DIR + TEXT_TYPE + COMMA_SEP +
                    ProjectEntry.COLUMN_NAME_COMPILER + TEXT_TYPE + COMMA_SEP +
                    ProjectEntry.COLUMN_NAME_COMPILER_ARGS + TEXT_TYPE + COMMA_SEP +
                    ProjectEntry.COLUMN_NAME_SOURCE_FILES + TEXT_TYPE + COMMA_SEP +
                    ProjectEntry.COLUMN_NAME_OUTPUT_FILE + TEXT_TYPE + COMMA_SEP +
                    ProjectEntry.COLUMN_NAME_OUTPUT_FORMAT + TEXT_TYPE + " )";
    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + ProjectEntry.TABLE_NAME;
    private static final String[] PROJ_ID = {
            ProjectEntry.COLUMN_NAME_ENTRY_ID
    };
    private static final String[] PROJ_ID_PATH = {
            ProjectEntry.COLUMN_NAME_ENTRY_ID,
            ProjectEntry.COLUMN_NAME_TITLE,
            ProjectEntry.COLUMN_NAME_PROJECT_DIR
    };

    // We assume column order is same as the SELECT statement implied by
    // the above projection.
    private static final int projid_col = 0;
    private static final int title_col = 1;
    private static final int projdir_col = 2;
    private static final int compiler_col = 3;
    private static final int compilerargs_col = 4;
    private static final int sourcefiles_col = 5;
    private static final int outputfile_col = 6;
    private static final int outputformat_col = 7;

    ProjectDbHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    private static int getProjectType(@NonNull File file) {
        // Try to guess what type of project this is from the provided file
        // Currently we only look at the extension
        String name = file.getName();
        String[] split_path = name.split("\\.");
        String ext = (split_path.length > 1) ? split_path[1].toLowerCase() : "";
        switch (ext) {
            case "inf":
                return PROJECT_TYPE_INFORM;
            case "t":
                return PROJECT_TYPE_TADS3;
            default:
                return PROJECT_TYPE_UNKNOWN;
        }
    }

    private static int getSourceFiles(@NonNull File file, @NonNull StringBuilder sb, int projType) {
        // if file is a file and has extension 'ext' add it as a source file
        // to stringbuilder sb; if it is a directory then scan files and subdirectories
        // of that directory. Returns project type associated with the extension of
        // the first valid source file it meets.
        if (file.isDirectory()) {
            try {
                File[] children = file.listFiles();
                if (children != null) {
                    for (File child : children) {
                        projType = getSourceFiles(child, sb, projType);
                    }
                }
            } catch (SecurityException e) {
                // If a security manager exists and its checkRead(String) method denies read access to the directory
                FabLogger.error("Cannot list contents of '" + file.getAbsolutePath() + "" +
                        "' as according to Android, your security manager has denied read access to this directory.");
            }
            return projType;
        } else {
            FabLogger.debug("Scanning '" + file.getPath() + "' ...");

            int fileType = getProjectType(file);
            switch (fileType) {
                case PROJECT_TYPE_INFORM:
                    if (projType == PROJECT_TYPE_INFORM || projType == PROJECT_TYPE_UNKNOWN) {
                        projType = PROJECT_TYPE_INFORM;
                    } else {
                        return projType;
                    }
                    break;
                case PROJECT_TYPE_TADS3:
                    if (projType == PROJECT_TYPE_TADS3 || projType == PROJECT_TYPE_UNKNOWN) {
                        projType = PROJECT_TYPE_TADS3;
                    } else {
                        return projType;
                    }
                    break;
                default:
                    return projType;
            }

            if (sb.length() > 0) {
                sb.append(SOURCE_FILE_DELIMITER);
            }
            sb.append(file.getAbsolutePath());

            return projType;
        }
    }

    @NonNull
    private static ContentValues getContentValues(@NonNull ProjectRecord pr) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(ProjectEntry.COLUMN_NAME_ENTRY_ID, pr.id);
        values.put(ProjectEntry.COLUMN_NAME_TITLE, pr.title);
        values.put(ProjectEntry.COLUMN_NAME_PROJECT_DIR, pr.dir);
        values.put(ProjectEntry.COLUMN_NAME_COMPILER, pr.compiler);
        values.put(ProjectEntry.COLUMN_NAME_COMPILER_ARGS, pr.compilerArgs);
        values.put(ProjectEntry.COLUMN_NAME_SOURCE_FILES, pr.sourceFiles);
        values.put(ProjectEntry.COLUMN_NAME_OUTPUT_FILE, pr.outputFile);
        values.put(ProjectEntry.COLUMN_NAME_OUTPUT_FORMAT, pr.outputFormat);
        return values;
    }

    static boolean put(@NonNull ProjectRecord pr, @NonNull SQLiteDatabase db) {
        Cursor cursor = db.query(
                ProjectEntry.TABLE_NAME,
                PROJ_ID,
                ProjectEntry.COLUMN_NAME_ENTRY_ID + " = \"" + pr.id + "\"",
                null,
                null,
                null,
                null
        );

        if (!cursor.moveToFirst()) {
            // cursor is empty - insert into database rather than update
            cursor.close();
            long newRowID = db.insert(
                    ProjectEntry.TABLE_NAME,
                    ProjectEntry.COLUMN_NAME_NULLABLE,
                    getContentValues(pr));
            return (newRowID == -1);
        }

        // it's already there - overwrite the existing record
        cursor.close();
        String selection = ProjectEntry.COLUMN_NAME_ENTRY_ID + " = ?";
        String[] selectionArgs = {pr.id};
        int count = db.update(
                ProjectEntry.TABLE_NAME,
                getContentValues(pr),
                selection,
                selectionArgs);
        return (count > 0);
    }

    private static void importProject(@NonNull Context c, @NonNull File file, @NonNull ProjectDbHelper dbHelper) {
        // create a new project in the database that includes all the relevant source files
        // in directory 'file'
        StringBuilder sourceFiles = new StringBuilder();
        String defOutputPath;
        String outDir;
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ProjectRecord pr = new ProjectRecord();
        pr.id = file.getAbsolutePath();

        int projType;
        if (!get(pr, db)) {
            // Check if we should add this project to the database
            if (file.isDirectory()) {
                projType = getSourceFiles(file, sourceFiles, PROJECT_TYPE_UNKNOWN);
                defOutputPath = file.getAbsolutePath() + "/" + file.getName();
                outDir = file.getAbsolutePath();
            } else {
                sourceFiles.append(file.getAbsolutePath());
                projType = getProjectType(file);
                outDir = file.getParent();

                String[] split = file.getName().split("\\.");
                String ext = split.length > 1 ? split[1] : "";
                String path = file.getAbsolutePath();
                defOutputPath = path.substring(0, path.length() - ext.length() - 1);
            }

            if (sourceFiles.length() > 0) {
                // Set default compiler name and arguments based on auto-detected project type
                // N.B. User will be able to modify these settings at any time by long clicking
                // project in the project selector view
                pr.title = file.getName();
                pr.dir = file.getAbsolutePath();
                pr.sourceFiles = sourceFiles.toString();

                switch (projType) {
                    case PROJECT_TYPE_INFORM:
                        pr.compiler = "inform";
                        pr.compilerArgs = "+code_path=" + outDir + " " + sourceFiles;
                        pr.outputFile = defOutputPath + ".z5";
                        pr.outputFormat = "zcode";
                        break;
                    case PROJECT_TYPE_TADS3:
                        String t3mPath = defOutputPath.concat(".t3m");
                        pr.compiler = "t3make";
                        pr.compilerArgs = "-f ".concat(t3mPath);
                        pr.outputFile = defOutputPath + ".t3";
                        pr.outputFormat = "tads3";
                        ensureT3MFileExists(c, t3mPath, pr.dir, sourceFiles, pr.outputFile);
                        break;
                    default:
                        // Didn't recognise any source files, don't add the project
                        FabLogger.error("importProject: did not recognise project type for directory " + file.getAbsolutePath());
                        db.close();
                        return;
                }
                put(pr, db);
            }
        }
        db.close();
    }

    private static void ensureT3MFileExists(@NonNull Context c, @NonNull String t3mPath,
                                            @NonNull String projPath, @NonNull StringBuilder sourceFiles, @NonNull String outputFile) {
        File t3m = new File(t3mPath);
        if (!t3m.exists()) {
            File objDir = new File(projPath + "/obj/");
            if (!objDir.exists()) {
                if (!objDir.mkdirs()) {
                    FabLogger.error("Couldn't create an object directory at '" + objDir + "'.");
                    return;
                }
            }
            String[] sources = sourceFiles.toString().split(SOURCE_FILE_DELIMITER);
            File f;
            try {
                FileWriter t3mWriter = new FileWriter(t3m);
                t3mWriter.append(c.getResources().getString(R.string.t3m_header));
                t3mWriter.append("\n\n");
                f = new File(outputFile);
                t3mWriter.append("-o ".concat(f.getName()).concat("\n"));
                t3mWriter.append("-pre\n-nodef\n-D LANGUAGE=en_us\n-D MESSAGESTYLE=neu\n-D INSTRUCTIONS_MENU\n-Fy obj\n-Fo obj\n-w1\n-we\n\n");
                t3mWriter.append("##sources\n");
                t3mWriter.append("-lib system\n-lib adv3/adv3\n");
                for (String source : sources) {
                    f = new File(source);
                    t3mWriter.append("-source ".concat(f.getName()).concat("\n"));
                }
                t3mWriter.flush();
                t3mWriter.close();
            } catch (IOException e) {
                FabLogger.error("Couldn't create new t3m file at " + t3mPath + ": " + e.getMessage());
            }
        }
    }

    /**
     * Sets the given ProjectRecord to match the record in the database
     * with the same id, if it exists.
     *
     * @param id - the ID to lookup.
     * @return null if id doesn't exist (or another error).
     */
    @Nullable
    static ProjectRecord get(@NonNull String id, @NonNull SQLiteDatabase db) {
        ProjectRecord pr = new ProjectRecord();
        pr.id = id;
        boolean ret = get(pr, db);
        return ret ? pr : null;
    }

    private static boolean get(@NonNull ProjectRecord pr, @NonNull SQLiteDatabase db) {
        Cursor cursor = db.query(
                ProjectEntry.TABLE_NAME,
                PROJ_ALL,
                ProjectEntry.COLUMN_NAME_ENTRY_ID + " = \"" + pr.id + "\"",
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
        pr.id = cursor.getString(projid_col);
        pr.title = cursor.getString(title_col);
        pr.dir = cursor.getString(projdir_col);
        pr.compiler = cursor.getString(compiler_col);
        pr.compilerArgs = cursor.getString(compilerargs_col);
        pr.sourceFiles = cursor.getString(sourcefiles_col);
        pr.outputFile = cursor.getString(outputfile_col);
        pr.outputFormat = cursor.getString(outputformat_col);

        cursor.close();
        return true;
    }

    public void refresh(@NonNull Context c, @NonNull File projectDir) {
        SQLiteDatabase db = getWritableDatabase();

        // 1) Go through each item in the database and check that its path still corresponds
        // to a real directory on the system. If not delete the database item.
        String path;
        String projid;
        int nRows;
        Cursor cursor = db.query(
                ProjectEntry.TABLE_NAME,
                PROJ_ID_PATH,
                null, null, null, null, null
        );

        try {
            while (cursor.moveToNext()) {
                projid = cursor.getString(projid_col);
                path = cursor.getString(projdir_col);
                File f = new File(path);
                if (!f.exists()) {
                    FabLogger.error("removing " + path + " from database");
                    nRows = db.delete(ProjectEntry.TABLE_NAME, ProjectEntry.COLUMN_NAME_ENTRY_ID + " = '" + projid + "'", null);
                    FabLogger.error(nRows + " deleted.");
                }
            }
        } finally {
            cursor.close();
        }
        db.close();

        // 2) Go through valid source files in the project directory and its sub-folders, and
        // ensure for each that metadata is stored in the database.
        // We assume that:
        //    1) any valid source file in the top level project directory is a single-file project in
        //       its own right;
        //    2) any subfolder of the top level project directory represents a multi-file project (as per
        //       the files stored in it and its subfolders, if any)
        for (File child : projectDir.listFiles()) {
            importProject(c, child, this);
        }
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO: is this the most appropriate behaviour?
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    static class ProjectRecord {
        public String id;
        public String title;
        public String dir;
        public String compiler;
        String compilerArgs;
        String sourceFiles;
        String outputFile;
        String outputFormat;
    }

    /* Inner class that defines the table contents */
    static abstract class ProjectEntry implements BaseColumns {
        static final String TABLE_NAME = "projmetadata";
        static final String COLUMN_NAME_ENTRY_ID = "projid";
        static final String COLUMN_NAME_TITLE = "title";             // name of project
        static final String COLUMN_NAME_PROJECT_DIR = "dir";         // project directory
        static final String COLUMN_NAME_COMPILER = "compiler";           // name of compiler
        static final String COLUMN_NAME_COMPILER_ARGS = "compilerargs";  // arguments for compiler
        static final String COLUMN_NAME_SOURCE_FILES = "sourcefiles";    // space delimited string with relative paths to source files
        static final String COLUMN_NAME_OUTPUT_FILE = "outputfile";     // the path to the compiled output file (game)
        static final String COLUMN_NAME_OUTPUT_FORMAT = "outputformat"; // the format of the output file - used to determine which terp to play it with
        static final String COLUMN_NAME_NULLABLE = "null";
    }
}