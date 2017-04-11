// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class UseConnectionOp extends Operation
{
  /** The name of the connection object to use. */
  private final String name;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param name The name of the connectio object to use.
   */
  public UseConnectionOp(OperationMetadata metadata, String name)
  {
    super(metadata);
    this.name = name;
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
    try {
      xContext.useConnection(name);

    } catch (BadConnectionNameException e) {
      throw new OperationException(e.getMessage(), getTestFile(), getLine(), getCommandDesc());
    }
  }
}
