/*----------------------------------------------------------------------*\

                               STX.C
                            Syntax Nodes

\*----------------------------------------------------------------------*/

#include "stx_x.h"


#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "id_x.h"
#include "adv_x.h"
#include "sym_x.h"
#include "res_x.h"
#include "elm_x.h"
#include "lst_x.h"
#include "wrd_x.h"
#include "context_x.h"
#include "cla_x.h"
#include "dump_x.h"

#include "lmList.h"

#include "acode.h"
#include "emit.h"
#include "opt.h"


/* PUBLIC: */


/*======================================================================*/
Syntax *newSyntax(Srcp srcp, Id *id, List *elements, List *restrictionList,
		  Srcp restrictionSrcp)
{
	Syntax *new;                  /* The newly created node */
	static int number = 1;

	progressCounter();

	new = NEW(Syntax);

	new->srcp = srcp;
	new->id = id;
	new->number = number++;
	new->elements = elements;
	new->firstSyntax = TRUE;	/* Assume first and only so far */
	new->nextSyntaxForSameVerb = NULL;
	new->restrictions = restrictionList;
	new->restrictionSrcp = restrictionSrcp;

	new->generated = FALSE;

	return(new);
}


/*======================================================================*/
Syntax *newSyntaxWithEOS(Srcp srcp, Id *id, List *restrictionList,
						 Srcp restrictionSrcp)
{
	Syntax *this;                  /* The newly created node */
	static int number = 1;

	progressCounter();

	this = NEW(Syntax);

	this->srcp = srcp;
	this->id = id;
	this->number = number++;
	this->elements = newList(newEndOfSyntax(), ELEMENT_LIST);
	this->elements->member.elm->stx = this;
	this->firstSyntax = TRUE;	/* Assume first and only so far */
	this->nextSyntaxForSameVerb = NULL;
	this->restrictions = restrictionList;
	this->restrictionSrcp = restrictionSrcp;

	this->generated = FALSE;

	return(this);
}


/*======================================================================*/
void addElement(Syntax *syntax, Element *element)
{
	insert(getListNode(syntax->elements, length(syntax->elements)), element, ELEMENT_LIST);
}



/*----------------------------------------------------------------------*/
static void setDefaultRestriction(List *parameters)
{
	/*
      Sets the restrictions class symbol to the objectSymbol for the
      restrictions that has no explicit declared.
	*/

	List *p;

	if (parameters != NULL && parameters->kind != SYMBOL_LIST)
		SYSERR("Not a symbol list", nulsrcp);

	ITERATE(p, parameters)
		if (p->member.sym->fields.parameter.element->res == NULL
			|| p->member.sym->fields.parameter.element->res->classId == NULL) {
			p->member.sym->fields.parameter.class = objectSymbol;
			p->member.sym->fields.parameter.type = INSTANCE_TYPE;
		}
}



/*======================================================================*/
Bool equalParameterLists(Syntax *stx1, Syntax *stx2)
{
	/*
	  Compare two syntax nodes and return true if their parameter lists
	  are equal (same ordering with same parameter names).
	*/

	List *elm1, *elm2;
	for (elm1 = stx1->parameters, elm2 = stx2->parameters;
		 elm1 != NULL && elm2 != NULL;
		 elm1 = elm1->next, elm2 = elm2->next) {
		if (!equalId(elm1->member.elm->id, elm2->member.elm->id))
			return FALSE;
		if (elm1->member.elm->flags != elm2->member.elm->flags)
			return FALSE;
	}
	return elm1 == elm2;          /* Both NULL => equal */
}


/*----------------------------------------------------------------------*/
static int countParameters(List *elms)
{
	/*
	  Count number of PARAMETER_ELEMENTs in a ELEMENT_LIST
	*/
	List *lst;
	int count = 0;

	ITERATE(lst, elms) {
		if (lst->member.elm->kind == PARAMETER_ELEMENT)
			count++;
	}
	return count;
}


/*----------------------------------------------------------------------*/
static Bool compatibleParameterLists(Syntax *stx1, Syntax *stx2)
{
	/*
	  Compare two syntax nodes and return true if their parameter lists
	  are compatible (same ordering with same parameter names).
	*/

	List *elm1, *elm2;
	int foundInOther = 0;
	Bool found;

	ITERATE(elm1, stx1->elements) {
		if (elm1->member.elm->kind == PARAMETER_ELEMENT) {
			found = FALSE;
			ITERATE(elm2, stx2->elements) {
				if (elm2->member.elm->kind == PARAMETER_ELEMENT)
					if (equalId(elm1->member.elm->id, elm2->member.elm->id)){
						found = TRUE;
						break;
					}
			}
			if (!found) return FALSE;
			else foundInOther++;
		}
	}
	return foundInOther == countParameters(stx2->elements);
}


