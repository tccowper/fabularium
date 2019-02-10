-- nowhere.i -- the 'limbo' location

synonyms
	n = north.
	s = south.
	e = east.
	w = west.
	u = up.
	d = down.
  	'exit' = out.
  	ne,northeast,se,southeast,nw,northwest,sw,southwest = nodirection. 

syntax
  	nodirection = nodirection.

  	verb nodirection does
    	"The only directions you'll need in this game are north, south, east, west, up,
    	down, in, and out." 
  	end verb.

the nowhere isa location.
	exit north to nowhere.
	exit south to nowhere.
	exit west to nowhere.
	exit east to nowhere.
	exit up to nowhere.
	exit down to nowhere.
end the nowhere.

syntax
	enter = enter (obj)
		where obj isa object
			else
				"That isn't something you can enter."
	enter = 'go' 'in' (obj).
	
add to every object
	is not enterable.

	verb enter
		check obj is enterable
			else
				"That isn't something you can go in."
		does
			"You go in."
	end verb.

end add to.
