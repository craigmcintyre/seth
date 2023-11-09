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
import java.util.List;
import java.util.Map;

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

    String cmd = metadata.getDescription();

    // Check if the command is to be ignored
    List<String> cmdIgnoreList = xContext.getCommandLineArgs().ignoreCommands;
    if (!cmdIgnoreList.isEmpty()) {
      for (String ignorableCmd : cmdIgnoreList) {
        if (cmd.equalsIgnoreCase(ignorableCmd)) {

          // Record the count of how many times this command was ignored
          xContext.getResult().accumulateIgnoredCommand(cmd);

          // Don't do anything more with this command
          return;
        }
      }
    }

    try {
      statement = connection.createStatement();

    } catch (SQLException e) {
      expectedResult.assertActualAsException(xContext, e);

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;
    }

    try {
      boolean hasResultSet = statement.execute(cmd);

      if (hasResultSet) {
        rs = statement.getResultSet();
        expectedResult.assertActualAsResultSet(xContext, rs, statement.getWarnings());

      } else {
        expectedResult.assertActualAsUpdateCount(xContext, statement.getUpdateCount(), statement.getWarnings());
      }

    } catch (SQLException e) {
      expectedResult.assertActualAsException(xContext, e);

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
