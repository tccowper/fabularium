-- touch.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


Add To Every thing
    Is
	touchable.
End Add To thing.


Syntax
    touch = touch (obj)
	Where obj Isa thing
	    Else "You can't touch that."

    touch_with = touch (obj1) 'with' (obj2)
	Where obj1 Isa thing
	    Else "You can't touch that."
	And obj2 Isa object
	    Else "You can only use objects to touch with."

Add To Every object
    Verb touch
        Check obj Is touchable
            Else "You can't touch that."
        Does
	    "You touch" Say The obj. "."
    End Verb.

    Verb touch_with
	When obj1
	    Check obj1 Is touchable
	        Else "You can't touch that."
	    And obj1 <> obj2
	        Else "It doesn't make sense to touch something with itself."
	    Does
	        "You touch" Say The obj1. "with" Say The obj2. "."
    End Verb.
End Add To.
