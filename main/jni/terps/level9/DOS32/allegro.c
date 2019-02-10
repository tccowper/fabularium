/***********************************************************************\
*
* Level 9 interpreter
* Version 5.1
* Copyright (c) 1996-2011 Glen Summers and contributors.
* Contributions from David Kinder, Alan Staniforth, Simon Baldwin,
* Dieter Baron and Andreas Scherrer.
*
* Level9, 32 bit DOS and Windows Allegro versions.
* Allegro 32 bit interface by David Kinder.
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
* The font data in font.h is generated from default.fnt by the
* command
*
*    bin2h default.fnt raw_font_data font.h
*
\***********************************************************************/

#include <ctype.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#ifdef __DJGPP__
#include <dos.h>
#endif

#include <allegro.h>

#ifdef _WIN32
#include <winalleg.h>
#include <commdlg.h>
#include "resource.h"
#endif

/* Definitions from the interpreter. */
#include "level9.h"

/* Data for the font. */
#include "font.h"

/* Explicitly specify Allegro drivers to reduce executable size. */
#ifdef __DJGPP__
BEGIN_GFX_DRIVER_LIST
  GFX_DRIVER_VESA3
  GFX_DRIVER_VESA2L
  GFX_DRIVER_VESA2B
  GFX_DRIVER_VESA1
END_GFX_DRIVER_LIST

BEGIN_COLOR_DEPTH_LIST
  COLOR_DEPTH_8
  COLOR_DEPTH_16
END_COLOR_DEPTH_LIST

BEGIN_DIGI_DRIVER_LIST
END_DIGI_DRIVER_LIST

BEGIN_MIDI_DRIVER_LIST
END_MIDI_DRIVER_LIST
#endif

#ifdef _MAX_PATH
#define MAX_FILE_NAME _MAX_PATH
#else
#define MAX_FILE_NAME 256
#endif

/* File name buffers */
char story_name[MAX_FILE_NAME];
char save_name[MAX_FILE_NAME];
char gfx_name[MAX_FILE_NAME];
char gfx_dir[MAX_FILE_NAME];
char script_name[MAX_FILE_NAME];

/* Custom font variables */
#define FONT_WIDTH_OFFSET 1536
#define FONT_HEIGHT 16
short* font_data = NULL;
char* font_width = NULL;

/* Buffered output variables */
#define OUTPUT_LIMIT 512
char output_buffer[OUTPUT_LIMIT];
int output_index = 0;

/* Input line variables */
#define DEFAULT_CURSOR_WIDTH 4
#define INPUT_PENDING 0
#define INPUT_LINE 1
#define INPUT_LIMIT 256
#define HISTORY_SIZE 20
char input_buffer[INPUT_LIMIT];
char input_history[HISTORY_SIZE][INPUT_LIMIT];
int history_index = 0;

/* Display bitmap variables */
BITMAP* display = NULL;
PALETTE palette;
int allegro_gfx_mode = GFX_AUTODETECT_FULLSCREEN;
int screen_width = 640;
int screen_height = 480;
int text_x = 0;
int text_y = 0;
int top_y = 0;

/* Paging variables */
int page_counter = 0;
int page_limit = 0;

/* Set this flag to indicate the interpreter should exit. */
volatile int exit_interpreter = FALSE;

/* Graphics variables */
int high_res_pics = 0;
int fast_pics = 0;
int gfx_width = 320;
int gfx_height = 96;
BITMAP* gfx = NULL;
BitmapType bmap_type = NO_BITMAPS;
int last_bitmap = -1;

/* Colours for graphics */
struct RGB colours[8] =
{
  { 0x00,0x00,0x00 },
  { 0x3F,0x00,0x00 },
  { 0x0C,0x39,0x0C },
  { 0x3F,0x3F,0x00 },
  { 0x00,0x00,0x3F },
  { 0x28,0x19,0x00 },
  { 0x00,0x3F,0x3F },
  { 0x3F,0x3F,0x3F }
};

/* Routines specific to this Allegro version of Level9. */

/* Free resources and exit. */
void stop(void)
{
  StopGame();
  FreeMemory();
  exit(0);
}

/* Exit the interpreter with a fatal error. */
void fatal(const char* txt)
{
  allegro_exit();
#ifdef _WIN32
  MessageBox(GetForegroundWindow(),txt,"Fatal Error",MB_OK|MB_ICONERROR); 
#else
  fprintf(stderr,"Fatal Error: %s\n",txt);
#endif
  stop();
}

/* Given fromname, construct filename as having the same path
   and name but with the extension ext. */
void set_filename(char* filename, const char* fromname, const char* ext)
{
  int i, index = -1;

  strcpy(filename,fromname);
  i = strlen(filename)-1;

  /* Search backwards to find either the first period or
     backslash, whichever comes first. */
  while ((i >= 0) && (index < 0))
  {
    switch (filename[i])
    {
    case '.':
      index = i;
      break;
    case '\\':
      i = 0;
      break;
    }
    i--;
  }

  /* If there is no extension on fromname, just add ext
     to filename. */
  if (index < 0)
    index = strlen(filename);
  strcpy(filename+index,ext);
}

/* Check if a file exists. */
int check_for_file(const char* filename)
{
  FILE* f = fopen(filename,"r");
  if (f != NULL)
  {
    fclose(f);
    return TRUE;
  }
  return FALSE;
}

/* Set all file names based on the name of the game file. */
void set_game_file(const char* game_name)
{
  char* p;

  strcpy(story_name,game_name);
  set_filename(save_name,game_name,".sav");

  /* Try the name of the game with ".dat" appended. */
  if (check_for_file(story_name) == FALSE)
    set_filename(story_name,game_name,".dat");

  /* Look for a graphics file. */
  set_filename(gfx_name,game_name,".pic");
  if (check_for_file(gfx_name) == FALSE)
    set_filename(gfx_name,game_name,".cga");
  if (check_for_file(gfx_name) == FALSE)
    set_filename(gfx_name,game_name,".hrc");

  /* Set up the directory for bitmap files. */
  strcpy(gfx_dir,story_name);
  p = strrchr(gfx_dir,'\\');
  if (p != NULL)
    p[1] = '\0';
  else
    strcpy(gfx_dir,".\\");
}

