// Radio Group ****************************************

#include <mywin.h>
#pragma hdrstop

RadGrp::RadGrp(Object *Parent,int ID_First,int ID_Last,int Value) : Object (Parent)
{
	ID_FIRST=ID_First;
	ID_LAST=ID_Last;
	SetValue(Value);
}

void RadGrp::SetValue(int Value)
{
	CheckRadioButton(ParentHWnd,ID_FIRST,ID_LAST,ID_FIRST+Value);
}

BOOL RadGrp::Create()
{ // Multiple hWnds ???
	return TRUE;
}


int RadGrp::GetValue() {
int i;
for (i=ID_FIRST;i<=ID_LAST;i++) if (IsDlgButtonChecked(ParentHWnd,i))
	break;
return i-ID_FIRST;
}
