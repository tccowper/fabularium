/*	macSPA.c							Date: 1995-02-15/reibert@home

    Adaption of SPA to Macintosh dialogs.

#define DBG
*/
#ifdef THINK_C
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>
#include <console.h>

#include "spa.h"
#if _SPA_H_!=42
#error "SPA header file version 4.2 required"
#endif

#define PRIVATE static
#define PUBLIC


/*----------------------------------------------------------------------
	
*/
#define DITL(N) struct {	/* Generic type for DITL list */\
	short count;\
	struct {\
		Handle h;\
		Rect box;\
		char kind;\
	} item[N];\
}

PRIVATE pascal void drawRing(DialogPtr p, short i); /* Own routine */

/* Used by all dialogs: */
PRIVATE Rect bounds;

PRIVATE int
	maxDOWN,
	maxRIGHT;



enum Fonts { Chicago = 1, Monaco = 4, Geneva = 3 };


PRIVATE SpaErrFun *pErrFun;	/* Points to errorfunction */

#define spaErr(s, m, a) (*pErrFun)(s, m, a)

#ifdef DBG
#define cancel() exit(1)
#else
#define cancel() abort()
#endif


/*----------------------------------------------------------------------
	Strings (read from resources)
*/
#define STR_RSRC 990	/* STR# for messages */

#ifdef SPA_LANG
#error "Messages must be defined in STR# 990"	
#endif

enum { Usg, ILF, IVE, RVE, FOE, TMI, MAX_STR };	/* Order sensitive */
PRIVATE char *SpaStr[6];

PRIVATE unsigned char *SpaAlertStr[8], *SpaStrOK, *SpaStrCancel, *SpaStrHelp;


/*----------------------------------------------------------------------
	_SPA_ITEM are mapped to a limited amount of dialog items
*/
#ifndef SPA_ITEMS
#define SPA_ITEMS 100	/* Max items ( args + opts + keys + bits ) */
#endif

#define _SPA_KeyItem (_SPA_Private+1)	/* New item for each keyword */
#define _SPA_BitsItem (_SPA_Private+2)	/* -"-               bit */


#define NI 4	/* No standard items */
#define MAX_SPAS (SPA_ITEMS+NI)

PRIVATE DITL(MAX_SPAS) itemList = { NI,
	0, { 0, -56, 20, 0 }, ctrlItem+btnCtrl,		/* OK */
	0, { 0, -116, 20, -64 }, ctrlItem+btnCtrl,	/* Cancel */
	0, { 0, -174, 20, -120 }, ctrlItem+btnCtrl,	/* Help */
	(Handle)drawRing, { 0, -56, 20, 0 }, userItem+itemDisable,	/* ring around OK button */
};
PRIVATE _SPA_ITEM *dialed[MAX_SPAS];
enum { Help = 3, okRing };

PRIVATE DialogPtr dp;	/* The main dialog */


PRIVATE struct {	/* Structure to hold various information about items */
	Rect box;		/* size */
	char kind;		/* ditl type */
	FILE *file;		/* default file */
	Boolean text;	/* has text ditl */
} prop[_SPA_Private+3] = {	/* CAVEAT! Order sensitive! */
	{{0}, 0, NULL, FALSE},								/* None */	
	{{4, 0, 18, 22}, ctrlItem+chkCtrl, NULL, FALSE},	/* Flag */
	{{4, 0, 20, 12}, statText, NULL, FALSE},			/* Bits */
	{{8, 0, 24, 12}, statText, NULL, TRUE},				/* Integer */
	{{8, 0, 24, 12}, statText, NULL, TRUE},				/* String */
	{{4, 0, 20, 12}, statText, NULL, FALSE},			/* KeyWord */
	{{4, 0, 24, 22}, ctrlItem+btnCtrl, NULL, FALSE},	/* Function */
	{{4, 0, 24, 22}, ctrlItem+btnCtrl, NULL, FALSE},//{{0}, 0, NULL, FALSE},								/* Help */
	{{6, 0, 26, 22}, ctrlItem+btnCtrl, stdin, TRUE},	/* InFile */
	{{6, 0, 26, 22}, ctrlItem+btnCtrl, stdout, TRUE},	/* OutFile */
	{{8, 0, 24, 12}, statText, NULL, TRUE},				/* Float */
	{{8, 0, 24, 12}, statText, NULL, FALSE},			/* Comment */
	{{0}, 0, NULL, FALSE},								/* Private */
	{{0, 2, 16, 24}, ctrlItem+radCtrl, NULL, FALSE},	/* KeyItem */
	{{0, 2, 16, 24}, ctrlItem+chkCtrl, NULL, FALSE}		/* BitsItem */
};


