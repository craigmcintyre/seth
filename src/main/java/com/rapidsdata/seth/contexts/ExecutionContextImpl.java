// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.PathRelativity;
import com.rapidsdata.seth.exceptions.BadConnectionNameException;
import com.rapidsdata.seth.exceptions.ConnectionNameExistsException;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.SethSystemException;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.Operation;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * The basic implementation of the ExecutionContext interface, which is used by operations when
 * they are being executed.
 */
public class ExecutionContextImpl implements ExecutionContext
{
  /** The TestContext that this ExecutionContext uses. */
  private final TestContext testContext;

  /** The list of Future objects for asynchronous child tasks. */
  private final List<Future<?>> futures;

  /** The name of the current connection returned by getConnection(). */
  private String currentConnectionName = DEFAULT_CONNECTION_NAME;

  /** A map of Connections, keyed by a connection name. */
  private final Map<String,Connection> connectionMap;


  /**
   * Constructor
   * @param testContext The TestContext that this ExecutionContext uses.
   */
  public ExecutionContextImpl(TestContext testContext,
                              List<Future<?>> futures,
                              Map<String,Connection> connectionMap)
  {
    this.testContext = testContext;
    this.futures = futures;
    this.connectionMap = connectionMap;
  }


  /**
   * Registers the Future object of an asynchronous task with the task running the test.
   * Typically called as a result of creating a thread in the test.
   * @param future The Future object to be registered.
   */
  @Override
  public void registerFuture(Future<?> future)
  {
    // This doesn't need any synchronisation since an ExecutionContext is only ever
    // used by a single thread.
    futures.add(future);
  }

  /**
   * Returns the name of the current connection object used by getConnection().
   * @return the name of the current connection object used by getConnection().
   */
  @Override
  public String getConnectionName()
  {
    return currentConnectionName;
  }

  /**
   * Changes the default connection object returned by getConnection() to the one
   * associated with the given case sensitive name.
   * @param name a case sensitive name associated with a connection.
   * @throws BadConnectionNameException if there are no connections with this name.
   */
  @Override
  public void useConnection(String name) throws BadConnectionNameException
  {
    // This doesn't need any synchronisation since an ExecutionContext is only ever
    // used by a single thread.
    if (!connectionMap.containsKey(name)) {
      final String msg = "There is no connection in this context called \"" + name + "\".";
      throw new BadConnectionNameException(msg);
    }

    currentConnectionName = name;
  }

  /**
   * Returns the current connection object to be used.
   * @return the current connection object to be used.
   */
  @Override
  public Connection getConnection()
  {
    Connection conn = connectionMap.get(currentConnectionName);

    if (conn == null) {
      final String msg = "No default connection.";
      throw new SethSystemException(msg);
    }
    return conn;
  }

  /**
   * Adds a new Connection object and makes it the new default connection returned by getConnection().
   * @param connection The new Connection object we are saving.
   * @param name       The name that we are saving the Connection object under.
   * @throws ConnectionNameExistsException if there is already a connection with this name.
   */
  @Override
  public void addConnection(Connection connection, String name) throws ConnectionNameExistsException
  {
    if (connectionMap.containsKey(name)) {
      final String msg = "A connection with the name \"" + name + "\" already exists in this context.";
      throw new ConnectionNameExistsException(msg);
    }

    connectionMap.put(name, connection);
  }

  /**
   * Removes the connection associated with this name. The next time getConnection() is called,
   * the Connection object associated with the name "default" will be returned.
   * N.B.: This method does not close the Connection object.
   * @param name the name associated with the Connection object that we wish to remove.
   * @returns the Connection object that was removed.
   * @throws BadConnectionNameException if there are no connections with this name.
   */
  @Override
  public Connection removeConnection(String name) throws BadConnectionNameException
  {
    if (!connectionMap.containsKey(name)) {
      final String msg = "There is no connection in this context called \"" + name + "\".";
      throw new BadConnectionNameException(msg);
    }

    return connectionMap.remove(name);
  }



  /***********************************************************************************************/
  /*** Proxied methods from other base contexts.                                               ***/
  /***********************************************************************************************/

  /**
   * Returns the test file currently being executed.
   * @return the test file currently being executed.
   */
  @Override
  public File getTestFile()
  {
    return testContext.getTestFile();
  }

  /**
   * A notification flag to child threads whether to continue testing or whether the test is ending
   * and they should stop executing.
   * @return true if the test is continuing or false if it is ending.
   */
  @Override
  public boolean continueTesting()
  {
    return testContext.continueTesting();
  }

  /**
   * Blocks the current thread until testing operations have completed (either successfully
   * or in failure) and threads should now start executing cleanup operations.
   */
  @Override
  public void waitForCleanup()
  {
    testContext.waitForCleanup();
  }

  /**
   * Marks the test result as being aborted and causes all the test threads to stop executing and
   * start cleaning up.
   */
  @Override
  public void abortTest()
  {
    testContext.abortTest();
  }

  /**
   * Marks the test result as successful and causes the test threads to begin cleaning up.
   */
  @Override
  public void markAsSucceeded()
  {
    testContext.markAsSucceeded();
  }

  /**
   * Marks the test result as a failure and causes the test threads to begin cleaning up.
   * @param failure
   */
  @Override
  public void markAsFailed(FailureException failure)
  {
    testContext.markAsFailed(failure);
  }

  /**
   * Accumulates a count of test steps that occurred in a given testing thread. Each thread should
   * report the number of test steps is executed when the thread completes.
   * @param count the number of test steps that the testing thread executed.
   */
  @Override
  public void accumulateTestSteps(long count)
  {
    testContext.accumulateTestSteps(count);
  }

  /**
   * Returns the time since the application started, in nanoseconds since the epoch.
   * @return the time since the application started, in nanoseconds since the epoch.
   */
  @Override
  public long getAppStartTime()
  {
    return testContext.getAppStartTime();
  }

  /**
   * Returns the list of test files to be executed.
   * @return the list of test files to be executed.
   */
  @Override
  public List<File> getTestFiles()
  {
    return testContext.getTestFiles();
  }

  /**
   * Returns the url that the JDBC driver should use to communicate to the system being tested.
   * @return the url that the JDBC driver should use to communicate to the system being tested.
   */
  @Override
  public String getUrl()
  {
    return testContext.getUrl();
  }

  /**
   * If true then the test files will only be validated and not executed.
   * If false then the test files will be executed in full.
   * @return whether to only validate or to fully execute the test files.
   */
  @Override
  public boolean onlyValidateTestFiles()
  {
    return testContext.onlyValidateTestFiles();
  }

  /**
   * Returns how we are to treat relative paths.
   * @return how we are to treat relative paths.
   */
  @Override
  public PathRelativity getPathRelativity()
  {
    return testContext.getPathRelativity();
  }

  /**
   * Returns the logger we are to use to write to the console and to file.
   * @return the logger we are to use to write to the console and to file.
   */
  @Override
  public TestLogger getLogger()
  {
    return testContext.getLogger();
  }

  /**
   * Returns the thread pool executor service, by which new threads can be created.
   * @return the thread pool executor service, by which new threads can be created.
   */
  @Override
  public ExecutorService getThreadPool()
  {
    return testContext.getThreadPool();
  }
}
