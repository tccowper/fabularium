/*----------------------------------------------------------------------*\

				LST.C
			Generic lists handling

\*----------------------------------------------------------------------*/

#include "lst_x.h"


/* IMPORT */
#include <stdio.h>

#include "util.h"


/* PUBLIC DATA */


/*======================================================================

  insert()

  Insert an element into a list at the point. Can not insert at end
  (i.e. on NULL lists)

  */
void insert(List *thePoint, void *element, ListKind kind)
{
  List *newListNode;

  if (thePoint == NULL)
    syserr("Inserting an element in a NULL list!", NULL);
  if (kind != thePoint->kind)
    syserr("Inserting wrong kind of element in list!", NULL);

  /* Move the first element to a new list node */
  newListNode = concat(NULL, thePoint->element.atr, kind);

  newListNode->next = thePoint->next;
  thePoint->element.atr = element;
  thePoint->next = newListNode;
}



List *tailOf(List *aList)
{
  List *tail;

  if (aList == NULL)
    return NULL;

  for (tail = aList; tail->next != NULL; tail = tail->next)
    ;
  return tail;
}


/*======================================================================*/
List *concat(List *list,	/* IN - List to concat to */
	     void *element,	/* IN - Pointer to any element type */
	     ListKind kind)	/* IN - Which kind of list? */
{
  List *new;			/* The newly created list node */
  List *tail;			/* Traversal pointer to find the tail */

  if (element == NULL) return(list);

  new = NEW(List);

  new->element.cla = (struct ClaNod *) element;
  new->kind = kind;

  new->next = NULL;
  if (list == NULL) {
    return(new);
  } else {
    tail = tailOf(list);
    tail->next = new;	/* Concat at end of list */
    return(list);
  }
}



/*======================================================================

  combine()

  Generic list combination.

  */
List *combine(List *list1,	/* IN - Lists to combine */
	      List *list2)
{
  List *tail = tailOf(list1);

  if (list1 == NULL) return(list2);
  if (list2 == NULL) return(list1);

  tail->next = list2;	/* Combine at end of list1 */
  return(list1);
}


/*======================================================================*/
int length(List *aList)
{
  int count = 0;
  List *thePoint;

  for (thePoint = aList; thePoint != NULL; thePoint = thePoint->next)
    count ++;
  return count;
}


/*----------------------------------------------------------------------*/
static List *removeFromList(List *theList, List *theElement)
{
  if (theList == NULL)		/* No list */
    return NULL;
  else if (theList == theElement) { /* First element */
    List *theRest = theElement->next;
    theElement->next = NULL;
    return theRest;
  } else {
    List *sentinel = theList;
    while (sentinel->next != theElement && sentinel->next != NULL)
      sentinel = sentinel->next;
    if (sentinel->next != NULL) {
      List *foundElement = sentinel->next;
      sentinel->next = sentinel->next->next;
      foundElement->next = NULL;
    }
  }
  return theList;
}    


/*======================================================================*/
List *sortList(List *theList, int compare(List *element1, List *element2))
{
  List *unsorted = theList;
  List *sorted = NULL;
  List *candidate;

  if (!compare) return theList;

  while (unsorted) {
    List *current = unsorted;
    candidate = unsorted;
    while (current) {
      if (compare(current, candidate) < 0)
	candidate = current;
      current = current->next;
    }
    unsorted = removeFromList(unsorted, candidate);
    if (sorted == NULL)
      sorted = candidate;
    else {
      List *tail = sorted;
      while (tail->next) tail = tail->next;
      tail->next = candidate;
      candidate->next = NULL;
    }
  }
  return sorted;
}
