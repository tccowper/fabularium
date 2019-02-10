/*----------------------------------------------------------------------*\

  WRD.C
  Dictionary Word Nodes

  \*----------------------------------------------------------------------*/

#include "wrd_x.h"

#include "alan.h"
#include "srcp_x.h"
#include "id_x.h"
#include "lst_x.h"

#include "lmList.h"
#include "acode.h"
#include "util.h"
#include "opt.h"
#include "emit.h"



/* PUBLIC: */

int words[WRD_CLASSES+1];


/* Private: */
static Word *wordTree = NULL;


/*======================================================================*/
Word *findWord(char *str)	/* IN - The string */
{
    Word *wrd;			/* Traversal pointers */
    int comp = 1;			/* Result of comparison */
    Word *lastWordFound;		/* The last word found */

    wrd = wordTree;
    while (wrd != NULL) {
        lastWordFound = wrd;			/* Set last word found */
        comp = compareStrings(str, lastWordFound->string);
        if (comp == 0)
            return(lastWordFound);
        if (comp < 0)
            wrd = lastWordFound->low;
        else
            wrd = lastWordFound->high;
    }
    return(NULL);
}


/*----------------------------------------------------------------------*/
static void insertWord(Word *new) {
    Word *wrd;			/* Traversal pointer */
    int comparison;		/* Comparison result */
    Word *lastWordFound;		/* The last word found */

    if (wordTree == NULL)
        wordTree = new;
    else {
        wrd = wordTree;
        while (wrd != NULL) {
            lastWordFound = wrd;			/* Set last word found */
            comparison = compareStrings(new->string, lastWordFound->string);
            if (comparison < 0)
                wrd = lastWordFound->low;
            else
                wrd = lastWordFound->high;
        }
        if (comparison < 0)
            lastWordFound->low = new;
        else
            lastWordFound->high = new;
    }
}


/*----------------------------------------------------------------------*/
static Bool findReference(Instance *ref, List *referenceList)
{
    List *l;

    for (l = referenceList; l != NULL; l = l->next)
        if (l->member.ins == ref)
            return TRUE;
    return FALSE;
}


/*----------------------------------------------------------------------*/
static int newWord(char *theWord, WordKind class, int code, void *references)
{
    Word *new;
    Word *existingWord;
    char *string;

    if (theWord == NULL)
        SYSERR("theWord == NULL", nulsrcp);

    /* Convert the word to lower case before storing it in the dictionary */
    string = strdup(theWord);

    /* Find the word if it exists */
    existingWord = findWord(string);
    if (existingWord != NULL) {
        if (!findReference(references, existingWord->ref[class])) {
            /* Add another reference */
            existingWord->classbits |= 1L<<class;
            existingWord->ref[class] = concat(existingWord->ref[class],
                                              references, REFERENCE_LIST);
            if (existingWord->code == -1)
                /* It was previously without a code */
                existingWord->code = code;
        }
        return existingWord->code;
    }

    new = NEW(Word);

    new->classbits = 1L<<class;
    new->string = string;
    new->code = code;
    memset(new->ref, 0, sizeof(new->ref));
    new->ref[class] = newList(references, REFERENCE_LIST);

    new->low = NULL;
    new->high = NULL;

    insertWord(new);

    words[class]++;
    words[WRD_CLASSES]++;

    /* Number the new word if so indicated */
    if (new->code == -1)
        new->code = words[class];
    if (new->code == 0)
        new->code = words[WRD_CLASSES];
    return new->code;
}


/*======================================================================*/
int newPronounWord(char *theWord, Instance *reference) {
    return newWord(theWord, PRONOUN_WORD, -1, reference);
}


/*======================================================================*/
int newSynonymWord(char *theWord, Word *reference) {
    return newWord(theWord, SYNONYM_WORD, 0, reference);
}

/*======================================================================*/
int newVerbWord(char *theWord, Syntax *reference) {
    return newWord(theWord, VERB_WORD, 0, reference);
}

/*======================================================================*/
int newPrepositionWord(char *theWord) {
    return newWord(theWord, PREPOSITION_WORD, 0, NULL);
}

/*======================================================================*/
int newDirectionWord(char *theWord, int code) {
    return newWord(theWord, DIRECTION_WORD, code, NULL);
}

