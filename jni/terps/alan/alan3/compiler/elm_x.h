#ifndef _ELM_X_H_
#define _ELM_X_H_
/*----------------------------------------------------------------------*\

				ELM_X.H
		     Syntax Element Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "elm.h"

/* USE: */


/* DATA: */


/* FUNCTIONS: */

/* Create a new Syntax Element node */
extern Element *newWordElement(Srcp srcp, Id *id);
extern Element *newParameterElement(Srcp srcp, Id *id, int flags);
extern Element *newEndOfSyntax(void);

/* Analyze a list of Syntax elements and return a list of the parameters */
extern List *analyzeElements(List *elms, List *ress, struct Syntax *stx);

extern Aaddr generateElements(List *elms, struct Syntax *stx);
extern void dumpElement(Element *elm);


#endif
