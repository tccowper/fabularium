/*----------------------------------------------------------------------*\

  PROP.C
  Property Nodes

\*----------------------------------------------------------------------*/

#include "prop_x.h"

/* IMPORT */
#include <stdio.h>
#include "alan.h"
#include "util.h"
#include "emit.h"
#include "lmList.h"


#include "srcp_x.h"
#include "atr_x.h"
#include "chk_x.h"
#include "cla_x.h"
#include "cnt_x.h"
#include "ext_x.h"
#include "id_x.h"
#include "description_x.h"
#include "initialize_x.h"
#include "article_x.h"
#include "lst_x.h"
#include "nam_x.h"
#include "scr_x.h"
#include "stm_x.h"
#include "sym_x.h"
#include "vrb_x.h"
#include "whr_x.h"
#include "wrd_x.h"
#include "dump_x.h"


/*======================================================================*/
Properties *newEmptyProps(void)
{
    return NEW(Properties);
}


/*======================================================================*/
Properties *newProps(Where *whr, List *names,
                     Srcp pronounsSrcp, List *pronouns,
                     List *attributes,
                     Initialize *init,
                     Description *description,
                     Srcp mentionedSrcp, List *mentioned,
                     Article *definite,
                     Article *indefinite,
                     Article *negative,
                     Container *container,
                     List *verbs,
                     Srcp enteredSrcp, List *enteredStatements,
                     List *exits,
                     List *scripts)
{
    Properties *new;                  /* The newly allocated area */

    progressCounter();

    new = NEW(Properties);

    new->whr = whr;
    new->names = names;
    new->pronounsSrcp = pronounsSrcp;
    new->pronouns = pronouns;
    new->attributes = attributes;
    new->initialize = init;
    new->description = description;
    new->mentioned = mentioned;
    new->mentionedSrcp = mentionedSrcp;
    new->definite = definite;
    new->indefinite = indefinite;
    new->negative = negative;
    new->container = container;
    new->verbs = verbs;
    new->enteredStatements = enteredStatements;
    new->enteredSrcp = enteredSrcp;
    new->exits = exits;
    new->scripts = scripts;

    return(new);
}


/*----------------------------------------------------------------------*/
static void symbolizeParent(Properties *props)
{
    Symbol *parent;

    if (props->parentId != NULL) {
        parent = lookup(props->parentId->string);
        if (parent == NULL)
            lmLogv(&props->parentId->srcp, 310, sevERR, props->parentId->string, "", NULL);
        else if (!isClass(parent))
            lmLog(&props->parentId->srcp, 350, sevERR, "");
        else {
            props->parentId->symbol = parent;
            setParent(props->id->symbol, props->parentId->symbol);
        }
    }
}


/*======================================================================*/
void addOpaqueAttribute(Properties *props, Bool opaque)
{
    Id *opaqueId = newId(nulsrcp, "opaque");
    Attribute *attribute = newBooleanAttribute(nulsrcp, opaqueId, opaque);

    attribute->id->code = OPAQUEATTRIBUTE;	/* Pre-defined 'opaque' code */
    /* Make sure the opaque attribute is first, so combine the lists */
    props->attributes = combine(newList(attribute, ATTRIBUTE_LIST),
                                props->attributes);
}


/*======================================================================*/
void addVisitsAttribute(Properties *props)
{
    Id *attributeId = newId(nulsrcp, "visits");
    Attribute *attribute = newIntegerAttribute(nulsrcp, attributeId, 0);
    attribute->readonly = TRUE;

    attribute->id->code = VISITSATTRIBUTE;	/* Pre-defined 'visits' code */
    /* Make sure the visits attribute is first, so combine the lists */
    props->attributes = combine(newList(attribute, ATTRIBUTE_LIST),
                                props->attributes);
}

/*======================================================================*/
void symbolizeProps(Properties *props, Bool inClassDeclaration)
{
    symbolizeContainer(props->container);
    symbolizeParent(props);
    symbolizeAttributes(props->attributes, inClassDeclaration);
    if (props->container)
        addOpaqueAttribute(props, props->container->body->opaque);
    if (inheritsFrom(props->id->symbol, locationSymbol))
        addVisitsAttribute(props);
    symbolizeWhere(props->whr);
    symbolizeExits(props->exits);
}


/*----------------------------------------------------------------------*/
static void analyzeMentioned(Properties *props, Context *context)
{
    if (props->mentioned != NULL) {
        if ((props->names != NULL) && inheritsFrom(props->id->symbol, locationSymbol))
            lmLog(&props->mentionedSrcp, 425, sevWAR, "");
        analyzeStatements(props->mentioned, context);
    }
}


