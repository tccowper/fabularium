/*----------------------------------------------------------------------*\

  EXP.C
  Expression Nodes

\*----------------------------------------------------------------------*/

#include "exp_x.h"

#include "util.h"

#include "srcp_x.h"
#include "type_x.h"
#include "whr_x.h"
#include "id_x.h"
#include "atr_x.h"
#include "sym_x.h"
#include "wht_x.h"
#include "cnt_x.h"
#include "lst_x.h"
#include "set_x.h"
#include "context_x.h"
#include "dump_x.h"

#include "lmList.h"

#include "adv.h"
#include "elm.h"
#include "ins.h"
#include "opt.h"

#include "emit.h"
#include "acode.h"
#include "encode.h"




/*----------------------------------------------------------------------*/
static Expression *newExpression(Srcp srcp, ExpressionKind kind)
{
    Expression *new;                      /* The newly allocated area */

    progressCounter();

    new = NEW(Expression);

    new->srcp = srcp;
    new->kind = kind;
    new->not = FALSE;

    return(new);
}


/*======================================================================*/
Expression *newWhatExpression(Srcp srcp, What *what) {
    Expression *exp = newExpression(srcp, WHAT_EXPRESSION);
    exp->fields.wht.wht = what;
    return exp;
}


/*======================================================================*/
Expression *newWhereExpression(Srcp srcp, Expression *what, Bool not, Where *where) {
    Expression *exp = newExpression(srcp, WHERE_EXPRESSION);
    exp->fields.whr.wht = what;
    exp->not = not;
    exp->fields.whr.whr = where;
    return exp;
}


/*======================================================================*/
Expression *newStringExpression(Srcp srcp, long fpos, int len)
{
    Expression *exp = newExpression(srcp, STRING_EXPRESSION);
    exp->fields.str.fpos = fpos;
    exp->fields.str.len = len;
    return exp;
}

/*======================================================================*/
Expression *newSetExpression(Srcp srcp, List *set)
{
    Expression *exp = newExpression(srcp, SET_EXPRESSION);
    exp->fields.set.members = set;
    return exp;
}

/*======================================================================*/
Expression *newScoreExpression(Srcp srcp)
{
    Expression *exp = newExpression(srcp, SCORE_EXPRESSION);
    return exp;
}

/*======================================================================*/
Expression *newIntegerExpression(Srcp srcp, int value)
{
    Expression *exp = newExpression(srcp, INTEGER_EXPRESSION);
    exp->fields.val.val = value;
    return exp;
}

/*======================================================================*/
Expression *newAttributeExpression(Srcp srcp, Id *attribute, Bool not,
                                   Expression *ofWhat) {
    Expression *exp = newExpression(srcp, ATTRIBUTE_EXPRESSION);
    exp->fields.atr.id = attribute;
    exp->not = not;
    exp->fields.atr.wht = ofWhat;
    return exp;
}

/*======================================================================*/
Expression *newIsaExpression(Srcp srcp, Expression *what, Bool not,
                             Id *class) {
    Expression *exp = newExpression(srcp, ISA_EXPRESSION);
    exp->fields.isa.what = what;
    exp->not = not;
    exp->fields.isa.class = class;
    return exp;
}

/*======================================================================*/
Expression *newAggregateExpression(Srcp srcp, AggregateKind kind,
                                   Id *attribute, List *filters) {
    Expression *exp = newExpression(srcp, AGGREGATE_EXPRESSION);
    exp->fields.agr.kind = kind;
    exp->fields.agr.attribute = attribute;
    exp->fields.agr.filters = filters;
    exp->fields.agr.type = UNINITIALIZED_TYPE;
    exp->fields.agr.class = NULL;
    return exp;
}

/*======================================================================*/
Expression *newBinaryExpression(Srcp srcp, Expression *left, Bool not,
                                OperatorKind kind, Expression *right) {
    Expression *exp = newExpression(srcp, BINARY_EXPRESSION);
    exp->fields.bin.left = left;
    exp->not = not;
    exp->fields.bin.op = kind;
    exp->fields.bin.right = right;
    return exp;
}


/*======================================================================*/
Expression *newBetweenExpression(Srcp srcp, Expression *expression, Bool not,
                                 Expression *low, Expression *high) {
    Expression *exp = newExpression(srcp, BETWEEN_EXPRESSION);
    exp->fields.btw.exp = expression;
    exp->not = not;
    exp->fields.btw.lowerLimit = low;
    exp->fields.btw.upperLimit = high;
    return exp;
}


/*======================================================================*/
Expression *newRandomRangeExpression(Srcp srcp, Expression *from, Expression *to) {
    Expression *exp = newExpression(srcp, RANDOM_EXPRESSION);
    exp->fields.rnd.from = from;
    exp->fields.rnd.to = to;
    return exp;
}


/*======================================================================*/
Expression *newRandomInExpression(Srcp srcp, Expression *what, Transitivity transitivity) {
    Expression *exp = newExpression(srcp, RANDOM_IN_EXPRESSION);
    exp->fields.rin.what = what;
    exp->fields.rin.transitivity = transitivity;
    return exp;
}


/*======================================================================*/
void symbolizeExpression(Expression *exp) {
    List *member;

    if (exp != NULL)
        switch (exp->kind) {
        case WHERE_EXPRESSION:
            symbolizeWhere(exp->fields.whr.whr);
            symbolizeExpression(exp->fields.whr.wht);
            break;
        case WHAT_EXPRESSION:
            symbolizeWhat(exp->fields.wht.wht);
            break;
        case ATTRIBUTE_EXPRESSION:
            symbolizeExpression(exp->fields.atr.wht);
            break;
        case BINARY_EXPRESSION:
            symbolizeExpression(exp->fields.bin.right);
            symbolizeExpression(exp->fields.bin.left);
            break;
        case SET_EXPRESSION:
            ITERATE(member, exp->fields.set.members)
                symbolizeExpression(member->member.exp);
            break;
        case RANDOM_EXPRESSION:
            symbolizeExpression(exp->fields.rnd.from);
            symbolizeExpression(exp->fields.rnd.to);
            break;
        case BETWEEN_EXPRESSION:
            symbolizeExpression(exp->fields.btw.lowerLimit);
            symbolizeExpression(exp->fields.btw.upperLimit);
            break;
        case RANDOM_IN_EXPRESSION:
            symbolizeExpression(exp->fields.rin.what);
            break;
        case INTEGER_EXPRESSION:
        case STRING_EXPRESSION:
        case AGGREGATE_EXPRESSION:
        case SCORE_EXPRESSION:
        case ISA_EXPRESSION:
            break;
        }
}


/*----------------------------------------------------------------------*/
static Bool idIsContainer(Id *id, Context *context) {
    switch (id->symbol->kind) {
    case PARAMETER_SYMBOL:
        return id->symbol->fields.parameter.restrictedToContainer || symbolIsContainer(classOfIdInContext(context, id));
    case LOCAL_SYMBOL:
        return symbolIsContainer(classOfIdInContext(context, id));
    case INSTANCE_SYMBOL:
        return symbolIsContainer(id->symbol);
    default: SYSERR("Unexpected id->symbol->kind", id->srcp);
    }
    return TRUE;                /* Assume the best to avoid spurious errors */
}


