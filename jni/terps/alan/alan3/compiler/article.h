#ifndef _ARTICLE_H_
#define _ARTICLE_H_
/*----------------------------------------------------------------------*\

			      ARTICLE.H
			Article Structure

\*----------------------------------------------------------------------*/

/* USE: */
#include "types.h"

#include "srcp.h"
#include "lst.h"

#include "acode.h"


/* Types */

typedef enum FormKind {
  FORM,
  ARTICLE
} FormKind;

typedef struct Article {
  Srcp srcp;
  FormKind kind;
  Bool isForm;
  List *statements;
  Aaddr address;
} Article;


#endif
