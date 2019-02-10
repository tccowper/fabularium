-- scenery.i
-- Library version 0.5.0

Every scenery Isa object
    Is 
	Not searchable.
	Not takeable.
	Not pushable.
	Not touchable.

    Verb examine, look_at, take
	Does Only
	    "That's only scenery."
    End Verb.

End Every scenery.
