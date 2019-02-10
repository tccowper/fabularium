#ifndef _WHT_H_
#define _WHT_H_
/*----------------------------------------------------------------------*\

				WHT.H
			      What Nodes
                                                                     
\*----------------------------------------------------------------------*/

/* USE: */
#include "id.h"


/* TYPES: */

typedef enum WhatKind {
  WHAT_LOCATION,
  WHAT_ACTOR,
  WHAT_THIS,
  WHAT_ID
} WhatKind;

typedef struct What {
  Srcp srcp;			/* Source position */
  WhatKind kind;		/* What kind */
  Id *id;			/* Identifier if WHAT_ID */
} What;

#endif
