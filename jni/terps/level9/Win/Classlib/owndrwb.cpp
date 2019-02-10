// Owner draw button ******************************

#include <mywin.h>
#pragma hdrstop

OwnDrwButton::OwnDrwButton(Object* Parent, int resId,
			 int drw, int sel, int foc, int dis) : Object(Parent) {
bm_draw=LoadBitmap(App::hInstance,MAKEINTRESOURCE(drw));
bm_select=LoadBitmap(App::hInstance,MAKEINTRESOURCE(sel));
bm_focus=LoadBitmap(App::hInstance,MAKEINTRESOURCE(foc));
bm_disabled=LoadBitmap(App::hInstance,MAKEINTRESOURCE(dis));
// get size of button?
hWnd=GetDlgItem(ParentHWnd,resId);
RECT rc;
GetWindowRect(hWnd,&rc);
Width=rc.right-rc.left;
Height=rc.bottom-rc.top;
}

OwnDrwButton::~OwnDrwButton() {
DeleteObject(bm_draw);
DeleteObject(bm_select);
DeleteObject(bm_focus);
DeleteObject(bm_disabled);
}

BOOL OwnDrwButton::WndProc(TMSG &Msg)
{
	switch (Msg.Msg)
	{
	#ifndef WIN32
		case WM_CTLCOLOR:
			switch ( (int) HIWORD(Msg.lParam)) // type of control
			{
				case CTLCOLOR_DLG:
				case CTLCOLOR_EDIT:
				case CTLCOLOR_LISTBOX:
					break;
				case CTLCOLOR_MSGBOX:  // this one works ?
					// Prevents white out with CTL3D
					Msg.RetVal=GetStockObject(NULL_BRUSH);
					return TRUE;
				case CTLCOLOR_SCROLLBAR:
				case CTLCOLOR_STATIC:
				case CTLCOLOR_BTN:
					break;
			}
			break;
	#endif
		case WM_DRAWITEM:
		{
			LPDRAWITEMSTRUCT drawInfo = (LPDRAWITEMSTRUCT) Msg.lParam; /* item-drawing information */
			switch (drawInfo->itemAction)
			{
				case ODA_DRAWENTIRE:
					if (drawInfo->itemState & ODS_DISABLED) DrawBitmap(drawInfo->hDC,bm_disabled);
					else if (drawInfo->itemState & ODS_SELECTED) DrawBitmap(drawInfo->hDC,bm_select);
					else  if (drawInfo->itemState & ODS_FOCUS)  DrawBitmap(drawInfo->hDC,bm_focus);
					else DrawBitmap(drawInfo->hDC,bm_draw);
					return TRUE;

				case ODA_SELECT:
					if (drawInfo->itemState & ODS_SELECTED) DrawBitmap(drawInfo->hDC,bm_select);
					else  if (drawInfo->itemState & ODS_FOCUS)  DrawBitmap(drawInfo->hDC,bm_focus);
					else DrawBitmap(drawInfo->hDC,bm_draw);
					return TRUE;

				case ODA_FOCUS:
					if (drawInfo->itemState & ODS_FOCUS) DrawBitmap(drawInfo->hDC,bm_focus);
						else DrawBitmap(drawInfo->hDC,bm_draw);
					return TRUE;
			}
			break;
		}
	}
	return FALSE;
}

