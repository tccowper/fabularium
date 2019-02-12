-- pull, pull down

add to every thing
	is not downable.
end add to.	

synonyms
	yank,drag,tug = pull.

syntax
	pull = pull (obj)
		where obj isa thing
			else "You can't pull that. "
	pull = pull on (obj).

add to every thing
	verb pull does
		"You pull on" say the obj. "but nothing happens."
	end verb.
end add to.

syntax
  	pull_down = pull down (obj)
    	where obj isa object
      		else "You can't pull that down!"
  	pull_down = pull (obj) down.
	pull_down = tear down (obj).
	pull_down = tear (obj) down.

add to every object

  	verb pull_down
    	check obj is downable
      		else "You can't pull that down."
    	does "You pull down" say the obj. "."
  	end verb.
end add to.
