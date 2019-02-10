#ifndef _ATR_X_H_
#define _ATR_X_H_
/*----------------------------------------------------------------------*\

				ATR_X.H
			Attribute Nodes Export

\*----------------------------------------------------------------------*/

#include "atr.h"


/* Use other definitions */

#include "elm.h"
#include "context.h"


/* Data: */

extern int attributeAreaSize;


/* Functions: */

/* Create a new Attribute node */
extern Attribute *newBooleanAttribute(Srcp srcp, Id *id, Bool value);
extern Attribute *newStringAttribute(Srcp srcp, Id *id, long fpos, int len);
extern Attribute *newIntegerAttribute(Srcp srcp, Id *id, int value);
extern Attribute *newReferenceAttribute(Srcp srcp, Id *id, Id *instance);
extern Attribute *newSetAttribute(Srcp srcp, Id *id, Expression *set);

extern void symbolizeAttributes(List *attributeList, Bool inClassDeclaration);
extern List *sortAttributes(List *attributeList);
extern Attribute *findAttribute(List *attributeList, Id *id);
extern List *combineAttributes(List *ownAttributes, List *inheritedAttributes);
extern void analyzeAllAttributes(void);
extern void analyzeAttributes(List *attributeList, Symbol *symbol, Context *context);
extern Attribute *resolveAttributeToExpression(Expression *exp, Id *attribute, Context *context);
extern Aaddr generateAttributes(List *attributeList, int instanceCode);
extern Aaddr generateStringInit(void);
extern Aaddr generateSetInit(void);
extern void dumpAttribute(Attribute *atr);

#endif