/* Handle command line arguments. These will be either options
   (which start with a dash) or the name of the game to be loaded.
   If this function returns a non-zero value, a game file has been
   taken from the command line. */
int process_args(int argc, char** argv)
{
  int got_story = 0;
  int got_gfx = 0;
  int i = 1;

  while (i < argc)
  {
    /* Is this an option? */
    if (argv[i][0] == '-')
    {
      switch (argv[i][1])
      {
      case 'd':
        if (i < argc+1)
        {
          switch (atoi(argv[i+1]))
          {
          case 1:
            screen_width = 640;
            screen_height = 400;
            break;
          case 2:
            screen_width = 640;
            screen_height = 480;
            break;
          case 3:
            screen_width = 800;
            screen_height = 600;
            break;
          }
          i++;
        }
        break;
      case 'f':
        fast_pics = 1;
        break;
      case 'h':
        high_res_pics = 1;
        break;
      }
    }
    else if (got_story == 0)
    {
      set_game_file(argv[i]);
      got_story = 1;
    }
    else if (got_gfx == 0)
    {
      strcpy(gfx_name,argv[i]);
      got_gfx = 1;
    }
    i++;
  }
  return got_story;
}

/* Set the default palette. */
void set_default_palette(void)
{
  int i;

  get_palette(palette);
  for (i = 0; i < 6; i++)
  {
    palette[0].r = 0x00;
    palette[0].g = 0x00;
    palette[0].b = 0x00;
  }
  palette[1].r = 0x3F;
  palette[1].g = 0x3F;
  palette[1].b = 0x3F;
  set_palette(palette);
}

/* Set up the font data. */
void init_font(void)
{
  /* Set up a pointer to the font_data. */
  font_data = (short*)raw_font_data;

  /* Get a pointer to the table of character widths. */
  font_width = (char*)(font_data+FONT_WIDTH_OFFSET);
}

/* Return the width of a character. */
int char_length(const unsigned char c)
{
  return font_width[c-32];
}

/* Draw a character c at x,y on the display bitmap and
   return the width of that character. */
int draw_char(const unsigned char c, int x, int y, int colour)
{
  char* font_ptr = (char*)(font_data+(FONT_HEIGHT*(c-32)));
  unsigned short value;
  int i, j;

  for (i = 0; i < FONT_HEIGHT; i++)
  {
    /* Read the single plane bitmap for each line. */
    value = *((unsigned short*)font_ptr+i);
    for (j = 0; j < 16; j++)
    {
      if (value & 1)
        putpixel(display,x+16-j,y+i,colour);
      value = value >> 1;
    }
  }
  return char_length(c);
}

/* Draw a string text starting at x,y. */
void draw_string(const unsigned char* text, int x, int y)
{
  int i, l = strlen(text);
  for (i = 0; i < l; i++)
    x += draw_char(text[i],x,y,1);
}

/* Draw the cursor. */
void draw_cursor(int x, int y, int w)
{
  xor_mode(TRUE);
  rectfill(display,x+1,y,x+w,y+FONT_HEIGHT-1,1);
  xor_mode(FALSE);
}

/* Return the total width of a string text */
int string_length(const unsigned char* text)
{
  int i, sl = 0, l = strlen(text);
  for (i = 0; i < l; i++)
    sl += char_length(text[i]);
  return sl;
}

/* Draw the display bitmap onto the visible screen. */
void screen_update(void)
{
  blit(display,screen,0,0,0,0,SCREEN_W,SCREEN_H);
}

/* Wait for the given number of milliseconds. If a picture is
   being drawn, run the appropriate number of picture opcodes.*/
void wait(int millis)
{
  int gfx_status = TRUE;
  int gfx_count = 0;
  int gfx_limit = millis/2;

  /* Run graphics */
  while (gfx_status && (fast_pics || (gfx_count < gfx_limit)))
  {
    gfx_status = RunGraphics();
    if (gfx_status)
      gfx_count++;
  }

  if (gfx_count > 0)
  {
    /* Copy the graphics into the main bitmap. */
    stretch_blit(gfx,display,0,0,gfx_width,gfx_height,0,0,SCREEN_W,top_y);

    /* Set the new palette. */
    set_palette(palette);

    screen_update();
  }

#ifdef _WIN32
  Sleep(millis);
#else
  delay(millis);
#endif
}

/* Wait for a key to be pressed and return it */
int timer_readkey(void)
{
  while (keypressed() == FALSE)
  {
    if (exit_interpreter)
    {
      allegro_exit();
      stop();
    }
    wait(40);
  }
  return readkey();
}

/* Flush buffered output to the display. */
void screen_flush(void)
{
  if (output_index > 0)
  {
    draw_string(output_buffer,text_x,text_y);
    text_x += string_length(output_buffer);

    output_index = 0;

    /* Update the visible screen. */
    screen_update();
  }
}

/* Scroll the display up one line. */
void screen_scroll(int y)
{
  blit(display,display,0,top_y+FONT_HEIGHT,0,top_y,
    SCREEN_W,SCREEN_H-top_y-FONT_HEIGHT);
  rectfill(display,0,y,SCREEN_W-1,SCREEN_H-1,0);
}

extern FILE* scriptfile;

/* Move the current text position down to the start of
   the next line. */
