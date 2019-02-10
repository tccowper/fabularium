#ifndef _INITIALIZE_H_
#define _INITIALIZE_H_
/*----------------------------------------------------------------------*\

			     initialize.h
			   Initialize Nodes

\*----------------------------------------------------------------------*/

/* USE: */
#include "acode.h"
#include "srcp.h"
#include "lst.h"


/* Types: */
typedef struct Initialize {
  Srcp srcp;			/* Source position */
  List *stms;			/* Statements */
  Aaddr stmsAddress;		/* Acode address to the statements */
} Initialize;


#endif
