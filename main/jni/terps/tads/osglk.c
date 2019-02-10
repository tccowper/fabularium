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
#include <assert.h>

#include "os.h"
#include "glk.h"
#include <android/log.h>

//#define DEBUG_TADS

/* status mode flag */
static int status_mode_ = 0;

/* HTML mode flag */
static int html_mode_ = 0;

/*
 *   Buffer containing score information for status line 
 */
static char status_score_buf_[256];

winid_t mainwin;

extern void fab_set_html_mode(int on);

/* ------------------------------------------------------------------------ */
/*
 *   Display routines.
 *
 */

/*
 *   Print a string on the console.  These routines come in two varieties:
 *   
 *   os_printz - write a NULL-TERMINATED string
 *.  os_print - write a COUNTED-LENGTH string, which may not end with a null
 *   
 *   These two routines are identical except that os_printz() takes a string
 *   which is terminated by a null byte, and os_print() instead takes an
 *   explicit length, and a string that may not end with a null byte.
 *   
 *   os_printz(str) may be implemented as simply os_print(str, strlen(str)).
 *   
 *   The string is written in one of three ways, depending on the status mode
 *   set by os_status():
 *   
 *   status mode == 0 -> write to main text window
 *.  status mode == 1 -> write to status line
 *.  anything else -> do not display the text at all
 *   
 *   Implementations are free to omit any status line support, in which case
 *   they should simply suppress all output when the status mode is anything
 *   other than zero.
 *   
 *   The following special characters must be recognized in the displayed
 *   text:
 *   
 *   '\n' - newline: end the current line and move the cursor to the start of
 *   the next line.  If the status line is supported, and the current status
 *   mode is 1 (i.e., displaying in the status line), then two special rules
 *   apply to newline handling: newlines preceding any other text should be
 *   ignored, and a newline following any other text should set the status
 *   mode to 2, so that all subsequent output is suppressed until the status
 *   mode is changed with an explicit call by the client program to
 *   os_status().
 *   
 *   '\r' - carriage return: end the current line and move the cursor back to
 *   the beginning of the current line.  Subsequent output is expected to
 *   overwrite the text previously on this same line.  The implementation
 *   may, if desired, IMMEDIATELY clear the previous text when the '\r' is
 *   written, rather than waiting for subsequent text to be displayed.
 *   
 *   All other characters may be assumed to be ordinary printing characters.
 *   The routine need not check for any other special characters.
 *   
 */
void os_printz(const char *str) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_printz");
#endif
    /* from oshtml.cpp */

    /* use our base counted-length writer */
    os_print(str, strlen(str));
}

void os_print(const char *str, size_t len) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_print");
#endif
    /* from oshtml.cpp */
    if (!str) {  return; }
    
    switch (status_mode_) {
        case 0:
            /* display the entire string normally */
            break;
        case 1:
            /* skip any leading newlines */
            for ( ; len != 0 && *str == '\n' ; ++str, --len) ;

            /* stop at the first newline after any other characters */
            if (len != 0) {
                const char *p;
                size_t rem;

                /* scan for a newline */
                for (p = str, rem = len ; rem != 0 && *p != '\n' ; ++p, --rem) ;

                /* if we found one, note it */
                if (rem != 0 && *p == '\n') {
                    /* switch to status mode 2 for subsequent output */
                    status_mode_ = 2;

                    /* display only the part before the newline */
                    len = p - str;
                }
            }
            break;
        case 2:
        default:
            /* 
             *   suppress everything in status mode 2 - this is the part after
             *   the initial line of status text, which is hidden until we
             *   explicitly return to the main text area by switching to status
             *   mode 0 
             */
            return;
    }

    /* display the string */
    if (len > 0) {
        tads_put_string(str, len);
    }
}

void oshtml_dbg_vprintf(const char *fmt, va_list argptr)
{
    char buf[1024];
    char *p;
    
    /* format the message */
    vsprintf(buf, fmt, argptr);

    /* 
     *   Remove any bold on/off sequences from the buffer.  Bold sequences
     *   are interpreted by the HTML parser as tag open/close sequences,
     *   so they can cause weird problems. 
     */
    for (p = buf ; *p != '\0' ; ++p)
    {
        /* if it's a bold on/off sequence, convert it to a space */
        if (*p == 1 || *p == 2)
            *p = ' ';
    }

    /* display it */
    __android_log_write(ANDROID_LOG_DEBUG, "TADS debugger: oshtml.cpp", buf);
}

/*
 *   printf to debug log window
 */
void oshtml_dbg_printf(const char *fmt, ...)
{
    /* from oshtml.cpp */
    va_list argptr;

    va_start(argptr, fmt);
    oshtml_dbg_vprintf(fmt, argptr);
    va_end(argptr);
}

/*
 *   Print to the debugger console.  These routines are for interactive
 *   debugger builds only: they display the given text to a separate window
 *   within the debugger UI (separate from the main game command window)
 *   where the debugger displays status information specific to the debugging
 *   session (such as compiler/build output, breakpoint status messages,
 *   etc).  For example, TADS Workbench on Windows displays these messages in
 *   its "Debug Log" window.
 *   
 *   These routines only need to be implemented for interactive debugger
 *   builds, such as TADS Workbench on Windows.  These can be omitted for
 *   regular interpreter builds.  
 */
void os_dbg_printf(const char *fmt, ...)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_dbg_printf");
#endif
    /* from oshtml.cpp */
    va_list argptr;

    va_start(argptr, fmt);
    oshtml_dbg_vprintf(fmt, argptr);
    va_end(argptr);
}

void os_dbg_vprintf(const char *fmt, va_list argptr)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_dbg_vprintf");
#endif
    /* from oshtml.cpp */
    oshtml_dbg_vprintf(fmt, argptr);
}

/*
 *   internal routine to set the status mode 
 */