void screen_newline(void)
{
  text_x = 0;
  page_counter++;

  /* If the position is at the bottom of the display,
     scroll the display. */
  if (text_y+(FONT_HEIGHT*2) > SCREEN_H)
  {
    screen_scroll(text_y);

    /* Is there a full page of text on the screen since
       the last pause? If so, put up a [MORE] prompt. */
    if (page_counter >= page_limit)
    {
      if (scriptfile == NULL)
      {
        const char* more = "[MORE]";

        draw_string(more,text_x,text_y);
        draw_cursor(text_x+string_length(more),text_y,DEFAULT_CURSOR_WIDTH);
        screen_update();
        timer_readkey();
        rectfill(display,0,text_y,SCREEN_W-1,SCREEN_H-1,0);
      }

      /* Set the counter to 1 so that the last line of this page
         is the first line of the next. */
      page_counter = 1;
    }
  }
  else
    text_y += FONT_HEIGHT;
  screen_update();
}

/* Check if the word just output runs off the right of the display.
   If so, break at the start of the word and flush the line up to
   the break, then return TRUE. */
int check_break(void)
{
  if (text_x+string_length(output_buffer) > SCREEN_W)
  {
    /* Find the previous space */
    int i = output_index-1;
    while (i > 0)
    {
      if (output_buffer[i] == ' ')
      {
        output_buffer[i] = 0;
        draw_string(output_buffer,text_x,text_y);
        screen_newline();

        memmove(output_buffer,output_buffer+i+1,output_index-i-1);
        output_index -= i+1;
        output_buffer[output_index] = 0;
        break;
      }
      i--;
    }
    return TRUE;
  }
  return FALSE;
}

/* Store a character in the text output buffer. */
void screen_character(char c)
{
  switch (c)
  {
  case '\n':  /* Return or enter */
    check_break();
    screen_flush();
    screen_newline();
    break;
  case '\b':  /* Backspace */
    if (output_index > 0)
      output_buffer[--output_index] = 0;
    break;
  case ' ':
    /* Check if the line should be broken at the previous word. */
    if (check_break() == FALSE)
    {
      if (output_index >= OUTPUT_LIMIT-1)
        screen_flush();
    }
    output_buffer[output_index++] = c;
    output_buffer[output_index] = 0;
    break;
  default:
    if (isprint(c))
    {
      if (output_index >= OUTPUT_LIMIT-1)
        screen_flush();
      output_buffer[output_index++] = c;
      output_buffer[output_index] = 0;
    }
    break;
  }
}

/* Write a string to the output buffer. */
void screen_string(const char* text)
{
  int i, l = strlen(text);
  for (i = 0; i < l; i++)
    screen_character(text[i]);
}

/* Set up the display after the game has been loaded. */
void prepare_screen(void)
{
  int i;

  /* Set up the screen. */
  set_color_depth(8);
  if (set_gfx_mode(allegro_gfx_mode,screen_width,screen_height,0,0) < 0)
    fatal(allegro_error);
  clear(screen);
  set_default_palette();

  /* Allocate a display bitmap. */
  display = create_bitmap(SCREEN_W,SCREEN_H);
  clear(display);

  /* Set picture resolution, if needed. */
  if (high_res_pics)
  {
    gfx_width = SCREEN_W;
    gfx_height = ((SCREEN_H/2) / FONT_HEIGHT) * FONT_HEIGHT;
  }

  /* Initialize the input and input history buffers. */
  input_buffer[0] = 0;
  for (i = 0; i < HISTORY_SIZE; i++)
    input_history[i][0] = 0;

  page_limit = ((SCREEN_H-top_y)/FONT_HEIGHT)-1;
  text_y = page_limit*FONT_HEIGHT;
}

/* Prompt the user for a yes or no response. */
int get_yes_no(const char* prompt)
{
  int key;
  char c;

  /* Display the prompt. */
  screen_string(prompt);
  screen_string("? (y/n) ");
  screen_flush();
  draw_cursor(text_x,text_y,DEFAULT_CURSOR_WIDTH);
  screen_update();

  /* Get a response. */
  key = timer_readkey();
  c = key & 0xFF;

  draw_cursor(text_x,text_y,DEFAULT_CURSOR_WIDTH);
  screen_character(c);
  screen_character('\n');

  return ((c == 'y') || (c == 'Y'));
}

/* Clear the display to the right and below the input prompt. */
void clear_input_display(void)
{
  rectfill(display,text_x,text_y,SCREEN_W-1,text_y+FONT_HEIGHT-1,0);
  if (SCREEN_H > text_y+FONT_HEIGHT)
    rectfill(display,0,text_y+FONT_HEIGHT,SCREEN_W-1,SCREEN_H-1,0);
}

/* Add the input line to the output buffer (called after input
   is complete). */
void write_input(char* input)
{
  int i, l, x, x_step;

  l = strlen(input);
  x = text_x;
  for (i = 0; i < l; i++)
  {
    /* Check if the character will fit on this line. */
    x_step = char_length(input[i]);
    if (x+x_step+DEFAULT_CURSOR_WIDTH >= SCREEN_W)
    {
      screen_character('\n');
      x = 0;
    }
    screen_character(input[i]);
    x += x_step;
  }
}

/* Draw the current input and the cursor. */
void draw_input(char* input, int cursor_index)
{
  int i, l, x, y, x_step, cursor_w;

  /* Clear the previous input. */
  clear_input_display();

  l = strlen(input);
  x = text_x;
  y = text_y;
  for (i = 0; i <= l; i++)
  {
    if (i < l)
    {
      /* Check if the character will fit on this line. */
      x_step = char_length(input[i]);
      if (x+x_step+DEFAULT_CURSOR_WIDTH >= SCREEN_W)
      {
        x = 0;

        /* Move to the next line. */
        if (y+(FONT_HEIGHT*2) > SCREEN_H)
        {
          screen_scroll(y);
          text_y -= FONT_HEIGHT;
        }
        else
          y += FONT_HEIGHT;
      }
      draw_char(input[i],x,y,1);
    }
    else
      x_step = 0;

    /* If this is the right place, draw the cursor. */
    if (i == cursor_index)
    {
      /* Get the cursor width. */
      if (i < l)
        cursor_w = x_step;
      else
        cursor_w = DEFAULT_CURSOR_WIDTH;

      draw_cursor(x,y,cursor_w);
    }
    x += x_step;
  }

  screen_update();
}

