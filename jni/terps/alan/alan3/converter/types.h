/*----------------------------------------------------------------------*\

    types.h

    Common types for the Alan compiler

\*----------------------------------------------------------------------*/
#ifndef _TYPES_
#define _TYPES_

typedef int Bool;
#ifndef TRUE
#define TRUE (0==0)
#define FALSE (!TRUE)
#endif

#endif
