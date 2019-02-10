#ifndef _LIM_H_
#define _LIM_H_
/*----------------------------------------------------------------------*\

				LIM.H
			     Limit Nodes

\*----------------------------------------------------------------------*/

/* USE: */
#include "srcp.h"
#include "atr.h"
#include "lst.h"
#include "cnt.h"


/* Types: */

typedef struct LimNod {		/* LIMIT */
  Srcp srcp;			/* Source position */
  Attribute *atr;			/* The attribute that limits */
  List *stms;			/* Statements to execute when exceeded */
  Aaddr stmadr;			/* ACODE address to statements */
} LimNod;



/* Data: */


/* Functions: */

/* Create a new Limit node */
extern LimNod *newlim(Srcp *srcp,
		      Attribute *atr,
		      List *stms);

/* Analyse a Limit node */
extern void analyzeLimit(LimNod *lim, Symbol *classSymbol, Context *context);

/* Generate code for the Limits of a container */
extern Aword generateLimits(ContainerBody *info);

/* Dump a Limit node */
extern void dumpLimit(LimNod *lim);


#endif
