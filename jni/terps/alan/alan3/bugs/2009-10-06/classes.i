-- ALAN NEW LIBRARY: CLASSES (file name: 'classes.i')


-- This library file defines various object and actor classes, as well as the instance 'hero'
-- (=the player character). Many of these classes are frequently used in verb 
-- definitions in 'verbs.i' so they should be edited or removed with caution.



-- 1. OBJECT CLASSES
-- ================= 

-- BACKGROUND
	-- is present in the location but cannot be reached or taken. 
	-- It doesn't appear automatically in room descriptions.
	-- An example of a background is for example a mountain visible in the distance. 
	-- Compare this with 'scenery' below.

-- CLOTHING 
	-- = a piece of clothing that behaves according to Alan Bampton's 'xwear.i' extension.
      -- This extension has been assimilated to this library. 
      -- This extension prevents clothes from being worn in an illogical order, e.g. you 
	-- cannot put on a shirt if you are already wearing a jacket, and so forth.
	-- Also the verbs 'wear', 'remove' and 'undress' are defined here.

-- CONTAINER_OBJECT
	-- is an object that can contain things.
	-- The contents of the object will be listed both after 'look' (= in the room description)
	-- and after 'examine'.
	-- See the tips in this section to change the conditions of the contents listing more 
	-- suitable for your purposes.

-- DEVICE  
	-- A machine or an electronic device, for example a TV. Can be turned 
	-- (=switched) on and off if it is not broken.
	-- Attributes: 'on' and NOT 'on'.

-- DOOR 
	-- can be opened, closed, locked and unlocked. 
	-- Is by default closed, not locked.
	-- Attributes: closeable, (not) closed, lockable, (not) locked.

-- LIQUID 
	-- can only be taken if it is in a container. You can fill something with it, 
	-- and you can pour it somewhere.
	-- A liquid is by default NOT drinkable.

-- LIGHTSOURCE 
	-- IS natural or NOT natural 
	-- (a natural lightsource is for example a match or a torch).
	-- Can be turned on and off, lighted and extinguished (= put out) if it 
      -- is not broken. A natural lightsource 
	-- cannot be turned on or off, it can only be lighted and extinguished.

-- SCENERY  
	-- Behaves like a normal object, can be reached and manipulated but not taken.
	-- The default message for 'take' will be "You deem the [object] as not important for
	-- your purposes and leave it where it is."
	-- The default message for 'examine' will be "The [object] doesn't appear particularly 
	-- interesting."
	-- A scenery object is not automatically listed after the room description when 
      -- you type 'look'. You have to include it manually in the room description.
	-- Compare this with 'background' above.

-- SOUND 
	-- Can be listened to but not examined, searched, smelled or manipulated.
      -- Cannot be turned on or off, this has to be implemented manually by giving 
      -- the sound the 'switchable' attribute.

-- SUPPORTER 
	-- You can put things on this and you can stand on this. It is declared a container, 
	-- so you can take things from it, as well. When there's something on a supporter, 
      -- an automatic listing of it will appear in the room description.

-- WEAPON  
	-- IS fireable (e.g. a cannon) or NOT fireable (e.g. a baseball bat).

-- WINDOW 
	-- can be opened, closed, looked through and out of.




-- 2. ACTOR CLASSES
-- ================

-- PERSON 
	-- is able to talk

-- NAMED_PERSON 
	-- has no article in front of the name

-- MALE
	-- a person that can be referred to with the pronoun 'him'

-- FEMALE
	-- a person that can be referred to with the pronoun 'her'



-- 3. INSTANCE
-- ===========


-- the hero




-- =============================================================

-- =============================================================
--
-- 1. Object classes
--
-- =============================================================

-- =============================================================





-- =============================================================

-- BACKGROUND

-- =============================================================


EVERY background ISA THING
	IS NOT reachable.
	DESCRIPTION ""
END EVERY.







-- ==============================================================

