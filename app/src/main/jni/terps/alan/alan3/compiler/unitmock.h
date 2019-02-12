#ifndef _UNIT_H_
#define _UNIT_H_
/*----------------------------------------------------------------------*\

  unit.h

  Alan compiler unit test header

\*----------------------------------------------------------------------*/

#include "acode.h"

#define ASSERT(x) (unitAssert((x), __FILE__, __LINE__, __FUNCTION__))


extern Aword *memory;


extern void registerUnitTest(void (*aCase)());
extern void unitAssert(int x, char sourceFile[], int lineNumber, const char function[]);
extern void loadACD(char fileName[]);
extern Aword convertFromACD(Aword w);

#endif
