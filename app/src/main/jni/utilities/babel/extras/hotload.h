/* hotload.h : The babel registry hotloader header
  L. Ross Raszewski
  This code is freely usable for all purposes.
 
  This work is licensed under the Creative Commons Attribution 4.0 International (CC BY 4.0).
  To view a copy of this license, visit
  http://creativecommons.org/licenses/by/4.0/ or send a letter to
  Creative Commons,
  543 Howard Street, 5th Floor,
  San Francisco, California, 94105, USA.
 
*/

#include "treaty.h"

typedef TREATY (*load_treaty)(char *, void *);

int babel_hotload(char *, char *, load_treaty, void *, void *);

