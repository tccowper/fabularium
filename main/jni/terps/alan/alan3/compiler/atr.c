/*----------------------------------------------------------------------*\

  ATR.C
  Attribute Nodes

  \*----------------------------------------------------------------------*/

#include "atr_x.h"


/* IMPORT: */
#include "util.h"
#include "emit.h"

#include "ins.h"
#include "opt.h"

#include "acode.h"

#include "lmList.h"
#include "encode.h"

/* USE: */
#include "srcp_x.h"
#include "id_x.h"
#include "adv_x.h"
#include "cla_x.h"
#include "ins_x.h"
#include "sym_x.h"
#include "lst_x.h"
#include "exp_x.h"
#include "sym_x.h"
#include "dump_x.h"
#include "type_x.h"
#include "context_x.h"

/* Exported data: */

int attributeAreaSize = 0;	/* # of Awords needed for attribute storage */


/*----------------------------------------------------------------------*/
static Attribute *newAttribute(Srcp *srcp,
                               TypeKind type,
                               Id *id,
                               int value,
                               long int fpos,
                               int len,
                               Id *reference,
                               Expression *set)
{
    Attribute *new;			/* The newly allocated area */

    progressCounter();

    new = NEW(Attribute);

    new->srcp = *srcp;
    new->type = type;
    new->id = id;
    new->inheritance = UNKNOWN_INHERITANCE;
    new->value = value;
    new->stringAddress = 0;
    new->encoded = FALSE;
    new->fpos = fpos;
    new->len = len;
    new->reference = reference;
    new->initialized = FALSE;
    new->set = set;

    return(new);
}


/*======================================================================*/
Attribute *newBooleanAttribute(Srcp srcp, Id *id, Bool value)
{
    Attribute *new;			/* The newly allocated area */

    new = newAttribute(&srcp, BOOLEAN_TYPE, id, value, 0, 0, NULL, NULL);

    return(new);
}


/*======================================================================*/
Attribute *newStringAttribute(Srcp srcp, Id *id, long fpos, int len)
{
    Attribute *new;			/* The newly allocated area */

    new = newAttribute(&srcp, STRING_TYPE, id, 0, fpos, len, NULL, NULL);

    return(new);
}


/*======================================================================*/
Attribute *newIntegerAttribute(Srcp srcp, Id *id, int value)
{
    Attribute *new;			/* The newly allocated area */

    new = newAttribute(&srcp, INTEGER_TYPE, id, value, 0, 0, NULL, NULL);

    return(new);
}


/*======================================================================*/
Attribute *newReferenceAttribute(Srcp srcp, Id *id, Id *instance)
{
    Attribute *new;			/* The newly allocated area */

    new = newAttribute(&srcp, REFERENCE_TYPE, id, 0, 0, 0, instance, NULL);

    return(new);
}


/*======================================================================*/
Attribute *newSetAttribute(Srcp srcp, Id *id, Expression *set)
{
    Attribute *new;			/* The newly allocated area */

    new = newAttribute(&srcp, SET_TYPE, id, 0, 0, 0, NULL, set);

    return(new);
}


/*----------------------------------------------------------------------*/
static void checkMultipleAttributes(List *atrs)
{
    List *al1;
    List *al2;

    ITERATE(al1, atrs) {
        Attribute *thisAttribute = al1->member.atr;
        /* Check multiple declaration */
        ITERATE(al2, al1->next) {
            Attribute *nextAttribute = al2->member.atr;
            if (equalId(thisAttribute->id, nextAttribute->id))
                lmLog(&nextAttribute->id->srcp, 218, sevERR, nextAttribute->id->string);
        }
    }
}


