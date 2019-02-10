/*----------------------------------------------------------------------*\

				OPT.C
			   Option Handling

\*----------------------------------------------------------------------*/

#include "alan.h"
#include "sysdep.h"

#include "srcp_x.h"
#include "lmList.h"

#include "acode.h"

#include "opt.h"		/* OPTIONS */
#include "emit.h"



/* Exports: */

OptDef opts[NOPT] = {
  {ENUMOPT, FALSE, "language", L_ENGLISH}, /* OPTION Language */
  {INTOPT, FALSE, "width", 75},            /* OPTION Width */
  {INTOPT, FALSE, "length", 24},           /* OPTION Length */
  {BOOLOPT, FALSE, "pack", FALSE},	   /* OPTION Pack */
  {BOOLOPT, FALSE, "debug", FALSE}         /* OPTION Debug */
};



/* Private: */

/* Enumerated values for Language-option */
static char *enumlang[] = {
  "english",
  "swedish",
  "german",
  NULL
};

/* Option bounds for numeric options */
static struct {
  int min,max;
} optbounds[NOPT] = {
  {  0,   0},
  { 24, 255},
  {  5, 255},
  {  0,   0},
  {  0, 255}
};

/* Table of pointers to enumerated values for enum options */
static char **enumtbl[NOPT] = {
  enumlang,
  NULL,
  NULL,
  NULL,
  NULL
};


/*----------------------------------------------------------------------

  optcode()

  Convert an option name to a code.

  */
static int optcode(char *id)
{
  int opt;

  for (opt = 0; opt <= NOPT-1; opt++)
    if (compareStrings(opts[opt].name, id) == 0)
      return(opt);

  return(EOF);
}


/*----------------------------------------------------------------------

  enumcode()

  Convert an option enum name to a code.

  */
static int enumcode(int opt, char *id)
{
  char **names;
  int i;

  if ((names = enumtbl[opt]) == NULL)
    return(EOF);

  for (i = 0; names[i] != NULL; i++)
    if (compareStrings(names[i], id) == 0)
      return(i);

  return(EOF);
}


/*======================================================================

  optint()

  Handle an INTEGER option.

  */
void optint(char *id, Srcp *srcp, int val)
{
  int opt;

  if ((opt = optcode(id)) == EOF) {
    lmLog(srcp, 601, sevWAR, id);
    return;
  }

  if (opts[opt].type != INTOPT
      ||  val < optbounds[opt].min
      ||  val > optbounds[opt].max) {
        lmLog(srcp, 602, sevWAR, id);
        return;
      }

  if (opts[opt].used) {
    lmLog(srcp, 600, sevWAR, id);
    return;
  }

  opts[opt].used = TRUE;
  opts[opt].value = val;
}


/*======================================================================

  optenum()

  Handle an ENUM option.

  */
void optenum(char *id, Srcp *srcp, char *val)
{
  int opt, code;

  if ((opt = optcode(id)) == EOF) {
    if (compareStrings(id, "no") == 0) {
      /* This was actually a NO optBool */
      optBool(val, srcp, FALSE);
    } else
      /* Unrecognized option */
      lmLog(srcp, 601, sevWAR, id);
    return;
  }

  if (opts[opt].type != ENUMOPT
      || (code = enumcode(opt, val)) == EOF) {
    lmLog(srcp, 602, sevWAR, id);
    return;
    }

  if (opts[opt].used) {
    lmLog(srcp, 600, sevWAR, id);
    return;
  }

  opts[opt].used = TRUE;
  opts[opt].value = code;
}


/*======================================================================

  optbool()

  Handle a BOOLEAN option.

  */
void optBool(char *id, Srcp *srcp, int val)
{
  int opt;

  if ((opt = optcode(id)) == EOF) {
    lmLog(srcp, 601, sevWAR, id);
    return;
  }

  if (opts[opt].type != BOOLOPT) {
    lmLog(srcp, 602, sevWAR, id);
    return;
  }

  if (opts[opt].used) {
    lmLog(srcp, 600, sevWAR, id);
    return;
  }

  opts[opt].used = TRUE;
  opts[opt].value = val;
}


/*======================================================================*/
void generateOptions(ACodeHeader *header)
{
  header->pageLength = opts[OPTLEN].value;
  header->pageWidth = opts[OPTWIDTH].value;
  header->pack = opts[OPTPACK].value;
  header->debug = opts[OPTDEBUG].value;
}
