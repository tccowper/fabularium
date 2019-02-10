#ifndef _ADD_H_
#define _ADD_H_
/*----------------------------------------------------------------------*\

				ADD.H
			     Add To Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE: */
#include "types.h"
#include "id.h"
#include "prop.h"


/* TYPES: */

typedef struct AddNode {        /* ADD TO */
    Srcp srcp;                  /* Source position */
    Id *toId;                   /* Class to add to */
    Properties *props;
    Aaddr adr;                  /* Generated at address */
} AddNode;


#endif