/* Formatting routine used when printing out help on in game commands.*/
void help_string(const char* command, const char* info)
{
  screen_string("  #");
  screen_string(command);
  screen_flush();
  text_x = 100;
  screen_string(info);
  screen_character('\n');
}

/* Handle the user pressing the help hot key */
void hotkey_help(void)
{
  screen_string("\nHot key -- Help\n");
  screen_string("\n"
    "Alt-C  show copyright and license info\n"
    "Alt-H  help\n"
    "Alt-X  exit game\n");

  screen_string("\nDuring input, the following commands can be entered:\n");
  help_string("save",
    "Saves position file directly, bypassing any prompts.");
  help_string("restore",
    "Restores position file directly, bypassing any protection code.");
  help_string("quit",
    "Exits the interpreter.");
  help_string("cheat",
    "Tries to bypass restore protection on some games, can be slow.");
  help_string("dictionary",
    "Lists game dictionary, press any key to stop.");
  help_string("picture <n>",
    "Shows picture number <n>.");
}

/* Handle the user pressing the information hot key */
void hotkey_info(void)
{
  screen_string("\nHot key -- Copyright and License Information\n");

  screen_string("\nLevel 9 Interpreter v5.1\n");
  screen_string("Copyright (c) 1996-2011 Glen Summers and contributors.\n");
  screen_string("Contributions from David Kinder, Alan Staniforth, Simon Baldwin,\n");
  screen_string("Dieter Baron and Andreas Scherrer.\n\n");

  screen_string("Level9 is released under the terms of the GNU General Public License.\n");
  screen_string("See the file COPYING that is included with this program for details.\n");
}

/* Handle the user pressing the exit hot key */
void hotkey_exit(void)
{
  screen_string("\nHot key -- Exit game\n");
  if (get_yes_no("Do you wish to quit"))
  {
    allegro_exit();
    stop();
  }
}

/* Test for a user entered hot key. */
void input_hotkeys(char c, char* input, int cursor_index)
{
  /* Is this a recognised hot key? */
  switch (c)
  {
  case KEY_C:
  case KEY_H:
  case KEY_X:
    clear_input_display();
    write_input(input);
    break;
  default:
    return;
  }

  switch (c)
  {
  case KEY_C:  /* Alt-C */
    hotkey_info();
    break;
  case KEY_H:  /* Alt-H */
    hotkey_help();
    break;
  case KEY_X:  /* Alt-X */
    hotkey_exit();
  }

  /* Redisplay the input line. */
  screen_character('\n');
  screen_flush();
  draw_input(input,cursor_index);
  page_counter = 0;
}

/* Get an input line from the history buffer. */
char* get_history(int position)
{
  int index = history_index - position;
  if (index < 0)
    index += HISTORY_SIZE;
  return input_history[index];
}

/* Get a line of input from the user. */
void input_line(char* input, unsigned int input_size, int game_input)
{
  int status, key = 0;
  unsigned int cursor_index = 0;
  int history_position = -1;
  char c, sc;

  screen_flush();
  page_counter = 0;
  cursor_index = strlen(input);
  draw_input(input,cursor_index);

  status = INPUT_PENDING;
  while (status == INPUT_PENDING)
  {
    key = timer_readkey();
    c = key & 0xFF;
    sc = (key & 0xFF00) >> 8;

    switch (c)
    {
    case '\n':  /* Return or enter */
    case '\r':
      status = INPUT_LINE;

      /* Add the input line to the output text. */
      clear_input_display();
      write_input(input);

      /* Store the line in the input history. */
      if (strcmp(input,"") != 0)
      {
        if (strcmp(input_history[history_index],input) != 0)
        {
          history_index++;
          if (history_index >= HISTORY_SIZE)
            history_index = 0;
          strcpy(input_history[history_index],input);
        }
      }
      break;
    case '\b':  /* Backspace */
      if (cursor_index > 0)
      {
        memmove(input+cursor_index-1,input+cursor_index,
          strlen(input)-cursor_index+1);
        cursor_index--;
        draw_input(input,cursor_index);
      }
      break;
    case 0:
      /* If the ASCII code is 0, look at the scan code. */
      switch (sc)
      {
      case KEY_LEFT:
        if (cursor_index > 0)
        {
          cursor_index--;
          draw_input(input,cursor_index);
        }
        break;
      case KEY_RIGHT:
        if (cursor_index < strlen(input))
        {
          cursor_index++;
          draw_input(input,cursor_index);
        }
        break;
      case KEY_HOME:
        if (cursor_index > 0)
        {
          cursor_index = 0;
          draw_input(input,cursor_index);
        }
        break;
      case KEY_END:
        if (cursor_index < strlen(input))
        {
          cursor_index = strlen(input);
          draw_input(input,cursor_index);
        }
        break;
      case KEY_DEL:
        if (cursor_index < strlen(input))
        {
          memmove(input+cursor_index,input+cursor_index+1,
            strlen(input)-cursor_index);
          draw_input(input,cursor_index);
        }
        break;
      case KEY_UP:
        if (game_input)
        {
          /* Are we at the end of the history buffer? */
          if (history_position < HISTORY_SIZE-1)
          {
            /* Is there an entry in the history buffer here? */
            if (strcmp(get_history(history_position+1),"") != 0)
            {
              history_position++;
              strcpy(input,get_history(history_position));

              /* Redraw the input line. */
              cursor_index = strlen(input);
              draw_input(input,cursor_index);
            }
          }
        }
        break;
      case KEY_DOWN:
        if (game_input)
        {
          if (history_position >= 0)
          {
            if (history_position > 0)
            {
              if (strcmp(get_history(history_position-1),"") != 0)
              {
                history_position--;
                strcpy(input,get_history(history_position));
              }
            }
            else
            {
              history_position--;
              strcpy(input,"");
            }
            cursor_index = strlen(input);
            draw_input(input,cursor_index);
          }
        }
        break;
      case KEY_F12:
        screen_update();
        break;
      default:
        if (game_input)
          input_hotkeys(sc,input,cursor_index);
        break;
      }
      break;
    case 1:
      /* If the ASCII code is 1, look at the scan code. */
      switch (sc)
      {
      case KEY_DEL: /* Shift-Delete */
        input[0] = 0;
        cursor_index = 0;
        draw_input(input,cursor_index);
        break;
      }
      break;
    default:
      if (isprint(c) && ((c&0x80) == 0))
      {
        if (strlen(input) < input_size-1)
        {
          memmove(input+cursor_index+1,input+cursor_index,
            strlen(input)-cursor_index+1);
          input[cursor_index++] = c;
          draw_input(input,cursor_index);
        }
      }
      break;
    }
  }
  page_counter = -1;
}

