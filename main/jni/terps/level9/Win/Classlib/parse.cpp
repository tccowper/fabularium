// handy parsing stuff
#include <mywin.h>
#pragma hdrstop

#include <string.h>

#include <parse.h>

Parse::Parse(bhp Buf,long Size) : Com(256)
{
	Buffer=Ptr=Buf;
	BufLen=Size;
	c=0;
	Eol=FALSE;
}

Parse::Parse(char* Buf) : Com(256)
{
	Buffer=Ptr=(bhp) Buf;
	BufLen=strlen(Buf);
	c=0;
	Eol=FALSE;
}

void Parse::SkipLine()
{
	Eol=FALSE;
	while (GetC()!='\n' && c>=0);
	if (GetC()!='\r') PutBackC();
}

char *Parse::GetCom()
{
	Com="";
	if (!Eol)
	{
		SkipSpace();
		if (strchr(".0123456789",c)) // pure numeric?
			while (strchr("0123456789.",GetC()) && !Eof()) Com+=c; // exponent?
		else
			while (!strchr(" +-*/,<>=;()\r\n\t",GetC()) && !Eof()) Com+=c;
		if (c==';' || c=='\n' || c=='\r')
		{
			PutBackC();  // only put back line terminator
			Eol=TRUE;
		}
	}
	return Com;
}

char *Parse::GetTerm()
{
	Com="";
	if (!Eol)
	{
		SkipSpace();
		while (!strchr(" +-*/,<>=;()\r\n\t",GetC()) && !Eof()) Com+=c;
		if (c==';' || c=='\n' || c=='\r')
		{
			PutBackC();  // only put back line terminator
			Eol=TRUE;
		}
	}
	return Com;
}


char *Parse::GetQStr()
{
	Com="";
	if (!Eol)
	{
		SkipSpace();
		if (GetC()=='\"')
		{
			bhp oPtr=Ptr;
			if (!SkipTo("\"")) return NULL;
			int n=Ptr-oPtr-1;
			if (n>255) return NULL;
			memcpy(Com,oPtr,n);
			Com[n]=0;
			if (strchr(Com,'\n')) return NULL;
			// skip to delimiter
			SkipSpace();
			if (!strchr("+-*/,<>=()\t",GetC())) PutBackC();
		}
		else
		{
			PutBackC();
			GetCom();
		}
	}
	return Com;
}

char *Parse::RestOfLine()
{
	SkipSpace();
	Com="";
	if (c!=';' && c>=' ')
	{
		while (GetC()>=' ') Com+=c;
		PutBackC();
	}
	Eol=TRUE;
	return Com;
}

char *Parse::GetLine()
{
	Com="";
	while (GetC()!='\n' && c!='\r' && !Eof()) Com+=c;
	PutBackC();
	// skip to newline
	if (c>=0) while (GetC()!='\n' && c>=0);
	Eol=FALSE;
	return Com;
}

void Parse::SkipSpace()
{
	if (!Eol)
	{
		while (GetC()==' ' || c=='\t');
		PutBackC();
	}
}

BOOL Parse::SkipTo(char *skip)
{
	Eol=FALSE;
	int n=strlen(skip);
	do
	{
		int i=0;
		while (GetC()==skip[i] && ++i<n);
		if (i==n) return TRUE;
	} while (c>=0);
	return FALSE;
}

BOOL Parse::GoTo(char *g)
{
	Ptr=Buffer;
	return SkipTo(g);
}

// *********************************

enum {ID_TEXT,ID_INT,ID_FLOAT};

ID *ID::IDList=NULL;

ID::ID(char *N,int V)
{
	Name=strdup(N);
	Value=V;
	StrValue=NULL;
	// add to IDList
	Next=IDList;
	IDList=this;
	Type=ID_INT;
	RefCount=0;
}

ID::ID(char *N,char *V)
{
	Name=strdup(N);
	Value=0;
	StrValue=strdup(V);
	// add to IDList
	Next=IDList;
	IDList=this;
	Type=ID_TEXT;
	RefCount=0;
}

ID::ID(char *N,double V)
{
	Name=strdup(N);
	Value=0;
	StrValue=NULL;
	DblValue=V;
	// add to IDList
	Next=IDList;
	IDList=this;
	Type=ID_FLOAT;
	RefCount=0;
}

ID::~ID()
{
	delete[] Name;
	if (StrValue) delete[] StrValue;
}

void ID::FreeIDs()
{
	ID *I=IDList,*I2;
	while (I)
	{
		I2=I->Next;
		delete I;
		I=I2;
	}
	IDList=NULL;
}

BOOL ID::EvalInt(char *Id,int &i)
{
	ID *I=Find(Id,ID_INT);
	if (I)
	{
		i=I->Value;
		I->RefCount++;
	}
	return I!=NULL;
}

BOOL ID::EvalStr(char *Id,char *&s)
{
	ID *I=Find(Id,ID_TEXT);
	if (I)
	{
		s=I->StrValue;
		I->RefCount++;
	}
	return I!=NULL;
}

BOOL ID::EvalDouble(char *Id,double &d)
{
	ID *I=Find(Id,ID_FLOAT);
	if (I)
	{
		d=I->DblValue;
		I->RefCount++;
	}
	return I!=NULL;
}

ID *ID::Find(char *Id)
{
	ID *I=IDList;
	while (I && stricmp(Id,I->Name)) I=I->Next;
	return I;
}

ID *ID::Find(char *Id,int Type)
{
	ID *I=IDList;
	while (I && (I->Type!=Type || stricmp(Id,I->Name))) I=I->Next;
	if (I) I->RefCount++;
	return I;
}

void ID::Assign(char *V)
{
	if (StrValue) delete[] StrValue;
	StrValue=strdup(V);
}

void ID::Assign(int V)
{
	Value=V;
}

void ID::Assign(double V)
{
	DblValue=V;
}

void ID::Set(char *Id,char *V)
{
	ID *I=Find(Id);
	if (I==NULL) new ID(Id,V);
	else I->Assign(V);
}

void ID::Set(char *Id,int V)
{
	ID *I=Find(Id);
	if (I==NULL) new ID(Id,V);
	else I->Assign(V);
}

void ID::Set(char *Id,double V)
{
	ID *I=Find(Id);
	if (I==NULL) new ID(Id,V);
	else I->Assign(V);
}

