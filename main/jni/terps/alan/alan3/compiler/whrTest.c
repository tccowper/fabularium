/*======================================================================*\

  whrTest.c

  Unit tests for WHR node in the Alan compiler

\*======================================================================*/

#include "whr.c"

#include <cgreen/cgreen.h>

#include "ins_x.h"
#include "cla_x.h"
#include "prop_x.h"


Describe(Where);
BeforeEach(Where) {}
AfterEach(Where) {}


Ensure(Where, InitialLocationOfObjectIsNowhere) {
  Id *locId = newId(nulsrcp, "atLoc");
  Where *whr = newWhere(&nulsrcp, FALSE, WHERE_AT,
			newWhatExpression(nulsrcp, newWhatId(nulsrcp, locId)));
  Id *id = newId(nulsrcp, "id");
  Instance *atLoc = newInstance(&nulsrcp, locId, NULL, NULL);
  Properties *properties = newProps(NULL, NULL, nulsrcp,
				    NULL, NULL, NULL, NULL, nulsrcp,
				    NULL, NULL, NULL, NULL, NULL,
				    NULL, nulsrcp, NULL, NULL, NULL);

  symbolizeWhere(whr);
  properties->id = id;

  symbolizeProps(properties, FALSE);
  assert_true(generateInitialLocation(properties) == 1); /* #nowhere */

  properties->whr = whr;
  symbolizeProps(properties, FALSE);
  assert_true(generateInitialLocation(properties) == atLoc->props->id->symbol->code);
}

Ensure(Where, InitialLocationOfLocationIsNull) {
  Id *locId = newId(nulsrcp, "atLoc");
  Where *whr = newWhere(&nulsrcp, FALSE, WHERE_AT,
			newWhatExpression(nulsrcp, newWhatId(nulsrcp, locId)));
  Instance *atLoc = newInstance(&nulsrcp, locId, NULL, NULL);
  Id *id = newId(nulsrcp, "id");
  Properties *properties = newProps(NULL, NULL, nulsrcp,
				    NULL, NULL, NULL, NULL, nulsrcp,
				    NULL, NULL, NULL, NULL, NULL,
				    NULL, nulsrcp, NULL, NULL, NULL);

  initClasses();

  symbolizeWhere(whr);
  id->symbol = newSymbol(id, INSTANCE_SYMBOL);
  properties->id = id;
  properties->parentId = newId(nulsrcp, "location");

  symbolizeProps(properties, FALSE);
  assert_that(generateInitialLocation(properties), is_equal_to(0));

  properties->whr = whr;
  assert_that(generateInitialLocation(properties), is_equal_to(atLoc->props->id->symbol->code));
}

TestSuite *whrTests()
{
    TestSuite *suite = create_test_suite();

    add_test_with_context(suite, Where, InitialLocationOfObjectIsNowhere);
    add_test_with_context(suite, Where, InitialLocationOfLocationIsNull);

    return suite;
}

