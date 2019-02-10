#ifndef _RES_X_H_
#define _RES_X_H_
/*----------------------------------------------------------------------*\

				RES_X.H
		Syntax Element Class Restriction Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "res.h"

/* USE: */
#include "srcp.h"
#include "stx.h"
#include "lst.h"
#include "id.h"


/* DATA: */


/* FUNCTIONS: */

extern Restriction *newRestriction(Srcp srcp, Id *parameterId,
			      RestrictionKind kind, Id *classId,
			      List *stms);
extern Bool hasRestriction(Symbol *parameterSymbol, Syntax *syntax);
extern void symbolizeRestrictions(List *restrictions, Symbol *theVerb);
extern void analyzeRestrictions(List *restrictions, Symbol *theVerb);
extern Aaddr generateRestrictions(List *restrictions, Syntax *stx);
extern void dumpRestriction(Restriction *res);


#endif
