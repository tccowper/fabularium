#include <mywin.h>
#pragma hdrstop

#include <string.h>
 
#include <stringc.h>

int String::Decimals=2;

void String::Set(char*S)
{
	if (S==NULL)
	{
		Str=NULL;
		Length=Max=0;
	}
	else
	{
		Str=strdup(S);
		Length=strlen(Str);
		Max=Length+1;
	}
}

void String::Assign(char*S)
{
	if (S==NULL)
	{
		if (Str) Str[0]=0;
		Length=0;
	}
	else
	{
		Length=strlen(S);
		if (Length>=Max)
		{
			if (Str) delete[] Str;
			Str=strdup(S);
			Max=Length+1;
		}
		else strcpy(Str,S);
	}
}

// asummes p<len
void String::Insert(char *s,int p)
{
	int la=strlen(s);
	if (Length+la+1>Max) ReAlloc(Length+la+1);
	memmove(Str+p+la,Str+p,Length-p+1);
	strncpy(Str+p,s,la);
	Length+=la;
}

void String::Insert(char c,int p)
{
	if (Length+2>Max) ReAlloc(Length+2);
	memmove(Str+p+1,Str+p,Length-p+1);
	Str[p]=c;
	Length+=1;
}

void String::AddString(char *Add)
{
	int la=strlen(Add);
	if (Length+la+1>Max) ReAlloc(Length+la+1);
	strcat(Str,Add);
	Length+=la;
}

void String::AddChar(char Add)
{
	if (Length+1>=Max) ReAlloc(Length+2);
	Str[Length++]=Add;
	Str[Length]=0;
}


void String::ReAlloc(int NewLen)
{
	char *NewStr=new char[Max=NewLen];
	if (Str)
	{
		strcpy(NewStr,Str);
		delete[] Str;
	}
	else *NewStr=0;
	Str=NewStr;
}

String & String::operator << (int i)
{
	char temp[11];
	AddString(itoa(i,temp,10));
	return *this;
}

String & String::operator << (long i)
{
	char temp[11];
	AddString(ltoa(i,temp,10));
	return *this;
}

String & String::operator << (double d)
{
	char Buf[32];
	sprintf(Buf,"%.*lf",Decimals,d);
	AddString(Buf);
	return *this;
}

void String::Pad(int Where,char Pad,int n)
{
	if (Length+n>=Max) ReAlloc(Length+n+1); // +1 for null
	memmove(Str+Where+n,Str+Where,Length-Where+1); // +1 for null
	for (int i=0;i<n;i++) Str[Where+i]=Pad;
	Length+=n;
}

