/*----------------------------------------------------------------------*\

  STM.C
  Statement Nodes

\*----------------------------------------------------------------------*/

#include "stm_x.h"


#include "alan.h"
#include "util.h"

#include "id_x.h"
#include "lst_x.h"
#include "srcp_x.h"
#include "context_x.h"
#include "adv_x.h"
#include "atr_x.h"
#include "cnt_x.h"
#include "exp_x.h"
#include "set_x.h"
#include "sym_x.h"
#include "whr_x.h"
#include "wht_x.h"
#include "type_x.h"
#include "resource_x.h"
#include "dump_x.h"

#include "lmList.h"

#include "scr.h"
#include "sco.h"
#include "opt.h"

#include "emit.h"
#include "encode.h"





/*======================================================================*/
Statement *newStatement(Srcp *srcp, StmKind class)
{
    Statement *new;                  /* The newly allocated area */

	progressCounter();

	new = NEW(Statement);

	new->srcp = *srcp;
	new->kind = class;

	return(new);
}

/*======================================================================*/
Statement *newDescribeStatement(Srcp srcp, Expression *what)
{
	Statement *new = newStatement(&srcp, DESCRIBE_STATEMENT);
	new->fields.describe.what = what;
	return(new);
}



/*======================================================================*/
Statement *newUseStatement(Srcp srcp, Id *script, Expression *actor)
{
	Statement *new = newStatement(&srcp, USE_STATEMENT);
	new->fields.use.script = script;
	new->fields.use.actorExp = actor;
	return(new);
}



/*======================================================================*/
Statement *newLocateStatement(Srcp srcp, Expression *what, Where *where)
{
	Statement *new = newStatement(&srcp, LOCATE_STATEMENT);
	new->fields.locate.what = what;
	new->fields.locate.where = where;
	return(new);
}


/*======================================================================*/
Statement *newEmptyStatement(Srcp srcp, Expression *what, Where *where)
{
	Statement *new = newStatement(&srcp, EMPTY_STATEMENT);
	new->fields.empty.what = what;
	new->fields.empty.where = where;
	return(new);
}


/*======================================================================*/
Statement *newIncludeStatement(Srcp srcp, Expression *what, Expression *set)
{
	Statement *new = newStatement(&srcp, INCLUDE_STATEMENT);
	new->fields.include.what = what;
	new->fields.include.set = set;
	return(new);
}


/*======================================================================*/
Statement *newExcludeStatement(Srcp srcp, Expression *what, Expression *set)
{
	Statement *new = newStatement(&srcp, EXCLUDE_STATEMENT);
	new->fields.include.what = what;
	new->fields.include.set = set;
	return(new);
}



/*======================================================================*/
Statement *newEachStatement(Srcp srcp, Id *loopId, List *filters, List *statements)
{
	Statement *new = newStatement(&srcp, EACH_STATEMENT);
	new->fields.each.loopId = loopId;
	new->fields.each.filters = filters;
	new->fields.each.stms = statements;
	return(new);
}


/*======================================================================*/
Statement *newStyleStatement(Srcp srcp, Id *style)
{
	Statement *new = newStatement(&srcp, STYLE_STATEMENT);

	if (strcasecmp(style->string, "normal") == 0)
		style->code = NORMAL_STYLE;
	else if (strcasecmp(style->string, "emphasized") == 0)
		style->code = EMPHASIZED_STYLE;
	else if (strcasecmp(style->string, "preformatted") == 0)
		style->code = PREFORMATTED_STYLE;
	else if (strcasecmp(style->string, "alert") == 0)
		style->code = ALERT_STYLE;
	else if (strcasecmp(style->string, "quote") == 0)
		style->code = QUOTE_STYLE;
	else
		lmLog(&style->srcp, 550, sevWAR, "'normal', 'emphasized', 'preformatted', 'alert' or 'quote'");

	new->fields.style.style = style->code;
	return(new);
}


/*======================================================================*/
Statement *newScheduleStatement(Srcp srcp, Expression *what, Where *where, Expression *when)
{
	Statement *new = newStatement(&srcp, SCHEDULE_STATEMENT);
	new->fields.schedule.what = what;
	new->fields.schedule.whr = where;
	new->fields.schedule.when = when;
	return(new);
}

/*======================================================================*/
Statement *newCancelStatement(Srcp srcp, Expression *what)
{
	Statement *new = newStatement(&srcp, CANCEL_STATEMENT);
	new->fields.cancel.what = what;
	return(new);
}

/*======================================================================*/
Statement *newShowStatement(Srcp srcp, Resource *resource)
{
	Statement *new = newStatement(&srcp, SHOW_STATEMENT);
	new->fields.show.resource = resource;
	return(new);
}

/*======================================================================*/
Statement *newPlayStatement(Srcp srcp, Resource *resource)
{
	Statement *new = newStatement(&srcp, PLAY_STATEMENT);
	new->fields.show.resource = resource;
	return(new);
}

/*======================================================================*/
Statement *newListStatement(Srcp srcp, Expression *what)
{
	Statement *new = newStatement(&srcp, LIST_STATEMENT);
	new->fields.list.wht = what;
	return(new);
}

/*----------------------------------------------------------------------*/
static void analyzeDescribe(Statement *stm, Context *context)
{
	analyzeExpression(stm->fields.describe.what, context);
}


/*======================================================================*/
Statement *newPrintStatement(Srcp srcp, int fpos, int length) {
	Statement *stm;
	/* Create a PRINT statement and enter the print info */
	stm = newStatement(&nulsrcp, PRINT_STATEMENT);
	stm->fields.print.fpos = fpos;
	stm->fields.print.len = length;
	return(stm);
}


/*======================================================================*/
Statement *newPrintStatementFromString(char *string) {
	int fpos;
	int length;

	fpos = ftell(txtfil);
	length = strlen(string);
	fprintf(txtfil, "%s", string);
	return newPrintStatement(nulsrcp, fpos, length);
}


/*======================================================================*/
List *newPrintStatementListFromString(char *string) {
	return newList(newPrintStatementFromString(string), STATEMENT_LIST);
}

/*----------------------------------------------------------------------*/
static void analyzePrint(Statement *stm, Context *context)
{
	int i;
	int parameter;
	char *buffer = allocate(stm->fields.print.len+1);

	if (context && context->kind == RULE_CONTEXT)
		lmLog(&stm->srcp, 444, sevERR, "Rules");

	fseek(txtfil, stm->fields.print.fpos, SEEK_SET);
	fread(buffer, 1, stm->fields.print.len, txtfil);
	fseek(txtfil, 0, SEEK_END);

	for (i = 0; i < stm->fields.print.len-1; i++) {
		if (buffer[i] == '$') {
			parameter = 0;
			i++;
			if (!isdigit((int)buffer[i]) || buffer[i] == '0') {
				if (strchr("+0-!", buffer[i]) != NULL) {
					i++;
					if (isdigit((int)buffer[i]))
						parameter = buffer[i] - '0';
				}
			} else
				parameter = buffer[i] - '0';
			if (parameter != 0)
				if (context == NULL || context->kind != VERB_CONTEXT
					|| parameter > length(context->verb->fields.verb.parameterSymbols))
					lmLog(&stm->srcp, 551, sevERR, "");
		}
	}
}



