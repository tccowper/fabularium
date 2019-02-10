// custom file dialog
#include <mywin.h>
#pragma hdrstop

#include <string.h>

#include <stringc.h>
#include <fname.h>

CustFileDlg::CustFileDlg(Object *Parent ,int Id, char *Buf,int BufS,const char *aTitle,const char *Filt,int *FiltInd,long flgs)
	 : Object(Parent)
{
	ID=Id;
	Buffer=Buf;
	BufSize=BufS;
	Title=aTitle;
	Filters=Filt;
	FiltIndex=FiltInd;
	hWnd=0;
	TrapOK=FALSE;
	Flags=flgs;

	int ofnSize=sizeof(OPENFILENAME);
	OSVERSIONINFO vi;
	ZeroMemory(&vi,sizeof(OSVERSIONINFO));
	vi.dwOSVersionInfoSize=sizeof(OSVERSIONINFO);
	::GetVersionEx(&vi);
	if (vi.dwPlatformId==VER_PLATFORM_WIN32_NT && vi.dwMajorVersion>=5)
		ofnSize = 0x58; // sizeof(OPENFILENAME) for Windows 2000 and above
	O=(OPENFILENAME*)malloc(ofnSize);
	ZeroMemory(O,ofnSize);
	O->lStructSize=ofnSize;
}

BOOL CustFileDlg::SetupWindow() { return TRUE; }
void CustFileDlg::Paint(HDC, BOOL, RECT &) {}
BOOL CustFileDlg::Create() { return TRUE; }
void CustFileDlg::FileSelected(char*) {}
void CustFileDlg::EndDialog(int) {}

CustFileDlg::~CustFileDlg()
{
	free(O);
}

#define IDC_FILE	1120

BOOL CustFileDlg::WndProc(TMSG &Msg)
{
	if (Msg.Msg==FokMsg && TrapOK)
	{
		FileSelected(O->lpstrFile);
		TrapOK=FALSE;
		return TRUE;
	}
	switch (Msg.Msg)
	{
		case WM_INITDIALOG:
			SetupWindow();
//			if (Ini) GetDialogPos();
			SetFocus(GetDlgItem(hWnd,IDOK));
			break;

		case WM_DESTROY:
			hWnd=NULL;
			break;

		case WM_PAINT:
			PAINTSTRUCT  ps;
			BeginPaint(hWnd, (LPPAINTSTRUCT) &ps);
			Paint(ps.hdc,ps.fErase,ps.rcPaint);
			EndPaint(hWnd, (LPPAINTSTRUCT) &ps);
			break;

		case WM_COMMAND:
			switch (LOWWPARAM(Msg.wParam))
			{
				case IDOK:
				case IDCANCEL:
					EndDialog(LOWWPARAM(Msg.wParam));
					break;
				case IDC_FILE:
					// only trap for open file
					if (!(Flags & OFN_OVERWRITEPROMPT) && HIWLPARAM(Msg)==LBN_SELCHANGE)
					{
						// bit of a hack
						// pick up FILEOK message
						TrapOK=TRUE;
						PostMessage(hWnd,WM_COMMAND,IDOK,0);
					}
					break;

			}    // switch (wParam)
	}     // switch (uMessage)

	return EV_FIND(Msg);
}

CustFileDlg *CustFileDlg::OpenDialog;

#ifdef __BORLANDC__
UINT _export CALLBACK CustFileDlg::MyOpenDlgProc(HWND hDlg,UINT Msg,WPARAM wParam,LPARAM lParam)
#else
UINT CALLBACK CustFileDlg::MyOpenDlgProc(HWND hDlg,UINT Msg,WPARAM wParam,LPARAM lParam)
#endif
{
	if (Msg==WM_INITDIALOG) OpenDialog->hWnd=hDlg;
	TMSG aMsg={Msg,wParam,lParam,0};
	return OpenDialog->WndProc(aMsg);
}

//	OFN_EXPLORER - new look commdlg
//	OFN_LONGNAMES - force long names for 3.x modules


int CustFileDlg::Execute(BOOL hook)
{
	OpenDialog=this;
	FName F(Buffer);
	String Name(BufSize);
	Name=F.GetName();
	String Dir;
	F.GetDir(Dir);

	O->hwndOwner=ParentHWnd;
	O->hInstance=App::hInstance;
	O->lpstrFilter=Filters;
	O->lpstrCustomFilter=NULL;
	O->nFilterIndex=FiltIndex ? *FiltIndex : 0;
	O->lpstrFile=Name;
	O->nMaxFile=Name.Size();
	O->lpstrFileTitle=NULL;
	O->lpstrInitialDir=Dir;
	O->lpstrTitle=Title;
	O->Flags=Flags | OFN_PATHMUSTEXIST | OFN_HIDEREADONLY | OFN_ENABLESIZING;
	if (hook)
		O->Flags |= OFN_ENABLEHOOK;

	if (ID>=0)
	{
		O->lpTemplateName=MAKEINTRESOURCE(ID);
		O->Flags |= OFN_ENABLETEMPLATE;
	}
	O->lpstrDefExt=NULL;
	O->lpfnHook=MyOpenDlgProc;

	FokMsg=RegisterWindowMessage(FILEOKSTRING);

	int ret=	(O->Flags & OFN_OVERWRITEPROMPT) ?
		GetSaveFileName(O) : GetOpenFileName(O);

	if (ret)
	{
		strcpy(Buffer,Name);
		if (FiltIndex) *FiltIndex=O->nFilterIndex;
	}
	return ret;
}
