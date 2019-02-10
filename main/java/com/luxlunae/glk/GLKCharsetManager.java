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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnsupportedCharsetException;

public class GLKCharsetManager {

    private final boolean mReverseByteOrderIfError;

    @Nullable
    private CharsetDecoder mDecoderISO_8859_1;
    @Nullable
    private CharsetDecoder mDecoderUTF8;
    @Nullable
    private CharsetDecoder mDecoderUTF32;
    @Nullable
    private CharsetDecoder mDecoderTADS2;
    @Nullable
    private CharsetEncoder mEncoderISO_8859_1;
    @Nullable
    private CharsetEncoder mEncoderUTF8;
    @Nullable
    private CharsetEncoder mEncoderUTF32;
    @Nullable
    private CharsetEncoder mEncoderTADS2;

    public GLKCharsetManager(ENDIAN byteOrder, @NonNull String tads2Charset) {
        mReverseByteOrderIfError =
                (byteOrder == ENDIAN.BIG_REVERSE_IF_ERROR || byteOrder == ENDIAN.LITTLE_REVERSE_IF_ERROR);
        String utf32Name =
                (byteOrder == ENDIAN.BIG_FIXED || byteOrder == ENDIAN.BIG_REVERSE_IF_ERROR) ? "UTF-32BE" : "UTF-32LE";

        // INITIALISE THE DECODERS
        // We shouldn't have any problems initialising ISO_8859_1 and UTF8, as these are part
        // of the StandardCharsets and should be on every user's device. But the others could fail.
        // If there is failure, don't die (it's possible the game doesn't use that particular decoder),
        // but do warn the user that text may not display correctly.
        mDecoderISO_8859_1 = getDecoder("ISO-8859-1");
        mDecoderUTF8 = getDecoder("UTF-8");
        mDecoderUTF32 = getDecoder(utf32Name);
        mDecoderTADS2 = getDecoder(tads2Charset);
        if (mDecoderTADS2 == null) {
            GLKLogger.warn("GLKCharsetManager: because the specified charset for TADS2 is not available on your device, we will now attempt to set it to UTF8 instead.");
            mDecoderTADS2 = mDecoderUTF8;
        }

        // INITIALISE THE ENCODERS
        // We shouldn't have any problems initialising ISO_8859_1 and UTF8, as these are part
        // of the StandardCharsets and should be on every user's device. But the others could fail.
        // If there is failure, don't die (it's possible the game doesn't use that particular encoder),
        // but do warn the user that input and other functions may not work properly.
        mEncoderISO_8859_1 = getEncoder("ISO-8859-1");
        mEncoderUTF8 = getEncoder("UTF-8");
        mEncoderUTF32 = getEncoder(utf32Name);
        mEncoderTADS2 = getEncoder(tads2Charset);
        if (mDecoderTADS2 == null) {
            mEncoderTADS2 = mEncoderUTF8;
        }
    }

    /**
     * Attempt to create a decoder for the supplied charset name.
     *
     * @param charsetName - charset that we want an decoder for.
     * @return the decoder, or NULL if there was an error (e.g. the user's device doesn't have
     * that charset installed).
     */
    @Nullable
    private CharsetDecoder getDecoder(@NonNull String charsetName) {
        try {
            Charset cs = Charset.forName(charsetName);
            CharsetDecoder decoder = cs.newDecoder();
            // If the decoding fails, it might be because endianness is incorrect,
            // in which case we'll want to try again with the reverse endian:
            decoder.onMalformedInput(CodingErrorAction.REPORT);
            decoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            return decoder;
        } catch (@NonNull UnsupportedCharsetException | IllegalCharsetNameException e) {
            // Thrown by Charset.forName. That function can also throw IllegalArgumentException
            // if its argument is null, which we don't bother catching here because we know charsetName
            // never will be (annotated as @NonNull)
            GLKLogger.warn("GLKCharsetManager: getDecoder: error: cannot find charset '" + charsetName +
                    "' on your device. If the game uses this charset, the text will not display correctly!");
            return null;
        }
    }

