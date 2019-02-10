/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// MagneticTitle.cpp: Title picture dialog class
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"
#include <math.h>

#include "Magnetic.h"
#include "MagneticTitle.h"

#include "ampdec.h"
#include "binfarc.h"
#include "binfplnt.h"
#include "binfstd.h"

#pragma warning(disable : 4611)

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// Implementation of the CMagneticTitleDlg dialog
/////////////////////////////////////////////////////////////////////////////

CMagneticTitleDlg::CMagneticTitleDlg(CWnd* pParent /*=NULL*/)
  : CDialog(CMagneticTitleDlg::IDD, pParent)
{
  //{{AFX_DATA_INIT(CMagneticTitleDlg)
  //}}AFX_DATA_INIT

  m_pRowPointers = NULL;
  m_pPixels = NULL;
  m_hPalette = NULL;
  m_hBitmap = NULL;
  m_dScaleX = 1.0;
  m_dScaleY = 1.0;
  m_pMusicThread = NULL;
}

CMagneticTitleDlg::~CMagneticTitleDlg()
{
  if (m_pRowPointers)
    delete[] m_pRowPointers;
  if (m_pPixels)
    delete[] m_pPixels;
  if (m_hPalette)
    ::DeleteObject(m_hPalette);
  if (m_hBitmap)
    ::DeleteObject(m_hBitmap);
  if (m_pMusicThread)
    delete m_pMusicThread;
}

void CMagneticTitleDlg::DoDataExchange(CDataExchange* pDX)
{
  CDialog::DoDataExchange(pDX);
  //{{AFX_DATA_MAP(CMagneticTitleDlg)
  //}}AFX_DATA_MAP
}

BEGIN_MESSAGE_MAP(CMagneticTitleDlg, CDialog)
  //{{AFX_MSG_MAP(CMagneticTitleDlg)
  ON_WM_KEYDOWN()
  ON_WM_LBUTTONDOWN()
  ON_WM_PAINT()
  //}}AFX_MSG_MAP
  ON_WM_QUERYNEWPALETTE()
  ON_WM_PALETTECHANGED()
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CMagneticTitleDlg message handlers
/////////////////////////////////////////////////////////////////////////////

BOOL CMagneticTitleDlg::OnInitDialog() 
{
  CDialog::OnInitDialog();
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();

  int iScrWidth = ::GetSystemMetrics(SM_CXFULLSCREEN);
  int iScrHeight = ::GetSystemMetrics(SM_CYFULLSCREEN);
  int iWidth = (int)(m_BitmapInfo.biWidth * m_dScaleX * pApp->GetScaleTitles());
  int iHeight = (int)(-1 * m_BitmapInfo.biHeight * m_dScaleY * pApp->GetScaleTitles());

  // Adjust for the borders
  CRect Window, Client;
  GetWindowRect(Window);
  GetClientRect(Client);
  iWidth += (Window.Width()-Client.Width());
  iHeight += (Window.Height()-Client.Height());

  MoveWindow((iScrWidth-iWidth)/2,(iScrHeight-iHeight)/2,iWidth,iHeight);
  return TRUE;
}

void CMagneticTitleDlg::OnKeyDown(UINT nChar, UINT nRepCnt, UINT nFlags) 
{
  CDialog::OnKeyDown(nChar, nRepCnt, nFlags);
  OnOK();
}

void CMagneticTitleDlg::OnLButtonDown(UINT nFlags, CPoint point) 
{
  CDialog::OnLButtonDown(nFlags, point);
  OnOK();
}

void CMagneticTitleDlg::OnPaint() 
{
  CPaintDC dc(this);
  CDC dcMem;
  dcMem.CreateCompatibleDC(&dc);

  if (m_hPalette)
  {
    dc.SelectPalette(CPalette::FromHandle(m_hPalette),FALSE);
    dc.RealizePalette();
  }

  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  CBitmap* pOldBitmap = dcMem.SelectObject(CBitmap::FromHandle(m_hBitmap));
  dc.BitBlt(0,0,
    (int)(m_BitmapInfo.biWidth * m_dScaleX * pApp->GetScaleTitles()),
    (int)(-1 * m_BitmapInfo.biHeight * m_dScaleY * pApp->GetScaleTitles()),
    &dcMem,0,0,SRCCOPY);
  dcMem.SelectObject(pOldBitmap);
}

