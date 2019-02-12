// colorbtn.h

class ColorButton : Object {
private:
	COLORREF Color;
public:
	ColorButton(Object* , int, COLORREF);
	virtual BOOL WndProc(TMSG &);
	BOOL Create() { return TRUE; }
	COLORREF GetColor();
};
