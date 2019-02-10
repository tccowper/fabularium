// event class
#include <listc.h>

class GENERICEV {};

typedef void (GENERICEV::*GenProc)();
typedef void (*NormProc)();

class Event : public BaseNode<Event>
{
public:
	Event(GENERICEV *,GenProc *,long);
	Event(NormProc,long);
	void Do();
	void AddToList();
	GENERICEV *Owner;
	GenProc GProc;
	NormProc NProc;

	DWORD EvTime;

	static void Test();
	static void Clear();
};

#define CLASSEVENT(CLASS,PROC,TIME) {\
	void (CLASS::*_P)() =&CLASS::PROC;\
	new Event((GENERICEV *)this,(GenProc*)&_P,TIME);}

#define CLASSEVENT2(CLASS,PTR,PROC,TIME) {\
	void (CLASS::*_P)() =&CLASS::PROC;\
	new Event((GENERICEV *)PTR,(GenProc*)&_P,TIME);}

#define EVENT(PROC,TIME) { new Event(PROC,TIME); }

