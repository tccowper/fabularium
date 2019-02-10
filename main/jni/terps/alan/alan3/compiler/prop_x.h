#ifndef _PROP_X_H_
#define _PROP_X_H_
/*----------------------------------------------------------------------*\

			       PROP_X.H
			Property Nodes Export

\*----------------------------------------------------------------------*/

#include "prop.h"

/* USE: */
#include "acode.h"
#include "ins.h"
#include "initialize.h"
#include "description.h"
#include "article.h"
#include "context.h"


/* DATA: */

/* METHODS: */

extern Properties *newEmptyProps(void);

extern Properties *newProps(Where *whr,
			    List *names,
			    Srcp pronounsSrcp, List *pronouns,
			    List *attributes,
			    Initialize *init,
			    Description *description,
			    Srcp mentionedSrcp, List *mentioned,
			    Article *definite,
			    Article *indefinite, Article *negative,
			    Container *cnt,
			    List *vrbs,
			    Srcp enteredSrcp, List *entered,
			    List *exts,
			    List *scrs);

extern void symbolizeProps(Properties *props, Bool inClassDeclaration);
extern void analyzeProps(Properties *props, Context *context);
extern void addOpaqueAttribute(Properties *props, Bool opaque);
extern void generateCommonPropertiesData(Properties *props);
extern void generateInstancePropertiesData(Properties *props);
extern void generatePropertiesEntry(InstanceEntry *entry, Properties *props);
extern void dumpProps(Properties *props);


#endif
