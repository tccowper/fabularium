#include "stdafx.h"
#include "MenuBar.h"

#include <shlwapi.h>

#ifdef _DEBUG
#define new DEBUG_NEW
#endif

#define DLLVERSION(major,minor) MAKELONG(minor,major)

namespace {
MenuBar* menuBar = NULL;
HHOOK msgHook = NULL;
}

IMPLEMENT_DYNAMIC(MenuBar, CToolBar)

BEGIN_MESSAGE_MAP(MenuBar, CToolBar)
  ON_WM_MOUSEMOVE()
  ON_WM_SETTINGCHANGE()
  ON_NOTIFY_REFLECT(NM_CUSTOMDRAW, OnCustomDraw)
  ON_NOTIFY_REFLECT(TBN_DROPDOWN, OnDropDown)
  ON_NOTIFY_REFLECT(TBN_HOTITEMCHANGE, OnHotItemChange)
  ON_UPDATE_COMMAND_UI_RANGE(0, 256, OnUpdateButton)
  ON_MESSAGE(WM_MENUBAR_POPUP, OnMenuPopup)
END_MESSAGE_MAP()

MenuBar::MenuBar()
{
  m_tracking = TRACK_NONE;
  m_popupTrack = -1;
  m_popupNew = -1;
  m_popupMenu = 0;
  m_arrowLeft = false;
  m_arrowRight = false;
  m_escapePressed = false;

  m_useF10 = true;
  m_useAltX = true;

  m_osVer.dwOSVersionInfoSize = sizeof m_osVer;
  ::GetVersionEx(&m_osVer);

  HMODULE user32 = ::LoadLibrary("user32.dll");
  m_getMenuInfo = (GETMENUINFO)::GetProcAddress(user32,"GetMenuInfo");
  m_setMenuInfo = (SETMENUINFO)::GetProcAddress(user32,"SetMenuInfo");
  m_useBitmaps = ((m_getMenuInfo != NULL) && (m_setMenuInfo != NULL));
}

MenuBar::~MenuBar()
{
  // Delete the contents of the bitmap mapping
  UINT id;
  Bitmap bitmap;
  POSITION pos = m_bitmaps.GetStartPosition();
  while (pos != NULL)
  {
    m_bitmaps.GetNextAssoc(pos,id,bitmap);
    ::DeleteObject(bitmap.bitmap);
    delete[] bitmap.initialBits;
  }
}

void MenuBar::SetUseF10(bool use)
{
  m_useF10 = use;
}

void MenuBar::SetUseAltX(bool use)
{
  m_useAltX = use;
}

BOOL MenuBar::Create(UINT id, CMenu* menu, CWnd* parent)
{
  // We need at least comctrl32 5.80. If not return true to indicate that the
  // program can carry on: it will simply get the original Windows menu instead.
  if (GetDllVersion("comctl32.dll") < DLLVERSION(5,80))
    return TRUE;

  // Create the menu toolbar
  if (CreateEx(parent,TBSTYLE_FLAT|TBSTYLE_LIST|TBSTYLE_TRANSPARENT,
    WS_CHILD|WS_VISIBLE|CBRS_ALIGN_TOP|CBRS_TOOLTIPS|CBRS_FLYBY) == FALSE)
  {
    return FALSE;
  }
  GetToolBarCtrl().SetBitmapSize(CSize(0,0));
  UpdateFont();

  // For Windows XP and earlier, disable theming so that the menu text colour can be set
  if (m_osVer.dwMajorVersion < 6)
  {
    HMODULE themes = ::LoadLibrary("uxtheme.dll");
    if (themes != 0)
    {
      SETWINDOWTHEME setWindowTheme = (SETWINDOWTHEME)::GetProcAddress(themes,"SetWindowTheme");
      if (setWindowTheme != NULL)
        (*setWindowTheme)(GetSafeHwnd(),L"",L"");
      ::FreeLibrary(themes);
    }
  }

  // Load the menus
  if (id == -1)
    m_menu.Attach(menu->Detach());
  else
    m_menu.LoadMenu(id);
  Update();

  // Remove the existing menu from the frame window
  ::SetMenu(GetParentFrame()->GetSafeHwnd(),0);
  return TRUE;
}

