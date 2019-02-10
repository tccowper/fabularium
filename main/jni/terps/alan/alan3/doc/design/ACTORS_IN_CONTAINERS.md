# Actors in containers

In current versions of Alan v3 actors are prohibited to enter containers. This
document tries to investigate the implications and repercussions of allow this.
A number of questions are put, mostly in terms of scenarios. Each question is
discussed and analysed with further actions listed. Testcases required are also
presented.

A primary design concern with Alan is that out-of-the-box the author should get
reasonable behavior, but that behaviour must be possible to replace, tweak or
leave out completely. One example of this is containers and
how they are described:

1. without any description it gets a default description that lists the instances in the container
2. authors can tweak the way all container lists are presented using message overrides
3. authors can add a description to the container object and if so have to cater for the listing with the possibility to
    1. leave the listing out completely or conditionally (depending on an attribute for example)
    2. use the built in `List` statement
    3. use specialised lists by iterating over instances in the container (`For Each i In This...`)

This example shows the true flexibility of the Alan language, allowing much to
be achieved with very little but yet allowing more complex behaviour with little
extra work. If actors are to be allowed in containers, the above must also be
true for every aspect of this.

As an extra complication we want to make any change to the langauge and the game
format backwards compatible, if possible.

## Question 1: Can actors move?

**Q:** What happens if an actor is in a container and then moves to another
location?

**A:** The actor should be removed from the container and located in the new
location.

### Discussion

*Isn't this automatic when the player types a direction? Because the hero is at
the location anyway, even if he's in a container there. If the container is
locked or closed, the game author should just take care of the checks.*

The two cases where the actor is the hero and when the actor is not the hero
must be discussed separately. Here we are considering the basic case with no
Extract checks.

#### The hero

As exits only apply to the hero, if the player types a directional command while
the hero is in a container the location of the hero is still a location with
possible exits. Which should be applied as usual. The only difference is ensure
that the exits are found by using the *location* of the hero and not its *where*
(which is in the container).

**Testcase:** Locate the hero out of a container into another location.

**Testcase:** Locate the hero out of the container into the location of the
container.

**Testcase:** Locate the hero out of a container into another location with
prohibiting Exit checks.

**Action:** Decide what happens if exit checks prohibit the hero to leave.
Should he still be in the container or not? Probably. By default we don't know
what the container represents.

#### Other actors

Other actors move by "floating" around and just jumping to the new location, so
if no Extract check prohibits the move, the actor will simply leave the
container, where ever that might be, and be located at the new location.

**Testcase:** Locate an actor out of a container into another location.

**Testcase:** Locate an actor out of the container into the location of the
container.

**Testcase:** Locate an actor out of a container into another location with
prohibiting Exit checks.

## Question 2: What if an actor can't be extracted?

**Q:** What happens if an actor is in a container that prohibits removals using
the EXTRACT clause and the actor moves to another location?

**A:** The actor should abort its script when trying to move and remain in the
container.

### Discussion

*This is what happens already now when you try to LOCATE an object out of such a
container (or if you try to EMPTY the container). Also an event trying to take
an object from such a container would not be successful. A script would probably
fail similarly?*

It is true that locating something from a container is subject to the Extract
checks, and so would an actor. An event would abort after a failing extract, and
so would a script.

Again we need to consider the two cases of the hero and other actors separately.

#### The hero

The players command will have to be aborted in the same way as if
he tried to take something out of the container or move through an exit with prohibiting checks. The hero should stay in the container.

**Testcase:** Locate the hero out of a container into another location with
prohibiting Extract checks.

**Testcase:** Locate the hero out of a container into the same location with
prohibiting Extract checks.

#### Other actors

The actors script step will be aborted. But what happens with the script
execution? There are three options:

1.  The step is considered ok and the script advances to the next step.
2.  The script execution is stopped, as if the aborted step was the last in the
    current script.
3. The step is retried until it succeeds.
4. Introducing a new clause to catch this type of errors in scripts.

It seems like #1 is wrong. It could lead to unimaginable spurious errors by
allowing the actor to continue executing with the assumption that the previous
step succeeded. But the author could actually cater for this case.

Although #2 and #3 both have merits, they seem to be to much automation and would
make overriding the default behaviour, continuing or not retrying resp., much harder on the author.

Number 4, although it can be done, is non-optimal since that would change the language and thus the format of the .a3c files. It would also introduce a new concept, that must be handled by the
author, in the mental model of language, *failure*. The repercussions are
unknown.

As one datapoint the current implementation lets the script continue if an actor
tries to locate something out of a container with an Extract clause that
prohibits it. This means case #1 would be consistent with the current behaviour
of actor scripts executing and failing during actions, and probably exactly what would happen if
actors in containers would be implemented in the simplest way possible, namely according to case #1.

**Testcase:** Locate an actor out of a container into another location with
prohibiting Extract checks.

**Testcase:** In a script locate an actor out of a container with prohibiting
Extract checks.

**Action:** Decide if the current implementation is ok, namely that failing
steps are considered completed.

