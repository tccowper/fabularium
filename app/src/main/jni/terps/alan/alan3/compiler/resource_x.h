#ifndef _RESOURCE_X_H_
#define _RESOURCE_X_H_

/* OWN definitions */
#include "resource.h"

/* USE other definitions */
#include "lst.h"



/* Data: */


/* FUNCTIONS: */

extern Resource *newResource(Srcp srcp, Id *fileName);
extern void analyzeResource(Resource *resource);
extern List *analyzeResources(List *resources);
extern void generateResources(List *resources);
extern void dumpResource(Resource *resource);

#endif
