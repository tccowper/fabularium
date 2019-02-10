/*
 * config.c - Storing to and retrieving from the configuration file
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

#include <stdio.h>
#include <string.h>
#include <gtk/gtk.h>

#include "config.h"
#include "gui.h"
#include "text.h"
#include "graphics.h"

#define CONFIG_VERSION_MAJOR  1
#define CONFIG_VERSION_MINOR  1

/* ------------------------------------------------------------------------- *
 * The configuration data structure. There is no guarantee that all of these *
 * settings will be up-to-date at all times. In particular, I haven't found  *
 * any way of detecting when the window partition changes.                   *
 * ------------------------------------------------------------------------- */

Configuration Config =
{
    2 * MIN_WINDOW_WIDTH,   /* window width                         */
    2 * MIN_WINDOW_HEIGHT,  /* window height                        */
    150,                    /* partition                            */
    MIN_WINDOW_WIDTH,       /* hints window width                   */
    MIN_WINDOW_HEIGHT,      /* hints window height                  */
    NULL,                   /* text font                            */
    NULL,                   /* text foreground colour               */
    NULL,                   /* text background colour               */
    NULL,                   /* statusline font                      */
    NULL,                   /* statusline foreground colour         */
    NULL,                   /* statusline background colour         */
    FALSE,                  /* scale image to constant height?      */
    1.0,                    /* image scaling                        */
    300,                    /* constant image height                */
    GDK_INTERP_BILINEAR,    /* interpolation mode for image scaling */
    1.0,                    /* red gamma                            */
    1.0,                    /* green gamma                          */
    1.0,                    /* blue gamma                           */
    NULL,                   /* graphics background colour           */
    TRUE,                   /* animate images                       */
    100                     /* animation delay (ms)                 */
};

/* ------------------------------------------------------------------------- *
 * Utility functions                                                         *
 * ------------------------------------------------------------------------- */

static gchar *get_config_filename ()
{
    /*
     * GLIB 2.6 introduced a g_get_user_config_dir() function. Perhaps we
     * should use that instead, but I don't want want to write the migration
     * code for it.
     */
    return g_build_filename (g_get_home_dir (), ".gtkmagnetic", NULL);
}

/* ------------------------------------------------------------------------- *
 * Configuration file writer.                                                *
 * ------------------------------------------------------------------------- */

void write_config_file ()
{
    gchar *filename;
    GIOChannel *file;
    GError *error = NULL;

    /*
     * HACK: As mentioned before, I don't know how to detect when the window
     * partition changes, so this is the only opportunity to make sure the
     * setting is up to date.
     */
    Config.window_split = gtk_paned_get_position (GTK_PANED (Gui.partition));
    
    filename = get_config_filename ();
    file = g_io_channel_new_file (filename, "w", &error);
    g_free (filename);

    if (file)
    {
	gchar *buf;
	gsize bytes_written;

	buf = g_strdup_printf (
	    "<?xml version=\"1.0\"?>\n\n"
	    "<configuration version=\"%d.%d\">\n"
	    "  <layout>\n"
	    "    <main_window>\n"
	    "      <width>%d</width>\n"
	    "      <height>%d</height>\n"
	    "      <split>%d</split>\n"
	    "    </main_window>\n\n"
	    
	    "    <hints_window>\n"
	    "      <width>%d</width>\n"
	    "      <height>%d</height>\n"
	    "    </hints_window>\n"
	    "  </layout>\n\n"
	    
	    "  <text>\n"
	    "    <font>%s</font>\n"
	    "    <foreground>%s</foreground>\n"
	    "    <background>%s</background>\n"
	    "  </text>\n\n"
	    
	    "  <statusline>\n"
	    "    <font>%s</font>\n"
	    "    <foreground>%s</foreground>\n"
	    "    <background>%s</background>\n"
	    "  </statusline>\n\n"
	    
	    "  <graphics>\n"
	    "    <constant_height>%s</constant_height>\n"
	    "    <scale>%f</scale>\n"
	    "    <height>%d</height>\n"
	    "    <filter>%d</filter>\n"
	    "    <gamma>\n"
	    "      <red>%f</red>\n"
	    "      <green>%f</green>\n"
	    "      <blue>%f</blue>\n"
	    "    </gamma>\n"
	    "    <background>%s</background>\n"
	    "    <animate>%s</animate>\n"
	    "    <delay>%d</delay>\n"
	    "  </graphics>\n"
	    "</configuration>\n",
	    CONFIG_VERSION_MINOR,
	    CONFIG_VERSION_MAJOR,
	    Config.window_width,
	    Config.window_height,
	    Config.window_split,
	    Config.hints_width,
	    Config.hints_height,
	    Config.text_font ? Config.text_font : "",
	    Config.text_fg ? Config.text_fg : "",
	    Config.text_bg ? Config.text_bg : "",
	    Config.status_font ? Config.status_font : "",
	    Config.status_fg ? Config.status_fg : "",
	    Config.status_bg ? Config.status_bg : "",
	    Config.image_constant_height ? "TRUE" : "FALSE",
	    Config.image_scale,
	    Config.image_height,
	    Config.image_filter,
	    Config.red_gamma,
	    Config.green_gamma,
	    Config.blue_gamma,
	    Config.graphics_bg ? Config.graphics_bg : "",
	    Config.animate_images ? "TRUE" : "FALSE",
	    Config.animation_delay);

	g_io_channel_write_chars (file, buf, -1, &bytes_written, &error);
	g_free (buf);
	g_io_channel_unref (file);
    }
}

