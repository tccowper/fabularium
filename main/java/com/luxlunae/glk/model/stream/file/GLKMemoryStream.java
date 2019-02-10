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
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.controller.GLKController;
import com.luxlunae.glk.model.stream.GLKInputStream;
import com.luxlunae.glk.model.stream.GLKOutputStream;
import com.luxlunae.glk.model.stream.window.GLKNonPairM;

import java.nio.ByteBuffer;

public class GLKMemoryStream implements GLKInputStream, GLKOutputStream {
    final boolean mIs32Bit;
    @NonNull
    private final GLKCharsetManager mCharsetMgr;
    private final boolean mUsingJNIByteBuffer;  // if this is true, then byte buffer will be freed via call to JNI upon close
    int mFileMode;
    @Nullable
    ByteBuffer mBB = null;
    int mEOF;                   // End-of-file marker: always one past the last char in the stream
    boolean mIsTextMode;        // By default, memory streams are in binary mode.
    private int mRock;
    private int mReadCount = 0;
    private int mWriteCount = 0;
    @Nullable
    private GLKNonPairM mEchoer = null;
    private int mStreamId = 0;

    /**
     * Open a stream which reads from or writes into a space in memory.
     *
     * @param buffer   - the memory buffer where output will be read from or written to.
     *                 If NULL, everything written to the stream is thrown away. If not NULL, then
     *                 the limit of the buffer should be set to indicate the extent of any existing data
     *                 in the buffer.
     * @param is32bit  - if true, buffer has unicode elements (4 bytes), otherwise has byte elements.
     * @param fileMode - must be filemode_Read, filemode_Write, or filemode_ReadWrite.
     */
    public GLKMemoryStream(@Nullable ByteBuffer buffer, boolean is32bit, int fileMode, @NonNull GLKCharsetManager mgr) {
        this(buffer, is32bit, fileMode, mgr, true, false);
    }

    public GLKMemoryStream(@Nullable ByteBuffer buffer, boolean is32bit,
                           int fileMode, @NonNull GLKCharsetManager mgr, boolean usingJNIByteBuffer, boolean openInTextMode) {
        mFileMode = fileMode;
        mIs32Bit = is32bit;
        mCharsetMgr = mgr;
        mBB = buffer;
        mEOF = (buffer != null && fileMode == GLKConstants.filemode_Read) ? buffer.limit() : 0;
        mUsingJNIByteBuffer = usingJNIByteBuffer;
        mIsTextMode = openInTextMode;
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
        return mStreamId;
    }

    @Override
    public void setStreamId(int id) {
        mStreamId = id;
    }

    /**
     * Close the memory stream and stop wrapping the underlying memory block.
     */
    @Override
    public void close() {
        if (mBB != null) {
            if (mUsingJNIByteBuffer) {
                GLKController.freeByteBuffer(mBB);
            }
            mBB = null;
        }
        setEchoer(null);
    }

    /**
     * Set the position of the mark.
     *
     * @param pos      - has different meanings depending upon seekmode:
     *                 <p>
     *                 seekmode_Start: pos characters after the beginning of the file.
     *                 <p>
     *                 seekmode_Current: pos characters after the current position (moving
     *                 backwards if pos is negative.)
     *                 <p>
     *                 seekmode_End: pos characters after the end of the file. (pos should
     *                 always be zero or negative, so that this will move backwards to a position
     *                 within the file.)
     * @param seekmode - either seekmode_Start, seekmode_Current or seekmode_End.
     */
    @Override
    public void setPosition(long pos, int seekmode) {
        if (mBB == null) return;
        GLKUtils.setBufferPos(mBB, (int) pos, seekmode, mIs32Bit, mIsTextMode, mEOF);
    }

    /**
     * Get the position of the mark.
     *
     * @return the mark position.
     */
    @Override
    public long getPosition() {
        if (mBB == null) return 0;
        return (long) GLKUtils.getBufferPos(mBB, mIs32Bit, mIsTextMode);
    }

    /**
     * Read one character.
     * <p/>
     * Result will be between 0 and 255. As with all basic text functions, Glk
     * assumes the Latin-1 encoding. If the end of the stream has been reached,
     * the result will be -1. [[Note that  high-bit characters (128..255) are
     * *not* returned as negative numbers.]]
     * <p/>
     * If the stream contains Unicode data -- for example, if it was created with
     * glk_stream_open_file_uni() or glk_stream_open_memory_uni() -- then
     * characters beyond 255 will be returned as 0x3F ("?").
     *
     * @return character read or -1 if end of stream.
     */
    @Override
    public int getChar(boolean char32bit) {
        if (mFileMode == GLKConstants.filemode_Write) {
            // This stream was opened write-only!
            GLKLogger.warn("GLKMemoryStream: read called on a write-only file.");
            return -1;
        }
        if (mBB == null || mBB.remaining() == 0) return -1;

        int ch = GLKUtils.getChar(mBB, mIs32Bit, mIsTextMode, char32bit);
        if (ch != -1)
            ++mReadCount;
        return ch;
    }

