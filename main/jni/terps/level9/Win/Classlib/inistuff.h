// inistuff.h

#include <stringc.h>

BOOL ReadIniString(const char *Section,const char *Item,String &S,BOOL Global=FALSE);
void WriteIniString(const char *Section,const char *Item,char *Val,BOOL Global=FALSE);
void ReadIniBool(const char *Section,const char *Item,BOOL &Bool,BOOL Global=FALSE);
void WriteIniBool(const char *Section,const char *Item,BOOL Bool,BOOL Global=FALSE);
void ReadIniInt(const char *Section,const char *Item,int &Int,BOOL Global=FALSE);
void WriteIniInt(const char *Section,const char *Item,int Int,BOOL Global=FALSE);
void ReadIniInt(const char *Section,const char *Item,long &Int,BOOL Global=FALSE);
void WriteIniInt(const char *Section,const char *Item,long Int,BOOL Global=FALSE);

void SetMru(HWND,UINT);
void ReadMru(int);
void AddToMru(char*);
void AddToMru2(char*);
void WriteMru();
extern SimpleList<String> MruList;
extern SimpleList<String> MruList2;
#define MRU_ID 300
#define MRU2_ID 350
#define MRU_MAX 9