/*----------------------------------------------------------------------
	Built-in items (concerning io with console)
	Strings are fetched from resources 
*/

enum { inCon, inFile };
enum { outCon, outFile, outEcho, outPrint };
PRIVATE int inChoice, outChoice;

PRIVATE char *inKws[3];
PRIVATE char *outKws[5];

PRIVATE SPA_DECLARE(biItems)
	SPA_KEYWORD(NULL, NULL, inChoice, inKws, inCon, NULL)
	SPA_KEYWORD(NULL, NULL, outChoice, outKws, outCon, NULL)
SPA_END


/*----------------------------------------------------------------------
	Help dialog structures & data
*/

typedef struct hitem {
	struct hitem *next;
	char *name;
	char *help;
} HItem, *HItemP;

PRIVATE HItemP 
	firstH = NULL,
	lastH = NULL;

PRIVATE int
	noH = 0,
	nameW = 0,
	helpW = 0;

PRIVATE DialogPtr hp;

PRIVATE DITL(2) hList = { 1,
	0, { 0, -56, 20, 0 }, ctrlItem+btnCtrl,		/* OK */
	(Handle)drawRing, { 0, -56, 20, 0 }, userItem+itemDisable,	/* ring around OK button */
};
#define hRing 2


/*----------------------------------------------------------------------
	Finding screen size w/o globals
*/
PRIVATE Rect screen;

PRIVATE void getScreenSize()
{
	GrafPort tempPort;
	GrafPtr savedPort;
	
	GetPort(&savedPort);
	OpenPort(&tempPort);
	screen = tempPort.portRect;
	SetPort(savedPort); 
	ClosePort(&tempPort);
}

/*----------------------------------------------------------------------
	Center dialog
*/
PRIVATE void centerDialog(Rect *rb)
{
	register int h = rb->right+4;
	register int v = rb->bottom+4;
	
	bounds.left = (screen.right - h)/2;
	if (bounds.left<0) bounds.left = 0;
	bounds.top = ((screen.bottom-20) - v)/2 + 20;
	if (bounds.top<20) bounds.top = 20;
	bounds.bottom = bounds.top + v;
	bounds.right = bounds.left + h;
}

/*----------------------------------------------------------------------
	Draw ring around OK button (user-item proc).
*/
PRIVATE pascal void drawRing(DialogPtr p, short i) {
	Handle itemHandle;
	short itemType;
	Rect itemRect;
	PenState pState;

	GetDItem(p, i, &itemType, &itemHandle, &itemRect);
	GetPenState(&pState);
	PenNormal();
	PenSize(3, 3);
/*	PenPat(dkGray);
ForeColor(blueColor); */
	InsetRect(&itemRect, -4, -4);
	FrameRoundRect(&itemRect, 16, 16);
	SetPenState(&pState);
}


/*----------------------------------------------------------------------
 	SFGetFile -- should need an additinal statText (DITL #10)
*/
PRIVATE Point SFWhere = { 100, 100 };
PRIVATE SFTypeList SFTypes = { 'TEXT' };

/*----------------------------------------------------------------------
	Set volume to open file
*/
PRIVATE void setVolume(int vRefNum) {
	ioParam pb;

	pb.ioNamePtr = 0;
	pb.ioVRefNum = vRefNum;
	PBSetVolSync((ParmBlkPtr)&pb);
}

/*----------------------------------------------------------------------
	Return default volume
*/
PRIVATE int defVolume;

PRIVATE void getDefVolume() {
	ioParam pb;

	pb.ioNamePtr = 0;
	pb.ioVRefNum = 0;
	PBGetVolSync((ParmBlkPtr)&pb);
	defVolume = pb.ioVRefNum;
}


/*----------------------------------------------------------------------
	Return main dialog item handle
*/
PRIVATE ControlHandle ditem(int i) {
	short kind;
	Handle item;
	Rect box;

	GetDItem(dp, i, &kind, &item, &box);
	return((ControlHandle)item);
}

/*----------------------------------------------------------------------
PRIVATE void setIBox(int i, int t, int l, int b, int r) {
	SetRect(&(itemList.item[i].box), l, t, r, b);
}
*/
#define setIBox(i,t,l,b,r) SetRect(&(itemList.item[i].box), l, t, r, b)


