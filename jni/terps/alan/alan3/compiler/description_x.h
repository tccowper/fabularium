#ifndef _DESCRIPTION_X_H_
#define _DESCRIPTION_X_H_

/* OWN definitions */
#include "description.h"

/* USE other definitions */
#include "context.h"


/* Data: */


/* FUNCTIONS: */

extern Description *newDescription(Srcp checkSrcp, List *checks, Srcp srcp, List *statements);
extern List *checksOf(Description *description);
extern List *doesOf(Description *description);
extern void analyzeDescription(Description *description, Context *context);
extern Aword checksAddressOf(Description *description);
extern Aword doesAddressOf(Description *description);
extern void generateDescription(Description *description);
extern void dumpDescription(Description *description);

#endif
