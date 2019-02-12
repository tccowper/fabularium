/*
 * ncurses-based Unix interface for Level-9 interpreter
 *
 * Copyright (c) Jim Cameron 1998
 */

/*
 * 9-Jun-1998 fixed bug in os_load_file
 *
 * A few notes on this port:
 *
 * I've modified level9.h to use the LITTLEENDIAN macro -- it assumes this if
 *  you're using DOS or Windows, otherwise it uses the byte sex macros. This
 *  shouldn't hurt a little-endian machine (Intel), but you must undefine
 *  LITTLEENDIAN if you've got a Motorola-type machine.
 *
 * If you don't specify a path, level9 Linux will look first in the directory
 *  $LEVEL9DIR, if defined, and then in the current directory.
 *
 * The line justify routine could do with a little work -- it looks a bit ugly
 *  on screen (the code looks a bit ugly as well, to be honest). How do you
 *  write text-mode justification?
 *
 * 21-Apr-2002, simonb@caldera.com - Updates for v3.0 interpreter:
 *
 *   FILE_DELIM defined locally
 *   Curses halfdelay in os_readchar()
 *   Os_stoplist()
 *   NULL second arg on LoadGame
 *   Stub graphics functions
 *   Strnicmp definition
 */

/*
 * Here are the defines which you might want to play with
 */
#define JUSTIFY      1
#define FILE_DELIM   '/'

/*
 * Set this if you are compiling on a little-endian machine (ARM, Intel)
 */
#define LITTLEENDIAN 1

/*
 * Define this as 1 to get the Emacs-type key bindings
 *  Ctrl-A (go to beginning of line)
 *  Ctrl-B (back one character)
 *  Ctrl-D (delete character at cursor)
 *  Ctrl-E (go to end of line)
 *  Ctrl-F (forward one character)
 *  Ctrl-K (delete to end of line)
 */
#define EMACS_TYPE_KEYS 1

/*
 * This will make justified text look nicer in some circumstances, but is
 *  slower and a bit of a yucky kludge. Don't use it if not justifying
 */
#define REPRINT_FLUSHED_TEXT 1


/*
 * #includes
 */
#include <ctype.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/*
 * Some C libraries define these, so remove any definition
 */
#ifdef stricmp
#undef stricmp
#endif
#ifdef strnicmp
#undef strnicmp
#endif

/*
 * Include definitions and prototypes for the interpreter
 */
#include "level9.h"
extern FILE* scriptfile;

/*
 * You might have to change this if not using ncurses
 */
#include <ncurses/curses.h>


#define CTRL_A '\x01'
#define CTRL_B '\x02'
#define CTRL_D '\x04'
#define CTRL_E '\x05'
#define CTRL_F '\x06'
#define CTRL_H '\x08'
#define CTRL_K '\x0B'


int   Line_width;
char *Line_buffer;
int   Line_ptr = 0;
int   Lines    = 0;
int   More_lines;

/*
 * This is used by the justification routine to hold the part of the input that
 *  wouldn't fit on the line
 */
char *Rump;

/*
 * See discussion with os_flush()
 */
#if !REPRINT_FLUSHED_TEXT
int  Line_pos = 0;
#endif


/*
 * Justification kludge (see end of printline() )
 */
L9BOOL Suppress_newline = FALSE;


/*
 * History!
 */
#define HISTSIZE 32
#define MAX_COMMAND_LENGTH 128

char  History_list [HISTSIZE] [MAX_COMMAND_LENGTH];
int   History_start;
int   History_end;

#define NEXT_HISTORY(h) { ++(h); if ((h) >= HISTSIZE) (h) = 0; }
#define PREV_HISTORY(h) { if ((h) == 0) (h) = HISTSIZE; --(h); }



/*
 * Local function prototypes
 */
void printline (void);
static L9BOOL input_i (char *ibuff, int size, L9BOOL suppress_history);


/*
 * From porting.txt :
        os_printchar() prints a character to the output. The interface
        can either buffer this character or print it immediately, but
        if buffering is used then the characters must all be sent to the
        output when the interpreter calls os_flush(). A paragraph of
        text is output as one long stream of characters, without line
        breaks, so the interface must provide its own word wrapping, and
        any other features that are desired, such as justification or a
        [More] prompt. The carriage return character is always '\r',
        rather than '\n'.
 * Got that? Good
 */
