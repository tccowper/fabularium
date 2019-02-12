#ifndef _WHR_X_H_
#define _WHR_X_H_
/*----------------------------------------------------------------------*\

  WHR.H
  Where Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "whr.h"

/* USE: */
#include "context.h"
#include "wht.h"


/* DATA: */


/* FUNCTIONS: */

extern Where *newWhere(Srcp *srcp, Transitivity transitivity, WhereKind kind, struct Expression *what);
extern void symbolizeWhere(Where *whr);
extern Bool verifyInitialLocation(Where *whr, Context *context);
extern void analyzeWhere(Where *whr, Context *context);
extern Aword generateInitialLocation(Properties *props);
extern void generateTransitivity(Transitivity transitivity);
extern void generateWhere(Where *whr);
extern char *whereKindToString(WhereKind kind);
extern char *transitivityToString(Transitivity transitivity);
extern void dumpTransitivity(Transitivity transitivity);
extern void dumpWhere(Where *whr);

#endif
