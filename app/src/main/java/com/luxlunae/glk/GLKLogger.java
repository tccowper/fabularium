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
package com.luxlunae.glk;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public final class GLKLogger {
    // Thread-safe logger
    private static final String GLK_LOG = "glk.log";

    // Enable or disable passing messages through to Android logger
    private static final boolean LOG_WARN = false;
    private static final boolean LOG_DEBUG = false;
    private static final boolean LOG_ERROR = false;
    private static GLKLogger mInstance;
    @Nullable
    private static FileWriter mFile;

    private GLKLogger(@NonNull Context c) {
        super();
        try {
            String prefix = GLKConstants.getDir(c, null);
            mFile = new FileWriter(new File(prefix + GLK_LOG));
        } catch (IOException e) {
            error("Couldn't create GLK log file.");
            mFile = null;
        }
    }

    public static synchronized void initialise(@NonNull Context c) {
        mInstance = new GLKLogger(c);
    }

    public static synchronized void flush() {
        if (mInstance != null) {
            mInstance.flushFile();
        }
    }

    public static synchronized void shutdown() {
        if (mInstance != null) {
            mInstance.flushFile();
            mInstance.closeFile();
        }
    }

    public static synchronized void debug(@NonNull String s) {
        if (LOG_DEBUG) {
            Log.d("GLKLogger", s);
        }
        if (mInstance != null) {
            mInstance.appendLineToLogFile("[D] ".concat(s));
        }
    }

    public static synchronized void error(@NonNull String s) {
        if (LOG_ERROR) {
            Log.e("GLKLogger", s);
        }
        if (mInstance != null) {
            mInstance.appendLineToLogFile("[E] ".concat(s));
        }
    }

    public static synchronized void warn(@NonNull String s) {
        if (LOG_WARN) {
            Log.w("GLKLogger", s);
        }
        if (mInstance != null) {
            mInstance.appendLineToLogFile("[W] ".concat(s));
        }
    }

    private void appendLineToLogFile(@NonNull String s) {
        if (mFile != null) {
            try {
                mFile.append(s.concat("\n"));
            } catch (IOException e) {
                // do nothing
            }
        }
    }

    private void flushFile() {
        if (mFile != null) {
            try {
                mFile.flush();
            } catch (IOException e) {
                error("Couldn't flush log file at " + GLK_LOG + ": " + e.getMessage());
            }
        }
    }

    private void closeFile() {
        if (mFile != null) {
            try {
                mFile.close();
                mFile = null;
            } catch (IOException e) {
                error("Couldn't close log file at " + GLK_LOG + ": " + e.getMessage());
            }
        }
    }
}
