<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="building.md">Prev: Building SETH</a></td>
    <td style="text-align: right;"><a href="test-syntax.md">Next: Test File Syntax/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# Running SETH

## Test Files and Testlist Files
Commands to be executed by SETH (and their expected results) are stored in test files - plain text 
files with a `.test` extension. This allows for grouping of multiple commands that all relate to 
testing the same functionality. 

Over time, a significant library of test files can be created. The paths of these files can be passed
to SETH to tell it to execute them, however that can become very tedious. To make this easier, SETH 
supports a testlist file, which is a plain text file with a `.testlist` extension. This file contains 
a list of paths to test files, one per line.

SETH can be instructed to run individual test files, or a testlist file which references multiple 
test files.

## Starting SETH
SETH is packaged as a single jar file. It can be run directly from the command line with
`java -jar seth.jar`. However it is easier to use a shell script wrapper, `seth.sh`, which sets up 
the environment for you. This can be found in the [src/main/resources](../src/main/resources/seth.sh) 
directory of the project. For convenience it is shown below:

```
#!/bin/bash

# Get the relative path to this file.
DIR=$(dirname "$0")
CLASSPATH=".:*:${DIR}:${DIR}/*"

# Database connection details
HOST="localhost"
PORT="9123"
DATABASE="myDatabase"
#USER?
#PASSWORD?

# Additional arguments for SETH. Enable as required.
#SETH_ARGS="--clean --logtests"   # log all test executions to a file
SETH_ARGS="--clean --logsteps"   # log executions of all steps within each test to a file.

# The JDBC URL that identifies the database and how to connect to it
JDBC_URL="jdbc:postgresql://${HOST}:${PORT}/${DATABASE}"

# Run SETH
java -cp "${CLASSPATH}" com.rapidsdata.seth.Seth -u "${JDBC_URL}" ${SETH_ARGS}  $@
```
With this script, you can run SETH by simply typing `./seth.sh` from the command line and
passing in the appropriate command line arguments.

Please consult the documentation of your database of choice for how to write a JDBC URL. Note
that a username and password may be required to be specified.

SETH needs to be told these two fundamental pieces of information when it is started:
1.	The JDBC connection URL, which determines which JDBC driver to be used by the test harness.
2.	The set of tests to be executed by the test harness.

The JDBC connection URL is specified on the command line with the syntax: `-u <url>`
The JDBC driver that is used **must exist on in the classpath of SETH**. This typically means putting 
the JDBC driver jar file in the same directory as the SETH jar file.

The set of test files to be executed can be specified one of two ways:
1.	As a list of file paths as the last command line parameter.
      e.g. `./seth.sh t1.test t2.test dir/*.test`
2.	By specifying the location of the testlist file which is a file whose contents list all of the 
test files to be executed. This is specified with the `–f` parameter.
      e.g. `./seth.sh -f <path/to/testlist.txt>`


## Testlist File
A Testlist file is a plain text file with a `.testlist` extension. It is a file that simply references
one or more test files to be executed, rather than specifying them all on the command line.

The format of the testlist file is to specify the path to one test file per line. Lines can be commented 
out with a `#`, `--` or `//` at the start of the line to turn that line into a comment. File globs can also
be used within a testlist file. **All files specified are considered relative to the location of the testlist 
file**, not the current working directory of the test harness. This allows for the testlist file and the tests 
it specified to be executed independently of where SETH was started from.

TestList files can also specify other testList files and their contents will be recursively added. After 
handling any globbing in filenames (i.e. specifying asterisks to represent multiple files) SETH considers any 
file name entry that does not end in `.test` (case insensitive) to be another testList file and will then 
process it accordingly.

### Example testlist file contents
```
# This is a comment in a testlist file.
-- This is another comment.
// And another comment.

# Run the test1.test which is in the same directory as this testlist file.
test1.test

# Run test2.test which exists in the dir subdirectory relative
# to this testlist file
dir/test2.test

# Run all the files ending with ".test" that exist in the dir2
# subdirectory relative to this testlist file.
dir2/*.test

# Run all the files ending with ".test" that exist in the dir3
# subdirectory and any directories under that too.
dir3/**.test

# Run all of the files included in the testlist file in dir4/testList
dir4/testList
```

## SETH Command Line Arguments

A basic SETH command line would now look like this:
`java –cp .:* com.rapidsdata.seth.Seth –u <url> -f <path/to/testlist>`

The `seth.sh` script file encapsulates this command line and makes it easier to specify the classpath 
and URL to use, only requiring the user to specify the test files to run:

`./seth.sh -f <path/to/testlist>`

SETH also accepts these command line arguments:

| Argument                                          | Default         | Meaning                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|---------------------------------------------------|-----------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `-u <url>`                                        |                 | The JDBC url that indicates which JDBC driver to use and how to connect to the data source.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       |
| `-f <filepath>`                                   |                 | The path to a file whose contents describe the paths to all the test files to be executed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | 
| `--logtests`                                      |                 | Causes SETH to log the execution of whole tests to a log file in the results directory.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `--logsteps`                                      |                 | Causes SETH to log the execution of individual steps within test files to a log file in the results directory.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `--lognameprefix`                                 |                 | A string that is prepended to the filename of the log file that is written out. Seth will append additional information to this log file, including the timestamp of the start time of the test.                                                                                                                                                                                                                                                                                                                                                                                                                                 |
| `--resultdir`	                                  | `"./results"`   | The path where SETH will write out log files and result files (if any).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
| `--resultname`                                    | `"results.xml"` | The name of the result file in the above result directory. This is not applicable for log results (the default) as a separate result file is not written (the results are in the log file itself).                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `--clean`	                                     |                 | Causes SETH to remove all contents of the result directory prior to running any tests.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
| `--resultformat ["log" \| "junit"]`               | `"log"`         | Specifies how SETH will write out the results of running the tests. junit result format is useful for integration with CI tools such as Bamboo.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |                                                                                                                                                                                                                                                                                                                                                                                                                                                                           
| `--relativity [CWD \| REFERER]`                   | `REFERER`       | Specifies how SETH will interpret relative paths. This is used by the testlist file and also by syntax such as the `INCLUDE` command. When this is set to `CWD` all paths will be considered relative to the current working directory when SETH was executed. When this value is set to `REFERER` (the default), all paths are considered relative to the path of the parent object. E.g., relative paths of test files in the testlist file would be relative to the path of the testlist file itself; a relative path of an `INCLUDE` command would be relative to the path of the test file containing the `INCLUDE` command. | 
| `--unordered`                                     |                 | Treat all ordered row expected results as unordered instead.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| `--nostop`                                        |                 | Don't stop the test when a failure is encountered.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `--script "<test_script_contents>"`               |                 | Used to specify the test script content to execute as a command line argument, instead of specifying the path in the filesystem to the test file to be executed. Cannot also specify a path to a test file or test list file if this option is used.                                                                                                                                                                                                                                                                                                                                                                              |
| `--record`	                                     |                 | Writes out a new test file in the result directory (given by `--resultDir`) whose expected results are generated from the actual results of the current test. The name of the file in the result directory is the same as the current test file's name.                                                                                                                                                                                                                                                                                                                                                                           |
| -`-opt "<key>=<val> [, <key>=<val>]"`             |                 | Applies an option to all test files that are run (unless they are overridden in a file). Refer to the Options section below.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `--var "<varName>=<value> [, <varName>=<value>]"` |                 | Sets one or more variables to their given values. These variables apply to all test files that are executed.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      |
| `-p <val> or --parallel <val>`                    | `1`             | Sets the number of tests that can be run in parallel. The default value of 1 means that each test file executes sequentially. A value of 4 means that no more than 4 test files will execute concurrently.                                                                                                                                                                                                                                                                                                                                                                                                                        |
| `--ignore "<regexPattern>"`                       |                 | Defines a regular expression (regex) that when matched against a command in a test file causes that command to be skipped. Multiple commands can be specified by specifying this option multiple times. <br>e.g. `--ignore "lscol .*"` will ignore all lscol commands irrespective of any parameters given to it.                                                                                                                                                                                                                                                                                                                   |

# Parallel Test Execution
By default SETH executes tests sequentially and in the order that was specified. However in order to speed up 
the execution of many tests, it may be possible to execute them in parallel. This can be achieved with the 
`-p` (or `--parallel`) command line argument that specifies the number of tests to execute concurrently 
(default is 1). e.g. setting this to 4 will execute 4 test files at the same time.

Care should be taken when this is used as all tests that may execute concurrently must not interfere with 
each other. e.g. if two tests have a `CREATE TABLE` statement for the same table name then this will cause 
an error for the second test file trying to create a table that already exists. Any objects that a test file 
creates must be uniquely named in order to avoid this problem. Use of [SETH variables](variables.md) can be used to achieve this.



<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="building.md">Prev: Building SETH</a></td>
    <td style="text-align: right;"><a href="test-syntax.md">Next: Test File Syntax/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>