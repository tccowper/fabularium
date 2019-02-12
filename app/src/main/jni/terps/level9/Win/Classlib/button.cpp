// button in window

#include <mywin.h>
#pragma hdrstop

Button::Button(Object *Parent,char *Title,int X, int Y, int W, int H,int ID)
		: Window(Parent,Title)
{
	x=X; y=Y; w=W; h=H;
	Style= WS_VISIBLE | WS_CHILD | BS_PUSHBUTTON;
	Create();
#ifndef WIN32
	SetWindowWord(hWnd,GWW_ID,ID);
#else
	SetWindowLong(hWnd,GWL_ID,ID);
#endif
}

char *Button::GetClassName()
{
	return "BUTTON";
}
