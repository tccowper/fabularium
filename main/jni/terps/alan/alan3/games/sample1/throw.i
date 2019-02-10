-- throw.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3

SYNONYMS
	dump, cast = throw.


SYNTAX
	throw = throw (obj) *
		WHERE obj ISA OBJECT
			ELSE "You can only throw objects."

Add To Every object
    Verb throw
	Check obj In hero
	    Else "You haven't got that!"
	Does
		"That wouldn't accomplish anything."
    End Verb.
End Add To.


Syntax
    throw_at = throw (obj1) 'at' (obj2)
        Where obj1 Isa object
	    Else "You can only throw objects."
	And obj2 Isa thing
	    Else "You can't throw anything at that."

    throw_to = throw (obj1) 'to' (obj2)
        Where obj1 Isa object
	    Else "You can't be serious."
	And obj2 Isa thing
	    Else "You can't throw anything to that."

Add To Every object
    Verb throw_at, throw_to 
	When obj1
	    Check obj1 In hero
		Else "You haven't got that!"
	    And obj2 Not In hero
	        Else
		    "You are carrying" Say The obj2. "."
	    And obj2 <> hero
		Else "You can't throw" Say The obj1. "at yourself."
	    Does 
	        Say The obj1. "bounces harmlessly off"
		Say The obj2. "and you catch it again."
    End Verb.
End Add To.


Syntax
    throw_in = throw (obj1) 'in' (obj2)
	Where obj1 Isa object
	    Else "Don't be silly."
	And obj2 Isa Container
	    Else "You can't throw anything in that."

Add To Every object
    Verb throw_in
	When obj1
	    Check obj1 In hero
	        Else "You haven't got that!"
	    And obj1 <> obj2
		Else "Now, that would be a good trick!"
	    And obj2 <> hero
	        Else "You can't put" Say The obj1. "into yourself!"
	    Does
		Locate obj1 In obj2.
		"Done."
    End Verb.
End Add.

