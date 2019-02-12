#ifndef _ENCODE_H_
#define _ENCODE_H_
/*----------------------------------------------------------------------*\

			       ENCODE.H
			    Text encoding

\*----------------------------------------------------------------------*/

/* USE other definitions */
#include "acode.h"


/* DATA */
extern int txtlen;		/* Number of bytes of text data */

/* FUNCTIONS */

extern void initEncoding(char textFileName[], char dataFileName[]);
extern void incFreq(int ch);
extern void encode(long *fpos, long *len);
extern void terminateEncoding(void);
extern Aaddr gefreq(void);


#endif
