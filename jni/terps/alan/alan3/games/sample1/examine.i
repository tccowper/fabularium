-- examine.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


ADD TO EVERY THING 
IS
	examinable.
	searchable.
END ADD TO THING. 

ADD TO EVERY ACTOR 
IS
	NOT searchable.
END ADD TO ACTOR.

----

SYNONYMS
	x, inspect, 'check' = examine.

SYNTAX
	examine = examine (obj) *
		WHERE obj ISA THING
			ELSE "You can't examine that!"

SYNTAX
	look_at = 'look' 'at' (obj) *
		WHERE obj ISA THING
			ELSE "You can't examine that!"

Add To Every thing
  VERB examine, look_at
	CHECK obj IS examinable
		ELSE 
			"You can't examine" Say The obj. "."
	DOES
		"There is nothing special about" Say The obj. "."
  END VERB.
End Add To.


----

SYNONYMS
	inside, into = 'in'.

SYNTAX
	look_in = 'look' 'in' (obj) 
		WHERE obj ISA THING
			ELSE "You can't look inside that."
		AND obj ISA CONTAINER
			ELSE "You can't look inside that."

Add To Every object
  VERB look_in
	CHECK obj IS examinable
		ELSE 
			"You can't look inside" Say The obj. "."
	DOES
		LIST obj.
  END VERB.
End Add To.


----

SYNTAX
	search = search (obj) 
		WHERE obj ISA THING
			ELSE "You can't search that!"

Add To Every object
  VERB search
	CHECK obj IS searchable
		ELSE 
			"You can't search" Say The obj. "."
	DOES
		"You find nothing of interest."
  END VERB.
End Add To.

