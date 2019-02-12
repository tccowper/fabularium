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

package com.luxlunae.fabularium;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.luxlunae.fabularium.play.IFictionDbHelper;
import com.luxlunae.fabularium.play.IFictionRecord;
import com.luxlunae.glk.GLKConstants;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MOVED_TEMP;
import static java.net.HttpURLConnection.HTTP_SEE_OTHER;

// Thanks https://androidresearch.wordpress.com/2013/05/10/dealing-with-asynctask-and-screen-orientation/
public class AsyncProgressFragment extends Fragment {

    private ProgressDialog mProgressDialog;
    private String mProgressMessage;
    private boolean mIsTaskRunning = false;
    private AsyncTask mTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void loadTextFile(@NonNull Resources res, @NonNull TextEditorView view, @NonNull File textFile) {
        if (!mIsTaskRunning) {
            mTask = new FileReaderTask(this, view);
            mProgressMessage = res.getString(R.string.progress_spinner_reading_files);
            ((FileReaderTask) mTask).execute(textFile);
        }
    }

    public void copyFiles(@NonNull Resources res, @NonNull String tag, @NonNull CopyFilesCallback callback, @NonNull File[] files) {
        // files should have an even number of elements with pattern (src, dst, src, dst, etc.)
        if (!mIsTaskRunning) {
            mTask = new CopyFilesTask(this, tag, callback);
            mProgressMessage = res.getString(R.string.progress_spinner_copying_files);
            ((CopyFilesTask) mTask).execute(files);
        }
    }

    public void copyAssets(@NonNull Resources res, @NonNull CopyAssetsCallback callback, @NonNull AssetManager assetMgr, @NonNull String[] paths) {
        // paths should have an even number of elements with pattern (srcAsset, dst, srcAsset, dst, etc.)
        if (!mIsTaskRunning) {
            mTask = new CopyAssetsTask(this, callback, assetMgr);
            mProgressMessage = res.getString(R.string.progress_spinner_copying_assets);
            ((CopyAssetsTask) mTask).execute(paths);
        }
    }

    public void renameFiles(@NonNull Resources res, @NonNull RenameFilesCallback callback, @NonNull File[] files) {
        // files should have an even number of elements with pattern (src, dst, src, dst, etc.)
        if (!mIsTaskRunning) {
            mTask = new RenameFilesTask(this, callback);
            mProgressMessage = res.getString(R.string.progress_spinner_renaming_files);
            ((RenameFilesTask) mTask).execute(files);
        }
    }

    public void deleteFiles(@NonNull Resources res, @NonNull DeleteFilesCallback callback, @NonNull File[] files) {
        if (!mIsTaskRunning) {
            mTask = new DeleteFilesTask(this, callback);
            mProgressMessage = res.getString(R.string.progress_spinner_deleting_files);
            ((DeleteFilesTask) mTask).execute(files);
        }
    }

    public void unzipFiles(@NonNull Resources res, @NonNull UnzipFilesCallback callback, @NonNull File[] files) {
        // files should have an even number of elements with pattern (src, dst, src, dst, etc.)
        if (!mIsTaskRunning) {
            mTask = new UnzipFilesTask(this, callback);
            mProgressMessage = res.getString(R.string.progress_spinner_unzipping_files);
            ((UnzipFilesTask) mTask).execute(files);
        }
    }

    public void downloadMetadata(@NonNull Resources res, @NonNull DownloadMetadataCallback callback, @NonNull Context c, @NonNull String serverUrl, @NonNull IFictionRecord[] ifrs) {
        if (!mIsTaskRunning) {
            mTask = new DownloadMetadataTask(this, serverUrl, callback, c);
            mProgressMessage = res.getString(R.string.progress_spinner_downloading_metadata);
            ((DownloadMetadataTask) mTask).execute(ifrs);
        }
    }

