#ifndef _DUMP_X_H
#define _DUMP_X_H
/*----------------------------------------------------------------------*\

				DUMP_X.H
			     Dump Support

\*----------------------------------------------------------------------*/

#include "dump.h"


/* Data: */

/* Functions: */

extern void put(char str[]);
extern void nl(void);
extern void indent(void);
extern void out(void);
extern void dumpString(char s[]);
extern void dumpAddress(int adr);
extern void dumpPointer(void *adr);
extern void dumpInt(int i);
extern void dumpBool(Bool b);


#endif