**Sidenote:** How would one ensure that the planned action of the actor was
actually achieved? A couple of ideas:

- Separate scripts for aquiring the object in question from other actions (not combining them) then the "stealing" script would always end after the
    attempt to get the object, failing or not. Rules or other scripts can then restart the stealing script if necessary.
- Rules?
- Step Wait Until?

## Question 3: Can the hero get out of the bed(room)?

**Q:** What happens if the hero is in the bed and types 'out' (when 'out' is an
exit out of the bedroom)?

**A:** The hero should be located out of the bed first and then move out of the
bedroom.  Also, the hero could move straight out of the bedroom (and not located
directly in the bedroom first), if nothing (e.g. EXTRACT) prohibits the
movement. But a message that the hero gets out of bed could be in place anyway.

### Discussion

*This can be done in an EXIT DOES statement, by the author.*

True, but see **Question 1**.

Additionally, to do the printout of the "You raise out of bed first.", the Exit
would need extra If statements to check if the hero is in the bed. This would be
ok if this was the only case. But if there where a lot of beds and chairs and
what not, that the hero needed to get out of before moving to another room, this
would be extremely tedious to handle in every exit. Especially if the containers
could be moved around so you don't actually know which checks that need them...

## Question 4: How would an actor be described?

**Q:** How would the hero be described if inside a container? How would any
actor be described if inside a container?

There could be an automatic way, such as

	>Look
	The bedroom (in bed) ....

	>Look
    Bedroom
    You are in the bedroom. Jack is here (in bed).

### Discussion

*This is the one point which could be handy to have built-in somehow. It* could
*be done manually by the author:*

	>Look
    Bedroom
    (in bed)
    Blah blah...

*Here the addition "in bed" would have to be added to the description of the
location.*

*or, in the other case above,*

	THE jack ISA PERSON AT bedroom
    	MENTIONED
        	IF jack IN bed
            	THEN "Jack (in bed)"
                ELSE "Jack"
			END IF.
	END THE.

*but some kind of automatic system for this would save the author's time.*

I totally agree that a built in way of handling this might be valuable. But it's
not that simple or clearcut. But there is an interesting duality with containers
vis a vis actors and vis a vis objects.

Containers are by default described using an implicit List statement. The
default for this is `The <container> contains`, followed by the objects
that are in the container. And here we are actually talking about instances of
object (or its subclasses). Instances of Entity cannot be in containers at all,
and instances of Things are 'invisible'.

But for actors it might be the other way around. Maybe the container would
say nothing about the actor (?) but the actor would say that it is in the
container?

Again we might need to discuss a number of different cases, the hero vs. other
actors and describing vs. just mentioning. And there is also the case of the
container...

### Other actors

#### Descriptions

Maybe it is possible to arrive at some logical structure here. E.g. other actors
when described should work like containers. If there is no description clause
the default actor print out (`<actor> is here`)  could be amended with
`in the <container>` or even replaced with `<actor> is in the <container>`. But the default is only used if there is no explicit
description clause. To draw on the similarity to containers, if the container has a
description, the listing of its content must be added explicitly in that
description.

**Action:** Figure out a simple way to indicate the actors "containment" that
can be added to explicit descriptions.

**Action:** Decide on a way to do the amended printout so that it can be
customized as needed and preferably don't break backwards compatibility (HARD!)

#### Mentioned

When another actor than the hero is to be mentioned, should that output be
amended with "in ..."? No, I don't think so. Consider

	> give mr anderson the spoon
    You give mr anderson the spoon.

We certainly don't want

	> give mr anderson the spoon
    You give mr anderson (on the chair) the spoon.

### The hero

For the hero the customary way to indicate standing/sitting etc. is to amend the
location title:

	Bedroom (on the bed)
    >

And since the hero is only described when explicitly requested by "Describe
hero." and never mentioned in any special context this should suffice, I think.
But ...

What about the difference between the "in" and "on"? 

**Action** How do we know if we should print "in" or "on" or something else?
Probably need some extra customization possibilities.

There is actually one situation where the hero might get mentioned and that is
if the player examines the container that the hero is in and that container
would list the hero. (see below)

### The container

Another question is how the container should handle actors 'inside itself' when
it comes to output. Should it list them too?

	> x bed
    The bed is a normal kingsize bed. The bed contains a pillow, you and your mistress.

So, yes, probably. But also consider:

	> look
    The bedroom (in bed)
    This is your bed room. There is a bed here. The bed contains a pillow, you and your mistress.

Although not optimal this probably works as a reasonable default. And, considering the theory
that mentioned actors should not indicate their containment, this probably works as a reasonable
default. If mentioned actors would indicate their containment we would get

	> look
    The bedroom (in bed)
    This is your bed room. There is a bed here. The bed contains a pillow, you (in bed) and your mistress.

which clearly is not what we want.

To not list actors of the container seems a less natural choice. But, again this would be the default and the author must have the possibility to

1. modify the description of the container and fairly easily replicate the standard behaviour (using `List This.`)
2. modify the way containers presents the items in itself (using `For Each i In This Do *formatting* End For.`)
3. modify which type of items, if any, in the container gets listed (using `For Each i In This, Isa *something* Do ...`)

