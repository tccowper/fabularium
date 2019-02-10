#ifndef _IFID_H_
#define _IFID_H_
/*----------------------------------------------------------------------*\

				IFID.H
			IFID Types and data

\*----------------------------------------------------------------------*/

/* USE: */
#include "srcp.h"
#include "acode.h"


/* TYPES: */

typedef struct IfidNode {	/* IFID */
  Srcp srcp;			/* Source position of the ifidentifier */
  char *name;			/* Name */
  Aaddr nameAddress;
  char *value;			/* Value */
  Aaddr valueAddress;
} IfidNode;

#endif


