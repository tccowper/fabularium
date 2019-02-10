-- talk.i

add to every thing
  	is not can_talk.
end add to thing.

add to every actor
  	is can_talk.
end add to actor.

synonyms
  	yell,scream = shout.

syntax
  shout = shout.

verb shout
  does
    "You make a lot of noise..."
end verb.

syntax
  	say_word = 'say' (topic)!
    	where topic isa thing
      		else "You can't say that."

add to every thing
  	verb say_word does
      	"$o? That's a nice word!"
  	end verb.
end add to.

syntax
  	say_to = 'say' (topic)! 'to' (act)
    	where topic isa thing
      		else "You can't say that."
    	and act isa thing
      		else "You can't talk to that."

add to every thing
  	verb say_to
    	when act
      		check act has can_talk
				else "You can't talk to that."
      		does say the act. "doesn't seem interested."
  	end verb.
end add to.

syntax
  	ask = ask (act) about (topic)!
    	where topic isa thing
      		else "You can't ask about that."
    	and act isa thing
      		else "You can't talk to that."

add to every thing
  	verb ask
    	when act
      		check act has can_talk
        		else "You can't talk to that."
      		does
				say the act. "says ""I don't know anything about" say the topic. "!"""
  	end verb.
end add to.

syntax
  	talk_about = talk 'to' (act) about (topic)!
    	where topic isa thing
      		else "You can't ask about that."
    	and act isa thing
      		else "You can't talk to that."
  	talk_about = tell (act) about (topic)!.

add to every thing
  	verb talk_about
    	when  topic
      		check act has can_talk
				else "You can't talk to that."
      		does
				"""I don't think I need to know about" say the topic. "$$,"" says"
				say the act. "."
  	end verb.
end add to.

syntax
  	talk_to = talk 'to' (act)
    	where act isa thing
      		else "You can't talk to that."

add to every thing
  	verb talk_to
    	check act has can_talk
      		else "You can't talk to that."
    	does
      		say the act. "looks at you, seemingly wondering if you have
	  		anything specific to talk about."
  	end verb.
end add to.
