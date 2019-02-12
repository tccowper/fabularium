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

package com.luxlunae.fabularium.explore;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class ZipShortcut extends File {
    @NonNull
    private final String mZipFilePath;
    @Nullable
    private final File mParent;
    @NonNull
    private final HashMap<String, ZipEntry> mAllZipEntries;
    @Nullable
    private final ZipEntry mThisZipEntry;

    ZipShortcut(@Nullable File parent, @NonNull String child,
                @NonNull String zipFilePath, @NonNull HashMap<String, ZipEntry> zipEntries) {
        // if parent is NULL, this is the root entry
        super(parent, child);
        mZipFilePath = zipFilePath;
        mParent = parent;
        mAllZipEntries = zipEntries;
        mThisZipEntry = zipEntries.get(child);
    }

    ZipShortcut(@NonNull ZipShortcut parent, @NonNull String child) {
        // if parent is NULL, this is the root entry
        super(parent, child);
        mParent = parent;
        mZipFilePath = parent.mZipFilePath;
        mAllZipEntries = parent.mAllZipEntries;
        mThisZipEntry = mAllZipEntries.get(child);
    }

    @Override
    public boolean isDirectory() {
        return mThisZipEntry == null || mThisZipEntry.isDirectory();
    }

    @Override
    public boolean isFile() {
        return mThisZipEntry != null && !mThisZipEntry.isDirectory();
    }

    @Override
    public File getParentFile() {
        if (mParent == null) {
            File zf = new File(mZipFilePath);
            return zf.getParentFile();
        }
        return mParent;
    }

    @Override
    public boolean canRead() {
        return true;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @NonNull
    File extractToTempFile(@NonNull Activity act, boolean useTmpExtension) throws IOException {
        if (isDirectory() || mThisZipEntry == null) {
            throw new IOException("Can't extract directory or non-existent file entry.");
        }
        File outputDir = act.getCacheDir();
        String name = getName();
        int pos = name.indexOf('.');
        String prefix;
        String ext;
        if (pos > 0) {
            prefix = name.substring(0, pos);
            ext = name.substring(pos);
        } else {
            prefix = name;
            ext = "";
        }
        ZipFile zf = new ZipFile(mZipFilePath);
        InputStream in = zf.getInputStream(mThisZipEntry);
        File outFile = File.createTempFile(prefix, useTmpExtension ? "" : ext, outputDir);
        OutputStream out = new FileOutputStream(outFile);
        byte[] buf = new byte[4096];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        out.close();
        in.close();
        return outFile;
    }

    @Override
    public File[] listFiles() {
        // Returns null if this abstract pathname does not
        // denote a directory, or if an I/O error occurs.
        if (!isDirectory()) {
            return null;
        }

        String fullPath = getAbsolutePath();
        ArrayList<File> list = new ArrayList<>();
        for (ZipEntry ze : mAllZipEntries.values()) {
            String zePath = "/" + ze.getName();
            File tmp = new File(zePath);
            if (tmp.getParent().equals(fullPath)) {
                list.add(new ZipShortcut(this, ze.getName(), mZipFilePath, mAllZipEntries));
            }
        }

        return list.toArray(new File[0]);
    }

    @Override
    public long length() {
        return (mThisZipEntry != null) ? mThisZipEntry.getSize() : 0;
    }

    @Override
    public long lastModified() {
        if (mThisZipEntry == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return mThisZipEntry.getLastModifiedTime().toMillis();
        } else {
            // Seems not to be supported on Android versions earlier
            // than O - return 0.
            return 0;
        }
    }
}
