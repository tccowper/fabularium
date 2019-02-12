/*
 * hints.c - Display external hints file
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
#include "config.h"
#include "gui.h"
#include "hints.h"

static GtkWidget *hintsWindow = NULL;

/*
 * This should be automagically called when the user changes the size of the
 * hints window.
 */

static gboolean configure_window (GtkWidget *widget, GdkEventConfigure *event,
				  gpointer user_data)
{
    Config.hints_width = event->width;
    Config.hints_height = event->height;
    return FALSE;
}

/* Recursively populate the hints tree */

static void fill_tree_store (GtkTreeStore *store, struct ms_hint *hints,
			     gint n, type8 *ptr, GtkTreeIter *parent)
{
    GtkTreeIter iter;
    gint i;

    if (hints[n].nodetype == 1)
    {
	/* This is a node. Populate and descend further */
	
	for (i = 0; i < hints[n].elcount; i++)
	{
	    gint link = hints[n].links[i];
	    
	    gtk_tree_store_append (store, &iter, parent);
	    gtk_tree_store_set (store, &iter, 0, ptr, -1);
	    fill_tree_store (store, hints, link, hints[link].content, &iter);
	    ptr += strlen((char *) ptr) + 1;
	}
    } else
    {
	/* This is a leaf */

	for (i = 0; i < hints[n].elcount; i++)
	{
	    gchar text[15];

	    gtk_tree_store_append (store, &iter, parent);
	    g_snprintf (text, sizeof (text), "Hint #%d", i + 1);
	    gtk_tree_store_set (store, &iter, 0, text, -1);
	    gtk_tree_store_append (store, &iter, &iter);
	    gtk_tree_store_set (store, &iter, 0, ptr, -1);
	    ptr += strlen((char *) ptr) + 1;
	}
    }
}

void hints_clear ()
{
    if (hintsWindow)
	gtk_widget_destroy (hintsWindow);
}

type8 ms_showhints (struct ms_hint *hints)
{
    GtkTreeStore *store;
    GtkCellRenderer *renderer;
    GtkTreeViewColumn *column;
    GtkWidget *scroll;
    GtkWidget *view;

    if (hintsWindow)
    {
	gtk_window_present (GTK_WINDOW (hintsWindow));
	return 1;
    }
    
    hintsWindow = gtk_window_new (GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title (GTK_WINDOW (hintsWindow), "GtkMagnetic");
    gtk_widget_set_size_request (
	hintsWindow, MIN_WINDOW_WIDTH, MIN_WINDOW_HEIGHT);
    gtk_window_set_default_size (
	GTK_WINDOW (hintsWindow), Config.hints_width, Config.hints_height);

    g_signal_connect (
	G_OBJECT (hintsWindow), "destroy",
	G_CALLBACK (gtk_widget_destroyed), &hintsWindow);
    g_signal_connect (
	G_OBJECT (hintsWindow), "configure-event",
	G_CALLBACK (configure_window), NULL);

    scroll = gtk_scrolled_window_new (NULL, NULL);
    gtk_scrolled_window_set_shadow_type (
	GTK_SCROLLED_WINDOW (scroll), GTK_SHADOW_ETCHED_IN);
    gtk_scrolled_window_set_policy (
	GTK_SCROLLED_WINDOW (scroll), GTK_POLICY_AUTOMATIC,
	GTK_POLICY_AUTOMATIC);

    gtk_container_add (GTK_CONTAINER (hintsWindow), scroll);
    
    store = gtk_tree_store_new (1, G_TYPE_STRING);
    fill_tree_store (store, hints, 0, hints[0].content, NULL);
    view = gtk_tree_view_new_with_model (GTK_TREE_MODEL (store));
    g_object_unref(store);
    
    renderer = gtk_cell_renderer_text_new ();
    column = gtk_tree_view_column_new_with_attributes (
	"Hints", renderer, "text", 0, NULL);
    gtk_tree_view_append_column (GTK_TREE_VIEW (view), column);

    gtk_container_add (GTK_CONTAINER (scroll), view);
    gtk_widget_show_all (GTK_WIDGET (hintsWindow));
    return 1;
}
