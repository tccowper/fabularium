//#include <dir.h>


#ifndef _fname_h
#define _fname_h

#include <stringc.h>

#ifndef MAX_PATH
#define MAX_PATH 256
#endif

class FName : public String
{
public:
	FName() : String(MAX_PATH) { }
	FName(char *);
	FName(char *p,char *n);
	FName(FName &);

	void NewPath(char *e);
	void NewName(char *e);
	void NewBaseName(char *n);
	void NewExt(char *e) ;
	void AddToPath(char *Add);
	char *GetName();
	void GetBaseName(String&);
	void GetDir(String&);
	char *GetExt();	
	long TimeStamp();
	void SetFileTime(long t);
	long GetFileSize();

	BOOL Exist();
	void Expand(char *Dir);

// inherit string operators...

//	FName &operator=(char*s) { *((String*) this)=s; return *this; }
	FName &operator=(char*s) { String::operator=(s); return *this; }
};

class TempFile : public FName
{
public:
	TempFile(char* Pre)
	{
		#ifdef WIN32
		FName Path;
		GetTempPath(MAX_PATH,Path);
		GetTempFileName(Path,Pre,0,*this);
		#else
		//	int Dr=GetTempDrive();
		GetTempFileName(0,Pre,0,*this);
		#endif
	}
	~TempFile()
	{
		OFSTRUCT of;
		OpenFile(*this,&of,OF_DELETE);
	}
};


#endif
