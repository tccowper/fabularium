/*======================================================================*\

  resourceTest.c

  Unit tests for RESOURCE node in the Alan compiler

\*======================================================================*/

#include "resource.c"

#include <cgreen/cgreen.h>

#include <sys/stat.h>
#include <stdio.h>

#include "unitList.h"


Describe(Resource);
BeforeEach(Resource) {}
AfterEach(Resource) {}


Ensure(Resource, testNumberImages) {
  List e[5];
  int j;
  Id *i[5] = {NULL,
		  newId(nulsrcp, "a"),
		  newId(nulsrcp, "b"),
		  newId(nulsrcp, "b"),
		  newId(nulsrcp, "c")
  };
  List *resources;

  e[1].member.resource = newResource(nulsrcp, i[1]);
  e[2].member.resource = newResource(nulsrcp, i[2]);
  e[3].member.resource = newResource(nulsrcp, i[3]);
  e[4].member.resource = newResource(nulsrcp, i[4]);

  /* Zero elements */
  resources = NULL;
  resourceNumber = 1;
  numberResources(resources);
  assert_true(resources == NULL);

  /* One element */
  e[1].next = NULL;
  e[1].member.resource->fileName->code = 0;
  resources = &e[1];
  resourceNumber = 1;
  numberResources(resources);
  assert_true(resources == &e[1]);
  assert_true(resources->next == NULL);
  assert_true(resources->member.resource->fileName->code == 1);

  /* Four elements */
  e[1].next = &e[2];
  e[2].next = &e[3];
  e[3].next = &e[4];
  e[4].next = NULL;
  for (j = 1; j < 5; j++)
    i[j]->code = 0;

  resources = &e[1];
  resourceNumber = 1;
  numberResources(resources);

  assert_true(e[1].member.resource->fileName->code == 1);
  assert_true(e[2].member.resource->fileName->code == 2);
  assert_true(e[3].member.resource->fileName->code == 2);
  assert_true(e[4].member.resource->fileName->code == 3);
}


Ensure(Resource, testGenerateBlcFile) {
  char *blcFileName = "unittest.blc";
  FILE *blcFile = openNewBlcFile("unittest.blc");
  struct stat fileStat;
  List *resources;
  static char *resourceName = "unittest.jpg";
  FILE *resourceFile = fopen(resourceName, "w");
  fclose(resourceFile);

  generateBlcFile(blcFile, NULL);
  fclose(blcFile);

  blcFile = fopen(blcFileName, "r");
  assert_true(blcFile != NULL);
  stat("unittest.blc", &fileStat);
  assert_true(fileStat.st_size == 0);

  resourceNumber = 1;
  resources = newList(newResource(nulsrcp, newId(nulsrcp, resourceName)), RESOURCE_LIST);
  analyzeResources(resources);
  analyzeResource(resources->member.resource);
  blcFile = openNewBlcFile("unittest.blc");
  generateBlcFile(blcFile, resources);
  fclose(blcFile);

  {
    char type[10];
    int number;
    char fileName[100];
    char chunk[10];
    blcFile = fopen(blcFileName, "r");
    fscanf(blcFile, "%s%d%s%s", type, &number, chunk, fileName);
    assert_true(strcmp(type, "Pict") == 0);
    assert_true(strcmp(chunk, "JPEG") == 0);
    assert_true(number == 1);
    assert_true(strcmp(fileName, resourceName) == 0);
  }
  unlink(blcFileName);
}

Ensure(Resource, testAnalyzeResource) {
  Resource *resource = newResource(nulsrcp, newId(nulsrcp, ""));
  char fileName[200] = "resourceUnitTest.jpg";
  char *legalExtensions[] = {".jpg", ".jpeg", ".png", ".PnG", ".mod", NULL};
  int i;

  resource->fileName->string = fileName;
  analyzeResource(resource);
  assert_true(readEcode() == 153);

  for (i = 0; legalExtensions[i] != NULL; i++) {
    strcpy(&fileName[16], legalExtensions[i]);
    FILE *f = fopen(fileName, "w");
    fclose(f);
    resource->fileName->string = fileName;
    analyzeResource(resource);
    assert_true(readEcode() == 0);
    unlink(fileName);
  }

  {
    char *fileName = "aslkd.phf";
    FILE *f = fopen(fileName, "w");
    fclose(f);
    resource->fileName->string = fileName;
    analyzeResource(resource);
    assert_true(readEcode() == 801);
    unlink(fileName);
  }
}


TestSuite *resourceTests()
{
    TestSuite *suite = create_test_suite(); 

    add_test_with_context(suite, Resource, testNumberImages);
    add_test_with_context(suite, Resource, testAnalyzeResource);
    add_test_with_context(suite, Resource, testGenerateBlcFile);

    return suite;
}