void MenuBar::LoadBitmaps(CBitmap& bitmap, CToolBarCtrl& bar, CSize size, bool alpha)
{
  if (!m_useBitmaps)
    return;

  // Create device contexts compatible with the display
  CDC dcFrom, dcTo;
  dcFrom.CreateCompatibleDC(NULL);
  dcTo.CreateCompatibleDC(NULL);

  // Load the toolbar bitmap into a device context
  CBitmap* oldFromBitmap = dcFrom.SelectObject(&bitmap);

  // Iterate over the toolbar buttons, extracting the bitmap image for each
  for (int i = 0; i < bar.GetButtonCount(); i++)
  {
    TBBUTTON button;
    bar.GetButton(i,&button);

    // Check that the button has a bitmap and is not a separator
    if ((button.iBitmap >= 0) && ((button.fsStyle & TBSTYLE_SEP) == 0))
    {
      // Create a new button bitmap
      BITMAPINFO bi;
      ::ZeroMemory(&bi,sizeof bi);
      bi.bmiHeader.biSize = sizeof (BITMAPINFOHEADER);
      bi.bmiHeader.biPlanes = 1;
      bi.bmiHeader.biBitCount = 32;
      bi.bmiHeader.biCompression = BI_RGB;
      bi.bmiHeader.biWidth = size.cx;
      bi.bmiHeader.biHeight = size.cy;
      DWORD* bits;
      CBitmap buttonBitmap;
      buttonBitmap.Attach(
        ::CreateDIBSection(dcFrom,&bi,DIB_RGB_COLORS,(VOID**)&bits,NULL,0));

      // Copy the image into the button bitmap
      CBitmap* oldToBitmap = dcTo.SelectObject(&buttonBitmap);
      dcTo.BitBlt(0,0,size.cx,size.cy,&dcFrom,size.cx*button.iBitmap,0,SRCCOPY);
      dcTo.SelectObject(oldToBitmap);

      // Set the button bitmap to use pre-computed alpha: that is,
      // the RGB components are already scaled by the alpha channel.
      ::GdiFlush();
      DWORD clear = bits[0];
      for (int y = 0; y < size.cy; y++)
      {
        for (int x = 0; x < size.cx; x++)
        {
          int i = x+(y*size.cx);
          if (alpha)
          {
            DWORD alpha = (bits[i]&0xff000000)>>24;
            if (alpha == 0)
              bits[i] = 0;
            else if (alpha != 0xff)
            {
              DWORD r = (bits[i]&0x00ff0000)>>16;
              DWORD g = (bits[i]&0x0000ff00)>>8;
              DWORD b = bits[i]&0x000000ff;
              r = (r*alpha)>>8; g = (g*alpha)>>8; b = (b*alpha)>>8;
              bits[i] = (alpha<<24)|(r<<16)|(g<<8)|b;
            }
          }
          else
          {
            if (bits[i] == clear)
              bits[i] = 0;
            else
              bits[i] = (0xff<<24)|(bits[i]&0x00ffffff);
          }
        }
      }

      // Make a copy of the bitmap bits
      DWORD* bitsCopy = new DWORD[size.cx*size.cy];
      memcpy(bitsCopy,bits,size.cx*size.cy*sizeof(DWORD));

      Bitmap lookup;
      if (m_bitmaps.Lookup(button.idCommand,lookup) == FALSE)
      {
        Bitmap info((HBITMAP)buttonBitmap.Detach(),size,bits,bitsCopy);
        m_bitmaps.SetAt(button.idCommand,info);
      }
    }
  }
  dcFrom.SelectObject(oldFromBitmap);
}