/*----------------------------------------------------------------------*/
static void analyzeSay(Statement *stm, Context *context)
{
	if (context && context->kind == RULE_CONTEXT)
		lmLog(&stm->srcp, 444, sevERR, "Rules");

	analyzeExpression(stm->fields.say.exp, context);

	/* Can't say Boolean values or Sets */
	switch (stm->fields.say.exp->type) {
	case BOOLEAN_TYPE:
	case SET_TYPE:
	case EVENT_TYPE:
		lmLog(&stm->srcp, 337, sevERR, typeToString(stm->fields.say.exp->type));
		break;
	default:
		break;
	}

	/* Can only use definite/indefinite forms if What is a instance */
	if (stm->fields.say.form != SAY_SIMPLE
		&& (stm->fields.say.exp->type != INSTANCE_TYPE
			&& stm->fields.say.exp->type != REFERENCE_TYPE
			&& stm->fields.say.exp->type != UNINITIALIZED_TYPE))
		lmLog(&stm->srcp, 339, sevERR, "");
}


/*----------------------------------------------------------------------*/
static void analyzeList(Statement *stm, Context *context)
{
	analyzeExpression(stm->fields.list.wht, context);
	verifyContainerExpression(stm->fields.list.wht, context, "LIST statement");
}


/*----------------------------------------------------------------------*/
static void analyzeEmpty(Statement *stm, Context *context)
{
    Expression *what = stm->fields.empty.what;
    Where *where = stm->fields.empty.where;

	analyzeExpression(what, context);
	verifyContainerExpression(what, context, "EMPTY statement");

	if (where->kind == WHERE_NEARBY)
		lmLog(&where->srcp, 415, sevERR, "EMPTY");
	if (where->transitivity != DEFAULT_TRANSITIVITY) {
        if (where->transitivity == DIRECTLY)
            lmLogv(&where->srcp, 422, sevWAR,
                   transitivityToString(where->transitivity),
                   "ignored in", "EMPTY statement", NULL);
        else
            lmLogv(&where->srcp, 422, sevERR,
                   transitivityToString(where->transitivity),
                   "not allowed in", "EMPTY statement", NULL);
    }

	analyzeWhere(where, context);
}


/*----------------------------------------------------------------------*/
static void analyzeLocate(Statement *stm, Context *context)
{
	Symbol *whtSymbol = NULL;
	Symbol *taken_class = NULL;
	Expression *what = stm->fields.locate.what;
	Where *where = stm->fields.locate.where;

	analyzeExpression(what, context);
	if (what->type != ERROR_TYPE) {
		if (what->type != INSTANCE_TYPE)
			lmLogv(&what->srcp, 428, sevERR, "What-clause in Locate statement", "an instance", NULL);
		else if (what->class) {
			if (!inheritsFrom(what->class, thingSymbol) && !inheritsFrom(what->class, locationSymbol))
				lmLog(&what->srcp, 405, sevERR, "be used in Locate statement");
			whtSymbol = what->class;
		}
	}
	if (where->transitivity != DEFAULT_TRANSITIVITY) {
        if (where->transitivity == DIRECTLY)
            lmLogv(&where->srcp, 422, sevWAR,
                   transitivityToString(where->transitivity), "ignored in", "LOCATE statement", NULL);
        else
            lmLogv(&where->srcp, 422, sevERR,
                   transitivityToString(where->transitivity), "not allowed in", "LOCATE statement", NULL);
    }

	analyzeWhere(where, context);
    where->transitivity = DIRECTLY;

	switch (where->kind) {
	case WHERE_HERE:
	case WHERE_AT:
		break;
	case WHERE_IN:
        /* Can the located be in a container? Not if its a location or actor. */
        /* TODO: Refactor to use a list of illegal container classes */
		if (inheritsFrom(what->class, locationSymbol))
			lmLog(&what->srcp, 402, sevERR, "A Location");
		else if (inheritsFrom(what->class, actorSymbol))
			lmLog(&what->srcp, 402, sevERR, "An Actor");
		taken_class = containerContent(where->what, DIRECTLY, context);
		if (taken_class != NULL && whtSymbol != NULL)
			if (!inheritsFrom(whtSymbol, taken_class))
				lmLog(&where->srcp, 404, sevERR, taken_class->string);
		break;
	case WHERE_NEAR:
	case WHERE_NEARBY:
		lmLog(&stm->srcp, 415, sevERR, "LOCATE");
		break;
	default:
		SYSERR("Unexpected Where kind", where->srcp);
		break;
	}
}



/*----------------------------------------------------------------------*/
static void verifyMakeAttribute(Id *attributeId, Attribute *foundAttribute)
{
	/* Verify that a found attribute can be used in a MAKE statement. */
	if (foundAttribute != NULL) {
		if (foundAttribute->type != BOOLEAN_TYPE && foundAttribute->type != ERROR_TYPE)
			lmLogv(&attributeId->srcp, 408, sevERR, "Attribute", "MAKE statement", "boolean", NULL);
		else
			attributeId->code = foundAttribute->id->code;
	}
}


/*----------------------------------------------------------------------*/
static void analyzeMake(Statement *stm, Context *context)
{
	Expression *wht = stm->fields.make.wht;
	Attribute *atr = NULL;

	analyzeExpression(wht, context);
	atr = resolveAttributeToExpression(wht, stm->fields.make.atr, context);
	verifyMakeAttribute(stm->fields.make.atr, atr);
    if (inheritsFrom(wht->class, literalSymbol))
        lmLog(&stm->srcp, 406, sevERR, "");
}


/*----------------------------------------------------------------------*/
static void verifySetAssignment(Expression *exp, Expression *wht) {
	if (!inheritsFrom(exp->class, wht->class)) {
		/* An empty set can be assigned to any set varible */
		if (exp->kind == SET_EXPRESSION && length(exp->fields.set.members) == 0)
			;
		else
			lmLog(&exp->srcp, 431, sevERR, wht->class->string);
	}
}


/*----------------------------------------------------------------------*/
static void analyzeSet(Statement *stm, Context *context)
{
    Expression *exp = stm->fields.set.exp;
    Expression *wht = stm->fields.set.wht;

    analyzeExpression(wht, context);
    if (wht->type != ERROR_TYPE) {
        if (wht->readonly)
            lmLog(&wht->srcp, 436, sevERR, "");
        if (wht->type == BOOLEAN_TYPE)
            lmLog(&wht->srcp, 419, sevERR, "Target for");
    }
    if (inheritsFrom(wht->fields.atr.wht->class, literalSymbol))
        lmLog(&stm->srcp, 406, sevERR, "");

    analyzeExpression(exp, context);
    if (exp->type != ERROR_TYPE)
        if (exp->type == BOOLEAN_TYPE)
            lmLog(&exp->srcp, 419, sevERR, "Expression in");

    if (!equalTypes(exp->type, wht->type))
        lmLog(&stm->srcp, 331, sevERR, "target and expression in SET statement");
    else {
        if (exp->class != NULL && wht->class != NULL) {
            if (exp->type == INSTANCE_TYPE) {
                if (!inheritsFrom(exp->class, wht->class))
                    lmLog(&exp->srcp, 430, sevERR, wht->class->string);
            } else if (exp->type == SET_TYPE) {
                verifySetAssignment(exp, wht);
            }
        }
    }
}


