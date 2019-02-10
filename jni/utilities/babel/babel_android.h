/*
 * Copyright (C) 2017 Tim Cadogan-Cowper.
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
#include <jni.h>

#ifndef _Included_fabularium_GameSelector
#define _Included_fabularium_GameSelector
#ifdef __cplusplus
extern "C" {
#endif

/* These functions create and destroy an opaque context object used by
 * the babel handler. Each context object describes one currently loaded
 * file. If you wish to process files in parallel (as a multithreaded
 * application might), you should use a separate context object for each
 * file (that is, each thread. On the other hand, if you want to have
 * separate threads handle, say, the cover art and metadata on the *same*
 * file, they should use the same context object). */
JNIEXPORT jstring JNICALL Java_com_luxlunae_fabularium_IFictionRecord_nBabelInit
  (JNIEnv *, jobject, jstring);

/* Calls the proper treaty module for the given selector (the story file and
 * story file extent are ommitted, as these are inferred from the babel handler
 * context).  This function will correctly and invisibly handle container
 * formats. */
JNIEXPORT jint JNICALL Java_com_luxlunae_fabularium_IFictionRecord_nBabelTreaty
  (JNIEnv *, jobject, jint, jobject);

JNIEXPORT jboolean JNICALL Java_com_luxlunae_fabularium_IFictionRecord_nBabelGetAuthoritative
  (JNIEnv *, jobject);

JNIEXPORT void JNICALL Java_com_luxlunae_fabularium_IFictionRecord_nBabelRelease
  (JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif
