/*
 * text.c - Text buffer
 * Copyright (c) 2002 Torbjörn Andersson <d91tan@Update.UU.SE>
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

#include "defs.h"
#include "main.h"
#include "config.h"
#include "gui.h"
#include "text.h"
#include "util.h"

static GtkTextMark *inputMark = NULL;
static gchar *inputBuffer = NULL;
static gchar *inputPtr = NULL;

static gboolean promptingForSeed = FALSE;

static GIOChannel *recordFile = NULL;
static GIOChannel *scriptFile = NULL;
static GIOChannel *replayFile = NULL;

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

static void clear_input_buffer ()
{
    g_free (inputBuffer);
    inputBuffer = NULL;
    inputPtr = NULL;
}

void text_insert (gchar *fmt, ...)
{
    va_list args;
    gchar *str, *ptr;
    
    va_start (args, fmt);
    str = g_strdup_vprintf (fmt, args);
    va_end (args);

    for (ptr = str; *ptr != '\0'; ptr++)
	ms_putchar (*ptr);

    g_free (str);
}

void text_clear (void)
{
    GtkTextIter start, end;

    /*
     * The first call to ms_statuschar() will clear the internal statusline
     * buffer. The second will clear the actual statusline. This is a bit of
     * a hack...
     */
    ms_statuschar (0x0a);
    ms_statuschar (0x0a);

    set_input_pending (FALSE);
    gtk_text_buffer_get_bounds (Gui.text_buffer, &start, &end);
    gtk_text_buffer_delete (Gui.text_buffer, &start, &end);

    promptingForSeed = FALSE;
    clear_input_buffer ();
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
	if (!Config.status_font)
	{
	    gtk_widget_modify_font (
		GTK_WIDGET (Gui.statusline.left), font_desc);
	    gtk_widget_modify_font (
		GTK_WIDGET (Gui.statusline.right), font_desc);
	}
	pango_font_description_free (font_desc);
    }

    if (Config.status_font)
    {
	PangoFontDescription *font_desc;
	
	font_desc = pango_font_description_from_string (Config.status_font);
	gtk_widget_modify_font (GTK_WIDGET (Gui.statusline.left), font_desc);
	gtk_widget_modify_font (GTK_WIDGET (Gui.statusline.right), font_desc);
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

    if (Config.status_fg && gdk_color_parse (Config.status_fg, &colour))
    {
	gtk_widget_modify_fg (Gui.statusline.left, GTK_STATE_NORMAL, &colour);
	gtk_widget_modify_fg (Gui.statusline.right, GTK_STATE_NORMAL, &colour);
    } else
    {
	gtk_widget_modify_fg (Gui.statusline.left, GTK_STATE_NORMAL, NULL);
	gtk_widget_modify_fg (Gui.statusline.right, GTK_STATE_NORMAL, NULL);
    }

    if (Config.status_bg && gdk_color_parse (Config.status_bg, &colour))
	gtk_widget_modify_bg (Gui.statusline.viewport, GTK_STATE_NORMAL,
			      &colour);
    else
	gtk_widget_modify_bg (Gui.statusline.viewport, GTK_STATE_NORMAL, NULL);
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
     * ENTER or Up/Down-arrow, and to make sure the text at the command prompt
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

    gtk_text_buffer_get_iter_at_mark (Gui.text_buffer, &start, inputMark);
    gtk_text_buffer_get_end_iter (Gui.text_buffer, &end);

    /*
     * HACK: Remove leading and trailing spaces to ensure that the padding we
     * use to keep the input area editable isn't included.
     */
    inputBuffer = g_strstrip (
	gtk_text_buffer_get_text (Gui.text_buffer, &start, &end, FALSE));
    inputPtr = inputBuffer;

    history_insert (inputBuffer);

    /*
     * I need to get rid of both "magnetic-input" and "magnetic-input-padding"
     * here, to ensure that there is no editable areas remaining around the old
     * input. But if I simply remove them the padded text will again become
     * visible, which isn't nice. Let's remove the old text completely, and
     * re-insert it with the "magnetic-old-input" tag.
     */

    set_input_pending (FALSE);
    gtk_text_buffer_delete (Gui.text_buffer, &start, &end);
    set_input_pending (TRUE);
			    
    gtk_text_buffer_get_end_iter (Gui.text_buffer, &end);
    gtk_text_buffer_insert_with_tags_by_name (
	Gui.text_buffer, &end, inputBuffer, -1, "magnetic-old-input", NULL);

    gtk_text_buffer_place_cursor (Gui.text_buffer, &end);

    /*
     * HACK: If we're doing a playback, then the command will be written to
     * the transcript file by ms_flush(), so there's no need to do it here.
     */
    
    if (scriptFile && !replayFile)
    {
	GError *error = NULL;
	gsize bytes_written;

	g_io_channel_write_chars (
	    scriptFile, inputBuffer, -1, &bytes_written, &error);
    }

    if (recordFile && strlen (inputBuffer) > 0)
    {
	GError *error = NULL;
	gsize bytes_written;

	g_io_channel_write_chars (
	    recordFile, inputBuffer, -1, &bytes_written, &error);
	g_io_channel_write_chars (
	    recordFile, "\n", -1, &bytes_written, &error);
    }
}

