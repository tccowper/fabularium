#ifndef _WRD_H_
#define _WRD_H_
/*----------------------------------------------------------------------*\

				WRD.H
			Dictionary Word Nodes

\*----------------------------------------------------------------------*/

/* USE: */
#include "acode.h"
#include "lst.h"
#include "id.h"


/* TYPES: */

typedef struct Word {		/* DICTIONARY ENTRY */
  int classbits;		/* Class of this entry as a bit in the set */
  int code;			/* Code for the word */
  char *string;			/* Name of this entry */
  List *ref[WRD_CLASSES];	/* Lists of references (objects etc) */
  Aaddr stradr;			/* ACODE address to string */
  Aaddr nounRefAddress;		/* ACODE address to reference table */
  Aaddr adjectiveRefAddress;	/* ACODE address to reference table */
  Aaddr pronounRefAddress;	/* ACODE address to reference table */
  struct Word *low, *high;	/* Links */
} Word;

#endif
