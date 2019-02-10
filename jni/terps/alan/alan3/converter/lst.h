#ifndef _LST_H_
#define _LST_H_
/*----------------------------------------------------------------------*\

				LST.H
			      List Nodes

\*----------------------------------------------------------------------*/

/* Use: */

/* Types */

typedef enum ListKind {
  UNKNOWN_LIST,
  ADD_LIST,
  ALTERNATIVE_LIST,
  ATTRIBUTE_LIST,
  CASE_LIST,
  CHECK_LIST,
  CLASS_LIST,
  CONTAINER_LIST,
  ELEMENT_LIST,
  EVENT_LIST,
  EXIT_LIST,
  EXPRESSION_LIST,
  ID_LIST,
  INSTANCE_LIST,
  LIMIT_LIST,
  LIST_EENT,
  LIST_LIST,
  MESSAGE_LIST,
  NAME_LIST,
  REFERENCE_LIST,
  RESTRICTION_LIST,
  RULE_LIST,
  SCRIPT_LIST,
  STATEMENT_LIST,
  STEP_LIST,
  STRING_LIST,
  SYMBOL_LIST,
  SYNONYM_LIST,
  SYNTAX_LIST,
  VERB_LIST,
  LAST_LIST_KIND
} ListKind;


typedef struct List {		/* GENERIC LISTS */
  ListKind kind;
  union {
    struct AddNode *add;
    struct AltNod *alt;
    struct Attribute *atr;
    struct ChkNod *chk;
    struct ClaNod *cla;
    struct Instance *ins;
    struct ResNod *res;
    struct Container *cnt;
    struct Element *elm;
    struct EvtNod *evt;
    struct Expression *exp;
    struct ExtNod *ext;
    struct LimNod *lim;
    struct MsgNod *msg;
    struct RulNod *rul;
    struct Script *scr;
    struct StmNod *stm;
    struct Step *stp;
    struct Syntax *stx;
    struct SynNod *syn;
    struct Symbol *sym;
    struct Verb *vrb;
    struct ElmEntry *eent;
    struct IdNode *id;
    struct List *lst;
    char *str;
  } element;			/* Pointer to any type of element */
  struct List *next;		/* Pointer to next list node */
} List;


#endif
