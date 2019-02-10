-- ALAN Standard Library 1.10
-- Definitions (file name: 'definitions.i')

-- Included in this file:
	-- general attributes
	-- some article declarations
	-- common synonyms
	-- the definition_block class 
		-- attributes for the start section
		-- messages for the hero
		-- verb check messages 
		-- illegal parameter messages, used in SYNTAX definitions of verbs
		-- implicit taking message
	-- the start_section instance
	-- the hero instance (the player character; "me")



-- General attributes
-- ==================

-- We define general attributes for every thing ( = object or actor): 


ADD TO EVERY THING

	IS examinable. 
	   inanimate.
	   movable.  
	   reachable. 		
		-- See also 'distant' below
	   takeable.

	HAS matching_key null_key.   		
		-- All lockable objects need a matching key to lock/unlock them.
		-- "null_key" is a default dummy that can be ignored.
	HAS text "".	

	NOT broken.
	NOT closeable.
	NOT closed.
	NOT distant.		
		-- Usage: you can e.g. talk to a "not reachable" actor but not to a "distant" one.
		-- You can also throw things to or at a not reachable target but not to a distant one.
		-- Default response for not reachable things: "The [thing] is out of your reach."
		-- Default response for distant things: "The [thing] is too far away."
	NOT drinkable. 
	NOT edible.
	NOT fireable.		
		-- can be used as a firearm
	NOT lockable.
	NOT locked.
	NOT readable.   
	NOT scenery.		
		-- Has special responses for 'examine' and 'take', behaves normally otherwise.
	NOT wearable.
	NOT writeable.

	CAN NOT talk.		


-- We still define that plural nouns are preceded by "some" (and not "a" or "an"):

	INDEFINITE ARTICLE
		IF THIS IS plural
			THEN "some"
			ELSE "a"
		END IF.

END ADD TO.	


-- If you need "an", you should declare it separately at the instance, e.g.:

-- THE owl ISA ACTOR
--  	AT woods
--  	INDEFINITE ARTICLE "an"
-- END THE.


-- (We add the 'plural' attribute to the 'entity' class, because the plural 
-- applies not only to things but also to e.g. parameters in syntax statements; ignore.)


ADD TO EVERY ENTITY
	IS NOT plural.
END ADD TO.


-- Some weight attributes for things:


ADD TO EVERY THING
	IS weight 0.
END ADD TO THING.


ADD TO EVERY ACTOR 
	IS weight 50. 
END ADD TO ACTOR. 


ADD TO EVERY OBJECT
	IS weight 5.
END ADD TO OBJECT. 



-- Common synonyms
-- ===============


-- Next, we declare synonyms for some words so that it will be possible
-- for the player to type commands such as both "put ball in box" and 
-- "put ball into box", etc:


SYNONYMS 

into, inside = 'in'.
onto = on.
thru = through.
using = 'with'.




-- Attributes for the my_game definition block
-- ===========================================

-- Here, we create a class, "definition_block", to group various definitions under.
-- In the game source file, the author should declare an instance 'my_game' which belongs
-- to this class.



