/***********************************************************************\
*
* Level 9 interpreter
* Version 5.1
* Copyright (c) 1996-2011 Glen Summers and contributors.
* Contributions from David Kinder, Alan Staniforth, Simon Baldwin,
* Dieter Baron and Andreas Scherrer.
*
* Level9 Amiga version by David Kinder.
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

#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>
#include <string.h>
#include <proto/asl.h>
#include <proto/amigaguide.h>
#include <proto/diskfont.h>
#include <proto/dos.h>
#include <proto/exec.h>
#include <proto/gadtools.h>
#include <proto/graphics.h>
#include <proto/icon.h>
#include <proto/intuition.h>
#include <exec/memory.h>
#include <graphics/videocontrol.h>
#include <libraries/asl.h>
#include <workbench/startup.h>
#include "level9.h"

#define QUALIFIER_SHIFT (IEQUALIFIER_LSHIFT|IEQUALIFIER_RSHIFT)
#define MAX(a,b) (((a) > (b)) ? (a) : (b))

struct NewMenu NewMenus[] =
{
  {NM_TITLE, "Project", 0, 0, 0, 0},
  {NM_ITEM, "New Game...", "N", 0, 0, 0},
  {NM_ITEM, NM_BARLABEL, 0, 0, 0, 0},
  {NM_ITEM, "Save...", "S", 0, 0, 0},
  {NM_ITEM, "Restore...", "R", 0, 0, 0},
  {NM_ITEM, NM_BARLABEL, 0, 0, 0, 0},
  {NM_ITEM, "Help...", "H", 0, 0, 0},
  {NM_ITEM, "About...", "?", 0, 0, 0},
  {NM_ITEM, NM_BARLABEL, 0, 0, 0, 0},
  {NM_ITEM, "Quit", "Q", 0, 0, 0},
  {NM_END, 0, 0, 0, 0, 0}};

char Version[] = "$VER:Level9 5.1 (29.05.2011)";
char TitleBar[] = "Level 9";

#define TEXTBUFFER_SIZE 1024
char TextBuffer[TEXTBUFFER_SIZE+1];
int TextBufferPtr;

#define HISTORY_LINES 20
unsigned char *History[HISTORY_LINES];
int HistoryPosition = HISTORY_LINES;

extern struct Library *IntuitionBase;
struct Library *AmigaGuideBase;
struct Screen *Screen, *DefaultPubScreen;
struct Window *Window, *OldWindowPtr;
struct RastPort *RastPort;
struct Menu *Menus;
struct DiskObject *Icon;
struct TextFont *Font;
struct FileRequester *GameReq, *SaveReq, *ScriptReq;
struct Process *ThisProcess;
APTR Visual;

int ScreenWidth, ScreenHeight;
int DisplayHeight, PreviousHeight;
int MoreCount;
int Playing;

extern FILE* scriptfile;

void amiga_init (char *dir);
void screen_ratio (struct Screen *screen);
void reset_cursor (void);
void rect (long xMin, long yMin, long xMax, long yMax, unsigned long pen);
void text (char *s, int n);
int check_len (char *s, int n);
void cursor (int under);
void set_window (int limit, int x);
int get_key (UWORD * qualifier_addr, int c, int wait);
void redraw_line (unsigned char *buffer, int *pos, int max_size);
void move_text (int offset, int max);
void cursor_left (unsigned char *buffer, int *pos);
void cursor_right (unsigned char *buffer, int *pos);
int fit_text (unsigned char *pointer, int new);
void into_buffer (unsigned char *buffer, unsigned char *new, int *pos, int max_size);
void store_hist (unsigned char *line);
int cmp (const char *a, const char *b);
void help (void);
void about (void);
LONG req (UBYTE * text, UBYTE * gadgets,...);
void busy (int busy);
struct FileRequester *alloc_freq (char *initdir);
void filereq (struct FileRequester *freq, char *buffer, char *title, ULONG save);
int newgame (char *fname);

void os_printchar (char c)
{
  if (Window == 0)
  {
    if (c == '\r')
      c = '\n';
    fputc (c, stdout);
    return;
  }

  if (c == '\r')
  {
    os_flush ();
    ClipBlit (RastPort,
	      Window->BorderLeft,
	      Window->BorderTop + RastPort->TxHeight,
	      RastPort,
	      Window->BorderLeft,
	      Window->BorderTop,
	      Window->Width - Window->BorderLeft - Window->BorderRight,
	      DisplayHeight * RastPort->TxHeight, 0xC0);
    rect (Window->BorderLeft,
	  Window->BorderTop + DisplayHeight * RastPort->TxHeight,
	  Window->Width - Window->BorderRight - 1,
	  Window->BorderTop + (DisplayHeight + 1) * RastPort->TxHeight - 1, 0);
    reset_cursor ();

    if (scriptfile == NULL)
    {
      if (++MoreCount >= DisplayHeight)
      {
	MoreCount = 0;
	text ("[More]", 6);
	cursor (0);
	get_key (0, -1, 1);
	cursor (0);
	rect (Window->BorderLeft,
	      Window->BorderTop + DisplayHeight * RastPort->TxHeight,
	      Window->Width - Window->BorderRight - 1,
	      Window->BorderTop + (DisplayHeight + 1) * RastPort->TxHeight - 1, 0);
        reset_cursor ();
      }
    }

    return;
  }

  if (isprint(c) == 0)
    return;
  if (TextBufferPtr >= TEXTBUFFER_SIZE)
    os_flush ();
  *(TextBuffer + (TextBufferPtr++)) = c;
}

L9BOOL os_input (char *ibuff, int size)
{
  int c, pos = 0;
  int inputting = 1;
  unsigned long saved_pen;
  UWORD qualifier;

  *ibuff = '\0';
  cursor (*ibuff);
  saved_pen = RastPort->FgPen;
  SetAPen (RastPort, 2);

  while (inputting)
  {
    set_window (1, TextLength (RastPort, ibuff + pos, strlen (ibuff + pos)));
    c = get_key (&qualifier, *(ibuff + pos), 1);
    set_window (0, 0);

    switch (c)
    {
    case -1:
      cursor (*(ibuff + pos));
      SetAPen (RastPort, 1);
      MoreCount = 0;
      return 0;

    case -2:
      cursor (*(ibuff + pos));
      cursor_right (ibuff, &pos);
      strcpy(ibuff,"save");
      SetAPen (RastPort, 1);
      MoreCount = 0;
      return 1;

    case -3:
      cursor (*(ibuff + pos));
      cursor_right (ibuff, &pos);
      strcpy(ibuff,"#restore");
      SetAPen (RastPort, 1);
      MoreCount = 0;
      return 1;

    case 13:
      if (strlen (ibuff) > 0)
      {
	cursor (*(ibuff + pos));
	cursor_right (ibuff, &pos);
	SetAPen (RastPort, 1);
	store_hist (ibuff);
	MoreCount = 0;
	return 1;
      }
      break;

    case 264:
      redraw_line (ibuff, &pos, size);
      break;

    case 127:
      if (pos > 0)
      {
	int deleted;

	cursor (*(ibuff + pos));
	deleted = *(ibuff + pos - 1);
	memmove (ibuff + pos - 1, ibuff + pos, strlen (ibuff) - pos + 1);
	move_text (-char_len (deleted), Window->Width - Window->BorderRight);
	Move (RastPort, RastPort->cp_x - char_len (deleted), RastPort->cp_y);
	pos--;
	cursor (*(ibuff + pos));
      }
      break;

    case 260:
      if (pos < strlen (ibuff))
      {
	int deleted;

	cursor (*(ibuff + pos));
	deleted = *(ibuff + pos);
	memmove (ibuff + pos, ibuff + pos + 1, strlen (ibuff) - pos);
	Move (RastPort, RastPort->cp_x + char_len (deleted), RastPort->cp_y);
	move_text (-char_len (deleted), Window->Width - Window->BorderRight);
	Move (RastPort, RastPort->cp_x - char_len (deleted), RastPort->cp_y);
	cursor (*(ibuff + pos));
      }
      break;

    case 131:
      if (pos > 0)
      {
	if (qualifier & QUALIFIER_SHIFT)
	{
	  cursor (*(ibuff + pos));
	  cursor_left (ibuff, &pos);
	  cursor (*(ibuff + pos));
	}
	else
	{
	  cursor (*(ibuff + pos--));
	  Move (RastPort, RastPort->cp_x - char_len (*(ibuff + pos)), RastPort->cp_y);
	  cursor (*(ibuff + pos));
	}
      }
      break;

    case 132:
      if (pos < strlen (ibuff))
      {
	if (qualifier & QUALIFIER_SHIFT)
	{
	  cursor (*(ibuff + pos));
	  cursor_right (ibuff, &pos);
	  cursor (*(ibuff + pos));
	}
	else
	{
	  cursor (*(ibuff + pos));
	  Move (RastPort, RastPort->cp_x + char_len (*(ibuff + pos)), RastPort->cp_y);
	  pos++;
	  cursor (*(ibuff + pos));
	}
      }
      break;

    case 129:
      if (qualifier & QUALIFIER_SHIFT)
      {
	HistoryPosition = 0;
	while ((*(History + HistoryPosition) == 0) && (HistoryPosition < HISTORY_LINES))
	  HistoryPosition++;
	into_buffer (ibuff, HistoryPosition == HISTORY_LINES ?
	  (unsigned char *) "" : *(History + HistoryPosition), &pos, size);
      }
      else
      {
	if ((HistoryPosition > 0) && (*(History + HistoryPosition - 1) != 0))
	{
	  HistoryPosition--;
	  into_buffer (ibuff, *(History + HistoryPosition), &pos, size);
	}
      }
      break;

    case 130:
      if (qualifier & QUALIFIER_SHIFT)
      {
	HistoryPosition = HISTORY_LINES;
	into_buffer (ibuff, "", &pos, size);
      }
      else
      {
	if ((HistoryPosition < HISTORY_LINES - 1) && (*(History + HistoryPosition + 1) != 0))
	{
	  HistoryPosition++;
	  into_buffer (ibuff, HistoryPosition == HISTORY_LINES ?
	    (unsigned char *) "" : *(History + HistoryPosition), &pos, size);
	}
	else
	{
	  HistoryPosition = HISTORY_LINES;
	  into_buffer (ibuff, "", &pos, size);
	}
      }
      break;

    default:
      if (c >= 32 && c <= 127)
      {
	if ((strlen (ibuff) < size) && (fit_text (ibuff + pos, c)))
	{
	  memmove (ibuff + pos + 1, ibuff + pos, strlen (ibuff) - pos + 1);
	  *(ibuff + pos) = c;

	  pos++;
	  cursor (*(ibuff + pos));
	  move_text (char_len (c), Window->Width - Window->BorderRight);
	  os_printchar (c);
	  cursor (*(ibuff + pos));
	}
      }
      break;
    }
  }
}

char os_readchar (int millis)
{
  char c;

  MoreCount = 0;
  c = get_key (0, -1, 0);

  if (c == 0)
  {
    Delay((millis*50)/1000);
    c = get_key (0, -1, 0);
  }
  return c;
}

L9BOOL os_stoplist (void)
{
  MoreCount = 0;
  return get_key (0, -1, 0);
}

void os_flush (void)
{
  if (Window == 0)
    return;
  if (TextBufferPtr < 1)
    return;

  static int flushing = 0;

  if (flushing != 0)
    return;
  flushing = 1;

  char *ptr, *space, *lastspace;
  int searching;

  *(TextBuffer+TextBufferPtr) = ' ';
  ptr = TextBuffer;
  while (check_len (ptr, TextBufferPtr))
  {
    space = ptr;
    lastspace = space;
    searching = 1;
    while (searching)
    {
      while (*space != ' ')
	space++;
      if (check_len (ptr, space - ptr))
      {
	space = lastspace;
	text (ptr, space - ptr);
	os_printchar ('\r');
	space++;
	if (*space == ' ')
	  space++;
	TextBufferPtr -= space - ptr;
	ptr = space;
	searching = 0;
      }
      else
	lastspace = space;
      space++;
    }
  }
  if (TextBufferPtr > 0)
    text (ptr, TextBufferPtr);
  TextBufferPtr = 0;

  flushing = 0;
}

L9BOOL os_save_file (L9BYTE * Ptr, int Bytes)
{
  char filename[256];
  FILE *f;

  filereq (SaveReq, filename, "Save Game", 1);
  if (strcmp (filename, "") == 0)
    return FALSE;

  if (f = fopen (filename, "w"))
  {
    fwrite (Ptr, 1, Bytes, f);
    fclose (f);
    return TRUE;
  }
  return FALSE;
}

L9BOOL os_load_file(L9BYTE *Ptr,int *Bytes,int Max)
{
  char filename[256];
  FILE *f;

  filereq (SaveReq, filename, "Restore Game", 0);
  if (strcmp (filename, "") == 0)
    return FALSE;

  if (f = fopen (filename, "r"))
  {
    *Bytes = fread (Ptr, 1, Max, f);
    fclose (f);
    return TRUE;
  }
  return FALSE;
}

FILE* os_open_script_file(void)
{
  char filename[256];

  filereq (ScriptReq, filename, "Play Script", 0);
  if (strcmp (filename, "") == 0)
    return FALSE;

  return fopen (filename, "rt");
}

L9BOOL os_get_game_file(char *NewName,int Size)
{
  filereq (GameReq, NewName, "Next Level9 Game File", 0);
}

void os_set_filenumber(char *NewName,int Size,int n)
{
char *file;
int i;

  file = FilePart(NewName);
  for (i = strlen(file)-1; i >= 0; i--)
  {
    if (isdigit(*(file+i)))
    {
      *(file+i) = '0'+n;
      return;
    }
  }
}

void os_graphics(int mode)
{
}

void os_cleargraphics(void)
{
}

void os_setcolour(int colour, int index)
{
}

void os_drawline(int x1, int y1, int x2, int y2, int colour1, int colour2)
{
}

void os_fill(int x, int y, int colour1, int colour2)
{
}

void os_show_bitmap(int pic, int x, int y)
{
}

int main (int argc, char **argv)
{
  amiga_init ("");
  newgame (argc > 1 ? argv[1] : 0);
  exit (0);
}

wbmain (struct WBStartup *wbmsg)
{
  char startdir[256];
  char *dir;

  strcpy (startdir, "");
  if (Icon = GetDiskObject (wbmsg->sm_ArgList[0].wa_Name))
  {
    if (dir = FindToolType (Icon->do_ToolTypes, "DIR"))
      strcpy (startdir, dir);
  }

  amiga_init (startdir);
  newgame (0);
  exit (0);
}

void amiga_init (char *dir)
{
  if ((DefaultPubScreen = LockPubScreen (0)) == 0)
    exit (1);
  screen_ratio (DefaultPubScreen);

  char prog_name[256];
  static char font_name[MAXFONTPATH];
  char *font_desc, *name_ptr;
  static WORD pens[] =
  {-1};
  static struct TextAttr font =
  {NULL, 0, FS_NORMAL, 0};
  int window = 0;

  if (Icon == NULL)
  {
    if (GetProgramName (prog_name, 256))
      Icon = GetDiskObject (prog_name);
  }

  if (Icon)
  {
    if (FindToolType (Icon->do_ToolTypes, "WINDOW"))
      window = 1;
    if (font_desc = FindToolType (Icon->do_ToolTypes, "FONT"))
    {
      strcpy (font_name, font_desc);
      if (name_ptr = strrchr (font_name, '/'))
      {
	font.ta_Name = font_name;
	font.ta_YSize = atoi (name_ptr + 1);
	strcpy (name_ptr, ".font");
      }
    }
    if (font.ta_Name)
      Font = OpenDiskFont (&font);
  }
  if (Font == NULL)
  {
    font.ta_Name = "topaz.font";
    font.ta_YSize = 8;
    Font = OpenFont (&font);
  }

  if (window == 0)
  {
    if ((Screen = OpenScreenTags (0,
				  SA_Pens, pens,
				  SA_DisplayID, GetVPModeID (&DefaultPubScreen->ViewPort),
				  SA_Overscan, OSCAN_TEXT,
				  SA_Depth, 2,
				  SA_Type, CUSTOMSCREEN | AUTOSCROLL,
				  SA_Font, &font,
				  SA_Title, TitleBar, TAG_DONE)) == 0)
      exit (1);
  }

  if ((Window = OpenWindowTags (0,
				WA_Left, 0,
				WA_Top, Screen ? 2 : DefaultPubScreen->BarHeight + 1,
				WA_Width, Screen ? Screen->Width : ScreenWidth,
				WA_Height, Screen ? Screen->Height - 2 : ScreenHeight - DefaultPubScreen->BarHeight - 1,
				WA_SmartRefresh, 1,
				WA_NewLookMenus, 1,
				WA_AutoAdjust, 1,
				WA_Borderless, Screen ? 1 : 0,
				WA_Backdrop, Screen ? 1 : 0,
				WA_Activate, 1,
				WA_CloseGadget, Screen ? 0 : 1,
				WA_DragBar, Screen ? 0 : 1,
				WA_DepthGadget, Screen ? 0 : 1,
				WA_SizeGadget, Screen ? 0 : 1,
				WA_SizeBBottom, Screen ? 0 : 1,
				WA_Title, TitleBar,
				WA_ScreenTitle, TitleBar,
				WA_IDCMP, IDCMP_RAWKEY | IDCMP_VANILLAKEY | IDCMP_MENUPICK | IDCMP_CLOSEWINDOW | IDCMP_CHANGEWINDOW,
				Screen ? WA_CustomScreen : WA_PubScreen, Screen ? Screen : DefaultPubScreen,
				TAG_DONE)) == 0)
    exit (1);

  ThisProcess = (struct Process *)FindTask(0);
  OldWindowPtr = ThisProcess->pr_WindowPtr;
  ThisProcess->pr_WindowPtr = Window;

  if ((Visual = GetVisualInfo (Window->WScreen, TAG_DONE)) == 0)
    exit (1);
  if ((Menus = CreateMenus (NewMenus, GTMN_NewLookMenus, TRUE, TAG_DONE)) == 0)
    exit (1);
  LayoutMenus (Menus, Visual, GTMN_NewLookMenus, TRUE, TAG_DONE);
  SetMenuStrip (Window, Menus);

  if ((GameReq = alloc_freq (dir)) == 0)
    exit (1);
  if ((SaveReq = alloc_freq (dir)) == 0)
    exit (1);
  if ((ScriptReq = alloc_freq (dir)) == 0)
    exit (1);

  RastPort = Window->RPort;
  SetDrMd (RastPort, JAM2);
  SetAPen (RastPort, 1);
  SetBPen (RastPort, 0);
  SetFont (RastPort, Font);
  DisplayHeight = ((Window->Height - Window->BorderTop - Window->BorderBottom) / RastPort->TxHeight) - 1;
  PreviousHeight = DisplayHeight;

  reset_cursor ();
}

__autoexit void amiga_exit (void)
{
  int i;

  for (i = 0; i < HISTORY_LINES; i++)
  {
    if (*(History + i))
      FreeVec (*(History + i));
    *(History + i) = 0;
  }

  if (GameReq)
    FreeAslRequest (GameReq);
  if (SaveReq)
    FreeAslRequest (SaveReq);
  if (ScriptReq)
    FreeAslRequest (ScriptReq);
  if (Menus)
    FreeMenus (Menus);
  if (Visual)
    FreeVisualInfo (Visual);
  if (ThisProcess)
    ThisProcess->pr_WindowPtr = OldWindowPtr;
  if (Window)
    CloseWindow (Window);
  if (Screen)
    CloseScreen (Screen);
  if (Font)
    CloseFont (Font);
  if (DefaultPubScreen)
    UnlockPubScreen (0, DefaultPubScreen);
  if (Icon)
    FreeDiskObject (Icon);
}

void screen_ratio (struct Screen *screen)
{
  struct TagItem vti[] =
  {VTAG_VIEWPORTEXTRA_GET, 0,
   VTAG_END_CM, 0};
  struct ViewPortExtra *vpe;

  ScreenWidth = screen->Width;
  ScreenHeight = screen->Height;
  if (screen->ViewPort.ColorMap)
  {
    if (VideoControl (screen->ViewPort.ColorMap, vti) == 0)
    {
      vpe = (struct ViewPortExtra *) vti[0].ti_Data;
      ScreenWidth = vpe->DisplayClip.MaxX - vpe->DisplayClip.MinX + 1;
      ScreenHeight = vpe->DisplayClip.MaxY - vpe->DisplayClip.MinY + 1;
    }
  }
}

void reset_cursor (void)
{
  Move (RastPort, Window->BorderLeft, Window->BorderTop + (DisplayHeight * RastPort->TxHeight));
}

void rect (long xMin, long yMin, long xMax, long yMax, unsigned long pen)
{
  unsigned long saved_pen;

  saved_pen = RastPort->FgPen;
  SetAPen (RastPort, pen);
  RectFill (RastPort, xMin, yMin, xMax, yMax);
  SetAPen (RastPort, saved_pen);
}

void text (char *s, int n)
{
  Move (RastPort, RastPort->cp_x, RastPort->cp_y + RastPort->TxBaseline);
  Text (RastPort, s, n);
  Move (RastPort, RastPort->cp_x, RastPort->cp_y - RastPort->TxBaseline);
}

int check_len (char *s, int n)
{
  return TextLength (RastPort, s, n) > Window->Width - Window->BorderRight - RastPort->cp_x;
}

void cursor (int under)
{
  int size;

  os_flush ();
  size = (under == 0) ? RastPort->TxWidth : char_len (under);

  SetDrMd (RastPort, COMPLEMENT);
  rect (RastPort->cp_x, RastPort->cp_y,
     RastPort->cp_x + size - 1, RastPort->cp_y + RastPort->TxHeight - 1, 0);
  SetDrMd (RastPort, JAM2);
}

void set_window (int limit, int x)
{
  if (limit)
  {
    WindowLimits (Window,
		  RastPort->cp_x + RastPort->TxWidth + Window->BorderRight + x,
		  Window->BorderTop + Window->BorderBottom + (RastPort->TxHeight * 3), ~0, ~0);
  }
  else
  {
    WindowLimits (Window, Window->Width, Window->Height, Window->Width,
		  Window->Height);
  }
}

int get_key (UWORD * qualifier_addr, int c, int wait)
{
  struct IntuiMessage *imsg;
  ULONG class;
  UWORD code, qualifier;
  int old_height;

  os_flush ();
  while (1)
  {
    while (imsg = (struct IntuiMessage *) GetMsg (Window->UserPort))
    {
      class = imsg->Class;
      code = imsg->Code;
      qualifier = imsg->Qualifier;
      ReplyMsg ((struct Message *) imsg);
      if (qualifier_addr)
	*qualifier_addr = qualifier;
      switch (class)
      {
      case IDCMP_RAWKEY:
	switch (code)
	{
	case 0x4C:
	  return 129;
	case 0x4D:
	  return 130;
	case 0x4F:
	  return 131;
	case 0x4E:
	  return 132;
	case 0x5F:
	  help ();
	  break;
	}
	break;
      case IDCMP_VANILLAKEY:
	switch (code)
	{
	case 8:
	  return 127;
	case 13:
	  return 13;
	case 127:
	  return 260;
	default:
	  if (code >= 32 && code <= 126)
	    return code;
	  break;
	}
	break;
      case IDCMP_CLOSEWINDOW:
	exit (0);
	break;
      case IDCMP_CHANGEWINDOW:
	old_height = DisplayHeight;
	DisplayHeight = ((Window->Height - Window->BorderTop - Window->BorderBottom) / RastPort->TxHeight) - 1;

	if (PreviousHeight > DisplayHeight)
	{
	  rect (Window->BorderLeft,
		Window->BorderTop + (DisplayHeight * RastPort->TxHeight),
		Window->Width - Window->BorderRight - 1,
		Window->Height - Window->BorderBottom - 1, 0);

	  if (RastPort->cp_y + RastPort->TxHeight > Window->Height - Window->BorderBottom)
	  {
	    Move (RastPort, RastPort->cp_x, Window->BorderTop + (DisplayHeight * RastPort->TxHeight));
	    rect (Window->BorderLeft, RastPort->cp_y,
		  Window->Width - Window->BorderRight - 1,
		  RastPort->cp_y + RastPort->TxHeight - 1, 0);
	    PreviousHeight = DisplayHeight;
	    return 264;
	  }
	}
	PreviousHeight = DisplayHeight;
	break;
      case IDCMP_MENUPICK:
	if (code != MENUNULL)
	{
	  if (MENUNUM (code) == 0)
	  {
	    switch (ITEMNUM (code))
	    {
	    case 0:
	      if (c != -1)
	      {
		cursor (c);
		SetAPen (RastPort, 1);
		int r = newgame (0);
		SetAPen (RastPort, 2);
		cursor (c);
		if (r != 0)
		  return r;
	      }
	      break;
	    case 2:
	      if (c != -1)
		return -2;
	      break;
	    case 3:
	      if (c != -1)
		return -3;
	      break;
	    case 5:
	      help ();
	      break;
	    case 6:
	      about ();
	      break;
	    case 8:
	      exit (0);
	      break;
	    }
	  }
	}
	break;
      }
    }
    if (wait)
      WaitPort (Window->UserPort);
    else
      return 0;
  }
}

void redraw_line (unsigned char *buffer, int *pos, int max_size)
{
  char *current_buffer;

  cursor (*(buffer + *pos));
  if ((current_buffer = AllocVec (max_size, MEMF_CLEAR)) == 0)
    return;
  strcpy (current_buffer, buffer);
  into_buffer (buffer, current_buffer, pos, max_size);
  FreeVec (current_buffer);
}

int char_len (int c)
{
  unsigned char buffer;

  if (c < 0)
    c += 256;
  buffer = (unsigned char) c;
  return TextLength (RastPort, &buffer, 1);
}

void move_text (int offset, int max)
{
  int xSource, xDest;

  xSource = RastPort->cp_x;
  xDest = RastPort->cp_x + offset;
  ClipBlit (RastPort, xSource, RastPort->cp_y,
	    RastPort, xDest, RastPort->cp_y,
	    max - MAX (xSource, xDest), RastPort->TxHeight, 0xC0);
}

void cursor_left (unsigned char *buffer, int *pos)
{
  while (*pos > 0)
  {
    (*pos)--;
    Move (RastPort, RastPort->cp_x - char_len (*(buffer + *pos)), RastPort->cp_y);
  }
}

void cursor_right (unsigned char *buffer, int *pos)
{
  while (*pos < strlen (buffer))
  {
    Move (RastPort, RastPort->cp_x + char_len (*(buffer + *pos)), RastPort->cp_y);
    (*pos)++;
  }
}

int fit_text (unsigned char *pointer, int new)
{
  int a, b;

  a = TextLength (RastPort, pointer, strlen (pointer)) + char_len (new) + RastPort->cp_x + RastPort->TxWidth;
  b = Window->Width - Window->BorderRight;
  return (a > b) ? 0 : 1;
}

void into_buffer (unsigned char *buffer, unsigned char *new, int *pos, int max_size)
{
  if (new == 0)
    return;
  cursor (*(buffer + *pos));

  int saved_x_position;
  int saved_colour;
  int i;

  cursor_left (buffer, pos);
  saved_colour = RastPort->FgPen;
  saved_x_position = RastPort->cp_x;
  SetAPen (RastPort, RastPort->BgPen);
  for (i = 0; i < strlen (buffer); i++)
    os_printchar (*(buffer + i));
  os_flush ();
  SetAPen (RastPort, saved_colour);
  Move (RastPort, saved_x_position, RastPort->cp_y);

  *buffer = 0;
  while (1)
  {
    if ((fit_text (buffer, *(new + *pos)) == 0) || (*(new + *pos) == 0) || (*pos == max_size))
    {
      for (i = 0; i < strlen (buffer); i++)
	os_printchar (*(buffer + i));
      os_flush ();
      cursor (*(buffer + *pos));
      return;
    }
    else
    {
      *(buffer + *pos) = *(new + *pos);
      (*pos)++;
      *(buffer + *pos) = 0;
    }
  }
}

void store_hist (unsigned char *line)
{
  int i;

  if ((*line != 0) && (cmp (*(History + HISTORY_LINES - 1), line) != 0))
  {
    if (*History)
      FreeVec (*History);
    for (i = 0; i < HISTORY_LINES - 1; i++)
      *(History + i) = *(History + i + 1);
    if (*(History + HISTORY_LINES - 1) = AllocVec (strlen (line) + 1, MEMF_CLEAR))
      strcpy (*(History + HISTORY_LINES - 1), line);
  }
  HistoryPosition = HISTORY_LINES;
}

int cmp (const char *a, const char *b)
{
  if ((a == 0) || (b == 0))
    return 1;
  return strcmp (a, b);
}

void help (void)
{
  struct NewAmigaGuide guide =
  {0, "Level9.guide", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

  guide.nag_Screen = Window->WScreen;
  if (AmigaGuideBase == 0)
    AmigaGuideBase = OpenLibrary ("amigaguide.library", 34);
  if (AmigaGuideBase != 0)
    CloseAmigaGuide (OpenAmigaGuide (&guide, TAG_DONE));
}

void about (void)
{
  req ("Level 9 Interpreter v5.1\n"
       "Copyright (c) 1996-2011 Glen Summers and contributors.\n"
       "Contributions from David Kinder, Alan Staniforth,\n"
       "Simon Baldwin, Dieter Baron and Andreas Scherrer.\n\n"
       "Level9 is released under the terms of the GNU General\n"
       "Public License. See the file COPYING that is included\n"
       "with this program for details.","Continue");
}

LONG req (UBYTE * text, UBYTE * gadgets,...)
{
  va_list arguments;
  LONG return_value;
  static struct EasyStruct requester =
  {sizeof (struct EasyStruct), 0, TitleBar, 0, 0};

  requester.es_TextFormat = text;
  requester.es_GadgetFormat = gadgets;
  va_start (arguments, gadgets);
  busy (1);
  return_value = EasyRequestArgs (Window, &requester, 0, arguments);
  busy (0);
  va_end (arguments);
}

void busy (int busy)
{
  if (Window && IntuitionBase->lib_Version >= 39)
    SetWindowPointer (Window, WA_BusyPointer, busy, TAG_DONE);
}

struct FileRequester * alloc_freq (char *initdir)
{
  return AllocAslRequestTags (ASL_FileRequest,
			      strcmp (initdir, "") == 0 ? TAG_IGNORE : ASLFR_InitialDrawer, initdir,
			      ASLFR_SleepWindow, 1,
			      ASLFR_RejectIcons, 1, TAG_DONE);
}

void filereq (struct FileRequester *freq, char *buffer, char *title, ULONG save)
{
  if (AslRequestTags (freq,
		      ASLFR_Window, Window,
		      ASLFR_TitleText, title,
		      ASLFR_DoSaveMode, save, TAG_DONE))
  {
    strcpy (buffer, freq->fr_Drawer);
    AddPart (buffer, freq->fr_File, 256);
  }
  else
    strcpy (buffer, "");
}

int newgame (char *fname)
{
char game[256];
BPTR lock;

  fname ? strcpy (game, fname) :
    filereq (GameReq, game, "Select a Level9 Game", 0);

  if (strcmp (game, "") == 0)
    return 0;

  if (lock = Lock(game,ACCESS_READ))
  {
    NameFromLock(lock,game,256);
    UnLock(lock);
  }

  if (LoadGame (game,NULL))
  {
    if (Playing)
      return -1;
    Playing = TRUE;
    while (RunGame ());
    Playing = FALSE;
  }
  else
    req ("Unable to load game", "Cancel");
  return -1;
}

