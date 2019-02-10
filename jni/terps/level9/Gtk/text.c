/*
 * text.c - Text buffer
 * Copyright (c) 2005 Torbjörn Andersson <d91tan@Update.UU.SE>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111, USA.
 */

#include <stdarg.h>
#include <stdlib.h>
#include <string.h>
#include <gtk/gtk.h>
#include <gdk/gdkkeysyms.h>

#include "level9.h"

#include "main.h"
#include "config.h"
#include "graphics.h"
#include "gui.h"
#include "main.h"
#include "text.h"
#include "util.h"

typedef struct {
    char *buf;
    int size;
} Buffer;

enum
{
    INPUT_NOT_BLOCKED,
    INPUT_BLOCKED,
    INPUT_BLOCKED_IN_MAIN_LOOP
};

static gboolean inputBlock = INPUT_NOT_BLOCKED;

static gboolean waitingForInput = FALSE;
static gboolean abortInput = FALSE;

static Buffer inputBuffer;

static GtkTextMark *inputMark = NULL;

static gulong hSigInsert = 0;
static gulong hSigDelete = 0;
static gulong hSigKeypress = 0;

static GString *bufferedText = NULL;

static void set_input_pending (gboolean pending);

static void sig_insert (GtkTextBuffer *buffer, GtkTextIter *arg1,
			gchar *arg2, gint arg3, gpointer user_data);
static void sig_delete (GtkTextBuffer *buffer, GtkTextIter *arg1,
			GtkTextIter *arg2, gpointer user_data);
static gboolean sig_keypress (GtkWidget *widget, GdkEventKey *event,
			      gpointer user_data);

/* ------------------------------------------------------------------------- *
 * Utility functions.                                                        *
 * ------------------------------------------------------------------------- */

void block_input ()
{
    if (inputBlock == INPUT_NOT_BLOCKED)
    {
	if (stop_main_loop ())
	    inputBlock = INPUT_BLOCKED_IN_MAIN_LOOP;
	else
	    inputBlock = INPUT_BLOCKED;
    } else
	g_warning ("block_input: Input is already blocked");
}

void unblock_input ()
{
    switch (inputBlock)
    {
	case INPUT_NOT_BLOCKED:
	    g_warning ("unblock_input: Input is not blocked");
	    break;

	case INPUT_BLOCKED_IN_MAIN_LOOP:
	    start_main_loop ();
	    /* Fallthrough */

	case INPUT_BLOCKED:
	    inputBlock = INPUT_NOT_BLOCKED;
	    break;
    }
}

void text_reinit ()
{
    GtkTextIter start, end;

    if (bufferedText)
	g_string_truncate (bufferedText, 0);

    if (waitingForInput)
    {
	abortInput = TRUE;
	gtk_main_quit ();
    }

    gtk_text_buffer_get_bounds (Gui.text_buffer, &start, &end);

    set_input_pending (FALSE);
    gtk_text_buffer_delete (Gui.text_buffer, &start, &end);
    set_input_pending (TRUE);
}

