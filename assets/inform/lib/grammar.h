! ==============================================================================
!   GRAMMAR:  Grammar table entries for the standard verbs library.
!
!   Supplied for use with Inform 6 -- Release 6.12.1 -- Serial number 160605
!
!   Copyright Graham Nelson 1993-2004 and David Griffith 2012-2016
!   This code is licensed under either the traditional Inform license as
!   described by the DM4 or the Artistic License version 2.0.  See the
!   file COPYING in the distribution archive or at
!   https://github.com/DavidGriffith/inform6lib/
!
!   In your game file, Include three library files in this order:
!       Include "Parser";
!       Include "VerbLib";
!       Include "Grammar";
! ==============================================================================

System_file;

#Ifdef		LIBRARY_STAGE;
#Iffalse	LIBRARY_STAGE >= AFTER_GRAMMAR;	! if not already included
#Iftrue		LIBRARY_STAGE == AFTER_VERBLIB;	! if okay to include it

! ------------------------------------------------------------------------------
!  The "meta-verbs", commands to the game rather than in the game, come first:
! ------------------------------------------------------------------------------

Verb meta 'brief'
    *                                           -> LMode1;

Verb meta 'verbose' 'long'
    *                                           -> LMode2;

Verb meta 'superbrief' 'short'
    *                                           -> LMode3;

Verb meta 'normal'
    *                                           -> LModeNormal;

Verb meta 'notify'
    *                                           -> NotifyOn
    * 'on'                                      -> NotifyOn
    * 'off'                                     -> NotifyOff;

Verb meta 'pronouns' 'nouns'
    *                                           -> Pronouns;

Verb meta 'quit' 'q//' 'die'
    *                                           -> Quit;

Verb meta 'recording'
    *                                           -> CommandsOn
    * 'on'                                      -> CommandsOn
    * 'off'                                     -> CommandsOff;

Verb meta 'replay'
    *                                           -> CommandsRead;

Verb meta 'restart'
    *                                           -> Restart;

Verb meta 'restore'
    *                                           -> Restore;

Verb meta 'save'
    *                                           -> Save;

Verb meta 'score'
    *                                           -> Score;

Verb meta 'fullscore' 'full'
    *                                           -> FullScore
    * 'score'                                   -> FullScore;

Verb meta 'script' 'transcript'
    *                                           -> ScriptOn
    * 'on'                                      -> ScriptOn
    * 'off'                                     -> ScriptOff;

Verb meta 'noscript' 'unscript'
    *                                           -> ScriptOff;

Verb meta 'verify'
    *                                           -> Verify;

Verb meta 'version'
    *                                           -> Version;

#Ifndef NO_PLACES;
Verb meta 'objects'
    *                                           -> Objects;
Verb meta 'places'
    *                                           -> Places;
#Endif; ! NO_PLACES

! ------------------------------------------------------------------------------
!  Debugging grammar
! ------------------------------------------------------------------------------

#Ifdef DEBUG;
Verb meta 'actions'
    *                                           -> ActionsOn
    * 'on'                                      -> ActionsOn
    * 'off'                                     -> ActionsOff;

Verb meta 'changes'
    *                                           -> ChangesOn
    * 'on'                                      -> ChangesOn
    * 'off'                                     -> ChangesOff;

Verb meta 'gonear'
    * anynumber                                 -> GoNear
    * noun                                      -> Gonear;


Verb meta 'goto'
    * anynumber                                 -> Goto;

Verb meta 'random'
    *                                           -> Predictable;

Verb meta 'routines' 'messages'
    *                                           -> RoutinesOn
    * 'on'                                      -> RoutinesOn
    * 'verbose'                                 -> RoutinesVerbose
    * 'off'                                     -> RoutinesOff;

Verb meta 'scope'
    *                                           -> Scope
    * anynumber                                 -> Scope
    * noun                                      -> Scope;

Verb meta 'showdict' 'dict'
    *                                           -> ShowDict
    * topic                                     -> ShowDict;

Verb meta 'showobj'
    *                                           -> Showobj
    * anynumber                                 -> Showobj
    * multi                                     -> Showobj;

Verb meta 'showverb'
    * special                                   -> Showverb;

Verb meta 'timers' 'daemons'
    *                                           -> TimersOn
    * 'on'                                      -> TimersOn
    * 'off'                                     -> TimersOff;

Verb meta 'trace'
    *                                           -> TraceOn
    * number                                    -> TraceLevel
    * 'on'                                      -> TraceOn
    * 'off'                                     -> TraceOff;

