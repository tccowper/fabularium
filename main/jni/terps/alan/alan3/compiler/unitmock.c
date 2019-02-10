/*======================================================================*\

unitmock.c

Mock some basic structures and functions for unittesting of the Alan compiler

\*======================================================================*/

#include "sysdep.h"
#include "acode.h"
#include "util.h"

#include "srcp_x.h"

#include <stdio.h>
#include <setjmp.h>


#include "unitmock.h"


Aword *memory;


/*======================================================================*/
Aword convertFromACD(Aword w)
{
    Aword s;                      /* The swapped ACODE word */
    char *wp, *sp;
    int i;

    wp = (char *) &w;
    sp = (char *) &s;

    if (littleEndian())
        for (i = 0; i < sizeof(Aword); i++)
            sp[sizeof(Aword)-1 - i] = wp[i];
    else
        for (i = 0; i < sizeof(Aword); i++)
            sp[i] = wp[i];
  
    return s;
}


/*----------------------------------------------------------------------*/
static Aword reversed(Aword w)		/* IN - The ACODE word to swap bytes in */
{
    Aword s;			/* The swapped ACODE word */
    char *wp, *sp;
    int i;

    wp = (char *) &w;
    sp = (char *) &s;

    for (i = 0; i < sizeof(Aword); i++)
        sp[sizeof(Aword)-1 - i] = wp[i];

    return (s);
}

/*----------------------------------------------------------------------*/
static void reverse(Aword *w)
{
    *w = reversed(*w);
}

/*----------------------------------------------------------------------*/
static void reverseHdr(ACodeHeader *header)
{
    int i;

    /* Reverse all words in the header except the first (version marking) */
    for (i = 1; i < sizeof(ACodeHeader)/sizeof(Aword); i++)
        reverse(&((Aword *)header)[i]);
}

/*======================================================================*/
void loadACD(char fileName[])
{
    ACodeHeader temporaryHeader;
    int readSize = 0;
    FILE *acdFile = fopen(fileName, "rb");

    readSize = fread(&temporaryHeader, 1, sizeof(temporaryHeader), acdFile);
    if (readSize != sizeof(temporaryHeader))
        SYSERR("Could not read header", nulsrcp);

    if (littleEndian())
        reverseHdr(&temporaryHeader);

    memory = calloc(4*temporaryHeader.size, 1);

    rewind(acdFile);
    fread(memory, sizeof(Aword), temporaryHeader.size, acdFile);

}
