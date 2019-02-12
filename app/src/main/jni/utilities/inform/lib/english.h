! ==============================================================================
!   ENGLISH:  Language Definition File
!
!   Supplied for use with Inform 6 -- Release 6.12.1 -- Serial number 160605
!
!   Copyright Graham Nelson 1993-2004 and David Griffith 2012-2016
!
!   This code is licensed under either the traditional Inform license as
!   described by the DM4 or the Artistic License version 2.0.  See the
!   file COPYING in the distribution archive or at
!   https://github.com/DavidGriffith/inform6lib/
!
!   This file is automatically Included in your game file by "parserm".
!   Strictly, "parserm" includes the file named in the "language__" variable,
!   whose contents can be defined by+language_name=XXX compiler setting (with a
!   default of "english").
!
!   Define the constant DIALECT_US before including "Parser" to obtain American
!   English.
! ==============================================================================

System_file;

#Ifndef LIBRARY_ENGLISH;	! if this file is already included,
				! don't try to include it again.

! ------------------------------------------------------------------------------
!   Part I.   Preliminaries
! ------------------------------------------------------------------------------

Constant EnglishNaturalLanguage;    ! Needed to keep old pronouns mechanism

Class   CompassDirection
  with  number 0, article "the",
        description [;
            if (location provides compass_look && location.compass_look(self)) rtrue;
            if (self.compass_look()) rtrue;
            L__M(##Look, 7, self);
        ],
        compass_look false,
        parse_name [; return -1; ]
  has   scenery;

Object Compass "compass" has concealed;

#Ifndef WITHOUT_DIRECTIONS;
CompassDirection -> n_obj  with short_name "north",     door_dir n_to,
                                name 'n//' 'north';
CompassDirection -> s_obj  with short_name "south",     door_dir s_to,
                                name 's//' 'south';
CompassDirection -> e_obj  with short_name "east",      door_dir e_to,
                                name 'e//' 'east';
CompassDirection -> w_obj  with short_name "west",      door_dir w_to,
                                name 'w//' 'west';
CompassDirection -> ne_obj with short_name "northeast", door_dir ne_to,
                                name 'ne'  'northeast';
CompassDirection -> nw_obj with short_name "northwest", door_dir nw_to,
                                name 'nw'  'northwest';
CompassDirection -> se_obj with short_name "southeast", door_dir se_to,
                                name 'se'  'southeast';
CompassDirection -> sw_obj with short_name "southwest", door_dir sw_to,
                                name 'sw'  'southwest';
CompassDirection -> u_obj  with short_name "up above",  door_dir u_to,
                                name 'u//' 'up' 'ceiling' 'above' 'sky';
CompassDirection -> d_obj  with short_name "ground",    door_dir d_to,
                                name 'd//' 'down' 'floor' 'below' 'ground';
#endif; ! WITHOUT_DIRECTIONS

CompassDirection -> in_obj  with short_name "inside",  door_dir in_to;
CompassDirection -> out_obj with short_name "outside", door_dir out_to;

! ------------------------------------------------------------------------------
!   Part II.   Vocabulary
! ------------------------------------------------------------------------------

Constant AGAIN1__WD     = 'again';
Constant AGAIN2__WD     = 'g//';
Constant AGAIN3__WD     = 'again';
Constant OOPS1__WD      = 'oops';
Constant OOPS2__WD      = 'o//';
Constant OOPS3__WD      = 'oops';
Constant UNDO1__WD      = 'undo';
Constant UNDO2__WD      = 'undo';
Constant UNDO3__WD      = 'undo';

Constant ALL1__WD       = 'all';
Constant ALL2__WD       = 'each';
Constant ALL3__WD       = 'every';
Constant ALL4__WD       = 'everything';
Constant ALL5__WD       = 'both';
Constant AND1__WD       = 'and';
Constant AND2__WD       = 'and';
Constant AND3__WD       = 'and';
Constant BUT1__WD       = 'but';
Constant BUT2__WD       = 'except';
Constant BUT3__WD       = 'but';
Constant ME1__WD        = 'me';
Constant ME2__WD        = 'myself';
Constant ME3__WD        = 'self';
Constant OF1__WD        = 'of';
Constant OF2__WD        = 'of';
Constant OF3__WD        = 'of';
Constant OF4__WD        = 'of';
Constant OTHER1__WD     = 'another';
Constant OTHER2__WD     = 'other';
Constant OTHER3__WD     = 'other';
Constant THEN1__WD      = 'then';
Constant THEN2__WD      = 'then';
Constant THEN3__WD      = 'then';

Constant NO1__WD        = 'n//';
Constant NO2__WD        = 'no';
Constant NO3__WD        = 'no';
Constant YES1__WD       = 'y//';
Constant YES2__WD       = 'yes';
Constant YES3__WD       = 'yes';

Constant AMUSING__WD    = 'amusing';
Constant FULLSCORE1__WD = 'fullscore';
Constant FULLSCORE2__WD = 'full';
Constant QUIT1__WD      = 'q//';
Constant QUIT2__WD      = 'quit';
Constant RESTART__WD    = 'restart';
Constant RESTORE__WD    = 'restore';

Array LanguagePronouns table

  ! word        possible GNAs                   connected
  !             to follow:                      to:
  !             a     i
  !             s  p  s  p
  !             mfnmfnmfnmfn

    'it'      $$001000111000                    NULL
    'him'     $$100000100000                    NULL
    'her'     $$010000010000                    NULL
    'them'    $$000111000111                    NULL;

Array LanguageDescriptors table

  ! word        possible GNAs   descriptor      connected
  !             to follow:      type:           to:
  !             a     i
  !             s  p  s  p
  !             mfnmfnmfnmfn

    'my'      $$111111111111    POSSESS_PK      0
    'this'    $$111111111111    POSSESS_PK      0
    'these'   $$000111000111    POSSESS_PK      0
    'that'    $$111111111111    POSSESS_PK      1
    'those'   $$000111000111    POSSESS_PK      1
    'his'     $$111111111111    POSSESS_PK      'him'
    'her'     $$111111111111    POSSESS_PK      'her'
    'their'   $$111111111111    POSSESS_PK      'them'
    'its'     $$111111111111    POSSESS_PK      'it'
    'the'     $$111111111111    DEFART_PK       NULL
    'a//'     $$111000111000    INDEFART_PK     NULL
    'an'      $$111000111000    INDEFART_PK     NULL
    'some'    $$000111000111    INDEFART_PK     NULL
    'lit'     $$111111111111    light           NULL
    'lighted' $$111111111111    light           NULL
    'unlit'   $$111111111111    (-light)        NULL;

Array LanguageNumbers table
    'one' 1 'two' 2 'three' 3 'four' 4 'five' 5
    'six' 6 'seven' 7 'eight' 8 'nine' 9 'ten' 10
    'eleven' 11 'twelve' 12 'thirteen' 13 'fourteen' 14 'fifteen' 15
    'sixteen' 16 'seventeen' 17 'eighteen' 18 'nineteen' 19 'twenty' 20;

! ------------------------------------------------------------------------------
!   Part III.   Translation
! ------------------------------------------------------------------------------

[ LanguageToInformese;
];

! ------------------------------------------------------------------------------
!   Part IV.   Printing
! ------------------------------------------------------------------------------

Constant LanguageAnimateGender   = male;
Constant LanguageInanimateGender = neuter;

Constant LanguageContractionForms = 2;     ! English has two:
                                           ! 0 = starting with a consonant
                                           ! 1 = starting with a vowel

[ LanguageContraction text;
    if (text->0 == 'a' or 'e' or 'i' or 'o' or 'u'
                or 'A' or 'E' or 'I' or 'O' or 'U') return 1;
    return 0;
];

Array LanguageArticles -->

 !   Contraction form 0:     Contraction form 1:
 !   Cdef   Def    Indef     Cdef   Def    Indef

     "The " "the " "a "      "The " "the " "an "          ! Articles 0
     "The " "the " "some "   "The " "the " "some ";       ! Articles 1

                   !             a           i
                   !             s     p     s     p
                   !             m f n m f n m f n m f n

Array LanguageGNAsToArticles --> 0 0 0 1 1 1 0 0 0 1 1 1;

[ LanguageDirection d;
    switch (d) {
      n_to:    print "north";
      s_to:    print "south";
      e_to:    print "east";
      w_to:    print "west";
      ne_to:   print "northeast";
      nw_to:   print "northwest";
      se_to:   print "southeast";
      sw_to:   print "southwest";
      u_to:    print "up";
      d_to:    print "down";
      in_to:   print "in";
      out_to:  print "out";
      default: return RunTimeError(9,d);
    }
];

[ LanguageNumber n f;
    if (n == 0)    { print "zero"; rfalse; }
    if (n < 0)     { print "minus "; n = -n; }
    if (n >= 1000) { print (LanguageNumber) n/1000, " thousand"; n = n%1000; f = 1; }
    if (n >= 100)  {
        if (f == 1) print ", ";
        print (LanguageNumber) n/100, " hundred"; n = n%100; f = 1;
    }
    if (n == 0) rfalse;
    #Ifdef DIALECT_US;
    if (f == 1) print " ";
    #Ifnot;
    if (f == 1) print " and ";
    #Endif;
    switch (n) {
      1:    print "one";
      2:    print "two";
      3:    print "three";
      4:    print "four";
      5:    print "five";
      6:    print "six";
      7:    print "seven";
      8:    print "eight";
      9:    print "nine";
      10:   print "ten";
      11:   print "eleven";
      12:   print "twelve";
      13:   print "thirteen";
      14:   print "fourteen";
      15:   print "fifteen";
      16:   print "sixteen";
      17:   print "seventeen";
      18:   print "eighteen";
      19:   print "nineteen";
      20 to 99: switch (n/10) {
        2:  print "twenty";
        3:  print "thirty";
        4:  print "forty";
        5:  print "fifty";
        6:  print "sixty";
        7:  print "seventy";
        8:  print "eighty";
        9:  print "ninety";
        }
        if (n%10 ~= 0) print "-", (LanguageNumber) n%10;
    }
];

[ LanguageTimeOfDay hours mins i;
    i = hours%12;
    if (i == 0) i = 12;
    if (i < 10) print " ";
    print i, ":", mins/10, mins%10;
    if ((hours/12) > 0) print " pm"; else print " am";
];

[ LanguageVerb i;
    switch (i) {
      'i//','inv','inventory':
               print "take inventory";
      'l//':   print "look";
      'x//':   print "examine";
      'z//':   print "wait";
      default: rfalse;
    }
    rtrue;
];

! ----------------------------------------------------------------------------
!  LanguageVerbIsDebugging is called by SearchScope.  It should return true
!  if word w is a debugging verb which needs all objects to be in scope.
! ----------------------------------------------------------------------------

#Ifdef DEBUG;
[ LanguageVerbIsDebugging w;
    if (w == 'purloin' or 'tree' or 'abstract'
                       or 'gonear' or 'scope' or 'showobj')
        rtrue;
    rfalse;
];
#Endif;

! ----------------------------------------------------------------------------
!  LanguageVerbLikesAdverb is called by PrintCommand when printing an UPTO_PE
!  error or an inference message.  Words which are intransitive verbs, i.e.,
!  which require a direction name as an adverb ('walk west'), not a noun
!  ('I only understood you as far as wanting to touch /the/ ground'), should
!  cause the routine to return true.
! ----------------------------------------------------------------------------

[ LanguageVerbLikesAdverb w;
    if (w == 'look' or 'go' or 'push' or 'walk')
        rtrue;
    rfalse;
];

! ----------------------------------------------------------------------------
!  LanguageVerbMayBeName is called by NounDomain when dealing with the
!  player's reply to a "Which do you mean, the short stick or the long
!  stick?" prompt from the parser. If the reply is another verb (for example,
!  LOOK) then then previous ambiguous command is discarded /unless/
!  it is one of these words which could be both a verb /and/ an
!  adjective in a 'name' property.
! ----------------------------------------------------------------------------

