-- attack.i

add to every thing
	is not shootable.not kickdownable.
end add to thing.

add to every object
	is not weapon.not shootable.
end add to object.

synonyms
  	swat,slap,punch,kill,smite,vanquish,fight,hit = attack.
  	fire = shoot.

syntax
  	attack = attack (act)
    	where act isa thing
      		else "That isn't something you can attack."

	kick = kick (act)
    	where act isa thing
      		else "That isn't something you can kick."

	kick_down = kick down (obj)
    	where obj isa object
      		else "That isn't something you can kick down."
  	kick_down = kick (obj) down.

add to every thing
  	verb attack
    	does describe violents.
  	end verb.

  	verb kick
    	does describe violents.
  	end verb.

  	verb kick_down
  		check obj is kickdownable
  			else "You can't kick that down."
  		does
  			"You kick down" say the obj. "."
  	end verb.

end add to.

syntax
  	attack_with = attack (act) 'with' (obj)
    	where act isa thing
      		else "That isn't something you can attack."
    	and obj isa object
      		else "You can't attack anything with that!"

add to every thing
  	verb attack_with
    	when obj
      		check obj here
        		else "You don't have that object to attack with."
      	and obj is weapon
        	else "There's no point attacking anything with that!"
    	does describe violents.
  	end verb.
end add to.

syntax
  	shoot = shoot (obj)
    	where obj isa thing
      		else "That isn't something you can shoot at."
  	shoot = shoot 'at' (obj).

add to every thing
  	verb shoot
    	does
      		if obj is shootable then
				"You need to say what to shoot at."
      		else
				"You need to say what you want to shoot"
        		say the obj. "with."
      		end if.
  	end verb.
end add to.

syntax
  	shoot_with = shoot (act) 'with' (obj)
    	where obj isa object
      		else "You can't shoot that."
    	and act isa thing
      		else "You can't shoot at that."

add to every thing

  	verb shoot_with
    	when obj
      		check obj here
        		else "You don't have that."
      	and obj is shootable
        	else "It's hard to figure how you could shoot anything with that."
    		does describe violents.
  	end verb.
  
end add to.

the violents isa object at nowhere
	description
     	if random 1 to 2 = 1 then
    		"Violence is not always the answer."
    	else "There has to be a better way."
		end if.
end the violents.		
