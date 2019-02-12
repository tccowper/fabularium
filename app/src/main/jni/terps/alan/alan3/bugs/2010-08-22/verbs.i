-- ALAN NEW LIBRARY: VERBS (file name: verbs.i)


----- This library file defines common verbs needed in gameplay. The verbs
----- are listed alphabetically. This file also includes common commands which are not
----- actually verbs, such as "inventory", "verbose" and "again". You are free to edit this
----- file for your own purposes in any way you like by adding, deleting or modifying verbs. 
----- Verbs originally defined in this file are the following:

----- VERB		SYNONYM					

----- about 	(+ help, info)
----- again 	(+ g)
----- answer 	(+ reply)
----- ask 		(+ enquire, inquire, interrogate)
----- attack 	(+ beat, fight, hit, kick, punch)
----- attack_with 
----- bite   	(+ chew)
----- break		(+ destroy)
----- break_with
----- brief 
----- burn 
----- burn_with
----- buy 		(+ purchase)
----- catch 
----- clean		(+ polish, wipe)
----- climb
----- climb_on
----- climb_through
----- close 
----- close_with
----- consult 
----- credits 	(+ acknowledgments, author, copyright)
----- cut
----- cut_with
----- dance	
----- dig
----- dive
----- dive_in
----- drink 	
----- drive
----- drop		(+ discard, dump, reject)
----- eat 		
----- empty
----- empty_in
----- empty_on
----- enter (+ obj)
----- examine	(+ check, inspect, observe, x)
-----	exit (+ obj)
----- extinguish	(+ put out, quench)
----- fill
----- fill_with
----- find		(+ locate)
----- fire	
----- flip	
----- follow
----- free 		(+ release)
----- get_up
----- get_off
----- give
----- go_to
----- hint		(+ hints)
----- inventory	(+ i, inv)
----- jump
----- jump_in
----- jump_on
----- kill 		(+ murder)
----- kill_with
----- kiss		(+ hug, embrace)
----- lie_down
----- lie_in
----- lie_on 
----- lift
----- light 	(+ lit)
----- listen0					(= 'listen', with no object)
----- listen 					(= 'listen to', with an object)			
----- lock
----- lock_with
----- look		(+ gaze, peek)
----- look_at
----- look_behind
----- look_in
----- look_out_of
----- look_through
----- look_under
----- look_up
----- no
----- notify (on, off) 				
----- open
----- open_with
----- play
----- play_with
----- pour
----- pour_in
----- pour_on
----- pray
----- pry
----- pry_with
----- pull
----- push
----- push_with
----- put 		(+ lay, place)
----- put_behind
----- put_down 							
----- put_in	(+ insert)
----- put_near
----- put_on
----- put_under
----- read
----- restart
----- restore
----- rub
----- save
----- say
----- say_to
----- score
----- search
----- sell
----- shake
----- shoot (at)
----- shoot_with
----- shout 	(+ scream, yell)
----- show		(+ reveal)
----- sing
----- sip
----- sit (down)
----- sit_on
----- sleep		(+ rest)
----- smell0					(= 'smell', with no object)
----- smell 					(= 'smell' with an object)
----- squeeze
----- stand (up)
----- stand_on
----- swim
----- swim_in
----- switch_on
----- switch_off
----- take		(+ carry, get, grab, hold, obtain)
----- take_from	(+ remove from)
----- talk
----- talk_to	(+ speak)
----- taste		(+ lick)
----- tear 		(+ rip)
----- tell		(+ enlighten, inform)
----- think
----- think_about	
----- throw
----- throw_at
----- tie
----- tie_to 
----- touch 	(+ feel)
----- turn		(+ rotate)
----- turn_on		
----- turn_off	
----- unlock
----- unlock_with
----- use
----- use_with
----- verbose
----- wait 		(+ z)
----- what_am_i
----- what_is
----- where_am_i
----- where_is
----- who_am_i
----- who_is
----- write
----- yes


----- Directions (north, south, up, etc.) are declared in the file 'locations.i'.


----- Verbs having to do with wearing clothes are defined in the file 'classes.i', 
----- subclass 'clothing'. These verbs, together with their synonyms, are:
-----
----- remove (+ doff, take off)
----- undress
----- wear (+ don, put on)




----- We first declare some default attributes for things (= objects and actors) below.
----- These attributes are frequently checked in verb definitions to prohibit
----- the use of verbs with objects and actors in an irrational way. For example, the hero
----- won't be able to eat anything that is not edible, and so forth.


----- Specific class attributes that override or complete the following 
----- attributes are declared in the files 'classes.i' and 'locations.i'.



ADD TO EVERY THING

	IS examinable. 
	   inanimate.
	   movable.  
	   reachable. 	
	   takeable.

	HAS text "".	

	NOT broken.
	NOT can_talk.
	NOT closeable.
	NOT closed.
	NOT drinkable. 
	NOT edible.
	NOT lit.
	NOT lockable.
	NOT locked.
	NOT plural.
	NOT readable.   
	NOT wearable.
	NOT writeable.

END ADD TO.


----- Next, we declare synonyms for some words so that it will be possible
----- for the player to type commands such as both "put ball in box" and 
----- "put ball into box", etc:


SYNONYMS 

into, inside = 'in'.
onto = on.
thru = through.
using = 'with'.



----- The verbs and commands:



-- =============================================================


----- ABOUT 
 

-- =============================================================


SYNTAX 'about' = 'about'.
	 

VERB 'about'
	DOES 
		STYLE emphasized.
		"This is a text adventure, also called interactive fiction, which means that what
		goes on in the story depends on what you type at the prompt. Commands you can type 
		are for example GO NORTH (or NORTH or just N), WEST, SOUTHEAST, UP, IN etc for 
		moving around, but you can try many
	      other things too, like TAKE LAMP, DROP EVERYTHING, EAT APPLE, EXAMINE BIRD or
		FOLLOW OLD MAN, to name just a few. LOOK (L) describes your surroundings, and 
		INVENTORY (I) lists what you are carrying. You can SAVE your game and RESTORE it 
		later on. 
		$pType CREDITS to see information about the author and the copyright issues.
		$pTo stop playing and end the program, type QUIT.$p"
		STYLE normal.
END VERB.


SYNONYMS help, info = 'about'.



-- =============================================================


----- AGAIN (= g)


-- =============================================================


SYNTAX again = again.


VERB again
	DOES
		"[The AGAIN command is not supported in this game. As a workaround, try using
		 the 'up' and 'down' arrow keys to scroll through your previous commands.]" 
END VERB.


SYNONYMS g = again.



-- =============================================================


----- ASK (= enquire, inquire, interrogate)


-- =============================================================


SYNTAX ask = ask (act) about (topic)!
    		WHERE act ISA ACTOR
      		ELSE "That's not something you can talk to."
    		AND topic ISA THING
      		ELSE "That doesn't seem to be something you can talk about with" SAY THE act. "."


ADD TO EVERY THING
  VERB ask
    WHEN act
      CHECK act HAS can_talk
        	ELSE "That's not something you can talk to."
	AND act IS reachable			-- you might want to remove this check in some situations, e.g.
							-- when an NPC (= a non-player character) is speaking 
							-- on the phone with the hero
		ELSE SAY THE act. "is too far away."
	AND act <> hero
		ELSE "It doesn't make much sense to ask yourself about something."
      DOES
		IF topic IN act
			THEN SAY THE act. "doesn't seem to want to talk about" SAY THE topic. "."
	      ELSIF topic = act
			THEN SAY THE act. "chooses to be silent."
		ELSIF topic = hero
			THEN """I think you know more about yourself than what I do!""," SAY THE act. "snaps."
		ELSE """I don't know anything about" SAY THE topic. "$$!""," SAY THE act. "snaps."
		END IF.
  END VERB.
END ADD TO.


SYNONYMS enquire, inquire, interrogate = ask.


----- note that 'consult' is defined separately



-- =============================================================


----- ATTACK (+ beat, fight, hit, punch)


-- =============================================================


SYNTAX attack = attack (obj)
    		WHERE obj ISA THING
      		ELSE "That’s not something you can $v."


ADD TO EVERY THING
  VERB attack
	CHECK obj IS examinable
		ELSE "That's not something you can $v."
	AND obj <> hero 
		ELSE "It doesn't make sense to $v yourself."
	AND obj NOT IN hero
		ELSE "It doesn't make much sense to $v something you're holding."
	AND obj NOT IN worn
		ELSE "It doesn't make sense to $v something you're wearing."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is too far away."
	AND hero IS NOT sitting
		ELSE "It is difficult to attack anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to attack anything while lying down."
    	DOES "Resorting to violence is not the answer."
  END VERB.
END ADD TO.


SYNONYMS beat, fight, hit, punch = attack.
 
-- Note that 'kick' is defined separately, to avoid absurd commands such as
-- 'kick man with sword' (see 'attack_with' below)



-- ==============================================================


----- ATTACK WITH


-- ==============================================================


SYNTAX attack_with = attack (obj1) 'with' (obj2)
    		WHERE obj1 ISA THING
      		ELSE "That's not something you can $v."
    		AND obj2 ISA WEAPON
     			ELSE 
				IF obj2 ISA ACTOR
		 			THEN 
						IF obj2 = hero
							THEN "It doesn't make sense to attack something with yourself."
							ELSE "You cannot use" SAY THE obj2. "to attack anything."
						END IF.
					ELSE "There's no point attacking anything with" SAY THE obj2. "."
				END IF.


ADD TO EVERY THING
  VERB attack_with
    WHEN obj1
	CHECK obj1 IS examinable
	  	ELSE "That's not something you can $v."
	AND obj2 IS takeable
		ELSE "You don't have" SAY THE obj2. "."
	AND obj1 <> hero 
		ELSE "It doesn't make sense to $v yourself."
	AND obj1 NOT IN hero
		ELSE "It doesn't make much sense to $v something you're holding."
	AND obj1 NOT IN worn
		ELSE "It doesn't make sense to $v something you're wearing."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND hero IS NOT sitting
		ELSE "It is difficult to attack anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to attack anything while lying down."
      DOES "Resorting to violence is not the answer."
  END VERB.
END ADD TO.



-- ===============================================================


----- BITE 	(+ chew, taste)
 

-- ===============================================================


