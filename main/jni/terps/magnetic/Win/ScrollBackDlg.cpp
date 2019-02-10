/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// ScrollBackDlg.cpp: Scrollback dialog class
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"

#include "Magnetic.h"
#include "ScrollBackDlg.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// Implementation of the CScrollBackDlg dialog
/////////////////////////////////////////////////////////////////////////////

CScrollBackDlg::CScrollBackDlg(CWnd* pParent /*=NULL*/)
  : m_TextTop(0), BaseDialog(CScrollBackDlg::IDD, pParent)
{
  //{{AFX_DATA_INIT(CScrollBackDlg)
    // NOTE: the ClassWizard will add member initialization here
  //}}AFX_DATA_INIT
}

void CScrollBackDlg::DoDataExchange(CDataExchange* pDX)
{
  BaseDialog::DoDataExchange(pDX);
  //{{AFX_DATA_MAP(CScrollBackDlg)
    // NOTE: the ClassWizard will add DDX and DDV calls here
  //}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(CScrollBackDlg, BaseDialog)
  //{{AFX_MSG_MAP(CScrollBackDlg)
  ON_WM_SIZE()
  ON_BN_CLICKED(IDC_COPY, OnCopy)
  //}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CScrollBackDlg message handlers
/////////////////////////////////////////////////////////////////////////////

BOOL CScrollBackDlg::OnInitDialog() 
{
  BaseDialog::OnInitDialog();
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  
  // Subclass the text control
  if (m_RichEdit.SubclassDlgItem(IDC_TEXT,this) == FALSE)
    return FALSE;

  // Get the relative position of the top of the text control
  CRect size;
  m_RichEdit.GetWindowRect(size);
  ScreenToClient(size);
  m_TextTop = size.top;

  // Change the window icon
  SetIcon(pApp->LoadIcon(IDR_MAINFRAME),TRUE);

  // Resize the dialog
  BaseDialog::MoveWindow(m_DialogRect);

  // Set the control to format the text so that it fits
  // into the window
  m_RichEdit.SetTargetDevice(NULL,0);
  
  // Set the background colour
  m_RichEdit.SetBackgroundColor(FALSE,GetSysColor(COLOR_3DFACE));

  // Put the text into the control
  m_RichEdit.SetWindowText(m_strScrollback);

  // Put the cursor at the end of the buffer
  m_RichEdit.SetSel(-1,-1);
  m_RichEdit.SendMessage(EM_SCROLLCARET);

  return TRUE;
}

void CScrollBackDlg::OnSize(UINT nType, int cx, int cy) 
{
  BaseDialog::OnSize(nType, cx, cy);

  // Resize the text control
  if (m_RichEdit.GetSafeHwnd() != NULL)
    m_RichEdit.SetWindowPos(NULL,0,m_TextTop,cx,cy-m_TextTop,SWP_NOZORDER);
}

void CScrollBackDlg::OnCopy() 
{
  m_RichEdit.Copy();
}

CRect& CScrollBackDlg::GetRect(void)
{
  return m_DialogRect;
}

CString& CScrollBackDlg::GetScrollback(void)
{
  return m_strScrollback;
}
