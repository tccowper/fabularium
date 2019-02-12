/*----------------------------------------------------------------------*\

			       article.c
			   Article Handling

\*----------------------------------------------------------------------*/

/* Own */
#include "article_x.h"

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
Article *newArticle(Srcp srcp, List *statements, Bool isForm) {
  Article *new = NEW(Article);

  new->srcp = srcp;
  new->statements = statements;
  new->isForm = isForm;

  return new;
}


/*======================================================================*/
void analyzeArticle(Article *article, Context *context) {
  if (article != NULL)
    analyzeStatements(article->statements, context);
}


/*======================================================================*/
void generateArticle(Article *article) {

  if (article != NULL) {
    article->address = nextEmitAddress();
    generateStatements(article->statements);
    emit0(I_RETURN);
  }
}


/*======================================================================*/
void generateArticleEntry(Article *article, ArticleEntry *entry) {
  if (article) {
    entry->address = article->address;
    entry->isForm = article->isForm;
  } else
    entry->address = 0;
}


/*----------------------------------------------------------------------*/
static void dumpFormKind(FormKind kind) {
  switch (kind) {
  case FORM: put("FORM"); break;
  case ARTICLE: put("ARTICLE"); break;
  }
}


/*======================================================================*/
void dumpArticle(Article *article) {

  if (article == NULL) {
    put("NULL");
    return;
  }

  put("ARTICLE: ");
  indent();
  put("srcp: "); dumpSrcp(article->srcp); nl();
  put("kind: "); dumpFormKind(article->kind); nl();
  put("isForm: "); dumpBool(article->isForm); nl();
  put("statements: "); dumpList(article->statements, STATEMENT_LIST);
  out();  
}
