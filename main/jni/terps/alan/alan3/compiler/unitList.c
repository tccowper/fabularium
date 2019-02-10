/*======================================================================*\

  unitList

  Replacement for lmList to be used in unit test where we want to have
  control over the messages logged

\*======================================================================*/

#include "lmList.h"
#include <stdio.h>
#include <setjmp.h>


/* Public for checking */
int lastEcode;
lmSev lastSev;

int readEcode()
{
  int e = lastEcode;

  lastEcode = 0;
  return e;
}

int readSev()
{
  lmSev s = lastSev;

  lastSev = sevNONE;
  return s;
}

extern void lmLog(Srcp *pos,
		  int ecode,
		  lmSev sev,
		  char *istrs)
{
  lastEcode = ecode;
  lastSev = sev;

  if (ecode == 997)
    printf("Unexpected SYSERR: ecode = %d, istrs = '%s'\n", ecode, istrs);
}

extern void lmLogv(Srcp *pos,
		   int ecode,
		   lmSev sev,
		   ...)
{
  lmLog(pos, ecode, sev, (void *)0);
}

extern lmSev lmSeverity()
{
  return lastSev;
}

extern void lmSkipLines(int lines)
{}

extern void lmLiPrint(char str[])
{}

extern void lmLiInit(char header[],
		     char src[],
		     lmMessages msect)
{}

extern void lmList(char ofnm[],
		   int lins,
		   int cols,
		   lmTyp typ,
		   lmSev sevs)
{}

extern void lmLiTerminate(void)
{}

extern void lmLiEnter(Srcp *pos,
		      Srcp *start,
		      char fnm[])
{}

extern int lmMsg(int i, Srcp *pos, char *msg)
{return 0;}
