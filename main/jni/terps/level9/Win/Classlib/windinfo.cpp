// list of predefined window classes stored as objects
#include <mywin.h>
#pragma hdrstop

WindowInfo *WindowInfo::WinObjList=NULL;

WindowInfo::WindowInfo(Object *O,HWND hw)
{
	Next=WinObjList;
	WinObjList=this;
	if (Next) Next->Prev=this;
	Prev=NULL;
	hWnd=hw;
	Obj=O;
}

WindowInfo::~WindowInfo()
{
	if (Prev) Prev->Next=Next;
	else WinObjList=Next;
	if (Next) Next->Prev=Prev;
}

Object *WindowInfo::Find(HWND hw)
{
	WindowInfo *Ptr=WinObjList;
	while (Ptr)
	{
		if (Ptr->hWnd==hw) return Ptr->Obj;
		Ptr=Ptr->Next;
	}
	return NULL;
}

void WindowInfo::Delete(HWND hw)
{
	WindowInfo *Ptr=WinObjList;
	while (Ptr)
	{
		if (Ptr->hWnd==hw)
		{
			delete Ptr;
			return;
		}
		Ptr=Ptr->Next;
	}
}

