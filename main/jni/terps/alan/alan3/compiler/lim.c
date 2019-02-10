/*----------------------------------------------------------------------*\

				LIM.C
			     Limit Nodes

\*----------------------------------------------------------------------*/

#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "atr_x.h"
#include "context_x.h"
#include "lst_x.h"
#include "sym_x.h"
#include "stm_x.h"
#include "dump_x.h"

#include "lmList.h"

#include "adv.h"
#include "exp.h"
#include "chk.h"
#include "vrb.h"
#include "lim.h"
#include "ext.h"
#include "ins.h"
#include "evt.h"
#include "rul.h"

#include "emit.h"
#include "acode.h"





/*======================================================================

  newlim()

  Allocates and initialises a limnod.

  */
LimNod *newlim(Srcp *srcp,	/* IN - Source Position */
	       Attribute *atr,	/* IN - The attribute */
	       List *stms)	/* IN - Statments */
{
  LimNod *new;			/* The newly allocated area */

  progressCounter();

  new = NEW(LimNod);

  new->srcp = *srcp;
  new->atr = atr;
  new->stms = stms;

  return(new);
}



/*======================================================================*/
void analyzeLimit(LimNod *lim, Symbol *classSymbol, Context *context)
{
  /* Analyze one limit. The attributes that defines the limits must be
     attributes for all instances the container accepts because we
     must be able to check them at run-time.  The predefined attribute
     COUNT is also allowed.
  */

  Attribute *attribute, *foundAttribute;

  progressCounter();

  /* Analyze the attribute */
  attribute = lim->atr;
  if (compareStrings(attribute->id->string, "count") == 0)
    attribute->id->code = 1-I_COUNT;	/* Use instruction code for COUNT meta attribute */
  else if (classSymbol != NULL) {
    foundAttribute = findAttribute(classSymbol->fields.entity.props->attributes, attribute->id);
    if (foundAttribute == NULL)
      lmLog(&attribute->srcp, 407, sevERR, classSymbol->string);
    else if (attribute->type != INTEGER_TYPE)
      unimpl(attribute->srcp, "Analyzer");
    else
      attribute->id->code = foundAttribute->id->code;
  }

  /* Analyze statments */
  analyzeStatements(lim->stms, context);
}



/*----------------------------------------------------------------------*/
static void generateLimit(LimNod *lim)
{
  progressCounter();

  /* Generate statements */
  lim->stmadr = nextEmitAddress();	/* Save ACODE address to statements */
  generateStatements(lim->stms);
  emit0(I_RETURN);
}



/*----------------------------------------------------------------------*/
static void generateLimitEntry(LimNod *lim)
{
  emit(lim->atr->id->code);
  emit(lim->atr->value);
  emit(lim->stmadr);
}


/*======================================================================*/
Aword generateLimits(ContainerBody *info)
{
  List *lst;		/* List of limits */
  Aword limadr;

  if (info->limits == NULL)
    return(0);

  /* First code for all limits */
  for (lst = info->limits; lst != NULL; lst = lst->next)
    generateLimit(lst->member.lim);

  limadr = nextEmitAddress();		/* Save ACODE address to limit table */
  for (lst = info->limits; lst != NULL; lst = lst->next)
    generateLimitEntry(lst->member.lim);
  emit(EOF);
  return(limadr);
}



/*======================================================================*/
void dumpLimit(LimNod *lim)
{
  put("LIM: "); dumpSrcp(lim->srcp); indent();
  put("atr: "); dumpAttribute(lim->atr); nl();
  put("stms: "); dumpList(lim->stms, STATEMENT_LIST); out();
}


