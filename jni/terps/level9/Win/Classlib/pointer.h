// pointer class

#ifndef _pointer_h
#define _pointer_h

template <class T>
class Pointer
{
public:
	Pointer()
	{
		Data=NULL;
	}
	Pointer(long Size)
	{
		Data=new huge T[Size];
	}
	Pointer(T huge *D)
	{
		Data=D;
	}
	~Pointer()
	{
		Free();
	}
	void Free()
	{
		if (Data) delete[] Data;
		Data=NULL;
	}
	void Alloc(long Size)
	{
		Free();
		Data=new huge T[Size];
	}
	void Copy(T huge *D,int n);
	void MemCopy(T huge *D,int n) { memcpy(Data,D,n*sizeof(T)); }
	void CopyTo(T huge *D,int n);

	T huge *Data;
	operator T huge *() { return Data; }
//if T isnt char

//	operator unsigned char *() { return Data; }
	Pointer &operator =(T huge *NewData)
	{
		Free();
		Data=NewData;
		return *this;
	}
	Pointer &operator =(Pointer &P)
	{
		Free();
		Data=P.Data;
		P.Data=NULL;
		return *this;
	}
	T &operator[](long i) { return Data[i]; };
	T &operator[](int i) { return Data[i]; };
};

template <class T> void Pointer<T>::Copy(T huge *D,int n)
{
	Alloc(n);
	T *Ptr=Data;
	for (int i=0;i<n;i++) *Ptr++=*D++;
}
template <class T> void Pointer<T>::CopyTo(T huge *D,int n)
{
	T *Ptr=Data;
	for (int i=0;i<n;i++) *D++=*Ptr++;
}

#endif