## Question 5: Can the hero escape from the cage?

**Q:** What happens if the hero is in a cage and types 'w' (out of the room)?
Can a check in EXTRACT prohibit exit commands?

	THE cage ISA OBJECT AT cave
    	CONTAINER
        	EXTRACT
            	CHECK cage IS NOT locked
                	ELSE ...

Exiting shouldn't be possible if the container is locked/etc. (This could be
defined at the library level?)

### Discussion

*Even if EXTRACT could not handle this, the author could make a check to the
EXIT statement. Even now, an actor moving from room to room through a script
could move through locked doors if the game author doesn't check that it
shouldn't happen.*

This is the same question a **Question 2**. The hero would be prohibited by the
Extract checks in the same way as Exit checks, but they should be applie first. Other
actors are subject to Extract checks which might abort their step (but might
continue, depending on the discussion in **Questions 2**).

## Question 6: Can actors randomly be located in containers?

**Q:** Will there be a problem with RANDOM in cases such as:

	THE box ISA OBJECT AT room1
		CONTAINER TAKING ACTOR.
	END THE.

	VERB test DOES
		LOCATE RANDOM IN  box IN hero.
	END VERB.

Say there are NPCs in the box (for any reason). Would the compiler issue any
warning, or should there be any reason for it to do so? If the NPCs are kittens,
this wouldn't be any problem (the hero could carry the kittens), but what if
there is a fat old man in the box? It wouldn't make sense to locate that in the
hero.

If this is allowed/possible/not avoidable, the game author should maybe account
for cases like this by himself?

**A:** No, there will not be any other problems than any other case of randomly picking something from a container even when actors are involved. The outcome would depend on a lot of other things like checks. The author needs to take care of this exactly like any other cases
using limits or other precautions to not allow what is unreasonable given the
game world semantics. Not a job for the compiler.

## Question 7: Is the seat occupied?

**Q:** If an NPC is scripted to sit down on a chair (container), and the hero is
already sitting on that chair, there should be a restriction that the NPC cannot
be seated on the same chair. This is more of a theoretical problem, since the
author can prohibit the hero from sitting down in all cases anyway.

**A:** Same as **Question 6**. Not a job for the compiler.

## Question 8: Do limits apply?

**Q:** Do LIMITS - COUNT apply when an NPC is acting out a script? E.g.

	THE seat ISA OBJECT AT bus
		CONTAINER TAKING ACTOR.
			LIMITS COUNT 1
				ELSE "The seat is already taken."
	END THE.

	THE passenger ISA ACTOR AT bus
		SCRIPT sitting_down
			STEP LOCATE passenger AT bus. "The passenger enters the bus."
            STEP LOCATE passenger AT seat. "The passenger sits down on the only free seats."
	END THE.

Would the passenger be located in the seat if someone was already sitting there?

**A:** Count, as all Limits, is always considered when locating something in a
container. If there already was **anything** in the seat (had it not been restricted to only take actors, entities say, even an object would count) and this would be the output

	> z
    The passenger enters the bus.
    > z
    The seat is already taken.
    >

Note that, as discussed previously, the actors script step is aborted. But depending on the decision in **Question 2** a third step in that script might be next to execute.

# Summary

There seems to be quite a few issues to resolve before a complete author/language level design is complete:

1. Decide where the hero ends up if trying to move through an Exit to another location from inside a container and the Exit checks prohibit this. In or out of the container. My suggestion would be to stay put, i.e. in the container.
2. Decide if the current implementation of failing script steps is ok, namely that failing steps are considered completed and execution will continue with the next step in the script, if any.
3. Figure out a simple way to indicate the actors "containment" that can be used in explicit descriptions in the same way as an author can add `List This.` in container descriptions.
4. Find a way to do the amended actor default printout (`The actor is here.` or `The actor is on/in the bed.`) so that it can be customized as needed and preferably don't break backwards compatibility (HARD!)
5. Decide if the containment message should go in the Location header and if so, how would that can be handled. (Probably the same way as the AGAIN message, indicating a new Message.)
6. Figure out a way to let each container decide if instances in it is "on" or "in" or even "carried". Note that this is in addition to the current way because the current only prints when listing content, now the container also needs to decide what to say when amending the actors description and the Location header (for the hero).

# Design Decisions (so far...)

* DD#1 If the hero is in a container and moves extract checks are applied first then exit checks
* DD#2 If the extract checks prohibit moving the heros move is aborted as if an exit check failed
* DD#3 If the exit checks fail the hero will remain in the container
* DD#4 If an actor (not the hero) is in a container and moves extract checks are applied
* DD#5 If an actor (not the hero) moves out of a container and the extract check fail that action fails and is aborted (event, script step or even a player command that tried to move the actor)
* DD#6 The location header should be possible to amend with "(in <cont>)", probably by adding an author level general amendment message, akin to M_AGAIN.
* DD#7 The built-in messages should say "in". The author is responsible for any deviation.