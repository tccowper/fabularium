/*----------------------------------------------------------------------*\

  adv.c
  Adventure Node

\*----------------------------------------------------------------------*/

/* Own: */
#include "adv.h"

/* Imports: */
#include "alan.h"
#include "util.h"

#include "lmList.h"
#include "smScan.h"

#include "add_x.h"
#include "cla_x.h"
#include "cnt_x.h"
#include "context_x.h"
#include "ins_x.h"
#include "lst_x.h"
#include "scr_x.h"
#include "srcp_x.h"
#include "stm_x.h"
#include "stx_x.h"
#include "sym_x.h"
#include "vrb_x.h"
#include "whr_x.h"
#include "wrd_x.h"
#include "atr_x.h"
#include "rul_x.h"
#include "ifid_x.h"
#include "resource_x.h"
#include "dump_x.h"


#include "ext.h"		/* EXT-nodes */
#include "evt.h"		/* EVT-nodes */
#include "syn.h"		/* SYN-nodes */
#include "msg.h"		/* MSG-nodes */

#include "sco.h"		/* SCORES */
#include "opt.h"		/* OPTIONS */
#include "options.h"		/* OPTIONS */

#include "emit.h"
#include "encode.h"
#include "acode.h"



/* PUBLIC */
Adventure adv;


/* PRIVATE */
static SourceFileEntry *sourceFileEntries;



/*======================================================================*/
void initAdventure(void)
{
    initSymbols();
    initClasses();
    initInstances();
    adv.ifids = initIfids();
}


/*======================================================================*/
void symbolizeAdventure()
{
    symbolizeClasses();
    symbolizeInstances();

    symbolizeWhere(adv.whr);
}


/*----------------------------------------------------------------------*/
static void analyzeStartAt(void)
{
    Context *context = newStartContext();

    if (adv.whr != NULL)
        switch (adv.whr->kind) {
        case WHERE_AT: {
            What *what = adv.whr->what->fields.wht.wht;
            if (what->kind == WHAT_ID) {
                instanceCheck(what->id, "START statement", "location");
            } else
                lmLog(&adv.whr->srcp, 211, sevERR, "");
            break;
        }
        default:
            lmLog(&adv.whr->srcp, 211, sevERR, "");
            break;
        }

    analyzeStatements(adv.stms, context);
}


/*----------------------------------------------------------------------*/
static void analyzePrompt(void)
{
    Context *context = newStartContext();
    if (adv.prompt != NULL)
        analyzeStatements(adv.prompt, context);
}


/*----------------------------------------------------------------------*/
static void analyzeSourceFilenames() {
    List *currentFile;
    int count = 0;

    sourceFileEntries = allocate(length(fileNames)*sizeof(SourceFileEntry));
    ITERATE(currentFile,fileNames) {
        sourceFileEntries[count].fpos = ftell(txtfil);
        sourceFileEntries[count].len = strlen(currentFile->member.str);
        fprintf(txtfil, "%s", currentFile->member.str);
        count++;
    }
}


/*====================================================================== */
void analyzeAdventure(void)
{
    addHero(&adv);
    if (nowhere == NULL || nowhere->code != NOWHERE)
        SYSERR("Nowhere != 1", nulsrcp);

    addLiteralInstance();

    symbolizeAdventure();

    addAdditions();
    addHeroContainer();

    setupDefaultProperties();

    analyzeAllAttributes();	/* Make sure attributes are analyzed
                               and typed before expressions */
    numberAllAttributes();	/* Then we can number and type check
                               inherited attributes */

    calculateTransitiveContainerContents();

    replicateInherited();

    prepareWords();			/* Prepare words in the dictionary */
    prepareMessages();      /* Prepare standard and user messages */
    prepareScores();        /* Prepare score handling */

    verbose("Syntax definitions");
    analyzeSyntaxes();

    verbose("Verbs");
    analyzeVerbs(adv.vrbs, NULL);

    verbose("Classes");
    analyzeClasses();

    verbose("Instances");
    analyzeInstances();
    theHero->fields.entity.props->whr = adv.whr;

    numberContainers();

    verbose("Events");
    analyzeEvents();

    verbose("Rules");
    analyzeRules();

    verbose("Synonyms");
    analyzeSynonyms();

    verbose("Message");
    analyzeMessages();

    analyzePrompt();

    analyzeStartAt();
    analyzeAllWords();
    adv.resources = analyzeResources(adv.resources);

    if (debugFlag)
        analyzeSourceFilenames();

    finalizeWords();
}


