-- ALAN NEW LIBRARY: CLASSES (file name: 'classes.i')


-- This library file defines various object and actor classes, as well as the 
-- instance 'hero'(=the player character). Many of these classes are frequently 
-- used in verb definitions in 'verbs.i' so they should be edited or 
-- removed with caution.


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


-- LISTABLE_CONTAINER
	-- Is a special kind of container, the contents of which will be listed both after 
	-- 'look' (= in the room description), 'look in' and 'examine' (if the container is open). 
	-- (The contents of a normal container object are not listed after 'examine' but only 
	-- after 'look' (=room description) and 'look in').


-- SCENERY  
	-- Behaves like a normal object, can be reached and manipulated but not taken.
	-- The default message for 'take' will be "Unimportant for your purposes, you decide to 
	-- leave the [object] where it is."
	-- The default message for 'examine' will be "The [object] doesn't appear particularly 
	-- interesting."
	-- A scenery object is not automatically listed after the room description when 
      -- you type 'look'. You have to include it manually in the room description.
	-- Compare this with 'background' above (a scenery object can be reached, 
	-- but a background object cannot.)


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
-- Actors are, like objects, usually preceded by an article in-game:
-- e.g. "You see a man here."
--	  "There is nothing special about the dog."
--
-- The following classes for actors are defined in this library:


-- NAMED_ACTOR
	-- has no article in front of the name when mentioned
	-- e.g. "You see Spot here."
	-- is not able to talk; this class is useful e.g. when defining animals

-- PERSON 
	-- has an article in front when mentioned (e.g. "a boy")
	-- is able to talk


-- NAMED_PERSON
	-- has no article in front of the name when mentioned (e.g. "Jim")
	-- is able to talk


-- + instructions for expressing male and female actors with 'PRONOUN him' or 'PRONOUN her'.



-- 3. INSTANCES
-- ============


-- the hero




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


EVERY background ISA THING
	IS NOT reachable.
	DESCRIPTION ""			
END EVERY.


-- Note that a background object in ALAN3 is different from the backdrop in Inform7
-- in that a background object is at one location at a time only, unless
-- you use the nested locations feature in ALAN which makes the object available
-- in several locations. 
-- Here is an example where a ceiling lamp is located in the lobby, the bedroom 
-- and the living-room of a house, but not in other locations:


-- 1) First, define the area where the object(s) should be found: 

-- THE lamp_rooms ISA LOCATION   				-- i.e. the area in which we'll nest	
-- END THE.								-- the three rooms mentioned above

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


-- Now, the lamp is found in all of the above locations.
 
-- Note that in this code you could define exits to other rooms of the house in the
-- normal way, without having to worry about the area at all, e.g.:

-- THE bedroom ISA LOCATION IN lamp_rooms
--	EXIT west TO upstairs_landing.
-- END THE.

-- THE upstairs_landing ISA LOCATION  -- ( = a location outside the defined area)
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


-- To use this class, see the documentation text right after the
-- code below.
-- This class makes use of Alan Bampton's 'xwear.i' extension
-- written originally for ALAN V2, converted here to V3 and 
-- assimilated fully to the present library. Thanks to Alan Bampton
-- for the permission to use the code here.


-----------------------------------------------------------------
-- First, we declare the container for clothing.
-----------------------------------------------------------------


THE worn ISA ENTITY			-- an entity is present everywhere and thus the hero's 		
	CONTAINER TAKING CLOTHING.				-- clothing is always accessible
		HEADER "You are wearing"
		ELSE "You're not wearing anything."
END THE.


-------------------------------------------------------------------
-- Next, we define the syntaxes for verbs needed for putting on 
-- and taking off clothing.
-------------------------------------------------------------------


SYNTAX wear = wear (obj)
		WHERE obj ISA CLOTHING
			ELSE "That's not something you can wear."
	 wear = put 'on' (obj).
	 wear = put (obj) 'on'.
	 wear = don (obj).

SYNTAX remove = remove (obj)
		WHERE obj ISA CLOTHING
			ELSE "That's not something you can remove since you're not wearing it."
	 remove = take 'off' (obj).
	 remove = take (obj) 'off'.
	 remove = doff (obj).

SYNTAX undress = undress.


-------------------------------------------------------------------
-- Now, we define some common attributes for clothing as well as 
-- how the above verbs behave with this class.
-------------------------------------------------------------------


EVERY clothing ISA OBJECT

	IS wearable.
	IS sex 0.
	IS headcover 0.
	IS handscover 0.
	IS feetcover 0.
	IS topcover 0.
	IS botcover 0.

	CONTAINER				-- to allow e.g. a wallet to be put into a jacket


  VERB wear

	CHECK THIS NOT IN worn 
		ELSE "You are already wearing" SAY THE THIS. "." 
	AND THIS IS takeable
		ELSE "You can't pick" SAY THE THIS. "up." 
	AND THIS IS reachable
		ELSE SAY THE THIS. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND sex OF THIS = sex OF hero OR sex OF THIS = 0
		ELSE
			"On second thoughts you decide"
			SAY THE THIS. "really won't suit a"
				IF sex OF hero =1 
					THEN "man"
					ELSE "woman"
				END IF.
				"like you at all."
	

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