Verb meta 'abstract'
    * anynumber 'to' anynumber                  -> XAbstract
    * noun 'to' noun                            -> XAbstract;

Verb meta 'purloin'
    * anynumber                                 -> XPurloin
    * multi                                     -> XPurloin;

Verb meta 'tree'
    *                                           -> XTree
    * anynumber                                 -> XTree
    * noun                                      -> XTree;

#Ifdef TARGET_GLULX;
Verb meta 'glklist'
    *                                           -> Glklist;
#Endif; ! TARGET_

#Endif; ! DEBUG

! ------------------------------------------------------------------------------
!  And now the game verbs.
! ------------------------------------------------------------------------------

[ ADirection; if (noun in compass) rtrue; rfalse; ];

Verb 'answer' 'say' 'shout' 'speak'
    * topic 'to' creature                       -> Answer;

Verb 'ask'
    * creature 'about' topic                    -> Ask
    * creature 'for' noun                       -> AskFor
    * creature 'to' topic                       -> AskTo
    * 'that' creature topic                     -> AskTo;

Verb 'attack' 'break' 'crack' 'destroy'
     'fight' 'hit' 'kill' 'murder' 'punch'
     'smash' 'thump' 'torture' 'wreck'
    * noun                                      -> Attack;

Verb 'blow'
    * held                                      -> Blow;

Verb 'bother' 'curses' 'darn' 'drat'
    *                                           -> Mild
    * topic                                     -> Mild;

Verb 'burn' 'light'
    * noun                                      -> Burn
    * noun 'with' held                          -> Burn;

Verb 'buy' 'purchase'
    * noun                                      -> Buy;

Verb 'climb' 'scale'
    * noun                                      -> Climb
    * 'up'/'over' noun                          -> Climb;

Verb 'close' 'cover' 'shut'
    * noun                                      -> Close
    * 'up' noun                                 -> Close
    * 'off' noun                                -> SwitchOff;

Verb 'consult'
    * noun 'about' topic                        -> Consult
    * noun 'on' topic                           -> Consult;

Verb 'cut' 'chop' 'prune' 'slice'
    * noun                                      -> Cut;

Verb 'dig'
    * noun                                      -> Dig
    * noun 'with' held                          -> Dig
    * 'in' noun                                 -> Dig
    * 'in' noun 'with' held                     -> Dig;


Verb 'disrobe' 'doff' 'shed'
    * held                                      -> Disrobe;

Verb 'drink' 'sip' 'swallow'
    * noun                                      -> Drink;

Verb 'drop' 'discard'
    * multiheld                                 -> Drop
    * multiexcept 'in'/'into'/'down' noun       -> Insert
    * multiexcept 'on'/'onto' noun              -> PutOn;

Verb 'throw'
    * held 'at'/'against'/'on'/'onto' noun      -> ThrowAt;

Verb 'eat'
    * held                                      -> Eat;

Verb 'empty'
    * noun                                      -> Empty
    * 'out' noun                                -> Empty
    * noun 'out'                                -> Empty
    * noun 'to'/'into'/'on'/'onto' noun         -> EmptyT;

Verb 'enter' 'cross'
    *                                           -> GoIn
    * noun                                      -> Enter;

Verb 'examine' 'x//' 'check' 'describe' 'watch'
    * noun                                      -> Examine;

Verb 'exit' 'out' 'outside'
    *                                           -> Exit
    * noun                                      -> Exit;

Verb 'fill'
    * noun                                      -> Fill
    * noun 'from' noun                          -> Fill;

Verb 'get'
    * 'out'/'off'/'up' 'of'/'from' noun         -> Exit
    * multi                                     -> Take
    * 'in'/'into'/'on'/'onto' noun              -> Enter
    * 'off' noun                                -> GetOff
    * multiinside 'from'/'off' noun             -> Remove;

Verb 'give' 'feed' 'offer' 'pay'
    * creature held                             -> Give reverse
    * held 'to' creature                        -> Give
    * 'over' held 'to' creature                 -> Give;

Verb 'go' 'run' 'walk'
    *                                           -> VagueGo
    * noun=ADirection                           -> Go
    * noun                                      -> Enter
    * 'out'/'outside'                           -> Exit
    * 'in'/'inside'                             -> GoIn
    * 'into'/'in'/'inside'/'through' noun       -> Enter;

Verb 'in' 'inside'
    *                                           -> GoIn;

Verb 'insert'
    * multiexcept 'in'/'into' noun              -> Insert;

