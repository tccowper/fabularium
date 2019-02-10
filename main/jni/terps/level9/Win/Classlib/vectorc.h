
#include <math.h>

template <class T>

class Vector
{
public:
	T x,y,z;

	Vector(T X,T Y,T Z) { x=X; y=Y; z=Z; }
	Vector(Vector &v) { x=v.x; y=v.y; z=v.z; }

	T Length2() { return sqrt(x*x+y*y+z*z); }
	T Length() { return sqrt(Length2()); }

	Vector& operator=(T v) { x=y=z=v; return *this; }
	Vector &operator=(Vector &v) { x=v.x; y=v.y; z=v.z; return *this; }

	Vector operator+(Vector &v) { return Vector<T>( x+v.x , y+v.y , z+v.z ); }
	Vector operator-(Vector &v) { return Vector<T>( x-v.x , y-v.y , z-v.z ); }
	Vector operator*(Vector &v) { return Vector<T>( y*v.z - z*v.y , z*v.x - x*v.z , x*v.y - y*v.x ); }

	Vector operator*(T t) { return Vector<T>(x*t,y*t,z*t); }
	Vector operator/(T t) { return Vector<T>(x/t,y/t,z/t); }

	Vector &operator+=(Vector &v) { x+=v.x; y+=v.y; z+=v.z; return *this; }
	Vector &operator-=(Vector &v) { x-=v.x; y-=v.y; z-=v.z; return *this; }
	Vector &operator*=(Vector &v)
	{
		T xtemp= y*v.z - z*v.y;
		T ytemp= z*v.x - x*v.z;
		z= x*v.y - y*v.x;
		x=xtemp;
		y=ytemp;
		return *this;
	}

	Vector &operator*=(T t) { x*=t; y*=t; z*=t; return *this; }
	Vector operator/=(T t) { x/=t; y/=t; z/=t; return *this; }

};

