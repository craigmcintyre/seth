<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="running.md">Prev: Running SETH</a></td>
    <td style="text-align: right;"><a href="commands.md">Next: Internal Commands/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# Test File Syntax

## Format
Test files are text files with a `.test` extension. Standard `.sql` files (files containing sql scripts and 
comments only) are compatible, but they don't do any expected/actual result comparisons. Test files can be 
considered a superset of sql files.

Test files can contain the following sections:
- cleanup (optional),
- thread blocks (optional)

The cleanup section, if specified, is always executed after a test completes. It allows tests to undo their 
changes whether they complete successfully or with an error. A failure of a statement in the cleanup section 
does not affect the execution of the other statements in this section. A cleanup section is always defined 
at the end of a file.

Thread blocks are used to specify a block of commands that are executed by one or more separate threads. 
They are specified at the time that the threads are created.

Any commands not in the cleanup section or in a thread block is executed by the main thread. Also note that 
there is no "setup" section in the test file. Simply write the necessary statements at the start of the file 
and include the appropriate expected result (if any).

## Execution
The test file is executed until there are no more statements to be executed by the **main thread**, or a 
child thread encounters an error (e.g., an actual/expected comparison failure). At this point any child 
threads are stopped and the main thread then executes the cleanup section, if specified. At the conclusion 
of the execution of the cleanup section the test is complete.

## Connections
When a test file is executed a new connection to the database server will be provided for it. There is no 
need for the test file to be concerned with creating new connections, however that can also be accommodated.

In addition, each new child thread that is created in a test also gets its own connection to the server, so 
there is no sharing of this resource and all threads can act independently.

## Statements and Expected Results
The test harness has vocabulary of internal commands. When parsing a test file, any syntax that does not 
match an internal command is assumed to be a command that should be sent to the server being tested. So a 
typo on an internal command will cause it to be sent to the server erroneously.

Statements generally start from a non-whitespace character until a semicolon at the end of a line.

Statements can be split across multiple lines. Expected results occur on the line below the statement.

Standard SQL line comments using `--` are supported, as are C++ line comments `//` and C block comments `/* */`.

The examples below use SQL commands to represent server statements, but in reality they could be any 
textual command.

Statements that explicitly do not fit the semicolon line ending can be specified by enclosing them in 
curly brackets and followed by a newline. For example:

```
{SELECT * FROM t ORDER BY U+1F4A9}
failure
```

Expected results can be written as follows:

<hr>

```aiignore
SELECT * FROM t;
```
**Above meaning:** No expected result specified, so not expecting success or failure. Failures will not cause test to 
abort but they will be reported in the log.

<hr>

```aiignore
SELECT * FROM t;
success
```
**Above meaning:** Not expecting any errors. Don't care about the details of the result.

<hr>

```aiignore
SELECT * FROM t;
mute
```
**Above meaning:** Not expecting success or failure. Failures will not cause test to abort and they will not 
be reported in the log.

<hr>

```aiignore
SELECT * FROM t;
unordered rows:
(1, 'abc')
(2, 'def')
```
**Above meaning:** 
Expecting 2 rows to be returned. Do not care about the order of the rows.
Rows are defined in parentheses, with a comma separating each row. The last row must not have a comma.
Data types are deduced from how they are written and follow the same rules for writing SQL constants.
E.g., decimals are written out in full (`1.23`) whereas floats are written in a scientific notation form (`1.23e0`),
timestamps are written out as `TIMESTAMP 'yyyy-mm-dd hh:mm:ss.ffff'`, etc.

<hr>

```aiignore
SELECT * FROM t;
rows:
(1, 'abc')
(2, 'def')
```
**Above meaning:** `rows:` is a synonym for `unordered rows:`

<hr>

```aiignore
SELECT * FROM t;
ordered rows:
(1, 'abc')
(2, 'def')
```
**Above meaning:** Expecting 2 rows to be returned. Ordering of the rows is significant.

<hr>

