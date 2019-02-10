/* This code is adapted from BLC, the blorb packer */
/* Credits goes to original author Ross Raszewski */


/* BLC: The Blorb Packager
   V .4b by L. Ross Raszewski
   Copyright 2000 by L. Ross Raszewski, but freely distributable. */

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#define BUF_SIZE 4096
void *my_realloc(void *buf, size_t size)
{
    buf=realloc(buf,size);
    if (!buf)
        {
            fprintf(stderr,"Error: Memory exceeded!\n");
            exit(2);
        }
    return buf;
}

void *my_malloc(int size)
{
    void *buf=malloc(size);
    if (!buf)
        {
            fprintf(stderr,"Error: Memory exceeded!\n");
            exit(2);
        }
    return buf;
}
#define MAX_BLORB 1024
// The program name.  DJGPP munges this, so we rebuild it here
char *MyName;
// The current line in the blc control file
int lineNumber=0;

/* The blorb chunk types. In addition to the chunk data, it holds the index
   information if needed */
struct Chunk {
    unsigned char type[5];
    unsigned char use[5];
    int resourceNumber;
    unsigned long length;
    char *data;
};

void write_int(FILE *f, unsigned int v)
// Write_int writes an integer to a file in blorb format 
{
    unsigned char v1=v&0xFF,
        v2=(v>>8)&0xFF,
        v3=(v>>16)&0xFF,
        v4=(v>>24)&0xFF;

    fwrite(&v4,1,1,f);
    fwrite(&v3,1,1,f);
    fwrite(&v2,1,1,f);
    fwrite(&v1,1,1,f);
}

void str_long(char *f, unsigned int v)
/* Write a long to a string, in a format suitable to later
   using with write_id */
{
    unsigned char v1=v&0xFF,
        v2=(v>>8)&0xFF,
        v3=(v>>16)&0xFF,
        v4=(v>>24)&0xFF;
  
    f[0]=v4;
    f[1]=v3;
    f[2]=v2;
    f[3]=v1;
}
void str_short(char *f, unsigned int v)
/* str_long writes a long to a string, in a format suitable to later
   using with write_id */
{
    unsigned char v1=v&0xFF,
        v2=(v>>8)&0xFF;
  
    f[0]=v2;
    f[1]=v1;
}

void write_id(FILE *f, unsigned char *s)
/* Write a string to a file as a blorb ID string (4 bytes, space padded) */
{
    int i;
    unsigned char sp=' ';
  
    for (i=0; i<strlen(s); i++)
        fwrite(&s[i],1,1,f);
    for (;i<4;i++)
        fwrite(&sp, 1,1,f);
}

void str_id(char *f, unsigned char *s)
/* Write a blorb identifier to a string */
{
    int i;
    unsigned char sp=' ';
  
    for (i=0; i<strlen(s); i++)
        f[i]=s[i];
    for (;i<4;i++)
        f[i]=sp;
}


/*----------------------------------------------------------------------*/
struct Chunk *readChunk(FILE *f)
/* Read one entry from a blc control file and loads a chunk from it */
{
    /* Malloc ourselves a new chunk */
    struct Chunk *chunk=(struct Chunk *)my_malloc(sizeof(struct Chunk));
    char *buffer=NULL; int buflen=0; int current=0; int c;
    /* Read in the BLC line */
    fscanf(f,"%s %d %s ",chunk->use,&(chunk->resourceNumber),chunk->type);
    // Abort if anything went wrong
    if (feof(f)){ free (chunk); return NULL; }
    // Read in the rest of the line to the buffer
    while(1){
        c=fgetc(f);
        if (c==EOF || c=='\n') break;
        if (current==buflen) {
            buflen=(buflen+1)*2;
            buffer=(char *)my_realloc(buffer,buflen);
        }
        buffer[current++]=c;
    }
    {
        FILE *in;
        buffer[current]=0;
        if (chunk->use[0]=='2') chunk->use[0]='0';
        in=fopen(buffer,"rb");
        if (!in) { printf("%s: Error: %d: Can't open file %s\n",MyName,lineNumber,buffer);
            free(buffer);
            free(chunk);
            return NULL;
        }
        free(buffer);
        fseek(in, 0, SEEK_END);
        chunk->length = ftell(in);
        buffer = (char *)my_malloc(chunk->length);
        fseek(in, 0, SEEK_SET);
        if ((fread(buffer,1,chunk->length,in))!=chunk->length)
            fprintf(stderr,"Couldn't read the file. What's up with that?\n");

        chunk->data=buffer;
        fclose(in);
    }
    return chunk;
}