static void oshtml_status_mode(int flag)
{
    /* from oshtml.cpp */
    static const char banner_start[] =
        "<banner id=statusline height=previous border>"
        "<body bgcolor=statusbg text=statustext><b>";
    static const char banner_score[] = "</b><tab align=right><i>";
    static const char banner_end[] = "</i><br height=0></banner>";

    /* start or end the status line banner */
    if (flag) {
        /* start the banner */
        fab_set_html_mode(1);
        glk_put_string(banner_start);
    } else {
        /* add the score to the banner */
        glk_put_string(banner_score);
        glk_put_string(status_score_buf_);
        glk_put_string(banner_end);
        fab_set_html_mode(0);
    }
}

/* 
 *   Set the status line mode.  There are three possible settings:
 *   
 *   0 -> main text mode.  In this mode, all subsequent text written with
 *   os_print() and os_printz() is to be displayed to the main text area.
 *   This is the normal mode that should be in effect initially.  This mode
 *   stays in effect until an explicit call to os_status().
 *   
 *   1 -> statusline mode.  In this mode, text written with os_print() and
 *   os_printz() is written to the status line, which is usually rendered as
 *   a one-line area across the top of the terminal screen or application
 *   window.  In statusline mode, leading newlines ('\n' characters) are to
 *   be ignored, and any newline following any other character must change
 *   the mode to 2, as though os_status(2) had been called.
 *   
 *   2 -> suppress mode.  In this mode, all text written with os_print() and
 *   os_printz() must simply be ignored, and not displayed at all.  This mode
 *   stays in effect until an explicit call to os_status().  
 */
void os_status(int stat) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_status");
#endif
    /* from oshtml.cpp */

    /* 
     *   If we're in HTML mode, don't do any automatic status line
     *   generation -- let the game do the status line the way it wants,
     *   without any predefined handling from the run-time system 
     */
    if (html_mode_)
        return;

    /* see what mode we're setting */
    switch(stat) {
        case 0:
        default:
            /* turn off status line mode */
            status_mode_ = 0;
            oshtml_status_mode(0);
            break;
        case 1:
            /* turn on status line mode */
            status_mode_ = 1;
            oshtml_status_mode(1);
            break;
    }
}

/* get the status line mode */
int os_get_status() {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_get_status");
#endif
    /* from oshtml.cpp */
    return status_mode_;
}

/* 
 *   Set the score value.  This displays the given score and turn counts on
 *   the status line.  In most cases, these values are displayed at the right
 *   edge of the status line, in the format "score/turns", but the format is
 *   up to the implementation to determine.  In most cases, this can simply
 *   be implemented as follows:
 *   
 *.  void os_score(int score, int turncount)
 *.  {
 *.     char buf[40];
 *.     sprintf(buf, "%d/%d", score, turncount);
 *.     os_strsc(buf);
 *.  }
 */
void os_score(int score, int turncount) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_score");
#endif
    /* from oshtml.cpp */
    char buf[128];
    
    /* build the default status line score format */
    sprintf(buf, "%d/%d", score, turncount);

    /* set the score string */
    os_strsc(buf);
}

/* display a string in the score area in the status line */
void os_strsc(const char *p) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_strsc");
#endif
    /* from oshtml.cpp */

    /* 
     *   remember the score string - it will be used the next time we
     *   rebuild the status line 
     */
    int len = strlen(p);
    if (len > sizeof(status_score_buf_))
        len = sizeof(status_score_buf_) - sizeof(status_score_buf_[0]);
    memcpy(status_score_buf_, p, len);
    status_score_buf_[len / sizeof(status_score_buf_[0])] = '\0';
}

/* clear the screen */
void oscls(void) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "oscls");
#endif
    fab_new_html_page(mainwin);
}

/* redraw the screen */
void os_redraw(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_redraw");
#endif
  /* do nothing */
}

/* flush any buffered display output */
void os_flush(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_flush");
#endif
  /* do nothing */
}

/*
 *   Update the display - process any pending drawing immediately.  This
 *   only needs to be implemented for operating systems that use
 *   event-driven drawing based on window invalidations; the Windows and
 *   Macintosh GUI's both use this method for drawing window contents.
 *   
 *   The purpose of this routine is to refresh the display prior to a
 *   potentially long-running computation, to avoid the appearance that the
 *   application is frozen during the computation delay.
 *   
 *   Platforms that don't need to process events in the main thread in order
 *   to draw their window contents do not need to do anything here.  In
 *   particular, text-mode implementations generally don't need to implement
 *   this routine.
 *   
 *   This routine doesn't absolutely need a non-empty implementation on any
 *   platform, but it will provide better visual feedback if implemented for
 *   those platforms that do use event-driven drawing.  
 */
void os_update_display()
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_update_display");
#endif
  /* do nothing */
}

/*
 *   Set text attributes.  Text subsequently displayed through os_print() and
 *   os_printz() are to be displayed with the given attributes.
 *   
 *   'attr' is a (bitwise-OR'd) combination of OS_ATTR_xxx values.  A value
 *   of zero indicates normal text, with no extra attributes.  
 */
void os_set_text_attr(int attr)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_set_text_attr");
#endif
    /* modified version of code from oshtml.cpp and tads2/glk/os_glk.c */

    /* 
     *   ignore attribute settings in HTML mode - calls must use appropriate
     *   HTML tags instead 
     */
    if (html_mode_)
        return;

    /* try to map attributes to Glk styles */
    if ((attr & OS_ATTR_BOLD) && (attr & OS_ATTR_ITALIC)) {
        glk_set_style(style_Alert);
    } else if (attr & OS_ATTR_BOLD) {
        glk_set_style(style_Subheader);
    } else if (attr & OS_ATTR_ITALIC) {
        glk_set_style(style_Emphasized);
    } else {
        glk_set_style(style_Normal);
    }
}

