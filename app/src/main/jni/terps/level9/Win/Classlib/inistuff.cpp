// ini stuff
#include <mywin.h>
#pragma hdrstop

#include <string.h>
#include <stdarg.h>
#include <stdio.h>

#include <stringc.h>

#ifdef WIN16

BOOL ReadIniString(const char *Section,const char *Item,String &S,BOOL)
{
	String Temp(256);
	if (GetPrivateProfileString(Section,Item,"",Temp,Temp.Size(),App::IniFile))
	{
		S=Temp;
		return TRUE;
	}
	return FALSE;;
}

void ReadIniBool(const char *Section,const char *Item,BOOL &Bool,BOOL)
{
	char temp[20];
	if	(GetPrivateProfileString(Section,Item,"",temp,20,App::IniFile))
		Bool=(stricmp(temp,"Yes")==0);
}

void WriteIniString(const char *Section,const char *Item,char *Val,BOOL)
{
	WritePrivateProfileString(Section,Item,Val,App::IniFile);
}

void WriteIniBool(const char *Section,const char *Item,BOOL Bool,BOOL)
{
	WritePrivateProfileString(Section,Item,Bool ? "Yes":"No",App::IniFile);
}

void ReadIniInt(const char *Section,const char *Item,int &Int,BOOL)
{
	Int=GetPrivateProfileInt(Section,Item,Int,App::IniFile);
}

void WriteIniInt(const char *Section,const char *Item,int Int,BOOL)
{
	WritePrivateProfileString(Section,Item,String() << Int,App::IniFile);
}

void ReadIniInt(const char *Section,const char *Item,long &Int,BOOL)
{
	String S(12);
	GetPrivateProfileString(Section,Item,"",S,S.Size(),App::IniFile);
	if (!S.Empty()) S >> Int;
}

void WriteIniInt(const char *Section,const char *Item,long Int,BOOL)
{
	WritePrivateProfileString(Section,Item,String() << Int,App::IniFile);
}

#else

// Win32 registry version

BOOL ReadIniString(const char *Section,const char *Item,String &S,BOOL Global)
{
	String Temp(256);
	DWORD Size=Temp.Size();
	if (RegQueryValueEx(Global ? App::Machine : App::User,String() << (char*) Section << ':' << (char*) Item,NULL,NULL,(LPBYTE) Temp.Str,&Size)==ERROR_SUCCESS)
	{
		S=Temp;
		return TRUE;
	}
	return FALSE;
}

void WriteIniString(const char *Section,const char *Item,char *Val,BOOL Global)
{
	HKEY Key=Global ? App::Machine : App::User;
	String Value;
	Value << (char*) Section << ':' << (char*) Item;

	if (Val) RegSetValueEx(Key,Value,0,REG_SZ,(LPBYTE) Val,strlen(Val)+1);
	else RegDeleteValue(Key,Value);
}

void ReadIniBool(const char *Section,const char *Item,BOOL &Bool,BOOL Global)
{
	DWORD Size=sizeof(Bool);
	RegQueryValueEx(Global ? App::Machine : App::User,String() << (char*) Section << ':' << (char*) Item,NULL,NULL,(LPBYTE) &Bool,&Size);
}

void WriteIniBool(const char *Section,const char *Item,BOOL Bool,BOOL Global)
{
	RegSetValueEx(Global ? App::Machine : App::User,String() << (char*) Section << ':' << (char*) Item,0,REG_DWORD,(LPBYTE) &Bool,sizeof(Bool));
}

void ReadIniInt(const char *Section,const char *Item,int &Int,BOOL Global)
{
	DWORD Size=sizeof(Int);
	RegQueryValueEx(Global ? App::Machine : App::User,String() << (char*) Section << ':' << (char*) Item,NULL,NULL,(LPBYTE) &Int,&Size);
}

void WriteIniInt(const char *Section,const char *Item,int Int,BOOL Global)
{
	RegSetValueEx(Global ? App::Machine : App::User,String() << (char*) Section << ':' << (char*) Item,0,REG_DWORD,(LPBYTE) &Int,sizeof(Int));
}

// long duplicates int but keeps 16 bit interface 

void ReadIniInt(const char *Section,const char *Item,long &Int,BOOL Global)
{
	DWORD Size=sizeof(Int);
	RegQueryValueEx(Global ? App::Machine : App::User,String() << (char*) Section << ':' << (char*) Item,NULL,NULL,(LPBYTE) &Int,&Size);
}

