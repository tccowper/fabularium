/*----------------------------------------------------------------------*\

  util.c

  Alan compiler utilities unit

\*----------------------------------------------------------------------*/

#include "util.h"


/* IMPORTS */
#include "alan.h"
#include "alan.version.h"
#include "sysdep.h"
#include "lmList.h"
#include "smScan.h"
#include "options.h"
#include "srcp_x.h"

#include <setjmp.h>


/* PUBLIC DATA */

Bool verboseFlag;		/* Verbose output */
long counter;			/* And counter for verbose mode */
long allocated;			/* Calculated memory usage */


/* PRIVATE DATA */


/* FUNCTIONS */

/*======================================================================*/
const char *version_string(int buildNumber) {
    static char buf[100];
    sprintf(buf, "Alan - Adventure Language Compiler, version %s", alan.version.string);
    if (buildNumber != 0) sprintf(&buf[strlen(buf)], "-%d", buildNumber);
    sprintf(&buf[strlen(buf)], " (%s %s)", alan.date, alan.time);
    return buf;
}


/*======================================================================*/
void progressCounter() {
  if (verboseFlag) {
    printf("%8ld\b\b\b\b\b\b\b\b", counter++);
    fflush(stdout);
  }
}


/*----------------------------------------------------------------------*/
static char *spaces(int length) {
    static char *string = "                                                                             ";

    return &string[strlen(string)-length];
}


/*======================================================================*/
void verbose(char *msg) {
    if (verboseFlag)
        printf("\n\t%s:%s", msg, spaces(30-strlen(msg)));
}


/*======================================================================*/
void *allocate(int length_in_bytes)		/* IN - Length to allocate */
{
  void *p = calloc(1, (size_t)length_in_bytes);

  if (p == NULL)
    panic("Out of memory");

  allocated += length_in_bytes;

  return p;
}


/*======================================================================*/
void deallocate(void *memory)
{
    free(memory);
}


/*======================================================================

  unimpl()

  An unimplemented constrution was encountered.

 */
void unimpl(Srcp srcp,		/* IN  - Where? */
        char *phase)	/* IN  - What phase? */
{
  lmLog(&srcp, 998, sevWAR, phase);
}



/*----------------------------------------------------------------------

  Find out whether error message is of one of the severities
  being printed.

*/
static int test_severity(char *err, lmSev sevs)
{
  /* Check if the severity was among the wanted ones */
  char c;
  lmSev sev = sevSYS;

  sscanf(err, "%*d %c", &c);
  switch (c) {
  case 'O': sev = sevOK;  break;
  case 'I': sev = sevINF; break;
  case 'W': sev = sevWAR; break;
  case 'E': sev = sevERR; break;
  case 'F': sev = sevFAT; break;
  case 'S': sev = sevSYS; break;
  default: SYSERR("Unexpected severity marker", nulsrcp);
  }
  return sev & sevs;
}


/*======================================================================*/
char *fileName(int fileNo) {
    static List nofile;
    List *fnm;
    int j;

    nofile.member.str = "<no file>";

    /* Advance to the correct file name */
    if (fileNo == -1)
        fnm = &nofile;
    else
        for (fnm = fileNames, j = 0; j<fileNo-1; j++)
            if (fnm != NULL) {
                fnm = fnm->next;
            }
    if (fnm == NULL)
        fnm = &nofile;
    return fnm->member.str;
}


/*----------------------------------------------------------------------*/
static void specialListing(lmSev sevs)
{
    int i,j;
    char err[1024], line[1024];
    Srcp srcp;
    List *fnm;
    List nofile;

    nofile.member.str = "<no file>";
    for (i = 1; lmMsg(i, &srcp, err); i++) {
        if (test_severity(err, sevs)) {
            /* Advance to the correct file name */
            if (srcp.file == -1)
                fnm = &nofile;
            else
                for (fnm = fileNames, j = 0; j < srcp.file; j++)
                    if (fnm != NULL)
                        fnm = fnm->next;
            if (fnm == NULL)
                fnm = &nofile;
            if (ccFlag)
                sprintf(line, "\"%s\", line %d(%d): %s\n",
                        fnm->member.str, srcp.line, srcp.col, err);
            else if (ideFlag)
                sprintf(line, "\"%s\", line %d %d-%d: %s\n",
                        fnm->member.str, srcp.line, srcp.startpos, srcp.endpos, err);
            else
                sprintf(line, "\"%s\", line %d:%d: ALAN-%s (column %d)\n",
                        fnm->member.str, srcp.line, srcp.col, err, srcp.col);

#ifdef __mac__
            lmLiPrint(line);
#else
            printf("%s", line);
#endif
        }
    }
}


