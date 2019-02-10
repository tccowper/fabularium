-- take.i
-- Library version 0.5.0

-- 0.6.0 - removed guard for taking things from animate containers since
--         Alan v3.0.29 supports Extract clause on containers
-- 0.5.0 - cleaned up formatting, made use of "." not making a leading space
-- 0.4.1 - converted to ALANv3



Add To Every thing
  Is
    InAnimate.
End Add To thing.

Add To Every object 
  Is
    takeable.
End Add To object.

Add To Every actor 
  Is
    Not inanimate.
End Add To actor.


Synonyms
  get, carry, obtain, grab, steal, confiscate, hold = take.


Syntax
  take = take (obj) *
    Where obj Isa object
      Else "You can't take that with you!"


Syntax
  pick_up1 = pick up (obj)*
    Where obj Isa object
      Else "You can't take that with you!"

  pick_up2 = pick (obj)* up
    Where obj Isa object
      Else "You can't take that with you!"

Add To Every object
  Verb take, pick_up1, pick_up2
    Check obj Is takeable
      Else "You can't take that!"
    And obj Not In worn
      Else "You've already got that - you're wearing that."
    And obj Not In hero
      Else "You've already got that."
    And weight Of obj <=50
      Else "That is too heavy to lift."
    Does
      Locate obj In hero.
      "Taken."
  End Verb.
End Add To.



Synonyms
  discard = drop.

Syntax
  drop = drop (obj)*.

Syntax
  put_down1 = put (obj) * down.

Syntax
  put_down2 = put down (obj)*.

Add To Every object
  Verb drop, put_down1, put_down2
    Check obj In hero
      Else "You aren't carrying that."
    Does
      Locate obj Here.
      "Dropped."
  End Verb.
End Add To.



Syntax
  take_from = 'take' (obj) 'from' (holder)
    Where obj Isa object
      Else "You can only take objects."
    And holder Isa thing
      Else "You can't take things from that!"
    And holder Isa Container
      Else "You can't take things from that!"

Add To Every object
  Verb take_from
    When obj
      Check obj Not In hero 
	Else
	  "You already have" Say The obj. "."
      And obj In holder
	Else
	  Say The obj. "is not there."
	Does
	  If holder=hero Then
	    "You don't need to take things from yourself!"
	  Else
	    Locate obj In hero.
	    "You take" Say The obj. "."
	  End If.
  End Verb.
End Add.

