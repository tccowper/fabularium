/****************************************************************************\
*
* Magnetic - Magnetic Scrolls Interpreter.
*
* Written by Niclas Karlsson <nkarlsso@abo.fi>,
*            David Kinder <davidk.kinder@virgin.net>,
*            Stefan Meier <Stefan.Meier@if-legends.org> and
*            Paul David Doherty <pdd@if-legends.org>
*
* Copyright (C) 1997-2008  Niclas Karlsson
*
* Amiga interface by David Kinder
* Changes for AROS by Matthias Rustler
*
*     This program is free software; you can redistribute it and/or modify
*     it under the terms of the GNU General Public License as published by
*     the Free Software Foundation; either version 2 of the License, or
*     (at your option) any later version.
*
*     This program is distributed in the hope that it will be useful,
*     but WITHOUT ANY WARRANTY; without even the implied warranty of
*     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*     GNU General Public License for more details.
*
*     You should have received a copy of the GNU General Public License
*     along with this program; if not, write to the Free Software
*     Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111, USA.
*
\****************************************************************************/

#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <proto/amigaguide.h>
#include <proto/asl.h>
#include <proto/diskfont.h>
#include <proto/dos.h>
#include <proto/exec.h>
#include <proto/gadtools.h>
#include <proto/graphics.h>
#include <proto/icon.h>
#include <proto/intuition.h>
#include <exec/memory.h>
#include <graphics/gfxmacros.h>
#include <graphics/videocontrol.h>
#include <hardware/custom.h>
#include <intuition/imageclass.h>
#include <intuition/intuitionbase.h>
#include <workbench/startup.h>
#include "defs.h"

#ifdef __AROS__
#include <aros/oldprograms.h>
#else
#include <graphics/display.h>
#endif

struct Screen *amiga_openscreen(struct TextAttr *font,int gfx_depth);
void amiga_init(void);
void amiga_scr(struct Screen *screen);
void amiga_resetcrsr(void);
void amiga_str(char *s);
void amiga_ch(char c);
void amiga_flush(int test);
void amiga_rect(long xMin,long yMin,long xMax,long yMax,unsigned long pen);
void amiga_text(char *s,int n);
int amiga_chrlen(int c);
void amiga_cursor(int under);
void amiga_setwin(int limit,int x);
int amiga_getch(UWORD *qualifier_addr,int c);
void amiga_help(void);
void amiga_about(void);
LONG amiga_req(UBYTE *text,UBYTE *gadgets,...);
void amiga_busy(int busy);
void amiga_input(char *ibuff,int size);
void amiga_redrawline(unsigned char *buffer,int *pos,int max_size);
void amiga_mvtext(int offset,int max);
void amiga_crleft(unsigned char *buffer,int *pos);
void amiga_crright(unsigned char *buffer,int *pos);
int amiga_fittext(unsigned char *pointer,int new);
void amiga_inbuf(unsigned char *buffer,unsigned char *new,int *pos,
  int max_size);
void amiga_storeh(unsigned char *line);
int amiga_cmp(const char *a,const char *b);
type8 amiga_getinputch(void);
void amiga_statusch(char c);
int amiga_checklen(char *s,int n);
void amiga_freq(char *initdir,char *buffer,char *title,char *pattern);
char *amiga_getextname(char *gamename,char *newname,const char *ext);
int amiga_sizewidth(void);
void amiga_showpic(type8 *buffer);
void amiga_freepic(void);
void amiga_drawpic(void);
void amiga_freepens(void);
void amiga_setcopper(void);
void amiga_clearcopper(int think);
void amiga_syscolours(int black);
void amiga_closepicwin(void);
void amiga_preparepicwin(void);
void amiga_openpicwin(void);
BPTR amiga_open_prefs(void);
void amiga_read_prefs(void);
void amiga_write_prefs(void);

extern type32 i_count;
extern type8 *picture_buffer;
type8 log_on = 0;
type8 *gamename = 0;
FILE *logfile1 = 0, *logfile2 = 0;
type8 ms_gfx_enabled;
type16 pic_width, pic_height;
type16 pic_palette[16];
type8 filename[128], gfx_name[256], hint_name[256];
type8 current_pic = -1;

#define SCRIPT_WIDTH 78
int transcript_idx = 0;
type8 transcript[SCRIPT_WIDTH];

type8 ms_load_file(type8s *name, type8 *ptr, type16 size)
{
FILE *fh;
type8 *realname;

  if (name)
  {
    realname = name;
  }
  else
  {
    strcpy(filename,"");
    do
    {
      amiga_str("Filename: ");
      amiga_input(filename,128);
      amiga_ch(0x0a);
    }
    while (strcmp(filename,"") == 0);
    realname = filename;
  }
  if (!(fh = fopen(realname,"rb"))) return 1;
  if (fread(ptr,1,size,fh) != size) return 1;
  fclose(fh);
  return 0;
}

type8 ms_save_file(type8s *name, type8 *ptr, type16 size)
{
FILE *fh;
type8 *realname;

  if (name)
  {
    realname = name;
  }
  else
  {
    strcpy(filename,"");
    do
    {
      amiga_str("Filename: ");
      amiga_input(filename,128);
      amiga_ch(0x0a);
    }
    while (strcmp(filename,"") == 0);
    realname = filename;
  }
  if (!(fh = fopen(realname,"wb"))) return 1;
  if (fwrite(ptr,1,size,fh) != size) return 1;
  fclose(fh);
  return 0;
}

void script_write(type8 c)
{
  if (log_on == 2 && fputc(c,logfile1) == EOF)
  {
    fclose(logfile1);
    log_on = 0;
  }
}

void transcript_write(type8 c)
{
  if (logfile2 == 0)
    return;

  switch (c)
  {
    case 0x08:
      if (transcript_idx > 0)
      {
	transcript_idx--;
      }
      break;
    case '\n':
      if (fwrite(transcript,1,transcript_idx,logfile2) < transcript_idx)
      {
	fclose(logfile2);
	logfile2 = 0;
	return;
      }
      fputc(c,logfile2);
      transcript_idx = 0;
      break;
    default:
      if (transcript_idx >= SCRIPT_WIDTH)
      {
	int spc = transcript_idx-1;
	while (spc > 0)
        {
	  if (transcript[spc] == ' ')
	  {
	    transcript[spc] = '\n';
	    break;
	  }
	  spc--;
        }
	if (fwrite(transcript,1,spc+1,logfile2) < spc+1)
	{
	  fclose(logfile2);
	  logfile2 = 0;
	  return;
	}
	memmove(transcript,transcript+spc+1,transcript_idx-spc-1);
	transcript_idx = transcript_idx-spc-1;
      }
      transcript[transcript_idx++] = c;
      break;
  }
}

void ms_statuschar(type8 c)
{
  amiga_statusch(c);
}

void ms_flush(void)
{
  amiga_flush(0);
}

void ms_putchar(type8 c)
{
  amiga_ch(c);
  transcript_write(c);
}

type8 ms_getchar(type8 trans)
{
static type8 buf[256];
static type16 pos = 0;
type8 c,i;

  ms_flush();

  if (!pos)
  {
    i = 0;
    while (1)
    {
      if (log_on == 1)
      {
	if ((c = fgetc(logfile1)) == EOF)
	{
	  log_on = 0;
	  fclose(logfile1);
	  c = amiga_getinputch();
	}
	else if (c == '\r')
	{
	  continue;
	}
	else if (c == '#')
	{
	char line[256];
	int got = 0;

	  if (fgets(line,256,logfile1))
	  {
	    if ((stricmp(line,"seed\n") == 0) || (stricmp(line,"seed\r\n") == 0))
	    {
	      if (fgets(line,256,logfile1))
	      {
		ms_seed(atoi(line));
		c = '\n';
		got = 1;
	      }
	    }
	  }
	  if (got == 0)
	  {
	    continue;
	  }
	}
	else if (c != '\n')
	{
	  amiga_ch(c);
	}
      }
      else
      {
	c = amiga_getinputch();
	if (c == '#' && !i && trans)
	{
	  while((c = amiga_getinputch()) != '\n' && c != EOF && i < 255)
	    buf[i++] = c;
	  buf[i] = 0;
	  c = '\n';
	  i = 0;
	  if (!strcmp(buf,"logoff") && log_on == 2)
	  {
	    amiga_str("\n[Closing script file]\n>");
	    log_on = 0;
	    fclose(logfile1);
	  }
	  else if (!strcmp(buf,"undo"))
	  {
	    c = 0;
	  }
	  else
	  {
	    amiga_str("\n[Nothing done]\n>");
	  }
	}
      }
      script_write(c);
      if (c != '\n') transcript_write(c);
      if (c == '\n' || c == EOF || i == 255) break;
      buf[i++] = c;
      if (!c) break;
    }
    buf[i] = '\n';
  }
  if ((c = buf[pos++]) == '\n' || !c) pos = 0;
  return c;
}