#ifdef __mac__
/*----------------------------------------------------------------------

  Write listing and/or error messages to screen or file

*/
static void listing(lmSev sevs)
{
  char *fnm;

  if (lstflg)
    fnm = lstfnm;
  else
    fnm = "";

  listing(fnm, lcount, ccount, fulflg?liFULL:liTINY, sevs);

  if (dmpflg) {
    lmSkipLines(0);
    duadv(dmpflg);
  }

  if (sumflg) {
    if (lmSeverity() < sevERR)
      summary();
    endtotal();			/* Stop timer */
    prtimes();
    stats();
  }
  lmLiTerminate();
}
#else

/*----------------------------------------------------------------------*/
static int get_terminal_columns() {
    return 0;
}

/*======================================================================*/
void createListingOnFile(char *listFileName, int lines, int columns,
                         lmTyp listingType, lmSev severities) {
    if (ccFlag || ideFlag) {
        lmList(listFileName, lines, columns, 0, 0);	/* Sort and prepare for retrieval */
        specialListing(severities);
    } else
        lmList(listFileName, lines, columns, listingType, severities);
}

/*======================================================================*/
void createListingOnScreen(lmTyp listingType, lmSev severities) {
    int lines = 0;
    int columns = get_terminal_columns();

    if (columns == 0) columns = 79;
    createListingOnFile("", lines, columns, listingType, severities);
}

#endif


static void (*handler)(char *) = NULL;

/*======================================================================*/
void setSyserrHandler(void (*f)(char *))
{
  handler = f;
}


/*----------------------------------------------------------------------*/
static char *srcpToString(Srcp srcp) {
    static char *buffer = NULL;
    if (!buffer) buffer = allocate(1000);

    if (srcp.line != 0 && srcp.col != 0)
        sprintf(buffer, " originated from %s:%d(%d)", fileName(srcp.file),
                srcp.line, srcp.col);
    else
        buffer[0] = '\0';
    return buffer;
}


/*======================================================================*/
void syserr(char *errorMessage, Srcp srcp, const char *function, char *file, int line)
{
  int messageLength;
  char *messageString;

  if (handler) {
    handler(errorMessage);
  } else {
    messageLength = strlen(errorMessage) + strlen(function) + strlen(file) + strlen(" in '()', :00000");

    messageString = allocate(messageLength+1);
    sprintf(messageString, "%s in '%s()', %s:%d%s", errorMessage, function, file, line, srcpToString(srcp));

    lmLog(&nulsrcp, 997, sevSYS, messageString);

    createListingOnScreen(liTINY, sevALL);
    terminate(EXIT_FAILURE);
  }
}


/*======================================================================

  panic()

  A catastrophe has happened. Print message but do as little as possible.

  */
void panic(char *str)
{
  printf("PANIC!! %s\n", str);
  terminate(EXIT_FAILURE);
}


/*======================================================================

  terminate()

  Terminate the program with an error code.

 */
#ifdef WINGUI
#include <windows.h>
void terminate(int ecode)
{
  if (guiMode) {
    char *message = "Finished with strange error status!";
    switch (lmSeverity()) {
    case sevOK:
      message = "Finished OK!"; break;
    case sevINF:
      message = "Finished OK with some informational messages."; break;
    case sevWAR:
      message = "Finished OK with warning messages."; break;
    case sevERR:
    case sevFAT:
      message = "Finished with errors."; break;
    }
    MessageBox(NULL, message, "Alan V3 (Development) Compilation Result", MB_OK);
  }
  exit(ecode);
}
#else
void terminate(int ecode)
{
  exit(ecode);
}
#endif


/*======================================================================*/
void strmov(char *to, char *from) {
    int i = 0;
    do {
        to[i] = from[i];
        i++;
    } while (from[i-1] != '\0');
}
