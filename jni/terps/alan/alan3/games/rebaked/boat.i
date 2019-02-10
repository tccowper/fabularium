-- boat.i

the cabin isa location name 'In a Passenger Cabin'
	entered locate floor here.locate wall here.locate ceiling here.
	description
	"You" if boys here then "and your two boys" end if.
	"are in a very small passenger cabin. The accomodations are basic but"
	if boys here then
		"adequate, with two bunks: one for you, and the boys squeeze and wrestle for room in the other."
	else "adequate."
	end if.
	"$nA door leads out of the cabin."
	if boys not here then "$pYour boys are nowhere to be seen."
	end if.
	exit out to deck does
		if boys here then
			"You step out onto the much roomier deck of the boat. The boys follow you out."
			locate boys at deck.
			locate jim at deck.
			locate tim at deck.
		else "You hurry out to the deck of the boat."
		end if.	
	end exit.
	exit north,south,east,west to cabin
  		check
    	"There isn't much room to move around in the"
    	if boys here then "crowded" else "small" 
		end if.
    	"cabin."
	end exit.
end the cabin.
-----------------------
	synonyms slab,slabs,bunks,bed,beds,cot,cots = bunk.	
the bunk isa underobj at cabin
	name bunk name my their 'boy''s' foam rubber bunk
	is takeable.sittable.layable.
  verb x does only
    "The bunks are simple wooden frames with slabs of foam rubber on top."
  end verb.
  verb search does only
	"A cursory search of the bunk and the foam rubber slab reveals nothing
	of interest." 
  end verb.
  verb look_under does only
    if bunk is not underlooked then 
		"Crawling down on your knees and peering under the bunks, you find under
		one of them an aged and crinkly travel brochure, which you take."
		make bunk underlooked.
		locate brochure in hero.
    else "There's nothing more but dust under the bunks."
    end if.
  end verb.
  verb lay_on 	  
  	does after 
    	"It's not very comfortable or restful, but it will do for the 
     	two-day journey to the island." 
 	end verb.
  verb take
    check "You can't take them. The bunks are built into the cabin
	wall."
  end verb.		
	verb push
   	check "You can't move the bunks at all. They're built in to the
	cabin wall."
  end Verb.	
end the bunk.
-----------------------
the dust isa scenery at cabin end the dust.
----------------------- 
the brochure isa object
	name travel brochure name travel name aged name faded
	name aged 'and' crinkly faded wrinkled travel brochure
  is readable.not readed.bendable.not seen.
 	verb x does only
  	"The travel brochure is long out of date, faded and wrinkled."
  	if brochure is not seen then
  		"No telling how long it has been forgotten and unnoticed under
that bunk."
  		make brochure seen.
  	end if.	
  end verb.
  verb read does only
		if brochure not in hero then
	    	"(Picking up the travel brochure)$p"
	      	locate brochure in hero.
	  end if.
		"Stiff with water damage and age, the travel brochure crinkles and
		cracks as you unfold and read it. Its faded pages tell travelers all
		about Wheewhistle Island. The island is unique in having the world's
		only known source of bazzleberries, known for their almost
		magical powers to give great longevity -- but at a terrific cost. The
		mere taste of a bazzleberry wipes the mind of nearly all memory."
		if brochure is not readed then
			"$p(How well you know that. A single berry wiped your memory and ended
			your career nearly fifteen years ago. Had it not been for good
			friends like Boffo to help you, there's no telling what would have
			happened to you and your boys!)$p"
		end if.	
		"The island's main attraction, of course, is the circus: ""Boffo's
		Bonanza Bigshow"". The star of the circus is, or was at the time of the
		brochure -- you! -- as Waldo the clown. There is a picture of you -- er,
		Waldo -- on the cover. Though the picture is faded now, it had once
		shown a bright and happy white smile, wavy orange hair covered with a
		blue derby, and a nose like a shiny red ball. He is holding a
		cream-covered pie, which was your trademark: Waldo the happy,
		pie-throwing clown. 
		$nThe brochure also includes a little information about daily life in
		Whoopdeville, and several of the shops offering souvenirs and
		refreshments to travellers and circus-goers."
	  make brochure readed.
	end verb.    
