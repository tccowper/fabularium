Grammar alan;

WHITESPACE  : (' '|'\r'|'\t'|'\u000C'|'\n') {$channel=HIDDEN;} ;

LINE_COMMENT : '--' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;} ;

fragment DIGIT : '0'..'9' ;

fragment LETTER : 'A' .. 'Z' | 'a' .. 'z' | '_' | '$' | '\u00e0'..'\u00f6' | '\u00f8'..'\u00fe' ;

INTEGER : DIGIT+;

STRING : '"' ~'"' '"' ;

fragment IDENTIFIER : LETTER ( LETTER | DIGIT | '_' )* ;


adventure : optional_options declarations start 
             ;

optional_options : 
                 | genSym0 options 
                    ;

genSym0 : 'options' 
         | 'option' 
            ;

options : option 
        | options option 
           ;

option : ID '.' 
       | ID ID '.' 
       | ID INTEGER '.' 
          ;

declarations : 
             | declarations declaration 
                ;

declaration : 'import' 
            | prompt 
            | messages 
            | class 
            | instance 
            | rule 
            | synonyms 
            | syntax 
            | verb 
            | addition 
            | event 
               ;

prompt : 'prompt' statements 
          ;

attributes : attribute_definition '.' 
           | attributes attribute_definition '.' 
              ;

attribute_definition : ID 
                     | 'not' ID 
                     | ID STRING 
                     | ID ID 
                     | ID optional_minus INTEGER 
                     | ID '{' optional_members '}' 
                        ;

optional_members : 
                 | set_members 
                    ;

set_members : set_member 
            | set_members ',' set_member 
               ;

set_member : what 
           | STRING 
           | optional_minus INTEGER 
              ;

synonyms : 'synonyms' synonym_list 
            ;

synonym_list : synonym_declaration 
             | synonym_list synonym_declaration 
                ;

synonym_declaration : id_list '=' ID '.' 
                       ;

messages : 'message' message_list 
            ;

message_list : message 
             | message_list message 
                ;

message : ID ':' statements 
           ;

syntax : 'syntax' syntax_list 
          ;

syntax_list : syntax_item 
            | syntax_list syntax_item 
               ;

syntax_item : ID '=' syntax_elements optional_syntax_restrictions 
               ;

syntax_elements : syntax_element 
                | syntax_elements syntax_element 
                   ;

syntax_element : ID 
               | '(' ID ')' optional_indicators 
                  ;

optional_indicators : 
                    | optional_indicators indicator 
                       ;

indicator : '*' 
          | '!' 
             ;

syntax_restriction_clauses : syntax_restriction 
                           | syntax_restriction_clauses 'and' 
                                syntax_restriction 
                              ;

syntax_restriction : ID 'isa' restriction_class 'else' statements 
                      ;

restriction_class : ID 
                  | 'container' 
                     ;

optional_syntax_restrictions : '.' 
                             | 'where' syntax_restriction_clauses 
                                ;

verb : verb_header verb_body verb_tail 
        ;

verb_header : optional_meta 'verb' id_list 
               ;

optional_meta : 
              | 'meta' 
                 ;

verb_body : simple_verb_body 
          | verb_alternatives 
             ;

verb_alternatives : verb_alternative 
                  | verb_alternatives verb_alternative 
                     ;

verb_alternative : 'when' ID simple_verb_body 
                    ;

simple_verb_body : optional_checks optional_does 
                    ;

verb_tail : 'end' 'verb' optional_id '.' 
             ;

optional_checks : 
                | 'check' statements 
                | 'check' check_list 
                   ;

check_list : check 
           | check_list 'and' check 
              ;

check : expression 'else' statements 
         ;

optional_does : 
              | does 
                 ;

does : 'does' optional_qual statements 
        ;

class : 'every' ID optional_heritage properties class_tail 
         ;

class_tail : 'end' 'every' optional_id genSym1 
              ;

genSym1 : 
         | '.' 
            ;

addition : 'add' 'to' genSym2 ID optional_heritage properties 
              add_tail 
            ;

genSym2 : 
         | 'every' 
            ;

add_tail : 'end' 'add' genSym3 optional_id genSym4 
            ;

genSym3 : 
         | 'to' 
            ;

genSym4 : 
         | '.' 
            ;

instance : 'the' ID optional_heritage properties instance_tail 
            ;

instance_tail : 'end' 'the' optional_id genSym5 
                 ;

genSym5 : 
         | '.' 
            ;

optional_heritage : 
                  | heritage 
                     ;

heritage : 'isa' ID genSym6 
            ;

genSym6 : 
         | '.' 
            ;

properties : 
           | properties property 
              ;

