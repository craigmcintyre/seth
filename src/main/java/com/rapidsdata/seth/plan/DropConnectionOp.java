// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DropConnectionOp extends Operation
{
  /** The name of the connection object to drop. */
  private final String name;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param name The name of the connectio object to use.
   */
  public DropConnectionOp(OperationMetadata metadata, ExpectedResult expectedResult, String name)
  {
    super(metadata, expectedResult);
    this.name = name;
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new DropConnectionOp(this.metadata, expectedResult, this.name);
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
      Connection conn = xContext.removeConnection(name);

      if (!conn.isClosed()) {
        conn.close();
      }

    } catch (BadConnectionNameException | DefaultConnectionNameException e) {
      expectedResult.compareActualAsFailure(e.getMessage());

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;

    } catch (SQLException e) {
      expectedResult.compareActualAsException(e);

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;
    }

    expectedResult.compareActualAsSuccess();
  }
}
