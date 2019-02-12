-- row.i

add to every thing
	is not rowable.
end add to.

syntax
	row = row.

verb row does 
	"You don't see anything here to row!"
end verb.	
		
syntax
	row_obj = row (obj)
		where obj isa object
			else "That isn't something you can row."

add to every object
	verb row_obj
		check obj is rowable
			else "That isn't something you can row."
		does "You row the" say the obj. "."
	end verb.
end add to.	
		
