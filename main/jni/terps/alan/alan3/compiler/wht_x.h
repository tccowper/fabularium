#ifndef _WHT_X_H_
#define _WHT_X_H_
/*----------------------------------------------------------------------*\

				WHT_X.H
			  What Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "wht.h"


/* USE: */
#include "context.h"

/* DATA: */

/* FUNCTIONS: */

extern What *newWhatLocation(Srcp srcp);
extern What *newWhatActor(Srcp srcp);
extern What *newWhatThis(Srcp srcp);
extern What *newWhatId(Srcp srcp, Id *id);
extern void symbolizeWhat(What *wht);
extern void whatIsNotContainer(What *wht, Context *context, char construct[]);
extern Symbol *symbolOfWhat(What *what, Context *context);
extern Bool isConstantWhat(What *what);
extern Bool verifyWhatContext(What *what, Context *context);
extern void generateWhat(What *wht, TypeKind type);
extern void dumpWhat(What *wht);

#endif
