// mdi.h

class MDIClient : public Object
{
private:
	DWORD Style;
	int x,y,w,h;

public:
	MDIClient(Object*);
	BOOL Create();
};

class MDIChild : public Window
{
public:
	MDIChild(Object*,char*,char *Name=NULL);
	~MDIChild();
	virtual char *GetClassName();
	LRESULT DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam);
	BOOL Create();
	void DestroyWindow();
};

//	LRESULT WMKeyUp(TMSG &);
//	LRESULT WMKeyDown(TMSG &);

//	EV_ENABLE(MDIChild)


class MDIFrame : public Window
{
private:
	MDIClient *MDI;
public:
	HWND hClient;
	MDIFrame(Object *,char *,char *N=NULL);
	LRESULT DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam);

	BOOL SetupWindow();
	void SetWindowMenu(int Pos);

	void MDITile() { SendMessage(hClient,WM_MDITILE,0,0); }
	void MDICascade() { SendMessage(hClient,WM_MDICASCADE,0,0); }
	void MDIArrangeIcons() { SendMessage(hClient,WM_MDIICONARRANGE,0,0); }


	BOOL WMSize(TMSG &);

	EV_ENABLE(MDIFrame)
};
