#ifndef _TOKEN_H_
#define _TOKEN_H_
/*----------------------------------------------------------------------*\

				TOK.H
			      Token Type

\*----------------------------------------------------------------------*/

/* USE: */
#include "srcp.h"


/* Types: */

typedef struct Token {
  int code;
  Srcp srcp;
  char chars[257];		/* The scanned characters */
  long fpos;			/* File position in text file for strings */
  int len;			/* Length of a text string */
} Token;


/* Data: */

/* Functions: */

#endif