/*
 *   Set the text foreground and background colors.  This sets the text
 *   color for subsequent os_printf() and os_vprintf() calls.
 *   
 *   The background color can be OS_COLOR_TRANSPARENT, in which case the
 *   background color is "inherited" from the current screen background.
 *   Note that if the platform is capable of keeping old text for
 *   "scrollback," then the transparency should be a permanent attribute of
 *   the character - in other words, it should not be mapped to the current
 *   screen color in the scrollback buffer, because doing so would keep the
 *   current screen color even if the screen color changes in the future. 
 *   
 *   Text color support is optional.  If the platform doesn't support text
 *   colors, this can simply do nothing.  If the platform supports text
 *   colors, but the requested color or attributes cannot be displayed, the
 *   implementation should use the best available approximation.  
 */
void os_set_text_color(os_color_t fg, os_color_t bg)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_set_text_color");
#endif
    /* like oshtml.cpp, we ignore this - callers must use HTML tags to set colors */
}

/*
 *   Set the screen background color.  This sets the text color for the
 *   background of the screen.  If possible, this should immediately redraw
 *   the main text area with this background color.  The color is given as an
 *   OS_COLOR_xxx value.
 *   
 *   If the platform is capable of redisplaying the existing text, then any
 *   existing text that was originally displayed with 'transparent'
 *   background color should be redisplayed with the new screen background
 *   color.  In other words, the 'transparent' background color of previously
 *   drawn text should be a permanent attribute of the character - the color
 *   should not be mapped on display to the then-current background color,
 *   because doing so would lose the transparency and thus retain the old
 *   screen color on a screen color change.  
 */
void os_set_screen_color(os_color_t color)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_set_screen_color");
#endif
    /* like oshtml.cpp, we ignore this - callers must use HTML tags to set colors */
}

/* 
 *   os_plain() - Use plain ascii mode for the display.  If possible and
 *   necessary, turn off any text formatting effects, such as cursor
 *   positioning, highlighting, or coloring.  If this routine is called,
 *   the terminal should be treated as a simple text stream; users might
 *   wish to use this mode for things like text-to-speech converters.
 *   
 *   Purely graphical implementations that cannot offer a textual mode
 *   (such as Mac OS or Windows) can ignore this setting.
 *   
 *   If this routine is to be called, it must be called BEFORE os_init().
 *   The implementation should set a flag so that os_init() will know to
 *   set up the terminal for plain text output.  
 */
void os_plain(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_plain");
#endif
    /* like oshtml.cpp, we ignore this -- we can only use the HTML mode */
}

/*
 *   Set the game title.  The output layer calls this routine when a game
 *   sets its title (via an HTML <title> tag, for example).  If it's
 *   convenient to do so, the OS layer can use this string to set a window
 *   caption, or whatever else makes sense on each system.  Most
 *   character-mode implementations will provide an empty implementation,
 *   since there's not usually any standard way to show the current
 *   application title on a character-mode display.  
 */
void os_set_title(const char *title)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_set_title");
#endif
    /* like oshtml.cpp, we can ignore title settings from the high-level output formatter
     * layer, since we parse the title tag ourselves in the underlying HTML layer. */
}

/*
 *   Show the system-specific MORE prompt, and wait for the user to respond.
 *   Before returning, remove the MORE prompt from the screen.
 *   
 *   This routine is only used and only needs to be implemented when the OS
 *   layer takes responsibility for pagination; this will be the case on
 *   most systems that use proportionally-spaced (variable-pitch) fonts or
 *   variable-sized windows, since on such platforms the OS layer must do
 *   most of the formatting work, leaving the standard output layer unable
 *   to guess where pagination should occur.
 *   
 *   If the portable output formatter handles the MORE prompt, which is the
 *   usual case for character-mode or terminal-style implementations, this
 *   routine is not used and you don't need to provide an implementation.
 *   Note that HTML TADS provides an implementation of this routine, because
 *   the HTML renderer handles line breaking and thus must handle
 *   pagination.  
 */
void os_more_prompt()
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_more_prompt");
#endif
    /* Modified version of tads2/glk/os_glk.c */
    int done;

    /* display the "MORE" prompt */
    /* (Actually don't do this because we'd need to write
     * another GLK extension function to track it and remove it
     * after the user hits a key. Maybe one day, but not today) */
    /* os_printz("[More]"); */
    
    /* wait for a keystroke */
    for (done = 0; !done; )
    {
        os_event_info_t evt;

        /* get an event */
        switch(os_get_event(0, 0, &evt))
        {
        case OS_EVT_KEY:
            /* stop waiting, show one page */
            done = 1;
            break;

        case OS_EVT_EOF:
            /* end of file - there's nothing to wait for now */
            done = 1;
            break;

        default:
            /* ignore other events */
            break;
        }
    }

   /* if (html_mode_ == 1) {
      os_printz("<BR HEIGHT=0>");
    } else {
      os_printz("\n");
    } */
}

/*
 *   Enter HTML mode.  This is only used when the run-time is compiled
 *   with the USE_HTML flag defined.  This call instructs the renderer
 *   that HTML sequences should be parsed; until this call is made, the
 *   renderer should not interpret output as HTML.  Non-HTML
 *   implementations do not need to define this routine, since the
 *   run-time will not call it if USE_HTML is not defined.  
 */
void os_start_html(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_start_html");
#endif
    fab_set_html_mode(1);

    /* note that we're in HTML mode */
    html_mode_ = 1;
}

/* exit HTML mode */
void os_end_html(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_end_html");
#endif
    fab_set_html_mode(0);

    /* note that we're not in HTML mode */
    html_mode_ = 0;
}

/*
 *   Set non-stop mode.  This tells the OS layer that it should disable any
 *   MORE prompting it would normally do.
 *   
 *   This routine is needed only when the OS layer handles MORE prompting; on
 *   character-mode platforms, where the prompting is handled in the portable
 *   console layer, this can be a dummy implementation.  
 */
void os_nonstop_mode(int flag)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_nonstop_mode");
#endif
  /* TODO */
}

