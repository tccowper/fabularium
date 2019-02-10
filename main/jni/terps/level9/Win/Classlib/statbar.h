// statbar.h

#include <listc.h>

class StatusBox : public BaseNode<StatusBox>
{
public:
	StatusBox(char *T,int X,int W);
	~StatusBox();
	void SetText(char *T);

	int x,w;
	char *Text;
};

class StatusBar :  Object
{
public:
	StatusBar(Object *P,int w,BOOL NumBox,BOOL CapsBox);
	~StatusBar();
	BOOL SetupWindow();
	void SetText(char *);
	void SetText(int,char *);
	void Paint(HDC dc);
	void Invalidate();
	void InsertBox(int After,char *T,int w);

	BOOL WMKeyUp(TMSG &);
	BOOL WMKeyDown(TMSG &);
	BOOL WMFocus(TMSG &);
	BOOL WMSize(TMSG &);
	BOOL WMGetMinMaxInfo(TMSG &);
	BOOL WMMenuSelect(TMSG &Msg);
	int dyStatbar;
private:
	StatusBox *AddBox(char *T,int w);
	void DrawText(StatusBox *S);
	char *OldText;
	StatusBox *TextBox,*NumLock,*CapsLock;
	List<StatusBox> BoxList;
	int dyBorder,dyBorderx2,dyBorderx3,dyBorderx8,dyBorderx9;
	int Pos,CharWidth;
	int nMenu;
	HMENU hMenu[10];
	HFONT Font;
	static BOOL bInKeyDown;
	RECT StatRecSizeAdv;

	void SetNumLockText();
	void SetCapsLockText();
	void ParsePopup(HMENU hm);

	EV_ENABLE(StatusBar)
};

