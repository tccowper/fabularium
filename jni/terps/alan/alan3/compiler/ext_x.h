#ifndef _EXT_X_H_
#define _EXT_X_H_
/*----------------------------------------------------------------------*\

				EXT.H
			  Exit Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "ext.h"

/* USE: */
#include "context.h"


/* DATA: */

extern int dirmin, dirmax;
extern int dircount;


/* FUNCTIONS: */

/* Create a new Exit node */
extern Exit *newExit(Srcp *srcp, List *dirs, Id *to, List *chks, List *stms);
extern void symbolizeExits(List *exts);
extern void analyzeExit(Exit *ext, Context *context);
extern void analyzeExits(List *exts, Context *context);
extern List *combineExits(List *ownExits, List *exitsToAdd);
extern Bool exitIdFound(Id *targetId, List *exits);
extern Aaddr generateExits(List *exts);
extern void dumpExit(Exit *ext);

#endif
