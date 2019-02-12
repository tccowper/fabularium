// unit edit

enum {U_FIXED,U_SCI,U_TIME,U_METRIC,U_CUSTOM};

struct Unit
{
	char *UnitStr;
	double Modifier;
};

class UnitEdit : public Edit {
public:
	UnitEdit(Object *Parent,int Id,double Value,int iUnit,char *Unit="",long Flags=0,double Min=0,double Max=0);
	UnitEdit(Object *Parent,int Id,double Value,Unit *U,long Flags=0,double Min=0,double Max=0);
	virtual ~UnitEdit();
	double GetValue(BOOL *err=NULL);
	virtual BOOL WndProc(TMSG &);

	long FLAGS;
	double MIN,MAX;
	char *Units;
	int UnitID;
	Unit *UnitTable;
	BOOL FindUnit(char *S, double &x);
	void Express(double Value);
	virtual BOOL CanClose();
};