void CMagneticTitleDlg::OnPaletteChanged(CWnd*)
{
  CDC* pDC = GetDC();

  if (m_hPalette)
  {
    pDC->SelectPalette(CPalette::FromHandle(m_hPalette),FALSE);
    if (pDC->RealizePalette())
      Invalidate(FALSE);
  }
  ReleaseDC(pDC);
}

BOOL CMagneticTitleDlg::OnQueryNewPalette()
{
  CDC* pDC = GetDC();
  int iColours = 0;

  if (m_hPalette)
  {
    pDC->SelectPalette(CPalette::FromHandle(m_hPalette),FALSE);
    if ((iColours = pDC->RealizePalette()) != 0)
      Invalidate(FALSE);
  }
  ReleaseDC(pDC);
  return iColours;
}

/////////////////////////////////////////////////////////////////////////////
// Call to show the dialog
/////////////////////////////////////////////////////////////////////////////
void CMagneticTitleDlg::ShowTitle(LPCTSTR pszGamePath)
{
  CMagneticApp* pApp = (CMagneticApp*)AfxGetApp();
  CString strGamePath(pszGamePath);

  if (strGamePath.GetLength() > 4)
  {
    CString strTitlePic = strGamePath.Left(strGamePath.GetLength()-4);
    strTitlePic += ".png";

    FILE *fp = fopen(strTitlePic,"rb");
    if (!fp)
      return;

    unsigned char header[8];
    fread(header,1,8,fp);
    if (!png_check_sig(header,8))
    {
      fclose(fp);
      return;
    }

    png_structp png_ptr = png_create_read_struct
      (PNG_LIBPNG_VER_STRING,(png_voidp)NULL,NULL,NULL);
    if (!png_ptr)
    {
      fclose(fp);
      return;
    }

    png_infop info_ptr = png_create_info_struct(png_ptr);
    if (!info_ptr)
    {
      png_destroy_read_struct(&png_ptr,
        (png_infopp)NULL,(png_infopp)NULL);
      fclose(fp);
      return;
    }

    png_infop end_info = png_create_info_struct(png_ptr);
    if (!end_info)
    {
      png_destroy_read_struct(&png_ptr,&info_ptr,(png_infopp)NULL);
      fclose(fp);
      return;
    }

    if (setjmp(png_ptr->jmpbuf))
    {
      png_destroy_read_struct(&png_ptr,&info_ptr,&end_info);
      fclose(fp);
      return;
    }

    png_init_io(png_ptr,fp);
    png_set_sig_bytes(png_ptr,8);
    png_read_info(png_ptr,info_ptr);

    png_uint_32 width = png_get_image_width(png_ptr,info_ptr);
    png_uint_32 height = png_get_image_height(png_ptr,info_ptr);
    int bit_depth = png_get_bit_depth(png_ptr,info_ptr);
    int color_type = png_get_color_type(png_ptr,info_ptr);

    if ((bit_depth != 8) || (color_type != PNG_COLOR_TYPE_RGB))
    {
      png_destroy_read_struct(&png_ptr,&info_ptr,&end_info);
      fclose(fp);
      return;
    }

    double aspect_ratio = png_get_pixel_aspect_ratio(png_ptr,info_ptr);

    if (png_get_valid(png_ptr,info_ptr,PNG_INFO_tRNS))
      png_set_expand(png_ptr);
    png_set_bgr(png_ptr);
    png_set_filler(png_ptr,0,PNG_FILLER_AFTER);

    m_BitmapInfo.biSize = sizeof(BITMAPINFOHEADER);
    m_BitmapInfo.biWidth = width;
    m_BitmapInfo.biHeight = height*-1;
    m_BitmapInfo.biPlanes = 1;
    m_BitmapInfo.biBitCount = 32;
    m_BitmapInfo.biCompression = BI_RGB;
    m_BitmapInfo.biSizeImage = 0;
    m_BitmapInfo.biXPelsPerMeter = 0;
    m_BitmapInfo.biYPelsPerMeter = 0;
    m_BitmapInfo.biClrUsed = 0;
    m_BitmapInfo.biClrImportant = 0;

    m_pPixels = new BYTE[width*height*4];

    m_pRowPointers = new png_bytep[height];
    for (int i = 0; i < (int)height; i++)
      m_pRowPointers[i] = m_pPixels+(width*i*4);
    png_read_image(png_ptr,m_pRowPointers);

    png_read_end(png_ptr,end_info);
    png_destroy_read_struct(&png_ptr,&info_ptr,&end_info);
    fclose(fp);

    CClientDC dcWnd(CWnd::GetDesktopWindow());
    CDC dc;
    dc.CreateCompatibleDC(&dcWnd);
    BYTE* pPixels;

    if (aspect_ratio == 1)
    {
      if (width >= 640)
      {
        m_dScaleX = 1.0;
        m_dScaleY = 1.0;
      }
      else
      {
        m_dScaleX = 2.0;
        m_dScaleY = 2.0;
      }
    }
    else
    {
      m_dScaleX = 2.0;
      m_dScaleY = 1.0;
    }

    int iWidth = m_BitmapInfo.biWidth;
    int iHeight = m_BitmapInfo.biHeight;
    m_BitmapInfo.biWidth = (int)(m_BitmapInfo.biWidth * m_dScaleX * pApp->GetScaleTitles());
    m_BitmapInfo.biHeight = (int)(m_BitmapInfo.biHeight * m_dScaleY * pApp->GetScaleTitles());

    m_hBitmap = ::CreateDIBSection(dc.m_hDC,
      (CONST BITMAPINFO*)&m_BitmapInfo,DIB_RGB_COLORS,
      (VOID**)&pPixels,NULL,0);
    m_BitmapInfo.biWidth = iWidth;
    m_BitmapInfo.biHeight = iHeight;

    if (m_hBitmap)
    {
      // Resize the title picture
      CBitmap* pOldBitmap = dc.SelectObject(CBitmap::FromHandle(m_hBitmap));
      ::StretchDIBits(dc.m_hDC,
        0,0,
        (int)(m_BitmapInfo.biWidth * m_dScaleX * pApp->GetScaleTitles()),
        (int)(-1 * m_BitmapInfo.biHeight * m_dScaleY * pApp->GetScaleTitles()),
        0,0,
        iWidth,
        -m_BitmapInfo.biHeight,
        (LPVOID)m_pPixels,(LPBITMAPINFO)&m_BitmapInfo,
        DIB_RGB_COLORS,SRCCOPY);
      dc.SelectObject(pOldBitmap);

      if (dc.GetDeviceCaps(RASTERCAPS) & RC_PALETTE)
        m_hPalette = CreateOctreePalette(m_hBitmap,236,8);
    }

    StartMusic(pszGamePath);
    DoModal();
    StopMusic();
  }
}