/*----------------------------------------------------------------------*/
static void checkSubclassing(Properties *props)
{
    if (props->parentId) {
        if (props->id->symbol == theHero) {
            if (!inheritsFrom(props->parentId->symbol, actorSymbol))
                lmLog(&props->parentId->srcp, 411, sevERR, "Inheritance from anything but 'actor' and its subclasses");
        } else if (props->parentId->symbol)
            if (props->parentId->symbol->fields.entity.prohibitedSubclassing &&
                !props->predefined)
                lmLog(&props->parentId->srcp, 423, sevERR, props->parentId->string);
    } else if (props->id->symbol != entitySymbol && props->id->symbol != theHero)
        lmLog(&props->id->srcp, 429, sevERR, "");
}


/*----------------------------------------------------------------------*/
static void analyzeCircularLocations(Properties *props)
{
    if (props->circularInspection == VISITED) {
        lmLog(&props->whr->srcp, 802, sevERR, props->id->string);
    } else {
        props->circularInspection = VISITED;
        if (props->whr
            && props->whr->kind == WHERE_AT
            && props->whr->what
            && props->whr->what->kind == WHAT_EXPRESSION
            && props->whr->what->fields.wht.wht
            && props->whr->what->fields.wht.wht->kind == WHAT_ID
            && props->whr->what->fields.wht.wht->id) {
            Symbol *sym = props->whr->what->fields.wht.wht->id->symbol;
            if (sym != NULL && sym->kind == INSTANCE_SYMBOL)
                analyzeCircularLocations(sym->fields.entity.props);
        }
    }
    props->circularInspection = VIRGIN;
}


/*======================================================================*/
void analyzeProps(Properties *props, Context *context)
{
    checkSubclassing(props);

    if (props->whr != NULL)
        verifyInitialLocation(props->whr, context);
    if (props->whr != NULL &&
        !(inheritsFrom(props->id->symbol, thingSymbol)
          || inheritsFrom(props->id->symbol, locationSymbol)))
        lmLog(&props->whr->srcp, 405, sevERR, "have initial locations");
    if (props->whr != NULL && props->whr->kind == WHERE_IN) {
        if (inheritsFrom(props->id->symbol, actorSymbol))
            lmLog(&props->whr->srcp, 402, sevERR, "An Actor");
        else if (inheritsFrom(props->id->symbol, locationSymbol))
            lmLog(&props->whr->srcp, 402, sevERR, "A Location");
    }

    /* Don't analyze attributes since those are analyzed already */

    analyzeNames(props);
    analyzeInitialize(props->initialize, context);
    analyzeDescription(props->description, context);
    analyzeStatements(props->enteredStatements, context);
    analyzeMentioned(props, context);
    analyzeStatements(props->mentioned, context);
    analyzeArticle(props->definite, context);
    analyzeArticle(props->indefinite, context);
    analyzeArticle(props->negative, context);
    analyzeVerbs(props->verbs, context);

    /* Have container ? */
    if (props->container) {
        /* But is a location? */
        if (inheritsFrom(props->id->symbol, locationSymbol))
            lmLogv(&props->id->srcp, 354, sevERR,
                   isClass(props->id->symbol)?"Class":"Instance",
                   props->id->string,
                   "location",
                   "Container properties, which is dubious in use",
                   NULL);
        analyzeContainer(props->container, context);
    }

    /* Have ENTERED or EXITs but not a location? */
    if (props->enteredStatements && !inheritsFrom(props->id->symbol, locationSymbol))
        lmLogv(&props->id->srcp, 352, sevERR,
               isClass(props->id->symbol)?"Class":"Instance",
               props->id->string,
               "location",
               "ENTERED statements which is not allowed",
               NULL);
    if (props->exits && !inheritsFrom(props->id->symbol, locationSymbol))
        lmLogv(&props->id->srcp, 352, sevERR,
               isClass(props->id->symbol)?"Class":"Instance",
               props->id->string,
               "location",
               "EXITs, which can never be traversed",
               NULL);
    analyzeExits(props->exits, context);

    /* Have scripts but not an actor? */
    if (props->scripts && !inheritsFrom(props->id->symbol, actorSymbol))
        lmLogv(&props->id->srcp, 352, sevERR,
               isClass(props->id->symbol)?"Class":"Instance",
               props->id->string,
               "actor",
               "SCRIPTs, which can never be executed",
               NULL);
    prepareScripts(props->scripts, props->id);
    analyzeScripts(props->scripts, context);
    analyzeCircularLocations(props);
}


