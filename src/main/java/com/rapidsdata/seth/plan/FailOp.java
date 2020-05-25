// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.OperationException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

/** An operator that always causes the test to fail. */
public class FailOp extends Operation
{
  private static final String DEFAULT_MSG = "Execution halted at FAIL command.";

  private final String failureMsg;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param failureMsg The failure message to display.
   */
  public FailOp(OperationMetadata metadata, ExpectedResult expectedResult, String failureMsg)
  {
    super(metadata, expectedResult);

    this.failureMsg = (failureMsg == null ? DEFAULT_MSG : failureMsg);
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new FailOp(this.metadata, expectedResult, this.failureMsg);
  }

  /**
   * Executes the operation.
   *
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws FailureException if an error occurs during the execution. e.g., if the
   *                          expected response does not meet the actual response.
   */
  @Override
  public void execute(ExecutionContext xContext) throws FailureException
  {
    throw new OperationException(failureMsg, metadata.getTestFile(), metadata.getLine(), describe());
  }

  @Override
  public String describe()
  {
    return "Command  : " + getCommandDesc();
  }
}