/*----------------------------------------------------------------------
	Map C string to static Pascal ditto.
*/
PRIVATE unsigned char *pass(register char *c) {
	static unsigned char buf[256];
	
	*buf = sprintf((char *)buf+1, "%s", c? c: "");
	return buf;
}

/*----------------------------------------------------------------------
	Map C string to Pascal ditto.

PRIVATE char *c2p(register char *c) {
	register int i;
	int l = strlen(c);
	
	for (i = l; i; i--) c[i] = c[i-1];
	*c = l;
	return c;
}
*/

/*----------------------------------------------------------------------
	Map Pascal string to C ditto.
*/
PRIVATE char *p2c(register char *c) {
	register int i, l = *c;
	
	for (i = 1; i<=l; i++) c[i-1] = c[i];
	c[l] = 0;
	return c;
}

/*----------------------------------------------------------------------
	Map C string to static Pascal ditto with extra colon.
*/
PRIVATE unsigned char *pasc(register char *c) {
	static unsigned char buf[256];
	
	*buf = sprintf((char *)buf+1, "%s:", c);
	return buf;
}

/*----------------------------------------------------------------------
	Copyconcat C strings to one new Pascal ditto.
*/
PRIVATE char *cccp(char *s1, char *s2) {
	register char *p;
	char buf[256];
	
	*buf = sprintf(buf+1, "%s%s", s1, s2);
	p = NewPtr(*buf+2);
	strcpy(p, buf);
	return p;
}


/*----------------------------------------------------------------------
	Create a new help line, compute size
*/
PRIVATE void newH(char *px, char *name, char *help) {
	register HItemP n = (HItemP)NewPtr(sizeof(HItem));
	register int nW, hW;
	
	if (n) {
		n->name = cccp(px, name);
		n->help = cccp(px, help);
		n->next = NULL;
		if (firstH) {
			lastH = lastH->next = n;
		} else {
			firstH = lastH = n;
		}
		if (*name) {
			nW = StringWidth((unsigned char *)n->name)+8;
			if (nW>nameW) nameW = nW;
			hW = StringWidth((unsigned char *)n->help);
			if (hW>helpW) helpW = hW;
		}
		noH++;
	}
}


/*----------------------------------------------------------------------
    Create help line for one SPA_ITEM.
*/
PRIVATE void newHelpItem(
	register _SPA_TYPE type,
	register char *name,
    register char *help
){
	register char *o;
	static char *h = NULL; /* A pointer into last string after a newline */
    
	if (!help /*|| !*help*/) help = h;
    if (help /*&& *help*/) {
    	o = help;
	    for (;*help; help++) {
			if (*help=='\n') { *help++ = 0; break; }
	   	}
		h = (*help? help: NULL);			
		newH(type>_SPA_Private?"  ":"", name, o);
    }
}


/*----------------------------------------------------------------------
	Call before first help request
*/
PRIVATE void prepareHelp() {
	GrafPtr p;
	int f, s;
	register int n;
	
	GetPort(&p); f = p->txFont; s = p->txSize;
	TextFont(Geneva); TextSize(9);
	for (n= NI; n<itemList.count; n++)
		if (dialed[n]) 
			newHelpItem(dialed[n]->type, dialed[n]->name, dialed[n]->help);

	for (n=OK-1; n<hRing; n++)
		OffsetRect(&(hList.item[n].box), helpW + nameW + 12, 34 + noH*11);

	TextFont(f); TextSize(s);
}

/*----------------------------------------------------------------------
	Display simple help in own dialog window
*/
PRIVATE void help() {
	GrafPtr savePort;
	Handle items, ok;
	char buf[256];
	short i;
	register int y;
	register HItemP hi;
	Rect box;

	GetPort(&savePort);

	centerDialog(&hList.item[hRing-1].box);

	asm {
		lea hList,a0
		move.l	#sizeof hList,d0
		_PtrToHand
		move.l	a0,items
	}
	hp = NewDialog(0, &bounds, CurApName, 0, 1, (WindowPtr)-1, 0, 0, items);
	GetDItem(hp, OK, &i, &ok, &box);
	SetCTitle((ControlHandle)ok, (unsigned char *)SpaStrOK);
	ShowWindow(hp);
	SetPort(hp);
	TextFont(Geneva); 
	*buf = sprintf(buf+1, "%s %s!", SpaStr[Usg], SpaAlertName);
	y = (hList.item[hRing-1].box.right-StringWidth((unsigned char *)buf))/2;
	MoveTo(y<4? 4: y,10); DrawString((unsigned char *)buf);
	TextSize(9);
	MoveTo(4, 20);
	for (y=30, hi= firstH; hi; hi= hi->next) {
		MoveTo(4, y); 
		if (*hi->name) {
			DrawString((unsigned char *)hi->name);
			MoveTo(8+nameW, y);
		} 
		DrawString((unsigned char *)hi->help);
		y += 11;
	}	
	ModalDialog(0, &i);
	DisposDialog(hp);
	SetPort(savePort);
/*	if (i==Cancel) cancel();*/
}

