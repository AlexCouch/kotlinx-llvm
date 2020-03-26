parser grammar ToylangParser;

options { tokenVocab=ToylangLexer; }

toylangFile             : lines=line* EOF;

docComment              : DOC_COMMENT;
line                    : statement NEWLINE*;

statement               : (letDeclaration | fnDeclaration | expression)? SEMICOLON;

letDeclaration          : LET (MUT?) IDENT typeAnnotation? assignment;

typeAnnotation          : COLON type #fnType;
fnDeclaration           : FN IDENT fnParams typeAnnotation? codeBlock;

fnParam                 : IDENT COLON type;
fnParams                : LPAREN (fnParam (COMMA fnParam)*) RPAREN;
fnArgs                  : LPAREN (expression (COMMA expression)*)* RPAREN;
codeblockStatement      : (letDeclaration | expression | returnStatement)? SEMICOLON;
codeBlockStatements     : block = codeblockStatement*;
codeBlock               : LCURLBRACE codeBlockStatements RCURLBRACE;
returnStatement         : RETURN expression;

assignment              : ASSIGN expression;
expression              : IDENT fnArgs                                                          # functionCall
                        | left=expression operator=(FORWORD_SLASH|ASTERISK) right=expression    # binaryOperation
                        | left=expression operator=(PLUS|MINUS) right=expression                # binaryOperation
                        | LPAREN expression RPAREN                                              # parenExpression
                        | MINUS expression                                                      # minusExpression
                        | STRING_OPEN (parts+=stringLiteralContent)* STRING_CLOSE               # stringLiteral
                        | INTLITERAL                                                            # intLiteral
                        | DECIMALLITERAL                                                        # decimalLiteral
                        | IDENT                                                                 # valueReference
                        ;


stringLiteralContent    : STRING_CONTENT
                        | INTERPOLATION_OPEN expression INTERPOLATION_CLOSE;

type                    : IDENT;