void os_printchar(char c)
{

  if (c == '\r')
  {

    /*
     * Newline
     */
    if (Line_ptr != 0)
    {
      Suppress_newline = FALSE;
    }

    os_flush ();

    /*
     * If the last line of the previous paragraph exactly filled the screen,
     *  we now have a blank line and should not print a newline
     */
    if (!Suppress_newline)
    {
      addch ('\n');
      refresh ();
      ++Lines;
    }
    else
    {
      Suppress_newline = FALSE;
    }

    Line_ptr = 0;

#   if !REPRINT_FLUSHED_TEXT
    Line_pos = 0;
#   endif

  }
  else
  {
    Line_buffer [Line_ptr] = c;

    ++Line_ptr;

#   if REPRINT_FLUSHED_TEXT
    if (Line_ptr >= Line_width)
    {
      printline ();
    }
#   else
    ++Line_pos;
    if (Line_pos >= Line_width)
    {
      Line_buffer [Line_ptr] = 0;
      printline ();
    }
#   endif

  }
  
  if ((scriptfile == NULL) && (Lines == More_lines))
  {
    printw ("[More]");
    refresh ();
    getch ();
    printw ("\r      \r");
    refresh ();
    Lines = 0;
  }

}


/*
 * From porting.txt :
        os_input() reads a line of text from the user, usually to accept
        the next command to be sent to the game. The text input must be
        stored in ibuff with a terminating zero, and be no longer than
        size characters. Normally os_input() should return TRUE, but may
        return FALSE to cause the entire input so far to be discarded.
        The reason for doing so is discussed in the section at the end
        on allowing the interpreter to load a new game without exiting.
 *
 * The command-line history behaviour mirrors that of bash(1); if you edit a
 *  line and then move to another point in the history the current line will
 *  be saved in that history slot.
 *
 * Command-line editing works the way you'd expect (subject to $TERM's being
 *  set correctly and your termcap file's being kosher, of course). The input
 *  cursor takes up all the buffer space which doesn't actually contain
 *  characters; characters to the right of the cursor are stored at the very
 *  end of the input buffer. left_pos is the next position where a character
 *  is about to be stored, and right_pos points at the character under the
 *  cursor. This makes insertion much easier. (I got this trick from
 *  disassembling an old word-processor on the BBC micro).
 *
 * No, I don't have tab completion. Feel free to add it in ... 8-)
 */
L9BOOL os_input (char *ibuff, int size)
{

  return input_i (ibuff, size, FALSE);

}


/*
 * The 'i' stands for internal
 * The suppress_history parameter is used by the save/load filename input
 *  routines to allow command-line editing without history
 */