end the brochure.
-----------------------
synonyms sons,kids,children = boys.
the boys isa object at cabin
	name boys name my two boys name two
	is plural.squeezable.can_talk.
  description
  verb x does only
    "Your two boys Jimmy and Timmy are filled with happy anticipation."
  end verb.
  verb take 
  	does only "You give your boys a loving squeeze and a hug." 
  end verb.
  verb squeeze 
  	does only "You give your boys a loving squeeze and a hug."
  end verb.
  verb talk_about 
  	when act does only
      "Jimmy and Timmy just giggle and wiggle. They'd rather play than
talk."
  end verb.			 
  verb ask 
  	when act does only
      "The boys just shuffle their feet and look down at the floor.
      $p""I dunno,"" they both mumble."
  end verb.	
  verb talk_to 
   	when act does only
      "Jimmy and Timmy just giggle and wiggle. They'd rather play than
talk."
  end verb.			 
end the boys.
-----------------------
the jim isa named_actor at cabin
	name 'Jimmy' name jimmy name jim
	container
	description 
	verb x does only
		"Jimmy is the older of the two boys, at seven years old. He has blond
		hair, blue eyes and freckles. He loves basketball and baseball, and
		wants to play as a pro when he grows up: ""If I'm tall I'll play
		basketball, and if I'm short I'll play shortstop!"""
	end verb.	 
	verb ask
		when act does only
			"""...uhmm, I guess; I mean, I dunno,"" he says."
	end verb.	
	verb talk_about 
		when act does only
		"""Yeah,"" he says, ignoring what you're really saying, ""and when we
		get to the circus I'm gonna pet that big white tiger, and eat some green
		ice -- "" he sticks his tongue out at his brother -- ""Bl-e-a-h, I hab a
		greed pung!"" the boys laugh together."
	end verb.	  
	verb talk_to does only
		"""Yeah,"" he says, ignoring what you're really saying, ""and when we
		get to the circus I'm gonna pet that big white tiger, and eat some green
		ice -- "" he sticks his tongue out at his brother -- ""Bl-e-a-h, I hab a
		greed pung!"" the boys laugh together."
	end verb.	  
end the jim.
-----------------------
the tim isa named_actor at cabin
	name 'Timmy' name timmy name tim
	container
	description
	verb x does only
		"Timmy, at five years old, is younger than his brother by two years. He
		has dark brown hair and blue eyes. He is rather shy, and likes playing
		with his yoyo." 
	end verb.	
	verb ask 
		when act does only
      "Timmy giggles nervously and looks around, as though the answer to
your question
      were somewhere nearby."
	end verb.	
	verb talk_about 
		when act does only
      "Timmy pretends to listen and understand what you're saying as he
flips his yoyo
      up and down and loopdeloop."
	end verb.	
	verb talk_to does only
      "Timmy pretends to listen and understand what you're saying as he
flips his yoyo
      up and down and loopdeloop."
	end verb.	
