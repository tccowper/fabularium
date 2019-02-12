/***********************************************************************\
*
* Level 9 interpreter
* Version 5.1
* Copyright (c) 1996-2011 Glen Summers and contributors.
* Contributions from David Kinder, Alan Staniforth, Simon Baldwin,
* Dieter Baron and Andreas Scherrer.
*
* Level9 32 bit Windows version by Glen Summers and David Kinder.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation; either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111, USA.
*
\***********************************************************************/

#include <mywin.h>
#pragma hdrstop
#include <htmlhelp.h>

#include <ctype.h>

#include "level9.h"

// define application name, main window title
#define AppName "Level9"
#define MainWinTitle "Level9"
 
// help file name and ini file are set from AppName
char HelpFileName[] = AppName".chm";

#ifdef WIN16
char Ini[] = AppName".ini";
#else
char Ini[] = "Software\\Level 9\\Interpreter";
#endif

#ifdef __BORLANDC__
#include "level9.rh"
#endif

#ifdef _MSC_VER
#include "resource.h"
#endif

String Output="";
int Line=0;
int LineOffset=0;
int LineStart=0;
int LastWordEnd=0;
HWND hWndMain=0;
HRGN hClip=0;
int GfxMode=0;
int GfxHeight=0;
int GfxPicWidth=0,GfxPicHeight=0;
HBITMAP hGfx=0,hGfxDraw=0;
HDC hGfxDC=0,hGfxDrawDC=0;
int FontHeight=0,LineSpacing=0;
LOGFONT lf;
HFONT Font=0;
COLORREF FontColour;
int PageWidth=0,PageHeight=0,WndHeight=0;
int Margin=0;
SimpleList<int> InputChars;
int iPos=0,Input=0;
String Hash(20);
FName LastFile;

/*#define L9PRINT*/

void LogPrint(char *Str,int Len)
{
#ifdef L9PRINT
  static FILE* log = NULL;
  if (log == NULL)
    log = fopen("c:\\temp\\level9.txt","wt");
  fprintf(log,"%.*s\n",Len,Str);
#endif
}

void DrawPicture(void);

void DisplayLine(int Line,char *Str,int Len)
{
  HDC dc=GetDC(hWndMain);
  HFONT OldFont=(HFONT) SelectObject(dc,Font);
  COLORREF OldCol=SetTextColor(dc,FontColour);
  COLORREF OldBk=SetBkColor(dc,GetSysColor(COLOR_WINDOW));
  if(hClip) SelectClipRgn(dc,hClip);
  TextOut(dc,Margin,Line*LineSpacing-LineOffset+GfxHeight,Str,Len);
  SelectObject(dc,OldFont);
  SetTextColor(dc,OldCol);
  SetBkColor(dc,OldBk);
  ReleaseDC(hWndMain,dc);
}

void DisplayLineJust(int Line,char *Str,int Len)
{
  HDC dc=GetDC(hWndMain);
  HFONT OldFont=(HFONT) SelectObject(dc,Font);
  COLORREF OldCol=SetTextColor(dc,FontColour);
  COLORREF OldBk=SetBkColor(dc,GetSysColor(COLOR_WINDOW));
  if(hClip) SelectClipRgn(dc,hClip);

  SIZE Size;
#ifdef WIN32
  GetTextExtentPoint32(dc,Str,Len,&Size);
#else
  GetTextExtentPoint(dc,Str,Len,&Size);
#endif

  // count spaces
  int nBreaks=0;
  char *Ptr=Str;
  for (int i=0;i<Len;i++) if (*Ptr++==' ') nBreaks++;
  if (nBreaks) SetTextJustification(dc,PageWidth-Size.cx-2*Margin,nBreaks);

  TextOut(dc,Margin,Line*LineSpacing-LineOffset+GfxHeight,Str,Len);

  SelectObject(dc,OldFont);
  SetTextColor(dc,OldCol);
  SetBkColor(dc,OldBk);
  ReleaseDC(hWndMain,dc);
}

int LineLength(char*str,int n)
{
  SIZE Size;
  HDC dc=GetDC(hWndMain);
  HFONT OldFont=(HFONT) SelectObject(dc,Font);
#ifdef WIN32
  GetTextExtentPoint32(dc,str,n,&Size);
#else
  GetTextExtentPoint(dc,str,n,&Size);
#endif
  SelectObject(dc,OldFont);
  ReleaseDC(hWndMain,dc);
  return Size.cx;
}

BOOL Caret=FALSE;
int Cursorx,Cursory;

void MakeCaret()
{
  if (GetFocus()==hWndMain && Caret)
  {
    CreateCaret(hWndMain, NULL, 2, FontHeight);
    SetCaretPos(Cursorx,Cursory+GfxHeight);
    ShowCaret(hWndMain);
  }
}

void KillCaret()
{
  if (Caret) DestroyCaret();
}

void SetCaret(int x,int y)
{
  Cursorx=x;
  Cursory=y;
  SetCaretPos(x,y+GfxHeight);
}

void Wait(int millis)
{
  if (hGfxDC)
  {
    int gfx_status = TRUE;
    int gfx_count = 0;
    int gfx_limit = 2*millis;

    while (gfx_status && (gfx_count < gfx_limit))
    {
      gfx_status = RunGraphics();
      if (gfx_status)
        gfx_count++;
    }

    if (gfx_count > 0)
      DrawPicture();
  }

#ifdef WIN32
  Sleep(millis);
#endif
}

