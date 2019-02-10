/*----------------------------------------------------------------------*\

	ListerMaker

	Source file for ListerMaker generated listing handler

\*----------------------------------------------------------------------*/

#include <stdio.h>
#include <time.h>
#include <string.h>
#include <stdlib.h>
#include <stdarg.h>
#include <ctype.h>



#include "lmList.h"


/*****************
 *               *
 * Private Types *
 *               *
 *****************/
#ifndef LMBOOL
#define LMBOOL int
#endif
#ifndef TRUE
#define TRUE (0==0)
#define FALSE (0!=0)
#endif

#define sevPUSH (sevSYS<<1)	/* Private Severity code for PUSH msg */
#define sevPOP  (sevSYS<<2)	/* Private Severity code for POP msg */
#define sevPAGE (sevSYS<<3)	/* Private Severity code for PAGE msg */
#define sevON   (sevSYS<<4)	/* Private Severity code for Listing On */
#define sevOFF  (sevSYS<<5)	/* Private Severity code for Listing Off */


typedef enum liPhase {
  PH_NONE = 0,
  PH_COLL = 1,
  PH_RETR = 2
} liPhase;

#define INITINCLUDE 10
#define INCINCLUDE  10
#define OMARG 8			/* Output margin length */
#define SRCWIDTH 1000
#define MSGWIDTH 1000
#define LSTWIDTH (SRCWIDTH+OMARG)
#define INITMSG 100
#define INCMSG  100
#define MAXMSG 1000
#define HEADERLINES 3
#define ESCAPECHAR '`'

/*********************
 *                   *
 *   INTERNAL DATA   *
 *                   *
 *********************/

static Srcp nulpos = {	/* Zero source position */
  0, 0, 0
};

static char *lmNoIns = "<Missing insertstring>";

#define lmMESSZ 2
typedef char *lmMsgs[lmMESSZ];

static lmMsgs msg[] = {
    { "0     ", "%1 Adventure Language System" },
    { "1     ", "        No warnings or errors detected." },
    { "2     ", "        No detected warnings." },
    { "3     ", "        No detected errors." },
    { "4     ", "        %1 informational message(s)." },
    { "5     ", "        %1 warning(s)." },
    { "6     ", "        %1 error(s)." },
    { "7     ", "Maximum number of messages exceeded." },
    { "100   ", "Parsing resumed here." },
    { "101   ", "%1 inserted." },
    { "102   ", "%1 deleted." },
    { "103   ", "%1 replaced by %2." },
    { "104   ", "Severe syntax error, construct ignored." },
    { "105   ", "Syntax error, couldn\'t recover." },
    { "106   ", "Parse stack overflow." },
    { "107   ", "Parse table error." },
    { "108   ", "Parsing terminated." },
    { "150   ", "Unterminated STRING." },
    { "151   ", "File name missing for $INCLUDE directive." },
    { "152   ", "Unterminated quoted identifier." },
    { "198   ", "Could not open output file \'%1\' for writing." },
    { "199   ", "Adventure source file (%1) not found." },
    { "201   ", "Mismatched block identifier, \'%1\' assumed." },
    { "202   ", "Multiple usage of direction \'%1\' in this EXIT." },
    { "203   ", "Multiple definition of EXIT \'%1\' in this location." },
    { "204   ", "Multiple use of %1 clause in this %2." },
    { "205   ", "Multiple usage of \'%1\' in this VERB definition." },
    { "206   ", "Multiple definition of SYNTAX for \'%1\'." },
    { "207   ", "VERB \'%1\' is not defined." },
    { "208   ", "\'%1\' is not a VERB." },
    { "209   ", "First element in a SYNTAX must be a player word." },
    { "210   ", "Action qualification not allowed here." },
    { "211   ", "Adventure must start at a Location." },
    { "212   ", "Syntax parameter \'%1\' overrides symbol." },
    { "213   ", "Verb alternatives not allowed here." },
    { "214   ", "Parameter not defined in syntax for \'%1\'." },
    { "215   ", "Syntax not compatible with syntax for \'%1\'." },
    { "216   ", "Parameter \'%1\' multiply defined in this SYNTAX." },
    { "217   ", "Only one multiple parameter allowed for each syntax. This one ignored." },
    { "218   ", "Multiple definition of attribute \'%1\'." },
    { "219   ", "Multiple declaration of %1." },
    { "220   ", "Multiple definition of \'%1\'." },
    { "221   ", "Multiple class restriction for parameter \'%1\'." },
    { "222   ", "Identifier \'%1\' in class definition is not a parameter." },
    { "223   ", "Unknown message identifier." },
    { "230   ", "No syntax defined for this verb, assumed \'%1 (object)\'." },
    { "301   ", "\'%1\' already declared as a Direction." },
    { "302   ", "\'%1\' already declared as a Location." },
    { "303   ", "\'%1\' already declared as a Verb." },
    { "304   ", "\'%1\' already declared as an Object." },
    { "305   ", "\'%1\' already declared as a Container." },
    { "306   ", "\'%1\' already declared as an Actor." },
    { "307   ", "\'%1\' already declared as an Event." },
    { "310   ", "Identifier \'%1\' not defined." },
    { "311   ", "Must refer to %1." },
    { "312   ", "Parameter not uniquely defined as %1, which is required." },
    { "315   ", "Attribute not defined for \'%1\'." },
    { "318   ", "Entity \'%1\' is not a Container." },
    { "320   ", "Word \'%1\' belongs to multiple word classes (%2 and %3)." },
    { "321   ", "Synonym target word \'%1\' not defined." },
    { "322   ", "Word \'%1\' already defined as a synonym." },
    { "330   ", "Wrong type of expression in context of %2. Must be of %1 type." },
    { "331   ", "Incompatible types in %1." },
    { "332   ", "Type of %1 attribute must match %2 attribute." },
    { "333   ", "The word \'%1\' is defined to be both a synonym and another word class." },
    { "334   ", "Multiple syntaxes are defined with this structure (words and parameters)." },
    { "335   ", "ELSE clause of a DEPENDING statement must be the last." },
    { "340   ", "Initial location for \'%1\' declared twice. This occurence ignored." },
    { "400   ", "Script not defined for Actor \'%1\'." },
    { "401   ", "Actor reference required outside Actor specification." },
    { "402   ", "An Actor can\'t be inside a Container." },
    { "403   ", "Script multiply defined for Actor \'%1\'." },
    { "404   ", "Attribute to %1 must be a default attribute." },
    { "406   ", "A Container, Integer or String have no default attributes." },
    { "407   ", "Attribute in LIMITS must be a default object attribute." },
    { "408   ", "Attributes in %1 must be of boolean type." },
    { "409   ", "No parameter defined in this context." },
    { "410   ", "A parameter may not be used in %1." },
    { "411   ", "%1 ignored for Actor \'hero\'." },
    { "412   ", "ACTOR is not allowed inside Events." },
    { "413   ", "Expression in %1 must be of integer type." },
    { "414   ", "Invalid initial location for %1." },
    { "415   ", "Invalid Where specification in %1 statement." },
    { "416   ", "Interval of size 1 in RANDOM expression." },
    { "417   ", "Comparing two constant entities will always yield the same result." },
    { "418   ", "Aggregate is only allowed on integer type attributes." },
    { "419   ", "%1 SET statement must be of integer or string type." },
    { "420   ", "Invalid What specification in %1." },
    { "501   ", "Location \'%1\' has no EXITs." },
    { "600   ", "Multiple use of option \'%1\', ignored." },
    { "601   ", "Unknown option, \'%1\'." },
    { "602   ", "Illegal value for option \'%1\'." },
    { "997   ", "SYSTEM ERROR: %1" },
    { "998   ", "Feature not implemented in %1." },
    { "999   ", "No Adventure generated." },
    { NULL }
};
typedef struct MSect {
  int offs;
  int len;
} MSect;