L9BOOL input_i (char *ibuff, int size, L9BOOL suppress_history)
{

  int left_pos = 0;
  int right_pos = size - 1;
  int got;
  int x, y;

  int history_current = History_end;

  /*
   * A macro for fetching a history list entry
   */
# define FETCH_HISTORY                              \
  getyx (stdscr, y, x);                             \
  move (y, x - left_pos);                           \
  clrtoeol ();                                      \
  strcpy (ibuff, History_list [history_current]);   \
  left_pos = strlen (ibuff);                        \
  right_pos = size - 1;                             \
  printw ("%s", ibuff)

  ibuff [right_pos] = '\0';

  /*
   * Player's got a chance to read all the text now
   */
  Lines = 0;

  /*
   * We are going to print a newline, but not through the normal channels.
   *  Make sure there's no dangling justification stuff
   */
# if REPRINT_FLUSHED_TEXT
  Line_ptr = 0;
# else
  Line_pos = 0;
# endif

  if (size > MAX_COMMAND_LENGTH)
  {
    size = MAX_COMMAND_LENGTH;
  }

  /*
   * Clear the current entry in the history list
   */
  if (!suppress_history)
  {
    History_list [History_end] [0] = '\0';
  }

  while (1)
  {

    got = getch ();
    switch (got)
    {

    case '\r':
    case '\n':
    case KEY_ENTER:

      strcpy (ibuff + left_pos, ibuff + right_pos);
      if ((strlen (ibuff) > 0) && !suppress_history)
      {
	strcpy (History_list [History_end], ibuff);
	NEXT_HISTORY (History_end);
	if (History_end == History_start)
	{
	  NEXT_HISTORY (History_start);
	}
      }
      getyx (stdscr, y, x);
      move (y, x - left_pos);
      printw ("%s\n", ibuff);
      refresh ();
      return TRUE;

    case CTRL_H:
    case KEY_BACKSPACE:
      if (left_pos == 0)
      {
	beep ();
      }
      else
      {
	--left_pos;
	/*
	 * CHANGEME This won't work when the cursor is at the beginning of a
	 *  line
	 */
	getyx (stdscr, y, x);
	mvdelch (y, x - 1);
      }

      break;

      /*
       * Delete character under cursor
       */
#   if EMACS_TYPE_KEYS
    case CTRL_D:
#   endif
    case KEY_DC:
      if (ibuff [right_pos] == '\0')
      {
	beep ();
      }
      else
      {
	++right_pos;
	delch ();
      }
      break;

    case KEY_UP:
      if ((history_current == History_start) || suppress_history)
      {
	beep ();
      }
      else
      {
	strcpy (ibuff + left_pos, ibuff + right_pos);
	strcpy (History_list [history_current], ibuff);
	PREV_HISTORY (history_current);
	FETCH_HISTORY;
      }
      break;

    case KEY_DOWN:
      if ((history_current == History_end) || suppress_history)
      {
	beep ();
      }
      else
      {
	strcpy (ibuff + left_pos, ibuff + right_pos);
	strcpy (History_list [history_current], ibuff);
	NEXT_HISTORY (history_current);
	FETCH_HISTORY;
      }
      break;

#   if EMACS_TYPE_KEYS
    case CTRL_B:
#   endif
    case KEY_LEFT:
      if (left_pos == 0)
      {
	beep ();
      }
      else
      {
	addch ('\x08');
	--left_pos;
	--right_pos;
	ibuff [right_pos] = ibuff [left_pos];
      }
      break;

#   if EMACS_TYPE_KEYS
    case CTRL_F:
#   endif
    case KEY_RIGHT:
      if (ibuff [right_pos] == '\0')
      {
	beep ();
      }
      else
      {
	ibuff [left_pos] = ibuff [right_pos];
	addch (ibuff [left_pos]);
	++right_pos;
	++left_pos;
      }
      break;

      /*
       * Move to beginning of line
       */
#   if EMACS_TYPE_KEYS
    case CTRL_A:
#   endif
    case KEY_HOME:
    case KEY_A1:
      while (left_pos > 0)
      {
	addch ('\x08');
	--left_pos;
	--right_pos;
	ibuff [right_pos] = ibuff [left_pos];
      }
      break;

      /*
       * Move to end of line
       */
#   if EMACS_TYPE_KEYS
    case CTRL_E:
#   endif
    case KEY_END:
    case KEY_C1:
      while (ibuff [right_pos] != '\0')
      {
	ibuff [left_pos] = ibuff [right_pos];
	addch (ibuff [left_pos]);
	++right_pos;
	++left_pos;
      }
      break;

      /*
       * Delete to the end of the line
       */
#   if EMACS_TYPE_KEYS
    case CTRL_K:
#   endif
    case KEY_EOL:
      right_pos = size - 1;
      clrtoeol ();
      break;

      /*
       * Misc. key
       */
    default:

      if ((left_pos >= right_pos) || (!isprint (got)))
      {
#       if DEBUG
	printw ("%d\n", got);
#       endif
	beep ();
      }
      else
      {
	ibuff [left_pos] = got;
	++left_pos;
	insch (got);
	addch (got);
      }

      break;

    }

    refresh ();

  }

# undef FETCH_HISTORY

  return FALSE;


}

