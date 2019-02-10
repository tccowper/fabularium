-- swim

add to every thing
	is not swimmable.
end add to.

syntax
	swim = swim (obj)
		where obj isa thing
			else "You can't swim in that!"
	swim = swim 'in' (obj).

add to every thing
	verb swim
		check obj is swimmable
			else "You can't swim in that!"
		does "You swim in" say the obj. "."
	end verb.
end add to.