char os_readchar(int millis)
{
  SimpleNode<int> *C;
  App::PeekLoop();
  if ((C=InputChars.GetFirst())==NULL)
  {
    Wait(millis);
    App::PeekLoop();
    if ((C=InputChars.GetFirst())==NULL) return 0;
  }
  char InputChar=(char)C->Item;
  delete C;
  return InputChar;
}

L9BOOL os_stoplist(void)
{
  return (os_readchar(0) != 0);
}

SimpleList<String> History;
int HistoryLines=0;
#define NUMHIST 20

void Erase(int Start,int End)
{
  RECT rc;
  rc.left=Margin+Start;
  rc.right=Margin+End;
  rc.top=Line*LineSpacing-LineOffset+GfxHeight;
  rc.bottom=Line*LineSpacing+FontHeight-LineOffset+GfxHeight;
  HDC dc=GetDC(hWndMain);
  if(hClip) SelectClipRgn(dc,hClip);
#ifdef WIN32
  FillRect(dc,&rc,(HBRUSH) GetClassLong(hWndMain,GCL_HBRBACKGROUND));
#else
  FillRect(dc,&rc,(HBRUSH) GetClassLong(hWndMain,GCW_HBRBACKGROUND));
#endif
  ReleaseDC(hWndMain,dc);
}

void CancelInput()
{
  if (Caret) InputChars.AddTail(-1);
}

void HashCommand(char *h)
{
  if (Caret)
  {
    Hash=h;
    InputChars.AddTail(-2);
  }
}

BOOL os_input(char*ibuff,int size)
{
  slIterator<String> HistPtr(History);
  if (HistPtr()) HistPtr--; // force out

  Input=Output.Len();
  iPos=0;
  Caret=TRUE;
  MakeCaret();
  SetCaret(Margin+LineLength(Output+LineStart,Input-LineStart),Line*LineSpacing-LineOffset);

  while (TRUE)
  {
    SimpleNode<int> *C;
    while ((C=InputChars.GetFirst())==NULL)
    {
      Wait(20);
      App::PeekLoop();
    }
    int InputChar=C->Item;
    delete C;
    if (InputChar<0)
    {
      strcpy(ibuff,Hash);
      KillCaret();
      Caret=FALSE;
      return InputChar<-1;
    }
    else if (InputChar=='\r')
    {
      strcpy(ibuff,Output+Input); // >500 chrs??
      KillCaret();
      Caret=FALSE;
      HistPtr.Last();
      if (*ibuff && (!HistPtr() || HistPtr.Get()!=ibuff))
      {
        History.AddTailRef(String(ibuff));
        if (HistoryLines==NUMHIST)
        delete History.GetFirst();
        else HistoryLines++;
      }
      return TRUE;
    }
    else switch (InputChar)
    {
      case 256+VK_DELETE:
        if (iPos>=Output.Len()-Input) break;
        iPos++;
      case 8:
        if (iPos>0)
        {
          int OldLen=LineLength(Output+LineStart,Output.Len()-LineStart);
          Output.Remove(Input+--iPos,1);
          int Len=LineLength(Output+LineStart,Output.Len()-LineStart);
          Erase(Len,OldLen);
        }
        break;
      case 256+VK_UP:
        if (!HistPtr()) HistPtr.Last();
        else
        {
          --HistPtr;
          if (!HistPtr()) HistPtr.First();
        }
        if (HistPtr())
        {
          int OldLen=LineLength(Output+LineStart,Output.Len()-LineStart);
          Output.Len(Input);
          Output.Insert(HistPtr.Get(),Input);
          iPos=Output.Len()-Input;
          int Len=LineLength(Output+LineStart,Output.Len()-LineStart);
          Erase(Len,OldLen);
        }
        break;
      case 256+VK_DOWN:
        if (HistPtr())
        {
          ++HistPtr;
          if (!HistPtr()) HistPtr.Last();
          else
          {
            int OldLen=LineLength(Output+LineStart,Output.Len()-LineStart);
            Output.Len(Input);
            Output.Insert(HistPtr.Get(),Input);
            iPos=Output.Len()-Input;
            int Len=LineLength(Output+LineStart,Output.Len()-LineStart);
            Erase(Len,OldLen);
          }
        }
        break;
      case 256+VK_LEFT:
        if (iPos>0) iPos--;
        break;
      case 256+VK_RIGHT:
        if (iPos<Output.Len()-Input) iPos++;
        break;
      case 256+VK_END:
        iPos=Output.Len()-Input;
        break;
      case 256+VK_HOME:
        iPos=0;
        break;
      case 26: // escape (clear?)
        break;

      default:
        // insert char at Pos;
        if (InputChar<256) Output.Insert((char)InputChar,Input+iPos++);
        break;
    }

    HideCaret(hWndMain);
    DisplayLine(Line,Output+LineStart,Output.Len()-LineStart);
    SetCaret(Margin+LineLength(Output+LineStart,Input-LineStart+iPos),Line*LineSpacing-LineOffset);
    ShowCaret(hWndMain);
  }
}

