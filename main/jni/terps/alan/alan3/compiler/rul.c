/*----------------------------------------------------------------------*\

				RUL.C
			     Rules Nodes

\*----------------------------------------------------------------------*/

#include "rul.h"

#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "lst_x.h"
#include "adv_x.h"
#include "exp_x.h"
#include "stm_x.h"
#include "context_x.h"
#include "dump_x.h"


#include "lmList.h"

#include "acode.h"
#include "emit.h"


/* PUBLIC */
int rulmin, rulmax;
int rulcount = 0;



/*======================================================================

  newrul()

  Allocates and initialises a new rulnod.

  */
RulNod *newRule(Srcp *srcp,      /* IN - Source Position */
                Expression *exp, /* IN - Expression to wait for */
                List *stms)      /* IN - List of statements */
{
  RulNod *new;		/* The newly allocated node */

  progressCounter();

  new = NEW(RulNod);

  new->srcp = *srcp;
  new->exp  = exp;
  new->stms = stms;

  rulcount++;
  return(new);
}



/*----------------------------------------------------------------------*/
static void analyzeRule(RulNod *rul)
{
  Context *context = newRuleContext();

  progressCounter();

  analyzeExpression(rul->exp, context);
  if (rul->exp->type != BOOLEAN_TYPE && rul->exp->type != ERROR_TYPE)
      lmLogv(&rul->exp->srcp, 330, sevERR, "boolean", "Rule", NULL);
  analyzeStatements(rul->stms, context);
}




/*======================================================================

  analyzeRules()

  Analyze the rules in this adventure;

  */
void analyzeRules(void)
{
  List *rul;		/* Traversal pointer */

  for (rul = adv.ruls; rul != NULL; rul = rul->next)
    analyzeRule(rul->member.rul);
}




/*======================================================================*/
Aaddr generateRules(void)
{
  List *lst;
  Aaddr adr;

  for (lst = adv.ruls; lst != NULL; lst = lst->next) {
    progressCounter();
    lst->member.rul->expadr = nextEmitAddress();
    generateExpression(lst->member.rul->exp);
    emit0(I_RETURN);
    lst->member.rul->stmadr = nextEmitAddress();
    generateStatements(lst->member.rul->stms);
    emit0(I_RETURN);
  }

  adr = nextEmitAddress();
  for (lst = adv.ruls; lst != NULL; lst = lst->next) {
      RuleEntry entry;
      entry.alreadyRun = FALSE;
      entry.exp = lst->member.rul->expadr;
      entry.stms = lst->member.rul->stmadr;
      emitEntry(&entry, sizeof(entry));
  }
  emit(EOF);
  return(adr);
}



/*----------------------------------------------------------------------*/
void dumpRule(RulNod *rul)
{
  put("RUL: "); dumpSrcp(rul->srcp); indent();
  put("exp: "); dumpExpression(rul->exp); nl();
  put("expadr: "); dumpAddress(rul->expadr); nl();
  put("stms: "); dumpList(rul->stms, STATEMENT_LIST); nl();
  put("stmadr: "); dumpAddress(rul->stmadr); out();
}



