/* glkop.c: Glulxe code for Glk API dispatching.
    Designed by Andrew Plotkin <erkyrath@eblong.com>
    http://eblong.com/zarf/glulx/index.html
*/

/* This code is actually very general; it could work for almost any
   32-bit VM which remotely resembles Glulxe or the Z-machine in design.
   
   To be precise, we make the following assumptions:

   - An argument list is an array of 32-bit values, which can represent
     either integers or addresses.
   - We can read or write to a 32-bit integer in VM memory using the macros
     ReadMemory(addr) and WriteMemory(addr), where addr is an address
     taken from the argument list.
   - A character array is a sequence of bytes somewhere in VM memory.
     The array can be turned into a C char array by the macro
     CaptureCArray(addr, len), and released by ReleaseCArray().
     The passin, passout hints may be used to avoid unnecessary copying.
   - An integer array is a sequence of integers somewhere in VM memory.
     The array can be turned into a C integer array by the macro
     CaptureIArray(addr, len), and released by ReleaseIArray().
     These macros are responsible for fixing byte-order and alignment
     (if the C ABI does not match the VM's). The passin, passout hints
     may be used to avoid unnecessary copying.
   - A Glk object array is a sequence of integers in VM memory. It is
     turned into a C pointer array (remember that C pointers may be more
     than 4 bytes!) The pointer array is allocated by
     CapturePtrArray(addr, len, objclass) and released by ReleasePtrArray().
     Again, the macros handle the conversion.
   - A Glk structure (such as event_t) is a set of integers somewhere
     in VM memory, which can be read and written with the macros
     ReadStructField(addr, fieldnum) and WriteStructField(addr, fieldnum).
     The fieldnum is an integer (from 0 to 3, for event_t.)
   - A VM string can be turned into a C-style string with the macro
     ptr = DecodeVMString(addr). After the string is used, this code
     calls ReleaseVMString(ptr), which should free any memory that
     DecodeVMString allocates.
   - A VM Unicode string can be turned into a zero-terminated array
     of 32-bit integers, in the same way, with DecodeVMUstring
     and ReleaseVMUstring.

     To work this code over for a new VM, just diddle the macros.
*/

#define ReadMemory(addr)  \
    (((addr) == 0xffffffff) \
      ? (stackptr -= 4, Stk4(stackptr)) \
      : (Mem4(addr)))
#define WriteMemory(addr, val)  \
    (((addr) == 0xffffffff) \
      ? (StkW4(stackptr, (val)), stackptr += 4) \
      : (MemW4((addr), (val))))
#define ReadStructField(addr, fieldnum)  \
    (((addr) == 0xffffffff) \
      ? (stackptr -= 4, Stk4(stackptr)) \
      : (Mem4((addr)+(fieldnum)*4)))
#define AddressOfArray(addr)                            \
   ((addr) < ramstart ? (gamefile_start + (addr)) : (memmap + (addr)))
#define WriteStructField(addr, fieldnum, val)  \
    (((addr) == 0xffffffff) \
      ? (StkW4(stackptr, (val)), stackptr += 4) \
      : (MemW4((addr)+(fieldnum)*4, (val))))
#define DecodeVMString(addr)  \
    (make_temp_string(addr))
#define ReleaseVMString(ptr)  \
    (free_temp_string(ptr))
#define DecodeVMUstring(addr)  \
    (make_temp_ustring(addr))
#define ReleaseVMUstring(ptr)  \
    (free_temp_ustring(ptr))

#include "glk.h"
#include "glulxe.h"
#include "gi_dispa.h"

typedef struct dispatch_splot_struct {
  int numwanted;
  int maxargs;
  gluniversal_t *garglist;
  glui32 *varglist;
  int numvargs;
  glui32 *retval;
} dispatch_splot_t;

static void prepare_glk_args(char *proto, dispatch_splot_t *splot);
static void parse_glk_args(dispatch_splot_t *splot, char **proto, int depth,
  int *argnumptr, glui32 subaddress, int subpassin);
