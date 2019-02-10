// App definitions *******************************

#include <mywin.h>
#pragma hdrstop

#include <stdlib.h>

HINSTANCE App::hInstance = 0;
HINSTANCE App::hPrevInstance = 0;

#ifdef WIN32
HKEY App::Machine;
HKEY App::User;
#endif

int App::nCmdShow = 0;
char *App::CmdLine = NULL;
char *App::Name = NULL;
char *App::IniFile = NULL;
Object *App::MainWindow = NULL;
BOOL App::Ctl3dEnabled=FALSE;
HINSTANCE App::hI3D;
TextWindow *App::TextWindow=NULL;
BOOL App::Active;

LRESULT CALLBACK InitialisationProc( HWND , UINT , WPARAM , LPARAM );

App::App(char *aName,char *Ini)
{
	Name=strdup(aName);
	if (Ini)
	{
		IniFile=strdup(Ini);
		#ifdef WIN32
		OpenKeys();
		#endif
	}
	Active=TRUE;
}

#ifdef WIN32
void App::OpenKeys()
{
	// open reg keys
	DWORD Disp;
	if (RegCreateKeyEx(HKEY_LOCAL_MACHINE,IniFile,0,"",REG_OPTION_NON_VOLATILE,KEY_ALL_ACCESS,NULL,&Machine,&Disp)==ERROR_SUCCESS)
	{
	}
	if (RegCreateKeyEx(HKEY_CURRENT_USER,IniFile,0,"",REG_OPTION_NON_VOLATILE,KEY_ALL_ACCESS,NULL,&User,&Disp)==ERROR_SUCCESS)
	{
	}
}
#endif

typedef BOOL (WINAPI *CallProcProc)(HANDLE);

void CallProc(HINSTANCE hi,LPSTR lp)
{
	CallProcProc fp=(CallProcProc) GetProcAddress(hi,lp);
	if (fp) fp(App::hInstance);
}

void App::EnableCtl3d()
{
	UINT OldError = SetErrorMode(SEM_NOOPENFILEERRORBOX);
	#ifdef WIN32
	if ((hI3D=LoadLibrary("ctl3d32.dll"))!=NULL) {
	#else
	if ((hI3D=LoadLibrary("ctl3dv2.dll"))>HINSTANCE_ERROR) {
	#endif
		Ctl3dEnabled=TRUE;

		CallProc(hI3D,"Ctl3dRegister");
		CallProc(hI3D,"Ctl3dAutoSubclass");
		}
	SetErrorMode(OldError);
}

App::~App()
{
	delete[] Name;
	if (IniFile) delete[] IniFile;
	if (MainWindow) delete MainWindow;
	if (Ctl3dEnabled)
	{
		CallProc(hI3D,"Ctl3dUnregister");
		FreeLibrary(hI3D);
	}
#ifdef WIN32
	RegCloseKey(Machine);
	RegCloseKey(User);
#endif
}

BOOL App::PeekMessage()
{
	MSG Msg;
	if (Active && ::PeekMessage(&Msg,NULL,0,0,PM_REMOVE))
	{
		TranslateMessage( &Msg );
		DispatchMessage( &Msg );
		return TRUE;
	}
	return FALSE;
}

void App::PeekLoop()
{
	while (PeekMessage());
}

void App::Quit()
{
	PostQuitMessage(0);
	Active=FALSE;
}

int App::MessageLoop( void )
{
	 MSG msg;

	 while( GetMessage( &msg, NULL, 0, 0 ) )
	 {
			TranslateMessage( &msg );
			DispatchMessage( &msg );
	 }
	 return msg.wParam;
}

int App::MainLoop()
{
	return MessageLoop();
}

int App::Run()
{
	InitMainWindow();
	if (!MainWindow) return 0;
	if (!MakeWindow(MainWindow)) return 0;
	
	ShowWindow(MainWindow->hWnd, nCmdShow );
	FirstIn();
	return MainLoop();
}

BOOL App::MakeWindow(Object *AObject)
{
	return AObject->Create();
}