/*======================================================================*/
void symbolizeAttributes(List *atrs, Bool inClassDeclaration)
{
    List *al;

    checkMultipleAttributes(atrs);

    ITERATE(al, atrs) {
        Attribute *thisAttribute = al->member.atr;
        if (thisAttribute->type == REFERENCE_TYPE) {
            symbolizeId(thisAttribute->reference);
            if (thisAttribute->reference->symbol) {
                thisAttribute->initialized = TRUE;
                if (isInstance(thisAttribute->reference->symbol))
                    thisAttribute->type = INSTANCE_TYPE;
                else if (thisAttribute->reference->symbol->kind == EVENT_SYMBOL)
                    thisAttribute->type = EVENT_TYPE;
                else if (isClass(thisAttribute->reference->symbol)
                         && inClassDeclaration)
                    thisAttribute->initialized = FALSE;
                else {
                    if (thisAttribute->reference->symbol->kind != ERROR_SYMBOL)
                        lmLogv(&thisAttribute->reference->srcp, 428, sevERR,
                               "Attribute value in reference attribute declaration",
                               "an instance or event", NULL);
                    thisAttribute->type = ERROR_TYPE;
                }
            } else
                thisAttribute->type = ERROR_TYPE;
        }
    }
}


static Id id = {{0,0,0}, "location", NULL, -1};
static Attribute locationAttributeFake = {{0,0,0}, INSTANCE_TYPE, &id, TRUE};
/*======================================================================*/
Attribute *findAttribute(List *attributes, Id *id)
{
    List *this;

    if (strcmp(id->string, "location") == 0) {
        locationAttributeFake.referenceClass = locationSymbol;
        return &locationAttributeFake;
    }

    ITERATE(this, attributes)
        if (equalId(this->member.atr->id, id))
            return this->member.atr;
    return NULL;
}


/*======================================================================*/
List *sortAttributes(List *attributes)
{
    List *sortedList = attributes;
    Bool change;			/* Change during sorting */
    List **lstp;			/* Pointer to a list pointer */
    List *tmp1, *tmp2;		/* Temporary pointers */

    if (attributes != NULL) {
        change = TRUE;
        while (change) {
            change = FALSE;
            for (lstp = &sortedList; (*lstp)->next != NULL; lstp = &(*lstp)->next) {
                tmp1 = *lstp;
                tmp2 = tmp1->next;
#ifdef SYSERR_MULTIPLE_ATTRIBUTES_WITH_SAME_CODE
                /* This is just a precaution, it may occur if you have
                   multiple declarations of the same inherited attribute */
                if (tmp1->element.atr->id->code != 0 &&
                    tmp1->element.atr->id->code == tmp2->element.atr->id->code)
                    syserr("Sorting multiple attributes with same code.", NULL);
#endif
                if (tmp1->member.atr->id->code > tmp2->member.atr->id->code) {
                    change = TRUE;
                    tmp1->next = tmp2->next;
                    tmp2->next = tmp1;
                    *lstp = tmp2;
                }
            }
        }
    }
    return sortedList;
}



/*----------------------------------------------------------------------*/
static Attribute *copyAttribute(Attribute *theOriginal)
{
    Attribute *theCopy = NEW(Attribute);

    memcpy(theCopy, theOriginal, sizeof(Attribute));
    theCopy->inheritance = INHERITED;
    return theCopy;
}


/*----------------------------------------------------------------------*/
static List *copyAttributeList(List *theOriginal)
{
    List *theCopy = NULL;
    List *traversal;

    for (traversal = theOriginal; traversal != NULL; traversal = traversal->next)
        theCopy = concat(theCopy, copyAttribute(traversal->member.atr),
                         ATTRIBUTE_LIST);
    return theCopy;
}



/*======================================================================*/
List *combineAttributes(List *ownAttributes, List *attributesToAdd)
{
    /* Insert all attributes from the list that are not there
       already, then sort the list.

       NOTE! that we use the codes to combine, so this can't be used
       before attributes have been numbered!!
    */

    List *own = ownAttributes;
    List *toAdd = attributesToAdd;
    List *new;

    while (own != NULL) {
        if (toAdd == NULL)
            break;
        else if (own->member.atr->id->code == toAdd->member.atr->id->code) {
            own = own->next;
            toAdd = toAdd->next;
        } else if (own->member.atr->id->code < toAdd->member.atr->id->code) {
            own = own->next;
        } else if (own->member.atr->id->code > toAdd->member.atr->id->code) {
            insert(own, copyAttribute(toAdd->member.atr), ATTRIBUTE_LIST);
            toAdd = toAdd->next;
        }
    }
    if (toAdd != NULL)
        new = combine(ownAttributes, copyAttributeList(toAdd));
    else
        new = ownAttributes;

    return sortAttributes(new);
}

