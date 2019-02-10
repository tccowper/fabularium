#ifndef _CHK_X_H_
#define _CHK_X_H_
/*----------------------------------------------------------------------*\

				CHK_X.H
			     Check Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "chk.h"


/* USE: */
#include "context.h"


/* DATA: */


/* FUNCTIONS: */

/* Create a new Check node */
extern CheckNode *newCheck(Expression *exp, List *stms);

/* Analyze a list of Checks */
extern void analyzeChecks(List *chks, Context *context);

/* Generate code for a list of Checks */
extern Aaddr generateChecks(List *chks);

/* Dump a Check node */
extern void dumpCheck(CheckNode *chk);


#endif