/////////////////////////////////////////////////////////////////////////////
// Create a palette using the Gervautz-Purgathofer octree colour
// quanitization algorithm. This code is adapted from an article in MSJ.
/////////////////////////////////////////////////////////////////////////////

HPALETTE CMagneticTitleDlg::CreateOctreePalette(HANDLE hImage, UINT nMaxColors, UINT nColorBits)
{
  DIBSECTION ds;
  int i, j, nPad;
  BYTE* pbBits;
  WORD* pwBits;
  DWORD* pdwBits;
  DWORD rmask, gmask, bmask;
  int rright, gright, bright;
  int rleft, gleft, bleft;
  BYTE r, g, b;
  WORD wColor;
  DWORD dwColor, dwSize;
  LOGPALETTE* plp;
  HPALETTE hPalette;
  NODE* pTree;
  UINT nLeafCount, nIndex;
  NODE* pReducibleNodes[9];

  // Initialize octree variables
  pTree = NULL;
  nLeafCount = 0;
  if (nColorBits > 8) // Just in case
    return NULL;
  for (i=0; i<=(int) nColorBits; i++)
    pReducibleNodes[i] = NULL;

  // Scan the DIB and build the octree
  GetObject (hImage, sizeof (ds), &ds);
  nPad = ds.dsBm.bmWidthBytes - (((ds.dsBmih.biWidth *
    ds.dsBmih.biBitCount) + 7) / 8);

  switch (ds.dsBmih.biBitCount)
  {
  case 16: // One case for 16-bit DIBs
    if (ds.dsBmih.biCompression == BI_BITFIELDS)
    {
      rmask = ds.dsBitfields[0];
      gmask = ds.dsBitfields[1];
      bmask = ds.dsBitfields[2];
    }
    else
    {
      rmask = 0x7C00;
      gmask = 0x03E0;
      bmask = 0x001F;
    }

    rright = GetRightShiftCount(rmask);
    gright = GetRightShiftCount(gmask);
    bright = GetRightShiftCount(bmask);

    rleft = GetLeftShiftCount(rmask);
    gleft = GetLeftShiftCount(gmask);
    bleft = GetLeftShiftCount(bmask);

    pwBits = (WORD*) ds.dsBm.bmBits;
    for (i = 0; i < abs(ds.dsBmih.biHeight); i++)
    {
      for (j = 0; j < ds.dsBmih.biWidth; j++)
      {
        wColor = *pwBits++;
        b = (BYTE) (((wColor & (WORD) bmask) >> bright) << bleft);
        g = (BYTE) (((wColor & (WORD) gmask) >> gright) << gleft);
        r = (BYTE) (((wColor & (WORD) rmask) >> rright) << rleft);
        AddColor(&pTree, r, g, b, nColorBits, 0, &nLeafCount,
          pReducibleNodes);
        while (nLeafCount > nMaxColors)
          ReduceTree(nColorBits, &nLeafCount, pReducibleNodes);
      }
      pwBits = (WORD*) (((BYTE*) pwBits) + nPad);
    }
    break;

  case 24: // Another for 24-bit DIBs
    pbBits = (BYTE*) ds.dsBm.bmBits;
    for (i = 0; i < abs(ds.dsBmih.biHeight); i++)
    {
      for (j = 0; j < ds.dsBmih.biWidth; j++)
      {
        b = *pbBits++;
        g = *pbBits++;
        r = *pbBits++;
        AddColor(&pTree, r, g, b, nColorBits, 0, &nLeafCount,
          pReducibleNodes);
        while (nLeafCount > nMaxColors)
          ReduceTree(nColorBits, &nLeafCount, pReducibleNodes);
      }
      pbBits += nPad;
    }
    break;

  case 32: // And another for 32-bit DIBs
    if (ds.dsBmih.biCompression == BI_BITFIELDS)
    {
      rmask = ds.dsBitfields[0];
      gmask = ds.dsBitfields[1];
      bmask = ds.dsBitfields[2];
    }
    else
    {
      rmask = 0x00FF0000;
      gmask = 0x0000FF00;
      bmask = 0x000000FF;
    }

    rright = GetRightShiftCount(rmask);
    gright = GetRightShiftCount(gmask);
    bright = GetRightShiftCount(bmask);

    pdwBits = (DWORD*) ds.dsBm.bmBits;
    for (i = 0; i < abs(ds.dsBmih.biHeight); i++)
    {
      for (j = 0; j < ds.dsBmih.biWidth; j++)
      {
        dwColor = *pdwBits++;
        b = (BYTE) ((dwColor & bmask) >> bright);
        g = (BYTE) ((dwColor & gmask) >> gright);
        r = (BYTE) ((dwColor & rmask) >> rright);
        AddColor(&pTree, r, g, b, nColorBits, 0, &nLeafCount,
          pReducibleNodes);
        while (nLeafCount > nMaxColors)
          ReduceTree(nColorBits, &nLeafCount, pReducibleNodes);
      }
      pdwBits = (DWORD*) (((BYTE*) pdwBits) + nPad);
    }
    break;

  default: // DIB must be 16, 24, or 32-bit!
    return NULL;
  }

  if (nLeafCount > nMaxColors)
  {
    DeleteTree (&pTree);
    return NULL;
  }

  // Create a logical palette from the colors in the octree
  dwSize = sizeof (LOGPALETTE) + ((nLeafCount - 1) * sizeof (PALETTEENTRY));
  if ((plp = (LOGPALETTE*) HeapAlloc(GetProcessHeap (), 0, dwSize)) == NULL)
  {
    DeleteTree (&pTree);
    return NULL;
  }

  plp->palVersion = 0x300;
  plp->palNumEntries = (WORD) nLeafCount;
  nIndex = 0;
  GetPaletteColors(pTree, plp->palPalEntry, &nIndex);
  hPalette = CreatePalette(plp);

  HeapFree(GetProcessHeap(), 0, plp);
  DeleteTree(&pTree);
  return hPalette;
}