[ LanguageVerbMayBeName w;
    if (w == 'long' or 'short' or 'normal'
                    or 'brief' or 'full' or 'verbose')
        rtrue;
    rfalse;
];

Constant NKEY__TX       = "N = next subject";
Constant PKEY__TX       = "P = previous";
Constant QKEY1__TX      = "  Q = resume game";
Constant QKEY2__TX      = "Q = previous menu";
Constant RKEY__TX       = "RETURN = read subject";

Constant NKEY1__KY      = 'N';
Constant NKEY2__KY      = 'n';
Constant PKEY1__KY      = 'P';
Constant PKEY2__KY      = 'p';
Constant QKEY1__KY      = 'Q';
Constant QKEY2__KY      = 'q';

Constant SCORE__TX      = "Score: ";
Constant MOVES__TX      = "Moves: ";
Constant TIME__TX       = "Time: ";
Constant CANTGO__TX     = "You can't go that way.";
Constant FORMER__TX     = "your former self";
Constant MYFORMER__TX   = "my former self";
Constant YOURSELF__TX   = "yourself";
Constant MYSELF__TX     = "myself";
Constant YOU__TX        = "You";
Constant DARKNESS__TX   = "Darkness";

Constant THOSET__TX     = "those things";
Constant THAT__TX       = "that";
Constant OR__TX         = " or ";
Constant NOTHING__TX    = "nothing";
Constant IS__TX         = " is";
Constant ARE__TX        = " are";
Constant IS2__TX        = "is ";
Constant ARE2__TX       = "are ";
Constant WAS__TX        = " was";
Constant WERE__TX       = " were";
Constant WAS2__TX       = "was ";
Constant WERE2__TX      = "were ";
Constant AND__TX        = " and ";
Constant WHOM__TX       = "whom ";
Constant WHICH__TX      = "which ";
Constant COMMA__TX      = ", ";
Constant COLON__TX      = ": ";

! ----------------------------------------------------------------------------
! FYI on nominative pronouns versus accusative pronouns...
! Consider the sentence "She hit him.".
! "She" is in the nominative case.  It appears at the beginning of a sentence.
! "him" is in the accusative case.  It won't appear at the beginning.
! ----------------------------------------------------------------------------

