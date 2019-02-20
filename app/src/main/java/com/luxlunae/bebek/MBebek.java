/*
 * Copyright (C) 2019 Tim Cadogan-Cowper.
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

package com.luxlunae.bebek;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.bebek.controller.MController;
import com.luxlunae.bebek.model.MAdventure;
import com.luxlunae.bebek.model.io.MBlorb;
import com.luxlunae.bebek.view.MView;
import com.luxlunae.fabularium.R;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.model.GLKModel;

import java.io.IOException;
import java.io.RandomAccessFile;

import static com.luxlunae.glk.GLKConstants.SUBDIR.LIB_ADRIFT;
import static com.luxlunae.glk.GLKConstants.getDir;

public final class MBebek {
    @Nullable
    private static MAdventure mAdv;

    public static int runTerp(@NonNull GLKModel m, @NonNull String[] args) {
        // 1. Create the view
        MView v = new MView(m);
        Context context = m.getApplicationContext();

        v.outImmediate(context.getString(R.string.BEBEK_LOADING_MSG));

        try {
            // 2. Create and initialise the model
            mAdv = new MAdventure(v);
            mAdv.setLibAdriftPath(getDir(context, LIB_ADRIFT));
            if (!mAdv.open(args[1])) {
                v.quit();
            }

            // 3. Create and run the controller
            // (For terps, we don't need to create a separate thread)
            MController c = new MController(mAdv, v);
            c.start();
        } catch (InterruptedException e) {
            return 2;
        } catch (IOException e) {
            return 1;
        }

        return 1;
    }

    public static byte[] getImage(@NonNull String imgPath) {
        if (mAdv == null) {
            return null;
        }

        if (mAdv.mVersion >= 4 && mAdv.mVersion < 5) {
            // Load directly from the TAF
            return getV4Media(imgPath);
        } else if (mAdv.mVersion >= 5) {
            // Load from a Blorb
            Integer resNum = mAdv.mBlorbMappings.get(imgPath);
            if (resNum != null && resNum > 0) {
                return MBlorb.getImage(mAdv, resNum);
            }
        }

        GLKLogger.error("TODO: Load ADRIFT image from file system: " + imgPath);
        return null;
    }

    public static byte[] getSound(@NonNull String sndPath) {
        if (mAdv == null) {
            return null;
        }

        if (mAdv.mVersion >= 4 && mAdv.mVersion < 5) {
            // Load directly from the TAF
            return getV4Media(sndPath);
        } else if (mAdv.mVersion >= 5) {
            // Load from a Blorb
            Integer resNum = mAdv.mBlorbMappings.get(sndPath);
            if (resNum != null && resNum > 0) {
                return MBlorb.getSound(mAdv, resNum);
            }
        }

        GLKLogger.error("TODO: Load ADRIFT sound from file system: " + sndPath);
        return null;
    }

    @Nullable
    private static byte[] getV4Media(@NonNull String path) {
        assert mAdv != null;

        try {
            MAdventure.v4Media media = mAdv.mV4Media.get(path);
            if (media != null) {
                RandomAccessFile advFile =
                        new RandomAccessFile(mAdv.getFullPath(), "r");
                advFile.seek(media.mOffset);
                byte[] data = new byte[media.mLength];
                advFile.read(data, 0, media.mLength);
                advFile.close();
                return data;
            }
        } catch (IOException e) {
            mAdv.mView.errMsg("File " + path + " not found in index.");
        }
        return null;
    }
}
