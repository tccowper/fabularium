/*======================================================================*\

  insTest.c

\*======================================================================*/

#include "ins.c"

#include <cgreen/cgreen.h>

#include "unitmock.h"
#include "unitList.h"

#include "adv_x.h"


Describe(Instance);
BeforeEach(Instance) {}
AfterEach(Instance) {}

Ensure(Instance, testCreateIns) {
  Srcp srcp = {1,2,3};
  Id *id = newId(srcp, "insId");
  Id *parent = newId(srcp, "parentId");
  Instance *ins;

  initAdventure();

  ins = newInstance(&srcp, id, parent, NULL);
  assert_true(equalSrcp(srcp, ins->srcp));
  assert_true(equalId(id, ins->props->id));
  assert_true(equalId(parent, ins->props->parentId));

  symbolizeInstance(ins);
  assert_true(readEcode() == 310 && readSev() == sevERR);
}


Ensure(Instance, testGenerateEmptyInstanceEntry) {
  Properties *props = newProps(NULL, NULL,
			       nulsrcp, NULL,
			       NULL, NULL, NULL,
			       nulsrcp, NULL, NULL, NULL, NULL,
			       NULL, NULL,
			       nulsrcp, NULL,
			       NULL, NULL);
  Instance *instance = newInstance(&nulsrcp, newId(nulsrcp, "aInstance"), NULL, props);
  int entryAddress;
  InstanceEntry *entry;
  
  initAdventure();
  initEmit("unit.a3c");
  symbolizeAdventure();

  generateInstancePropertiesData(instance->props);
  entryAddress = nextEmitAddress();
  generateInstanceEntry(instance);
  finalizeEmit();
  writeHeader(&acodeHeader);
  terminateEmit();

  loadACD("unit.a3c");
  entry = (InstanceEntry *) &memory[entryAddress];
  assert_true(convertFromACD(entry->description) == 0);
  assert_true(convertFromACD(entry->parent) == 0);
}

Ensure(Instance, testGenerateInstances) {
  Srcp srcp = {12,13,14};
  Instance *ins;
  Aaddr address;
  Aaddr instanceTableAddress;
  InstanceEntry *instanceTable;
  int firstAdr = sizeof(ACodeHeader)/sizeof(Aword);
  int instanceSize = sizeof(InstanceEntry)/sizeof(Aword);

  initAdventure();
  initEmit("unit.a3c");
  symbolizeAdventure();

  address = generateInstanceTable(allInstances);
  assert_true(address == firstAdr);
  address = nextEmitAddress();
  assert_true(address == firstAdr + instanceSize + 1/*EOF*/);

  initAdventure();
  initEmit("unit.a3c");
  ins = newInstance(&srcp, newId(srcp, "aSimpleInstance"), NULL, NULL);
  symbolizeInstances();
  generateInstanceData(ins);

  instanceTableAddress = nextEmitAddress();
  generateInstanceEntry(ins);

  /* End should be at the size of the table and one instance */
  address = nextEmitAddress();
  assert_true(address == instanceTableAddress + instanceSize);
  acodeHeader.size = address;
  finalizeEmit();
  writeHeader(&acodeHeader);
  terminateEmit();

  loadACD("unit.a3c");
  instanceTable = (InstanceEntry *) &memory[instanceTableAddress];
  assert_true(convertFromACD(instanceTable->code) == ins->props->id->symbol->code);
  assert_true(convertFromACD(instanceTable->id) == ins->props->idAddress);
  assert_true(convertFromACD(instanceTable->parent) == (ins->props->parentId?ins->props->parentId->symbol->code:0));
  assert_true(convertFromACD(instanceTable->initialAttributes) == ins->props->attributeAddress);
  assert_true(convertFromACD(instanceTable->checks) == checksAddressOf(ins->props->description));
  assert_true(convertFromACD(instanceTable->description) == doesAddressOf(ins->props->description));
  assert_true(convertFromACD(instanceTable->mentioned) == ins->props->mentionedAddress);
  assert_true(convertFromACD(instanceTable->exits) == ins->props->exitsAddress);
  assert_true(convertFromACD(instanceTable->verbs) == ins->props->verbsAddress);
}


Ensure(Instance, testHero) {
  ACodeHeader header;
  int count;
  int count2;
  Aword buffer[100];
  initEmitBuffer(buffer);

  assert_true(theHero == NULL);
  initAdventure();
  count = instanceCount;
  count2 = length(adv.inss);
  addHero(&adv);
  assert_true(theHero != NULL);
  assert_true(theHero->code != 0);
  assert_that(length(adv.inss), is_equal_to(count2+1));
  symbolizeAdventure();
  assert_true(inheritsFrom(theHero, lookup("actor")));
  generateInstances(&header);
  assert_true(header.theHero == count+1);
}


Ensure(Instance, testNowhereIsGenerated) {
  nowhere = NULL;
  initAdventure();
  assert_true(nowhere != NULL);
  assert_true(nowhere->code == NOWHERE);
  symbolizeAdventure();
  assert_true(inheritsFrom(nowhere, lookup("location")));
}


TestSuite *insTests()
{
    TestSuite *suite = create_test_suite(); 

    add_test_with_context(suite, Instance, testCreateIns);
    add_test_with_context(suite, Instance, testGenerateEmptyInstanceEntry);
    add_test_with_context(suite, Instance, testGenerateInstances);
    add_test_with_context(suite, Instance, testHero);
    add_test_with_context(suite, Instance, testNowhereIsGenerated);

    return suite;
}
