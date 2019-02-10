/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// HintDialog.h: Hint dialog class
//
/////////////////////////////////////////////////////////////////////////////

#include "Dialogs.h"

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

// Listbox for displaying hints
class CHintListBox : public CListBox
{
  DECLARE_DYNAMIC(CHintListBox)

public:
  CHintListBox();
  virtual ~CHintListBox();

protected:
  DECLARE_MESSAGE_MAP()

public:
  virtual void MeasureItem(LPMEASUREITEMSTRUCT /*lpMeasureItemStruct*/);
  virtual void DrawItem(LPDRAWITEMSTRUCT /*lpDrawItemStruct*/);

public:
  void SetItemHeights(void);
};

// Dialog for showing hints
class CHintDialog : public BaseDialog
{
// Construction
public:
  CHintDialog(CWnd* pParent = NULL);   // standard constructor

// Dialog Data
  //{{AFX_DATA(CHintDialog)
  enum { IDD = IDD_HINTS };
  CButton  m_topicButton;
  CButton  m_hintButton;
  CButton  m_prevButton;
  CHintListBox m_hintList;
  //}}AFX_DATA

// Overrides
  // ClassWizard generated virtual function overrides
  //{{AFX_VIRTUAL(CHintDialog)
protected:
  virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
  virtual BOOL DestroyWindow();
  //}}AFX_VIRTUAL

// Implementation
protected:

  // Generated message map functions
  //{{AFX_MSG(CHintDialog)
  virtual BOOL OnInitDialog();
  afx_msg void OnSize(UINT nType, int cx, int cy);
  afx_msg void OnPrevious();
  afx_msg void OnTopics();
  afx_msg void OnShowHint();
  afx_msg void OnChangeHints();
  afx_msg void OnDblClkHints();
  afx_msg void OnGetMinMaxInfo(MINMAXINFO* lpMMI);
  //}}AFX_MSG
  DECLARE_MESSAGE_MAP()

public:
    void SetHints(struct ms_hint* hints);

protected:
  int LoadHintSet(int element);
  void UpdateHintList(void);

  struct ms_hint* m_allHints;
  int m_currHint;
  int m_visibleHints;

protected:
  void LayoutControls(void);

  CHintListBox m_hintControl;
  CRect m_btnSize;
};
