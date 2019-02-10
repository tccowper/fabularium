/*======================================================================* \

  symTest.c

  A unit test module for SYM nodes in the Alan compiler

\*======================================================================*/

#include "sym.c"

#include <cgreen/cgreen.h>

#include "unitList.h"

#include "elm_x.h"
#include "prop_x.h"
#include "ins_x.h"
#include "adv_x.h"
#include "context_x.h"


Describe(Symbol);
BeforeEach(Symbol) {}
AfterEach(Symbol) {}


/*======================================================================

  Symbol table tests

*/

static char symbolName1[] = "n-is-in-the-middle";
static char symbolName2[] = "b-is-lower";
static char symbolName3[] = "p-is-higher";

Id *symbolId1;
Id *symbolId2;
Id *symbolId3;

Symbol *sym1;
Symbol *sym2;
Symbol *sym3;

static void initUnitTestSymbols() {
    symbolId1 = newId(nulsrcp, symbolName1);
    symbolId2 = newId(nulsrcp, symbolName2);
    symbolId3 = newId(nulsrcp, symbolName3);

    sym1 = newSymbol(symbolId1, CLASS_SYMBOL);
    sym2 = newSymbol(symbolId2, CLASS_SYMBOL);
    sym3 = newSymbol(symbolId3, CLASS_SYMBOL);
}


Ensure(Symbol, testSymCheck)
{
  Srcp srcp = {14, 12, 333};
  Id *unknownId = newId(srcp, "unknownId");
  Id *aClassId = newId(srcp, "aClassId");
  Symbol *aClassSymbol = newSymbol(aClassId, CLASS_SYMBOL);
  Id *anInstanceId = newId(srcp, "anInstanceId");
  Symbol *anInstanceSymbol = newSymbol(anInstanceId, INSTANCE_SYMBOL);
  Symbol *foundSymbol;


  foundSymbol = symcheck(unknownId, CLASS_SYMBOL, NULL);
  assert_true(foundSymbol == NULL);
  assert_true(readEcode() == 310 && readSev() == sevERR);

  foundSymbol = symcheck(aClassId, CLASS_SYMBOL, NULL);
  assert_true(foundSymbol == aClassSymbol);

  foundSymbol = symcheck(aClassId, INSTANCE_SYMBOL, NULL);
  assert_true(readEcode() == 319 && readSev() == sevERR);

  foundSymbol = symcheck(anInstanceId, INSTANCE_SYMBOL, NULL);
  assert_true(foundSymbol == anInstanceSymbol);
}


static List *createOneParameter(char *id)
{
    return newList(newParameterElement(nulsrcp, newId(nulsrcp, id), 0), ELEMENT_LIST);
}

static List *createThreeParameters(char *id1, char *id2, char *id3)
{
    return concat(
                  concat(
                         newList(newParameterElement(nulsrcp,
                                                     newId(nulsrcp, id1), 0),
                                 ELEMENT_LIST),
                         newParameterElement(nulsrcp,
                                             newId(nulsrcp, id2), 0),
                         ELEMENT_LIST),
                  newParameterElement(nulsrcp,
                                      newId(nulsrcp, id3), 0),
                  ELEMENT_LIST);
}

Ensure(Symbol, testVerbSymbols) {
  Id *v1Id = newId(nulsrcp, "v1");
  Symbol *v1Symbol = newSymbol(v1Id, VERB_SYMBOL);
  Symbol *foundSymbol;
  List *parameters, *l, *p;
  Context context;

  foundSymbol = lookup("v1");
  assert_true(foundSymbol == v1Symbol);

  parameters = createOneParameter("p1");
  setParameters(v1Symbol, parameters);
  assert_that(v1Symbol->fields.verb.parameterSymbols, is_non_null);
  for (l = v1Symbol->fields.verb.parameterSymbols,
	 p = parameters;
       l && p;
       l = l->next, p = p->next)
    assert_true(l->member.sym->fields.parameter.element == p->member.elm);

  foundSymbol = lookupInParameterList("p1", v1Symbol->fields.verb.parameterSymbols);
  assert_true(foundSymbol == v1Symbol->fields.verb.parameterSymbols->member.sym);

  context.kind = VERB_CONTEXT;
  context.verb = v1Symbol;
  foundSymbol = lookupInContext("p1", &context);
  assert_true(foundSymbol == v1Symbol->fields.verb.parameterSymbols->member.sym);

  
}


