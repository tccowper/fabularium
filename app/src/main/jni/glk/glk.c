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
 *
 * Portions of the date and time routines below, as marked, are
 * taken from cgdate.c of Andrew Plotkin's cheapglk code (see
 * https://github.com/erkyrath/cheapglk), subject to the following licence:
 *
 *  The MIT License
 *
 *  Copyright (c) 1998-2016, Andrew Plotkin
 * 
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 * 
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *  
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <setjmp.h>
#include <android/log.h>

#include "glk.h"

/* Below global variables are all set dynamically when
 * the startup code is called in glkstart.c */
JNIEnv * env;
jmp_buf term_buf;

jclass GLKModel_class;
jobject GLKModel_obj;
jclass GLKController_class;

jclass GLKEvent_class;
jobject GLKEvent_obj;
jfieldID ev_type;
jfieldID ev_win;
jfieldID ev_val1;
jfieldID ev_val2;

jclass Point_class;
jobject Point_obj;
jfieldID pt_x;
jfieldID pt_y;

jclass TADSBannerInfo_class;
jobject TADSBannerInfo_obj;
jfieldID ban_align;
jfieldID ban_style;
jfieldID ban_rows;
jfieldID ban_columns;
jfieldID ban_pix_width;
jfieldID ban_pix_height;
jfieldID ban_os_line_wrap;

#define MID(fname) \
 jmethodID m_ ##fname;

  MID(glk_window_iterate);
  MID(glk_window_open);
  MID(glk_window_close);
  MID(glk_window_get_size);
  MID(glk_window_set_arrangement);
  MID(glk_window_get_arrangement);
  MID(glk_window_get_parent);
  MID(glk_window_get_root);
  MID(glk_window_clear);
  MID(glk_window_move_cursor);
  MID(glk_window_get_stream);
  MID(glk_window_fill_rect);
  MID(glk_window_set_echo_stream);
  MID(glk_set_window);
  MID(glk_exit);
  MID(glk_select);
  MID(glk_tick);
  MID(glk_set_terminators_line_event);
  MID(glk_image_get_info);
  MID(glk_image_draw_scaled);
  MID(glk_image_draw);
  MID(glk_stream_set_current);
  MID(glk_stream_get_current);
  MID(glk_put_string);
  MID(glk_put_buffer);
  MID(glk_put_char);
  MID(glk_stream_get_position);
  MID(glk_stream_set_position);
  MID(glk_get_buffer_stream);
  MID(glk_schannel_create);
  MID(glk_schannel_destroy);
  MID(glk_schannel_play);
  MID(glk_schannel_set_volume);
  MID(glk_schannel_play_ext);
  MID(glk_schannel_play_multi);
  MID(glk_schannel_stop);
  MID(glk_put_buffer_stream);
  MID(glk_put_char_stream);
  MID(glk_get_char_stream);
  MID(glk_put_string_stream);
  MID(glk_fileref_create_by_prompt);
  MID(glk_fileref_create_by_name);
  MID(glk_fileref_does_file_exist);
  MID(glk_fileref_destroy);
  MID(glk_stream_iterate);
  MID(glk_fileref_iterate);
  MID(glk_schannel_iterate);
  MID(glk_stream_open_file);
  MID(glk_stream_open_memory);
  MID(glk_stream_close);
  MID(glk_set_style);
  MID(glk_set_style_stream);
  MID(glk_request_timer_events);
  MID(glk_stylehint_set);
  MID(glk_request_char_event);
  MID(glk_cancel_char_event);
  MID(glk_request_line_event);
  MID(glk_cancel_line_event);
  MID(glk_gestalt);
  MID(glk_gestalt_ext);
  MID(glk_current_time);
  MID(glk_current_simple_time);
  MID(glk_time_to_date_utc);
  MID(glk_time_to_date_local);
  MID(glk_simple_time_to_date_utc);
  MID(glk_simple_time_to_date_local);
  MID(glk_date_to_time_utc);
  MID(glk_date_to_time_local);
  MID(glk_date_to_simple_time_utc);
  MID(glk_date_to_simple_time_local);
  MID(glk_buffer_to_lower_case_uni);
  MID(glk_buffer_to_upper_case_uni);
  MID(glk_buffer_to_title_case_uni);
  MID(glk_buffer_canon_decompose_uni);
  MID(glk_buffer_canon_normalize_uni);
  MID(glk_char_to_lower);
  MID(glk_char_to_upper);
  MID(glk_window_get_rock);
  MID(glk_window_get_type);
  MID(glk_window_get_echo_stream);
  MID(glk_window_get_sibling);
  MID(glk_stream_get_rock);
  MID(glk_fileref_create_temp);
  MID(glk_fileref_get_rock);
  MID(glk_fileref_delete_file);
  MID(glk_fileref_create_from_fileref);
  MID(glk_get_line_stream);
  MID(glk_stylehint_clear);
  MID(glk_style_distinguish);
  MID(glk_style_measure);
  MID(glk_select_poll);
  MID(glk_request_mouse_event);
  MID(glk_cancel_mouse_event);
  MID(glk_window_flow_break);
  MID(glk_window_erase_rect);
  MID(glk_window_set_background_color);
  MID(glk_schannel_get_rock);
  MID(glk_sound_load_hint);
  MID(glk_schannel_create_ext);
  MID(glk_schannel_set_volume_ext);
  MID(glk_schannel_pause);
  MID(glk_schannel_unpause);
  MID(glk_set_hyperlink);
  MID(glk_set_hyperlink_stream);
  MID(glk_request_hyperlink_event);
  MID(glk_cancel_hyperlink_event);
  MID(glk_set_echo_line_event);
  MID(glk_stream_open_resource);

  MID(glkplus_stream_open_pathname);
  MID(glkplus_fileref_get_name);
  MID(glkplus_set_zcolors);
  MID(glkplus_set_zcolors_stream);
  MID(glkplus_set_reversevideo);
  MID(glkplus_set_reversevideo_stream);
  MID(glkplus_set_html_mode);
  MID(glkplus_new_html_page);

  MID(tads_get_input);
  MID(tads_put_string);
  MID(tads_put_string_stream);
  MID(tadsban_create);
  MID(tadsban_get_charwidth);
  MID(tadsban_get_charheight);
  MID(tadsban_getinfo);
  MID(tadsban_delete);
  MID(tadsban_orphan);
  MID(tadsban_clear);
  MID(tadsban_set_attr);
  MID(tadsban_set_color);
  MID(tadsban_set_screen_color);
  MID(tadsban_flush);
  MID(tadsban_set_size);
  MID(tadsban_size_to_contents);
  MID(tadsban_start_html);
  MID(tadsban_end_html);
  MID(tadsban_goto);

#undef MID

/*****************************************************************************/
/* HELPER FUNCTIONS, TO SHOW FATAL ERRORS AND MANAGE LONG-LIVED BYTE BUFFERS */
/*****************************************************************************/

jobject* globalBBRefs;
int nBBRefs = 0;
int MAX_BB_REFS;

void fatalError (const char * s) {
   /* Display a fatal error string and shutdown */
   char tmp[300];
   sprintf(tmp, "*** fatal error: %s ***\n", s);
   glk_put_string(tmp);
   printf("%s", tmp);
   longjmp(term_buf, 4);
}

void initByteBuffers(int max_refs) {
  MAX_BB_REFS = (int)max_refs;
  globalBBRefs = calloc(MAX_BB_REFS, sizeof(jobject));
}

void freeByteBuffers() {
  if (globalBBRefs == NULL) {
    /* nothing to free */
    return;
  }

  printf("freeing remaining byte buffers...\n");

  int tot = nBBRefs;
  for (int i = 0; i < MAX_BB_REFS && nBBRefs > 0; i++) {
    if (globalBBRefs[i] != NULL) {
       (*env)->DeleteGlobalRef(env, globalBBRefs[i]);
       globalBBRefs[i] = NULL;
       printf("  freed byte buffer %d of %d...\n", (i + 1), tot);
       nBBRefs--;
    }
  }

  if (nBBRefs != 0) {
    printf("MEMORY LEAK DETECTED: # byte buffer refs after JNI destruction (should be 0): %d\n", nBBRefs);
  }

  free(globalBBRefs);
  globalBBRefs = NULL;
}

