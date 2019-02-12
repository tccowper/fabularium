// Dialog definitions *****************************

#include <mywin.h>
#pragma hdrstop

#include <stdlib.h>
#include <stdio.h>

#include <stringc.h>
#include <parse.h>

Dialog *D;

BOOL PASCAL DialogHandler (HWND hDlg, UINT uMessage,
								WPARAM wParam, LPARAM lParam);

Dialog::Dialog(Object *Parent ,int Id, char *Sect) : Object(Parent)
{
	lpProcDialog = (DLGPROC) MakeProcInstance( (FARPROC)DialogHandler, App::hInstance);
	ID=Id;
	hWnd=0;
	Section=Sect;
}

Dialog::~Dialog()
{
	if (hWnd)
	{
		if (IsModal) ::EndDialog(hWnd,IDCANCEL);
		else DestroyWindow(hWnd); //destroy modeless dialog
	}
	FreeProcInstance( (FARPROC)lpProcDialog);
}

int Dialog::Execute()
{
	IsModal=TRUE;
	D=this; // temp storage to initialise
	return DialogBox(App::hInstance,
		MAKEINTRESOURCE(ID), ParentHWnd,lpProcDialog);
}

static BYTE* offsetDWord(BYTE* ptr)
{
  int offset = (int)(DWORD_PTR)ptr;
  if (offset & 3)
    return ptr + (4 - (offset & 3));
  return ptr;
}

int Dialog::ExecuteWithFont()
{
	IsModal=TRUE;
	D=this; // temp storage to initialise

  int code = -1;

  NONCLIENTMETRICS ncm;
  ::ZeroMemory(&ncm,sizeof ncm);
  ncm.cbSize = sizeof ncm;
  ::SystemParametersInfo(SPI_GETNONCLIENTMETRICS,sizeof ncm,&ncm,0);
  OSVERSIONINFO osvi;
  osvi.dwOSVersionInfoSize = sizeof osvi;
  ::GetVersionEx(&osvi);
  WCHAR fontName[256];
  MultiByteToWideChar(CP_ACP,0,
    ncm.lfMessageFont.lfFaceName,strlen(ncm.lfMessageFont.lfFaceName)+1,fontName,256);

  HRSRC resInfo = FindResource(App::hInstance,MAKEINTRESOURCE(ID),RT_DIALOG);
  if (resInfo != 0)
  {
    DWORD resSize = SizeofResource(App::hInstance,resInfo);
    HGLOBAL resGlobal = LoadResource(App::hInstance,resInfo);
    if (resGlobal != 0)
    {
      BYTE* resMem = (BYTE*)LockResource(resGlobal);
      if (resMem != NULL)
      {
        HGLOBAL copyGlobal = GlobalAlloc(GMEM_ZEROINIT,resSize+64);
        if (copyGlobal != 0)
        {
          BYTE* copyMem = (BYTE*)GlobalLock(copyGlobal);
          if (copyMem != 0)
          {
            int titleLen = (wcslen(((WCHAR*)resMem)+15)+1)*sizeof(WCHAR);
            int copy1Size = (18*sizeof(WORD))+titleLen;
            int font1Len = (wcslen((WCHAR*)(resMem+copy1Size))+1)*sizeof(WCHAR);
            int copy2Size = resSize-copy1Size-font1Len;
            int font2Len = (wcslen(fontName)+1)*sizeof(WCHAR);

            memcpy(copyMem,resMem,copy1Size);
            wcscpy((WCHAR*)(copyMem+copy1Size),fontName);
            memcpy(offsetDWord(copyMem+copy1Size+font2Len),offsetDWord(resMem+copy1Size+font1Len),copy2Size);
            *((WORD*)(copyMem+copy1Size-(3*sizeof(WORD)))) = (osvi.dwMajorVersion < 6) ? 8 : 9;
            GlobalUnlock(copyGlobal);
          }
          code = DialogBoxIndirect(App::hInstance,(LPDLGTEMPLATE)copyGlobal,ParentHWnd,lpProcDialog);
          GlobalFree(copyGlobal);
        }
      }
    }
  }

  return code;
}

BOOL Dialog::Create()
{
	IsModal=FALSE;
	D=this;
	// modeless dialog creation
	hWnd=CreateDialog(App::hInstance,
		MAKEINTRESOURCE(ID), ParentHWnd,lpProcDialog);
	return hWnd!=NULL;
}