/* 
 *   Set busy cursor.  If 'flag' is true, provide a visual representation
 *   that the system or application is busy doing work.  If 'flag' is
 *   false, remove any visual "busy" indication and show normal status.
 *   
 *   We provide a prototype here if your osxxx.h header file does not
 *   #define a macro for os_csr_busy.  On many systems, this function has
 *   no effect at all, so the osxxx.h header file simply #define's it to
 *   do an empty macro.  
 */
void os_csr_busy(int flag)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_csr_busy");
#endif
  /* We don't yet support this, and probably never will */
}

/* ------------------------------------------------------------------------ */
/*
 *   User Input Routines
 */

/*
 *   Ask the user for a filename, using a system-dependent dialog or other
 *   mechanism.  Returns one of the OS_AFE_xxx status codes (see below).
 *   
 *   prompt_type is the type of prompt to provide -- this is one of the
 *   OS_AFP_xxx codes (see below).  The OS implementation doesn't need to
 *   pay any attention to this parameter, but it can be used if desired to
 *   determine the type of dialog to present if the system provides
 *   different types of dialogs for different types of operations.
 *   
 *   file_type is one of the OSFTxxx codes for system file type.  The OS
 *   implementation is free to ignore this information, but can use it to
 *   filter the list of files displayed if desired; this can also be used
 *   to apply a default suffix on systems that use suffixes to indicate
 *   file type.  If OSFTUNK is specified, it means that no filtering
 *   should be performed, and no default suffix should be applied.  
 *
 *   os_askfile status codes:
 *
 *     OS_AFE_SUCCESS 
 *       
 *       Success.
 *
 *     OS_AFE_FAILURE
 *
 *       Generic failure - this is largely provided for compatibility with
 *       past versions, in which only zero and non-zero error codes were
 *       meaningful; since TRUE is defined as 1 on most platforms, we assume
 *       that 1 is probably the generic non-zero error code that most OS
 *       implementations have traditionally used.  In addition, this can be
 *       used to indicate any other error for which there is no more specific
 *       error code. 
 *
 *    OS_AFE_CANCEL
 *
 *       User cancelled. 
 *
 *  os_askfile prompt types:
 *
 *    OS_AFP_OPEN
 *
 *       Choose an existing file to open for reading.
 *
 *    OS_AFP_SAVE
 *
 *       Choose a filename for saving to a file.
 *
 */
int os_askfile(const char *prompt, char *fname_buf, int fname_buf_len,
               int prompt_type, os_filetype_t file_type)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_askfile");
#endif
    /* Modified version of Gargoyle (and tads2/glk/os_glk.c) source */
    frefid_t fileref;
    glui32 gprompt, gusage;

    if (prompt_type == OS_AFP_OPEN) {
        gprompt = filemode_Read;
    } else {
        gprompt = filemode_ReadWrite;
    }

    /* TO DO: For now we assume that OSFTUNK is a save to ensure that we don't have issues with
     * the odd game such as Babel which uses this extension for saves.  Of course
     * that means it may break any games that use this for data.  Oh well, one more thing
     * to fix if we can ever find the time... */
    if (file_type == OSFTSAVE || file_type == OSFTT3SAV || file_type == OSFTUNK) {
        gusage = fileusage_SavedGame;
    } else if (file_type == OSFTLOG || file_type == OSFTTEXT) {
        gusage = fileusage_Transcript;
    } else {
        gusage = fileusage_Data;
    }

    fileref = glk_fileref_create_by_prompt(gusage, gprompt, 0);
    if (fileref == NULL) {
        return OS_AFE_CANCEL;
    }

    strcpy(fname_buf, garglk_fileref_get_name(fileref));

    glk_fileref_destroy(fileref);

    return OS_AFE_SUCCESS;
}

/* 
 *   Read a string of input.  Fills in the buffer with a null-terminated
 *   string containing a line of text read from the standard input.  The
 *   returned string should NOT contain a trailing newline sequence.  On
 *   success, returns 'buf'; on failure, including end of file, returns a
 *   null pointer.  
 */
unsigned char *os_gets(unsigned char *buf, size_t buflen)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_gets");
#endif 
    int flags = 0;
    int res = 0;

    flags = tads_get_input(buf, buflen, 0, mainwin, 0);
    res = ((flags & 0x10) == 0x10);

    return res > 0 ? buf : NULL;
}

