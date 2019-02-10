// printer class

class Printer
{
public:
	Printer();
	~Printer();
	void Setup(Window *Obj);
	void Print(Window *Obj);
	void DeleteGlobals();
	void GetDefault();
	BOOL PrintDialog(Window *Obj,UINT Flags);

	PRINTDLG pd;
};
