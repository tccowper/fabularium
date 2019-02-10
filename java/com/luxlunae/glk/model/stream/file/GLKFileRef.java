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
package com.luxlunae.glk.model.stream.file;

import android.support.annotation.NonNull;

import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.model.GLKModel;
import com.luxlunae.glk.model.stream.GLKStream;

import java.io.File;
import java.io.IOException;

public class GLKFileRef extends File implements GLKStream {
    private static final long serialVersionUID = 5496995245825997775L;
    private final boolean mIsTextMode;
    private int mRock;
    private int mStreamID;

    public GLKFileRef(@NonNull String filename, int usage, int rock) {
        super(filename);
        mRock = rock;
        mIsTextMode = ((usage & GLKConstants.fileusage_TextMode) != 0);
    }

    public GLKFileRef(int usage, int rock) throws IOException {
        super(File.createTempFile("glk", ".tmp").getAbsolutePath());
        mRock = rock;
        mIsTextMode = ((usage & GLKConstants.fileusage_TextMode) != 0);
        deleteOnExit();
    }

    /**
     * Given a usage flag and a base filename, constructs a path
     * consistent with the GLK spec.
     * <p/>
     * The recommended file suffixes depend on the usage: ".glkdata" for
     * fileusage_Data, ".glksave" for fileusage_SavedGame, ".txt" for
     * fileusage_Transcript and fileusage_InputRecord.
     *
     * @param filename - base filename
     * @return an absolute GLK recommended path name
     */
    @NonNull
    public static String getGLKPath(@NonNull GLKModel m, @NonNull String filename, int usage) {
        switch (usage & GLKConstants.fileusage_TypeMask) {
            case GLKConstants.fileusage_Data:
                return m.mGameDataPath + filename + GLKConstants.GLK_DATA_EXT;
            case GLKConstants.fileusage_SavedGame:
                return m.mGameDataPath + filename + GLKConstants.GLK_SAVE_EXT;
            case GLKConstants.fileusage_Transcript:
                return m.mGameDataPath + filename + GLKConstants.GLK_TRANSCRIPT_EXT;
            case GLKConstants.fileusage_InputRecord:
                return m.mGameDataPath + filename + GLKConstants.GLK_INPUT_RECORD_EXT;
        }

        return filename;
    }

    boolean isTextMode() {
        return mIsTextMode;
    }

    @Override
    public int getRock() {
        return mRock;
    }

    @Override
    public void setRock(int rock) {
        mRock = rock;
    }

    @Override
    public int getStreamId() {
        return mStreamID;
    }

    @Override
    public void setStreamId(int id) {
        mStreamID = id;
    }

    @Override
    public void close() {
        // do nothing
    }
}
