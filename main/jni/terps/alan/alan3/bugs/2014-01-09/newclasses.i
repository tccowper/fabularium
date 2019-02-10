-- ALAN Standard Library v1.10
-- Classes (file name: 'classes.i')


-- This library file defines various object and actor classes. 
-- Many of these classes are frequently  used in verb definitions in 'verbs.i' 
-- so they should be edited or removed with caution. However, to ease things up, 
-- it is mentioned at the beginning of every class below if and where the class
-- is cross-referenced in the other library files.


-- Contents:




-- 1. OBJECT CLASSES
-- ================= 



-- BACKGROUND
	-- Is present in the location but cannot be reached or taken.
	-- Doesn't appear automatically in room descriptions.
	-- A typical example of a background would be for example the sun, 
	-- or a mountain visible in the distance. 
	-- Compare this with 'scenery' below (a background object cannot be reached, 
	-- but a scenery object can.)


-- CLOTHING    
	-- Is a piece of clothing that behaves according to Alan Bampton's 'xwear.i' extension.
      -- The said extension has been fully assimilated to this library. 
      -- This extension prevents clothes from being worn in an illogical order, e.g. you 
	-- cannot put on a shirt if you are already wearing a jacket, and so forth.
	-- Also the verbs 'wear', 'remove' and 'undress' are defined here.


-- DEVICE  
	-- Is a  machine or an electronic device, for example a TV. Can be turned 
	-- (=switched) on and off if it is not broken.
	-- Attributes: 'on' and NOT 'on', NOT broken.
      -- Is described automatically as being either on or off when examined. 


-- DOOR 
	-- Can be opened, closed, locked and unlocked. 
	-- Is by default closed, not locked.
	-- Attributes: closeable, (NOT) closed, lockable, (NOT) locked.
	-- Is described automatically as being either open or closed when examined.


-- LIQUID 
	-- Can only be taken if it is in a container. You can fill something with it, 
	-- and you can pour it somewhere.
	-- A liquid is by default NOT drinkable.


-- LIGHTSOURCE 
	-- IS natural or NOT natural 
	-- (a natural lightsource is for example a match or a torch).
	-- Can be turned on and off, lighted and extinguished (= put out) if it 
      -- is not broken. A natural lightsource 
	-- cannot be turned on or off, it can only be lighted and extinguished (= put out).
	-- When examined, a lightsource is automatically supplied with a description of
	-- whether it is providing light or not.


-- LISTED_CONTAINER
	-- Is a special kind of container, the contents of which will be listed both after 
	-- 'look' (= in the room description), 'look in' and 'examine' (if the container is open). 
	-- (The contents of a normal container object are not listed after 'examine' but only 
	-- after 'look' (=room description) and 'look in').


-- SOUND 
	-- Can be listened to but not examined, searched, smelled or manipulated.
      -- Cannot initially be turned on or off, this has to be implemented manually by giving 
      -- the sound the 'switchable' attribute.


-- SUPPORTER 
	-- You can put things on this and you can stand on this. It is declared a container, 
	-- so you can take things from it, as well. When there's something on a supporter, 
      -- an automatic listing of it will appear in the room description and after 'examine'.


-- WEAPON  
	-- IS fireable (e.g. a cannon) or NOT fireable (e.g. a baseball bat).


-- WINDOW 
	-- Can be opened, closed, looked through and out of.
	-- Will be automatically described as being either open or closed when examined.



-- 2. ACTOR CLASSES
-- ================

-- the ACTORS are defined here to be NOT inanimate CONTAINERS (so that they can e.g.
-- receive and carry things. The 'contents' (= possessions) of an actor (except for the hero) 
-- are automatically listed after 'look' and 'examine'. (For the hero, the command 'inventory'
-- must be used.)
--
-- Actors are usually preceded by an article in-game:
-- e.g. "You see a man here."
--	  "There is nothing special about the dog."
-- unless they are declared as 'named'.
--
-- The following classes for actors are defined in this library:


-- PERSON 
	-- is able to talk (= 'CAN talk'). 


-- FEMALE
	-- a subclass of person (= is able to talk)
      -- can be referred to with the pronoun 'her'


-- MALE
	-- a subclass of person (= is able to talk)
      -- can be referred to with the pronoun 'him'





-- The contents end here. 



-- =============================================================

-- =============================================================
--
-- 1. Object classes
--
-- =============================================================

-- =============================================================





-- =============================================================


----- BACKGROUND


-- =============================================================


-- (This class is not cross-referenced elsewhere in this or any other library file.)


EVERY background ISA OBJECT
	IS distant.
	DESCRIPTION ""			
END EVERY.


-- A background object is present in the location but cannot be reached.
-- Note that a background object in ALAN3 is different from e.g. the backdrop in Inform7
-- in that a background object is at one location at a time only, unless
-- you use the nested locations feature in ALAN which makes the object available
-- in several locations. 
-- Here is an example where a ceiling lamp is located in the lobby, the bedroom 
-- and the living-room of a house, but not in other locations:


-- 1) First, define the area where the object(s) should be found: 

-- THE lamp_rooms ISA LOCATION   					
-- END THE.								

-- and then define which locations belong to that area:

-- THE lobby ISA LOCATION IN lamp_rooms
-- END THE.

-- THE bedroom ISA LOCATION IN lamp_rooms
-- END THE.

-- THE livingroom ISA LOCATION IN lamp_rooms
-- END THE.



-- 2) Then, place the background object in the area:

-- THE ceiling_lamp ISA BACKGROUND IN lamp_rooms
--	NAME ceiling lamp
-- END THE.


-- Now, the lamp is in scope in all of the above locations. (You'll have to include in the
-- location description manually though.)
 
-- Note that in this code you could define exits to other rooms of the house in the
-- normal way, without having to worry about the area at all, e.g.:

-- THE bedroom ISA LOCATION IN lamp_rooms
--	EXIT west TO upstairs_landing.
-- END THE.

-- THE upstairs_landing ISA LOCATION  
		-- ( = a location outside the defined area)
--	EXIT east TO bedroom.
-- END THE.

-- etc.

-- Naturally, you could also define a scenery object or a normal object to be
-- in several locations at once, in the same way. Note, however, that if you define 
-- a takeable object in this manner, it will disappear from the other locations once 
-- you take it in one location. Also, when manipulated (e.g. broken), the object
-- will be affected in all of the locations it is found.



-- ==============================================================


----- CLOTHING     (+ the verbs WEAR, REMOVE, UNDRESS)


-- ==============================================================


-- (See the file 'verbs.i', verbs 'inventory' and 'take' where the 
-- container 'worn', defined below, is used in the verb definitions.)


-- To use this class, see the documentation text right after the
-- code below.

-- This class makes use of Alan Bampton's 'xwear.i' extension
-- written originally for ALAN V2, converted here to V3 and 
-- assimilated fully to the present library. Thanks to Alan Bampton
-- for the permission to use the code here.


-----------------------------------------------------------------
-- First, we declare the container for clothing.
-----------------------------------------------------------------


-- an entity is present everywhere and thus the hero's clothing is always accessible:

THE worn ISA ENTITY							
	CONTAINER TAKING CLOTHING.			
		HEADER SAY hero_worn_header OF my_game.
		ELSE SAY hero_worn_else OF my_game.
END THE.





-------------------------------------------------------------------
-- Now, we define some common attributes for clothing as well as 
-- how the verbs 'wear' and 'remove' (and their synonyms) behave with this class.
-------------------------------------------------------------------