/* Callback called when the close button is pressed. */
void close_button_callback(void)
{
  /* Set a flag to indicate the interpreter should exit. */
  exit_interpreter = TRUE;
}

/* Hook called to handle application switching. */
void display_switch_hook(void)
{
  screen_update();
}

/* Run the interpreter. */
void run_game(void)
{
  /* Set up the font. */
  init_font();

  /* Initialize the Allegro graphics library. */
  install_allegro(SYSTEM_AUTODETECT,&errno,atexit);

  /* Add a callback to handle the close button. */
  set_close_button_callback(close_button_callback);

  /* Add a hook to handle application switching. */
  set_display_switch_callback(SWITCH_IN,display_switch_hook);

  /* Set up the keyboard handler. */
  install_keyboard();

  /* Set the window title. */
  set_window_title("Level 9 Interpreter");

#ifdef _WIN32
  /* Set the window icon. */
  SendMessage(win_get_window(),WM_SETICON,ICON_BIG,
    (LPARAM)LoadIcon(GetModuleHandle(NULL),MAKEINTRESOURCE(IDI_ICON)));
#endif

  /* Set up the screen display. */
  prepare_screen();

  /* Try to load the game into the interpreter. */
  if (LoadGame(story_name,gfx_name) == 0)
  {
    fatal("Failed to start game");
    return;
  }

  /* Run interpreter opcodes until the game exits. */
  while (RunGame() != 0);

  /* Exit from the game. */
  stop();
}

/* Filters for the file dialogs */
const char* game_filter = 
  "Level 9 Game Files (*.dat)\0*.dat\0"
  "Spectrum Snapshots (*.sna)\0*.sna\0"
  "All Files (*.*)\0*.*\0";
const char* save_filter = 
  "Level 9 Saved Games (*.sav)\0*.sav\0"
  "All Files (*.*)\0*.*\0";

#ifdef _WIN32

DWORD display_type = 0;
DWORD display_width = 0;
DWORD display_height = 0;

#define NUMBER_MODES 4

struct DisplayMode
{
  const char* name;
  int width;
  int height;
};

struct DisplayMode display_modes[NUMBER_MODES] =
{
  { "Window",-1,-1 },
  { "Full Screen (640x400)",640,400 },
  { "Full Screen (640x480)",640,480 },
  { "Full Screen (800x600)",800,600 }
};

/* Set the display control. */
void set_display_control(HWND wnd)
{
  HWND control = GetDlgItem(wnd,IDC_DISPLAY);
  if (control)
  {
    int i;

    /* Clear any list items. */
    SendMessage(control,CB_RESETCONTENT,0,0);

    /* Add all display modes. */
    for (i = 0; i < NUMBER_MODES; i++)
      SendMessage(control,CB_ADDSTRING,0,(LPARAM)display_modes[i].name);

    /* Select the current display mode. */
    SendMessage(control,CB_SETCURSEL,display_type,0);
  }
}

/* Set the width and height controls. */
void set_size_controls(HWND wnd, int update)
{
  HWND control;
  char number[256];

  /* Get the display mode control. */
  control = GetDlgItem(wnd,IDC_DISPLAY);
  if (control)
  {
    /* Get the display mode. */
    int display = SendMessage(control,CB_GETCURSEL,0,0);
    if (display >= 0)
    {
      control = GetDlgItem(wnd,IDC_WIDTH);
      if (control)
      {
        /* If the mode's width is -1, use the user entered value. */
        int width = display_modes[display].width;
        int previous = EnableWindow(control,(width < 0));

        /* Always update if going from or to a fixed width mode. */
        if (update || (width >= 0) || previous)
        {
          _snprintf(number,256,"%d",(width < 0) ? display_width : width);
          SendMessage(control,WM_SETTEXT,0,(LPARAM)number);
        }
      }

      control = GetDlgItem(wnd,IDC_HEIGHT);
      if (control)
      {
        /* If the mode's height is -1, use the user entered value. */
        int height = display_modes[display].height;
        int previous = EnableWindow(control,(height < 0));

        /* Always update if going from or to a fixed width mode. */
        if (update || (height >= 0) || previous)
        {
          _snprintf(number,256,"%d",(height < 0) ? display_height : height);
          SendMessage(control,WM_SETTEXT,0,(LPARAM)number);
        }
      }
    }
  }
}

