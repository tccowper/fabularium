/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// MagneticView.cpp: Implementation of the view class
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"
#include <stdio.h>
#include <string.h>

#include "Magnetic.h"
#include "MagneticDoc.h"
#include "MagneticView.h"
#include "MagneticTitle.h"
#include "Dialogs.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

#pragma warning(disable : 4310)

/////////////////////////////////////////////////////////////////////////////
// Implementation of CMagneticView
/////////////////////////////////////////////////////////////////////////////

// Class constants
const int CMagneticView::SPECIAL_KEYS = 128;
const int CMagneticView::MAX_LINES = 64;

IMPLEMENT_DYNCREATE(CMagneticView, CView)

BEGIN_MESSAGE_MAP(CMagneticView, CView)
  //{{AFX_MSG_MAP(CMagneticView)
  ON_WM_CHAR()
  ON_WM_KEYDOWN()
  ON_WM_DESTROY()
  ON_WM_SIZE()
  ON_WM_CREATE()
  ON_COMMAND(ID_FILE_RECORD, OnRecord)
  ON_COMMAND(ID_FILE_PLAYBACK, OnPlayback)
  ON_COMMAND(ID_FILE_SCRIPT, OnScript)
  ON_UPDATE_COMMAND_UI(ID_FILE_RECORD, OnUpdateRecord)
  ON_UPDATE_COMMAND_UI(ID_FILE_PLAYBACK, OnUpdatePlayback)
  ON_UPDATE_COMMAND_UI(ID_FILE_SCRIPT, OnUpdateScript)
  ON_UPDATE_COMMAND_UI(ID_FILE_OPEN, OnUpdateFileOpen)
  ON_UPDATE_COMMAND_UI(ID_VIEW_FONT, OnUpdateViewFont)
  ON_COMMAND(ID_VIEW_SCROLLBACK, OnScrollback)
  ON_COMMAND(ID_EDIT_PASTE, OnEditPaste)
  ON_UPDATE_COMMAND_UI(ID_VIEW_OPTIONS, OnUpdateViewOptions)
  ON_WM_ERASEBKGND()
  ON_WM_TIMER()
  ON_UPDATE_COMMAND_UI(ID_VIEW_SCROLLBACK, OnUpdateScrollback)
  ON_COMMAND(ID_TOGGLE_GFX, OnToggleGfx)
  //}}AFX_MSG_MAP
END_MESSAGE_MAP()

CMagneticView::CMagneticView() : m_PicWnd(m_Picture,m_AnimFrames),
  m_strRecName("Magnetic.rec"), m_strScrName("Magnetic.scr"), m_strFileName("Magnetic.sav")
{
  m_pOldFont = NULL;
  m_pTextFont = NULL;
  m_pTextDC = NULL;
  m_pFileRecord = NULL;
  m_pFileScript = NULL;
  m_bMorePrompt = false;
  m_bStatusBar = false;
  m_bAnimate = false;
}

CMagneticView::~CMagneticView()
{
  ClearAll();
}

BOOL CMagneticView::PreCreateWindow(CREATESTRUCT& cs)
{
  ClearAll();
  return CView::PreCreateWindow(cs);
}


int CMagneticView::OnCreate(LPCREATESTRUCT lpCreateStruct) 
{
  if (CView::OnCreate(lpCreateStruct) == -1)
    return -1;

  TextSetup();
  m_iTimer = SetTimer(1,100,NULL);

  return 0;
}

void CMagneticView::OnDestroy() 
{
  KillTimer(m_iTimer);
  TextClearup();

  CView::OnDestroy();
}

void CMagneticView::OnDraw(CDC* pDrawDC)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  // Don't draw anything if there isn't a loaded game
  if (pApp->GetGameLoaded() == 0)
    return;

  CDC BitmapDC;
  BitmapDC.CreateCompatibleDC(pDrawDC);

  CRect Client;
  GetClientRect(Client);

  CBitmap Bitmap;
  CSize Size(Client.Width(),Client.Height());
  Bitmap.CreateCompatibleBitmap(pDrawDC,Size.cx,Size.cy);
  CBitmap* pOldBitmap = BitmapDC.SelectObject(&Bitmap);

  // Set up the font
  CFont* pOldFont = NULL;
  TEXTMETRIC FontInfo;

  pOldFont = BitmapDC.SelectObject(m_pTextFont);
  BitmapDC.GetTextMetrics(&FontInfo);
  int iFontHeight = (int)(FontInfo.tmHeight*1.1);
  int iPicWidth = 0;
  int iPicHeight = 0;
  CRect TextClient(Client);

  if (m_bStatusBar)
  {
    TextClient.top += iFontHeight;

    // Display the status line
    LONG lStatSpace = (LONG)(Client.Width()*0.075);
    SolidRect(&BitmapDC,CRect(Client.left,0,Client.right,iFontHeight),
      pApp->GetForeColour());

    BitmapDC.SetTextColor(pApp->GetBackColour());
    BitmapDC.SetBkColor(pApp->GetForeColour());
    
    BitmapDC.TextOut(lStatSpace,0,m_strStatLocation,m_strStatLocation.GetLength());
    BitmapDC.TextOut(lStatSpace*10,0,m_strStatScore,m_strStatScore.GetLength());
  }

  // If the picture is to be drawn in the main window, leave enough
  // space and draw both background and picture
  if (pApp->GetShowGraphics() == CMagneticApp::ShowGraphics::MainWindow)
  {
    int iOffset = m_bStatusBar ? iFontHeight : 0;
    iPicWidth = m_Picture.GetScaledWidth();
    iPicHeight = m_Picture.GetScaledHeight();
    if (iPicWidth > 0 && iPicHeight > 0)
    {
      TextClient.top += iPicHeight;

      BitmapDC.FillSolidRect(Client.left,Client.top+iOffset,Client.Width(),
        iPicHeight,pApp->GetGfxColour());
      CRect PicArea((Client.Width()-iPicWidth)/2,iOffset,
        (Client.Width()+iPicWidth)/2,iOffset+iPicHeight);
      m_Picture.Paint(&BitmapDC,PicArea,m_AnimFrames);
    }
  }

  BitmapDC.FillSolidRect(TextClient,pApp->GetBackColour());
  int y = TextClient.top;

  // Repaginate if necessary
  TrimOutput();
  if (m_PageTable.GetSize() == 0)
  {
    Paginate(&BitmapDC,0,0);
    if (m_bMorePrompt)
      m_PageTable.RemoveAt(m_PageTable.GetSize()-1);
  }
  LPCSTR lpszOutput = m_strOutput;

  BitmapDC.SetTextColor(pApp->GetForeColour());
  BitmapDC.SetBkColor(pApp->GetBackColour());

  // Work out the number of lines of text to draw
  m_iMaxLines = (TextClient.Height()-(2*pApp->GetMargins().cy)) / iFontHeight;
  y += pApp->GetMargins().cy;
  
  // Starting position in the text output buffer
  int i = m_PageTable.GetSize() - m_iMaxLines - 1;

  // Adjust for a More... prompt
  int offset = 0;
  if (m_bMorePrompt)
  {
    if (iPicWidth > 0 && iPicHeight > 0)
    {
      if (m_iLines > m_iMaxLines)
      {
        if (m_iLines - m_iMaxLines - 1 > 0)
        {
          offset = m_iLines - m_iMaxLines - 1;
          i -= offset;
        }
      }
    }
  }

  if (i < 0)
    i = 0;

  // Draw the text
  int iCount = 0;
  while (i < m_PageTable.GetSize()-1-offset)
  {
    iCount = m_PageTable[i+1]-m_PageTable[i]-1;
    if (iCount > 0)
    {
      if (*(lpszOutput+m_PageTable[i]+iCount-1) == '\0')
        iCount--;
    }
    BitmapDC.TextOut(pApp->GetMargins().cx,y,lpszOutput+m_PageTable[i],iCount);
    y += iFontHeight;
    i++;
  }

  // Store information on the last line for updating later
  CRect LastLine(0,y-iFontHeight,TextClient.Width(),y);
  m_LastLineRect = LastLine;

  // Clear the end of the last line to allow input editing
  if (i > 0)
  {
    CSize TextLen = BitmapDC.GetTextExtent(lpszOutput+m_PageTable[i-1],
      m_PageTable[i]-m_PageTable[i-1]-1);
    LastLine.left += TextLen.cx + pApp->GetMargins().cx;

    SolidRect(&BitmapDC,LastLine,pApp->GetBackColour());
  }

  // Remove the font
  BitmapDC.SelectObject(pOldFont);

  pDrawDC->BitBlt(0,0,Size.cx,Size.cy,&BitmapDC,0,0,SRCCOPY);
  BitmapDC.SelectObject(pOldBitmap);
}

