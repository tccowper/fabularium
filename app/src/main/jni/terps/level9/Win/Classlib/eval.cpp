// evaluation routines
#include <mywin.h>
#pragma hdrstop

#include <math.h>
#include <stdio.h>

#include "eval.h"


enum {TERM=-2,NONE=-1,LOR,LAND,REL,EQU,ADDI,MULT,UOP};
enum {BRAK=-1,ADD,SUB,MUL,DIV,LE,LT,GE,GT,EQ,NE,SIN,COS,ATN,ACS,ASN,SQRT,AND,OR,UPL,UMN,MOD};

EvalParse::uOps EvalParse::UnaryOps[]=
{
	{"cos",COS,UOP},
	{"sin",SIN,UOP},
	{"atan",ATN,UOP},
	{"acos",ACS,UOP},
	{"asin",ASN,UOP},
	{"sqrt",SQRT,UOP},
	{"+",UPL,UOP},
	{"-",UMN,UOP},
	{"(",BRAK,NONE},
};

EvalParse::bOps EvalParse::BinaryOps[]=
{
	{"+",1,ADD,ADDI},
	{"-",1,SUB,ADDI},
	{"*",1,MUL,MULT},
	{"/",1,DIV,MULT},
	{"=",1,EQ,EQU},
	{"<=",2,LE,REL},  // put before <
	{"<>",2,NE,EQU},	// put before <
	{"<",1,LT,REL},
	{">=",2,GE,REL},	// put before >
	{">",1,GT,REL},
	{")",1,BRAK,NONE},
	{",",1,BRAK,TERM},
	{"and",3,AND,LAND},
	{"or",2,OR,LOR},
	{"mod",3,MOD,ADDI}
};

BOOL EvalParse::UnaryOp(int &Op,int &Prec)
{
	for (int i=0;i<UOPS;i++)
		if (stricmp(UnaryOps[i].Name,Com)==0)
		{
			Op=UnaryOps[i].op;
			Prec=UnaryOps[i].Prec;
			return TRUE;
		}
	return FALSE;
}

void EvalParse::BinaryOp(int &Op, int &Prec)
{
	SkipSpace();
	for (int i=0;i<BOPS;i++)
		if (strnicmp(BinaryOps[i].Name,(char*)Ptr,BinaryOps[i].Len)==0)
		{
			Op=BinaryOps[i].op;
			Prec=BinaryOps[i].Prec;
			Ptr+=BinaryOps[i].Len;
			return;
		}
	Prec=TERM;
	if (c==';' || c<' ') Eol=TRUE;
}

double EvalParse::EvalTerm()
{
	double d=0;
	if (!ID::EvalDouble(Com,d))
	{
		int i;
		if (ID::EvalInt(Com,i))
			d=i;
			else if (sscanf(Com,"%lf",&d)!=1) Error=ERR_UNKNOWNID;
	}
	return d;
}

double EvalParse::Evaluate()
{
	int RightOp=NONE,RightPrec=NONE;
	double RightTerm=0;

	SubEval(RightOp,RightPrec,RightTerm);
	return RightTerm;
}

void EvalParse::SubEval(int &LeftOp,int &LeftPrec,double &LeftTerm)
{
	int RightOp,RightPrec;
	double RightTerm=0;

	GetCom(); // get term to delimiter
	PutBackC(); // put back delimiter
	if (Com.Empty() && strchr("+-(",c)) Com+=GetC();

	if (UnaryOp(RightOp,RightPrec))
	{
		SubEval(RightOp,RightPrec,RightTerm);
		if (RightPrec==NONE) BinaryOp(RightOp,RightPrec); // close brak
	}
	else
	{
		RightTerm=EvalTerm();
		BinaryOp(RightOp,RightPrec);
	}

	while (LeftPrec<RightPrec) SubEval(RightOp,RightPrec,RightTerm);

	switch (LeftOp)
	{
		case ADD:LeftTerm+=RightTerm;break;
		case SUB:LeftTerm-=RightTerm;break;
		case MUL:LeftTerm*=RightTerm;break;
		case DIV:LeftTerm/=RightTerm;break;
		case SIN:LeftTerm=sin(RightTerm);break; //takes radians
		case COS:LeftTerm=cos(RightTerm);break;
		case ATN:LeftTerm=atan(RightTerm);break;  //returns radians
		case ACS:LeftTerm=acos(RightTerm);break;
		case ASN:LeftTerm=asin(RightTerm);break;
		case SQRT:LeftTerm=sqrt(RightTerm);break;
		case LE:LeftTerm=(LeftTerm<=RightTerm);break;
		case LT:LeftTerm=(LeftTerm<RightTerm);break;
		case EQ:LeftTerm=(LeftTerm==RightTerm);break;
		case NE:LeftTerm=(LeftTerm!=RightTerm);break;
		case GT:LeftTerm=(LeftTerm>RightTerm);break;
		case GE:LeftTerm=(LeftTerm>=RightTerm);break;
		case AND:LeftTerm=((int) LeftTerm & (int) RightTerm);break;
		case OR :LeftTerm=((int) LeftTerm | (int) RightTerm);break;
		case UPL:LeftTerm=RightTerm;break;
		case UMN:LeftTerm=-RightTerm;break;
		case MOD:LeftTerm=((int) LeftTerm % (int) RightTerm);break;
		default:
			LeftTerm=RightTerm;
	}
	LeftOp=RightOp;
	LeftPrec=RightPrec;
}

int EvalParse::GetInt()
{
	Error=ERR_NONE;
	return (int) Evaluate();
}

double EvalParse::GetDouble()
{
	Error=ERR_NONE;
	return Evaluate();
}

