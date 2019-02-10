#include "stdafx.h"
#include "Dialogs.h"

IMPLEMENT_DYNAMIC(BaseDialog, CDialog)

BaseDialog::BaseDialog(UINT templateId, CWnd* parent) : CDialog(templateId,parent)
{
}

BaseDialog::BaseDialog()
{
}

// Copied from MFC sources to enable a call to our CreateDlgIndirect()
INT_PTR BaseDialog::DoModal()
{
  LPCDLGTEMPLATE lpDialogTemplate = m_lpDialogTemplate;
  HGLOBAL hDialogTemplate = m_hDialogTemplate;
  HINSTANCE hInst = AfxGetResourceHandle();
  if (m_lpszTemplateName != NULL)
  {
    hInst = AfxFindResourceHandle(m_lpszTemplateName, RT_DIALOG);
    HRSRC hResource = ::FindResource(hInst, m_lpszTemplateName, RT_DIALOG);
    hDialogTemplate = LoadResource(hInst, hResource);
  }
  if (hDialogTemplate != NULL)
    lpDialogTemplate = (LPCDLGTEMPLATE)LockResource(hDialogTemplate);

  if (lpDialogTemplate == NULL)
    return -1;

  HWND hWndParent = PreModal();
  AfxUnhookWindowCreate();
  BOOL bEnableParent = FALSE;
  if (hWndParent && hWndParent != ::GetDesktopWindow() && ::IsWindowEnabled(hWndParent))
  {
    ::EnableWindow(hWndParent, FALSE);
    bEnableParent = TRUE;
  }

  TRY
  {
    AfxHookWindowCreate(this);
    if (CreateDlgIndirect(lpDialogTemplate, CWnd::FromHandle(hWndParent), hInst))
    {
      if (m_nFlags & WF_CONTINUEMODAL)
      {
        DWORD dwFlags = MLF_SHOWONIDLE;
        if (GetStyle() & DS_NOIDLEMSG)
          dwFlags |= MLF_NOIDLEMSG;
        RunModalLoop(dwFlags);
      }

      if (m_hWnd != NULL)
      {
        SetWindowPos(NULL, 0, 0, 0, 0, SWP_HIDEWINDOW|
          SWP_NOSIZE|SWP_NOMOVE|SWP_NOACTIVATE|SWP_NOZORDER);
      }
    }
  }
  CATCH_ALL(e)
  {
    e->Delete();
    m_nModalResult = -1;
  }
  END_CATCH_ALL

  if (bEnableParent)
    ::EnableWindow(hWndParent, TRUE);
  if (hWndParent != NULL && ::GetActiveWindow() == m_hWnd)
    ::SetActiveWindow(hWndParent);

  DestroyWindow();
  PostModal();

  if (m_lpszTemplateName != NULL || m_hDialogTemplate != NULL)
    UnlockResource(hDialogTemplate);
  if (m_lpszTemplateName != NULL)
    FreeResource(hDialogTemplate);

  return m_nModalResult;
}

// Copied from MFC sources to enable a call to our CreateDlgIndirect()
BOOL BaseDialog::Create(LPCTSTR lpszTemplateName, CWnd* pParentWnd)
{
  m_lpszTemplateName = lpszTemplateName;
  if (IS_INTRESOURCE(m_lpszTemplateName) && m_nIDHelp == 0)
    m_nIDHelp = LOWORD((DWORD_PTR)m_lpszTemplateName);

  HINSTANCE hInst = AfxFindResourceHandle(lpszTemplateName, RT_DIALOG);
  HRSRC hResource = ::FindResource(hInst, lpszTemplateName, RT_DIALOG);
  HGLOBAL hTemplate = LoadResource(hInst, hResource);
  LPCDLGTEMPLATE lpDialogTemplate = (LPCDLGTEMPLATE)LockResource(hTemplate);

  if (pParentWnd == NULL)
    pParentWnd = AfxGetMainWnd();
  m_lpDialogInit = NULL;
  BOOL bResult = CreateDlgIndirect(lpDialogTemplate, pParentWnd, hInst);

  UnlockResource(hTemplate);
  FreeResource(hTemplate);

  return bResult;
}

INT_PTR CALLBACK AfxDlgProc(HWND, UINT, WPARAM, LPARAM);

