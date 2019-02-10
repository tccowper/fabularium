#ifndef _STP_H_
#define _STP_H_
/*----------------------------------------------------------------------*\

				STP.H
			      Step Nodes

\*----------------------------------------------------------------------*/


/* USE: */
#include "srcp.h"
#include "exp.h"
#include "lst.h"
#include "acode.h"


/* TYPES: */

typedef struct Step {
  Srcp srcp;			/* Source position */
  Expression *after;		/* Possibly after some time */
  Aaddr afteradr;		/* ACODE address to after expression */
  Expression *exp;		/* or a conditional expression */
  Aaddr expadr;			/* ACODE address to conditional expression */
  List *stms;			/* Statements in this block */
  Aaddr stmadr;			/* ACODE address to statements */
} Step;

#endif
