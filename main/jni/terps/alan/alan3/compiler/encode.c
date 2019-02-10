/*----------------------------------------------------------------------*\

  encode.c

  Text encoding routines for Alan compiler

  \*---------------------------------------------------------------------*/

#include "alan.h"

#include "util.h"
#include "sysdep.h"

#include "acode.h"
#include "opt.h"		/* Options */
#include "emit.h"

#include "srcp_x.h"

#include "encode.h"


/* PUBLIC */
int txtlen = 0;			/* How many bytes of text data? */


#define NOOFCHAR 256
#define NOOFSYMBOLS (NOOFCHAR+1)
#define MAXFREQ 16383


/* Character frequency table */
static int chFreq[NOOFCHAR+1], cumFreq[NOOFSYMBOLS+1];

/* Codes for lowest and highest Char used */
static int minCh = 256;
static int maxCh = 0;



/*======================================================================

  incFreq()

  Increment the frequency for a particular character.

*/
void incFreq(int ch)		/* IN - The character to increment for */
{
    chFreq[ch]++;

    minCh = minCh < ch? minCh: ch;
    maxCh = maxCh > ch? maxCh: ch;
}


/*----------------------------------------------------------------------

  The actual arithmetic encoding is done here.

*/
static int buffer;			/* Bit buffer */
static int bitsToGo;			/* Available space in buffer */


static void startOutputingBits(void)
{
    bitsToGo = 8;
}


static void outputBit(int bit)	/* IN - the bit to output */
{
    buffer = buffer>>1;		/* Make space for another bit */
    if (bit)
        buffer |= 0x80;
    bitsToGo--;
    if (!bitsToGo) {		/* If no more room, output it */
        putc(buffer, datfil);
        txtlen++;
        bitsToGo = 8;
        buffer = 0;
    }
}


static void doneOutputingBits(void)
{
    putc(buffer>>bitsToGo, datfil);
    txtlen++;
}



/* Current state of the coding */
static CodeValue low, high;	/* Ends of the current region */
static int bitsToFollow;	/* Number of bits to output */
				/* after next bit */


static void bitPlusFollow(int bit) /* IN - the bit to output */
{
    outputBit(bit);
    while (bitsToFollow) {
        outputBit(!bit);
        bitsToFollow--;
    }
}


static void startEncoding(void)
{
    low = 0;
    high = TOPVALUE;
    bitsToFollow = 0;
}


static void encodeChar(int ch)
{
    int symbol = ch + 1;
    long range;			/* Size of the current code region */

    range = (long) (high-low)+1;

    /* Narrow the region to that allotted for this symbol */
    high = low + range*cumFreq[symbol-1]/cumFreq[0]-1;
    low = low + range*cumFreq[symbol]/cumFreq[0];

    for(;;) {			/* Loop to output the bits */
        if (high < HALF)
            bitPlusFollow(0);
        else if (low >= HALF) {
            bitPlusFollow(1);
            low = low - HALF;
            high = high - HALF;
        } else if (low >= ONEQUARTER && high < THREEQUARTER) {
            bitsToFollow++;
            low = low - ONEQUARTER;
            high = high - ONEQUARTER;
        } else
            break;

        /* Scale up the range */
        low = 2*low;
        high = 2*high + 1;
    }
}


static void doneEncoding(void)
{
    /* Output two bits that selects the current code range */
    bitsToFollow++;
    if (low < ONEQUARTER)
        bitPlusFollow(0);
    else
        bitPlusFollow(1);
}


/*======================================================================

  initEncoding()

  Prepare for encoding. Calculate the cumulative frequencies for all
  characters encountered in the text. If the model overflows restart
  by dividing all character frequencies by 2.

*/
void initEncoding(char *textFileName, char *dataFileName)
{
    int i;
    Bool ok = FALSE;		/* Model is ok? */

    /* Open the temporary text and data files (file of encoded or plain text) */
    txtfil = fopen(textFileName, READ_MODE);
    datfil = fopen(dataFileName, WRITE_MODE);
    if (!datfil) {
        char errorString[1000];
        sprintf(errorString, "Could not open output file '%s' for writing", dataFileName);
        SYSERR(errorString, nulsrcp);
    }

    /* Make sure there is at least one character of each in frequency table */
    for (i = 0; i <= EOFChar; i++)
        if (chFreq[i] == 0)
            chFreq[i] = 1;

    /* Calculate and verify that the cumulative freq. is within range */
    while (!ok) {
        /* Set up cumulative frequency counts */
        cumFreq[NOOFSYMBOLS] = 0;
        for (i = NOOFSYMBOLS; i; i--)
            cumFreq[i-1] = cumFreq[i] + chFreq[i-1];
        if (cumFreq[0] > MAXFREQ) {
            /* Change the character frequencies by dividing them by 2 */
            /* Remember to keep the 1's */
            for (i = NOOFSYMBOLS-1; i>1; i--)
                if (chFreq[i] > 1)
                    chFreq[i] >>= 1;
        } else
            ok = TRUE;
    }

}



/*======================================================================

  encode()

  Encodes text from the text file into the data file. If packing is
  turned on, an arithmetic compression is performed, else the text is
  just copied.

*/
void encode(long int *fpos,	/* INOUT - The file position */
	    long int *length)	/* INOUT - Data length */
{
    int len;
    int ch;

    fseek(txtfil, *fpos, 0);
    *fpos = ftell(datfil);

    if (opts[OPTPACK].value) {
        /* Use arithmetic packing model */
        startOutputingBits();
        startEncoding();
        for (len = *length; len; len--) {
            ch = getc(txtfil);
            encodeChar(ch);
        }
        encodeChar(EOFChar);
        doneEncoding();
        doneOutputingBits();
    } else {
        /* use straight text */
        for (len = *length; len; len--)
            putc(getc(txtfil), datfil);
        txtlen += *length;
    }
}


/*======================================================================

  gefreq()

  Generate the frequency table so that the interpreter can unpack the
  text again.

*/
Aaddr gefreq(void)
{
    int i;
    Aaddr adr = nextEmitAddress();

    if (!opts[OPTPACK].value)
        return 0;
    else {
        for (i = 0; i < NOOFSYMBOLS+1; i++) {
            progressCounter();
            emit(cumFreq[i]);
        }
        emit(EOF);
        return(adr);
    }
}

/*======================================================================

  terminateEncoding()

  Terminate the encoding process.

*/
void terminateEncoding(void)
{
    fclose(datfil);
    fclose(txtfil);
}
