/*----------------------------------------------------------------------*\

	smScSema.c

	ScannerMaker generated semantic actions

\*----------------------------------------------------------------------*/

/* %%IMPORT */


#include "a2a3.h"
#include "lmList.h"

/* For open, read & close */
#ifdef __sun__
#include <unistd.h>
#include <fcntl.h>
#endif
#ifdef __vms__
#include <unixio.h>
#endif
#ifdef __dos__
#include <io.h>
#endif
#include <fcntl.h>


/* END %%IMPORT */
#include "smScan.h"

/* %%DECLARATION */


#include "str.h"

#define COPYMAX (smThis->smLength>9999?9999:smThis->smLength)


/* PUBLIC */
smScContext lexContext = NULL;	/* Scanner context */

List *fileNames = NULL;

int scannedLines();


/* PRIVATE */
static int lines = 0;		/* Updated at end of each file */

Bool smScanEnter(
		 char fnm[],	/* IN - Name of file to open */
		 Bool search	/* IN - Search the include paths */
){
  smScContext this;
  char fnmbuf[300] = "";

  this = smScNew(sm_MAIN_MAIN_Scanner);
  if (fnm == NULL)
    this->fd = 0;
  else {
    List *ip;

    if (search) {
      strcpy(fnmbuf, fnm);
#ifdef THINK_C
      if ((this->fd = open(fnmbuf, O_TEXT)) < 0) { /* Does automatic <cr> to <nl> conversion */
#else
      if ((this->fd = open(fnmbuf, 0)) < 0) {
#endif
	for (ip = NULL /*includePaths*/; ip != NULL; ip = ip->next) {
	  strcpy(fnmbuf, ip->element.str);
#ifndef __mac__
	  if (ip->element.str[strlen(ip->element.str)] != '/')
	    strcat(fnmbuf, "/");
#endif
	  strcat(fnmbuf, fnm);
#ifdef THINK_C
	  if ((this->fd = open(fnmbuf, O_TEXT)) > 0)
#else
	  if ((this->fd = open(fnmbuf, 0)) > 0)
#endif
	    break;
	}
	if (ip == NULL)
	  return FALSE;
      }
    } else {
      strcat(fnmbuf, fnm);
#ifdef THINK_C
      if ((this->fd = open(fnmbuf, O_TEXT)) < 0)
#else
      if ((this->fd = open(fnmbuf, 0)) < 0)
#endif
	return FALSE;
    }
  }

  /* Remember the filename */
  this->fileName = newString(fnmbuf);
  fileNames = concat(fileNames, this->fileName, STRING_LIST);
  this->fileNo = fileNo++;
  this->previous = lexContext;
  lexContext = this;

  return TRUE;
}

int scannedLines(void)
{
  return(lines - 1);
}


extern unsigned char *smMap;
extern unsigned char *smDFAcolVal;
extern unsigned char *smDFAerrCol;
extern unsigned char *smIsoMap;
extern unsigned char *smIsoDFAcolVal;
extern unsigned char *smIsoDFAerrCol;
extern unsigned char *smMacMap;
extern unsigned char *smMacDFAcolVal;
extern unsigned char *smMacDFAerrCol;
extern unsigned char *smDosMap;
extern unsigned char *smDosDFAcolVal;
extern unsigned char *smDosDFAerrCol;


/* END %%DECLARATION */

int smScReader(
     smScContext smThis,
     unsigned char *smBuffer,
     unsigned int smLength)
{


#ifdef __MWERKS__
 /* Metrowerks does not do automatic <cr> to <nl> conversion on text files!!! */
  {
    int count, pos;

    count = read(smThis->fd, (char *)smBuffer, smLength);
    for (pos = 0; pos < count; pos++)
      if (smBuffer[pos] == '\r')
	smBuffer[pos] = '\n';

    return count;
  }
#else
  return read(smThis->fd, (char *)smBuffer, smLength);
#endif


}    


int smScAction(
     smScContext smThis,
     int smInternalCode,
     Token *smToken)
{
  enum {
    smSkipToken		= -1,
    smContinueToken	= -2
  };
  switch(smInternalCode) {
  case 100:		/* INTEGER*/ 
    {
	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, COPYMAX)] = '\0';
    
}
    break;

  case 101:		/* IDENT*/ 
    {
	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, COPYMAX)] = '\0';
	(void) strlow(smToken->chars);
    
}
    break;

  case 102:		/* IDENT*/ 
    {{
	/* If terminated by \n illegal! */
	if (smThis->smText[smThis->smLength-1] == '\n')
	  lmLog(&smToken->srcp, 152, sevERR, "");

	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, COPYMAX)] = '\0';
    }
}
    break;

  case 103:		/* STRING*/ 
    {
	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 0, COPYMAX)] = '\0';
    
}
    break;

  case 108:		/* INCLUDE*/ 
    {
      Srcp srcp, start;
      Token token;
      int i;
      char c;

      smThis->smScanner = sm_MAIN_FILENAME_Scanner;
      smScan(smThis, &token);		/* Get file name */
      smThis->smScanner = sm_MAIN_MAIN_Scanner;
      if (token.code == sm_MAIN_IDENT_Token) {
	/* Found an ID which is a file name */
	do {
	  i = smScSkip(smThis, 1);
	  c = smThis->smText[smThis->smLength-1];
	} while (c != '\n' && i != 0); /* Skip to end of line or EOF */

	srcp = token.srcp;	/* Insert the file before next line */
	srcp.line++;
	srcp.col = 1;

	if (smScanEnter(token.chars, TRUE)) {
	  start.file = fileNo-1;
	  start.line = 0;	/* Start at beginning */
	  lmLiEnter(&srcp, &start, lexContext->fileName);
	  /* Use the new scanner to get next token and return it */
	  return smScan(lexContext, smToken);
	} else
	  lmLog(&token.srcp, 199, sevFAT, token.chars);
      } else
	lmLog(&token.srcp, 151, sevFAT, token.chars); /* Not a file name */
  
}
    break;

  case 109:		/* IDENT*/ 
    {{
	smToken->chars[smScCopy(smThis, (unsigned char *)smToken->chars, 1, COPYMAX-1)] = '\0';
    }
}
    break;
  }
  return smToken->code;
}

int smScPostHook(
     smScContext smThis,
     Token *smToken)
{
  enum {
    smSkipToken		= -1
  };


  smToken->srcp.file = smThis->fileNo;
  if (smToken->code == sm_MAIN_ENDOFTEXT_Token) {
    lines += smThis->smLine;
    close(smThis->fd);
    if (smThis->previous) {
      lexContext = smThis->previous;
      smScDelete(smThis);
      return smScan(lexContext, smToken);
      }
  }


  return smToken->code;
}

