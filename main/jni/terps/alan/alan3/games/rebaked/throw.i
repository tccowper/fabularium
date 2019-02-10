-- throw.i

synonyms
	dump,cast,toss,heave = throw.

syntax
	throw = throw (obj) *
		where obj isa object
			else "You can only throw objects."

add to every object
    verb throw
		check obj in hero
	    	else "You haven't got that!"
		does
	    	"You can't throw very far," say the obj. "ends up on the ground."
	    	locate obj here.
    end verb.
end add to.

syntax
    throw_at = throw (obj1) 'at' (obj2)
        where obj1 isa object
	    	else "You can only throw objects."
		and obj2 isa thing
	    	else "You can't throw anything at that."
    throw_at = throw (obj1) 'to' (obj2).
    
add to every object
    verb throw_at
		when obj1
	    	check obj1 here
				else "You haven't got that!"
	    	and obj2 not in hero
	        	else "You are carrying" say the obj2. "."
	    	and obj2 <> hero
				else "You can't throw" say the obj1. "at yourself."
	    	does
	        	say the obj1. "bounces harmlessly off"
				say the obj2. "and ends up on the ground."
				locate obj1 here.
    end verb.
end add to.

syntax
    throw_in = throw (obj1) 'in' (obj2)
		where obj1 isa object
	    	else "Don't be silly."
		and obj2 isa container
	    	else "You can't throw anything in that."

add to every object
    verb throw_in
		when obj1
	    	check obj1 in hero
	        	else "You haven't got that!"
	    	and obj1 <> obj2
				else "Now, that would be a good trick!"
	    	and obj2 <> hero
	        	else "You can't put" say the obj1. "into yourself!"
	    	does
				locate obj1 in obj2.
				"Done."
    end verb.
end add.

