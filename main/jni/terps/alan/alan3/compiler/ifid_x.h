#ifndef _IFID_X_H_
#define _IFID_X_H_
/*----------------------------------------------------------------------*\

			       IFID_X.H
			  Ifid Nodes Export

\*----------------------------------------------------------------------*/

#include "ifid.h"

/* USE: */
#include "lst_x.h"


/* DATA: */


/* METHODS: */
extern List *initIfids(void);
extern IfidNode *newIfid(Srcp srcp, char name[], char value[]);
extern Aaddr generateIfids(List *ifids);
extern void dumpIfid(IfidNode *ifid);
#endif