void ms_showpic(type32 c,type8 mode)
{
type8 *picdata;

  if (ms_gfx_enabled != 1) return;
  switch (mode)
  {
    case 0:	/* Graphics off */
      amiga_closepicwin();
      break;
    case 1:     /* Graphics on (thumbnails) */
    case 2:	/* Graphics on (normal) */
      amiga_preparepicwin();
      if (c == current_pic) return;
      current_pic = -1;
      amiga_freepic();
      picdata = ms_extract(c,&pic_width,&pic_height,pic_palette,0);
      if (picdata)
      {
	current_pic = c;
	amiga_showpic(picdata);
      }
      break;
  }
}

void ms_fatal(type8s *txt)
{
  fprintf(stderr,"Fatal error: %s\n",txt);
  exit(1);
}

type8 ms_showhints(struct ms_hint *hints)
{
  return 0;
}

void ms_playmusic(type8 * midi_data, type32 length, type16 tempo)
{
}

/* Password functions */

type8 result[128];

type8 obfuscate(type8 c)
{
static type8 state;
type8 i;

  if (!c) state = 0;
  else
  {
    state ^= c;
    for (i = 0;i < 13; i++)
    {
      if ((state & 1) ^ ((state >> 1) & 1)) state |= 0x80;
      else state &= 0x7f;
      state >>= 1;
    }
  }
  return state;
}

void passwd(char *str)
{
type8 tmp,name[128],pad[] = "MAGNETICSCR";
type32 i,j;

  for (i = j = 0; i < strlen(str); i++)
  {
    if (str[i] != 0x20) name[j++] = toupper(str[i]);
  }
  name[j] = 0;

  tmp = name[strlen(name)-1];
  if ((tmp=='#') || (tmp==']')) name[strlen(name)-1] = 0;
  if (strlen(name) < 12)
  {
    for (i = strlen(name), j = 0; i < 12; i++, j++) name[i] = pad[j];
    name[i] = 0;
  }

  obfuscate(0);
  i = j = 0;
  while (name[i])
  {
    tmp = obfuscate(name[i++]);
    if (name[i]) tmp += obfuscate(name[i++]);
    tmp &= 0x1f;
    if (tmp < 26) tmp += 'A';
    else tmp += 0x16;
    result[j++] = tmp;
  }
  result[j] = 0;
}

/* Amiga specific routines */

void CloseWindowSafely(struct Window **win)
{
struct IntuiMessage *msg, *succ;

  Forbid();

  msg = (struct IntuiMessage *)(*win)->UserPort->mp_MsgList.lh_Head;
  while (succ = (struct IntuiMessage *)msg->ExecMessage.mn_Node.ln_Succ)
  {
    if (msg->IDCMPWindow == *win)
    {
      Remove((struct Node *)msg);
      ReplyMsg((struct Message *)msg);
    }
    msg = succ;
  }

  (*win)->UserPort = NULL;
  ModifyIDCMP(*win,0);
  Permit();
  ClearMenuStrip(*win);
  CloseWindow(*win);
  *win = NULL;
}

struct MagneticPreferences
{
  WORD Version;
  WORD WindowLeft, WindowTop;
  WORD WindowWidth, WindowHeight;
  WORD GfxWinLeft, GfxWinTop;
};
struct MagneticPreferences MagneticPrefs;

struct NewMenu NewMenus[] =
{
  { NM_TITLE,"Project",0,0,0,0 },
  { NM_ITEM,"Help...","H",0,0,0 },
  { NM_ITEM,"About...","?",0,0,0 },
  { NM_ITEM,NM_BARLABEL,0,0,0,0 },
  { NM_ITEM,"Save Prefs",0,0,0,0 },
  { NM_ITEM,NM_BARLABEL,0,0,0,0 },
  { NM_ITEM,"Quit","Q",0,0,0 },
  { NM_END,0,0,0,0,0 }};

char Version[] = "$VER:Magnetic 2.3 (24.10.2010)";
char TitleBar[] = "Magnetic Scrolls";

#define HISTORY_LINES 20
unsigned char *History[HISTORY_LINES];
int HistoryPosition = HISTORY_LINES;

#define TEXTBUFFER_SIZE 256
char TextBuffer[TEXTBUFFER_SIZE+1];
int TextBufferPtr;

#define INPUTBUFFER_SIZE 256
char InputBuffer[INPUTBUFFER_SIZE+1];
int InputBufferPtr = -1;

#define STATUSBUFFER_SIZE 256
char StatusLoc[STATUSBUFFER_SIZE],StatusScore[STATUSBUFFER_SIZE];

LONG PicturePens[16] =
  { -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1 };
USHORT ScreenColours[16];
USHORT CustomColours[16];
USHORT BlackColours[4] = { 0x0000,0x0FFF,0x0000,0x0FFF };
USHORT BlackSysColours[4];

extern struct IntuitionBase *IntuitionBase;
struct Library *AmigaGuideBase;
struct Screen *Screen, *DefaultPubScreen;
struct Window *Window, *PictureWindow, *OldWindowPtr;
struct RastPort *RastPort;
struct Menu *Menus;
struct DiskObject *Icon;
struct TextFont *Font;
struct Process *ThisProcess;
struct FileRequester *FileReq;
struct MsgPort *MsgPort;
struct BitMap PicBitMap;
APTR Visual;
int ScreenWidth, ScreenHeight;
int DisplayHeight, PreviousHeight;
int MoreCount = -1;
int TextLeft, PreviousChar;
int BitMapWidth;
int BitMapMem = 0;
int CopperActive = 0;
int LimitPicWin = 0;

#define PIC_DEPTH 8
#define FIXED_GFX_HEIGHT 140

#define GFX_NONE    0
#define GFX_WINDOW  1
#define GFX_COPPER  2

int Graphics = GFX_NONE;
int GfxDoubleWidth = 0;

#define DISP_SCREEN 0
#define DISP_WINDOW 1
#define DISP_BLACK  2

int Display = DISP_SCREEN;

int ColText = 1;
int ColInput = 2;
int ColStatText = 2;
int ColStatBar = 3;

#define QUALIFIER_SHIFT (IEQUALIFIER_LSHIFT|IEQUALIFIER_RSHIFT)
#define MIN(a,b) (((a) < (b)) ? (a) : (b))
#define MAX(a,b) (((a) > (b)) ? (a) : (b))
#define CHAR_DELETED 0x100

#define KEYCODE_BACKSPACE	128
#define KEYCODE_DELETE		129
#define KEYCODE_CURSOR_UP	130
#define KEYCODE_CURSOR_DOWN	131
#define KEYCODE_CURSOR_LEFT	132
#define KEYCODE_CURSOR_RIGHT	133
#define KEYCODE_PASSWORD	134
#define KEYCODE_RESIZE_WINDOW	135
#define KEYCODE_TEXT_FRONT	136
#define KEYCODE_GFX_FRONT	137

struct Screen *amiga_openscreen(struct TextAttr *font,int gfx_depth)
{
static WORD pens[] = { -1 };
struct Screen *screen;

  screen = OpenScreenTags(0,
    SA_Pens,pens,
    SA_DisplayID,GetVPModeID(&DefaultPubScreen->ViewPort),
    SA_Overscan,OSCAN_TEXT,
    SA_Depth,Graphics == GFX_NONE ? 2 : gfx_depth,
    SA_Type,CUSTOMSCREEN|AUTOSCROLL,
    SA_Font,font,
    SA_LikeWorkbench,1,
    SA_FullPalette,1,
    SA_SharePens,1,
    SA_Quiet,Display == DISP_BLACK ? 1 : 0,
    SA_ShowTitle,Display == DISP_BLACK ? 0 : 1,
    SA_Title,TitleBar,TAG_DONE);

  if (screen)
    if (Display == DISP_BLACK) LoadRGB4(&screen->ViewPort,BlackColours,4);

  return screen;
}

