-- hero.i

synonyms
  me,myself,self = hero.

the hero isa actor
	container
    	header
      		"You are carrying"
    			else
      		"You are empty-handed."
  	is standing.

	verb x does only
--		if suit not in worn then
			"You are a concerned and loving parent, trying to fulfill a promise to your
			children."
--		else
--			show 'waldo.jpg'.
--			"$n$nYou look remarkably like Waldo the clown!"
--		end if.
		if hero is not standing then
			"You are on"
			list pos_cntr.
		end if.
	end verb.

  verb talk_to
  	when act does only
      "You won't learn any more than you already know by talking to yourself."
  end verb.

  verb ask
  	when act does only
      "You should ask someone else: you won't learn any more than you already know
      by talking to yourself."
  end verb.

end the hero.