int FindLineLength(int LineStart)
{
  int LastWordEnd=0;
  int Pos=LineStart;
  char c;
  while (TRUE)
  {
    c= Output[Pos++];
    if (c=='\r' || c==' ' || c==0)
    {
      if (LineLength((char*)Output+LineStart,Pos-LineStart-1)>PageWidth-2*Margin) return LastWordEnd+1;
      else if (c==0) return Pos-1;
      else if (c=='\r') return Pos;
      LastWordEnd=Pos-LineStart-1;
    }
  }
}

#define SCROLLBACK 2000

void NewLine()
{
  while (Output.Len()>SCROLLBACK)
  {
    int Len=FindLineLength(0);
    Output.Remove(0,Len);
    LineStart-=Len;
    LineOffset-=LineSpacing;
    Line--;
  }
  Line++;

  if (Line*LineSpacing+FontHeight-LineOffset>PageHeight)
  {
    int Shift=Line*LineSpacing+FontHeight-LineOffset-PageHeight;
    RECT rc;
    rc.left=0;
    rc.top=GfxHeight;
    rc.right=PageWidth;
    rc.bottom=PageHeight+GfxHeight;
    ScrollWindow(hWndMain,0,-Shift,NULL,&rc);
    LineOffset+=Shift;
    UpdateWindow(hWndMain);
  }
}

void os_flush()
{
  if (LineLength((char*)Output+LineStart,Output.Len()-LineStart)>PageWidth-2*Margin)
  {
    LogPrint((char*) Output+LineStart,LastWordEnd);
    DisplayLineJust(Line,(char*) Output+LineStart,LastWordEnd);
    LineStart+=LastWordEnd+1;
    NewLine();
  }
  DisplayLine(Line,(char*) Output+LineStart,Output.Len()-LineStart);
  LastWordEnd=Output.Len()-LineStart;
}

void os_printchar(char c)
{
  Output << c;
  if (c=='\r' || c==' ')
  {
    if (LineLength((char*)Output+LineStart,Output.Len()-LineStart-1)>PageWidth-2*Margin)
    {
      LogPrint((char*) Output+LineStart,LastWordEnd);
      DisplayLineJust(Line,(char*) Output+LineStart,LastWordEnd);
      LineStart+=LastWordEnd+1;
      NewLine();
    }
    if (c=='\r')
    {
      LogPrint((char*) Output+LineStart,Output.Len()-LineStart-1);
      DisplayLine(Line,(char*) Output+LineStart,Output.Len()-LineStart-1);
      LineStart=Output.Len();
      NewLine();
    }
    LastWordEnd=Output.Len()-LineStart-1;
  }
}

void Redraw()
{
  int l=0;
  int LineStart=0,LastWordEnd=0;
  int Pos=0;
  char c;
  do
  {
    c= Output[Pos++];
    if (c=='\r' || c==' ' || c==0)
    {
      if (LineLength((char*)Output+LineStart,Pos-LineStart-1)>PageWidth-2*Margin)
      {
        DisplayLineJust(l,(char*) Output+LineStart,LastWordEnd);
        LineStart+=LastWordEnd+1;
        l++;
      }
      if (c=='\r' || c==0)
      {
        DisplayLine(l,(char*) Output+LineStart,Pos-LineStart-1);
        if (c=='\r')
        {
          LineStart=Pos;
          l++;
        }
      }
      LastWordEnd=Pos-LineStart-1;
    }
  } while (c);
  DrawPicture();
}

void Paginate()
{
  int l=0;
  int LineStart=0,LastWordEnd=0;
  int Pos=0;
  char c;
  do
  {
    c= Output[Pos++];
    if (c=='\r' || c==' ' || c==0)
    {
      if (LineLength((char*)Output+LineStart,Pos-LineStart-1)>PageWidth-2*Margin)
      {
        LineStart+=LastWordEnd+1;
        l++;
      }
      if (c=='\r')
      {
        LineStart=Pos;
        l++;
      }
      LastWordEnd=Pos-LineStart-1;
    }
  } while (c);
  Line=l;
  LineOffset=max(0,Line*LineSpacing+FontHeight-PageHeight);

  if (Caret) SetCaret(Margin+LineLength(Output+LineStart,Input-LineStart+iPos),Line*LineSpacing-LineOffset);
}

void Resize()
{
  PageHeight = WndHeight;
  if (GfxMode)
  {
    GfxHeight = WndHeight/2;
    PageHeight -= GfxHeight;
  }
  else
    GfxHeight = 0;

  if (hClip)
    DeleteObject(hClip);
  hClip=CreateRectRgn(0,GfxHeight,PageWidth,WndHeight);

  if (hGfxDraw)
    DeleteObject(hGfxDraw);
  hGfxDraw = 0;
  if (hGfxDrawDC)
    DeleteDC(hGfxDrawDC);
  hGfxDrawDC = 0;

  Paginate();
  InvalidateRect(hWndMain,NULL,TRUE);
}

const char Filters[]="Level 9 Game Files (*.dat)\0*.dat\0Spectrum Snapshots (*.sna)\0*.sna\0All Files (*.*)\0*.*\0\0";
int FiltIndex;
const char GameFilters[]="Saved game file (*.sav)\0*.sav\0All Files (*.*)\0*.*\0\0";
FName LastGameFile;
int GameFiltIndex;

void os_set_filenumber(char *NewName,int Size,int n)
{
  FName fn(NewName);
  String S;
  fn.GetBaseName(S);
  while (isdigit(S.Last())) S.Remove(S.Len()-1,1);
  fn.NewBaseName(S << n);
  strcpy(NewName,fn);
}

