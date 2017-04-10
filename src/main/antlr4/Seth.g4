// Copyright 2016 Boray Data Co. Ltd.  All rights reserved.

grammar Seth ;

testFile          : statements cleanupSection? ;

cleanupSection    : CLEANUP statementBlock ;

statementBlock    : '{' statements '}' ;
testFileBlock     : '{' testFile '}' ;

statements        : statement* ;
statement         : sethStatement | sqlStatement ;

sqlStatement      : enclosedSqlStatement | nakedSqlStatement ;
enclosedSqlStatement  : '{' ~('}')+ '}' ;
nakedSqlStatement     : {_input.LT(1).getType() != CLEANUP}?  // SQL statements cannot start with CLEANUP.
                        (~(';' | '}'))+ ';';                  // The parser gets confused without this.


sethStatement     : compoundStatements | singularStatements ;

compoundStatements : loopStatement
                   | createThreadStatement
                   ;

singularStatements : (  sleepStatement
                      | logStatement
                      | synchroniseStmt
                      | createConnStmt
                      | useConnectionStmt
                      | disconnectConnStmt
                      | includeFileStmt
                      | emptyStatement
                     ) ';' ;

loopStatement     : LOOP ( loopCount=INT )? statementBlock ;
createThreadStatement : CREATE (THREAD | (threadCount=INT (THREADS | THREAD))) testFileBlock ;

sleepStatement      : SLEEP  millis=INT ;
logStatement        : LOG  logStr=STR ;
synchroniseStmt     : (SYNCHRONISE | SYNCHRONIZE) (syncName=STR ',')? syncCount=INT ;
createConnStmt      : CREATE CONNECTION connName=STR (',' url=STR)? ;
useConnectionStmt   : USE CONNECTION? conName=STR ;
disconnectConnStmt  : DISCONNECT CONNECTION? ( connName=STR )? ;
includeFileStmt     : INCLUDE FILE?  filePath=STR ;
emptyStatement      : ;


// lexer ------------------------------------------------------


CLEANUP               : C L E A N U P;
CONNECTION            : C O N N E C T I O N;
CREATE                : C R E A T E;
DISCONNECT            : D I S C O N N E C T;
FILE                  : F I L E;
INCLUDE               : I N C L U D E;
LOG                   : L O G;
LOOP                  : L O O P;
SLEEP                 : S L E E P;
SYNCHRONISE           : S Y N C H R O N I S E;
SYNCHRONIZE           : S Y N C H R O N I Z E;
THREADS               : T H R E A D S;
THREAD                : T H R E A D;
USE                   : U S E;

HINT_START            : '/*+';
HINT_END              : BC_END;


fragment A            : 'a' | 'A';
fragment B            : 'b' | 'B';
fragment C            : 'c' | 'C';
fragment D            : 'd' | 'D';
fragment E            : 'e' | 'E';
fragment F            : 'f' | 'F';
fragment G            : 'g' | 'G';
fragment H            : 'h' | 'H';
fragment I            : 'i' | 'I';
fragment J            : 'j' | 'J';
fragment K            : 'k' | 'K';
fragment L            : 'l' | 'L';
fragment M            : 'm' | 'M';
fragment N            : 'n' | 'N';
fragment O            : 'o' | 'O';
fragment P            : 'p' | 'P';
fragment Q            : 'q' | 'Q';
fragment R            : 'r' | 'R';
fragment S            : 's' | 'S';
fragment T            : 't' | 'T';
fragment U            : 'u' | 'U';
fragment V            : 'v' | 'V';
fragment W            : 'w' | 'W';
fragment X            : 'x' | 'X';
fragment Y            : 'y' | 'Y';
fragment Z            : 'z' | 'Z';

fragment ID_LETTER    : 'a' .. 'z' | 'A' .. 'Z' | '_' ;
fragment DIGIT        : '0' .. '9' ;

fragment DUBLDUBL     : '""' ;
fragment DUBLSINGL    : '\'\'' ;

ID  : (ID_LETTER (ID_LETTER | DIGIT)*) | QID ;
QID : '"' (DUBLDUBL | ~["\r\n])* '"';

FLT : ('+' | '-')? (DEC | DIGIT+) ('e' | 'E') ('+' | '-')? DIGIT+ ;
INT : ('+' | '-')? DIGIT+ ;
DEC : ('+' | '-')? ( (DIGIT+ '.' DIGIT*) | '.' DIGIT+ ) ;
STR : '\'' (DUBLSINGL | (~'\''))* '\'' ;

// We put comments and whitespace on a hidden tokeniser channel so that we can reconstruct
// the original statement and pass it on to the execution engine unchanged.
LC :      ('//' | '--') .*? ('\n' | EOF) -> channel(HIDDEN) ;
BC :      '/*' ~('+') .*? BC_END -> channel(HIDDEN) ;   // ignore block comments but don't match SQL hints
BC_END :  '*/';

WS :      [ \t\n\r]+    -> channel(HIDDEN) ;

// Must be the last lexer definition. This catches tokens like '*', '<', etc,
// which aren't implicitly defined in the grammar above but are used in SQL statements.
ALL_OTHER_CHARACTERS : .;