    /**
     * Attempt to create an encoder for the supplied charset name.
     *
     * @param charsetName - charset that we want an encoder for.
     *
     * @return the encoder, or NULL if there was an error (e.g. the user's device doesn't have
     * that charset installed).
     */
    @Nullable
    private CharsetEncoder getEncoder(@NonNull String charsetName) {
        try {
            Charset cs = Charset.forName(charsetName);
            CharsetEncoder encoder = cs.newEncoder();
            encoder.onMalformedInput(CodingErrorAction.REPLACE);
            encoder.onUnmappableCharacter(CodingErrorAction.REPLACE);
            return encoder;
        } catch (@NonNull UnsupportedCharsetException | IllegalCharsetNameException e) {
            // Thrown by Charset.forName. That function can also throw IllegalArgumentException
            // if its argument is null, which we don't bother catching here because we know charsetName
            // never will be (annotated as @NonNull)
            GLKLogger.warn("GLKCharsetManager: getEncoder: error: cannot find charset '" + charsetName +
                    "' on your device. If the game uses this charset, user input and other things may not work properly!");
            return null;
        }
    }

    /**
     * Read a TADS string from a byte buffer.
     *
     * @param src - the source byte buffer to read the string from. The limit of this buffer should be set
     *            to one byte past the last readable character.
     *
     * @param isTads3 - whether we are reading a TADS 2 string or a TADS 3 string. This determines which encoding to use
     *                when interpreting the byte buffer - TADS 3 games always use UTF-8, but TADS 2 games can use anything.
     *                Therefore for TADS 2 games, we rely on the user's specified encoding, only falling back to UTF-8
     *                if that setting is invalid.
     *
     * @return the decoded string, or "" if there was an error.
     */
    @NonNull
    public String getTADSString(@NonNull ByteBuffer src, boolean isTads3) {
        CharBuffer ret = null;

        if (!src.hasRemaining()) {
            return "";
        }

        try {
            if (isTads3) {
                if (mDecoderUTF8 != null) {
                    ret = mDecoderUTF8.decode(src);
                }
            } else {
                if (mDecoderTADS2 != null) {
                    ret = mDecoderTADS2.decode(src);
                }
            }
        } catch (MalformedInputException e) {
            GLKLogger.error("GLKCharsetManager: TADS decoder: couldn't decode text: malformed input: " + e.getMessage());
        } catch (IllegalStateException e) {
            GLKLogger.error("GLKCharsetManager: TADS decoder: illegal state exception: " + e.getMessage());
            return "";
        } catch (CharacterCodingException e) {
            GLKLogger.error("GLKCharsetManager: TADS decoder: character coding exception: " + e.getMessage());
            return "";
        }

        src.rewind();  // in case anything else wants to read from the buffer after this call completes
        return (ret != null ? ret.toString() : "");
    }

    /**
     * Read a standard GLK string from a byte buffer.
     *
     * @param src        - the source byte buffer to read the string from. The limit of this buffer should be set
     *                   to one byte past the last readable character.
     *
     * @param srcIs32Bit - if TRUE the source buffer has 32bit elements, if FALSE it has 8 bit elements.
     *
     * @return the decoded string, or "" if there was an error.
     */
    @NonNull
    public String getGLKString(@NonNull ByteBuffer src, boolean srcIs32Bit) {
        CharBuffer ret = null;

        if (!src.hasRemaining()) {
            return "";
        }

        try {
            if (srcIs32Bit) {
                if (mDecoderUTF32 != null) {
                    ret = mDecoderUTF32.decode(src);
                }
            } else {
                if (mDecoderISO_8859_1 != null) {
                    ret = mDecoderISO_8859_1.decode(src);
                }
            }
        } catch (MalformedInputException e) {
            GLKLogger.warn("GLKCharsetManager: decoder: couldn't decode text: malformed input: " + e.getMessage());
            if (srcIs32Bit && mReverseByteOrderIfError) {
                Charset cs = mDecoderUTF32.charset();
                String charsetName = cs.name();
                GLKLogger.warn("GLKCharsetManager: UTF32 decoder is currently '" + charsetName + "': will retry with reversed byte order");
                mDecoderUTF32 = getDecoder(cs.name().equals("UTF-32BE") ? "UTF-32LE" : "UTF-32BE");
                if (mDecoderUTF32 != null) {
                    src.rewind();
                    try {
                        ret = mDecoderUTF32.decode(src);
                    } catch (@NonNull IllegalStateException | CharacterCodingException e2) {
                        GLKLogger.error("GLKCharsetManager: decoder: reversed byte order ('" +
                                mDecoderUTF32.charset() + "') didn't work - giving up.");
                        return "";
                    }
                }
            }
        } catch (IllegalStateException e) {
            GLKLogger.error("GLKCharsetManager: decoder: illegal state exception: " + e.getMessage());
            return "";
        } catch (CharacterCodingException e) {
            GLKLogger.error("GLKCharsetManager: decoder: character coding exception: " + e.getMessage());
            return "";
        }

        src.rewind();  // in case anything else wants to read from the buffer after this call completes
        return (ret != null ? ret.toString() : "");
    }