char os_readchar(int millis)
{

  int c;

  /* Set curses delay in 1/10 secs, with a lower limit of 1/10 sec */
  halfdelay ((millis / 100) > 0 ? millis / 100 : 1);
  c = getch ();
  cbreak ();

  if (c == ERR)
  {
    return 0;
  }

  /*
   * This is a hack for multiple-choice games (Adrian Mole) ... if you've had
   *  time to hit a key you've had time to read what's on the screen, or if
   *  not, it's your lookout
   */
  Lines = 0;

  return c;

}

L9BOOL os_stoplist(void)
{
  int c;

  nodelay (stdscr, TRUE);
  c = getch ();
  nodelay (stdscr, FALSE);

  return (c != ERR);
}

/*
 * From porting.txt :
	If the calls to os_printchar() are being buffered by the
	interface then the buffered text must be printed when os_flush()
	is called.
 *
 * jim sez:
 *  Some games (Adrian Mole) have a habit of flushing in the middle of a line
 *   (I don't recommend this; you'll get a wet bum) which confuses the
 *   justification. I'm getting round this problem by:
 *  If REPRINT_FLUSHED_TEXT is defined as true, leaving the flushed characters
 *   in the buffer and printing a carriage return at the beginning of all
 *   lines. Thus printing of more text will cause the original text to be
 *   overwritten with the justified version. This will look nicer, but cause
 *   hackers to go Ur, Yuk.
 *  If REPRINT_FLUSHED_TEXT is undefined or false, keeping track of the
 *   current column, leaving printed text on the screen and justifying the
 *   rest of the line as best I can. This might cause strange effects if there
 *   is very little room left at the end of the line.
 *  If you don't use justification, don't reprint flushed text either. There's
 *   no point.
 */
void os_flush(void)
{

# if REPRINT_FLUSHED_TEXT
  int x, y;
  getyx (stdscr, y, x);
  move (y, 0);
# else
  Line_ptr = 0;
# endif

  Line_buffer [Line_ptr] = '\0';
  addstr (Line_buffer);
  refresh ();

}



/*
 * From porting.txt :
	os_save_file() should prompt the user in some way (with either
	text or a file requester) for a filename to save the area of
	memory of size Bytes pointed to by Ptr. TRUE or FALSE should be
	returned depending on whether or not the operation was successful.
 */
L9BOOL os_save_file(L9BYTE * Ptr, int Bytes)
{

  char fname [256];
  FILE *outfile;

  os_flush();
  printw("\nSave file: ");
  refresh ();
  input_i (fname, 256, TRUE);
  
  outfile = fopen (fname, "w");
  if (outfile == NULL)
  {
    return FALSE;
  }

  fwrite (Ptr, 1, Bytes, outfile);
  fclose (outfile);

  return TRUE;

}



/*
 * From porting.txt :
	os_load_file() should prompt the user for the name of a file to
	load. At most Max bytes should be loaded into the memory pointed
	to by Ptr, and the number of bytes read should be placed into the
	variable pointed to by Bytes.
 */
L9BOOL os_load_file(L9BYTE *Ptr,int *Bytes,int Max)
{

  char fname [256];
  FILE *infile;

  os_flush();
  printw("\nLoad file: ");
  refresh ();
  input_i (fname, 256, TRUE);

  infile = fopen (fname, "r");
  if (infile == NULL)
  {
    return FALSE;
  }

  *Bytes = fread (Ptr, 1, Max, infile);
  fclose (infile);
  return TRUE;

}




/*
 * From porting.txt :
	os_load_file() should prompt the user for the name of a file to
	load. At most Max bytes should be loaded into the memory pointed
	to by Ptr, and the number of bytes read should be placed into the
	variable pointed to by Bytes.
 */
FILE* os_open_script_file(void)
{

  char fname [256];

  os_flush();
  printw("\nPlay script: ");
  refresh ();
  input_i (fname, 256, TRUE);

  return fopen (fname, "rt");

}




/*
 * From porting.txt :
	os_get_game_file() should prompt the user for a new game file, to
	be stored in NewName, which can take a maximum name of Size
	characters. This is used in the Adrian Mole games (and possibly
	others) which load in the next part of the game after the part
	currently being played has completed. These games were originally
	written for tape-based systems where the call was simply "load
	the next game from the tape".
 */
