/*
 * main.c - GTK+ 2.x interface for Magnetic 2.x
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

#include <string.h>
#include <gtk/gtk.h>

#include "defs.h"
#include "main.h"
#include "version.h"
#include "config.h"
#include "gui.h"
#include "text.h"
#include "graphics.h"
#include "hints.h"
#include "util.h"

static gboolean main_loop (gpointer data);

gboolean applicationExiting = FALSE;

/* Exit the application */

void close_application (GtkWidget *widget, gpointer user_data)
{
    write_config_file ();
    stop_recording (TRUE);
    stop_scripting (TRUE);
    stop_replaying (TRUE);
    gtk_main_quit ();
    applicationExiting = TRUE;
}

/* ------------------------------------------------------------------------- *
 * Main Loop                                                                 *
 * ------------------------------------------------------------------------- */

static guint mainIdleHandler = 0;

void start_main_loop ()
{
    if (!mainIdleHandler)
	mainIdleHandler = g_idle_add (main_loop, NULL);
}

void stop_main_loop ()
{
    if (mainIdleHandler)
    {
	g_source_remove (mainIdleHandler);
	mainIdleHandler = 0;
    }
}

/*
 * This function will be called as an idle handler, i.e. it will be called
 * whenever GTK+ doesn't have anything more important to do.
 */

static gboolean main_loop (gpointer data)
{
    gboolean result;
    int i;

    /*
     * Make sure we turn off the idle handler if the game isn't running. This
     * shouldn't happen.
     */
    if (!ms_is_running ())
    {
	g_warning ("main_loop was called while the game wasn't running");
	mainIdleHandler = 0;
	return FALSE;
    }

    for (i = 0; i < MAX_INSTRUCTIONS; i++)
    {
	result = ms_rungame ();

	if (!result)
	{
	    text_insert ("\n[End of session]\n");
	    mainIdleHandler = 0;
	    ms_flush ();
	    ms_stop ();
	    ms_freemem ();

	    gtk_text_view_scroll_mark_onscreen (
		GTK_TEXT_VIEW (Gui.text_view),
		gtk_text_buffer_get_insert (Gui.text_buffer));
	    break;
	}
    }

    return result;
}

type8 ms_load_file (type8s *name, type8 *ptr, type16 size)
{
    gchar *filename;
    GIOChannel *file = NULL;
    gsize bytes_read;
    GError *error = NULL;

    const gchar *filters[] =
	{
	    "Saved game files (*.sav)", "*.sav",
	    NULL
	};

    if (!name)
    {
	filename = file_selector (FALSE, NULL, filters, "Restore game");
	if (!filename)
	    return -1;
    } else
	filename = g_strdup ((gchar *) name);

    if (g_file_test (filename, G_FILE_TEST_EXISTS))
    {
	file = g_io_channel_new_file (filename, "r", &error);
	g_io_channel_set_encoding (file, NULL, &error);
	g_io_channel_read_chars (file, (gchar *) ptr, size, &bytes_read, &error);
	g_io_channel_unref (file);
    }
    g_free (filename);
    return file ? 0 : -1;
}

type8 ms_save_file (type8s *name, type8 *ptr, type16 size)
{
    gchar *filename;
    GIOChannel *file;
    gsize bytes_written;
    GError *error = NULL;

    const gchar *filters[] =
	{
	    "Saved game files (*.sav)", "*.sav",
	    NULL
	};

    if (!name)
    {
	filename = file_selector (TRUE, NULL, filters, "Save game");
	if (!filename)
	    return -1;
    } else
	filename = g_strdup ((gchar *) name);

    file = g_io_channel_new_file (filename, "w", &error);
    g_io_channel_set_encoding (file, NULL, &error);
    g_io_channel_write_chars (file, (gchar *) ptr, size, &bytes_written, &error);
    g_io_channel_unref (file);
    g_free (filename);
    return 0;
}

void ms_fatal (type8s *txt)
{
    /* We really should do something more sensible here */
    g_warning ("Magnetic Fatal Error: %s", txt);
}

void ms_playmusic(type8 * midi_data, type32 length, type16 tempo)
{
}

