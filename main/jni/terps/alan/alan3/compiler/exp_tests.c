#include <cgreen/cgreen.h>

#include "exp_x.h"

#include "lmList.mock"
#include "srcp.mock"
#include "whr.mock"
#include "id.mock"
#include "wht.mock"
#include "encode.mock"
#include "context.mock"
#include "atr.mock"
#include "sym.mock"
#include "set.mock"
#include "prop.mock"
#include "emit.mock"
#include "smScSema.mock"



Describe(Expression);

BeforeEach(Expression) {
}

AfterEach(Expression) {}


Ensure(Expression, containerTakes_returns_null_for_non_containers) {
    /* Given: there is an expression refering to an instance that is not a container */
    What *what = newWhatLocation(nulsrcp);
    Symbol *aSymbol = newSymbol(newId(nulsrcp, "loc"), INSTANCE_SYMBOL);
    expect(symbolOfWhat, will_return(aSymbol));
    expect(containerSymbolTakes, will_return(NULL));
    Expression *whatExpression = newWhatExpression(nulsrcp, what);
    
    /* Then: containerTakes() should return NULL for the object class symbol */
    assert_that(containerContent(whatExpression, DIRECTLY, NULL), is_null);
}
