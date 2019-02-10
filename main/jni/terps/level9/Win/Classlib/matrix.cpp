#include <math.h>

#include <matrix.h>

Matrix::Matrix()
{
	n=3;
	Data=new double[n*n];
}

Matrix::Matrix(int m)
{
	n=m;
	Data=new double[n*n];
}

Matrix::~Matrix()
{
	delete[] Data;
}

void Matrix::ReSize(int m)
{
	if (m!=n)
	{
		delete[] Data;
		n=m;
		Data=new double[n*n];
	}
}

Matrix &Matrix::operator=(int v)
{
	for(int i=0;i<n;i++)
		for(int j=0;j<n;j++)
		{
			(*this)[i][j]=(i==j) ? v : 0;
		}
	return *this;
}

Matrix Matrix::Temp;

Matrix &Matrix::operator *(Matrix &M)
{
	int i,j,k;
	Temp.ReSize(n);
	double t;
	for (i=0;i<n;i++)
		for (j=0;j<n;j++)
		{
			t=0;
			for (k=0;k<n;k++)	t+=(*this)[k][j]*M[i][k];
			Temp[i][j]=t;
		}
	return Temp;
}

Matrix &Matrix::operator *(double v)
{
	int i,j;
	Temp.ReSize(n);
	for (i=0;i<n;i++)
		for (j=0;j<n;j++)
			Temp[i][j]=(*this)[i][j]*v;
	return Temp;
}

Matrix &Matrix::operator +(Matrix &M)
{
	int i,j;
	Temp.ReSize(n);
	for (i=0;i<n;i++)
		for (j=0;j<n;j++)
			Temp[i][j]=(*this)[i][j]+M[i][j];
	return Temp;
}

Vector &Matrix::operator*(Vector &V)
{
	int j,k;
	double t;

	for (j=0;j<n;j++)
	{
		t=0;
		for (k=0;k<3;k++)	t+=(*this)[k][j]*V[k];
		Vector::Temp[j]=t;
	}
	return Vector::Temp;
}

// ***************************************************

Vector::Vector()
{
	Data=new double[n=3];
}

Vector::Vector(int m)
{
	Data=new double[n=m];
}

Vector::Vector(double x,double y,double z)
{
	Data=new double[n=3];
	Data[0]=x;
	Data[1]=y;
	Data[2]=z;
}

Vector::~Vector()
{
	delete[] Data;
}

void Vector::ReSize(int m)
{
	if (m!=n)
	{
		delete[] Data;
		Data=new double[n=m];
	}
}

double Vector::Length()
{
	double l=0;
	for (int i=0;i<n;i++) l+=Data[i]*Data[i];
	return sqrt(l);
}

Vector& Vector::operator=(double v)
{
	for (int i=0;i<n;i++) Data[i]=v;
	return *this;
}

Vector &Vector::operator=(Vector &v)
{
	for (int i=0;i<n;i++) Data[i]=v[i];
	return *this;
}

Vector Vector::Temp;

Vector &Vector::operator+(Vector &v)
{
	Temp.ReSize(n);
	for (int i=0;i<n;i++) Temp.Data[i]=Data[i]+v[i];
	return Temp;
}

Vector &Vector::operator*(double v)
{
	Temp.ReSize(n);
	for (int i=0;i<n;i++) Temp.Data[i]=Data[i]*v;
	return Temp;
}

