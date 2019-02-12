#include <cgreen/cgreen.h>

#include "stm_x.h"

#include "lmList.mock"
#include "context.mock"
#include "exp.mock"
#include "emit.mock"
#include "sym.mock"
#include "adv.mock"
#include "wht.mock"
#include "whr.mock"
#include "srcp.mock"
#include "id.mock"
#include "encode.mock"
#include "smScSema.mock"
#include "set.mock"
#include "atr.mock"
#include "resource.mock"

#include "lst_x.h"


/* Global data */
FILE *txtfil;
int totalScore;


Describe(Statement);
BeforeEach(Statement) {}
AfterEach(Statement) {}


Ensure(Statement, analyzeIf_creates_restricting_context_for_Id_Isa_expression) {
    What *what = newWhatId(nulsrcp, newId(nulsrcp, "id"));
    Expression *what_expression = newWhatExpression(nulsrcp, what);
    Expression *isa = newIsaExpression(nulsrcp, what_expression, FALSE, NULL);
    Statement *statement = newStatement(&nulsrcp, IF_STATEMENT);

    isa->type = BOOLEAN_TYPE;
    statement->fields.iff.exp = isa;
    statement->fields.iff.thn = NULL;
    statement->fields.iff.els = NULL;

    expect(analyzeExpression);
    expect(pushContext);
    expect(addRestrictionInContext);
    
    analyzeStatements(newList(statement, STATEMENT_LIST), NULL);

}

Ensure(Statement, analyzeIf_does_not_create_restricting_context_for_Isa_expression_with_anything_but_Id) {
    What *what = newWhatId(nulsrcp, newId(nulsrcp, "id"));
    Expression *what_expression = newWhatExpression(nulsrcp, what);
    Expression *attribute_expression = newAttributeExpression(nulsrcp, newId(nulsrcp, "attribute"), FALSE, what_expression);
    Expression *isa = newIsaExpression(nulsrcp, attribute_expression, FALSE, NULL);
    Statement *statement = newStatement(&nulsrcp, IF_STATEMENT);

    isa->type = BOOLEAN_TYPE;
    statement->fields.iff.exp = isa;
    statement->fields.iff.thn = NULL;
    statement->fields.iff.els = NULL;

    expect(analyzeExpression);
    
    never_expect(pushContext);
    never_expect(addRestrictionInContext);
    
    analyzeStatements(newList(statement, STATEMENT_LIST), NULL);

}
