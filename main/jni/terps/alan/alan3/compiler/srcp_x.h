#ifndef _SRCP_X_H
#define _SRCP_X_H
/*----------------------------------------------------------------------*\

				SRCP_X.H
		     Source Position Type Exports

\*----------------------------------------------------------------------*/

#include "srcp.h"
#include "acode.h"

/* USE: */
#include "types.h"


/* DATA: */
extern Srcp nulsrcp;


/* FUNCTIONS: */
extern Bool equalSrcp(Srcp srcp1, Srcp srcp2);
extern void generateSrcp(Srcp srcp);
extern Aaddr generateSrcps(void);
extern void dumpSrcp(Srcp srcp);

#endif