#ifdef DBG
PRIVATE dumpSpaItem(register _SPA_ITEM *s) {
	printf("[");
	if (s) {
	printf("%d %10s(%p) %p(), %d %p, %p %p, %g %p, %p %p, %p()",
			s->type, s->name, s->help, s->postFun,
			s->i, s->ip, s->s, s->sp, s->f, s->fp, s->F, s->FP, s->hFun);
	} else printf("NULL");
	printf("] %lx\n", s);
}

PRIVATE dumpItem(register int i) {
	printf("%2d = [", i);
	printf("%4x %3d %3d %3d %3d %p", itemList.item[i].kind,
		   itemList.item[i].box.top, itemList.item[i].box.left,
		   itemList.item[i].box.bottom, itemList.item[i].box.right,
		   itemList.item[i].h);
	printf("]\n");
}
#endif

/*----------------------------------------------------------------------
	Write errors with standard alert dialog
*/
PRIVATE SPA_ERRFUN(biErrFun) {
	spaAlert(sev, "%s: %s", msg, add);
}

/*----------------------------------------------------------------------
	Create new _SPA_[Bit,Key]Item (for setItem)
*/
PRIVATE _SPA_ITEM *new_SPA(
	register _SPA_ITEM *old,
	char *name,
	_SPA_TYPE type,
	int def
){
	register _SPA_ITEM *s = (_SPA_ITEM *)NewPtr(sizeof(_SPA_ITEM));
	
	memcpy(s, old, sizeof(_SPA_ITEM));
	s->type = type;
	s->name = name;
	s->help = NULL;
	s->s = (char *)old;	/* Let s point to the master */
	s->i = def;
	return s;
}

/*----------------------------------------------------------------------
	Map SPA items to DITL dittos
*/

PRIVATE int setItem(register int i, register _SPA_ITEM *spit) {
	register int len;
	Rect r;

	if (i>=MAX_SPAS) {
		spaErr('S', SpaStr[TMI], "setItem");
		exit(2);
	}

	len = StringWidth((unsigned char *)pass(spit->type==_SPA_Comment?spit->help:spit->name));
	dialed[i] = spit;
	
	r = prop[spit->type].box;
	r.right += len;		/* r is the right size but at origo */
	itemList.item[i].box = r;
	OffsetRect(&(itemList.item[i].box), 2, maxDOWN);
	
	itemList.item[i++].kind = prop[spit->type].kind;
	switch (spit->type) {
	case _SPA_Comment:
		if (!spit->help) dialed[--i] = NULL;
		break;
	case _SPA_String:
		*spit->sp = malloc((size_t)256);
	case _SPA_Integer:
	case _SPA_Float:
		setIBox(i, maxDOWN+8, itemList.item[i-1].box.right + 4, 
				maxDOWN+24, maxRIGHT);
		itemList.item[i].kind = editText;
		dialed[i++] = NULL;
		break;
	case _SPA_InFile:
	case _SPA_OutFile:
		setIBox(i, maxDOWN+8, itemList.item[i-1].box.right+8,
				maxDOWN+24, maxRIGHT);
		itemList.item[i].kind = editText;
		dialed[i++] = NULL;
		*spit->sp = malloc((size_t)64);
		break;
	case _SPA_Flag:
		if (dialed[i-2]->type == _SPA_Flag) {
			r.right += itemList.item[i-2].box.right;
			if (r.right<=maxRIGHT)
				setIBox(i-1, 
					itemList.item[i-2].box.top, itemList.item[i-2].box.right+4,
					itemList.item[i-2].box.bottom, r.right);
		}
		break;
	case _SPA_Help:
	case _SPA_Function:
		if (*spit->name==0) { dialed[--i] = NULL; break; }
		if (dialed[i-2]->type == _SPA_Function || dialed[i-2]->type == _SPA_Help) {
			r.right += itemList.item[i-2].box.right;
			if (r.right<=maxRIGHT)
				setIBox(i-1, 
					itemList.item[i-2].box.top, itemList.item[i-2].box.right+4,
					itemList.item[i-2].box.bottom, r.right);
		}
		break;
	case _SPA_KeyWord: {
		register char **kws;
		register int fKW;
		
		fKW = i;
		for (kws = spit->sp; *kws; kws++) {
			i = setItem(i, new_SPA(spit, *kws, _SPA_KeyItem, fKW));
		}
		break;}
	case _SPA_BitsItem:
	case _SPA_KeyItem:
		itemList.item[i-1].box = r;
		OffsetRect( &(itemList.item[i-1].box), itemList.item[i-2].box.right, itemList.item[i-2].box.top);
		break;
	case _SPA_Bits: {
		register char *set, *setItemName;
		register int ths;

		for (ths = 0, set = spit->s; *set; set++, ths++) {
			setItemName = NewPtr(2);
			setItemName[0] = *set; setItemName[1] = 0;
			i = setItem(i, new_SPA(spit, setItemName, _SPA_BitsItem, ths));
		}
		break;}
	case _SPA_None:
		dialed[--i] = NULL;
		break;
	default:
		spaErr('F', SpaStr[ILF], "setItem");
		break;
	}
	/* Vi vet att i-1 är nederst/högerst för denna spaItem */
/*	if (itemList.item[i-1].box.bottom>maxDOWN) */
		maxDOWN = itemList.item[i-1].box.bottom;
	if (itemList.item[i-1].box.right>maxRIGHT)
		maxRIGHT = itemList.item[i-1].box.right;

	return i;
}


