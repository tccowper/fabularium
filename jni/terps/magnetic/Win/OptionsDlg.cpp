/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// OptionsDlg.cpp: Options dialog class
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"

#include "Magnetic.h"
#include "OptionsDlg.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// Implementation of the COptionsDlg dialog
/////////////////////////////////////////////////////////////////////////////

COptionsDlg::COptionsDlg(CWnd* pParent /*=NULL*/) : BaseDialog(COptionsDlg::IDD, pParent)
{
  //{{AFX_DATA_INIT(COptionsDlg)
  m_dScaleFactor = 0.0;
  m_dScaleTitles = 0.0;
  m_bPredict = FALSE;
  m_iSeed = 0;
  m_iShowPics = -1;
  m_dGamma = 0.0;
  m_bAnimWait = FALSE;
  m_bHintWindow = FALSE;
  //}}AFX_DATA_INIT
}

void COptionsDlg::DoDataExchange(CDataExchange* pDX)
{
  BaseDialog::DoDataExchange(pDX);
  //{{AFX_DATA_MAP(COptionsDlg)
  DDX_Control(pDX, IDC_PREDICT, m_Predict);
  DDX_Control(pDX, IDC_SEED, m_Seed);
  DDX_Control(pDX, IDC_SEED_LABEL, m_SeedLabel);
  DDX_Text(pDX, IDC_SCALE, m_dScaleFactor);
  DDV_MinMaxDouble(pDX, m_dScaleFactor, 0.5, 5.);
  DDX_Text(pDX, IDC_SCALET, m_dScaleTitles);
  DDV_MinMaxDouble(pDX, m_dScaleTitles, 0.5, 5.);
  DDX_Check(pDX, IDC_PREDICT, m_bPredict);
  DDX_Text(pDX, IDC_SEED, m_iSeed);
  DDX_CBIndex(pDX, IDC_SHOWPIC, m_iShowPics);
  DDX_Text(pDX, IDC_GAMMA, m_dGamma);
  DDV_MinMaxDouble(pDX, m_dGamma, 0.5, 5.);
  DDX_Check(pDX, IDC_ANIM_WAIT, m_bAnimWait);
  DDX_Check(pDX, IDC_HINT_WINDOW, m_bHintWindow);
  //}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(COptionsDlg, BaseDialog)
  //{{AFX_MSG_MAP(COptionsDlg)
  ON_WM_HELPINFO()
  ON_BN_CLICKED(IDC_PREDICT, OnChangePredict)
  //}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// COptionsDlg message handlers
/////////////////////////////////////////////////////////////////////////////

BOOL COptionsDlg::OnInitDialog() 
{
  BaseDialog::OnInitDialog();
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  
  // Subclass the spin controls
  if (m_Spin.SubclassDlgItem(IDC_SPIN,this) == FALSE)
    return FALSE;
  m_Spin.SetRange(1,5);
  if (m_SpinTitles.SubclassDlgItem(IDC_SPINT,this) == FALSE)
    return FALSE;
  m_SpinTitles.SetRange(1,5);
  if (m_SpinGamma.SubclassDlgItem(IDC_SPING,this) == FALSE)
    return FALSE;
  m_SpinGamma.SetRange(1,5);
  
  // Subclass the colour controls
  if (m_FColour.SubclassDlgItem(IDC_FORE,this) == FALSE)
    return FALSE;
  if (m_BColour.SubclassDlgItem(IDC_BACK,this) == FALSE)
    return FALSE;
  if (m_GColour.SubclassDlgItem(IDC_GFX,this) == FALSE)
    return FALSE;

  // Set the colours
  m_FColour.SetCurrentColour(pApp->GetForeColour());
  m_BColour.SetCurrentColour(pApp->GetBackColour());
  m_GColour.SetCurrentColour(pApp->GetGfxColour());

  m_Seed.EnableWindow(m_bPredict == TRUE);
  return TRUE;
}

HWND WINAPI AfxHtmlHelp(HWND hWnd, LPCTSTR szHelpFilePath, UINT nCmd, DWORD_PTR dwData);

BOOL COptionsDlg::OnHelpInfo(HELPINFO* pHelpInfo) 
{
  static int m_helpIds[] =
  {
    IDC_SHOWPIC,1,
    IDC_PIC_LABEL,1,
    IDC_SCALE,2,
    IDC_SCALE_LABEL,2,
    IDC_SPIN,2,
    IDC_FORE,3,
    IDC_TEXT_LABEL,3,
    IDC_BACK,4,
    IDC_BACK_LABEL,4,
    IDC_SCALET,5,
    IDC_SCALET_LABEL,5,
    IDC_SPINT,5,
    IDC_GAMMA,6,
    IDC_GAMMA_LABEL,6,
    IDC_SPING,6,
    IDC_GFX,7,
    IDC_GFX_LABEL,7,
    IDC_PREDICT,8,
    IDC_SEED,8,
    IDC_SEED_LABEL,8,
    IDC_ANIM_WAIT,9,
    IDC_HINT_WINDOW,10,
    0,0
  };

  if (pHelpInfo->iContextType == HELPINFO_WINDOW)
  {
    // Is there a help topic for this control?
    int* id = m_helpIds;
    while (*id != 0)
    {
      if (pHelpInfo->iCtrlId == *id)
      {
        CString helpFile(AfxGetApp()->m_pszHelpFilePath);
        helpFile.Append("::/options.txt");

        // Show the help popup
        AfxHtmlHelp((HWND)pHelpInfo->hItemHandle,helpFile,
          HH_TP_HELP_WM_HELP,(DWORD_PTR)m_helpIds);
        return TRUE;
      }
      id += 2;
    }
  }
  return TRUE;
}

void COptionsDlg::OnChangePredict() 
{
  m_Seed.EnableWindow(m_Predict.GetCheck() == 1);
}

COLORREF COptionsDlg::GetForeColour(void)
{
  return m_FColour.GetCurrentColour();
}

COLORREF COptionsDlg::GetBackColour(void)
{
  return m_BColour.GetCurrentColour();
}

COLORREF COptionsDlg::GetGfxColour(void)
{
  return m_GColour.GetCurrentColour();
}
