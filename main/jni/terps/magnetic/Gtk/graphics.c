/*
 * graphics.c - Pictures and animations
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
#include <math.h>
#include <gtk/gtk.h>

#include "defs.h"
#include "main.h"
#include "config.h"
#include "gui.h"
#include "graphics.h"
#include "sound.h"

/* ------------------------------------------------------------------------- *
 * Utility functions.                                                        *
 * ------------------------------------------------------------------------- */

struct rgb
{
    gint red;
    gint green;
    gint blue;
};

/*
 * Apply gamma-correction to a colour component. There are apparently several
 * ways of doing this, but the output from ImageMagick's algorithm looks much
 * better to me than the output from Windows Magnetic's algorithm. At least
 * when compensating for the rather dark pictures in Corruption.
 */

static unsigned char apply_gamma (unsigned char colour, double gamma)
{
#if 1
    /* Gamma correction, the way ImageMagick does it. */
    return (unsigned char)
	CLAMP (pow ((double) colour / 255.0, 1.0 / gamma) * 255.0, 0.0, 255.0);
#else
    /* Gamma correction, the way Windows Magnetic does it. */
    return (unsigned char)
	CLAMP (sqrt ((double) colour * (double) colour * gamma), 0.0, 255.0);
#endif
}

/*
 * The *_colour256[] arrays will provide mappings from the original palette
 * (three bits per colour), to the real palette (eight bits per colour). We
 * use three mappings so that we aren't restricted to using the same gamma
 * for all three components.
 */

static const guchar colour256[] = { 0, 36, 73, 109, 146, 182, 219, 255 };

static guchar r_colour256[8];
static guchar g_colour256[8];
static guchar b_colour256[8];

void graphics_init ()
{
    gint i;

    for (i = 0; i < 8; i++)
    {
	r_colour256[i] = apply_gamma (colour256[i], Config.red_gamma);
	g_colour256[i] = apply_gamma (colour256[i], Config.green_gamma);
	b_colour256[i] = apply_gamma (colour256[i], Config.blue_gamma);
    }
}

static gint currentPicture = -1;
static gint currentMode = -1;

void graphics_clear ()
{
    ms_showpic (0, 0);
    currentPicture = -1;
    currentMode = -1;
}

void graphics_refresh ()
{
    GdkColor colour;
    GtkWidget *viewport;

    graphics_init ();
    
    if (currentPicture != -1 && currentMode != -1)
	ms_showpic (currentPicture, currentMode);

    /*
     * The picture's parent widget is, I believe, an automagically created
     * viewport thingy, so we don't have any handle to it.
     */
    viewport = gtk_widget_get_parent (Gui.picture);

    if (Config.graphics_bg && gdk_color_parse (Config.graphics_bg, &colour))
	gtk_widget_modify_bg (viewport, GTK_STATE_NORMAL, &colour);
    else
	gtk_widget_modify_bg (viewport, GTK_STATE_NORMAL, NULL);
}

/* ------------------------------------------------------------------------- *
 * Still pictures.                                                           *
 * ------------------------------------------------------------------------- */

struct anti_frame
{
    gint x;
    gint y;
    gint width;
    gint height;
    guchar *data;
};

struct picture
{
    type16 width, height;
    struct rgb rgb_palette[16];
    guchar *rgb_data;
    GdkPixbuf *pixbuf;
    gint anti_frame_count;
    struct anti_frame *anti_frames;
    guint animation_timer;
};

static void free_picture (struct picture *picture)
{
    if (picture->animation_timer != 0)
    {
	g_source_remove (picture->animation_timer);
	picture->animation_timer = 0;
    }
    if (picture->rgb_data)
    {
	g_free (picture->rgb_data);
	picture->rgb_data = NULL;
    }
    if (picture->pixbuf)
    {
	g_object_unref (picture->pixbuf);
	picture->pixbuf = NULL;
    }
    if (picture->anti_frames)
    {
	gint i;

	for (i = 0; i < picture->anti_frame_count; i++)
	    if (picture->anti_frames[i].data)
		g_free (picture->anti_frames[i].data);

	g_free (picture->anti_frames);
	picture->anti_frames = NULL;
    }
}