end the tim.
-----------------------
-- the yoyo isa object in tim
-- 	name yoyo name 'timmy''s' yoyo
-- 	name round 'form' 'of' fluorescent red plastic
-- 	name fluorescent red name fluorescent name round 'form'
-- 	name round name 'string' name 'timmy''s'
-- 	is playable.not seen.turnable.
-- 	description
-- 		if yoyo is seen then
-- 			if yoyo in socket then ""
-- 			else "$pTimmy's yoyo is here."
-- 			end if.	
-- 		else
-- 			"$pOn the ground you see a round form of flourescent red
-- plastic lying
-- 			uncharacteristically still."
-- 		end if.	 				
-- 	verb x does only
-- 		if yoyo in tim then
-- 			"It's a blur of fluorescent red plastic on a string: zzzipp
-- down, zzzip
-- 			up, loopdeloop, whizzzz..."
-- 		elsif yoyo in socket then
-- 			"If only Timmy were here to see his yoyo serving as Waldo's
-- 			big red ""nose""!"
-- 		elsif yoyo is not seen then
-- 			"It is Timmy's yoyo. He would not easily leave it behind."
-- 			make yoyo seen.
-- 		else
-- 			"Timmy's yoyo is made from bright red plastic, about three
-- inches
-- 			across, and a string for playing with it."
-- 		end if.		
-- 	end verb.
-- 	verb take does only
-- 		if yoyo in tim then
-- 			"You wouldn't dream of taking Timmy's yoyo from him."
-- 		elsif yoyo in socket then
-- 			"You may as well leave the yoyo in the door so you can go in
-- and out of
-- 			the cottage. After all this is done you can buy Timmy a
-- brand new and
-- 			flashier yoyo."
-- 		else "Taken."
-- 			locate yoyo in hero.
-- 		end if.
-- 	end verb.			
-- 	verb play_with does only
-- 		if yoyo in tim then
-- 			"You wouldn't dream of taking timmy's yoyo from him."
-- 		elsif yoyo in socket then
-- 			"You may as well leave the yoyo in the door so you can go in
-- and out of
-- 			the cottage. After all this is done you can buy Timmy a
-- brand new and
-- 			flashier yoyo."
-- 		else
-- 			"You flip the yoyo down, zzzz, and up, whizzz."
-- 			locate yoyo in hero. 	
-- 		end if.
-- 	end verb.
--   verb turn does only
--     if yoyo in socket then
-- 			"You turn the yoyo like a doorknob. You can go right into
-- the cottage."
-- 			make frontdoor not locked.
-- 		else "Turning the yoyo doesn't do much."
-- 		end if.	
--   end verb.
-- end the yoyo.		
-----------------------
the newspaper isa object at cabin
	name newspaper name copy name whoopdee
	name copy 'of' 'the' daily newspaper  
	name news name news paper name daily
  name extra bold print headline
  name print name whoopdee dew
  is readable.not readed.weapon.
	description
		"$pThere's a copy of the daily newspaper here."
  verb x does only
    "It's the town of Whoopdeville's morning edition, ""THE WHOOPDEE
DEW"". The
    	headline is in extra bold print."
  	end verb.
 	verb read does only
  	if newspaper not in hero then 
    	"(Picking up the newspaper)$p"
      locate newspaper in hero.
    end if.
	  "The front page news isn't good. The headline reads:
	  $p$t$t$t* ZOMBIE INVASION! *
	  $pSomething strange and terrible is happening to the inhabitants
of Wheewhistle Island:
	  they are turning into zombies!
	  There does not seem to be any explanation for the strange
phenomenon at this time.
	  $nAll the town of Whoopdeville's businesses have closed, as well
as the circus. 
	  The few people who have not become zombies have gone into hiding.
Everyone able to do so is advised 
	  to evacuate the island immediately. All travelers to Wheewhistle
Island or to Boffo's Bigtime Bonanza 
	  Circus are advised to stay away until further notice."  
	  if boys here then
	  	"$p""Sorry, boys,"" you try to explain to Jimmy and Timmy. ""It
looks like we'll
	    have to cancel our trip to the circus.""
	    $nThey look stunned and shocked. You turn to the
	    comics section of the paper in an effort to cheer the boys, but
the comics have
	    been cancelled for the seriousness of the situation.$n"
	  end if.
	  "Turning to the want-ads you see:
		$p$t$t~Adventurer Wanted~
		$n$tLarge Reward Offered!!!
		$n$tApply to Boffo the Clown, town of Whoopdeville.
		$n$tNote: Must have no fear of zombies."
		if boys here then
			"$p""Hmm, well, boys,"" you say, ""there is some hope. It
seems your uncle Boffo is still okay. He
		  must be hiding somewhere on the islan--""
		  $pThere is no answer from the boys. You look up from the
paper. Jimmy and Timmy
		  are gone."
			make stewart toldya.
	  	make stewart panicked.
