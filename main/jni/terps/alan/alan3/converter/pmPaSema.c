/*----------------------------------------------------------------------*\

	pmPaSema.c

	ParserMaker generated semantic actions

\*----------------------------------------------------------------------*/

/* %%IMPORT */


#include "srcp.h"
#include "smScan.h"
#include "token.h"
#include "lmList.h"
#include "a2a3.h"
#include "util.h"

/* END %%IMPORT */

/* System dependencies
 * -------------------
 */

/* These datatypes should be defined to be unsigned integers of length 1, 2
 * and 4 bytes respectively.
 */
typedef unsigned char UByte1;
typedef unsigned short UByte2;
typedef unsigned int UByte4;

/* Token and Srcp definition */
#include "alanCommon.h"

/* Attribute stacks *\
\* ---------------- */
/* %%ATTRIBUTES */
/* The semantic attributes for grammar symbols */
typedef struct pmGrammar {
    int val;
    char *string;
} pmGrammar;

/* END %%ATTRIBUTES */

extern short pmStkP;
extern Token pmSySt[];
extern pmGrammar pmSeSt[];

/* %%DECLARATIONS - User data and routines */

#line 38 "alan.pmk"


static int level = 0;
static void newline()
{
  int i;
  fprintf(outFile, "\n");
  if (level < 0) printf("Level = %d", level);
  for (i = 0; i < level; i++)
    fprintf(outFile, "  ");
}

static void indent()
{
  level++;
}

static void outdent()
{
  level--;
}


static char *(v3Keywords[]) = {"to", "is", "at", "in", "an", "by",
"if", "on", "do", "or", "of", "end", "not", "and", "isa", "add",
"the", "are", "has", "say", "set", "for", "use", "max", "min", "sum",
"verb", "does", "exit", "when", "else", "form", "name", "with",
"then", "step", "wait", "here", "list", "show", "from", "make",
"last", "word", "into", "each", "stop", "quit", "look", "save",
"this", "only", "event", "start", "check", "where", "every", "count",
"after", "until", "empty", "strip", "first", "words", "elsif",
"score", "actor", "syntax", "script", "option", "opaque", "taking",
"limits", "header", "nearby", "locate", "cancel", "depend", "visits",
"system", "random", "before", "options", "message", "article",
"entered", "extract", "include", "exclude", "restore", "restart",
"between", "current", "synonyms", "definite", "describe", "schedule",
"increase", "decrease", "contains", "location", "container",
"mentioned", "character", "depending", "attributes", "indefinite",
"characters", "description", NULL};

static void print(char string[])
{
  fprintf(outFile, "%s", string);
}

static void idPrint(char id[])
{
  int wordIndex = 0;

  while (v3Keywords[wordIndex] != NULL && strcmp(id, v3Keywords[wordIndex]) != 0)
    wordIndex++;
  if (v3Keywords[wordIndex] != NULL) fprintf(outFile, "'%s'", id);
  else fprintf(outFile, "%s", id);
}

static void stringPrint(char string[])
{
  int ch, i;
  int endOfLine;
  char *remaining = string;
  Bool printed = FALSE;

  newline();
  onlyOneSpace(string);

  endOfLine = 75-level*2;
  while (endOfLine < strlen(remaining)) {
    while(remaining[endOfLine] != ' ' && remaining[endOfLine] != '\0' && endOfLine > 0) endOfLine--;
    ch = remaining[endOfLine];
    remaining[endOfLine] = '\0';
    for (i = 0; i < endOfLine; i++) if (remaining[i] == '\n') remaining[i] = ' ';
    if (printed) print(" ");
    print(remaining);
    remaining[endOfLine] = ch;
    remaining = &remaining[endOfLine];
    while(*remaining == ' ' || *remaining == '\n') remaining++;
    newline();
    printed = TRUE;
    endOfLine = 75-level*2;
  }
  if (printed) print(" ");
  for (i = 0; i < strlen(remaining); i++)
    if (remaining[i] == '\n')
      remaining[i] = ' ';
  print(remaining);
}



/* END %%DECLARATIONS */


/*----------------------------------------------------------------------------
 * pmPaSema - The semantic actions
 *----------------------------------------------------------------------------
 */
