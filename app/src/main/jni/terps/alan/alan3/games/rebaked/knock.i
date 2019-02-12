-- knock.i

syntax
	knock_on = knock 'on' (obj)
		where obj isa thing
			else "You can't knock on that!"

add to every thing
  	verb knock_on does
		"You knock on" say the obj. "."
  	end verb.
end add to.

syntax
	knock = knock.

verb knock does
	"You need to say what you want to knock on."
end verb.
