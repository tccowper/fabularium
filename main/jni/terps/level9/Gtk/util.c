/*
 * util.c - Utility functions
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

#include <ctype.h>
#include <stdarg.h>
#include <string.h>
#include <gtk/gtk.h>

#include "level9.h"

#include "gui.h"
#include "text.h"
#include "util.h"

L9BOOL os_stoplist ()
{
    return (os_readchar (0) != 0);
}

L9BOOL os_get_game_file(char *NewName, int Size)
{
    gchar *file_name;

    const gchar *filters[] =
	{
	    "Level 9 data files (*.dat)", "*.dat",
	    "Spectrum snapshots (*.sna)", "*.sna",
	    NULL
	};
    
    file_name =
	file_selector(FALSE, NewName, filters, "Select next game file");

    if (!file_name)
	return FALSE;

    strncpy(NewName, file_name, Size);
    return TRUE;
}

void os_set_filenumber(char *NewName, int Size, int n)
{
    int i;

    /* Assume that the number is one digit only. */
    for (i = strlen(NewName) - 1; i >= 0; i--) {
	if (isdigit(NewName[i])) {
	    NewName[i] = n + '0';
	    break;
	}
    }
}

static gchar *savedFolder = NULL;

gchar *file_selector (gboolean save, gchar *name, const gchar *filters[],
		      const gchar *title_fmt, ...)
{
    GtkWidget *dialog;
    va_list args;
    gchar *title;
    gchar *filename = NULL;

    /*
     * We need to turn off os_input and os_readchar here, because when using
     * #save and #restore the game is still running in the background, and
     * these functions do horrible things to the file selector dialog if
     * allowed to run. It's probably gtk_main that messes things up.
     */

    block_input ();

    va_start (args, title_fmt);
    title = g_strdup_vprintf (title_fmt, args);
    va_end (args);

    dialog = gtk_file_chooser_dialog_new (
	title, GTK_WINDOW (Gui.main_window),
	save ? GTK_FILE_CHOOSER_ACTION_SAVE : GTK_FILE_CHOOSER_ACTION_OPEN,
	GTK_STOCK_CANCEL, GTK_RESPONSE_CANCEL,
	save ? GTK_STOCK_SAVE : GTK_STOCK_OPEN, GTK_RESPONSE_ACCEPT,
	NULL);

    gtk_dialog_set_default_response (GTK_DIALOG (dialog), GTK_RESPONSE_ACCEPT);

    if (name)
    {
	if (save)
	    gtk_file_chooser_set_current_name (
		GTK_FILE_CHOOSER (dialog), name);
	else
	    gtk_file_chooser_set_filename (GTK_FILE_CHOOSER (dialog), name);
    } else if (savedFolder)
	gtk_file_chooser_set_current_folder (
	    GTK_FILE_CHOOSER (dialog), savedFolder);

    if (filters)
    {
	GtkFileFilter *filter;
	int i;

	for (i = 0; filters[i]; i += 2)
	{
	    filter = gtk_file_filter_new ();
	    gtk_file_filter_set_name (filter, filters[i]);
	    gtk_file_filter_add_pattern (filter, filters[i + 1]);
	    gtk_file_chooser_add_filter (GTK_FILE_CHOOSER (dialog), filter);
	}

	filter = gtk_file_filter_new ();
	gtk_file_filter_set_name (filter, "All files (*.*)");
	gtk_file_filter_add_pattern (filter, "*");
	gtk_file_chooser_add_filter (GTK_FILE_CHOOSER (dialog), filter);
    }

  try_again:

    if (gtk_dialog_run (GTK_DIALOG (dialog)) != GTK_RESPONSE_ACCEPT) {
	gtk_widget_destroy (dialog);
	unblock_input ();
	return NULL;
    }

    filename = gtk_file_chooser_get_filename (GTK_FILE_CHOOSER (dialog));

    if (save && g_file_test (filename, G_FILE_TEST_EXISTS))
    {
	GtkWidget *warning;
	gchar *basename;
	gint result;

	basename = g_path_get_basename (filename);
	warning = gtk_message_dialog_new (
	    GTK_WINDOW (dialog),
	    GTK_DIALOG_DESTROY_WITH_PARENT,
	    GTK_MESSAGE_WARNING,
	    GTK_BUTTONS_YES_NO,
	    "The file '%s' already exists.\nSave anyway?",
	    basename);
	result = gtk_dialog_run (GTK_DIALOG (warning));
	g_free (basename);
	gtk_widget_destroy (warning);

	if (result != GTK_RESPONSE_YES) {
	    g_free (filename);
	    goto try_again;
	}
    }

    g_free (savedFolder);
    savedFolder =
	gtk_file_chooser_get_current_folder (GTK_FILE_CHOOSER (dialog));

    gtk_widget_destroy (dialog);
    unblock_input ();

    return filename;
}

#ifdef MAKE_FILENAMES_ABSOLUTE
#define MAX_TOKENS 80

gchar *make_filename_absolute (gchar *filename)
{
    gchar *absolute_filename;
    gchar **tokens;
    gchar *result[MAX_TOKENS];
    gchar *name;
    gchar *new_filename = NULL;
    gchar *root = NULL;
    int i, j;

    /* We only want the actual path, so skip the root element */

    name = (gchar *) g_path_skip_root (filename);

    /*
     * If it's a relative path, we prefix it with the current working directory
     * and try again.
     */

    if (name == NULL)
    {
	gchar *current_dir;
	gchar *new_filename;

	current_dir = g_get_current_dir ();
	new_filename =
	    g_strdup_printf ("%s%c%s", current_dir, G_DIR_SEPARATOR, filename);
	g_free (current_dir);

	filename = new_filename;
	name = (gchar *) g_path_skip_root (filename);
    }

    /*
     * Extract the root element, minus the directory separator at the end. On
     * a Unix system that means the root will almost certainly be an empty
     * string, but on a Windows system it could be a drive letter or a share.
     */

    root = (gchar *) g_malloc (name - filename);
    strncpy (root, filename, name - filename - 1);
    root[name - filename - 1] = 0;

    /*
     * Split the name into its component parts and parse them. We assume that
     * "." and ".." are the only "special" parts that can appear in a filename.
     */

    tokens = g_strsplit (name, G_DIR_SEPARATOR_S, MAX_TOKENS);

    j = 0;

    for (i = 0; tokens[i]; i++)
    {
	if (strcmp (tokens[i], ".") == 0)
	    continue;

	if (strcmp (tokens[i], "..") == 0)
	{
	    if (j > 0)
		j--;

	    continue;
	}

	result[j] = tokens[i];
	j++;
    }

    /* Glue the pieces together again into the absolute filename */

    absolute_filename = (gchar *) g_malloc (MAX_PATH + 1);
    *absolute_filename = 0;

    if (root)
    {
	strcat (absolute_filename, root);
	g_free (root);
    }
	
    for (i = 0; i < j; i++)
    {
	strcat (absolute_filename, G_DIR_SEPARATOR_S);
	strcat (absolute_filename, result[i]);
    }

    g_free (new_filename);
    g_strfreev (tokens);

    return absolute_filename;
}
#endif
