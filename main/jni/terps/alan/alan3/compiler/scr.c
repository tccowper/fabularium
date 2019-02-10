/*----------------------------------------------------------------------*\

                                SCR.C
                             Script Nodes

\*----------------------------------------------------------------------*/

#include "scr_x.h"

/* IMPORT: */
#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "id_x.h"
#include "lst_x.h"
#include "stm_x.h"
#include "stp_x.h"
#include "sym_x.h"
#include "dump_x.h"

#include "lmList.h"
#include "acode.h"
#include "emit.h"


static int scriptCode = 0;		/* Numbering code */
static List *allScripts;


/*======================================================================*/
Script *newScript(Srcp *srcp, Id *id, Description *description, List *steps
)
{
  Script *new;          /* The newly allocated node */

  progressCounter();

  new = NEW(Script);

  new->srcp = *srcp;
  new->id = id;
  new->id->code = ++scriptCode;
  if (description)
    new->description = description->does;
  else
    new->description = NULL;
  new->steps = steps;

  allScripts = concat(allScripts, new, SCRIPT_LIST);

  return(new);
}



/*======================================================================

  prepareScripts()

  Prepare scripts for this actor (i.e. number them)

  */
void prepareScripts(List *scrs, Id *id)
{
  List *lst;
  List *scrlst;

  if (scrs == NULL) return;

  /* Look for redefinition of script names */
  for (lst = scrs; lst != NULL; lst = lst->next) {
    /* Any multiple of this name ? */
    for (scrlst = lst->next; scrlst != NULL; scrlst = scrlst->next) {
      if (equalId(lst->member.script->id, scrlst->member.script->id))
	lmLog(&scrlst->member.script->srcp, 403, sevERR, id->string);
    }
  }
}


/*======================================================================

  analyzeScripts()

  Analyze the scripts for one actor.

  */
void analyzeScripts(List *scripts, Context *context)
{
  List *lst;

  if (scripts == NULL) return;

  /* Error if defined for HERO */
  if (context->instance != 0) {
    if (scripts != NULL && context->instance->props->id->symbol == theHero)
      lmLog(&scripts->member.script->srcp, 411, sevWAR, "Script");
  }

  for (lst = scripts; lst != NULL; lst = lst->next) {
    /* Analyze the statements */
    analyzeStatements(lst->member.script->description, context);

    /* Finally, analyse the steps inside the script */
    analyzeSteps(lst->member.script->steps, context);
  }
}


/*----------------------------------------------------------------------*/
static Aaddr generateScriptDescription(Script *script)
{
  Aaddr address = 0;

  if (script->description != NULL) {
    address = nextEmitAddress();
    generateStatements(script->description);
    emit0(I_RETURN);
  }
  return address;
}


/*======================================================================*/
Aaddr generateScripts(ACodeHeader *header)
{
  List *lst;
  Aaddr scriptTableAddress;
  ScriptEntry entry;

  if (allScripts == NULL) return 0;

  for (lst = allScripts; lst != NULL; lst = lst->next) {
    lst->member.script->stepAddress = generateSteps(lst->member.script->steps);
    lst->member.script->descriptionAddress = generateScriptDescription(lst->member.script);
    lst->member.script->stringAddress = nextEmitAddress();
    emitString(lst->member.script->id->string);
  }

  /* Script table */
  scriptTableAddress = nextEmitAddress();
  for (lst = allScripts; lst != NULL; lst = lst->next) {
    entry.id = lst->member.script->stringAddress;
    entry.code = lst->member.script->id->code;
    entry.description = lst->member.script->descriptionAddress;
    entry.steps = lst->member.script->stepAddress;
    emitEntry(&entry, sizeof(entry));
  }
  emit(EOF);
  return(scriptTableAddress);
}


/*======================================================================*/
void dumpScript(Script *scr)
{
  put("SCRIPT: "); dumpSrcp(scr->srcp); indent();
  put("id: "); dumpId(scr->id); nl();
  put("steps: "); dumpList(scr->steps, STEP_LIST); nl();
  put("stepAddress: "); dumpAddress(scr->stepAddress); out();
}