void WriteIniInt(const char *Section,const char *Item,long Int,BOOL Global)
{
	RegSetValueEx(Global ? App::Machine : App::User,String() << (char*) Section << ':' << (char*) Item,0,REG_DWORD,(LPBYTE) &Int,sizeof(Int));
}
#endif

int MruNum,BeforeID;
HWND MruHw;
#define MRU_BREAKLEN 40
SimpleList<String> MruList;
SimpleList<String> MruList2;

BOOL DeleteBlock(HMENU hm,int id)
{
	int id2=id;
	while (DeleteMenu(hm,id2,MF_BYCOMMAND)) id2++;
	return id2>id;
}

void AddBlock(HMENU hm,SimpleList<String> &sList,int &Pos,int &Num,int ID)
{
	slIterator<String> it(sList);
	while (it())
	{
		// make short version of name, double up &'s
		String S=it.Get();

		if (S.Len()>MRU_BREAKLEN)
		{
			// keep first 2 and last 2 idenifiers
			int n=2,i=0,j;
			do
			{
				i=1+S.Pos('\\',i);
				if (S[i]=='\\') i++;
				else if (--n==0) break;
			} while (i>0);

			n=2,j=S.Len();
			do
			{
				j=S.rPos('\\',j);
				if (--n==0) break;
			} while (j>=0);
			// remove from i to j
			if (i>0 && j>i)
			{
				S.Remove(i,j-i);
				S.Insert("...",i);
			}
		}

		int i=0;
		while ((i=1+S.Pos('&',i))>0)
		{
			S.Insert('&',i++);
		}

		String T;
		T << '&' << Num << ' ' << S;
		
		InsertMenu(hm,Pos++,MF_BYPOSITION,ID+Num++,T);
		it++;
	}
}


void SetMru(HWND hw,UINT before)
{
	BeforeID=before;
	MruHw=hw;
	HMENU hMenu=GetMenu(hw);
	hMenu=GetSubMenu(hMenu,0);
	if (hMenu==NULL) return;
	int Pos;
	BOOL Del=DeleteBlock(hMenu,MRU_ID+1);
	BOOL Del2=DeleteBlock(hMenu,MRU2_ID+1);

	// get pos of before id
	int n=GetMenuItemCount(hMenu);
	for (Pos=0;Pos<n;Pos++) if (GetMenuItemID(hMenu,Pos)==before) break;
	if (Pos==n) return;
	if (Del) Pos--; // seperators
	if (Del2) Pos--;
	
	int Num=1;
	AddBlock(hMenu,MruList,Pos,Num,MRU_ID);
	if (!Del && Num>1) InsertMenu(hMenu,Pos,MF_BYPOSITION | MF_SEPARATOR,0,NULL);
	Pos++;

	int Num2=Num;
	AddBlock(hMenu,MruList2,Pos,Num2,MRU2_ID+1-Num);
	if (!Del2 && Num2>Num) InsertMenu(hMenu,Pos,MF_BYPOSITION | MF_SEPARATOR,0,NULL);
}

void ReadMru(int Num)
{
	MruNum=Num;
	String S;
	int i=1;
	while (ReadIniString("MRU",String()<<i,S))
	{
		MruList.AddTail(S);
		i++;
	}

	i=1;
	while (ReadIniString("MRU2",String()<<i,S))
	{
		MruList2.AddTail(S);
		i++;
	}
}

void AddToList(SimpleList<String> &sList,char *Add)
{
	slIterator<String> it(sList);
	while (it())
	{
		if (it.Get()==Add)
		{
			// remove it
			it.Remove();
			break;
		}
		it++;
	}
	sList.AddHead(String(Add));
	if (sList.Count()>MruNum) sList.RemoveTail();
	// rebuild mru list
	SetMru(MruHw,BeforeID);
}

void AddToMru(char*Add)
{
	AddToList(MruList,Add);
}

void AddToMru2(char*Add)
{
	AddToList(MruList2,Add);
}

void WriteMru()
{
	int i=1;
	slIterator<String> it(MruList);
	while (it())
	{
		WriteIniString("MRU",String()<<i,it.Get());
		i++;
		it++;
	}

	i=1;
	slIterator<String> it2(MruList2);
	while (it2())
	{
		WriteIniString("MRU2",String()<<i,it2.Get());
		i++;
		it2++;
	}

}