/*----------------------------------------------------------------------*/
static Bool expressionIsContainer(Expression *exp, Context *context) {
    switch (exp->kind) {
    case WHAT_EXPRESSION:
        switch (exp->fields.wht.wht->kind) {
        case WHAT_THIS: return symbolIsContainer(symbolOfContext(context));
        case WHAT_LOCATION: return symbolIsContainer(locationSymbol);
        case WHAT_ACTOR: return symbolIsContainer(actorSymbol);
        case WHAT_ID: return idIsContainer(exp->fields.wht.wht->id, context);
        default: SYSERR("Unexpected wht->kind", exp->fields.wht.wht->srcp);
        }
    case ATTRIBUTE_EXPRESSION:
        return symbolIsContainer(exp->class);
        break;
    default:
        SYSERR("Unexpected Expression kind", exp->srcp);
    }
    return TRUE; /* If anything is wrong assume that it is a container to get fewer spurious errors */
}

/*----------------------------------------------------------------------*/
static void expressionIsNotContainer(Expression *exp, Context *context,
                                     char constructDescription[]) {
    switch (exp->kind) {
    case WHAT_EXPRESSION:
        whatIsNotContainer(exp->fields.wht.wht, context, constructDescription); break;
    case ATTRIBUTE_EXPRESSION:
        lmLogv(&exp->srcp, 311, sevERR, "Expression", "a Container", "because the class of the attribute infered from its initial value does not have the Container property", NULL);
        break;
    default:
        SYSERR("Unexpected Expression kind", exp->srcp);
    }
}

/*======================================================================*/
Bool verifyContainerExpression(Expression *what, Context *context,
                               char constructDescription[]) {

    if (what->type != ERROR_TYPE) {
        if (what->type != INSTANCE_TYPE) {
            lmLogv(&what->srcp, 428, sevERR, constructDescription, "an instance", NULL);
            return FALSE;
        } else if (!expressionIsContainer(what, context)) {
            expressionIsNotContainer(what, context, constructDescription);
            return FALSE;
        }
    }
    return TRUE;
}


/*======================================================================*/
Symbol *symbolOfExpression(Expression *exp, Context *context) {
    if (exp == NULL) return NULL;

    switch (exp->kind) {
    case WHAT_EXPRESSION:
        return symbolOfWhat(exp->fields.wht.wht, context);
    case ATTRIBUTE_EXPRESSION:
        return exp->class;
    default:
        SYSERR("Unexpected expression kind", exp->srcp);
    }
    return NULL;
}


/*======================================================================*/
Symbol *containerContent(Expression *what, Transitivity transitivity, Context *context) {

    /* Find what classes a container might contain */

    Symbol *symbol = NULL;
    Symbol *content = NULL;

    switch (what->kind) {
    case WHAT_EXPRESSION:
        symbol = symbolOfWhat(what->fields.wht.wht, context);
        if (transitivity == DIRECTLY)
            content = containerSymbolTakes(symbol);
        else
            content = containerMightContain(symbol);
        break;
    case ATTRIBUTE_EXPRESSION:
        symbol = what->class;
        if (transitivity == DIRECTLY)
            content = containerSymbolTakes(symbol);
        else
            content = containerMightContain(symbol);
        break;
    default:
        break;
    }
    return content;
}


/*----------------------------------------------------------------------*/
static char *aggregateToString(AggregateKind agr)
{
    switch (agr) {
    case SUM_AGGREGATE: return("SUM"); break;
    case MAX_AGGREGATE: return("MAX"); break;
    case MIN_AGGREGATE: return("MIN"); break;
    case COUNT_AGGREGATE: return("COUNT"); break;
    default: SYSERR("Unexpected aggregate kind", nulsrcp);
    }
    return NULL;
}


/*======================================================================*/
Bool isConstantIdentifier(Id *id)
{
    if (id->symbol)
        return id->symbol->kind != PARAMETER_SYMBOL
            && id->symbol->kind != LOCAL_SYMBOL;
    else
        return TRUE;
}


/*======================================================================*/
Bool isConstantExpression(Expression *exp)
{
    switch (exp->kind) {
    case INTEGER_EXPRESSION:
    case STRING_EXPRESSION:
        return TRUE;
    case SET_EXPRESSION:
        {
            List *members;
            ITERATE(members, exp->fields.set.members) {
                if (!isConstantExpression(members->member.exp))
                    return FALSE;
            }
            return TRUE;
        }
    case WHAT_EXPRESSION:
        return isConstantWhat(exp->fields.wht.wht);

        /* TODO: Some of these might also be constant, e.g. <instance id> Isa <class> */
    case BETWEEN_EXPRESSION:
    case ISA_EXPRESSION:
    case BINARY_EXPRESSION:
    case ATTRIBUTE_EXPRESSION:
    case WHERE_EXPRESSION:
    case AGGREGATE_EXPRESSION:
    case RANDOM_EXPRESSION:
    case RANDOM_IN_EXPRESSION:
    case SCORE_EXPRESSION:
        return FALSE;
    }
    return FALSE;
}


/*----------------------------------------------------------------------*/
static void analyzeWhereExpression(Expression *exp, Context *context)
{
    Expression *what = exp->fields.whr.wht;
    Where *where = exp->fields.whr.whr;

    switch (where->kind) {
    case WHERE_HERE:
    case WHERE_NEARBY:
        if (context->kind == RULE_CONTEXT)
            lmLogv(&exp->srcp, 443, sevERR, "Rule context", "Here or Nearby", NULL);
        break;

    case WHERE_AT:
    case WHERE_NEAR:
        analyzeExpression(where->what, context);
        if (where->what->type != ERROR_TYPE)
            if (where->what->type != INSTANCE_TYPE && where->what->type != REFERENCE_TYPE)
                lmLogv(&where->what->srcp, 428, sevERR, "Expression after AT or NEAR", "an instance", NULL);
        break;

    case WHERE_IN:
        analyzeExpression(where->what, context);
        if (where->what->type == SET_TYPE) {/* Can be in a container and in a set */
            if (exp->fields.whr.whr->transitivity != DEFAULT_TRANSITIVITY)
                lmLogv(&where->srcp, 325, sevERR, transitivityToString(exp->fields.whr.whr->transitivity),
                       "IN operating on a SET", NULL);
        } else {
            verifyContainerExpression(where->what, context, "Expression after IN");
        }
        break;

    default:
        SYSERR("Unrecognized switch", where->srcp);
        break;
    }

    analyzeExpression(what, context);
    if (what->type != ERROR_TYPE) {
        if (what->type == INSTANCE_TYPE) {
            switch (what->kind) {
            case WHAT_EXPRESSION:
                switch (what->fields.wht.wht->kind) {
                case WHAT_LOCATION:
                    lmLogv(&what->srcp, 324, sevERR, "Current Location", "the What-clause of a Where expression", NULL);
                    break;
                case WHAT_ID:
                case WHAT_THIS:
                case WHAT_ACTOR:
                    break;
                default:
                    SYSERR("Unrecognized switch", what->fields.wht.wht->srcp);
                    break;
                }
                break;
            case ATTRIBUTE_EXPRESSION:
                break;
            default:
                SYSERR("Unrecognized switch", what->srcp);
                break;
            }
        } else if (where->what && where->what->type != SET_TYPE)
            lmLogv(&what->srcp, 428, sevERR, "The What clause of a Where expression", "an instance", NULL);
    }

    if (where->kind == WHERE_IN && where->what->type == SET_TYPE) {
        where->kind = WHERE_INSET;
        verifySetMember(where->what, what, "Set member test");
    }
    exp->type = BOOLEAN_TYPE;
}



