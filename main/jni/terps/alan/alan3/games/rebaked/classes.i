-- classes.i
-----------------------
-- SCENERY --
-----------------------
every scenery isa object
  	description ""
    is not searchable.
    verb x does
	    "That isn't anything you need to be concerned about."
    end verb.
  	verb take
  		check
  			"There's no point trying to take that. You won't need it."
  	end verb.
  	verb touch
    	check
    		"That's not important. Don't worry about it."
  	end verb.
  	verb push
    	check
    		"No need to push that. It's not important."
  	end verb.
end every scenery.
-----------------------
every farobj isa object
	description
 	verb take check "You can't reach that from here."
	end verb.
	verb search check "That's much too far away to search."
	end verb.
	verb push check "You can't reach far enough to push it from here."
	end verb.
	verb pull check "You can't reach far enough to pull it from here."
	end verb.
	verb touch check "You can't reach that from here."
	end verb.
end every.
-----------------------
every farplace isa farobj
  description
  is not touchable.not takeable.not searchable.
  verb x does only "You could see it a lot better by going there."
  end verb.
end every.
-----------------------
-- OBJECT CLASSES
-----------------------
every seatobj isa object -- all chairs, etc., should be seatobjs.
	is not takeable.
	indefinite article "the"
	description
	verb x does describe this.
		if this in pos_cntr then
			"You are sitting on"
			list pos_cntr.
		end if.
	end verb.
	verb sit_on does
		make hero not standing.
		locate pos_cntr here.
		locate this in pos_cntr.
		"You sit on"
		list pos_cntr.
	end verb.
end every.
-----------------------
every bedobj isa seatobj
	is layable.
	indefinite article "the"
	description
	verb x does describe this.
		if this in pos_cntr then
			"You are lying on"
			list pos_cntr.
		end if.
	end verb.
	verb lay_on does
		make hero not standing.
		locate pos_cntr here.
		locate this in pos_cntr.
		"You lay down on"
		list pos_cntr.
	end verb.
end every.
-----------------------
every floorobject isa object
	is not takeable.
	indefinite article "the"
	description
	verb x does
		describe this.
		if this in pos_cntr then
			"You are standing on"
			list pos_cntr.
		end if.
	end verb.
	verb stand_on does
		make hero not standing.
		locate pos_cntr here.
		locate this in pos_cntr.
		"You stand on"
		list pos_cntr.
	end verb.
end every.
-----------------------
every doorobj isa object
	name door
	description
	is not takeable.open.openable.closeable.
	has otherside doorobj.	
	verb x does "The door is"
		if this is not open then "closed."
		else "open."
		end if.
	end verb.				
	verb open check 
		"Opening and closing the door is not important. You can go in or out
		doors in this game unless they are locked."
	end verb.
	verb close 	check 
		"Opening and closing the door is not important. You can go in or out
		doors in this game unless they are locked."
	end verb.			
end every.
-----------------------
every underobj isa object
	is underlookable.not underlooked.
	description
	verb look_under does make this underlooked.
	end verb.	
end every. 		
-----------------------
every closeable isa object
  opaque container header "It contains"
  is openable. 
  verb open does after
    make this not opaque.
    list this.
  end verb.
  verb close does after 
    make this opaque.
  end verb.
  verb look_in does only  
	if this is not open then
		"(First opening it)$p"
		make this open.
	make this not opaque.
	end if.	
	list this.
	end verb.  
end every.
-----------------------


