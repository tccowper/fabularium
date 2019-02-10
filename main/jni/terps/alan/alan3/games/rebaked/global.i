-- global.i
-----------------------
add to every thing
  	is not animate.
end add to thing.

add to every object 
  is takeable.not gotten.
end add to object.

add to every actor 
  is animate.
end add to actor.
-----------------------
-- GLOBAL and SEMI-GLOBAL OBJECTS
-----------------------
the sun isa farobj
	is not takeable.not touchable.
	verb x does only
		if smog here then "The purple smog is too hazy to see the sun."
		else "You know better than to look at the blinding sun!"	 
		end if.
	end verb.
end the sun.
-----------------------	
the sky isa scenery
	is not takeable.
	verb x does only
		if smog here then "The purple haze fills the sky."
		else "It's a beautiful clear blue."
		end if.
	end verb.
end the sky.
-----------------------			
the smog isa scenery 
	name purple fog name mist name smog
	is not takeable.
	verb x does only "The fog hangs in the air like like a damp purple mist."
	end verb.
end the smog.
-----------------------		 
the ground isa scenery
	is not takeable.
	indefinite article "the"	
 	verb x does only
		"It's the surface that you stand and walk around on. But you knew that." 
 	end verb.
	verb stand_on does only
		make hero standing.
		"You are standing on the ground."
		empty pos_cntr here.
		locate pos_cntr at nowhere. 
	end verb.  	
end the ground.
-----------------------
the wall isa scenery 
	is not takeable.
	name wall name walls
	verb x does "The walls are solid. There's no going through them."
	end verb.
end the wall.
-----------------------
the ceiling isa scenery 
	is not takeable.
end the ceiling.
-----------------------
the floor isa scenery
	is not takeable.
	indefinite article "the"	
 	verb x does only
		"It's the surface you stand and walk around on. But you knew that." 
 	end verb.
	verb stand_on does only
		make hero standing.
		"You are standing on the floor."
		empty pos_cntr here.
		locate pos_cntr at nowhere. 
	end verb.  	
end the floor.
-----------------------
-- LOSE MESSAGE and END
-----------------------
event uhoh "$p$t*** You have come to an unfortunate end. ***$p"
  	quit.
end event.
-----------------------
-- CUSTOM PARAMETERS
-----------------------
add to every entity
  	is not plural.
end add to. 
message see_start:
	if parameter1 is plural then "There are $01"
  	else "There is $01"
  	end if.
message no_such: "You don't see any $1 here."
message what: "Please rephrase that and try again."
message what_word: "Be more specific about what you mean by '$1'."
-----------------------

