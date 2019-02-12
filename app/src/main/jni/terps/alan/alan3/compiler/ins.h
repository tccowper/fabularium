#ifndef _INS_H_
#define _INS_H_
/*----------------------------------------------------------------------*\

				INS.H
			     Instance Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE: */
#include "srcp.h"
#include "id.h"
#include "sym.h"
#include "prop.h"


/* TYPES: */

typedef struct Instance {	/* INSTANCE */
  Srcp srcp;			/* Source position */
  Properties *props;
  InspectionState visited;
} Instance;


#endif
