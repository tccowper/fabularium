/*======================================================================*\

  elmTest.c

  Unit tests for ELM node in the Alan compiler

  \*======================================================================*/

#include "elm.c"

#include <cgreen/cgreen.h>

#include "stx_x.h"
#include "lst_x.h"

Describe(Element);
BeforeEach(Element) {}
AfterEach(Element) {}


Ensure(Element, testPartition) {
    List *p = NULL;
    Element *e = newEndOfSyntax();
    List *ep = newList(e, ELEMENT_LIST);
    List *epp = newList(ep, LIST_LIST);

    assert_true(partitionElements(&p) == NULL);
    assert_true(partitionElements(&epp) != NULL);
}

Ensure(Element, entryForParameterShouldMergeFlagsFromAllElements) {
    // Create three syntaxes
    Syntax *firstSyntax = newSyntaxWithEOS(nulsrcp, NULL, NULL, nulsrcp);
    Syntax *secondSyntax = newSyntaxWithEOS(nulsrcp, NULL, NULL, nulsrcp);
    Syntax *thirdSyntax = newSyntaxWithEOS(nulsrcp, NULL, NULL, nulsrcp);

    // And three parameter elements
    Element *firstElement = newParameterElement(nulsrcp, NULL, 0);
    Element *secondElement = newParameterElement(nulsrcp, NULL, 0);
    Element *thirdElement = newParameterElement(nulsrcp, NULL, 0);

    Aword buffer[100];
    initEmitBuffer(buffer);

    // Add the elements to the syntaxes
    addElement(firstSyntax, firstElement);
    addElement(secondSyntax, secondElement);
    addElement(thirdSyntax, thirdElement);

    // Put them in a list/collection as one partition
    List *partition = NULL;
    partition = concat(partition, firstSyntax->elements, LIST_LIST);
    partition = concat(partition, secondSyntax->elements, LIST_LIST);
    partition = concat(partition, thirdSyntax->elements, LIST_LIST);

    ElementEntry entry;

    firstElement->flags = MULTIPLEBIT;
    secondElement->flags = 0;
    thirdElement->flags = 0;

    entryForParameter(&entry, partition, NULL);
    assert_true(entry.flags == MULTIPLEBIT);

    firstElement->flags = 0;
    secondElement->flags = MULTIPLEBIT;
    thirdElement->flags = 0;

    entryForParameter(&entry, partition, NULL);
    assert_true(entry.flags == MULTIPLEBIT);


    firstElement->flags = 0;
    secondElement->flags = 0;
    thirdElement->flags = MULTIPLEBIT;

    entryForParameter(&entry, partition, NULL);
    assert_true(entry.flags == MULTIPLEBIT);

}

TestSuite *elmTests()
{
    TestSuite *suite = create_test_suite();

    add_test_with_context(suite, Element, testPartition);
    add_test_with_context(suite, Element, entryForParameterShouldMergeFlagsFromAllElements);

    return suite;
}

