-- meta.i

synonyms
	q = 'quit'.

syntax
	'quit' = 'quit'.

verb 'quit' does
	quit.
end verb.

syntax
	'save' = 'save'.

verb 'save' does
	"Good thinking..."
	save.
	"$pGame saved."
end verb.

syntax
	'restore' = 'restore'.

verb 'restore' does
	restore.
	"Restored."
	look.
end verb.

syntax
	'restart' = 'restart'.

verb 'restart' does
	restart.
end verb.

synonyms
	points = 'score'.

syntax
	'score' = 'score'.

verb 'score' does only
	"The goal of the game is"
		if boys not here then
			"to rescue everyone and save the day. If you succeed,
		 	your score will be 100 per cent.
		 	$pIf you fail, well, the score won't really matter."
		 else
		 	"not about points or scores."
		 end if.
end verb.

syntax
	verbose = verbose.

verb verbose
	check
		"""Verbose"" is the only setting used in this game. Descriptions of
		locations may change, providing help and clues."
end verb.

syntax
	brief = brief.

verb brief
	check
		"""Verbose"" is the only setting used in this game. Descriptions
		of locations may change, providing help and clues."
end verb.

synonyms
	hints, clues, clue = hint.

syntax
	hint = hint.

verb hint does only
	"The only clues and hints in this game are in what you find along the
	way."
end verb.
-----------------------
synonyms
	walkthru = walkthrough.

syntax
	walkthrough = walkthrough.

verb walkthrough does only
	"(A walkthrough is included with the game in a separate file
	called ""walkthru.txt"".)"
end verb.

synonyms
	z = 'wait'.

syntax
	'wait' = 'wait'.

verb 'wait' does
	"Time goes by..."
end verb.

synonyms
	g = again.

syntax
	again = again.

verb again does
	"You can repeat previous commands by using the up and down arrow keys to
	scroll through them -- or if you're using the MS-DOS interpreter you can
	press the F3 key to repeat your last command."
end verb.
