#ifndef _CNT_X_H_
#define _CNT_X_H_
/*----------------------------------------------------------------------*\

				CNT_X.H
			   Container Nodes

\*----------------------------------------------------------------------*/

#include "cnt.h"

/* USE: */
#include "wht.h"
#include "context.h"


/* DATA: */


/* FUNCTIONS: */

extern ContainerBody *newContainerBody(Srcp srcp, Bool opaque, Id *takes, List *lims, List *hstms, List *estms, List *extractChecks, List *extractStatements);
extern Container *newContainer( ContainerBody *info);
extern void symbolizeContainer(Container *cnt);
extern void verifyContainerForInitialLocation(What *wht, Context *context, char construct[]);
extern void analyzeContainer(Container *cnt, Context *context);
extern void numberContainers(void);
extern Aaddr generateContainers(ACodeHeader *header);
extern void dumpContainer(Container *container);

#endif
