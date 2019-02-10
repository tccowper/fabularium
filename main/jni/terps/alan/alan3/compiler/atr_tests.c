#include <cgreen/cgreen.h>

#include "atr_x.h"
#include "lst_x.h"

#include "id.mock"
#include "sym.mock"
#include "exp.mock"
#include "srcp.mock"
#include "cla.mock"
#include "ins.mock"
#include "encode.mock"
#include "adv.mock"
#include "context.mock"
#include "wht.mock"
#include "lmList.mock"
#include "smScSema.mock"
#include "emit.mock"


Describe(Attribute);
BeforeEach(Attribute) {entitySymbol = newSymbol(newId(nulsrcp, "entity"), CLASS_SYMBOL);}
AfterEach(Attribute) {}

static Id *given_a_parameter_id_of_class(char *parameterName, Symbol *classOfParameter) {
    Id *id = newId(nulsrcp, parameterName);
    id->symbol = newSymbol(id, PARAMETER_SYMBOL);
    id->symbol->fields.parameter.class = classOfParameter;
    return id;
}
static Id *given_an_attribute_id(char *attributeName) {
    return newId(nulsrcp, attributeName);
}
static Id *given_a_class(char *className) {
    Id *classId = newId(nulsrcp, className);
    Symbol *classSymbol = newSymbol(classId, CLASS_SYMBOL);
    classId->symbol = classSymbol;
    return classId;
}
static Attribute *given_an_attribute(Id *id) {
    return newBooleanAttribute(nulsrcp, id, TRUE);
}
static Expression *given_a_what_expression_of_class(Id *parameterName, Symbol *classSymbol) {
    What *what = newWhatId(nulsrcp, parameterName);
    Expression *whatExp = newWhatExpression(nulsrcp, what);
    whatExp->class = classSymbol;
    return whatExp;
}
static void given_that_the_class_has_the_attribute(Id *classId, Attribute *attribute) {
    Symbol *symbol = classId->symbol;
    symbol->fields.entity.props = NEW(Properties);
    symbol->fields.entity.props->attributes = newList(attribute, ATTRIBUTE_LIST);
}


Ensure(Attribute, finds_restriction_for_parameter_from_context) {
    Id *parameterId = given_a_parameter_id_of_class("parameter", entitySymbol);
    Id *attributeId = given_an_attribute_id("atr");
    Attribute *atr = given_an_attribute(attributeId);
    Id *classId = given_a_class("someClass");
    Expression *exp = given_a_what_expression_of_class(parameterId, classId->symbol);
    Context *context = NULL;    /* Any will do since we have mocked it away */

    given_that_the_class_has_the_attribute(classId, atr);

    expect(classOfIdInContext, will_return(classId->symbol));

    assert_that(resolveAttributeToExpression(exp, attributeId, context), is_equal_to(atr));
}