--	  	locate yoyo at inlog.
	  	locate lifeboat at pier.
	  	locate boys at nowhere.
	  	locate jim at nowhere.
	  	locate tim at nowhere.
	  end if.
	end verb.
end the newspaper.
-----------------------
the cabinscene isa scenery at cabin
	name cabin
	is not takeable.
	verb x does only
	"You are IN the cabin. Look around."
	end verb.
end the cabinscene.	
-----------------------
the cabindoor isa doorobj at cabin
	is open. has otherside deckdoor.
end the cabindoor.
-----------------------
the deck isa location name 'On the Deck'
	entered locate sun here. locate sky here.
  description
    if stewart is not toldya then
		  "Stewart, the boat's steward, approaches.
			$p""I'm so sorry to hear the bad news,"" he says. ""The
captain and I
			were just talking about it. Is that weird or what? 
			$nOh ...how are your boys taking it?"" He looks down
			at them with sympathetic eyes.
			$pYou have no idea what in the world the steward is talking
about, and
			your expression shows it.
		  $n""Oh my,"" he says, clearly embarrassed. ""I hate to be the
one to tell you. I don't think 
		  I could explain it very well in any case. 
		  Perhaps you should read the newspaper for"
		  if newspaper not here then "yourself. There should be a copy
in your cabin.""" 
		  else "yourself."
		  end if.
		  "$nHe smiles at the boys and
		  gives them a salute, then turns back to his duties
elsewhere.$p"
		  make stewart toldya. 
    end if.	
    if stewart is panicked then
    	"The steward is leaning over the rail of the boat just as you
arrive on the
     	deck. He turns and yells out to the bridge above, ""Man
overboard! Man
     	overboard!""
     	$nThe boat's great whistle lets out a big bass ""toooot!"" and
the boat turns in
     	a sharp arc and slows."
			if stewart is not toldya then "Stewart" else "The steward"
			end if.
			"notices you and runs over to you.
      $n""Oh my gosh! I'm so sorry! your boys, they took the lifeboat
and -- ""
      $pThe captain arrives from the bridge and speaks briefly with the
steward,
      sending him to duties elsewhere on the boat. then the captain
turns to you.
      $n""This is terrible. We have never had anyone go overboard
before, certainly no
      children. We are going to do everything we can to find your boys
and get them
      back. but you need to know that if they've gone to the island --
well, I'm as
      sorry as I can be, but we can't go after them there."" 
      $n""Look,"" says the captain, pointing off in the distance. ""See
that strange
      purple fog?""
      $nYou look across the Whoopenholler River. A heavy fog like a
thick
      purple cloud floats on the surface of the water.
      $n""It's surrounding the whole of Wheewhistle Island,"" the
captain explains. ""What
   		with the rocks, the current, and now that fog, it's just too
dangerous.""
      $n""What!?"" you say, astounded and angry. ""If my sons are trying
to get to the
      island -- and I'm sure they are -- they are going into that fog!
You've got to
      follow them! You've got to save them!""
      $n""I'm sorry,"" the captain says. ""It's not possible for a boat
this big to
      navigate through that fog. We would surely wreck on the rocks.
Frankly, that
      little lifeboat your boys are in has a much better chance in that
fog than we
      do. We are doing all we can, and I have radioed for help. but I
simply will not
      take this boat and all her passengers into that fog. Again, I'm
terribly
      sorry.""
      $nHe turns and hurries to help the crew look for the boys and the
lifeboat.$p" 
      make stewart not panicked.
      make captain toldya. 
			schedule nogame at hero after 12. 
    end if.
    "You"
		if boys here then "and the boys"
		end if.
    "are on the deck of an old-time paddlewheel boat. Its great pipe
stack chugs
    clouds of steamy smoke into the sky."
    if boys not here then "Ropes that once held the lifeboat now dangle
freely."
    else "There is a small lifeboat held by ropes to the side of the
boat deck." 
    end if.
    "A door leads back in to your cabin."
    if captain is toldya then "$pThe purple fog is in the distance."
    else "$pAn eerie purple cloud is on the water not too far away."
    end if.	
  verb jump_overboard does only
  	schedule jump_in_river at hero after 0.
  end verb.      		      
  exit north,south,east,west to deck
    check "You walk about on the deck of the boat."
  end exit.
  exit up,down to deck
    check 
			"Passengers are not permitted to the other decks while the
boat is
			moving."
  end exit.
  exit 'in' to cabin does
    if boys here then
    	"You and the boys return to your cabin."
    	locate boys at cabin.
    	locate jim at cabin.
    	locate tim at cabin.
    else "You return to your cabin."
    end if.	
  end exit.
end the deck.
-----------------------
the stewart isa named_actor  
  name 'Stewart' name stewart 'the' steward
  is not toldya.not panicked.
end the stewart.
------------------------
the stewardobj isa farobj at deck
  name 'Stewart' name stewart 'the' steward
  	verb x check
			"The steward has moved on to other areas of the boat, where
he has many duties
			to perform."
	end verb.  	
end the stewardobj.
-----------------------
the captain isa farobj at deck
  is not toldya.
	verb x does only
    "The captain isn't here right now. But you can be pretty certain
that he's doing
    whatever is necessary."
	end verb.	
end the captain.		
-----------------------
the lifeboat isa scenery at deck
	name whoopen 'whaler''s' lifeboat name life boat
	is sittable.enterable.rowable.driveable.not seen.
	description
		if lifeboat is seen then 
			"Below the pier and in the water is the Whoopen Whaler's lifeboat."
		else ""
		end if.
	verb sit_on does only
		if lifeboat at deck then
            describe noboat.
		else
			"It isn't easy, but you carefully climb down the ladder and pivot
			around, lowering yourself into the lifeboat."
			locate hero at lboat.
			locate river at lboat.
		end if.		
	end verb.
	verb enter does only
		if lifeboat at deck then
            describe noboat.
		else
			"It isn't easy, but you carefully climb down the ladder and pivot
			around, lowering yourself into the lifeboat."
			locate hero at lboat.
			locate river at lboat.
		end if.		
	end verb.
	verb x does only
		if lifeboat at deck then
            describe noboat.
		else
			"The lifeboat is rocking up and down on the waves of the water. It is
			for the moment pinned to the pier by the ladder."
		end if.	
	end verb.
	verb row does only
		if lifeboat at deck then
            describe noboat.
		else "You will have to get in the boat first."
		end if.
	end verb.
	verb drive does only
		if lifeboat at deck then
            describe noboat.
		else "You will have to get in the boat first."
		end if.
	end verb.	
end the lifeboat.
-----------------------
the noboat isa object at nowhere
    description
		"The lifeboat is only for emergencies. There is no need to be concerned
		about the lifeboat now."
end the noboat.
-----------------------		
the lboat isa location name 'In the Lifeboat'
	description
		"You are in the lifeboat. It rocks and bobs roughly on the water. There
		are two bench-like seats with a pair of oarlocks for each seat. Only one
		oarlock has an oar." 
	verb row does only
        describe adrift.
	end verb.	   		
	exit up,out to pier does
		"You climb up and out of the lifeboat, back onto the pier."
		locate river at pier.
	end exit.
	exit north,south,east,west to lboat does
        describe adrift.
	end exit.	  		
end the lboat.
-----------------------
the lboatobj isa scenery at lboat
	name life boat name lifeboat
	is rowable.driveable.not takeable. 
	verb x does only
		"You are in the boat." 
	end verb.
	verb row does only
        describe adrift.
	end verb.	   		 	
	verb drive does only
		describe adrift.
	end verb.	
end the lboatobj.
-----------------------	   		 	
the oar isa object at lboat
	name oar name oars
	is rowable.
	description
	verb x does only
		"It's a simple but sturdy wooden oar pivoting in a metal
		oarlock."
	end verb.
	verb row does only
        describe adrift.
	end verb.	   		 
	verb take 
		check "The oar is held securely in its oarlock."
	end verb.	
end the oar.
-----------------------
the adrift isa object at nowhere
    description
		"Using your hands, you push the boat away from the ladder and pier,
		situate yourself on the seat with the one oar and begin to row.
		$nThe swift current takes the boat in a direction you don't want to go.
		No matter how hard you row, the single oar is not enough to overcome the
		strength of the Whoopenholler River. You are carried far away from the
		island and never heard from again."
		schedule uhoh at hero after 0.
end the adrift.
-----------------------
the oarlock isa scenery at lboat
	is not takeable.
	name oarlock name oarlocks
	verb x does only
		"The oarlocks are metal rings and pivots for the oars."
	end verb.
end the oarlock.
-----------------------
the aidkit isa closeable at lboat
	name first aid kit name first aid
	verb x does only
		"It's a small white metal box with a red cross printed on the
top."
	end verb.
end the aidkit.
-----------------------
the ointment isa object in aidkit	 
	name bug bite ointment name tube
	indefinite article "some"
	is wearable.
	verb x does only
		if ointment in worn then
			"The ointment is hardly visible on your skin."
		else "The label on the tube says: ""...for the prevention and
treatment of bug bites."""
		end if.
	end verb.
