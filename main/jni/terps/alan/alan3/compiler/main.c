/*----------------------------------------------------------------------*\

  main.c

  Alan compiler main program unit - command-line version

\*----------------------------------------------------------------------*/

#include <locale.h>

#include "util.h"
#include "alan.h"
#include "spa.h"
#include "options.h"
#include "alan.version.h"
#include "lst_x.h"


/*======================================================================*/

static SPA_FUN(usage)
{
    printf("Usage: ALAN <adventure> [-help] [options]\n");

}

static SPA_ERRFUN(paramError)
{
  char *sevstr;

  switch (sev) {
  case 'E': sevstr = "error"; break;
  case 'W': sevstr = "warning"; break;
  default: sevstr = "internal error"; break;
  }
  printf("Parameter %s: %s, %s\n", sevstr, msg, add);
  usage(NULL, NULL, 0);
  terminate(EXIT_FAILURE);
}

static SPA_FUN(extraArg)
{
  printf("Extra argument: '%s'\n", rawName);
  usage(NULL, NULL, 0);
  terminate(EXIT_FAILURE);
}

static SPA_FUN(xit) {terminate(EXIT_SUCCESS);}

static SPA_FUN(addInclude)
{
  /* Add the include path to our list */
  importPaths = concat(importPaths, spaArgument(1), STRING_LIST);
  /* Now we can skip the include path */
  spaSkip(1);
}

static char *charsets[] = {"iso", "mac", "dos"};


static SPA_DECLARE(arguments)
#ifdef __dos__
     SPA_STRING("adventure", "file name, default extension '.ala'", srcptr, NULL, NULL)
#else
     SPA_STRING("adventure", "file name, default extension '.alan'", srcptr, NULL, NULL)
#endif
     SPA_FUNCTION("", "extra argument", extraArg)
SPA_END

static SPA_DECLARE(options)
     SPA_HELP("help", "this help", usage, xit)
     SPA_FLAG("verbose", "verbose messages", verboseFlag, FALSE, NULL)
     SPA_FLAG("warnings", "[don't] show warning messages", warningFlag, TRUE, NULL)
     SPA_FLAG("infos", "[don't] show informational messages", infoFlag, FALSE, NULL)
     SPA_FUNCTION("include <path>", "additional directory to search after current when\nlooking for imported files (may be repeated)", addInclude)
     SPA_FUNCTION("import <path>", "additional directory to search after current when\nlooking for imported files (may be repeated)", addInclude)
     SPA_KEYWORD("charset <set>", "which character set source is in (iso|mac|dos)", charset, charsets, 0, NULL)
     SPA_FLAG("ide", "list messages in a format appropriate for AlanIDE\n", ideFlag, FALSE, NULL)
     SPA_FLAG("cc", "show messages on the screen in old 'cc' format\n", ccFlag, FALSE, NULL)
     SPA_FLAG("full", "full source in the list file (or on screen)", fullFlag, FALSE, NULL)
     SPA_INTEGER("height <lines>", "height of pages in list file", lcount, 74, NULL)
     SPA_INTEGER("width <characters>", "width of pages in list file", ccount, 112, NULL)
     SPA_FLAG("listing", "create listing file", listingFlag, FALSE, NULL)
     SPA_FLAG("debug", "force debug option in adventure", debugFlag, FALSE, NULL)
     SPA_FLAG("pack", "force pack option in adventure", packFlag, FALSE, NULL)
     SPA_FLAG("summary", "print a summary", summaryFlag, FALSE, NULL)
#ifdef WINGUI
     SPA_FLAG("gui", "use gui", guiMode, TRUE, NULL)
#endif
     SPA_BITS("dump", "dump the internal form, where\n\
synonyms\n\
parameter mapping table\n\
syntaxes\n\
symbols\n\
verbs\n\
classes\n\
instances\n\
containers\n\
events\n\
rules\n\
everything\n\
include pointer addresses\n\
after parse (will abort after dump)\n\
after analysis (will abort after dump)\n\
after code generation", dumpFlags, "ypxsvciker!a123", NULL, NULL)
     SPA_FLAG("xml", "output an XML representation of the game (experimental)", xmlFlag, FALSE, NULL)
SPA_END

/*======================================================================*\
  MAIN
\*======================================================================*/
#ifdef WINGUI
#include <windows.h>
#include <shlwapi.h>

#define FILENAMESIZE 1000
static char inFileName[FILENAMESIZE];
static char fullInFileName[FILENAMESIZE];

static OPENFILENAME ofn;

