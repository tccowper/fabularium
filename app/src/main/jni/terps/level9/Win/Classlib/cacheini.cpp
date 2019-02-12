#include <mywin.h>
#pragma hdrstop

#include <string.h>
#include <stdio.h>

#include <parse.h>

#include "cacheini.h"

CachedIni::CachedIni()
{
	LineList=Last=NULL;
	TotalSize=0;
}

CachedIni::CachedIni(bhp ptr,long size)
{
	LineList=Last=NULL;
	TotalSize=0;
	Parse P(ptr,size);
	do
	{
		AddLine(P.GetLine());
	} while (!P.Eof());
}

CachedIni::~CachedIni()
{
	Line *L=LineList,*L2;
	while (L)
	{
		L2=L->Next;
		delete L;
		L=L2;
	}
}

void CachedIni::AddLine(char *s)
{
	InsertLine(s,Last);
}

void CachedIni::DeleteLine(Line *L)
{
	if (L->Prev) L->Prev->Next=L->Next;
	else LineList=L->Next;
	if (L->Next) L->Next->Prev=L->Prev;
	else Last=L->Prev;
	delete L;
}

CachedIni::Line *CachedIni::InsertLine(char *s,Line *L)
{
	Line *newLine=new Line(s);
	TotalSize+=newLine->Len+1;

	if (L)
	{
		newLine->Next=L->Next;
		if (L->Next) L->Next->Prev=newLine;
		else Last=newLine;
		L->Next=newLine;
	}
	else
	{
		LineList=Last=newLine;
		newLine->Next=NULL;
	}
	newLine->Prev=L;
	return newLine;
}

bhp CachedIni::Write()
{
	bhp Buf=new huge BYTE[TotalSize];
	bhp ptr=Buf;
	Line *L=LineList;
	while (L)
	{
		ptr=L->Write(ptr);
		L=L->Next;
	}
	return Buf;
}

void CachedIni::Write(char *fName)
{
	FILE *f=fopen(fName,"wt");
	Line *L=LineList;
	while (L)
	{
		fprintf(f,"%s\n",L->Str);
		L=L->Next;
	}
	fclose(f);
}

BOOL CachedIni::Read(char*fname)
{
	FILE *f=fopen(fname,"rt");
	if (f==NULL) return FALSE;
	char temp[256];
	while (fgets(temp,256,f))
	{
		char *p=strchr(temp,'\n');
		if (p) *p=0;
		AddLine(temp);
	}
	fclose(f);
	return TRUE;
}

CachedIni::Line *CachedIni::FindSection(char *Section)
{
	char *Sect=new char[strlen(Section)+3];
	sprintf(Sect,"[%s]",Section);

	Line *L=LineList;
	while (L)
	{
		if (!stricmp(L->Str,Sect)) break;
		L=L->Next;
	}
	delete[] Sect;
	return L;
}

CachedIni::Line *CachedIni::FindItem(char *Item,Line *L)
{
	int len=strlen(Item);
	while (L && *L->Str!='[')
	{
		if (!strnicmp(L->Str,Item,len) && L->Str[len]=='=')
			return L;
		L=L->Next;
	}
	return NULL;
}

// fails if multiple matches eg device=
CachedIni::Line *CachedIni::FindItem2(char *Item,char *Full,Line *L)
{
//	int len=strlen(Item);
//	int nm=0;
//	Line *Match=NULL;
	while (L && *L->Str!='[')
	{
		if (Full && !stricmp(Full,L->Str)) return L; // exact match so exit
//		if (!strnicmp(L->Str,Item,len) && L->Str[len]=='=')
//		{
//			nm++;
//			Match=L;
//		}
		L=L->Next;
	}
//	return nm>1 ? NULL : Match;
	return NULL;
}


void CachedIni::AddToSection(char *Section,char *Item,char *Value)
{
	Line *L=FindSection(Section);
	char *Val=NULL;
	if (Value && *Value)
	{
		Val=new char[strlen(Item)+strlen(Value)+2];
		sprintf(Val,"%s=%s",Item,Value);
	}
	if (L)
	{
		 Line *I=FindItem2(Item,Val,L->Next);
		 if (I)
		 {
			if (Val) I->SetString(Val);
			else DeleteLine(I);
		 }
		 else if (Val)
		 {
			// find end of section
			Line *Prev;
			do
			{
				Prev=L;
				L=L->Next;
			} while (L && *L->Str!='[');
			while (*Prev->Str==0) Prev=Prev->Prev; // skip back blank lines

			InsertLine(Val,Prev);
		 }
	}
	else if (Val) // only section create if item not blank
	{
		char *Sect=new char[strlen(Section)+3];
		sprintf(Sect,"[%s]",Section);
		AddLine(Sect);
		AddLine(Val);
		delete[] Sect;
	}
	if (Val) delete[] Val;
}

BOOL CachedIni::GetItem(char *Section,char *Item,String &S)
{
	Line *L=FindSection(Section);
	if (!L) return FALSE;
	L=FindItem(Item,L->Next);
	if (!L) return FALSE;
	char *p=strchr(L->Str,'=')+1;
	S=p;
	return TRUE;
}

BOOL CachedIni::GetItemInt(char *Section,char *Item,int &Int)
{
	String Temp;
	if (!GetItem(Section,Item,Temp)) return FALSE;
	sscanf(Temp,"%d",&Int);
	return TRUE;
}

BOOL CachedIni::GetItemInt(char *Section,char *Item,long &Int)
{
	String Temp;
	if (!GetItem(Section,Item,Temp)) return FALSE;
	sscanf(Temp,"%ld",&Int);
	return TRUE;
}

BOOL CachedIni::GetItemBool(char *Section,char *Item)
{
	String Temp;
	if (!GetItem(Section,Item,Temp)) return FALSE;
	Temp.ToLower();
	return Temp=="yes" || Temp=="true";
}

BOOL CachedIni::GetSection(char *Section,String &Str)
{
	Str="";
	Line *L=FindSection(Section);
	if (L==NULL) return FALSE;
	L=L->Next;
	while (L && *L->Str!='[')
	{
		Str << L->Str << '\n';
		L=L->Next;
	}
	return TRUE;
}

BOOL IniScan::GetNextItem()
{
	do
	{
		L=L->Next;
	} while (L && (*L->Str==0 || *L->Str==';'));

	if (L && *L->Str=='[') L=NULL;
	return L!=NULL;
}

