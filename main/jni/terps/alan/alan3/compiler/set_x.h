#ifndef _SET_X_H_
#define _SET_X_H_
/*----------------------------------------------------------------------*\

				SET_X.H
			     Set handling

\*----------------------------------------------------------------------*/

#include "exp.h"

#include "context.h"

extern Symbol *classOfMembers(Expression *exp);
extern void verifySetMember(Expression *theSet, Expression *theMember, char contextMessage[]);
void analyzeSetMembers(List *set, TypeKind *type, Symbol **class, Context *context);
#endif