EVERY clothing ISA OBJECT

	IS wearable.
	IS sex 0.
	IS headcover 0.
	IS handscover 0.
	IS feetcover 0.
	IS topcover 0.
	IS botcover 0.

	CONTAINER			
	-- to allow e.g. a wallet to be put into a jacket


	-- If the clothing contains something, e.g. if a jacket contains a wallet,
    	-- the wallet will be mentioned when the jacket is examined: 

	VERB examine
		DOES AFTER
			IF THIS IS NOT OPAQUE
				THEN 
					IF COUNT ISA OBJECT, IN THIS > 0		
						THEN LIST THIS.					
					END IF.							
			END IF.									
	END VERB.



  	VERB wear
		CHECK THIS NOT IN worn 
			ELSE SAY check_obj_not_in_worn1 OF my_game.
		AND THIS IS takeable
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_obj_takeable OF my_game. 
					ELSE SAY check_obj_takeable OF my_game. 
				END IF. 
		AND CURRENT LOCATION IS lit
			ELSE SAY check_current_loc_lit OF my_game.
		AND sex OF THIS = sex OF hero OR sex OF THIS = 0
			ELSE SAY check_clothing_sex OF my_game.
	
		DOES ONLY


--------------------------------------------------------------------
-- 'wear_flag' is a multi-purpose flag used for several purposes in 
-- this library, here it is reset to 0 before proceeding as a matter
-- of 'housekeeping' for the code.
--------------------------------------------------------------------


		SET wear_flag OF hero TO 0.


--------------------------------------------------------------------
-- First check to see if the player is carrying the item already, if
-- not, set the 'wear_flag' to 1 to indicate the item was picked up
-- in this turn.
--------------------------------------------------------------------


		IF THIS NOT IN hero 
			THEN
				SET wear_flag OF hero TO 1.
		END IF.

	
--------------------------------------------------------------------
--  Now see if the player can put this item on by testing 
--  all of its coverage attributes against the player's state.
--------------------------------------------------------------------


--------------------------------------------------------------------
-- First check the 'topcover' attributes, if 'obj' fails this test
-- then it means the hero is already wearing clothes that cover the
-- topcover area and those clothes are of the same layer or a layer 
-- that belongs on top of the 'obj' item. In either case it would 
-- NOT be possible to put on the 'obj'. To 'flag' this condition add
-- 5 to the 'wear_flag' attribute as an indicator this test failed.
--------------------------------------------------------------------


		IF topcover OF THIS <> 0 AND topcover OF THIS <= SUM OF topcover IN worn 
			THEN
				INCREASE wear_flag OF hero BY 5.
		END IF.

	
--------------------------------------------------------------------
-- Perform a similar test for other attributes.
--------------------------------------------------------------------


		--IF THIS IN tempworn 
			--THEN
			
		IF handscover OF THIS <> 0 AND handscover OF THIS <= SUM OF handscover IN worn 
			THEN
				INCREASE wear_flag OF hero BY 5.
		END IF.
	

		IF feetcover OF THIS <> 0 AND feetcover OF THIS <= SUM OF feetcover IN worn 
			THEN
				INCREASE wear_flag OF hero BY 5.	
		END IF.

	
		IF headcover OF THIS <> 0 AND headcover OF THIS <= SUM OF headcover IN worn 
			THEN
				INCREASE wear_flag OF hero BY 5.	
		END IF.


--------------------------------------------------------------------
--  botcover is a special case, adjust the 'tempcovered OF hero' 
--  attribute so that the code rejects non sensible options.
--  First of all, discount any coatlike clothes as these never 
--  affect ability to put on other lower body only garments.
--------------------------------------------------------------------


		SET tempcovered OF hero TO SUM OF botcover in worn.
		
		IF tempcovered OF hero >63 and botcover OF THIS < 33
			THEN 
				SET tempcovered OF hero TO tempcovered OF hero -64.
		END IF.


--------------------------------------------------------------------
-- Now discount any dress/ skirt coverall like clothes as these do 
-- not technically affect ability to put on lower body only clothes.
-- Special clause here excludes the full body coverage 'teddy' type
-- garment - as a skirt WOULD prevent that from being removed. 
-- ( dress/coat garments automatically prevent this by virtue of 
-- having higher 'topcover' settings than the teddy )
--------------------------------------------------------------------


		IF tempcovered OF hero >31 AND botcover OF THIS < 16 and botcover OF THIS <> 4 
			THEN
				SET tempcovered OF hero TO tempcovered OF hero -32.
		END IF.


--------------------------------------------------------------------
-- IF tempcovered OF hero is still > 15 then must have trousers 
-- type clothing on - therefore disallow wearing dress type clothing
-- because, although technically possible, it is not very sensible. 
--------------------------------------------------------------------


		IF tempcovered OF hero >15 AND botcover OF THIS > 16 
			THEN
				SET tempcovered OF hero TO tempcovered OF hero +16.
		END IF.


--------------------------------------------------------------------
--  From here down, clothes DO work as they do for other areas.
--------------------------------------------------------------------


		IF botcover OF THIS <> 0  AND botcover OF THIS <= tempcovered OF hero 
			THEN
				INCREASE wear_flag OF hero BY 5.		
		END IF.

	
--------------------------------------------------------------------
-- At this point, 'wear_flag' will be 0 if the obj was held by the
-- player and can be put on, or l if he picked it up this turn and 
-- it can be put on. Any higher value means one or more of the 
-- tests failed and the player cannot put on these clothes. 
--------------------------------------------------------------------


		IF wear_flag OF hero >1 
			THEN
				IF THIS NOT IN hero 
					THEN "You pick up the" SAY THE THIS. "."
				END IF.
				
				LOCATE THIS IN hero.
				EMPTY worn IN tempworn.	
				LIST tempworn.

				"Trying to put" SAY THE THIS. "on isn't very sensible."

				EMPTY tempworn IN worn.

		ELSIF wear_flag OF hero = 1 
			THEN
				LOCATE THIS IN worn.

				"You pick up the" SAY THE THIS.

				IF THIS IS NOT plural 
					THEN "and put it on."
					ELSE "and put them on."
				END IF.

		ELSE
			LOCATE THIS IN worn.
			"You put on" SAY THE THIS. "."
		END IF.

END VERB.



VERB remove
	CHECK THIS IN worn
		ELSE SAY check_obj_in_worn OF my_game.
	AND CURRENT LOCATION IS lit
		ELSE SAY check_current_loc_lit OF my_game.

	DOES ONLY
	
	SET wear_flag OF hero TO 0.

	
--------------------------------------------------------------------
-- Check the total 'topcover' of items worn. Because of the number
-- sequence used, by dividing the sum of the worn attributes by two 
-- and then comparing the result to the individual 'topcover' of the
-- obj in question, ( the former can only ever be greater than the 
-- latter if an article of clothing is worn that goes over 'obj' ) 
-- it's easy to tell if the obj ought to be removable. A temporary
-- attribute is used here because it needs to be manipulated. Once
-- again 'wear_flag' is used to indicate the results.
--------------------------------------------------------------------


	SET tempcovered OF hero TO SUM OF topcover IN worn /2.
	IF topcover OF THIS <> 0 AND topcover OF THIS < tempcovered OF hero
		 THEN
			INCREASE wear_flag OF hero BY 1.
	END IF.

		
--------------------------------------------------------------------
-- Perform a similar test for other attributes.
--------------------------------------------------------------------


	SET tempcovered OF hero TO SUM OF handscover IN worn /2.
	IF handscover OF THIS <> 0 AND handscover OF THIS < tempcovered OF hero 
		THEN
			INCREASE wear_flag OF hero BY 1.
	END IF.		


	SET tempcovered OF hero TO SUM OF feetcover IN worn /2.
	IF feetcover OF THIS <> 0 AND feetcover OF THIS < tempcovered OF hero 
		THEN
			INCREASE wear_flag OF hero BY 1.		
	END IF.	


	SET tempcovered OF hero TO SUM OF headcover IN worn /2.
	IF headcover OF THIS <> 0 AND headcover OF THIS < tempcovered OF hero 
		THEN
			INCREASE wear_flag OF hero BY 1.
	END IF.		


