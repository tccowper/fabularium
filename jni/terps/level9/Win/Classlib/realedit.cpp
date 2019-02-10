// RealEdit ***********************************************

#include <mywin.h>
#pragma hdrstop

#include <string.h>
#include <stdio.h>
#ifdef __BORLANDC__
#include <values.h>
#else
#include <limits.h>
#endif

RealEdit::RealEdit(Object *Parent,int Id,double Value,int Decs,long Flags,double Min,double Max) : Edit(Parent,Id)
{
	FLAGS=Flags;
	MIN=Min;
	MAX=Max;
	char temp[20];
	sprintf(temp,"%.*g",Decs,Value);
	SetString(temp);
}

BOOL RealEdit::CanClose()
{
	char temp[128]="";
	int err=0;
	double Value=GetValue(&err);
	if (err) strcpy(temp,"Please enter a valid number");
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

double RealEdit::GetValue(BOOL *err)
{
	double Value;
	char *temp=GetString();
	if (sscanf(temp,"%lf",&Value)<=0 && err) *err=1;
	delete[] temp;
	return Value;
}

