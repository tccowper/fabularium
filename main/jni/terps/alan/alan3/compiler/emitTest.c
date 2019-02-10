/*======================================================================*\

  emitTest.c

  Unit tests for EMIT node in the Alan compiler

\*======================================================================*/

#include "emit.c"

#include <cgreen/cgreen.h>

Describe(Emit);
BeforeEach(Emit) {}
AfterEach(Emit) {}


Ensure(Emit, testEmit) {
  int expectedAddress;
  Aword emitTestArray[5] = {1, 2, 3, 4, 5};

 initEmit("unit.a3c");

  expectedAddress = nextEmitAddress();

  emit(0);
  expectedAddress += 1;
  assert_true(nextEmitAddress() == expectedAddress);
  expectedAddress = nextEmitAddress();

  emitString("123");
  expectedAddress += 1;
  assert_true(nextEmitAddress() == expectedAddress);
  expectedAddress = nextEmitAddress();

  emitString("1234");
  expectedAddress += 2;
  assert_true(nextEmitAddress() == expectedAddress);
  expectedAddress = nextEmitAddress();

  emit0(I_IF);
  expectedAddress += 1;
  assert_true(nextEmitAddress() == expectedAddress);
  expectedAddress = nextEmitAddress();

  emitN((Aword *)&emitTestArray, 5);
  expectedAddress += 5;
  assert_true(nextEmitAddress() == expectedAddress);
  expectedAddress = nextEmitAddress();

  emitEntry((Aword *)&emitTestArray, 6*4);
  expectedAddress += 6;
  assert_true(nextEmitAddress() == expectedAddress);
  expectedAddress = nextEmitAddress();
}


static void generateTextDataFile(char textDataFileName[], char textData[])
{
  FILE *textDataFile = fopen(textDataFileName, WRITE_MODE);
  fwrite(textData, strlen(textData), 1, textDataFile);
  fclose(textDataFile);
}


Ensure(Emit, testEmitTextDataToAcodeFile) {
  char textDataFileName[] = "emitTestData";
  char textData[] = "asfasjfalsfhwerouwr87340183482jlasfls";
  int i;

  generateTextDataFile(textDataFileName, textData);
  acdfil = fopen("emitTestAcode", WRITE_MODE);
  copyTextDataToAcodeFile(textDataFileName);
  fclose(acdfil);
  acdfil = fopen("emitTestAcode", READ_MODE);
  for (i = 0; i < strlen(textData); i ++)
    if (fgetc(acdfil) != textData[i]) {
      assert_true(FALSE);
    }
  fclose(acdfil);
  unlink(textDataFileName);
}

#ifdef EXTENDED_HEADER
// This is the prettiest test case in Alan, and we just don't need it...
Ensure(Emit, finalizeEmitShouldAdjustCRCWithoutChangingSize) {
    int i;
    Aword buffer[100];

    /* Given: */
    crc = 0;
    acodeHeader.acdcrc = 0;
    initEmitBuffer(buffer);
    
    Aword *headerAsArray = (Aword *)&acodeHeader;

    int sizeDiff = ASIZE(ACodeHeader) - ASIZE(Pre3_0beta3Header);
    for (i = ASIZE(Pre3_0beta3Header); i < ASIZE(ACodeHeader); i++)
        headerAsArray[i] = 0x22334455;

    int size = nextEmitAddress();

    /* When: */
    finalizeEmit();

    /* Then: */
    assert_true(acodeHeader.acdcrc == sizeDiff * ((Aword)0x22 + (Aword)0x33 + (Aword)0x44 + (Aword)0x55));
    assert_true(acodeHeader.size == size);
}
#endif

TestSuite *emitTests()
{
    TestSuite *suite = create_test_suite();

    add_test_with_context(suite, Emit, testEmit);
    add_test_with_context(suite, Emit, testEmitTextDataToAcodeFile);
    //    add_test_with_context(suite, Emit, finalizeEmitShouldAdjustCRCWithoutChangingSize);

    return suite;
}