--------------------------------------------------------------------
-- botcover is a special case - first discount any coatlike clothes
-- as these do not affect ability to take off other lower garments.
--------------------------------------------------------------------


	SET tempcovered OF hero TO SUM OF botcover in worn.
	IF tempcovered OF hero >63 
		THEN 
			SET tempcovered OF hero TO tempcovered OF hero -64.
	END IF.


--------------------------------------------------------------------
-- Now discount any dress/ skirt coverall like clothes as these do 
-- not affect ability to take off other lower garments.	The 'teddy'
-- type garment is expressly NOT included in the exclusion here.
--------------------------------------------------------------------


	IF tempcovered OF hero >31 and botcover OF THIS <>4 
		THEN
			SET tempcovered OF hero TO tempcovered OF hero -32.
	END IF.


--------------------------------------------------------------------
-- Now process the manipulated value just as was done for the others
--------------------------------------------------------------------


	SET tempcovered OF hero TO tempcovered OF hero /2.
	IF botcover OF THIS <> 0 AND botcover OF THIS < tempcovered OF hero 
		THEN
			INCREASE wear_flag OF hero BY 1.
	END IF.


--------------------------------------------------------------------
-- Depending on the value of 'wear_flag' print and process the obj
-- as needed. If 'wear_flag' is NOT 0 then the clothes cannot be 
-- removed.
--------------------------------------------------------------------


	IF wear_flag OF hero >0 
		THEN
			LIST worn.
			"Trying to take" SAY THE THIS. "off isn't very sensible."
		ELSE
			LOCATE THIS IN hero.
			"You take off" SAY THE THIS. "." 
	END IF.
END VERB.


END EVERY.



--------------------------------------------------------------------
-- These attributes are used internally in the library - ignore! 
--------------------------------------------------------------------

ADD TO EVERY ACTOR
	IS tempcovered 0.
	IS wear_flag 0.
	IS sex 0.
END ADD TO.
	

--------------------------------------------------------------------
-- A container used to provide a temporary storage space - ignore! 
--------------------------------------------------------------------

THE tempworn ISA OBJECT
	CONTAINER TAKING CLOTHING.
	HEADER "You're already wearing"
END THE tempworn.



-----------------------------------------------------------------------
-- INSTRUCTIONS FOR USING THE CLOTHING CLASS 
-----------------------------------------------------------------------


-- Here is a quick overview for using the class 'clothing'. 

-- A piece of clothing in your game code should look 
-- something similar to the following four examples:


-- THE jacket ISA CLOTHING AT lobby
-- 	IS topcover 32.
-- END THE.


-- use IN to refer to containers:

-- THE jeans ISA CLOTHING IN wardrobe		
--	IS botcover 16.					
-- END THE.


-- IN worn = worn by the player character (hero):

-- THE hat ISA CLOTHING IN worn  		 
--	IS headcover 2.
-- END THE.


-- worn by an NPC called Joe:

-- THE sweater ISA CLOTHING IN joe_worn   
	-- Don't declare clothing attributes for NPCs (unless the hero is meant to take 
	-- and wear the NPC's clothing).
	-- NPCs cannot wear clothing in layers!   			
-- END THE. 			
		

-- Define separate containers like the above
-- for clothes worn by non-player characters.
-- If you defined here 'IN joe', the clothing
-- would be listed in Joe's possessions:
-- "You see Joe here. Joe is carrying a book and 
-- a sweater." For a concrete example of how to do 
-- this, scroll down to the class 'actor'.
-- Note also that if the piece of clothing worn
-- by an NPC is not meant to be takeable by the
-- player character, you should declare the
-- the piece of clothing to be NOT takeable.


-- In defining a piece of clothing, you should
--
--  1) define it ISA CLOTHING (and not: ISA OBJECT)
--
--  2) give it one of five attributes 'headcover', 'topcover', botcover', 'footcover'
--  or 'handcover'; sometimes two of these are needed.
--  Which attribute(s) to use depends on the type of clothing; see the clothing table below.

--  3) A number 2, 4, 8, 16, 32 or 64 needs to be added after the above attribute.
--  You cannot decide the number yourself; look it up from the clothing table below.
--  If the value of an attribute for a piece of clothing is 0 in the table, don't mention 
--  this attribute in connection with your clothing object.


-- The above is enough; the rest is then handled automatically by the library.


-- The quick guide ends here. The clothing table follows right below. The text following the 
-- table gives more details about the principles and the use of this class. 


-- The clothing table
-----------------------


-- Here is the chart showing a selection of fairly typical clothing items and the values to 
-- set to obtain appropriate behaviour. Should you wish to create an article of clothing not
-- listed, usually a bit of lateral thought as to what it is most like and where it fits into 
-- the scheme of things will suggest a workable set of values, but be aware that you MUST use 
-- values in this chart, simply adding things with intermediate values is probably going to 
-- create nasty bugs:


-- Clothing 			Headcover	Topcover 	Botcover 	Footcover 	Handcover

-- hat				2		0		0		0		0
-- vest/bra             	0        	2         	0		0		0
-- undies/panties		0		0		2		0		0
-- teddy				0		4		4		0		0
-- blouse/shirt/T-shirt	0		8		0		0		0
-- dress/coveralls		0		8		32		0		0
-- skirt				0		0		32		0		0
-- trousers/shorts		0		0		16		0		0
-- sweater/pullover		0		16		0		0		0
-- jacket				0		32		0		0		0
-- coat				0		64		64		0		0
-- socks/stockings		0		0		0		2		0
-- tights/pantiehose		0		0		8		2		0
-- shoes/boots			0		0		0		4		0			
-- gloves				0		0		0		0		2




-- =============================================================


----- DEVICE


-- =============================================================


-- (This class is not cross-referenced elsewhere in this or any other library file.)


EVERY device ISA OBJECT
	IS NOT 'on'.
	

	VERB examine
		DOES 
			IF THIS IS NOT plural
				THEN "It is" 
				ELSE "They are"
			END IF.
			
			IF THIS IS 'on'
				THEN "currently on."
				ELSE "currently off."
			END IF.
	END VERB.


	VERB turn_on
		CHECK THIS IS NOT 'on'
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_device_not_on_sg OF my_game. 
					ELSE SAY check_device_not_on_pl OF my_game.
				END IF.
		AND CURRENT LOCATION IS lit
			ELSE SAY check_current_loc_lit OF my_game.
		AND THIS IS reachable
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_obj_reachable_sg OF my_game.
					ELSE SAY check_obj_reachable_pl OF my_game.
				END IF.
		AND THIS IS NOT broken
			ELSE SAY check_obj_not_broken OF my_game.
		DOES ONLY
			"You turn on" SAY THE THIS. "."
			MAKE THIS 'on'.
	END VERB.


	VERB turn_off
		CHECK THIS IS 'on'
			ELSE 
				 IF THIS IS NOT plural
					THEN SAY check_device_on_sg OF my_game.
					ELSE SAY check_device_on_pl OF my_game.
				 END IF.
		AND CURRENT LOCATION IS lit
			ELSE SAY check_current_loc_lit OF my_game.
		AND THIS IS reachable
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_obj_reachable_sg OF my_game.
					ELSE SAY check_obj_reachable_pl OF my_game.
				END IF.
		DOES ONLY 
			"You turn off" SAY THE THIS. "."
			MAKE THIS NOT 'on'.
	END VERB.


