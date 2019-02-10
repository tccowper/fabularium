#ifndef _WHR_H_
#define _WHR_H_
/*----------------------------------------------------------------------*\

  WHR.H
  Where Nodes
                                                                     
\*----------------------------------------------------------------------*/


/* USE other definitions */
#include "wht.h"


/* TYPES: */

typedef enum WhrKind {          /* WHERE kinds */
    WHERE_DEFAULT,              /* None */
    WHERE_HERE,                 /* <what> HERE */
    WHERE_NEARBY,               /* <what> NEARBY */
    WHERE_NEAR,                 /* <what> NEAR <what> */
    WHERE_AT,                   /* AT <what> */
    WHERE_IN,                   /* IN <what> */
    WHERE_INSET                 /* IN <set> */
} WhereKind;


typedef enum Transitivity {
    DEFAULT_TRANSITIVITY = 100,
    TRANSITIVELY = 101,
    DIRECTLY = 102,
    INDIRECTLY = 103
} Transitivity;

typedef struct Where {          /* WHERE node */
    Srcp srcp;                  /* Source position */
    Transitivity transitivity;
    WhereKind kind;             /* Where kind */
    struct Expression *what;    /* What */
} Where;


#endif