--	IF THIS IN tempworn THEN
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


	IF wear_flag OF hero >1 THEN
		IF THIS NOT IN hero 
			THEN "You pick up the" SAY THE THIS. "."
		END IF.
		LOCATE THIS IN hero.
		EMPTY worn IN tempworn.	
		LIST tempworn.
		"Trying to put" 
			SAY THE THIS.
		"on isn't very sensible."
		EMPTY tempworn IN worn.
	ELSIF wear_flag OF hero = 1 
		THEN
			LOCATE THIS IN worn.
			"You pick up the" SAY THE THIS.
				IF THIS IS plural 
					THEN
						"and put them on."
					ELSE
						"and put it on."
				END IF.
	ELSE
		LOCATE THIS IN worn.
		"You put on" SAY THE THIS. "."
	END IF.

END VERB.



VERB remove
	CHECK THIS IN worn
		ELSE 
			"You are not wearing" SAY THE THIS. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."

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


	IF wear_flag OF hero >0 THEN
		LIST worn.
		"Trying to take" SAY THE THIS. "off isn't very sensible."
	ELSE
		LOCATE obj IN hero.
		"You take off" SAY THE THIS. "." 
	END IF.
END VERB.


VERB examine
	DOES AFTER
		IF THIS IS NOT OPAQUE
			THEN 
				IF COUNT ISA OBJECT, IN THIS > 0		-- if the piece of clothing contains
					THEN LIST THIS.				-- something, e.g. if a jacket contains a wallet,
				END IF.						-- the wallet will be mentioned when the
		END IF.								-- jacket is examined
END VERB.


END EVERY.


--------------------------------------------------------------------
-- The verb 'undress' needs to be defined outside the object class
-- as it is intransitive:
--------------------------------------------------------------------


VERB undress
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		IF COUNT IN worn, ISA CLOTHING > 0
			THEN "You don't feel like undressing is a good idea right now."
			ELSE "You're not wearing anything you can remove."  											
		END IF.									
													
	    -- or, to make it work, use the following lines instead:					
	    --IF COUNT IN worn, ISA CLOTHING > 0 
			--THEN EMPTY worn IN hero.
				--"You remove all the items you were wearing."
		    	--ELSE "You're not wearing anything you can remove."
	    -- END IF.
END VERB.



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
-- DOCUMENTATION FOR CLOTHING 
-----------------------------------------------------------------------


-- The quick guide
------------------

-- Here is a quick overview for using the class 'clothing'. A more closely-detailed
-- description of this class is found below this overview.


-- A piece of clothing in your game code should look something similar to these four examples:


-- THE jacket ISA CLOTHING AT lobby
-- 	IS topcover 32.
-- END THE.


-- THE jeans ISA CLOTHING IN wardrobe	
--	IS botcover 16.
-- END THE.


-- THE hat ISA CLOTHING IN worn  		 -- IN worn = worn by the player character
--	IS headcover 2.
-- END THE.


-- THE sweater ISA CLOTHING IN joe_worn   -- = worn by an NPC called Joe.
--    IS topcover 16.				-- Define separate containers like this 
-- END THE. 					-- for clothes worn by non-player characters.
							-- If you defined here 'IN joe', the clothing
							-- would be listed in Joe's possessions:
							-- "You see Joe here. Joe is carrying a book and 
							-- a sweater." 


-- In defining a piece of clothing, you should
--  1) define it ISA CLOTHING (and not: ISA OBJECT)
--  2) give it one of five attributes 'headcover', 'topcover', botcover', 'footcover'
--  or 'handcover'; sometimes two of these are needed.
--   Which attribute(s) to use depends on the type of clothing; see the clothing table below.
--  3) A number 2, 4, 8, 16, 32 or 64 needs to be added after the above attribute.
--   You cannot decide the number yourself; look it up from the clothing table below.
--   If the value of an attribute for a piece of clothing is 0 in the table, don't mention 
--   this attribute in connection with your clothing object.


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
-- vest/bra				0		2 		0		0		0
-- undies/panties			0		0		2		0		0
-- teddy				0		4		4		0		0
-- blouse/shirt/T-shirt		0		8		0		0		0
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



-- The concept of "xwear.i"  (This and the following paragraphs are taken from Alan
---------------------------	-- Bampton's original  'xwear' documentation, with minor
					-- alterations.)