/*
 * Return a long-lived ByteBuffer that wraps the memory beginning
 * at 'address' and continuing for 'capacity' bytes.
 * This ByteBuffer will remain valid (and can be safely passed back to
 * Java) until you free it through a call to unwrapByteBuffer.
 *
 * TODO: We have a problem here if the terp ever calls realloc
 * (e.g. git/glulxe @setmemsize) as this byte buffer may no longer
 * be pointing to the correct area of memory. How can we update it?
 * Or should we just disallow those opcodes in git / glulxe?
 */
jobject wrapByteBuffer(void* address, jlong capacity) {
  if (globalBBRefs == NULL) {
    printf("glk.c: wrapByteBuffer: global byte buffer array not initialised.\n");
    return NULL;
  }

  if (capacity <= 0) {
    printf("glk.c: wrapByteBuffer: called with capacity <= 0.\n");
    return NULL;
  }

  /* Find the first vacant slot */
  int i = 0;
  for (i = 0; i < MAX_BB_REFS; i++) {
    if (globalBBRefs[i] == NULL) {
      break;
    }
  }

  if (i == MAX_BB_REFS) {
    fatalError("glk.c: wrapByteBuffer: Insufficient memory to allocate another byte buffer reference...");
    return NULL;
  }

  /* Wrap the byte buffer and stash it */
  jobject localRef = (*env)->NewDirectByteBuffer(env, address, capacity);
  if (!localRef) {
    printf("glk.c: wrapByteBuffer: call to NewDirectByteBuffer failed.\n");
    return NULL;
  }
  jobject globalRef = (*env)->NewGlobalRef(env, localRef);
  if (!globalRef) {
    printf("glk.c: wrapByteBuffer: call to NewGlobalRef failed.\n");
    return NULL;
  }
  (*env)->DeleteLocalRef(env, localRef);
  globalBBRefs[i] = globalRef;

  /* Update our counter */  
  nBBRefs++;

  return globalRef;
}

/*
 * Frees the memory taken up by a ByteBuffer 'bb', previously generated by a call
 * to wrapByteBuffer.
 */
void unwrapByteBuffer(JNIEnv * e, jobject bb) {
  if (globalBBRefs == NULL) {
     printf("glk.c: unwrapByteBuffer: global byte buffer array is NULL.\n");
     return;
  }

  /* Try to find this byte buffer object in our global cache */
  for (int i = 0; i < MAX_BB_REFS; i++) {
    if (globalBBRefs[i] != NULL) {
      if ((*e)->IsSameObject(e, bb, globalBBRefs[i]) == JNI_TRUE) {
          /* we received the object that we saved */
          (*env)->DeleteGlobalRef(e, globalBBRefs[i]);
          globalBBRefs[i] = NULL;
          nBBRefs--;
          break;
      }
    }
  }
}

/*****************************************************************************/
/*              FROM HERE ON, ALL THE STANDARD GLK CALLS                     */
/*****************************************************************************/

/*#define JNI_DEBUG*/

void glk_exit(void) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_exit");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_exit, GLKModel_obj);
  longjmp(term_buf, 1);
}

void glk_set_interrupt_handler(void (*func)(void)) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_interrupt_handler");
  #endif
  printf("glk.c: warning: glk_set_interrupt_handler is not yet supported.\n");
}

void glk_tick(void) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_tick");
  #endif
  /* Don't waste time calling the Java layer; this op code no longer does anything */
  /* (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_tick, GLKModel_obj); */
}

glui32 glk_gestalt(glui32 sel, glui32 val) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_gestalt");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticLongMethod(env, GLKController_class, m_glk_gestalt,
                                                       GLKModel_obj, (jlong) sel, (jlong) val);
  return ret;
}

glui32 glk_gestalt_ext(glui32 sel, glui32 val, glui32 *arr, glui32 arrlen) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_gestalt_ext");
  #endif
  jint c = (jint)arrlen;
  jlong tmp[c];
  int i;
  for (i = 0; i < c; i++) {
    tmp[i] = (jlong)arr[i];
  }
  jlongArray jarr = (*env)->NewLongArray(env, c);
  (*env)->SetLongArrayRegion(env, jarr, 0, c, tmp);
  glui32 ret = (glui32) (*env)->CallStaticLongMethod(env, GLKController_class, m_glk_gestalt_ext,
                                                       GLKModel_obj, (jlong) sel, (jlong) val,
                                                       jarr);
  (*env)->DeleteLocalRef(env, jarr);
  return ret;
}

unsigned char glk_char_to_lower(unsigned char ch) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_char_to_lower");
  #endif
  unsigned char ret = (unsigned char) (*env)->CallStaticCharMethod(env, GLKController_class,
                                                                     m_glk_char_to_lower,
                                                                     GLKModel_obj, (jchar) ch);
  return ret;
}

unsigned char glk_char_to_upper(unsigned char ch) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_char_to_upper");
  #endif
  unsigned char ret = (unsigned char) (*env)->CallStaticCharMethod(env, GLKController_class,
                                                                     m_glk_char_to_upper,
                                                                     GLKModel_obj, (jchar) ch);
  return ret;
}

winid_t glk_window_get_root(void) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_get_root");
  #endif
  winid_t ret = (winid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_window_get_root, GLKModel_obj);
  return ret;
}

winid_t glk_window_open(winid_t split, glui32 method, glui32 size, glui32 wintype, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_open");
  #endif
  winid_t ret = (winid_t) (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_window_open,
                                                        GLKModel_obj, (jint) split, (jint) method,
                                                        (jint) size, (jint) wintype, (jint) rock);
  return ret;
}

void glk_window_close(winid_t win, stream_result_t *result) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_close");
  #endif
  jlongArray res = (*env)->NewLongArray(env, 2);
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_close, GLKModel_obj,
                                 (jint) win, res);
  if (result != NULL) {
    jlong* r = (*env)->GetLongArrayElements(env, res, NULL);
    result->readcount = (glui32)r[0];
    result->writecount = (glui32)r[1];
    (*env)->ReleaseLongArrayElements(env, res, r, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, res);
}

void glk_window_get_size(winid_t win, glui32 *widthptr, glui32 *heightptr) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_get_size");
  #endif
  jobject obj = (*env)->CallStaticObjectMethod(env, GLKController_class, m_glk_window_get_size,
                                                 GLKModel_obj, (jint) win);
  if (widthptr != NULL) {
    *widthptr = (*env)->GetIntField(env, obj, pt_x);
  }
  if (heightptr != NULL) {
    *heightptr = (*env)->GetIntField(env, obj, pt_y);
  }
  (*env)->DeleteLocalRef(env, obj);
}

void glk_window_set_arrangement(winid_t win, glui32 method, glui32 size, winid_t keywin) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_arrangement");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_set_arrangement,
                                 GLKModel_obj, (jint) win, (jint) method, (jint) size,
                                 (jint) keywin);
}

void glk_window_get_arrangement(winid_t win, glui32 *methodptr, glui32 *sizeptr, winid_t *keywinptr) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_get_arrangement");
  #endif
  jintArray meth = (*env)->NewIntArray(env, 1);
  jintArray sz = (*env)->NewIntArray(env, 1);
  jintArray kw = (*env)->NewIntArray(env, 1);
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_get_arrangement,
                                 GLKModel_obj, (jint) win, meth, sz, kw);
  if (methodptr != NULL) {
    jint* m = (*env)->GetIntArrayElements(env, meth, NULL);
    *methodptr = (glui32)m[0];
    (*env)->ReleaseIntArrayElements(env, meth, m, JNI_ABORT);
  }
  if (sizeptr != NULL) {
    jint* s = (*env)->GetIntArrayElements(env, sz, NULL);
    *sizeptr = (glui32)s[0];
    (*env)->ReleaseIntArrayElements(env, sz, s, JNI_ABORT);
  }
  if (keywinptr != NULL) {
    jint* k = (*env)->GetIntArrayElements(env, kw, NULL);
    *keywinptr = (winid_t)k[0];
    (*env)->ReleaseIntArrayElements(env, kw, k, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, meth);
  (*env)->DeleteLocalRef(env, sz);
  (*env)->DeleteLocalRef(env, kw);
}

winid_t glk_window_iterate(winid_t win, glui32 *rockptr) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_iterate");
  #endif
  jintArray rock = (*env)->NewIntArray(env, 1);
  winid_t ret = (winid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_window_iterate, GLKModel_obj,
                                                        (jint) win, rock);
  if (rockptr != NULL) {
    jint* r = (*env)->GetIntArrayElements(env, rock, NULL);
    *rockptr = (glui32)r[0];
    (*env)->ReleaseIntArrayElements(env, rock, r, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, rock);
  return ret;
}

