! -
! MOVECLASS, a library file to provide random, directed and 'intelligent'
!            movement for NPCs
!
! Version 8.10, written by Neil Brown          neil@highmount.demon.co.uk
!                      and Alan Trewartha      alan@alant.demon.co.uk
!               with Glulx additions by Matthew T. Russotto 13 March 2001
!
! Last altered 5th April 2001.
!
! The functions of this library are too complex to go into here, so please
! refer to the brief manual which should be near to where you found this
! file, and is named 'moveman.txt'.
!
! If you are including the library file FOLLOWER.H in your game code, please
! include this file AFTERWARDS and not before, otherwise errors will occur.
! -

System_file;
! This is necessary to compile with Graham's current Inform 6.21 compiler.
  #ifndef WORDSIZE;
  Constant TARGET_ZCODE;
  Constant WORDSIZE 2;
  #endif;

                                     ! NPC PROPERTIES
Property before_action;              ! Run before moving.
Property after_action;               ! Run after a successful move.
Property caprice alias time_left;    ! %age chance of moving when random

Property npc_open;                   ! A property of doors

Global path_size_limit = 10;         ! Depth of path searching

Constant   RANDOM_MOVE = 0;          ! The different possible move_types
Constant    AIMED_MOVE = 1;
Constant       NO_MOVE = 2;
Constant   PRESET_MOVE = 3;

Constant      ANY_PATH = $$00000000; ! The different types of AIMED_MOVEs.
Constant UNLOCKED_PATH = $$00001000; ! Bitmaps so that they can be combined
Constant     OPEN_PATH = $$00010000; ! in principle
Constant DOORLESS_PATH = $$00100000;

!Ifndef NPCRoom;
!  Class NPCRoom with link_data 0 0 0;
!EndIf;

#ifdef TARGET_ZCODE;
 Class MTR_npcdirclass
  with npc_dirs 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0;
                        ! The calculated directions that the NPC takes.
                        ! Note this is a word array, but the dirs are held as
                        ! single bytes, so a path of 64 moves is possible.
#ifnot; !TARGET_GLULX
 Class MTR_npcdirclass
  with npc_dirs 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0;
                        ! The assumption that a direction will be a small
                        ! (byte-sized) integer is bogus in Glulx
                        ! However, Glulx allows big properties, so we don't
                        ! lose.  --MTR
#endif;