void CMagneticView::OnChar(UINT nChar, UINT nRepCnt, UINT nFlags) 
{
  switch (nChar)
  {
  case ' ':
    // Use a special character for space to make sure that Repaginate()
    // doesn't split the input line.
    nChar = SPECIAL_KEYS + VK_SPACE;
    break;
  case 13:
    nChar = 10;
    break;
  }  

  // Add to the input buffer
  m_Input.Add(nChar);
  CView::OnChar(nChar, nRepCnt, nFlags);
}

void CMagneticView::OnKeyDown(UINT nChar, UINT nRepCnt, UINT nFlags) 
{
  switch (nChar)
  {
  case VK_LEFT:    // Cursor left
  case VK_RIGHT:  // Cursor right
  case VK_UP:      // Cursor up
  case VK_DOWN:    // Cursor down
  case VK_HOME:    // Home
  case VK_END:    // End
  case VK_DELETE:  // Delete
    m_Input.Add(nChar + SPECIAL_KEYS);
    break;
  }
  CView::OnKeyDown(nChar, nRepCnt, nFlags);
}

void CMagneticView::OnSize(UINT nType, int cx, int cy) 
{
  if (m_bMorePrompt == false)
  {
    // Clear pagination
    m_PageTable.RemoveAll();
    CView::OnSize(nType, cx, cy);
  }
}

void CMagneticView::OnTimer(UINT nIDEvent) 
{
  Animate();
  CView::OnTimer(nIDEvent);
}

void CMagneticView::OnRecord() 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  SimpleFileDialog RecordDlg(FALSE,NULL,m_strRecName,OFN_HIDEREADONLY|OFN_ENABLESIZING,
    "Record Files (*.rec)|*.rec|All Files (*.*)|*.*||",this);
  RecordDlg.m_ofn.lpstrTitle = "Record Input File";

  switch (m_Recording)
  {
  case Recording::RecordingOff:
    if (ms_is_running())
      pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
    if (RecordDlg.DoModal() == IDOK)
    {
      m_strRecName = RecordDlg.GetPathName();
      if ((m_pFileRecord = fopen(m_strRecName,"wt")) != NULL)
        m_Recording = Recording::RecordingOn;
    }
    break;
  case Recording::RecordingOn:
    if (m_pFileRecord)
      fclose(m_pFileRecord);
    m_pFileRecord = NULL;
    m_Recording = Recording::RecordingOff;
    break;
  case Recording::PlaybackOn:
    break;
  }
}

void CMagneticView::OnPlayback() 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  SimpleFileDialog PlayDlg(TRUE,NULL,m_strRecName,
    OFN_FILEMUSTEXIST|OFN_HIDEREADONLY|OFN_ENABLESIZING,
    "Record Files (*.rec)|*.rec|All Files (*.*)|*.*||",this);
  PlayDlg.m_ofn.lpstrTitle = "Play Back a File";

  switch (m_Recording)
  {
  case Recording::RecordingOff:
    if (ms_is_running())
      pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
    if (PlayDlg.DoModal() == IDOK)
    {
      m_strRecName = PlayDlg.GetPathName();
      if ((m_pFileRecord = fopen(m_strRecName,"rt")) != NULL)
      {
        m_Recording = Recording::PlaybackOn;
        if (pApp->GetGameLoaded() && ms_is_running())
          pApp->SetRedrawStatus(CMagneticApp::Redraw::EndPlayback);
      }
    }
    break;
  case Recording::RecordingOn:
    break;
  case Recording::PlaybackOn:
    if (m_pFileRecord)
      fclose(m_pFileRecord);
    m_pFileRecord = NULL;
    m_Recording = Recording::RecordingOff;
    break;
  }
}

void CMagneticView::OnScript() 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  SimpleFileDialog ScriptDlg(FALSE,NULL,m_strScrName,OFN_HIDEREADONLY|OFN_ENABLESIZING,
    "Script Files (*.scr)|*.scr|All Files (*.*)|*.*||",this);
  ScriptDlg.m_ofn.lpstrTitle = "Scripting";

  switch (m_Scripting)
  {
  case Scripting::ScriptingOff:
    if (ms_is_running())
      pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
    if (ScriptDlg.DoModal() == IDOK)
    {
      m_strScrName = ScriptDlg.GetPathName();
      if ((m_pFileScript = fopen(m_strScrName,"wt")) != NULL)
        m_Scripting = Scripting::ScriptingOn;
    }
    break;
  case Scripting::ScriptingOn:
    if (m_pFileScript)
      fclose(m_pFileScript);
    m_pFileScript = NULL;
    m_Scripting = Scripting::ScriptingOff;
    break;
  }
}

