-- ALAN NEW LIBRARY: VERBS (file name: verbs.i)

----- This library defines common verbs needed in gameplay. The verbs are listed alphabetically.
----- This file also includes common commands which are not actually verbs, such as ‘inventory’,
----- ‘verbose’ and ‘again’. You are free to edit this file for your own purposes in any way you 
----- like by adding, deleting or modifying verbs. Verbs originally defined in this file 
----- are the following:


----- about 	(+ help, info)
----- again 	(+ g)
----- answer 	(+ reply)
----- ask 		(+ enquire, inquire, interrogate)
----- attack 	(+ beat, fight, hit, kick, punch)
----- attack with 
----- bite   	(+ chew, taste)
----- break		(+ destroy)
----- brief 
----- burn 
----- burn with
----- buy 		(+ purchase)
----- catch 
----- clean		(+ polish, wipe)
----- climb
----- climb on
----- close 
----- close with
----- consult 
----- credits 	(+ acknowledgments, author)
----- dance	
----- dig
----- dive
----- dive in
----- drink 	
----- drive
----- drop		(+ discard, dump)
----- eat 		
----- empty
----- enter (+ obj)
----- examine	(+ check, inspect, observe, x)
-----	exit (+ obj)
----- extinguish	(+ put out, quench)
----- fill
----- fill with
----- find		(+ locate)
----- fire	
----- flip	
----- follow
----- free 		(+ release)
----- get up
----- get off
----- give
----- go to
----- hint		(+ hints)
----- inventory	(+ i, inv)
----- jump
----- jump in
----- jump on
----- kill 		(+ murder)
----- kill with
----- kiss		(+ hug, embrace)
----- lie down
----- lie in
----- lie on 
----- lift
----- light 	(+ lit)
----- listen 	('listen0')
----- listen to 	('listen')
----- lock
----- lock with
----- look		(+ gaze, peek)
----- look at
----- look behind
----- look in
----- look out of
----- look through
----- look under
----- look up
----- no
----- open
----- open with
----- play
----- play with
----- pour
----- pour in
----- pour on
----- pray
----- pry
----- pry with
----- pull
----- push
----- push with
----- put 		(+ place)
----- put behind
----- put down 	(works as 'drop')
----- put in	(+ insert)
----- put near
----- put on
----- put under
----- read
----- (remove)	(+ take off, doff) (in 'classes.i', subclass 'clothing')
----- restart
----- restore
----- rub
----- save
----- say
----- say to
----- score
----- search
----- sell
----- shake
----- shoot (at)
----- shoot with
----- shout 	(+ yell)
----- show
----- sing
----- sip
----- sit (down)
----- sit on
----- sleep		(+ rest)
----- smell 	('smell0')
----- smell (+ obj)
----- squeeze
----- stand (up)
----- stand on
----- swim
----- swim in
----- take		(+ carry, get, grab, hold, obtain)
----- take from	(+ remove from)
----- talk
----- talk to	(+ speak)
----- tear 		(+ rip)
----- think
----- think about	
----- throw
----- throw at
----- tie
----- tie to 
----- touch 	(+ feel)
----- turn
----- turn on	(+ switch on)	
----- turn off	(+ switch off)
----- (undress) 	(in ´classes.i', subclass 'clothing')
----- unlock
----- unlock with
----- use
----- use with
----- verbose
----- wait 		(+ z)
----- (wear)	(+ put on, don) (in 'classes.i', subclass 'clothing')
----- what am i
----- what is
----- where am i
----- where is
----- who am i
----- who is
----- write
----- yes



----- Directions (north, south, up, etc.) are declared in the file 'locations.i'.

----- Verbs connected with wearing clothes ('remove', 'undress' and 'wear', together with 
----- synonyms) are defined in the file 'classes.i', subclass 'clothing'.





----- We first declare some default attributes for things (= objects and actors).
----- These attributes are frequently checked in verb definitions, to hinder
----- the use of verbs with objects and actors in an irrational way. For example, the hero
----- won't be able to eat something that is not edible, and so forth.

----- Specific object and actor attributes that override or complete the following 
----- attributes are declared in 'classes.i'.




ADD TO EVERY THING

	IS examinable. 
	   inanimate.
	   movable.  
	   reachable. 	
	   takeable.

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

	HAS text "".

END ADD TO.



----- Next, we declare synonyms for some prepositions so that
----- it is possible for the player to type for example both "put ball in box" and
----- "put ball into box", etc.


SYNONYMS 


into, inside = 'in'.
onto = on.
thru = through.








----- The verbs:





-- =============================================================


----- ABOUT 
 

-- =============================================================


SYNTAX 'about' = 'about'.
	 

VERB 'about'
	DOES STYLE emphasized.
		"This is a text adventure, also called interactive fiction, which means that what
		goes on in the story depends on what you type at the prompt. Commands you can type 
		are for example GO NORTH (or NORTH  
	      or just N), WEST, SOUTHEAST, UP, IN etc for moving around, but you can try many
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


SYNTAX
	again = again.

VERB again
	DOES
		"[The AGAIN command is not supported in this game. You can use the 'up' and 'down' arrow 
		keys to scroll through your previous commands.]" 
END VERB.

SYNONYMS
	g = again.



-- =============================================================


----- ASK (= enquire, inquire, interrogate)


-- =============================================================


SYNTAX
  ask = ask (act) about (topic)!
    WHERE act ISA ACTOR
      	ELSE "That's not something you can talk to."
    AND topic ISA THING
      	ELSE "That doesn't seem to be something you can talk about with" SAY THE act. "."

ADD TO EVERY THING
  VERB ask
    WHEN act
      CHECK act HAS can_talk
        	ELSE "That's not something you can talk to."
	AND act <> hero
		ELSE "It doesn't make much sense to ask yourself about something."
      DOES
		IF topic IN act
			THEN SAY THE act. "doesn't seem to want to talk about" SAY THE topic. "."
	      ELSIF topic = act
			THEN SAY THE act. "chooses to be silent."
		ELSIF topic = hero
			THEN """I think you know more about yourself than what I do!""" SAY THE act. "snaps."
		ELSE """I don't know anything about" SAY THE topic. "$$!,""" SAY THE act. "snaps."
		END IF.
  END VERB.
END ADD TO.



SYNONYMS enquire, inquire, interrogate = ask.


----- note that 'consult' is defined separately






-- =============================================================


----- ATTACK (+ beat, fight, hit, kick, punch)


-- =============================================================


SYNTAX
  attack = attack (obj)
    WHERE obj ISA THING
      	ELSE "That’s not something you can attack."

ADD TO EVERY THING
  VERB attack
	CHECK obj IS examinable
		ELSE "That's not something you can $v."
	AND obj IS reachable
		ELSE SAY THE obj. "is too far away."
	AND obj <> hero 
		ELSE "It doesn't make sense to $v yourself."
	AND obj NOT IN hero
		ELSE "It doesn't make much sense to $v something you're holding."
	AND obj NOT IN worn
		ELSE "It doesn't make sense to $v something you're wearing."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to attack anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to attack anything while lying down."
    	DOES "That would be needlessly brutal."
  END VERB.
END ADD TO.


SYNONYMS
  beat, fight, hit, kick, punch = attack.





-- ==============================================================


----- ATTACK WITH


-- ==============================================================


SYNTAX
  attack_with = attack (obj1) 'with' (obj2)
    WHERE obj1 ISA THING
      	ELSE "That's not something you can attack."
    AND obj2 ISA WEAPON
     		ELSE 
			IF obj2 ISA ACTOR
		 		THEN "You cannot use" SAY THE obj2. "to attack anything."
				ELSE "There's no point attacking anything with" 
					SAY THE obj2. "."
			END IF.


ADD TO EVERY THING
  VERB attack_with
    WHEN obj1
	CHECK obj1 IS examinable
	  	ELSE "That's not something you can $v."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
      AND obj2 In hero
        	ELSE "You are not holding" SAY THE obj2. "."
	AND obj1 <> hero 
		ELSE "It doesn't make sense to $v yourself."
	AND obj1 NOT IN hero
		ELSE "It doesn't make much sense to $v something you're holding."
	AND obj1 NOT IN worn
		ELSE "It doesn't make sense to $v something you're wearing."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to attack anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to attack anything while lying down."
      DOES "That would be needlessly brutal."
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
	CHECK obj IN hero
		ELSE "You don't have" SAY THE obj. "."
	AND obj IS edible
		ELSE "That's not something you should $v."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "You take a bite of" SAY THE obj. ". It tastes rather good."
