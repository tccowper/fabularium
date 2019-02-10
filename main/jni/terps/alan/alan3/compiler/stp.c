/*----------------------------------------------------------------------*\

				STP.C
			      Step Nodes

\*----------------------------------------------------------------------*/

#include "stp_x.h"

/* USE: */
#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "lst_x.h"
#include "exp_x.h"
#include "stm_x.h"
#include "dump_x.h"

#include "lmList.h"
#include "emit.h"
#include "acode.h"




/*======================================================================*/
Step *newStep(Srcp *srcp,	/* IN - Source Position */
	      Expression *after, /* IN - Ticks to wait */
	      Expression *exp,	/* IN - Condition to wait for */
	      List *stms)	/* IN - List of statements */
{
  Step *new;		/* The newly allocated node */

  progressCounter();

  new = NEW(Step);

  new->srcp = *srcp;
  new->after = after;
  new->exp  = exp;
  new->stms = stms;

  return(new);
}


/*======================================================================*/
void analyzeSteps(List *stps, Context *context)
{
  List *lst;

  for (lst = stps; lst != NULL; lst = lst->next) {
    Step *step = lst->member.stp;
    if (step->after != NULL) {
      analyzeExpression(step->after, context);
      if (step->after->type != INTEGER_TYPE)
	lmLogv(&step->after->srcp, 330, sevERR, "Integer", "Step After", NULL);
    }
    if (step->exp != NULL) {
      analyzeExpression(step->exp, context);
      if (step->exp->type != BOOLEAN_TYPE)
	lmLogv(&step->exp->srcp, 330, sevERR, "Boolean", "Step Wait Until", NULL);
    }
    analyzeStatements(step->stms, context);
  }
}



/*======================================================================*/
Aaddr generateSteps(List *stps)
{
  List *lst;
  Aaddr adr;
  StepEntry stepEntry;

  for (lst = stps; lst != NULL; lst = lst->next) {
    Step *step = lst->member.stp;
    if (step->after != NULL) { /* After specified */
      step->afteradr = nextEmitAddress();
      generateExpression(step->after);
      emit0(I_RETURN);
    } else
      step->afteradr = 0;
    if (step->exp != NULL) { /* Condition specified */
      step->expadr = nextEmitAddress();
      generateExpression(step->exp);
      emit0(I_RETURN);
    } else
      step->expadr = 0;
    step->stmadr = nextEmitAddress();
    generateStatements(step->stms);
    emit0(I_RETURN);
  }

  /* Now generate a step table */
  adr = nextEmitAddress();
  for (lst = stps; lst != NULL; lst = lst->next) {
    stepEntry.after = lst->member.stp->afteradr;
    stepEntry.exp = lst->member.stp->expadr;
    stepEntry.stms = lst->member.stp->stmadr;
    emitEntry(&stepEntry, sizeof(StepEntry));
  }
  emit(EOF);
  return(adr);
}



/*======================================================================*/
void dumpStep(Step *stp)
{
  put("STP: "); dumpSrcp(stp->srcp); indent();
  put("after: "); dumpExpression(stp->after); nl();
  put("afteradr: "); dumpAddress(stp->afteradr); nl();
  put("exp: "); dumpExpression(stp->exp); nl();
  put("stms: "); dumpList(stp->stms, STATEMENT_LIST); nl();
  put("stmadr: "); dumpAddress(stp->stmadr); out();
}



