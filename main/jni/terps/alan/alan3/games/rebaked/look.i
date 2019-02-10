-- look.i

add to every thing
  	is not transparent.not look_underable.
end add to.

synonyms
	l = 'look'.
	underneath,beneath,below = under.

syntax
	'look' = 'look'.
	'look' = 'look' around.

syntax
	look_through = 'look' through (obj)
		where obj isa thing
			else "That isn't something to look through."
	look_through = 'look' out (obj).

	look_under = 'look' under (obj)
		where obj isa thing
			else "That isn't something to look under. "

add to every thing
  	verb look_through
    	check obj is transparent
      		else "You can't see anything through that."
		does "You look through " say the obj. "."
  	end verb.
end add to.

add to every thing
  	verb look_under
    	check obj is look_underable
      		else "There's no way to look under that."
		does "You look under" say the obj. "."
  	end verb.
end add to.

verb 'look' does
	"$n"
	look.
end verb.