void text_refresh (void)
{
    GdkColor colour;

#ifdef USE_CURSOR_COLOUR_HACK
    GdkColor *cursor_colour;
    char buffer[256];
#endif

    if (Config.text_font)
    {
	PangoFontDescription *font_desc;
	
	font_desc = pango_font_description_from_string (Config.text_font);
	gtk_widget_modify_font (GTK_WIDGET (Gui.text_view), font_desc);
	pango_font_description_free (font_desc);
    }

#ifdef USE_CURSOR_COLOUR_HACK
    if (Config.text_fg && gdk_color_parse (Config.text_fg, &colour))
    {
	gtk_widget_modify_text (Gui.text_view, GTK_STATE_NORMAL, &colour);
	cursor_colour = &colour;
    } else {
	GtkRcStyle *style;

	gtk_widget_set_style (Gui.text_view, NULL);
	gtk_widget_modify_text (Gui.text_view, GTK_STATE_NORMAL, NULL);

	style = gtk_widget_get_modifier_style (Gui.text_view);
	cursor_colour = &style->text[GTK_STATE_NORMAL];
    }

    sprintf(buffer,
	    "style \"level9-style\" {\n"
	    "  GtkTextView::cursor-color = { %d, %d, %d }\n"
	    "}\n"
	    "\n"
	    "class \"GtkTextView\" style \"level9-style\"\n",
	    cursor_colour->red, cursor_colour->green, cursor_colour->blue);
    gtk_rc_parse_string (buffer);
#else
    if (Config.text_fg && gdk_color_parse (Config.text_fg, &colour))
	gtk_widget_modify_text (Gui.text_view, GTK_STATE_NORMAL, &colour);
    else
	gtk_widget_modify_text (Gui.text_view, GTK_STATE_NORMAL, NULL);
#endif

    if (Config.text_bg && gdk_color_parse (Config.text_bg, &colour))
	gtk_widget_modify_base (Gui.text_view, GTK_STATE_NORMAL, &colour);
    else
	gtk_widget_modify_base (Gui.text_view, GTK_STATE_NORMAL, NULL);
}

/* ------------------------------------------------------------------------- *
 * Command history. I would have liked to use GList or some other GLib data  *
 * type, but as far as I can see none of them would quite fit my needs.      *
 * Instead, the commands are stored in a circular buffer. These are always   *
 * tricky to get right, but I believe the one below works now.               *
 * ------------------------------------------------------------------------- */

#define HISTORY_SIZE 100

static struct
{
    gchar *buffer[HISTORY_SIZE];
    gboolean empty;
    gint start;
    gint end;
    gint retrieve;
} history;

void text_init ()
{
    gint i;

    for (i = 0; i < HISTORY_SIZE; i++)
	history.buffer[i] = NULL;

    history.empty = TRUE;
    history.start = 0;
    history.end = 0;
    history.retrieve = -1;

    /*
     * The text signal handlers are used to detect when the user presses
     * ENTER or Up/Down-arrow and to make sure the text at the command prompt
     * stays editable.
     */

    hSigInsert = g_signal_connect_after (
	G_OBJECT (Gui.text_buffer), "insert-text",
	G_CALLBACK (sig_insert), NULL);
    hSigDelete = g_signal_connect_after (
	G_OBJECT (Gui.text_buffer), "delete-range",
	G_CALLBACK (sig_delete), NULL);
    hSigKeypress = g_signal_connect (
	G_OBJECT (Gui.text_view), "key-press-event",
	G_CALLBACK (sig_keypress), NULL);

    set_input_pending (FALSE);
}

static void history_insert (gchar *str)
{
    gchar *new_str;

    if (strlen (str) == 0)
	return;
    
    new_str = g_strdup (str);

    if (!history.empty)
    {
	if (++history.end >= HISTORY_SIZE)
	    history.end = 0;

	if (history.end == history.start && ++history.start >= HISTORY_SIZE)
	    history.start = 0;
    } else
	history.empty = FALSE;

    if (history.buffer[history.end])
	g_free (history.buffer[history.end]);

    history.retrieve = -1;
    history.buffer[history.end] = new_str;
}

static gchar *history_retrieve (gint direction)
{
    if (history.empty)
	return NULL;

    if (direction > 0)
    {
	if (history.retrieve == history.end)
	    history.retrieve = -1;

	if (history.retrieve == -1)
	    return "";

	if (++history.retrieve >= HISTORY_SIZE)
	    history.retrieve = 0;
    } else if (history.retrieve != -1)
    {
	if (history.retrieve == history.start)
	    return NULL;
	    
	if (--history.retrieve < 0)
	    history.retrieve = HISTORY_SIZE - 1;
    } else
	history.retrieve = history.end;

    return history.buffer[history.retrieve];
}

