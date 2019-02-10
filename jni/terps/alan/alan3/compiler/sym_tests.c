#include <cgreen/cgreen.h>

#include "sym_x.h"

#include "util.h"

#include "smScSema.mock"
#include "lmList.mock"
#include "atr.mock"
#include "str.mock"
#include "prop.mock"
#include "ext.mock"
#include "id.mock"
#include "cnt.mock"
#include "srcp.mock"
#include "elm.mock"
#include "emit.mock"


Describe(Symbol);

BeforeEach(Symbol) {
    entitySymbol = newSymbol(newId(nulsrcp, "entity"), CLASS_SYMBOL);
    entitySymbol->fields.entity.parent = NULL;
    locationSymbol = newSymbol(newId(nulsrcp, "location"), CLASS_SYMBOL);
    locationSymbol->fields.entity.parent = entitySymbol;
    thingSymbol = newSymbol(newId(nulsrcp, "thing"), CLASS_SYMBOL);
    thingSymbol->fields.entity.parent = entitySymbol;
    objectSymbol = newSymbol(newId(nulsrcp, "object"), CLASS_SYMBOL);
    objectSymbol->fields.entity.parent = thingSymbol;
    actorSymbol = newSymbol(newId(nulsrcp, "actor"), CLASS_SYMBOL);
    actorSymbol->fields.entity.parent = thingSymbol;

    literalSymbol = newSymbol(newId(nulsrcp, "literal"), CLASS_SYMBOL);
    thingSymbol->fields.entity.parent = entitySymbol;
    integerSymbol = newSymbol(newId(nulsrcp, "integer"), CLASS_SYMBOL);
    integerSymbol->fields.entity.parent = literalSymbol;
    stringSymbol = newSymbol(newId(nulsrcp, "string"), CLASS_SYMBOL);
    stringSymbol->fields.entity.parent = literalSymbol;
}

AfterEach(Symbol) {}

Ensure(Symbol, can_return_class_of_symbol_for_instance) {
    Symbol *parent = NEW(Symbol);
    Id *id = newId(nulsrcp, "classname");
    Symbol *symbol = newInstanceSymbol(id, NULL, parent);

    assert_that(classOfSymbol(symbol), is_equal_to(parent));
}

Ensure(Symbol, can_return_class_of_symbol_for_parameter) {
    Symbol *parent = NEW(Symbol);
    Id *id = newId(nulsrcp, "classname");
    Element *element = newParameterElement(nulsrcp, id, 0);
    Symbol *symbol = newParameterSymbol(element);
    symbol->fields.parameter.class = parent;

    assert_that(classOfSymbol(symbol), is_equal_to(parent));
}

Ensure(Symbol, can_return_class_of_symbol_for_local) {
    Symbol *parent = NEW(Symbol);
    Id *id = newId(nulsrcp, "classname");
    Element *element = newParameterElement(nulsrcp, id, 0);
    Symbol *symbol = newParameterSymbol(element);
    symbol->fields.parameter.class = parent;

    assert_that(classOfSymbol(symbol), is_equal_to(parent));
}


static void mocked_syserr(char *string) {
    mock(string);
}

Ensure(Symbol, cannot_return_class_of_symbol_for_verb) {
    Id *id = newId(nulsrcp, "verbname");
    Symbol *symbol = newVerbSymbol(id);

    setSyserrHandler(mocked_syserr);
    expect(mocked_syserr);

    classOfSymbol(symbol);
}

Ensure(Symbol, can_get_type_according_to_symbol) {
    assert_that(basicTypeFromClassSymbol(integerSymbol), is_equal_to(INTEGER_TYPE));
}

Ensure(Symbol, can_get_type_of_a_local_or_parameter_symbol) {
    Symbol *local_symbol = NEW(Symbol);
    Symbol *parameter_symbol = NEW(Symbol);

    local_symbol->kind = LOCAL_SYMBOL;
    parameter_symbol->kind = PARAMETER_SYMBOL;

    local_symbol->fields.local.type = STRING_TYPE;
    parameter_symbol->fields.parameter.type = INTEGER_TYPE;

    assert_that(typeOfSymbol(local_symbol), is_equal_to(STRING_TYPE));
    assert_that(typeOfSymbol(parameter_symbol), is_equal_to(INTEGER_TYPE));
}