static void new_random_seed (gchar *str)
{
    type32 seed;

    seed = atol (str);
    text_insert ("\n[New random seed: %ld]", seed);
    ms_seed (seed);
}

gboolean special_command (char *cmd)
{
    if (promptingForSeed)
    {
	new_random_seed (cmd);
	promptingForSeed = FALSE;
	return TRUE;
    }
    
    if (g_ascii_strcasecmp (cmd, "#RECORDING ON") == 0 ||
	g_ascii_strcasecmp (cmd, "#RECORD ON") == 0)
    {
	start_recording (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#RECORDING OFF") == 0 ||
	g_ascii_strcasecmp (cmd, "#RECORD OFF") == 0)
    {
	stop_recording (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#RECORDING") == 0 ||
	g_ascii_strcasecmp (cmd, "#RECORD") == 0)
    {
	if (recordFile)
	    stop_recording (FALSE);
	else
	    start_recording (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#SCRIPTING ON") == 0 ||
	g_ascii_strcasecmp (cmd, "#SCRIPT ON") == 0)
    {
	start_scripting (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#SCRIPTING OFF") == 0 ||
	g_ascii_strcasecmp (cmd, "#SCRIPT OFF") == 0)
    {
	stop_scripting (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#SCRIPTING") == 0 ||
	g_ascii_strcasecmp (cmd, "#SCRIPT") == 0)
    {
	if (scriptFile)
	    stop_scripting (FALSE);
	else
	    start_scripting (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#REPLAYING ON") == 0 ||
	g_ascii_strcasecmp (cmd, "#REPLAY ON") == 0)
    {
	start_replaying (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#REPLAYING OFF") == 0 ||
	g_ascii_strcasecmp (cmd, "#REPLAY OFF") == 0)
    {
	stop_replaying (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#REPLAYING") == 0 ||
	g_ascii_strcasecmp (cmd, "#REPLAY") == 0)
    {
	if (replayFile)
	    stop_replaying (FALSE);
	else
	    start_replaying (FALSE);
	return TRUE;
    }

    if (g_ascii_strcasecmp (cmd, "#SEED") == 0)
    {
	text_insert ("\n[Enter new random seed at the prompt.]");
	promptingForSeed = TRUE;
	return TRUE;
    }
    
    if (g_ascii_strncasecmp (cmd, "#SEED ", 6) == 0)
    {
	new_random_seed (cmd + 6);
	return TRUE;
    }

    return FALSE;
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
	Gui.text_buffer, "magnetic-input", &start, &end);
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
	    Gui.text_buffer, &start, " ", -1, "magnetic-input-padding", NULL);
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
	    fetch_command_at_prompt ();
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
 * Recording and scripting.                                                  *
 * ------------------------------------------------------------------------- */

typedef enum
{
    FILE_READ,
    FILE_WRITE,
} file_mode_t;

static void start_something (gboolean silent, GIOChannel **file,
			     file_mode_t mode, gchar *verbing,
			     gchar *cverbing, gchar *noun)
{
    /*
     * HACK: This function may seem to block while waiting for the user to
     * supply a filename, but that won't keep the GTK+ main loop from
     * invoking the idle handler, which will invoke the game main loop, which
     * will call ms_getchar().
     *
     * We solve this by shutting down the idle handler. We could avoid this
     * by doing the special command parsing in sig_keypress(), but in the
     * end I decided this was a worse hack.
     */

    stop_main_loop ();
    
    if (!*file)
    {
	gchar *filename;
	GError *error = NULL;

	if (mode == FILE_READ)
	    filename = file_selector (FALSE, NULL, NULL, "Open %s", noun);
	else
	    filename = file_selector (TRUE, NULL, NULL, "Save %s", noun);

	switch (mode)
	{
	    case FILE_READ:
		if (!filename)
		{
		    if (!silent)
			text_insert ("\n[%s aborted.]", cverbing);
		    return;
		}

		*file = g_io_channel_new_file (filename, "r", &error);
		break;
		
	    case FILE_WRITE:
		if (!filename)
		{
		    if (filename)
			g_free (filename);

		    if (!silent)
			text_insert ("\n[%s aborted.]", cverbing);
		    return;
		}

		*file = g_io_channel_new_file (filename, "w", &error);
		break;

	    default:
		break;
	}

	if (filename)
	    g_free (filename);
	
	if (!silent)
	    text_insert ("\n[%s on.]", cverbing);
    } else if (!silent)
	text_insert ("\n[Already %s.]", verbing);

    start_main_loop ();
}

static void stop_something (gboolean silent, GIOChannel **file,
			    gchar *verbing, gchar *cverbing)
{
    if (*file)
    {
	if (!silent)
	    text_insert ("\n[%s off.]", cverbing);

	g_io_channel_unref (*file);
	*file = NULL;
    } else if (!silent)
	text_insert ("\n[Not %s.]", verbing);
}

void start_recording (gboolean silent)
{
    start_something (silent, &recordFile, FILE_WRITE, "recording",
		     "Recording", "record");
}

void stop_recording (gboolean silent)
{
    stop_something (silent, &recordFile, "recording", "Recording");
}

void start_scripting (gboolean silent)
{
    start_something (silent, &scriptFile, FILE_WRITE, "scripting",
		     "Scripting", "script");
}

void stop_scripting (gboolean silent)
{
    if (scriptFile)
	g_io_channel_write_chars (scriptFile, "\n", -1, NULL, NULL);
    stop_something (silent, &scriptFile, "scripting", "Scripting");
}

void start_replaying (gboolean silent)
{
    start_something (silent, &replayFile, FILE_READ, "replaying",
		     "Replaying", "replay script");
}

void stop_replaying (gboolean silent)
{
    stop_something (silent, &replayFile, "replaying", "Replaying");
}

/* ------------------------------------------------------------------------- *
 * The status line.                                                          *
 * ------------------------------------------------------------------------- */

void ms_statuschar (type8 c)
{
    static gchar buffer[82];
    static gint pos = 0;

    switch (c)
    {
	case 0x0a:
	    gtk_label_set_text (GTK_LABEL (Gui.statusline.left), buffer);
	    gtk_label_set_text (GTK_LABEL (Gui.statusline.right), buffer + 70);

	    pos = 0;
	    memset (buffer, 0, sizeof (buffer));
	    break;
	case 0x09:
	    pos = 70;
	    break;
	default:
	    if (pos < 82)
	    {
		buffer[pos++] = c;
		buffer[pos] = '\0';
	    }
	    break;
    }
}

/* ------------------------------------------------------------------------- *
 * The text buffer.                                                          *
 * ------------------------------------------------------------------------- */

void ms_putchar (type8 c)
{
    if (!bufferedText)
	bufferedText = g_string_new (NULL);

    /*
     * I thought I would have to do something to handle non-ASCII characters,
     * but for whaverer reason that doesn't seem to be necessary. I don't
     * think any of the Magnetic Scrolls games will ever try to output
     * anything else than ASCII anyway, so I'd have to do it myself using
     * text_insert().
     */

    if (c >= 0x20 || c == 0x0a)
	g_string_append_c (bufferedText, c);
    else if (c == 0x08)
    {
	gsize len = bufferedText->len;

	/*
	 * Surely no one will be evil enough to erase the last character when
	 * the buffer is empty? We'd have to remove the character from the
	 * actual text buffer in that case. For now, let's not.
	 */
	if (len > 0)
	    g_string_truncate (bufferedText, len - 1);
    }
}

void ms_flush ()
{
    GError *error = NULL;
    gsize bytes_written;
    gint line_count;

    if (!bufferedText || bufferedText->len == 0)
	return;

    set_input_pending (FALSE);
    
    gtk_text_buffer_insert_at_cursor (
	Gui.text_buffer, bufferedText->str, bufferedText->len);

    if (scriptFile)
    {
	gchar *ptr;
	gchar *space_ptr = NULL;
	gint len;

	for (ptr = bufferedText->str, len = 0; *ptr; ptr++)
	{
	    if (*ptr == '\n')
	    {
		len = 0;
		space_ptr = NULL;
	    } else
		len++;

	    if (*ptr == ' ')
		space_ptr = ptr;

	    if (len >= 78 && space_ptr)
	    {
		*space_ptr = '\n';
		len = ptr - space_ptr;
		space_ptr = NULL;
	    }
	}
	    
	g_io_channel_write_chars (
	    scriptFile, bufferedText->str, -1, &bytes_written, &error);
    }

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

type8 ms_getchar (type8 trans)
{
    gunichar c;

    /*
     * Check if this is the first call to ms_getchar() since the last command
     * was processed.
     */
	
    if (!inputBuffer)
    {
	GtkTextIter iter;

	stop_main_loop ();

	/* Make the text at the input prompt editable */
	
	gtk_text_buffer_get_end_iter (Gui.text_buffer, &iter);

	if (!inputMark)
	    inputMark = gtk_text_buffer_create_mark (
		Gui.text_buffer, NULL, &iter, TRUE);
	else
	{
	    /*
	     * Scroll the view so that the previous input is still visible.
	     * It's not quite a substitute for a "more" prompt, but it will
	     * have to do for now.
	     */
	    gtk_text_view_scroll_to_mark (
		GTK_TEXT_VIEW (Gui.text_view), inputMark,
		0.0, TRUE, 0.0, 0.0);
	    gtk_text_buffer_move_mark (Gui.text_buffer, inputMark, &iter);
	}

	gtk_text_buffer_insert_with_tags_by_name (
	    Gui.text_buffer, &iter, " ", -1, "magnetic-input-padding", NULL);
	gtk_text_buffer_get_iter_at_mark (
	    Gui.text_buffer, &iter, inputMark);
	gtk_text_buffer_place_cursor (Gui.text_buffer, &iter);

	/* If it's a replay, get the command from a file */
	
	if (replayFile)
	{
	    GError *error = NULL;
	    gchar *line;
	    gsize length;
	    GIOStatus res;
	    
	    res = g_io_channel_read_line (
		replayFile, &line, &length, NULL, &error);

	    /*
	     * Remove terminating character, or stop the playback. For some
	     * reason, a length of 0 doesn't always imply EOF.
	     */
	    if (length > 0)
	    {
		line[length - 1] = '\0';
		text_insert (line);
		ms_flush ();
		g_free (line);
		fetch_command_at_prompt ();
	    } else
	    {
		inputBuffer = g_strdup ("");
		inputPtr = inputBuffer;
		if (res == G_IO_STATUS_EOF)
		    stop_replaying (TRUE);
	    }
	}

	/*
	 * If it's not a replay, or if the replay just ended, get the command
	 * from the keyboard.
	 */
	
	if (!replayFile)
	{
	    set_input_pending (TRUE);
	    gtk_main ();
	    if (applicationExiting)
	    {
		gtk_main_quit ();
		return '\n';
	    }
	    set_input_pending (FALSE);
	}

	start_main_loop ();
    }

    /* This will probably happen when starting a completely new game. */
    if (!inputBuffer)
	return 1;

    if (trans != 0 && inputPtr == inputBuffer)
    {
	if (g_ascii_strcasecmp (inputBuffer, "#UNDO") == 0)
	{
	    clear_input_buffer ();
	    return 0;
	}

	if (special_command (inputBuffer))
	{
	    clear_input_buffer ();
	    inputBuffer = g_strdup ("");
	    inputPtr = inputBuffer;
	}
    }

    /*
     * HACK: The Magnetic Windows games behave strangely on empty input, so
     * we fake an input buffer that shouldn't produce any output. Hopefully.
     */
    if (ms_is_magwin () && strlen (inputBuffer) == 0)
    {
	g_free (inputBuffer);
	inputBuffer = g_strdup (".");
	inputPtr = inputBuffer;
    }

    do
    {
	c = g_utf8_get_char (inputPtr);
	inputPtr = g_utf8_next_char (inputPtr);
    } while ((c > 0x7f || c < 0x20) && c != 0x00);

    if (c == 0x00)
    {
	clear_input_buffer ();
	return '\n';
    }

    return (type8) c;
}