static gboolean do_history (gint direction)
{
    GtkTextIter start, end;

    gtk_text_buffer_get_iter_at_mark (
	Gui.text_buffer, &start,
	gtk_text_buffer_get_insert (Gui.text_buffer));
    gtk_text_buffer_get_end_iter (Gui.text_buffer, &end);

    if (gtk_text_iter_editable (&start, FALSE) ||
	gtk_text_iter_equal (&start, &end))
    {
	gchar *str;

	str = history_retrieve (direction);
	if (str)
	{
	    gtk_text_buffer_get_iter_at_mark (
		Gui.text_buffer, &start, inputMark);
	    gtk_text_buffer_place_cursor (Gui.text_buffer, &start);
	    gtk_text_buffer_delete (Gui.text_buffer, &start, &end);
	    gtk_text_buffer_insert_at_cursor (Gui.text_buffer, str, -1);
	    gtk_text_view_scroll_mark_onscreen (
		GTK_TEXT_VIEW (Gui.text_view), inputMark);
	}

	return TRUE;
    }

    return FALSE;
}

/* ------------------------------------------------------------------------- *
 * Retreiving and parsing user input.                                        *
 * ------------------------------------------------------------------------- */

static void fetch_command_at_prompt ()
{
    GtkTextIter start;
    GtkTextIter end;
    gchar *buf;
    gunichar ch;
    int copied = 0;

    gtk_text_buffer_get_iter_at_mark (Gui.text_buffer, &start, inputMark);
    gtk_text_buffer_get_end_iter (Gui.text_buffer, &end);

    if (inputBuffer.buf) {
	/*
	 * HACK: Remove leading and trailing spaces to ensure that the padding
	 * we use to keep the input area editable isn't included.
	 */
	buf = g_strstrip (
	    gtk_text_buffer_get_text (Gui.text_buffer, &start, &end, FALSE));

	history_insert (buf);

	copied = 0;
    
	while ((ch = g_utf8_get_char (buf))) {
	    if (ch >= 0x20 && ch <= 0x7f) {
		if (copied >= inputBuffer.size - 1)
		    break;
		inputBuffer.buf[copied++] = ch;
	    }
	    buf = g_utf8_next_char (buf);
	}

	inputBuffer.buf[copied] = 0;
    }

    /*
     * I need to get rid of both "level9-input" and "level9-input-padding"
     * here, to ensure that there is no editable areas remaining around the old
     * input. But if I simply remove them the padded text will again become
     * visible, which isn't nice. Let's remove the old text completely, and
     * re-insert it with the "level9-old-input" tag.
     */

    set_input_pending (FALSE);
    gtk_text_buffer_delete (Gui.text_buffer, &start, &end);
    set_input_pending (TRUE);

    gtk_text_buffer_get_end_iter (Gui.text_buffer, &end);

    if (inputBuffer.buf)
    {
	gtk_text_buffer_insert_with_tags_by_name (
	    Gui.text_buffer, &end, inputBuffer.buf, -1, "level9-old-input",
	    NULL);
    }
}

/* ------------------------------------------------------------------------- *
 * Signal handling.                                                          *
 * ------------------------------------------------------------------------- */

/*
 * By design, new text on the boundary of a tag is not affected by that tag.
 * In this case that means that the new text may be non-editable. So every
 * time the user adds new text we apply the input tag to the entire input
 * region of the buffer. As far as I can understand, all the overlapping input
 * tags will be automagically merged.
 */

static void sig_insert (GtkTextBuffer *buffer, GtkTextIter *arg1,
			gchar *arg2, gint arg3, gpointer user_data)
{
    GtkTextIter start;
    GtkTextIter end;

    gtk_text_buffer_get_iter_at_mark (Gui.text_buffer, &start, inputMark);
    gtk_text_buffer_get_end_iter (Gui.text_buffer, &end);
    gtk_text_buffer_apply_tag_by_name (
	Gui.text_buffer, "level9-input", &start, &end);
}

