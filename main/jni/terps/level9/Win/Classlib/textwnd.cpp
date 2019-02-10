// simple Text window

#include <mywin.h>
#pragma hdrstop

#include <stdarg.h>
#include <fname.h>

#ifdef WIN32
#define MaxLines 2000
#else
#define MaxLines 1000
#endif

EV_START(TextWindow)
	EV_MESSAGE(WM_CLOSE,WMClose)
	EV_MESSAGE(WM_CREATE,WMCreate)
EV_END

TextWindow::TextWindow(Object *Parent,char *title,BOOL Stay) : Window(Parent,title)
{
	App::TextWindow=this;
	App::Active=TRUE; // hack
	Style=WS_OVERLAPPEDWINDOW;
	StayOpen=Stay;
	First=NULL;
	Last=NULL;
	Lines=0;
	MaxStr=0;
	Flags|=W_OVERSCROLL;
}

char *TextWindow::GetClassName()
{
	return "TextWindow";
}

TextWindow::~TextWindow()
{
	App::TextWindow=NULL;
	DeleteObject(Font);
	// clean up mem
	ClearMem();
}

void TextWindow::ClearMem()
{
	TWString *temp=First,*temp2;
	while (temp)
	{
		delete[] temp->String;
		temp2=temp->Next;
		delete temp;
		temp=temp2;
	}
}

void TextWindow::Clear()
{
	ClearMem();
	First=NULL;
	Last=NULL;
	Lines=0;
	MaxStr=0;
	InvalidateRect(hWnd,NULL,TRUE);
}

BOOL TextWindow::WMCreate(TMSG&)
{
	ShowWindow(hWnd, SW_HIDE );

	TEXTMETRIC tm;
	HDC dc=GetDC(hWnd);
	
	LOGFONT lf;
	memset(&lf,0,sizeof(lf));
	lf.lfHeight=-12;
	lf.lfWeight=FW_NORMAL;
	lf.lfCharSet=ANSI_CHARSET;
	lf.lfOutPrecision=OUT_TT_PRECIS;
	lf.lfClipPrecision=CLIP_DEFAULT_PRECIS;
	lf.lfQuality=PROOF_QUALITY;
	lf.lfPitchAndFamily=4; //FIXED_PITCH | 4 | FF_MODERN;
	strcpy(lf.lfFaceName,"Courier New");
	Font=CreateFontIndirect(&lf);
	HFONT OldFont=(HFONT) SelectObject(dc,Font);
	GetTextMetrics(dc, &tm);
	SelectObject(dc,OldFont);
	ReleaseDC(hWnd, dc);

	FontHeight = tm.tmHeight + tm.tmExternalLeading;
	FontWidth = tm.tmAveCharWidth;
	return FALSE;
}

BOOL TextWindow::WMClose(TMSG&)
{
	if (Parent)
	{
		ShowWindow(hWnd, SW_HIDE );
		SetFocus(ParentHWnd);
	}
	else DestroyWindow();
	return TRUE;
}

void TextWindow::AddString(char *S)
{
	TWString *temp;
	if (Lines==MaxLines)
	{
		// delete last
		temp=Last->Prev;
		delete[] Last->String;
		delete Last;
		Last=temp;
		Last->Next=NULL;
	}
	else Lines++;

	temp=new TWString;
	temp->String=strdup(S);
	temp->Next=First;
	if (First) First->Prev=temp;
	else Last=temp;
	First=temp;
	temp->Prev=NULL;
	if ((int)strlen(S)>MaxStr) MaxStr=strlen(S);

	SetVirtualExtent(MaxStr*FontWidth,Lines*FontHeight,FontWidth,FontHeight,0,0);
	ShowWindow(hWnd, SW_SHOW );

	RECT rc;
	GetClientRect(hWnd,&rc);
	if (Lines*FontHeight<rc.bottom)
	{
		rc.top=(Lines-1)*FontHeight;
		rc.bottom=Lines*FontHeight;
		InvalidateRect(hWnd,&rc,TRUE);
	}
	else SendMessage(hWnd,WM_VSCROLL,SB_BOTTOM,0);	// sroll to bottom


//	InvalidateRect(hWnd,NULL,TRUE);  // improve??
	App::PeekLoop();
}

void TextWindow::Paint(HDC dc, BOOL, RECT&)
{
	HFONT OldFont=(HFONT) SelectObject(dc,Font);
	COLORREF OldBk=::SetBkColor(dc,GetSysColor(COLOR_WINDOW));
		
	int y=0;
	TWString *temp=Last;
	while (temp)
	{
		TextOut(dc,0,y,temp->String,strlen(temp->String));
		y+=FontHeight;
		temp=temp->Prev;
	}
	SelectObject(dc,OldFont);
	::SetBkColor(dc,OldBk);
}

void TextWindowInit(Object *Parent,char *Title,BOOL Stay)
{
	if (App::TextWindow==NULL) (new TextWindow(Parent,Title,Stay))->Create();
}

int printf(const char *fmt,...)
{
	char temp[256];
	va_list ap;
	va_start(ap,fmt);
	vsprintf(temp,fmt,ap);
	va_end(ap);
	if (App::TextWindow==NULL)
	{
		FName F;
		GetModuleFileName(App::hInstance,F,MAX_PATH);
		TextWindowInit(0,F.GetName(),TRUE);
	}
	App::TextWindow->AddString(temp);
	// if message loop??
	return 0;
}
