parser grammar ToylangParser;

options { tokenVocab=ToylangLexer; }

toylangFile             : lines=line+ ;

line                    : statement(NEWLINE | EOF)*;

statement               : letDeclaration | fnDeclaration;

letDeclaration          : LET (MUT?) IDENT assignment;

fnDeclaration           : FN IDENT fnParams codeBlock;

fnParam                 : IDENT COLON type;
fnParams                : LPAREN ((fnParam) | (fnParam COMMA fnParam))* RPAREN;
codeBlockStatements     : statement+;
codeBlock               : LCURLBRACE codeBlockStatements RCURLBRACE;
returnStatement         : RETURN expression SEMICOLON;

assignment              : ASSIGN expression;
expression              : left=expression operator=(DIVISION|ASTERISK) right=expression # binaryOperation
                        | left=expression operator=(PLUS|MINUS) right=expression        # binaryOperation
                        | LPAREN expression RPAREN                                      # parenExpression
                        | IDENT                                                         # valueReference
                        | MINUS expression                                              # minusExpression
                        | STRING_OPEN (parts+=stringLiteralContent)* STRING_CLOSE       # stringLiteral
                        | INTLITERAL                                                    # intLiteral
                        | DECIMALLITERAL                                                # decimalLiteral ;


stringLiteralContent    : STRING_CONTENT
                        | INTERPOLATION_OPEN expression INTERPOLATION_CLOSE;

type                    : IDENT;