! Accusative
[ ThatOrThose obj;
    if (obj == player) {
	if (player provides narrative_voice) {
	    if (player.narrative_voice == 1) { print "me"; return; }
	    if (player.narrative_voice == 3) { CDefart(player); return; }
	}
	print "you";
	return;
    }
    if (obj has pluralname)       { print "those"; return; }
    if (obj has female)           { print "her"; return; }
    if (obj has male or animate)
        if (obj hasnt neuter)     { print "him"; return; }
    print "that";
];

! Accusative
[ ItOrThem obj;
    if (obj == player) {
	if (player provides narrative_voice) {
	    if (player.narrative_voice == 1) { print "myself"; return; }
	    if (player.narrative_voice == 3) { CDefart(player); return; }
	}
	print "yourself";
	return;
    }
    if (obj has pluralname)       { print "them"; return; }
    if (obj has female)           { print "her"; return; }
    if (obj has male or animate)
        if (obj hasnt neuter)     { print "him"; return; }
    print "it";
];

! Nominative
[ CThatOrThose obj;
    if (obj == player) {
	if (player provides narrative_voice) {
	    if (player.narrative_voice == 1) { print "I"; return; }
	    if (player.narrative_voice == 3) { CDefart(player); return; }
	}
	print "You";
	return;
    }
    if (obj has pluralname)		{ print "Those"; return; }
    if (obj has female)			{ print "She"; return; }
    if (obj has male or animate) {
        if (obj hasnt neuter)		{ print "He"; return; }
    }
    print "That";
];

! Nominative
[ CTheyreorThats obj;
    if (obj == player) {
	if (player provides narrative_voice) {
	    if (player.narrative_voice == 1) { print "I'm"; return; }
	    if (player.narrative_voice == 3) { CDefart(player); print "'s"; return; }
	}
	print "You're";
	return;
    }
    if (obj has pluralname)		{ print "They're"; return; }
    if (obj has female)			{ print "She's"; return; }
    if (obj has male or animate) {
        if (obj hasnt neuter)		{ print "He's"; return; }
    }
    print "That's";
];

[ IsOrAre obj;
    if (player provides narrative_tense && player.narrative_tense == PAST_TENSE) {
        if (obj has pluralname || obj == player) print "were"; else print "was";
        return;
    }
    if (obj has pluralname || obj == player) print "are"; else print "is";
    return;
];

[ nop x; x = x; ];      ! print rule to absorb unwanted return value