-- The basic idea behind this part of the library is that clothing is worn in
--  'layers' and it is rather silly to allow players to (say) take off or put 
-- on a shirt if they are wearing a jacket. To simulate this in ALAN I've chosen 
-- to apply a numeric based layering system, and divide the body into five zones 
-- of coverage. The zones are 'head', 'hands', 'feet', 'top' (for top half of torso) 
-- and 'bot' (for bottom half of torso). 
-- All objects have these five zones defined as default attributes (set to 0), for the 
-- head zone the attribute is headcover for the hands zone handscover and so on.
-- Every clothing object will thus need one or more of its 'zonecover' attributes 
-- set to reflect the zone(s) it covers and its relative position in the layers of 
-- clothing worn. A simple example would be a shirt, this covers only the 'top' zone 
-- and so needs its topcover attribute to be set (to 8, just why it's 8 will become 
-- clear shortly).
-- The principle used is that the closer to the skin an item is normally worn, 
-- the lower its 'cover' attribute is. The library operates on the assumption that 
-- items with higher value cover attributes for a particular zone are worn over items 
-- with lower value attributes. When a player attempts to put on an article of clothing, 
-- each zone it would affect is checked and compared to the related zonal total of any 
-- clothes already worn. If the value of the new clothing is not greater that the total(s)
-- of clothing already worn (on a zone by zone basis) then the library will not allow 
-- the wearing of that item. There are a few notable exceptions to this rule, but 
-- I'll come back to those a little later.


-- How it works in practice
---------------------------


-- This part of the library might sound complex, but it is actually very simple to use,
-- here's an example of how it works.
-- Assume our hero starts the game wearing just vest and shorts and the player issues 
-- the command 'put on shirt'. A quick check of the chart below should reveal that a shirt 
-- has only its topcover attribute set to non-zero, (all the other zones are zero, 
-- which means they are irrelevant for this item). 
-- The library totals the topcover attributes of all the clothes currently worn, like so:-
-- Starting with a total of 0, and checking the vest, this has a topcover attribute of 2, 
-- so total topcover is 0 + 2 = 2.
-- Next the library checks the shorts, these have a topcover attribute of 0, so total 
-- topcover is 2 + 0 = 2.
-- As there are no other clothes to consider, the library now compares the topcover attribute 
-- of the item we are attempting to put on, (a shirt with a topcover attribute of 8 in 
-- this case) to the total value of items already worn (2). 
-- Because the topcover attribute of the shirt (8) is greater than the calculated total (2), 
-- this is evaluated as being a 'legal' instruction and the library allows the shirt to be put on.
-- Now consider the situation had the player started the game wearing vest, shorts and a jacket, 
-- this is what happens should he try to 'put on the shirt.'
-- Starting with a total of 0, and checking the vest, this has a topcover attribute of 2, 
-- so total topcover is 0 + 2 = 2.
-- Next the library checks the shorts, these have a topcover attribute of 0, so total 
-- topcover is 2 + 0 = 2.
-- Finally the library checks the jacket, this has a topcover attribute of 32, so total 
-- topcover is 2 + 32 = 34.
-- Because the topcover attribute of the shirt (8) is now not greater than the calculated 
-- total (34), this is evaluated as being an 'illegal' instruction and the library won't 
-- allow the shirt to be put on.
-- That demonstrates the basic principle of the library, removing clothes uses a variation 
-- of the 'compare to total' equation to allow / disallow removal of clothing, an example would 
-- be that our vest, shorts and jacket wearing player would NOT be allowed to remove 
-- the vest while he still had the jacket on.


-- Exceptions to the rule...
----------------------------


-- Now I'll confuse the issue. Firstly the numbers in my chart are not born of some sort of 
-- weird fixation with multiples, there is a very good reason why the numbers are set as they 
-- are, computer/maths types will recognise the sequence and realise it is all 'binary' based 
-- and know it makes it possible to calculate exactly what the player is wearing in terms of 
-- layers.
-- Some female clothing breaks the rules defined above and is not so easy to deal with. An example,
-- although pantyhose is worn under a skirt, dress or coat it can actually be put on or removed 
-- with the garment worn over it still on. The library recognises this capability and deals with 
-- it properly by assigning the dress/skirt and coat items particular properties in that they 
-- don't affect the ability to wear or remove lower layer clothing that covers the bottom of 
-- the torso only.
-- Although it's physically possible to put on/remove trousers while wearing a skirt or dress,
-- this (and a few other neat dressing/undressing tricks) is considered illegal here.


-- How to create clothes that use 'xwear.i'
-------------------------------------------


-- That covers the way the library works, here's how to define objects to actually make 
-- use of it. As an example, this is how to define a dress and restrict it to being worn 
-- by a female. 


-- THE dress ISA CLOTHING IN changingroom
	-- NAME little black dress
	-- MENTIONED "little black dress"
	-- IS
		-- botcover 32.
		-- topcover 8.
		-- sex 2.
