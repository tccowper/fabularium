// filedlg.h

#define OFN_EXPLORER                 0x00080000     // new look commdlg
#define OFN_LONGNAMES                0x00200000     // force long names for 3.x modules

class CustFileDlg : public Object
{
public:
	CustFileDlg(Object *Parent ,int Id, char *Buf,int BufS,const char *aTitle,const char *Filt,int *FiltInd,long flgs=OFN_EXPLORER);
	~CustFileDlg();
	BOOL WndProc(TMSG &);
	int Execute(BOOL hook = TRUE);
	virtual void FileSelected(char*);
private:
	UINT FokMsg;
	BOOL TrapOK;
	int ID;
	long Flags;
	char *Buffer;
	int BufSize;
	const char *Title,*Filters;
	int *FiltIndex;
	virtual BOOL SetupWindow();
	virtual void EndDialog(int);
	virtual void Paint(HDC, BOOL, RECT &);
	BOOL Create();

	OPENFILENAME *O;
	static CustFileDlg *OpenDialog;
	static UINT PASCAL MyOpenDlgProc(HWND hDlg,UINT Msg,WPARAM wParam,LPARAM lParam);
};
