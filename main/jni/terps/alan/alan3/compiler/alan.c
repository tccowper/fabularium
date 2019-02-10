/*----------------------------------------------------------------------*\

  alan.c

  Alan compiler main program unit

\*----------------------------------------------------------------------*/

#include "sysdep.h"
#include "types.h"
#include "util.h"

#ifdef __sun__
#include <unistd.h>
#endif

#include "alan.h"

/* IMPORTS */
#include "alan.version.h"
#include "opt.h"
#include "dump_x.h"
#include "lmList.h"
#include "pmParse.h"
#include "smScan.h"
#include "options.h"

#include "srcp_x.h"
#include "adv_x.h"

#ifdef WINGUI
#include <windows.h>
#endif


/* PUBLIC DATA */
FILE *txtfil;			/* File of collected text data */
FILE *datfil;			/* File of encoded text */

int fileNo = 0;			/* File number to use next */
long counter = 0;		/* Number of new's so far, for verbose */
#ifdef WINGUI
Bool guiMode = FALSE;		/* Using the GUI? Or console? */
#endif


/* PRIVATE */

static void *heap;		/* Address to first free heap area - before */


/* Timing */
#include "timing.h"


static TIBUF tbuf;
static TIBUF totalTime;
static TIBUF compilationTime;

static struct {
  long int pars;
  long int sem;
  long int gen;
  long int comp;
  long int tot;
} tim;



/*----------------------------------------------------------------------*/
static void startTotalTiming(void)
{
  tistart(&totalTime);
}


/*----------------------------------------------------------------------*/
static void startTimingCompilation(void)
{
  tistart(&compilationTime);
}


/*----------------------------------------------------------------------*/
static void startTiming(void)
{
  tistart(&tbuf);
}


/*----------------------------------------------------------------------*/
static void endParseTiming(void)
{
  tistop(&tbuf);
  tim.pars = tbuf.pu_elapsed;
}


/*----------------------------------------------------------------------*/
static void endSemanticsTiming(void)
{
  tistop(&tbuf);
  tim.sem = tbuf.pu_elapsed;
}


/*----------------------------------------------------------------------*/
static void endGenerationTiming(void)
{
  tistop(&tbuf);
  tim.gen = tbuf.pu_elapsed;
}


/*----------------------------------------------------------------------*/
static void endCompilationTiming(void)
{
  tistop(&compilationTime);
  tim.comp = compilationTime.pu_elapsed;
}


/*----------------------------------------------------------------------*/
static void endTotalTiming(void)
{
  tistop(&totalTime);
  tim.tot = totalTime.pu_elapsed;
}


/*----------------------------------------------------------------------*/
static void printTimes(void)
{
  char str[80];
  
  lmSkipLines(8);
  lmLiPrint("");
  lmLiPrint(       "        Timing");
  lmLiPrint(       "        ------");
  (void)sprintf(str,   "        Parse Time:             %6ld", tim.pars);
  lmLiPrint(str);
  if (tim.sem != 0) {
    (void)sprintf(str, "        Analysis Time:          %6ld", tim.sem);
    lmLiPrint(str);
  }
  if (tim.gen != 0) {
    (void)sprintf(str, "        Code Generation Time:   %6ld", tim.gen);
    lmLiPrint(str);
  }
  (void)sprintf(str,   "        ------------------------------");
  lmLiPrint(str);
  (void)sprintf(str,   "        Compilation Time:       %6ld", tim.comp);
  lmLiPrint(str);
  lmLiPrint("");
  (void)sprintf(str,   "        Total Time:             %6ld", tim.tot);
  lmLiPrint(str);
}



/*----------------------------------------------------------------------*/
static void statistics(void)
{
  int lins;
  char str[80];
  
  lmSkipLines(7);
  lmLiPrint("");
  
  lins = scannedLines();
  if (fileNo > 1)
    (void)sprintf(str, "        %d source lines read from %d files.", lins, fileNo);
  else
    (void)sprintf(str, "        %d source lines read.", lins);
  lmLiPrint(str);
  lmLiPrint("");
  
  if (tim.tot != 0 && tim.comp != 0) {
    (void)sprintf(str, "        %d lines/CPUminute.",
	    (int)(60L*1000L*(long)lins/tim.comp));
    lmLiPrint(str);
    lmLiPrint("");
  }
  
  (void)sprintf(str,   "        Estimated dynamic memory usage = %ld bytes.",
				(long int)((char *)malloc(10000)-(char *)heap));
  lmLiPrint(str);
  (void)sprintf(str,   "        Calculated       - \"\" -        = %ld bytes.",
	  allocated);
  lmLiPrint(str);
  lmLiPrint("");
}