BOOL os_get_game_file(char* Name,int Size)
{
  return CustFileDlg(App::MainWindow,-1,Name,Size,"Load Level 9 Game File",Filters,&FiltIndex).Execute(FALSE);
}

BOOL os_save_file(BYTE *Ptr,int Bytes)
{
  CancelInput();
  if (CustFileDlg(App::MainWindow,-1,LastGameFile,LastGameFile.Size(),"Save Current Position",GameFilters,&GameFiltIndex,OFN_OVERWRITEPROMPT | OFN_EXPLORER).Execute(FALSE))
  {
    LastGameFile.Update();
    if (!LastGameFile.GetExt()) LastGameFile.NewExt("sav");

    FILE *f=fopen(LastGameFile,"wb");
    if (f)
    {
      fwrite(Ptr,1,Bytes,f);
      fclose(f);
      return TRUE;
    }
  }
  return FALSE;
}

BOOL os_load_file(BYTE *Ptr,int *Bytes,int Max)
{
  CancelInput();
  if (CustFileDlg(App::MainWindow,-1,LastGameFile,LastGameFile.Size(),"Restore Saved Position",GameFilters,&GameFiltIndex,OFN_FILEMUSTEXIST | OFN_EXPLORER).Execute(FALSE))
  {
    LastGameFile.Update();
    FILE *f=fopen(LastGameFile,"rb");
    if (f)
    {
      *Bytes=filelength(f);
      if (*Bytes>Max)
        MessageBox(App::MainWindow->hWnd,"Not a valid saved game file","Load Error",MB_OK | MB_ICONEXCLAMATION);
      else
      {
        fread(Ptr,1,*Bytes,f);
        fclose(f);
        return TRUE;
      }
    }
  }
  return FALSE;
}

const char AllFilters[]="All Files (*.*)\0*.*\0\0";
FName LastScriptFile;
int ScriptFiltIndex;

FILE* os_open_script_file(void)
{
  CancelInput();
  if (CustFileDlg(App::MainWindow,-1,LastScriptFile,LastScriptFile.Size(),"Play Back Script File",AllFilters,&ScriptFiltIndex,OFN_FILEMUSTEXIST | OFN_EXPLORER).Execute(FALSE))
  {
    LastScriptFile.Update();
    return fopen(LastScriptFile,"rt");
  }
  return FALSE;
}

typedef struct tagBITMAPINFO32
{
  BITMAPINFOHEADER bmiHeader; 
  RGBQUAD bmiColors[32];
}
BITMAPINFO32;

RGBQUAD Colours[8] =
{
  { 0x00,0x00,0x00,0 },
  { 0x00,0x00,0xFF,0 },
  { 0x30,0xE8,0x30,0 },
  { 0x00,0xFF,0xFF,0 },
  { 0xFF,0x00,0x00,0 },
  { 0x00,0x68,0xA0,0 },
  { 0xFF,0xFF,0x00,0 },
  { 0xFF,0xFF,0xFF,0 }
};
RGBQUAD Palette[32];

COLORREF GetIndexColour(int index)
{
  int x = 8*index;
  return RGB(x,x,x);
}

void SetIndexPalette(RGBQUAD* pal)
{
  for (int i = 0; i < 32; i++)
  {
    pal[i].rgbBlue = 8*i;
    pal[i].rgbGreen = 8*i;
    pal[i].rgbRed = 8*i;
  }
}

void DrawPicture(void)
{
  if (hGfxDC)
  {
    if (hGfxDrawDC == 0)
    {
      hGfxDrawDC = CreateCompatibleDC(hGfxDC);
      SetStretchBltMode(hGfxDrawDC,COLORONCOLOR);

      BITMAPINFO32 info;
      ZeroMemory(&info,sizeof(BITMAPINFO32));
      info.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
      info.bmiHeader.biPlanes = 1;
      info.bmiHeader.biBitCount = 8;
      info.bmiHeader.biCompression = BI_RGB;
      info.bmiHeader.biWidth = PageWidth;
      info.bmiHeader.biHeight = GfxHeight * -1;
      info.bmiHeader.biClrUsed = 32;
      info.bmiHeader.biClrImportant = 32;
      SetIndexPalette(info.bmiColors);

      VOID* bits;
      hGfxDraw = CreateDIBSection(hGfxDrawDC,(BITMAPINFO*)&info,DIB_RGB_COLORS,&bits,NULL,0);
      SelectObject(hGfxDrawDC,hGfxDraw);
    }

    RGBQUAD pal[32];
    SetIndexPalette(pal);
    SetDIBColorTable(hGfxDrawDC,0,32,pal);

    RECT rc;
    rc.left = 0;
    rc.right = PageWidth;
    rc.top = 0;
    rc.bottom = GfxHeight;
    FillRect(hGfxDrawDC,&rc,(HBRUSH)GetStockObject(BLACK_BRUSH));

    if (GfxMode == 2)
    {
      int w = GfxPicWidth;
      int h = GfxPicHeight;

      if (w < 512)
      {
        w *= 2;
        h *= 2;
      }

      int x = (PageWidth-w)/2;
      int y = (GfxHeight-h)/2;
      if (x < 0)
        x = 0;
      if (y < 0)
        y = 0;

      StretchBlt(hGfxDrawDC,x,y,w,h,
        hGfxDC,0,0,GfxPicWidth,GfxPicHeight,SRCCOPY);
    }
    else
    {
      StretchBlt(hGfxDrawDC,0,0,PageWidth,GfxHeight,
        hGfxDC,0,0,GfxPicWidth,GfxPicHeight,SRCCOPY);
    }

    SetDIBColorTable(hGfxDrawDC,0,32,Palette);
    HDC dc = GetDC(hWndMain);
    BitBlt(dc,0,0,PageWidth,GfxHeight,hGfxDrawDC,0,0,SRCCOPY);
    ReleaseDC(hWndMain,dc);
  }
}

