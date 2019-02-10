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
package com.luxlunae.glk.model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.util.LruCache;

import com.luxlunae.bebek.Bebek;
import com.luxlunae.glk.GLKCharsetManager;
import com.luxlunae.glk.GLKConstants;
import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;
import com.luxlunae.glk.model.stream.file.GLKFileRef;
import com.luxlunae.glk.model.stream.file.GLKMemoryStream;
import com.luxlunae.glk.model.stream.sound.GLKSoundStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class GLKResourceManager {

    @NonNull
    private final GLKModel mModel;
    @Nullable
    private LruCache<String, GLKDrawable> mImageCache;

    GLKResourceManager(@NonNull GLKModel m) {
        mModel = m;
    }

    @Nullable
    @SuppressWarnings("JniMissingFunction")
    private static native byte[] getTADSResource(@NonNull String resname);

    @SuppressWarnings("JniMissingFunction")
    private static native void freeTADSResource();

    @Nullable
    @SuppressWarnings("JniMissingFunction")
    private static native byte[] getBlorbResource(int method, int[] result, int usage, int resnum);

    @SuppressWarnings("JniMissingFunction")
    private static native void freeBlorbResource();

    public boolean getSound(@NonNull String sndId, @NonNull GLKSoundStream s) {
        int[] chunkInfo = new int[2];
        byte[] data;

        s.reset();      // ensure the sound stream is in a valid state for setting a data source
        switch (mModel.mTerpID) {
            case BOCFEL:
            case GIT:
            case GLULXE:
                // Try to load the sound from a blorb file
                data = getBlorbResource(GLKConstants.bb_method_Memory, chunkInfo, GLKConstants.bb_ID_Snd, Integer.valueOf(sndId));
                break;
            case TADS:
                // Try to load the sound from a TADS 2 or 3 resource file
                data = getTADSResource(sndId);
                break;
            case HUGO:
            case SCARE:
                // Try to load the sound from SND[sndID].glkdata
                try {
                    FileInputStream is = new FileInputStream(GLKFileRef.getGLKPath(mModel, "SND" + sndId, GLKConstants.fileusage_Data));
                    s.setDataSource(is.getFD());
                    s.prepare();
                } catch (FileNotFoundException e) {
                    GLKLogger.error("GLKResourceManager: getSound: could not load HUGO/SCARE sound as file " +
                            GLKFileRef.getGLKPath(mModel, "SND" + sndId, GLKConstants.fileusage_Data) + " does not exist");
                    return false;
                } catch (IOException e) {
                    GLKLogger.error("GLKResourceManager: getSound: could not read HUGO/SCARE sound file: " + e.getMessage());
                    return false;
                }
                return true;
            case BEBEK:
                // Try to load the sound from a blorb file (using Java interface)
                data = Bebek.getSound(sndId);
                if (data == null) {
                    GLKLogger.error("GLKResourceManager: getSound: could not load Java blorb sound resource: " + sndId);
                    return false;
                }
                break;
            default:
                return false;
        }

        if (data == null) {
            GLKLogger.error("GLKResourceManager: getSound: could not load sound resource: " + sndId);
            return false;
        }

        try {
            switch (mModel.mTerpID) {
                case BOCFEL:
                case GIT:
                case GLULXE: {
                    String chunkType = GLKUtils.id32ToString(chunkInfo[1]);
                    GLKLogger.debug("GLKResourceManager: Attempting to set BLORB sound data source for ID " + sndId + " of type '" + chunkType + "'");
                    s.setDataSource(data, chunkType);
                    freeBlorbResource();
                    s.prepare();
                    break;
                }
                case TADS: {
                    String chunkType = sndId.substring(sndId.length() - 3, sndId.length());
                    GLKLogger.debug("GLKResourceManager: Attempting to set TADS sound data source for ID " + sndId + " of type '" + chunkType + "'");
                    s.setDataSource(data, chunkType);
                    freeTADSResource();
                    s.prepare();
                    break;
                }
                case BEBEK: {
                    String chunkType = sndId.substring(sndId.length() - 3, sndId.length());
                    GLKLogger.debug("GLKResourceManager: Attempting to set BEBEK sound data source for ID " + sndId + " of type '" + chunkType + "'");
                    s.setDataSource(data, chunkType);
                    s.prepare();
                    break;
                }
            }
        } catch (IOException e) {
            GLKLogger.error("GLKResourceManager: getSound: failed to set data source: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Retrieve an image from the cache or the active game's resource file (method depends on game format).
     *
     * @param imgID    - id of picture to find
     * @param width    - if <=0 use original image width, otherwise return scaled to this width
     * @param height   - if <=0 use original image height, otherwise return scaled to this height
     * @param sizeInDp - if TRUE, width and height are in DP.  If FALSE, they are in PX.
     * @return the image if successful, NULL if not.
     */
    @Nullable
    public GLKDrawable getImage(@NonNull String imgID, int width, int height, boolean sizeInDp) {
        //Logger.debug("getImage: '" + path + "', width = " + width + ", height = " + height);
        Resources res = mModel.getApplicationContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        // Try to load from the cache
        if (mImageCache == null) {
            // We save memory by only initialising this the first time we need it
            createImageCache(mModel.mPicCacheSize);
        }
        String cacheID = imgID.concat(String.valueOf(width).concat(String.valueOf(height)));
        GLKDrawable drawable = mImageCache.get(cacheID);
        if (drawable == null) {
            Bitmap img;
            if (sizeInDp) {
                if (width > 0) {
                    width = GLKUtils.dpToPx(width, dm);
                }
                if (height > 0) {
                    height = GLKUtils.dpToPx(height, dm);
                }
            }

            // Load the image in different ways, depending upon the game format
            byte[] data;
            switch (mModel.mTerpID) {
                case TADS:
                    // Try to load the picture from a TADS 2 or 3 resource file
                    data = getTADSResource(imgID);
                    if (data == null) {
                        GLKLogger.error("GLKResourceManager: getImage: could not find tads resource: " + imgID);
                        return null;
                    }
                    img = GLKUtils.getBitmapFromByteArray(data, data.length, width, height);
                    if (img == null) {
                        GLKLogger.error("GLKResourceManager: getImage: could not decode bitmap!");
                        return null;
                    }
                    freeTADSResource();
                    break;
                case HUGO:
                case SCARE:
                    // Try to load the picture from PIC[imgID].glkdata
                    File f = new File(GLKFileRef.getGLKPath(mModel, "PIC" + imgID, GLKConstants.fileusage_Data));
                    if (f.exists()) {
                        img = GLKUtils.getBitmapFromFile(f.getAbsolutePath(), width, height);
                    } else {
                        GLKLogger.error("GLKResourceManager: getImage: could not load HUGO/SCARE picture as file " + f.getAbsolutePath() + " does not exist");
                        return null;
                    }
                    break;
                case BOCFEL:
                case GIT:
                case GLULXE:
                    // Try to load the picture from a blorb file
                    data = getBlorbResource(GLKConstants.bb_method_Memory, null, GLKConstants.bb_ID_Pict, Integer.valueOf(imgID));
                    if (data == null) {
                        GLKLogger.error("GLKResourceManager: getImage: could not load blorb picture resource: " + imgID);
                        return null;
                    }
                    img = GLKUtils.getBitmapFromByteArray(data, data.length, width, height);
                    freeBlorbResource();
                    break;
                case BEBEK:
                    // Load the picture from the blorb file (using Java interface)
                    data = Bebek.getImage(imgID);
                    if (data == null) {
                        GLKLogger.error("GLKResourceManager: getImage: could not load Java blorb picture resource: " + imgID);
                        return null;
                    }
                    img = GLKUtils.getBitmapFromByteArray(data, data.length, width, height);
                    break;
                default:
                    GLKLogger.error("GLKResourceManager: getImage: don't know how to load image resource for terp " + mModel.mTerpID);
                    return null;
            }

            if (img != null) {
                drawable = new GLKDrawable(res, img, imgID);
                int w = (width > 0) ? width : (sizeInDp ? GLKUtils.dpToPx(img.getWidth(), dm) : img.getWidth());
                int h = (height > 0) ? height : (sizeInDp ? GLKUtils.dpToPx(img.getHeight(), dm) : img.getHeight());
                drawable.setBounds(0, 0, w, h);

                // Cache the image for future references
                mImageCache.put(cacheID, drawable);
            }
        }

        return drawable;
    }

    @Nullable
    public GLKMemoryStream getMemoryStream(int fileNum, boolean unicode) {
        // Try to load the requested blorb resource
        GLKCharsetManager charsetManager = mModel.mCharsetMgr;
        if (charsetManager == null) {
            return null;
        }
        int[] chunkInfo = new int[2];
        byte[] tmp = getBlorbResource(GLKConstants.bb_method_Memory, chunkInfo, GLKConstants.bb_ID_Data, fileNum);

        if (tmp == null || tmp.length == 0) {
            freeBlorbResource();
            return null;
        }

        // We successfully loaded the resource, now wrap it with a new read-only memory stream
        // From GLK spec:
        //   "A Blorb file can contain images and sounds,
        //    but it can also contain raw data files. A data file is identified by number,
        //    not by a filename. The Blorb usage field will be 'Data'. The chunk type
        //    will be 'TEXT' for text resources, 'BINA' or 'FORM' for binary resources."
        byte[] tmp2 = tmp.clone();
        freeBlorbResource();
        String chunkType = GLKUtils.id32ToString(chunkInfo[1]);
        return new GLKMemoryStream(ByteBuffer.wrap(tmp2), unicode, GLKConstants.filemode_Read,
                charsetManager, false, chunkType.equals("TEXT"));
    }

    /**
     * Works out the width and height of image id without loading it into memory.
     *
     * @param imgID   - the ID of the image
     * @param options - upon successful completion this will contain the height and width info
     * @return true if success, false otherwise (e.g. image doesn't exist in blorb file)
     */
    public boolean getImageSize(int imgID, @NonNull BitmapFactory.Options options) {
        options.inJustDecodeBounds = true;
        switch (mModel.mTerpID) {
            case HUGO:
            case SCARE:
                // Try to load picture dimensions from PIC[imgID].glkdata
                File f = new File(GLKFileRef.getGLKPath(mModel, "PIC" + imgID, GLKConstants.fileusage_Data));
                if (f.exists()) {
                    BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                } else {
                    GLKLogger.error("GLKResourceManager: getImageSize: could not load HUGO/SCARE picture as file " + f.getAbsolutePath() + " does not exist");
                    return false;
                }
                break;
            case BOCFEL:
            case GIT:
            case GLULXE:
                // Try to load picture dimensions from a blorb file
                byte[] data = getBlorbResource(GLKConstants.bb_method_Memory, null, GLKConstants.bb_ID_Pict, imgID);
                if (data == null) {
                    GLKLogger.error("GLKResourceManager: getImageSize: could not load blorb picture resource: " + imgID);
                    return false;
                }
                BitmapFactory.decodeByteArray(data, 0, data.length, options);
                freeBlorbResource();
                break;
            default:
                GLKLogger.error("GLKResourceManager: getImageSize: don't know how to load image resource for terp " + mModel.mTerpID);
                return false;
        }
        return true;
    }

    private void createImageCache(int cacheSize) {
        GLKLogger.debug("GLKResourceManager: picture cache set to " + cacheSize + "KB");
        mImageCache = new LruCache<String, GLKDrawable>(cacheSize) {
            @Override
            protected int sizeOf(String key, @NonNull GLKDrawable drawable) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return drawable.getBitmap().getByteCount() / 1024;
            }
        };
    }

    void clearImageCache() {
        if (mImageCache != null) {
            mImageCache.evictAll();
        }
    }
}
