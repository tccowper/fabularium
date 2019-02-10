/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// PictureWnd.cpp: Implementation of the picture window
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"
#include <math.h>

#include "Magnetic.h"
#include "PictureWnd.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CMagneticPic
/////////////////////////////////////////////////////////////////////////////

CMagneticPic::CMagneticPic() : m_Size(0,0), m_Origin(0,0)
{
  m_pBitmapInfo = NULL;
  m_pBitmap = NULL;
  m_pPalette = NULL;
  m_pMask = NULL;
}

CMagneticPic::~CMagneticPic()
{
  ClearAll();
}

void CMagneticPic::Paint(CDC* pDC, const CRect& Area, const CArray<CMagneticPic*,CMagneticPic*>& Animation) const
{
  if (m_pPalette)
  {
    pDC->SelectPalette(m_pPalette,FALSE);
    pDC->RealizePalette();
  }

  CDC BitmapDC;
  BitmapDC.CreateCompatibleDC(pDC);

  CBitmap Bitmap;
  Bitmap.CreateCompatibleBitmap(pDC,m_Size.cx,m_Size.cy);
  CBitmap* pOldBitmap = BitmapDC.SelectObject(&Bitmap);

  BitmapDC.FillSolidRect(0,0,m_Size.cx,m_Size.cy,RGB(0x00,0x00,0x00));

  // Draw the main picture into the buffer
  Paint(&BitmapDC);

  // Draw the animation frames
  for (int i = 0; i < Animation.GetSize(); i++)
    Animation[i]->Paint(&BitmapDC);

  // Draw the buffer into the device context
  pDC->StretchBlt(Area.left,Area.top,Area.Width(),Area.Height(),
    &BitmapDC,0,0,m_Size.cx,m_Size.cy,SRCCOPY);
  BitmapDC.SelectObject(pOldBitmap);
}

void CMagneticPic::Paint(CDC* pDC) const
{
  if (m_pBitmap && m_pBitmapInfo)
  {
    if (m_pMask)
    {
      // Set up a buffer device context and bitmap
      CDC BitmapDC;
      BitmapDC.CreateCompatibleDC(pDC);
      CBitmap Bitmap;
      Bitmap.CreateCompatibleBitmap(pDC,m_Size.cx,m_Size.cy);
      CBitmap* pOldBitmap = BitmapDC.SelectObject(&Bitmap);

      // Draw the image into the buffer
      if (::StretchDIBits(BitmapDC.GetSafeHdc(),
        0,0,m_Size.cx,m_Size.cy,0,0,m_Size.cx,m_Size.cy,
        (LPVOID)m_pBitmap,m_pBitmapInfo,DIB_RGB_COLORS,SRCCOPY) <= 0)
        return;

      // Set up the mask
      CDC MaskDC;
      MaskDC.CreateCompatibleDC(pDC);
      CBitmap* pOldMask = MaskDC.SelectObject(m_pMask);

      // Cut out mask in the device context
      pDC->SetTextColor(RGB(0x00,0x00,0x00));
      pDC->SetBkColor(RGB(0xFF,0xFF,0xFF));
      pDC->BitBlt(m_Origin.x,m_Origin.y,m_Size.cx,m_Size.cy,
        &MaskDC,0,0,SRCAND);

      // Draw the image into the device context
      pDC->BitBlt(m_Origin.x,m_Origin.y,m_Size.cx,m_Size.cy,
        &BitmapDC,0,0,SRCPAINT);

      // Clear up
      MaskDC.SelectObject(pOldMask);
      BitmapDC.SelectObject(pOldBitmap);
    }
    else
    {
      ::StretchDIBits(pDC->GetSafeHdc(),
        m_Origin.x,m_Origin.y,m_Size.cx,m_Size.cy,0,0,m_Size.cx,m_Size.cy,
        (LPVOID)m_pBitmap,m_pBitmapInfo,DIB_RGB_COLORS,SRCCOPY);
    }

/*
    // For debugging, show a border around each frame
    CBrush FrameBrush;
    FrameBrush.CreateSolidBrush(RGB(0,0,0));
    CRect FrameRect(m_Origin.x,m_Origin.y,
      m_Origin.x+m_Size.cx,m_Origin.y+m_Size.cy);
    pDC->FrameRect(FrameRect,&FrameBrush);
*/
  }
}

bool CMagneticPic::IsValid(void) const
{
  return (m_pBitmap && m_pBitmapInfo);
}

int CMagneticPic::SetPalette(CDC* pDC, CWnd* pWnd) const
{
  int iColours = 0;
  if (m_pPalette)
  {
    pDC->SelectPalette(m_pPalette,FALSE);
    if ((iColours = pDC->RealizePalette()) != 0)
      pWnd->Invalidate(FALSE);
  }
  return iColours;
}

