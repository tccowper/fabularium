!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
!       DOORS2          An INFORM 6.15 library creating doors with
!                                                parsed names for directions
!                       By Max Kalus <max.kalus@student.uni-augsburg.de>
!
!   What can this module do?
!   ========================
!   In some games doors will be used a great deal. If you do, you might
!   make the experience, that, if you have two doors in one room, the
!   player might want to know which direction each of these leads to.
!   He might even act acordingly, and enter "look at north door". The
!   problem with "north" will be, that on the other side, the door
!   will be "south", so giving "north" and "south" as names might not
!   be the best thing to do. The case gets especially unnerving, if
!   you have two doors in the same room, both leading north-south
!   directions.
!
!   This module will relieve everybody with above problems. This
!   module provides a class called DirDoor (inherits from Connector if
!   "doors.h" is included, see below). DirDoor's parse_name routine
!   looks up direction names and parses them accordingly. Of course
!   you can still use your name Property for each individual door.
!   DirDoor takes care of these names, too.
!
!   Example:
!
!   DirDoor My_Door "door"
!		   with
!		   name "door" "enchanted" "unbreakable" "boring",
!		   s_to Room_A,
!		   n_to Room_B;
!
!   If in Room_A, you could type "look at southern door", while in
!    Room_B you could tell the parser "open north door". Nifty, eh?
!
!   Using Doors2 with "doors.h"
!   ===========================
!
!   This module is intended to be used with L. Ross Raszewski's
!   "doors.h", but can be used without it. If you use it with doors.h,
!   please make sure you include "doors2.h" *after* "doors.

#ifndef Connector;
class Connector;
#endif;

Class DirDoor
	class Connector,
	with
	parse_name [n word;
		word = NextWordStopped();
		while (word ~= -1)
		{
			if (IsAWordIn(word,self,name) == 1) n++;
			if (location == self.s_to && (word == 'north' or 'northern')) n++;
			if (location == self.n_to && (word == 'south' or 'southern')) n++;
			if (location == self.e_to && (word == 'west' or 'western')) n++;
			if (location == self.w_to && (word == 'east' or 'eastern')) n++;
			if (location == self.se_to && (word == 'northwest' or 'northeastern')) n++;
			if (location == self.sw_to && (word == 'northeast' or 'northeastern')) n++;
			if (location == self.ne_to && (word == 'southwest' or 'southwestern')) n++;
			if (location == self.nw_to && (word == 'southeast' or 'southeastern')) n++;
			word = NextWordStopped();
		}
		return n;
	],
	has door;

! -------------- IsAWordIn -----------------
#ifndef IsAWordIn;
[ IsAWordIn w obj prop k l m;
k=obj.&prop; l=(obj.#prop)/2;
for (m=0:m<l:m++)
if (w==k-->m) rtrue;
rfalse;
];
#endif;

