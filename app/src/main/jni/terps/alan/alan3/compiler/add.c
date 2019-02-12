/*----------------------------------------------------------------------*\

  ADD.C
  Add To Nodes

\*----------------------------------------------------------------------*/

#include "add_x.h"

/* IMPORT: */
#include "adv_x.h"
#include "prop_x.h"
#include "sym_x.h"
#include "id_x.h"
#include "srcp_x.h"
#include "atr_x.h"
#include "lst_x.h"
#include "vrb_x.h"
#include "ext_x.h"
#include "whr_x.h"
#include "dump_x.h"
#include "description_x.h"
#include "context_x.h"

#include "scr.h"
#include "ext.h"
#include "util.h"
#include "lmList.h"


/*======================================================================*/
AddNode *newAdd(Srcp srcp,
                Id *id,
                Id *parent,
                Properties *props)
{
    AddNode *new;

    progressCounter();

    new = NEW(AddNode);

    new->srcp = srcp;
    if (props)
        new->props = props;
    else
        new->props = newEmptyProps();
    new->toId = id;

    if (parent != NULL)
        lmLogv(&parent->srcp, 341, sevERR, "heritage", "", NULL);

    return(new);
}


#ifndef PROPERTIESOF
#define PROPERTIESOF(s) ((s)->fields.entity.props)
#endif

/*----------------------------------------------------------------------*/
static void addInitialLocation(AddNode *add, Symbol *original)
{
    Properties *props = add->props;

    if (props->whr != NULL) {
        if (PROPERTIESOF(original)->whr != NULL)
            lmLog(&add->props->whr->srcp, 336, sevERR,
                  "an Initial location when the class already have it");
        else {
            if (!inheritsFrom(PROPERTIESOF(original)->id->symbol, thingSymbol) && props->whr != NULL)
                lmLog(&props->whr->srcp, 405, sevERR, "have initial locations");
            else {
                symbolizeWhere(props->whr);
                if (verifyInitialLocation(props->whr, NULL))
                    PROPERTIESOF(original)->whr = props->whr;
            }
        }
    }
}


/*----------------------------------------------------------------------*/
static void addNames(AddNode *add, Symbol *original)
{
    Properties *props = add->props;

    if (props->names != NULL)
        PROPERTIESOF(original)->names = combine(props->names,
                                                PROPERTIESOF(original)->names);
}


/*----------------------------------------------------------------------*/
static void addPronouns(AddNode *add, Symbol *original)
{
    Properties *props = add->props;

    if (props->pronouns != NULL) {
        if (PROPERTIESOF(original)->pronouns != NULL)
            lmLog(&add->props->pronounsSrcp, 336, sevERR,
                  "Pronouns when the class already have it");
        else
            PROPERTIESOF(original)->pronouns = props->pronouns;
    }
}

/*----------------------------------------------------------------------*/
static void addAttributes(AddNode *add, Symbol *originalSymbol)
{
    List *addedAttributes = add->props->attributes;
    Properties *originalProps = originalSymbol->fields.entity.props;
    List *originalAttributes = originalProps->attributes;
    List *l;

    if (addedAttributes == NULL) return;
    symbolizeAttributes(addedAttributes, TRUE);

    ITERATE(l, addedAttributes) {
        Attribute *originalAttribute = findAttribute(originalAttributes, l->member.atr->id);
        if (originalAttribute != NULL) /* It was found in the original */
            lmLog(&l->member.atr->id->srcp, 336, sevERR, "an attribute which already exists");
    }
    originalProps->attributes = combine(originalProps->attributes,
                                        addedAttributes);
}


/*----------------------------------------------------------------------*/
static void addInitialize(AddNode *add, Symbol *original)
{
    Properties *props = add->props;

    if (props->initialize != NULL) {
        if (PROPERTIESOF(original)->initialize != NULL)
            lmLog(&add->props->initializeSrcp, 336, sevERR,
                  "an Initialize clause when the class already have it");
        else
            PROPERTIESOF(original)->initialize = props->initialize;
    }
}