/*----------------------------------------------------------------------*/
static void analyzeIncrease(Statement *stm, Context *context)
{
    Expression *wht = stm->fields.incr.wht;
    analyzeExpression(wht, context);
    if (wht->readonly)
        lmLog(&wht->srcp, 436, sevERR, "");


    if (stm->fields.incr.step != NULL) {
        analyzeExpression(stm->fields.incr.step, context);
        if (stm->fields.incr.step->type != INTEGER_TYPE
            && stm->fields.incr.step->type != ERROR_TYPE)
            lmLogv(&stm->fields.incr.step->srcp, 408, sevERR, "Expression",
                   stm->kind==INCREASE_STATEMENT?"INCREASE statement":"DECREASE statement",
                   "integer", NULL);
    }
}


/*----------------------------------------------------------------------*/
static void analyzeIncludeAndExclude(Statement *stm, Context *context)
{
    Expression *what = stm->fields.include.what;
    Expression *set = stm->fields.include.set;
    char *message = stm->kind == INCLUDE_STATEMENT?"INCLUDE statement"
        :"EXCLUDE statement";

    analyzeExpression(what, context);
    analyzeExpression(set, context);
    if (set->type != ERROR_TYPE) {
        if (set->type != SET_TYPE)
            lmLogv(&set->srcp, 330, sevERR, "Set", message, NULL);
        else
            verifySetMember(set, what, message);
    }
}

/*----------------------------------------------------------------------*/
static void analyzeSchedule(Statement *stm, Context *context)
{
    Expression *what = stm->fields.schedule.what;
    Where *whr = stm->fields.schedule.whr;

    analyzeExpression(what, context);
    if (what->type != ERROR_TYPE && what->type != EVENT_TYPE)
        lmLog(&what->srcp, 331, sevERR, "SCHEDULE statement. Event type required");

    /* Now lookup where-clause */
    analyzeWhere(whr, context);
    whr->transitivity = DIRECTLY;

    switch (whr->kind) {
    case WHERE_DEFAULT:
        if (context->kind == RULE_CONTEXT)
            lmLog(&stm->srcp, 445, sevWAR, "");
        whr->kind = WHERE_HERE;
        break;
    case WHERE_HERE:
    case WHERE_AT:
        break;
    case WHERE_IN:
    case WHERE_NEAR:
    case WHERE_NEARBY:
        lmLog(&whr->srcp, 415, sevERR, "SCHEDULE");
        break;
    default:
        SYSERR("Unrecognized switch", whr->srcp);
        break;
    }

    /* Analyze the when (AFTER) expression */
    analyzeExpression(stm->fields.schedule.when, context);
    if (stm->fields.schedule.when->type != INTEGER_TYPE)
        lmLog(&stm->fields.schedule.when->srcp, 331, sevERR, "When-clause of SCHEDULE statement");

}


/*----------------------------------------------------------------------*/
static void analyzeCancel(Statement *stm, Context *context)
{
    Expression *what = stm->fields.cancel.what;

    analyzeExpression(what, context);
    if (what->type != ERROR_TYPE &&
        what->type != EVENT_TYPE)
        lmLog(&stm->fields.cancel.what->srcp, 331, sevERR, "CANCEL statement. Event type required");
}


/*----------------------------------------------------------------------*/
static Bool is_restricting_expression(Expression *exp) {
    return exp->kind == ISA_EXPRESSION
        && exp->fields.isa.what->kind == WHAT_EXPRESSION
        && exp->fields.isa.what->fields.wht.wht->kind == WHAT_ID;
}


/*----------------------------------------------------------------------*/
static void analyzeIf(Statement *stm, Context *context)
{
    Expression *exp = stm->fields.iff.exp;

    analyzeExpression(exp, context);
    if (!equalTypes(exp->type, BOOLEAN_TYPE))
        lmLogv(&exp->srcp, 330, sevERR, "boolean", "'IF'", NULL);

    if (is_restricting_expression(exp)) {
        Context *restricted_context = pushContext(context);
        addRestrictionInContext(restricted_context, exp);
        analyzeStatements(stm->fields.iff.thn, restricted_context);
        free(restricted_context);
    } else
        analyzeStatements(stm->fields.iff.thn, context);

    if (stm->fields.iff.els != NULL)
        analyzeStatements(stm->fields.iff.els, context);
}


/*----------------------------------------------------------------------*/
static void findScript(Symbol *symbol, Id *scriptId) {
    Script *script;
    script = lookupScript(symbol, scriptId);
    if (script != NULL)
        scriptId->code = script->id->code;
    else {
        char *str = "class";
        switch (symbol->kind) {
        case CLASS_SYMBOL: str = "class"; break;
        case LOCAL_SYMBOL:
        case INSTANCE_SYMBOL: str = "actor"; break;
        case PARAMETER_SYMBOL: str = "parameter"; break;
        default: SYSERR("Unexpected symbol kind", scriptId->srcp);
        }
        lmLogv(&scriptId->srcp, 400, sevERR, scriptId->string, str, symbol->string, NULL);
    }
}


/*----------------------------------------------------------------------*/
static Symbol *analyzeIdForActorStatement(Id *id, Context *context) {
    if (id->symbol != NULL)
        switch (id->symbol->kind) {
        case PARAMETER_SYMBOL:
        case LOCAL_SYMBOL:
            return classOfIdInContext(context, id);
        case INSTANCE_SYMBOL:
            return id->symbol;
        default: SYSERR("Unexpected id->symbol->kind", id->srcp);
        }
    return NULL;
}


/*----------------------------------------------------------------------*/
static Symbol *analyzeWhatForActorStatement(What *wht, Context *context) {
    switch (wht->kind) {
    case WHAT_ID:
        return analyzeIdForActorStatement(wht->id, context);
    case WHAT_THIS:
    case WHAT_LOCATION:
    case WHAT_ACTOR:
        return symbolOfWhat(wht, context);
    }
    return NULL;
}


/*----------------------------------------------------------------------*/
static Symbol *analyzeUseWithActor(Statement *stm, Context *context) {
    Expression *exp = stm->fields.use.actorExp;
    analyzeExpression(exp, context);
    if (exp->type != ERROR_TYPE)
        if (exp->type != INSTANCE_TYPE || !inheritsFrom(exp->class, actorSymbol)) {
            lmLogv(&exp->srcp, 351, sevERR, "USE statement", "an instance", "actor", NULL);
            return NULL;
        }
    switch (exp->kind) {
    case WHAT_EXPRESSION:
        return analyzeWhatForActorStatement(exp->fields.wht.wht, context);
    case ATTRIBUTE_EXPRESSION:
        return symbolOfExpression(exp, context);
    default: SYSERR("Unexpected exp->kind", exp->srcp);
    }
    return NULL;
}


