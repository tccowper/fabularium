/*
 * util.c - Utility functions
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
#include <gtk/gtk.h>

#include "gui.h"
#include "util.h"

static gchar *savedFolder = NULL;

gchar *file_selector (gboolean save, gchar *name, const gchar *filters[],
		      const gchar *title_fmt, ...)
{
    GtkWidget *dialog;
    va_list args;
    gchar *title;
    gchar *filename = NULL;

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
    return filename;
}
