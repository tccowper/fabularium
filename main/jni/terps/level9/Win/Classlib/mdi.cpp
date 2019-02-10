// MDIClient
#include <mywin.h>
#pragma hdrstop

#include <stdlib.h>

// Client ****************************************************

MDIClient::MDIClient(Object *P) : Object(P)
{
	Style= WS_CHILD | WS_VISIBLE | WS_GROUP | WS_TABSTOP | WS_CLIPCHILDREN;
	x = CW_USEDEFAULT;
	y = 0;
	w = CW_USEDEFAULT;
	h = 0;
}

BOOL MDIClient::Create()
{
	if ( x == CW_USEDEFAULT || w == CW_USEDEFAULT )
	{
		RECT rc;
		GetClientRect(ParentHWnd,&rc);
		x = rc.left;
		y = rc.top;
		w = rc.right - rc.left;
		h = rc.bottom - rc.top;
	}

	CLIENTCREATESTRUCT ccs={NULL,CM_FIRSTCHILD};
	hWnd=CreateWindow("MDICLIENT","",Style,x,y,w,h,ParentHWnd,
		NULL,App::hInstance, &ccs );
	return hWnd!=NULL;
}

// Child ****************************************************

/*
EV_START(MDIClient)
	EV_MESSAGE(WM_KEYUP,WMKeyUp)
	EV_MESSAGE(WM_KEYDOWN,WMKeyDown)
EV_END
*/

MDIChild::MDIChild(Object *P,char *T,char *N) : Window (P,T,N)
{
	Style=WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS;
}

MDIChild::~MDIChild()
{
}

LRESULT MDIChild::DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam)
{
	return DefMDIChildProc(hWnd,Msg,wParam,lParam);
}

char *MDIChild::GetClassName()
{
	return "Child";
}

BOOL MDIChild::Create()
{
	if (!Register()) return FALSE;
	
	MDICREATESTRUCT mcs=
		{
		GetClassName(),Title,App::hInstance,x,y,w,h,Style,NULL
		};

	if (Style & (WS_HSCROLL | WS_VSCROLL)) Flags|=W_SCROLLBARS;

	CreationWindow=this;

#ifdef WIN32
	hWnd = (HWND) SendMessage( ((MDIFrame*)Parent)->hClient, WM_MDICREATE, 0, (LPARAM)(&mcs));
#else
	hWnd = LOWORD(SendMessage( ((MDIFrame*)Parent)->hClient, WM_MDICREATE, 0, (LPARAM)(&mcs)));
#endif
	if (!hWnd)	return FALSE;

	// user may not want child to save its position??
	if (Name) GetWindowState(W_SAVESTATE | W_SAVEMIN);
	return SetupWindow();
}

void MDIChild::DestroyWindow()
{
	SendMessage( ((MDIFrame*)Parent)->hClient, WM_MDIDESTROY, (WPARAM) hWnd, 0);
}

// pass keypresses to StatBar if present
/*
LRESULT MDIClient::WMKeyUp(TMSG &Msg)
{
	if (((MDIFrame*)Parent)->StatBar) ((MDIFrame*)Parent)->StatBar->WMKeyUp(Msg);
}

LRESULT MDIClient::WMKeyDown(TMSG &Msg)
{
	if (((MDIFrame*)Parent)->StatBar) ((MDIFrame*)Parent)->StatBar->WMKeyDown(Msg);
}
*/

// Frame **********************************************************

EV_START(MDIFrame)
	EV_COMMAND(CM_MDITILE,MDITile)
	EV_COMMAND(CM_MDICASCADE,MDICascade)
	EV_COMMAND(CM_MDIARRANGEICONS,MDIArrangeIcons)

	EV_MESSAGE(WM_SIZE,WMSize)
EV_END

MDIFrame::MDIFrame(Object *P,char *T,char *N) : Window(P,T,N)
{
}

LRESULT MDIFrame::DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam)
{
	return DefFrameProc(hWnd,hClient,Msg,wParam,lParam);
}

// needs to be called from derived SetupWindow
BOOL MDIFrame::SetupWindow()
{
	MDI=new MDIClient(this);
	if (!MDI || !MDI->Create()) return FALSE;
	hClient=MDI->hWnd;
	return TRUE;
}

// pick up WM_SIZE in case of StatBar
BOOL MDIFrame::WMSize(TMSG &Msg)
{
	int h=HIWORD(Msg.lParam);
	if (StatBar) h-=StatBar->dyStatbar;
	MoveWindow(hClient, 0,0, LOWORD(Msg.lParam), h, TRUE);
	return TRUE;
}

void MDIFrame::SetWindowMenu(int Pos)
{
	HMENU hm1=GetMenu(hWnd);
	HMENU hm2=GetSubMenu(hm1,Pos);
	#ifdef WIN32
	SendMessage(hClient,WM_MDISETMENU,(WPARAM) NULL,(LPARAM) hm2);
	#else
	SendMessage(hClient,WM_MDISETMENU,FALSE,MAKELPARAM(hm1,hm2));
	#endif
	DrawMenuBar(hWnd);
}

//Change Menu on child activation
//key press in child for statbar