void MenuBar::Update(void)
{
  SetBitmaps(&m_menu);

  // Add menu headings
  if (m_menu.GetMenuItemCount() > 0)
  {
    SetButtons(0,m_menu.GetMenuItemCount());
    for (UINT i = 0; i < m_menu.GetMenuItemCount(); i++)
    {
      CString title;
      m_menu.GetMenuString(i,title,MF_BYPOSITION);
      SetButtonInfo(i,i,BTNS_BUTTON|BTNS_AUTOSIZE|BTNS_DROPDOWN,0);
      SetButtonText(i,title);
    }
  }
}

CMenu* MenuBar::GetMenu(void) const
{
  return const_cast<CMenu*>(&m_menu);
}

BOOL MenuBar::TranslateFrameMessage(MSG* msg)
{
  // Stop tracking if the user clicked outside of the menu bar
  if ((msg->message >= WM_LBUTTONDOWN) && (msg->message <= WM_MOUSELAST))
  {
    if ((msg->hwnd != GetSafeHwnd()) && (m_tracking != TRACK_NONE))
      SetTrackingState(TRACK_NONE);
    return FALSE;
  }

  bool alt = ((HIWORD(msg->lParam) & KF_ALTDOWN) != 0);
  bool shift = ((::GetKeyState(VK_SHIFT) & 0x80000000) != 0);
  bool ctrl = ((::GetKeyState(VK_CONTROL) & 0x80000000) != 0);

  switch (msg->message)
  {
  case WM_SYSKEYUP:

    // Check for menu key (Alt) or F10 without Alt/Ctrl/Shift
    if ((msg->wParam == VK_MENU) || (m_useF10 && (msg->wParam == VK_F10) && !(shift||ctrl||alt)))
    {
      switch (m_tracking)
      {
      case TRACK_NONE:
        SetTrackingState(TRACK_BUTTON,0);
        break;
      case TRACK_BUTTON:
        SetTrackingState(TRACK_NONE);
        break;
      }
      return TRUE;
    }
    break;

  case WM_KEYDOWN:
  case WM_SYSKEYDOWN:

    // Check for menu key (Alt) or F10 without Alt/Ctrl/Shift
    if ((msg->wParam == VK_MENU) || (m_useF10 && (msg->wParam == VK_F10) && !(shift||ctrl||alt)))
      return FALSE;

    if (m_tracking == TRACK_BUTTON)
    {
      switch (msg->wParam)
      {
      case VK_LEFT:
        GetToolBarCtrl().SetHotItem(GetNextButton(GetToolBarCtrl().GetHotItem(),true));
        return TRUE;
      case VK_RIGHT:
        GetToolBarCtrl().SetHotItem(GetNextButton(GetToolBarCtrl().GetHotItem(),false));
        return TRUE;
      case VK_UP:
      case VK_DOWN:
      case VK_RETURN:
        PostMessage(WM_MENUBAR_POPUP,GetToolBarCtrl().GetHotItem(),TRUE);
        return TRUE;
      case VK_ESCAPE:
        SetTrackingState(TRACK_NONE);
        return TRUE;
      case VK_SPACE:
        SetTrackingState(TRACK_NONE);
        GetParentFrame()->PostMessage(WM_SYSCOMMAND,SC_KEYMENU,' ');
        return TRUE;
      }
    }

    if ((alt || (m_tracking == TRACK_BUTTON)) && ((msg->wParam >= '0') && (msg->wParam <= 'Z')))
    {
      // Alt-X, or else X while tracking menu buttons
      UINT id;
      if (GetToolBarCtrl().MapAccelerator((TCHAR)msg->wParam,&id))
      {
        if (!alt || m_useAltX)
        {
          PostMessage(WM_MENUBAR_POPUP,id,TRUE);
          return TRUE;
        }
      }
      else if ((m_tracking == TRACK_BUTTON) && !alt)
        return TRUE;
    }

    if (m_tracking != TRACK_NONE)
      SetTrackingState(TRACK_NONE);
    break;
  }
  return FALSE;
}

void MenuBar::OnMouseMove(UINT nFlags, CPoint pt)
{
  // Let the mouse change the active button in button tracking mode
  if (m_tracking == TRACK_BUTTON)
  {
    int hot = HitTest(pt);
    if ((hot >= 0) && (pt != m_mouse))
      GetToolBarCtrl().SetHotItem(hot);
    return;
  }
  m_mouse = pt;
  CToolBar::OnMouseMove(nFlags,pt);
}