-- CLOTHING     (+ the verbs WEAR, REMOVE, UNDRESS)

-- ==============================================================



-- To use this subclass, see the documentation text right after the
-- code below.
-- This subclass makes use of Alan Bampton's 'xwear.i' extension
-- written originally for ALAN V2, converted here to V3 and 
-- assimilated fully to the present library. 





-----------------------------------------------------------------
-- First, we declare the container for clothing.
-----------------------------------------------------------------


THE worn ISA ENTITY		
	CONTAINER TAKING CLOTHING.
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
-- how the above verbs behave with this subclass.
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
-- First check the	 'topcover' attributes, if 'obj' fails this test
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
			THEN
				"You pick up the" SAY THE THIS. "."
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





VERB examine
	DOES AFTER
		IF THIS IS NOT OPAQUE
			THEN 
				IF COUNT ISA OBJECT, IN THIS > 0
					THEN LIST THIS.
				END IF.
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



-----------------------------------------------------------------------
-- DOCUMENTATION FOR CLOTHING 
-----------------------------------------------------------------------


-- The quick guide
------------------

-- Here follows a quick overview for using the subclass 'clothing'. A more closely-detailed
-- description of this subclass is found below this overview.


-- A piece of clothing in your game should look something like these three examples:

-- THE jacket ISA CLOTHING AT lobby
-- 	IS topcover 32.
-- END THE.

-- THE jeans ISA CLOTHING IN wardrobe
--	IS botcover 16.
-- END THE.

-- THE hat ISA CLOTHING IN worn   -- IN worn = worn by the player character
--	IS headcover 2.
-- END THE.

-- In defining a piece of clothing, you should
--  1) define it ISA CLOTHING (and not: ISA OBJECT)
--  2) give it one of five attributes 'headcover', 'topcover', botcover', 'footcover'
--  or 'handcover'; sometimes two of these are needed.
--   Which attribute(s) to use depends on the type of clothing; see the clothing table below.
--  3) A number 2, 4, 8, 16, 32 or 64 needs to be added after the above attribute.
--   You cannot decide the number yourself; look it up from the clothing table below.
--   If the value of an attribute for a piece of clothing is 0 in the table, don't mention this 
--   attribute in connection with your clothing object.

-- The above is enough; the rest is then handled automatically by the library.

-- End of the quick guide. See the clothing table right below. The following chapters
-- shed more light on the principles and the use of this subclass. 




-- The clothing 'table'
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






-- The concept of "xwear.i" (This and the following chapters are taken from Alan
---------------------------	Bampton's 'xwear' documentation)

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


-- CONTAINER_OBJECT


-- =============================================================



EVERY container_object ISA OBJECT			-- ACTORS are separately defined as containers further below
	CONTAINER

	VERB examine 
		DOES AFTER 
			IF THIS IS NOT OPAQUE
				THEN LIST THIS.
			END IF.
	END VERB.

	VERB look_in
		DOES AFTER 
			IF THIS IS NOT OPAQUE
				THEN LIST THIS.
			END IF.
	END VERB.

END EVERY.


-- Tips for using container objects:


-- 1)

-- The contents of a normal CONTAINER are not listed when examined. 
-- If you wish to have the contents listed after 'examine', you should
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
-- THE box ISA CONTAINER_OBJECT
-- ...
-- END THE.
--
-- Then, 'examine' works fine and lists the contents of the container.


-- 2)

-- For the command 'inventory' to list the contents of a container 
-- the hero is carrying, you should manually list all possible containers
-- that can be carried by the hero in your game
-- under the verb 'inventory' in the file 'verbs.i', thus:
--
-- VERB i
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
-- If you leave out these additions, the bag and the box will be listed after 'inventory' 
-- in the following way:
-- 
-- "You are carrying a bag and a box."
-- 
-- only. But with the above additions, the outcome is e.g.
--
-- "You are carrying a bag and a box. The bag contains a loaf of bread. The box is empty."



-- 3)

