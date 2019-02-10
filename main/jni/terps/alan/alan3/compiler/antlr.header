Grammar alan;

WHITESPACE  : (' '|'\r'|'\t'|'\u000C'|'\n') {$channel=HIDDEN;} ;

LINE_COMMENT : '--' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;} ;

fragment DIGIT : '0'..'9' ;

fragment LETTER : 'A' .. 'Z' | 'a' .. 'z' | '_' | '$' | '\u00e0'..'\u00f6' | '\u00f8'..'\u00fe' ;

INTEGER : DIGIT+;

STRING : '"' ~'"' '"' ;

fragment IDENTIFIER : LETTER ( LETTER | DIGIT | '_' )* ;

