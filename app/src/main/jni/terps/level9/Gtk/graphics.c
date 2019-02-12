/*
 * graphics.c - Pictures and animations
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

#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <gtk/gtk.h>

#include "level9.h"

#include "main.h"
#include "config.h"
#include "gui.h"
#include "graphics.h"

#define ANIMATION_INTERVAL 20

static int graphicsMode = 0;
static gchar *graphicsDir = NULL;
static BitmapType bitmapType = NO_BITMAPS;

static int currentBitmap = -1;

static gboolean interactedWithBitmap = FALSE;

/* ------------------------------------------------------------------------- *
 * Utility functions.                                                        *
 * ------------------------------------------------------------------------- */

typedef struct
{
    guchar red;
    guchar green;
    guchar blue;
} palEntry;

static const palEntry basePalette[] =
{
    { 0x00, 0x00, 0x00 }, /* Black  */
    { 0xFF, 0x00, 0x00 }, /* Red    */
    { 0x30, 0xE8, 0x30 }, /* Green  */
    { 0xFF, 0xFF, 0x00 }, /* Yellow */
    { 0x00, 0x00, 0xFF }, /* Blue   */
    { 0xA0, 0x68, 0x00 }, /* Brown  */
    { 0x00, 0xFF, 0xFF }, /* Cyan   */
    { 0xFF, 0xFF, 0xFF }  /* White  */
};

static palEntry imagePalette[32];

static guchar *frameBuffer = NULL;
static guchar *rgbBuffer = NULL;
static int frameWidth = 0;
static int frameHeight = 0;
static int bitmapWidth = 0;
static int bitmapHeight = 0;

static GdkPixbuf *pixbuf = NULL;

static guint animationTimer = 0;
static guint animationCost = 0;

/*
 * Apply gamma-correction to a colour component. There are apparently several
 * ways of doing this, but the output from ImageMagick's algorithm looks much
 * better to me than the output from Windows Magnetic's algorithm. At least
 * when compensating for the rather dark pictures in Corruption.
 */

static guchar apply_gamma (guchar colour, double gamma)
{
#if 1
    /* Gamma correction, the way ImageMagick does it. */
    return (guchar)
	CLAMP (pow ((double) colour / 255.0, 1.0 / gamma) * 255.0, 0.0, 255.0);
#else
    /* Gamma correction, the way Windows Magnetic does it. */
    return (guchar)
	CLAMP (sqrt ((double) colour * (double) colour * gamma), 0.0, 255.0);
#endif
}

void graphics_init ()
{
    int i;

    for (i = 0; i < G_N_ELEMENTS (imagePalette); i++)
    {
	imagePalette[i].red = 0;
	imagePalette[i].green = 0;
	imagePalette[i].blue = 0;
    }

    if (animationTimer)
    {
	g_source_remove (animationTimer);
	animationTimer = 0;
    }

    if (pixbuf)
    {
	g_object_unref (pixbuf);
	pixbuf = NULL;
    }

    g_free (frameBuffer);
    g_free (rgbBuffer);

    frameBuffer = NULL;
    rgbBuffer = NULL;

    frameWidth = 0;
    frameHeight = 0;
    bitmapWidth = 0;
    bitmapHeight = 0;

    bitmapType = NO_BITMAPS;

    currentBitmap = -1;
    interactedWithBitmap = FALSE;
}

static void display_picture ()
{
    palEntry pal[32];
    guchar *ptr;
    int i, j;

    if (!frameBuffer)
	return;

    if (pixbuf)
	g_object_unref (pixbuf);

    if (bitmapWidth == 0 || bitmapHeight == 0)
	return;

    for (i = 0; i < G_N_ELEMENTS (pal); i++)
    {
	pal[i].red = apply_gamma (imagePalette[i].red, Config.red_gamma);
	pal[i].green = apply_gamma (imagePalette[i].green, Config.green_gamma);
	pal[i].blue = apply_gamma (imagePalette[i].blue, Config.blue_gamma);
    }

    for (i = 0; i < frameHeight; i++)
    {
	ptr = rgbBuffer + i * frameWidth * 3;
	for (j = 0; j < frameWidth; j++)
	{
	    guchar c = frameBuffer[i * frameWidth + j];

	    *ptr++ = pal[c].red;
	    *ptr++ = pal[c].green;
	    *ptr++ = pal[c].blue;
	}
    }

    pixbuf = gdk_pixbuf_new_from_data (
	rgbBuffer, GDK_COLORSPACE_RGB, FALSE, 8, bitmapWidth, bitmapHeight,
	3 * frameWidth,	NULL, NULL);

    if ((Config.image_constant_height && Config.image_height != bitmapHeight)
	|| (!Config.image_constant_height && Config.image_scale != 1.0))
    {
	GdkPixbuf *scaled_pixbuf;
	gdouble scale_factor;

	if (Config.image_constant_height)
	    scale_factor =
		(gdouble) Config.image_height / (gdouble) bitmapHeight;
	else
	    scale_factor = Config.image_scale;

	scaled_pixbuf = gdk_pixbuf_scale_simple (
	    pixbuf,
	    (gint) ((gdouble) bitmapWidth * scale_factor + 0.5),
	    (gint) ((gdouble) bitmapHeight * scale_factor + 0.5),
	    Config.image_filter);
	
	g_object_unref (pixbuf);
	pixbuf = scaled_pixbuf;
    }

    gtk_image_set_from_pixbuf (GTK_IMAGE (Gui.picture), pixbuf);
}

