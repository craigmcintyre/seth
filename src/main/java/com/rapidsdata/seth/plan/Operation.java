// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.io.File;

public abstract class Operation
{
  /** The metadata about where this operation came from in the test file. */
  protected final OperationMetadata metadata;

  /** The expected result for this operation. */
  protected final ExpectedResult expectedResult;

  protected final boolean executeImmediately;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   */
  public Operation(OperationMetadata metadata, ExpectedResult expectedResult)
  {
    this.metadata = metadata;
    this.expectedResult = expectedResult;
    this.executeImmediately = false;
  }


  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   */
  public Operation(OperationMetadata metadata, ExpectedResult expectedResult, boolean executeImmediately)
  {
    this.metadata = metadata;
    this.expectedResult = expectedResult;
    this.executeImmediately = executeImmediately;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  public abstract Operation rewriteWith(ExpectedResult expectedResult);

  /**
   * Returns the test file that this operation came from.
   * @return the test file that this operation came from.
   */
  public File getTestFile()
  {
    return metadata.getTestFile();
  }

  /**
   * Returns the line number that this operation occurs on in the above file.
   * @return the line number that this operation occurs on in the above file.
   */
  public long getLine()
  {
    return metadata.getLine();
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
   * This can be overridden because some methods (e.g., LOOP / CREATE THREAD)
   * don't need to be shown with all their detail.
   * @return a string describing the command to be executed.
   */
  protected String getCommandDesc()
  {
    return metadata.getDescription();
  }

  /**
   * Returns a string describing the expected result, or null if there isn't one.
   * @return a string describing the expected result, or null if there isn't one.
   */
  protected String getExpectedResultDesc()
  {
    // TODO
    return null;
  }

  public String describe()
  {
    StringBuffer sb = new StringBuffer(1024);

    sb.append("Command  : ")
      .append(getCommandDesc());

    String expected = getExpectedResultDesc();

    if (expected != null && !expected.isEmpty()) {
      sb.append("\nExpected : ")
        .append(expected);
    }

    return sb.toString();
  }

  /**
   * Returns whether the operation should execute immediately after it is created,
   * or as part of the regularly running test.
   * @return true if the operation should execute immediately after it is created
   * or false if it can run sequentially with the rest of the test operations.
   */
  public boolean getExecuteImmediately()
  {
    return executeImmediately;
  }

  @Override
  public String toString()
  {
    return getCommandDesc();
  }
}
