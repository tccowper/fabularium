-- nowhere.i
-- Library version 0.5.0

-- 0.4.1 - converted to ALANv3



SYNONYMS
	n = north.
	s = south.
	e = east.
	w = west.
	ne = northeast.
	se = southeast.
	nw = northwest.
	sw = southwest.
	u = up.
	d = down.

-- Useful for placing disappearing things
-- Also defines the default directions


THE nowhere ISA LOCATION.
	EXIT north TO nowhere.
	EXIT south TO nowhere.
	EXIT west TO nowhere.
	EXIT east TO nowhere.
	EXIT northeast TO nowhere.
	EXIT southeast TO nowhere.
	EXIT northwest TO nowhere.
	EXIT southwest TO nowhere.
	EXIT up TO nowhere.
	EXIT down TO nowhere.
END THE nowhere.
