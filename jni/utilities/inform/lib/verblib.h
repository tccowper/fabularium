! ==============================================================================
!   VERBLIB:  Front end to standard verbs library.
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
!   In your game file, Include three library files in this order:
!       Include "Parser";
!       Include "VerbLib";
!       Include "Grammar";
! ==============================================================================

System_file;

#Ifdef		LIBRARY_STAGE;
#Iffalse	LIBRARY_STAGE >= AFTER_VERBLIB;	! if not already included
#Iftrue		LIBRARY_STAGE == AFTER_PARSER;	! if okay to include it

! ------------------------------------------------------------------------------

Default AMUSING_PROVIDED    1;
Default MAX_CARRIED         100;
Default MAX_SCORE           0;
Default NUMBER_TASKS        1;
Default OBJECT_SCORE        4;
Default ROOM_SCORE          5;
Default SACK_OBJECT         0;
Default TASKS_PROVIDED      1;

#Ifndef task_scores;
Array  task_scores -> 0 0 0 0;
#Endif;

Array  task_done -> NUMBER_TASKS;

#Ifndef LibraryMessages;
Object LibraryMessages;
#Endif;

#Ifndef NO_PLACES;
[ ObjectsSub; Objects1Sub(); ];
[ PlacesSub;  Places1Sub(); ];
#Endif; ! NO_PLACES

#Ifdef USE_MODULES;
Link "verblibm";
#Ifnot;
Include "verblibm";
#Endif; ! USE_MODULES

! ==============================================================================

Undef LIBRARY_STAGE; Constant LIBRARY_STAGE = AFTER_VERBLIB;

#Ifnot;		! LIBRARY_STAGE < AFTER_VERBLIB but ~= AFTER_PARSER
			! (this shouldn't happen because if 'parser' isn't there, LIBRARY_STAGE isn't defined)
Message "Error: 'parser' needs to be correctly included before including 'verblib'. This will cause a big number of errors!";
#Endif;

#Ifnot;		! LIBRARY_STAGE >= AFTER_VERBLIB: already included
Message "Warning: 'verblib' included twice; ignoring second inclusion. (Ignore this if this is on purpose.)";
#Endif;

#Ifnot;		! LIBRARY_STAGE is not defined (likely, 'parser' hasn't been included)
Message "Error: 'parser' needs to be correctly included before including 'verblib'. This will cause a big number of errors!";
#Endif;

! ==============================================================================
