#ifndef _CONTEXT_H_
#define _CONTEXT_H_
/*----------------------------------------------------------------------*\

				CONTEXT.H
		     Execution Context Structure

\*----------------------------------------------------------------------*/

/* USE: */
#include "lst.h"
#include "evt.h"
#include "ins.h"
#include "cla.h"
#include "sym.h"
#include "exp.h"

/* TYPES: */

typedef enum {
    NULL_CONTEXT,
    INSTANCE_CONTEXT,
    CLASS_CONTEXT,
    EVENT_CONTEXT,
    VERB_CONTEXT,
    RULE_CONTEXT,
    START_CONTEXT
} ContextKind;


typedef struct Context {
    ContextKind kind;
    struct Context *previous;
    Symbol *verb;
    Instance *instance;
    Class *class;
    Event *event;
    Expression *classRestriction;
} Context;


#endif
