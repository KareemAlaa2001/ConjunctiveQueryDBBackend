grammar Minibase;

query
    : head ':-' body
    ;

head
    : ID_UPPER '(' ')'
    | ID_UPPER '(' variable (',' variable)* ')'
    ;

body
    : atom (',' atom)*
    ;

atom
    : relationalAtom
    | comparisonAtom
    ;

relationalAtom
    : ID_UPPER '(' term (',' term)* ')'
    ;

comparisonAtom
    : term cmpOp term
    ;

term
    : variable
    | constant
    ;

variable
    : ID_LOWER
    ;

constant
    : INT
    | STRING
    ;

cmpOp
    : '=' | '!=' | '<' | '<=' | '>' | '>='
    ;

INT : [0-9]+ ;

STRING : '\'' [a-zA-Z \t]* '\'' ;

ID_UPPER : [A-Z]+ ;

ID_LOWER : [a-z]+ ;

WS : [ \t\r\n]+ -> skip ;