/* Read a value from a control. */
void read_control(HWND wnd, UINT id)
{
  HWND control = GetDlgItem(wnd,id);
  if (control)
  {
    switch (id)
    {
    case IDC_DISPLAY:
      {
        /* Get the currently selected display mode. */
        int display = SendMessage(control,CB_GETCURSEL,0,0);
        if (display >= 0)
          display_type = display;
      }
      break;
    case IDC_WIDTH:
    case IDC_HEIGHT:
      {
        /* Get the text in the control. */
        char number[256];
        SendMessage(control,WM_GETTEXT,256,(LPARAM)number);

        /* Convert the text in the control to a number. */
        switch (id)
        {
        case IDC_WIDTH:
          sscanf(number,"%d",&display_width);
          break;
        case IDC_HEIGHT:
          sscanf(number,"%d",&display_height);
          break;
        }
      }
      break;
    }
  }
}

/* Centre a window in the screen. */
void centre_window(HWND wnd)
{
  RECT wnd_size, screen_size;

  if (GetWindowRect(wnd,&wnd_size))
  {
    /* Get the size of the workspace. */
    if (SystemParametersInfo(SPI_GETWORKAREA,0,&screen_size,0))
    {
      int x = (screen_size.bottom-wnd_size.bottom+wnd_size.top)/2;
      int y = (screen_size.right-wnd_size.right+wnd_size.left)/2;

      /* Resize the window. */
      SetWindowPos(wnd,0,x,y,0,0,
        SWP_NOOWNERZORDER|SWP_NOSIZE|SWP_NOZORDER);
    }
  }
}

/* Hook function to for file dialogs. */
UINT APIENTRY file_dlg_hook(HWND hdlg, UINT uiMsg, WPARAM wParam, LPARAM lParam)
{
  switch (uiMsg)
  {
  case WM_NOTIFY:
    {
      HWND hwnd = GetParent(hdlg);
      if ((hwnd != 0) && (lParam != 0))
      {
        if (((LPNMHDR)lParam)->code == CDN_INITDONE)
        {
          /* Centre the dialog in the screen. */
          centre_window(hwnd);

          /* Set up extra display controls. */
          set_display_control(hdlg);
          set_size_controls(hdlg,TRUE);
        }
      }
    }
    break;
  case WM_COMMAND:
    switch (LOWORD(wParam))
    {
    case IDC_DISPLAY:
      /* Update the width and height controls. */
      set_size_controls(hdlg,FALSE);
      read_control(hdlg,LOWORD(wParam));
      break;
    case IDC_WIDTH:
    case IDC_HEIGHT:
      /* Has the user tabbed away from the control? */
      if (HIWORD(wParam) == EN_KILLFOCUS)
        read_control(hdlg,LOWORD(wParam));
      break;
    }
    break;
  }
  return 0;
}

/* Entry point for the Windows version */
int main(int argc, char** argv)
{
  const char* key_name = "Software\\Level 9\\Graphical Interpreter";
  HKEY registry_key = 0;
  DWORD key_action = 0;

  char initial_dir[MAX_FILE_NAME];
  char game_filename[MAX_FILE_NAME];
  DWORD filter = 0;
  int got_game = 0;

  /* Default width and height. */
  display_width = (GetSystemMetrics(SM_CXFULLSCREEN)*3)/4;
  display_height = (GetSystemMetrics(SM_CYFULLSCREEN)*3)/4;

  /* Open the registry key. */
  RegCreateKeyEx(HKEY_CURRENT_USER,key_name,0,"",REG_OPTION_NON_VOLATILE,
    KEY_ALL_ACCESS,NULL,&registry_key,&key_action);

  /* Set up initial directory and display options. */
  game_filename[0] = '\0';
  initial_dir[0] = '\0';
  if (registry_key != 0)
  {
    DWORD buffer_size = MAX_FILE_NAME;
    DWORD word_size;

    RegQueryValueEx(registry_key,"Directory",NULL,NULL,
      initial_dir,&buffer_size);
    word_size = sizeof(DWORD);
    RegQueryValueEx(registry_key,"Filter",NULL,NULL,
      (LPBYTE)&filter,&word_size);

    word_size = sizeof(DWORD);
    RegQueryValueEx(registry_key,"Display",NULL,NULL,
      (LPBYTE)&display_type,&word_size);
    word_size = sizeof(DWORD);
    RegQueryValueEx(registry_key,"Width",NULL,NULL,
      (LPBYTE)&display_width,&word_size);
    word_size = sizeof(DWORD);
    RegQueryValueEx(registry_key,"Height",NULL,NULL,
      (LPBYTE)&display_height,&word_size);
  }

  /* Check for command line arguments. */
  if (process_args(argc,argv))
    got_game = 1;
  else
  {
    OPENFILENAME open_file;

    /* Set up an open file dialog. */
    ZeroMemory(&open_file,sizeof(OPENFILENAME));
    open_file.lStructSize = sizeof(OPENFILENAME);
    open_file.hInstance = GetModuleHandle(NULL);
    open_file.lpstrFilter = game_filter;
    open_file.nFilterIndex = filter;
    open_file.lpstrFile = game_filename;
    open_file.nMaxFile = MAX_FILE_NAME;
    open_file.lpstrInitialDir = initial_dir;
    open_file.lpstrTitle = "Open a Level 9 Game";
    open_file.Flags = OFN_FILEMUSTEXIST|OFN_HIDEREADONLY|
      OFN_ENABLEHOOK|OFN_ENABLETEMPLATE|OFN_EXPLORER;
    open_file.lpfnHook = file_dlg_hook;
    open_file.lpTemplateName = MAKEINTRESOURCE(IDD_OPENEXTRA);

    /* Get a file name from the user. */
    if (GetOpenFileName(&open_file))
    {
      set_game_file(open_file.lpstrFile);
      filter = open_file.nFilterIndex;
      got_game = 1;

      /* Store the user's settings for next time. */
      if (registry_key != 0)
      {
        char* p;

        /* Remove the file name. */
        p = strrchr(game_filename,'\\');
        if (p != NULL)
          *p = '\0';

        RegSetValueEx(registry_key,"Directory",0,REG_SZ,
          game_filename,strlen(game_filename)+1);
        RegSetValueEx(registry_key,"Filter",0,REG_DWORD,
          (LPBYTE)&filter,sizeof(DWORD));
        RegSetValueEx(registry_key,"Display",0,REG_DWORD,
          (LPBYTE)&display_type,sizeof(DWORD));
        RegSetValueEx(registry_key,"Width",0,REG_DWORD,
          (LPBYTE)&display_width,sizeof(DWORD));
        RegSetValueEx(registry_key,"Height",0,REG_DWORD,
          (LPBYTE)&display_height,sizeof(DWORD));
      }
    }
  }

  if (got_game)
  {
    /* Get the display mode, width and height. */
    if (display_type == 0)
    {
      allegro_gfx_mode = GFX_AUTODETECT_WINDOWED;
      screen_width = display_width;
      screen_height = display_height;
    }
    else
    {
      allegro_gfx_mode = GFX_AUTODETECT_FULLSCREEN;
      screen_width = display_modes[display_type].width;
      screen_height = display_modes[display_type].height;
    }

    /* Run the interpreter. */
    run_game();
  }
  return 0;
}