/*
 *   Read a string of input with an optional timeout.  This behaves like
 *   os_gets(), in that it allows the user to edit a line of text (ideally
 *   using the same editing keys that os_gets() does), showing the line of
 *   text under construction during editing.  This routine differs from
 *   os_gets() in that it returns if the given timeout interval expires
 *   before the user presses Return (or the local equivalent).
 *   
 *   If the user presses Return before the timeout expires, we store the
 *   command line in the given buffer, just as os_gets() would, and we
 *   return OS_EVT_LINE.  We also update the display in the same manner that
 *   os_gets() would, by moving the cursor to a new line and scrolling the
 *   displayed text as needed.
 *   
 *   If a timeout occurs before the user presses Return, we store the
 *   command line so far in the given buffer, statically store the cursor
 *   position, insert mode, buffer text, and anything else relevant to the
 *   editing state, and we return OS_EVT_TIMEOUT.
 *   
 *   If the implementation does not support the timeout operation, this
 *   routine should simply return OS_EVT_NOTIMEOUT immediately when called;
 *   the routine should not allow the user to perform any editing if the
 *   timeout is not supported.  Callers must use the ordinary os_gets()
 *   routine, which has no timeout capabilities, if the timeout is not
 *   supported.
 *   
 *   When we return OS_EVT_TIMEOUT, the caller is responsible for doing one
 *   of two things.
 *   
 *   The first possibility is that the caller performs some work that
 *   doesn't require any display operations (in other words, the caller
 *   doesn't invoke os_printf, os_getc, or anything else that would update
 *   the display), and then calls os_gets_timeout() again.  In this case, we
 *   will use the editing state that we statically stored before we returned
 *   OS_EVT_TIMEOUT to continue editing where we left off.  This allows the
 *   caller to perform some computation in the middle of user command
 *   editing without interrupting the user - the extra computation is
 *   transparent to the user, because we act as though we were still in the
 *   midst of the original editing.
 *   
 *   The second possibility is that the caller wants to update the display.
 *   In this case, the caller must call os_gets_cancel() BEFORE making any
 *   display changes.  Then, the caller must do any post-input work of its
 *   own, such as updating the display mode (for example, closing HTML font
 *   tags that were opened at the start of the input).  The caller is now
 *   free to do any display work it wants.
 *   
 *   If we have information stored from a previous call that was interrupted
 *   by a timeout, and os_gets_cancel(true) was never called, we will resume
 *   editing where we left off when the cancelled call returned; this means
 *   that we'll restore the cursor position, insertion state, and anything
 *   else relevant.  Note that if os_gets_cancel(false) was called, we must
 *   re-display the command line under construction, but if os_gets_cancel()
 *   was never called, we will not have to make any changes to the display
 *   at all.
 *   
 *   Note that when resuming an interrupted editing session (interrupted via
 *   os_gets_cancel()), the caller must re-display the prompt prior to
 *   invoking this routine.
 *   
 *   Note that we can return OS_EVT_EOF in addition to the other codes
 *   mentioned above.  OS_EVT_EOF indicates that an error occurred reading,
 *   which usually indicates that the application is being terminated or
 *   that some hardware error occurred reading the keyboard.  
 *   
 *   If 'use_timeout' is false, the timeout should be ignored.  Without a
 *   timeout, the function behaves the same as os_gets(), except that it
 *   will resume editing of a previously-interrupted command line if
 *   appropriate.  (This difference is why the timeout is optional: a caller
 *   might not need a timeout, but might still want to resume a previous
 *   input that did time out, in which case the caller would invoke this
 *   routine with use_timeout==false.  The regular os_gets() would not
 *   satisfy this need, because it cannot resume an interrupted input.)  
 */
static char * timebuf = NULL;
static size_t timelen = 0;

int os_gets_timeout(unsigned char *buf, size_t bufl,
                    unsigned long timeout_in_milliseconds, int use_timeout)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_gets_timeout");
#endif
    /* Modified version of Gargoyle source */
    int timer = use_timeout ? timeout_in_milliseconds : 0;
    int timeout = 0;
    int initlen = 0;
    int res = 0;
    int flags = 0;

    /* restore saved buffer contents */
    if (timebuf)
    {
        assert(timelen && timelen <= bufl);
        memcpy(buf, timebuf, timelen);
        initlen = timelen - 1;
        buf[initlen] = 0;
        free(timebuf);
        timebuf = 0;
    }

    flags = tads_get_input(buf, bufl, initlen, mainwin, timer);
    timeout = ((flags & 0x01) == 0x01);
    res = ((flags & 0x10) == 0x10);

    /* save or print buffer contents */
    if (res && timer)
    {
        if (timeout)
        {
            timelen = strlen(buf) + 1;
            timebuf = malloc(timelen);
            memcpy(timebuf, buf, timelen);
        }
        else
        {
            glk_set_style(style_Input);
            os_print(buf, strlen(buf));
            os_print("\n", 1);
            glk_set_style(style_Normal);
        }
    }

    return timeout ? OS_EVT_TIMEOUT : res ? OS_EVT_LINE : OS_EVT_EOF;
}

/*
 *   Cancel an interrupted editing session.  This MUST be called if any
 *   output is to be displayed after a call to os_gets_timeout() returns
 *   OS_EVT_TIMEOUT.
 *   
 *   'reset' indicates whether or not we will forget the input state saved
 *   by os_gets_timeout() when it last returned.  If 'reset' is true, we'll
 *   clear the input state, so that the next call to os_gets_timeout() will
 *   start with an empty input buffer.  If 'reset' is false, we will retain
 *   the previous input state, if any; this means that the next call to
 *   os_gets_timeout() will re-display the same input buffer that was under
 *   construction when it last returned.
 *   
 *   This routine need not be called if os_gets_timeout() is to be called
 *   again with no other output operations between the previous
 *   os_gets_timeout() call and the next one.
 *   
 *   Note that this routine needs only a trivial implementation when
 *   os_gets_timeout() is not supported (i.e., the function always returns
 *   OS_EVT_NOTIMEOUT).  
 */
void os_gets_cancel(int reset)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_gets_cancel");
#endif
    /* From Gargoyle source */

    if (timebuf)
    {
        glk_set_style(style_Input);
        os_print(timebuf, strlen(timebuf));
        os_print("\n", 1);
        glk_set_style(style_Normal);

        if (reset)
        {
            free(timebuf);
            timebuf = 0;
        }
    }
}

/* Change a Glk key into a TADS one, using the CMD_xxx codes from
   osifc.h */
static int glktotads(unsigned int key)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "glktotads");
#endif
    /* Based on tads2/glk/oss_glk.c */

    /* Characters 0 - 255 we return per normal */
    if (key < 256) {
        return key;
    }

    switch (key) {
        case keycode_Up:
            return CMD_UP;
        case keycode_Down:
            return CMD_DOWN;
        case keycode_Left:
            return CMD_LEFT;
        case keycode_Right:
            return CMD_RIGHT;
        case keycode_PageUp:
            return CMD_PGUP;
        case keycode_PageDown:
            return CMD_PGDN;
        case keycode_Home:
            return CMD_HOME;
        case keycode_End:
            return CMD_END;
        case keycode_Func1:
            return CMD_F1;
        case keycode_Func2:
            return CMD_F2;
        case keycode_Func3:
            return CMD_F3;
        case keycode_Func4:
            return CMD_F4;
        case keycode_Func5:
            return CMD_F5;
        case keycode_Func6:
            return CMD_F6;
        case keycode_Func7:
            return CMD_F7;
        case keycode_Func8:
            return CMD_F8;
        case keycode_Func9:
            return CMD_F9;
        case keycode_Func10:
            return CMD_F10;
        default:
            return 0;
    }
}

