// hash window stuff

#include <mywin.h>
#pragma hdrstop

// cannot make hash table in constructor as GetEvInfo still points to pure virtual function

HashWindow::HashWindow(Object *Parent,char *Title,char *N,int HashSize) : Window(Parent,Title,N)
{
	// allocate hash table
	HashTable=new int[HashSize];
	// initialise hash table
	for (int i=0;i<HashSize;i++) HashTable[i]=-1;
	Mask=HashSize-1;
}

BOOL HashWindow::Create()
{
	MakeHashTable();
	return Window::Create();
}

void HashWindow::MakeHashTable()
{
	GEN_EV_INFO *Ptr=GetEvInfo(),*Start=Ptr;
	// make hash table
	BYTE Hash;
	while (Ptr->Msg)
	{
		Hash=(Ptr->Msg ^ (Ptr->Msg>>8)) & Mask;
		// add msg onto Hash list
		Ptr->Next=HashTable[Hash];
		HashTable[Hash]=(char) (Ptr-Start);
		Ptr++;
	}
}

HashWindow::~HashWindow()
{
	// free hash table
	delete[] HashTable;
}

// define new search routine
BOOL HashWindow::EV_SEARCH(GEN_EV_INFO *EV_TABLE,TMSG &Msg,GENERIC *Owner)
{
	int First=HashTable[(Msg.Msg ^ (Msg.Msg>>8))&Mask];
	if (First<0) return FALSE;
	GEN_EV_INFO *Ptr=EV_TABLE+First;
	while (Ptr->Msg!=Msg.Msg || ( Ptr->Type==0 && Ptr->Id!=Msg.wParam ) )
	{
		if ((First=Ptr->Next)<0) return FALSE;
		Ptr=EV_TABLE+First;
	}
	switch (Ptr->Type)
	{
		case 0:
			( Owner->*Ptr->Proc)();
			break;

		case 1:
			return (Owner->*( (Proc1) Ptr->Proc) ) (Msg);
		}

	return TRUE;
}

