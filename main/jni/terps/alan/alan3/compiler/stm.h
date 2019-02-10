#ifndef _STM_H_
#define _STM_H_
/*----------------------------------------------------------------------*\

  STM.H
  Statement Nodes

\*----------------------------------------------------------------------*/

/* USE: */

#include "srcp.h"
#include "wht.h"
#include "whr.h"
#include "id.h"
#include "exp.h"
#include "lst.h"
#include "resource.h"
#include "acode.h"


/* Types: */

typedef enum StmKind {		/* STATEMENT kinds */
    NOP_STATEMENT,
    PRINT_STATEMENT,
    STYLE_STATEMENT,
    QUIT_STATEMENT,
    LOOK_STATEMENT,
    SAVE_STATEMENT,
    RESTORE_STATEMENT,
    RESTART_STATEMENT,
    SCORE_STATEMENT,
    VISITS_STATEMENT,
    DESCRIBE_STATEMENT,
    SAY_STATEMENT,
    LIST_STATEMENT,
    SHOW_STATEMENT,
    PLAY_STATEMENT,
    EMPTY_STATEMENT,
    LOCATE_STATEMENT,
    INCLUDE_STATEMENT,
    EXCLUDE_STATEMENT,
    MAKE_STATEMENT,
    SET_STATEMENT,
    INCREASE_STATEMENT,
    DECREASE_STATEMENT,
    SCHEDULE_STATEMENT,
    CANCEL_STATEMENT,
    IF_STATEMENT,
    USE_STATEMENT,
    STOP_STATEMENT,
    SYSTEM_STATEMENT,
    DEPEND_STATEMENT,
    DEPENDCASE_STATEMENT,
    EACH_STATEMENT,
    STRIP_STATEMENT,
    TRANSCRIPT_STATEMENT
} StmKind;


typedef struct Statement {      /* STATEMENT */
    Srcp srcp;                  /* Source position */
    StmKind kind;               /* Class tag */
    union {                     /* Class dependent fields */

        struct {                /* for PRINT */
            Bool encoded;
            long fpos;          /* Position in the text file */
            long len;			/* and length */
        } print;

        struct {                /* for SCORE */
            int count;          /* Number of this SCORE */
            int score;          /* Its value */
        } score;

        struct {                /* for VISITS */
            int count;          /* How many times not to show long descriptions */
        } visits;

        struct {                /* for DESCRIBE */
            Expression *what;   /* What? */
        } describe;

        struct {                /* for SAY */
            Expression *exp;    /* What? */
            SayForm form;		/* In what form? SIMPLE, DEFINITE or INDEFINITE */
        } say;

        struct {                /* for LIST */
            Expression *wht;    /* What? */
        } list;

        struct {                /* for SHOW */
            Resource *resource;
        } show;

        struct {                /* for PLAY */
            Resource *resource;
        } play;

        struct {                /* for EMPTY */
            Expression *what;   /* What? */
            Where *where;		/* Where? */
        } empty;

        struct {                /* for LOCATE */
            Expression *what;   /* What? */
            Where *where;		/* Where? */
        } locate;

        struct {                /* for INCLUDE & EXCLUDE */
            Expression *what;   /* What? */
            Expression *set;    /* In which set? */
        } include;

        struct {                /* for MAKE */
            Expression *wht;    /* What? */
            Bool not;			/* Was there a NOT ? */
            Id *atr;            /* Id of the attribute */
        } make;

        struct {                /* for SET */
            Expression *wht;    /* What? */
            Expression *exp;    /* The new value */
        } set;

        struct {                /* for INCR/DECR */
            Expression *wht;    /* What? */
            Expression *step;   /* By what? */
        } incr;

        struct {                /* for SCHEDULE */
            Expression *what;   /* What? Usually an event id.. */
            Where *whr;         /* Where? */
            Expression *when;
        } schedule;

        struct {                /* for CANCEL */
            Expression *what;   /* What to cancel */
        } cancel;

        struct {                /* for IF */
            Expression *exp;
            List *thn;
            List *els;
        } iff;

        struct {                  /* USE */
            Id *script;           /* Which script to use (name) */
            Expression *actorExp; /* For which actor */
        } use;

        struct {                /* STOP */
            Expression *actor;	/* Which actor */
        } stop;

        struct {                /* SYSTEM */
            long fpos;          /* Position to string to execute */
            long len;			/* Length of the string */
        } system;

        struct {                /* DEPEND */
            Expression *exp;    /* Depending expression */
            List *cases;		/* The cases */
        } depend;

        struct {               /* DEPCASE */
            Expression *exp;   /* The partial right hand expression */
            List *stms;
        } depcase;

        struct {                /* EACH */
            Id *loopId;
            TypeKind type;
            List *filters;
            List *stms;
            Expression *setExpression; /* Holding filter which is the set to loop over */
        } each;

        struct {               /* STRIP */
            Bool first;        /* TRUE = First (from the beginning) */
            Expression *count; /* Optional count integer expression */
            Bool wordOrChar;   /* TRUE = words, FALSE = characters */
            Expression *from;
            Expression *into;
        } strip;

        struct {
            int style;
        } style;

        struct {
            Bool on_or_off;
        } transcript;

    } fields;
} Statement;



#endif