FName GfxDir;
BitmapType GfxBmapType = NO_BITMAPS;
BYTE* GfxBits = NULL;
int GfxBmapWidth = 0, GfxBmapHeight = 0;
int LastBitmap = -1, DelayBitmap = 0;

void os_graphics(int mode)
{
  GfxBmapType = NO_BITMAPS;
  LastBitmap = -1;

  switch (mode)
  {
  case 0:
    GfxMode = 0;
    break;
  case 1:
    GfxMode = 1;
    break;
  case 2:
    LastFile.GetDir(GfxDir);
    GfxDir.AddChar('\\');
    GfxBmapType = DetectBitmaps(GfxDir);
    GfxMode = (GfxBmapType != NO_BITMAPS) ? 2 : 0;
    break;
  }

  Resize();

  if (hGfx)
    DeleteObject(hGfx);
  hGfx = 0;
  GfxBits = NULL;
  if (hGfxDC)
    DeleteDC(hGfxDC);
  hGfxDC = 0;

  if (GfxMode)
  {
    HDC dc = GetDC(hWndMain);
    hGfxDC = CreateCompatibleDC(dc);
    SetStretchBltMode(hGfxDC,COLORONCOLOR);
    ReleaseDC(hWndMain,dc);

    if (GfxMode == 2)
    {
      GfxBmapWidth = MAX_BITMAP_WIDTH;
      GfxBmapHeight = MAX_BITMAP_HEIGHT;
      GfxPicWidth = 0;
      GfxPicHeight = 0;
    }
    else
    {
      GetPictureSize(&GfxBmapWidth,&GfxBmapHeight);
      GfxPicWidth = GfxBmapWidth;
      GfxPicHeight = GfxBmapHeight;
    }

    BITMAPINFO32 info;
    ZeroMemory(&info,sizeof(BITMAPINFO32));
    info.bmiHeader.biSize = sizeof(BITMAPINFOHEADER);
    info.bmiHeader.biPlanes = 1;
    info.bmiHeader.biBitCount = 8;
    info.bmiHeader.biCompression = BI_RGB;
    info.bmiHeader.biWidth = GfxBmapWidth;
    info.bmiHeader.biHeight = GfxBmapHeight * -1;
    info.bmiHeader.biClrUsed = 32;
    info.bmiHeader.biClrImportant = 32;
    SetIndexPalette(info.bmiColors);

    hGfx = CreateDIBSection(hGfxDC,(BITMAPINFO*)&info,DIB_RGB_COLORS,(VOID**)&GfxBits,NULL,0);
    SelectObject(hGfxDC,hGfx);
  }
}

void os_cleargraphics(void)
{
  if (hGfxDC)
  {
    LastBitmap = -1;

    RECT rc;
    rc.left = 0;
    rc.right = GfxBmapWidth;
    rc.top = 0;
    rc.bottom = GfxBmapHeight;

    HBRUSH br = CreateSolidBrush(GetIndexColour(0));
    HGDIOBJ old = SelectObject(hGfxDC,br);
    FillRect(hGfxDC,&rc,br);
    SelectObject(hGfxDC,old);
    DeleteObject(br);
  }
}

void os_setcolour(int colour, int index)
{
  Palette[colour] = Colours[index];
}

COLORREF LineColour1 = 0;
COLORREF LineColour2 = 0;

VOID CALLBACK LineProc(int x, int y, LPARAM)
{
  if (GetPixel(hGfxDC,x,y) == LineColour2)
    SetPixel(hGfxDC,x,y,LineColour1);
}

void os_drawline(int x1, int y1, int x2, int y2, int colour1, int colour2)
{
  if (hGfxDC)
  {
    LineColour1 = GetIndexColour(colour1);
    LineColour2 = GetIndexColour(colour2);
    LineDDA(x1,y1,x2,y2,LineProc,NULL);
    LineProc(x2,y2,NULL);
  }
}

void os_fill(int x, int y, int colour1, int colour2)
{
  if (hGfxDC)
  {
    COLORREF colour = GetIndexColour(colour2);
    if (GetPixel(hGfxDC,x,y) == colour)
    {
      HBRUSH br = CreateSolidBrush(GetIndexColour(colour1));
      HGDIOBJ old = SelectObject(hGfxDC,br);
      ExtFloodFill(hGfxDC,x,y,colour,FLOODFILLSURFACE);
      SelectObject(hGfxDC,old);
      DeleteObject(br);
    }
  }
}