/* Array of all chunks */
struct Chunk *blorb[MAX_BLORB];

/* Number of chunks in this file */
int noOfChunks=1;

/* Number of chunks we need to index */
int indexEntries=0;

/* Offsets of index entries */
unsigned long int *blorbIndex;

/*----------------------------------------------------------------------*/
void buildIndex(FILE *f)
/* Build the index chunk for a blorb file, loading all other chunks */
{
    int i,n=0; char *dp;
    blorb[0]=(struct Chunk *)my_malloc(sizeof(struct Chunk));
    /* Write the chunk type */
    strcpy(blorb[0]->type,"RIdx");
    strcpy(blorb[0]->use,"0"); 
    /* Load all the chunks */
    while(!feof(f)) {
        lineNumber++;
        blorb[noOfChunks] = readChunk(f);
        if(blorb[noOfChunks]) {
            /* Find out how many resources there are */
            if (strcmp(blorb[noOfChunks]->use,"0")) n++;
            noOfChunks++;
        }
    }

    /* Write the length of the resource index chunk, and allocate its data space */
    blorb[0]->length=(12*n)+4;
    blorb[0]->data=(char *)my_malloc(blorb[0]->length);
    blorbIndex=(long *)my_malloc(n*sizeof(unsigned long));
    /* The first thing in the data chunk is the number of entries */
    str_long(blorb[0]->data,n);
    /* Now, scroll through the chunks, noting each one in the index chunk */
    dp=blorb[0]->data+4;
    for(i=1;i<noOfChunks;i++)
        if (strcmp(blorb[i]->use,"0")) {
            str_id(dp,blorb[i]->use);
            dp+=4;
            str_long(dp,blorb[i]->resourceNumber);
            dp+=4;
            blorbIndex[indexEntries++]=(dp-(blorb[0]->data))+20;
            dp+=4;
        }
}

/*----------------------------------------------------------------------*/
void writeChunk(FILE *outFile, struct Chunk *theChunk)
/* Write one chunk to a file */
{
    int zeroPadder=0;
    /* AIFF files are themselves chunks, so we just write their data, not
       their other info. */
    if (strcmp(theChunk->type, "FORM")){
        write_id(outFile, theChunk->type);
        write_int(outFile, theChunk->length);
    }
    fwrite(theChunk->data, 1, theChunk->length, outFile);
    /* Pad chunks of odd length */
    if (theChunk->length%2) fwrite(&zeroPadder, 1, 1, outFile);
}

/*----------------------------------------------------------------------*/
void writeBlorb(FILE *blcFile, FILE *out)
/* WriteBlorb generates a blorb from a BLC file */
{
    int n=0,i;
    /* Write the IFF header */
    write_id(out,"FORM");
    write_id(out,"latr");  /* We'll find this out at the end */
    write_id(out,"IFRS");
    /* Build the index chunk */
    buildIndex(blcFile);
    for(i=0;i<noOfChunks;i++) {
        /* Write the index entries into the index chunk */
        if (strcmp(blorb[i]->use, "0")) {
            long t=ftell(out);
            fseek(out, blorbIndex[n++], SEEK_SET);
            write_int(out, t);
            fseek(out, t, SEEK_SET);
        }
        /* Write the chunk to the file */
        writeChunk(out,blorb[i]);
        /* Deallocate the chunk */
        free(blorb[i]->data);
        free(blorb[i]);
    }
    /* We don't need the index offsets any more either. */
    free(blorbIndex);
    /* Size of the data section of the blorb file */
    n=ftell(out)-8;
    fseek(out, 4, SEEK_SET);
    /* Write that to the file */
    write_int(out, n);
}