static void unparse_glk_args(dispatch_splot_t *splot, char **proto, int depth,
  int *argnumptr, glui32 subaddress, int subpassout);

/* init_dispatch():
   Set up the class hash tables and other startup-time stuff.
*/
int init_dispatch()
{
  return TRUE;
}

/* perform_glk():
   Turn a list of Glulx arguments into a list of Glk arguments,
   dispatch the function call, and return the result. 
*/
glui32 perform_glk(glui32 funcnum, glui32 numargs, glui32 *arglist)
{
  glui32 retval = 0;

  switch (funcnum) {
    /* To speed life up, we implement commonly-used Glk functions
       directly -- instead of bothering with the whole prototype 
       mess. */

  case 0x0047: /* stream_set_current */
    if (numargs != 1)
      goto WrongArgNum;
    glk_stream_set_current(find_stream_by_id(arglist[0]));
    break;
  case 0x0048: /* stream_get_current */
    if (numargs != 0)
      goto WrongArgNum;
    retval = find_id_for_stream(glk_stream_get_current());
    break;
  case 0x0080: /* put_char */
    if (numargs != 1)
      goto WrongArgNum;
    glk_put_char(arglist[0] & 0xFF);
    break;
  case 0x0081: /* put_char_stream */
    if (numargs != 2)
      goto WrongArgNum;
    glk_put_char_stream(find_stream_by_id(arglist[0]), arglist[1] & 0xFF);
    break;
  case 0x00A0: /* char_to_lower */
    if (numargs != 1)
      goto WrongArgNum;
    retval = glk_char_to_lower(arglist[0] & 0xFF);
    break;
  case 0x00A1: /* char_to_upper */
    if (numargs != 1)
      goto WrongArgNum;
    retval = glk_char_to_upper(arglist[0] & 0xFF);
    break;
  case 0x0128: /* put_char_uni */
    if (numargs != 1)
      goto WrongArgNum;
    glk_put_char_uni(arglist[0]);
    break;
  case 0x012B: /* put_char_stream_uni */
    if (numargs != 2)
      goto WrongArgNum;
    glk_put_char_stream_uni(find_stream_by_id(arglist[0]), arglist[1]);
    break;

  WrongArgNum:
    fatal_error("Wrong number of arguments to Glk function.");
    break;

  default: {
    /* Go through the full dispatcher prototype foo. */
    char *proto, *cx;
    dispatch_splot_t splot;
    int argnum, argnum2;

    /* Grab the string. */
    proto = gidispatch_prototype(funcnum);
    if (!proto)
      fatal_error("Unknown Glk function.");

    splot.varglist = arglist;
    splot.numvargs = numargs;
    splot.retval = &retval;

    /* The work goes in four phases. First, we figure out how many
       arguments we want, and allocate space for the Glk argument
       list. Then we go through the Glulxe arguments and load them 
       into the Glk list. Then we call. Then we go through the 
       arguments again, unloading the data back into Glulx memory. */

    /* Phase 0. */
    prepare_glk_args(proto, &splot);

    /* Phase 1. */
    argnum = 0;
    cx = proto;
    parse_glk_args(&splot, &cx, 0, &argnum, 0, 0);

    /* Phase 2. */
    gidispatch_call(funcnum, argnum, splot.garglist);

    /* Phase 3. */
    argnum2 = 0;
    cx = proto;
    unparse_glk_args(&splot, &cx, 0, &argnum2, 0, 0);
    if (argnum != argnum2)
      fatal_error("Argument counts did not match.");

    break;
  }
  }

  return retval;
}