    public void refreshDatabase(@NonNull Resources res, @NonNull Context c, @NonNull RefreshDbHelper dbHelper,
                                @NonNull File syncDir, @NonNull RefreshDbCallbacks callbacks) {
        if (!mIsTaskRunning) {
            mTask = new RefreshDatabaseTask(this, c, dbHelper, callbacks);
            mProgressMessage = res.getString(dbHelper instanceof IFictionDbHelper ?
                    R.string.progress_spinner_refresh_gamedb : R.string.progress_spinner_refresh_projdb);
            ((RefreshDatabaseTask) mTask).execute(syncDir);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // If we are returning here from a screen orientation
        // and the AsyncTask is still working, re-create and display the
        // progress dialog.
        if (mIsTaskRunning) {
            FragmentActivity act = getActivity();
            if (act != null) {
                mProgressDialog = ProgressDialog.show(act,
                        getString(R.string.progress_spinner_title), mProgressMessage);
            }
        }
    }

    public void onTaskStarted() {
        mIsTaskRunning = true;
        FragmentActivity act = getActivity();
        if (act != null) {
            mProgressDialog = ProgressDialog.show(act,
                    getString(R.string.progress_spinner_title), mProgressMessage);
        }
    }

    public void onTaskFinished() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        mIsTaskRunning = false;
    }

    @Override
    public void onDetach() {
        // All dialogs should be closed before leaving the activity in order to avoid
        // the: Activity has leaked window com.android.internal.policy... exception
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        super.onDetach();
    }

    public interface RefreshDbCallbacks {
        void onPostExecute();
    }

    public interface RefreshDbHelper {
        void refresh(Context c, File syncDir);
    }

    public interface DownloadMetadataCallback {
        void onDownloadMetadataPostExecute(@Nullable IFictionRecord[] ifr, @Nullable String lastErrorMsg);
    }

    public interface CopyFilesCallback {
        void onCopyFilesPostExecute(@NonNull String tag, @Nullable File[] results);
    }

    public interface CopyAssetsCallback {
        void onCopyAssetsPostExecute(@Nullable File[] results);
    }

    public interface RenameFilesCallback {
        void onRenameFilesPostExecute(@Nullable File[] results);
    }

    public interface DeleteFilesCallback {
        void onDeleteFilesPostExecute(int nDeleted);
    }

    public interface UnzipFilesCallback {
        void onUnzipFilesPostExecute(@Nullable File[] results);
    }

    /* Supported tasks follow */

    private static class FileReaderTask extends AsyncTask<File, Void, String> {
        @NonNull
        private final WeakReference<TextEditorView> tvRef;
        @NonNull
        private final AsyncProgressFragment mListener;

