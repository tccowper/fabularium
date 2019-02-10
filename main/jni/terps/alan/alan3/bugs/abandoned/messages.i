-- ALAN Standard Library v1.00
-- Messages (file name: 'messages.i')


-- This library file lists all the standard runtime messages listed in the manual, for easy modification.



MESSAGE AFTER_BUT: "You must give at least one object after '$1'."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	  AGAIN: "(again)" 	 -- if you wish to disable this, make the string empty ("")
	  BUT_ALL: "You can only use '$1' AFTER '$2'."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	  CAN_NOT_CONTAIN: "$+1 can not contain $+2."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	  CANT0: "You can't do that."     -- note that the fifth token in CANT0 is a zero, not an 'o'.
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
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
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	  NO_SUCH: "You can't see any such thing."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
		IF hero AT bbr
			 THEN 
				IF value OF bbr_m < 2 
					THEN "(If you tried to guess an object, that was a good shot. Carry on!)" 
						INCREASE value OF bbr_m.
				END IF.
		END IF.
	  NO_WAY: "You can't go that way."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	  NOT_MUCH: "That doesn't leave much to $v!"
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	  NOUN: "You must supply a noun."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
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
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
		IF hero AT bbr
			 THEN 
				IF value OF bbr_m < 2 
					THEN "(If you tried to guess an object, that was a good shot. Carry on!)" 
						INCREASE value OF bbr_m.
				END IF.
		END IF.
	  WHAT: "I don't understand."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
        WHAT_WORD: "I don't know what you mean by '$1'."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.
	  WHICH_PRONOUN_START: "I don't know if you by '$1'"
	  WHICH_PRONOUN_FIRST: "mean $+1"
	  WHICH_START: "I don't know if you mean $+1"
	  WHICH_COMMA: ", $+1"
	  WHICH_OR: "or $+1."
		IF hero AT lr THEN "$p$p(The action stops.)" END IF.

THE bbr_m ISA OBJECT
	HAS value 0.
END THE.


-- These messages are automatically translated into German or Swedish in your game 
-- if you define in the beginning of your source code
--
-- OPTIONS
--    LANGUAGE German.
-- 
-- or
--
-- OPTIONS 
-- 	LANGUAGE Swedish.
--
-- Only the exact equivalents of the above phrases will then be shown; if you change the above English
-- messages to your own wordings, the translations will still be only for the default. 
--
--
-- If you wish to write your game in some other language, you must manually translate
-- the messages above. There might be support for more languages in the future, depending 
-- on demand.

