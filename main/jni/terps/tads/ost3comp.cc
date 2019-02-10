/* This file implements some of the functions described in
 * tads2/osifc.h.  Only functions needed by the TADS 3 compiler are
 * implemented here.
 *
 * The functions are "portable"; they don't make use of curses/ncurses.
 */
#include <stdio.h>
#include <string.h>

#ifdef HAVE_GLOB_H
#include <glob.h>
#endif

#include "os.h"

/* Set the game title.
 *
 * Does nothing in the compiler.  This should actually be implemented in
 * tads3/os_stdio.cpp, but for some reason it's not.
 */
void
os_set_title( const char* )
{
}

/* Initialize.
 */
int
os_init( int*, char**, const char*, char*, int )
{
    return 0;
}

/* =====================================================================
 *
 * The functions defined below are not needed by the interpreter, or
 * have a curses-specific implementation and are therefore only used
 * when building the compiler (the compiler doesn't use curses, just
 * plain stdio).
 */

/* Read a character from the keyboard and return the low-level,
 * untranslated key code whenever possible.
 */
int
os_getc_raw( void )
{
    return getchar();
}

