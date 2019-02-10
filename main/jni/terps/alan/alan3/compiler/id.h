#ifndef _ID_H_
#define _ID_H_
/*----------------------------------------------------------------------*\

				ID.H
			Identifier Nodes Types

\*----------------------------------------------------------------------*/

/* USE: */
#include "acode.h"
#include "sym.h"
#include "srcp.h"


/* TYPES: */

typedef struct IdNode {        /* ID */
    Srcp srcp;                 /* Source position of the identifier */
    char *string;              /* String representation */
    Symbol *symbol;            /* Symbol table pointer */
    int code;
    Aaddr stringAddress;        /* Amemory address to string, if any */
} Id;

#endif