static void display_picture (struct picture *picture)
{
    /*
     * We can't be sure the new picture will fit into the old pixbuf, so we
     * throw it away and create a new one instead. The picture may still be
     * visible afterwards, but that's just the GUI holding on to it after the
     * graphics code has dropped it.
     */
    if (picture->pixbuf)
	g_object_unref (picture->pixbuf);

    picture->pixbuf = gdk_pixbuf_new_from_data (
	picture->rgb_data, GDK_COLORSPACE_RGB, FALSE, 8, picture->width,
	picture->height, 3 * picture->width, NULL, NULL);

    if ((Config.image_constant_height &&
	 Config.image_height != picture->height) ||
	(!Config.image_constant_height &&
	 Config.image_scale != 1.0))
    {
	GdkPixbuf *scaled_pixbuf;
	gdouble scale_factor;

	if (Config.image_constant_height)
	    scale_factor =
		(gdouble) Config.image_height / (gdouble) picture->height;
	else
	    scale_factor = Config.image_scale;

	scaled_pixbuf = gdk_pixbuf_scale_simple (
	    picture->pixbuf,
	    (gint) ((gdouble) picture->width * scale_factor + 0.5),
	    (gint) ((gdouble) picture->height * scale_factor + 0.5),
	    Config.image_filter);
	
	g_object_unref (picture->pixbuf);
	picture->pixbuf = scaled_pixbuf;
    }

    gtk_image_set_from_pixbuf (GTK_IMAGE (Gui.picture), picture->pixbuf);
}

/* ------------------------------------------------------------------------- *
 * Animated pictures. It would have been really neat if we could have just   *
 * handed off the animation data to a GdkPixbufAnimation and then let that   *
 * handle everything. Unfortunately I see no way of doing that.              *
 * ------------------------------------------------------------------------- */

static gboolean display_animation (gpointer user_data)
{
    struct picture *picture = (struct picture *) user_data;
    struct ms_position *frame_pos;
    struct anti_frame *anti_frames;
    guchar *anti_frame_ptr;
    guchar *rgb_ptr;
    type16 frame_count;
    gint i;
    gint x, y;

    /* Extract information about the current set of frames to apply */
    
    if (ms_animate (&frame_pos, &frame_count) == 0)
	return FALSE;

    /*
     * Remove the previous set of frames by applying a set of "anti-frames".
     * In fact, we could probably just restore the image to its original
     * state, but this may be marginally more efficient. Or not.
     *
     * The anti-frames are applied in the opposite order that they were
     * created, so it should work even if they were overlapping.
     */
    
    if (picture->anti_frames)
    {
	for (i = picture->anti_frame_count - 1; i >= 0; i--)
	{
	    anti_frame_ptr = picture->anti_frames[i].data;
	    
	    if (anti_frame_ptr)
	    {
		gint x_min, x_max;
		gint y_min, y_max;

		x_min = picture->anti_frames[i].x;
		x_max = x_min + picture->anti_frames[i].width - 1;
		y_min = picture->anti_frames[i].y;
		y_max = y_min + picture->anti_frames[i].height - 1;

		rgb_ptr =
		    picture->rgb_data + 3 * (y_min * picture->width + x_min);
		
		for (y = y_min; y <= y_max; y++)
		{
		    memcpy (rgb_ptr, anti_frame_ptr,
			    3 * picture->anti_frames[i].width);
		    rgb_ptr += (3 * picture->width);
		    anti_frame_ptr += (3 * picture->anti_frames[i].width);
		}

		g_free (picture->anti_frames[i].data);
	    }
	}
	g_free (picture->anti_frames);
	picture->anti_frames = NULL;
    }

    /*
     * Apply a new set of frames to the picture, while at the same time
     * creating the set of "anti-frames" that will be applied the next
     * time.
     */
    
    if (frame_count > 0)
	anti_frames = g_malloc (frame_count * sizeof (struct anti_frame));
    else
	anti_frames = NULL;
    
    for (i = 0; i < frame_count; i++)
    {
	type16 frame_width;
	type16 frame_height;
	type8 *frame_mask;
	type8 *frame_data;
	gint y_min, y_max, x_min, x_max;
	gint mask_width;

	frame_data = ms_get_anim_frame (frame_pos[i].number, &frame_width,
					&frame_height, &frame_mask);

	if (!frame_data)
	{
	    anti_frames[i].data = NULL;
	    continue;
	}

	/* Clip the frame to fit into the picture */
	
	y_min = MAX (0, frame_pos[i].y);
	y_max = MIN (picture->height, frame_pos[i].y + frame_height) - 1;
	x_min = MAX (0, frame_pos[i].x);
	x_max = MIN (picture->width, frame_pos[i].x + frame_width) - 1;

	/* Does anything at all remain of the frame? */

	if (y_max < y_min || x_max < x_min)
	{
	    anti_frames[i].data = NULL;
	    continue;
	}

	/* Frame mask properties */

	/*
	 * The documentation states that "a mask is exactly 1/8 the size of
	 * the image". This is incorrect. Each line of the mask is made up
	 * from a number of whole 16-bit words. So the mask width is always
	 * an even number.
	 */
	
	mask_width = (((frame_width - 1) / 8) + 2) & (~1);

	/* Prepare the anti-frame */
	
	anti_frames[i].x = x_min;
	anti_frames[i].y = y_min;
	anti_frames[i].width = (x_max - x_min) + 1;
	anti_frames[i].height = (y_max - y_min) + 1;
	anti_frames[i].data =
	    g_malloc (3 * anti_frames[i].width * anti_frames[i].height);

	anti_frame_ptr = anti_frames[i].data;

	for (y = y_min; y <= y_max; y++)
	{
	    type8 *frame_ptr;
	    type8 *mask_ptr = NULL;
	    gint frame_x = x_min - frame_pos[i].x;
	    gint frame_y = y - frame_pos[i].y;

	    /* Copy a line of the picture to the anti-frame */
	    
	    rgb_ptr = picture->rgb_data + 3 * (y * picture->width + x_min);
	    memcpy (anti_frame_ptr, rgb_ptr, 3 * anti_frames[i].width);
	    anti_frame_ptr += 3 * anti_frames[i].width;

	    /* Copy a line of the frame to the picture */
	    
	    frame_ptr =	frame_data + frame_width * frame_y + frame_x;

	    if (frame_mask)
		mask_ptr = frame_mask + mask_width * frame_y + (frame_x / 8);
	    
	    for (x = x_min; x <= x_max; x++)
	    {
		gboolean mask_bit = FALSE;

		/*
		 * If the frame has a mask at all, it will have one bit for
		 * every pixel in the frame. A zero bit means the pixel is
		 * opaque, while a one bit means the pixel is transparent.
		 *
		 * Maybe this really is what the documentation says, but it
		 * wasn't immediately obvious to me.
		 */

		if (mask_ptr)
		{
		    mask_bit = (*mask_ptr & (128 >> (frame_x % 8))) != 0;

		    if ((frame_x % 8) == 7)
			mask_ptr++;
		}

		frame_x++;

		if (!mask_bit)
		{
		    *rgb_ptr++ = picture->rgb_palette[*frame_ptr].red;
		    *rgb_ptr++ = picture->rgb_palette[*frame_ptr].green;
		    *rgb_ptr++ = picture->rgb_palette[*frame_ptr].blue;
		} else
		    rgb_ptr += 3;

		frame_ptr++;
	    }
	}
    }

    /* Display the resulting picture */
    
    display_picture (picture);
    picture->anti_frames = anti_frames;
    picture->anti_frame_count = frame_count;
    return TRUE;
}

