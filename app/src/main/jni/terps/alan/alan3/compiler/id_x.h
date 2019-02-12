#ifndef _ID_X_H_
#define _ID_X_H_
/*----------------------------------------------------------------------*\

				ID_X.H
		       Identifier Nodes Export

\*----------------------------------------------------------------------*/

#include "id.h"

/* USE: */
#include "types.h"
#include "sym.h"


/* DATA: */


/* METHODS: */
extern char *generateIdName(void);
extern Bool isGeneratedId(Id *id);
extern Id *newId(Srcp srcp, char str[]);
extern List *newIdList(List *list, char *str);
extern Bool equalId(Id *id1, Id *id2);
extern void symbolizeId(Id *id);
extern Id *findIdInList(Id *theId, List *theList);
extern void generateId(Id *id, TypeKind type);
extern void dumpId(Id *id);
#endif
