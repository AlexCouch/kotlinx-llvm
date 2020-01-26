parser grammar ToylangParser;

options { tokenVocab=ToylangLexer; }

toylangFile             : lines=line+ ;

line                    : statement(NEWLINE | EOF)*;

statement               : letDeclaration | fnDeclaration;

letDeclaration          : LET (MUT?) IDENT assignment SEMICOLON;

fnDeclaration           : FN IDENT fnParams fnType? codeBlock;

fnParam                 : IDENT COLON type;
fnParams                : LPAREN ((fnParam) | (fnParam COMMA fnParam))* RPAREN;
fnType                  : COLON type;
codeBlockStatements     : statement+;
codeBlock               : LCURLBRACE codeBlockStatements returnStatement? RCURLBRACE;
returnStatement         : RETURN expression SEMICOLON;

assignment              : ASSIGN expression;
expression              : left=expression operator=(DIVISION|ASTERISK) right=expression # binaryOperation
                        | left=expression operator=(PLUS|MINUS) right=expression        # binaryOperation
                        | LPAREN expression RPAREN                                      # parenExpression
                        | MINUS expression                                              # minusExpression
                        | STRING_OPEN (parts+=stringLiteralContent)* STRING_CLOSE       # stringLiteral
                        | INTLITERAL                                                    # intLiteral
                        | DECIMALLITERAL                                                # decimalLiteral
                        | IDENT                                                         # valueReference;


stringLiteralContent    : STRING_CONTENT
                        | INTERPOLATION_OPEN expression INTERPOLATION_CLOSE;

type                    : IDENT;