SYNTAX bite = bite (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can bite."


ADD TO EVERY OBJECT
  VERB bite
	CHECK obj IS edible
		ELSE "That's not something you should $v."
	AND obj IS takeable
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES 
		-- This if-statement takes care of implicit taking; i.e. if the hero
		-- doesn't have the object, (s)he will take it automatically first.
		-- This same if-statement is found in numerous other verbs throughout 
		-- the library, as well.

		IF obj NOT DIRECTLY IN hero	
			THEN  "(taking" SAY THE obj. "first)$n"
				LOCATE obj IN hero.
		END IF.

		
		"You take a bite of" SAY THE obj. "$$. It tastes rather good."
	
  END VERB.
END ADD TO.


SYNONYMS chew = bite.



-- ===============================================================


----- BREAK


-- ===============================================================


SYNTAX break = break (obj)
		WHERE obj ISA OBJECT
			ELSE 
				IF obj ISA ACTOR
					THEN "Resorting to violence is not the answer."
					ELSE "That's not something you can $v."
				END IF.


ADD TO EVERY OBJECT
  VERB break
	CHECK obj IS examinable
		ELSE "That's not something you can break."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable 
		ELSE SAY THE obj. "is out of your reach."
	
	DOES
		"Resorting to violence is not the answer."
  END VERB.
END ADD TO.


SYNONYMS destroy = break.




-- ===============================================================


----- BREAK WITH


-- ===============================================================


SYNTAX break_with = break (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "That's not something you can $v."
		AND obj2 ISA OBJECT
			ELSE "That's not something you can $v things with."


ADD TO EVERY OBJECT
   VERB break_with
	WHEN obj1
	CHECK obj1 IS examinable
		ELSE "That's not something you can break."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	DOES		
		"Trying to break" SAY THE obj1. "with" SAY THE obj2. 
		"wouldn't accomplish anything."
   END VERB.
END ADD TO.



-- ================================================================


----- BRIEF


-- ================================================================


-- Use "Visits 0." or "Visits 1000." in the START section if you want
-- the game to start in verbose or brief mode. (By default,
-- all games start in the verbose mode.)


SYNTAX brief = brief.


VERB brief
	DOES
		Visits 1000.
		"Brief mode is now on. Location descriptions will only be shown
		the first time you visit."
END VERB.



-- =================================================================


----- BURN


-- =================================================================


SYNTAX burn = burn (obj)
		WHERE obj ISA OBJECT
			ELSE 
				IF obj ISA ACTOR
					THEN "That would be needlessly brutal."
					ELSE "That's not something you can burn."
				END IF.


ADD TO EVERY OBJECT
  VERB burn
	CHECK obj IS examinable
		ELSE "That's not something you can burn."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"You must state what you want to burn" SAY THE obj. "with."
  END VERB.
END ADD TO.



-- =================================================================


----- BURN WITH


-- =================================================================


SYNTAX burn_with = burn (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE 
				IF obj1 ISA ACTOR
					THEN "That would be needlessly brutal."
					ELSE "That's not something you can burn."
				END IF.
		AND obj2 ISA OBJECT
			ELSE 
				IF obj2 ISA ACTOR
					THEN "It doesn't make sense to burn something with" SAY THE obj2. "."
					ELSE "It's not possible to burn something with that."
				END IF.


ADD TO EVERY THING
  VERB burn_with
	WHEN obj1
	CHECK obj1 IS examinable
		ELSE "That's not something you can burn."
	AND obj1 <> obj2 
		ELSE "It doesn't make sense to burn something with itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	DOES
		"You can't burn" SAY THE obj1. "with" SAY THE obj2. "."
  END VERB.
END ADD TO.



-- ==================================================================


----- BUY (+ purchase)


-- ==================================================================


SYNTAX buy = buy (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can $v."


ADD TO EVERY OBJECT
  VERB buy
	CHECK obj IS examinable
		ELSE "That's not something you can buy."
	DOES
		"That's not for sale."
  END VERB.
END ADD TO.


SYNONYMS purchase = buy.



-- ==================================================================


----- CATCH


-- ==================================================================


SYNTAX catch = catch (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can catch."


ADD TO EVERY THING
  VERB catch
	CHECK obj <> hero 
		ELSE "It doesn't make sense to $v yourself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to catch anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to catch anything while lying down."
	DOES
	      "That doesn't need to be caught."
  END VERB.
END ADD TO.



-- ==================================================================


----- CLEAN ( + wipe, polish)


-- ==================================================================


SYNTAX clean = clean (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can $v."


ADD TO EVERY OBJECT
  VERB clean
	CHECK obj IS examinable
		ELSE "That's not something you can $v."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"Nothing would be achieved by that."
  END VERB.
END ADD TO.


SYNONYMS wipe, polish = clean.


----- notice that 'rub' is defined separately



-- ==============================================================


----- CLIMB


-- ==============================================================


SYNTAX climb = climb (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can climb."


ADD TO EVERY OBJECT
  VERB climb
	CHECK obj IS examinable
		ELSE "That's not something you can climb."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND hero IS NOT sitting
		ELSE "It is difficult to climb while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to climb while lying down."
	DOES 
		"That's not something you can climb."
  END VERB.
END ADD TO.



-- ==============================================================


----- CLIMB ON


-- ==============================================================


SYNTAX climb_on = climb 'on' (obj)
		WHERE obj ISA SUPPORTER
			ELSE "That's not something you can climb on."


ADD TO EVERY OBJECT
  VERB climb_on
	CHECK obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to climb anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to climb anywhere while lying down."
	DOES 
		"That's not something you can climb on."
  END VERB.
END ADD TO.



-- ==============================================================


----- CLIMB THROUGH


-- ==============================================================


SYNTAX climb_through = climb through (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can climb through."


ADD TO EVERY OBJECT
  VERB climb_through
	CHECK obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to climb anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to climb anywhere while lying down."
	DOES 
		"That's not something you can climb through."
  END VERB.
END ADD TO.



-- ==============================================================


----- CLOSE (+ shut)


-- ==============================================================


SYNTAX close = close (obj)
        	WHERE obj ISA OBJECT
	    		ELSE "That's not something you can $v."


ADD TO EVERY OBJECT
  VERB close
	CHECK obj IS closeable
	    ELSE "That's not something you can close."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS NOT closed
	    ELSE "It is already closed."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
	    MAKE obj closed.
	    "You close the" SAY THE obj. "."
  END VERB.
END ADD TO.


SYNONYMS shut = close.



-- ==============================================================


----- CLOSE WITH


-- ==============================================================


SYNTAX close_with = close (obj1) 'with' (obj2)
        	WHERE obj1 ISA OBJECT
	    		ELSE "That's not something you can $v."
	  	AND obj2 ISA OBJECT
	    		ELSE "You can't $v anything with that."


ADD TO EVERY OBJECT
  VERB close_with
    WHEN obj1
	CHECK obj1 IS closeable
		ELSE "That's not something you can $v."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS NOT closed
	    ELSE "It is already closed."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	DOES
	    "You can't $v" SAY THE obj1. "with" SAY THE obj2. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- CONSULT


-- ==============================================================


SYNTAX consult = consult (obj1) about (obj2)!
		WHERE obj1 ISA THING
			ELSE "That's not something you can consult."
		AND obj2 ISA THING
			ELSE "That's not something you can find information about."
	

       consult = 'look' 'up' (obj2) 'in' (obj1).


ADD TO EVERY THING
  VERB consult
    WHEN obj1
	CHECK obj1 IS examinable
		ELSE "That's not something you can consult."
	AND obj1 <> hero 
		ELSE "It doesn't make sense to consult yourself about anything."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	DOES 
		IF obj1 ISA OBJECT
			THEN "You find nothing useful about" SAY THE obj2. "in" SAY THE obj1. "."
			ELSE SAY THE obj1. "chooses to be silent on that subject."
		END IF.
  END VERB.
END ADD TO.


--- another 'consult' formulation added to guide players to use the right phrasing:


SYNTAX consult_error = consult (obj)
	WHERE obj ISA THING
		ELSE "That's not something you can consult."


ADD TO EVERY THING
  VERB consult_error
	DOES "To consult something, use the formulation CONSULT PERSON/THING ABOUT PERSON/THING."	
  END VERB.
END ADD TO.



-- ==============================================================


----- CREDITS (+ acknowledgments, author, copyright)


-- ==============================================================


SYNTAX credits = credits.


VERB credits
	DOES
		"The author retains the copyright to this game.
		$pThis game was written using the ALAN Adventure Language. ALAN is 
		an interactive fiction authoring system by Thomas Nilsson.
		$nE-mail address: thomas.nilsson@progindus.se $pFurther information 
		about the ALAN system can be obtained from
		the World Wide Web Internet site
		$ihttp://www.alanif.se$p"
END VERB.


SYNONYMS acknowledgments, author, copyright = credits.



-- ==============================================================


----- CUT


-- ==============================================================


SYNTAX cut = cut (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can cut."


ADD TO EVERY OBJECT
  VERB cut
	DOES "You need to specify what you want to cut" SAY THE obj. "with."
  END VERB.
END ADD TO.



-- ==============================================================


----- CUT WITH


-- ==============================================================


SYNTAX cut_with = cut (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "That's not something you can cut."
		AND obj2 ISA OBJECT
			ELSE "That's not something you can cut with."


ADD TO EVERY OBJECT
  VERB cut_with
    WHEN obj1
	CHECK obj1 <> obj2
		ELSE "You can't cut something with itself."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	DOES 
		"You can't cut" SAY THE obj1. "with" SAY THE obj2. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- DANCE


-- ==============================================================


SYNTAX dance = dance.


VERB dance
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to dance while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to dance while lying down."
  DOES
    	"How about a waltz?"
END VERB.



-- ==============================================================


----- DIG


-- ==============================================================


SYNTAX dig = dig (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can dig."


ADD TO EVERY OBJECT
  VERB dig
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to dig anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to dig anything while lying down."
	DOES 
		"There is nothing suitable to dig here."
  END VERB.
END ADD TO.



-- ==============================================================


----- DIVE


-- ==============================================================


SYNTAX dive = dive.


VERB dive 	
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to dive anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to dive anywhere while lying down."
	
	DOES 
		"There is no water suitable for swimming here."
END VERB.



-- ==============================================================


----- DIVE IN


-- ==============================================================


SYNTAX dive_in = dive 'in' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can dive into."


ADD TO EVERY OBJECT
  VERB dive_in
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to dive anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to dive anywhere while lying down."
	
	DOES 
		"That's not something you can dive into."
  END VERB.
END ADD TO.



-- ==============================================================


----- DRINK 


-- ==============================================================


SYNTAX drink = drink (obj)
		WHERE obj ISA LIQUID		-- see 'classes.i'
			ELSE "That's not something you can drink."


ADD TO EVERY LIQUID
  VERB drink
	CHECK obj IS drinkable
		ELSE "That's not something you can drink."
	AND obj IS takeable
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		-- implicit taking:
		IF vessel OF obj NOT DIRECTLY IN hero
			THEN 
				IF vessel OF obj = zero_vessel OR vessel OF obj IS NOT takeable
					THEN "You can't carry" SAY THE obj. "around in your bare hands."
					ELSE LOCATE vessel OF obj IN hero.
						"(taking" SAY THE vessel OF obj. "first)$n"
				END IF.
		END IF.
		-- end of implicit taking.
		
		IF obj IN hero 		-- i.e. if the implicit taking was successful
			THEN
				"You drink all of" SAY THE obj. "."
				LOCATE obj AT nowhere.
		END IF.

  END VERB.
END ADD TO.



-- ==============================================================


----- DRIVE


-- ==============================================================


SYNTAX drive = drive (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can drive."


ADD TO EVERY OBJECT
  VERB drive
	DOES 
		"That's not something you can drive."
  END VERB.
END ADD TO.


-- another 'drive' formulation added to guide players to use the right phrasing:


SYNTAX drive_error = drive.


VERB drive_error
	DOES "To drive something, use the phrasing DRIVE SOMETHING."
END VERB.



-- ==============================================================


----- DROP


-- ==============================================================


SYNTAX drop = drop (obj)*
		WHERE obj ISA OBJECT
			ELSE "That's not something you can $v."
  	
	
	drop = put (obj) * down.
  
	
	drop = put down (obj)*.


ADD TO EVERY OBJECT
  VERB drop
   	 CHECK obj IN hero
      	ELSE "You don't have" SAY THE obj. "."
    	DOES
      	LOCATE obj HERE.
      	"Dropped."
  END VERB.
END ADD TO.


SYNONYMS
  discard, dump, reject = drop.



-- ==============================================================


----- EAT 


-- ==============================================================


SYNTAX eat = eat (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can eat."


ADD TO EVERY OBJECT
  VERB eat
	CHECK obj IS edible
		ELSE "That's not something you can eat."
	AND obj IS takeable
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		-- implicit taking:
		IF obj NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj. "first)$n"
				LOCATE obj IN hero.
		END IF.
		-- end of implicit taking.
		
		"You eat all of" SAY THE obj. "."
		LOCATE obj AT nowhere.

  END VERB.
END ADD.



-- ==============================================================


----- EMPTY


-- ==============================================================



SYNTAX 'empty' = 'empty' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can $v."
		AND obj ISA CONTAINER
			ELSE "You can only empty containers."


ADD TO EVERY OBJECT
  VERB 'empty'
	CHECK obj IS takeable
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS NOT closed
		ELSE "You can't, since" SAY THE obj. "is closed."
	DOES 
		-- implicit taking:
		IF obj NOT DIRECTLY IN hero
			THEN "(taking" SAY THE obj. "first)$n"
				LOCATE obj IN hero.
		END IF.
		-- end of implicit taking.

		IF COUNT ISA OBJECT, IN obj = 0
			THEN "There is nothing in" SAY THE obj.
			ELSE 	"You $v the contents of" SAY THE obj.
					IF floor HERE
						THEN "on the floor."
						ELSE "on the ground."
					END IF.
				EMPTY obj AT hero.
		END IF.
  END VERB.
END ADD TO.



-- ==============================================================


----- EMPTY IN		


-- ==============================================================



SYNTAX empty_in = 'empty' (obj1) 'in' (obj2)
	WHERE obj1 ISA OBJECT
		ELSE "That's not something you can empty."
	AND obj1 ISA CONTAINER
		ELSE "That's not something you can empty."
	AND obj2 ISA OBJECT					
		ELSE "That's not something you can empty things into."
	AND obj2 ISA CONTAINER
		ELSE SAY THE obj2. "is not something you can empty things into."


ADD TO EVERY OBJECT
VERB empty_in
   WHEN obj1
	CHECK obj1 <> obj2
		ELSE "It doesn't make sense to $v something into itself."
	AND obj1 IS takeable
		ELSE "You don't have" SAY THE obj1. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 NOT DIRECTLY IN obj2
		ELSE SAY THE obj1. "is already in" SAY THE obj2. "."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IS reachable
		ELSE SAY THE obj2. "is out of your reach."
	AND obj1 IS NOT closed
		ELSE "You can't, since" SAY THE THIS. "is closed." 
	AND obj2 IS NOT closed
		ELSE "You can't, since" SAY THE obj2. "is closed."
	DOES
		-- implicit taking:
		IF obj1 NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj1. "first)$n"
				LOCATE obj1 IN hero.
		END IF.
		-- end of implicit taking.
			
		IF COUNT ISA OBJECT, IN obj1 = 0
			THEN "There is nothing in" SAY THE obj1. "."
		ELSE EMPTY obj1 IN obj2.
			"You empty the contents of" SAY THE obj1. 
			"into" SAY THE obj2. "."	
		END IF.
END VERB.
END ADD TO.



-- ==============================================================


----- EMPTY ON		


-- ==============================================================



SYNTAX empty_on = 'empty' (obj1) 'on' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "That's not something you can empty."
		AND obj1 ISA CONTAINER
			ELSE "That's not something you can empty."
		AND obj2 ISA OBJECT
			ELSE 
				IF obj2 ISA ACTOR
					THEN 
						IF obj2 = hero 
							THEN "That wouldn't make sense."
							ELSE	"That wouldn't be polite."
						END IF.
					ELSE "You can't empty anything onto" SAY THE obj2. "."
				END IF.
		AND obj2 ISA CONTAINER
			ELSE SAY THE obj2. "is not something you can empty things onto."



ADD TO EVERY OBJECT
VERB empty_on
   WHEN obj1
	CHECK obj1 <> obj2
		ELSE "It doesn't make sense to $v something on itself."
	AND obj1 IS takeable
		ELSE "You don't have" SAY THE obj1. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IS reachable
		ELSE SAY THE obj2. "is out of your reach."
	AND obj1 IS NOT closed
		ELSE "You can't, since" SAY THE obj1. "is closed."
	AND obj2 IS NOT closed
		ELSE "You can't, since" SAY THE obj2. "is closed."
	DOES
		-- implicit taking:
		IF obj1 NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj1. "first)$n"
				LOCATE obj1 IN hero.
		END IF.
		-- end of implicit taking.
			
		IF COUNT ISA OBJECT, IN obj1 = 0
			THEN "There is nothing in" SAY THE obj1. "."
		ELSE 
				IF obj2 = floor OR obj2 = ground 
					THEN EMPTY obj1 AT hero.
						"You empty the contents of" SAY THE obj1. "on" SAY THE obj2. "."
				ELSIF obj2 ISA SUPPORTER
					THEN EMPTY obj1 IN obj2.
						"You empty the contents of" SAY THE obj1. "on" SAY THE obj2. "."
				ELSE "It wouldn't be sensible to empty anything on" SAY THE obj2.
				END IF.
		END IF.
END VERB.
END ADD TO.



-- ==============================================================


----- ENTER (+ obj)


-- ==============================================================


SYNTAX enter = enter (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can enter."
      	AND obj ISA CONTAINER
			ELSE "That's not something you can enter."


ADD TO EVERY OBJECT
  VERB enter
	 CHECK hero IS NOT sitting
		ELSE "It is difficult to enter anything while sitting down."
	 AND hero IS NOT lying_down
		ELSE "It is difficult to enter anything while lying down."
   	 DOES 
		"That's not something you can enter."
  END VERB.
END ADD TO.


--- another 'enter' formulation declared to guide players to use the right formulation:


SYNTAX enter_error = enter.


VERB enter_error
	DOES "You must state what you want to enter."
END VERB. 



-- ==============================================================


----- EXAMINE (+ look at)


-- ==============================================================


SYNTAX examine = examine (obj) 
		WHERE obj ISA THING
			ELSE "That's not something you can examine."


SYNTAX examine = 'look' 'at' (obj).
	 examine = 'look' (obj).			-- note that this formulation is allowed, too


ADD TO EVERY THING
  VERB examine
    	CHECK obj IS examinable
      	ELSE "That's not something you can examine."
    	AND CURRENT LOCATION IS lit
		ELSE "You can't see anything in the dark."
    	DOES
		IF obj IS readable
			THEN 
				IF text OF obj = ""
					THEN "There is nothing written on" SAY THE obj. "."
					ELSE "You read" SAY THE obj. ". It says ""$$" SAY text OF obj. "$$""." 
				END IF.
      		ELSE "There is nothing special about" SAY THE obj. "." 
		END IF.
  END VERB.
END ADD TO.


SYNONYMS
	'check', inspect, observe, x = examine.



-- ==============================================================


----- EXIT (obj)


-- ==============================================================


SYNTAX 'exit' = 'exit' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can exit."
      	AND obj ISA CONTAINER
			ELSE "That's not something you can exit."


ADD TO EVERY OBJECT
  VERB 'exit'
	CHECK hero IN obj
		ELSE 
			IF obj = current_room
				THEN "You must state a direction where to go."
				ELSE "But you aren't in" SAY THE obj. "!"	
			END IF.
	DOES 
		"You exit" SAY THE obj. "."
		LOCATE hero AT CURRENT LOCATION.
  END VERB.
END ADD TO.


--- another 'exit' formulation added to guide players to use the right formulation:


SYNTAX exit_error = 'exit'.


VERB exit_error
	DOES 
		"You must state what you want to exit."
END VERB.



-- ==============================================================


----- EXTINGUISH	(+ put out)


-- ==============================================================


SYNONYMS quench = extinguish.


SYNTAX extinguish = extinguish (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can extinguish."
       extinguish = put 'out' (obj).


ADD TO EVERY OBJECT
  VERB extinguish
	DOES 
		"That's not on fire."
  END VERB.
END ADD TO.



-- ==============================================================


----- FILL


-- ==============================================================


SYNTAX fill = fill (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can fill."
		AND obj ISA CONTAINER
			ELSE "That's not something you can fill."


ADD TO EVERY OBJECT
  VERB fill
	DOES 
		"You have to say what you want to fill" SAY THE obj. "with."
  END VERB.
END ADD TO.



-- ==============================================================


----- FILL WITH


-- ==============================================================


SYNTAX fill_with = fill (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "That's not something you can fill."
		AND obj1 ISA CONTAINER
			ELSE "That's not something you can fill."
		AND obj2 ISA OBJECT
			ELSE "It's not possible to fill something with that."


ADD TO EVERY OBJECT
  VERB fill_with
    WHEN obj1
	CHECK obj1 <> obj2
		ELSE "It doesn't make sense to fill something with itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj2 NOT IN obj1
		ELSE SAY THE obj1. "is already full of" SAY obj2. "."
	AND obj1 IS reachable
		ELSE SAY THE obj2. "is out of your reach."
	DOES 
		-- "That wouldn't accomplish anything."
	
		"You fill" SAY THE obj1. "with" SAY obj2. "."
		LOCATE obj2 IN obj1.
  END VERB.
END ADD TO.



-- ==============================================================


----- FIND


-- ==============================================================


SYNTAX
	find = find (obj)!
		WHERE obj ISA THING
			ELSE "That's not something you need to find."


ADD TO EVERY THING
  VERB find
	CHECK obj <> hero 
		ELSE "You're right here!"
	AND CURRENT LOCATION IS lit
		ELSE "It's too dark to find anything here."
	AND obj NOT HERE
		ELSE "The" SAY obj. "is right here!"
	DOES
		"You'll have to find it yourself."
  END VERB.
END ADD TO.


SYNONYMS 'locate' = find.



-- ==============================================================


----- FIRE


-- ==============================================================


SYNTAX fire = fire (obj)
		WHERE obj ISA WEAPON
			ELSE "That's not something you can fire."


ADD TO EVERY OBJECT
  VERB fire
	CHECK obj IS fireable
		ELSE "That's not something you can fire."
	AND obj IN hero
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"You fire" SAY the obj. "into the air."
  END VERB.
END ADD TO.



-- ==============================================================


----- FIRE AT


-- ==============================================================


SYNTAX fire_at = fire (obj1) 'at' (obj2)
		WHERE obj1 ISA WEAPON
			ELSE "That's not something you can fire."
		AND obj2 ISA THING
			ELSE "That's not something you can shoot at."


ADD TO EVERY OBJECT
  VERB fire_at
    WHEN obj1
	CHECK obj1 IS fireable
		ELSE "That's not something you can fire."
	AND obj1 IN hero
		ELSE "You don't have" SAY THE obj1. "."
	AND obj2 <> hero 
		ELSE "There's no need to be that desperate."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"Resorting to violence is not the answer."
  END VERB.
END ADD TO.


-- another formulation added:


SYNTAX fire_at_error = fire 'at' (obj)
	WHERE obj ISA THING
		ELSE "That's not something you can fire at."


ADD TO EVERY THING
VERB fire_at_error
	CHECK COUNT ISA WEAPON, IS fireable, IN hero > 0
		ELSE "You are not holding any firearm."
	AND obj <> hero
		ELSE "There's no need to be that desperate."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"Resorting to violence is not the answer."
END VERB.
END ADD TO.



-- ==============================================================


----- FIX (mend, repair)


-- ==============================================================


SYNTAX
	fix = fix (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can $v."


ADD TO EVERY OBJECT
  VERB fix
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS broken
		ELSE "The" SAY obj. "doesn't need fixing."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"Please be more specific. How do you intend to fix it?"
  END VERB.
END ADD TO.


SYNONYMS mend, repair = fix.



-- ==============================================================


----- FLIP


-- ==============================================================


SYNTAX flip = flip (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can flip."


ADD TO EVERY OBJECT
  VERB flip 
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES ONLY 
		"That's not something you need to flip."
  END VERB.
END ADD TO.



-- ==============================================================


----- FOLLOW


-- ==============================================================


SYNTAX follow = follow (obj)!
		WHERE obj ISA THING
			ELSE "That's not something you can follow."


ADD TO EVERY ACTOR
  VERB follow
	CHECK obj <> hero
		ELSE "It doesn't make sense to follow yourself." 
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj NOT AT hero
		ELSE SAY THE obj. "is right here."
	AND hero IS NOT sitting
		ELSE "It is difficult to follow anybody while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to follow anybody while lying down."
	AND obj NOT NEAR hero					
		ELSE "You follow" SAY THE obj. "."
			LOCATE hero AT obj.
	DOES 
		"You don't quite know where" SAY THE obj. "went. You must state a direction 
		where you want to go."
  END VERB.
END ADD TO.



-- ==============================================================


----- FREE (+ release)


-- ==============================================================


SYNTAX free = free (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can $v."


ADD TO EVERY THING
  VERB free
	CHECK obj IS examinable
		ELSE "That's not something you can $v."
	AND obj <> hero 
		ELSE "You don't need to be freed at present."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "out of your reach."
	DOES 
		"That doesn't need to be $vd."
  END VERB.
END ADD TO.


SYNONYMS release = free.



-- ==============================================================


------ GET OFF


-- ==============================================================


SYNTAX get_off = get off (obj)
	WHERE obj ISA SUPPORTER
		ELSE "That's not something you can get off."


ADD TO EVERY OBJECT
  VERB get_off
	DOES
		IF hero IS sitting OR hero IS lying_down
			THEN "You get off" SAY THE obj. "."
				MAKE hero NOT lying_down.
				MAKE hero NOT sitting.
			ELSE "You're standing up already."
		END IF.
  END VERB.
END ADD TO.


-- ==============================================================


------ GET UP


-- ==============================================================


SYNTAX get_up = get up.		
		

VERB get_up
	DOES
		IF hero IS sitting 
			THEN "You stand up."
				MAKE hero NOT sitting.
		ELSIF hero IS lying_down
			THEN "You get up."
				MAKE hero NOT lying_down.
		ELSE "You're standing up already."
		END IF.
END VERB.



-- ==============================================================


----- GIVE (+ hand, offer)


-- ==============================================================


SYNTAX give = 'give' (obj1) 'to' (obj2)
    		WHERE obj1 ISA OBJECT
      		ELSE "You can only give away objects."
    		AND obj2 ISA ACTOR
      		ELSE "That's not something you can give things to."
  	 

	 give = give (obj2) (obj1).


ADD TO EVERY OBJECT
  VERB give
    WHEN obj1
	CHECK obj1 IS takeable
		ELSE "You don't have" SAY THE obj1. "."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to give something to itself."
	AND obj2 <> hero
		ELSE "It doesn't make sense to give something to yourself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj2 IS reachable
		ELSE SAY THE obj2. "is too far away."
	AND obj1 NOT IN obj2
		ELSE SAY THE obj2. "already has" SAY THE obj1. "."
      DOES
		-- implicit taking:
		IF obj1 NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj1. "first)$n"
			LOCATE obj1 IN hero.
		END IF.
		-- end of implicit taking.
		
		"You give" SAY THE obj1. "to" SAY THE obj2. "."
	  	LOCATE obj1 IN obj2.

  END VERB.
END ADD TO.


SYNONYMS hand, offer = give.



-- ==============================================================


----- GO TO


-- ==============================================================


SYNTAX go_to = 'to' (obj)!					-- because 'go' is predefined in the parser, it can't be used 
		WHERE obj ISA THING				-- in verb definitions
			ELSE "It's not possible to go to that."


ADD TO EVERY THING
  VERB go_to
	CHECK obj <> hero
		ELSE "You're right here!"
	AND hero IS NOT sitting
		ELSE "It is difficult to go anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to go anywhere while lying down."
	DOES 
		IF obj AT hero
			THEN 
				IF obj IS NOT reachable
					THEN "You can't reach" SAY THE obj. "from here."
					ELSE 
						IF CURRENT LOCATION IS lit
							THEN "That's right here!"
							ELSE "It is too dark to see."
						END IF.
				END IF.
			ELSE "You can't see" SAY THE obj. "anywhere nearby. You must state a
					 direction where you want to go."
		END IF.
  END VERB.
END ADD TO.


SYNONYMS walk = go.		-- here we define a synonym for the predefined parser word 'go'
					-- which is not visible in the syntax itself.
					-- Thus, you will be able to say e.g. both 'go to shop' and 'walk to shop'
					-- (as well as e.g. both 'go east' and 'walk east').



-- ==============================================================


----- HELP -> see ABOUT


-- ==============================================================





-- ==============================================================


----- HINT (+ hints)


-- ==============================================================


SYNTAX hint = hint.


VERB hint
	DOES
		"Unfortunately hints are not available in this game."
END VERB.


SYNONYMS
	hints = hint.



-- ==============================================================


----- INVENTORY (+ i, inv)


-- ==============================================================


ADD TO EVERY THING
   IS weight 0.
END ADD TO THING.


ADD TO EVERY ACTOR 
   IS weight 50. 
END ADD TO ACTOR. 


ADD TO EVERY OBJECT
   IS weight 5.
END ADD TO OBJECT. 


SYNTAX i = i.


VERB i
	DOES 
		LIST hero.

		-- if the hero in your game carries any containers you want the contents of 
		-- to be listed after 'inventory', you should manually add the listing right here
            -- according to the following example:
		-- 
		-- IF bag IN hero
		--     THEN LIST bag.
		-- END IF.
		--
		-- This way, 'inventory' will yield e.g. "You are carrying a bag. The bag contains a book."
		-- If you leave the above addition out, the outcome will be just "You are carrying a bag.", with
		-- no comment on what is inside the bag.
		
		IF COUNT IN worn > 0		-- see the file 'classes.i', subclass 'clothing'
			THEN LIST worn. 		
		END IF.
	
END VERB.


SYNONYMS inv, inventory  = i.



-- ==============================================================


----- JUMP


-- ==============================================================


SYNTAX jump = jump.


VERB jump
	CHECK hero IS NOT sitting
		ELSE "It is difficult to jump while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to jump while lying down."
	DOES
		"You jump on the spot, to no avail."
END VERB.



-- ==============================================================


----- JUMP IN


-- ==============================================================


SYNTAX jump_in = jump 'in' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can jump into."
		AND obj ISA CONTAINER
			ELSE "That's not something you can jump into."


ADD TO EVERY OBJECT
  VERB jump_in
	CHECK CURRENT LOCATION IS lit
		ELSE "It's too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is too far away."
	AND hero IS NOT sitting
		ELSE "It is difficult to jump while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to jump while lying down."
	DOES
		"That's not something you can jump into."
	
  END VERB.
END ADD TO.



-- ==============================================================


----- JUMP ON


-- ==============================================================


SYNTAX jump_on = jump 'on' (obj)
		WHERE obj ISA OBJECT
		      ELSE "That's not something you can jump on."
		AND obj ISA SUPPORTER
			ELSE "That's not something you can jump on."


ADD TO EVERY OBJECT
  VERB jump_on
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is too far away."
	AND hero IS NOT sitting
		ELSE "It is difficult to jump while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to jump while lying down."
	DOES
		IF obj ISA SUPPORTER
			THEN "That wouldn't accomplish anything."
			ELSE "That's not something you can jump onto."
		END IF.
  END VERB.
END ADD TO.


-- =============================================================


----- KICK 


-- =============================================================


SYNTAX kick = kick (obj)
    		WHERE obj ISA THING
      		ELSE "That’s not something you can kick."


ADD TO EVERY THING
  VERB kick
	CHECK obj IS examinable
		ELSE "That's not something you can kick."
	AND obj <> hero 
		ELSE "It doesn't make sense to kick yourself."
	AND obj NOT IN hero
		ELSE "It doesn't make much sense to kick something you're holding."
	AND obj NOT IN worn
		ELSE "It doesn't make sense to kick something you're wearing."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is too far away."
    	DOES "Resorting to violence is not the answer."
  END VERB.
END ADD TO.



-- ==============================================================


-- KILL (+ murder)


-- ==============================================================


SYNTAX kill = kill (obj)
		WHERE obj ISA ACTOR
			ELSE "That's not something you can $v."


ADD TO EVERY ACTOR
  VERB kill
	CHECK obj <> hero 
		ELSE "There's no need to be that desperate."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"That would be needlessly brutal."
  END VERB.
END ADD TO.



-- ==============================================================


-- KILL WITH 


-- ==============================================================


SYNTAX kill_with = kill (obj1) 'with' (obj2)
		WHERE obj1 ISA ACTOR
			ELSE "That's not something you can $v."
		AND obj2 ISA WEAPON
			ELSE "You cannot kill anybody with" SAY THE obj2. "."


ADD TO EVERY ACTOR
  VERB kill_with
	WHEN obj1
	CHECK obj1 <> hero 
		ELSE "There's no need to be that desperate."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"That would be needlessly brutal."
  END VERB.
END ADD TO.



-- ==============================================================


----- KISS (+ hug, embrace)


-- ==============================================================


SYNTAX kiss = kiss (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can kiss."


ADD TO EVERY THING
  VERB kiss
	CHECK obj IS examinable
		ELSE "That's not something you can kiss."
	AND obj <> hero
		ELSE "There is no time for that now."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		IF obj ISA ACTOR
			THEN SAY THE obj. "avoids your advances."
			ELSE "Nothing would be achieved by that."
		END IF.
  END VERB.
END ADD TO.


SYNONYMS hug, embrace = kiss.



-- ==============================================================


----- KNOCK 


-- ==============================================================


SYNTAX knock = knock 'on' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can knock on."
	

       knock = knock (obj).


ADD TO EVERY OBJECT
  VERB knock
	CHECK obj IS examinable
		ELSE "That's not something you can knock on."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"You knock on" SAY THE obj. "$$. Nothing happens."
  END VERB.
END ADD TO.


--- another 'knock' formulation added to guide players to use the right phrasing:


SYNTAX knock_error = knock.


VERB knock_error
	DOES 
		"Please use the formulation KNOCK ON SOMETHING to knock on something."
END VERB.



-- ==============================================================


----- LIE DOWN


-- ==============================================================


SYNTAX lie_down = lie 'down'.
	 
	 lie_down = lie.


VERB lie_down
	CHECK hero IS NOT lying_down
		ELSE "You're lying down already."
	DOES 
		"There's no need to lie down right now."
		-- If you need this to work, insert the following lines instead of the above:
				-- DOES "You lie down."
				-- MAKE hero lying_down.
				-- MAKE hero NOT sitting_down.
END VERB.


-- When the hero is sitting or lying down, it will be impossible for her/him to
-- perform certain actions, as numerous verbs in the library have checks for this. 
-- For example, if the hero is lying down and the player types 'attack [something]',
-- the response will be "It will be difficult to attack anything while
-- lying down." 
-- Also, it is often essential to make certain objects NOT reachable when you are sitting
-- or lying down.



-- ==============================================================


----- LIE IN


-- ==============================================================


SYNTAX lie_in = lie 'in' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can lie in."
		AND obj ISA CONTAINER
			ELSE "That's not something you can lie in."
	

       lie_in = lie 'down' 'in' (obj).
	

ADD TO EVERY OBJECT
  VERB lie_in
	CHECK hero IS NOT lying_down
		ELSE "You're lying down already."
	DOES 
		"There's no need to lie down in" SAY THE obj. "."
			-- If you need this to work, insert the following two lines instead of the above:
				-- DOES "You lie down in" SAY THE obj. "."
				-- LOCATE hero IN obj.
				-- MAKE hero lying_down.
  END VERB.
END ADD TO.


-- When the hero is sitting or lying down, it will be impossible for her/him to
-- perform certain actions, as numerous verbs in the library have checks for this. 
-- For example, if the hero is lying down and the player types 'attack [something]',
-- the response will be "It will be difficult to attack anything while
-- lying down." 
-- Also, it is often essential to make certain objects NOT reachable when you are sitting
-- or lying down.



-- ==============================================================


----- LIE ON


-- ==============================================================


SYNTAX lie_on = lie 'on' (obj)
		WHERE obj ISA SUPPORTER
			ELSE "That's not something you can lie on."
       

	 lie_on = lie 'down' 'on' (obj).


ADD TO EVERY OBJECT
  VERB lie_on
	CHECK hero IS NOT lying_down
		ELSE "You're lying down already."
	DOES 
		"There's no need to lie down on" SAY THE obj. "."
		-- If you need this to work, insert the following two lines instead of the above:
			-- DOES "You lie down on" SAY THE obj. "."
			-- LOCATE hero IN obj.    
			-- MAKE hero lying_down.
  END VERB.
END ADD TO.



-- When the hero is sitting or lying down, it will be impossible for her/him to
-- perform certain actions, as numerous verbs in the library have checks for this. 
-- For example, if the hero is lying down and the player types 'attack [something]',
-- the response will be "It will be difficult to attack anything while
-- lying down." 
-- Also, it is often essential to make certain objects NOT reachable when you are sitting
-- or lying down.



-- ==============================================================


----- LIFT


-- ==============================================================


SYNTAX lift = lift (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can lift."


ADD TO EVERY THING
  VERB lift  
	CHECK obj IS examinable
		ELSE "That's not something you can lift."
	AND obj IS reachable
		ELSE "The object is too far away."
	AND obj IS movable
		ELSE "That's not something you can lift."
	AND obj IS inanimate
		ELSE SAY THE obj. "wouldn't probably appreciate that."
	AND obj <> hero
		ELSE "It doesn't make sense to lift yourself."
	AND obj NOT IN hero
		ELSE "You're already holding" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"That wouldn't accomplish anything."	
  END VERB.
END ADD TO.



-- ==============================================================


----- LIGHT (+ lit)


-- ==============================================================


SYNTAX light = light (obj)
		WHERE obj ISA LIGHTSOURCE
			ELSE "That's not something you can $v."


ADD TO EVERY OBJECT
  VERB light
	CHECK obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"That's not something you can $v."
  END VERB.
END ADD TO.


SYNONYMS lit = light.



-- ==============================================================


----- LISTEN


-- ==============================================================


SYNTAX listen0 = listen.


VERB listen0
	DOES
		"You hear nothing unusual."
END VERB.



-- ==============================================================


----- LISTEN TO


-- ==============================================================


SYNTAX listen = listen 'to' (obj)!
		WHERE obj ISA THING
			ELSE "That's not something you can listen to."


ADD TO EVERY THING
  VERB listen
	CHECK obj <> hero 
		ELSE "It doesn't make sense to listen to yourself."
	DOES
		IF obj AT hero
			THEN "You hear nothing unusual."
		ELSIF obj NEAR hero
			THEN "You can't hear" SAY THE obj. "very well from here."
		ELSE "You can't hear anything."
		END IF.
  END VERB.
END ADD TO.



-- ==============================================================


----- LOCK


-- ==============================================================


SYNTAX lock = lock (obj)
    		WHERE obj ISA OBJECT
      		ELSE "That's not something you can lock."


ADD TO EVERY OBJECT
  VERB lock
	CHECK obj IS lockable
	    ELSE "That's not something you can lock."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj IS NOT locked
	    ELSE "It's already locked."
	DOES
	    "You have to state what you want to lock" SAY THE obj. "with."
	    --  If you need this to work, use the following lines instead:
	    -- MAKE obj locked. "You" 
	    -- IF obj IS NOT closed
		-- THEN "close and"
		-- MAKE obj closed.
	    -- END IF.
	    -- "lock" SAY THE obj. "."
  END VERB.
END ADD TO. 



-- ==============================================================


----- LOCK WITH


-- ==============================================================


SYNTAX lock_with = lock (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
	    		ELSE "That's not something you can lock."
		AND obj2 ISA OBJECT
	    		ELSE "You can't lock anything with that."


ADD TO EVERY OBJECT
  VERB lock_with
    WHEN obj1
	    CHECK obj1 IS lockable
		ELSE "That's not something you can lock."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	    AND obj1 IS NOT locked
		ELSE "It's already locked."
	    AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	    AND obj2 IN hero
		ELSE
		    "You don't have" SAY THE obj2. "."
	    DOES
		"You can't lock" SAY THE obj1. "with" SAY THE obj2. "."
		 -- If you need this to work, use the following lines instead:
	       -- MAKE obj1 locked. "You"
		 -- IF obj1 IS NOT closed
			-- THEN "close and"
			-- MAKE obj1 closed.
		 -- END IF.
		 -- "lock" SAY THE obj1. "with" SAY THE obj2. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- LOOK


-- ==============================================================


SYNTAX 'look' = 'look'.


VERB 'look'
	DOES
		LOOK.
		INCREASE described OF CURRENT LOCATION. 		
		-- see 'locations.i', attribute 'described'.
END VERB.


SYNONYMS l = 'look'.



-- ==============================================================


----- LOOK AT -> see EXAMINE


-- ==============================================================





-- ==============================================================


----- LOOK BEHIND


-- ==============================================================


SYNTAX look_behind = 'look' behind (obj)
		WHERE obj ISA THING
			ELSE "You can't look behind that."


ADD TO EVERY THING
  VERB look_behind 
	CHECK obj IS examinable
		ELSE 
			"You can't look behind" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj <> hero 
		ELSE "Turning your head, you notice nothing unusual behind yourself."
	DOES 
		"You notice nothing unusual behind" SAY THE obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- LOOK IN


-- ==============================================================


SYNTAX
	look_in = 'look' 'in' (obj) 
		WHERE obj ISA OBJECT
			ELSE "You can't look inside" SAY THE obj. "."
		AND obj ISA CONTAINER
			ELSE "You can't look inside" SAY THE obj. "."


ADD TO EVERY OBJECT
  VERB look_in
	CHECK obj IS examinable
		ELSE 
			"You can't look inside" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS NOT closed
		ELSE "You can't, since" SAY THE obj. "is closed."
	
	DOES  
		LIST obj.
  END VERB.
END ADD TO.



-- ==============================================================


----- LOOK OUT OF


-- ==============================================================


SYNTAX look_out_of = 'look' 'out' 'of' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can look out of."


ADD TO EVERY OBJECT
  VERB look_out_of 
	CHECK obj IS examinable
		ELSE 
			"You can't look out of" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"That's not something you can look out of."
  END VERB.
END ADD TO.



-- ==============================================================


----- LOOK THROUGH


-- ==============================================================


SYNTAX look_through = 'look' through (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can look through."


ADD TO EVERY OBJECT
  VERB look_through
	CHECK obj IS examinable
		ELSE "You can't look through" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"You can't see through" SAY THE obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- LOOK UNDER


-- ==============================================================


SYNTAX look_under = 'look' under (obj)
		WHERE obj ISA THING
			ELSE "You can't look under that."


ADD TO EVERY THING
  VERB look_under 
	CHECK obj IS examinable
		ELSE "You can't look under" SAY THE obj. "."
	AND obj <> hero
		ELSE "It doesn't make sense to look under yourself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"You notice nothing unusual under" SAY THE obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- LOOK UP 	(-> see also CONSULT)


-- ==============================================================


SYNTAX look_up = 'look' up.


VERB look_up
	DOES "Looking up, you see nothing unusual."
END VERB.



-- ==============================================================


----- NO


-- ==============================================================


SYNTAX 'no' = 'no'.


VERB 'no'
	DOES "You sound rather negative."
END VERB.



-- ==============================================================


----- NOTIFY


-- ==============================================================


-- Thanks to Steve Griffiths whose 'Score notification' sample was used
-- in declaring this verb.



SYNTAX notify = notify. 

	 notify_on = notify 'on'.	-- The instructions tell the player that mere 'notify'
						-- is enough, but these two verbs are implemented
	 notify_off = notify 'off'.	-- in case (s)he adds the prepositions to the end anyway.


VERB notify 
	DOES 
		IF hero IS notify_on 
			THEN MAKE hero NOT notify_on. 
				"Score notification is now disabled. (You can turn it back on 
				using the NOTIFY command again.)" 
			ELSE MAKE hero notify_on. "Score notification is now enabled. 
				(You can turn it off using the NOTIFY command again.)" 
		END IF. 
END VERB. 


VERB notify_on
	DOES 
		IF hero IS notify_on 
			THEN "Score notification is already enabled."
			ELSE MAKE hero notify_on. 
				"Score notification is now enabled. 
				(You can turn it off using the NOTIFY command again.)" 
		END IF. 
END VERB. 


VERB notify_off
	DOES 
		IF hero IS notify_on 
			THEN MAKE hero NOT notify_on. 
				"Score notification is now disabled. (You can turn it back on 
				using the NOTIFY command again.)" 
			ELSE "Score notification is already disabled." 
		END IF. 
END VERB. 


-- The 'notify' verb allows the players to disable the score change 
-- messages. (Some players find such messages annoying.) 
-- The verb toggles the hero's 'notify_on' attribute on and off. That 
-- attribute is checked by the 'checkscore' event to determine whether 
-- to display the score msg or not. 
	

-- The following event is run each turn to check if the game score is greater than 
-- the last recorded score (which is stored in the Hero's 'oldscore' 
-- attribute). If the score is greater, then the 'Score has gone up...' 
-- text is displayed (as long as the player hasn't disabled it by using the 
-- 'notify' verb - which sets 'notify_on' to off 
-- - i.e. the hero 'IS NOT notify_on'.) 

-- NOTE: The ALAN scoring system records the game score in a thing called 
-- score. It isn't called score OF anything; its just 'score'. 

-- NOTE: This event assumes score can only increase, if score can go 
-- down then would need to modify this code a bit. 


EVENT check_score 
	IF oldscore OF hero < score 
		THEN 
			IF hero IS notify_on 
				THEN 			-- ie: the player wants to see score msgs 
					"$p(Your score has just gone up by" SAY (score - oldscore OF hero). 
					IF (score - oldscore OF hero) = 1
						THEN "point.)"
						ELSE "points.)"
					END IF. 
					-- this msg only displayed the first time player is notified 
					-- of a score change 
					IF hero IS NOT seen_notify 
						THEN MAKE hero seen_notify. 
							"$p(You can use the NOTIFY command to disable score change messages.)" 
					END IF. 
			END IF.
 
			SET oldscore OF hero TO score. 
	END IF. 
	-- run the 'check_score' event again next turn: 
	SCHEDULE check_score AT hero AFTER 1.
END EVENT. 




-- ==============================================================


----- OPEN


-- ==============================================================


SYNTAX open = open (obj)
    		WHERE obj ISA OBJECT
      		ELSE "That's not something you can open."


ADD TO EVERY OBJECT
  VERB open
    CHECK obj IS closeable
      ELSE "That's not something you can open."
    AND CURRENT LOCATION IS lit
	ELSE "It is too dark to see."
    AND obj IS reachable
	ELSE SAY THE obj. "is out of your reach."
    AND obj IS closed
      ELSE SAY THE obj. "is already open."
    AND obj IS NOT locked
	ELSE SAY THE obj. "appears to be locked."
    DOES
      	MAKE obj NOT closed.
		"You open" SAY THE obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- OPEN WITH


-- ==============================================================


SYNTAX open_with = open (obj1) 'with' (obj2)
    		WHERE obj1 ISA OBJECT
      		ELSE "That's not something you can open."
    		AND obj2 ISA OBJECT
      		ELSE "You can't open anything with" SAY THE obj2.


ADD TO EVERY OBJECT
  VERB open_with
    WHEN obj1
	    CHECK obj1 IS closeable
		  ELSE "That's not something you can open."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	    AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	    AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	    AND obj1 IS closed
		  ELSE SAY THE obj1. "is already open."
	    AND obj1 IS NOT locked
		ELSE SAY THE obj1. "appears to be locked."
	    DOES
		  "You can't open" SAY THE obj1. "with" SAY THE obj2. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- PLAY


-- ==============================================================


SYNTAX 'play' = 'play' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can play."


ADD TO EVERY OBJECT
  VERB 'play'
	DOES 
		"That's not something you can play."
  END VERB.
END ADD TO.



-- ==============================================================


----- PLAY WITH


-- ==============================================================


SYNTAX play_with = 'play' 'with' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can play with."


ADD TO EVERY OBJECT
  VERB play_with
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"After second thoughts you don't find it purposeful to start
		 playing with" SAY THE obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- POUR		


-- ==============================================================


SYNTAX pour = pour (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can pour."
	AND obj ISA CONTAINER
		ELSE "That's not something you can pour."


ADD TO EVERY OBJECT
VERB pour
	CHECK obj IS takeable
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj IS NOT closed
		ELSE "You can't, since" SAY THE obj. "is closed." 
	DOES 
		-- implicit taking:
		IF obj NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj. "first)$n"
				LOCATE obj IN hero.
		END IF.
		-- end of implicit taking.
			
		IF COUNT ISA OBJECT, IN obj = 0
			THEN "There is nothing in" SAY THE obj. "."
		ELSE EMPTY obj AT hero.
			"You pour the contents of" SAY THE obj. 
				IF floor HERE 
					THEN "on the floor."
					ELSE "on the ground."
				END IF.
		END IF.
				
END VERB.
END ADD TO.



-- ==============================================================


----- POUR IN		


-- ==============================================================



SYNTAX pour_in = pour (obj1) 'in' (obj2)
	WHERE obj1 ISA OBJECT
		ELSE "That's not something you can pour."
	AND obj1 ISA CONTAINER
		ELSE "That's not something you can pour."
	AND obj2 ISA OBJECT					
		ELSE "That's not something you can pour things into."
	AND obj2 ISA CONTAINER
		ELSE SAY THE obj2. "is not something you can pour things into."


ADD TO EVERY OBJECT
VERB pour_in
   WHEN obj1
	CHECK obj1 <> obj2
		ELSE "It doesn't make sense to $v something into itself."
	AND obj1 IS takeable
		ELSE "You don't have" SAY THE obj1. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 NOT DIRECTLY IN obj2
		ELSE SAY THE obj1. "is already in" SAY THE obj2. "."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IS reachable
		ELSE SAY THE obj2. "is out of your reach."
	AND obj1 IS NOT closed
		ELSE "You can't, since" SAY THE THIS. "is closed." 
	AND obj2 IS NOT closed
		ELSE "You can't, since" SAY THE obj2. "is closed."
	DOES
		-- implicit taking:
		IF obj1 NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj1. "first)$n"
				LOCATE obj1 IN hero.
		END IF.
		-- end of implicit taking.
			
		IF COUNT ISA OBJECT, IN obj1 = 0
			THEN "There is nothing in" SAY THE obj1. "."
		ELSE EMPTY obj1 IN obj2.
			"You pour the contents of" SAY THE obj1. 
			"into" SAY THE obj2. "."	
		END IF.
END VERB.
END ADD TO.



-- ==============================================================


----- POUR ON		


-- ==============================================================



SYNTAX pour_on = pour (obj1) 'on' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "That's not something you can pour."
		AND obj1 ISA CONTAINER
			ELSE "That's not something you can pour."
		AND obj2 ISA OBJECT
			ELSE 
				IF obj2 ISA ACTOR
					THEN 
						IF obj2 = hero 
							THEN "That wouldn't make sense."
							ELSE	"That wouldn't be polite."
						END IF.
					ELSE "You can't pour anything onto" SAY THE obj2. "."
				END IF.
		AND obj2 ISA CONTAINER
			ELSE SAY THE obj2. "is not something you can pour things onto."



ADD TO EVERY OBJECT
VERB pour_on
   WHEN obj1
	CHECK obj1 <> obj2
		ELSE "It doesn't make sense to $v something on itself."
	AND obj1 IS takeable
		ELSE "You don't have" SAY THE obj1. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IS reachable
		ELSE SAY THE obj2. "is out of your reach."
	AND obj1 IS NOT closed
		ELSE "You can't, since" SAY THE obj1. "is closed."
	AND obj2 IS NOT closed
		ELSE "You can't, since" SAY THE obj2. "is closed."
	DOES
		-- implicit taking:
		IF obj1 NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj1. "first)$n"
				LOCATE obj1 IN hero.
		END IF.
		-- end of implicit taking.
			
		IF COUNT ISA OBJECT, IN obj1 = 0
			THEN "There is nothing in" SAY THE obj1. "."
		ELSE 
				IF obj2 = floor OR obj2 = ground 
					THEN EMPTY obj1 AT hero.
						"You pour the contents of" SAY THE obj1. "on" SAY THE obj2. "."
				ELSIF obj2 ISA SUPPORTER
					THEN EMPTY obj1 IN obj2.
						"You pour the contents of" SAY THE obj1. "on" SAY THE obj2. "."
				ELSE "It wouldn't be sensible to pour anything on" SAY THE obj2.
				END IF.
		END IF.
END VERB.
END ADD TO.



-- ==============================================================


----- PRAY 


-- ==============================================================


SYNTAX pray = pray.


VERB pray
	DOES "Your prayers don't seem to help right now."
END VERB.



-- ==============================================================


----- PRY


-- ==============================================================


SYNTAX pry = pry (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can pry."


ADD TO EVERY OBJECT
VERB pry
	DOES "You must state what you want to pry" SAY THE obj. "with."
END VERB.
END ADD TO.



-- ==============================================================


----- PRY_WITH


-- ==============================================================


SYNTAX pry_with = pry (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "That's not something you can pry."
		AND obj2 ISA OBJECT
			ELSE "You can't pry anything with that."


ADD TO EVERY OBJECT
VERB pry_with
	WHEN obj1
	CHECK obj1 IS examinable
		ELSE "That's not something you can pry."
	AND obj1 <> obj2 
		ELSE "How intelligent."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2.
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	DOES "That doesn't work."
END VERB.
END ADD TO.



-- ==============================================================


----- PULL


-- ==============================================================


SYNTAX pull = pull (obj)
		WHERE obj ISA OBJECT
			ELSE 
				IF obj ISA ACTOR
					THEN 
						IF obj = hero
							THEN "It doesn't make sense to pull yourself."
							ELSE SAY THE obj. "wouldn't probably appreciate that."
						END IF.
					ELSE "That's not something you can pull."
				END IF.


ADD TO EVERY OBJECT
VERB pull
	CHECK obj IS movable
		ELSE "It's not possible to pull" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES "That wouldn't accomplish anything."
END VERB.
END ADD TO.



-- ==============================================================


----- PUSH


-- ==============================================================


SYNTAX push = push (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can push."


ADD TO EVERY OBJECT
    VERB PUSH
	CHECK obj IS movable
	      ELSE "That's not something you can push."
	AND obj NOT IN hero
		ELSE "But you're holding" SAY THE obj. "."
	AND obj <> hero
		ELSE "It doesn't make sense to push yourself."
	AND obj IS inanimate
		ELSE SAY THE obj. "wouldn't probably appreciate that."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
	    "You give" SAY THE obj. "a little push. Nothing happens."
    END VERB.
END ADD TO.


SYNONYMS press = push.



-- ==============================================================


----- PUSH WITH


-- ==============================================================


SYNTAX push_with = push (obj1) 'with' (obj2)
		WHERE obj1 ISA THING
	    		ELSE "That's not something you can push."
		AND obj2 ISA OBJECT
	    		ELSE "You can use only objects to push things with."


ADD TO EVERY OBJECT
    VERB push_with
	WHEN obj1
	CHECK obj1 IS movable
	        ELSE "That's not something you can push."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to push something with itself."
	AND obj1 NOT IN hero
		ELSE "But you're holding" SAY THE obj1. "."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	AND obj1 <> hero
		ELSE "It doesn't make sense to push yourself with something."
	AND obj1 IS inanimate
		ELSE SAY THE obj1. "wouldn't probably appreciate that." 
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	DOES
		"Using" SAY THE obj2. "you push" SAY THE obj1. "$$. Nothing happens."
    END VERB. 
END ADD TO.



-- ==============================================================


----- PUT (+ lay, locate, place)


-- ==============================================================


SYNTAX put = put (obj) 
		WHERE obj ISA OBJECT
			ELSE "You can't put that anywhere."


ADD TO EVERY OBJECT
  VERB put
	CHECK obj IN HERO
		ELSE "You don't have" SAY THE obj.
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"You must state where you want to put it."
  END VERB.
END ADD TO.


SYNONYMS lay, place = put.



-- ==============================================================


----- PUT DOWN	(works as  'drop')


-- ==============================================================


----- The syntax for 'put down' has been declared in the 'drop' verb.





-- ==============================================================


----- PUT IN	(+ insert)


-- ==============================================================


SYNTAX put_in = put (obj1) 'in' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE 
				IF obj1 ISA ACTOR
				  THEN 
					IF obj1 = hero
						THEN "It doesn't make sense to put yourself into something."
						ELSE SAY THE obj1. "wouldn't probably appreciate that."
					END IF.
				  ELSE "You can't put that anywhere."
				END IF.
		AND obj2 ISA OBJECT
			ELSE "That's not something you can put things in."
		AND obj2 ISA CONTAINER
			ELSE "You can't put anything there." 
	 
   
        put_in = insert (obj1) 'in' (obj2).
		

ADD TO EVERY OBJECT
    VERB put_in
	WHEN obj1
	    CHECK obj1 <> obj2
	        	ELSE "It doesn't make sense to put something into itself."
	    AND obj1 IS takeable
		  	ELSE "You don't have" SAY THE obj1. "."
	    AND obj1 NOT IN obj2
		  	ELSE 
				IF obj2 ISA SUPPORTER
					THEN "You can't put" SAY THE obj1. "inside" SAY THE obj2. "."
					ELSE SAY THE obj1. "is in" SAY THE obj2. "already."
				END IF.
	    AND obj2 IS NOT closed
		  	ELSE "You can't, since" SAY THE obj2. "is closed."
	    AND CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
	    AND obj2 IS reachable
		  	ELSE SAY THE obj2. "is out of your reach."
	    DOES
			-- implicit taking:
			IF obj1 NOT DIRECTLY IN hero
				THEN  "(taking" SAY THE obj1. "first)$n"
					LOCATE obj1 IN hero.
			END IF.
			-- end of implicit taking.
			
			LOCATE obj1 IN obj2.
			"You put" SAY THE obj1. "into" SAY THE obj2. "."
				
    END VERB.
END ADD TO.



-- ==============================================================


----- PUT BEHIND, NEAR, UNDER


-- ==============================================================


SYNTAX put_near = put (obj1) 'near' (obj2)
        	WHERE obj1 ISA OBJECT
	    		ELSE
				IF obj1 ISA ACTOR
					THEN SAY THE obj1. "wouldn't probably appreciate that."
					ELSE "You can't put that anywhere."
				END IF.
	  	AND obj2 ISA THING
	    		ELSE "You can't put anything near that."


    	 put_behind = put (obj1) behind (obj2)
       	 WHERE obj1 ISA OBJECT
	    		ELSE 
				IF obj1 ISA ACTOR
					THEN SAY THE obj1. "wouldn't probably appreciate that."
					ELSE "You can't put that anywhere."
				END IF.
		AND obj2 ISA THING
	    		ELSE "You can't put anything behind that."

    
   	 put_under = put (obj1) under (obj2)
        	WHERE obj1 ISA OBJECT
	    		ELSE
				IF obj1 ISA ACTOR
					THEN SAY THE obj1. "wouldn't probably appreciate that."
					ELSE "You can't put that anywhere."
				END IF.
		AND obj2 ISA THING
	    		ELSE "You can't put anything under that."


ADD TO EVERY OBJECT
    VERB put_near, put_behind, put_under
	WHEN obj1	
	    CHECK obj2 NOT IN hero
		  ELSE "That would be futile."
	    AND obj1 IS takeable
		  ELSE "You don't have" SAY THE obj1. "."	
	    
	    AND obj1 <> obj2
		   ELSE "That doesn't make sense."
	    AND obj2 <> hero
		   ELSE "That would be futile."
	    AND CURRENT LOCATION IS lit
		    ELSE "It is too dark to see."
	    AND obj2 IS reachable
		   ELSE SAY THE obj2. "is out of your reach."
	    DOES
		   "That wouldn't accomplish anything."
		
             -- To make it work, type e.g.:	
		 -- IF obj1 NOT IN hero
			-- THEN  "(taking" SAY THE obj1. "first)$n"
		 -- END IF.
		 -- "You put" SAY THE obj1. "near" --(or behind or under) SAY THE obj2. "."
		 -- (+ you would need to define some attributes to check that 
		 -- the object is behind another object, etc.)
	
    END VERB.
END ADD TO.



-- ==============================================================


----- PUT_ON


-- ==============================================================


-- To use this verb in the meaning 'wear', see the file 'classes.i',
--  class 'clothing', verb 'wear'.

-- You can put things on the floor/ground or on a supporter. In other 
-- cases, the response will be "That wouldn't accomplish anything."



SYNTAX put_on = put (obj1) 'on' (obj2)
		WHERE obj1 ISA OBJECT
	    		ELSE
				IF obj1 ISA ACTOR
					THEN 
						IF obj1 = hero
							THEN "That would be futile."
							ELSE SAY THE obj1. "wouldn't probably 
								appreciate that."
						END IF.
					ELSE "You can't put that anywhere."
				END IF.
		AND obj2 ISA OBJECT	
	    		ELSE "You can't well put anything on that."	
      	AND obj2 ISA CONTAINER
	    		ELSE "You can't well put anything on that."


ADD TO EVERY OBJECT
    VERB put_on
	WHEN obj1
	    CHECK obj2 NOT IN hero
		  ELSE "That would be futile."
	    AND obj1 IS takeable
		   ELSE "You don't have" SAY THE obj1. "."	
	    AND obj1 <> obj2
		   ELSE "That doesn't make sense."
	    AND CURRENT LOCATION IS lit
		    ELSE "It is too dark to see."
	    AND obj1 NOT IN obj2
		   ELSE SAY THE obj1. "is on" SAY THE obj2. "already."
	    AND obj2 IS reachable
		   ELSE SAY THE obj2. "is out of your reach."
	    DOES	  
			-- implicit taking:
		 	IF obj1 NOT DIRECTLY IN hero
				THEN  "(taking" SAY THE obj1. "first)$n"
					LOCATE obj1 IN hero.
			END IF.
			-- end of implicit taking.
		
			IF obj2 = floor OR obj2 = ground
				THEN LOCATE obj1 AT hero.
				"You put" SAY THE obj1. "on" SAY THE obj2. "."
			ELSIF obj2 ISA SUPPORTER
				THEN LOCATE obj1 IN obj2.
				"You put" SAY THE obj1. "on" SAY THE obj2. "."
			ELSE "That wouldn't accomplish anything."
			END IF.	
    END VERB.
END ADD TO.



-- ==============================================================


----- PUT OUT  (works as -> EXTINGUISH)


-- ==============================================================


----- The syntax for 'put out' has been declared in the 'extinguish' verb.



-- ==============================================================


----- QUIT


-- ==============================================================


SYNTAX
	'quit' = 'quit'.


VERB 'quit'
	DOES
		QUIT.
END VERB.


SYNONYMS q = 'quit'.



-- ==============================================================


----- READ


-- ==============================================================


SYNTAX read = read (obj)
		WHERE obj ISA OBJECT
	    		ELSE "That's not something you can read."


ADD TO EVERY OBJECT
    VERB read
	CHECK obj IS readable
	    ELSE
		"That's not something you can read."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is too far away."
	DOES
		IF text OF obj = ""
			THEN "There's nothing written on" SAY THE obj. "."
			ELSE "You read" SAY THE obj. ". It says ""$$" SAY text OF obj. "$$""." 
		END IF.
    END VERB.
END ADD TO.



-- ==============================================================


-- REMOVE


-- ==============================================================


-- see the file 'classes.i', subclass 'clothing' for the definition of
-- this verb





-- ==============================================================


----- REMOVE FROM      ( => TAKE FROM)


-- ==============================================================


----- this verb works as 'take from'



-- ==============================================================


----- RESTART


-- ==============================================================


SYNTAX 'restart' = 'restart'.


VERB 'restart'
	DOES
		RESTART.
END VERB.



-- ==============================================================


----- RESTORE


-- ==============================================================


SYNTAX 'restore' = 'restore'.


VERB 'restore'
	DOES
		RESTORE.
END VERB.



-- ==============================================================


----- RUB (+ massage)


-- ==============================================================


SYNTAX rub = rub (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can rub."


ADD TO EVERY THING
  VERB rub
	CHECK obj IS examinable
		ELSE "That's not something you can rub."
	AND obj <> hero
		ELSE "There's no time for that now."
	AND obj IS inanimate
		ELSE "You aren't sure whether" SAY THE obj. 
			"would appreciate that."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"Nothing would be achieved by that."
  END VERB.
END ADD TO.


SYNONYMS massage = rub.



-- ==============================================================


----- SAVE


-- ==============================================================


SYNTAX 'save' = 'save'.


VERB 'save'
	DOES
		SAVE.
END VERB.



-- ==============================================================


----- SAY


-- ==============================================================


SYNTAX 'say' = 'say' (str)
    		WHERE str ISA STRING
      		ELSE "That's not something you can say."


ADD TO EVERY THING
  VERB 'say'
    DOES
      "You utter" SAY str. "$$. Nothing happens."
  END VERB.
END ADD TO.



-- ==============================================================


----- SAY TO


-- ==============================================================


SYNTAX say_to = 'say' (str) 'to' (act)
    		WHERE str ISA STRING
      		ELSE "Nothing happens."
    		AND act ISA ACTOR
      		ELSE "That's not something you can talk to."


ADD TO EVERY THING
  VERB say_to
    WHEN act
      CHECK act HAS can_talk
		ELSE "That's not something you can talk to."
	AND act IS reachable
		ELSE SAY THE act. "is too far away."
      DOES
		SAY THE act. "doesn't seem interested."
  END VERB.
END ADD TO.



-- ==============================================================


----- SCORE


-- ==============================================================


SYNTAX 'score' = 'score'.


VERB 'score'
	DOES
		SCORE.
		-- (or, if you wish to disable the score, use the following kind of 
			-- line instead of the above:)
		-- "There is no score in this game."
END VERB 'score'.



-- ==============================================================


----- SCRATCH


-- ==============================================================


SYNTAX scratch = scratch (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can scratch."


ADD TO EVERY THING
  VERB scratch
	CHECK obj IS examinable
		ELSE "That's not something you can scratch."
	AND obj IS inanimate
		ELSE SAY THE obj. "wouldn't probably appreciate that."
	AND obj <> hero
		ELSE "That wouldn't help matters."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"Nothing would be achieved by that."
  END VERB.
END ADD TO.



-- ==============================================================


------ SEARCH


-- ==============================================================


SYNTAX search = search (obj) 
		WHERE obj ISA THING
			ELSE "That's not something you can search."


ADD TO EVERY THING
  VERB search
	CHECK obj <> hero
		ELSE LIST hero.
	AND obj IS inanimate
		ELSE SAY THE obj. "would probably object to that."
	AND CURRENT LOCATION IS lit			
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES 
		"You find nothing of interest."
  END VERB.
END ADD TO.



-- ==============================================================


----- SELL


-- ==============================================================


SYNTAX sell = sell (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can sell."


ADD TO EVERY OBJECT
  VERB sell
	CHECK obj IS examinable
		ELSE "That's not something you can sell."
	DOES
		"There's nobody here who would be interested to buy" SAY THE obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- SHAKE


-- ==============================================================


SYNTAX shake = shake (obj)
		WHERE obj ISA OBJECT
			ELSE 
				IF obj ISA ACTOR
					THEN 
						IF obj = hero
							THEN "It doesn't make sense 
								to shake yourself."
							ELSE SAY THE obj. "wouldn't probably 
								appreciate that."
						END IF.
					ELSE "That's not something you can shake."
				END IF.


ADD TO EVERY OBJECT
VERB shake
	CHECK obj IS examinable
		ELSE "That's not something you can shake."
	AND obj IS movable
		ELSE "Shaking" SAY THE obj. "is not possible."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES 
		IF obj IN hero
			THEN "You shake" SAY THE obj. "cautiously in your hands. Nothing happens."
			ELSE "There is no reason to start shaking" SAY THE obj. "."
		END IF.
END VERB.
END ADD TO.



-- ==============================================================


----- SHOOT 


-- ==============================================================



SYNTAX shoot = shoot (obj)
    		WHERE obj ISA THING
      		ELSE "That's not something you can shoot."
       shoot = shoot 'at' (obj).

ADD TO EVERY THING
  VERB shoot
	CHECK obj <> hero 
		ELSE "There's no need to be that desperate."
	AND COUNT ISA WEAPON, IS fireable, IN hero > 0
		ELSE "You don't have anything to shoot with."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"Resorting to violence is not the answer."
  END VERB.
END ADD TO.


-- another  'shoot' formulation added, to guide players to use the right phrasing:


SYNTAX shoot_error = shoot.


VERB shoot_error
	DOES "You must state what you want to shoot, e.g. SHOOT BEAR WITH RIFLE."
END VERB.


	
-- ==============================================================


----- SHOOT WITH


-- ==============================================================


SYNTAX shoot_with = shoot (obj1) 'with' (obj2)
    		WHERE obj1 ISA THING
      		ELSE "That's not something you can shoot."
    		AND obj2 ISA WEAPON
      		ELSE "That's not something you can shoot with."

	 shoot_with = shoot (obj2) 'at' (obj1).
			-- to allow player input such as 'shoot rifle at bear'


ADD TO EVERY THING
  VERB shoot_with
    WHEN obj1
      CHECK obj2 IN hero
        ELSE "You don't have" SAY THE obj2. "."
	AND obj1 IS examinable
		ELSE "That's not something you can shoot."
	AND obj2 IS fireable
		ELSE "That's not something you can shoot with."
	AND obj1 <> hero 
		ELSE "There's no need to be that desperate."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to shoot something with itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
      DOES
           "Resorting to violence is not the answer."
  END VERB.
END ADD TO.



-- ==============================================================


----- SHOUT


-- ==============================================================


SYNTAX shout = shout.


VERB shout
  	DOES
    		"Nothing results from your $ving."
END VERB.


SYNONYMS scream, yell = shout.



-- ==============================================================


----- SHOW


-- ==============================================================


SYNTAX 'show' = 'show' (obj1) 'to' (obj2)
		WHERE obj1 ISA THING
			ELSE "That's not something you can show."
		AND obj2 ISA ACTOR
			ELSE "That's not something you can show things to."


ADD TO EVERY THING
  VERB 'show'
	WHEN obj1
	CHECK obj1 IN hero
		ELSE "You don't have" SAY THE obj1. "."
	AND obj2 <> hero 
		ELSE "It doesn't make sense to show something to yourself."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to show something to itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		SAY THE obj2. "is not especially interested."
  END VERB.
END ADD TO.


SYNONYMS reveal = 'show'.



-- ==============================================================


----- SING (+ hum, whistle)


-- ==============================================================


SYNTAX sing = sing.


VERB sing
  DOES
    	"You $v a little tune."
END VERB.


SYNONYMS hum, whistle = sing.



-- ==============================================================


----- SIP 
	

-- ==============================================================


SYNTAX sip = sip (obj)
		WHERE obj ISA LIQUID
			ELSE "That's not something you can drink."


ADD TO EVERY OBJECT
  VERB sip
	CHECK obj IS drinkable
		ELSE "That's not something you can drink."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES	
		IF vessel OF obj NOT DIRECTLY IN hero
			THEN 
				IF vessel OF obj = zero_vessel OR vessel OF obj IS NOT takeable
					THEN "It's not worth the bother trying to take a sip of" SAY THE obj. "."
				ELSE LOCATE vessel OF obj IN hero.
					"(taking" SAY THE vessel OF obj. "first)$n"
				END IF.
		END IF.
		
		"You take a sip of" SAY THE obj. "$$. It tastes rather good."
			
  END VERB.
END ADD TO.



-- ==============================================================


----- SIT


-- ==============================================================


SYNTAX sit = sit.
	 
 
       sit = sit 'down'.


VERB sit 
	CHECK hero IS NOT sitting
		ELSE "You're sitting down already."
	DOES 
		"You feel no urge to sit down at present."
		-- (or, if you wish to make it work, use the following instead of the above:
		-- IF hero IS lying_down
		--	THEN "You sit up."
		--		MAKE hero NOT lying_down.
		-- 	ELSE "You sit down."
		-- END IF.
		-- MAKE hero sitting.
END VERB.

-- When the hero is sitting or lying down, it will be impossible for her/him to
-- perform certain actions, as numerous verbs in the library have checks for this. 
-- For example, if the hero is sitting and the player types 'attack [something]',
-- the response will be "It will be difficult to attack anything while
-- sitting down." 
-- Also, it is often essential to make certain objects NOT reachable when
-- sitting or lying down.



-- ==============================================================


----- SIT_ON


-- ==============================================================


SYNTAX sit_on = sit 'on' (obj)
		WHERE obj ISA SUPPORTER
			ELSE "That's not something you can sit on."


ADD TO EVERY SUPPORTER
  VERB sit_on
	CHECK hero IS NOT sitting
		ELSE "You're sitting down already."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"You feel no urge to sit down at present."
		-- (or, to make it work, use the following instead of the above:)
		-- IF hero lying_down 
		-- 	THEN "You get up and sit down on" SAY THE obj. "."
		--		MAKE hero NOT lying_down.
		--	ELSE "You sit down on" SAY THE obj. "."
		-- END IF.
		-- LOCATE hero IN obj.
		-- MAKE hero sitting.
  END VERB.
END ADD TO.


-- When the hero is sitting or lying down, it will be impossible for her/him to
-- perform certain actions, as numerous verbs in the library have checks for this. 
-- For example, if the hero is sitting and the player types 'attack [something]',
-- the response will be "It will be difficult to attack anything while
-- sitting down." 
-- Also, it is often essential to make certain objects NOT reachable when
-- sitting or lying down.



-- ==============================================================


----- SLEEP	(+ rest)


-- ==============================================================


SYNTAX sleep = sleep.


VERB sleep
	DOES 
		"There's no need to $v right now."
END VERB.


SYNONYMS rest = sleep.



-- ==============================================================


----- SMELL (smell0)


-- ==============================================================


SYNTAX smell0 = smell.


VERB smell0
    DOES
		"You smell nothing unusual."
END VERB.



-- ==============================================================


----- SMELL (+ obj)


-- ==============================================================


SYNTAX smell = smell (obj)!
		WHERE obj ISA THING
	    		ELSE "That's not something you can smell."


ADD TO EVERY THING
  VERB smell
	DOES 
		IF obj ISA SOUND
			THEN "That's not something you can smell."
	    		ELSE "You smell nothing unusual."
		END IF.	
  END VERB.
END ADD TO.



-- ==============================================================


----- SQUEEZE


-- ==============================================================


SYNTAX squeeze = squeeze (obj)
		WHERE obj ISA THING
	    		ELSE "That's not something you can squeeze."


ADD TO EVERY THING
    VERB squeeze
	CHECK obj IS examinable
		ELSE "That's not something you can squeeze."
	AND CURRENT LOCATION IS lit
		ELSE 
			IF obj = hero
				THEN "Nothing would be achieved by that."  
					-- you can squeeze yourself in the dark, as well
				ELSE "It is too dark to see."
			END IF.
	AND obj IS reachable
			ELSE SAY THE obj. "is out of your reach."		
	
	DOES
	    	IF obj ISA ACTOR
			THEN "That wouldn't be polite."
			ELSE "Trying to squeeze" SAY THE obj. "wouldn't be sensible."
		END IF.
    END VERB.
END ADD TO.



-- ==============================================================


----- STAND


-- ==============================================================


SYNTAX stand = stand.

	
	 stand = stand 'up'.


VERB stand
	CHECK hero IS NOT lying_down
		ELSE "You get up."
			MAKE hero NOT lying_down.
	AND hero IS NOT sitting
		ELSE "You stand up."
			MAKE hero NOT sitting. 
	DOES 
		"You're standing up already."
END VERB.



-- ==============================================================


----- STAND_ON


-- ==============================================================


SYNTAX stand_on = stand 'on' (obj)
		WHERE obj ISA SUPPORTER
			ELSE "That's not something you can stand on."


       stand_on = get 'on' (obj).  


ADD TO EVERY SUPPORTER
VERB stand_on
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"You feel no urge to stand on" SAY THE obj. "."
		-- or, to make it work, use the following instead of the above:
		-- "You get on" SAY THE obj. "."
		-- LOCATE hero IN obj.
		-- MAKE hero NOT sitting. MAKE hero NOT lying_down.
END VERB.
END ADD TO.



-- ==============================================================


----- SWIM


-- ==============================================================


SYNTAX swim = swim.


VERB swim 
	CHECK hero IS NOT sitting
		ELSE "It is difficult to try swimming while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to try swimming while lying down."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"There is no water suitable for swimming here."
END VERB.



-- ==============================================================


----- SWIM IN


-- ==============================================================


SYNTAX swim_in = swim 'in' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can swim in."
		AND obj ISA CONTAINER
			ELSE "That's not something you can swim in."


ADD TO EVERY OBJECT
VERB swim_in
	CHECK hero IS NOT sitting
		ELSE "It is difficult to swim anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to swim anywhere while lying down."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"That's not something you can swim in."
END VERB.
END ADD TO.



-- ==============================================================


----- SWITCH 


-- ==============================================================


-- This is a mere 'switch' verb with no 'on' or 'off' following after it. 
-- This verb is defined further in 'classes.i', under 'device' and 'lightsource'.
-- This verb exists just to cover cases where the player forgets to write
-- 'on' or 'off' after 'switch'. 
-- If the player types 'switch tv', the tv object will be switched on 
-- if it is off, and vice cersa.
-- Below, just the basic syntax is declared.



SYNTAX switch = switch (obj)
	WHERE obj ISA THING 
		ELSE "That's not something you can switch."
   
     
ADD TO EVERY THING
	VERB switch
		DOES "That's not something you can switch."
	END VERB.
END ADD TO.



-- ==============================================================


----- SWITCH ON


-- ==============================================================


----- The syntax for 'switch on' has been declared in the 'turn_on' verb.




-- ==============================================================


----- SWITCH OFF


-- ==============================================================


----- The syntax for 'switch off' has been declared in the 'turn_off' verb.




-- ==============================================================


----- TAKE	(+ carry, get, grab, hold, obtain, pick up)


-- ==============================================================


SYNTAX take = take (obj)
    		WHERE obj ISA OBJECT
      		ELSE 
				IF obj ISA ACTOR
					THEN 
						IF obj = hero
							THEN "Taking yourself is not possible."
							ELSE SAY THE obj. "would probably object to that."
						END IF.
					ELSE "That's not something you can take."
				END IF.


	take = get (obj).


  	take = pick up (obj).


  	take = pick (obj) up.


ADD TO EVERY OBJECT
  VERB take
	CHECK CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
    	AND obj IS takeable
      	ELSE 
			IF obj ISA SCENERY
				THEN "Unimportant for your purposes, you leave the" SAY THE obj. "where it is."
				ELSE "That's not something you can take."
			END IF.
    	AND obj IS movable
		ELSE SAY THE obj. "is much too heavy for you to move."
    	AND obj IS inanimate
		ELSE SAY THE obj. "would probably object to that."
    	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
    	AND weight Of obj <=50					
      	ELSE SAY THE obj. "is too heavy to lift."
    	DOES
		IF obj DIRECTLY IN hero			
			-- i.e. the object to be taken is carried by the hero already						
			THEN "You already have" SAY THE obj. "."  	
		ELSIF obj DIRECTLY IN worn		
			-- i.e. the object to be taken is a piece of clothing that the player character is wearing;
			 -- here, this verb works in practise like 'take off'. 
			THEN "You take off" SAY THE obj. "and carry it in your hands."
				LOCATE obj IN hero.		
		ELSE "Taken."				
			-- this covers also cases where the object to be taken is in another container,
			 -- such as a wallet that is in a jacket the hero is carrying or wearing.
			LOCATE obj IN hero.		
		END IF.			
  END VERB.
END ADD TO.


SYNONYMS
  carry, grab, hold, obtain = take.



-- ==============================================================


-----  TAKE FROM


-- ==============================================================


SYNTAX take_from = 'take' (obj1) 'from' (obj2)
    		WHERE obj1 ISA OBJECT
      		ELSE "You can only take objects."
    		AND obj2 ISA THING
      		ELSE "It's not possible to take things from there."
    		AND obj2 ISA CONTAINER
      		ELSE "It's not possible to take things from there."


 	 take_from = remove (obj1)* 'from' (obj2).

 
	 take_from = get (obj1) 'from' (obj2).


ADD TO EVERY OBJECT
  VERB take_from
    WHEN obj1
	CHECK obj2 <> hero
		ELSE "You can't take things from yourself!"
      AND obj1 NOT IN hero 		
	  	ELSE	"You already have" SAY THE obj1. "."
	AND obj1 <> obj2
		ELSE "You can't take something from itself!"
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj1 IN obj2
		ELSE
			IF obj2 IS inanimate
	  			THEN SAY THE obj1. "is not in" SAY THE obj2. "."
				ELSE SAY THE obj2. "doesn't have" SAY THE obj1. "."
			END IF.
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IS NOT closed
		ELSE "You can't, since" SAY THE obj2. "is closed."
	AND obj1 IS movable
		ELSE SAY THE obj1. "is much too heavy for you to take."
	AND obj1 IS takeable
      	ELSE 
			IF obj1 ISA SCENERY
				THEN "Unimportant for your purposes, you leave the" 
					SAY THE obj1. "where it is."
				ELSE "That's not something you can take."
			END IF.
	AND weight Of obj1 <=50
      	ELSE SAY THE obj1. "is too heavy."
	DOES
		LOCATE obj1 IN hero.
	    	"You take" SAY THE obj1. "from" SAY THE obj2. "."	
  END VERB.
END ADD TO.



-- ==============================================================


----- TALK


-- ==============================================================


SYNTAX talk = talk.


VERB talk
	DOES 
		"To talk to somebody, you can ASK PERSON ABOUT THING
		or TELL PERSON ABOUT THING."
END VERB.



-- ==============================================================


----- TALK_TO


-- ==============================================================


SYNTAX talk_to = talk 'to' (act) 
    		WHERE act ISA ACTOR
      		ELSE "That's not something you can talk to."
  

ADD TO EVERY THING
  VERB talk_to
	DOES 
		"To talk to somebody, you can ASK PERSON ABOUT THING or
		TELL PERSON ABOUT THING."
  END VERB.
END ADD TO.


SYNTAX talk_to_a = talk 'to' (act) about (topic)!
    		WHERE act ISA ACTOR
      		ELSE "That's not something you can talk to."
		AND topic ISA THING
		      ELSE "That's not something you can talk about."
      

ADD TO EVERY THING
  VERB talk_to_a
	WHEN act
		DOES 
			"To talk to somebody, you can ASK PERSON ABOUT THING or
  			TELL PERSON ABOUT THING."
  END VERB.
END ADD TO.


-- ==============================================================


----- TASTE		(+ lick)


-- ==============================================================


SYNTAX taste = taste (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can taste."


ADD TO EVERY OBJECT
  VERB taste
	CHECK obj IS examinable
		ELSE "That's not something you can taste."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES "You taste nothing unexpected."
  END VERB.
END ADD TO. 	



-- ==============================================================


----- TEAR	(+ rip)


-- ==============================================================


SYNTAX tear = tear (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can tear."


ADD TO EVERY OBJECT
  VERB tear
	CHECK obj IS examinable
		ELSE "That's not something you can tear."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES 
		"That would be futile."
  END VERB.
END ADD TO.


SYNONYMS rip = tear.



-- ==============================================================


----- TELL 	(+ enlighten, inform)


-- ==============================================================


SYNTAX tell = tell (act) about (topic)!
    		WHERE act ISA ACTOR
      		ELSE "That's not something you can talk to."
    		AND topic ISA THING
      		ELSE "That doesn't seem to be something you can talk
			      about with" SAY THE act. "."


ADD TO EVERY THING
  VERB tell
    WHEN act
      CHECK act HAS can_talk
        	ELSE "That's not something you can talk to."
	AND act IS reachable
		ELSE SAY THE act. "is too far away."
	AND act <> hero
		ELSE "It doesn't make much sense to tell yourself about something."
      DOES
		IF topic IN act
			THEN SAY THE act. "doesn't seem to want to talk about" SAY THE topic. "."
	      ELSIF topic = act
			THEN SAY THE act. "chooses to be silent."
		ELSIF topic = hero
			THEN SAY THE act. "doesn't seem interested."
		ELSE SAY THE act. "doesn't seem interested."
		END IF.
  END VERB.
END ADD TO.


SYNONYMS enlighten, inform = tell.



-- ==============================================================


----- THINK		(+ ponder, meditate, reflect)


-- ==============================================================


SYNTAX think = think.


VERB think 
	DOES 
		"Nothing helpful comes to your mind."
END VERB.


SYNONYMS ponder, meditate, reflect = think.



-- ==============================================================


----- THINK ABOUT


-- ==============================================================


SYNTAX think_about = think 'about' (obj)
		WHERE obj ISA THING
			ELSE "That's not something fruitful to think about."


ADD TO EVERY THING
  VERB think_about
	DOES 
		"Nothing helpful comes to your mind."
  END VERB.
END ADD TO.



-- ==============================================================


----- THROW   


-- ==============================================================


SYNTAX throw = throw (obj) 
		WHERE obj ISA OBJECT
			ELSE "That's not something you can throw."


ADD TO EVERY OBJECT
  VERB throw
	CHECK obj IS examinable
		ELSE "That's not something you can throw."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		-- implicit taking:
		IF obj NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj. "first)$n"
			LOCATE obj IN hero.
		END IF.
		-- end of implicit taking.
				
		"You can't throw very far;" SAY THE obj. "ends up on the"
			IF floor HERE
				THEN "floor"
				ELSE "ground"
			END IF.
		"nearby."
	    	LOCATE obj AT hero.
			
  END VERB.
END ADD TO.




-- ==============================================================


----- THROW AT 	(+ throw to)


-- ==============================================================


SYNTAX throw_at = throw (obj1) 'at' (obj2)
       	 WHERE obj1 ISA OBJECT
	    		ELSE "You can only throw objects."
	  	 AND obj2 ISA THING
	    		ELSE "It's not possible to throw things at that."


    	throw_to = throw (obj1) 'to' (obj2)
      	  WHERE obj1 ISA OBJECT
	    		ELSE "You can only throw objects."
	  	  AND obj2 ISA THING
	   		ELSE "It's not possible to throw things to that."


ADD TO EVERY OBJECT
  VERB throw_at, throw_to 
    WHEN obj1
	    CHECK obj1 IS examinable
		  	ELSE "That's not something you can throw."
	    AND obj2 IS examinable
		  	ELSE "That's not something you can throw things at."
	    AND obj1 <> obj2
			ELSE "It doesn't make sense to throw something at itself."
	    AND obj2 NOT IN hero
	        	ELSE "You are carrying" SAY THE obj2. "."
	    AND obj2 <> hero
		   	ELSE "You cannot throw things at yourself."
	    AND CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
	    AND obj2 IS reachable
		  	ELSE SAY THE obj2. "is too far away."
	    DOES 
			-- implicit taking:
		  	IF obj1 NOT DIRECTLY IN hero
				THEN  "(taking" SAY THE obj1. "first)$n"
					LOCATE obj1 IN hero.
		  	END IF.
			-- end of implicit taking.

      	  	IF obj2 IS inanimate
				THEN "The" SAY obj1. "bounces harmlessly off" 
					SAY THE obj2. "and ends up on the"
		  				IF floor HERE
							THEN "floor"
							ELSE "ground"
		  				END IF.
		     			"nearby."
		  			LOCATE obj1 HERE.
				ELSE "The" SAY obj2. "catches" SAY THE obj1. "and tosses"
						IF obj1 IS NOT plural
							THEN "it"
							ELSE "them"
						END IF.
					"back to you. You grab hold of"
						IF obj1 IS NOT plural
							THEN "it"
							ELSE "them"
						END IF.
 
					"again."
		  	END IF.

  END VERB.
END ADD TO.



-- ==============================================================


------ THROW IN


-- ==============================================================


SYNTAX throw_in = throw (obj1) 'in' (obj2)
		WHERE obj1 ISA OBJECT
	    		ELSE "That's not something you can throw."
		AND obj2 ISA OBJECT
	    		ELSE "That's not something you can throw things into."
		AND obj2 ISA CONTAINER
	    		ELSE "That's not something you can throw things into."


ADD TO EVERY OBJECT
  VERB throw_in
    WHEN obj1
          CHECK obj1 IS examinable
		  ELSE "That's not something you can throw."
	    AND obj2 IS examinable
		  ELSE "That's not something you can throw things into."
	    AND obj1 <> obj2
		ELSE "It doesn't make sense to throw something into itself."
	    AND obj2 <> hero
	        ELSE "You can't throw" SAY THE obj1. "into yourself."
	    AND CURRENT LOCATION IS lit
		  ELSE "It is too dark to see."
	    AND obj1 NOT IN obj2
		  ELSE SAY THE obj1. "is in" SAY THE obj2. "already!"
	    AND obj2 IS reachable
		  ELSE SAY THE obj2. "is too far away."
	    DOES
		  -- implicit taking:
		  IF obj1 NOT DIRECTLY IN hero
		 	THEN  "(taking" SAY THE obj1. "first)$n"
				LOCATE obj1 IN hero.
		  END IF.
		  -- end of implicit taking.

		  LOCATE obj1 IN obj2.
		  "You throw" SAY THE obj1. "into" SAY THE obj2. "."
				
  END VERB.
END ADD TO.



-- ==============================================================


----- TIE


-- ==============================================================


SYNTAX tie = tie (obj) 
		WHERE obj ISA OBJECT
			ELSE "That's not something you can tie."


ADD TO EVERY OBJECT
  VERB tie
	DOES 
		"You must state where do you want to tie" SAY obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- TIE TO


-- ==============================================================


SYNTAX tie_to = tie (obj1) 'to' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "That's not something you can tie."
		AND obj2 ISA OBJECT
			ELSE "Nothing can be tied to that."


ADD TO EVERY OBJECT
  VERB tie_to
	WHEN obj1
	CHECK obj1 IS examinable
		  ELSE "That's not something you can tie."
	AND obj2 IS examinable
		  ELSE "That's not something you can tie things to."
	AND obj1 IS takeable
		  ELSE "You don't have" SAY THE obj1. "." 
	AND obj1 <> obj2
		ELSE "It doesn't make sense to tie something to itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj2 IS reachable
		  ELSE SAY THE obj2. "is out of your reach."
	DOES 
		-- implicit taking:
		IF obj1 NOT DIRECTLY IN hero
			THEN  "(taking" SAY THE obj1. "first)$n"
				LOCATE obj1 IN hero.
		END IF.
		-- end of implicit taking.
						
		LOCATE obj1 IN hero.
		"It's not possible to tie" SAY THE obj1. "to" SAY THE obj2. "."	

  END VERB.
END ADD TO.



-- ==============================================================


----- TOUCH


-- ==============================================================


SYNTAX touch = touch (obj)
		WHERE obj ISA THING
	    		ELSE "That's not something you can touch."
    

ADD TO EVERY THING
  VERB touch
        CHECK obj IS examinable
            ELSE "That's not something you can touch."
	  AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	  AND obj IS inanimate
		ELSE "You are not sure whether" SAY THE obj. "would appreciate that."
        DOES
	      "You feel nothing unexpected."
  END VERB.
END ADD TO.


SYNONYMS feel = touch.



-- ==============================================================


----- TOUCH WITH


-- ==============================================================


SYNTAX touch_with = touch (obj1) 'with' (obj2)
	WHERE obj1 ISA THING
   		ELSE "That's not something you can touch."
	AND obj2 ISA OBJECT
	    	ELSE "You can only use objects to touch with."


ADD TO EVERY THING
  VERB touch_with
	WHEN obj1
	    CHECK obj1 IS examinable
	        ELSE "That's not something you can touch."
	    AND obj2 IS examinable
		  ELSE "That's not something you can touch things with."
	    AND obj2 IN hero
		  ELSE "You don't have" SAY THE obj2. "."
	    AND obj1 <> obj2
	        ELSE "It doesn't make sense to touch something with itself."
	    AND obj1 IS inanimate
		  ELSE "You are not sure whether" SAY THE obj1. "would appreciate that."
	    AND CURRENT LOCATION IS lit
		  ELSE "It is too dark to see."
	    AND obj1 IS reachable
		  ELSE SAY THE obj1. "is out of your reach."
	    DOES
	        "You touch" SAY THE obj1. "with" SAY THE obj2. ". Nothing special happens."
  END VERB.
END ADD TO.



-- ==============================================================


----- TURN	(+ rotate)


-- ==============================================================


SYNTAX turn = turn (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can turn."


       turn = rotate (obj).   -- we don't declare 'rotate' a synonym for 'turn'
				     -- through a SYNONYMS statement as we don't want
				     -- it to be possible for the player to type something
				     -- like 'rotate tv on' (see 'turn on' and 'turn off' below)


ADD TO EVERY OBJECT
  VERB turn
	CHECK obj IS examinable
		ELSE "That's not something you can turn."
	AND obj IS movable
		ELSE "It's not possible to turn" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES "You turn" SAY THE obj. 
		IF obj IN hero
			THEN "in your hands"
		END IF.
		"$$. You notice nothing unusual about it"
		IF obj NOT IN hero
			THEN "and return it to its original position"
		END IF.
		"."
  END VERB.
END ADD TO.



-- ==============================================================


----- TURN ON


-- ==============================================================


----- Only devices and lightsources can be turned on and off. These classes are 
----- defined in 'classes.i' with proper checks for 'on' and 'NOT on', 'lit' and 'NOT lit'. 
----- Trying to turn on or off an ordinary object will default here to "That's not 
----- something you can turn on".


SYNTAX turn_on = turn 'on' (obj)
		WHERE obj ISA OBJECT
	    		ELSE "That's not something you can $v on."

  	 turn_on = switch 'on' (obj).


       turn_on = turn (obj) 'on'.


       turn_on = switch (obj) 'on'.
		


-- Note that 'switch' is not declared a synonym for 'turn'.
-- This is because 'turn' has also other meanings, e.g. 'turn page' which is
-- not equal with 'switch page'. 
-- A separate 'switch' verb is declared in 'classes.i', subclass 'device'.
-- This verb merely covers cases where the player forgets to type 'on' or 'off'.



ADD TO EVERY OBJECT
  VERB turn_on
	DOES
		"That's not something you can $v on."
  END VERB.
END ADD TO.



-- ==============================================================


----- TURN OFF


-- ==============================================================


----- Only devices and lightsources can be turned on and off. These classes 
----- are defined in 'classes.i' with proper checks for 'on' and 'NOT on', 
----- 'lit' and 'NOT lit'. 


SYNTAX turn_off = turn off (obj)
		WHERE obj ISA OBJECT
	 	   	ELSE "That's not something you can $v off."

	turn_off = switch off (obj).
		

    	turn_off = turn (obj) off.
		
      
	turn_off = switch (obj) off.
		


-- Note that 'switch' is not declared a synonym for 'turn'.
-- This is because 'turn' has also other meanings, e.g. 'turn page' which is
-- not equal with 'switch page'. 
-- A separate 'switch' verb is declared in 'classes.i', subclasses 'device' and 'lightsource'.
-- This verb merely covers cases where the player forgets to type 'on' or 'off'.
    	

ADD TO EVERY OBJECT
  VERB turn_off
	DOES
		"That's not something you can $v off."
  END VERB.
END ADD TO.



-- ==============================================================


----- USE


-- ==============================================================


SYNTAX 'use' = 'use' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can use."


ADD TO EVERY OBJECT
  VERB 'use'
	DOES
		"Please be more specific. How do you intend to use it?"
  END VERB.
END ADD TO.



-- ==============================================================


----- USE WITH


-- ==============================================================


SYNTAX use_with = 'use' (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
	    		ELSE "You can only use objects."
		AND obj2 ISA OBJECT
	    		ELSE "You can only use objects."


ADD TO EVERY OBJECT
  VERB use_with
    WHEN obj1
	CHECK obj1 <> obj2 
		ELSE "You can't use something with itself."
	DOES 
		"Please be more specific. How do you intend to use them together?"
  END VERB.
END ADD TO.



-- ==============================================================


----- UNDRESS


-- ==============================================================


-- See the file 'classes.i', subclass CLOTHING for the definition
-- of this verb.





-- ==============================================================


----- UNLOCK


-- ==============================================================


SYNTAX unlock = unlock (obj)
       	 WHERE obj ISA OBJECT
		    ELSE "That's not something you can unlock."


ADD TO EVERY OBJECT
  VERB unlock
	CHECK obj IS lockable
	    ELSE "That's not something you can unlock."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj IS locked
	    ELSE "It's already unlocked."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
	    "You must state what you want to unlock" SAY THE obj. "with."
  END VERB.
END ADD TO.



-- =============================================================


----- UNLOCK WITH


-- =============================================================


SYNTAX unlock_with = unlock (obj1) 'with' (obj2)
		WHERE obj1 ISA OBJECT
	    		ELSE "That's not something you can unlock."
		AND obj2 ISA OBJECT
	    		ELSE "You can't unlock anything with that."


ADD TO EVERY OBJECT
  VERB unlock_with
        WHEN obj1
	    CHECK obj1 IS lockable
	        ELSE "That's not something you can unlock."
	    AND obj2 In hero
		  ELSE "You don't have" SAY THE obj2. "."
	    AND obj1 <> obj2
		ELSE "It doesn't make sense to unlock something with itself."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	    AND obj1 IS locked
		ELSE "It's already unlocked."
	    AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	  DOES
		SAY THE obj2. "doesn't unlock" SAY THE obj1. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- VERBOSE (see also -> BRIEF)


-- ==============================================================


SYNTAX verbose = verbose.


VERB verbose
	DOES
		VISITS 0.
		"Verbose mode is now on. Location descriptions will be 
		always shown in full."
END VERB.



-- ==============================================================


----- WAIT (= z)


-- ==============================================================


SYNTAX 'wait' = 'wait'.


VERB 'wait'
	DOES
		"Time passes..."
END VERB.


SYNONYMS
	z = 'wait'.



-- ==============================================================


----- WHAT AM I


-- ==============================================================


SYNTAX what_am_i = 'what' am i.


VERB what_am_i
	DOES 
		"Maybe examining yourself might help."
END VERB.



-- ==============================================================


----- WHAT IS


-- ==============================================================


SYNTAX what_is = 'what' 'is' (obj)!
		WHERE obj ISA THING
			ELSE "That's not something I know about."


ADD TO EVERY THING
  VERB what_is
	DOES 
		"You'll have to find it out yourself."
  END VERB.
END ADD TO.



-- ==============================================================


----- WHERE AM I


-- ==============================================================


SYNTAX where_am_i = 'where' am i.


VERB where_am_i
	DOES 
		LOOK.
END VERB.



-- ==============================================================


----- WHERE IS


-- ==============================================================


SYNTAX where_is = 'where' 'is' (obj)!
		WHERE obj ISA THING
			ELSE "That's not something I know about."


ADD TO EVERY THING
  VERB where_is 
	CHECK obj NOT AT hero
		ELSE "That's right here!"
	DOES 
		"You'll have to find it out yourself."
  END VERB.
END ADD TO.



-- ==============================================================


----- WHO AM I


-- ==============================================================


SYNTAX who_am_i = who am i.


VERB who_am_i 
	DOES 
		"Maybe examining yourself might help."
END VERB.



-- ==============================================================


----- WHO IS


-- ==============================================================


SYNTAX who_is = 'who' 'is' (act)!
		WHERE act ISA ACTOR
			ELSE "That's not somebody I know."


ADD TO EVERY ACTOR
  VERB who_is
	DOES 
		"You'll have to find it out yourself."
  END VERB.
END ADD TO.



-- ==============================================================


----- WRITE


-- ==============================================================


SYNTAX write = write (str) 'on' (obj)
		WHERE str ISA STRING
			ELSE "Please state inside double quotes ("""") 
				what you want to write."
		AND obj ISA OBJECT
			ELSE "Nothing can be written there."
	

	 write = write (str) 'in' (obj).


ADD TO EVERY OBJECT
  VERB write 
     WHEN obj 
        CHECK obj IS writeable 
		ELSE "Nothing can be written there."
	  AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	  AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	  DOES 	
		 IF text OF obj = ""
					THEN SET text OF obj TO str.
					ELSE SET text OF obj TO text OF obj + " " + str. 
		  END IF.
				"You write ""$$" SAY str. "$$"" on" SAY THE obj. "."
			   	MAKE obj readable.
  END VERB. 
END ADD TO.



-- ================================================================


----- YES


-- ================================================================


SYNTAX yes = yes.


VERB yes 
	DOES "You sound rather positive."
END VERB.



