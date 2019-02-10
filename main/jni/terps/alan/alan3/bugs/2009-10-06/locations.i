-- ALAN NEW LIBRARY: LOCATIONS (file name: 'locations.i')


-- This library file defines the default directions (exits) and the location 'nowhere', a useful
-- place to locate things when you want to remove them from play.
-- This file also defines two specific location types: rooms (= indoor locations) and
-- sites (= outdoor locations). In addition, a class 'dark_location' is defined in this file.
-- You may modify this file in any way that suits your purposes.
-- To use this file, you should have it in the same folder as your source code file , and the line
--
-- IMPORT 'locations.i'.
--
-- in your source code.



-- =======================================================

-- 1. The location 'nowhere' and the default directions

-- =======================================================


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






-- =======================================================================

-- 2. Location classes 'room' and 'site' for indoor and outdoor locations

-- =======================================================================


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





THE outdoor ISA LOCATION
END THE outdoor.


THE indoor ISA LOCATION
END THE indoor.


EVERY room ISA LOCATION	AT indoor 
END EVERY.


EVERY site ISA LOCATION	AT outdoor
END EVERY.


EVERY roomobject ISA OBJECT AT indoor 
END EVERY.


EVERY siteobject ISA OBJECT AT outdoor
END EVERY.


THE floor ISA roomobject
	IS NOT takeable.
	IS NOT movable.
	DESCRIPTION ""

	VERB examine DOES ONLY 
		"You notice nothing unusual about the floor here." 
	END VERB.

	VERB look_under, look_through, look_behind
		DOES ONLY "That's not possible."
	END VERB.

END THE.


THE walls ISA roomobject
	IS NOT takeable.
	IS NOT movable.
	DESCRIPTION ""

	VERB examine DOES ONLY 
		"You notice nothing unusual about the walls here." 
	END VERB.

	VERB look_under, look_through, look_behind
		DOES ONLY "That's not possible."
	END VERB.

END THE.


SYNONYMS wall = walls.


THE ceiling ISA roomobject
	IS NOT reachable.	
	DESCRIPTION ""
	
	VERB examine DOES ONLY 
		"You notice nothing unusual about the ceiling here." 
	END VERB.

	VERB look_under, look_through, look_behind
		DOES ONLY "That's not possible."
	END VERB.

END THE.


THE ground ISA siteobject
	IS NOT takeable.
	IS NOT movable.
	DESCRIPTION ""
	
	VERB examine 
		DOES ONLY "You notice nothing unusual about the ground here." 
	END VERB.

	VERB look_under, look_through, look_behind
		DOES ONLY "That's not possible."
	END VERB.

	VERB dig
		DOES ONLY "The ground is not suitable for digging here."
	END VERB.

END THE.


THE sky ISA siteobject
	IS NOT reachable.
	DESCRIPTION ""
	
	VERB examine DOES ONLY 
		"You notice nothing unusual about the sky." 
	END VERB.

	VERB look_under, look_through, look_behind
		DOES ONLY "That's not possible."
	END VERB.

END THE.




-- NOTE: it is often a good idea to modify the 'examine' verb for the above objects.
-- Here is an example for 'walls':

-- THE walls ISA roomobject
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
-- END THE walls.
--
-- In other words, making the description of the walls, etc., individual for each room 
-- gives a better impression than if you just use the default messages. 








-- ==========================================================

-- 3. Dark locations

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

	DESCRIPTION 
		CHECK COUNT ISA LIGHTSOURCE, IS lit, HERE > 0 
			ELSE "It is pitch black. You can't see anything at all." 

END EVERY dark_location. 


WHEN CURRENT LOCATION IS NOT lit AND COUNT ISA lightsource, IS lit, HERE > 0 => 
MAKE CURRENT LOCATION lit. 
LOOK. 


WHEN CURRENT LOCATION ISA dark_location AND CURRENT LOCATION IS lit
AND COUNT ISA lightsource, IS lit, HERE = 0 => 
MAKE CURRENT LOCATION NOT lit. 
"It is now pitch black."





-- To define a dark location, use the formulation seen in the following example: 


-- THE basement ISA dark_location
-- 	EXIT up TO kitchen.
-- ...
-- END THE.


-- The description of a dark_location will automatically be: "It is pitch black.
-- You can't see anything at all."
-- If you add a description to a dark location, this description will be shown only
-- if/when the location will be lit by any means:

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

-- Note that you cannot change the name of a location mid-game. Thus, if you make a dark location 
-- called e.g. 'Darkness' and make it lit at some point in the game, the name will still be 'Darkness' 
-- even if the location description can be changed to describe the illuminated location. 
-- To change the name, you must locate the hero in another location when the dark location 
-- is lit. For example,
--
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
-- WHEN lantern IS lit AND hero AT darkness => LOCATE hero AT treasure_chamber.
--
--