BOOL BaseDialog::CreateDlgIndirect(LPCDLGTEMPLATE lpDialogTemplate, CWnd* pParentWnd, HINSTANCE hInst)
{
  if(!hInst)
    hInst = AfxGetResourceHandle();

  HGLOBAL hTemplate = NULL;
  HWND hWnd = NULL;

  TRY
  {
    CDialogTemplate dlgTemp(lpDialogTemplate);
    SetFont(dlgTemp);
    hTemplate = dlgTemp.Detach();
    lpDialogTemplate = (DLGTEMPLATE*)GlobalLock(hTemplate);

    m_nModalResult = -1;
    m_nFlags |= WF_CONTINUEMODAL;

    AfxHookWindowCreate(this);
    hWnd = ::CreateDialogIndirect(hInst, lpDialogTemplate,
      pParentWnd->GetSafeHwnd(), AfxDlgProc);
  }
  CATCH_ALL(e)
  {
    e->Delete();
    m_nModalResult = -1;
  }
  END_CATCH_ALL

  if (!AfxUnhookWindowCreate())
    PostNcDestroy();

  if (hWnd != NULL && !(m_nFlags & WF_CONTINUEMODAL))
  {
    ::DestroyWindow(hWnd);
    hWnd = NULL;
  }

  if (hTemplate != NULL)
  {
    GlobalUnlock(hTemplate);
    GlobalFree(hTemplate);
  }

  if (hWnd == NULL)
    return FALSE;
  return TRUE;
}

void BaseDialog::SetFont(CDialogTemplate& dlgTemplate)
{
  NONCLIENTMETRICS ncm;
  ::ZeroMemory(&ncm,sizeof ncm);
  ncm.cbSize = sizeof ncm;
  ::SystemParametersInfo(SPI_GETNONCLIENTMETRICS,sizeof ncm,&ncm,0);

  OSVERSIONINFO osvi;
  osvi.dwOSVersionInfoSize = sizeof osvi;
  ::GetVersionEx(&osvi);
  WORD fontSize = (osvi.dwMajorVersion < 6) ? 8 : 9;

  dlgTemplate.SetFont(ncm.lfMessageFont.lfFaceName,fontSize);
}

GetFontDialog::GetFontDialog(LOGFONT& logFont, UINT templateId, CWnd* parent)
  : BaseDialog(templateId,parent), m_logFont(logFont)
{
  ::ZeroMemory(&m_logFont,sizeof (LOGFONT));
}

BOOL GetFontDialog::OnInitDialog()
{
  GetFont()->GetLogFont(&m_logFont);
  EndDialog(0);
  return FALSE;
}

IMPLEMENT_DYNAMIC(SimpleFileDialog, CFileDialog)

SimpleFileDialog::SimpleFileDialog(BOOL bOpenFileDialog,
  LPCTSTR lpszDefExt, LPCTSTR lpszFileName, DWORD dwFlags,
  LPCTSTR lpszFilter, CWnd* pParentWnd)
: CFileDialog(bOpenFileDialog, lpszDefExt, lpszFileName,
  dwFlags, lpszFilter, pParentWnd, 0)
{
}

INT_PTR SimpleFileDialog::DoModal()
{
  // Don't use a hook procedure for Vista
  OSVERSIONINFO osvi;
  osvi.dwOSVersionInfoSize = sizeof osvi;
  ::GetVersionEx(&osvi);
  if (osvi.dwMajorVersion >= 6)
    m_ofn.Flags &= ~OFN_ENABLEHOOK;

  DWORD nOffset = lstrlen(m_ofn.lpstrFile)+1;
  memset(m_ofn.lpstrFile+nOffset, 0, (m_ofn.nMaxFile-nOffset)*sizeof(TCHAR));

  HWND hWndFocus = ::GetFocus();
  BOOL bEnableParent = FALSE;
  m_ofn.hwndOwner = PreModal();
  AfxUnhookWindowCreate();
  if (m_ofn.hwndOwner != NULL && ::IsWindowEnabled(m_ofn.hwndOwner))
  {
    bEnableParent = TRUE;
    ::EnableWindow(m_ofn.hwndOwner, FALSE);
  }

  _AFX_THREAD_STATE* pThreadState = AfxGetThreadState();

  if (m_ofn.Flags & OFN_EXPLORER)
    pThreadState->m_pAlternateWndInit = this;
  else
    AfxHookWindowCreate(this);

  INT_PTR nResult;
  if (m_bOpenFileDialog)
    nResult = ::GetOpenFileName(&m_ofn);
  else
    nResult = ::GetSaveFileName(&m_ofn);

  pThreadState->m_pAlternateWndInit = NULL;

  if (bEnableParent)
    ::EnableWindow(m_ofn.hwndOwner, TRUE);
  if (::IsWindow(hWndFocus))
    ::SetFocus(hWndFocus);

  PostModal();
  return nResult ? nResult : IDCANCEL;
}
