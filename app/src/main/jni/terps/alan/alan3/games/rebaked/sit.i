-- sit (down), stand (up)

add to every object
	is not sittable.
end add to.

the pos_cntr isa object
	container
		header
			""
	description
end the pos_cntr.

syntax
	sit_on = sit on (sobj)
		where sobj isa seatobj
			else "you can't sit on that!"
  sit_on = sit 'in' (sobj).
  sit_on = sit down on (sobj).
  sit_on = sit down 'in' (sobj).

syntax

sit = sit.
sit = sit down.

  	verb sit does
  		if hero is standing then
  			"You sit on the ground."
  			make hero not standing.
  		else "You are already sitting."
  		end if.
  	end verb.

syntax

stand = stand.
stand = stand up.

verb stand
  	check hero is not standing
  		else "You are already standing."
  	does
  		"You stand up."
  		empty pos_cntr here.
  		locate pos_cntr at nowhere.
  		make hero standing.
end verb.

syntax
	stand_on = stand on (flobj)
		where flobj isa floorobject else
			"That's not something you need to stand on."


