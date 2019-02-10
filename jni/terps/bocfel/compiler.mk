ifeq ($(CC), gcc)
CFLAGS+=	-Wall -Wshadow -Wmissing-prototypes -std=c99 -pedantic
endif

ifeq ($(CC), clang)
CFLAGS+=	-Wall -Wunused-macros -Wmissing-prototypes -std=c99 -pedantic
endif

ifeq ($(CC), icc)
CFLAGS+=	-w2 -ww1793 -wd2259,2557,869,981 -std=c99
endif

ifeq ($(CC), suncc)
CFLAGS+=	-xc99=all -Xc -v
endif

ifeq ($(CC), opencc)
CFLAGS+=	-Wall -std=c99
endif

ifeq ($(CC), cparser)
CFLAGS+=	-Wno-experimental -std=c99
endif

ifeq ($(shell basename $(CC)), ccc-analyzer)
CFLAGS+=	-std=c99
endif

ifneq ($(CCHOST),)
CC:=	$(CCHOST)-$(CC)
endif
