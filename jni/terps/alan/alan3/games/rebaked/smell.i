--smell

synonyms
	sniff = smell.

syntax
    smell = smell.


verb smell does
	"Sniffing in the air, you smell nothing unusual."
end verb.

syntax
    smell_obj = smell (obj)
		where obj isa thing
	    	else "That isn't something you can smell."

add to every thing
    verb smell_obj does
	    "You smell" say the obj. "."
    end verb.
end add to.
