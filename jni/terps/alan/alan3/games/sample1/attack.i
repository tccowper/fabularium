-- attack.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3


Add To Every thing
Is 
   Not Shootable.
End Add To thing. 

Add To Every object
Is
	Not weapon.
	Not shootable.
End Add To object.


Synonyms
  kill, fight, hit = attack.
  fire = shoot.


Syntax
  attack = attack (act)
    Where act Isa thing
      Else "You can't attack that."

Add To Every thing
  Verb attack
    Does
      "Violence is not the answer."
  End Verb.
End Add To.



Syntax
  attack_with = attack (act) 'with' (obj)
    Where act Isa thing
      Else "You can't attack that."
    And obj Isa object
      Else "You can't attack anything with that!"

Add To Every thing
  Verb attack_with
    When obj
      Check obj In hero
        Else "You don't have that object to attack with."
      And obj Is weapon
        Else "No point attacking anything with that!"
      Does
        "Violence is not the answer."
  End Verb.
End Add To.


Syntax
  shoot = shoot (obj)
    Where obj Isa thing
      Else "You can't shoot at that."
  shoot = shoot 'at' (obj).


Add To Every thing
  Verb shoot
    Does
      If obj Is shootable Then
	"You need to specify what to shoot at."
      Else
	"You need to specify what you want to shoot"
        Say The obj. "with."
      End If.
  End Verb.
End Add To.



Syntax
  shoot_at = shoot (obj) 'at' (act)
    Where obj Isa object
      Else "You can't shoot that."
    And act Isa thing
      Else "You can't shoot at that."

  shoot_with = shoot (act) 'with' (obj)
    Where obj Isa object
      Else "You can't shoot that."
    And act Isa thing
      Else "You can't shoot at that."

Add To Every thing
  Verb shoot_at
    When obj
      Check obj In hero
        Else "You don't have that."
      And obj Is shootable
        Else "You can't shoot anything with that."
      Does
        "Violence is not the answer."
  End Verb.

  Verb shoot_with
    When obj
      Check obj In hero
        Else "You don't have that."
      And obj Is shootable
        Else "You can't shoot anything with that."
      Does
        "Violence is not the answer."
  End Verb.
End Add To.

