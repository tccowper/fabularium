/***********************************************************************\
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
*
* Magnetic v2.3, 32 bit DOS Allegro version.
*
* Allegro 32 bit DOS DJGPP interface by David Kinder, based in
* part on Stefan Jokisch's 16 bit DOS interface.
*
\***********************************************************************/

#include <ctype.h>
#include <stdio.h>
#include <string.h>
#include <time.h>

#include <allegro.h>
#include <png.h>

/* Definitions from the interpreter. */
#include "defs.h"

/* Explicitly specify Allegro drivers to reduce executable size. */
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

/* File name buffers */
#define MAX_FILE_NAME 256
char story_name[MAX_FILE_NAME];
char gfx_name[MAX_FILE_NAME];
char pic_name[MAX_FILE_NAME];
char font_name[MAX_FILE_NAME];
char hint_name[MAX_FILE_NAME];
char save_name[MAX_FILE_NAME];
char script_name[MAX_FILE_NAME];
char record_name[MAX_FILE_NAME];

/* Custom font variables */
#define FONT_DATA_SIZE 3168
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
#define INPUT_UNDO 2
#define INPUT_LIMIT 256
#define HISTORY_SIZE 20
char input_buffer[INPUT_LIMIT];
int input_index = 0;
char input_history[HISTORY_SIZE][INPUT_LIMIT];
int history_index = 0;

/* Display bitmap variables */
BITMAP* display = NULL;
PALETTE palette;
int screen_width;
int screen_height;
int status_bar = 0;
int status_x = 0;
int text_x = 0;
int text_y = 0;
int top_y = 0;

/* Paging variables */
int page_counter = 0;
int page_limit = 0;

/* Structure for an animation frame. */
struct frame
{
  int x, y;
  BITMAP* bitmap;
};

/* Graphics variables */
#define ANIM_TIME_STEP (CLOCKS_PER_SEC/10)
#define MAX_FRAMES 20
BITMAP* picture = NULL;
BITMAP* pic_display = NULL;
struct frame frames[MAX_FRAMES];
int frame_count = 0;
int picture_map[16];
int animation = FALSE;
int wait_for_anim = FALSE;

/* Scripting variables. */
int script_status = FALSE;
int script_index = 0;
int script_x = 0;
int script_width;
char script_buffer[OUTPUT_LIMIT];
FILE* script_file;

/* Recording and playback variables. */
#define REC_NONE 0
#define REC_RECORD 1
#define REC_PLAYBACK 2
int record_status = REC_NONE;
FILE* record_file;

/* Routines specific to this Allegro version of Magnetic. */

/* Required prototypes */
int input_line(char* input, int input_size, int game_input);
void stop_script(void);
void stop_record(void);
void hotkey_seed(void);

/* Free resources and exit. */
void stop(void)
{
  ms_freemem();
  stop_script();
  stop_record();
  exit(0);
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

/* Handle command line arguments. These will be either options
   (which start with a dash) or the name of the game to be loaded. */
void process_args(int argc, char** argv)
{
  int got_story = 0;
  int got_gfx = 0;
  int got_hint = 0;
  int i = 1;

  /* Option defaults. */
  screen_width = 640;
  screen_height = 480;
  strcpy(font_name,"default.fnt");
  script_width = 76;
  wait_for_anim = FALSE;

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
      case 'g':
        if (i < argc+1)
        {
          set_filename(font_name,argv[i+1],".fnt");
          i++;
        }
        break;
      case 's':
        if (i < argc+1)
        {
          int w = atoi(argv[i+1]);
          if (w >= 40)
            script_width = w;
          i++;
        }
        break;
      case 'w':
        wait_for_anim = TRUE;
        break;
      }
    }
    else if (got_story == 0)
    {
      set_filename(story_name,argv[i],".mag");
      set_filename(gfx_name,argv[i],".gfx");
      set_filename(pic_name,argv[i],".png");
      set_filename(hint_name,argv[i],".hnt");
      set_filename(save_name,argv[i],".sav");
      set_filename(script_name,argv[i],".scr");
      set_filename(record_name,argv[i],".rec");
      got_story = 1;
    }
    else if (got_gfx == 0)
    {
      set_filename(gfx_name,argv[i],".gfx");
      set_filename(pic_name,argv[i],".png");
      set_filename(hint_name,argv[i],".hnt");
      got_gfx = 1;
    }
    else if (got_hint == 0)
    {
      set_filename(hint_name,argv[i],".hnt");
      got_hint = 1;
    }
    i++;
  }

  /* Is there a story file? If not, print help
     information and exit. */
  if (got_story == 0)
  {
    puts(
      "Magnetic v2.3, an interpreter for Magnetic Scrolls games.\n"
      "Copyright (C) Niclas Karlsson 1997-2008.\n"
      "Written by Niclas Karlsson, David Kinder, Stefan Meier and Paul David Doherty.\n\n"
      "Syntax: magnetic [options] story-file [graphics-file] [hint-file]\n\n"
      "        -d #     display mode: 1 is 640x400, 2 is 640x480, 3 is 800x600\n"
      "        -g font  text font\n"
      "        -s #     transcript width\n"
      "        -w       playback waits for animations\n\n"
      "Defaults are: -d 2 -g default -s 76");
    exit(0);
  }
}