/*----------------------------------------------------------------------*/
static TypeKind verifyExpressionAttribute(Expression *attributeExpression,
                                          Attribute *foundAttribute)
{
    Id *attributeId = attributeExpression->fields.atr.id;
    TypeKind type = UNINITIALIZED_TYPE;

    if (attributeExpression->kind != ATTRIBUTE_EXPRESSION)
        SYSERR("Not an Attribute Expression", attributeExpression->srcp);

    if (foundAttribute == NULL) {
        return ERROR_TYPE;
    } else if (foundAttribute->id->symbol == NULL) {
        attributeId->code = foundAttribute->id->code;
        attributeExpression->fields.atr.atr = foundAttribute;
        type = foundAttribute->type;
    } else
        SYSERR("Attribute with symbol", attributeExpression->srcp);
    return type;
}


/*----------------------------------------------------------------------*/
static void analyzeAttributeExpression(Expression *exp, Context *context)
{
    Attribute *atr;
    Expression *what = exp->fields.atr.wht;

    analyzeExpression(what, context);

    switch (what->kind) {
    case WHAT_EXPRESSION: {
        atr = resolveAttributeToExpression(what, exp->fields.atr.id, context);
        exp->type = verifyExpressionAttribute(exp, atr);
        if (atr) exp->readonly = atr->readonly;
        if (exp->type == INSTANCE_TYPE || exp->type == REFERENCE_TYPE) {
            if (atr->referenceClass != NULL)
                /* Set the expressions class to the class of the attribute */
                exp->class = atr->referenceClass;
        } else if (exp->type == SET_TYPE)
            exp->class = classOfMembers(exp);
        break;
    }

    case ATTRIBUTE_EXPRESSION:
        if (what->type != ERROR_TYPE) {
            if (what->type != INSTANCE_TYPE) {
                exp->type = ERROR_TYPE;
                lmLogv(&what->srcp, 428, sevERR, "Expression", "an instance", NULL);
            } else {
                atr = resolveAttributeToExpression(what, exp->fields.atr.id, context);
                exp->type = verifyExpressionAttribute(exp, atr);
                if (exp->type == INSTANCE_TYPE)
                    exp->class = atr->referenceClass;
                if (atr) exp->readonly = atr->readonly;
            }
        } else
            exp->type = ERROR_TYPE;
        break;

    default:
        exp->type = ERROR_TYPE;
        lmLog(&exp->srcp, 420, sevERR, "attribute reference");
    }
}

/*----------------------------------------------------------------------*/
static void analyzeSetExpression(Expression *exp, Context *context)
{
    analyzeSetMembers(exp->fields.set.members, &exp->fields.set.memberType, &exp->fields.set.memberClass, context);
    if (exp->fields.set.memberType == ERROR_TYPE)
        exp->type = ERROR_TYPE;
    else {
        exp->type = SET_TYPE;
        exp->class = exp->fields.set.memberClass;
    }
}

/*----------------------------------------------------------------------*/
static void analyzeBinary(Expression *exp) {
	switch (exp->fields.bin.op) {
	case AND_OPERATOR:
	case OR_OPERATOR:
		if (!equalTypes(exp->fields.bin.left->type, BOOLEAN_TYPE))
			lmLogv(&exp->fields.bin.left->srcp, 330, sevERR, "boolean", "AND/OR", NULL);
		if (!equalTypes(exp->fields.bin.right->type, BOOLEAN_TYPE))
			lmLogv(&exp->fields.bin.right->srcp, 330, sevERR, "boolean", "AND/OR", NULL);
		exp->type = BOOLEAN_TYPE;
		break;

	case NE_OPERATOR:
	case EQ_OPERATOR:
		if (!equalTypes(exp->fields.bin.left->type, exp->fields.bin.right->type))
			lmLog(&exp->srcp, 331, sevERR, "expression");
		else if (exp->fields.bin.left->type != ERROR_TYPE && exp->fields.bin.right->type != ERROR_TYPE)
			if (exp->fields.bin.left->type == INSTANCE_TYPE) {
				What *leftWhat = exp->fields.bin.left->fields.wht.wht;
				What *rightWhat = exp->fields.bin.right->fields.wht.wht;
				if (leftWhat->kind == WHAT_ID && rightWhat->kind == WHAT_ID)
					if (isConstantIdentifier(leftWhat->id)
					    && isConstantIdentifier(rightWhat->id))
						lmLog(&exp->srcp, 417, sevINF, NULL);
			}
		exp->type = BOOLEAN_TYPE;
		break;

	case EXACT_OPERATOR:
		if (!equalTypes(exp->fields.bin.left->type, STRING_TYPE))
			lmLogv(&exp->fields.bin.left->srcp, 330, sevERR, "string", "'=='", NULL);
		if (!equalTypes(exp->fields.bin.right->type, STRING_TYPE))
			lmLogv(&exp->fields.bin.right->srcp, 330, sevERR, "string", "'=='", NULL);
		exp->type = BOOLEAN_TYPE;
		break;

	case LE_OPERATOR:
	case GE_OPERATOR:
	case LT_OPERATOR:
	case GT_OPERATOR:
		if (!equalTypes(exp->fields.bin.left->type, INTEGER_TYPE))
			lmLogv(&exp->fields.bin.left->srcp, 330, sevERR, "integer", "relational", NULL);
		if (!equalTypes(exp->fields.bin.right->type, INTEGER_TYPE))
			lmLogv(&exp->fields.bin.right->srcp, 330, sevERR, "integer", "relational", NULL);
		exp->type = BOOLEAN_TYPE;
		break;

	case PLUS_OPERATOR:
		if (!equalTypes(exp->fields.bin.left->type, INTEGER_TYPE)
		    && !equalTypes(exp->fields.bin.left->type, STRING_TYPE))
			lmLogv(&exp->fields.bin.left->srcp, 330, sevERR, "integer or string", "arithmetic", NULL);
		if (!equalTypes(exp->fields.bin.right->type, INTEGER_TYPE)
		    && !equalTypes(exp->fields.bin.right->type, STRING_TYPE))
			lmLogv(&exp->fields.bin.right->srcp, 330, sevERR, "integer or string", "arithmetic", NULL);
		if (!equalTypes(exp->fields.bin.left->type, exp->fields.bin.right->type)) {
			lmLog(&exp->srcp, 331, sevERR, "expression");
			exp->type = ERROR_TYPE;
		} else
			exp->type = exp->fields.bin.left->type;
		break;

	case MINUS_OPERATOR:
	case MULT_OPERATOR:
	case DIV_OPERATOR:
		if (!equalTypes(exp->fields.bin.left->type, INTEGER_TYPE))
			lmLogv(&exp->fields.bin.left->srcp, 330, sevERR, "integer", "arithmetic", NULL);
		if (!equalTypes(exp->fields.bin.right->type, INTEGER_TYPE))
			lmLogv(&exp->fields.bin.right->srcp, 330, sevERR, "integer", "arithmetic", NULL);
		exp->type = INTEGER_TYPE;
		break;

	case CONTAINS_OPERATOR:
		if (!equalTypes(exp->fields.bin.left->type, STRING_TYPE))
			lmLogv(&exp->fields.bin.left->srcp, 330, sevERR, "string", "'CONTAINS'", NULL);
		if (!equalTypes(exp->fields.bin.right->type, STRING_TYPE))
			lmLogv(&exp->fields.bin.right->srcp, 330, sevERR, "string", "'CONTAINS'", NULL);
		exp->type = BOOLEAN_TYPE;
		break;

	default:
		SYSERR("Unrecognized binary operator", exp->srcp);
		break;
	}
}



