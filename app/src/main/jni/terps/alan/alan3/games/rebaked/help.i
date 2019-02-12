-- help.i

synonyms
	h,intro = help.

syntax
	help = help.

verb help does only
	"""WALDO'S PIE (Rebaked)"" is an adventure of the imagination, in the
	form of Interactive Fiction (""IF""). Unlike other interactive computer
	games and video games, IF tests the powers of your mind, not your
	reflexes. There is no time pressure, no forced play -- only the
	excitement of a fantasy world developing in your imagination, a world of
	giants, evil wizards, alchemy and magic -- and the delight of
	discovering the solutions to puzzles and obstacles between you and your
	goal: saving the day and winning the game.
	$nAs you play the game, you will find that you take part in the
	unfolding story. Indeed, you become central to the development of the
	plot and the story line. What you see (and sometimes hear, taste, touch
	or smell) will be laid out for you, and the rest is up to you: what will
	you do? Which way will you go? How will you get past Tumbo the Giant
	with the bazzleberry pie? Whatever you do, it will affect what happens
	next or later in the story.
	$nIn every place you travel (usually by going 'north', 'south', 'east',
	'west', 'in', 'out', 'up' or 'down'), it pays to examine everything (you
	can type 'examine' or just 'x'): everyday objects might have just the
	clue, or be just the thing you need to move closer to your goal. 'Read'
	roadway signs, books, notes, newspapers; 'talk to' various characters
	you meet (they may not all be human beings); 'take' or 'get' things
	along the way that may help you later.
	$nMost often a simple format of commands will do, such as 'get the book'
	or 'read the sign', though sometimes something like 'put the hat on the
	doll' or 'unlock the door with the key' might be useful. (Type 'look' or
	the letter 'l' to see a description of where you are, or the letter 'i'
	(for 'inventory') to see what you have with you, at any time.)
	$nBe sure to 'save' your game frequently -- sometimes you might get into
	an unfortunate and irreversible situation that ends the game or makes it
	unwinnable before you reach your goal.
	$pGood luck, worthy adventurer -- and most important of all, enjoy the game!
	$p(Please type 'credits' to see who helped in the creation of this game.)"
end verb.

synonyms
	notes, author = credits.

syntax
	credits = credits.

verb credits does only
	"Thank you for playing ""WALDO'S PIE (Rebaked)"". It was written by
	Michael Arnaud, who retains the copyright. Please feel free to e-mail
	him with questions or comments at marn0@cox.net.
	$pThe author thanks Thomas Nilsson, who created and continually updates
	and supports the ALAN 3 authoring language (http://www.alanif.se/)."
end verb.
