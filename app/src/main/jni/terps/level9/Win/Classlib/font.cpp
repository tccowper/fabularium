#include <mywin.h>
#pragma hdrstop

#include <string.h>

void ShadowText(HDC dc,int x,int y,char *font,int size,char *string,COLORREF Col)
{
	HFONT Font=CreateFont(size,0,0,0,FW_BOLD,0,0,0,
		ANSI_CHARSET,OUT_TT_PRECIS,CLIP_TT_ALWAYS,PROOF_QUALITY,
		VARIABLE_PITCH | 4 | FF_DONTCARE,font);
	HFONT OldFont=(HFONT) SelectObject(dc,Font);
	SIZE sz;
	GetTextExtentPoint(dc, string,strlen(string), &sz);
	int BkMode=GetBkMode(dc);
	SetBkMode(dc,TRANSPARENT);
	COLORREF OldCol=SetTextColor(dc,RGB(0,0,0));
	TextOut(dc,x+1-sz.cx/2,y+1-sz.cy/2,string,strlen(string));
	SetTextColor(dc,Col);
	TextOut(dc,x-sz.cx/2,y-sz.cy/2,string,strlen(string));
	SetTextColor(dc,OldCol);
	SetBkMode(dc,BkMode);
	SelectObject(dc,OldFont);
	DeleteObject(Font);
}

void CentreText(HDC dc,int x,int y,char *font,int size,char *string,COLORREF Col)
{
	HFONT Font=CreateFont(size,0,0,0,FW_NORMAL,0,0,0,
		ANSI_CHARSET,OUT_TT_PRECIS,CLIP_TT_ALWAYS,PROOF_QUALITY,
		VARIABLE_PITCH | 4 | FF_DONTCARE,font);
	HFONT OldFont=(HFONT) SelectObject(dc,Font);
	SIZE sz;
	GetTextExtentPoint(dc, string,strlen(string), &sz);
	int BkMode=GetBkMode(dc);
	SetBkMode(dc,TRANSPARENT);
	COLORREF OldCol=SetTextColor(dc,Col);
	TextOut(dc,x-sz.cx/2,y-sz.cy/2,string,strlen(string));
	SetTextColor(dc,OldCol);
	SetBkMode(dc,BkMode);
	SelectObject(dc,OldFont);
	DeleteObject(Font);
}

void AngleText(HDC dc,int x,int y,char *font,int size,int rot,char *string,COLORREF Col)
{
	HFONT Font=CreateFont(size,0,rot,0,FW_NORMAL,0,0,0,
		ANSI_CHARSET,OUT_TT_PRECIS,CLIP_TT_ALWAYS,PROOF_QUALITY,
		VARIABLE_PITCH | 4 | FF_DONTCARE,font);
	HFONT OldFont=(HFONT) SelectObject(dc,Font);
	int BkMode=SetBkMode(dc,TRANSPARENT);
	COLORREF OldCol=SetTextColor(dc,Col);
	TextOut(dc,x,y,string,strlen(string));
	SetTextColor(dc,OldCol);
	SetBkMode(dc,BkMode);
	SelectObject(dc,OldFont);
	DeleteObject(Font);
}

