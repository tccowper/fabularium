// dde stuff
#include <mywin.h>
#pragma hdrstop

#include <dde.h>

#include <string.h>

EV_START(DDEWindow)
	EV_MESSAGE(WM_DDE_TERMINATE,WMDdeTerminate)
	EV_MESSAGE(WM_DDE_ACK,WMDdeAck)
	EV_MESSAGE(WM_DDE_DATA,WMDDEData)
EV_END

//derived windiw should call this if overridden
void DDEWindow::Destroy()
{
	if (ServerWindow) DDETerminate();
}

BOOL DDEWindow::DDEInitialise(char *App,char *Topic)
{
	ServerWindow=NULL;
	ATOM aApp=GlobalAddAtom(App);
	ATOM aTopic=GlobalAddAtom(Topic);
	SendMessage((HWND)-1, SentMessage=WM_DDE_INITIATE, (WPARAM) hWnd, MAKELONG(aApp, aTopic));
	GlobalDeleteAtom(aApp);
	GlobalDeleteAtom(aTopic);
	// Ack should have come already
	SentMessage=0;
	return (ServerWindow!=NULL);
}

void DDEWindow::DDETerminate()
{
	HWND W = ServerWindow;
	ServerWindow = NULL;
	if ( IsWindow( W ) ) PostMessage( W, WM_DDE_TERMINATE, (WPARAM) hWnd, 0 );
}

BOOL DDEWindow::WMDdeTerminate(TMSG &Msg)
{
	// server is terminating?
	if ( (HWND) Msg.wParam == ServerWindow )	DDETerminate();
	return TRUE;
}

BOOL DDEWindow::WMDdeAck(TMSG &Msg)
{
	switch (SentMessage)
	{
		case WM_DDE_INITIATE:
		{
			if ( ServerWindow == NULL ) ServerWindow = (HWND) Msg.wParam;
			else PostMessage( (HWND) Msg.wParam, WM_DDE_TERMINATE, (LPARAM) hWnd,0); // already got response
			GlobalDeleteAtom( LOWORD(Msg.lParam) );
			GlobalDeleteAtom( HIWORD(Msg.lParam) );
			break;
		}
		case WM_DDE_EXECUTE:
		{
			GlobalFree( (HGLOBAL) HIWORD(Msg.lParam) );
			SentMessage = 0;
			WORD Status=LOWORD(Msg.lParam);
			AckFlag=((DDEACK*)&Status)->fAck;
//			SetFocus( hWnd ); // ??
			break;
		}
		case WM_DDE_REQUEST:
			GlobalDeleteAtom( HIWORD(Msg.lParam) );
			SentMessage = 0;
			break;
	}
	return TRUE;
}

void DDEWindow::Wait()
{
	DWORD Time=GetCurrentTime();
	MSG Msg;
	while ( SentMessage && (GetCurrentTime() - Time < 3000) )
	{
		if (PeekMessage( &Msg, hWnd, 0, 0, PM_REMOVE ) )
		{
			TranslateMessage(&Msg);
			DispatchMessage(&Msg);
		}
	}
}

BOOL DDEWindow::DDESend(char *c)
{
	HGLOBAL HCommands = GlobalAlloc( GHND | GMEM_DDESHARE, strlen(c));
	if ( !HCommands ) return FALSE;

	LPSTR lpCommands = (LPSTR) GlobalLock( HCommands ); // lock while putting in data
	strcpy(lpCommands,c);
	GlobalUnlock( HCommands );
	if ( PostMessage( ServerWindow, WM_DDE_EXECUTE, (WPARAM) hWnd, MAKELONG(0,HCommands) ) )
		SentMessage = WM_DDE_EXECUTE;
	else
	{
		GlobalFree( HCommands );
		return FALSE;
	}

	AckFlag=FALSE;
	Wait();
	return AckFlag;
}

BOOL DDEWindow::DDDRequest(char **d,char *Item)
{
	DataPtr=d;
	SentMessage=WM_DDE_REQUEST;
	ATOM aItem=GlobalAddAtom(Item);
	PostMessage(ServerWindow,WM_DDE_REQUEST,(WPARAM) hWnd,MAKELPARAM(CF_TEXT,aItem));
	AckFlag=FALSE;
	Wait();
	return AckFlag;
}

BOOL DDEWindow::WMDDEData(TMSG &Msg)
{
	SentMessage=0;
	HGLOBAL hData=(HGLOBAL) LOWORD(Msg.lParam);
	if (hData==NULL) return FALSE;
	else
	{
	DDEDATA *DDEData=(DDEDATA *) GlobalLock(hData);
	if (DDEData->fAckReq) PostMessage(ServerWindow,WM_DDE_ACK,(WPARAM) hWnd,MAKELPARAM(0x8000,HIWORD(Msg.lParam)));
	else GlobalDeleteAtom(HIWORD(Msg.lParam));
	*DataPtr=strdup((char*)DDEData->Value);
	BOOL rel=DDEData->fRelease;
	GlobalUnlock(hData);
	if (rel) GlobalFree(hData);
	}
	return AckFlag=TRUE;
}