end the ointment.
-----------------------		
the boatropes isa scenery at deck
	name ropes name rope
	is plural.climbable.
	verb x does only
		if boys here then "The ropes hold the lifeboat." 
		else "The ropes dangle freely over the water."
		end if.
	end verb.
	verb climb
		check "You don't need to climb the ropes."
	end verb.		
end the boatropes.
-----------------------
the preserver isa object at deck
	name life preserver name life
	name round white life preserver
	name ring name donut name jacket
	is wearable.readable.
	description
  	if preserver is not gotten then
      "$pThere is a life preserver attached to the cabin wall."
  	else "$pThere is a life preserver here."
  	end if.
  verb x does only
    "The life preserver is of the common donut style white, about three
feet wide,
    with the name of the boat printed in black block letters: ""THE
WHOOPEN WHALER"""
	end verb.
	verb read does only
		"""THE WHOOPEN WHALER"" "
  end verb. 
	verb wear does only 
    "Taking the life preserver, you slip it over your head and wriggle
it down
    around your chest."
	  make preserver gotten.
  	locate preserver in worn.
	end verb.
	verb remove does only
		"You wriggle it down over your hips to your ankles, and step out
of the
		life preserver."
		locate preserver here.
	end verb.
end the preserver.
-----------------------
the smokesky isa scenery at deck
   name smoke name steam name clouds
