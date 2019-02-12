/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// HintDialog.cpp: Hint dialog class
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"
#include "Magnetic.h"
#include "HintDialog.h"

extern "C"
{
#include "defs.h"
}

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// Implementation of the CHintDialog dialog
/////////////////////////////////////////////////////////////////////////////

CHintDialog::CHintDialog(CWnd* pParent) :  BaseDialog(CHintDialog::IDD,pParent)
{
  //{{AFX_DATA_INIT(CHintDialog)
  //}}AFX_DATA_INIT

  m_allHints = NULL;
  m_currHint = 0;
  m_visibleHints = 0;
}

void CHintDialog::DoDataExchange(CDataExchange* pDX)
{
  BaseDialog::DoDataExchange(pDX);
  //{{AFX_DATA_MAP(CHintDialog)
  DDX_Control(pDX, IDC_TOPICS, m_topicButton);
  DDX_Control(pDX, IDC_SHOWHINT, m_hintButton);
  DDX_Control(pDX, IDC_PREVIOUS, m_prevButton);
  DDX_Control(pDX, IDC_HINTLIST, m_hintList);
  //}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(CHintDialog, BaseDialog)
  //{{AFX_MSG_MAP(CHintDialog)
  ON_WM_SIZE()
  ON_WM_GETMINMAXINFO()
  ON_BN_CLICKED(IDC_PREVIOUS, OnPrevious)
  ON_BN_CLICKED(IDC_TOPICS, OnTopics)
  ON_BN_CLICKED(IDC_SHOWHINT, OnShowHint)
  ON_LBN_SELCHANGE(IDC_HINTLIST, OnChangeHints)
  ON_LBN_DBLCLK(IDC_HINTLIST, OnDblClkHints)
  //}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CHintDialog message handlers
/////////////////////////////////////////////////////////////////////////////

void CHintDialog::SetHints(struct ms_hint* hints) 
{
  m_allHints = hints;
}

int CHintDialog::LoadHintSet(int element)
{
  m_hintList.ResetContent();
  if (m_allHints != NULL)
  {
    int j = 0;
    for (int i = 0; i < m_allHints[element].elcount; i++)
    {
      if (m_allHints[element].nodetype == 1)
      {
        m_hintList.AddString((char*)(m_allHints[element].content+j));
        m_visibleHints = 0;
      }
      else
      {
        CString item;
        if (i <= m_visibleHints)
          item.Format("%d %s",i+1,m_allHints[element].content+j);
        else
          item.Format("%d ",i+1);
        m_hintList.AddString(item);
      }
      j += strlen((char*)(m_allHints[element].content+j))+1;
    }
    m_currHint = element;

    // Set controls
    if (m_allHints[element].parent == 0xFFFF)
    {
      m_topicButton.EnableWindow(false);
      m_prevButton.EnableWindow(false);
    }
    else
    {
      m_topicButton.EnableWindow(true);
      m_prevButton.EnableWindow(true);
    }

    if ((m_allHints[m_currHint].nodetype == 1) ||
       ((m_allHints[m_currHint].nodetype != 1) && (m_allHints[m_currHint].elcount == m_visibleHints+1)))
      m_hintButton.EnableWindow(false);

    m_hintList.SetItemHeights();
    return element;
  }
  return -1;
}

void CHintDialog::UpdateHintList()
{
  int currSel = m_hintList.GetCurSel();
  int newIndex = 0;
  if (m_allHints[m_currHint].nodetype == 1)
  {
    newIndex = m_allHints[m_currHint].links[currSel];
    m_visibleHints = 0;
  }
  else
  {
    newIndex = m_currHint;
    m_visibleHints++;
  }
  LoadHintSet(newIndex);
}

void CHintDialog::LayoutControls(void)
{
  CRect size;
  GetClientRect(size);
  int w = size.Width();
  int h = size.Height();
  int bw = m_btnSize.Width();
  int bh = m_btnSize.Height();

  m_topicButton.MoveWindow(w-bw-8,7,bw,bh,FALSE);
  m_hintButton.MoveWindow(w-bw-8,12+bh,bw,bh,FALSE);
  m_prevButton.MoveWindow(w-bw-8,17+(2*bh),bw,bh,FALSE);
  GetDlgItem(IDOK)->MoveWindow(w-bw-8,h-bh-8,bw,bh,FALSE);

  m_hintList.MoveWindow(8,8,w-bw-24,h-16,FALSE);
  m_hintList.SetItemHeights();
  Invalidate(TRUE);
}

BOOL CHintDialog::DestroyWindow()
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  // Save the window position
  WINDOWPLACEMENT Place;
  GetWindowPlacement(&Place);
  pApp->GetHintsRect() = Place.rcNormalPosition;

  return BaseDialog::DestroyWindow();
}

BOOL CHintDialog::OnInitDialog() 
{
  BaseDialog::OnInitDialog();

  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  SetIcon(pApp->LoadIcon(IDR_MAINFRAME),TRUE);

  // Work out the size of buttons
  m_btnSize.SetRect(0,0,50,14);
  MapDialogRect(&m_btnSize);

  CRect& rPlace = pApp->GetHintsRect();
  if (rPlace.Width() > 0)
    MoveWindow(rPlace);
  LayoutControls();

  if (m_allHints != NULL)
  {
    m_currHint = 0;
    m_visibleHints = 0;
    LoadHintSet(0);
  }
  else
    m_currHint = -1;

  return TRUE;
}