property : where genSym7 
         | container_properties 
         | description 
         | genSym8 article_or_form 
         | name 
         | pronoun 
         | initialize 
         | entered 
         | mentioned 
         | 'definite' article_or_form 
         | 'negative' article_or_form 
         | is attributes 
         | script 
         | exit 
         | verb 
            ;

genSym7 : 
         | '.' 
            ;

genSym8 : 
         | 'indefinite' 
            ;

exit : 'exit' id_list 'to' ID optional_exit_body '.' 
        ;

optional_exit_body : 
                   | optional_checks optional_does 'end' 'exit' 
                        optional_id 
                      ;

is : 'is' 
   | 'are' 
   | 'has' 
   | 'can' 
      ;

optional_description : 
                     | description 
                        ;

description : 'description' optional_checks optional_does 
            | 'description' statements 
               ;

article_or_form : article 
                | form 
                   ;

article : 'article' 
        | 'article' statements 
           ;

form : 'form' 
     | 'form' statements 
        ;

entered : 'entered' statements 
           ;

initialize : 'initialize' statements 
              ;

mentioned : 'mentioned' statements 
             ;

name : 'name' ids optional_full_stop 
        ;

pronoun : 'pronoun' id_list optional_full_stop 
           ;

optional_full_stop : 
                   | '.' 
                      ;

container_properties : genSym9 optionally_opaque 'container' 
                          container_body 
                        ;

genSym9 : 
         | 'with' 
            ;

optionally_opaque : 
                  | 'opaque' 
                     ;

container_body : optional_taking optional_limits optional_header 
                    optional_empty optional_extract 
               | '.' 
                  ;

optional_taking : 
                | 'taking' ID '.' 
                   ;

optional_limits : 
                | 'limits' limits 
                   ;

limits : limit 
       | limits limit 
          ;

limit : limit_attribute else_or_then statements 
         ;

else_or_then : 'else' 
             | 'then' 
                ;

limit_attribute : attribute_definition 
                | 'count' INTEGER 
                   ;

optional_header : 
                | 'header' statements 
                   ;

optional_empty : 
               | 'else' statements 
                  ;

optional_extract : 
                 | 'extract' optional_checks optional_does 
                 | 'extract' statements 
                    ;

event : event_header statements event_tail 
         ;

event_header : 'event' ID 
                ;

event_tail : 'end' 'event' optional_id '.' 
              ;

script : 'script' ID genSym10 optional_description step_list 
          ;

genSym10 : 
          | '.' 
             ;

step_list : step 
          | step_list step 
             ;

step : 'step' statements 
     | 'step' 'after' expression genSym11 statements 
     | 'step' 'wait' 'until' expression genSym12 statements 
        ;

genSym11 : 
          | '.' 
             ;

genSym12 : 
          | '.' 
             ;

rule : 'when' expression then statements optional_end_when 
        ;

then : '=>' 
     | 'then' 
        ;

optional_end_when : 
                  | 'end' 'when' genSym13 
                     ;

genSym13 : 
          | '.' 
             ;

start : 'start' where '.' optional_statements 
         ;

optional_statements : 
                    | statements 
                       ;

statements : statement 
           | statements statement 
              ;

statement : output_statement 
          | special_statement 
          | manipulation_statement 
          | actor_statement 
          | event_statement 
          | assignment_statement 
          | repetition_statement 
          | conditional_statement 
             ;

output_statement : STRING 
                 | 'describe' what '.' 
                 | 'say' say_form expression '.' 
                 | 'list' primary '.' 
                 | 'show' ID '.' 
                 | 'play' ID '.' 
                 | 'style' ID '.' 
                    ;

say_form : 
         | 'the' 
         | 'an' 
         | 'it' 
         | 'no' 
            ;

manipulation_statement : 'empty' primary optional_where '.' 
                       | 'locate' primary where '.' 
                       | 'include' primary 'in' what '.' 
                       | 'exclude' primary 'from' what '.' 
                          ;

event_statement : 'cancel' what '.' 
                | 'schedule' what optional_where 'after' 
                     expression '.' 
                   ;

assignment_statement : 'make' primary something '.' 
                     | 'strip' optional_first_or_last 
                          optional_expression 
                          optional_word_or_character 'from' expression 
                          optional_into '.' 
                     | 'increase' attribute_reference 
                          optional_by_clause '.' 
                     | 'decrease' attribute_reference 
                          optional_by_clause '.' 
                     | 'set' attribute_reference 'to' expression 
                          '.' 
                        ;

optional_by_clause : 
                   | 'by' expression 
                      ;

optional_first_or_last : 
                       | 'first' 
                       | 'last' 
                          ;

optional_word_or_character : 
                           | 'word' 
                           | 'words' 
                           | 'character' 
                           | 'characters' 
                              ;

