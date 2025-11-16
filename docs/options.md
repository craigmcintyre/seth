<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="commands.md">Prev: SETH Commands</a></td>
    <td style="text-align: right;"><a href="variables.md">Next: Variables/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# Options

SETH supports options, which are arbitrary keys and (optional) values that can change how an expected 
result is interpreted. e.g. an option may force string comparisons to be done in case insensitive manner, 
or to force the rounding of numbers to a given number of significant digits before they are compared.

Options can be specified at a number of different levels, where the lower levels override the options 
with the same key name specified at a higher level. The levels within SETH that options can be set include:

1.	The application, via the command line arguments (refer to the `--opt` argument above). Options 
specified here apply to all test files being executed.
2.	In a test file, via the command `SET OPTION` or `UNSET OPTION`. Options specified here apply to 
all statements in the current test file.
3.	Before an expected result. Options specified here apply to the entire expected result 
(e.g. the entire ResultSet).
4.	Before an individual row in an expected result. Options specified here apply to that row only.
      
In the above list, an option on an individual row will always override conflicting options (i.e. those 
with the same <key> name) specified on the expected result.


## Specifying an Option

When specifying an option, the <key> can be specified as a quoted string or as an unquoted string if 
it starts with a character and doesn't contain spaces.

It is optional to specify a <value> for a given <key>. If a value is specified then the following 
formats are accepted:

- quoted strings;
- unquoted names (i.e. must start with a character and not contain any spaces);
- unquoted keywords: `true`, `false`; and
- numbers: integers, floats, decimals
- 
To specify an option before an expected result or a row, simply wrap the option definition in square 
brackets (see examples below). Multiple options can be specified together by separating them with commas.

The presence of an option at one level cannot be removed at another level. e.g. if the `ignorecase` 
option is specified on the command line then this cannot be removed with the command `UNSET OPTION 'ignorecase'` 
because this command works at the test level. Instead the option can be overridden at a lower level, 
e.g. by specifying `SET OPTION ignorecase = false` at the test level.

Seth does not validate the keys or values of options. Options that are misspelled or use unsupported 
values will simply be ignored when they are encountered.

The key names of options are case insensitive.


## Supported Options

Some of the options that currently exist are:

| Name                  | Default                                     | Description |
|-----------------------|---------------------------------------------|-------------|
| `ignorecase`          | `false` (case sensitive)                    | If this key is present and doesn't have a value of `false` or `0` then all string comparisons to expected results are done so in a case insensitive manner. |
| `precisionRounding`   | `-1` (no rounding)                          | If this key is present and has a value greater than zero then all decimals and floats are rounded to this number of significant digits before being compared to the expected result. <br><br>e.g. the value `123.456` has 6 digits of precision. If `precisionRounding` is set to `2` then this value will be rounded to `120.000`. If `precisionRounding` is set to `4` then this value will be rounded to `123.400`. |
| `decimalRounding`     | `-1` (no rounding)                          | If this key is present and has a value greater than zero then all decimals and floats are rounded to this number of decimal places before being compared to the expected result. <br><br>e.g. the value `123.456` has 3 decimal places. If decimalRounding is set to `2` then this value will be rounded to `123.45`. |
| `ignoreTrailingSpace` | `false` (case sensitive)                    | If this key is present and doesn't have a value of `false` or `0` then all actual string values are first stripped of any trailing whitespace before they are compared to expected results. |
| `noVarRefEval`        | `false` (variable references are evaluated) | If this key is present and has a value of `true` then variables references (e.g. `${myVar}`) are not resolved and replaced by their value. Instead the command or string that they are contained in remains unchanged. |
| `badVarRef`           | `error`                                     | This option sets the behaviour when an invalid variable reference is attempting to be resolved. This option has 3 valid values: `error`, `empty` or `noEval`. <br><br>When set to `error` (the default) an error will be produced and the current test will abort. When set to `empty` the invalid variable reference will be replaced by an empty string. When set to `noEval` then the invalid variable reference will not be modified at all and it will remain in the original command or string. |


## Option Examples

```aiignore
SELECT * FROM t;
[ignorecase] unordered rows:
(0,'A'),
(1,'B'),
(2,'C')
```
In the above example an option is specified before the expected result. This option 
will then apply to all rows in that expected result.

<hr>

```aiignore
SELECT * FROM t;
unordered rows:
(0,'A'),
[ignorecase] (1,'B'),
(2,'C')
```
In the above example an option is specified before the second row. This option will only apply to 
this specific row in that expected result.

<hr>

```aiignore
SET OPTION ignorecase;
success
SELECT * FROM t;
unordered rows:
(0,'A'),
(1,'B'),
(2,'C')
```
In the above example an option is specified for the entire test file. This option will then apply 
to all rows in all expected results that follow.

<hr>

```aiignore
./seth.sh --opt "ignorecase" ...
```
In the above example, SETH is started with an option on the command line. All test files will be 
executed with this option.

<hr>

```aiignore
SELECT * FROM t;
[ignorecase, decimalRounding = 2] unordered rows:
(0.00,'A'),
(1.11,'B'),
(2.22,'C')
```
In the above example, multiple options have been specified for a given expected result. Both of 
these options will apply to all rows in this expected result.



<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="commands.md">Prev: SETH Commands</a></td>
    <td style="text-align: right;"><a href="variables.md">Next: Variables/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>