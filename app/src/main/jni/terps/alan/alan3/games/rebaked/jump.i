-- jump.i

add to every thing
  is not jump_inable.
  	not jump_onable. -- "jump offable"
  	not jump_thruable.
end add to.

synonyms
	hop,leap = jump.

syntax
	jump_on = jump 'on' (obj)
		where obj isa thing
			else "You can't jump on that!"

  	jump_off  = jump 'off' (obj)
		where obj isa thing
			else "You can't jump off that!"

  	jump_in = jump 'in' (obj)
		where obj isa thing
			else "You can't jump in that!"

  	jump_through = jump through (obj)
		where obj isa thing
			else "You can't jump through that!"

add to every thing

  	verb jump_on
		check obj is jump_onable
			else "You can't jump on that."
  		does
			"You jump on" say the obj. "."
  	end verb.

  	verb jump_off
  		check obj is jump_onable
    		else "You can't jump off that!"
  		does
			"You jump off" say the obj. "."
  	end verb.

  	verb jump_in
  		check obj is jump_inable
    		else "You can't jump in that!"
  		does
			"You jump in" say the obj. "."
  	end verb.

  	verb jump_through
  		check obj is jump_thruable
    		else "You can't jump through that!"
  		does
			"You jump through" say the obj. "."
  	end verb.

end add to.

syntax
	jump = jump.

verb jump does
	"You jump up and down. Boingg! Boingg!"
end verb.

syntax
  jump_overboard = jump overboard.

  verb jump_overboard does
  		"Huh? You're not on a boat."
  end verb.
