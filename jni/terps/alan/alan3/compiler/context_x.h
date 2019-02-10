#ifndef _CONTEXT_X_H_
#define _CONTEXT_X_H_
/*----------------------------------------------------------------------*\

				CONTEXT_X.H
		     Execution Context Structure Exports

\*----------------------------------------------------------------------*/

#include "context.h"

/* USE: */
#include "id.h"
#include "exp.h"

/* DATA: */

/* FUNCTIONS: */

extern Context *newNullContext(void);
extern Context *newRuleContext(void);
extern Context *newStartContext(void);
extern Context *newEventContext(void);
extern Context *newClassContext(Class *theClass);
extern Context *newInstanceContext(Instance *theInstance);
extern Context *newVerbContext(Symbol *theVerb);
extern Context *pushContext(Context *context);
extern Symbol *symbolOfContext(Context *context);
extern Bool inEntityContext(Context *context);
extern Bool inLocationContext(Context *context);
extern Id *classIdInContext(Context *context);
extern Symbol *classOfIdInContext(Context *context, Id *id);
extern void addRestrictionInContext(Context *context, Expression *isa);
extern Symbol *contextRestrictsIdTo(Context *context, Id *id);
#endif