-- The following verb switches a device off if the device is on, and vice versa.

	
	VERB switch
		CHECK CURRENT LOCATION IS lit
			ELSE SAY check_current_loc_lit OF my_game.
		AND THIS IS reachable
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_obj_reachable_sg OF my_game.
					ELSE SAY check_obj_reachable_pl OF my_game.
				END IF.
		AND THIS IS NOT broken
			ELSE SAY check_obj_not_broken OF my_game.
		DOES ONLY
			IF THIS IS 'on'
				THEN "You switch off" SAY THE THIS. "."
					MAKE THIS NOT 'on'.
				ELSE "You switch on" SAY THE THIS. "."
					MAKE THIS 'on'.
			END IF.
	END VERB.

END EVERY.



-- =============================================================


----- DOOR


-- =============================================================


-- (This class is not cross-referenced elsewhere in this or any other library file.)


EVERY door ISA OBJECT
	IS closeable.
	IS closed.
	IS lockable.
	IS NOT locked.
	IS NOT takeable.

	HAS matching_key null_key.

	-- If a door is lockable/locked, you should state at the door instance
	-- which object will unlock it, with the matching_key attribute. 
		-- e.g.

		-- THE attic_door ISA DOOR
			-- HAS matching_key brass_key.
			-- ...
		--   END THE.

	-- null_key is a default dummy object which can be ignored.

	VERB examine
		DOES 
			IF THIS IS NOT plural
				THEN "It is" 
				ELSE "They are"
			END IF.
		
			IF THIS IS closed
				THEN "currently closed."
				ELSE "currently open."
			END IF.
	END VERB.

		

	VERB knock
		DOES ONLY
			IF THIS IS closed
				THEN "You knock on" SAY THE THIS. "$$. There is no reply."
				ELSE "You don't find it purposeful to knock on the open door"
					IF THIS IS NOT plural
						THEN "."
						ELSE "$$s."
					END IF.
					
			END IF.
	END VERB.



	VERB look_behind
		DOES ONLY 
			IF THIS IS closed
				THEN "You cannot look behind"
					IF THIS IS NOT plural
						THEN "the door - it is closed."
						ELSE "the doors - they are closed."
					END IF.
				ELSE "You notice nothing special behind the door"
					IF THIS IS NOT plural
						THEN "."
						ELSE "$$s."
					END IF.
			END IF.
	END VERB.



	VERB look_under
		DOES ONLY
			IF THIS IS closed
				THEN "The gap under the closed door"
					IF THIS IS plural 
						THEN "$$s"
					END IF.
					"is so narrow that you can't
					see anything of what lies on the other side."
				ELSE "You notice nothing special under the door"
					IF THIS IS plural
						THEN "$$s."
						ELSE "."
					END IF.
			END IF.
	END VERB.


END EVERY.


THE null_key ISA OBJECT
END THE.


-- =============================================================


----- LIGHTSOURCE


-- =============================================================


-- (In the file 'verbs.i', ISA LIGHTSOURCE is used in the syntax definition of the verb 'light'.)


EVERY lightsource ISA OBJECT
	IS NOT lit.
	IS natural. 	-- A natural lightsource is for example a candle, a match or a torch. 
				-- A NOT natural lightsource is for example a flashlight or a lamp.
				-- You cannot switch on or off a natural lightsource.


	VERB examine
		DOES 
			IF THIS IS lit
				THEN 
					IF THIS IS natural
						THEN 
							IF THIS IS NOT plural
								THEN "It is"
								ELSE "They are"
							END IF.
							"currently lit."
						ELSE 
							IF THIS IS NOT plural
								THEN "It is"
								ELSE "They are"
							END IF.
							"currently on."
					END IF.
				ELSE
					IF THIS IS natural
						THEN 
							IF THIS IS NOT plural
								THEN "It is"
								ELSE "They are"
							END IF.
							"currently not lit."
						ELSE 
							IF THIS IS NOT plural
								THEN "It is"
								ELSE "They are"
							END IF.
							"currently off."
					END IF.
			END IF.
	END VERB.

	
	VERB light
		CHECK THIS IS NOT lit
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_lightsource_not_lit_sg OF my_game.
					ELSE SAY check_lightsource_not_lit_pl OF my_game.
				END IF.
		AND THIS IS NOT broken
			ELSE SAY check_obj_not_broken OF my_game.
		DOES ONLY
			IF THIS IS natural
				THEN "You light" SAY THE THIS. "." 
					MAKE THIS lit.
				ELSE "You turn on" SAY THE THIS. "."
					MAKE THIS lit.
			END IF.
	END VERB.


	VERB extinguish
		CHECK THIS IS lit
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_lightsource_lit_sg OF my_game.
					ELSE SAY check_lightsource_lit_pl OF my_game.
				END IF.
		DOES ONLY "You extinguish" SAY THE THIS. "."
			MAKE THIS NOT lit.
	END VERB.


	VERB turn_on
		CHECK THIS IS NOT natural
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_obj_suitable_on_sg OF my_game.
					ELSE SAY check_obj_suitable_on_pl OF my_game.
				END IF.
		AND THIS IS NOT lit
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_lightsource_not_lit_sg OF my_game.
					ELSE SAY check_lightsource_not_lit_pl OF my_game.
				END IF.
		AND THIS IS NOT broken
			ELSE SAY check_obj_not_broken OF my_game.
		DOES ONLY
			"You turn on" SAY THE THIS. "."
			MAKE THIS lit.
					
	END VERB.


	VERB turn_off
		CHECK THIS IS NOT natural
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_obj_suitable_off_sg OF my_game.
					ELSE SAY check_obj_suitable_off_pl OF my_game.
				END IF.
		AND THIS IS lit
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_lightsource_lit_sg OF my_game.
					ELSE SAY check_lightsource_lit_sg OF my_game.
				END IF.
				
		DOES ONLY 
			"You turn off" SAY THE THIS. "."
			MAKE THIS NOT lit.	
			  
	END VERB.


