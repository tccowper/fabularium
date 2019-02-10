/*----------------------------------------------------------------------*\

  SYM.C
  Symbol Table Nodes

\*----------------------------------------------------------------------*/

#include "sym_x.h"

/* IMPORTS */
#include "sysdep.h"
#include "util.h"
#include "lmList.h"

#include "srcp_x.h"
#include "cla_x.h"
#include "elm_x.h"
#include "cnt_x.h"
#include "id_x.h"
#include "atr_x.h"
#include "exp_x.h"
#include "ext_x.h"
#include "lst_x.h"
#include "type_x.h"
#include "prop_x.h"
#include "dump_x.h"
#include "emit.h"
#include "options.h"

/* EXPORTS: */
int frameLevel = 0;

int classCount = 0;
int instanceCount = 0;
int directionCount = 0;
int attributeCount;
int verbCount = 0;
int eventCount = 0;

Symbol *entitySymbol;
Symbol *thingSymbol;
Symbol *objectSymbol;
Symbol *locationSymbol;
Symbol *actorSymbol;
Symbol *literalSymbol;
Symbol *stringSymbol;
Symbol *integerSymbol;
Symbol *theHero;
Symbol *nowhere;
Symbol *messageVerbSymbolForInstance;
Symbol *messageVerbSymbolFor2Instances;
Symbol *messageVerbSymbolForString;
Symbol *messageVerbSymbolFor2Strings;
Symbol *messageVerbSymbolFor2Integers;
Symbol *messageVerbSymbolFor3Integers;



/* PRIVATE: */
static Symbol *symbolTree = NULL;
static Bool firstSymbolDumped = TRUE;

typedef struct Frame {
    /* A frame defines a local scope with local variables.
       To find a variable you need to do linear search in the list.
       Since frames may be nested you should recurse until outerFrame == NULL */
    int level;
    List *localSymbols;
    struct Frame *outerFrame;
} Frame;

static Frame *currentFrame = NULL;


typedef struct SymbolIteratorState {
    Symbol *symbol;
    int done;                      /* 0 = nothing, so start with this node it self */
                                   /* 1 = have done this node so go down lower branch */
                                   /* 2 = have gone down the lower branch, so go down higher branch */
                                   /* 3 = have gone down the higher branch, so pop */
} SymbolIteratorState;

typedef struct SymbolIteratorStruct {
    size_t size;
    size_t stackP;
    SymbolIteratorState *stack;
} SymbolIteratorStruct;


/*======================================================================*/
void idRedefined(Id *id, Symbol *sym, Srcp previousDefinition)
{
    int error_code = 0;

    switch (sym->kind) {
    case DIRECTION_SYMBOL: error_code = 301; break;
    case VERB_SYMBOL: error_code = 303; break;
    case INSTANCE_SYMBOL: error_code = 304; break;
    case CLASS_SYMBOL: error_code = 305; break;
    case EVENT_SYMBOL: error_code = 307; break;
    default: error_code = 308; break;
    }

    lmLog(&id->srcp, error_code, sevERR, id->string);
    lmLog(&previousDefinition, 399, sevINF, id->string);
}



/*----------------------------------------------------------------------*/
static void insertSymbol(Symbol *symbol)
{
    Symbol *s1,*s2;             /* Traversal pointers */
    int comp = 0;               /* Result of comparison */

    symbol->lower = NULL;
    symbol->higher = NULL;

    s1 = symbolTree;
    s2 = NULL;

    while (s1 != NULL) {
        s2 = s1;
        comp = compareStrings(symbol->string, s1->string);
        if (comp < 0)
            s1 = s1->lower;
        else
            s1 = s1->higher;
    }

    if (s2 == NULL)
        symbolTree = symbol;
    else if(comp < 0)
        s2->lower = symbol;
    else
        s2->higher = symbol;
}


/*----------------------------------------------------------------------*/
static void addLocal(Symbol *new)
{
    if (currentFrame == NULL)
        SYSERR("Adding local variable without an active frame", nulsrcp);

    if (currentFrame->localSymbols == NULL)
        new->fields.local.number = 1;
    else
        new->fields.local.number = currentFrame->localSymbols->member.sym->fields.local.number + 1;

    new->fields.local.level = currentFrame->level;

    currentFrame->localSymbols = concat(currentFrame->localSymbols, new, SYMBOL_LIST);
}


/*----------------------------------------------------------------------*/
static void anotherSymbolKindAsString(SymbolKind kind, Bool found, char *string)
{
    if (found) strcat(string, " or ");
    switch(kind) {
    case CLASS_SYMBOL: strcat(string, "a Class"); break;
    case INSTANCE_SYMBOL:strcat(string, "an Instance"); break;
    case VERB_SYMBOL: strcat(string, "a Verb"); break;
    case EVENT_SYMBOL: strcat(string, "an Event"); break;
    case LOCAL_SYMBOL:
    case DIRECTION_SYMBOL:
    case PARAMETER_SYMBOL:
    case FUNCTION_SYMBOL:
    case ERROR_SYMBOL:
    case MAX_SYMBOL:
        SYSERR("Unimplemented case", nulsrcp);
    }
}


/*----------------------------------------------------------------------*/
static char *symbolKindsAsString(SymbolKind kinds)
{
    Bool found = FALSE;
    char *string = allocate(100);
    int i;

    string[0] = '\0';
    for (i = 1; i <= MAX_SYMBOL; i = i<<1) {
        if (kinds&i) {
            anotherSymbolKindAsString(kinds&i, found, string);
            found = TRUE;
        }
    }
    return string;
}



