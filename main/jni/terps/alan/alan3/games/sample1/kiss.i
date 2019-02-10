-- kiss.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3



SYNTAX
	kiss = kiss (obj)
		WHERE obj ISA THING
			ELSE "You can't kiss that!"

Add To Every thing
  VERB kiss
	DOES
		IF obj=HERO THEN
			"Well, if you must!"
		ELSE
			IF obj IS InAnimate THEN
				"You kiss" Say The obj. "."
			ELSE 
				Say The obj. "avoids your advances."
			END IF.
		END IF.
  END VERB.
End Add To.