-- The following verb switches a NOT natural lightsource on if it is off, and vice versa
-- (when the player forgets, or doesn't bother, to type 'on' or 'off' after 'switch').


	VERB switch
		CHECK THIS IS NOT natural
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_lightsource_switchable_sg OF my_game.
					ELSE SAY check_lightsource_switchable_pl OF my_game.
				END IF.
		AND THIS IS reachable
			ELSE 
				IF THIS IS NOT plural
					THEN SAY check_obj_reachable_sg OF my_game.
					ELSE SAY check_obj_reachable_pl OF my_game.
				END IF.
		AND THIS IS NOT distant
			ELSE
				IF THIS IS NOT plural
					THEN SAY check_obj_not_distant_sg OF my_game. 
					ELSE SAY check_obj_not_distant_pl OF my_game. 
				END IF.
		AND THIS IS NOT broken
			ELSE SAY check_obj_not_broken OF my_game.	
		DOES ONLY
			IF THIS IS lit
				THEN "You switch off" SAY THE THIS. "."
					MAKE THIS NOT lit.
				ELSE "You switch on" SAY THE THIS. "."
					MAKE THIS lit.
			END IF.
	END VERB.
	

END EVERY.



-- ==============================================================


----- LIQUID


-- ==============================================================


-- (In the file 'verbs.i', ISA LIQUID is used in the syntax definitions of the verbs 'drink' and 'sip'.)


EVERY liquid ISA OBJECT		
	
	CONTAINER
		HEADER "In" SAY THE THIS. "you see"
		ELSE "There is nothing in" SAY THE THIS. "."	

		-- We declare this class a container to enable player commands such as
		-- 'throw sack into water', 'look into water' and 'take pearl from water'.
		-- Also cases such as 'pour red potion into blue potion' require that this 
		-- class behaves like a container. 
	

	HAS vessel null_vessel.	

		-- The 'vessel' attribute takes care that if a liquid is
		-- in a container, the verb 'take' will automatically take the 
		-- container instead (if the container is takeable). Trying 				
		-- take a liquid that is in a fixed-in-place container, as well
		-- as trying to take a liquid outside any container, will yield 			
		-- "You can't carry [the liquid] around in your bare hands." 
		-- The default value 'null_vessel' tells the compiler that the liquid
		-- is not in any container. null_vessel is a dummy default that can be
		-- ignored.
							


	-- If you have some liquid in a container in your game, you should declare the 
	-- liquid instance thus:
	
 	-- THE juice ISA LIQUID
	--      IN bottle
	-- 	  HAS vessel bottle.	-- i.e. the value of the 'vessel' attribute is the 
	-- 	  ...				-- container the liquid is in
	-- END THE juice.

	-- Then, taking and pouring liquids work smoothly.
	-- If you don't declare the 'vessel' attribute for the liquid instance, taking a liquid will 
	-- yield "You can't carry [the liquid] around in your bare hands."; only taking its 
	-- container will work in that case.

	-- The verb 'pour', as defined in this library, also works for the container of a liquid;
	-- i.e. if there is some juice in a bottle, 'pour bottle' and 'pour juice' work equally well.
	-- Note, however, that the verb 'empty' is not a synonym for 'pour';
	-- 'empty' only works for container objects.


	INITIALIZE
		SCHEDULE check_vessel AT THIS AFTER 0.		-- this event is defined further below


	VERB examine
		DOES ONLY
			IF vessel OF THIS <> null_vessel
				THEN 
					IF vessel OF THIS IS NOT closed
						THEN "You notice nothing unusual about" SAY THE THIS.
						ELSE "You can't, since" SAY THE vessel OF THIS. 
								IF THIS IS NOT plural
									THEN "is"
									ELSE "are"
								END IF.
								"closed."
								-- Here we prohibit the player from examining
								-- a liquid when the liquid is in a closed container.
					END IF.
				ELSE "You notice nothing unusual about" SAY THE THIS. "."
			END IF.
	END VERB.
		

	VERB look_in
		DOES ONLY
			IF vessel OF THIS <> null_vessel
				THEN 
					IF vessel OF THIS IS NOT closed
						THEN "You see nothing special in" SAY THE THIS. "."
						ELSE "You can't, since" SAY THE vessel OF THIS.
								IF THIS IS NOT plural
									THEN "is"
									ELSE "are"
								END IF.
								"closed."
								-- Here we prohibit the player from looking into
								-- a liquid when the liquid is in a closed container.
					END IF.
				ELSE "You see nothing special in" SAY THE THIS. "."
			END IF.
	END VERB.
		

	VERB take
		DOES ONLY
			IF vessel OF THIS = null_vessel OR vessel OF THIS IS NOT takeable
				THEN "You can't carry" SAY THE THIS. "around in your bare hands."
			ELSE LOCATE vessel OF THIS IN hero.
				"($$" SAY THE vessel OF THIS. "$$)$nTaken." 
			END IF.
	END VERB.


	VERB take_from
	   WHEN obj
		DOES ONLY
			IF vessel OF THIS = null_vessel OR vessel OF THIS IS NOT takeable
				THEN "You can't carry" SAY THE THIS. "around in your bare hands."
			ELSE LOCATE vessel OF THIS IN hero.
				"($$" SAY THE vessel OF THIS. "$$)$nTaken." 
			END IF.
	END VERB.	


	VERB drop
		DOES ONLY
			"($$" SAY THE vessel OF THIS. "$$)$nDropped."
			LOCATE vessel OF THIS AT hero.
	END VERB.
				
			
	VERB give
		WHEN obj
		DOES ONLY
			-- implicit taking:
			IF THIS NOT IN hero
				THEN 
					IF vessel OF THIS = null_vessel OR vessel OF THIS IS NOT takeable
						THEN "You can't carry" SAY THE THIS. "around in your bare hands."
					ELSE LOCATE vessel OF THIS IN hero.
						"(taking" SAY THE vessel OF THIS. "first)$n"
					END IF.
			END IF.
			-- end of implicit taking.

			IF THIS IN hero   
				-- i.e. if the implicit taking was successful
				THEN
					"You give" SAY THE vessel OF THIS. "to" SAY THE recip. "."
					LOCATE vessel OF THIS IN recip.
			END IF.
		
			-- there is no 'ELSE' statement in this last IF -clause, as the 'IF THIS NOT 
			-- IN hero' clause above it takes care of the 'ELSE' alternative.

	END VERB.


	VERB pour
		DOES ONLY
			-- implicit taking:
			IF THIS NOT IN hero
				THEN 
					IF vessel OF THIS = null_vessel OR vessel OF THIS IS NOT takeable
						THEN "You can't pour" SAY THE THIS. "anywhere since you are not
							carrying" 
								IF THIS IS NOT plural
									THEN "it."
									ELSE "them."
								END IF.
					ELSE LOCATE vessel OF THIS IN hero.
						"(taking" SAY THE vessel OF THIS. "first)$n"
					END IF.
			END IF.
			-- end of implicit taking.
			
			IF THIS IN hero
				THEN
					"You pour" SAY THE THIS.
						IF floor HERE
							THEN "on the floor."
							ELSE "on the ground."
						END IF.
					LOCATE THIS AT hero.
					SET vessel OF THIS TO null_vessel.
			END IF.

	END VERB.


	VERB pour_in
		WHEN obj
			DOES ONLY
				-- implicit taking:
				IF THIS NOT IN hero
					THEN 
						IF vessel OF THIS = null_vessel
							THEN "You can't carry" SAY THE THIS. "around in your bare hands."
						ELSIF vessel OF THIS IS NOT takeable
							THEN "You don't have" SAY THE vessel OF THIS. "."
						ELSE LOCATE vessel OF THIS IN hero.
							"(taking" SAY THE vessel OF THIS. "first)$n"
						END IF.
				END IF.
				-- end of implicit taking.

				IF THIS IN hero		--i.e. if the implicit taking was successful
					THEN
						"You pour" SAY THE THIS. "into" SAY THE cont. "."
						LOCATE THIS IN cont.
						SET vessel OF THIS TO cont.
				END IF.
		WHEN cont
			DOES ONLY
				IF vessel OF THIS = null_vessel
					THEN 
						"There's not much sense pouring" SAY THE obj. "into" SAY THE THIS. "."
					ELSE 
						IF vessel OF THIS IS NOT closed
							THEN "It wouldn't accomplish anything trying to pour" SAY THE obj. 
								"into" SAY THE THIS. "."
							ELSE "You can't, since" SAY THE vessel OF THIS. 
								IF THIS IS NOT plural
									THEN "is"
									ELSE "are"
								END IF.
								"closed."
						END IF.
				END IF.
	END VERB.


	VERB pour_on
		WHEN obj
			DOES ONLY
				-- implicit taking:
				IF THIS NOT IN hero
					THEN 
						IF vessel OF THIS = null_vessel 
							THEN "You can't carry" SAY THE THIS. "around in your bare hands."
						ELSIF vessel OF THIS IS NOT takeable
							THEN "You don't have" SAY THE vessel OF THIS. "." 
						ELSE LOCATE vessel OF THIS IN hero.
							"(taking" SAY THE vessel OF THIS. "first)$n"
						END IF.
				END IF.
				-- end of implicit taking.
				
				IF THIS IN hero		
					-- i.e. if the implicit taking was successful
					THEN
						IF surface = floor OR surface = ground
							THEN LOCATE THIS AT hero.
						 		"You pour" SAY THE THIS. "on" SAY THE surface. "."
								SET vessel OF THIS TO null_vessel.
						ELSIF surface ISA SUPPORTER
							THEN LOCATE THIS IN surface.
								"You pour" SAY THE THIS. "on" SAY THE surface. "."
				  				SET vessel OF THIS TO null_vessel.
						ELSE "It wouldn't be sensible to pour anything on" SAY THE surface.
						END IF.
				END IF.
	END VERB.		


	VERB fill_with
		-- when something is filled with a liquid, this something becomes the
		-- vessel of the liquid
		WHEN substance					
			 DOES SET vessel OF THIS TO cont.   	  
	END VERB.


	VERB put_in
		WHEN obj
			DOES ONLY 
				IF vessel OF THIS = null_vessel
					THEN "You can't carry" SAY THE THIS. "around in your bare hands."
					ELSE 
						IF vessel OF THIS IS takeable
							THEN
								-- implicit taking:
								IF THIS NOT IN hero
									THEN 
										IF vessel OF THIS = null_vessel 
											THEN "You can't carry" SAY THE THIS. "around in your bare hands."
										ELSE LOCATE vessel OF THIS IN hero.
											"(taking" SAY THE vessel OF THIS. "first)$n"
										END IF.
								END IF.
								-- end of implicit taking.

								LOCATE vessel OF THIS IN cont.
						      	"You put" SAY THE vessel OF THIS. "into" SAY THE cont. "."

							ELSE "You don't have" SAY THE vessel OF THIS. "."
						END IF.
				END IF.
	      WHEN cont
			DOES ONLY
			IF vessel OF THIS = null_vessel
				THEN 
					"There's not much sense putting" SAY THE obj. "into" SAY THE THIS. "."
				ELSE 
					IF vessel OF THIS IS NOT closed
						THEN 
							IF obj = vessel OF THIS
								THEN "That doesn't make sense."
								ELSE "It wouldn't accomplish anything trying to put" SAY THE obj. 
									"into" SAY THE vessel OF THIS. "."
							END IF.
						ELSE "You can't, since" SAY THE vessel OF THIS. 
								IF THIS IS NOT plural
									THEN "is"
									ELSE "are"
								END IF.
							"closed."
					END IF.
			END IF.
	END VERB.


	VERB put_on
		WHEN obj
			DOES ONLY
				-- implicit taking:
				IF THIS NOT IN hero
					THEN 
						IF vessel OF THIS = null_vessel 
							THEN "You can't carry" SAY THE THIS. "around in your bare hands."
						ELSIF vessel OF THIS IS NOT takeable
							THEN "You don't have" SAY THE vessel OF THIS. "." 
						ELSE LOCATE vessel OF THIS IN hero.
							"(taking" SAY THE vessel OF THIS. "first)$n"
						END IF.
				END IF.
				-- end of implicit taking.

				IF THIS IN hero 				
					-- i.e. if the implicit taking was successful
					THEN
				   IF vessel OF THIS = null_vessel
					THEN "You can't carry" SAY THE THIS. "around in your bare hands."
					ELSE 
						IF vessel OF THIS IS takeable
							THEN "You put" SAY THE vessel OF THIS. "onto" SAY THE surface. "."
								LOCATE vessel OF THIS IN surface.
							ELSE "You can't carry" SAY THE THIS. "around in your bare hands."
						END IF.
				   END IF.
				END IF.
		WHEN surface
			DOES ONLY "It is not possible to $v" SAY obj. "onto" SAY THE THIS. "."
	END VERB.


	-- throwing liquids, whether in containers or not, is disabled:


	VERB throw
		DOES ONLY
			"Throwing" SAY THE THIS. "around wouldn't be sensible."
	END VERB.


	VERB throw_at
	   WHEN projectile
		DOES ONLY
			"Throwing" 
				IF vessel OF THIS = null_vessel
					THEN SAY THE THIS.
					ELSE SAY THE vessel OF THIS. 
				END IF.	
				"around wouldn't be sensible."
	    WHEN target
		DOES ONLY
			IF projectile = vessel OF THIS
				THEN "That doesn't make sense."
				ELSE "It wouldn't accomplish anything trying to throw" SAY THE projectile. "at" 
					IF vessel OF THIS = null_vessel
						THEN SAY THE THIS.
						ELSE SAY THE vessel OF THIS. 
					END IF.
					"."
			END IF.
	END VERB.


	
	VERB throw_to
	   WHEN projectile
		DOES ONLY
			"Throwing"
				IF vessel OF THIS = null_vessel
					THEN SAY THE THIS.
					ELSE SAY THE vessel OF THIS. 
				END IF.	
				"to" SAY THE recipient. "wouldn't be sensible."
	   WHEN recipient
		DOES ONLY
			IF projectile = vessel OF THIS
				THEN "That doesn't make sense."
				ELSE
					"Throwing something to" 
						IF vessel OF THIS = null_vessel
							THEN SAY THE THIS.
							ELSE SAY THE vessel OF THIS. 
						END IF.
					"wouldn't accomplish anything."
			END IF.
	END VERB.


	VERB throw_in
	   WHEN projectile
		DOES ONLY
		     "It wouldn't be sensible throwing" SAY THE THIS. "into" SAY THE cont. "."
	   WHEN cont
		DOES ONLY
			IF projectile = vessel OF THIS
				THEN "That doesn't make sense."
				ELSE "It wouldn't be sensible trying to throw" SAY THE projectile. "in" SAY THE THIS. "."
			END IF.
	END VERB.



	-- The verbs 'empty', 'empty_in' and 'empty_on' will be disabled as ungrammatical with liquids:	

	VERB 'empty'
		DOES ONLY "You can only empty containers."
	END VERB.

	VERB empty_in
		WHEN obj
		DOES ONLY "You can only empty containers."
	END VERB.

	VERB empty_on
		WHEN obj
		DOES ONLY "You can only empty containers."
	END VERB.
	

END EVERY.



-- Here is the default vessel for liquids; if the vessel of a liquid is
-- 'null_vessel', it means that the liquid is not in any container.


THE null_vessel ISA OBJECT
	CONTAINER 
END THE.



-- This event checks that if a liquid is outside a container, its container will
-- be 'null_vessel':


EVENT check_vessel
	FOR EACH liq ISA LIQUID, DIRECTLY AT CURRENT LOCATION DO	
			SET vessel OF liq TO null_vessel.
	END FOR.
	SCHEDULE check_vessel AFTER 1.
END EVENT.




-- =============================================================


----- LISTED_CONTAINER


-- =============================================================


-- (This class is not cross-referenced elsewhere in this or any other library file.)


EVERY LISTED_CONTAINER ISA OBJECT			
	CONTAINER						 	

		--  (ACTORS are separately  defined containers further below.)

	VERB examine 
		DOES 
			IF THIS IS closeable
				THEN 
					IF THIS IS closed
						THEN 
							IF THIS IS NOT plural
								THEN "It is"
								ELSE "They are"
							END IF.
							"closed."
						ELSE
							IF THIS IS NOT plural
								THEN "It is"
								ELSE "They are"
							END IF.
							"open."
					END IF.
			END IF.

			IF THIS IS NOT OPAQUE
				THEN LIST THIS.
				ELSE "You can't see inside" SAY THE THIS. "."
			END IF.
	END VERB.


	VERB look_in
		DOES ONLY
			IF THIS IS NOT OPAQUE
				THEN LIST THIS.
				ELSE "You can't, since" SAY THE THIS. 
						IF THIS IS NOT plural
							THEN "is"
							ELSE "are"
						END IF.
					"closed."
			END IF.
	END VERB.


-- Note that closed listed_containers are by default opaque and they become not opaque when
-- they are opened: 


	VERB open
 		DOES
			MAKE THIS NOT OPAQUE.
 			LIST THIS.
 	END VERB.


 	VERB close
 		DOES
 			MAKE THIS OPAQUE.
 	END VERB.


END EVERY.






-- Tips for using container objects:


-- 1)


