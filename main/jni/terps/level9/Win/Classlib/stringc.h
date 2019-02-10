#include <stdlib.h>
#include <string.h>

#ifndef _stringc_h
#define _stringc_h

class String
{
public:
	char *Str;
	int Length,Max;
	String()
	{
		Str=NULL;
		Max=Length=0;
	}
	String(char *S) { Set(S); }
	String(String &S) { Set(S); }
	void Set(char*S);
	String(char *S,int n)
	{
		Str=new char[Max=n+1];
		Length=0;
		Assign(S);
	}
	String(int n)
	{
		Str=new char[Max=n];
		*Str=Length=0;
	}
	~String()
	{
		if (Str) delete[] Str;
	}
	int Len() { return Length; }
	void Len(int NewLen)
	{
		Str[NewLen]=0;
		Length=NewLen;
	}
	void Update()
	{
		Length=strlen(Str);
	}
	int Size() { return Max; }
	void AddString(char *Add);
	void ReAlloc(int NewLen);
	void Assign(char*S);
	void AddChar(char Add);
	int Pos(char c)
	{
		char *p=strchr(Str,c);
		return p ? p-Str : -1;
	}
	int Pos(char c,int pos)
	{
		char *p=strchr(Str+pos,c);
		return p ? p-Str : -1;
	}
	int rPos(char c)
	{
		char *p=strrchr(Str,c);
		return p ? p-Str : -1;
	}
	int rPos(char c,int pos)
	{
		char temp=Str[pos];
		Str[pos]=0;
		char *p=strrchr(Str,c);
		Str[pos]=temp;
		return p ? p-Str : -1;
	}
	void Insert(char *s,int p);
	void Insert(char c,int p);
	void Remove(int From,int n)
	{
		memmove(Str+From,Str+From+n,Length-From-n+1);
		Update();
	}
	void Pad(int Where,char Pad,int n);
	char Last() { return Length ? Str[Length-1] : 0; }

	String & operator << (char *Add) { AddString(Add); return *this; }
	String & operator += (char *Add) { return *this << Add; }
	String & operator << (char Add) { AddChar(Add); return *this; }
	String & operator += (char Add) { return *this << Add; }
	String & operator << (String &Add) { AddString(Add); return *this; }
	String & operator += (String &Add) { return *this << Add; }

	String & operator << (int i);
	String & operator += (int i) { return *this << i; }
	String & operator >> (int &i) { i=atoi(Str); return *this; }

	String & operator << (long i);
	String & operator += (long i) { return *this << i; }
	String & operator >> (long &i) { i=atol(Str); return *this; }

	String & operator << (double d);
	String & operator >> (double &d) { d=atof(Str); return *this; }

	static int Decimals;
	operator char*() { return Str; }

	String & operator =(String &S) { Assign(S); return *this; }
	String & operator =(char *S) { Assign(S); return *this; }
	int operator < (String &S) { return strcmp(Str,S)<0; }
	int operator <= (String &S) { return strcmp(Str,S)<=0; }
	int operator ==(String &S) { return strcmp(Str,S)==0; }
	int operator !=(String &S) { return strcmp(Str,S)!=0; }
	int operator > (String &S) { return strcmp(Str,S)>0; }
	int operator >= (String &S) { return strcmp(Str,S)>=0; }

	int operator < (char *S) { return strcmp(Str,S)<0; }
	int operator <= (char *S) { return strcmp(Str,S)<=0; }
	int operator ==(char *S) { return strcmp(Str,S)==0; }
	int operator !=(char *S) { return strcmp(Str,S)!=0; }
	int operator > (char *S) { return strcmp(Str,S)>0; }
	int operator >= (char *S) { return strcmp(Str,S)>=0; }

	char &operator [](int Pos) { return Str[Pos]; }
	BOOL Empty() { return Str==NULL || *Str==0; }
	BOOL Null() { return Str==NULL; }
	void ToUpper() { strupr(Str); }
	void ToLower() { strlwr(Str); }

	String & operator --()
	{
		if (Length) Str[--Length]=0;
		return *this;
	}
};

#endif
