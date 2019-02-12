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
package com.luxlunae.glk.model.stream;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.luxlunae.glk.model.stream.window.GLKNonPairM;

import java.nio.ByteBuffer;

public interface GLKOutputStream extends GLKStream {
    long getWriteCount();

    void putString(@NonNull String s);

    void putChar(int c);

    void putBuffer(@NonNull ByteBuffer buffer, boolean unicode);

    void setStyle(int s);

    void startHyperlink(int linkval);

    void endHyperlink();

    void setEchoer(@Nullable GLKNonPairM w);
}
