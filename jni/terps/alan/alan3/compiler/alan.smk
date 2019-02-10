%%OPTIONS

	Prefix 'sm';
	Pack RDS, GCS, LES, ERROR;

%%IMPORT

/* For open, read & close */
#include <fcntl.h>

#include "sysdep.h"

#include "types.h"

#include "alan.h"
#include "lmList.h"
#include "encode.h"
#include "util.h"

%%EXPORT

#include "sysdep.h"
#include "types.h"

#include "lst_x.h"

extern smScContext lexContext;

/* List of file names */
extern List *fileNames;

extern Bool smScanEnter(Srcp srcp, char fnm[], Bool search);
extern int scannedLines(void);
extern void setCharacterSet(int charset);

%%DECLARATIONS

#include "str.h"

#define COPYMAX (smThis->smLength>256?256:smThis->smLength)


/* PUBLIC */
smScContext lexContext = NULL;	/* Scanner context */

/* TODO: Move this to somewhere outside of scanner, like utils or main */
List *fileNames = NULL;

int scannedLines();


/* PRIVATE */
static int lines = 0;		/* Updated at end of each file */
#ifndef O_BINARY
#define O_BINARY 0
#endif

static Bool find_and_open_in_path_element(smScContext this, List *ip, char fnm[]) {
    char fnmbuf[300] = "";

    if (ip != NULL) {
        strcpy(fnmbuf, ip->member.str);
#ifndef __mac__
        if (fnmbuf[strlen(fnmbuf)-1] != '/')
            strcat(fnmbuf, "/");
#endif
    }
    strcat(fnmbuf, fnm);
    return (this->fd = open(fnmbuf, O_RDONLY|O_BINARY)) > 0;
}

static char *open_import(smScContext this, char fnm[], Bool search) {
    List *ip;

    if (search) {
        if (!find_and_open_in_path_element(this, NULL, fnm)) {
            for (ip = importPaths; ip != NULL; ip = ip->next) {
                if (find_and_open_in_path_element(this, ip, fnm))
                    break;
            }
            if (ip)
                return ip->member.str;    /* Return the prefix found */
            else
                return NULL;
        }
        
    } else {
        if (!find_and_open_in_path_element(this, NULL, fnm))
            return NULL;              /* Return not found */
    }
    return "";
}

static void register_filename(smScContext this, char *prefix, char filename[]) {
    char *full_name = allocate(strlen(prefix)+strlen(filename)+2);
    if (prefix != NULL) {
        strcpy(full_name, prefix);
        if (strlen(prefix) > 0 && prefix[strlen(prefix)-1] != '/')
            strcat(full_name, "/");
    }
    strcat(full_name, filename);   
    this->fileName = newString(full_name);
    fileNames = concat(fileNames, this->fileName, STRING_LIST);
    free(full_name);
}

static void switch_scanner(smScContext this) {
    this->fileNo = fileNo++;
    this->previous = lexContext;
    lexContext = this;
}



Bool smScanEnter(Srcp srcp,     /* IN - The source position of the import statement */
                 char fnm[],	/* IN - Name of file to open */
                 Bool search	/* IN - Search the import paths? */
){
    smScContext this;
    List *p;
    char *prefix = "";

    for (p = fileNames; p != NULL; p = p->next) {
        if (strcmp(fnm, p->member.str) == 0) {
            lmLog(&srcp, 197, sevWAR, fnm);
            return FALSE;
        }
    }

    this = smScNew(sm_MAIN_MAIN_Scanner);
    if (fnm == NULL)
        this->fd = 0;
    else {
        if (!(prefix = open_import(this, fnm, search))) {
            lmLog(&srcp, 199, sevFAT, fnm);
            return FALSE;
        }
    }

    register_filename(this, prefix, fnm);
    switch_scanner(this);

    return TRUE;
}

int scannedLines(void)
{
  return(lines - 1);
}


