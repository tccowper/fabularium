-- wear.i

add to every object
    is not wearable.
end add to object.

syntax
    wear = wear (obj)
		where obj isa object
	    	else "You can't wear that."
    wear = put (obj) 'on'.
    wear = put 'on' (obj).

add to every object
    verb wear
		check obj is wearable
	    	else "You can't wear" say the obj. "."
		and obj not in worn
	    	else "You are already wearing" say the obj. "."
		and obj is takeable
	    	else "You can't pick" say the obj. "up."
		does
	    	if obj not in hero then
	    		"(First picking up" say the obj. ")$n"
	    	end if.
	    	locate obj in worn.
	    	"You put on" say the obj. "."
    end verb.
end add to.

syntax
    'remove' = 'remove' (obj)
		where obj isa object
	    	else "You can't remove that."
    'remove' = take (obj) off.
    'remove' = take off (obj).

add to every object
    verb 'remove'
		check obj in worn
	    	else "You are not wearing" say the obj. "."
		does
	    	locate obj in hero.
	    	"You take off" say the obj. "."
    end verb.
end add to.

syntax undress = undress.

add to every object
    verb undress does
	    if count in worn, isa thing > 0 then 
	    	empty worn in hero.
			"You remove all the items you where wearing."
	    else "You're not wearing anything you can remove."
	    end if.
    end verb.
end add to.