Verb 'inventory' 'inv' 'i//'
    *                                           -> Inv
    * 'tall'                                    -> InvTall
    * 'wide'                                    -> InvWide;

Verb 'jump' 'hop' 'skip'
    *                                           -> Jump
    * 'in' noun                                 -> JumpIn
    * 'into' noun                               -> JumpIn
    * 'on' noun                                 -> JumpOn
    * 'upon' noun                               -> JumpOn
    * 'over' noun                               -> JumpOver;

Verb 'kiss' 'embrace' 'hug'
    * creature                                  -> Kiss;

Verb 'leave'
    *                                           -> VagueGo
    * noun=ADirection                           -> Go
    * noun                                      -> Exit
    * 'into'/'in'/'inside'/'through' noun       -> Enter;

Verb 'listen' 'hear'
    *                                           -> Listen
    * noun                                      -> Listen
    * 'to' noun                                 -> Listen;

Verb 'lock'
    * noun 'with' held                          -> Lock;

Verb 'look' 'l//'
    *                                           -> Look
    * 'at' noun                                 -> Examine
    * 'inside'/'in'/'into'/'through'/'on' noun  -> Search
    * 'under' noun                              -> LookUnder
    * 'up' topic 'in' noun                      -> Consult
    * noun=ADirection                           -> Examine
    * 'to' noun=ADirection                      -> Examine;

Verb 'no'
    *                                           -> No;

Verb 'open' 'uncover' 'undo' 'unwrap'
    * noun                                      -> Open
    * noun 'with' held                          -> Unlock;

Verb 'peel'
    * noun                                      -> Take
    * 'off' noun                                -> Take;

Verb 'pick'
    * 'up' multi                                -> Take
    * multi 'up'                                -> Take;

Verb 'pray'
    *                                           -> Pray;

Verb 'pry' 'prise' 'prize' 'lever' 'jemmy' 'force'
    * noun 'with' held                          -> Unlock
    * 'apart'/'open' noun 'with' held           -> Unlock
    * noun 'apart'/'open' 'with' held           -> Unlock;

Verb 'pull' 'drag'
    * noun                                      -> Pull;

Verb 'push' 'clear' 'move' 'press' 'shift'
    * noun                                      -> Push
    * noun noun                                 -> PushDir
    * noun 'to' noun                            -> Transfer;

Verb 'put'
    * multiexcept 'in'/'inside'/'into' noun     -> Insert
    * multiexcept 'on'/'onto' noun              -> PutOn
    * 'on' held                                 -> Wear
    * 'down' multiheld                          -> Drop
    * multiheld 'down'                          -> Drop;

Verb 'read'
    * noun                                      -> Examine
    * 'about' topic 'in' noun                   -> Consult
    * topic 'in' noun                           -> Consult;

Verb 'remove'
    * held                                      -> Disrobe
    * multi                                     -> Take
    * multiinside 'from' noun                   -> Remove;

Verb 'rub' 'clean' 'dust' 'polish' 'scrub'
     'shine' 'sweep' 'wipe'
    * noun                                      -> Rub;

Verb 'search'
    * noun                                      -> Search;

Verb 'set' 'adjust'
    * noun                                      -> Set
    * noun 'to' special                         -> SetTo;

Verb 'show' 'display' 'present'
    * creature held                             -> Show reverse
    * held 'to' creature                        -> Show;

Verb 'shit' 'damn' 'fuck' 'sod'
    *                                           -> Strong
    * topic                                     -> Strong;

Verb 'sing'
    *                                           -> Sing;

Verb 'sit' 'lie'
    * 'on' 'top' 'of' noun                      -> Enter
    * 'on'/'in'/'inside' noun                   -> Enter;

Verb 'sleep' 'nap'
    *                                           -> Sleep;

Verb 'smell' 'sniff'
    *                                           -> Smell
    * noun                                      -> Smell;

Verb 'sorry'
    *                                           -> Sorry;

Verb 'squeeze' 'squash'
    * noun                                      -> Squeeze;

Verb 'stand'
    *                                           -> Exit
    * 'up'                                      -> Exit
    * 'on' noun                                 -> Enter;

Verb 'swim' 'dive'
    *                                           -> Swim;

Verb 'swing'
    * noun                                      -> Swing
    * 'on' noun                                 -> Swing;

Verb 'switch'
    * noun                                      -> Switchon
    * noun 'on'                                 -> Switchon
    * noun 'off'                                -> Switchoff
    * 'on' noun                                 -> Switchon
    * 'off' noun                                -> Switchoff;