L9BOOL os_get_game_file(char *NewName,int Size)
{

  os_flush();
  addstr ("Load next game: ");
  refresh ();
  return input_i (NewName, Size, TRUE);
  return TRUE;

}




/*
 * From porting.txt :
	os_set_filename() is for multi-part games originally written for
	disk-based systems, which used game filenames such as

		gamedat1.dat	gamedat2.dat

	etc. The routine should take the full filename in NewName (of
	maximum size Size) and modify it to reflect the number n, e.g.
	os_set_filename("gamedat1.dat",2) should leave "gamedat2.dat"
	in NewName.
 */
void os_set_filenumber(char *NewName,int Size,int n)
{

  char *leafname;
  int i;

  leafname = strrchr (NewName, FILE_DELIM);
  if (leafname == NULL)
  {
    leafname = NewName;
  }

  for (i = strlen (leafname) - 1; i >= 0; i--)
  {
    if (isdigit (leafname [i]))
    {
      leafname [i] = '0' + n;
      break;
    }
  }

}



/*
 * From porting.txt :
	You must provide your own main() entry point for the program.
	The simplest such main() is given in generic.c, which just calls
	LoadGame() and then sits in a loop calling RunGame(). These
	functions are discussed below.
 */
int main (int argc, char *argv [])
{

  int i;
  char gamename [256];
  char *envbuf;
  L9BOOL gotgame;

  /*
   * Check byte sex
   */
# if LITTLEENDIAN
  L9UINT32 test = 0x12345678;
  char *tcp = (char *) &test;
# else
  L9UINT32 test = 0x78563412;
  char *tcp = (char *) &test;
# endif
  if ((tcp [0] != 0x78) || (tcp [1] != 0x56) ||
      (tcp [2] != 0x34) || (tcp [3] != 0x12))
  {
    fprintf (stderr, "%s: compiled with the wrong byte sex!\n"
	             " Check the LITTLEENDIAN macro in os/unix-curses.c\n",
	             argv [0]);
    exit (1);
  }

  if (argc != 2)
  {
    fprintf (stderr, "Syntax: %s <gamefile>\n",argv[0]);
    exit (1);
  }

  printf ("Level 9 Interpreter v5.1\n"
	  "Copyright (c) 1996-2011 Glen Summers and contributors.\n"
	  "Contributions from David Kinder, Alan Staniforth, Simon Baldwin,\n"
	  "Dieter Baron and Andreas Scherrer.\n"
	  "Unix Curses interface by Jim Cameron.\n");

  /*
   * Initialise the curses library---these are the recommended options from
   *  ncurses (3X) on my Linux box
   */
  initscr ();
  cbreak ();
  noecho ();
  nonl   ();
  intrflush (stdscr, FALSE);
  keypad    (stdscr, TRUE);
  scrollok  (stdscr, TRUE);

  /*
   * Get the terminal characteristics for line width and More prompt
   */
  Line_width  = tgetnum ("co");
  if (Line_width == ERR)
  {
    fprintf (stderr, "Couldn't get terminal width---falling back on good old"
	             " 80 columns\n\n");
    Line_width = 80;
  }

  More_lines = tgetnum ("li");
  if (More_lines == ERR)
  {
    fprintf (stderr, "Couldn't get terminal height---guessing\n\n");
    More_lines = 24;
  }
  More_lines--;

  Line_buffer = malloc (2 * (Line_width + 1));
  if (Line_buffer == NULL)
  {
    endwin ();
    fprintf (stderr, "Couldn't allocate buffer for pretty-print\n\n");
    exit (1);
  }
  Rump        = Line_buffer + Line_width + 1;


  /*
   * Make sure the line buffer is terminated
   */
  Line_buffer [Line_width] = '\0';

  gotgame = FALSE;
  if (strchr (argv [1], '/') == NULL)
  {
    /*
     * Look first in $LEVEL9DIR, if it is defined
     */
    envbuf = getenv ("LEVEL9DIR");
    if (envbuf != NULL)
    {
      sprintf (gamename, "%s/%s", envbuf, argv [1]);
      gotgame = LoadGame (gamename, NULL);
    }
  }

  if (!gotgame)
  {
    if (!LoadGame (argv [1], NULL))
    {
      endwin ();
      fprintf(stderr, "%s: couldn't open Level9 file %s\n",
	      argv [0], argv [1]);
      exit (1);
    }
  }

  /*
   * Main game loop!
   */
  while (1)
  {
    if (!RunGame ())
    {
      break;
    }
  }

  StopGame();
  FreeMemory();

  free (Line_buffer);

  /*
   * Stop ncursing!
   */
  endwin ();

  printf ("Thanks for playing Level9 ...\n\n");

  return 0;

}




