#include <cgreen/cgreen.h>

#include "id_x.h"

/* Mocks: */
#include "srcp.mock"
#include "lmList.mock"
#include "smScSema.mock"
#include "sym.mock"
#include "adv.mock"
#include "emit.mock"

/* Use actual implementation: */
#include "str.c"


Describe(Id);

BeforeEach(Id) {}

AfterEach(Id) {}


Ensure(Id, testGeneratedId) {
  Id *id1 = NEW(Id);
  Id *id2 = NEW(Id);
  id1->string = generateIdName();
  id2->string = generateIdName();

  assert_true(isGeneratedId(id1));
  assert_true(isGeneratedId(id2));
  assert_true(!equalId(id1, id2));
}


Ensure(Id, will_say_same_strings_are_equal) {
    Id* id1 = newId(nulsrcp, "abc");
    Id* id2 = newId(nulsrcp, "abc");

    assert_that(equalId(id1, id2));
}


Ensure(Id, will_say_different_strings_are_not_equal) {
    Id* id1 = newId(nulsrcp, "abc");
    Id* id2 = newId(nulsrcp, "abc1");

    assert_that(!equalId(id1, id2));
}