/*----------------------------------------------------------------------*/
static Bool isWhatId(Expression *exp)
{
    return exp->kind == WHAT_EXPRESSION
        && exp->fields.wht.wht->kind == WHAT_ID;
}

/*----------------------------------------------------------------------*/
static Bool hasSingleIdentifierMember(List *members)
{
    if (members == NULL) return FALSE;
    return length(members) == 1
        && members->kind == EXPRESSION_LIST
        && isWhatId(members->member.exp);
}

/*----------------------------------------------------------------------*/
static char *theSingleIdentifier(List *members)
{
    return members->member.exp->fields.wht.wht->id->string;
}

/*----------------------------------------------------------------------*/
static void analyzeSetAttribute(Attribute *thisAttribute, Context *context)
{
    List *members = thisAttribute->set->fields.set.members;

    if (hasSingleIdentifierMember(members)) {
        Symbol *symbol = lookup(theSingleIdentifier(members));
        if (symbol != NULL) {
            if (isClass(symbol)) {
                thisAttribute->set->fields.set.memberClass = symbol;
                thisAttribute->setClass = thisAttribute->set->fields.set.memberClass;
                thisAttribute->set->fields.set.memberType = classToType(symbol);
                thisAttribute->setType = thisAttribute->set->fields.set.memberType;
                thisAttribute->set->fields.set.members = NULL;
                return;
            }
        }
    }
    if (length(members) > 0) {
        analyzeExpression(thisAttribute->set, context);
        if (!isConstantExpression(thisAttribute->set))
            lmLog(&thisAttribute->set->srcp, 433, sevERR, "");
        thisAttribute->setType = thisAttribute->set->fields.set.memberType;
        if (thisAttribute->setType == ERROR_TYPE)
            thisAttribute->type = ERROR_TYPE;
        else
            thisAttribute->type = SET_TYPE;
        thisAttribute->setClass = thisAttribute->set->fields.set.memberClass;
    }
}

/*----------------------------------------------------------------------*/
static void analyzeInheritedSetAttribute(Attribute *thisAttribute,
                                         Attribute *inheritedAttribute,
                                         Symbol *definingSymbol) {

    if (thisAttribute->setClass != NULL &&
        !inheritsFrom(thisAttribute->setClass, inheritedAttribute->setClass)) {
        lmLogv(&thisAttribute->srcp, 329, sevERR, definingSymbol->string,
               "of its members",
               thisAttribute->setClass->string,
               inheritedAttribute->setClass->string, NULL);
        thisAttribute->type = ERROR_TYPE;
    } else
        /* Set this member class to the inherited one since it defines it */
        thisAttribute->setClass = inheritedAttribute->setClass;
}


/*----------------------------------------------------------------------*/
static void analyzeReferenceAttribute(Attribute *thisAttribute) {
    /* Set initial value and referenceClass */
    if (thisAttribute->reference->symbol != NULL) {
        thisAttribute->value = thisAttribute->reference->symbol->code;
        if (thisAttribute->type == REFERENCE_TYPE)
            thisAttribute->referenceClass = thisAttribute->reference->symbol;
        else
            thisAttribute->referenceClass = thisAttribute->reference->symbol->fields.entity.parent;
    }
}


/*----------------------------------------------------------------------*/
static void analyzeEventAttribute(Attribute *thisAttribute) {
    /* Set initial value */
    if (thisAttribute->reference->symbol != NULL) {
        thisAttribute->value = thisAttribute->reference->symbol->code;
    }
}