/*----------------------------------------------------------------------*/
static void analyzeBinaryExpression(Expression *exp, Context *context)
{
	if (exp->fields.bin.left->type == UNINITIALIZED_TYPE)
	    analyzeExpression(exp->fields.bin.left, context);
	if (exp->fields.bin.right->type == UNINITIALIZED_TYPE)
		analyzeExpression(exp->fields.bin.right, context);

    analyzeBinary(exp);
}


/*----------------------------------------------------------------------*/
static void analyzeWhereFilter(Expression *theFilterExpression,
                               Context *context)
{
    Where *where = theFilterExpression->fields.whr.whr;

    analyzeWhere(where, context);
}

/*----------------------------------------------------------------------*/
static void analyzeClassingFilter(char *message,
                                  Context *context,
                                  Expression *theFilter)
{
    /* Actually, the following expressions, that can act as filters,
       restricts the class of the instances we match:
       WHERE/AT - a THING
       WHERE/IN - the class the container takes
       WHERE/INSET - the class of the members in the set
       ISA - the class it tests for
       BETWEEN - integers

       This is provided that there is not a NOT present.

       TODO: Analyze all expressions and match them up to analyze
       whether they are compatible and what attributes can be guaranteed
       to be available. This should really go in
       analyzeFilterExpressions().

       NOTE: Directly might also complicate things if we wanted to warn
       for multiple incompatible filters.
    */

    switch (theFilter->kind) {
    case ISA_EXPRESSION:
        (void) symcheck(theFilter->fields.isa.class, CLASS_SYMBOL, context);
        if (theFilter->fields.isa.class->symbol == NULL) {
            theFilter->type = ERROR_TYPE;
        } else {
            theFilter->class = theFilter->fields.isa.class->symbol;
            theFilter->type = INSTANCE_TYPE;
        }
        break;
    case BETWEEN_EXPRESSION:
        /* This can only be an integer */
        theFilter->type = INTEGER_TYPE;
        theFilter->class = integerSymbol;
        analyzeExpression(theFilter->fields.btw.lowerLimit, context);
        if (!equalTypes(theFilter->fields.btw.lowerLimit->type, INTEGER_TYPE))
            lmLogv(&theFilter->fields.btw.lowerLimit->srcp, 330, sevERR, "integer", "'BETWEEN'", NULL);
        analyzeExpression(theFilter->fields.btw.upperLimit, context);
        if (!equalTypes(theFilter->fields.btw.upperLimit->type, INTEGER_TYPE))
            lmLogv(&theFilter->fields.btw.upperLimit->srcp, 330, sevERR, "integer", "'BETWEEN'", NULL);
        break;
    case WHERE_EXPRESSION:
        analyzeWhereFilter(theFilter, context);
        Where *where = theFilter->fields.whr.whr;
        switch (theFilter->fields.whr.whr->kind) {
        case WHERE_INSET:
            /* TODO Find the class and type of items in the set */
            theFilter->class = where->what->class;
            theFilter->type = where->what->type;
            break;
        case WHERE_IN:
            theFilter->class = containerContent(where->what, where->transitivity, context);
            theFilter->type = where->what->type;
            break;
        case WHERE_HERE:
        case WHERE_AT:
            theFilter->class = entitySymbol;
            theFilter->type = INSTANCE_TYPE;
            break;
        default:
            break;
        }
    case ATTRIBUTE_EXPRESSION:
        break;
    default:
        unimpl(theFilter->srcp, "Analysis : Unimplemented aggregate filter type");
    }
}


/*----------------------------------------------------------------------*/
static void analyzeAttributeFilter(Expression *theFilterExpression,
                                   Symbol *classSymbol,
                                   char *aggregateString)
{
    Attribute *attribute;
    Id *attributeId = theFilterExpression->fields.atr.id;

    if (classSymbol != NULL && isClass(classSymbol)) {
        /* Only do attribute semantic check if class is defined */
        attribute = findAttribute(classSymbol->fields.entity.props->attributes,
                                  attributeId);
        if (attribute == NULL) {
            lmLogv(&attributeId->srcp, 316, sevERR, attributeId->string,
                   "instances iterated over using",
                   aggregateString,
                   classSymbol->string, NULL);
        } else if (!equalTypes(BOOLEAN_TYPE, attribute->type)) {
            lmLog(&attributeId->srcp, 440, sevERR, "Aggregate");
        } else
            attributeId->code = attribute->id->code;
    } else
        lmLog(&theFilterExpression->srcp, 226, sevERR, "");
}


/*----------------------------------------------------------------------*/
static void analyzeNonClassingFilter(char *message,
                                     Context *context,
                                     Expression *theFilter,
                                     Symbol *classSymbol,
                                     Bool *foundWhere)
{
    switch (theFilter->kind) {
    case ATTRIBUTE_EXPRESSION:
        analyzeAttributeFilter(theFilter, classSymbol, message);
        break;
    case WHERE_EXPRESSION:
    case ISA_EXPRESSION:
    case BETWEEN_EXPRESSION:
        break;
    default:
        SYSERR("Unimplemented aggregate filter expression type", theFilter->srcp);
    }
}


/*----------------------------------------------------------------------*/
static Bool expressionIsActualWhere(Expression *expression) {
    switch (expression->kind) {
    case WHERE_EXPRESSION:
        switch (expression->fields.whr.whr->kind) {
        case WHERE_DEFAULT:
        case WHERE_HERE:
        case WHERE_NEARBY:
        case WHERE_NEAR:
        case WHERE_AT:
        case WHERE_IN:
            return TRUE; /* expression->fields.whr.whr->transitivity != DIRECTLY; */
        case WHERE_INSET:
            return FALSE;
        }
    default:
        return FALSE;
    }
}


/*----------------------------------------------------------------------*/
static Symbol *combineFilterClasses(Symbol *original, Symbol *addition, Srcp srcp) {
    if (original == addition)
        return original;
    else if (original == NULL)
        return addition;
    else if (addition == NULL)
        return original;
    else if (inheritsFrom(original, addition))
        return original;
    else if (inheritsFrom(addition, original))
        return addition;
    else {
        lmLog(&srcp, 441, sevERR, "");
        return original;
    }
}


