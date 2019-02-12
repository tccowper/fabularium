/*======================================================================*\

  atrTest.c

  Unit tests for ATR node in the Alan compiler

\*======================================================================*/

#include "atr.c"

#include <cgreen/cgreen.h>

#include "unitList.h"

#include "ins_x.h"
#include "cla_x.h"
#include "wht_x.h"
#include "prop_x.h"
#include "context_x.h"

Describe(Attribute);
BeforeEach(Attribute) {}
AfterEach(Attribute) {}


Ensure(Attribute, testCreateSetAttribute)
{
  List *set = newList(newIntegerExpression(nulsrcp, 1), EXPRESSION_LIST);
  Expression *setExp = newSetExpression(nulsrcp, set);
  Attribute *atr = newSetAttribute(nulsrcp, newId(nulsrcp, "setAttribute"), setExp);
  assert_true(atr->type == SET_TYPE);
  assert_true(length(atr->set->fields.set.members) == 1);
  assert_true(atr->set->fields.set.members->member.exp->kind == INTEGER_EXPRESSION);
}

Ensure(Attribute, testSingleIdentifierInMember) {
  Expression *exp1 = newWhatExpression(nulsrcp, newWhatId(nulsrcp, newId(nulsrcp, "what")));
  Expression *exp2 = newWhatExpression(nulsrcp, newWhatThis(nulsrcp));

  assert_true(!hasSingleIdentifierMember(NULL));
  assert_true(hasSingleIdentifierMember(newList(exp1, EXPRESSION_LIST)));
  assert_true(!hasSingleIdentifierMember(newList(exp2, EXPRESSION_LIST)));
  assert_true(!hasSingleIdentifierMember(concat(newList(exp1, EXPRESSION_LIST), exp2, EXPRESSION_LIST)));
}

Ensure(Attribute, testIsWhatId) {
  Expression *exp1 = newWhatExpression(nulsrcp, newWhatThis(nulsrcp));
  Expression *exp2 = newWhatExpression(nulsrcp, newWhatId(nulsrcp, newId(nulsrcp, "what")));
  Expression *exp3 = newBetweenExpression(nulsrcp, NULL, FALSE, NULL, NULL);
  
  assert_true(!isWhatId(exp1));
  assert_true(isWhatId(exp2));
  assert_true(!isWhatId(exp3));
}

Ensure(Attribute, testInferClassInSetAttribute)
{
  initAdventure();
  symbolizeClasses();

  Id *classId = newId(nulsrcp, "object");
  Instance *instance = newInstance(&nulsrcp, newId(nulsrcp, "t"), classId, NULL);
  List *set = newList(newWhatExpression(nulsrcp, newWhatId(nulsrcp, newId(nulsrcp, "t"))), EXPRESSION_LIST);
  Expression *setExp = newSetExpression(nulsrcp, set);
  Attribute *atr = newSetAttribute(nulsrcp, newId(nulsrcp, "setAttribute"), setExp);

  symbolizeProps(instance->props, FALSE);
  analyzeSetAttribute(atr, newNullContext());
  assert_true(atr->type == SET_TYPE);
  assert_true(atr->setType == INSTANCE_TYPE);
  assert_true(atr->setClass == objectSymbol);
  assert_true(length(atr->set->fields.set.members) == 1);

  classId = newId(nulsrcp, "location");
  instance = newInstance(&nulsrcp, newId(nulsrcp, "u"), classId, NULL);
  set = concat(set,
	       newWhatExpression(nulsrcp,
				 newWhatId(nulsrcp, newId(nulsrcp, "u"))),
	       EXPRESSION_LIST);

  symbolizeProps(instance->props, FALSE);
  analyzeSetAttribute(atr, newNullContext());
  assert_true(atr->type == SET_TYPE);
  assert_true(atr->setType == INSTANCE_TYPE);
  assert_true(atr->setClass == entitySymbol);
  assert_true(length(atr->set->fields.set.members) == 2);
}

