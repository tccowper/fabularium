# Makefile for alan compiler
# This makefile is to ensure that all sources are up-to-date
# It will generate parser, scanner, lister and version files
# using tools only available on ThoNi's machines
# Any source distribution should be complete with respect to these
# files and this Makefile can be ignored.
#
# REMEMBER: You have to set both the Path to include the ToolMaker
# directory and the TMHOME environment variable to point there!

TMHOME	= $(HOME)/Utveckling/ToolMaker
TMLIB	= $(TMHOME)/lib/ansi-c

EXTRAS = \
	alan.tmk \
	alan.lmk \
	alan.smk \
	alan.pmk

TMSRCS = \
	pmParse.c pmPaSema.c \
	pmErr.c \
	smScan.c smScSema.c\
	lmList.c

IMPQ    = -sTMHOME\(\"$(TMHOME)\"\)

all : tm smScan.c version.h

tm: .pmkstamp .smkstamp .lmkstamp
	touch .tmstamp

.lmkstamp : alan.lmk alan.tmk $(TMLIB)/List.imp $(TMLIB)/Common.imp
	lmk $(LMKQ) -generate tables alan
	imp $(IMPQ) alan.lmt
	touch .lmkstamp

.pmkstamp: alan.pmk alan.tmk $(TMLIB)/Parse.imp $(TMLIB)/Err.imp $(TMLIB)/Common.imp
	pmk $(PMKQ) -generate tables alan
	sed -e "s/%%SET currentOs(\"WIN32\")/%%SET currentOs(\"cygwin\")/" alan.pmt > alan.pmt2
	imp $(IMPQ) alan.pmt2
	sed -e "1,/P R O D/d" -e "/Summary/,$$ d" alan.pml > alan.prod
	touch .pmkstamp

.smkstamp : alan.smk alan.tmk alan.voc $(TMLIB)/Scan.imp $(TMLIB)/Common.imp
	smk alan -generate tables
	sed -e "s/%%SET currentOs(\"WIN32\")/%%SET currentOs(\"cygwin\")/" alan.smt > alan.smt2
	imp $(IMPQ) alan.smt2
	touch .smkstamp

smScanx.c : .smkstamp

pmParse.h pmParse.c pmPaSema.c pmErr.c alan.voc alan.pml:
	-rm .pmkstamp
	make -f Makefile.tm .pmkstamp

smScan.h smScSema.c:
	-rm .smkstamp
	make -f Makefile.tm .smkstamp

lmList.h lmList.c:
	-rm .lmkstamp
	make -f Makefile.tm .lmkstamp