glui32 glk_window_get_rock(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_get_rock");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_window_get_rock, GLKModel_obj,
                                                      (jint) win);
  return ret;
}

glui32 glk_window_get_type(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_window_get_type");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_window_get_type, GLKModel_obj,
                                                      (jint) win);
  return ret;
}

winid_t glk_window_get_parent(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_get_parent");
  #endif
  winid_t ret = (winid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_window_get_parent, GLKModel_obj,
                                                        (jint) win);
  return ret;
}

winid_t glk_window_get_sibling(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_window_get_sibling");
  #endif
  winid_t ret = (winid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_window_get_sibling, GLKModel_obj,
                                                        (jint) win);
  return ret;
}

void glk_window_clear(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_clear");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_clear, GLKModel_obj,
                                 (jint) win);
}

void glk_window_move_cursor(winid_t win, glui32 xpos, glui32 ypos) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_move_cursor");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_move_cursor, GLKModel_obj,
                                 (jint) win, (jint) xpos, (jint) ypos);
}

strid_t glk_window_get_stream(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_window_get_stream");
  #endif
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_window_get_stream, GLKModel_obj,
                                                        (jint) win);
  return ret;
}

void glk_window_set_echo_stream(winid_t win, strid_t str) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_set_echo_stream");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_set_echo_stream,
                                 GLKModel_obj, (jint) win, (jint) str);
}

strid_t glk_window_get_echo_stream(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_get_echo_stream");
  #endif
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_window_get_echo_stream, GLKModel_obj,
                                                        (jint) win);
  return ret;
}

void glk_set_window(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_window");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_set_window, GLKModel_obj,
                                 (jint) win);
}

strid_t glk_stream_open_file(frefid_t fileref, glui32 fmode, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_open_file");
  #endif
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_open_file, GLKModel_obj,
                                                        (jint) fileref, (jint) fmode, (jint) rock,
                                                        JNI_FALSE);
  return ret;
}

strid_t glk_stream_open_memory(char* buf, glui32 buflen, glui32 fmode, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_open_memory");
  #endif
  /* Following reference should be freed from the Java end when the memory stream is closed: */
  jobject bb = (buf != NULL && buflen > 0) ? (jobject)wrapByteBuffer((void *)buf, (jlong)buflen) : NULL;
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_open_memory, GLKModel_obj, bb,
                                                        (jint) fmode, (jint) rock, JNI_FALSE);
  return ret;
}

void glk_stream_close(strid_t str, stream_result_t *result) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_close");
  #endif
  jlongArray res = (*env)->NewLongArray(env, 2);
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_stream_close, GLKModel_obj,
                                 (jint) str, res);
  if (result != NULL) {
    jlong* r = (*env)->GetLongArrayElements(env, res, NULL);
    result->readcount = (glui32)r[0];
    result->writecount = (glui32)r[1];
    (*env)->ReleaseLongArrayElements(env, res, r, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, res);
}

strid_t glk_stream_iterate(strid_t str, glui32 *rockptr) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_iterate");
  #endif
  jintArray rock = (*env)->NewIntArray(env, 1);
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_iterate, GLKModel_obj,
                                                        (jint) str, rock);
  if (rockptr != NULL) {
    jint* r = (*env)->GetIntArrayElements(env, rock, NULL);
    *rockptr = (glui32)r[0];
    (*env)->ReleaseIntArrayElements(env, rock, r, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, rock);
  return ret;
}

glui32 glk_stream_get_rock(strid_t str) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_get_rock");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_stream_get_rock, GLKModel_obj,
                                                      (jint) str);
  return ret;
}

void glk_stream_set_position(strid_t str, glsi32 pos, glui32 seekmode) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_set_position");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_stream_set_position, GLKModel_obj,
                                 (jint) str, (jlong) pos, (jint) seekmode);
}

glui32 glk_stream_get_position(strid_t str) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_get_position");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticLongMethod(env, GLKController_class,
                                                       m_glk_stream_get_position, GLKModel_obj,
                                                       (jint) str);
  return ret;
}

void glk_stream_set_current(strid_t str) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_set_current");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_stream_set_current, GLKModel_obj,
                                 (jint) str);
}

strid_t glk_stream_get_current(void) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_get_current");
  #endif
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_get_current, GLKModel_obj);
  return ret;
}

void glk_put_char(unsigned char ch) {
  #ifdef JNI_DEBUG
    #ifdef JNI_DEBUG_SHOW_CHARS
      __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_char");
    #endif
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_char, GLKModel_obj, (jint) ch,
                                 JNI_FALSE);
}

void glk_put_char_stream(strid_t str, unsigned char ch) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_char_stream");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_char_stream, GLKModel_obj,
                                 (jint) str, (jint) ch, JNI_FALSE);
}

void glk_put_string(char *s) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_string");
  #endif

  if (s == NULL) return;

  int len;
  for (len = 0; s[len] != 0; len++) { }

  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)s);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_string, GLKModel_obj, arr,
                                   JNI_FALSE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void glk_put_string_stream(strid_t str, char *s) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_string_stream");
  #endif

  if (s == NULL) return;

  int len;
  for (len = 0; s[len] != 0; len++) { }

  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)s);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_string_stream, GLKModel_obj,
                                   (jint) str, arr, JNI_FALSE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void glk_put_buffer(char* buf, glui32 len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_buffer");
  #endif

  if (buf == NULL || len == 0) {
    if (buf == NULL) {
      printf("glk.c: glk_put_buffer called with null buffer!\n");
    } else {
      printf("glk.c: glk_put_buffer called with zero length!\n");
    }
    return;
  }

  /* Make a temporary byte array for the Java call - in this case it's read-only! */
  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)buf);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_buffer, GLKModel_obj, arr,
                                   (jint) len, JNI_FALSE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void glk_put_buffer_stream(strid_t str, char* buf, glui32 len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_buffer_stream");
  #endif

  if (buf == NULL || len == 0) {
    if (buf == NULL) {
      printf("glk.c: glk_put_buffer_stream called with null buffer!\n");
    } else {
      printf("glk.c: glk_put_buffer_stream called with zero length!\n");
    }
    return;
  }

  /* Make a temporary byte array for the Java call - in this case it's read-only! */
  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)buf);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_buffer_stream, GLKModel_obj,
                                   (jint) str, arr, (jint) len, JNI_FALSE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void glk_set_style(glui32 styl) {
   #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_style");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_set_style, GLKModel_obj,
                                 (jint) styl);
}

void glk_set_style_stream(strid_t str, glui32 styl) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_set_style_stream");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_set_style, GLKModel_obj,
                                 (jint) str, (jint) styl);
}

glsi32 glk_get_char_stream(strid_t str) {
  #ifdef JNI_DEBUG
    #ifdef JNI_DEBUG_SHOW_CHARS
      __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_get_char_stream");
    #endif
  #endif
  glsi32 ret = (glsi32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_get_char_stream, GLKModel_obj,
                                                      (jint) str, JNI_FALSE);
  return ret;
}

glui32 glk_get_line_stream(strid_t str, char* buf, glui32 len) {
    #ifdef JNI_DEBUG
      __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_get_line_stream");
    #endif

    if (buf == NULL || len == 0) return 0;

    jint ret;

    /* Make a temporary byte array for the Java call */
    jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
    if (arr != NULL) {
      ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_get_line_stream,
                                          GLKModel_obj, (jint) str, arr, (jint) len, JNI_FALSE);
      if (ret > 0) {
        (*env)->GetByteArrayRegion(env, arr, 0, ret+1, (jbyte*)buf); /* ret + 1 to ensure we get the null terminator */
      }
      (*env)->DeleteLocalRef(env, arr);
    }

    return (glui32)ret;
}

glui32 glk_get_buffer_stream(strid_t str, char* buf, glui32 len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_get_buffer_stream");
  #endif

  if (buf == NULL || len == 0) return 0;

  jint ret;

  /* Make a temporary byte array for the Java call */
  jbyteArray arr = (*env)->NewByteArray(env, len);
  if (arr != NULL) {
    ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_get_buffer_stream,
                                        GLKModel_obj, (jint) str, arr, (jint) len, JNI_FALSE);
    if (ret > 0) {
      (*env)->GetByteArrayRegion(env, arr, 0, ret, (jbyte*)buf);
    }
    (*env)->DeleteLocalRef(env, arr);
  }

  return (glui32)ret;
}

void glk_stylehint_set(glui32 wintype, glui32 styl, glui32 hint, glsi32 val) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stylehint_set");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_stylehint_set, GLKModel_obj,
                                 (jint) wintype, (jint) styl, (jint) hint, (jint) val);
}