void CMagneticView::OnScrollback() 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
  AfxGetMainWnd()->GetWindowRect(m_Scrollback.GetRect());
  m_Scrollback.DoModal();
}

void CMagneticView::OnEditPaste() 
{
  if (OpenClipboard())
  {
    // Get text from the clipboard
    LPCTSTR pszText = (LPCTSTR)GetClipboardData(CF_TEXT);
    if (pszText)
    {
      while (*pszText != '\0')
      {
        int c = (int)*(pszText++);
        switch (c)
        {
        case ' ':
          c = SPECIAL_KEYS + VK_SPACE;
          break;
        case 13:
        case 10:
          c = 0;
          break;
        }  
        // Add to the input buffer
        if (c)
          m_Input.Add(c);
      }
    }
    CloseClipboard();
  }
}

BOOL CMagneticView::OnEraseBkgnd(CDC* pDC) 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  CRect Background;
  GetClientRect(Background);

  if (pApp->GetGameLoaded() == 0)
    pDC->FillSolidRect(Background,pApp->GetBackColour());

  return 1;
}

void CMagneticView::OnToggleGfx() 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  if (GetFocus() == this)
  {
    if (m_bMorePrompt == false)
    {
      switch (pApp->GetShowGraphics())
      {
      case CMagneticApp::ShowGraphics::NoGraphics:
        pApp->SetShowGraphics(CMagneticApp::ShowGraphics::SeparateWindow);
        break;
      case CMagneticApp::ShowGraphics::SeparateWindow:
        pApp->SetShowGraphics(CMagneticApp::ShowGraphics::MainWindow);
        break;
      case CMagneticApp::ShowGraphics::MainWindow:
        pApp->SetShowGraphics(CMagneticApp::ShowGraphics::NoGraphics);
        break;
      }
      pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
      SetPictureWindowState();
    }
  }
}

/////////////////////////////////////////////////////////////////////////////
// Update handlers
/////////////////////////////////////////////////////////////////////////////

void CMagneticView::OnUpdateRecord(CCmdUI* pCmdUI) 
{
  switch (m_Recording)
  {
  case Recording::RecordingOff:
    pCmdUI->Enable(m_bMorePrompt == false);
    pCmdUI->SetCheck(0);
    break;
  case Recording::RecordingOn:
    pCmdUI->Enable(m_bMorePrompt == false);
    pCmdUI->SetCheck(1);
    break;
  case Recording::PlaybackOn:
    pCmdUI->Enable(FALSE);
    pCmdUI->SetCheck(0);
    break;
  }
}

void CMagneticView::OnUpdatePlayback(CCmdUI* pCmdUI) 
{
  switch (m_Recording)
  {
  case Recording::RecordingOff:
    pCmdUI->Enable(m_bMorePrompt == false);
    pCmdUI->SetCheck(0);
    break;
  case Recording::RecordingOn:
    pCmdUI->Enable(FALSE);
    pCmdUI->SetCheck(0);
    break;
  case Recording::PlaybackOn:
    pCmdUI->Enable(m_bMorePrompt == false);
    pCmdUI->SetCheck(1);
    break;
  }
}

void CMagneticView::OnUpdateScript(CCmdUI* pCmdUI) 
{
  pCmdUI->Enable(m_bMorePrompt == false);
  switch (m_Scripting)
  {
  case Scripting::ScriptingOff:
    pCmdUI->SetCheck(0);
    break;
  case Scripting::ScriptingOn:
    pCmdUI->SetCheck(1);
    break;
  }
}

void CMagneticView::OnUpdateFileOpen(CCmdUI* pCmdUI) 
{
  pCmdUI->Enable(m_bMorePrompt == false);
}

void CMagneticView::OnUpdateViewFont(CCmdUI* pCmdUI) 
{
  pCmdUI->Enable(m_bMorePrompt == false);
}

void CMagneticView::OnUpdateViewOptions(CCmdUI* pCmdUI) 
{
  pCmdUI->Enable(m_bMorePrompt == false);
}

void CMagneticView::OnUpdateScrollback(CCmdUI* pCmdUI) 
{
  pCmdUI->Enable(m_bMorePrompt == false);
}

#ifdef _DEBUG
void CMagneticView::AssertValid() const
{
  CView::AssertValid();
}

void CMagneticView::Dump(CDumpContext& dc) const
{
  CView::Dump(dc);
}

CMagneticDoc* CMagneticView::GetDocument()
{
  ASSERT(m_pDocument->IsKindOf(RUNTIME_CLASS(CMagneticDoc)));
  return (CMagneticDoc*)m_pDocument;
}
#endif //_DEBUG

/////////////////////////////////////////////////////////////////////////////
// Support functions for the interpreter
/////////////////////////////////////////////////////////////////////////////

CMagneticView* CMagneticView::GetView(void)
{
  CFrameWnd* pFrame = (CFrameWnd*)(AfxGetApp()->m_pMainWnd);
  if (pFrame == NULL)
    return NULL;
  CView* pView = pFrame->GetActiveView();
  if (pView == NULL)
    return NULL;

  // Fail if view is of wrong kind
  if (!pView->IsKindOf(RUNTIME_CLASS(CMagneticView)))
    return NULL;

  return (CMagneticView*)pView;
}

void CMagneticView::SolidRect(CDC* pDC, LPCRECT lpRect, COLORREF Colour)
{
  CPen NewPen(PS_SOLID,1,Colour);
  CPen* pOldPen = pDC->SelectObject(&NewPen);

  CBrush NewBrush(Colour);
  CBrush* pOldBrush = pDC->SelectObject(&NewBrush);

  pDC->Rectangle(lpRect);

  pDC->SelectObject(pOldPen);
  pDC->SelectObject(pOldBrush);
}

void CMagneticView::ClearAll(void)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  m_strOutput.Empty();
  m_strStatLocation.Empty();
  m_strStatScore.Empty();
  m_strStatCurrent.Empty();
  m_Scrollback.GetScrollback().Empty();
  m_PageTable.RemoveAll();
  m_Input.RemoveAll();
  m_History.RemoveAll();
  m_iLines = 0;
  m_iMaxLines = 0;
  m_bStatusBar = false;
  m_bAnimate = false;

  pApp->SetGameLoaded(0);

  m_Recording = Recording::RecordingOff;
  if (m_pFileRecord)
    fclose(m_pFileRecord);
  m_pFileRecord = NULL;
  m_Scripting = Scripting::ScriptingOff;
  if (m_pFileScript)
    fclose(m_pFileScript);
  m_pFileScript = NULL;
  m_strScript.Empty();

  if (m_PicWnd.GetSafeHwnd())
    m_PicWnd.SendMessage(WM_CLOSE,0,0);
  m_Picture.ClearAll();
  SetAnimate(FALSE);
  ClearAnims();
}

