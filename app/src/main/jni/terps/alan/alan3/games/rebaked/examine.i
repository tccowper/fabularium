-- examine.i

add to every thing
	is examinable.searchable.
end add to thing.

add to every actor
	is not searchable.
end add to actor.

synonyms
	examine, inspect, 'check' = x.

syntax
	x = x (obj)
		where obj isa thing
			else "That's just not the sort of thing you can examine."
	x = 'look' 'at' (obj).

add to every object
  	verb x
		check obj is examinable
			else "You can't examine" say the obj. "."
		does ""
  	end verb.
end add to.

synonyms
	inside,into = 'in'.

syntax
	look_in = 'look' 'in' (obj)
		where obj isa thing
			else "You can't see inside that."
		and obj isa container
			else "You can't see inside that."

add to every object
  	verb look_in
		check obj is examinable
			else "You can't look inside" say the obj. "."
		does list obj.
  	end verb.
end add to.

syntax
	search = search (obj)
		where obj isa thing
			else "That's not something you can search."
	search = search 'in' (obj).

add to every object
  	verb search
		check obj is searchable
			else
				"There doesn't seem to be much to search about" say the obj. "."
		does "A cursory search reveals nothing of particular interest."
  	end verb.
end add to.