-- To declare a container the contents of which should not be listed after 'look' or 'examine',
-- declare it an 'opaque container' in the following way:

-- THE box ISA OBJECT
--   OPAQUE CONTAINER
-- ...
-- END THE.


-- Objects in an opaque container cannot be seen or manipulated.
-- To change this, use e.g.
--
-- MAKE box NOT OPAQUE.
--






-- =============================================================

-- DEVICE

-- =============================================================


EVERY device ISA OBJECT
	IS switchable.
	IS NOT 'on'.
	

	VERB examine
		DOES 
			IF THIS IS 'on'
				THEN "It is currently on."
				ELSE "It is currently off."
			END IF.
	END VERB.


	VERB turn_on1, turn_on2, switch_on1, switch_on2
		CHECK THIS IS NOT broken
			ELSE "Nothing happens."
		AND THIS IS NOT 'on'
			ELSE SAY THE THIS. "is already on."
		AND CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
		DOES ONLY
					"You turn on" SAY THE THIS. "."
					MAKE THIS 'on'.
	END VERB.


	VERB turn_off1, turn_off2, switch_off1, switch_off2
		CHECK THIS IS 'on'
			ELSE SAY THE THIS. "is already off."
		AND CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
		DOES ONLY 
					"You turn off" SAY THE THIS. "."
					MAKE THIS NOT 'on'.
	END VERB.
	

END EVERY.




-- =============================================================

-- DOOR

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


	VERB look_behind
		DOES ONLY 
			IF THIS IS closed
				THEN "You cannot look behind the door; it is closed."
				ELSE "You notice nothing special behind the door."
			END IF.
	END VERB.


	VERB look_under
		DOES ONLY
			IF THIS IS closed
				THEN "The space under the closed door is so narrow that you can't
					see anything of what lies on the other side."
				ELSE "You notice nothing special under the door."
			END IF.
	END VERB.


END EVERY.




-- =============================================================

-- LIGHTSOURCE

-- =============================================================


EVERY lightsource ISA OBJECT
	IS NOT lit.
	IS natural. -- A natural lightsource is for example a candle, a match or a torch. 
			-- A NOT natural lightsource is for example a flashlight or a lamp.
			-- You cannot switch on or off a natural lightsource.

	VERB examine
		DOES 
			IF THIS IS lit
				THEN "It is currently lit."
				ELSE "It is currently not lit."
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
			ELSE "But" SAY THE THIS. "is not providing light."
		DOES ONLY "You extinguish the" SAY THE THIS. "."
			MAKE THIS NOT lit.
	END VERB.



	VERB turn_on1, turn_on2, switch_on1, switch_on2
		CHECK THIS IS NOT natural
			ELSE "That's not something you can turn on."
		AND THIS IS NOT lit
			ELSE SAY THE THIS. "is already providing light."
		AND THIS IS NOT broken
			ELSE "Nothing happens."
		DOES ONLY
					"You turn on" SAY THE THIS. "."
					MAKE THIS lit.
					
	END VERB.



	VERB turn_off1, turn_off2, switch_off1, switch_off2
		CHECK THIS IS NOT natural
			ELSE "That's not something you can turn off."
		AND THIS IS lit
			ELSE SAY THE THIS. "is already off."
		DOES ONLY 
					"You turn off the" SAY THIS. "."
					MAKE THIS NOT lit.	
			  
	END VERB.
	

END EVERY.



-- ==============================================================

-- LIQUID

-- ==============================================================


EVERY liquid ISA OBJECT


	VERB take
		DOES ONLY "You cannot carry" SAY THE THIS. "around in your hands."
	END VERB.


	VERB pour
		DOES ONLY "You pour the liquid onto the ground where it quickly evaporates."
				LOCATE THIS AT nowhere.
	END VERB.

	VERB pour_in
		WHEN obj1
		DOES ONLY "You pour" SAY THE THIS. "into" SAY THE obj2.
				LOCATE THIS AT obj2.
	END VERB.

	VERB pour_on
		WHEN obj1
		DOES ONLY "That wouldn't accomplish anything."
	END VERB.

	VERB put_in
		WHEN obj1
		DOES ONLY "You pour" SAY THE THIS. "into" SAY THE obj2. "."
				LOCATE THIS IN obj2.
	END VERB.