/*======================================================================*/
Bool analyzeFilterExpressions(char *message, List *filters,
                              Context *context, Symbol **foundClass) {
    List *lst;
    Bool foundWhere = FALSE;
    Bool foundIsa = FALSE;
    Bool error = FALSE;
    Symbol *class = NULL;

    /* Analyze the filters which may restrict to a class, return the class id */
    ITERATE(lst, filters) {
        Expression *exp = lst->member.exp;
        analyzeClassingFilter(message, context, exp);
        class = combineFilterClasses(class, exp->class, exp->srcp);
        if (exp->type == ERROR_TYPE)
            error = TRUE;
        if (exp->kind == ISA_EXPRESSION) {
            if (foundIsa)
                lmLogv(&exp->srcp, 224, sevWAR, "Isa (class)",
                       message, NULL);
            foundIsa = TRUE;
        }
        if (expressionIsActualWhere(exp)) {
            if (foundWhere)
                lmLogv(&exp->srcp, 224, sevERR, "Where", message, NULL);
            foundWhere = TRUE;
        }
    }

    ITERATE(lst, filters) {
        analyzeNonClassingFilter(message,  context, lst->member.exp,
                                 class, &foundWhere);
    }

    *foundClass = class;
    return !error;
}


/*----------------------------------------------------------------------*/
static void analyzeAggregate(Expression *exp, Context *context)
{
    Attribute *atr = NULL;
    Symbol *class = NULL;       /* Class resulting from filters if any */
    char message[200] = "";
    exp->type = INTEGER_TYPE;

    strcat(message, aggregateToString(exp->fields.agr.kind));
    strcat(message, " Aggregation");

    if (!analyzeFilterExpressions(message, exp->fields.agr.filters, context,
                                  &class))
        exp->type = ERROR_TYPE;
    else if (class == integerSymbol)
        exp->fields.agr.type = INTEGER_TYPE;
    else
        exp->fields.agr.type = INSTANCE_TYPE;
    exp->fields.agr.class = class;

    if (exp->fields.agr.kind != COUNT_AGGREGATE) {
        /* Now analyze the attribute to do the arithmetic aggregation on */
        if (!class && exp->type != ERROR_TYPE) {
            /* Absence of classing filters makes arithmetic aggregates
               impossible since attributes can never be guaranteed to be
               defined in all instances. */
            // TODO: well, actually that means that only attributes from 'entity' can be used...
            lmLog(&exp->fields.agr.attribute->srcp, 226, sevERR, "");
        } else if (class) {
            /* Only do this if there was a classing filter found */
            atr = findAttribute(class->fields.entity.props->attributes,
                                exp->fields.agr.attribute);
            if (atr == NULL) {
                lmLogv(&exp->fields.agr.attribute->srcp, 316, sevERR,
                       exp->fields.agr.attribute->string,
                       "instances aggregated over using",
                       aggregateToString(exp->fields.agr.kind),
                       class->string, NULL);
            } else if (!equalTypes(INTEGER_TYPE, atr->type)) {
                lmLog(&exp->fields.agr.attribute->srcp, 418, sevERR, "");
            } else
                exp->fields.agr.attribute->code = atr->id->code;
        }
    } else if (class == NULL && exp->type != ERROR_TYPE)
        /* Also for COUNT we want to warn for counting the universe, which
           is probably not what he wanted */
        lmLog(&exp->srcp, 225, sevWAR, aggregateToString(exp->fields.agr.kind));
}


/*----------------------------------------------------------------------*/
static void analyzeRandom(Expression *exp, Context *context)
{
    exp->type = INTEGER_TYPE;
    analyzeExpression(exp->fields.rnd.from, context);
    if (!equalTypes(INTEGER_TYPE, exp->fields.rnd.from->type)) {
        lmLogv(&exp->fields.rnd.from->srcp, 408, sevERR, "Expression", "RANDOM expression", "integer", NULL);
        exp->type = ERROR_TYPE;
    }

    analyzeExpression(exp->fields.rnd.to, context);
    if (!equalTypes(INTEGER_TYPE, exp->fields.rnd.to->type)) {
        lmLogv(&exp->fields.rnd.to->srcp, 408, sevERR, "Expression", "RANDOM expression", "integer", NULL);
        exp->type = ERROR_TYPE;
    }
}


/*----------------------------------------------------------------------*/
static void analyzeRandomIn(Expression *exp, Context *context)
{
    analyzeExpression(exp->fields.rin.what, context);
    if (exp->fields.rin.what->type != SET_TYPE) { /* In a container or in a set? */
        if (verifyContainerExpression(exp->fields.rin.what, context,
                                      "'Random In' expression")) {
            exp->type = INSTANCE_TYPE;
            exp->class = containerContent(exp->fields.rin.what, exp->fields.rin.transitivity, context);
        } else
            exp->type = ERROR_TYPE;
    } else {                    /* In a set */
        exp->class = exp->fields.rin.what->class;
        exp->type = classToType(exp->fields.rin.what->class);
        /* Transitivity is not supported in set membership of course */
        if (exp->fields.rin.transitivity != DEFAULT_TRANSITIVITY)
            lmLogv(&exp->srcp, 422, sevERR, transitivityToString(exp->fields.rin.transitivity),
                   "not allowed for", "Random In operating on a Set", NULL);
    }
}


/*----------------------------------------------------------------------*/
static void analyzeWhatExpression(Expression *exp, Context *context)
{
    Symbol *symbol;
    Id *classId;

    if (exp->kind != WHAT_EXPRESSION)
        SYSERR("Not a WHAT-expression", exp->srcp);
    if (!verifyWhatContext(exp->fields.wht.wht, context)) {
        exp->type = ERROR_TYPE;
        return;
    }

    switch (exp->fields.wht.wht->kind) {

    case WHAT_LOCATION:
        if (context->kind == RULE_CONTEXT)
            lmLogv(&exp->fields.wht.wht->srcp, 412, sevERR, "Location", "Rules", NULL);
        exp->type = INSTANCE_TYPE;
        exp->class = locationSymbol;
        break;

    case WHAT_ACTOR:
        if (context->kind == EVENT_CONTEXT || context->kind == RULE_CONTEXT)
            lmLogv(&exp->fields.wht.wht->srcp, 412, sevERR, "Actor", "Events and Rules", NULL);
        exp->type = INSTANCE_TYPE;
        exp->class = actorSymbol;
        break;

    case WHAT_ID:
        symbol = symcheck(exp->fields.wht.wht->id, INSTANCE_SYMBOL|EVENT_SYMBOL, context);
        if (symbol != NULL) {
            switch (symbol->kind) {
            case PARAMETER_SYMBOL:
            case LOCAL_SYMBOL:
                exp->type = typeOfSymbol(symbol);
                exp->class = classOfIdInContext(context, exp->fields.wht.wht->id);
                if (exp->class->fields.entity.isBasicType)
                    exp->type = basicTypeFromClassSymbol(exp->class);
                break;
            case INSTANCE_SYMBOL:
                exp->type = INSTANCE_TYPE;
                exp->class = symbol->fields.entity.parent;
                break;
            case EVENT_SYMBOL:
                exp->type = EVENT_TYPE;
                break;
            case ERROR_SYMBOL:
                exp->type = ERROR_TYPE;
                break;
            default:
                SYSERR("Unexpected symbolKind", exp->srcp);
                break;
            }
        } else
            exp->type = ERROR_TYPE;
        break;

    case WHAT_THIS:
        exp->type = INSTANCE_TYPE;
        classId = classIdInContext(context);
        if (classId)
            exp->class = classId->symbol;
        break;

    default:
        SYSERR("Unrecognized switch", exp->srcp);
        break;
    }
}


