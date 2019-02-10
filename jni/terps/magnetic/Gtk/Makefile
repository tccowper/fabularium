# Makefile - GtkMagnetic Makefile
# Copyright (c) 2002 Torbjörn Andersson <d91tan@Update.UU.SE>
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111, USA.

########################################################################

SOUND_SYSTEM = SMPEG
# SOUND_SYSTEM = SDL_MIXER
# SOUND_SYSTEM = XMMS
# SOUND_SYSTEM = NONE

# OPTIMIZE_CFLAGS = -g
OPTIMIZE_CFLAGS = -O2 -DG_DISABLE_ASSERT

# EXTRA_CFLAGS = -DGTK_DISABLE_DEPRECATED -DG_DISABLE_DEPRECATED -DPANGO_DISABLE_DEPRECATED
EXTRA_CFLAGS =

MAGNETIC = ../Generic

#######################################################################

CC = gcc

OBJS = emu.o main.o config.o gui.o text.o graphics.o util.o hints.o

DEF_CFLAGS = -Wall `pkg-config --cflags gtk+-2.0` -I$(MAGNETIC)

EMU_CFLAGS = $(DEF_CFLAGS) $(OPTIMIZE_CFLAGS) $(EXTRA_CFLAGS)
CFLAGS = $(EMU_CFLAGS) -ansi

GTK_LIBS = `pkg-config --libs gtk+-2.0`

#######################################################################

# Check which sound system to use

ifeq ($(SOUND_SYSTEM),SMPEG)
	OBJS += sound_smpeg.o
	SOUND_LIBS = `sdl-config --libs` -lsmpeg
endif

ifeq ($(SOUND_SYSTEM),SDL_MIXER)
	OBJS += sound_sdlmixer.o
	SOUND_LIBS = `sdl-config --libs` -lSDL_mixer
endif

ifeq ($(SOUND_SYSTEM),XMMS)
	OBJS += sound_xmms.o
	SOUND_LIBS = `xmms-config --libs`
endif

ifeq ($(SOUND_SYSTEM), NONE)
	OBJS += sound_none.o
	SOUND_LIBS =
endif

#######################################################################

LIBS = $(GTK_LIBS) $(SOUND_LIBS)

gtkmagnetic: $(OBJS)
	$(CC) -o gtkmagnetic $(OBJS) $(LIBS)

clean:
	$(RM) gtkmagnetic *.o *~

emu.o:
	$(CC) -c $(EMU_CFLAGS) $(MAGNETIC)/emu.c

main.o: main.c main.h version.h config.h gui.h text.h graphics.h hints.h util.h
config.o: config.c config.h gui.h text.h graphics.h
gui.o: gui.c main.h config.h gui.h
text.o: text.c main.h config.h gui.h text.h util.h
graphics.o: graphics.c main.h config.h gui.h graphics.h sound.h
sound_none.o: sound_none.c sound.h
util.o: util.c gui.h util.h
hints.o: hints.c config.h gui.h hints.h

sound_smpeg.o: sound_smpeg.c sound.h
	$(CC) -c $(CFLAGS) `sdl-config --cflags` sound_smpeg.c

sound_sdlmixer.o: sound_sdlmixer.c sound.h
	$(CC) -c $(CFLAGS) `sdl-config --cflags` sound_sdlmixer.c

sound_xmms.o: sound_xmms.c sound.h
	$(CC) -c $(CFLAGS) `xmms-config --cflags` sound_xmms.c