/*======================================================================*/
int newNounWord(char *theWord, int code, Instance *reference) {
    return newWord(theWord, NOUN_WORD, code, reference);
}

/*======================================================================*/
int newAdjectiveWord(char *theWord, Instance *reference) {
    return newWord(theWord, ADJECTIVE_WORD, 0, reference);
}


/*======================================================================

  prepareWords()

  Prepare the dictionary by inserting some words dependent on the
  selected language, for example.

*/
void prepareWords(void)
{

    /* Enter some words in the dictionary. Predefined pronouns are added
       to the classes, not here */
    switch (opts[OPTLANG].value) {
    case L_ENGLISH:
        newWord("go", NOISE_WORD, 0, NULL);
        newWord("them", THEM_WORD, 0, NULL);
        newWord("except", EXCEPT_WORD, 0, NULL);
        newWord("but", EXCEPT_WORD, 0, NULL);
        newWord("and", CONJUNCTION_WORD, 0, NULL);
        newWord("all", ALL_WORD, 0, NULL);
        newWord("everything", ALL_WORD, 0, NULL);
        newWord("then", CONJUNCTION_WORD, 0, NULL);
        break;
    case L_SWEDISH:
        newWord("gå", NOISE_WORD, 0, NULL);
        newWord("dem", THEM_WORD, 0, NULL);
        newWord("utom", EXCEPT_WORD, 0, NULL);
        newWord("förutom", EXCEPT_WORD, 0, NULL);
        newWord("och", CONJUNCTION_WORD, 0, NULL);
        newWord("allt", ALL_WORD, 0, NULL);
        newWord("alla", ALL_WORD, 0, NULL);
        break;
    case L_GERMAN:
        newWord("gehen", NOISE_WORD, 0, NULL);
        newWord("sie", THEM_WORD, 0, NULL);
        newWord("ausser", EXCEPT_WORD, 0, NULL);
        newWord("und", CONJUNCTION_WORD, 0, NULL);
        newWord("alles", ALL_WORD, 0, NULL);
        break;
    default:
        SYSERR("Unrecognized language", nulsrcp);
        break;
    }
}


#define HASBIT(b, w) (((1L<<(b))&w)==(1L<<(b)))
#define ISASYNONYM(w) HASBIT(SYNONYM_WORD, (w))
#define ISADIRECTION(w) HASBIT(DIRECTION_WORD, (w))
#define ISAVERB(w) HASBIT(VERB_WORD, (w))
#define ISAADJECTIVE(w) HASBIT(ADJECTIVE_WORD, (w))

/*----------------------------------------------------------------------*/
static void analyzeWord(Word *wrd) {
    if (ISASYNONYM(wrd->classbits) && (~(1L<<SYNONYM_WORD))&wrd->classbits)
        /* Synonyms can not be of any other class */
        lmLog(NULL, 333, sevERR, wrd->string);

    else if (ISADIRECTION(wrd->classbits) && ISAVERB(wrd->classbits))
        /* Directions and verbs won't work */
        lmLogv(NULL, 320, sevERR, wrd->string, "direction", "verb", NULL);

#ifdef ADJVERB_PROBLEM
    else if (ISAADJECTIVE(wrd->classbits) && ISAVERB(wrd->classbits))
        /* Adjectives and verbs don't work as expected */
        lmLogv(NULL, 320, sevWAR, wrd->string, "adjective", "verb", NULL);
#endif
}



/*----------------------------------------------------------------------

  Analyze one word in the dictionary to find any words that are
  defined to be of multiple word classes that we want to warn about.

*/
void analyzeWords(Word *wrd)
{
    /* Analyze one word in the dictionary to find any words that are
       defined to be of multiple word classes that we want to warn
       about.
    */

    if (wrd == NULL) return;

    analyzeWords(wrd->low);
    analyzeWord(wrd);
    analyzeWords(wrd->high);

}


/*======================================================================*/
void analyzeAllWords(void)
{
    /* Analyze the dictionary to find any words that are defined to be
       of multiple word classes that we want to warn about. */

    analyzeWords(wordTree);
}


