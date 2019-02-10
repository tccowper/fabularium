// MyWin.h
#define WIN32_LEAN_AND_MEAN

#include <windows.h>
#include <commdlg.h>

// Fixed menu responses
#define IDM_SYSMENU 300
#define IDS_POPUP1 301

#define CM_MDITILE 400
#define CM_MDICASCADE 401
#define CM_MDIARRANGEICONS 402

#define CM_FIRSTCHILD 500

#ifndef __BORLANDC__
// msvc hacks
// needs to be here to prevent <Unknown> knackeration
#include <mmsystem.h>

#define MAXINT INT_MAX
#define strnicmp _strnicmp
#define asm __asm
#define random(x) ((long)x*rand()/RAND_MAX)
#endif

#ifdef TRACEFILE
#include <fstream.h>
#define TRACE(s) { ofstream(TRACEFILE,ios::app) << (char*) (s) << '\n'; }
#else
#define TRACE(s)
#endif

#if defined(__WIN32__) || defined (_WIN32)
#define LOWWPARAM(x) LOWORD(x)
#define HIWLPARAM(x) HIWORD(x.wParam)
#define LLHW(x)		HIWORD(x.wParam)
#define HILP32(x)		x.lParam
typedef int int32;
typedef unsigned int uint32;
typedef BYTE *bhp;
#define huge
#define export
#ifndef WIN32
#define WIN32
#endif
#else
#define LOWWPARAM(x) (x)
#define HIWLPARAM(x) HIWORD(x.lParam)
#define LLHW(x)		LOWORD(x.lParam)
#define HILP32(x)		HIWORD(x.lParam)
typedef long int32;
typedef unsigned long uint32;
typedef BYTE huge *bhp;
#define WIN16
#endif

struct TMSG
{
	UINT Msg;
	WPARAM wParam;
	LPARAM lParam;
	LRESULT RetVal;
};

template <class T> class EV_INFO
{
public:
	unsigned int Type,Msg,Id;
	int Next;
	void (T::*Proc)();
};

//Generic Class for casts
class GENERIC {};
typedef EV_INFO<GENERIC> GEN_EV_INFO;

#define HASH_EV_ENABLE(Type)\
	EV_ENABLE(Type)\
	GEN_EV_INFO *GetEvInfo() { return (GEN_EV_INFO*) EV_TABLE; }\

#define EV_ENABLE(Type)\
	static EV_INFO <Type> EV_TABLE[];\
public:\
	virtual BOOL EV_FIND(TMSG &);\
	typedef Type MyType;\
	typedef void (Type::*MyProc)();

#define EV_START(Type)\
	BOOL Type::EV_FIND(TMSG &Msg)\
	{\
		return EV_SEARCH( (GEN_EV_INFO *) Type::EV_TABLE, Msg, (GENERIC *)this);\
	}\
	EV_INFO <Type> Type::EV_TABLE[]={

#define EV_START2(Type,Base)\
	BOOL Type::EV_FIND(TMSG &Msg)\
	{\
		if (EV_SEARCH( (GEN_EV_INFO *) Type::EV_TABLE, Msg, (GENERIC *)this)) return TRUE;\
		return Base::EV_FIND(Msg);\
	}\
	EV_INFO <Type> Type::EV_TABLE[]={


#define EV_COMMAND(MES,PROC) {0,WM_COMMAND,MES,0,(MyProc) &(MyType::PROC)},
#define EV_COMMANDNOTIFY(MES,PROC) {2,WM_COMMAND,MES,0,(MyProc) &(MyType::PROC)},
#define EV_MESSAGE(MES,PROC) {1,MES,0,0,(MyProc) &(MyType::PROC)},
#define EV_END {0,0,0,0,NULL} };

#include <misc.h>

#include <object.h>
#include <windinfo.h>
#include <statbar.h>
#include <window.h>
#include <mdi.h>
#include <textwnd.h>
#include <ddewnd.h>
#include <app.h>
#include <main.h>

#include <dialog.h>
#include <filedlg.h>
#include <dlgwnd.h>
#include <edit.h>
#include <numedit.h>
#include <realedit.h>
#include <unitedit.h>
#include <radgrp.h>
#include <hashwnd.h>

#include <button.h>
#include <colorbtn.h>
#include <owndrwb.h>

#include <inistuff.h>

#include <printer.h>

