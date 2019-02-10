/////////////////////////////////////////////////////////////////////////////
//
// Magnetic 2
// Magnetic Scrolls Interpreter
//
// Visual C++ MFC Windows interface by David Kinder
//
// MagneticDoc.h: Document class
//
/////////////////////////////////////////////////////////////////////////////

#if _MSC_VER >= 1000
#pragma once
#endif // _MSC_VER >= 1000

class CMagneticDoc : public CDocument
{
protected: // create from serialization only
  CMagneticDoc();
  DECLARE_DYNCREATE(CMagneticDoc)

// Overrides
  // ClassWizard generated virtual function overrides
  //{{AFX_VIRTUAL(CMagneticDoc)
  public:
  virtual BOOL OnOpenDocument(LPCTSTR lpszPathName);
  //}}AFX_VIRTUAL

// Implementation
public:
  virtual ~CMagneticDoc();
#ifdef _DEBUG
  virtual void AssertValid() const;
  virtual void Dump(CDumpContext& dc) const;
#endif

// Generated message map functions
protected:
  //{{AFX_MSG(CMagneticDoc)
  //}}AFX_MSG
  DECLARE_MESSAGE_MAP()
};
