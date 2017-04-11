// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.plan;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.exceptions.ConnectionNameExistsException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.OperationException;
import com.rapidsdata.seth.exceptions.ValidationException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
  public CreateConnectionOp(OperationMetadata metadata, String name, String url)
  {
    super(metadata);
    this.name = name;
    this.url = url;
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
    if (xContext.hasConnection(name)) {
      final String msg = "Cannot create connection as this connection name already exists: " + name;
      throw new OperationException(msg, getTestFile(), getLine(), getCommandDesc());
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
      final String msg = "Unable to create a connection using URL: " + connUrl;
      throw new OperationException(msg, getTestFile(), getLine(), getCommandDesc());
    }

    try {
      xContext.addConnection(conn, name);

    } catch (ConnectionNameExistsException e) {
      // Should never happen because connections are not shared.
      final String msg = "Cannot create connection as this connection name already exists: " + name;
      throw new OperationException(msg, getTestFile(), getLine(), getCommandDesc());
    }
  }
}
