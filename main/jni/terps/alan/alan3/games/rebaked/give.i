-- give.i

syntax
  	give = 'give' (obj) 'to' (recip)
    	where obj isa object
      		else "You can only give away objects."
    	and recip isa container
      		else "You can't give things to that!"
  	give = give (recip) (obj).

add to every object
  	verb give
    	when obj
      		check obj in hero
				else "You don't have" say the obj. "."
      		does
				if recip=hero then
	  				"You already have" say the obj. "!"
				else
	  				"You give" say the obj. "to" say the recip. "."
	  				locate obj in recip.
				end if.
  	end verb.
end add to.
