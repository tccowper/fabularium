#include <mywin.h>
#pragma hdrstop

#include <fstream>

#include <bitmapc.h>
#include <findcol.h>

class LoadFail{};
class SaveFail{};
class Incompatible{};

Bitmap::Bitmap(char*fName)
{
	BITMAPFILEHEADER bmfh;
	BITMAPINFOHEADER bmih;
	std::ifstream f(fName,std::ios::in | std::ios::binary);
	if (f.fail()) throw LoadFail();

	f.read((char*) &bmfh,sizeof(bmfh));
	if (f.gcount()!=sizeof(bmfh)) throw LoadFail();
	f.read((char*) &bmih,sizeof(bmih));
	if (f.gcount()!=sizeof(bmih)) throw LoadFail();
	Width=bmih.biWidth;
	Height=bmih.biHeight;
	Bits=bmih.biBitCount;
	Pitch=(Bits*Width/8+3)&~3; // real width in bytes

	if (Bits<8 || bmih.biCompression!=BI_RGB) throw Incompatible();
	Image.Alloc((int32) Pitch*Height);

	if (Bits==8)
	{
		Pal.Alloc(256);
		f.read((char*) Pal.Data,256*sizeof(RGBQUAD));
	}
	f.read((char*)Image.Data,(int32) Pitch*Height);
	if (f.gcount()!=(int32) Pitch*Height) throw LoadFail();
}

void Bitmap::ConvertTo256(RGBQUAD *DestPal,int PalSize)
{
	Pointer<BYTE> Image2((int32) Width*Height);
	bhp buf,buf2=Image2;

	if (Bits==8)
	{
		// generate pixel translation
		BYTE PixTrans[256];
		int i;
		for (i=0;i<256;i++)
			PixTrans[i]=FindCol(Pal[i].rgbRed,Pal[i].rgbGreen,Pal[i].rgbBlue,DestPal,PalSize);

		// do pixel translation
		for (i=0;i<Height;i++)
		{
			buf=Image + (int32) i*Pitch;
			for (int j=0;j<Width;j++)
				*buf2++=PixTrans[*buf++];
		}
	}
	else
	{
		for (int i=0;i<Height;i++)
		{
			buf=Image + (int32) i*Pitch;
			for (int j=0;j<Width;j++)
			{
				*buf2++=FindCol(buf[2],buf[1],buf[0],DestPal,PalSize);
				buf+=3;
			}
		}
	}

	Image=Image2;
	Bits=8;
	Pitch=Width*Bits/8;
}

void Bitmap::SaveRaw(char *fName)
{
// must have been converted to 8 bit first
	if (Bits!=8) throw Incompatible();

	std::ofstream f(fName,std::ios::out | std::ios::binary);
	if (f.fail()) throw SaveFail();
	int32 Data=Width; // may be 16 bit
	f.write((char*) &(Data=Width),sizeof(int32));
	f.write((char*) &(Data=Height),sizeof(int32));
	f.write((char*) &(Data=1),sizeof(int32));

	for (int i=Height-1;i>=0;i--)
		f.write((char*)(Image + (int32) i*Pitch),Width);
}

void Bitmap::ConvertTo24Bit()
{
	if (Bits==24) return;
	if (Bits!=8) throw Incompatible();
	Pointer<BYTE> Image2((int32) 3*Width*Height);
	bhp buf,buf2=Image2;

	for (int i=0;i<Height;i++)
	{
		buf=Image + (int32) i*Pitch;
		for (int j=0;j<Width;j++)
		{
			RGBQUAD *Col=Pal+*buf++;
			*buf2++=Col->rgbBlue;
			*buf2++=Col->rgbGreen;
			*buf2++=Col->rgbRed;
		}
	}
	Image=Image2;
	Bits=24;
	Pitch=Width*Bits/8;
}

void Bitmap::ChangeSize(int w,int h)
{
	ConvertTo24Bit();
	Pointer<BYTE> Image2((int32) 3*w*h);
	bhp buf,buf2=Image2;
	for (int i=0;i<h;i++)
		for (int j=0;j<w;j++)
		{
			buf=Image + ((int32) Width*j/w)*3 + ((int32) Height*i/h)*Pitch;
			int w2=Width*(j+1)/w-Width*j/w;
			int h2=Height*(i+1)/h-Height*i/h;
			int32 btot,gtot,rtot;
			btot=gtot=rtot=0;

			for (int k=0;k<h2;k++)
			{
				for (int l=0;l<w2;l++)
				{
					btot+=*buf++;
					gtot+=*buf++;
					rtot+=*buf++;
				}
				buf+=Pitch-3*w2;
			}
			*buf2++=btot/(w2*h2);
			*buf2++=gtot/(w2*h2);
			*buf2++=rtot/(w2*h2);
		}
	Image=Image2;
	Width=w;
	Height=h;
	Pitch=Width*Bits/8;
}

