#include <cgreen/cgreen.h>
#include <cgreen/mocks.h>

#include "whr_x.h"

#include "lmList.mock"
#include "smScSema.mock"
#include "srcp.mock"
#include "exp.mock"
#include "sym.mock"
#include "cnt.mock"
#include "emit.mock"


static bool syserr_called;

static void syserrHandler(char *handler) {
    syserr_called = TRUE;
}

Describe(Where);

BeforeEach(Where) {
    syserr_called = FALSE;
    setSyserrHandler(syserrHandler);
}

AfterEach(Where) {}

Ensure(Where, generates_direct_transitivity_for_directly) {
    expect(emitConstant, when(word, is_equal_to(DIRECT)));
    generateTransitivity(DIRECTLY);
}

Ensure(Where, generates_indirect_transitivity_for_indirectly) {
    expect(emitConstant, when(word, is_equal_to(INDIRECT)));
    generateTransitivity(INDIRECTLY);
}

Ensure(Where, generates_transitive_transitivity_for_transitively) {
    expect(emitConstant, when(word, is_equal_to(TRANSITIVE)));
    generateTransitivity(TRANSITIVELY);
}

Ensure(Where, generates_transitive_for_default_transitivity) {
    expect(emitConstant, when(word, is_equal_to(TRANSITIVE)));
    generateTransitivity(DEFAULT_TRANSITIVITY);
}

/* Ensure that we can't generate wrong sort of transitivity codes by
   mistake */
Ensure(Where, generates_syserr_for_ATrans_value_DIRECT) {
    generateTransitivity((Transitivity)DIRECT);
    assert_that(syserr_called);
}

Ensure(Where, generates_syserr_for_ATrans_value_INDIRECT) {
    generateTransitivity((Transitivity)INDIRECT);
    assert_that(syserr_called);
}

Ensure(Where, generates_syserr_for_ATrans_value_TRANSITIVE) {
    generateTransitivity((Transitivity)TRANSITIVE);
    assert_that(syserr_called);
}