static gboolean graphics_callback (gpointer user_data)
{
    int result = TRUE;

    if (applicationExiting)
    {
	animationTimer = 0;
	return FALSE;
    }
    
    animationCost = 0;
    while (animationCost < Config.animation_speed)
    {
	result = RunGraphics ();

	if (!result)
	{
	    animationTimer = 0;
	    break;
	}
    }

    display_picture ();
    return result;
}

void graphics_run ()
{
    if (graphicsMode == 1)
    {
	if (Config.animate_images)
	{
	    /*
	     * The graphics will be redrawn several times per second. Using an
	     * expensive scaler will really, really slow things down here. To
	     * ensure tha the GUI does not bog down completely under the load,
	     * we make sure that this timer has a lower priority than anything
	     * GTK+ itself does.
	     */
	    if (!animationTimer)
		animationTimer = g_timeout_add_full (
		    G_PRIORITY_LOW, ANIMATION_INTERVAL, graphics_callback,
		    NULL, NULL);
	} else
	{
	    while (RunGraphics ())
		;
	    display_picture ();
	}
    }
}

void graphics_set_directory (gchar *dir)
{
    graphicsDir = dir;
    bitmapType = DetectBitmaps (dir);
}

void graphics_refresh ()
{
    GdkColor colour;
    GtkWidget *viewport;

    if (animationTimer)
    {
	g_source_remove (animationTimer);
	animationTimer = g_timeout_add_full (
	    G_PRIORITY_LOW, ANIMATION_INTERVAL, graphics_callback, NULL, NULL);
    }

    display_picture ();

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

void os_graphics (int mode)
{
    if (animationTimer)
    {
	g_source_remove (animationTimer);
	animationTimer = 0;
    }

    g_free (frameBuffer);
    g_free (rgbBuffer);
    frameBuffer = NULL;
    rgbBuffer = NULL;

    graphicsMode = mode;

    switch (graphicsMode)
    {
	case 1:
	    /* Line/Fill graphics */
	    GetPictureSize (&frameWidth, &frameHeight);
	    bitmapWidth = frameWidth;
	    bitmapHeight = frameHeight;
	    break;

	case 2:
	    /* Bitmap graphics */
	    if (bitmapType == NO_BITMAPS)
		return;

	    frameWidth = MAX_BITMAP_WIDTH;
	    frameHeight = MAX_BITMAP_HEIGHT;
	    break;

	default:
	    /* No graphics */
	    return;
    }

    if (frameWidth == 0 || frameHeight == 0)
	return;

    frameBuffer = (guchar *) g_malloc0 (frameHeight * frameWidth);
    rgbBuffer = (guchar *) g_malloc (frameHeight * frameWidth * 3);
}

void os_cleargraphics ()
{
    if (!frameBuffer)
	return;

    memset (frameBuffer, 0, frameWidth * frameHeight);
}

void os_setcolour (int colour, int index)
{
    if (colour < 0 || colour >= G_N_ELEMENTS (imagePalette))
	return;

    if (index < 0 || index >= G_N_ELEMENTS (basePalette))
	return;

    imagePalette[colour].red = basePalette[index].red;
    imagePalette[colour].green = basePalette[index].green;
    imagePalette[colour].blue = basePalette[index].blue;
}

static void plot (int x, int y, int colour1, int colour2)
{
    if (x < 0 || x >= frameWidth || y < 0 || y >= frameHeight)
	return;

    if (frameBuffer[y * frameWidth + x] == colour2)
	frameBuffer[y * frameWidth + x] = colour1;
}

void os_drawline (int x1, int y1, int x2, int y2, int colour1, int colour2)
{
    /* Bresenham's line algorithm, as described by Wikipedia */
    int delta_x, delta_y;
    int x_step, y_step;
    int err, delta_err;
    int x, y;

    animationCost++;

    gboolean steep = abs (y2 - y1) > abs (x2 - x1);

    if (steep)
    {
	int tmp;

	tmp = x1;
	x1 = y1;
	y1 = tmp;

	tmp = x2;
	x2 = y2;
	y2 = tmp;
    }

    delta_x = abs (x2 - x1);
    delta_y = abs (y2 - y1);
    err = 0;
    delta_err = delta_y;
    x = x1;
    y = y1;

    x_step = (x1 < x2) ? 1 : -1;
    y_step = (y1 < y2) ? 1 : -1;

    if (steep)
	plot (y, x, colour1, colour2);
    else
	plot (x, y, colour1, colour2);

    while (x != x2)
    {
	x += x_step;
	err += delta_err;

	if (2 * err > delta_x)
	{
	    y += y_step;
	    err -= delta_x;
	}

	if (steep)
	    plot (y, x, colour1, colour2);
	else
	    plot (x, y, colour1, colour2);
    }
}

/*
 * Filling is a lot harder to get right than it looks. I made several attempts
 * at adapting algorithms from Interactive Computer Graphics by Peter Burger
 * and Duncan Gillies, before taking the more practical route and adapting the
 * one found in Bill Kendrick's TuxPaint. Hopefully this one will work...
 */

void os_fill (int x, int y, int colour1, int colour2)
{
    int left, right;
    int i;

    animationCost++;

    if (colour1 == colour2)
	return;
    
    if (x < 0 || x >= frameWidth || y < 0 || y >= frameHeight)
	return;

    if (frameBuffer[y * frameWidth + x] != colour2)
	return;

    /* Find left side, filling along the way */

    left = x;

    while (left >= 0 && frameBuffer[y * frameWidth + left] == colour2)
    {
	frameBuffer[y * frameWidth + left] = colour1;
	left--;
    }

    left++;

    /* Find right side, filling along the way */

    right = x + 1;
    
    while (right < frameWidth && frameBuffer[y * frameWidth + right] == colour2)
    {
	frameBuffer[y * frameWidth + right] = colour1;
	right++;
    }

    right--;

    for (i = left; i <= right; i++)
    {
	if (y - 1 >= 0 && frameBuffer[(y - 1) * frameWidth + i] == colour2)
	    os_fill (i, y - 1, colour1, colour2);

	if (y + 1 < frameHeight && frameBuffer[(y + 1) * frameWidth + i] == colour2)
	    os_fill (i, y + 1, colour1, colour2);
    }
}

void graphics_interact ()
{
    if (graphicsMode == 2)
	interactedWithBitmap = TRUE;
}

void os_show_bitmap (int pic, int x, int y)
{
    Bitmap *bitmap;
    int i;

    if (pic == currentBitmap)
	return;

    bitmap = DecodeBitmap (graphicsDir, bitmapType, pic, x, y);

    if (!bitmap)
	return;

    if (currentBitmap != -1 && !interactedWithBitmap)
    {
	guint context_id;

	context_id = gtk_statusbar_get_context_id (
	    GTK_STATUSBAR (Gui.statusbar), "os_show_bitmap");

	gtk_widget_show (Gui.statusbar);
	gtk_statusbar_push (
	    GTK_STATUSBAR (Gui.statusbar), context_id, "Press any key...");

	os_flush ();
	os_readchar (5000);

	if (applicationExiting)
	    return;

	gtk_widget_hide (Gui.statusbar);
	gtk_statusbar_pop (GTK_STATUSBAR (Gui.statusbar), context_id);
    }

    interactedWithBitmap = FALSE;
    currentBitmap = pic;

    bitmapWidth = bitmap->width;
    bitmapHeight = bitmap->height;

    for (i = 0; i < bitmap->npalette; i++)
    {
	imagePalette[i].red = bitmap->palette[i].red;
	imagePalette[i].green = bitmap->palette[i].green;
	imagePalette[i].blue = bitmap->palette[i].blue;
    }

    for (i = 0; i < bitmap->height; i++)
    {
	memcpy (frameBuffer + i * frameWidth,
		bitmap->bitmap + i * bitmap->width,
		bitmap->width);
    }

    display_picture ();
}
