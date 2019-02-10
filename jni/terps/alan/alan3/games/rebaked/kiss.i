-- kiss.i

syntax
	kiss = kiss (obj)
		where obj isa thing
			else "You can't kiss that!"

add to every thing
  	verb kiss does
		if obj=hero then
			"Well, if you must!"
		else
			if obj is not animate then
				"You kiss" say the obj. "."
			else
				Say The obj. "avoids your advances."
			end if.
		end if.
  end verb.
end add to.