/*----------------------------------------------------------------------*/
static void addDescriptionCheck(AddNode *add, Symbol *originalSymbol)
{
    Properties *addedProps = add->props;
    Properties *originalProps = originalSymbol->fields.entity.props;

    if (checksOf(addedProps->description) != NULL) {
        if (checksOf(originalProps->description) != NULL)
            lmLogv(&addedProps->description->checkSrcp, 241, sevERR, "A Description Check is",
                   originalSymbol->string, NULL);
        else {
            if (originalProps->description == NULL)
                originalProps->description = newDescription(addedProps->description->checkSrcp,
                                                            addedProps->description->checks, nulsrcp, NULL);
            else {
                originalProps->description->checkSrcp = addedProps->description->checkSrcp;
                originalProps->description->checks = addedProps->description->checks;
            }
        }
    }
}


/*----------------------------------------------------------------------*/
static void addDescription(AddNode *add, Symbol *originalSymbol)
{
    Properties *addedProps = add->props;
    Properties *originalProps = originalSymbol->fields.entity.props;

    if (doesOf(addedProps->description) != NULL) {
        if (doesOf(originalProps->description) != NULL)
            lmLogv(&addedProps->description->doesSrcp, 241, sevERR, "A Description is",
                   originalSymbol->string, NULL);
        else {
            if (originalProps->description == NULL)
                originalProps->description = newDescription(nulsrcp, NULL,
                                                            addedProps->description->doesSrcp,
                                                            addedProps->description->does);
            else {
                originalProps->description->doesSrcp = addedProps->description->doesSrcp;
                originalProps->description->does = addedProps->description->does;
            }
        }
    }
}


/*----------------------------------------------------------------------*/
static void addArticles(AddNode *add, Symbol *original)
{
    if (add->props->definite != NULL) {
        if (original->fields.entity.props->definite != NULL)
            lmLog(&add->props->definite->srcp, 336, sevERR,
                  "a Definite Article when the class already have it");
        else
            original->fields.entity.props->definite = add->props->definite;
    }

    if (add->props->indefinite != NULL) {
        if (original->fields.entity.props->indefinite != NULL)
            lmLog(&add->props->indefinite->srcp, 336, sevERR,
                  "Indefinite Article when the class already have it");
        else
            original->fields.entity.props->indefinite = add->props->indefinite;
    }

    if (add->props->negative != NULL) {
        if (original->fields.entity.props->negative != NULL)
            lmLog(&add->props->negative->srcp, 336, sevERR,
                  "Negative Article when the class already have it");
        else
            original->fields.entity.props->negative = add->props->negative;
    }
}


/*----------------------------------------------------------------------*/
static void addMentioned(AddNode *add, Symbol *original)
{
    Properties *props = add->props;

    if (props->mentioned != NULL) {
        if (original->fields.entity.props->mentioned != NULL)
            lmLog(&add->props->mentionedSrcp, 336, sevERR,
                  "Mentioned clause when the class already have it");
        else
            original->fields.entity.props->mentioned = add->props->mentioned;
    }
}


/*----------------------------------------------------------------------*/
static void addContainer(AddNode *add, Symbol *original)
{
    Properties *props = add->props;

    if (props->container == NULL) return;

    if (original->fields.entity.props->container != NULL)
        lmLog(&props->container->body->srcp, 336, sevERR,
              "container properties when the class already have it");
    else
        original->fields.entity.props->container = props->container;

}