END VERB.
END ADD TO.


SYNONYMS chew, taste = bite.




-- ===============================================================


----- BREAK


-- ===============================================================


SYNTAX break = break (obj)
	WHERE obj ISA OBJECT
		ELSE 
			IF obj ISA ACTOR
				THEN "That would be needlessly brutal."
				ELSE "That's not something you can break."
			END IF.


ADD TO EVERY OBJECT
VERB break
	CHECK obj IS examinable
		ELSE "That's not something you can break."
	AND obj IS reachable 
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That would be needlessly brutal."
END VERB.
END ADD TO.


SYNONYMS destroy = break.





-- ================================================================


----- BRIEF


-- ================================================================


-- Use "Visits 0." or "Visits 1000." in the START section if you want
-- the game to start in verbose or brief mode. 



SYNTAX
	brief = brief.

VERB brief
	DOES
		Visits 1000.
		"Brief mode is now on. Location descriptions will only be shown
		the first time you visit."
END VERB brief.




-- =================================================================


----- BURN


-- =================================================================



SYNTAX
	burn = burn (obj)
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
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"You must state what you want to burn" SAY THE obj. "with."
  END VERB.
END ADD TO.




-- =================================================================


----- BURN WITH


-- =================================================================



SYNTAX
	burn_with = burn (obj1) 'with' (obj2)
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
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IN hero
		ELSE "You're not holding" SAY THE obj2. "."
	AND obj1 <> obj2 
		ELSE "It doesn't make sense to burn something with itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"You can't burn" SAY THE obj1. "with" SAY THE obj2. "."
  END VERB.
END ADD TO.




-- ==================================================================


----- BUY (+ purchase)


-- ==================================================================



