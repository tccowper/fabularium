#ifndef _ADD_X_H_
#define _ADD_X_H_
/*----------------------------------------------------------------------*\

			     Add To Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "add.h"

/* USE: */


/* DATA: */


/* METHODS: */

extern AddNode *newAdd(Srcp srcp,
                       Id *id,
                       Id *parent,
                       Properties *props);
extern void addAdditions(void);
extern void dumpAdd(AddNode *add);

#endif