[ SubjectNotPlayer obj reportage v2 v3 past;
    if (past && player provides narrative_tense && player.narrative_tense == PAST_TENSE) {
        v2 = past;
        v3 = past;
    }
    if (reportage && actor ~= player) {
        L__M(##Miscellany, 60, actor);
        if (obj == actor) {
            print (theActor) obj, " ", (string) v3;
            return;
        }
        else
            if (obj has pluralname) {
                print (the) obj, " ", (string) v2;
                return;
            }
            else {print (the) obj, " ", (string) v3; return;}
    }
   else
        if (obj has pluralname) { print (The) obj, " ", (string) v2; return;}
        else                    { print (The) obj, " ", (string) v3; return;}
];

[ CSubjectVoice obj v1 v2 v3 past;
    if (past && player provides narrative_tense && player.narrative_tense == PAST_TENSE) {
        v1 = past;
        v2 = past;
        v3 = past;
    } else {
        if (v2 == 0) v2 = v1;
        if (v3 == 0) v3 = v1;
    }
    if (obj ~= player) { print (string) v3; return; }

    if (player provides narrative_voice) switch (player.narrative_voice) {
      1:  print (string) v1; return;
      2:  ! Do nothing.
      3:  print (string) v3; return;
      default: RunTimeError(16, player.narrative_voice);
    }

    print (string) v2; return;
];

[ CSubjectVerb obj reportage nocaps v1 v2 v3 past;
    if (past && player provides narrative_tense && player.narrative_tense == PAST_TENSE) {
        v1 = past;
        v2 = past;
        v3 = past;
    } else {
        if (v2 == 0) v2 = v1;
        if (v3 == 0) v3 = v1;
    }
    if (obj == player) {
        if (player provides narrative_voice) switch (player.narrative_voice) {
          1:  print "I ", (string) v1; return;
          2:  ! Do nothing.
          3:  CDefart(player);
              print " ", (string) v3; return;
          default: RunTimeError(16, player.narrative_voice);
        }
        if (nocaps) { print "you ", (string) v2; return; }
	print "You ", (string) v2; return;
    }
    SubjectNotPlayer(obj, reportage, v2, v3);
];

[ CSubjectIs obj reportage nocaps;
    if (obj == player) {
        if (player provides narrative_voice) switch (player.narrative_voice) {
          1:  Tense("I'm", "I was"); return;
          2:  ! Do nothing.
          3:  CDefart(player);
              Tense(" is", " was"); return;
          default: RunTimeError(16, player.narrative_voice);
        }
        if (nocaps) Tense("you're", "you were");
        else Tense("You're", "You were");
        return;
    }
    SubjectNotPlayer(obj, reportage, "are", "is", "was");
];

[ CSubjectIsnt obj reportage nocaps;
    if (obj == player) {
        if (player provides narrative_voice) switch (player.narrative_voice) {
          1:  Tense("I'm not", "I wasn't"); return;
          2:  ! Do nothing.
          3:  CDefart(player);
              Tense(" isn't", " wasn't"); return;
          default: RunTimeError(16, player.narrative_voice);
        }
        if (nocaps) Tense("you aren't", "you weren't");
        else Tense("You aren't", "You weren't");
	return;
    }
    SubjectNotPlayer(obj, reportage, "aren't", "isn't", "wasn't");
];

[ CSubjectHas obj reportage nocaps;
    if (obj == player) {
        if (player provides narrative_voice) switch (player.narrative_voice) {
          1:  Tense("I've", "I had"); return;
          2:  ! Do nothing.
          3:  CDefart(player);
              Tense(" has", " had"); return;
          default: RunTimeError(16, player.narrative_voice);
        }
        if (nocaps) Tense("you've", "you'd");
        else Tense("You've", "You'd");
        return;
    }
    SubjectNotPlayer(obj, reportage, "have", "has", "had");
];

[ CSubjectWill obj reportage nocaps;
    if (obj == player) {
        if (player provides narrative_voice) switch (player.narrative_voice) {
          1:  Tense("I'll", "I would've"); return;
          2:  ! Do nothing.
          3:  CDefart(player);
              Tense(" will", " would've"); return;
          default: RunTimeError(16, player.narrative_voice);
        }
        if (nocaps) Tense("you'll", "you'd");
        else Tense("You'll", "You'd");
        return;
    }
    SubjectNotPlayer(obj, reportage, "will", "will", "would");
];

[ CSubjectCan obj reportage nocaps;
    CSubjectVerb(obj, reportage, nocaps, "can", 0, "can", "could");
];

[ CSubjectCant obj reportage nocaps;
    CSubjectVerb(obj, reportage, nocaps, "can't", 0, "can't", "couldn't");
];

[ CSubjectDont obj reportage nocaps;
    CSubjectVerb(obj, reportage, nocaps, "don't", 0, "doesn't", "didn't");
];


[ OnesSelf obj;
    if (obj == player) {
        if (player provides narrative_voice) switch(player.narrative_voice) {
            1:  print (string) MYSELF__TX; return;
            2:  ! Do nothing.
            3:  if (obj has female) {print "herself"; return;}
                print "himself"; return;
          default: RunTimeError(16, player.narrative_voice);
        }
        print "yourself"; return;
    }
    if (obj has male) { print "himself"; return; }
    if (obj has female) {print "herself"; return; }
    print "itself"; return;
];


[ Possessive obj caps;
    if (obj == player) {
        if (player provides narrative_voice) switch(player.narrative_voice) {
          1:  if (caps) print "M"; else print "m"; print "y"; return;
          2:  ! Do nothing.
          3:  CDefart(player);
              print "'s"; return;
          default: RunTimeError(16, player.narrative_voice);
        }
        if (caps) print "Y"; else print "y";
        print "our"; return;
    }
    if (caps) print "H"; else print "h";
    if (obj has male) { print "is"; return; }
    if (obj has female) { print "er"; return; }
    if (caps) print "I"; else { print "i"; print "ts"; return; }
];

[ PossessiveCaps obj;
    Possessive(obj, true);
];

[ theActor obj;
    if (obj == player) {
        if (obj provides narrative_voice) {
            switch (obj.narrative_voice) {
              1:  print "I"; return;
              2:  ! Do nothing.
              3:  if (obj has neuter) { print "it"; return; }
                  if (obj has female) { print "she"; return; }
                  print "he"; return;
              default: RunTimeError(16, player.narrative_voice);
            }
        }
        print "you"; return;
    }
    if (obj has pluralname)       { print "they"; return; }
    if (obj has female)           { print "she"; return; }
    if (obj has male or animate)
        if (obj hasnt neuter)     { print "he"; return; }
                                    print "that";
];

[ SupportObj obj s1 s2;
    if (obj has supporter)          print (string) s1;
    else                            print (string) s2;
];

[ PluralObj obj s1 s2 past;
    if (player provides narrative_tense && player.narrative_tense == PAST_TENSE) {
        print (string) past;
        return;
    }
    if (obj has pluralname)         print (string) s1;
    else                            print (string) s2;
];

! ----------------------------------------------------------------------------
! Tense is a little helper function to present the correct tense of a
! verb.  The first parameter is the verb in present tense.  The second
! parameter is the verb in past tense.  If the second parameter is
! omitted, then nothing will be printed if the appropriate tense is past.
! ----------------------------------------------------------------------------
[ Tense present past;
    if (player provides narrative_tense && player.narrative_tense == PAST_TENSE) {
        if (past == false) return;
        print (string) past;
    }
    else
        print (string) present;
];

[ DecideAgainst;
    CSubjectVerb(actor, false, false, "decide",0,"decides","decided");
    print " that";
    Tense("'s not", " wasn't");
    " such a good idea.";
];

#Ifdef TARGET_ZCODE;

[ LowerCase c;    ! for ZSCII matching ISO 8859-1
   switch (c) {
     'A' to 'Z':                            c = c + 32;
     202, 204, 212, 214, 221:               c--;
     217, 218:                              c = c - 2;
     158 to 160, 167, 168, 208 to 210:      c = c - 3;
     186 to 190, 196 to 200:                c = c - 5 ;
     175 to 180:                            c = c - 6;
   }
   return c;
];

[ UpperCase c;    ! for ZSCII matching ISO 8859-1
   switch (c) {
     'a' to 'z':                            c = c - 32;
     201, 203, 211, 213, 220:               c++;
     215, 216:                              c = c + 2;
     155 to 157, 164, 165, 205 to 207:      c = c + 3;
     181 to 185, 191 to 195:                c = c + 5 ;
     169 to 174:                            c = c + 6;
   }
   return c;
];

#Ifnot; ! TARGET_GLULX

[ LowerCase c; return glk_char_to_lower(c); ];
[ UpperCase c; return glk_char_to_upper(c); ];

#Endif; ! TARGET_



[ LanguageLM n x1 x2;
  Answer,Ask:
            print "There ";
            Tense("is", "was");
            " no reply.";
! Ask:      see Answer
  Attack:   print "Violence ";
            Tense("isn't", "wasn't");
            " the answer to this one.";
  Blow:     CSubjectCant(actor,true);
            " usefully blow ", (thatorthose) x1, ".";
  Burn: switch (n) {
        1:  print "This dangerous act would ";
            Tense("achieve", "have achieved");
            " little.";
        2:  DecideAgainst();
    }
  Buy:      print "Nothing ";
            Tense("is", "was");
            " on sale.";
  Climb: switch (n) {
        1:  print "Climbing ", (ThatOrThose) x1, " would ";
            Tense("achieve", "have achieved");
            " little.";
        2:  DecideAgainst();
    }
  Close: switch (n) {
        1:  CSubjectIs(x1,true);
            print " not something ", (theActor) actor;
            Tense(" can close", " could have closed");
            ".";
        2:  CSubjectIs(x1,true); " already closed.";
        3:  CSubjectVerb(actor,false,false,"close",0,"closes","closed");
            " ", (the) x1, ".";
        4:  "(first closing ", (the) x1, ")";
    }
  CommandsOff: switch (n) {
        1: "[Command recording off.]";
        #Ifdef TARGET_GLULX;
        2: "[Command recording already off.]";
        #Endif; ! TARGET_
    }
  CommandsOn: switch (n) {
        1: "[Command recording on.]";
        #Ifdef TARGET_GLULX;
        2: "[Commands are currently replaying.]";
        3: "[Command recording already on.]";
        4: "[Command recording failed.]";
        #Endif; ! TARGET_
    }
  CommandsRead: switch (n) {
        1: "[Replaying commands.]";
        #Ifdef TARGET_GLULX;
        2: "[Commands are already replaying.]";
        3: "[Command replay failed.  Command recording is on.]";
        4: "[Command replay failed.]";
        5: "[Command replay complete.]";
        #Endif; ! TARGET_
    }
  Consult:  CSubjectVerb(actor,true,false,"discover",0,"discovers","discovered");
            print " nothing of interest in ";
            if (x1 == player) { OnesSelf(x1); ".";}
            else print_ret (the) x1, ".";
  Cut: switch (n) {
        1:  print "Cutting ", (ThatOrThose) x1, " up would ";
            Tense("achieve", "have achieved");
            " little.";
        2:  DecideAgainst();
    }
  Dig:      print "Digging would ";
            Tense("achieve", "have achieved");
            " nothing here.";
  Disrobe: switch (n) {
        1:  CSubjectIsnt(actor,true); " wearing ", (ThatOrThose) x1, ".";
        2:  CSubjectVerb(actor,false,false,"take off",0,"takes off", "took off");
            " ", (the) x1, ".";
    }
  Drink:    print "There";
            Tense("'s", " was");
            " nothing suitable to drink here.";
  Drop: switch (n) {
        1:  CSubjectIs(x1,true); " already here.";
        2:  CSubjectVerb(actor, false, false, "haven't got", 0, "hasn't got",
                         "didn't have");
            " ", (the) x1, ".";
        3:  "(first taking ", (the) x1, " off)";
        4:  "Dropped.";
    }
  Eat: switch (n) {
        1:  CSubjectIs(x1,true); " plainly inedible.";
        2:  CSubjectVerb(actor,false,false,"eat",0,"eats", "ate"); print " ", (the) x1;
                if (actor == player) ". Not bad."; else ".";
    }
  EmptyT: switch (n) {
        1:  CSubjectCant(x1,true); " contain things.";
        2:  CSubjectIs(x1,true); " closed.";
        3:  CSubjectIs(x1,true); " empty already.";
        4:  print "That wouldn't ";
            Tense("empty", "have emptied");
            " anything.";
    }
  Enter: switch (n) {
        1:  print "But "; CSubjectIs(actor,true,true);
            " already ", (nop) SupportObj(x1,"on ","in "), (the) x1, ".";
        2:  CSubjectIs(x1,true);
            print " not something ", (theActor) actor;
            Tense(" can ", " could ");
            switch (x2) {
              'stand':  "stand on.";
              'sit':    "sit down on.";
              'lie':    "lie down on.";
              default:  "enter.";
            }
        3:  CSubjectCant(actor,true);
            " get into the closed ", (name) x1, ".";
        4:  CSubjectCan(actor,true);
            " only get into something free-standing.";
        5:  CSubjectVerb(actor,false,false,"get",0,"gets","got");
            SupportObj(x1," onto"," into"); " ", (the) x1, ".";
        6:  "(getting ", (nop) SupportObj(x1,"off","out of"), " ", (the) x1, ")";
        7:  if (x1 has supporter) "(getting onto ", (the) x1, ")";
            if (x1 has container) "(getting into ", (the) x1, ")";
                                  "(entering ", (the) x1, ")";
    }
  Examine: switch (n) {
        1:  "Darkness, noun.  An absence of light to see by.";
        2:  CSubjectVerb(actor,true,false,"see",0,"sees","saw");
            " nothing special about ", (the) x1, ".";
        3:  CSubjectIs(x1,true);
            Tense(" currently");
            print " switched ";
            if (x1 has on) "on."; else "off.";
    }
  Exit: switch (n) {
        1:  print "But ";
            CSubjectIsnt(actor,true,true);
            " in anything at the moment.";
        2:  CSubjectCant(actor,false);
            " get out of the closed ", (name) x1, ".";
        3:  CSubjectVerb(actor,false,false,"get",0,"gets", "got");
            print " ";
            SupportObj(x1,"off","out of"); " ", (the) x1, ".";
        4:  CSubjectIsnt(actor,true);
            print " ";
            SupportObj(x1,"on","in"); " ", (the) x1, ".";
        5:  "(first getting ", (nop) SupportObj(x1,"off","out of"),
              " ", (the) x1, ")";
        6:  CSubjectVerb(actor,false,false,"stand",0,"stands","stood"); " up.";
    }
  Fill: switch (n) {
        1:  print "There ";
            Tense("isn't", "wasn't");
            " anything obvious with which to fill ", (the) x1, ".";
        2:  print "Filling ", (the) x1, " from ", (the) x2;
            Tense(" doesn't", " didn't");
            " make sense.";
    }
  FullScore: switch (n) {
        1:  if (deadflag) print "The score was "; else print "The score is ";
                "made up as follows:^";
        2:  "finding sundry items";
        3:  "visiting various places";
        4:  print "total (out of ", MAX_SCORE; ")";
    }
  GetOff:   print "But ";
            CSubjectIsnt(actor,true,true); " on ", (the) x1, " at the moment.";
  Give: switch (n) {
        1:  CSubjectIsnt(actor,true); " holding ", (the) x1, ".";
        2:  CSubjectVerb(actor,false,false,"juggle",0,"juggles","juggled");
            print " ", (the) x1, " for a while, but ";
            CSubjectVoice(actor,"don't","don't","doesn't","didn't");
            " achieve much.";
        3:  CSubjectDont(x1,true); " seem interested.";
        4:  CSubjectVerb(actor,false,false,"hand over",0,"hands over","handed over");
            " ", (the) x1, ".";
    }
  Go: switch (n) {
        1:  CSubjectWill(actor,true);
            Tense(" have", " had");
                " to get ", (nop) SupportObj(x1,"off","out of"), " ", (the) x1, " first.";
        2:  CSubjectCant(actor,true); " go that way.";
        3:  CSubjectIs  (actor,true); " unable to climb ", (the) x1, ".";
        4:  CSubjectIs  (actor,true); " unable to descend by ", (the) x1, ".";
        5:  CSubjectCant(actor,true); " since ", (the) x1, " ", (IsOrAre) x1, " in the way.";
        6:  CSubjectCant(actor,true); " since ", (the) x1, " ", (nop) PluralObj(x1,"lead","leads","led"), " nowhere.";
        7:  CSubjectVerb(actor,false,false,"depart",0,"departs","departed"); ".";
    }
  Insert: switch (n) {
        1:  CSubjectVerb(actor,true,false,"need",0,"needs","needed");
            print " to be holding ", (the) x1, " before ", (theActor) actor;
            Tense(" can", " could");
            " put ", (ItOrThem) x1, " into something else.";
        2:  CSubjectCant(x1,true); " contain things.";
        3:  CSubjectIs  (x1,true); " closed.";
        4:  CSubjectWill(actor,true);
            Tense(" need", " needed");
            " to take ", (ItOrThem) x1, " off first.";
        5:  CSubjectCant(actor,true); " put something inside itself.";
        6:  "(first taking ", (ItOrThem) x1, " off)";
        7:  print "There ";
            Tense(" is", " was");
            " no more room in ", (the) x1, ".";
        8:  "Done.";
        9:  CSubjectVerb(actor,false,false,"put",0,"puts","put"); " ", (the) x1, " into ", (the) x2, ".";
    }
  Inv: switch (n) {
        1:  CSubjectIs  (actor,false); " carrying nothing.";
        2:  CSubjectIs  (actor,false); print " carrying";
        3:  ":";
        4:  ".";
    }
  Jump: CSubjectVerb(actor,false,false,"jump",0,"jumps","jumped"); " on the spot, fruitlessly.";
  JumpIn:
        print "Jumping in ", (the) x1, " ";
        Tense("would achieve", "would have achieved");
        " nothing here.";
  JumpOn:
        print "Jumping upon ", (the) x1, " ";
        Tense("would achieve", "would have achieved");
        " nothing here.";
  JumpOver: switch (n) {
        1:  CSubjectVerb(actor,true,false,"achieve",0,"achieve","achieved"); " nothing by this.";
        2:  DecideAgainst();
    }
  Kiss:     "Keep your mind on the game.";
  Listen:   CSubjectVerb(actor,true,false,"hear",0,"hears","heard"); " nothing unexpected.";
  ListMiscellany: switch (n) {
        1:  print " (providing light)";
        2:  print " (which ", (IsOrAre) x1, " closed)";
        3:  print " (closed and providing light)";
        4:  print " (which ", (IsOrAre) x1, " empty)";
        5:  print " (empty and providing light)";
        6:  print " (which ", (IsOrAre) x1, " closed and empty)";
        7:  print " (closed, empty and providing light)";
        8:  print " (providing light and being worn";
        9:  print " (providing light";
        10: print " (being worn";
        11: print " (which ", (IsOrAre) x1, " ";
        12: print "open";
        13: print "open but empty";
        14: print "closed";
        15: print "closed and locked";
        16: print " and empty";
        17: print " (which ", (IsOrAre) x1, " empty)";
        18: print " containing ";
        19: print " (on ";
        20: print ", on top of ";
        21: print " (in ";
        22: print ", inside ";
    }
  LMode1:   print " is now in its ";
            if (initial_lookmode == 1) print "normal ";
            "~brief~ printing mode, which gives long descriptions
             of places never before visited and short descriptions otherwise.";
  LMode2:   print " is now in its ";
            if (initial_lookmode ~= 1 or 3) print "normal ";
            "~verbose~ mode, which always gives long descriptions
             of locations (even if you've been there before).";
  LMode3:   print " is now in its ";
            if (initial_lookmode == 3) print "normal ";
            "~superbrief~ mode, which always gives short descriptions
             of locations (even if you haven't been there before).";
  Lock: switch (n) {
        1:  CSubjectDont(x1,true);
            print " seem to be something ", (theActor) actor;
            Tense(" can", " could");
            " lock.";
        2:  CSubjectIs  (x1,true); " locked at the moment.";
        3:  CSubjectWill(actor,true); " first have to close ", (the) x1, ".";
        4:  CSubjectDont(x1,true); " seem to fit the lock.";
        5:  CSubjectVerb(actor,false,false,"lock",0,"locks","locked"); " ", (the) x1, ".";
    }
  Look: switch (n) {
        1:  print " (on ", (the) x1, ")";
        2:  print " (in ", (the) x1, ")";
        3:  print " (as ", (object) x1, ")";
        4:  print "^On ", (the) x1;
            WriteListFrom(child(x1),
              ENGLISH_BIT+RECURSE_BIT+PARTINV_BIT+TERSE_BIT+CONCEAL_BIT+ISARE_BIT);
            ".";
        5,6:
            if (x1 ~= location) {
                if (x1 has supporter) print "^On "; else print "^In ";
                print (the) x1, " ", (theActor) actor, " ";
                Tense("can", "could");
            }
            else { new_line; CSubjectCan(actor,false); }
            if (n == 5) print " also";
            print " see ";
            WriteListFrom(child(x1),
              ENGLISH_BIT+RECURSE_BIT+PARTINV_BIT+TERSE_BIT+CONCEAL_BIT+WORKFLAG_BIT);
            if (x1 ~= location) "."; else " here.";
        7:  CSubjectVerb(actor,true,false,"see",0,"sees", "saw"); " nothing unexpected in that direction.";
    }
  LookUnder: switch (n) {
        1:  print "But it";
            Tense("'s", " was");
            " dark.";
        2:  CSubjectVerb(actor,true,false,"find",0,"finds", "found"); " nothing of interest.";
    }
  Mild:     "Quite.";
  Miscellany: switch (n) {
        1:  "(considering the first sixteen objects only)^";
        2:  "Nothing to do!";
        3:  print " "; CSubjectVerb(player, false, false, "died", "have died", "has died");
	    print " ";
        4:  print " "; CSubjectVerb(player, false, false, "won", "have won", "has won");
            print " ";
        5:  print "^Would you like to RESTART, RESTORE a saved game";
            #Ifdef DEATH_MENTION_UNDO;
            print ", UNDO your last move";
            #Endif;
            if (TASKS_PROVIDED == 0) print ", give the FULL score for that game";
            if (deadflag == 2 && AMUSING_PROVIDED == 0)
                print ", see some suggestions for AMUSING things to do";
            SerialComma(3); print " or QUIT?";
        6:  "[Your interpreter does not provide ~undo~.  Sorry!]";
            #Ifdef TARGET_ZCODE;
        7:  "~Undo~ failed.  [Not all interpreters provide it.]";
            #Ifnot; ! TARGET_GLULX
        7:  "[You cannot ~undo~ any further.]";
            #Endif; ! TARGET_
        8:  "Please give one of the answers above.";
        9:  print "^It ";
            Tense("is now", "was");
            print " pitch dark in ";
            Tense("here", "there");
            "!";
        10: "I beg your pardon?";
        11: "[You can't ~undo~ what hasn't been done!]";
        12: "[Can't ~undo~ twice in succession. Sorry!]";
        13: "[Previous turn undone.]";
        14: "Sorry, that can't be corrected.";
        15: "Think nothing of it.";
        16: "~Oops~ can only correct a single word.";
        17: print "It ";
            Tense("is", "was");
            print " pitch dark, and ", (theActor) actor;
            Tense(" can't", " couldn't");
            " see a thing.";
        18: print "yourself";
        19: "As good-looking as ever.";
        20: "To repeat a command like ~frog, jump~, just say ~again~, not ~frog, again~.";
        21: CSubjectCan(actor,true); " hardly repeat that.";
        22: CSubjectCant(actor, true); " begin with a comma.";
        23: CSubjectVerb(actor, true, false, "seem", "seem", "seems", "seemed");
            print " to want to talk to someone, but I ";
            Tense("can't", "couldn't"); " see whom.";
        24: CSubjectCant(actor, true); " talk to ", (the) x1, ".";
        25: "To talk to someone, try ~someone, hello~ or some such.";
        26: "(first taking ", (the) x1, ")";
        27: "I didn't understand that sentence.";
        28: print "I only understood you as far as wanting to ";
        29: "I didn't understand that number.";
        30: CSubjectCant(actor,true); " see any such thing.";
        31: CSubjectVerb(actor, true, false, "seem", "seem", "seems", "seemed");
            " to have said too little!";
        32: CSubjectIsnt(actor); " holding that!";
        33: "You can't use multiple objects with that verb.";
        34: "You can only use multiple objects once on a line.";
        35: "I'm not sure what ~", (address) x1, "~ refers to.";
        36: "You excepted something not included anyway!";
        37: CSubjectCan(actor,true); " only do that to something animate.";
            #Ifdef DIALECT_US;
        38: "That's not a verb I recognize.";
            #Ifnot;
        38: "That's not a verb I recognise.";
            #Endif;
        39: "That's not something you need to refer to in the course of this game.";
        40: CSubjectCant(actor,true); " see ~", (address) x1, "~ (", (the) x2, ") at the moment.";
        41: "I didn't understand the way that finished.";
        42: if (x1 == 0) print "None"; else print "Only ", (number) x1;
            print " of those ";
            if (x1 == 1) print "is"; else print "are";
            " available.";
        43: "Nothing to do!";
        44: print "There ";
            Tense("is", "was");
            " nothing to ", (address) x1, ".";
        45: print "Who do you mean, ";
        46: print "Which do you mean, ";
        47: "Sorry, you can only have one item here. Which exactly?";
        48: print "Whom ";
            CSubjectVoice(player, "do", "do", "does", "did");
            print " ";
            CSubjectVerb(player, false, true, "want", "want", "want", "want");
            if (x1 ~= player && x1 ~= nothing) print " ", (the) x1;
            print " to "; PrintCommand(); "?";
        49: print "What ";
            CSubjectVoice(player, "do", "do", "does", "did");
            print " ";
            CSubjectVerb(player, false, true, "want", "want", "want", "want");
            if (x1 ~= player && x1 ~= nothing) print " ", (the) x1;
            print " to "; PrintCommand(); "?";
        50: print "The score has just gone ";
            if (x1 > 0) print "up"; else { x1 = -x1; print "down"; }
            print " by ", (number) x1, " point";
            if (x1 > 1) print "s";
        51: "(Since something dramatic has happened, your list of commands has been cut short.)";
        52: "^Type a number from 1 to ", x1, ", 0 to redisplay or press ENTER.";
        53: "^[Please press SPACE.]";
        54: "[Comment recorded.]";
        55: "[Comment NOT recorded.]";
        56: ".";
        57: "?";
        58: "(first taking ", (the) x1, " ", (nop) SupportObj(x2,"off","out of"), " ", (the) x2, ")";
        59: "You'll have to be more specific.";
        60: print (The) x1, " observes that ";
    }
  No,Yes:   "That was a rhetorical question.";
  NotifyOff:
            "Score notification off.";
  NotifyOn: "Score notification on.";
  Objects: switch (n) {
        1:  "Objects ", (nop) CSubjectVerb(actor, false, true, "have", "have", "has"), " handled:^";
        2:  "None.";
        3:  print "   (worn)";
        4:  print "   (held)";
        5:  print "   (given away)";
        6:  print "   (in ", (name) x1, ")";
        7:  print "   (in ", (the) x1, ")";
        8:  print "   (inside ", (the) x1, ")";
        9:  print "   (on ", (the) x1, ")";
        10: print "   (lost)";
    }
  Open: switch (n) {
        1:  CSubjectIs  (x1,true);
            print " not something ", (theActor) actor;
            Tense(" can open", " could have opened");
            ".";
        2:  CSubjectVerb(x1,true,false,"seem",0,"seems","seemed"); " to be locked.";
        3:  CSubjectIs  (x1,true); " already open.";
        4:  CSubjectVerb(actor,false,false,"open",0,"opens","opened"); print " ", (the) x1;
            Tense(", revealing ", " and revealed ");
                if (WriteListFrom(child(x1), ENGLISH_BIT+TERSE_BIT+CONCEAL_BIT) == 0) "nothing.";
                ".";
        5:  CSubjectVerb(actor,false,false,"open",0,"opens","opened"); " ", (the) x1, ".";
        6:  "(first opening ", (the) x1, ")";
    }
  Order:    CSubjectHas(x1,false); " better things to do.";
  Places: switch (n) {
        1:  print "You have visited: ";
        2:  ".";
    }
  Pray:     print "Nothing practical ";
            Tense("results", "resulted");
            " from ", (Possessive) actor, " prayer.";
  Prompt:   print "^>";
  Pronouns: switch (n) {
        1:  print "At the moment, ";
        2:  print "means ";
        3:  print "is unset";
        4:  "no pronouns are known to the game.";
        5:  ".";
    }
  Pull,Push,Turn: switch (n) {
        1:  if (player provides narrative_voice && player.narrative_voice == 3) {
                print_ret (The) player, " ", (nop) Tense("isn't", "wasn't"),
                  " likely to help matters by punishing ",
                              (OnesSelf) player, " that way.";
            } else {
                "Punishing ", (OnesSelf) player, " that way ",
                              (nop) Tense("isn't", "wasn't"), " likely to help matters.";
            }
        2:  CSubjectIs  (x1,true); " fixed in place.";
        3:  CSubjectIs  (actor,true); " unable to.";
        4:  print "Nothing obvious ";
            Tense("happens", "happened");
            ".";
        5:  print "That would ";
            Tense("be", "have been");
            " less than courteous.";
        6:  DecideAgainst();
    }
! Push: see Pull
  PushDir: switch (n) {
        1:  print "That really ";
            Tense("wouldn't", "didn't");
            " serve any purpose.";
        2:  print "That's ";
            Tense("not", "wasn't");
            " a direction.";
        3:  print "Not that way ", (theActor) actor;
            Tense(" can't", "couldn't");
            ".";
    }
  PutOn: switch (n) {
        1:  CSubjectVerb(actor,true,false,"need",0,"needs","needed");
            print " to be holding ", (the) x1, " before ", (theActor) actor;
            Tense(" can", " could");
            " put ", (ItOrThem) x1, " on top of something else.";
        2:  CSubjectCant(actor,true,true); " put something on top of itself.";
        3:  print "Putting things on ", (the) x1, " would";
            Tense(" achieve", "'ve achieved");
            " nothing.";
        4:  CSubjectVerb(actor,true,false,"lack",0,"lacks","lacked"); " the dexterity.";
        5:  "(first taking ", (ItOrThem) x1, " off)";
        6:  print "There ";
            Tense("is", "was");
            " no more room on ", (the) x1, ".";
        7:  "Done.";
        8:  CSubjectVerb(actor,false,false,"put",0,"puts","put"); " ", (the) x1, " on ", (the) x2, ".";
    }
  Quit: switch (n) {
        1:  print "Please answer yes or no.";
        2:  print "Are you sure you want to quit? ";
    }
  Remove: switch (n) {
        1:  CSubjectIs  (x1,true); " unfortunately closed.";
        2:  print "But ";
            CSubjectIsnt(x1,true); " there now.";
        3:  "Removed.";
    }
  Restart: switch (n) {
        1:  print "Are you sure you want to restart? ";
        2:  "Failed.";
    }
  Restore: switch (n) {
        1:  "Restore failed.";
        2:  "Ok.";
    }
  Rub: switch (n) {
        1:  CSubjectVerb(actor,true,false,"achieve",0,"achieves","achieved");
            " nothing by this.";
        2:  DecideAgainst();
    }
  Save: switch (n) {
        1:  "Save failed.";
        2:  "Ok.";
    }
  Score: switch (n) {
        1:  if (deadflag) print "In that game you scored "; else print "You have so far scored ";
                print score, " out of a possible ", MAX_SCORE, ", in ", turns, " turn";
                if (turns ~= 1) print "s";
                return;
        2:  "There is no score in this story.";
    }
  ScriptOff: switch (n) {
        1:  "Transcripting is already off.";
        2:  "^End of transcript.";
        3:  "Attempt to end transcript failed.";
    }
  ScriptOn: switch (n) {
        1:  "Transcripting is already on.";
        2:  "Start of a transcript of";
        3:  "Attempt to begin transcript failed.";
    }
  Search: switch (n) {
        1:  print "But it";
            Tense("'s", " was");
            " dark.";
        2:  print "There ";
            Tense("is", "was");
            " nothing on ", (the) x1, ".";
        3:  print "On ", (the) x1;
                WriteListFrom(child(x1), ENGLISH_BIT+TERSE_BIT+CONCEAL_BIT+ISARE_BIT);
                ".";
        4:  CSubjectVerb(actor,true,false,"find",0,"finds","found"); " nothing of interest.";
        5:  CSubjectCant(actor,true); " see inside, since ", (the) x1, " ", (IsOrAre) x1, " closed.";
        6:  "", (The) x1, " ", (IsOrAre) x1, " empty.";

        7:  print "In ", (the) x1;
                WriteListFrom(child(x1), ENGLISH_BIT+TERSE_BIT+CONCEAL_BIT+ISARE_BIT);
                ".";
    }
   ! Preceding "No," unable to be used for Set and SetTo
  Set:      CSubjectCant(actor,true); " set ", (ThatOrThose) x1, ".";
  SetTo:    CSubjectCant(actor,true); " set ", (ThatOrThose) x1, " to anything.";
  Show: switch (n) {
        1:  CSubjectIsnt(actor,true); " holding ", (the) x1, ".";
        2:  CSubjectIs  (x1,true); " unimpressed.";
    }
  Sing:     print (PossessiveCaps) actor, " singing ";
            Tense("is", "was");
            " abominable.";
  Sleep:    CSubjectIsnt(actor,true); " feeling especially drowsy.";
  Smell: switch (n) {
        1:  CSubjectVerb(actor,true,false,"smell",0,"smells","smelled"); " nothing unexpected.";
        2:  DecideAgainst();
    }
            #Ifdef DIALECT_US;
  Sorry:    "Oh, don't apologize.";
            #Ifnot;
  Sorry:    "Oh, don't apologise.";
            #Endif;
  Squeeze: switch (n) {
        1:  DecideAgainst();
        2:  CSubjectVerb(actor,true,false,"achieve",0,"achieves","achieved"); " nothing by this.";
    }
  Strong:   print "Real adventurers ";
            Tense ("do", "did");
            " not use such language.";
  Swim:     print "There";
            Tense("'s not", " wasn't");
            " enough water to swim in.";
  Swing:    print "There";
            Tense("'s", " was");
            " nothing sensible to swing here.";
  SwitchOff: switch (n) {
        1:  CSubjectIs  (x1,true);
            print " not something ", (theActor) actor, " ";
            Tense("can", "could");
            " switch.";
        2:  CSubjectIs  (x1,true); " already off.";
        3:  CSubjectVerb(actor,false,false,"switch",0,"switches","switched"); " ", (the) x1, " off.";
    }
  SwitchOn: switch (n) {
        1:  CSubjectIs  (x1,true);
            print " not something ", (theActor) actor, " ";
            Tense("can", "could");
            " switch.";
        2:  CSubjectIs  (x1,true); " already on.";
        3:  CSubjectVerb(actor,false,false,"switch",0,"switches","switched"); " ", (the) x1, " on.";
    }

  Take: switch (n) {
        1:  "Taken.";
        2:  CSubjectIs  (actor,false); " always self-possessed.";
        3:  print "I don't suppose ", (the) x1, " would ";
            Tense("care", "have cared");
            " for that.";
        4:  CSubjectWill(actor,true);
            print " have ";
            Tense("", "had ");
            "to get ", (nop) SupportObj(x1,"off","out of"), " ", (the) x1, " first.";
        5:  CSubjectVerb(actor,true,false,"already have",0,"already has","already had"); " ", (ThatOrThose) x1, ".";
        6:  CSubjectVerb(x2,true,false,"seem",0,"seems","seemed"); " to belong to ", (the) x1, ".";
        7:  CSubjectVerb(x2,true,false,"seem",0,"seems","seemed"); " to be a part of ", (the) x1, ".";
        8:  CSubjectIs  (x1,true); " not available.";
        9:  CSubjectIs  (x1,true); " not open.";
        10: CSubjectIs  (x1,true); " hardly portable.";
        11: CSubjectIs  (x1,true); " fixed in place.";
        12: CSubjectIs  (actor,true); " carrying too many things already.";
        13: "(putting ", (the) x1, " into ", (the) x2, " to make room)";
    }
  Taste: switch (n) {
        1:  CSubjectVerb(actor,true,false,"taste",0,"tastes","tasted"); " nothing unexpected.";
        2:  DecideAgainst();
    }
  Tell: switch (n) {
        1:  CSubjectVerb(actor,false,false,"talk",0,"talks","talked");
            " to ", (OnesSelf) actor, " for a while.";
        2:  print "This provoke";
            Tense("s", "d");
            " no reaction.";
    }
  Think:    "What a good idea.";
  ThrowAt: switch (n) {
        1:  "Futile.";
        2:  CSubjectVerb(actor,true,false,"lack",0,"lacks","lacked");
            print " the nerve when it ";
            Tense("comes", "came");
            " to the crucial moment.";
    }
  Tie: switch (n) {
        1:  CSubjectVerb(actor,true,false,"would",0,0);
            Tense(" achieve", " have achieved");
            " nothing by this.";
        2:  DecideAgainst();
    }
  Touch: switch (n) {
        1:  DecideAgainst();
        2:  CSubjectVerb(actor,true,false,"feel",0,"feels","felt"); " nothing unexpected.";
        3:  print "That really ";
            Tense("wouldn't", "didn't");
            " serve any purpose.";
    }
! Turn: see Pull.
  Unlock:  switch (n) {
        1:  CSubjectDont(x1,true);
            print " seem to be something ", (theActor) actor;
            Tense(" can unlock", " could have unlocked");
            ".";
        2:  CSubjectIs  (x1,true); " unlocked at the moment.";
        3:  CSubjectDont(x1,true); " seem to fit the lock.";
        4:  CSubjectVerb(actor,false,false,"unlock",0,"unlocks","unlocked"); " ", (the) x1, ".";
        5:  "(first unlocking ", (the) x1, ")";
    }
  VagueGo:  CSubjectWill(actor);
            print " have ";
            Tense("", "had ");
            "to say which compass direction to go in.";
  Verify: switch (n) {
        1:  "The game file has verified as intact.";
        2:  "The game file did not verify as intact, and may be corrupt.";
    }
  Wait:     print "Time passe";
            Tense("s", "d");
            ".";
  Wake:     print "The dreadful truth is, this ";
            Tense("is", "was");
            " not a dream.";
  WakeOther:print "That seem";
            Tense("s", "ed");
            " unnecessary.";
  Wave: switch (n) {
        1:  print "But ";
            CSubjectIsnt(actor,true,true); " holding ", (ThatOrThose) x1, ".";
        2:  CSubjectVerb(actor,false,false,"look",0,"looks","looked");
            print " ridiculous waving ", (the) x1;
            if (x2)
                " at ", (the) x2, ".";
            ".";
        3:  DecideAgainst();
    }
  WaveHands:
        CSubjectVerb(actor,false,false,"wave",0,"waves","waved");
        switch (n) {
        1: ! nothing
        2: print " at ", (the) x1;
        }
        ", feeling foolish.";
  Wear: switch (n) {
        1:  CSubjectCant(actor,true); " wear ", (ThatOrThose) x1, "!";
        2:  CSubjectIs  (actor,true); " not holding ", (ThatOrThose) x1, "!";
        3:  CSubjectIs  (actor,true); " already wearing ", (ThatOrThose) x1, "!";
        4:  CSubjectVerb(actor,false,false,"put on",0,"puts on","put on"); " ", (the) x1, ".";
    }
! Yes:  see No.
];

! ==============================================================================

Constant LIBRARY_ENGLISH;       ! for dependency checking.
#Endif;

! ==============================================================================
