--squeeze

add to every thing
	is not squeezeable.not squeeze_thruable.
end add to thing.

synonyms
	hug, squish = squeeze.
	'between' = 'through'.
syntax
	squeeze = squeeze (obj)
		where obj isa thing
			else "You can't squeeze that."

add to every object
    verb squeeze
		check obj is squeezeable
	    	else "You can't squeeze that."
		does "You squeeze" say the obj. "."
    end verb.
end add to.

syntax
	squeeze_through = squeeze through (obj)
		where obj isa object
			else "You can't squeeze through that."
	squeeze_through = squeeze 'in' through (obj).

add to every object
	verb squeeze_through
		check obj is squeeze_thruable
			else "You can't squeeze through that."
		does "You squeeze through" say the obj. "."
	end verb.
end add to.

