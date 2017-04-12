// Copyright (c) 2017 Boray Data Co. Ltd.  All rights reserved.

package com.rapidsdata.seth;

import com.rapidsdata.seth.contexts.ExecutionContext;
import com.rapidsdata.seth.contexts.ExecutionContextImpl;
import com.rapidsdata.seth.contexts.TestContext;
import com.rapidsdata.seth.exceptions.FailureException;
import com.rapidsdata.seth.exceptions.TestSetupException;
import com.rapidsdata.seth.logging.TestLogger;
import com.rapidsdata.seth.plan.Operation;
import com.rapidsdata.seth.plan.Plan;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class TestRunner implements Runnable
{
  private static final String DEFAULT_CONNECTION_NAME = "default";

  /** The test plan to be executed. */
  private final Plan plan;

  /**
   * If true then this thread is the first thread running the test. When this thread reaches the
   * end of the test statements then the test ends and all child threads will start cleaning up.
   */
  private final boolean isPrimaryThread;

  /** A list of Futures from any child threads. */
  private final List<Future<?>> childFutures = new LinkedList<>();

  /** A map of connections keyed by a user-defined name. */
  private final Map<String, Connection> connectionMap = new HashMap<>();

  /** The context containing common test information. */
  private final TestContext testContext;

  /**
   * Constructor
   * @param plan the plan to be executed.
   * @param testContext the test context that holds various test information.
   * @param isPrimaryThread whether this is running as the primary thread of the test.
   */
  public TestRunner(Plan plan, TestContext testContext, boolean isPrimaryThread)
  {
    this.plan = plan;
    this.testContext = testContext;
    this.isPrimaryThread = isPrimaryThread;

    // increment the count of the active threads.
    testContext.incrementActiveThreads();
  }

  /**
   * Constructor for child threads of a test.
   * @param plan the plan to be executed.
   * @param testContext the test context that holds various test information.
   */
  public TestRunner(Plan plan, TestContext testContext)
  {
    this.plan = plan;
    this.testContext = testContext;
    this.isPrimaryThread = false;

    // increment the count of the active threads.
    testContext.incrementActiveThreads();
  }

  /**
   * When an object implementing interface <code>Runnable</code> is used
   * to create a thread, starting the thread causes the object's
   * <code>run</code> method to be called in that separately executing
   * thread.
   * <p>
   * The general contract of the method <code>run</code> is that it may
   * take any action whatsoever.
   *
   * @see Thread#run()
   */
  @Override
  public void run()
  {
    TestLogger logger = testContext.getLogger();

    long stepCount = 0;

    // Mark the test result as having started.
    testContext.markAsStarted();

    // Make the default connection.
    try {
      createDefaultConnection();

    } catch (TestSetupException e) {
      testContext.markAsFailed(e);
      testContext.decrementActiveThreads();

      // No test operations run, so nothing to cleanup.
      return;
    }


    try {
      // Make the execution context that each operation will use.
      ExecutionContext xContext = new ExecutionContextImpl(testContext, childFutures, connectionMap);

      // Run all of the test operations until they complete, an error occurs or
      // until we are told that the test is not longer continuing.
      boolean earlyExit = false;
      for (Operation op : plan.getTestOperations()) {

        // Check if a failure occurred in another thread and we have to stop running the test.
        if (!testContext.continueTesting()) {
          earlyExit = true;
          break;
        }

        logger.testStepExecuting(op.getTestFile(), op.toString(), op.getLine());
        ++stepCount;

        try {
          op.execute(xContext);

        } catch (FailureException e) {
          testContext.markAsFailed(e);
          earlyExit = true;
        }
      }

      if (!earlyExit) {
        if (isPrimaryThread) {
          // If we are the primary thread then when we have finished all the test operations
          // we can mark the test as having succeeded. This will also result in causing all
          // other child threads to start their own cleanup actions.
          testContext.markAsSucceeded();

        } else {
          // Since we are not the primary thread, when we have finished our test operations
          // we must wait until the primary thread has finished its operations or until
          // an error has occurred.
          testContext.waitForCleanup();
        }
      }

      // Run all of the cleanup operations
      for (Operation op : plan.getCleanupOperations()) {

        logger.testStepExecuting(op.getTestFile(), op.toString(), op.getLine());

        try {
          op.execute(xContext);

        } catch (FailureException e) {
          // TODO: ignore failures in cleanup?
        }
      }

      // Wait for all child threads to exit.
      waitForChildrenToExit();

    } finally {
      closeAllConnections();
      testContext.decrementActiveThreads();
      testContext.accumulateTestSteps(stepCount);
    }
  }

  /**
   * Creates the default connection to the server and saves it in the connection map
   * under the name "default".
   * @throws TestSetupException if there is an error creating the connection.
   */
  protected void createDefaultConnection() throws TestSetupException
  {
    Connection conn = null;

    try {
      conn = DriverManager.getConnection(testContext.getUrl());

    } catch (SQLException e) {
      final String msg = "Could not create the default connection to the server with url: \"" +
                         testContext.getUrl() + "\".";
      throw new TestSetupException(msg, e, testContext.getTestFile());
    }

    connectionMap.put(ExecutionContext.DEFAULT_CONNECTION_NAME, conn);
  }

  /**
   * Wait for all child threads to exit.
   */
  protected void waitForChildrenToExit()
  {
    for (Future<?> future : childFutures) {

      while (true) {  // necessary to retry in case we get an InterruptedException

        try {
          future.get();
          break;

        } catch (InterruptedException e) {
          continue;

        } catch (ExecutionException e) {
          // ignore
        }
      } // while(true)
    }
  }

  /**
   * Closes all open connections in the connectionMap.
   */
  protected void closeAllConnections()
  {
    for (Connection conn : connectionMap.values()) {
      try {
        conn.close();
      } catch (SQLException e) { /*ignore*/ }
    }

    connectionMap.clear();
  }
}
