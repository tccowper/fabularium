#ifndef _pmErr_h_
#define _pmErr_h_

extern void pmRPoi(	/* Error recovery - restart point  */
Token *sym			/* IN the restart symbol */
);
extern void pmISym(	/* Error recovery - insert symbol  */
int code,			/* IN terminal code number */
const char *sstr,		/* IN terminal string */
const char *pstr,		/* IN the terminals print symbol */
Token *sym			/* OUT the created scanner symbol */
);
extern void pmDSym(	/* Error recovery - delete symbol  */
Token *sym,			/* IN terminal code number */
const char *sstr,		/* IN terminal string */
const char *pstr		/* IN terminals print string */
);
extern void pmMess(	/* Error recovery - error message  */
Token *sym,			/* IN error token */
int method,			/* IN recovery method */
int code,			/* IN error classification */
int severity			/* IN error severity code */
);


#endif


