<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="options.md">Prev: Options</a></td>
    <td style="text-align: right;"><a href="examples.md">Next: Examples/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# Variables

SETH supports the concept of variables in test files. A variable in a test file is replaced by its 
corresponding value before that command or result is executed by SETH. In this way, SETH variables 
can be used to alter what SETH does at runtime.

## Using Variables

Variables are specified like bash variables with a `${` and then a `}` enclosing the variable name. 
e.g. `${myVariable}`.

Variables can be specified anywhere a regular identifier or string is specified. They are particularly 
useful in DDL statements like `CREATE TABLE` to help ensure that the name of the object created is unique. 
e.g. `CREATE TABLE ${testName}_myTable (...)`. They can also be used in results too.

SETH variables are case insensitive. The variable `${foo}` is resolved the same as `${FOO}`.

## Setting Variables

Variables can be set one of two ways:

1.	In the test file with a `SET VARIABLE` statement
2.	As a command line argument

### SET/UNSET VARIABLE statement

A `SET VARIABLE` statement allows a user to define and set a SETH variable within a test file. That 
variable is set for the life of that test file’s execution. The variable is not defined for any other 
test files. The syntax to define a variable is:

```aiignore
SET (VAR | VARS | VARIABLE | VARIABLES) <varName> = <value> [ , ... ] ;
```

One variable or multiple variables can be set at the same time. The value that a variable is set to 
can be a string (enclosed with single or double quotes), numbers, boolean or IDs (single word 
unquoted identifiers).

Here are some examples:

```aiignore
SET VAR x = 'abcd';
SET VARIABLES x = 'abcd', y = 5, z = true;
```

Variables can also be unset so that they no longer can be resolved to a value. This can be done 
with the `UNSET` command, which takes a list of one or more variable names:

```aiignore
UNSET (VAR | VARS | VARIABLE | VARIABLES) <varName> [ , ... ] ;
```

For example:

```aiignore
UNSET VAR x ;
UNSET VARIABLES x, y, z ;
```

### Setting Variables from the Command Line

Variables can also be specified on the command line. When they are set this way the variables will apply 
to **all** test files that are being executed. If a test file also defines a variable by the same name then 
it will overwrite the previous definition of the variable. Setting a variable from the command line allows 
test files to be written that do slightly different things depending on how they are run. e.g. a variable 
reference might be used to specify a file to be loaded, and this variable might have a different definition 
specified on the command line each time SETH is executed.

Variables are specified via the command line with the `--variables` or `--vars` arguments. Because spaces 
are used to separate command line arguments, the variable name and value pairings must be specified without 
any intervening spaces, or the whole argument must be quoted. Here are some examples:

```aiignore
./seth.sh --vars x='abcd' <other_params...>
./seth.sh --vars x='abcd',y=5,z=true <other_params...>
./seth.sh --vars "x = 'abcd', y = 5, z = true" <other_params...>
```

The first example above sets a single variable, `x`. The second example sets multiple variables, but please 
note how there are no spaces between the comma or equals signs in any name=value pairing. The third example 
has spaces between each name=value pairing as well as before and after the equals signs and because of this 
the whole list of variables is quoted to ensure it is treated as a single argument.

## Resolving Variables

If a variable name cannot be resolved then by default an error is returned and the test fails. This behaviour 
can be controlled with the option `badVarRef`. Setting this option to `error` will mean that an unresolved 
variable reference will give an error. Setting this option to `empty` will mean that an unresolved variable 
reference will be replaced by an empty string (like bash). Setting this option to `noEval` will mean that an 
unresolved variable reference will not be evaluated at all and the variable reference will remain in the 
string as written.

The option `noVarRefEval` can also be set to `true` to tell SETH to not try to resolve any variables at all. 
Any variable references will be left in the test file and it will be executed as written.

## Special Variables

SETH automatically sets up a number of special variables that can be used within test files without having 
to define them beforehand. These are described below:

| Variable Name | Value Description |
| ------------- | ----------------- |
| `${testName}` | The name of root test file, without the file name extension, being executed. If test file `A` includes test file `B` then the value of this variable when test file `B` is being parsed will be `A`. <br><br>e.g. if the test file is named `myFile.test` then this variable will have the value `myFile`. |
| `${testName1}` | Same as `${testName}` above. |
| `${testName2}` through to `${testName9}` | Contains the value of `${testName}` but with the name of parent directories prepended to it.<br><br>e.g. `${testName2}` has a 2-part name with the name of the parent directory, an underscore and then the name of the test file.<br><br>`${testName3}` has a 3-part name with the name of the grandparent directory, an underscore, name of the parent directory, an underscore and then the name of the test file.<br><br>e.g. given the file `/home/user/tests/myFile.test`, the following variable references would resolve as shown:<br><br>`${testName}` would resolve to `myFile`<br><br>`${testName2}` would resolve to `tests_myFile`<br><br>`${testName3}` would resolve to `user_tests_myFile`<br><br>`${testName4}` would resolve to `home_user_tests_myFile`<br><br>…and so on. |



<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="options.md">Prev: Options</a></td>
    <td style="text-align: right;"><a href="examples.md">Next: Examples/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>