/*----------------------------------------------------------------------*/
static void analyzeBetweenExpression(Expression *exp, Context *context)
{
    analyzeExpression(exp->fields.btw.exp, context);
    if (!equalTypes(exp->fields.btw.exp->type, INTEGER_TYPE))
        lmLogv(&exp->fields.btw.exp->srcp, 330, sevERR, "integer", "'BETWEEN'", NULL);

    analyzeExpression(exp->fields.btw.lowerLimit, context);
    if (!equalTypes(exp->fields.btw.lowerLimit->type, INTEGER_TYPE))
        lmLogv(&exp->fields.btw.lowerLimit->srcp, 330, sevERR, "integer", "'BETWEEN'", NULL);

    analyzeExpression(exp->fields.btw.upperLimit, context);
    if (!equalTypes(exp->fields.btw.upperLimit->type, INTEGER_TYPE))
        lmLogv(&exp->fields.btw.upperLimit->srcp, 330, sevERR, "integer", "'BETWEEN'", NULL);

    exp->type = BOOLEAN_TYPE;
}


/*----------------------------------------------------------------------*/
static void analyzeIsaExpression(Expression *expression, Context *context)
{
    Expression *what = expression->fields.isa.what;
    switch (expression->fields.isa.what->kind) {
    case WHAT_EXPRESSION:
        switch (what->fields.wht.wht->kind) {
        case WHAT_ID:
            symcheck(what->fields.wht.wht->id, INSTANCE_SYMBOL, context);
            break;
        case WHAT_LOCATION:
        case WHAT_ACTOR:
            break;
        default:
            unimpl(expression->srcp, "Analyzer");
            break;
        }
        break;
    case ATTRIBUTE_EXPRESSION:
        analyzeExpression(what, context);
        if (!equalTypes(what->type, INSTANCE_TYPE))
            lmLog(&expression->srcp, 434, sevERR, "'ISA'");
        break;
    default:
        lmLog(&expression->srcp, 434, sevERR, "'ISA'");
        break;
    }

    symcheck(expression->fields.isa.class, CLASS_SYMBOL, context);

    expression->type = BOOLEAN_TYPE;
}

/*======================================================================*/
void analyzeExpression(Expression *expression, Context *context)
{
    if (expression == NULL)       /* Ignore empty expressions (syntax error probably) */
        return;
    switch (expression->kind) {

    case WHERE_EXPRESSION:
        analyzeWhereExpression(expression, context);
        break;

    case ATTRIBUTE_EXPRESSION:
        analyzeAttributeExpression(expression, context);
        break;

    case BINARY_EXPRESSION:
        analyzeBinaryExpression(expression, context);
        break;

    case INTEGER_EXPRESSION:
        expression->type = INTEGER_TYPE;
        break;

    case STRING_EXPRESSION:
        expression->type = STRING_TYPE;
        break;

    case SET_EXPRESSION:
        analyzeSetExpression(expression, context);
        break;

    case AGGREGATE_EXPRESSION:
        analyzeAggregate(expression, context);
        break;

    case RANDOM_EXPRESSION:
        analyzeRandom(expression, context);
        break;

    case RANDOM_IN_EXPRESSION:
        analyzeRandomIn(expression, context);
        break;

    case SCORE_EXPRESSION:
        expression->type = INTEGER_TYPE;
        break;

    case WHAT_EXPRESSION:
        analyzeWhatExpression(expression, context);
        break;

    case BETWEEN_EXPRESSION:
        analyzeBetweenExpression(expression, context);
        break;

    case ISA_EXPRESSION:
        analyzeIsaExpression(expression, context);
        break;
    }
}


/*======================================================================*/
void generateBinaryOperator(Expression *exp)
{
    switch (exp->fields.bin.op) {
    case AND_OPERATOR:
        emit0(I_AND);
        break;
    case OR_OPERATOR:
        emit0(I_OR);
        break;
    case NE_OPERATOR:
        if (exp->fields.bin.right->type == STRING_TYPE) {
            emit0(I_STREQ);
            emit0(I_NOT);
        } else
            emit0(I_NE);
        break;
    case EQ_OPERATOR:
        if (exp->fields.bin.right->type == STRING_TYPE)
            emit0(I_STREQ);
        else
            emit0(I_EQ);
        break;
    case EXACT_OPERATOR:
        emit0(I_STREXACT);
        break;
    case LE_OPERATOR:
        emit0(I_LE);
        break;
    case GE_OPERATOR:
        emit0(I_GE);
        break;
    case LT_OPERATOR:
        emit0(I_LT);
        break;
    case GT_OPERATOR:
        emit0(I_GT);
        break;
    case PLUS_OPERATOR:
        if (exp->type == INTEGER_TYPE)
            emit0(I_PLUS);
        else if (exp->type == STRING_TYPE)
            emit0(I_CONCAT);
        else
            SYSERR("Unexpected type", nulsrcp);
        break;
    case MINUS_OPERATOR:
        emit0(I_MINUS);
        break;
    case MULT_OPERATOR:
        emit0(I_MULT);
        break;
    case DIV_OPERATOR:
        emit0(I_DIV);
        break;
    case CONTAINS_OPERATOR:
        emit0(I_CONTAINS);
        break;
    }
}


/*----------------------------------------------------------------------*/
static void generateBinaryExpression(Expression *exp)
{
    generateExpression(exp->fields.bin.left);
    generateExpression(exp->fields.bin.right);
    generateBinaryOperator(exp);
    if (exp->not) emit0(I_NOT);
}


/*----------------------------------------------------------------------*/
static void generateWhereRHS(Where *where) {

    switch (where->kind) {
    case WHERE_HERE:
        generateTransitivity(where->transitivity);
        emit0(I_HERE);
        break;
    case WHERE_NEARBY:
        generateTransitivity(where->transitivity);
        emit0(I_NEARBY);
        break;
    case WHERE_NEAR:
        generateExpression(where->what);
        generateTransitivity(where->transitivity);
        emit0(I_NEAR);
        break;
    case WHERE_IN:
        generateExpression(where->what);
        generateTransitivity(where->transitivity);
        emit0(I_IN);
        break;
    case WHERE_INSET:
        generateExpression(where->what);
        emit0(I_INSET);
        break;
    case WHERE_AT:
        generateExpression(where->what);
        generateTransitivity(where->transitivity);
        emit0(I_AT);
        break;
    case WHERE_DEFAULT:
        SYSERR("Generating WHERE_DEFAULT", nulsrcp);
    }
}




/*----------------------------------------------------------------------*/
static void generateWhereExpression(Expression *exp)
{
    Where *where = exp->fields.whr.whr;
    Expression *what = exp->fields.whr.wht;

    generateExpression(what);
    generateWhereRHS(where);
    if (exp->not) emit0(I_NOT);
}



/*======================================================================*/
void generateAttributeAccess(Expression *exp)
{
    if (exp->type == STRING_TYPE)
        emit0(I_ATTRSTR);
    else if (exp->type == SET_TYPE)
        emit0(I_ATTRSET);
    else
        emit0(I_ATTRIBUTE);
}


/*======================================================================*/
void generateAttributeReference(Expression *exp) {
    generateExpression(exp->fields.atr.wht);
    generateId(exp->fields.atr.id, exp->type);
}


/*======================================================================*/
void generateLvalue(Expression *exp) {
    switch (exp->kind) {
    case WHAT_EXPRESSION:
        generateWhat(exp->fields.wht.wht, exp->type);
        break;
    case ATTRIBUTE_EXPRESSION:
        generateAttributeReference(exp);
        break;
    default: SYSERR("Unexpected expression kind", exp->srcp);
    }
}