/* Set the default palette. */
void set_default_palette(void)
{
  get_palette(palette);
  palette[0].r = 0x00;
  palette[0].g = 0x00;
  palette[0].b = 0x00;
  palette[1].r = 0xFF;
  palette[1].g = 0xFF;
  palette[1].b = 0xFF;
  set_palette(palette);
}

/* Set up the font data. */
void init_font(void)
{
  FILE* font_fp = fopen(font_name,"rb");
  if (font_fp == NULL)
    ms_fatal("Cannot load font data");

  /* Allocate a buffer and read in the data. */
  font_data = malloc(FONT_DATA_SIZE);
  fread(font_data,FONT_DATA_SIZE,1,font_fp);
  fclose(font_fp);

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

/* Set the visible screen's palette. */
void screen_palette(unsigned short* new_palette, int size)
{
  int i, max = 0, max_prod = 0, fg = 0;

  /* Copy the palette. */
  for (i = 0; i < 16; i++)
  {
    palette[i+2].r = (new_palette[i]>>8)&7;
    palette[i+2].g = (new_palette[i]>>4)&7;
    palette[i+2].b = (new_palette[i]>>0)&7;

    palette[i+2].r = (63*palette[i+2].r+4)/7;
    palette[i+2].g = (63*palette[i+2].g+4)/7;
    palette[i+2].b = (63*palette[i+2].b+4)/7;
  }

  /* Find the brightest colour and use it for the text. */
  for (i = 0; i < 16; i++)
  {
    int sum = palette[i+2].r + palette[i+2].g + palette[i+2].b;
    int prod = palette[i+2].r * palette[i+2].g * palette[i+2].b;
    if ((sum > max) || ((sum == max) && (prod >= max_prod)))
    {
      max = sum;
      max_prod = prod;
      fg = i+2;
    }
    picture_map[i] = i+2;
  }
  palette[1].r = palette[fg].r;
  palette[1].g = palette[fg].g;
  palette[1].b = palette[fg].b;

  set_palette(palette);
}

/* Clear or redraw the current picture on the display. */
void draw_picture(int draw)
{
  int i, y = 0, h = 0;

  if (status_bar)
    y += FONT_HEIGHT;

  if (picture != NULL)
  {
    /* Get the height of the picture in terms of text
       lines, and use it to set the size of the text area. */
    h = (picture->h+1) / FONT_HEIGHT;
    if (((picture->h+1) % FONT_HEIGHT) > 0)
      h++;
  }
  top_y = y+(h*FONT_HEIGHT);
  page_limit = ((SCREEN_H-top_y)/FONT_HEIGHT)-1;

  if (picture != NULL)
  {
    /* Clear the top section of the display. */
    int x = (SCREEN_W-picture->w)/2;
    rectfill(display,0,y,SCREEN_W-1,top_y-1,0);
    if (draw)
    {
      /* Draw the main picture. */
      blit(picture,pic_display,0,0,0,0,picture->w,picture->h);

      /* Draw frames of animation. */
      for (i = 0; i < frame_count; i++)
      {
        if (frames[i].bitmap != NULL)
        {
          masked_blit(frames[i].bitmap,pic_display,
            0,0,frames[i].x,frames[i].y,
            frames[i].bitmap->w,frames[i].bitmap->h);
        }
      }

      /* Draw into the display bitmap. */
      blit(pic_display,display,0,0,x,y+1,pic_display->w,pic_display->h);
    }
  }
}

/* Decode the interpreter format bitmap. */
void decode_picture(BITMAP** ptr, unsigned char* data, unsigned char* mask, int width, int height)
{
  int x, y;
  *ptr = create_bitmap(width,height);

  if (mask != NULL)
  {
    int mod = width % 16;
    int mw = width + ((mod > 0) ? (16 - mod) : 0);
    int mbw = mw / 8;

    for (y = 0; y < height; y++)
    {
      for (x = 0; x < width; x++)
      {
        /* Test if each pixel should be drawn through the mask. */
        if ((mask[(y*mbw)+(x/8)] & (128 >> (x%8))) == 0)
          putpixel(*ptr,x,y,picture_map[data[(y*width)+x] & 0x0F]);
        else
          putpixel(*ptr,x,y,0);
      }
    }
  }
  else
  {
    for (y = 0; y < height; y++)
    {
      for (x = 0; x < width; x++)
        putpixel(*ptr,x,y,picture_map[data[(y*width)+x] & 0x0F]);
    }
  }
}

/* Set the picture bitmap. Note that picture_map must have
   previously been initialized by a call to screen_palette(). */
void set_picture(unsigned char* pic_data, int width, int height)
{
  /* Free any previous bitmap and allocate a new one. */
  if (picture != NULL)
  {
    draw_picture(FALSE);
    destroy_bitmap(picture);
    destroy_bitmap(pic_display);
    picture = NULL;
    pic_display = NULL;
  }

  if (pic_data != NULL)
  {
    decode_picture(&picture,pic_data,NULL,width,height);
    pic_display = create_bitmap(width,height);
  }
}

/* Clear the current animation frames. */
void clear_animation(void)
{
  int i;

  for (i = 0; i < frame_count; i++)
    destroy_bitmap(frames[i].bitmap);
  frame_count = 0;
}

/* Advance animation one frame. */
void screen_animate(void)
{
  if (animation)
  {
    struct ms_position* positions;
    unsigned short count;
    int i;

    /* Get the new animation frames. */
    if (ms_animate(&positions,&count) == 0)
    {
      animation = FALSE;
      return;
    }

    if (count > MAX_FRAMES)
      return;
    clear_animation();

    /* Decode each frame. */
    for (i = 0; i < count; i++)
    {
      unsigned char* anim_data;
      unsigned char* mask_data;
      unsigned short width, height;

      frames[i].x = positions[i].x;
      frames[i].y = positions[i].y;
      anim_data = ms_get_anim_frame(positions[i].number,&width,&height,&mask_data);
      if (anim_data != NULL)
        decode_picture(&(frames[i].bitmap),anim_data,mask_data,width,height);
      else
        frames[i].bitmap = NULL;
    }
    frame_count = count;
  }
}

/* Wait for a key to be pressed and return it */
int timer_readkey(void)
{
  clock_t clk = clock();
  while (keypressed() == FALSE)
  {
    if (clock() > clk+ANIM_TIME_STEP)
    {
      screen_animate();
      draw_picture(TRUE);
      screen_update();
      clk = clock();
    }
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
      /* Only show a [MORE] prompt if command playback is
         not active. */
      if (record_status != REC_PLAYBACK)
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

/* Write a character to the status line. */
void status_character(char c)
{
  switch (c)
  {
  /* 0x09 indicates that what follows is to go on the
     right of the status line, i.e. score and moves. */
  case 0x09:
    status_x = (int)(SCREEN_W*0.75);
    break;
  /* 0x0A indicates that what follows is to go on the
     left of the status line, i.e. current location. */
  case 0x0A:
    status_x = (int)(SCREEN_W*0.05);
    break;
  default:
    if (isprint(c))
    {
      /* If this is the first character on the left of
         the status bar, clear the status bar before
         drawing the character. */
      if (status_x == (int)(SCREEN_W*0.05))
        rectfill(display,0,0,SCREEN_W-1,FONT_HEIGHT-1,1);
      status_x += draw_char(c,status_x,0,0);
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
void prepare_screen(int init_code)
{
  int i;

  /* Set up the screen. */
  set_color_depth(8);
  if (set_gfx_mode(GFX_AUTODETECT,screen_width,screen_height,0,0) < 0)
    ms_fatal(allegro_error);
  clear(screen);
  set_default_palette();

  /* Allocate a display bitmap. */
  display = create_bitmap(SCREEN_W,SCREEN_H);
  clear(display);

  /* Initialize the input and input history buffers. */
  input_buffer[0] = 0;
  for (i = 0; i < HISTORY_SIZE; i++)
    input_history[i][0] = 0;

  /* Does this game have a status bar? */
  if (ms_is_magwin() == 0)
  {
    status_bar = TRUE;
    top_y = FONT_HEIGHT;
    status_character(0x0A);
  }
  text_y = top_y;
  page_limit = ((SCREEN_H-top_y)/FONT_HEIGHT)-1;

  /* The non-MagneticWindows game start with graphics,
     so if the interpreter graphics have initialized
     correctly, make enough space available. */
  if ((ms_is_magwin() == 0) && (init_code == 2))
    text_y += FONT_HEIGHT*10;
}

/* Prompt the user for a yes or no response. */
int get_yes_no(const char* prompt)
{
  int key;
  char c;

  /* Display the prompt. */
  screen_string(prompt);
  screen_string("? (y/n) >");
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

/* Get a line of text from the input playback file. */
int rec_input_line(char* input, int input_size)
{
  int status = INPUT_PENDING;

  /* Wait for animation to complete? */
  if (record_status == REC_PLAYBACK)
  {
    if (wait_for_anim)
    {
      clock_t clk = clock();
      while (animation)
      {
        if (clock() > clk+ANIM_TIME_STEP)
        {
          screen_animate();
          draw_picture(TRUE);
          screen_update();
          clk = clock();
        }
      }
    }

    /* Get a line from the file and remove the final '\n'. */
    if (fgets(input,input_size,record_file) != NULL)
    {
      int l = strlen(input);
      if (l > 0)
      {
        if (input[l-1] == '\n')
          input[l-1] = 0;
      }

      /* Is this a special command? */
      if (input[0] == '#')
      {
        clear_input_display();

        if (stricmp(input,"#seed") == 0)
          hotkey_seed();

        input[0] = 0;
      }
      status = INPUT_LINE;
    }
    else
    {
      input[0] = 0;
      stop_record();
    }
  }
  return status;
}

/* Start recording or playback. */
void start_record(int action)
{
  if (record_status == REC_NONE)
  {
    switch (action)
    {
    case REC_RECORD:
      record_file = fopen(record_name,"wt");
      if (record_file)
        record_status = REC_RECORD;
      break;
    case REC_PLAYBACK:
      record_file = fopen(record_name,"rt");
      if (record_file)
        record_status = REC_PLAYBACK;
      break;
    }
  }
}

/* Stop recording or playback. */
void stop_record(void)
{
  if (record_status != REC_NONE)
  {
    fclose(record_file);
    record_status = REC_NONE;
  }
}

/* Flush buffered script output to the script file. */
void script_flush(void)
{
  if (script_status && (script_index > 0))
  {
    fputs(script_buffer,script_file);
    script_x += strlen(script_buffer);

    script_index = 0;
  }
}

/* Check if the word just output to the script buffer should be
   broken at the start of the word. */
int script_check_break(void)
{
  if (script_status)
  {
    if (script_x+strlen(script_buffer) > script_width)
    {
      /* Find the previous space */
      int i = script_index-1;
      while (i > 0)
      {
        if (script_buffer[i] == ' ')
        {
          script_buffer[i] = 0;
          fputs(script_buffer,script_file);
          fputc('\n',script_file);
          script_x = 0;

          memmove(script_buffer,script_buffer+i+1,script_index-i-1);
          script_index -= i+1;
          script_buffer[script_index] = 0;
          break;
        }
        i--;
      }
      return TRUE;
    }
  }
  return FALSE;
}

/* Output a character to the scripting file. */
void script_character(char c)
{
  if (script_status)
  {
    switch (c)
    {
    case '\n':  /* Return or enter */
      script_check_break();
      script_flush();
      fputc('\n',script_file);
      script_x = 0;
      break;
    case '\b':  /* Backspace */
      if (script_index > 0)
        script_buffer[--script_index] = 0;
      break;
    case ' ':
      /* Check if the line should be broken at the previous word. */
      if (script_check_break() == FALSE)
      {
        if (script_index >= OUTPUT_LIMIT-1)
          script_flush();
      }
      script_buffer[script_index++] = c;
      script_buffer[script_index] = 0;
      break;
    default:
      if (isprint(c))
      {
        if (script_index >= OUTPUT_LIMIT-1)
          script_flush();
        script_buffer[script_index++] = c;
        script_buffer[script_index] = 0;
      }
      break;
    }
  }
}

/* Output a string to the scripting file. */
void script_string(const char* text)
{
  if (script_status)
  {
    int i, l = strlen(text);
    for (i = 0; i < l; i++)
      script_character(text[i]);
  }
}

/* Add the input line to the script buffer. */
void script_write_input(char* input)
{
  if (script_status)
  {
    int i, l, x;

    script_flush();
    l = strlen(input);
    x = script_x;
    for (i = 0; i < l; i++)
    {
      /* Check if the character will fit on this line. */
      if (x >= script_width)
      {
        script_character('\n');
        x = 0;
      }
      script_character(input[i]);
      x++;
    }
  }
}

/* Start scripting. */
void start_script(void)
{
  if (script_status == FALSE)
  {
    script_file = fopen(script_name,"wt");
    if (script_file)
    {
      script_status = TRUE;
      script_string("*** start of transcription ***\n");
    }
  }
}

/* Stop scripting. */
void stop_script(void)
{
  if (script_status)
  {
    script_string("\n*** end of transcription ***\n");
    script_flush();
    fclose(script_file);
    script_status = FALSE;
  }
}

/* Add the input line to the input record file. */
void record_write_input(char* input)
{
  if (record_status == REC_RECORD)
    fprintf(record_file,"%s\n",input);
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

/* Handle the user pressing the help hot key */
void hotkey_help(void)
{
  screen_string("\nHot key -- Help\n");
  screen_string ("\n"
    "Alt-C  show copyright and license info\n"
    "Alt-D  output screen dump\n"
    "Alt-H  help\n"
    "Alt-P  playback on\n"
    "Alt-R  recording on/off\n"
    "Alt-S  seed random numbers\n"
    "Alt-T  transcription on/off\n"
    "Alt-U  undo last turn\n"
    "Alt-X  exit game\n");
}

/* Handle the user pressing the information hot key */
void hotkey_info(void)
{
  screen_string("\nHot key -- Copyright and License Information\n");
  screen_string("\nMagnetic v2.3, an interpreter for Magnetic Scrolls games.\n");
  screen_string("Copyright (C) 1997-2008 Niclas Karlsson.\n\n");

  screen_string("Magnetic is released under the terms of the GNU General Public License.\n");
  screen_string("See the file COPYING that is included with this program for details.\n\n");

  screen_string("Magnetic was written by Niclas Karlsson, David Kinder, Stefan Meier and ");
  screen_string("Paul David Doherty. This Allegro version was written by David Kinder, ");
  screen_string("based in part on Stefan Jokisch's 16-bit DOS port.\n");
}

/* Handle the user pressing the playback hot key */
void hotkey_playback(char* input, int input_size, int* status)
{
  switch (record_status)
  {
  case REC_RECORD:
    screen_string("\nRecording is currently active\n");
    break;
  case REC_PLAYBACK:
    screen_string("\nInput playback is already active\n");
    break;
  default:
    screen_string("\nHot key -- Playback on\n");
    screen_string("Enter filename: ");
    if (input_line(record_name,MAX_FILE_NAME,FALSE) == INPUT_LINE)
    {
      start_record(REC_PLAYBACK);
      if (record_status == REC_PLAYBACK)
      {
        screen_character('\n');
        *status = rec_input_line(input,input_size);
      }
      else
        screen_string("\nCannot open file\n");
    }
    break;
  }
}

/* Handle the user pressing the record hot key */
void hotkey_record(void)
{
  switch (record_status)
  {
  case REC_RECORD:
    screen_string("\nHot key -- Recording off\n");
    stop_record();
    break;
  case REC_PLAYBACK:
    screen_string("\nInput playback is currently active\n");
    break;
  default:
    screen_string("\nHot key -- Recording on\n");
    screen_string("Enter filename: ");
    if (input_line(record_name,MAX_FILE_NAME,FALSE) == INPUT_LINE)
    {
      start_record(REC_RECORD);
      if (record_status == REC_RECORD)
        screen_character('\n');
      else
        screen_string("\nCannot open file\n");
    }
    break;
  }
}

/* Handle the user pressing the script hot key */
void hotkey_script(void)
{
  /* If scripting is off, start scripting. */
  if (script_status == FALSE)
  {
    screen_string("\nHot key -- Transcription on\n");
    screen_string("Enter filename: ");
    if (input_line(script_name,MAX_FILE_NAME,FALSE) == INPUT_LINE)
    {
      start_script();
      if (script_status)
      {
        script_character('>');
        screen_character('\n');
      }
      else
        screen_string("\nCannot open file\n");
    }
  }
  else
  {
    /* Turn scripting off. */
    stop_script();
    screen_string("\nHot key -- Transcription off\n");
  }
}

/* Handle the user pressing the random number seed hot key. */
void hotkey_seed()
{
  char input[32];

  screen_string("\nHot key -- Seed random numbers\n");
  screen_string("Enter seed value (or return to randomize): ");

  input[0] = 0;
  if (input_line(input,32,FALSE) == INPUT_LINE)
  {
    if (strcmp(input,"") != 0)
      ms_seed(atoi(input));
    else
      ms_seed(time(NULL));
  }
  screen_character('\n');
}

/* Handle the user pressing the undo hot key by setting the
   input status flag to INPUT_UNDO. */
void hotkey_undo(char* input, int* status)
{
  screen_string("\nHot key -- Undo turn");
  *status = INPUT_UNDO;
  input[0] = 0;
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

/* Handle the user pressing the screen dump hotkey. */
void hotkey_screen()
{
  char* bmp_name = "Magnetic.bmp";

  screen_string("\nHot key -- Output screen dump\n");

  if (save_bitmap(bmp_name,display,palette) == 0)
    screen_string("Screen dump saved to \"");
  else
    screen_string("Failed to save screen to \"");
  screen_string(bmp_name);
  screen_string("\".\n");
}

/* Test for a user entered hot key. */
void input_hotkeys(char c, char* input, int input_size, int cursor_index, int* status)
{
  /* Is this a recognised hot key? */
  switch (c)
  {
  case KEY_C:
  case KEY_D:
  case KEY_H:
  case KEY_P:
  case KEY_R:
  case KEY_S:
  case KEY_T:
  case KEY_U:
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
  case KEY_D:  /* Alt-D */
    hotkey_screen();
    break;
  case KEY_H:  /* Alt-H */
    hotkey_help();
    break;
  case KEY_P:  /* Alt-P */
    hotkey_playback(input,input_size,status);
    break;
  case KEY_R:  /* Alt-R */
    hotkey_record();
    break;
  case KEY_S:  /* Alt-P */
    hotkey_seed();
    break;
  case KEY_T:  /* Alt-T */
    hotkey_script();
    break;
  case KEY_U:  /* Alt-U */
    hotkey_undo(input,status);

    /* Undo ends the current input, so the routine
       exits here to avoid redrawing the input line. */
    return;
  case KEY_X:  /* Alt-X */
    hotkey_exit();
  }

  /* Redisplay the input line. */
  screen_string("\n>");
  screen_flush();
  page_counter = 0;

  /* Remove input line is playback has become active. */
  if ((c == KEY_P) && (*status == INPUT_LINE))
  {
    clear_input_display();
    write_input(input);
    script_write_input(input);
  }
  else
    draw_input(input,cursor_index);
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
int input_line(char* input, int input_size, int game_input)
{
  int status, key;
  int cursor_index = 0;
  int history_position = -1;
  char c, sc;

  screen_flush();
  page_counter = 0;
  cursor_index = strlen(input);
  draw_input(input,cursor_index);

  /* If input playback is active, get a line from the file. */
  status = rec_input_line(input,input_size);
  if (status == INPUT_LINE)
  {
    clear_input_display();
    write_input(input);
    if (game_input)
      script_write_input(input);
  }

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
      if (game_input)
      {
        script_write_input(input);
        record_write_input(input);
      }

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
      default:
        if (game_input)
          input_hotkeys(sc,input,input_size,cursor_index,&status);
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
      if (isprint(c))
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
  return status;
}

/* Display the title picture. */
void title(void)
{
  FILE* fp;
  unsigned char header[8];
  png_structp png_ptr;
  png_infop info_ptr;
  png_infop end_info;
  png_uint_32 width, height;
  int bit_depth, color_type, i, x, y, w, h;
  double aspect_ratio;
  BITMAP* bitmap;
  unsigned char* pixels;
  png_bytep* row_ptrs;

  /* Initialize the PNG library and get information on the title picture. */
  fp = fopen(pic_name,"rb");
  if (!fp)
    return;

  fread(header,1,8,fp);
  if (!png_check_sig(header,8))
  {
    fclose(fp);
    return;
  }

  png_ptr = png_create_read_struct(PNG_LIBPNG_VER_STRING,(png_voidp)NULL,NULL,NULL);
  if (!png_ptr)
  {
    fclose(fp);
    return;
  }

  info_ptr = png_create_info_struct(png_ptr);
  if (!info_ptr)
  {
    png_destroy_read_struct(&png_ptr,(png_infopp)NULL,(png_infopp)NULL);
    fclose(fp);
    return;
  }

  end_info = png_create_info_struct(png_ptr);
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

  width = png_get_image_width(png_ptr,info_ptr);
  height = png_get_image_height(png_ptr,info_ptr);
  bit_depth = png_get_bit_depth(png_ptr,info_ptr);
  color_type = png_get_color_type(png_ptr,info_ptr);
  aspect_ratio = png_get_pixel_aspect_ratio(png_ptr,info_ptr);

  if ((bit_depth != 8) || (color_type != PNG_COLOR_TYPE_RGB))
  {
    png_destroy_read_struct(&png_ptr,&info_ptr,&end_info);
    fclose(fp);
    return;
  }

  if (png_get_valid(png_ptr,info_ptr,PNG_INFO_tRNS))
    png_set_expand(png_ptr);

  /* Set up the display. */
  set_color_depth(16);
  if (set_gfx_mode(GFX_AUTODETECT,screen_width,screen_height,0,0) < 0)
  {
    png_destroy_read_struct(&png_ptr,&info_ptr,&end_info);
    fclose(fp);
    return;
  }
  clear(screen);

  /* Allocate a bitmap */
  bitmap = create_bitmap(width,height);

  /* Set up buffers for decoding. */
  pixels = malloc(width*height*3);
  row_ptrs = malloc(sizeof(png_bytep)*height);
  for (i = 0; i < height; i++)
    row_ptrs[i] = pixels+(width*i*3);

  /* Decode the picture. */
  png_read_image(png_ptr,row_ptrs);
  png_read_end(png_ptr,end_info);
  png_destroy_read_struct(&png_ptr,&info_ptr,&end_info);
  fclose(fp);

  for (y = 0; y < height; y++)
  {
    for (x = 0; x < width; x++)
    {
      putpixel(bitmap,x,y,makecol16(
        row_ptrs[y][(x*3)+0],
        row_ptrs[y][(x*3)+1],
        row_ptrs[y][(x*3)+2]));
    }
  }

  /* Resize the picture. */
  w = width * aspect_ratio;
  h = height;
  if ((aspect_ratio == 1.0) && (w < 640))
  {
    w *= 2;
    h *= 2;
  }

  /* Display the picture. */
  x = (SCREEN_W-w)/2;
  y = (SCREEN_H-h)/2;
  stretch_blit(bitmap,screen,0,0,width,height,x,y,w,h);

  readkey();

  /* Free buffers. */
  free(row_ptrs);
  free(pixels);
  destroy_bitmap(bitmap);
}

int main(int argc, char** argv)
{
  int init_code = 0;

  /* Set up this port of Magnetic. */
  process_args(argc,argv);

  /* Set up the font. */
  init_font();

  /* Initialize the Allegro graphics library. */
  allegro_init();

  /* Set up the keyboard handler. */
  install_keyboard();

  /* Call the interpreter's setup function. */
  init_code = ms_init(story_name,gfx_name,hint_name,NULL);
  if (init_code == 0)
    ms_fatal("Failed to start game");

  /* Show the title screen. */
  title();

  /* Run interpreter opcodes until the game exits. */
  prepare_screen(init_code);
  while (ms_rungame() != 0);

  allegro_exit();
  stop();
  return 0;
}

/* Magnetic interface routines. */

type8 ms_load_file(type8s* name, type8* ptr, type16 size)
{
  FILE *fp;
  int result;

  if (name == NULL)
  {
    screen_string("Please enter filename: ");
    if (input_line(save_name,MAX_FILE_NAME,FALSE) != INPUT_LINE)
      return 1;
    screen_character('\n');
  }
  else
    strcpy(save_name,name);

  fp = fopen(save_name,"rb");
  if (fp == NULL)
    return 1;

  result = fread(ptr,size,1,fp);
  fclose (fp);
  return (result != 0) ? 0 : 1;
}

type8 ms_save_file(type8s* name, type8* ptr, type16 size)
{
  FILE *fp;
  int result;

  if (name == NULL)
  {
    screen_string("Please enter filename: ");
    if (input_line(save_name,MAX_FILE_NAME,FALSE) != INPUT_LINE)
      return 1;
    screen_character('\n');
  }
  else
    strcpy(save_name,name);

  fp = fopen(save_name,"wb");
  if (fp == NULL)
    return 1;

  result = fwrite(ptr,size,1,fp);
  fclose (fp);
  return (result != 0) ? 0 : 1;
}

void ms_statuschar(type8 c)
{
  if (status_bar)
    status_character(c);
}

void ms_putchar(type8 c)
{
  screen_character(c);
  script_character(c);
}

void ms_flush(void)
{
  screen_flush();
  script_flush();
}

type8 ms_getchar(type8 trans)
{
  char c = 0;

  /* Call input_line() if there is no text in the input buffer. */
  if (input_buffer[0] == 0)
  {
    if (input_line(input_buffer,INPUT_LIMIT,trans ? TRUE : FALSE) == INPUT_UNDO)
      return 0;
    input_index = 0;
  }

  /* Return the next character in the input buffer. */
  c = input_buffer[input_index++];
  if (c == 0)
  {
    c = '\n';
    input_buffer[0] = 0;
  }
  return c;
}

void ms_showpic(type32 c, type8 mode)
{
  clear_animation();
  switch (mode)
  {
  case 0:
    set_default_palette();
    set_picture(NULL,0,0);
    animation = FALSE;
    break;
  case 1:
  case 2:
    {
      type8* picture = NULL;
      type16 width, height;
      type16 pic_palette[16];
      type8 is_anim = 0;

      picture = ms_extract(c,&width,&height,pic_palette,&is_anim);
      if (picture != NULL)
      {
        screen_palette(pic_palette,16);
        set_picture(picture,width,height);

        animation = is_anim ? TRUE : FALSE;
        if (animation)
          screen_animate();
      }
    }
    break;
  }
  draw_picture(TRUE);
}

type8 ms_showhints(struct ms_hint* hints)
{
  return 0;
}

void ms_playmusic(type8 * midi_data, type32 length, type16 tempo)
{
}

void ms_fatal(type8s* txt)
{
  allegro_exit();
  fprintf(stderr,"Fatal error: %s\n",txt);
  stop();
}