    /**
     * Write a TADS string to a byte buffer.
     *
     * @param src - the string to write.
     *
     * @param isTads3 - whether we are writing a TADS 2 string or a TADS 3 string. This determines which encoding to use
     *                when writing to the byte buffer - TADS 3 games always use UTF-8, but TADS 2 games can use anything.
     *                Therefore for TADS 2 games, we rely on the user's specified encoding, only falling back to UTF-8
     *                if that setting is invalid.
     *
     * @return a new byte buffer containing the encoded string, or NULL if there was an error.
     */
    @Nullable
    public ByteBuffer putTADSString(@NonNull String src, boolean isTads3) {
        // Returns a new bytebuffer containing byte encoding of String src,
        // or NULL if there is an error
        // TADS 3 text is always UTF-8
        // TADS 2 text can be anything - we depend entirely on what the user has told us (or assume UTF-8 if
        // the user has specified an invalid encoding)
        CharsetEncoder encoder = (isTads3) ? mEncoderUTF8 : mEncoderTADS2;
        if (encoder == null) {
            return null;
        }

        ByteBuffer ret = null;
        try {
            ret = encoder.encode(CharBuffer.wrap(src));
        } catch (IllegalStateException e) {
            GLKLogger.error("GLKCharsetManager: TADS encoder: illegal state exception: " + e.getMessage());
        } catch (CharacterCodingException e) {
            GLKLogger.error("GLKCharsetManager: TADS encoder: character coding exception: " + e.getMessage());
        }
        return ret;
    }

    /**
     * Writes a standard GLK string to a byte buffer.
     *
     * @param src         - the string to write.
     *
     * @param dest        - the byte buffer to write into. The limit of this buffer should be
     *                    set to the buffer's capacity.
     *
     * @param destIs32Bit - if TRUE the destination buffer has 32bit elements, if FALSE it has 8 bit elements.
     *
     * @param destIsText  - if TRUE the destination buffer is encoded text, if FALSE it is binary.
     *
     * @return the number of elements copied into dest.
     */
    public int putGLKString(@NonNull String src,
                            @NonNull ByteBuffer dest, boolean destIs32Bit, boolean destIsText) {
        int destPosOld = dest.position();

        try {
            // Create the appropriate encoder
            CharsetEncoder encoder;
            if (destIs32Bit) {
                if (destIsText) {
                    encoder = mEncoderUTF8;
                } else {
                    encoder = mEncoderUTF32;
                }
            } else {
                encoder = mEncoderISO_8859_1;
            }

            // Encode the source string into a byte buffer
            if (encoder != null) {
                ByteBuffer srcBytes = encoder.encode(CharBuffer.wrap(src));
                srcBytes.limit(Math.min(dest.remaining(), srcBytes.limit()));
                dest.put(srcBytes);
            }
        } catch (BufferOverflowException e) {
            GLKLogger.error("GLKCharsetManager: encoder: no more space to write: " + e.getMessage());
        } catch (BufferUnderflowException e) {
            GLKLogger.error("GLKCharsetManager: encoder: no more chars to read: " + e.getMessage());
        } catch (IllegalStateException e) {
            GLKLogger.error("GLKCharsetManager: encoder: illegal state exception: " + e.getMessage());
        } catch (CharacterCodingException e) {
            GLKLogger.error("GLKCharsetManager: encoder: character coding exception: " + e.getMessage());
        }

        // Work out how many codepoints were copied into the dest buffer
        int len = dest.position() - destPosOld;
        if (destIs32Bit) {
            len /= 4;
        }
        return len;
    }

    public enum ENDIAN {
        BIG_FIXED, LITTLE_FIXED, BIG_REVERSE_IF_ERROR, LITTLE_REVERSE_IF_ERROR
    }

}
