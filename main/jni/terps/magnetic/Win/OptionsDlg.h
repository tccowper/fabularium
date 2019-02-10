/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// OptionsDlg.h: Options dialog class
//
/////////////////////////////////////////////////////////////////////////////

#include "ColourButton.h"
#include "Dialogs.h"

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

class COptionsDlg : public BaseDialog
{
// Construction
public:
  COptionsDlg(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
  //{{AFX_DATA(COptionsDlg)
  enum { IDD = IDD_OPTIONS };
  CButton  m_Predict;
  CEdit    m_Seed;
  CStatic  m_SeedLabel;
  double  m_dScaleFactor;
  double  m_dScaleTitles;
  BOOL    m_bPredict;
  int      m_iSeed;
  int      m_iShowPics;
  double  m_dGamma;
  BOOL    m_bAnimWait;
  BOOL    m_bHintWindow;
  //}}AFX_DATA

// Overrides
  // ClassWizard generated virtual function overrides
  //{{AFX_VIRTUAL(COptionsDlg)
  protected:
  virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
  //}}AFX_VIRTUAL

// Implementation
protected:

  // Generated message map functions
  //{{AFX_MSG(COptionsDlg)
  virtual BOOL OnInitDialog();
  afx_msg BOOL OnHelpInfo(HELPINFO* pHelpInfo);
  afx_msg void OnChangePredict();
  //}}AFX_MSG
  DECLARE_MESSAGE_MAP()

public:
  COLORREF GetForeColour(void);
  COLORREF GetBackColour(void);
  COLORREF GetGfxColour(void);

protected:
  CSpinButtonCtrl m_Spin, m_SpinTitles, m_SpinGamma;
  ColourButton m_FColour, m_BColour, m_GColour;
  CButton m_FDefault, m_BDefault;
};
