// Window definitions **************************

#include <mywin.h>
#pragma hdrstop

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

Window::Window(Object *Parent,char *aTitle,char *aName) : Object(Parent)
{
	//grab title
	Title=strdup(aTitle);
	Flags=0;
	Name=aName ? strdup(aName) : NULL;
	hWnd=0;
	//set defaults
	Style=WS_OVERLAPPEDWINDOW;
	x = CW_USEDEFAULT;
	y = 0;
	w = CW_USEDEFAULT;
	h = 0;

	Menu = NULL;
	OldStyle = 0;
	Icon=-1;
	// scroll bar stuff
	// sensible defalults ?

	xMax=GetSystemMetrics(SM_CXFULLSCREEN);
	yMax=GetSystemMetrics(SM_CYFULLSCREEN);
	xStep=yStep=1;
	xPos=yPos=0;
	StatBar=NULL;
}

char *Window::GetClassName()
{
	return App::Name;
}

/* The Object can be deleted in two ways
1) Windows sends a WM_CLOSE and then a WM_DESTROY message which does 'delete this'
2) The application does 'delete Object' at any time
*/

Window::~Window()
{
	//free memory
	if (Title) delete[] Title;
	if (Name) delete[] Name;
	if (HIWORD(Menu)) delete[] Menu;

	// no more messages, not even destroy
	SetWindowLong(hWnd,GWL_WNDPROC,(long) NullProc);
	SetPointer( hWnd, NULL );

	// only destroy if option 2) above
	if (!(Flags & W_DESTROYED)) DestroyWindow();
}

// MDI will change this
void Window::DestroyWindow()
{
	::DestroyWindow(hWnd);
}

BOOL Window::Register()
{
	WNDCLASS wc;

	if ( !GetClassInfo( 0, GetClassName(), &wc) &&
		 !GetClassInfo(App::hInstance, GetClassName(),&wc) )
	{
		wc.style = CS_DBLCLKS ; //CS_HREDRAW | CS_VREDRAW;
		wc.lpfnWndProc   = InitialisationProc;
		wc.cbClsExtra    = 0;
		// Reserve extra bytes for each instance of the window;
		// we will use these bytes to store a pointer to the C++
		// (MainWindow) object corresponding to the window.
		// the size of a 'this' pointer depends on the memory model.
		wc.cbWndExtra    = sizeof( Window * );

		wc.hInstance     = App::hInstance;
		wc.hIcon         = Icon>=0 ? LoadIcon( App::hInstance, MAKEINTRESOURCE(Icon))  : NULL;
		wc.hCursor       = LoadCursor( NULL, IDC_ARROW );
		wc.hbrBackground = (HBRUSH)(COLOR_WINDOW + 1);

		wc.lpszMenuName  = NULL;
		wc.lpszClassName = GetClassName();

		return RegisterClass( &wc );
	}
	return TRUE;
}


Window *Window::CreationWindow=NULL;

BOOL Window::Create()
{
	if (!Register()) return FALSE;
	
	HMENU hM=0;
	if (Menu) hM=LoadMenu(App::hInstance,Menu);
	if (Style & (WS_HSCROLL | WS_VSCROLL)) Flags|=W_SCROLLBARS;
	CreationWindow=this;
	hWnd=CreateWindow(GetClassName(),Title,Style,x,y,w,h,ParentHWnd,
		hM,App::hInstance,(LPSTR) this );
	if (!hWnd)	return FALSE;

	if (!SetupWindow()) 
	{
		DestroyWindow();
		return FALSE;
	}
	else
	{
		if (StatBar) StatBar->SetupWindow();
		// autocreate??
		// read in x,y,w,h if used setdef ??
	}
	return TRUE;
}

void Window::Destroy() {}

