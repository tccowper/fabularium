/*----------------------------------------------------------------------*\

				NAM.C
			    Names handling

\*----------------------------------------------------------------------*/

#include "nam_x.h"

#include "alan.h"
#include "msg.h"
#include "stm_x.h"
#include "srcp_x.h"
#include "lst_x.h"
#include "emit.h"
#include "util.h"


/*----------------------------------------------------------------------*/
static int saveName(List *names, Id *id)
{
  List *nameList;
  char *buf = NULL;
  int len = 0;

  if (names != NULL) {
    for (nameList = names->member.lst; nameList != NULL; nameList = nameList->next) {
      buf = allocate(strlen(nameList->member.id->string)+2);
      toIso(buf, nameList->member.id->string, charset);
      if (nameList->next)
	strcat(buf, " ");
      generateText(buf);
      len = len + strlen(buf);
      free(buf);
    }
  } else {
    buf = allocate(strlen(id->string)+1);
    toIso(buf, id->string, charset);
    generateText(buf);
    len = strlen(buf);
    free(buf);
  }
  return(len);
}


/*======================================================================*/
void analyzeNames(Properties *props) {
  Statement *stm;

  /* Create a PRINT statement for the first name */
  stm = newStatement(&nulsrcp, PRINT_STATEMENT);
  stm->fields.print.fpos = ftell(txtfil);
  stm->fields.print.len = saveName(props->names, props->id);
  props->nameStatement = newList(stm, STATEMENT_LIST);
}
