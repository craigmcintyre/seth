// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.ValidationException;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

public class LogOp extends Operation
{
  /** The message to be logged. */
  private final String message;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param message The message to be logged.
   */
  public LogOp(OperationMetadata metadata, ExpectedResult expectedResult, String message)
  {
    super(metadata, expectedResult);

    this.message = message;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new LogOp(this.metadata, expectedResult, this.message);
  }

  /**
   * Validates the operation.
   * This does not execute the operation, but it ensures that the operation is semantically correct.
   * e.g., an INCLUDE statement can find the file it is including, the statement has the correct
   * expected result, etc.
   *
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws ValidationException if the validation fails.
   */
  @Override
  public void validate(ExecutionContext xContext) throws ValidationException
  {
    // TODO: Validate the expected result type.
  }

  /**
   * Executes the operation.
   * @param xContext The execution context, which encapsulates any necessary parameters.
   * @throws FailureException if an error occurs during the execution. e.g., if the
   *                          expected response does not meet the actual response.
   */
  @Override
  public void execute(ExecutionContext xContext) throws FailureException
  {
    xContext.getLogger().log(message);
  }
}