void CMagneticPic::NewPicture(int iWidth, int iHeight, const unsigned char* pBuffer, const unsigned short* pPalette)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  ClearAll();
  m_pBitmapInfo = (BITMAPINFO*)
    (new unsigned char[sizeof(BITMAPINFOHEADER)+(16*sizeof(RGBQUAD))]);

  // The bitmap data must be long-word aligned
  int iModulo = iWidth % 4;
  int iDibWidth = iWidth + ((iModulo > 0) ? (4-iModulo) : iModulo);

  m_pBitmapInfo->bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
  m_pBitmapInfo->bmiHeader.biWidth = iWidth;
  m_pBitmapInfo->bmiHeader.biHeight = iHeight * -1;  // top-down bitmap
  m_pBitmapInfo->bmiHeader.biPlanes = 1;
  m_pBitmapInfo->bmiHeader.biBitCount = 8;
  m_pBitmapInfo->bmiHeader.biCompression = BI_RGB;
  m_pBitmapInfo->bmiHeader.biSizeImage = 0;
  m_pBitmapInfo->bmiHeader.biXPelsPerMeter = 0;
  m_pBitmapInfo->bmiHeader.biYPelsPerMeter = 0;
  m_pBitmapInfo->bmiHeader.biClrUsed = 16;
  m_pBitmapInfo->bmiHeader.biClrImportant = 0;

  LOGPALETTE* pLogPalette = (LPLOGPALETTE)
    new unsigned char[sizeof(LOGPALETTE)+(16*sizeof(PALETTEENTRY))];

  pLogPalette->palVersion = 0x300;
  pLogPalette->palNumEntries = 16;

  for (int i = 0; i < 16; i++)
  {
    int red = (pPalette[i]&0x0F00)>>3;
    int green = (pPalette[i]&0x00F0)<<1;
    int blue = (pPalette[i]&0x000F)<<5;

    // Gamma correction
    double dGamma = pApp->GetGamma();
    if (dGamma > 0.0)
    {
      red = (int)sqrt((double)red*(double)red*dGamma);
      green = (int)sqrt((double)green*(double)green*dGamma);
      blue = (int)sqrt((double)blue*(double)blue*dGamma);
    }

    m_pBitmapInfo->bmiColors[i].rgbRed = (unsigned char)(red > 0xFF ? 0xFF : red);
    m_pBitmapInfo->bmiColors[i].rgbGreen = (unsigned char)(green > 0xFF ? 0xFF : green);
    m_pBitmapInfo->bmiColors[i].rgbBlue = (unsigned char)(blue > 0xFF ? 0xFF : blue);
    m_pBitmapInfo->bmiColors[i].rgbReserved = 0;
    pLogPalette->palPalEntry[i].peRed = m_pBitmapInfo->bmiColors[i].rgbRed;
    pLogPalette->palPalEntry[i].peGreen = m_pBitmapInfo->bmiColors[i].rgbGreen;
    pLogPalette->palPalEntry[i].peBlue = m_pBitmapInfo->bmiColors[i].rgbBlue;
    pLogPalette->palPalEntry[i].peFlags = 0;
  }

  m_pBitmap = new unsigned char[iDibWidth*iHeight];
  for (i = 0; i < iHeight; i++)
  {
    for (int j = 0; j < iDibWidth; j++)
    {
      if (j < iWidth)
        m_pBitmap[(i*iDibWidth)+j] = pBuffer[(i*iWidth)+j];
      else
        m_pBitmap[(i*iDibWidth)+j] = 0;
    }
  }

  m_pPalette = new CPalette;
  m_pPalette->CreatePalette(pLogPalette);
  delete[] pLogPalette;

  // Store the bitmap size
  m_Size = CSize(iWidth,iHeight);
}

int CMagneticPic::GetScaledWidth(void) const
{
  int iWidth = 0;
  if (m_pBitmap && m_pBitmapInfo)
  {
    CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
    double dScale = pApp->GetScaleFactor();
    iWidth = (int)(m_Size.cx * dScale);
  }
  return iWidth;
}

int CMagneticPic::GetScaledHeight(void) const
{
  int iHeight = 0;
  if (m_pBitmap && m_pBitmapInfo)
  {
    CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
    double dScale = pApp->GetScaleFactor();
    iHeight = (int)(m_Size.cy * dScale);
  }
  return iHeight;
}

void CMagneticPic::ClearAll(void)
{
  if (m_pBitmapInfo)
    delete[] m_pBitmapInfo;
  m_pBitmapInfo = NULL;

  if (m_pBitmap)
    delete[] m_pBitmap;
  m_pBitmap = NULL;

  if (m_pPalette)
    delete m_pPalette;
  m_pPalette = NULL;

  if (m_pMask)
    delete m_pMask;
  m_pMask = NULL;
}

void CMagneticPic::SetOrigin(const CPoint& Origin)
{
  m_Origin = Origin;
}

