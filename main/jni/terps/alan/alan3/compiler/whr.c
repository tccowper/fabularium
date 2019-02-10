/*----------------------------------------------------------------------*\

				WHR.C
			     Where Nodes

\*----------------------------------------------------------------------*/

#include "whr_x.h"

#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "wht_x.h"
#include "sym_x.h"
#include "id_x.h"
#include "cnt_x.h"
#include "exp_x.h"
#include "dump_x.h"

#include "lmList.h"
#include "acode.h"
#include "emit.h"



/*======================================================================*/
Where *newWhere(Srcp *srcp, Transitivity transitivity, WhereKind kind, Expression *what) {
    Where *new;

    progressCounter();

    new = NEW(Where);

    new->srcp = *srcp;
    new->transitivity = transitivity;
    new->kind = kind;
    new->what = what;

    return(new);
}


/*======================================================================*/
char *transitivityToString(Transitivity transitivity) {
    switch (transitivity) {
    case DEFAULT_TRANSITIVITY: return "Default";
    case TRANSITIVELY: return "Transitively";
    case DIRECTLY: return "Directly";
    case INDIRECTLY: return "Indirectly";
    }
    SYSERR("Unexpected transitivity", nulsrcp); return "ERROR";
}


/*======================================================================*/
void symbolizeWhere(Where *whr)
{
    if (whr == NULL) return;

    switch (whr->kind) {
    case WHERE_NEARBY:
    case WHERE_AT:
    case WHERE_IN:
        symbolizeExpression(whr->what);
        break;
    default:
        break;
    }
}


/*======================================================================*/
Bool verifyInitialLocation(Where *whr, Context *context)
{
    if (whr->transitivity != DEFAULT_TRANSITIVITY) {
        if (whr->transitivity == DIRECTLY)
            lmLogv(&whr->srcp, 422, sevWAR, transitivityToString(whr->transitivity),
                   "ignored for", "Initial location", NULL);
        else
            lmLogv(&whr->srcp, 422, sevERR, transitivityToString(whr->transitivity),
                   "not allowed for", "Initial location", NULL);
    }

    if (whr->what == NULL || whr->what->kind != WHAT_EXPRESSION)
        lmLogv(&whr->srcp, 355, sevERR, "", NULL);
    else
        switch (whr->kind) {
        case WHERE_AT:
            if (whr->what->fields.wht.wht->kind == WHAT_ID) {
                instanceCheck(whr->what->fields.wht.wht->id, "Initial location using AT", "location");
            } else {
                lmLog(&whr->srcp, 355, sevERR, "");
                return FALSE;
            }
            break;
        case WHERE_IN:
            verifyContainerForInitialLocation(whr->what->fields.wht.wht, context, "Expression after IN");
            break;
        default:
            lmLogv(&whr->srcp, 355, sevERR, "", NULL);
            return FALSE;
            break;
        }
    return TRUE;
}


/*======================================================================*/
void analyzeWhere(Where *whr, Context *context) {
    switch (whr->kind) {
    case WHERE_DEFAULT:
        break;
    case WHERE_HERE:
    case WHERE_NEARBY:
        if (context->kind == RULE_CONTEXT)
            lmLogv(&whr->srcp, 443, sevERR, "Rule context", "Here or Nearby", NULL);
        break;
    case WHERE_AT:
    case WHERE_NEAR:
        analyzeExpression(whr->what, context);
        if (whr->what->type != ERROR_TYPE && whr->what->type != INSTANCE_TYPE && whr->what->type != REFERENCE_TYPE)
            lmLogv(&whr->what->srcp, 428, sevERR, "Expression after AT", "an instance", NULL);
        break;
    case WHERE_IN:
        analyzeExpression(whr->what, context);
        if (whr->what->type != ERROR_TYPE) {
            if (whr->what->type == SET_TYPE)
                whr->kind = WHERE_INSET;
            else
                verifyContainerExpression(whr->what, context, "Expression after IN");
        }
        break;
    case WHERE_INSET:
        SYSERR("Unrecognized switch", whr->srcp);
        break;
    }
}


/*======================================================================

  Generate a location reference according to the WHR for initial locations.
  This means that it can only be an identifier. Can only be AT location or
  IN container.

  */
Aword generateInitialLocation(Properties *props)
{
    if (props->whr != NULL)
        switch (props->whr->kind) {
        case WHERE_IN:
        case WHERE_AT:
            return props->whr->what->fields.wht.wht->id->symbol->code;
        default: SYSERR("Unexpected Where kind as initial location", props->whr->srcp);
        }

    if (inheritsFrom(props->id->symbol, locationSymbol))
        return 0;
    else
        return 1; /* Anything not a location should be at #nowhere if undefined */
}


/*======================================================================*/
void generateTransitivity(Transitivity transitivity) {
    switch (transitivity) {
    case DEFAULT_TRANSITIVITY:
    case TRANSITIVELY: emitConstant(TRANSITIVE); break;
    case DIRECTLY: emitConstant(DIRECT); break;
    case INDIRECTLY: emitConstant(INDIRECT); break;
    default: SYSERR("Unexpected transitivity", nulsrcp);
    }
}


/*======================================================================*/
void generateWhere(Where *where)
{
    switch (where->kind) {

    case WHERE_AT:
        generateExpression(where->what);
        if (!inheritsFrom(where->what->class, locationSymbol)) {
            generateTransitivity(where->transitivity);
            emit0(I_WHERE);
        }
        break;

    case WHERE_IN:
    case WHERE_INSET:
        generateExpression(where->what);
        break;

    case WHERE_HERE:
        emitVariable(V_CURLOC);
        break;

    default:
        SYSERR("Unrecognised switch", where->srcp);
        break;
    }
}


/*======================================================================*/
char *whereKindToString(WhereKind kind) {
    switch (kind) {
    case WHERE_DEFAULT: return "Default"; break;
    case WHERE_HERE: return "Here"; break;
    case WHERE_NEARBY: return "Nearby"; break;
    case WHERE_NEAR: return "Near"; break;
    case WHERE_AT: return "At"; break;
    case WHERE_IN: return "In"; break;
    case WHERE_INSET: return "In Set"; break;
    }
    return "Unknown";
}


/*======================================================================*/
void dumpTransitivity(Transitivity transitivity) {
    put(transitivityToString(transitivity));
    put(" ");
}


/*======================================================================*/
void dumpWhere(Where *whr)
{
    if (whr == NULL) {
        put("NULL");
        return;
    }

    put("WHR: "); dumpSrcp(whr->srcp); indent();
    put("whr: "); dumpTransitivity(whr->transitivity);
    put(whereKindToString(whr->kind));
    nl();
    switch (whr->kind) {
    case WHERE_HERE:
    case WHERE_NEARBY:
        break;
    default:
        put("wht: "); dumpExpression(whr->what);
        break;
    }
    out();
}
