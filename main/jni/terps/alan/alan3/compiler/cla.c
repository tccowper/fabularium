/*----------------------------------------------------------------------*\

				CLA.C
			     Class Nodes

\*----------------------------------------------------------------------*/

#include "cla_x.h"

/* IMPORT */
#include <stdio.h>
#include "types.h"
#include "opt.h"

#include "ext.h"
#include "srcp_x.h"
#include "id_x.h"
#include "sym_x.h"
#include "stm_x.h"
#include "adv_x.h"
#include "atr_x.h"
#include "prop_x.h"
#include "lst_x.h"
#include "description_x.h"
#include "article_x.h"
#include "context_x.h"
#include "dump_x.h"

#include "emit.h"
#include "util.h"
#include "options.h"
#include "lmList.h"


/* PUBLIC DATA */

/* Predefined classes */
Class *entity,
  *thing,
  *object,
  *location,
  *actor,
  *literal,
  *integer,
  *string;



/* PRIVATE DATA */

static List *allClasses = NULL;

/*----------------------------------------------------------------------*/
static void addPredefinedProperties() {

}


/*======================================================================*/
void initClasses()
{
  Id *entityId = newId(nulsrcp, "entity");
  Id *literalId = newId(nulsrcp, "literal");
  Id *locationId = newId(nulsrcp, "location");
  Id *thingId = newId(nulsrcp, "thing");
  Id *objectId = newId(nulsrcp, "object");
  Id *actorId = newId(nulsrcp, "actor");
  Id *integerId = newId(nulsrcp, "integer");
  Id *stringId = newId(nulsrcp, "string");

  allClasses = NULL;

  entity = newClass(&nulsrcp, entityId, NULL, NULL);
  adv.clas = concat(adv.clas, entity, CLASS_LIST);
  entitySymbol = entity->props->id->symbol;
  entity->props->predefined = TRUE;


  location = newClass(&nulsrcp, locationId, entityId, NULL);
  adv.clas = concat(adv.clas, location, CLASS_LIST);
  locationSymbol = location->props->id->symbol;
  location->props->predefined = TRUE;

  thing = newClass(&nulsrcp, thingId, entityId, NULL);
  adv.clas = concat(adv.clas, thing, CLASS_LIST);
  thingSymbol = thing->props->id->symbol;
  thing->props->predefined = TRUE;

  object = newClass(&nulsrcp, objectId, thingId, NULL);
  adv.clas = concat(adv.clas, object, CLASS_LIST);
  objectSymbol = object->props->id->symbol;
  object->props->predefined = TRUE;

  actor = newClass(&nulsrcp, actorId, thingId, NULL);
  adv.clas = concat(adv.clas, actor, CLASS_LIST);
  actorSymbol = actor->props->id->symbol;
  actor->props->predefined = TRUE;

  literal = newClass(&nulsrcp, literalId, entityId, NULL);
  adv.clas = concat(adv.clas, literal, CLASS_LIST);
  literalSymbol = literal->props->id->symbol;
  literalSymbol->fields.entity.prohibitedSubclassing = TRUE;
  literal->props->predefined = TRUE;

  integer = newClass(&nulsrcp, integerId, literalId, NULL);
  adv.clas = concat(adv.clas, integer, CLASS_LIST);
  integerSymbol = integer->props->id->symbol;
  integerSymbol->fields.entity.prohibitedSubclassing = TRUE;
  integerSymbol->fields.entity.isBasicType = TRUE;
  integer->props->predefined = TRUE;

  string = newClass(&nulsrcp, stringId, literalId, NULL);
  adv.clas = concat(adv.clas, string, CLASS_LIST);
  stringSymbol = string->props->id->symbol;
  stringSymbol->fields.entity.prohibitedSubclassing = TRUE;
  stringSymbol->fields.entity.isBasicType = TRUE;
  string->props->predefined = TRUE;

  addPredefinedProperties();
}


/*======================================================================*/
Class *newClass(Srcp *srcp,	/* IN - Source Position */
		Id *id,
		Id *parent,
		Properties *props)
{
  Class *new;                  /* The newly allocated area */

  progressCounter();

  new = NEW(Class);

  new->srcp = *srcp;
  if (props == NULL)
    new->props = newEmptyProps();
  else
    new->props = props;

  new->props->id = id;
  new->props->parentId = parent;
  new->props->id->symbol = newSymbol(id, CLASS_SYMBOL);
  new->props->id->symbol->fields.entity.props = new->props;

  allClasses = concat(allClasses, new, CLASS_LIST);

  if (compareStrings(id->string, "container") == 0)
    lmLogv(&id->srcp, 260, sevERR, "class", "'container'", "the built-in container property", NULL);

  return(new);
}


/*----------------------------------------------------------------------*/
static void symbolizeClass(Class *cla)
{
    symbolizeProps(cla->props, TRUE);

    if (cla->props->parentId != NULL) {
        if (cla->props->parentId->symbol != NULL) {
            if (!isClass(cla->props->parentId->symbol))
                lmLog(&cla->props->parentId->srcp, 350, sevERR, "");
            else
                setParent(cla->props->id->symbol, cla->props->parentId->symbol);
        }
    }
}


/*======================================================================*/
void symbolizeClasses(void)
{
    List *l;

    for (l = allClasses; l; l = l->next)
        symbolizeClass(l->member.cla);
}


/*----------------------------------------------------------------------*/
static void analyzeClass(Class *class)
{
  Context *context = newClassContext(class);

  analyzeProps(class->props, context);
}