void os_show_bitmap(int pic, int x, int y)
{
  if ((DelayBitmap == -1) && (pic > 0) && (x == 0) && (y == 0))
  {
    DelayBitmap = pic;
    return;
  }
  DelayBitmap = 0;

  if (LastBitmap == pic)
    return;

  if ((GfxMode == 2) && GfxBits)
  {
    Bitmap* bitmap = DecodeBitmap(GfxDir,GfxBmapType,pic,x,y);
    if (bitmap)
    {
      GfxPicWidth = bitmap->width;
      GfxPicHeight = bitmap->height;

      memset(GfxBits,0,GfxBmapWidth*GfxBmapHeight);
      for (int y = 0; y < GfxPicHeight; y++)
        memcpy(GfxBits+(y*GfxBmapWidth),bitmap->bitmap+(y*GfxPicWidth),GfxPicWidth);

      for (int i = 0; i < bitmap->npalette; i++)
      {
        Palette[i].rgbRed = bitmap->palette[i].red;
        Palette[i].rgbGreen = bitmap->palette[i].green;
        Palette[i].rgbBlue = bitmap->palette[i].blue;
        Palette[i].rgbReserved = 0;
      }

      LastBitmap = pic;
      DrawPicture();

      if (pic == 0)
      {
        SetTimer(hWndMain,1,2000,NULL);
        DelayBitmap = -1;
      }
    }
  }
}

// About Dialog ***************************************

class AboutDialog : public Dialog
{
public:
  AboutDialog(Object *Parent) : Dialog(Parent,IDD_ABOUT,"AboutDialog") {}

  BOOL SetupWindow()
  {
    HWND logoWnd = GetDlgItem(hWnd,IDC_LOGO);
    RECT logoRect;
    GetWindowRect(logoWnd,&logoRect);
    ScreenToClient(hWnd,(LPPOINT)&logoRect);
    ScreenToClient(hWnd,((LPPOINT)&logoRect)+1);
    double aspect = ((double)(logoRect.right-logoRect.left))/(logoRect.bottom-logoRect.top);

    HWND groupWnd = GetDlgItem(hWnd,IDC_GROUP);
    RECT groupRect;
    GetWindowRect(groupWnd,&groupRect);
    ScreenToClient(hWnd,(LPPOINT)&groupRect);
    ScreenToClient(hWnd,((LPPOINT)&groupRect)+1);

    logoRect.right = groupRect.left-logoRect.left;
    logoRect.bottom = logoRect.top+(int)((logoRect.right-logoRect.left)/aspect);
    MoveWindow(logoWnd,logoRect.left,logoRect.top,
      logoRect.right-logoRect.left,logoRect.bottom-logoRect.top,TRUE);
    return TRUE;
  }
};

// MainWindow *****************************************

class MainWindow : public HashWindow
{
public:
  MainWindow(Object *Parent,char *Title);
  ~MainWindow();

  BOOL Playing;

  void Destroy();
  BOOL SetupWindow();
  void OpenFile(char *name);

  void CmHelpContents();
  void CmAbout();
  void CmExit();
  void CmOpen();
  void CmSelectFont();
  void CmRestore() { HashCommand("#restore"); }
  void CmSave() { HashCommand("save"); }
  void CmDictionary() { HashCommand("#dictionary"); }
  void CmPaste();

  void SetFont();
  void DelFonts();

// message response functions

  BOOL LButtonDown(TMSG &);
  BOOL RButtonDown(TMSG &);
  BOOL LButtonUp(TMSG &);
  BOOL WMMouseMove(TMSG &);
  BOOL WMSize(TMSG &);

  BOOL WMKeyDown(TMSG &);
  BOOL WMChar(TMSG &);
  BOOL WMSetFocus(TMSG&);
  BOOL WMKillFocus(TMSG&);
  BOOL WMTimer(TMSG&);

// window paint request
  void Paint(HDC, BOOL, RECT&);

// enable message response
  HASH_EV_ENABLE(MainWindow)
};

// define response functions
EV_START(MainWindow)
// command messages
  EV_COMMAND(CM_ABOUT, CmAbout)
  EV_COMMAND(CM_HELPCONTENTS, CmHelpContents)
  EV_COMMAND(CM_EXIT, CmExit)
  EV_COMMAND(CM_OPEN, CmOpen)
  EV_COMMAND(CM_FONT, CmSelectFont)
  EV_COMMAND(CM_FILELOAD, CmRestore)
  EV_COMMAND(CM_FILESAVE, CmSave)
  EV_COMMAND(CM_DICTIONARY, CmDictionary)
  EV_COMMAND(CM_PASTE, CmPaste)

// windows messages
  EV_MESSAGE(WM_LBUTTONDOWN, LButtonDown)
  EV_MESSAGE(WM_RBUTTONDOWN, RButtonDown)
  EV_MESSAGE(WM_MOUSEMOVE, WMMouseMove)
  EV_MESSAGE(WM_LBUTTONUP, LButtonUp)
  EV_MESSAGE(WM_KEYDOWN, WMKeyDown)
  EV_MESSAGE(WM_CHAR, WMChar)
  EV_MESSAGE(WM_SIZE, WMSize)
  EV_MESSAGE(WM_SETFOCUS, WMSetFocus)
  EV_MESSAGE(WM_KILLFOCUS, WMKillFocus)
  EV_MESSAGE(WM_TIMER, WMTimer)

EV_END

// main window constructor
MainWindow::MainWindow(Object *Parent,char *Title) : HashWindow(Parent,Title,"MainWindow")
{
  // set window style
  Style=WS_OVERLAPPEDWINDOW;

#ifdef __BORLANDC__
  // give it a menu and icon
  AssignMenu(IDM_MENU);
  SetIcon(IDI_ICON);
#endif

  Flags|=W_OVERSCROLL;
}

