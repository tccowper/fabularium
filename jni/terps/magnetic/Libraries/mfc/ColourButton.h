#pragma once

class ColourButton : public CButton
{
public:
  ColourButton();
  ~ColourButton();

  COLORREF GetCurrentColour(void) { return m_colour; }
  void SetCurrentColour(COLORREF c) { m_colour = c; }

  BOOL SubclassDlgItem(UINT id, CWnd* parent);

protected:
  afx_msg void OnClicked();
  afx_msg void OnCustomDraw(NMHDR*, LRESULT*);

  void DrawItem(LPDRAWITEMSTRUCT);
  void DrawControl(CDC* dc, CRect r, bool disabled, bool focus);

  bool IsAppThemed(void);
  HTHEME OpenThemeData(CWnd* wnd, LPCWSTR classList);
  void CloseThemeData(HTHEME theme);
  void GetThemeBackgroundContentRect(
    HTHEME theme, CDC* dc, int partId, int stateId, RECT* rect);
  DWORD GetDllVersion(const char* dllName);

  DECLARE_MESSAGE_MAP()

  COLORREF m_colour;
  HMODULE m_themes;
};