/*----------------------------------------------------------------------
	Create main dialog
*/
PRIVATE void createDialog(
	register _SPA_ITEM args[], 
	register _SPA_ITEM opts[]
){
	ControlHandle items, d1, d2;
	register int i, n;
	char buf[256];

	maxDOWN = maxRIGHT = 0;
	i = NI;
	for (n= 0; biItems[n].name; n++) i = setItem(i, &biItems[n]);
	if (args) for (n= 0; args[n].name; n++) i = setItem(i, &args[n]);
	if (opts) for (n= 0; opts[n].name; n++) i = setItem(i, &opts[n]);
	itemList.count = i;

	for (i=OK-1; i<okRing; i++)
		OffsetRect(&(itemList.item[i].box), maxRIGHT, maxDOWN+8);

	centerDialog(&itemList.item[okRing-1].box);

	asm {
		lea itemList,a0
		move.l	#sizeof itemList,d0
		_PtrToHand
		move.l	a0,items
	}
	dp = NewDialog(0, &bounds, CurApName, 0, 1, (WindowPtr)-1, 0, 0, (Handle)items);
	SetCTitle(ditem(OK), (unsigned char *)SpaStrOK);
	SetCTitle(ditem(Cancel), (unsigned char *)SpaStrCancel);
	SetCTitle(ditem(Help), (unsigned char *)SpaStrHelp);

/*	for (i= 0; i<itemList.count; i++) dumpItem(i);/**/
	for (i= NI; i<itemList.count; i++) if (dialed[i]) {
/*		printf("%2d = ", i); dumpSpaItem(dialed[i]);/**/
		d1 = ditem(i+1);
		d2 = ditem(i+2);
		switch (dialed[i]->type) {
		case _SPA_Flag:
			SetCtlValue(d1, dialed[i]->i);
			*(dialed[i]->ip) = dialed[i]->i;
			/* Fall thru */
		case _SPA_Help:
		case _SPA_Function:
			SetCTitle(d1, (unsigned char *)pass(dialed[i]->name));
			break;
		case _SPA_Comment:
			SetIText((Handle)d1, pass(dialed[i]->help));
			break;
		case _SPA_String:
			SetIText((Handle)d1, pasc(dialed[i]->name));
			if (dialed[i]->s) {
				strcpy(*dialed[i]->sp, dialed[i]->s);
			} else **dialed[i]->sp = 0;
			SetIText((Handle)d2, pass(dialed[i]->s));
			break;
		case _SPA_Integer:
			SetIText((Handle)d1, pasc(dialed[i]->name));
			*buf = sprintf(buf+1, "%d", dialed[i]->i);
			*dialed[i]->ip = dialed[i]->i;
			SetIText((Handle)d2, (unsigned char *)buf);
			break;
		case _SPA_InFile:
		case _SPA_OutFile:
			SetCTitle(d1, pasc(dialed[i]->name));
			if (dialed[i]->s) {
				strcpy(*dialed[i]->sp,  dialed[i]->s);
			} else **dialed[i]->sp = 0;
			dialed[i]->i = defVolume;
			SetIText((Handle)d2, pass(dialed[i]->s));
			break;
		case _SPA_Float:
			SetIText((Handle)d1, pasc(dialed[i]->name));
			*buf = sprintf(buf+1, "%g", dialed[i]->f);
			*dialed[i]->fp = dialed[i]->f;
			SetIText((Handle)d2, (unsigned char *)buf);
			break;
		case _SPA_KeyWord:
		case _SPA_Bits:
			SetIText((Handle)d1, pasc(dialed[i]->name));
			*dialed[i]->ip = dialed[i]->i;
			break;
		case _SPA_KeyItem:
			SetCTitle(d1, pass(dialed[i]->name));
			SetCtlValue(d1, 
				((_SPA_ITEM *)(dialed[i]->s))->i == i-dialed[i]->i);
			break;
		case _SPA_BitsItem:
			SetCTitle(d1, pass(dialed[i]->name));
			SetCtlValue(d1, 
				((_SPA_ITEM *)(dialed[i]->s))->i & (1<<dialed[i]->i));
			break;
		}
	}
}


