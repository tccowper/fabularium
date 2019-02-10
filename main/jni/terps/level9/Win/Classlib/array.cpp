// array stuff

template <class T>

class Array
{
public:
	Array()
	{
		Data=NULL;
		Dead=Used=Size=0;
	}
	Array(int n)
	{
		Data=new T*[Size=n];
		for (int i=0;i<n;i++) Data[i]=NULL;
		Dead=Used=0;
	}
	T* AddEnd()
	{
		if (Used==Size) Resize();
		return Data+Used++;
	}
	T *Add()
	{
		if (Dead==0) return AddEnd();
		else
		{
			T *A=Data;

			while (*A) A++;
			return A;
		}
	}
	void Delete(int i)
	{
		if (Data[i])
		{
			delete Data[i];
			Data[i]=NULL;
			if (i+1==Used) Used--;
			else Dead++;
		}
	}


	T **Data;
};

