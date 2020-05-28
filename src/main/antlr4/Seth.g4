// Copyright 2016 Boray Data Co. Ltd.  All rights reserved.

grammar Seth ;

testFile          : statements cleanupSection? ;

cleanupSection    : CLEANUP statementBlock ;

statementBlock    : '{' statements '}' ;
testFileBlock     : '{' testFile '}' ;

statements        : statement* ;
statement         : (sethStatement | serverStatement) expected=expectedResult? ;

serverStatement      : enclosedServerStatement | nakedServerStatement ;
enclosedServerStatement  : '{' ~('}')+ '}' ;
nakedServerStatement     : {_input.LT(1).getType() != CLEANUP}?   // Server statements cannot start with CLEANUP.
                           (~(';' | '}'))+ ';';                   // The parser gets confused without this
                                                                  // semantic predicate.


sethStatement     : compoundStatements | singularStatements ;

compoundStatements : loopStatement
                   | createThreadStatement
                   | shuffleStatement
                   ;

singularStatements : (  sleepStatement
                      | logStatement
                      | synchroniseStmt
                      | createConnStmt
                      | useConnectionStmt
                      | dropConnectionStmt
                      | includeFileStmt
                      | failStatement
                      | emptyStatement
                     ) ';' ;

loopStatement       : LOOP  (countedLoopStatement | timedLoopStatement);
timedLoopStatement  : FOR count=INT (HOURS | MINUTES | SECONDS | MILLISECONDS) statementBlock;
countedLoopStatement: ( loopCount=INT )? statementBlock;
createThreadStatement : CREATE (THREAD | (threadCount=INT (THREADS | THREAD))) testFileBlock ;
shuffleStatement    : SHUFFLE statementBlock;

sleepStatement      : SLEEP  millis=INT ;
logStatement        : LOG  logStr=STR ;
synchroniseStmt     : (SYNCHRONISE | SYNCHRONIZE) (syncName=STR (',' syncCount=INT)? )? ;
createConnStmt      : CREATE CONNECTION connName=STR (',' url=STR)? ;
useConnectionStmt   : USE CONNECTION? connName=STR ;
dropConnectionStmt  : DROP CONNECTION? ( connName=STR )? ;
includeFileStmt     : INCLUDE FILE?  filePath=STR ;
failStatement       : FAIL (msg=STR)? ;
emptyStatement      : ;


expectedResult      : success
                    | mute
                    | failure
                    | warning
                    | unorderedRows
                    | orderedRows
                    | containsRows
                    | rowCount
                    | rowRange
                    | affectedRowsCount
                    | resultFile ;

resultFile          : RESULT FILE? ':' filePath=STR ;

success             : SUCCESS ;
mute                : MUTE ;
failure             : failureCodeAndMsg | failureErrorCode | failureErrorMsg | failureAny;

failureCodeAndMsg   : failureCodeAndMsgPrefix | failureCodeAndMsgSuffix | failureCodeAndMsgSubset;
failureCodeAndMsgPrefix : FAILURE (PREFIX)? ':' code=INT ',' msg=STR ;
failureCodeAndMsgSuffix : FAILURE SUFFIX ':' code=INT ',' msg=STR ;
failureCodeAndMsgSubset : FAILURE CONTAINS ':' code=INT ',' msg=STR ;

failureErrorCode    : FAILURE ':' code=INT ;

failureErrorMsg     : failureErrorMsgPrefix | failureErrorMsgSuffix | failureErrorMsgSubset;
failureErrorMsgPrefix : FAILURE (PREFIX)? ':' msg=STR ;
failureErrorMsgSuffix : FAILURE SUFFIX ':' msg=STR ;
failureErrorMsgSubset : FAILURE CONTAINS (ALL | ANY)? ':' msgs=strList ;

failureAny          : FAILURE ;

warning             : warningCount | warningMsgPrefix | warningMsgSuffix | warningMsgSubset | warningAny;
warningCount        : WARNINGS ':' count=INT ;
warningMsgPrefix    : WARNING (PREFIX)? ':' msg=STR ;
warningMsgSuffix    : WARNING SUFFIX ':' msg=STR ;
warningMsgSubset    : WARNING CONTAINS ':' msg=STR ;
warningAny          : WARNING ;

unorderedRows       : (UNORDERED)? ROWS ':' resultSet ;
orderedRows         : ORDERED ROWS ':' resultSet ;
containsRows        : ( (DOES | MUST)? NOT)? (CONTAINS | CONTAIN) ROWS ':' resultSet ;
rowCount            : ROWS ':' count=INT ;
rowRange            : ROW RANGE ':' lowerInclusivity=( '[' | '(' ) (lowerVal=INT)? ',' (upperVal=INT)? upperInclusivity=( ']' | ')' ) ;
affectedRowsCount   : AFFECTED ':' count=INT ;

resultSet           : columnNames? rowData+ ;
columnNames         : '[' columnName (',' columnName)* ']' ;
columnName          : stringVal | dontCareVal | ignoreRemainingColumns ;
rowData             : '(' columnData (',' columnData)* ')' ;
columnData          : booleanVal
                    | integerVal
                    | decimalVal
                    | floatVal
                    | stringVal
                    | dateVal
                    | timeVal
                    | timestampVal
                    | intervalVal
                    | nullVal
                    | dontCareVal
                    | ignoreRemainingColumns ;

