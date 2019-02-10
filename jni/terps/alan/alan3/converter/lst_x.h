#ifndef _LST_X_H_
#define _LST_X_H_
/*----------------------------------------------------------------------*\

				LST.H
			      List Nodes

\*----------------------------------------------------------------------*/

#include "lst.h"


/* USE: */

/* DATA: */

extern void (*(dumpNodeTable[]))();


/* FUNCTIONS: */

#define TRAVERSE(loopVariable,initExpression) for (loopVariable = initExpression; loopVariable != NULL; loopVariable = loopVariable->next)

extern void initDumpNodeList();
extern List *concat(List *lst, void *elem, ListKind kind);
extern List *combine(List *lst1, List *lst2);
extern void insert(List *where, void *element, ListKind kind);
extern int length(List *aList);
extern List *tailOf(List *aList);
extern List *sortList(List *aList, int comparer(List *e1, List *e2));


/* Dump a list of nodes */
extern void dumpList(List *lst, ListKind nod);

/* Dump a list of lists of nodes */
extern void dumpListOfLists(List *lstlst, ListKind nod);

#endif
