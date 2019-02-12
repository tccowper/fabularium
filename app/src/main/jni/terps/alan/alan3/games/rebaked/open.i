-- open.i

syntax
  	open = open (obj)
    	where obj isa object
      		else "You can't open that."

add to every object
  	is not openable.not open.

  	verb open
    	check obj is openable
      		else "You can't open that!"
    	and obj is not open
      		else "It's already open."
    	does
      		make obj open.
			say the obj. "is now open."
  	end verb.
end add to.

syntax
  	open_with = open (obj1) 'with' (obj2)
    	where obj1 isa object
      		else "You can't open that."
    	and obj2 isa object
      		else "You can't open anything with that."

add to every object
    verb open_with
		when obj1
	    	check obj2 in hero
	        	else "You don't have" Say The obj2. "."
	    	does "You can't open" Say The obj1. "with" Say The obj2. "."
    end verb.
end add to.

synonyms
  	shut = close.

syntax
    close = close (obj)
        where obj isa object
	    	else "You can only close objects."

add to every object
    verb close
		check obj is openable
	    	else "You can't close that."
		and obj is open
	    	else "It is not open."
		does
	    	make obj not open.
	    	say the obj. "is now closed."
    end verb.
end add to.

syntax
    close_with = close (obj1) 'with' (obj2)
        where obj1 isa object
	    else "You can't close that."
	and obj2 isa object
	    else "You can't close anything with that."

add to every object
    verb close_with
        when obj1
	    	check obj2 in hero
	    		else "You don't have" Say The obj2. "."
			does
	    		"You can't close" Say The obj1. "with" Say The obj2. "."
    end verb.
end add to.