/*----------------------------------------------------------------------*/
static void analyzeInheritedReferenceAttribute(Attribute *thisAttribute,
                                               Attribute *inheritedAttribute,
                                               Symbol *definingSymbol) {

    if (!inheritsFrom(thisAttribute->reference->symbol, inheritedAttribute->referenceClass)) {
        if (thisAttribute->referenceClass != NULL)
            lmLogv(&thisAttribute->srcp, 329, sevERR, definingSymbol->string,
                   "of the instance that it refers to",
                   thisAttribute->referenceClass->string,
                   inheritedAttribute->reference->symbol->string, NULL);
        thisAttribute->type = ERROR_TYPE;
    } else
        /* Set the class to the inherited one */
        thisAttribute->referenceClass = inheritedAttribute->referenceClass;
}


/*======================================================================*/
void analyzeAttributes(List *atrs, Symbol *owningSymbol, Context *context)
{
    List *theList;

    ITERATE (theList, atrs) {
        Attribute *thisAttribute = theList->member.atr;
        Attribute *inheritedAttribute = findInheritedAttribute(owningSymbol, thisAttribute->id);

        thisAttribute->definingSymbol = owningSymbol;
        switch (thisAttribute->type) {
        case SET_TYPE:
            analyzeSetAttribute(thisAttribute, context);
            break;
        case INSTANCE_TYPE:
        case REFERENCE_TYPE:
            analyzeReferenceAttribute(thisAttribute);
            break;
        case EVENT_TYPE:
            analyzeEventAttribute(thisAttribute);
        default: break;
        }

        if (inheritedAttribute != NULL) {
            Symbol *definingSymbol = definingSymbolOfAttribute(owningSymbol->fields.entity.parent, thisAttribute->id);
            if (!equalTypes(inheritedAttribute->type, thisAttribute->type)) {
                lmLogv(&thisAttribute->srcp, 332, sevERR, definingSymbol->string, typeToString(inheritedAttribute->type), NULL);
            } else if (isComplexType(thisAttribute->type)) {
                /* Verify that the inherited member class is a superclass
                   to the one in this attribute */
                if (thisAttribute->type == SET_TYPE) {
                    analyzeInheritedSetAttribute(thisAttribute, inheritedAttribute, definingSymbol);
                } else if (thisAttribute->type == INSTANCE_TYPE) {
                    analyzeInheritedReferenceAttribute(thisAttribute, inheritedAttribute, definingSymbol);
                } else
                    SYSERR("Unimplemented complex attribute type", thisAttribute->srcp);
            }
        } else if (thisAttribute->type == SET_TYPE
                   && thisAttribute->set->fields.set.memberType == UNINITIALIZED_TYPE)
            /* Empty set initializations are not allowed unless inherited */
            lmLog(&thisAttribute->srcp, 413, sevERR, "");
    }
}