end the smokesky.  
-----------------------
the boatdeck isa object at deck
  	name boat deck name paddlewheel boat
  	is not takeable.jump_onable.
  	description
   	verb x does only
      "You are on the deck of the paddlewheel boat. Look around."
  	end verb.
   	verb jump_off does only
  		schedule jump_in_river at hero after 0.
  	end verb.	
end the boatdeck.
-----------------------
the deck_cabin isa scenery at deck
	name passenger cabin
	is enterable.
	verb x does only "You can go back in to your cabin." 
	end verb.
	verb enter does only
  	if boys here then
    	"You and the boys return to your cabin."
    	locate boys at cabin.
    	locate jim at cabin.
    	locate tim at cabin.
    else "You return to your cabin."
    end if.	
		locate hero at cabin.	
	end verb.
end the deck_cabin.
-----------------------
the deckdoor isa doorobj at deck
	is open.has otherside cabindoor.
	verb x does before
    "The door leads from the boat deck back in to the passenger cabin."
	end verb.
end the deckdoor.
-----------------------
the pipestack isa farobj at deck
	name pipestack name pipe
  name great chugging pipestack
  name pipe stack
  verb x does only
    "The pipestack belches puffs of steam in the air. A large steam
whistle attached
    to the pipestack can be sounded by the captain from the bridge."
  end verb.
end the pipestack.
-----------------------
the bridge isa farobj at deck
	verb x does only
    "The bridge is the operations center of the boat. But you knew