/* Test symbol table by inserting a symbol with an initial name */
Ensure(Symbol, testBuildSymbol1) {
    initUnitTestSymbols();

    sym2 = lookup(symbolName1);

    assert_true(sym1 == sym2);
    assert_true(strcmp(sym2->string, symbolName1) == 0);
    assert_true(sym2->kind == CLASS_SYMBOL);
}


/* Test symbol table by inserting a symbol with a higher name */
Ensure(Symbol, testBuildSymbolHigher) {
    Id *symbolId2 = newId(nulsrcp, symbolName2);

    Symbol *sym1 = newSymbol(symbolId2, CLASS_SYMBOL);
    Symbol *sym2 = lookup(symbolName2);
  
    assert_true(sym1 == sym2);
    assert_true(strcmp(sym2->string, symbolName2) == 0);
    assert_true(sym2->kind == CLASS_SYMBOL);
}

/* Test symbol table by inserting a symbol with a lower name */
Ensure(Symbol, testBuildSymbolLower) {
    Id *symbolId3 = newId(nulsrcp, symbolName3);

    Symbol *sym1 = newSymbol(symbolId3, CLASS_SYMBOL);
    Symbol *sym2 = lookup(symbolName3);

    assert_true(sym1 == sym2);
    assert_true(strcmp(sym2->string, symbolName3) == 0);
    assert_true(sym2->kind == CLASS_SYMBOL);
}

/* Test inheritance by setting it and retrieving it */
Ensure(Symbol, testInherit1) {
    /* Insert inheritance in alphabetical order */
    initUnitTestSymbols();

    setParent(sym1, sym2);
    setParent(sym2, sym3);

    assert_true(parentOf(sym1) == sym2);
    assert_true(parentOf(sym2) == sym3);
    assert_true(parentOf(sym3) == NULL);
}


/* Test symbol table by verifying inheritance */
Ensure(Symbol, testInherit2) {
    initUnitTestSymbols();

    setParent(sym1, sym2);
    setParent(sym2, sym3);

    assert_true(!inheritsFrom(NULL, NULL));

    assert_that(inheritsFrom(sym1, sym2), is_true);
    assert_that(inheritsFrom(sym2, sym3), is_true);
    assert_that(inheritsFrom(sym1, sym3), is_true);
    assert_that(inheritsFrom(sym3, sym3), is_true);

    assert_that(!inheritsFrom(sym3, sym1), is_true);
    assert_that(!inheritsFrom(sym3, sym2), is_true);
}


/* Test symbol table by verifying inheritance */
Ensure(Symbol, testInheritErrorSymbol) {
    Symbol *err = newSymbol(newId(nulsrcp, "ErrorSymbol"), ERROR_SYMBOL);

    initUnitTestSymbols();

    assert_true(inheritsFrom(err, sym1));
    assert_true(inheritsFrom(sym1, err));
    assert_true(inheritsFrom(err, err));
}


/* Test symbol table initialisation */
Ensure(Symbol, testSymbolTableInit) {
    Symbol *entitySymbol;
    Symbol *thingSymbol;
    Symbol *objectSymbol;
    Symbol *actorSymbol;
    Symbol *locationSymbol;
    
    initUnitTestSymbols();

    initAdventure();
    adv.whr = NULL;
    assert_true(classCount == 8);	/* Standard classes */
    assert_true(instanceCount == 1);	/* #nowhere */
    addHero(&adv);
    assert_true(instanceCount == 2);

    symbolizeAdventure();
    thingSymbol = lookup("thing");
    entitySymbol = lookup("entity");
    objectSymbol = lookup("object");
    actorSymbol = lookup("actor");
    locationSymbol = lookup("location");

    assert_true(entity->props->id->symbol == entitySymbol);
    assert_true(thing->props->id->symbol == thingSymbol);
    assert_true(object->props->id->symbol == objectSymbol);
    assert_true(location->props->id->symbol == locationSymbol);
    assert_true(actor->props->id->symbol == actorSymbol);
  
    assert_true(inheritsFrom(thingSymbol, entitySymbol));
    assert_true(inheritsFrom(thingSymbol, thingSymbol));
    assert_true(inheritsFrom(locationSymbol, entitySymbol));
    assert_true(inheritsFrom(objectSymbol, thingSymbol));
    assert_true(inheritsFrom(actorSymbol, thingSymbol));

    setParent(sym1, objectSymbol);
    setParent(sym2, actorSymbol);
    setParent(sym3, locationSymbol);

    assert_true(inheritsFrom(sym1, objectSymbol));
    assert_true(inheritsFrom(sym2, actorSymbol));
    assert_true(inheritsFrom(sym3, locationSymbol));

    assert_true(inheritsFrom(sym1, thingSymbol));
    assert_true(inheritsFrom(sym2, thingSymbol));
    assert_true(!inheritsFrom(sym3, thingSymbol));
    assert_true(inheritsFrom(sym3, entitySymbol));

}