```aiignore
SELECT * FROM t;
ordered rows:
(1, *)
(2, *)
```
**Above meaning:**
Expecting 2 rows to be returned in a given order. Each row must have 2 columns,
but we don't care what the value of the second column is. Only the first column is compared.

<hr>

```aiignore
SELECT * FROM t;
ordered rows:
(1, ...)
(2, ...)
```
**Above meaning:**
Expecting 2 rows to be returned in a given order. Each row must have at least 1 column,
but we don't care what the value of the columns after the first column are. Only the first column 
is compared. So `...` means zero or more columns whose values we are not interested in.

<hr>

```aiignore
SELECT * FROM t;
rows:
['col1', 'col2', 'col3']
(1, 'abc', true)
(2, 'def', false)
```
**Above meaning:** column names can be specified in square brackets. It is an error if an expected 
column name does not match the actual column name returned.

<hr>

```aiignore
SELECT * FROM t;
rows:
['col1', *, 'col3']
(1, 'abc', true)
(2, 'def', false)
```
**Above meaning:** column names are typically strings but they can also be specified using an asterisk 
to indicate that you don’t care about the name of that specific column.

<hr>

```aiignore
SELECT * FROM t;
rows:
['col1', ...]
(1, 'abc', true)
(2, 'def', false)
```
**Above meaning:** column names can also be specified using an ellipsis (three dots) to indicate that 
you don’t care about the names of any other columns from this column forward. If the ellipsis is used 
then it must be the last expected column name that is specified. i.e., you cannot specify a string 
column name or a don’t care column name (asterisk character) after specifying an ellipsis.

<hr>

```aiignore
SELECT * FROM t;
contains rows:
(1, 'abc', true)
(2, 'def', false)
```
**Above meaning:** All of the rows specified above must be present in the ResultSet. 
Any other rows in the ResultSet are ignored.

<hr>

```aiignore
SELECT * FROM t;
does not contain rows:
(1, 'abc', true)
(2, 'def', false)
```
**Above meaning:** Any of the rows specified above must not be present in the ResultSet. 
Any other rows in the ResultSet are ignored.

<hr>

```aiignore
SELECT * FROM t;
rows: 2
```
**Above meaning:** 2 rows are expected to be returned from the statement. Don't care about the 
contents. This only applies to `SELECT` statements as a ResultSet must be returned. This distinguishes 
it from the `affected: <count>` expected result where a ResultSet must not be returned.

<hr>

```aiignore
SELECT * FROM t;
row range: [<lowerCount>, <upperCount>)
```
**Above meaning:** 
Allows the user to specify that the number of rows returned must fall within a given range.
The range can be specified with inclusive or exclusive lower and upper bounds by using a square 
bracket or parenthesis respectively.

An unlimited lower or upper bound can be specified by not providing a numeric value. Note that the comma must be supplied.

e.g.: `row range: [1, 5)` means there must be 1 or more but less than 5 rows returned.

e.g.: `row range: (0, )` means that there must be more than zero rows returned.

e.g.: `row range: ( , 10]` means that there must be 10 or less rows returned.

This expected result doesn't care about the row contents. This only applies to `SELECT` statements as 
a ResultSet must be returned. This distinguishes it from the `affected: <count>` expected result where 
a ResultSet must not be returned.

<hr>

```aiignore
INSERT INTO t VALUES (1, 'abc');
affected: 1
```
**Above meaning:** An affected row count of 1 is expected. Can be applicable to inserts/updates/deletes only as
it would be an error if a result set were returned.

<hr>

```aiignore
SELECT * FROM t;
failure
```
**Above meaning:** Don't care about the type of failure, just that the statement did not succeed.

<hr>

```aiignore
SELECT * FROM t;
failure: 42
```
**Above meaning:** Failure with a specific error number is expected. Uses the vendor-specific error-code.

<hr>

