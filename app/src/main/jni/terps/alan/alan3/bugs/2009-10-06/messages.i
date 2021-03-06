-- ALAN NEW LIBRARY: MESSAGES (file name: 'messages.i')


-- This library file lists all the standard runtime messages listed in the manual, for easy modification.



MESSAGE AFTER_BUT: "You must give at least one object after '$1'."
	  AGAIN: "(again)" 	 -- if you wish to disable this, set this message to an empty string ("")
	  BUT_ALL: "You can only use '$1' AFTER '$2'."
	  CAN_NOT_CONTAIN: "$+1 can not contain $+2."
	  CANT0: "You can't do that."
	  CARRIES: "$+1 carries"
	  'CONTAINS': "$+1 contains"
	  CONTAINS_COMMA: "$01,"
          CONTAINS_AND: "$01 and"
	  CONTAINS_END: "$01." 
	  EMPTY_HANDED: "$+1 is empty-handed."
	  HAVE_SCORED: "You have scored $1 points out of $2."
	  IS_EMPTY: "$+1 is empty."
	  MORE: "<More>"
	  MULTIPLE: "You can't refer to multiple objects with '$v'."
	  NO_SUCH: "I can't see any $1 here."
	  NO_WAY: "You can't go that way."
	  NOT_MUCH: "That doesn't leave much to $v!"
	  NOUN: "You must supply a noun."
        NOT_A_SAVEFILE: "That file does not seem to be an Alan game save file."
        QUIT_ACTION: "Do you want to RESTART, RESTORE, QUIT or UNDO? "
		-- these four alternatives are hardwired to the interpreter and cannot be changed.
	  REALLY: "Are you sure (RETURN confirms)?"
	  RESTORE_FROM: "Enter file name to restore from"
	  SAVE_FAILED: "Sorry, save failed."
	  SAVE_MISSING: "Sorry, could not open the save file."
	  SAVE_NAME: "Sorry, the save file did not contain a save for this adventure."
          SAVE_OVERWRITE: "That file already exists, overwrite (y)?"
	  SAVE_VERSION: "Sorry, the save file was created by a different version."
	  SAVE_WHERE: "Enter file name to save in"
	  SEE_START: "There is $01"
	  SEE_COMMA: ", $01"
	  SEE_AND: "and $01"
	  SEE_END: "here."
	  NO_UNDO: "No further undo available."
	  UNDONE: "'$1' undone."
	  UNKNOWN_WORD: "I don't know the word '$1'."
	  WHAT: "I don't understand."
          WHAT_WORD: "I don't know what you mean by '$1'."
	  WHICH_PRONOUN_START: "I don't know if you by '$1'"
	  WHICH_PRONOUN_FIRST: "mean $+1"
	  WHICH_START: "I don't know if you mean $+1"
	  WHICH_COMMA: ", $+1"
	  WHICH_OR: "or $+1."