-- END THE.


-- Notice that it is only necessary to mention the attributes that are non-zero, this 
-- minimises the amount of coding required. Apart from making sure the hero is set as 
-- 'sex 2.' if we wish her to be able to wear this dress, this is all the author needs 
-- to do, the library will police what is wearable and keep track of things without any 
-- further work.
-- I expect most authors would prefer to start their games with a player at least partially 
-- dressed. This is just a matter of defining the required clothes object as being in the 
-- 'worn' container. Here's how to define underwear that our female hero(ine?) starts 
-- the game wearing.


-- THE bra ISA CLOTHING IN worn
	-- NAME white bra
	-- MENTIONED "white bra"
	-- IS
		-- topcover 2.
		-- sex 2.
-- END THE bra.


-- THE panties ISA CLOTHING IN worn
	-- NAME white panties
	-- MENTIONED "white panties"
	-- IS
		-- botcover 2.
		-- plural.
		-- sex 2.
-- END THE panties.


-- The library as it stands also prevents wearing of duplicate clothes, or things that are 
-- logically mutually exclusive - e.g. the player can wear a dress or a skirt, but not both.



-- =============================================================


----- DEVICE


-- =============================================================


EVERY device ISA OBJECT
	IS NOT 'on'.
	

	VERB examine
		DOES 
			IF THIS IS 'on'
				THEN "It is currently on."
				ELSE "It is currently off."
			END IF.
	END VERB.


	VERB turn_on
		CHECK THIS IS NOT 'on'
			ELSE SAY THE THIS. "is already on."
		AND CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
		AND THIS IS reachable
			ELSE SAY THE THIS. "is out of your reach."
		AND THIS IS NOT broken
			ELSE "Nothing happens."
		DOES ONLY
			"You turn on" SAY THE THIS. "."
			MAKE THIS 'on'.
	END VERB.


	VERB turn_off
		CHECK THIS IS 'on'
			ELSE SAY THE THIS. "is already off."
		AND CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
		AND THIS IS reachable
			ELSE SAY THE THIS. "is out of your reach."
		DOES ONLY 
			"You turn off" SAY THE THIS. "."
			MAKE THIS NOT 'on'.
	END VERB.


-- The following verb switches a device off if the device is on, and vice versa.

	
	VERB switch
		CHECK CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
		AND THIS IS reachable
			ELSE SAY THE THIS. "is out of your reach."
		AND THIS IS NOT broken
			ELSE "Nothing happens."
		DOES ONLY
			IF THIS IS 'on'
				THEN "You switch" SAY THE THIS. "off."
					MAKE THIS NOT 'on'.
				ELSE "You switch" SAY THE THIS. "on."
					MAKE THIS 'on'.
			END IF.
	END VERB.

END EVERY.



-- =============================================================


----- DOOR


-- =============================================================


EVERY door ISA OBJECT
	IS closeable.
	IS closed.
	IS lockable.
	IS NOT locked.
	IS NOT takeable.


	VERB examine
		DOES 
			IF THIS IS closed
				THEN "It is currently closed."
				ELSE "It is currently open."
			END IF.
	END VERB.

		

	VERB knock
		DOES ONLY
			IF THIS IS closed
				THEN "You knock on" SAY THE THIS. "$$. There is no reply."
				ELSE "You don't find it purposeful to knock on the open door."
			END IF.
	END VERB.



	VERB look_behind
		DOES ONLY 
			IF THIS IS closed
				THEN "You cannot look behind the door - it is closed."
				ELSE "You notice nothing special behind the door."
			END IF.
	END VERB.


	VERB look_under
		DOES ONLY
			IF THIS IS closed
				THEN "The gap under the closed door is so narrow that you can't
					see anything of what lies on the other side."
				ELSE "You notice nothing special under the door."
			END IF.
	END VERB.


END EVERY.



-- =============================================================


----- LIGHTSOURCE


-- =============================================================


EVERY lightsource ISA OBJECT
	IS NOT lit.
	IS NOT broken.
	IS natural. -- A natural lightsource is for example a candle, a match or a torch. 
			-- A NOT natural lightsource is for example a flashlight or a lamp.
			-- You cannot switch on or off a natural lightsource.



	VERB examine
		DOES 
			IF THIS IS lit
				THEN 
					IF THIS IS natural
						THEN "It is currently lit."
						ELSE "It is currently on."
					END IF.
				ELSE
					IF THIS IS natural
						THEN "It is currently not lit."
						ELSE "It is currently off."
					END IF.
			END IF.
	END VERB.


	VERB light
		CHECK THIS IS NOT lit
			ELSE SAY THE obj. "is already providing light."
		AND THIS IS NOT broken
			ELSE "Nothing happens."
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
			ELSE "But" SAY THE THIS. "is not providing light!"
		DOES ONLY "You extinguish the" SAY THE THIS. "."
			MAKE THIS NOT lit.
	END VERB.


	VERB turn_on
		CHECK THIS IS NOT natural
			ELSE "That's not something you can $v on."
		AND THIS IS NOT lit
			ELSE SAY THE THIS. "is already providing light."
		AND THIS IS NOT broken
			ELSE "Nothing happens."
		DOES ONLY
					"You turn on" SAY THE THIS. "."
					MAKE THIS lit.
					
	END VERB.


	VERB turn_off
		CHECK THIS IS NOT natural
			ELSE "That's not something you can $v off."
		AND THIS IS lit
			ELSE SAY THE THIS. "is already off."
		DOES ONLY 
					"You turn off the" SAY THIS. "."
					MAKE THIS NOT lit.	
			  
	END VERB.