/* ------------------------------------------------------------------------- *
 * Configuration file reader/parser. The parser_state variable decides which *
 * tags are currently expected, and one of the parser* variables is set to   *
 * point at the location where the parsed value should be stored. Unknown    *
 * tags are silently ignored.                                                *
 *                                                                           *
 * This makes it very simple to add new tags or even to change the expected  *
 * layout of the configuration file. I'm quite pleased with the way this     *
 * code turned out.                                                          *
 * ------------------------------------------------------------------------- */

static enum
{
    CONFIG_PARSE_TOPLEVEL,
    CONFIG_PARSE_CONFIGURATION,
    CONFIG_PARSE_LAYOUT,
    CONFIG_PARSE_LAYOUT_MAIN_WINDOW,
    CONFIG_PARSE_LAYOUT_HINTS_WINDOW,
    CONFIG_PARSE_TEXT,
    CONFIG_PARSE_STATUSLINE,
    CONFIG_PARSE_GRAPHICS,
    CONFIG_PARSE_GRAPHICS_GAMMA
} parser_state = CONFIG_PARSE_TOPLEVEL;

static gboolean *parserBool = NULL;
static gint *parserInt = NULL;
static gdouble *parserFloat = NULL;
static gchar **parserChar = NULL;

static void config_parse_reset ()
{
    parserBool = NULL;
    parserInt = NULL;
    parserFloat = NULL;
    parserChar = NULL;
}

/* Parse opening tag and attributes */