// this is called to setup the window
BOOL MainWindow::SetupWindow()
{
#ifdef WIN32
  SetWindowLong(hWnd,GWL_EXSTYLE,WS_EX_OVERLAPPEDWINDOW);
#endif

#ifdef _MSC_VER
  AssignMenu(IDM_MENU);
  SetIcon(IDI_ICON);
#endif

  // load window pos from ini file (will also be automatically saved on exit)
  GetWindowState();
  SetMru(hWnd,CM_EXIT);
  hWndMain=hWnd;
  SetFont();
  Playing=FALSE;

  return TRUE;
}

void MainWindow::CmSelectFont()
{
  CHOOSEFONT cf;

  cf.lStructSize=sizeof(cf);
  cf.hwndOwner=hWnd;
  cf.lpLogFont=&lf;
  cf.rgbColors=FontColour;

  cf.Flags=CF_INITTOLOGFONTSTRUCT | CF_SCREENFONTS | CF_EFFECTS;

  if (ChooseFont(&cf))
  {
    DelFonts();
    SetFont();
    FontColour=cf.rgbColors;
    KillCaret();
    Paginate();
    InvalidateRect(hWnd,NULL,TRUE);
    MakeCaret();
  }
}

void MainWindow::SetFont()
{
  Font=CreateFontIndirect(&lf);
  HDC dc=GetDC(hWnd);
  HFONT OldFont=(HFONT) SelectObject(dc,Font);
  TEXTMETRIC tm;
  GetTextMetrics(dc,&tm);
  FontHeight=tm.tmHeight;
  Margin=tm.tmAveCharWidth;
  LineSpacing=(int) 1.1*FontHeight;
  SelectObject(dc,OldFont);
  ReleaseDC(hWnd,dc);
}

void MainWindow::DelFonts()
{
  DeleteObject(Font);
}

// this is called when the window is destroyed
void MainWindow::Destroy()
{
  // close help if open
  DelFonts();
  StopGame();
  Playing=FALSE;
  FreeMemory();
  CancelInput();
}

MainWindow::~MainWindow()
{
}

void MainWindow::OpenFile(char *name)
{
  // in input routine?, cause fall through
  CancelInput();
  // clear buffers etc..., even if fails as invalidates game memory
  Output="";
  LineStart=LastWordEnd=0;
  Paginate();
  InvalidateRect(hWnd,NULL,TRUE);

  // look for a picture datafile
  FName picname(name);
  picname.NewExt("pic");
  if (!picname.Exist()) picname.NewExt("cga");
  if (!picname.Exist()) picname.NewExt("hrc");
  if (!picname.Exist()) picname.NewName("picture.dat");

  if (!LoadGame(name,picname)) MessageBox(hWnd,"Unable to load game file","Load Error",MB_OK | MB_ICONEXCLAMATION);
  else
  {
    LastFile=name;
    AddToMru(LastFile);

    if (Playing) return;
    Playing=TRUE;
    while (Playing && RunGame()) App::PeekLoop();
    Playing=FALSE;
  }
}

void MainWindow::CmOpen()
{
  FName fn=LastFile;
  if (os_get_game_file(fn,fn.Size()))
    OpenFile(fn);
}

void MainWindow::CmExit()
{
  DestroyWindow();
}

void MainWindow::CmHelpContents()
{
  FName HelpFile;
  GetModuleFileName(App::hInstance,HelpFile,HelpFile.Size());
  HelpFile.Update();
  HelpFile.NewName(HelpFileName);
  HtmlHelp(hWnd, HelpFile, HH_DISPLAY_TOPIC, 0L);
}

void MainWindow::Paint(HDC, BOOL, RECT&)
{
  Redraw();
}

void MainWindow::CmAbout()
{
  AboutDialog(this).ExecuteWithFont();
}

void MainWindow::CmPaste()
{
  if (OpenClipboard(hWndMain))
  {
    HGLOBAL handle = ::GetClipboardData(CF_TEXT);
    if (handle)
    {
      LPTSTR text = (LPTSTR)::GlobalLock(handle); 
      if (text) 
      {
        while (*text != 0)
        {
          if (isprint(*text))
            InputChars.AddTail(*text);
          text++;
        }
        ::GlobalUnlock(handle); 
      }
    }
    CloseClipboard();
  }
}

BOOL MainWindow::WMKeyDown(TMSG &Msg)
{
  bool ctrl = (::GetKeyState(VK_CONTROL) & 0x8000) != 0;
  bool shift = (::GetKeyState(VK_SHIFT) & 0x8000) != 0;

  switch ((int) Msg.wParam)   // the virtual key code
  {
    case VK_F1:
      CmHelpContents();
      break;
    case VK_F2: CmOpen(); break;
    case VK_F3: CmRestore(); break;
    case VK_F4: CmSave(); break;
    case VK_F5: CmDictionary(); break;

    case 'V':
      if (ctrl)
        CmPaste();
      break;
    case VK_INSERT:
      if (shift)
        CmPaste();
      break;

    case VK_LEFT:
    case VK_RIGHT:
    case VK_UP:
    case VK_DOWN:
    case VK_DELETE:
    case VK_END:
    case VK_HOME:
      InputChars.AddTail(256+Msg.wParam);
      break;
    }
  return TRUE; // message handled
}

