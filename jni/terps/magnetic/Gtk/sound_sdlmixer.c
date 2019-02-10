/*
 * sound_sdlmixer.c - SDL_mixer sound backend
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
 * Both SDL and SDL_mixer are distributed under the GNU Library Public
 * License, version 2. For more information about SDL and associated
 * libraries, see <http://www.libsdl.org/>.
 */

#include <gtk/gtk.h>
#include "SDL.h"
#include "SDL_mixer.h"

#include "sound.h"

/*
 * REALLY GROSS HACK:
 * 
 * To mix sounds from several sources together, SDL_mixer first has to
 * convert them all to the audio device's expected format. Unfortunately the
 * frequency converter is atrociously bad. It can only multiply or divide
 * the source data's frequency by 2, until it reaches something it believes
 * is close enough.
 *
 * So we basically have to know the sample frequency of the MP3:s, or it will
 * sound horrible. Yuck!
 *
 * There are plans to rewrite SDL_mixer to fix this, and also remove the
 * somewhat artificial distinction between sound and music, but at the time
 * of writing this work has not yet begun.
 */

#define SDLMIXER_BAD_FREQUENCY_CONVERTER

#ifdef SDLMIXER_BAD_FREQUENCY_CONVERTER
#   define MIXER_FREQUENCY    16000
#else
#   define MIXER_FREQUENCY    44100
#endif

static Mix_Music *mp3 = NULL;

void sound_start_music (gchar *filename)
{
    if (!g_file_test (filename, G_FILE_TEST_EXISTS))
	return;

    if (SDL_Init (SDL_INIT_AUDIO) < 0)
	return;

    if (Mix_OpenAudio (MIXER_FREQUENCY, MIX_DEFAULT_FORMAT, 2, 4096) < 0)
    {
	SDL_Quit ();
	return;
    }

    mp3 = Mix_LoadMUS (filename);
    if (!mp3)
    {
	SDL_Quit ();
	return;
    }

    Mix_PlayMusic (mp3, -1);
}

void sound_stop_music ()
{
    if (mp3)
    {
	Mix_FreeMusic (mp3);
	SDL_Quit ();
    }
}
