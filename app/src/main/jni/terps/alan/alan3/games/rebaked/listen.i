-- listen.i

syntax
	listen_to = listen 'to' (obj)
		where obj isa thing
			else "You can't listen to that!"

add to every thing
  	verb listen_to does
		"You listen to" say the obj. "."
  	end verb.
end add to.

syntax
	listen = listen.

verb listen does
	"You hear nothing unusual."
end verb.

