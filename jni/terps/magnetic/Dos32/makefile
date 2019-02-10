# makefile for Magnetic

LIBPNGDIR = ../../../Libraries/libpng
ZLIBDIR = ../../../Libraries/zlib

CC = gcc
CFLAGS = -O3 -ffast-math -I../generic -I$(LIBPNGDIR) -I$(ZLIBDIR) -DPNG_NO_SNPRINTF
LIBS = -lalleg -lm
OBJS = allegro.o emu.o \
	png.o pngget.o pngerror.o pngmem.o pngrio.o pngrutil.o pngread.o \
	pngrtran.o pngset.o pngtrans.o adler32.o crc32.o inffast.o inflate.o \
	inftrees.o zutil.o

default : magnetic.exe
.PHONY  : clean

clean :
	del *.o

png.o: $(LIBPNGDIR)/png.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngget.o: $(LIBPNGDIR)/pngget.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngerror.o: $(LIBPNGDIR)/pngerror.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngmem.o: $(LIBPNGDIR)/pngmem.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngrio.o: $(LIBPNGDIR)/pngrio.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngrutil.o: $(LIBPNGDIR)/pngrutil.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngread.o: $(LIBPNGDIR)/pngread.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngrtran.o: $(LIBPNGDIR)/pngrtran.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngset.o: $(LIBPNGDIR)/pngset.c
	$(CC) $(CFLAGS) -c -o $@ $^
pngtrans.o: $(LIBPNGDIR)/pngtrans.c
	$(CC) $(CFLAGS) -c -o $@ $^

adler32.o: $(ZLIBDIR)/adler32.c
	$(CC) $(CFLAGS) -c -o $@ $^
crc32.o: $(ZLIBDIR)/crc32.c
	$(CC) $(CFLAGS) -c -o $@ $^
inffast.o: $(ZLIBDIR)/inffast.c
	$(CC) $(CFLAGS) -c -o $@ $^
inflate.o: $(ZLIBDIR)/inflate.c
	$(CC) $(CFLAGS) -c -o $@ $^
inftrees.o: $(ZLIBDIR)/inftrees.c
	$(CC) $(CFLAGS) -c -o $@ $^
zutil.o: $(ZLIBDIR)/zutil.c
	$(CC) $(CFLAGS) -c -o $@ $^

allegro.o : allegro.c
	$(CC) $(CFLAGS) -s -o $@ -c $<

emu.o : ../generic/emu.c
	$(CC) $(CFLAGS) -s -o $@ -c $<

magnetic.exe : $(OBJS)
	$(CC) -s -o $@ $(OBJS) $(LIBS)