int CMagneticTitleDlg::GetRightShiftCount(DWORD dwVal)
{
  int i;

  for (i = 0; i < sizeof (DWORD) * 8; i++)
  {
    if (dwVal & 1)
      return i;
    dwVal >>= 1;
  }
  return -1;
}

int CMagneticTitleDlg::GetLeftShiftCount(DWORD dwVal)
{
  int nCount, i;

  nCount = 0;
  for (i = 0; i < sizeof (DWORD) * 8; i++)
  {
    if (dwVal & 1)
      nCount++;
    dwVal >>= 1;
  }
  return (8 - nCount);
}

void CMagneticTitleDlg::AddColor(NODE** ppNode, BYTE r, BYTE g, BYTE b, UINT nColorBits,
  UINT nLevel, UINT* pLeafCount, NODE** pReducibleNodes)
{
  int nIndex, shift;
  static BYTE mask[8] = { 0x80, 0x40, 0x20, 0x10, 0x08, 0x04, 0x02, 0x01 };

  // If the node doesn't exist, create it
  if (*ppNode == NULL)
      *ppNode = CreateNode(nLevel, nColorBits, pLeafCount, pReducibleNodes);

  // Update color information if it's a leaf node
  if ((*ppNode)->bIsLeaf)
  {
    (*ppNode)->nPixelCount++;
    (*ppNode)->nRedSum += r;
    (*ppNode)->nGreenSum += g;
    (*ppNode)->nBlueSum += b;
  }
  // Recurse a level deeper if the node is not a leaf
  else
  {
    shift = 7 - nLevel;
    nIndex = (((r & mask[nLevel]) >> shift) << 2) |
             (((g & mask[nLevel]) >> shift) << 1) |
              ((b & mask[nLevel]) >> shift);
    AddColor(&((*ppNode)->pChild[nIndex]), r, g, b, nColorBits,
      nLevel + 1, pLeafCount, pReducibleNodes);
  }
}

