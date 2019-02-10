#ifndef _EVT_H_
#define _EVT_H_
/*----------------------------------------------------------------------*\

  EVT.H
  Event Nodes

\*----------------------------------------------------------------------*/


/* Use other definitions */
#include "srcp.h"
#include "id.h"
#include "lst.h"

#include "acode.h"


/* Types: */

typedef struct Event {         /* EVENT */
    Srcp srcp;                 /* Source position */
    Id *id;                    /* Name of this event */
    Aaddr nameAddress;         /* Address to name string (debug) */
    List *stms;                /* List of statements to execute */
    Aaddr stmadr;              /* ACODE address of event statements */
} Event;



/* Data: */


/* Functions: */

extern Event *newEvent(Srcp *srcp, Id *id, List *stms);
extern void analyzeEvents(void);
extern Aaddr generateEvents(ACodeHeader *header);
extern void dumpEvent(Event *evt);


#endif