/* ------------------------------------------------------------------------- *
 * Main picture display function.                                            *
 * ------------------------------------------------------------------------- */

void ms_showpic (type32 c, type8 mode)
{
    static struct picture picture;
    type8 *raw_data;
    type16 palette[16];
    type8 is_anim;
    guchar *ptr;
    gint i;

    /*
     * Remember the current picture and mode so that we can refresh whatever
     * image is shown if the graphics settings change.
     */
    
    currentPicture = c;
    currentMode = mode;

    /* Remove the current picture */
    
    free_picture (&picture);
    
    /* Mode 0 means turn off graphics */

    if (mode == 0)
    {
	gtk_image_set_from_pixbuf (GTK_IMAGE (Gui.picture), NULL);
	return;
    }

    raw_data = ms_extract (c, &picture.width, &picture.height, palette,
			   &is_anim);

    if (!raw_data)
	return;

    /* Convert the raw, paletted image to RGB data */
    
    picture.rgb_data =
	g_malloc (3 * picture.width * picture.height);

    for (i = 0; i < 16; i++)
    {
	picture.rgb_palette[i].red   = r_colour256[(palette[i] & 0x0f00) >> 8];
	picture.rgb_palette[i].green = g_colour256[(palette[i] & 0x00f0) >> 4];
	picture.rgb_palette[i].blue  = b_colour256[palette[i] & 0x000f];
    }

    for (i = 0, ptr = picture.rgb_data;
	 i < picture.width * picture.height; i++)
    {
	*ptr++ = picture.rgb_palette[raw_data[i]].red;
	*ptr++ = picture.rgb_palette[raw_data[i]].green;
	*ptr++ = picture.rgb_palette[raw_data[i]].blue;
    }
    
    if (is_anim)
    {
	/*
	 * It is quite unlikely that display_animation() would return
	 * anything else than TRUE here, since if it returns FALSE that
	 * would mean we have a one-frame animation.
	 *
	 * However, if animations are turned off we still want the first
	 * set of frames to be applied, because at least for some animations
	 * the static picture is just a test pattern.
	 */
	if (display_animation (&picture) && Config.animate_images)
	    picture.animation_timer =
		g_timeout_add (Config.animation_delay, display_animation,
			       &picture);
	    
    } else
	display_picture (&picture);
}

