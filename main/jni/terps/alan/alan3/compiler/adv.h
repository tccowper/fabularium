#ifndef _ADV_H_
#define _ADV_H_

/* USE other definitions */
#include "lst.h"
#include "whr.h"

/* Types: */

typedef struct Adventure {
    char *name;			/* The basename of the adventure */
    List *syns;			/* List of synonyms */
    List *stxs;			/* List of syntax definitions */
	List *stxsStartingWithInstanceReference; /* List of syntax definitions that starts with an instance reference */
    List *vrbs;			/* List of global verbs */
    List *clas;			/* List of defined classes */
    List *adds;			/* List of additions to the classes */
    List *inss;			/* List of defined instances */
    List *evts;			/* List of events */
    List *cnts;			/* List of containers */
    List *ruls;			/* List of rules */
    List *stringAttributes;	/* List of string attributes to initialize */
    List *setAttributes;        /* List of set attributes to initialize */
    List *prompt;               /* Statements for the user prompt, if any */
    Where *whr;			/* Where to start */
    List *stms;			/* List of start statements */
    int *scores;		/* Pointer to array of scores */
    List *msgs;			/* List of error messages */
    List *resources;		/* List of resources */
    List *ifids;		/* List of IF identifications, incl. IFID */
} Adventure;

#endif