void glk_stylehint_clear(glui32 wintype, glui32 styl, glui32 hint) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stylehint_clear");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_stylehint_clear, GLKModel_obj,
                                 (jint) wintype, (jint) styl, (jint) hint);
}

glui32 glk_style_distinguish(winid_t win, glui32 styl1, glui32 styl2) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_style_distinguish");
  #endif
  return (glui32) (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_style_distinguish,
                                                GLKModel_obj, (jint) win, (jint) styl1,
                                                (jint) styl2);
}

glui32 glk_style_measure(winid_t win, glui32 styl, glui32 hint, glui32 *result) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_style_measure");
  #endif

  jintArray res = (*env)->NewIntArray(env, 1);
  glui32 ret = (glui32) (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_style_measure,
                                                      GLKModel_obj, (jint) win, (jint) styl,
                                                      (jint) hint, res);
  if (result != NULL) {
    jint* r = (*env)->GetIntArrayElements(env, res, NULL);
    *result = (glui32)r[0];
    (*env)->ReleaseIntArrayElements(env, res, r, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, res);
  return ret;
}

frefid_t glk_fileref_create_temp(glui32 usage, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_create_temp");
  #endif
  frefid_t ret = (frefid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                          m_glk_fileref_create_temp, GLKModel_obj,
                                                          (jint) usage, (jint) rock);
  return ret;
}

frefid_t glk_fileref_create_by_name(glui32 usage, char *name, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_create_by_name");
  #endif
  
  if (name == NULL) return NULL;

  int len;
  for (len = 0; name[len] != 0; len++) { }

  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)name);
    frefid_t ret = (frefid_t) ((*env)->CallStaticIntMethod(env, GLKController_class,
                                                             m_glk_fileref_create_by_name,
                                                             GLKModel_obj, (jint) usage, arr,
                                                             (jint) rock));
    (*env)->DeleteLocalRef(env, arr);
    return ret;
  }
  return NULL;
}

frefid_t glk_fileref_create_by_prompt(glui32 usage, glui32 fmode, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_create_by_prompt");
  #endif
  frefid_t ret = (frefid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                          m_glk_fileref_create_by_prompt,
                                                          GLKModel_obj, (jint) usage, (jint) fmode,
                                                          (jint) rock);
  return ret;
}

frefid_t glk_fileref_create_from_fileref(glui32 usage, frefid_t fref, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_create_from_fileref");
  #endif
  frefid_t ret = (frefid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                          m_glk_fileref_create_from_fileref,
                                                          GLKModel_obj, (jint) usage, (jint) fref,
                                                          (jint) rock);
  return ret;
}

void glk_fileref_destroy(frefid_t fref) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_destroy");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_fileref_destroy, GLKModel_obj,
                                 (jint) fref);
}

frefid_t glk_fileref_iterate(frefid_t fref, glui32 *rockptr) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_iterate");
  #endif
  jintArray rock = (*env)->NewIntArray(env, 1);
  frefid_t ret = (frefid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                          m_glk_fileref_iterate, GLKModel_obj,
                                                          (jint) fref, rock);
  if (rockptr != NULL) {
    jint* r = (*env)->GetIntArrayElements(env, rock, NULL);
    *rockptr = (glui32)r[0];
    (*env)->ReleaseIntArrayElements(env, rock, r, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, rock);
  return ret;
}

glui32 glk_fileref_get_rock(frefid_t fref) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_get_rock");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_fileref_get_rock, GLKModel_obj,
                                                      (jint) fref);
  return ret;
}

void glk_fileref_delete_file(frefid_t fref) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_delete_file");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_fileref_delete_file, GLKModel_obj,
                                 (jint) fref);
}

glui32 glk_fileref_does_file_exist(frefid_t fref) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_fileref_does_file_exist");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_fileref_does_file_exist, GLKModel_obj,
                                                      (jint) fref);
  return ret;
}

void glk_select(event_t *event) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_select");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_select, GLKModel_obj,
                                 GLKEvent_obj);

  if (event != NULL) {
    event->type = (*env)->GetIntField(env, GLKEvent_obj, ev_type);
    event->win = (winid_t)(*env)->GetIntField(env, GLKEvent_obj, ev_win);
    event->val1 = (*env)->GetIntField(env, GLKEvent_obj, ev_val1);
    event->val2 = (*env)->GetIntField(env, GLKEvent_obj, ev_val2);
  }
}

void glk_select_poll(event_t *event) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_select_poll");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_select_poll, GLKModel_obj,
                                 GLKEvent_obj);

  if (event != NULL) {
    event->type = (*env)->GetIntField(env, GLKEvent_obj, ev_type);
    event->win = (winid_t)(*env)->GetIntField(env, GLKEvent_obj, ev_win);
    event->val1 = (*env)->GetIntField(env, GLKEvent_obj, ev_val1);
    event->val2 = (*env)->GetIntField(env, GLKEvent_obj, ev_val2);
  }
}

void glk_request_timer_events(glui32 millisecs) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_request_timer_events");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_request_timer_events, GLKModel_obj,
                                 (jlong) millisecs);
}

void glk_request_line_event(winid_t win, char *buf, glui32 maxlen, glui32 initlen) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_request_line_event");
  #endif
  /* Following reference should be freed from the Java end when the line is completed or cancelled. */
  jobject bb = (jobject)wrapByteBuffer((void *)buf, (jlong)maxlen);
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_request_line_event, GLKModel_obj,
                                 (jint) win, bb, (jint) initlen, JNI_FALSE);
}

void glk_request_char_event(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_request_char_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_request_char_event, GLKModel_obj,
                                 (jint) win);
}

void glk_request_mouse_event(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_request_mouse_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_request_mouse_event, GLKModel_obj,
                                 (jint) win);
}

void glk_cancel_line_event(winid_t win, event_t *event) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_cancel_line_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_cancel_line_event, GLKModel_obj,
                                 (jint) win, GLKEvent_obj);

  if (event != NULL) {
    event->type = (*env)->GetIntField(env, GLKEvent_obj, ev_type);
    event->win = (winid_t)(*env)->GetIntField(env, GLKEvent_obj, ev_win);
    event->val1 = (*env)->GetIntField(env, GLKEvent_obj, ev_val1);
    event->val2 = (*env)->GetIntField(env, GLKEvent_obj, ev_val2);
  }
}

void glk_cancel_char_event(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_cancel_char_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_cancel_char_event, GLKModel_obj,
                                 (jint) win);
}

void glk_cancel_mouse_event(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_cancel_mouse_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_cancel_mouse_event, GLKModel_obj,
                                 (jint) win);
}

void glk_set_echo_line_event(winid_t win, glui32 val) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_echo_line_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_set_echo_line_event, GLKModel_obj,
                                 (jint) win, (jint) val);
}

void glk_set_terminators_line_event(winid_t win, glui32 *keycodes, glui32 count) {
  #ifdef JNI_DEBUG
   __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_terminators_line_event");
  #endif
  jint c = (jint)count;
  jlong tmp[c];
  int i;
  for (i = 0; i < c; i++) {
    tmp[i] = (jlong)keycodes[i];
  }
  jlongArray jkeycodes = (*env)->NewLongArray(env, c);
  (*env)->SetLongArrayRegion(env, jkeycodes, 0, c, tmp);
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_set_terminators_line_event,
                                 GLKModel_obj, (jint) win, jkeycodes);
  (*env)->DeleteLocalRef(env, jkeycodes);
}

glui32 glk_buffer_to_lower_case_uni(glui32 * buf, glui32 len, glui32 numchars) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_buffer_to_lower_case_uni");
  #endif
  jobject bb = (jobject)wrapByteBuffer((void *)buf, (jlong)(len * 4));
  jint ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_buffer_to_lower_case_uni,
                                           GLKModel_obj, bb, (jint) numchars);
  unwrapByteBuffer(env, bb);
  return ret;
}

glui32 glk_buffer_to_upper_case_uni(glui32 * buf, glui32 len, glui32 numchars) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_buffer_to_upper_case_uni");
  #endif
  jobject bb = (jobject)wrapByteBuffer((void *)buf, (jlong)(len * 4));
  jint ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_buffer_to_upper_case_uni,
                                           GLKModel_obj, bb, (jint) numchars);
  unwrapByteBuffer(env, bb);
  return ret;
}

