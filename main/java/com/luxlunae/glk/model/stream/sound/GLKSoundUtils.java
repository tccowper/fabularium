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
package com.luxlunae.glk.model.stream.sound;

import android.support.annotation.NonNull;

import com.luxlunae.glk.GLKLogger;
import com.luxlunae.glk.GLKUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class GLKSoundUtils {

    private final static boolean DEBUG_SND_UTILS = false;

    // AIFF constants
    private static final int FORM = GLKUtils.ubyteToID32('F', 'O', 'R', 'M');
    private static final int AIFC = GLKUtils.ubyteToID32('A', 'I', 'F', 'C');
    private static final int AIFF = GLKUtils.ubyteToID32('A', 'I', 'F', 'F');
    private static final int COMM = GLKUtils.ubyteToID32('C', 'O', 'M', 'M');
    private static final int SSND = GLKUtils.ubyteToID32('S', 'S', 'N', 'D');

    // WAV constants
    private static final int RIFF = GLKUtils.ubyteToID32('R', 'I', 'F', 'F');
    private static final int WAVE = GLKUtils.ubyteToID32('W', 'A', 'V', 'E');
    private static final int FMT = GLKUtils.ubyteToID32('f', 'm', 't', ' ');
    private static final int DATA = GLKUtils.ubyteToID32('d', 'a', 't', 'a');
    private static final short WAVE_FORMAT_PCM = 0x0001;

    static void AIFF2WAV(@NonNull DataInputStream aiff, @NonNull DataOutputStream wav) throws IOException {
        // AIFF file format is specified at
        //		http://eblong.com/zarf/ftp/aiff-c.9.26.91.ps
        // WAVE file format is specified at
        //		https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
        //      http://www-mmsp.ece.mcgill.ca/Documents/AudioFormats/WAVE/WAVE.html

        //====================
        // READ THE AIFF FILE
        //====================

        // (1) Byte order in AIFF files is big-endian, the same as a DataInputStream object
        //
        // (2) The ID data type is a 32 bit concatenation of four printable ASCII characters in
        //     range 0x20 - 0x7E. Upper / lowercase is significant, that is, IDs are compared
        //     using a simple 32-bit equality check.

        // ------------------------------------------
        // 1. Read the header of the container chunk
        // ------------------------------------------

        // [ID, 4 bytes]: Should always be 'FORM'. Indicates this is a FORM chunk.
        final int aifFileID = aiff.readInt();
        if (aifFileID != FORM) {
            GLKLogger.error("Error: AIFF file does not have correct container chunk ID: '" + GLKUtils.id32ToString(aifFileID) + "'");
            throw new IOException();
        }

        // [Signed 4 bytes]: Size of the data portion of the FORM chunk. Note that the data
        // portion has been broken into two parts, formType and chunks[].
        final int aifFileDataSize = aiff.readInt() - 4;     // don't include the 4 bytes taken by formType

        // [ID, 4 bytes]: Describes what's in the form chunk.
        //   For AIFF-C, this should be 'AIFC'.
        //   For AIFF, this should be 'AIFF'.
        final int aifFileFormType = aiff.readInt();
        boolean aifFileCompressed;
        if (aifFileFormType == AIFC) {
            aifFileCompressed = true;
        } else if (aifFileFormType == AIFF) {
            aifFileCompressed = false;
        } else {
            GLKLogger.error("Error: AIFF file does not have correct container form type: '" + GLKUtils.id32ToString(aifFileFormType) + "'");
            throw new IOException();
        }

        // ------------------------------------------
        // 2. Read the chunks in the container chunk
        // ------------------------------------------

        // Try to find and process the Common Chunk and the Sound Data Chunk
        int len = 0;
        boolean gotCommon = false;
        int numChannels = 0;            // number of channels: 1 = mono, 2 = stereo, 4 = four-channel sound, etc.
        long numSampleFrames = 0;       // unsigned int - number of sample frames in the sound data chunk. For uncompressed sound data, total sound points = numSampleFrames * numChannels
        int sampleSizeInBits = 0;       // number of bits in each sample point of uncompressed data. Any number from 1 to 32.
        float sampleRate = 0;           // 80 bit IEEE Standard 754 floating point number, sample rate at which sound is to be played back, in sample frames per second
        boolean gotSndData = false;
        long offset;
        long blockSize;
        byte[] soundData = null;
        int ckID;
        int ckDataSize;
        int sndDataSize;
        long timeStamp = 0;

        while (len < aifFileDataSize) {
            // [ID, 4 bytes]: Chunk ID.
            ckID = aiff.readInt();

            // [Signed 4 bytes]: Chunk data size.
            ckDataSize = aiff.readInt();

            if (ckID == COMM) {
                // ------------
                // COMMON CHUNK
                // ------------
                // This is required - one and only one common chunk must be in each AIFF / AIFC.

                // [Signed 2 bytes]: Number of audio channels for the sound
                // For multi-channel sounds, single sample points from each
                // channel are interleaved. A set of interleaved sample
                // points is called a sample frame. For monophonic sound,
                // a sample frame is a single sample point.
                numChannels = aiff.readShort();

                // [Unsigned 4 bytes]: The number of sample frames in the Sound Data Chunk.
                // Note that this is not the number of bytes nor the number of sample points
                // in the Sound Data Chunk. For uncompressed sound data, the total number of
                // sample points in the file is numSampleFrames * numChannels.
                numSampleFrames = (long) aiff.readInt();
                numSampleFrames &= 0X00000000FFFFFFFFL;  // zero 4 high order bytes

                // [Signed 2 bytes]: Sample size is the number of bits in each sample of
                // uncompressed sound data. It can be any number from 1 to 32. For compressed
                // sound data, sampleSize indicates the number of bits in each sample before
                // compression.
                sampleSizeInBits = aiff.readShort();
                if (sampleSizeInBits < 1 || sampleSizeInBits > 32) {
                    GLKLogger.error("Error: AIFF has incorrect value for sample size (should be 1 <= val <= 32): " + sampleSizeInBits);
                    throw new IOException();
                }

                // [Signed 10 bytes]: The sample rate at which the sound is to be played back,
                // in sample frames per second.
                sampleRate = (float) GLKUtils.read80float(aiff);

                if (aifFileCompressed) {
                    GLKLogger.error("TODO: Currrently don't support compressed AIFF files.");
                    // These extra fields only appear in compressed AIFF files

                    // [ID, 4 bytes]: Compression type is used by programs to identify the compression algorithm, if any, used
                    // on the sound data. [pstring]: Compression name is used by people to identify the compression algorithm.
                    // Use compressionType to select the decompression routine. Values for compressionType are: 'NONE', 'ACE2',
                    // 'ACE8', 'MAC3' and 'MAC6'.
                    //compressionType = (int)read32(ck.ckData, 16);
                    //Logger.debug("Compression type = '" + id32ToString(compressionType) + "'");
                    //lenCompressionName = read8(common.ckData, 20);
                }

                gotCommon = true;
            } else if (ckID == SSND) {
                // ----------------
                // SOUND DATA CHUNK
                // ----------------
                // The sound data chunk contains the actual sample frames.

                // [Unsigned 4 bytes]: offset determines where the first sample frame in the soundData begins.
                // offset is in bytes. Most applications won't use offset and should set it to 0.
                offset = (long) aiff.readInt();
                offset &= 0X00000000FFFFFFFFL;  // zero 4 high order bytes
                if (offset != 0) {
                    GLKLogger.error("TODO: Handle non-zero sound data offset in AIFF files.");
                    throw new IOException();
                }

                // [Unsigned 4 bytes]: blockSize is used in conjunction with offset for block-aligning sound data.
                // It contains the size in bytes of the blocks the sound data is aligned to. As with offset, most
                // applications won't use blockSize and should set it to 0.
                blockSize = (long) aiff.readInt();
                blockSize &= 0X00000000FFFFFFFFL;  // zero 4 high order bytes
                if (blockSize != 0) {
                    GLKLogger.error("TODO: Handle non-zero sound data block sizes in AIFF files.");
                    throw new IOException();
                }

                // [Array of signed bytes]: soundData contains the sample frames that make up the sound. The number
                // of sample frames in the sound data is determined by the numSampleFrames parameter in the
                // Common Chunk. If soundData[] contains an odd number of bytes, a zero pad byte is added at the
                // end (but not used for playback).
                sndDataSize = ckDataSize - 8;  // ckDataSize includes 8 bytes for offset and blocksize
                soundData = new byte[sndDataSize];
                if (aiff.read(soundData) != sndDataSize) {
                    throw new IOException();
                }

                gotSndData = true;
            } else {
                // We ignore all other chunks (if any):
                //   FVER, MARK, COMT, INST, MIDI, AESD, APPL
                if (DEBUG_SND_UTILS) {
                    GLKLogger.warn("AIFF2WAV: Skipping chunk.");
                }
                aiff.skipBytes(ckDataSize);
            }

            if (ckDataSize % 2 != 0) {
                // if the chunk has an odd number of bytes, there
                // is also a padding byte - skip it
                aiff.skipBytes(1);
            }
            len += (ckDataSize + 8 + 2);
        }

        // Must be one and only one common chunk in the aiff file
        if (!gotCommon) {
            GLKLogger.error("Error: AIFF file does not contain a Common Chunk!");
            throw new IOException();
        }

        // Max of one sound data chunk in the aiff file
        if (!gotSndData) {
            GLKLogger.error("Error: AIFF file does not contain a Sound Data Chunk!");
            throw new IOException();
        }

        //==================================
        // WRITE OUT THE WAV FILE (PCM DATA)
        //==================================

        // WAV is little-endian (just to make our lives more interesting).
        // So we need to swap the byte order as we write it out
        final int M = sampleSizeInBits / 8;     // Each sample is M bytes long
        final long Ns = numSampleFrames;        // Total number of blocks
        final short Nc = (short) numChannels;    // Each block consists of Nc samples
        final int F = (int) sampleRate;          // Sampling rate in blocks per second

        final int dataLength = (int) (M * Nc * Ns);
        final int paddingByte = (dataLength % 2 != 0) ? 1 : 0;

        // Header
        wav.writeInt(RIFF);
        wav.writeInt(GLKUtils.swapEndian32(4 + 24 + 8 + dataLength + paddingByte));
        wav.writeInt(WAVE);

        // Format chunk
        wav.writeInt(FMT);                                                // chunk ID
        wav.writeInt(GLKUtils.swapEndian32(16));                        // chunk size
        wav.writeShort(GLKUtils.swapEndian16(WAVE_FORMAT_PCM));           // wFormatTag
        wav.writeShort(GLKUtils.swapEndian16(Nc));                        // nChannels
        wav.writeInt(GLKUtils.swapEndian32(F));                           // nSamplesPerSec
        wav.writeInt(GLKUtils.swapEndian32(F * M * Nc));                    // nAvgBytesPerSec
        wav.writeShort(GLKUtils.swapEndian16((short) (M * Nc)));            // nBlockAlign
        wav.writeShort(GLKUtils.swapEndian16((short) sampleSizeInBits));  // wBitsPerSample (rounds up to 8*M)

        // Data chunk
        wav.writeInt(DATA);
        wav.writeInt(GLKUtils.swapEndian32(dataLength));
        wav.write(soundData);
        if (paddingByte == 1) {
            wav.writeByte(0);
        }

        if (DEBUG_SND_UTILS) {
            // What did we find?
            GLKLogger.debug("AIFF2WAV: Time stamp = " + timeStamp);
            GLKLogger.debug("AIFF2WAV: NumChannels = " + Nc);
            GLKLogger.debug("AIFF2WAV: Sample Rate = " + F);
            GLKLogger.debug("AIFF2WAV: Byte Rate = " + F * M * Nc);
            GLKLogger.debug("AIFF2WAV: Block Align = " + M * Nc);
            GLKLogger.debug("AIFF2WAV: Bits per sample = " + sampleSizeInBits);
            GLKLogger.debug("AIFF2WAV: Number of bytes of data = " + dataLength);
            GLKLogger.debug("AIFF2WAV: Actual size of data array = " + soundData.length);
        }
    }
}