static void config_parse_start_element (GMarkupParseContext *context,
					const gchar *element_name,
					const gchar **attribute_names,
					const gchar **attribute_values,
					gpointer user_data,
					GError **error)
{
    config_parse_reset ();
    
    switch (parser_state)
    {
	case CONFIG_PARSE_TOPLEVEL:
	    if (strcmp (element_name, "configuration") == 0)
	    {
		gint version_major = 0;
		gint version_minor = 0;
		gint i;
		
		parser_state = CONFIG_PARSE_CONFIGURATION;

		for (i = 0; attribute_names[i]; i++)
		{
		    if (strcmp (attribute_names[i], "version") == 0)
			sscanf (attribute_values[i], "%d.%d", &version_major,
				&version_minor);
		}

		if (version_major != CONFIG_VERSION_MAJOR ||
		    version_minor != CONFIG_VERSION_MINOR)
		{
		    g_message (
			"Config version mismatch. Expected %d.%d, found %d.%d",
			CONFIG_VERSION_MAJOR, CONFIG_VERSION_MINOR,
			version_major, version_minor);
		    if (version_major == CONFIG_VERSION_MAJOR &&
			version_minor <= CONFIG_VERSION_MINOR)
			g_message ("This should be quite harmless.");
		    else
			g_message (
			    "Some of your settings have probably been lost.");
		}
	    }
	    break;
	    
	case CONFIG_PARSE_CONFIGURATION:
	    if (strcmp (element_name, "layout") == 0)
		parser_state = CONFIG_PARSE_LAYOUT;
	    else if (strcmp (element_name, "text") == 0)
		parser_state = CONFIG_PARSE_TEXT;
	    else if (strcmp (element_name, "statusline") == 0)
		parser_state = CONFIG_PARSE_STATUSLINE;
	    else if (strcmp (element_name, "graphics") == 0)
		parser_state = CONFIG_PARSE_GRAPHICS;
	    break;

	case CONFIG_PARSE_LAYOUT:
	    if (strcmp (element_name, "main_window") == 0)
		parser_state = CONFIG_PARSE_LAYOUT_MAIN_WINDOW;
	    else if (strcmp (element_name, "hints_window") == 0)
		parser_state = CONFIG_PARSE_LAYOUT_HINTS_WINDOW;
	    break;

	case CONFIG_PARSE_LAYOUT_MAIN_WINDOW:
	    if (strcmp (element_name, "width") == 0)
		parserInt = &(Config.window_width);
	    else if (strcmp (element_name, "height") == 0)
		parserInt = &(Config.window_height);
	    else if (strcmp (element_name, "split") == 0)
		parserInt = &(Config.window_split);
	    break;

	case CONFIG_PARSE_LAYOUT_HINTS_WINDOW:
	    if (strcmp (element_name, "width") == 0)
		parserInt = &(Config.hints_width);
	    else if (strcmp (element_name, "height") == 0)
		parserInt = &(Config.hints_height);
	    break;

	case CONFIG_PARSE_TEXT:
	    if (strcmp (element_name, "font") == 0)
		parserChar = &(Config.text_font);
	    else if (strcmp (element_name, "background") == 0)
		parserChar = &(Config.text_bg);
	    else if (strcmp (element_name, "foreground") == 0)
		parserChar = &(Config.text_fg);
	    break;

	case CONFIG_PARSE_STATUSLINE:
	    if (strcmp (element_name, "font") == 0)
		parserChar = &(Config.status_font);
	    if (strcmp (element_name, "foreground") == 0)
		parserChar = &(Config.status_fg);
	    else if (strcmp (element_name, "background") == 0)
		parserChar = &(Config.status_bg);
	    break;
	    
	case CONFIG_PARSE_GRAPHICS:
	    if (strcmp (element_name, "constant_height") == 0)
		parserBool = &(Config.image_constant_height);
	    else if (strcmp (element_name, "scale") == 0)
		parserFloat = &(Config.image_scale);
	    else if (strcmp (element_name, "height") == 0)
		parserInt = &(Config.image_height);
	    else if (strcmp (element_name, "filter") == 0)
		parserInt = &(Config.image_filter);
	    else if (strcmp (element_name, "gamma") == 0)
		parser_state = CONFIG_PARSE_GRAPHICS_GAMMA;
	    else if (strcmp (element_name, "background") == 0)
		parserChar = &(Config.graphics_bg);
	    else if (strcmp (element_name, "animate") == 0)
		parserBool = &(Config.animate_images);
	    else if (strcmp (element_name, "delay") == 0)
		parserInt = &(Config.animation_delay);
	    break;

	case CONFIG_PARSE_GRAPHICS_GAMMA:
	    if (strcmp (element_name, "red") == 0)
		parserFloat = &(Config.red_gamma);
	    else if (strcmp (element_name, "green") == 0)
		parserFloat = &(Config.green_gamma);
	    else if (strcmp (element_name, "blue") == 0)
		parserFloat = &(Config.blue_gamma);
	    break;

	default:
	    break;
    }
}

/* Parse closing tag */

