-- touch.i

add to every thing
    is touchable.
end add to thing.

syntax
    touch = touch (obj)
		where obj isa thing
	    	else "You can't touch that."
    touch_with = touch (obj1) 'with' (obj2)
		where obj1 isa thing
	    	else "You can't touch that."
		and obj2 isa object
	    	else "You can only use objects to touch with."

add to every object
    verb touch
        check obj is touchable
            else "You can't touch that."
        does "You touch" say the obj. "."
    end verb.

    verb touch_with
		when obj1
	    	check obj1 is touchable
	        	else "You can't touch that."
	    	and obj1 <> obj2
	        	else "It doesn't make sense to touch something with itself."
	    	does
	        	"You touch" say the obj1. "with" say the obj2. "."
    end verb.
end add to.
