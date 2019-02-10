/*----------------------------------------------------------------------*\

                               ALT.C
		       Verb Alternatives Nodes

\*----------------------------------------------------------------------*/

#include "util.h"

#include "lmList.h"

#include "srcp_x.h"
#include "id_x.h"
#include "chk_x.h"
#include "lst_x.h"
#include "stm_x.h"
#include "sym_x.h"
#include "context_x.h"
#include "dump_x.h"

#include "alt.h"
#include "elm.h"

#include "emit.h"
#include "acode.h"


/* PUBLIC: */


/*======================================================================*/
Alternative *newAlternative(Srcp srcp,	/* IN - Source Position */
		       Id *id,	/* IN - The name */
		       List *chks,	/* IN - Checks */
		       QualKind qual,	/* IN - Action qualifier */
		       List *stms)	/* IN - Statements (does-part) */
{
  Alternative *new;			/* The newly created node */

  progressCounter();

  new = NEW(Alternative);

  new->srcp = srcp;
  new->id = id;
  new->chks = chks;
  new->qual = qual;
  new->stms = stms;

  return(new);
}



/*----------------------------------------------------------------------*/
static void analyzeAlternative(Alternative *alt,
			       Context *context)
{
  Symbol *parameter;
  int matchedParameters = 0;
  List *parameters;

  if (alt->id != NULL) {
    /* Alternatives given, find out for which parameter this one is */
    parameter = lookupParameter(alt->id, context->verb->fields.verb.parameterSymbols);
    if (parameter == NULL)
		lmLogv(&alt->id->srcp, 214, sevERR, alt->id->string, context->verb->string, verbHasParametersOrNoneMessage(context), NULL);
    else {
      alt->id->symbol = parameter;
      alt->parameterNumber = parameter->code;
    }
  } else {			/* No alternative given */
    if (inLocationContext(context))
      alt->parameterNumber = -1;
    else if (context->verb != NULL) {
      if (context->verb->fields.verb.parameterSymbols != NULL)
	alt->parameterNumber = 0;
      for (parameters = context->verb->fields.verb.parameterSymbols;
	   parameters != NULL;
	   parameters = parameters->next) {
	if (alt->stms != NULL) {
	  /* Ignore alts without statements, checks doesn't matter */
	  if (context->instance != NULL) {
	    if (context->instance->props->parentId != NULL)
	      if (inheritsFrom(context->instance->props->parentId->symbol,
			       parameters->member.sym->fields.parameter.class))
		matchedParameters++;
	  } else if (context->class != NULL) {
	    if (inheritsFrom(context->class->props->id->symbol,
			     parameters->member.sym->fields.parameter.class))
	      matchedParameters++;
	  }
	}
      }
      if (matchedParameters > 1)
	lmLog(&alt->srcp, 223, sevWAR, context->verb->string);
    }
  }

  if (alt->chks != NULL && alt->chks->member.chk->exp == NULL && alt->stms != NULL)
    lmLog(&alt->srcp, 227, sevWAR, "");

  analyzeChecks(alt->chks, context);
  analyzeStatements(alt->stms, context);
}



/*======================================================================*/
void analyzeAlternatives(List *alts,
			 Context *context)
{
  List *lst;

  for (lst = alts; lst != NULL; lst = lst->next)
    analyzeAlternative(lst->member.alt, context);
}



/*----------------------------------------------------------------------*/
static Aint generateQualifier(QualKind qualifier)
{
    switch (qualifier) {
    case QUAL_BEFORE: return Q_BEFORE;
    case QUAL_AFTER: return Q_AFTER;
    case QUAL_ONLY: return Q_ONLY;
    case QUAL_DEFAULT: return Q_DEFAULT;
    default:
        SYSERR("Unrecognized switch", nulsrcp);
        return Q_DEFAULT;
    }
}


/*----------------------------------------------------------------------*/
static void generateAlternativeEntry(Alternative *alt)
{
  AltEntry entry;

  entry.param = alt->parameterNumber;
  entry.qual = generateQualifier(alt->qual);
  entry.checks = alt->chkadr;
  entry.action = alt->stmadr;

  emitEntry(&entry, sizeof(entry));
}


/*----------------------------------------------------------------------*/
static void generateAlternative(Alternative *alt)
{
  /* First the action, if there is any */
  if (alt->stms == NULL)
    alt->stmadr = 0;
  else {
    alt->stmadr = nextEmitAddress();
    generateStatements(alt->stms);
    emit0(I_RETURN);
  }

  /* Then possible CHECKs */
  if (alt->chks == NULL)
    alt->chkadr = 0;
  else
    alt->chkadr = generateChecks(alt->chks);
}



/*======================================================================*/
Aaddr generateAlternatives(List *alts)
{
  List *lst;
  Aaddr altadr;

  for (lst = alts; lst != NULL; lst = lst->next)
    generateAlternative(lst->member.alt);

  altadr = nextEmitAddress();
  for (lst = alts; lst != NULL; lst = lst->next)
    generateAlternativeEntry(lst->member.alt);
  emit(EOF);

  return(altadr);
}



/*----------------------------------------------------------------------

  duqual()

  Dump a verb qualifier.

  */
static void duqual (QualKind qual)
{
  switch (qual) {
  case QUAL_AFTER:   put("AFTER"); break;
  case QUAL_BEFORE:  put("BEFORE"); break;
  case QUAL_ONLY:    put("ONLY"); break;
  case QUAL_DEFAULT: put("DEFAULT"); break;
  default: put("*** ERROR ***");
  }
}



/*======================================================================*/
void dumpAlternative(Alternative *alt)
{
  if (alt == NULL) {
    put("NULL");
    return;
  }

  put("ALT: "); dumpSrcp(alt->srcp); put(" qual: "); duqual(alt->qual); indent();
  put("id: "); dumpId(alt->id); nl();
  put("chks: "); dumpList(alt->chks, CHECK_LIST); nl();
  put("stms: "); dumpList(alt->stms, STATEMENT_LIST); out();
}