Class moveclass
 class MTR_npcdirclass
  with move_type
           RANDOM_MOVE, ! The default move_type is to move randomly
       caprice      20, ! Chance the NPC will move each turn when RANDOM_MOVE
       prepath_name  0, ! The name of the predetermined path array
       prepath_size  0, ! The length of the set path
       npc_stage     0, ! Position along set path array
       npc_target,      ! The target destination
       npc_blocked [; NPC_Path(self,RANDOM_MOVE); ],
                        ! Alternatively do nothing and wait for the path to
                        ! unblock. Or, more intelligently look for a less
                        ! restrictive path.
       npc_ifblocked 0, ! Free for use by npc_blocked
       npc_arrived [; NPC_Path(self,RANDOM_MOVE); ],
                        ! Redefine this within the actual NPC object
                        ! for more sophisticated results. Deals with what
                        ! happens when an NPC arrives at its destination. In
                        ! this case, it returns to random movement.
       walkoff "walks off",
       walkon  "arrives",
       follow_action,   ! } In case Follower has been included but the NPC
       follow_object,   ! } isn't of FollowClass.
       daemon [ i n k;
           if (RunRoutines(self,before_action)) rtrue;

           switch(self.move_type)
           { 0, RANDOM_MOVE:                          ! Random movement
#ifdef DEBUG;
if (parser_trace>1)
print "[RANDOM_MOVE daemon for ", (the) self ,"]^";
#endif;
              if (random(100)>self.caprice) rfalse;
              objectloop (i in compass)
                if (LeadsTo(i,parent(self),ANY_PATH))
                {  n++;
#ifdef DEBUG;
if (parser_trace>1)
print "[Choice ",n, ": ",(GiveDir) i ,"]^";
#endif;
                }
              if (n==0) rfalse;
              k=random(n); n=0;                       ! Choose one direction
#ifdef DEBUG;
if (parser_trace>1)
print "[Choosing ",k, "]^";
#endif;
              objectloop (i in compass)
              { if (LeadsTo(i,parent(self),ANY_PATH)) n++;
                if (n==k)
                {  MoveNPCDir(self,i);
                   break;
                }
              }

             1, AIMED_MOVE :                   ! Moving on a calculated path
#ifdef TARGET_ZCODE;
              i=self.&npc_dirs->self.npc_stage;
#ifnot; ! TARGET_GLULX
              i=self.&npc_dirs-->self.npc_stage;
#endif; ! TARGET

#ifdef DEBUG;
if (parser_trace>1)
print "[AIMED_MOVE daemon moving ", (the) self, " ", (GiveDir) i,"]^";
#endif;
              if (i==0  || MoveNPCDir(self,i)) ! Routine only called if i~=0
                  self.npc_stage++;
              if (parent(self)==self.npc_target)
                  self.npc_arrived();

             2, NO_MOVE :                                ! Not moving at all
#ifdef DEBUG;
if (parser_trace>1)
print "[NO_MOVE daemon for ", (the) self, " doing nothing]^";
#endif;

             3, PRESET_MOVE :               ! Moving on a predetermined path
#ifdef TARGET_ZCODE;
              i=(self.prepath_name)->self.npc_stage;
#ifnot; ! TARGET_GLULX
              i=(self.prepath_name)-->self.npc_stage;
#endif; ! TARGET

#ifdef DEBUG;
if (parser_trace>1)
print "[PRESET_MOVE daemon moving ", (the) self," ", (GiveDir) i, "]^";
#endif;
              if (i==0 ||MoveNPCDir(self,i))   ! Routine only called if i~=0
                  self.npc_stage++;
              if (self.npc_stage>=self.prepath_size)
                  self.npc_arrived();

       default: "** MoveClass Error: move_type set to an unacceptable
                                     value for ", (the) self, " **";
          }
        ];



