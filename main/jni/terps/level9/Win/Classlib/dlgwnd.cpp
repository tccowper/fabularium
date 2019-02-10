// DlgWindow definitions *******************************
// Duplicates TWindow somewhat

#include <mywin.h>
#pragma hdrstop


DlgWindow::DlgWindow(Object *Parent, int Id) : Dialog(Parent,Id)
{
	Menu = NULL;
	OldStyle = 0;
}

void DlgWindow::AssignMenu(LPSTR menu)
{
	HMENU OldMenu,hM;
	if (HIWORD(Menu)) delete[] Menu;

	if (HIWORD(menu)) Menu = strdup(menu);
	else Menu = menu;

	if (hWnd) {
		OldMenu=GetMenu(hWnd);
		hM=menu ? LoadMenu(App::hInstance,menu) : (HMENU) NULL;
		SetMenu(hWnd,hM);
		if (OldMenu && !Menu) Resize( -GetSystemMetrics(SM_CYMENU) );
		if (!OldMenu && Menu) Resize( GetSystemMetrics(SM_CYMENU) );

		if (OldMenu) DestroyMenu(OldMenu);
		}
}

void DlgWindow::AssignMenu(int MenuId)
{
	AssignMenu((LPSTR)MAKEINTRESOURCE(MenuId));
}

BOOL DlgWindow::Create()
{
	BOOL ret=Dialog::Create();
	if (ret && Menu)
	{
		HMENU hM=LoadMenu(App::hInstance,Menu);
		SetMenu(hWnd,hM);
		// resize dialog
		Resize(GetSystemMetrics(SM_CYMENU));
	}
	return ret;
}

void DlgWindow::ShowTitleBar()
{
	Style=GetWindowLong(hWnd,GWL_STYLE); // need this ?
	Style|=WS_CAPTION | OldStyle;
	SetWindowLong(hWnd,GWL_STYLE,Style);
	// resize dialog
	Resize( GetSystemMetrics(SM_CYCAPTION) );
}

void DlgWindow::HideTitleBar()
{
	Style=GetWindowLong(hWnd,GWL_STYLE); // need this ?
	OldStyle=Style & ( WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU);
	Style&=~( (WS_CAPTION & ~WS_BORDER) | WS_MINIMIZEBOX | WS_MAXIMIZEBOX | WS_SYSMENU);
	SetWindowLong(hWnd,GWL_STYLE,Style);
	// resize dialog
	Resize( -GetSystemMetrics(SM_CYCAPTION) );
}

void DlgWindow::Resize(int h)
{
	RECT rc;
	GetWindowRect(hWnd,&rc);
	int height=rc.bottom-rc.top+h;
	SetWindowPos(hWnd,NULL, rc.left, rc.top-h, rc.right-rc.left, height , SWP_NOZORDER | SWP_DRAWFRAME);
	ShowWindow(hWnd,SW_SHOW);
}

