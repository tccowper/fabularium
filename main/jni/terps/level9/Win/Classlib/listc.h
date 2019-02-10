#ifndef _list_h
#define _list_h

template <class T> class List
{
public:
	T *Anchor;

	List()
	{
		Anchor=NULL;
	}
	virtual ~List()
	{
		DeleteAll();
	}
	void DeleteAll();
	void AddTail(T *NewNode)
	{
		if (Anchor)
		{
			Anchor->Prev->Next=NewNode;
			NewNode->Prev=Anchor->Prev;
		}
		else Anchor=NewNode;
		NewNode->Next=Anchor;
		Anchor->Prev=NewNode;
		NewNode->BaseList=this;
	}
	void AddHead(T *NewNode)
	{
		if (Anchor)
		{
			NewNode->Next=Anchor;
			NewNode->Prev=Anchor->Prev;
			Anchor->Prev->Next=NewNode;
			Anchor->Prev=NewNode;
		}
		else
		{
			NewNode->Prev=NewNode;
			NewNode->Next=NewNode;
		}
		Anchor=NewNode;
		NewNode->BaseList=this;
	}
	void AddAfter(T *NewNode,T *After)
	{
		After->Next->Prev=NewNode;
		NewNode->Next=After->Next;
		After->Next=NewNode;
		NewNode->Prev=After;
		NewNode->BaseList=this;
	}
	void AddBefore(T *NewNode,T *Before)
	{
		Before->Prev->Next=NewNode;
		NewNode->Prev=Before->Prev;
		Before->Prev=NewNode;
		NewNode->Next=Before;
		NewNode->BaseList=this;
	}
	void Remove(T *Node)
	{
		// get next anchor before removing from chain
		if (Node==Anchor) if ((Anchor=Anchor->Next)==Node) Anchor=NULL;
		Node->Prev->Next=Node->Next;
		Node->Next->Prev=Node->Prev;
		Node->BaseList=NULL;
	}
	void RemoveHead()
	{
		Remove(GetFirst());
	}
	void RemoveTail()
	{
		Remove(GetLast());
	}
	T *GetFirst()
	{
		return Anchor;
	}
	BOOL IsEmpty()
	{
		return Anchor==NULL;
	}
	T *GetLast()
	{
		return Anchor ? Anchor->Prev : NULL;
	}
	void GetNext(T *&Node)
	{
		if ((Node=Node->Next)==Anchor) Node=NULL;
	}
	void GetPrev(T *&Node)
	{
		Node=(Node==Anchor) ? NULL : Node->Prev;
	}
	void Sort();
	void SubSort(T *Item,T *Item2);
	void qSort();
	void Swap(T *&,T *&);
	T* operator [](int Num);
	int Count();
};

template <class T> class BaseNode
{
public:
	BaseNode()
	{
		BaseList=NULL;
	}
	BaseNode(List<T> *aList)
	{
		BaseList=aList;
		aList->AddTail((T*) this);
	}
	virtual ~BaseNode()
	{
		if (BaseList) BaseList->Remove((T*) this);
	}
	virtual int operator < (BaseNode &) { return 0; };
	T *GetNext()
	{
		T *Node=(T*) this;
		BaseList->GetNext(Node);
		return Node;
	}
	List<T> *BaseList;
	T *Next,*Prev;
};

template <class T> void List<T>::DeleteAll()
{
	while (Anchor) delete Anchor;
}

template <class T> T* List<T>::operator [](int Num)
{
	T *Node=Anchor;
	while (Node!=NULL && Num-->0) GetNext(Node);
	return Node;
}

template <class T> int List<T>::Count()
{
	int n=0;
	T *Node=GetFirst();
	while (Node)
	{
		n++;
		GetNext(Node);
	}
	return n;
}

template <class T> void List<T>::Swap(T *&Item1,T *&Item2)
{
	T *i1p=Item1->Prev;
	T *i2p=Item2->Prev;
	T *A=Anchor;

	if (i1p!=Item2)
	{
		Remove(Item2);
		AddAfter(Item2,i1p);
	}

	if (i2p!=Item1)
	{
		Remove(Item1);
		AddAfter(Item1,i2p);
	}

	if (Item1==A) Anchor=Item2;
	else if (Item2==A) Anchor=Item1;

	T *Temp=Item1;
	Item1=Item2;
	Item2=Temp;
}