static void config_parse_end_element (GMarkupParseContext *context,
				      const gchar *element_name,
				      gpointer user_data,
				      GError **error)
{
    config_parse_reset ();
    
    switch (parser_state)
    {
	case CONFIG_PARSE_CONFIGURATION:
	    if (strcmp (element_name, "configuration") == 0)
		parser_state = CONFIG_PARSE_TOPLEVEL;
	    break;

	case CONFIG_PARSE_LAYOUT:
	    if (strcmp (element_name, "layout") == 0)
		parser_state = CONFIG_PARSE_CONFIGURATION;
	    break;

	case CONFIG_PARSE_LAYOUT_MAIN_WINDOW:
	    if (strcmp (element_name, "main_window") == 0)
		parser_state = CONFIG_PARSE_LAYOUT;
	    break;

	case CONFIG_PARSE_LAYOUT_HINTS_WINDOW:
	    if (strcmp (element_name, "hints_window") == 0)
		parser_state = CONFIG_PARSE_LAYOUT;
	    break;
	    
	case CONFIG_PARSE_TEXT:
	    if (strcmp (element_name, "text") == 0)
		parser_state = CONFIG_PARSE_CONFIGURATION;
	    break;
	    
	case CONFIG_PARSE_GRAPHICS:
	    if (strcmp (element_name, "graphics") == 0)
		parser_state = CONFIG_PARSE_CONFIGURATION;
	    break;

	case CONFIG_PARSE_GRAPHICS_GAMMA:
	    if (strcmp (element_name, "gamma") == 0)
		parser_state = CONFIG_PARSE_GRAPHICS;
	    break;

	case CONFIG_PARSE_STATUSLINE:
	    if (strcmp (element_name, "statusline") == 0)
		parser_state = CONFIG_PARSE_CONFIGURATION;
	    break;

	default:
	    break;
    }
}

/* Parse text between tags */

static void config_parse_text (GMarkupParseContext *context,
			       const gchar *text,
			       gsize text_len,
			       gpointer user_data,
			       GError **error)
{
    if (parserBool)
	*parserBool = (strcmp (text, "TRUE") == 0) ? TRUE : FALSE;
    else if (parserInt)
	*parserInt = (gint) g_ascii_strtod (text, NULL);
    else if (parserFloat)
	*parserFloat = g_ascii_strtod (text, NULL);
    else if (parserChar)
    {
	if (*parserChar)
	    g_free (*parserChar);
	if (strlen (text) > 0)
	    *parserChar = g_strdup (text);
    }
}

/*
 * Error handling. We really ought to do something more useful here, but it
 * shouldn't ever happen...
 */

static void config_parse_error (GMarkupParseContext *context,
				GError *error,
				gpointer user_data)
{
    g_warning ("Config file parser error: %s", error->message);
}

void read_config_file ()
{
    GMarkupParseContext *context;
    GMarkupParser parser =
	{
	    config_parse_start_element,
	    config_parse_end_element,
	    config_parse_text,
	    NULL,
	    config_parse_error
	};
    gchar *filename;
    gchar *text;
    gsize length;
    GError *error = NULL;

    filename = get_config_filename ();
    g_file_get_contents (filename, &text, &length, &error);
    g_free (filename);

    if (text)
    {
	context = g_markup_parse_context_new (&parser, 0, NULL, NULL);
	g_markup_parse_context_parse (context, text, -1, &error);
	g_markup_parse_context_end_parse (context, &error);
	g_markup_parse_context_free (context);

	if (parser_state != CONFIG_PARSE_TOPLEVEL)
	    g_warning ("Unexpected parser state at end of config file");
    }

    /* Apply the settings from the configuration file */

    text_refresh ();
    graphics_refresh ();
    gui_refresh ();
}

/* ------------------------------------------------------------------------- *
 * Configuration window.                                                     *
 * ------------------------------------------------------------------------- */

