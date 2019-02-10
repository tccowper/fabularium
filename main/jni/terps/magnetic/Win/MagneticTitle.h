/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// MagneticTitle.h: Title picture dialog class
//
/////////////////////////////////////////////////////////////////////////////

extern "C"
{
#include "png.h"
}

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

class CMagneticTitleDlg : public CDialog
{
// Construction
public:
  CMagneticTitleDlg(CWnd* pParent = NULL);   // standard constructor
  ~CMagneticTitleDlg();

// Dialog Data
  //{{AFX_DATA(CMagneticTitleDlg)
  enum { IDD = IDD_TITLE };
  //}}AFX_DATA

// Overrides
  // ClassWizard generated virtual function overrides
  //{{AFX_VIRTUAL(CMagneticTitleDlg)
  protected:
  virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
  //}}AFX_VIRTUAL

// Implementation
protected:

  // Generated message map functions
  //{{AFX_MSG(CMagneticTitleDlg)
  virtual BOOL OnInitDialog();
  afx_msg void OnKeyDown(UINT nChar, UINT nRepCnt, UINT nFlags);
  afx_msg void OnLButtonDown(UINT nFlags, CPoint point);
  afx_msg void OnPaint();
  //}}AFX_MSG
  afx_msg void OnPaletteChanged(CWnd* pFocusWnd);
  afx_msg BOOL OnQueryNewPalette();
  DECLARE_MESSAGE_MAP()

public:
  void ShowTitle(LPCTSTR pszGamePath);

protected:
  png_bytep* m_pRowPointers;
  BYTE* m_pPixels;
  BITMAPINFOHEADER m_BitmapInfo;
  HPALETTE m_hPalette;
  HBITMAP m_hBitmap;
  double m_dScaleX;
  double m_dScaleY;

protected:
  HPALETTE CreateOctreePalette(HANDLE hImage, UINT nMaxColors, UINT nColorBits);

  typedef struct _NODE
  {
    BOOL bIsLeaf;               // TRUE if node has no children
    UINT nPixelCount;           // Number of pixels represented by this leaf
    UINT nRedSum;               // Sum of red components
    UINT nGreenSum;             // Sum of green components
    UINT nBlueSum;              // Sum of blue components
    struct _NODE* pChild[8];    // Pointers to child nodes
    struct _NODE* pNext;        // Pointer to next reducible node
  }
  NODE;

  int GetRightShiftCount(DWORD dwVal);
  int GetLeftShiftCount(DWORD dwVal);
  void AddColor(NODE** ppNode, BYTE r, BYTE g, BYTE b, UINT nColorBits,
    UINT nLevel, UINT* pLeafCount, NODE** pReducibleNodes);
  NODE* CreateNode(UINT nLevel, UINT nColorBits, UINT* pLeafCount,
    NODE** pReducibleNodes);
  void ReduceTree(UINT nColorBits, UINT* pLeafCount, NODE** pReducibleNodes);
  void DeleteTree(NODE** ppNode);
  void GetPaletteColors(NODE* pTree, PALETTEENTRY* pPalEntries, UINT* pIndex);

protected:
  void StartMusic(LPCTSTR pszGamePath);
  void StopMusic();
  CWinThread* m_pMusicThread;

  static UINT MusicThread(LPVOID pParam);
  static CString m_strMusic;
  static BOOL m_bMusicHalt;
};