[ NPC_path npc   movement_type targetroom path_type
           steps last_room     found      i j k;

  if (metaclass(movement_type)==Object && movement_type ofclass NPCRoom)
  {   path_type=targetroom;
      targetroom=movement_type;
      movement_type=AIMED_MOVE;            ! To stay compatible with old code
  }
#ifdef DEBUG;
if (parser_trace>1)
{print "^[NPC_Path setting ", (the) self," to ";
 switch (movement_type)
 {     NO_MOVE: print "NO_MOVE";
   RANDOM_MOVE: print "RANDOM_MOVE";
   PRESET_MOVE: print "PRESET_MOVE";
    AIMED_MOVE: print "AIMED_MOVE towards ", (name) targetroom;
       default: print "**UNDEFINED**";
 }
 print "]^";
}
#endif;

  if (~~(npc ofclass moveclass))
  { print "*** MoveClass Error: NPC_path called for non-moveclass object '",
           (the) npc, "' ***";
    rfalse;
  }

  if (movement_type==NO_MOVE)              ! Call to set NO_MOVE
  {  npc.move_type=NO_MOVE;
     rtrue;
  }

  if (movement_type==RANDOM_MOVE)          ! Call to set RANDOM_MOVE
  {  npc.move_type=RANDOM_MOVE;
     if (path_type~=0)
         npc.caprice=path_type;
     rtrue;
  }

  if (movement_type==PRESET_MOVE)          ! Call to set PRESET_MOVE
     return NPCprepath(npc,targetroom,path_type);

  if (movement_type~=AIMED_MOVE)
     rfalse;

! VZEFH check
  if (parent(npc)==0)
  { print "^*** MoveClas Error: NPC_path called for object '",
           (the)npc, "' which has parent==0 ***";
    rfalse;
  }

  if (~~(parent(npc) ofclass NPCRoom))
     rfalse;


  ! link_data-->0 is previous ROOM in the linked list of rooms searched
  ! link_data-->1 is previous STEP on the possible path
  ! link_data-->2 is previous STEP_DIR along the possible path

  last_room=parent(npc);        ! All rooms being searched are linked in a list
  last_room.&link_data-->0=0;   ! starting with 'last_room' and stepping back
  last_room.&link_data-->1=-1;  ! along the link_room of each room.

  if (last_room==targetroom)
  { found=true;                 ! Allowing the npc_arrived property to run
    steps=0;                    ! if the npc starts in targetroom
    npc.&npc_dirs-->0=0;
  }
  else
  { for (steps=1: steps<path_size_limit:steps++)
    { i=last_room;                       ! Start at the end of the list
      while (i ~= 0)
      { objectloop (j in Compass)        ! Explore all directions
        { k=LeadsTo(j,i,path_type);
          if (k ofclass NPCRoom &&
              k.&link_data-->1==0)       ! Only want 'NPCRoom's with a 0 STEP
          { k.&link_data-->1=i;          ! Add such rooms as a STEP on from 'i'
            k.&link_data-->2=j;          ! Store direction moved to get there
            k.&link_data-->0=last_room;  ! Add this room to the linked list
            last_room=k;
            if (k==targetroom) found=true;
#Ifdef DEBUG;
if (parser_trace>1) print "[Found: ",(name) k, "]^";
#Endif;
          }
          if (found) break;
        }
        if (found) break;
        if (i.&link_data-->0==i.&link_data-->1)
          i=0;                           ! If link_data STEP = ROOM then the
        else                             ! remaining rooms on the linked list
          i=i.&link_data-->0;            ! have already been explored and we
      }                                  ! can end this iteration.
      if (found) break;
    }
  }

  if (found)
  { npc.move_type=AIMED_MOVE;            ! Set NPC to AIMED_MOVE
    npc.npc_target=targetroom;           ! and fill in all the details
    npc.prepath_size=steps;
    npc.npc_stage=0;
#ifdef DEBUG;
if (parser_trace>1)
print "[Path has ",steps, " steps...^";
#endif;
    i=last_room;                         ! Now go back to the end of the list
    while (i.&link_data-->1~=-1)
    {
#ifdef DEBUG;
if (parser_trace>1)
print (name) i," is ", (GiveDir) i.&link_data-->2, " of...^";
#endif;

#ifdef TARGET_ZCODE;
      npc.&npc_dirs->(steps-1)=i.&link_data-->2;
#ifnot; ! TARGET_GLULX
      npc.&npc_dirs-->(steps-1)=i.&link_data-->2;
#endif; ! TARGET

      steps--;                           ! Write npc_dirs with the STEP_DIRs
      i=i.&link_data-->1;                ! And go back along the STEPs

    }
#ifdef DEBUG;
if (parser_trace>1)
print "where we started!]^";
#endif;
  }
#Ifdef DEBUG;
if(parser_trace>1 && found==false) print "[No path found!]^";
#Endif;

  while (last_room~=0)
  {
#ifdef DEBUG;
if (parser_trace>4)
{print "[",(name) last_room," = ";
 print (object) last_room.&link_data-->0, ", ";
 print (object) last_room.&link_data-->1, ", ";
 print (object) last_room.&link_data-->2, "]^";
}
#endif;
     last_room.&link_data-->1=0;           ! Go back along the linked list
     last_room=last_room.&link_data-->0;   ! resetting the STEP data. Only
  }                                        ! NPCRooms with a 0 STEP are added to
                                           ! the linked list (see above)
  return found;
];



[ NPCPrePath npc path_array path_length fakevar;
  fakevar=fakevar;            ! In case code tries passing a room name too
  if (npc ofclass moveclass)
  { npc.npc_stage=0;
    npc.move_type=PRESET_MOVE;
    npc.prepath_name=path_array;
    npc.prepath_size=path_length;
  }
  else
  { "*** MoveClass Error: NPCPrePath called for non-moveclass object '",
     (the) npc, "' ***";
  }
];