/*----------------------------------------------------------------------*/
static void addVerbs(AddNode *add, Symbol *originalSymbol)
{
    Properties *originalProps = originalSymbol->fields.entity.props;
    List *verbList;
    List *verbIdList;
    Bool inhibitAdd = FALSE;

    if (add->props->verbs != NULL) {
        if (originalSymbol == entitySymbol)
            lmLog(&add->props->verbs->member.vrb->srcp, 426, sevWAR, "");
        ITERATE(verbList, add->props->verbs) {
            ITERATE(verbIdList, verbList->member.vrb->ids)
                if (verbIdFound(verbIdList->member.id, originalProps->verbs)) {
                    inhibitAdd = TRUE;
                    lmLogv(&verbIdList->member.id->srcp, 240, sevERR, "Verb", verbIdList->member.id->string, originalSymbol->string, NULL);
                }
        }
        if (!inhibitAdd)
            originalProps->verbs = combine(originalProps->verbs, add->props->verbs);
    }
}


/*----------------------------------------------------------------------*/
static void addScripts(AddNode *add, Symbol *original)
{
    Properties *props = add->props;
    Properties *originalProps = original->fields.entity.props;
    List *addedScripts;
    List *originalScripts;
    List *scriptsToAdd = NULL;
    Bool doNotAdd = FALSE;

    if (props->scripts == NULL) return;

    if (!inheritsFrom(original, actorSymbol)) {
        lmLog(&add->props->scripts->member.script->srcp, 336, sevERR, "scripts to a class which is not a subclass of the predefined class 'actor'");
        doNotAdd = TRUE;
    }
    ITERATE(addedScripts, props->scripts) {
        Script *addedScript = addedScripts->member.script;
        Bool duplicate = FALSE;
        ITERATE(originalScripts, originalProps->scripts) {
            Script *originalScript = originalScripts->member.script;
            if (equalId(addedScript->id, originalScript->id)) {
                lmLogv(&addedScript->srcp, 240, sevERR,
                       "Script", addedScript->id->string, add->toId->string, NULL);
                duplicate = TRUE;
                break;
            }
        }
        if (!duplicate && !doNotAdd)
            scriptsToAdd = concat(scriptsToAdd, addedScript, SCRIPT_LIST);
    }
    originalProps->scripts = combine(originalProps->scripts, scriptsToAdd);
}


/*----------------------------------------------------------------------*/
static void addEntered(AddNode *add, Symbol *originalSymbol)
{
    Properties *props = add->props;

    if (props->enteredStatements != NULL) {
        if (!inheritsFrom(originalSymbol, locationSymbol)) {
            lmLog(&add->props->enteredSrcp, 336, sevERR, "Entered clause to something not inheriting from the predefined class 'location'");
        } else {
            if (originalSymbol->fields.entity.props->enteredStatements != NULL) {
                lmLogv(&add->props->enteredSrcp, 344, sevWAR,
                       "Entered clause",  originalSymbol->string, NULL);
                lmLog(&originalSymbol->fields.entity.props->enteredSrcp, 345, sevWAR, "Entered clause");
            } else {
                PROPERTIESOF(originalSymbol)->enteredStatements = props->enteredStatements;
                PROPERTIESOF(originalSymbol)->enteredSrcp = props->enteredSrcp;
            }
        }
    }
}


/*----------------------------------------------------------------------*/
static void addExits(AddNode *add, Symbol *originalSymbol)
{
    Properties *originalProps = originalSymbol->fields.entity.props;
    List *exitList;
    List *exitIdList;
    Bool inhibitAdd = FALSE;

    if (add->props->exits != NULL) {
        symbolizeExits(add->props->exits);
        if (!inheritsFrom(originalSymbol, locationSymbol)) {
            lmLog(&add->props->exits->member.ext->srcp, 336, sevERR, "Exits to something not inheriting from the predefined class 'location'");
            inhibitAdd = TRUE;
        }

        ITERATE(exitList, add->props->exits) {
            ITERATE(exitIdList, exitList->member.ext->directions)
                if (exitIdFound(exitIdList->member.id, originalProps->exits)) {
                    inhibitAdd = TRUE;
                    lmLogv(&exitIdList->member.id->srcp, 240, sevERR, "Exit", exitIdList->member.id->string, originalSymbol->string, NULL);
                }
        }
        if (!inhibitAdd)
            /* If there was no error above we can combine the additions,
               else it doesn't matter */
            originalProps->exits = combine(originalProps->exits, add->props->exits);
    }
}