// could use Window event table for this stuff
BOOL Window::WndProc(TMSG &Msg)
{
	// handle internal actions
	switch (Msg.Msg)
	{
		case WM_QUERYENDSESSION:
		case WM_CLOSE:
			if (!CanClose()) return TRUE; //returns 0
			break;
		case WM_DESTROY:  // quit if main window
			if (this==App::MainWindow) App::Quit();
			if (Flags & W_SAVEPOS) SaveWindowState();
			Flags|=W_DESTROYED;
			Destroy();
			// MainWindow is deleted on program exit
			if (this==App::MainWindow) App::Quit();
			else delete this;
			return TRUE;
		case WM_PAINT:
			PAINTSTRUCT  ps;
			BeginPaint(hWnd, (LPPAINTSTRUCT) &ps);
			SetWindowOrgEx(ps.hdc,xPos,yPos,NULL);
			Paint(ps.hdc,ps.fErase,ps.rcPaint);

			if (StatBar) // draw statbar after window
			{
				SetWindowOrgEx(ps.hdc,0,0,NULL); // dont scroll statbar
				StatBar->Paint(ps.hdc);
			}
			EndPaint(hWnd, (LPPAINTSTRUCT) &ps);
			break;

		case WM_HSCROLL:			HScroll(Msg); break;
		case WM_VSCROLL:			VScroll(Msg); break;
		case WM_SIZE:				WMSize(Msg); break;
		case WM_GETMINMAXINFO:	WMGetMinMaxInfo(Msg); break;
		case WM_COMMAND:
		{
			int id=LOWORD(Msg.wParam);
			if (id>MRU_ID && id<=MRU_ID+MRU_MAX)
			{
				OpenFile(MruList[id-MRU_ID-1]);
				return TRUE;
			}
			else if (id>MRU2_ID && id<=MRU2_ID+MRU_MAX)
			{
				OpenFile2(MruList2[id-MRU2_ID-1]);
				return TRUE;
			}
			break;
		}
		case WM_INITMENU:
			InitMenu((HMENU) Msg.wParam); // handle of menu to initialize 
			break;
 	}
	if (StatBar) StatBar->EV_FIND(Msg);
	//allow user to grab any message
	return EV_FIND(Msg);
}

BOOL Window::SetupWindow() { return TRUE; }
void Window::OpenFile(char*) {}
void Window::OpenFile2(char*) {}
void Window::InitMenu(HMENU) {}

void Window::Paint(HDC, BOOL, RECT&) {}

void Window::EnableMenuItem(int id,BOOL state) {
HMENU hmenu=GetMenu(hWnd);
::EnableMenuItem(hmenu,id,MF_BYCOMMAND | (state) ? MF_ENABLED : MF_GRAYED);
}

void Window::CheckMenuItem(int id,BOOL state) {
HMENU hmenu=GetMenu(hWnd);
::CheckMenuItem(hmenu,id,MF_BYCOMMAND | (state) ? MF_CHECKED : MF_UNCHECKED);
}


void Window::AssignMenu(LPSTR menu)
{
	HMENU OldMenu,hM;
	if (HIWORD(Menu)) delete[] Menu;

	if (HIWORD(menu)) Menu = strdup(menu);
	else Menu = menu;

	if (hWnd) {
		OldMenu=GetMenu(hWnd);
		hM=menu ? LoadMenu(App::hInstance,menu) : (HMENU) NULL;
		SetMenu(hWnd,hM);

		if (OldMenu) DestroyMenu(OldMenu);
		}
}

void Window::AssignMenu(int MenuId) {
AssignMenu((LPSTR)MAKEINTRESOURCE(MenuId));
}

void Window::ShowTitleBar() {
Style=GetWindowLong(hWnd,GWL_STYLE); // need this ?
Style|=WS_CAPTION | OldStyle;
SetWindowLong(hWnd,GWL_STYLE,Style);
SetWindowPos(hWnd,NULL, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_DRAWFRAME);
ShowWindow(hWnd,SW_SHOW);
// allow update of scroll bars ?
}

void Window::HideTitleBar() {
Style=GetWindowLong(hWnd,GWL_STYLE); // need this ?
OldStyle=Style & ( WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU);
Style&=~( (WS_CAPTION & ~WS_BORDER) | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU);
SetWindowLong(hWnd,GWL_STYLE,Style);
SetWindowPos(hWnd,NULL, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_DRAWFRAME);
ShowWindow(hWnd,SW_SHOW);
// allow update of scroll bars ?
}

