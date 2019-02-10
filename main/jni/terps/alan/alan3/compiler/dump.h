#ifndef _DUMP_H
#define _DUMP_H
/*----------------------------------------------------------------------*\

				DUMP.H
			     Dump Support

\*----------------------------------------------------------------------*/

/* USE other definitions */
#include "sysdep.h"
#include "types.h"


/* Types: */

/* Dump kinds */
typedef enum dmpKd {
  DUMP_NOTHING = 0,
  DUMP_SYNONYMS = 1L,
  DUMP_PARSETABLE = (long)DUMP_SYNONYMS<<1,
  DUMP_SYNTAXTABLE = (long)DUMP_PARSETABLE<<1,
  DUMP_SYMBOLTABLE = (long)DUMP_SYNTAXTABLE<<1,
  DUMP_VERBTABLE = (long)DUMP_SYMBOLTABLE<<1,
  DUMP_CLASSES = (long)DUMP_VERBTABLE<<1,
  DUMP_INSTANCES = (long)DUMP_CLASSES<<1,
  DUMP_CONTAINERS = (long)DUMP_INSTANCES<<1,
  DUMP_EVENTS = (long)DUMP_CONTAINERS<<1,
  DUMP_RULES = (long)DUMP_EVENTS<<1,
  DUMP_ALL = (long)DUMP_RULES<<1,
  DUMP_ADDRESSES = (long)DUMP_ALL<<1,
  DUMP_AFTER_PARSE = (long)DUMP_ADDRESSES<<1,
  DUMP_AFTER_ANALYSIS = (long)DUMP_AFTER_PARSE<<1
} DmpKind;


/* Data: */


#endif