-- The contents of a normal CONTAINER are not listed when examined
-- (you'll need to look *into* the object to see what it contains). 
-- If you wish to have the contents listed after 'examine' anyway, 
-- you should
--
-- instead of:
--
-- THE box ISA OBJECT
--   CONTAINER
-- ...
-- END THE.
--
-- do this:
--
-- THE box ISA LISTED_CONTAINER
-- ...
-- END THE.
--
-- Then, 'examine' lists the contents of the container.
--
-- The contents of a listed_container are also listed when the container 
-- is opened. This doesn't happen with normal containers.


-- 2)


-- For the command 'inventory' to list the contents of a container 
-- the hero is carrying, go to 'verbs.i', find the command 'inventory'
-- and list one by one all relevant containers that the hero might carry,
-- thus:
--
-- VERB inventory
-- 	DOES LIST hero.
--    ...
--		IF bag IN hero			
--			THEN LIST bag.
--		END IF.
--		
--		IF box IN hero
--			THEN LIST box.
--		END IF.
--    ...
-- END VERB.
--
--
-- If you leave out these additions, the bag and the box will be listed after the 
-- command 'inventory' in the following way:
-- 
-- "You are carrying a bag and a box."
-- 
-- only. But with the above additions, the outcome is e.g.
--
-- "You are carrying a bag and a box. The bag contains a loaf of bread. The box 
-- is empty."


