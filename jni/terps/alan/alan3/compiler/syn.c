/*----------------------------------------------------------------------*\

				SYN.C
			    Synonym Nodes

\*----------------------------------------------------------------------*/

#include "syn.h"


#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "lst_x.h"
#include "adv_x.h"
#include "wrd_x.h"
#include "id_x.h"
#include "dump_x.h"

#include "lmList.h"
#include "acode.h"


#include "emit.h"



/*======================================================================*/
Synonym *newSynonym(Srcp *srcp, List *synonymIdList, Id *targetId)
{
  Synonym *new;

  progressCounter();

  new = NEW(Synonym);

  new->srcp = *srcp;
  new->ids = synonymIdList;
  new->id = targetId;

  return(new);
}



/*======================================================================*/
void analyzeSynonyms(void)
/*  Analyze all synonyms by finding the target words in the dictionary,
    and inserting the synonyms. */
{
  List *lst;		/* Traversal pointer for target list */
  List *slst;		/* Traversal pointer for synonyms lists */
  Word *wrd;		/* Target word */
  Word *swrd;		/* Synonym word */

  for (lst = adv.syns; lst != NULL; lst = lst->next) {
    progressCounter();
    wrd = findWord(lst->member.syn->id->string);
    if (wrd == NULL)		/* Couldn't find target word */
      lmLog(&lst->member.syn->id->srcp, 321, sevWAR, lst->member.syn->id->string);
    else
      for (slst = lst->member.syn->ids; slst != NULL; slst = slst->next) {
	/* Look up the synonym */
	swrd = findWord(slst->member.id->string);
	if (swrd != NULL && (swrd->classbits&SYNONYM_BIT)!=0)
	  lmLog(&slst->member.id->srcp, 322, sevWAR, slst->member.id->string);
	else
	  newSynonymWord(slst->member.id->string, wrd);
      }
  }
}


/*======================================================================*/
void dumpSynonym(Synonym *syn)
{
  if (syn == NULL) {
    put("NULL");
    return;
  }

  put("SYN: "); dumpSrcp(syn->srcp); indent();
  put("id: "); dumpId(syn->id); nl();
  put("ids: "); dumpList(syn->ids, ID_LIST); out();
}