/*======================================================================*/
Symbol *newParameterSymbol(Element *element)
{
    Symbol *new;                  /* The newly created symnod */

    new = NEW(Symbol);

    new->kind = PARAMETER_SYMBOL;
    new->string = element->id->string;
    new->fields.parameter.element = element;
    element->id->symbol = new;
    element->id->symbol->code = element->id->code;

    return new;
}


/*----------------------------------------------------------------------*/
static Bool mayOverride(SymbolKind overridingKind, SymbolKind originalKind) {
    switch (overridingKind) {
    case LOCAL_SYMBOL:
        switch (originalKind) {
        case DIRECTION_SYMBOL:
        case VERB_SYMBOL:
            return TRUE;
        default:
            return FALSE;
        }
    default:
        return FALSE;
    }
}


/*======================================================================*/
Symbol *newSymbol(Id *id, SymbolKind kind)
{
    Symbol *new;                  /* The newly created symnod */

    if (id == NULL)
        return NULL;

    new = lookup(id->string);
    if (new != NULL && !mayOverride(kind, new->kind))
        idRedefined(id, new, new->srcp);

    new = NEW(Symbol);

    new->kind = kind;
    new->string = id->string;
    new->srcp = id->srcp;

    if (kind == LOCAL_SYMBOL)
        addLocal(new);
    else
        insertSymbol(new);

    switch (kind) {
    case CLASS_SYMBOL:
        new->code = ++classCount;
        new->fields.entity.parent = NULL;
        new->fields.entity.attributesNumbered = FALSE;
        new->fields.entity.replicated = FALSE;
        break;
    case INSTANCE_SYMBOL:
        new->code = ++instanceCount;
        new->fields.entity.parent = NULL;
        new->fields.entity.attributesNumbered = FALSE;
        new->fields.entity.replicated = FALSE;
        break;
    case DIRECTION_SYMBOL:
        new->code = ++directionCount;
        break;
    case VERB_SYMBOL:
        new->code = ++verbCount;
        break;
    case EVENT_SYMBOL:
        new->code = ++eventCount;
        break;
    case LOCAL_SYMBOL:
    case ERROR_SYMBOL:
        break;
    default:
        SYSERR("Unexpected switch on SYMBOLKIND", id->srcp);
    }

    return new;
}

/*======================================================================*/
Symbol *newInstanceSymbol(Id *id, Properties *props, Symbol *parent) {
    Symbol *new = newSymbol(id, INSTANCE_SYMBOL);
    new->fields.entity.props = props;
    new->fields.entity.parent = parent;
    return new;
}

/*======================================================================*/
Symbol *newClassSymbol(Id *id, Properties *props, Symbol *parent) {
    Symbol *new = newSymbol(id, CLASS_SYMBOL);
    new->fields.entity.props = props;
    new->fields.entity.parent = parent;
    return new;
}


/*======================================================================*/
Symbol *newVerbSymbol(Id *id) {
    Symbol *new = newSymbol(id, VERB_SYMBOL);
    new->fields.verb.parameterSymbols = NULL;
    new->fields.verb.firstSyntax = NULL;
    return new;
}

/*======================================================================*/
TypeKind basicTypeFromClassSymbol(Symbol *class) {
    if (class == stringSymbol)
        return STRING_TYPE;
    else if (class == integerSymbol)
        return INTEGER_TYPE;
    else
        return INSTANCE_TYPE;
}

/*======================================================================*/
TypeKind typeOfSymbol(Symbol *symbol) {
    switch (symbol->kind) {
    case LOCAL_SYMBOL: return symbol->fields.local.type;
    case PARAMETER_SYMBOL: return symbol->fields.parameter.type;
    default: SYSERR("Unexpected symbol->kind", nulsrcp); return ERROR_TYPE;
    }
}

/*----------------------------------------------------------------------*/
static void setParameterClass(Symbol *s, int parameter, Symbol *class) {
    List *pl = s->fields.verb.parameterSymbols;
    int p;

    for (p = 1; p < parameter; p++)
        pl = pl->next;

    pl->member.sym->fields.parameter.class = class;
	pl->member.sym->fields.parameter.type = basicTypeFromClassSymbol(class);
}


/*----------------------------------------------------------------------*/
static Symbol *createMessageVerb(int parameterCount, Symbol *typeSymbol) {
    Symbol *symbol;
    char name[50];
    int p;
    Id *id;
    List *parameterList = NULL;

    sprintf(name, "$message%d%s$", parameterCount, typeSymbol->string);
    symbol = newVerbSymbol(newId(nulsrcp, name));

    for (p = 1; p <= parameterCount; p++) {
        sprintf(name, "parameter%d", p);
        id = newId(nulsrcp, name);
        parameterList = concat(parameterList,
                               newParameterElement(nulsrcp, id, 0),
                               ELEMENT_LIST);
        id->code = p;
    }

    setParameters(symbol, parameterList);

    for (p = 1; p <= parameterCount; p++)
        setParameterClass(symbol, p, typeSymbol);
    return(symbol);
}



/*======================================================================*/
void createMessageVerbs() {
    messageVerbSymbolForInstance = createMessageVerb(1, entitySymbol);
    messageVerbSymbolForString = createMessageVerb(1, stringSymbol);
    messageVerbSymbolFor2Integers = createMessageVerb(2, integerSymbol);
    messageVerbSymbolFor3Integers = createMessageVerb(3, integerSymbol);
    messageVerbSymbolFor2Strings = createMessageVerb(2, stringSymbol);
    messageVerbSymbolFor2Instances = createMessageVerb(2, entitySymbol);
}