static void toggle_sensitivity (GtkToggleButton *toggle_button,
				gpointer user_data)
{
    if (gtk_toggle_button_get_active (toggle_button))
	gtk_widget_set_sensitive (GTK_WIDGET (user_data), TRUE);
    else
	gtk_widget_set_sensitive (GTK_WIDGET (user_data), FALSE);
}

static GtkWidget *add_font_button (GtkWidget *tab, gchar *text, gchar *title)
{
    GtkWidget *label;
    GtkWidget *font_button;

    label = gtk_label_new (text);
    gtk_misc_set_alignment (GTK_MISC (label), 0.0, 0.5);
    gtk_box_pack_start (GTK_BOX (tab), label, TRUE, TRUE, 0);

    font_button = gtk_font_button_new ();
    gtk_font_button_set_use_font (GTK_FONT_BUTTON (font_button), TRUE);
    gtk_font_button_set_use_size (GTK_FONT_BUTTON (font_button), TRUE);
    gtk_font_button_set_title (GTK_FONT_BUTTON (font_button), title);
    gtk_box_pack_start (GTK_BOX (tab), font_button, TRUE, TRUE, 0);

    return font_button;
}

static GtkWidget *add_scale (GtkWidget *tab, gchar *text, gdouble min,
			     gdouble max, gdouble step, gdouble value)
{
    GtkWidget *scale;
    GtkWidget *label;

    label = gtk_label_new (text);
    gtk_misc_set_alignment (GTK_MISC (label), 0.0, 0.5);
    gtk_box_pack_start (GTK_BOX (tab), label, TRUE, TRUE, 0);
    
    scale = gtk_hscale_new_with_range (min, max, step);
    gtk_scale_set_digits (GTK_SCALE (scale), 2);
    gtk_scale_set_value_pos (GTK_SCALE (scale), GTK_POS_RIGHT);
    gtk_range_set_value (GTK_RANGE (scale), value);
    gtk_box_pack_start (GTK_BOX (tab), scale, TRUE, TRUE, 0);

    return scale;
}

typedef struct
{
    GtkWidget *checkbox;
    GtkWidget *button;
} ColourSetting;

static void update_colour_setting (gchar **colour, ColourSetting *s)
{
    g_free (*colour);

    if (gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (s->checkbox)))
    {
	GdkColor c;

	gtk_color_button_get_color (GTK_COLOR_BUTTON (s->button), &c);
	*colour = g_strdup_printf ("#%02X%02X%02X",
				   c.red / 256,
				   c.green / 256,
				   c.blue / 256);
    } else
	*colour = NULL;
}

static ColourSetting *add_colour_setting (GtkWidget *tab, gchar *text,
					  gchar *title, gchar *colour_name)
{
    ColourSetting *s;
    GdkColor colour;

    s = g_new (ColourSetting, 1);

    s->checkbox = gtk_check_button_new_with_label (text);
    gtk_box_pack_start (GTK_BOX (tab), s->checkbox, TRUE, TRUE, 0);

    s->button = gtk_color_button_new ();
    gtk_box_pack_start (GTK_BOX (tab), s->button, TRUE, TRUE, 0);
    gtk_color_button_set_title (GTK_COLOR_BUTTON (s->button), title);

    if (colour_name && gdk_color_parse (colour_name, &colour))
    {
	gtk_toggle_button_set_active (GTK_TOGGLE_BUTTON (s->checkbox), TRUE);
	gtk_color_button_set_color (GTK_COLOR_BUTTON (s->button), &colour);
    } else
	gtk_widget_set_sensitive (GTK_WIDGET (s->button), FALSE);

    g_signal_connect (
	G_OBJECT (s->checkbox), "toggled", G_CALLBACK (toggle_sensitivity),
	s->button);
    return s;
}

static gulong hSigScaleChanged = 0;

static GtkWidget *imageScaleLabel;
static GtkWidget *imageScale;

static gint tmpImageHeight;
static gfloat tmpImageScale;