template <class T> void List<T>::Sort()
{
	T *Item=GetFirst(),*Item2;
	while (Item)
	{
		Item2=Item;
		GetNext(Item2);
		while (Item2)
		{
			if ((*Item2)<(*Item)) Swap(Item,Item2);
			GetNext(Item2);
		}
		GetNext(Item);
	}
}

template <class T> void List<T>::SubSort(T *Item,T *Item2)
{
	if (Item==Item2) return;
	T *First,*Last;
	if (*Item2<*Item)
	{
		First=Item2;
		Last=Item;
	}
	else
	{
		First=Item;
		Last=Item2;
	}

	do
	{
		if (*Item2<*Item) Swap(Item,Item2);

		if (Item->Next==Item2) break;
		Item=Item->Next;
		Item2=Item2->Prev;
	} while (Item!=Item2);

	Item2=Item2->Next; // make sure Item2 is not corrupted
	SubSort(First,Item);
	SubSort(Item2->Prev,Last);
}

template <class T> void List<T>::qSort()
{
	SubSort(GetFirst(),GetLast());
}

/*
Usage:

List<Object> ObjectList; // Create a List of Objects

class Object : BaseNode<Object> // Define the Object
{
	Object(...) : BaseNode<Object>(ObjectList) // for single list, simple constructor
	{}
	Object(List<Object> *ObjList,...) : BaseNode<Object>(ObjList) // multiple lists, more complex constructor
	{}
};

new Object(...); // Create and add to Objectlist
new Object(OtherList,...); // Create and add to any ObjectList

delete O; // deletes object O and removes from its parent list

// For simpler object:

class Object : BaseNode<Object>
{
	Object(...)
	{]
};

O=new Object(...);
if (O->Valid) ObjectList.AddTail(O); else delete O;

*/

// Simple List ***********************************

// e.g.
// SimpleList<int> IntList;
// Item should have default constructor and copy constructor

template <class T> class SimpleNode : public BaseNode< SimpleNode<T> >
{
public:
	SimpleNode(T &Data) { Item=Data; }
	T Item;
	int operator < (SimpleNode&Comp) { return Item<Comp.Item; }
};

template <class T> class SimpleList : public List< SimpleNode<T> >
{
public:
	void AddTailRef(T &Data) { List< SimpleNode<T> >::AddTail(new SimpleNode<T>(Data)); }
	void AddTail(T Data) { List< SimpleNode<T> >::AddTail(new SimpleNode<T>(Data)); }
	void AddHeadRef(T &Data) { List< SimpleNode<T> >::AddHead(new SimpleNode<T>(Data)); }
	void AddHead(T Data) { List< SimpleNode<T> >::AddHead(new SimpleNode<T>(Data)); }
	T operator [](int Num) { return (List<SimpleNode<T> >::operator[](Num))->Item; }
};

template <class T> class slIterator
{
public:
	SimpleList<T> *List;
	SimpleNode<T> *Node;
	slIterator(SimpleList<T> &ListId)
	{
		List=&ListId;
		Node=List->GetFirst();
	}
	int operator()() { return Node!=NULL; }
	operator T() { return Node->Item; }
	operator T*() { return &Node->Item; }
	void operator++() { List->GetNext(Node); }
	void operator++(int) { List->GetNext(Node); }
	void operator--() { List->GetPrev(Node); }
	void operator--(int) { List->GetPrev(Node); }
	void First() { Node=List->GetFirst(); }
	void Last() { Node=List->GetLast(); }
	T Get() { return Node->Item; }
	void Remove()
	{
		SimpleNode<T> *Node2=Node;
		operator++();
		List->Remove(Node2);
	}
};

/*
Iterator I(ListIdendifier);
while (I())
{
	(T) I;
	++I;
}
*/

#endif