/*======================================================================*/
void initSymbols()
{
    symbolTree = NULL;
    instanceCount = 0;
    classCount = 0;
    attributeCount = PREDEFINEDATTRIBUTES; /* Set number of attributes
                                              predefined so counting
                                              starts at next number */
}


/*======================================================================*/
void newFrame(void)
{
    Frame *theNew = NEW(Frame);

    theNew->localSymbols = NULL;
    if (currentFrame == NULL)
        theNew->level = 1;
    else
        theNew->level = currentFrame->level + 1;

    theNew->outerFrame = currentFrame;
    currentFrame = theNew;
}

/*======================================================================*/
void deleteFrame(void)
{
    Frame *outerFrame = currentFrame->outerFrame;
    List *locals, *next;

    for (locals = currentFrame->localSymbols; locals != NULL; locals = next) {
        next = locals->next;
        free(locals);
    }

    free(currentFrame);
    currentFrame = outerFrame;
}


/*----------------------------------------------------------------------*/
static Symbol *lookupInParameterList(char *idString, List *parameterSymbols)
{
    List *l;

    for (l = parameterSymbols; l != NULL; l = l->next)
        if (compareStrings(idString, l->member.sym->fields.parameter.element->id->string) == 0)
            return l->member.sym;
    return NULL;
}


/*======================================================================*/
Symbol *lookupParameter(Id *parameterId, List *parameterSymbols)
{
    List *p;

    for (p = parameterSymbols; p != NULL; p = p->next) {
        if (p->member.sym->kind == PARAMETER_SYMBOL)
            if (equalId(parameterId, p->member.sym->fields.parameter.element->id))
                return p->member.sym;
    }
    return NULL;
}


/*======================================================================*/
Symbol *lookup(char *idString)
{
    Symbol *s1;                   /* Traversal pointer */
    int comp;                     /* Result of comparison */

    if (idString == NULL) SYSERR("NULL string", nulsrcp);

    s1 = symbolTree;

    while (s1 != NULL) {
        comp = compareStrings(idString, s1->string);
        if (comp == 0)
            return s1;
        else if (comp < 0)
            s1 = s1->lower;
        else
            s1 = s1->higher;
    }

    return(NULL);
}


/*----------------------------------------------------------------------*/
static Symbol *lookupInFrames(char *idString)
{
    Frame *thisFrame = currentFrame;
    List *localSymbolList;

    while (thisFrame != NULL) {
        for (localSymbolList = thisFrame->localSymbols; localSymbolList != NULL; localSymbolList = localSymbolList->next) {
            if (compareStrings(idString, localSymbolList->member.sym->string) == 0)
                return localSymbolList->member.sym;
        }
        thisFrame = thisFrame->outerFrame;
    }
    return NULL;
}


/*----------------------------------------------------------------------*/
static Symbol *lookupInContext(char *idString, Context *context)
{
    Symbol *foundSymbol = NULL;

    if ((foundSymbol = lookupInFrames(idString)) != NULL)
        return foundSymbol;

    if (context != NULL) {
        switch (context->kind){
        case VERB_CONTEXT:
            if (context->verb != NULL)
                foundSymbol = lookupInParameterList(idString, context->verb->fields.verb.parameterSymbols);
            break;
        case EVENT_CONTEXT:
        case RULE_CONTEXT:
        case START_CONTEXT:
        case CLASS_CONTEXT:
        case INSTANCE_CONTEXT:
        case NULL_CONTEXT:
            foundSymbol = lookup(idString);
            break;
        default:
            SYSERR("Unexpected context kind", nulsrcp);
            break;
        }
        if (foundSymbol != NULL)
            return foundSymbol;
    }

    return lookup(idString);
}


/*----------------------------------------------------------------------*/
static Bool isEntity(Symbol *s){
    return s->kind == CLASS_SYMBOL || s->kind == INSTANCE_SYMBOL;
}

/*----------------------------------------------------------------------*/
static Bool hasParent(Symbol *s) {
    if (!isEntity(s)) SYSERR("Wrong kind of symbol", nulsrcp);
    return s->fields.entity.parent != NULL;
}

/*----------------------------------------------------------------------*/
static Properties *propertiesOf(Symbol *s) {
    if (!s) return NULL;
    if (!isEntity(s)) SYSERR("Wrong kind of symbol", nulsrcp);
    return s->fields.entity.props;
}


/*======================================================================*/
Script *lookupScript(Symbol *theSymbol, Id *scriptName)
{
    List *scripts;

    while (theSymbol != NULL) {
        switch (theSymbol->kind) {
        case INSTANCE_SYMBOL:
        case CLASS_SYMBOL:
            scripts = propertiesOf(theSymbol)->scripts;
            break;
        case PARAMETER_SYMBOL:
            theSymbol = theSymbol->fields.parameter.class;
            scripts = propertiesOf(theSymbol)->scripts;
            break;
        case LOCAL_SYMBOL:
            theSymbol = theSymbol->fields.local.class;
            scripts = propertiesOf(theSymbol)->scripts;
            break;
        default:
            SYSERR("Unexpected symbol kind", nulsrcp);
            return NULL;
        }
        while (scripts != NULL) {
            if (equalId(scriptName, scripts->member.script->id))
                return scripts->member.script;
            scripts = scripts->next;
        }
        theSymbol = parentOf(theSymbol);
    }

    return NULL;
}


