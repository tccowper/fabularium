/*
 * config.h - Storing to and retrieving from the configuration file
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

#ifndef _CONFIG_H
#define _CONFIG_H

/*
 * Note that this is the number of lines in the text buffer, not the text view.
 * Since this distinction is probably not obvious to the average user, I have
 * not made it configurable. Instead, let's just make it fairly large.
 */
#define MAX_SCROLLBACK 500

/*
 * This is stupidly small, but on the off-chance that some user wants to be
 * able to play on a 320x240 screen...
 */

#define MIN_WINDOW_WIDTH 300
#define MIN_WINDOW_HEIGHT 200

/*
 * It's probably more efficient to run a couple of instruction every time the
 * application is idle. This defines how many.
 */

#define MAX_INSTRUCTIONS 20

/*
 * We insert "dummy" spaces to make sure that the editable region is never
 * empty, otherwise GTK+ will remove it completely. To keep this space from
 * showing, we can use the "invisible" attribute.
 *
 * GTK+ 2.6.2 still complains that this attribute is not yet supported, but it
 * appears to work well enough for this purpose. If it doesn't work for you,
 * or the warning message bothers you, you can disable the use of it here.
 */

#define USE_INVISIBLE_TEXT

/*
 * GTK+ 2.6.8 still doesn't have any sensible way of changing the cursor
 * colour, and GtkTextView will use black by default no matter what the current
 * background colour is. Define this to use an ugly hack which seems to work.
 */

#define USE_CURSOR_COLOUR_HACK

typedef struct
{
    gint window_width;
    gint window_height;
    gint window_split;

    gint hints_width;
    gint hints_height;
    
    gchar *text_font;
    gchar *text_fg;
    gchar *text_bg;
    gchar *status_font;
    gchar *status_fg;
    gchar *status_bg;
    
    gboolean image_constant_height;
    gdouble image_scale;
    gint image_height;
    gint image_filter;     /* Should really be GdkInterpType, not gint */
    gdouble red_gamma;
    gdouble green_gamma;
    gdouble blue_gamma;
    gchar *graphics_bg;
    gboolean animate_images;
    gint animation_delay;
} Configuration;

extern Configuration Config;

void read_config_file (void);
void write_config_file (void);
void do_config (void);

#endif