void pmPaSema(
int rule			/* IN production number */
)
{
#line 137 "alan.pmk"
    switch (rule) {
    case 1: { /* <adventure> = <optional_options> <units> <start>; */
#line 142 "alan.pmk"
 newline(); 	break;}
    case 3: { /* <optional_options> = 'OPTIONS' __genSym#0 <options>; */
#line 151 "alan.pmk"
 outdent(); newline(); newline(); 	break;}
    case 4: { /* __genSym#0 =; */
#line 149 "alan.pmk"
 print("Options"); indent(); newline(); 	break;}
    case 5: { /* <options> = <option>; */
#line 155 "alan.pmk"
 newline(); 	break;}
    case 7: { /* <option> = ID '.'; */
#line 160 "alan.pmk"

	idPrint(pmSeSt[pmStkP+1].string); print(".");
    	break;}
    case 8: { /* <option> = ID ID '.'; */
#line 164 "alan.pmk"

	idPrint(pmSeSt[pmStkP+1].string); print(" "); idPrint(pmSeSt[pmStkP+2].string); printf(".");
    	break;}
    case 9: { /* <option> = ID Integer '.'; */
#line 168 "alan.pmk"

	idPrint(pmSeSt[pmStkP+1].string); print(" "); print(pmSySt[pmStkP+2].chars); print(".");
    	break;}
    case 26: { /* <default> = 'DEFAULT' __genSym#1 'ATTRIBUTES' <attributes>; */
#line 207 "alan.pmk"

	outdent();
	outdent();
	newline();
	print("End Add To.");
	newline();
    	break;}
    case 27: { /* __genSym#1 =; */
#line 198 "alan.pmk"

	newline();
	print("Add To Every thing");
	indent();
	newline();
	print("Is");
	indent();
    	break;}
    case 28: { /* <location_default> = 'LOCATION' __genSym#2 'ATTRIBUTES' <attributes>; */
#line 228 "alan.pmk"

	outdent();
	outdent();
	newline();
	print("End Add To.");
	newline();
    	break;}
    case 29: { /* __genSym#2 =; */
#line 219 "alan.pmk"

	newline();
	print("Add To Every location");
	indent();
	newline();
	print("Is");
	indent();
    	break;}
    case 30: { /* <object_default> = 'OBJECT' __genSym#3 'ATTRIBUTES' <attributes>; */
#line 249 "alan.pmk"

	outdent();
	outdent();
	newline();
	print("End Add To.");
	newline();
    	break;}
    case 31: { /* __genSym#3 =; */
#line 240 "alan.pmk"

	newline();
	print("Add To Every object");
	indent();
	newline();
	print("Is");
	indent();
    	break;}
    case 32: { /* <actor_default> = 'ACTOR' __genSym#4 'ATTRIBUTES' <attributes>; */
#line 270 "alan.pmk"

	outdent();
	outdent();
	newline();
	print("End Add To.");
	newline();
    	break;}
    case 33: { /* __genSym#4 =; */
#line 261 "alan.pmk"

	newline();
	print("Add To Every actor");
	indent();
	newline();
	print("Is");
	indent();
    	break;}
    case 34: { /* <attributes> = <attribute> '.'; */
#line 282 "alan.pmk"
 print("."); 	break;}
    case 35: { /* <attributes> = <attributes> <attribute> '.'; */
#line 283 "alan.pmk"
 print("."); 	break;}
    case 36: { /* <attribute> = ID; */
#line 287 "alan.pmk"
 newline(); idPrint(pmSeSt[pmStkP+1].string); 	break;}
    case 37: { /* <attribute> = 'NOT' ID; */
#line 288 "alan.pmk"
 newline(); print("Not "); idPrint(pmSeSt[pmStkP+2].string); 	break;}
    case 38: { /* <attribute> = ID <optional_minus> Integer; */
#line 289 "alan.pmk"
 newline(); idPrint(pmSeSt[pmStkP+1].string); print(" "); print(pmSeSt[pmStkP+2].string); print(" "); print(pmSySt[pmStkP+3].chars); 	break;}
    case 39: { /* <attribute> = ID STRING; */
#line 290 "alan.pmk"
 newline(); idPrint(pmSeSt[pmStkP+1].string); print(" "); stringPrint(pmSySt[pmStkP+2].chars); 	break;}
    case 40: { /* <synonyms> = 'SYNONYMS' __genSym#5 <synonym_list>; */
#line 296 "alan.pmk"
 outdent(); newline(); 	break;}
    case 41: { /* __genSym#5 =; */
#line 295 "alan.pmk"
 newline(); print("Synonyms"); indent(); newline(); 	break;}
    case 44: { /* <synonym> = <id_list> '=' ID '.'; */
#line 303 "alan.pmk"
 print(" = "); idPrint(pmSeSt[pmStkP+3].string); print("."); newline(); 	break;}
    case 49: { /* <syntax> = 'SYNTAX' __genSym#6 <syntax_list>; */
#line 321 "alan.pmk"
 outdent(); newline(); 	break;}
    case 50: { /* __genSym#6 =; */
#line 320 "alan.pmk"
 newline(); print("Syntax"); indent(); 	break;}
    case 54: { /* <syntax_item1> = ID '='; */
#line 331 "alan.pmk"
 newline(); idPrint(pmSeSt[pmStkP+1].string); print(" ="); 	break;}
    case 58: { /* <syntax_element> = ID; */
#line 341 "alan.pmk"
 print(" "); idPrint(pmSeSt[pmStkP+1].string); 	break;}
    case 59: { /* <syntax_element> = '(' ID ')' <optional_indicators>; */
#line 342 "alan.pmk"
 print(" ("); idPrint(pmSeSt[pmStkP+2].string); print(")"); print(pmSeSt[pmStkP+4].string); 	break;}
    case 60: { /* <optional_indicators> =; */
#line 345 "alan.pmk"
 pmSeSt[pmStkP+1].string = malloc(1); strcpy(pmSeSt[pmStkP+1].string, ""); 	break;}
    case 61: { /* <optional_indicators> = <optional_indicators> <indicator>; */
#line 347 "alan.pmk"

	    pmSeSt[pmStkP+1].string = realloc(pmSeSt[pmStkP+1].string,
		strlen(pmSeSt[pmStkP+1].string) + strlen(pmSeSt[pmStkP+2].string) + 1);
	    strcat(pmSeSt[pmStkP+1].string, pmSeSt[pmStkP+2].string);
		break;}
    case 62: { /* <indicator> = '*'; */
#line 354 "alan.pmk"
 pmSeSt[pmStkP+1].string = "*"; 	break;}
    case 63: { /* <indicator> = '!'; */
#line 355 "alan.pmk"
 pmSeSt[pmStkP+1].string = "!"; 	break;}
    case 64: { /* <optional_class_restrictions> = '.'; */
#line 359 "alan.pmk"
 print("."); 	break;}
    case 65: { /* <optional_class_restrictions> = 'WHERE' __genSym#7 <class_restrictions>; */
#line 361 "alan.pmk"
 outdent(); outdent(); newline(); 	break;}
    case 66: { /* __genSym#7 =; */
#line 360 "alan.pmk"
 indent(); newline(); print("Where "); indent(); 	break;}
    case 69: { /* __genSym#8 =; */
#line 366 "alan.pmk"
 outdent(); newline(); print("And "); indent(); 	break;}
    case 70: { /* <class_restriction> = <class_restriction1> <class_restriction2>; */
#line 370 "alan.pmk"
 outdent(); 	break;}
    case 71: { /* <class_restriction1> = ID 'ISA'; */
#line 374 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); print(" Isa "); 	break;}
    case 73: { /* __genSym#9 =; */
#line 378 "alan.pmk"
 newline(); print("Else "); indent(); 	break;}
    case 74: { /* <classes> = <class_identifier>; */
#line 382 "alan.pmk"
 print(pmSeSt[pmStkP+1].string); 	break;}
    case 75: { /* <classes> = <classes> 'OR' <class_identifier>; */
#line 383 "alan.pmk"
 print(" ****OR**** "); idPrint(pmSeSt[pmStkP+3].string); 	break;}
    case 76: { /* <class_identifier> = 'OBJECT'; */
#line 387 "alan.pmk"
 pmSeSt[pmStkP+1].string = "object"; 	break;}
    case 77: { /* <class_identifier> = 'ACTOR'; */
#line 388 "alan.pmk"
 pmSeSt[pmStkP+1].string = "actor"; 	break;}
    case 78: { /* <class_identifier> = 'CONTAINER'; */
#line 389 "alan.pmk"
 pmSeSt[pmStkP+1].string = "container"; 	break;}
    case 81: { /* <class_identifier> = 'CONTAINER' 'OBJECT'; */
#line 390 "alan.pmk"
 pmSeSt[pmStkP+1].string = "**** container object ****"; 	break;}
    case 82: { /* <class_identifier> = 'CONTAINER' 'ACTOR'; */
#line 391 "alan.pmk"
 pmSeSt[pmStkP+1].string = "**** container actor ****"; 	break;}
    case 79: { /* <class_identifier> = 'INTEGER'; */
#line 392 "alan.pmk"
 pmSeSt[pmStkP+1].string = "integer"; 	break;}
    case 80: { /* <class_identifier> = 'STRING'; */
#line 393 "alan.pmk"
 pmSeSt[pmStkP+1].string = "string"; 	break;}
    case 86: { /* <verb_header> = 'VERB' __genSym#10 <id_list>; */
#line 405 "alan.pmk"
 indent(); 	break;}
    case 87: { /* __genSym#10 =; */
#line 405 "alan.pmk"
 newline(); print("Verb "); 	break;}
    case 92: { /* <verb_alternative> = <verb_alternative1> <simple_verb_body>; */
#line 416 "alan.pmk"
 outdent(); 	break;}
    case 93: { /* <verb_alternative1> = 'WHEN' ID; */
#line 419 "alan.pmk"
 newline(); print("When "); idPrint(pmSeSt[pmStkP+2].string); indent(); 	break;}
    case 95: { /* <verb_tail> = 'END' 'VERB' __genSym#11 <optional_id> '.'; */
#line 425 "alan.pmk"
 print("."); newline(); 	break;}
    case 96: { /* __genSym#11 =; */
#line 425 "alan.pmk"
 outdent(); newline(); print("End Verb"); 	break;}
    case 98: { /* <optional_checks> = <check1> <statements>; */
#line 431 "alan.pmk"
 outdent(); 	break;}
    case 99: { /* <optional_checks> = <check1> <check_list>; */
#line 432 "alan.pmk"
 outdent(); 	break;}
    case 100: { /* <check1> = 'CHECK'; */
#line 436 "alan.pmk"
 newline(); print("Check "); indent(); 	break;}
    case 103: { /* __genSym#12 =; */
#line 440 "alan.pmk"
 outdent(); newline(); print("And "); indent(); 	break;}
    case 104: { /* <check> = <expression> 'ELSE' __genSym#13 <statements>; */
#line 443 "alan.pmk"
 outdent(); 	break;}
    case 105: { /* __genSym#13 =; */
#line 443 "alan.pmk"
 newline(); print("Else "); indent(); 	break;}
    case 108: { /* <does> = 'DOES' __genSym#14 <optional_qual> <statements>; */
#line 453 "alan.pmk"
 outdent(); 	break;}
    case 109: { /* __genSym#14 =; */
#line 453 "alan.pmk"
 newline(); print("Does"); indent(); 	break;}
    case 110: { /* <location> = <location_header> <location_body> <location_tail>; */
#line 458 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); print("."); 	break;}
    case 111: { /* <location_header> = <location_id> <optional_name>; */
#line 463 "alan.pmk"
 pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+1].string; 	break;}
    case 112: { /* <location_id> = 'LOCATION' ID; */
#line 468 "alan.pmk"

	newline(); newline();
	print("The "); idPrint(pmSeSt[pmStkP+2].string); print(" Isa location");
	indent(); newline();
	pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+2].string;
    	break;}
    case 117: { /* <location_body_part> = __genSym#15 <is> __genSym#16 <attributes>; */
#line 482 "alan.pmk"
 outdent(); 	break;}
    case 120: { /* __genSym#15 =; */
#line 482 "alan.pmk"
 newline(); 	break;}
    case 121: { /* __genSym#16 =; */
#line 482 "alan.pmk"
 indent(); 	break;}
    case 122: { /* <location_does> = 'DOES' __genSym#17 <optional_qual> <statements>; */
#line 490 "alan.pmk"
 outdent(); 	break;}
    case 123: { /* __genSym#17 =; */
#line 490 "alan.pmk"
 newline(); print("Entered"); indent(); 	break;}
    case 124: { /* <location_tail> = 'END' 'LOCATION' <optional_id> '.'; */
#line 495 "alan.pmk"
 outdent(); newline(); print("End The "); 	break;}
    case 127: { /* <exit> = 'EXIT' __genSym#18 <id_list> <to_id> <optional_exit_body> '.'; */
#line 505 "alan.pmk"
 print("."); newline(); 	break;}
    case 128: { /* __genSym#18 =; */
#line 504 "alan.pmk"
 newline(); print("Exit "); 	break;}
    case 129: { /* <to_id> = 'TO' ID; */
#line 508 "alan.pmk"
 print(" To "); idPrint(pmSeSt[pmStkP+2].string); 	break;}
    case 131: { /* <optional_exit_body> = __genSym#19 <optional_checks> <optional_does> 'END' 'EXIT' <optional_id>; */
#line 513 "alan.pmk"
 outdent(); newline(); print("End Exit"); if (pmSeSt[pmStkP+6].string[0] != '\0') {print(" "); idPrint(pmSeSt[pmStkP+6].string); } 	break;}
    case 132: { /* __genSym#19 =; */
#line 512 "alan.pmk"
 indent(); 	break;}
    case 133: { /* <object> = <object_header> <object_body> <object_tail>; */
#line 519 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); print("."); 	break;}
    case 134: { /* <object_header> = <object_id> <optional_where> <optional_names> <optional_where>; */
#line 524 "alan.pmk"
 pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+1].string;	break;}
    case 135: { /* <object_id> = 'OBJECT' ID; */
#line 529 "alan.pmk"

	newline(); newline(); print("The "); idPrint(pmSeSt[pmStkP+2].string); print(" Isa object");
	indent(); newline();
	pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+2].string;
    	break;}
    case 142: { /* <object_body_part> = __genSym#20 <is> __genSym#21 <attributes>; */
#line 543 "alan.pmk"
 outdent(); 	break;}
    case 144: { /* __genSym#20 =; */
#line 543 "alan.pmk"
 newline(); 	break;}
    case 145: { /* __genSym#21 =; */
#line 543 "alan.pmk"
 indent(); 	break;}
    case 146: { /* <object_tail> = 'END' 'OBJECT' <optional_id> '.'; */
#line 552 "alan.pmk"
 outdent(); newline(); print("End The "); 	break;}
    case 148: { /* <optional_attributes> = <optional_attributes> <is> __genSym#22 <attributes>; */
#line 560 "alan.pmk"
 outdent(); 	break;}
    case 149: { /* __genSym#22 =; */
#line 560 "alan.pmk"
 indent(); newline(); 	break;}
    case 150: { /* <is> = 'IS'; */
#line 563 "alan.pmk"
 print("Is "); 	break;}
    case 151: { /* <is> = 'ARE'; */
#line 564 "alan.pmk"
 print("Are "); 	break;}
    case 152: { /* <is> = 'HAS'; */
#line 565 "alan.pmk"
 print("Has "); 	break;}
    case 155: { /* <description> = 'DESCRIPTION'; */
#line 573 "alan.pmk"
 newline(); print("Description"); newline(); 	break;}
    case 156: { /* <description> = 'DESCRIPTION' __genSym#23 <statements>; */
#line 574 "alan.pmk"
 outdent(); newline(); 	break;}
    case 157: { /* __genSym#23 =; */
#line 574 "alan.pmk"
 newline(); print("Description"); indent(); 	break;}
    case 159: { /* <article> = 'ARTICLE' __genSym#24 <statements>; */
#line 578 "alan.pmk"
 outdent(); newline(); 	break;}
    case 160: { /* __genSym#24 =; */
#line 578 "alan.pmk"
 newline(); print("Article"); indent(); 	break;}
    case 161: { /* <mentioned> = 'MENTIONED' __genSym#25 <statements>; */
#line 581 "alan.pmk"
 outdent(); newline(); 	break;}
    case 162: { /* __genSym#25 =; */
#line 581 "alan.pmk"
 newline(); print("Mentioned"); indent(); 	break;}
    case 167: { /* <name> = 'NAME' __genSym#26 <ids>; */
#line 592 "alan.pmk"
 newline(); 	break;}
    case 168: { /* __genSym#26 =; */
#line 592 "alan.pmk"
 print("Name "); 	break;}
    case 169: { /* <properties> = 'CONTAINER' __genSym#27 <container_body>; */
#line 597 "alan.pmk"
 outdent(); 	break;}
    case 170: { /* __genSym#27 =; */
#line 597 "alan.pmk"
 newline(); print("With Container"); indent(); 	break;}
    case 171: { /* <container> = <container_header> <container_body> <container_tail>; */
#line 601 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); print("."); newline(); 	break;}
    case 172: { /* <container_header> = 'CONTAINER' ID; */
#line 604 "alan.pmk"
 newline(); print("The "); idPrint(pmSeSt[pmStkP+2].string);
			indent(); newline();
			print("Container");
			pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+2].string; indent(); 	break;}
    case 174: { /* <container_tail> = 'END' 'CONTAINER' <optional_id> '.'; */
#line 614 "alan.pmk"
 outdent(); outdent(); newline(); print("End The "); 	break;}
    case 176: { /* <optional_limits> = 'LIMITS' __genSym#28 <limits>; */
#line 618 "alan.pmk"
 outdent(); 	break;}
    case 177: { /* __genSym#28 =; */
#line 618 "alan.pmk"
 newline(); print("Limits"); indent(); 	break;}
    case 180: { /* <limit> = <limit_attribute> 'THEN' __genSym#29 <statements>; */
#line 625 "alan.pmk"
 outdent(); outdent(); 	break;}
    case 181: { /* __genSym#29 =; */
#line 625 "alan.pmk"
 indent(); newline(); print("Else"); indent(); 	break;}
    case 183: { /* <limit_attribute> = 'COUNT' Integer; */
#line 629 "alan.pmk"
 newline(); print("Count "); print(pmSySt[pmStkP+2].chars); 	break;}
    case 185: { /* <optional_header> = 'HEADER' __genSym#30 <statements>; */
#line 633 "alan.pmk"
 outdent(); 	break;}
    case 186: { /* __genSym#30 =; */
#line 633 "alan.pmk"
 newline(); print("Header"); indent(); 	break;}
    case 188: { /* <optional_empty> = 'ELSE' __genSym#31 <statements>; */
#line 637 "alan.pmk"
 outdent(); 	break;}
    case 189: { /* __genSym#31 =; */
#line 637 "alan.pmk"
 newline(); print("Else"); indent(); 	break;}
    case 190: { /* <event> = <event_header> __genSym#32 <statements> <event_tail>; */
#line 642 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); print("."); newline(); 	break;}
    case 191: { /* __genSym#32 =; */
#line 642 "alan.pmk"
 indent(); 	break;}
    case 192: { /* <event_header> = 'EVENT' ID; */
#line 646 "alan.pmk"
 newline(); print("Event "); idPrint(pmSeSt[pmStkP+2].string);
	pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+2].string; 	break;}
    case 193: { /* <event_tail> = 'END' 'EVENT' <optional_id> '.'; */
#line 651 "alan.pmk"
 outdent(); newline(); print("End Event "); 	break;}
    case 194: { /* <actor> = <actor_header> <actor_body> <actor_tail>; */
#line 657 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); print("."); newline(); 	break;}
    case 195: { /* <actor_header> = <actor_id> <optional_where> <optional_names> <optional_where>; */
#line 662 "alan.pmk"
 pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+1].string; 	break;}
    case 196: { /* <actor_id> = 'ACTOR' ID; */
#line 667 "alan.pmk"

	newline(); newline(); print("The "); idPrint(pmSeSt[pmStkP+2].string); print(" Isa actor");
	indent(); newline();
	pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+2].string;
    	break;}
    case 201: { /* <actor_body_part> = __genSym#33 <is> __genSym#34 <attributes>; */
#line 681 "alan.pmk"
 outdent(); 	break;}
    case 204: { /* __genSym#33 =; */
#line 681 "alan.pmk"
 newline(); 	break;}
    case 205: { /* __genSym#34 =; */
#line 681 "alan.pmk"
 indent(); 	break;}
    case 206: { /* <actor_tail> = 'END' 'ACTOR' <optional_id> '.'; */
#line 688 "alan.pmk"
 outdent(); newline(); print("End The "); 	break;}
    case 210: { /* <script1> = 'SCRIPT' <integer_or_id> '.'; */
#line 703 "alan.pmk"
 newline(); print("Script "); print(pmSeSt[pmStkP+2].string); print("."); indent(); 	break;}
    case 211: { /* <script2> = <optional_description> <step_list>; */
#line 707 "alan.pmk"
 outdent(); 	break;}
    case 214: { /* <step> = 'STEP' __genSym#35 <statements>; */
#line 716 "alan.pmk"
 outdent(); 	break;}
    case 217: { /* __genSym#35 =; */
#line 716 "alan.pmk"
 newline(); print("Step"); indent(); 	break;}
    case 215: { /* <step> = <step_after_integer> <statements>; */
#line 717 "alan.pmk"
 outdent(); 	break;}
    case 216: { /* <step> = 'STEP' 'WAIT' 'UNTIL' __genSym#36 <expression> <statements>; */
#line 718 "alan.pmk"
 outdent(); 	break;}
    case 218: { /* __genSym#36 =; */
#line 718 "alan.pmk"
 newline(); print("Step Wait Until "); indent(); 	break;}
    case 219: { /* <step_after_integer> = 'STEP' 'AFTER' Integer; */
#line 722 "alan.pmk"
 newline(); print("Step After "); print(pmSySt[pmStkP+3].chars); indent(); 	break;}
    case 220: { /* <rule> = 'WHEN' __genSym#37 <expression> '=>' __genSym#38 <statements>; */
#line 727 "alan.pmk"
 outdent(); newline(); 	break;}
    case 221: { /* __genSym#37 =; */
#line 727 "alan.pmk"
 newline(); print("When "); 	break;}
    case 222: { /* __genSym#38 =; */
#line 727 "alan.pmk"
 print(" Then"); indent(); 	break;}
    case 224: { /* __genSym#39 =; */
#line 732 "alan.pmk"
 newline(); newline(); print("Start "); 	break;}
    case 225: { /* __genSym#40 =; */
#line 732 "alan.pmk"
 print("."); indent(); 	break;}
    case 237: { /* <fullstop> = '.'; */
#line 757 "alan.pmk"
 print("."); 	break;}
    case 238: { /* <output_statement> = STRING; */
#line 762 "alan.pmk"
 stringPrint(pmSySt[pmStkP+1].chars); 	break;}
    case 242: { /* __genSym#41 =; */
#line 763 "alan.pmk"
 newline(); print("Describe "); 	break;}
    case 243: { /* __genSym#42 =; */
#line 764 "alan.pmk"
 newline(); print("Say "); 	break;}
    case 244: { /* __genSym#43 =; */
#line 765 "alan.pmk"
 newline(); print("List "); 	break;}
    case 245: { /* <special_statement> = 'QUIT' '.'; */
#line 769 "alan.pmk"
 newline(); print("Quit."); 	break;}
    case 246: { /* <special_statement> = 'LOOK' '.'; */
#line 770 "alan.pmk"
 newline(); print("Look."); 	break;}
    case 247: { /* <special_statement> = 'SAVE' '.'; */
#line 771 "alan.pmk"
 newline(); print("Save."); 	break;}
    case 248: { /* <special_statement> = 'RESTORE' '.'; */
#line 772 "alan.pmk"
 newline(); print("Restore."); 	break;}
    case 249: { /* <special_statement> = 'RESTART' '.'; */
#line 773 "alan.pmk"
 newline(); print("Restart."); 	break;}
    case 250: { /* <special_statement> = 'SCORE' __genSym#44 <optional_integer> '.'; */
#line 774 "alan.pmk"
 print("."); 	break;}
    case 253: { /* __genSym#44 =; */
#line 774 "alan.pmk"
 newline(); print("Score "); 	break;}
    case 251: { /* <special_statement> = 'VISITS' Integer '.'; */
#line 775 "alan.pmk"
 newline(); print("Visits "); print(pmSySt[pmStkP+2].chars); print("."); 	break;}
    case 252: { /* <special_statement> = 'SYSTEM' STRING '.'; */
#line 776 "alan.pmk"
 newline(); print("****System**** "); stringPrint(pmSySt[pmStkP+2].chars); 	break;}
    case 256: { /* __genSym#45 =; */
#line 782 "alan.pmk"
 newline(); print("Empty "); 	break;}
    case 257: { /* __genSym#46 =; */
#line 782 "alan.pmk"
 print(" "); 	break;}
    case 258: { /* __genSym#47 =; */
#line 783 "alan.pmk"
 newline(); print("Locate "); 	break;}
    case 259: { /* __genSym#48 =; */
#line 783 "alan.pmk"
 print(" "); 	break;}
    case 262: { /* __genSym#49 =; */
#line 788 "alan.pmk"
 print("After "); 	break;}
    case 260: { /* <event_statement> = 'CANCEL' ID '.'; */
#line 790 "alan.pmk"
 newline(); print("Cancel "); idPrint(pmSeSt[pmStkP+2].string); print("."); 	break;}
    case 263: { /* <schedule1> = 'SCHEDULE' ID; */
#line 794 "alan.pmk"
 newline(); print("Schedule "); idPrint(pmSeSt[pmStkP+2].string); print(" "); 	break;}
    case 268: { /* __genSym#50 =; */
#line 799 "alan.pmk"
 newline(); print("Make "); 	break;}
    case 269: { /* __genSym#51 =; */
#line 799 "alan.pmk"
 print(" "); 	break;}
    case 270: { /* __genSym#52 =; */
#line 800 "alan.pmk"
 newline(); print("Set "); 	break;}
    case 271: { /* __genSym#53 =; */
#line 800 "alan.pmk"
 print(" To "); 	break;}
    case 272: { /* __genSym#54 =; */
#line 801 "alan.pmk"
 newline(); print("Increase "); 	break;}
    case 273: { /* __genSym#55 =; */
#line 802 "alan.pmk"
 newline(); print("Decrease "); 	break;}
    case 276: { /* __genSym#56 =; */
#line 807 "alan.pmk"
 print(" By "); 	break;}
    case 279: { /* <if_statement> = 'IF' __genSym#57 <expression> 'THEN' __genSym#58 <statements> <optional_elsif_list> <optional_else_part> 'END' 'IF' '.'; */
#line 820 "alan.pmk"
 outdent(); newline(); print("End If."); 	break;}
    case 280: { /* __genSym#57 =; */
#line 817 "alan.pmk"
 newline(); print("If "); 	break;}
    case 281: { /* __genSym#58 =; */
#line 818 "alan.pmk"
 print(" Then"); indent(); 	break;}
    case 287: { /* __genSym#59 =; */
#line 834 "alan.pmk"
 outdent(); newline(); print("Elsif "); 	break;}
    case 288: { /* __genSym#60 =; */
#line 835 "alan.pmk"
 print(" Then "); indent(); 	break;}
    case 291: { /* __genSym#61 =; */
#line 841 "alan.pmk"
 outdent(); newline(); print("Else "); indent(); 	break;}
    case 292: { /* <depending_statement> = 'DEPENDING' 'ON' __genSym#62 <primary> __genSym#63 <depend_cases> 'END' 'DEPEND' '.'; */
#line 849 "alan.pmk"
 outdent(); newline(); print("End Depend."); 	break;}
    case 293: { /* __genSym#62 =; */
#line 846 "alan.pmk"
 newline(); print("Depending On "); 	break;}
    case 294: { /* __genSym#63 =; */
#line 847 "alan.pmk"
 indent(); newline(); 	break;}
    case 297: { /* __genSym#64 =; */
#line 854 "alan.pmk"
 newline(); 	break;}
    case 299: { /* <depend_case> = <right_hand_side> ':' __genSym#65 <statements>; */
#line 858 "alan.pmk"
 outdent(); 	break;}
    case 300: { /* __genSym#65 =; */
#line 858 "alan.pmk"
 print(" Then "); indent(); 	break;}
    case 298: { /* <depend_case> = 'ELSE' __genSym#66 <statements>; */
#line 859 "alan.pmk"
 outdent(); 	break;}
    case 301: { /* __genSym#66 =; */
#line 859 "alan.pmk"
 print("Else "); indent(); 	break;}
    case 303: { /* __genSym#67 =; */
#line 864 "alan.pmk"
 newline(); print("Use Script "); 	break;}
    case 304: { /* <script_reference> = <integer_or_id>; */
#line 868 "alan.pmk"
 print(pmSeSt[pmStkP+1].string); 	break;}
    case 306: { /* <optional_for_actor> = 'FOR' ID; */
#line 873 "alan.pmk"
 print(" For "); idPrint(pmSeSt[pmStkP+2].string); 	break;}
    case 309: { /* __genSym#68 =; */
#line 879 "alan.pmk"
 print(" Or "); 	break;}
    case 312: { /* __genSym#69 =; */
#line 884 "alan.pmk"
 print(" And "); 	break;}
    case 315: { /* __genSym#70 =; */
#line 889 "alan.pmk"
 print(" "); 	break;}
    case 322: { /* __genSym#71 =; */
#line 897 "alan.pmk"
 print(" Between "); 	break;}
    case 323: { /* __genSym#72 =; */
#line 897 "alan.pmk"
 print(" And "); 	break;}
    case 324: { /* __genSym#73 =; */
#line 898 "alan.pmk"
 print(" Contains "); 	break;}
    case 330: { /* <primary> = '(' __genSym#74 <expression> ')'; */
#line 902 "alan.pmk"
 print(")"); 	break;}
    case 333: { /* __genSym#74 =; */
#line 902 "alan.pmk"
 print("("); 	break;}
    case 325: { /* <primary> = <optional_minus> Integer; */
#line 903 "alan.pmk"
 print(pmSeSt[pmStkP+1].string); print(pmSySt[pmStkP+2].chars); 	break;}
    case 326: { /* <primary> = STRING; */
#line 904 "alan.pmk"
 stringPrint(pmSySt[pmStkP+1].chars); 	break;}
    case 334: { /* __genSym#75 =; */
#line 907 "alan.pmk"
 print("Isa object, "); 	break;}
    case 335: { /* __genSym#76 =; */
#line 908 "alan.pmk"
 print("Random "); 	break;}
    case 336: { /* __genSym#77 =; */
#line 908 "alan.pmk"
 print(" To "); 	break;}
    case 328: { /* <primary> = 'SCORE'; */
#line 909 "alan.pmk"
 print("Score"); 	break;}
    case 338: { /* <aggregate> = 'SUM' 'OF' ID; */
#line 913 "alan.pmk"
 print(" (Sum Of "); idPrint(pmSeSt[pmStkP+3].string); 	break;}
    case 339: { /* <aggregate> = 'MAX' 'OF' ID; */
#line 914 "alan.pmk"
 print(" (Max Of "); idPrint(pmSeSt[pmStkP+3].string); 	break;}
    case 337: { /* <aggregate> = 'COUNT'; */
#line 915 "alan.pmk"
 print(" Count "); 	break;}
    case 340: { /* <something> = <optional_not> ID; */
#line 920 "alan.pmk"
 idPrint(pmSeSt[pmStkP+2].string); 	break;}
    case 341: { /* <what> = 'OBJECT'; */
#line 924 "alan.pmk"
 print("object"); 	break;}
    case 342: { /* <what> = 'LOCATION'; */
#line 925 "alan.pmk"
 print("Current Location"); 	break;}
    case 343: { /* <what> = 'ACTOR'; */
#line 926 "alan.pmk"
 print("Current Actor"); 	break;}
    case 344: { /* <what> = ID; */
#line 927 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); 	break;}
    case 346: { /* <optional_where> = <where>; */
#line 933 "alan.pmk"
 print(" "); 	break;}
    case 347: { /* <where> = 'HERE'; */
#line 937 "alan.pmk"
 print("Here"); 	break;}
    case 348: { /* <where> = 'NEARBY'; */
#line 938 "alan.pmk"
 print("Nearby"); 	break;}
    case 351: { /* __genSym#78 =; */
#line 939 "alan.pmk"
 print("At "); 	break;}
    case 352: { /* __genSym#79 =; */
#line 940 "alan.pmk"
 print("In "); 	break;}
    case 353: { /* <binop> = '+'; */
#line 944 "alan.pmk"
 print("+"); 	break;}
    case 354: { /* <binop> = '-'; */
#line 945 "alan.pmk"
 print("-"); 	break;}
    case 355: { /* <binop> = '*'; */
#line 946 "alan.pmk"
 print("*"); 	break;}
    case 356: { /* <binop> = '/'; */
#line 947 "alan.pmk"
 print("/"); 	break;}
    case 357: { /* <relop> = '<>'; */
#line 951 "alan.pmk"
 print("<>"); 	break;}
    case 358: { /* <relop> = '='; */
#line 952 "alan.pmk"
 print("="); 	break;}
    case 359: { /* <relop> = '=='; */
#line 953 "alan.pmk"
 print("=="); 	break;}
    case 360: { /* <relop> = '>='; */
#line 954 "alan.pmk"
 print(">="); 	break;}
    case 361: { /* <relop> = '<='; */
#line 955 "alan.pmk"
 print("<="); 	break;}
    case 362: { /* <relop> = '>'; */
#line 956 "alan.pmk"
 print(">"); 	break;}
    case 363: { /* <relop> = '<'; */
#line 957 "alan.pmk"
 print("<"); 	break;}
    case 365: { /* <optional_qual> = 'BEFORE'; */
#line 962 "alan.pmk"
 print(" Before"); 	break;}
    case 366: { /* <optional_qual> = 'AFTER'; */
#line 963 "alan.pmk"
 print(" After"); 	break;}
    case 367: { /* <optional_qual> = 'ONLY'; */
#line 964 "alan.pmk"
 print(" Only"); 	break;}
    case 369: { /* <optional_not> = 'NOT'; */
#line 969 "alan.pmk"
 print("Not "); 	break;}
    case 370: { /* <optional_id> =; */
#line 973 "alan.pmk"
 pmSeSt[pmStkP+1].string = ""; 	break;}
    case 371: { /* <optional_id> = ID; */
#line 974 "alan.pmk"
 pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+1].string; 	break;}
    case 372: { /* <ids> = ID; */
#line 978 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); 	break;}
    case 373: { /* <ids> = <ids> ID; */
#line 979 "alan.pmk"
 print(" "); idPrint(pmSeSt[pmStkP+2].string); 	break;}
    case 374: { /* <id_list> = ID; */
#line 983 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); 	break;}
    case 375: { /* <id_list> = <id_list> ',' ID; */
#line 984 "alan.pmk"
 print(", "); idPrint(pmSeSt[pmStkP+3].string); 	break;}
    case 377: { /* <optional_integer> = Integer; */
#line 989 "alan.pmk"
 print(pmSySt[pmStkP+1].chars); 	break;}
    case 378: { /* <optional_minus> =; */
#line 993 "alan.pmk"
 pmSeSt[pmStkP+1].string = ""; 	break;}
    case 379: { /* <optional_minus> = '-'; */
#line 994 "alan.pmk"
 pmSeSt[pmStkP+1].string = "-"; 	break;}
    case 381: { /* <id_of> = ID 'OF'; */
#line 1002 "alan.pmk"
 idPrint(pmSeSt[pmStkP+1].string); print(" Of "); 	break;}
    case 382: { /* <integer_or_id> = Integer; */
#line 1006 "alan.pmk"
 pmSeSt[pmStkP+1].string = malloc(strlen(pmSySt[pmStkP+1].chars)+2);
		sprintf(pmSeSt[pmStkP+1].string, "s%s", pmSySt[pmStkP+1].chars); 	break;}
    case 383: { /* <integer_or_id> = ID; */
#line 1008 "alan.pmk"
 pmSeSt[pmStkP+1].string = pmSeSt[pmStkP+1].string; 	break;}
    case 384: { /* ID = IDENT; */
#line 1012 "alan.pmk"
 pmSeSt[pmStkP+1].string = pmSySt[pmStkP+1].chars; 	break;}
    case 385: { /* ID = 'DEFAULT'; */
#line 1013 "alan.pmk"
 pmSeSt[pmStkP+1].string = "default"; 	break;}
    case 386: { /* ID = 'ARTICLE'; */
#line 1014 "alan.pmk"
 pmSeSt[pmStkP+1].string = "article"; 	break;}
    case 387: { /* ID = 'MESSAGE'; */
#line 1015 "alan.pmk"
 pmSeSt[pmStkP+1].string = "message"; 	break;}
    case 388: { /* ID = 'QUIT'; */
#line 1016 "alan.pmk"
 pmSeSt[pmStkP+1].string = "quit"; 	break;}
    case 389: { /* ID = 'SAVE'; */
#line 1017 "alan.pmk"
 pmSeSt[pmStkP+1].string = "save"; 	break;}
    case 390: { /* ID = 'RESTORE'; */
#line 1018 "alan.pmk"
 pmSeSt[pmStkP+1].string = "restore"; 	break;}
    case 391: { /* ID = 'RESTART'; */
#line 1019 "alan.pmk"
 pmSeSt[pmStkP+1].string = "restart"; 	break;}
    case 392: { /* ID = 'WAIT'; */
#line 1020 "alan.pmk"
 pmSeSt[pmStkP+1].string = "wait"; 	break;}
    case 393: { /* ID = 'BETWEEN'; */
#line 1021 "alan.pmk"
 pmSeSt[pmStkP+1].string = "between"; 	break;}
    case 394: { /* ID = 'CONTAINS'; */
#line 1022 "alan.pmk"
 pmSeSt[pmStkP+1].string = "contains"; 	break;}
    case 395: { /* ID = 'ON'; */
#line 1023 "alan.pmk"
 pmSeSt[pmStkP+1].string = "on"; 	break;}
    case 396: { /* ID = 'IN'; */
#line 1024 "alan.pmk"
 pmSeSt[pmStkP+1].string = "in"; 	break;}
    case 397: { /* ID = 'AFTER'; */
#line 1025 "alan.pmk"
 pmSeSt[pmStkP+1].string = "after"; 	break;}
    case 398: { /* ID = 'BEFORE'; */
#line 1026 "alan.pmk"
 pmSeSt[pmStkP+1].string = "before"; 	break;}
    case 399: { /* ID = 'CHECK'; */
#line 1027 "alan.pmk"
 pmSeSt[pmStkP+1].string = "check"; 	break;}
    case 400: { /* ID = 'DEPEND'; */
#line 1028 "alan.pmk"
 pmSeSt[pmStkP+1].string = "depend"; 	break;}
    case 401: { /* ID = 'EXIT'; */
#line 1029 "alan.pmk"
 pmSeSt[pmStkP+1].string = "exit"; 	break;}
    case 402: { /* ID = 'FOR'; */
#line 1030 "alan.pmk"
 pmSeSt[pmStkP+1].string = "for"; 	break;}
    case 403: { /* ID = 'INTEGER'; */
#line 1031 "alan.pmk"
 pmSeSt[pmStkP+1].string = "integer"; 	break;}
    case 404: { /* ID = 'ISA'; */
#line 1032 "alan.pmk"
 pmSeSt[pmStkP+1].string = "isa"; 	break;}
    case 405: { /* ID = 'LIMITS'; */
#line 1033 "alan.pmk"
 pmSeSt[pmStkP+1].string = "limits"; 	break;}
    default: break; }
}/*pmPaSema()*/