/*======================================================================*/
Symbol *classOfSymbol(Symbol *symbol) {
    switch (symbol->kind) {
    case PARAMETER_SYMBOL: return symbol->fields.parameter.class;
    case LOCAL_SYMBOL: return symbol->fields.local.class;
    case INSTANCE_SYMBOL: return symbol->fields.entity.parent;
    case CLASS_SYMBOL: return symbol;
    default: SYSERR("Unexpected symbol kind", nulsrcp); return NULL;
    }
}


/*======================================================================*/
Bool isClass(Symbol *symbol) {
    return symbol->kind == CLASS_SYMBOL;
}


/*======================================================================*/
Bool isInstance(Symbol *symbol) {
    return symbol->kind == INSTANCE_SYMBOL;
}


/*======================================================================*/
TypeKind classToType(Symbol* symbol) {
    if (!isClass(symbol))
        SYSERR("Not a class", nulsrcp);
    if (symbol == integerSymbol) return INTEGER_TYPE;
    else if (symbol == stringSymbol) return STRING_TYPE;
    else return INSTANCE_TYPE;
}


/*======================================================================*/
Bool symbolIsContainer(Symbol *symbol) {
    if (symbol != NULL) {
        switch (symbol->kind) {
        case CLASS_SYMBOL:
        case INSTANCE_SYMBOL:
            if (propertiesOf(symbol) != NULL)
                return propertiesOf(symbol)->container != NULL
                    || symbolIsContainer(symbol->fields.entity.parent);
            else
                return FALSE;
        case PARAMETER_SYMBOL:
            return symbol->fields.parameter.restrictedToContainer
                || symbolIsContainer(symbol->fields.parameter.class);
        case LOCAL_SYMBOL:
            return symbolIsContainer(symbol->fields.local.class);
        case VERB_SYMBOL:
            /* Probably an error recovery or duplicate declaration error */
            return FALSE;
        default:
            SYSERR("Unexpected Symbol kind", nulsrcp);
        }
    }
    return FALSE;
}


/*======================================================================*/
Bool symbolIsActor(Symbol *symbol) {
    if (symbol != NULL)
        switch(symbol->kind) {
        case CLASS_SYMBOL:
        case INSTANCE_SYMBOL:
            return symbol == actorSymbol || symbolIsActor(symbol->fields.entity.parent);
        default:
            return FALSE;
        }
    return FALSE;
}


/*======================================================================*/
SymbolIterator createSymbolIterator(void) {
    SymbolIterator iterator = allocate(sizeof(SymbolIteratorStruct));
    iterator->size = 1000;
    iterator->stack = allocate(1000*sizeof(SymbolIteratorState));
    SymbolIteratorState *state = &iterator->stack[iterator->stackP];
    state->symbol = symbolTree;
    state->done = 0;
    return iterator;
}


/*----------------------------------------------------------------------*/
static Symbol *pushSymbolIterator(SymbolIterator iterator, Symbol *parent, Symbol *symbol) {
    SymbolIteratorState *state = &iterator->stack[iterator->stackP];
    state->done = 0;
    state->symbol = symbol;
    return getNextInstanceOf(iterator, parent);
}


/*======================================================================*/
Symbol *getNextInstanceOf(SymbolIterator iterator, Symbol *parent) {
    if (iterator == NULL || iterator->stack == NULL || iterator->stack[iterator->stackP].symbol == NULL)
        SYSERR("Illegal SymbolIterator", nulsrcp);

 pop: {
        SymbolIteratorState *state = &iterator->stack[iterator->stackP];
        Symbol *symbol = state->symbol;
        switch (state->done) {
        case 0: {
            state->done = 1;
            if (isInstance(symbol) && inheritsFrom(symbol, parent)) {
                return symbol;
            } else
                /* Fallthrough! */;
        }
        case 1: {
            state->done = 2;
            iterator->stackP++;
            if (symbol->lower)
                return pushSymbolIterator(iterator, parent, symbol->lower);
            else
                /* Fallthrough! */;
        }
        case 2: {
            state->done = 3;
            if (symbol->higher)
                return pushSymbolIterator(iterator, parent, symbol->higher);
            else
                /* Fallthrough! */;
        }
        case 3:
            if (iterator->stackP != 0) {
                --iterator->stackP;
                goto pop;
            }
            /* Fallthrough! Done! */
        }
    }
    return NULL;
}


/*======================================================================*/
void destroyIterator(SymbolIterator iterator) {
    deallocate(iterator->stack);
    deallocate(iterator);
}


/*----------------------------------------------------------------------*/
static Bool recurseTreeForInstanceOf(Symbol *current, Symbol *theClass) {
    if (current) {
        if (isInstance(current) && inheritsFrom(current, theClass))
            return TRUE;
        else
            return recurseTreeForInstanceOf(current->higher, theClass)
                || recurseTreeForInstanceOf(current->lower, theClass);
    }
    return FALSE;
}



/*======================================================================*/
Bool instancesExist(Symbol *theClass) {
    return recurseTreeForInstanceOf(symbolTree, theClass);
}


/*----------------------------------------------------------------------*/
static Symbol *most_general_class(Symbol *s1, Symbol *s2) {
    if (!s1) return s2;
    if (!s2) return s1;
    if (inheritsFrom(s1, s2))
        return s2;
    if (inheritsFrom(s2, s1))
        return s1;
    return commonParent(s1, s2);
}



