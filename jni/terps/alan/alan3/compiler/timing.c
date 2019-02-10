/* timing.c */

#include "sysdep.h"
#include "timing.h"


/*======================================================================*/
void tistart(TIBUFP tb)
{
#ifdef HAVE_TIMES_H
    // TODO Remove times() since it is obsolete, use gettimeofday instead
    times((struct tms *)tb);
    tb->pu_start = tb->tms.tms_utime;
    tb->cu_start = tb->tms.tms_cutime;
#endif
}


/*======================================================================*/
void tistop(TIBUFP tb)
{
#ifdef HAVE_TIMES_H
    times((struct tms *)tb);
    tb->pu_elapsed = TICK * (tb->tms.tms_utime - tb->pu_start);
    tb->cu_elapsed = TICK * (tb->tms.tms_cutime - tb->cu_start);
#endif
}
