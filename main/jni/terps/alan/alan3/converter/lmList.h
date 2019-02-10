#ifndef _lmList_H_
#define _lmList_H_
/*----------------------------------------------------------------------*\

	lmList.h

	Header file for ListerMaker generated error message and listing
	handler

\*----------------------------------------------------------------------*/

#include "alanCommon.h"


/* Insert string separator */
#define lmSEPARATOR ((char)0xff)

/* Severity type and codes */
typedef int lmSev;
#define sevNONE 0
#define sevOK   (1<<0)
#define sevINF  (1<<1)
#define sevWAR  (1<<2)
#define sevERR  (1<<3)
#define sevFAT  (1<<4)
#define sevSYS  (1<<5)

#define sevALL (sevOK|sevINF|sevWAR|sevERR|sevFAT|sevSYS)


/* Listing types */
typedef int lmTyp;
#define liNONE  0
#define liSUM   (1<<0)		/* Summary */
#define liMSG   (1<<1)		/* Source lines with messages*/
#define liOK    (1<<2)		/* Correct source lines */
#define liINCL  (1<<3)		/* Look also in PUSHed files */
#define liHEAD  (1<<4)		/* Heading */

#define liTINY (liSUM|liMSG|liHEAD|liINCL)
#define liFULL (liTINY|liOK)

typedef enum lmMessages {
    lm_ENGLISH_Messages
} lmMessages;


/* UNINITIALISED: */
/* Initialise the lmLister System */
extern void lmLiInit(char header[],
				 char src[],
				 lmMessages msect);

/* COLLECTING: */
/* Log a message at a source position */
extern void lmLog(Srcp *pos,
				int ecode,
				lmSev sev,
				char *istrs);

/* Log a message at a source position using va_arg handling */
extern void lmLogv(Srcp *pos,
				int ecode,
				lmSev sev,
				...);

/* Turn listing completely off after a particular source position */
extern void lmLiOff(Srcp *pos);

/* Turn listing on again at a particular source position */
extern void lmLiOn(Srcp *pos);

/* Start reading an included file at a particular source position */
extern void lmLiEnter(Srcp *pos,
				  Srcp *start,
				  char fnm[]);

/* Stop reading from an included file prematurely */
extern void lmLiExit(Srcp *pos);

/* (Un)conditionally skip to a new page at a source position */
extern void lmLiPage(Srcp *pos,
				 int lins);

/* Read worst severity logged so far */
extern lmSev lmSeverity(void);
extern lmSev lmLocSeverity(void);
extern void lmResLocSeverity(void);

/* RETRIEVING: */
/* Create a listing of a selected type in a file or the screen */
extern void lmList(char ofnm[],
				 int lins,
				 int cols,
				 lmTyp typ,
				 lmSev sevs);

/* Return the i'th formatted message, return 0 if not found */
extern int lmMsg(int i,
			       Srcp *pos,
			       char *msg);

/* Print a string on a line in the output file */
extern void lmLiPrint(char str[]);

/* (Un)conditionally skip to a new page in the output file */
extern void lmSkipLines(int lins);

/* Terminate the lmLister system */
extern void lmLiTerminate(void);


#endif

