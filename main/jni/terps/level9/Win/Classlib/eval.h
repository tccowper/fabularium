// eval2.h

#include <parse.h>

enum {ERR_NONE,ERR_UNKNOWNID};

class EvalParse : public Parse
{
public:
	EvalParse(bhp Buf, long Size) : Parse(Buf,Size) {}
	double Evaluate();
	int GetInt();
	double GetDouble();
	int Error;

private:
	double EvalTerm();
	void SubEval(int &LeftOp,int &LeftPrec,double &LeftTerm);
	BOOL UnaryOp(int &Op,int &Prec);
	void BinaryOp(int &Op,int &Prec);

	#define UOPS 9
	#define BOPS 15
	static struct uOps
	{
		char *Name;
		int op;
		int Prec;
	} UnaryOps[UOPS];
	static struct bOps
	{
		char *Name;
		int Len;
		int op;
		int Prec;
	} BinaryOps[BOPS];
};

inline double Evaluate(char *Str)
{
	return EvalParse((bhp)Str,strlen(Str)).Evaluate();
}