/*======================================================================*/
void analyzeAllAttributes() {
    analyzeAllClassAttributes();
    analyzeAllInstanceAttributes();
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeOfInstance(Id *id, Id *attribute) {
    Attribute *atr = NULL;
    Symbol *sym = id->symbol;

    id->code = sym->code;
    atr = findAttribute(sym->fields.entity.props->attributes, attribute);
    if (atr == NULL)
        lmLog(&attribute->srcp, 315, sevERR, id->string);
    return atr;
}


/*----------------------------------------------------------------------*/
static Attribute *findAttributeOfSymbol(Symbol *symbol, Id *attribute, Id *id, char *message) {
	Attribute *atr = NULL;

    if (symbol) {
        switch (symbol->kind) {
        case CLASS_SYMBOL:
        case INSTANCE_SYMBOL:
            atr = findAttribute(symbol->fields.entity.props->attributes, attribute);
            break;
        case ERROR_SYMBOL:
            break;
        default:
            SYSERR("unexpected symbol->kind", attribute->srcp);
            break;
        }
    }
    if (atr == NULL)
        lmLogv(&attribute->srcp, 316, sevERR, attribute->string, message,
               id->string, symbol->string, NULL);
    return(atr);
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeOfParameter(Id *id, Id *attributeId, Context *context) {
    Attribute *atr = NULL;
    Symbol *sym = id->symbol;

    if (sym->fields.parameter.class != NULL) {
        Symbol *classOfId = classOfIdInContext(context, id);
        atr = findAttributeOfSymbol(classOfId, attributeId, id, "parameter");
    }
	return atr;
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeOfLocal(Id *id, Id *attribute, Context *context) {
    Attribute *atr = NULL;
    Symbol *sym = id->symbol;

    if (sym->fields.local.class != NULL) {
        Symbol *classOfLocal = classOfIdInContext(context, id);
        atr = findAttributeOfSymbol(classOfLocal, attribute, id, "variable");
    }
	return atr;
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeOfId(Id *id, Id *attribute, Context *context)
{
    Attribute *atr = NULL;
    Symbol *sym = id->symbol;

    if (sym) {
        switch (sym->kind) {
        case INSTANCE_SYMBOL: atr = resolveAttributeOfInstance(id, attribute); break;
        case PARAMETER_SYMBOL: atr = resolveAttributeOfParameter(id, attribute, context); break;
        case LOCAL_SYMBOL: atr = resolveAttributeOfLocal(id, attribute, context); break;
        case ERROR_SYMBOL: break;
        default: SYSERR("Unexpected symbol kind", id->srcp);
        }
        return atr;
    } else /* no symbol found */
        return NULL;
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeOfCurrentActor(Id *attribute, Context *context)
{
    Attribute *atr = NULL;

    atr = findAttribute(actorSymbol->fields.entity.props->attributes, attribute);
    if (atr == NULL)
        lmLogv(&attribute->srcp, 314, sevERR, attribute->string, "Actor", "actor", NULL);
    return atr;
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeOfCurrentLocation(Id *attribute, Context *context)
{
    Attribute *atr = NULL;

    atr = findAttribute(locationSymbol->fields.entity.props->attributes, attribute);
    if (atr == NULL)
        lmLogv(&attribute->srcp, 314, sevERR, attribute->string, "Location", "location", NULL);
    return atr;
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeOfThis(Id *attribute, Context *context)
{
    Attribute *atr = NULL;
    Context *thisContext = context;
    Bool contextFound = FALSE;

    while (!contextFound && thisContext != NULL) {
        switch (thisContext->kind) {
        case CLASS_CONTEXT:
            if (thisContext->class == NULL)
                SYSERR("Context->class == NULL", attribute->srcp);

            atr = findAttribute(thisContext->class->props->attributes, attribute);
            contextFound = TRUE;
            break;

        case INSTANCE_CONTEXT:
            if (thisContext->instance == NULL)
                SYSERR("context->instance == NULL", attribute->srcp);

            atr = findAttribute(thisContext->instance->props->attributes, attribute);
            contextFound = TRUE;
            break;

        default:
            thisContext = thisContext->previous;
        }
    }
    /* If no context found then THIS is not defined here which we should
       already have reported. Report that the attribute was not found. */
    if (contextFound && atr == NULL)
        lmLog(&attribute->srcp, 313, sevERR, attribute->string);
    return atr;
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeToClass(Symbol *class, Id *attribute, Context *context) {
    Attribute *atr = NULL;

    if (class != NULL) {
        atr = findAttribute(class->fields.entity.props->attributes, attribute);
        if (!atr)
            lmLogv(&attribute->srcp, 317, sevERR, attribute->string, "the expression",
                   class->string, NULL);
    }
    return atr;
}


/*----------------------------------------------------------------------*/
static Attribute *resolveAttributeToWhat(What *what, Id *attribute, Context *context)
{
    /* Analyze a reference to an attribute. Will handle static identifiers and
       parameters and return a reference to the attribute node, if all is well. */

    switch (what->kind) {
    case WHAT_ID: return resolveAttributeOfId(what->id, attribute, context); break;
    case WHAT_ACTOR: return resolveAttributeOfCurrentActor(attribute, context); break;
    case WHAT_LOCATION: return resolveAttributeOfCurrentLocation(attribute, context); break;
    case WHAT_THIS: return resolveAttributeOfThis(attribute, context); break;
    default: SYSERR("Unexpected what->kind in switch", what->srcp);
    }
    return NULL;
}


/*======================================================================*/
Attribute *resolveAttributeToExpression(Expression *exp, Id *attributeId, Context *context) {
    switch (exp->kind) {
    case WHAT_EXPRESSION:
        return resolveAttributeToWhat(exp->fields.wht.wht, attributeId, context);
    case ATTRIBUTE_EXPRESSION:
        return resolveAttributeToClass(exp->class, attributeId, context);
    default:
        lmLog(&exp->srcp, 442, sevERR, "");
    }
    return NULL;
}


/*----------------------------------------------------------------------*/
static void generateAttribute(Attribute *attribute, int instanceCode)
{
	AttributeEntry entry;
	Attribute *new;

	if (attribute->type == STRING_TYPE || attribute->type == SET_TYPE) {
		/* Now make a copy to use for initialisation if attribute is
		   inherited, else the address will be overwritten by generation
		   of other instances of the same attribute */
		if (attribute->type == STRING_TYPE) {
			/* We need to ensure that it is encode it first */
			if (!attribute->encoded) {
				encode(&attribute->fpos, &attribute->len);
				attribute->encoded = TRUE;
			}
			new = newStringAttribute(attribute->srcp, attribute->id, attribute->fpos, attribute->len);
			adv.stringAttributes = concat(adv.stringAttributes, new, ATTRIBUTE_LIST);
		} else {			/* SET ATTRIBUTE */
			/* Make a copy to keep the address in */
			new = newSetAttribute(attribute->srcp, attribute->id, attribute->set);
			new->setType = attribute->setType;
			adv.setAttributes = concat(adv.setAttributes, new, ATTRIBUTE_LIST);
		}
		new->address = nextEmitAddress(); /* Record on which Aadress to put it */
		new->instanceCode = instanceCode; /* Which instance owns it? */
	}

	entry.code = attribute->id->code;
	entry.value = attribute->value;
	entry.id = attribute->stringAddress;
	emitEntry(&entry, sizeof(entry));
}


/*======================================================================*/
static void generateAttributeNames(List *atrs) {
	List *lst;
    if (opts[OPTDEBUG].value) {
        for (lst = atrs; lst != NULL; lst = lst->next) {
            lst->member.atr->stringAddress = nextEmitAddress();
            emitString(lst->member.atr->id->string);
        }
    }
}


/*======================================================================*/
static void generateAttributeEntries(List *atrs, int instanceCode) {
	List *lst;
    for (lst = atrs; lst != NULL; lst = lst->next) {
        if (instanceCode == 0) printf("instance == 0\n");
        generateAttribute(lst->member.atr, instanceCode);
        attributeAreaSize += AwordSizeOf(AttributeEntry);
    }
    emit(EOF);
}


/*======================================================================*/
Aword generateAttributes(List *atrs, int instanceCode) /* IN - List of attribute nodes */
{
    Aaddr adr;

	generateAttributeNames(atrs);

    adr = nextEmitAddress();

	generateAttributeEntries(atrs, instanceCode);

    attributeAreaSize += 1;

    return(adr);
}



/*======================================================================*/
Aaddr generateStringInit(void)
{
    /* Generate initialisation value table for string attributes. */

    List *atrs;
    StringInitEntry entry;
    Aaddr adr = nextEmitAddress();

    for (atrs = adv.stringAttributes; atrs != NULL; atrs = atrs->next) {
        entry.fpos = atrs->member.atr->fpos;
        entry.len = atrs->member.atr->len;
        entry.instanceCode = atrs->member.atr->instanceCode;
        entry.attributeCode = atrs->member.atr->id->code;
        emitEntry(&entry, sizeof(entry));
    }
    emit(EOF);
    return adr;
}


/*======================================================================*/
void generateSet(Expression *exp) {
    List *elements;

    ITERATE (elements, exp->fields.set.members)
        switch (exp->fields.set.memberType) {
        case INSTANCE_TYPE: generateSymbol(symbolOfExpression(elements->member.exp, NULL)); break;
        case INTEGER_TYPE: emit(elements->member.exp->fields.val.val); break;
        default: SYSERR("Generating unexpected type in Set attribute", elements->member.exp->srcp);
        }
    emit(EOF);
}



/*----------------------------------------------------------------------*/
static Aaddr generateSetAttribute(Attribute *atr)
{
    /* Generate initial set for an attribute */

    Aaddr adr = nextEmitAddress();

    if (atr->setType == STRING_TYPE)
        SYSERR("Can't generate STRING sets yet", atr->srcp);

    generateSet(atr->set);

    return adr;
}


/*======================================================================*/
Aaddr generateSetInit(void)
{
    /* Generate initialisation value table for set attributes. */

    List *atrs;
    SetInitEntry entry;
    Aaddr adr;

    ITERATE (atrs, adv.setAttributes)
        atrs->member.atr->setAddress = generateSetAttribute(atrs->member.atr);

    adr = nextEmitAddress();
    ITERATE (atrs, adv.setAttributes) {
        entry.size = length(atrs->member.atr->set->fields.set.members);
        entry.setAddress = atrs->member.atr->setAddress;
        entry.instanceCode = atrs->member.atr->instanceCode;
        entry.attributeCode = atrs->member.atr->id->code;
        emitEntry(&entry, sizeof(entry));
    }
    emit(EOF);

    return adr;
}


/*----------------------------------------------------------------------*/
static void dumpInheritance(AttributeInheritance inheritance)
{
    switch (inheritance) {
    case UNKNOWN_INHERITANCE: put("UNKNOWN"); break;
    case LOCAL: put("LOCAL"); break;
    case INHERITED_REDEFINED: put("INHERITED/REDEFINED"); break;
    case INHERITED: put("INHERITED"); break;
    }
}


/*======================================================================*/
void dumpAttribute(Attribute *atr)
{
    put("ATR: "); dumpSrcp(atr->srcp); indent();
    put("type: "); dumpType(atr->type);
    put(", inheritance: "); dumpInheritance(atr->inheritance); nl();
    put("id: "); dumpId(atr->id); nl();
    put("definingSymbol: "); dumpSymbol(atr->definingSymbol); nl();
    put("instanceCode: "); dumpInt(atr->instanceCode); nl();
    put("address: "); dumpAddress(atr->address); nl();
    switch (atr->type) {
    case STRING_TYPE:
        put("stringAddress: "); dumpAddress(atr->stringAddress);
        put(", fpos: "); dumpInt(atr->fpos);
        put(", len: "); dumpInt(atr->len);
        break;
    case INTEGER_TYPE:
        put("value: "); dumpInt(atr->value);
        break;
    case BOOLEAN_TYPE:
        put("value: "); dumpBool(atr->value);
        break;
    case REFERENCE_TYPE:
    case INSTANCE_TYPE:
        put("reference: "); dumpId(atr->reference); nl();
        put("referenceClass: "); dumpSymbol(atr->referenceClass); nl();
        put("initialized: "); dumpBool(atr->initialized);
        break;
    case SET_TYPE:
        put("setType: "); dumpType(atr->setType); nl();
        if (atr->setType == INSTANCE_TYPE) {
            put("atr->setClass: "); dumpSymbol(atr->setClass); nl();
        }
        put("set: "); dumpExpression(atr->set);
        break;
    default:
        put("stringAddress: "); dumpAddress(atr->stringAddress);
        put(", fpos: "); dumpInt(atr->fpos);
        put(", len: "); dumpInt(atr->len); nl();
        put("value: "); dumpInt(atr->value); nl();
        put("instance: "); dumpId(atr->reference); nl();
        put("setType: "); dumpType(atr->setType); nl();
        if (atr->setType == INSTANCE_TYPE) {
            put("atr->setClass: "); dumpPointer(atr->setClass);
            if (atr->setClass != NULL) {
                put(" \""); put(atr->setClass->string); put("\"");
            }
            nl();
        }
        put("set: "); dumpExpression(atr->set);
        break;
    }
    out();

}
