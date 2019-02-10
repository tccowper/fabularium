#ifndef _ELM_H_
#define _ELM_H_
/*----------------------------------------------------------------------*\

				ELM.H
			      Syntax Element Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE: */
#include "srcp.h"
#include "lst.h"
#include "stx.h"
#include "res.h"
#include "id.h"


/* TYPES: */

typedef enum ElementKind {
  PARAMETER_ELEMENT,
  WORD_ELEMENT,
  END_OF_SYNTAX
} ElementKind;

typedef struct Element {
    Srcp srcp;			/* Source position of this element */
    ElementKind kind;		/* Kind of Parameter */
    Id *id;			/* Identifier */
    Aaddr idAddress;            /* Address to parameter name */
    int flags;			/* Multiple/omni etc. flags */
    Restriction *res;		/* Link to the class restriction check */
    Syntax *stx;                /* Pointer back to syntax node */
} Element;


#endif