/*----------------------------------------------------------------------*/
static void setInitialParameterClass(Symbol* verbSymbol, Syntax *syntax) {
	List *parameters;

	ITERATE(parameters, verbSymbol->fields.verb.parameterSymbols) {
		Symbol *parameterSymbol = parameters->member.sym;
		parameterSymbol->fields.parameter.type = INSTANCE_TYPE;
#ifdef RESTRICT_TO_OBJECT_BEFORE_RESTRICTION
		if (hasRestriction(parameterSymbol, syntax))
			/* Set initial type of parameter to entity */
			parameterSymbol->fields.parameter.class = entitySymbol;
		else
			/* Set type of parameter to object */
			parameterSymbol->fields.parameter.class = objectSymbol;
#else
		parameterSymbol->fields.parameter.class = entitySymbol;
#endif
	}
}


/*----------------------------------------------------------------------*/
static ElementKind firstElementKind(Syntax *stx) {
	return stx->elements->member.elm->kind;
}


/*----------------------------------------------------------------------*/
static void analyzeSyntax(Syntax *stx)
{
	Symbol *verbSymbol;

	progressCounter();

	/* Find which verb it defines */
	verbSymbol = lookup(stx->id->string);
	if (verbSymbol == NULL) {
		if (!isGeneratedId(stx->id))
			lmLog(&stx->id->srcp, 207, sevERR, stx->id->string);
	} else if (verbSymbol->kind != VERB_SYMBOL)
		lmLog(&stx->id->srcp, 208, sevERR, stx->id->string);
	else {
		stx->id->symbol = verbSymbol;
		stx->id->code = verbSymbol->code;

		stx->parameters = analyzeElements(stx->elements, stx->restrictions, stx);
		/* Register the parameters, use last syntax if multiple */
		if (stx->firstSyntax) {
			setParameters(verbSymbol, stx->parameters);

			symbolizeRestrictions(stx->restrictions, verbSymbol);
			setInitialParameterClass(verbSymbol, stx);
			analyzeRestrictions(stx->restrictions, verbSymbol);
			setDefaultRestriction(verbSymbol->fields.verb.parameterSymbols);
		}

		if (firstElementKind(stx) == PARAMETER_ELEMENT)
			/* A syntax starting with a parameter reference, all of
			   which we need to collect separately since there is no
			   word to lookup to get them */
			adv.stxsStartingWithInstanceReference = concat(adv.stxsStartingWithInstanceReference, stx, SYNTAX_LIST);

		/* Link last syntax element to this stx to prepare for code generation */
		(getLastListNode(stx->elements))->member.elm->stx = stx;
	}
}

/*----------------------------------------------------------------------*/
static void connectSyntaxesForSameVerb(List *syntaxes) {
	List *lst, *other;
	Bool error;

	ITERATE(lst, syntaxes) {
		error = FALSE;
		for (other = lst->next; other != NULL; other = other->next) {
			if (equalId(other->member.stx->id, lst->member.stx->id)) {
				lst->member.stx->nextSyntaxForSameVerb = other->member.stx;
				other->member.stx->firstSyntax = FALSE;
				if (!compatibleParameterLists(lst->member.stx, other->member.stx)) {
					lmLog(&other->member.stx->id->srcp, 206, sevERR,
						  lst->member.stx->id->string);
					error = TRUE;
				}
				if (other->member.stx->restrictions != NULL) {
					lmLog(&other->member.stx->restrictionSrcp, 250, sevERR,
						  lst->member.stx->id->string);
					error = TRUE;
				}
				break;			/* We only need to find one this time around,
								   others will be found in next traversal */
			}
		}
		if (error) {
			char insertString[1000];
			sprintf(insertString, "syntax for '%s'", lst->member.stx->id->string);
			lmLog(&lst->member.stx->id->srcp, 205, sevWAR, insertString);
		}
	}
}


/*======================================================================*/
void analyzeSyntaxes(void)
{
  List *lst;

  /* Check and connect definitions for multiple syntax for a verb */
  connectSyntaxesForSameVerb(adv.stxs);
  /* Now do the analysis */
  for (lst = adv.stxs; lst != NULL; lst = lst->next)
    analyzeSyntax(lst->member.stx);
}