/*----------------------------------------------------------------------*/
static Symbol *analyzeUseWithoutActor(Statement *stm, Context *context) {
    Symbol *sym = NULL;
    if (context->kind == INSTANCE_CONTEXT) {
        if (context->instance == NULL || context->instance->props == NULL)
            SYSERR("Strange context", stm->srcp);
        if (!inheritsFrom(context->instance->props->id->symbol, actorSymbol))
            lmLog(&stm->srcp, 356, sevERR, "");
        else
            sym = context->instance->props->id->symbol;
    } else if (context->kind == CLASS_CONTEXT) {
        if (context->class == NULL || context->class->props == NULL)
            SYSERR("Strange context", stm->srcp);
        if (!inheritsFrom(context->class->props->id->symbol, actorSymbol))
            lmLog(&stm->srcp, 356, sevERR, "");
        else
            sym = context->class->props->id->symbol;
    }
	return sym;
}


/*----------------------------------------------------------------------*/
static void analyzeUse(Statement *stm, Context *context)
{
	/* Analyze a USE statement. It must refer to a script that is
	   defined within the mentioned actor. If the actor is not specified
	   the actor is assumed to be the one we are in (it is an error if we
	   are not). */

	Symbol *symbol = NULL;

	if (stm->fields.use.actorExp == NULL && context->kind != CLASS_CONTEXT && context->kind != INSTANCE_CONTEXT)
		lmLog(&stm->srcp, 401, sevERR, "");
	else {
		if (stm->fields.use.actorExp != NULL)
            symbol = analyzeUseWithActor(stm, context);
		else
            symbol = analyzeUseWithoutActor(stm, context);

		if (symbol != NULL)
            findScript(symbol, stm->fields.use.script);
	}
}


/*----------------------------------------------------------------------*/
static void analyzeStop(Statement *stm, Context *context)
{
	Symbol *sym;
	Expression *exp = stm->fields.stop.actor;

	analyzeExpression(exp, context);
	if (exp->type != ERROR_TYPE) {
        if (exp->kind == WHAT_EXPRESSION && exp->fields.wht.wht->kind == WHAT_ID
            && exp->fields.wht.wht->id->symbol != NULL
            && exp->fields.wht.wht->id->symbol->kind != INSTANCE_SYMBOL)
            sym = classOfIdInContext(context, exp->fields.wht.wht->id);
        else
            sym = symbolOfExpression(exp, context);
		if (sym) {
			if (!inheritsFrom(sym, actorSymbol))
				lmLogv(&stm->fields.stop.actor->srcp, 351, sevERR, "STOP statement", "an instance", "actor", NULL);
		}
	}
}


/*----------------------------------------------------------------------*/
static void analyzeDepend(Statement *stm, Context *context)
{
	/* Analyze a DEPENDING statement. The case clauses have partial expressions
	   (operator & right hand side) that must be completed with the
	   one in the depend as their left hand side. The case clauses are
	   just hooked to the depend part of the expression. Each case is
	   analyzed as a complete expression, relying on binary expression
	   logic to avoid analyzing the left hand side multiple times. */

	List *cases;

	for (cases = stm->fields.depend.cases; cases != NULL; cases = cases->next) {
		if (cases->member.stm->fields.depcase.exp != NULL) {
			Expression *exp = cases->member.stm->fields.depcase.exp;
			/* Unless it is an ELSE clause, set left hand of case expression
			   to be the depend expression */
			switch (exp->kind) {
			case BINARY_EXPRESSION:
				exp->fields.bin.left = stm->fields.depend.exp;
				break;
			case WHERE_EXPRESSION:
				exp->fields.whr.wht = stm->fields.depend.exp;
				break;
			case ATTRIBUTE_EXPRESSION:
				exp->fields.atr.wht = stm->fields.depend.exp;
				break;
			case BETWEEN_EXPRESSION:
				exp->fields.btw.exp = stm->fields.depend.exp;
				break;
			case ISA_EXPRESSION:
				exp->fields.isa.what = stm->fields.depend.exp;
				break;
			default:
				SYSERR("Unrecognized switch case on Expression kind", exp->srcp);
			}
		} else
			/* If this is an ELSE-case there can not be any other afterwards */
			if (cases->next != NULL)
				lmLog(&cases->member.stm->srcp, 335, sevERR, "");

		/* Analyze the expression and the statements */
		analyzeExpression(cases->member.stm->fields.depcase.exp, context);
		analyzeStatements(cases->member.stm->fields.depcase.stms, context);
	}
}


/*----------------------------------------------------------------------*/
static void analyzeEach(Statement *stm, Context *context)
{
	Symbol *loopSymbol;
	Symbol *class = NULL;

	/* Analyze the partial filter expressions */
	if (stm->fields.each.filters != NULL)
		analyzeFilterExpressions("EACH statement", stm->fields.each.filters,
								 context, &class);

	/* Create a new frame and register the loop variable */
	newFrame();
	loopSymbol = newSymbol(stm->fields.each.loopId, LOCAL_SYMBOL);
	loopSymbol->fields.local.type = INSTANCE_TYPE; /* Assume instances */
	if (class != NULL) {
		loopSymbol->fields.local.class = class;
		if (class == integerSymbol)
			loopSymbol->fields.local.type = INTEGER_TYPE;
	} else
		loopSymbol->fields.local.class = entitySymbol;

	/* Analyze the statements in the loop body */
	analyzeStatements(stm->fields.each.stms, context);

	stm->fields.each.type = loopSymbol->fields.local.type;
	deleteFrame();
}


/*----------------------------------------------------------------------*/
static void analyzeShow(Statement *stm, Context *context)
{
	ResourceKind kind = stm->fields.show.resource->kind;
	analyzeResource(stm->fields.show.resource);
	if (kind != PICT_RESOURCE && kind != NULL_RESOURCE)
		lmLog(&stm->fields.play.resource->fileName->srcp, 450, sevERR, "Show");
	adv.resources = concat(adv.resources, stm->fields.show.resource, RESOURCE_LIST);
}


/*----------------------------------------------------------------------*/
static void analyzePlay(Statement *stm, Context *context)
{
	ResourceKind kind = stm->fields.play.resource->kind;
	analyzeResource(stm->fields.play.resource);
	if (kind != SND_RESOURCE && kind != NULL_RESOURCE)
		lmLog(&stm->fields.play.resource->fileName->srcp, 450, sevERR, "Play");
	adv.resources = concat(adv.resources, stm->fields.play.resource, RESOURCE_LIST);
}


