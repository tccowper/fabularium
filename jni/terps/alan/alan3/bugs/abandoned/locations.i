-- ALAN Standard Library v1.00
-- Locations (file name: 'locations.i')


-- This library file defines the default directions (exits) and the location 'nowhere', 
-- a useful place to locate things when you want to remove them from play.
-- This file also defines three specific location classes: rooms (= indoor locations),
-- sites (= outdoor locations) as well as the class 'dark_location'. 
-- Finally, the attributes 'visited' and 'described' as well as the instance 'room' 
-- (to make possible player commands such as 'examine room' and 'exit room') are defined.
-- You may modify this file in any way that suits your purposes.
-- To use this file, you should have it in the same folder as your source code file, 
-- and the line
--
-- IMPORT 'locations.i'.
--
-- in your source code.



-- ========================================================


-----  1. The location 'nowhere' and the default directions


-- ========================================================


THE nowhere ISA LOCATION

	EXIT  
		north, 
		south, 
		east, 
		west, 
		northeast, 
		southeast, 
		northwest, 
		southwest, 
		up, 
		down, 
		'in', 
		out 
		
		TO nowhere.

		
END THE nowhere.


SYNONYMS
		n = north.
		s = south.
		e = east.
		w = west.
		ne = northeast.
		se = southeast.
		nw = northwest.
		sw = southwest.
		u = up.
		d = down.


-- Note:
 

-- 1) the directions defined above (and their synonyms) are not predefined in or 
-- hardwired to the interpreter in any way, so you can replace them altogether or add new 
-- ones to be used alongside with them.


-- 2) when you want to remove things from play, you can
--
-- LOCATE [object] AT nowhere.
--
-- e.g.
-- 
-- THE piece_of_paper ISA OBJECT
-- ...
--    VERB tear
-- 		DOES ONLY "You tear the piece of paper to shreds."
-- 		LOCATE piece_of_paper AT nowhere.
-- 	END VERB.
--
-- END THE piece_of_paper.

-- "The evil Professor Murray bursts out laughing."


-- =========================================================================


----- 2. Location classes 'room' and 'site' for indoor and outdoor locations


-- =========================================================================


-- ROOM and SITE are optional location classes you can use to ease up coding.
-- All ROOMS have a floor, walls and a ceiling. All SITES have a ground and a sky.
-- Thus, you will be able to define e.g.
--
-- THE kitchen ISA ROOM
-- 
-- and it will automatically have a floor, walls and a ceiling,
-- 
-- or:
--
-- THE greenmeadow ISA SITE
--
-- and the ground and the sky are automatically found in that location.
--
--
-- Of course, you will still be able to define locations in the usual way, e.g.
--
-- THE kitchen ISA LOCATION
--
-- etc, but the floor, walls and ceiling won't be automatically included there.
-- The walls, floor, ceiling, ground and sky are not takeable or movable.
-- This library file also defines the sky and the ceiling to be out of reach, 
-- so that they can't be e.g. touched.