void CMagneticView::TrimOutput(void)
{
  // Remove old output
  if (m_PageTable.GetSize() > MAX_LINES * 4)
  {
    int iTrim = m_PageTable.GetSize() - MAX_LINES;
    if (iTrim > 0)
    {
      while (iTrim > 0)
      {
        m_PageTable.RemoveAt(0);
        iTrim--;
      }
      m_strOutput =
        m_strOutput.Right(m_strOutput.GetLength() - m_PageTable[0]);
      m_PageTable.RemoveAll();
    }
  }
}

int CMagneticView::Paginate(CDC* pDC, int p1, int p2)
{
  int iNewLines = 0;

  // Clear previous pagination
  if (p1+p2 == 0)
    m_PageTable.RemoveAll();

  LPCSTR lpszOutput = m_strOutput;
  int iOutSize = m_strOutput.GetLength();
  char c;

  while (p1+p2 < iOutSize)
  {
    c = lpszOutput[p1+p2];

    // Break line where possible
    switch (c)
    {
    case 10:
      if (LineFull(pDC,lpszOutput+p1,p2))
        p2 = FindPreviousSpace(lpszOutput+p1,p2-1);
      p1 += p2+1;
      p2 = 0;
      m_PageTable.Add(p1);
      iNewLines++;
      break;
    case ' ':
      if (LineFull(pDC,lpszOutput+p1,p2))
      {
        p2 = FindPreviousSpace(lpszOutput+p1,p2-1);
        p1 += p2+1;
        p2 = 0;
        m_PageTable.Add(p1);
        iNewLines++;
      }
      else
        p2++;
      break;
    default:
      p2++;
      break;
    }
  }

  // Add the last line
  if (p1+p2 > iOutSize)
    m_PageTable.Add(iOutSize+1);
  else
    m_PageTable.Add(p1+p2+1);

  return iNewLines;
}

void CMagneticView::ClearPagination(void)
{
  m_PageTable.RemoveAll();
  m_iLines = 0;
}

int CMagneticView::FindPreviousSpace(LPCSTR lpszText, int iPos)
{
  if (iPos < 0)
    return 0;

  int iNewPos = iPos;

  // Find previous space character, if there is one
  while (lpszText[iNewPos] != ' ')
  {
    if (iNewPos > 0)
      iNewPos--;
    else
    {
      iNewPos = iPos;
      break;
    }
  }
  return iNewPos;
}

BOOL CMagneticView::LineFull(CDC* pDC, LPCSTR lpszText, int iLength)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  CSize TextLen = pDC->GetTextExtent(lpszText,iLength);
  CRect Client;
  GetClientRect(Client);

  return (TextLen.cx >= (Client.Width()-(2*pApp->GetMargins().cx))) ? 1 : 0;
}

void CMagneticView::AddOutChar(char c)
{
  static const int SCRIPT_WIDTH = 70;

  if (c == '\b') // Is this a delete character?
  {
    int len;

    len = m_strOutput.GetLength();
    if (len > 0)
      m_strOutput = m_strOutput.Left(len-1);

    len = m_Scrollback.GetScrollback().GetLength();
    if (len > 0)
    {
      m_Scrollback.GetScrollback() =
        m_Scrollback.GetScrollback().Left(len-1);
    }

    if (m_Scripting == Scripting::ScriptingOn)
    {
      len = m_strScript.GetLength();
      if (len > 0)
        m_strScript = m_strScript.Left(len-1);
    }
  }
  else if (isprint(c) || (c == '\n'))
  {
    m_strOutput += c;

    // Update pagination information
    if (m_PageTable.GetSize() > 1)
    {
      int p1, p2;

      p1 = m_PageTable[m_PageTable.GetSize()-2];
      p2 = m_PageTable[m_PageTable.GetSize()-1]-p1-1;
      m_PageTable.RemoveAt(m_PageTable.GetSize()-1);
      m_iLines += Paginate(m_pTextDC,p1,p2);
    }
    else
      m_iLines += Paginate(m_pTextDC,0,0);

    if ((m_iLines > m_iMaxLines) && (m_Recording != Recording::PlaybackOn))
    {
      CFrameWnd* pFrame = (CFrameWnd*)(AfxGetApp()->m_pMainWnd);
      if (pFrame)
      {
        pFrame->SetMessageText("Press a key for more...");
        pFrame->DragAcceptFiles(FALSE);
      }

      m_bMorePrompt = true;
      bool done;
      GetInput(done,false);

      // If the window has been closed, at this point even this view
      // object may be invalid. Check that the window is still present,
      // and if not, return without changing anything at all.
      pFrame = (CFrameWnd*)(AfxGetApp()->m_pMainWnd);
      if (pFrame == NULL)
        return;  // Program ending

      m_bMorePrompt = false;
      pFrame->SetMessageText(AFX_IDS_IDLEMESSAGE);
      pFrame->DragAcceptFiles(TRUE);
    }

    if (c == '\n')
      m_Scrollback.GetScrollback() += '\r';
    m_Scrollback.GetScrollback() += c;

    if (m_Scripting == Scripting::ScriptingOn)
    {
      switch (c)
      {
      case 10:
        if (m_pFileScript)
          fprintf(m_pFileScript,"%s\n",m_strScript);
        m_strScript.Empty();
        break;
      case ' ':
        if (m_strScript.GetLength() > SCRIPT_WIDTH)
        {
          if (m_pFileScript)
            fprintf(m_pFileScript,"%s\n",m_strScript);
          m_strScript.Empty();
        }
        else
          m_strScript += c;
        break;
      default:
        m_strScript += c;
        break;
      }
    }
  }
}

void CMagneticView::AddStatChar(char c)
{
  m_bStatusBar = true;
  switch (c)
  {
  case 9:
    m_strStatLocation = m_strStatCurrent;
    m_strStatCurrent.Empty();
    break;
  case 10:
    m_strStatScore = m_strStatCurrent;
    m_strStatCurrent.Empty();
    break;
  default:
    m_strStatCurrent += c;
    break;
  }
}

void CMagneticView::SetCursorPos(CDC* pDC, int iRight)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  int i = m_PageTable.GetSize()-2;  // Start of the last visible line
  int offset = m_iLines-m_iMaxLines-1;
  if (offset > 0)
    i -= offset;

  if (i >= 0)
  {
    CSize TextLen = pDC->GetTextExtent((LPCTSTR)m_strOutput+m_PageTable[i],
      m_PageTable[i+1]-m_PageTable[i]-1-iRight);

    // Set the caret position
    SetCaretPos(CPoint(TextLen.cx+pApp->GetMargins().cx,m_LastLineRect.top));
  }
}