/*
 * Print the text in the line buffer up to the word in progress, justifying it
 *  if wanted
 */
void printline (void)
{

  char *spaceptr;
  char *hyphenptr;

# if JUSTIFY
  int dead_space;
  int num_spaces;
  int scount = 0;
  char *stringptr;
# endif

# if REPRINT_FLUSHED_TEXT
  int x, y;
# endif

  /*
   * ... otherwise ANY full-length line in a paragraph will cause the end-of-
   *  paragraph newline to be suppressed (oops!)
   */
  Suppress_newline = FALSE;

  ++Lines;

# if REPRINT_FLUSHED_TEXT
  getyx (stdscr, y, x);
  move (y, 0);
# endif

  spaceptr  = strrchr (Line_buffer, ' ');
  hyphenptr = strrchr (Line_buffer, '-');
  if ((spaceptr == NULL) && (hyphenptr == NULL))
  {

    /*
     * Buffer contains no spaces - just print the whole line
     */
    printw ("%s\n", Line_buffer);
    Line_ptr = 0;
    refresh ();
    return;

  }

  if (spaceptr > hyphenptr)
  {
    *spaceptr = '\0';
    strcpy (Rump, spaceptr + 1);
  }
  else
  {
    strcpy (Rump, hyphenptr + 1);
    hyphenptr [1] = '\0';
  }

# if JUSTIFY

  /*
   * This isn't a very sophisticated justify routine. In particular, leading
   *  spaces at the beginning of lines will cause curious results
   * It looks a little ugly. I should like to give priority when adding spaces
   *  to the positions following punctuation
   * Count the spaces in the line
   */
  dead_space = Line_width - strlen (Line_buffer);
  num_spaces = -1;
  spaceptr = Line_buffer;
  while (spaceptr != NULL)
  {
    spaceptr = strchr (spaceptr + 1, ' ');
    ++num_spaces;
  }

  if (num_spaces > 0)
  {
    stringptr = Line_buffer;
    while (*stringptr != '\0')
    {
      addch (*stringptr);
      if (*stringptr == ' ')
      {
	scount += dead_space;
	while (scount > num_spaces)
	{
	  scount -= num_spaces;
	  addch (' ');
	}
      }
      ++stringptr;
    }
  }
  else
  {
    addstr (Line_buffer);
  }

  addch ('\n');

# else

  printw ("%s\n", Line_buffer);

# endif /* JUSTIFY */

  strcpy (Line_buffer, Rump);
  Line_ptr = strlen (Line_buffer);

# if !REPRINT_FLUSHED_TEXT
  Line_pos = Line_ptr;
# endif

  /*
   * If the last line of a paragraph exactly fills the screen, we will get an
   *  extra newline when attempting to flush the blank `next' line. Suppress
   *  this
   */
  if (Line_ptr == 0)
  {
    Suppress_newline = TRUE;
  }

  refresh ();

}




/*
 * jim -- being Linux, we must provide stricmp
 */
int stricmp (const char *s1, const char *s2)
{

  int diff;

  while ((*s1 != '\0') || (*s2 != '\0'))
  {
    diff = toupper (*s1) - toupper (*s2);
    if (diff != 0)
    {
      return diff;
    }
    ++s1;
    ++s2;
  }

  return 0;

}
int strnicmp (const char *s1, const char *s2, int n)
{

  int diff, count = 0;

  while ((*s1 != '\0') || (*s2 != '\0') || count < n)
  {
    diff = toupper (*s1) - toupper (*s2);
    if (diff != 0)
    {
      return diff;
    }
    ++s1;
    ++s2;
    ++count;
  }

  return 0;

}

/*
 * Stub graphics routines.
 */
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