void MenuBar::OnSettingChange(UINT, LPCTSTR)
{
  UpdateFont();
}

void MenuBar::OnCustomDraw(NMHDR* nmhdr, LRESULT* result)
{
  NMTBCUSTOMDRAW* nmtbcd = (NMTBCUSTOMDRAW*)nmhdr;
  *result = CDRF_DODEFAULT;

  switch (nmtbcd->nmcd.dwDrawStage)
  {
  case CDDS_PREPAINT:
    *result = CDRF_NOTIFYITEMDRAW;
    break;
  case CDDS_ITEMPREPAINT:
    {
      BOOL flat;
      if (::SystemParametersInfo(SPI_GETFLATMENU,0,&flat,0) == 0)
        flat = FALSE;

      if (flat)
      {
        *result = TBCDRF_NOEDGES|TBCDRF_NOOFFSET|TBCDRF_HILITEHOTTRACK;
        nmtbcd->clrHighlightHotTrack = ::GetSysColor(COLOR_MENUHILIGHT);
        if ((nmtbcd->nmcd.uItemState & CDIS_HOT) != 0)
          nmtbcd->clrText = ::GetSysColor(COLOR_HIGHLIGHTTEXT);
      }
    }
    break;
  }
}

void MenuBar::OnDropDown(NMHDR* nmhdr, LRESULT* result)
{
  NMTOOLBAR* nmtb = (NMTOOLBAR*)nmhdr;
  *result = TBDDRET_DEFAULT;

  PostMessage(WM_MENUBAR_POPUP,nmtb->iItem,FALSE);
}

void MenuBar::OnHotItemChange(NMHDR* nmhdr, LRESULT* result)
{
  NMTBHOTITEM* nmtbhi = (NMTBHOTITEM*)nmhdr;
  *result = 0;

  if (m_tracking != TRACK_NONE)
  {
    // When tracking, don't allow mouse or keyboard events to set the hot button to -1
    if (nmtbhi->dwFlags & (HICF_ACCELERATOR|HICF_ARROWKEYS|HICF_DUPACCEL|HICF_MOUSE))
    {
      int button = (nmtbhi->dwFlags & HICF_LEAVING) ? -1 : nmtbhi->idNew;
      if (button == -1)
        *result = 1;
    }
  }
}

void MenuBar::OnUpdateButton(CCmdUI* pCmdUI)
{
  // Always enable the menu buttons
  pCmdUI->Enable(TRUE);
}

LRESULT MenuBar::OnMenuPopup(WPARAM wparam, LPARAM lparam)
{
  TrackPopupMenu((int)wparam,lparam != FALSE);
  return 1;
}

void MenuBar::OnUpdateCmdUI(CFrameWnd* target, BOOL disableIfNoHndler)
{
  if (m_tracking == TRACK_NONE)
  {
    BOOL always;
    if (::SystemParametersInfo(SPI_GETKEYBOARDCUES,0,&always,0) == 0)
      always = TRUE;

    bool alt = ((::GetKeyState(VK_MENU) & 0x8000) != 0);
    bool f10 = ((::GetKeyState(VK_F10) & 0x8000) != 0) && m_useF10;

    // Show or hide the menu keyboard shortcuts
    SendMessage(WM_UPDATEUISTATE,
      MAKEWPARAM((always || alt || f10) ? UIS_CLEAR : UIS_SET,UISF_HIDEACCEL));
  }
  CToolBar::OnUpdateCmdUI(target,disableIfNoHndler);
}