/* Create a new CLAss symbol */
Ensure(Symbol, testCreateClassSymbol) {
    Srcp srcp = {12,3,45};
    Id *id = newId(srcp, "cla");
    Id *heritage = newId(nulsrcp, "object");
    Symbol *sym, *obj;

    (void) newClass(&srcp, heritage, NULL, NULL);
    (void) newClass(&srcp, id, heritage, NULL);

    symbolizeClasses();

    sym = lookup("cla");
    obj = lookup("object");

    assert_true(sym != NULL);
    assert_true(obj != NULL);
    assert_true(strcmp(sym->string, "cla") == 0);
    assert_true(strcmp(obj->string, "object") == 0);
    assert_true(inheritsFrom(sym, obj));
}

Ensure(Symbol, testLookupScript) {
    Symbol *classSymbol;
    Symbol *instanceSymbol;
    Id *notAScriptId = newId(nulsrcp, "notAScript");
    Id *script1Id = newId(nulsrcp, "script1");
    Id *script2Id = newId(nulsrcp, "script2");
    Id *script3Id = newId(nulsrcp, "script3");
    Id *script4Id = newId(nulsrcp, "script4");
    Script script1 = {{0,0,0}, script1Id};
    Script script2 = {{0,0,0}, script2Id};
    Script script3 = {{0,0,0}, script3Id};
    Script script4 = {{0,0,0}, script4Id};
    List *classScripts;
    List *instanceScripts;

    initAdventure();
    classSymbol = newSymbol(newId(nulsrcp, "aClass"), CLASS_SYMBOL);
    instanceSymbol = newSymbol(newId(nulsrcp, "anInstance"), INSTANCE_SYMBOL);
    setParent(instanceSymbol, classSymbol);
    classScripts = newList(&script1, SCRIPT_LIST);
    classScripts = concat(classScripts, &script2, SCRIPT_LIST);
    instanceScripts = newList(&script3, SCRIPT_LIST);
    instanceScripts = concat(instanceScripts, &script4, SCRIPT_LIST);

    classSymbol->fields.entity.props = NEW(Properties);
    classSymbol->fields.entity.props->scripts = classScripts;
    instanceSymbol->fields.entity.props = NEW(Properties);
    instanceSymbol->fields.entity.props->scripts = instanceScripts;

    assert_true(lookupScript(classSymbol, notAScriptId) == NULL);
    assert_true(lookupScript(classSymbol, script1Id) == &script1);
    assert_true(lookupScript(classSymbol, script2Id) == &script2);

    assert_true(lookupScript(instanceSymbol, notAScriptId) == NULL);
    assert_true(lookupScript(instanceSymbol, script1Id) == &script1);
    assert_true(lookupScript(instanceSymbol, script2Id) == &script2);
    assert_true(lookupScript(instanceSymbol, script3Id) == &script3);
    assert_true(lookupScript(instanceSymbol, script4Id) == &script4);
}


Ensure(Symbol, testNewFrame) {
    Symbol *verbSymbol;
    Element *element;
    Symbol *parameterSymbol;
    Symbol *localSymbol1;
    Symbol *localSymbol2;
    Id *parameterId = newId(nulsrcp, "p");
    Id *localId = newId(nulsrcp, "p");
    Context context;

    initAdventure();
    /* Create a verb v with a parameter p */
    verbSymbol = newSymbol(newId(nulsrcp, "v"), VERB_SYMBOL);
    element = newParameterElement(nulsrcp, parameterId, 0);
    parameterSymbol = newParameterSymbol(element);

    verbSymbol->fields.verb.parameterSymbols = newList(parameterSymbol, SYMBOL_LIST);
    context.kind = VERB_CONTEXT;
    context.verb = verbSymbol;
    context.previous = NULL;
    context.class = NULL;
    context.instance = NULL;

    assert_true(lookupInContext("p", &context) == parameterSymbol);

    /* Now create a new frame with a local variable "p" */
    newFrame();
    localSymbol1 = newSymbol(localId, LOCAL_SYMBOL);
    assert_true(lookupInContext("p", &context) == localSymbol1);
    newFrame();
    localSymbol2 = newSymbol(localId, LOCAL_SYMBOL);
    assert_true(lookupInContext("p", &context) == localSymbol2);
    assert_true(localSymbol2->fields.local.level == 2);
    deleteFrame();
    assert_true(lookupInContext("p", &context) == localSymbol1);
    assert_true(localSymbol1->fields.local.level == 1);
    deleteFrame();
    assert_true(lookupInContext("p", &context) == parameterSymbol);
}


