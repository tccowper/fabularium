/*----------------------------------------------------------------------*\

			       description.c
			   Description Handling

\*----------------------------------------------------------------------*/

/* Own */
#include "description_x.h"

/* Imports: */
#include "sysdep.h"
#include "util.h"
#include "srcp_x.h"
#include "lst_x.h"
#include "chk_x.h"
#include "stm_x.h"
#include "dump_x.h"
#include "emit.h"


/* Private data */


/*======================================================================*/
Description *newDescription(Srcp checkSrcp, List *checks,
			    Srcp doesSrcp, List *does) {
  Description *new = NEW(Description);

  new->doesSrcp = doesSrcp;
  new->checkSrcp = checkSrcp;
  new->checks = checks;
  new->doesSrcp = doesSrcp;
  new->does = does;

  return new;
}


/*======================================================================*/
List *checksOf(Description *description) {
  if (description != NULL && description->checks != NULL)
    return description->checks;
  return NULL;
}

/*======================================================================*/
List *doesOf(Description *description) {
  if (description != NULL && description->does != NULL)
    return description->does;
  return NULL;
}

/*======================================================================*/
void analyzeDescription(Description *description, Context *context) {
  if (description) {
    if (description->checks)
      analyzeChecks(description->checks, context);
    if (description->does)
      analyzeStatements(description->does, context);
  }
}


/*======================================================================*/
Aaddr checksAddressOf(Description *description) {
  if (description != NULL && description->checks != NULL)
    return description->checksAddress;
  return 0;
}

/*======================================================================*/
Aaddr doesAddressOf(Description *description) {
  if (description != NULL && description->does != NULL)
    return description->doesAddress;
  return 0;
}

/*======================================================================*/
void generateDescription(Description *description) {

  if (checksOf(description) != NULL)
    description->checksAddress = generateChecks(checksOf(description));

  if (doesOf(description) != NULL) {
    description->doesAddress = nextEmitAddress();
    generateStatements(doesOf(description));
    emit0(I_RETURN);
  }

}


/*======================================================================*/
void dumpDescription(Description *description) {

  if (description == NULL) {
    put("NULL");
    return;
  }

  put("DESCRIPTION: ");
  indent();
  put("checkSrcp: "); dumpSrcp(description->checkSrcp); nl();
  put("checks: "); dumpList(description->checks, CHECK_LIST); nl();
  put("doesSrcp: "); dumpSrcp(description->doesSrcp); nl();
  put("does: "); dumpList(description->does, STATEMENT_LIST);
  out();  
}
