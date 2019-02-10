// button.h

class Button : public Window {
public:
	Button(Object *Parent,char *Title,int X, int Y, int W, int H,int ID);
	virtual char *GetClassName();
};
