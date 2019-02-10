#ifndef _STP_X_H_
#define _STP_X_H_
/*----------------------------------------------------------------------*\

				STP_X.H
			      Step Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "stp.h"

/* USE: */
#include "context.h"


/* DATA: */


/* FUNCTIONS: */

extern Step *newStep(Srcp *srcp, Expression *after, Expression *exp, List *stms);
extern void analyzeSteps(List *steps, Context *context);
extern Aaddr generateSteps(List *steps);
extern void dumpStep(Step *step);


#endif
