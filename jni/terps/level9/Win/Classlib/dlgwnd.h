// dlgwnd.h

// cannot devive from Window and Dialog as conficts arise
class DlgWindow : public Dialog {
	long OldStyle;
	void Resize(int);
public:
	DlgWindow(Object *, int);
	LPSTR Menu;
	int	x,y,w,h;
	DWORD Style;
	void AssignMenu(LPSTR m);
	void AssignMenu(int m);
	BOOL Create();
	void ShowTitleBar();
	void HideTitleBar();
};
