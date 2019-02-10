-- turn.i

add to every object
    is not 'on'.not switchable.
end add to.

syntax
    turn_on = turn on (obj)
		where obj isa object
	    	else "You can't turn that on."
    turn_on = turn (obj) 'on'.
	turn_on = switch 'on' (obj).
	turn_on = switch (obj) on.

  	light = light (obj)
    	where obj isa object
      		else "You can't light that. "
      		
add to every object
    verb turn_on,light
		check obj is switchable
	    	else "You can't turn that on."
		does
	    	make obj 'on'.
	    	"You turn on" say the obj. "."
    end verb.
end add to.

syntax
    turn_off = turn off (obj)
		where obj isa object
	    	else "You can't turn that off."
    turn_off = turn (obj) off.
    turn_off = switch off (obj).
    turn_off = switch (obj) off.

add to every object
    verb turn_off
		check obj is switchable
	    	else "You can't turn that off."
		does
	    	make obj not 'on'.
	    	"You turn off" say the obj. "."
    end verb.
end add to.

add to every object
	is not turnable.
end add to.

synonyms
  spin,flip,rotate,crank = 'turn'.

syntax
	turn = turn (obj)
  		where obj isa object
    		else "You can't turn that."

	turn_over = turn (obj) over
  		where obj isa object
    		else "You can't turn that over."
	turn_over = turn over (obj).
	turn_over = turn (obj) upside down.

	turn_around = turn (obj) around
  		where obj isa object
    		else "You can't turn that around."

add to every object
  	verb turn
    	check obj is turnable
      		else "You can't turn that."
    	does
    		"You turn the" say the obj."."
  	end verb.
end add to.

add to every object
  	verb turn_over,turn_around
    	check obj is turnable
      		else "You can't turn that whichaway. "
    	does "You turn" say the obj."."
  end verb.
end add to.
