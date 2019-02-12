# Makefile - GtkLevel9 Makefile
# Copyright (c) 2005 Torbjörn Andersson <d91tan@Update.UU.SE>

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

# OPTIMIZE_CFLAGS = -g
OPTIMIZE_CFLAGS = -O2 -DG_DISABLE_ASSERT

# EXTRA_CFLAGS = -DGTK_DISABLE_DEPRECATED -DG_DISABLE_DEPRECATED -DPANGO_DISABLE_DEPRECATED
EXTRA_CFLAGS =

LEVEL9 = ..

#######################################################################

CC = gcc

OBJS = level9.o bitmap.o main.o config.o gui.o text.o graphics.o util.o

DEF_CFLAGS = -Wall `pkg-config --cflags gtk+-2.0` -I$(LEVEL9)
EMU_CFLAGS = -DBITMAP_DECODER $(DEF_CFLAGS) $(OPTIMIZE_CFLAGS) $(EXTRA_CFLAGS)

CFLAGS = $(EMU_CFLAGS) -ansi

GTK_LIBS = `pkg-config --libs gtk+-2.0`

#######################################################################

LIBS = $(GTK_LIBS)

gtklevel9: $(OBJS)
	$(CC) -o gtklevel9 $(OBJS) $(LIBS)

clean:
	$(RM) gtklevel9 *.o *~

# The main Level 9 interpreter needs some special hackery. It uses stricmp()
# and strnicmp(), which are not portable. Use the same dirty trick as the Glk
# port to fix that. In addition, GCC 4 complains that it "will break strict-
# aliasing rules", so turn off that kind of optimizations.

level9.o:
	$(CC) -c $(EMU_CFLAGS) -fno-strict-aliasing -DNEED_STRICMP_PROTOTYPE -Dstricmp=g_ascii_strcasecmp -Dstrnicmp=g_ascii_strncasecmp $(LEVEL9)/level9.c
bitmap.o:
	$(CC) -c $(EMU_CFLAGS) -fno-strict-aliasing $(LEVEL9)/bitmap.c

main.o: main.c main.h version.h config.h gui.h text.h graphics.h util.h
config.o: config.c config.h gui.h text.h graphics.h
gui.o: gui.c main.h config.h gui.h
text.o: text.c main.h config.h gui.h text.h util.h
graphics.o: graphics.c main.h config.h gui.h graphics.h
util.o: util.c gui.h util.h

