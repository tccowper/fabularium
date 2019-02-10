// radgrp.h

class RadGrp : Object {
	int ID_FIRST;
	int ID_LAST;
public:
	RadGrp(Object *,int,int,int);
	int GetValue();
	void SetValue(int Value);
	BOOL Create();
};
