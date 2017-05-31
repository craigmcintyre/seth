// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class ServerOp extends Operation
{
  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   */
  public ServerOp(OperationMetadata metadata, ExpectedResult expectedResult)
  {
    super(metadata, expectedResult);
  }

  /**
   * Rewrites the current operation with the given expected result.
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new ServerOp(this.metadata, expectedResult);
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
    TestLogger logger = xContext.getLogger();

    // Get the currently active connection.
    Connection connection = xContext.getConnection();
    Statement statement = null;
    ResultSet rs = null;

    try {
      statement = connection.createStatement();

    } catch (SQLException e) {
      expectedResult.assertActualAsException(e);

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;
    }

    try {
      boolean hasResultSet = statement.execute(metadata.getDescription());

      if (hasResultSet) {
        rs = statement.getResultSet();
        expectedResult.assertActualAsResultSet(rs);

      } else {
        expectedResult.assertActualAsUpdateCount(statement.getUpdateCount());
      }

    } catch (SQLException e) {
      expectedResult.assertActualAsException(e);

    } finally {

      try {
        if (rs != null && !rs.isClosed())                 { rs.close();         }
        if (statement != null && !statement.isClosed())   { statement.close();  }
      } catch (SQLException e) {
        // ignore
      }
    }
  }
}
