/*----------------------------------------------------------------------*\

  CNT.C
  Container Nodes

\*----------------------------------------------------------------------*/

#include "cnt_x.h"

/* IMPORTS: */
#include "alan.h"

#include "srcp_x.h"
#include "adv_x.h"
#include "sym_x.h"
#include "lst_x.h"
#include "stm_x.h"
#include "chk_x.h"
#include "id_x.h"
#include "context_x.h"
#include "dump_x.h"

#include "util.h"
#include "stm.h"
#include "elm.h"
#include "lim.h"

#include "lmList.h"
#include "acode.h"
#include "emit.h"



/* PUBLIC: */

int containerCount = 0;



/*======================================================================*/
ContainerBody *newContainerBody(Srcp srcp,
                                Bool opaque,
                                Id *takes,
                                List *lims,
                                List *hstms,
                                List *estms,
                                List *extractChecks,
                                List *extractStatements)
{
    ContainerBody *new;		/* The newly allocated area */

    new = NEW(ContainerBody);

    new->srcp = srcp;
    new->analyzed = FALSE;
    new->generated = FALSE;
    new->opaque = opaque;
    if (takes != NULL)
        new->taking = takes;
    else {
        new->taking = newId(nulsrcp, "object");
        new->taking->symbol = objectSymbol;
    }
    new->mayContain = NULL;     /* Will be analyzed later */
    new->limits = lims;
    new->hstms = hstms;
    new->estms = estms;
    new->extractChecks = extractChecks;
    new->extractStatements = extractStatements;

    return(new);
}


/*----------------------------------------------------------------------*/
static ContainerBody *newEmptyContainerBody(void) {
    return newContainerBody(nulsrcp, FALSE, NULL, NULL, NULL, NULL, NULL, NULL);
}


/*======================================================================*/
Container *newContainer(ContainerBody *body)
{
    Container *new;		/* The newly allocated area */

    progressCounter();

    new = NEW(Container);
    new->ownerProperties = NULL;
    if (body == NULL)
        new->body = newEmptyContainerBody();
    else
        new->body = body;

    adv.cnts = concat(adv.cnts, new, CONTAINER_LIST);

    return(new);
}


/*======================================================================*/
void symbolizeContainer(Container *theContainer) {
    if (theContainer != NULL) {
        Id *id = theContainer->body->taking;
        id->symbol = lookup(id->string);
    }
}


/*======================================================================*/
void verifyContainerForInitialLocation(What *wht, Context *context, char *constructMessage)
{
    Symbol *sym;

    if (wht == NULL)
        return;

    switch (wht->kind) {
    case WHAT_THIS:
        lmLogv(&wht->srcp, 412, sevERR, "instance (This)", "declarations", NULL);
        break;
    case WHAT_ID:
        sym = symcheck(wht->id, INSTANCE_SYMBOL, context);
        if (sym)
            switch (sym->kind) {
            case INSTANCE_SYMBOL:
                if (sym->fields.entity.props->container == NULL)
                    lmLogv(&wht->srcp, 318, sevERR, wht->id->string, constructMessage, NULL);
                else {
                    Symbol *class;
                    switch (context->kind) {
                    case CLASS_CONTEXT:
                        class = context->class->props->id->symbol;
                        break;
                    case INSTANCE_CONTEXT:
                        if (context->instance->props->parentId != NULL)
                            class = context->instance->props->parentId->symbol;
                        else
                            class = entitySymbol; /* Resonable default if errors caused the parent to be NULL */
                        break;
                    default: SYSERR("Unexpected context->kind", wht->srcp); return;
                    }
                    if (!inheritsFrom(class, sym->fields.entity.props->container->body->taking->symbol))
                        lmLog(&wht->srcp, 404, sevERR, sym->fields.entity.props->container->body->taking->string);
                }
                break;
            case ERROR_SYMBOL:
                break;
            default:
                SYSERR("Unexpected symbol kind", wht->srcp);
                break;
            }
        break;

    case WHAT_LOCATION:
        lmLogv(&wht->srcp, 412, sevERR, "Location", "declarations", NULL);
        break;

    case WHAT_ACTOR:
        lmLogv(&wht->srcp, 412, sevERR, "Actor", "declarations", NULL);
        break;

    default:
        SYSERR("Unexpected wht->kind", wht->srcp);
        break;
    }
}