/*======================================================================*/
/* NOTE difference between "takes" and "may contain" (transitively)! */
Symbol *containerSymbolTakes(Symbol *symbol) {
    Properties *props;
    if (symbol != NULL) {
        switch (symbol->kind) {
        case INSTANCE_SYMBOL:
        case CLASS_SYMBOL:
            props = propertiesOf(symbol);
            if (props != NULL) {
                if (props->container != NULL)
                    return props->container->body->taking->symbol;
                else
                    return containerSymbolTakes(parentOf(symbol));
            }
            break;
        case PARAMETER_SYMBOL:
            return containerSymbolTakes(symbol->fields.parameter.class);
            break;
        case LOCAL_SYMBOL:
            return containerSymbolTakes(symbol->fields.local.class);
            break;
        case ERROR_SYMBOL:
            break;
        default:
            SYSERR("Unexpected Symbol kind", nulsrcp);
        }
    }
    return NULL;
}


/*----------------------------------------------------------------------*/
static Bool symbolHasContainerProperties(Symbol *this) {
    Properties *props = propertiesOf(this);
    return props && props->container && props->container->body;
}


/* #define DEBUG_CONTAINER_CONTENT */
#ifdef DEBUG_CONTAINER_CONTENT
static char prefix[1000] = "";
#endif

/*----------------------------------------------------------------------*/
static Symbol *recurseContainersForContent(Symbol *this) {
    if (symbolHasContainerProperties(this)) {
        ContainerBody *body = propertiesOf(this)->container->body;
        if (body->visited)
            return body->mayContain;

        Symbol *taken_class = containerSymbolTakes(this);
        Symbol *most_general = taken_class;

        /* Now, remember that we've seen this to terminate loops */
        body->visited = TRUE;
        if (symbolHasContainerProperties(taken_class)) {
#ifdef DEBUG_CONTAINER_CONTENT
            printf("%s%s - container: %s (%s)\n", prefix, this->string, taken_class->string, most_general->string);
            strcpy(&prefix[strlen(prefix)], " ");
#endif
            most_general = most_general_class(most_general,
                                              recurseContainersForContent(taken_class));
#ifdef DEBUG_CONTAINER_CONTENT
            prefix[strlen(prefix)-1] = '\0';
            printf("%s%s - getting: %s\n", prefix, this->string, most_general->string);
#endif
        }
        if (instancesExist(taken_class)) {
            SymbolIterator iterator = createSymbolIterator();
            Symbol *instance = getNextInstanceOf(iterator, taken_class);
            while (instance) {
                if (instance != this) {
#ifdef DEBUG_CONTAINER_CONTENT
                    printf("%s%s - instance: %s (%s)\n", prefix, this->string, instance->string, most_general->string);
                    strcpy(&prefix[strlen(prefix)], " ");
#endif
                    most_general = most_general_class(most_general,
                                                      recurseContainersForContent(instance));
#ifdef DEBUG_CONTAINER_CONTENT
                    prefix[strlen(prefix)-1] = '\0';
                    printf("%s%s - getting: %s\n", prefix, this->string, most_general->string);
#endif
                }
                instance = getNextInstanceOf(iterator, taken_class);
            }
            destroyIterator(iterator);
        }
#ifdef DEBUG_CONTAINER_CONTENT
        printf("%s%s - deciding: %s\n", prefix, this->string, most_general->string);
#endif
        body->mayContain = most_general;
        return most_general;
    } else
        return NULL;
}


/*----------------------------------------------------------------------*/
static void traversSymbolsForContainerContents(Symbol *this) {
    if (this) {
        traversSymbolsForContainerContents(this->lower);
        if (isClass(this) || isInstance(this)) {
            if (symbolHasContainerProperties(this))
                propertiesOf(this)->container->body->mayContain = recurseContainersForContent(this);
        }
        traversSymbolsForContainerContents(this->higher);
    }
}


/*======================================================================*/
void calculateTransitiveContainerContents(void) {
    traversSymbolsForContainerContents(symbolTree);
}


/*======================================================================*/
Symbol *containerMightContain(Symbol *symbol) {
    if (symbol) {
        switch (symbol->kind) {
        case CLASS_SYMBOL:
        case INSTANCE_SYMBOL:
            if (symbolHasContainerProperties(symbol))
                return propertiesOf(symbol)->container->body->mayContain;
            else if (hasParent(symbol))
                return containerMightContain(parentOf(symbol));
            return NULL;
        case PARAMETER_SYMBOL:
            return containerMightContain(symbol->fields.parameter.class);
        default:
            SYSERR("Unexpected type of symbol", nulsrcp);
        }
    }
    return NULL;
}


/*======================================================================*/
void setParent(Symbol *child, Symbol *parent)
{
    if (!isClass(child) && !isInstance(child))
        SYSERR("Not a CLASS or INSTANCE", nulsrcp);
    child->fields.entity.parent = parent;
}


/*======================================================================*/
Symbol *parentOf(Symbol *child)
{
    if (!isClass(child) && !isInstance(child))
        SYSERR("Not a CLASS or INSTANCE", nulsrcp);
    return child->fields.entity.parent;
}


/*======================================================================*/
Bool inheritsFrom(Symbol *child, Symbol *ancestor)
{
    Symbol *p;

    if (child == NULL || ancestor == NULL) return FALSE;

    if (isInstance(ancestor))
        SYSERR("Can not inherit from an instance", nulsrcp);

    if (child->kind == ERROR_SYMBOL || ancestor->kind == ERROR_SYMBOL)
        return TRUE;

    if (child->kind == PARAMETER_SYMBOL)
        child = child->fields.parameter.class;

    if ((!isClass(child) && !isInstance(child)) || !isClass(ancestor))
        return FALSE;           /* Probably spurious */

    p = child;                  /* To be the class itself is OK */
    while (p && p != ancestor)
        p = parentOf(p);

    return (p != NULL);
}


