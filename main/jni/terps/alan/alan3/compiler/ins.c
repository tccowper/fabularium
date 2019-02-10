/*----------------------------------------------------------------------*\

  INS.C
  Instance Nodes

\*----------------------------------------------------------------------*/

#include "ins_x.h"

#include "options.h"
#include "sysdep.h"
#include "util.h"
#include "emit.h"
#include "adv.h"

#include "description_x.h"
#include "id_x.h"
#include "lst_x.h"
#include "scr_x.h"
#include "prop_x.h"
#include "srcp_x.h"
#include "sym_x.h"
#include "wrd_x.h"
#include "atr_x.h"
#include "context_x.h"
#include "dump_x.h"
#include "cnt_x.h"

#include "lmList.h"


static List *allInstances = NULL;


/*======================================================================*/
void initInstances()
{
    allInstances = NULL;
    addNowhere();
}


/*----------------------------------------------------------------------*/
static void ensureHeroInheritsFromActor(Symbol *hero) {
    Id *actorId = newId(nulsrcp, "actor");

    if (actorSymbol == NULL) SYSERR("ActorSymbol == NULL", nulsrcp);
    if (hero->kind == INSTANCE_SYMBOL && hero->fields.entity.props != NULL &&
        hero->fields.entity.props->parentId == NULL) {
        hero->fields.entity.parent = actorSymbol;
        hero->fields.entity.props->parentId = actorId;
    }
}


/*----------------------------------------------------------------------*/
void addHeroContainer() {
    if (symbolIsActor(theHero)) {
        if (!symbolIsContainer(theHero))
            theHero->fields.entity.props->container = newContainer(NULL);
        symbolizeProps(theHero->fields.entity.props, FALSE);
    } else {
        /* Else probably error recovery error... Ensure some things are available */
        if (theHero->fields.entity.props == NULL)
            theHero->fields.entity.props = newEmptyProps();
    }
}


/*======================================================================*/
void addHero(Adventure *adv)
{
    Symbol *hero = lookup("hero");
    Instance *theHeroInstance = NULL;
    Id *actorId = newId(nulsrcp, "actor");

    if (hero == NULL) {
        theHeroInstance = newInstance(&nulsrcp, newId(nulsrcp, "hero"),
                                      actorId, NULL);
        adv->inss = concat(adv->inss, theHeroInstance, INSTANCE_LIST);
        theHero = theHeroInstance->props->id->symbol;
    } else {
        theHero = hero;
    }
    ensureHeroInheritsFromActor(theHero);
}


/*======================================================================*/
void addLiteralInstance(void)
{
    // Add a special instance representing all literals
    // Literals can't have modifiable attributes so one is enough
    // It should be generated as the last instance.
    Id *literalClassId = newId(nulsrcp, "literal");
    Symbol *literalClassSymbol = lookup("literal");

    Properties *props = newProps(NULL, NULL, nulsrcp, NULL, NULL, NULL, NULL, nulsrcp, NULL, NULL, NULL, NULL, NULL, NULL, nulsrcp, NULL, NULL, NULL);
    literalClassId->symbol = literalClassSymbol;
    (void) newInstance(&nulsrcp, newId(nulsrcp, "#literal"), literalClassId, props);
    props->predefined = TRUE;
}


/*======================================================================*/
void addNowhere(void)
{
    Instance *theNowhereInstance;

    theNowhereInstance = newInstance(&nulsrcp, newId(nulsrcp, "#nowhere"),
                                     newId(nulsrcp, "location"), NULL);
    nowhere = theNowhereInstance->props->id->symbol;
}


/*======================================================================*/
Instance *newInstance(Srcp *srcp,
                      Id *id,
                      Id *parent,
                      Properties *props)
{
    Instance *new;                  /* The newly allocated area */

    progressCounter();

    new = NEW(Instance);

    new->srcp = *srcp;
    if (props)
        new->props = props;
    else
        new->props = newEmptyProps();
    new->props->id = id;
    new->props->parentId = parent;

    new->props->id->symbol = newSymbol(id, INSTANCE_SYMBOL);
    new->props->id->symbol->fields.entity.props = new->props;

    allInstances = concat(allInstances, new, INSTANCE_LIST);

    return(new);
}



/*----------------------------------------------------------------------*/
static void symbolizeInstance(Instance *ins)
{
    symbolizeProps(ins->props, FALSE);
}


