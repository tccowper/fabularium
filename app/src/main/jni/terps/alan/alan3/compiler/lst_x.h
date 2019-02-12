#ifndef _LST_X_H_
#define _LST_X_H_
/*----------------------------------------------------------------------*\

				LST.H
			      List Nodes

\*----------------------------------------------------------------------*/

#include "lst.h"


/* USE: */
#include <stdio.h>


/* DATA: */

extern void (*(dumpNodeTable[]))();
extern void (*(xmlNodeTable[]))();


/* FUNCTIONS: */

// TODO Define iterator functions?

#define ITERATE(loopVariable,initExpression) for (loopVariable = initExpression; loopVariable != NULL; loopVariable = loopVariable->next)

extern void initDumpNodeList();
extern List *newEmptyList(ListKind kind);
extern List *newList(void *member, ListKind kind);
extern List *concat(List *list, void *member, ListKind kind);
extern List *combine(List *list1, List *list2);
extern void insert(List *where, void *member, ListKind kind);
extern int length(List *aList);
extern List *getLastListNode(List *aList);
extern List *sortList(List *aList, int comparer(List *e1, List *e2));
extern List *copyList(List *aList);
extern void *getMember(List *aList, int number);
extern void *getLastMember(List *theList);
extern List *getListNode(List *aList, int number);

extern void addListNodeDumper(ListKind kind, void (dumper)(void *));
extern void addXmlNodeDumper(ListKind kind, void (dumper)(FILE *));
extern void dumpList(List *lst, ListKind node);
extern void dumpListOfLists(List *lstlst, ListKind node);

extern void xmlList(List *lst, ListKind node, FILE *xmlFile);

#endif
