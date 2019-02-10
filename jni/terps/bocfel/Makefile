SRCS=	blorb.c branch.c dict.c iff.c io.c math.c meta.c memory.c objects.c osdep.c patches.c process.c random.c screen.c sound.c stack.c unicode.c util.c zoom.c zterp.c
OBJS=	$(SRCS:%.c=%.o)

include config.mk
include compiler.mk

CFLAGS+=	-g

ifdef FAST
  GLK=
  PLATFORM=
  NO_SAFETY_CHECKS=1
  NO_CHEAT=1
  NO_WATCHPOINTS=1
endif

ifdef GLK
  SRCS+=	glkstart.c
  CFLAGS+=	-I$(GLK)
  MACROS+=	-DZTERP_GLK

  include $(GLK)/Make.$(GLK)
  LDADD+=	-L$(GLK) $(GLKLIB) $(LINKLIBS)
endif

ifdef GLK_TICK
  MACROS+=	-DZTERP_GLK_TICK
endif

ifeq ($(PLATFORM), unix)
  MACROS+=	-DZTERP_UNIX
ifndef GLK
  LDADD+=	-lcurses
endif
endif

ifeq ($(PLATFORM), win32)
  MACROS+=	-DZTERP_WIN32
endif

ifdef NO_SAFETY_CHECKS
  MACROS+=	-DZTERP_NO_SAFETY_CHECKS
endif

ifdef NO_CHEAT
  MACROS+=	-DZTERP_NO_CHEAT
endif

ifdef NO_WATCHPOINTS
  MACROS+=	-DZTERP_NO_WATCHPOINTS
endif

ifdef TANDY
  MACROS+=	-DZTERP_TANDY
endif

all: bocfel

%.o: %.c
	$(CC) $(OPT) $(CFLAGS) $(MACROS) -c $<

bocfel: $(OBJS)
	$(CC) $(OPT) -o $@ $^ $(LDADD)

clean:
	rm -f bocfel *.o

install: bocfel
	mkdir -p $(DESTDIR)$(BINDIR) $(DESTDIR)$(MANDIR)
	install bocfel $(DESTDIR)$(BINDIR)
	install -m644 bocfel.6 $(DESTDIR)$(MANDIR)

.PHONY: depend
depend:
	makedepend -f- -Y $(MACROS) $(SRCS) > deps 2> /dev/null

tags: $(SRCS)
	ctags -I ZASSERT --c-kinds=+l $^ *.h

bocfel.html: bocfel.6
	mandoc -Thtml -Ostyle=mandoc.css -Ios= $< > $@

include deps