/* read_prefix():
   Read the prefixes of an argument string -- the "<>&+:#!" chars. 
*/
static char *read_prefix(char *cx, int *isref, int *isarray,
  int *passin, int *passout, int *nullok, int *isretained, 
  int *isreturn)
{
  *isref = FALSE;
  *passin = FALSE;
  *passout = FALSE;
  *nullok = TRUE;
  *isarray = FALSE;
  *isretained = FALSE;
  *isreturn = FALSE;
  while (1) {
    if (*cx == '<') {
      *isref = TRUE;
      *passout = TRUE;
    }
    else if (*cx == '>') {
      *isref = TRUE;
      *passin = TRUE;
    }
    else if (*cx == '&') {
      *isref = TRUE;
      *passout = TRUE;
      *passin = TRUE;
    }
    else if (*cx == '+') {
      *nullok = FALSE;
    }
    else if (*cx == ':') {
      *isref = TRUE;
      *passout = TRUE;
      *nullok = FALSE;
      *isreturn = TRUE;
    }
    else if (*cx == '#') {
      *isarray = TRUE;
    }
    else if (*cx == '!') {
      *isretained = TRUE;
    }
    else {
      break;
    }
    cx++;
  }
  return cx;
}

/* prepare_glk_args():
   This reads through the prototype string, and pulls Floo objects off the
   stack. It also works out the maximal number of gluniversal_t objects
   which could be used by the Glk call in question. It then allocates
   space for them.
*/
static void prepare_glk_args(char *proto, dispatch_splot_t *splot)
{
  static gluniversal_t *garglist = NULL;
  static int garglist_size = 0;

  int ix;
  int numwanted, numvargswanted, maxargs;
  char *cx;

  cx = proto;
  numwanted = 0;
  while (*cx >= '0' && *cx <= '9') {
    numwanted = 10 * numwanted + (*cx - '0');
    cx++;
  }
  splot->numwanted = numwanted;

  maxargs = 0; 
  numvargswanted = 0; 
  for (ix = 0; ix < numwanted; ix++) {
    int isref, passin, passout, nullok, isarray, isretained, isreturn;
    cx = read_prefix(cx, &isref, &isarray, &passin, &passout, &nullok,
      &isretained, &isreturn);
    if (isref) {
      maxargs += 2;
    }
    else {
      maxargs += 1;
    }
    if (!isreturn) {
      if (isarray) {
        numvargswanted += 2;
      }
      else {
        numvargswanted += 1;
      }
    }
        
    if (*cx == 'I' || *cx == 'C') {
      cx += 2;
    }
    else if (*cx == 'Q') {
      cx += 2;
    }
    else if (*cx == 'S' || *cx == 'U') {
      cx += 1;
    }
    else if (*cx == '[') {
      int refdepth, nwx;
      cx++;
      nwx = 0;
      while (*cx >= '0' && *cx <= '9') {
        nwx = 10 * nwx + (*cx - '0');
        cx++;
      }
      maxargs += nwx; /* This is *only* correct because all structs contain
                         plain values. */
      refdepth = 1;
      while (refdepth > 0) {
        if (*cx == '[')
          refdepth++;
        else if (*cx == ']')
          refdepth--;
        cx++;
      }
    }
    else {
      fatal_error("Illegal format string.");
    }
  }

  if (*cx != ':' && *cx != '\0')
    fatal_error("Illegal format string.");

  splot->maxargs = maxargs;

  if (splot->numvargs != numvargswanted)
    fatal_error("Wrong number of arguments to Glk function.");

  if (garglist && garglist_size < maxargs) {
    glulx_free(garglist);
    garglist = NULL;
    garglist_size = 0;
  }
  if (!garglist) {
    garglist_size = maxargs + 16;
    garglist = (gluniversal_t *)glulx_malloc(garglist_size 
      * sizeof(gluniversal_t));
  }
  if (!garglist)
    fatal_error("Unable to allocate storage for Glk arguments.");

  splot->garglist = garglist;
}