BOOL MenuBar::OnMenuInput(MSG& msg)
{
  switch (msg.message)
  {
  case WM_KEYDOWN:
    if ((msg.wParam == VK_LEFT) && m_arrowLeft)
      TrackNewMenu(GetNextButton(m_popupTrack,true));
    else if ((msg.wParam == VK_RIGHT) && m_arrowRight)
      TrackNewMenu(GetNextButton(m_popupTrack,false));
    else if (msg.wParam == VK_ESCAPE)
      m_escapePressed = true;
    break;

  case WM_MOUSEMOVE:
    {
      // If the mouse has moved to a new menu, show it
      CPoint pt = msg.lParam;
      ScreenToClient(&pt);
      if (pt != m_mouse)
      {
        int button = HitTest(pt);
        if ((button >= 0) && (button != m_popupTrack))
          TrackNewMenu(button);
        m_mouse = pt;
      }
    }
    break;

  case WM_LBUTTONDOWN:
    {
      // If the button for the current menu is selected, cancel the menu
      CPoint pt = msg.lParam;
      ScreenToClient(&pt);
      if (HitTest(pt) == m_popupTrack)
      {
        TrackNewMenu(-1);
        return TRUE;
      }
    }
    break;
  }
  return FALSE;
}

void MenuBar::OnMenuSelect(HMENU menu, UINT flags)
{
  if (m_tracking != TRACK_NONE)
  {
    m_arrowRight = ((flags & MF_POPUP) == 0);
    m_arrowLeft = (menu == m_popupMenu);
  }
}

void MenuBar::OnMeasureItem(LPMEASUREITEMSTRUCT mis)
{
  if (mis->CtlType == ODT_MENU)
  {
    Bitmap bitmap;
    if (m_bitmaps.Lookup(mis->itemID,bitmap))
    {
      mis->itemWidth = bitmap.size.cx;
      mis->itemHeight = bitmap.size.cy;
    }
    else
    {
      mis->itemWidth = 0;
      mis->itemHeight = 0;
    }
  }
}

void MenuBar::OnDrawItem(LPDRAWITEMSTRUCT dis)
{
  if ((dis->CtlType == ODT_MENU) && ((dis->itemState & ODS_GRAYED) == 0))
  {
    Bitmap bitmap;
    if (m_bitmaps.Lookup(dis->itemID,bitmap))
    {
      CDC dcFrom;
      dcFrom.CreateCompatibleDC(NULL);
      CDC* dc = CDC::FromHandle(dis->hDC);

      // Copy in the initial bitmap values and adjust for the background colour
      ::GdiFlush();
      memcpy(bitmap.bits,bitmap.initialBits,bitmap.size.cx*bitmap.size.cy*sizeof(DWORD));
      COLORREF back = dc->GetBkColor();
      for (int y = 0; y < bitmap.size.cy; y++)
      {
        for (int x = 0; x < bitmap.size.cx; x++)
        {
          DWORD pixel = bitmap.bits[x+(y*bitmap.size.cx)];
          int b = pixel & 0xff; pixel >>= 8;
          int g = pixel & 0xff; pixel >>= 8;
          int r = pixel & 0xff; pixel >>= 8;
          int a = pixel & 0xff;

          if (a == 0)
          {
            r = GetRValue(back); g = GetGValue(back); b = GetBValue(back);
            bitmap.bits[x+(y*bitmap.size.cx)] = (0xff<<24)|(r<<16)|(g<<8)|b;
          }
          else if (a == 255)
          {
            // Do nothing
          }
          else
          {
            a = 255-a;
            r += (a * (GetRValue(back)) >> 8);
            g += (a * (GetGValue(back)) >> 8);
            b += (a * (GetBValue(back)) >> 8);
            bitmap.bits[x+(y*bitmap.size.cx)] = (0xff<<24)|(r<<16)|(g<<8)|b;
          }
        }
      }

      CBitmap* old = dcFrom.SelectObject(CBitmap::FromHandle(bitmap.bitmap));
      CRect r(dis->rcItem);
      dc->BitBlt(r.left,r.top,r.Width(),r.Height(),&dcFrom,0,0,SRCCOPY);
      dcFrom.SelectObject(old);
    }
  }
}