-- (We make use of ALAN's nested locations feature in the following definitions: )


THE outdoor ISA LOCATION
END THE outdoor.


THE indoor ISA LOCATION
END THE indoor.


EVERY room ISA LOCATION	AT indoor 
END EVERY.


EVERY site ISA LOCATION	AT outdoor
END EVERY.


EVERY room_object ISA OBJECT AT indoor 
END EVERY.


EVERY site_object ISA OBJECT AT outdoor
END EVERY.


THE floor ISA room_object
	IS NOT takeable.
	IS NOT movable.
	CONTAINER 			-- to allow 'empty/pour something on floor'
	DESCRIPTION ""


	VERB examine DOES ONLY 
		"You notice nothing unusual about the floor." 
			IF hero AT lr THEN "$p$p(The Kitchen)$nYou notice nothing unusual about the floor." END IF.
	END VERB.


	-- As we have declared the floor a container, we will disable some verbs
	-- defined to work with containers:


	VERB empty_in, pour_in
	   WHEN cont
		DOES ONLY "That's not something you can $v things into."
				IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.


	VERB look_in
		DOES ONLY "That's not possible."
			IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.


	VERB put_in
	   WHEN cont
		DOES ONLY "That's not something you can $v things into."
			IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.


	VERB take_from
	   WHEN holder
		DOES ONLY "If you want to pick up something, just TAKE it."
				IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.


	VERB throw_in
	   WHEN cont
		DOES ONLY "That's not something you can $v things into."
			IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.

	VERB touch
		DOES ONLY "You feel nothing unexpected."
			IF hero AT lr THEN "$p$p(The Kitchen)$nYou feel nothing unexpected." END IF.
	END VERB.
		
	


END THE.


THE wall ISA room_object
	NAME wall NAME walls
	IS NOT takeable.
	IS NOT movable.
	DESCRIPTION ""


	VERB examine 
		DOES ONLY "You notice nothing unusual about the walls here." 
			IF hero AT lr THEN "$p$p(The Kitchen)$nYou notice nothing unusual about the walls here." END IF.
	END VERB.


	VERB touch
		DOES ONLY "You feel nothing unexpected."
			IF hero AT lr THEN "$p$p(The Kitchen)$nYou feel nothing unexpected." END IF.
	END VERB.

END THE.



THE ceiling ISA room_object
	IS NOT reachable.	
	DESCRIPTION ""

	
	VERB examine DOES ONLY 
		"You notice nothing unusual about the ceiling." 
			IF hero AT lr THEN "$p$p(The Kitchen)$nYou notice nothing unusual about the ceiling." END IF.
	END VERB.

END THE.



THE ground ISA site_object
	IS NOT takeable.
	IS NOT movable.
	CONTAINER				-- to allow 'empty/pour something on ground'
	DESCRIPTION ""

	
	VERB examine 
		DOES ONLY "You notice nothing unusual about the ground." 
	END VERB.


	VERB dig
		DOES ONLY "The ground is not suitable for digging here."
	END VERB.


	-- As we have declared the floor to be a container, we will disable some verbs
	-- defined to work with containers:


	VERB empty_in, pour_in
	   WHEN cont
		DOES ONLY "That's not something you can $v things into."
	END VERB.


	VERB look_in
		DOES ONLY "That's not possible."
	END VERB.


	VERB put_in
	   WHEN cont
		DOES ONLY "That's not something you can $v things into."
	END VERB.


	VERB take_from
	   WHEN holder
		DOES ONLY "If you want to pick up something, just TAKE it."
	END VERB.


	VERB throw_in
	   WHEN cont
		DOES ONLY "That's not something you can $v things into."
	END VERB.


END THE.



THE sky ISA site_object
	IS NOT reachable.
	DESCRIPTION ""

	
	VERB examine 
		DOES ONLY "You notice nothing unusual about the sky." 
	END VERB.


END THE.


-- We still declare some shared behaviour for all indoor and outdoor objects:


ADD TO EVERY room_object
    
	VERB put_behind, put_near, put_under
		WHEN bulk
		DOES ONLY "That's not possible."
			IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.

	VERB look_behind, look_through, look_under
		DOES ONLY "That's not possible."
			IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.

END ADD TO.	


ADD TO EVERY site_object
    
	VERB put_behind, put_near, put_under
		WHEN bulk
		DOES ONLY "That's not possible."
			IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.

	VERB look_behind, look_through, look_under
		DOES ONLY "That's not possible."
			IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	END VERB.

END ADD TO.	


-- NOTE: it is often a good idea to modify the 'examine' verb for the above objects.
-- Here is an example for 'wall':

-- THE wall ISA room_object
-- :
-- :
-- VERB examine DOES ONLY
--	IF hero AT kitchen
--		THEN "Numerous shelves, full of various kitchen utensils, line the walls."
--	ELSIF hero AT livingroom
--		THEN "The walls are decorated with flowery wallpaper here."
--	:

--	:
--	END IF.
-- END VERB.
-- END THE wall.
--
-- In other words, making the description of the walls, etc., individual for each room 
-- gives a better impression than if you just use the default messages. 



-- ==========================================================


----- 3. Dark locations


-- ==========================================================


ADD TO EVERY LOCATION
	IS lit. 
END ADD TO. 


EVERY dark_location ISA LOCATION
	IS NOT lit. 

	ENTERED

		IF COUNT ISA LIGHTSOURCE, IS lit, HERE > 0	
			THEN MAKE THIS lit.				
		END IF.

		IF COUNT ISA LIGHTSOURCE, IS lit, HERE = 0
			THEN MAKE THIS NOT lit.
		END IF.								

		-- These ENTERED statements take care
		-- of the dark location being correctly lit or not lit at entrance, 
		-- the WHEN rules below take care of the change when the hero is 
		-- already in the location.
		
	DESCRIPTION 
		CHECK COUNT ISA LIGHTSOURCE, IS lit, HERE > 0 
			ELSE "It is pitch black. You can't see anything at all." 

END EVERY dark_location. 




-- To define a dark location, use a formulation like the following: 


-- THE basement ISA dark_location
-- 	EXIT up TO kitchen.
-- ...
-- END THE.


-- The description of a dark_location will automatically be: "It is pitch black.
-- You can't see anything at all." (Edit the description in the code above to change this.)


-- If you add a description to a dark_location, this description will be shown only
-- if/when the location is lit by any means:

-- THE basement ISA dark_location
--    DESCRIPTION "Cobwebs and old junk are the only things you see here."
-- 	EXIT up TO kitchen.
-- END THE.


-- In darkness, you are not able to manipulate things other than turn on a lightsource 
-- and drop items you're carrying (these checks are found in 'verbs.i'). You can exit normally 
-- and use verbs that don't require seeing, such as 'smell', 'listen' and 'think'. 
-- If you are in a dark location with an NPC (= a non-player character), you are able to 
-- communicate with him by asking and telling but not by showing and giving. If you wish to 
-- change this, see the respective verbs in 'verbs.i' and edit their checks.


-- Note that you cannot change the name of a location mid-game. Thus, if you define a dark location 
-- called e.g. 'Darkness' and wish to make it lit at some point in the game, the name will still be 'Darkness' 
-- even if the location description can be changed to describe the illuminated location. 
-- To change the location name, you must locate the hero in another location when the dark location 
-- is lit. For example,
--
--  
-- THE lantern ISA LIGHTSOURCE      -- (see 'classes.i')
-- VERB turn_on
--    DOES 
--		IF hero AT darkness
--			THEN LOCATE hero AT treasure_chamber.
--		...
--		END IF.
-- ...
-- END VERB.
--
-- etc.
--
--
-- Alternatively, you can also use a rule, e.g.
-- 
--
-- WHEN lantern IS lit 
-- 	AND hero AT darkness 
-- THEN LOCATE hero AT treasure_chamber.



-- Suppose you want all dark_locations in the game to become lighted. It can be done e.g. like this:


-- THE main_power_switch ISA DEVICE
-- 	VERB switch_on
-- 		DOES ONLY
-- 			FOR EACH d1 ISA dark_location DO
--				MAKE d1 lit.
--			END EACH.		
-- 	END VERB.
-- END THE.

-- FOR EACH makes a loop that affects all instances of the class described after the ISA statement.
-- The 'd1' above is a temporary variable needed in the FOR EACH phrase and not anywhere 
-- else in the game code. You can name this variable in any way you wish.



-- =====================================================================


----- 4. The attributes 'visited' and 'described' 


-- =====================================================================


-- A location has the value 'visited 0' until the hero visits it for the first time, and the
-- value increases on every subsequent visit.
-- This helps when you need to control if or how many times a location has been visited,
-- and also if you want the location description to be different after the first visit.
 
-- A location has the value 'described 0' before the first location description,
-- and the value increases every time the description is shown.
-- This distinction is handy when you want the first-time description of a location to be different from
-- the subsequent ones (even if the hero is in the location still for the first time).


ADD TO EVERY LOCATION
	HAS visited 0.	
	HAS described 0.
	
	ENTERED
		 IF CURRENT ACTOR = hero
			THEN 
			 	INCREASE visited OF THIS.
	       		INCREASE described OF THIS.   -- this value also increases after 'look' (see 'verbs.i'.).
		 END IF.

END ADD TO.						


-- A location not visited at all has the 'visited' value 0. When the hero enters it 
-- the first time, the 'visited' value will change to 1. On the second visit the value 
-- will be 2, etc. 
-- Now, in your source code you can define something like the following:


-- THE kitchen ISA LOCATION
-- 	DESCRIPTION 
--		"You are in the kitchen."
--		IF visited OF THIS = 1
--			THEN "This is your first time here."
--			ELSE "You remember you've been here before."		
--		END IF.
--	...
-- END THE.

-- Note that if you have an NPC moving around in the game, the visited value of any location
-- will increase when the NPC enters the location, as well (ENTERED applies to all moving actors). 
-- This is most often not what is wanted, and that's why the 'if' statement 
-- (IF CURRENT ACTOR = hero) is included above. 


-- You can also check whether the hero has been in a location if needed:

-- THE king ISA ACTOR
--	...
--	VERB ask
--		WHEN act
--			IF topic = treasure_chamber
--				THEN
--					IF visited OF treasure_chamber = 0
--						THEN "You are not supposed to know anything about the treasure
--						      chamber - you haven't found it yet."
--						ELSE """Just take what you want from the chamber"", the king smiles."
--					END IF.
--			...
--			END IF.
--	END VERB.
-- END THE.
			

-- etc.


-- Suppose you want the location description to be different after the first time the description is shown,
-- even if you are in the location still for the first time. Then, you can use the 'described' attribute, e.g.:


-- THE library ISA ROOM
--	DESCRIPTION
--		IF described OF THIS = 1
--			THEN "There is an old man reading at a table in one of the corners."
--			ELSE "The old man keeps on reading at his table."
--		END IF.
-- END THE.



-- =======================================================================


-- 5. The instance 'room'


-- =======================================================================


-- Through defining the instance 'room' we make possible player commands such as 'examine room' 
-- and 'exit room', among others. These commands only work in indoor locations.
-- We have to identify this instance differently from 'room' which has already been 
-- defined as a class above, that's why we identify it here as 'current_room'. Through the NAME
-- statement, it can be still called a 'room' in-game.


THE current_room ISA OBJECT
	AT indoor
	NAME 'room'
	IS NOT takeable.
	IS NOT movable.
	CONTAINER			-- to allow the player command 'exit room'.
	DESCRIPTION ""


	VERB examine
		DOES ONLY LOOK. 
			INCREASE described OF CURRENT LOCATION.		
	END VERB.				


	VERB search
		DOES ONLY "Try examining one thing at a time."
			IF hero AT lr THEN "$p$p(The Kitchen)$nTry examining one thing at a time." END IF.
	END VERB.


	VERB enter
		DOES ONLY "You must state a direction where to go."
			IF hero AT lr THEN "$p$p(The Kitchen)$nYou must state a direction where to go." END IF.
	END VERB.


	VERB 'exit'
		DOES ONLY "You must state a direction where to go."
			IF hero AT lr THEN "$p$p(The Kitchen)$nYou must state a direction where to go." END IF.

			
	END VERB.


	VERB look_under, look_through, look_behind
		DOES ONLY "That's not possible."
			IF hero AT lr THEN "$p$p(The Kitchen)$nThat's not possible." END IF.
	END VERB.


END THE.


THE kitchen_object ISA ROOM
	NAME kitchen
	DESCRIPTION ""
END THE.

