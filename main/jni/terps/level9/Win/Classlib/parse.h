#ifndef _parse_h
#define _parse_h

#include <stringc.h>

class Parse
{
public:
	Parse(bhp,long);
	Parse(char*);

	String Com;
	char *GetCom();
	char *GetTerm();
	char *GetQStr();
	char *RestOfLine();
	char *GetLine();
	void SkipLine();
	BOOL Eof();
	BOOL SkipTo(char *skip);
	BOOL GoTo(char *g);
	bhp Buffer;
	bhp Ptr;
	BOOL Eol;
	long BufLen;
	char c;
	char GetC();
	void PutBackC();
	void SkipSpace();

	Parse& operator >> (int &i) { i=atoi(GetCom()); return *this; }
	Parse& operator >> (long &i) { i=atol(GetCom()); return *this; }
};

inline void Parse::PutBackC()
{
	if (!Eol && !Eof()) Ptr--;
}

inline char Parse::GetC()
{
	return c= ((Ptr<Buffer+BufLen) ? *Ptr++ : -1);
}

inline BOOL Parse::Eof()
{
	return c==-1;
}

// ID Stuff *************************************************************************

class ID {
public:
	char *Name;
	int Value;
	char *StrValue;
	double DblValue;
	int Type;
	int RefCount;
	ID *Next;
	ID(char *N,int V);
	ID(char *N,char *V);
	ID(char *N,double V);

	void Assign(char *V);
	void Assign(int);
	void Assign(double);

	~ID();
	static void FreeIDs();
	static ID *IDList;
	static ID *Find(char *Id);
	static ID *Find(char *Id,int);

	static BOOL ID::EvalInt(char *Id,int &);
	static BOOL ID::EvalStr(char *Id,char *&);
	static BOOL ID::EvalDouble(char *Id,double &);

	static void Set(char *Id,int V);
	static void Set(char *Id,char* V);
	static void Set(char *Id,double V);
};

#endif

