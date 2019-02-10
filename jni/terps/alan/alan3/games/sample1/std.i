-- std.i
-- Library version 0.5.0

-- All verb definitions have a small, simple default body. To make
-- them do other things use DOES ONLY in your specialised verb body.

-- player character, etc

IMPORT 'global.i'.

-- Standard verbs
IMPORT 'look.i'.
IMPORT 'take.i'.    -- + pick up, drop, put down
IMPORT 'open.i'.    -- + close
IMPORT 'lock.i'.    -- + unlock
IMPORT 'eat.i'.     -- + drink
IMPORT 'throw.i'.
IMPORT 'push.i'.
IMPORT 'touch.i'.
IMPORT 'examine.i'. -- + look at, search
IMPORT 'read.i'.
IMPORT 'put.i'.     -- + put near,behind,on,under 
IMPORT 'give.i'.
IMPORT 'talk.i'.    -- + ask, tell, say, shout, 
IMPORT 'attack.i'.  -- + shoot
IMPORT 'kiss.i'.
IMPORT 'turn.i'.    -- + switch
IMPORT 'listen.i'.
IMPORT 'smell.i'.
IMPORT 'knock.i'.
IMPORT 'jump.i'.
IMPORT 'wear.i'.   -- + remove, undress, put on

IMPORT 'help.i'.    -- + notes, hint


-- Scenery

IMPORT 'scenery.i'.

-- Inventory verb and inventory limits (including clothing items)

IMPORT 'invent.i'.



-- The limbo location and directions
-- Defines directions as full words, and short directions as synonyms to these.
-- So remember to use the full words in your exits or you will have E 333's
-- (e.g. 'e' defined both as a synonym and another word class)

IMPORT 'nowhere.i'.


-- Verbose and brief mode

IMPORT 'brief.i'.


-- Score, save, restore etc.

IMPORT 'meta.i'.
