#ifndef _RUL_H_
#define _RUL_H_
/*----------------------------------------------------------------------*\

				RUL.H
			      Rule Nodes

\*----------------------------------------------------------------------*/


/* USE: */
#include "srcp.h"
#include "exp.h"
#include "lst.h"
#include "acode.h"


/* Types: */

typedef struct RulNod {		/* RULE */
  Srcp srcp;			/* Source position */
  Expression *exp;		/* Expression */
  Aaddr expadr;			/* ACODE address to expression code */
  List *stms;			/* Statements */
  Aaddr stmadr;			/* ACODE address to statements */
} RulNod;


#endif