Ensure(Attribute, testMultipleAtr)
{
  List *attributeList;

  attributeList = newList(newBooleanAttribute(nulsrcp, newId(nulsrcp, "anAttr"), FALSE), ATTRIBUTE_LIST);
  attributeList = concat(attributeList, newBooleanAttribute(nulsrcp, newId(nulsrcp, "anAttr"), FALSE), ATTRIBUTE_LIST);

  readEcode();
  checkMultipleAttributes(attributeList);
  assert_true(readEcode() == 218 && readSev() == sevERR);
}


Ensure(Attribute, testFindInList)
{
  List *attributes = NULL;
  Id *id = newId(nulsrcp, "theAttribute");
  Attribute *theAttribute = newBooleanAttribute(nulsrcp, id, FALSE);
  Attribute *anotherAttribute = newBooleanAttribute(nulsrcp, newId(nulsrcp, "another"), FALSE);

  /* Test empty list */
  assert_true(findAttribute(attributes, id) == NULL);

  /* Test one element */
  attributes = concat(attributes, theAttribute, ATTRIBUTE_LIST);
  assert_true(findAttribute(attributes, id) == theAttribute);

  /* Test last element */
  attributes = combine(newList(anotherAttribute, ATTRIBUTE_LIST), attributes);
  attributes = combine(newList(anotherAttribute, ATTRIBUTE_LIST), attributes);
  assert_true(findAttribute(attributes, id) == theAttribute);

  /* Test in the middle */
  attributes = concat(attributes, anotherAttribute, ATTRIBUTE_LIST);
  assert_true(findAttribute(attributes, id) == theAttribute);
}

static Class *createClass(char string[], List *attributes)
{
  Properties *props = newEmptyProps();
  Class *theClass;

  props->attributes = attributes;
  theClass = newClass(&nulsrcp, newId(nulsrcp, string), NULL, props);
  return theClass;
}

static Instance *createInstance(char string[], List *attributes)
{
  Properties *props = newEmptyProps();
  Instance *theInstance;

  props->attributes = attributes;
  theInstance = newInstance(&nulsrcp, newId(nulsrcp, string), NULL, props);
  return theInstance;
}

static List *create2Attributes(char firstString[], char secondString[])
{
  List *theList;

  theList = newList(newBooleanAttribute(nulsrcp, newId(nulsrcp, firstString), FALSE), ATTRIBUTE_LIST);
  theList = concat(theList, newBooleanAttribute(nulsrcp, newId(nulsrcp, secondString), FALSE), ATTRIBUTE_LIST);
  return theList;
}

static int attributeCode(Properties *props, char *string)
{
  Attribute *atr = findAttribute(props->attributes, newId(nulsrcp, string));
  return atr->id->code;
}


static void numberTheAttributes(List *aList, int n1, int n2)
{
  aList->member.atr->id->code = n1;
  aList->next->member.atr->id->code = n2;
}


static Bool equalLists(List *list1, List *list2)
{
  List *t1 = list1;
  List *t2 = list2;

  while (t1 != NULL && t2 != NULL && t1->member.atr->id->code && t2->member.atr->id->code) {
    t1 = t1->next;
    t2 = t2->next;
  }
  return t1 == NULL && t2 == NULL;
}

Ensure(Attribute, testCombineAttributes)
{
  List *ownList = create2Attributes("x", "y");
  List *inheritedList = create2Attributes("y", "z");
  List *theCombinedList;

  assert_true(combineAttributes(NULL, NULL) == NULL);

  numberTheAttributes(ownList, 1, 2);
  numberTheAttributes(inheritedList, 2, 3);
  theCombinedList = combineAttributes(ownList, NULL);
  assert_true(length(theCombinedList) == length(ownList));
  assert_true(theCombinedList == ownList);

  theCombinedList = combineAttributes(NULL, inheritedList);
  assert_true(length(theCombinedList) == length(inheritedList));
  assert_true(equalLists(theCombinedList, inheritedList));

  theCombinedList = combineAttributes(ownList, inheritedList);
  assert_true(length(theCombinedList) == 3);
}