/*----------------------------------------------------------------------*/
static void analyzeStrip(Statement *stm, Context *context)
{
	if (stm->fields.strip.count != NULL) {
		analyzeExpression(stm->fields.strip.count, context);
		if (!equalTypes(stm->fields.strip.count->type, INTEGER_TYPE))
			lmLogv(&stm->fields.strip.count->srcp, 330, sevERR, "integer", "STRIP statement", NULL);
	}

	analyzeExpression(stm->fields.strip.from, context);
	if (!equalTypes(stm->fields.strip.from->type, STRING_TYPE))
		lmLogv(&stm->fields.strip.from->srcp, 330, sevERR, "string", "STRIP statement", NULL);
	if (stm->fields.strip.from->kind != ATTRIBUTE_EXPRESSION)
		lmLogv(&stm->fields.strip.from->srcp, 428, sevERR, "Expression", "an attribute", NULL);

	if (stm->fields.strip.into != NULL) {
		analyzeExpression(stm->fields.strip.into, context);
		if (!equalTypes(stm->fields.strip.into->type, STRING_TYPE))
			lmLogv(&stm->fields.strip.into->srcp, 330, sevERR, "string", "STRIP statement", NULL);
		if (stm->fields.strip.into->kind != ATTRIBUTE_EXPRESSION)
			lmLogv(&stm->fields.strip.into->srcp, 428, sevERR, "Expression", "an attribute", NULL);
	}
}



/*----------------------------------------------------------------------*/
static void analyzeStatement(Statement *stm, Context *context)
{
	switch (stm->kind) {
	case NOP_STATEMENT:
	case STYLE_STATEMENT:
	case QUIT_STATEMENT:
	case LOOK_STATEMENT:
	case SAVE_STATEMENT:
	case RESTORE_STATEMENT:
	case RESTART_STATEMENT:
	case VISITS_STATEMENT:
	case SYSTEM_STATEMENT:
	case TRANSCRIPT_STATEMENT:
		/* Nothing to analyse */
		break;
	case PRINT_STATEMENT:
		analyzePrint(stm, context);
		break;
	case SCORE_STATEMENT:
		if (stm->fields.score.count != 0) {
			adv.scores[stm->fields.score.count] = stm->fields.score.score;
			totalScore += stm->fields.score.score;
		}
		break;
	case DESCRIBE_STATEMENT:
		analyzeDescribe(stm, context);
		break;
	case SAY_STATEMENT:
		analyzeSay(stm, context);
		break;
	case LIST_STATEMENT:
		analyzeList(stm, context);
		break;
	case EMPTY_STATEMENT:
		analyzeEmpty(stm, context);
		break;
	case LOCATE_STATEMENT:
		analyzeLocate(stm, context);
		break;
	case MAKE_STATEMENT:
		analyzeMake(stm, context);
		break;
	case SET_STATEMENT:
		analyzeSet(stm, context);
		break;
	case INCREASE_STATEMENT:
	case DECREASE_STATEMENT:
		analyzeIncrease(stm, context);
		break;
	case INCLUDE_STATEMENT:
	case EXCLUDE_STATEMENT:
		analyzeIncludeAndExclude(stm, context);
		break;
	case SCHEDULE_STATEMENT:
		analyzeSchedule(stm, context);
		break;
	case CANCEL_STATEMENT:
		analyzeCancel(stm, context);
		break;
	case IF_STATEMENT:
		analyzeIf(stm, context);
		break;
	case USE_STATEMENT:
		analyzeUse(stm, context);
		break;
	case STOP_STATEMENT:
		analyzeStop(stm, context);
		break;
	case DEPEND_STATEMENT:
		analyzeDepend(stm, context);
		break;
	case EACH_STATEMENT:
		analyzeEach(stm, context);
		break;
	case SHOW_STATEMENT:
		analyzeShow(stm, context);
		break;
	case PLAY_STATEMENT:
		analyzePlay(stm, context);
		break;
	case STRIP_STATEMENT:
		analyzeStrip(stm, context);
		break;
	default:
		unimpl(stm->srcp, "Analyzer");
		break;
	}
}



/*======================================================================*/
void analyzeStatements(List *stms,
					   Context *context)
{
	while (stms != NULL) {
		analyzeStatement(stms->member.stm, context);
		stms = stms->next;
	}
}


/*----------------------------------------------------------------------*/
static void generatePrint(Statement *stm)
{
	/* Generate the code for a PRINT-stm. The text is found and copied
	   to the data file (and encoded if requested!). */

	if (!stm->fields.print.encoded)
		encode(&stm->fields.print.fpos, &stm->fields.print.len);
	stm->fields.print.encoded = TRUE;
	emit2(I_PRINT, stm->fields.print.fpos, stm->fields.print.len);
}



/*----------------------------------------------------------------------*/
static void generateScore(Statement *stm)
{
	emitConstant(stm->fields.score.count);
	emit0(I_SCORE);
}



/*----------------------------------------------------------------------*/
static void generateDescribe(Statement *stm)
{
	generateExpression(stm->fields.describe.what);
	emit0(I_DESCRIBE);
}


/*----------------------------------------------------------------------*/
static void generateSay(Statement *stm)
{
	generateExpression(stm->fields.say.exp);
	switch (stm->fields.say.exp->type) {
	case INTEGER_TYPE:
		emit0(I_SAYINT);
		break;
	case STRING_TYPE:
		emit0(I_SAYSTR);
		break;
	case REFERENCE_TYPE:
	case INSTANCE_TYPE:
		emit1(I_SAY, stm->fields.say.form);
		break;
	case UNINITIALIZED_TYPE:
		SYSERR("Uninitialized type", stm->srcp);
		break;
	default:
		unimpl(stm->srcp, "Code Generator");
		break;
	}
}


/*----------------------------------------------------------------------*/
static void generateList(Statement *stm)
{
	generateExpression(stm->fields.list.wht);
	emit0(I_LIST);
}


/*----------------------------------------------------------------------*/
static void generateShow(Statement *stm)
{
	emit2(I_SHOW, stm->fields.show.resource->fileName->code, 0);
}


/*----------------------------------------------------------------------*/
static void generatePlay(Statement *stm)
{
	emit1(I_PLAY, stm->fields.play.resource->fileName->code);
}


/*----------------------------------------------------------------------*/
static void generateEmpty(Statement *stm)
{
	generateWhere(stm->fields.empty.where);
	generateExpression(stm->fields.empty.what);
	emit0(I_EMPTY);
}



/*----------------------------------------------------------------------*/
static void generateLocate(Statement *stm)
{
	generateWhere(stm->fields.locate.where);
	generateExpression(stm->fields.locate.what);
	emit0(I_LOCATE);
}


/*----------------------------------------------------------------------*/
static void generateMake(Statement *stm)
{
	emitConstant(!stm->fields.make.not);
	generateExpression(stm->fields.make.wht);
	emitConstant(stm->fields.make.atr->code);
	emit0(I_MAKE);
}


/*----------------------------------------------------------------------*/
static void generateSetStatement(Statement *stm)
{
	generateExpression(stm->fields.set.exp);

	generateLvalue(stm->fields.set.wht);

	switch (stm->fields.set.exp->type) {
	case SET_TYPE:
		emit0(I_SETSET);
		break;
	case STRING_TYPE:
		emit0(I_SETSTR);
		break;
	default:
		emit0(I_SET);
		break;
	}
}


