/*======================================================================*\

  wrdTest.c

  Unit tests for WRD node in the Alan compiler

\*======================================================================*/

#include "wrd.c"

#include <cgreen/cgreen.h>

#include "unitmock.h"


Describe(Word);
BeforeEach(Word) {}
AfterEach(Word) {}


Ensure(Word, testInsertWord) {
  Word w1, w2, w3, w4;

  w1.string = "s1";
  w1.low = w1.high = NULL;
  w2.string = "s2";
  w2.low = w2.high = NULL;
  w3.string = "s3";
  w3.low = w3.high = NULL;
  w4.string = "s4";
  w4.low = w4.high = NULL;

  wordTree = NULL;
  insertWord(&w1);
  assert_true(wordTree == &w1);
  insertWord(&w3);
  assert_true(wordTree->high == &w3);
  insertWord(&w2);
  assert_true(wordTree->high->low == &w2);
  insertWord(&w4);
  assert_true(wordTree->high->high == &w4);
}

Ensure(Word, testNewWord) {
  Instance i1;

  wordTree = NULL;

  newPronounWord("p", &i1);
  assert_true(strcmp(wordTree->string, "p") == 0);
  assert_true(wordTree->ref[PRONOUN_WORD]->member.ins == &i1);

  newSynonymWord("s", findWord("p"));
  assert_true(strcmp(wordTree->high->string, "s") == 0);
  assert_true(wordTree->high->ref[SYNONYM_WORD]->member.word == findWord("p"));
}

Ensure(Word, testGenerateWordEntry) {
  Word w1, *w2;
  DictionaryEntry de[2];

  wordTree = NULL;
  initEmitBuffer((Aword*)&de);

  w1.stradr = 14;
  w1.classbits = VERB_BIT;
  w1.code = 17;
  w1.nounRefAddress = 19;
  w1.adjectiveRefAddress = 21;

  generateWordEntry(&w1);
  assert_true(convertFromACD(de[0].string) == 14);
  assert_true(convertFromACD(de[0].classBits) == VERB_BIT);
  assert_true(convertFromACD(de[0].code) == 17);
  assert_true(convertFromACD(de[0].nounRefs) == 19);
  assert_true(convertFromACD(de[0].adjectiveRefs) == 21);  

  newSynonymWord("w2", &w1);
  w2 = findWord("w2");
  w2->stradr = 15;
  w2->classbits = SYNONYM_BIT;

  generateWordEntry(w2);
  assert_true(convertFromACD(de[1].string) == 15);
  assert_true(convertFromACD(de[1].classBits) == (SYNONYM_BIT|VERB_BIT));
  assert_true(convertFromACD(de[1].code) == 17);
  assert_true(convertFromACD(de[1].nounRefs) == 19);
  assert_true(convertFromACD(de[1].adjectiveRefs) == 21);  
}

Ensure(Word, canSeeIfWordDefinedInDictionary) {
	assert_that(findWord("added"), is_null);
	newWord("added", NOISE_WORD, 0, NULL);
	assert_that(findWord("added"), is_not_equal_to(NULL));
}

TestSuite *wrdTests()
{
    TestSuite *suite = create_test_suite();

    add_test_with_context(suite, Word, testInsertWord);
    add_test_with_context(suite, Word, testNewWord);
    add_test_with_context(suite, Word, testGenerateWordEntry);
	add_test_with_context(suite, Word, canSeeIfWordDefinedInDictionary);

    return suite;
}