/* parse_glk_args():
   This long and unpleasant function translates a set of Floo objects into
   a gluniversal_t array. It's recursive, too, to deal with structures.
*/
static void parse_glk_args(dispatch_splot_t *splot, char **proto, int depth,
  int *argnumptr, glui32 subaddress, int subpassin)
{
  char *cx;
  int ix, argx;
  int gargnum, numwanted;
  void *opref;
  gluniversal_t *garglist;
  glui32 *varglist;
  
  garglist = splot->garglist;
  varglist = splot->varglist;
  gargnum = *argnumptr;
  cx = *proto;

  numwanted = 0;
  while (*cx >= '0' && *cx <= '9') {
    numwanted = 10 * numwanted + (*cx - '0');
    cx++;
  }

  for (argx = 0, ix = 0; argx < numwanted; argx++, ix++) {
    char typeclass;
    int skipval;
    int isref, passin, passout, nullok, isarray, isretained, isreturn;
    cx = read_prefix(cx, &isref, &isarray, &passin, &passout, &nullok,
      &isretained, &isreturn);
    
    typeclass = *cx;
    cx++;

    skipval = FALSE;
    if (isref) {
      if (!isreturn && varglist[ix] == 0) {
        if (!nullok)
          fatal_error("Zero passed invalidly to Glk function.");
        garglist[gargnum].ptrflag = FALSE;
        gargnum++;
        skipval = TRUE;
      }
      else {
        garglist[gargnum].ptrflag = TRUE;
        gargnum++;
      }
    }
    if (!skipval) {
      glui32 thisval;

      if (typeclass == '[') {

        parse_glk_args(splot, &cx, depth+1, &gargnum, varglist[ix], passin);

      }
      else if (isarray) {
        /* definitely isref */

        switch (typeclass) {
        case 'C':
          /* This test checks for a giant array length, which is 
             deprecated. It displays a warning and cuts it down to
             something reasonable. Future releases of this interpreter
             may remove this test and go on to verify_array_addresses(),
             which treats this case as a fatal error. */
          if (varglist[ix+1] > endmem
              || varglist[ix]+varglist[ix+1] > endmem) {
              nonfatal_warning_i("Memory access was much too long -- perhaps a print_to_array call with only one argument", varglist[ix+1]);
              varglist[ix+1] = endmem - varglist[ix];
          }
          verify_array_addresses(varglist[ix], varglist[ix+1], 1);
          garglist[gargnum].array = (void*) AddressOfArray(varglist[ix]);
          gargnum++;
          ix++;
          garglist[gargnum].uint = varglist[ix];
          gargnum++;
          cx++;
          break;
        case 'I':
          /* See comment above. */
          if (varglist[ix+1] > endmem/4
              || varglist[ix+1] > (endmem-varglist[ix])/4) {
              nonfatal_warning_i("Memory access was much too long -- perhaps a print_to_array call with only one argument", varglist[ix+1]);
              varglist[ix+1] = (endmem - varglist[ix]) / 4;
          }
          verify_array_addresses(varglist[ix], varglist[ix+1], 4);
          garglist[gargnum].array = AddressOfArray(varglist[ix]);
          gargnum++;
          ix++;
          garglist[gargnum].uint = varglist[ix];
          gargnum++;
          cx++;
          break;
        case 'Q':
          /* This case was added after the giant arrays were deprecated,
             so we don't bother to allow for that case. We just verify
             the length. */
          verify_array_addresses(varglist[ix], varglist[ix+1], 4);
          garglist[gargnum].array = AddressOfArray(varglist[ix]);
          gargnum++;
          ix++;
          garglist[gargnum].uint = varglist[ix];
          gargnum++;
          cx++;
          break;
        default:
          fatal_error("Illegal format string.");
          break;
        }
      }
      else {
        /* a plain value or a reference to one. */

        if (isreturn) {
          thisval = 0;
        }
        else if (depth > 0) {
          /* Definitely not isref or isarray. */
          if (subpassin)
            thisval = ReadStructField(subaddress, ix);
          else
            thisval = 0;
        }
        else if (isref) {
          if (passin)
            thisval = ReadMemory(varglist[ix]);
          else
            thisval = 0;
        }
        else {
          thisval = varglist[ix];
        }

        switch (typeclass) {
        case 'I':
          if (*cx == 'u')
            garglist[gargnum].uint = (glui32)(thisval);
          else if (*cx == 's')
            garglist[gargnum].sint = (glsi32)(thisval);
          else
            fatal_error("Illegal format string.");
          gargnum++;
          cx++;
          break;
        case 'Q':
          garglist[gargnum].opaqueref = (void *) thisval;
          gargnum++;
          cx++;
          break;
        case 'C':
          if (*cx == 'u') 
            garglist[gargnum].uch = (unsigned char)(thisval);
          else if (*cx == 's')
            garglist[gargnum].sch = (signed char)(thisval);
          else if (*cx == 'n')
            garglist[gargnum].ch = (char)(thisval);
          else
            fatal_error("Illegal format string.");
          gargnum++;
          cx++;
          break;
        case 'S':
          garglist[gargnum].charstr = DecodeVMString(thisval);
          gargnum++;
          break;
#ifdef GLK_MODULE_UNICODE
        case 'U':
          garglist[gargnum].unicharstr = DecodeVMUstring(thisval);
          gargnum++;
          break;
#endif /* GLK_MODULE_UNICODE */
        default:
          fatal_error("Illegal format string.");
          break;
        }
      }
    }
    else {
      /* We got a null reference, so we have to skip the format element. */
      if (typeclass == '[') {
        int numsubwanted, refdepth;
        numsubwanted = 0;
        while (*cx >= '0' && *cx <= '9') {
          numsubwanted = 10 * numsubwanted + (*cx - '0');
          cx++;
        }
        refdepth = 1;
        while (refdepth > 0) {
          if (*cx == '[')
            refdepth++;
          else if (*cx == ']')
            refdepth--;
          cx++;
        }
      }
      else if (typeclass == 'S' || typeclass == 'U') {
        /* leave it */
      }
      else {
        cx++;
        if (isarray)
          ix++;
      }
    }    
  }

  if (depth > 0) {
    if (*cx != ']')
      fatal_error("Illegal format string.");
    cx++;
  }
  else {
    if (*cx != ':' && *cx != '\0')
      fatal_error("Illegal format string.");
  }
  
  *proto = cx;
  *argnumptr = gargnum;
}

