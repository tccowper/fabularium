#include "stdafx.h"
#include "ColourButton.h"

#include <shlwapi.h>

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

BEGIN_MESSAGE_MAP(ColourButton, CButton)
  ON_CONTROL_REFLECT(BN_CLICKED, OnClicked)
  ON_NOTIFY_REFLECT(NM_CUSTOMDRAW, OnCustomDraw)
END_MESSAGE_MAP()

ColourButton::ColourButton()
{
  m_colour = RGB(0,0,0);
  m_themes = ::LoadLibrary("UxTheme.dll");
}

ColourButton::~ColourButton()
{
  if (m_themes)
    ::FreeLibrary(m_themes);
}

BOOL ColourButton::SubclassDlgItem(UINT id, CWnd* parent)
{
  if (CButton::SubclassDlgItem(id,parent))
  {
    if (GetDllVersion("comctl32.dll") < MAKELONG(0,6))
      ModifyStyle(0,BS_OWNERDRAW,0);
    return TRUE;
  }
  return FALSE;
}

void ColourButton::OnClicked() 
{
  CColorDialog dialog(m_colour,CC_FULLOPEN,this);
  if (dialog.DoModal() == IDOK)
  {
    m_colour = dialog.GetColor();
    Invalidate();
  }
}

void ColourButton::OnCustomDraw(NMHDR* nmhdr, LRESULT* result)
{
  NMCUSTOMDRAW* nmcd = (NMCUSTOMDRAW*)nmhdr;
  *result = CDRF_DODEFAULT;

  switch (nmcd->dwDrawStage)
  {
  case CDDS_PREPAINT:
    {
      CDC* dc = CDC::FromHandle(nmcd->hdc);
      CRect r(nmcd->rc);

      bool selected = ((nmcd->uItemState & CDIS_SELECTED) != 0);
      bool disabled = ((nmcd->uItemState & CDIS_DISABLED) != 0);
      bool focus = ((nmcd->uItemState & CDIS_FOCUS) != 0);
      if (SendMessage(WM_QUERYUISTATE) & UISF_HIDEFOCUS)
        focus = false;

      if (IsAppThemed())
      {
        HTHEME theme = OpenThemeData(this,L"Button");
        if (theme)
        {
          // Get the area to draw into
          GetThemeBackgroundContentRect(theme,dc,BP_PUSHBUTTON,PBS_NORMAL,r);
          CloseThemeData(theme);
        }
      }
      else
      {
        UINT state = DFCS_BUTTONPUSH|DFCS_ADJUSTRECT;
        if (disabled)
          state |= DFCS_INACTIVE;
        if (selected)
          state |= DFCS_PUSHED;
        dc->DrawFrameControl(r,DFC_BUTTON,state);

        if (selected)
          r.OffsetRect(1,1);
      }

      DrawControl(dc,r,disabled,focus);
      *result = CDRF_SKIPDEFAULT;
    }
    break;
  }
}

void ColourButton::DrawItem(LPDRAWITEMSTRUCT dis)
{
  bool selected = ((dis->itemState & ODS_SELECTED) != 0);
  bool disabled = ((dis->itemState & ODS_DISABLED) != 0);
  bool focus = ((dis->itemState & ODS_FOCUS) != 0) && ((dis->itemState & ODS_NOFOCUSRECT) == 0);
  CDC* dc = CDC::FromHandle(dis->hDC);
  CRect r(dis->rcItem);

  UINT state = DFCS_BUTTONPUSH|DFCS_ADJUSTRECT;
  if (disabled)
    state |= DFCS_INACTIVE;
  if (selected)
    state |= DFCS_PUSHED;
  dc->DrawFrameControl(r,DFC_BUTTON,state);

  if (selected)
    r.OffsetRect(1,1);
  DrawControl(dc,r,disabled,focus);
}

void ColourButton::DrawControl(CDC* dc, CRect r, bool disabled, bool focus)
{
  r.DeflateRect(2,2);

  // Draw the colour that the button represents
  CPen pen;
  pen.CreatePen(PS_SOLID,1,::GetSysColor(disabled ? COLOR_GRAYTEXT : COLOR_BTNTEXT));
  CBrush brush;
  brush.CreateSolidBrush(m_colour);
  CPen* oldPen = dc->SelectObject(&pen);
  CBrush* oldBrush = dc->SelectObject(&brush);
  dc->Rectangle(r);
  dc->SelectObject(oldPen);
  dc->SelectObject(oldBrush);

  // Draw the focus rectangle
  if (focus)
  {
    r.InflateRect(1,1);
    dc->DrawFocusRect(r);
  }
}

bool ColourButton::IsAppThemed(void)
{
  if (m_themes)
  {
    typedef BOOL(__stdcall *ISAPPTHEMED)();

    ISAPPTHEMED isAppThemed =
      (ISAPPTHEMED)::GetProcAddress(m_themes,"IsAppThemed");
    if (isAppThemed)
      return (*isAppThemed)() != FALSE;
  }
  return false;
}

HTHEME ColourButton::OpenThemeData(CWnd* wnd, LPCWSTR classList)
{
  if (m_themes)
  {
    typedef HTHEME(__stdcall *OPENTHEMEDATA)(HWND, LPCWSTR);

    OPENTHEMEDATA openThemeData =
      (OPENTHEMEDATA)::GetProcAddress(m_themes,"OpenThemeData");
    if (openThemeData)
      return (*openThemeData)(wnd->GetSafeHwnd(),classList);
  }
  return 0;
}

void ColourButton::CloseThemeData(HTHEME theme)
{
  if (m_themes)
  {
    typedef HRESULT(__stdcall *CLOSETHEMEDATA)(HTHEME);

    CLOSETHEMEDATA closeThemeData =
      (CLOSETHEMEDATA)::GetProcAddress(m_themes,"CloseThemeData");
    if (closeThemeData)
      (*closeThemeData)(theme);
  }
}

void ColourButton::GetThemeBackgroundContentRect(
  HTHEME theme, CDC* dc, int partId, int stateId, RECT* rect)
{
  if (m_themes)
  {
    typedef HRESULT(__stdcall *GETTHEMEBACKCONTENTRECT)(HTHEME, HDC, int, int,
      const RECT *, RECT *);

    GETTHEMEBACKCONTENTRECT getThemeBackRect =
      (GETTHEMEBACKCONTENTRECT)::GetProcAddress(m_themes,"GetThemeBackgroundContentRect");
    if (getThemeBackRect)
    {
      CRect result;
      (*getThemeBackRect)(theme,dc->GetSafeHdc(),partId,stateId,rect,&result);
      *rect = result;
    }
  }
}

DWORD ColourButton::GetDllVersion(const char* dllName)
{
  DWORD version = 0;

  HINSTANCE dll = ::LoadLibrary(dllName);
  if (dll != 0)
  {
    DLLGETVERSIONPROC dllGetVersion = (DLLGETVERSIONPROC)::GetProcAddress(dll,"DllGetVersion");
    if (dllGetVersion != NULL)
    {
      DLLVERSIONINFO dvi;
      ::ZeroMemory(&dvi,sizeof dvi);
      dvi.cbSize = sizeof dvi;

      if (SUCCEEDED((*dllGetVersion)(&dvi)))
        version = MAKELONG(dvi.dwMinorVersion,dvi.dwMajorVersion);
    }
    ::FreeLibrary(dll);
  }
  return version;
}
