#ifndef _STM_X_H_
#define _STM_X_H_
/*----------------------------------------------------------------------*\

				STM.H
			   Statement Nodes

\*----------------------------------------------------------------------*/

#include "stm.h"


/* USE: */
#include "context.h"

#include "srcp.h"
#include "ins.h"
#include "lst.h"


/* DATA: */


/* METHODS: */


/* Create a new Statement node */
extern Statement *newStatement(Srcp *srcp, StmKind class);
extern Statement *newUseStatement(Srcp srcp, Id *script, Expression *actor);
extern Statement *newDescribeStatement(Srcp srcp, Expression *what);
extern Statement *newLocateStatement(Srcp srcp, Expression *what, Where *where);
extern Statement *newEmptyStatement(Srcp srcp, Expression *what, Where *where);
extern Statement *newIncludeStatement(Srcp srcp, Expression *what, Expression *set);
extern Statement *newExcludeStatement(Srcp srcp, Expression *what, Expression *set);
extern Statement *newEachStatement(Srcp srcp, Id *loopId, List *filters, List *statements);
extern Statement *newScheduleStatement(Srcp srcp, Expression *what, Where *where, Expression *when);
extern Statement *newCancelStatement(Srcp srcp, Expression *what);
extern Statement *newListStatement(Srcp srcp, Expression *what);
extern Statement *newStyleStatement(Srcp srcp, Id *style);
extern Statement *newShowStatement(Srcp srcp, Resource *resource);
extern Statement *newPlayStatement(Srcp srcp, Resource *resource);
extern List *newPrintStatementListFromString(char *string);


/* Analyze a list of statements */
extern void analyzeStatements(List *stms, Context *context);

/* Generate code for a list of statements */
extern void generateStatements(List *stms);

/* Dump a statement node */
extern void dumpStatement(Statement *stm);


#endif
