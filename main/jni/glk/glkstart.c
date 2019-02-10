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
#include <string.h>
#include <stdio.h>
#include <unistd.h>
#include <stdlib.h>
#include <setjmp.h>
#include <android/log.h>

/* Need dlfcn.h for the routines to
   dynamically load libraries */
#include <dlfcn.h>

#include "glk.h"
#include "glkstart.h"
#include "gi_blorb.h"

static glkunix_startup_t startdata;

extern JNIEnv * env;
extern jmp_buf term_buf;

extern jclass GLKModel_class;
extern jobject GLKModel_obj;
extern jclass GLKController_class;

extern jclass GLKEvent_class;
extern jobject GLKEvent_obj;
extern jfieldID ev_type;
extern jfieldID ev_win;
extern jfieldID ev_val1;
extern jfieldID ev_val2;

extern jclass Point_class;
extern jobject Point_obj;
extern jfieldID pt_x;
extern jfieldID pt_y;

extern jclass TADSBannerInfo_class;
extern jobject TADSBannerInfo_obj;
extern jfieldID ban_align;
extern jfieldID ban_style;
extern jfieldID ban_rows;
extern jfieldID ban_columns;
extern jfieldID ban_pix_width;
extern jfieldID ban_pix_height;
extern jfieldID ban_os_line_wrap;

#define MID(fname) \
  extern jmethodID m_ ##fname;

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

static int sout = -1;
static int serr = -1;

void getGlobalRef(JNIEnv * e, const char* clazz, jobject* globalObj, jclass* globalClass) {
  jclass cls = (*e)->FindClass(e, clazz);
  if (cls) {
     *globalClass = (*e)->NewGlobalRef(e, cls);
     jmethodID meth = (*e)->GetMethodID(e, cls, "<init>", "()V");
     if (meth) {
        jobject obj = (*e)->NewObject(e, cls, meth);
        *globalObj = (*e)->NewGlobalRef(e, obj);
        (*e)->DeleteLocalRef(e, cls);
        (*e)->DeleteLocalRef(e, obj);
     } else {
        (*e)->DeleteLocalRef(e, cls);
        printf("glkstart.c: getGlobalRef: didn't find constructor.\n");
     }
  } else {
    printf("glkstart.c: getGlobalRef: didn't find class.\n");
  }
}