void CMagneticView::InsertChar(CString& strInsert, char cChar, int iPos, int iIsOut)
{
  // Get access to the buffer with space for one more character
  int iLength = strInsert.GetLength();
  LPSTR lpszBuffer = strInsert.GetBuffer(iLength+1);

  // Shift the right hand characters up one position
  int i;
  for (i = iLength+1; i > iLength-iPos; i--)
    lpszBuffer[i] = lpszBuffer[i-1];

  // Add the new character
  lpszBuffer[i] = cChar;

  // Back to a CString
  strInsert.ReleaseBuffer();

  if (iIsOut && m_PageTable.GetSize() > 0)
    m_PageTable[m_PageTable.GetSize()-1] = m_PageTable[m_PageTable.GetSize()-1] + 1;
}

void CMagneticView::RemoveChar(CString& strRemove, int iPos, int iIsOut)
{
  // Get access to the buffer
  int iLength = strRemove.GetLength();
  LPSTR lpszBuffer = strRemove.GetBuffer(iLength);

  // Shift the characters down one position
  for (int i = iLength-iPos; i < iLength; i++)
    lpszBuffer[i] = lpszBuffer[i+1];

  // Back to a CString
  strRemove.ReleaseBuffer();

  if (iIsOut && m_PageTable.GetSize() > 0)
    m_PageTable[m_PageTable.GetSize()-1] = m_PageTable[m_PageTable.GetSize()-1] - 1;
}

void CMagneticView::TextSetup(void)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  if (m_pTextDC)
    return;

  // Set up a device context for font information
  m_pTextDC = new CPaintDC(this);

  // Create the font
  m_pTextFont = new CFont();
  m_pTextFont->CreateFontIndirect(pApp->GetLogFont());
  m_pOldFont = m_pTextDC->SelectObject(m_pTextFont);
}

void CMagneticView::TextClearup(void)
{
  m_PageTable.RemoveAll();

  if (m_pTextDC && m_pOldFont)
    m_pTextDC->SelectObject(m_pOldFont);
  if (m_pTextFont)
    delete m_pTextFont;
  if (m_pTextDC)
    delete m_pTextDC;

  m_pOldFont = NULL;
  m_pTextFont = NULL;
  m_pTextDC = NULL;
}

void CMagneticView::UseHistory(CString& strNewInput, int iOldLength)
{
  m_strOutput = m_strOutput.Left(m_strOutput.GetLength()-iOldLength);
  m_strOutput += strNewInput;

  if (m_PageTable.GetSize() > 0)
  {
    m_PageTable[m_PageTable.GetSize()-1] =
      m_PageTable[m_PageTable.GetSize()-1] +
      strNewInput.GetLength() - iOldLength;
  }
}

void CMagneticView::Animate(void)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  if (m_bAnimate && (pApp->GetShowGraphics() != CMagneticApp::ShowGraphics::NoGraphics))
  {
    struct ms_position * Positions;
    type16 Count, Width, Height;
    type8 * pMask;
    
    if (ms_animate(&Positions,&Count) == 0)
    {
      m_bAnimate = false;
      return;
    }

    ClearAnims();
    for (int i = 0; i < Count; i++)
    {
      type8* pPictureData = ms_get_anim_frame(Positions[i].number,&Width,&Height,&pMask);
      if (pPictureData)
      {
        CMagneticPic* pFrame = new CMagneticPic();
        pFrame->NewPicture(Width,Height,pPictureData,m_Palette);
        pFrame->SetOrigin(CPoint(Positions[i].x,Positions[i].y));
        pFrame->SetMask(pMask);
        m_AnimFrames.Add(pFrame);
      }
    }

    switch (pApp->GetShowGraphics())
    {
    case CMagneticApp::ShowGraphics::SeparateWindow:
      if (m_PicWnd.GetSafeHwnd())
        m_PicWnd.Invalidate();
      break;
    case CMagneticApp::ShowGraphics::MainWindow:
      Invalidate();
      break;
    }
  }
}

void CMagneticView::ClearAnims(void)
{
  for (int i = 0; i < m_AnimFrames.GetSize(); i++)
    delete m_AnimFrames[i];
  m_AnimFrames.RemoveAll();
}

void CMagneticView::SetPictureWindowState(void)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  if (pApp->GetShowGraphics() == CMagneticApp::ShowGraphics::SeparateWindow)
  {
    if ((m_PicWnd.GetSafeHwnd() == NULL) && (m_Picture.IsValid()))
    {
      CRect WindowArea(pApp->GetPicTopLeft(),CSize(0,0));
      if (m_PicWnd.CreatePicWnd(this,WindowArea))
        SetFocus();
    }
    m_PicWnd.Update();
  }
  else
  {
    if (m_PicWnd.GetSafeHwnd())
      m_PicWnd.SendMessage(WM_CLOSE,0,0);
  }
}

void CMagneticView::PlayMusic(unsigned char* pMidiData, int iLength, int iTempo)
{
  if (pMidiData == NULL)
  {
    if (m_Perform != NULL)
      m_Perform->Stop(NULL,NULL,0,0);
    if (m_Segment != NULL)
    {
      m_Segment->SetParam(GUID_Unload,0xFFFFFFFF,0,0,(void*)m_Perform);
      m_Segment.Release();
    }
    return;
  }

  if (m_Perform == NULL)
  {
    if (FAILED(CoCreateInstance(CLSID_DirectMusicPerformance,NULL,CLSCTX_ALL,
      IID_IDirectMusicPerformance,(void**)&m_Perform)))
      return;
    if (FAILED(m_Perform->Init(NULL,NULL,0)))
      return;
    if (FAILED(m_Perform->AddPort(NULL)))
      return;
  }
  else
    m_Perform->Stop(NULL,NULL,0,0);

  if (m_Segment != NULL)
  {
    m_Segment->SetParam(GUID_Unload,0xFFFFFFFF,0,0,(void*)m_Perform);
    m_Segment.Release();
  }

  CComPtr<IDirectMusicLoader> loader;
  if (FAILED(CoCreateInstance(CLSID_DirectMusicLoader,NULL,CLSCTX_ALL,
    IID_IDirectMusicLoader,(LPVOID*)&loader)))
    return;

  DMUS_OBJECTDESC obj;
  ZeroMemory(&obj,sizeof obj);
  obj.dwSize = sizeof obj;
  obj.dwValidData = DMUS_OBJ_CLASS|DMUS_OBJ_MEMORY;
  obj.guidClass = CLSID_DirectMusicSegment;
  obj.pbMemData = pMidiData;
  obj.llMemLength = iLength;
  if (FAILED(loader->GetObject(&obj,IID_IDirectMusicSegment,(void**)&m_Segment)))
    return;

  m_Segment->SetParam(GUID_StandardMIDIFile,0xFFFFFFFF,0,0,NULL);
  if (FAILED(m_Segment->SetParam(GUID_Download,0xFFFFFFFF,0,0,(void*)m_Perform)))
    return;

  DMUS_TEMPO_PARAM tempo;
  ZeroMemory(&tempo,sizeof tempo);
  tempo.dblTempo = (double)iTempo;
  m_Segment->SetParam(GUID_TempoParam,0xFFFFFFFF,0,0,&tempo);

  m_Perform->PlaySegment(m_Segment,0,0,NULL);
}

