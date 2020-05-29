// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.plan.expectedResults.ExpectedResult;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class CreateConnectionOp extends Operation
{
  /** The name of the connection object to use. */
  private final String name;

  /** The url of the connection. May be null. */
  private final String url;

  /**
   * Constructor
   * @param metadata The metadata about where this operation came from in the test file.
   * @param name The name of the connectio object to use.
   * @param url The url of the connection. May be null.
   */
  public CreateConnectionOp(OperationMetadata metadata, ExpectedResult expectedResult, String name, String url)
  {
    super(metadata, expectedResult);
    this.name = name;
    this.url = url;
  }

  /**
   * Rewrites the current operation with the given expected result.
   *
   * @param expectedResult the expected result to compare to.
   * @return the newly rewritten, immutable Operation with the new expected result.
   */
  @Override
  public Operation rewriteWith(ExpectedResult expectedResult)
  {
    return new CreateConnectionOp(this.metadata, expectedResult, this.name, this.url);
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
    if (xContext.hasConnection(name)) {
      final String msg = "Cannot create connection as this connection name already exists: " + name;
      expectedResult.assertActualAsFailure(xContext, msg);

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;
    }

    String connUrl = this.url;

    if (connUrl == null) {
      // Use the default URL.
      connUrl = xContext.getUrl();
    }

    Connection conn;
    try {
      conn = DriverManager.getConnection(connUrl);

    } catch (SQLException e) {
      expectedResult.assertActualAsException(xContext, e);

      // Since the above call returned, we must have expected this failure otherwise
      // an exception would have been thrown. Job done.
      return;
    }

    try {
      xContext.addConnection(conn, name);

    } catch (ConnectionNameExistsException e) {
      // Should never happen because connections are not shared.
      throw new SethSystemException(e);
    }

    expectedResult.assertActualAsSuccess(xContext, null);
  }
}
