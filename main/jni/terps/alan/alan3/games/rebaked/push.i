-- push.i

add to every thing
	is pushable.
end add to thing.

syntax
	push = push (obj)
		where obj isa thing
			else "You can't push that."

add to every object
    verb push
		check obj is pushable
	    	else "You can't push that."
		does "You push" say the obj. "."
    end verb.
end add to.

syntax
    push_with = push (obj1) 'with' (obj2)
		where obj1 isa thing
	    	else "You can't push that."
		and obj2 isa object
	    	else "You can use only objects to push things with."

add to every object
    verb push_with
		when obj1
	    	check obj1 is pushable
	        	else "You can't push that."
	    	does "Using" say the obj2. "you push" say the obj1. "."
    end verb.
end add to.
