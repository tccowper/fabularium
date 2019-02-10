#include <cgreen/cgreen.h>

#include "ins_x.h"

#include "smScan.h"
#include "lmList.mock"
#include "srcp.mock"
#include "sym.mock"
#include "prop.mock"
#include "wrd.mock"
#include "atr.mock"
#include "emit.mock"
#include "cnt.mock"
#include "id.mock"
#include "context.mock"
#include "smScSema.mock"


Describe(Instance);

BeforeEach(Instance) {
}

AfterEach(Instance) {}


Ensure(Instance, can_ensure_hero_inherits_from_actor_when_hero_has_no_properties) {
    Adventure *adventure = NEW(Adventure);
    Symbol *verb_hero = NEW(Symbol);

    actorSymbol = NEW(Symbol);

    expect(lookup, when(idString, is_equal_to_string("hero")), will_return(verb_hero));
    addHero(adventure);
}

TestSuite *ins_tests(void) {
    TestSuite *suite = create_test_suite();
    add_test_with_context(suite, Instance, can_ensure_hero_inherits_from_actor_when_hero_has_no_properties);
    return suite;
}