/*======================================================================*/
Syntax *defaultSyntax0(char *verbName)
{
    /*
      Returns the address a default syntax node which is used for verbs
      without any defined syntax (global verbs, without a parameter):

      Syntax x = x.
    */

    Syntax *stx;
    List *elements;

    elements = concat(newList(newWordElement(nulsrcp, newId(nulsrcp,
                                                            verbName)),
                              ELEMENT_LIST),
                      newEndOfSyntax(), ELEMENT_LIST);
    stx = newSyntax(nulsrcp, newId(nulsrcp, verbName), elements, NULL, nulsrcp);

    adv.stxs = concat(adv.stxs, stx, SYNTAX_LIST);
    analyzeSyntax(stx);                   /* Make sure the syntax is analysed */
    return stx;
}


/*======================================================================*/
Syntax *defaultSyntax1(Id *verb, Context *context)
{
	/*
	  Returns the address a default syntax node which is used for verbs
	  in instances (taking one parameter) without any defined syntax:

	  Syntax x = x (<id>).

	  Where <id> is the class identifier of the class or inherited class
	  in which the declaration occured.
	*/

	Syntax *stx;
	List *elements;
	Id *classId;
	Restriction *res;

	classId = classIdInContext(context);
	if (classId == NULL)		/* No class, so use any, should not occur? */
		classId = newId(nulsrcp, "object");

	elements = concat(concat(newList(newWordElement(nulsrcp,
                                                    newId(nulsrcp, verb->string)),
                                     ELEMENT_LIST),
							 newParameterElement(nulsrcp, newId(nulsrcp,
																classId->string),
												 0),
							 ELEMENT_LIST),
					  newEndOfSyntax(),
					  ELEMENT_LIST);
	stx = newSyntax(nulsrcp, newId(nulsrcp, verb->string), elements, NULL, nulsrcp);

	adv.stxs = concat(adv.stxs, stx, SYNTAX_LIST);

	/* Add restriction for the parameter class in context */
	res = newRestriction(nulsrcp,
						 newId(nulsrcp, classId->string),
						 ID_RESTRICTION,
						 newId(nulsrcp, classId->string),
						 NULL);
	res->autoGenerated = TRUE;

	stx->restrictions = newList(res, RESTRICTION_LIST);

	if (strcmp(classId->string, "location") == 0)
		/* Default syntaxes restricting to locations might be surprising... */
		lmLog(&verb->srcp, 232, sevWAR, "");

	analyzeSyntax(stx);                   /* Make sure the syntax is analysed */
	return stx;
}



/*----------------------------------------------------------------------*/
static void generateParseTree(Syntax *stx)
{
	Word *wrd;
	List *lst = NULL;
	List *elements = NULL;            /* A list of parallell elms-lists */

	progressCounter();

	if (!stx->generated) {
		if (firstElementKind(stx) == WORD_ELEMENT) {
			/* First word is a verb which points to all stxs starting with that word */
			wrd = findWord(stx->elements->member.elm->id->string);
			/* Ignore words that are not verbs and prepositions */
			if (wrd->classbits&(1L<<PREPOSITION_WORD))
				lst = wrd->ref[PREPOSITION_WORD];
			if (wrd->classbits&(1L<<VERB_WORD))
				lst = wrd->ref[VERB_WORD];
		} else /* A parameter */
			/* Use the list of all syntaxes that starts with an instance reference */
			lst = adv.stxsStartingWithInstanceReference;
		/* Create a list of all parallell syntax elements */
		while (lst) {
			elements = concat(elements, lst->member.stx->elements, LIST_LIST);
			lst->member.stx->generated = TRUE;
			lst = lst->next;
		}
		stx->elementsAddress = generateElements(elements, stx);
		stx->generated = TRUE;
	} else
		stx->elementsAddress = 0;
}



/*----------------------------------------------------------------------*/
static void generateParseEntry(Syntax *stx)
{
	SyntaxEntry entry;

	if (stx->elementsAddress != 0) {
		if (firstElementKind(stx) == WORD_ELEMENT)
			/* The code for the verb word */
			entry.code = stx->elements->member.elm->id->code;
		else
			/* Starting with a parameter */
			entry.code = 0;
		/* Address to syntax element tables */
		entry.elms = stx->elementsAddress;
		entry.parameterNameTable = stx->parameterNameTable;
		emitEntry(&entry, sizeof(entry));
	}
}