static MSect msects[] = {
    {0, 95}
};
static lmMessages currMsect = (lmMessages)0;

typedef struct Srctyp {		/* Stack of source files */
  char *fnm;			/* File name of source file */
  LMBOOL printed;		/* Is name shown yet? */
  FILE *file;			/* File descriptor */
  int fno;			/* File number */
  int lno;			/* Line number */
  int mno;			/* Message number for PUSH from this file */
  LMBOOL open;			/* Is it open? */
} Srctyp;

static Srctyp *src;
static int srcEntries = 0;

static int srclev;		/* Source file level */

static struct {			/* Message counters */
  int infos;
  int warns;
  int errs;
  int msgs;
} count;

static struct {			/* Output file */
  char *name;			/* Name ... */
  FILE *file;			/* and file pointer */
  LMBOOL open;			/* Is it open? */
} out;

static liPhase phase;	/* Phase of LIST */

static char header[LSTWIDTH+1]; /* Constructed header string */

static lmTyp lsttyp; /* Requested listing type */
static lmSev lstsev; /* and severities */

static LMBOOL liston = TRUE;	/* Is listing turned on now? */
static LMBOOL pageSkipped = FALSE;

static char *lihdr;		/* The list header insert string */

/* Sort part of message */
typedef struct sortRec {
  Srcp pos;		/* Source position */
  lmSev sev;	/* Severity code */
  int ref;			/* Reference to MSGREC record */
} sortRec;

/* Data part of message */
typedef struct msgRec {
  int code;			/* Error code */
  Srcp start;		/* Possible start position (for PUSH) */
  char *insert;			/* Insert string(s) */
} msgRec;

static lmSev maxsev;		/* Highest severity so far */
static lmSev maxlocsev;	/* Highest local severity so far */

static struct msgRec *mdarr;    /* Message data array */
static int marrEntries;
static struct sortRec *msarr;   /* Message sort array */

/**********************
 *                    *
 * Listing Parameters *
 *                    *
 **********************/

static int paglen;		/* Page length and */
static int pagwdt;		/* ... width */

static int pagnum;		/* Current page number */
static int plnum;		/* Current page line number */


/*******************
 *                 *
 * Static routines *
 *                 *
 *******************/

#define inset(x,y)(x&y)
static void prlin(char str[], LMBOOL cont, LMBOOL wrdwrp, int indent);



/*----------------------------------------------------------------------

   error()

   Internal or usage error. Print a message.

 */
static void error(
     char str[]			/* IN - the error message */
) {
  printf("\n***** lmList - %s\n", str);
}



/*----------------------------------------------------------------------

   sortmsg()

   Sort error messages in msarr[]

 */
static void sortmsg(void)
{
  int i;
  LMBOOL swap, ready;
  struct sortRec temp;		/* Temporary storage for sort record... */
  int f1, f2;			/* ... files, ... */
  int l1, l2;			/* ... lines */
  int c1, c2;			/* ... columns */
  int n1, n2;			/* Message reference numbers, ie. log order */
  
  /* Sort the error messages */
  ready = FALSE;
  while (!ready) {
    ready = TRUE;
    f1 = msarr[0].pos.file;
    l1 = msarr[0].pos.line;
    c1 = msarr[0].pos.col;
    n1 = msarr[0].ref;
    for(i = 0; i < (count.msgs - 1); i++) {
      f2 = msarr[i+1].pos.file;
      l2 = msarr[i+1].pos.line;
      c2 = msarr[i+1].pos.col;
      n2 = msarr[i+1].ref;	/* Use log order as last component */
      if (f2 != f1)
	swap = (f2 <= f1);
      else
      if (l2 != l1)
	swap = (l2 <= l1);
      else 
      if (c2 != c1)
	swap = (c2 <= c1);
      else
      swap = (n2 < n1);
      if (swap) {
	temp = msarr[i];
	msarr[i] = msarr[i+1];
	msarr[i+1] = temp;
	ready = FALSE;
      }
      f1 = f2;
      l1 = l2;
      c1 = c2;
      n1 = n2;
    }
  }
}



/*----------------------------------------------------------------------

   getmsg()

   Get a message template from the ERRMSG file.

 */
static void getmsg(
     int i,			/* IN - error/message code */
     char mstr[]		/* OUT - the message for that code */
)
{
  int msgIx;		/* Message array index */
  char msgId[16];	/* Requested message identity */

  sprintf(msgId, "%-6d", i);
  for (msgIx = msects[currMsect].offs; ; msgIx++) {
    if (msg[msgIx][0] == NULL || 
        msgIx >= msects[currMsect].offs + msects[currMsect].len) {
      sprintf(mstr, "<< Lister: No message for code %d found >>", i);
      return;
    }

    if (strcmp(msg[msgIx][0],msgId)==0) break;
  }
    
  /* Copy message to caller */
  strcpy(mstr, msg[msgIx][1]);
}



