#ifndef _SYM_X_H_
#define _SYM_X_H_
/*----------------------------------------------------------------------*\

				SYM.H
			  Symbol Table Nodes

\*----------------------------------------------------------------------*/

#include "sym.h"

/* USE: */
#include "types.h"
#include "elm.h"
#include "lst.h"
#include "id.h"
#include "atr.h"
#include "context.h"
#include "scr.h"


/* DATA: */

extern int frameLevel;

extern int classCount;
extern int instanceCount;
extern int eventCount;
extern int verbCount;

extern Symbol *entitySymbol;
extern Symbol *thingSymbol;
extern Symbol *objectSymbol;
extern Symbol *locationSymbol;
extern Symbol *actorSymbol;
extern Symbol *literalSymbol;
extern Symbol *stringSymbol;
extern Symbol *integerSymbol;
extern Symbol *theHero;
extern Symbol *nowhere;
extern Symbol *messageVerbSymbolForInstance;
extern Symbol *messageVerbSymbolFor2Instances;
extern Symbol *messageVerbSymbolForString;
extern Symbol *messageVerbSymbolFor2Strings;
extern Symbol *messageVerbSymbolFor2Integers;
extern Symbol *messageVerbSymbolFor3Integers;


/* FUNCTIONS: */

extern void idRedefined(Id *id, Symbol *sym, Srcp previousDefinition);
extern void initSymbols(void);
extern void createMessageVerbs();

extern Symbol *newSymbol(Id *id, SymbolKind kind);
extern Symbol *newInstanceSymbol(Id *id, Properties *props, Symbol *parent);
extern Symbol *newParameterSymbol(Element *element);
extern Symbol *newClassSymbol(Id *id, Properties *props, Symbol *parent);
extern Symbol *newVerbSymbol(Id *id);
extern Symbol *symcheck(Id *id, SymbolKind kind, Context *context);

/* Lookup a symbol */
extern Symbol *lookup(char idString[]);
extern Script *lookupScript(Symbol *aSymbol, Id *scriptName);
extern Symbol *lookupParameter(Id *parameterId, List *parameterSymbols);
extern void instanceCheck(Id *id, char *what, char className[]);
extern Bool isClass(Symbol *symbol);
extern Bool isInstance(Symbol *symbol);
extern Bool symbolIsContainer(Symbol *symbol);
extern Bool symbolIsActor(Symbol *symbol);
extern Symbol *containerSymbolTakes(Symbol *symbol);

extern void newFrame(void);
extern void deleteFrame(void);
extern char *verbHasParametersMessage(Context *context);
extern char *verbHasParametersOrNoneMessage(Context *context);
extern Symbol *classOfSymbol(Symbol *symbol);
extern TypeKind basicTypeFromClassSymbol(Symbol *class);
extern TypeKind typeOfSymbol(Symbol *symbol);

/* Inheritance of a class */
extern void setParent(Symbol *child, Symbol *parent);
extern Symbol *parentOf(Symbol *child);
extern Bool inheritsFrom(Symbol *child, Symbol *ancestor);
extern void inheritCheck(Id *id, char *what, char classOrInstance[], char className[]);
extern Symbol *commonParent(Symbol *sym1, Symbol *sym2);

extern TypeKind classToType(Symbol *symbol);
extern Symbol *definingSymbolOfAttribute(Symbol *symbol, Id *id);

extern Bool instancesExist(Symbol *someClass);
extern SymbolIterator createSymbolIterator(void);
extern Symbol *getNextInstanceOf(SymbolIterator iterator, Symbol *parent);

extern void calculateTransitiveContainerContents(void);
extern Symbol *containerMightContain(Symbol *symbol);

extern void setParameters(Symbol *verb, List *parameters);

extern Attribute *findInheritedAttribute(Symbol *symbol, Id *id);
extern void numberAllAttributes(void);
extern void replicateInherited(void);

extern void generateSymbol(Symbol *symbol);

/* Dump */
extern void dumpSymbol(Symbol *symbol);
extern void dumpSymbols(void);

#endif
