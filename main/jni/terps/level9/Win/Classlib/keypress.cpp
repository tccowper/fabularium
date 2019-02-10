// keypress
#include <mywin.h>
#pragma hdrstop
#include <mmsystem.h>

#include <keypress.h>

// Key Press Dialog **************************************

EV_START(KeyPressDialog)
	EV_MESSAGE(WM_SYSKEYDOWN, WMSysKeyDown)
	EV_MESSAGE(WM_KEYDOWN, WMKeyDown)
	EV_MESSAGE(WM_KILLFOCUS, WMKillFocus)
	EV_MESSAGE(WM_LBUTTONDOWN, WMLButtonDown)
	EV_MESSAGE(WM_MBUTTONDOWN, WMMButtonDown)
	EV_MESSAGE(WM_RBUTTONDOWN, WMRButtonDown)
	EV_MESSAGE(WM_TIMER, WMTimer)
EV_END

KeyPressDialog *Dlg;

KeyPressDialog::KeyPressDialog(Object *Parent,int ID,int *K,int JID) : Window(Parent,"Press a Key/Button")
{
	Jid=JID;
	Key=K;
	//Scan=0;
	Id=ID;
	Style=WS_OVERLAPPED | WS_VISIBLE;
	HWND hw=GetDlgItem(Parent->hWnd,ID);
	RECT rc;
	GetWindowRect(hw,&rc);
	x=(rc.right+rc.left)/2-100;
	y=(rc.top+rc.bottom)/2-20;
	w=200;
	h=40;
}

BOOL KeyPressDialog::SetupWindow()
{
	SetCapture(hWnd);
	if (Jid>=0)	SetTimer(hWnd,0,100,NULL);
	return TRUE;
}

/*
void SetKeyScanText(HWND hWnd,int ID,long Scan)
{
	char temp[20];
	GetKeyNameText(Scan,temp,20);
	SetDlgItemText(hWnd,ID,temp);
}
*/

void KeyPressDialog::Destroy()
{
	SetKeyText(Parent->hWnd,Id,*Key);
	//*Key=MapVirtualKey(Scan>>16,1);
	ReleaseCapture();
	if (Jid>=0) KillTimer(hWnd,0);
	delete Dlg;
}

BOOL KeyPressDialog::WMKillFocus(TMSG&)
{
	DestroyWindow();
	return TRUE;
}

BOOL KeyPressDialog::WMSysKeyDown(TMSG &Msg)
{	// gets alt and F10
	//Scan=lParam;
	*Key=Msg.wParam;
	DestroyWindow();
	return TRUE;
}


BOOL KeyPressDialog::WMKeyDown(TMSG &Msg)
{
	//Scan=lParam;
	*Key=Msg.wParam;
	DestroyWindow();
	return TRUE;
}

BOOL KeyPressDialog::WMLButtonDown(TMSG&)
{
	*Key=VK_LBUTTON;
	DestroyWindow();
	return TRUE;
}

BOOL KeyPressDialog::WMMButtonDown(TMSG&)
{
	*Key=VK_MBUTTON;
	DestroyWindow();
	return TRUE;
}

BOOL KeyPressDialog::WMRButtonDown(TMSG&)
{
	*Key=VK_RBUTTON;
	DestroyWindow();
	return TRUE;
}

BOOL KeyPressDialog::WMTimer(TMSG&)
{
	JOYINFO ji;
	joyGetPos(Jid,&ji);
	// avoid multiple bits
	if (ji.wButtons & JOY_BUTTON1) *Key=-JOY_BUTTON1;
	else if (ji.wButtons & JOY_BUTTON2) *Key=-JOY_BUTTON2;
	else if (ji.wButtons & JOY_BUTTON3) *Key=-JOY_BUTTON3;
	else if (ji.wButtons & JOY_BUTTON4) *Key=-JOY_BUTTON4;
	if (ji.wButtons) DestroyWindow();
	return TRUE;
}

void SetKeyText(HWND hWnd,int ID,int Key)
{
	switch (Key) {
		case VK_LBUTTON:SetDlgItemText(hWnd,ID,"L Button");break;
		case VK_MBUTTON:SetDlgItemText(hWnd,ID,"M Button");break;
		case VK_RBUTTON:SetDlgItemText(hWnd,ID,"R Button");break;
		case -JOY_BUTTON1:SetDlgItemText(hWnd,ID,"JStick1");break;
		case -JOY_BUTTON2:SetDlgItemText(hWnd,ID,"JStick2");break;
		case -JOY_BUTTON3:SetDlgItemText(hWnd,ID,"JStick3");break;
		case -JOY_BUTTON4:SetDlgItemText(hWnd,ID,"JStick4");break;
		default:
			char temp[20];
			Key=MapVirtualKey(Key,0);
			GetKeyNameText((long) Key<<16,temp,20);
			SetDlgItemText(hWnd,ID,temp);
			break;
		}
}

