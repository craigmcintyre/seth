<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="test-syntax.md">Prev: Test File Syntax</a></td>
    <td style="text-align: right;"><a href="options.md">Next: Options/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# Internal Commands
The test harness will also recognise a number of internal commands to help write more advanced tests. 
These are described below:

## Looping
```aiignore
LOOP <int> {
code_block
}
```
**Above meaning:** Loops the commands in code_block a given number of times.

<hr>

```aiignore
LOOP {
code_block
}
```
**Above meaning:** Loops the commands in code_block forever.

<hr> 

```aiignore
LOOP FOR <int> (HOURS | MINUTES | SECONDS | MILLISECONDS) {
code_block
}
```
**Above meaning:** Loops the commands in code_block for a given amount of time instead of iteration count.

## Shuffling Statements
```aiignore
SHUFFLE {
code_block
}
```
**Above meaning:** Executes the statements in the code_block in a randomised order.

## Sleeping
```aiignore
SLEEP <int_millis>;
```
**Above meaning:** Causes the current thread to stop executing for the given number of milliseconds.

## Logging
```aiignore
LOG 'Some log message.';
```
**Above meaning:** Causes the current thread to write a timestamped message to the test log.

## Creating Threads
```aiignore
CREATE THREAD {
code_block
}
```
**Above meaning:** Creates a single thread, which executes the code in code_block. Threads automatically 
get their own connection so they are not using the main thread's one.

<hr>

```aiignore
CREATE <int> THREADS {
code_block
}
```
**Above meaning:** Creates N number of threads, that all execute the code in code_block. Threads 
automatically get their own connection so they are not using the main thread's one.

## Synchronising Threads
```aiignore
SYNCHRONISE;
```
or
```aiignore
SYNCHRONIZE;
```
**Above meaning:** Creates a shared synchronisation object with a default name and blocks on it until all 
currently active threads in this test have also done the same. Then they all get released together.

<hr>

```aiignore
SYNCHRONISE '<someName>';
```
**Above meaning:**  Creates a shared synchronisation object with a specific name and then blocks on it 
until all active threads in this test have also done the same. Then they all get released together.

<hr>

```aiignore
SYNCHRONISE '<someName>', <int>;
```
**Above meaning:** Creates a shared synchronisation object with a specific name and then blocks on it 
until a total of `<int>` threads have also done the same. Then they all get released together.

## Creating New Connections
```aiignore
CREATE CONNECTION '<someName>';
```
Above meaning: Creates a connection with a given name to the default URL. Subsequent commands will 
use this connection from this point on.

<hr>

```aiignore
CREATE CONNECTION '<someName>', '<someURL>';
```
**Above meaning:** Creates a connection with a given name and URL. Subsequent commands will use this 
connection from this point on.

## Switching Between Connections
```aiignore
USE CONNECTION '<someName>';
```
or
```aiignore
USE '<someName>';
```
**Above meaning:** Retrieves the previous created connection and will use it for subsequent commands. 
The original connection of the test file can be retrieved under the name "default" (case sensitive).

## Dropping Connections
```aiignore
DROP CONNECTION '<someName>';
```
or
```aiignore
DROP '<someName>';
```
**Above meaning:** Closes the existing connection with the given name. Subsequent server commands 
revert back to using the default connection that was created when the test file is executed. This 
default connection cannot be dropped. The default connection name is `default` (case sensitive).

## Including Content From Other Files
```aiignore
INCLUDE FILE '<path>';
```
or
```aiignore
INCLUDE '<path>';
```
**Above meaning:** Includes and runs the content of the specified file in the context of the current file. 
Not to be used for running a list of tests, but for including common test steps.

If the path specified is a relative path then by default it is relative to the location of the current test 
file, unless this is overridden in an option to the test harness.

If the included file contains its own `CLEANUP` section then these clean up commands will be executed before 
the current file's CLEANUP section commands. If there are multiple files being included, then their `CLEANUP` 
sections will execute in the same order as their inclusion.

## Failing a Test
```aiignore
FAIL;
```
**Above meaning:** Always causes the test file to be failed when this command is executed.

## Setting and Unsetting Options
```aiignore
SET OPTION <key>;
SET OPTION <key> = <value>;
UNSET OPTION <key>;
```
**Above meaning:**  Sets / unsets keys and values for the current test file only. Refer to the 
[Options](options.md) section for further details.



<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="test-syntax.md">Prev: Test File Syntax</a></td>
    <td style="text-align: right;"><a href="options.md">Next: Options/a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>