#include <cgreen/cgreen.h>

#include "context_x.h"

#include "srcp.mock"
#include "sym.mock"
#include "id.mock"
#include "exp.mock"
#include "wht.mock"
#include "lmList.mock"

List *fileNames = NULL;

Symbol *theVerbSymbol;

Describe(Context);
BeforeEach(Context) {
    theVerbSymbol = newSymbol(newId(nulsrcp, "theVerb"), VERB_SYMBOL);
}
AfterEach(Context) {}

Ensure(Context, returns_null_for_not_restricted_parameter_in_single_context) {
    Context *context = newVerbContext(theVerbSymbol);
    Id *parameter = newId(nulsrcp, "parameter");

    assert_that(contextRestrictsIdTo(context, parameter), is_null);
}

Ensure(Context, returns_class_for_restricted_parameter_in_single_context) {
    Context *context = newVerbContext(theVerbSymbol);
    Id *parameter = newId(nulsrcp, "parameter");
    What *parameterWhat = newWhatId(nulsrcp, parameter);
    Expression *parameterExpression = newWhatExpression(nulsrcp, parameterWhat);
    Id *theClassId = newId(nulsrcp, "theClass");
    Symbol *theClass = newSymbol(theClassId, CLASS_SYMBOL);
    Expression *theExpression = newIsaExpression(nulsrcp, parameterExpression, FALSE, theClassId);

    theClassId->symbol = theClass;

    addRestrictionInContext(context, theExpression);
    assert_that(contextRestrictsIdTo(context, parameter), is_equal_to(theClass));

    Context *context2 = pushContext(context);
    assert_that(contextRestrictsIdTo(context2, parameter), is_equal_to(theClass));
}