/*----------------------------------------------------------------------

   insert()

   Insert the insert string/s into textual message.

 */
static void insert(
     char istr[],		/* IN - insert string/s */
     char mstr[]		/* INOUT - the textual message */
)
{
  char *rb;			/* Result buffer */
  char *s;			/* Insertion string pointer */
  int rbidx = 0;		/* Result buffer index */
  int msidx = 0;		/* Textual message (mstr) index */
  int iptr;

  /* Allocate temporary buffer */
  if ((rb = (char *) malloc(1000)) == NULL) {
    error("Out of memory.");
    return;
  }

  /* Copy contents of mstr and istr into rb */
  while(mstr[msidx] != '\0') {
    if (mstr[msidx] == ESCAPECHAR && mstr[msidx+1]) {
      /* escaped character */
      msidx++;
      rb[rbidx++] = mstr[msidx++];
    } else if (mstr[msidx] == '%' && isdigit((int)mstr[msidx+1])) {
      /* insertion string */
      for (msidx++, iptr = 0; isdigit((int)mstr[msidx]); msidx++)
        iptr = iptr * 10 + mstr[msidx] - '0';

      if (iptr > 0) {
        /* find the correct insertion string */
        for (s = istr; iptr > 1 && *s; iptr--) {
          for (; *s && *s != lmSEPARATOR; s++);
          if (*s) s++;
	}

        /* copy insertion string */
        for (; *s && *s != lmSEPARATOR; rb[rbidx++] = *(s++));
      }
    } else {
      /* ordinary character */
      rb[rbidx++] = mstr[msidx++];
    }
  }
  rb[rbidx] = '\0';		/* Null terminate rb[] */
  strcpy(mstr, rb);		/* Copy result to caller */
  free(rb);
}


/*----------------------------------------------------------------------

   crehead()

   Create list header string including date and page number padding

 */
static void crehead(void)
{
  char curtim[50];		/* Current time */
  time_t ticks;			/* Time in seconds */
  int i;			/* Loop */
  
  /* Create first part of header message from ERRMSG info */
  getmsg(0, header);
  insert(lihdr, header);	/* Insert the header insert string */
  strcat(header, " - ");
  
  /* Copy top level source file name if there is room */
  if (strlen(header) + strlen(src[0].fnm) + 2 < pagwdt - 5 -2) {
    strcat(header, src[0].fnm);
    strcat(header, " - ");
  }
  
  /* Get current time */
  time(&ticks);
  { struct tm* t; 
    t = localtime(&ticks); 
    strftime(curtim, (size_t)50, "%Y-%m-%d %H:%M", t); 
  }
  if (strlen(header) + strlen(curtim) < pagwdt - 5 - 2)
    strcat(header, curtim);

  /* Pad w.r.t. page width */
  for (i = (pagwdt - 5 - 2) - strlen(header); i > 0; i--)
    strcat(header, " ");

}



/*----------------------------------------------------------------------

   prhead()

   Print list header.

 */
static void prhead(void)
{
  int i;

  fprintf(out.file, "%s %5d.", header, pagnum);
  for (i = HEADERLINES; i>0; i--)
    fprintf(out.file, "\n");
}



/*----------------------------------------------------------------------

   getsrc()

   Get a source line from current source file.  Returns TRUE if end of
   file.

 */
static LMBOOL getsrc(
     char *sline		/* INOUT - the source line */
)
{
  static char oline[SRCWIDTH+1];	/* Overflow text line */
  LMBOOL eof, tmpeof;		/* EOF indicators */

  eof = !src[srclev].open || (fgets(sline, SRCWIDTH, src[srclev].file) == 0);

  /* Remove NEWLINE at the end of source line */
  if (!eof) {
    /* Was there a newline last? */
    if (sline[strlen(sline)-1] != '\n')
      /* No, so find that or real end of file */
      do {
	/* Don't tell about the EOF yet! */
	tmpeof = (fgets(oline, SRCWIDTH, src[srclev].file) == 0);
      } while (!tmpeof && oline[strlen(oline)-1] != '\n');
    else
      sline[strlen(sline)-1] = '\0';
  } else
    sline[0] = '\0';		/* No more input */
  return(eof);
}



/*----------------------------------------------------------------------

   geterr()

   Get errors for a source line

 */
static void geterr(
     int fil,			/* IN - source file number */
     int line,			/* IN - source line number */
     int *first,		/* OUT - lines first error in msarr[] */
     int *last,			/* OUT - lines last error in msarr[] */
     lmSev *errflg /* OUT - set of severities found in line */
)
{
  /* initialize */
  *errflg = sevNONE; 
  *first=0;
  *last = 0;
  if (count.msgs <= 0)
    return;

  /* First skip files (possibly) and lines with lower number */
  while ((*first < count.msgs) && (msarr[*first].pos.file < fil))
    (*first)++;
  /* at correct file ? */
  if (*first == count.msgs || msarr[*first].pos.file != fil)
    return;

  while ((*first < count.msgs) && (msarr[*first].pos.line < line))
    (*first)++;
  /* at correct line ? */
  if (*first == count.msgs || msarr[*first].pos.line != line)
    return;

  /* Find last error for the line */
  if (*first >= 0) {
    for (*last = *first; (*last < count.msgs)
        && (msarr[*last].pos.file == fil) 
        && (msarr[*last].pos.line == line) 
       ; (*last)++)
    *errflg |= msarr[*last].sev; /* this severity was found */
    (*last)--;
  }
}



/*----------------------------------------------------------------------

   skippage()

   Skip to next page on the list device 

 */
static void skippage(void)
{
  pageSkipped = TRUE;
  if (paglen >= 20) {
    pagnum = pagnum + 1;
    if (pagnum > 1)
      fprintf(out.file,"\f");	/* Form feed */
    if (inset(liHEAD, lsttyp))
      prhead();			/* Output page header ... */
    plnum = HEADERLINES+1;	/* so now at some line on new page */
  }

}



/*----------------------------------------------------------------------

   prlin()

   Print line. Handles wrapping and page feeds.

 */