```aiignore
SELECT * FROM t;
failure: "flux capacitor is empty"
```
**Above meaning:** failure with a specific error message is expected. The expected result string must 
be an exact match for the beginning substring of the actual result error message. i.e., The actual 
result error message can be longer but the strings are only compared up to the length of the expected 
result error message provided.

<hr>

```aiignore
SELECT * FROM t;
failure prefix: "flux capacitor is empty"
```
**Above meaning:** Same as immediately above. Failure with a specific error message is expected. The expected 
result string must be an exact match for the beginning substring of the actual result error message. i.e., 
The actual result error message can be longer but the strings are only compared up to the length of the 
expected result error message provided.

<hr>

```aiignore
SELECT * FROM t;
failure suffix: "flux capacitor is empty"
```
**Above meaning:** failure with a specific error message is expected. The expected result string must 
be an exact match for the end substring of the actual result error message. i.e., The actual result 
error message can be longer but the expected result must be a trailing subset of the actual result string.

<hr>

```aiignore
SELECT * FROM t;
failure contains: "flux capacitor is empty"
```
**Above meaning:** failure with a specific error message is expected. The expected result string must be a 
subset of the actual result error message.

<hr>

```aiignore
SELECT * FROM t;
failure contains all: "flux capacitor is empty", "to infinity and beyond"
```
**Above meaning:** failure with a specific error message is expected. Each of the expected result strings 
must be present as a subset of the actual result error message.

<hr>

```aiignore
SELECT * FROM t;
failure contains any: "flux capacitor is empty", "to infinity and beyond"
```
**Above meaning:** failure with a specific error message is expected. Any of the expected result strings 
must be present as a subset of the actual result error message.

<hr>

```aiignore
SELECT * FROM t;
failure: 42, "flux capacitor is empty"
```
**Above meaning:** failure with both a specific error number and message is expected. The error number 
must match exactly. The expected result string must be an exact match for the beginning substring of 
the actual result error message. i.e., The actual result error message can be longer but the strings 
are only compared up to the length of the expected result error message provided.

<hr>

```aiignore
SELECT * FROM t;
failure prefix: 42, "flux capacitor is empty"
```
**Above meaning:** Same as immediately above. Failure with both a specific error number and message is 
expected. The error number must match exactly. The expected result string must be an exact match for 
the beginning substring of the actual result error message. i.e., The actual result error message can 
be longer but the strings are only compared up to the length of the expected result error message provided.

<hr>

```aiignore
SELECT * FROM t;
failure suffix: 42, "flux capacitor is empty"
```
**Above meaning:** failure with both a specific error number and message is expected. The error number 
must match exactly. The expected result string must be an exact match for the end substring of the actual 
result error message. i.e., The actual result error message can be longer but the expected result must be 
a trailing subset of the actual result string.

<hr>

```aiignore
SELECT * FROM t;
failure contains: 42, "flux capacitor is empty"
```
Above meaning: failure with both a specific error number and message is expected. The error number must 
match exactly. The expected result string must be a subset of the actual result error message.

<hr>

```aiignore
SELECT * FROM t;
result file: 'path/to/my/file'
```
**Above meaning:** reads an expected result from the specified file. The referenced file may not include 
any other syntax elements other than the expected result. e.g., the referenced file may contain this content:

```
unordered rows:
("TABLES")
('INDEXES')
('SCHEMAS')
('NODES')
('FEDERATIONS')
('CONNECTORS')
('TABLE_PROVIDERS')
('COLUMNS')
('CATALOGS')
('T1')
```

Specifying results in a separate file may be useful where the expected result is quite large (e.g., 
it has lots of rows) and it would make the test file more difficult to read. By default, a relative 
path to a result file will be relative to the test file that references it. That way both the test 
file and result file can be moved consistently and they can still be invoked without requiring any changes.

<hr>

```aiignore
SELECT * FROM t;
warning
```
**Above meaning:** The statement must succeed and there must be at least one warning returned. 
Don't care about the message of the warning.

<hr>

