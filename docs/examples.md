<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="variables.md">Prev: Variables</a></td>
    <td style="text-align: right;"></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>

# Example Test Files

## Example 1 - A basic test

```aiignore
CREATE SCHEMA IF NOT EXISTS public;
success
DROP TABLE t IF EXISTS;
success

CREATE TABLE t (col0 INTEGER, col1 STRING);
success

INSERT INTO t VALUES (0, 'a'), (1,'b'), (2,'c');
affected: 3

SELECT * FROM t;
unordered rows:
(0,'a'),
(1,'b'),
(2,'c')
SELECT * FROM t;
[ignorecase] unordered rows:
(0,'A'),
(1,'B'),
(2,'C')

cleanup {
    DROP SCHEMA IF EXISTS public;
}
```

## Example 2 - A multithreaded load test

```aiignore
-- Ensure a clean starting point.
DROP SCHEMA IF EXISTS public;
success

-- Create a child thread to continuously create and drop schemas
-- forever until the primary thread finishes.
CREATE THREAD {

LOOP {
    CREATE SCHEMA public;
    success
    
        DROP SCHEMA public;
        success
    }
}

-- Create a second child thread to continuously try to create tables
-- in the above schema.
CREATE THREAD {

    LOOP {  
    -- Note that the expected response is muted - we don't care if
    -- this statement fails because we are actually testing the
    -- creation/dropping of schemas instead.
    CREATE TABLE IF NOT EXISTS t (col0 INTEGER, col1 STRING);
    mute
    
        DROP TABLE IF EXISTS t;
        mute
    }
}

-- Meanwhile the main thread will wait for 5 minutes before exiting
-- the test.
SLEEP 300000;

-- When the main thread exits it will stop any child threads.

cleanup {
    DROP SCHEMA IF EXISTS public;
}
```





<table style="width:100%;">
  <tr>
    <td style="text-align: left;"><a href="variables.md">Prev: Variables</a></td>
    <td style="text-align: right;"></td>
  </tr>
  <tr>
    <td colspan="2" style="text-align: center;"><a href="index.md">Contents</a></td>
  </tr>
</table>