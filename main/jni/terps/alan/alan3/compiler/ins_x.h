#ifndef _INS_X_H_
#define _INS_X_H_
/*----------------------------------------------------------------------*\

                               INS_X.H
                        Instance Nodes Export
                                                                     
\*----------------------------------------------------------------------*/

#include "ins.h"

/* USE: */
#include "adv.h"
#include <stdio.h>


/* DATA: */
extern int instanceCount;


/* METHODS: */

extern void initInstances(void);
extern void addHero(Adventure *adv);
extern void addHeroContainer(void);
extern void addLiteralInstance(void);
extern void addNowhere(void);
extern Instance *newInstance(Srcp *srcp,
			     Id *id,
			     Id *heritage,
			     Properties *props);
extern void symbolizeInstances(void);
extern void analyzeAllInstanceAttributes();
extern void analyzeInstances(void);
extern void generateInstances(ACodeHeader *header);
extern void dumpInstance(Instance *ins);
extern void xmlInstance(Instance *ins, FILE *xmlFile);
extern void xmlInstances(FILE *xmlFile);

#endif
