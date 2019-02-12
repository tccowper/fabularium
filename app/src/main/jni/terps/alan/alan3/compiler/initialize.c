/*----------------------------------------------------------------------*\

			     initialize.c
		   Node for initializing a instance

\*----------------------------------------------------------------------*/

#include "initialize_x.h"

/* IMPORT */
#include "util.h"
#include "emit.h"
#include "dump_x.h"
#include "srcp_x.h"
#include "stm_x.h"
#include "lst_x.h"


/*======================================================================*/
Initialize *newInitialize(Srcp srcp, List *statements)
{
  Initialize *new;                  /* The newly allocated area */

  progressCounter();

  new = NEW(Initialize);

  new->srcp = srcp;
  new->stms = statements;

  return(new);
}

/*======================================================================*/
void analyzeInitialize(Initialize *init, Context *context)
{
  if (init)
    analyzeStatements(init->stms, context);
}

/*======================================================================*/
void generateInitialize(Initialize *init)
{
  if (init) {
    init->stmsAddress = nextEmitAddress();
    generateStatements(init->stms);
    emit0(I_RETURN);
  }
}

/*======================================================================*/
void dumpInitialize(Initialize *init)
{
  put("INITIALIZE: "); dumpPointer(init);
  if (init) {
    dumpSrcp(init->srcp); indent();
    put("stms: "); dumpList(init->stms, STATEMENT_LIST); nl();
    put("stmsAddress: "); dumpAddress(init->stmsAddress); out();
  }
}
