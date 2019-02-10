#ifndef _VRB_H_
#define _VRB_H_
/*----------------------------------------------------------------------*\

				VRB.H
			      Verb Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE: */
#include "srcp.h"
#include "lst.h"
#include "sym.h"
#include "stx.h"


/* TYPES: */

typedef struct Verb {
    Srcp srcp;           /* Source position */
    List *ids;           /* List of verb IDs to perform this action */
    List *alternatives;  /* Alternative bodies */
    Bool meta;
    Symbol *symbol;
    Aaddr altAddress;    /* AN - Address to alt-table */
    Syntax *stx;         /* AN - Which syntax defines this verb */
} Verb;

#endif
