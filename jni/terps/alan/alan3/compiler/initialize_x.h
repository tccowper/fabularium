#ifndef _INITIALIZE_X_H_
#define _INITIALIZE_X_H_
/*----------------------------------------------------------------------*\

			    initialize_x.h
			   Intialize Nodes

\*----------------------------------------------------------------------*/

#include "initialize.h"

/* USE: */
#include "context.h"

/* DATA: */


/* FUNCTIONS: */
extern Initialize *newInitialize(Srcp srcp, List *statements);
extern void analyzeInitialize(Initialize *init, Context *context);
extern void generateInitialize(Initialize *init);
extern void dumpInitialize(Initialize *init);

#endif