BOOL Window::WMGetMinMaxInfo(TMSG &Msg)
{
	if (!(Flags & W_SCROLLBARS) || (Flags & W_OVERSCROLL)) return TRUE;
	MINMAXINFO *lpmmi = (MINMAXINFO*) Msg.lParam; /* address of structure */
#ifdef WIN32
	RECT rc={0,0,xMax,yMax}; // client area
	AdjustWindowRect(&rc,Style,(BOOL) GetMenu(hWnd));
	rc.right+=GetSystemMetrics(SM_CXVSCROLL);
	rc.bottom+=GetSystemMetrics(SM_CYHSCROLL)-rc.top;
	
	// AdjustWindowRect should do this ???????????????
	int cxf=GetSystemMetrics(SM_CXSIZEFRAME);
	int cyf=GetSystemMetrics(SM_CYSIZEFRAME);
	rc.right+=2*cxf+1;
	rc.bottom+=2*cyf+1;
	
	RECT Deskrc;
	GetWindowRect(GetDesktopWindow(),&Deskrc);
	if ( Deskrc.right > rc.right ) lpmmi->ptMaxTrackSize.x=lpmmi->ptMaxSize.x=rc.right;
	if ( Deskrc.bottom > rc.bottom ) lpmmi->ptMaxTrackSize.y=lpmmi->ptMaxSize.y=rc.bottom;
	lpmmi->ptMaxPosition.x=(Deskrc.right-lpmmi->ptMaxSize.x)/2;
	lpmmi->ptMaxPosition.y=(Deskrc.bottom-lpmmi->ptMaxSize.y)/2;

#else
	//as xyMax are long only replace if neccesary, when int > xyMax so ok to cast to (int)
	RECT rc;
	GetWindowRect(GetDesktopWindow(),&rc);

	int cxf=GetSystemMetrics(SM_CXFRAME);
	int cxvs=GetSystemMetrics(SM_CXVSCROLL);
	int cyf=GetSystemMetrics(SM_CYFRAME);
	int cyhs=GetSystemMetrics(SM_CYHSCROLL);

	int cym= (GetMenu(hWnd)) ? GetSystemMetrics(SM_CYMENU) : 0;
	int cyc= (Style&( (WS_CAPTION & ~WS_BORDER) | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU))
			? GetSystemMetrics(SM_CYCAPTION) : 0;

	if ( rc.right > xMax ) lpmmi->ptMaxSize.x=(int) xMax+2*cxf+cxvs;
	if ( rc.bottom > yMax ) lpmmi->ptMaxSize.y=(int) yMax+2*cyf+cyhs+cyc+cym;
	lpmmi->ptMaxPosition.x=(rc.right-lpmmi->ptMaxSize.x)/2;
	lpmmi->ptMaxPosition.y=(rc.bottom-lpmmi->ptMaxSize.y)/2;
	if ( rc.right > xMax ) lpmmi->ptMaxTrackSize.x=(int) xMax+2*cxf+cxvs;
	if ( rc.bottom > yMax ) lpmmi->ptMaxTrackSize.y=(int) yMax+2*cyf+cyhs+cyc+cym;
#endif
	return TRUE;
}

BOOL Window::WMSize(TMSG &Msg)
{
	if (!(Flags & W_SCROLLBARS)) return TRUE;
	
	//int nWidth = LOWORD(Msg.lParam);  /* width of client area              */
	//int nHeight = HIWORD(Msg.lParam); /* height of client area             */
	
	switch (Msg.wParam)
	{
		case SIZE_MAXIMIZED:
		case SIZE_RESTORED:
			SetVirtualExtent(xMax,yMax,xStep,yStep,0,0);
			break;

		case SIZE_MINIMIZED:
		case SIZE_MAXHIDE:
		case SIZE_MAXSHOW:
			break;
	}

	return TRUE;
}