void MenuBar::TrackPopupMenu(int button, bool keyboard)
{
  while (button >= 0)
  {
    // Show the menu as selected in the menu bar
    m_popupNew = -1;
    GetToolBarCtrl().PressButton(button,TRUE);
    UpdateWindow();

    // If triggered by the keyboard, select the first item
    if (keyboard)
    {
      GetOwner()->PostMessage(WM_KEYDOWN,VK_DOWN,1);
      GetOwner()->PostMessage(WM_KEYUP,VK_DOWN,1);
    }

    SetTrackingState(TRACK_POPUP,button);

    // Trap menu input for keys and "hot tracking"
    menuBar = this;
    msgHook = ::SetWindowsHookEx(WH_MSGFILTER,InputFilter,NULL,::GetCurrentThreadId());

    // Show the menu
    CRect rect;
    GetToolBarCtrl().GetRect(button,rect);
    ClientToScreen(&rect);
    CMenu* menu = m_menu.GetSubMenu(button);
    m_popupMenu = menu->GetSafeHmenu();
    menu->TrackPopupMenu(TPM_LEFTALIGN|TPM_LEFTBUTTON|TPM_VERTICAL,rect.left,rect.bottom,
      GetParentFrame(),&rect);
    m_popupMenu = 0;

    // Remove the input hook
    ::UnhookWindowsHookEx(msgHook);
    msgHook = NULL;
    menuBar = NULL;

    GetToolBarCtrl().PressButton(button,FALSE);
    UpdateWindow();

    SetTrackingState(m_escapePressed ? TRACK_BUTTON : TRACK_NONE,button);

    // If not -1, then the user has moved to another menu
    button = m_popupNew;
  }
}

void MenuBar::SetTrackingState(TrackingState track, int button)
{
  if (track == m_tracking)
    return;

  if (track == TRACK_NONE)
    button = -1;
  GetToolBarCtrl().SetHotItem(button);

  // Start tracking a menu
  if (track == TRACK_POPUP)
  {
    m_escapePressed = false;
    m_arrowRight = true;
    m_arrowLeft = true;
    m_popupTrack = button;
  }

  m_tracking = track;
}

void MenuBar::TrackNewMenu(int button)
{
  if (button != m_popupTrack)
  {
    // Cancel the current menu
    GetParentFrame()->PostMessage(WM_CANCELMODE);

    // Store the index of the new menu to show
    m_popupNew = button;
  }
}

int MenuBar::HitTest(CPoint pt)
{
  int hit = GetToolBarCtrl().HitTest(&pt);
  if ((hit >= 0) && (hit < GetToolBarCtrl().GetButtonCount()))
  {
    // Check that the hit button is visible
    CRect rect;
    GetClientRect(&rect);
    if (rect.PtInRect(pt))
      return hit;
  }
  return -1;
}

int MenuBar::GetNextButton(int button, bool goBack)
{
  if (goBack)
  {
    button--;
    if (button < 0)
      button = GetToolBarCtrl().GetButtonCount()-1;
  }
  else
  {
    button++;
    if (button >= GetToolBarCtrl().GetButtonCount())
      button = 0;
  }
  return button;
}

void MenuBar::SetBitmaps(CMenu* menu)
{
  if (!m_useBitmaps)
    return;

  bool hasBitmap = false;
  for (UINT i = 0; i < menu->GetMenuItemCount(); i++)
  {
    UINT id = menu->GetMenuItemID(i);
    switch (id)
    {
    case -1: // Sub-menu
      SetBitmaps(menu->GetSubMenu(i));
      break;
    case 0: // Separator
      break;
    default:
      {
        Bitmap bitmap;
        if (m_bitmaps.Lookup(id,bitmap))
        {
          MENUITEMINFO mii;
          mii.cbSize = sizeof mii;
          mii.fMask = MIIM_BITMAP;

          // Alpha bitmaps for menu items are only supported from Vista onwards:
          // for earlier Windows we will have to draw the bitmap ourselves.
          if (m_osVer.dwMajorVersion < 6)
            mii.hbmpItem = HBMMENU_CALLBACK;
          else
            mii.hbmpItem = bitmap.bitmap;
          menu->SetMenuItemInfo(i,&mii,TRUE);
          hasBitmap = true;
        }
      }
      break;
    }
  }

  if (hasBitmap)
  {
    MENUINFO mi;
    mi.cbSize = sizeof mi;
    mi.fMask = MIM_STYLE;
    (*m_getMenuInfo)(menu->GetSafeHmenu(),&mi);
    mi.dwStyle |= MNS_CHECKORBMP;
    (*m_setMenuInfo)(menu->GetSafeHmenu(),&mi);
  }
}

