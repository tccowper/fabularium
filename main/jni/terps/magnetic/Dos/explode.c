/*
 * explode.c
 *
 * This an utility program to extract all files from the Magnetic Scrolls
 * Collection and Wonderland, MS-DOS versions. The file names are case
 * sensitive and up to six characters long. Each file also has a type which
 * is a number in the range from 3 to 10. The meaning of certain file types
 * isn't quite clear, whereas others are rather obvious:
 *
 *  	 3 - ?
 *  	 4 - ?
 *  	 5 - ?
 *  	 6 - picture data
 *  	 7 - additional picture information
 *  	 8 - font data
 *  	 9 - ?
 *   	10 - mouse pointer data
 *
 * Don't be surprised if this program produces 1030 files from Wonderland
 * even though it reports 1031 files. The data for the fixed font appears
 * twice in the resource files due to a harmless bug.
 *
 * MS-DOS file names aren't case sensitive, and therefore one or two
 * underscores are appended to certain file names to make them distinct. If
 * your OS suffers from the same restriction, make sure the #ifdef'ed code
 * is included during compilation.
 *
 * Usage: explode RDF-file
 *
 * This utility program was written by Stefan Jokisch in 1997.
 *
 * [tab width == 3]
 *
 */

#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>

#define BUF_SIZE 1024
#define DOT '.'

typedef unsigned char byte;

FILE *files[10];
long length[10];

int filec;

byte buf[BUF_SIZE];

void error (const char *s)
{

	 fprintf (stderr, "Fatal: %s\n", s);
	 exit (1);

}/* error */

void ms_open (const char *name)
{
	FILE *fp;
	int result;

	if (!(fp = fopen (name, "rt")))
		error ("Cannot open RDF file");

	while ((result = fscanf (fp, " %256s %ld\n", buf, length + filec)) == 2)
		if (!(files[filec++] = fopen (buf, "rb")))
			error ("Cannot open resource file");

	fclose (fp);

	if (result != EOF)
		error ("Bad format RDF file");

}/* ms_open */

void ms_read (long *offset, unsigned size)
{
	long l = 0;
	unsigned read = 0;
	int i;

	for (i = 0; i < filec && read < size; i++) {

		if (l + length[i] > *offset + read) {

			unsigned s = size - read;

			fseek (files[i], *offset + read - l, 0);

			if (*offset + size > l + length[i])
				s = length[i] - ftell (files[i]);

			if (!fread (buf + read, s, 1, files[i]))
				error ("Resource file read error");

			read += s;

		}

		l += length[i];

	}

	if (read != size)
		error ("Bad offset");

	*offset += size;

}/* ms_read */

void extract_file (const char *name, long offset, long size)
{
	long read = 0;
	FILE *out;

	if (!(out = fopen (name, "wb")))
		error ("Cannot open output file");

	while (read < size) {

		long s = size - read;

		if (s > BUF_SIZE)
			s = BUF_SIZE;

		ms_read (&offset, (unsigned) s);

		if (!fwrite (buf, s, 1, out))
			error ("Error writing output file");

		read += s;

	}

	if (fclose (out))
		error ("Error writing output file");

}/* extract_file */

void extract_all (void)
{
	long offset = 0;
	int count, i;

	ms_read (&offset, 4);

	offset = ((long) buf[0] <<  0) |
				((long) buf[1] <<  8) |
				((long) buf[2] << 16) |
				((long) buf[3] << 24);

	ms_read (&offset, 2);

	count = ((int) buf[0] << 0) |
			  ((int) buf[1] << 8);

	printf ("Going to extract %d files...\n\n", count);

	for (i = 0; i < count; i++) {

		char name[13];
		long o, l;
		unsigned n;
		int j;
#ifdef __MSDOS__
		int underscores;
#endif

		ms_read (&offset, 18);

		o = ((long) buf[2] <<  0) |
			 ((long) buf[3] <<  8) |
			 ((long) buf[4] << 16) |
			 ((long) buf[5] << 24);

		l = ((long) buf[6] <<  0) |
			 ((long) buf[7] <<  8) |
			 ((long) buf[8] << 16) |
			 ((long) buf[9] << 24);

		n = ((unsigned) buf[16] << 0) |
			 ((unsigned) buf[17] << 8);

		for (j = 0; j < 6 && buf[10 + j]; j++)
			name[j] = buf[10 + j];

		name[j] = 0;

		printf ("(%04d)  %-6s  offset %05lx  length %05lx  type %04x",
				  i + 1, name, o, l, n);

#ifdef __MSDOS__

		underscores = 2;

		for (j = 0; name[j]; j++)
			if (islower (name[j]))
				underscores = 1;

		if (islower (name[0]))
			underscores = 0;

		for (j = 0; name[j]; j++)
			name[j] = toupper (name[j]);

		while (underscores--)
			name[j++] = '_';

#endif

		name[j++] = '.';
		name[j++] = '0' + (n / 100) % 10;
		name[j++] = '0' + (n / 10) % 10;
		name[j++] = '0' + (n / 1) % 10;

		name[j] = 0;

		printf ("  saving as %s\n", name);

		extract_file (name, o, l);

	}

}/* extract_all */

void ms_close (void)
{

	int i;

	for (i = 0; i < filec; i++)
		fclose (files[i]);

}/* ms_close */

int main (int argc, char **argv)
{

	if (argc == 2) {

		ms_open (argv[1]);

		extract_all ();

		ms_close ();

	} else printf ("Explode by Stefan Jokisch\n"
						"(for use with Magnetic Scrolls Collection and Wonderland)\n"
						"\n"
						"Usage: %s RDF-file\n", argv[0]);

	return 0;

}/* main */