-- The following verb switches a NOT natural lightsource on if it is off, and vice versa
-- (when the player forgets, or doesn't bother, to type 'on' or 'off' after switch).


	VERB switch
		CHECK THIS IS NOT natural
			ELSE SAY THE obj. "is not something you can switch on or off."
		AND THIS IS reachable
			ELSE SAY THE THIS. "is out of your reach."
		AND THIS IS NOT broken
			ELSE "Nothing happens."
		AND CURRENT LOCATION IS lit
			ELSE 
				"You switch" SAY THE THIS. "on."
				MAKE THIS lit.		
		DOES ONLY
			IF THIS IS lit
				THEN "You switch" SAY THE THIS. "off."
					MAKE THIS NOT lit.
				ELSE "You switch" SAY THE THIS. "on."
					MAKE THIS lit.
			END IF.
	END VERB.
	

END EVERY.



-- ==============================================================


----- LIQUID


-- ==============================================================


EVERY liquid ISA OBJECT		
	
	CONTAINER
		HEADER "In" SAY THE THIS. "you see"
		ELSE "There is nothing in" SAY THE THIS. "."	

		-- We declare this class a container to enable player commands such as
		-- 'throw sack into water', 'look into water' and 'take pearl from water'.
		-- Also cases such as 'pour red potion into blue potion' require that this 
		-- class behaves like a container. 
	

	HAS vessel zero_vessel.		-- The 'vessel' attribute takes care of that if a liquid is in a container,
						-- the verb 'take' will automatically take the container instead
						-- (if the container is takeable). Trying to take a liquid that
						-- is in a fixed-in-place container will yield "You can't carry
						-- [the liquid] around in your bare hands." 
						-- The default value 'zero_vessel' tells the compiler that the liquid is 
						-- not in any container. 
							


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

	-- The verb 'pour', as defined in this library, works also for the container of a liquid;
	-- i.e. if there is some juice in a bottle, 'pour bottle' and 'pour juice' work equally well.
	-- Note also that the verb 'empty' is not a synonym for 'pour';
	-- 'empty' only works for container objects.


	INITIALIZE
		SCHEDULE check_vessel AT THIS AFTER 0.		-- this event is defined further below

		
	VERB take
		DOES ONLY
			IF vessel OF THIS = zero_vessel OR vessel OF THIS IS NOT takeable
				THEN "You can't carry" SAY THE THIS. "around in your bare hands."
			ELSE LOCATE vessel OF THIS IN hero.
				"($$" SAY THE vessel OF THIS. "$$)$nTaken." 
			END IF.
	END VERB.


	VERB take_from
	   WHEN obj1
		DOES ONLY
			IF vessel OF THIS = zero_vessel OR vessel OF THIS IS NOT takeable
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
		WHEN obj1
		DOES ONLY
			-- implicit taking:
			IF THIS NOT IN hero
				THEN 
					IF vessel OF THIS = zero_vessel OR vessel OF THIS IS NOT takeable
						THEN "You can't carry" SAY THE THIS. "around in your bare hands."
					ELSE LOCATE vessel OF THIS IN hero.
						"(taking" SAY THE vessel OF THIS. "first)$n"
					END IF.
			END IF.
			-- end of implicit taking.

			IF THIS IN hero			-- i.e. if the implicit taking was successful
				THEN
					"You give" SAY THE vessel OF THIS. "to" SAY THE obj2. "."
					LOCATE vessel OF THIS IN obj2.
			END IF.
	END VERB.


	VERB pour
		DOES ONLY
			-- implicit taking:
			IF THIS NOT IN hero
				THEN 
					IF vessel OF THIS = zero_vessel OR vessel OF THIS IS NOT takeable
						THEN "You can't pour" SAY THE THIS. "anywhere since you are not
							carrying it."
					ELSE LOCATE vessel OF THIS IN hero.
						"(taking" SAY THE vessel OF THIS. "first)$n"
					END IF.
			END IF.
			-- end of implicit taking.
			
			IF THIS IN hero		-- i.e. if the implicit taking was successful
				THEN
					"You pour" SAY THE THIS.
						IF floor HERE
							THEN "on the floor."
							ELSE "on the ground."
						END IF.
					LOCATE THIS AT hero.
					SET vessel OF THIS TO zero_vessel.
			END IF.
	END VERB.


	VERB pour_in
		WHEN obj1
			DOES ONLY
				-- implicit taking:
				IF THIS NOT IN hero
					THEN 
						IF vessel OF THIS = zero_vessel OR vessel OF THIS IS NOT takeable
							THEN "You don't have" SAY THE vessel OF THIS. "." 
						ELSE LOCATE vessel OF THIS IN hero.
							"(taking" SAY THE vessel OF THIS. "first)$n"
						END IF.
				END IF.
				-- end of implicit taking.

				IF THIS IN hero		--i.e. if the implicit taking was successful
					THEN
						"You pour" SAY THE THIS. "into" SAY THE obj2. "."
						LOCATE THIS IN obj2.
						SET vessel OF THIS TO obj2.
				END IF.
	END VERB.


	VERB pour_on
		WHEN obj1
			DOES ONLY
				-- implicit taking:
				IF THIS NOT IN hero
					THEN 
						IF vessel OF THIS = zero_vessel 
							THEN "You can't carry" SAY THE THIS. "around in your bare hands."
						ELSIF vessel OF THIS IS NOT takeable
							THEN "You don't have" SAY THE vessel OF THIS. "." 
						ELSE LOCATE vessel OF THIS IN hero.
							"($$" SAY THE vessel OF THIS. "$$)$n"
						END IF.
				END IF.
				-- end of implicit taking.
				
				IF THIS IN hero		-- i.e. if the implicit taking was successful
					THEN
						IF obj2 = floor OR obj2 = ground
							THEN LOCATE THIS AT hero.
						 		"You pour" SAY THE THIS. "on" SAY THE obj2. "."
								SET vessel OF THIS TO zero_vessel.
						ELSIF obj2 ISA SUPPORTER
							THEN LOCATE THIS IN obj2.
								"You pour" SAY THE THIS. "on" SAY THE obj2. "."
				  				SET vessel OF THIS TO zero_vessel.
						ELSE "It wouldn't be sensible to pour anything on" SAY THE obj2.
						END IF.
				END IF.
	END VERB.		


	VERB fill_with
		WHEN obj2
			 DOES SET vessel OF THIS TO obj1.
	END VERB.


	VERB put_in
		WHEN obj1
			DOES SET vessel OF THIS TO obj2.
	END VERB.


	VERB put_on
		WHEN obj1
			 DOES SET vessel OF THIS TO obj2.
		WHEN obj2
			DOES ONLY "It is not possible to $v" SAY obj1. "onto" SAY THE THIS. "."
	END VERB.


	-- The verbs 'empty', 'empty_in' and 'empty on' will be disabled as ungrammatical with liquids:	

	VERB 'empty'
		DOES ONLY "You can only empty containers."
	END VERB.

	VERB empty_in, empty_on
		WHEN obj1
		DOES ONLY "You can only empty containers."
	END VERB.


END EVERY.



-- Here is the default vessel for liquids; if the vessel of a liquid is
-- 'zero_vessel', it means that the liquid is not in any container.


THE zero_vessel ISA OBJECT
	CONTAINER 
END THE.



-- This event checks that if a liquid is outside a container, its container will
-- be 'zero_vessel':


EVENT check_vessel
	FOR EACH liq ISA LIQUID, DIRECTLY AT CURRENT LOCATION DO	
		SET vessel OF liq TO zero_vessel.
	END FOR.
	SCHEDULE check_vessel AFTER 1.
END EVENT.




-- =============================================================


----- LISTABLE_CONTAINER


-- =============================================================


EVERY LISTABLE_CONTAINER ISA OBJECT			-- ACTORS are separately defined as 
	CONTAINER						 --  containers further below


	VERB examine 
		DOES 
			IF THIS IS closeable
				THEN 
					IF THIS IS closed
						THEN "It is closed."
						ELSE "It is open."
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
				ELSE "You can't, since" SAY THE THIS. "is closed."
			END IF.
	END VERB.


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
-- THE box ISA LISTABLE_CONTAINER
-- ...
-- END THE.
--
-- Then, 'examine' lists the contents of the container.


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


-- THE box ISA LISTABLE_CONTAINER
--   OPAQUE CONTAINER
-- ...
-- END THE.


-- Objects in an opaque container cannot be seen or manipulated.
-- To change this, declare e.g.
--
-- MAKE box NOT OPAQUE.
--



-- ==============================================================


----- SCENERY


-- ==============================================================


EVERY scenery ISA OBJECT
	IS NOT takeable.
	DESCRIPTION ""


	VERB examine
		DOES ONLY SAY THE THIS. "doesn't appear particularly interesting to you."
	END VERB.

	-- see also checks in the verbs 'take' and 'take from' in 'verbs.i'

END EVERY.



-- ===============================================================


----- SOUND


-- ===============================================================


EVERY sound ISA OBJECT
	IS NOT examinable.
	IS NOT reachable.
	IS NOT movable.


	VERB smell
		DOES ONLY "That's not something you can smell."
	END VERB.


END EVERY.



-- ==============================================================


----- SUPPORTER


-- ==============================================================


EVERY supporter ISA OBJECT
	CONTAINER
		HEADER "On" SAY THE THIS. "you see"
		ELSE "There's nothing on" SAY THE THIS. "."


	VERB examine
		DOES LIST THIS.
	END VERB.


	VERB look_in							
		DOES ONLY "That's not something you can look into."
	END VERB.


	VERB put_in
   	  WHEN obj2
		DOES ONLY "You can't put anything inside" SAY THE THIS. "."
	END VERB.


	VERB search
		DOES ONLY "You find nothing further of interest."
	END VERB.


END EVERY.


-- To place objects on a supporter, do the definition in the following way:
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
--	IN tray				-- note the IN here, even if the apple will be described as 
-- 	...					-- being on the tray
-- END THE.
--
--
-- THE book ISA OBJECT
--   IN table
--    ... 
-- END THE.
--
--
-- Note that the 'examine' command will list what is on the surface of a supporter, not what,
-- if anything, is inside the supporter. For example, if you have a supporter called 'table' in your game 
-- with a drawer in it,
--
-- DON'T do this:
--
-- THE drawer ISA OBJECT
--	CONTAINER
-- 	IN table.
-- END THE.
--
-- or this:
--
-- THE drawer ISA LISTABLE_CONTAINER
--    IN table.			
-- END THE.    
--
-- This would result in something like 
--
-- "There's a table here. On the table you see a book and a drawer."
--
--
-- Instead, do either of the two following things:
--
-- 
-- 1)
--
-- THE drawer ISA OBJECT
--	CONTAINER
--	AT bedroom
-- END THE.
--
-- or
--
-- THE drawer ISA LISTABLE_CONTAINER
--    AT bedroom  
-- END THE.
--
--
-- In other words, just declare the drawer present in the location, not as part of the table.
-- ('AT table' would not be possible, as all objects must be initially located to
-- instances inheriting from the class 'location'.)
-- If this method feels clumsy, try the next one:
--
-- 
-- 2)
-- 
-- THE table_parts ISA OBJECT
--	AT bedroom	 
--	DESCRIPTION ""			-- we don't want this object to appear in the room description
--    CONTAINER
--		HEADER "In the table there is"
--		ELSE ""			-- if the parts are removed from the table, there will be no 
-- END THE.					-- mention of them in the description of the table
--
--
-- THE drawer ISA LISTABLE_CONTAINER
--    IN table_parts
--    ...
-- END THE.
--
--
-- THE table ISA SUPPORTER
-- ...
--	     VERB examine
-- 			DOES ONLY 
--				LIST table.			-- lists what is on the table
--				LIST table_parts.		-- lists the components of the table, i.e. the drawer in this example
--	            ...	
--         END VERB.
--
-- END THE.
--
--
-- In other words, define a separate container for the component(s) in the supporter
-- which will then be listed after the surface objects have been listed.
-- Consequently, the above will result in something like this:
--
-- > examine table
-- On the table you see a book. In the table there is a drawer.	
--
--
-- Note that in a location description (e.g. after LOOK), only the things *on* a supporter
-- (and not any components) will be described, according to how things are defined in 
-- this library.
-- 
--