static void prlin(
     char str[],		/* IN - the string to print out */
     LMBOOL cont,		/* IN - TRUE : equally sized line
				   to follow on the same page */
     LMBOOL wrdwrp,		/* IN - TRUE : wrap after BLANK or COMMA
				   if possible. */
     int indent			/* IN - possible prefix to indent split lines
				   with */
)
{
  static char obuf[LSTWIDTH + 1]; /* Output buffer */
  int nline;			/* Number of sublines */
  int i, wrap;			/* Index and wrap point */
  char omarg[OMARG+1];		/* Output margin string */
  char *indentation;
  int omargLen;
  
  strcpy(omarg, "");		/* Init omarg[] */
  omargLen = 0;
  indentation = (char *)malloc(indent+1); /* and indentation */
  for (i = 0; i < indent; i++) indentation[i] = ' ';
  indentation[indent] = '\0';
  indentation[0] = '\0';	/* No indent first line */

  /* Get number of lines needed */
  nline = (strlen(str)/(pagwdt-OMARG)) + 1;
  
  /* Do they fit on this page ? */
  if (cont)			/* Line to come ? */
    nline = nline * 2;
  if (plnum + nline > paglen)	/* End of page? */
    skippage();			/* Yes - skip to next page */

  /* First line should always start at beginning of line */
  /* Trailing lines should be OMARG shorter and start at OMARG */
  i = 0;
  omargLen = 0;
  do {
    if (strlen(&str[i]) > pagwdt-omargLen) {
      wrap = pagwdt - omargLen;	/* Set default wrap point */
      if (wrdwrp)		/* Try to find a space before 20 chars*/
	while(str[i+wrap] != ' ' && str[i+wrap] != ',' && wrap > 20)
	  wrap--;
	if (wrap == 20)		/* else use default wrap point */
	  wrap = pagwdt - omargLen;
    } else
      wrap = strlen(&str[i]);
    strncpy(obuf, &str[i], (size_t)wrap); /* Copy the string */
    obuf[wrap] = '\0';		/* Terminate it */
    fprintf(out.file, "%s%s%s\n", omarg, indentation, obuf);
    plnum++;			/* Increment number of lines on this page */
    strcpy(omarg, "        ");	/* Start next line with a margin */
    if (indent > 0)
        indentation[0] = ' ';	/* And any possible indent */
    omargLen = OMARG + indent;
    i = i + wrap;		/* Move the input string pointer */
    if (wrdwrp && str[i] != '\0')
      if (str[i] == ' ') i++;
  } while (str[i] != '\0');

  free(indentation);
}


/*----------------------------------------------------------------------

  prfnm()

  Print the name of an include file as a kind of header.

  */
static void prfnm(void)
{
  lmSkipLines(6);
  prlin("", FALSE, FALSE, 0);
  prlin("", FALSE, FALSE, 0);
  prlin(src[srclev].fnm, FALSE, FALSE, 0);
  prlin("", FALSE, FALSE, 0);
  src[srclev].printed = TRUE;
}  


/*----------------------------------------------------------------------

   prsrcl()

   Print a source line including line number

 */
static void prsrcl(
     int sln,			/* IN - Source line number */
     char slstr[],		/* IN - Source line string */
     lmSev errflg 		/* IN - Set of errors on this line */
)
{
  static char lbuf[SRCWIDTH + OMARG]; /* Line buffer */
  
  if (srclev == 0)
    sprintf(lbuf,"%5d.  %s", sln, slstr);
  else
    sprintf(lbuf,"%5d.%1d %s", sln, srclev, slstr);
  
  /* Possibly output source if requested */
  if (inset(errflg, lstsev)) {
    /* There is a message in this line that we want to show */
    if (inset(liMSG, lsttyp)) {
      if (!pageSkipped)
	skippage();			/* Skip list to next page */
      if (!src[srclev].printed)
	prfnm();
      prlin(lbuf, TRUE, FALSE, 0);	/* Error line to follow */
    }
  } else {
    /* No interesting message on this line, show it anyway? */
    if (inset(liOK, lsttyp)) {
      if (!pageSkipped)
	skippage();			/* Skip list to next page */
      if (!src[srclev].printed)
	prfnm();
      prlin(lbuf, FALSE, FALSE, 0); /* Only line */
    }
  }
  
}



/*----------------------------------------------------------------------

  liFormatMsg()

  Gets the message text for a message with a specified index in
  msarr[]. NOTE the difference to lmMsg()!!

 */
static void liFormatMsg(
     int i,			/* IN - Message index >= 0 */
     char msgstr[]		/* OUT - Formatted message */
)
{
  char svchar;			/* Severity character */
  char errstr[MSGWIDTH+1]; /* Room for ERRMSG string */
  int mdidx;			/* Index for mdarr[] */
  


  if (phase == PH_COLL)
      phase = PH_RETR;

  if (count.msgs == 0)
      return;

  /* Check if message index out of range */
  if (i < 0 || i >= count.msgs) {
    error("liFormatMsg(): Message index out of range.");
    strcpy(msgstr, "Message index out of range.");
    return;
  }

  /* Create textual message prefix */
  switch (msarr[i].sev) {
  case sevOK:  svchar = 'O'; break;
  case sevINF: svchar = 'I'; break;
  case sevWAR: svchar = 'W'; break;
  case sevERR: svchar = 'E'; break;
  case sevFAT: svchar = 'F'; break;
  case sevSYS: svchar = 'S'; break;
  default:     svchar = '?'; break;
  }
  
  /* Find index in mdarr[] */
  mdidx = msarr[i].ref;		/* 1 indirection */
  
  /* Format text */
  sprintf(msgstr, "%d %c : ", mdarr[mdidx].code, svchar);
  
  /* Get textual message from ERRMSG */
  getmsg(mdarr[mdidx].code, errstr);

  /* Add the message text, and expand it */
  strcat(msgstr, errstr);
  if (mdarr[mdidx].insert != NULL)
    insert(mdarr[mdidx].insert, msgstr);
}




/*----------------------------------------------------------------------

   prerrm()

   Print column markers and error messages for a source line.

 */