/*----------------------------------------------------------------------*/
static void generateAttributeExpression(Expression *exp)
{
    generateAttributeReference(exp);
    generateAttributeAccess(exp);
    if (exp->not) emit0(I_NOT);
}


/*======================================================================*/
void generateFilter(Expression *exp)
{
    /* Generate a right hand side expression, filter, where the left
       hand has been evaluated so that the stack contains the lhs on
       top.  NOTE that the lhs is NULL in all these cases.  This is used
       for aggregate and loop filters. */

    switch (exp->kind) {
    case WHERE_EXPRESSION:
        generateWhereRHS(exp->fields.whr.whr);
        break;
    case ISA_EXPRESSION:
        generateId(exp->fields.isa.class, exp->type);
        emit0(I_ISA);
        break;
    case BINARY_EXPRESSION:
        generateExpression(exp->fields.bin.right);
        generateBinaryOperator(exp);
        break;
    case ATTRIBUTE_EXPRESSION:
        generateId(exp->fields.atr.id, exp->type);
        generateAttributeAccess(exp);
        break;
    case BETWEEN_EXPRESSION:
        generateBetweenCheck(exp);
        break;
    default:
        SYSERR("Unimplemented aggregate filter expression", exp->srcp);
    }
    if (exp->not) emit0(I_NOT);
}


#define MAXINT (0x07ffffff)

/*----------------------------------------------------------------------*/
static void generateIntegerAggregateLimit(Expression *exp) {
    List *filter;

    ITERATE(filter, exp->fields.agr.filters) {
        if (filter->member.exp->kind == WHERE_EXPRESSION)
            if (filter->member.exp->fields.whr.whr->kind == WHERE_INSET) {
                generateExpression(filter->member.exp->fields.whr.whr->what);
                exp->fields.agr.setExpression = filter->member.exp;
                emit0(I_SETSIZE);
                return;
            }
    }
}


/*----------------------------------------------------------------------*/
static void generateIntegerAggregateLoopValue(Expression *exp) {
    generateExpression(exp->fields.whr.whr->what);
    emit0(I_SETMEMB);
}


/*----------------------------------------------------------------------*/
static void generateLoopValue(Expression *exp) {
    /* Calcuate loop value, usually the same as the index */
    emit0(I_DUP);
    if (exp->fields.agr.type == INTEGER_TYPE)
        generateIntegerAggregateLoopValue(exp->fields.agr.setExpression);
}

static void generateInitialAggregationValue(Expression *exp) {
    switch (exp->fields.agr.kind) {
    case COUNT_AGGREGATE:
    case MAX_AGGREGATE:
    case SUM_AGGREGATE: emitConstant(0); break;
    case MIN_AGGREGATE: emitConstant(MAXINT); break;
    }
}

static void generateLoopLimit(Expression *exp) {
    if (exp->fields.agr.type == INTEGER_TYPE)
        generateIntegerAggregateLimit(exp);
    else
        emitVariable(V_MAX_INSTANCE);	/* Loop limit */
}

static void generateLoopStart() {
    emitConstant(1);
    emit0(I_LOOP);
}

static void generateAttributeExistanceFilter(Expression *exp) {
    generateLoopValue(exp);
    generateSymbol(definingSymbolOfAttribute(exp->fields.agr.class, exp->fields.agr.attribute));
    emit0(I_ISA);
    emit0(I_NOT);
    emit0(I_IF);
    emit0(I_LOOPNEXT);
    emit0(I_ENDIF);
}

static void generateAggregationFilter(Expression *exp, List *lst) {
    generateLoopValue(exp);
    generateFilter(lst->member.exp);
    emit0(I_NOT);
    emit0(I_IF);
    emit0(I_LOOPNEXT);
    emit0(I_ENDIF);
}

static void generateAllFilters(Expression *exp) {
    List *lst;
    ITERATE(lst,exp->fields.agr.filters) {
        generateAggregationFilter(exp, lst);
    }
}

static void generateAttributeRetrieval(Expression *exp) {
    generateLoopValue(exp);
    emitConstant(exp->fields.agr.attribute->code);
    emit0(I_ATTRIBUTE);		/* Cannot be anything but INTEGER */
}

static void generateAggregation(Expression *exp) {
    switch (exp->fields.agr.kind) {
    case SUM_AGGREGATE: emit0(I_SUM); break;
    case MAX_AGGREGATE: emit0(I_MAX); break;
    case MIN_AGGREGATE: emit0(I_MIN); break;
    case COUNT_AGGREGATE: emit0(I_COUNT); break;
    }
}

static void generateLoopEnd() {
    emit0(I_LOOPEND);
}


/*----------------------------------------------------------------------*/
static void generateAggregateExpression(Expression *exp)
{
    generateInitialAggregationValue(exp);
    generateLoopLimit(exp);
    generateLoopStart();
    if (exp->fields.agr.kind != COUNT_AGGREGATE)
        generateAttributeExistanceFilter(exp);
    generateAllFilters(exp);
    if (exp->fields.agr.kind != COUNT_AGGREGATE)
        generateAttributeRetrieval(exp);
    generateAggregation(exp);
    generateLoopEnd();
}



/*----------------------------------------------------------------------*/
static void generateRandomExpression(Expression *exp)
{
    generateExpression(exp->fields.rnd.to);
    generateExpression(exp->fields.rnd.from);
    emit0(I_RND);
}

/*----------------------------------------------------------------------*/
static void generateRandomInExpression(Expression *exp)
{
    generateExpression(exp->fields.rin.what);
    if (exp->fields.rin.what->type == SET_TYPE)
        emit0(I_SETSIZE);
    else {
        emitConstant(exp->fields.rin.transitivity);
        emit0(I_CONTSIZE);
    }
    emitConstant(1);		/* Lower random value */
    emit0(I_RND);
    generateExpression(exp->fields.rin.what);
    if (exp->fields.rin.what->type == SET_TYPE)
        emit0(I_SETMEMB);
    else {
        emitConstant(exp->fields.rin.transitivity);
        emit0(I_CONTMEMB);
    }
}


/*----------------------------------------------------------------------*/
static void generateScoreExpression(Expression *exp)
{
    emitVariable(V_SCORE);
}


/*----------------------------------------------------------------------*/
static void generateWhatExpression(Expression *exp)
{
    generateWhat(exp->fields.wht.wht, exp->type);
}



/*======================================================================*/
void generateBetweenCheck(Expression *exp)
{
    generateExpression(exp->fields.btw.lowerLimit);
    generateExpression(exp->fields.btw.upperLimit);
    emit0(I_BTW);
}


/*----------------------------------------------------------------------*/
static void generateBetweenExpression(Expression *exp)
{
    generateExpression(exp->fields.btw.exp);
    generateBetweenCheck(exp);
    if (exp->not) emit0(I_NOT);
}


/*----------------------------------------------------------------------*/
static void generateIsaExpression(Expression *exp)
{
    generateExpression(exp->fields.isa.what);
    generateId(exp->fields.isa.class, exp->type);
    emit0(I_ISA);
    if (exp->not) emit0(I_NOT);
}


