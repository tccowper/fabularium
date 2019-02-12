# Makefile for DOS Level9, Borland C 4.5

CC = bcc +bcc.cfg
LINK = tlink -c -x -L\dosapps\bc45\lib

all : level9.exe

clean :
  del *.exe *.obj

tidy :
  del *.obj

level9.exe : level9.obj dos.obj
  $(LINK) c0l.obj $?, $<,, emu.lib mathl.lib cl.lib

level9.obj : ..\level9.c
  $(CC) -c -o$@ $?