    /**
     * Read from the stream until either the end-of-stream is reached
     * or the given buffer is filled to its limit. No terminal null is
     * placed in the buffer.
     *
     * @param buffer      - destination buffer.
     * @param buffer32bit - if true, destination buffer has 32 bit chars, otherwise it has 8 bit chars.
     * @return number of elements actually read.
     */
    @Override
    public int getBuffer(@NonNull ByteBuffer buffer, boolean buffer32bit) {
        if (mFileMode == GLKConstants.filemode_Write) {
            // This stream was opened write-only!
            GLKLogger.warn("GLKMemoryStream: read called on a write-only file.");
            return 0;
        }
        if (mBB == null) return 0;
        int lenBytes = Math.min(buffer.remaining(), mBB.remaining());
        if (lenBytes == 0) return 0;  // nothing to read

        mBB.limit(mBB.position() + lenBytes);
        int nReadChars = GLKUtils.copyBuffer(mBB, mIs32Bit, mIsTextMode, buffer, buffer32bit, false);
        mBB.limit(mEOF);

        mReadCount += nReadChars;
        return nReadChars;
    }

    /**
     * Read from the stream, until a newline has been read, or the
     * given buffer has been filled to one character less than its limit, or the
     * end of the stream has been reached (whichever occurs first). A terminal
     * null ('\0') is then appended to the buffer.
     *
     * @param buffer      - destination buffer.
     * @param buffer32bit - if true, destination buffer has 32 bit chars, otherwise it has 8 bit chars.
     * @return number of characters actually read, including any new line, but not including
     * the terminal null.
     */
    @Override
    public int getLine(@NonNull ByteBuffer buffer, boolean buffer32bit) {
        if (mFileMode == GLKConstants.filemode_Write) {
            // This stream was opened write-only!
            GLKLogger.warn("GLKMemoryStream: read called on a write-only file.");
            return 0;
        }
        if (mBB == null) return 0;
        int lenBytes = Math.min(buffer.remaining() - (buffer32bit ? 4 : 1), mBB.remaining()); // ensure space for null terminator
        if (lenBytes <= 0) return 0;  // nothing to read

        mBB.limit(mBB.position() + lenBytes);
        int nReadChars = GLKUtils.copyLine(mBB, mIs32Bit, mIsTextMode, buffer, buffer32bit);
        mBB.limit(mEOF);

        mReadCount += nReadChars;
        return nReadChars;
    }

    /**
     * Print one character.
     *
     * @param ch - character to print.
     */
    @Override
    public void putChar(int ch) {
        if (mFileMode == GLKConstants.filemode_Read) {
            // This stream was opened read-only!
            GLKLogger.warn("GLKMemoryStream: write called on a read-only file.");
            return;
        }

        // Refer GLK spec - write count is incremented
        // irrespective of whether enough space in the stream
        ++mWriteCount;
        if (mBB == null) return;
        mBB.limit(mBB.capacity());
        GLKUtils.putChar(ch, mBB, mIs32Bit, mIsTextMode);
        mEOF = Math.max(mEOF, mBB.position());
        mBB.limit(mEOF);
    }

    /**
     * Print a block of characters.
     *
     * @param buffer      - source buffer
     * @param buffer32bit - if true, source buffer has 32 bit chars, otherwise it has 8 bit chars.
     */
    @Override
    public void putBuffer(@NonNull ByteBuffer buffer, boolean buffer32bit) {
        if (mFileMode == GLKConstants.filemode_Read) {
            // This stream was opened read-only!
            GLKLogger.warn("GLKMemoryStream: write called on a read-only file.");
            return;
        }

        // Refer GLK spec - write count is incremented
        // irrespective of whether enough space in the stream
        mWriteCount += (buffer32bit ? (buffer.remaining() / 4) : buffer.remaining());
        if (mBB == null) return;
        mBB.limit(mBB.capacity());
        GLKUtils.copyBuffer(buffer, buffer32bit, false, mBB, mIs32Bit, mIsTextMode);
        mEOF = Math.max(mEOF, mBB.position());
        mBB.limit(mEOF);
    }

    /**
     * Print a string.
     *
     * @param s - source string
     */
    @Override
    public void putString(@NonNull String s) {
        if (mFileMode == GLKConstants.filemode_Read) {
            // This stream was opened read-only!
            GLKLogger.warn("GLKMemoryStream: write called on a read-only file.");
            return;
        }

        // Refer GLK spec - write count is incremented
        // irrespective of whether enough space in the stream
        mWriteCount += s.length();
        if (mBB == null) return;
        mBB.limit(mBB.capacity());
        mCharsetMgr.putGLKString(s, mBB, mIs32Bit, mIsTextMode);
        mEOF = Math.max(mEOF, mBB.position());
        mBB.limit(mEOF);
    }

    @Override
    public long getWriteCount() {
        return mWriteCount;
    }

    @Override
    public long getReadCount() {
        return mReadCount;
    }

    @Override
    public void setStyle(int s) {
        // "for a memory stream, style changes have no effect."
    }

    @Override
    public void startHyperlink(int linkval) {
        // by default memory streams are in binary mode so this has no effect
    }

    @Override
    public void endHyperlink() {
        // by default memory streams are in binary mode so this has no effect
    }

    @Override
    public void setEchoer(@Nullable GLKNonPairM w) {
        if (mEchoer != null) {
            // Detach ourselves from the existing echoer
            mEchoer.setEchoee(null);
        }
        mEchoer = w;
    }
}
