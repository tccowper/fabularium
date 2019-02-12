// Edit ***********************************************

#include <mywin.h>
#pragma hdrstop

#include <string.h>

Edit::Edit(Object *Parent,int Id,char *Init,int Len) : Object(Parent)
{
	ID=Id;
	hWnd=GetDlgItem(ParentHWnd,ID);
	if (Len>0) SendMessage(hWnd,EM_LIMITTEXT,(WPARAM) Len-1,0);
	new WindowInfo(this,hWnd);
	SubClass();
// had to move this to end for WIN32 ('95) ??
	if (Init) SetString(Init);
}

BOOL Edit::Create()
{ // Exists in dialog
	return TRUE;
}

Edit::~Edit()
{
	WindowInfo::Delete(hWnd);
}

int Edit::GetLength()
{
	return SendMessage(hWnd,WM_GETTEXTLENGTH,0,0);
}

char *Edit::GetString(char *s,int BufSize)
{
	if (s==NULL)
	{
		BufSize=GetLength()+1;
		s=new char[BufSize];
	}

//	GetDlgItemText(ParentHWnd,ID,s,BufSize);
	GetWindowText(hWnd,s,BufSize);
	return s;
}

void Edit::SetString(char *s)
{
	SetWindowText(hWnd,s);
//	SetDlgItemText(ParentHWnd,ID,s);
}

void Edit::AddString(char *s)
{
	int BufSize=GetLength()+strlen(s)+1;
	char *temp=new char[BufSize];
	GetString(temp,BufSize);
	strcat(temp,s);
	SetString(temp);
	delete[] temp;
}

LRESULT CALLBACK EditProc( HWND hWnd, UINT iMessage, WPARAM wParam,
											LPARAM lParam )
{
	TMSG Msg={iMessage,wParam,lParam,0};
	Edit *Obj=(Edit*) WindowInfo::Find(hWnd);
	if (Obj)
	{
		Obj->WndProc(Msg);
		return Msg.RetVal;
	}
	return 0;
}

LRESULT Edit::DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam)
{
#ifdef __BORLANDC__
	return CallWindowProc((FARPROC) OldProc, hWnd, Msg, wParam, lParam );
#else
	return CallWindowProc((WNDPROC) OldProc, hWnd, Msg, wParam, lParam );
#endif
}

void Edit::SubClass()
{
	OldProc=(WNDPROC) GetWindowLong(hWnd,GWL_WNDPROC);
	SetWindowLong(hWnd,GWL_WNDPROC,(DWORD) EditProc);
}