CArray<int,int>& CMagneticView::GetPageTable(void)
{
  return m_PageTable;
}

CString& CMagneticView::GetFileName(void)
{
  return m_strFileName;
}

CPictureWnd& CMagneticView::GetPictureWindow(void)
{
  return m_PicWnd;
}

CMagneticPic& CMagneticView::GetPicture(void)
{
  return m_Picture;
}

unsigned short* CMagneticView::GetPalette(void)
{
  return m_Palette;
}

void CMagneticView::SetAnimate(BOOL bAnim)
{
  m_bAnimate = bAnim ? true : false;
}

BOOL CMagneticView::GetAnimate(void)
{
  return m_bAnimate;
}

bool CMagneticView::GetMorePrompt(void)
{
  return m_bMorePrompt;
}

CMagneticView::Recording CMagneticView::GetRecording(void)
{
  return m_Recording;
}

void CMagneticView::SetRecording(Recording Record)
{
  m_Recording = Record;
}

FILE* CMagneticView::GetRecordFile(void)
{
  return m_pFileRecord;
}

void CMagneticView::SetRecordFile(FILE* pFile)
{
  m_pFileRecord = pFile;
}

/////////////////////////////////////////////////////////////////////////////
// Static support functions for the interpreter
/////////////////////////////////////////////////////////////////////////////

void CMagneticView::CaretOn(void)
{
  CMagneticView* pView = CMagneticView::GetView();
  if (pView == NULL)
    return;

  TEXTMETRIC FontInfo;
  pView->m_pTextDC->GetTextMetrics(&FontInfo);
  int iFontHeight = (int)(FontInfo.tmHeight*1.1);

  // Turn the caret on
  pView->CreateSolidCaret(2,iFontHeight);
  pView->ShowCaret();
}

void CMagneticView::CaretOff(void)
{
  CMagneticView* pView = CMagneticView::GetView();

  if (pView)
    pView->HideCaret();
  DestroyCaret();
}

void CMagneticView::MakeFilePath(CString& strNewPath, LPCTSTR pszOldPath, LPCTSTR pszExt)
{
  strNewPath = pszOldPath;
  LPSTR pszNewPath = strNewPath.GetBuffer(strNewPath.GetLength()+
                     strlen(pszExt)+1);

  char* pExtPos = strrchr(pszNewPath,'.');
  char* pDirPos = strrchr(pszNewPath,'/');
  if (pExtPos > pDirPos)
    strcpy(pExtPos,pszExt);
  else
    strcat(pszNewPath,pszExt);

  strNewPath.ReleaseBuffer();
}

BOOL CMagneticView::OpenGame(LPCTSTR lpszPathName)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  CMagneticView* pView = CMagneticView::GetView();

  if (pApp->GetGameLoaded() && ms_is_running())
    pApp->SetRedrawStatus(CMagneticApp::Redraw::EndOpcode);

  if (pView)
  {
    pView->ClearAll();
    pView->PlayMusic(NULL,0,0);
  }

  CString strGfxName, strHntName, strSndName;
  MakeFilePath(strGfxName,lpszPathName,".gfx");
  MakeFilePath(strHntName,lpszPathName,".hnt");
  MakeFilePath(strSndName,lpszPathName,".snd");

  // Free previous game
  ms_freemem();

  // Initialize new game
  pApp->SetGameLoaded(ms_init((type8s*)lpszPathName,
    (type8s*)((LPCTSTR)strGfxName),
    (type8s*)((LPCTSTR)strHntName),
    (type8s*)((LPCTSTR)strSndName)));

  // If required, make the random number generator predictable
  if (pApp->GetPredictable())
    ms_seed(pApp->GetRandomSeed());

  // Check status of loaded game
  if (pApp->GetGameLoaded() == 0)
  {
    CString strMessage;

    strMessage.Format("Failed to load game \"%s\"",lpszPathName);
    AfxMessageBox(strMessage,MB_ICONEXCLAMATION);
  }
  else
  {
    if (pView)
    {
      pView->m_bStatusBar = false;
      pView->Invalidate();
    }

    // Show the title picture, if possible
    CMagneticTitleDlg Title;
    Title.ShowTitle(lpszPathName);

    if (pView)
    {
      // Set up default file names
      MakeFilePath(pView->m_strRecName,lpszPathName,".rec");
      MakeFilePath(pView->m_strScrName,lpszPathName,".scr");
      MakeFilePath(pView->m_strFileName,lpszPathName,".sav");

      pView->m_bStatusBar = ms_is_magwin() ? false : true;
    }
  }

  if (pView)
    pView->Invalidate();
  return (pApp->GetGameLoaded() != 0) ? TRUE : FALSE;
}

