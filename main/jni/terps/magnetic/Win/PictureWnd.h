/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// PictureWnd.h: Declaration of the picture window class
//
/////////////////////////////////////////////////////////////////////////////

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

/////////////////////////////////////////////////////////////////////////////
// CMagneticPic
/////////////////////////////////////////////////////////////////////////////

class CMagneticPic
{
public:
  CMagneticPic();
  ~CMagneticPic();

  void Paint(CDC* pDC, const CRect& Area, const CArray<CMagneticPic*,CMagneticPic*>& Animation) const;
  void Paint(CDC* pDC) const;

  bool IsValid(void) const;
  int SetPalette(CDC* pDC, CWnd* pWnd) const;
  int GetScaledWidth(void) const;
  int GetScaledHeight(void) const;

  void NewPicture(int iWidth, int iHeight, const unsigned char* pBuffer, const unsigned short* pPalette);
  void ClearAll(void);
  void SetOrigin(const CPoint& Origin);
  void SetMask(const unsigned char* pMask);

protected:
  // Bitmap data
  BITMAPINFO* m_pBitmapInfo;
  unsigned char* m_pBitmap;
  CPalette* m_pPalette;
  CBitmap* m_pMask;

  CSize m_Size;
  CPoint m_Origin;
};

/////////////////////////////////////////////////////////////////////////////
// CPictureWnd window
/////////////////////////////////////////////////////////////////////////////

class CPictureWnd : public CWnd
{
// Construction
public:
  CPictureWnd(CMagneticPic& Picture, CArray<CMagneticPic*,CMagneticPic*>& AnimFrames);

// Attributes
public:

// Operations
public:
  BOOL CreatePicWnd(CWnd* pParent, const RECT& rect);
  void Update(void);

// Overrides
  // ClassWizard generated virtual function overrides
  //{{AFX_VIRTUAL(CPictureWnd)
  //}}AFX_VIRTUAL

// Implementation
public:
  virtual ~CPictureWnd();

  // Generated message map functions
protected:
  //{{AFX_MSG(CPictureWnd)
  afx_msg void OnPaint();
  afx_msg void OnChar(UINT nChar, UINT nRepCnt, UINT nFlags);
  afx_msg void OnDestroy();
  afx_msg void OnKeyDown(UINT nChar, UINT nRepCnt, UINT nFlags);
  afx_msg void OnPaletteChanged(CWnd* pFocusWnd);
  afx_msg BOOL OnQueryNewPalette();
  //}}AFX_MSG
  DECLARE_MESSAGE_MAP()

protected:
  // Main input window
  CWnd* m_pMagneticWnd;

  // Window borders
  CSize m_Borders;

  // Picture data
  const CMagneticPic& m_Picture;
  const CArray<CMagneticPic*,CMagneticPic*>& m_AnimFrames;
};
