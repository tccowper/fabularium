-- turn.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


Add To Every object 
    Is
	Not 'on'.
	Not switchable.
End Add To object.


Syntax
    turn_on1 = turn 'on' (obj)
	Where obj Isa object
	    Else "You can't turn that on."

    turn_on2 = turn (obj) 'on'
	Where obj Isa object
	    Else "You can't turn that on."

    switch_on1 = switch 'on' (obj)
	Where obj Isa object
	    Else "You can't switch that on."

    switch_on2 = switch (obj) 'on'
	Where obj Isa object
	    Else "You can't switch that on."


Add To Every object
    Verb turn_on1, turn_on2, switch_on1, switch_on2
	Check obj Is switchable
	    Else "You can't turn that on."
	And obj Is Not 'on'
	    Else "It's already on."
	Does
	    Make obj 'on'.
	    "You turn on" Say The obj. "."
    End Verb.
End Add To.



Syntax
    turn_off1 = turn off (obj)
	Where obj Isa object
	    Else "You can't turn that off."

    turn_off2 = turn (obj) off
	Where obj Isa object
	    Else "You can't turn that off."

    switch_off1 = switch off (obj)
	Where obj Isa object
	    Else "You can't switch that off."

    switch_off2 = switch (obj) off
	Where obj Isa object
	    Else "You can't switch that off."


Add To Every object
    Verb turn_off1, turn_off2, switch_off1, switch_off2
	Check obj Is switchable
	    Else "You can't turn that off."
	And obj Is 'on'
	    Else "It's already off."
	Does
	    Make obj Not 'on'.
	    "You turn off" Say The obj. "."
    End Verb.
End Add To.

