#ifndef _EXP_X_H_
#define _EXP_X_H_
/*----------------------------------------------------------------------*\

				EXP_X.H
			   Expression Nodes

\*----------------------------------------------------------------------*/

#include "exp.h"

/* USE other definitions */
#include "srcp.h"
#include "context.h"



/* Data: */


/* Functions: */

extern Expression *newAttributeExpression(Srcp srcp, Id *attribute,
					  Bool not, Expression *ofWhat);
extern Expression *newBinaryExpression(Srcp srcp, Expression *left, Bool not,
				       OperatorKind operator,
				       Expression *right);
extern Expression *newBetweenExpression(Srcp srcp, Expression *exp, Bool not,
					Expression *low, Expression *high);
extern Expression *newStringExpression(Srcp srcp, long fpos, int len);
extern Expression *newSetExpression(Srcp srcp, List *set);
extern Expression *newScoreExpression(Srcp srcp);
extern Expression *newIntegerExpression(Srcp srcp, int value);
extern Expression *newIsaExpression(Srcp srcp, Expression *what, Bool not,
				    Id *class);
extern Expression *newWhatExpression(Srcp srcp, What *what);
extern Expression *newWhereExpression(Srcp srcp, Expression *what, Bool not, Where *where);
extern Expression *newAggregateExpression(Srcp srcp, AggregateKind kind,
					  Id *attribute, List *filters);
extern Expression *newRandomRangeExpression(Srcp srcp, Expression *from,
					    Expression *to);
extern Expression *newRandomInExpression(Srcp srcp, Expression *what, Transitivity transitivity);

extern void symbolizeExpression(Expression *exp);
extern void analyzeExpression(Expression *exp, Context *context);
extern Bool analyzeFilterExpressions(char *message, List *filters,
				     Context *context, Symbol **foundClass);
extern Bool isConstantIdentifier(Id *id);
extern Bool isConstantExpression(Expression *exp);
extern Symbol *containerContent(Expression *what, Transitivity transitivity, Context *context);
extern Bool verifyContainerExpression(Expression *what, Context *context,
				      char referer[]);
extern Symbol *symbolOfExpression(Expression *exp, Context *context);
extern void generateExpression(Expression *exp);
extern void generateFilter(Expression *exp);
extern void generateBinaryOperator(Expression *exp);
extern void generateLvalue(Expression *exp);
extern void generateAttributeReference(Expression *exp);
extern void generateAttributeAccess(Expression *exp);
extern void generateBetweenCheck(Expression *exp);
extern void dumpExpression(Expression *exp);

#endif