Ensure(Symbol, testReplicateContainer) {
    Symbol *child = newSymbol(newId(nulsrcp, "child"), CLASS_SYMBOL);
    Symbol *parent = newSymbol(newId(nulsrcp, "parent"), CLASS_SYMBOL);
    Container *container = newContainer(newContainerBody(nulsrcp, FALSE, NULL, (void *)1, (void *)2, (void *)3, (void *)4, (void *)5));

    child->fields.entity.props = NEW(Properties);
    parent->fields.entity.props = NEW(Properties);
    setParent(child, parent);
    parent->fields.entity.props->container = container;

    replicateContainer(child);

    assert_true(child->fields.entity.props->container->body->limits == (void *)1);
    assert_true(child->fields.entity.props->container->body->hstms == (void *)2);
    assert_true(child->fields.entity.props->container->body->estms == (void *)3);
}

Ensure(Symbol, testCreateMessageVerbs) {
    Symbol *v, *p;
    Symbol *typeSymbol = newClassSymbol(newId(nulsrcp, "type"), NULL, NULL);

    v = createMessageVerb(0, typeSymbol);
    assert_true(v->kind == VERB_SYMBOL);
    assert_true(length(v->fields.verb.parameterSymbols) == 0);

    v = createMessageVerb(2, typeSymbol);
    assert_true(v->kind == VERB_SYMBOL);
    assert_true(length(v->fields.verb.parameterSymbols) == 2);

    p = v->fields.verb.parameterSymbols->member.sym;
    assert_true(p->kind == PARAMETER_SYMBOL);
    assert_true(p->fields.parameter.type == INSTANCE_TYPE);
    assert_true(p->fields.parameter.class == typeSymbol);
    p = v->fields.verb.parameterSymbols->next->member.sym;
    assert_true(p->kind == PARAMETER_SYMBOL);
    assert_true(p->fields.parameter.type == INSTANCE_TYPE);
    assert_true(p->fields.parameter.class == typeSymbol);
}

Ensure(Symbol, testInheritOpaqueAttribute) {
    /* Set up a parent class with container properties */
    Bool opaqueState = TRUE;
    ContainerBody *pBody = newContainerBody(nulsrcp, opaqueState, NULL, NULL,
                                            NULL, NULL, NULL, NULL);
    Container *pCont = newContainer(pBody);
    Properties *pProps = newProps(NULL, NULL,
                                  nulsrcp, NULL,
                                  NULL, NULL, NULL,
                                  nulsrcp, NULL, NULL,
                                  NULL, NULL,
                                  pCont, NULL,
                                  nulsrcp, NULL,
                                  NULL, NULL);
    Symbol *parent = newClassSymbol(newId(nulsrcp, "p"), pProps, NULL);

    /* Setup a child */
    List *attributes = newList(newBooleanAttribute(nulsrcp,
                                                   newId(nulsrcp, "b"),
                                                   FALSE),
                               ATTRIBUTE_LIST);
						      
    Properties *cProps = newProps(NULL, NULL,
                                  nulsrcp, NULL,
                                  attributes, NULL, NULL,
                                  nulsrcp, NULL, NULL,
                                  NULL, NULL,
                                  NULL, NULL,
                                  nulsrcp, NULL,
                                  NULL, NULL);
    Symbol *child = newInstanceSymbol(newId(nulsrcp, "c"), cProps, parent);

    replicateContainer(child);

    assert_true(length(cProps->attributes) == 2);
    assert_true(cProps->attributes->member.atr->id->code == OPAQUEATTRIBUTE); /* Predefined OPAQUE */
    assert_true(cProps->attributes->member.atr->value == opaqueState);
}

/*----------------------------------------------------------------------*/
Ensure(Symbol, testMultipleSymbolKinds) {
    assert_true(multipleSymbolKinds(0) == FALSE);
    assert_true(multipleSymbolKinds(INSTANCE_SYMBOL) == FALSE);
    assert_true(multipleSymbolKinds(INSTANCE_SYMBOL|CLASS_SYMBOL) == TRUE);
}


