#ifndef _WRD_X_H_
#define _WRD_X_H_
/*----------------------------------------------------------------------*\

				WRD_X.H
		     Dictionary Word Nodes Export

\*----------------------------------------------------------------------*/

#include "wrd.h"


/* USE: */
#include "acode.h"
#include "ins.h"
#include "stx.h"


/* DATA: */

extern int words[];



/* FUNCTIONS: */

/* Find a Word in the dictonary */
extern Word *findWord(char str[]);

/* Insert a Word into the dictionary */
extern int newPronounWord(char *theWord, Instance *reference);
extern int newPrepositionWord(char *theWord);
extern int newSynonymWord(char *theWord, Word *original);
extern int newAdjectiveWord(char *theWord, Instance *reference);
extern int newVerbWord(char *theWord, Syntax *syntax);
extern int newDirectionWord(char *theWord, int code);
extern int newNounWord(char *theWord, int code, Instance *reference);

extern void prepareWords(void);
extern void analyzeAllWords(void);
extern void finalizeWords(void);
extern Aaddr generateAllWords(void);


#endif
