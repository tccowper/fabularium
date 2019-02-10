#ifndef _STX_X_H_
#define _STX_X_H_
/*----------------------------------------------------------------------*\

				STX.H
			      Syntax Nodes
                                                                     
\*----------------------------------------------------------------------*/

#include "stx.h"

/* USE: */
#include "context.h"
#include "elm.h"


/* DATA: */


/* FUNCTIONS: */

extern Syntax *newSyntax(Srcp srcp, Id *verb, List *elements,
			 List *restrictions, Srcp restrictionSrcp);
extern Syntax *newSyntaxWithEOS(Srcp srcp, Id *id, List *restrictionList,
				Srcp restrictionSrcp);

extern void addElement(Syntax *syntax, Element *element);

/* Create a default syntax node */
extern Syntax *defaultSyntax0(char vrbstr[]);
extern Syntax *defaultSyntax1(Id *verb, Context *context);

/* Compare parameter lists of two syntaxes */
extern Bool equalParameterLists(Syntax *stx1,
		     Syntax *stx2);

extern void analyzeSyntaxes(void);
extern Aaddr generateParseTable(void);
extern Aaddr generateParameterNames(List *stxs);
extern Aaddr generateParameterMappingTable(void);
extern void dumpSyntax(Syntax *stx);


#endif
