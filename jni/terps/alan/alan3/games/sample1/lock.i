-- lock.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


Add To Every object
  Is
    Not lockable.
    locked.
End Add To object.


Syntax
  lock = lock (obj)
    Where obj Isa object
      Else "You can't lock that."

Add To Every object
    Verb lock
	Check obj Is lockable
	    Else "You can't lock that!"
	And obj Is Not locked
	    Else "It's already locked."
	Does
	    Make obj locked. Say The obj. "is now locked."
	End Verb.
End Add To object. 



Syntax
    lock_with = lock (obj) 'with' (key)
	Where obj Isa object
	    Else "You can't lock that."
	And key Isa object
	    Else "You can't lock anything with that."

Add To Every Object
    Verb lock_with
	When obj
	    Check obj Is lockable
		Else "You can't lock that!"
	    And obj Is Not locked
		Else "It's already locked."
	    And key In hero
		Else
		    "You don't have" Say The key. "."
	    Does
	        Make obj locked.
		Say The obj. "is now locked."
    End Verb.
End Add.


Syntax
    unlock = unlock (obj)
        Where obj Isa object
	    Else "You can't lock that."

Add To Every object
    Verb unlock
	Check obj Is lockable
	    Else "You can't unlock that!"
	And obj Is locked
	    Else "It's already unlocked."
	Does
	    "You'll have to say what you want to unlock it with."
    End Verb.
End Add To.


Syntax
    unlock_with = unlock (obj) 'with' (key)
	Where obj Isa object
	    Else "You can't lock that."
	And key Isa object
	    Else "You can't lock anything with that."

Add To Every object
    Verb unlock_with
        When obj
	    Check obj Is lockable
	        Else "You can't unlock that!"
	    And obj Is locked
		Else "It's already unlocked."
	    And key In hero
		Else
		    "You don't have" Say The key. "."
	  Does
		"You unlock the" SAY obj. "."
    End Verb.
End Add.
