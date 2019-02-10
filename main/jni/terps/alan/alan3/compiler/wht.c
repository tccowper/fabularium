/*----------------------------------------------------------------------*\

  WHT.C
  What Nodes

\*----------------------------------------------------------------------*/

#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "wht_x.h"
#include "exp_x.h"
#include "id_x.h"
#include "sym_x.h"
#include "context_x.h"
#include "dump_x.h"

#include "lmList.h"
#include "emit.h"


/*----------------------------------------------------------------------*/
static What *newWhat(Srcp *srcp,	/* IN - Source position */
					 WhatKind kind,	/* IN - What kind */
					 Id *id)	/* IN - ID or NULL */
{
	What *new;

	progressCounter();

	new = NEW(What);

	new->srcp = *srcp;
	new->kind = kind;
	new->id = id;

	return(new);
}


/*======================================================================*/
What *newWhatLocation(Srcp srcp)
{
	return newWhat(&srcp, WHAT_LOCATION, NULL);
}

/*======================================================================*/
What *newWhatActor(Srcp srcp)
{
	return newWhat(&srcp, WHAT_ACTOR, NULL);
}

/*======================================================================*/
What *newWhatThis(Srcp srcp)
{
	return newWhat(&srcp, WHAT_THIS, NULL);
}

/*======================================================================*/
What *newWhatId(Srcp srcp, Id *id)
{
	return newWhat(&srcp, WHAT_ID, id);
}

/*======================================================================*/
void symbolizeWhat(What *wht)
{
	switch (wht->kind) {
	case WHAT_ID:
		symbolizeId(wht->id);
		break;
	default:
		break;
	}
}


/*======================================================================*/
Symbol *symbolOfWhat(What *what, Context *context) {
	switch (what->kind) {
	case WHAT_THIS:
		return symbolOfContext(context);
	case WHAT_LOCATION:
		return locationSymbol;
	case WHAT_ACTOR:
		return actorSymbol;
	case WHAT_ID:
        return what->id->symbol;
	default:
		SYSERR("Unexpected What kind", what->srcp);
	}
	return NULL;
}



/*======================================================================*/
void whatIsNotContainer(What *wht, Context *context, char construct[])
{
	Symbol *sym;

	if (wht == NULL)
		return;

	switch (wht->kind) {
	case WHAT_THIS:
		lmLog(&wht->srcp, 309, sevERR, "");
		break;
	case WHAT_ID:
		sym = wht->id->symbol;
		if (sym)
			switch (sym->kind) {
			case PARAMETER_SYMBOL:
				lmLogv(&wht->srcp, 312, sevERR, "Parameter", wht->id->string, "a Container",
					   "because it is not restricted to Container, or to a class which has the Container property in the Syntax or using surrounding If statements", NULL);
				break;
			default:
				lmLogv(&wht->srcp, 318, sevERR, wht->id->string, construct, NULL);
				break;
			}
		break;

	case WHAT_LOCATION:
		break;

	case WHAT_ACTOR:
		lmLogv(&wht->srcp, 428, sevERR, construct, "a Container, which the Current Actor is not since the class 'actor' does not have the Container property", NULL);
		break;

	default:
		SYSERR("Unrecognized switch", wht->srcp);
		break;
	}
}


/*======================================================================*/
Bool isConstantWhat(What *what) {
	switch (what->kind) {
	case WHAT_ACTOR:
	case WHAT_LOCATION:
	case WHAT_THIS:
		return FALSE;
	case WHAT_ID:
		return isConstantIdentifier(what->id);
    default:
        SYSERR("Unexpected what->kind", what->srcp);
        return FALSE;
	}
}

/*======================================================================*/
Bool verifyWhatContext(What *what, Context *context) {
	switch (what->kind) {

	case WHAT_ACTOR:
		if (context->kind == EVENT_CONTEXT) {
			lmLogv(&what->srcp, 412, sevERR, "Actor", "Events", NULL);
			return FALSE;
		}
		break;

	case WHAT_LOCATION:
	case WHAT_ID:
		break;

	case WHAT_THIS:
		if (!inEntityContext(context)) {
			lmLog(&what->srcp, 421, sevERR, "");
			return FALSE;
		}
		break;

	default:
		SYSERR("Unexpected What kind", what->srcp);
		break;
	}
	return TRUE;
}


/*======================================================================*/
void generateWhat(What *wht, TypeKind type)
{
	switch (wht->kind) {
	case WHAT_LOCATION:
		emitVariable(V_CURLOC);
		break;
	case WHAT_ACTOR:
		emitVariable(V_CURACT);
		break;
	case WHAT_ID:
		generateId(wht->id, type);
		break;
	case WHAT_THIS:
		emitVariable(V_CURRENT_INSTANCE);
		break;
	default:
		SYSERR("Unexpected case", wht->srcp);
	}
}


/*======================================================================

  dumpWhat()

  Dump a What node.

*/
void dumpWhat(What *wht)
{
	if (wht == NULL) {
		put("NULL");
		return;
	}

	put("WHAT: "); dumpSrcp(wht->srcp); indent();
	put("kind: ");
	switch (wht->kind) {
	case WHAT_LOCATION: put("LOCATION"); break;
	case WHAT_ACTOR: put("ACTOR"); break;
	case WHAT_ID: put("ID "); break;
	case WHAT_THIS: put("THIS"); break;
	default: put("*** ERROR ***"); break;
	}
	nl();
	put("id: "); dumpId(wht->id); out();
}


