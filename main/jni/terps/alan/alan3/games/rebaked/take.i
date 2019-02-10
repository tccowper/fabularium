-- take.i

synonyms
	carry,get,obtain,snag,snatch,lift,grab,steal,confiscate,catch,hold =
	take.

syntax
	take = take (obj) *
	  	where obj isa object
	      else "you can't take that with you!"
	take = pick up (obj)*.
	take = pick (obj)* up.

add to every object
	verb take
	    check obj is takeable
	      else "You can't take that!"
	    and obj not in worn
	      else "You've already got that -- you're wearing it."
	    and obj not in hero
	      else "You've already got that."
	    does
	      locate obj in hero.
	      make obj gotten.
	      "You take" say the obj. "."
	end verb.
end add to.

synonyms
	discard = drop.

syntax
    drop = drop (obj)*.
    drop = put (obj)* down.
    drop = put down (obj)*.

add to every object
    verb drop
        check obj here
        	else "That isn't here."
    	does
      		locate obj here.
      		"Dropped."
  	end verb.
end add to.

syntax
	drop_in = drop (obj1) 'in' (obj2)
		where obj1 isa object
			else "You can't drop that anywhere."
		and obj2 isa container
			else "You can't drop anything in that." 

add to every object
    verb drop_in
		when obj1
	    	check obj1 here
				else Say The obj1. "isn't here."
	    	and obj1 <> obj2
	      		else "You can't drop something into itself!"
	    	and obj2 <> hero
	      		else "you can't drop" say obj1. "into yourself!"
	    	does
	      		locate obj1 in obj2.
				"Done."
    end verb.
end add to.

syntax
  	take_from = 'take' (obj) 'from' (holder)
    	where obj isa object
      		else "You can only take objects."
    	and holder isa thing
      		else "You can't take things from that!"
    	and holder isa container
      		else "You can't take things from that!"
  	take_from = 'take' (obj) out 'of' (holder).

add to every object
  	verb take_from
    	when obj
      		check obj not in hero 
				else "You already have" say the obj. "."
      		and obj in holder
				else say the obj. "is not there."
			does
	  			if holder=hero then
	    			"You don't need to take things from yourself!"
	  			else 
	    			locate obj in hero.
      				make obj gotten.
	    			"You take" say the obj. ". "
	  			end if.
  	end verb.
end add.
