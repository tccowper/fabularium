-- invent.i

synonyms
	i,inventory = invent.

syntax
	invent = invent.

verb invent does
	list hero.
	list worn.
end verb.

the worn isa thing
	container
		header
			"You are wearing"
		else
			""
end the worn.
