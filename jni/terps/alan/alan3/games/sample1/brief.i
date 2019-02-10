-- brief.i
-- Library version 0.5.0


--
-- Use "Visits 0." or "Visits 1000." in the START section if you want
-- the game to start in verbose or brief mode. 
--

SYNTAX
	verbose = verbose.

VERB verbose
	DOES
		Visits 0.
		"Verbose mode is now on."
END VERB verbose.


SYNTAX
	brief = brief.

VERB brief
	DOES
		Visits 1000.
		"Brief mode is now on. Location descriptions will only be shown
		the first time you visit."
END VERB brief.