static int getInFileName() {
  static char filter[] =
    "Alan Source Files (*.alan)\0*.alan\0"\
    "ALA Source Files (*.ala)\0*.ala\0"\
    "All Files (*.*)\0*.*\0\0";

  ofn.lStructSize = sizeof(OPENFILENAME);
  ofn.hInstance = NULL;
  ofn.lpstrFilter = filter;
  ofn.lpstrCustomFilter = NULL;
  ofn.lpstrFile = fullInFileName;
  ofn.lpstrTitle = "Choose an Alan V3 source file to compile";
  ofn.nMaxFile = FILENAMESIZE;
  ofn.lpstrFileTitle = inFileName;
  ofn.nMaxFileTitle = FILENAMESIZE;
  ofn.Flags = OFN_HIDEREADONLY;
  return GetOpenFileName(&ofn);
}

static int argc;
static char *argv[10];

static int splitCommandLine(char commandLine[])
{
  char *start = commandLine;
  char *end = strpbrk(commandLine, " \"");

  argv[0] = "alan";
  argc = 1;
  while (end) {
    argv[argc] = start;
    if (*end == '\"') {
      end = strpbrk(start+1, "\"");
      end++;
    }
    *end = '\0';
    start = end+1;
    while (*start == ' ') start++;
    end = strpbrk(start, " ");
    argc++;
  }
  if (*start != '\0') {
    argv[argc] = start;
    argc++;
  }
  return argc;
}

static char *removeExeResidue(char cmdLine[])
{
    /* MingW seems to forget to strip of the whole program name if it
       contains spaces, Windows surrounds those with quote-marks so any
       residue will end in: */
    static char *residue = ".exe\"";
    char *cp = strstr(cmdLine, residue);
    if (cp) {
        MessageBox(NULL, "INTERNAL: Had to strip exe.residue...", "Alan V3 compiler", MB_OK);
        cp += strlen(residue);
        while (*cp == ' ') cp++;
    } else
        cp = cmdLine;
    return cp;
}


static void remapWindowsFilename(char string[])
{
  if (string[0] == '"') {
    strcpy(string, &string[1]);
    string[strlen(string)-1] = '\0';
  }

#ifdef REMAPSLASH
  for (int i = 0; string[i] != '\0'; i++)
    if (string[i] == '\\')
      string[i] = '/';
#endif
}

int WINAPI WinMain(HINSTANCE instance, HINSTANCE prevInstance, PSTR cmdLine, int cmdShow)
{
    int nArgs;

    nArgs = splitCommandLine(removeExeResidue(cmdLine));

#ifdef ARGSDISPLAY
    MessageBox(NULL, "Hello!", "Alan V3 compiler", MB_OK);
    for (int i = 0; i < nArgs; i++) {
        char buf[199];
        sprintf(buf, "arg %d :\"%s\"", i, argv[i]);
        MessageBox(NULL, buf, "Alan V3 compiler", MB_OK);
    }
#endif

    if (nArgs == 1) {
        if (!getInFileName())
            return -1;
        argv[1] = fullInFileName;
        argc = 2;
    } else
        /* If we run from a CMD windows we will see Windows-style filenames */
        remapWindowsFilename(argv[1]);

#ifdef ARGSDISPLAY
    MessageBox(NULL, argv[1], "Alan V3 compiler : argv[1]", MB_OK);
    MessageBox(NULL, fullInFileName, "Alan V3 compiler : fullInFileName", MB_OK);
    MessageBox(NULL, inFileName, "Alan V3 compiler : inFileName", MB_OK);
    MessageBox(NULL, fopen(argv[1], "r")!=NULL?"OK":"Not Ok", "Alan V3 compiler : open argv[1]", MB_OK);
#endif

    /* -- get arguments -- */
    nArgs = spaProcess(argc, argv, arguments, options, paramError);

    if (guiMode) {
        if (AllocConsole())
            freopen("con:", "w", stdout);
        else
            MessageBox(NULL, "Failed to allocate a console.\nCompilation will continue but can not display error messages.", "Error", MB_OK);
    }

    char directory[500];
    strcpy(directory, argv[1]);
    PathRemoveFileSpec(directory);
    SetCurrentDirectory(directory);

#else

int main(int argc,		/* IN - argument count */
         char **argv		/* IN - program arguments */
)
{
    int nArgs;			/* Number of supplied args */

    /* Pick up any locale settings */
    setlocale(LC_ALL, "");

    /* -- get arguments -- */
    nArgs = spaProcess(argc, argv, arguments, options, paramError);
#endif

    /* Say hello ! */
    if (verboseFlag) {
#if (BUILD+0) != 0
        printf("%s\n", version_string(BUILD));
#else
        printf("%s\n", version_string(0));
#endif
    }

    if (nArgs == 0) {
        usage(NULL, NULL, 0);
        terminate(EXIT_FAILURE);
    } else if (nArgs > 1)
        terminate(EXIT_FAILURE);

    compile();
    return 0;
}