/*----------------------------------------------------------------------*/
static void verifyAdd(AddNode *add, Symbol *originalSymbol)
{
    /* Can't add anything except verbs to non-instantiable classes */
    if (originalSymbol->fields.entity.prohibitedSubclassing) {
        int propsCount = 1;		/* Verbs-slot is not counted so start at 1 */

        if (add->props->whr)
            lmLogv(&add->props->whr->srcp, 424, sevERR, "initial location", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->names)
            lmLogv(&add->srcp, 424, sevERR, "names", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->pronouns)
            lmLogv(&add->srcp, 424, sevERR, "pronouns", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->attributes)
            lmLogv(&add->props->attributes->member.atr->srcp, 424, sevERR, "attributes", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->initialize)
            lmLogv(&add->props->initialize->srcp, 424, sevERR, "initialize", originalSymbol->string, NULL);
        propsCount++;

        if (checksOf(add->props->description) != NULL || doesOf(add->props->description) != NULL)
            lmLogv(&add->props->description->doesSrcp, 424, sevERR, "description", originalSymbol->string, NULL);
        propsCount+=2;

        if (add->props->definite)
            lmLogv(&add->props->definite->srcp, 424, sevERR, "article", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->indefinite)
            lmLogv(&add->props->indefinite->srcp, 424, sevERR, "article", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->negative)
            lmLogv(&add->props->negative->srcp, 424, sevERR, "article", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->mentioned)
            lmLogv(&add->props->mentionedSrcp, 424, sevERR, "mentioned", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->container)
            lmLogv(&add->props->container->body->srcp, 424, sevERR, "container", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->scripts)
            lmLogv(&add->props->scripts->member.script->srcp, 424, sevERR, "scripts", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->enteredStatements)
            lmLogv(&add->props->enteredSrcp, 424, sevERR, "entered", originalSymbol->string, NULL);
        propsCount++;

        if (add->props->exits)
            lmLogv(&add->props->exits->member.ext->srcp, 424, sevERR, "exits", originalSymbol->string, NULL);
        propsCount++;

        if (propsCount != NOOFPROPS)
            SYSERR("Wrong number of property checks", add->srcp);
    }
}


/*----------------------------------------------------------------------*/
static void addAddition(AddNode *add)
{
    Symbol *originalClass = symcheck(add->toId, CLASS_SYMBOL, NULL);

    if (originalClass != NULL) {
        int propCount = 0;
        verifyAdd(add, originalClass);
        addInitialLocation(add, originalClass); propCount++;
        addNames(add, originalClass); propCount++;
        addPronouns(add, originalClass); propCount++;
        addAttributes(add, originalClass); propCount++;
        addInitialize(add, originalClass); propCount++;
        addDescriptionCheck(add, originalClass); propCount++;
        addDescription(add, originalClass); propCount++;
        addArticles(add, originalClass); propCount+=3;
        addMentioned(add, originalClass); propCount++;
        addContainer(add, originalClass); propCount++;
        addVerbs(add, originalClass); propCount++;
        addScripts(add, originalClass); propCount++;
        addEntered(add, originalClass);  propCount++;
        addExits(add, originalClass); propCount++;
        if (propCount != NOOFPROPS)
            SYSERR("Wrong property count", add->srcp);
    }
}


/*======================================================================*/
void addAdditions(void)
{
    List *l;

    for (l = adv.adds; l != NULL; l = l->next)
        addAddition(l->member.add);
}


/*======================================================================*/
void dumpAdd(AddNode *add)
{
    put("ADD: "); dumpSrcp(add->srcp); indent();
    put("toId: "); dumpId(add->toId); nl();
    put("props: "); dumpProps(add->props); out();
}
