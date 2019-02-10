// owndrwb.h

class OwnDrwButton : Object {
public:
	HBITMAP bm_draw,bm_select,bm_focus,bm_disabled;
	int Width,Height;
	OwnDrwButton(Object* parent, int resId, int dr, int s, int f, int dis);
	virtual ~OwnDrwButton();
	virtual BOOL WndProc(TMSG &);
	BOOL Create() { return TRUE; }
};
