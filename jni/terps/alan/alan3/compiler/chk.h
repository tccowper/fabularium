#ifndef _CHK_H_
#define _CHK_H_
/*----------------------------------------------------------------------*\

				CHK.H
			     Check Nodes

\*----------------------------------------------------------------------*/

/* USE: */
#include "exp.h"
#include "lst.h"
#include "acode.h"


/* TYPES: */

typedef struct ChkNod {		/* CHECK */
  Expression *exp;		/* Expression to check */
  Aaddr expadr;			/* ACODE address to code for expression */
  List *stms;			/* Statements for false check */
  Aaddr stmadr;			/* ACODE address to statement code */
} CheckNode;


#endif