static int bufchar = 0;
static int timechar = 0;

static int getglkchar(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "getglkchar");
#endif
    /* Based on tads2/glk/oss_glk.c, with reference to Gargoyle source */
    event_t event;

    timechar = 0;
    glk_request_char_event(mainwin);

    do {
        glk_select(&event);
        if (event.type == evtype_Timer) {
            timechar = 1;
        }
    } while (event.type != evtype_CharInput && event.type != evtype_Timer);

    glk_cancel_char_event(mainwin);

    return timechar ? 0 : event.val1;
}

/* 
 *   Read a character from the keyboard.  For extended keystrokes, this
 *   function returns zero, and then returns the CMD_xxx code for the
 *   extended keystroke on the next call.  For example, if the user
 *   presses the up-arrow key, the first call to os_getc() should return
 *   0, and the next call should return CMD_UP.  Refer to the CMD_xxx
 *   codes below.
 *   
 *   os_getc() should return a high-level, translated command code for
 *   command editing.  This means that, where a functional interpretation
 *   of a key and the raw key-cap interpretation both exist as CMD_xxx
 *   codes, the functional interpretation should be returned.  For
 *   example, on Unix, Ctrl-E is conventionally used in command editing to
 *   move to the end of the line, following Emacs key bindings.  Hence,
 *   os_getc() should return CMD_END for this keystroke, rather than
 *   (CMD_CTRL + 'E' - 'A'), because CMD_END is the high-level command
 *   code for the operation.
 *   
 *   The translation ability of this function allows for system-dependent
 *   key mappings to functional meanings.  
 */
int os_getc(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_getc");
#endif
    /* Based on tads2/glk/oss_glk.c, with reference to Gargoyle source */
    unsigned int c;

    if (bufchar) {
        c = bufchar;
        bufchar = 0;
        return c;
    }

    c = getglkchar();
    
    if (c == keycode_Return) {
        c = '\n';
    } else if (c == keycode_Tab) {
        c = '\t';
    } else if (c == keycode_Escape) {
        c = '\e';
    }

    if (c < 256) {
        return c;
    }

    bufchar = glktotads(c);
    return 0;
}

/*
 *   Read a character from the keyboard, following the same protocol as
 *   os_getc() for CMD_xxx codes (i.e., when an extended keystroke is
 *   encountered, os_getc_raw() returns zero, then returns the CMD_xxx code
 *   on the subsequent call).
 *   
 *   This function differs from os_getc() in that this function returns the
 *   low-level, untranslated key code whenever possible.  This means that,
 *   when a functional interpretation of a key and the raw key-cap
 *   interpretation both exist as CMD_xxx codes, this function returns the
 *   key-cap interpretation.  For the Unix Ctrl-E example in the comments
 *   describing os_getc() above, this function should return 5 (the ASCII
 *   code for Ctrl-E), because the CMD_CTRL interpretation is the low-level
 *   key code.
 *   
 *   This function should return all control keys using their ASCII control
 *   codes, whenever possible.  Similarly, this function should return ASCII
 *   27 for the Escape key, if possible.  
 *   
 *   For keys for which there is no portable ASCII representation, this
 *   should return the CMD_xxx sequence.  So, this function acts exactly the
 *   same as os_getc() for arrow keys, function keys, and other special keys
 *   that have no ASCII representation.  This function returns a
 *   non-translated version ONLY when an ASCII representation exists - in
 *   practice, this means that this function and os_getc() vary only for
 *   CTRL keys and Escape.
 */
int os_getc_raw(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_getc_raw");
#endif
    /* Same as tads2/glk/os_glk.c */
    return os_getc();
}

/* wait for a character to become available from the keyboard */
void os_waitc(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_waitc");
#endif
    /* Same as tads2/glk/os_glk.c */
    os_getc();
}

/*
 *   Get an input event.  The event types are shown above.  If use_timeout
 *   is false, this routine should simply wait until one of the events it
 *   recognizes occurs, then return the appropriate information on the
 *   event.  If use_timeout is true, this routine should return
 *   OS_EVT_TIMEOUT after the given number of milliseconds elapses if no
 *   event occurs first.
 *   
 *   This function is not obligated to obey the timeout.  If a timeout is
 *   specified and it is not possible to obey the timeout, the function
 *   should simply return OS_EVT_NOTIMEOUT.  The trivial implementation
 *   thus checks for a timeout, returns an error if specified, and
 *   otherwise simply waits for the user to press a key.  
 */
int os_get_event(unsigned long timeout_in_milliseconds, int use_timeout,
                 os_event_info_t *info)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_get_event");
#endif
    /* Based on Gargoyle source, with reference to tads2/glk/os_glk.c */

    /* start timer */
    int timer = use_timeout ? timeout_in_milliseconds : 0;
    if (timer) {
        glk_request_timer_events(timer);
    }

    /* get a key */
    info->key[0] = os_getc_raw();
    if (info->key[0] == 0 && timechar == 0) {
        info->key[1] = os_getc_raw();
    }

    /* stop timer */
    if (timer) {
        glk_request_timer_events(0);
    }

    /* return the event */
    return timechar ? OS_EVT_TIMEOUT : OS_EVT_KEY;
}


/* ------------------------------------------------------------------------ */
/*
 *   OS main entrypoint
 */