SYNTAX
	buy = buy (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can buy."


ADD TO EVERY OBJECT
  VERB buy
	CHECK obj IS examinable
		ELSE "That's not something you can buy."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"That's not for sale."
  END VERB.
END ADD TO.


SYNONYMS purchase = buy.




-- ==================================================================


----- CATCH


-- ==================================================================




SYNTAX
	catch = catch (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can catch."


ADD TO EVERY THING
  VERB catch
	CHECK obj <> hero 
		ELSE "It doesn't make sense to $v yourself."
	AND hero IS NOT sitting
		ELSE "It is difficult to catch anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to catch anything while lying down."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
	      "That doesn't need to be caught."
  END VERB.
END ADD TO.



-- ==================================================================


----- CLEAN ( + wipe, polish)


-- ==================================================================


SYNTAX
	clean = clean (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can clean."


ADD TO EVERY OBJECT
  VERB clean
	CHECK obj IS examinable
		ELSE "That's not something you can $v."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"Nothing would be achieved by that."
  END VERB.
END ADD TO.


SYNONYMS wipe, polish = clean.


----- notice that 'rub' is defined separately





-- ==============================================================


----- CLIMB


-- ==============================================================



SYNTAX
	climb = climb (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can climb."


ADD TO EVERY OBJECT
  VERB climb
	CHECK obj IS examinable
		ELSE "That's not something you can climb."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to climb while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to climb while lying down."
	DOES "That's not something you can climb."
  END VERB.
END ADD TO.



-- ==============================================================


----- CLIMB ON


-- ==============================================================



SYNTAX
	climb_on = climb 'on' (obj)
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
	DOES "That's not something you can climb on."
  END VERB.
END ADD TO.




-- ==============================================================


----- CLOSE (+ shut)


-- ==============================================================




SYNTAX
    close = close (obj)
        	WHERE obj ISA OBJECT
	    		ELSE "That's not something you can close."

ADD TO EVERY OBJECT
VERB close
	CHECK obj IS closeable
	    ELSE "That's not something you can close."
	AND obj IS NOT closed
	    ELSE "It is already closed."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
	    MAKE obj closed.
	    "You close the" SAY THE obj. "."
END VERB.
END ADD TO.


SYNONYMS shut = close.




-- ==============================================================


----- CLOSE WITH


-- ==============================================================



SYNTAX
    close_with = close (obj1) 'with' (obj2)
        WHERE obj1 ISA OBJECT
	    Else "That's not something you can close."
	  AND obj2 ISA OBJECT
	    Else "You can't close anything with that."

ADD TO EVERY OBJECT
    VERB close_with
      WHEN obj1
	CHECK obj1 IS closeable
		ELSE "That's not something you can open."
	AND obj1 IS NOT closed
	    ELSE "It is already closed."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
	    "You can't close" SAY THE obj1. "with" SAY THE obj2. "."
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
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj1 <> hero 
		ELSE "It doesn't make sense to $v yourself about anything."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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


----- CREDITS (+ acknowledgments)


-- ==============================================================



SYNTAX
	credits = credits.

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


SYNONYMS acknowledgments, author = credits.




-- ==============================================================


----- DANCE


-- ==============================================================





SYNTAX
  dance = dance.


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
	DOES "There is nothing suitable to dig here."
END VERB.
END ADD TO.




-- ==============================================================


----- DIVE


-- ==============================================================




SYNTAX dive = dive.

VERB dive 	
	CHECK hero IS NOT sitting
		ELSE "It is difficult to dive anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to dive anywhere while lying down."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "There is no water suitable for swimming here."
END VERB.




-- ==============================================================


----- DIVE IN


-- ==============================================================




SYNTAX dive_in = dive 'in' (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can dive into."


ADD TO EVERY OBJECT
VERB dive_in
	CHECK hero IS NOT sitting
		ELSE "It is difficult to dive anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to dive anywhere while lying down."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That's not something you can dive into."
END VERB.
END ADD TO.





-- ==============================================================


----- DRINK 


-- ==============================================================


SYNTAX
drink = drink (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can drink."

ADD TO EVERY OBJECT
VERB drink
	CHECK obj IS drinkable
		ELSE "That's not something you can drink."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj IN hero	
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		LOCATE obj AT nowhere.
		"You drink all of" SAY THE obj. "."
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


SYNTAX
  drop = drop (obj)*
	WHERE obj ISA OBJECT
		ELSE "That's not something you can drop."
  drop = put (obj) * down.
  drop = put down (obj)*.


ADD TO EVERY OBJECT
  VERB drop
    CHECK obj IN hero
      ELSE "You aren't carrying" SAY THE obj. "."
    DOES
      LOCATE obj HERE.
      "Dropped."
  END VERB.
END ADD TO.

SYNONYMS
  discard, dump = drop.




-- ==============================================================


----- EAT 


-- ==============================================================



SYNTAX
	eat = eat (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can eat."



ADD TO EVERY OBJECT
  VERB eat
	CHECK obj IS edible
		ELSE "That's not something you can eat."
	AND obj IN hero
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		LOCATE obj AT nowhere.
		"You eat all of" SAY THE obj. "."
  END VERB.
END ADD.




-- ==============================================================


----- EMPTY 	


-- ==============================================================


SYNTAX 'empty' = 'empty' (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can empty."
	AND obj ISA CONTAINER
		ELSE "That's not something you can empty."



ADD TO EVERY OBJECT
VERB 'empty'
	CHECK obj IN hero
		ELSE "You're not holding" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES 
		"You empty the contents of" SAY THE THIS. "on the floor."
		LOCATE THIS AT hero.
END VERB.
END ADD TO.



-- ==============================================================


----- ENTER (+ obj)


-- ==============================================================



SYNTAX enter = enter (obj)
	WHERE obj ISA CONTAINER
		ELSE "That's not something you can enter."


ADD TO EVERY OBJECT
VERB enter
	CHECK hero IS NOT sitting
		ELSE "It is difficult to enter anything while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to enter anything while lying down."
    DOES "That's not something you can enter."
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



SYNTAX
	examine = examine (obj) 
		WHERE obj ISA THING
			ELSE "That's not something you can examine."


SYNTAX
	examine = 'look' 'at' (obj).


ADD TO EVERY THING
  VERB examine
    CHECK obj IS examinable
      ELSE "You can't examine" SAY THE obj. "."
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


----- EXIT (+ obj)


-- ==============================================================



SYNTAX 'exit' = 'exit' (obj)
	WHERE obj ISA CONTAINER
		ELSE "That's not something you can exit."


ADD TO EVERY OBJECT
VERB 'exit'
	CHECK hero IN obj
		ELSE "But you aren't in" SAY THE obj. "!"	
	DOES "You exit" SAY THE obj. "."
		LOCATE hero AT CURRENT LOCATION.
END VERB.
END ADD TO.



--- another 'exit' formulation added to guide players to use the right formulation:


SYNTAX exit_error = 'exit'.


VERB exit_error
	DOES "You must state what you want to exit."
END VERB.




-- ==============================================================


----- EXTINGUISH


-- ==============================================================




SYNONYMS quench = extinguish.


SYNTAX extinguish = extinguish (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can extinguish."
       extinguish = put 'out' (obj).


ADD TO EVERY OBJECT
VERB extinguish
	DOES "That's not on fire."
END VERB.
END ADD TO.



-- ==============================================================


----- FILL


-- ==============================================================




SYNTAX fill = fill (obj)
	WHERE obj ISA CONTAINER
		ELSE "That's not something you can fill."


ADD TO EVERY OBJECT
VERB fill
	DOES "You have to say what you want to fill" SAY THE obj. "with."
END VERB.
END ADD TO.




-- ==============================================================


----- FILL WITH


-- ==============================================================


SYNTAX fill_with = fill (obj1) 'with' (obj2)
	WHERE obj1 ISA CONTAINER
		ELSE "That's not something you can fill."
	AND obj2 ISA OBJECT
		ELSE "It's not possible to fill something with that."



ADD TO EVERY OBJECT
VERB fill_with
	WHEN obj1
	CHECK obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to fill something with itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "You fill" SAY THE obj1. "with" SAY THE obj2. "."
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
	CHECK obj NOT here
		ELSE "The" SAY obj. "is right here!"
	AND CURRENT LOCATION IS lit
		ELSE "It's too dark to find anything here."
	AND obj <> hero 
		ELSE "You're right here!"
	DOES
		"You'll have to find it yourself."
  END VERB.
END ADD TO.




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
		ELSE "You are not holding" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "You fire" SAY the obj. "into the air."
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
			ELSE "You are not holding" SAY THE obj1. "."
		AND obj2 <> hero 
			ELSE "There's no need to be that desperate."
		AND CURRENT LOCATION IS lit
			ELSE "It is too dark to see."
		DOES "That would be needlessly brutal."
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
	CHECK obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj IS broken
		ELSE "The" SAY obj. "doesn't need fixing."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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
	DOES ONLY "That's not something you need to flip."
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
	AND obj NOT AT hero
		ELSE SAY THE obj. "is right here."
	AND hero IS NOT sitting
		ELSE "It is difficult to follow anybody while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to follow anybody while lying down."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj NOT NEAR hero
		ELSE "You follow" SAY THE obj.
			LOCATE hero AT obj.
	DOES "You don't quite know where" SAY THE obj. "went. You must state a direction where you want to go."
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
	DOES "That doesn't need to be $vd."
END VERB.
END ADD TO.


SYNONYMS release = free.





-- ==============================================================


------ GET UP


-- ==============================================================


SYNTAX get_up = take up.		-- because 'get' is declared as a synonym of 'take' further below
		

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




SYNTAX
  give = 'give' (obj1) 'to' (obj2)
    WHERE obj1 ISA OBJECT
      ELSE "You can only give away objects."
    AND obj2 ISA ACTOR
      ELSE "That's not something you can give things to."
  give = give (obj2) (obj1).


ADD TO EVERY OBJECT
  VERB give
    WHEN obj1
      CHECK obj1 IN hero
		ELSE "You don't have" SAY THE obj1. "."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to give something to itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
      DOES
		IF obj2 = hero 
			THEN "You already have" SAY THE obj1. "!"
			ELSE "You give" SAY THE obj1. "to" SAY THE obj2. "."
	  			LOCATE obj1 IN obj2.
		END IF.
  END VERB.
END ADD TO.


SYNONYMS hand, offer = give.




-- ==============================================================


----- GO TO


-- ==============================================================


SYNTAX go_to = 'to' (obj)!				-- because 'go' is predefined in the parser, it can't be used 
	WHERE obj ISA THING				  -- in verb definitions
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
			ELSE "You can't see" SAY THE obj. "anywhere nearby. You must state a direction where you want to go."
		END IF.
END VERB.
END ADD TO.





-- ==============================================================


----- HELP -> see ABOUT


-- ==============================================================






-- ==============================================================


----- HINT (+ hints)


-- ==============================================================



SYNONYMS
	hints = hint.

SYNTAX
	hint = hint.

VERB hint
	DOES
		"Unfortunately hints are not available in this game."
END VERB.



-- ==============================================================


----- INVENTORY (+ i, inv)


-- ==============================================================



ADD TO EVERY THING
   IS weight 0.
END ADD TO THING.


ADD TO EVERY ACTOR IS
	weight 50. 
END ADD TO ACTOR. 


ADD TO EVERY OBJECT
IS
	weight 5.
END ADD TO OBJECT. 


SYNTAX
	i = i.

VERB i
		DOES LIST hero.

		IF COUNT IN worn > 0		-- see the file 'classes.i', subclass 'clothing'
			THEN LIST worn. 
		END IF.
	
END VERB.



SYNONYMS inv, inventory = i.




-- ==============================================================


----- JUMP


-- ==============================================================



SYNTAX
	jump = jump.

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




SYNTAX
	jump_in = jump 'in' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can jump into."
		AND obj ISA CONTAINER
			ELSE "That's not something you can jump into."

ADD TO EVERY OBJECT
  VERB jump_in
	CHECK obj IS reachable
		ELSE SAY THE obj. "is too far away."
	AND CURRENT LOCATION IS lit
		ELSE "It's too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to jump while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to jump while lying down."
	DOES
		IF obj ISA SUPPORTER
			THEN "That's not something you can jump into."
			ELSE "That wouldn't accomplish anything."
		END IF.
  END VERB.
END ADD TO.




-- ==============================================================


----- JUMP ON


-- ==============================================================



SYNTAX
	jump_on = jump 'on' (obj)
		WHERE obj ISA OBJECT
		      ELSE "That's not something you can jump on."
		AND obj ISA SUPPORTER
			ELSE "That's not something you can jump on."

ADD TO EVERY OBJECT
  VERB jump_on
	CHECK obj IS reachable
		ELSE SAY THE obj. "is too far away."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND hero IS NOT sitting
		ELSE "It is difficult to jump while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to jump while lying down."
	DOES
		"That wouldn't accomplish anything."
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
	DOES "That would be needlessly brutal."
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
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That would be needlessly brutal."
END VERB.
END ADD TO.




-- ==============================================================


----- KISS (+ hug, embrace)


-- ==============================================================



SYNTAX
	kiss = kiss (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can kiss."

ADD TO EVERY THING
VERB kiss
	CHECK obj IS examinable
		ELSE "That's not something you can kiss."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj <> hero
		ELSE "There is no time for that now."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		IF obj IS inanimate 
			THEN
				"You $v" SAY THE obj. "."
			ELSE 
				SAY THE obj. "avoids your advances."
				END IF.
END VERB.
END ADD TO.


SYNONYMS hug, embrace = kiss.





-- ==============================================================


----- KNOCK 


-- ==============================================================



SYNTAX
	knock = knock 'on' (obj)
		WHERE obj ISA OBJECT
			ELSE "That's not something you can knock on."

ADD TO EVERY OBJECT
  VERB knock
	CHECK obj IS examinable
		ELSE "That's not something you can knock on."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"You knock on" SAY THE obj. "$$. There's no reply."
  END VERB.
END ADD TO.


--- another 'knock' formulation added to guide players to use the right phrasing:

SYNTAX knock_error = knock.

VERB knock_error 
	DOES "You need to specify what to knock on."
END VERB.




-- ==============================================================


----- LIE DOWN


-- ==============================================================



SYNTAX lie_down = lie 'down'.
	 lie_down = lie.


VERB lie_down
	CHECK hero IS NOT lying_down
		ELSE "You're lying down already."
	DOES "There's no need to lie down right now."
END VERB.





-- ==============================================================


----- LIE IN


-- ==============================================================


SYNTAX lie_in = lie 'in' (obj)
	WHERE obj ISA CONTAINER
		ELSE "That's not something you can lie in."
	lie_in = lie 'down' 'in' (obj).
	

ADD TO EVERY OBJECT
VERB lie_in
	CHECK hero IS NOT lying_down
		ELSE "You're lying down already."
	DOES "There's no need to lie down in" SAY THE obj. "."
	-- If you need this to work, insert the following two lines instead of the above:
	-- DOES "You lie down in" SAY THE obj. "."
		-- LOCATE hero IN obj.
END VERB.
END ADD TO.




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
	DOES "There's no need to lie down on" SAY THE obj. "."
	-- If you need this to work, insert the following two lines instead of the above:
	-- DOES "You lie down on" SAY THE obj. "."
		-- LOCATE hero IN obj.    
END VERB.
END ADD TO.



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
	AND obj IS takeable
		ELSE "It's not possible to lift that."
	AND obj IS movable
		ELSE "It's not possible to lift that."
	AND obj IS inanimate
		ELSE SAY THE obj. "wouldn't probably appreciate that."
	AND obj NOT IN hero
		ELSE "You're already holding" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That wouldn't accomplish anything."
	
END VERB.
END ADD TO.



-- ==============================================================


----- LIGHT (+ lit)


-- ==============================================================



SYNTAX
	light = light (obj)
		WHERE obj ISA LIGHTSOURCE
			ELSE "That's not something you should lit."


ADD TO EVERY OBJECT
  VERB light
	CHECK obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"You don't have anything to light" SAY THE obj. "with."
  END VERB.
END ADD TO.


SYNONYMS lit = light.



-- ==============================================================


----- LISTEN


-- ==============================================================





SYNTAX
	listen0 = listen.


VERB listen0
	DOES
		"You hear nothing unusual."
END VERB.




-- ==============================================================


----- LISTEN TO


-- ==============================================================




SYNTAX
	listen = listen 'to' (obj)!
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


SYNTAX
  lock = lock (obj)
    WHERE obj ISA OBJECT
      ELSE "That's not something you can lock."


ADD TO EVERY OBJECT
    VERB lock
	CHECK obj IS lockable
	    ELSE "That's not something you can lock."
	AND obj IS NOT locked
	    ELSE "It's already locked."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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



SYNTAX
    lock_with = lock (obj1) 'with' (obj2)
	WHERE obj1 ISA OBJECT
	    ELSE "That's not something you can lock."
	AND obj2 ISA OBJECT
	    ELSE "You can't lock anything with that."

ADD TO EVERY OBJECT
    VERB lock_with
	WHEN obj1
	    CHECK obj1 IS lockable
		ELSE "That's not something you can lock."
	    AND obj1 IS NOT locked
		ELSE "It's already locked."
	    AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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



SYNONYMS
	l = 'look'.

SYNTAX
	'look' = 'look'.

VERB 'look'
	DOES
		LOOK.
END VERB.




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
			"You can't look behind" Say The obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	AND obj <> hero 
		ELSE "Turning your head, you notice nothing unusual behind yourself."
	DOES "You notice nothing unusual behind" SAY THE obj. "."
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
	AND obj IS NOT closed
		ELSE "You can't, since" SAY THE obj. "is closed."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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
			"You can't look out of" Say The obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That's not something you can look out of."
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
		ELSE 
			"You can't look through" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "You can't see through" SAY THE obj. "."
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
		ELSE 
			"You can't look under" SAY THE obj. "."
	AND obj <> hero
		ELSE "It doesn't make sense to look under yourself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "You notice nothing unusual under" SAY THE obj. "."
END VERB.
END ADD TO.




-- ==============================================================


----- LOOK UP -> see CONSULT


-- ==============================================================







-- ==============================================================


----- NO


-- ==============================================================



SYNTAX 'no' = 'no'.

VERB 'no'
	DOES "You sound rather negative."
END VERB.







-- ==============================================================


----- OPEN


-- ==============================================================



SYNTAX
  open = open (obj)
    WHERE obj ISA OBJECT
      ELSE "That's not something you can open."

ADD TO EVERY OBJECT
  VERB open
    CHECK obj IS closeable
      ELSE "That's not something you can open."
    AND obj IS closed
      ELSE "It's already open."
    AND obj IS reachable
	ELSE SAY THE obj. "is out of your reach."
    AND obj IS NOT locked
	ELSE SAY THE obj. "appears to be locked."
    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
    DOES
      MAKE obj NOT closed.
	"You open" SAY THE obj. "."
  END VERB.
END ADD TO.



-- ==============================================================


----- OPEN WITH


-- ==============================================================


SYNTAX
  open_with = open (obj1) 'with' (obj2)
    WHERE obj1 ISA OBJECT
      ELSE "That's not something you can open."
    AND obj2 ISA OBJECT
      ELSE "You can't open anything with" SAY THE obj2.

ADD TO EVERY OBJECT
    VERB open_with
	WHEN obj1
	    CHECK obj1 IS closeable
		  ELSE "That's not something you can open."
	    AND obj1 IS closed
		  ELSE SAY THE obj1. "is already open."
	    AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	    AND obj1 IS NOT locked
		ELSE SAY THE obj1. "appears to be locked."
	    AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2. "."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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
	DOES "That's not something you can play."
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
	DOES "You don't find it purposeful to start playing with" SAY THE obj. "."
END VERB.
END ADD TO.



-- ==============================================================


----- POUR 


-- ==============================================================



SYNTAX pour = pour (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can pour."



ADD TO EVERY OBJECT
VERB pour
	CHECK obj <> hero 
		ELSE "It doesn't make sense to $v yourself."
	AND obj IN hero
		ELSE "You're not holding" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That's not something you can pour."
END VERB.
END ADD TO.





-- ==============================================================


----- POUR IN


-- ==============================================================


SYNTAX pour_in = pour (obj1) 'in' (obj2)
	WHERE obj1 ISA OBJECT
		ELSE "That's not something you can pour."
	AND obj2 ISA CONTAINER
		ELSE "You can't pour anything into that."


ADD TO EVERY OBJECT
VERB pour_in
	WHEN obj1
	CHECK obj1 IN hero
		ELSE "You don't have" SAY THE obj1. "."
	AND obj1 NOT IN obj2
		ELSE SAY THE obj1. "is already in" SAY THE obj2. "."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to pour something into itself."
	AND obj2 IS NOT closed
		ELSE "You can't, since the" SAY THE obj2. "is closed."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "It's not possible to pour" SAY THE obj1. "into" SAY THE obj2. "."
		-- If you need to make this work, use the following lines instead:
		-- "You pour" SAY THE obj1. "into" SAY THE obj2. "."
		-- LOCATE obj1 IN obj2. 
END VERB.
END ADD TO.




-- ==============================================================


----- POUR ON


-- ==============================================================




SYNTAX pour_on = pour (obj1) 'on' (obj2)
	WHERE obj1 ISA OBJECT
		ELSE "That's not something you can pour."
	AND obj2 ISA THING
		ELSE "You can't pour anything onto that."


ADD TO EVERY OBJECT
VERB pour_on
	WHEN obj1
	DOES
		"That wouldn't accomplish anything."
		-- (or, to make it work, use the following lines instead of the above:
		-- "You pour" SAY THE obj1. "on" SAY THE obj2. ". Nothing happens."
			LOCATE obj1 AT hero.
END VERB.
END ADD TO.




-- ==============================================================


----- PRAY 


-- ==============================================================



SYNTAX pray = pray.

VERB pray
	DOES "Prayers don't seem to help right now."
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
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj2 IN hero
		ELSE "You don't have" SAY THE obj2.
	AND obj1 <> obj2 
		ELSE "How intelligent."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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
				THEN SAY THE obj. "wouldn't probably appreciate that."
				ELSE "That's not something you can pull."
			END IF.


ADD TO EVERY OBJECT
VERB pull
	CHECK obj IS movable
		ELSE "It's not possible to pull" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That wouldn't accomplish anything."
END VERB.
END ADD TO.




-- ==============================================================


----- PUSH


-- ==============================================================


SYNTAX
	push = push (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can push."


ADD TO EVERY OBJECT
    VERB PUSH
	CHECK obj IS movable
	    ELSE "That's not something you can push."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj NOT IN hero
		ELSE "But you're holding" SAY THE obj. "."
	AND obj <> hero
		ELSE "It doesn't make sense to push yourself."
	AND obj IS inanimate
		ELSE SAY THE obj. "wouldn't probably appreciate that."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
	    "You give" SAY THE obj. "a little push. Nothing happens."
    END VERB.
END ADD TO.




-- ==============================================================


----- PUSH WITH


-- ==============================================================




SYNTAX
    push_with = push (obj1) 'with' (obj2)
	WHERE obj1 ISA THING
	    ELSE "That's not something you can push."
	AND obj2 ISA OBJECT
	    ELSE "You can use only objects to push things with."


ADD TO EVERY OBJECT
    VERB push_with
	WHEN obj1
	CHECK obj1 IS movable
	        ELSE "That's not something you can push."
	AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	AND obj1 NOT IN hero
		ELSE "But you're holding" SAY THE obj1. "."
	AND obj2 IN hero
		ELSE "You're not holding" SAY THE obj2. "."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to push something with itself."
	AND obj1 <> hero
		ELSE "It doesn't make sense to push yourself with something."
	AND obj2 <> hero
		ELSE "It doesn't make sense to push something with yourself."
	AND obj1 IS inanimate
		ELSE SAY THE obj1. "wouldn't probably appreciate that." 
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"Using" SAY THE obj2. "you push" SAY THE obj1. "$$. Nothing happens."
    END VERB. 
END ADD TO.



-- ==============================================================


----- PUT (+ lay, locate, place)


-- ==============================================================



SYNTAX
	put = put (obj) *
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

SYNONYMS
	lay, 'locate', place = put.



-- ==============================================================


----- PUT DOWN	(works as  'drop')


-- ==============================================================


----- The syntax for 'put down' has been declared in the 'drop' verb.






-- ==============================================================


----- PUT IN	(+ insert)


-- ==============================================================



SYNTAX
	put_in = put (obj1) 'in' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE 
				IF obj1 ISA ACTOR
					THEN SAY THE obj1. "wouldn't probably appreciate that."
					ELSE "You can't put that anywhere."
				END IF.
		AND obj2 ISA CONTAINER
			ELSE "You can't put anything there." 
	put_in = insert (obj1) 'in' (obj2).
		


ADD TO EVERY OBJECT
    VERB put_in
	WHEN obj1
	    CHECK obj1 <> obj2
	        ELSE "It doesn't make sense to put something into itself."
	    AND obj1 <> hero
		  ELSE "You can't put yourself anywhere. If you want to enter something, just say so."
	    AND obj2 <> hero
	        ELSE "You can't put" SAY THE obj1. "into yourself! If you want to eat or drink something,
			just say so."
	    AND obj2 IS reachable
		  ELSE SAY THE obj2. "is out of your reach."
	    AND obj1 NOT IN obj2
		  ELSE SAY THE obj1. "is in" SAY THE obj2. "already!"
	    AND obj2 IS NOT closed
		  ELSE "You can't, since" SAY THE obj2. "is closed."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	    DOES
		  IF obj1 NOT IN hero
			THEN
				IF obj1 IS reachable AND obj1 IS takeable
					THEN "(taking" SAY THE obj1. "first)$n"
						LOCATE obj1 IN hero.
						LOCATE obj1 IN obj2.
						"Done."
					ELSE "You haven't got" SAY THE obj1. "."
				END IF.
		  	ELSE 
				LOCATE obj1 IN obj2.
				"Done."
		  END IF.
    END VERB.
END ADD TO.



-- ==============================================================


----- PUT BEHIND, NEAR, UNDER


-- ==============================================================


SYNTAX
    put_near = put (obj1) 'near' (obj2)
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
	    CHECK obj1 IN hero
		  ELSE
		    "You haven't got" SAY THE obj1. "."
	    AND obj2 NOT IN hero
		  ELSE
		    "That would be futile."
	    AND obj2 IS reachable
		   ELSE SAY THE obj2. "is out of your reach."
	    AND obj1 <> obj2
		   ELSE "That doesn't make sense."
	    AND obj2 <> hero
		   ELSE "That would be futile."
	    AND CURRENT LOCATION IS lit
		    ELSE "It is too dark to see."
	    DOES
		"That wouldn't accomplish anything."
    END VERB.
END ADD TO.




-- ==============================================================


----- PUT_ON


-- ==============================================================


-- to use this verb in the meaning 'wear', see the file 'classes.i', subclass 'clothing', verb 'wear'.



SYNTAX put_on = put (obj1) 'on' (obj2)
	WHERE obj1 ISA OBJECT
	    ELSE
			IF obj1 ISA ACTOR
				THEN SAY THE obj1. "wouldn't probably appreciate that."
				ELSE "You can't put that anywhere."
			END IF.
	AND obj2 ISA THING
	    ELSE "You can't well put anything on that."	
      AND obj2 ISA SUPPORTER
	    ELSE 
			IF obj2 ISA ACTOR
				THEN SAY THE obj2. "might not appreciate that very much."
				ELSE "You can't well put anything on that."
			END IF.	 


ADD TO EVERY OBJECT
    VERB put_on
	WHEN obj1
	    CHECK obj2 NOT IN hero
		  ELSE
		    "That would be futile."
	    AND obj2 IS reachable
		   ELSE SAY THE obj2. "is out of your reach."
	    AND obj1 <> obj2
		   ELSE "That doesn't make sense."
	    AND obj2 <> hero
		   ELSE "That would be futile."
	    AND obj1 NOT IN obj2
		   ELSE SAY THE obj1. "is on" SAY THE obj2. "already."
	    AND CURRENT LOCATION IS lit
		    ELSE "It is too dark to see."
	    DOES	  
		IF obj1 NOT IN hero
			THEN
				IF obj1 IS reachable AND obj1 IS takeable
					THEN "(taking" SAY THE obj1. "first)$n"
						LOCATE obj1 IN obj2.
						"Done."
					ELSE "You haven't got" SAY THE obj1. "."
				END IF.
		  	ELSE 
				LOCATE obj1 IN obj2.
				"Done."
		 END IF.
    END VERB.
END ADD TO.




-- ==============================================================


----- PUT OUT  (works as EXTINGUISH)


-- ==============================================================





-- ==============================================================


----- QUIT


-- ==============================================================




SYNONYMS q = 'quit'.

SYNTAX
	'quit' = 'quit'.

VERB 'quit'
	DOES
		QUIT.
END VERB.



-- ==============================================================


----- READ


-- ==============================================================



SYNTAX
    read = read (obj)
	WHERE obj ISA OBJECT
	    ELSE "You can't read that."

ADD TO EVERY OBJECT
    VERB read
	CHECK obj IS examinable
		ELSE "That's not something you can read."
	AND obj IS readable
	    ELSE
		"There is nothing written on" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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





-- ==============================================================


----- RESTART


-- ==============================================================



SYNTAX
	'restart' = 'restart'.

VERB 'restart'
	DOES
		RESTART.
END VERB.


-- ==============================================================


----- RESTORE


-- ==============================================================



SYNTAX
	'restore' = 'restore'.

VERB 'restore'
	DOES
		RESTORE.
END VERB.



-- ==============================================================


----- RUB (+ massage)


-- ==============================================================




SYNTAX
	rub = rub (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can rub."


ADD TO EVERY THING
  VERB rub
	CHECK obj IS examinable
		ELSE "That's not something you can rub."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj <> hero
		ELSE "There's no time for that now."
	AND obj IS inanimate
		ELSE "You aren't sure whether" SAY THE obj. "would appreciate that."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"Nothing would be achieved by that."
  END VERB.
END ADD TO.


SYNONYMS massage = rub.



-- ==============================================================


----- SAVE


-- ==============================================================


SYNTAX
	'save' = 'save'.

VERB 'save'
	DOES
		SAVE.
END VERB.



-- ==============================================================


----- SAY


-- ==============================================================



SYNTAX
  say_word = 'say' (topic)!
    WHERE topic ISA THING
      ELSE "You can't say that."

ADD TO EVERY THING
  VERB say_word
    DOES
      SAY THE topic. "$$? That's a nice word!"
  END VERB.
END ADD TO.



-- ==============================================================


----- SAY TO


-- ==============================================================



SYNTAX
  say_to = 'say' (topic)! 'to' (act)
    WHERE topic ISA THING
      ELSE "Nothing happens."
    AND act ISA ACTOR
      Else "That's not something you can talk to."

ADD TO EVERY THING
  VERB say_to
    WHEN act
      CHECK act HAS can_talk
		ELSE "That's not something you can talk to."
      DOES
		SAY THE act. "doesn't seem interested."
  END VERB.
END ADD TO.




-- ==============================================================


----- SCORE


-- ==============================================================



SYNTAX
	'score' = 'score'.

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


SYNTAX
	scratch = scratch (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can scratch."


ADD TO EVERY THING
  VERB scratch
	CHECK obj IS examinable
		ELSE "That's not something you can scratch."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj <> hero
		ELSE "That wouldn't help matters."
	AND obj IS inanimate
		ELSE SAY THE obj. "wouldn't probably appreciate that."
	DOES
		"Nothing would be achieved by that."
  END VERB.
END ADD TO.





-- ==============================================================


------ SEARCH


-- ==============================================================




SYNTAX
	search = search (obj) 
		WHERE obj ISA THING
			ELSE "That's not something you can search."


ADD TO EVERY THING
  VERB search
	CHECK obj <> hero
		ELSE LIST hero.
	AND obj IS inanimate
		ELSE SAY THE obj. "would probably object to that."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit			
		ELSE "You can't see anything in the dark."

	DOES 
		"You find nothing of interest."
  END VERB.
END ADD TO.





-- ==============================================================


----- SELL


-- ==============================================================


SYNTAX
	sell = sell (obj)
		WHERE obj ISA THING
			ELSE "That's not something you can sell."


ADD TO EVERY THING
  VERB sell
	CHECK obj IS examinable
		ELSE "That's not something you can sell."
	AND obj <> hero 
		ELSE "It doesn't make sense to sell yourself."
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
				THEN SAY THE obj. "wouldn't probably appreciate that."
				ELSE "That's not something you can shake."
			END IF.



ADD TO EVERY OBJECT
VERB shake
	CHECK obj IN hero
		ELSE "You're not holding" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "You shake" SAY THE obj. "a bit. Nothing happens."
END VERB.
END ADD TO.





-- ==============================================================


----- SHOOT (AT)


-- ==============================================================



SYNTAX
  shoot = shoot (obj)
    WHERE obj ISA THING
      ELSE "That's not something you can shoot at."
  shoot = shoot 'at' (obj).


ADD TO EVERY THING
  VERB shoot
	CHECK obj <> hero 
		ELSE "There's no need to be that desperate."
	DOES "You must state what you want shoot" SAY THE obj. "with."
  END VERB.
END ADD TO.




-- ==============================================================


----- SHOOT WITH


-- ==============================================================


SYNTAX
  shoot_with = shoot (obj1) 'with' (obj2)
    WHERE obj1 ISA THING
      ELSE "That's not something you can shoot."
    AND obj2 ISA WEAPON
      ELSE "That's not something you can shoot with."

ADD TO EVERY THING
  VERB shoot_with
    WHEN obj1
      CHECK obj2 IN hero
        ELSE "You are not carrying" SAY THE obj2. "."
	AND obj1 IS examinable
		ELSE "That's not something you can shoot."
	AND obj1 <> hero 
		ELSE "There's no need to be that desperate."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to shoot something with itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
      DOES
        "That would be needlessly brutal."
  END VERB.
END ADD TO.




-- ==============================================================


----- SHOUT


-- ==============================================================




SYNTAX
  shout = shout.

VERB shout
  DOES
    "Nothing results from your $ving."
END VERB.

SYNONYMS
  yell, scream = shout.





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
	CHECK obj1 <> hero
		ELSE "It doesn't make sense to show something to youself."
	AND obj2 <> hero 
		ELSE "It doesn't make sense to show something to yourself."
	AND obj1 <> obj2
		ELSE "It doesn't make sense to show something to itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES SAY THE obj2. "is not especially interested."
END VERB.
END ADD TO.






-- ==============================================================


----- SING (+ hum, whistle)


-- ==============================================================




SYNTAX
  sing = sing.

VERB sing
  DOES
    "You $v a little tune."
END VERB.

SYNONYMS
  hum, whistle = sing.





-- ==============================================================


----- SIP 


-- ==============================================================


SYNTAX
sip = sip (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can drink."

ADD TO EVERY OBJECT
VERB sip
	CHECK obj IS drinkable
		ELSE "That's not something you can drink."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND obj IN hero	
		ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		"You take a sip of" SAY THE obj. ". It tastes rather good."
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
	DOES "You feel no urge to sit down at present."
		-- (or, if you wish to make it work, use the following instead of the above:
		-- "You sit down."
		-- MAKE hero sitting.
END VERB.




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
	DOES "You feel no urge to sit down at present."
		-- (or, to make it work, use the following instead of the above:)
		-- "You sit down on" SAY THE obj. "."
		-- LOCATE hero IN obj.
		-- MAKE hero sitting.
END VERB.
END ADD TO.





-- ==============================================================


----- SLEEP	(+ rest)


-- ==============================================================



SYNTAX sleep = sleep.


VERB sleep
	DOES "There's no need to $v right now."
END VERB.


SYNONYMS rest = sleep.





-- ==============================================================


----- SMELL (smell0)


-- ==============================================================



SYNTAX
    smell0 = smell.

VERB smell0
    DOES
	"You smell nothing unusual."
END VERB.



-- ==============================================================


----- SMELL (+ obj)


-- ==============================================================


SYNTAX
    smell = smell (obj)!
	WHERE obj ISA THING
	    ELSE "That's not something you can smell."


ADD TO EVERY THING
    VERB smell
	DOES
	    IF obj AT hero
			THEN "You smell nothing unusual."
	    ELSIF obj NEAR hero
			THEN "You smell nothing unusual."
	    ELSE "You can't smell" SAY THE obj. "from here."
	    END IF.
    END VERB.
END ADD TO.




-- ==============================================================


----- SQUEEZE


-- ==============================================================


SYNTAX
    squeeze = squeeze (obj)
	WHERE obj ISA THING
	    Else "That's not something you can squeeze."


ADD TO EVERY THING
    VERB squeeze
	CHECK obj IS examinable
		ELSE "That's not something you can squeeze."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE 
			IF obj = hero
				THEN "Nothing would be achieved by that."
				ELSE "It is too dark to see."
			END IF.
	DOES
	    "Nothing would be achieved by that."
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
	DOES "You're standing up already."
END VERB.



-- ==============================================================


----- STAND_ON


-- ==============================================================


SYNTAX stand_on = stand 'on' (obj)
	WHERE obj ISA SUPPORTER
		ELSE "That's not something you can stand on."

       stand_on = take 'on' (obj).  -- because 'get' is declared a synonym of 'take'
						-- further below

ADD TO EVERY SUPPORTER
VERB stand_on
	DOES "You feel no urge to stand on" SAY THE obj. "."
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
	DOES "There is no water suitable for swimming here."
END VERB.




-- ==============================================================


----- SWIM IN


-- ==============================================================



SYNTAX swim_in = swim 'in' (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can swim in."


ADD TO EVERY OBJECT
VERB swim_in
	CHECK hero IS NOT sitting
		ELSE "It is difficult to swim anywhere while sitting down."
	AND hero IS NOT lying_down
		ELSE "It is difficult to swim anywhere while lying down."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That's not something you can swim in."
END VERB.
END ADD TO.




-- ==============================================================


----- TAKE


-- ==============================================================




SYNTAX
  take = take (obj)
    WHERE obj ISA OBJECT
      ELSE 
		IF obj ISA ACTOR
			THEN SAY THE obj. "would probably object to that."
			ELSE "That's not something you can take."
		END IF.


SYNTAX
  pick_up1 = pick up (obj)
    WHERE obj ISA OBJECT
      ELSE "That's not something you can pick up."

  pick_up2 = pick (obj) up
    WHERE obj ISA OBJECT
      ELSE "That's not something you can pick up."


ADD TO EVERY OBJECT
  VERB take, pick_up1, pick_up2
    CHECK obj IS movable
	ELSE SAY THE obj. "is much too heavy for you to move."
    AND obj IS takeable
      ELSE "That's not something you can take."
    AND obj IS reachable
	ELSE SAY THE obj. "is out of your reach."
    AND obj IS inanimate
	ELSE SAY THE obj. "would probably object to that."
    AND weight Of obj <=50
      ELSE SAY THE obj. "is too heavy to lift."
    AND CURRENT LOCATION IS lit
	ELSE "It is too dark to see."
    DOES
	IF obj IN hero
		THEN 
			IF obj IN worn
				THEN 
					IF obj ISA CLOTHING
						THEN LOCATE obj IN hero.
							"You take off" SAY THE obj. "and carry it in your hands."
						ELSE LOCATE obj IN hero.
							"Taken." 
					END IF.
				ELSE "You already have" SAY THE obj. "."
			END IF.
		ELSE LOCATE obj IN hero.
			"Taken."
	END IF.
  END VERB.
END ADD TO.


SYNONYMS
  carry, get, grab, hold, obtain = take.





-- ==============================================================


-----  TAKE FROM


-- ==============================================================



SYNTAX
  take_from = 'take' (obj1) 'from' (obj2)
    WHERE obj1 ISA OBJECT
      ELSE "You can only take objects."
    AND obj2 ISA THING
      ELSE "It's not possible to take things from there."
    AND obj2 ISA CONTAINER
      ELSE "It's not possible to take things from there."

  take_from = remove (obj1)* 'from' (obj2).


ADD TO EVERY OBJECT
VERB take_from
    WHEN obj1
	CHECK obj2 <> hero
		ELSE "You can't take things from yourself!"
	AND obj2 ISA OBJECT
		ELSE SAY THE obj2. "rather holds on to"
			IF obj2 ISA FEMALE
				THEN "her"
			ELSIF obj2 ISA MALE
				THEN "his"
			ELSE "its"
			END IF.
			"belongings."
	AND obj2 NOT IN hero
		ELSE 
			LOCATE obj1 IN hero.
	    		"You take" SAY THE obj1. "from" SAY THE obj2. "."
			MAKE obj1 reachable.
      AND obj1 NOT IN hero 		
	  	ELSE	"You already have" SAY THE obj1. "."
	AND obj1 <> obj2
		ELSE "You can't take something from itself!"
	AND obj1 IN obj2
		ELSE
			IF obj2 IS inanimate
	  			THEN SAY THE obj1. "is not there."
				ELSE SAY THE obj2. "doesn't have" SAY THE obj1. "."
			END IF.
	AND obj1 IS reachable
		ELSE 
			IF obj2 ISA ACTOR
				THEN "Maybe it would be polite to ask" SAY THE obj2. "first."
				ELSE SAY THE obj1. "is out of your reach."
			END IF.
	AND obj2 IS NOT closed
		  ELSE "You can't, since" SAY THE obj2. "is closed."
	AND obj1 IS takeable
		ELSE "That's not something you can take."
	AND obj1 IS movable
		ELSE SAY THE obj1. "is much too heavy for you to take."
	AND weight Of obj1 <=50
      	ELSE SAY THE obj1. "is too heavy."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
		LOCATE obj1 IN hero.
	    	"You take" SAY THE obj1. "from" SAY THE obj2. "."
		MAKE obj1 reachable.
	
END VERB.
END ADD TO.



-- ==============================================================


----- TALK


-- ==============================================================



SYNTAX
  talk_to = talk 'to' (act) about (topic)!
    WHERE topic ISA THING
      ELSE "That's not something you can ask about."
    AND act ISA ACTOR
      ELSE "That's not something you can talk to."
  talk_to = tell (act) about (topic)!.

ADD TO EVERY THING
  VERB talk_to
    WHEN topic
      CHECK act HAS can_talk
		ELSE "That's not something you can talk to."
      DOES
		SAY THE act. "doesn't seem interested."
  END VERB.
END ADD TO.


SYNTAX
  talk_to_a = talk 'to' (act)
    WHERE act ISA actor
      ELSE "You can't talk to that."

ADD TO EVERY THING
  VERB talk_to_a
    CHECK act HAS can_talk
      ELSE "You can't talk to that."
    DOES
      SAY THE act. "looks at you, seemingly wondering if you have
	  anything specific to talk about."
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
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "That's not a good idea."
END VERB.
END ADD TO.




-- ==============================================================


----- THINK		(+ ponder)


-- ==============================================================



SYNTAX think = think.

VERB think 
	DOES "Nothing helpful comes to your mind."
END VERB.



SYNONYMS ponder = think.




-- ==============================================================


----- THINK ABOUT


-- ==============================================================



SYNTAX think_about = think 'about' (obj)
	WHERE obj ISA THING
		ELSE "That's not something fruitful to think about."


ADD TO EVERY THING
VERB think_about
	DOES "Nothing helpful comes to your mind."
END VERB.
END ADD TO.



-- ==============================================================


----- THROW   (+ cast)


-- ==============================================================




SYNTAX
	throw = throw (obj) *
		WHERE obj ISA OBJECT
			ELSE "You can only throw objects."


ADD TO EVERY OBJECT
    VERB throw
	CHECK obj IN hero
	    ELSE "You don't have" SAY THE obj. "."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
	    "You can't throw very far;" SAY THE obj. "ends up on the"
		IF floor HERE
			THEN "floor"
			ELSE "ground"
		END IF.
		"nearby."
	    LOCATE obj HERE.
    END VERB.
END ADD TO.


SYNONYMS
	cast = throw.





-- ==============================================================


----- THROW AT


-- ==============================================================



SYNTAX
    throw_at = throw (obj1) 'at' (obj2)
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
	    CHECK obj1 IN hero
		ELSE "You are not holding" SAY THE obj1. "."
	    AND obj1 <> obj2
		ELSE "It doesn't make sense to throw something at itself."
	    AND obj2 NOT IN hero
	        ELSE
		    "You are carrying" SAY THE obj2. "."
	    AND obj2 <> hero
		   ELSE "You cannot throw things at yourself."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	    DOES 
		  IF obj2 IS inanimate
			THEN
	        		SAY THE obj1. "bounces harmlessly off"
		  		SAY THE obj2. "and ends up on the"
		  			IF floor HERE
						THEN "floor"
						ELSE "ground"
		  			END IF.
		     			"nearby."
		  			LOCATE obj1 HERE.
			ELSE SAY THE obj2. "catches" SAY THE obj1. "and tosses"
				IF THIS IS NOT plural
					THEN "it"
					ELSE "them"
				END IF.
				"back to you. You grab hold of it again."
		   END IF.
    END VERB.
END ADD TO.



SYNTAX
    throw_in = throw (obj1) 'in' (obj2)
	WHERE obj1 ISA OBJECT
	    ELSE "That's not something you can throw."
	AND obj2 ISA OBJECT
	    ELSE "That's not something you can throw things into."
	AND obj2 ISA CONTAINER
	    ELSE "That's not something you can throw things into."


ADD TO EVERY OBJECT
    VERB throw_in
	WHEN obj1
	    CHECK obj1 NOT IN obj2
		  ELSE SAY THE obj1. "is in" SAY THE obj2. "already!"
	    AND obj1 IN hero
	        ELSE "You are not holding" SAY THE obj1. "."
	    AND obj1 <> obj2
		ELSE "It doesn't make sense to throw something into itself."
	    AND obj2 <> hero
	        ELSE "You can't throw" SAY THE obj1. "into yourself! If you wish to eat or drink
			something, just say so."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	    DOES
		LOCATE obj1 IN obj2.
		"Done."
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
	DOES "You must state where do you want to tie" SAY obj. "."
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
	CHECK obj1 IN hero
		ELSE "You don't have" SAY THE obj1.
	AND obj1 <> obj2
		ELSE "It doesn't make sense to tie something to itself."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES "It's not possible to tie" SAY THE obj1. "to" SAY THE obj2. "."
END VERB.
END ADD TO.





-- ==============================================================


----- TOUCH


-- ==============================================================


SYNTAX
    touch = touch (obj)
	WHERE obj ISA THING
	    ELSE "That's not something you can touch."

    

ADD TO EVERY THING
    VERB touch
        CHECK obj IS examinable
            Else "That's not something you can touch."
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
	    		Else "That's not something you can touch."
		AND obj2 ISA OBJECT
	    		ELSE "You can only use objects to touch with."

ADD TO EVERY THING
VERB touch_with
	WHEN obj1
	    CHECK obj1 IS examinable
	        ELSE "That's not something you can touch."
	    AND obj1 IS reachable
		  ELSE SAY THE obj1. "is out of your reach."
	    AND obj1 <> obj2
	        ELSE "It doesn't make sense to touch something with itself."
	    AND obj1 IS inanimate
		  ELSE "You are not sure whether" SAY THE obj1. "would appreciate that."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	    DOES
	        "You touch" SAY THE obj1. "with" SAY THE obj2. ". Nothing special happens."
END VERB.
END ADD TO.



-- ==============================================================


----- TURN


-- ==============================================================


SYNTAX turn = turn (obj)
	WHERE obj ISA OBJECT
		ELSE "That's not something you can turn."

ADD TO EVERY OBJECT
VERB turn
	CHECK obj IS examinable
		ELSE "That's not something you can turn."
	AND obj IS movable
		ELSE "It's not possible to turn" SAY THE obj. "."
	AND obj IS reachable
		ELSE SAY THE obj. "is too far away."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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

----- Only devices and lightsources can be turned on and off. These subclasses are defined in 'classes.i'
----- with proper checks for 'on' and 'NOT on', 'lit' and 'NOT lit'. Trying to turn on or off 
----- an ordinary object will default here to "You can't turn that on".





SYNTAX
    turn_on1 = turn 'on' (obj)
	WHERE obj ISA OBJECT
	    Else "That's not something you can turn on."

    turn_on2 = turn (obj) 'on'
	WHERE obj ISA OBJECT
	    Else "That's not something you can turn on."

    switch_on1 = switch 'on' (obj)
	WHERE obj ISA OBJECT
	    ELSE "That's not something you can switch on."

    switch_on2 = switch (obj) 'on'
	WHERE obj ISA OBJECT
	    ELSE "That's not something you can switch on."


ADD TO EVERY OBJECT
    VERB turn_on1, turn_on2, switch_on1, switch_on2
	CHECK obj IS examinable
		ELSE "That's not something you can turn off."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"That's not something you can $v on."
    END VERB.
END ADD TO.



-- ==============================================================


----- TURN OFF


-- ==============================================================


----- Only devices and lightsources can be turned on and off. These subclasses are defined in 'classes.i'
----- with proper checks for 'on' and 'NOT on', 'lit' and 'NOT lit'. 



SYNTAX
    turn_off1 = turn off (obj)
	WHERE obj ISA OBJECT
	    ELSE "You can't turn that off."

    turn_off2 = turn (obj) off
	WHERE obj ISA OBJECT
	    ELSE "You can't turn that off."

    switch_off1 = switch off (obj)
	WHERE obj ISA OBJECT
	    ELSE "You can't switch that off."

    switch_off2 = switch (obj) off
	WHERE obj ISA OBJECT
	    ELSE "You can't switch that off."


ADD TO EVERY OBJECT
    VERB turn_off1, turn_off2, switch_off1, switch_off2
	CHECK obj IS examinable
		ELSE "That's not something you can turn off."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	DOES
		"That's not something you can $v off."
    END VERB.
END ADD TO.




-- ==============================================================


----- USE


-- ==============================================================


SYNTAX
	'use' = 'use' (obj)
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
	    Else "You can only use objects."
	AND obj2 ISA OBJECT
	    Else "You can only use objects."

ADD TO EVERY OBJECT
VERB use_with
	WHEN obj1
	CHECK obj1 <> obj2 
		ELSE "You can't use something with itself."
	DOES "Please be more specific. How do you intend to use them together?"
END VERB.
END ADD TO.




-- ==============================================================


----- UNDRESS


-- ==============================================================




-- See the file 'classes.i', subclass 'clothing' for the definition
-- of this verb.




-- ==============================================================


----- UNLOCK


-- ==============================================================




SYNTAX
    unlock = unlock (obj)
        WHERE obj ISA OBJECT
	    ELSE "That's not something you can unlock."


ADD TO EVERY OBJECT
    VERB unlock
	CHECK obj IS lockable
	    ELSE "That's not something you can unlock."
	AND obj IS locked
	    ELSE "It's already unlocked."
	AND obj IS reachable
		ELSE SAY THE obj. "is out of your reach."
	AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	DOES
	    "You must state what you want to unlock" SAY THE obj. "with."
    END VERB.
END ADD TO.




-- =============================================================


----- UNLOCK WITH


-- =============================================================



SYNTAX
    unlock_with = unlock (obj1) 'with' (obj2)
	WHERE obj1 ISA OBJECT
	    ELSE "That's not something you can unlock."
	And obj2 ISA OBJECT
	    ELSE "You can't unlock anything with that."



ADD TO EVERY OBJECT
    VERB unlock_with
        WHEN obj1
	    CHECK obj1 IS lockable
	        ELSE "That's not something you can unlock."
	    AND obj1 IS locked
		ELSE "It's already unlocked."
	    AND obj1 IS reachable
		ELSE SAY THE obj1. "is out of your reach."
	    AND obj2 In hero
		ELSE
		    "You don't have" SAY THE obj2. "."
	    AND obj1 <> obj2
		ELSE "It doesn't make sense to unlock something with itself."
	    AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
	  DOES
		SAY THE obj2. "doesn't unlock" SAY THE obj1.
    END VERB.
END ADD TO.




-- ==============================================================


----- VERBOSE (see also -> BRIEF)


-- ==============================================================



SYNTAX
	verbose = verbose.

VERB verbose
	DOES
		VISITS 0.
		"Verbose mode is now on. Location descriptions will be always shown in full."
END VERB verbose.





-- ==============================================================


----- WAIT (= z)


-- ==============================================================



SYNTAX
	'wait' = 'wait'.

VERB 'wait'
	DOES
		"Time passes..."
END VERB.

SYNONYMS
	z = 'wait'.





-- ==============================================================


----- WEAR


-- ==============================================================





-- See the file 'classes.i', subclass 'clothing' for definitions of the verbs
-- 'wear', (put on), 'remove', (take off) and 'undress'.







-- ==============================================================


----- WHAT AM I


-- ==============================================================




SYNTAX what_am_i = 'what' am i.


VERB what_am_i
	DOES "Maybe examining yourself might help."
END VERB.




-- ==============================================================


----- WHAT IS


-- ==============================================================



SYNTAX
	what_is = 'what' 'is' (obj)!
		WHERE obj ISA THING
			ELSE "That's not something I know about."


ADD TO EVERY THING
VERB what_is
	DOES "You'll have to find it out yourself."
END VERB.
END ADD TO.



-- ==============================================================


----- WHERE AM I


-- ==============================================================


SYNTAX where_am_i = 'where' am i.

VERB where_am_i
	DOES LOOK.
END VERB.



-- ==============================================================


----- WHERE IS


-- ==============================================================



SYNTAX
	where_is = 'where' 'is' (obj)!
		WHERE obj ISA THING
			ELSE "That's not something I know about."


ADD TO EVERY THING
VERB where_is 
	CHECK obj NOT AT hero
		ELSE "That's right here!"
	DOES "You'll have to find it out yourself."
END VERB.
END ADD TO.


-- ==============================================================


----- WHO AM I


-- ==============================================================


SYNTAX who_am_i = who am i.

VERB who_am_i 
	DOES "Maybe examining yourself might help."
END VERB.



-- ==============================================================


----- WHO IS


-- ==============================================================



SYNTAX
	who_is = 'who' 'is' (act)!
		WHERE act ISA ACTOR
			ELSE "That's not somebody I know."


ADD TO EVERY ACTOR
VERB who_is
	DOES "You'll have to find it out yourself."
END VERB.
END ADD TO.



-- ==============================================================


----- WRITE


-- ==============================================================



SYNTAX write = write (str) 'on' (obj)
	WHERE str ISA STRING
		ELSE "Please state inside double quotes ("""") what you want to write."
	AND obj ISA OBJECT
		ELSE "Nothing can be written there."
	write = write (str) 'in' (obj).




ADD TO EVERY OBJECT
VERB write 
     WHEN obj 
        CHECK obj IS writeable 
		ELSE "Nothing can be written there."
	  AND obj IS reachable
		ELSE "You can't reach" SAY THE obj. "."
	  AND CURRENT LOCATION IS lit
		ELSE "It is too dark to see."
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




