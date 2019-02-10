-- put.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


SYNONYMS
	place = put.

SYNTAX
	put = put (obj) *
		WHERE obj ISA OBJECT
			ELSE "You can't put that anywhere."

Add To Every object
  VERB put
	CHECK obj IN HERO
		ELSE "You haven't got that."
	DOES
		LOCATE obj HERE.
		"Dropped."
  END VERB.
End Add To.




SYNTAX
	put_in = put (obj1) 'in' (obj2)
		WHERE obj1 ISA OBJECT
			ELSE "You can't put that anywhere."
		AND obj2 ISA CONTAINER
			ELSE "You can't put anything in that." 

Add To Every object
    Verb put_in
	When obj1
	    Check obj1 In hero
		Else
		    "You haven't got" Say The obj1. "."
	    And obj1 <> obj2
	        Else "You can't put something into itself!"
	    And obj2 <> hero
	        Else "You can't put" Say obj1. "into yourself!"
	    Does
	        Locate obj1 In obj2.
		"Done." 
    End Verb.
End Add To.



Syntax
    put_near = put (obj1) 'near' (obj2)
        Where obj1 Isa object
	    Else "You can't put that anywhere."
	And obj2 Isa thing
	    Else "You can't put anything near that."

    put_behind = put (obj1) behind (obj2)
        Where obj1 Isa object
	    Else "You can't put that anywhere."
	And obj2 Isa thing
	    Else "You can't put anything behind that."

    put_on = put (obj1) 'on' (obj2)
	Where obj1 Isa object
	    Else "You can't put that anywhere."
	And obj2 Isa thing
	    Else "You can't put anything on that."

    put_under = put (obj1) under (obj2)
        Where obj1 Isa object
	    Else "You can't put that anywhere."
	And obj2 Isa thing
	    Else "You can't put anything under that."

Add To Every object
    Verb put_near, put_behind, put_on, put_under
	When obj1
	    Check obj1 In hero
		Else
		    "You haven't got" Say The obj1. "."
	    And obj2 Not In hero
		Else
		    "You are carrying" Say The obj2.
		    ". If you want to take" Say the obj1. "just say so."
	    Does
		"Naaah. I'd rather just put" Say The obj1. "down here."
		Locate obj1 At obj2.
    End Verb.
End Add To.