/*----------------------------------------------------------------------*/
static void generateSetExpression(Expression *exp) {
    List *members;

    emit0(I_NEWSET);
    ITERATE(members, exp->fields.set.members) {
        generateExpression(members->member.exp);
        emit0(I_INCLUDE);		/* Add member to set on top of stack */
    }
}

/*======================================================================*/
void generateExpression(Expression *exp)
{
    if (exp == NULL) {
        SYSERR("Generating a NULL expression", nulsrcp);
        emitConstant(0);
        return;
    }

    if ((Bool)opts[OPTDEBUG].value)
        generateSrcp(exp->srcp);

    switch (exp->kind) {

    case BINARY_EXPRESSION:
        generateBinaryExpression(exp);
        break;

    case WHERE_EXPRESSION:
        generateWhereExpression(exp);
        break;

    case ATTRIBUTE_EXPRESSION:
        generateAttributeExpression(exp);
        break;

    case INTEGER_EXPRESSION:
        emitConstant(exp->fields.val.val);
        break;

    case STRING_EXPRESSION:
        if (!exp->fields.str.encoded)
            encode(&exp->fields.str.fpos, &exp->fields.str.len);
        exp->fields.str.encoded = TRUE;
        emit2(I_GETSTR, exp->fields.str.fpos, exp->fields.str.len);
        break;

    case AGGREGATE_EXPRESSION:
        generateAggregateExpression(exp);
        break;

    case RANDOM_EXPRESSION:
        generateRandomExpression(exp);
        break;

    case RANDOM_IN_EXPRESSION:
        generateRandomInExpression(exp);
        break;

    case SCORE_EXPRESSION:
        generateScoreExpression(exp);
        break;

    case WHAT_EXPRESSION:
        generateWhatExpression(exp);
        break;

    case BETWEEN_EXPRESSION:
        generateBetweenExpression(exp);
        break;

    case ISA_EXPRESSION:
        generateIsaExpression(exp);
        break;

    case SET_EXPRESSION:
        generateSetExpression(exp);
        break;
    }
}



/*----------------------------------------------------------------------*/
static void dumpOperator(OperatorKind op)
{
    switch (op) {
    case AND_OPERATOR:
        put("AND");
        break;
    case OR_OPERATOR:
        put("OR");
        break;
    case NE_OPERATOR:
        put("<>");
        break;
    case EQ_OPERATOR:
        put("=");
        break;
    case EXACT_OPERATOR:
        put("==");
        break;
    case LE_OPERATOR:
        put("<=");
        break;
    case GE_OPERATOR:
        put(">=");
        break;
    case LT_OPERATOR:
        put("LT");
        break;
    case GT_OPERATOR:
        put("GT");
        break;
    case PLUS_OPERATOR:
        put("PLUS");
        break;
    case MINUS_OPERATOR:
        put("MINUS");
        break;
    case MULT_OPERATOR:
        put("MULT");
        break;
    case DIV_OPERATOR:
        put("DIV");
        break;
    case CONTAINS_OPERATOR:
        put("CONTAINS");
        break;
    }
}



/*----------------------------------------------------------------------*/
static void dumpAggregateKind(AggregateKind agr)
{
    put(aggregateToString(agr));
}


/*======================================================================*/
void dumpExpression(Expression *exp)
{
    if (exp == NULL) {
        put("NULL");
        return;
    }

    put("EXP: ");
    switch (exp->kind) {
    case BINARY_EXPRESSION:
        put("BIN ");
        break;
    case WHERE_EXPRESSION:
        put("WHR ");
        if (exp->not) put("NOT ");
        break;
    case ATTRIBUTE_EXPRESSION:
        if (exp->not) put("NOT ");
        put("ATR ");
        break;
    case INTEGER_EXPRESSION:
        put("INT ");
        break;
    case STRING_EXPRESSION:
        put("STR ");
        break;
    case SET_EXPRESSION:
        put("SET ");
        break;
    case AGGREGATE_EXPRESSION:
        put("AGR ");
        break;
    case RANDOM_EXPRESSION:
        put("RND ");
        break;
    case RANDOM_IN_EXPRESSION:
        put("RIN ");
        break;
    case SCORE_EXPRESSION:
        put("SCORE ");
        break;
    case WHAT_EXPRESSION:
        put("WHT ");
        break;
    case BETWEEN_EXPRESSION:
        if (exp->not) put("NOT ");
        put("BTW ");
        break;
    case ISA_EXPRESSION:
        if (exp->not) put("NOT ");
        put("ISA ");
        break;
    }

    dumpSrcp(exp->srcp);
    indent();
    put("type: "); dumpType(exp->type); nl();
    put("class: "); dumpSymbol(exp->class); nl();

    switch (exp->kind) {
    case WHERE_EXPRESSION:
        put("wht: "); dumpExpression(exp->fields.whr.wht); nl();
        put("whr: "); dumpWhere(exp->fields.whr.whr);
        break;
    case ATTRIBUTE_EXPRESSION:
        put("wht: "); dumpExpression(exp->fields.atr.wht); nl();
        put("atr: "); dumpId(exp->fields.atr.id);
        break;
    case INTEGER_EXPRESSION:
        put("val: "); dumpInt(exp->fields.val.val);
        break;
    case STRING_EXPRESSION:
        put("fpos: "); dumpInt(exp->fields.str.fpos); nl();
        put("len: "); dumpInt(exp->fields.str.len);
        break;
    case SET_EXPRESSION:
        put("set: "); dumpList(exp->fields.set.members, EXPRESSION_LIST); nl();
        put("memberType: "); dumpType(exp->fields.set.memberType); nl();
        put("memberClass: "); dumpSymbol(exp->fields.set.memberClass); nl();
        put("address: "); dumpInt(exp->fields.set.address);
        break;
    case BINARY_EXPRESSION:
        put("operator: "); dumpOperator(exp->fields.bin.op); nl();
        put("left: "); dumpExpression(exp->fields.bin.left); nl();
        put("right: "); dumpExpression(exp->fields.bin.right);
        break;
    case AGGREGATE_EXPRESSION:
        put("kind: "); dumpAggregateKind(exp->fields.agr.kind); nl();
        put("attribute: "); dumpId(exp->fields.agr.attribute); nl();
        put("filters: "); dumpList(exp->fields.agr.filters, EXPRESSION_LIST);
        break;
    case RANDOM_EXPRESSION:
        put("from: "); dumpExpression(exp->fields.rnd.from); nl();
        put("to: "); dumpExpression(exp->fields.rnd.to);
        break;
    case RANDOM_IN_EXPRESSION:
        put("what: "); dumpExpression(exp->fields.rin.what); nl();
        put("transitivity: "); dumpTransitivity(exp->fields.rin.transitivity);
        break;
    case WHAT_EXPRESSION:
        put("wht: "); dumpWhat(exp->fields.wht.wht);
        break;
    case SCORE_EXPRESSION:
        break;
    case BETWEEN_EXPRESSION:
        put("val: "); dumpExpression(exp->fields.btw.exp); nl();
        put("lower: "); dumpExpression(exp->fields.btw.lowerLimit); nl();
        put("upper: "); dumpExpression(exp->fields.btw.upperLimit);
        break;
    case ISA_EXPRESSION:
        put("wht: "); dumpExpression(exp->fields.isa.what); nl();
        put("id: "); dumpId(exp->fields.isa.class);
        break;
    }
    out();
}