/*----------------------------------------------------------------------*/
static Aaddr generateSourceFileTable() {
    int count = 0;
    Aaddr adr = nextEmitAddress();

    if (opts[OPTDEBUG].value)
        for (count = 0; count < length(fileNames); count++) {
            long fpos, len;
            fpos = sourceFileEntries[count].fpos;
            len = sourceFileEntries[count].len;
            encode(&fpos, &len);
            sourceFileEntries[count].fpos = fpos;
            sourceFileEntries[count].len = len;
            emitEntry(&sourceFileEntries[count], sizeof(SourceFileEntry));
        }
    emit(EOF);
    return adr;
}


/*======================================================================*/
void generateAdventure(char acodeFileName[],
		       char textFileName[],
		       char dataFileName[])
{
    Aaddr parameterNamesAddress = 0;

    initEmit(acodeFileName);		/* Initialise code emit */
    initEncoding(textFileName, dataFileName);	/* Initialise encoding of text */
    if (lmSeverity() > sevWAR)
        return;

    acodeHeader.ifids = generateIfids(adv.ifids);

    verbose("Dictionary");
    acodeHeader.dictionary = generateAllWords();

    verbose("SyntaxTable");
    acodeHeader.syntaxTableAddress = generateParseTable();

    verbose("Parameter Mapping");
    if (debugFlag)
        parameterNamesAddress = generateParameterNames(adv.stxs);
    acodeHeader.parameterMapAddress = generateParameterMappingTable();
    emit(parameterNamesAddress);

    acodeHeader.maxParameters = 10;	/* TODO calculate and move this to a better place */

    verbose("Verbs");
    acodeHeader.verbTableAddress = generateVerbs(adv.vrbs);

    verbose("Classes");
    acodeHeader.classTableAddress = generateClasses();

    verbose("Instances");
    generateInstances(&acodeHeader);

    verbose("Containers");
    acodeHeader.containerTableAddress = generateContainers(&acodeHeader);

    verbose("Scripts");
    acodeHeader.scriptTableAddress = generateScripts(&acodeHeader);

    verbose("Events");
    acodeHeader.eventTableAddress = generateEvents(&acodeHeader);

    verbose("Rules");
    acodeHeader.ruleTableAddress = generateRules();

    generateScores(&acodeHeader);

    verbose("Messages");
    acodeHeader.messageTableAddress = gemsgs();

    verbose("Character Encoding");
    acodeHeader.freq = gefreq();	/* Character frequencies */


    /* Options */
    generateOptions(&acodeHeader);

    /* Player prompt */
    if (adv.prompt != NULL) {
        acodeHeader.prompt = nextEmitAddress();	/* Save ACODE address to prompt */
        generateStatements(adv.prompt);
        emit0(I_RETURN);
    }

    /* Start statements */
    acodeHeader.start = nextEmitAddress();	/* Save ACODE address to start */
    generateStatements(adv.stms);
    emit0(I_RETURN);

    /* String & Set attribute initialisation tables */
    acodeHeader.stringInitTable = generateStringInit();
    acodeHeader.setInitTable = generateSetInit();

    /* Source filename and line table */
    acodeHeader.sourceFileTable = generateSourceFileTable();
    acodeHeader.sourceLineTable = generateSrcps();

    /* All resource files found so package them */
    generateResources(adv.resources);

    terminateEncoding();

    finalizeEmit();


    /* Finally, include all text data and write the file header */
    copyTextDataToAcodeFile(dataFileName);
    writeHeader(&acodeHeader);

    terminateEmit();
}




