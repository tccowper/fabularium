
#include <stdio.h>
#include <stdlib.h>

#ifndef MAX_FILE_NAME
#define MAX_FILE_NAME 128
#endif

static char long_name[MAX_FILE_NAME + 1];
static char short_name[MAX_FILE_NAME + 1];

/* Try to convert the filename from a Windows 9x/XP
   long filename, if possible */
int convert (void)
{
	short_name[0] = '\0';

	asm {
		push ds
		push es
		mov ax, 7160h
		mov cl, 1
		mov ch, 80h
		mov si, seg long_name
		mov ds, si
		mov si, offset long_name
		mov di, seg short_name
		mov es, di
		mov di, offset short_name
		int 21h
		pop es
		pop ds
		jc error;
	}

	if (short_name[0] == '\0')
		return 0;

	return 1;

error:

	return 0;
}

/* Open then close a file with a long filename, if possible */
int open_and_close (int mode)
{
	asm {
		push ds
		mov ax, 716Ch
		mov bx, 2021h
		mov cx, 0h
		mov dx, mode
		mov si, seg long_name
		mov ds, si
		mov si, offset long_name
		mov di, 0h
		int 21h
		pop ds
		jc error;

		mov bx, ax
		mov ah, 3eh
		int 21h
		jc error;
	}

	return 1;

error:

	return 0;
}

const char far *lshort (const char far *path)
{
	strcpy (long_name, path);

	if (convert ())
		return short_name;

	return path;
}

FILE far *lfopen (const char far *path, const char far *mode)
{
	FILE *f = NULL;

	strcpy (long_name, path);

	switch (mode[0])
	{
	case 'r':

		if (convert ())
			f = fopen (short_name, mode);
		break;

	case 'w':

		if (open_and_close (0x12)) {
			if (convert ())
				f = fopen (short_name, mode);
		}
		break;

	case 'a':

		if (open_and_close (0x11)) {
			if (convert ())
				f = fopen (short_name, mode);
		}
		break;
	}

	if (f == NULL)
		f = fopen (path, mode);
	return f;
}

long lfread (void far *ptr, long size, long n, FILE far *fp)
{
	long sofar = 0;

	n *= size;

	while (sofar < n) {

		size_t next = 0x8000, read;

		if (sofar + next > n)
			next = n - sofar;

		sofar += (read = fread (((char huge *) ptr) + sofar, 1, next, fp));

		if (read < next)
			break;

	}

	return sofar / size;

}

