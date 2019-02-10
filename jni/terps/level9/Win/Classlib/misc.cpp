// Misc useful routines **********************************

#include <mywin.h>
#pragma hdrstop

#include <stdlib.h>

#include <string.h>
#include <dos.h>
#include <stdio.h>

#ifdef WIN32
#include <shlobj.h>

#else
#include <dir.h>
#endif

char *strdup(char *s)
{
	if (s==NULL) return NULL;
	char *t=new char[strlen(s)+1];
	if (t) strcpy(t,s);
	return t;
}

void EnableDlgButton(HWND hDlg,int ID, BOOL Enable) {
HWND hw=GetDlgItem(hDlg,ID);
EnableWindow(hw,Enable);
}

void DrawBitmap(HDC hDC, HBITMAP hBitmap) {
// need grey brush to remove white edges when using Ctrl3d
BITMAP bm;
HDC hMemDC = CreateCompatibleDC(hDC);
GetObject(hBitmap, sizeof(BITMAP), &bm);
SelectObject(hMemDC, hBitmap);
BitBlt(hDC, 0,0, bm.bmWidth, bm.bmHeight, hMemDC, 0, 0, SRCCOPY);
DeleteDC(hMemDC);
}

void StretchBitmap(HDC hDC, int dw, int dh,HBITMAP hBitmap) {
BITMAP bm;
HDC hMemDC = CreateCompatibleDC(hDC);
GetObject(hBitmap, sizeof(BITMAP), &bm);
SelectObject(hMemDC, hBitmap);
StretchBlt(hDC, 0,0,dw,dh, hMemDC, 0,0, bm.bmWidth, bm.bmHeight, SRCCOPY);
DeleteDC(hMemDC);
}

// Establishes a set of custom colors
COLORREF  CustCols[]={
	RGB(255,0,0),RGB(0,255,0),RGB(255,255,0), RGB(0,0,255),
	RGB(255,0,255),RGB(0,255,255),RGB(255,255,255), RGB(0,0,0),
	RGB(127,0,0),RGB(0,127,0),RGB(127,127,0), RGB(0,0,127),
	RGB(127,0,127),RGB(0,127,127),RGB(127,127,127), RGB(0,0,0) };

BOOL GetColor(HWND HWnd, COLORREF &Col) {
CHOOSECOLOR CC;
CC.lStructSize = sizeof(CHOOSECOLOR);
CC.hwndOwner=HWnd;
CC.rgbResult=Col;
CC.lpCustColors=CustCols;
CC.Flags=CC_RGBINIT;
if (ChooseColor(&CC)) {
	Col=CC.rgbResult;
	return TRUE;
	}
return FALSE;
}

BOOL OK2Save(char *s,char * Title) {
	char temp[255];
	FILE *f=fopen(s,"r");
	if (f!=NULL) {
		fclose(f);
		sprintf(temp,"File \"%s\" exists. OK to overwrite?",s);
		if (MessageBox(0,temp,Title,MB_YESNOCANCEL | MB_ICONQUESTION)!=IDYES) return FALSE;
		}
	// what if read only file ?
	f=fopen(s,"w");
	if (f) fclose(f);
	else {
		sprintf(temp,"Unable to write to file :%s",s);
		MessageBox(0,temp,Title,MB_OK | MB_ICONEXCLAMATION);
		return FALSE;
		}
	return TRUE;
}
/*
BOOL GetSaveFName(HWND hWnd,char *Buffer,int BufSize,const char *Filters,int &Fi,char *Title,DWORD Flags)
{
	if (GetFile(hWnd,Buffer,BufSize,Filters,Fi,Title,Flags))
	{
		if (OK2Save(Buffer,"")) return TRUE;
	}
	return FALSE;
}
*/
void DeleteFile(char *fname)
{
	OFSTRUCT of;
	OpenFile(fname,&of,OF_DELETE);
}

BOOL DirExist(char *path)
{
#ifdef WIN32
	DWORD dw=GetFileAttributes(path);
	return (dw!=DWORD(-1) && (dw & FILE_ATTRIBUTE_DIRECTORY));
#else
	if (strlen(path)<=3) return TRUE; // for "c:\\"
	char temp[MAX_PATH];
	strcpy(temp,path);
	if (temp[strlen(temp)-1]=='\\') temp[strlen(temp)-1]=0;
	ffblk ff;
	int h=findfirst(temp,&ff,FA_DIREC);
	return h==0;
#endif
}

BOOL CheckPath(HWND hw,char *Path,char *Title,BOOL Query)
{
	//check path ok
	FName P(Path,""),temp;
	if (DirExist(P)) return TRUE;
	if (Query && MessageBox(hw,String()<<"Path \""<<Path<<"\" does not exist! Create?",Title,MB_YESNO | MB_ICONQUESTION)==IDNO) return FALSE;

	int n=P.Pos('\\');
	do
	{
		strncpy(temp,P,n);
		temp[n]=0;
		if (!DirExist(temp) &&
#ifdef WIN32
			!CreateDirectory(temp,NULL))
#else
			mkdir(temp)<0)
#endif
		{
			MessageBox(hw,String()<<"Error creating directory!\n\r"<<temp,Title,MB_OK | MB_ICONEXCLAMATION);
			return FALSE;
		}
		n=P.Pos('\\',n+1);
	} while (n>=0);
	return TRUE;
}

void GetTempFName(char *Pre,FName &F)
{
	#ifdef WIN32
	FName Path;
	GetTempPath(MAX_PATH,Path);
	GetTempFileName(Path,Pre,0,F);
	#else
//	int Dr=GetTempDrive();
	GetTempFileName(0,Pre,0,F);
	#endif
}

long filelength(FILE *f)
{
	long pos=ftell(f);
	fseek(f,0,SEEK_END);
	long size=ftell(f);
	fseek(f,pos,SEEK_SET);
	return size;
}

#ifndef WIN32

#define MAXREAD 0xff00L

long fread(bhp ptr,int size,long num,FILE *f)
{
	long bytes=num*size,read;
	do
	{
		read=fread((void*) ptr,(int) 1,(int) min(bytes,MAXREAD),f);
		ptr+=read;
		bytes-=read;
	} while (bytes || read==0);
	return num-bytes/size;
}
#endif


#ifdef WIN32

int CALLBACK SetSelProc(HWND hWnd,UINT uMsg,LPARAM,LPARAM lpData)
{
	if (uMsg==BFFM_INITIALIZED) SendMessage(hWnd,BFFM_SETSELECTION,TRUE,lpData);
		return 0;
}

BOOL SelectDirectory(HWND hw,FName &Dir,char *Title)
{
	LPMALLOC pMalloc;
	BOOL ret=FALSE;
	if (SUCCEEDED(SHGetMalloc(&pMalloc)))
	{
		BROWSEINFO bi;
		bi.hwndOwner=hw;
		bi.pidlRoot=NULL;
		bi.pszDisplayName=Dir;
		bi.lpszTitle=Title;
		bi.ulFlags=BIF_RETURNONLYFSDIRS;
		bi.lpfn=SetSelProc;
		bi.lParam=(LPARAM) (char*) Dir;
		bi.iImage=0;

		ITEMIDLIST *iList=SHBrowseForFolder(&bi);
		if (iList)
		{
			ret=SHGetPathFromIDList(iList,Dir);
			pMalloc->Free(iList);
		}
		pMalloc->Release();
	}

	return ret;
}

#endif