-- ==============================================================


----- WEAPON


-- ==============================================================


EVERY weapon ISA OBJECT
	IS NOT fireable.
END EVERY.


-- see checks for this class in the file 'verbs.i', verbs 'attack_with', 'fire',
-- 'fire_with', 'kill_with', 'shoot' and 'shoot_with'.



-- ==============================================================


----- WINDOW


-- ==============================================================


-- You can look out of and through a window. 
-- When examined, a window is automatically described to be either open or closed.


EVERY window ISA OBJECT
	IS closeable.
	IS closed.
	IS NOT takeable.


	VERB examine
		DOES 
			IF THIS IS closed
				THEN "It is currently closed."
				ELSE "It is currently open."
			END IF.
	END VERB.


	VERB look_behind
		DOES ONLY 
			"That's not possible."
	END VERB.


	VERB look_out_of
		DOES ONLY "You see nothing special looking out of the window."
	END VERB.


	VERB look_through
		DOES ONLY "You see nothing special looking through the window."
	END VERB.


END EVERY.



-- ===============================================================

-- ===============================================================
--
-- 2. Actor classes
--
-- ===============================================================

-- ===============================================================


-- First, we declare some common characteristics for all actors:


ADD TO EVERY ACTOR
   IS NOT inanimate.
   IS NOT following. 
   IS NOT sitting.
   IS NOT lying_down.	
	
   CONTAINER								-- so that actors can receive and carry objects

   INITIALIZE								-- all actors will obey this script from the start of the game
	USE SCRIPT following_hero FOR THIS.
	
	SCRIPT following_hero						-- this code will make any actor follow the hero
			STEP WAIT UNTIL hero NOT HERE			-- if the actor is given the attribute 'following'.
				 IF THIS IS following
					THEN
						LOCATE THIS AT hero.
						SAY THE THIS. "follows you."				
				 END IF.
			       USE SCRIPT following_hero FOR THIS.

			

   
   VERB examine
	DOES 
		IF COUNT ISA THING, IN THIS > 0  			-- This if-statement will make a description of an
			THEN LIST THIS. 					-- actor's possessions show up every time the actor is examined.
		END IF.							-- Note that this doesn't apply to the hero which is defined
										-- separately further below.
   END VERB. 	
   	