void amiga_init(void)
{
char prog_name[256];
char prog_path[256];
static char font_name[MAXFONTPATH];
char *name_ptr;
static struct TextAttr font = {NULL, 0, FS_NORMAL, 0};
int i, window_left, window_top, window_width, window_height;

  if (IntuitionBase->LibNode.lib_Version < 37) exit(0);

  if ((DefaultPubScreen = LockPubScreen(0)) == 0) exit (1);
  amiga_scr(DefaultPubScreen);

  if (Icon == NULL)
  {
    if (GetProgramName(prog_name,256)) Icon = GetDiskObject(prog_name);
  }
  if (Icon == NULL)
  {
    strcpy(prog_path,"PROGDIR:");
    strcat(prog_path,prog_name);
    Icon = GetDiskObject(prog_path);
  }
  if (Icon)
  {
  char *tooltype;

    if (tooltype = FindToolType(Icon->do_ToolTypes,"DISPLAY"))
    {
      if (stricmp(tooltype,"WINDOW") == 0) Display = DISP_WINDOW;
      if (stricmp(tooltype,"SCREEN") == 0) Display = DISP_SCREEN;
      if (stricmp(tooltype,"BLACK") == 0) Display = DISP_BLACK;
    }

    if (tooltype = FindToolType(Icon->do_ToolTypes,"GRAPHICS"))
    {
      if (stricmp(tooltype,"WINDOW") == 0) Graphics = GFX_WINDOW;
      if (stricmp(tooltype,"COPPER") == 0) Graphics = GFX_COPPER;
    }

    if (tooltype = FindToolType(Icon->do_ToolTypes,"FONT"))
    {
      strcpy(font_name,tooltype);
      if (name_ptr = strrchr(font_name,'/'))
      {
	font.ta_Name = font_name;
	font.ta_YSize = atoi(name_ptr+1);
	strcpy(name_ptr,".font");
      }
    }
    if (font.ta_Name) Font = OpenDiskFont(&font);

    if (tooltype = FindToolType(Icon->do_ToolTypes,"DOUBLEGFXWIDTH"))
      GfxDoubleWidth = 1;
  }
  if (Font == NULL)
  {
    font.ta_Name = "topaz.font";
    font.ta_YSize = 8;
    Font = OpenFont(&font);
  }

  if (ms_gfx_enabled != 2) Graphics = GFX_NONE;
  switch (Graphics)
  {
    case GFX_WINDOW:
      if (Display == DISP_BLACK) Graphics = GFX_NONE;
      if (IntuitionBase->LibNode.lib_Version < 39) Graphics = GFX_NONE;
      break;
    case GFX_COPPER:
      if (Display == DISP_WINDOW) Graphics = GFX_NONE;
      break;
  }

  if (Display == DISP_BLACK)
  {
    ColText = 1;
    ColInput = 1;
    ColStatBar = 1;
    ColStatText = 0;
    for (i = 0; i < 4; i++)
      BlackSysColours[i] = GetRGB4(DefaultPubScreen->ViewPort.ColorMap,i);
  }

  if ((Display == DISP_SCREEN) || (Display == DISP_BLACK))
  {
    switch (Graphics)
    {
      case GFX_WINDOW:
	Screen = amiga_openscreen(&font,5);
	break;
      case GFX_COPPER:
	Screen = amiga_openscreen(&font,4);
	break;
      default:
	Screen = amiga_openscreen(&font,2);
	break;
    }
    if (Screen == NULL)
    {
      Graphics = GFX_NONE;
      if ((Screen = amiga_openscreen(&font,2)) == NULL) exit(1);
    }
  }

  if ((MsgPort = CreateMsgPort()) == NULL) exit(1);
  amiga_read_prefs();

  if (Screen)
    window_top = Display == DISP_BLACK ? 0 : Screen->BarHeight+1;
  else
    window_top = DefaultPubScreen->BarHeight+1;
  if (Graphics == GFX_COPPER) window_top += FIXED_GFX_HEIGHT;

  if ((Screen == NULL) && (MagneticPrefs.WindowLeft != -1))
  {
    window_left = MagneticPrefs.WindowLeft;
    window_top = MagneticPrefs.WindowTop;
    window_width = MagneticPrefs.WindowWidth;
    window_height = MagneticPrefs.WindowHeight;
  }
  else
  {
    window_left = 0;
    window_width = Screen ? Screen->Width : ScreenWidth;
    window_height = Screen ?
      Screen->Height-window_top : ScreenHeight-window_top;
  }

  if ((Window = OpenWindowTags(0,
    WA_Left,window_left,
    WA_Top,window_top,
    WA_Width,window_width,
    WA_Height,window_height,
    WA_SmartRefresh,1,
    WA_NewLookMenus,1,
    WA_AutoAdjust,1,
    WA_Borderless,Screen ? 1 : 0,
    WA_Backdrop,Screen ? 1 : 0,
    WA_Activate,1,
    WA_CloseGadget,Screen ? 0 : 1,
    WA_DragBar,Screen ? 0 : 1,
    WA_DepthGadget,Screen ? 0 : 1,
    WA_SizeGadget,Screen ? 0 : 1,
    WA_SizeBBottom,Screen ? 0 : 1,
    WA_IDCMP,0,
    Screen ? TAG_IGNORE : WA_Title,TitleBar,
    WA_ScreenTitle,TitleBar,
    Screen ? WA_CustomScreen : WA_PubScreen,
      Screen ? Screen : DefaultPubScreen,
    TAG_DONE)) == 0) exit(1);

  Window->UserPort = MsgPort;
  ModifyIDCMP(Window,IDCMP_RAWKEY|IDCMP_VANILLAKEY|IDCMP_MENUPICK|
    IDCMP_CLOSEWINDOW|IDCMP_CHANGEWINDOW);

  ThisProcess = (struct Process *)FindTask(0);
  OldWindowPtr = ThisProcess->pr_WindowPtr;
  ThisProcess->pr_WindowPtr = Window;

  if ((Visual = GetVisualInfo(Window->WScreen,TAG_DONE)) == 0) exit (1);
  if ((Menus = CreateMenus(NewMenus,GTMN_NewLookMenus,TRUE,TAG_DONE)) == 0)
    exit(1);
  LayoutMenus(Menus,Visual,GTMN_NewLookMenus,TRUE,TAG_DONE);
  SetMenuStrip(Window,Menus);

  RastPort = Window->RPort;
  SetDrMd(RastPort,JAM2);
  SetAPen(RastPort,ColText);
  SetBPen(RastPort,0);
  SetFont(RastPort,Font);
  RastPort->Mask = 0x03;
  DisplayHeight = ((Window->Height-Window->BorderTop-Window->BorderBottom)/
    RastPort->TxHeight)-1;
  PreviousHeight = DisplayHeight;

  switch (Graphics)
  {
    case GFX_WINDOW:
      for (i = 0; i < 16; i++)
      {
	PicturePens[i] = ObtainPen(Window->WScreen->ViewPort.ColorMap,-1,
	  0,0,0,PEN_EXCLUSIVE);
      }
      for (i = 0; i < 16; i++)
      {
	if (PicturePens[i] == -1)
	{
	  amiga_freepens();
	  Graphics = GFX_NONE;
	}
      }
      break;
    case GFX_COPPER:
      for (i = 0; i < 16; i++)
      {
	PicturePens[i] = i;
	ScreenColours[i] = GetRGB4(Screen->ViewPort.ColorMap,i);
      }
      amiga_openpicwin();
      break;
  }

  amiga_resetcrsr();
}

void amiga_exit(void)
{
int i;

  if (TextLeft)
  {
    amiga_ch(0x0a);
    amiga_text("[Hit any key to exit.]",22);
    amiga_cursor(0);
    amiga_getch(0,-1);
    amiga_cursor(0);
  }

  for (i = 0; i < HISTORY_LINES; i++)
  {
    if (*(History+i)) FreeVec(*(History+i));
    *(History+i) = 0;
  }
  if (ThisProcess) ThisProcess->pr_WindowPtr = OldWindowPtr;
  amiga_freepic();
  amiga_freepens();
  amiga_clearcopper(0);
  if (PictureWindow) CloseWindowSafely(&PictureWindow);
  if (Window) CloseWindowSafely(&Window);
  if (Menus) FreeMenus(Menus);
  if (Visual) FreeVisualInfo(Visual);
  if (MsgPort) DeleteMsgPort(MsgPort);
  if (Screen) CloseScreen(Screen);
  if (Font) CloseFont(Font);
  if (DefaultPubScreen) UnlockPubScreen(0,DefaultPubScreen);
  if (Icon) FreeDiskObject(Icon);
  if (FileReq) FreeAslRequest(FileReq);
  if (AmigaGuideBase) CloseLibrary(AmigaGuideBase);
}