/* 
 *   Initialize.  This should be called during program startup to
 *   initialize the OS layer and check OS-specific command-line arguments.
 *   
 *   If 'prompt' and 'buf' are non-null, and there are no arguments on the
 *   given command line, the OS code can use the prompt to ask the user to
 *   supply a filename, then store the filename in 'buf' and set up
 *   argc/argv to give a one-argument command string.  (This mechanism for
 *   prompting for a filename is obsolescent, and is retained for
 *   compatibility with a small number of existing implementations only;
 *   new implementations should ignore this mechanism and leave the
 *   argc/argv values unchanged.)  
 */
int os_init(int *argc, char *argv[], const char *prompt,
            char *buf, int bufsiz) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_init");
#endif
    mainwin = glk_window_open(0, 0, 0, wintype_TextBuffer, 0);
    if (!mainwin) {
        __android_log_write(ANDROID_LOG_ERROR, "osglk.c - os_init", "fatal: could not open main window.");
        glk_exit();
    }
    glk_set_window(mainwin);
    return 0;
}

/*
 *   Uninitialize.  This is called prior to progam termination to reverse
 *   the effect of any changes made in os_init().  For example, if
 *   os_init() put the terminal in raw mode, this should restore the
 *   previous terminal mode.  This routine should not terminate the
 *   program (so don't call exit() here) - the caller might have more
 *   processing to perform after this routine returns.
 */
void os_uninit(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_uninit");
#endif
}

/* 
 *   Pause prior to exit, if desired.  This is meant to be called by
 *   portable code just before the program is to be terminated; it can be
 *   implemented to show a prompt and wait for user acknowledgment before
 *   proceeding.  This is useful for implementations that are using
 *   something like a character-mode terminal window running on a graphical
 *   operating system: this gives the implementation a chance to pause
 *   before exiting, so that the window doesn't just disappear
 *   unceremoniously.
 *   
 *   This is allowed to do nothing at all.  For regular character-mode
 *   systems, this routine usually doesn't do anything, because when the
 *   program exits, the terminal will simply return to the OS command
 *   prompt; none of the text displayed just before the program exited will
 *   be lost, so there's no need for any interactive pause.  Likewise, for
 *   graphical systems where the window will remain open, even after the
 *   program exits, until the user explicitly closes the window, there's no
 *   need to do anything here.
 *   
 *   If this is implemented to pause, then this routine MUST show some kind
 *   of prompt to let the user know we're waiting.  In the simple case of a
 *   text-mode terminal window on a graphical OS, this should simply print
 *   out some prompt text ("Press a key to exit...") and then wait for the
 *   user to acknowledge the prompt (by pressing a key, for example).  For
 *   graphical systems, the prompt could be placed in the window's title
 *   bar, or status-bar, or wherever is appropriate for the OS.  
 */
void os_expause(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_expause");
#endif
  /* Don't do anything */
}

/* 
 *   Terminate.  This should exit the program with the given exit status.
 *   In general, this should be equivalent to the standard C library
 *   exit() function, but we define this interface to allow the OS code to
 *   do any necessary pre-termination cleanup.  
 */
void os_term(int status) {
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_term");
#endif
    glk_exit();
}

/*
 *   Install/uninstall the break handler.  If possible, the OS code should
 *   set (if 'install' is true) or clear (if 'install' is false) a signal
 *   handler for keyboard break signals (control-C, etc, depending on
 *   local convention).  The OS code should set its own handler routine,
 *   which should note that a break occurred with an internal flag; the
 *   portable code uses os_break() from time to time to poll this flag.
 */
void os_instbrk(int install)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_instbrk");
#endif
}

/*
 *   Check for user break ("control-C", etc) - returns true if a break is
 *   pending, false if not.  If this returns true, it should "consume" the
 *   pending break (probably by simply clearing the OS code's internal
 *   break-pending flag).  
 */
int os_break(void)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_break");
#endif
    return 0;
}

/*
 *   Yield CPU; returns TRUE if user requested an interrupt (a "control-C"
 *   type of operation to abort the entire program), FALSE to continue.
 *   Portable code should call this routine from time to time during lengthy
 *   computations that don't involve any UI operations; if practical, this
 *   routine should be invoked roughly every 10 to 100 milliseconds.
 *
 *   The purpose of this routine is to support "cooperative multitasking"
 *   systems, such as pre-X MacOS, where it's necessary for each running
 *   program to call the operating system explicitly in order to yield the
 *   CPU from time to time to other concurrently running programs.  On
 *   cooperative multitasking systems, a program can only lose control of
 *   the CPU by making specific system calls, usually related to GUI events;
 *   a program can never lose control of the CPU asynchronously.  So, a
 *   program that performs lengthy computations without any UI interaction
 *   can cause the rest of the system to freeze up until the computations
 *   are finished; but if a compute-intensive program explicitly yields the
 *   CPU from time to time, it allows other programs to remain responsive.
 *   Yielding the CPU at least every 100 milliseconds or so will generally
 *   allow the UI to remain responsive; yielding more frequently than every
 *   10 ms or so will probably start adding noticeable overhead.
 *
 *   On single-tasking systems (such as MS-DOS), there's only one program
 *   running at a time, so there's no need to yield the CPU; on virtually
 *   every modern system, the OS automatically schedules CPU time without
 *   the running programs having any say in the matter, so again there's no
 *   need for a program to yield the CPU.  For systems where this routine
 *   isn't needed, the system header should simply #define os_yield to
 *   something like "((void)0)" - this will allow the compiler to completely
 *   ignore calls to this routine for systems where they aren't needed.
 *
 *   Note that this routine is NOT meant to provide scheduling hinting to
 *   modern systems with true multitasking, so a trivial implementation is
 *   fine for any modern system.
 */
int os_yield(void)
{
   /* Fabularium will only ever run on a modern OS (Android), so no need
    * to do anything here (and don't bother with the overheads of calling
    * glk_tick) */
    return 0;
}

