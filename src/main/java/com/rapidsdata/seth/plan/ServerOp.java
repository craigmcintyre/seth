// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.OperationException;
import com.rapidsdata.seth.exceptions.ValidationException;
import com.rapidsdata.seth.logging.TestLogger;

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
  public ServerOp(OperationMetadata metadata)
  {
    super(metadata);
  }

  /**
   * Validates the operation.
   * This does not execute the operation, but it ensures that the operation is semantically correct.
   * e.g., the statement has the correct expected result, etc.
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
      final String msg = "Could not create a JDBC Statement.";
      throw new OperationException(msg, e, getTestFile(), getLine(), getCommandDesc());
    }

    try {
      boolean hasResultSet = statement.execute(metadata.getDescription());

      if (hasResultSet) {
        rs = statement.getResultSet();
        logger.log("Got a resultSet.");
      }

      // TODO: compare to the expected result

    } catch (SQLException e) {
      // TODO compare to the expected result
      logger.error(e.getMessage());

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
