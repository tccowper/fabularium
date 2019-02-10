/*----------------------------------------------------------------------*\

  CONTEXT.C
  Context Nodes

\*----------------------------------------------------------------------*/

#include "context_x.h"
#include "string.h"
#include "util.h"
#include "sym_x.h"
#include "srcp_x.h"
#include "id_x.h"


/*----------------------------------------------------------------------*/
static Context *newContext(ContextKind kind, void *item)
{
    Context *new = NEW(Context);

    new->kind = kind;
    new->previous = NULL;

    switch (kind) {
    case INSTANCE_CONTEXT: new->instance = (Instance *)item; break;
    case CLASS_CONTEXT: new->class = (Class *)item; break;
    case VERB_CONTEXT: new->verb = (Symbol *)item; break;
    case EVENT_CONTEXT: new->event = (Event *)item; break;
    case START_CONTEXT:
    case RULE_CONTEXT:
    case NULL_CONTEXT:
        break;
    default:
        SYSERR("Unexpected Context kind", nulsrcp);
    }
    return new;
}

/*======================================================================*/
Context *newNullContext(void) {
    Context *new = newContext(NULL_CONTEXT, NULL);
    return new;
}

/*======================================================================*/
Context *newRuleContext(void) {
    Context *new = newContext(RULE_CONTEXT, NULL);
    return new;
}

/*======================================================================*/
Context *newStartContext(void) {
    Context *new = newContext(START_CONTEXT, NULL);
    return new;
}

/*======================================================================*/
Context *newEventContext(void) {
    Context *new = newContext(EVENT_CONTEXT, NULL);
    return new;
}

/*======================================================================*/
Context *newClassContext(Class *class) {
    Context *new = newContext(CLASS_CONTEXT, NULL);
    new->class = class;
    return new;
}

/*======================================================================*/
Context *newVerbContext(Symbol *theVerb) {
    Context *new = newContext(VERB_CONTEXT, NULL);
    new->verb = theVerb;
    return new;
}

/*======================================================================*/
Context *newInstanceContext(Instance *theInstance) {
    Context *new = newContext(INSTANCE_CONTEXT, NULL);
    new->instance = theInstance;
    return new;
}

/*======================================================================*/
Symbol *symbolOfContext(Context *context) {
    switch (context->kind) {
    case INSTANCE_CONTEXT: return context->instance->props->id->symbol;
    case CLASS_CONTEXT: return context->class->props->id->symbol;
    case VERB_CONTEXT: return symbolOfContext(context->previous);
    case EVENT_CONTEXT:
    case RULE_CONTEXT:
    case NULL_CONTEXT:
        return NULL;
    default:
        SYSERR("Unexpected Context kind", nulsrcp);
    }
    return NULL;
}

/*======================================================================*/
Symbol *classOfIdInContext(Context *context, Id *id) {
    Symbol *restrictedTo = contextRestrictsIdTo(context, id);
    if (restrictedTo == NULL)
        return classOfSymbol(id->symbol);
    else
        return restrictedTo;
}


/*----------------------------------------------------------------------*/
static Context *duplicateContext(Context *previous) {
    Context *new = NEW(Context);

    if (previous) {
        memcpy(new, previous, sizeof(Context));
        new->classRestriction = NULL;
    }
    return new;
}


/*======================================================================*/
Context *pushContext(Context *previous)
{
    Context *new = duplicateContext(previous);

    new->previous = previous;
    return new;
}


/*======================================================================*/
Bool inEntityContext(Context *context)
{
    return (context->instance != NULL || context->class != NULL);
}


/*======================================================================*/
Bool inLocationContext(Context *context)
{
    return (context->instance != NULL
            && inheritsFrom(context->instance->props->id->symbol, locationSymbol))
        || (context->class != NULL
            && inheritsFrom(context->class->props->id->symbol, locationSymbol));
}


/*======================================================================*/
Id *classIdInContext(Context *context)
{
    Id *classId = NULL;

    switch (context->kind) {
    case INSTANCE_CONTEXT:
        if (context->instance->props->parentId)
            classId = context->instance->props->parentId;
        break;
    case CLASS_CONTEXT:
        classId = context->class->props->id;
        break;
    case VERB_CONTEXT:
        if (context->instance)
            classId = context->instance->props->parentId;
        else if (context->class)
            classId = context->class->props->id;
        else
            SYSERR("No instance or class in context", nulsrcp);
        break;
    default:
        break;
    }

    return(classId);
}

/*======================================================================*/
Symbol *contextRestrictsIdTo(Context *initialContext, Id *id) {
    Context *currentContext = initialContext;

    while (currentContext != NULL) {
        if (currentContext->classRestriction != NULL) {
            if (currentContext->classRestriction->kind != ISA_EXPRESSION)
                SYSERR("Wrong kind of expression in context restriction", id->srcp);
            if (equalId(id, currentContext->classRestriction->fields.isa.what->fields.wht.wht->id))
                return currentContext->classRestriction->fields.isa.class->symbol;
        }
        currentContext = currentContext->previous;
    }
    return NULL;
}

/*======================================================================*/
void addRestrictionInContext(Context *context, Expression *exp) {
    if (exp->kind != ISA_EXPRESSION)
        SYSERR("Wrong kind of expression in context restriction", exp->srcp);
    context->classRestriction = exp;
}
