/*----------------------------------------------------------------------*\

  sysdep.h

  System dependencies file for Alan Adventure Language compiler

  N.B. The test for symbols used here should really be of three types
  - processor name (like PC, x86, ...)
  - os name (DOS, WIN32, Solaris2, ...)
  - compiler name and version (DJGPP, CYGWIN, GCC271, THINK-C, ...)

  The set symbols should indicate if a feature is on or off like the GNU
  AUTOCONFIG package does. This is not completely done yet!

\*----------------------------------------------------------------------*/
#ifndef _SYSDEP_H_
#define _SYSDEP_H_


/* Place definitions of OS and compiler here if necessary */
#ifndef __sun__
#ifdef sun
#define __sun__
#endif
#endif

#ifdef _INCLUDE_HPUX_SOURCE
#define __hp__
#endif

#ifndef __unix__
#ifdef unix
#define __unix__
#endif
#endif

#ifdef __APPLE__
// At least GCC 3.x does define this for Darwin
#define __macosx__
#define __unix__
#endif

#ifdef DOS
#define __dos__
#endif

#ifdef __BORLANDC__
#define __dos__
#endif

#ifdef __MINGW32__
#define __windows__
#endif

#ifdef __CYGWIN32__
#define __cygwin__
#endif

#ifdef __PACIFIC__
#define  __dos__
#define HAVE_SHORT_FILENAMES
#endif


/*----------------------------------------------------------------------

  Below follows OS and compiler dependent settings. They should not be
  changed except for introducing new sections when porting to new
  environments.

 */

/************/
/* Includes */
/************/

#include <stdio.h>
#include <ctype.h>
#include <unistd.h>

#ifdef __STDC__
#include <stdlib.h>
#include <string.h>
#endif

#ifdef __mac__
#include <stdlib.h>
#include <string.h>
#endif


/***********************/
/* ISO character sets? */
/***********************/

/* Common case first */
#define ISO 1
#define NATIVECHARSET 0

#ifdef __dos__
#undef ISO
#define ISO 0
#undef NATIVECHARSET
#define NATIVECHARSET 2
#endif

#ifdef __win__
#undef ISO
#define ISO 1
#undef NATIVECHARSET
#define NATIVECHARSET 2
#endif

/* Old Macs uses other CHARSET, Mac OS X uses ISO */
#ifdef __old_mac__
#undef ISO
#define ISO 0
#undef NATIVECHARSET
#define NATIVECHARSET 1
#endif


/**************/
/* File modes */
/**************/
#define READ_MODE "rb"
#define WRITE_MODE "wb"
#ifndef O_TEXT
#define O_TEXT 0
#endif


/****************/
/* Have termio? */
/****************/

#ifdef __CYGWIN__
#define HAVE_TERMIO
#endif

#ifdef __unix__
#define HAVE_TERMIO
#endif


/*******************************/
/* Is ANSI control available?  */
/*******************************/

#ifdef __CYGWIN__
#define HAVE_ANSI
#endif


/* Have times.h? */
#ifndef __MINGW32__
#define HAVE_TIMES_H
#endif

/* Special cases and definition overrides */

#ifdef __dos__

/* Return codes */
#define EXIT_SUCCESS 0
#define EXIT_FAILURE  1

#endif


/* Native character functions */
extern int isSpace(unsigned int c);      /* IN - Native character to test */
extern int isLower(unsigned int c);      /* IN - Native character to test */
extern int isUpper(unsigned int c);      /* IN - Native character to test */
extern int isLetter(unsigned int c);     /* IN - Native character to test */
extern int toLower(unsigned int c);      /* IN - Native character to convert */
extern int toUpper(unsigned int c);      /* IN - Native character to convert */
extern char *strlow(char str[]); /* INOUT - Native string to convert */
extern char *strupp(char str[]); /* INOUT - Native string to convert */

/* ISO character functions */
extern int isISOLetter(int c);  /* IN - ISO character to test */
extern char IsoToLowerCase(int c); /* IN - ISO character to convert */
extern char IsoToUpperCase(int c); /* IN - ISO character to convert */
extern char *stringLower(char str[]); /* INOUT - ISO string to convert */
extern char *stringUpper(char str[]); /* INOUT - ISO string to convert */
extern int compareStrings(char str1[], char str2[]); /* Case-insensitive compare */

/* ISO string conversion functions */
extern void toIso(char copy[],  /* OUT - Mapped string */
		  char original[], /* IN - string to convert */
		  int charset);	/* IN - The current character set */

extern void fromIso(char copy[], /* OUT - Mapped string */
		    char original[]); /* IN - string to convert */

extern void toNative(char copy[], /* OUT - Mapped string */
		     char original[], /* IN - string to convert */
		     int charset); /* IN - current character set */

extern int littleEndian(void);

extern char *baseNameStart(char *fullPathName);

#endif                          /* -- sysdep.h -- */

