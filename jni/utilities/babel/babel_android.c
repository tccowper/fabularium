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

/*  Entry point for Fabularium - mirrors the babel_handler.c
 * 
 * char *babel_init(char *filename)
 *      Initializes babel to use the specified file. MUST be called before
 *      babel_treaty.  Returns the human-readable name of the format
 *      or NULL if the format is not known. Do not call babel_treaty unless
 *      babel_init returned a nonzero value.
 *      The returned string will be the name of a babel format, possibly
 *      prefixed by "blorbed " to indicate that babel will process this file
 *      as a blorb.
 * int32 babel_treaty(int32 selector, void *output, void *output_extent)
 *      Dispatches the call to the treaty handler for the currently loaded
 *      file.
 *      When processing a blorb, all treaty calls will be deflected to the
 *      special blorb handler.  For the case of GET_STORY_FILE_IFID_SEL,
 *      The treaty handler for the underlying format will be called if an
 *      IFID is not found in the blorb resources.
 * void babel_release()
 *      Frees all resources allocated during babel_init.
 *      You should do this even if babel_init returned NULL.
 *      After this is called, do not call babel_treaty until after
 *      another successful call to babel_init.
 * char *babel_get_format()
 *      Returns the same value as the last call to babel_init (ie, the format name)
 * int32 babel_md5_ifid(char *buffer, int extent);
 *      Generates an MD5 IFID from the loaded story.  Returns zero if something
 *      went seriously wrong. 
 */
#include <string.h>
#include "treaty.h"
#include "babel_handler.h"
#include "babel_android.h"

JNIEXPORT jstring JNICALL Java_com_luxlunae_fabularium_play_IFictionRecord_nBabelInit
  (JNIEnv * e, jobject jc, jstring sf) {
      char * tmp = (char *)((*e)->GetStringUTFChars(e, sf, NULL));
      const char * r1 = babel_init(tmp);
      (*e)->ReleaseStringUTFChars(e, sf, tmp);
      return (*e)->NewStringUTF(e, r1);
}

JNIEXPORT jint JNICALL Java_com_luxlunae_fabularium_play_IFictionRecord_nBabelTreaty
  (JNIEnv * e, jobject jc, jint sel, jobject output) {
  if (output != NULL) {
      void* buf = (*e)->GetDirectBufferAddress(e, output);
      jlong output_extent = (jlong)((*e)->GetDirectBufferCapacity(e, output));
      return (jint)babel_treaty((int32)sel, buf, (int32)output_extent);
  } else {
      return (jint)babel_treaty((int32)sel, NULL, 0);
  }
}

JNIEXPORT jboolean JNICALL Java_com_luxlunae_fabularium_play_IFictionRecord_nBabelGetAuthoritative
  (JNIEnv * e, jobject jc) {
  int32 ret = babel_get_authoritative();
  return (ret != 0) ? JNI_TRUE : JNI_FALSE; 
}

JNIEXPORT jboolean JNICALL Java_com_luxlunae_fabularium_play_IFictionRecord_nBabelGenerateIFID
  (JNIEnv * e, jobject jc, jobject buffer) {
  int32 ret = 0;
  if (buffer != NULL) {
      char* buf = (char *)(*e)->GetDirectBufferAddress(e, buffer);
      int32 buf_extent = (int32)((*e)->GetDirectBufferCapacity(e, buffer));
      ret = babel_md5_ifid(buf, buf_extent);
  }
  /* error */
  return (ret != 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL Java_com_luxlunae_fabularium_play_IFictionRecord_nBabelRelease
  (JNIEnv * e, jobject jc) {
      babel_release();
}