-- 3)


-- To declare a container the contents of which should not be listed after 'look' 
-- or 'examine', declare it an 'opaque container' in the following way:


-- THE box ISA OBJECT
--   OPAQUE CONTAINER
-- ...
-- END THE.


-- or


-- THE box ISA LISTED_CONTAINER
--   OPAQUE CONTAINER
-- ...
-- END THE.


-- Objects in an opaque container cannot be seen or manipulated.
-- To change this, declare e.g.
--
-- MAKE box NOT OPAQUE.
--




-- ===============================================================


----- SOUND


-- ===============================================================


-- (This class is not cross-referenced in this or any other library file.)


EVERY sound ISA OBJECT
	IS NOT examinable.
	IS NOT takeable.
	IS NOT reachable.
	IS NOT movable.

	VERB smell
		DOES ONLY
			 IF THIS IS NOT plural
				THEN "That's not"
				ELSE "Those are not"
			 END IF.
			"something you can smell."
	END VERB.


END EVERY.



-- ==============================================================


----- SUPPORTER


-- ==============================================================


-- (See the file 'verbs.i', verbs 'climb_on', 'empty_on', 'get_off', 'jump_on',
-- 'lie_on', 'pour_on', 'put_in', 'put_on', 'sit_on', 'stand_on', and 'take_from'
-- where SUPPORTER is used in either syntax definitions, verb checks
-- or verb definitions.)
 

EVERY supporter ISA OBJECT
	CONTAINER
		HEADER "On" SAY THE THIS. "you see"
		ELSE "There's nothing on" SAY THE THIS. "."		
	

	VERB examine
		DOES 
			LIST THIS.
	END VERB.


	-- in the following, we disable some verbs that are defined to work with normal containers:


	VERB look_in							
		DOES ONLY 
			IF THIS IS NOT plural
				THEN "That's not"
				ELSE "Those are not"
			END IF.
			"something you can look into."
	END VERB.


	VERB empty_in, pour_in
	   WHEN cont
		DOES ONLY
			 IF THIS IS NOT plural
				THEN "That's not"
				ELSE "Those are not"
			END IF.
			"something you can pour things into."
	END VERB.


	VERB put_in
   	  WHEN cont
		DOES ONLY "You can't put anything inside" SAY THE THIS. "."
	END VERB.


	VERB throw_in
   	  WHEN cont
		DOES ONLY "You can't put anything inside" SAY THE THIS. "."
	END VERB.


END EVERY.



-- To place objects on a supporter, define them in the following way:
--
-- Define the supporter first; e.g.
--
-- THE tray ISA SUPPORTER
-- ...
-- END THE.
--
--
-- Then, the objects on the supporter:
--
--
-- THE apple ISA OBJECT
--	IN tray				
-- 	...					
-- END THE.
--
-- Note the IN above, even if the apple will be described as being *on* the tray.
--
--
-- THE book ISA OBJECT
--   IN table
--    ... 
-- END THE.
--
--
-- Note that the 'examine' command will list what is on the surface of a supporter, not what,
-- if anything, is inside the supporter. For example, if you have a supporter called 'table' 
-- in your game with two drawers in it,
--
-- DON'T do this:
--
-- THE drawer1 ISA OBJECT
--     NAME bottom drawer
--	 CONTAINER
-- 	 IN table.
-- END THE.
--
-- or this:
--
-- THE drawer2 ISA LISTED_CONTAINER
--     NAME top drawer
--     IN table.			
-- END THE.    
--
-- This would result in something like 
--
-- "There's a table here. On the table you see a book, a bottom drawer and a top drawer."
--
--
-- Instead, do the following:
--
-- THE table ISA SUPPORTER 
--     AT bedroom
--     HAS components {drawer1, drawer2}.
-- 	 ...
--     VERB examine
--		DOES 
--			FOR EACH c IN components OF THIS DO
--				SAY "The table has" SAY AN c. "." 
--					IF c IS NOT closed
--						THEN LIST c.
--						ELSE SAY THE c. "is closed."
--					END IF.
--			END FOR.
--	 END VERB.
--     ...
-- END THE.
--
-- THE drawer1 ISA LISTED_CONTAINER 
--    OPAQUE CONTAINER
--    NAME bottom drawer
--    AT bedroom 
-- 	IS closed.
-- END THE.
--
-- THE drawer2 ISA LISTED_CONTAINER
--	NAME top drawer
--	AT bedroom
--	IS NOT closed.
-- END THE.
--
-- THE book ISA OBJECT IN table
-- ...
-- END THE book.
--
-- THE diary ISA OBJECT IN drawer2
-- ...
-- END THE diary.


-- In other words, declare the drawers components of the table, in the manner described above.
-- The result will then be e.g. something like this:
--
-- "You see a table here. There is a book on the table. The table has a bottom drawer. The bottom drawer
-- is closed. The table has a top drawer. The top drawer contains a diary."




-- ==============================================================


----- WEAPON


-- ==============================================================


-- (See the file 'verbs.i', verbs 'attack_with', 'fire',
-- 'fire_with', 'kill_with', 'shoot' and 'shoot_with' where WEAPON is used
-- either in the syntax definitions or verb checks.)


EVERY weapon ISA OBJECT
	IS NOT fireable.
END EVERY.




-- ==============================================================


----- WINDOW


-- ==============================================================


-- (This class is not cross-referenced elsewhere in this or any other library file.)


-- You can look out of and through a window. 
-- When examined, a window is automatically described as being either open or closed.


EVERY window ISA OBJECT
	IS closeable.
	IS closed.
	IS NOT takeable.


	VERB examine
		DOES 
			IF THIS IS closed
				THEN 
					IF THIS IS NOT plural
						THEN "It is"
						ELSE "They are"
					END IF.
					"currently closed."
				ELSE 
					IF THIS IS NOT plural
						THEN "It is"
						ELSE "They are"
					END IF.
					"currently open."
			END IF.
	END VERB.


	VERB look_behind
		DOES ONLY 
			"That's not possible."
	END VERB.


	VERB look_out_of
		DOES ONLY "You see nothing special looking out of the"
				IF THIS IS NOT plural
					THEN "window."
					ELSE "windows."
				END IF.
	END VERB.


	VERB look_through
		DOES ONLY "You see nothing special looking through the"
				IF THIS IS NOT plural
					THEN "window."
					ELSE "windows."
				END IF.
	END VERB.


