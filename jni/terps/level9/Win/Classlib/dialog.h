// Dialog.h

class Dialog : public Object {
	DLGPROC lpProcDialog;
	int ID;
public:
	BOOL IsModal;
	char *Section;
	Dialog(Object *,int,char *Sect=NULL);
	virtual ~Dialog();
	int Execute();
	int ExecuteWithFont();
	virtual BOOL SetupWindow();
	virtual void Paint(HDC, BOOL, RECT &);
	virtual void EndDialog(int Result);
	BOOL Create();
	void KillDialog(int);
	void SaveDialogPos();
	void GetDialogPos();
	void CentreDlgOnParent();

	BOOL ChildIDMessage(UINT id ,TMSG &);
	BOOL ChildMessage(HWND hw,TMSG &);
};