/* ------------------------------------------------------------------------- *
 * Splash pictures. I'm not sure this code actually belongs here...          *
 * ------------------------------------------------------------------------- */

static gboolean main_window_keypress (GtkWidget *widget, GdkEventKey *event,
				      gpointer user_data)
{
    gtk_main_quit ();
    return FALSE;
}

static void wait_for_keypress ()
{
    gulong sig;

    /*
     * TODO: Also check for mouse clicks? I tried adding that, but for some
     * reason it wouldn't work.
     */
    
    sig = g_signal_connect (G_OBJECT (Gui.main_window), "key-press-event",
			    G_CALLBACK (main_window_keypress), NULL);
    gtk_main ();
    if (!applicationExiting)
	g_signal_handler_disconnect (G_OBJECT (Gui.main_window), sig);
}

void display_splash_screen (gchar *splash_filename, gchar *music_filename)
{
    GdkPixbuf *pixbuf;
    GError *error = NULL;
    gdouble scale_factor;
    gint window_width, window_height;

    pixbuf = gdk_pixbuf_new_from_file (splash_filename, &error);

    if (pixbuf)
    {
	GtkWidget *viewport;
	GtkWidget *splash_screen;
	GdkPixbuf *scaled_pixbuf;
	gint splash_width;
	gint splash_height;

	g_object_ref (Gui.main_box);
	gtk_container_remove (GTK_CONTAINER (Gui.main_window), Gui.main_box);

	viewport = gtk_viewport_new (NULL, NULL);
	gtk_container_add (GTK_CONTAINER (Gui.main_window), viewport);
	
	splash_screen = gtk_image_new ();
	gtk_container_add (GTK_CONTAINER (viewport), splash_screen);
	gtk_widget_show (splash_screen);
	
	splash_width = gdk_pixbuf_get_width (pixbuf);
	splash_height = gdk_pixbuf_get_height (pixbuf);

	/*
	 * The other versions check the pixel aspect ratio of the PNG, but
	 * since I don't see any easy way of querying that I cheat. I know
	 * that if the splash screen is taller than it's wide, double its
	 * width.
	 *
	 * Then we scale the image to match the window size, without losing
	 * the aspect ratio.
	 */
	if (splash_width < splash_height)
	    splash_width *= 2;

	window_width = Config.window_width;
	window_height = Config.window_height;
	
	scale_factor = MIN (
	    (gdouble) window_width / (gdouble) splash_width,
	    (gdouble) window_height / (gdouble) splash_height);

	splash_width = (gint) (splash_width * scale_factor + 0.5);
	splash_height = (gint) (splash_height * scale_factor + 0.5);
	
	scaled_pixbuf = gdk_pixbuf_scale_simple (
	    pixbuf, splash_width, splash_height, Config.image_filter);
	g_object_unref (pixbuf);
	gtk_image_set_from_pixbuf (GTK_IMAGE (splash_screen), scaled_pixbuf);
	g_object_unref (scaled_pixbuf);

	if (Config.graphics_bg)
	{
	    GdkColor colour;

	    if (gdk_color_parse (Config.graphics_bg, &colour))
		gtk_widget_modify_bg (viewport, GTK_STATE_NORMAL, &colour);
	}
	
	gtk_widget_show_all (Gui.main_window);

	if (g_file_test (music_filename, G_FILE_TEST_EXISTS))
	    sound_start_music (music_filename);

	wait_for_keypress ();
	sound_stop_music ();

	if (!applicationExiting)
	{
	    gtk_widget_destroy (splash_screen);
	    gtk_widget_destroy (viewport);
	    gtk_container_add (GTK_CONTAINER (Gui.main_window), Gui.main_box);
	}

	/*
	 * Putting the main box back into the main window doesn't add to
	 * the refcount, so don't unref it here.
	 */
    }
}