/*----------------------------------------------------------------------*/
static void generateIncrease(Statement *stm)
{
	generateExpression(stm->fields.incr.wht);
	if (stm->fields.incr.step != NULL)
		generateExpression(stm->fields.incr.step);
	else
		emitConstant(1);
	if (stm->kind == INCREASE_STATEMENT)
		emit0(I_INCR);
	else
		emit0(I_DECR);

	generateLvalue(stm->fields.incr.wht);
	emit0(I_SET);
}


/*----------------------------------------------------------------------*/
static void generateIncludeAndExclude(Statement *stm)
{
	generateExpression(stm->fields.include.set);
	generateExpression(stm->fields.include.what);
	if (stm->kind == INCLUDE_STATEMENT)
		emit0(I_INCLUDE);
	else
		emit0(I_EXCLUDE);
	generateLvalue(stm->fields.include.set);
	emit0(I_SETSET);
}


/*----------------------------------------------------------------------*/
static void generateSchedule(Statement *stm)
{
	generateExpression(stm->fields.schedule.when);

	/* NOTE: we can't use gewhr() because the semantics of the schedule */
	/* statement is such that at scheduling AT something does not mean */
	/* where that something is now but where it is when the event is run! */
	switch (stm->fields.schedule.whr->kind) {
	case WHERE_DEFAULT:
	case WHERE_HERE:
		emitVariable(V_CURLOC);
		break;

	case WHERE_AT:
		generateWhat(stm->fields.schedule.whr->what->fields.wht.wht, INSTANCE_TYPE);
		break;

	default:
		unimpl(stm->srcp, "Code Generator");
		return;
	}
	generateExpression(stm->fields.schedule.what);
	emit0(I_SCHEDULE);
}


/*----------------------------------------------------------------------*/
static void generateCancel(Statement *stm) /* IN - Statement to generate */
{
	generateExpression(stm->fields.cancel.what);
	emit0(I_CANCEL);
}


/*----------------------------------------------------------------------*/
static void generateIf(Statement *stm)
{
	generateExpression(stm->fields.iff.exp);
	emit0(I_IF);
	generateStatements(stm->fields.iff.thn);
	if (stm->fields.iff.els != NULL) {
		emit0(I_ELSE);
		generateStatements(stm->fields.iff.els);
	}
	emit0(I_ENDIF);
}


/*----------------------------------------------------------------------*/
static void generateUse(Statement *stm)
{
	if (stm->fields.use.actorExp == NULL) { /* No actor specified, use current */
		emitConstant(stm->fields.use.script->code);
		emitVariable(V_CURRENT_INSTANCE);
		emit0(I_USE);
	} else {
		emitConstant(stm->fields.use.script->code);
		generateExpression(stm->fields.use.actorExp);
		emit0(I_USE);
	}
}


/*----------------------------------------------------------------------*/
static void generateStop(Statement *stm)
{
	generateExpression(stm->fields.stop.actor);
	emit0(I_STOP);
}


/*----------------------------------------------------------------------*/
static void generateDepend(Statement *stm)
{
	/* Generate DEPENDING statement.

	   Code generation principle:				Stack:

	   DEPEND

	   depend expression						   	d-exp

       <no DEPCASE for first case>
	   DUP---------------+					d-exp	d-exp
	   case1 expression  |			c-exp	d-exp	d-exp
	   case1 operator    |					case1?	d-exp
	   DEPEXEC           |							d-exp
	   stms1 ------------+

	   DEPCASE ----------+							d-exp
	   DUP               |					d-exp	d-exp
	   case2 expression  |			c-exp	d-exp	d-exp
	   case2 operator    |					case2?	d-exp
	   DEPEXEC           |							d-exp
	   stms1 ------------+

       <repeat for each case>

	   DEPELSE-----------+ optional
	   stmsn-------------+

	   ENDDEP

	   DEPCASE does nothing but must be there to indicate start of a
	   new level for skipping over statements.

	   Executing a DEPCASE or DEPELSE indicates the end of executing a
	   matching case so skip to the ENDDEP (on this level).

	   After the DEPCASE is a DUP to duplicate the depend value, note
	   that this must be done with DUPSTR if the type of the value is
	   a string. Then comes the case expression and then the operator
	   which does the compare.

	   DEPEXEC inspects the results on the stack top and if true
	   continues else skips to the instruction after next DEPCASE,
	   DEPELSE or to the ENDDEP.

	   ENDDEP just pops off the initially pushed depend expression.

	*/

	List *cases;

	emit0(I_DEPEND);
	generateExpression(stm->fields.depend.exp);
	/* For each case: */
	for (cases = stm->fields.depend.cases; cases != NULL; cases = cases->next) {
		/* If it is not the ELSE clause ... */
		if (cases->member.stm->fields.depcase.exp != NULL) {
			/* Generate a DEPCASE (if not first case) and a DUP */
			if (cases != stm->fields.depend.cases)
				emit0(I_DEPCASE);
            if (stm->fields.depend.exp->type == STRING_TYPE)
                emit0(I_DUPSTR);
            else
                emit0(I_DUP);
			/* ...and the case expression (right hand + operator) */
			generateFilter(cases->member.stm->fields.depcase.exp);
			emit0(I_DEPEXEC);
		} else
			emit0(I_DEPELSE);
		/* ...and then the statements */
		generateStatements(cases->member.stm->fields.depcase.stms);
	}
	emit0(I_ENDDEP);
}


/*----------------------------------------------------------------------*/
static void generateIntegerLoopLimit(Statement *statement) {
	List *filter;

	if (statement->fields.each.filters->member.exp->kind == BETWEEN_EXPRESSION)
		generateExpression(statement->fields.each.filters->member.exp->fields.btw.upperLimit);
	else
		ITERATE(filter, statement->fields.each.filters) {
			if (filter->member.exp->kind == WHERE_EXPRESSION)
				if (filter->member.exp->fields.whr.whr->kind == WHERE_INSET) {
					generateExpression(filter->member.exp->fields.whr.whr->what);
					statement->fields.each.setExpression = filter->member.exp;
					emit0(I_SETSIZE);
					return;
				}
		}
}


/*----------------------------------------------------------------------*/
static void generateIntegerLoopIndex(Expression *exp) {
	/* There are two instances when the loop index might have integer
	   type: when the loop is integer (using BETWEEN) or when looping
	   over an integer set. If we are looping over an integer set we
	   will use the integers from 1 to SETSIZE and convert them to
	   values using SETMEMBER */
	if (exp->kind == BETWEEN_EXPRESSION)
		generateExpression(exp->fields.btw.lowerLimit);
	else
		emitConstant(1);
}


/*----------------------------------------------------------------------*/
static void generateIntegerLoopValue(Expression *setExpression) {
	emit0(I_DUP);		/* Use the index as the loop value */
	if (setExpression != NULL) {
		generateExpression(setExpression->fields.whr.whr->what);
		emit0(I_SETMEMB);
	}
}


