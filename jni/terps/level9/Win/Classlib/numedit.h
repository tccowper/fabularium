// NumEdit.h

//NumEdit Flags
#define NE_MIN 1
#define NE_MAX 2
#define NE_MINMAX 3
#define NE_POS 4
#define NE_NEG 8
#define NE_NONZERO 16
#define NE_POWER2 32
#define NE_DIVBY	64
#define NE_HEX 128

class NumEdit : Edit {
public:
	NumEdit::NumEdit(Object *Parent,int Id,long Value,long Flags=0,long Min=0,long Max=0,int Div=1);
	long GetValue(BOOL *err=NULL);
	void SetValue(long Value);
	void SetRange(long Min,long Max);
	virtual BOOL CanClose();
private:
	int DivBy;
	long FLAGS,MIN,MAX;
};
