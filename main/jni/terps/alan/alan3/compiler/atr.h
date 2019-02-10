#ifndef _ATR_H_
#define _ATR_H_

/* USE other definitions */
#include "srcp.h"
#include "sym.h"
#include "id.h"
#include "exp.h"
#include "acode.h"


/* TYPES: */

typedef enum {
	UNKNOWN_INHERITANCE,
	LOCAL,
	INHERITED_REDEFINED,
	INHERITED
} AttributeInheritance;

typedef struct Attribute {
	Srcp srcp;			  /* Source position of the attribute */
	TypeKind type;		  /* Type of this attribute */
	Id *id;			  /* Id of the attribute */
	Bool readonly;		  /* Is the attribute a readonly attribute? */
	Symbol *definingSymbol;	/* Which entity originally defined it? */
	AttributeInheritance inheritance;
	Aword stringAddress;	/* Acode address to the attribute name */
	Aword address;			/* Acode address to the attribute value */
	Aint instanceCode;		/* Code of the owning instance, used
							   to generate string and set
							   initilization data */

	int value;				/* INTEGER - The initial value */

	long fpos;				/* STRING - initial value */
	long len;				/* STRING */
	Bool encoded;			/* STRING */

	Id *reference;		/* REFERENCE - initial value and class in the
							   symbol */
	Symbol *referenceClass;
	Bool initialized;		/* Is it initialized or only classified */

	Expression *set;		/* SET - An expression for the initial set */
	TypeKind setType;		/* SET - Type of elements in SET */
	Aaddr setAddress;		/* Address to the attributes initial set */

	Symbol *setClass;		/* Class of instance elements in SET attributes
							   and REFERENCE attributes */
} Attribute;

#endif