optional_into : 
              | 'into' expression 
                 ;

conditional_statement : if_statement 
                      | depending_statement 
                         ;

if_statement : 'if' expression 'then' statements 
                  optional_elsif_list optional_else_part 'end' 'if' 
                  '.' 
                ;

optional_elsif_list : 
                    | elsif_list 
                       ;

elsif_list : 'elsif' expression 'then' statements 
           | elsif_list 'elsif' expression 'then' statements 
              ;

optional_else_part : 
                   | 'else' statements 
                      ;

depending_statement : 'depending' 'on' primary depend_cases 'end' 
                         genSym14 '.' 
                       ;

genSym14 : 'depend' 
          | 'depending' 
             ;

depend_cases : depend_case 
             | depend_cases depend_case 
                ;

depend_case : 'else' statements 
            | right_hand_side 'then' statements 
               ;

repetition_statement : for_each ID optional_loop_filters 'do' 
                          statements 'end' for_each genSym15 
                        ;

genSym15 : 
          | '.' 
             ;

optional_loop_filters : 
                      | filters 
                      | 'between' arithmetic 'and' arithmetic 
                         ;

for_each : 'for' 
         | 'each' 
         | 'for' 'each' 
            ;

actor_statement : 'stop' what '.' 
                | 'use' 'script' ID optional_for_actor '.' 
                   ;

optional_for_actor : 
                   | 'for' what 
                      ;

special_statement : 'quit' '.' 
                  | 'look' '.' 
                  | 'save' '.' 
                  | 'restore' '.' 
                  | 'restart' '.' 
                  | 'score' optional_integer '.' 
                  | 'transcript' on_or_off '.' 
                  | 'system' STRING '.' 
                  | 'visits' INTEGER '.' 
                     ;

on_or_off : 'on' 
          | 'off' 
             ;

optional_expression : 
                    | expression 
                       ;

expression : term 
           | expression 'or' term 
              ;

term : factor 
     | term 'and' factor 
        ;

factor : arithmetic 
       | factor optional_not where 
       | factor optional_not relop arithmetic 
       | factor optional_not 'contains' arithmetic 
       | factor optional_not 'between' arithmetic 'and' 
            arithmetic 
          ;

arithmetic : primary 
           | aggregate filters 
           | primary 'isa' ID 
           | primary is something 
           | arithmetic binop primary 
              ;

filters : filter 
        | filters ',' filter 
           ;

filter : optional_not where 
       | optional_not 'isa' ID 
       | is something 
          ;

right_hand_side : filter 
                | optional_not relop primary 
                | optional_not 'contains' factor 
                | optional_not 'between' arithmetic 'and' 
                     arithmetic 
                   ;

primary : STRING 
        | what 
        | 'score' 
        | optional_minus INTEGER 
        | '{' optional_members '}' 
        | '(' expression ')' 
        | 'random' optional_transitivity 'in' primary 
        | 'random' primary 'to' primary 
           ;

aggregate : 'count' 
          | aggregator 'of' ID 
             ;

aggregator : 'max' 
           | 'min' 
           | 'sum' 
              ;

something : optional_not ID 
             ;

what : simple_what 
     | attribute_reference 
        ;

simple_what : ID 
            | 'this' 
            | 'current' 'actor' 
            | 'current' 'location' 
               ;

attribute_reference : ID 'of' what 
                    | what ':' ID 
                       ;

optional_where : 
               | where 
                  ;

where : optional_transitivity 'here' 
      | optional_transitivity 'nearby' 
      | optional_transitivity 'at' primary 
      | optional_transitivity 'in' primary 
      | optional_transitivity 'near' what 
         ;

binop : '+' 
      | '-' 
      | '*' 
      | '/' 
         ;

relop : '<>' 
      | '=' 
      | '==' 
      | '>=' 
      | '<=' 
      | '>' 
      | '<' 
         ;

optional_qual : 
              | 'before' 
              | 'after' 
              | 'only' 
                 ;

optional_not : 
             | 'not' 
                ;

optional_transitivity : 
                      | 'transitively' 
                      | 'directly' 
                      | 'indirectly' 
                         ;

optional_id : 
            | ID 
               ;

ids : ID 
    | ids ID 
       ;

id_list : ID 
        | id_list ',' ID 
           ;

optional_integer : 
                 | INTEGER 
                    ;

optional_minus : 
               | '-' 
                  ;

ID : IDENTIFIER 
 | 'location' 
 | 'actor' 
 | 'opaque' 
 | 'visits' 
 | 'contains' 
 | 'on' 
 | 'it' 
 | 'of' 
 | 'first' 
 | 'into' 
 | 'taking' 
 | 'off' 
    ;