void amiga_scr(struct Screen *screen)
{
struct TagItem vti[] = {
  { VTAG_VIEWPORTEXTRA_GET,0 },
  { VTAG_END_CM,0 }
};
struct ViewPortExtra *vpe;

  ScreenWidth = screen->Width;
  ScreenHeight = screen->Height;
  if (screen->ViewPort.ColorMap)
  {
    if (VideoControl(screen->ViewPort.ColorMap,vti) == 0)
    {
      vpe = (struct ViewPortExtra *)vti[0].ti_Data;
      ScreenWidth = vpe->DisplayClip.MaxX-vpe->DisplayClip.MinX+1;
      ScreenHeight = vpe->DisplayClip.MaxY-vpe->DisplayClip.MinY+1;
    }
  }
}

void amiga_resetcrsr(void)
{
  Move(RastPort,Window->BorderLeft,
    Window->BorderTop+(DisplayHeight*RastPort->TxHeight));
}

void amiga_str(char *s)
{
  while (*s != 0) amiga_ch(*s++);
}

void amiga_ch(char c)
{
  switch (c)
  {
  case 0x0a:
    PreviousChar = 0;
    amiga_flush(0);
    MoreCount++;
    if (RastPort->cp_y == Window->BorderTop+
      (DisplayHeight*RastPort->TxHeight))
    {
      ClipBlit(RastPort,Window->BorderLeft,
	Window->BorderTop+(RastPort->TxHeight*2),
	RastPort,Window->BorderLeft,
	Window->BorderTop+RastPort->TxHeight,
	Window->Width-Window->BorderLeft-Window->BorderRight,
	(DisplayHeight-1)*RastPort->TxHeight,0xC0);
      amiga_rect(Window->BorderLeft,
	Window->BorderTop+DisplayHeight*RastPort->TxHeight,
	Window->Width-Window->BorderRight-1,
	Window->BorderTop+(DisplayHeight+1)*RastPort->TxHeight-1,0);
      amiga_resetcrsr();

      if (MoreCount >= DisplayHeight-1)
      {
	MoreCount = 0;
	if (log_on != 1)
	{
	  amiga_text("[More]",6);
	  amiga_cursor(0);
	  amiga_getch(0,-1);
	  amiga_cursor(0);
	  amiga_rect(Window->BorderLeft,
	    Window->BorderTop+DisplayHeight*RastPort->TxHeight,
	    Window->Width-Window->BorderRight-1,
            Window->BorderTop+(DisplayHeight+1)*RastPort->TxHeight-1,0);
	  amiga_resetcrsr();
	}
      }
    }
    else
      Move(RastPort,Window->BorderLeft,RastPort->cp_y+RastPort->TxHeight);
    break;
  case 0x08:
    if (TextBufferPtr == 0) return;
    if (PreviousChar & CHAR_DELETED)
      PreviousChar = 0;
    else
      PreviousChar |= CHAR_DELETED;
    TextBufferPtr--;
    break;
  default:
    if (isprint(c) == 0) return;
    PreviousChar = (int)c;
    if (TextBufferPtr >= TEXTBUFFER_SIZE) amiga_flush(0);
    if (c == ' ')
    {
    int test;

      test = amiga_checklen(TextBuffer,TextBufferPtr);
      if (test) amiga_flush(1);
    }
    *(TextBuffer+(TextBufferPtr++)) = c;
    TextLeft = 1;
    break;
  }
}

int amiga_checklen(char *s,int n)
{
  return TextLength(RastPort,s,n) >
    Window->Width-Window->BorderRight-RastPort->cp_x;
}

void amiga_flush(int test)
{
static int semaphore = 0;
int check;

  if (TextBufferPtr < 1) return;
  if (semaphore) return;
  semaphore = 1;

  check = test ? 1 : amiga_checklen(TextBuffer,TextBufferPtr);
  if (check)
  {
  int i;

    i = TextBufferPtr-1;
    while ((i > 0) && *(TextBuffer+i) != ' ') i--;
    amiga_text(TextBuffer,i);
    amiga_ch(0x0a);
    if (*(TextBuffer+i) == ' ') i++;
    amiga_text(TextBuffer+i,TextBufferPtr-i);
  }
  else amiga_text(TextBuffer,TextBufferPtr);

  TextBufferPtr = 0;
  semaphore = 0;
}

void amiga_rect(long xMin,long yMin,long xMax,long yMax,unsigned long pen)
{
unsigned long saved_pen;

  saved_pen = RastPort->FgPen;
  SetAPen(RastPort,pen);
  RectFill(RastPort,xMin,yMin,xMax,yMax);
  SetAPen(RastPort,saved_pen);
}

void amiga_text(char *s,int n)
{
  if (n < 1) return;
  Move(RastPort,RastPort->cp_x,RastPort->cp_y+RastPort->TxBaseline);
  Text(RastPort,s,n);
  Move(RastPort,RastPort->cp_x,RastPort->cp_y-RastPort->TxBaseline);
}

int amiga_chrlen(int c)
{
unsigned char buffer;

  if (c < 0) c += 256;
  buffer = (unsigned char)c;
  return TextLength(RastPort,&buffer,1);
}

void amiga_cursor(int under)
{
int size;

  amiga_flush(0);
  size = (under == 0) ? RastPort->TxWidth : amiga_chrlen(under);

  SetDrMd(RastPort,COMPLEMENT);
  amiga_rect(RastPort->cp_x,RastPort->cp_y,RastPort->cp_x+size-1,
    RastPort->cp_y+RastPort->TxHeight-1,0);
  SetDrMd(RastPort,JAM2);
}

void amiga_setwin(int limit,int x)
{
  if (limit)
    WindowLimits(Window,
      RastPort->cp_x+RastPort->TxWidth+Window->BorderRight+x,
      Window->BorderTop+Window->BorderBottom+(RastPort->TxHeight*3),~0,~0);
  else
    WindowLimits(Window,Window->Width,Window->Height,Window->Width,
      Window->Height);
}

int amiga_getch(UWORD *qualifier_addr,int c)
{
struct IntuiMessage *imsg;
struct Window *this_win;
ULONG class;
UWORD code, qualifier;
int old_height, copper = 0;

  amiga_flush(0);
  TextLeft = 0;
  while (1)
  {
    while (imsg = (struct IntuiMessage *)GetMsg(MsgPort))
    {
      class = imsg->Class;
      code = imsg->Code;
      qualifier = imsg->Qualifier;
      this_win = imsg->IDCMPWindow;
      if (class == IDCMP_MENUVERIFY)
      {
	copper = CopperActive;
	if (copper) amiga_clearcopper(1);
	amiga_syscolours(0);
      }
      ReplyMsg ((struct Message *)imsg);
      if (qualifier_addr) *qualifier_addr = qualifier;
      switch (class)
      {
      case IDCMP_RAWKEY:
	switch (code)
	{
	case 0x4C:
	  return KEYCODE_CURSOR_UP;
	case 0x4D:
	  return KEYCODE_CURSOR_DOWN;
	case 0x4E:
	  return KEYCODE_CURSOR_RIGHT;
	case 0x4F:
	  return KEYCODE_CURSOR_LEFT;
	case 0x50:
	  return KEYCODE_TEXT_FRONT;
	case 0x51:
	  return KEYCODE_GFX_FRONT;
	case 0x59:
	  return KEYCODE_PASSWORD;
	case 0x5F:
	  amiga_help();
	  break;
	}
	break;
      case IDCMP_VANILLAKEY:
	switch (code)
	{
	case 8:
	  return KEYCODE_BACKSPACE;
	case 13:
	  return 13;
	case 127:
	  return KEYCODE_DELETE;
	default:
	  if (code >= 32 && code <= 126) return code;
	  break;
	}
	break;
      case IDCMP_CLOSEWINDOW:
	if (this_win == Window)	exit(0);
	if (this_win == PictureWindow) amiga_closepicwin();
	break;
      case IDCMP_CHANGEWINDOW:
	if (this_win == Window)
	{
	  old_height = DisplayHeight;
	  DisplayHeight = ((Window->Height-Window->BorderTop-
	    Window->BorderBottom)/RastPort->TxHeight)-1;

	  if (PreviousHeight > DisplayHeight)
	  {
	    amiga_rect(Window->BorderLeft,
	      Window->BorderTop+(DisplayHeight*RastPort->TxHeight),
	      Window->Width-Window->BorderRight-1,
	      Window->Height-Window->BorderBottom-1,0);

	    if (RastPort->cp_y+RastPort->TxHeight >
	      Window->Height-Window->BorderBottom)
	    {
	      Move(RastPort,RastPort->cp_x,
		Window->BorderTop+(DisplayHeight*RastPort->TxHeight));
	      amiga_rect(Window->BorderLeft,RastPort->cp_y,
		Window->Width-Window->BorderRight-1,
		RastPort->cp_y+RastPort->TxHeight-1,0);
	      PreviousHeight = DisplayHeight;
	      return KEYCODE_RESIZE_WINDOW;
	    }
	  }
	  PreviousHeight = DisplayHeight;
	}
	if (PictureWindow && (this_win == PictureWindow))
	{
	  if (LimitPicWin)
	  {
	    WindowLimits(PictureWindow,
	      128,64,
	      PictureWindow->Width,
	      PictureWindow->Height);
	    LimitPicWin = 0;
	  }
	  amiga_drawpic();
	}
	break;
      case IDCMP_MENUPICK:
	if (copper) amiga_setcopper();
	amiga_syscolours(1);
	if (code != MENUNULL)
	{
	  if (MENUNUM(code) == 0)
	  {
	    switch (ITEMNUM(code))
	    {
	    case 0:
	      amiga_help();
	      break;
	    case 1:
	      amiga_about();
	      break;
	    case 3:
	      amiga_write_prefs();
	      break;
	    case 5:
	      exit(0);
	      break;
	    }
	  }
	}
	break;
      }
    }
    ModifyIDCMP(Window,Window->IDCMPFlags | IDCMP_MENUVERIFY);
    WaitPort(MsgPort);
    ModifyIDCMP(Window,Window->IDCMPFlags & ~IDCMP_MENUVERIFY);
  }
}

