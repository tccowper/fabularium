-- eat.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


ADD TO EVERY OBJECT 
IS
	NOT edible.
	NOT drinkable.
END ADD TO OBJECT. 

SYNTAX
	eat = eat (obj)
	WHERE obj ISA OBJECT
		ELSE "You can't eat" Say An obj. "!"

	drink = drink (obj)
	WHERE obj ISA OBJECT
		ELSE "You can't drink" Say An obj. "!"

Add To Every object
  VERB eat
	CHECK obj IS edible
		ELSE "You can't eat that!"
	DOES
		LOCATE obj AT Nowhere.
		"You eat" Say the obj. "."
  END VERB.

  VERB drink
	CHECK obj IS drinkable
		ELSE "That is not drinkable."
	DOES
		LOCATE obj AT Nowhere.
		"You drink" Say The obj. "."
  END VERB.
End Add.

