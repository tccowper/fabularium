-- wear.i
-- library version 0.4.1

-- 0.4.1 - converted to ALANv3


Add To Every object 
    Is
	Not wearable.
End Add To object.


Syntax
    wear = wear (obj)
	Where obj Isa object
	    Else "You can't wear that." 
    wear = put (obj) 'on'.
    wear = put 'on' (obj).

Add To Every object
    Verb wear
	Check obj Is wearable
	    Else
	        "You can't wear" Say The obj. "."
	And obj Not In worn
	    Else
	        "You are already wearing" Say The obj. "."
	And obj Is takeable
	    Else
		"You can't pick" Say The obj. "up."
	Does
	    If obj Not In hero Then
	    	Locate obj In hero.
		"(You pick" Say The obj. "up.)$n"
	    End If. 
	    Locate obj In worn.
	    "You put on" Say The obj. "."
    End Verb.
End Add To.




Syntax 
    'remove' = 'remove' (obj)
	Where obj Isa object
	    Else "You can't remove that." 
    'remove' = take (obj) off.
    'remove' = take off (obj).

Add To Every object
    Verb 'remove'
	Check obj In worn
	    Else 
		"You are not wearing" Say The obj. "."
	Does
	    Locate obj In hero.
	    "You take off" Say The obj. "."
    End Verb.
End Add To.



Syntax undress = undress.

Add To Every object
    Verb undress
	Does
	    If Count In worn, Isa Thing > 0 Then
	    	Empty worn In hero.
		"You remove all the items you were wearing."
	    Else
	        "You're not wearing anything you can remove."
	    End If.
    End Verb.
End Add To.
