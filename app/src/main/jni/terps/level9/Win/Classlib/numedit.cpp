// NumEdit ***********************************************

#include <mywin.h>
#pragma hdrstop

#include <string.h>
#include <stdio.h>
#ifdef __BORLANDC__
#include <values.h>
#else
#include <limits.h>
#endif

NumEdit::NumEdit(Object *Parent,int Id,long Value,long Flags,long Min,long Max,int Div) : Edit(Parent,Id)
{
	FLAGS=Flags;
	MIN=Min;
	MAX=Max;
	DivBy=Div;
	SetValue(Value);
}

void NumEdit::SetRange(long Min,long Max)
{
	MIN=Min;
	MAX=Max;
}

void NumEdit::SetValue(long Value)
{
	char temp[11];
	sprintf(temp,FLAGS & NE_HEX ? "%lX" : "%ld",Value);
	SetString(temp);
}

BOOL Pow2(unsigned long  x)
{
	if (x!=0) while ( (x&1)==0) x>>=1;
	return x==1;
}

BOOL NumEdit::CanClose()
{
	char temp[128]="";
	int err=0;
	long Value=GetValue(&err);
	if (err) strcpy(temp,"Please enter a valid number");
	else if ((Value<MIN && (FLAGS & NE_MIN) && !(FLAGS & NE_MAX)))
		sprintf(temp,"Please enter a number >= %ld",MIN);
	else if ((Value>MAX && (FLAGS & NE_MAX) && !(FLAGS & NE_MIN)))
		sprintf(temp,"Please enter a number <= %ld",MAX);
	else if ((( Value<MIN || Value>MAX ) && ((FLAGS & NE_MINMAX)==NE_MINMAX)))
		sprintf(temp,"Please enter a number %ld <= x <= %ld",MIN,MAX);
	else if (Value<0 && (FLAGS & NE_POS))
		strcpy(temp,"Please enter a positive number");
	else if (Value>0 && (FLAGS & NE_NEG))
		strcpy(temp,"Please enter a negative number");
	else if (Value==0 && (FLAGS & NE_NONZERO))
		strcpy(temp,"Please enter a non-zero number");
	else if ((FLAGS & NE_POWER2) && !Pow2(Value))
		strcpy(temp,"Please enter a power of 2");
	else if ( (FLAGS & NE_DIVBY) && (Value % DivBy!=0) ) {
		sprintf(temp,"Please enter a number divisible by %d",DivBy);
		}
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

long NumEdit::GetValue(BOOL *err)
{
	long Value;
	char *temp=GetString();
	if (err) *err=0;
	if (sscanf(temp,FLAGS & NE_HEX ? "%lX" : "%ld",&Value)<=0 && err) *err=1;
	delete[] temp;
	return Value;
}

