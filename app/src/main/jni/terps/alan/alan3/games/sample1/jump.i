-- jump.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


SYNTAX
	jump_on = jump 'on' (obj)
		WHERE obj ISA THING
			ELSE "You can't jump on that!"

Add To Every thing
  VERB jump_on
	DOES
		"You jump on" Say The obj. "."
  END VERB.
End Add To.




SYNTAX
	jump = jump.

VERB jump
	DOES
		"You jump up and down."
END VERB.
