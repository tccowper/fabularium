# makefile for Level9

CC = gcc
CFLAGS = -O3 -ffast-math -I.. -DBITMAP_DECODER
LIBS = -lalleg -lm
OBJS = allegro.o level9.o bitmap.o

default : level9.exe
.PHONY  : clean

clean :
	del *.o

allegro.o : allegro.c
	$(CC) $(CFLAGS) -s -o $@ -c $<

level9.o : ../level9.c
	$(CC) $(CFLAGS) -s -o $@ -c $<

bitmap.o : ../bitmap.c
	$(CC) $(CFLAGS) -s -o $@ -c $<

level9.exe : $(OBJS)
	$(CC) -s -o $@ $(OBJS) $(LIBS)