END_OF_MAIN()

#else

/* Entry point for the interpreter. */
int main(int argc, char** argv)
{
  /* Set up this port of Level9. */
  if (process_args(argc,argv) == FALSE)
  {
    /* Print help information and exit. */
    puts(
      "Level 9 Interpreter v5.1\n"
      "Copyright (c) 1996-2011 Glen Summers and contributors.\n"
      "Contributions from David Kinder, Alan Staniforth, Simon Baldwin,\n"
      "Dieter Baron and Andreas Scherrer.\n"
      "\n"
      "Level9 is released under the terms of the GNU General Public License.\n"
      "See the file COPYING that is included with this program for details.\n"
      "\n"
      "Syntax: level9 [options] story-file [graphics-file]\n\n"
      "        -d #   display mode: 1 is 640x400, 2 is 640x480, 3 is 800x600\n"
      "        -f     draw pictures as fast as possible\n"
      "        -h     use high resolution for pictures, may cause fill errors\n"
      "        story-file is a Level 9 data file, a 48K Spectrum SNA snapshot\n"
      "        or any linear memory dump containing a Level 9 game.");
    exit(0);
  }

  /* Run the interpreter. */
  run_game();
  return 0;
}

#endif

/* Get a file name from the user. The buffer must be of
   length MAX_FILE_NAME. */
int get_file_name(const char* prompt, const char* filter, char* buffer, int save)
{
#ifdef _WIN32
  if (allegro_gfx_mode == GFX_AUTODETECT_WINDOWED)
  {
    OPENFILENAME open_file;
    BOOL got_file = FALSE;
    char filename[MAX_FILE_NAME];

    strcpy(filename,buffer);

    /* Set up a file dialog. */
    ZeroMemory(&open_file,sizeof(OPENFILENAME));
    open_file.lStructSize = sizeof(OPENFILENAME);
    open_file.lpstrFilter =  filter;
    open_file.lpstrFile = filename;
    open_file.nMaxFile = MAX_FILE_NAME;
    open_file.lpstrTitle = prompt;
    open_file.Flags = OFN_HIDEREADONLY|OFN_ENABLEHOOK|OFN_EXPLORER;
    open_file.lpfnHook = file_dlg_hook;

    /* Get a file name from the user. */
    if (save)
      got_file = GetSaveFileName(&open_file);
    else
      got_file = GetOpenFileName(&open_file);

    if (got_file)
    {
      strcpy(buffer,filename);
      return TRUE;
    }
    return FALSE;
  }
#endif

  screen_flush();
  if (text_x > 0)
    screen_character('\n');
  screen_string(prompt);
  screen_string(": ");

  input_line(buffer,MAX_FILE_NAME,FALSE);
  screen_character('\n');
  return TRUE;
}

/* Level9 interface routines. */

/* Load a file into the interpreter's memory. */
L9BOOL os_load_file(L9BYTE* Ptr, int* Bytes, int Max)
{
  FILE *fp;

  if (get_file_name("Game to load",save_filter,save_name,FALSE) == FALSE)
    return FALSE;

  fp = fopen(save_name,"rb");
  if (fp == NULL)
    return FALSE;

  *Bytes = fread(Ptr,1,Max,fp);
  fclose(fp);
  return TRUE;
}

/* Save a file from the interpreter's memory. */
L9BOOL os_save_file(L9BYTE* Ptr, int Bytes)
{
  FILE *fp;

  if (get_file_name("Game to save",save_filter,save_name,TRUE) == FALSE)
    return FALSE;

  fp = fopen(save_name,"wb");
  if (fp == NULL)
    return FALSE;

  fwrite(Ptr,1,Bytes,fp);
  fclose(fp);
  return TRUE;
}

/* Open a file to read commands from. */
FILE* os_open_script_file(void)
{
  FILE *fp;

  if (get_file_name("Script to read",NULL,script_name,FALSE) == FALSE)
    return FALSE;

  return fopen(script_name,"rt");
}

/* Print a character. */
void os_printchar(char c)
{
  if (c == '\r')
    c = '\n';
  screen_character(c);
}

/* Flush any buffered output. */
void os_flush(void)
{
  screen_flush();
}

/* Get a line of input from the user. */
L9BOOL os_input(char* ibuff, int size)
{
  ibuff[0] = '\0';
  while (ibuff[0] == '\0')
    input_line(ibuff,size,TRUE);
  return TRUE;
}

/* Get a character from the user. waiting for at least the
   argument number of millseconds. */
