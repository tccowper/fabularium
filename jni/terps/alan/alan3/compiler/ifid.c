/*----------------------------------------------------------------------*\

  IFID.C
  IFID handling

\*----------------------------------------------------------------------*/

#include "ifid_x.h"

#include <stdio.h>
#ifdef __POSIX__
#include <sys/time.h>
#else
#include <sys/timeb.h>
#endif

#include "srcp_x.h"
#include "str.h"
#include "lmList.h"
#include "dump_x.h"
#include "util.h"
#include "emit.h"
#include "adv_x.h"




/*======================================================================*/
IfidNode *newIfid(Srcp srcp, char *name, char *value)
{
  IfidNode *new;			/* The newly allocated area */

  progressCounter();

  new = NEW(IfidNode);

  new->srcp = srcp;
  new->name  = newString(name);
  new->value = value;

  return(new);
}




/*======================================================================*/
static void fillRandomBytes(char buffer[], int nbytes)
{
  static int initted = 0;
#ifdef __POSIX__
  struct timeval times;
#else
  struct timeb times;
#endif
  long time_now;
  int i;

  if (!initted) {
#ifdef __POSIX__
    gettimeofday(&times, NULL);
    time_now = times.tv_usec;
#else
    ftime(&times);
    time_now = times.millitm;
#endif
    srand(time_now);
    initted = 1;
  }

  for (i = 0; i < nbytes; i++) {
    *buffer++ = rand() & 0xFF;
  }
}



/*======================================================================*/
static char *randomUUID()
{
  char buffer[16];
  int b, s;
  static char string[46];	/* 32 hexdigits, 4 dashes, 9 "UUID:////"
				   00112233-4455-6677-8899-aabbccddeeff */
  int i, j;

  fillRandomBytes(buffer, 16); b = 0;
  sprintf(string, "UUID://"); s = 7;
  for (i = 0; i < 4; i++, s+=2, b++)
    sprintf(&string[s], "%2.2x", buffer[b]);
  strcat(string, "-"); s++;
  for (j = 0; j < 3; j++) {
    for (i = 0; i < 2; i++, s+=2, b++)
      sprintf(&string[s], "%2.2x", buffer[b]);
    strcat(string, "-"); s++;
  }
  for (i = 0; i < 6; i++, s+=2, b++)
    sprintf(&string[s], "%2.2x", buffer[b]);
  strcat(string, "//");
  return string;
}


/*======================================================================*/
static Bool isValidUUID(char *uuid)
{
  int i;

  if (strlen(uuid) != 45) return FALSE;
  if (strncmp(uuid, "UUID://", 7) != 0) return FALSE;
  for (i = 7; i <= 14; i++) if (!isxdigit((uint)uuid[i])) return FALSE;
  if (uuid[15] != '-') return FALSE;
  for (i = 16; i <= 19; i++) if (!isxdigit((uint)uuid[i])) return FALSE;
  if (uuid[20] != '-') return FALSE;
  for (i = 21; i <= 24; i++) if (!isxdigit((uint)uuid[i])) return FALSE;
  if (uuid[25] != '-') return FALSE;
  for (i = 26; i <= 29; i++) if (!isxdigit((uint)uuid[i])) return FALSE;
  if (uuid[30] != '-') return FALSE;
  for (i = 31; i <= 42; i++) if (!isxdigit((uint)uuid[i])) return FALSE;
  if (strcmp(&uuid[43], "//") != 0) return FALSE;
  return TRUE;
}


/*======================================================================*/
static char *readOrCreateIFID()
{
  char ifidfnm[255] = "";
  FILE *ifidFile;
  static char buffer[46];	/* 32 hexdigits, 4 dashes, 9 "UUID:////" */

  /* -- create IFID file name -- */
  if (adv.name)
    strcpy(ifidfnm, adv.name);
  strcat(ifidfnm, ".ifid");

  if ((ifidFile = fopen(ifidfnm, "r")) != NULL)
    fread(buffer, 45, 1, ifidFile);
  else
    buffer[0] = '\0';
  if (!isValidUUID(buffer)) {
    strcpy(buffer, randomUUID());
    if ((ifidFile = fopen(ifidfnm, "w")) != NULL)
      fwrite(buffer, 45, 1, ifidFile);
  }

  return buffer;
}


/*======================================================================*/
List *initIfids()
{
  return(newList(newIfid(nulsrcp, newString("IFID"), readOrCreateIFID()), IFID_LIST));
}


/*======================================================================*/
static void generateIfid(IfidNode *ifid)
{
  ifid->nameAddress = nextEmitAddress();
  emitString(ifid->name);
  ifid->valueAddress = nextEmitAddress();
  emitString(ifid->value);
}


/*======================================================================*/
Aaddr generateIfids(List *ifids)
{
  List *lst;
  Aaddr ifidAddress = nextEmitAddress();

  ITERATE(lst, ifids)
    generateIfid(lst->member.ifid);

  ITERATE(lst, ifids) {
    emit(lst->member.ifid->nameAddress);
    emit(lst->member.ifid->valueAddress);
  }
  emit(EOF);
  return ifidAddress;
}


/*======================================================================*/
void dumpIfid(IfidNode *ifid)
{
  if (ifid == NULL) {
    put("NULL");
    return;
  }

  put("IFID: "); dumpSrcp(ifid->srcp);
  dumpString(ifid->name); put("="); dumpString(ifid->value);
}
