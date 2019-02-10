template <class T>

class Array
{
public:
	Array(int n)
	{
		Data=new T*[Size=n];
		Used=0;
	}
	~Array();
	void Clear();
	void Add(T *Item)
	{
		if (Used==Size) Resize();
		Data[Used++]=Item;
	}
	void Delete(int i)
	{
		delete Data[i];
		memcpy(Data+i,Data+i+1,(--Used-i)*sizeof(T));
	}
	void Resize();

	T **Data;
	int Used,Size;
	T* operator [](int i)
	{
		return Data[i];
	}
};

template <class T>
Array<T>::~Array()
{
	Clear();
}

template <class T>
void Array<T>::Clear()
{
	for (int i=0;i<Used;i++)
		delete Data[i];
	Used=0;
}

#define INC 10

template <class T>
void Array<T>::Resize()
{
// create new array
	T **NewData=new T*[Size=(long)Size*(100+INC)/100];
	for (int i=0;i<Used;i++) NewData[i]=Data[i];
	delete[] Data;
	Data=NewData;
}

