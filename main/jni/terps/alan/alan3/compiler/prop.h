#ifndef _PROP_H_
#define _PROP_H_
/*----------------------------------------------------------------------*\

			       PROP.H
			    Property Nodes

\*----------------------------------------------------------------------*/


/* USE: */
#include "types.h"
#include "lst.h"
#include "whr.h"
#include "cnt.h"
#include "description.h"
#include "initialize.h"
#include "article.h"

#include "acode.h"


/* Currently we handle the following properties:

	Initial location
	Name(s)
	Pronouns
	Attributes
	Initialization
	Description Check
	Description Statements
	Defininte Article/Form
	Indefinite Article/Form
	Negative Article/Form
	Mentioned Clause
	Container
	Verbs
   Actor only:
	Scripts
   Location only:
	Entered Statements
	Exits
*/

#define NOOFPROPS 16

/* Types */
typedef enum {
  VIRGIN = 0,
  VISITED = 1,
  REPORTED = 2
} InspectionState;


typedef struct Properties {
  Id *id;                       /* Identifier */
  InspectionState circularInspection;
  Bool predefined;
  Aaddr idAddress;
  Id *parentId;       /* The instances parent */
  Where *whr;         /* Where is it initially? */
  List *names;			/* List of lists of adjectives and nouns (ids)*/
  List *nameStatement;		/* Statement to print the first name */
  Aaddr nameAddress;		/* Address to name printing statement */
  Srcp pronounsSrcp;
  List *pronouns;		/* List of words that can be used as pronouns */
  List *attributes;
  Aword attributeAddress;	/* ACODE address to attribute list */

  Srcp initializeSrcp;
  Initialize *initialize;	/* The Initialize node */

  Description *description;	/* The Description */
  Article *definite;		/* Definite Form/Article printing statements */
  Article *indefinite;		/* Indefinite Form/Article printing statements */
  Article *negative;		/* Negative Form/Article printing statements */
  List *mentioned;		/* Short (inventory like) statements */
  Srcp mentionedSrcp;
  Aword mentionedAddress;	/* ACODE address to short description (mentioned) code */
  Container *container;		/* Pointer to container property node */
  List *verbs;			/* List of verbs */
  Aword verbsAddress;		/* ACODE address to local verb table */
  List *scripts;		/* List of scripts */
  Aaddr scriptsAddress;		/* ACODE address to scripts */
  Srcp enteredSrcp;
  List *enteredStatements;	/* ENTERED statements */
  Aword enteredAddress;		/* ACODE address to ENTERED code */
  List *exits;			/* List of exits */
  Aaddr exitsAddress;		/* ACODE address to exits table */
} Properties;


#endif
