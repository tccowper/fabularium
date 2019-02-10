// status bar class
// drawn in parent window rather than independant window
#include <mywin.h>
#pragma hdrstop

#include <string.h>

// status box **********************************************

StatusBox::StatusBox(char *T,int X,int W)
{
	Text=strdup(T);
	x=X; w=W;
}

StatusBox::~StatusBox()
{
	if (Text) delete[] Text;
}

void StatusBox::SetText(char *T)
{
	if (Text) delete[] Text;
	Text=strdup(T);
}

// status bar **********************************************

//	void Paint(HDC dc);
EV_START(StatusBar)
	EV_MESSAGE(WM_KEYUP,WMKeyUp)
	EV_MESSAGE(WM_KEYDOWN,WMKeyDown)
	EV_MESSAGE(WM_SETFOCUS,WMFocus)
	EV_MESSAGE(WM_SIZE,WMSize)
	EV_MESSAGE(WM_GETMINMAXINFO,WMGetMinMaxInfo)
	EV_MESSAGE(WM_MENUSELECT,WMMenuSelect)
EV_END

StatusBox *StatusBar::AddBox(char *T,int w)
{
	StatusBox *S=new StatusBox(T,dyBorderx8 + Pos,w*CharWidth);
	BoxList.AddTail(S);
	Pos+=w*CharWidth+dyBorderx8;
	return S;
}

void StatusBar::InsertBox(int After,char *T,int w)
{
	StatusBox *Box=BoxList[After];

	StatusBox *S=new StatusBox(T,Box->x+Box->w+dyBorderx8,w*CharWidth);
	S->SetText(T);
	BoxList.AddAfter(S,Box);
	BoxList.GetNext(S);
	while (S)
	{
		S->x+=w*CharWidth+dyBorderx8;
		BoxList.GetNext(S);
	}
	DrawText(Box);
	Invalidate();
}


StatusBar::StatusBar(Object *P,int w,BOOL NumBox,BOOL CapsBox) : Object(P)
{
	dyBorder   = GetSystemMetrics(SM_CYBORDER);
	dyBorderx2 = dyBorder * 2;
	dyBorderx3 = dyBorder * 3;
	dyBorderx8 = dyBorder * 8;
	dyBorderx9 = dyBorder * 9;

	HDC dc = GetDC(NULL);
	int Fntheight = MulDiv(-10, GetDeviceCaps(dc, LOGPIXELSY), 72);
	Font = CreateFont(Fntheight, 0, 0, 0, 400, 0, 0, 0,
		 ANSI_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS,
		 DEFAULT_QUALITY, VARIABLE_PITCH | FF_SWISS, "Helv");
	SelectObject(dc, Font);

	TEXTMETRIC tm;
	GetTextMetrics(dc, &tm);
	ReleaseDC(NULL, dc);

	dyStatbar = tm.tmHeight + tm.tmExternalLeading + (7*dyBorder);
	CharWidth = tm.tmMaxCharWidth;
	Pos=0;
	TextBox=AddBox("",w);
	NumLock = NumBox ? AddBox("",3) : NULL;
	CapsLock = CapsBox ? AddBox("",3) :NULL;
	nMenu=0;
	OldText=NULL;
}

StatusBar::~StatusBar()
{
	if (OldText) delete[] OldText;
	DeleteObject(Font);
}

BOOL StatusBar::SetupWindow()
{
	nMenu=0;
	ParsePopup(GetMenu(ParentHWnd));
	return TRUE;
}

void StatusBar::SetText(char *T)
{
	TextBox->SetText(T);
	DrawText(TextBox);
}

void StatusBar::SetText(int Box,char *T)
{
	StatusBox *S=BoxList[Box];
	S->SetText(T);
	DrawText(S);
}

void StatusBar::DrawText(StatusBox *S)
{
	if (!S->Text) return;

	RECT rc,rcTemp;
	HDC dc = GetDC(ParentHWnd);
	GetClientRect(ParentHWnd, &rc);
	rc.top = rc.bottom - dyStatbar;


	rcTemp.top    = rc.top + dyBorder*4;
	rcTemp.bottom = rc.bottom - dyBorderx3;
	rcTemp.left   = dyBorder + S->x;
	rcTemp.right  = rcTemp.left + S->w- dyBorder;

	SelectObject(dc, Font);
	SetTextColor(dc, GetSysColor(COLOR_BTNTEXT));
	SetBkColor(dc, GetSysColor(COLOR_BTNFACE));

	ExtTextOut(dc, rcTemp.left + dyBorderx2, rcTemp.top,
						 ETO_OPAQUE | ETO_CLIPPED, &rcTemp, S->Text,
						 strlen(S->Text), NULL);
	ReleaseDC(ParentHWnd, dc);

}