PRIVATE char *GS(int n) {
	static char buf[256];

	switch (dialed[n]->type) {
	case _SPA_InFile:
	case _SPA_OutFile:
	case _SPA_String:
	case _SPA_Integer:
	case _SPA_Float:
		GetIText((Handle)ditem(n+2), (unsigned char *)buf);
		p2c(buf);
		break;
	case _SPA_KeyWord:
		strcpy(buf, (dialed[n]->sp)[*dialed[n]->ip]);	
		break;
    case _SPA_Bits: {
		register int i = 0, j = 0;
		register char *set = dialed[n]->s;	
		for (; set[i]; i++) {
	    	if ((1<<i)&*(dialed[n]->ip)) buf[j++] = set[i];
		}
		buf[j] = 0;
    } break;
	case _SPA_Flag:
		strcpy(buf, *(dialed[n]->ip)? "ON": "OFF");
		break;
	default:
		strcpy(buf, dialed[n]->name);
		break;
	}
	return buf;
}


/*----------------------------------------------------------------------
*/
PRIVATE char *newStr(unsigned char *p) {
	char *c = NewPtrClear(*p+1);
	memcpy(c, p+1, *p);
	return c;
}

PRIVATE unsigned char *cpStr(unsigned char *p) {
	unsigned char *c = (unsigned char *)NewPtr(*p+1);
	memcpy(c, p, *p+1);
	return c;
}

PRIVATE void initStrs() {
#define next(S) (S += *S, S++)
	struct STR {
		short length;
		unsigned char n[];
	} *hp;
	register unsigned char *c;
	register int i;
	
	Handle h = GetResource('STR#', STR_RSRC);
	if (h == NULL) {
		printf("PANIC!! Resource STR# %d not found!", STR_RSRC);
		exit(1);
	}

	HLock(h); /* For safety */
	hp = (struct STR *)*h;

	/* CAVEAT! Extremly order sensitive code follows! */
	c = hp->n; /* First string */
	for (i = Usg; i<MAX_STR; i++) { SpaStr[i] = newStr(c); next(c); } /* 1..6 */
	next(c);
	next(c);
	next(c);
	SpaStrOK = cpStr(c); next(c);	/* 10 */
	SpaStrCancel = cpStr(c); next(c);
	SpaStrHelp = cpStr(c); next(c);

	for (i = 0; i<7; i++) { SpaAlertStr[i] = cpStr(c); next(c); } /* 13..19 */
	outKws[0] = inKws[0] = newStr(c); next(c); /* 20 */
	outKws[1] = inKws[1] = newStr(c); next(c);
	outKws[2] = newStr(c); next(c);
	outKws[3] = newStr(c); next(c);

	biItems[0].name = newStr(c); next(c);	/* 24 */
	biItems[0].help = newStr(c); next(c);
	biItems[1].name = newStr(c); next(c);
	biItems[1].help = newStr(c); 			/* 27 */

	HUnlock(h);
	ReleaseResource(h);
	
#undef next
}


