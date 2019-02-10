#ifndef _EMIT_H_
#define _EMIT_H_
/*----------------------------------------------------------------------*\

				EMIT.H
			ACODE Emit Header File

\*----------------------------------------------------------------------*/

/* USE other definitions */
#include "acode.h"


/* DATA */

ACodeHeader acodeHeader;


/* FUNCTIONS */

extern void initEmit(char acodeFileName[]);
extern void initEmitBuffer(Aword *bufferToUse);
extern Aaddr nextEmitAddress(void);
extern void emitString(char str[]);
extern void emitVariable(Aword word);
extern void emitConstant(int word);
extern void emitN(void *words, int noOfWords);
extern void emitEntry(void *entryToEmit, int noOfBytes);
extern void emit0(Aword op);
extern void emit1(Aword op, Aword arg1);
extern void emit2(Aword op, Aword arg1, Aword arg2);
extern void emit3(Aword op, Aword arg1, Aword arg2, Aword arg3);
extern void copyTextDataToAcodeFile(char dataFileName[]);
extern Aint emitControlStructure(void);
extern void writeHeader(ACodeHeader *acodeHeader);
extern void emit(Aword word);
extern Aword reversed(Aword word);
extern void finalizeEmit(void);
extern void terminateEmit(void);

#endif
