// keypress.h

class KeyPressDialog : public Window {
public:
//	long Scan;
	int *Key;
	int Id,Jid;
	KeyPressDialog(Object *Parent,int ID,int *Key,int JID=-1);
	BOOL SetupWindow();
	void Destroy();

	BOOL WMSysKeyDown(TMSG&);
	BOOL WMKeyDown(TMSG&);
	BOOL WMKillFocus(TMSG&);
	BOOL WMLButtonDown(TMSG&);
	BOOL WMMButtonDown(TMSG&);
	BOOL WMRButtonDown(TMSG&);
	BOOL WMTimer(TMSG&);

EV_ENABLE(KeyPressDialog)
} ;

void SetKeyText(HWND hWnd,int ID,int Key);
