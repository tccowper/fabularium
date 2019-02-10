-- read.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


Add To Every object
    Is
	Not readable.
End Add To object.


Syntax
    read = read (obj)
	Where obj Isa object
	    Else "You can't read that."

Add To Every object
    Verb read
	Check obj Is readable
	    Else 
		"There is nothing written on" Say The obj. "."
	Does
	    "You read" Say The obj. "."
    End Verb.
End Add To.
