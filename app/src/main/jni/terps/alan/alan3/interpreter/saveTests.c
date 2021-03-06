#include "cgreen/cgreen.h"

#include "save.c"


Describe(Save);
BeforeEach(Save) {}
AfterEach(Save) {}


Ensure(Save, canSaveRestore) {
  FILE *saveFile = fopen("testSaveFile", "w");
  Aword scoreTable = EOF;
  int i;

  /* Set up empty eventQ and scores and other irrelevant data */
  eventQueueTop = 0;
  scores = &scoreTable;
  adventureName = "adventure";
  adventureFileName = "adventure.a3c";

  /* Init header */
  header = allocate(sizeof(ACodeHeader));
  header->instanceMax = 3;
  header->attributesAreaSize = 21*sizeof(AttributeEntry)/sizeof(Aword);
  header->scoreCount = 0;
  header->stringInitTable = 0;

  /* Initialize a fake instance table */
  instances = malloc(4*sizeof(InstanceEntry));
  instances[0].initialAttributes = 12; /* Shouldn't matter where.. */
  instances[1].initialAttributes = 12; /* Shouldn't matter where.. */
  instances[2].initialAttributes = 12; /* Shouldn't matter where.. */
  instances[3].initialAttributes = 12; /* Shouldn't matter where.. */

  /* Allocate an attribute area and initialize it */
  attributes = malloc(21*sizeof(AttributeEntry));
  for (i = 0; i<20; i++) {
    attributes[i].code = i;
    attributes[i].value = i;
    attributes[i].id = 0;
  }
  attributes[20].code = EOF;

  /* Fake admin areas for 3 instances */
  admin = allocate(5*sizeof(AdminEntry));
  admin[1].attributes = &attributes[0];
  admin[1].attributes[0].code = 11;
  admin[1].attributes[0].value = 11;
  admin[2].attributes = &attributes[5];
  admin[2].attributes[0].code = 22;
  admin[2].attributes[0].value = 22;
  admin[3].attributes = &attributes[7];
  admin[3].attributes[0].code = 33;
  admin[3].attributes[0].value = 33;

  /* Save the game data */
  saveGame(saveFile);
  fclose(saveFile);
  free(attributes);

  /* Get another attribute area and initialize admin areas */
  attributes = malloc(21*sizeof(AttributeEntry));
  for (i = 0; i<21; i++) {
    attributes[20-i].code = i;
    attributes[20-i].value = i;
  }
  admin[1].attributes = &attributes[0];
  admin[2].attributes = &attributes[5];
  admin[3].attributes = &attributes[7];

  saveFile = fopen("testSaveFile", "r");
  restoreGame(saveFile);
  fclose(saveFile);
  unlink("testSaveFile");

  assert_equal(11, admin[1].attributes[0].code);
  assert_equal(11, admin[1].attributes[0].value);
  assert_equal(22, admin[2].attributes[0].code);
  assert_equal(22, admin[2].attributes[0].value);
  assert_equal(33, admin[3].attributes[0].code);
  assert_equal(33, admin[3].attributes[0].value);
}

Ensure(Save, canSaveStrings) {
  char *testFileName = "testSaveStringFile";
  char *testString = "hejhopp";
  FILE *saveFile = fopen(testFileName, "w");
  Aword scoreTable = EOF;
  StringInitEntry *initEntry;

  /* Set up empty eventQ and scores and other irrelevant data */
  eventQueueTop = 0;
  header->scoreCount = 0;
  scores = &scoreTable;
  adventureName = "adventure";
  adventureFileName = "adventure.a3c";

  /* Init header for one instance with one attribute */
  header->instanceMax = 1;
  header->attributesAreaSize = sizeof(AttributeEntry)/sizeof(Aword);

  /* Initialize a fake instance table */
  instances = malloc(2*sizeof(InstanceEntry));
  instances[1].parent = 0;

  /* Allocate an attribute area and initialize it */
  attributes = malloc(2*sizeof(AttributeEntry));
  attributes[0].code = 1;
  attributes[0].value = toAptr(strdup(testString));
  attributes[0].id = 0;
  attributes[1].code = EOF;

  /* Fake admin areas for one instances */
  admin = allocate(2*sizeof(AdminEntry));
  admin[1].attributes = &attributes[0];

  /* A String Init Table is required */
  memory = allocate(3*sizeof(StringInitEntry));
  header->stringInitTable = 1;
  initEntry = (StringInitEntry*)pointerTo(1);
  initEntry->instanceCode = 1;
  initEntry->attributeCode = 1;
  *((Aword *)&initEntry[1]) = EOF;

  /* Save the game data */
  saveGame(saveFile);
  fclose(saveFile);
  admin[1].attributes[0].value = toAptr(strdup("i lingonskogen"));

  saveFile = fopen(testFileName, "r");
  restoreGame(saveFile);
  fclose(saveFile);
  unlink(testFileName);

  assert_equal(0, strcmp((char *)fromAptr(admin[1].attributes[0].value), testString));
}

