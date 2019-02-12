#ifndef _MSG_H_
#define _MSG_H_
/*----------------------------------------------------------------------*\

				MSG.H
			       Messages

\*----------------------------------------------------------------------*/


/* USE: */
#include "acode.h"
#include "id.h"


/* Types: */
typedef struct MsgNod {
  Srcp srcp;
  Id *id;			/* ID of message type */
  MsgKind msgno;		/* It's identity */
  List *stms;			/* List of statements */
  Aaddr stmadr;			/* Address to generated statements */
} Message;


/* Data: */



/* Functions: */

/* Create a new node with a message declaration */
extern Message *newMessage(Srcp *srcp, Id *id, List *stms);

/* Prepare all system messages depending on the choosen language */
extern void prepareMessages(void);

/* Analyze the system messages */
extern void analyzeMessages(void);

/* Generate the system messages */
extern Aword gemsgs(void);

/* Generate a text string as a encoded message in the data file */
extern void generateText(char txt[]);


#endif
