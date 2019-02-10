// hashwnd.h

// hash window

class HashWindow : public Window {
public:
	HashWindow::HashWindow(Object *Parent,char *Title,char *N=NULL,int HashSize=256);
	~HashWindow();
	BOOL Create();
	void MakeHashTable();
	int *HashTable,Mask;
	BOOL EV_SEARCH(GEN_EV_INFO *,TMSG&,GENERIC *);
	virtual GEN_EV_INFO *GetEvInfo()=0;
};