void amiga_help(void)
{
int copper;
struct NewAmigaGuide guide =
  { 0,"Magnetic.guide",0,0,0,0,0,0,0,0,0,0,0 };

  copper = CopperActive;
  if (copper) amiga_clearcopper(1);
  amiga_syscolours(0);
  guide.nag_Screen = Window->WScreen;
  if (AmigaGuideBase == 0)
    AmigaGuideBase = OpenLibrary("amigaguide.library",34);
  if (AmigaGuideBase != 0)
    CloseAmigaGuide(OpenAmigaGuide(&guide,TAG_DONE));
  if (copper) amiga_setcopper();
  amiga_syscolours(1);
}

void amiga_about(void)
{
  amiga_req("Magnetic v2.3, an interpreter for Magnetic Scrolls games.\n"
	    "Copyright © 1997-2008 Niclas Karlsson.\n\n"
	    "Magnetic is released under the terms of the GNU General\n"
            "Public License. See the file COPYING that is included\n"
            "with this program for details.\n\n"
	    "Magnetic was written by Niclas Karlsson, David Kinder,\n"
            "Stefan Meier and Paul David Doherty. This Amiga\n"
            "version was written by David Kinder.","Continue");
}

LONG amiga_req(UBYTE *text,UBYTE *gadgets,...)
{
int copper;
va_list arguments;
LONG return_value;
static struct EasyStruct requester =
  { sizeof(struct EasyStruct),0,TitleBar,0,0 };

  copper = CopperActive;
  amiga_syscolours(0);
  if (copper)
    amiga_clearcopper(1);
  requester.es_TextFormat = text;
  requester.es_GadgetFormat = gadgets;
  va_start(arguments,gadgets);
  amiga_busy(1);
  return_value = EasyRequestArgs(Window,&requester,0,arguments);
  amiga_busy(0);
  va_end(arguments);
  if (copper)
    amiga_setcopper();
  amiga_syscolours(1);
  return return_value;
}

void amiga_busy(int busy)
{
  if (IntuitionBase->LibNode.lib_Version >= 39)
  {
    if (Window)
      SetWindowPointer(Window,WA_BusyPointer,busy,TAG_DONE);
    if (PictureWindow)
      SetWindowPointer(PictureWindow,WA_BusyPointer,busy,TAG_DONE);
  }
}

void amiga_input(char *ibuff,int size)
{
int c,pos = 0,del = 0;
unsigned long saved_pen;
UWORD qualifier;

  amiga_flush(0);
  *ibuff = '\0';

  if (PreviousChar & CHAR_DELETED)
  {
    if (isalnum(PreviousChar&0xff))
    {
    int x;

      x = RastPort->cp_x;
      amiga_ch(PreviousChar&0xff);
      amiga_flush(0);
      Move(RastPort,x,RastPort->cp_y);
      del = 1;
    }
  }

  amiga_cursor(*ibuff);
  saved_pen = RastPort->FgPen;
  SetAPen(RastPort,ColInput);

  while (TRUE)
  {
    amiga_setwin(1,TextLength(RastPort,ibuff+pos,strlen(ibuff+pos)));
    c = amiga_getch(&qualifier,*(ibuff+pos));
    amiga_setwin(0,0);
    if (del)
    {
      amiga_cursor(*(ibuff+pos));
      amiga_rect(RastPort->cp_x,RastPort->cp_y,
	Window->Width-Window->BorderRight-1,
	RastPort->cp_y+RastPort->TxHeight-1,0);
      amiga_cursor(*(ibuff+pos));
      del = 0;
    }

    switch (c)
    {
    case 13:
      amiga_cursor(*(ibuff+pos));
      amiga_crright(ibuff,&pos);
      SetAPen(RastPort,ColText);
      amiga_storeh(ibuff);
      MoreCount = -1;
      return;

    case KEYCODE_RESIZE_WINDOW:
      amiga_redrawline(ibuff,&pos,size);
      break;

    case KEYCODE_PASSWORD:
      if ((HistoryPosition > 0) && (*(History+HistoryPosition-1) != 0))
      {
      extern type8 result[128];

	passwd(*(History+HistoryPosition-1));
	amiga_inbuf(ibuff,result,&pos,size);
      }
      break;

    case KEYCODE_TEXT_FRONT:
      WindowToFront(Window);
      break;

    case KEYCODE_GFX_FRONT:
      if (PictureWindow) WindowToFront(PictureWindow);
      break;

    case KEYCODE_BACKSPACE:
      if (pos > 0)
      {
      int deleted;

	amiga_cursor(*(ibuff+pos));
	deleted = *(ibuff+pos-1);
	memmove(ibuff+pos-1,ibuff+pos,strlen(ibuff)-pos+1);
	amiga_mvtext(-amiga_chrlen(deleted),
	  Window->Width-Window->BorderRight);
	Move(RastPort,RastPort->cp_x-amiga_chrlen(deleted),RastPort->cp_y);
	pos--;
	amiga_cursor(*(ibuff+pos));
      }
      break;

    case KEYCODE_DELETE:
      if (pos < strlen(ibuff))
      {
      int deleted;

	amiga_cursor(*(ibuff+pos));
	deleted = *(ibuff+pos);
	memmove(ibuff+pos,ibuff+pos+1,strlen(ibuff)-pos);
	Move(RastPort,RastPort->cp_x+amiga_chrlen(deleted),RastPort->cp_y);
	amiga_mvtext(-amiga_chrlen(deleted),
	  Window->Width-Window->BorderRight);
	Move(RastPort,RastPort->cp_x-amiga_chrlen(deleted),RastPort->cp_y);
	amiga_cursor(*(ibuff+pos));
      }
      break;

    case KEYCODE_CURSOR_LEFT:
      if (pos > 0)
      {
	if (qualifier & QUALIFIER_SHIFT)
	{
	  amiga_cursor(*(ibuff+pos));
	  amiga_crleft(ibuff,&pos);
	  amiga_cursor(*(ibuff+pos));
	}
	else
	{
	  amiga_cursor(*(ibuff+pos--));
	  Move(RastPort,RastPort->cp_x-amiga_chrlen(*(ibuff+pos)),
	    RastPort->cp_y);
	  amiga_cursor(*(ibuff+pos));
	}
      }
      break;

    case KEYCODE_CURSOR_RIGHT:
      if (pos < strlen(ibuff))
      {
	if (qualifier & QUALIFIER_SHIFT)
	{
	  amiga_cursor(*(ibuff+pos));
	  amiga_crright(ibuff,&pos);
	  amiga_cursor(*(ibuff+pos));
	}
	else
	{
	  amiga_cursor(*(ibuff+pos));
	  Move(RastPort,RastPort->cp_x+amiga_chrlen(*(ibuff+pos)),
	    RastPort->cp_y);
	  pos++;
	  amiga_cursor(*(ibuff+pos));
	}
      }
      break;

    case KEYCODE_CURSOR_UP:
      if (qualifier & QUALIFIER_SHIFT)
      {
	HistoryPosition = 0;
	while ((*(History + HistoryPosition) == 0) &&
	  (HistoryPosition < HISTORY_LINES)) HistoryPosition++;
	amiga_inbuf(ibuff,HistoryPosition == HISTORY_LINES ?
	  (unsigned char *) "" : *(History+HistoryPosition),&pos,size);
      }
      else
      {
	if ((HistoryPosition > 0) && (*(History+HistoryPosition-1) != 0))
	{
	  HistoryPosition--;
	  amiga_inbuf(ibuff,*(History+HistoryPosition),&pos,size);
	}
      }
      break;

    case KEYCODE_CURSOR_DOWN:
      if (qualifier & QUALIFIER_SHIFT)
      {
	HistoryPosition = HISTORY_LINES;
	amiga_inbuf(ibuff,"",&pos,size);
      }
      else
      {
	if ((HistoryPosition < HISTORY_LINES-1) &&
	  (*(History+HistoryPosition+1) != 0))
	{
	  HistoryPosition++;
	  amiga_inbuf(ibuff,HistoryPosition == HISTORY_LINES ?
	    (unsigned char *) "" : *(History+HistoryPosition),&pos,size);
	}
	else
	{
	  HistoryPosition = HISTORY_LINES;
	  amiga_inbuf(ibuff,"",&pos,size);
	}
      }
      break;

    default:
      if (c >= 32 && c <= 127)
      {
	if ((strlen(ibuff) < size) && (amiga_fittext(ibuff+pos,c)))
	{
	  memmove(ibuff+pos+1,ibuff+pos,strlen(ibuff)-pos+1);
	  *(ibuff+pos) = c;

	  pos++;
	  amiga_cursor(*(ibuff+pos));
	  amiga_mvtext(amiga_chrlen(c),Window->Width-Window->BorderRight);
	  amiga_ch(c);
	  amiga_cursor(*(ibuff+pos));
	}
      }
      break;
    }
  }
}