/*
 *   Set the default saved-game extension.  This routine will NOT be
 *   called when we're using the standard saved game extension; this
 *   routine will be invoked only if we're running as a stand-alone game,
 *   and the game author specified a non-standard saved-game extension
 *   when creating the stand-alone game.
 *   
 *   This routine is not required if the system does not use the standard,
 *   semi-portable os0.c implementation.  Even if the system uses the
 *   standard os0.c implementation, it can provide an empty routine here
 *   if the system code doesn't need to do anything special with this
 *   information.
 *   
 *   The extension is specified as a null-terminated string.  The
 *   extension does NOT include the leading period.  
 */
void os_set_save_ext(const char *ext)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_set_save_ext");
#endif
  /* We don't do anything */
}


/* ------------------------------------------------------------------------ */
/*
 *   External banner interface 
 */
void *os_banner_create(void *parent, int where, void *other, int wintype,
                       int align, int siz, int siz_units,
                       unsigned long style) {
  return tadsban_create(parent, where, other, wintype, align, siz, siz_units, style);
}

int os_banner_get_charwidth(void *banner_handle) {
  return tadsban_get_charwidth(banner_handle);
}

int os_banner_get_charheight(void *banner_handle) {
  return tadsban_get_charheight(banner_handle);
}

int os_banner_getinfo(void *banner_handle, os_banner_info_t *info) {
  return tadsban_getinfo(banner_handle, info);
}

void os_banner_delete(void *banner_handle) {
  tadsban_delete(banner_handle);
}

void os_banner_orphan(void *banner_handle) {
  tadsban_orphan(banner_handle);
}

void os_banner_clear(void *banner_handle) {
  tadsban_clear(banner_handle);
}

void os_banner_disp(void *banner_handle, const char *txt, size_t len) {
  tadsban_disp(banner_handle, txt, len);
}

void os_banner_set_attr(void *banner_handle, int attr) {
  tadsban_set_attr(banner_handle, attr);
}

void os_banner_set_color(void *banner_handle, os_color_t fg, os_color_t bg) {
  tadsban_set_color(banner_handle, fg, bg);
}

void os_banner_set_screen_color(void *banner_handle, os_color_t color) {
  tadsban_set_screen_color(banner_handle, color);
}

void os_banner_flush(void *banner_handle) {
  tadsban_flush(banner_handle);
}

void os_banner_set_size(void *banner_handle, int siz, int siz_units, int is_advisory) {
  tadsban_set_size(banner_handle, siz, siz_units, is_advisory);
}

void os_banner_size_to_contents(void *banner_handle) {
  tadsban_size_to_contents(banner_handle);
}

void os_banner_start_html(void *banner_handle) {
  tadsban_start_html(banner_handle);
}

void os_banner_end_html(void *banner_handle) {
  tadsban_end_html(banner_handle);
}

void os_banner_goto(void *banner_handle, int row, int col) {
  tadsban_goto(banner_handle, row, col);
}

/* ------------------------------------------------------------------------ */
/*
 *   Get system information.  'code' is a SYSINFO_xxx code, which
 *   specifies what type of information to get.  The 'param' argument's
 *   meaning depends on which code is selected.  'result' is a pointer to
 *   an integer that is to be filled in with the result value.  If the
 *   code is not known, this function should return false.  If the code is
 *   known, the function should fill in *result and return true.
 */
int os_get_sysinfo(int code, void *param, long *result)
{
#ifdef DEBUG_TADS
    __android_log_write(ANDROID_LOG_DEBUG, "osglk.c", "os_get_sysinfo");
#endif
    *result = 0;
    
    switch(code)
    {
      case SYSINFO_TEXT_HILITE:
      case SYSINFO_BANNERS:
        *result = 1;
        break;

      case SYSINFO_AUDIO_FADE:
      case SYSINFO_AUDIO_CROSSFADE:
        /* We support fades and crossfades for everything except MIDI. */
        *result = SYSINFO_AUDIOFADE_MPEG | SYSINFO_AUDIOFADE_OGG | SYSINFO_AUDIOFADE_WAV;
        break;

      case SYSINFO_TEXT_COLORS:
        *result = SYSINFO_TXC_RGB;
        break;

      case SYSINFO_HTML:
        *result = (int)glk_gestalt(gestalt_HTML, 0);
        break;
     
      case SYSINFO_INTERP_CLASS:
        *result = glk_gestalt(gestalt_HTML, 0) ? SYSINFO_ICLASS_HTML : SYSINFO_ICLASS_TEXTGUI;
        break;

      case SYSINFO_MNG:
      case SYSINFO_MNG_TRANS:
      case SYSINFO_MNG_ALPHA:
      case SYSINFO_PNG_TRANS:
      case SYSINFO_PNG_ALPHA:
      case SYSINFO_JPEG:
      case SYSINFO_PNG:
      case SYSINFO_PREF_IMAGES:
        *result = (int)glk_gestalt(gestalt_Graphics, 0);
        break;

      case SYSINFO_OGG:
      case SYSINFO_WAV:
      case SYSINFO_MIDI:
      case SYSINFO_WAV_MIDI_OVL:
      case SYSINFO_WAV_OVL:
      case SYSINFO_PREF_SOUNDS:
        *result = (int)glk_gestalt(gestalt_Sound2, 0);
        break;

      case SYSINFO_MPEG:
      case SYSINFO_MPEG1:
      case SYSINFO_MPEG2:
      case SYSINFO_MPEG3:
      case SYSINFO_PREF_MUSIC:
        *result = (int)glk_gestalt(gestalt_SoundMusic, 0);
        break;

      case SYSINFO_LINKS_HTTP:
      case SYSINFO_LINKS_FTP:
      case SYSINFO_LINKS_NEWS:
      case SYSINFO_LINKS_MAILTO:
      case SYSINFO_LINKS_TELNET:
      case SYSINFO_PREF_LINKS:
        *result = (int)glk_gestalt(gestalt_Hyperlinks, 0);
        break;

      default:
        // We didn't recognize the code
        return false;
    }
    
    // We recognized the code.
    return true;
}

