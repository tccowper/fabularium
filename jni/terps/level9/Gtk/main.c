/*
 * main.c - GTK+ 2.x interface for Level9 5.1
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

#include <string.h>
#include <gtk/gtk.h>

#include "level9.h"

#include "main.h"
#include "version.h"
#include "config.h"
#include "gui.h"
#include "text.h"
#include "graphics.h"
#include "util.h"

static gboolean main_loop (gpointer user_data);

gboolean applicationExiting = FALSE;

/* Exit the application */

void close_application (GtkWidget *widget, gpointer user_data)
{
    stop_main_loop ();
    StopGame ();
    write_config_file ();
    applicationExiting = TRUE;
    graphics_reinit ();
    text_reinit ();
    gtk_main_quit ();
}

/* ------------------------------------------------------------------------- *
 * Main Loop                                                                 *
 * ------------------------------------------------------------------------- */

static guint mainIdleHandler = 0;

gboolean start_main_loop ()
{
    if (mainIdleHandler)
	return FALSE;

    mainIdleHandler = g_idle_add (main_loop, NULL);
    return TRUE;
}

gboolean stop_main_loop ()
{
    if (!mainIdleHandler)
	return FALSE;

    g_source_remove (mainIdleHandler);
    mainIdleHandler = 0;
    return TRUE;
}

static gboolean main_loop (gpointer user_data)
{
    gboolean result;
    int i;

    /*
     * Run a couple of instructions. This seems to be a bit more efficient than
     * having the idle handler run one instruction every call. Maybe.
     */
    
    for (i = 0; i < MAX_INSTRUCTIONS; i++)
    {
	result = RunGame ();
	if (!result)
	{
	    mainIdleHandler = 0;
	    break;
	}
    }

    return result;
}

L9BOOL os_load_file (L9BYTE *Ptr, int *Bytes, int Max)
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

    filename = file_selector (FALSE, NULL, filters, "Restore game");
    if (!filename)
	return FALSE;

    if (g_file_test (filename, G_FILE_TEST_EXISTS))
    {
	gchar *ptr = (gchar *) Ptr;

	file = g_io_channel_new_file (filename, "r", &error);
	g_io_channel_set_encoding (file, NULL, &error);
	g_io_channel_read_chars (file, ptr, Max, &bytes_read, &error);
	g_io_channel_unref (file);

	*Bytes = bytes_read;
    }
    g_free (filename);
    return file != NULL;
}

L9BOOL os_save_file (L9BYTE *Ptr, int Bytes)
{
    gchar *ptr = (gchar *) Ptr;
    gchar *filename;
    GIOChannel *file;
    gsize bytes_written;
    GError *error = NULL;

    const gchar *filters[] =
	{
	    "Saved game files (*.sav)", "*.sav",
	    NULL
	};

    filename = file_selector (TRUE, NULL, filters, "Save game");
    if (!filename)
	return FALSE;

    file = g_io_channel_new_file (filename, "w", &error);
    g_io_channel_set_encoding (file, NULL, &error);
    g_io_channel_write_chars (file, ptr, Bytes, &bytes_written, &error);
    g_io_channel_unref (file);
    g_free (filename);
    return TRUE;
}

FILE* os_open_script_file (void)
{
    gchar *filename;

    filename = file_selector (FALSE, NULL, NULL, "Play script");
    if (!filename)
	return NULL;

    return fopen (filename, "rt");
}

void do_about ()
{
    GtkWidget *about;

    about = gtk_about_dialog_new ();

    gtk_show_about_dialog (
	GTK_WINDOW (Gui.main_window),
	"name",
	"Level 9 Interpreter",
	
	"version",
	"v5.1",
	
	"copyright",
	"Copyright (c) 1996-2011 Glen Summers and contributors.\n"
	"Contributions from David Kinder, Alan Staniforth, Simon Baldwin,\n"
	"Dieter Baron and Andreas Scherrer.",

	"comments",
	"GTK+ 2.6 interface v" LEVEL9_VERSION_GUI
	" by Torbj\303\266rn Andersson <d91tan@Update.UU.SE>",
	
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

void start_new_game (gchar *game_filename, gchar *graphics_filename)
{
    gchar *graphics_dir;
    int i;

    const gchar *filters[] =
	{
	    "Level 9 data files (*.dat)", "*.dat",
	    "Spectrum snapshots (*.sna)", "*.sna",
	    NULL
	};
    
    if (!game_filename)
	game_filename = file_selector (FALSE, NULL, filters, "Open game file");
#ifdef MAKE_FILENAMES_ABSOLUTE
    else
    {
	gchar *tmp;

	tmp = game_filename;
	game_filename = make_filename_absolute (game_filename);
	g_free (tmp);
    }
#endif

    if (!game_filename)
	return;

    if (graphics_filename)
	graphics_dir = g_strdup (graphics_filename);
    else
	graphics_dir = g_strdup (game_filename);

    for (i = strlen (graphics_dir) - 1; i >= 0; i--)
    {
	if (G_IS_DIR_SEPARATOR (graphics_dir[i]))
	{
	    graphics_dir[i + 1] = 0;
	    break;
	}
    }

    if (i < 0)
	graphics_dir[0] = 0;
    
    if (!graphics_filename)
	graphics_filename = change_file_extension (game_filename, "cga");

    if (!g_file_test (graphics_filename, G_FILE_TEST_EXISTS))
    {
	g_free (graphics_filename);
	graphics_filename = change_file_extension (game_filename, "hrc");
    }

    if (!g_file_test (graphics_filename, G_FILE_TEST_EXISTS))
    {
	g_free (graphics_filename);
	graphics_filename = g_strdup_printf ("%spicture.dat", graphics_dir);
    }

    text_reinit ();
    graphics_reinit ();

    graphics_set_directory (graphics_dir);

    if (!LoadGame (game_filename, graphics_filename))
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
    {
	gtk_widget_grab_focus (Gui.text_view);
	start_main_loop ();
    }

    g_free (game_filename);
    g_free (graphics_filename);
}

int main (int argc, char *argv[])
{
    gchar *game_filename = NULL;
    gchar *graphics_filename = NULL;

    gtk_init (&argc, &argv);

    gui_init ();
    text_init ();
    graphics_init ();

    read_config_file ();

    if (argc >= 3)
	graphics_filename = g_strdup (argv[2]);
    if (argc >= 2)
	game_filename = g_strdup (argv[1]);

    gtk_widget_show_all (Gui.main_window);
    gtk_widget_hide (Gui.statusbar);
    start_new_game (game_filename, graphics_filename);
    gtk_main ();

    return 0;  
}