void amiga_redrawline(unsigned char *buffer,int *pos,int max_size)
{
char *current_buffer;

  amiga_cursor(*(buffer+*pos));
  if ((current_buffer = AllocVec(max_size,MEMF_CLEAR)) == 0) return;
  strcpy(current_buffer,buffer);
  amiga_inbuf(buffer,current_buffer,pos,max_size);
  FreeVec(current_buffer);
}

void amiga_mvtext(int offset,int max)
{
int xSource, xDest;

  xSource = RastPort->cp_x;
  xDest = RastPort->cp_x+offset;
  ClipBlit(RastPort,xSource,RastPort->cp_y,RastPort,xDest,RastPort->cp_y,
    max-MAX(xSource,xDest),RastPort->TxHeight,0xC0);
}

void amiga_crleft(unsigned char *buffer,int *pos)
{
  while (*pos > 0)
  {
    (*pos)--;
    Move(RastPort,RastPort->cp_x-amiga_chrlen(*(buffer+*pos)),
      RastPort->cp_y);
  }
}

void amiga_crright(unsigned char *buffer,int *pos)
{
  while (*pos < strlen(buffer))
  {
    Move(RastPort,RastPort->cp_x+amiga_chrlen(*(buffer+*pos)),
      RastPort->cp_y);
    (*pos)++;
  }
}

int amiga_fittext(unsigned char *pointer,int new)
{
int a,b;

  a = TextLength(RastPort,pointer,strlen(pointer))+amiga_chrlen(new)+
    RastPort->cp_x+RastPort->TxWidth;
  b = Window->Width-Window->BorderRight;
  return (a > b) ? 0 : 1;
}

void amiga_inbuf(unsigned char *buffer,unsigned char *new,int *pos,
  int max_size)
{
int saved_x_position,saved_colour,i;

  if (new == 0) return;
  amiga_cursor(*(buffer+*pos));

  amiga_crleft(buffer,pos);
  saved_colour = RastPort->FgPen;
  saved_x_position = RastPort->cp_x;
  SetAPen(RastPort,RastPort->BgPen);
  for (i = 0; i < strlen(buffer); i++) amiga_ch(*(buffer+i));
  amiga_flush(0);
  SetAPen(RastPort,saved_colour);
  Move(RastPort,saved_x_position,RastPort->cp_y);

  *buffer = 0;
  while (TRUE)
  {
    if ((amiga_fittext(buffer,*(new+*pos)) == 0) || (*(new+*pos) == 0)
      || (*pos == max_size))
    {
      for (i = 0; i < strlen(buffer); i++) amiga_ch(*(buffer+i));
      amiga_flush(0);
      amiga_cursor(*(buffer+*pos));
      return;
    }
    else
    {
      *(buffer+*pos) = *(new+*pos);
      (*pos)++;
      *(buffer+*pos) = 0;
    }
  }
}

void amiga_storeh(unsigned char *line)
{
int i;

  if ((*line != 0) && (amiga_cmp(*(History+HISTORY_LINES-1),line) != 0))
  {
    if (*History) FreeVec(*History);
    for (i = 0; i < HISTORY_LINES-1; i++) *(History+i) = *(History+i+1);
    if (*(History+HISTORY_LINES-1) = AllocVec(strlen(line)+1, MEMF_CLEAR))
      strcpy(*(History+HISTORY_LINES-1),line);
  }
  HistoryPosition = HISTORY_LINES;
}

int amiga_cmp(const char *a,const char *b)
{
  if ((a == 0) || (b == 0)) return 1;
  return strcmp (a, b);
}

type8 amiga_getinputch(void)
{
type8 c;

  if (InputBufferPtr == -1)
  {
    amiga_input(InputBuffer,INPUTBUFFER_SIZE);
    InputBufferPtr = 0;
  }

  c = *(InputBuffer+InputBufferPtr++);
  if (c == 0)
  {
    c = 0x0a;
    InputBufferPtr = -1;
  }
  return c;
}

void amiga_statusch(char c)
{
static int i = 0, mode = 0;
int x,y,x1;

  switch (c)
  {
  case 0x09:
    *(StatusLoc+i) = 0;
    mode = 1;
    i = 0;
    break;
  case 0x0a:
    *(StatusScore+i) = 0;
    mode = 0;
    i = 0;

    x = RastPort->cp_x;
    y = RastPort->cp_y;

    SetAPen(RastPort,ColStatText);
    SetBPen(RastPort,ColStatBar);
    Move(RastPort,Window->BorderLeft+
      ((Window->Width-Window->BorderLeft-Window->BorderRight)/20),
      Window->BorderTop+RastPort->TxBaseline);
    amiga_rect(Window->BorderLeft,Window->BorderTop,
      RastPort->cp_x-1,Window->BorderTop+RastPort->TxHeight-1,ColStatBar);
    Text(RastPort,StatusLoc,strlen(StatusLoc));
    x1 = RastPort->cp_x;
    Move(RastPort,Window->BorderLeft+
      (((Window->Width-Window->BorderLeft-Window->BorderRight)*19)/20)-
      TextLength(RastPort,StatusScore,strlen(StatusScore)),
      Window->BorderTop+RastPort->TxBaseline);
    amiga_rect(x1,Window->BorderTop,
      RastPort->cp_x-1,Window->BorderTop+RastPort->TxHeight-1,ColStatBar);
    Text(RastPort,StatusScore,strlen(StatusScore));
    amiga_rect(RastPort->cp_x,Window->BorderTop,
      Window->Width-Window->BorderRight-1,
      Window->BorderTop+RastPort->TxHeight-1,ColStatBar);

    Move(RastPort,x,y);
    SetAPen(RastPort,ColText);
    SetBPen(RastPort,0);
    break;
  default:
    if (isprint(c))
    {
      if (mode == 1)
	*(StatusScore+i) = c;
      else
	*(StatusLoc+i) = c;
      i++;
    }
    break;
  }
}

void amiga_freq(char *initdir,char *buffer,char *title,char *pattern)
{
  if (FileReq == 0)
  {
    FileReq = AllocAslRequestTags(ASL_FileRequest,
      strcmp(initdir,"") == 0 ? TAG_IGNORE : ASLFR_InitialDrawer,initdir,
      ASLFR_SleepWindow, 1,
      ASLFR_RejectIcons,1,
      ASLFR_DoPatterns,1,TAG_DONE);
  }
  strcpy(buffer,"");
  if (FileReq)
  {
    if (AslRequestTags(FileReq,
      ASLFR_Window,Window,
      ASLFR_TitleText,title,
      ASLFR_InitialPattern,pattern,TAG_DONE))
    {
      strcpy(buffer,FileReq->fr_Drawer);
      AddPart(buffer,FileReq->fr_File,256);
    }
  }
}

char *amiga_getextname(char *gamename, char *newname, const char* ext)
{
char *filepart;
char *extpart;

  strcpy(newname,gamename);
  filepart = FilePart(newname);

  extpart = strrchr(filepart,'.');
  if (extpart == NULL)
    extpart = filepart+strlen(filepart);
  strcpy(extpart,ext);

  return newname;
}

