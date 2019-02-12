-- lock.i

add to every object
  	is not lockable.locked.
end add to object.

syntax
  	lock = lock (obj)
    	where obj isa object
      		else "You can't lock that."

add to every object
    verb lock
		check obj is lockable
	    	else "You can't lock that!"
		and obj is not locked
	    	else "It's already locked."
		does
	    	make obj locked. say the obj. "is now locked."
	end verb.
end add to object.

syntax
    lock_with = lock (obj) 'with' (key)
		where obj isa object
	    	else "You can't lock that."
		and key isa object
	    	else "You can't lock anything with that."

add to every object
    verb lock_with
		when obj
	    	check obj is lockable
				else "You can't lock that!"
	    	and obj is not locked
				else "It's already locked."
	    	and key here
				else "You don't have" say the key. "."
	    	does
	        	make obj locked.
				say the obj. "is now locked."
    end verb.
end add.

syntax
    unlock = unlock (obj)
        where obj isa object
	    	else "You can't lock that."

add to every object
    verb unlock
		check obj is lockable
	    	else "You can't unlock that!"
		and obj is locked
	    	else "It's already unlocked."
		does
	    	make obj not locked.
	    	say the obj. "is now unlocked."
    end verb.
end add to.

syntax
    unlock_with = unlock (obj) 'with' (key)
		where obj isa object
	    	else "You can't lock that."
		and key isa object
	    	else "You can't lock anything with that."

add to every object
    verb unlock_with
        when obj
	    	check obj is lockable
	        	else "You can't unlock that!"
	    	and obj is locked
				else "It's already unlocked."
	    	and key in hero
				else "You don't have" say the key. "."
	  		does
				make obj not locked.
				say the obj. "is now unlocked."
    end verb.
end add.