/*======================================================================*/
void analyzeAllClassAttributes() {
  List *l;
  ITERATE(l, allClasses) {
    Properties *props = l->member.cla->props;
    analyzeAttributes(props->attributes, props->id->symbol, newClassContext(l->member.cla));
  }
}


/*======================================================================*/
void analyzeClasses(void)
{
  List *l;

  for (l = allClasses; l; l = l->next)
    analyzeClass(l->member.cla);
}


/*======================================================================*/
void setupDefaultProperties() {
  char *the = "the";
  char *an = "a";
  char *any = "any";

  /* Add articles */
  switch (opts[OPTLANG].value) {
  case L_ENGLISH: any = "any"; the = "the"; an = "a"; break;
  case L_SWEDISH: any = "någon"; the = ""; an = "en"; break;
  case L_GERMAN: any = "kein"; the = "der"; an = "einer"; break;
  }

  if (!entitySymbol->fields.entity.props->definite)
    entitySymbol->fields.entity.props->definite = newArticle(nulsrcp,
							     newPrintStatementListFromString(the),
							     FALSE);
  if (!entitySymbol->fields.entity.props->indefinite)
    entitySymbol->fields.entity.props->indefinite = newArticle(nulsrcp,
							       newPrintStatementListFromString(an),
							       FALSE);
  if (!entitySymbol->fields.entity.props->negative)
    entitySymbol->fields.entity.props->negative = newArticle(nulsrcp,
							     newPrintStatementListFromString(any),
							     FALSE);

  /* Add pronouns */
  if (entity->props->pronouns == NULL)
    switch (opts[OPTLANG].value) {
    case L_ENGLISH:
      entity->props->pronouns = newIdList(NULL, "it");
      break;
    case L_SWEDISH:
      entity->props->pronouns = newIdList(newIdList(NULL,
						    "det"),
					  "den");
      break;
    case L_GERMAN:
      entity->props->pronouns = newIdList(newIdList(newIdList(NULL,
							      "es"),
						    "ihn"),
					  "sie");
      break;
    }
}


/*----------------------------------------------------------------------*/
static void generateClassData(Class *cla)
{
  generateCommonPropertiesData(cla->props);
  if (debugFlag) {
    cla->props->idAddress = nextEmitAddress();
    emitString(cla->props->id->string);
  }
}


/*----------------------------------------------------------------------*/
static void generateClassEntry(Class *cla)
{
  ClassEntry entry;

  cla->adr = nextEmitAddress();

  entry.code = cla->props->id->symbol->code;	/* First own code */

  if (cla->props->parentId == NULL)	/* Then parents */
    entry.parent = 0;
  else
    entry.parent = cla->props->parentId->symbol->code;

  entry.name = cla->props->nameAddress;

  if (cla->props->pronouns)
    entry.pronoun = cla->props->pronouns->member.id->code;
  else
    entry.pronoun = 0;

  entry.id = cla->props->idAddress;

  if (cla->props->initialize != 0)
    entry.initialize = cla->props->initialize->stmsAddress;
  else
    entry.initialize = 0;

  entry.descriptionChecks = checksAddressOf(cla->props->description);
  entry.description = doesAddressOf(cla->props->description);
  entry.entered = cla->props->enteredAddress;

  generateArticleEntry(cla->props->definite, &entry.definite);
  generateArticleEntry(cla->props->indefinite, &entry.indefinite);
  generateArticleEntry(cla->props->negative, &entry.negative);

  entry.mentioned = cla->props->mentionedAddress;
  entry.verbs = cla->props->verbsAddress;

  emitEntry(&entry, sizeof(entry));
}


/*======================================================================*/
Aaddr generateClasses(void)
{
  List *l;
  Aaddr adr;

  acodeHeader.entityClassId = entitySymbol->code;
  acodeHeader.thingClassId = thingSymbol->code;
  acodeHeader.objectClassId = objectSymbol->code;
  acodeHeader.locationClassId = locationSymbol->code;
  acodeHeader.actorClassId = actorSymbol->code;
  acodeHeader.literalClassId = literalSymbol->code;
  acodeHeader.integerClassId = integerSymbol->code;
  acodeHeader.stringClassId = stringSymbol->code;
  acodeHeader.classMax = classCount;

  for (l = allClasses; l; l = l->next)
    generateClassData(l->member.cla);

  adr = nextEmitAddress();
  for (l = allClasses; l; l = l->next)
    generateClassEntry(l->member.cla);
  emit(EOF);

  return (adr);
}



/*======================================================================*/
void dumpClass(Class *cla)
{
  put("CLA: "); dumpSrcp(cla->srcp); indent();
  put("props: "); dumpProps(cla->props); out();
}


/*======================================================================*/
void xmlClass(Class *cla, FILE* xmlFile)
{
    fprintf(xmlFile, "        <class NAME=\"%s\"", cla->props->id->string);
    if (cla->props->parentId)
        fprintf(xmlFile, " PARENT=\"%s\"", cla->props->parentId->string);
    fprintf(xmlFile, ">\n");
    fprintf(xmlFile, "        </class>\n");
}


/*======================================================================*/
void xmlClasses(FILE *xmlFile)
{
    fprintf(xmlFile, "    <classes>\n");
    xmlList(allClasses, CLASS_LIST, xmlFile);
    fprintf(xmlFile, "    </classes>\n");
}