PRIVATE void cleanUp() {
	HItemP g, h = firstH;
	int i;
	
	while (h) { /* Cleanup any help messages */
		DisposePtr(h->name);
		DisposePtr(h->help);
		g = h->next;
		DisposePtr((char *)h);
		h = g;
	}

	for (i = Usg; i<MAX_STR; i++) DisposePtr(SpaStr[i]);

	DisposePtr((char *)SpaStrOK);
	DisposePtr((char *)SpaStrCancel);
	DisposePtr((char *)SpaStrHelp);

	for (i = 0; i<2; i++) {
		DisposePtr(inKws[i]);
		DisposePtr(biItems[i].name);
		DisposePtr(biItems[i].help);
	}
	for (i = 0; i<4; i++) DisposePtr(outKws[i]);
}



/***********************************************************************
    Public functions.
*/

/*----------------------------------------------------------------------
	The "main" routine
*/
PUBLIC int _spaPreProcess(
	int *ac,				/* Not used */
	char **av[],			/* Not used */
	_SPA_ITEM arguments[],
	_SPA_ITEM options[],
	SpaErrFun *errfun
){
	short i;
	register int n;
	SFReply sfr;
	register Handle h;
	char buf[256];

	console_options.title = CurApName;
	cshow(stdin);
    pErrFun= (errfun? errfun: biErrFun);

    getDefVolume();
	getScreenSize();

	initStrs();
    if (!SpaAlertName)	/* If no name given, get application name */
    	SpaAlertName = newStr(CurApName);

	/* present dialog */
	createDialog(arguments, options);
	ShowWindow(dp);
	
	/* engage in dialog */
	do {
		ModalDialog(0, &i);
		n = i-1; h = (Handle)ditem(i);	/* Set for faster & smaller code */
		switch (i) {
		case OK: break;
		case Cancel: cancel();
		case Help:
			if (!firstH) prepareHelp();
			help();
			break;
		default:
			if (dialed[n]) { 
				switch (dialed[n]->type) {
				case _SPA_Flag:
					*dialed[n]->ip = ! *dialed[n]->ip;
					SetCtlValue((ControlHandle)h, *dialed[n]->ip);
					break;
				case _SPA_KeyItem:
					SetCtlValue(ditem(*dialed[n]->ip+dialed[n]->i+1), 0);
					SetCtlValue((ControlHandle)h, 1);
					*dialed[n]->ip = n-dialed[n]->i;
					break;
				case _SPA_BitsItem: 
					*dialed[n]->ip ^= (1<<dialed[n]->i);
					SetCtlValue((ControlHandle)h, !GetCtlValue((ControlHandle)h));
					break;
				case _SPA_InFile:
				case _SPA_OutFile:
					if (dialed[n]->type==_SPA_InFile) 
						SFGetFile(SFWhere, pasc(dialed[n]->name), 
								  NULL, 1, SFTypes, NULL, &sfr);
					else
						SFPutFile(SFWhere, pasc(dialed[n]->name), 
								  (ConstStr255Param)"\p", NULL, &sfr);
					if (sfr.good) {
						sprintf(*dialed[n]->sp, "%#s", sfr.fName);
						dialed[n]->i = sfr.vRefNum;
						SetIText((Handle)ditem(i+1), sfr.fName);
					} else {
						dialed[n]->i = defVolume;
						if (dialed[n]->s)
							strcpy(*dialed[n]->sp, dialed[n]->s);
						else **dialed[n]->sp = 0;
					}
					break;
				case _SPA_Help:
					if (dialed[n]->hFun) {	/* Exec help function */
						(*dialed[n]->hFun)(dialed[n]->name, GS(n), TRUE);
					}
					break;
				} 
				if (dialed[n]->postFun) {	/* Exec any function */
					(*dialed[n]->postFun)(
					 	dialed[n]->name,
					 	GS(n),
						dialed[n]->type==_SPA_Flag? *dialed[n]->ip: TRUE
					);
				}
			} else if (dialed[--n]) {
				GetIText(h, (unsigned char *)buf); p2c(buf);
				switch (dialed[n]->type) {
				case _SPA_InFile:
				case _SPA_OutFile:
				case _SPA_String:
					strcpy(*dialed[n]->sp, buf);
					break;
				case _SPA_Integer:
					*dialed[n]->ip = dialed[n]->i;
					if (*buf && sscanf(buf, "%i", dialed[n]->ip)!=1) {
						/* Number not ok */
						*buf = sprintf(buf+1, "%d", dialed[n]->i);
						SetIText(h, (unsigned char *)buf);
						spaErr('W', SpaStr[IVE], dialed[n]->name);
					}
					break;
				case _SPA_Float:
					*(dialed[n]->fp) = dialed[n]->f;
					if (*buf && sscanf(buf, "%g", dialed[n]->fp)!=1) {
						/* Number not ok */
						*buf = sprintf(buf+1, "%g", dialed[n]->f);
						SetIText(h, (unsigned char *)buf);
						spaErr('W', SpaStr[RVE], dialed[n]->name);
					}
					break;
				}
			}
		}
	} while (i != OK);

	DisposDialog(dp);

	/* Set stdin? */
	if (inChoice == inFile) {
		SFGetFile(SFWhere, pasc(biItems[0].name), NULL, 1, SFTypes, NULL, &sfr);
		if (sfr.good) {
			setVolume(sfr.vRefNum);
			freopen(p2c((char *)sfr.fName), "r", stdin);
		}
	}

	/* Set stdout? */
	if (outChoice == outPrint)
		cecho2printer(stdout);
	else if (outChoice != outCon) {
		SFPutFile(SFWhere, pasc(biItems[1].name), (ConstStr255Param)"\p", NULL, &sfr);
		if (sfr.good) {
			setVolume(sfr.vRefNum);
			p2c((char *)sfr.fName);
			if (outChoice == outFile)
				freopen((char *)sfr.fName, "w", stdout);
			else
				cecho2file((char *)sfr.fName, 0, stdout);
		}
	}

	/* Update user files; some cleanup */
	for (n= NI; n<itemList.count; n++)
		if (dialed[n]) switch (dialed[n]->type) {
		case _SPA_InFile:
		case _SPA_OutFile:
			if (**dialed[n]->sp) {
				setVolume(dialed[n]->i);
				*dialed[n]->FP = 
					fopen(*dialed[n]->sp, 
						  (dialed[n]->type==_SPA_InFile? "r": "w"));
				if (!*dialed[n]->FP) { /* open failure */
					spaErr('E', SpaStr[FOE], *dialed[n]->sp);
					*dialed[n]->FP = prop[dialed[n]->type].file;
					**(dialed[n]->sp) = 0;
    			}
			} else
				*dialed[n]->FP = prop[dialed[n]->type].file;
			break;
		case _SPA_BitsItem:
			DisposePtr(dialed[n]->name);
			/* Fall thru */
		case _SPA_KeyItem: 
			DisposePtr((char *)dialed[n]);
			break;
		}
	
	cleanUp();
	return 1;
}

