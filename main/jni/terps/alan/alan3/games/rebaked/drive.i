-- drive.i

add to every object
	is not driveable.
end add to.

synonyms
	'start' = drive.

syntax
	drive = drive (obj)
		where obj isa object
			else "You can't drive that. "

add to every object
	verb drive
		check obj is driveable
			else "That's not something you can drive."
	does "You drive" say the obj. "."
	end verb.
end add to.
