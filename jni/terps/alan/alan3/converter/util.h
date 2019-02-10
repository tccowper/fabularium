#ifndef _UTIL_H_
#define _UTIL_H_
/*----------------------------------------------------------------------*\

  util.h

  Alan compiler utilities unit

\*----------------------------------------------------------------------*/

/* IMPORTS */

#include "types.h"
#include "srcp.h"


/* PUBLIC DATA */

extern Srcp nulsrcp;		/* NULL position for list */
extern long allocated;		/* Calculated memory usage */
extern Bool verbose;		/* Verbose output */
extern long counter;		/* And counter for verbose mode */


/* FUNCTIONS */

#define NEW(x) ((x *) allocate(sizeof(x)))

extern void showProgress(void);
extern void *allocate(int len);
extern char *strlow(char str[]);
extern void unimpl(Srcp *srcp, char *phase);
extern void syserr(char *str, char function[]);
extern void panic(char *str);
extern void terminate(int ecode);
extern void onlyOneSpace(char string[]);
#endif