static void toggle_constant_height (GtkToggleButton *togglebutton,
				    gpointer user_data)
{
    /*
     * Apparently changing the image scale the way we do below will cause
     * the "value_changed" signal to be emitted, which will screw things up
     * quite badly. So we block that signal temporarily.
     */
    
    g_signal_handler_block (G_OBJECT (imageScale), hSigScaleChanged);
    
    if (gtk_toggle_button_get_active (togglebutton))
    {
	gtk_label_set_text (GTK_LABEL (imageScaleLabel), "Image height:");
	gtk_scale_set_digits (GTK_SCALE (imageScale), 0);
	gtk_range_set_range (GTK_RANGE (imageScale), 50, 1000);
	gtk_range_set_increments (GTK_RANGE (imageScale), 1.0, 50.0);
	gtk_range_set_value (GTK_RANGE (imageScale), tmpImageHeight);
    } else
    {
	gtk_label_set_text (GTK_LABEL (imageScaleLabel), "Scale factor:");
	gtk_scale_set_digits (GTK_SCALE (imageScale), 2);
	gtk_range_set_range (GTK_RANGE (imageScale), 0.1, 5.0);
	gtk_range_set_increments (GTK_RANGE (imageScale), 0.01, 0.1);
	gtk_range_set_value (GTK_RANGE (imageScale), tmpImageScale);
    }

    g_signal_handler_unblock (G_OBJECT (imageScale), hSigScaleChanged);
}

static void change_image_scale (GtkRange *range, gpointer user_data)
{
    if (gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (user_data)))
	tmpImageHeight = (gint) gtk_range_get_value (range);
    else
	tmpImageScale = gtk_range_get_value (range);
}

typedef struct
{
    const gchar *description;
    GdkInterpType interp_type;
} ComboBoxItem;

static const ComboBoxItem imageFilters[] = {
    /*
     * In reality, the value and index into this array are probably the same.
     * But I don't want to make that assumption.
     */
    { "Nearest neighbor", GDK_INTERP_NEAREST  },
    { "Tiles",            GDK_INTERP_TILES    },
    { "Bilinear",         GDK_INTERP_BILINEAR },
    { "Hyperbolic",       GDK_INTERP_HYPER    }
};

static int get_interp_type_index (GdkInterpType interp_type)
{
    int i;

    for (i = 0; i < G_N_ELEMENTS (imageFilters); i++)
	if (imageFilters[i].interp_type == interp_type)
	    return i;

    return -1;
}