Ensure(Symbol, should_return_the_class_as_common_parent_for_same_class) {
    Symbol *parent = newClassSymbol(newId(nulsrcp, "class"), NULL, NULL);

    assert_that(commonParent(parent, parent), is_equal_to(parent));
}

Ensure(Symbol, should_return_the_parent_as_common_parent_if_one_is_child) {
    Symbol *parent = newClassSymbol(newId(nulsrcp, "parent"), NULL, NULL);
    Symbol *child = newClassSymbol(newId(nulsrcp, "child"), NULL, parent);

    assert_that(commonParent(parent, child), is_equal_to(parent));
    assert_that(commonParent(child, parent), is_equal_to(parent));
}

Ensure(Symbol, should_return_the_parent_as_common_parent_if_one_is_instance) {
    Symbol *parent = newClassSymbol(newId(nulsrcp, "class"), NULL, NULL);
    Symbol *instance = newInstanceSymbol(newId(nulsrcp, "instance"), NULL, parent);

    assert_that(commonParent(parent, instance), is_equal_to(parent));
    assert_that(commonParent(instance, parent), is_equal_to(parent));
}

Ensure(Symbol, should_return_the_common_parent_as_common_parent_if_both_have_same_parent) {
    Symbol *parent = newClassSymbol(newId(nulsrcp, "parent"), NULL, NULL);
    Symbol *child1 = newClassSymbol(newId(nulsrcp, "child1"), NULL, parent);
    Symbol *child2 = newClassSymbol(newId(nulsrcp, "child2"), NULL, parent);

    assert_that(commonParent(child1, child2), is_equal_to(parent));
    assert_that(commonParent(child2, child1), is_equal_to(parent));
}


static Properties *createContainerPropertiesTaking(Symbol *taken_class) {
    ContainerBody *containerBody = NEW(ContainerBody);
    containerBody->taking = newId(nulsrcp, "");
    containerBody->taking->symbol = taken_class;

    Container *container = NEW(Container);
    container->body = containerBody;

    return newProps(NULL, NULL, nulsrcp, NULL, NULL,
                    NULL, NULL, nulsrcp, NULL, NULL,
                    NULL, NULL, container, NULL, nulsrcp,
                    NULL, NULL, NULL);
}


static Id *givenAClassId(char *name) {
    Id *class_id = newId(nulsrcp, name);
    Symbol *class_symbol = newClassSymbol(class_id, NULL, NULL);
    class_id->symbol = class_symbol;
    return class_id;
}


static Symbol *givenAClass(char *name) {
    return newClassSymbol(newId(nulsrcp, name), NULL, NULL);
}


static Symbol *givenAnInstance(char *name) {
    return newInstanceSymbol(newId(nulsrcp, name), NULL, NULL);
}


static Symbol *givenAnInstanceSymbolInheritingFrom(char *name, Symbol *parent) {
    Symbol *symbol = newInstanceSymbol(newId(nulsrcp, name), NULL, parent);
    return symbol;
}


static Symbol *givenAnInstanceTaking(char *name, Symbol *taken_class) {
    Properties *properties = createContainerPropertiesTaking(taken_class);
    Symbol *symbol = newInstanceSymbol(newId(nulsrcp, name), properties, NULL);
    return symbol;
}


static Symbol *givenAClassTaking(char *name, Symbol *taken_class) {
    Properties *properties = createContainerPropertiesTaking(taken_class);
    Symbol *symbol = newClassSymbol(newId(nulsrcp, name), properties, NULL);
    return symbol;
}


/* Story: Instance exist of class or subclass */

Ensure(Symbol, can_find_no_existing_instances_of_a_class_if_there_are_none) {
    assert_that(instancesExist(objectSymbol), is_null);

    givenAnInstanceSymbolInheritingFrom("a", actorSymbol);
    assert_that(instancesExist(objectSymbol), is_null);
}

Ensure(Symbol, can_find_existing_instance_of_a_class) {
    givenAnInstanceSymbolInheritingFrom("instance", objectSymbol);
    assert_that(instancesExist(objectSymbol));
}

Ensure(Symbol, can_find_existing_instance_of_a_subclass) {
    givenAnInstanceSymbolInheritingFrom("o", objectSymbol);
    assert_that(instancesExist(thingSymbol));
}