BOOL Dialog::SetupWindow() { return TRUE; }

void Dialog::EndDialog(int) {}

// Call from EndDialog()
void Dialog::SaveDialogPos()
{
	RECT rc;
	GetWindowRect(hWnd,&rc);
	WriteIniString(Section,"Position",String()<< (int) rc.left << ',' << (int) rc.top);
}

// call this from SetupWindow
void Dialog::GetDialogPos()
{
	RECT rc;
	// if this puts dialog completely offscreen, then dont do it???
	String S;
	ReadIniString(Section,"Position",S);
	if (!S.Empty())
	{
		Parse P(S);
		P >> rc.left >> rc.top;
		SetWindowPos(hWnd,0,rc.left,rc.top,0,0,SWP_NOSIZE | SWP_NOZORDER);
	}
}

void Dialog::CentreDlgOnParent()
{
	RECT rc;
	GetWindowRect(ParentHWnd,&rc);
	int x=(rc.left+rc.right)/2;
	int y=(rc.top+rc.bottom)/2;
	GetClientRect(hWnd,&rc);
	SetWindowPos(hWnd,0,x-rc.right/2,y-rc.bottom/2,0,0,SWP_NOSIZE | SWP_NOZORDER);
}

void Dialog::KillDialog(int Value)
{
	if (Value==IDOK)
	{
		if (!CanClose()) return;
		if (Section) SaveDialogPos();
	}

	EndDialog(Value);
	if (IsModal) ::EndDialog(hWnd, Value);
	else
	{
		DestroyWindow(hWnd);
	}
}

void Dialog::Paint(HDC, BOOL, RECT&) {}

BOOL Dialog::ChildIDMessage(UINT id ,TMSG &Msg)
{
	return ChildMessage(GetDlgItem(hWnd,id),Msg);
}

BOOL Dialog::ChildMessage(HWND hw,TMSG &Msg)
{
	Object *Child=FirstChild();
	while (Child)
	{
		if (Child->hWnd==hw) return Child->WndProc(Msg);
		Child=NextChild(Child);
	}
	return FALSE;
}


// if breakpoint is set in this code then dialogs lose their modalness ?????

BOOL PASCAL DialogHandler (HWND hDlg, UINT uMessage,
								WPARAM wParam, LPARAM lParam)

{
//allow user to grab any message
	Dialog *Dlg;
	if ( (Dlg=(Dialog*)WindowInfo::Find(hDlg) )==NULL)
	{
		if (D==NULL) return FALSE;
		new WindowInfo((Object*) (Dlg=D),hDlg);
		D=NULL;
	}
	TMSG Msg={uMessage,wParam,lParam,0};
	if (Dlg->EV_FIND(Msg)) return TRUE;

	switch (uMessage)
	{

		case WM_INITDIALOG:
			Dlg->hWnd=hDlg;
			Dlg->SetupWindow();
			if (Dlg->Section) Dlg->GetDialogPos();
			return TRUE;

		case WM_DESTROY:  //if main window?
			if (Dlg==App::MainWindow) PostQuitMessage(0);
			WindowInfo::Delete(hDlg);
			Dlg->hWnd=NULL;
			return TRUE;

		case WM_PAINT:
			PAINTSTRUCT  ps;
			BeginPaint(Dlg->hWnd, (LPPAINTSTRUCT) &ps);
			Dlg->Paint(ps.hdc,ps.fErase,ps.rcPaint);
			EndPaint(Dlg->hWnd, (LPPAINTSTRUCT) &ps);
			return TRUE;

		case WM_DRAWITEM:
			return Dlg->ChildIDMessage((UINT) wParam,Msg);
	#ifndef WIN32
		case WM_CTLCOLOR:
			return Dlg->ChildMessage((HWND) LOWORD(lParam),Msg);
	#endif
		case WM_COMMAND:
			switch (wParam)
			{
				case IDOK:
				case IDCANCEL:
					Dlg->KillDialog(wParam);
					return (TRUE);

				default:
#ifndef WIN32
					return Dlg->ChildMessage((HWND) LOWORD(lParam),Msg);
#else
					return Dlg->ChildMessage((HWND) lParam,Msg);
#endif
			}    // switch (wParam)
		}     // switch (uMessage)

	return FALSE;
}

