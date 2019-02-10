// bitmap class

#include <pointer.h>

class LoadFail;
class SaveFail;
class Incompatible;

class Bitmap
{
public:
	Bitmap(char*);
	~Bitmap() {}
	void ConvertTo256(RGBQUAD *DestPal,int PalSize);
	void ConvertTo24Bit();
	void SaveRaw(char *fName);
	void ChangeSize(int w,int h);	

	Pointer<RGBQUAD> Pal;
	Pointer<BYTE> Image;
	int Width,Height,Pitch,Bits;
};
