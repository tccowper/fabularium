#include <mywin.h>
#pragma hdrstop

#include <string.h>
#include <stdio.h>
#include <math.h>

#ifdef __BORLANDC__
#include <values.h>
#else
#include <limits.h>
#endif

Unit SciUnit[]={
	{"T",	1e12},
	{"G",	1e9 },
	{"M",	1e6 },
	{"k",	1000 },
	{"",	1.00 },
	{"m",	1e-3 },
	{"u",	1e-6 },
	{"n",	1e-9 },
	{"p",	1e-12},
	{"f",	1e-15},
	{"a",	1e-18},
	{"",	0}},

	TimeUnit[]={
	{"h",  3600 },
	{"hr", 3600 },
	{"m",  60   },
	{"min",60   },
	{"s",  1.00 },
	{"ms", 1e-3 },
	{"us", 1e-6 },
	{"",	0}},

	MetricUnit[]={
	{"km", 1000 },
	{"m",	 1.00 },
	{"cm", 1e-2 },
	{"mm", 1e-3 },
	{"um", 1e-6 },
	{"nm", 1e-9 },
	{"",	0}};

Unit *UTab[]={NULL,SciUnit,TimeUnit,MetricUnit};

UnitEdit::UnitEdit(Object *Parent,int Id,double Value,int iUnit,char *Unit,long Flags,double Min,double Max)
	: Edit(Parent,Id)
{
	Units=strdup(Unit);
	UnitID=iUnit;
	UnitTable=UTab[UnitID];
	FLAGS=Flags;
	MIN=Min;
	MAX=Max;
	Express(Value);
}

UnitEdit::UnitEdit(Object *Parent,int Id,double Value,Unit *U,long Flags,double Min,double Max)
	: Edit(Parent,Id)
{
	Units=strdup("");
	UnitID=U_CUSTOM;
	UnitTable=U;
	FLAGS=Flags;
	MIN=Min;
	MAX=Max;
	Express(Value);
}

BOOL UnitEdit::FindUnit(char *S, double &x)
{
	if (*S==0) return TRUE;
	char temp[10];

	for (	Unit *U=UnitTable ; U->Modifier!=0 ; U++)
	{
		// add U->UnitStr to Units
		strcpy(temp,U->UnitStr);
		strcat(temp,Units);
		if (!strcmp(temp,S))
		{
			x= U->Modifier>0 ? U->Modifier*x : -U->Modifier/x;
			return TRUE;
		}
	}
	return FALSE;
}

// express Value with Given units
void UnitEdit::Express(double Value)
{
	// validate Value, so scroll bar doesnt go beyond range
	if (Value<MIN && (FLAGS & NE_MIN)) Value=MIN;
	else if (Value>MAX && (FLAGS & NE_MAX)) Value=MAX;
	// could do some more validation

	double tempr;
	char Str[20],temp[10];
	if (UnitID==U_FIXED)
	{
		strcpy(temp,Units);
		tempr=Value;
	}
	else
	{
		Unit *U,*U2;
		for (U=UnitTable,U2=NULL;U->Modifier!=0;U++)
		{
			if (U->Modifier>0) // do not use reciprocal units
			{
				tempr=fabs(Value/U->Modifier);
				if ((U->Modifier==1.0 && !U2) || (tempr>=0.1 && tempr<100)) U2=U;
			}
		}
		if (U2==NULL) U2=UnitTable; // no 1.00 modifier and no match

		// in case reciprocal unit is first on list
		tempr= U2->Modifier>0 ? Value/U2->Modifier : -1/(Value*U2->Modifier);
		strcpy(temp,U2->UnitStr);
		strcat(temp,Units);
	}

	sprintf(Str,"%.4g",tempr);

	if (*temp)
	{
		strcat(Str," ");
		strcat(Str,temp);
	}
	SetString(Str);
}

UnitEdit::~UnitEdit()
{
	delete[] Units;
}

enum {U_INVALIDNUMBER=1,U_WRONGUNITS};

BOOL UnitEdit::CanClose()
{
	char temp[128]="";
	int err;
	double Value=GetValue(&err);
	if (err==U_INVALIDNUMBER) strcpy(temp,"Please enter a valid number");
	else if (err==U_WRONGUNITS) strcpy(temp,"Unknown units");
	else if ((Value<MIN && (FLAGS & NE_MIN) && !(FLAGS & NE_MAX)))
		sprintf(temp,"Please enter a number above %lf",MIN);
	else if ((Value>MAX && (FLAGS & NE_MAX) && !(FLAGS & NE_MIN)))
		sprintf(temp,"Please enter a number below %lf",MAX);
	else if ((( Value<MIN || Value>MAX ) && (FLAGS & NE_MINMAX)))
		sprintf(temp,"Please enter a number between %lf and %lf",MIN,MAX);
	else if (Value<0 && (FLAGS & NE_POS))
		strcpy(temp,"Please enter a positive number");
	else if (Value>0 && (FLAGS & NE_NEG))
		strcpy(temp,"Please enter a negative number");
	else if (Value==0 && (FLAGS & NE_NONZERO))
		strcpy(temp,"Please enter a non-zero number");
	if (*temp)
	{
		// handle needs to be that of window not control
		MessageBox(ParentHWnd,temp,"Error",MB_OK | MB_ICONEXCLAMATION);
		SendMessage(hWnd, EM_SETSEL,0,MAXINT);
		SetFocus(hWnd);
		return FALSE;
	}
	return TRUE;
}

double UnitEdit::GetValue(BOOL *err)
{
	double Value;
	char Extra[20]="";
	int error=0;
	char *temp=GetString();
	if (sscanf(temp,"%lf%s",&Value,Extra)<=0) error=U_INVALIDNUMBER;
	// search for Extra modifier
	if (UnitID==U_FIXED)
	{
		if (*Extra && strcmp(Extra,Units)) error=U_WRONGUNITS;
	}
	else
	{
		if (!FindUnit(Extra,Value)) error=U_WRONGUNITS;
	}

	if (err) *err=error;
	delete[] temp;
	return Value;
}

BOOL UnitEdit::WndProc(TMSG &Msg)
{
	if (Msg.Msg==WM_VSCROLL)
	{
		switch (Msg.wParam)
		{
		case SB_LINEUP:
			if (CanClose())
			{
				double V=GetValue();
				Express(V+pow(floor(log10(V))-2,10));
			}
			break;
		case SB_LINEDOWN:
			if (CanClose())
			{
				double V=GetValue();
				Express(V-pow(floor(log10(V))-2,10));
			}
			break;
		}
	}

	return ((Msg.RetVal=DefProc(hWnd,Msg.Msg,Msg.wParam,Msg.lParam))==0);
}

