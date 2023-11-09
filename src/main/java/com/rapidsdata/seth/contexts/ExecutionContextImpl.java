// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth.contexts;

import com.rapidsdata.seth.*;
import com.rapidsdata.seth.exceptions.*;
import com.rapidsdata.seth.logging.TestLogger;

import java.io.File;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.CyclicBarrier;
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
      final String msg = "There is no connection in this context with this name: " + name;
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
      // Should never happen.
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
      final String msg = "A connection with this name already exists in this context: " + name;
      throw new ConnectionNameExistsException(msg);
    }

    connectionMap.put(name, connection);

    // Set this connection as the default.
    try {
      useConnection(name);

    } catch (BadConnectionNameException e) {
      // Should never happen
      throw new SethSystemException(e);
    }
  }

  /**
   * Removes the connection associated with this name. The next time getConnection() is called,
   * the Connection object associated with the name "default" will be returned.
   * N.B.: This method does not close the Connection object.
   * @param name the name associated with the Connection object that we wish to remove.
   * @returns the Connection object that was removed.
   * @throws BadConnectionNameException if there are no connections with this name.
   * @throws DefaultConnectionNameException if the operation occurs on the default connection.
   */
  @Override
  public Connection removeConnection(String name) throws BadConnectionNameException,
                                                         DefaultConnectionNameException
  {
    if (!connectionMap.containsKey(name)) {
      final String msg = "There is no connection in this context with this name: " + name;
      throw new BadConnectionNameException(msg);
    }

    if (name.equals(DEFAULT_CONNECTION_NAME)) {
      final String msg = "Cannot drop the default connection: " + DEFAULT_CONNECTION_NAME;
      throw new DefaultConnectionNameException(msg);
    }

    Connection conn = connectionMap.remove(name);

    // Set the default connection as the current one.
    try {
      useConnection(DEFAULT_CONNECTION_NAME);

    } catch (BadConnectionNameException e) {
      // Should never happen
      throw new SethSystemException(e);
    }

    return conn;
  }

  /**
   * Returns true if a connection with this name currently exists, otherwise false.
   * @param name the name of the connection we are checking for.
   * @return true if a connection with this name currently exists, otherwise false.
   */
  public boolean hasConnection(String name)
  {
    return connectionMap.containsKey(name);
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
   * Marks the test result that the test has started executing.
   */
  @Override
  public void markAsStarted()
  {
    testContext.markAsStarted();
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
   * Returns the result of the test
   */
  @Override
  public TestResult getResult() {
    return testContext.getResult();
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
   * Increments a count of the number of active threads in the system.
   */
  @Override
  public void incrementActiveThreads()
  {
    testContext.incrementActiveThreads();
  }

  /**
   * Decrements a count of the number of active threads in the system.
   */
  @Override
  public void decrementActiveThreads()
  {
    testContext.decrementActiveThreads();
  }

  /**
   * Returns the number of active threads in the system.
   * @return the number of active threads in the system.
   */
  @Override
  public int getNumActiveThreads()
  {
    return testContext.getNumActiveThreads();
  }

  /**
   * Returns the synchronisation barrier associated with a given name for synchronising threads on.
   * Creates the barrier if one does not exist (threadsafe).
   * @param name     the name to associate with the synchronisation barrier.
   * @param parties  the number of threads to wait on.
   * @return the synchronisation barrier.
   */
  @Override
  public CyclicBarrier getOrCreateSyncObject(String name, int parties)
  {
    return testContext.getOrCreateSyncObject(name, parties);
  }

  /**
   * Removes the given synchronisation barrier associated with a given name.
   * @param name  the name of the object to remove.
   * @param barrier the actual synchronisation object to be removed.
   */
  @Override
  public void removeSyncObject(String name, CyclicBarrier barrier)
  {
    testContext.removeSyncObject(name, barrier);
  }

  /**
   * Returns the time that the application started, in milliseconds since the epoch.
   * @return the time that the application started, in milliseconds since the epoch.
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
  public List<TestableFile> getTestableFiles()
  {
    return testContext.getTestableFiles();
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

  /**
   * Returns the command line arguments used to run the program.
   * @return the command line arguments used to run the program.
   */
  @Override
  public CommandLineArgs getCommandLineArgs()
  {
    return testContext.getCommandLineArgs();
  }

  /**
   * Returns the options that are applied to the whole application.
   * @return the options that are applied to the whole application.
   */
  @Override
  public Options getAppOptions()
  {
    return testContext.getAppOptions();
  }

  /**
   * Returns the options that are applied to the this test.
   * @return the options that are applied to the this test.
   */
  @Override
  public Options getTestOptions()
  {
    return testContext.getTestOptions();
  }

}
