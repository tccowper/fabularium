#ifndef _SCR_H_
#define _SCR_H_
/*----------------------------------------------------------------------*\

				SCR.H
			     Script Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE other definitions */
#include "srcp.h"
#include "lst.h"
#include "ins.h"


/* Types: */

typedef struct Script {
  Srcp srcp;			/* Source position */
  Id *id;			/* Id for this script */
  Aaddr stringAddress;
  List *description;		/* List of optional description statements */
  Aaddr descriptionAddress;	/* ACODE address description statements */
  List *steps;			/* List of step-blocks */
  Aaddr stepAddress;		/* ACODE address to step table */
} Script;

#endif