static void prerrm(
     int first,			/* IN - First error to mark */
     int last,			/* IN - Last error to mark */
     char src[]			/* IN - Source line */
)
{
  static char line[LSTWIDTH+1];	/* Output line buffer */
  char *msg;			/* Message line buffer */
  int *msgnum;			/* Message number for each message (malloc) */
  int number;			/* Error number */
  int msgIndex;			/* Message index */
  int numberIndex;		/* Message number index */
  int outIndex;			/* line[] index */
  int preCol;			/* Previous column marked */
  int i;			/* Loop */
  LMBOOL anymsg;		/* TRUE : at least 1 message printed */
  int indent;

  number = 0;
  outIndex = 0;
  preCol = -1;			/* No prev column */
  numberIndex = 0;

  if (!pageSkipped)
      skippage();			/* Skip list to next page */

  /* Allocate message number array */
  msgnum = (int *)malloc(sizeof(int)*(last-first+1));

  strcpy(line, "=====>   ");	/* Init. line[] */

  /* For all error messages */
  for (msgIndex = first; msgIndex <= last; msgIndex++) {
    /* First skip over unwanted and internal messages */
    while ((!inset(msarr[msgIndex].sev, lstsev) ||
	    msarr[msgIndex].sev > sevSYS) && (msgIndex <= last))
      msgIndex++;
    if (msgIndex > last)
      break;

    /* Pad with SPACE or TAB to marker column (if it wasn't 0) */
    if (msarr[msgIndex].pos.col != 0) {
      while (outIndex < msarr[msgIndex].pos.col-1) {
	if (outIndex >= SRCWIDTH) {
	  line[outIndex] = '\0';
	  break;
	}
	if (outIndex != preCol)	{ /* Don't overwrite a previous marker */
	  /* If source contained TAB, pad with that instead */
	  if (src[outIndex] == '\t')
	    line[OMARG+outIndex] = '\t';
	  else
	    line[OMARG+outIndex] = ' ';
	}
	outIndex = outIndex + 1;		/* Next column */
      }
      
      /* Set a new marker if not at the same column */
      if (outIndex != preCol) {
	/* Update error number */
	if (number < 9)
	  number++;
	line[OMARG+outIndex] = (number + '0'); /* Convert to digit */
	preCol = outIndex;
      }
    }
    /* Remember which marker number this message had */
    if(msarr[msgIndex].pos.col == 0)
      msgnum[numberIndex] = 0;
    else
      msgnum[numberIndex] = number;
    numberIndex = numberIndex + 1;
  }
  line[OMARG+outIndex+1] = '\0'; /* Terminate line[] */
  
  /* Print column marker line - if it's not empty */
  if (number > 0)
    prlin(line, FALSE, FALSE, 0);
  
  /* Print empty line */
  prlin("", FALSE, FALSE, 0);
  
  /* Print the error messages */
  anymsg = FALSE;		/* To avoid double LF */
  msgIndex = first;
  for (i = 0; i < numberIndex; i++) {
    anymsg = TRUE;
    if (msgnum[i] == 0)
      /* Prefix for unnumbered message */
      strcpy(line, "        ");
    else
      /* Prefix for numbered message */
      sprintf(line, "  *%d*   ", msgnum[i]);
    indent = strlen(line);	/* Calculate indent for wrapping lines */

    /* Again skip unwanted messages */
    while ((!inset(msarr[msgIndex].sev, lstsev)
	    || msarr[msgIndex].sev >= sevPUSH
	    ) && (msgIndex <= last))
      msgIndex++;
    
    /* Get formatted message text */
    msg = (char *) malloc((size_t)MSGWIDTH +
			  (mdarr[msarr[msgIndex].ref].insert?
			   strlen(mdarr[msarr[msgIndex].ref].insert): 0));
    if (!msg)
      error("Out of memory!");
    else {
      liFormatMsg(msgIndex, msg);

      /* Merge prefix and textual message */
      strcat(line, msg);
    
      /* Print the message, wrap between words if necessary */
      prlin(line, FALSE, TRUE, indent);
      msgIndex = msgIndex + 1;	/* Next message */
      free(msg);
    }
  }
  
  /* Print empty line - if any message was printed */
  if (anymsg)
    prlin("", FALSE, FALSE, 0);

  free((char *)msgnum);
}



/*----------------------------------------------------------------------

  liOpenOutput()

  Open the output file.

 */
static void liOpenOutput(
     char ofnm[]		/* IN - Output file name string */
)
{
  /* Open the output file/device, but first check if previously open */
  if (out.open && strcmp(out.name, "") != 0)
    fclose(out.file);
  out.name = ofnm;
  if (strcmp(out.name, "") == 0) /* Output to standard output */
    out.file = stdout;
  else {
    out.file = fopen(out.name, "w");
    if (out.file == NULL) {	/* Couldn't open list file */
      out.file = stdout;	/* So list on standard output */
      out.name = NULL;		/* Remember! */
    }
  }
  out.open = (out.file != NULL);
}


/*----------------------------------------------------------------------

  liOpenSrc()

  Open a source file. Level will be srclev.

 */
static void liOpenSrc(
     char srcfnm[],		/* IN - Source file name string */
     int fno			/* IN - File number for this file */
)
{
  src[srclev].fnm = srcfnm;
  src[srclev].printed = FALSE;	/* Not shown yet */
  src[srclev].file = fopen(srcfnm, "r"); /* Open it */
  src[srclev].open = (src[srclev].file != NULL); /* OK? */
  src[srclev].fno = fno;	/* Set file number */
  src[srclev].lno = 0;
  src[srclev].mno = 0;
}


/*----------------------------------------------------------------------

   liPush()

   Internal function to push to an include file. 

 */
static void liPush(
     int first,			/* IN - first ... */
     int last			/* IN - ... and last error msgs */
)
{
  int i;			/* Scratch error number */
  static char srcline[SRCWIDTH]; /* Source line buffer for skipping */

  /* extend src dynamic storage */
  if (srclev == srcEntries-1) {
    srcEntries += INCINCLUDE;
    src = (Srctyp *) realloc((char *) src, (size_t)sizeof(Srctyp) * srcEntries);
  }

  /* Find any previous push made from this line */
  if (src[srclev].mno != 0)
    first = src[srclev].mno;	/* Start at next message */

  /* Then find next PUSH message */
  for (i = first; i <= last; i++)
    if (msarr[i].sev == sevPUSH)
      break;

  /* Found any more PUSH? */
  if (i > last) {
    /* No, so stay in the previous file! */
    src[srclev].mno = 0;
    return;
  }

  /* Remember which one to start at next */
  src[srclev].mno = i+1;

  /* Index into mdarr! */
  i = msarr[i].ref;

  /* Push one level */
  srclev++;
  
  /* Open the source file */
  liOpenSrc(mdarr[i].insert, mdarr[i].code);

  /* Next line read is line 1 */
  src[srclev].lno = 1;

  /* Start line at line 0 => line 1 */
  if (mdarr[i].start.line == 0)
    mdarr[i].start.line = 1;

  /* If not start at first position, find start position */
  while (src[srclev].lno < mdarr[i].start.line) {
    (void) getsrc(srcline);
    src[srclev].lno++;		/* Increment line number */
  }
}