glui32 glk_buffer_to_title_case_uni(glui32 * buf, glui32 len, glui32 numchars, glui32 lowerrest) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_buffer_to_title_case_uni");
  #endif
  jobject bb = (jobject)wrapByteBuffer((void *)buf, (jlong)(len * 4));
  jint ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_buffer_to_title_case_uni,
                                           GLKModel_obj, bb, (jint) numchars,
                                           lowerrest == 0 ? JNI_FALSE : JNI_TRUE);
  unwrapByteBuffer(env, bb);
  return ret;
}

void glk_put_char_uni(glui32 ch) {
  #ifdef JNI_DEBUG
    #ifdef JNI_DEBUG_SHOW_CHARS
      __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_char_uni");
    #endif
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_char, GLKModel_obj, (jint) ch,
                                 JNI_TRUE);
}

void glk_put_string_uni(glui32 *s) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_string_uni");
  #endif

  if (s == NULL) return;

  int len;
  for (len = 0; s[len] != 0; len++) { }
  len *= 4;  /* convert to length in bytes */

  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)s);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_string, GLKModel_obj, arr,
                                   JNI_TRUE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void glk_put_buffer_uni(glui32 *buf, glui32 len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_buffer_uni");
  #endif

  if (buf == NULL || len == 0) return;

  int byteLen = len * 4;

   /* Make a temporary byte array for the Java call - in this case it's read-only! */
  jbyteArray arr = (*env)->NewByteArray(env, (jint)byteLen);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)byteLen, (const jbyte*)buf);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_buffer, GLKModel_obj, arr,
                                   (jint) len, JNI_TRUE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void glk_put_char_stream_uni(strid_t str, glui32 ch) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_char_stream_uni");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_char_stream, GLKModel_obj,
                                 (jint) str, (jint) ch, JNI_TRUE);
}

void glk_put_string_stream_uni(strid_t str, glui32 *s) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_string_stream_uni");
  #endif

  if (s == NULL) return;

  int len;
  for (len = 0; s[len] != 0; len++) { }
  len *= 4;  /* convert to length in bytes */

  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)s);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_string_stream, GLKModel_obj,
                                   (jint) str, arr, JNI_TRUE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void glk_put_buffer_stream_uni(strid_t str, glui32 *buf, glui32 len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_put_buffer_stream_uni");
  #endif

  if (buf == NULL || len == 0) return;

  int byteLen = len * 4;

  /* Make a temporary byte array for the Java call - in this case it's read-only! */
  jbyteArray arr = (*env)->NewByteArray(env, (jint)byteLen);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)byteLen, (const jbyte*)buf);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_put_buffer_stream, GLKModel_obj,
                                   (jint) str, arr, (jint) len, JNI_TRUE);
    (*env)->DeleteLocalRef(env, arr);
  }
}

glsi32 glk_get_char_stream_uni(strid_t str) {
  #ifdef JNI_DEBUG
    #ifdef JNI_DEBUG_SHOW_CHARS
      __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_get_char_stream_uni");
    #endif
  #endif
  glsi32 ret = (glsi32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_get_char_stream, GLKModel_obj,
                                                      (jint) str, JNI_TRUE);
  return ret;
}

glui32 glk_get_buffer_stream_uni(strid_t str, glui32 *buf, glui32 len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_get_buffer_stream_uni");
  #endif
 
  if (buf == NULL || len == 0) return 0;

  int ret;
  int byteLen = len * 4;

  /* Make a temporary byte array for the Java call */
  jbyteArray arr = (*env)->NewByteArray(env, (jint)byteLen);
  if (arr != NULL) {
    ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_get_buffer_stream,
                                        GLKModel_obj, (jint) str, arr, (jint) len, JNI_TRUE);
    if (ret > 0) {
      (*env)->GetByteArrayRegion(env, arr, 0, (jint)(ret * 4), (jbyte*)buf);
    }
    (*env)->DeleteLocalRef(env, arr);
  }

  return ret;
}

glui32 glk_get_line_stream_uni(strid_t str, glui32 *buf, glui32 len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_get_line_stream_uni");
  #endif

  if (buf == NULL || len == 0) return 0;

  int ret;
  int byteLen = len * 4;

  /* Make a temporary byte array for the Java call */
  jbyteArray arr = (*env)->NewByteArray(env, (jint)byteLen);
  if (arr != NULL) {
    ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_get_line_stream,
                                        GLKModel_obj, (jint) str, arr, (jint) len, JNI_TRUE);
    if (ret > 0) {
      (*env)->GetByteArrayRegion(env, arr, 0, (jint)(ret+1) * 4, (jbyte*)buf);  /* ret + 1 to ensure we get the null terminator */
    }
    (*env)->DeleteLocalRef(env, arr);
  }

  return ret;
}

strid_t glk_stream_open_file_uni(frefid_t fileref, glui32 fmode, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_open_file_uni");
  #endif
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_open_file, GLKModel_obj,
                                                        (jint) fileref, (jint) fmode, (jint) rock,
                                                        JNI_TRUE);
  return ret;
}

strid_t glk_stream_open_memory_uni(glui32 *buf, glui32 buflen, glui32 fmode, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_open_memory_uni");
  #endif
  /* Following reference should be freed from the Java end when the memory stream is closed: */
  jobject bb = (buf != NULL && buflen > 0) ? (jobject)wrapByteBuffer((void *)buf, (jlong)(buflen * 4)) : NULL;
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_open_memory, GLKModel_obj, bb,
                                                        (jint) fmode, (jint) rock, JNI_TRUE);
  return ret;
}

void glk_request_char_event_uni(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_request_char_event_uni");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_request_char_event, GLKModel_obj,
                                 (jint) win);
}

void glk_request_line_event_uni(winid_t win, glui32 *buf, glui32 maxlen, glui32 initlen) {
   #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_request_line_event_uni");
  #endif
  /* Following reference will be freed from the Java end when line is completed or cancelled */
  jobject bb = (jobject)wrapByteBuffer((void *)buf, (jlong)(maxlen * 4));
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_request_line_event, GLKModel_obj,
                                 (jint) win, bb, (jint) initlen, JNI_TRUE);
}

glui32 glk_buffer_canon_decompose_uni(glui32 *buf, glui32 len, glui32 numchars) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_buffer_canon_decompose_uni");
  #endif
  jobject bb = (jobject)wrapByteBuffer((void *)buf, (jlong)(len * 4));
  jint ret = (*env)->CallStaticIntMethod(env, GLKController_class,
                                           m_glk_buffer_canon_decompose_uni, GLKModel_obj, bb,
                                           (jint) numchars);
  unwrapByteBuffer(env, bb);
  return ret;
}

glui32 glk_buffer_canon_normalize_uni(glui32 *buf, glui32 len, glui32 numchars) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_buffer_canon_normalize_uni");
  #endif
  jobject bb = (jobject)wrapByteBuffer((void *)buf, (jlong)(len * 4));
  jint ret = (*env)->CallStaticIntMethod(env, GLKController_class,
                                           m_glk_buffer_canon_normalize_uni, GLKModel_obj, bb,
                                           (jint) numchars);
  unwrapByteBuffer(env, bb);
  return ret;
}

glui32 glk_image_draw(winid_t win, glui32 image, glsi32 val1, glsi32 val2) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_image_draw");
  #endif
  jboolean res = (*env)->CallStaticBooleanMethod(env, GLKController_class, m_glk_image_draw,
                                                   GLKModel_obj, (jint) win, (jint) image,
                                                   (jint) val1, (jint) val2);
  return (res == JNI_TRUE) ? 1 : 0;
}

glui32 glk_image_draw_scaled(winid_t win, glui32 image, glsi32 val1, glsi32 val2, glui32 width, glui32 height) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_image_draw_scaled");
  #endif
  jboolean res = (*env)->CallStaticBooleanMethod(env, GLKController_class,
                                                   m_glk_image_draw_scaled, GLKModel_obj,
                                                   (jint) win, (jint) image, (jint) val1,
                                                   (jint) val2, (jint) width, (jint) height);
  return (res == JNI_TRUE) ? 1 : 0;
}

glui32 glk_image_get_info(glui32 image, glui32 *width, glui32 *height) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_image_get_info");
  #endif
  jboolean res = (*env)->CallStaticBooleanMethod(env, GLKController_class, m_glk_image_get_info,
                                                   GLKModel_obj, (jint) image, Point_obj);
  if (width != NULL) {
    *width = (*env)->GetIntField(env, Point_obj, pt_x);
  }
  if (height != NULL) {
    *height = (*env)->GetIntField(env, Point_obj, pt_y);
  }
  return (res == JNI_TRUE) ? 1 : 0;
}

