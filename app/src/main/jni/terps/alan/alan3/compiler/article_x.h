#ifndef _ARTICLE_X_H_
#define _ARTICLE_X_H_

/* OWN definitions */
#include "article.h"

/* USE other definitions */
#include "context.h"


/* Data: */


/* FUNCTIONS: */

extern Article *newArticle(Srcp srcp, List *statements, Bool isForm);
extern void analyzeArticle(Article *article, Context *context);
extern void generateArticle(Article *article);
extern void generateArticleEntry(Article *article, ArticleEntry *entry);
extern void dumpArticle(Article *article);

#endif
