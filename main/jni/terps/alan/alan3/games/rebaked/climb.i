-- climb.i

add to every thing
  	is not climbable.
end add to.

synonyms
	clamber,scale = climb.

syntax
  	climb = climb (obj)
    	where obj isa object
      		else "You can't climb that!"

	climb_on = climb 'on' (obj)
    	where obj isa object
			else "You can't climb on that!"
	climb_on = climb up (obj).
	climb_on = climb up 'on' (obj).

  	climb_over = climb over (obj)
    	where obj isa object
			else "You can't climb over that!"

  	climb_down = climb down (obj)
    	where obj isa object
			else "You can't climb down that!"
	climb_down = climb down 'from' (obj).
	climb_down = climb off (obj).
	climb_down = climb down off (obj).

add to every object
  	verb climb
    	check obj is climbable
      		else "You can't climb that."
  		does "You climb on" say the obj. "."
  	end verb.

	verb climb_on
		check obj is climbable
			else "You can't climb on that!"
		does "You climb on" say the obj. "."
	end verb.

	verb climb_over
    	check obj is climbable
      		else "You can't climb up on that."
  		does "You climb over" say the obj. "."
  	end verb.

  	verb climb_down
    	check obj is climbable
      		else "You can't climb down that."
  		does "You climb down" say the obj. "."
  	end verb.
end add to.
