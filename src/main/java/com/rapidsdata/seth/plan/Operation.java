// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.ValidationException;

import java.io.File;

public abstract class Operation
{
  /** The metadata about where this operation came from in the test file. */
  private final OperationMetadata metadata;


  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   */
  public Operation(OperationMetadata metadata)
  {
    this.metadata = metadata;
  }

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
   * Validates the operation.
   * This does not execute the operation, but it ensures that the operation is semantically correct.
   * e.g., the statement has the correct expected result, etc.
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws ValidationException if the validation fails.
   */
  public abstract void validate(ExecutionContext xContext) throws ValidationException;

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


  @Override
  public String toString()
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
}