/* unparse_glk_args():
   This is about the reverse of parse_glk_args(). 
*/
static void unparse_glk_args(dispatch_splot_t *splot, char **proto, int depth,
  int *argnumptr, glui32 subaddress, int subpassout)
{
  char *cx;
  int ix, argx;
  int gargnum, numwanted;
  void *opref;
  gluniversal_t *garglist;
  glui32 *varglist;
  
  garglist = splot->garglist;
  varglist = splot->varglist;
  gargnum = *argnumptr;
  cx = *proto;

  numwanted = 0;
  while (*cx >= '0' && *cx <= '9') {
    numwanted = 10 * numwanted + (*cx - '0');
    cx++;
  }

  for (argx = 0, ix = 0; argx < numwanted; argx++, ix++) {
    char typeclass;
    int skipval;
    int isref, passin, passout, nullok, isarray, isretained, isreturn;
    cx = read_prefix(cx, &isref, &isarray, &passin, &passout, &nullok,
      &isretained, &isreturn);
    
    typeclass = *cx;
    cx++;

    skipval = FALSE;
    if (isref) {
      if (!isreturn && varglist[ix] == 0) {
        if (!nullok)
          fatal_error("Zero passed invalidly to Glk function.");
        garglist[gargnum].ptrflag = FALSE;
        gargnum++;
        skipval = TRUE;
      }
      else {
        garglist[gargnum].ptrflag = TRUE;
        gargnum++;
      }
    }
    if (!skipval) {
      glui32 thisval = 0;

      if (typeclass == '[') {

        unparse_glk_args(splot, &cx, depth+1, &gargnum, varglist[ix], passout);

      }
      else if (isarray) {
        /* definitely isref */

        switch (typeclass) {
        case 'C':
          gargnum++;
          ix++;
          gargnum++;
          cx++;
          break;
        case 'I':
          gargnum++;
          ix++;
          gargnum++;
          cx++;
          break;
        case 'Q':
          gargnum++;
          ix++;
          gargnum++;
          cx++;
          break;
        default:
          fatal_error("Illegal format string.");
          break;
        }
      }
      else {
        /* a plain value or a reference to one. */

        if (isreturn || (depth > 0 && subpassout) || (isref && passout)) {
          skipval = FALSE;
        }
        else {
          skipval = TRUE;
        }

        switch (typeclass) {
        case 'I':
          if (!skipval) {
            if (*cx == 'u')
              thisval = (glui32)garglist[gargnum].uint;
            else if (*cx == 's')
              thisval = (glui32)garglist[gargnum].sint;
            else
              fatal_error("Illegal format string.");
          }
          gargnum++;
          cx++;
          break;
        case 'Q':
          if (!skipval) {
            thisval = (glui32)garglist[gargnum].opaqueref;
          }
          gargnum++;
          cx++;
          break;
        case 'C':
          if (!skipval) {
            if (*cx == 'u') 
              thisval = (glui32)garglist[gargnum].uch;
            else if (*cx == 's')
              thisval = (glui32)garglist[gargnum].sch;
            else if (*cx == 'n')
              thisval = (glui32)garglist[gargnum].ch;
            else
              fatal_error("Illegal format string.");
          }
          gargnum++;
          cx++;
          break;
        case 'S':
          if (garglist[gargnum].charstr)
            ReleaseVMString(garglist[gargnum].charstr);
          gargnum++;
          break;
#ifdef GLK_MODULE_UNICODE
        case 'U':
          if (garglist[gargnum].unicharstr)
            ReleaseVMUstring(garglist[gargnum].unicharstr);
          gargnum++;
          break;
#endif /* GLK_MODULE_UNICODE */
        default:
          fatal_error("Illegal format string.");
          break;
        }

        if (isreturn) {
          *(splot->retval) = thisval;
        }
        else if (depth > 0) {
          /* Definitely not isref or isarray. */
          if (subpassout)
            WriteStructField(subaddress, ix, thisval);
        }
        else if (isref) {
          if (passout)
            WriteMemory(varglist[ix], thisval); 
        }
      }
    }
    else {
      /* We got a null reference, so we have to skip the format element. */
      if (typeclass == '[') {
        int numsubwanted, refdepth;
        numsubwanted = 0;
        while (*cx >= '0' && *cx <= '9') {
          numsubwanted = 10 * numsubwanted + (*cx - '0');
          cx++;
        }
        refdepth = 1;
        while (refdepth > 0) {
          if (*cx == '[')
            refdepth++;
          else if (*cx == ']')
            refdepth--;
          cx++;
        }
      }
      else if (typeclass == 'S' || typeclass == 'U') {
        /* leave it */
      }
      else {
        cx++;
        if (isarray)
          ix++;
      }
    }    
  }

  if (depth > 0) {
    if (*cx != ']')
      fatal_error("Illegal format string.");
    cx++;
  }
  else {
    if (*cx != ':' && *cx != '\0')
      fatal_error("Illegal format string.");
  }
  
  *proto = cx;
  *argnumptr = gargnum;
}

/* find_stream_by_id():
   This is used by some interpreter code which has to, well, find a Glk
   stream given its ID. 
*/
strid_t find_stream_by_id(glui32 objid)
{
  if (!objid)
    return NULL;

  /* Recall that class 1 ("b") is streams. */
  return (strid_t) objid;
}

/* find_id_for_window():
   Return the ID of a given Glk window.
*/
glui32 find_id_for_window(winid_t win)
{
  if (!win)
    return 0;

  return (glui32) win;
}

/* find_id_for_stream():
   Return the ID of a given Glk stream.
*/
glui32 find_id_for_stream(strid_t str)
{
  if (!str)
    return 0;

  return (glui32) str;
}

/* find_id_for_fileref():
   Return the ID of a given Glk fileref.
*/
glui32 find_id_for_fileref(frefid_t fref)
{
  if (!fref)
    return 0;

  return (glui32) fref;
}

/* find_id_for_schannel():
   Return the ID of a given Glk schannel.
*/
glui32 find_id_for_schannel(schanid_t schan)
{
  if (!schan)
    return 0;

  return (glui32) schan;
}