/*----------------------------------------------------------------------

   liPop()

   Pop the level of source files. If no more return TRUE (for actual
   end of file).

 */
static LMBOOL liPop(void) {
  int lev;			/* Level loop variable */

  if (srclev == 0)
    return(TRUE);
  else {
    if (src[srclev].open) {
      fclose(src[srclev].file);
      src[srclev].open = FALSE;
    }

    /* If the file name was printed then we have shown something from */
    /* the file so make an empty line and make sure previous file */
    /* names are shown again when printed */
    if (src[srclev].printed) {
      prlin("", FALSE, FALSE, 0);	/* One empty line after shown file */
      for (lev = srclev; lev >= 0; lev--)
	  src[lev].printed = FALSE; /* Reset printed names on files below */
    }
    srclev--;			/* Pop one level */
    src[srclev].lno--;		/* Rewind one line */
    return(FALSE);
  }

}


/*----------------------------------------------------------------------

   prpack()

   Print a source package (= source line and error messages).

 */
static void prpack(
    LMBOOL *eof			/* OUT - TRUE if eof */
) {
  int first;			/* First error index for this line */
  int last;			/* Last error index for this line */
  int i;			/* Counter index */
  lmSev msgflg; /* Set of msg severities found */
  static char sline[SRCWIDTH+1]; /* Source line buffer */
 

  src[srclev].lno++;	/* Increment source line number */

  /* Any messages for this line ? */
  geterr( src[srclev].fno, 
	 src[srclev].lno, &first, &last, &msgflg);
  
  /* PUSH or POP from this line */
  if (inset(msgflg, sevPUSH)) {
    liPush(first, last);
    geterr( src[srclev].fno, 
	   src[srclev].lno, &first, &last, &msgflg);
  }
  if (inset(msgflg, sevPOP)) {
    *eof = liPop();
    return;
  }

  /* Check for PAGE, LISTON, LISTOFF */
  if (inset(msgflg, sevOFF))
    liston = FALSE;
  if (inset(msgflg, sevON))
    liston = TRUE;
  if (inset(msgflg, sevPAGE)) {
    for (i= first; i <= last; i++)
      if (msarr[i].sev == sevPAGE)
	break;
    lmSkipLines(mdarr[msarr[i].ref].code); /* code contains #lines */
  }
    

  /* Now we can remove any possible internal error flags */
  msgflg &= sevALL;
  
  /* Read next source line */
  *eof = getsrc(sline);

  /* Print source line if requested */
  if (liston || inset(msgflg, lstsev))
    prsrcl(src[srclev].lno, sline, msgflg);
  
  /* If line contained wanted errors... */
  if (inset(msgflg, lstsev))
    /* Print column markers and error messages */
    prerrm(first, last, sline);

}


/*----------------------------------------------------------------------

   prupack()

   Print the end package (the messages for the top level file but
   without any line number).

 */
static void prupack(void)
{
  int first, last;
  lmSev errflg;

  geterr( src[0].fno,  0, &first, &last, &errflg);
  if (inset(errflg, lstsev)) {
    prlin("", FALSE, FALSE, 0);	/* Empty line before trail. errors */
    prerrm(first, last, "");
  }
}

/*----------------------------------------------------------------------

   prgpack()

   Print the global package (the messages with no source position)

 */
static void prgpack(void)
{
  int first, last;
  lmSev errflg;

  geterr(-1, 0, &first, &last, &errflg);
  if (inset(errflg, lstsev)) {
    prlin("", FALSE, FALSE, 0);	/* Empty lines before global errors */
    prlin("", FALSE, FALSE, 0);
    prerrm(first, last, "");
  }
}



/*----------------------------------------------------------------------

   prepi()

   Print epilogue.

 */
static void prepi(void)
{
  static char sbuf[SRCWIDTH+1];	/* String buffer */
  char nstr[20];		/* Number string */
  
  if (!inset(liSUM, lsttyp))
    return;
  
  prlin("", FALSE, FALSE, 0);
  
  if ((count.warns + count.errs) == 0) {
    getmsg(1, sbuf);
    prlin(sbuf, FALSE, FALSE, 0);
  } else {
    if (count.errs != 0) {
      getmsg(6, sbuf);
      sprintf(nstr, "%d", count.errs);
      insert(nstr, sbuf);	/* Insert the number */
      prlin(sbuf, FALSE, FALSE, 0);
    } else {
      getmsg(3, sbuf);
      prlin(sbuf, FALSE, FALSE, 0);
    }
    if (count.warns != 0) {
      getmsg(5, sbuf);
      sprintf(nstr, "%d", count.warns);
      insert(nstr, sbuf);	/* Insert the number */
      prlin(sbuf, FALSE, FALSE, 0);
    } else {
      getmsg(2, sbuf);
      prlin(sbuf, FALSE, FALSE, 0);
    }
  }
  if (count.infos != 0) {	/* Any informational messages? */
    getmsg(4, sbuf);
    sprintf(nstr, "%d", count.infos);
    insert(nstr, sbuf);		/* Insert the number */
    prlin(sbuf, FALSE, FALSE, 0);
  }
  
  prlin("", FALSE, FALSE, 0);

}



/*----------------------------------------------------------------------

   liLog()

   Internal routine for logging a message. Takes care of special format
   messages like PUSH and POP messages.
 */