/*====================================================================== */
void dumpAdventure(enum dmpKd dmp)
{
    if (dmp&DUMP_ALL)
        dmp |= (-1L&~DUMP_ADDRESSES);


    put("ADV: "); indent();

    put("synonyms: ");
    if (dmp&DUMP_SYNONYMS)
        dumpList(adv.syns, SYNONYM_LIST);
    else
        put("--");
    nl();

    put("syntaxes: ");
    if (dmp&DUMP_SYNTAXTABLE)
        dumpList(adv.stxs, SYNTAX_LIST);
    else
        put("--");
    nl();

    put("verbs: ");
    if (dmp&DUMP_VERBTABLE)
        dumpList(adv.vrbs, VERB_LIST);
    else
        put("--");
    nl();

    put("classes: ");
    if (dmp&DUMP_CLASSES) {
        dumpList(adv.clas, CLASS_LIST);
        nl();
        put("additions: ");
        dumpList(adv.adds, ADD_LIST);
    } else
        put("--");
    nl();

    put("instances: ");
    if (dmp&DUMP_INSTANCES)
        dumpList(adv.inss, INSTANCE_LIST);
    else
        put("--");
    nl();

    put("containers: ");
    if (dmp&DUMP_CONTAINERS)
        dumpList(adv.cnts, CONTAINER_LIST);
    else
        put("--");
    nl();

    put("events: ");
    if (dmp&DUMP_EVENTS)
        dumpList(adv.evts, EVENT_LIST);
    else
        put("--");
    nl();

    put("rules: ");
    if (dmp&DUMP_RULES)
        dumpList(adv.ruls, RULE_LIST);
    else
        put("--");
    nl();

    put("symbols: ");
    if (dmp&DUMP_SYMBOLTABLE)
        dumpSymbols();
    else
        put("--");
    nl();

    put("whr: ");
    dumpWhere(adv.whr);
    nl();

    put("stms: ");
    dumpList(adv.stms, STATEMENT_LIST);
    out();
    nl();

    put("ifids: ");
    dumpList(adv.ifids, IFID_LIST);
    out();
    nl();
}


/*----------------------------------------------------------------------*/
static void xmlStart(FILE *xmlFile) {
    fprintf(xmlFile, "    <start WHERE=\"%s\" />\n", adv.whr->what->fields.wht.wht->id->string);
}

/*======================================================================*/
void xmlAdventure(void) {
    char *xmlFileName = strdup(adv.name);
    FILE *xmlFile;

    if (xmlFlag) {
        xmlFileName = realloc(xmlFileName, strlen(xmlFileName)+4+1);
        strcat(xmlFileName, ".xml");

        xmlFile = fopen(xmlFileName, "w");
        fprintf(xmlFile, "<?xml version=\"1.0\"?>\n");
        fprintf(xmlFile, "<adventure>\n");
        xmlClasses(xmlFile);
        xmlInstances(xmlFile);
        xmlStart(xmlFile);
        fprintf(xmlFile, "</adventure>\n");
        fclose(xmlFile);
    }
}


/*======================================================================

  summary()

  Print out a short summary of the adventure.

*/
void summary(void)
{
    char str[80];

    lmSkipLines(8);

    lmLiPrint("");
    lmLiPrint("        Summary");
    lmLiPrint("        -------");
    if (verbCount != 0) {
        (void)sprintf(str, "        Verbs:                  %6d", verbCount);
        lmLiPrint(str);
    }
    if (classCount != 0) {
        (void)sprintf(str, "        Classes:                %6d", classCount);
        lmLiPrint(str);
    }
    if (instanceCount != 0) {
        (void)sprintf(str, "        Instances:              %6d", instanceCount);
        lmLiPrint(str);
    }
    (void)sprintf(str  , "        Words:                  %6d", words[WRD_CLASSES]);
    lmLiPrint(str);
    (void)sprintf(str,   "        Acode:                  %6d words (%d bytes)",
                  (int)acodeHeader.size, (int)(acodeHeader.size*sizeof(Aword)));
    lmLiPrint(str);
    (void)sprintf(str,   "        Text data:              %6d bytes", txtlen);
    lmLiPrint(str);
}