Ensure(Save, canSaveSets) {
  char *testFileName = "testSaveSetFile";
  Set *testSet[4];
  FILE *saveFile = fopen(testFileName, "w");
  Aword scoreTable = EOF;
  SetInitEntry *initEntry;
  int i,j;

  /* Set up empty eventQ and scores and other irrelevant data */
  eventQueueTop = 0;
  header->scoreCount = 0;
  scores = &scoreTable;
  adventureName = "adventure";
  adventureFileName = "adventure.a3c";

  /* Init header for one instance with four attributes */
  header->instanceMax = 1;
  header->attributesAreaSize = 4*sizeof(AttributeEntry)/sizeof(Aword);
  header->scoreCount = 0;
  header->stringInitTable = 0;

  /* Initialize a fake instance table */
  instances = malloc(2*sizeof(InstanceEntry));
  instances[1].parent = 0;

  /* Set up the test sets */
  for (i = 0; i < 4; i++) {
    testSet[i] = newSet(i);
    for (j = 0; j < i; j++)
      addToSet(testSet[i], j);
  }

  /* Allocate an attribute area and initialize it */
  attributes = malloc(5*sizeof(AttributeEntry));
  for (i = 0; i < 4; i++) {
    attributes[i].code = i+1;
    attributes[i].value = toAptr(copySet(testSet[i]));
    attributes[i].id = 0;
  }
  attributes[4].code = EOF;

  /* Fake admin areas for one instances */
  admin = allocate(2*sizeof(AdminEntry));
  admin[1].attributes = &attributes[0];

  /* A Set Init Table is required */
  memory = allocate(5*sizeof(SetInitEntry));
  header->setInitTable = 1;
  initEntry = (SetInitEntry*)pointerTo(1);
  initEntry[0].instanceCode = 1;
  initEntry[0].attributeCode = 1;
  initEntry[1].instanceCode = 1;
  initEntry[1].attributeCode = 2;
  initEntry[2].instanceCode = 1;
  initEntry[2].attributeCode = 3;
  initEntry[3].instanceCode = 1;
  initEntry[3].attributeCode = 4;
  *((Aword *)&initEntry[4]) = EOF;

  /* Save the game data */
  saveGame(saveFile);
  fclose(saveFile);

  /* Set new values */
  for (i = 0; i < 4; i++)
      admin[1].attributes[i].value = toAptr(newSet(0));

  saveFile = fopen(testFileName, "r");
  restoreGame(saveFile);
  fclose(saveFile);
  unlink(testFileName);

  for (i = 0; i < 4; i++)
      assert_true(equalSets((Set *)fromAptr(admin[1].attributes[i].value), testSet[i]));
}

Ensure(Save, canSaveRestoreScore) {
  char *fileName = "testSaveRestoreScore";
  FILE *saveFile;
  int i;
  Aword *oldScores;
  int scoreCount = 48;

  header = allocate(sizeof(ACodeHeader));
  oldScores = scores = allocate(scoreCount*sizeof(Aword));
  header->scoreCount = scoreCount;

  for (i = 0; i < scoreCount; i++)
    scores[i] = i;

  saveFile = fopen(fileName, "wb");
  saveScores(saveFile);
  fclose(saveFile);

  scores = allocate(scoreCount*sizeof(Aword));

  for (i = 0; i < scoreCount; i++)
    scores[i] = 50-i;

  saveFile = fopen(fileName, "rb");
  restoreScores(saveFile);

  assert_equal(scoreCount, header->scoreCount);
  assert_equal(0, memcmp(scores, oldScores, scoreCount*sizeof(Aword)));
  assert_equal(EOF, fgetc(saveFile));

  fclose(saveFile);
  unlink(fileName);
  free(scores);
  free(oldScores);
  free(header);
}