booleanVal          : TRUE | FALSE ;
integerVal          : INT ;
decimalVal          : DEC ;
floatVal            : FLT ;
stringVal           : STR ;
dateVal             : (DATE STR) | DTE ;
timeVal             : (TIME STR) | TME ;
timestampVal        : (TIMESTAMP STR) | TSP ;
intervalVal         : yearMonthInterval | dayTimeInterval ;
nullVal             : NULL ;
dontCareVal         : ASTERISK ;
ignoreRemainingColumns: ELLIPSIS ;

yearMonthInterval   : INTERVAL ('+' | minus='-')? STR
                      (   y=YEAR
                        | y2m=YEAR TO MONTH
                        | m=MONTH
                      );

dayTimeInterval     : INTERVAL ('+' | minus='-')? STR
                      (   d=DAY
                        | (DAY TO (d2h=HOUR | d2m=MINUTE | d2s=SECOND))
                        | (HOUR TO (h2m=MINUTE | h2s=SECOND))
                        | (m2s=MINUTE TO SECOND)
                        | h=HOUR
                        | m=MINUTE
                        | s=SECOND
                      );

strList             : STR ( (',')? STR )* ;

// lexer ------------------------------------------------------


AFFECTED              : A F F E C T E D;
ALL                   : A L L;
ANY                   : A N Y;
CLEANUP               : C L E A N U P;
CONNECTION            : C O N N E C T I O N;
CONTAINS              : C O N T A I N S;
CONTAIN               : C O N T A I N;
CREATE                : C R E A T E;
DATE                  : D A T E;
DAY                   : D A Y;
DOES                  : D O E S;
DROP                  : D R O P;
FAILURE               : F A I L U R E;
FAIL                  : F A I L;
FALSE                 : F A L S E;
FILE                  : F I L E;
FOR                   : F O R;
HOURS                 : H O U R S;
HOUR                  : H O U R;
INCLUDE               : I N C L U D E;
INTERVAL              : I N T E R V A L;
LOG                   : L O G;
LOOP                  : L O O P;
MILLISECONDS          : M I L L I S E C O N D S;
MINUTES               : M I N U T E S;
MINUTE                : M I N U T E;
MONTH                 : M O N T H;
MUST                  : M U S T;
MUTE                  : M U T E;
NOT                   : N O T;
NULL                  : N U L L;
ORDERED               : O R D E R E D;
PREFIX                : P R E F I X;
RESULT                : R E S U L T;
RANGE                 : R A N G E;
ROWS                  : R O W S;
ROW                   : R O W;
SECONDS               : S E C O N D S;
SECOND                : S E C O N D;
SHUFFLE               : S H U F F L E;
SLEEP                 : S L E E P;
SUCCESS               : S U C C E S S;
SUFFIX                : S U F F I X;
SYNCHRONISE           : S Y N C H R O N I S E;
SYNCHRONIZE           : S Y N C H R O N I Z E;
TEST                  : T E S T;
THREADS               : T H R E A D S;
THREAD                : T H R E A D;
TIMESTAMP             : T I M E S T A M P;
TIME                  : T I M E;
TO                    : T O;
TRUE                  : T R U E;
UNORDERED             : U N O R D E R E D;
USE                   : U S E;
WARNINGS              : W A R N I N G S;
WARNING               : W A R N I N G;
YEAR                  : Y E A R;

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
fragment TWO_DIGITS   : DIGIT DIGIT ;
fragment FOUR_DIGITS  : DIGIT DIGIT DIGIT DIGIT ;

fragment DUBLDUBL     : '""' ;
fragment DUBLSINGL    : '\'\'' ;

fragment SINGLE_STR   : '\'' (DUBLSINGL | (~'\''))* '\'' ;
fragment DOUBLE_STR   : '"'  (DUBLDUBL  | (~'"'))* '"' ;

ID  : (ID_LETTER (ID_LETTER | DIGIT)*) ;

TSP : FOUR_DIGITS '-' TWO_DIGITS '-' TWO_DIGITS (' ' | T) TWO_DIGITS ':' TWO_DIGITS ':' TWO_DIGITS ('.' DIGIT+)? Z?;
DTE : FOUR_DIGITS '-' TWO_DIGITS '-' TWO_DIGITS ;
TME : TWO_DIGITS ':' TWO_DIGITS ':' TWO_DIGITS ('.' DIGIT+)? ;
FLT : ('+' | '-')? (DEC | DIGIT+) ('e' | 'E') ('+' | '-')? DIGIT+ ;
INT : ('+' | '-')? DIGIT+ ;
DEC : ('+' | '-')? ( (DIGIT+ '.' DIGIT*) | '.' DIGIT+ ) ;
STR : SINGLE_STR | DOUBLE_STR ;

// We put comments and whitespace on a hidden tokeniser channel so that we can reconstruct
// the original statement and pass it on to the execution engine unchanged.
LC :      ('//' | '--') .*? ('\n' | EOF) -> channel(HIDDEN) ;
BC :      '/*' ~('+') .*? BC_END -> channel(HIDDEN) ;   // ignore block comments but don't match SQL hints
BC_END :  '*/';

ASTERISK : '*' ;
ELLIPSIS : '...' ;

WS :      [ \t\n\r]+    -> channel(HIDDEN) ;

// Must be the last lexer definition. This catches tokens like '*', '<', etc,
// which aren't implicitly defined in the grammar above but are used in SQL statements.
ALL_OTHER_CHARACTERS : .;