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
	smScanx.c smScSema.c\
	lmList.c alanCommon.h

IMPQ    = -sTMHOME\(\"$(TMHOME)\"\)

all : tm smScanx.c sysdep.h sysdep.c version.h alan.atg alan.g

.PHONY: x
x :
	@echo TMLIB=$(TMLIB)
	@echo TMHOME=$(TMHOME)


tm: .pmkstamp .smkstamp .lmkstamp alan.prod
	touch .tmstamp

.lmkstamp: alan.lmk alan.tmk $(TMLIB)/List.imp $(TMLIB)/Common.imp
	lmk $(LMKQ) -generate tables alan
	imp $(IMPQ) alan.lmt
	touch .lmkstamp

.pmkstamp: alan.pmk alan.tmk $(TMLIB)/Parse.imp $(TMLIB)/Err.imp $(TMLIB)/Common.imp
	pmk $(PMKQ) -generate tables alan
	sed -e "s/%%SET currentOs(\"WIN32\")/%%SET currentOs(\"cygwin\")/" alan.pmt > alan.pmt2
	imp $(IMPQ) alan.pmt2
	touch .pmkstamp

alan.prod : prod.sed alan.pml
	sed -f prod.sed alan.pml > alan.prod

alan.atg : coco.sed coco.header alan.prod
	cp coco.header alan.atg
	sed -f coco.sed alan.prod >> alan.atg

alan.g : antlr.sed antlr.header alan.prod
	cp antlr.header alan.g
	sed -f antlr.sed alan.prod >> alan.g

.smkstamp : alan.smk alan.tmk alan.voc $(TMLIB)/Scan.imp $(TMLIB)/Common.imp
	smk alan -generate tables
	sed -e "s/%%SET currentOs(\"WIN32\")/%%SET currentOs(\"cygwin\")/" alan.smt > alan.smt2
	imp $(IMPQ) alan.smt2
	sed -e "1,/START of scanning tables/d" -e "/END of scanning tables/,$$ d" -e "/static UByte1 smMap/,/;/d" -e "/static UByte1 smDFAcolVal/,/;/d" -e "/static UByte1 smDFAerrCol/,/;/d" smScan.c > smScan.tbl
	echo "/* ISO scanner tables */" > smScan.iso.new
	echo "UByte1 smIsoMap[256]={" >> smScan.iso.new
	sed -e "1,/static UByte1 smMap/d" -e "/;/,$$ d" smScan.c >> smScan.iso.new
	echo ";" >> smScan.iso.new
	echo "" >> smScan.iso.new
	echo "UByte1 smIsoDFAcolVal[256]={" >> smScan.iso.new
	sed -e "1,/static UByte1 smDFAcolVal/d" -e "/;/,$$ d" smScan.c >> smScan.iso.new
	echo ";" >> smScan.iso.new
	echo "" >> smScan.iso.new
	echo "UByte1 smIsoDFAerrCol[256]={" >> smScan.iso.new
	sed -e "1,/static UByte1 smDFAerrCol/d" -e "/;/,$$ d" smScan.c >> smScan.iso.new
	echo ";" >> smScan.iso.new
	echo "" >> smScan.iso.new
	if test -f smScan.iso ; then \
		if cmp smScan.iso smScan.iso.new ; then \
			rm smScan.iso.new ; \
		else \
			mv smScan.iso.new smScan.iso ; \
		fi ; \
	else \
		mv smScan.iso.new smScan.iso ; \
	fi ;
	smk -set MAC alan -generate tables
	imp $(IMPQ) alan.smt
	echo "/* MAC scanner tables */" > smScan.mac.new
	echo "UByte1 smMacMap[256]={" >> smScan.mac.new
	sed -e "1,/static UByte1 smMap/d" -e "/;/,$$ d" smScan.c >> smScan.mac.new
	echo ";" >> smScan.mac.new
	echo "" >> smScan.mac.new
	echo "UByte1 smMacDFAcolVal[256]={" >> smScan.mac.new
	sed -e "1,/static UByte1 smDFAcolVal/d" -e "/;/,$$ d" smScan.c >> smScan.mac.new
	echo ";" >> smScan.mac.new
	echo "" >> smScan.mac.new
	echo "UByte1 smMacDFAerrCol[256]={" >> smScan.mac.new
	sed -e "1,/static UByte1 smDFAerrCol/d" -e "/;/,$$ d" smScan.c >> smScan.mac.new
	echo ";" >> smScan.mac.new
	echo "" >> smScan.mac.new
	if test -f smScan.mac ; then \
		if cmp smScan.mac smScan.mac.new ; then \
			rm smScan.mac.new ; \
		else \
			mv smScan.mac.new smScan.mac ; \
		fi ; \
	else \
		mv smScan.mac.new smScan.mac ; \
	fi ;
	smk -set PC alan -generate tables
	imp $(IMPQ) alan.smt
	echo "/* DOS scanner tables */" > smScan.dos.new
	echo "UByte1 smDosMap[256]={" >> smScan.dos.new
	sed -e "1,/static UByte1 smMap/d" -e "/;/,$$ d" smScan.c >> smScan.dos.new
	echo ";" >> smScan.dos.new
	echo "" >> smScan.dos.new
	echo "UByte1 smDosDFAcolVal[256]={" >> smScan.dos.new
	sed -e "1,/static UByte1 smDFAcolVal/d" -e "/;/,$$ d" smScan.c >> smScan.dos.new
	echo ";" >> smScan.dos.new
	echo "" >> smScan.dos.new
	echo "UByte1 smDosDFAerrCol[256]={" >> smScan.dos.new
	sed -e "1,/static UByte1 smDFAerrCol/d" -e "/;/,$$ d" smScan.c >> smScan.dos.new
	echo ";" >> smScan.dos.new
	echo "" >> smScan.dos.new
	if test -f smScan.dos ; then \
		if cmp smScan.dos smScan.dos.new ; then \
			rm smScan.dos.new ; \
		else \
			mv smScan.dos.new smScan.dos ; \
		fi ; \
	else \
		mv smScan.dos.new smScan.dos ; \
	fi
	sed -e "1,/START of scanning tables/w smScan.head" -e "/END of scanning tables/,$$ w smScan.tail" smScan.c > /dev/null
	cat smScan.head > smScanx.c
	echo "UByte1 *smMap;" >> smScanx.c
	echo "UByte1 *smDFAcolVal;" >> smScanx.c
	echo "UByte1 *smDFAerrCol;" >> smScanx.c
	echo "" >> smScanx.c
	cat smScan.iso >> smScanx.c
	cat smScan.mac >> smScanx.c
	cat smScan.dos >> smScanx.c
	cat smScan.tbl >> smScanx.c
	cat smScan.tail >> smScanx.c
	dos2unix smScanx.c
	dos2unix smScSema.c
	touch .smkstamp

smScanx.c : .smkstamp

pmParse.h pmParse.c pmPaSema.c pmErr.c alan.voc alan.pml:
	-rm .pmkstamp
	make -f Makefile.tm .pmkstamp

smScan.h smScSema.c:
	-rm .smkstamp
	make -f Makefile.tm .smkstamp

lmList.h lmList.c alanCommon.h:
	-rm .lmkstamp
	make -f Makefile.tm .lmkstamp