void MenuBar::UpdateFont(void)
{
  if (m_font.GetSafeHandle() != 0)
    m_font.DeleteObject();

  GetToolBarCtrl().SetButtonSize(CSize(0,::GetSystemMetrics(SM_CYMENU)));

  // Use the system menu font
  NONCLIENTMETRICS ncm;
  ncm.cbSize = sizeof ncm;
  ::SystemParametersInfo(SPI_GETNONCLIENTMETRICS,ncm.cbSize,&ncm,0);
  m_font.CreateFontIndirect(&ncm.lfMenuFont);
  SetFont(&m_font);
}

LRESULT CALLBACK MenuBar::InputFilter(int code, WPARAM wp, LPARAM lp)
{
  // Intercept any menu related messages
  if ((code == MSGF_MENU) && menuBar)
  {
    if (menuBar->OnMenuInput(*((MSG*)lp)))
      return TRUE;
  }
  return ::CallNextHookEx(msgHook,code,wp,lp);
}

DWORD MenuBar::GetDllVersion(const char* dllName)
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
        version = DLLVERSION(dvi.dwMajorVersion,dvi.dwMinorVersion);
    }
    ::FreeLibrary(dll);
  }
  return version;
}

MenuBar::Bitmap::Bitmap() : size(0,0)
{
  bitmap = 0;
  bits = NULL;
  initialBits = NULL;
}

MenuBar::Bitmap::Bitmap(HBITMAP bitmap_, SIZE size_, DWORD* bits_, DWORD* initialBits_) : size(size_)
{
  bitmap = bitmap_;
  bits = bits_;
  initialBits = initialBits_;
}

IMPLEMENT_DYNAMIC(MenuBarFrameWnd, CFrameWnd)

BEGIN_MESSAGE_MAP(MenuBarFrameWnd, CFrameWnd)
  ON_WM_MENUSELECT()
  ON_WM_MEASUREITEM()
  ON_WM_DRAWITEM()
END_MESSAGE_MAP()

void MenuBarFrameWnd::OnMenuSelect(UINT id, UINT flags, HMENU menu)
{
#if (_MFC_VER == 0x700) // MFC bug
  menu = ((CMenu*)menu)->GetSafeHmenu();
#endif

  if (m_menuBar.GetSafeHwnd() != 0)
    m_menuBar.OnMenuSelect(menu,flags);
  CFrameWnd::OnMenuSelect(id,flags,menu);
}

void MenuBarFrameWnd::OnMeasureItem(int id, LPMEASUREITEMSTRUCT mis)
{
  if (m_menuBar.GetSafeHwnd() != 0)
    m_menuBar.OnMeasureItem(mis);
  CFrameWnd::OnMeasureItem(id,mis);
}

void MenuBarFrameWnd::OnDrawItem(int id, LPDRAWITEMSTRUCT dis)
{
  if (m_menuBar.GetSafeHwnd() != 0)
    m_menuBar.OnDrawItem(dis);
  CFrameWnd::OnDrawItem(id,dis);
}

BOOL MenuBarFrameWnd::PreTranslateMessage(MSG* msg)
{
  if (m_menuBar.GetSafeHwnd() != 0)
  {
    if ((msg->hwnd == GetSafeHwnd()) || IsChild(CWnd::FromHandle(msg->hwnd)))
    {
      if (m_menuBar.TranslateFrameMessage(msg))
        return TRUE;
    }
  }
  return CFrameWnd::PreTranslateMessage(msg);
}

