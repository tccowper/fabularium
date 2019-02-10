SRCS=	scott.c bsd.c
OBJS=	$(SRCS:%.c=%.o)

include config.mk
include compiler.mk
include $(GLK)/Make.$(GLK)

CFLAGS+=-I$(GLK) -g
LDADD+=	-L$(GLK) $(GLKLIB) $(LINKLIBS)

all: scott

%.o: %.c
	$(CC) $(OPT) $(CFLAGS) -c $<

scott: $(OBJS)
	$(CC) $(OPT) -o $@ $^ $(LDADD)

install: scott
	install -d $(DESTDIR)$(MANDIR)/man6
	install -m644 scott.6 $(DESTDIR)$(MANDIR)/man6/
	install -d $(DESTDIR)$(BINDIR)
	install -m755 scott $(DESTDIR)$(BINDIR)/

clean:
	rm -f scott $(OBJS)
