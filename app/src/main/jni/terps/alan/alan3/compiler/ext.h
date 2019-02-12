#ifndef _EXT_H_
#define _EXT_H_
/*----------------------------------------------------------------------*\

				EXT.H
			      Exit Nodes

\*----------------------------------------------------------------------*/


/* USE: */
#include "acode.h"
#include "srcp.h"
#include "lst.h"
#include "id.h"


/* Types: */

typedef struct Exit {		/* EXIT */
  Srcp srcp;			/* Source position of this exit */
  List *directions;			/* Directions of the exits */
  Id *target;		/* Id of the location to exit to */
  List *chks;			/* List of checks */
  Aaddr chkadr;			/* ACODE address of check code */
  List *stms;			/* List of statements */
  Aaddr stmadr;			/* ACODE address of statements code */
} Exit;

#endif
