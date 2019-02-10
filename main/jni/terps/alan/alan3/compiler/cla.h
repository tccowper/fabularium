#ifndef _CLA_H_
#define _CLA_H_
/*----------------------------------------------------------------------*\

				CLASS.H
			     Class Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE: */
#include "types.h"
#include "id.h"
#include "prop.h"


/* TYPES: */

typedef struct Class {		/* CLASS */
  Srcp srcp;			/* Source position */
  Properties *props;
  Aaddr adr;			/* Generated at address */
} Class;


#endif
