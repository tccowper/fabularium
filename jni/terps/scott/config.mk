# Select the compiler to be used.  See compiler.mk for supported compilers.
# Even if your compiler is not officially supported, it might still work; C99
# support is required, however.
CC=		gcc

# Select the optimization level.
OPT=		-O

# Set the target directories for the install of the binary and the man page.
PREFIX=		/usr
BINDIR=		$(PREFIX)/games
MANDIR=		$(PREFIX)/man

# This variable controls which Glk implementation will be used.  The
# Glk library must exist in a directory of the same name (so if Glk is set to
# glktermw, the Glk implementation will be found in glktermw/).
GLK=		gargoyle
