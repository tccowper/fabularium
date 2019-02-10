/*----------------------------------------------------------------------*\

  options.h

  Alan compiler command line option flags
  NOTE that options in the generated Acode is handled by opt.h/.c

\*----------------------------------------------------------------------*/

#include "lst.h"
#include "dump.h"

extern char *srcptr;		/* Pointer to adventure name */
extern int charset;			/* Which charset to read source in */
extern Bool warningFlag;	/* Show warnings */
extern Bool infoFlag;		/* Show informational messages */
extern Bool fullFlag;		/* Full source listing */
extern Bool listingFlag;	/* Create listing file */
extern Bool ccFlag;			/* Show messages as old 'cc' */
extern Bool ideFlag;		/* Format messages for AlanIDE */
extern int lcount;			/* Number of lines per page */
extern int ccount;			/* -"-    columns */
extern DmpKind dumpFlags;	/* Dump internal form flags */
extern Bool xmlFlag;		/* Output XML option flag */
extern Bool debugFlag;		/* Debug option flag */
extern Bool packFlag;		/* Pack option flag */
extern Bool summaryFlag;	/* Print a summary */
extern List *importPaths;	/* List of additional include paths */