END ADD TO.
				

-- To make an actor follow the hero, give it the 'following' attribute, e.g.:

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

--
-- If you wish to have an actor follow the hero right from the start of the game, you can naturally just declare
--
-- THE servant ISA ACTOR
-- 	IS following.
-- ...
-- END THE.
--
--

-- To stop an actor from following the hero, just make the actor NOT following.




-- ================================================================


----- PERSON			-- HAS can_talk


-- ================================================================


EVERY person ISA ACTOR
    HAS can_talk.
END EVERY.



-- ================================================================


----- NAMED_ACTOR		-- NOT can_talk


-- ================================================================


EVERY named_actor ISA ACTOR
  DESCRIPTION SAY THIS. "is here."
  DEFINITE ARTICLE ""
  INDEFINITE ARTICLE ""
END EVERY.



-- ================================================================


----- NAMED_PERSON		-- HAS can_talk


-- ================================================================


EVERY named_person ISA PERSON
  DESCRIPTION SAY THIS. "is here."
  DEFINITE ARTICLE ""
  INDEFINITE ARTICLE ""
END EVERY.



-- ================================================================


----- Expressing FEMALE and MALE		


-- ================================================================


-- 'Male' and 'female' are not declared as separate classes in this library. 
-- This is to avoid numerous new classes such as female_actor, named_female_actor,
-- female_person, named_female_person, and the same for male. Instead, just
--
-- add the line
-- 
-- PRONOUN him
--
-- or
--
-- PRONOUN her
--
-- to any actor instance to make it possible for the player to refer to the 
-- actor with these pronouns. For example,
--
-- THE jessie ISA NAMED_PERSON
--    PRONOUN her
--    ...
-- END THE.



