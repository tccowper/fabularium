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
import android.support.annotation.Nullable;

import com.luxlunae.glk.GLKCharsetManager;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class GLKFileStream extends GLKMemoryStream {
    private static final int BYTE_BUFFER_CHUNK = 102400;        // 100 kB
    @NonNull
    private final GLKFileRef mFref;
    @Nullable
    private RandomAccessFile mFile = null;
    @Nullable
    private FileChannel mFileC = null;

    public GLKFileStream(@NonNull GLKFileRef fref, boolean is32bit, @NonNull GLKCharsetManager mgr) {
        super(null, is32bit, GLKConstants.filemode_Read, mgr);
        mFref = fref;
        mIsTextMode = fref.isTextMode();
    }

    /**
     * Open the file stream for reading and/or writing.
     *
     * @param fm - one of the following values:
     *           <p>
     *           filemode_Read: The file must already exist; the player will be asked
     *           to select from existing files which match the usage.
     *           <p>
     *           filemode_Write: The file should not exist; if the player selects an
     *           existing file, he will be warned that it will be replaced.
     *           <p>
     *           filemode_ReadWrite: The file may or may not exist; if it already
     *           exists, the player will be warned that it will be modified.
     *           <p>
     *           filemode_WriteAppend: Same behavior as filemode_ReadWrite.
     * @return TRUE if success, FALSE otherwise.
     */
    public boolean open(int fm) {
        mFileMode = fm;

        String mode = "";
        try {
            switch (fm) {
                case GLKConstants.filemode_Read:
                    mode = "r";
                    break;
                case GLKConstants.filemode_Write:
                case GLKConstants.filemode_ReadWrite:
                case GLKConstants.filemode_WriteAppend:
                    mode = "rw";
                    break;
                default:
                    GLKLogger.error("GLKFileStream: opened with unrecognised file mode: " + fm);
                    return false;
            }

            mFile = new RandomAccessFile(mFref, mode);
            mFileC = mFile.getChannel();
            if (mFileC == null) {
                GLKLogger.error("GLKFile: could not get channel for " + mFref.getAbsolutePath());
                return false;
            }

            if (fm == GLKConstants.filemode_Write) {
                // Write
                mBB = ByteBuffer.allocateDirect(BYTE_BUFFER_CHUNK);
                if (mBB == null) {
                    GLKLogger.error("GLKFile: cannot allocate byte buffer with size " + BYTE_BUFFER_CHUNK);
                    return false;
                }
                mFileC.truncate(0);
                mEOF = 0;
            } else {
                // Read, ReadWrite or WriteAppend
                mBB = ByteBuffer.allocateDirect(Math.max((int) mFileC.size(), BYTE_BUFFER_CHUNK));
                if (mBB == null) {
                    GLKLogger.error("GLKFile: cannot allocate byte buffer.");
                    return false;
                }
                mFileC.read(mBB);
                mEOF = mBB.position();
                if (fm != GLKConstants.filemode_WriteAppend) {
                    mBB.rewind();
                }
            }
            mBB.limit(mEOF);
        } catch (IOException fe) {
            GLKLogger.error("GLKFile: could not open " + mFref.getAbsolutePath() + " with mode '" + mode + "'");
            GLKLogger.error(fe.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Close the file stream, and flush any changes.
     */
    @Override
    public void close() {
        if (mFileC != null) {
            try {
                if (mBB != null && mFileMode != GLKConstants.filemode_Read && mEOF > 0) {
                    mBB.rewind();
                    mFileC.position(0);
                    mFileC.write(mBB);
                }
                mFileC.close();
            } catch (IOException e) {
                GLKLogger.error("GLKFile: could not close " + mFref.getAbsolutePath());
                GLKLogger.error(e.getMessage());
            }
            mFileC = null;
        }
        if (mFile != null) {
            try {
                mFile.close();
            } catch (IOException e) {
                GLKLogger.error("GLKFile: could not close " + mFref.getAbsolutePath());
                GLKLogger.error(e.getMessage());
            }
            mFile = null;
        }
        mBB = null;
        setEchoer(null);
    }

    @Override
    public void putChar(int ch) {
        if (mBB != null) {
            int bytesNeeded = mIs32Bit ? 4 : 1;
            if ((mBB.capacity() - mBB.position()) < bytesNeeded) {
                extendBB();
            }
        } else {
            GLKLogger.warn("GLKFileStream: putChar " + (char) ch + ": dropping because mBB is null.");
        }
        super.putChar(ch);
    }

    @Override
    public void putBuffer(@NonNull ByteBuffer buffer, boolean buffer32bit) {
        if (mBB != null) {
            int bytesNeeded = buffer.remaining() * 4;   // this is the upper bound, not necessarily the exact amount
            if ((mBB.capacity() - mBB.position()) < bytesNeeded) {
                extendBB();
            }
        } else {
            GLKLogger.warn("GLKFileStream: putBuffer: dropping because mBB is null.");
        }
        super.putBuffer(buffer, buffer32bit);
    }

    @Override
    public void putString(@NonNull String s) {
        if (mBB != null) {
            int bytesNeeded = s.length() * 4;           // this is the upper bound, not necessarily the exact amount
            if ((mBB.capacity() - mBB.position()) < bytesNeeded) {
                extendBB();
            }
        } else {
            GLKLogger.warn("GLKFileStream: putStrig '" + s + "': dropping because mBB is null.");
        }
        super.putString(s);
    }

    /**
     * Helper function to dynamically grow the underlying byte buffer if there is insufficient space.
     */
    private void extendBB() {
        if (mBB == null) return;
        int pos = mBB.position();
        ByteBuffer tmp = ByteBuffer.allocateDirect(mBB.capacity() * 2);
        mBB.rewind();
        tmp.put(mBB);
        mBB = tmp;
        mBB.position(pos).limit(mEOF);
        GLKLogger.debug("GLKFileStream: grew byte buffer to " + mBB.capacity());
    }
}
