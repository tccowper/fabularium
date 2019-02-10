/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// MagneticView.h: Declaration of the view class
//
/////////////////////////////////////////////////////////////////////////////

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

#include "HintDialog.h"
#include "PictureWnd.h"
#include "ScrollBackDlg.h"

extern "C"
{
#include "defs.h"
}

class CMagneticView : public CView
{
protected: // create from serialization only
  CMagneticView();
  DECLARE_DYNCREATE(CMagneticView)

// Attributes
public:
  CMagneticDoc* GetDocument();

// Operations
public:
  static CMagneticView* GetView(void);

// Overrides
  // ClassWizard generated virtual function overrides
  //{{AFX_VIRTUAL(CMagneticView)
  public:
  virtual void OnDraw(CDC* pDC);  // overridden to draw this view
  virtual BOOL PreCreateWindow(CREATESTRUCT& cs);
  //}}AFX_VIRTUAL

// Implementation
public:
  virtual ~CMagneticView();
#ifdef _DEBUG
  virtual void AssertValid() const;
  virtual void Dump(CDumpContext& dc) const;
#endif

public:
  enum Recording
  {
    RecordingOff,
    RecordingOn,
    PlaybackOn
  };
  enum Scripting
  {
    ScriptingOff,
    ScriptingOn
  };

public:
  // Support functions for the interpreter
  void TextSetup(void);
  void TextClearup(void);
  void UseHistory(CString& strNewInput, int iOldLength);
  void AddOutChar(char c);
  void AddStatChar(char c);
  void SetCursorPos(CDC* pDC, int iRight);
  void InsertChar(CString& strInsert, char cChar, int iPos, int iIsOut = FALSE);
  void RemoveChar(CString& strRemove, int iPos, int iIsOut = FALSE);
  void ClearAll(void);
  void TrimOutput(void);
  void ClearAnims(void);
  void SetPictureWindowState(void);

  CArray<int,int>& GetPageTable(void);
  CString& GetFileName(void);
  CPictureWnd& GetPictureWindow(void);
  CMagneticPic& GetPicture(void);
  unsigned short* GetPalette(void);
  void SetAnimate(BOOL bAnim);
  BOOL GetAnimate(void);
  void Animate(void);
  void PlayMusic(unsigned char* pMidiData, int iLength, int iTempo);

  bool GetMorePrompt(void);
  Recording GetRecording(void);
  void SetRecording(Recording Record);
  FILE* GetRecordFile(void);
  void SetRecordFile(FILE* pFile);
  void ClearPagination(void);

  // Static support functions for the interpreter
  static void CaretOn(void);
  static void CaretOff(void);
  static void MakeFilePath(CString& strNewPath, LPCTSTR pszOldPath, LPCTSTR pszExt);
  static BOOL OpenGame(LPCTSTR lpszPathName);
  static char GetInput(bool& done, bool trans);
  static char GetPlaybackChar(FILE* file);

protected:
  void SolidRect(CDC* pDC, LPCRECT lpRect, COLORREF Colour);
  int Paginate(CDC* pDC, int p1, int p2);
  BOOL LineFull(CDC* pDC, LPCSTR lpszText, int iLength);
  int FindPreviousSpace(LPCSTR lpszText, int iPos);

// Generated message map functions
protected:
  //{{AFX_MSG(CMagneticView)
  afx_msg void OnChar(UINT nChar, UINT nRepCnt, UINT nFlags);
  afx_msg void OnKeyDown(UINT nChar, UINT nRepCnt, UINT nFlags);
  afx_msg void OnDestroy();
  afx_msg void OnSize(UINT nType, int cx, int cy);
  afx_msg int OnCreate(LPCREATESTRUCT lpCreateStruct);
  afx_msg void OnRecord();
  afx_msg void OnPlayback();
  afx_msg void OnScript();
  afx_msg void OnUpdateRecord(CCmdUI* pCmdUI);
  afx_msg void OnUpdatePlayback(CCmdUI* pCmdUI);
  afx_msg void OnUpdateScript(CCmdUI* pCmdUI);
  afx_msg void OnUpdateFileOpen(CCmdUI* pCmdUI);
  afx_msg void OnUpdateViewFont(CCmdUI* pCmdUI);
  afx_msg void OnScrollback();
  afx_msg void OnEditPaste();
  afx_msg void OnUpdateViewOptions(CCmdUI* pCmdUI);
  afx_msg BOOL OnEraseBkgnd(CDC* pDC);
  afx_msg void OnTimer(UINT nIDEvent);
  afx_msg void OnUpdateScrollback(CCmdUI* pCmdUI);
  afx_msg void OnToggleGfx();
  //}}AFX_MSG
  DECLARE_MESSAGE_MAP()

protected:
  static const int SPECIAL_KEYS;
  static const int MAX_LINES;

  CArray<int,int> m_PageTable;    // Keeps track of current pagination

  int m_iLines;
  int m_iMaxLines;
  bool m_bMorePrompt;

  CString m_strOutput;
  CArray<int,int> m_Input;
  CRect m_LastLineRect;
  CArray<CString, CString&> m_History;

  CString m_strStatLocation;
  CString m_strStatScore;
  CString m_strStatCurrent;
  bool m_bStatusBar;

  CFont* m_pTextFont;      // Current output text fount
  CFont* m_pOldFont;      // Previous DC font
  CDC* m_pTextDC;          // Device context for text attributes

  CString m_strFileName;  // File name for load and save dialog
  CString m_strRecName;    // File name for recording
  CString m_strScrName;    // File name for scripting

  Recording m_Recording;  // File recording status
  FILE* m_pFileRecord;
  Scripting m_Scripting;  // Scripting status
  FILE* m_pFileScript;
  CString m_strScript;

  CScrollBackDlg m_Scrollback;  // Scrollback dialog

  CPictureWnd m_PicWnd;          // Picture window
  CMagneticPic m_Picture;        // Picture data
  unsigned short m_Palette[16];

  CArray<CMagneticPic*,CMagneticPic*> m_AnimFrames;  // Animation data
  CPoint m_AnimPoint;
  UINT m_iTimer;
  bool m_bAnimate;

  CComPtr<IDirectMusicPerformance> m_Perform;
  CComPtr<IDirectMusicSegment> m_Segment;

public:
  CHintDialog m_hintDlg;  // Hint dialog
};

#ifndef _DEBUG  // debug version in MagneticView.cpp
inline CMagneticDoc* CMagneticView::GetDocument()
   { return (CMagneticDoc*)m_pDocument; }
#endif