/*======================================================================*/
Symbol *commonParent(Symbol *symbol1, Symbol *symbol2)
{
    Symbol *class1 = classOfSymbol(symbol1);
    Symbol *class2 = classOfSymbol(symbol2);

    if (class1 == class2)
        return class1;
    else if (inheritsFrom(symbol1, symbol2))
        return symbol2;
    else if (inheritsFrom(symbol2, symbol1))
        return symbol1;
    else
        return commonParent(parentOf(symbol1), parentOf(symbol2));
}


/*----------------------------------------------------------------------*/
static Bool multipleSymbolKinds(SymbolKind kind) {
    int i;
    Bool found = FALSE;

    for (i = 1; i < MAX_SYMBOL; i=i<<1)
        if (kind&i) {
            if (found)
                return TRUE;
            else
                found = TRUE;
        }
    return FALSE;
}


/*----------------------------------------------------------------------*/
static Symbol *lookupClass(Id *id, Symbol *symbol) {
    if (symbol != NULL && !isClass(symbol)) {
        Symbol *otherSymbol = lookup(id->string);
        if (otherSymbol != NULL)
            return otherSymbol;
    }
    return symbol;
}


/*----------------------------------------------------------------------*/
static List *getParameterSymbols(Context *context)
{
    if (context->kind == VERB_CONTEXT)
        return context->verb->fields.verb.parameterSymbols;
    else
        return NULL;
}


/*----------------------------------------------------------------------*/
static char *identifierListForParameters(Context *context) {
    List *parameters = getParameterSymbols(context);
    char *identifiers = (char *)allocate(200);
    List *list;
    Bool first = TRUE;

    if (parameters == NULL)
        SYSERR("NULL parameters", nulsrcp);

    ITERATE(list, parameters) {
        if (!first) {
            if (list->next != NULL)
                strcat(identifiers, "', '");
            else
                strcat(identifiers, "' and '");
        } else
            strcat(identifiers, "'");
        strcat(identifiers, list->member.sym->string);
        first = FALSE;
    }
    strcat(identifiers, "'");
    return identifiers;
}


/*======================================================================*/
void setParameters(Symbol *verb, List *parameters)
{
    /* Parameters are sent as a list of Elments. Set it in the verb symbol. */
    List *parameterSymbols = NULL;
    List *param;

    if (verb == NULL) return;

    if (verb->kind != VERB_SYMBOL) {
        /* Probably a syntactic error! */
        return;
    }

    if (parameters == NULL) return;

    if (parameters->kind != ELEMENT_LIST)
        SYSERR("Not a parameter list", nulsrcp);

    ITERATE(param, parameters) {
        Symbol *parameterSymbol = newParameterSymbol(param->member.elm);
        parameterSymbols = concat(parameterSymbols, parameterSymbol, SYMBOL_LIST);
    }

    verb->fields.verb.parameterSymbols = parameterSymbols;
}


/*======================================================================*/
char *verbHasParametersMessage(Context *context) {
    static char message[2000];
    message[0] = '\0';
    if (context && context->kind == VERB_CONTEXT) {
        List *parameterSymbols = getParameterSymbols(context);
        if (length(parameterSymbols) > 0)
            sprintf(message, " The verb '%s' has the parameter%s %s.",
                    context->verb->string, length(parameterSymbols)>1?"s":"",
                    identifierListForParameters(context));
    }
    return message;
}


/*======================================================================*/
char *verbHasParametersOrNoneMessage(Context *context) {
    char *message = verbHasParametersMessage(context);
    if (strlen(message) > 0)
        return message;
    else {
        static char noParametersMessage[2000];
        sprintf(noParametersMessage, " The verb '%s' has no parameters.", context->verb->string);
        return noParametersMessage;
    }
}


/*======================================================================*/
Symbol *symcheck(Id *id, SymbolKind requestedKinds, Context *context)
{
    Symbol *sym;

    sym = lookupInContext(id->string, context);
    if (requestedKinds == CLASS_SYMBOL)
        sym = lookupClass(id, sym);

    if (!sym) {
        if (!isGeneratedId(id)) {
            lmLogv(&id->srcp, 310, sevERR, id->string, verbHasParametersMessage(context), NULL);
        }
    } else if (sym->kind == PARAMETER_SYMBOL || sym->kind == LOCAL_SYMBOL) {
        if ((requestedKinds&INSTANCE_SYMBOL) == 0) {
            if (multipleSymbolKinds(requestedKinds))
                lmLogv(&id->srcp, 319, sevERR, id->string, "of correct type for this context", NULL);
            else
                lmLogv(&id->srcp, 319, sevERR, id->string, symbolKindsAsString(requestedKinds), NULL);
            return NULL;
        }
    } else
        if (requestedKinds != 0)
            if (sym->kind != ERROR_SYMBOL && (sym->kind&requestedKinds) == 0) {
                if (multipleSymbolKinds(requestedKinds))
                    lmLogv(&id->srcp, 319, sevERR, id->string, "of correct type for this context", NULL);
                else
                    lmLogv(&id->srcp, 319, sevERR, id->string, symbolKindsAsString(requestedKinds), NULL);
                return NULL;
            }
    id->symbol = sym;
    return sym;
}


/*======================================================================*/
void inheritCheck(Id *id, char reference[], char toWhat[], char className[])
{
    /* Check that the given identifier inherits the class passed as a string.
       This will only be used for built in class checks (location, actor etc.)
    */

    Symbol *theClassSymbol = lookup(className);

    if (theClassSymbol == NULL) SYSERR("There is no such class", id->srcp);

    if (id->symbol != NULL && !inheritsFrom(id->symbol, theClassSymbol))
        lmLogv(&id->srcp, 351, sevERR, reference, toWhat, className, NULL);
}