EVERY definition_block ISA LOCATION


	-- attributes for the start section:
	-- =================================

	HAS title "My New Game".		
    	HAS subtitle "".			     	
    	HAS author "An ALAN Author".   	
    	HAS year 2014.				
    	HAS version 1.					
    	
    	HAS intro_text "". 


	-- messages for the hero:
	-- ======================

	HAS hero_desc "".
	HAS hero_cont_header "You are carrying".
	HAS hero_cont_header_dark "Although you cannot see your belongings in the dark, 
		you recall that you're carrying".
	HAS hero_cont_else "You are empty-handed.".

	HAS hero_worn_header "You are wearing".
	HAS hero_worn_else "You're not wearing anything.".


	-- description message for dark locations:
	-- =======================================

	HAS dark_loc_desc "It is pitch black. You can't see anything at all.".
	


	-- all illegal parameter messages, typically found in the ELSE parts of SYNTAX structures and
	-- the first two below being by far the most common.
	-- ==========================================================================================


	-- the general message for when a parameter is not suitable with the verb:
	--------------------------------------------------------------------------

 	HAS illegal_parameter_sg "That's not something you can $v.".				-- (numerous)
	HAS illegal_parameter_pl "Those are not something you can $v.".


	-- variations of the above message when a preposition is required after the verb:
	---------------------------------------------------------------------------------

	HAS illegal_parameter_about_sg "That's not something you can $v about.".		-- ask_about, consult_about, tell_about, think_about
	HAS illegal_parameter_about_pl "Those are not something you can $v about.".
	HAS illegal_parameter_at_sg "That's not something you can $v at.".			-- fire_at
	HAS illegal_parameter_at_pl "Those are not something you can $v at.".
	HAS illegal_parameter2_at_sg "That's not something you can $v things at.".		-- throw_at
	HAS illegal_parameter2_at_pl "Those are not something you can $v things at.".
	HAS illegal_parameter_for_sg "That's not something you can $v for.".			-- ask_for
	HAS illegal_parameter_for_pl "Those are not something you can $v for.".
	HAS illegal_parameter2_from_sg "That's not something you can take things from.".	-- take_from
	HAS illegal_parameter2_from_pl "Those are not something you can take things from.".
	HAS illegal_parameter_in_sg "That's not something you can $v in.".			-- dive_in, jump_in, lie_in
	HAS illegal_parameter_in_pl "Those are not something you can $v in.".
	HAS illegal_parameter_on_sg "That's not something you can $v on.".			-- climb_on, knock, lie_on, switch_on, turn_on
	HAS illegal_parameter_on_pl "Those are not something you can $v on.".
	HAS illegal_parameter_off_sg "That's not something you can $v off.".			-- get_off, switch_off, turn_off
	HAS illegal_parameter_off_pl "Those are not something you can $v off.".
	HAS illegal_parameter_to_sg "That's not something you can $v to.".			-- listen_to, talk_to
	HAS illegal_parameter_to_pl "Those are not something you can $v to.".
	HAS illegal_parameter2_to_sg "That's not something you can $v things to.".		-- give, show, tell, tie_to, throw_to
	HAS illegal_parameter2_to_pl "Those are not something you can $v things to.".	
	HAS illegal_parameter_with_sg "That's not something you can $v with.".		-- kill_with, shoot_with, play_with
	HAS illegal_parameter_with_pl "Those are not something you can $v with.".	
	HAS illegal_parameter2_with_sg "That's not something you can $v things with.".	-- attack_with, break_with, burn_with, close_with, 
																 -- + cut_with, fill_with, lock_with, open_with, pry_with,
																 -- + push_with, unlock_with
	HAS illegal_parameter2_with_pl "Those are not something you can $v things with.".
		

	-- other illegal parameter messages:
	------------------------------------ 

	HAS illegal_parameter_examine_sg "That's not something you can examine.".			-- examine
	HAS illegal_parameter_examine_pl "Those are not something you can examine.".

	HAS illegal_parameter_look_out_sg "That's not something you can look out of.".		-- look_out_of  
	HAS illegal_parameter_look_out_pl "Those are not something you can look out of.".
	HAS illegal_parameter_look_through "You can't look through $+1.".				-- look_through  

	HAS illegal_parameter_obj "You can only $v objects.".							-- give, put, use, use_with

	HAS illegal_parameter_string "Please state inside double quotes ("""") what you want to $v.".	-- answer, say, write
	
	HAS illegal_parameter_talk_sg "That's not something you can talk to.".			-- ask, ask_for, say_to, tell
	HAS illegal_parameter_talk_pl "Those are not something you can talk to.".

	HAS illegal_parameter_there "It's not possible to $v there.".					-- look_behind, look_under, go
	HAS illegal_parameter2_there "It's not possible to $v anything there.".    		-- empty_in, empty_on, pour_in, pour_on, put_in,  
															    		 -- + put_on, put_against, put_behind, put_near, 
																	 -- + put_under, throw_in, write

	HAS illegal_parameter_what_sg "That's not something I know about.".				-- what_is, where_is
	HAS illegal_parameter_what_pl "Those are not something I know about.".			-- what_is, where_is
	HAS illegal_parameter_who_sg "That's not somebody I know about.".				-- who_is
	HAS illegal_parameter_who_pl "Those are not somebody I know about.".				-- who_is
		
	
	-- verb check messages, found before DOES sections of verbs and used mainly in verbs.i:	
	-- ====================================================================================


	-- a) attribute checks
	----------------------

		
	-- the general check message for when an instance cannot be used with the verb :
	--------------------------------------------------------------------------------	
		
	HAS check_obj_suitable_sg "That's not something you can $v.".				-- (numerous)				
	HAS check_obj_suitable_pl "Those are not something you can $v.".


	-- variations of the above message, needed e.g. when a preposition is required after the verb:
	----------------------------------------------------------------------------------------------

	HAS check_obj_suitable_at_sg "That's not something you can $v at.".			-- fire_at
	HAS check_obj_suitable_at_pl "Those are not something you can $v at.".
	HAS check_obj2_suitable_at_sg "That's not something you can $v things at.".	-- throw_at
	HAS check_obj2_suitable_at_pl "Those are not something you can $v things at.".
	HAS check_obj2_suitable_for_sg "That's not something you can $v for.".		-- ask_for
	HAS check_obj2_suitable_for_pl "Those are not something you can $v for.".
	HAS check_obj_suitable_off_sg "That's not something you can $v off.".			-- turn_off, switch_off
	HAS check_obj_suitable_off_pl "Those are not something you can $v off.".
	HAS check_obj_suitable_on_sg "That's not something you can $v on.".			-- knock, switch_on, turn_on
	HAS check_obj_suitable_on_pl "Those are not something you can $v on."	.	
	HAS check_obj_suitable_with_sg "That's not something you can $v with.".		-- play_with
	HAS check_obj_suitable_with_pl "Those are not something you can $v with.".		
	HAS check_obj2_suitable_with_sg "That's not something you can $v things with.".		-- break_with, burn_with, cut_with, fill_with, 
	HAS check_obj2_suitable_with_pl "Those are not something you can $v things with.".	 -- + lock_with, open_with, pry_with, push_with,
																	 -- + unlock_with

	HAS check_obj_suitable_examine_sg "That's not something you can examine.".		-- examine
	HAS check_obj_suitable_examine_pl "Those are not something you can examine.".	-- examine

	HAS check_obj_suitable_look_out_sg "That's not something you can look out of.".		-- look_out_of
	HAS check_obj_suitable_look_out_pl "Those are not something you can look out of.".			
	HAS check_obj_suitable_look_through "You can't look through $+1.".				-- look_through


	-- checks for open, closed and locked objects:
	----------------------------------------------

	HAS check_obj_closed_sg "$+1 is already open.".						-- open, open_with
	HAS check_obj_closed_pl "$+1 are already open.".
	HAS check_obj_not_closed1_sg "$+1 is already closed.".				-- close, close_with
	HAS check_obj_not_closed1_pl "$+1 are already closed.".
	HAS check_obj_not_closed2_sg "You can't, since $+1 is closed.".			-- empty, empty (in/on), look_in, pour (in/on)
	HAS check_obj_not_closed2_pl "You can't, since $+1 are closed.".
	HAS check_obj2_not_closed_sg "You can't, since $+2 is closed.".			-- empty_in, pour_in, put_in, take_from, throw_in
	HAS check_obj2_not_closed_pl "You can't, since $+2 are closed.".
	HAS check_obj_locked_sg "$+1 is already unlocked.".					-- unlock, unlock_with
	HAS check_obj_locked_pl "$+1 are already unlocked.".
	HAS check_obj_not_locked1_sg "$+1 is already locked.". 				-- lock, lock_with
	HAS check_obj_not_locked1_pl "$+1 are already locked.".
	HAS check_obj_not_locked2_sg "$+1 appears to be locked.".				-- open, open_with
	HAS check_obj_not_locked2_pl "$+1 appear to be locked.".


	-- checks for "not reachable" and "distant" objects:
	----------------------------------------------------

	HAS check_obj_reachable_sg "$+1 is out of your reach.".				-- (numerous)
	HAS check_obj_reachable_pl "$+1 are out of your reach.".
	HAS check_obj2_reachable_sg "$+2 is out of your reach.".				-- fill_with, put_in, tie_to			
	HAS check_obj2_reachable_pl "$+2 are out of your reach.".
	HAS check_obj_reachable_ask "$+1 can't reach $+2.".					-- ask_for
	HAS check_obj_not_distant_sg "$+1 is too far away.".					-- (numerous)
	HAS check_obj_not_distant_pl "$+1 are too far away.".
	HAS check_obj2_not_distant_sg "$+2 is too far away.".					-- empty_in, pour_in, put_in, throw_at, throw_in, throw_to
	HAS check_obj2_not_distant_pl "$+2 are too far away.".
	

	-- checks for the hero sitting or lying_down:
	---------------------------------------------

	HAS check_hero_not_sitting1 "It is difficult to $v while sitting down.".   		-- (with many intransitive verbs)
	HAS check_hero_not_sitting2 "It is difficult to $v anything while sitting down.".	-- (with many transitive verbs)
	HAS check_hero_not_sitting3 "It is difficult to $v anywhere while sitting down.".	-- (with many verbs of motion)
	HAS check_hero_not_sitting4 "You're sitting down already.".					-- sit, sit_on
	HAS check_hero_not_lying_down1 "It is difficult to $v while lying down.".			-- (with many intranstive verbs)
	HAS check_hero_not_lying_down2 "It is difficult to $v anything while lying down.".	-- (with many transitive verbs)
	HAS check_hero_not_lying_down3 "It is difficult to $v anywhere while lying down.".	-- (with many verbs of motion)
	HAS check_hero_not_lying_down4 "You're lying down already.".					-- lie_down, lie_in


	-- other attribute checks:
	--------------------------

	HAS check_act_can_talk_sg "That's not something you can talk to.".				-- ask, ask_for, say_to, tell
	HAS check_act_can_talk_pl "Those are not something you can talk to.". 
		
	HAS check_obj_broken_sg "That doesn't need fixing.".  						-- fix					
	HAS check_obj_broken_pl "Those don't need fixing.".

	HAS check_obj_inanimate1 "$+1 wouldn't probably appreciate that.".				-- push, push_with, scratch, search
	HAS check_obj_inanimate2 "You are not sure whether $+1 would appreciate that.".		-- rub, touch, touch_with
	
	HAS check_obj_movable "It's not possible to $v $+1.".							-- lift, pull, push
			
	HAS check_obj_not_scenery "Unimportant for your purposes, you decide to leave $+1 be.".	-- examine, take, take_from

	HAS check_obj_suitable_there "It's not possible to $v there.".					-- go, look_behind, look_under
	HAS check_obj2_suitable_there "It's not possible to $v anything there.".			-- tie_to

	HAS check_obj_takeable "You don't have $+1.".							-- (numerous; this check is in verbs with implicit taking)
	HAS check_obj2_takeable "You don't have $+2.".								-- fill_with
	
	HAS check_obj_weight_sg "$+1 is too heavy to $v.".							-- lift, take, take_from
	HAS check_obj_weight_pl "$+1 are too heavy to $v.".

	HAS check_obj_writeable "Nothing can be written there.".						-- write


	-- b) location and containment checks for actors and objects
	------------------------------------------------------------


	-- containment checks for actors other than the hero (checks for the hero are listed separately below):
	----------------------------------------------------------------------------------------------
	
	HAS check_act_near_hero "You don't quite know where $+1 went.  				-- follow
		You should state direction where you want to go.".

	HAS check_obj_in_act_sg "$+1 doesn't have $+2.".						-- take_from
	HAS check_obj_in_act_pl "$+1 don't have $+2.".
	HAS check_obj_not_in_act_sg "$+2 already has $+1.".						-- give
	HAS check_obj_not_in_act_pl "$+2 already have $+1.". 


	-- location and containment checks for the hero:
	------------------------------------------------

	HAS check_count_obj_in_cont "There is nothing in $+1.".						-- empty, empty_in, empty_on, pour, pour_in, pour_on
	HAS check_count_weapon_in_act "You are not carrying any firearms.".				-- shoot

	HAS check_hero_in_cont "But you aren't in $+1 at present.".					-- exit
	HAS check_hero_not_in_cont "But you are already in $+1!".						-- enter

	

	HAS check_obj_not_at_hero_sg "$+1 is right here.".  							-- find, follow, go_to
	HAS check_obj_not_at_hero_pl "$+1 are right here.".
	HAS check_obj_in_hero "You don't have the $+1.".							-- fire, put
	HAS check_obj2_in_hero "You don't have the $+2.".							-- (numerous)
	HAS check_obj_not_in_hero1 "It doesn't make sense to $v something you're holding.".  	-- e.g. attack, shoot
	HAS check_obj_not_in_hero2 "You already have $+1.".							-- ask_for, take, take_from
	HAS check_obj2_not_in_hero1 "You are carrying $+2.".   						-- e.g. throw_at, throw_to
	HAS check_obj2_not_in_hero2 "That would be futile.".							-- put_against, put_behind, put_near, put_under	


	-- checking whether an object is in a container or not:
	-------------------------------------------------------

	HAS check_obj_in_cont_sg "$+1 is not in $+2.".								-- take_from
	HAS check_obj_in_cont_pl "$+1 are not in $+2.".
	HAS check_obj_not_in_cont_sg "The $+1 is in $+2 already.".						-- put_in, throw_in
	HAS check_obj_not_in_cont_pl "The $+1 are in $+2 already.".
	HAS check_obj_not_in_cont2_sg "The $+1 is already full of $+2.".				-- fill_with
	HAS check_obj_not_in_cont2_pl "The $+1 is already full of $+2.".


	-- checking whether an object is on a surface or not:
	-----------------------------------------------------
	
	HAS check_obj_on_surface_sg "$+1 is not on $+2.".					-- take_from
	HAS check_obj_on_surface_pl "$+1 are not on $+2.".
	HAS check_obj_not_on_surface_sg "$+1 is already on $+2.".				-- empty_on, pour_on, put_on
	HAS check_obj_not_on_surface_pl "$+1 are already on $+2.".
	

	-- checking whether an object is worn or not:
	---------------------------------------------	

	HAS check_obj_in_worn "You are not wearing $+1.".    							-- e.g. take_off
	HAS check_obj_not_in_worn1 "You are already wearing $+1.".   					-- e.g. put_on, wear
    	HAS check_obj_not_in_worn2 "It doesn't make sense to $v something you're wearing.".	-- e.g. attack, shoot
	HAS check_obj_not_in_worn3_sg "You take off $+1 and carry it in your hands.".		-- remove, take 
	HAS check_obj_not_in_worn3_pl "You take off $+1 and carry them in your hands.".
    	

	-- c) checking location states
	------------------------------	

    	HAS check_current_loc_lit "It is too dark to see.".					-- (numerous)


	-- d) logical checks
	--------------------

	HAS check_obj_not_hero1 "It doesn't make sense to $v yourself.".
	HAS check_obj_not_hero2 "There is no need to be that desperate.".  			-- fire_at, kill, kill_with, shoot, shoot_with
	HAS check_obj_not_hero3 "That wouldn't accomplish anything.".				-- scratch, touch
	HAS check_obj_not_hero4 "You're right here.".							-- find, go_to
	HAS check_obj_not_hero5 "You don't have to be freed.".					-- free
	HAS check_obj_not_hero6 "There is no time for that now.".								-- kiss, rub, play_with
	HAS check_obj_not_hero7 "Turning your head, you notice nothing unusual behind yourself.".		-- look_behind 
	HAS check_obj_not_hero8 "You notice nothing unusual under yourself.".						-- look_under
	HAS check_obj_not_hero9 "You can't tie yourself to anything.".				-- tie_to
	HAS check_obj2_not_hero1 "That doesn't make sense.".
	HAS check_obj2_not_hero2 "That would be futile.".						-- put_against, put_behind, put_near, put_under
	HAS check_obj2_not_hero3 "You can't $v things to yourself.".				-- give, show, tie_to

	HAS check_obj_not_obj2_at "It doesn't make sense to $v something at itself.".		-- fire_at, throw_at
	HAS check_obj_not_obj2_from "It doesn't make sense to $v something from itself.".	-- take_from
	HAS check_obj_not_obj2_in "It doesn't make sense to $v something into itself.".		-- empty_in, pour_in, put_in, throw_in
	HAS check_obj_not_obj2_on "It doesn't make sense to $v something onto itself.".		-- empty_on, pour_on
	HAS check_obj_not_obj2_to "It doesn't make sense to $v something to itself.".		-- throw_to, tie_to
	HAS check_obj_not_obj2_with "It doesn't make sense to $v something with itself.".  	-- close_with, cut_with, fill_with lock_with, 
																	   -- + open_with, push_with, touch_with, 
																	   -- + unlock_with
	
	HAS check_obj_not_obj2_put "That doesn't make sense."	.					-- put_against, put_behind, put_near, put_under

	
    	-- e) additional checks for classes:
	------------------------------------

	HAS check_clothing_sex "On second thoughts you decide $+1 won't really suit you.".			-- clothing: wear
	HAS check_cont_not_supporter "You can't put $+1 inside $+2.".							-- supporter: put_in
	HAS check_device_on_sg "$+1 is already off.". 										-- device: turn_off, switch_off
	HAS check_device_on_pl "$+1 are already off.".
	HAS check_device_not_on_sg "$+1 is already on.". 									-- device: turn_on, switch_on
	HAS check_device_not_on_pl "$+1 are already on.".
	HAS check_door_matching_key "You can't use $+2 to $v $+1.".							-- door: lock_with, unlock_with	
	HAS check_lightsource_lit_sg "But $+1 is not lit.".									-- lightsource: extinguish, turn_off
	HAS check_lightsource_lit_pl "But $+1 are not lit.".
	HAS check_lightsource_not_lit_sg "$+1 is already lit.".								-- lightsource: light, turn_on
	HAS check_lightsource_not_lit_pl "$+1 are already lit.".
	HAS check_lightsource_switchable_sg "That's not something you can switch on and off."	.		-- lightsource: switch 
	HAS check_lightsource_switchable_pl "Those are not something you can switch on and off.".
	HAS check_obj_not_broken "Nothing happens.".										-- device, lightsource: light, turn_on


	-- messages for implicit taking:
	-- =============================

    	HAS implicit_taking_message "(taking $+1 first)$n".	

	-- The following verbs are preceded by implicit taking:
			-- bite, drink, eat, empty, empty_in, empty_on, give, pour, pour_in, pour_on,  
			-- put_in, put_on, throw, throw_at, throw_in, throw_to, tie_to.
	-- In ditransitive verbs, only the first parameter (the direct object) is taken implicitly.


END EVERY.




-- The start section:
-- ==================


THE start_section ISA definition_block

  	DESCRIPTION
   		SAY intro_text OF my_game.
   		
		"$p" STYLE alert. SAY title OF my_game. STYLE normal. 
   		
		IF subtitle OF my_game = ""
			THEN "$$" 
      		ELSE "$n" SAY subtitle OF my_game.
   		END IF.
   		
		"$n(C)" SAY year OF my_game. "by" SAY author OF my_game.
   		
		"$nProgrammed with the ALAN Interactive Fiction Language v3.0"
   		
		IF version OF my_game <> 0
			THEN "$nVersion" SAY version OF my_game. 
   		END IF.	
   		
		"$nAll rights reserved."

END THE start_section.



-- The my_game instance defined as a meta-location (ignore):
-- =========================================================


ADD TO EVERY LOCATION
	HAS sections {my_game}.
   	
	INITIALIZE
		EXCLUDE my_game FROM sections OF THIS.

	 	
				FOR EACH l ISA LOCATION
					DO
			  			IF l <> my_game 
							THEN LOCATE l AT my_game.
						END IF.
				END FOR.
		
				FOR EACH r1 ISA ROOM
					DO 
						LOCATE r1 AT indoor.		
				END FOR.

				FOR EACH s1 ISA SITE
					DO 
						LOCATE s1 AT outdoor.
				END FOR.

				FOR EACH l ISA LOCATION DO
  					IF sections OF l <> {} AND l <> my_game
						THEN
    			   				FOR EACH s IN sections OF l 
								DO
									IF l <> my_game AND s <> my_game
      									THEN LOCATE s AT l.
									END IF.	
    							END FOR.
  			 		END IF.
				END FOR.
	
	
		
		
END ADD TO.



-- All default runtime messages are listed below:
-- ==============================================


MESSAGE 
	AFTER_BUT: "You must give at least one object after '$1'."
	AGAIN: ""
	BUT_ALL: "You can only use '$1' AFTER '$2'."
	CAN_NOT_CONTAIN: "$+1 can not contain $+2."
	CANT0: "You can't do that."    
		 -- note that the fifth token in CANT0 is a zero, not an 'o'.
	CARRIES: "$+1 carries"
	'CONTAINS': "$+1 contains"
	CONTAINS_COMMA: "$01,"
    	CONTAINS_AND: "$01 and"
	CONTAINS_END: "$01." 
	EMPTY_HANDED: "$+1 is empty-handed."
	HAVE_SCORED: "You have scored $1 points out of $2."
    	IMPOSSIBLE_WITH: "That's impossible with $+1."
	IS_EMPTY: "$+1 is empty."
	MORE: "<More>"
	MULTIPLE: "You can't refer to multiple objects with '$v'."
	NO_SUCH: "I can't see any $1 here."
	NO_WAY: "You can't go that way."
	NOT_MUCH: "That doesn't leave much to $v!"
	NOUN: "You must supply a noun."
    	NOT_A_SAVEFILE: "That file does not seem to be an Alan game save file."
    	QUIT_ACTION: "Do you want to RESTART, RESTORE, QUIT or UNDO? "
		-- these four alternatives are hardwired to the interpreter and cannot be changed.
	REALLY: "Are you sure (RETURN confirms)?"
	RESTORE_FROM: "Enter file name to restore from"
	SAVE_FAILED: "Sorry, save failed."
	SAVE_MISSING: "Sorry, could not open the save file."	
	SAVE_NAME: "Sorry, the save file did not contain a save for this adventure."
   	SAVE_OVERWRITE: "That file already exists, overwrite (y)?"
	SAVE_VERSION: "Sorry, the save file was created by a different version."
	SAVE_WHERE: "Enter file name to save in"
	SEE_START: 
		IF parameter1 IS NOT plural
			THEN "There is $01"
			ELSE "There are $01"
		END IF.
	SEE_COMMA: ", $01"
	SEE_AND: "and $01"
	SEE_END: "here."
	NO_UNDO: "No further undo available."
	UNDONE: "'$1' undone."
	UNKNOWN_WORD: "I don't know the word '$1'."
	WHAT: "I don't understand."
    	WHAT_WORD: "I don't know what you mean by '$1'."
	WHICH_PRONOUN_START: "I don't know if you by '$1'"
	WHICH_PRONOUN_FIRST: "mean $+1"
	WHICH_START: "I don't know if you mean $+1"
	WHICH_COMMA: ", $+1"
	WHICH_OR: "or $+1."


-- The messages above follow the default formulations given in the manual, except for
--  AGAIN for which the original default is "(again)", and for
--  SEE_START for which the original default is "There is $01" (no plural formulation given).


-- The default messages are automatically translated into German or Swedish in your game 
-- if you define in the beginning of your source code
--
-- OPTIONS
--    LANGUAGE German.
-- 
-- or
--
-- OPTIONS 
-- 	LANGUAGE Swedish.
--
-- Only the exact equivalents of the default phrases will then be shown; if you change 
-- the above messages to your own wordings, the translations will still be only for the default. 
--
-- If you wish to write your game in some other language, you must manually translate
-- the messages above. There might be support for more languages in the future, depending 
-- on demand.




-- The hero
-- ========


THE hero ISA ACTOR 
	DEFINITE ARTICLE ""
	INDEFINITE ARTICLE ""
	MENTIONED "yourself"
	PRONOUN ''			
		-- to disable 'it' from referring to 'me'.
	CAN talk.
	IS NOT sitting.
	IS NOT lying_down.
	
	------------------------------------

	-- These three attributes, as well as the 'schedule' statement following them,
	-- are needed for the 'notify' command ('verbs.i'); ignore.

	HAS oldscore 0. 		
			-- Records previous score so 'checkscore' event
 			-- can compare with the current score 
	IS notify_on. 		
			-- Set by 'notify' verb, records whether 
			-- player wants to see score messages or not. 
	IS NOT seen_notify. 	
			-- Records whether player has seen the notify verb 
			-- instructions yet. 

	INITIALIZE
		SCHEDULE check_score AT hero AFTER 0.		

	------------------------------------


     CONTAINER

  	LIMITS	
			-- Remove this whole LIMITS section if you 
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


END THE.


SYNONYMS
	me, myself, yourself, self = hero.


-- Please refer also 'verbs.i' where there are numerous verb checks for the hero, 
-- so that the hero cannot e.g. attack himself etc.


-- end of file.