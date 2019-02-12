// window.h

#define W_SAVEPOS 1
#define W_SAVESTATE 2
#define W_SAVEMIN 4
#define W_NOSIZE 8

#define W_DESTROYED 16
#define W_SCROLLBARS 32
#define W_OVERSCROLL 64

class Window : public Object
{
public:
	Window(Object *,char *,char *aName=NULL);
	virtual ~Window();
// xpos and ypos are the coords of the client area origin in virtual coords
// and xMax,yMax are the virtual client area extents
	static Window* CreationWindow;
	long OldStyle;
	LPSTR Menu;
	int	x,y,w,h,Flags,Icon;
	DWORD Style;
	char *Title,*Name;
	virtual BOOL SetupWindow();
	void AssignMenu(LPSTR);
	void AssignMenu(int);
	void EnableMenuItem(int id,BOOL state);
	void CheckMenuItem(int id,BOOL state);
	void ShowTitleBar();
	void HideTitleBar();

	void SetVirtualExtent(long,long,int,int,int,int);
	BOOL HScroll(TMSG &);
	BOOL VScroll(TMSG &);
	BOOL WMSize(TMSG &);
	BOOL WMGetMinMaxInfo(TMSG &);

	virtual void Paint(HDC, BOOL, RECT &);
	BOOL Create();
	virtual void Destroy();
	virtual void DestroyWindow();
	virtual char *GetClassName();
	BOOL Register();
	virtual BOOL WndProc(TMSG &);
	virtual void OpenFile(char*);
	virtual void OpenFile2(char*);
	virtual void InitMenu(HMENU);
	void SetBkColor(COLORREF);
	void SetIcon(int);
	void GetWindowState(int aSaveFlags=W_SAVESTATE);
	StatusBar *StatBar;

	long xPos,yPos,xMax,yMax;
	int xStep,yStep,xPageSize,yPageSize;
	void SaveWindowState();
};

inline Window *GetPointer( HWND hWnd )
{
	return (Window *) GetWindowLong( hWnd, 0 );
}

inline void SetPointer( HWND hWnd, Window *pWindow )
{
	SetWindowLong( hWnd, 0, (LONG) pWindow );
}