/* -- local variables for main() -- */
  
static char srcfnm[255];	/* File name of source file */
static char txtfnm[255];	/*   - " -   of collected text file */
static char datfnm[255];	/*   - " -   of encoded data file */
static char acdfnm[255];	/*   - " -   of ACODE file */
static char lstfnm[255];	/*   - " -   of listing file */


/*----------------------------------------------------------------------

  prepareNames()

  Prepare all file names needed.

 */
static void prepareFileNames(void)
{
  /* Save source file name */
  strcpy(srcfnm, srcptr);

  /* -- Then strip directory -- */
  if((srcptr = strrchr(srcfnm, ']')) == NULL
     && (srcptr = strrchr(srcfnm, '>')) == NULL
     && (srcptr = strrchr(srcfnm, '/')) == NULL
     && (srcptr = strrchr(srcfnm, '\\')) == NULL
     && (srcptr = strrchr(srcfnm, ':')) == NULL)
    srcptr = &srcfnm[0];
  else
    srcptr++;
  /* Save the basename as the name of the adventure */
  adv.name = strdup(srcptr);

  /* -- check for .ALAN suffix and add one if missing -- */
  if(strrchr(srcptr, '.') == NULL) { /* Point to last '.' */
#ifdef __dos__
    strcat(srcptr, ".ala");	/* Was there none add */
#else
#ifndef __mac__
    strcat(srcptr, ".alan");	/* Was there none add */
#endif
#endif
  } else {
    char *p = strrchr(adv.name, '.');
    *p = '\0';
  }

  /* -- create list file name -- */
  strcpy(lstfnm, adv.name);
  strcat(lstfnm, ".lis");
  
  /* -- create string data file names -- */
  strcpy(txtfnm, adv.name);
  strcat(txtfnm, ".tmp");
  strcpy(datfnm, adv.name);
  strcat(datfnm, ".dat");
  
  /* -- create ACODE file name -- */
  strcpy(acdfnm, adv.name);
  strcat(acdfnm, ".a3c");
}


/*----------------------------------------------------------------------*/
static void bookmarkHeap() {
  heap = malloc((size_t)10000);		/* Remember where heap starts */
  free(heap);
}


/*----------------------------------------------------------------------*/
static void setupCompilation() {
  lmLiInit(alan.shortHeader, srcfnm, lm_ENGLISH_Messages);
  setCharacterSet(charset);

  if (!smScanEnter(nulsrcp, srcfnm, FALSE)) {
    /* Failed to open the source file */
      createListingOnScreen(liMSG, sevALL);
    terminate(EXIT_FAILURE);
  }
}


/*----------------------------------------------------------------------*/
static void parse() {
  verbose("Parsing");

  startTimingCompilation();			/* Start timing compilation */

  txtfil = fopen(txtfnm, "w+");	/* Open a temporary text file */
  if (!txtfil) {
    char errorString[1000];
    sprintf(errorString, "Could not open output file '%s' for writing", txtfnm);
    SYSERR(errorString, nulsrcp);
  }

  /* First initialise */
  initAdventure();

  /* Then parse the program and build internal representation */
  startTiming();
  pmParse();
  endParseTiming();			/* End of parsing pass */
}


/*----------------------------------------------------------------------*/
static void analyze() {
  verbose("Analyzing");
  startTiming();
  analyzeAdventure();			/* Analyze the adventure */
  endSemanticsTiming();			/* End of semantic pass */
  /* All text is now output so close text file */
  fclose(txtfil);

}


/*----------------------------------------------------------------------*/
static void generate() {
  /* OK so far ? */
  if (lmSeverity() < sevERR) {
    /* Yes, so generate an adventure */
    verbose("Generating");

    opts[OPTDEBUG].value = debugFlag; /* Debugging enabled? */
    if (packFlag)		/* Force packing */
      opts[OPTPACK].value = TRUE;
    startTiming();
    generateAdventure(acdfnm, txtfnm, datfnm);
    endGenerationTiming();			/* End of generating pass */
  } else {
    lmLog(NULL, 999, sevINF, "");
  }
}


/*----------------------------------------------------------------------*/
static void removeTemporaryFiles() {
  if (dumpFlags == 0) {
    unlink(txtfnm);
    unlink(datfnm);
  }
}


/*----------------------------------------------------------------------*/
static void dumpAndExitAfterPhase(int phase) {
  if ((dumpFlags&phase) != 0) {
    createListingOnScreen(liTINY, 0);
    lmSkipLines(0);
    dumpAdventure(dumpFlags);
    terminate(EXIT_FAILURE);
  }
}


