// event handler class
#include <mywin.h>
#pragma hdrstop

#include "event.h"

List<Event> EventList;
extern DWORD Time;

Event::Event(GENERICEV *This,GenProc *GP,long Delay) : BaseNode<Event>()
{
	// take a copy of function
	GProc=*GP;
	Owner=This;
	EvTime=Time+Delay;
	AddToList();
}

Event::Event(NormProc P,long Delay) : BaseNode<Event>()
{
	// take a copy of function
	NProc=P;
	Owner=NULL;
	EvTime=Time+Delay;
	AddToList();
}

void Event::AddToList()
{
	// put onto Eventlist in crono order
	Event *List=EventList.GetFirst(),*Prev=NULL;
	while (List && EvTime>List->EvTime)
	{
		Prev=List;
		EventList.GetNext(List);
	}
	if (Prev) EventList.AddAfter(this,Prev);
	else EventList.AddHead(this);
}

void Event::Do()
{
	//call proc
	// remove from eventlist in case evlist is affected by call
	EventList.Remove(this);
	if (Owner) (Owner->*GProc)();
	else NProc();
	delete this;
}

void Event::Test()
{
	Event *Ev;
	Ev=EventList.GetFirst();
	while (Ev && Time>Ev->EvTime)
	{
		Ev->Do();
		Ev=EventList.GetFirst();
	}
}

void Event::Clear()
{
	EventList.DeleteAll();
}