/*======================================================================*/
void instanceCheck(Id *id, char reference[], char className[])
{
    /* Check that the given identifier inherits the class passed as a string.
       This will only be used for built in class checks (location, actor etc.)
    */

    Symbol *theClassSymbol = lookup(className);

    if (theClassSymbol == NULL) SYSERR("There is no such class", id->srcp);

    if (id->symbol != NULL)
        if (id->symbol->kind != ERROR_SYMBOL)
            if (!isInstance(id->symbol) || !inheritsFrom(id->symbol, theClassSymbol))
                lmLogv(&id->srcp, 351, sevERR, reference, "an instance", className, NULL);
}


/*======================================================================*/
Symbol *definingSymbolOfAttribute(Symbol *symbol, Id *id)
{
    /* Find the symbol which defines an attribute by traversing its parents. */

    Attribute *foundAttribute;

    if (symbol == NULL)
        return NULL;

    if (!isClass(symbol) && !isInstance(symbol))
        return NULL;

    if ((foundAttribute = findAttribute(propertiesOf(symbol)->attributes, id)) == NULL)
        return definingSymbolOfAttribute(parentOf(symbol), id);
    else
        return symbol;
}



/*======================================================================*/
Attribute *findInheritedAttribute(Symbol *symbol, Id *id)
{
    /* From a symbol traverse its inheritance tree to find a named attribute. */
    Symbol *definingSymbol =
        definingSymbolOfAttribute(parentOf(symbol), id);

    if (definingSymbol == NULL) return NULL;

    return findAttribute(propertiesOf(definingSymbol)->attributes, id);
}


/*----------------------------------------------------------------------*/
static void numberAttributes(Symbol *symbol)
{
    List *theList;
    Attribute *inheritedAttribute;

    if (symbol->fields.entity.attributesNumbered) return;

    for (theList = propertiesOf(symbol)->attributes; theList != NULL;
         theList = theList->next){
        Attribute *thisAttribute = theList->member.atr;
        inheritedAttribute = findInheritedAttribute(symbol, thisAttribute->id);
        if (inheritedAttribute != NULL) {
            thisAttribute->id->code = inheritedAttribute->id->code;
            thisAttribute->inheritance = INHERITED_REDEFINED;
        } else if (thisAttribute->id->code == 0) {
            thisAttribute->id->code = ++attributeCount;
            thisAttribute->inheritance = LOCAL;
        } /* Else its a pre-defined attribute which is numbered already! */
    }

    propertiesOf(symbol)->attributes = sortAttributes(propertiesOf(symbol)->attributes);
    symbol->fields.entity.attributesNumbered = TRUE;
}


/*----------------------------------------------------------------------*/
static void numberParentAttributes(Symbol *symbol)
{
    /* Recurse the parental chain and number the attributes. */
    if (symbol == NULL || symbol->fields.entity.attributesNumbered) return;

    numberParentAttributes(parentOf(symbol));
    numberAttributes(symbol);
}


/*----------------------------------------------------------------------*/
static void numberAttributesRecursively(Symbol *symbol)
{
    /* Recurse the parent to number its attributes.
       Number all attributes in the symbol (if it is a class or an instance);
    */

    if (symbol == NULL) return;

    if (isClass(symbol) || isInstance(symbol)) {
        /* Only a class or instance have attributes */

        numberParentAttributes(parentOf(symbol));
        numberAttributes(symbol);
    }

    /* Recurse in the symbolTree */
    if (symbol->lower != NULL) numberAttributesRecursively(symbol->lower);
    if (symbol->higher != NULL) numberAttributesRecursively(symbol->higher);

}



/*======================================================================*/
void numberAllAttributes(void)
{
    /* Traverse all classes and instances in the symbol table and give all
       attributes unique numbers. Start by recursing through the
       parents. Remember where we have been by looking at the code which
       might already have been set.
    */
    numberAttributesRecursively(symbolTree);
}


/*----------------------------------------------------------------------*/
static void replicateNames(Symbol *symbol)
{
    if (propertiesOf(symbol)->names == NULL)
        propertiesOf(symbol)->names = propertiesOf(parentOf(symbol))->names;
    else if (propertiesOf(parentOf(symbol))->names != NULL)
        propertiesOf(symbol)->names = combine(propertiesOf(symbol)->names,
                                              propertiesOf(parentOf(symbol))->names);
}


/*----------------------------------------------------------------------*/
static void replicatePronouns(Symbol *symbol)
{
    if (propertiesOf(symbol)->pronouns == NULL)
        propertiesOf(symbol)->pronouns = propertiesOf(parentOf(symbol))->pronouns;
}


/*----------------------------------------------------------------------*/
static void replicateAttributes(Symbol *symbol)
{
    List *atr;

    propertiesOf(symbol)->attributes =
        combineAttributes(propertiesOf(symbol)->attributes,
                          propertiesOf(parentOf(symbol))->attributes);

    /* Verify that there are no inherited, non-initialized, attributes */
    ITERATE(atr, propertiesOf(symbol)->attributes) {
        Attribute *thisAttribute = atr->member.atr;
        if (thisAttribute->type == REFERENCE_TYPE)
            if (!thisAttribute->initialized && !isClass(symbol))
                lmLogv(&propertiesOf(symbol)->id->srcp, 328, sevERR,
                       thisAttribute->id->string,
                       thisAttribute->definingSymbol->string,
                       NULL);
    }
}