END EVERY.




-- ==============================================================

-- SCENERY

-- ==============================================================



EVERY scenery ISA OBJECT

	DESCRIPTION ""

	VERB examine
		DOES ONLY SAY THE THIS. "doesn't appear particularly interesting to you."
	END VERB.

	VERB take
		DOES ONLY "You deem" SAY THE THIS. "as not important to your 
                       purposes and leave it where it is."
	END VERB.

	VERB take_from
		WHEN obj1
		DOES ONLY "You deem" SAY THE THIS. "as not important to your
				purposes and leave it where it is."
	END VERB. 

END EVERY.



-- ===============================================================

-- SOUND

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

-- SUPPORTER

-- ==============================================================


EVERY supporter ISA OBJECT
	CONTAINER
		HEADER "On" SAY THE THIS. "you see"
		ELSE "There's nothing on" SAY THE THIS. "."


VERB examine
	DOES AFTER LIST THIS.
END VERB.

VERB search
	DOES ONLY "You find nothing further of interest."
END VERB.


END EVERY.





-- ==============================================================

-- WEAPON

-- ==============================================================


EVERY weapon ISA OBJECT
	IS NOT fireable.
END EVERY.






-- ==============================================================

-- WINDOW

-- ==============================================================


EVERY window ISA OBJECT
	IS closeable.
	IS closed.
	IS NOT takeable.

	VERB examine
		DOES 
			IF THIS IS closed
				THEN "It is closed."
				ELSE "It is open."
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
   
   CONTAINER			-- so that actors can possess things
			 

   VERB examine
	DOES AFTER 
		IF COUNT ISA THING, IN THIS > 0  -- this if-statement will make a description of an
			THEN LIST THIS. 	-- actor's possessions show up every time the actor is examined
		END IF.
   END VERB. 	
   

END ADD TO.




-- ================================================================

-- PERSON

-- ================================================================


EVERY person ISA ACTOR
    HAS can_talk.
END EVERY.



-- ================================================================

-- NAMED_ACTOR		-- HAS NOT can_talk

-- ================================================================


EVERY named_actor ISA ACTOR
  DESCRIPTION SAY THIS. "is here."
	IF COUNT ISA THING, IN THIS > 0
		THEN LIST THIS.
	END IF.
  DEFINITE ARTICLE ""
  INDEFINITE ARTICLE ""
END EVERY.




-- ================================================================

-- NAMED_PERSON		-- HAS can_talk

-- ================================================================


EVERY named_person ISA PERSON
  DESCRIPTION SAY THIS. "is here."
	IF COUNT ISA THING, IN THIS > 0
		THEN LIST THIS.
	END IF.
  DEFINITE ARTICLE ""
  INDEFINITE ARTICLE ""
END EVERY.


-- ================================================================

-- FEMALE AND MALE

-- ================================================================


EVERY female ISA PERSON
    PRONOUN her
END EVERY.


EVERY male ISA PERSON
	PRONOUN him
END EVERY.	




-- ================================================================

-- ================================================================

-- 3. The instance HERO

-- ================================================================

-- ================================================================


THE hero ISA ACTOR
	DEFINITE ARTICLE ""
	INDEFINITE ARTICLE ""
	MENTIONED "yourself"
	IS NOT sitting.
	IS NOT lying_down.

      CONTAINER
  	LIMITS	-- remove this whole section (until 'HEADER') if you wish to have no limits to
			--  how much the hero can carry.
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
				THEN LIST worn.
			END IF.
	END VERB.



END THE.


SYNONYMS
  me, myself, yourself, self = hero.





