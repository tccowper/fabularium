/*======================================================================*\

  claTest.c

\*======================================================================*/

#include "cla.c"

#include <cgreen/cgreen.h>

#include "adv.h"
#include "ins_x.h"
#include "emit.h"

#include "unitmock.h"
#include "unitList.h"


Describe(Class);
BeforeEach(Class) {}
AfterEach(Class) {}


Ensure(Class, testCreateClass) {
  Srcp srcp = {1,2,3};
  Id *id = newId(srcp, "claId");
  Id *parent = newId(srcp, "parentId");

  /* Create a class with unknown inheritance */
  Class *cla = newClass(&srcp, id, parent, NULL);

  assert_true(equalSrcp(cla->srcp, srcp));
  assert_true(equalId(cla->props->id, id));
  assert_true(equalId(cla->props->parentId, parent));

  symbolizeClasses();
  assert_true(readEcode() == 310 && readSev() == sevERR);

  /* Add the inheritance id, resymbolize */
  (void) newInstance(&srcp, parent, NULL, NULL);
  symbolizeClasses();
  assert_true(readEcode() == 350 && readSev() == sevERR);
}


Ensure(Class, testGenerateClasses) {
  Srcp srcp = {12,13,14};
  Aaddr addr;
  int firstAdr = AwordSizeOf(ACodeHeader);
  static int NOOFPREDEFEINEDCLASSES = 8;
  static int classSize = AwordSizeOf(ClassEntry);
  int baseAddress = firstAdr + NOOFPREDEFEINEDCLASSES*classSize;

  initAdventure();

  initEmit("unit.a3c");
  symbolizeAdventure();
  addr = generateClasses();
  /* Table should start directly after header */
  assert_true(addr == firstAdr);
  /* header + PREDEFINED classes + 1 EOF should be generated*/
  assert_true(nextEmitAddress() == baseAddress + 1);

  initEmit("unit.a3c");
  symbolizeClasses();
  (void) newClass(&srcp, newId(srcp, "aSimpleClass"), NULL, NULL);
  addr = generateClasses();
  assert_true(addr == firstAdr);	/* Should start at first address after header */
  assert_true(nextEmitAddress() == baseAddress + classSize + 1);	/* (predefined+1) classes + EOF */
}

Ensure(Class, testGenerateEmptyClassEntry) {
  Properties *props = newProps(NULL,
			       NULL,
			       nulsrcp, NULL,
			       NULL,
			       NULL,
			       NULL,
			       nulsrcp, NULL, NULL, NULL, NULL,
			       NULL, NULL,
			       nulsrcp, NULL,
			       NULL,
			       NULL);
  Class *class = newClass(&nulsrcp, newId(nulsrcp, "aClass"), NULL, props);
  int entryAddress;
  ClassEntry *entry;
  
  initAdventure();
  initEmit("unit.a3c");
  symbolizeAdventure();

  generateCommonPropertiesData(class->props);
  entryAddress = nextEmitAddress();
  generateClassEntry(class);
  finalizeEmit();
  writeHeader(&acodeHeader);
  terminateEmit();

  loadACD("unit.a3c");
  entry = (ClassEntry *) &memory[entryAddress];
  assert_true(convertFromACD(entry->description) == 0);
  assert_true(convertFromACD(entry->parent) == 0);
}

TestSuite *claTests()
{
    TestSuite *suite = create_test_suite();

    add_test_with_context(suite, Class, testCreateClass);
    add_test_with_context(suite, Class, testGenerateClasses);
    add_test_with_context(suite, Class, testGenerateEmptyClassEntry);

    return suite;
}