extern unsigned char *smMap;
extern unsigned char *smDFAcolVal;
extern unsigned char *smDFAerrCol;
extern unsigned char smIsoMap[256];
extern unsigned char smIsoDFAcolVal[256];
extern unsigned char smIsoDFAerrCol[256];
extern unsigned char smMacMap[256];
extern unsigned char smMacDFAcolVal[256];
extern unsigned char smMacDFAerrCol[256];
extern unsigned char smDosMap[256];
extern unsigned char smDosDFAcolVal[256];
extern unsigned char smDosDFAerrCol[256];

static int currentCharSet;

void setCharacterSet(int set)
{
  currentCharSet = set;
  switch (set) {
  case 0:
    smMap = &smIsoMap[0];
    smDFAcolVal = &smIsoDFAcolVal[0];
    smDFAerrCol = &smIsoDFAerrCol[0];
    break;
  case 1:
    smMap = &smMacMap[0];
    smDFAcolVal = &smMacDFAcolVal[0];
    smDFAerrCol = &smMacDFAerrCol[0];
    break;
  case 2:
    smMap = &smDosMap[0];
    smDFAcolVal = &smDosDFAcolVal[0];
    smDFAerrCol = &smDosDFAerrCol[0];
    break;
  }
}


%%CONTEXT

  smScContext previous;
  int fd;
  char *fileName;
  int fileNo;


%%READER

  return read(smThis->fd, (char *)smBuffer, smLength);

%%POSTHOOK

  smToken->srcp.file = smThis->fileNo;
  smToken->srcp.startpos = smThis->smPosition;
  smToken->srcp.endpos = smThis->smNextPosition;
  if (smToken->code == sm_MAIN_ENDOFTEXT_Token) {
    lines += smThis->smLine;
    close(smThis->fd);
    if (smThis->previous) {
      lexContext = smThis->previous;
      smScDelete(smThis);
      return smScan(lexContext, smToken);
      }
  }

%%MAP

  [A-Z]		= [a-z];
  [\xC0-\xD6]	= [\xe0-\xf6];
  [\xD8-\xDE]	= [\xf8-\xfe];

%%DEFINITIONS

  letter	= [a-z\xdf\xe0-\xf6\xf8-\xfe];
  digit		= [0-9];

------------------------------------------------------------------------------
%%VOCABULARY main

%%SCANNER main