void glk_window_flow_break(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_flow_break");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_flow_break, GLKModel_obj,
                                 (jint) win);
}

void glk_window_erase_rect(winid_t win, glsi32 left, glsi32 top, glui32 width, glui32 height) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_erase_rect");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_erase_rect, GLKModel_obj,
                                 (jint) win, (jint) left, (jint) top, (jint) width, (jint) height);
}

void glk_window_fill_rect(winid_t win, glui32 color,
    glsi32 left, glsi32 top, glui32 width, glui32 height) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_fill_rect");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_fill_rect, GLKModel_obj,
                                 (jint) win, (jint) color, (jint) left, (jint) top, (jint) width,
                                 (jint) height);
}

void glk_window_set_background_color(winid_t win, glui32 color) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_window_set_background_color");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_window_set_background_color,
                                 GLKModel_obj, (jint) win, (jint) color);
}

schanid_t glk_schannel_create(glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_create");
  #endif
  schanid_t ret = (schanid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                            m_glk_schannel_create, GLKModel_obj,
                                                            (jint) rock);
  return ret;
}

void glk_schannel_destroy(schanid_t chan) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_ERROR, "glk.c", "glk_schannel_destroy");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_schannel_destroy, GLKModel_obj,
                                 (jint) chan);
}

schanid_t glk_schannel_iterate(schanid_t chan, glui32 *rockptr) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_iterate");
  #endif
  jintArray rock = (*env)->NewIntArray(env, 1);
  schanid_t ret = (schanid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                            m_glk_schannel_iterate, GLKModel_obj,
                                                            (jint) chan, rock);
  if (rockptr != NULL) {
    jint* r = (*env)->GetIntArrayElements(env, rock, NULL);
    *rockptr = (glui32)r[0];
    (*env)->ReleaseIntArrayElements(env, rock, r, JNI_ABORT);
  }
  (*env)->DeleteLocalRef(env, rock);
  return ret;
}

glui32 glk_schannel_get_rock(schanid_t chan) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_get_rock");
  #endif
  glui32 ret = (glui32) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                      m_glk_schannel_get_rock, GLKModel_obj,
                                                      (jint) chan);
  return ret;
}

glui32 glk_schannel_play(schanid_t chan, glui32 snd) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_play");
  #endif
  jboolean ret = (glui32) (*env)->CallStaticBooleanMethod(env, GLKController_class,
                                                            m_glk_schannel_play,
                                                            GLKModel_obj, (jint) chan, (jint) snd);
  return ret == JNI_TRUE ? 1 : 0;
}

glui32 glk_schannel_play_ext(schanid_t chan, glui32 snd, glui32 repeats, glui32 notify) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_play_ext");
  #endif
  jboolean ret = (*env)->CallStaticBooleanMethod(env, GLKController_class,
                                                            m_glk_schannel_play_ext,
                                                            GLKModel_obj, (jint) chan, (jint) snd, (jint) repeats,
                                                            (jint) notify);
  return ret == JNI_TRUE ? 1 : 0;
}

glui32 glk_schannel_play_multi(schanid_t *chanarray, glui32 chancount,
    glui32 *sndarray, glui32 soundcount, glui32 notify) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_play_multi");
  #endif
  
  jintArray ca = (*env)->NewIntArray(env, chancount);
  jintArray sa = (*env)->NewIntArray(env, soundcount);
  jint* c = (*env)->GetIntArrayElements(env, ca, NULL);
  jint* s = (*env)->GetIntArrayElements(env, sa, NULL);

  int i;
  for (i = 0; i < chancount; i++) {
    c[i] = (jint)chanarray[i];
  }
  (*env)->ReleaseIntArrayElements(env, ca, c, 0);
  for (i = 0; i < soundcount; i++) {
    s[i] = (jint)sndarray[i];
  }
  (*env)->ReleaseIntArrayElements(env, sa, s, 0);

  jint ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_glk_schannel_play_multi,
                                 GLKModel_obj, ca, sa, (jint)notify);
  
  (*env)->DeleteLocalRef(env, ca);
  (*env)->DeleteLocalRef(env, sa);

  return (glui32)ret;
}

void glk_schannel_stop(schanid_t chan) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_stop");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_schannel_stop, GLKModel_obj,
                                 (jint) chan);
}

void glk_schannel_set_volume(schanid_t chan, glui32 vol) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_set_volume");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_schannel_set_volume, GLKModel_obj,
                                 (jint) chan, (jlong) vol);
}

void glk_sound_load_hint(glui32 snd, glui32 flag) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_sound_load_hint");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_sound_load_hint, GLKModel_obj,
                                 (jint) snd, (jint) flag);
}

schanid_t glk_schannel_create_ext(glui32 rock, glui32 volume) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_create_ext");
  #endif
  schanid_t ret = (schanid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                            m_glk_schannel_create_ext, GLKModel_obj,
                                                            (jint) rock, (jlong) volume);
  return ret;
}

void glk_schannel_pause(schanid_t chan) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_pause");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_schannel_pause, GLKModel_obj,
                                 (jint) chan);
}

void glk_schannel_unpause(schanid_t chan) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_schannel_unpause");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_schannel_unpause, GLKModel_obj,
                                 (jint) chan);
}

void glk_schannel_set_volume_ext(schanid_t chan, glui32 vol,
    glui32 duration, glui32 notify) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_volume_ext");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_schannel_set_volume_ext,
                                 GLKModel_obj, (jint) chan, (jlong) vol, (jint) duration,
                                 (jint) notify);
}

void glk_set_hyperlink(glui32 linkval) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_hyperlink");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_set_hyperlink, GLKModel_obj,
                                 (jint) linkval);
}

void glk_set_hyperlink_stream(strid_t str, glui32 linkval) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_set_hyperlink_stream");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_set_hyperlink_stream, GLKModel_obj,
                                 (jint) str, (jint) linkval);
}

void glk_request_hyperlink_event(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_request_hyperlink_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_request_hyperlink_event,
                                 GLKModel_obj, (jint) win);
}

void glk_cancel_hyperlink_event(winid_t win) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_cancel_hyperlink_event");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glk_cancel_hyperlink_event,
                                 GLKModel_obj, (jint) win);
}

/* Our date and time functions below are reproduced from cgdate.c of cheapglk.
   Designed by Andrew Plotkin <erkyrath@eblong.com>
   See https://github.com/erkyrath/cheapglk */
time_t timegm(struct tm *tm) {
  time_t answer;
  putenv("TZ=UTC");
  tzset();
  answer=mktime(tm);
  putenv("TZ=");
  tzset();
  return answer;
}

/* From Plotkin's cgdate.c: 
   Convert a Unix timestamp, along with a microseconds value, to a glktimeval. */
static void gli_timestamp_to_time(time_t timestamp, glsi32 microsec, glktimeval_t *time) {
  if (sizeof(timestamp) <= 4) {
    /* This platform has 32-bit time, but we can't do anything
       about that. Hope it's not 2038 yet. */
    if (timestamp >= 0) {
      time->high_sec = 0;
    } else {
      time->high_sec = -1;
    }
    time->low_sec = timestamp;
  } else {
    /* The cast to int64_t shouldn't be necessary, but it
       suppresses a pointless warning in the 32-bit case.
       (Remember that we won't be executing this line in the
       32-bit case.) */
    time->high_sec = (((int64_t)timestamp) >> 32) & 0xFFFFFFFF;
    time->low_sec = timestamp & 0xFFFFFFFF;
  }

  time->microsec = microsec;
}

/* From Plotkin's cgdate.c:
   Divide a Unix timestamp by a (positive) value. */
static glsi32 gli_simplify_time(time_t timestamp, glui32 factor) {
  /* We want to round towards negative infinity, which takes a little
     bit of fussing. */
  if (timestamp >= 0) {
    return timestamp / (time_t)factor;
  } else {
    return -1 - (((time_t)-1 - timestamp) / (time_t)factor);
  }
}

/* From Plotkin's cgdate.c:
   Copy a POSIX tm structure to a glkdate. */
static void gli_date_from_tm(glkdate_t *date, struct tm *tm) {
  date->year = 1900 + tm->tm_year;
  date->month = 1 + tm->tm_mon;
  date->day = tm->tm_mday;
  date->weekday = tm->tm_wday;
  date->hour = tm->tm_hour;
  date->minute = tm->tm_min;
  date->second = tm->tm_sec;
}

