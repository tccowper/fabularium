-- push.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3

ADD TO EVERY THING 
IS
	pushable.
END ADD TO THING.


SYNTAX
	push = push (obj)
		WHERE obj ISA THING
			ELSE "You can't push that."
Add To Every object
    Verb push
	Check obj Is pushable
	    Else "You can't push that."
	Does
	    "You push" Say The obj. "."
    End Verb.
End Add To.


Syntax
    push_with = push (obj1) 'with' (obj2)
	Where obj1 Isa thing
	    Else "You can't push that."
	And obj2 Isa object
	    Else "You can use only objects to push things with."

Add To Every object
    Verb push_with
	When obj1
	    Check obj1 IS pushable
	        Else "You can't push that."
	    Does
		"Using" Say The obj2. "you push" Say The obj1. "."
    End Verb.
End Add To.
