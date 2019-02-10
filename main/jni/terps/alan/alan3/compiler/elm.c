/*----------------------------------------------------------------------*\

  ELM.C
  Element Nodes

  \*----------------------------------------------------------------------*/

#include "elm_x.h"

#include "util.h"

#include "srcp_x.h"
#include "id_x.h"
#include "lst_x.h"
#include "wrd_x.h"
#include "dump_x.h"
#include "lmList.h"

#include "stx.h"
#include "sym.h"		/* SYM-nodes */
#include "lst.h"		/* LST-nodes */

#include "emit.h"
#include "acode.h"


/* PUBLIC: */


/* PRIVATE: */

static int level = 0;


/*----------------------------------------------------------------------*/
static Element *newElement(Srcp *srcp,
			   ElementKind kind,
			   Id *id,
			   int flags)
{
    Element *new;                                  /* The newly created node */

    progressCounter();

    new = NEW(Element);

    new->srcp = *srcp;
    new->kind = kind;
    new->id = id;
    new->flags = flags;
    new->res = NULL;
    new->stx = NULL;

    return(new);
}


/*======================================================================*/
Element *newWordElement(Srcp srcp, Id *word)
{
    Element *new;			/* The newly created node */

    new = newElement(&srcp, WORD_ELEMENT, word, 0);

    return new;
}


/*======================================================================*/
Element *newParameterElement(Srcp srcp, Id *word, int flags)
{
    Element *new;			/* The newly created node */

    new = newElement(&srcp, PARAMETER_ELEMENT, word, flags);

    return new;
}


/*======================================================================*/
Element *newEndOfSyntax(void)
{
    Element *new;			/* The newly created node */

    new = newElement(&nulsrcp, END_OF_SYNTAX, NULL, 0);

    return new;
}


/*----------------------------------------------------------------------*/
static void analyzeElement(Element *elm)
{
    progressCounter();

    switch (elm->kind) {
    case WORD_ELEMENT:
        elm->id->code = newPrepositionWord(elm->id->string);
        break;
    case PARAMETER_ELEMENT:
    case END_OF_SYNTAX:
        break;
    default:
        SYSERR("Unknown element node kind", elm->id->srcp);
        break;
    }
}

/*----------------------------------------------------------------------*/
static void checkForDuplicatedParameterNames(List *parameters) {
    List *elements, *list;

    for (list = parameters; list != NULL; list = list->next) {
        Element *outerElement = list->member.elm;
        for (elements = list->next; elements != NULL; elements = elements->next) {
            Element *innerElement = elements->member.elm;
            if (equalId(outerElement->id, innerElement->id))
                lmLog(&innerElement->id->srcp, 216, sevERR, innerElement->id->string);
        }
    }
}


/*======================================================================*/
List *analyzeElements(List *elements, List *restrictions, Syntax *syntax)
{
    Element *firstElement = elements->member.elm; /* Set to be the first (yes, there is always at least one!) */
    List *list, *parameters = NULL;
    List *restrictionList;
    int parameterCount = 1;
    Bool multiple = FALSE;

#ifdef REQUIRE_VERB_FIRST
    if (firstElement->kind != WORD_ELEMENT)
        /* First element must be a player word */
        lmLog(&firstElement->srcp, 209, sevERR, "");
    else
        firstElement->id->code = newVerbWord(firstElement->id->string, syntax);
#else
    if (firstElement->kind == WORD_ELEMENT) {
        firstElement->id->code = newVerbWord(firstElement->id->string, syntax);
		elements = elements->next; /* Done with the first element */
	}
#endif

    /* Analyze the elements, number parameters and find the restriction */
    for (list = elements; list != NULL; list = list->next) {
        Element *element = list->member.elm;
        if (element->kind == PARAMETER_ELEMENT) {
            element->id->code = parameterCount++;
            if ((element->flags & MULTIPLEBIT) != 0) {
                if (multiple)
                    lmLog(&element->srcp, 217, sevWAR, "");
                else
                    multiple = TRUE;
            }
            parameters = concat(parameters, element, ELEMENT_LIST);

            /* Find first class restrictions */
            for (restrictionList = restrictions; restrictionList; restrictionList = restrictionList->next) {
                if (equalId(restrictionList->member.res->parameterId, element->id)) {
                    element->res = restrictionList->member.res;
                    restrictionList->member.res->parameterId->code = element->id->code;
                }
            }
        }
        analyzeElement(element);
    }

    checkForDuplicatedParameterNames(parameters);
    return parameters;
}



/*----------------------------------------------------------------------*/
static Bool equalElements(List *element1, List *element2)
{
    if (element1 == NULL || element2 == NULL)
        return element2 == element1;
    else if (element1->member.elm->kind == element2->member.elm->kind) {
        switch (element1->member.elm->kind) {
        case END_OF_SYNTAX:
        case PARAMETER_ELEMENT:
            return TRUE;
        case WORD_ELEMENT:
            return equalId(element1->member.elm->id, element2->member.elm->id);
        }
        SYSERR("Unexpected element kind", element1->member.elm->srcp);
    } else
        return FALSE;
    return FALSE;
}


/*----------------------------------------------------------------------
  Advances a copy of the incoming list of elmList pointers parallell
  to their next elm, which it returns.  */
static List *advance(List *elmsList) /* IN - The list to advance */
{
    List *list;
	List *copy = copyList(elmsList);

    for (list = copy; list != NULL; list = list->next) {
        list->member.lst = list->member.lst->next;
    }
	return copy;
}


/*----------------------------------------------------------------------*/
static List *first(List **listP)
{
    List *theFirst = *listP;

    *listP = theFirst->next;	/* Set list to point to second element */
    theFirst->next = NULL;	/* Remove first element */
    return theFirst;
}