static void liLog(
     Srcp *srcp,	/* IN - source position for message */
     Srcp *start,	/* IN - srcp for possible PUSH message */
     int ecode,			/* IN - error code to log */
     lmSev sev,	/* IN - severity to log */
     char *istrs		/* IN - concatenated insert strings */
) {
  /* Room for more messages ? */
  if (count.msgs > MAXMSG)
    return;

  /* extend mdarr & msarr dynamic storage */
  if (count.msgs == marrEntries) {
    marrEntries += INCMSG;
    mdarr = (msgRec *) realloc((char *) mdarr, (size_t)sizeof(msgRec) * marrEntries);
    msarr = (sortRec *) realloc((char *) msarr, (size_t)sizeof(sortRec) * marrEntries);
  }
  
  /* Store the message data */
  mdarr[count.msgs].code = ecode; /* store error code */

  /* If a start-srcp was given, store it */
  if (start != NULL)
    mdarr[count.msgs].start = *start;

  /* Allocate dynamic string storage */
  if (istrs != NULL) {
    mdarr[count.msgs].insert = (char *) malloc((size_t)strlen(istrs)+1);
    if (!mdarr[count.msgs].insert) {
      error("Out of memory!");
      mdarr[count.msgs].insert = lmNoIns; 
    } else
      strcpy(mdarr[count.msgs].insert, istrs); /* Store insert string/s */
  } else
    mdarr[count.msgs].insert = NULL;
  
  /* Update sort record */
  if (srcp != NULL)
    msarr[count.msgs].pos = *srcp; /* Store source position */
  else
    msarr[count.msgs].pos = nulpos; /* Use zero source position */
  msarr[count.msgs].ref = count.msgs; /* Reference to mdarr[] */
  msarr[count.msgs].sev = sev;	/* Store severity code */
  
  count.msgs++;
  
  /* Close error collection ? */
  if (count.msgs == MAXMSG)
    liLog(&nulpos, NULL, 7, sevWAR, 0);
}


/*----------------------------------------------------------------------

  liInit()

  Initialise for listing production.

 */
static void liInit(
     lmTyp typ,	/* IN - Listing type */
     lmSev sevs,	/* IN - Severities to include */
     int lins,			/* IN - Lines per page */
     int cols			/* IN - Columns per line */
)
{
  /* Init values */
  lsttyp = typ;			/* Listing type */
  lstsev = sevs;		/* and severities */
  paglen = lins;		/* Lines per page */
  if (cols == 0)		/* and columns */
    pagwdt = 80;
  else if (cols < 60)
    pagwdt = 60;
  else if (cols > LSTWIDTH)
    pagwdt = LSTWIDTH;
  else
    pagwdt = cols;
  pagnum = 0;			/* Current page number = 0 */
}



/*======================================================================

   lmLiInit()

   Init the Lister module

 */
void lmLiInit(
     char header[],		/* IN - String to be inserted in header */
     char srcf[],		/* IN - Name of top level source file */
     lmMessages msect	/* IN - Message sector */
) {
  /* Create dynamic storage structures */
  src = (Srctyp *) malloc((size_t)sizeof(Srctyp) * INITINCLUDE);
  srcEntries = INITINCLUDE;
  mdarr = (msgRec *) malloc((size_t)sizeof(msgRec) * INITMSG);
  msarr = (sortRec *) malloc((size_t)sizeof(sortRec) * INITMSG);
  marrEntries = INITMSG;
  
  /* Save name of top level source and message file */
  src[0].fnm = (char *) malloc((size_t)strlen(srcf)+1);
  strcpy(src[0].fnm, srcf);
  currMsect = msect;
  
  /* Save header insert string */
  lihdr = (char *) malloc((size_t)strlen(header)+1);
  strcpy(lihdr, header);
  
  /* Init static variables etc. */
  count.msgs = 0;		/* number of messages so far is 0 */
  count.errs = 0;		/* number of errors so far is 0 */
  count.warns = 0;		/* number of warnings so far is 0 */
  count.infos = 0;		/* number of infos so far is 0 */
  phase = PH_COLL;		/* Message collection phase */
  maxsev = sevOK;		/* Highest severity is OK!! */
  maxlocsev = sevOK;		/* Highest local severity is OK!! */
  /* Initialize file field of nulpos */
  nulpos.file = -1;

}



/*======================================================================

   lmLog()

   Log an error message, insert strings separatated using %(lmkPrefix)separator

 */
void lmLog(
    Srcp *pos,		/* IN - Source position */
    int ecode,				/* IN - The error code */
    lmSev sev,		/* IN - Severity code */
    char istrs[]			/* IN - Insert strings */
) {
    Srcp srcpos, *srcp = &srcpos;	/* Source position copy */
    
  /* Check phase */
  if (phase != PH_COLL) {
    error("lmLog(): Phase error.");
    return;
  }
  
  if (pos != NULL) {
    /* check source position */
    srcpos = *pos;
    if (srcp->col < 0) {
      error("lmLog(): Source position column < 0.");
      srcp->col = 0;
    }
    if (srcp->line < 0) {
      error("lmLog(): Source position line < 0.");
      srcp->line = 0;
    }
    if (srcp->line == 0) {
      srcp->col = 0;
    }
  } else
    srcp = NULL;

  /* Update highest severity unless it was an internal severity code */
  if(sev > maxsev && sev <= sevSYS)
    maxsev = sev;
  if(sev > maxlocsev && sev <= sevSYS)
    maxlocsev = sev;
  
  /* Update warning and error counters */
  switch (sev) {
  case sevOK:  break;
  case sevINF: count.infos++; break;
  case sevWAR: count.warns++; break;
  case sevERR:
  case sevFAT:
  case sevSYS: count.errs++; break;
  }
  
  /* Log the message */
  liLog(srcp, NULL, ecode, sev, istrs);

}



/*======================================================================

   lmLogv()

   Log an error message, insert strings using va_arg

 */
void lmLogv(
    Srcp *pos,		/* IN - Source position */
    int ecode,			/* IN - The error code */
    lmSev sev,	/* IN - Severity code */
    ...
) {
  char *errorstring = (char *)malloc((size_t)256);
  int curlen = 256;
  char *arg, *tmp;
  int len = 0;
  va_list ap;

  /* Check errorstring */
  if (!errorstring) {
    error("Out of memory!");
    return;
  }

  /* Check phase */
  if (phase != PH_COLL) {
    error("lmLog(): Phase error.");
    return;
  }

  va_start(ap, sev);
  errorstring[0] = '\0';
  while ((arg = va_arg(ap, char *))) {
    if (len + strlen(arg) >= curlen) {
      tmp = errorstring;
      curlen += 256;
      errorstring = (char *)malloc((size_t)curlen);
      /* Check errorstring */
      if (!errorstring) {
	error("Out of memory!");
	return;
      }
      strcpy(errorstring, tmp);
      free(tmp);
    }
    strcat(errorstring, arg);
    len = strlen(errorstring);
    errorstring[len] = lmSEPARATOR;
    errorstring[len+1] = '\0';
      
  }

  va_end(ap);

  lmLog(pos, ecode, sev, errorstring);
  free(errorstring);
}


