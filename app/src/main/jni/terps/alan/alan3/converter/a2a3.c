/*----------------------------------------------------------------------*\

	Alan v2 to v3 converter

	Main program

\*----------------------------------------------------------------------*/

#include <stdio.h>
#include "a2a3.h"

#include "lmList.h"
#include "smScan.h"
#include "pmParse.h"


/* PUBLIC */
int fileNo = 0;

#define FILENAMESIZE 1000
static char inFileName[FILENAMESIZE];

FILE *outFile;

#ifdef WINGUI
#include <windows.h>
#include <direct.h>

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
  ofn.lpstrTitle = "Choose an Alan V2 source file to convert to V3 syntax";
  ofn.nMaxFile = FILENAMESIZE;
  ofn.lpstrFileTitle = inFileName;
  ofn.nMaxFileTitle = FILENAMESIZE;
  ofn.Flags = OFN_HIDEREADONLY;
  return GetOpenFileName(&ofn);
}

static int getOutFileName() {
  ofn.lpstrFile = fullOutFileName;
  ofn.lpstrFileTitle = outFileName;
  ofn.lpstrTitle = "Choose a file to store Alan V3 source output in";
  ofn.Flags = OFN_OVERWRITEPROMPT;
  return GetSaveFileName(&ofn);
}

static char *argv[10];

static int splitCommandLine(char commandLine[])
{
  char *start = commandLine;
  char *end = strpbrk(commandLine, " \"");
  int i = 0;

  while (end) {
    argv[i] = start;
    if (*end == '\"') {
      end = strpbrk(start+1, "\"");
      end++;
    }
    *end = '\0';
    start = end+1;
    while (*start == ' ') start++;
    end = strpbrk(start, " ");
    i++;
  }
  if (*start != '\0') {
    argv[i] = start;
    i++;
  }
  return i;
}

#ifdef WINGUI
static void setDirectory(char fileName[])
{
  char *directory = strdup(fileName);
  char *endOfPath = strrchr(directory, '\\');

  if (endOfPath != NULL) {
    *endOfPath = '\0';
    _chdir(directory);
  }
}
#endif


static char *removeExeResidue(char cmdLine[])
{
  /* MingW seems to forget to strip of the whole program name if it
     contains spaces, Windows surrounds thoose with quote-marks so any
     residue will end in: */
  static char *residue = ".exe\"";
  char *cp = strstr(cmdLine, residue);
  if (cp) {
    cp += strlen(residue);
    while (*cp == ' ') cp++;
  } else
    cp = cmdLine;
  return cp;
}


int WINAPI WinMain(HINSTANCE instance, HINSTANCE prevInstance, PSTR cmdLine, int cmdShow)
{
  int args;

  args = splitCommandLine(removeExeResidue(cmdLine));

#ifdef SHOWARGS
  for (int i = 0; i < args; i++) {
    char buf[199];
    sprintf(buf, "arg %d :\"%s\"", i, argv[i]);
    MessageBox(NULL, buf, "Alan V2 to V3 converter", MB_OK);
  }
#endif

  if (args>2) {
    printf("Can't have more than two arguments.");
    return -1;
  }

  if (args<1) {
    if (!getInFileName())
      return 0;
  } else
    strcpy(fullInFileName, argv[0]);

  if (args<2)
    if (getOutFileName())
      outFile = fopen(fullOutFileName, "w");
    else
      outFile = stdout;
  else
    outFile = fopen(argv[1], "w");
  if (outFile == NULL) {
    printf("Could not open output file");
    exit(-1);
  }

#ifdef WINGUI
  setDirectory(fullInFileName);
#endif

  lmLiInit("", "", lm_ENGLISH_Messages);
  if (!smScanEnter(fullInFileName, FALSE)) {
    printf("Could not open source file");
    exit(-1);
  } else {
    pmParse();
  }
  return 0;
}

#else

int main(int argc, char* argv[]) {
  strcpy(inFileName, argv[1]);
  outFile = stdout;
  lmLiInit("", "", lm_ENGLISH_Messages);
  if (!smScanEnter(inFileName, FALSE)) {
    printf("Could not open source file");
    exit(-1);
  } else {
    pmParse();
  }
  return 0;
}
#endif
