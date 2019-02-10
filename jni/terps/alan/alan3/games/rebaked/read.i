-- read.i

add to every object
    is not readable.
end add to object.


syntax
    read = read (obj)
		where obj isa object
	    	else "You can't read that."

add to every object
    verb read
		check obj is readable
	    	else "There is nothing written on" say the obj. "."
		does "You read" say the obj. "."
    end verb.
end add to.