void Window::SetVirtualExtent(long xSize,long ySize,int xSt, int ySt,int xPgSize,int yPgSize)
{
	// prevent reentrance
	static BOOL InFn=FALSE;
	if (InFn) return;
	InFn=TRUE;

	xMax=xSize;
	yMax=ySize;
	xStep=xSt;
	yStep=ySt;

	RECT rc;
	GetClientRect(hWnd,&rc);
	if (StatBar) rc.bottom-=StatBar->dyStatbar;
	xPageSize=xPgSize ? xPgSize :rc.right;
	yPageSize=yPgSize ? yPgSize :rc.bottom;
	
#ifdef WIN32

	SCROLLINFO si;
	si.cbSize=sizeof(si);
	si.fMask=SIF_ALL;
	si.nMin=0;
	si.nMax=xMax-1;
	si.nPage=xPageSize;
	si.nPos=xPos;
	SetScrollInfo(hWnd,SB_HORZ,&si,TRUE);
	si.nMax=yMax-1;
	si.nPage=yPageSize;
	si.nPos=yPos;
	SetScrollInfo(hWnd,SB_VERT,&si,TRUE);
	// scoll bars may have gone
	GetClientRect(hWnd,&rc);
	if (StatBar) rc.bottom-=StatBar->dyStatbar;
	// resive oversized window
	if (!(Flags & W_OVERSCROLL) && rc.right>=xMax && rc.bottom>=yMax)
	{
		int newW= min( (long) rc.right, xMax );
		int newH= min( (long) rc.bottom, yMax );
		RECT rc2;
		GetWindowRect(hWnd,&rc2);
		SetWindowPos(hWnd,NULL,0,0,newW+rc2.right-rc2.left-rc.right,
				newH+rc2.bottom-rc2.top-rc.bottom,
				SWP_NOMOVE | SWP_NOZORDER);
//		GetClientRect(hWnd,&rc);
//		if (StatBar) rc.bottom-=StatBar->dyStatbar;
	}
	xPageSize=xPgSize ? xPgSize :rc.right;
	yPageSize=yPgSize ? yPgSize :rc.bottom;
	si.fMask=SIF_PAGE;
	si.nPage=xPageSize;
	SetScrollInfo(hWnd,SB_HORZ,&si,TRUE);
	si.nPage=yPageSize;
	SetScrollInfo(hWnd,SB_VERT,&si,TRUE);
	si.fMask=SIF_POS;
	GetScrollInfo(hWnd,SB_HORZ,&si);
	int newxPos=si.nPos;
	GetScrollInfo(hWnd,SB_VERT,&si);
	int newyPos=si.nPos;
	if (newxPos!=xPos || newyPos!=yPos)
	{
		ScrollWindow(hWnd, xPos-newxPos, yPos-newyPos , NULL, NULL);
		xPos=newxPos;
		yPos=newyPos;
		UpdateWindow(hWnd);
	}

#else

	SetScrollRange(hWnd,SB_HORZ,0, max ( (int) ( xMax - rc.right ), 0 ) , FALSE );
	// may have lost scrollbar, so get size again
	GetClientRect(hWnd,&rc);
	if (StatBar) rc.bottom-=StatBar->dyStatbar;
	xPageSize=xPgSize ? xPgSize :rc.right;
	yPageSize=yPgSize ? yPgSize :rc.bottom;
	SetScrollRange(hWnd,SB_VERT,0, max ( (int) ( yMax - rc.bottom ), 0 ), FALSE );

	long newxPos = max ( (long) min( xPos , xMax-rc.right ) , 0L);
	long newyPos = max ( (long) min( yPos , yMax-rc.bottom ) , 0L);
	if (newxPos!=xPos || newyPos!=yPos)
	{
		ScrollWindow(hWnd, (int) (xPos-newxPos), (int) (yPos-newyPos) , NULL, NULL);
		xPos=newxPos;
		yPos=newyPos;
		UpdateWindow(hWnd);
	}
	SetScrollPos(hWnd, SB_HORZ, (int) xPos, TRUE);
	SetScrollPos(hWnd, SB_VERT, (int) yPos, TRUE);

	if (!(Flags & W_OVERSCROLL) && rc.right>=xMax && rc.bottom>=yMax)
	{
		int newW= min( (long) rc.right, xMax );
		int newH= min( (long) rc.bottom, yMax );
		RECT rc2;
		GetWindowRect(hWnd,&rc2);
		SetWindowPos(hWnd,NULL,0,0,newW+rc2.right-rc2.left-rc.right,
				newH+rc2.bottom-rc2.top-rc.bottom,
				SWP_NOMOVE | SWP_NOZORDER);
	}
#endif

	InFn=FALSE;
	Flags |= W_SCROLLBARS;
}

