// textwnd.h

#define TW_STAYOPEN 1

class TextWindow : public Window
{
public:
	TextWindow(Object *Parent,char *title,BOOL Stay=FALSE);
	~TextWindow();
	virtual char *GetClassName();
	void AddString(char *S);
	void Clear();
	BOOL StayOpen;
protected:
	int Lines,MaxStr,FontHeight,FontWidth;
	HFONT Font;

	struct TWString
	{
		char *String;
		TWString *Next,*Prev;
	} *First,*Last;

	void ClearMem();
	void Paint(HDC, BOOL, RECT&);
	BOOL WMClose(TMSG&);
	BOOL WMCreate(TMSG&);

EV_ENABLE(TextWindow)
};

void TextWindowInit(Object *Parent,char *Title,BOOL Stay=TRUE);
extern "C"
{
	int printf(const char *fmt,...);
}


