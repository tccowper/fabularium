/*======================================================================*\

  stmTest.c

  Unit tests for STM node in the Alan compiler

\*======================================================================*/

#include "stm.c"

#include <cgreen/cgreen.h>

#include "cla_x.h"
#include "unitList.h"


Describe(Statement);
BeforeEach(Statement) {}
AfterEach(Statement) {}


Ensure(Statement, testVerifySetAssignment) {
  Expression *exp = newSetExpression(nulsrcp, NULL);
  Expression *wht = newWhatExpression(nulsrcp, newWhatId(nulsrcp, newId(nulsrcp, "setAttribute")));

  initClasses();

  wht->type = SET_TYPE;
  exp->type = SET_TYPE;

  exp->class = locationSymbol;
  wht->class = objectSymbol;

  /* Set a set attribute to an empty set (of a different member class) */
  (void)readEcode();
  verifySetAssignment(exp, wht);
  assert_true(readEcode() == 0);

  /* Set a set attribute to a non-empty set of different type */
  /* Dummy element to make length > 0 */
  exp->fields.set.members = newList(exp, EXPRESSION_LIST);
  verifySetAssignment(exp, wht);
  assert_true(readEcode() == 431);

  /* Set a set attribute to a non-empty set the same type */
  exp->class = wht->class;
  verifySetAssignment(exp, wht);
  assert_true(readEcode() == 0);

  /* Assign a set attribute from another set attribute of the same type */
  exp->kind = ATTRIBUTE_EXPRESSION;
  verifySetAssignment(exp, wht);
  assert_true(readEcode() == 0);

  /* Assign a set attribute from another set attribute of a different type */
  exp->class = locationSymbol;
  verifySetAssignment(exp, wht);
  assert_true(readEcode() == 431);
}

TestSuite *stmTests()
{
    TestSuite *suite = create_test_suite(); 

    add_test_with_context(suite, Statement, testVerifySetAssignment);

    return suite;
}

