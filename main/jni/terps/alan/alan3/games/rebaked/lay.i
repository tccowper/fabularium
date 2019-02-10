-- lay (down)

add to every object
	is not layable.
end add to.

synonyms
 lie = lay.

syntax
  lay_on = lay on (bobj)
    where bobj isa bedobj
      else "that isn't something to lay on."

  lay_on = lay down on (bobj).
  lay_on = lay 'in' (bobj).
  lay_on = lay down 'in' (bobj).
  lay_on = sleep 'in' (bobj).
  lay_on = sleep on (bobj).
  
