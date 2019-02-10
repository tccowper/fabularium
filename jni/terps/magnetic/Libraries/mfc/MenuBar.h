#pragma once

#include <afxtempl.h>

#define WM_MENUBAR_POPUP (WM_USER+0)

typedef HRESULT (STDAPICALLTYPE* SETWINDOWTHEME)(HWND,LPCWSTR,LPCWSTR);
typedef BOOL (WINAPI* GETMENUINFO)(HMENU,LPMENUINFO);
typedef BOOL (WINAPI* SETMENUINFO)(HMENU,LPCMENUINFO);

class MenuBar : public CToolBar
{
public:
  MenuBar();
  ~MenuBar();

  void SetUseF10(bool use);
  void SetUseAltX(bool use);
  BOOL Create(UINT id, CMenu* menu, CWnd* parent);
  void LoadBitmaps(CBitmap& bitmap, CToolBarCtrl& bar, CSize size, bool alpha);
  void Update(void);
  CMenu* GetMenu(void) const;

  BOOL TranslateFrameMessage(MSG* msg);
  void OnMenuSelect(HMENU menu, UINT flags);
  void OnMeasureItem(LPMEASUREITEMSTRUCT mis);
  void OnDrawItem(LPDRAWITEMSTRUCT mis);

  static DWORD GetDllVersion(const char* dllName);

protected: 
  DECLARE_DYNAMIC(MenuBar)

  afx_msg void OnMouseMove(UINT, CPoint);
  afx_msg void OnSettingChange(UINT, LPCTSTR);
  afx_msg void OnCustomDraw(NMHDR*, LRESULT*);
  afx_msg void OnDropDown(NMHDR*, LRESULT*);
  afx_msg void OnHotItemChange(NMHDR*, LRESULT*);
  afx_msg void OnUpdateButton(CCmdUI*);
  afx_msg LRESULT OnMenuPopup(WPARAM, LPARAM);
  DECLARE_MESSAGE_MAP()

  void OnUpdateCmdUI(CFrameWnd*, BOOL);
  BOOL OnMenuInput(MSG& msg);

  enum TrackingState
  {
    TRACK_NONE,   // Not tracking anything
    TRACK_BUTTON, // Tracking buttons (F10/Alt mode)
    TRACK_POPUP   // Tracking popup menus
  };

  void TrackPopupMenu(int button, bool keyboard);
  void SetTrackingState(TrackingState track, int button = -1);
  void TrackNewMenu(int button);
  int HitTest(CPoint pt);
  int GetNextButton(int button, bool goBack);
  void SetBitmaps(CMenu* menu);
  void UpdateFont(void);

  static LRESULT CALLBACK InputFilter(int code, WPARAM wp, LPARAM lp);

  OSVERSIONINFO m_osVer;
  GETMENUINFO m_getMenuInfo;
  SETMENUINFO m_setMenuInfo;
  bool m_useBitmaps;
  bool m_useF10;
  bool m_useAltX;

  CMenu m_menu;
  CFont m_font;

  struct Bitmap
  {
    Bitmap();
    Bitmap(HBITMAP, SIZE, DWORD*, DWORD*);

    HBITMAP bitmap;
    CSize size;
    DWORD* bits;
    DWORD* initialBits;
  };
  CMap<UINT,UINT,Bitmap,Bitmap&> m_bitmaps;

  TrackingState m_tracking;
  int m_popupTrack;
  int m_popupNew;
  HMENU m_popupMenu;
  bool m_arrowLeft;
  bool m_arrowRight;
  bool m_escapePressed;
  CPoint m_mouse;
};

class MenuBarFrameWnd : public CFrameWnd
{
protected: 
  DECLARE_DYNAMIC(MenuBarFrameWnd)

  afx_msg void OnMenuSelect(UINT, UINT, HMENU);
  afx_msg void OnMeasureItem(int, LPMEASUREITEMSTRUCT);
  afx_msg void OnDrawItem(int, LPDRAWITEMSTRUCT);
  DECLARE_MESSAGE_MAP()

  BOOL PreTranslateMessage(MSG*);

  virtual BOOL CreateMenuBar(UINT id, CMenu* menu);
  virtual BOOL CreateBar(UINT id, UINT highId);
  CMenu* GetMenu(void) const;

  static bool IsHighColour(void);
  static void LoadBitmap(CBitmap& bitmap, UINT id);

  CReBar m_coolBar;
  MenuBar m_menuBar;
  CToolBar m_toolBar;
};
