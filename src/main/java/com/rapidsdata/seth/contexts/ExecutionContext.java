// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.exceptions.BadConnectionNameException;
import com.rapidsdata.seth.exceptions.ConnectionNameExistsException;
import com.rapidsdata.seth.plan.Operation;

import java.sql.Connection;
import java.util.Deque;
import java.util.concurrent.Future;

/** A container object that is used when executing test operations. */
public interface ExecutionContext extends TestContext
{
  /**
   * Registers the Future object of an asynchronous task with the task running the test.
   * Typically called as a result of creating a thread in the test.
   * @param future The Future object to be registered.
   */
  public void registerFuture(Future<?> future);

  /**
   * Pushes a queue of cleanup operations onto the head of a queue of cleanup operations to be
   * performed when the test finishes.
   * @param cleanupOps The queue of cleanup operations to be performed when the test finishes.
   */
  public void pushCleanupOperations(Deque<Operation> cleanupOps);

  /**
   * Inserts additional test operations to be executed immediately after the current operation finishes.
   * @param newTestOps The queue of new operations to be inserted into the existing queue of
   *                   test operations.
   */
  public void pushTestOperations(Deque<Operation> newTestOps);

  /**
   * Returns the name of the current connection object used by getConnection().
   * @return the name of the current connection object used by getConnection().
   */
  public String getConnectionName();

  /**
   * Changes the default connection object returned by getConnection() to the one
   * associated with the given case sensitive name.
   * @param name a case sensitive name associated with a connection.
   * @throws BadConnectionNameException if there are no connections with this name.
   */
  public void useConnection(String name) throws BadConnectionNameException;

  /**
   * Returns the current connection object to be used.
   * @return the current connection object to be used.
   */
  public Connection getConnection();

  /**
   * Adds a new Connection object and makes it the new default connection returned by getConnection().
   * @param connection The new Connection object we are saving.
   * @param name The name that we are saving the Connection object under.
   * @throws ConnectionNameExistsException if there is already a connection with this name.
   */
  public void addConnection(Connection connection, String name) throws ConnectionNameExistsException;

  /**
   * Removes the connection associated with this name. The next time getConnection() is called,
   * the Connection object associated with the name "default" will be returned.
   * N.B.: This method does not close the Connection object.
   * @param name the name associated with the Connection object that we wish to remove.
   * @returns the Connection object that was removed.
   * @throws BadConnectionNameException if there are no connections with this name.
   */
  public Connection removeConnection(String name) throws BadConnectionNameException;
}