        FileReaderTask(@NonNull AsyncProgressFragment f, @NonNull TextEditorView tv) {
            // Use a WeakReference to ensure the view can be garbage collected
            tvRef = new WeakReference<>(tv);
            tv.setText("[Loading text.. please wait]");
            mListener = f;
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Nullable
        @Override
        protected String doInBackground(@NonNull File... files) {
            try {
                StringBuilder sb = new StringBuilder();
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(files[0])));
                    CharBuffer cb = CharBuffer.allocate(1024);
                    while (r.read(cb) != -1) {
                        cb.flip();
                        sb.append(cb);
                        cb.clear();
                    }
                    r.close();
                } catch (IOException e) {
                    FabLogger.error("Couldn't load file '" + files[0].getAbsolutePath() + "': " + e.getMessage());
                    return null;
                }
                return sb.toString();
            } catch (OutOfMemoryError e) {
                FabLogger.error("Couldn't load file '" + files[0].getAbsolutePath() + "' - out of memory!");
                return null;
            }
        }

        // Once complete, see if TextEditor is still around and load in the string.
        @Override
        protected void onPostExecute(@Nullable String text) {
            FabLogger.flush();
            if (isCancelled()) {
                text = null;
            }
            if (text != null) {
                final TextEditorView tv = tvRef.get();
                if (tv != null) {
                    // TODO: this next call can cause an ANR error for large files
                    // How to handle it? Short of implementing our own text view
                    // with better performance than the stock Android component,
                    // it's not clear...
                    tv.setText(text);
                }
            }
            mListener.onTaskFinished();
        }
    }

    private static class DeleteFilesTask extends AsyncTask<File, Integer, Integer> {
        @NonNull
        private final AsyncProgressFragment mListener;
        @NonNull
        private WeakReference<DeleteFilesCallback> mCallback;

        DeleteFilesTask(@NonNull AsyncProgressFragment f, @NonNull DeleteFilesCallback callback) {
            mCallback = new WeakReference<>(callback);
            mListener = f;
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Override
        protected Integer doInBackground(@NonNull File... files) {
            // Thanks http://stackoverflow.com/questions/4943629/how-to-delete-a-whole-folder-and-content
            int nDeleted = 0;

            for (File file : files) {
                if (file.isDirectory()) {
                    String[] children;
                    try {
                        children = file.list();
                    } catch (SecurityException e) {
                        // If a security manager exists and its checkRead(String) method denies read access to the directory
                        FabLogger.error("deleteFile: cannot delete '" + file.getAbsolutePath() + "" +
                                "' as according to Android, your security manager has denied read access to this directory.");
                        continue;
                    }

                    if (children == null) {
                        // Either this abstract pathname does not
                        // denote a directory, or if an I/O error occurred.
                        FabLogger.error("deleteFile: cannot access '" + file.getAbsolutePath() +
                                "' - according to Android either the pathname does not denote a directory, or an I.O error" +
                                "occurred. Skipping...");
                        continue;
                    }

                    for (String child : children) {
                        doInBackground(new File(file, child));
                    }
                }

                nDeleted += file.delete() ? 1 : 0;  // delete child file or empty directory
            }

            return nDeleted;
        }

        @Override
        protected void onPostExecute(Integer result) {
            FabLogger.flush();
            DeleteFilesCallback callback = mCallback.get();
            if (callback != null) {
                callback.onDeleteFilesPostExecute(result);
            }
            mListener.onTaskFinished();
        }
    }

    private static class UnzipFilesTask extends AsyncTask<File, File[], File[]> {
        @NonNull
        private final AsyncProgressFragment mListener;
        @NonNull
        private WeakReference<UnzipFilesCallback> mCallback;

        UnzipFilesTask(@NonNull AsyncProgressFragment f, @NonNull UnzipFilesCallback callback) {
            mCallback = new WeakReference<>(callback);
            mListener = f;
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Nullable
        @Override
        protected File[] doInBackground(@NonNull File... mappings) {
            // returns an array with one entry for each mapping, s.t.
            // if the unzip was successful the entry is the destination path; otherwise
            // the entry is NULL
            int sz = mappings.length;

            if (sz % 2 != 0) {
                // the mappings should be (srcZipFile, destDir), (srcZipFile, destDir), etc.
                return null;
            }

            File[] ret = new File[sz / 2];
            for (int i = 0, k = 0; i < sz; i += 2, k++) {
                File src = mappings[i];
                File dst = mappings[i + 1];

                if (!src.isDirectory()) {
                    ret[k] = null;             // assume failure

                    if (!dst.isDirectory() || !dst.exists()) {
                        // failed - try the next one
                        FabLogger.error("Cannot unzip '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' - the destination is either not a directory, or doesn't exist.");
                        continue;
                    }

                    int nEntriesRead = 0;

                    // First try to load entries as though the file is a ZIP
                    try {
                        ZipInputStream in = new ZipInputStream(new FileInputStream(src));
                        byte[] buf = new byte[4096];
                        int len;
                        ZipEntry ze;
                        while ((ze = in.getNextEntry()) != null) {
                            nEntriesRead++;
                            File outFile = new File(dst, ze.getName());

                            if (ze.isDirectory()) {
                                if (!outFile.exists() && !outFile.mkdirs()) {
                                    // failed - try the next one
                                    FabLogger.error("Cannot unzip '" + src.getAbsolutePath() + "' to '" + outFile.getAbsolutePath() +
                                            "' - I can't create the destination directory.");
                                    continue;
                                }
                            } else {
                                File outParent = outFile.getParentFile();

                                // create the destination directory, if it doesn't exist
                                if (outParent == null) {
                                    // failed - try the next one
                                    FabLogger.error("Cannot unzip '" + src.getAbsolutePath() + "' to '" + outFile.getAbsolutePath() +
                                            "' - cannot access the destination's parent directory.");
                                    continue;
                                }

                                if (!outParent.exists() && !outParent.mkdirs()) {
                                    // failed - try the next one
                                    FabLogger.error("Cannot unzip '" + src.getAbsolutePath() + "' to '" + outFile.getAbsolutePath() +
                                            "' - the destination's parent directory doesn't exist and I can't create it.");
                                    continue;
                                }

                                OutputStream out = new FileOutputStream(outFile);
                                while ((len = in.read(buf)) > 0) {
                                    out.write(buf, 0, len);
                                }
                                out.close();
                            }

                            in.closeEntry();
                        }
                        in.close();

                    } catch (IOException e1) {
                        // failed - try the next one
                        FabLogger.error("Cannot unzip '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' - IO error: " + e1.getMessage());
                        continue;
                    }

                    if (nEntriesRead == 0) {
                        // Nothing read - perhaps it is a GZIP?
                        // TODO
                    }

                    if (nEntriesRead > 0) {
                        // success
                        ret[k] = dst;
                    }
                }
            }

            return ret;
        }

        @Override
        protected void onPostExecute(@Nullable File[] result) {
            FabLogger.flush();
            UnzipFilesCallback callback = mCallback.get();
            if (callback != null) {
                callback.onUnzipFilesPostExecute(result);
            }
            mListener.onTaskFinished();
        }
    }

    private static class RenameFilesTask extends AsyncTask<File, File[], File[]> {
        @NonNull
        private final AsyncProgressFragment mListener;
        @NonNull
        private WeakReference<RenameFilesCallback> mCallback;

        RenameFilesTask(@NonNull AsyncProgressFragment f, @NonNull RenameFilesCallback callback) {
            mCallback = new WeakReference<>(callback);
            mListener = f;
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Nullable
        @Override
        protected File[] doInBackground(@NonNull File... mappings) {
            // returns an array with one entry for each mapping, s.t.
            // if the copy was successful the entry is the new destination file; otherwise
            // the entry is NULL
            int sz = mappings.length;

            if (sz % 2 != 0) {
                // the mappings should be (srcPath, destPath), (srcPath, destPath), etc.
                return null;
            }

            File[] ret = new File[sz / 2];
            for (int i = 0, k = 0; i < sz; i += 2, k++) {
                File src = mappings[i];
                File dst = mappings[i + 1];
                try {
                    ret[k] = src.renameTo(dst) ? dst : null;
                } catch (SecurityException e) {
                    // If a security manager exists and its checkRead(String) method denies read access to the directory
                    FabLogger.error("Cannot rename '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                            "' as according to Android, your security manager has denied write access to either the old or new pathnames.");
                    ret[k] = null;
                }
            }

            return ret;
        }

        @Override
        protected void onPostExecute(@Nullable File[] result) {
            FabLogger.flush();
            RenameFilesCallback callback = mCallback.get();
            if (callback != null) {
                callback.onRenameFilesPostExecute(result);
            }
            mListener.onTaskFinished();
        }
    }

    private static class CopyAssetsTask extends AsyncTask<String, File[], File[]> {
        @NonNull
        private final AsyncProgressFragment mListener;
        @NonNull
        private WeakReference<AssetManager> mAssetMgr;
        @NonNull
        private WeakReference<CopyAssetsCallback> mCallback;

        CopyAssetsTask(@NonNull AsyncProgressFragment f, @NonNull CopyAssetsCallback callback, @NonNull AssetManager mgr) {
            mCallback = new WeakReference<>(callback);
            mAssetMgr = new WeakReference<>(mgr);
            mListener = f;
        }

        private static boolean copyAssets(AssetManager assetManager, File src, @NonNull File dst) {
            String[] children;
            String srcPath = src.getAbsolutePath().substring(1);   // chop off the '/'
            try {
                FabLogger.debug("Listing assets in " + srcPath);
                children = assetManager.list(srcPath);
            } catch (@NonNull IOException | SecurityException e) {
                FabLogger.error("Could not read assets in folder '" + srcPath + "'");
                return false;
            }

            if (children == null) {
                return false;
            }

            if (children.length > 0) {
                // src is a directory
                boolean ret = true;
                for (String child : children) {
                    if (!copyAssets(assetManager, new File(src, child), new File(dst, child))) {
                        ret = false;
                    }
                }
                return ret;
            } else {
                // src is a file

                // create the destination directory, if it doesn't exist
                File parent = dst.getParentFile();
                if (parent == null) {
                    // failed - try the next one
                    FabLogger.error("Cannot copy asset '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                            "' - cannot access the destination's parent directory.");
                    return false;
                }

                if (!parent.exists() && !parent.mkdirs()) {
                    FabLogger.error("Cannot copy asset '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                            "' - the destination's parent directory doesn't exist and I can't create it.");
                    return false;
                }

                // copy the file
                try {
                    InputStream in = assetManager.open(srcPath);
                    OutputStream out = new FileOutputStream(dst.isDirectory() ?
                            new File(dst, src.getName()) : dst);
                    byte[] buf = new byte[4096];
                    int len;
                    try {
                        while ((len = in.read(buf)) > 0) {
                            out.write(buf, 0, len);
                        }
                    } catch (IOException e) {
                        // failed - try the next one
                        FabLogger.error("Cannot copy asset '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' - IO error: " + e.getMessage());
                        return false;
                    } finally {
                        in.close();
                        out.close();
                    }
                } catch (IOException e1) {
                    // failed - try the next one
                    FabLogger.error("Cannot copy asset '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                            "' - IO error: " + e1.getMessage());
                    return false;
                }

                // success
                return true;
            }
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Override
        @Nullable
        protected File[] doInBackground(@NonNull String... mappings) {
            // args should be pairs - (srcAssetPath, destStoragePath)
            // returns an array with one entry for each mapping, s.t.
            // if the copy was successful the entry is the new destination file/dir; otherwise
            // the entry is NULL
            AssetManager assetManager = mAssetMgr.get();
            if (assetManager == null) {
                FabLogger.error("CopyAssetsTask: asset manager is null!");
                return null;
            }

            int sz = mappings.length;

            if (sz % 2 != 0) {
                // the mappings should be (srcPath, destPath), (srcPath, destPath), etc.
                return null;
            }

            File[] ret = new File[sz / 2];
            for (int i = 0, k = 0; i < sz; i += 2, k++) {
                String srcAssetPath = mappings[i];
                String dstStoragePath = mappings[i + 1];

                File dst = new File(dstStoragePath);
                if (!dst.exists()) {
                    FabLogger.debug("The asset directory or file " + dst.getAbsolutePath() + " does not exist. Attempting to create it.");
                    if (dst.isDirectory()) {
                        if (!dst.mkdirs()) {
                            FabLogger.error("Could not create directory: " + dst.getAbsolutePath());
                            ret[k] = null;
                            continue;
                        }
                    }
                    FabLogger.debug("Copying asset '" + srcAssetPath + "' to '" + dstStoragePath + "'");
                    ret[k] = copyAssets(assetManager, new File(srcAssetPath), dst) ? dst : null;
                }
            }

            return ret;
        }

        @Override
        protected void onPostExecute(@Nullable File[] result) {
            FabLogger.flush();
            CopyAssetsCallback callback = mCallback.get();
            if (callback != null) {
                callback.onCopyAssetsPostExecute(result);
            }
            mListener.onTaskFinished();
        }
    }

    private static class DownloadMetadataTask extends AsyncTask<IFictionRecord, Void, IFictionRecord[]> {
        @NonNull
        private final String mServerUrl;
        @NonNull
        private final WeakReference<DownloadMetadataCallback> mCallback;
        @NonNull
        private final WeakReference<Context> mContext;
        @NonNull
        private final AsyncProgressFragment mListener;
        @Nullable
        private static String mLastErrorMsg = null;

        DownloadMetadataTask(@NonNull AsyncProgressFragment f, @NonNull String serverUrl,
                             @NonNull DownloadMetadataCallback callback, @NonNull Context c) {
            mCallback = new WeakReference<>(callback);
            mServerUrl = serverUrl;
            mContext = new WeakReference<>(c);
            mListener = f;
        }

        @NonNull
        private static HttpURLConnection openConnection(@NonNull String address,
                                                        boolean allowRedirect) throws IOException {
            // Thanks https://stackoverflow.com/questions/15754633/android-httpurlconnection-handle-http-redirects
            // We allow up to one level of redirects
            HttpURLConnection conn = (HttpURLConnection) new URL(address).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setInstanceFollowRedirects(false);

            int code = conn.getResponseCode();
            boolean redirected = code == HTTP_MOVED_PERM ||
                    code == HTTP_MOVED_TEMP || code == HTTP_SEE_OTHER;
            if (allowRedirect && redirected) {
                address = conn.getHeaderField("Location");
                conn.disconnect();
                conn = openConnection(address, false);
            }

            return conn;
        }

        // Get XML from a url address
        @NonNull
        private static String getXMLFromURL(@NonNull String address) throws IOException {
            HttpURLConnection conn = openConnection(address, true);
            //conn.setRequestProperty("Accept", "application/xml");

            BufferedReader rdr = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder xml = new StringBuilder();
            String line;
            while ((line = rdr.readLine()) != null) {
                xml.append(line).append('\n');
            }
            return xml.toString();
        }

        // Get a bitmap from a URL address
        @Nullable
        private byte[] getBitmapFromURL(@NonNull String address) throws IOException {
            FabLogger.debug("Downloading cover art from " + address);
            HttpURLConnection conn = openConnection(address, true);

            final int length = conn.getContentLength();
            if (length <= 0) {
                mLastErrorMsg = "Did not find a cover image at " + address;
                FabLogger.error(mLastErrorMsg);
                return null;
            }

            InputStream in = new BufferedInputStream(conn.getInputStream(), 8192);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte bytes[] = new byte[8192];
            int count;
            while ((count = in.read(bytes)) != -1) {
                out.write(bytes, 0, count);
            }
            in.close();
            out.close();

            return out.toByteArray();
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Override
        @Nullable
        protected IFictionRecord[] doInBackground(@NonNull IFictionRecord... ifrs) {
            // returns an array with one entry for each ifid - either the entry is NULL if there
            // was an error, or an IFiction object if data was retrieved
            IFictionRecord[] ret = new IFictionRecord[ifrs.length];
            int k = 0;
            Context c = mContext.get();

            if (c == null) {
                mLastErrorMsg = "Cannot get a valid context.";
                FabLogger.error("AsyncLoaders: downloadeMetadata: " + mLastErrorMsg);
                return null;
            }

            final File gameDataDir;
            try {
                gameDataDir = new File(GLKConstants.getDir(c, GLKConstants.SUBDIR.GAMEDATA));
            } catch (IOException e) {
                mLastErrorMsg = "Cannot get game data directory.";
                FabLogger.error("AsyncLoaders: downloadMetadata: ");
                return null;
            }

            try {
                for (IFictionRecord ifr : ifrs) {
                    // Assume failure
                    ret[k] = null;

                    String format = ifr.format;  // don't overwrite this
                    if (ifr.ifids != null && ifr.ifids.length > 0) {
                        String ifid = ifr.ifids[0];

                        // Try to create the game data directory if it doesn't already exist
                        File ifidDir = new File(gameDataDir, ifid);
                        if (!ifidDir.exists() && !ifidDir.mkdirs()) {
                            FabLogger.warn("AsyncLoaders: downloadMetadata: Cannot create IFID game data subdirectory.");
                        }

                        // Try to download the metadata
                        String xml = getXMLFromURL(mServerUrl + ifr.ifids[0]);
                        boolean r = IFictionRecord.loadMetadataFromXML(ifr, xml);
                        ifr.format = format;
                        if (r) {
                            ret[k] = ifr;
                        }

                        // Try to download the cover
                        if (ifr.coverURL != null && !ifr.coverURL.equals("")) {
                            byte[] b = getBitmapFromURL(ifr.coverURL);
                            FabLogger.error("bitmap size is " + b.length);
                            if (b != null) {
                                IFictionRecord.saveCoverToDisk(c, b, 0, b.length, ifid);
                                ret[k] = ifr;
                            }
                        }
                    }

                    k++;
                }
                return ret;
            } catch (IOException e) {
                mLastErrorMsg = e.getMessage();
                return null;
            }
        }

        @Override
        protected void onPostExecute(@Nullable IFictionRecord[] result) {
            FabLogger.flush();
            DownloadMetadataCallback callback = mCallback.get();
            if (callback != null) {
                callback.onDownloadMetadataPostExecute(result, mLastErrorMsg);
            }
            mListener.onTaskFinished();
        }
    }

    private static class CopyFilesTask extends AsyncTask<File, File[], File[]> {
        @NonNull
        private final AsyncProgressFragment mListener;
        @NonNull
        private WeakReference<CopyFilesCallback> mCallback;
        @NonNull
        private String mTag;

        CopyFilesTask(@NonNull AsyncProgressFragment f, @NonNull String tag, @NonNull CopyFilesCallback callback) {
            mCallback = new WeakReference<>(callback);
            mTag = tag;
            mListener = f;
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Override
        @Nullable
        protected File[] doInBackground(@NonNull File... mappings) {
            // returns an array with one entry for each mapping, s.t.
            // if the copy was successful the entry is the new destination file; otherwise
            // the entry is NULL
            int sz = mappings.length;

            if (sz % 2 != 0) {
                // the mappings should be (srcPath, destPath), (srcPath, destPath), etc.
                return null;
            }

            File[] ret = new File[sz / 2];
            for (int i = 0, k = 0; i < sz; i += 2, k++) {
                File src = mappings[i];
                File dst = mappings[i + 1];

                if (src.isDirectory()) {
                    ret[k] = dst;              // assume success
                    File[] children;
                    try {
                        children = src.listFiles();
                    } catch (SecurityException e) {
                        // If a security manager exists and its checkRead(String) method denies read access to the directory
                        FabLogger.error("Cannot copy folder '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' as according to Android, your security manager has denied read access to this directory.");
                        continue;
                    }

                    if (children != null) {
                        for (File child : children) {
                            File[] r = doInBackground(child, new File(dst, child.getName()));
                            if (r == null || r.length != 1 || r[0] == null) {
                                ret[k] = null;
                            }
                        }
                    }
                } else {
                    ret[k] = null;             // assume failure

                    // create the destination directory, if it doesn't exist
                    File parent = dst.getParentFile();
                    if (parent == null) {
                        // failed - try the next one
                        FabLogger.error("Cannot copy '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' - cannot access the destination's parent directory.");
                        continue;
                    }

                    if (!parent.exists() && !parent.mkdirs()) {
                        // failed - try the next one
                        FabLogger.error("Cannot copy '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' - the destination's parent directory doesn't exist and I can't create it.");
                        continue;
                    }

                    // if the file already exists, check with user if we should proceed
                    if (dst.exists()) {
                        // TODO: for now this is just a guard, in future pop up a dialog box that
                        // allows user to decide if they want to overwrite the destination file
                        // failed - try the next one
                        FabLogger.error("Cannot copy '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' - the destination file already exists. Delete the destination file first, then run this command again.");
                        continue;
                    }

                    try {
                        InputStream in = new FileInputStream(src);
                        OutputStream out = new FileOutputStream(dst.isDirectory() ?
                                new File(dst, src.getName()) : dst);
                        byte[] buf = new byte[4096];
                        int len;
                        try {
                            while ((len = in.read(buf)) > 0) {
                                out.write(buf, 0, len);
                            }
                        } catch (IOException e) {
                            // failed - try the next one
                            FabLogger.error("Cannot copy '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                    "' - IO error: " + e.getMessage());
                            continue;
                        } finally {
                            in.close();
                            out.close();
                        }
                    } catch (IOException e1) {
                        // failed - try the next one
                        FabLogger.error("Cannot copy '" + src.getAbsolutePath() + "' to '" + dst.getAbsolutePath() +
                                "' - IO error: " + e1.getMessage());
                        continue;
                    }

                    // success
                    ret[k] = dst;
                }
            }

            return ret;
        }

        @Override
        protected void onPostExecute(@Nullable File[] result) {
            FabLogger.flush();
            CopyFilesCallback callback = mCallback.get();
            if (callback != null) {
                callback.onCopyFilesPostExecute(mTag, result);
            }
            mListener.onTaskFinished();
        }
    }

    private static class RefreshDatabaseTask extends AsyncTask<File, Void, Void> {
        @NonNull
        private final AsyncProgressFragment mListener;
        @NonNull
        private final RefreshDbHelper mDbHelper;
        @NonNull
        private final WeakReference<RefreshDbCallbacks> mCallbacks;
        @NonNull
        private final WeakReference<Context> mContext;

        RefreshDatabaseTask(@NonNull AsyncProgressFragment f, @NonNull Context c,
                            @NonNull RefreshDbHelper dbHelper, @NonNull RefreshDbCallbacks callbacks) {
            super();
            mDbHelper = dbHelper;
            mCallbacks = new WeakReference<>(callbacks);
            mContext = new WeakReference<>(c);
            mListener = f;
        }

        @Override
        protected void onPreExecute() {
            mListener.onTaskStarted();
        }

        @Nullable
        @Override
        protected Void doInBackground(File... syncDirs) {
            Context c = mContext.get();
            if (c != null) {
                mDbHelper.refresh(c, syncDirs[0]);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            RefreshDbCallbacks c = mCallbacks.get();
            if (c != null) {
                c.onPostExecute();
            }
            mListener.onTaskFinished();
        }
    }
}