void CMagneticPic::SetMask(const unsigned char* pMask)
{
  if (m_pMask)
  {
    delete m_pMask;
    m_pMask = NULL;
  }

  if (m_pBitmap && m_pBitmapInfo && pMask)
  {
    int iModulo = m_Size.cx % 16;
    int iWidth = m_Size.cx + ((iModulo > 0) ? (16-iModulo) : 0);
    int iByteWidth = iWidth / 8;
    unsigned char* pBits = new unsigned char[iByteWidth * m_Size.cy];

    for (int i = 0; i < m_Size.cy; i++)
    {
      for (int j = 0; j < iByteWidth; j++)
        pBits[(i*iByteWidth)+j] = pMask[(i*iByteWidth)+j];
    }

    m_pMask = new CBitmap;
    m_pMask->CreateBitmap(m_Size.cx,m_Size.cy,1,1,pBits);
    delete[] pBits;
  }
}

/////////////////////////////////////////////////////////////////////////////
// CPictureWnd
/////////////////////////////////////////////////////////////////////////////

BEGIN_MESSAGE_MAP(CPictureWnd, CWnd)
  //{{AFX_MSG_MAP(CPictureWnd)
  ON_WM_PAINT()
  ON_WM_CHAR()
  ON_WM_DESTROY()
  ON_WM_KEYDOWN()
  ON_WM_PALETTECHANGED()
  ON_WM_QUERYNEWPALETTE()
  //}}AFX_MSG_MAP
END_MESSAGE_MAP()

CPictureWnd::CPictureWnd(CMagneticPic& Picture, CArray<CMagneticPic*,CMagneticPic*>& AnimFrames) :
  m_Picture(Picture), m_AnimFrames(AnimFrames), m_Borders(0,0)
{
  m_pMagneticWnd = NULL;
}

CPictureWnd::~CPictureWnd()
{
}


/////////////////////////////////////////////////////////////////////////////
// CPictureWnd interpreter interface
/////////////////////////////////////////////////////////////////////////////

BOOL CPictureWnd::CreatePicWnd(CWnd* pParent, const RECT& rect)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  m_pMagneticWnd = pParent;
  BOOL bCreate = CreateEx(0,AfxRegisterWndClass(0),"Magnetic",
    WS_CAPTION|WS_VISIBLE|WS_SYSMENU,rect,pParent,0,NULL);
  if (bCreate == FALSE)
    return FALSE;
  SetIcon(pApp->LoadIcon(IDR_MAINFRAME),TRUE);

  // Get the horizontal and vertical border sizes
  CRect r1, r2;
  GetWindowRect(r1);
  GetClientRect(r2);
  m_Borders.cx = r1.Width() - r2.Width();
  m_Borders.cy = r1.Height() - r2.Height();

  return TRUE;
}

void CPictureWnd::Update(void)
{
  if (GetSafeHwnd())
  {
    // Resize the window to fit the picture and then repaint
    CRect rWnd;
    GetWindowRect(rWnd);
    rWnd.right = rWnd.left + m_Picture.GetScaledWidth() + m_Borders.cx;
    rWnd.bottom = rWnd.top + m_Picture.GetScaledHeight() + m_Borders.cy;
    MoveWindow(rWnd,TRUE);
    Invalidate();
  }
}

/////////////////////////////////////////////////////////////////////////////
// CPictureWnd message handlers
/////////////////////////////////////////////////////////////////////////////

void CPictureWnd::OnPaint() 
{
  CPaintDC PaintDC(this);
  CRect ClientArea;
  GetClientRect(ClientArea);
  m_Picture.Paint(&PaintDC,ClientArea,m_AnimFrames);
}

void CPictureWnd::OnChar(UINT nChar, UINT nRepCnt, UINT nFlags) 
{
  // Send the key to the Magnetic window
  if (m_pMagneticWnd)
    m_pMagneticWnd->SendMessage(WM_CHAR,nChar,nRepCnt|(nFlags<<16));
  
  CWnd::OnChar(nChar, nRepCnt, nFlags);
}

void CPictureWnd::OnKeyDown(UINT nChar, UINT nRepCnt, UINT nFlags) 
{
  // Send the key to the Magnetic window
  if (m_pMagneticWnd)
    m_pMagneticWnd->SendMessage(WM_KEYDOWN,nChar,nRepCnt|(nFlags<<16));
  
  CWnd::OnKeyDown(nChar, nRepCnt, nFlags);
}

void CPictureWnd::OnDestroy() 
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  // Store the position of the top-left corner of the window.
  CRect rWnd;
  CPoint& TopLeft = pApp->GetPicTopLeft();
  GetWindowRect(rWnd);

  TopLeft = rWnd.TopLeft();

  CWnd::OnDestroy();
}

void CPictureWnd::OnPaletteChanged(CWnd*) 
{
  CDC* pDC = GetDC();
  m_Picture.SetPalette(pDC,this);
  ReleaseDC(pDC);
}

BOOL CPictureWnd::OnQueryNewPalette() 
{
  CDC* pDC = GetDC();
  int iColours = m_Picture.SetPalette(pDC,this);
  ReleaseDC(pDC);
  return iColours;
}