void StatusBar::Paint(HDC dc)
{
	 RECT rc,rcTemp;
	 GetClientRect(ParentHWnd, &rc);
	 rc.top = rc.bottom - dyStatbar;

/* Border color */
	HBRUSH Brush = CreateSolidBrush(GetSysColor(COLOR_BTNFACE));
	rcTemp = rc;
	rcTemp.top += dyBorderx2;
	FillRect(dc, &rcTemp, Brush);
	DeleteObject(Brush);

/* Shadow color */
	Brush = CreateSolidBrush(GetSysColor(COLOR_BTNSHADOW));
	/* Top and left of boxes */
	StatusBox *Ptr=BoxList.GetFirst();
	while (Ptr)
	{
		rcTemp.top = rc.top + dyBorderx3;
		rcTemp.bottom = rcTemp.top + dyBorder;
		rcTemp.left = Ptr->x;
		rcTemp.right  = rcTemp.left + Ptr->w;
		FillRect(dc, &rcTemp, Brush);
		rcTemp.bottom = rc.bottom - dyBorderx2;
		rcTemp.right  = rcTemp.left + dyBorder;
		FillRect(dc, &rcTemp, Brush);
		BoxList.GetNext(Ptr);
	}
	DeleteObject(Brush);

	Brush = CreateSolidBrush(GetSysColor(COLOR_BTNHIGHLIGHT));
	/* Bottom and right of boxes */
	Ptr=BoxList.GetFirst();
	while (Ptr)
	{
		rcTemp.top    = rc.bottom  - dyBorderx3;
		rcTemp.bottom = rcTemp.top + dyBorder;
		rcTemp.left   = Ptr->x;
		rcTemp.right  = rcTemp.left + Ptr->w;
		FillRect(dc, &rcTemp, Brush);

		rcTemp.top    = rc.top + dyBorderx3;
		rcTemp.left   = Ptr->x+Ptr->w;
		rcTemp.right  = rcTemp.left + dyBorder;
		FillRect(dc, &rcTemp, Brush);

		BoxList.GetNext(Ptr);
	}

	/* Across the top */
	rcTemp = rc;
	rcTemp.top += dyBorder;
	rcTemp.bottom = rcTemp.top + dyBorder;
	FillRect(dc, &rcTemp, Brush);
	DeleteObject(Brush);

	/* solid black line across top */

	Brush = CreateSolidBrush(GetSysColor(COLOR_WINDOWTEXT));
	rcTemp = rc;
	rcTemp.bottom = rcTemp.top;
	rcTemp.bottom += dyBorder;
	FillRect(dc, &rcTemp, Brush);
	DeleteObject(Brush);

	/* now the text, with the button face background */

	SelectObject(dc, Font);
	SetTextColor(dc, GetSysColor(COLOR_BTNTEXT));
	SetBkColor(dc, GetSysColor(COLOR_BTNFACE));

	 /* Text */
	Ptr=BoxList.GetFirst();
	while (Ptr)
	{
		if (Ptr->Text)
		{
			rcTemp.top    = rc.top + dyBorder*4;
			rcTemp.bottom = rc.bottom - dyBorderx3;
			rcTemp.left   = dyBorder + Ptr->x;
			rcTemp.right  = rcTemp.left + Ptr->w - dyBorder;
			ExtTextOut(dc, rcTemp.left + dyBorderx2, rcTemp.top,
						ETO_OPAQUE | ETO_CLIPPED, &rcTemp, Ptr->Text,
						strlen(Ptr->Text), NULL);
		}
		BoxList.GetNext(Ptr);
	}
}

BOOL StatusBar::bInKeyDown=FALSE;

BOOL StatusBar::WMKeyUp(TMSG &)
{
	bInKeyDown = FALSE;
	return TRUE;
}

