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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Debug;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;

public final class GLKUtils {

    private GLKUtils() {
        // prevent instantiation
    }

    /**
     * If the given directory doesn't already exist, try to create it (including any
     * missing parent directories).
     *
     * @param destDirPath - directory to check
     * @return true if and only if the directory was created, along with
     * all necessary parent directories; false otherwise
     * @throws SecurityException If a security manager exists and its checkRead(java.lang.String)
     *                           method does not permit verification of the existence of the named directory and all
     *                           necessary parent directories; or if the checkWrite(java.lang.String) method does not
     *                           permit the named directory and all necessary parent directories to be created.
     */
    public static boolean makeDir(@NonNull String destDirPath) throws SecurityException {
        File dir = new File(destDirPath);

        if (!dir.exists()) {
            GLKLogger.debug("The directory " + dir.getAbsolutePath() + " does not exist. Attempting to create it.");
            if (!dir.mkdirs()) {
                GLKLogger.error("Could not create directory: " + dir.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    public static int translateKeyToGLKCode(int ch) {
        // Translate special keys to GLK constants
        switch (ch) {
            case 8:
                return GLKConstants.keycode_Delete;
            case 9:
                return GLKConstants.keycode_Tab;
            case 10:
                return GLKConstants.keycode_Return;
            case 27:
                return GLKConstants.keycode_Escape;
            case 8592:
                return GLKConstants.keycode_Left;
            case 8593:
                return GLKConstants.keycode_Up;
            case 8594:
                return GLKConstants.keycode_Right;
            case 8595:
                return GLKConstants.keycode_Down;
            case 8670:
                return GLKConstants.keycode_PageUp;
            case 8671:
                return GLKConstants.keycode_PageDown;
            default:
                return ch;
        }
    }

    @Nullable
    public static GLKActivity getActivityForView(View view) {
        // Thanks Gomino
        // https://stackoverflow.com/questions/8276634/android-get-hosting-activity-from-a-view
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (context instanceof GLKActivity) ? (GLKActivity) context : null;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    @NonNull
    public static int[] getPrimitiveIntArray(@NonNull List<Integer> listInt) {
        // Thanks
        //   https://stackoverflow.com/questions/718554/how-to-convert-an-arraylist-containing-integers-to-primitive-int-array
        int[] ret = new int[listInt.size()];
        int i = 0;
        for (Integer n : listInt) {
            ret[i++] = n;
        }
        return ret;
    }

    public static int spToPx(int sp, @NonNull DisplayMetrics dm) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, dm);
    }

    public static int dpToPx(int dp, @NonNull DisplayMetrics dm) {
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm) + 0.5f);
    }

    @NonNull
    public static Rect dpToPx(@NonNull Rect r, @NonNull DisplayMetrics dm) {
        Rect ret = new Rect();
        ret.left = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, r.left, dm) + 0.5f);
        ret.top = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, r.top, dm) + 0.5f);
        ret.right = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, r.right, dm) + 0.5f);
        ret.bottom = (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, r.bottom, dm) + 0.5f);
        return ret;
    }

    public static int pxToDp(int px, @NonNull DisplayMetrics dm) {
        return (int) ((float) px / ((float) dm.densityDpi / DisplayMetrics.DENSITY_DEFAULT));
    }

    private static int calculateInSampleSize(@NonNull BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static Bitmap getBitmapFromByteArray(@NonNull byte[] data, int byteCount, int reqWidth, int reqHeight) {
        try {
            if (reqWidth == 0 || reqHeight == 0) {
                return BitmapFactory.decodeByteArray(data, 0, byteCount);
            }

            // Check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, byteCount, options);

            // Scale if required (in which case DON'T load entire bitmap into memory first)
            if (reqWidth != options.outWidth || reqHeight != options.outHeight) {
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;
                Bitmap b = BitmapFactory.decodeByteArray(data, 0, byteCount, options);
                if (b != null) {
                    Bitmap sc = Bitmap.createScaledBitmap(b, reqWidth, reqHeight, true);
                    if (sc != b) {
                        // createScaledBitmap will return the original object if it
                        // is the same size we're scaling to, so need to check if we
                        // have a new object before doing any recycling..!
                        b.recycle();
                    }
                    return sc;
                } else {
                    return null;
                }
            } else {
                return BitmapFactory.decodeByteArray(data, 0, byteCount);
            }
        } catch (OutOfMemoryError e) {
            GLKLogger.warn("getBitmapFromByteArray: out of memory, will not display image.");
            return null;
        }
    }

    public static Bitmap getBitmapFromResource(@NonNull Resources res, int resId,
                                               int reqWidth, int reqHeight) {
        try {
            // Check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeResource(res, resId, options);

            // Scale if required (in which case DON'T load entire bitmap into memory first)
            if (reqWidth != options.outWidth || reqHeight != options.outHeight) {
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;
                Bitmap b = BitmapFactory.decodeResource(res, resId, options);
                if (b != null) {
                    Bitmap sc = Bitmap.createScaledBitmap(b, reqWidth, reqHeight, true);
                    if (sc != b) {
                        // createScaledBitmap will return the original object if it
                        // is the same size we're scaling to, so need to check if we
                        // have a new object before doing any recycling..!
                        b.recycle();
                    }
                    return sc;
                } else {
                    return null;
                }
            } else {
                return BitmapFactory.decodeResource(res, resId);
            }
        } catch (OutOfMemoryError e) {
            GLKLogger.warn("getBitmapFromResource: out of memory, will not display image.");
            return null;
        }
    }

    public static Bitmap getBitmapFromFile(@NonNull String path, int reqWidth, int reqHeight) {
        try {
            if (reqWidth == 0 || reqHeight == 0) {
                return BitmapFactory.decodeFile(path);
            }

            // Check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, options);

            // Scale if required (in which case DON'T load entire bitmap into memory first)
            if (reqWidth != options.outWidth || reqHeight != options.outHeight) {
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                options.inJustDecodeBounds = false;
                Bitmap b = BitmapFactory.decodeFile(path, options);
                if (b != null) {
                    Bitmap sc = Bitmap.createScaledBitmap(b, reqWidth, reqHeight, true);
                    if (sc != b) {
                        // createScaledBitmap will return the original object if it
                        // is the same size we're scaling to, so need to check if we
                        // have a new object before doing any recycling..!
                        b.recycle();
                    }
                    return sc;
                } else {
                    return null;
                }
            } else {
                return BitmapFactory.decodeFile(path);
            }
        } catch (OutOfMemoryError e) {
            GLKLogger.warn("getBitmapFromFile: out of memory, will not display image.");
            return null;
        }
    }

    private static short read8(@NonNull ByteBuffer bb) throws BufferUnderflowException {
        // read unsigned byte
        return ((short) (bb.get() & 0xff));
    }

    private static void write8(@NonNull ByteBuffer bb, short vl) throws BufferOverflowException {
        // write unsigned byte
        bb.put((byte) (vl & 0xff));
    }

    private static long read32(@NonNull ByteBuffer bb) throws BufferUnderflowException {
        // read unsigned 32-bit integer
        return ((long) bb.getInt() & 0xffffffffL);
    }

    public static long read32(@NonNull byte[] b, int addr) {
        // read unsigned 32-bit integer
        int firstByte = (0x000000FF & (b[addr]));
        int secondByte = (0x000000FF & (b[addr + 1]));
        int thirdByte = (0x000000FF & (b[addr + 2]));
        int fourthByte = (0x000000FF & (b[addr + 3]));
        return (firstByte << 24
                | secondByte << 16
                | thirdByte << 8
                | fourthByte)
                & 0xFFFFFFFFL;
    }

    /**
     * read_ieee_extended
     * Extended precision IEEE floating-point conversion routine.
     *
     * @param dis - data input stream to read from.
     * @return double
     * @throws IOException if any problems reading
     */
    public static double read80float(@NonNull DataInputStream dis) throws IOException {
        double f;
        int expon;
        long hiMant, loMant;
        long t1, t2;
        double HUGE = (3.40282346638528860e+38);

        expon = dis.readUnsignedShort();

        t1 = dis.readUnsignedShort();
        t2 = dis.readUnsignedShort();
        hiMant = t1 << 16 | t2;

        t1 = dis.readUnsignedShort();
        t2 = dis.readUnsignedShort();
        loMant = t1 << 16 | t2;

        if (expon == 0 && hiMant == 0 && loMant == 0) {
            f = 0;
        } else {
            if (expon == 0x7FFF)
                f = HUGE;
            else {
                expon -= 16383;
                expon -= 31;
                f = (hiMant * Math.pow(2, expon));
                expon -= 32;
                f += (loMant * Math.pow(2, expon));
            }
        }

        return f;
    }

    /**
     * big2little
     * Protected helper method to swap the order of bytes in a 32 bit int
     *
     * @param i - a 32 bit integer
     * @return 32 bits swapped value
     */
    public static int swapEndian32(int i) {
        int b1, b2, b3, b4;

        b1 = (i & 0xFF) << 24;
        b2 = (i & 0xFF00) << 8;
        b3 = (i & 0xFF0000) >> 8;
        b4 = (i & 0xFF000000) >>> 24;

        i = (b1 | b2 | b3 | b4);

        return i;
    }

    /**
     * big2little
     * Protected helper method to swap the order of bytes in a 16 bit short
     *
     * @param i - a 16 bit short
     * @return 16 bits swapped value
     */
    public static short swapEndian16(short i) {
        short high, low;

        high = (short) ((i & 0xFF) << 8);
        low = (short) ((i & 0xFF00) >>> 8);
        i = (short) (high | low);

        return i;
    }

    public static int ubyteToID32(char c1, char c2, char c3, char c4) {
        return ((c1 << 24) | (c2 << 16) | (c3 << 8) | c4);
    }

    public static String id32ToString(int id) {
        return String.valueOf((char) ((id >> 24) & 0xFF)) + (char) ((id >> 16) & 0xFF) + (char) ((id >> 8) & 0xFF) + (char) (id & 0xFF);
    }

    public static int getBufferPos(@NonNull ByteBuffer buf, boolean is32Bit, boolean isText) {
        int curPosInBytes = buf.position();
        if (is32Bit && isText) {
            // Buffer has UTF-8 characters, which can vary from 1-4 bytes.
            // Need to work out what current byte position is in terms of UTF-8 chars.
            // To do this, we currently scan the number of UTF-8 chars between
            // the start of the buffer and the current position (in bytes).
            // TODO: inefficient...
            buf.rewind();
            int ipos = 0;
            int validUTF8 = 1;
            while (ipos < curPosInBytes && validUTF8 > 0) {
                validUTF8 = seekToNextUTF8Char(buf);
                if (validUTF8 > 0) {
                    ipos++;
                }
            }
            buf.position(curPosInBytes);
            return ipos;
        }
        return (is32Bit ? (curPosInBytes / 4) : curPosInBytes);
    }

    private static void reverse(@NonNull byte[] array) {
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }

    public static void setBufferPos(@NonNull ByteBuffer buf, int pos, int seekmode, boolean is32Bit, boolean isText, int posEOF) {
        try {
            if (is32Bit && isText) {
                // Buffer has UTF-8 characters, which can vary from 1-4 bytes.
                // To seek a certain number of characters on a UTF-8 stream, we currently
                // scan the UTF-8 chars one-by-one. TODO: not very efficient...
                int ipos = 0;
                int validUTF8 = 1;
                switch (seekmode) {
                    case GLKConstants.seekmode_Start:
                        buf.rewind();
                        // fall through to next case =>
                    case GLKConstants.seekmode_Current:
                        while (ipos < pos && validUTF8 > 0) {
                            validUTF8 = seekToNextUTF8Char(buf);
                            if (validUTF8 > 0) {
                                ipos++;
                            }
                        }
                        break;
                    case GLKConstants.seekmode_End:
                        // TODO: extremely inefficient...
                        byte[] tmp = new byte[posEOF];
                        int i = Math.abs(pos);
                        int newPos = posEOF;
                        buf.rewind();
                        buf.get(tmp);
                        reverse(tmp);
                        ByteBuffer tmpBB = ByteBuffer.wrap(tmp);
                        tmpBB.limit(posEOF);
                        while (ipos < i && validUTF8 > 0) {
                            validUTF8 = seekToNextUTF8Char(tmpBB);
                            if (validUTF8 > 0) {
                                ipos++;
                                newPos -= validUTF8;
                            }
                        }
                        buf.position(newPos);
                        break;
                    default:
                        GLKLogger.error("GLKUtils: setBufferPos: unrecognised seekmode: " + seekmode);
                        break;
                }
            } else {
                int ipos = is32Bit ? (pos * 4) : pos;
                switch (seekmode) {
                    case GLKConstants.seekmode_Start:
                        buf.position(ipos);
                        break;
                    case GLKConstants.seekmode_End:
                        buf.position(posEOF + ipos);
                        break;
                    case GLKConstants.seekmode_Current:
                        buf.position(buf.position() + ipos);
                        break;
                    default:
                        GLKLogger.error("GLKUtils: setBufferPos: unrecognised seekmode: " + seekmode);
                        break;
                }
            }
        } catch (IllegalArgumentException e) {
            GLKLogger.error("GLKUtils: setBufferPos: illegal argument exception: " + e.getMessage());
        }
    }

    /**
     * Copies a block of characters from one buffer (binary or text) to another (binary or text).
     *
     * @param src         - the source buffer to read the characters from. The limit of this buffer should be set
     *                    to one byte past the last readable character. Reading continues until the source buffer's
     *                    limit is reached or there is no more space in the destination buffer (whichever comes first).
     * @param srcIs32Bit  - if TRUE the source buffer has 32bit elements, if FALSE it has 8 bit elements.
     * @param srcIsText   - if TRUE the source buffer is encoded text, if FALSE it is binary.
     * @param dest        - the destination buffer to copy the characters into. The limit of this buffer should be
     *                    set to the buffer's capacity.
     * @param destIs32Bit - if TRUE the destination buffer has 32bit elements, if FALSE it has 8 bit elements.
     * @param destIsText  - if TRUE the destination buffer is encoded text, if FALSE it is binary.
     * @return the number of characters transferred.
     */
    public static int copyBuffer(@NonNull ByteBuffer src, boolean srcIs32Bit, boolean srcIsText,
                                 @NonNull ByteBuffer dest, boolean destIs32Bit, boolean destIsText) {
        int ch;
        int nCharsCopied = 0;
        int srcStartPos = src.position();
        int srcLimit = src.limit();
        int bytesToTransfer = Math.min(src.remaining(), dest.remaining());
        int srcPrevUTF8Pos = src.position();

        try {
            if ((!srcIs32Bit && !destIs32Bit) ||
                    (srcIs32Bit && destIs32Bit && srcIsText == destIsText)) {
                // For these cases, we can just copy the bytes over in bulk:
                //    1. binary 8 / ISO-8559-1 => binary 8 / ISO-8559-1
                //    2. UTF-8 => UTF-8
                //    3. binary 32 => binary 32
                if (srcIs32Bit && srcIsText) {
                    // UTF-8 => UTF-8
                    // adjust bytesToTransfer downwards so it stops at boundary of last valid UTF-8 char
                    GLKLogger.error("copyBuffer: FIXME: UTF-8 => UTF-8");
                } else if (srcIs32Bit) {
                    // binary 32 => binary 32
                    // ensure we only copy bytes in multiple of 4 - round
                    // down to nearest multiple of 4
                    bytesToTransfer -= bytesToTransfer % 4;
                }
                src.limit(srcStartPos + bytesToTransfer);
                dest.put(src);
                src.limit(srcLimit);
                nCharsCopied = (srcIs32Bit) ? (bytesToTransfer / 4) : bytesToTransfer;
            } else {
                // Other cases require transformation of elements
                // e.g. from 8 bit to 32 bit, or from binary to UTF 8
                if (destIs32Bit) {
                    if (srcIs32Bit) {
                        if (destIsText) {
                            // 32 bit binary => UTF-8
                            while (src.hasRemaining()) {
                                ch = (int) read32(src);
                                putCharUTF8(ch, dest);
                                nCharsCopied++;
                            }
                        } else {
                            // UTF-8 => 32 bit binary
                            while (src.hasRemaining()) {
                                srcPrevUTF8Pos = src.position();
                                ch = getCharUTF8(src);
                                dest.putInt(ch);
                                nCharsCopied++;
                            }
                        }
                    } else {
                        if (destIsText) {
                            // ISO-8559-1 => UTF-8
                            while (src.hasRemaining()) {
                                ch = read8(src);
                                if (ch > 255) ch = 0x3F;
                                putCharUTF8(ch, dest);
                                nCharsCopied++;
                            }
                        } else {
                            // 8 bit binary => 32 bit binary
                            while (src.hasRemaining()) {
                                ch = read8(src);
                                dest.putInt(ch);
                                nCharsCopied++;
                            }
                        }
                    }
                } else {
                    if (destIsText) {
                        if (srcIsText) {
                            // UTF-8 => ISO-8859-1
                            while (src.hasRemaining()) {
                                srcPrevUTF8Pos = src.position();
                                ch = getCharUTF8(src);
                                if (ch > 255) ch = 0x3F;
                                write8(dest, (short) ch);
                                nCharsCopied++;
                            }
                        } else {
                            // 32 bit binary => ISO-8859-1
                            while (src.hasRemaining()) {
                                ch = (int) read32(src);
                                if (ch > 255) ch = 0x3F;
                                write8(dest, (short) ch);
                                nCharsCopied++;
                            }
                        }
                    } else {
                        if (srcIsText) {
                            // UTF-8 => 8 bit binary
                            while (src.hasRemaining()) {
                                srcPrevUTF8Pos = src.position();
                                ch = getCharUTF8(src);
                                write8(dest, (short) ch);
                                nCharsCopied++;
                            }
                        } else {
                            // 32 bit binary => 8 bit binary
                            while (src.hasRemaining()) {
                                ch = (int) read32(src);
                                write8(dest, (short) ch);
                                nCharsCopied++;
                            }
                        }
                    }
                }
            }
        } catch (BufferOverflowException e) {
            GLKLogger.warn("GLKUtils: copyBuffer: no more space to write, already transferred " + nCharsCopied + " chars.");
            GLKLogger.warn("GLKUtils: copyBuffer: src = " + src.toString() + ", dest = " + dest.toString());

            // we've read one more character than we wrote, so we need to back off
            // by one character in the source buffer to ensure any future operations
            // on this stream start at the right place.
            if (srcIs32Bit && srcIsText) {
                src.position(srcPrevUTF8Pos);
            } else {
                int pos = src.position();
                if (srcIs32Bit) {
                    src.position(pos - 4);
                } else {
                    src.position(pos - 1);
                }
            }
        } catch (BufferUnderflowException e) {
            GLKLogger.warn("GLKUtils: copyBuffer: no more chars to read, already transferred " + nCharsCopied + " chars.");
            GLKLogger.warn("GLKUtils: copyBuffer: src = " + src.toString() + ", dest = " + dest.toString());
        }
        return nCharsCopied;
    }

    /**
     * Transfers a null-terminated line from one buffer (binary or text) to another binary buffer.
     *
     * @param src         - the source buffer to read the characters from. The limit of this buffer should be set
     *                    to one byte past the last readable character. Reading continues until: (i) a new line
     *                    character '\n' is read; (ii) the source buffer's limit (less one character for the null
     *                    terminator) is reached; or (iii) there is no more space in the destination buffer
     *                    (whichever comes first).
     * @param srcIs32Bit  - if TRUE the source buffer has 32bit elements, if FALSE it has 8 bit elements.
     * @param srcIsText   - if TRUE the source buffer is encoded text, if FALSE it is binary.
     * @param dest        - the destination buffer to copy the characters into. The limit of this buffer should be
     *                    set to the buffer's capacity.
     * @param destIs32Bit - if TRUE the destination buffer has 32bit elements, if FALSE it has 8 bit elements.
     * @return the number of characters transferred, including any new line but excluding the terminating null.
     */
    public static int copyLine(@NonNull ByteBuffer src, boolean srcIs32Bit, boolean srcIsText,
                               @NonNull ByteBuffer dest, boolean destIs32Bit) {
        int ch = -1;
        int nCharsCopied = 0;

        try {
            if (destIs32Bit) {
                if (srcIs32Bit) {
                    if (srcIsText) {
                        // 32 bit, text mode (UTF-8) => 32 bit
                        while (src.hasRemaining() && ch != '\n') {
                            ch = getCharUTF8(src);
                            dest.putInt(ch);
                            nCharsCopied++;
                        }
                    } else {
                        // 32 bit, binary mode => 32 bit
                        while (src.hasRemaining() && ch != '\n') {
                            ch = src.getInt();
                            dest.putInt(ch);
                            nCharsCopied++;
                        }
                    }
                } else {
                    // 8 bit, binary/text mode (ISO-8859-1) => 32 bit
                    while (src.hasRemaining() && ch != '\n') {
                        ch = read8(src);
                        dest.putInt(ch);
                        nCharsCopied++;
                    }
                }
                dest.putInt(0);  // null-terminate
            } else {
                if (srcIs32Bit) {
                    if (srcIsText) {
                        // 32 bit, text mode (UTF-8) => 8 bit
                        while (src.hasRemaining() && ch != '\n') {
                            ch = getCharUTF8(src);
                            if (ch > 255) ch = 0x3F;
                            write8(dest, (short) ch);
                            nCharsCopied++;
                        }
                    } else {
                        // 32 bit, binary mode => 8 bit
                        while (src.hasRemaining() && ch != '\n') {
                            ch = src.getInt();
                            write8(dest, (short) ch);
                            nCharsCopied++;
                        }
                    }
                } else {
                    // 8 bit, text (ISO-8859-1) / binary mode => 8 bit
                    while (src.hasRemaining() && ch != '\n') {
                        ch = read8(src);
                        write8(dest, (short) ch);
                        nCharsCopied++;
                    }
                }
                dest.put((byte) 0);  // null-terminate
            }
        } catch (BufferOverflowException e) {
            GLKLogger.error("GLKUtils: copyLine: no more space to write: " + e.getMessage());

            // ensure dest buffer is null-terminated
            int i;
            if (destIs32Bit) {
                i = dest.limit() - 4;
                if (i >= 0) {
                    dest.putInt(i, 0);
                }
            } else {
                i = dest.limit() - 1;
                if (i >= 0) {
                    dest.put((byte) 0);
                }
            }
            nCharsCopied--;
        } catch (BufferUnderflowException e) {
            GLKLogger.error("GLKUtils: copyLine: no more chars to read: " + e.getMessage());

            // ensure dest buffer is null-terminated
            int i;
            if (destIs32Bit) {
                i = Math.min(dest.limit() - 4, dest.position());
                if (i >= 0) {
                    dest.putInt(i, 0);
                }
            } else {
                i = Math.min(dest.limit(), dest.position());
                if (i >= 0) {
                    dest.put((byte) 0);
                }
            }
        }
        return nCharsCopied;
    }

    /**
     * Reads one character from a buffer (binary or text).
     *
     * @param src         - the source buffer to read from. The limit of this buffer should be set to one byte past
     *                    the last readable character.
     * @param srcIs32Bit  - if TRUE the source buffer has 32bit elements, if FALSE it has 8 bit elements.
     * @param srcIsText   - if TRUE the source buffer is encoded text, if FALSE it is binary.
     * @param destIs32Bit - if TRUE the returned character will be 32 bit, if FALSE it will be 8 bit.
     * @return the character read, or -1 if no more characters are available.
     */
    public static int getChar(@NonNull ByteBuffer src, boolean srcIs32Bit, boolean srcIsText,
                              boolean destIs32Bit) {
        int ch;
        try {
            if (srcIs32Bit) {
                if (srcIsText) {
                    ch = getCharUTF8(src);
                    if (!destIs32Bit && ch > 255) ch = 0x3F;
                } else {
                    ch = src.getInt();
                }
            } else {
                ch = read8(src);
            }
        } catch (BufferUnderflowException e) {
            return -1;
        }
        return ch;
    }

    /**
     * Writes one character to a buffer (binary or text).
     *
     * @param ch          - the character to write
     * @param dest        - te destination buffer. The limit of this buffer should be
     *                    set to the buffer's capacity.
     * @param destIs32Bit - if TRUE the destination buffer has 32bit elements, if FALSE it has 8 bit elements.
     * @param destIsText  - if TRUE the destination buffer is encoded text, if FALSE it is binary.
     */
    public static void putChar(int ch,
                               @NonNull ByteBuffer dest, boolean destIs32Bit, boolean destIsText) {
        try {
            if (destIs32Bit) {
                if (destIsText) {
                    putCharUTF8(ch, dest);
                } else {
                    dest.putInt(ch);
                }
            } else {
                if (ch > 255) ch = 0x3F;
                dest.put((byte) (ch & 0xFF));  // write unsigned byte
            }
        } catch (BufferOverflowException e) {
            // Don't show this error message, as there are a lot of glulxe games that overflow
            // their memory buffers (e.g. Alabaster, City of Secrets). Just silently ignore (ho hum).
            //Logger.error("GLKUtils: putChar: no more space to write: charIn = '" + (char)charIn + "'.");
        }
    }

    private static int seekToNextUTF8Char(@NonNull ByteBuffer buf) {
        // returns the number of bytes skipped
        int startPos = buf.position();
        int val = read8(buf);
        int skipBytes = 0;
        if (val < 0x80) {
            skipBytes = 1;
        } else if ((val & 0xe0) == 0xc0) {
            skipBytes = 2;
        } else if ((val & 0xf0) == 0xe0) {
            skipBytes = 3;
        } else if ((val & 0xf0) == 0xf0) {
            skipBytes = 4;
        } else {
            GLKLogger.error("GLKFile: malformed character");
            return skipBytes;
        }
        buf.position(startPos + skipBytes);
        return skipBytes;
    }

    private static int getCharUTF8(@NonNull ByteBuffer src) throws BufferUnderflowException {
        // THIS IS AN ATOMIC READ: for multi-byte UTF8 chars, we need to ensure that they
        // are either fully read or not at all - don't allow partial reads. Hence we
        // check whether buffer has sufficient bytes remaining before reading all the bytes.
        int res;
        int val0, val1, val2, val3;
        int startSrcPos = src.position();

        val0 = read8(src);
        if (val0 < 0x80) {
            res = val0;
            return res;
        }

        if ((val0 & 0xe0) == 0xc0) {
            if (src.remaining() < 1) {
                src.position(startSrcPos);
                throw new BufferUnderflowException();
            }
            val1 = read8(src);
            if ((val1 & 0xc0) != 0x80) {
                GLKLogger.error("GLKUtils: getCharUTF8: malformed two-byte character");
                return '?';
            }
            res = (val0 & 0x1f) << 6;
            res |= (val1 & 0x3f);
            return res;
        }

        if ((val0 & 0xf0) == 0xe0) {
            if (src.remaining() < 2) {
                src.position(startSrcPos);
                throw new BufferUnderflowException();
            }
            val1 = read8(src);
            val2 = read8(src);
            if ((val1 & 0xc0) != 0x80) {
                GLKLogger.error("GLKUtils: getCharUTF8: malformed three-byte character");
                return '?';
            }
            if ((val2 & 0xc0) != 0x80) {
                GLKLogger.error("GLKUtils: getCharUTF8: malformed three-byte character");
                return '?';
            }
            res = (((val0 & 0xf) << 12) & 0x0000f000);
            res |= (((val1 & 0x3f) << 6) & 0x00000fc0);
            res |= (((val2 & 0x3f)) & 0x0000003f);
            return res;
        }

        if ((val0 & 0xf0) == 0xf0) {
            if (src.remaining() < 3) {
                src.position(startSrcPos);
                throw new BufferUnderflowException();
            }
            if ((val0 & 0xf8) != 0xf0) {
                GLKLogger.error("GLKUtils: getCharUTF8: malformed four-byte character");
                return '?';
            }
            val1 = read8(src);
            val2 = read8(src);
            val3 = read8(src);
            if ((val1 & 0xc0) != 0x80) {
                GLKLogger.error("GLKUtils: getCharUTF8: malformed four-byte character");
                return '?';
            }
            if ((val2 & 0xc0) != 0x80) {
                GLKLogger.error("GLKUtils: getCharUTF8: malformed four-byte character");
                return '?';
            }
            if ((val3 & 0xc0) != 0x80) {
                GLKLogger.error("GLKUtils: getCharUTF8: malformed four-byte character");
                return '?';
            }
            res = (((val0 & 0x7) << 18) & 0x1c0000);
            res |= (((val1 & 0x3f) << 12) & 0x03f000);
            res |= (((val2 & 0x3f) << 6) & 0x000fc0);
            res |= (((val3 & 0x3f)) & 0x00003f);
            return res;
        }

        GLKLogger.error("GLKFile: malformed character");
        return '?';
    }

    private static void putCharUTF8(int ch,
                                    @NonNull ByteBuffer dest) throws BufferOverflowException {
        // THIS IS AN ATOMIC WRITE: for multi-byte UTF8 chars, we need to ensure that they
        // are either fully written or not at all - don't allow partial transfers. Hence we
        // check whether buffer has sufficient space before writing the bytes.
        if (ch < 0x80) {
            dest.put((byte) (ch & 0xFF));
        } else if (ch < 0x800) {
            if (dest.remaining() < 2) throw new BufferOverflowException();
            dest.put((byte) ((0xC0 | ((ch & 0x7C0) >> 6)) & 0xFF));
            dest.put((byte) ((0x80 | (ch & 0x03F)) & 0xFF));
        } else if (ch < 0x10000) {
            if (dest.remaining() < 3) throw new BufferOverflowException();
            dest.put((byte) ((0xE0 | ((ch & 0xF000) >> 12)) & 0xFF));
            dest.put((byte) ((0x80 | ((ch & 0x0FC0) >> 6)) & 0xFF));
            dest.put((byte) ((0x80 | (ch & 0x003F)) & 0xFF));
        } else if (ch < 0x200000) {
            if (dest.remaining() < 4) throw new BufferOverflowException();
            dest.put((byte) ((0xF0 | ((ch & 0x1C0000) >> 18)) & 0xFF));
            dest.put((byte) ((0x80 | ((ch & 0x03F000) >> 12)) & 0xFF));
            dest.put((byte) ((0x80 | ((ch & 0x000FC0) >> 6)) & 0xFF));
            dest.put((byte) ((0x80 | (ch & 0x00003F)) & 0xFF));
        } else {
            dest.put((byte) (0x3F & 0xFF));
        }
    }

    public static void ensureDrawableWithinWidth(int maxWidthPX, @NonNull Drawable d) {
        Rect rectBounds = d.getBounds();
        if (rectBounds.width() > maxWidthPX) {
            float factor = (float) maxWidthPX / (float) rectBounds.width();
            rectBounds.right = (int) ((float) rectBounds.right * factor);
            rectBounds.bottom = (int) ((float) rectBounds.bottom * factor);
            d.setBounds(rectBounds);
        }
    }

    public static String getMemoryInfo() {
        Debug.MemoryInfo memoryInfo = new Debug.MemoryInfo();
        Debug.getMemoryInfo(memoryInfo);
        return String.format(Locale.US,
                "Memory: Pss=%.2f MB, Private=%.2f MB, Shared=%.2f MB\n\n",
                memoryInfo.getTotalPss() / 1024.0,
                memoryInfo.getTotalPrivateDirty() / 1024.0,
                memoryInfo.getTotalSharedDirty() / 1024.0);
    }

    public static CharSequence getLine(int pos, int minStartPos, int maxEndPos, @NonNull CharSequence spanned, @NonNull TextPaint tp, int width) {
        // Returns the line in 'spanned' containing position 'pos'
        int len = spanned.length();
        if (len > 0) {
            // Scan back to the last line break or start of text, whichever comes first
            int posStart;
            for (posStart = pos; posStart > 0; posStart--) {
                if (spanned.charAt(posStart) == '\n') {
                    posStart++;
                    break;
                }
            }
            posStart = Math.max(minStartPos, posStart);
            posStart = Math.max(0, posStart);

            // Scan forward to next line break or end of text, whichever comes first
            int posEnd;
            for (posEnd = pos + 1; posEnd < len; posEnd++) {
                if (spanned.charAt(posEnd) == '\n') {
                    break;
                }
            }
            posEnd = Math.min(maxEndPos, posEnd);
            posEnd = Math.min(len, posEnd);

            if (posStart < posEnd) {
                CharSequence txt = spanned.subSequence(posStart, posEnd);
                StaticLayout tmpLayout = new StaticLayout(txt, tp,
                        width, Layout.Alignment.ALIGN_NORMAL, 0f,
                        0f, false);
                int pos1 = getStartLinePos(pos, tmpLayout);
                int pos2 = getEndLinePos(pos1, tmpLayout);
                if (pos2 > pos1) {
                    return txt.subSequence(pos1, pos2);
                }
            }
        }
        return null;
    }

    private static int getStartLinePos(int pos, @NonNull StaticLayout tempLayout) {
        int line = tempLayout.getLineForOffset(pos - 1);
        // N.B. getLineStart returns a value in the range [0 … getLineCount()]
        return tempLayout.getLineStart(line);
    }

    private static int getEndLinePos(int startLinePos, @NonNull StaticLayout tempLayout) {
        int len = tempLayout.getText().length();
        int line = tempLayout.getLineForOffset(startLinePos);
        // N.B. getLineEnd returns the text offset AFTER the last character on the line
        return Math.min(len, tempLayout.getLineEnd(line));
    }

    /**
     * Measures the width of some spanned text.
     *
     * @param spanned - the spanned text
     * @param tp      - the text paint instance
     * @param width   - the maximum width (in pixels) of each line
     * @return the width of the spanned text, in pixels.
     */
    public static float getSpannedWidth(@NonNull CharSequence spanned, @NonNull TextPaint tp, int width) {
        StaticLayout tempLayout = new StaticLayout(spanned + "\n", tp, width,
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
        int lineCount = tempLayout.getLineCount();
        float textWidth = 0;
        for (int x = 0; x < lineCount; x++) {
            textWidth += tempLayout.getLineWidth(x);
        }
        return textWidth;
    }

    public static boolean isTerminator(int key, @Nullable int[] terminators) {
        if (key == GLKConstants.keycode_Return) {
            return true;
        }
        if (terminators != null) {
            for (int terminator : terminators) {
                if (key == terminator) {
                    return true;
                }
            }
        }
        return false;
    }
}