/*======================================================================

   lmLiOff()

   Turn listing off at source position.

 */
void lmLiOff(
    Srcp *srcp		/* IN - The first source position to exclude */
) {
    liLog(srcp, NULL, 0, sevOFF, NULL);
}



/*======================================================================

   lmLiOn()

   Turn listing on again (if it was off).

 */
void lmLiOn(
    Srcp *srcp		/* IN - The first source position to include */
) {
    liLog(srcp, NULL, 0, sevON, "");
}



/*======================================================================

   lmLiPage()

   Skip to next page at source position. If 'lins' <> 0 then a form feed is
   only inserted if less that 'lins' lines left on the page.
 */
void lmLiPage(
     Srcp *srcp,	/* IN - Source position */
     int lins			/* IN - Number of lines */
) {
    liLog(srcp, NULL, lins, sevPAGE, "");
}


/*======================================================================

   lmLiEnter()

   Push to an include file. Use error code to store file number,
   severity to indicate a PUSH.

 */
void lmLiEnter(
    Srcp *srcp,		/* IN - Where to include the file */
    Srcp *start,	/* IN - First position to include from file */
    char fnm[]			/* IN - File name of file to include */
) {
    liLog(srcp, start, start->file, sevPUSH, fnm);
}


/*======================================================================

   lmLiExit()

   Prematurely pop from an include file at specified source position.

 */
void lmLiExit(
    Srcp *srcp		/* IN - Where to return from an include file */
) {
    liLog(srcp, NULL, 0, sevPOP, "");
}




/*======================================================================

   lmSeverity()

   Return highest severity logged so far.

 */
lmSev lmSeverity(
) {
  return (maxsev);
}



/*======================================================================

   lmLocSeverity()

   Return highest local severity logged so far.

 */
lmSev lmLocSeverity(
) {
  return (maxlocsev);
}



/*======================================================================

   lmResLocSeverity()

   Reset local severity.

 */
void lmResLocSeverity(void) {
  maxlocsev = sevOK;
}



/*======================================================================

   lmMsg()

   Get a message with the specified number. Returns 0 if not found.
   This is the public version of liFormatMsg(), but uses numbers of
   actual user defined messages instead of indices in m?arr[].


 */
int lmMsg(
     int msgno,			/* IN - Message number >= 1 */
     Srcp *srcp,	/* OUT - Source position */
     char msgstr[]		/* OUT - Formatted message */
)
{
  int number;			/* Counting user messages */
  int index;			/* Index in msarr[] */

  /* Check phase */
  switch (phase) {
  case PH_NONE:
      error("lmMsg(): Phase error.");
      return(0);
  case PH_COLL:
      /* Sort messages, let liFormatMsg() alter phase and open msg file */
      sortmsg();
      liFormatMsg(0, msgstr);
      break;
  case PH_RETR:
      break;
  }

  /* Find message index from number */
  number = 1;
  for (index = 0; index < count.msgs; index++) {
      if (msarr[index].sev <= sevSYS)
	  /* A user message */
	  if (number++ == msgno) {	/* Found the requested message! */
	      /* Return source position */
	      *srcp = msarr[index].pos;
	      /* Format the message text into the user area */
	      liFormatMsg(index, msgstr);
	      return(msgno);
	  }
  }

  return(0);			/* Didn't find it! */
}


/*======================================================================

   lmList()

   Create listing file.

 */
void lmList(
     char ofnm[],		/* IN - Output file name string */
     int lins,			/* IN - Lines per page */
     int cols,			/* IN - Columns per line */
     lmTyp typ,	/* IN - Listing type */
     lmSev sevs	/* IN - Severities to include */
)
{
  char mess[MSGWIDTH+1];
  LMBOOL seof;			/* TRUE - source end of file reached */
  
  /* First check phase */
  if (phase == PH_NONE) {
    error("lmList(): Phase error.");
    return;
  }
  
  liInit(typ, sevs, lins, cols);
  liOpenOutput(ofnm);

  /* Open the source file */
  srclev = 0;
  liOpenSrc(src[0].fnm, 0);
  
  /* Sort messages, let liFormatMsg() alter phase and open msg file */
  sortmsg();
  liFormatMsg(0, mess);
  
  crehead();			/* Create list header string */
  skippage();			/* Skip list to next page */
  
  /* Until end of source file */
  seof = !src[0].open;
  while (!seof) {
    /* Print next package, i.e. source line + error messages */
    prpack(&seof);
    if (seof)			/* End of this file? */
      seof = liPop();		/* Try surrounding ... */
  }
  prupack();			/* Print unnumbered messages */
  
  /* Output the global messages */
  prgpack();

  /* Print epilogue */
  prepi();
  
  /* Close source file */
  if (src[0].open)
    fclose(src[0].file);
}






/*======================================================================

  lmLiPrint()

  Print one supplimentary line in the output file.

  */
void lmLiPrint(
    char str[]			/* IN - the string to print */
) {
  if (phase != PH_RETR)
    error("lmPrint(): Phase error.");
  else
    prlin(str, FALSE, FALSE, 0);
}



/*======================================================================
  lmSkipLines()

  Skip to a new page if not enough number of lines left on the page.

  */
void lmSkipLines(
     int lins			/* IN - minimum number of lines left */
) {
  if (phase != PH_RETR)
    error("lmSkipLines(): Phase error.");
  else if ((lins == 0) || (plnum + lins > paglen+HEADERLINES))
    skippage();
}



/*======================================================================
  lmLiTerminate()

  Clean up after one session.

  */
void lmLiTerminate(
) {
  int i;
  
  /* Free all allocated string areas */
  if (src[0].fnm != NULL)
    free(src[0].fnm);
  free(src);
  
  for (i=0; i < count.msgs; i++)
    if (mdarr[i].insert != lmNoIns && mdarr[i].insert != NULL)
      free(mdarr[i].insert);
  free(mdarr);
  free(msarr);

  free(lihdr);
  
  /* Close output file if it wasn't the terminal */
  if (out.open && out.name && strcmp(out.name, "") != 0)
    fclose(out.file);	
  out.open = FALSE;
  
  phase = PH_NONE;
}

