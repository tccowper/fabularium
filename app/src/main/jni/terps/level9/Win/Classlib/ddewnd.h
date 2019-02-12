// ddehnd.h

class DDEWindow : public Window
{
public:
	DDEWindow(Object *Parent,char *Title) : Window(Parent,Title){}
	BOOL DDEInitialise(char *App,char *Topic);
	BOOL DDDRequest(char **d,char *Item);
	BOOL DDESend(char *c);
	void DDETerminate();
private:
	HWND ServerWindow;
	UINT SentMessage;
	char **DataPtr;
	BOOL AckFlag;

	void Wait();
	BOOL WMDdeTerminate(TMSG &);
	BOOL WMDdeAck(TMSG &);
	BOOL WMDDEData(TMSG &);
	virtual void Destroy();

EV_ENABLE(DDEWindow)
};