/*----------------------------------------------------------------------*/
static List *partitionElements(List **elmsListP) /* INOUT - Address to pointer to the list */
{
    /*
      Partitions a list of elmLists into one list containing all elms
      equal to the first one, and one list containing the rest of the
      list.
    */

    List *part, *rest, *elms, *this, *p;

    if (*elmsListP == NULL)
        return NULL;

    /* Remove the first element from the list to form the base for the new partition */
    rest = *elmsListP;
    part = first(&rest);

    elms = rest;
    while (elms != NULL) {
        if (equalElements(part->member.lst, elms->member.lst)) {
            this = first(&elms);
            part = combine(part, this);
            if (rest == this)
                rest = elms;
            else {
                for (p = rest; p->next != this; p = p->next)
                    ;
                p->next = elms;
            }
        } else {
            elms = elms->next;
        }
    }
    *elmsListP = rest;
    return part;
}


/*----------------------------------------------------------------------*/
static ElementEntry *newEntryForPartition(List **entries) {
    ElementEntry *entry;

    entry = NEW(ElementEntry);
    entry->flags = 0;
    *entries = concat(*entries, entry, ELEMENT_ENTRIES_LIST);
    return(entry);
}


/*----------------------------------------------------------------------*/
static Aaddr restrictionTableAddress(List *partition) {
	return partition->member.lst->member.elm->stx->restrictionsAddress;
}


/*----------------------------------------------------------------------*/
static void entryForEOS(ElementEntry *entry, List *partition) {
    List *lst;
    if (partition->next != NULL) { /* More than one element in this partition? */
        /* That means that two syntax's are the same */
        for (lst = partition; lst != NULL; lst = lst->next)
            lmLog(&lst->member.lst->member.elm->stx->srcp, 334, sevWAR, "");
    }
    entry->code = EOS;        /* End Of Syntax */
    entry->flags = partition->member.lst->member.elm->stx->number; /* Syntax number */
    /* Point to the generated class restriction table */
    entry->next = restrictionTableAddress(partition);
}


/*----------------------------------------------------------------------*/
static void entryForParameter(ElementEntry *entry, List *partition, Syntax *stx) {
    List *element;

    entry->code = 0;
    entry->flags = partition->member.lst->member.elm->flags;
	/* TODO: ORing flags here is a problem, might actually give some
	   syntaxes different flags than intended which is not at all good
	   (omnipotent!!!) */
    ITERATE(element, partition->next) {
        entry->flags |= element->member.lst->member.elm->flags;
    }

    entry->next = generateElements(advance(partition), stx);
}


/*----------------------------------------------------------------------*/
static void entryForWord(ElementEntry *entry, Syntax *stx, List *partition) {
    entry->code = partition->member.lst->member.elm->id->code;
    entry->flags = 0;
    entry->next = generateElements(advance(partition), stx);
}


/*----------------------------------------------------------------------*/
static Aaddr generateEntries(List *entries, ElementEntry *entry) {
    List *lst;
    Aaddr elmadr;
    elmadr = nextEmitAddress();
    for (lst = entries; lst; lst = lst->next)
        emitEntry(lst->member.eent, sizeof(*entry));
    emit(EOF);
    return(elmadr);
}


/*======================================================================*/
Aaddr generateElements(List *elementLists, Syntax *stx)
{
    /*
      Generate the data structure for the syntax elements.  NOTE that
      the list is not the list of words as specified in the syntax
      statement.  Instead this list contains all identical elms for
      *all* syntax structures. Also note that the list links not elms
      but list nodes where the first element is the elm to consider (an
      extra level of lists!).

      This function is recursive in pre-order by grouping equal elements
      in the next level and generating each group first, then a table
      for this group pointing to the next level for each group, a.s.o.

      Currently this is a bit vasteful in that it always creates a new
      list.

      It should really request a partitioned list from partitionElements()
      instead and then traverse that list. This requires rewriting
      partitionElements() to deliver a list of partitions instead.

      TODO This code would be much clearer if it used Collection instead
      of Lists for the partitions.

    */
    List *elms = elementLists;
    List *partition;                   /* The current partion */
    Aaddr elmadr;
    List *entries = NULL;         /* List of next level entries */
    ElementEntry *entry;		/* One entry in the list */

    if (elms == NULL)
        return 0;			/* End of chain */

    progressCounter();

    level++;
    for (partition = partitionElements(&elms); partition != NULL; partition = partitionElements(&elms)) {
        /* Make one entry for this partition */
        entry = newEntryForPartition(&entries);

        switch (partition->member.lst->member.elm->kind) {

        case END_OF_SYNTAX:		/* This partition was at end of syntax */
            entryForEOS(entry, partition);
            break;

        case PARAMETER_ELEMENT:
            entryForParameter(entry, partition, stx);
            break;

        case WORD_ELEMENT:
            entryForWord(entry, stx, partition);
            break;
        }
    }

    /* Finally, generate this level */
    elmadr = generateEntries(entries, entry);

    level--;
    return(elmadr);
}



/*======================================================================*/
void dumpElement(Element *element)
{
    if (element == NULL) {
        put("NULL");
        return;
    }

    put("ELM: "); dumpPointer(element); dumpSrcp(element->srcp); indent();
    put("kind: ");
    switch (element->kind) {
    case PARAMETER_ELEMENT: {
        char buf[80];
        sprintf(buf, "PARAMETER (Flags: 0x%x)", element->flags);
        put(buf);
        nl();
        break;
    }
    case WORD_ELEMENT:
        put("WORD"); nl();
        break;
    case END_OF_SYNTAX:
        put("EOS"); nl();
        break;
    default:
        put("*** ERROR ***"); nl();
        break;
    }
    put("id: "); dumpId(element->id); out();
}