/*----------------------------------------------------------------------*/
static void generateRestrictionTable(void) {
	List *lst;

	/* Generate all syntax parameter restriction checks */
	ITERATE(lst, adv.stxs) {
		Syntax *stx = lst->member.stx;
		Syntax *nextSyntax;
		if (stx->firstSyntax) {
			stx->restrictionsAddress = generateRestrictions(stx->restrictions, stx);
			for (nextSyntax = stx->nextSyntaxForSameVerb; nextSyntax;
				 nextSyntax = nextSyntax->nextSyntaxForSameVerb)
				nextSyntax->restrictionsAddress = stx->restrictionsAddress;
		}
	}
}


/*----------------------------------------------------------------------*/
Aaddr generateParameterNamesForOneSyntax(Syntax *syntax) {
    List *lst;
    Aaddr adr;

    /* Generate all parameter names */
    ITERATE(lst, syntax->parameters) {
        Element *elm = lst->member.elm;
        elm->idAddress = nextEmitAddress();
        emitString(elm->id->string);
    }
    emit(EOF);

    adr = nextEmitAddress();
    /* Generate a list of addresses */
    ITERATE(lst, syntax->parameters) {
        Element *elm = lst->member.elm;
        emit(elm->idAddress);
    }
    emit(EOF);
    return adr;
}


/*======================================================================*/
Aaddr generateParameterNames(List *stxs) {
    List *stx;
    Aaddr adr;

    ITERATE(stx, stxs)
        stx->member.stx->parameterNameTable = generateParameterNamesForOneSyntax(stx->member.stx);

    adr = nextEmitAddress();
    ITERATE(stx, stxs)
        emit(stx->member.stx->parameterNameTable);
    emit(EOF);

    emit(adr);
    return adr;
}


/*======================================================================*/
Aaddr generateParseTable(void) {
    List *lst;
    Aaddr parseTableAddress;

    generateRestrictionTable();

    ITERATE(lst, adv.stxs)
        generateParseTree(lst->member.stx);

    parseTableAddress = nextEmitAddress();
    ITERATE(lst, adv.stxs)
        generateParseEntry(lst->member.stx);
    emit(EOF);

    return(parseTableAddress);
}


/*----------------------------------------------------------------------*/
static void generateParameterMapping(Syntax *syntax)
{
	List *list;
	Aaddr parameterMappingTableAddress = nextEmitAddress();
	List *originalParameters = syntax->id->symbol->fields.verb.parameterSymbols;
	List *originalPosition;
	Bool found = FALSE;

	ITERATE(list, syntax->parameters) {
		/* Generate a parameter mapping entry */
		ITERATE(originalPosition, originalParameters) {
			/* Find its original position */
			if (strcmp(list->member.elm->id->string, originalPosition->member.sym->string) == 0) {
				generateSymbol(originalPosition->member.sym);
				found = TRUE;
				break;
			}
		}
		if (!found) SYSERR("Could not find parameter", nulsrcp);
	}
	emit(EOF);
	syntax->parameterMappingAddress = parameterMappingTableAddress;
}


/*======================================================================*/
Aaddr generateParameterMappingTable(void)
{
	List *list;
	Aaddr parameterMappingTableAddress;
	ParameterMapEntry entry;

	ITERATE(list, adv.stxs)
		generateParameterMapping(list->member.stx);

	parameterMappingTableAddress = nextEmitAddress();
	ITERATE(list, adv.stxs) {
		entry.syntaxNumber = list->member.stx->number;
		entry.parameterMapping = list->member.stx->parameterMappingAddress;
		entry.verbCode = list->member.stx->id->code;
		emitEntry(&entry, sizeof(entry));
	}
	emit(EOF);

	return parameterMappingTableAddress;
}


/*======================================================================*/
void dumpSyntax(Syntax *stx)
{
	if (stx == NULL) {
		put("NULL");
		return;
	}

	put("STX: "); dumpPointer(stx); dumpSrcp(stx->srcp); indent();
	put("verbId: "); dumpId(stx->id); nl();
	put("number: "); dumpInt(stx->number); nl();
	put("generated: "); dumpBool(stx->generated); nl();
	put("elmsadr: "); dumpAddress(stx->elementsAddress); nl();
	put("elements: "); dumpList(stx->elements, ELEMENT_LIST); nl();
	put("resadr: "); dumpAddress(stx->restrictionsAddress); nl();
	put("restrictionList: "); dumpList(stx->restrictions, RESTRICTION_LIST); nl();
	put("parameters: "); dumpList(stx->parameters, ELEMENT_LIST); out();
}