/*======================================================================*/
void generateCommonPropertiesData(Properties *props)
{
    if (props->nameStatement != NULL) {
        props->nameAddress = nextEmitAddress();
        generateStatements(props->nameStatement);
        emit0(I_RETURN);
    }

    generateInitialize(props->initialize);
    generateDescription(props->description);

    generateArticle(props->definite);
    generateArticle(props->indefinite);
    generateArticle(props->negative);

    if (props->mentioned != NULL) {
        props->mentionedAddress = nextEmitAddress();
        generateStatements(props->mentioned);
        emit0(I_RETURN);
    }

    props->verbsAddress = generateVerbs(props->verbs);

    if (props->enteredStatements != NULL) {
        props->enteredAddress = nextEmitAddress();
        generateStatements(props->enteredStatements);
        emit0(I_RETURN);
    }
    props->exitsAddress = generateExits(props->exits);
}


/*======================================================================*/
void generateInstancePropertiesData(Properties *props)
{
    props->idAddress = nextEmitAddress();
    emitString(props->id->string);

    props->attributeAddress = generateAttributes(props->attributes, props->id->symbol->code);

    /* Now generate all the things both instances and classes have */
    generateCommonPropertiesData(props);
}


/*======================================================================*/
void generatePropertiesEntry(InstanceEntry *entry, Properties *props)
{
    entry->code = props->id->symbol->code; /* First own code */
    entry->id = props->idAddress; /* Address to the id string */

    if (props->parentId == NULL)	/* Then parents... */
        entry->parent = 0;
    else
        entry->parent = props->parentId->symbol->code;

    entry->initialLocation = generateInitialLocation(props);
    if (entry->initialLocation == 0 && !inheritsFrom(props->id->symbol, locationSymbol))
        entry->initialLocation = 1;
    entry->initialAttributes = props->attributeAddress;

    entry->name = props->nameAddress;

    if (props->pronouns)
        entry->pronoun = props->pronouns->member.id->code;
    else
        entry->pronoun = 0;

    if (props->initialize)
        entry->initialize = props->initialize->stmsAddress;
    else
        entry->initialize = 0;

    entry->checks = checksAddressOf(props->description);
    entry->description = doesAddressOf(props->description);

    if (props->container != NULL)
        entry->container = props->container->code;
    else
        entry->container = 0;

    entry->mentioned = props->mentionedAddress;

    generateArticleEntry(props->definite, &entry->definite);
    generateArticleEntry(props->indefinite, &entry->indefinite);
    generateArticleEntry(props->negative, &entry->negative);

    entry->verbs = props->verbsAddress;

    entry->entered = props->enteredAddress;
    entry->exits = props->exitsAddress;
}


/*======================================================================*/
void dumpProps(Properties *props)
{
    put("PROPS: "); dumpPointer(props); indent();
    put("id: "); dumpId(props->id); nl();
    put("parentId: "); dumpId(props->parentId); nl();
    put("whr: "); dumpWhere(props->whr); nl();
    put("names: "); dumpListOfLists(props->names, NAME_LIST); nl();
    put("pronoun: "); dumpList(props->pronouns, ID_LIST); nl();
    put("initialize: "); dumpInitialize(props->initialize); nl();
    put("container: "); dumpContainer(props->container); nl();
    put("attributes: "); dumpList(props->attributes, ATTRIBUTE_LIST); nl();
    put("attributeAddress: "); dumpAddress(props->attributeAddress); nl();

    put("enteredSrcp: "); dumpSrcp(props->enteredSrcp); nl();
    put("entered: "); dumpList(props->enteredStatements, STATEMENT_LIST); nl();

    put("description: "); dumpDescription(props->description); nl();
    put("definite: "); dumpArticle(props->definite); nl();
    put("indefinite: "); dumpArticle(props->indefinite); nl();
    put("negative: "); dumpArticle(props->negative); nl();
    put("mentioned: "); dumpList(props->mentioned, STATEMENT_LIST); nl();
    put("mentionedAddress: "); dumpAddress(props->mentionedAddress); nl();
    put("scripts: "); dumpList(props->scripts, SCRIPT_LIST); nl();
    put("scriptsAddress: "); dumpAddress(props->scriptsAddress); nl();
    put("verbs: "); dumpList(props->verbs, VERB_LIST); nl();
    put("verbsAddress: "); dumpAddress(props->verbsAddress); nl();
    put("exits: "); dumpList(props->exits, EXIT_LIST); nl();
    put("exitsAddress: "); dumpAddress(props->exitsAddress); out();
}
