// app.h

class App
{
public:
	App(char *aName,char *Ini=NULL);
	virtual ~App();
	static HINSTANCE hInstance;
	static HINSTANCE hPrevInstance;
#ifdef WIN32
	static HKEY Machine;
	static HKEY User;
	static void OpenKeys();
#endif
	static char *CmdLine;
	static int nCmdShow;
	static Object *MainWindow;
	static TextWindow *TextWindow;
	static char *Name;
	static char *IniFile;
	static int MessageLoop( void );
	virtual int MainLoop();
	static void PeekLoop();
	static BOOL PeekMessage();
	void EnableCtl3d();
	int Run();
	virtual void FirstIn() {};
	static BOOL Active;
	static void Quit();
private:
	static BOOL Ctl3dEnabled;
	static HINSTANCE hI3D;
	virtual void InitMainWindow() =0 ;
	BOOL MakeWindow(Object *);
};
