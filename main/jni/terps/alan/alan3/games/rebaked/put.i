-- put.i

synonyms
	place = put.

syntax
	put = put (obj) *
		where obj isa object
			else "You can't put that anywhere."

add to every object
  	verb put
		check obj in hero
			else "You haven't got that."
		does
			locate obj here.
			"Dropped."
  	end verb.
end add to.

syntax
	put_in = put (obj1) 'in' (obj2)
		where obj1 isa object
			else "You can't put that anywhere."
		and obj2 isa container
			else "You can't put anything in that."

add to every object
    verb put_in
		when obj1
	    	check obj1 in hero
				else
		    		"You haven't got" say the obj1. "."
	    	and obj1 <> obj2
	        	else "You can't put something into itself!"
	    	and obj2 <> hero
	        	else "You can't put" say obj1. "into yourself!"

	    	does
	        	locate obj1 in obj2.
				"Done."
    end verb.
end add to.

syntax
    put_near = put (obj1) 'near' (obj2)
        where obj1 isa object
	    	else "You can't put that anywhere."
		and obj2 isa thing
	    	else "You can't put anything near that."

    put_behind = put (obj1) behind (obj2)
        where obj1 isa object
	    	else "You can't put that anywhere."
		and obj2 isa thing
	    	else "You can't put anything behind that."

    put_on = put (obj1) 'on' (obj2)
		where obj1 isa object
	    	else "You can't put that anywhere."
		and obj2 isa thing
	    	else "You can't put anything on that."

    put_under = put (obj1) under (obj2)
        where obj1 isa object
	    	else "You can't put that anywhere."
		and obj2 isa thing
	    	else "You can't put anything under that."

add to every object
    verb put_near,put_behind,put_on,put_under
		when obj1
	    	check obj1 here
				else "You haven't got" say the obj1. "."
	    	and obj2 not in hero
				else
		    		"You are carrying" say the obj2.
		    		". If you want to take" say the obj1. "just say so."
	    	does
				"Naaah. i'd rather just put" say the obj1. "down here."
			locate obj1 at obj2.
    end verb.
end add to.