```aiignore
SELECT * FROM t;
warnings: 2
```
**Above meaning:** The statement must succeed and a given number of warnings must be returned. 
Don't care about the message of the warning.

```
SELECT * FROM t;
warning: "flux capacitor is empty"
```
**Above meaning:** success with a specific warning message is expected. The warning can come from the 
JDBC Statement object or the JDBC ResultSet object. The expected warning string must be an exact 
match for the beginning substring of the actual warning message returned. i.e., The actual warning 
message can be longer but the strings are only compared up to the length of the expected warning message 
provided. Any additional warning messages are ignored so long as one of them matches.

<hr>

```aiignore
SELECT * FROM t;
warning prefix: "flux capacitor is empty"
```
**Above meaning:** Same as immediately above. success with a specific warning message is expected. The 
warning can come from the JDBC Statement object or the JDBC ResultSet object. The expected warning 
string must be an exact match for the beginning substring of the actual warning message returned. 
i.e., The actual warning message can be longer but the strings are only compared up to the length 
of the expected warning message provided. Any additional warning messages are ignored so long as 
one of them matches.

<hr>

```aiignore
SELECT * FROM t;
warning suffix: "flux capacitor is empty"
```
**Above meaning:** success with a specific warning message is expected. The warning can come from the 
JDBC Statement object or the JDBC ResultSet object. The expected warning string must be an exact 
match for the end substring of the actual warning message returned. i.e., The actual warning message 
can be longer but the expected warning message must be a trailing subset of the actual warning message. 
Any additional warning messages are ignored so long as one of them matches.

<hr>

```aiignore
SELECT * FROM t;
warning contains: "flux capacitor is empty"
```
**Above meaning:** failure with a specific error message is expected. The expected result string must 
be a subset of the actual result error message. Alternatively, success with a specific warning message 
is expected. The warning can come from the JDBC Statement object or the JDBC ResultSet object. The 
expected warning string must be a subset of the actual warning message returned. Any additional warning 
messages are ignored so long as one of them matches.


## Expected Result Data Types
When writing an expected result and specifying rows, the way the column values are written determines 
how SETH treats the values. The following rules apply:

- `null` is treated as a SQL NULL value.
- a value consisting of digits is treated as an integer. e.g. `123`
- a value consisting of digits, a period and an exponent (e) is treated as a floating point value. e.g. `1.23e2`
- a value consisting of digits and a period is treated as a floating point value. e.g. `1.23`
- a value of true or false is treated as a boolean.
- a value enclosed in single quotes is treated as a string.
- Any of the following formats is treated as a date:
    - `DATE 'YYYY-MM-DD'`
    - `YYYY-MM-DD`
- Any of the following formats is treated as a time:
    - `TIME 'hh:mm:ss'`
    - `hh:mm:ss`
- Any of the following formats is treated as a timestamp:
    - `TIMESTAMP 'YYYY-MM-DD hh:mm:ss[.ffffff]'`
    - `TIMESTAMP 'YYYY-MM-DDThh:mm:ss[.ffffff]'`
    - `TIMESTAMP 'YYYY-MM-DD hh:mm:ss[.ffffff]Z'`
    - `TIMESTAMP 'YYYY-MM-DDThh:mm:ss[.ffffff]Z'`
    - `YYYY-MM-DD hh:mm:ss[.ffffff]`
    - `YYYY-MM-DDThh:mm:ss[.ffffff]`
    - `YYYY-MM-DD hh:mm:ss[.ffffff]Z`
    - `YYYY-MM-DDThh:mm:ss[.ffffff]Z`
- Intervals are written in the standard SQL format, like this:
    - `INTERVAL '1-2' YEAR TO MONTH`
    - `INTERVAL -'2' MONTH`
    - `INTERVAL '1 2:3:4.5' DAY TO SECOND`
    - `INTERVAL -'3:4.000005' MINUTE TO SECOND`
    - `INTERVAL '5' SECOND`
