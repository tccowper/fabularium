#ifndef _RUL_X_H_
#define _RUL_X_H_
/*----------------------------------------------------------------------*\

				RUL.H
			      Rule Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE: */
#include "rul.h"


/* Data: */
extern int rulmin, rulmax;
extern int rulcount;



/* Functions: */

/* Allocate a new Rule node */
extern RulNod *newRule(Srcp *srcp, Expression *exp, List *stms);
extern void analyzeRules(void);
extern Aaddr generateRules(void);
extern void dumpRules(RulNod *rul);


#endif
