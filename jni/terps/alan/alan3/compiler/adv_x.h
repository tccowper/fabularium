#ifndef _ADV_X_H_
#define _ADV_X_H_

/* OWN definitions */
#include "adv.h"

/* USE other definitions */
#include "dump.h"


/* Data: */
extern Adventure adv;


/* FUNCTIONS: */

extern void initAdventure(void);
extern void symbolizeAdventure(void);
extern void analyzeAdventure(void);
extern void generateAdventure(char acdfnm[], char txtfnm[], char datfnm[]);
extern void dumpAdventure(enum dmpKd dmp);
extern void xmlAdventure(void);
extern void summary(void);


#endif
