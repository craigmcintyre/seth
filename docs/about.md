<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="index.md">Prev: Table of Contents</a></td>
    <td style="text-align: right;"><a href="version-history.md">Next: Version History</a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# About SETH

SETH is the **S**QL **E**xecution **T**est **H**arness, a tool for testing SQL-based databases. It can be used for 
any database with a JDBC interface.

With SETH you write a test file that contains the SQL statements you wish to execute and the results that you expect
from those statements. SETH will execute these against your database and compare the actual results to the expected
results in a nuanced way. SETH offers a number of ways of expressing the expected result - it's not a dumb string
comparison.

SETH understands common SQL data types and how to compare them. Comparing floating point numbers is not the same as
comparing integers. SETH also allows the writer to be choosy about what needs to be compared and what can be ignored.

SETH offers looping, concurrency & multi-threading support, synchronisation, multi-connections,
command randomisation, variables, options, result recording and much, much more.

SETH is written in Java so it will run mostly anywhere and it requires a JDBC driver for the database server
being tested. The commands do not need to be SQL, but SETH does understand SQL data types for nuanced comparisons.

SETH was designed, written and documented by myself, Craig McIntyre. The copyright is owned by my former employer
Boray Data Co Ltd, who originally open sourced it [here](https://bitbucket.org/rapidsdataproject/seth).
They can be contacted at info@boraydata.cn.

SETH is licenced under the AGPL. Licence details can be found [here](LICENSE).

SETH was written as the main test tool for functionally testing a new SQL database being developed. It routinely ran
close to 10,000 tests in each CI run, providing significant developer productivity and assurance that the system was
operating as it should be.

SETH can be adopted to run against any system that accepts commands and returns data, so long as that system can 
provide a basic JDBC interface that can execute statements and return result sets and metadata. 

<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="index.md">Prev: Table of Contents</a></td>
    <td style="text-align: right;"><a href="version-history.md">Next: Version History</a></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>