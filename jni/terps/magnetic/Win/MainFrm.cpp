/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// MainFrm.cpp: Implementation of the frame class
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"

#include "Magnetic.h"
#include "MagneticDoc.h"
#include "MagneticView.h"
#include "MainFrm.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// Implementation of CMainFrame
/////////////////////////////////////////////////////////////////////////////

IMPLEMENT_DYNCREATE(CMainFrame, MenuBarFrameWnd)

BEGIN_MESSAGE_MAP(CMainFrame, MenuBarFrameWnd)
  //{{AFX_MSG_MAP(CMainFrame)
  ON_WM_CREATE()
  ON_COMMAND(ID_HELP, OnHelpFinder)
  ON_WM_PALETTECHANGED()
  ON_WM_QUERYNEWPALETTE()
  //}}AFX_MSG_MAP
END_MESSAGE_MAP()

static UINT indicators[] =
{
  ID_SEPARATOR,           // status line indicator
  ID_INDICATOR_CAPS,
  ID_INDICATOR_NUM,
};

CMainFrame::CMainFrame()
{
}

CMainFrame::~CMainFrame()
{
}

int CMainFrame::OnCreate(LPCREATESTRUCT lpCreateStruct)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  if (MenuBarFrameWnd::OnCreate(lpCreateStruct) == -1)
    return -1;
  if (!CreateBar(IDR_MAINFRAME,IDB_TOOLBAR32))
    return -1;

  if (!m_statusBar.Create(this) ||
      !m_statusBar.SetIndicators(indicators,sizeof(indicators)/sizeof(UINT)))
  {
    return -1;
  }

  BOOL bToolBar, bStatusBar;
  pApp->GetControlBars(bToolBar,bStatusBar);
  ShowControlBar(&m_toolBar,bToolBar,TRUE);
  ShowControlBar(&m_statusBar,bStatusBar,TRUE);
  return 0;
}

BOOL CMainFrame::DestroyWindow() 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  // Save the window position
  WINDOWPLACEMENT Place;
  GetWindowPlacement(&Place);

  int& iMax = pApp->GetWindowMax();
  CRect& rPlace = pApp->GetWindowRect();

  iMax = (Place.showCmd == SW_SHOWMAXIMIZED);
  rPlace = Place.rcNormalPosition;

  BOOL bToolBar = m_toolBar.GetStyle() & WS_VISIBLE;
  BOOL bStatusBar = m_statusBar.GetStyle() & WS_VISIBLE;
  pApp->SetControlBars(bToolBar,bStatusBar);

  return MenuBarFrameWnd::DestroyWindow();
}

BOOL CMainFrame::PreCreateWindow(CREATESTRUCT& cs)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  cs.style &= ~FWS_ADDTOTITLE;

  CRect& rPlace = pApp->GetWindowRect();
  if (rPlace.Width() > 0)
  {
    cs.x = rPlace.left;
    cs.y = rPlace.top;
    cs.cx = rPlace.Width();
    cs.cy = rPlace.Height();
  }

  return MenuBarFrameWnd::PreCreateWindow(cs);
}

void CMainFrame::OnPaletteChanged(CWnd*) 
{
  CMagneticView* pView = CMagneticView::GetView();
  if (pView == NULL)
    return;

  CDC* pDC = GetDC();
  pView->GetPicture().SetPalette(pDC,this);
  ReleaseDC(pDC);
}

BOOL CMainFrame::OnQueryNewPalette() 
{
  CMagneticView* pView = CMagneticView::GetView();
  if (pView == NULL)
    return 0;

  CDC* pDC = GetDC();
  int iColours = pView->GetPicture().SetPalette(pDC,this);
  ReleaseDC(pDC);
  return iColours;
}

#ifdef _DEBUG
void CMainFrame::AssertValid() const
{
  MenuBarFrameWnd::AssertValid();
}

void CMainFrame::Dump(CDumpContext& dc) const
{
  MenuBarFrameWnd::Dump(dc);
}

#endif //_DEBUG
