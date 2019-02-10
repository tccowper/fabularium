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

import android.media.MediaPlayer;
import android.support.annotation.NonNull;

import com.luxlunae.glk.model.stream.GLKStream;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

// Represents one sound channel.
// A channel can be playing exactly one sound at a time. If you want to play
// more than one sound simultaneously, you need more than one sound channel.
// On the other hand, a single sound can be played on several channels at the
// same time, or overlapping itself.
public class GLKSoundStream extends MediaPlayer implements GLKStream {
    @NonNull
    private final Random mRandGen = new Random();
    private int mRock;
    private int mStreamId;
    private int mRandom = 100;  // the probability a sound will play on this channel, from 0 to 100

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

    @Override
    public void close() {
        if (isPlaying()) {
            stop();
        }
        setOnCompletionListener(null);
        release();
    }

    @Override
    public void start() {
        // Work out if we should play
        if (mRandGen.nextInt(100) <= mRandom) {
            super.start();
        }
    }

    public void setProbability(int random) {
        mRandom = random;
        if (mRandom < 0) {
            mRandom = 0;
        } else if (mRandom > 100) {
            mRandom = 100;
        }
    }

    /**
     * Number of times the sound should be repeated. A repeat
     * value of -1 (or rather 0xFFFFFFFF) means that the sound should
     * repeat forever. A repeat value of 0 means that the sound will not be played
     * at all; nothing happens.
     *
     * @param repeats - number of times to repeat the sound.
     */
    public void setRepeating(final int repeats) {
        setOnCompletionListener(null);
        if (repeats > 1) {
            setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                int n = 0;

                @Override
                public void onCompletion(@NonNull MediaPlayer mp) {
                    if (n++ < repeats) {
                        mp.start();
                    }
                }
            });
        } else if (repeats == -1) {
            setLooping(true);
        }
    }

    public void setDataSource(@NonNull byte[] audioData, @NonNull String audioType) throws IOException {
        String ext;
        switch (audioType) {
            case "FORM":
                //  An AIFF (Audio IFF) file has chunk type 'FORM', and formtype 'AIFF'.
                //  AIFF is an uncompressed digital-sample format developed in the late 1980s. The AIFF format is available at these locations:
                //		http://www.digitalpreservation.gov/formats/fdd/fdd000005.shtml
                //		http://eblong.com/zarf/ftp/aiff-c.9.26.91.ps
                ext = ".aif";
                break;
            case "OGGV":
                // An Ogg Vorbis file has chunk type 'OGGV'. This is a high-quality (but lossy) audio compression format,
                // comparable to MP3 (but without the patent concerns that encumber MP3). The Ogg format is available at:
                ext = ".ogg";
                break;
            case "MOD":
                // MOD is an Amiga-originated format for music synthesized from note samples. Over the years, other formats of this
                // type -- generally called "tracker" or "module music" formats -- have arisen. Blorb supports four: original ".MOD" files,
                // ImpulseTracker (".IT"), FastTracker 2 Extended (".XM"), and ScreamTracker 3 (".S3M").
                //
                // Because tracker-playing libraries typically handle many formats, it is most practical for Blorb to lump them all together.
                // Regardless of which tracker format is used, the chunk type will be 'MOD '.
                // The formats are described here:
                //		http://www.digitalpreservation.gov/formats/fdd/fdd000126.shtml
                ext = ".mod";
                break;
            default:
                //Logger.warn("glk_schannel_play_ext: unrecognised sound format: '" + audioType + "'");
                //return;
                ext = "." + audioType.toLowerCase();
                break;
        }

        final File tmpSound = File.createTempFile("glksnd", ext);
        DataOutputStream out = new DataOutputStream(new FileOutputStream(tmpSound));

        if (audioType.equals("FORM")) {
            // Convert AIFF files to WAV as Android doesn't support the former
            // WAVE file format is specified at
            //		https://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(audioData));
            GLKSoundUtils.AIFF2WAV(in, out);
        }
        out.write(audioData);
        out.close();
        FileInputStream fis = new FileInputStream(tmpSound);

        // Get the media player ready
        reset();
        setDataSource(fis.getFD());
    }
}
