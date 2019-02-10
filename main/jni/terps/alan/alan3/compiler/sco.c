/*----------------------------------------------------------------------*\

				SCO.C
				Scores

\*----------------------------------------------------------------------*/

#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "adv_x.h"

#include "lmList.h"

#include "sco.h"
#include "emit.h"
#include "acode.h"




/* PUBLIC: */

int scoreCount = 0;
int totalScore = 0;



/*======================================================================*/
void prepareScores(void)
{
  adv.scores = (int *) allocate((scoreCount+1)*sizeof(int));
}


/*======================================================================*/
void generateScores(ACodeHeader *header)
{
  int i;

  header->scores = nextEmitAddress();
  if (scoreCount != 0) {
    for (i = 1; i <= scoreCount; i++)
      emit(adv.scores[i]);
  }
  emit(EOF);

  header->maximumScore = totalScore;
  header->scoreCount = scoreCount;
}
