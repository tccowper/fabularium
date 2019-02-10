#pragma once

#include <afxpriv.h>

class BaseDialog : public CDialog
{
  DECLARE_DYNAMIC(BaseDialog)

public:
  BaseDialog(UINT templateId, CWnd* parent = NULL);

  virtual INT_PTR DoModal();
  virtual BOOL Create(LPCTSTR lpszTemplateName, CWnd* pParentWnd = NULL);

protected:
  BaseDialog();
  virtual void SetFont(CDialogTemplate& dlgTemplate);

  BOOL CreateDlgIndirect(LPCDLGTEMPLATE lpDialogTemplate, CWnd* pParentWnd, HINSTANCE hInst);
};

class GetFontDialog : public BaseDialog
{
public:
  GetFontDialog(LOGFONT& logFont, UINT templateId, CWnd* parent = NULL);

protected:
  virtual BOOL OnInitDialog();
  LOGFONT& m_logFont;
};

class SimpleFileDialog : public CFileDialog
{
  DECLARE_DYNAMIC(SimpleFileDialog)

public:
  explicit SimpleFileDialog(BOOL bOpenFileDialog,
    LPCTSTR lpszDefExt = NULL,
    LPCTSTR lpszFileName = NULL,
    DWORD dwFlags = OFN_HIDEREADONLY | OFN_OVERWRITEPROMPT,
    LPCTSTR lpszFilter = NULL,
    CWnd* pParentWnd = NULL);

  virtual INT_PTR DoModal();
};
