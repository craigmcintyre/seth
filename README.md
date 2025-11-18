# SETH

SETH is the **S**QL **E**xecution **T**est **H**arness, a tool for testing SQL-based databases. It can be used for 
any database with a JDBC interface. 

With SETH you write a test file that contains the SQL statements you wish to execute and the results that you expect
from those statements. SETH will execute these against your database and compare the actual results to the expected
results in a nuanced way. SETH offers a number of ways of expressing the expected result - it's not a dumb string 
comparison.

Here are some examples of the sort of statements and expected results you can write in a test file:

```
# This is a comment

# Expect exactly 2 rows to be returned, but we don't care about their order.
SELECT * FROM t;
unordered rows:
  (2, 'def')
  (1, 'abc')
  
# For this statement we do care about the order the rows are returned.
SELECT * FROM t ORDER BY col1;
ordered rows:
  (1, 'abc')
  (2, 'def')
  
# Here we only care that the result has 2 rows.
# We don't care about their values.
SELECT * FROM t ORDER BY col1;
rows: 2

# This tests that the resultset contains 5 columns with names col1 through col5.
# It validates the first column value (an integer), ignores the value of the second 
# column value (*), validates the 3rd column value (a SQL date) and ignores all 
# remaining column values.
SELECT * FROM t2;
unordered rows:
['col1', 'col2', 'col3', 'col4', 'col5']
(1, *, date '2025-01-01', ...)
(2, *, date '2025-02-01', ...)
(3, *, date '2025-03-01', ...)

# We can also do negative tests and ensure that the error message contains the right message.
SELECT * FROM mr_fusion;
failure contains: "flux capacitor is empty"
```

SETH offers looping, concurrency & multi-threading support, synchronisation, multi-connections, 
command randomisation, variables, options, result recording and much, much more. 

SETH is written in Java so it will run mostly anywhere and it requires a JDBC driver for the database server 
being tested. The commands do not need to be SQL, but SETH does understand SQL data types for nuanced comparisons.

SETH was designed, written and documented by myself, Craig McIntyre. The copyright is owned by my former employer 
Boray Data Co Ltd, who originally open sourced it [here](https://bitbucket.org/rapidsdataproject/seth). 
They can be contacted at info@boraydata.cn.

SETH is licenced under the AGPL. Licence details can be found [here](LICENSE).

You can download the latest release [here](https://github.com/craigmcintyre/seth/releases).

Full documentation, including build and usage details, can be found [here](docs/index.md).

