-- meta.i
-- Library version 0.5.0


SYNTAX
	'quit' = 'quit'.
	q = q.

VERB 'quit'
	DOES
		QUIT.
END VERB.

VERB q
	DOES
		"Please write the full 'quit' command to exit from the game.
		$p(You can't undo a quit instruction. So to avoid accidentally 
		exiting the game by typing 'q' when you meant to do something else, 
		you must write the 'quit' command in full.)"
END VERB.


SYNTAX
	'save' = 'save'.

VERB 'save'
	DOES
		SAVE.
		"Ok."
END VERB.


SYNTAX
	'restore' = 'restore'.

VERB 'restore'
	DOES
		RESTORE.
		"Done.$n"
		LOOK.
END VERB.


SYNTAX
	'restart' = 'restart'.

VERB 'restart'
	DOES
		RESTART.
END VERB.



SYNTAX
	'score' = 'score'.

VERB 'score'
	DOES
		SCORE.
END VERB 'score'.


SYNONYMS
	z = 'wait'.

SYNTAX
	'wait' = 'wait'.

VERB 'wait'
	DOES
		"Time passes..."
END VERB.


SYNONYMS
	g = again.

SYNTAX
	again = again.

VERB again
	DOES
		"The 'again' command is not available, sorry. You can probably use 
		the up and down arrow keys to scroll through your previous commands 
		(unless you're using the MSDOS interpreter in which case you can 
		press the F3 key to repeat your last command.)"
END VERB.


--SYNTAX
--    undo = undo.

--VERB undo
--    DOES
--        "Unfortunately you cannot 'undo' commands in this game."
--END VERB.

