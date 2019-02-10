// handy filename manipulation
#include <mywin.h>
#pragma hdrstop

#include <time.h>
#include <fcntl.h>
#include <io.h>
#include <stdio.h>

#include <fname.h>

FName::FName(char *f) : String(MAX_PATH)
{
	Assign(f);
}

FName::FName(char *p,char *n) : String(MAX_PATH)
{
	NewPath(p);
	NewName(n);
}

FName::FName(FName &F) : String(MAX_PATH)
{
	Assign(F);
}

void FName::NewExt(char *e)
{
	if (strchr(GetName(),'.')) Len(rPos('.'));
	*this  << '.' << e;
}

void FName::NewPath(char *p)
{
	String n(GetName());
	Assign(p);
	// force trailing "\\"
	if (!Empty() && Last()!='\\') *this << '\\';
	*this << n;
}

void FName::NewBaseName(char *n)
{
	String S(GetExt());
	NewName(n);
	if (!S.Null()) *this << '.' << S;
}

void FName::NewName(char *n)
{
	if (!strchr(n,'\\')) // has relative path?
	{
		int p=rPos('\\');
		if (p>=0) Len(p+1);
	}
	AddString(n);
}

char *FName::GetName()
{
	return Str+rPos('\\')+1; // -1 if not in
}

char *FName::GetExt()
{
	return strchr(GetName(),'.') ? Str+rPos('.')+1 : NULL;
}

void FName::GetBaseName(String &S)
{
	S=GetName();
	int n=S.Pos('.');
	if (n>=0) S.Len(n);
}

void FName::GetDir(String &S)
{
	S=Str;
	int n=rPos('\\');
	if (n>=0) S.Len(n); else S="."; // no trailing "\\"
}

void FName::AddToPath(char *Add)
{
	String S(GetName());
	NewName("");
	AddString(Add);
	if (Last()!='\\') *this << '\\';
	AddString(S);
}

long FName::TimeStamp()
{
#ifdef WIN32
	HANDLE f=CreateFile(Str,GENERIC_READ,0,NULL,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,NULL);
	if (f==INVALID_HANDLE_VALUE) return -1;
	FILETIME ft;
	GetFileTime(f,NULL,NULL,&ft);
	WORD Date,Time;
	FileTimeToDosDateTime(&ft,&Date,&Time);
	CloseHandle(f);
	return Time+(Date<<16);
#else
	int hand;
	union Time
	{
		ftime ft;
		unsigned long dw;
	} ft;
	if ((hand=open(Str,O_RDONLY))<0) return -1;
	getftime(hand,&ft.ft);
	close(hand);
	return ft.dw;
#endif
}

void FName::SetFileTime(long t)
{
#ifdef WIN32
	HANDLE f=CreateFile(Str,GENERIC_WRITE,0,NULL,OPEN_EXISTING,FILE_ATTRIBUTE_NORMAL,NULL);
	if (f==INVALID_HANDLE_VALUE) return;
	FILETIME ft;
	DosDateTimeToFileTime(HIWORD(t),LOWORD(t),&ft);
	::SetFileTime(f,NULL,NULL,&ft);
	CloseHandle(f);
#else
	int hand;
	union Time {
		ftime ft;
		DWORD dw;
		} ft;
	ft.dw=t;
	if ((hand=open(Str,O_RDONLY))<0) return;
	setftime(hand,&ft.ft);
	close(hand);
#endif
}

long FName::GetFileSize()
{
	int hand=open(Str,O_RDONLY);
	long len=filelength(hand);
	close(hand);
	return len;
}

BOOL FName::Exist()
{
	if (Empty()) return FALSE;
	FILE *f=fopen(Str,"rb");
	if (f)
	{
		fclose(f);
		return TRUE;
	}
	return FALSE;
}

// expand relative references
void FName::Expand(char *Dir)
{
// qualified name
	if (Empty() || Pos(':')>=0 || Str[0]=='\\') return;

	int n=strlen(Dir);
	if (Dir[n-1]=='\\') n--;

	char *Ptr=Str;
	while (!strncmp(Ptr,"..\\",3))
	{
		Ptr+=3;
		while (n>0 && Dir[--n]!='\\');
	}

	String S(Ptr);
	Assign(Dir);
	Len(n);
	*this << '\\' << S;
}