/*----------------------------------------------------------------------*/
static void listingOnFile() {
  if (listingFlag) {
    createListingOnFile(lstfnm, lcount, ccount, fullFlag?liFULL:liTINY, sevALL);
    if (dumpFlags) {
      lmSkipLines(0);
      dumpAdventure(dumpFlags);
    }
    if (summaryFlag) {
      if (lmSeverity() < sevERR)
	summary();
      statistics();
    }
  }
}


/*----------------------------------------------------------------------*/
static void listingOnScreen() {
  lmSev sevs;

  if (verboseFlag) printf("\n");

  /* Check what messages to show on the screen */
  sevs = sevALL;
  if (!warningFlag)
    sevs &= ~sevWAR;
  if (!infoFlag)
    sevs &= ~sevINF;
  createListingOnScreen(listingFlag?liTINY:(fullFlag?liFULL:liTINY), sevs);

  if (dumpFlags != 0 && !listingFlag) {
    lmSkipLines(0);
    dumpAdventure(dumpFlags);
  }
  if (summaryFlag) {
    if (lmSeverity() < sevERR)
      summary();
    endTotalTiming();			/* Stop timer */
    printTimes();
    statistics();
  }
}

/* Import of dump functions to be used in initDumpers */
extern void dumpAdd();
extern void dumpAlternative();
extern void dumpAttribute();
extern void dumpCheck();
extern void dumpClass();
extern void dumpElement();
extern void dumpEvent();
extern void dumpExit();
extern void dumpExpression();
extern void dumpId();
extern void dumpIfid();
extern void dumpInstance();
extern void dumpLimit();
extern void dumpPointer();
extern void dumpRestriction();
extern void dumpRule();
extern void dumpScript();
extern void dumpStatement();
extern void dumpStep();
extern void dumpSyntax();
extern void dumpSynonym();
extern void dumpVerb();

extern void xmlClass();
extern void xmlInstance();
extern void xmlExit();

/*----------------------------------------------------------------------*/
static void initDumpers(void) {
    addListNodeDumper(ADD_LIST, &dumpAdd);
    addListNodeDumper(ALTERNATIVE_LIST, &dumpAlternative);
    addListNodeDumper(ATTRIBUTE_LIST, &dumpAttribute);
    addListNodeDumper(CHECK_LIST, &dumpCheck);
    addListNodeDumper(CLASS_LIST, &dumpClass);
    addListNodeDumper(CONTAINER_LIST, &dumpPointer);
    addListNodeDumper(ELEMENT_LIST, &dumpElement);
    addListNodeDumper(EVENT_LIST, &dumpEvent);
    addListNodeDumper(EXIT_LIST, &dumpExit);
    addListNodeDumper(EXPRESSION_LIST, &dumpExpression); 
    addListNodeDumper(ID_LIST, &dumpId);
    addListNodeDumper(IFID_LIST, &dumpIfid);
    addListNodeDumper(INSTANCE_LIST, &dumpInstance);
    addListNodeDumper(LIMIT_LIST, &dumpLimit);
    addListNodeDumper(NAME_LIST, &dumpId);
    addListNodeDumper(RESTRICTION_LIST, &dumpRestriction);
    addListNodeDumper(RULE_LIST, &dumpRule);
    addListNodeDumper(SCRIPT_LIST, &dumpScript);
    addListNodeDumper(STATEMENT_LIST, &dumpStatement);
    addListNodeDumper(STEP_LIST, &dumpStep);
    addListNodeDumper(SYNTAX_LIST, &dumpSyntax);
    addListNodeDumper(SYNONYM_LIST, &dumpSynonym);
    addListNodeDumper(VERB_LIST, &dumpVerb);

    addXmlNodeDumper(CLASS_LIST, &xmlClass);
    addXmlNodeDumper(INSTANCE_LIST, &xmlInstance);
    addXmlNodeDumper(EXIT_LIST, &xmlExit);
}


/************************************************************************/
void compile(void) {

    initDumpers();

    startTotalTiming();
    prepareFileNames();

    bookmarkHeap();
    setupCompilation();
    parse();
    dumpAndExitAfterPhase(DUMP_AFTER_PARSE);
    analyze();
    dumpAndExitAfterPhase(DUMP_AFTER_ANALYSIS);
    generate();
    endCompilationTiming();
    removeTemporaryFiles();
    xmlAdventure();
    listingOnFile();
    listingOnScreen();
    lmLiTerminate();

#ifdef MALLOC
    if (malloc_verify() == 0) printf("Error in heap!\n");
#endif

    if (lmSeverity() < sevERR)
        terminate(EXIT_SUCCESS);
    else
        terminate(EXIT_FAILURE);
}
