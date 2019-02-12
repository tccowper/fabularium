// Colour button ******************************

#include <mywin.h>
#pragma hdrstop

ColorButton::ColorButton(Object* Parent, int resId,COLORREF Col) : Object(Parent) {
hWnd=GetDlgItem(ParentHWnd,resId);
Color=Col;
}

BOOL ColorButton::WndProc(TMSG &Msg)
{

	switch (Msg.Msg)
	{
		case WM_COMMAND:
			if (::GetColor(ParentHWnd,Color)) InvalidateRect(hWnd,NULL,TRUE);
			break;

		case WM_DRAWITEM:
		{
			LPDRAWITEMSTRUCT drawInfo = (LPDRAWITEMSTRUCT) Msg.lParam; /* item-drawing information */
			switch (drawInfo->itemAction)
			{
				case ODA_DRAWENTIRE:
				{
					HPEN DefPen=(HPEN) SelectObject(drawInfo->hDC,GetStockObject(BLACK_PEN));
					HBRUSH Brush=(HBRUSH)CreateSolidBrush(Color);
					HBRUSH DefBrush=(HBRUSH) SelectObject(drawInfo->hDC,Brush);
					Rectangle(drawInfo->hDC,drawInfo->rcItem.left,drawInfo->rcItem.top,drawInfo->rcItem.right,drawInfo->rcItem.bottom);
					SelectObject(drawInfo->hDC,DefBrush);
					SelectObject(drawInfo->hDC,DefPen);
					DeleteObject(Brush);
					return TRUE;
				}
				case ODA_SELECT:
					return TRUE;

				case ODA_FOCUS:
					return TRUE;
			}
		}
		break;

	}
	return FALSE;
}

COLORREF ColorButton::GetColor()
{
	return Color;
}

