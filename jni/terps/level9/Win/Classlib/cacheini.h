// cacheini.h

#ifndef _cacheini_h
#define _cacheini_h

#include <string.h>
#include <stringc.h>

class CachedIni
{
public:
	class Line
	{
	public:
		Line(char *s)
		{
			Str=strdup(s);
			Len=strlen(Str);
		}
		~Line()
		{
			delete[] Str;
		}
		void SetString(char *s)
		{
			delete[] Str;
			Str=strdup(s);
			Len=strlen(Str);
		}
		bhp Write(bhp Buf)
		{
			strcpy((char*)Buf,Str);
			*(Buf+Len)='\n';
			return Buf+Len+1;
		}
		char *Str;
		int Len;
		Line *Next,*Prev;
	};

	CachedIni();
	CachedIni(bhp ptr,long size);
	~CachedIni();

	void AddLine(char *s);
	void AddToSection(char *Section,char *Item,char *Value);
	BOOL GetItem(char *Section,char *Item,String &S);
	BOOL GetItemInt(char *Section,char *Item,int &Int);
	BOOL GetItemInt(char *Section,char *Item,long &Int);
	BOOL GetItemBool(char *Section,char *Item);

	BOOL GetSection(char *Section,String &Str);

	bhp Write();
	void Write(char*);
	BOOL Read(char*);

	Line *LineList,*Last;
	static Line *TempLine;
	long TotalSize;

	void DeleteLine(Line *L);
	Line *InsertLine(char *s,Line *L);
	Line *FindSection(char *Str);
	Line *FindItem(char *Item,Line *L);
	Line *FindItem2(char *Item,char *Full,Line *L);
};

class IniScan
{
public:
	CachedIni::Line *L;
	IniScan(CachedIni &C,char *Sect)
	{
		Section(C,Sect);
	}
	void Section(CachedIni &C,char *Sect)
	{
		L=C.FindSection(Sect);
		if (L) GetNextItem();
	}
	BOOL GetNextItem();
	operator char*()
	{
		return L->Str;
	}
	char *Str()
	{
		return L->Str;
	}
	int Len()
	{
		return L->Len;
	}
	BOOL operator ()() { return L!=NULL; }
	BOOL operator ++() { return GetNextItem(); }
	BOOL operator ++(int) { return GetNextItem(); }
};


#endif