// WM_VSCROLL
BOOL Window::VScroll(TMSG &Msg)
{
if (!(Flags & W_SCROLLBARS)) return TRUE;

int nScrollCode = LOWORD(Msg.wParam);   /* scroll bar value     */
int nPos = LLHW(Msg);   /* scroll box position  */

long newPos=yPos;
switch (nScrollCode) {
	case SB_LINEUP:
		newPos-=yStep; break;
	case SB_PAGEUP:
		newPos-=yPageSize; break;
	case SB_LINEDOWN:
		newPos+=yStep; break;
	case SB_PAGEDOWN:
		newPos+=yPageSize; break;
	case SB_THUMBTRACK:
	case SB_THUMBPOSITION:
		newPos=(long) nPos; break;
	case SB_BOTTOM:
		newPos=yMax; break;
	case SB_TOP:
		newPos=0; break;
	case SB_ENDSCROLL:
		break;
	}
#ifdef WIN32
	if (newPos != yPos)
	{
		if (StatBar) StatBar->Invalidate();
		SCROLLINFO si;
		si.cbSize=sizeof(si);
		si.fMask=SIF_POS;
		si.nPos=newPos;
		SetScrollInfo(hWnd,SB_VERT,&si,TRUE);
		GetScrollInfo(hWnd,SB_VERT,&si);

		ScrollWindow(hWnd, 0, yPos-si.nPos , NULL, NULL);
		yPos=si.nPos;
		UpdateWindow(hWnd);
	}

#else
	RECT rc;
	GetClientRect(hWnd,&rc);
	if (StatBar) rc.bottom-=StatBar->dyStatbar;
	if ( (newPos = max( min( newPos, yMax-rc.bottom ), 0L)) != yPos)
	{
		if (StatBar) StatBar->Invalidate();
		ScrollWindow(hWnd, 0, (int) (yPos-newPos) , NULL, NULL);
		SetScrollPos(hWnd, SB_VERT, (int) newPos, TRUE);
		yPos=newPos;
		UpdateWindow(hWnd);
	}
#endif

return TRUE;
}


BOOL  Window::HScroll(TMSG &Msg)
{
if (!(Flags & W_SCROLLBARS)) return TRUE;

int nScrollCode = LOWORD(Msg.wParam);   /* scroll bar value     */
int nPos = LLHW(Msg);   /* scroll box position  */

long newPos=xPos;
switch (nScrollCode) {
	case SB_LINELEFT:
		newPos-=xStep; break;
	case SB_PAGELEFT:
		newPos-=xPageSize; break; // page??
	case SB_LINERIGHT:
		newPos+=xStep; break;
	case SB_PAGERIGHT:
		newPos+=xPageSize; break;
	case SB_THUMBTRACK:
	case SB_THUMBPOSITION:
		newPos=(long) nPos; break;
	case SB_LEFT:
		newPos=0; break;
	case SB_RIGHT:
		newPos=xMax; break;
	case SB_ENDSCROLL:
		break;
	}

#ifdef WIN32
	if (newPos != xPos)
	{
		if (StatBar) StatBar->Invalidate();
		SCROLLINFO si;
		si.cbSize=sizeof(si);
		si.fMask=SIF_POS;
		si.nPos=newPos;
		SetScrollInfo(hWnd,SB_HORZ,&si,TRUE);
		GetScrollInfo(hWnd,SB_HORZ,&si);

		ScrollWindow(hWnd, xPos-si.nPos ,0, NULL, NULL);
		xPos=si.nPos;
		UpdateWindow(hWnd);
	}
#else

	RECT rc;
	GetClientRect(hWnd,&rc);
	if ( (newPos = max( min( newPos , xMax-rc.right ), 0L)) !=xPos)
	{
		if (StatBar) StatBar->Invalidate();
		ScrollWindow(hWnd, (int) (xPos-newPos), 0 , NULL, NULL);
		SetScrollPos(hWnd, SB_HORZ, (int) newPos, TRUE);
		xPos=newPos;
		UpdateWindow(hWnd);
	}
#endif

return TRUE;
}