/* STORY: Find common ancestor of two classes */
Ensure(Symbol, should_find_thing_as_common_ancestor_to_object_and_actor) {
    assert_that(commonParent(objectSymbol, actorSymbol), is_equal_to(thingSymbol));
}



/* STORY: Calculate what a container might contain */

/* In the following rather complex structures need to be set up and
   the name of the tests get *very* long, so this is the notation
   used:

   C<c>     : Class <c>, is parent to C<c+1>
   C<c><n> : nth subclass to C<c-1>
   I<c>     : Instance of class C<c>
   I<c><n> : nth instance of class C<c>
   Concatenation with '_' is "taking" and "__" exists instances of
   I1_C3     : is an instance of C1 taking C3
   I1_C3__I3 : is an instance of C1 taking C3 where there is an instance I3
*/


Ensure(Symbol, returns_null_as_possible_content_if_null) {
    assert_that(containerMightContain(NULL), is_null);
}


/* If there's only a single container instance it can only contain what it takes */
Ensure(Symbol, calculates_content_of_I1_C2_to_C2) {
    Symbol *c2 = givenAClass("c2");
    Symbol *i1 = givenAnInstanceTaking("i1", c2);

    calculateTransitiveContainerContents();

    assert_that(containerMightContain(i1), is_equal_to(c2));
}


Ensure(Symbol, calculates_content_of_I1_C2_I2_C3_to_C3) {
    Symbol *c2 = givenAClass("c2");
    Symbol *c3 = givenAClass("c3");
    setParent(c3, c2);

    Symbol *i1 = givenAnInstanceTaking("i1", c2);

    Symbol *i2 = givenAnInstanceTaking("i2", c3);
    setParent(i2, c2);

    calculateTransitiveContainerContents();

    assert_that(containerMightContain(i1), is_equal_to(c2));
}


Ensure(Symbol, calculates_content_of_I1_C3_I3_C2_to_C2) {
    Symbol *c2 = givenAClass("c2");
    Symbol *c3 = givenAClass("c3");
    setParent(c3, c2);

    Symbol *i1 = givenAnInstanceTaking("i1", c3);

    Symbol *i3 = givenAnInstanceTaking("i3", c2);
    setParent(i3, c3);

    calculateTransitiveContainerContents();

    assert_that(containerMightContain(i1), is_equal_to(c2));
}


Ensure(Symbol, calculates_content_of_I1_C3_I3_C2_I2_C3_to_C2) {
    Symbol *c2 = givenAClass("c2");
    Symbol *c3 = givenAClass("c3");
    setParent(c3, c2);

    Symbol *i1 = givenAnInstanceTaking("i1", c3);

    Symbol *i3 = givenAnInstanceTaking("i3", c2);
    setParent(i3, c3);

    Symbol *i2 = givenAnInstanceTaking("i2", c3);
    setParent(i2, c2);

    calculateTransitiveContainerContents();

    assert_that(containerMightContain(i1), is_equal_to(c2));
}


Ensure(Symbol, calculates_content_of_I1_C2_I2_C3_I3_C2_to_C2) {
    Symbol *c2 = givenAClass("c2");
    Symbol *c3 = givenAClass("c3");
    setParent(c3, c2);

    Symbol *i1 = givenAnInstanceTaking("i1", c2);

    Symbol *i2 = givenAnInstanceTaking("i2", c3);
    setParent(i2, c2);

    Symbol *i3 = givenAnInstanceTaking("i3", c2);
    setParent(i3, c3);

    calculateTransitiveContainerContents();

    assert_that(containerMightContain(i1), is_equal_to(c2));
}


Ensure(Symbol, calculates_content_of_I1_C2_C3_to_C2) {
    Symbol *c3 = givenAClass("c3");
    Symbol *c2 = givenAClassTaking("c2", c3);
    setParent(c3, c2);

    Symbol *i1 = givenAnInstanceTaking("i1", c2);

    calculateTransitiveContainerContents();

    assert_that(containerMightContain(i1), is_equal_to(c2));
}


Ensure(Symbol, can_calculate_content_even_when_there_are_non_entity_symbols) {
    newSymbol(newId(nulsrcp, "v"), VERB_SYMBOL);
    calculateTransitiveContainerContents();
}


