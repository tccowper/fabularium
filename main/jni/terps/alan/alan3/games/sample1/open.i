-- open.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3
-- 0.5.0 - Changed attributes around:
--		'open' indicates state
--		'openable' indicates if it is possible to open
--		'closeable' indicates if it is possible to close

Syntax
  open = open (obj)
    Where obj Isa object
      Else "You can't open that."

Add To Every object
  Is
    Not openable.
    Not open.

  Verb open
    Check obj Is openable
      Else "You can't open that!"
    And obj Is Not open
      Else "It's already open."
    And obj Is Not locked
	Else "It's locked."
    Does
      Make obj open.
	Say The obj. "is now open."
  End Verb.

End Add To.


Syntax
  open_with = open (obj1) 'with' (obj2)
    Where obj1 Isa object
      Else "You can't open that."
    And obj2 Isa object
      Else "You can't open anything with that."

Add To Every object
    Verb open_with
	When obj1
	    Check obj2 In hero
	        Else "You don't have" Say The obj2. "."
	    Does
	    	"You can't open" Say The obj1. "with" Say The obj2. "."
    End Verb.
End Add To.



Synonyms
  shut = close.

Syntax
    close = close (obj)
        Where obj Isa object
	    Else "You can only close objects."

Add To Every object
    Is Not closeable.

    Verb close
	Check obj Is closeable
	    Else "You can't close that."
	And obj Is open
	    Else "It is not open."
	Does
	    Make obj Not open.
	    Say The obj. "is now closed."
    End Verb.

End Add To.


Syntax
    close_with = close (obj1) 'with' (obj2)
        Where obj1 Isa object
	    Else "You can't close that."
	And obj2 Isa object
	    Else "You can't close anything with that."

Add To Every object
    Verb close_with
        When obj1
	    Check obj2 In hero
	    	Else
		    "You don't have" Say The obj2. "."
	Does
	    "You can't close" Say The obj1. "with" Say The obj2. "."
    End Verb.
End Add To.

