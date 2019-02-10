#ifndef _TYPE_H_
#define _TYPE_H_
/*----------------------------------------------------------------------*\

				TYPE.H
			      Type Nodes

\*----------------------------------------------------------------------*/


/* USE other definitions */


/* Types: */

typedef enum TypeKind {		/* TYPE kinds */
    UNINITIALIZED_TYPE = 0,
    ERROR_TYPE,
    INTEGER_TYPE,
    BOOLEAN_TYPE,
    STRING_TYPE,
    REFERENCE_TYPE,           /* Not an actual type, abstract type for
                                 types allowed in reference attributes */
    INSTANCE_TYPE,
    EVENT_TYPE,
    SET_TYPE
} TypeKind;


#endif