Ensure(Symbol, can_calculate_content_when_parent_is_container) {
    Symbol *c1 = givenAClassTaking("c1", objectSymbol);
    Symbol *c2 = givenAClass("c2");
    setParent(c2, c1);

    calculateTransitiveContainerContents();
    assert_that(containerMightContain(c2), is_equal_to(objectSymbol));
}


Ensure(Symbol, can_calculate_content_when_parent_is_container_and_create_before) {
    Symbol *c2 = givenAClass("c2");
    Symbol *c1 = givenAClassTaking("c1", objectSymbol);
    setParent(c2, c1);

    calculateTransitiveContainerContents();
    assert_that(containerMightContain(c2), is_equal_to(objectSymbol));
}


Ensure(Symbol, can_calculate_content_when_parent_of_contained_class_is_container) {
    Symbol *cont = givenAClassTaking("cont", objectSymbol);
    setParent(cont, objectSymbol);

    Symbol *c = givenAnInstance("c");
    setParent(c, cont);

    calculateTransitiveContainerContents();
    assert_that(containerMightContain(c), is_equal_to(objectSymbol));
}

Ensure(Symbol, can_calculate_contained_class_for_parameter) {
    Symbol *c1 = givenAClassTaking("c1", objectSymbol);
    Id *p = newId(nulsrcp, "p");
    Element *element = NEW(Element);
    element->id = p;
    Symbol *parameter = newParameterSymbol(element);
    parameter->fields.parameter.class = c1;

    calculateTransitiveContainerContents();

    assert_that(containerMightContain(parameter), is_equal_to(objectSymbol));
}



/* STORY: Iterate over all instances of a class */
Ensure(Symbol, returns_no_instances_if_there_are_none) {
    SymbolIterator iterator = createSymbolIterator();
    assert_that(getNextInstanceOf(iterator, objectSymbol), is_null);
}

Ensure(Symbol, returns_two_instances_if_there_are_two) {
    Symbol *i1 = givenAnInstanceSymbolInheritingFrom("i1", objectSymbol);
    Symbol *i2 = givenAnInstanceSymbolInheritingFrom("i2", objectSymbol);

    SymbolIterator iterator = createSymbolIterator();

    Symbol *s1 = getNextInstanceOf(iterator, objectSymbol);
    Symbol *s2 = getNextInstanceOf(iterator, objectSymbol);

    if (s1 == i1)
        assert_that(s2, is_equal_to(i2));
    else {
        assert_that(s1, is_equal_to(i2));
        assert_that(s2, is_equal_to(i1));
    }
    assert_that(getNextInstanceOf(iterator, objectSymbol), is_null);
}


Ensure(Symbol, returns_two_instances_if_one_is_a_subclass) {
    Symbol *i1 = givenAnInstanceSymbolInheritingFrom("i1", objectSymbol);
    Symbol *subclass = givenAClass("subclass");
    setParent(subclass, objectSymbol);
    Symbol *i2 = givenAnInstanceSymbolInheritingFrom("i2", subclass);

    SymbolIterator iterator = createSymbolIterator();

    Symbol *s1 = getNextInstanceOf(iterator, objectSymbol);
    Symbol *s2 = getNextInstanceOf(iterator, objectSymbol);

    if (s1 == i1)
        assert_that(s2, is_equal_to(i2));
    else {
        assert_that(s1, is_equal_to(i2));
        assert_that(s2, is_equal_to(i1));
    }
    assert_that(getNextInstanceOf(iterator, objectSymbol), is_null);
}


/* STORY: The class taken by a container */

Ensure(Symbol, should_return_null_as_taken_class_for_class_with_no_properties) {
    /* Given: some class that is not a container with no properties */
    Id *theClass = givenAClassId("non_container");

    /* Then: containerSymbolTakes() should return NULL for that class symbol */
    assert_that(containerSymbolTakes(theClass->symbol), is_null);
}


Ensure(Symbol, should_return_taken_class_for_container_instance) {
    /* Given: there is some class */
	Id *taken_class_id = givenAClassId("taken");

    /* And: there is an instance taking that class */
    Symbol *queryed_symbol = givenAnInstanceTaking("queryed_instance_symbol", taken_class_id->symbol);

    /* When: containerSymbolTakes() is called on that instance */
    /* Then: the taken class is returned */
    assert_that(containerSymbolTakes(queryed_symbol), is_equal_to(taken_class_id->symbol));
}


