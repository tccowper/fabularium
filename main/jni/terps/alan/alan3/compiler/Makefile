#######################################################################
# This is the general Makefile for building the Alan Compiler
# It figures out on which host platform (and possibly on which computer)
# it is running an will include specific Makefiles to build on that
# particular host
#
# In the included platform specific Makefiles you should handle all
# target specific compile and link settings
#

ifneq ($(EMACS),)
JREGROUTPUT = -noansi
else
UNITOUTPUT ?= -c
endif

# Note that this must not have spaces in it lest the variable will include it
OS := ${if ${findstring CYGWIN,${shell uname}},Cygwin,${shell uname}}

# Include the common source lists, need to be before the included rules
include sources.mk

# Include the correct main Makefile depending on the system name
#
ifeq ($(findstring CYGWIN, $(shell uname)), CYGWIN)
ifeq ($(shell uname -n), thoni64)
include Makefile.thoni
else
include Makefile.cygwin
endif
else
include Makefile.$(shell uname)
endif

# then platform and machine independent rules
include Makefile.common
