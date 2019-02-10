#include <mywin.h>
#pragma hdrstop

#define FACTOR(x) ((int)((x)*256))

int FindCol(int Red,int Green,int Blue,RGBQUAD *Cols,int PalSize)
{
	// find closest match
	long a,b,c;
	int NearCol;
	unsigned long MinDiff,Diff;
	MinDiff=0xffffffffL;

	for (int x=0;x<PalSize;x++)
	{
		a=Red-Cols[x].rgbRed;
		b=Green-Cols[x].rgbGreen;
		c=Blue-Cols[x].rgbBlue;
		Diff=a*a*FACTOR(0.9)+b*b*FACTOR(0.8)+c*c*FACTOR(1.0); // get correct factors
		if (Diff<MinDiff)
		{
			NearCol=x;
			MinDiff=Diff;
		}
	}
	// NearCol;
	return (NearCol==PalSize-1) ? 255 : NearCol;
}
