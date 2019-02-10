/*======================================================================*\

  vrbTest.c

  Unit tests for VRB node in the Alan compiler

\*======================================================================*/

#include "vrb.c"

#include <cgreen/cgreen.h>

#include "unitList.h"


Describe(Verb);
BeforeEach(Verb) {}
AfterEach(Verb) {}


Ensure(Verb, testMultipleVerbs) {
    Id *v1 = newId(nulsrcp, "v1");
    Id *v2 = newId(nulsrcp, "v2");
    Id *v3 = newId(nulsrcp, "v3");
    Id *v4 = newId(nulsrcp, "v4");
    Id *v5 = newId(nulsrcp, "v5");
    Id *v6 = newId(nulsrcp, "v6");
    List *verbIds1 = newList(v1, ID_LIST);
    List *verbIds2 = concat(newList(v2, ID_LIST), v3, ID_LIST);
    List *verbIds3 = concat(newList(v1, ID_LIST), v5, ID_LIST);
    List *verbs = concat(concat(newList(newVerb(&nulsrcp, verbIds1, NULL, FALSE),
                                        VERB_LIST),
                                newVerb(&nulsrcp, verbIds2, NULL, FALSE),
                                VERB_LIST),
                         newVerb(&nulsrcp, verbIds3, NULL, FALSE),
                         VERB_LIST);

    checkMultipleVerbDeclarations(verbs);
    assert_true(readEcode() != 0);
    assert_true(readSev() != 0);

    assert_true(verbIdFound(v1, verbs));
    assert_true(verbIdFound(v2, verbs));
    assert_true(verbIdFound(v3, verbs));
    assert_true(!verbIdFound(v4, verbs));
    assert_true(verbIdFound(v5, verbs));
    assert_true(!verbIdFound(v6, verbs));
}


TestSuite *vrbTests()
{
    TestSuite *suite = create_test_suite(); 

    add_test_with_context(suite, Verb, testMultipleVerbs);

    return suite;
}