/* From Plotkin's cgdate.c:
   Copy a glkdate to a POSIX tm structure.
   This is used in the "glk_date_to_..." functions, which are supposed
   to normalize the glkdate. We're going to rely on the mktime() /
   timegm() functions to do that -- except they don't handle microseconds.
   So we'll have to do that normalization here, adjust the tm_sec value,
   and return the normalized number of microseconds.
*/
static glsi32 gli_date_to_tm(glkdate_t *date, struct tm *tm) {
  glsi32 microsec;

  memset(tm, 0, sizeof(tm));
  tm->tm_year = date->year - 1900;
  tm->tm_mon = date->month - 1;
  tm->tm_mday = date->day;
  tm->tm_wday = date->weekday;
  tm->tm_hour = date->hour;
  tm->tm_min = date->minute;
  tm->tm_sec = date->second;
  microsec = date->microsec;

  if (microsec >= 1000000) {
    tm->tm_sec += (microsec / 1000000);
    microsec = microsec % 1000000;
  } else if (microsec < 0) {
    microsec = -1 - microsec;
    tm->tm_sec -= (1 + microsec / 1000000);
    microsec = 999999 - (microsec % 1000000);
  }

  return microsec;
}

void glk_current_time(glktimeval_t *time) {
  /* No point going back to Java to do this, just handle it directly in C. */
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_current_time");
  #endif

  /* From Plotkin's cgdate.c: */
  struct timeval tv;
  if (gettimeofday(&tv, NULL)) {
    gli_timestamp_to_time(0, 0, time);
    fatalError("glk_current_time: gettimeofday() failed.");
    return;
  }

  gli_timestamp_to_time(tv.tv_sec, tv.tv_usec, time);
}

glsi32 glk_current_simple_time(glui32 factor) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_current_simple_time");
  #endif

  /* From Plotkin's cgdate.c: */
  struct timeval tv;

  if (factor == 0) {
    fatalError("glk_current_simple_time: factor cannot be zero.");
    return 0;
  }

  if (gettimeofday(&tv, NULL)) {
    fatalError("glk_current_simple_time: gettimeofday() failed.");
    return 0;
  }

  return gli_simplify_time(tv.tv_sec, factor);
}

void glk_time_to_date_utc(glktimeval_t *time, glkdate_t *date) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_time_to_date_utc");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp;
  struct tm tm;

  timestamp = time->low_sec;
  if (sizeof(timestamp) > 4) {
    timestamp += ((int64_t)time->high_sec << 32);
  }

  gmtime_r(&timestamp, &tm);

  gli_date_from_tm(date, &tm);
  date->microsec = time->microsec;
}

void glk_time_to_date_local(glktimeval_t *time, glkdate_t *date) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_time_to_date_local");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp;
  struct tm tm;

  timestamp = time->low_sec;
  if (sizeof(timestamp) > 4) {
    timestamp += ((int64_t)time->high_sec << 32);
  }

  localtime_r(&timestamp, &tm);

  gli_date_from_tm(date, &tm);
  date->microsec = time->microsec;
}

void glk_simple_time_to_date_utc(glsi32 time, glui32 factor, glkdate_t *date) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_simple_time_to_date_utc");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp = (time_t)time * factor;
  struct tm tm;

  gmtime_r(&timestamp, &tm);

  gli_date_from_tm(date, &tm);
  date->microsec = 0;
}

void glk_simple_time_to_date_local(glsi32 time, glui32 factor, glkdate_t *date) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_simple_time_to_date_local");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp = (time_t)time * factor;
  struct tm tm;

  localtime_r(&timestamp, &tm);

  gli_date_from_tm(date, &tm);
  date->microsec = 0;
}

void glk_date_to_time_utc(glkdate_t *date, glktimeval_t *time) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_date_to_time_utc");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp;
  struct tm tm;
  glsi32 microsec;

  microsec = gli_date_to_tm(date, &tm);
  tm.tm_isdst = 0;
  timestamp = timegm(&tm);

  gli_timestamp_to_time(timestamp, microsec, time);
}

void glk_date_to_time_local(glkdate_t *date, glktimeval_t *time) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_date_to_time_local");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp;
  struct tm tm;
  glsi32 microsec;

  microsec = gli_date_to_tm(date, &tm);
  tm.tm_isdst = -1;
  timestamp = mktime(&tm);

  gli_timestamp_to_time(timestamp, microsec, time);
}

glsi32 glk_date_to_simple_time_utc(glkdate_t *date, glui32 factor) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_date_to_simple_time_utc");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp;
  struct tm tm;

  if (factor == 0) {
    fatalError("glk_date_to_simple_time_utc: factor cannot be zero.");
    return 0;
  }

  gli_date_to_tm(date, &tm);
  /* The timegm function is not standard POSIX. If it's not available
       on your platform, try setting the env var "TZ" to "", calling
       mktime(), and then resetting "TZ". */
  tm.tm_isdst = 0;
  timestamp = timegm(&tm);

  return gli_simplify_time(timestamp, factor);
}

glsi32 glk_date_to_simple_time_local(glkdate_t *date, glui32 factor) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_date_to_simple_time_local");
  #endif

  /* From Plotkin's cgdate.c: */
  time_t timestamp;
  struct tm tm;

  if (factor == 0) {
    fatalError("glk_date_to_simple_time_local: factor cannot be zero.");
    return 0;
  }

  gli_date_to_tm(date, &tm);
  tm.tm_isdst = -1;
  timestamp = mktime(&tm);

  return gli_simplify_time(timestamp, factor);
}

strid_t glk_stream_open_resource(glui32 filenum, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_open_resource");
  #endif
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_open_resource, GLKModel_obj,
                                                        (jint) filenum, (jint) rock, JNI_FALSE);
  return ret;
}


strid_t glk_stream_open_resource_uni(glui32 filenum, glui32 rock) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glk_stream_open_resource_uni");
  #endif
  strid_t ret = (strid_t) (*env)->CallStaticIntMethod(env, GLKController_class,
                                                        m_glk_stream_open_resource, GLKModel_obj,
                                                        (jint) filenum, (jint) rock, JNI_TRUE);
  return ret;
}

/* ===================================================
 * NON-OFFICIAL GLK EXTENSIONS
 * =================================================== */

void glkunix_set_base_file(char *filename)
{
}

strid_t glkunix_stream_open_pathname(char *pathname, glui32 textmode, glui32 rock)
{
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "glkunix_stream_open_pathname");
  #endif

  if (pathname == NULL) return NULL;

  int len;
  for (len = 0; pathname[len] != 0; len++) { }

  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)pathname);
      strid_t ret = (strid_t) ((*env)->CallStaticIntMethod(env, GLKController_class,
                                                           m_glkplus_stream_open_pathname,
                                                           GLKModel_obj, arr,
                                                           (textmode != 0) ? JNI_TRUE : JNI_FALSE,
                                                           (jint) rock));
    (*env)->DeleteLocalRef(env, arr);
    return ret;
  }
  return NULL;
}

char* garglk_fileref_get_name(frefid_t fref) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_fileref_get_name");
  #endif

  jbyteArray arr = (jbyteArray) (*env)->CallStaticObjectMethod(env, GLKController_class,
                                                                 m_glkplus_fileref_get_name,
                                                                 GLKModel_obj, (jint) fref);
  if (arr != NULL) {
    int len = (*env)->GetArrayLength(env, arr);
    char* buf = malloc(len+1);  // TODO: can we stop this memory leak?
    (*env)->GetByteArrayRegion(env, arr, 0, (jint)len, (jbyte*)buf);
    buf[len] = '\0';  // ensure NULL-terminated!
    return buf;
  }
  return NULL;
}

void garglk_set_program_name(const char *name) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_program_name");
  #endif
  /* we just ignore this */
}

void garglk_set_program_info(const char *info) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_program_info");
  #endif
  /* we just ignore this */
}

void garglk_set_story_name(const char *name) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_story_name");
  #endif
  /* we just ignore this */
}

void garglk_set_story_title(const char *title) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_story_title");
  #endif
  /* we just ignore this */
}

void garglk_set_config(const char *name) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_config");
  #endif
  /* we just ignore this */
}

void garglk_unput_string(char *str) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_unput_string");
  #endif
  /* we just ignore this */
}

void garglk_unput_string_uni(glui32 *str) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_unput_string_uni");
  #endif
  /* we just ignore this */
}

void garglk_set_zcolors(glui32 fg, glui32 bg) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_zcolors");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glkplus_set_zcolors, GLKModel_obj,
                                 (jint) fg, (jint) bg);
}

