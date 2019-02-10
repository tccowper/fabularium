/*
 * gui.c - User interface
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

#include <gtk/gtk.h>

#include "main.h"
#include "config.h"
#include "gui.h"
#include "text.h"

GuiWidgets Gui;

/*
 * This should be automagically called every time the user changes the size
 * of the game window.
 */

static gboolean configure_window (GtkWidget *widget, GdkEventConfigure *event,
				  gpointer user_data)
{
    Config.window_width = event->width;
    Config.window_height = event->height;
    return FALSE;
}


void gui_refresh ()
{
    gtk_window_set_default_size (
	GTK_WINDOW (Gui.main_window), Config.window_width,
	Config.window_height);
    gtk_paned_set_position (
	GTK_PANED (Gui.partition), Config.window_split);
    gtk_widget_grab_focus (Gui.text_view);
}

static void do_open ()
{
    start_new_game (NULL, NULL);
}

static void do_quit ()
{
    close_application (NULL, NULL);
}

static GtkActionEntry menuEntries[] =
{
    { "FileMenu", NULL, "_File" },
    { "Open", GTK_STOCK_OPEN, "_Open", "<control>O", NULL,
      G_CALLBACK (do_open) },
    { "Prefs", GTK_STOCK_PREFERENCES, "_Preferences...", NULL, NULL,
      G_CALLBACK (do_config) },
    { "Quit", GTK_STOCK_QUIT, "_Quit", "<control>Q", NULL,
      G_CALLBACK (do_quit) },
    { "HelpMenu", NULL, "_Help" },
    { "About", GTK_STOCK_ABOUT, "_About", NULL, NULL,
      G_CALLBACK (do_about) }
};
    
static const char *uiDescr =
"<ui>"
"  <menubar name='MenuBar'>"
"    <menu action='FileMenu'>"
"      <menuitem name='Open...' action='Open'/>"
"      <menuitem name='Preferences...' action='Prefs'/>"
"      <separator/>"
"      <menuitem name='Quit' action='Quit'/>"
"    </menu>"
"    <menu action='HelpMenu'>"
"      <menuitem name='About' action='About'/>"
"    </menu>"
"  </menubar>"
"</ui>";

void gui_init ()
{
    GtkUIManager *ui_manager;
    GtkActionGroup *action_group;
    GtkAccelGroup *accel_group;
    GError *error;

    GtkWidget *text_scroll;

    Gui.main_window = gtk_window_new (GTK_WINDOW_TOPLEVEL);
    gtk_window_set_title (GTK_WINDOW (Gui.main_window), "GtkLevel9");
    gtk_widget_set_size_request (Gui.main_window, MIN_WINDOW_WIDTH,
				 MIN_WINDOW_HEIGHT);

    g_signal_connect (G_OBJECT (Gui.main_window), "destroy",
		      G_CALLBACK (close_application), NULL);
    g_signal_connect (G_OBJECT (Gui.main_window), "configure-event",
		      G_CALLBACK (configure_window), NULL);

    /* The main "box" */

    Gui.main_box = gtk_vbox_new (FALSE, 0);
    gtk_container_add (GTK_CONTAINER (Gui.main_window), Gui.main_box);

    /* Menus */
    action_group = gtk_action_group_new ("MenuActions");

    gtk_action_group_add_actions (
	action_group, menuEntries, G_N_ELEMENTS (menuEntries),
	Gui.main_window);

    ui_manager = gtk_ui_manager_new ();

    gtk_ui_manager_set_add_tearoffs (ui_manager, TRUE);

    accel_group = gtk_ui_manager_get_accel_group (ui_manager);
    gtk_window_add_accel_group (GTK_WINDOW (Gui.main_window), accel_group);
    gtk_ui_manager_insert_action_group (ui_manager, action_group, 0);

    error = NULL;
    if (!gtk_ui_manager_add_ui_from_string (ui_manager, uiDescr, -1, &error))
    {
	/* This is bad, but not catastrophic. Keep running. */
	g_message ("Building menus failed: %s", error->message);
	g_error_free (error);
    }

    gtk_box_pack_start (
	GTK_BOX (Gui.main_box),
	gtk_ui_manager_get_widget (ui_manager, "/MenuBar"),
	FALSE, FALSE, 0);

    /* The game area; picture and text */
    
    Gui.partition = gtk_vpaned_new ();
    gtk_box_pack_start (GTK_BOX (Gui.main_box), Gui.partition, TRUE, TRUE, 0);

    Gui.statusbar = gtk_statusbar_new ();
    gtk_statusbar_set_has_resize_grip (GTK_STATUSBAR (Gui.statusbar), FALSE);
    gtk_box_pack_end (GTK_BOX (Gui.main_box), Gui.statusbar, FALSE, FALSE, 0);

    Gui.picture_area = gtk_scrolled_window_new (NULL, NULL);
    gtk_scrolled_window_set_policy (
	GTK_SCROLLED_WINDOW (Gui.picture_area), GTK_POLICY_AUTOMATIC,
	GTK_POLICY_AUTOMATIC);
    gtk_paned_add1 (GTK_PANED (Gui.partition), Gui.picture_area);

    text_scroll = gtk_scrolled_window_new (NULL, NULL);
    gtk_scrolled_window_set_policy (
	GTK_SCROLLED_WINDOW (text_scroll), GTK_POLICY_NEVER,
	GTK_POLICY_AUTOMATIC);
    gtk_scrolled_window_set_shadow_type (
	GTK_SCROLLED_WINDOW (text_scroll), GTK_SHADOW_IN);
    gtk_paned_add2 (GTK_PANED (Gui.partition), text_scroll);

    Gui.picture = gtk_image_new ();
    gtk_scrolled_window_add_with_viewport (
	GTK_SCROLLED_WINDOW (Gui.picture_area), Gui.picture);

    Gui.text_buffer = gtk_text_buffer_new (NULL);

    gtk_text_buffer_create_tag (
	Gui.text_buffer, "level9-input", "weight", PANGO_WEIGHT_BOLD,
	"editable", TRUE, NULL);
    gtk_text_buffer_create_tag (
	Gui.text_buffer, "level9-old-input", "weight", PANGO_WEIGHT_BOLD,
	"editable", FALSE, NULL);
    gtk_text_buffer_create_tag (
	Gui.text_buffer, "level9-input-padding",
#ifdef USE_INVISIBLE_TEXT
	"invisible", TRUE,
#endif
	"weight", PANGO_WEIGHT_BOLD, "editable", TRUE, NULL);

    Gui.text_view = gtk_text_view_new_with_buffer (Gui.text_buffer);
    gtk_text_view_set_wrap_mode (GTK_TEXT_VIEW (Gui.text_view), GTK_WRAP_WORD);
    gtk_text_view_set_left_margin (GTK_TEXT_VIEW (Gui.text_view), 3);
    gtk_text_view_set_right_margin (GTK_TEXT_VIEW (Gui.text_view), 3);
    gtk_text_view_set_editable (GTK_TEXT_VIEW (Gui.text_view), FALSE);
    gtk_text_view_set_cursor_visible (GTK_TEXT_VIEW (Gui.text_view), TRUE);
    gtk_container_add (GTK_CONTAINER (text_scroll), Gui.text_view);
}
