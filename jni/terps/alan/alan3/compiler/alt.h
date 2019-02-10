#ifndef _ALT_H_
#define _ALT_H_
/*----------------------------------------------------------------------*\

				ALT.H
		       Verb Alternatives Nodes

\*----------------------------------------------------------------------*/


/* USE: */
#include "srcp.h"
#include "lst.h"
#include "ins.h"
#include "id.h"
#include "context.h"

#include "acode.h"


/* TYPES: */

typedef enum QualKind {		/* QUAL kinds */
  QUAL_BEFORE,
  QUAL_AFTER,
  QUAL_ONLY,
  QUAL_DEFAULT
} QualKind;


typedef struct AltNod {		/* ALTERNATIVE */
  Srcp srcp;			/* Source position of this alternative */
  Id *id;			/* The parameter ID */
  int parameterNumber;
  QualKind qual;		/* Qualifier, when to execute */
  List *chks;			/* Checks */
  Aaddr chkadr;			/* ACODE address to check table */
  List *stms;			/* Does-part statements */
  Aaddr stmadr;			/* ACODE address to action code */
} Alternative;



/* DATA: */


/* FUNCTIONS: */

/* Create a new verb Alternative node */
extern Alternative *newAlternative(Srcp srcp,
			      Id *id,
			      List *chks,
			      QualKind qual,
			      List *stms);

/* Analyze a list of verb Alternatives */
extern void analyzeAlternatives(List *alts, Context *context);

/* Generate code for a list of verb Alternatives */
extern Aaddr generateAlternatives(List *alts);

/* Dump a verb Alternative */
extern void dumpAlternative(Alternative *alt);

#endif