that."
	end verb.
end the bridge.
-----------------------
the paddlewheel isa farobj at deck
	name paddlewheel name paddle wheel
	name paddle name paddles
	verb x does only
    "The blades of the paddlewheel cut and splash beneath the surface of
the water
    with confident ease. "
  end verb.
end the paddlewheel.
-----------------------
the whistle isa farobj at deck
	name whistle name large steam whistle
	verb x does only
    "The large whistle looks like it could serve in the low register of
a pipe
    organ. It can be sounded with the steam from the stack with a pull
on a cable
    from the bridge."
 	end verb.
end the whistle.
-----------------------
the river isa farobj at deck
  	name 'Whoopenholler River' name whoopenholler name river name water
  	container header ""
  	is jump_inable.swimmable.enterable.
	verb x does only
	  "The Whoopenholler River is deep and wide and often very choppy.
Its currents
	  are unpredictable, except that the river always seems to flow in
the opposite
	  direction you are trying to go. the swift and strong currents make
it much too
	  dangerous to swim in the river." 
	end verb.
	verb jump_in does only
		schedule jump_in_river at hero after 0.
	end verb.
	verb enter does only
		schedule jump_in_river at hero after 0.
	end verb.
	verb throw_in
	  when obj2
	    check
        "Anything you throw in the river is very likely to be lost
forever. Maybe you
        should hold on to it for now."
	end verb.	 
	verb put_in, drop_in
	  when obj2
	    check
        "Anything you throw in the river is very likely to be lost
forever. Maybe you
        should hold on to it for now."
	end verb.	 
	verb swim does only
		schedule jump_in_river at hero after 0.
	end verb.
end the river.
-----------------------
the purpfog isa farobj at deck
	name purple fog name purple cloud 'of' fog
	name thick purple fog cloud name wheewhistle island
	name eerie purple name eerie
	verb x does only
    "The thick purple fog covers Wheewhistle Island. In fact you can't
see the
    island or anything else that might be in that fog at all."
	end verb.
end the purpfog.
-----------------------
event jump_in_river
	if wwpier not here then
		if boys not here then
			"Suddenly, and to the shock of the others on the boat, you
clamber over
			the rail and jump into the Whoopenholler River. You are
immediately
			pulled down and away by its irresistible" 
	    if preserver not in worn then
	      "currents, never to be seen or heard from again.$n"
	      cancel nogame.
	      schedule uhoh at hero after 0.
	    else
				"currents. Providence is with you, however, as the life
preserver buoys
				you back to the surface. You are tossed about by the
river, but
				mercifully you are at last washed onto the hard wooden
planks of the
				Wheewhistle Pier."
	      cancel nogame.
	      locate hero at pier.
	    end if.
	  else
     	"You start to climb over the rail of the boat.
			$p""Whoa there,"" Stewart intervenes, pulling you back to
the deck. ""I
			don't think that's such a good idea.""
			$pJimmy and Timmy wrap their arms around you and hug you
tightly.
			$n""Don't worry, boys,"" you reassure them. ""I was just
testing the
			rail."" "
	    	if preserver in worn then
					"$nThey don't ask, but clearly seem to wonder why
you're wearing the
					life preserver."
	    	end if.
     	"$pOnce he is satisfied that you are safe, the steward returns
to his duties."	
	  end if.       
	else "There's no need to go through all that again!"
  end if.
end event.
-----------------------
event nogame
	"$pTime goes by. Minutes for you seem like hours; hours seem like
days. There is
	no sign of your sons or of the lifeboat. It finally becomes
painfully clear that
	the boys have vanished into the purple fog.
	$pEventually the paddlewheel boat returns to the home port. It's a
very sad time
	for everyone as you disembark without Jimmy and Timmy."
	schedule uhoh at hero after 0.
end event.
-----------------------

-- REMOVE THE FOLLOWING AFTER TESTING

the pier isa location
description "You are at the pier."
end the pier.

the wwpier isa object at pier
end the wwpier.
