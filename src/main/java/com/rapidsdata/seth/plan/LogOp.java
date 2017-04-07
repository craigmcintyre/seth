// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.ValidationException;

public class LogOp extends Operation
{
  /** The message to be logged. */
  private final String message;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param message The message to be logged.
   */
  public LogOp(OperationMetadata metadata, String message)
  {
    super(metadata);

    this.message = message;
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