BOOL MainWindow::WMChar(TMSG &Msg)
{
  if (isprint(Msg.wParam) || (Msg.wParam==8) || (Msg.wParam==13))
    InputChars.AddTail(Msg.wParam);
  return TRUE;
}

BOOL MainWindow::LButtonDown(TMSG &)
{
  return TRUE;
}

BOOL MainWindow::WMMouseMove(TMSG &)
{
  return TRUE;
}

BOOL MainWindow::LButtonUp(TMSG &)
{
  return TRUE;
}

BOOL MainWindow::RButtonDown(TMSG &)
{
  return TRUE;
}

BOOL MainWindow::WMSize(TMSG &Msg)
{
  if (Msg.wParam != SIZE_MINIMIZED)
  {
    PageWidth = LOWORD(Msg.lParam);
    WndHeight = HIWORD(Msg.lParam);
    Resize();
  }
  return TRUE;
}

BOOL MainWindow::WMSetFocus(TMSG&)
{
  MakeCaret();
  return TRUE;
}

BOOL MainWindow::WMKillFocus(TMSG&)
{
  KillCaret();
  return TRUE;
}

BOOL MainWindow::WMTimer(TMSG& Msg)
{
  if (Msg.wParam == 1)
  {
    KillTimer(hWndMain,1);

    int pic = DelayBitmap;
    DelayBitmap = 0;
    if (pic > 0)
      os_show_bitmap(pic,0,0);
  }
  return TRUE;
}

// App *****************************************

class MyApp : public App
{
public:
  MyApp(char *Name);
  ~MyApp();
  
private:
  void InitMainWindow();
  void SetDefs();
  void ReadIni();
  void WriteIni();
  void FirstIn();
};

void MyApp::SetDefs()
{
  // Set application default settings here
  LastFile="";
  FiltIndex=0;
  LastGameFile="";
  GameFiltIndex=0;
  LastScriptFile="";

  NONCLIENTMETRICS ncm;
  memset(&ncm,0,sizeof(ncm));
  ncm.cbSize = sizeof(ncm);
  SystemParametersInfo(SPI_GETNONCLIENTMETRICS,ncm.cbSize,&ncm,0);

  memset(&lf,0,sizeof(lf));
  HDC dc = GetDC(hWndMain);
  lf.lfHeight=-MulDiv(10,GetDeviceCaps(dc,LOGPIXELSY),72);
  ReleaseDC(hWndMain,dc);
  lf.lfWeight=FW_NORMAL;
  lf.lfCharSet=ANSI_CHARSET;
  lf.lfOutPrecision=OUT_TT_PRECIS;
  lf.lfClipPrecision=CLIP_DEFAULT_PRECIS;
  lf.lfQuality=PROOF_QUALITY;
  lf.lfPitchAndFamily=4;
  strcpy(lf.lfFaceName,ncm.lfMessageFont.lfFaceName);

  FontColour=RGB(0,0,0);
}

void MyApp::ReadIni()
{
  // read information from ini file
  SetDefs();
  ReadIniString("General","LastFile",(String&)LastFile);
  ReadIniInt("General","FiltIndex",FiltIndex);
  ReadIniString("General","LastGameFile",(String&)LastGameFile);
  ReadIniInt("General","GameFiltIndex",GameFiltIndex);

  ReadIniInt("Font","Size",lf.lfHeight);
  String S(LF_FACESIZE);
  ReadIniString("Font","Name",S);
  if (*S) strcpy(lf.lfFaceName,S);
  ReadIniInt("Font","Colour",(long&) FontColour);
}

void MyApp::WriteIni()
{
  // write information to ini file
  WriteIniString("General","LastFile",LastFile);
  WriteIniInt("General","FiltIndex",FiltIndex);
  WriteIniString("General","LastGameFile",LastGameFile);
  WriteIniInt("General","GameFiltIndex",GameFiltIndex);

  long FontHeight=lf.lfHeight;
  WriteIniInt("Font","Size",FontHeight);
  WriteIniString("Font","Name",lf.lfFaceName);
  WriteIniInt("Font","Colour",(long) FontColour);
}

MyApp::MyApp(char *Name) : App(Name,Ini)
{
  // enable 3d dialog boxes
#ifdef WIN16
  EnableCtl3d();
#else
  OSVERSIONINFO ovi;
  ovi.dwOSVersionInfoSize=sizeof(ovi);
  GetVersionEx(&ovi);
  if (ovi.dwMajorVersion == 3) EnableCtl3d();
#endif

  // read from ini file
  ReadMru(4);
  ReadIni();
};

void MyApp::InitMainWindow()
{
  MainWindow=new ::MainWindow(0,MainWinTitle);
}

MyApp::~MyApp()
{
  // write information to ini file
  WriteMru();
  WriteIni();
}

void MyApp::FirstIn()
{
  if (__argc > 1)
  {
    // Convert to a full path name
    char fullname[_MAX_PATH];
    char* part;
    if (::GetFullPathName(__argv[1],_MAX_PATH,fullname,&part) != 0)
      ((::MainWindow*) MainWindow)->OpenFile(fullname);
  }
  else
    ((::MainWindow*) MainWindow)->CmOpen();
}

// main function, called from WinMain()
int Main()
{
  // create and run application
  return MyApp(AppName).Run();
}