/*======================================================================*/
void finalizeWords(void)
{
	/* If some common noise words have not been added to the dictionary, then
	   add them */
	switch (opts[OPTLANG].value) {
	case L_ENGLISH:
		newWord("the", NOISE_WORD, 0, NULL);
		break;
	case L_SWEDISH:
		newWord("den", NOISE_WORD, 0, NULL);
		newWord("det", NOISE_WORD, 0, NULL);
		newWord("de", NOISE_WORD, 0, NULL);
		break;
	case L_GERMAN:
		newWord("der", NOISE_WORD, 0, NULL);
		newWord("die", NOISE_WORD, 0, NULL);
		newWord("das", NOISE_WORD, 0, NULL);
		break;
	}
}



/*----------------------------------------------------------------------*/
static void generateWordReference(Word *wrd) {
    List *lst;

    if (wrd->classbits&NOUN_BIT) {
        wrd->nounRefAddress = nextEmitAddress();
        for (lst = wrd->ref[NOUN_WORD]; lst != NULL; lst = lst->next)
            generateId(lst->member.ins->props->id, 0);
        emit(EOF);
    } else
        wrd->nounRefAddress = 0;

    if (wrd->classbits&ADJECTIVE_BIT) {
        wrd->adjectiveRefAddress = nextEmitAddress();
        for (lst = wrd->ref[ADJECTIVE_WORD]; lst != NULL; lst = lst->next)
            generateId(lst->member.ins->props->id, 0);
        emit(EOF);
    } else
        wrd->adjectiveRefAddress = 0;

    if (wrd->classbits&PRONOUN_BIT) {
        wrd->pronounRefAddress = nextEmitAddress();
        for (lst = wrd->ref[PRONOUN_WORD]; lst != NULL; lst = lst->next)
            generateId(lst->member.ins->props->id, 0);
        emit(EOF);
    } else
        wrd->pronounRefAddress = 0;
}


/*----------------------------------------------------------------------*/
static void generateWordReferences(Word *wrd)
{
    if (wrd == NULL)
        return;

    /* First generate for lower */
    generateWordReferences(wrd->low);

    generateWordReference(wrd);

    /* Then for higher */
    generateWordReferences(wrd->high);

}



/*----------------------------------------------------------------------*/
static void generateWordStrings(Word *wrd)
{
    if (wrd == NULL)
        return;

    /* First generate for lower */
    generateWordStrings(wrd->low);

    /* Then this node */
    wrd->stradr = nextEmitAddress();	/* Save address to string */
    emitString(wrd->string);

    /* Then for higher */
    generateWordStrings(wrd->high);

}


/*----------------------------------------------------------------------*/
static void generateWordEntry(Word *wrd) {

    DictionaryEntry de;

    de.string = wrd->stradr;

    /* Synonyms can not be anything else... */
    if (wrd->classbits == SYNONYM_BIT) {
        /* If a synonym generate same as for original but mark as synonym */
        /* The reference is actually a pointer to the original Word */
        Word *original = (Word *)wrd->ref[SYNONYM_WORD]->member.word;
        de.classBits = original->classbits|SYNONYM_BIT;
        de.code = original->code;
        de.adjectiveRefs = original->adjectiveRefAddress;
        de.nounRefs = original->nounRefAddress;
        de.pronounRefs = original->pronounRefAddress;
    } else {
        de.classBits = wrd->classbits;
        de.code = wrd->code;
        de.adjectiveRefs = wrd->adjectiveRefAddress;
        de.nounRefs = wrd->nounRefAddress;
        de.pronounRefs = wrd->pronounRefAddress;
    }
    emitEntry(&de, sizeof(DictionaryEntry));
}


/*----------------------------------------------------------------------*/
static void generateWordEntries(Word *wrd)
{
    if (wrd->low != NULL) generateWordEntries(wrd->low);
    progressCounter();
    generateWordEntry(wrd);
    if (wrd->high != NULL) generateWordEntries(wrd->high);
}



/*======================================================================*/
Aaddr generateAllWords(void)
{
    Aaddr adr;

    /* First generate reference lists */
    generateWordReferences(wordTree);

    /* and strings */
    generateWordStrings(wordTree);

    /* Now traverse the word tree and generate dictionary entries */
    adr = nextEmitAddress();	/* Save ACODE address to dictionary */
    generateWordEntries(wordTree); /* Recursively... */

    emit(EOF);

    return(adr);
}