CMagneticTitleDlg::NODE* CMagneticTitleDlg::CreateNode(UINT nLevel, UINT nColorBits, UINT* pLeafCount,
  NODE** pReducibleNodes)
{
  NODE* pNode;

  if ((pNode = (NODE*) HeapAlloc(GetProcessHeap(), HEAP_ZERO_MEMORY,
    sizeof (NODE))) == NULL)
    return NULL;

  pNode->bIsLeaf = (nLevel == nColorBits) ? TRUE : FALSE;
  if (pNode->bIsLeaf)
    (*pLeafCount)++;
  else
  {
    // Add the node to the reducible list for this level
    pNode->pNext = pReducibleNodes[nLevel];
    pReducibleNodes[nLevel] = pNode;
  }
  return pNode;
}

void CMagneticTitleDlg::ReduceTree(UINT nColorBits, UINT* pLeafCount, NODE** pReducibleNodes)
{
  int i;
  NODE* pNode;
  UINT nRedSum, nGreenSum, nBlueSum, nChildren;

  // Find the deepest level containing at least one reducible node
  for (i=nColorBits - 1; (i>0) && (pReducibleNodes[i] == NULL); i--);

  // Reduce the node most recently added to the list at level i
  pNode = pReducibleNodes[i];
  pReducibleNodes[i] = pNode->pNext;

  nRedSum = nGreenSum = nBlueSum = nChildren = 0;
  for (i = 0; i < 8; i++)
  {
    if (pNode->pChild[i] != NULL)
    {
      nRedSum += pNode->pChild[i]->nRedSum;
      nGreenSum += pNode->pChild[i]->nGreenSum;
      nBlueSum += pNode->pChild[i]->nBlueSum;
      pNode->nPixelCount += pNode->pChild[i]->nPixelCount;
      HeapFree(GetProcessHeap (), 0, pNode->pChild[i]);
      pNode->pChild[i] = NULL;
      nChildren++;
    }
  }

  pNode->bIsLeaf = TRUE;
  pNode->nRedSum = nRedSum;
  pNode->nGreenSum = nGreenSum;
  pNode->nBlueSum = nBlueSum;
  *pLeafCount -= (nChildren - 1);
}

void CMagneticTitleDlg::DeleteTree(NODE** ppNode)
{
  int i;

  for (i = 0; i < 8; i++)
  {
    if ((*ppNode)->pChild[i] != NULL)
      DeleteTree(&((*ppNode)->pChild[i]));
  }
  HeapFree(GetProcessHeap(), 0, *ppNode);
  *ppNode = NULL;
}

void CMagneticTitleDlg::GetPaletteColors(NODE* pTree, PALETTEENTRY* pPalEntries, UINT* pIndex)
{
  int i;

  if (pTree->bIsLeaf)
  {
    pPalEntries[*pIndex].peRed =
      (BYTE) ((pTree->nRedSum) / (pTree->nPixelCount));
    pPalEntries[*pIndex].peGreen =
      (BYTE) ((pTree->nGreenSum) / (pTree->nPixelCount));
    pPalEntries[*pIndex].peBlue =
      (BYTE) ((pTree->nBlueSum) / (pTree->nPixelCount));
    (*pIndex)++;
  }
  else
  {
    for (i = 0; i < 8; i++)
    {
      if (pTree->pChild[i] != NULL)
        GetPaletteColors(pTree->pChild[i], pPalEntries, pIndex);
    }
  }
}

