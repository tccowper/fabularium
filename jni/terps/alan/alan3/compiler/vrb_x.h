#ifndef _VRB_X_H_
#define _VRB_X_H_
/*----------------------------------------------------------------------*\

				VRB_X.H
			  Verb Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "vrb.h"

/* USE: */
#include "lst.h"
#include "context.h"


/* DATA: */


/* FUNCTIONS: */

extern Verb *newVerb(Srcp *srcp, List *ids, List *alts, Bool meta);
extern void analyzeVerbs(List *vrbs, Context *context);
extern Bool verbIdFound(Id *verbId, List *verbList);
extern Aaddr generateVerbs(List *vrbs);
extern void dumpVerb(Verb *vrb);


#endif