- A value of `*` means that SETH shouldn't care about the value of a given column.
- A value of `...` means that SETH shouldn't care about the value of a given column and any columns after it.


## Comparison of Floating Point Values

Floating point values are an approximate data type - they have a very wide range but they cannot 
represent all numbers precisely. Because of this SETH has logic to carefully and systematically 
compare an expected and actual floating point value. With an exact numeric type like integers, 
SETH can simply compare the two values directly. However doing this with floating point values 
is likely to return an answer that the two values do not match, yet when they are printed out 
they look identical. The reason for this lies in the least significant digits of the floating 
point values, and these least significant digits may not be printed to the screen. The expected 
and actual values can differ by a tiny amount and any difference is enough for a direct comparison to fail.

Instead SETH compares floating point values by rounding the actual value to a specific level of 
precision and then comparing the expected and actual values at that level of precision. The level 
of precision that is used is taken from how the expected value is specified. For example, if the 
expected value is `0.128e0` then there are 3 decimal places in this expected value. The actual value 
is rounded to 3 decimal places and then a comparison is made. So if the actual value was `0.128000001e0` 
then this gets rounded to 0.128e0 and the comparison succeeds. However if the actual value is `0.12859463e0` 
then this first gets rounded to `0.129e0` and the comparison will fail.

So when specifying a floating point value as an expected value, pay particular attention to the number 
of decimal places that are being specified, as this determines the acceptable level of precision of 
the actual value.


## Comparison of Intervals

The interval data type is not natively supported in JDBC, despite it being well defined in the SQL 
standard. It is not possible in JDBC to simply get a column as an interval data type. As a result, 
SETH needs to retrieve intervals as strings, parse them to extract out the component fields, and then 
compare the actual interval value against the expected interval value. Because of this, intervals are 
not supported on platforms that do not have a string representation of their interval class that matches 
the SQL definition of an interval literal. Postgres is an example of this. It returns a string representation 
of its intervals that are non-standard. SETH was modified to support this, but SETH would need to be modified 
to support additional databases with non-standard interval representations.

As a result, SETH currently only supports interval literals on RapidsSE and Postgresql.
Each database can also treat intervals in different ways. So SETH needs to be careful when comparing interval 
values - it needs to compare them with nuance. For example, SETH is unable to get the specific type of interval 
(e.g. `HOUR TO SECOND`) from Postgres through the JDBC driver, so SETH cannot ensure that the expected value 
and actual value have the same specific interval type.

Here is how SETH compares intervals:
1. If SETH is able to get the specific interval type (e.g. `HOUR TO SECOND`) from the actual result, 
then this is compared to the specific interval type of the expected result. It is an error if both 
do not exactly match.

    - SETH may be able to gleam from the field values in the actual result whether it is a `YEAR-MONTH` 
   or a `DAY-TIME` category of interval. If so, SETH will check that the category of the expected value matches.
   
2. SETH compares only the relevant fields for the two intervals. If multiple fields are involved, the 
relevant fields are normalised down to the least significant field before comparison.

    - Example 1:

        - If both the expected and actual values are of type `INTERVAL YEAR`, then only the year field 
        of each is directly compared.
        - However if the actual value came from Postgres such that SETH only knows that it is some sort 
        of `YEAR-MONTH` category of interval, then both the expected and actual values are treated as 
        `YEAR TO MONTH` intervals. In this case, the year field of both the expected and actual values 
        are normalised to months before they are compared. This ensures that an expected value of 1 year 
        is treated as equivalent to an actual value of 12 months.

    - Example 2:

        - If both the expected and actual results were of type `INTERVAL HOUR TO MINUTE` then only the 
        hour and minute fields would be involved in the comparison. The day, second and microsecond fields 
        would be ignored. The hour field would first be normalised to minutes before comparison, that way 
        a value of 1 hour is treated as being the same as a value of 60 minutes.



<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="running.md">Prev: Running SETH</a></td>
    <td style="text-align: right;"><a href="commands.md">Next: Internal Commands/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>