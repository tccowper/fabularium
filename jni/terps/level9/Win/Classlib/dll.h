// dll class
#ifndef _DLL_H
#define _DLL_H

class DLL
{
public:
	DLL(char*n)
	{
		Name=strdup(n);
		Loaded=FALSE;
	}
	~DLL()
	{
		Unload();
		delete[] Name;
	}
	BOOL Load();
	void Unload()
	{
		if (Loaded) FreeLibrary(Inst);
			Loaded=FALSE;
	}
	HINSTANCE Inst;
	BOOL Loaded;
	char *Name;
};

inline BOOL DLL::Load()
{
	Inst=LoadLibrary(Name);
#ifdef WIN32
	if (!Inst) return FALSE;
#else
	if (Inst<HINSTANCE_ERROR) return FALSE;
#endif
	return Loaded=TRUE;
}

#endif