void do_about ()
{
    GtkWidget *about;
    const gchar *authors[] = {
	"Niclas Karlsson <nkarlsso@abo.fi>",
	"David Kinder <d.kinder@btinternet.com>",
	"Stefan Meier <Stefan.Meier@if-legends.org>",
	"Paul David Doherty <pdd@if-legends.org>",
	NULL
    };

    about = gtk_about_dialog_new ();

    gtk_show_about_dialog (
	GTK_WINDOW (Gui.main_window),
	"name",
	"Magnetic Scrolls Interpreter",
	
	"version",
	"v2.3",
	
	"copyright",
	"Copyright (C) 1997-2008 Niclas Karlsson",

	"comments",
	"GTK+ 2.6 interface v" MAGNETIC_VERSION_GUI
	" by Torbj\303\266rn Andersson <d91tan@Update.UU.SE>",

	"authors",
	authors,
	
	"license",
	"This program is free software; you can redistribute it and/or modify\n"
	"it under the terms of the GNU General Public License as published by\n"
	"the Free Software Foundation; either version 2 of the License, or\n"
	"(at your option) any later version.\n\n"

	"This program is distributed in the hope that it will be useful,\n"
	"but WITHOUT ANY WARRANTY; without even the implied warranty of\n"
	"MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\n"
	"GNU General Public License for more details.\n\n"

	"You should have received a copy of the GNU General Public License\n"
	"along with this program; if not, write to the Free Software\n"
	"Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111, USA.\n",

	NULL);

    gtk_widget_destroy (about);
}

static gchar *change_file_extension (gchar *filename, gchar *extension)
{
    gchar *new_filename;
    gchar *ptr;

    ptr = strrchr (filename, '.');
    if (ptr)
	*ptr = '\0';

    new_filename = g_strconcat (filename, ".", extension, NULL);

    if (ptr)
	*ptr = '.';
    
    if (!g_file_test (new_filename, G_FILE_TEST_EXISTS))
    {
	for (ptr = strrchr (new_filename, '.'); *ptr; ptr++)
	    *ptr = g_ascii_toupper (*ptr);
    }

    return new_filename;
}

gboolean start_new_game (gchar *game_filename, gchar *graphics_filename,
			 gchar *splash_filename, gchar *music_filename,
			 gchar *hints_filename)
{
    const gchar *filters[] =
	{
	    "Magnetic Scrolls data file (*.mag)", "*.mag",
	    NULL
	};

    if (!game_filename)
	game_filename = file_selector (FALSE, NULL, filters, "Open game file");

    if (!game_filename)
	return TRUE;

    stop_main_loop ();

    if (ms_is_running ())
    {
	ms_stop ();
	ms_freemem ();
    }

    stop_recording (TRUE);
    stop_scripting (TRUE);
    stop_replaying (TRUE);

    if (!graphics_filename)
	graphics_filename = change_file_extension (game_filename, "gfx");

    if (!splash_filename)
	splash_filename = change_file_extension (game_filename, "png");
    
    if (!music_filename)
	music_filename = change_file_extension (game_filename, "mp3");

    if (!hints_filename)
	hints_filename = change_file_extension (game_filename, "hnt");

    display_splash_screen (splash_filename, music_filename);

    text_clear ();
    graphics_clear ();
    hints_clear ();

    if (applicationExiting)
	return FALSE;

    if (!ms_init ((type8s *) game_filename, (type8s *) graphics_filename, (type8s *) hints_filename), NULL)
    {
	GtkWidget *error;
	gchar *basename;
	
	basename = g_path_get_basename (game_filename);
	error = gtk_message_dialog_new (
	    GTK_WINDOW (Gui.main_window),
	    GTK_DIALOG_DESTROY_WITH_PARENT,
	    GTK_MESSAGE_ERROR,
	    GTK_BUTTONS_OK,
	    "Could not start the game! The most likely cause is\n"
	    "that '%s' is not a valid game file.",
	    basename);
	gtk_dialog_run (GTK_DIALOG (error));
	g_free (basename);
	gtk_widget_destroy (error);
    } else
	start_main_loop ();
    
    g_free (game_filename);
    g_free (graphics_filename);
    g_free (splash_filename);
    g_free (music_filename);
    g_free (hints_filename);
    
    gtk_widget_grab_focus (Gui.text_view);
    return TRUE;
}

int main (int argc, char *argv[])
{
    gchar *game_filename = NULL;
    gchar *graphics_filename = NULL;
    gchar *splash_filename = NULL;
    gchar *music_filename = NULL;
    gchar *hints_filename = NULL;

    gtk_init (&argc, &argv);

    gui_init ();
    text_init ();
    graphics_init ();

    read_config_file ();

    if (argc >= 6)
	hints_filename = g_strdup (argv[5]);
    if (argc >= 5)
	music_filename = g_strdup (argv[4]);
    if (argc >= 4)
	splash_filename = g_strdup (argv[3]);
    if (argc >= 3)
	graphics_filename = g_strdup (argv[2]);
    if (argc >= 2)
	game_filename = g_strdup (argv[1]);

    gtk_widget_show_all (Gui.main_window);

    if (start_new_game (game_filename,
			graphics_filename,
			splash_filename,
			music_filename,
			hints_filename))
	gtk_main ();

    return 0;  
}