Ensure(Attribute, testAttributeListsInSymbolTable)
{
  Class *firstClass, *secondClass;
  List *firstClassAttributes, *secondClassAttributes, *firstInstanceAttributes, *secondInstanceAttributes;
  Symbol *firstClassSymbol, *secondClassSymbol, *firstInstanceSymbol, *secondInstanceSymbol;
  int x, y, z;
  Instance *firstInstance, *secondInstance;


  initAdventure();
  firstClassAttributes = create2Attributes("a1", "a12");
  secondClassAttributes = create2Attributes("a1", "a21");

  firstClass = createClass("firstClass", firstClassAttributes);
  secondClass = createClass("secondClass", secondClassAttributes);

  firstClassSymbol = lookup("firstClass");
  assert_true(firstClassSymbol->fields.entity.props->attributes == firstClassAttributes);
  secondClassSymbol = lookup("secondClass");
  assert_true(secondClassSymbol->fields.entity.props->attributes == secondClassAttributes);
  
  firstInstanceAttributes = create2Attributes("a11", "a12");
  secondInstanceAttributes = create2Attributes("a1", "a22");

  firstInstance = createInstance("firstInstance", firstInstanceAttributes);
  secondInstance = createInstance("secondInstance", secondInstanceAttributes);

  firstInstanceSymbol = lookup("firstInstance");
  assert_true(firstInstanceSymbol->fields.entity.props->attributes == firstInstanceAttributes);
  secondInstanceSymbol = lookup("secondInstance");
  assert_true(secondInstanceSymbol->fields.entity.props->attributes == secondInstanceAttributes);

  /* Now set up a class hierarchy:
  location
     !
     fC = a1 + a12 -----+
     !                  !  
     fI = a11 + a12	sC = a1 + a21
                        !
                        sI = a1 + a22
  */
  setParent(firstClassSymbol, location->props->id->symbol);
  setParent(secondClassSymbol, firstClassSymbol);
  setParent(firstInstanceSymbol, firstClassSymbol);
  setParent(secondInstanceSymbol, secondClassSymbol);

  numberAllAttributes();

  assert_true(attributeCode(firstClass->props, "a1") != 0);
  assert_true(attributeCode(firstClass->props, "a12") != 0);
  assert_true(attributeCode(secondClass->props, "a1") != 0);
  assert_true(attributeCode(secondClass->props, "a21") != 0);
  assert_true(attributeCode(firstInstance->props, "a11") != 0);
  assert_true(attributeCode(firstInstance->props, "a12") != 0);
  assert_true(attributeCode(secondInstance->props, "a1") != 0);
  assert_true(attributeCode(secondInstance->props, "a22") != 0);

  assert_true(attributeCode(firstClass->props, "a1") != attributeCode(firstClass->props, "a12"));
  assert_true(attributeCode(secondClass->props, "a1") != attributeCode(secondClass->props, "a21"));
  assert_true(attributeCode(firstInstance->props, "a11") != attributeCode(firstInstance->props, "a12"));
  assert_true(attributeCode(secondInstance->props, "a1") != attributeCode(secondInstance->props, "a22"));

  x = attributeCode(firstClass->props, "a1");
  assert_true(attributeCode(secondClass->props, "a1") == x);
  assert_true(attributeCode(secondInstance->props, "a1") == x);

  y = attributeCode(firstClass->props, "a12");
  assert_true(attributeCode(firstInstance->props, "a12") == y);

  z = attributeCode(secondClass->props, "a21");
  assert_true(attributeCode(secondInstance->props, "a22") != z);
}