char CMagneticView::GetInput(bool& done, bool trans)
{
  static const int MAX_HISTORY = 20;
  done = true;

  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  CMagneticView* pView = CMagneticView::GetView();
  if (pView == NULL)
    return 0;

  int cInput = 0;        // Input character
  int iPosition = 0;    // Current cursor position
  int iHistory = -1;    // Current history position
  static CString strFullLine;

  // Input line already obtained?
  if (strFullLine.GetLength() > 0)
  {
    cInput = strFullLine[0];
    strFullLine = strFullLine.Right(strFullLine.GetLength()-1);
    if (cInput == (signed char)(CMagneticView::SPECIAL_KEYS + VK_SPACE))
      cInput = ' ';
    return (char)cInput;
  }

  if (pView->m_bMorePrompt)
    pView->m_PageTable.RemoveAt(pView->m_PageTable.GetSize()-1);
  else
    pView->m_PageTable.RemoveAll();

  // Refresh the view
  pView->Invalidate();
  CaretOn();

  while (cInput != 10 && cInput != 1)
  {
    pView = CMagneticView::GetView();
    if (pView == NULL)
      break;

    // Wait for a character
    CArray<int, int>& Input = pView->m_Input;
    if (Input.GetSize() == 0)
    {
      if (pView)
        pView->ShowCaret();
      pApp->PumpMessage();
      pApp->CWinApp::OnIdle(0);  // Call base class OnIdle();
      pView = CMagneticView::GetView();
      if (pView)
      {
        pView->HideCaret();
        CMagneticApp::Redraw Status = pApp->GetRedrawStatus();
        switch (Status)
        {
        case CMagneticApp::Redraw::EndPlayback:
          done = false;    // intentional fall-through
        case CMagneticApp::Redraw::EndLine:
        case CMagneticApp::Redraw::EndOpcode:
          Input.RemoveAll();
          strFullLine.Empty();
          if (Status == CMagneticApp::Redraw::EndOpcode)
            cInput = 1;
          else
            cInput = 10;  // intentional fall-through
        case CMagneticApp::Redraw::ThisLine:
          CaretOff();
          CaretOn();
          pView->Invalidate();
          break;
        }
        pView->SetCursorPos(pView->m_pTextDC,strFullLine.GetLength()-iPosition);
      }
    }
    else
    {
      cInput = (pView->m_bMorePrompt) ? 10 : Input[0];
      Input.RemoveAt(0);
      
      int iInsertPos, iRemovePos;
      switch (cInput)
      {
      case 10:                                      // Return
        strFullLine += (char)cInput;
        break;
      case CMagneticView::SPECIAL_KEYS + VK_LEFT:    // Cursor left
        if (iPosition > 0)
          iPosition--;
        break;
      case CMagneticView::SPECIAL_KEYS + VK_RIGHT:  // Cursor right
        if (iPosition < strFullLine.GetLength())
          iPosition++;
        break;
      case CMagneticView::SPECIAL_KEYS + VK_HOME:    // Home
        iPosition = 0;
        break;
      case CMagneticView::SPECIAL_KEYS + VK_END:    // End
        iPosition = strFullLine.GetLength();
        break;
      case CMagneticView::SPECIAL_KEYS + VK_DELETE:  // Delete
        if (iPosition < strFullLine.GetLength())
        {
          iRemovePos = strFullLine.GetLength() - iPosition;
          pView->RemoveChar(strFullLine,iRemovePos);
          pView->RemoveChar(pView->m_strOutput,iRemovePos,TRUE);
        }
        break;
      case 8:                                        // Backspace
        if (iPosition > 0)
        {
          iRemovePos = strFullLine.GetLength() - iPosition + 1;
          pView->RemoveChar(strFullLine,iRemovePos);
          pView->RemoveChar(pView->m_strOutput,iRemovePos,TRUE);
          iPosition--;
        }
        break;
      case CMagneticView::SPECIAL_KEYS + VK_UP:      // Cursor up
        if (iHistory < pView->m_History.GetSize()-1)
          iHistory++;
        if ((iHistory >= 0) && (pView->m_History.GetSize() > 0))
        {
          int iOldLength = strFullLine.GetLength();
          strFullLine = pView->m_History[iHistory];
          pView->UseHistory(strFullLine,iOldLength);
          iPosition = strFullLine.GetLength();
        }
        break;
      case CMagneticView::SPECIAL_KEYS + VK_DOWN:    // Cursor down
        if (iHistory > 0)
          iHistory--;
        if ((iHistory >= 0) && (pView->m_History.GetSize() > 0))
        {
          int iOldLength = strFullLine.GetLength();
          strFullLine = pView->m_History[iHistory];
          pView->UseHistory(strFullLine,iOldLength);
          iPosition = strFullLine.GetLength();
        }
        break;
      case CMagneticView::SPECIAL_KEYS + VK_SPACE:  // Space
        iInsertPos = strFullLine.GetLength() - iPosition;
        pView->InsertChar(pView->m_strOutput,(char)cInput,iInsertPos,TRUE);
        pView->InsertChar(strFullLine,(char)cInput,iInsertPos);
        iPosition++;
        break;
      default:
        if (isprint(cInput) && (cInput < CMagneticView::SPECIAL_KEYS))
        {
          // Insert the character into the input string
          iInsertPos = strFullLine.GetLength() - iPosition;
          pView->InsertChar(pView->m_strOutput,(char)cInput,iInsertPos,TRUE);
          pView->InsertChar(strFullLine,(char)cInput,iInsertPos);
          iPosition++;
        }
        break;
      }

      // Update the input line
      pView->InvalidateRect(pView->m_LastLineRect,FALSE);
    }
  }

  if (pView && (strFullLine.GetLength() > 0))
  {
    if (pView->m_bMorePrompt == false)
    {
      // Store in input history
      CString strHistory = strFullLine.Left(strFullLine.GetLength()-1);
      if (strHistory.GetLength() > 0)
      {
        pView->m_History.InsertAt(0,strHistory);
        if (pView->m_History.GetSize() > MAX_HISTORY)
          pView->m_History.RemoveAt(pView->m_History.GetSize()-1);
      }

      int i;
      while ((i = strHistory.Find((char)(CMagneticView::SPECIAL_KEYS + VK_SPACE))) >= 0)
        strHistory.SetAt(i,' ');

      // Input recording
      if ((pView->m_Recording == Recording::RecordingOn) && (pView->m_pFileRecord))
          fprintf(pView->m_pFileRecord,"%s\n",strHistory);

      // Scrollback buffer
      pView->m_Scrollback.GetScrollback() += strHistory;

      // Scripting
      if (pView->m_Scripting == Scripting::ScriptingOn)
        pView->m_strScript += strHistory;
    }

    if (trans && (strFullLine.CompareNoCase("#undo\n") == 0))
    {
      cInput = 0;
      strFullLine.Empty();
    }
    else
    {
      cInput = strFullLine[0];
      strFullLine = strFullLine.Right(strFullLine.GetLength()-1);
    }
  }

  if (pView)
    pView->m_iLines = 0;
  CaretOff();

  if (cInput == (signed char)(CMagneticView::SPECIAL_KEYS + VK_SPACE))
    cInput = ' ';

  return (char)cInput;
}

char CMagneticView::GetPlaybackChar(FILE* file)
{
  bool got = false;
  char c = '\0';

  while (got == false)
  {
    c = (char)fgetc(file);
    if (feof(file))
      return c;

    if (c == '#')
    {
      char line[256];
      if (fgets(line,256,file))
      {
        if (stricmp(line,"seed\n") == 0)
        {
          if (fgets(line,256,file))
          {
            ms_seed(atoi(line));
            c = 10;
            got = true;
          }
        }
      }
    }
    else
      got = true;
  }
  return c;
}