BOOL StatusBar::WMKeyDown(TMSG &Msg)
{
	if ( !bInKeyDown )
	{
		if ( Msg.wParam == VK_CAPITAL ) SetCapsLockText();
		else if ( Msg.wParam == VK_NUMLOCK ) SetNumLockText();
		bInKeyDown = TRUE;
	}
	return TRUE;
}

BOOL StatusBar::WMFocus(TMSG &)
{
	SetCapsLockText();
	SetNumLockText();
	return TRUE;
}

void StatusBar::SetNumLockText()
{
	/* Num Lock Text */
	if (NumLock)
	{
		int State=GetKeyState(VK_NUMLOCK) & 0x0001;
		NumLock->SetText(State ? "NUM" : "");
		DrawText(NumLock);
	}
}

void StatusBar::SetCapsLockText()
{
	/* Caps Lock Text */
	if (CapsLock)
	{
		int State=GetKeyState(VK_CAPITAL) & 0x0001;
		CapsLock->SetText(State ? "CAPS" : "");
		DrawText(CapsLock);
	}
}

void StatusBar::Invalidate()
{
	RECT rc;
	GetClientRect(ParentHWnd, &rc);
	rc.top = rc.bottom - dyStatbar;
	InvalidateRect(ParentHWnd,&rc,TRUE);
}

BOOL StatusBar::WMSize(TMSG &)
{
	RECT rc;
	GetClientRect(ParentHWnd, &rc);
	rc.top = rc.bottom - dyStatbar;
	if (rc.top < StatRecSizeAdv.top)
	{
		/* Window SHRANK, need to invalidate current status rect */
		InvalidateRect(ParentHWnd, &rc,FALSE);
	}
	else
	{
		/* Window GREW, need to invalidate prev status rect */
		InvalidateRect(ParentHWnd, &StatRecSizeAdv,TRUE);
	}
	return TRUE;
}

BOOL StatusBar::WMGetMinMaxInfo(TMSG &)
{
	GetClientRect(ParentHWnd, &StatRecSizeAdv);
	StatRecSizeAdv.top = StatRecSizeAdv.bottom - dyStatbar;
	return TRUE;
}

#define NOMENU 0
// WM_MENUSELECT
BOOL StatusBar::WMMenuSelect(TMSG &Msg)
{
	int nMenuID=NOMENU;
	WORD flags=LLHW(Msg);
	WORD item=LOWWPARAM(Msg.wParam);

	if (HILP32(Msg) == NULL && flags == 0xffff)
	{
		// Exiting menu mode.  hMenu && hmenuPopup are NULL.
		//nMenuID = NOMENU;

		if (OldText)
		{
			SetText(OldText);
			delete[] OldText;
			OldText=NULL;
		}

		return TRUE;
	}
	else if (flags & MF_SYSMENU)
	{
		// System menu is up
		if (!(flags & MF_POPUP)) nMenuID = item;
			// System menu item is selected
			// System menu: item contains SC_* code.
		else nMenuID = IDM_SYSMENU;
			// System menu and no item is selected
	}
	else
	{
		if ((flags & MF_POPUP))
		{
			// Pulling down a popup submenu: hmenuPopup contains popup handle.
			#ifdef WIN32
			HMENU hm=GetSubMenu((HMENU) Msg.lParam,item);
			for (int i=0;i<nMenu;i++)
				if (hm==hMenu[i])
				{
					nMenuID=IDS_POPUP1+i;
					break;
				}
			#else
			for (int i=0;i<nMenu;i++)
				if (item==hMenu[i])
				{
					nMenuID=IDS_POPUP1+i;
					break;
				}
			#endif
		}
		else nMenuID = item;
	 }

	//SetStatbarText(hwnd, nMenuID,TRUE);
	char temp[255];
	if ( LoadString(App::hInstance, nMenuID, temp, 255) == 0)
		strcpy(temp,"");
	if (!OldText) OldText=strdup(TextBox->Text ? TextBox->Text : ""); // null is reserved to indicate not in menu
	SetText(temp);
	return TRUE;
}

void StatusBar::ParsePopup(HMENU hm)
{
	if (hm==NULL) return;
	HMENU hm2;
	int n=GetMenuItemCount(hm);
	for (int i=0;i<n;i++)
	{
		hm2=GetSubMenu(hm,i);
		if (hm2)
		{
			// add hm2 to hmenu list
			hMenu[nMenu++]=hm2;
			ParsePopup(hm2);
		}
	}
}