jint JNI_OnLoad(JavaVM* vm, void* reserved)
{
  if ((*vm)->GetEnv(vm, (void**)(&env), JNI_VERSION_1_6) != JNI_OK) {
    printf("glkstart.c: JNI__OnLoad: failed to update env...\n");
    return -1;
  }

  // Get jclass with env->FindClass.
  // Register methods with env->RegisterNatives.

  printf("glkstart.c: JNI__OnLoad: initialising cterp global refs...\n");

  /* Cache commonly used methods, etc. in global variables */
  jclass tmp1 = (*env)->FindClass(env, "com/luxlunae/glk/model/GLKModel");
  GLKModel_class = (*env)->NewGlobalRef(env, tmp1);
  (*env)->DeleteLocalRef(env, tmp1);
  jclass tmp2 = (*env)->FindClass(env, "com/luxlunae/glk/controller/GLKController");
  GLKController_class = (*env)->NewGlobalRef(env, tmp2);
  (*env)->DeleteLocalRef(env, tmp2);

  /* The two main GLK data structures and their fields - we reuse these objects as needed */
  getGlobalRef(env, "com/luxlunae/glk/controller/GLKEvent", &GLKEvent_obj, &GLKEvent_class);
  ev_type = (*env)->GetFieldID(env, GLKEvent_class, "type", "I");
  ev_win = (*env)->GetFieldID(env, GLKEvent_class, "win", "I");
  ev_val1 = (*env)->GetFieldID(env, GLKEvent_class, "val1", "I");
  ev_val2 = (*env)->GetFieldID(env, GLKEvent_class, "val2", "I");

  getGlobalRef(env, "android/graphics/Point", &Point_obj, &Point_class);
  pt_x = (*env)->GetFieldID(env, Point_class, "x", "I");
  pt_y = (*env)->GetFieldID(env, Point_class, "y", "I");

  getGlobalRef(env, "com/luxlunae/glk/model/TADSBannerInfo", &TADSBannerInfo_obj, &TADSBannerInfo_class);
  ban_align = (*env)->GetFieldID(env, TADSBannerInfo_class, "align", "I");
  ban_style = (*env)->GetFieldID(env, TADSBannerInfo_class, "style", "J");
  ban_rows = (*env)->GetFieldID(env, TADSBannerInfo_class, "rows", "I");
  ban_columns = (*env)->GetFieldID(env, TADSBannerInfo_class, "columns", "I");
  ban_pix_width = (*env)->GetFieldID(env, TADSBannerInfo_class, "pix_width", "I");
  ban_pix_height = (*env)->GetFieldID(env, TADSBannerInfo_class, "pix_height", "I");
  ban_pix_height = (*env)->GetFieldID(env, TADSBannerInfo_class, "pix_height", "I");
  ban_os_line_wrap = (*env)->GetFieldID(env, TADSBannerInfo_class, "os_line_wrap", "I");

  /* Initialise methods */
  #define MID(fname, sig, suf) \
    m_ ## fname ## suf = (*env)->GetStaticMethodID(env, GLKController_class, #fname, #sig); \
    if (m_ ## fname ## suf == NULL) { \
      __android_log_write(ANDROID_LOG_ERROR, "glk", "failed to get method ID for " #fname); \
    }

  MID(glk_window_iterate, (Lcom/luxlunae/glk/model/GLKModel;I[I)I,);
  MID(glk_window_open, (Lcom/luxlunae/glk/model/GLKModel;IIIII)I,);
  MID(glk_window_close, (Lcom/luxlunae/glk/model/GLKModel;I[J)V,);
  MID(glk_window_get_size, (Lcom/luxlunae/glk/model/GLKModel;I)Landroid/graphics/Point;,);
  MID(glk_window_set_arrangement, (Lcom/luxlunae/glk/model/GLKModel;IIII)V,);
  MID(glk_window_get_arrangement, (Lcom/luxlunae/glk/model/GLKModel;I[I[I[I)V,);
  MID(glk_window_get_parent, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_window_get_root, (Lcom/luxlunae/glk/model/GLKModel;)I,);
  MID(glk_window_clear, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_window_move_cursor, (Lcom/luxlunae/glk/model/GLKModel;III)V,);
  MID(glk_window_get_stream, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_window_fill_rect, (Lcom/luxlunae/glk/model/GLKModel;IIIIII)V,);
  MID(glk_window_set_echo_stream, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glk_set_window, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_exit, (Lcom/luxlunae/glk/model/GLKModel;)V,);
  MID(glk_select, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/controller/GLKEvent;)V,);
  MID(glk_tick, (Lcom/luxlunae/glk/model/GLKModel;)V,);
  MID(glk_set_terminators_line_event, (Lcom/luxlunae/glk/model/GLKModel;I[J)V,);
  MID(glk_image_get_info, (Lcom/luxlunae/glk/model/GLKModel;ILandroid/graphics/Point;)Z,);
  MID(glk_image_draw_scaled, (Lcom/luxlunae/glk/model/GLKModel;IIIIII)Z,);
  MID(glk_image_draw, (Lcom/luxlunae/glk/model/GLKModel;IIII)Z,);
  MID(glk_stream_set_current, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_stream_get_current, (Lcom/luxlunae/glk/model/GLKModel;)I,);
  MID(glk_put_string, (Lcom/luxlunae/glk/model/GLKModel;[BZ)V,);

  MID(glk_put_buffer, (Lcom/luxlunae/glk/model/GLKModel;[BIZ)V,);

  MID(glk_put_char, (Lcom/luxlunae/glk/model/GLKModel;IZ)V,);

  MID(glk_stream_get_position, (Lcom/luxlunae/glk/model/GLKModel;I)J,);
  MID(glk_stream_set_position, (Lcom/luxlunae/glk/model/GLKModel;IJI)V,);

  MID(glk_get_buffer_stream, (Lcom/luxlunae/glk/model/GLKModel;I[BIZ)I,);

  MID(glk_schannel_create, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_schannel_destroy, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_schannel_play, (Lcom/luxlunae/glk/model/GLKModel;II)Z,);
  MID(glk_schannel_set_volume, (Lcom/luxlunae/glk/model/GLKModel;IJ)V,);
  MID(glk_schannel_play_ext, (Lcom/luxlunae/glk/model/GLKModel;IIII)Z,);
  MID(glk_schannel_play_multi, (Lcom/luxlunae/glk/model/GLKModel;[I[II)I,);
  MID(glk_schannel_stop, (Lcom/luxlunae/glk/model/GLKModel;I)V,);

  MID(glk_put_buffer_stream, (Lcom/luxlunae/glk/model/GLKModel;I[BIZ)V,);

  MID(glk_put_char_stream, (Lcom/luxlunae/glk/model/GLKModel;IIZ)V,);

  MID(glk_get_char_stream, (Lcom/luxlunae/glk/model/GLKModel;IZ)I,);
  MID(glk_put_string_stream, (Lcom/luxlunae/glk/model/GLKModel;I[BZ)V,);
  MID(glk_fileref_create_by_prompt, (Lcom/luxlunae/glk/model/GLKModel;III)I,);
  MID(glk_fileref_create_by_name, (Lcom/luxlunae/glk/model/GLKModel;I[BI)I,);
  MID(glk_fileref_does_file_exist, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_fileref_destroy, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_stream_iterate, (Lcom/luxlunae/glk/model/GLKModel;I[I)I,);
  MID(glk_fileref_iterate, (Lcom/luxlunae/glk/model/GLKModel;I[I)I,);
  MID(glk_schannel_iterate, (Lcom/luxlunae/glk/model/GLKModel;I[I)I,);
  MID(glk_stream_open_file, (Lcom/luxlunae/glk/model/GLKModel;IIIZ)I,);

  MID(glk_stream_open_memory, (Lcom/luxlunae/glk/model/GLKModel;Ljava/nio/ByteBuffer;IIZ)I,);

  MID(glk_stream_close, (Lcom/luxlunae/glk/model/GLKModel;I[J)V,);
  MID(glk_set_style, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_set_style_stream, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glk_request_timer_events, (Lcom/luxlunae/glk/model/GLKModel;J)V,);
  MID(glk_stylehint_set, (Lcom/luxlunae/glk/model/GLKModel;IIII)V,);
  MID(glk_request_char_event, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_cancel_char_event, (Lcom/luxlunae/glk/model/GLKModel;I)V,);

  MID(glk_request_line_event, (Lcom/luxlunae/glk/model/GLKModel;ILjava/nio/ByteBuffer;IZ)V,);

  MID(glk_cancel_line_event, (Lcom/luxlunae/glk/model/GLKModel;ILcom/luxlunae/glk/controller/GLKEvent;)V,);
  MID(glk_gestalt, (Lcom/luxlunae/glk/model/GLKModel;JJ)J,);
  MID(glk_gestalt_ext, (Lcom/luxlunae/glk/model/GLKModel;JJ[J)J,);
  MID(glk_current_time, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/model/GLKTime;)V,);
  MID(glk_current_simple_time, (Lcom/luxlunae/glk/model/GLKModel;J)I,);
  MID(glk_time_to_date_utc, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/model/GLKTime;Lcom/luxlunae/glk/model/GLKDate;)V,);
  MID(glk_time_to_date_local, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/model/GLKTime;Lcom/luxlunae/glk/model/GLKDate;)V,);
  MID(glk_simple_time_to_date_utc, (Lcom/luxlunae/glk/model/GLKModel;IJLcom/luxlunae/glk/model/GLKDate;)V,);
  MID(glk_simple_time_to_date_local, (Lcom/luxlunae/glk/model/GLKModel;IJLcom/luxlunae/glk/model/GLKDate;)V,);
  MID(glk_date_to_time_utc, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/model/GLKDate;Lcom/luxlunae/glk/model/GLKTime;)V,);
  MID(glk_date_to_time_local, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/model/GLKDate;Lcom/luxlunae/glk/model/GLKTime;)V,);
  MID(glk_date_to_simple_time_utc, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/model/GLKDate;J)I,);
  MID(glk_date_to_simple_time_local, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/model/GLKDate;J)I,);

  MID(glk_buffer_to_lower_case_uni, (Lcom/luxlunae/glk/model/GLKModel;Ljava/nio/ByteBuffer;I)I,);
  MID(glk_buffer_to_upper_case_uni, (Lcom/luxlunae/glk/model/GLKModel;Ljava/nio/ByteBuffer;I)I,);
  MID(glk_buffer_to_title_case_uni, (Lcom/luxlunae/glk/model/GLKModel;Ljava/nio/ByteBuffer;IZ)I,);
  MID(glk_buffer_canon_decompose_uni, (Lcom/luxlunae/glk/model/GLKModel;Ljava/nio/ByteBuffer;I)I,);
  MID(glk_buffer_canon_normalize_uni, (Lcom/luxlunae/glk/model/GLKModel;Ljava/nio/ByteBuffer;I)I,);

  MID(glk_char_to_lower, (Lcom/luxlunae/glk/model/GLKModel;C)C,);
  MID(glk_char_to_upper, (Lcom/luxlunae/glk/model/GLKModel;C)C,);
  MID(glk_window_get_rock, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_window_get_type, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_window_get_echo_stream, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_window_get_sibling, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_stream_get_rock, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_fileref_create_temp, (Lcom/luxlunae/glk/model/GLKModel;II)I,);
  MID(glk_fileref_get_rock, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_fileref_delete_file, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_fileref_create_from_fileref, (Lcom/luxlunae/glk/model/GLKModel;III)I,);

  MID(glk_get_line_stream, (Lcom/luxlunae/glk/model/GLKModel;I[BIZ)I,);

  MID(glk_stylehint_clear, (Lcom/luxlunae/glk/model/GLKModel;III)V,);

  MID(glk_style_distinguish, (Lcom/luxlunae/glk/model/GLKModel;III)I,);
  MID(glk_style_measure, (Lcom/luxlunae/glk/model/GLKModel;III[I)I,);

  MID(glk_select_poll, (Lcom/luxlunae/glk/model/GLKModel;Lcom/luxlunae/glk/controller/GLKEvent;)V,);
  MID(glk_request_mouse_event, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_cancel_mouse_event, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_window_flow_break, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_window_erase_rect, (Lcom/luxlunae/glk/model/GLKModel;IIIII)V,);
  MID(glk_window_set_background_color, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glk_schannel_get_rock, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(glk_sound_load_hint, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glk_schannel_create_ext, (Lcom/luxlunae/glk/model/GLKModel;IJ)I,);
  MID(glk_schannel_set_volume_ext, (Lcom/luxlunae/glk/model/GLKModel;IJII)V,);
  MID(glk_schannel_pause, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_schannel_unpause, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_set_hyperlink, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_set_hyperlink_stream, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glk_request_hyperlink_event, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_cancel_hyperlink_event, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glk_set_echo_line_event, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glk_stream_open_resource, (Lcom/luxlunae/glk/model/GLKModel;IIZ)I,);

  MID(glkplus_stream_open_pathname, (Lcom/luxlunae/glk/model/GLKModel;[BZI)I,)
  MID(glkplus_fileref_get_name, (Lcom/luxlunae/glk/model/GLKModel;I)[B,)
  MID(glkplus_set_zcolors, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glkplus_set_zcolors_stream, (Lcom/luxlunae/glk/model/GLKModel;III)V,);
  MID(glkplus_set_reversevideo, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(glkplus_set_reversevideo_stream, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(glkplus_set_html_mode, (Lcom/luxlunae/glk/model/GLKModel;Z)V,);
  MID(glkplus_new_html_page, (Lcom/luxlunae/glk/model/GLKModel;I)V,);

  MID(tads_get_input, (Lcom/luxlunae/glk/model/GLKModel;[BIII)I,);
  MID(tads_put_string, (Lcom/luxlunae/glk/model/GLKModel;[B)V,);
  MID(tads_put_string_stream, (Lcom/luxlunae/glk/model/GLKModel;[BI)V,);
  MID(tadsban_create, (Lcom/luxlunae/glk/model/GLKModel;IIIIIIIJ)I,);
  MID(tadsban_get_charwidth, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(tadsban_get_charheight, (Lcom/luxlunae/glk/model/GLKModel;I)I,);
  MID(tadsban_getinfo, (Lcom/luxlunae/glk/model/GLKModel;ILcom/luxlunae/glk/model/TADSBannerInfo;)I,);
  MID(tadsban_delete, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(tadsban_orphan, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(tadsban_clear, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(tadsban_set_attr, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(tadsban_set_color, (Lcom/luxlunae/glk/model/GLKModel;III)V,);
  MID(tadsban_set_screen_color, (Lcom/luxlunae/glk/model/GLKModel;II)V,);
  MID(tadsban_flush, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(tadsban_set_size, (Lcom/luxlunae/glk/model/GLKModel;IIII)V,);
  MID(tadsban_size_to_contents, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(tadsban_start_html, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(tadsban_end_html, (Lcom/luxlunae/glk/model/GLKModel;I)V,);
  MID(tadsban_goto, (Lcom/luxlunae/glk/model/GLKModel;III)V,);
  #undef MID

  return JNI_VERSION_1_6;
}

void JNI_OnUnload(JavaVM *vm, void *reserved)
{
  if ((*vm)->GetEnv(vm, (void**)(&env), JNI_VERSION_1_6) != JNI_OK) {
    printf("glkstart.c: JNI__OnUnload: failed to update env...\n");
    return;
  }

  (*env)->DeleteGlobalRef(env, GLKModel_class);
  (*env)->DeleteGlobalRef(env, GLKModel_obj);
  (*env)->DeleteGlobalRef(env, GLKController_class);
  (*env)->DeleteGlobalRef(env, GLKEvent_class);
  (*env)->DeleteGlobalRef(env, GLKEvent_obj);
  (*env)->DeleteGlobalRef(env, Point_class);
  (*env)->DeleteGlobalRef(env, Point_obj);
  (*env)->DeleteGlobalRef(env, TADSBannerInfo_class);
  (*env)->DeleteGlobalRef(env, TADSBannerInfo_obj);
}

JNIEXPORT void JNICALL Java_com_luxlunae_glk_controller_GLKController_freeByteBuffer
  (JNIEnv * e, jclass jc, jobject bb) {
  unwrapByteBuffer(e, bb);
}

void cleanup() {
  for (int i = 0; i < startdata.argc; i++) {
    free(startdata.argv[i]);
  }
  free(startdata.argv);
  freeByteBuffers();

  /* restore stdout and stderr */
  fflush(NULL);
  if (sout != -1) {
    dup2(sout, fileno(stdout));
    close(sout);
    sout = -1;
  }
  if (serr != -1) {
    dup2(serr, fileno(stderr));
    close(serr);
    serr = -1;
  }
}

static t_find_resource tads_find_resource;

JNIEXPORT jint JNICALL Java_com_luxlunae_glk_controller_GLKController_runTerp
  (JNIEnv * e, jobject jc, jstring terpLibName, jobject glkModel, jobjectArray args, jstring outFilePath) {
  /* This is the main entry point for running the terp
   * It will return one of the following values:
   *   1. the terp calls glk_exit()
   *   2. the user forces the terp to quit (which results in a call from Java to stopTerp())
   *   3. Android tells the GLKActivity to stop (e.g. because memory is runnning out)
   *   4. the terp calls exit(), usually because of a fatal error
   *   5. a signal is sent (e.g. SIGSEGV) just prior to the terp crashing - we
   *      try to detect and gracefully deal with this as best we can, but of course there 
   *      are some situations where we may not be able to (TODO)
   */
  int i;
  const char *error;
  void *terp;
  g_main terp_main;
  g_startup_code terp_startup_code;

  if (outFilePath != NULL) {
    const char *outFile = (*e)->GetStringUTFChars(e, outFilePath, 0);

    /* redirect stdout and stderr to the logging file */
    sout = dup(fileno(stdout));
    serr = dup(fileno(stderr));
    if (freopen(outFile, "w", stdout) == NULL) {
      __android_log_write(ANDROID_LOG_ERROR, "glkstart.c", "runTerp: could not redirect stdout");
      sout = -1;
    }
    if (freopen(outFile, "a", stderr) == NULL) {
      __android_log_write(ANDROID_LOG_ERROR, "glkstart.c", "runTerp: could not redirect stderr.");
      serr = -1;
    }
  }

  const char *libName = (*e)->GetStringUTFChars(e, terpLibName, 0);

  /* Load dynamically loaded library */
  terp = dlopen(libName, RTLD_LAZY);

  (*e)->ReleaseStringUTFChars(e, terpLibName, libName);

  if (!terp) {
   fprintf(stderr, "Couldn't open terp plugin: %s\n", dlerror());
   cleanup();
    return 5;
  }
 
  /* Get symbols */
  dlerror();
  terp_main = dlsym(terp, "glk_main");
  if ((error = dlerror())) {
   fprintf(stderr, "Couldn't find glk_main: %s\n", error);
   cleanup();
   return 5;
  }
  terp_startup_code = dlsym(terp, "glkunix_startup_code");
  if ((error = dlerror())) {
   fprintf(stderr, "Couldn't find glkunix_startup_code: %s\n", error);
   cleanup();
   return 5;
  }
  tads_find_resource = dlsym(terp, "tads_find_resource_fp");
  dlerror();  /* don't worry at this stage if we can't find the tads function - we only need it for TADS games */

  /* Ensure the global environment variable is pointing to the terp thread */
  env = e;
  GLKModel_obj = (*e)->NewGlobalRef(e, glkModel);
  initByteBuffers(5000);

  /* Get the arguments */
  startdata.argc = (*e)->GetArrayLength(e, args);
  startdata.argv = malloc(startdata.argc * sizeof(char*));
  const char *a;
  
  printf("===========\nRunning ");
  for (i = 0; i < startdata.argc; i++) {
    jstring arg = (jstring) ((*e)->GetObjectArrayElement(e, args, i));
    a = (*e)->GetStringUTFChars(e, arg, 0);
    startdata.argv[i] = strdup(a);
    (*e)->ReleaseStringUTFChars(e, arg, a);
    printf("%s ", startdata.argv[i]);
  }
  printf("\n===========\n\n");

  /* N.B. it's unsafe to assign the return value of setjmp - according to C99 spec
   * the below should work.
   * Thanks https://stackoverflow.com/questions/22893178/how-to-safely-get-the-return-value-of-setjmp
   * Also need to take care re any local variables that have changed between the setjmp call and the
   * longjmp call, in our case we only call cleanup() after the longjmp, which uses global variables
   * and hence should be safe */
  switch (setjmp(term_buf)) {
    case 0:
      /* Call the relevant terp - we assume it has already been loaded in Java
       through a call to LoadLibrary */
      if (terp_startup_code(&startdata)) {
        terp_main();
      }
      glk_exit();
      printf("===========\nStopping: unrecognised reason\n===========\n\n");
      cleanup();        /* shouldn't get here */
      dlclose(terp);
      return (jint)-1;
    case 1:
      printf("===========\nStopping: glk_exit() called\n===========\n\n");
      cleanup();
      dlclose(terp);
      return (jint)1;   /* glk_exit() */
    case 2:
      printf("===========\nStopping: user forced quit\n===========\n\n");
      cleanup();
      dlclose(terp);
      return (jint)2;   /* user force quit */
    case 3:
      printf("===========\nStopping: Android forced quit\n===========\n\n");
      cleanup();
      dlclose(terp);
      return (jint)3;   /* Android force quit */
    case 4:
      printf("===========\nStopping: terp called exit()\n===========\n\n");
      cleanup();
      dlclose(terp);
      return (jint)4;   /* terp called exit() - probably an error */
    case 5:
      printf("===========\nStopping: terp has crashed\n===========\n\n");
      cleanup();
      dlclose(terp);
      return (jint)5;   /* terp about to crash, try to gracefully handle the signal */
    default:
      /* Unrecognised return code */
      printf("===========\nStopping: unrecognised reason\n===========\n\n");
      cleanup();
      dlclose(terp);
      return (jint)-1;
  } 
}

JNIEXPORT void JNICALL Java_com_luxlunae_glk_controller_GLKController_stopTerp
  (JNIEnv * e, jobject jc, jint code) {
  /* called by Java layer to forcefully stop the terp.
   * currently code is either 2 (user initiated) or 3 (Android OS initiated) */
  longjmp(term_buf, (int)code);
}

void ___wrap_exit(int status) {
  /* wrap exit calls and send them back to the Java layer
   * so can gracefully terminate process and messages
   * correctly relayed back to user. */
  longjmp(term_buf, 4);
}

/* Blorb calls */
static giblorb_map_t *blorbmap = NULL;
static jbyteArray chunkData;
static glui32 chunkNum;

giblorb_err_t giblorb_set_resource_map(strid_t file)
{
    giblorb_err_t err;

    err = giblorb_create_map(file, &blorbmap);
    if (err)
    {
        blorbmap = NULL;
        return err;
    }

    return giblorb_err_None;
}

giblorb_map_t *giblorb_get_resource_map()
{
    return blorbmap;
}

JNIEXPORT jbyteArray JNICALL
Java_com_luxlunae_glk_model_GLKResourceManager_getBlorbResource(JNIEnv *e, jobject jc,
                                                   jint method, jintArray result, jint usage, jint resnum)
{
  giblorb_result_t res;
  giblorb_err_t err;

  if (blorbmap == NULL) {
    /* no blorbmap associated with this game */
    return NULL;
  }

  /* find the resource */
  err = giblorb_load_resource(blorbmap, (glui32)method, &res, (glui32)usage, (glui32)resnum);
  if (err)
  {
    return NULL;
  }

  if (result != NULL) {
      /* store the chunk information (result array should have at least 2 elements) */
      jsize len = (*e)->GetArrayLength(e, result);
      if (len > 1) {
        jint *resBody = (*e)->GetIntArrayElements(e, result, 0);
        resBody[0] = (jint)res.chunknum;
        resBody[1] = (jint)res.chunktype;
        (*e)->ReleaseIntArrayElements(e, result, resBody, 0);
      }
  }

  /* send the chunk data back to the Java layer (or NULL if error/no data) */
  jbyteArray arr = (*e)->NewByteArray(e, res.length);
  if (arr != NULL) {
    chunkData = (*e)->NewGlobalRef(e, arr);
    (*e)->DeleteLocalRef(e, arr);
    (*e)->SetByteArrayRegion(e, chunkData, 0, res.length, (const jbyte*)res.data.ptr);
    chunkNum = res.chunknum;
  }
  
  return chunkData;
}

JNIEXPORT void JNICALL Java_com_luxlunae_glk_model_GLKResourceManager_freeBlorbResource(JNIEnv *e, jobject jc) {
  if (chunkData != NULL) {
    (*e)->DeleteGlobalRef(e, chunkData);
    chunkData = NULL;
    giblorb_unload_chunk(blorbmap, chunkNum);
  }
}

/* Additional terp-specific functions */

static jbyteArray tadsData;

char* tads_load_resource(const char *fname, const char *resname,
                       tads_resinfo *info) {
    FILE *fp;
    int found;

    if (tads_find_resource == NULL) {
        /* we didn't find this function when the terp loaded */
        fprintf(stderr, "Can't load TADS resource - could not dynamically load function.\n");
        return NULL;
    }

    /* open the file */
    if ((fp = fopen(fname, "rb")) == 0)
    {
        /* we couldn't open the file, so there's no resource to be found */
        return NULL;
    }

    /* find the resource */
    if (!tads_find_resource(fp, resname, info))
    {
      fclose(fp);
      return NULL;
    }

    /* seek to the resource */
    fseek(fp, info->seek_pos, 0);
    char* data = malloc(info->siz);
    fread(data, info->siz, 1, fp);

    /* we're done with the file - close it */
    fclose(fp);

    /* return our found or not-found indication */
    return data;
}

JNIEXPORT jbyteArray JNICALL Java_com_luxlunae_glk_model_GLKResourceManager_getTADSResource
  (JNIEnv * e, jobject jc, jstring resname) {
  // attempt to load a TADS resource
  // return the bytes of the resource or NULL if not found
  // when finished with the resource in the Java layer, you should free it by
  // calling freeTADSResource()
  char * respath = (char *)((*e)->GetStringUTFChars(e, resname, NULL));
  tads_resinfo inf;
  const char* gamepath = startdata.argv[startdata.argc - 1];
  char* data = NULL;

  /* 1) First try to find a resource file .rsN (TADS2) or .3rN (TADS3), beginning 
     with N=9 until N=0. If successful, use that. */
  
  /* TODO */
  
  if (data == NULL) {
    /* 2) OK, step 1 wasn't successful, try to find the resource in the game file instead */
    data = tads_load_resource(gamepath, respath, &inf);
  }

  (*e)->ReleaseStringUTFChars(e, resname, respath);

  /* send the data back to the Java layer (or NULL if error/no data) */
  if (data == NULL) return NULL;
  jbyteArray arr = (*e)->NewByteArray(e, inf.siz);
  if (arr != NULL) {
    tadsData = (*e)->NewGlobalRef(e, arr);
    (*e)->DeleteLocalRef(e, arr);
    (*e)->SetByteArrayRegion(e, tadsData, 0, inf.siz, (const jbyte*)data);
  }
  free(data);

  return tadsData;
}

JNIEXPORT void JNICALL Java_com_luxlunae_glk_model_GLKResourceManager_freeTADSResource
  (JNIEnv * e, jobject jc) {
  if (tadsData != NULL) {
    (*e)->DeleteGlobalRef(e, tadsData);
    tadsData = NULL;
  }
}

