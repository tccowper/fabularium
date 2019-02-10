#ifndef _SYM_H_
#define _SYM_H_
/*----------------------------------------------------------------------*\

  SYM.H
  Symbol Table Nodes

  ----------------------------------------------------------------------*/


/* USE: */
#include "types.h"
#include "lst.h"
#include "type.h"
#include "srcp.h"


/* TYPES: */

typedef enum SymbolKind {
    CLASS_SYMBOL = 1,
    INSTANCE_SYMBOL = 2,
    FUNCTION_SYMBOL = 4,
    VERB_SYMBOL = 8,
    DIRECTION_SYMBOL = 16,
    PARAMETER_SYMBOL = 32,
    EVENT_SYMBOL = 64,
    LOCAL_SYMBOL = 128,
    ERROR_SYMBOL = 256,
    MAX_SYMBOL = 512
} SymbolKind;


typedef struct Symbol {		/* SYMBOL TABLE ENTRY */
    SymbolKind kind;		/* What kind of symbol? */
    char *string;			/* Name of this entry */
    Srcp srcp;
    int code;			/* Internal code for this symbol in its kind */
    struct Symbol *lower, *higher;	/* Links to build a binary search tree */
    union {

        struct {
            struct Symbol *parent;
            Bool attributesNumbered;
            Bool replicated;
            struct Properties *props;
            Bool prohibitedSubclassing;
            Bool isBasicType;
        } entity;

        struct {
            Bool meta;
            List *parameterSymbols;
            struct Syntax *firstSyntax;
        } verb;

        struct {
            struct Element *element;
            struct Symbol *class;
            Bool restrictedToContainer;
            TypeKind type;
        } parameter;

        struct {
            int number;
            int level;
            TypeKind type;
            struct Symbol *class;
        } local;

    } fields;
} Symbol;


/* Abstract type for SymbolIterators */
typedef struct SymbolIteratorStruct *SymbolIterator;

#endif