void do_config ()
{
    GtkWidget *dialog;
    GtkWidget *dummy;
    GtkWidget *tabs;
    GtkWidget *graphics_tab;
    ColourSetting *text_fg;
    ColourSetting *text_bg;
    ColourSetting *status_fg;
    ColourSetting *status_bg;
    ColourSetting *graphics_bg;
    GtkWidget *colour_tab;
    GtkWidget *text_font;
    GtkWidget *status_font;
    GtkWidget *constant_height;
    GtkWidget *red_gamma;
    GtkWidget *green_gamma;
    GtkWidget *blue_gamma;
    GtkWidget *animate_images;
    GtkWidget *animation_delay;
    GtkWidget *image_filter;
    gint filter_idx = 0;
    gint i;

    /*
     * Some settings, such as the partition, may have changed since they were
     * last restored. Save them now so the config dialog won't accidentally
     * restore the old values.
     */
    write_config_file ();
    
    dialog = gtk_dialog_new_with_buttons (
	"Preferences",
	GTK_WINDOW (Gui.main_window),
	GTK_DIALOG_MODAL | GTK_DIALOG_DESTROY_WITH_PARENT,
	GTK_STOCK_OK,
	GTK_RESPONSE_ACCEPT,
	GTK_STOCK_CANCEL,
	GTK_RESPONSE_REJECT,
	NULL);

    gtk_window_set_default_size (GTK_WINDOW (dialog), 400, 400);

    /* Top level */

    tabs = gtk_notebook_new ();
    gtk_box_pack_start (
	GTK_BOX (GTK_DIALOG (dialog)->vbox), tabs, TRUE, TRUE, 0);

    colour_tab = gtk_vbox_new (FALSE, 3);
    gtk_container_set_border_width (GTK_CONTAINER (colour_tab), 5);
    gtk_notebook_append_page (GTK_NOTEBOOK (tabs), colour_tab,
			      gtk_label_new ("Text and Colour"));

    graphics_tab = gtk_vbox_new (FALSE, 1);
    gtk_container_set_border_width (GTK_CONTAINER (graphics_tab), 5);
    gtk_notebook_append_page (GTK_NOTEBOOK (tabs), graphics_tab,
			      gtk_label_new ("Graphics"));

    /* Text and colour settings */

    text_font = add_font_button (colour_tab, "Text font:", "Select text font");
    status_font =
	add_font_button (colour_tab, "Status font:", "Select status font");

    if (!Config.text_font)
    {
	GtkStyle *style;
	gchar *font_name;

	style = gtk_widget_get_style (Gui.text_view);
	font_name = pango_font_description_to_string (style->font_desc);
	gtk_font_button_set_font_name (
	    GTK_FONT_BUTTON (text_font), font_name);
	g_free (font_name);
    } else {
	gtk_font_button_set_font_name (
	    GTK_FONT_BUTTON (text_font), Config.text_font);
    }

    if (!Config.status_font)
	gtk_font_button_set_font_name (
	    GTK_FONT_BUTTON (status_font),
	    gtk_font_button_get_font_name (GTK_FONT_BUTTON (text_font)));
    else
	gtk_font_button_set_font_name (
	    GTK_FONT_BUTTON (status_font), Config.status_font);
    
    text_fg = add_colour_setting (
	colour_tab, "Override text foreground colour",
	"Select text coreground colour", Config.text_fg);
    text_bg = add_colour_setting (
	colour_tab, "Override text background colour",
	"Select text background colour", Config.text_bg);
    status_fg = add_colour_setting (
	colour_tab, "Override statusline foreground colour",
	"Select statusline foreground colour", Config.status_fg);
    status_bg = add_colour_setting (
	colour_tab, "Override statusline backgorund colour",
	"Select statusline background colour", Config.status_bg);
    graphics_bg = add_colour_setting (
	colour_tab, "Override picture background colour",
	"Select picture background colour", Config.graphics_bg);
 
    /* Picture settings */

    constant_height = gtk_check_button_new_with_label (
	"Scale image to constant height");
    gtk_toggle_button_set_active (
	GTK_TOGGLE_BUTTON (constant_height), Config.image_constant_height);
    gtk_box_pack_start (
	GTK_BOX (graphics_tab), constant_height, TRUE, TRUE, 0);

    g_signal_connect (
	G_OBJECT (constant_height), "toggled",
	G_CALLBACK (toggle_constant_height), NULL);

    imageScaleLabel = gtk_label_new (NULL);
    gtk_misc_set_alignment (GTK_MISC (imageScaleLabel), 0.0, 0.5);
    gtk_box_pack_start (
	GTK_BOX (graphics_tab), imageScaleLabel, TRUE, TRUE, 0);

    imageScale = gtk_hscale_new (NULL);
    gtk_scale_set_value_pos (GTK_SCALE (imageScale), GTK_POS_RIGHT);
    gtk_box_pack_start (GTK_BOX (graphics_tab), imageScale, TRUE, TRUE, 0);

    hSigScaleChanged = g_signal_connect (
	G_OBJECT (imageScale), "value-changed",
	G_CALLBACK (change_image_scale), constant_height);
    
    tmpImageScale = Config.image_scale;
    tmpImageHeight = Config.image_height;
    
    toggle_constant_height (GTK_TOGGLE_BUTTON (constant_height), NULL);

    dummy = gtk_label_new ("Interpolation mode:");
    gtk_misc_set_alignment (GTK_MISC (dummy), 0.0, 0.5);
    gtk_box_pack_start (GTK_BOX (graphics_tab), dummy, TRUE, TRUE, 0);

    image_filter = gtk_combo_box_new_text ();
    gtk_box_pack_start (GTK_BOX (graphics_tab), image_filter, TRUE, TRUE, 0);

    for (i = 0; i < G_N_ELEMENTS (imageFilters); i++)
    {
	gtk_combo_box_append_text (
	    GTK_COMBO_BOX (image_filter), imageFilters[i].description);
    }

    gtk_combo_box_set_active (
	GTK_COMBO_BOX (image_filter),
	get_interp_type_index (Config.image_filter));

    dummy = gtk_hseparator_new ();
    gtk_box_pack_start (GTK_BOX (graphics_tab), dummy, TRUE, TRUE, 8);

    red_gamma = add_scale (
	graphics_tab, "Red gamma:", 0.1, 5.0, 0.1, Config.red_gamma);
    green_gamma = add_scale (
	graphics_tab, "Green gamma:", 0.1, 5.0, 0.1, Config.green_gamma);
    blue_gamma = add_scale (
	graphics_tab, "Blue gamma:", 0.1, 5.0, 0.1, Config.blue_gamma);

    dummy = gtk_hseparator_new ();
    gtk_box_pack_start (GTK_BOX (graphics_tab), dummy, TRUE, TRUE, 8);

    animate_images = gtk_check_button_new_with_label ("Animate images");
    gtk_box_pack_start (GTK_BOX (graphics_tab), animate_images, TRUE, TRUE, 0);

    animation_delay = add_scale (
	graphics_tab, "Animation delay (ms):", 50.0, 500.0, 10.0,
	Config.animation_delay);
    gtk_scale_set_digits (GTK_SCALE (animation_delay), 0);
    
    gtk_toggle_button_set_active (
	GTK_TOGGLE_BUTTON (animate_images), Config.animate_images);
    gtk_widget_set_sensitive (
	GTK_WIDGET (animation_delay), Config.animate_images);

    g_signal_connect (
	G_OBJECT (animate_images), "toggled", G_CALLBACK (toggle_sensitivity),
	animation_delay);
    
    /* Run the dialog */

    gtk_widget_show_all (GTK_WIDGET (GTK_DIALOG (dialog)->vbox));

    if (gtk_dialog_run (GTK_DIALOG (dialog)) == GTK_RESPONSE_ACCEPT)
    {
	g_free (Config.text_font);
	Config.text_font = g_strdup (
	    gtk_font_button_get_font_name (GTK_FONT_BUTTON (text_font)));

	g_free (Config.status_font);
	Config.status_font = g_strdup (
	    gtk_font_button_get_font_name (GTK_FONT_BUTTON (status_font)));

	update_colour_setting (&(Config.text_fg), text_fg);
	update_colour_setting (&(Config.text_bg), text_bg);
	update_colour_setting (&(Config.status_fg), status_fg);
	update_colour_setting (&(Config.status_bg), status_bg);
	update_colour_setting (&(Config.graphics_bg), graphics_bg);

	Config.image_constant_height =
	    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (constant_height));
	Config.image_scale = tmpImageScale;
	Config.image_height = tmpImageHeight;
	Config.red_gamma = gtk_range_get_value (GTK_RANGE (red_gamma));
	Config.green_gamma = gtk_range_get_value (GTK_RANGE (green_gamma));
	Config.blue_gamma = gtk_range_get_value (GTK_RANGE (blue_gamma));

	Config.animate_images =
	    gtk_toggle_button_get_active (GTK_TOGGLE_BUTTON (animate_images));
	Config.animation_delay =
	    (gint) gtk_range_get_value (GTK_RANGE (animation_delay));

 	filter_idx = gtk_combo_box_get_active (GTK_COMBO_BOX (image_filter));
	if (filter_idx != -1)
	    Config.image_filter = imageFilters[filter_idx].interp_type;
	else
	    Config.image_filter = GDK_INTERP_BILINEAR;
	
	write_config_file ();

	/* Apply settings */

	text_refresh ();
	graphics_refresh ();
	gui_refresh ();
    }
    
    gtk_widget_destroy (dialog);
    g_free (text_fg);
    g_free (text_bg);
    g_free (status_fg);
    g_free (status_bg);
    g_free (graphics_bg);
}

