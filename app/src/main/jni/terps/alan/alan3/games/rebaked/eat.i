-- eat.i

add to every object
	is not edible.not drinkable.
end add to.

syntax
	eat = eat (obj)
		where obj isa object
			else "You can't eat" say an obj. "!"

	drink = drink (obj)
		where obj isa object
			else "You can't drink" say an obj. "!"

add to every object
  	verb eat
		check obj is edible
			else "Eeew! You can't eat that!"
		does
			locate obj at nowhere.
			"You eat" say the obj. "."
  	end verb.

  	verb drink
		check obj is drinkable
			else "That is not drinkable."
		does
			locate obj at nowhere.
			"You drink" say the obj. "."
  	end verb.
end add.