%%RULES

  Integer = digit+
    %%
	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, COPYMAX)] = '\0';
    %%;


  IDENTIFIER = letter (letter ! digit ! '_')*			-- normal id
    %%
	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, COPYMAX)] = '\0';
        if (currentCharSet != NATIVECHARSET)
          toNative(smToken->chars, smToken->chars, currentCharSet);
    %%;

  IDENTIFIER = '\'' ([^\'\n]!'\'''\'')* ('\'' ! '\n')		-- quoted id
    %%{
	char *c;

	/* If terminated by \n illegal! */
	if (smThis->smText[smThis->smLength-1] == '\n')
	  lmLog(&smToken->srcp, 152, sevERR, "");

	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 1, COPYMAX-1)] = '\0';
	/* Replace any doubled quotes by single */
	for (c = strchr(smToken->chars, '\''); c; c = strchr(c, '\'')) {
	    strmov(c, &c[1]);
	    c++;
	}
    }%%;


  STRING = '"' ([^"]!'"''"')* '"'
    %%
      int len = 0;		/* The total length of the copied data */
      Bool space = FALSE;
      int i, c;

      smToken->fpos = ftell(txtfil); /* Remember where it starts */
      smThis->smText[smThis->smLength-1] = '\0';
      if (currentCharSet != 0) /* Convert string from non ISO characters if needed */
        toIso((char *)&smThis->smText[1], (char *)&smThis->smText[1], currentCharSet);

      for (i = 1; i < smThis->smLength-1; i++) {
	/* Write the character */
	if (isspace(c = smThis->smText[i])) {
	  if (!space) {		/* Are we looking at spaces? */
	    /* No, so output a space and remember */
	    putc(' ', txtfil);
	    incFreq(' ');
	    space = TRUE;
	    len++;
	  }
        } else {
	  putc(c, txtfil);
	  incFreq(c);
	  space = FALSE;
	  len++;
	  if (c == '"') i++;	/* skip second '"' */
	}
      }
      smToken->len = len;
    %%;

  'import' = 'import' 
    %%
        Srcp srcp, start;
        Token token;
        static int i;
        static char c;

        smThis->smScanner = sm_MAIN_FILENAME_Scanner;
        smScan(smThis, `&token);		/* Get file name */
        smThis->smScanner = sm_MAIN_MAIN_Scanner;
        if (token.code == sm_MAIN_IDENTIFIER_Token) {
	        /* Found an ID which is a file name */

            /* Skip all whitespace */
            do {
                i = smScSkip(smThis, 1);
                c = smThis->smText[smThis->smLength-1];
            } while (isspace((int)c) && i > 0);

            if (c != '.') {
                lmLog(`&token.srcp, 109, sevERR, "expected terminating '.'");
                i = smScSkip(smThis, -1);
            }

            srcp = token.srcp;	/* Insert the file before next line */
            srcp.line++;
            srcp.col = 1;

            if (smScanEnter(token.srcp, token.chars, TRUE)) {
                start.file = fileNo-1;
                start.line = 0;	/* Start at beginning */
                lmLiEnter(`&srcp, `&start, lexContext->fileName);
            }
        } else
            lmLog(`&token.srcp, 151, sevFAT, token.chars); /* Not a file name */
  %%;

  'location' = 'location'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'actor' = 'actor'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'opaque' = 'opaque'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'visits' = 'visits'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'on' = 'on'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'it' = 'it'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'contains' = 'contains'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'of' = 'of'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'first' = 'first'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'into' = 'into'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'taking' = 'taking'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  'off' = 'off'
    %%
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, smThis->smLength)] = '\0';
    %%;

  Unknown = _Unknown;

  EndOfText = _EndOfText;

%%SKIP

  blanks = [ \r\n\t\x0d]+;

  comment = '--' [^\n\r]*;

  include = '$include' 
    %%
      Srcp srcp, start;
      Token token;
      int i;
      char c;

      smThis->smScanner = sm_MAIN_FILENAME_Scanner;
      smToken->srcp.startpos = smThis->smPosition;
      smToken->srcp.endpos = smThis->smNextPosition;
      smScan(smThis, `&token);		/* Get file name */
      smThis->smScanner = sm_MAIN_MAIN_Scanner;
      if (token.code == sm_MAIN_IDENTIFIER_Token) {
          /* Found an ID which is a file name */
          srcp = token.srcp;	/* Insert the file before next line */
          srcp.line++;
          srcp.col = 1;

          /* Skip to end of line or EOF */
          do {
              i = smScSkip(smThis, 1);
              c = smThis->smText[smThis->smLength-1];
          } while (c != '\n' && i != 0);

          lmLog(`&smToken->srcp, 154, sevERR, token.chars); /* INCLUDE is deprecated */

          if (smScanEnter(token.srcp, token.chars, TRUE)) {
              smToken->srcp.file = fileNo-1;
              start.file = fileNo-1;
              start.line = 0;	/* Start at beginning */
              lmLiEnter(`&srcp, `&start, lexContext->fileName);
              /* Use the new scanner to get next token and return it */
              return smScan(lexContext, smToken);
          }
      } else  /* Did not scan any file name */
          lmLog(`&token.srcp, 151, sevFAT, token.chars);
  %%;


%%SCANNER filename

%%RULES

  Identifier = '\'' ([^\']!'\'''\'')* '\''	-- quoted id
    %%{
        smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 1, COPYMAX-1)] = '\0';
    }%%;

  Unknown = _Unknown;

  EndOfText = _EndOfText;

%%SKIP

  blanks = [ \n\t]+;