/*----------------------------------------------------------------------*/
static void generateEach(Statement *statement)
{
	List *filter;

	/* Loop variable is always local variable #1 in the frame */
	/* Generate a new FRAME */
	emit1(I_FRAME, 1);		/* One local variable in this block */
	frameLevel++;

	/* Push upper limit */
	if (statement->fields.each.type == INSTANCE_TYPE) {
		emitVariable(V_MAX_INSTANCE);
	} else if (statement->fields.each.type == INTEGER_TYPE) {
		generateIntegerLoopLimit(statement);
	} else
		SYSERR("Unexpected type", statement->srcp);

	/* Push start index */
	if (statement->fields.each.type == INTEGER_TYPE)
		generateIntegerLoopIndex(statement->fields.each.filters->member.exp);
	else				/* It's looping over instances */
		emitConstant(2);		/* Ignore #nowhere */

	/* Start loop */
	emit0(I_LOOP);
	generateSrcp(statement->srcp);

	/* Generate loop value from loop index */
	if (statement->fields.each.type == INTEGER_TYPE)
		generateIntegerLoopValue(statement->fields.each.setExpression);
	else
		emit0(I_DUP);

	/* Store the loop value in the local variable */
	emit2(I_SETLOCAL, 0, 1);	/* We already have the value on the stack */

	/* Generate filters */
	ITERATE(filter, statement->fields.each.filters) {
		emit2(I_GETLOCAL, 0, 1);
		generateFilter(filter->member.exp);
		emit0(I_NOT);
		emit0(I_IF);
		emit0(I_LOOPNEXT);
		emit0(I_ENDIF);
	}

	generateStatements(statement->fields.each.stms);

	/* End of loop */
	emit0(I_LOOPEND);

	/* End of block */
	emit0(I_ENDFRAME);
	frameLevel--;
}

/*----------------------------------------------------------------------*/
static void generateStrip(Statement *stm)
{
	/* Push First/Last indicator */
	emitConstant(stm->fields.strip.first);

	/* Push count, implicit = 1 */
	if (stm->fields.strip.count != NULL)
		generateExpression(stm->fields.strip.count);
	else
		emitConstant(1);

	/* Push words or character indicator */
	emitConstant(stm->fields.strip.wordOrChar);

	/* Push attribute reference */
	generateAttributeReference(stm->fields.strip.from);

	emit0(I_STRIP);		/* Will modify the FROM and leave rest on stack */

	/* If there was an INTO clause we set the string attribute */
	if (stm->fields.strip.into != NULL) {
		generateAttributeReference(stm->fields.strip.into);
		emit0(I_SETSTR);
	} else				/* Pop of the rest produced above */
		emit0(I_POP);
}


/*----------------------------------------------------------------------*/
static void generateSystem(Statement *stm)
{
	encode(&stm->fields.system.fpos, &stm->fields.system.len);
	emitConstant(stm->fields.system.len);
	emitConstant(stm->fields.system.fpos);
	emit0(I_SYSTEM);
}


/*----------------------------------------------------------------------*/
static void generateStyle(Statement *stm)
{
	emitConstant(stm->fields.style.style);
	emit0(I_STYLE);
}


/*----------------------------------------------------------------------*/
static void generateTranscript(Statement *stm)
{
	emitConstant(stm->fields.transcript.on_or_off);
	emit0(I_TRANSCRIPT);
}



/*----------------------------------------------------------------------*/
static void generateStatement(Statement *stm)
{
	if ((Bool)opts[OPTDEBUG].value)
		generateSrcp(stm->srcp);

	switch (stm->kind) {

	case NOP_STATEMENT:
		break;

	case PRINT_STATEMENT:
		generatePrint(stm);
		break;

	case STYLE_STATEMENT:
		generateStyle(stm);
		break;

	case QUIT_STATEMENT:
		emit0(I_QUIT);
		break;

	case LOOK_STATEMENT:
		emit0(I_LOOK);
		break;

	case SAVE_STATEMENT:
		emit0(I_SAVE);
		break;

	case RESTORE_STATEMENT:
		emit0(I_RESTORE);
		break;

	case RESTART_STATEMENT:
		emit0(I_RESTART);
		break;

	case VISITS_STATEMENT:
		emitConstant(stm->fields.visits.count);
		emit0(I_VISITS);
		break;

	case SCORE_STATEMENT:
		generateScore(stm);
		break;

	case DESCRIBE_STATEMENT:
		generateDescribe(stm);
		break;

	case SAY_STATEMENT:
		generateSay(stm);
		break;

	case LIST_STATEMENT:
		generateList(stm);
		break;

	case SHOW_STATEMENT:
		generateShow(stm);
		break;

	case PLAY_STATEMENT:
		generatePlay(stm);
		break;

	case EMPTY_STATEMENT:
		generateEmpty(stm);
		break;

	case LOCATE_STATEMENT:
		generateLocate(stm);
		break;

	case MAKE_STATEMENT:
		generateMake(stm);
		break;

	case SET_STATEMENT:
		generateSetStatement(stm);
		break;

	case INCREASE_STATEMENT:
	case DECREASE_STATEMENT:
		generateIncrease(stm);
		break;

	case INCLUDE_STATEMENT:
	case EXCLUDE_STATEMENT:
		generateIncludeAndExclude(stm);
		break;

	case SCHEDULE_STATEMENT:
		generateSchedule(stm);
		break;

	case CANCEL_STATEMENT:
		generateCancel(stm);
		break;

	case IF_STATEMENT:
		generateIf(stm);
		break;

	case USE_STATEMENT:
		generateUse(stm);
		break;

	case STOP_STATEMENT:
		generateStop(stm);
		break;

	case DEPEND_STATEMENT:
		generateDepend(stm);
		break;

	case SYSTEM_STATEMENT:
		generateSystem(stm);
		break;

	case EACH_STATEMENT:
		generateEach(stm);
		break;

	case STRIP_STATEMENT:
		generateStrip(stm);
		break;

	case TRANSCRIPT_STATEMENT:
		generateTranscript(stm);
		break;

	default:
		unimpl(stm->srcp, "Code Generator");
		break;
	}
}


/*======================================================================*/
void generateStatements(List *stms)
{
	List *current = stms;

	for (current = stms; current != NULL; current = current->next) {
		generateStatement(current->member.stm);
	}
}


/*----------------------------------------------------------------------*/
static void dumpStyle(int style) {
	switch (style) {
	case NORMAL_STYLE: put("Normal"); break;
	case EMPHASIZED_STYLE: put("Emphasized"); break;
	case PREFORMATTED_STYLE: put("Preformatted"); break;
	case ALERT_STYLE: put("Alert"); break;
	case QUOTE_STYLE: put("Quote"); break;
	}
}


/*----------------------------------------------------------------------*/
static void dumpForm(SayForm form)
{
	switch (form) {
	case SAY_SIMPLE: put("SIMPLE"); break;
	case SAY_DEFINITE: put("DEFINITE"); break;
	case SAY_INDEFINITE: put("INDEFINITE"); break;
	case SAY_NEGATIVE: put("NEGATIVE"); break;
	case SAY_PRONOUN: put("PRONOUN"); break;
	}
}


