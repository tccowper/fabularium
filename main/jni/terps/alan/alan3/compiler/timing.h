/* timing.h */

#ifdef HAVE_TIMES_H
#include <sys/times.h>
#endif

typedef struct {
#ifdef HAVE_TIMES_H
    struct tms tms;
#endif
    long pu_start;		/* ticks */
    long pu_elapsed;	/* ms */
    long cu_start;		/* ticks */
    long cu_elapsed;	/* ms */
    long real_start;	/* s */
    long real_elapsed;	/* s */
} TIBUF, *TIBUFP;

#define TICK (1000/60)		/* Factor to make ticks to ms */

extern void tistart(TIBUFP tb);
extern void tistop(TIBUFP tb);
