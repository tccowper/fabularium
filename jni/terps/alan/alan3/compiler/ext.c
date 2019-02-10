/*----------------------------------------------------------------------*\

  EXT.C
  Exit Nodes

  \*----------------------------------------------------------------------*/

#include "ext_x.h"

#include "alan.h"
#include "util.h"

#include "srcp_x.h"
#include "sym_x.h"
#include "id_x.h"
#include "lst_x.h"
#include "stm_x.h"
#include "chk_x.h"
#include "wrd_x.h"
#include "dump_x.h"

#include "elm.h"                /* ELM-nodes */

#include "emit.h"
#include "lmList.h"
#include "acode.h"


/* PUBLIC: */

int dirmin, dirmax;
int dircount = 0;



/*======================================================================*/
Exit *newExit(Srcp *srcp, List *dirs, Id *target, List *chks, List *stms)
{
    Exit *new;			/* The newly created node */
    Symbol *sym;
    List *lst;			/* Traversal pointer */

    progressCounter();

    new = NEW(Exit);

    new->srcp = *srcp;
    new->directions = dirs;
    new->target = target;
    new->chks = chks;
    new->stms = stms;

    for (lst = dirs; lst != NULL; lst = lst->next) {
        sym = lookup(lst->member.id->string); /* Find any earlier definition */
        if (sym == NULL) {
            lst->member.id->symbol = newSymbol(lst->member.id, DIRECTION_SYMBOL);
            lst->member.id->code = lst->member.id->symbol->code;
            newDirectionWord(lst->member.id->string, lst->member.id->symbol->code);
        } else if (sym->kind == DIRECTION_SYMBOL) {
            lst->member.id->symbol = sym;
            lst->member.id->code = lst->member.id->symbol->code;
        } else
            idRedefined(lst->member.id, sym, sym->srcp);
    }

    return(new);
}



/*-----------------------------------------------------------------------*/
static void symbolizeExit(Exit *theExit)
{
    symbolizeId(theExit->target);
#ifdef FIXME // Why?
    symbolizeChecks(theExit->checks);
    symbolizeStatements(theExit->does);
#endif
}


/*======================================================================*/
void symbolizeExits(List *theExitList)
{
    List *lst;

    for (lst = theExitList; lst != NULL; lst = lst->next)
        symbolizeExit(lst->member.ext);
}


/*======================================================================*/
void analyzeExit(Exit *ext, Context *context)
{
    instanceCheck(ext->target, "Target of an Exit", "location");

    analyzeChecks(ext->chks, context);
    analyzeStatements(ext->stms, context);
}



/*======================================================================*/
Bool exitIdFound(Id *targetId, List *exits)
{
    List *theExit;
    List *theIdInList;

    for (theExit = exits; theExit != NULL; theExit = theExit->next) {
        for (theIdInList = theExit->member.ext->directions; theIdInList != NULL; theIdInList = theIdInList->next)
            if (findIdInList(targetId, theIdInList) != NULL)
                return TRUE;
    }
    return FALSE;
}


/*======================================================================*/
void analyzeExits(List *exts, Context *context)
{
    List *ext, *dir, *lst, *other;

    for (lst = exts; lst != NULL; lst = lst->next)
        analyzeExit(lst->member.ext, context);

    /* Check for multiple definitions of a direction */
    for (ext = exts; ext != NULL; ext = ext->next) {
        dir = ext->member.ext->directions;
        /* First check other directions in this EXIT */
        for (other = dir->next; other != NULL; other = other->next) {
            if (other->member.id->symbol != NULL && dir->member.id->symbol != NULL)
                if (other->member.id->symbol->code == dir->member.id->symbol->code) {
                    lmLog(&other->member.id->srcp, 202, sevWAR,
                          other->member.id->string);
                    break;
                }
        }
        /* Then the directions in the other EXITs */
        for (lst = ext->next; lst != NULL; lst = lst->next) {
            for (other = lst->member.ext->directions; other != NULL; other = other->next)
                if (other->member.id->symbol->code == dir->member.id->symbol->code) {
                    lmLog(&other->member.id->srcp, 203, sevWAR,
                          other->member.id->string);
                    break;
                }
        }
    }
}


