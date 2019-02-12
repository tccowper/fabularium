/*
 * sound_smpeg.c - SMPEG sound backend
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

/*
 * Both SDL and SMPEG are distributed under the GNU Library Public License,
 * version 2. For more information about SDL and associated libraries, see
 * <http://www.libsdl.org/>.
 *
 * The SMPEG CVS tree resides at <http://www.icculus.org/>.
 */

#include <gtk/gtk.h>
#include "smpeg/smpeg.h"

#include "sound.h"

/*
 * At least in SMPEG 0.4.4 it is absolutely necessary to call SMPEG_status()
 * regularly, because that's where loops are handled. Define this constant to
 * work around this brain-damage.
 */
#define SMPEG_LOOP_BRAIN_DAMAGE

static SMPEG *mp3 = NULL;

#ifdef SMPEG_LOOP_BRAIN_DAMAGE
static gboolean poll_music (gpointer data)
{
    if (!mp3)
	return FALSE;

    SMPEG_status (mp3);
    return TRUE;
}
#endif

void sound_start_music (gchar *filename)
{
    if (!g_file_test (filename, G_FILE_TEST_EXISTS))
	return;

    if (SDL_Init (SDL_INIT_AUDIO) < 0)
	return;

    mp3 = SMPEG_new (filename, NULL, 1);
    if (SMPEG_error (mp3))
    {
	SMPEG_delete (mp3);
	SDL_Quit ();
	mp3 = NULL;
	return;
    }

    SMPEG_enableaudio (mp3, 1);
    SMPEG_enablevideo (mp3, 0);
    SMPEG_setvolume (mp3, 100);
    SMPEG_loop (mp3, 1);
    SMPEG_play (mp3);

#ifdef SMPEG_LOOP_BRAIN_DAMAGE
    /* Five times per second should be plenty */
    g_timeout_add (200, poll_music, NULL);
#endif
}

void sound_stop_music ()
{
    if (mp3)
    {
	SMPEG_stop (mp3);
	SMPEG_delete (mp3);
	SDL_Quit ();
	mp3 = NULL;
    }
}