void garglk_set_zcolors_stream(strid_t str, glui32 fg, glui32 bg) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_zcolors_stream");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glkplus_set_zcolors_stream,
                                 GLKModel_obj, str, (jint) fg, (jint) bg);
}

void garglk_set_reversevideo(glui32 reverse) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_reversevideo");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glkplus_set_reversevideo, GLKModel_obj,
                                 (jint) reverse);
}

void garglk_set_reversevideo_stream(strid_t str, glui32 reverse) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "garglk_set_reversevideo_stream");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glkplus_set_reversevideo_stream,
                                 GLKModel_obj, (jint) str, (jint) reverse);
}

void fab_set_html_mode(int on) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "fab_set_html_mode");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glkplus_set_html_mode, GLKModel_obj,
                                 on ? JNI_TRUE : JNI_FALSE);
}

void fab_new_html_page(int mainwin) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_INFO, "glk.c", "fab_new_html_page");  
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_glkplus_new_html_page, GLKModel_obj,
                                 (jint) mainwin);
}

/* TADS BANNER INTERFACE */
int tads_get_input(unsigned char *buf, size_t len, int initlen, winid_t mainwin, int timer) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tads_get_input");
  #endif
    
  if (buf == NULL || len == 0) return 0;

  jint ret;

  /* Make a temporary byte array for the Java call */
  jbyteArray arr = (*env)->NewByteArray(env, len);
  if (arr != NULL) {
    ret = (*env)->CallStaticIntMethod(env, GLKController_class, m_tads_get_input,
                                        GLKModel_obj, arr, (jint)initlen, (jint)mainwin, (jint)timer);
    if (ret > 0) {
      (*env)->GetByteArrayRegion(env, arr, 0, len, (jbyte*)buf);
    }
    (*env)->DeleteLocalRef(env, arr);
  }

  return ret;
}

void tads_put_string(const char *buf, size_t len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tads_put_string");
  #endif
    
  if (buf == NULL || len == 0) {
    if (buf == NULL) {
      printf("glk.c: tads_put_string called with null buffer!\n");
    } else {
      printf("glk.c: tads_put_string called with zero length!\n");
    }
    return;
  }

  /* Make a temporary byte array for the Java call - in this case it's read-only! */
  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, (jint)len, (const jbyte*)buf);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_tads_put_string, GLKModel_obj, arr);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void *tadsban_create(void *parent, int where, void *other, int wintype,
                       int align, int siz, int siz_units,
                       unsigned long style) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_create");
  #endif
  int ref = (*env)->CallStaticIntMethod(env, GLKController_class, m_tadsban_create, GLKModel_obj,
                                          (jint) parent, (jint) where, (jint) other, (jint) wintype,
                                          (jint)align, (jint)siz, (jint)siz_units, (jlong)style);
  return (void *)ref;
}

int tadsban_get_charwidth(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_get_charwidth");
  #endif
  int ret = (int) (*env)->CallStaticIntMethod(env, GLKController_class, m_tadsban_get_charwidth,
                                                GLKModel_obj, (jint) banner_handle);
  return ret;
}

int tadsban_get_charheight(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_get_charheight");
  #endif
  int ret = (int) (*env)->CallStaticIntMethod(env, GLKController_class, m_tadsban_get_charheight,
                                                GLKModel_obj, (jint) banner_handle);
  return ret;
}

/*
 *   Banner information structure.  This is filled in by the system-specific
 *   implementation in os_banner_getinfo().  
 */
struct os_banner_info_t
{
    /* alignment */
    int align;

    /* style flags - these indicate the style flags actually in use */
    unsigned long style;

    /* 
     *   Actual on-screen size of the banner, in rows and columns.  If the
     *   banner is displayed in a proportional font or can display multiple
     *   fonts of different sizes, this is approximated by the number of "0"
     *   characters in the window's default font that will fit in the
     *   window's display area.  
     */
    int rows;
    int columns;

    /*
     *   Actual on-screen size of the banner in pixels.  This is meaningful
     *   only for full HTML interpreter; for text-only interpreters, these
     *   are always set to zero.
     *   
     *   Note that even if we're running on a GUI operating system, these
     *   aren't meaningful unless this is a full HTML interpreter.  Text-only
     *   interpreters should always set these to zero, even on GUI OS's.  
     */
    int pix_width;
    int pix_height;

    /* 
     *   OS line wrapping flag.  If this is set, the window uses OS-level
     *   line wrapping because the window uses a proportional font, so the
     *   caller does not need to (and should not) perform line breaking in
     *   text displayed in the window.
     *   
     *   Note that OS line wrapping is a PERMANENT feature of the window.
     *   Callers can note this information once and expect it to remain
     *   fixed through the window's lifetime.  
     */
    int os_line_wrap;
};
typedef struct os_banner_info_t os_banner_info_t;

int tadsban_getinfo(void *banner_handle, os_banner_info_t *info) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_getinfo");
  #endif
  int ret = (int) (*env)->CallStaticIntMethod(env, GLKController_class, m_tadsban_getinfo,
                                                GLKModel_obj, (jint) banner_handle,
                                                TADSBannerInfo_obj);
  if (info != NULL) {
    info->align = (*env)->GetIntField(env, TADSBannerInfo_obj, ban_align);
    info->style = (*env)->GetLongField(env, TADSBannerInfo_obj, ban_style);
    info->rows = (*env)->GetIntField(env, TADSBannerInfo_obj, ban_rows);
    info->columns = (*env)->GetIntField(env, TADSBannerInfo_obj, ban_columns);
    info->pix_width = (*env)->GetIntField(env, TADSBannerInfo_obj, ban_pix_width);
    info->pix_height = (*env)->GetIntField(env, TADSBannerInfo_obj, ban_pix_height);
    info->os_line_wrap = (*env)->GetIntField(env, TADSBannerInfo_obj, ban_os_line_wrap);
  }
  return ret; 
}

void tadsban_delete(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_delete");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_delete, GLKModel_obj,
                                 (jint) banner_handle);
}

void tadsban_orphan(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_orphan");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_orphan, GLKModel_obj,
                                 (jint) banner_handle);
}

void tadsban_clear(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_clear");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_clear, GLKModel_obj,
                                 (jint) banner_handle);
}

void tadsban_disp(void *banner_handle, const char *txt, size_t len) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_disp");
  #endif
  
  if (txt == NULL || len == 0) return;

  jbyteArray arr = (*env)->NewByteArray(env, (jint)len);
  if (arr != NULL) {
    (*env)->SetByteArrayRegion(env, arr, 0, len, (const jbyte*)txt);
    (*env)->CallStaticVoidMethod(env, GLKController_class, m_tads_put_string_stream, GLKModel_obj,
                                   arr, (jint) banner_handle);
    (*env)->DeleteLocalRef(env, arr);
  }
}

void tadsban_set_attr(void *banner_handle, int attr) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_set_attr");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_set_attr, GLKModel_obj,
                                 (jint) banner_handle, (jint) attr);
}

void tadsban_set_color(void *banner_handle, int fg, int bg) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_set_color");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_set_color, GLKModel_obj,
                                 (jint) banner_handle, (jint) fg, (jint) bg);
}

void tadsban_set_screen_color(void *banner_handle, int color) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_set_screen_color");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_set_screen_color, GLKModel_obj,
                                 (jint) banner_handle, (jint) color);
}

void tadsban_flush(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_flush");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_flush, GLKModel_obj,
                                 (jint) banner_handle);
}

void tadsban_set_size(void *banner_handle, int siz, int siz_units, int is_advisory) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_set_size");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_set_size, GLKModel_obj,
                                 (jint) banner_handle, (jint) siz, (jint) siz_units,
                                 (jint) is_advisory);
}

void tadsban_size_to_contents(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_size_to_contents");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_size_to_contents, GLKModel_obj,
                                 (jint) banner_handle);
}

void tadsban_start_html(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_start_html");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_start_html, GLKModel_obj,
                                 (jint) banner_handle);
}

void tadsban_end_html(void *banner_handle) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_end_html");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_end_html, GLKModel_obj,
                                 (jint) banner_handle);
}

void tadsban_goto(void *banner_handle, int row, int col) {
  #ifdef JNI_DEBUG
    __android_log_write(ANDROID_LOG_DEBUG, "glk.c", "tadsban_goto");
  #endif
  (*env)->CallStaticVoidMethod(env, GLKController_class, m_tadsban_goto, GLKModel_obj,
                                 (jint) banner_handle);
}

