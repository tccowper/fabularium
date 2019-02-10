// edit.h

class Edit : public Object {

public:
	Edit(Object *,int,char *s=NULL,int Len=0);
	virtual ~Edit();
	char *GetString(char *s=NULL,int BufSize=0);
	void SetString(char *);
	void AddString(char *);
	int GetLength();
	LRESULT DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam);
private:
	int ID;
	BOOL Create();
	void SubClass();
	WNDPROC OldProc;
};