static void numberAttributes123(List *l)
{
  l->member.atr->id->code = 1;
  l->next->member.atr->id->code = 2;
  l->next->next->member.atr->id->code = 3;
}

static void numberAttributes321(List *l)
{
  l->member.atr->id->code = 3;
  l->next->member.atr->id->code = 2;
  l->next->next->member.atr->id->code = 1;
}

static void numberAttributes231(List *l)
{
  l->member.atr->id->code = 2;
  l->next->member.atr->id->code = 3;
  l->next->next->member.atr->id->code = 1;
}

static Bool attributesAreSorted(List *list)
{
  List *l;
  int previousCode = 0;

  for (l = list; l; l = l->next) {
    if (l->member.atr->id->code <= previousCode)
      return FALSE;
    previousCode = l->member.atr->id->code;
  }
  return TRUE;
}

Ensure(Attribute, testSortAttributes)
{
  List *attributeList = newList(newBooleanAttribute(nulsrcp, newId(nulsrcp, "a"), FALSE), ATTRIBUTE_LIST);
  List *originalList = attributeList;

  assert_true(sortAttributes(NULL) == NULL);
  assert_true(sortAttributes(attributeList) == originalList);

  attributeList = combine(attributeList, create2Attributes("x", "y"));
  numberAttributes123(attributeList);
  attributeList = sortAttributes(attributeList);
  assert_true(attributesAreSorted(attributeList));

  numberAttributes321(attributeList);
  attributeList = sortAttributes(attributeList);
  assert_true(attributesAreSorted(attributeList));

  numberAttributes231(attributeList);
  attributeList = sortAttributes(attributeList);
  assert_true(attributesAreSorted(attributeList));
}

Ensure(Attribute, testGenerateAttributes)
{
  int attributeEntrySize = AwordSizeOf(AttributeEntry);
  int address;
  Instance *firstInstance;
  Aword buffer[100];

  firstInstance = createInstance("firstInstance", create2Attributes("a11", "a12"));

  initEmitBuffer(buffer);

  attributeAreaSize = 0;
  address = generateAttributes(firstInstance->props->attributes, 1);
  assert_true(nextEmitAddress() == address + 2*attributeEntrySize + 1);
  assert_true(attributeAreaSize == 2*attributeEntrySize+1);
}

Ensure(Attribute, testResolveThisAttributeForClass)
{
  List *theAttributes = create2Attributes("x", "y");
  Properties *theProps = newProps(NULL, NULL,
				  nulsrcp, NULL,
				  theAttributes, NULL, NULL,
				  nulsrcp, NULL, NULL, NULL, NULL,
				  NULL, NULL,
				  nulsrcp, NULL,
				  NULL, NULL);
  Class *theClass = newClass(&nulsrcp, newId(nulsrcp, "aClass"), NULL, theProps);
  Context context = {CLASS_CONTEXT, NULL, NULL, NULL, theClass, NULL};
  Attribute *theResolvedAttribute;

  theResolvedAttribute = resolveAttributeOfThis(newId(nulsrcp, "x"), &context);
  assert_true(theResolvedAttribute == theAttributes->member.atr);
}

TestSuite *atrTests() {
    TestSuite *suite = create_test_suite();

    add_test_with_context(suite, Attribute, testCreateSetAttribute);
    add_test_with_context(suite, Attribute, testIsWhatId);
    add_test_with_context(suite, Attribute, testSingleIdentifierInMember);
    add_test_with_context(suite, Attribute, testInferClassInSetAttribute);
    add_test_with_context(suite, Attribute, testMultipleAtr);
    add_test_with_context(suite, Attribute, testAttributeListsInSymbolTable);
    add_test_with_context(suite, Attribute, testSortAttributes);
    add_test_with_context(suite, Attribute, testCombineAttributes);
    add_test_with_context(suite, Attribute, testGenerateAttributes);
    add_test_with_context(suite, Attribute, testResolveThisAttributeForClass);
    return suite;
}