/*======================================================================*/
void dumpStatement(Statement *stm)
{
	if (stm == NULL) {
		put("NULL");
		return;
	}

	put("STM: ");
	switch(stm->kind) {
	case PRINT_STATEMENT: put("PRINT "); break;
	case DESCRIBE_STATEMENT: put("DESCRIBE "); break;
	case SAY_STATEMENT: put("SAY "); break;
	case LIST_STATEMENT: put("LIST "); break;
	case IF_STATEMENT: put("IF "); break;
	case MAKE_STATEMENT: put("MAKE "); break;
	case SET_STATEMENT: put("SET "); break;
	case INCREASE_STATEMENT: put("INCR "); break;
	case DECREASE_STATEMENT: put("DECR "); break;
	case LOCATE_STATEMENT: put("LOCATE "); break;
	case EMPTY_STATEMENT: put("EMPTY "); break;
	case INCLUDE_STATEMENT: put("INCLUDE "); break;
	case EXCLUDE_STATEMENT: put("REMOVE "); break;
	case SCHEDULE_STATEMENT: put("SCHEDULE "); break;
	case CANCEL_STATEMENT: put("CANCEL "); break;
	case LOOK_STATEMENT: put("LOOK "); break;
	case QUIT_STATEMENT: put("QUIT "); break;
	case SCORE_STATEMENT: put("SCORE "); break;
	case USE_STATEMENT: put("USE "); break;
	case STRIP_STATEMENT: put("STRIP "); break;
	case STOP_STATEMENT: put("STOP "); break;
	case SAVE_STATEMENT: put("SAVE "); break;
	case RESTORE_STATEMENT: put("RESTORE "); break;
	case RESTART_STATEMENT: put("RESTART "); break;
	case VISITS_STATEMENT: put("VISITS "); break;
	case NOP_STATEMENT: put("NOP "); break;
	case SHOW_STATEMENT: put("SHOW "); break;
	case PLAY_STATEMENT: put("PLAY "); break;
	case SYSTEM_STATEMENT: put("SYSTEM "); break;
	case DEPEND_STATEMENT: put("DEPEND "); break;
	case DEPENDCASE_STATEMENT: put("DEPENDCASE "); break;
	case EACH_STATEMENT: put("EACH "); break;
	case STYLE_STATEMENT: put("STYLE "); break;
	case TRANSCRIPT_STATEMENT: put("TRANSCRIPT "); break;
	}
	dumpSrcp(stm->srcp);

	switch(stm->kind) {
	case LOOK_STATEMENT:
	case NOP_STATEMENT:
	case QUIT_STATEMENT:
	case SAVE_STATEMENT:
	case RESTORE_STATEMENT:
	case RESTART_STATEMENT:
		break;
	default:
		indent();
		switch(stm->kind) {
		case PRINT_STATEMENT:
			put("fpos: "); dumpInt(stm->fields.print.fpos); nl();
			put("len: "); dumpInt(stm->fields.print.len);
			break;
		case SCORE_STATEMENT:
			put("count: "); dumpInt(stm->fields.score.count); nl();
			put("score: "); dumpInt(stm->fields.score.score);
			break;
		case DESCRIBE_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.describe.what);
			break;
		case SAY_STATEMENT:
			put("exp: "); dumpExpression(stm->fields.say.exp); nl();
			put("form: "); dumpForm(stm->fields.say.form);
			break;
		case LIST_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.list.wht);
			break;
		case EMPTY_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.empty.what); nl();
			put("whr: "); dumpWhere(stm->fields.empty.where);
			break;
		case LOCATE_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.locate.what); nl();
			put("whr: "); dumpWhere(stm->fields.locate.where);
			break;
		case INCLUDE_STATEMENT:
		case EXCLUDE_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.include.what); nl();
			put("set: "); dumpExpression(stm->fields.include.set);
			break;
		case MAKE_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.make.wht); nl();
			put("not: "); dumpBool(stm->fields.make.not); nl();
			put("atr: "); dumpId(stm->fields.make.atr);
			break;
		case SET_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.set.wht); nl();
			put("exp: "); dumpExpression(stm->fields.set.exp);
			break;
		case INCREASE_STATEMENT:
		case DECREASE_STATEMENT:
			put("wht: "); dumpExpression(stm->fields.incr.wht); nl();
			put("step: "); dumpExpression(stm->fields.incr.step);
			break;
		case SCHEDULE_STATEMENT:
			put("what: "); dumpExpression(stm->fields.schedule.what); nl();
			put("whr: "); dumpWhere(stm->fields.schedule.whr); nl();
			put("when: "); dumpExpression(stm->fields.schedule.when);
			break;
		case CANCEL_STATEMENT:
			put("id: "); dumpExpression(stm->fields.cancel.what);
			break;
		case IF_STATEMENT:
			put("exp: "); dumpExpression(stm->fields.iff.exp); nl();
			put("thn: "); dumpList(stm->fields.iff.thn, STATEMENT_LIST); nl();
			put("els: "); dumpList(stm->fields.iff.els, STATEMENT_LIST);
			break;
		case USE_STATEMENT:
			put("script: "); dumpId(stm->fields.use.script); nl();
			put("actor: "); dumpExpression(stm->fields.use.actorExp);
			break;
		case STOP_STATEMENT:
			put("actor: "); dumpExpression(stm->fields.stop.actor);
			break;
		case EACH_STATEMENT:
			put("loopId: "); dumpId(stm->fields.each.loopId); nl();
			put("type: "); dumpType(stm->fields.each.type); nl();
			put("filters: "); dumpList(stm->fields.each.filters, EXPRESSION_LIST); nl();
			put("stms: "); dumpList(stm->fields.each.stms, STATEMENT_LIST);
			break;
		case VISITS_STATEMENT:
			put("count: "); dumpInt(stm->fields.visits.count);
			break;
		case STRIP_STATEMENT:
			put("first: "); dumpBool(stm->fields.strip.first); nl();
			put("count: "); dumpExpression(stm->fields.strip.count); nl();
			put("word?: "); dumpBool(stm->fields.strip.wordOrChar); nl();
			put("from: "); dumpExpression(stm->fields.strip.from); nl();
			put("into: "); dumpExpression(stm->fields.strip.into);
			break;
		case STYLE_STATEMENT:
			put("style: "); dumpStyle(stm->fields.style.style);
			break;
		case PLAY_STATEMENT:
		case SHOW_STATEMENT:
			put("resource: "); dumpResource(stm->fields.play.resource);
			break;
		case NOP_STATEMENT:
		case QUIT_STATEMENT:
		case LOOK_STATEMENT:
		case SAVE_STATEMENT:
		case RESTORE_STATEMENT:
		case RESTART_STATEMENT:
		case SYSTEM_STATEMENT:
			break;
		case DEPEND_STATEMENT:
		case DEPENDCASE_STATEMENT:
			break;
		case TRANSCRIPT_STATEMENT:
			put("on_or_off: "); dumpBool(stm->fields.transcript.on_or_off);
			break;
		}
		out();
	}
}
