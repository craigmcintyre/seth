// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.sql.Connection;
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
      expectedResult.assertActualAsFailure(xContext, e.getMessage());

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;

    } catch (SQLException e) {
      expectedResult.assertActualAsException(xContext, e);

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;
    }

    expectedResult.assertActualAsSuccess(xContext, null);
  }
}