/*
 * A tag can be considered a pair of marks, defining the two edges of the
 * text to be tagged. However, when the mark and the anti-mark connects, both
 * will be deleted.
 *
 * HACK: This means that it's not possible to have an empty editable region,
 * so we make sure we always have at least one space in it.
 */

static void sig_delete (GtkTextBuffer *buffer, GtkTextIter *arg1,
			GtkTextIter *arg2, gpointer user_data)
{
    GtkTextIter start;
    GtkTextIter end;

    gtk_text_buffer_get_iter_at_mark (Gui.text_buffer, &start, inputMark);
    gtk_text_buffer_get_end_iter (Gui.text_buffer, &end);
    if (gtk_text_iter_equal (&start, &end))
    {
	gtk_text_buffer_insert_with_tags_by_name (
	    Gui.text_buffer, &start, " ", -1, "level9-input-padding", NULL);
	gtk_text_buffer_get_iter_at_mark (Gui.text_buffer, &start, inputMark);
	gtk_text_buffer_place_cursor (Gui.text_buffer, &start);
    }
}

static gboolean sig_keypress (GtkWidget *widget, GdkEventKey *event,
			      gpointer user_data)
{
    /* Ignore keypresses with the Shift or Control modifier. */
    if (event->state & (GDK_SHIFT_MASK | GDK_CONTROL_MASK))
	return FALSE;

    switch (event->keyval)
    {
	case GDK_KP_Enter:
	case GDK_Return:
	    gtk_main_quit ();
	    return TRUE;

	case GDK_Up:
	case GDK_KP_Up:
	    return do_history (-1);

	case GDK_Down:
	case GDK_KP_Down:
	    return do_history (1);
	    
	default:
	    break;
    }

    return FALSE;
}

static void set_input_pending (gboolean pending)
{
    if (pending)
    {
	g_signal_handler_unblock (G_OBJECT (Gui.text_buffer), hSigInsert);
	g_signal_handler_unblock (G_OBJECT (Gui.text_buffer), hSigDelete);
	g_signal_handler_unblock (G_OBJECT (Gui.text_view), hSigKeypress);
    } else
    {
	g_signal_handler_block (G_OBJECT (Gui.text_buffer), hSigInsert);
	g_signal_handler_block (G_OBJECT (Gui.text_buffer), hSigDelete);
	g_signal_handler_block (G_OBJECT (Gui.text_view), hSigKeypress);
    }
}

/* ------------------------------------------------------------------------- *
 * The text buffer.                                                          *
 * ------------------------------------------------------------------------- */

void os_printchar (char c)
{
    if (!bufferedText)
	bufferedText = g_string_new (NULL);

    if (c == '\r')
	g_string_append (bufferedText, "\n");
    else if (c >= 0x20)
	g_string_append_c (bufferedText, c);
}

void os_flush ()
{
    gint line_count;

    if (!bufferedText || bufferedText->len == 0)
	return;

    set_input_pending (FALSE);

    gtk_text_buffer_insert_at_cursor (
	Gui.text_buffer, bufferedText->str, bufferedText->len);

    g_string_truncate (bufferedText, 0);

    line_count = gtk_text_buffer_get_line_count (Gui.text_buffer);

    if (line_count > MAX_SCROLLBACK)
    {
	GtkTextIter start, end;

	gtk_text_buffer_get_start_iter (Gui.text_buffer, &start);
	gtk_text_buffer_get_iter_at_line (
	    Gui.text_buffer, &end, line_count - MAX_SCROLLBACK);

	gtk_text_buffer_delete (Gui.text_buffer, &start, &end);
    }

    set_input_pending (TRUE);
}

/* ------------------------------------------------------------------------- *
 * Main text input display function.                                         *
 * ------------------------------------------------------------------------- */