Ensure(Symbol, testClassToType) {
    Symbol *symbol = newClassSymbol(newId(nulsrcp, "newclass"), NULL, NULL);

    initClasses();

    assert_true(classToType(integerSymbol) == INTEGER_TYPE);
    assert_true(classToType(stringSymbol) == STRING_TYPE);
    assert_true(classToType(symbol) == INSTANCE_TYPE);
}

static Context *givenAVerbContextWithOneParameter(Id *v1Id) {
    List *parameters;
    Symbol *v1Symbol;
    Context *context;

    initAdventure();

    v1Symbol = newSymbol(v1Id, VERB_SYMBOL);
    context = newVerbContext(v1Symbol);
    parameters = createOneParameter("p1");
    setParameters(v1Symbol, parameters);
    return context;
}

static Context *givenAVerbContextWithThreeParameters(Id *v1Id) {
    List *parameters;
    Symbol *v1Symbol;
    Context *context;

    initAdventure();

    v1Symbol = newSymbol(v1Id, VERB_SYMBOL);
    context = newVerbContext(v1Symbol);
    parameters = createThreeParameters("p1", "p2", "p3");
    setParameters(v1Symbol, parameters);
    return context;
}


Ensure(Symbol, testParameterReference) {
    Id *p1Id = newId(nulsrcp, "p1");
    Symbol *foundSymbol;
    Id *v1Id = newId(nulsrcp, "v1");
    Context *context;

    context = givenAVerbContextWithOneParameter(v1Id);

    /* Parameter not found if not in verb context */
    foundSymbol = symcheck(p1Id, INSTANCE_SYMBOL, NULL);
    assert_true(foundSymbol == NULL);
    assert_true(readEcode() == 310 && readSev() == sevERR); /* Not found! */

    foundSymbol = symcheck(p1Id, INSTANCE_SYMBOL, context);
    assert_true(foundSymbol != NULL);
    assert_true(foundSymbol->kind == PARAMETER_SYMBOL);
    assert_true(equalId(foundSymbol->fields.parameter.element->id, p1Id));
}

Ensure(Symbol, testCanFindParametersFromVerbContext) {
    List *parameterSymbols;
    Id *p1Id = newId(nulsrcp, "p1");
    Id *v1Id = newId(nulsrcp, "v1");
    Context *context;

    context = givenAVerbContextWithOneParameter(v1Id);

    parameterSymbols = getParameterSymbols(context);
    assert_true(length(parameterSymbols) == 1);
    assert_true(strcmp(parameterSymbols->member.sym->string, p1Id->string) == 0);
}

Ensure(Symbol, testCanListParametersFromVerbContext) {
    List *parameterSymbols;
    Id *v1Id = newId(nulsrcp, "v1");
    Context *context;

    context = givenAVerbContextWithThreeParameters(v1Id);

    parameterSymbols = getParameterSymbols(context);
    assert_true(length(parameterSymbols) == 3);
    assert_true(strcmp(identifierListForParameters(context), "'p1', 'p2' and 'p3'") == 0);
}


TestSuite *symTests()
{
    TestSuite *suite = create_test_suite(); 

    add_test_with_context(suite, Symbol, testMultipleSymbolKinds);
    add_test_with_context(suite, Symbol, testSymCheck);
    add_test_with_context(suite, Symbol, testBuildSymbol1);
    add_test_with_context(suite, Symbol, testBuildSymbolHigher);
    add_test_with_context(suite, Symbol, testBuildSymbolLower);
    add_test_with_context(suite, Symbol, testInherit1);
    add_test_with_context(suite, Symbol, testInherit2);
    add_test_with_context(suite, Symbol, testInheritErrorSymbol);
    add_test_with_context(suite, Symbol, testSymbolTableInit);
    add_test_with_context(suite, Symbol, testCreateClassSymbol);
    add_test_with_context(suite, Symbol, testVerbSymbols);
    add_test_with_context(suite, Symbol, testLookupScript);
    add_test_with_context(suite, Symbol, testNewFrame);
    add_test_with_context(suite, Symbol, testReplicateContainer);
    add_test_with_context(suite, Symbol, testCreateMessageVerbs);
    add_test_with_context(suite, Symbol, testInheritOpaqueAttribute);
    add_test_with_context(suite, Symbol, testClassToType);
    add_test_with_context(suite, Symbol, testParameterReference);
    add_test_with_context(suite, Symbol, testCanFindParametersFromVerbContext);
    add_test_with_context(suite, Symbol, testCanListParametersFromVerbContext);

    return suite;
}