int amiga_sizewidth(void)
{
struct DrawInfo *dri;
struct Image *size;
int width = 0;

  if (dri = GetScreenDrawInfo(Window->WScreen))
  {
    if (size = NewObject(NULL,SYSICLASS,
      SYSIA_DrawInfo,dri,SYSIA_Which,SIZEIMAGE,TAG_DONE))
    {
      width = size->Width;
      DisposeObject(size);
    }
    FreeScreenDrawInfo(Window->WScreen,dri);
  }
  return width;
}

void amiga_showpic(type8 *buffer)
{
int i,j,val;
int alloc = 0;

  if (Graphics == GFX_NONE) return;
  amiga_freepic();

  BitMapWidth = GfxDoubleWidth ? pic_width*2 : pic_width;
  InitBitMap(&PicBitMap,PIC_DEPTH,BitMapWidth,pic_height);

  for (i = 0; i < PIC_DEPTH; i++)
  {
    if (PicBitMap.Planes[i] = AllocRaster(BitMapWidth,pic_height))
    {
      BltClear(PicBitMap.Planes[i],RASSIZE(BitMapWidth,pic_height),0);
      alloc++;
    }
  }
  BitMapMem = 1;

  if (alloc == PIC_DEPTH)
  {
  int bitposx,xbit,bits,picsize;
  int pos = 0;
  int ypos = 0;
  int bitpos = 0;

    i = 0;
    bits = GfxDoubleWidth ? 128+64 : 128;
    picsize = pic_width*pic_height;
    do
    {
      if (pos >= pic_width)
      {
	ypos++;
	bitpos = ypos*PicBitMap.BytesPerRow;
	pos = 0;
      }
      val = PicturePens[buffer[i++]];
      xbit = GfxDoubleWidth ? pos*2 : pos;
      bitposx = bitpos+(xbit>>3);
      for (j = 0; j < PIC_DEPTH; j++)
      {
	if (val & (1<<j))
	  *(PicBitMap.Planes[j]+bitposx) |= bits>>(xbit&7);
      }
      pos++;
    }
    while (i < picsize);
  }
  else
  {
    amiga_freepic();
    return;
  }

  for (i = 0; i < 16; i++)
  {
  int r,g,b;

    r = (pic_palette[i]&0x0F00)>>7;
    g = (pic_palette[i]&0x00F0)>>3;
    b = (pic_palette[i]&0x000F)<<1;

    switch (Graphics)
    {
      case GFX_WINDOW:
	SetRGB4(&(Window->WScreen->ViewPort),PicturePens[i],r,g,b);
	break;
      case GFX_COPPER:
	CustomColours[i] = (r<<8)|(g<<4)|b;
	break;
    }
  }

  amiga_openpicwin();

  if (Graphics == GFX_WINDOW)
  {
    WindowLimits(PictureWindow,128,64,-1,-1);
    ChangeWindowBox(PictureWindow,
      PictureWindow->LeftEdge,
      PictureWindow->TopEdge,
      BitMapWidth+Window->WScreen->WBorLeft+amiga_sizewidth(),
      pic_height+Window->WScreen->Font->ta_YSize+
	Window->WScreen->WBorTop+1+Window->WScreen->WBorBottom);
    LimitPicWin = 1;
   }

  SetAPen(PictureWindow->RPort,0);
  RectFill(PictureWindow->RPort,
    PictureWindow->BorderLeft,
    PictureWindow->BorderTop,
    PictureWindow->Width-PictureWindow->BorderRight-1,
    PictureWindow->Height-PictureWindow->BorderBottom-1);
  if (Graphics == GFX_COPPER)
  {
    amiga_setcopper();
    amiga_drawpic();
  }
}

void amiga_freepic(void)
{
  if (BitMapMem)
  {
  int i;

    for (i = 0; i < PIC_DEPTH; i++)
    {
      if (PicBitMap.Planes[i])
      {
	FreeRaster(PicBitMap.Planes[i],BitMapWidth,pic_height);
	PicBitMap.Planes[i] = NULL;
      }
    }
    BitMapMem = 0;
  }
}

void amiga_drawpic(void)
{
  if (BitMapMem)
  {
  int l,w,h;

    w = MIN(BitMapWidth,PictureWindow->Width-PictureWindow->BorderLeft-
      PictureWindow->BorderRight);
    h = MIN(pic_height,PictureWindow->Height-PictureWindow->BorderTop-
      PictureWindow->BorderBottom);
    l = (Graphics == GFX_COPPER) ?
      (PictureWindow->Width-BitMapWidth)/2 : PictureWindow->BorderLeft;
    BltBitMapRastPort(&PicBitMap,0,0,PictureWindow->RPort,l,
      PictureWindow->BorderTop,w,h,0xC0);
  }
}

void amiga_freepens(void)
{
  if (Window && (Graphics == GFX_WINDOW))
  {
  int i;

    for (i = 0; i < 16; i++)
    {
      if (PicturePens[i] != -1)
      {
	ReleasePen(Window->WScreen->ViewPort.ColorMap,PicturePens[i]);
	PicturePens[i] = -1;
      }
    }
  }
}

void amiga_setcopper(void)
{
#ifndef __AROS__
extern __far struct Custom custom;
struct TagItem uCopTags[] = {
  { VTAG_USERCLIP_SET,0L },
  { VTAG_END_CM,0L }
};
struct UCopList *uCopList;

  if (CopperActive) amiga_clearcopper(0);

  if (uCopList = AllocVec(sizeof(struct UCopList),MEMF_CLEAR))
  {
  int i;

    CINIT(uCopList,64);
    if (Screen->ViewPort.Modes & INTERLACE)
    {
      CWAIT(uCopList,PictureWindow->TopEdge-1,0);
    }
    else
    {
      CWAIT(uCopList,PictureWindow->TopEdge,0);
    }
    for (i = 0; i < 16; i++)
      CMOVE(uCopList,custom.color[i],CustomColours[i]);
    if (Screen->ViewPort.Modes & INTERLACE)
    {
      CWAIT(uCopList,PictureWindow->TopEdge-1+FIXED_GFX_HEIGHT,0);
    }
    else
    {
      CWAIT(uCopList,PictureWindow->TopEdge+FIXED_GFX_HEIGHT,0);
    }
    for (i = 0; i < 16; i++)
      CMOVE(uCopList,custom.color[i],ScreenColours[i]);
    CEND(uCopList);

    Forbid();
    Screen->ViewPort.UCopIns = uCopList;
    Permit();
    VideoControl(Screen->ViewPort.ColorMap,uCopTags);
    RethinkDisplay();
  }

  CopperActive = 1;
#endif /* !__AROS__ */
}

void amiga_clearcopper(int think)
{
#ifndef __AROS__
struct TagItem uCopTags[] = {
  { VTAG_USERCLIP_CLR,0L },
  { VTAG_END_CM,0L }
};
struct UCopList *uCopList;

  if (CopperActive == 0) return;

  VideoControl(Screen->ViewPort.ColorMap,uCopTags);
  Forbid();
  uCopList = Screen->ViewPort.UCopIns;
  Screen->ViewPort.UCopIns = 0;
  Permit();
  FreeCopList(uCopList->FirstCopList);
  FreeVec(uCopList);

  if (think) RethinkDisplay();

  CopperActive = 0;
#endif /* !__AROS__ */
}

void amiga_syscolours(int black)
{
  if (Display == DISP_BLACK) LoadRGB4(&Screen->ViewPort,
    black ? BlackColours : BlackSysColours,4);
}

void amiga_closepicwin(void)
{
  current_pic = -1;
  amiga_freepic();
  amiga_clearcopper(1);
  if (PictureWindow)
  {
    CloseWindowSafely(&PictureWindow);
    if (Graphics == GFX_COPPER)
    {
      if ((Display == DISP_SCREEN) || (Display == DISP_BLACK))
      {
      int window_top;

	window_top = Display == DISP_BLACK ? 0 : Screen->BarHeight+1;
	ChangeWindowBox(Window,0,window_top,Window->Width,
	  Screen->Height-window_top);
      }
    }
  }
}

void amiga_preparepicwin(void)
{
  if (PictureWindow == NULL)
  {
    if (Graphics == GFX_COPPER)
    {
      if ((Display == DISP_SCREEN) || (Display == DISP_BLACK))
      {
      int window_top;

	window_top = Display == DISP_BLACK ? 0 : Screen->BarHeight+1;
	window_top += FIXED_GFX_HEIGHT;
	ChangeWindowBox(Window,0,window_top,Window->Width,
	  Screen->Height-window_top);
	amiga_rect(Window->BorderLeft,Window->BorderTop+RastPort->TxHeight,
	  Window->Width-Window->BorderRight-1,
	  Window->Height-Window->BorderTop-RastPort->TxHeight-1,0);
	amiga_resetcrsr();
      }
    }
  }
}