BOOL MenuBarFrameWnd::CreateMenuBar(UINT id, CMenu* menu)
{
  if (!m_menuBar.Create(id,menu,this))
    return FALSE;

  // Create the cool bar with an appropriate style
  DWORD style = WS_CHILD|WS_VISIBLE|WS_CLIPSIBLINGS|WS_CLIPCHILDREN;
  style |= (m_menuBar.GetSafeHwnd() != 0) ? CBRS_ALIGN_TOP : CBRS_TOP;
  if (!m_coolBar.Create(this,0,style))
    return FALSE;

  // Add the menus and toolbar
  if (m_menuBar.GetSafeHwnd() != 0)
  {
    // Only add the menu bar if it was created. If not, we are just using
    // the ordinary Windows menus.
    if (!m_coolBar.AddBar(&m_menuBar,NULL,NULL,RBBS_NOGRIPPER))
      return FALSE;
  }
  return TRUE;
}

BOOL MenuBarFrameWnd::CreateBar(UINT id, UINT highId)
{
  // Only use high colour bitmaps if using at least common controls
  // version 6 on a 32 bit colour display
  if (!IsHighColour())
    highId = (UINT)-1;

  // Create the toolbar and load the resource for it
  if (!m_toolBar.CreateEx(this,TBSTYLE_FLAT|TBSTYLE_TRANSPARENT,
    WS_CHILD|WS_VISIBLE|CBRS_ALIGN_TOP|CBRS_TOOLTIPS|CBRS_FLYBY))
    return FALSE;
  if (!m_toolBar.LoadToolBar(id))
    return FALSE;

  // If a high colour bitmap identifier has been passed, add it to the toolbar
  if (highId != -1)
  {
    CBitmap bitmap;
    LoadBitmap(bitmap,highId);
    if (!m_toolBar.SetBitmap((HBITMAP)bitmap.Detach()))
      return FALSE;
  }

  // Load the bitmap again for the menu icons
  CBitmap bitmap;
  LoadBitmap(bitmap,highId != -1 ? highId : id);
  m_menuBar.LoadBitmaps(bitmap,m_toolBar.GetToolBarCtrl(),CSize(16,15),highId != -1);
  if (!m_menuBar.Create(id,0,this))
    return FALSE;

  // Create the cool bar with an appropriate style
  DWORD style = WS_CHILD|WS_VISIBLE|WS_CLIPSIBLINGS|WS_CLIPCHILDREN;
  style |= (m_menuBar.GetSafeHwnd() != 0) ? CBRS_ALIGN_TOP : CBRS_TOP;
  if (!m_coolBar.Create(this,0,style))
    return FALSE;

  // Add the menus and toolbar
  if (m_menuBar.GetSafeHwnd() != 0)
  {
    // Only add the menu bar if it was created. If not, we are just using
    // the ordinary Windows menus.
    if (!m_coolBar.AddBar(&m_menuBar,NULL,NULL,RBBS_NOGRIPPER))
      return FALSE;
  }
  if (!m_coolBar.AddBar(&m_toolBar,NULL,NULL,RBBS_NOGRIPPER|RBBS_BREAK))
    return FALSE;
  return TRUE;
}

CMenu* MenuBarFrameWnd::GetMenu(void) const
{
  if (m_menuBar.GetSafeHwnd() != 0)
    return m_menuBar.GetMenu();
  return CFrameWnd::GetMenu();
}

bool MenuBarFrameWnd::IsHighColour(void)
{
  DWORD commonVer = MenuBar::GetDllVersion("comctl32.dll");
  HDC dc = ::GetDC(NULL);
  int colourDepth = ::GetDeviceCaps(dc,BITSPIXEL);
  ::ReleaseDC(NULL,dc);
  return ((commonVer >= DLLVERSION(6,0)) && (colourDepth >= 32));
}

void MenuBarFrameWnd::LoadBitmap(CBitmap& bitmap, UINT id)
{
  bitmap.LoadBitmap(id);
  if (bitmap.GetSafeHandle() == 0)
    bitmap.Attach(::LoadBitmap(::GetModuleHandle(NULL),MAKEINTRESOURCE(id)));
}