/*======================================================================
    spaExit()

    Clean up and then exit.
*/
PUBLIC void spaExit(
    int exitCode
){
	DisposDialog(dp);
/*	cleanUp(); -- what the heck were dying soon enough */
	exit(exitCode);
}


/*======================================================================
    Program name for Alerts (tail of argv[0])
*/
PUBLIC char *SpaAlertName = NULL;

/*======================================================================
    Alert at this level and higher
*/
PUBLIC char SpaAlertLevel = 'I';

/*======================================================================
    spaAlert()

    Error notification (name: sev! <fmt ...>) to user.
    Exits on severe errors.
*/
PRIVATE int level(char sev)
{
	static char *sevstr = "DIWEFS";
	register char *s;
	
	s = strchr(sevstr, sev);
	return s? s-sevstr: 6;
}

PUBLIC void spaAlert(	/* Error notification; Exits on severe errors */
    char sev,			/* IN - [IWEFS] */
    char * fmt,			/* IN - printf-format for additional things */
    ...					/* IN - addtional things */
){
    va_list ap;
    short i,
		lev;			/* LEVEL gives alert number: */
	static short	
		aIrt[] = { 991, 991, 		/* Note for DI */
				   992, 992, 		/* Alert for WE */
				   990, 990, 990	/* Stop for FS? */
		}; 

    lev = level(sev);
    if (lev>=level(SpaAlertLevel)) {
		char buf[256];
		
		va_start(ap, fmt);
		*buf = vsprintf(buf+1, fmt, ap);
		va_end(ap);
		ParamText(pass(SpaAlertName), SpaAlertStr[lev],
				  (unsigned char *)buf, (unsigned char *)"\p");
		i = Alert(aIrt[lev], NULL);
    } else i = Cancel+1;

	if (lev>3 /*level('E')*/ || i==Cancel) cancel();
}

#endif
/*--- EoF --------------------------------------------------------------*/
