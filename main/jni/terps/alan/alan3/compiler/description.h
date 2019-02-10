#ifndef _DESCRIPTION_H_
#define _DESCRIPTION_H_
/*----------------------------------------------------------------------*\

			      DESCRIPTION.H
			Description Structure

\*----------------------------------------------------------------------*/

/* USE: */
#include "srcp.h"
#include "lst.h"
#include "acode.h"


/* TYPES: */

typedef struct Description {
  Srcp checkSrcp;
  List *checks;
  Aword checksAddress;		/* ACODE address to description checks */
  Srcp doesSrcp;
  List *does;			/* DESCRIPTION statements */
  Aword doesAddress;		/* ACODE address to DESCRIPTION code */
} Description;


#endif