/*======================================================================*/
void symbolizeInstances(void)
{
    List *l;

    for (l = allInstances; l; l = l->next)
        symbolizeInstance(l->member.ins);
}


/*======================================================================*/
void analyzeAllInstanceAttributes() {
    List *l;
    ITERATE(l, allInstances) {
        Properties *props = l->member.ins->props;
        analyzeAttributes(props->attributes, props->id->symbol, newInstanceContext(l->member.ins));
    }
}


/*----------------------------------------------------------------------*/
static void analyzeNameWords(Instance *instance)
{
    List *nameList, *list;

    /* Note names as words in the dictionary */
    if (instance->props->names == NULL) /* No name, use identifier as a noun */
        newNounWord(instance->props->id->string, instance->props->id->code, instance);
    else {
        for (nameList = instance->props->names; nameList != NULL; nameList = nameList->next) {
            for (list = nameList->member.lst; list->next != NULL; list = list->next)
                newAdjectiveWord(list->member.id->string, instance);
            newNounWord(list->member.id->string, list->member.id->code, instance);
        }
    }
}


/*----------------------------------------------------------------------*/
static void analyzePronouns(Instance *instance)
{
    List *p;

    ITERATE(p, instance->props->pronouns)
        p->member.id->code = newPronounWord(p->member.id->string, instance);
}

/*----------------------------------------------------------------------*/
static void analyzeInstance(Instance *instance)
{
    Context *context = newInstanceContext(instance);

    /* Only instances need names and pronouns in the dictionary */
    analyzeNameWords(instance);
    analyzePronouns(instance);

    analyzeProps(instance->props, context);
}


/*======================================================================*/
void analyzeInstances(void)
{
    List *l;

    for (l = allInstances; l; l = l->next)
        analyzeInstance(l->member.ins);
}


/*----------------------------------------------------------------------*/
static void generateInstanceData(Instance *ins)
{
    generateInstancePropertiesData(ins->props);
}


/*----------------------------------------------------------------------*/
static void generateInstanceEntry(Instance *ins)
{
    InstanceEntry entry;

    generatePropertiesEntry(&entry, ins->props);
    emitEntry(&entry, sizeof(entry));
}


/*----------------------------------------------------------------------*/
static Aaddr generateInstanceTable(List *instances)
{
    Aaddr address = nextEmitAddress();
    List *l;

    for (l = instances; l; l = l->next)
        generateInstanceEntry(l->member.ins);
    emit(EOF);
    return address;
}


/*----------------------------------------------------------------------*/
void generateInstanceId(Instance *ins)
{
    ins->props->id->stringAddress = nextEmitAddress();
    emitString(ins->props->id->string);
}


/*----------------------------------------------------------------------*/
void generateInstanceIdTable(List *instances)
{
    List *l;

    for (l = instances; l; l = l->next) {
        emit(l->member.ins->props->id->stringAddress);
    }
    emit(EOF);
}


/*======================================================================*/
void generateInstances(ACodeHeader *header)
{
    List *l;

    if (debugFlag) {
        /* Generate all programmer ids for all instances */
        for (l = allInstances; l; l = l->next)
            generateInstanceId(l->member.ins);
    }

    for (l = allInstances; l; l = l->next)
        generateInstanceData(l->member.ins);

    header->instanceTableAddress = generateInstanceTable(allInstances);

    if (debugFlag)
        /* Generate table for all programmer names here, since here we
           can calculate it without having to have a pointer stored
           anywhere */
        generateInstanceIdTable(allInstances);

    header->instanceMax = instanceCount;
    header->attributesAreaSize = attributeAreaSize;
    header->theHero = theHero->code;
}



/*======================================================================*/
void dumpInstance(Instance *ins)
{
    put("INS: "); dumpSrcp(ins->srcp); indent();
    put("props: "); dumpProps(ins->props); out();
}


/*======================================================================*/
void xmlInstance(Instance *ins, FILE* xmlFile)
{
    fprintf(xmlFile, "        <instance NAME=\"%s\" PARENT=\"%s\">\n", ins->props->id->string, ins->props->parentId->string);
    xmlList(ins->props->exits, EXIT_LIST, xmlFile);
    fprintf(xmlFile, "        </instance>\n");
}


/*======================================================================*/
void xmlInstances(FILE *xmlFile)
{
    fprintf(xmlFile, "    <instances>\n");
    xmlList(allInstances, INSTANCE_LIST, xmlFile);
    fprintf(xmlFile, "    </instances>\n");
}