void Window::SetBkColor(COLORREF Col)
{
	HBRUSH Brush=CreateSolidBrush(Col);
	#ifdef WIN32
	HBRUSH OldBrush=(HBRUSH) SetClassLong(hWnd,GCL_HBRBACKGROUND,(LONG) Brush);
	#else
	HBRUSH OldBrush=SetClassWord(hWnd,GCW_HBRBACKGROUND,Brush);
	#endif
	DeleteObject(OldBrush); // what if stock objcect ?
	InvalidateRect(hWnd,NULL,TRUE);
}

void Window::SetIcon(int id)
{
	Icon=id;
	if (hWnd && Icon>=0)
	{
		HICON I=LoadIcon(App::hInstance,MAKEINTRESOURCE(id));
		#ifdef WIN32
		::SendMessage(hWnd,WM_SETICON,ICON_BIG,(LPARAM)I);
		#else
		HICON OldIcon=(HICON) SetClassWord(hWnd,GCW_HICON, I);
		if (OldIcon) DestroyIcon(OldIcon);
		#endif
	}
}


void Window::SaveWindowState()
{
	WINDOWPLACEMENT wp;
	wp.length=sizeof(WINDOWPLACEMENT);
	GetWindowPlacement(hWnd,&wp);

	if (Flags & W_SAVESTATE)
	{
		char *temp="Normal";
		BOOL RestToMax=wp.flags & WPF_RESTORETOMAXIMIZED;
		if (wp.showCmd==SW_SHOWMAXIMIZED ||
			( wp.showCmd==SW_SHOWMINIMIZED && !(Flags & W_SAVEMIN) && RestToMax ) ) temp="Maximized";
		else if ( (Flags & W_SAVEMIN) && (wp.showCmd==SW_SHOWMINIMIZED)) temp= RestToMax ? "MinFromMax" : "Minimized";
		else if ( wp.showCmd==SW_HIDE) temp="Hidden";
		WriteIniString(Name,"State",temp);
	}
	WriteIniString(Name,"Window",	String()<< (int) wp.rcNormalPosition.left << ',' << (int) wp.rcNormalPosition.top << ',' << (int) wp.rcNormalPosition.right  << ',' << (int) wp.rcNormalPosition.bottom);
}

void Window::GetWindowState(int aSaveFlags)
{
	if (Name==NULL) Name=strdup(Title);
	Flags|=aSaveFlags | W_SAVEPOS;

	WINDOWPLACEMENT wp;
	wp.length=sizeof(WINDOWPLACEMENT);
	if (hWnd) GetWindowPlacement(hWnd,&wp); // fill max position
	wp.flags=0;
	wp.showCmd=SW_SHOWNORMAL;

	String S;
	ReadIniString(Name,"State",S);
	if (!S.Empty())
	{
		if (S=="MinFromMax")
		{
			Style|=WS_MINIMIZE;
			wp.flags=WPF_RESTORETOMAXIMIZED;
			wp.showCmd=SW_SHOWMINIMIZED;
		}
		else if (S=="Maximized")
		{
			Style|=WS_MAXIMIZE;
			wp.showCmd=SW_SHOWMAXIMIZED;
		}
		else if (S=="Minimized")
		{
			Style|=WS_MINIMIZE;
			wp.showCmd=SW_SHOWMINIMIZED;
		}
		else if (S=="Hidden")
		{
			wp.showCmd=SW_HIDE;
		}
	}

	ReadIniString(Name,"Window",S="");
	if (!S.Empty())
	{
		int ww,hh;
		sscanf(S,"%d,%d,%d,%d",&x,&y,&ww,&hh);
		if (!(Flags & W_NOSIZE)) { w=ww-x; h=hh-y;}
		//	wp.ptMinPosition
		//	wp.ptMaxPosition.x=0;
		//	wp.ptMaxPosition.y=0;
		wp.rcNormalPosition.left=x;
		wp.rcNormalPosition.top=y;
		wp.rcNormalPosition.right=x+w;
		wp.rcNormalPosition.bottom=y+h;
	}
	if (this==App::MainWindow)
	{
		App::nCmdShow=wp.showCmd;
		// minimizing MDI frame now messes up
		if (wp.showCmd==SW_SHOWMINIMIZED)
		{
			wp.showCmd= (wp.flags==WPF_RESTORETOMAXIMIZED) ? SW_SHOWMAXIMIZED : SW_SHOWNORMAL;
		}
	}
	if (hWnd) SetWindowPlacement(hWnd,&wp);
}