Verb 'take' 'carry' 'hold'
    * multi                                     -> Take
    * 'off' held                                -> Disrobe
    * multiinside 'from'/'off' noun             -> Remove
    * 'inventory'                               -> Inv;

Verb 'taste'
    * noun                                      -> Taste;

Verb 'tell'
    * creature 'about' topic                    -> Tell
    * creature 'to' topic                       -> AskTo;

Verb 'think'
    *                                           -> Think;

Verb 'tie' 'attach' 'connect' 'fasten' 'fix'
    * noun                                      -> Tie
    * noun 'to' noun                            -> Tie;

Verb 'touch' 'feel' 'fondle' 'grope'
    * noun                                      -> Touch;

Verb 'transfer'
    * noun 'to' noun                            -> Transfer;

Verb 'turn' 'rotate' 'screw' 'twist' 'unscrew'
    * noun                                      -> Turn
    * noun 'on'                                 -> Switchon
    * noun 'off'                                -> Switchoff
    * 'on' noun                                 -> Switchon
    * 'off' noun                                -> Switchoff;

Verb 'unlock'
    * noun 'with' held                          -> Unlock;

Verb 'wait' 'z//'
    *                                           -> Wait;

Verb 'wake' 'awake' 'awaken'
    *                                           -> Wake
    * 'up'                                      -> Wake
    * creature                                  -> WakeOther
    * creature 'up'                             -> WakeOther
    * 'up' creature                             -> WakeOther;

Verb 'wave'
    *                                           -> WaveHands
    * noun                                      -> Wave
    * noun 'at' noun                            -> Wave
    * 'at' noun                                 -> WaveHands;

Verb 'wear' 'don'
    * held                                      -> Wear;

Verb 'yes' 'y//'
    *                                           -> Yes;

! ------------------------------------------------------------------------------
!  This routine is no longer used here, but provided to help existing games
!  which use it as a general parsing routine:

[ ConTopic w;
    consult_from = wn;
    do w = NextWordStopped();
    until (w == -1 || (w == 'to' && action_to_be == ##Answer));
    wn--;
    consult_words = wn - consult_from;
    if (consult_words == 0) return -1;
    if (action_to_be == ##Answer or ##Ask or ##Tell) {
        w = wn; wn = consult_from; parsed_number = NextWord();
        if (parsed_number == 'the' && consult_words > 1) parsed_number = NextWord();
        wn = w;
        return 1;
    }
    return 0;
];

! ------------------------------------------------------------------------------
!  Final task: provide trivial routines if the user hasn't already:
! ------------------------------------------------------------------------------

Default Story           0;
Default Headline        0;
Default d_obj           NULL;
Default u_obj           NULL;

Stub AfterLife         0;
Stub AfterPrompt       0;
Stub Amusing           0;
Stub BeforeParsing     0;
Stub ChooseObjects     2;
Stub DarkToDark        0;
Stub DeathMessage      0;
Stub Epilogue          0;
Stub GamePostRoutine   0;
Stub GamePreRoutine    0;
Stub InScope           1;
Stub LookRoutine       0;
Stub NewRoom           0;
Stub ObjectDoesNotFit  2;
Stub ParseNumber       2;
Stub ParserError       1;
Stub PrintTaskName     1;
Stub PrintVerb         1;
Stub TimePasses        0;
Stub UnknownVerb       1;

#Ifdef TARGET_GLULX;
Stub HandleGlkEvent    2;
Stub IdentifyGlkObject 4;
Stub InitGlkWindow     1;
#Endif; ! TARGET_GLULX

#Ifndef PrintRank;
[ PrintRank; "."; ];
#Endif;

#Ifndef ParseNoun;
[ ParseNoun obj; obj = obj; return -1; ];
#Endif;

#Ifdef INFIX;
Include "infix";
#Endif;

! ==============================================================================

Undef LIBRARY_STAGE; Constant LIBRARY_STAGE = AFTER_GRAMMAR;

#Ifnot;		! LIBRARY_STAGE < AFTER_GRAMMAR but ~= AFTER_VERBLIB
Message "Error: 'verblib' needs to be correctly included before including 'grammar'. This will cause a big number of errors!";
#Endif;

#Ifnot;		! LIBRARY_STAGE >= AFTER_GRAMMAR : already included
Message "Warning: 'grammar' included twice; ignoring second inclusion. (Ignore this if this is on purpose.)";
#Endif;

#Ifnot;		! LIBRARY_STAGE is not defined
Message "Error: 'parser', then 'verblib' need to be correctly included before including 'grammar'. This will cause a big number of errors!";
#Endif;

! ==============================================================================