Ensure(Symbol, should_return_taken_class_for_container_class) {
    /* Given: there is some class */
	Id *taken_class_id = givenAClassId("taken");

    /* And: there is an class taking that class */
    Symbol *queryed_symbol = givenAClassTaking("queryed_instance_symbol", taken_class_id->symbol);

    /* When: containerSymbolTakes() is called on that class */
    /* Then: the taken class is returned */
    assert_that(containerSymbolTakes(queryed_symbol), is_equal_to(taken_class_id->symbol));
}


Ensure(Symbol, should_return_taken_class_for_instance_inheriting_container) {
    /* Given: there is some class */
	Id *taken_class_id = givenAClassId("taken");

    /* And: there is n class taking that class */
    Symbol *container_class_symbol = givenAClassTaking("container_class", taken_class_id->symbol);

    /* And: there is an instance of that class */
    Symbol *queryed_symbol = newInstanceSymbol(newId(nulsrcp, "instance"), newEmptyProps(), container_class_symbol);

    /* When: containerSymbolTakes() is called on that instance */
    /* Then: the taken class is returned */
    assert_that(containerSymbolTakes(queryed_symbol), is_equal_to(taken_class_id->symbol));
}


Ensure(Symbol, should_return_taken_class_for_instance_of_class_inheriting_container) {
    /* Given: there is some class */
	Id *taken_class_id = givenAClassId("taken");

    /* And: there is n class taking that class */
    Symbol *container_class_symbol = givenAClassTaking("container_class", taken_class_id->symbol);

    /* And: there is a class inheriting from that class */
    Symbol *inherited_container_class_symbol = newInstanceSymbol(newId(nulsrcp, "inherited_container_class"),
                                                                 newEmptyProps(), container_class_symbol);

    /* And: there is an instance of that class */
    Symbol *queryed_symbol = newInstanceSymbol(newId(nulsrcp, "instance"), newEmptyProps(), inherited_container_class_symbol);

    /* When: containerSymbolTakes() is called on that instance */
    /* Then: the taken class is returned */
    assert_that(containerSymbolTakes(queryed_symbol), is_equal_to(taken_class_id->symbol));
}

Ensure(Symbol, can_say_a_null_pointer_is_not_an_actor) {
    /* Given: a null pointer */

    /* When: testing for actor */
    assert_that(!symbolIsActor(NULL));
}

Ensure(Symbol, can_say_the_actorSymbol_is_an_actor) {
    /* Given: a pointer to the symbol */

    /* When: testing for actor */
    assert_that(symbolIsActor(actorSymbol));
}

Ensure(Symbol, can_say_a_verb_symbol_is_not_an_actor) {
    /* Given: a pointer to the symbol */
    Symbol *symbol = newSymbol(newId(nulsrcp, "myVerb"), VERB_SYMBOL);

    /* When: testing for actor */
    assert_that(!symbolIsActor(symbol));
}

Ensure(Symbol, can_say_a_direct_descendant_of_actor_symbol_is_an_actor) {
    /* Given: a pointer to an actor symbol */
    Symbol *symbol = newSymbol(newId(nulsrcp, "myActor"), INSTANCE_SYMBOL);
    symbol->fields.entity.parent = actorSymbol;

    /* When: testing for actor */
    assert_that(symbolIsActor(symbol));
}

Ensure(Symbol, can_say_a_indirect_descendant_of_actor_symbol_is_an_actor) {
    /* Given: a pointer to a symbol inheriting from a symbol inheriting from actor */
    Symbol *symbol1 = newSymbol(newId(nulsrcp, "myActor1"), INSTANCE_SYMBOL);
    Symbol *symbol2 = newSymbol(newId(nulsrcp, "myActor2"), INSTANCE_SYMBOL);
    Symbol *symbol3 = newSymbol(newId(nulsrcp, "myActor3"), INSTANCE_SYMBOL);
    symbol1->fields.entity.parent = actorSymbol;
    symbol2->fields.entity.parent = symbol1;
    symbol3->fields.entity.parent = symbol2;

    /* When: testing for actor */
    assert_that(symbolIsActor(symbol3));
}
