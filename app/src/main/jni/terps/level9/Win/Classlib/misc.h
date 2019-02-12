// misc.h
#include <stdio.h>
#include <fname.h>

void EnableDlgButton(HWND hDlg,int ID, BOOL Enable);
inline void swap(int &A,int &B) {int temp=A; A=B; B=temp;}  // use macro ??
inline char *YesNo(BOOL yn) { return yn ? "Yes" : "No"; }
BOOL OK2Save(char *s,char * Title);
BOOL GetSaveFName(HWND hWnd,char *Buffer,int BufSize,const char *Filters,char *Title,DWORD Flags);
void DeleteFile(char *fname);
BOOL CheckPath(HWND hw,char *Path,char *Title,BOOL Query);
char *strdup(char *s);
BOOL GetColor(HWND HWnd, COLORREF &Col);
void DrawBitmap(HDC hDC, HBITMAP hBitmap);
void GetTempFName(char *Pre,FName &F);

long filelength(FILE *f);
#ifdef WIN32
BOOL SelectDirectory(HWND hw,FName &Dir,char *Title);
#else
long fread(bhp ptr,int size,long num,FILE *f);
#endif