Ifndef NOISY_DIR_TOS;
Message "** MoveClass::LeadsTo assuming quiet *_to (NOISY_DIR_TOS not defined) **";
Endif;

[ LeadsTo direction thisroom path_type k tmp tmp2;
#ifdef DEBUG;
   if (parser_trace>2)
     print "[LeadsTo ", (name)direction, " ", (name)thisroom, "]^";
#endif;

   if (~~(direction provides door_dir)) rfalse;
   if (~~(thisroom provides direction.door_dir)) rfalse;
   k=thisroom.(direction.door_dir);

   #ifdef NOISY_DIR_TOS;
     if (ZRegion(k)==2) rfalse;
   #endif;

   #ifndef NOISY_DIR_TOS;
     if (ZRegion(k)==2)
         k=RunRoutines(thisroom, direction.door_dir);
   #endif;

   if (ZRegion(k)~=1) rfalse;
   if (k has door)
   { if (path_type & DOORLESS_PATH) rfalse;
     if ((path_type & OPEN_PATH) && k hasnt open) rfalse;
     if ((path_type & UNLOCKED_PATH) && k has locked) rfalse;
     tmp=parent(k);
     move k to thisroom;
     tmp2=k.door_to();
     if (tmp==0) remove k; else move k to tmp;
     k=tmp2;
   }
   if (~~(k ofclass NPCRoom)) rfalse;
   return k;
];



[ MoveNpcDir npc direction i j p message;
  message=2;
  p=parent(npc);
  i=LeadsTo(direction,p, ANY_PATH);
  if (i==0)
  { npc.npc_blocked();
#ifdef DEBUG;
if (parser_trace>1)
print "[MoveNPCDir blocked: Direction leads nowhere]^";
#endif;
    rfalse;
  }

  j=p.(direction.door_dir);
  if (ZRegion(j)==2) j=j();
  if (j has door)
  { if (j provides npc_open)          ! npc_open returns
    { message=j.npc_open(npc);        ! 2 to go through door as normal
      if (message==false)             ! 1 to go through door and prevent
      { npc.npc_blocked();            !      walkon/walkoff run/printing
#ifdef DEBUG;                         ! 0 to stop npc using door
if (parser_trace>1)
print "[MoveNPCDir blocked: ", (the) j, "'s npc_open returned false]^";
#endif;
        rfalse;
      }
    }
    else
      if (j hasnt open)
      {   npc.npc_blocked();
#ifdef DEBUG;
if (parser_trace>1)
print "[MoveNPCDir blocked: ", (the) j, " is closed with no npc_open]^";
#endif;
          rfalse;
      }
  }

  MoveNPC(npc, i, ##Go, direction);

  if (p==location && message==2)      ! If npc_open used then it must return 2
  { if (ZRegion(npc.walkoff)==3)      ! if it wants walkon/walkoff to execute
        print "^", (The) npc, " ", (string) npc.walkoff,
              " ", (GiveDir) direction, ".^";
    else
        npc.walkoff(direction);
  }

  if (parent(npc)==location && message==2)
  { if (ZRegion(npc.walkon)==3)
      print "^", (The) npc, " ", (string) npc.walkon, ".^";
    else
      npc.walkon(direction);
  }

  if (npc provides after_action) npc.after_action();
  rtrue;
];



Ifndef MoveNPC; ! Provides MoveNPC if program isn't including 'Follower'
[ MoveNPC npc dest actn objn;
  move npc to dest;
  actn=actn;
  objn=objn;
];
Endif;



[ GiveDir i;
  switch(i)
  { n_obj: print "to the north";
    s_obj: print "to the south";
    e_obj: print "to the east";
    w_obj: print "to the west";
   ne_obj: print "to the northeast";
   nw_obj: print "to the northwest";
   se_obj: print "to the southeast";
   sw_obj: print "to the southwest";
    u_obj: print "upwards";
    d_obj: print "downwards";
   in_obj: print "inside";
  out_obj: print "outside";
  }
];
