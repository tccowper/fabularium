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

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.glk.GLKConstants;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;

public class RunProgramService extends Service {
    public static final String CMD_ARGS = "com.luxlunae.fabularium.CMD_ARGS";
    public static final String CMD_RECEIVER = "com.luxlunae.fabularium.CMD_RECEIVER";
    public static final String CMD_RESULT = "com.luxlunae.fabularium.CMD_RESULT";
    public static final String CMD_RESULT_MSG = "com.luxlunae.fabularium.CMD_RESULT_MSG";
    private static final String OUTPUT_FILE = "out.txt";

    @SuppressWarnings("JniMissingFunction")
    public static native int runProgram(@NonNull String progLibName, @NonNull RunProgramService g, @NonNull String[] argv, @NonNull String outPath);

    private static String readFile(@NonNull File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        CharBuffer cb = CharBuffer.allocate(1024);
        while (r.read(cb) != -1) {
            cb.flip();
            sb.append(cb);
            cb.clear();
        }
        r.close();
        return sb.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(@NonNull Intent intent, int flags, int startId) {
        if ((flags & START_FLAG_RETRY) != 0) {
            FabLogger.debug("onStartCommand called automatically - will just ignore");
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        // Get the arguments
        String args = intent.getStringExtra(CMD_ARGS);
        if (args == null) {
            FabLogger.error("RunProgramService: attempted to start service with no args.");
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        // Load the program bootstrap, if we haven't already
        try {
            // It should be safe to call loadLibrary for the bootstrap module even if it
            // has already been loaded because, as per the Android docs for Runtime.loadLibrary,
            //
            //   "If this method is called more than once with the same library name, the
            //    second and subsequent calls are ignored."
            //
            System.loadLibrary("utils");
        } catch (@NonNull UnsatisfiedLinkError | SecurityException e) {
            FabLogger.error("RunProgramService: could not load utilloader.");
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }

        // Dynamically load the correct program based on the first argument.
        // We need to use the absolute path to the program module  (.so file).
        // It seems Android versions above KitKat only need the library name, but KitKat
        // and lower need the absolute path, otherwise they'll fail with a LinkLookupError.
        String argv[] = args.split(" ");
        if (argv.length == 0) {
            FabLogger.error("RunProgramService: no arguments provided.");
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }
        ApplicationInfo appInfo = getApplicationContext().getApplicationInfo();
        String progLibPath = appInfo.nativeLibraryDir + "/lib" + argv[0] + ".so";

        // Start the program
        String outputFilePath;
        try {
            outputFilePath = GLKConstants.getDir(getApplicationContext(), null) + OUTPUT_FILE;
        } catch (@NonNull IOException | SecurityException e) {
            FabLogger.error("RunProgramService: cannot access output file. Check you have enabled file permissions. Also check that, if you have overridden the default paths in your settings, Fabularium has read/write access to that path.");
            return START_NOT_STICKY;
        }
        int status = runProgram(progLibPath, this, argv, outputFilePath);

        // Read in any output
        File outFile = new File(outputFilePath);
        String output = "";
        try {
            output = readFile(outFile);
        } catch (IOException e) {
            // do nothing - just assume there was no output to display
        }
        if (!outFile.delete()) {
            FabLogger.warn("RunProgramService: cannot delete output file.");
        }

        // Send the result back to the calling activity
        ResultReceiver rec = intent.getParcelableExtra(CMD_RECEIVER);
        Bundle bundle = new Bundle();
        bundle.putInt(CMD_RESULT, status);
        bundle.putString(CMD_RESULT_MSG, output);
        rec.send(Activity.RESULT_OK, bundle);

        // Tell the service to die
        stopSelfResult(startId);
        return START_NOT_STICKY;
    }
}