/*======================================================================*/
void analyzeContainer(Container *theContainer, Context *context)
{
    List *lims;			/* List of limits */

    if (theContainer == NULL) return;

    progressCounter();

    if (context->kind == INSTANCE_CONTEXT)
        theContainer->ownerProperties = context->instance->props;

    if (!theContainer->body->analyzed) {
        /* Analyze which class it takes */
        Id *id = theContainer->body->taking;
        id->symbol = symcheck(id, CLASS_SYMBOL, context);
        if (id->symbol == actorSymbol)
            lmLogv(&id->srcp, 402, sevERR, "An Actor", NULL);
        if (id->symbol == locationSymbol)
            lmLogv(&id->srcp, 402, sevERR, "A Location", NULL);

        /* Analyze the limits */
        for (lims = theContainer->body->limits; lims != NULL; lims = lims->next)
            analyzeLimit(lims->member.lim, id->symbol, context);

        /* Analyze header and empty statments */
        analyzeStatements(theContainer->body->hstms, context);
        analyzeStatements(theContainer->body->estms, context);
        theContainer->body->analyzed = TRUE;

        /* Analyze the extract checks and statements */
        analyzeChecks(theContainer->body->extractChecks, context);
        analyzeStatements(theContainer->body->extractStatements, context);
    }
}



/*======================================================================*/
void numberContainers(void)
{
    List *lst;			/* The list of containers */

    /* We must number the containers in the order that they have in the
       adv-list since that is the order the container bodies will be
       generated into the ContainerEntry table */
    for (lst = adv.cnts; lst != NULL; lst = lst->next)
        if (lst->member.cnt->ownerProperties != NULL)
            lst->member.cnt->code = ++containerCount;
}


/*----------------------------------------------------------------------*/
static void generateContainerBody(ContainerBody *body)
{
    progressCounter();

    if (!body->generated) {
        body->limadr = generateLimits(body);

        if (body->hstms != NULL) {
            body->hadr = nextEmitAddress();
            generateStatements(body->hstms);
            emit0(I_RETURN);
        } else
            body->hadr = 0;

        if (body->estms != NULL) {
            body->eadr = nextEmitAddress();
            generateStatements(body->estms);
            emit0(I_RETURN);
        } else
            body->eadr = 0;

        if (body->extractChecks != NULL) {
            body->extractChecksAddress = generateChecks(body->extractChecks);
        } else
            body->extractChecksAddress = 0;

        if (body->extractStatements != NULL) {
            body->extractStatementsAddress = nextEmitAddress();
            generateStatements(body->extractStatements);
            emit0(I_RETURN);
        } else
            body->extractStatementsAddress = 0;
        body->generated = TRUE;
    }
}



/*----------------------------------------------------------------------*/
static void generateContainerEntry(Container *cnt)
{
    ContainerEntry entry;

    entry.class = cnt->body->taking->symbol->code;
    entry.limits = cnt->body->limadr;
    entry.header = cnt->body->hadr;
    entry.empty = cnt->body->eadr;
    entry.extractChecks = cnt->body->extractChecksAddress;
    entry.extractStatements = cnt->body->extractStatementsAddress;
    entry.owner = cnt->ownerProperties->id->symbol->code;
    emitEntry(&entry, sizeof(entry));
}


/*======================================================================*/
Aaddr generateContainers(ACodeHeader *header)
{
    List *lst;			/* The list of containers */
    Aaddr adr;

    if (adv.cnts == NULL)		/* Any containers at all? */
        adr = nextEmitAddress();
    else {
        /* Limits, header and empty statements for the container */
        for (lst = adv.cnts; lst != NULL; lst = lst->next)
            if (lst->member.cnt->ownerProperties != NULL)
                generateContainerBody(lst->member.cnt->body);

        adr = nextEmitAddress();	/* Save ACODE address to container list */
        /* Container list */
        for (lst = adv.cnts; lst != NULL; lst = lst->next)
            if (lst->member.cnt->ownerProperties != NULL)
                generateContainerEntry(lst->member.cnt);
    }
    emit(EOF);

    header->containerMax = containerCount;

    return(adr);
}



/*======================================================================*/
void dumpContainer(Container *container)
{
    if (container == NULL) {
        put("NULL");
        return;
    }

    put("CONTAINER: "); dumpPointer(container); dumpSrcp(container->body->srcp); indent();
    put("code: "); dumpInt(container->code); nl();
    put("ownerProperties: "); dumpPointer(container->ownerProperties); nl();
    put("body: "); dumpPointer(container->body); nl();
    put("body.takes: "); dumpId(container->body->taking); nl();
    put("body.lims: "); dumpList(container->body->limits, LIMIT_LIST); nl();
    put("body.limadr: "); dumpAddress(container->body->limadr); nl();
    put("body.hstms: "); dumpList(container->body->hstms, STATEMENT_LIST); nl();
    put("body.hadr: "); dumpAddress(container->body->hadr); nl();
    put("body.estms: "); dumpList(container->body->estms, STATEMENT_LIST); nl();
    put("body.eadr: "); dumpAddress(container->body->eadr); nl();
    put("body.extractCheck: "); dumpList(container->body->extractChecks, CHECK_LIST); nl();
    put("body.extractCheckAdr: "); dumpAddress(container->body->extractChecksAddress); nl();
    put("body.extractStatements: "); dumpList(container->body->extractStatements, STATEMENT_LIST); nl();
    put("body.extractStatementsAdr: "); dumpAddress(container->body->extractStatementsAddress); out();
}
