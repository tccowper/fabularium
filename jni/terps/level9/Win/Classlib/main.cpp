// WinMain and object init
#include <mywin.h>
#pragma hdrstop

#include <stringc.h>

// Window::CreationWindow should be set
LRESULT CALLBACK InitialisationProc( HWND hWnd, UINT iMessage, WPARAM wParam,
											LPARAM lParam )
{
	Window *CW=Window::CreationWindow;
	if ( iMessage == WM_CREATE )
	{
		// Store a pointer to this object in the window's extra bytes;
		// this will enable us to access this object (and its member
		// functions) in WndProc where we are
		// given only a handle to identify the window.
		Window::CreationWindow=NULL;
		SetPointer( hWnd, CW );
		// make sure hWnd is set of correct handling of WM_CREATE
		CW->hWnd=hWnd;
		// Now let the object perform whatever
		// initialization it needs for WM_CREATE in its own
		// WndProc.
		SetWindowLong(hWnd,GWL_WNDPROC,(long) MainWndProc);

		TMSG Msg={iMessage,wParam,lParam,0};
		if (CW->WndProc(Msg)) return Msg.RetVal;
	}
	// The messages that
	// precede WM_CREATE must be processed without using pWindow so we
	// pass them to DefProc.
	return CW->DefProc( hWnd, iMessage, wParam, lParam );
}

LRESULT CALLBACK MainWndProc( HWND hWnd, UINT iMessage, WPARAM wParam,
											LPARAM lParam )
{
	// Pointer to the (C++ object that is the) window.
	Window *pWindow = GetPointer( hWnd );
	TMSG Msg={iMessage,wParam,lParam,0};
	if (pWindow->WndProc(Msg)) return Msg.RetVal;
	return pWindow->DefProc( hWnd, iMessage, wParam, lParam );
}

// this may call the wrong proc (MDI frame or child, but so what??)
LRESULT CALLBACK NullProc( HWND hWnd, UINT iMessage, WPARAM wParam,
											LPARAM lParam )
{
	return DefWindowProc( hWnd, iMessage, wParam, lParam );
}

// WinMain *************************************

int Main();

int PASCAL WinMain( HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpszCmdLine, int nCmdShow )
{
	App::hInstance = hInstance;
	App::hPrevInstance = hPrevInstance;
	App::CmdLine = lpszCmdLine;
	App::nCmdShow = nCmdShow;

	int ret=Main();

	// TextWindow open
	if (App::TextWindow && (App::TextWindow->StayOpen))
	{
		App::TextWindow->Parent=NULL;
		App::MainWindow=(Object *) App::TextWindow;
		String S(GetWindowTextLength(App::MainWindow->hWnd)+1);
		GetWindowText(App::MainWindow->hWnd,S,S.Size());
		S.Update();
		S << " (finished)";
		SetWindowText(App::MainWindow->hWnd,S);
		ShowWindow(App::MainWindow->hWnd, SW_SHOW );
		// wait until closed
		return App::MessageLoop();
		// delete App::TextWindow; ??
		}
	return ret;
}