/*----------------------------------------------------------------------*/
static void replicateContainer(Symbol *symbol)
{
    /* A container node can be generated once, we only have to keep
       container code and owner property pointer local so the global
       part can just be pointed to. */

    if (propertiesOf(symbol)->container == NULL && propertiesOf(parentOf(symbol))->container != NULL) {
        Properties *props = propertiesOf(symbol);
        /* Create a new Container Instance and link parents Container Body */
        props->container = newContainer(propertiesOf(parentOf(symbol))->container->body);
        props->container->ownerProperties = props;

        /* Add OPAQUE attribute */
        addOpaqueAttribute(props, props->container->body->opaque);
    }
}

/*----------------------------------------------------------------------*/
static void replicateExits(Symbol *symbol)
{
    propertiesOf(symbol)->exits = combineExits(propertiesOf(symbol)->exits,
                                               propertiesOf(parentOf(symbol))->exits);
}


/*----------------------------------------------------------------------*/
static void replicateScripts(Symbol *symbol)
{
    /* The parent may and may not have scripts. Any of those should be
       accessible from the current instance, however if it is overridden it
       should use the local version. During run-time we simply lookup
       scripts using the parental chain. This means that an instance only
       have the local scripts in its list. So there is nothing to do
       here. */
}



/*----------------------------------------------------------------------*/
static void replicateInitialLocation(Symbol *symbol)
{
    if (propertiesOf(symbol)->whr == NULL)
        propertiesOf(symbol)->whr = propertiesOf(parentOf(symbol))->whr;
}


/*----------------------------------------------------------------------*/
static void replicate(Symbol *symbol)
{
    replicateInitialLocation(symbol);
    replicateNames(symbol);
    replicatePronouns(symbol);
    replicateAttributes(symbol);
    /* Initialize is handled by interpreter */
    /* And so is Description Check and Description */
    /* ... and the Articles/Forms */
    replicateContainer(symbol);
    /* ... and the Verbs */
    replicateScripts(symbol);
    /* ... and Entered */
    replicateExits(symbol);
}


/*----------------------------------------------------------------------

  Recurse the parental chain and replicate any inherited things that
  requires local replicated data.

*/
static void replicateSymbol(Symbol *symbol)
{
    if (symbol == NULL) return;

    if (symbol->fields.entity.replicated) {
        return;
    }

    if (hasParent(symbol)) {
        replicateSymbol(parentOf(symbol));
        replicate(symbol);
    }
    symbol->fields.entity.replicated = TRUE;
}


/*----------------------------------------------------------------------*/
static void replicateSymbolTree(Symbol *symbol)
{
    if (symbol == NULL) return;

    if (isClass(symbol) || isInstance(symbol)) {
        replicateSymbol(symbol);
    }

    /* Recurse in the symbolTree */
    if (symbol->lower != NULL) replicateSymbolTree(symbol->lower);
    if (symbol->higher != NULL) replicateSymbolTree(symbol->higher);
}



/*======================================================================

  replicateInherited()

  Traverse the heritage of the symbol and replicate all inherited
  attributes, scripts etc. that are not locally redefined, thus
  creating a complete list of all attributes etc. that this symbol has.

  We will mark a completed symbol with "replicated" so that we can use
  it directly if we encounter it later instead of redoing it.

*/
void replicateInherited(void)
{
    replicateSymbolTree(symbolTree);
}


/*======================================================================*/
void generateSymbol(Symbol *symbol) {
    emitConstant(symbol->code);
}

/*----------------------------------------------------------------------*/
static void dumpSymbolKind(SymbolKind kind)
{
    switch (kind) {
    case CLASS_SYMBOL: put("CLASS"); break;
    case INSTANCE_SYMBOL: put("INSTANCE"); break;
    case VERB_SYMBOL: put("VERB"); break;
    case DIRECTION_SYMBOL: put("DIRECTION"); break;
    case PARAMETER_SYMBOL: put("PARAMETER"); break;
    case EVENT_SYMBOL: put("EVENT"); break;
    case LOCAL_SYMBOL: put("LOCAL"); break;
    default: put("*** UNKNOWN ***"); break;
    }
}

/*----------------------------------------------------------------------*/
static void dumpSymbolLeaf(Symbol *symbol)
{
    if (symbol == NULL) {
        put("NULL");
        return;
    }

    put("SYMBOL: "); dumpPointer(symbol); dumpSymbolKind(symbol->kind); indent();
    put("string: "); dumpString(symbol->string);
    put(", code: "); dumpInt(symbol->code);
    if (dumpFlags&DUMP_ADDRESSES) {
        nl();
        put("lower: "); dumpPointer(symbol->lower); put("higher: "); dumpPointer(symbol->higher);
    }
    out();
}


/*----------------------------------------------------------------------*/
static void dumpSymbolsRecursively(Symbol *symbol)
{
    if (symbol == NULL) return;
    dumpSymbolsRecursively(symbol->lower);
    if (firstSymbolDumped) firstSymbolDumped = FALSE; else nl();
    dumpSymbolLeaf(symbol);
    dumpSymbolsRecursively(symbol->higher);
}


/*======================================================================*/
void dumpSymbols(void)
{
    dumpPointer(symbolTree);
    indent();
    dumpSymbolsRecursively(symbolTree);
    out();
}


/*======================================================================*/
void dumpSymbol(Symbol *symbol)
{
    if (symbol == NULL) {
        put("NULL");
        return;
    }

    dumpPointer(symbol); dumpSymbolKind(symbol->kind); put(" ");
    dumpString(symbol->string);
    put(":"); dumpInt(symbol->code);
}

/* Local Variables: */
/* c-basic-offset: 4 */
/* indent-tabs-mode: nil */
/* End: */