/////////////////////////////////////////////////////////////////////////////
// Start and stop the thread which plays MPEG Layer3 audio files
/////////////////////////////////////////////////////////////////////////////

void CMagneticTitleDlg::StartMusic(LPCTSTR pszGamePath)
{
  CString strGamePath(pszGamePath);

  if (strGamePath.GetLength() > 4)
  {
    m_strMusic = strGamePath.Left(strGamePath.GetLength()-4);
    m_strMusic += ".mp3";
    m_bMusicHalt = FALSE;

    m_pMusicThread = AfxBeginThread(MusicThread,NULL,
      THREAD_PRIORITY_NORMAL,0,CREATE_SUSPENDED);
    if (m_pMusicThread)
    {
      m_pMusicThread->m_bAutoDelete = FALSE;
      m_pMusicThread->ResumeThread();
    }
  }
}

void CMagneticTitleDlg::StopMusic()
{
  if (m_pMusicThread)
  {
    m_bMusicHalt = TRUE;
    WaitForSingleObject(m_pMusicThread->m_hThread,INFINITE);
  }
}

CString CMagneticTitleDlg::m_strMusic;
BOOL CMagneticTitleDlg::m_bMusicHalt;

UINT CMagneticTitleDlg::MusicThread(LPVOID)
{
  sbinfile AudioFile;
  if (AudioFile.open(m_strMusic,sbinfile::openro) < 0)
    return 1;

  abinfile AWaveData;
  binfile *WaveData = &AudioFile;
  if (AudioFile.getmode() & binfile::modeseek)
  {
    char riff[4];
    AudioFile.seekend(-128);
    AudioFile.read(riff,3);
    AWaveData.open(AudioFile,0,AudioFile.length()-(memcmp(riff,"TAG",3) ? 0 : 128));
    WaveData = &AWaveData;
  }

  int down = 0;
  int chn = 0;
  int freq,stereo;

  ampegdecoder Decoder;
  if (Decoder.open(*WaveData,freq,stereo,1,down,chn))
    return 1;

  ntplaybinfile AudioPlayer;
  if (AudioPlayer.open(freq,stereo,1,4608*4,32))
    return 1;

  float vol = 1;
  float bal = 0;
  float ctr = 0;
  float sep = 1;
  float srnd = 1;

  float vols[3][3];
  vols[0][0] = (float)(0.5*(1.0-bal)*(1.0-ctr+sep));
  vols[0][1] = (float)(0.5*(1.0-bal)*(1.0+ctr-sep));
  vols[0][2] = (float)(1-bal);
  vols[1][0] = (float)(0.5*(1.0+bal)*(1.0-ctr-sep)*srnd);
  vols[1][1] = (float)(0.5*(1.0+bal)*(1.0+ctr+sep)*srnd);
  vols[1][2] = (float)((1.0+bal)*srnd);
  vols[2][0] = (float)(0.5*(1.0-ctr));
  vols[2][1] = (float)(0.5*(1.0+ctr));
  vols[2][2] = (float)(1.0);
  Decoder.ioctl(Decoder.ioctlsetstereo,vols,0);
  vols[0][0]=vol;
  Decoder.ioctl(Decoder.ioctlsetvol,&vols[0][0],0);

  static float l3equal[576];
  float pitch = 0;
  float pitch0 = 1764;
  int i;

  for (i = 0; i < 576; i++)
    l3equal[i] = (float)exp(pitch*log((i+0.5)*freq/(576.0*2*pitch0)));
  Decoder.ioctl(Decoder.ioctlsetequal576,l3equal,0);

  short sampbuf[2304];
  peekch(Decoder);
  while (m_bMusicHalt == FALSE)
  {
    int l = Decoder.read(sampbuf,4608);
    if (!l)
      break;
    AudioPlayer.write(sampbuf,l);
  }

  Decoder.close();
  WaveData->close();
  AudioFile.close();
  if (m_bMusicHalt)
    AudioPlayer.abort();
  else
    AudioPlayer.close();

  return 0;
}