/*----------------------------------------------------------------------*/
static Bool haveExit(List *ownExits, Id *direction) {
    List *exits;
    List *directions;

    ITERATE(exits, ownExits) {
        ITERATE(directions, exits->member.ext->directions) {
            if (equalId(directions->member.id, direction))
                return TRUE;
        }
    }
    return FALSE;
}

/*----------------------------------------------------------------------*/
static Exit *copyExitExcludingOwn(Exit *original, List *ownExits) {
    List *directionsToCopy = NULL;
    List *direction;

    ITERATE (direction, original->directions)
        if (!haveExit(ownExits, direction->member.id))
            directionsToCopy = concat(directionsToCopy, direction->member.id, ID_LIST);
    return newExit(&original->srcp, directionsToCopy, original->target,
                   original->chks, original->stms);
}


/*======================================================================*/
List *combineExits(List *ownExits, List *exitsToAdd)
{
    /* Insert all exits from the list to add that are not there
       already.
    */
    List *toAdd;
    List *direction;
    List *new = NULL;

    ITERATE(toAdd, exitsToAdd) {
        Bool foundOneToAdd = FALSE;
        /* Each exit may have multiple directions so we must traverse that
           list to see if we should copy this Exit node */
        ITERATE(direction, toAdd->member.ext->directions) {
            if (!haveExit(ownExits, direction->member.id)) {
                foundOneToAdd = TRUE;
                break;
            }
        }
        if (foundOneToAdd)
            new = concat(new, copyExitExcludingOwn(toAdd->member.ext, ownExits), EXIT_LIST);
    }
    return combine(ownExits, new);
}


/*----------------------------------------------------------------------*/
static Aaddr generateExitStatements(Exit *ext)
{
    Aaddr stmadr = nextEmitAddress();

    if (ext->stms == NULL)
        return(0);

    generateStatements(ext->stms);
    emit0(I_RETURN);
    return(stmadr);
}



/*----------------------------------------------------------------------

  generateExitEntry()

  Generate one exit entry in the exit table.

*/
static void generateExitEntry(Exit *ext) /* IN - The exit to generate */
{
    List *dir;
    ExitEntry entry;

    for (dir = ext->directions; dir != NULL; dir = dir->next) {
        entry.code = dir->member.id->symbol->code;
        entry.checks = ext->chks? ext->chkadr : 0;
        entry.action = ext->stms? ext->stmadr : 0;
        entry.target = ext->target->symbol->code;
        emitEntry(&entry, sizeof(entry));
    }
}



/*======================================================================*/
Aaddr generateExits(List *exits)
{
    List *lst;			/* Traversal pointer */
    Aaddr extadr;			/* The adress where the exits start */

    if (exits == NULL)
        return(0);

    for (lst = exits; lst != NULL; lst = lst->next) {
        lst->member.ext->chkadr = nextEmitAddress();
        if (lst->member.ext->chks != NULL)
            lst->member.ext->chkadr = generateChecks(lst->member.ext->chks);
        lst->member.ext->stmadr = generateExitStatements(lst->member.ext);
        emit0(I_RETURN);
    }

    extadr = nextEmitAddress();
    for (lst = exits; lst != NULL; lst = lst->next)
        generateExitEntry(lst->member.ext);
    emit(EOF);
    return(extadr);
}




/*======================================================================

  dumpExit()

  Dump an Exit node.

*/
void dumpExit(Exit *ext)
{
    if (ext == NULL) {
        put("NULL");
        return;
    }

    put("EXT: "); dumpSrcp(ext->srcp); indent();
    put("dirs: "); dumpList(ext->directions, ID_LIST); nl();
    put("target: "); dumpId(ext->target); nl();
    put("chks: "); dumpList(ext->chks, CHECK_LIST); nl();
    put("chkadr: "); dumpAddress(ext->chkadr); nl();
    put("stms: "); dumpList(ext->stms, STATEMENT_LIST); nl();
    put("stmadr: "); dumpAddress(ext->stmadr); out();
}


/*======================================================================*/
void xmlExit(Exit *exit, FILE* xmlFile)
{
    List *direction;
    ITERATE(direction, exit->directions) {
        fprintf(xmlFile, "            <exit DIRECTION=\"%s\" TARGET=\"%s\" />\n",
                direction->member.id->string, exit->target->string);
    }
}
