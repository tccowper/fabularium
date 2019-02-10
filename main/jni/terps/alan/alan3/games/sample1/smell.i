-- smell.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


Syntax
    smell0 = smell.

Verb smell0
    Does
	"You smell nothing unusual."
End Verb.


Syntax
    smell = smell (obj)
	Where obj Isa thing
	    Else "You can't smell that!"

Add To Every thing
    Verb smell
	Does
	    "You smell" Say The obj. "."
    End Verb.
End Add To.
