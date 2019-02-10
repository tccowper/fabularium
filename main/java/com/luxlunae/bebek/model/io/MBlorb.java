/*
 * Original ADRIFT code Copyright (C) 1997 - 2018 Campbell Wild
 * This port and modifications Copyright (C) 2018 - 2019 Tim Cadogan-Cowper.
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

package com.luxlunae.bebek.model.io;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.glk.GLKLogger;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;

import static com.luxlunae.bebek.MGlobals.errMsg;

public class MBlorb {

    // TODO: For now we implement this entirely in Java, but
    // ideally we would just pull in the C implementation
    // without all the accompanying GLK bits
    @Nullable
    static byte[] mExecResource;
    @Nullable
    static String mMetadata;
    @Nullable
    private static String mFilename;
    private static int mReadOffset = 0;
    @Nullable
    private static RandomAccessFile mBlorb;
    private static int mFrontispiece = -1;
    @NonNull
    private static HashMap<String, Long> mResourceIndex = new HashMap<>();
    @Nullable
    private static MFormChunk mFORMChunk;

    private static long byteToInt(@NonNull byte[] b) {
        long l = 0;
        l |= b[0] & 0xFF;
        l <<= 8;
        l |= b[1] & 0xFF;
        l <<= 8;
        l |= b[2] & 0xFF;
        l <<= 8;
        l |= b[3] & 0xFF;
        return l;
    }

    static boolean loadBlorb(@NonNull RandomAccessFile blorb,
                             @NonNull String filename) {
        clear();
        mFilename = filename;
        mBlorb = blorb;
        mReadOffset = 0;
        mFORMChunk = new MFormChunk();
        return mFORMChunk.load();
    }

    public static byte[] getSound(int resNum) {
        try {
            mBlorb = new RandomAccessFile(mFilename, "r");
            Long offset = mResourceIndex.get("Snd " + resNum);
            if (offset != null) {
                MSoundResourceChunk soundChunk = new MSoundResourceChunk();
                mBlorb.seek(offset);
                if (soundChunk.load()) {
                    return soundChunk.mSoundData;
                }
            }
            mBlorb.close();
            mBlorb = null;
        } catch (Exception ex) {
            errMsg("getSound error", ex);
        }
        return null;
    }

    public static byte[] getImage(int resNum) {
        try {
            mBlorb = new RandomAccessFile(mFilename, "r");
            Long offset = mResourceIndex.get("Pict" + resNum);
            if (offset != null) {
                MPictResourceChunk imageChunk = new MPictResourceChunk();
                mBlorb.seek(offset);
                if (imageChunk.load()) {
                    return imageChunk.mImageData;
                }
            }
            mBlorb.close();
            mBlorb = null;
        } catch (Exception ex) {
            errMsg("getImage error", ex);
        }
        return null;
    }

    private static void clear() {
        mExecResource = null;
        mResourceIndex.clear();
        mMetadata = null;
        mFrontispiece = -1;
    }

    private static abstract class MChunk {
        @NonNull
        String mID = "";
        @NonNull
        ArrayList<MChunk> mChunks = new ArrayList<>();
        private long mLength;

        @NonNull
        public String getID() {
            return mID;
        }

        public void setID(@NonNull String id) {
            mID = id;
        }

        public long getLength() {
            return mLength;
        }

        public void setLength(long value) {
            mLength = value;
        }

        boolean load() {
            return load(-1);
        }

        public boolean load(long startPos) {
            if (mBlorb == null) {
                return false;
            }

            try {
                if (startPos > -1) {
                    mBlorb.seek(startPos);
                }

                // First 4 bytes tell us what sort of chunk this is
                byte[] id = new byte[4];
                mReadOffset += mBlorb.read(id, 0, 4);
                mID = new String(id, "UTF-8");

                // Next 4 bytes tell us the size of the chunk
                byte[] sz = new byte[4];
                mReadOffset += mBlorb.read(sz, 0, 4);
                mLength = byteToInt(sz);

                return true;
            } catch (IOException e) {
                return false;
            }
        }

        void skipPadding() {
            if (mBlorb == null) {
                return;
            }

            try {
                if (mBlorb.getFilePointer() % 2 == 1) {
                    mBlorb.skipBytes(1);
                }
            } catch (IOException e) {
                GLKLogger.error("MBlorb: skipPadding: error: " + e.getMessage());
            }
        }
    }

    private static class MSkipChunk extends MChunk {
        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            try {
                mBlorb.seek(mBlorb.getFilePointer() + getLength());
                skipPadding();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private static class MFrontispieceChunk extends MChunk {
        @Override
        @NonNull
        public String getID() {
            return "Fspc";
        }

        @Override
        public void setID(@NonNull String id) {
            if (!id.equals("Fspc")) {
                GLKLogger.error("Bad ID in Frontispiece Chunk: " + id);
            }
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            try {
                // Number of a Pict resource
                byte[] imgResNum = new byte[4];
                mReadOffset += mBlorb.read(imgResNum, 0, 4);
                mFrontispiece = (int) (byteToInt(imgResNum));
                skipPadding();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private static class MMetaDataChunk extends MChunk {
        @Override
        @NonNull
        public String getID() {
            return "IFmd";
        }

        @Override
        public void setID(@NonNull String id) {
            if (!id.equals("IFmd")) {
                GLKLogger.error("Bad ID in Metadata Chunk: " + id);
            }
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            try {
                byte[] meta = new byte[(int) getLength()];
                mReadOffset += mBlorb.read(meta, 0, (int) getLength());
                mMetadata = new String(meta, "UTF-8");
                skipPadding();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private static class MDataChunk extends MChunk {
        @NonNull
        @Override
        public String getID() {
            if (mID.equals("")) {
                mID = "TEXT";
            }
            return mID;
        }

        @Override
        public void setID(@NonNull String id) {
            switch (id) {
                case "TEXT":
                case "BINA":
                    mID = id;
                    break;
                default:
                    GLKLogger.error("Bad ID in Data Resource Chunk: " + id);
                    break;
            }
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            try {
                byte[] dt = new byte[(int) getLength()];
                mReadOffset += mBlorb.read(dt, 0, (int) getLength());
                String data = new String(dt, "UTF-8");
                String dataType = data.substring(0, 4);

                skipPadding();

                switch (dataType) {
                    case "RLAY":
                        // Restore Runner Layout
                        GLKLogger.error("MBlorb: TODO: restore runner layout");
                        break;
                }
                return true;
            } catch (IOException e) {
                return false;
            }
        }

    }

    private static class MExecResourceChunk extends MChunk {
        @NonNull
        @Override
        public String getID() {
            return mID;
        }

        @Override
        public void setID(@NonNull String id) {
            switch (id) {
                case "ZCOD":
                case "GLUL":
                case "TAD2":
                case "TAD3":
                case "HUGO":
                case "ALAN":
                case "ADRI":
                case "LEVE":
                case "AGT ":
                case "MAGS":
                case "ADVS":
                case "EXEC":
                    mID = id;
                    break;
                default:
                    GLKLogger.error("Bad ID in Exec Resource Chunk: " + id);
                    break;
            }
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            try {
                byte[] execRes = new byte[(int) getLength()];
                mReadOffset += mBlorb.read(execRes, 0, (int) getLength());
                mExecResource = execRes;
                skipPadding();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private static class MResourceIndexChunk extends MChunk {
        @NonNull
        @Override
        public String getID() {
            return "RIdx";
        }

        @Override
        public void setID(@NonNull String id) {
            if (!id.equals("RIdx")) {
                GLKLogger.error("Bad ID in Resource Index Chunk: " + id);
            }
        }

        int getResourceCount() {
            return mResourceIndex.size();
        }

        @Override
        public long getLength() {
            return (long) (4 + (12 * getResourceCount()));
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            try {
                // Next 4 bytes tell us the number of resources in the index
                byte[] count = new byte[4];
                mReadOffset += mBlorb.read(count, 0, 4);
                int resCount = (int) (byteToInt(count));
                for (int i = 0; i < resCount; i++) {
                    String resKey;
                    byte[] usage = new byte[4];
                    mReadOffset += mBlorb.read(usage, 0, 4);
                    resKey = new String(usage, "UTF-8");

                    byte[] resNum = new byte[4];
                    mReadOffset += mBlorb.read(resNum, 0, 4);
                    resKey += byteToInt(resNum);

                    byte[] resOffset = new byte[4];
                    mReadOffset += mBlorb.read(resOffset, 0, 4);
                    mResourceIndex.put(resKey, byteToInt(resOffset));
                }

                skipPadding();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    private static class MSoundResourceChunk extends MChunk {
        byte[] mSoundData;

        @Override
        public void setID(@NonNull String id) {
            switch (id) {
                case "AIFF":
                case "OGGV":
                case "MOD ":
                    mID = id;
                    break;
                case "MP3 ":
                case "WAVE":
                case "MIDI":
                    // We allow, although the Blorb specification
                    // technically doesn't allow these formats.
                    mID = id;
                    break;
                default:
                    GLKLogger.error("Bad ID in Sound Resource Chunk: " + id);
                    break;
            }
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            byte[] snd = new byte[(int) getLength()];
            try {
                mBlorb.read(snd, 0, (int) getLength());
                mSoundData = snd;
                skipPadding();
                return true;

            } catch (IOException e) {
                return false;
            }
        }
    }

    private static class MPictResourceChunk extends MChunk {
        byte[] mImageData;

        @Override
        public void setID(@NonNull String id) {
            switch (id) {
                case "PNG ":
                case "JPEG":
                case "GIF ":
                    mID = id;
                    break;
                default:
                    GLKLogger.error("Bad ID in Picture Resource Chunk: " + id);
                    break;
            }
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            byte[] img = new byte[(int) getLength()];
            try {
                mBlorb.read(img, 0, (int) getLength());
                mImageData = img;
                skipPadding();
                return true;

            } catch (IOException e) {
                return false;
            }
        }
    }

    private static class MFormChunk extends MChunk {
        @Nullable
        MResourceIndexChunk mResIndexChunk;

        @NonNull
        @Override
        public String getID() {
            return "FORM";
        }

        @Override
        public void setID(@NonNull String id) {
            if (!id.equals("FORM")) {
                GLKLogger.error("Bad ID in Form Chunk: " + id);
            }
        }

        @Override
        public long getLength() {
            long ret = 0;
            for (MChunk c : mChunks) {
                ret += c.getLength();
            }
            return ret;
        }

        @Override
        public boolean load(long startPos) {
            if (!super.load(startPos)) {
                return false;
            }

            assert mBlorb != null;

            try {
                // Next 4 bytes tell us the FORM type
                byte[] id = new byte[4];
                mReadOffset += mBlorb.read(id, 0, 4);
                mResIndexChunk = new MResourceIndexChunk();
                if (!mResIndexChunk.load()) {
                    return false;
                }

                long fileLen = mBlorb.length();
                while (mBlorb.getFilePointer() < fileLen) {
                    MChunk cnk;

                    // Peek at the next 4 bytes to work out what chunk type it is
                    byte[] chunkType = new byte[4];
                    mReadOffset = (int) (mBlorb.getFilePointer());
                    mReadOffset += mBlorb.read(chunkType, 0, 4);
                    switch (new String(chunkType, "UTF-8")) {
                        case "ZCOD":
                        case "GLUL":
                        case "TAD2":
                        case "TAD3":
                        case "HUGO":
                        case "ALAN":
                        case "ADRI":
                        case "LEVE":
                        case "AGT ":
                        case "MAGS":
                        case "ADVS":
                        case "EXEC":
                            cnk = new MExecResourceChunk();
                            break;
                        case "GIF ":
                        case "PNG ":
                        case "JPEG":
                            cnk = new MPictResourceChunk();
                            break;
                        case "AIFF":
                        case "OGGV":
                        case "MOD ":
                        case "MP3 ":
                        case "WAVE":
                        case "MIDI":
                            cnk = new MSoundResourceChunk();
                            break;
                        case "IFmd":
                            cnk = new MMetaDataChunk();
                            break;
                        case "Fspc":
                            cnk = new MFrontispieceChunk();
                            break;
                        case "Plte":
                        case "IFhd":
                            // Colour Palette
                            // Game Identifier
                            // => Ignore
                            cnk = new MSkipChunk();
                            break;
                        case "TEXT":
                        case "BINA":
                            cnk = new MDataChunk();
                            break;
                        default:
                            // Unknown type
                            cnk = new MSkipChunk();
                            break;
                    }
                    if (!cnk.load(mReadOffset - 4)) {
                        return false;
                    }
                }
                skipPadding();
                return true;

            } catch (IOException e) {
                return false;
            }
        }
    }
}
