/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// MagneticDoc.cpp: Document class
//
/////////////////////////////////////////////////////////////////////////////

#include "StdAfx.h"

#include "Magnetic.h"
#include "MagneticDoc.h"
#include "MagneticView.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// Implementation of CMagneticDoc
/////////////////////////////////////////////////////////////////////////////

IMPLEMENT_DYNCREATE(CMagneticDoc, CDocument)

BEGIN_MESSAGE_MAP(CMagneticDoc, CDocument)
  //{{AFX_MSG_MAP(CMagneticDoc)
  //}}AFX_MSG_MAP
END_MESSAGE_MAP()

CMagneticDoc::CMagneticDoc()
{
}

CMagneticDoc::~CMagneticDoc()
{
}

BOOL CMagneticDoc::OnOpenDocument(LPCTSTR lpszPathName) 
{
  if (!CDocument::OnOpenDocument(lpszPathName))
    return FALSE;

  return CMagneticView::OpenGame(lpszPathName);
}

#ifdef _DEBUG
void CMagneticDoc::AssertValid() const
{
  CDocument::AssertValid();
}

void CMagneticDoc::Dump(CDumpContext& dc) const
{
  CDocument::Dump(dc);
}
#endif //_DEBUG
