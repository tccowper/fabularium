# Makefile for DOS Magnetic, Borland C 4.5

CC = bcc +bcc.cfg
LINK = tlink -x -L\dosapps\bc45\lib

all : magnetic.exe title.exe xtract64.exe xtractpc.exe xtractmw.exe gfxlink.exe gfxlink2.exe

clean :
  del *.exe *.obj

tidy :
  del *.obj

# Magnetic interpreter

magnetic.exe : emu.obj maindos.obj getopt.obj lfread.obj
  $(LINK) c0l.obj $?, $<,, emu.lib mathl.lib cl.lib

emu.obj : ..\generic\emu.c
  $(CC) -c -DSAVEMEM -DNO_ANIMATION -o$@ $?

# Title pictures

title.exe : title.obj showpng.obj\
            png.obj pngget.obj pngerror.obj pngrio.obj pngrutil.obj pngmem.obj pngread.obj\
            pngrtran.obj pngset.obj pngtrans.obj\
            adler32.obj crc32.obj inffast.obj inflate.obj inftrees.obj zutil.obj
  $(LINK) @&&|
c0l.obj+
$?
$<,
emu.lib+
mathl.lib+
cl.lib
|

png.obj : \programs\librar~1\libpng\png.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngget.obj : \programs\librar~1\libpng\pngget.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngerror.obj : \programs\librar~1\libpng\pngerror.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngmem.obj : \programs\librar~1\libpng\pngmem.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngrio.obj : \programs\librar~1\libpng\pngrio.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngrutil.obj : \programs\librar~1\libpng\pngrutil.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngread.obj : \programs\librar~1\libpng\pngread.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngrtran.obj : \programs\librar~1\libpng\pngrtran.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngset.obj : \programs\librar~1\libpng\pngset.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?
pngtrans.obj : \programs\librar~1\libpng\pngtrans.c
  $(CC) -c -DPNG_NO_SNPRINTF -o$@ $?

adler32.obj : \programs\librar~1\zlib\adler32.c
  $(CC) -c -o$@ $?
crc32.obj : \programs\librar~1\zlib\crc32.c
  $(CC) -c -o$@ $?
inffast.obj : \programs\librar~1\zlib\inffast.c
  $(CC) -c -o$@ $?
inflate.obj : \programs\librar~1\zlib\inflate.c
  $(CC) -c -o$@ $?
inftrees.obj : \programs\librar~1\zlib\inftrees.c
  $(CC) -c -o$@ $?
zutil.obj : \programs\librar~1\zlib\zutil.c
  $(CC) -c -o$@ $?

# Story file extractors

xtract64.exe : xtract64.obj
  $(LINK) c0l.obj $?, $<,, emu.lib mathl.lib cl.lib

xtract64.obj : ..\generic\xtract64.c
  $(CC) -c -o$@ $?

xtractpc.exe : xtractpc.obj
  $(LINK) c0l.obj $?, $<,, emu.lib mathl.lib cl.lib

xtractpc.obj : ..\generic\xtractpc.c
  $(CC) -c -o$@ $?

xtractmw.exe : xtractmw.obj
  $(LINK) c0l.obj $?, $<,, emu.lib mathl.lib cl.lib

xtractmw.obj : ..\generic\xtractmw.c
  $(CC) -c -o$@ $?

# Graphics extractors

gfxlink.exe : gfxlink.obj
  $(LINK) c0l.obj $?, $<,, emu.lib mathl.lib cl.lib

gfxlink.obj : ..\generic\gfxlink.c
  $(CC) -c -o$@ $?

gfxlink2.exe : gfxlink2.obj
  $(LINK) c0l.obj $?, $<,, emu.lib mathl.lib cl.lib

gfxlink2.obj : ..\generic\gfxlink2.c
  $(CC) -c -o$@ $?

