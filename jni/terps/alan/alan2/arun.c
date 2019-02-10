/*----------------------------------------------------------------------*\

  ARUN.C

  Main program for interpreter for ALAN Adventure Language


\*----------------------------------------------------------------------*/

#include "main.h"
#include "term.h"
#ifdef HAVE_SHORT_FILENAMES
#include "av.h"
#else
#include "alan.version.h"
#endif
#include "args.h"

#ifdef GLK
#include "glkio.h"
#ifdef __win__
#include "WinGlk.h"
#else
#include "glk.h"
#endif
#endif

/*======================================================================

  main()

  Main program of main unit in Alan interpreter module, ARUN

  */

#ifdef GLK
#ifdef _PROTOTYPES_
void glk_main(void)
#else
void glk_main()
#endif
#else
#ifdef _PROTOTYPES_
int main(
     int argc,
     char *argv[]
)
#else
int main(argc, argv)
     int argc;
     char *argv[];
#endif
#endif
{
#ifdef MALLOC
  malloc_debug(2);
#endif

  /* Set up page format in case we get a system error */
  lin = col = 1;
  header->paglen = 24;
  header->pagwidth = 70;

  getPageSize();

#ifdef GLK
  /* args() is called from glkstart.c */
#else
  args(argc, argv);
#endif

  if (dbgflg||verbose) {
    if (dbgflg) printf("<");
    printf("Arun, Adventure Interpreter version %s (%s %s)",
	   alan.version.string, alan.date, alan.time);
    if (dbgflg) printf(">");
    newline();
  }
  
  if (advnam == NULL || *advnam == 0) {
#ifdef WINGLK
    char *filename;
    filename = (char*)winglk_get_initial_filename(NULL, "Arun : Select an Alan game file",
		"Alan v2 Game Files (*.acd)|*.acd||");
    if (filename) {
      char *directoryPart;
      if (((directoryPart = strrchr(filename, '\\')) == NULL)
	  && ((directoryPart = strrchr(filename, ':')) == NULL))
	advnam = strdup(filename);
      else
	advnam = strdup(directoryPart+1);
      advnam[strlen(advnam)-4] = '\0'; /* Strip off .A3C */
    } else {
      printf("You should supply a game file to play.\n");
      usage();
      terminate(0);
    }
#else
    printf("You should supply a game file to play.\n");
    usage();
    terminate(0);
#endif
  }

#ifdef WINGLK
  winglk_window_set_title(advnam);
#endif

  run();

#ifdef GLK
  return;
#else
  return(EXIT_SUCCESS);
#endif
}

