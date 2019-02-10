// Object.h

typedef	BOOL (GENERIC::*Proc1)(TMSG&);
typedef	void (GENERIC::*NotifyProc)(UINT);

class Object {
	Object *ChildList,*Next,*Prev;
public :
	HWND hWnd,ParentHWnd;
	Object *Parent;
	Object(Object *);
	virtual ~Object();
	virtual BOOL EV_FIND(TMSG&);
	virtual BOOL EV_SEARCH(GEN_EV_INFO *,TMSG&,GENERIC *);
	virtual BOOL CanClose();
	Object *FirstChild() { return ChildList; }
	Object *NextChild(Object *Child) { return Child->Next; }
	virtual BOOL Create();
	virtual BOOL WndProc(TMSG &);
	virtual LRESULT DefProc(HWND hWnd,UINT Msg,WPARAM wParam,LPARAM lParam);
};