-- ================================================================

-- ================================================================

-- 3. The hero

-- ================================================================

-- ================================================================



THE hero ISA ACTOR
	DEFINITE ARTICLE ""
	INDEFINITE ARTICLE ""
	MENTIONED "yourself"
	IS NOT sitting.
	IS NOT lying_down.
	
	------------------------------------

	-- These three attributes, as well as the 'schedule' statement following them,
	-- are needed for the 'notify' command ('verbs.i'); ignore.

	HAS oldscore 0. 		-- Records previous score so 'checkscore' event
 					-- can compare with the current score 
	IS notify_on. 		-- Set by 'notify' verb, records whether 
					-- player wants to see score messages or not. 
	IS NOT seen_notify. 	-- Records whether player has seen the notify verb 
					-- instructions yet. 

	INITIALIZE
		SCHEDULE check_score AT hero AFTER 0.		

	------------------------------------

      CONTAINER

  	LIMITS	-- Remove this whole section (from this line until 'HEADER') if you 
			-- wish to have no limits to how much the hero can carry. 
    		COUNT 10 Then
      		"You can't carry anything more. You have to drop something 
			first."
    		WEIGHT 50 Then
      		"You can't carry anything more. You have to drop something 
			first."
 
  	HEADER
      	IF CURRENT LOCATION IS NOT lit
			THEN "Although you cannot see your belongings in the dark, you 
				remember that you're carrying"
			ELSE "You are carrying"
		END IF.
    	ELSE
      	"You are empty-handed."



	VERB examine
		DOES ONLY "You notice nothing unusual about yourself."	
			IF COUNT ISA CLOTHING, IN worn > 0
				THEN LIST worn.			-- this will list what the hero is wearing
			END IF.
	END VERB.


END THE.


SYNONYMS
  me, myself, yourself, self = hero.


-- Please refer also 'verbs.i' where there are numerous verb checks for the hero, 
-- so that the hero cannot e.g. attack himself etc.

