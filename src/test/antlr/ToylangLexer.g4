lexer grammar ToylangLexer;

channels { WHITESPACE }

NEWLINE             : ('\r\n' | '\r' | '\n') -> channel(WHITESPACE);
WS                  : [\t ]+ -> channel(WHITESPACE);

LET                 : 'let';
MUT                 : 'mut';
FN                  : 'fn';

IDENT               : [a-z][A-Za-z0-9_]+;

INTLITERAL          : [0-9]+;
DECIMALLITERAL      : [0-9]+ 'f' | [0-9]+ '.' [0-9]+;


PLUS                : '+';
MINUS               : '-';
ASTERISK            : '*';
DIVISION            : '/';
ASSIGN              : '=';
LPAREN              : '(';
RPAREN              : ')';
COLON               : ':';
SEMICOLON           : ';';
COMMA               : ',';
LCURLBRACE          : '{';
RCURLBRACE          : '}';

RETURN              : 'return';

STRING_OPEN         : '"' -> pushMode(MODE_IN_STRING);

UNMATCHED           : . ;

mode MODE_IN_STRING;

ESCAPE_STRING_DELIMITER : '\\"';
ESCAPE_SLASH            : '\\\\';
ESCAPE_NEWLINE          : '\\n';
STRING_CLOSE            : '"' -> popMode;
INTERPOLATION_OPEN      : '\\$\\{' -> pushMode(MODE_IN_INTERPOLATION);
STRING_CONTENT          : ~["\n\r\t]+;

STR_UNMATCHED           : . -> type(UNMATCHED);

mode MODE_IN_INTERPOLATION;

INTERPOLATION_CLOSE     : '}' -> popMode;
INTERP_WS               : [\t ]+ -> channel(WHITESPACE), type(WS);
INTERP_LET              : 'let'->type(LET);
INTERP_MUT              : 'mut'->type(MUT);

INTERP_INTLIT           : [0-9]+ -> type(INTLITERAL);
INTERP_DECI_LIT         : ([0-9]+ 'f' | [0-9]+) '.' [0-9]+ -> type(DECIMALLITERAL);

INTERP_PLUS             : '+' -> type(PLUS);
INTERP_MINUS            : '-' -> type(MINUS);
INTERP_ASTERISK         : '*' -> type(ASTERISK);
INTERP_DIVISION         : '/' -> type(DIVISION);
INTERP_LPAREN           : '(' -> type(LPAREN);
INTERP_RPAREN           : ')' -> type(PLUS);

INTERP_IDENT            : [_]*[a-z][A-Za-z0-9_]* -> type(IDENT);
INTERP_STRING_OPEN      : '"' -> type(STRING_OPEN), pushMode(MODE_IN_STRING);
INTERP_UNMATCHED        : . -> type(UNMATCHED);
