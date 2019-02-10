// realedit.h

class RealEdit : public Edit {
public:
	RealEdit::RealEdit(Object *Parent,int Id,double Value,int Decs=3,long Flags=0,double Min=0,double Max=0);
	double GetValue(BOOL *err=NULL);
	virtual BOOL CanClose();
private:
	long FLAGS;
	double MIN,MAX;
};
