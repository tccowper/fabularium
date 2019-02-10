-- give.i
-- Library version 0.5.0

-- 0.5.0 - added syntax synonym "give (recip) (obj)"
-- 0.4.1 - converted to ALANv3



Syntax
  give = 'give' (obj) 'to' (recip)
    Where obj Isa object
      Else "You can only give away objects."
    And recip Isa Container
      Else "You can't give things to that!"
  give = give (recip) (obj).

Add To Every object
  Verb give
    When obj
      Check obj In hero
	Else
	  "You don't have" Say The obj. "."
      Does
	If recip=hero Then
	  "You already have" Say The obj. "!"
	Else
		"That wouldn't accomplish anything."
	End If.
  End Verb.
End Add To.


