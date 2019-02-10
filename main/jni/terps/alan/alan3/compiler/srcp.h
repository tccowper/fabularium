#ifndef _SRCP_H
#define _SRCP_H
/*----------------------------------------------------------------------*\

				SRCP.H
			 Source Position Type

\*----------------------------------------------------------------------*/

/* USE: */


/* TYPES: */

typedef struct Srcp {
  int file;
  int line;
  int col;
  int startpos;
  int endpos;
} Srcp;

#endif