/////////////////////////////////////////////////////////////////////////////
// Interface to the Magnetic interpreter
/////////////////////////////////////////////////////////////////////////////

type8 ms_load_file(type8s *name, type8 *ptr, type16 size)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  CString strLoadName;
  FILE *fh;

  if (name == NULL)
  {
    CMagneticView* pView = CMagneticView::GetView();
    if (pView == NULL)
      return 0;

    SimpleFileDialog LoadDlg(TRUE,NULL,pView->GetFileName(),
      OFN_FILEMUSTEXIST|OFN_HIDEREADONLY|OFN_ENABLESIZING,
      "Saved Game Files (*.sav)|*.sav|All Files (*.*)|*.*||",pView);
    LoadDlg.m_ofn.lpstrTitle = "Load a Saved Game";

    if (ms_is_running())
      pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
    if (LoadDlg.DoModal() == IDOK)
    {
      strLoadName = LoadDlg.GetPathName();
      pView->GetFileName() = strLoadName;
    }
    else
      return 0;
  }
  else
    strLoadName = (char*)name;

  if ((fh = fopen(strLoadName,"rb")) == NULL)
    return 1;
  if (fread(ptr,1,size,fh) != size)
    return 1;
  fclose(fh);
  return 0;
}

type8 ms_save_file(type8s *name, type8 *ptr, type16 size)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  CString strLoadName;
  FILE *fh;

  if (name == NULL)
  {
    CMagneticView* pView = CMagneticView::GetView();
    if (pView == NULL)
      return 0;

    SimpleFileDialog SaveDlg(FALSE,NULL,pView->GetFileName(),OFN_HIDEREADONLY|OFN_ENABLESIZING,
      "Saved Game Files (*.sav)|*.sav|All Files (*.*)|*.*||",pView);
    SaveDlg.m_ofn.lpstrTitle = "Save the Current Game";

    if (ms_is_running())
      pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
    if (SaveDlg.DoModal() == IDOK)
    {
      strLoadName = SaveDlg.GetPathName();
      pView->GetFileName() = strLoadName;
    }
    else
      return 0;
  }
  else
    strLoadName = (char*)name;

  if ((fh = fopen(strLoadName,"wb")) == NULL)
    return 1;
  if (fwrite(ptr,1,size,fh) != size)
    return 1;
  fclose(fh);
  return 0;
}

void ms_statuschar(type8 c)
{
  CMagneticView* pView = CMagneticView::GetView();
  if (pView != NULL)
    pView->AddStatChar(c);
}

void ms_putchar(type8 c)
{
  CMagneticView* pView = CMagneticView::GetView();
  if (pView != NULL)
    pView->AddOutChar(c);
}

void ms_flush(void)
{
}

type8 ms_getchar(type8 trans)
{
  bool done = false;
  char c = 0;

  do
  {
    CMagneticView* pView = CMagneticView::GetView();
    if (pView == NULL)
      return 1;

    if (pView->GetRecording() == CMagneticView::Recording::PlaybackOn)
    {
      if (pView->GetRecordFile())
      {
        CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

        if (pApp->GetAnimWait())
        {
          while (pView->GetAnimate() && (ms_anim_is_repeating() == 0))
          {
            if (pApp->PumpMessage() == FALSE)
            {
              ::PostQuitMessage(0);
              return 1;
            }
            pApp->CWinApp::OnIdle(0);
          }
        }

        char cInput = CMagneticView::GetPlaybackChar(pView->GetRecordFile());
        if (feof(pView->GetRecordFile()) != 0)
        {
          pView->SetRecording(CMagneticView::Recording::RecordingOff);
          fclose(pView->GetRecordFile());
          pView->SetRecordFile(NULL);
          pView->ClearPagination();
        }
        else
        {
          if (cInput == '\n')
          {
            cInput = 10;
            pView->TrimOutput();
            pView->GetPageTable().RemoveAll();
            pView->Invalidate();
          }
          else
            pView->AddOutChar(cInput);
          return cInput;
        }
      }
    }
    c = CMagneticView::GetInput(done,trans != 0);
  }
  while (done == false);
  return c;
}

void ms_showpic(type32 c, type8 mode)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  if (pApp->GetShowGraphics() != CMagneticApp::ShowGraphics::NoGraphics)
  {
    CMagneticView* pView = CMagneticView::GetView();
    if (pView == NULL)
      return;

    type8 *pPictureData = NULL;
    type16 Width, Height;
    type8 IsAnim = 0;

    switch (mode)
    {
    case 0:  // Graphics off
      if (pView->GetPictureWindow().GetSafeHwnd())
        pView->GetPictureWindow().SendMessage(WM_CLOSE,0,0);
      pView->GetPicture().ClearAll();
      pView->SetAnimate(FALSE);
      pView->ClearAnims();
      break;

    case 1:  // Graphics on (thumbnails)
    case 2:  // Graphics on (normal)
      pView->SetAnimate(FALSE);
      pView->ClearAnims();
      pPictureData = ms_extract(c,&Width,&Height,pView->GetPalette(),&IsAnim);
      if (pPictureData)
      {
        pView->GetPicture().NewPicture(Width,Height,pPictureData,pView->GetPalette());
        pView->SetAnimate(IsAnim != 0);
        if (IsAnim != 0)
          pView->Animate();

        pView->Invalidate();
        pView->SetPictureWindowState();
      }
      break;
    }
  }
}

void ms_fatal(type8s *txt)
{
  static bool InProgress = false;
  if (InProgress == false)
  {
    InProgress = true;
    
    CString Error;
    Error.Format("Magnetic has encountered an internal error:\n%s",txt ? (char*)txt : "");
    AfxGetMainWnd()->MessageBox(Error,"Internal Error",MB_ICONERROR|MB_OK);
    
    ms_stop();

    CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
    pApp->SetRedrawStatus(CMagneticApp::Redraw::EndOpcode);

    InProgress = false;
  }
}

type8 ms_showhints(struct ms_hint * hints)
{
  CMagneticView* pView = CMagneticView::GetView();
  if (pView == NULL)
    return 0;

  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  if (pApp->GetUseHintWindow() == FALSE)
    return 0;
  if (ms_is_running())
     pApp->SetRedrawStatus(CMagneticApp::Redraw::ThisLine);
  pView->m_hintDlg.SetHints(hints);
  pView->m_hintDlg.DoModal();
  return 1;
}

void ms_playmusic(type8 * midi_data, type32 length, type16 tempo)
{
  CMagneticView* pView = CMagneticView::GetView();
  if (pView != NULL)
    pView->PlayMusic(midi_data,length,tempo);
}
