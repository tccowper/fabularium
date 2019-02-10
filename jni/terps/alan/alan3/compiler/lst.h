#ifndef _LST_H_
#define _LST_H_
/*----------------------------------------------------------------------*\

				LST.H
			      List Nodes

\*----------------------------------------------------------------------*/

/* USE: */

/* TYPES */

typedef enum ListKind {
  UNKNOWN_LIST,
  ADD_LIST,
  ALTERNATIVE_LIST,
  ATTRIBUTE_LIST,
  CASE_LIST,
  CHECK_LIST,
  CLASS_LIST,
  CONTAINER_LIST,
  ELEMENT_ENTRIES_LIST,
  ELEMENT_LIST,
  EVENT_LIST,
  EXIT_LIST,
  EXPRESSION_LIST,
  ID_LIST,
  IFID_LIST,
  INSTANCE_LIST,
  LIMIT_LIST,
  LIST_LIST,
  MESSAGE_LIST,
  NAME_LIST,
  REFERENCE_LIST,
  RESOURCE_LIST,
  RESTRICTION_LIST,
  RULE_LIST,
  SCRIPT_LIST,
  SRCP_LIST,
  STATEMENT_LIST,
  STEP_LIST,
  STRING_LIST,
  SYMBOL_LIST,
  SYNONYM_LIST,
  SYNTAX_LIST,
  VERB_LIST,
  WORD_LIST,
  LAST_LIST_KIND
} ListKind;


typedef struct List {		/* GENERIC LISTS */
  ListKind kind;
  union {
    void *ptr;			/* Generic member pointer */
    struct AddNode *add;
    struct AltNod *alt;
    struct Attribute *atr;
    struct ChkNod *chk;
    struct Class *cla;
    struct Container *cnt;
    struct Element *elm;
    struct ElmEntry *eent;
    struct Event *evt;
    struct Exit *ext;
    struct Expression *exp;
    struct IdNode *id;
    struct IfidNode *ifid;
    struct Instance *ins;
    struct LimNod *lim;
    struct List *lst;
    struct MsgNod *msg;
    struct ResNod *res;
    struct Resource *resource;
    struct RulNod *rul;
    struct Script *script;
    struct Srcp *srcp;
    struct Statement *stm;
    struct Step *stp;
    struct Symbol *sym;
    struct Synonym *syn;
    struct Syntax *stx;
    struct Verb *vrb;
    struct Word *word;
    char *str;
  } member;			/* Pointer to any type of element */
  struct List *next;		/* Pointer to next list node */
} List;


#endif
