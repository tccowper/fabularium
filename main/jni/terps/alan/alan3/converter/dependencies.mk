a2a3.o: a2a3.c lmList.h alanCommon.h srcp.h token.h smScan.h sysdep.h \
  lst_x.h lst.h types.h pmParse.h
a2a3.version.o: a2a3.version.c a2a3.version.h version.h
lmList.o: lmList.c lmList.h alanCommon.h srcp.h token.h
lst.o: lst.c lst_x.h lst.h util.h types.h srcp.h
pmErr.o: pmErr.c srcp.h smScan.h alanCommon.h token.h sysdep.h lst_x.h \
  lst.h types.h lmList.h
pmPaSema.o: pmPaSema.c srcp.h smScan.h alanCommon.h token.h sysdep.h \
  lst_x.h lst.h types.h lmList.h
pmParse.o: pmParse.c srcp.h smScan.h alanCommon.h token.h sysdep.h \
  lst_x.h lst.h types.h lmList.h pmParse.h
smScSema.o: smScSema.c a2a3.h lmList.h alanCommon.h srcp.h token.h \
  smScan.h sysdep.h lst_x.h lst.h types.h str.h
smScan.o: smScan.c a2a3.h lmList.h alanCommon.h srcp.h token.h smScan.h \
  sysdep.h lst_x.h lst.h types.h
str.o: str.c str.h sysdep.h types.h
util.o: util.c util.h types.h srcp.h sysdep.h lmList.h alanCommon.h \
  token.h
