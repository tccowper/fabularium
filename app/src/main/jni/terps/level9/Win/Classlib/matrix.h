// matrix stuff

#include <string.h>

class Vector
{
//friend Matrix;
public:
	double *Data;
	int n;
	static Vector Temp;


	Vector();
	Vector(int);
	Vector(double x,double y,double z);
	~Vector();
	void ReSize(int m);
	double Length();
	Vector &operator=(double);
	Vector &operator=(Vector &);
	Vector &operator+(Vector &);
	Vector &operator*(double);
	double &operator[](int i)
	{
		return Data[i];
	}

};

class Matrix
{
private:
	double *Data;
	int n;
	static Matrix Temp;

public:
	Matrix();
	Matrix(int);
	~Matrix();
	void ReSize(int m);
	Matrix &operator=(Matrix&M)
	{
		memcpy(Data,M.Data,n*n*sizeof(double));
		return *this;
	}
	Matrix &operator=(int);
	Matrix &operator*(Matrix &);
	Matrix &operator*(double);
	Matrix &operator+(Matrix &);
	Vector &operator*(Vector &V);

	double *operator[](int j)
	{
		return Data+j*n;
	}

};

