/*
 * sound_xmms.c - XMMS sound backend
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
 * The X Multimedia System (XMMS) is distributed under the GNU General
 * Public License. For more information, see <http://www.xmms.org/>.
 */

/*
 * This backend is rather UNIX-centric. It's also rather silly. I don't
 * imagine anyone will actually want to use it, but who knows...?
 */

#include <stdlib.h>
#include <gtk/gtk.h>
#include "xmmsctrl.h"

#include "sound.h"

/*
 * I have received confirmation that there a deadlocking bug was introduced
 * in XMMS 1.2.6 which causes the following line to hang:
 *
 *     xmms_remote_playlist (xmms_session, playlist, length, FALSE);
 *
 * Fortunately this code, which should have the exact same effect, will work
 * around the bug:
 *
 *     xmms_remote_playlist_clear (xmms_session);
 *     xmms_remote_playlist (xmms_session, playlist, length, TRUE);
 *     xmms_remote_play (xmms_session);
 *
 * The bug will supposedly be fixed in XMMS 1.2.8.
 */

#define XMMS_PLAYLIST_BUG

static gint old_length;
static gchar **old_playlist;
static gboolean old_repeat;

static void start_music0 (gchar *filename)
{
    gchar *playlist[1];
    gint i;

    playlist[0] = filename;

    old_length = xmms_remote_get_playlist_length (0);
    old_playlist = (gchar **) g_malloc (old_length * sizeof (gchar *));
    for (i = 0; i < old_length; i++)
	old_playlist[i] = xmms_remote_get_playlist_file (0, i);

#ifdef XMMS_PLAYLIST_BUG
    xmms_remote_playlist_clear (0);
    xmms_remote_playlist (0, playlist, 1, TRUE);
    xmms_remote_play (0);
#else
    xmms_remote_playlist (0, playlist, 1, FALSE);
#endif

    old_repeat = xmms_remote_is_repeat (0);
    if (!old_repeat)
	xmms_remote_toggle_repeat (0);
}

static gboolean poll_xmms (gpointer data)
{
    if (xmms_remote_is_running (0))
    {
	start_music0 ((gchar *) data);
	return FALSE;
    }
    return TRUE;
}

void sound_start_music (gchar *filename)
{
    if (!xmms_remote_is_running (0))
    {
	system ("xmms &");
	g_timeout_add (200, poll_xmms, (gpointer) filename);
    } else
	start_music0 (filename);
}

void sound_stop_music ()
{
    gint i;
    
    if (xmms_remote_is_running (0))
    {
	if (xmms_remote_is_playing (0))
	    xmms_remote_stop (0);

	xmms_remote_playlist_clear (0);
	xmms_remote_playlist (0, old_playlist, old_length, TRUE);

	if (xmms_remote_is_repeat (0) != old_repeat)
	    xmms_remote_toggle_repeat (0);
    }

    for (i = 0; i < old_length; i++)
	g_free (old_playlist[i]);
    g_free (old_playlist);
}
