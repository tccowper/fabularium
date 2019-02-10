/*----------------------------------------------------------------------*\

                                TYPE.C
			     Type Handling

\*----------------------------------------------------------------------*/

#include "type_x.h"

#include "dump_x.h"
#include "util.h"
#include "srcp_x.h"


/*======================================================================*/
Bool isComplexType(TypeKind type) {
  return type == SET_TYPE || type == INSTANCE_TYPE;
}

/*======================================================================*/
char *typeToString(TypeKind type)
{
  switch (type) {
  case BOOLEAN_TYPE: return "Boolean"; break;
  case INTEGER_TYPE: return "Integer"; break;
  case STRING_TYPE: return "String"; break;
  case REFERENCE_TYPE: return "Reference"; break;
  case INSTANCE_TYPE: return "Instance"; break;
  case EVENT_TYPE: return "Event"; break;
  case SET_TYPE: return "Set"; break;
  case ERROR_TYPE: return "ERROR"; break;
  case UNINITIALIZED_TYPE: return "UNINITIALIZED"; break;
  }
  return "***ERROR: Unexpected type kind***";
}


/*======================================================================*/
Bool equalTypes(TypeKind typ1, TypeKind typ2)
{
    if (typ1 == UNINITIALIZED_TYPE || typ2 == UNINITIALIZED_TYPE)
        SYSERR("Unintialised type", nulsrcp);
    if (typ1 == REFERENCE_TYPE) typ1 = INSTANCE_TYPE;
    if (typ2 == REFERENCE_TYPE) typ2 = INSTANCE_TYPE;
    return (typ1 == ERROR_TYPE || typ2 == ERROR_TYPE || typ1 == typ2);
}


/*======================================================================*/
void dumpType(TypeKind type)
{
  put(typeToString(type));
}


