// Object definitiions ***********************************

#include <mywin.h>
#pragma hdrstop

Object::Object(Object *AParent)
{
	Parent=AParent;
	//Add onto parents childlist
	if (Parent) {
		Next=Parent->ChildList;
		Parent->ChildList=this;
		if (Next) Next->Prev=this;
		ParentHWnd=Parent->hWnd;
		}
	else { // no parent
		Next=NULL;
		ParentHWnd=0;
		}

	ChildList=Prev=NULL;
}

Object::~Object()
{
	// delete all children
	Object *List=ChildList,*Nxt;
	while (List) {
		Nxt=List->Next;
		delete List;
		List=Nxt;
		}
	// remove this object from parents Childlist
	if (Next) Next->Prev=Prev;
	if (Prev) Prev->Next=Next;
	// only child?
	if (Parent && Parent->ChildList==this) Parent->ChildList=Next;
}

BOOL Object::CanClose()
{
	Object *Child=FirstChild();
	while (Child)
	{
		if (!Child->CanClose()) return FALSE;
		Child=NextChild(Child);
	}
	return TRUE;
}

BOOL Object::EV_FIND(TMSG &)
{
	return FALSE;
}

BOOL Object::Create() { return FALSE; }

// returns TRUE if message is processed
BOOL Object::EV_SEARCH(GEN_EV_INFO * EV_TABLE, TMSG &Msg, GENERIC *Owner)
{
	while ((EV_TABLE)->Msg)
	{
		if (EV_TABLE->Msg==Msg.Msg)
		{
			switch (EV_TABLE->Type)
			{
				case 0:
					if (EV_TABLE->Id==LOWORD(Msg.wParam))
					{
						( Owner->*EV_TABLE->Proc)();
						return TRUE;
					}
					break;

				case 2:
					if (EV_TABLE->Id==LOWORD(Msg.wParam))
					{
#ifndef WIN32
						( Owner->*((NotifyProc) EV_TABLE->Proc) ) (HIWORD(Msg.lParam));
#else
						( Owner->*((NotifyProc) EV_TABLE->Proc) ) (HIWORD(Msg.wParam));
#endif
						return TRUE;
					}
					break;

				case 1:
					return (Owner->*( (Proc1) EV_TABLE->Proc) ) (Msg);
			}
		}
	EV_TABLE++;
	}
	return FALSE;
}

BOOL Object::WndProc(TMSG &Msg)
{
	return (Msg.RetVal=DefProc(hWnd,Msg.Msg,Msg.wParam,Msg.lParam))==0;
}

LRESULT Object::DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam)
{
	return DefWindowProc(hWnd,Msg,wParam,lParam);
}

