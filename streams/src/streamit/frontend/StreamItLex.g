/*
 * StreamItLex.g: Lexical tokens for StreamIt
 * $Id: StreamItLex.g,v 1.1 2002-06-12 17:57:29 dmaze Exp $
 */

header {
	package streamit.frontend;
}

options {
	mangleLiteralPrefix = "TK_";
	// language="Cpp";
}

class StreamItLex extends Lexer;
options {
	exportVocab=StreamItLex;
	charVocabulary = '\3'..'\377';
	k=3;
}

tokens {
	// Stream types:
	"filter"; "pipeline"; "splitjoin"; "feedbackloop";
	// Composite streams:
	"add";
	// Splitters and joiners:
	"split"; "join";
	"duplicate"; "roundrobin";
	// Feedback loops:
	"body"; "loop"; "enqueue";
	// Special functions:
	"init"; "work";
	// Manipulating tapes:
	"peek"; "pop"; "push";
	// Basic types:
	"float"; "int"; "void";
	// Complicated types:
	"struct"; "template";
	// Control flow:
	"if"; "else"; "while"; "for"; "switch"; "case"; "default";
	// Other:
	"print";
}

ARROW :	"->" ;

WS	:	(' '
	|	'\t'
	|	'\n'	{newline();}
	|	'\r')
		{ _ttype = Token.SKIP; }
	;


SL_COMMENT : 
	"//" 
	(~'\n')* '\n'
	{ _ttype = Token.SKIP; newline(); }
	;

ML_COMMENT
	:	"/*"
		(	{ LA(2)!='/' }? '*'
		|	'\n' { newline(); }
		|	~('*'|'\n')
		)*
		"*/"
			{ $setType(Token.SKIP); }
	;


LPAREN
//options {
//	paraphrase="'('";
//}
	:	'('
	;

RPAREN
//options {
//	paraphrase="')'";
//}
	:	')'
	;

LCURLY:	'{' ;
RCURLY:	'}'	;
LSQUARE: '[' ;
RSQUARE: ']' ;
PLUS: '+' ;
PLUS_EQUALS: "+=" ;
INCREMENT: "++" ;
MINUS: '-' ;
MINUS_EQUALS: "-=" ;
DECREMENT: "--" ;
STAR: '*';
DIV: '/';
MOD: '%';
LOGIC_AND: "&&";
LOGIC_OR: "||";
XOR: '^';
ASSIGN: '=';
EQUAL: "==";
NOT_EQUAL: "!=";
LESS_THAN: '<';
LESS_EQUAL: "<=";
MORE_THAN: '>';
MORE_EQUAL: ">=";
QUESTION: '?';
COLON: ':';
SEMI: ';';
COMMA: ',';
DOT: '.';

CHAR_LITERAL
	:	'\'' (ESC|~'\'') '\''
	;

STRING_LITERAL
	:	'"' (ESC|~'"')* '"'
	;

protected
ESC	:	'\\'
		(	'n'
		|	'r'
		|	't'
		|	'b'
		|	'f'
		|	'"'
		|	'\''
		|	'\\'
		|	'0'..'3'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	DIGIT
				(
					options {
						warnWhenFollowAmbig = false;
					}
				:	DIGIT
				)?
			)?
		|	'4'..'7'
			(
				options {
					warnWhenFollowAmbig = false;
				}
			:	DIGIT
			)?
		)
	;

protected
DIGIT
	:	'0'..'9'
	;

NUMBER
	:	 (DIGIT)+ (DOT (DIGIT)+ )? (('e' | 'E') (DIGIT)+ )?
	;

ID
options {
	testLiterals = true;
	paraphrase = "an identifier";
}
	:	('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*
	;


