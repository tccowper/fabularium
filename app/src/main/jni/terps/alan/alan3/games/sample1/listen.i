-- listen.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3



SYNTAX
	listen_to = listen 'to' (obj)
		WHERE obj ISA THING
			ELSE "You can't listen to that!"

Add To Every thing
  VERB listen_to
	DOES
		"You listen to" Say The obj. "."
  END VERB.
End Add To.




SYNTAX
	listen = listen.


VERB listen
	DOES
		"You hear nothing unusual."
END VERB.