void scroll_text_buffer (gboolean force)
{
    GtkTextIter iter;

    /* Make the text at the input prompt editable */
	
    gtk_text_buffer_get_end_iter (Gui.text_buffer, &iter);

    if (!inputMark)
	inputMark = gtk_text_buffer_create_mark (
	    Gui.text_buffer, NULL, &iter, TRUE);
    else
    {
	if (!force)
	{
	    GtkTextIter iter2;
	
	    gtk_text_buffer_get_iter_at_mark (
		Gui.text_buffer, &iter2, inputMark);

	    /*
	     * The Level 9 games have the annoying habit of calling os_readchar
	     * repeatedly, with a very short timeout. If I scroll the buffer
	     * every time it becomes impossible to use scrollback in games that
	     * use this extensively, e.g. the Adrian Mole games.
	     *
	     * Try to avoid this by only scrolling when the input mark isn't
	     * already at the end of the buffer. I have a feeling that this is
	     * a quite brittle solution, but it appears to be working at the
	     * moment.
	     */
	
	    if (gtk_text_iter_equal (&iter, &iter2))
		return;
	}

	/*
	 * Scroll the view so that the previous input is still visible. It's
	 * not quite a substitute for a "more" prompt, but it will have to do
	 * for now.
	 */
	gtk_text_view_scroll_to_mark (
	    GTK_TEXT_VIEW (Gui.text_view), inputMark,
	    0.0, TRUE, 0.0, 0.0);
	gtk_text_buffer_move_mark (Gui.text_buffer, inputMark, &iter);
    }

    gtk_text_buffer_insert_with_tags_by_name (
	Gui.text_buffer, &iter, " ", -1, "level9-input-padding", NULL);
    gtk_text_buffer_get_iter_at_mark (
	Gui.text_buffer, &iter, inputMark);

    gtk_text_buffer_place_cursor (Gui.text_buffer, &iter);
}

static gboolean readchar_keypress (GtkWidget *widget, GdkEventKey *event,
				   gpointer user_data)
{
    char *c = (char *) user_data;

    *c = (char) event->keyval;
    gtk_main_quit ();
    graphics_interact ();
    return FALSE;
}

static gboolean readchar_timeout (gpointer user_data)
{
    gtk_main_quit ();
    return FALSE;
}

char os_readchar (int millis)
{
    gulong sig;
    guint timer;
    char c = 0;

    if (inputBlock != INPUT_NOT_BLOCKED)
	return 0;

    if (applicationExiting)
	return 0;

    inputBuffer.buf = NULL;
    inputBuffer.size = 0;

    scroll_text_buffer (FALSE);

    timer = g_timeout_add (millis, readchar_timeout, NULL);
    sig = g_signal_connect (G_OBJECT (Gui.main_window), "key_press_event",
			    G_CALLBACK (readchar_keypress), &c);

    graphics_run ();

    stop_main_loop ();
    waitingForInput = TRUE;
    gtk_main ();
    waitingForInput = FALSE;
    start_main_loop ();

    if (applicationExiting)
    {
	gtk_main_quit ();
	return 0;
    }

    g_signal_handler_disconnect (G_OBJECT (Gui.main_window), sig);
    g_source_remove (timer);

    if (abortInput)
    {
	abortInput = FALSE;
	return 0;
    }

    fetch_command_at_prompt ();
    return c;
}

L9BOOL os_input (char *ibuff, int size)
{
    if (inputBlock != INPUT_NOT_BLOCKED)
	return FALSE;

    if (applicationExiting)
	return FALSE;

    inputBuffer.buf = ibuff;
    inputBuffer.size = size;
    
    set_input_pending (TRUE);
    scroll_text_buffer (TRUE);

    graphics_run ();

    stop_main_loop ();
    waitingForInput = TRUE;
    gtk_main ();
    waitingForInput = FALSE;
    start_main_loop ();

    if (applicationExiting)
    {
	gtk_main_quit ();
	return FALSE;
    }

    set_input_pending (FALSE);

    if (abortInput)
    {
	abortInput = FALSE;
	return FALSE;
    }

    graphics_interact ();
    fetch_command_at_prompt ();
    return TRUE;
}