char os_readchar(int millis)
{
  if (exit_interpreter)
  {
    allegro_exit();
    stop();
  }

  /* If no key has been pressed, wait. */
  if (millis > 0)
  {
    if (keypressed() == FALSE)
      wait(millis);
  }

  if (keypressed())
  {
    int key = readkey();

    /* Check for Alt-X or F12 ... */
    if (key == KEY_X << 8)
      hotkey_exit();
    else if (key == KEY_F12 << 8)
      screen_update();

    /* ... otherwise, return the key pressed. */
    return key;
  }
  return 0;
}

/* Check if the listing should be stopped. */
L9BOOL os_stoplist(void)
{
  return (os_readchar(0) != 0);
}

/* Get the next game file name from the user. */
L9BOOL os_get_game_file(char* NewName, int Size)
{
  if (get_file_name("Load next game",game_filter,story_name,FALSE) == FALSE)
    return FALSE;

  if ((int)strlen(story_name) < Size)
  {
    strcpy(NewName,story_name);
    return TRUE;
  }
  return FALSE;
}

/* Work out the file name of a game part. */
void os_set_filenumber(char* NewName, int Size, int n)
{
  char *p;
  int i;

  p = strrchr(NewName,'\\');
  if (p == NULL)
    p = NewName;
  for (i = strlen(p)-1; i >= 0; i--)
  {
    if (isdigit(p[i]))
    {
      p[i] = '0'+n;
      return;
    }
  }
}

/* Set the graphics mode. */
void os_graphics(int mode)
{
  bmap_type = NO_BITMAPS;
  last_bitmap = -1;

  switch (mode)
  {
  case 0:
    top_y = 0;
    break;
  case 1:
    /* Get the height of the picture in terms of text
       lines, and use it to set the size of the text area. */
    top_y = ((SCREEN_H/2) / FONT_HEIGHT) * FONT_HEIGHT;
    break;
  case 2:
    bmap_type = DetectBitmaps(gfx_dir);
    if (bmap_type != NO_BITMAPS)
      top_y = ((SCREEN_H/2) / FONT_HEIGHT) * FONT_HEIGHT;
    else
      top_y = 0;
    break;
  }

  page_limit = ((SCREEN_H-top_y)/FONT_HEIGHT)-1;
}

/* Clear the graphics window. */
void os_cleargraphics(void)
{
  if (top_y > 0)
  {
    /* If needed, allocate the graphics bitmap. */
    if (gfx == NULL)
    {
      /* Get the picture size from the interpreter. */
      if (high_res_pics == 0)
        GetPictureSize(&gfx_width,&gfx_height);
      gfx = create_bitmap(gfx_width,gfx_height);
    }

    /* Clear the graphics bitmap */
    rectfill(gfx,0,0,gfx_width-1,gfx_height-1,2);
  }
}

/* Set a colour from the 8 colour palette. */
void os_setcolour(int colour, int index)
{
  palette[colour+2] = colours[index];
}

/* Callback to draw lines. */
void line_callback(BITMAP* bmp, int x, int y, int d)
{
  int colour1 = ((d >> 2) & 3) + 2;
  int colour2 = (d & 3) + 2;

  int pixel = getpixel(bmp,x,y);
  if (pixel == colour2)
  {
    /* getpixel() returns -1 for pixels outside the bitmap,
       so the much faster _putpixel() can be used. */
    _putpixel(bmp,x,y,colour1);
  }
}

/* Draw a line on the graphics window. */
void os_drawline(int x1, int y1, int x2, int y2, int colour1, int colour2)
{
  if (top_y > 0)
  {
    if (high_res_pics)
    {
      int pic_width, pic_height;
      GetPictureSize(&pic_width,&pic_height);

      x1 *= ((double)gfx_width) / ((double)pic_width);
      y1 *= ((double)gfx_height) / ((double)pic_height);
      x2 *= ((double)gfx_width) / ((double)pic_width);
      y2 *= ((double)gfx_height) / ((double)pic_height);
    }

    /* Draw the line. */
    do_line(gfx,(int)x1,(int)y1,(int)x2,(int)y2,
      (colour1<<2)+colour2,line_callback);
  }
}

/* Fill the graphics window from the given point. */
void os_fill(int x, int y, int colour1, int colour2)
{
  if (top_y > 0)
  {
    int pixel;

    if (high_res_pics)
    {
      int pic_width, pic_height;
      GetPictureSize(&pic_width,&pic_height);

      x *= ((double)gfx_width) / ((double)pic_width);
      y *= ((double)gfx_height) / ((double)pic_height);
    }

    /* Flood fill the graphics window. */
    pixel = getpixel(gfx,(int)x,(int)y);
    if (pixel == colour2+2)
      floodfill(gfx,(int)x,(int)y,colour1+2);
  }
}

/* Show a bitmap picture */
void os_show_bitmap(int picnum, int x, int y)
{
  if (last_bitmap == picnum)
    return;

  if (top_y > 0)
  {
    Bitmap* bitmap = DecodeBitmap(gfx_dir,bmap_type,picnum,x,y);
    if (bitmap != NULL)
    {
      BITMAP* pic;
      int i, x, y;

      pic = create_bitmap(bitmap->width,bitmap->height);
      for (y = 0; y < bitmap->height; y++)
      {
        for (x = 0; x < bitmap->width; x++)
          _putpixel(pic,x,y,bitmap->bitmap[(y*bitmap->width)+x]+2);
      }
      stretch_blit(pic,display,0,0,bitmap->width,bitmap->height,0,0,SCREEN_W,top_y);
      destroy_bitmap(pic);

      for (i = 0; i < bitmap->npalette; i++)
      {
        palette[i+2].r = bitmap->palette[i].red >> 2;
        palette[i+2].g = bitmap->palette[i].green >> 2;
        palette[i+2].b = bitmap->palette[i].blue >> 2;
      }
      set_palette(palette);

      /* Show the new bitmap. */
      screen_update();

      if (picnum == 0)
      {
#ifdef _WIN32
        Sleep(1000);
#else
        delay(1000);
#endif
      }
    }
  }
}

