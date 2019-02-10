#include <dos.h>
#include <stdio.h>
#include <stdlib.h>
#include <png.h>

typedef unsigned char byte;
typedef unsigned short word;

#define VesaCall(func) { _AH = 0x4f; _AL = func; asm int 0x10; }

void showPNG (const char *fname, unsigned short Mode1, unsigned short Mode2)
{
	png_structp png_ptr = NULL;
	png_infop info_ptr = NULL;
	png_bytep row_pointer = NULL;
	png_uint_32 width, height;
	png_color_16 *background;
	int bit_depth, color_type;
	FILE *fp = NULL;

	struct {
		word ModeAttributes; /* mode attributes */
		byte WinAAttributes; /* window A attributes */
		byte WinBAttributes; /* window B attributes */
		word WinGranularity; /* window granularity */
		word WinSize; /* window size */
		word WinASegment; /* window A start segment */
		word WinBSegment; /* window B start segment */
		word WinFuncPtrOffs; /* pointer to window function, offset */
		word WinFuncPtrSeg; /* pointer to window function, segment */
		word BytesPerScanLine; /* bytes per scan line */
		word XResolution; /* horizontal resolution */
		word YResolution; /* vertical resolution */
		byte XCharSize; /* character cell width */
		byte YCharSize; /* character cell height */
		byte NumberOfPlanes; /* number of memory planes */
		byte BitsPerPixel; /* bits per pixel */
		byte NumberOfBanks; /* number of banks */
		byte MemoryModel; /* memory model type */
		byte BankSize; /* bank size in kb */
		byte NumberOfImagePages; /* number of images */
		byte Padding; /* reserved for page function */
		byte RedMaskSize; /* size of direct color red mask in bits */
		byte RedFieldPosition; /* bit position of LSB of red mask */
		byte GreenMaskSize; /* size of direct color green mask in bits */
		byte GreenFieldPosition; /* bit position of LSB of green mask */
		byte BlueMaskSize; /* size of direct color blue mask in bits */
		byte BlueFieldPosition; /* bit position of LSB of blue mask */
		byte RsvdMaskSize; /* size of direct color reserved mask in bits */
		byte DirectColorModeInfo; /* Direct Color mode attributes */
		byte Reserved[216]; /* remainder of ModeInfoBlock */
	} ModeInfo;

	byte *Image;
	word *Screen;
	word Window;
	word MemoryWindowPos;
	word CurrentMemoryWindowPos;
	long Offset;
	long Granularity;
	int RIndex, GIndex, BIndex;
	int WidthMult;
	int HeightMult;
	int i;
	float AspectRatio;
	word Pattern;
	word X, Y, Y2;
	word DX, DY;
	word NewMode;

	/* Check for availability of given VESA modes */

	for (i = 0; i < 2; i++)
	{
		NewMode = (i == 0) ? Mode1 : Mode2;

		_ES = FP_SEG (&ModeInfo);
		_DI = FP_OFF (&ModeInfo);
		_CX = NewMode;

		VesaCall (1);

		if (_AH == 0x00 && _AL == 0x4f && (ModeInfo.ModeAttributes & 1))
			goto found;
	}

	goto cleanup;

found:
	/* Activate the new video mode */

	_BX = NewMode; VesaCall (2);

	/* Open the PNG file */

	if ((fp = fopen (fname, "rb")) == NULL)
		goto cleanup;

	/* Initialise PNG structures */

	if ((png_ptr = png_create_read_struct (PNG_LIBPNG_VER_STRING, NULL, NULL, NULL)) == NULL)
		goto cleanup;
	if ((info_ptr = png_create_info_struct (png_ptr)) == NULL)
		goto cleanup;

	if (setjmp (png_ptr->jmpbuf))
		goto cleanup;

	png_init_io (png_ptr, fp);
	png_read_info (png_ptr, info_ptr);
	png_get_IHDR (png_ptr, info_ptr, &width, &height, &bit_depth, &color_type, NULL, NULL, NULL);

	/* Check the aspect ratio of the picture */

	AspectRatio = png_get_pixel_aspect_ratio (png_ptr, info_ptr);
	if (AspectRatio == 1.0) {

		if (width >= 640) {

			WidthMult = 1;
			HeightMult = 1;

		} else {

			WidthMult = 2;
			HeightMult = 2;

		}

	} else {

		WidthMult = 2;
		HeightMult = 1;

	}

	if (HeightMult * height > ModeInfo.YResolution)
		height = ModeInfo.YResolution / HeightMult;

	/* Make sure the picture has correct format */

	if (WidthMult * width > ModeInfo.XResolution || bit_depth != 8 || color_type != PNG_COLOR_TYPE_RGB)
		goto cleanup;

	/* Fill the screen with background colour */

	Window = 0; FP_SEG (Screen) = 0xa000;

	if ((ModeInfo.WinBAttributes & 5) == 5)
		{ Window = 1; FP_SEG (Screen) = ModeInfo.WinBSegment; }
	if ((ModeInfo.WinAAttributes & 5) == 5)
		{ Window = 0; FP_SEG (Screen) = ModeInfo.WinASegment; }

	CurrentMemoryWindowPos = 0;

	RIndex = ModeInfo.RedFieldPosition - 8 + ModeInfo.RedMaskSize;
	GIndex = ModeInfo.GreenFieldPosition - 8 + ModeInfo.GreenMaskSize;
	BIndex = ModeInfo.BlueFieldPosition - 8 + ModeInfo.BlueMaskSize;

	Pattern = 0;

	Granularity = (long) ModeInfo.WinGranularity * 1024;

	FP_OFF (Screen) = (word) Granularity;

	if (png_get_bKGD (png_ptr, info_ptr, &background)) {

		Pattern |= (RIndex > 0) ? (background->red << RIndex) : (background->red >> -RIndex);
		Pattern |= (GIndex > 0) ? (background->green << GIndex) : (background->green >> -GIndex);
		Pattern |= (BIndex > 0) ? (background->blue << BIndex) : (background->blue >> -BIndex);

		for (Y = 0; Y < ModeInfo.YResolution; Y++)

			for (X = 0; X < ModeInfo.XResolution; X++) {

				if (FP_OFF (Screen) == (word) Granularity) {

					FP_OFF (Screen) = 0;

					_BH = 0x00;
					_BL = Window;
					_DX = CurrentMemoryWindowPos++;

					VesaCall (5)

				}

				*Screen++ = Pattern;

			}

		}

	/* Draw the picture line by line */

	if ((row_pointer = malloc (png_get_rowbytes (png_ptr, info_ptr))) == NULL)
		goto cleanup;

	DX = ((ModeInfo.XResolution - WidthMult * width) / 2) & ~1;
	DY = ((ModeInfo.YResolution - HeightMult * height) / 2) & ~1;

	Offset = (long) ModeInfo.BytesPerScanLine * DY + 2 * DX;

	for (Y = 0; Y < height; Y++) {

		png_read_rows (png_ptr, &row_pointer, NULL, 1);

		for (Y2 = 0; Y2 < HeightMult; Y2++) {

			MemoryWindowPos = Offset / Granularity;
			FP_OFF (Screen) = Offset % Granularity;

			Image = row_pointer;

			for (X = 0; X < width; X++) {

				if (CurrentMemoryWindowPos != MemoryWindowPos) {

					CurrentMemoryWindowPos = MemoryWindowPos;

					_BH = 0x00;
					_BL = Window;
					_DX = MemoryWindowPos;

					VesaCall (5)

				}

				Pattern = 0;

				Pattern |= (RIndex > 0) ? (*Image++ << RIndex) : (*Image++ >> -RIndex);
				Pattern |= (GIndex > 0) ? (*Image++ << GIndex) : (*Image++ >> -GIndex);
				Pattern |= (BIndex > 0) ? (*Image++ << BIndex) : (*Image++ >> -BIndex);

				*Screen++ = Pattern;
				if (WidthMult == 2)
					*Screen++ = Pattern;

				if (FP_OFF (Screen) == (word) Granularity)
					{ MemoryWindowPos++; FP_OFF (Screen) = 0; }

			}

			Offset += ModeInfo.BytesPerScanLine;

		}

	}

	/* Wait until a key is pressed */

	_AH = 0; geninterrupt (0x16);

	/* Read the rest of the PNG file and clean up */

	png_read_end (png_ptr, info_ptr);

cleanup:

	if (row_pointer != NULL)
		free (row_pointer);

	if (png_ptr != NULL && info_ptr != NULL)
		png_destroy_read_struct (&png_ptr, &info_ptr, NULL);
	if (png_ptr != NULL && info_ptr == NULL)
		png_destroy_read_struct (&png_ptr, NULL, NULL);

	if (fp != NULL)
		fclose (fp);

}/* showPNG */