void CHintDialog::OnSize(UINT nType, int cx, int cy)
{
  BaseDialog::OnSize(nType,cx,cy);
  if (GetDlgItem(IDOK)->GetSafeHwnd())
    LayoutControls();
}

void CHintDialog::OnGetMinMaxInfo(MINMAXINFO* lpMMI)
{
  // Set up the minimum size of the client area
  lpMMI->ptMinTrackSize.x = (2*m_btnSize.Width())+(3*8);
  lpMMI->ptMinTrackSize.y = (4*m_btnSize.Height())+(4*8);

  CRect clientSize, windowSize;
  GetClientRect(clientSize);
  GetWindowRect(windowSize);

  // Take account of the window border
  lpMMI->ptMinTrackSize.x += windowSize.Width()-clientSize.Width();
  lpMMI->ptMinTrackSize.y += windowSize.Height()-clientSize.Height();
}

void CHintDialog::OnPrevious() 
{
  if (m_currHint != -1)
    LoadHintSet(m_allHints[m_currHint].parent);
}

void CHintDialog::OnTopics() 
{
  if (m_currHint != -1)
    LoadHintSet(0);
}

void CHintDialog::OnShowHint() 
{
  UpdateHintList();
}

void CHintDialog::OnChangeHints() 
{
  if ((m_hintList.GetCurSel() == LB_ERR) ||
     ((m_allHints[m_currHint].nodetype != 1) && (m_visibleHints+1  == m_allHints[m_currHint].elcount) ))
    m_hintButton.EnableWindow(false);
  else
    m_hintButton.EnableWindow(true);
}

void CHintDialog::OnDblClkHints() 
{
  if (!((m_hintList.GetCurSel() == LB_ERR) ||
     ((m_allHints[m_currHint].nodetype != 1) && (m_visibleHints+1  == m_allHints[m_currHint].elcount) )))
    UpdateHintList();
}

/////////////////////////////////////////////////////////////////////////////
// Implementation of the CHintListBox control
/////////////////////////////////////////////////////////////////////////////

IMPLEMENT_DYNAMIC(CHintListBox, CListBox)

CHintListBox::CHintListBox()
{
}

CHintListBox::~CHintListBox()
{
}

BEGIN_MESSAGE_MAP(CHintListBox, CListBox)
END_MESSAGE_MAP()

void CHintListBox::SetItemHeights(void)
{
  CRect clientRect;
  GetClientRect(clientRect);
  clientRect.DeflateRect(2,0);

  HDC dc = ::GetDC(NULL);
  HFONT oldFont = (HFONT)::SelectObject(dc,GetFont()->GetSafeHandle());

  for (int i = 0; i < GetCount(); i++)
  {
    // Get the item text
    CString item;
    GetText(i,item);

    // Get the height of the text
    CRect r = clientRect;
    SetItemHeight(i,
      ::DrawText(dc,item,item.GetLength(),r,DT_LEFT|DT_WORDBREAK|DT_CALCRECT));
  }

  ::SelectObject(dc,oldFont);
  ::ReleaseDC(NULL,dc);
}

void CHintListBox::MeasureItem(LPMEASUREITEMSTRUCT lpMeasureItemStruct)
{
  if (lpMeasureItemStruct->itemID != -1)
  {
    HDC dc = ::GetDC(NULL);
    HFONT oldFont = (HFONT)::SelectObject(dc,GetFont()->GetSafeHandle());

    CString item;
    GetText(lpMeasureItemStruct->itemID,item);

    CRect clientRect;
    GetClientRect(clientRect);
    clientRect.DeflateRect(2,0);

    lpMeasureItemStruct->itemHeight =
      ::DrawText(dc,item,item.GetLength(),clientRect,DT_LEFT|DT_WORDBREAK|DT_CALCRECT);

    ::SelectObject(dc,oldFont);
    ::ReleaseDC(NULL,dc);
  }
}

void CHintListBox::DrawItem(LPDRAWITEMSTRUCT lpDrawItemStruct)
{
  CDC* dc = CDC::FromHandle(lpDrawItemStruct->hDC);
  if (lpDrawItemStruct->itemState & ODS_SELECTED)
  {
    dc->FillSolidRect(&lpDrawItemStruct->rcItem,::GetSysColor(COLOR_HIGHLIGHT));
    dc->SetTextColor(::GetSysColor(COLOR_HIGHLIGHTTEXT));
  }
  else
  {
    dc->FillSolidRect(&lpDrawItemStruct->rcItem,::GetSysColor(COLOR_WINDOW));
    dc->SetTextColor(::GetSysColor(COLOR_WINDOWTEXT));
  }

  if (lpDrawItemStruct->itemID != -1)
  {
    CString item;
    GetText(lpDrawItemStruct->itemID,item);

    CRect textRect(lpDrawItemStruct->rcItem);
    textRect.DeflateRect(2,0);
    dc->DrawText(item,textRect,DT_LEFT|DT_WORDBREAK);
  }

  if (lpDrawItemStruct->itemState & ODS_FOCUS)
    dc->DrawFocusRect(&lpDrawItemStruct->rcItem);
}
