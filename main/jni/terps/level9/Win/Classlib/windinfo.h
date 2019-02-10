// window object list

class WindowInfo
{
private:
	WindowInfo *Next,*Prev;
	HWND hWnd;
	Object *Obj;
	static WindowInfo *WinObjList;
public:
	WindowInfo(Object *O,HWND hw);
	~WindowInfo();
	static Object *Find(HWND hw);
	static void Delete(HWND hw);
};