void amiga_openpicwin(void)
{
  if (PictureWindow == NULL)
  {
  int gfxwin_left, gfxwin_top;

    if (MagneticPrefs.GfxWinLeft != -1)
    {
      gfxwin_left = MagneticPrefs.GfxWinLeft;
      gfxwin_top = MagneticPrefs.GfxWinTop;
    }
    else
    {
      gfxwin_left = 0;
      gfxwin_top = 0;
    }

    switch (Graphics)
    {
      case GFX_WINDOW:
	if ((PictureWindow = OpenWindowTags(0,
	  WA_Left,gfxwin_left,
	  WA_Top,gfxwin_top,
	  WA_Width,BitMapWidth+Window->WScreen->WBorLeft+amiga_sizewidth(),
	  WA_Height,pic_height+Window->WScreen->Font->ta_YSize+
	    Window->WScreen->WBorTop+1+Window->WScreen->WBorBottom,
	  WA_SmartRefresh,1,
	  WA_NewLookMenus,1,
	  WA_AutoAdjust,1,
	  WA_Activate,0,
	  WA_CloseGadget,1,
	  WA_DragBar,1,
	  WA_DepthGadget,1,
	  WA_SizeGadget,1,
	  WA_SizeBRight,1,
	  WA_IDCMP,0,
	  WA_Title,TitleBar,
	  WA_ScreenTitle,TitleBar,
	  WA_CustomScreen,Window->WScreen,TAG_DONE)) == NULL) return;
	break;
      case GFX_COPPER:
	if ((PictureWindow = OpenWindowTags(0,
	  WA_Left,0,
	  WA_Top,Display == DISP_BLACK ? 0 : Screen->BarHeight+1,
	  WA_Width,Screen->Width,
	  WA_Height,FIXED_GFX_HEIGHT,
	  WA_SmartRefresh,1,
	  WA_NewLookMenus,1,
	  WA_Borderless,1,
	  WA_Backdrop,1,
	  WA_Activate,0,
	  WA_CloseGadget,0,
	  WA_DragBar,0,
	  WA_DepthGadget,0,
	  WA_SizeGadget,0,
	  WA_IDCMP,0,
	  WA_ScreenTitle,TitleBar,
	  WA_CustomScreen,Screen,TAG_DONE)) == NULL) return;
	break;
      default:
	return;
    }

    PictureWindow->UserPort = MsgPort;
    ModifyIDCMP(PictureWindow,
      IDCMP_RAWKEY|IDCMP_VANILLAKEY|IDCMP_MENUPICK|IDCMP_CLOSEWINDOW|
      IDCMP_CHANGEWINDOW);
    SetMenuStrip(PictureWindow,Menus);
  }
}

BPTR amiga_open_prefs(void)
{
BPTR prefs_file;

  if ((prefs_file = Open("PROGDIR:Magnetic.prefs",MODE_OLDFILE)) != 0)
    return prefs_file;
  if ((prefs_file = Open("ENVARC:Magnetic.prefs",MODE_OLDFILE)) != 0)
    return prefs_file;
  return 0;
}

void amiga_read_prefs(void)
{
BPTR prefs_file;

  MagneticPrefs.Version = 1;
  MagneticPrefs.WindowLeft = -1;
  MagneticPrefs.WindowTop = -1;
  MagneticPrefs.WindowWidth = -1;
  MagneticPrefs.WindowHeight = -1;
  MagneticPrefs.GfxWinLeft = -1;
  MagneticPrefs.GfxWinTop = -1;

  if (prefs_file = amiga_open_prefs())
  {
    Read(prefs_file,&MagneticPrefs,sizeof(struct MagneticPreferences));
    Close(prefs_file);
  }
}

void amiga_write_prefs(void)
{
BPTR prefs_file;

  if (Window && (Display == DISP_WINDOW))
  {
    MagneticPrefs.WindowLeft = Window->LeftEdge;
    MagneticPrefs.WindowTop = Window->TopEdge;
    MagneticPrefs.WindowWidth = Window->Width;
    MagneticPrefs.WindowHeight = Window->Height;
  }
  if (PictureWindow && (Graphics == GFX_WINDOW))
  {
    MagneticPrefs.GfxWinLeft = PictureWindow->LeftEdge;
    MagneticPrefs.GfxWinTop = PictureWindow->TopEdge;
  }

  if ((prefs_file = Open("PROGDIR:Magnetic.prefs",MODE_NEWFILE)) != 0)
  {
    Write(prefs_file,&MagneticPrefs,sizeof(struct MagneticPreferences));
    Close(prefs_file);
  }
}

#ifdef _DCC
int wbmain(struct WBStartup *wbmsg)
{
  main(0,(char **)wbmsg);
}
#endif /* _DCC */

main(int argc, char **argv)
{
type8 running, *gfxname = 0, *hintname = 0;
type32 dlimit, slimit;

  atexit(amiga_exit);
  dlimit = slimit = 0xffffffff;

  if (argc == 0)
  {
  struct WBStartup *wbmsg = (struct WBStartup *)argv;
  char startdir[256], *dir;
  static char gamepath[256];

    strcpy(startdir,"");
    if (Icon = GetDiskObject(wbmsg->sm_ArgList[0].wa_Name))
      if (dir = FindToolType(Icon->do_ToolTypes,"DIR"))
	strcpy(startdir,dir);

    amiga_freq(startdir,gamepath,TitleBar,"#?.mag");
    gamename = gamepath;
  }
  else
  {
    int i;
    for (i = 1; i < argc; i++)
    {
      if (argv[i][0] == '-')
      {
	switch (tolower(argv[i][1]))
	{
	  case 'd':
	    if (strlen(argv[i]) > 2)
	      dlimit = atoi(&argv[i][2]);
	    else dlimit = 0;
	    break;
	  case 's':
	    if (strlen(argv[i]) > 2)
	      slimit = atoi(&argv[i][2]);
	    else slimit = 655360;
	    break;
	  case 't':
	    if (!(logfile2 = fopen(&argv[i][2],"w")))
	      printf("Failed to open \"%s\" for writing.\n",&argv[i][2]);
	    break; 
	  case 'r':
	    if (logfile1 = fopen(&argv[i][2],"r"))
	      log_on = 1;
	    else printf("Failed to open \"%s\" for reading.\n",&argv[i][2]);
	    break;
	  case 'w':
	    if (logfile1 = fopen(&argv[i][2],"w"))
	      log_on = 2;
	    else printf("Failed to open \"%s\" for writing.\n",&argv[i][2]);
	    break;
	  default:
	    printf("Unknown option -%c, ignoring.\n",argv[i][1]);
	}
      }
      else
      {
	if (!gamename)
	  gamename=argv[i];
	else if (!gfxname)
	  gfxname=argv[i];
	else if (!hintname)
	  hintname=argv[i];
      }
    }

    if (!gamename)
    {
      printf("Magnetic v2.3, an interpreter for Magnetic Scrolls games. Copyright © Niclas\n"
	     "Karlsson 1997-2008. Written by Niclas Karlsson, David Kinder, Stefan Meier\n"
	     "and Paul David Doherty.\n\n"
	     "Syntax: %s [options] game [gfxfile] [hintname]\n\n"
	     "Where the options are:\n"
	     " -rname read script file\n"
	     " -tname write transcript file\n"
	     " -wname write script file\n\n"
	     "The interpreter commands are:\n"
	     " #undo   to undo the previous command\n"
	     " #logoff to turn off script writing\n\n",argv[0]);
      exit(1);
    }
  }

  if (gfxname == 0)
    gfxname = amiga_getextname(gamename,gfx_name,".gfx");
  if (hintname == 0)
    hintname = amiga_getextname(gamename,hint_name,".hnt");
  if (!(ms_gfx_enabled = ms_init(gamename,gfxname,hintname,NULL)))
  {
    printf("Couldn't start up game \"%s\".\n",gamename);
    exit(1);
  }
  amiga_init();

  ms_gfx_enabled--;
  running = 1;
  while ((i_count < slimit) && running)
  {
    if (i_count >= dlimit) ms_status();
    running = ms_rungame();
  }
  if (i_count == slimit)
  {
    printf("\n\nSafety limit (%d) reached.\n",(int)slimit);
    ms_status();
  }
  ms_freemem();
  if (log_on)
    fclose(logfile1);
  exit(0);
}

