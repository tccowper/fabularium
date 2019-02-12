#ifndef _CNT_H_
#define _CNT_H_
/*----------------------------------------------------------------------*\

				CNT.H
			   Container Nodes

\*----------------------------------------------------------------------*/

/* USE: */
#include "acode.h"
#include "srcp.h"
#include "id.h"
#include "lst.h"


/* Types: */
typedef struct ContainerBody {
    Srcp srcp;            /* Source position */
    Bool analyzed;        /* Is this container definition analyzed? */
    Bool generated;       /* And generated? */
    Bool opaque;          /* An opaque container? */
    Id *taking;           /* Which class does it take? */
    Bool visited;         /* Recursing flag for containment calculation to terminate */
    Symbol *mayContain;   /* Which class may it contain transitively? */
    List *limits;         /* Limits */
    Aaddr limadr;         /* ACODE address to limit table */
    List *hstms;          /* Header statements */
    Aaddr hadr;           /* ACODE address to header statements */
    List *estms;          /* 'Empty' statements */
    Aaddr eadr;           /* ACODE address to 'empty' statements */
    List *extractChecks;  /* Extract checks */
    Aaddr extractChecksAddress;
    List *extractStatements;	/* Extract statements (DOES) */
    Aaddr extractStatementsAddress;
} ContainerBody;

typedef struct Container {              /* To be used in instances */
    int code;                           /* Code for this container */
    struct Properties *ownerProperties;	/* Pointer to parents properties */
    ContainerBody *body;                /* Common info */
} Container;



#endif