END EVERY.



-- ===============================================================

-- ===============================================================
--
-- 2. Actors
--
-- ===============================================================

-- ===============================================================


-- First, we declare some common characteristics for all actors:


ADD TO EVERY ACTOR
   	IS NOT inanimate.
   	IS NOT following. 
   	IS NOT sitting.
   	IS NOT lying_down.	
   	IS NOT named.	
	-- = the actor's name is not known to the player; see also 
	-- the example below
   
   -- Important: if you don't need an article in front of an actor name (e.g. 'Jim', 
   -- as opposed to e.g. 'a/the man'), declare the instance as 'named':

   -- THE jim ISA ACTOR
   --	    IS named.
   --   ...
   -- END THE.	

   -- (Remember also that all actors 'CAN NOT talk' by default. If you want the actor to be able 
   -- to talk, give it the attribute 'CAN talk' or declare it ISA PERSON (see further below))
	
   	DEFINITE ARTICLE 
		IF THIS IS NOT named
			THEN "the"		
			ELSE ""
		END IF.

   	INDEFINITE ARTICLE 
		IF THIS IS NOT named
			THEN 								
				IF THIS IS NOT plural
					THEN "a"				
					ELSE ""
				END IF.		
			ELSE ""
		END IF.
	
	-- if you need "an", you must declare it separately at the actor instance
	
   	CONTAINER							
		-- so that actors can receive and carry objects

  	INITIALIZE								
		-- all actors will obey this script from the start of the game
	
		IF THIS <> hero
			THEN USE SCRIPT following_hero FOR THIS.
		END IF.
	
		SCRIPT following_hero						
			-- this code will make any actor follow the hero
			-- if the actor is given the attribute 'following'.
			STEP WAIT UNTIL hero NOT HERE			
				 
				IF THIS IS following
					THEN
						LOCATE THIS AT hero.
						"$p" SAY THE THIS. 
							IF THIS IS NOT plural
								THEN "follows you."
								ELSE "follow you."
							END IF.				
				END IF.
				
				USE SCRIPT following_hero FOR THIS.


	DESCRIPTION
		IF THIS IS NOT named
			THEN 
				IF THIS IS NOT plural
					THEN "There is" SAY AN THIS. "here."		
					ELSE "There are" SAY THIS. "here."	
				END IF.	
			ELSE SAY THIS. 
				IF THIS IS NOT plural
					THEN "is here."
					ELSE "are here."
				END IF.
		END IF.			

   

	VERB examine
		DOES 
			IF COUNT ISA THING, IN THIS > 0  		
				THEN LIST THIS. 			
			END IF.							
										
											
   	END VERB.									
   
	-- The above if-statement will make a description of an
	-- actor's possessions show up every time the actor is examined.
	-- Note that this doesn't apply to the hero which is defined
	-- separately further below.-- This listing will be overridden if you define an
	-- individual response to the actor instance being examined using DOES ONLY. 
   	-- If your still want to list the possessions of the actor after your own 'examine'
	-- response, you should state 'LIST [actor].' in the 'examine' verb there.

END ADD TO.


-- In order that clothing worn by an NPC is described after 'look' and
-- 'examine' we need the following code. See the instructions right after it.

			
	
EVERY npc_worn ISA THING 
	HAS carrier null_carrier. 
		-- The value of the 'carrier' attribute is the actor wearing the clothing.
		-- Here, 'null_carrier' is a dummy default instance that can be ignored.

	
	CONTAINER TAKING CLOTHING.
		HEADER SAY THE carrier OF THIS. "is wearing"
		ELSE ""

	DESCRIPTION ""		
		-- we don't want this container to appear in room descriptions
	
	INITIALIZE					
		LOCATE THIS AT carrier OF THIS.
	    	SCHEDULE check_npc_worn AT THIS AFTER 0.
			
END EVERY.	



EVENT check_npc_worn
	FOR EACH npcw ISA npc_worn DO
		IF npcw NOT AT carrier OF npcw
			THEN LOCATE npcw AT carrier OF npcw.
		END IF.
	END FOR.
	SCHEDULE check_npc_worn AFTER 1.
END EVENT.


THE null_carrier ISA ACTOR		
	-- a dummy default instance, ignore.
END THE.



-- To describe what an NPC is wearing (after the commands 'look and 'examine'), define the NPC e.g. like this:


-- THE mr_smith ISA ACTOR
--	DESCRIPTION	
--		"blah blah" LIST mr_smith_worn.  
			-- Leave this LIST statement out if you don't want to
			-- have the actor's clothing listed after 'look'.	

--    VERB examine
--		DOES ONLY "blah blah"
--				(LIST mr_smith.)  	
				-- This lists what Mr Smith is carrying.			
--				LIST mr_smith_worn.	
				-- this lists what Mr Smith is wearing. 
--    END VERB.						  
--
-- END THE.


-- THE mr_smith_worn ISA NPC_WORN	
			-- All containers for clothing worn by NPCs should be declared ISA NPC_WORN.
-- 	HAS carrier mr_smith.		
			-- The value of the 'carrier' attribute is the actor wearing the clothes.
-- END THE.


-- THE bowler_hat ISA CLOTHING IN mr_smith_worn
	-- don't declare the clothing attributes (e.g. "IS headcover 2.") for NPCs - NPCs cannot 
     -- wear clothing in layers! 
-- END THE. 


-- Note that if you don't want the player character to be able to take a piece of clothing 
-- worn by another character, you should declare the piece of clothing NOT takeable! 
				

------------------------------------------


-- To make an actor follow the hero, give it the 'following' attribute, e.g.:
--
-- THE bob ISA ACTOR
--	...
--   	VERB whatever
--		DOES MAKE bob following.				
--    END VERB.								
--	....								
--										
-- END THE bob.								
--										  
-- etc.								


-- If you wish to have an actor follow the hero right from the start of the game,
-- you can naturally just declare
--
-- THE servant ISA ACTOR
-- 	IS following.
-- ...
-- END THE.
--
--

-- To stop an actor from following the hero, just make the actor NOT following.


--------------------------------------------


-- Tip: if you have in your game an actor that starts off as unnamed (such as 'a man'),
-- and the player learns his name later on (say, 'Jim'), you should define the actor in
-- e.g. the following way to make the player able to refer to him with 
-- both 'man' and 'Jim':

-- THE jim ISA PERSON
--    NAME man NAME jim	
--    PRONOUN him		
--    MENTIONED 
--		IF jim IS NOT named		-- By default, all actors are 'NOT named'. 
--			THEN "man"
--			ELSE "Jim"
--		END IF.
-- ..
--  VERB ask
--    WHEN act
--       IF topic = name
--          THEN """My name is Jim"", he replies."
--			MAKE jim named.
--	   END IF.
--  END VERB.
--
-- END THE.

-- The 'named' attribute is meant to be used in cases like this. The indefinite and definite
-- articles don't have to be declared here, as their behavior has been declared in the 
-- general actor class above.



-- ================================================================


----- PERSON			-- CAN talk


-- ================================================================


EVERY person ISA ACTOR
    CAN talk.
END EVERY.



-- ================================================================


----- FEMALE and MALE		


-- ================================================================


-- MALE and FEMALE are actually subclasses of PERSON, and so they both
-- have the ability to talk.


EVERY female ISA PERSON
	PRONOUN her
END EVERY.


EVERY male ISA PERSON
	PRONOUN him
END EVERY.




