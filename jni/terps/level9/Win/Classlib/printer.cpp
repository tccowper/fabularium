#include <mywin.h>
#pragma hdrstop

#include <string.h>
#include <cderr.h>

Printer::Printer()
{
	memset(&pd, 0, sizeof(PRINTDLG));
	pd.lStructSize = sizeof(PRINTDLG);
	GetDefault();
}

void Printer::GetDefault()
{
	DeleteGlobals();
	pd.Flags = PD_RETURNDEFAULT;
	PrintDlg(&pd);
	// pd now has hDevMode and hDevNames set
}

void Printer::DeleteGlobals()
{
	if (pd.hDevMode != NULL)
	{
		GlobalFree(pd.hDevMode);
		pd.hDevMode=NULL;
	}
	if (pd.hDevNames != NULL)
	{
		GlobalFree(pd.hDevNames);
		pd.hDevNames=NULL;
	}
}

Printer::~Printer()
{
	DeleteGlobals();
}

BOOL Printer::PrintDialog(Window *Obj,UINT Flags)
{
	pd.hwndOwner = Obj->hWnd;
	pd.Flags = Flags;

	while (PrintDlg(&pd)==0)
	{
		switch (CommDlgExtendedError())
		{
			case PDERR_DEFAULTDIFFERENT:
			{
				DEVNAMES *dn=(DEVNAMES*) GlobalLock(pd.hDevNames);
				dn->wDefault&=~DN_DEFAULTPRN;
				GlobalUnlock(pd.hDevNames);
				break;
			}
			case PDERR_PRINTERNOTFOUND:
				MessageBox(Obj->hWnd,"Printer not Found\r\nReverting to default printer","Error",MB_OK);
				GetDefault();
				pd.Flags = Flags; // reset flags as GetDefault changes them
				break;
			default:
				return FALSE;
		}
	}
	return TRUE;
}

void Printer::Setup(Window *Obj)
{
	PrintDialog(Obj,PD_PRINTSETUP);
	// pd now has updated hDevMode and hDevNames
}

void Printer::Print(Window *Obj)
{
	if (PrintDialog(Obj,PD_RETURNDC))
	{
		RECT Client;
		GetClientRect(Obj->hWnd,&Client);

		SetMapMode(pd.hDC, MM_ANISOTROPIC);
		SetWindowExtEx(pd.hDC, Client.right, Client.bottom,NULL);
		SetViewportExtEx(pd.hDC, GetDeviceCaps(pd.hDC,HORZRES),GetDeviceCaps(pd.hDC,VERTRES),NULL);

		DOCINFO dInfo={sizeof(DOCINFO),"Test-Doc",NULL};
		StartDoc(pd.hDC,&dInfo);
		StartPage(pd.hDC);

		Obj->Paint(pd.hDC,FALSE,Client);

		EndPage(pd.hDC);
		EndDoc(pd.hDC);

		DeleteDC(pd.hDC);
	}
}

