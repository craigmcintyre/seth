// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;

public abstract class Operation
{
  /** The path of the test file that this operation came from. */
  private final String testFilePath;

  /** The line number that this operation occurs on in the above file. */
  private final long line;


  /**
   * Constructor
   * @param testFilePath The path of the test file that this operation came from.
   * @param line The line number that this operation occurs on in the above file.
   */
  public Operation(String testFilePath, long line)
  {
    this.testFilePath = testFilePath;
    this.line = line;
  }

  /**
   * Returns the path of the test file that this operation came from.
   * @return the path of the test file that this operation came from.
   */
  public String getTestFilePath()
  {
    return testFilePath;
  }

  /**
   * Returns the line number that this operation occurs on in the above file.
   * @return the line number that this operation occurs on in the above file.
   */
  public long getLine()
  {
    return line;
  }

  /**
   * Executes the operation.
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws FailureException if an error occurs during the execution. e.g., if the
   *                          expected response does not meet the actual response.
   */
  public abstract void execute(ExecutionContext xContext) throws FailureException;

  /**
   * Returns a string describing the command to be executed.
   * @return a string describing the command to be executed.
   */
  protected abstract String getCommandDesc();

  /**
   * Returns a string describing the expected result, or null if there isn't one.
   * @return a string describing the expected result, or null if there isn't one.
   */
  protected abstract String getExpectedResultDesc();


  @Override
  public String toString()
  {
    String expected = getExpectedResultDesc();
    if (expected == null || expected.isEmpty()) {
      expected = "(none)";
    }

    String desc = String.format("Command  : %s\nExpected : %s",
                                getCommandDesc(),
                                expected);

    return desc;
  }
}
